/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.net.ipsec.ike;

import static android.net.ipsec.ike.IkeManager.getIkeLog;

import android.system.ErrnoException;
import android.system.Os;
import android.util.LongSparseArray;

import com.android.internal.annotations.VisibleForTesting;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * IkeUdpEncapPortPacketHandler is a template for IKE Sockets that target port 4500.
 *
 * <p>Specifically, this class helps for IKE Sockets that target port 4500 and need to utilize
 * 4-byte non-ESP markers in their incoming/outgoing IKE packets, as specified in RFC 3948 Section
 * 2.2.
 */
public class IkeUdpEncapPortPacketHandler {
    private static final String TAG = IkeUdpEncapPortPacketHandler.class.getSimpleName();

    @VisibleForTesting static final int NON_ESP_MARKER_LEN = 4;
    @VisibleForTesting static final byte[] NON_ESP_MARKER = new byte[NON_ESP_MARKER_LEN];

    private final FileDescriptor mSocket;

    /** Creates a IkeUdpEncapPortPacketHandler with the given FileDescriptor. */
    public IkeUdpEncapPortPacketHandler(FileDescriptor socket) {
        mSocket = socket;
    }

    /** Package private */
    @VisibleForTesting
    static class PacketReceiver implements IkeSocket.IPacketReceiver {
        private static final String TAG = IkeUdpEncapPortPacketHandler.class.getSimpleName();

        public void handlePacket(
                byte[] recvbuf, LongSparseArray<IkeSocket.Callback> spiToCallback) {
            if (recvbuf.length < NON_ESP_MARKER_LEN) {
                getIkeLog().d(TAG, "Received too short of packet. Ignoring.");
                return;
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(recvbuf);

            // Check the existence of the Non-ESP Marker. A received packet can be either an IKE
            // packet starts with 4 zero-valued bytes Non-ESP Marker or an ESP packet starts with 4
            // bytes ESP SPI. ESP SPI value can never be zero.
            byte[] espMarker = new byte[NON_ESP_MARKER_LEN];
            byteBuffer.get(espMarker);
            if (!Arrays.equals(NON_ESP_MARKER, espMarker)) {
                // Drop the received ESP packet.
                getIkeLog().e(TAG, "Received an ESP packet. Dropped.");
                return;
            }

            // Re-direct IKE packet to IkeSessionStateMachine according to the locally generated
            // IKE SPI.
            byte[] ikePacketBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(ikePacketBytes);
            IkeSocket.parseAndDemuxIkePacket(ikePacketBytes, spiToCallback, TAG);
        }
    }

    /**
     * Send encoded IKE packet to destination address
     *
     * @param ikePacket encoded IKE packet
     * @param serverAddress IP address of remote server
     *
     * <p>package-private
     */
    void sendIkePacket(byte[] ikePacket, InetAddress serverAddress) {
        getIkeLog()
                .d(
                        TAG,
                        "Send packet to "
                                + serverAddress.getHostAddress()
                                + "( "
                                + ikePacket.length
                                + " bytes)");
        try {
            ByteBuffer buffer = ByteBuffer.allocate(NON_ESP_MARKER_LEN + ikePacket.length);

            // Build outbound UDP Encapsulation packet body for sending IKE message.
            buffer.put(NON_ESP_MARKER).put(ikePacket);
            buffer.rewind();

            // Use unconnected UDP socket because one {@IkeSocket} may be shared by
            // multiple IKE sessions that send messages to different destinations.
            Os.sendto(mSocket, buffer, 0, serverAddress, IkeSocket.SERVER_PORT_UDP_ENCAPSULATED);
        } catch (ErrnoException | IOException e) {
            // TODO: Handle exception
            getIkeLog().e(TAG, "error sending IKE packet", e);
        }
    }
}

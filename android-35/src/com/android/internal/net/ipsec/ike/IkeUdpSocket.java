/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.os.Handler;
import android.system.ErrnoException;
import android.system.Os;
import android.util.LongSparseArray;

import com.android.internal.annotations.VisibleForTesting;

import libcore.io.IoUtils;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** IkeUdpSocket is an abstract superclass for all non-encapsulating IKE UDP sockets. */
public abstract class IkeUdpSocket extends IkeSocket {
    private static final String TAG = IkeUdpSocket.class.getSimpleName();

    protected static IPacketReceiver sPacketReceiver = new PacketReceiver();

    // FileDescriptor for sending and receving IKE packet.
    protected final FileDescriptor mSocket;

    protected IkeUdpSocket(FileDescriptor socket, IkeSocketConfig sockConfig, Handler handler) {
        super(sockConfig, handler);
        mSocket = socket;
    }

    /** Get FileDescriptor for use with the packet reader polling loop */
    @Override
    protected FileDescriptor getFd() {
        return mSocket;
    }

    /** Package private */
    @VisibleForTesting
    static final class PacketReceiver implements IkeSocket.IPacketReceiver {
        public void handlePacket(byte[] recvbuf, LongSparseArray<Callback> spiToCallback) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(recvbuf);

            // Re-direct IKE packet to IkeSessionStateMachine according to the locally generated
            // IKE SPI.
            byte[] ikePacketBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(ikePacketBytes);
            parseAndDemuxIkePacket(ikePacketBytes, spiToCallback, TAG);
        }
    }

    /** Package private */
    @VisibleForTesting
    static void setPacketReceiver(IkeSocket.IPacketReceiver receiver) {
        sPacketReceiver = receiver;
    }

    /**
     * Handle received IKE packet. Invoked when there is a read event. Any desired copies of
     * |recvbuf| should be made in here, as the underlying byte array is reused across all reads.
     */
    @Override
    protected void handlePacket(byte[] recvbuf, int length) {
        sPacketReceiver.handlePacket(Arrays.copyOfRange(recvbuf, 0, length), mSpiToCallback);
    }

    /**
     * Send encoded IKE packet to destination address
     *
     * @param ikePacket encoded IKE packet
     * @param serverAddress IP address of remote server
     */
    @Override
    public void sendIkePacket(byte[] ikePacket, InetAddress serverAddress) {
        getIkeLog()
                .d(
                        this.getClass().getSimpleName(),
                        "Sending packet to "
                                + serverAddress.getHostAddress()
                                + "( "
                                + ikePacket.length
                                + " bytes)");
        try {
            ByteBuffer buffer = ByteBuffer.wrap(ikePacket);

            // Use unconnected UDP socket because one {@IkeUdpSocket} may be shared by
            // multiple IKE sessions that send messages to different destinations.
            Os.sendto(mSocket, buffer, 0, serverAddress, getIkeServerPort());
        } catch (ErrnoException | IOException e) {
            getIkeLog().i(this.getClass().getSimpleName(), "Failed to send packet", e);
        }
    }

    @Override
    public int getIkeServerPort() {
        return SERVER_PORT_NON_UDP_ENCAPSULATED;
    }

    /** Implement {@link AutoCloseable#close()} */
    @Override
    public void close() {
        try {
            IoUtils.close(mSocket);
        } catch (IOException e) {
            getIkeLog().e(this.getClass().getSimpleName(), "Failed to close UDP Socket", e);
        }

        // PacketReader unregisters file descriptor from listener on thread with which the Handler
        // constructor argument is associated.
        super.close();
    }
}

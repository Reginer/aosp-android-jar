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

import android.os.Handler;
import android.system.ErrnoException;

import com.android.internal.annotations.VisibleForTesting;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * IkeUdp6WithEncapPortSocket uses an IPv6-bound {@link FileDescriptor} to send and receive IKE
 * packets.
 *
 * <p>IkeUdp6WithEncapPortSocket is usually used when IKE Session has IPv6 address and is required
 * to send message to port 4500, as per MOBIKE spec (RFC 4555).
 *
 * <p>Caller MUST provide one IkeSocketConfig when trying to get an instance of
 * IkeUdp6WithEncapPortSocket. Each IkeSocketConfig will only be bound to by one
 * IkeUdp6WithEncapPortSocket instance. When caller requests an IkeUdp6WithEncapPortSocket with an
 * already bound IkeSocketConfig, the existing instance will be returned.
 */
public final class IkeUdp6WithEncapPortSocket extends IkeUdp6Socket {
    private static final String TAG = IkeUdp6WithEncapPortSocket.class.getSimpleName();

    // Map from IkeSocketConfig to IkeUdp6WithEncapPortSocket instances.
    private static Map<IkeSocketConfig, IkeUdp6WithEncapPortSocket> sConfigToSocketMap =
            new HashMap<>();

    private static IPacketReceiver sPacketReceiver =
            new IkeUdpEncapPortPacketHandler.PacketReceiver();

    private final IkeUdpEncapPortPacketHandler mUdpEncapPortPacketHandler;

    private IkeUdp6WithEncapPortSocket(
            FileDescriptor socket, IkeSocketConfig sockConfig, Handler handler) {
        super(socket, sockConfig, handler);

        mUdpEncapPortPacketHandler = new IkeUdpEncapPortPacketHandler(getFd());
    }

    /**
     * Get an IkeUdp6WithEncapPortSocket instance.
     *
     * <p>Return the existing IkeUdp6WithEncapPortSocket instance if it has been created for the
     * input IkeSocketConfig. Otherwise, create and return a new IkeUdp6WithEncapPortSocket
     * instance.
     *
     * @param sockConfig the socket configuration
     * @param callback the callback for signalling IkeSocket events
     * @param handler the Handler used to process received packets
     * @return an IkeUdp6WithEncapPortSocket instance
     */
    public static IkeUdp6WithEncapPortSocket getIkeUdpEncapSocket(
            IkeSocketConfig sockConfig, IkeSocket.Callback callback, Handler handler)
            throws ErrnoException, IOException {
        IkeUdp6WithEncapPortSocket ikeSocket = sConfigToSocketMap.get(sockConfig);
        if (ikeSocket == null) {
            ikeSocket =
                    new IkeUdp6WithEncapPortSocket(openUdp6Sock(sockConfig), sockConfig, handler);

            // Create and register FileDescriptor for receiving IKE packet on current thread.
            ikeSocket.start();

            sConfigToSocketMap.put(sockConfig, ikeSocket);
        }
        ikeSocket.mRegisteredCallbacks.add(callback);
        return ikeSocket;
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

    @Override
    public void sendIkePacket(byte[] ikePacket, InetAddress serverAddress) {
        mUdpEncapPortPacketHandler.sendIkePacket(ikePacket, serverAddress);
    }

    @Override
    public int getIkeServerPort() {
        return SERVER_PORT_UDP_ENCAPSULATED;
    }

    /** Implement {@link AutoCloseable#close()} */
    @Override
    public void close() {
        sConfigToSocketMap.remove(getIkeSocketConfig());

        super.close();
    }
}

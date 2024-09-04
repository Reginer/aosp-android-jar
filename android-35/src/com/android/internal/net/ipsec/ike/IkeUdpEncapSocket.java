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

import android.net.IpSecManager;
import android.net.IpSecManager.ResourceUnavailableException;
import android.net.IpSecManager.UdpEncapsulationSocket;
import android.os.Handler;
import android.os.Looper;
import android.system.ErrnoException;

import com.android.internal.annotations.VisibleForTesting;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * IkeUdpEncapSocket uses an {@link UdpEncapsulationSocket} to send and receive IKE packets.
 *
 * <p>Caller MUST provide one IkeSocketConfig when trying to get an instance of IkeUdpEncapSocket.
 * Each IkeSocketConfig can only be bound by one IkeUdpEncapSocket instance. When caller requests
 * for IkeUdpEncapSocket with an already bound IkeSocketConfig, an existing instance will be
 * returned.
 */
public final class IkeUdpEncapSocket extends IkeSocket {
    private static final String TAG = "IkeUdpEncapSocket";

    // Map from IkeSocketConfig to IkeSocket instances.
    private static Map<IkeSocketConfig, IkeUdpEncapSocket> sConfigToSocketMap = new HashMap<>();

    private static IPacketReceiver sPacketReceiver =
            new IkeUdpEncapPortPacketHandler.PacketReceiver();

    // UdpEncapsulationSocket for sending and receving IKE packet.
    private final UdpEncapsulationSocket mUdpEncapSocket;

    private final IkeUdpEncapPortPacketHandler mUdpEncapPortPacketHandler;

    private IkeUdpEncapSocket(
            UdpEncapsulationSocket udpEncapSocket, IkeSocketConfig sockConfig, Handler handler) {
        super(sockConfig, handler);
        mUdpEncapSocket = udpEncapSocket;

        mUdpEncapPortPacketHandler = new IkeUdpEncapPortPacketHandler(getFd());
    }

    /**
     * Get an IkeUdpEncapSocket instance.
     *
     * <p>Return the existing IkeUdpEncapSocket instance if it has been created for the input
     * IkeSocketConfig. Otherwise, create and return a new IkeUdpEncapSocket instance.
     *
     * @param sockConfig the socket configuration
     * @param ipsecManager for creating {@link UdpEncapsulationSocket}
     * @param callback the callback for signalling IkeSocket events
     * @return an IkeUdpEncapSocket instance
     */
    public static IkeUdpEncapSocket getIkeUdpEncapSocket(
            IkeSocketConfig sockConfig,
            IpSecManager ipsecManager,
            IkeSocket.Callback callback,
            Looper looper)
            throws ErrnoException, IOException, ResourceUnavailableException {
        IkeUdpEncapSocket ikeSocket = sConfigToSocketMap.get(sockConfig);
        if (ikeSocket == null) {
            UdpEncapsulationSocket udpEncapSocket = ipsecManager.openUdpEncapsulationSocket();
            FileDescriptor fd = udpEncapSocket.getFileDescriptor();
            applySocketConfig(sockConfig, fd, false /* isIpv6 */);

            ikeSocket = new IkeUdpEncapSocket(udpEncapSocket, sockConfig, new Handler(looper));

            // Create and register FileDescriptor for receiving IKE packet on current thread.
            ikeSocket.start();

            sConfigToSocketMap.put(sockConfig, ikeSocket);
        }
        ikeSocket.mRegisteredCallbacks.add(callback);
        return ikeSocket;
    }

    /**
     * Returns the {@link UdpEncapsulationSocket}
     *
     * @return the {@link UdpEncapsulationSocket} for sending and receiving IKE packets.
     */
    public UdpEncapsulationSocket getUdpEncapsulationSocket() {
        return mUdpEncapSocket;
    }

    /** Get FileDescriptor for use with the packet reader polling loop */
    @Override
    protected FileDescriptor getFd() {
        return mUdpEncapSocket.getFileDescriptor(); // Returns cached FD
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

        try {
            mUdpEncapSocket.close();
        } catch (IOException e) {
            getIkeLog()
                    .e(
                            TAG,
                            "Failed to close UDP Encapsulation Socket with Port= "
                                    + mUdpEncapSocket.getPort());
        }

        // PacketReader unregisters file descriptor on thread with which the Handler constructor
        // argument is associated.
        super.close();
    }
}

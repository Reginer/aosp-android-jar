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

import static android.system.OsConstants.AF_INET6;
import static android.system.OsConstants.IPPROTO_UDP;
import static android.system.OsConstants.SOCK_DGRAM;

import android.net.InetAddresses;
import android.net.TrafficStats;
import android.os.Handler;
import android.system.ErrnoException;
import android.system.Os;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * IkeUdp6Socket uses an IPv6-bound {@link FileDescriptor} to send and receive IKE packets.
 *
 * <p>Caller MUST provide one IkeSocketConfig when trying to get an instance of IkeUdp6Socket. Each
 * IkeSocketConfig will only be bound to by one IkeUdp6Socket instance. When caller requests an
 * IkeUdp6Socket with an already bound IkeSocketConfig, the existing instance will be returned.
 */
public class IkeUdp6Socket extends IkeUdpSocket {
    private static final String TAG = IkeUdp6Socket.class.getSimpleName();

    private static final InetAddress INADDR_ANY = InetAddresses.parseNumericAddress("::");

    // Map from IkeSocketConfig to IkeUdp6Socket instances.
    private static Map<IkeSocketConfig, IkeUdp6Socket> sConfigToSocketMap = new HashMap<>();

    protected IkeUdp6Socket(FileDescriptor socket, IkeSocketConfig sockConfig, Handler handler) {
        super(socket, sockConfig, handler == null ? new Handler() : handler);
    }

    /**
     * Get an IkeUdp6Socket instance.
     *
     * <p>Return the existing IkeUdp6Socket instance if it has been created for the input
     * IkeSocketConfig. Otherwise, create and return a new IkeUdp6Socket instance.
     *
     * @param sockConfig the socket configuration
     * @param callback the callback for signalling IkeSocket events
     * @param handler the Handler used to process received packets
     * @return an IkeUdp6Socket instance
     */
    public static IkeUdp6Socket getInstance(
            IkeSocketConfig sockConfig, IkeSocket.Callback callback, Handler handler)
            throws ErrnoException, IOException {
        IkeUdp6Socket ikeSocket = sConfigToSocketMap.get(sockConfig);
        if (ikeSocket == null) {
            ikeSocket = new IkeUdp6Socket(openUdp6Sock(sockConfig), sockConfig, handler);

            // Create and register FileDescriptor for receiving IKE packet on current thread.
            ikeSocket.start();

            sConfigToSocketMap.put(sockConfig, ikeSocket);
        }
        ikeSocket.mRegisteredCallbacks.add(callback);
        return ikeSocket;
    }

    protected static FileDescriptor openUdp6Sock(IkeSocketConfig sockConfig)
            throws ErrnoException, IOException {
        FileDescriptor sock = Os.socket(AF_INET6, SOCK_DGRAM, IPPROTO_UDP);
        TrafficStats.tagFileDescriptor(sock);
        Os.bind(sock, INADDR_ANY, 0);
        applySocketConfig(sockConfig, sock, true /* isIpv6 */);

        return sock;
    }

    /** Implement {@link AutoCloseable#close()} */
    @Override
    public void close() {
        sConfigToSocketMap.remove(getIkeSocketConfig());

        super.close();
    }
}

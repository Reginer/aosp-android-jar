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

import static android.system.OsConstants.AF_INET;
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
 * IkeUdp4Socket uses an IPv4-bound {@link FileDescriptor} to send and receive IKE packets.
 *
 * <p>Caller MUST provide one IkeSocketConfig when trying to get an instance of IkeUdp4Socket. Each
 * IkeSocketConfig will only be bound to by one IkeUdp4Socket instance. When caller requests an
 * IkeUdp4Socket with an already bound IkeSocketConfig, the existing instance will be returned.
 */
public final class IkeUdp4Socket extends IkeUdpSocket {
    private static final String TAG = IkeUdp4Socket.class.getSimpleName();

    private static final InetAddress INADDR_ANY = InetAddresses.parseNumericAddress("0.0.0.0");

    // Map from IkeSocketConfig to IkeUdp4Socket instances.
    private static Map<IkeSocketConfig, IkeUdp4Socket> sConfigToSocketMap = new HashMap<>();

    private IkeUdp4Socket(FileDescriptor socket, IkeSocketConfig sockConfig, Handler handler) {
        super(socket, sockConfig, handler == null ? new Handler() : handler);
    }

    /**
     * Get an IkeUdp4Socket instance.
     *
     * <p>Return the existing IkeUdp4Socket instance if it has been created for the input
     * IkeSocketConfig. Otherwise, create and return a new IkeUdp4Socket instance.
     *
     * @param sockConfig the socket configuration
     * @param callback the callback for signalling IkeSocket events
     * @param handler the Handler used to process received packets
     * @return an IkeUdp4Socket instance
     */
    public static IkeUdp4Socket getInstance(
            IkeSocketConfig sockConfig, IkeSocket.Callback callback, Handler handler)
            throws ErrnoException, IOException {
        IkeUdp4Socket ikeSocket = sConfigToSocketMap.get(sockConfig);
        if (ikeSocket == null) {
            FileDescriptor sock = Os.socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
            TrafficStats.tagFileDescriptor(sock);
            Os.bind(sock, INADDR_ANY, 0);
            applySocketConfig(sockConfig, sock, false /* isIpv6 */);

            ikeSocket = new IkeUdp4Socket(sock, sockConfig, handler);

            // Create and register FileDescriptor for receiving IKE packet on current thread.
            ikeSocket.start();

            sConfigToSocketMap.put(sockConfig, ikeSocket);
        }
        ikeSocket.mRegisteredCallbacks.add(callback);
        return ikeSocket;
    }

    /** Implement {@link AutoCloseable#close()} */
    @Override
    public void close() {
        sConfigToSocketMap.remove(getIkeSocketConfig());

        super.close();
    }
}

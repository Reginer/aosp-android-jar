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

package com.android.internal.net.ipsec.ike.net;

import android.net.Network;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * IkeLocalAddressGenerator generates a local IP address for the given Network using the specified
 * address family, remote address, and server port number.
 */
public class IkeLocalAddressGenerator {
    /** Generate and return a local IP address on the specified Network. */
    public InetAddress generateLocalAddress(
            Network network, boolean isIpv4, InetAddress remoteAddress, int serverPort)
            throws ErrnoException, IOException {
        FileDescriptor sock =
                Os.socket(
                        isIpv4 ? OsConstants.AF_INET : OsConstants.AF_INET6,
                        OsConstants.SOCK_DGRAM,
                        OsConstants.IPPROTO_UDP);
        network.bindSocket(sock);
        Os.connect(sock, remoteAddress, serverPort);
        InetSocketAddress localAddr = (InetSocketAddress) Os.getsockname(sock);
        Os.close(sock);

        return localAddr.getAddress();
    }
}

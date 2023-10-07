/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.net.module.util;

import static android.net.util.SocketUtils.closeSocket;

import android.annotation.NonNull;
import android.system.NetlinkSocketAddress;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.SocketAddress;

/**
 * Collection of utilities to interact with raw sockets.
 *
 * This class also provides utilities to interact with {@link android.net.util.SocketUtils}
 * because it is in the API surface could not be simply move the frameworks/libs/net/
 * to share with module.
 *
 * TODO: deprecate android.net.util.SocketUtils and replace with this class.
 */
public class SocketUtils {

    /**
     * Make a socket address to communicate with netlink.
     */
    @NonNull
    public static SocketAddress makeNetlinkSocketAddress(int portId, int groupsMask) {
        return new NetlinkSocketAddress(portId, groupsMask);
    }

    /**
     * Close a socket, ignoring any exception while closing.
     */
    public static void closeSocketQuietly(FileDescriptor fd) {
        try {
            closeSocket(fd);
        } catch (IOException ignored) {
        }
    }

    private SocketUtils() {}
}

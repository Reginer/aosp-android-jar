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

package com.android.net.module.util;


import android.annotation.Nullable;

import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * Various utilities used in connectivity code.
 * @hide
 */
public final class ConnectivityUtils {
    private ConnectivityUtils() {}


    /**
     * Return IP address and port in a string format.
     */
    public static String addressAndPortToString(InetAddress address, int port) {
        return String.format(
                (address instanceof Inet6Address) ? "[%s]:%d" : "%s:%d",
                        address.getHostAddress(), port);
    }

    /**
     * Return true if the provided address is non-null and an IPv6 Unique Local Address (RFC4193).
     */
    public static boolean isIPv6ULA(@Nullable InetAddress addr) {
        return addr instanceof Inet6Address
                && ((addr.getAddress()[0] & 0xfe) == 0xfc);
    }

    /**
     * Returns the {@code int} nearest in value to {@code value}.
     *
     * @param value any {@code long} value
     * @return the same value cast to {@code int} if it is in the range of the {@code int}
     * type, {@link Integer#MAX_VALUE} if it is too large, or {@link Integer#MIN_VALUE} if
     * it is too small
     */
    public static int saturatedCast(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }
}

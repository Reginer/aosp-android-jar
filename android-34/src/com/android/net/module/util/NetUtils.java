/*
 * Copyright 2019 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.RouteInfo;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

/**
 * Collection of network common utilities.
 * @hide
 */
public final class NetUtils {

    /**
     * Check if IP address type is consistent between two InetAddress.
     * @return true if both are the same type. False otherwise.
     */
    public static boolean addressTypeMatches(@NonNull InetAddress left,
            @NonNull InetAddress right) {
        return (((left instanceof Inet4Address) && (right instanceof Inet4Address))
                || ((left instanceof Inet6Address) && (right instanceof Inet6Address)));
    }

    /**
     * Find the route from a Collection of routes that best matches a given address.
     * May return null if no routes are applicable, or the best route is not a route of
     * {@link RouteInfo.RTN_UNICAST} type.
     *
     * @param routes a Collection of RouteInfos to chose from
     * @param dest the InetAddress your trying to get to
     * @return the RouteInfo from the Collection that best fits the given address
     */
    @Nullable
    public static RouteInfo selectBestRoute(@Nullable Collection<RouteInfo> routes,
            @Nullable InetAddress dest) {
        if ((routes == null) || (dest == null)) return null;

        RouteInfo bestRoute = null;

        // pick a longest prefix match under same address type
        for (RouteInfo route : routes) {
            if (addressTypeMatches(route.getDestination().getAddress(), dest)) {
                if ((bestRoute != null)
                        && (bestRoute.getDestination().getPrefixLength()
                        >= route.getDestination().getPrefixLength())) {
                    continue;
                }
                if (route.matches(dest)) bestRoute = route;
            }
        }

        if (bestRoute != null && bestRoute.getType() == RouteInfo.RTN_UNICAST) {
            return bestRoute;
        } else {
            return null;
        }
    }

    /**
     * Get InetAddress masked with prefixLength.  Will never return null.
     * @param address the IP address to mask with
     * @param prefixLength the prefixLength used to mask the IP
     */
    public static InetAddress getNetworkPart(InetAddress address, int prefixLength) {
        byte[] array = address.getAddress();
        maskRawAddress(array, prefixLength);

        InetAddress netPart = null;
        try {
            netPart = InetAddress.getByAddress(array);
        } catch (UnknownHostException e) {
            throw new RuntimeException("getNetworkPart error - " + e.toString());
        }
        return netPart;
    }

    /**
     *  Masks a raw IP address byte array with the specified prefix length.
     */
    public static void maskRawAddress(byte[] array, int prefixLength) {
        if (prefixLength < 0 || prefixLength > array.length * 8) {
            throw new RuntimeException("IP address with " + array.length
                    + " bytes has invalid prefix length " + prefixLength);
        }

        int offset = prefixLength / 8;
        int remainder = prefixLength % 8;
        byte mask = (byte) (0xFF << (8 - remainder));

        if (offset < array.length) array[offset] = (byte) (array[offset] & mask);

        offset++;

        for (; offset < array.length; offset++) {
            array[offset] = 0;
        }
    }
}

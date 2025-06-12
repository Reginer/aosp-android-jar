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

import android.annotation.NonNull;
import android.os.Parcel;
import android.util.Log;


import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Collection of utilities to interact with {@link InetAddress}
 * @hide
 */
public class InetAddressUtils {

    private static final String TAG = InetAddressUtils.class.getSimpleName();
    private static final int INET4_ADDR_LENGTH = 4;
    private static final int INET6_ADDR_LENGTH = 16;

    /**
     * Writes an InetAddress to a parcel. The address may be null. This is likely faster than
     * calling writeSerializable.
     * @hide
     */
    public static void parcelInetAddress(Parcel parcel, InetAddress address, int flags) {
        byte[] addressArray = (address != null) ? address.getAddress() : null;
        parcel.writeByteArray(addressArray);
        if (address instanceof Inet6Address) {
            final Inet6Address v6Address = (Inet6Address) address;
            final boolean hasScopeId = v6Address.getScopeId() != 0;
            parcel.writeBoolean(hasScopeId);
            if (hasScopeId) parcel.writeInt(v6Address.getScopeId());
        }

    }

    /**
     * Reads an InetAddress from a parcel. Returns null if the address that was written was null
     * or if the data is invalid.
     * @hide
     */
    public static InetAddress unparcelInetAddress(Parcel in) {
        byte[] addressArray = in.createByteArray();
        if (addressArray == null) {
            return null;
        }

        try {
            if (addressArray.length == INET6_ADDR_LENGTH) {
                final boolean hasScopeId = in.readBoolean();
                final int scopeId = hasScopeId ? in.readInt() : 0;
                return Inet6Address.getByAddress(null /* host */, addressArray, scopeId);
            }

            return InetAddress.getByAddress(addressArray);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Create a Inet6Address with scope id if it is a link local address. Otherwise, returns the
     * original address.
     */
    public static Inet6Address withScopeId(@NonNull final Inet6Address addr, int scopeid) {
        if (!addr.isLinkLocalAddress()) {
            return addr;
        }
        try {
            return Inet6Address.getByAddress(null /* host */, addr.getAddress(),
                    scopeid);
        } catch (UnknownHostException impossible) {
            Log.wtf(TAG, "Cannot construct scoped Inet6Address with Inet6Address.getAddress("
                    + addr.getHostAddress() + "): ", impossible);
            return null;
        }
    }

    /**
     * Create a v4-mapped v6 address from v4 address
     *
     * @param v4Addr Inet4Address which is converted to v4-mapped v6 address
     * @return v4-mapped v6 address
     */
    public static Inet6Address v4MappedV6Address(@NonNull final Inet4Address v4Addr) {
        final byte[] v6AddrBytes = new byte[INET6_ADDR_LENGTH];
        v6AddrBytes[10] = (byte) 0xFF;
        v6AddrBytes[11] = (byte) 0xFF;
        System.arraycopy(v4Addr.getAddress(), 0 /* srcPos */, v6AddrBytes, 12 /* dstPos */,
                INET4_ADDR_LENGTH);
        try {
            // Using Inet6Address.getByAddress since InetAddress.getByAddress converts v4-mapped v6
            // address to v4 address internally and returns Inet4Address
            return Inet6Address.getByAddress(null /* host */, v6AddrBytes, -1 /* scope_id */);
        } catch (UnknownHostException impossible) {
            // getByAddress throws UnknownHostException when the argument is the invalid length
            // but INET6_ADDR_LENGTH(16) is the valid length.
            Log.wtf(TAG, "Failed to generate v4-mapped v6 address from " + v4Addr, impossible);
            return null;
        }
    }

    private InetAddressUtils() {}
}

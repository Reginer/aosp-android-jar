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
import android.net.MacAddress;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 * Collection of MAC address utilities.
 * @hide
 */
public final class MacAddressUtils {

    private static final long VALID_LONG_MASK = (1L << 48) - 1;
    private static final long LOCALLY_ASSIGNED_MASK = longAddrFromByteAddr(
            MacAddress.fromString("2:0:0:0:0:0").toByteArray());
    private static final long MULTICAST_MASK = longAddrFromByteAddr(
            MacAddress.fromString("1:0:0:0:0:0").toByteArray());
    private static final long OUI_MASK = longAddrFromByteAddr(
            MacAddress.fromString("ff:ff:ff:0:0:0").toByteArray());
    private static final long NIC_MASK = longAddrFromByteAddr(
            MacAddress.fromString("0:0:0:ff:ff:ff").toByteArray());
    // Matches WifiInfo.DEFAULT_MAC_ADDRESS
    private static final MacAddress DEFAULT_MAC_ADDRESS =
            MacAddress.fromString("02:00:00:00:00:00");
    private static final int ETHER_ADDR_LEN = 6;

    /**
     * @return true if this MacAddress is a multicast address.
     */
    public static boolean isMulticastAddress(@NonNull MacAddress address) {
        return (longAddrFromByteAddr(address.toByteArray()) & MULTICAST_MASK) != 0;
    }

    /**
     * Returns a generated MAC address whose 46 bits, excluding the locally assigned bit and the
     * unicast bit, are randomly selected.
     *
     * The locally assigned bit is always set to 1. The multicast bit is always set to 0.
     *
     * @return a random locally assigned, unicast MacAddress.
     */
    public static @NonNull MacAddress createRandomUnicastAddress() {
        return createRandomUnicastAddress(null, new SecureRandom());
    }

    /**
     * Returns a randomly generated MAC address using the given Random object and the same
     * OUI values as the given MacAddress.
     *
     * The locally assigned bit is always set to 1. The multicast bit is always set to 0.
     *
     * @param base a base MacAddress whose OUI is used for generating the random address.
     *             If base == null then the OUI will also be randomized.
     * @param r a standard Java Random object used for generating the random address.
     * @return a random locally assigned MacAddress.
     */
    public static @NonNull MacAddress createRandomUnicastAddress(@Nullable MacAddress base,
            @NonNull Random r) {
        long addr;

        if (base == null) {
            addr = r.nextLong() & VALID_LONG_MASK;
        } else {
            addr = (longAddrFromByteAddr(base.toByteArray()) & OUI_MASK)
                    | (NIC_MASK & r.nextLong());
        }
        addr |= LOCALLY_ASSIGNED_MASK;
        addr &= ~MULTICAST_MASK;
        MacAddress mac = MacAddress.fromBytes(byteAddrFromLongAddr(addr));
        if (mac.equals(DEFAULT_MAC_ADDRESS)) {
            return createRandomUnicastAddress(base, r);
        }
        return mac;
    }

    /**
     * Convert a byte address to long address.
     */
    public static long longAddrFromByteAddr(byte[] addr) {
        Objects.requireNonNull(addr);
        if (!isMacAddress(addr)) {
            throw new IllegalArgumentException(
                    Arrays.toString(addr) + " was not a valid MAC address");
        }
        long longAddr = 0;
        for (byte b : addr) {
            final int uint8Byte = b & 0xff;
            longAddr = (longAddr << 8) + uint8Byte;
        }
        return longAddr;
    }

    /**
     * Convert a long address to byte address.
     */
    public static byte[] byteAddrFromLongAddr(long addr) {
        byte[] bytes = new byte[ETHER_ADDR_LEN];
        int index = ETHER_ADDR_LEN;
        while (index-- > 0) {
            bytes[index] = (byte) addr;
            addr = addr >> 8;
        }
        return bytes;
    }

    /**
     * Returns true if the given byte array is a valid MAC address.
     * A valid byte array representation for a MacAddress is a non-null array of length 6.
     *
     * @param addr a byte array.
     * @return true if the given byte array is not null and has the length of a MAC address.
     *
     */
    public static boolean isMacAddress(byte[] addr) {
        return addr != null && addr.length == ETHER_ADDR_LEN;
    }
}

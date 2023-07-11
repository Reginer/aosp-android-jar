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

import static android.net.NetworkCapabilities.NET_CAPABILITY_BIP;
import static android.net.NetworkCapabilities.NET_CAPABILITY_CBS;
import static android.net.NetworkCapabilities.NET_CAPABILITY_DUN;
import static android.net.NetworkCapabilities.NET_CAPABILITY_EIMS;
import static android.net.NetworkCapabilities.NET_CAPABILITY_ENTERPRISE;
import static android.net.NetworkCapabilities.NET_CAPABILITY_FOTA;
import static android.net.NetworkCapabilities.NET_CAPABILITY_IA;
import static android.net.NetworkCapabilities.NET_CAPABILITY_IMS;
import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.net.NetworkCapabilities.NET_CAPABILITY_MCX;
import static android.net.NetworkCapabilities.NET_CAPABILITY_MMS;
import static android.net.NetworkCapabilities.NET_CAPABILITY_MMTEL;
import static android.net.NetworkCapabilities.NET_CAPABILITY_OEM_PAID;
import static android.net.NetworkCapabilities.NET_CAPABILITY_OEM_PRIVATE;
import static android.net.NetworkCapabilities.NET_CAPABILITY_RCS;
import static android.net.NetworkCapabilities.NET_CAPABILITY_SUPL;
import static android.net.NetworkCapabilities.NET_CAPABILITY_VEHICLE_INTERNAL;
import static android.net.NetworkCapabilities.NET_CAPABILITY_VSIM;
import static android.net.NetworkCapabilities.NET_CAPABILITY_WIFI_P2P;
import static android.net.NetworkCapabilities.NET_CAPABILITY_XCAP;
import static android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_ETHERNET;
import static android.net.NetworkCapabilities.TRANSPORT_USB;
import static android.net.NetworkCapabilities.TRANSPORT_VPN;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI_AWARE;

import android.annotation.NonNull;
import android.net.NetworkCapabilities;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Utilities to examine {@link android.net.NetworkCapabilities}.
 * @hide
 */
public final class NetworkCapabilitiesUtils {
    // Transports considered to classify networks in UI, in order of which transport should be
    // surfaced when there are multiple transports. Transports not in this list do not have
    // an ordering preference (in practice they will have a deterministic order based on the
    // transport int itself).
    private static final int[] DISPLAY_TRANSPORT_PRIORITIES = new int[] {
        // Users think of their VPNs as VPNs, not as any of the underlying nets
        TRANSPORT_VPN,
        // If the network has cell, prefer showing that because it's usually metered.
        TRANSPORT_CELLULAR,
        // If the network has WiFi aware, prefer showing that as it's a more specific use case.
        // Ethernet can masquerade as other transports, where the device uses ethernet to connect to
        // a box providing cell or wifi. Today this is represented by only the masqueraded type for
        // backward compatibility, but these networks should morally have Ethernet & the masqueraded
        // type. Because of this, prefer other transports instead of Ethernet.
        TRANSPORT_WIFI_AWARE,
        TRANSPORT_BLUETOOTH,
        TRANSPORT_WIFI,
        TRANSPORT_ETHERNET,
        TRANSPORT_USB

        // Notably, TRANSPORT_TEST is not in this list as any network that has TRANSPORT_TEST and
        // one of the above transports should be counted as that transport, to keep tests as
        // realistic as possible.
    };

    /**
     * Capabilities that suggest that a network is restricted.
     * See {@code NetworkCapabilities#maybeMarkCapabilitiesRestricted},
      * and {@code FORCE_RESTRICTED_CAPABILITIES}.
     */
    @VisibleForTesting
    public static final long RESTRICTED_CAPABILITIES = packBitList(
            NET_CAPABILITY_BIP,
            NET_CAPABILITY_CBS,
            NET_CAPABILITY_DUN,
            NET_CAPABILITY_EIMS,
            NET_CAPABILITY_ENTERPRISE,
            NET_CAPABILITY_FOTA,
            NET_CAPABILITY_IA,
            NET_CAPABILITY_IMS,
            NET_CAPABILITY_MCX,
            NET_CAPABILITY_RCS,
            NET_CAPABILITY_VEHICLE_INTERNAL,
            NET_CAPABILITY_VSIM,
            NET_CAPABILITY_XCAP,
            NET_CAPABILITY_MMTEL);

    /**
     * Capabilities that force network to be restricted.
     * See {@code NetworkCapabilities#maybeMarkCapabilitiesRestricted}.
     */
    private static final long FORCE_RESTRICTED_CAPABILITIES = packBitList(
            NET_CAPABILITY_ENTERPRISE,
            NET_CAPABILITY_OEM_PAID,
            NET_CAPABILITY_OEM_PRIVATE);

    /**
     * Capabilities that suggest that a network is unrestricted.
     * See {@code NetworkCapabilities#maybeMarkCapabilitiesRestricted}.
     */
    @VisibleForTesting
    public static final long UNRESTRICTED_CAPABILITIES = packBitList(
            NET_CAPABILITY_INTERNET,
            NET_CAPABILITY_MMS,
            NET_CAPABILITY_SUPL,
            NET_CAPABILITY_WIFI_P2P);

    /**
     * Get a transport that can be used to classify a network when displaying its info to users.
     *
     * While networks can have multiple transports, users generally think of them as "wifi",
     * "mobile data", "vpn" and expect them to be classified as such in UI such as settings.
     * @param transports Non-empty array of transports on a network
     * @return A single transport
     * @throws IllegalArgumentException The array is empty
     */
    public static int getDisplayTransport(@NonNull int[] transports) {
        for (int transport : DISPLAY_TRANSPORT_PRIORITIES) {
            if (CollectionUtils.contains(transports, transport)) {
                return transport;
            }
        }

        if (transports.length < 1) {
            // All NetworkCapabilities representing a network have at least one transport, so an
            // empty transport array would be created by the caller instead of extracted from
            // NetworkCapabilities.
            throw new IllegalArgumentException("No transport in the provided array");
        }
        return transports[0];
    }


    /**
     * Infers that all the capabilities it provides are typically provided by restricted networks
     * or not.
     *
     * @param nc the {@link NetworkCapabilities} to infer the restricted capabilities.
     *
     * @return {@code true} if the network should be restricted.
     */
    // TODO: Use packBits(nc.getCapabilities()) to check more easily using bit masks.
    public static boolean inferRestrictedCapability(NetworkCapabilities nc) {
        // Check if we have any capability that forces the network to be restricted.
        for (int capability : unpackBits(FORCE_RESTRICTED_CAPABILITIES)) {
            if (nc.hasCapability(capability)) {
                return true;
            }
        }

        // Verify there aren't any unrestricted capabilities.  If there are we say
        // the whole thing is unrestricted unless it is forced to be restricted.
        for (int capability : unpackBits(UNRESTRICTED_CAPABILITIES)) {
            if (nc.hasCapability(capability)) {
                return false;
            }
        }

        // Must have at least some restricted capabilities.
        for (int capability : unpackBits(RESTRICTED_CAPABILITIES)) {
            if (nc.hasCapability(capability)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unpacks long value into an array of bits.
     */
    public static int[] unpackBits(long val) {
        int size = Long.bitCount(val);
        int[] result = new int[size];
        int index = 0;
        int bitPos = 0;
        while (val != 0) {
            if ((val & 1) == 1) result[index++] = bitPos;
            val = val >>> 1;
            bitPos++;
        }
        return result;
    }

    /**
     * Packs a list of ints in the same way as packBits()
     *
     * Each passed int is the rank of a bit that should be set in the returned long.
     * Example : passing (1,3) will return in 0b00001010 and passing (5,6,0) will return 0b01100001
     *
     * @param bits bits to pack
     * @return a long with the specified bits set.
     */
    public static long packBitList(int... bits) {
        return packBits(bits);
    }

    /**
     * Packs array of bits into a long value.
     *
     * Each passed int is the rank of a bit that should be set in the returned long.
     * Example : passing [1,3] will return in 0b00001010 and passing [5,6,0] will return 0b01100001
     *
     * @param bits bits to pack
     * @return a long with the specified bits set.
     */
    public static long packBits(int[] bits) {
        long packed = 0;
        for (int b : bits) {
            packed |= (1L << b);
        }
        return packed;
    }
}

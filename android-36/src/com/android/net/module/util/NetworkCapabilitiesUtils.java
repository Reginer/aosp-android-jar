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
import static android.net.NetworkCapabilities.TRANSPORT_SATELLITE;
import static android.net.NetworkCapabilities.TRANSPORT_USB;
import static android.net.NetworkCapabilities.TRANSPORT_VPN;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI_AWARE;

import static com.android.net.module.util.BitUtils.packBits;
import static com.android.net.module.util.BitUtils.unpackBits;

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
        TRANSPORT_USB,
        TRANSPORT_SATELLITE
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
    public static final long RESTRICTED_CAPABILITIES =
            (1L << NET_CAPABILITY_BIP) |
            (1L << NET_CAPABILITY_CBS) |
            (1L << NET_CAPABILITY_DUN) |
            (1L << NET_CAPABILITY_EIMS) |
            (1L << NET_CAPABILITY_ENTERPRISE) |
            (1L << NET_CAPABILITY_FOTA) |
            (1L << NET_CAPABILITY_IA) |
            (1L << NET_CAPABILITY_IMS) |
            (1L << NET_CAPABILITY_MCX) |
            (1L << NET_CAPABILITY_RCS) |
            (1L << NET_CAPABILITY_VEHICLE_INTERNAL) |
            (1L << NET_CAPABILITY_VSIM) |
            (1L << NET_CAPABILITY_XCAP) |
            (1L << NET_CAPABILITY_MMTEL);

    /**
     * Capabilities that force network to be restricted.
     * See {@code NetworkCapabilities#maybeMarkCapabilitiesRestricted}.
     */
    private static final long FORCE_RESTRICTED_CAPABILITIES =
            (1L << NET_CAPABILITY_ENTERPRISE) |
            (1L << NET_CAPABILITY_OEM_PAID) |
            (1L << NET_CAPABILITY_OEM_PRIVATE);

    /**
     * Capabilities that suggest that a network is unrestricted.
     * See {@code NetworkCapabilities#maybeMarkCapabilitiesRestricted}.
     */
    @VisibleForTesting
    public static final long UNRESTRICTED_CAPABILITIES =
            (1L << NET_CAPABILITY_INTERNET) |
            (1L << NET_CAPABILITY_MMS) |
            (1L << NET_CAPABILITY_SUPL) |
            (1L << NET_CAPABILITY_WIFI_P2P);

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
    public static boolean inferRestrictedCapability(NetworkCapabilities nc) {
        return inferRestrictedCapability(packBits(nc.getCapabilities()));
    }

    /**
     * Infers that all the capabilities it provides are typically provided by restricted networks
     * or not.
     *
     * @param capabilities see {@link NetworkCapabilities#getCapabilities()}
     *
     * @return {@code true} if the network should be restricted.
     */
    public static boolean inferRestrictedCapability(long capabilities) {
        // Check if we have any capability that forces the network to be restricted.
        if ((capabilities & FORCE_RESTRICTED_CAPABILITIES) != 0) {
            return true;
        }

        // Verify there aren't any unrestricted capabilities.  If there are we say
        // the whole thing is unrestricted unless it is forced to be restricted.
        if ((capabilities & UNRESTRICTED_CAPABILITIES) != 0) {
            return false;
        }

        // Must have at least some restricted capabilities.
        if ((capabilities & RESTRICTED_CAPABILITIES) != 0) {
            return true;
        }
        return false;
    }

}

/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.net.util;

import android.annotation.NonNull;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

/**
 * Collection of utilities for socket keepalive offload.
 *
 * @hide
 */
public final class KeepaliveUtils {

    public static final String TAG = "KeepaliveUtils";

    /**
     * Read supported keepalive count for each transport type from overlay resource. This should be
     * used to create a local variable store of resource customization, and use it as the input for
     * {@link #getSupportedKeepalivesForNetworkCapabilities}.
     *
     * @param context The context to read resource from.
     * @return An array of supported keepalive count for each transport type.
     * @deprecated This is used by CTS 13, but can be removed after switching it to
     * {@link ConnectivityManager#getSupportedKeepalives()}.
     */
    @NonNull
    @Deprecated
    public static int[] getSupportedKeepalives(@NonNull Context context) {
        return context.getSystemService(ConnectivityManager.class).getSupportedKeepalives();
    }

    /**
     * Get supported keepalive count for the given {@link NetworkCapabilities}.
     *
     * @param supportedKeepalives An array of supported keepalive count for each transport type.
     * @param nc The {@link NetworkCapabilities} of the network the socket keepalive is on.
     *
     * @return Supported keepalive count for the given {@link NetworkCapabilities}.
     */
    public static int getSupportedKeepalivesForNetworkCapabilities(
            @NonNull int[] supportedKeepalives, @NonNull NetworkCapabilities nc) {
        final int[] transports = nc.getTransportTypes();
        if (transports.length == 0) return 0;
        int supportedCount = supportedKeepalives[transports[0]];
        // Iterate through transports and return minimum supported value.
        for (final int transport : transports) {
            if (supportedCount > supportedKeepalives[transport]) {
                supportedCount = supportedKeepalives[transport];
            }
        }
        return supportedCount;
    }
}

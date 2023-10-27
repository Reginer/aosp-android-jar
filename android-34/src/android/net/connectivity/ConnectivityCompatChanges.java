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
package android.net.connectivity;

import android.compat.annotation.ChangeId;
import android.compat.annotation.EnabledAfter;
import android.compat.annotation.EnabledSince;
import android.os.Build;

/**
 * The class contains all CompatChanges for the Connectivity module.
 *
 * <p>This is the centralized place for the CompatChanges used in the Connectivity module.
 * Putting all the CompatChanges in single place makes it possible to manage them under a single
 * platform_compat_config.
 * @hide
 */
public final class ConnectivityCompatChanges {

    /**
     * The {@link android.net.LinkProperties#getRoutes()} now can contain excluded as well as
     * included routes. Use {@link android.net.RouteInfo#getType()} to determine route type.
     *
     * @hide
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = Build.VERSION_CODES.S_V2)
    public static final long EXCLUDED_ROUTES = 186082280;

    /**
     * When enabled, apps targeting < Android 12 are considered legacy for
     * the NSD native daemon.
     * The platform will only keep the daemon running as long as there are
     * any legacy apps connected.
     *
     * After Android 12, direct communication with the native daemon might not work since the native
     * daemon won't always stay alive. Using the NSD APIs from NsdManager as the replacement is
     * recommended.
     * Another alternative could be bundling your own mdns solutions instead of
     * depending on the system mdns native daemon.
     *
     * This compatibility change applies to Android 13 and later only. To toggle behavior on
     * Android 12 and Android 12L, use RUN_NATIVE_NSD_ONLY_IF_LEGACY_APPS.
     *
     * @hide
     */
    @ChangeId
    @EnabledSince(targetSdkVersion = android.os.Build.VERSION_CODES.S)
    // This was a platform change ID with value 191844585L before T
    public static final long RUN_NATIVE_NSD_ONLY_IF_LEGACY_APPS_T_AND_LATER = 235355681L;

    /**
     * The self certified capabilities check should be enabled after android 13.
     *
     * <p> See {@link android.net.NetworkCapabilities} for more details.
     *
     * @hide
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = Build.VERSION_CODES.TIRAMISU)
    public static final long ENABLE_SELF_CERTIFIED_CAPABILITIES_DECLARATION = 266524688;

    /**
     * Apps targeting < Android 14 use a legacy NSD backend.
     *
     * The legacy apps use a legacy native daemon as NsdManager backend, but other apps use a
     * platform-integrated mDNS implementation as backend.
     *
     * @hide
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = Build.VERSION_CODES.TIRAMISU)
    public static final long ENABLE_PLATFORM_MDNS_BACKEND = 270306772L;
    private ConnectivityCompatChanges() {
    }
}

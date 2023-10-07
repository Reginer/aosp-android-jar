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
 * limitations under the License
 */

package com.android.libraries.tv.tvsystem.wifi;

import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;

import java.util.concurrent.Executor;

/**
 * Provides access to APIs in {@link WifiManager} that are otherwise @hidden.
 *
 * @hide
 */
@SystemApi
public final class TvWifiManager {
    private final WifiManager mWifiManager;

    public TvWifiManager(@NonNull Context context) {
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    /**
     * Starts a local-only hotspot with a specific configuration applied. See
     * {@link WifiManager#startLocalOnlyHotspot(WifiManager.LocalOnlyHotspotCallback, Handler)}.
     *
     * Applications need either {@link android.Manifest.permission#NETWORK_SETUP_WIZARD} or
     * {@link android.Manifest.permission#NETWORK_SETTINGS} to call this method.
     *
     * Since custom configuration settings may be incompatible with each other, the hotspot started
     * through this method cannot coexist with another hotspot created through
     * startLocalOnlyHotspot. If this is attempted, the first hotspot request wins and others
     * receive {@link WifiManager.LocalOnlyHotspotCallback#ERROR_GENERIC} through
     * {@link WifiManager.LocalOnlyHotspotCallback#onFailed}.
     *
     * @param config Custom configuration for the hotspot. See {@link SoftApConfiguration}.
     * @param executor Executor to run callback methods on, or null to use the main thread.
     * @param callback Callback object for updates about hotspot status, or null for no updates.
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD})
    public void startLocalOnlyHotspot(@NonNull SoftApConfiguration config,
            @Nullable Executor executor,
            @Nullable WifiManager.LocalOnlyHotspotCallback callback) {
      android.net.wifi.SoftApConfiguration.Builder frameworkConfig =
            new android.net.wifi.SoftApConfiguration.Builder()
                .setBssid(config.getBssid())
                .setSsid(config.getSsid())
                .setPassphrase(config.getWpa2Passphrase(), SECURITY_TYPE_WPA2_PSK);
      if (config.getBssid() != null) {
          frameworkConfig.setMacRandomizationSetting(
                  android.net.wifi.SoftApConfiguration.RANDOMIZATION_NONE);
      }
      mWifiManager.startLocalOnlyHotspot(frameworkConfig.build(), executor, callback);
    }
}

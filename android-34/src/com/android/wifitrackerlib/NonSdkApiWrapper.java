/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.wifitrackerlib;

import android.app.admin.DevicePolicyManager;
import android.app.admin.WifiSsidPolicy;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Wrapper class to decouple WifiTrackerLibDefaults from non-SDK API usage at build time, for use
 * by apps that build against the SDK (e.g. SetupWizard).
 *
 * Clients may wish to provide their own implementation of these methods when copying this
 * library over to their own codebase.
 */
class NonSdkApiWrapper {
    private NonSdkApiWrapper() {
        // Empty constructor to make this class non-instantiable.
    }

    /**
     * Starts the System captive portal app.
     */
    static void startCaptivePortalApp(
            @NonNull ConnectivityManager connectivityManager, @NonNull Network network) {
        return;
    }

    /**
     * Find the annotation of specified id in rawText and linkify it with helpUriString.
     */
    static CharSequence linkifyAnnotation(Context context, CharSequence rawText, String id,
            String helpUriString) {
        // TODO(b/162368129): Should SUW be concerned with annotation linking?
        return rawText;
    }

    /**
     * Tries to get WifiInfo from network capabilities if it is VCN-over-Wifi.
     */
    static WifiInfo getVcnWifiInfo(@NonNull NetworkCapabilities networkCapabilities) {
        // This is only useful for treating CELLULAR over WIFI as a carrier merged network in
        // provider model Settings. Since SUW doesn't use the provider model, this is not used.
        return null;
    }

    /**
     * Returns whether or not the device is in retail demo mode.
     */
    static boolean isDemoMode(@NonNull Context context) {
        // This should be false since SUW is not used in demo mode.
        return false;
    }

    /**
     * Returns true if the WifiInfo is for the primary network, false otherwise.
     */
    static boolean isPrimary(@NonNull WifiInfo wifiInfo) {
        // TODO(b/230643853): WifiInfo.isPrimary() currently requires NETWORK_SETTINGS permission to
        //                    access, which SUW does not hold. Always return true (WifiTracker1
        //                    behavior) until SUW can access this field.
        return true;
    }

    /**
     * Returns true if the NetworkCapabilities is OEM_PAID or OEM_PRIVATE
     */
    static boolean isOemCapabilities(@NonNull NetworkCapabilities capabilities) {
        // SUW can't access NET_CAPABILITY_OEM_PAID or NET_CAPABILITY_OEM_PRIVATE since they aren't
        // public APIs. Return false here since we don't need to worry about secondary OEM networks
        // in SUW for now.
        return false;
    }

    /**
     * Returns the {@link WifiSsidPolicy} of the device.
     */
    @Nullable
    static WifiSsidPolicy getWifiSsidPolicy(@NonNull DevicePolicyManager devicePolicyManager) {
        // Return null since SUW does not have QUERY_ADMIN_POLICY permission.
        return null;
    }
}

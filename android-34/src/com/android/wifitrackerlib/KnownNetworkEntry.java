/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.os.Build.VERSION_CODES;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.sharedconnectivity.app.KnownNetwork;
import android.net.wifi.sharedconnectivity.app.KnownNetworkConnectionStatus;
import android.net.wifi.sharedconnectivity.app.SharedConnectivityManager;
import android.os.Handler;
import android.text.BidiFormatter;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;

/**
 * WifiEntry representation of a Known Network provided via {@link SharedConnectivityManager}.
 */
@TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
public class KnownNetworkEntry extends StandardWifiEntry{
    static final String TAG = "KnownNetworkEntry";

    @Nullable private final SharedConnectivityManager mSharedConnectivityManager;
    @NonNull private final KnownNetwork mKnownNetworkData;

    /**
     * If editing this IntDef also edit the definition in:
     * {@link android.net.wifi.sharedconnectivity.app.KnownNetworkConnectionStatus}
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            KnownNetworkConnectionStatus.CONNECTION_STATUS_UNKNOWN,
            KnownNetworkConnectionStatus.CONNECTION_STATUS_SAVED,
            KnownNetworkConnectionStatus.CONNECTION_STATUS_SAVE_FAILED,
    })
    public @interface ConnectionStatus {} // TODO(b/271868642): Add IfThisThanThat lint

    KnownNetworkEntry(
            @NonNull WifiTrackerInjector injector, @NonNull Context context,
            @NonNull Handler callbackHandler, @NonNull StandardWifiEntryKey key,
            @NonNull WifiManager wifiManager,
            @Nullable SharedConnectivityManager sharedConnectivityManager,
            @NonNull KnownNetwork knownNetworkData) {
        super(injector, context, callbackHandler, key, wifiManager,
                false /* forSavedNetworksPage */);
        mSharedConnectivityManager = sharedConnectivityManager;
        mKnownNetworkData = knownNetworkData;
    }

    KnownNetworkEntry(
            @NonNull WifiTrackerInjector injector, @NonNull Context context,
            @NonNull Handler callbackHandler, @NonNull StandardWifiEntryKey key,
            @Nullable List<WifiConfiguration> configs, @Nullable List<ScanResult> scanResults,
            @NonNull WifiManager wifiManager,
            @Nullable SharedConnectivityManager sharedConnectivityManager,
            @NonNull KnownNetwork knownNetworkData) throws IllegalArgumentException {
        super(injector, context, callbackHandler, key, configs, scanResults, wifiManager,
                false /* forSavedNetworksPage */);
        mSharedConnectivityManager = sharedConnectivityManager;
        mKnownNetworkData = knownNetworkData;
    }

    @Override
    public synchronized String getSummary(boolean concise) {
        return mContext.getString(R.string.wifitrackerlib_known_network_summary,
                BidiFormatter.getInstance().unicodeWrap(
                        mKnownNetworkData.getNetworkProviderInfo().getDeviceName()));
    }

    @Override
    public synchronized void connect(@Nullable ConnectCallback callback) {
        mConnectCallback = callback;
        if (mSharedConnectivityManager == null) {
            if (callback != null) {
                mCallbackHandler.post(() -> callback.onConnectResult(
                        ConnectCallback.CONNECT_STATUS_FAILURE_UNKNOWN));
            }
            return;
        }
        mSharedConnectivityManager.connectKnownNetwork(mKnownNetworkData);
    }

    @Override
    public synchronized boolean isSaved() {
        return false;
    }

    @Override
    public synchronized boolean isSuggestion() {
        return false;
    }

    @WorkerThread
    protected synchronized boolean connectionInfoMatches(@NonNull WifiInfo wifiInfo) {
        if (wifiInfo.isPasspointAp() || wifiInfo.isOsuAp()) {
            return false;
        }
        return Objects.equals(getStandardWifiEntryKey().getScanResultKey(),
                ssidAndSecurityTypeToStandardWifiEntryKey(WifiInfo.sanitizeSsid(wifiInfo.getSSID()),
                        wifiInfo.getCurrentSecurityType()).getScanResultKey());
    }

    /**
     * Trigger ConnectCallback with data from SharedConnectivityService.
     * @param status KnownNetworkConnectionStatus#ConnectionStatus enum.
     */
    public void onConnectionStatusChanged(@ConnectionStatus int status) {
        if (status == KnownNetworkConnectionStatus.CONNECTION_STATUS_SAVE_FAILED
                && mConnectCallback != null) {
            mCallbackHandler.post(() -> mConnectCallback.onConnectResult(
                    ConnectCallback.CONNECT_STATUS_FAILURE_UNKNOWN));
        }
    }
}

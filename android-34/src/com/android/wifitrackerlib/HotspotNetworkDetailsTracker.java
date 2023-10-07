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

import static com.android.wifitrackerlib.HotspotNetworkEntry.HotspotNetworkEntryKey;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.net.wifi.sharedconnectivity.app.HotspotNetwork;
import android.os.Build;
import android.os.Handler;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.Lifecycle;

import java.time.Clock;
import java.util.List;

/**
 * Implementation of NetworkDetailsTracker that tracks a single HotspotNetworkEntry.
 */
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class HotspotNetworkDetailsTracker extends NetworkDetailsTracker {
    private static final String TAG = "HotspotNetworkDetailsTracker";

    private final HotspotNetworkEntry mChosenEntry;

    private HotspotNetwork mHotspotNetworkData;

    public HotspotNetworkDetailsTracker(
            @NonNull Lifecycle lifecycle,
            @NonNull Context context,
            @NonNull WifiManager wifiManager,
            @NonNull ConnectivityManager connectivityManager,
            @NonNull Handler mainHandler,
            @NonNull Handler workerHandler,
            @NonNull Clock clock,
            long maxScanAgeMillis,
            long scanIntervalMillis,
            String key) {
        this(new WifiTrackerInjector(context), lifecycle, context, wifiManager, connectivityManager,
                mainHandler, workerHandler, clock, maxScanAgeMillis, scanIntervalMillis, key);
    }

    @VisibleForTesting
    HotspotNetworkDetailsTracker(
            @NonNull WifiTrackerInjector injector,
            @NonNull Lifecycle lifecycle,
            @NonNull Context context,
            @NonNull WifiManager wifiManager,
            @NonNull ConnectivityManager connectivityManager,
            @NonNull Handler mainHandler,
            @NonNull Handler workerHandler,
            @NonNull Clock clock,
            long maxScanAgeMillis,
            long scanIntervalMillis,
            String key) {
        super(injector, lifecycle, context, wifiManager, connectivityManager,
                mainHandler, workerHandler, clock, maxScanAgeMillis, scanIntervalMillis, TAG);
        Log.v(TAG, "key: " + key);
        HotspotNetworkEntryKey entryKey = new HotspotNetworkEntryKey(key);
        if (entryKey.isVirtualEntry()) {
            Log.e(TAG, "Network details not relevant for virtual entry");
        }
        mChosenEntry = new HotspotNetworkEntry(mInjector, mContext, mMainHandler, mWifiManager,
                mSharedConnectivityManager, entryKey);
        // It is safe to call updateStartInfo() in the main thread here since onStart() won't have
        // a chance to post handleOnStart() on the worker thread until the main thread finishes
        // calling this constructor.
        updateStartInfo();
        Log.v(TAG, "mChosenEntry: " + mChosenEntry);
    }

    @AnyThread
    @Override
    @NonNull
    public WifiEntry getWifiEntry() {
        return mChosenEntry;
    }

    @WorkerThread
    @Override
    protected void handleOnStart() {
        updateStartInfo();
    }

    @WorkerThread
    @Override
    protected void handleServiceConnected() {
        if (mEnableSharedConnectivityFeature && mSharedConnectivityManager != null) {
            mHotspotNetworkData = mSharedConnectivityManager.getHotspotNetworks().stream().filter(
                    network -> network.getDeviceId() == mChosenEntry.getHotspotNetworkEntryKey()
                            .getDeviceId()).findFirst().orElse(null);
        }
        if (mHotspotNetworkData == null) {
            throw new IllegalArgumentException(
                    "Cannot find data for given HotspotNetworkEntry key!");
        }
        mChosenEntry.updateHotspotNetworkData(mHotspotNetworkData);
    }

    @WorkerThread
    @Override
    protected void handleHotspotNetworksUpdated(List<HotspotNetwork> networks) {
        if (mEnableSharedConnectivityFeature) {
            mHotspotNetworkData = networks.stream().filter(network -> network.getDeviceId()
                    == mChosenEntry.getHotspotNetworkEntryKey().getDeviceId()).findFirst().orElse(
                    null);
        }
        if (mHotspotNetworkData == null) {
            throw new IllegalArgumentException(
                    "Cannot find data for given HotspotNetworkEntry key!");
        }
        mChosenEntry.updateHotspotNetworkData(mHotspotNetworkData);
    }

    @WorkerThread
    private void updateStartInfo() {
        handleDefaultSubscriptionChanged(SubscriptionManager.getDefaultDataSubscriptionId());
        Network currentNetwork = mWifiManager.getCurrentNetwork();
        if (currentNetwork == null) return;
        NetworkCapabilities networkCapabilities =
                mConnectivityManager.getNetworkCapabilities(currentNetwork);
        if (networkCapabilities != null) {
            // getNetworkCapabilities(Network) obfuscates location info such as SSID and
            // networkId, so we need to set the WifiInfo directly from WifiManager.
            handleNetworkCapabilitiesChanged(currentNetwork,
                    new NetworkCapabilities.Builder(networkCapabilities)
                            .setTransportInfo(mWifiManager.getConnectionInfo())
                            .build());
        }
        LinkProperties linkProperties = mConnectivityManager.getLinkProperties(currentNetwork);
        if (linkProperties != null) {
            handleLinkPropertiesChanged(currentNetwork, linkProperties);
        }
    }
}

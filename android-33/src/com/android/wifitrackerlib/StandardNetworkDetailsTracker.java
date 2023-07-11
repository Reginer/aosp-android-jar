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

package com.android.wifitrackerlib;

import static androidx.core.util.Preconditions.checkNotNull;

import static com.android.wifitrackerlib.StandardWifiEntry.ScanResultKey;
import static com.android.wifitrackerlib.StandardWifiEntry.StandardWifiEntryKey;

import static java.util.stream.Collectors.toList;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.Lifecycle;

import java.time.Clock;
import java.util.Collections;

/**
 * Implementation of NetworkDetailsTracker that tracks a single StandardWifiEntry.
 */
public class StandardNetworkDetailsTracker extends NetworkDetailsTracker {
    private static final String TAG = "StandardNetworkDetailsTracker";

    private final StandardWifiEntryKey mKey;
    private final StandardWifiEntry mChosenEntry;
    private final boolean mIsNetworkRequest;
    private NetworkInfo mCurrentNetworkInfo;

    public StandardNetworkDetailsTracker(@NonNull Lifecycle lifecycle,
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
    StandardNetworkDetailsTracker(
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
        mKey = new StandardWifiEntryKey(key);
        if (mKey.isNetworkRequest()) {
            mIsNetworkRequest = true;
            mChosenEntry = new NetworkRequestEntry(mInjector, mContext, mMainHandler, mKey,
                    mWifiManager, false /* forSavedNetworksPage */);
        } else {
            mIsNetworkRequest = false;
            mChosenEntry = new StandardWifiEntry(mInjector, mContext, mMainHandler, mKey,
                    mWifiManager, false /* forSavedNetworksPage */);
        }
        // It is safe to call updateStartInfo() in the main thread here since onStart() won't have
        // a chance to post handleOnStart() on the worker thread until the main thread finishes
        // calling this constructor.
        updateStartInfo();
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
    protected void handleWifiStateChangedAction() {
        conditionallyUpdateScanResults(true /* lastScanSucceeded */);
    }

    @WorkerThread
    @Override
    protected void handleScanResultsAvailableAction(@NonNull Intent intent) {
        checkNotNull(intent, "Intent cannot be null!");
        conditionallyUpdateScanResults(
                intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true));
    }

    @WorkerThread
    @Override
    protected void handleConfiguredNetworksChangedAction(@NonNull Intent intent) {
        checkNotNull(intent, "Intent cannot be null!");
        conditionallyUpdateConfig();
    }

    @WorkerThread
    private void updateStartInfo() {
        conditionallyUpdateScanResults(true /* lastScanSucceeded */);
        conditionallyUpdateConfig();
        final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        final Network currentNetwork = mWifiManager.getCurrentNetwork();
        mCurrentNetworkInfo = mConnectivityManager.getNetworkInfo(currentNetwork);
        mChosenEntry.updateConnectionInfo(wifiInfo, mCurrentNetworkInfo);
        handleNetworkCapabilitiesChanged(
                mConnectivityManager.getNetworkCapabilities(currentNetwork));
        handleLinkPropertiesChanged(mConnectivityManager.getLinkProperties(currentNetwork));
        mChosenEntry.setIsDefaultNetwork(mIsWifiDefaultRoute);
        mChosenEntry.setIsLowQuality(mIsWifiValidated && mIsCellDefaultRoute);
    }

    /**
     * Updates the tracked entry's scan results up to the max scan age (or more, if the last scan
     * was unsuccessful). If Wifi is disabled, the tracked entry's level will be cleared.
     */
    private void conditionallyUpdateScanResults(boolean lastScanSucceeded) {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            mChosenEntry.updateScanResultInfo(Collections.emptyList());
            return;
        }

        long scanAgeWindow = mMaxScanAgeMillis;
        if (lastScanSucceeded) {
            cacheNewScanResults();
        } else {
            // Scan failed, increase scan age window to prevent WifiEntry list from
            // clearing prematurely.
            scanAgeWindow += mScanIntervalMillis;
        }
        mChosenEntry.updateScanResultInfo(mScanResultUpdater.getScanResults(scanAgeWindow));
    }

    /**
     * Updates the tracked entry's WifiConfiguration from getPrivilegedConfiguredNetworks(), or sets
     * it to null if it does not exist.
     */
    private void conditionallyUpdateConfig() {
        mChosenEntry.updateConfig(
                mWifiManager.getPrivilegedConfiguredNetworks().stream()
                        .filter(this::configMatches)
                        .collect(toList()));
    }

    /**
     * Updates ScanResultUpdater with new ScanResults matching mChosenEntry.
     */
    private void cacheNewScanResults() {
        mScanResultUpdater.update(mWifiManager.getScanResults().stream()
                .filter(scan -> new ScanResultKey(scan).equals(mKey.getScanResultKey()))
                .collect(toList()));
    }

    private boolean configMatches(@NonNull WifiConfiguration config) {
        if (config.isPasspoint()) {
            return false;
        }
        return mKey.equals(new StandardWifiEntryKey(config, mKey.isTargetingNewNetworks()));
    }
}

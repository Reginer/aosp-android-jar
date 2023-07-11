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

package com.android.wifitrackerlib;

import static androidx.core.util.Preconditions.checkNotNull;

import static com.android.wifitrackerlib.PasspointWifiEntry.uniqueIdToPasspointWifiEntryKey;
import static com.android.wifitrackerlib.WifiEntry.WIFI_LEVEL_UNREACHABLE;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.Lifecycle;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of NetworkDetailsTracker that tracks a single PasspointWifiEntry.
 */
public class PasspointNetworkDetailsTracker extends NetworkDetailsTracker {
    private static final String TAG = "PasspointNetworkDetailsTracker";

    private final PasspointWifiEntry mChosenEntry;
    private OsuWifiEntry mOsuWifiEntry;
    private NetworkInfo mCurrentNetworkInfo;
    private WifiConfiguration mCurrentWifiConfig;

    public PasspointNetworkDetailsTracker(@NonNull Lifecycle lifecycle,
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
    PasspointNetworkDetailsTracker(
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

        Optional<PasspointConfiguration> optionalPasspointConfig =
                mWifiManager.getPasspointConfigurations()
                        .stream()
                        .filter(passpointConfig -> TextUtils.equals(key,
                                uniqueIdToPasspointWifiEntryKey(passpointConfig.getUniqueId())))
                        .findAny();
        if (optionalPasspointConfig.isPresent()) {
            mChosenEntry = new PasspointWifiEntry(mInjector, mContext, mMainHandler,
                    optionalPasspointConfig.get(), mWifiManager,
                    false /* forSavedNetworksPage */);
        } else {
            Optional<WifiConfiguration> optionalWifiConfig =
                    mWifiManager.getPrivilegedConfiguredNetworks()
                            .stream()
                            .filter(wifiConfig -> wifiConfig.isPasspoint()
                                    && TextUtils.equals(key,
                                            uniqueIdToPasspointWifiEntryKey(wifiConfig.getKey())))
                            .findAny();
            if (optionalWifiConfig.isPresent()) {
                mChosenEntry = new PasspointWifiEntry(mInjector, mContext, mMainHandler,
                        optionalWifiConfig.get(), mWifiManager,
                        false /* forSavedNetworksPage */);
            } else {
                throw new IllegalArgumentException(
                        "Cannot find config for given PasspointWifiEntry key!");
            }
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
    protected  void handleOnStart() {
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

    @WorkerThread
    private void updatePasspointWifiEntryScans(@NonNull List<ScanResult> scanResults) {
        checkNotNull(scanResults, "Scan Result list should not be null!");

        List<Pair<WifiConfiguration, Map<Integer, List<ScanResult>>>> matchingWifiConfigs =
                mWifiManager.getAllMatchingWifiConfigs(scanResults);
        for (Pair<WifiConfiguration, Map<Integer, List<ScanResult>>> pair : matchingWifiConfigs) {
            final WifiConfiguration wifiConfig = pair.first;
            final String key = uniqueIdToPasspointWifiEntryKey(wifiConfig.getKey());

            if (TextUtils.equals(key, mChosenEntry.getKey())) {
                mCurrentWifiConfig = wifiConfig;
                mChosenEntry.updateScanResultInfo(mCurrentWifiConfig,
                        pair.second.get(WifiManager.PASSPOINT_HOME_NETWORK),
                        pair.second.get(WifiManager.PASSPOINT_ROAMING_NETWORK));
                return;
            }
        }
        // No AP in range; set scan results to null but keep the last seen WifiConfig to display
        // the previous information while out of range.
        mChosenEntry.updateScanResultInfo(mCurrentWifiConfig,
                null /* homeScanResults */,
                null /* roamingScanResults */);
    }

    @WorkerThread
    private void updateOsuWifiEntryScans(@NonNull List<ScanResult> scanResults) {
        checkNotNull(scanResults, "Scan Result list should not be null!");

        Map<OsuProvider, List<ScanResult>> osuProviderToScans =
                mWifiManager.getMatchingOsuProviders(scanResults);
        Map<OsuProvider, PasspointConfiguration> osuProviderToPasspointConfig =
                mWifiManager.getMatchingPasspointConfigsForOsuProviders(
                        osuProviderToScans.keySet());

        if (mOsuWifiEntry != null) {
            mOsuWifiEntry.updateScanResultInfo(osuProviderToScans.get(
                    mOsuWifiEntry.getOsuProvider()));
        } else {
            // Create a new OsuWifiEntry to link to the chosen PasspointWifiEntry
            for (OsuProvider provider : osuProviderToScans.keySet()) {
                PasspointConfiguration provisionedConfig =
                        osuProviderToPasspointConfig.get(provider);
                if (provisionedConfig != null && TextUtils.equals(mChosenEntry.getKey(),
                        uniqueIdToPasspointWifiEntryKey(provisionedConfig.getUniqueId()))) {
                    mOsuWifiEntry = new OsuWifiEntry(mInjector, mContext, mMainHandler, provider,
                            mWifiManager, false /* forSavedNetworksPage */);
                    mOsuWifiEntry.updateScanResultInfo(osuProviderToScans.get(provider));
                    mOsuWifiEntry.setAlreadyProvisioned(true);
                    mChosenEntry.setOsuWifiEntry(mOsuWifiEntry);
                    return;
                }
            }
        }

        // Remove mOsuWifiEntry if it is no longer reachable
        if (mOsuWifiEntry != null && mOsuWifiEntry.getLevel() == WIFI_LEVEL_UNREACHABLE) {
            mChosenEntry.setOsuWifiEntry(null);
            mOsuWifiEntry = null;
        }
    }

    /**
     * Updates the tracked entry's scan results up to the max scan age (or more, if the last scan
     * was unsuccessful). If Wifi is disabled, the tracked entry's level will be cleared.
     */
    private void conditionallyUpdateScanResults(boolean lastScanSucceeded) {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            mChosenEntry.updateScanResultInfo(mCurrentWifiConfig,
                    null /* homeScanResults */,
                    null /* roamingScanResults */);
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

        List<ScanResult> currentScans = mScanResultUpdater.getScanResults(scanAgeWindow);
        updatePasspointWifiEntryScans(currentScans);
        updateOsuWifiEntryScans(currentScans);
    }

    /**
     * Updates the tracked entry's PasspointConfiguration from getPasspointConfigurations()
     */
    private void conditionallyUpdateConfig() {
        mWifiManager.getPasspointConfigurations().stream()
                .filter(config -> TextUtils.equals(
                        uniqueIdToPasspointWifiEntryKey(config.getUniqueId()),
                        mChosenEntry.getKey()))
                .findAny().ifPresent(config -> mChosenEntry.updatePasspointConfig(config));
    }

    /**
     * Updates ScanResultUpdater with new ScanResults.
     */
    private void cacheNewScanResults() {
        mScanResultUpdater.update(mWifiManager.getScanResults());
    }
}

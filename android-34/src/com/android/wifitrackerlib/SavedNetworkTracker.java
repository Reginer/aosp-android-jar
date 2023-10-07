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

import static com.android.wifitrackerlib.PasspointWifiEntry.uniqueIdToPasspointWifiEntryKey;
import static com.android.wifitrackerlib.StandardWifiEntry.ScanResultKey;
import static com.android.wifitrackerlib.StandardWifiEntry.StandardWifiEntryKey;

import static java.util.stream.Collectors.toMap;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityDiagnosticsManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.Handler;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.Lifecycle;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wi-Fi tracker that provides all Wi-Fi related data to the Saved Networks page.
 *
 * These include
 * - List of WifiEntries for all saved networks, dynamically updated with ScanResults
 * - List of WifiEntries for all saved subscriptions, dynamically updated with ScanResults
 */
public class SavedNetworkTracker extends BaseWifiTracker {

    private static final String TAG = "SavedNetworkTracker";

    private final SavedNetworkTrackerCallback mListener;

    // Lock object for data returned by the public API
    private final Object mLock = new Object();

    @GuardedBy("mLock") private final List<WifiEntry> mSavedWifiEntries = new ArrayList<>();
    @GuardedBy("mLock") private final List<WifiEntry> mSubscriptionWifiEntries = new ArrayList<>();

    // Cache containing saved StandardWifiEntries. Must be accessed only by the worker thread.
    private final List<StandardWifiEntry> mStandardWifiEntryCache = new ArrayList<>();
    // Cache containing saved PasspointWifiEntries. Must be accessed only by the worker thread.
    private final Map<String, PasspointWifiEntry> mPasspointWifiEntryCache = new ArrayMap<>();

    public SavedNetworkTracker(@NonNull Lifecycle lifecycle, @NonNull Context context,
            @NonNull WifiManager wifiManager,
            @NonNull ConnectivityManager connectivityManager,
            @NonNull Handler mainHandler,
            @NonNull Handler workerHandler,
            @NonNull Clock clock,
            long maxScanAgeMillis,
            long scanIntervalMillis,
            @Nullable SavedNetworkTrackerCallback listener) {
        this(new WifiTrackerInjector(context), lifecycle, context, wifiManager, connectivityManager,
                mainHandler, workerHandler, clock, maxScanAgeMillis, scanIntervalMillis, listener);
    }

    @VisibleForTesting
    SavedNetworkTracker(
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
            @Nullable SavedNetworkTrackerCallback listener) {
        super(injector, lifecycle, context, wifiManager, connectivityManager,
                mainHandler, workerHandler, clock, maxScanAgeMillis, scanIntervalMillis, listener,
                TAG);
        mListener = listener;
    }

    /**
     * Returns a list of WifiEntries for all saved networks. If a network is in range, the
     * corresponding WifiEntry will be updated with live ScanResult data.
     * @return
     */
    @AnyThread
    @NonNull
    public List<WifiEntry> getSavedWifiEntries() {
        synchronized (mLock) {
            return new ArrayList<>(mSavedWifiEntries);
        }
    }

    /**
     * Returns a list of WifiEntries for all saved subscriptions. If a subscription network is in
     * range, the corresponding WifiEntry will be updated with live ScanResult data.
     * @return
     */
    @AnyThread
    @NonNull
    public List<WifiEntry> getSubscriptionWifiEntries() {
        synchronized (mLock) {
            return new ArrayList<>(mSubscriptionWifiEntries);
        }
    }

    /** Check whether or not CA certificate is set.
     *
     * WifiEnterpriseConfig::hasCaCertificate() is only available
     * after API level 33.
     */
    private static boolean hasCaCertificate(WifiEnterpriseConfig ec) {
        if (ec.getCaCertificateAliases() != null) return true;
        if (ec.getCaCertificates() != null) return true;
        if (!TextUtils.isEmpty(ec.getCaPath())) return true;
        return false;
    }

    private static boolean isCertificateUsedByConfiguration(
            WifiConfiguration config, String certAlias) {
        if (TextUtils.isEmpty(certAlias)) return false;
        if (config == null) return false;
        if (config.enterpriseConfig == null) return false;
        WifiEnterpriseConfig ec = config.enterpriseConfig;
        if (!ec.isEapMethodServerCertUsed()) return false;
        if (!hasCaCertificate(ec) && TextUtils.isEmpty(ec.getClientCertificateAlias())) {
            return false;
        }

        String[] aliases = ec.getCaCertificateAliases();
        if (aliases != null) {
            for (String s: aliases) {
                if (!TextUtils.isEmpty(s) && certAlias.equals(s)) {
                    return true;
                }
            }
        }
        String clientAlias = ec.getClientCertificateAlias();
        if (!TextUtils.isEmpty(clientAlias)
                && certAlias.equals(clientAlias)) {
            return true;
        }
        return false;
    }

    /**
     * Check whether or not a certifiate is required by saved networks or network suggestions.
     */
    @AnyThread
    public boolean isCertificateRequired(String certAlias) {
        // Configurations from Wi-Fi Network Suggestion
        List<WifiConfiguration> configurations = mWifiManager.getNetworkSuggestions()
                .stream().map(s -> s.getWifiConfiguration())
                .collect(Collectors.toList());
        // Configurations from regular Wi-Fi configurations.
        configurations.addAll(mWifiManager.getConfiguredNetworks());

        return configurations.stream()
                .anyMatch(c -> isCertificateUsedByConfiguration(c, certAlias));
    }

    /**
     * Returns a list of network names which requires the certificate alias.
     *
     * @return a list of network names.
     */
    @AnyThread
    @NonNull
    public List<String> getCertificateRequesterNames(String certAlias) {
        // Configurations from Wi-Fi Network Suggestion
        List<WifiConfiguration> configurations = mWifiManager.getNetworkSuggestions()
                .stream().map(s -> s.getWifiConfiguration())
                .collect(Collectors.toList());
        // Configurations from regular Wi-Fi configurations.
        configurations.addAll(mWifiManager.getConfiguredNetworks());

        return configurations.stream()
                .filter(c -> isCertificateUsedByConfiguration(c, certAlias))
                .map(c -> c.SSID).collect(Collectors.toSet())
                .stream().collect(Collectors.toList());
    }

    private List<WifiEntry> getAllWifiEntries() {
        List<WifiEntry> allEntries = new ArrayList<>();
        allEntries.addAll(mStandardWifiEntryCache);
        allEntries.addAll(mPasspointWifiEntryCache.values());
        return allEntries;
    }

    private void clearAllWifiEntries() {
        mStandardWifiEntryCache.clear();
        mPasspointWifiEntryCache.clear();
    }

    @WorkerThread
    @Override
    protected void handleOnStart() {
        // Remove stale WifiEntries remaining from the last onStop().
        clearAllWifiEntries();

        // Update configs and scans
        updateStandardWifiEntryConfigs(mWifiManager.getConfiguredNetworks());
        updatePasspointWifiEntryConfigs(mWifiManager.getPasspointConfigurations());
        mScanResultUpdater.update(mWifiManager.getScanResults());
        conditionallyUpdateScanResults(true /* lastScanSucceeded */);

        // Trigger callbacks manually now to avoid waiting until the first calls to update state.
        Network currentNetwork = mWifiManager.getCurrentNetwork();
        if (currentNetwork != null) {
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
        updateWifiEntries();
    }

    @WorkerThread
    @Override
    protected void handleWifiStateChangedAction() {
        conditionallyUpdateScanResults(true /* lastScanSucceeded */);
        updateWifiEntries();
    }

    @WorkerThread
    @Override
    protected void handleScanResultsAvailableAction(@Nullable Intent intent) {
        checkNotNull(intent, "Intent cannot be null!");
        conditionallyUpdateScanResults(intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED,
                true /* defaultValue */));
        updateWifiEntries();
    }

    @WorkerThread
    @Override
    protected void handleConfiguredNetworksChangedAction(@Nullable Intent intent) {
        checkNotNull(intent, "Intent cannot be null!");
        updateStandardWifiEntryConfigs(mWifiManager.getConfiguredNetworks());
        updatePasspointWifiEntryConfigs(mWifiManager.getPasspointConfigurations());
        updateWifiEntries();
    }

    @WorkerThread
    @Override
    protected void handleNetworkStateChangedAction(@NonNull Intent intent) {
        WifiInfo primaryWifiInfo = mWifiManager.getConnectionInfo();
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (primaryWifiInfo == null || networkInfo == null) {
            return;
        }
        for (WifiEntry entry : getAllWifiEntries()) {
            entry.onPrimaryWifiInfoChanged(primaryWifiInfo, networkInfo);
        }
    }

    @WorkerThread
    @Override
    protected void handleLinkPropertiesChanged(
            @NonNull Network network, @Nullable LinkProperties linkProperties) {
        for (WifiEntry entry : getAllWifiEntries()) {
            entry.updateLinkProperties(network, linkProperties);
        }
    }

    @WorkerThread
    @Override
    protected void handleNetworkCapabilitiesChanged(
            @NonNull Network network, @NonNull NetworkCapabilities capabilities) {
        updateConnectionInfo(network, capabilities);
        updateWifiEntries();
    }

    @WorkerThread
    @Override
    protected void handleNetworkLost(@NonNull Network network) {
        for (WifiEntry entry : getAllWifiEntries()) {
            entry.onNetworkLost(network);
        }
        updateWifiEntries();
    }

    @WorkerThread
    @Override
    protected void handleConnectivityReportAvailable(
            @NonNull ConnectivityDiagnosticsManager.ConnectivityReport connectivityReport) {
        for (WifiEntry entry : getAllWifiEntries()) {
            entry.updateConnectivityReport(connectivityReport);
        }
    }


    @WorkerThread
    protected void handleDefaultNetworkCapabilitiesChanged(@NonNull Network network,
            @NonNull NetworkCapabilities networkCapabilities) {
        for (WifiEntry entry : getAllWifiEntries()) {
            entry.onDefaultNetworkCapabilitiesChanged(network, networkCapabilities);
        }
    }

    @WorkerThread
    @Override
    protected void handleDefaultNetworkLost() {
        for (WifiEntry entry : getAllWifiEntries()) {
            entry.onDefaultNetworkLost();
        }
    }

    /**
     * Update the list returned by {@link #getSavedWifiEntries()} and
     * {@link #getSubscriptionWifiEntries()} with the current states of the entry caches.
     */
    private void updateWifiEntries() {
        synchronized (mLock) {
            mSavedWifiEntries.clear();
            mSavedWifiEntries.addAll(mStandardWifiEntryCache);
            Collections.sort(mSavedWifiEntries, WifiEntry.TITLE_COMPARATOR);
            mSubscriptionWifiEntries.clear();
            mSubscriptionWifiEntries.addAll(mPasspointWifiEntryCache.values());
            Collections.sort(mSubscriptionWifiEntries, WifiEntry.TITLE_COMPARATOR);
            if (isVerboseLoggingEnabled()) {
                Log.v(TAG, "Updated SavedWifiEntries: "
                        + Arrays.toString(mSavedWifiEntries.toArray()));
                Log.v(TAG, "Updated SubscriptionWifiEntries: "
                        + Arrays.toString(mSubscriptionWifiEntries.toArray()));
            }
        }
        notifyOnSavedWifiEntriesChanged();
        notifyOnSubscriptionWifiEntriesChanged();
    }

    private void updateStandardWifiEntryScans(@NonNull List<ScanResult> scanResults) {
        checkNotNull(scanResults, "Scan Result list should not be null!");

        // Group scans by StandardWifiEntry key
        final Map<ScanResultKey, List<ScanResult>> scanResultsByKey = scanResults.stream()
                .collect(Collectors.groupingBy(StandardWifiEntry.ScanResultKey::new));

        // Iterate through current entries and update each entry's scan results
        mStandardWifiEntryCache.forEach(entry -> {
            // Update scan results if available, or set to null.
            entry.updateScanResultInfo(
                    scanResultsByKey.get(entry.getStandardWifiEntryKey().getScanResultKey()));
        });
    }

    private void updatePasspointWifiEntryScans(@NonNull List<ScanResult> scanResults) {
        checkNotNull(scanResults, "Scan Result list should not be null!");

        Set<String> seenKeys = new TreeSet<>();
        List<Pair<WifiConfiguration, Map<Integer, List<ScanResult>>>> matchingWifiConfigs =
                mWifiManager.getAllMatchingWifiConfigs(scanResults);
        for (Pair<WifiConfiguration, Map<Integer, List<ScanResult>>> pair : matchingWifiConfigs) {
            final WifiConfiguration wifiConfig = pair.first;
            final String key = uniqueIdToPasspointWifiEntryKey(wifiConfig.getKey());
            seenKeys.add(key);
            // Skip in case we don't have a PasspointWifiEntry for the returned unique identifier.
            if (!mPasspointWifiEntryCache.containsKey(key)) {
                continue;
            }

            mPasspointWifiEntryCache.get(key).updateScanResultInfo(wifiConfig,
                    pair.second.get(WifiManager.PASSPOINT_HOME_NETWORK),
                    pair.second.get(WifiManager.PASSPOINT_ROAMING_NETWORK));
        }

        for (PasspointWifiEntry entry : mPasspointWifiEntryCache.values()) {
            if (!seenKeys.contains(entry.getKey())) {
                // No AP in range; set scan results and connection config to null.
                entry.updateScanResultInfo(null /* wifiConfig */,
                        null /* homeScanResults */,
                        null /* roamingScanResults */);
            }
        }
    }

    /**
     * Conditionally updates the WifiEntry scan results based on the current wifi state and
     * whether the last scan succeeded or not.
     */
    @WorkerThread
    private void conditionallyUpdateScanResults(boolean lastScanSucceeded) {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            updateStandardWifiEntryScans(Collections.emptyList());
            updatePasspointWifiEntryScans(Collections.emptyList());
            return;
        }

        long scanAgeWindow = mMaxScanAgeMillis;
        if (lastScanSucceeded) {
            // Scan succeeded, cache new scans
            mScanResultUpdater.update(mWifiManager.getScanResults());
        } else {
            // Scan failed, increase scan age window to prevent WifiEntry list from
            // clearing prematurely.
            scanAgeWindow += mScanIntervalMillis;
        }
        updateStandardWifiEntryScans(mScanResultUpdater.getScanResults(scanAgeWindow));
        updatePasspointWifiEntryScans(mScanResultUpdater.getScanResults(scanAgeWindow));
    }

    private void updateStandardWifiEntryConfigs(@NonNull List<WifiConfiguration> configs) {
        checkNotNull(configs, "Config list should not be null!");

        // Group configs by StandardWifiEntry key
        final Map<StandardWifiEntryKey, List<WifiConfiguration>> wifiConfigsByKey = configs.stream()
                .filter(config -> !config.carrierMerged)
                .collect(Collectors.groupingBy(StandardWifiEntryKey::new));

        // Iterate through current entries and update each entry's config
        mStandardWifiEntryCache.removeIf(entry -> {
            // Update config if available, or set to null (unsaved)
            entry.updateConfig(wifiConfigsByKey.remove(entry.getStandardWifiEntryKey()));
            // Entry is now unsaved, remove it.
            return !entry.isSaved();
        });

        // Create new entry for each unmatched config
        for (StandardWifiEntryKey key : wifiConfigsByKey.keySet()) {
            mStandardWifiEntryCache.add(new StandardWifiEntry(mInjector, mContext, mMainHandler,
                    key, wifiConfigsByKey.get(key), null, mWifiManager,
                    true /* forSavedNetworksPage */));
        }
    }

    @WorkerThread
    private void updatePasspointWifiEntryConfigs(@NonNull List<PasspointConfiguration> configs) {
        checkNotNull(configs, "Config list should not be null!");

        final Map<String, PasspointConfiguration> passpointConfigsByKey =
                configs.stream().collect(toMap(
                        (config) -> uniqueIdToPasspointWifiEntryKey(config.getUniqueId()),
                        Function.identity()));

        // Iterate through current entries and update each entry's config or remove if no config
        // matches the entry anymore.
        mPasspointWifiEntryCache.entrySet().removeIf((entry) -> {
            final PasspointWifiEntry wifiEntry = entry.getValue();
            final String key = wifiEntry.getKey();
            final PasspointConfiguration cachedConfig = passpointConfigsByKey.remove(key);
            if (cachedConfig != null) {
                wifiEntry.updatePasspointConfig(cachedConfig);
                return false;
            } else {
                return true;
            }
        });

        // Create new entry for each unmatched config
        for (String key : passpointConfigsByKey.keySet()) {
            mPasspointWifiEntryCache.put(key,
                    new PasspointWifiEntry(mInjector, mContext, mMainHandler,
                            passpointConfigsByKey.get(key), mWifiManager,
                            true /* forSavedNetworksPage */));
        }
    }

    /**
     * Updates all matching WifiEntries with the given connection info.
     * @param network Network for which the NetworkCapabilities have changed.
     * @param capabilities NetworkCapabilities that have changed.
     */
    @WorkerThread
    private void updateConnectionInfo(
            @NonNull Network network, @NonNull NetworkCapabilities capabilities) {
        for (WifiEntry entry : getAllWifiEntries()) {
            entry.onNetworkCapabilitiesChanged(network, capabilities);
        }
    }

    /**
     * Posts onSavedWifiEntriesChanged callback on the main thread.
     */
    @WorkerThread
    private void notifyOnSavedWifiEntriesChanged() {
        if (mListener != null) {
            mMainHandler.post(mListener::onSavedWifiEntriesChanged);
        }
    }

    /**
     * Posts onSubscriptionWifiEntriesChanged callback on the main thread.
     */
    @WorkerThread
    private void notifyOnSubscriptionWifiEntriesChanged() {
        if (mListener != null) {
            mMainHandler.post(mListener::onSubscriptionWifiEntriesChanged);
        }
    }

    /**
     * Listener for changes to the list of saved and subscription WifiEntries
     *
     * These callbacks must be run on the MainThread.
     */
    public interface SavedNetworkTrackerCallback extends BaseWifiTracker.BaseWifiTrackerCallback {
        /**
         * Called when there are changes to
         *      {@link #getSavedWifiEntries()}
         */
        @MainThread
        void onSavedWifiEntriesChanged();

        /**
         * Called when there are changes to
         *      {@link #getSubscriptionWifiEntries()}
         */
        @MainThread
        void onSubscriptionWifiEntriesChanged();
    }
}

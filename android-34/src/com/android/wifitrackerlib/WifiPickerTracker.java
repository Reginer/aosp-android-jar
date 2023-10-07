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

import static android.os.Build.VERSION_CODES;

import static androidx.core.util.Preconditions.checkNotNull;

import static com.android.wifitrackerlib.OsuWifiEntry.osuProviderToOsuWifiEntryKey;
import static com.android.wifitrackerlib.PasspointWifiEntry.uniqueIdToPasspointWifiEntryKey;
import static com.android.wifitrackerlib.StandardWifiEntry.ScanResultKey;
import static com.android.wifitrackerlib.StandardWifiEntry.StandardWifiEntryKey;
import static com.android.wifitrackerlib.WifiEntry.CONNECTED_STATE_DISCONNECTED;
import static com.android.wifitrackerlib.WifiEntry.WIFI_LEVEL_UNREACHABLE;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import android.annotation.TargetApi;
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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.sharedconnectivity.app.HotspotNetwork;
import android.net.wifi.sharedconnectivity.app.HotspotNetworkConnectionStatus;
import android.net.wifi.sharedconnectivity.app.KnownNetwork;
import android.net.wifi.sharedconnectivity.app.KnownNetworkConnectionStatus;
import android.os.Handler;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.os.BuildCompat;
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
 * Wi-Fi tracker that provides all Wi-Fi related data to the Wi-Fi picker page.
 *
 * These include
 * - The connected WifiEntry
 * - List of all visible WifiEntries
 * - Number of saved networks
 * - Number of saved subscriptions
 */
public class WifiPickerTracker extends BaseWifiTracker {

    private static final String TAG = "WifiPickerTracker";

    private final WifiPickerTrackerCallback mListener;

    // Lock object for data returned by the public API
    private final Object mLock = new Object();
    // List representing the return value of the getActiveWifiEntries() API
    @GuardedBy("mLock")
    @NonNull private final List<WifiEntry> mActiveWifiEntries = new ArrayList<>();
    // List representing the return value of the getWifiEntries() API
    @GuardedBy("mLock")
    @NonNull private final List<WifiEntry> mWifiEntries = new ArrayList<>();
    // NetworkRequestEntry representing a network that was connected through the NetworkRequest API
    private NetworkRequestEntry mNetworkRequestEntry;

    // Cache containing saved WifiConfigurations mapped by StandardWifiEntry key
    private final Map<StandardWifiEntryKey, List<WifiConfiguration>> mStandardWifiConfigCache =
            new ArrayMap<>();
    // Cache containing suggested WifiConfigurations mapped by StandardWifiEntry key
    private final Map<StandardWifiEntryKey, List<WifiConfiguration>> mSuggestedConfigCache =
            new ArrayMap<>();
    // Cache containing network request WifiConfigurations mapped by StandardWifiEntry key.
    private final ArrayMap<StandardWifiEntryKey, List<WifiConfiguration>>
            mNetworkRequestConfigCache = new ArrayMap<>();
    // Cache containing visible StandardWifiEntries. Must be accessed only by the worker thread.
    private final List<StandardWifiEntry> mStandardWifiEntryCache = new ArrayList<>();
    // Cache containing available suggested StandardWifiEntries. These entries may be already
    // represented in mStandardWifiEntryCache, so filtering must be done before they are returned in
    // getWifiEntry() and getConnectedWifiEntry().
    private final List<StandardWifiEntry> mSuggestedWifiEntryCache = new ArrayList<>();
    // Cache containing saved PasspointConfigurations mapped by PasspointWifiEntry key.
    private final Map<String, PasspointConfiguration> mPasspointConfigCache = new ArrayMap<>();
    // Cache containing Passpoint WifiConfigurations mapped by network id.
    private final SparseArray<WifiConfiguration> mPasspointWifiConfigCache = new SparseArray<>();
    // Cache containing visible PasspointWifiEntries. Must be accessed only by the worker thread.
    private final Map<String, PasspointWifiEntry> mPasspointWifiEntryCache = new ArrayMap<>();
    // Cache containing visible OsuWifiEntries. Must be accessed only by the worker thread.
    private final Map<String, OsuWifiEntry> mOsuWifiEntryCache = new ArrayMap<>();

    private MergedCarrierEntry mMergedCarrierEntry;

    private int mNumSavedNetworks;

    private final List<KnownNetwork> mKnownNetworkDataCache = new ArrayList<>();
    private final List<KnownNetworkEntry> mKnownNetworkEntryCache = new ArrayList<>();
    private final List<HotspotNetwork> mHotspotNetworkDataCache = new ArrayList<>();
    private final List<HotspotNetworkEntry> mHotspotNetworkEntryCache = new ArrayList<>();

    /**
     * Constructor for WifiPickerTracker.
     * @param lifecycle Lifecycle this is tied to for lifecycle callbacks.
     * @param context Context for registering broadcast receiver and for resource strings.
     * @param wifiManager Provides all Wi-Fi info.
     * @param connectivityManager Provides network info.
     * @param mainHandler Handler for processing listener callbacks.
     * @param workerHandler Handler for processing all broadcasts and running the Scanner.
     * @param clock Clock used for evaluating the age of scans
     * @param maxScanAgeMillis Max age for tracked WifiEntries.
     * @param scanIntervalMillis Interval between initiating scans.
     * @param listener WifiTrackerCallback listening on changes to WifiPickerTracker data.
     */
    public WifiPickerTracker(@NonNull Lifecycle lifecycle, @NonNull Context context,
            @NonNull WifiManager wifiManager,
            @NonNull ConnectivityManager connectivityManager,
            @NonNull Handler mainHandler,
            @NonNull Handler workerHandler,
            @NonNull Clock clock,
            long maxScanAgeMillis,
            long scanIntervalMillis,
            @Nullable WifiPickerTrackerCallback listener) {
        this(new WifiTrackerInjector(context), lifecycle, context, wifiManager, connectivityManager,
                mainHandler, workerHandler, clock, maxScanAgeMillis, scanIntervalMillis, listener);
    }

    @VisibleForTesting
    WifiPickerTracker(
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
            @Nullable WifiPickerTrackerCallback listener) {
        super(injector, lifecycle, context, wifiManager, connectivityManager,
                mainHandler, workerHandler, clock, maxScanAgeMillis, scanIntervalMillis, listener,
                TAG);
        mListener = listener;
    }

    /**
     * Returns the WifiEntry representing the current primary connection.
     */
    @AnyThread
    public @Nullable WifiEntry getConnectedWifiEntry() {
        synchronized (mLock) {
            if (mActiveWifiEntries.isEmpty()) {
                return null;
            }
            // Primary entry is sorted to be first.
            WifiEntry primaryWifiEntry = mActiveWifiEntries.get(0);
            if (!primaryWifiEntry.isPrimaryNetwork()) {
                return null;
            }
            return primaryWifiEntry;
        }
    }

    /**
     * Returns a list of all connected/connecting Wi-Fi entries, including the primary and any
     * secondary connections.
     */
    @AnyThread
    public @NonNull List<WifiEntry> getActiveWifiEntries() {
        synchronized (mLock) {
            return new ArrayList<>(mActiveWifiEntries);
        }
    }

    /**
     * Returns a list of disconnected, in-range WifiEntries.
     *
     * The currently connected entry is omitted and may be accessed through
     * {@link #getConnectedWifiEntry()}
     */
    @AnyThread
    public @NonNull List<WifiEntry> getWifiEntries() {
        synchronized (mLock) {
            return new ArrayList<>(mWifiEntries);
        }
    }

    /**
     * Returns the MergedCarrierEntry representing the active carrier subscription.
     */
    @AnyThread
    public @Nullable MergedCarrierEntry getMergedCarrierEntry() {
        if (!isInitialized() && mMergedCarrierEntry == null) {
            // Settings currently relies on the MergedCarrierEntry being available before
            // handleOnStart() is called in order to display the W+ toggle. Populate it here if
            // we aren't initialized yet.
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                mMergedCarrierEntry = new MergedCarrierEntry(mWorkerHandler, mWifiManager,
                        /* forSavedNetworksPage */ false, mContext, subId);
            }
        }
        return mMergedCarrierEntry;
    }

    /**
     * Returns the number of saved networks.
     */
    @AnyThread
    public int getNumSavedNetworks() {
        return mNumSavedNetworks;
    }

    /**
     * Returns the number of saved subscriptions.
     */
    @AnyThread
    public int getNumSavedSubscriptions() {
        return mPasspointConfigCache.size();
    }

    private List<WifiEntry> getAllWifiEntries() {
        List<WifiEntry> allEntries = new ArrayList<>();
        allEntries.addAll(mStandardWifiEntryCache);
        allEntries.addAll(mSuggestedWifiEntryCache);
        allEntries.addAll(mPasspointWifiEntryCache.values());
        allEntries.addAll(mOsuWifiEntryCache.values());
        if (mEnableSharedConnectivityFeature) {
            allEntries.addAll(mKnownNetworkEntryCache);
            allEntries.addAll(mHotspotNetworkEntryCache);
        }
        if (mNetworkRequestEntry != null) {
            allEntries.add(mNetworkRequestEntry);
        }
        if (mMergedCarrierEntry != null) {
            allEntries.add(mMergedCarrierEntry);
        }
        return allEntries;
    }

    private void clearAllWifiEntries() {
        mStandardWifiEntryCache.clear();
        mSuggestedWifiEntryCache.clear();
        mPasspointWifiEntryCache.clear();
        mOsuWifiEntryCache.clear();
        if (mEnableSharedConnectivityFeature) {
            mKnownNetworkEntryCache.clear();
            mHotspotNetworkEntryCache.clear();
        }
        mNetworkRequestEntry = null;
        mMergedCarrierEntry = null;
    }

    @WorkerThread
    @Override
    protected void handleOnStart() {
        // Remove stale WifiEntries remaining from the last onStop().
        clearAllWifiEntries();

        // Update configs and scans
        updateWifiConfigurations(mWifiManager.getPrivilegedConfiguredNetworks());
        updatePasspointConfigurations(mWifiManager.getPasspointConfigurations());
        mScanResultUpdater.update(mWifiManager.getScanResults());
        conditionallyUpdateScanResults(true /* lastScanSucceeded */);

        // Trigger callbacks manually now to avoid waiting until the first calls to update state.
        handleDefaultSubscriptionChanged(SubscriptionManager.getDefaultDataSubscriptionId());
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
        notifyOnNumSavedNetworksChanged();
        notifyOnNumSavedSubscriptionsChanged();
        updateWifiEntries();
    }

    @WorkerThread
    @Override
    protected void handleWifiStateChangedAction() {
        if (getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            clearAllWifiEntries();
        }
        updateWifiEntries();
    }

    @WorkerThread
    @Override
    protected void handleScanResultsAvailableAction(@NonNull Intent intent) {
        checkNotNull(intent, "Intent cannot be null!");
        conditionallyUpdateScanResults(
                intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true));
        updateWifiEntries();
    }

    @WorkerThread
    @Override
    protected void handleConfiguredNetworksChangedAction(@NonNull Intent intent) {
        checkNotNull(intent, "Intent cannot be null!");

        processConfiguredNetworksChanged();
    }

    @WorkerThread
    /** All wifi entries and saved entries needs to be updated. */
    protected void processConfiguredNetworksChanged() {
        updateWifiConfigurations(mWifiManager.getPrivilegedConfiguredNetworks());
        updatePasspointConfigurations(mWifiManager.getPasspointConfigurations());
        // Update scans since config changes may result in different entries being shown.
        final List<ScanResult> scanResults = mScanResultUpdater.getScanResults();
        updateStandardWifiEntryScans(scanResults);
        updateNetworkRequestEntryScans(scanResults);
        updatePasspointWifiEntryScans(scanResults);
        updateOsuWifiEntryScans(scanResults);
        if (mEnableSharedConnectivityFeature && BuildCompat.isAtLeastU()) {
            updateKnownNetworkEntryScans(scanResults);
            // Updating the hotspot entries here makes the UI more reliable when switching pages or
            // when toggling settings while the internet picker is shown.
            updateHotspotNetworkEntries();
        }
        notifyOnNumSavedNetworksChanged();
        notifyOnNumSavedSubscriptionsChanged();
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
        updateNetworkCapabilities(network, capabilities);
        updateWifiEntries();
    }

    @WorkerThread
    @Override
    protected void handleNetworkLost(@NonNull Network network) {
        for (WifiEntry entry : getAllWifiEntries()) {
            entry.onNetworkLost(network);
        }
        if (mNetworkRequestEntry != null
                && mNetworkRequestEntry.getConnectedState() == CONNECTED_STATE_DISCONNECTED) {
            mNetworkRequestEntry = null;
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

    @WorkerThread
    @Override
    protected void handleDefaultSubscriptionChanged(int defaultSubId) {
        updateMergedCarrierEntry(defaultSubId);
    }

    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    @Override
    protected void handleKnownNetworksUpdated(List<KnownNetwork> networks) {
        if (mEnableSharedConnectivityFeature) {
            mKnownNetworkDataCache.clear();
            mKnownNetworkDataCache.addAll(networks);
            updateKnownNetworkEntryScans(mScanResultUpdater.getScanResults());
            updateWifiEntries();
        }
    }

    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    @Override
    protected void handleHotspotNetworksUpdated(List<HotspotNetwork> networks) {
        if (mEnableSharedConnectivityFeature) {
            mHotspotNetworkDataCache.clear();
            mHotspotNetworkDataCache.addAll(networks);
            updateHotspotNetworkEntries();
            updateWifiEntries();
        }
    }
    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    protected void handleHotspotNetworkConnectionStatusChanged(
            @NonNull HotspotNetworkConnectionStatus status) {
        mHotspotNetworkEntryCache.stream().filter(
                entry -> entry.getHotspotNetworkEntryKey().getDeviceId()
                        == status.getHotspotNetwork().getDeviceId()).forEach(
                                entry -> entry.onConnectionStatusChanged(status.getStatus()));
    }

    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    @Override
    protected void handleKnownNetworkConnectionStatusChanged(
            @NonNull KnownNetworkConnectionStatus status) {
        final ScanResultKey key = new ScanResultKey(status.getKnownNetwork().getSsid(),
                status.getKnownNetwork().getSecurityTypes().stream().toList());
        mKnownNetworkEntryCache.stream().filter(
                entry -> entry.getStandardWifiEntryKey().getScanResultKey().equals(key)).forEach(
                        entry -> entry.onConnectionStatusChanged(status.getStatus()));
    }

    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    @Override
    protected void handleServiceConnected() {
        if (mEnableSharedConnectivityFeature) {
            mKnownNetworkDataCache.clear();
            mKnownNetworkDataCache.addAll(mSharedConnectivityManager.getKnownNetworks());
            mHotspotNetworkDataCache.clear();
            mHotspotNetworkDataCache.addAll(mSharedConnectivityManager.getHotspotNetworks());
            updateKnownNetworkEntryScans(mScanResultUpdater.getScanResults());
            updateHotspotNetworkEntries();
            updateWifiEntries();
        }
    }

    /**
     * Update the list returned by getWifiEntries() with the current states of the entry caches.
     */
    @WorkerThread
    protected void updateWifiEntries() {
        synchronized (mLock) {
            mActiveWifiEntries.clear();
            mActiveWifiEntries.addAll(mStandardWifiEntryCache);
            mActiveWifiEntries.addAll(mSuggestedWifiEntryCache);
            mActiveWifiEntries.addAll(mPasspointWifiEntryCache.values());
            if (mEnableSharedConnectivityFeature) {
                mActiveWifiEntries.addAll(mHotspotNetworkEntryCache);
            }
            if (mNetworkRequestEntry != null) {
                mActiveWifiEntries.add(mNetworkRequestEntry);
            }
            mActiveWifiEntries.removeIf(entry ->
                    entry.getConnectedState() == CONNECTED_STATE_DISCONNECTED);
            Set<ScanResultKey> activeHotspotNetworkKeys = new ArraySet<>();
            for (WifiEntry entry : mActiveWifiEntries) {
                if (entry instanceof HotspotNetworkEntry) {
                    activeHotspotNetworkKeys.add(((HotspotNetworkEntry) entry)
                            .getHotspotNetworkEntryKey().getScanResultKey());
                }
            }
            mActiveWifiEntries.removeIf(entry -> entry instanceof StandardWifiEntry
                    && activeHotspotNetworkKeys.contains(
                    ((StandardWifiEntry) entry).getStandardWifiEntryKey().getScanResultKey()));
            mActiveWifiEntries.sort(WifiEntry.WIFI_PICKER_COMPARATOR);
            mWifiEntries.clear();
            final Set<ScanResultKey> scanResultKeysWithVisibleSuggestions =
                    mSuggestedWifiEntryCache.stream()
                            .filter(entry -> {
                                if (entry.isUserShareable()) return true;
                                synchronized (mLock) {
                                    return mActiveWifiEntries.contains(entry);
                                }
                            })
                            .map(entry -> entry.getStandardWifiEntryKey().getScanResultKey())
                            .collect(Collectors.toSet());
            Set<String> passpointUtf8Ssids = new ArraySet<>();
            for (PasspointWifiEntry passpointWifiEntry : mPasspointWifiEntryCache.values()) {
                passpointUtf8Ssids.addAll(passpointWifiEntry.getAllUtf8Ssids());
            }
            Set<ScanResultKey> knownNetworkKeys = new ArraySet<>();
            for (KnownNetworkEntry knownNetworkEntry : mKnownNetworkEntryCache) {
                knownNetworkKeys.add(
                        knownNetworkEntry.getStandardWifiEntryKey().getScanResultKey());
            }
            Set<ScanResultKey> hotspotNetworkKeys = new ArraySet<>();
            for (HotspotNetworkEntry hotspotNetworkEntry : mHotspotNetworkEntryCache) {
                if (!hotspotNetworkEntry.getHotspotNetworkEntryKey().isVirtualEntry()) {
                    hotspotNetworkKeys.add(
                            hotspotNetworkEntry.getHotspotNetworkEntryKey().getScanResultKey());
                }
            }
            for (StandardWifiEntry entry : mStandardWifiEntryCache) {
                entry.updateAdminRestrictions();
                if (mActiveWifiEntries.contains(entry)) {
                    continue;
                }
                if (!entry.isSaved()) {
                    if (scanResultKeysWithVisibleSuggestions
                            .contains(entry.getStandardWifiEntryKey().getScanResultKey())) {
                        continue;
                    }
                    // Filter out any unsaved entries that are already provisioned with Passpoint
                    if (passpointUtf8Ssids.contains(entry.getSsid())) {
                        continue;
                    }
                }
                if (mEnableSharedConnectivityFeature) {
                    // Filter out any StandardWifiEntry that is matched with a KnownNetworkEntry
                    if (knownNetworkKeys
                            .contains(entry.getStandardWifiEntryKey().getScanResultKey())) {
                        continue;
                    }
                    // Filter out any StandardWifiEntry that is matched with a HotspotNetworkEntry
                    if (hotspotNetworkKeys
                            .contains(entry.getStandardWifiEntryKey().getScanResultKey())) {
                        continue;
                    }
                }
                mWifiEntries.add(entry);
            }
            mWifiEntries.addAll(mSuggestedWifiEntryCache.stream().filter(entry ->
                    entry.getConnectedState() == CONNECTED_STATE_DISCONNECTED
                            && entry.isUserShareable()).collect(toList()));
            mWifiEntries.addAll(mPasspointWifiEntryCache.values().stream().filter(entry ->
                    entry.getConnectedState() == CONNECTED_STATE_DISCONNECTED).collect(toList()));
            mWifiEntries.addAll(mOsuWifiEntryCache.values().stream().filter(entry ->
                    entry.getConnectedState() == CONNECTED_STATE_DISCONNECTED
                            && !entry.isAlreadyProvisioned()).collect(toList()));
            mWifiEntries.addAll(getContextualWifiEntries().stream().filter(entry ->
                    entry.getConnectedState() == CONNECTED_STATE_DISCONNECTED).collect(toList()));
            if (mEnableSharedConnectivityFeature) {
                mWifiEntries.addAll(mKnownNetworkEntryCache.stream().filter(entry ->
                        entry.getConnectedState() == CONNECTED_STATE_DISCONNECTED).collect(
                        toList()));
                mWifiEntries.addAll(mHotspotNetworkEntryCache.stream().filter(entry ->
                        entry.getConnectedState() == CONNECTED_STATE_DISCONNECTED).collect(
                        toList()));
            }
            Collections.sort(mWifiEntries, WifiEntry.WIFI_PICKER_COMPARATOR);
            if (isVerboseLoggingEnabled()) {
                Log.v(TAG, "Connected WifiEntries: "
                        + Arrays.toString(mActiveWifiEntries.toArray()));
                Log.v(TAG, "Updated WifiEntries: " + Arrays.toString(mWifiEntries.toArray()));
            }
        }
        notifyOnWifiEntriesChanged();
    }

    /**
     * Updates the MergedCarrierEntry returned by {@link #getMergedCarrierEntry()) with the current
     * default data subscription ID, or sets it to null if not available.
     */
    @WorkerThread
    private void updateMergedCarrierEntry(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            if (mMergedCarrierEntry == null) {
                return;
            }
            mMergedCarrierEntry = null;
        } else {
            if (mMergedCarrierEntry != null && subId == mMergedCarrierEntry.getSubscriptionId()) {
                return;
            }
            mMergedCarrierEntry = new MergedCarrierEntry(mWorkerHandler, mWifiManager,
                    /* forSavedNetworksPage */ false, mContext, subId);
            Network currentNetwork = mWifiManager.getCurrentNetwork();
            if (currentNetwork != null) {
                NetworkCapabilities networkCapabilities =
                        mConnectivityManager.getNetworkCapabilities(currentNetwork);
                if (networkCapabilities != null) {
                    // getNetworkCapabilities(Network) obfuscates location info such as SSID and
                    // networkId, so we need to set the WifiInfo directly from WifiManager.
                    mMergedCarrierEntry.onNetworkCapabilitiesChanged(currentNetwork,
                            new NetworkCapabilities.Builder(networkCapabilities)
                                    .setTransportInfo(mWifiManager.getConnectionInfo())
                                    .build());
                }
                LinkProperties linkProperties =
                        mConnectivityManager.getLinkProperties(currentNetwork);
                if (linkProperties != null) {
                    mMergedCarrierEntry.updateLinkProperties(currentNetwork, linkProperties);
                }
            }
        }
        notifyOnWifiEntriesChanged();
    }

    /**
     * Get the contextual WifiEntries added according to customized conditions.
     */
    protected List<WifiEntry> getContextualWifiEntries() {
        return Collections.emptyList();
    }

    /**
     * Update the contextual wifi entry according to customized conditions.
     */
    protected void updateContextualWifiEntryScans(@NonNull List<ScanResult> scanResults) {
        // do nothing
    }

    /**
     * Updates or removes scan results for the corresponding StandardWifiEntries.
     * New entries will be created for scan results without an existing entry.
     * Unreachable entries will be removed.
     *
     * @param scanResults List of valid scan results to convey as StandardWifiEntries
     */
    @WorkerThread
    private void updateStandardWifiEntryScans(@NonNull List<ScanResult> scanResults) {
        checkNotNull(scanResults, "Scan Result list should not be null!");

        // Group scans by ScanResultKey key
        final Map<ScanResultKey, List<ScanResult>> scanResultsByKey = scanResults.stream()
                .filter(scan -> !TextUtils.isEmpty(scan.SSID))
                .collect(Collectors.groupingBy(ScanResultKey::new));
        final Set<ScanResultKey> newScanKeys = new ArraySet<>(scanResultsByKey.keySet());

        // Iterate through current entries and update each entry's scan results
        mStandardWifiEntryCache.forEach(entry -> {
            final ScanResultKey scanKey = entry.getStandardWifiEntryKey().getScanResultKey();
            newScanKeys.remove(scanKey);
            // Update scan results if available, or set to null.
            entry.updateScanResultInfo(scanResultsByKey.get(scanKey));
        });
        // Create new StandardWifiEntry objects for each leftover group of scan results.
        for (ScanResultKey scanKey: newScanKeys) {
            final StandardWifiEntryKey entryKey =
                    new StandardWifiEntryKey(scanKey, true /* isTargetingNewNetworks */);
            final StandardWifiEntry newEntry = new StandardWifiEntry(mInjector, mContext,
                    mMainHandler, entryKey, mStandardWifiConfigCache.get(entryKey),
                    scanResultsByKey.get(scanKey), mWifiManager,
                    false /* forSavedNetworksPage */);
            mStandardWifiEntryCache.add(newEntry);
        }

        // Remove any entry that is now unreachable due to no scans or unsupported
        // security types.
        mStandardWifiEntryCache.removeIf(
                entry -> entry.getLevel() == WIFI_LEVEL_UNREACHABLE);
    }

    /**
     * Updates or removes scan results for the corresponding StandardWifiEntries.
     * New entries will be created for scan results without an existing entry.
     * Unreachable entries will be removed.
     *
     * @param scanResults List of valid scan results to convey as StandardWifiEntries
     */
    @WorkerThread
    private void updateSuggestedWifiEntryScans(@NonNull List<ScanResult> scanResults) {
        checkNotNull(scanResults, "Scan Result list should not be null!");

        // Get every ScanResultKey that is user shareable
        final Set<StandardWifiEntryKey> userSharedEntryKeys =
                mWifiManager.getWifiConfigForMatchedNetworkSuggestionsSharedWithUser(scanResults)
                        .stream()
                        .map(StandardWifiEntryKey::new)
                        .collect(Collectors.toSet());

        // Group scans by ScanResultKey key
        final Map<ScanResultKey, List<ScanResult>> scanResultsByKey = scanResults.stream()
                .filter(scan -> !TextUtils.isEmpty(scan.SSID))
                .collect(Collectors.groupingBy(ScanResultKey::new));

        // Iterate through current entries and update each entry's scan results and shareability.
        final Set<StandardWifiEntryKey> seenEntryKeys = new ArraySet<>();
        mSuggestedWifiEntryCache.forEach(entry -> {
            final StandardWifiEntryKey entryKey = entry.getStandardWifiEntryKey();
            seenEntryKeys.add(entryKey);
            // Update scan results if available, or set to null.
            entry.updateScanResultInfo(scanResultsByKey.get(entryKey.getScanResultKey()));
            entry.setUserShareable(userSharedEntryKeys.contains(entryKey));
        });
        // Create new StandardWifiEntry objects for each leftover config with scan results.
        for (StandardWifiEntryKey entryKey : mSuggestedConfigCache.keySet()) {
            final ScanResultKey scanKey = entryKey.getScanResultKey();
            if (seenEntryKeys.contains(entryKey)
                    || !scanResultsByKey.containsKey(scanKey)) {
                continue;
            }
            final StandardWifiEntry newEntry = new StandardWifiEntry(mInjector, mContext,
                    mMainHandler, entryKey, mSuggestedConfigCache.get(entryKey),
                    scanResultsByKey.get(scanKey), mWifiManager,
                    false /* forSavedNetworksPage */);
            newEntry.setUserShareable(userSharedEntryKeys.contains(entryKey));
            mSuggestedWifiEntryCache.add(newEntry);
        }

        // Remove any entry that is now unreachable due to no scans or unsupported
        // security types.
        mSuggestedWifiEntryCache.removeIf(entry -> entry.getLevel() == WIFI_LEVEL_UNREACHABLE);
    }

    @WorkerThread
    private void updatePasspointWifiEntryScans(@NonNull List<ScanResult> scanResults) {
        checkNotNull(scanResults, "Scan Result list should not be null!");

        Set<String> seenKeys = new TreeSet<>();
        List<Pair<WifiConfiguration, Map<Integer, List<ScanResult>>>> matchingWifiConfigs =
                mWifiManager.getAllMatchingWifiConfigs(scanResults);

        for (Pair<WifiConfiguration, Map<Integer, List<ScanResult>>> pair : matchingWifiConfigs) {
            final WifiConfiguration wifiConfig = pair.first;
            final List<ScanResult> homeScans =
                    pair.second.get(WifiManager.PASSPOINT_HOME_NETWORK);
            final List<ScanResult> roamingScans =
                    pair.second.get(WifiManager.PASSPOINT_ROAMING_NETWORK);
            final String key = uniqueIdToPasspointWifiEntryKey(wifiConfig.getKey());
            seenKeys.add(key);

            // Create PasspointWifiEntry if one doesn't exist for the seen key yet.
            if (!mPasspointWifiEntryCache.containsKey(key)) {
                if (wifiConfig.fromWifiNetworkSuggestion) {
                    mPasspointWifiEntryCache.put(key, new PasspointWifiEntry(mInjector, mContext,
                            mMainHandler, wifiConfig, mWifiManager,
                            false /* forSavedNetworksPage */));
                } else if (mPasspointConfigCache.containsKey(key)) {
                    mPasspointWifiEntryCache.put(key, new PasspointWifiEntry(mInjector, mContext,
                            mMainHandler, mPasspointConfigCache.get(key), mWifiManager,
                            false /* forSavedNetworksPage */));
                } else {
                    // Failed to find PasspointConfig for a provisioned Passpoint network
                    continue;
                }
            }
            mPasspointWifiEntryCache.get(key).updateScanResultInfo(wifiConfig,
                    homeScans, roamingScans);
        }

        // Remove entries that are now unreachable
        mPasspointWifiEntryCache.entrySet()
                .removeIf(entry -> entry.getValue().getLevel() == WIFI_LEVEL_UNREACHABLE
                        || (!seenKeys.contains(entry.getKey()))
                        && entry.getValue().getConnectedState() == CONNECTED_STATE_DISCONNECTED);
    }

    @WorkerThread
    private void updateOsuWifiEntryScans(@NonNull List<ScanResult> scanResults) {
        checkNotNull(scanResults, "Scan Result list should not be null!");

        Map<OsuProvider, List<ScanResult>> osuProviderToScans =
                mWifiManager.getMatchingOsuProviders(scanResults);
        Map<OsuProvider, PasspointConfiguration> osuProviderToPasspointConfig =
                mWifiManager.getMatchingPasspointConfigsForOsuProviders(
                        osuProviderToScans.keySet());
        // Update each OsuWifiEntry with new scans (or empty scans).
        for (OsuWifiEntry entry : mOsuWifiEntryCache.values()) {
            entry.updateScanResultInfo(osuProviderToScans.remove(entry.getOsuProvider()));
        }

        // Create a new entry for each OsuProvider not already matched to an OsuWifiEntry
        for (OsuProvider provider : osuProviderToScans.keySet()) {
            OsuWifiEntry newEntry = new OsuWifiEntry(mInjector, mContext, mMainHandler, provider,
                    mWifiManager, false /* forSavedNetworksPage */);
            newEntry.updateScanResultInfo(osuProviderToScans.get(provider));
            mOsuWifiEntryCache.put(osuProviderToOsuWifiEntryKey(provider), newEntry);
        }

        // Pass a reference of each OsuWifiEntry to any matching provisioned PasspointWifiEntries
        // for expiration handling.
        mOsuWifiEntryCache.values().forEach(osuEntry -> {
            PasspointConfiguration provisionedConfig =
                    osuProviderToPasspointConfig.get(osuEntry.getOsuProvider());
            if (provisionedConfig == null) {
                osuEntry.setAlreadyProvisioned(false);
                return;
            }
            osuEntry.setAlreadyProvisioned(true);
            PasspointWifiEntry provisionedEntry = mPasspointWifiEntryCache.get(
                    uniqueIdToPasspointWifiEntryKey(provisionedConfig.getUniqueId()));
            if (provisionedEntry == null) {
                return;
            }
            provisionedEntry.setOsuWifiEntry(osuEntry);
        });

        // Remove entries that are now unreachable
        mOsuWifiEntryCache.entrySet()
                .removeIf(entry -> entry.getValue().getLevel() == WIFI_LEVEL_UNREACHABLE);
    }

    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    private void updateKnownNetworkEntryScans(@NonNull List<ScanResult> scanResults) {
        checkNotNull(scanResults, "Scan Result list should not be null!");

        // Group scans by ScanResultKey key
        final Map<ScanResultKey, List<ScanResult>> scanResultsByKey = scanResults.stream()
                .filter(scan -> !TextUtils.isEmpty(scan.SSID))
                .collect(Collectors.groupingBy(ScanResultKey::new));

        // Create a map of KnownNetwork data by ScanResultKey
        final Map<ScanResultKey, KnownNetwork> knownNetworkDataByKey =
                mKnownNetworkDataCache.stream().collect(Collectors.toMap(
                        data -> new ScanResultKey(data.getSsid(),
                                new ArrayList<>(data.getSecurityTypes())),
                        data -> data,
                        (data1, data2) -> {
                            Log.e(TAG,
                                    "Encountered duplicate key data in "
                                            + "updateKnownNetworkEntryScans");
                            return data1; // When duplicate data is encountered, use first one.
                        }));

        // Create set of ScanResultKeys for known networks from service that are included in scan
        final Set<ScanResultKey> newScanKeys = knownNetworkDataByKey.keySet().stream().filter(
                scanResultsByKey::containsKey).collect(Collectors.toSet());

        // Iterate through current entries and update each entry's scan results
        mKnownNetworkEntryCache.forEach(entry -> {
            final ScanResultKey scanKey = entry.getStandardWifiEntryKey().getScanResultKey();
            newScanKeys.remove(scanKey);
            // Update scan results if available, or set to null.
            entry.updateScanResultInfo(scanResultsByKey.get(scanKey));
        });

        // Get network and capabilities if new network entries are being created
        Network network = null;
        NetworkCapabilities capabilities = null;
        if (!newScanKeys.isEmpty()) {
            network = mWifiManager.getCurrentNetwork();
            if (network != null) {
                capabilities = mConnectivityManager.getNetworkCapabilities(network);
                if (capabilities != null) {
                    // getNetworkCapabilities(Network) obfuscates location info such as SSID and
                    // networkId, so we need to set the WifiInfo directly from WifiManager.
                    capabilities = new NetworkCapabilities.Builder(capabilities).setTransportInfo(
                            mWifiManager.getConnectionInfo()).build();
                }
            }
        }

        // Create new KnownNetworkEntry objects for each leftover group of scan results.
        for (ScanResultKey scanKey : newScanKeys) {
            final StandardWifiEntryKey entryKey =
                    new StandardWifiEntryKey(scanKey, true /* isTargetingNewNetworks */);
            final KnownNetworkEntry newEntry = new KnownNetworkEntry(mInjector, mContext,
                    mMainHandler, entryKey, null /* configs */,
                    scanResultsByKey.get(scanKey), mWifiManager,
                    mSharedConnectivityManager, knownNetworkDataByKey.get(scanKey));
            if (network != null && capabilities != null) {
                newEntry.onNetworkCapabilitiesChanged(network, capabilities);
            }
            mKnownNetworkEntryCache.add(newEntry);
        }

        // Remove any entry that is now unreachable due to no scans or unsupported
        // security types.
        mKnownNetworkEntryCache.removeIf(
                entry -> entry.getLevel() == WIFI_LEVEL_UNREACHABLE);
    }

    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    private void updateHotspotNetworkEntries() {
        // Map HotspotNetwork data by deviceID
        final Map<Long, HotspotNetwork> hotspotNetworkDataById =
                mHotspotNetworkDataCache.stream().collect(Collectors.toMap(
                        HotspotNetwork::getDeviceId,
                        data -> data,
                        (data1, data2) -> {
                            Log.e(TAG,
                                    "Encountered duplicate key data in "
                                            + "updateHotspotNetworkEntries");
                            return data1; // When duplicate data is encountered, use first one.
                        }));
        final Set<Long> newDeviceIds = new ArraySet<>(hotspotNetworkDataById.keySet());

        // Remove entries not in latest data set from service
        mHotspotNetworkEntryCache.removeIf(
                entry -> !newDeviceIds.contains(entry.getHotspotNetworkEntryKey().getDeviceId()));

        // Iterate through entries and update HotspotNetwork data
        mHotspotNetworkEntryCache.forEach(entry -> {
            final Long deviceId = entry.getHotspotNetworkEntryKey().getDeviceId();
            newDeviceIds.remove(deviceId);
            entry.updateHotspotNetworkData(hotspotNetworkDataById.get(deviceId));
        });

        // Get network and capabilities if new network entries are being created
        Network network = null;
        NetworkCapabilities capabilities = null;
        if (!newDeviceIds.isEmpty()) {
            network = mWifiManager.getCurrentNetwork();
            if (network != null) {
                capabilities = mConnectivityManager.getNetworkCapabilities(network);
                if (capabilities != null) {
                    // getNetworkCapabilities(Network) obfuscates location info such as SSID and
                    // networkId, so we need to set the WifiInfo directly from WifiManager.
                    capabilities = new NetworkCapabilities.Builder(capabilities).setTransportInfo(
                            mWifiManager.getConnectionInfo()).build();
                }
            }
        }

        // Create new HotspotNetworkEntry objects for each new device ID
        for (Long deviceId : newDeviceIds) {
            final HotspotNetworkEntry newEntry = new HotspotNetworkEntry(mInjector, mContext,
                    mMainHandler, mWifiManager, mSharedConnectivityManager,
                    hotspotNetworkDataById.get(deviceId));
            if (network != null && capabilities != null) {
                newEntry.onNetworkCapabilitiesChanged(network, capabilities);
            }
            mHotspotNetworkEntryCache.add(newEntry);
        }
    }

    @WorkerThread
    private void updateNetworkRequestEntryScans(@NonNull List<ScanResult> scanResults) {
        checkNotNull(scanResults, "Scan Result list should not be null!");
        if (mNetworkRequestEntry == null) {
            return;
        }

        final ScanResultKey scanKey =
                mNetworkRequestEntry.getStandardWifiEntryKey().getScanResultKey();
        List<ScanResult> matchedScans = scanResults.stream()
                .filter(scan -> scanKey.equals(new ScanResultKey(scan)))
                .collect(toList());
        mNetworkRequestEntry.updateScanResultInfo(matchedScans);
    }

    /**
     * Conditionally updates the WifiEntry scan results based on the current wifi state and
     * whether the last scan succeeded or not.
     */
    @WorkerThread
    private void conditionallyUpdateScanResults(boolean lastScanSucceeded) {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            updateStandardWifiEntryScans(Collections.emptyList());
            updateSuggestedWifiEntryScans(Collections.emptyList());
            updatePasspointWifiEntryScans(Collections.emptyList());
            updateOsuWifiEntryScans(Collections.emptyList());
            if (mEnableSharedConnectivityFeature && BuildCompat.isAtLeastU()) {
                mKnownNetworkEntryCache.clear();
                mHotspotNetworkEntryCache.clear();
            }
            updateNetworkRequestEntryScans(Collections.emptyList());
            updateContextualWifiEntryScans(Collections.emptyList());
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

        List<ScanResult> scanResults = mScanResultUpdater.getScanResults(scanAgeWindow);
        updateStandardWifiEntryScans(scanResults);
        updateSuggestedWifiEntryScans(scanResults);
        updatePasspointWifiEntryScans(scanResults);
        updateOsuWifiEntryScans(scanResults);
        if (mEnableSharedConnectivityFeature && BuildCompat.isAtLeastU()) {
            updateKnownNetworkEntryScans(scanResults);
            // Updating the hotspot entries here makes the UI more reliable when switching pages or
            // when toggling settings while the internet picker is shown.
            updateHotspotNetworkEntries();
        }
        updateNetworkRequestEntryScans(scanResults);
        updateContextualWifiEntryScans(scanResults);
    }

    /**
     * Updates the WifiConfiguration caches for saved/ephemeral/suggested networks and updates the
     * corresponding WifiEntries with the new configs.
     *
     * @param configs List of all saved/ephemeral/suggested WifiConfigurations
     */
    @WorkerThread
    private void updateWifiConfigurations(@NonNull List<WifiConfiguration> configs) {
        checkNotNull(configs, "Config list should not be null!");
        mStandardWifiConfigCache.clear();
        mSuggestedConfigCache.clear();
        mNetworkRequestConfigCache.clear();
        for (WifiConfiguration config : configs) {
            if (config.carrierMerged) {
                continue;
            }
            StandardWifiEntryKey standardWifiEntryKey =
                    new StandardWifiEntryKey(config, true /* isTargetingNewNetworks */);
            if (config.isPasspoint()) {
                mPasspointWifiConfigCache.put(config.networkId, config);
            } else if (config.fromWifiNetworkSuggestion) {
                if (!mSuggestedConfigCache.containsKey(standardWifiEntryKey)) {
                    mSuggestedConfigCache.put(standardWifiEntryKey, new ArrayList<>());
                }
                mSuggestedConfigCache.get(standardWifiEntryKey).add(config);
            } else if (config.fromWifiNetworkSpecifier) {
                if (!mNetworkRequestConfigCache.containsKey(standardWifiEntryKey)) {
                    mNetworkRequestConfigCache.put(standardWifiEntryKey, new ArrayList<>());
                }
                mNetworkRequestConfigCache.get(standardWifiEntryKey).add(config);
            } else {
                if (!mStandardWifiConfigCache.containsKey(standardWifiEntryKey)) {
                    mStandardWifiConfigCache.put(standardWifiEntryKey, new ArrayList<>());
                }
                mStandardWifiConfigCache.get(standardWifiEntryKey).add(config);
            }
        }
        mNumSavedNetworks = (int) mStandardWifiConfigCache.values().stream()
                .flatMap(List::stream)
                .filter(config -> !config.isEphemeral())
                .map(config -> config.networkId)
                .distinct()
                .count();

        // Iterate through current entries and update each entry's config
        mStandardWifiEntryCache.forEach(entry ->
                entry.updateConfig(mStandardWifiConfigCache.get(entry.getStandardWifiEntryKey())));

        // Iterate through current suggestion entries and update each entry's config
        mSuggestedWifiEntryCache.removeIf(entry -> {
            entry.updateConfig(mSuggestedConfigCache.get(entry.getStandardWifiEntryKey()));
            // Remove if the suggestion does not have a config anymore.
            return !entry.isSuggestion();
        });
        // Update suggestion scans to make sure we mark which suggestions are user-shareable.
        updateSuggestedWifiEntryScans(mScanResultUpdater.getScanResults());

        if (mNetworkRequestEntry != null) {
            mNetworkRequestEntry.updateConfig(
                    mNetworkRequestConfigCache.get(mNetworkRequestEntry.getStandardWifiEntryKey()));
        }
    }

    @WorkerThread
    private void updatePasspointConfigurations(@NonNull List<PasspointConfiguration> configs) {
        checkNotNull(configs, "Config list should not be null!");
        mPasspointConfigCache.clear();
        mPasspointConfigCache.putAll(configs.stream().collect(
                toMap(config -> uniqueIdToPasspointWifiEntryKey(
                        config.getUniqueId()), Function.identity())));

        // Iterate through current entries and update each entry's config or remove if no config
        // matches the entry anymore.
        mPasspointWifiEntryCache.entrySet().removeIf((entry) -> {
            final PasspointWifiEntry wifiEntry = entry.getValue();
            final String key = wifiEntry.getKey();
            wifiEntry.updatePasspointConfig(mPasspointConfigCache.get(key));
            return !wifiEntry.isSubscription() && !wifiEntry.isSuggestion();
        });
    }

    /**
     * Updates all matching WifiEntries with the given network capabilities. If there are
     * currently no matching WifiEntries, then a new WifiEntry will be created for the capabilities.
     * @param network Network for which the NetworkCapabilities have changed.
     * @param capabilities NetworkCapabilities that have changed.
     */
    @WorkerThread
    private void updateNetworkCapabilities(
            @NonNull Network network, @NonNull NetworkCapabilities capabilities) {
        if (mStandardWifiConfigCache.size()
                + mSuggestedConfigCache.size() + mPasspointWifiConfigCache.size()
                + mNetworkRequestConfigCache.size() == 0) {
            // We're connected but don't have any configured networks, so fetch the list of configs
            // again. This can happen when we fetch the configured networks after SSR, but the Wifi
            // thread times out waiting for driver restart and returns an empty list of networks.
            updateWifiConfigurations(mWifiManager.getPrivilegedConfiguredNetworks());
        }
        for (WifiEntry entry : getAllWifiEntries()) {
            entry.onNetworkCapabilitiesChanged(network, capabilities);
        }
        // Create a WifiEntry for the current connection if there are no scan results yet.
        conditionallyCreateConnectedStandardWifiEntry(network, capabilities);
        conditionallyCreateConnectedSuggestedWifiEntry(network, capabilities);
        conditionallyCreateConnectedPasspointWifiEntry(network, capabilities);
        conditionallyCreateConnectedNetworkRequestEntry(network, capabilities);
    }

    /**
     * Updates the connection info of the current NetworkRequestEntry. A new NetworkRequestEntry is
     * created if there is no existing entry, or the existing entry doesn't match WifiInfo.
     */
    @WorkerThread
    private void conditionallyCreateConnectedNetworkRequestEntry(
            @NonNull Network network, @NonNull NetworkCapabilities capabilities) {
        final List<WifiConfiguration> matchingConfigs = new ArrayList<>();

        WifiInfo wifiInfo = Utils.getWifiInfo(capabilities);
        if (wifiInfo != null) {
            for (int i = 0; i < mNetworkRequestConfigCache.size(); i++) {
                final List<WifiConfiguration> configs = mNetworkRequestConfigCache.valueAt(i);
                if (!configs.isEmpty() && configs.get(0).networkId == wifiInfo.getNetworkId()) {
                    matchingConfigs.addAll(configs);
                    break;
                }
            }
        }
        if (matchingConfigs.isEmpty()) {
            return;
        }

        // WifiInfo matches a request config, create a NetworkRequestEntry or update the existing.
        final StandardWifiEntryKey entryKey = new StandardWifiEntryKey(matchingConfigs.get(0));
        if (mNetworkRequestEntry == null
                || !mNetworkRequestEntry.getStandardWifiEntryKey().equals(entryKey)) {
            mNetworkRequestEntry = new NetworkRequestEntry(mInjector, mContext, mMainHandler,
                    entryKey, mWifiManager, false /* forSavedNetworksPage */);
            mNetworkRequestEntry.updateConfig(matchingConfigs);
            updateNetworkRequestEntryScans(mScanResultUpdater.getScanResults());
        }
        mNetworkRequestEntry.onNetworkCapabilitiesChanged(network, capabilities);
    }

    /**
     * If the given network is a standard Wi-Fi network and there are no scan results for this
     * network yet, create and cache a new StandardWifiEntry for it.
     */
    @WorkerThread
    private void conditionallyCreateConnectedStandardWifiEntry(
            @NonNull Network network, @NonNull NetworkCapabilities capabilities) {
        WifiInfo wifiInfo = Utils.getWifiInfo(capabilities);
        if (wifiInfo == null || wifiInfo.isPasspointAp() || wifiInfo.isOsuAp()) {
            return;
        }

        final int connectedNetId = wifiInfo.getNetworkId();
        for (List<WifiConfiguration> configs : mStandardWifiConfigCache.values()) {
            // List of configs match as long as one of them matches the connected network ID.
            if (configs.stream()
                    .map(config -> config.networkId)
                    .filter(networkId -> networkId == connectedNetId)
                    .count() == 0) {
                continue;
            }
            final StandardWifiEntryKey entryKey =
                    new StandardWifiEntryKey(configs.get(0), true /* isTargetingNewNetworks */);
            for (StandardWifiEntry existingEntry : mStandardWifiEntryCache) {
                if (entryKey.equals(existingEntry.getStandardWifiEntryKey())) {
                    return;
                }
            }
            final StandardWifiEntry connectedEntry =
                    new StandardWifiEntry(mInjector, mContext, mMainHandler, entryKey, configs,
                            null, mWifiManager, false /* forSavedNetworksPage */);
            connectedEntry.onNetworkCapabilitiesChanged(network, capabilities);
            mStandardWifiEntryCache.add(connectedEntry);
            return;
        }
    }

    /**
     * If the given network is a suggestion network and there are no scan results for this network
     * yet, create and cache a new StandardWifiEntry for it.
     */
    @WorkerThread
    private void conditionallyCreateConnectedSuggestedWifiEntry(
            @NonNull Network network, @NonNull NetworkCapabilities capabilities) {
        WifiInfo wifiInfo = Utils.getWifiInfo(capabilities);
        if (wifiInfo == null || wifiInfo.isPasspointAp() || wifiInfo.isOsuAp()) {
            return;
        }
        final int connectedNetId = wifiInfo.getNetworkId();
        for (List<WifiConfiguration> configs : mSuggestedConfigCache.values()) {
            if (configs.isEmpty() || configs.get(0).networkId != connectedNetId) {
                continue;
            }
            final StandardWifiEntryKey entryKey =
                    new StandardWifiEntryKey(configs.get(0), true /* isTargetingNewNetworks */);
            for (StandardWifiEntry existingEntry : mSuggestedWifiEntryCache) {
                if (entryKey.equals(existingEntry.getStandardWifiEntryKey())) {
                    return;
                }
            }
            final StandardWifiEntry connectedEntry =
                    new StandardWifiEntry(mInjector, mContext, mMainHandler, entryKey, configs,
                            null, mWifiManager, false /* forSavedNetworksPage */);
            connectedEntry.onNetworkCapabilitiesChanged(network, capabilities);
            mSuggestedWifiEntryCache.add(connectedEntry);
            return;
        }
    }

    /**
     * If the given network is a Passpoint network and there are no scan results for this network
     * yet, create and cache a new StandardWifiEntry for it.
     */
    @WorkerThread
    private void conditionallyCreateConnectedPasspointWifiEntry(
            @NonNull Network network, @NonNull NetworkCapabilities capabilities) {
        WifiInfo wifiInfo = Utils.getWifiInfo(capabilities);
        if (wifiInfo == null || !wifiInfo.isPasspointAp()) {
            return;
        }

        WifiConfiguration cachedWifiConfig = mPasspointWifiConfigCache.get(wifiInfo.getNetworkId());
        if (cachedWifiConfig == null) {
            return;
        }
        final String key = uniqueIdToPasspointWifiEntryKey(cachedWifiConfig.getKey());
        if (mPasspointWifiEntryCache.containsKey(key)) {
            // Entry already exists, skip creating a new one.
            return;
        }
        PasspointConfiguration passpointConfig = mPasspointConfigCache.get(
                uniqueIdToPasspointWifiEntryKey(cachedWifiConfig.getKey()));
        PasspointWifiEntry connectedEntry;
        if (passpointConfig != null) {
            connectedEntry = new PasspointWifiEntry(mInjector, mContext, mMainHandler,
                    passpointConfig, mWifiManager,
                    false /* forSavedNetworksPage */);
        } else {
            // Suggested PasspointWifiEntry without a corresponding PasspointConfiguration
            connectedEntry = new PasspointWifiEntry(mInjector, mContext, mMainHandler,
                    cachedWifiConfig, mWifiManager,
                    false /* forSavedNetworksPage */);
        }
        connectedEntry.onNetworkCapabilitiesChanged(network, capabilities);
        mPasspointWifiEntryCache.put(connectedEntry.getKey(), connectedEntry);
    }

    /**
     * Posts onWifiEntryChanged callback on the main thread.
     */
    @WorkerThread
    private void notifyOnWifiEntriesChanged() {
        if (mListener != null) {
            mMainHandler.post(mListener::onWifiEntriesChanged);
        }
    }

    /**
     * Posts onNumSavedNetworksChanged callback on the main thread.
     */
    @WorkerThread
    private void notifyOnNumSavedNetworksChanged() {
        if (mListener != null) {
            mMainHandler.post(mListener::onNumSavedNetworksChanged);
        }
    }

    /**
     * Posts onNumSavedSubscriptionsChanged callback on the main thread.
     */
    @WorkerThread
    private void notifyOnNumSavedSubscriptionsChanged() {
        if (mListener != null) {
            mMainHandler.post(mListener::onNumSavedSubscriptionsChanged);
        }
    }

    /**
     * Listener for changes to the list of visible WifiEntries as well as the number of saved
     * networks and subscriptions.
     *
     * These callbacks must be run on the MainThread.
     */
    public interface WifiPickerTrackerCallback extends BaseWifiTracker.BaseWifiTrackerCallback {
        /**
         * Called when there are changes to
         *      {@link #getConnectedWifiEntry()}
         *      {@link #getWifiEntries()}
         *      {@link #getMergedCarrierEntry()}
         */
        @MainThread
        void onWifiEntriesChanged();

        /**
         * Called when there are changes to
         *      {@link #getNumSavedNetworks()}
         */
        @MainThread
        void onNumSavedNetworksChanged();

        /**
         * Called when there are changes to
         *      {@link #getNumSavedSubscriptions()}
         */
        @MainThread
        void onNumSavedSubscriptionsChanged();
    }
}

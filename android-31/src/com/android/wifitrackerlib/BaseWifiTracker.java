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

import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;

import static java.util.stream.Collectors.toList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkKey;
import android.net.NetworkRequest;
import android.net.NetworkScoreManager;
import android.net.ScoredNetwork;
import android.net.TransportInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkScoreCache;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for WifiTracker functionality.
 *
 * This class provides the basic functions of issuing scans, receiving Wi-Fi related broadcasts, and
 * keeping track of the Wi-Fi state.
 *
 * Subclasses are expected to provide their own API to clients and override the empty broadcast
 * handling methods here to populate the data returned by their API.
 *
 * This class runs on two threads:
 *
 * The main thread
 * - Processes lifecycle events (onStart, onStop)
 * - Runs listener callbacks
 *
 * The worker thread
 * - Drives the periodic scan requests
 * - Handles the system broadcasts to update the API return values
 * - Notifies the listener for updates to the API return values
 *
 * To keep synchronization simple, this means that the vast majority of work is done within the
 * worker thread. Synchronized blocks are only to be used for data returned by the API updated by
 * the worker thread and consumed by the main thread.
*/

public class BaseWifiTracker implements LifecycleObserver {
    private final String mTag;

    private static boolean sVerboseLogging;

    public static boolean isVerboseLoggingEnabled() {
        return BaseWifiTracker.sVerboseLogging;
    }

    private boolean mIsStarted;

    // Registered on the worker thread
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        @WorkerThread
        public void onReceive(Context context, Intent intent) {
            if (!mIsStarted) {
                mIsStarted = true;
                handleOnStart();
            }

            String action = intent.getAction();

            if (isVerboseLoggingEnabled()) {
                Log.v(mTag, "Received broadcast: " + action);
            }

            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                    mScanner.start();
                } else {
                    mScanner.stop();
                }
                notifyOnWifiStateChanged();
                handleWifiStateChangedAction();
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                mNetworkScoreManager.requestScores(mWifiManager.getScanResults().stream()
                        .map(NetworkKey::createFromScanResult)
                        .filter(mRequestedScoreKeys::add)
                        .collect(toList()));
                handleScanResultsAvailableAction(intent);
            } else if (WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION.equals(action)) {
                handleConfiguredNetworksChangedAction(intent);
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                handleNetworkStateChangedAction(intent);
            } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
                handleRssiChangedAction();
            } else if (TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
                handleDefaultSubscriptionChanged(intent.getIntExtra(
                        "subscription", SubscriptionManager.INVALID_SUBSCRIPTION_ID));
            }
        }
    };
    private final BaseWifiTracker.Scanner mScanner;
    private final BaseWifiTrackerCallback mListener;

    protected final Context mContext;
    protected final WifiManager mWifiManager;
    protected final ConnectivityManager mConnectivityManager;
    protected final NetworkScoreManager mNetworkScoreManager;
    protected final Handler mMainHandler;
    protected final Handler mWorkerHandler;
    protected final long mMaxScanAgeMillis;
    protected final long mScanIntervalMillis;
    protected final ScanResultUpdater mScanResultUpdater;
    protected final WifiNetworkScoreCache mWifiNetworkScoreCache;
    protected boolean mIsWifiValidated;
    protected boolean mIsWifiDefaultRoute;
    protected boolean mIsCellDefaultRoute;
    private final Set<NetworkKey> mRequestedScoreKeys = new HashSet<>();

    // Network request for listening on changes to Wifi link properties and network capabilities
    // such as captive portal availability.
    private final NetworkRequest mNetworkRequest = new NetworkRequest.Builder()
            .clearCapabilities()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .addTransportType(TRANSPORT_WIFI)
            .build();

    private final ConnectivityManager.NetworkCallback mNetworkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                @WorkerThread
                public void onLinkPropertiesChanged(@NonNull Network network,
                        @NonNull LinkProperties lp) {
                    if (!mIsStarted) {
                        mIsStarted = true;
                        handleOnStart();
                    }
                    if (!isPrimaryWifiNetwork(
                            mConnectivityManager.getNetworkCapabilities(network))) {
                        return;
                    }
                    handleLinkPropertiesChanged(lp);
                }

                @Override
                @WorkerThread
                public void onCapabilitiesChanged(@NonNull Network network,
                        @NonNull NetworkCapabilities networkCapabilities) {
                    if (!mIsStarted) {
                        mIsStarted = true;
                        handleOnStart();
                    }
                    if (!isPrimaryWifiNetwork(networkCapabilities)) {
                        return;
                    }
                    final boolean oldWifiValidated = mIsWifiValidated;
                    mIsWifiValidated = networkCapabilities.hasCapability(NET_CAPABILITY_VALIDATED);
                    if (isVerboseLoggingEnabled() && mIsWifiValidated != oldWifiValidated) {
                        Log.v(mTag, "Is Wifi validated: " + mIsWifiValidated);
                    }
                    handleNetworkCapabilitiesChanged(networkCapabilities);
                }

                @Override
                @WorkerThread
                public void onLost(@NonNull Network network) {
                    if (!mIsStarted) {
                        mIsStarted = true;
                        handleOnStart();
                    }
                    if (!isPrimaryWifiNetwork(
                            mConnectivityManager.getNetworkCapabilities(network))) {
                        return;
                    }
                    mIsWifiValidated = false;
                }
            };

    private final ConnectivityManager.NetworkCallback mDefaultNetworkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                @WorkerThread
                public void onCapabilitiesChanged(@NonNull Network network,
                        @NonNull NetworkCapabilities networkCapabilities) {
                    if (!mIsStarted) {
                        mIsStarted = true;
                        handleOnStart();
                    }
                    final boolean oldWifiDefault = mIsWifiDefaultRoute;
                    final boolean oldCellDefault = mIsCellDefaultRoute;
                    // raw Wifi or VPN-over-Wifi is default => Wifi is default.
                    mIsWifiDefaultRoute = networkCapabilities.hasTransport(TRANSPORT_WIFI);
                    mIsCellDefaultRoute = networkCapabilities.hasTransport(TRANSPORT_CELLULAR);
                    if (mIsWifiDefaultRoute != oldWifiDefault
                            || mIsCellDefaultRoute != oldCellDefault) {
                        if (isVerboseLoggingEnabled()) {
                            Log.v(mTag, "Wifi is the default route: " + mIsWifiDefaultRoute);
                            Log.v(mTag, "Cell is the default route: " + mIsCellDefaultRoute);
                        }
                        handleDefaultRouteChanged();
                    }
                }

                @WorkerThread
                public void onLost(@NonNull Network network) {
                    if (!mIsStarted) {
                        mIsStarted = true;
                        handleOnStart();
                    }
                    mIsWifiDefaultRoute = false;
                    mIsCellDefaultRoute = false;
                    if (isVerboseLoggingEnabled()) {
                        Log.v(mTag, "Wifi is the default route: false");
                        Log.v(mTag, "Cell is the default route: false");
                    }
                    handleDefaultRouteChanged();
                }
            };

    private boolean isPrimaryWifiNetwork(@Nullable NetworkCapabilities networkCapabilities) {
        if (networkCapabilities == null) {
            return false;
        }
        final TransportInfo transportInfo = networkCapabilities.getTransportInfo();
        if (!(transportInfo instanceof WifiInfo)) {
            return false;
        }
        return ((WifiInfo) transportInfo).isPrimary();
    }

    /**
     * Constructor for BaseWifiTracker.
     *
     * @param lifecycle Lifecycle this is tied to for lifecycle callbacks.
     * @param context Context for registering broadcast receiver and for resource strings.
     * @param wifiManager Provides all Wi-Fi info.
     * @param connectivityManager Provides network info.
     * @param networkScoreManager Provides network scores for network badging.
     * @param mainHandler Handler for processing listener callbacks.
     * @param workerHandler Handler for processing all broadcasts and running the Scanner.
     * @param clock Clock used for evaluating the age of scans
     * @param maxScanAgeMillis Max age for tracked WifiEntries.
     * @param scanIntervalMillis Interval between initiating scans.
     */
    BaseWifiTracker(@NonNull Lifecycle lifecycle, @NonNull Context context,
            @NonNull WifiManager wifiManager,
            @NonNull ConnectivityManager connectivityManager,
            @NonNull NetworkScoreManager networkScoreManager,
            @NonNull Handler mainHandler,
            @NonNull Handler workerHandler,
            @NonNull Clock clock,
            long maxScanAgeMillis,
            long scanIntervalMillis,
            BaseWifiTrackerCallback listener,
            String tag) {
        lifecycle.addObserver(this);
        mContext = context;
        mWifiManager = wifiManager;
        mConnectivityManager = connectivityManager;
        mNetworkScoreManager = networkScoreManager;
        mMainHandler = mainHandler;
        mWorkerHandler = workerHandler;
        mMaxScanAgeMillis = maxScanAgeMillis;
        mScanIntervalMillis = scanIntervalMillis;
        mListener = listener;
        mTag = tag;

        mScanResultUpdater = new ScanResultUpdater(clock,
                maxScanAgeMillis + scanIntervalMillis);
        mWifiNetworkScoreCache = new WifiNetworkScoreCache(mContext,
                new WifiNetworkScoreCache.CacheListener(mWorkerHandler) {
                    @Override
                    public void networkCacheUpdated(List<ScoredNetwork> networks) {
                        handleNetworkScoreCacheUpdated();
                    }
                });
        mScanner = new BaseWifiTracker.Scanner(workerHandler.getLooper());
        sVerboseLogging = mWifiManager.isVerboseLoggingEnabled();
    }

    /**
     * Registers the broadcast receiver and network callbacks and starts the scanning mechanism.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    @MainThread
    public void onStart() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter,
                /* broadcastPermission */ null, mWorkerHandler);
        mConnectivityManager.registerNetworkCallback(mNetworkRequest, mNetworkCallback,
                mWorkerHandler);
        mConnectivityManager.registerDefaultNetworkCallback(mDefaultNetworkCallback,
                mWorkerHandler);
        final NetworkCapabilities defaultNetworkCapabilities = mConnectivityManager
                .getNetworkCapabilities(mConnectivityManager.getActiveNetwork());
        if (defaultNetworkCapabilities != null) {
            mIsWifiDefaultRoute = defaultNetworkCapabilities.hasTransport(TRANSPORT_WIFI);
            mIsCellDefaultRoute = defaultNetworkCapabilities.hasTransport(TRANSPORT_CELLULAR);
        } else {
            mIsWifiDefaultRoute = false;
            mIsCellDefaultRoute = false;
        }
        if (isVerboseLoggingEnabled()) {
            Log.v(mTag, "Wifi is the default route: " + mIsWifiDefaultRoute);
            Log.v(mTag, "Cell is the default route: " + mIsCellDefaultRoute);
        }

        mNetworkScoreManager.registerNetworkScoreCache(
                NetworkKey.TYPE_WIFI,
                mWifiNetworkScoreCache,
                NetworkScoreManager.SCORE_FILTER_SCAN_RESULTS);
        mWorkerHandler.post(() -> {
            if (!mIsStarted) {
                mIsStarted = true;
                handleOnStart();
            }
        });
    }

    /**
     * Unregisters the broadcast receiver, network callbacks, and pauses the scanning mechanism.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @MainThread
    public void onStop() {
        mWorkerHandler.post(mScanner::stop);
        mContext.unregisterReceiver(mBroadcastReceiver);
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        mConnectivityManager.unregisterNetworkCallback(mDefaultNetworkCallback);
        mNetworkScoreManager.unregisterNetworkScoreCache(NetworkKey.TYPE_WIFI,
                mWifiNetworkScoreCache);
        mWorkerHandler.post(mRequestedScoreKeys::clear);
        mIsStarted = false;
    }

    /**
     * Returns the state of Wi-Fi as one of the following values.
     *
     * <li>{@link WifiManager#WIFI_STATE_DISABLED}</li>
     * <li>{@link WifiManager#WIFI_STATE_ENABLED}</li>
     * <li>{@link WifiManager#WIFI_STATE_DISABLING}</li>
     * <li>{@link WifiManager#WIFI_STATE_ENABLING}</li>
     * <li>{@link WifiManager#WIFI_STATE_UNKNOWN}</li>
     */
    @AnyThread
    public int getWifiState() {
        return mWifiManager.getWifiState();
    }

    /**
     * Method to run on the worker thread when onStart is invoked.
     * Data that can be updated immediately after onStart should be populated here.
     */
    @WorkerThread
    protected  void handleOnStart() {
        // Do nothing.
    }

    /**
     * Handle receiving the WifiManager.WIFI_STATE_CHANGED_ACTION broadcast
     */
    @WorkerThread
    protected void handleWifiStateChangedAction() {
        // Do nothing.
    }

    /**
     * Handle receiving the WifiManager.SCAN_RESULTS_AVAILABLE_ACTION broadcast
     */
    @WorkerThread
    protected void handleScanResultsAvailableAction(@NonNull Intent intent) {
        // Do nothing.
    }

    /**
     * Handle receiving the WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION broadcast
     */
    @WorkerThread
    protected void handleConfiguredNetworksChangedAction(@NonNull Intent intent) {
        // Do nothing.
    }

    /**
     * Handle receiving the WifiManager.NETWORK_STATE_CHANGED_ACTION broadcast
     */
    @WorkerThread
    protected void handleNetworkStateChangedAction(@NonNull Intent intent) {
        // Do nothing.
    }

    /**
     * Handle receiving the WifiManager.RSSI_CHANGED_ACTION broadcast
     */
    @WorkerThread
    protected void handleRssiChangedAction() {
        // Do nothing.
    }

    /**
     * Handle link property changes for the current connected Wifi network.
     */
    @WorkerThread
    protected void handleLinkPropertiesChanged(@Nullable LinkProperties linkProperties) {
        // Do nothing.
    }

    /**
     * Handle network capability changes for the current connected Wifi network.
     */
    @WorkerThread
    protected void handleNetworkCapabilitiesChanged(@Nullable NetworkCapabilities capabilities) {
        // Do nothing.
    }

    /**
     * Handle when the default route changes. Whether Wifi is the default route is stored in
     * mIsWifiDefaultRoute.
     */
    @WorkerThread
    protected void handleDefaultRouteChanged() {
        // Do nothing.
    }

    /**
     * Handle updates to the Wifi network score cache, which is stored in mWifiNetworkScoreCache
     */
    @WorkerThread
    protected void handleNetworkScoreCacheUpdated() {
        // Do nothing.
    }

    /**
     * Handle updates to the default data subscription id from SubscriptionManager.
     */
    @WorkerThread
    protected void handleDefaultSubscriptionChanged(int defaultSubId) {
        // Do nothing.
    }

    /**
     * Scanner to handle starting scans every SCAN_INTERVAL_MILLIS
     */
    @WorkerThread
    private class Scanner extends Handler {
        private static final int SCAN_RETRY_TIMES = 3;

        private int mRetry = 0;
        private boolean mIsActive;

        private Scanner(Looper looper) {
            super(looper);
        }

        private void start() {
            if (!mIsActive) {
                mIsActive = true;
                if (isVerboseLoggingEnabled()) {
                    Log.v(mTag, "Scanner start");
                }
                postScan();
            }
        }

        private void stop() {
            mIsActive = false;
            if (isVerboseLoggingEnabled()) {
                Log.v(mTag, "Scanner stop");
            }
            mRetry = 0;
            removeCallbacksAndMessages(null);
        }

        private void postScan() {
            if (mWifiManager.startScan()) {
                mRetry = 0;
            } else if (++mRetry >= SCAN_RETRY_TIMES) {
                // TODO(b/70983952): See if toast is needed here
                if (isVerboseLoggingEnabled()) {
                    Log.v(mTag, "Scanner failed to start scan " + mRetry + " times!");
                }
                mRetry = 0;
                return;
            }
            postDelayed(this::postScan, mScanIntervalMillis);
        }
    }

    /**
     * Posts onWifiStateChanged callback on the main thread.
     */
    @WorkerThread
    private void notifyOnWifiStateChanged() {
        if (mListener != null) {
            mMainHandler.post(mListener::onWifiStateChanged);
        }
    }

    /**
     * Base callback handling Wi-Fi state changes
     *
     * Subclasses should extend this for their own needs.
     */
    protected interface BaseWifiTrackerCallback {
        /**
         * Called when the value for {@link #getWifiState() has changed.
         */
        @MainThread
        void onWifiStateChanged();
    }
}

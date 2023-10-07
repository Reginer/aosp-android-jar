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

import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static android.os.Build.VERSION_CODES;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityDiagnosticsManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.net.wifi.sharedconnectivity.app.HotspotNetwork;
import android.net.wifi.sharedconnectivity.app.HotspotNetworkConnectionStatus;
import android.net.wifi.sharedconnectivity.app.KnownNetwork;
import android.net.wifi.sharedconnectivity.app.KnownNetworkConnectionStatus;
import android.net.wifi.sharedconnectivity.app.SharedConnectivityClientCallback;
import android.net.wifi.sharedconnectivity.app.SharedConnectivityManager;
import android.net.wifi.sharedconnectivity.app.SharedConnectivitySettingsState;
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
import androidx.core.os.BuildCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

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

public class BaseWifiTracker {
    private final String mTag;

    private static boolean sVerboseLogging;

    public static boolean mEnableSharedConnectivityFeature = false;

    public static boolean isVerboseLoggingEnabled() {
        return BaseWifiTracker.sVerboseLogging;
    }

    private int mWifiState = WifiManager.WIFI_STATE_DISABLED;

    private boolean mIsInitialized = false;
    private boolean mIsStopped = true;

    // Registered on the worker thread
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        @WorkerThread
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (isVerboseLoggingEnabled()) {
                Log.v(mTag, "Received broadcast: " + action);
            }

            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                mWifiState = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (mWifiState == WifiManager.WIFI_STATE_ENABLED) {
                    mScanner.start();
                } else {
                    mScanner.stop();
                }
                notifyOnWifiStateChanged();
                handleWifiStateChangedAction();
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                handleScanResultsAvailableAction(intent);
            } else if (WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION.equals(action)) {
                handleConfiguredNetworksChangedAction(intent);
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                handleNetworkStateChangedAction(intent);
            } else if (TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
                handleDefaultSubscriptionChanged(intent.getIntExtra(
                        "subscription", SubscriptionManager.INVALID_SUBSCRIPTION_ID));
            }
        }
    };
    private final BaseWifiTracker.Scanner mScanner;
    private final BaseWifiTrackerCallback mListener;

    protected final WifiTrackerInjector mInjector;
    protected final Context mContext;
    protected final WifiManager mWifiManager;
    protected final ConnectivityManager mConnectivityManager;
    protected final ConnectivityDiagnosticsManager mConnectivityDiagnosticsManager;
    protected final Handler mMainHandler;
    protected final Handler mWorkerHandler;
    protected final long mMaxScanAgeMillis;
    protected final long mScanIntervalMillis;
    protected final ScanResultUpdater mScanResultUpdater;

    @Nullable protected SharedConnectivityManager mSharedConnectivityManager = null;

    // Network request for listening on changes to Wifi link properties and network capabilities
    // such as captive portal availability.
    private final NetworkRequest mNetworkRequest = new NetworkRequest.Builder()
            .clearCapabilities()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .addTransportType(TRANSPORT_WIFI)
            .addTransportType(TRANSPORT_CELLULAR) // For VCN-over-Wifi
            .build();

    private final ConnectivityManager.NetworkCallback mNetworkCallback =
            new ConnectivityManager.NetworkCallback(
                    ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO) {
                @Override
                @WorkerThread
                public void onLinkPropertiesChanged(@NonNull Network network,
                        @NonNull LinkProperties lp) {
                    handleLinkPropertiesChanged(network, lp);
                }

                @Override
                @WorkerThread
                public void onCapabilitiesChanged(@NonNull Network network,
                        @NonNull NetworkCapabilities networkCapabilities) {
                    handleNetworkCapabilitiesChanged(network, networkCapabilities);
                }

                @Override
                @WorkerThread
                public void onLost(@NonNull Network network) {
                    handleNetworkLost(network);
                }
            };

    private final ConnectivityManager.NetworkCallback mDefaultNetworkCallback =
            new ConnectivityManager.NetworkCallback(
                    ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO) {
                @Override
                @WorkerThread
                public void onCapabilitiesChanged(@NonNull Network network,
                        @NonNull NetworkCapabilities networkCapabilities) {
                    handleDefaultNetworkCapabilitiesChanged(network, networkCapabilities);
                }

                @WorkerThread
                public void onLost(@NonNull Network network) {
                    handleDefaultNetworkLost();
                }
            };

    private final ConnectivityDiagnosticsManager.ConnectivityDiagnosticsCallback
            mConnectivityDiagnosticsCallback =
            new ConnectivityDiagnosticsManager.ConnectivityDiagnosticsCallback() {
        @Override
        public void onConnectivityReportAvailable(
                @NonNull ConnectivityDiagnosticsManager.ConnectivityReport report) {
            handleConnectivityReportAvailable(report);
        }
    };

    private final Executor mConnectivityDiagnosticsExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            mWorkerHandler.post(command);
        }
    };

    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    private final Executor mSharedConnectivityExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            mWorkerHandler.post(command);
        }
    };

    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Nullable
    private SharedConnectivityClientCallback mSharedConnectivityCallback = null;

    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @NonNull
    private SharedConnectivityClientCallback createSharedConnectivityCallback() {
        return new SharedConnectivityClientCallback() {
            @Override
            public void onHotspotNetworksUpdated(@NonNull List<HotspotNetwork> networks) {
                handleHotspotNetworksUpdated(networks);
            }

            @Override
            public void onKnownNetworksUpdated(@NonNull List<KnownNetwork> networks) {
                handleKnownNetworksUpdated(networks);
            }

            @Override
            public void onSharedConnectivitySettingsChanged(
                    @NonNull SharedConnectivitySettingsState state) {
                handleSharedConnectivitySettingsChanged(state);
            }

            @Override
            public void onHotspotNetworkConnectionStatusChanged(
                    @NonNull HotspotNetworkConnectionStatus status) {
                handleHotspotNetworkConnectionStatusChanged(status);
            }

            @Override
            public void onKnownNetworkConnectionStatusChanged(
                    @NonNull KnownNetworkConnectionStatus status) {
                handleKnownNetworkConnectionStatusChanged(status);
            }

            @Override
            public void onServiceConnected() {
                handleServiceConnected();
            }

            @Override
            public void onServiceDisconnected() {
                handleServiceDisconnected();
            }

            @Override
            public void onRegisterCallbackFailed(Exception exception) {
                handleRegisterCallbackFailed(exception);
            }
        };
    }

    /**
     * Constructor for BaseWifiTracker.
     * @param injector Injector for commonly referenced objects.
     * @param lifecycle Lifecycle this is tied to for lifecycle callbacks.
     * @param context Context for registering broadcast receiver and for resource strings.
     * @param wifiManager Provides all Wi-Fi info.
     * @param connectivityManager Provides network info.
     * @param mainHandler Handler for processing listener callbacks.
     * @param workerHandler Handler for processing all broadcasts and running the Scanner.
     * @param clock Clock used for evaluating the age of scans
     * @param maxScanAgeMillis Max age for tracked WifiEntries.
     * @param scanIntervalMillis Interval between initiating scans.
     */
    @SuppressWarnings("StaticAssignmentInConstructor")
    BaseWifiTracker(
            @NonNull WifiTrackerInjector injector,
            @NonNull Lifecycle lifecycle, @NonNull Context context,
            @NonNull WifiManager wifiManager,
            @NonNull ConnectivityManager connectivityManager,
            @NonNull Handler mainHandler,
            @NonNull Handler workerHandler,
            @NonNull Clock clock,
            long maxScanAgeMillis,
            long scanIntervalMillis,
            BaseWifiTrackerCallback listener,
            String tag) {
        mInjector = injector;
        lifecycle.addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            @MainThread
            public void onStart() {
                BaseWifiTracker.this.onStart();
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            @MainThread
            public void onStop() {
                BaseWifiTracker.this.onStop();
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            @MainThread
            public void onDestroy() {
                BaseWifiTracker.this.onDestroy();
            }
        });
        mContext = context;
        mWifiManager = wifiManager;
        mConnectivityManager = connectivityManager;
        mConnectivityDiagnosticsManager =
                context.getSystemService(ConnectivityDiagnosticsManager.class);
        if (mEnableSharedConnectivityFeature && BuildCompat.isAtLeastU()) {
            mSharedConnectivityManager = context.getSystemService(SharedConnectivityManager.class);
            mSharedConnectivityCallback = createSharedConnectivityCallback();
        }
        mMainHandler = mainHandler;
        mWorkerHandler = workerHandler;
        mMaxScanAgeMillis = maxScanAgeMillis;
        mScanIntervalMillis = scanIntervalMillis;
        mListener = listener;
        mTag = tag;

        mScanResultUpdater = new ScanResultUpdater(clock,
                maxScanAgeMillis + scanIntervalMillis);
        mScanner = new BaseWifiTracker.Scanner(workerHandler.getLooper());
        sVerboseLogging = mWifiManager.isVerboseLoggingEnabled();
    }

    /**
     * Registers the broadcast receiver and network callbacks and starts the scanning mechanism.
     */
    @MainThread
    public void onStart() {
        mWorkerHandler.post(() -> {
            mIsStopped = false;
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            filter.addAction(WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver, filter,
                    /* broadcastPermission */ null, mWorkerHandler);
            mConnectivityManager.registerNetworkCallback(mNetworkRequest, mNetworkCallback,
                    mWorkerHandler);
            mConnectivityManager.registerDefaultNetworkCallback(mDefaultNetworkCallback,
                    mWorkerHandler);
            mConnectivityDiagnosticsManager.registerConnectivityDiagnosticsCallback(mNetworkRequest,
                    mConnectivityDiagnosticsExecutor, mConnectivityDiagnosticsCallback);
            if (mSharedConnectivityManager != null && mSharedConnectivityCallback != null
                    && BuildCompat.isAtLeastU()) {
                mSharedConnectivityManager.registerCallback(mSharedConnectivityExecutor,
                        mSharedConnectivityCallback);
            }
            handleOnStart();
            mIsInitialized = true;
        });
    }

    /**
     * Unregisters the broadcast receiver, network callbacks, and pauses the scanning mechanism.
     */
    @MainThread
    public void onStop() {
        mWorkerHandler.post(() -> {
            mIsStopped = true;
            mScanner.stop();
            try {
                mContext.unregisterReceiver(mBroadcastReceiver);
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
                mConnectivityManager.unregisterNetworkCallback(mDefaultNetworkCallback);
                mConnectivityDiagnosticsManager.unregisterConnectivityDiagnosticsCallback(
                        mConnectivityDiagnosticsCallback);
                if (mSharedConnectivityManager != null && mSharedConnectivityCallback != null
                        && BuildCompat.isAtLeastU()) {
                    boolean result =
                            mSharedConnectivityManager.unregisterCallback(
                                    mSharedConnectivityCallback);
                    if (!result) {
                        Log.e(mTag, "onStop: unregisterCallback failed");
                    }
                }
            } catch (IllegalArgumentException e) {
                // Already unregistered in onDestroyed().
            }
        });
    }

    /**
     * Unregisters the broadcast receiver network callbacks in case the Activity is destroyed before
     * the worker thread runnable posted in onStop() runs.
     */
    @MainThread
    public void onDestroy() {
        try {
            mContext.unregisterReceiver(mBroadcastReceiver);
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            mConnectivityManager.unregisterNetworkCallback(mDefaultNetworkCallback);
            mConnectivityDiagnosticsManager.unregisterConnectivityDiagnosticsCallback(
                    mConnectivityDiagnosticsCallback);
            if (mSharedConnectivityManager != null && mSharedConnectivityCallback != null
                    && BuildCompat.isAtLeastU()) {
                boolean result =
                        mSharedConnectivityManager.unregisterCallback(
                                mSharedConnectivityCallback);
                if (!result) {
                    Log.e(mTag, "onDestroyed: unregisterCallback failed");
                }
            }
        } catch (IllegalArgumentException e) {
            // Already unregistered in onStop() worker thread runnable.
        }
    }

    /**
     * Returns true if this WifiTracker has already been initialized in the worker thread via
     * handleOnStart()
     */
    @AnyThread
    boolean isInitialized() {
        return mIsInitialized;
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
        return mWifiState;
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
     * Handle link property changes for the given network.
     */
    @WorkerThread
    protected void handleLinkPropertiesChanged(
            @NonNull Network network, @Nullable LinkProperties linkProperties) {
        // Do nothing.
    }

    /**
     * Handle network capability changes for the current connected Wifi network.
     */
    @WorkerThread
    protected void handleNetworkCapabilitiesChanged(
            @NonNull Network network, @NonNull NetworkCapabilities capabilities) {
        // Do nothing.
    }

    /**
     * Handle the loss of a network.
     */
    @WorkerThread
    protected void handleNetworkLost(@NonNull Network network) {
        // Do nothing.
    }

    /**
     * Handle receiving a connectivity report.
     */
    @WorkerThread
    protected void handleConnectivityReportAvailable(
            @NonNull ConnectivityDiagnosticsManager.ConnectivityReport connectivityReport) {
        // Do nothing.
    }

    /**
     * Handle default network capabilities changed.
     */
    @WorkerThread
    protected void handleDefaultNetworkCapabilitiesChanged(@NonNull Network network,
            @NonNull NetworkCapabilities networkCapabilities) {
        // Do nothing.
    }

    /**
     * Handle default network loss.
     */
    @WorkerThread
    protected void handleDefaultNetworkLost() {
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
     * Handle updates to the list of tether networks from SharedConnectivityManager.
     */
    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    protected void handleHotspotNetworksUpdated(List<HotspotNetwork> networks) {
        // Do nothing.
    }

    /**
     * Handle updates to the list of known networks from SharedConnectivityManager.
     */
    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    protected void handleKnownNetworksUpdated(List<KnownNetwork> networks) {
        // Do nothing.
    }

    /**
     * Handle changes to the shared connectivity settings from SharedConnectivityManager.
     */
    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    protected void handleSharedConnectivitySettingsChanged(
            @NonNull SharedConnectivitySettingsState state) {
        // Do nothing.
    }

    /**
     * Handle changes to the shared connectivity settings from SharedConnectivityManager.
     */
    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    protected void handleHotspotNetworkConnectionStatusChanged(
            @NonNull HotspotNetworkConnectionStatus status) {
        // Do nothing.
    }

    /**
     * Handle changes to the shared connectivity settings from SharedConnectivityManager.
     */
    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    protected void handleKnownNetworkConnectionStatusChanged(
            @NonNull KnownNetworkConnectionStatus status) {
        // Do nothing.
    }

    /**
     * Handle service connected callback from SharedConnectivityManager.
     */
    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    protected void handleServiceConnected() {
        // Do nothing.
    }

    /**
     * Handle service disconnected callback from SharedConnectivityManager.
     */
    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    protected void handleServiceDisconnected() {
        // Do nothing.
    }

    /**
     * Handle register callback failed callback from SharedConnectivityManager.
     */
    @TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @WorkerThread
    protected void handleRegisterCallbackFailed(Exception exception) {
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
        private final WifiScanner.ScanListener mFirstScanListener = new WifiScanner.ScanListener() {
            @Override
            public void onPeriodChanged(int periodInMs) {
                // No-op.
            }

            @Override
            public void onResults(WifiScanner.ScanData[] results) {
                mWorkerHandler.post(() -> {
                    if (!mIsActive || mIsStopped) {
                        return;
                    }
                    if (sVerboseLogging) {
                        Log.v(mTag, "Received scan results from first scan request.");
                    }
                    List<ScanResult> scanResults = new ArrayList<>();
                    if (results != null) {
                        for (WifiScanner.ScanData scanData : results) {
                            scanResults.addAll(List.of(scanData.getResults()));
                        }
                    }
                    // Fake a SCAN_RESULTS_AVAILABLE_ACTION. The results should already be populated
                    // in mScanResultUpdater, which is the source of truth for the child classes.
                    mScanResultUpdater.update(scanResults);
                    handleScanResultsAvailableAction(
                            new Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                                    .putExtra(WifiManager.EXTRA_RESULTS_UPDATED, true));
                    // Now start scanning via WifiManager.startScan().
                    postScan();
                });
            }

            @Override
            public void onFullResult(ScanResult fullScanResult) {
                // No-op.
            }

            @Override
            public void onSuccess() {
                // No-op.
            }

            @Override
            public void onFailure(int reason, String description) {
                mWorkerHandler.post(() -> {
                    if (!mIsActive || mIsStopped) {
                        return;
                    }
                    Log.e(mTag, "Failed to scan! Reason: " + reason + ", ");
                    // First scan failed, start scanning normally anyway.
                    postScan();
                });
            }
        };

        private Scanner(Looper looper) {
            super(looper);
        }

        private void start() {
            if (mIsActive || mIsStopped) {
                return;
            }
            mIsActive = true;
            if (isVerboseLoggingEnabled()) {
                Log.v(mTag, "Scanner start");
            }
            if (BuildCompat.isAtLeastU()) {
                // Start off with a fast scan of 2.4GHz, 5GHz, and 6GHz RNR using WifiScanner.
                // After this is done, fall back to WifiManager.startScan() to get the rest of
                // the bands and hidden networks.
                // TODO(b/274177966): Move to using WifiScanner exclusively once we have
                //                    permission to use ScanSettings.hiddenNetworks.
                WifiScanner.ScanSettings scanSettings = new WifiScanner.ScanSettings();
                scanSettings.band = WifiScanner.WIFI_BAND_BOTH;
                scanSettings.setRnrSetting(WifiScanner.WIFI_RNR_ENABLED);
                WifiScanner wifiScanner = mContext.getSystemService(WifiScanner.class);
                if (wifiScanner != null) {
                    wifiScanner.startScan(scanSettings, mFirstScanListener);
                    return;
                } else {
                    Log.e(mTag, "Failed to retrieve WifiScanner!");
                }
            }
            postScan();
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
            if (!mIsActive || mIsStopped) {
                Log.wtf(mTag, "Tried to run scan loop when we've already stopped!");
                return;
            }
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

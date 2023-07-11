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

import static com.android.wifitrackerlib.Utils.getBestScanResultByLevel;
import static com.android.wifitrackerlib.WifiEntry.ConnectCallback.CONNECT_STATUS_FAILURE_UNKNOWN;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.ProvisioningCallback;
import android.os.Handler;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.os.BuildCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * WifiEntry representation of an Online Sign-up entry, uniquely identified by FQDN.
 */
class OsuWifiEntry extends WifiEntry {
    static final String KEY_PREFIX = "OsuWifiEntry:";

    // Scan result list must be thread safe for generating the verbose scan summary
    @NonNull private final List<ScanResult> mCurrentScanResults = new ArrayList<>();

    @NonNull private final String mKey;
    @NonNull private final Context mContext;
    @NonNull private final OsuProvider mOsuProvider;
    private String mSsid;
    private String mOsuStatusString;
    private boolean mIsAlreadyProvisioned = false;
    private boolean mHasAddConfigUserRestriction = false;
    private final UserManager mUserManager;

    /**
     * Create an OsuWifiEntry with the associated OsuProvider
     */
    OsuWifiEntry(
            @NonNull WifiTrackerInjector injector,
            @NonNull Context context, @NonNull Handler callbackHandler,
            @NonNull OsuProvider osuProvider,
            @NonNull WifiManager wifiManager,
            boolean forSavedNetworksPage) throws IllegalArgumentException {
        super(callbackHandler, wifiManager, forSavedNetworksPage);

        checkNotNull(osuProvider, "Cannot construct with null osuProvider!");

        mContext = context;
        mOsuProvider = osuProvider;
        mKey = osuProviderToOsuWifiEntryKey(osuProvider);
        mUserManager = injector.getUserManager();
        if (BuildCompat.isAtLeastT() && mUserManager != null) {
            mHasAddConfigUserRestriction = mUserManager.hasUserRestriction(
                    UserManager.DISALLOW_ADD_WIFI_CONFIG);
        }
    }

    @Override
    public String getKey() {
        return mKey;
    }

    @Override
    public synchronized String getTitle() {
        final String friendlyName = mOsuProvider.getFriendlyName();
        if (!TextUtils.isEmpty(friendlyName)) {
            return friendlyName;
        }
        if (!TextUtils.isEmpty(mSsid)) {
            return mSsid;
        }
        final Uri serverUri = mOsuProvider.getServerUri();
        if (serverUri != null) {
            return serverUri.toString();
        }
        return "";
    }

    @Override
    public synchronized String getSummary(boolean concise) {
        if (hasAdminRestrictions()) {
            return mContext.getString(R.string.wifitrackerlib_admin_restricted_network);
        }

        // TODO(b/70983952): Add verbose summary
        if (mOsuStatusString != null) {
            return mOsuStatusString;
        } else if (isAlreadyProvisioned()) {
            return concise ? mContext.getString(R.string.wifitrackerlib_wifi_passpoint_expired)
                    : mContext.getString(
                    R.string.wifitrackerlib_tap_to_renew_subscription_and_connect);
        } else {
            return mContext.getString(R.string.wifitrackerlib_tap_to_sign_up);
        }
    }

    @Override
    public synchronized String getSsid() {
        return mSsid;
    }

    @Override
    public String getMacAddress() {
        // TODO(b/70983952): Fill this method in in case we need the mac address for verbose logging
        return null;
    }

    @Override
    public synchronized boolean canConnect() {
        //check user restriction and whether the network is already provisioned
        if (hasAdminRestrictions()) {
            return false;
        }
        return mLevel != WIFI_LEVEL_UNREACHABLE
                && getConnectedState() == CONNECTED_STATE_DISCONNECTED;
    }

    @Override
    public synchronized void connect(@Nullable ConnectCallback callback) {
        mConnectCallback = callback;
        mWifiManager.stopRestrictingAutoJoinToSubscriptionId();
        mWifiManager.startSubscriptionProvisioning(mOsuProvider, mContext.getMainExecutor(),
                new OsuWifiEntryProvisioningCallback());
    }

    @WorkerThread
    synchronized void updateScanResultInfo(@Nullable List<ScanResult> scanResults)
            throws IllegalArgumentException {
        if (scanResults == null) scanResults = new ArrayList<>();

        mCurrentScanResults.clear();
        mCurrentScanResults.addAll(scanResults);

        final ScanResult bestScanResult = getBestScanResultByLevel(scanResults);
        if (bestScanResult != null) {
            mSsid = bestScanResult.SSID;
            if (getConnectedState() == CONNECTED_STATE_DISCONNECTED) {
                mLevel = mWifiManager.calculateSignalLevel(bestScanResult.level);
            }
        } else {
            mLevel = WIFI_LEVEL_UNREACHABLE;
        }
        notifyOnUpdated();
    }

    @NonNull
    static String osuProviderToOsuWifiEntryKey(@NonNull OsuProvider osuProvider) {
        checkNotNull(osuProvider, "Cannot create key with null OsuProvider!");
        return KEY_PREFIX + osuProvider.getFriendlyName() + ","
                + osuProvider.getServerUri().toString();
    }

    @WorkerThread
    @Override
    protected boolean connectionInfoMatches(@NonNull WifiInfo wifiInfo,
            @NonNull NetworkInfo networkInfo) {
        return wifiInfo.isOsuAp() && TextUtils.equals(
                wifiInfo.getPasspointProviderFriendlyName(), mOsuProvider.getFriendlyName());
    }

    @Override
    protected String getScanResultDescription() {
        // TODO(b/70983952): Fill this method in.
        return "";
    }

    OsuProvider getOsuProvider() {
        return mOsuProvider;
    }

    synchronized boolean isAlreadyProvisioned() {
        return mIsAlreadyProvisioned;
    }

    synchronized void setAlreadyProvisioned(boolean isAlreadyProvisioned) {
        mIsAlreadyProvisioned = isAlreadyProvisioned;
    }

    private boolean hasAdminRestrictions() {
        if (mHasAddConfigUserRestriction && !mIsAlreadyProvisioned) {
            return true;
        }
        return false;
    }

    class OsuWifiEntryProvisioningCallback extends ProvisioningCallback {
        @Override
        @MainThread public void onProvisioningFailure(int status) {
            synchronized (OsuWifiEntry.this) {
                if (TextUtils.equals(
                        mOsuStatusString, mContext.getString(
                                R.string.wifitrackerlib_osu_completing_sign_up))) {
                    mOsuStatusString =
                            mContext.getString(R.string.wifitrackerlib_osu_sign_up_failed);
                } else {
                    mOsuStatusString =
                            mContext.getString(R.string.wifitrackerlib_osu_connect_failed);
                }
            }
            final ConnectCallback connectCallback = mConnectCallback;
            if (connectCallback != null) {
                connectCallback.onConnectResult(CONNECT_STATUS_FAILURE_UNKNOWN);
            }
            notifyOnUpdated();
        }

        @Override
        @MainThread public void onProvisioningStatus(int status) {
            String newStatusString = null;
            switch (status) {
                case OSU_STATUS_AP_CONNECTING:
                case OSU_STATUS_AP_CONNECTED:
                case OSU_STATUS_SERVER_CONNECTING:
                case OSU_STATUS_SERVER_VALIDATED:
                case OSU_STATUS_SERVER_CONNECTED:
                case OSU_STATUS_INIT_SOAP_EXCHANGE:
                case OSU_STATUS_WAITING_FOR_REDIRECT_RESPONSE:
                    newStatusString = String.format(mContext.getString(
                            R.string.wifitrackerlib_osu_opening_provider),
                            getTitle());
                    break;
                case OSU_STATUS_REDIRECT_RESPONSE_RECEIVED:
                case OSU_STATUS_SECOND_SOAP_EXCHANGE:
                case OSU_STATUS_THIRD_SOAP_EXCHANGE:
                case OSU_STATUS_RETRIEVING_TRUST_ROOT_CERTS:
                    newStatusString = mContext.getString(
                    R.string.wifitrackerlib_osu_completing_sign_up);
                    break;
            }
            synchronized (OsuWifiEntry.this) {
                boolean updated = !TextUtils.equals(mOsuStatusString, newStatusString);
                mOsuStatusString = newStatusString;
                if (updated) {
                    notifyOnUpdated();
                }
            }
        }

        @Override
        @MainThread public void onProvisioningComplete() {
            synchronized (OsuWifiEntry.this) {
                mOsuStatusString = mContext.getString(R.string.wifitrackerlib_osu_sign_up_complete);
            }
            notifyOnUpdated();

            PasspointConfiguration passpointConfig = mWifiManager
                    .getMatchingPasspointConfigsForOsuProviders(Collections.singleton(mOsuProvider))
                    .get(mOsuProvider);
            final ConnectCallback connectCallback = mConnectCallback;
            if (passpointConfig == null) {
                // Failed to find the config we just provisioned
                if (connectCallback != null) {
                    connectCallback.onConnectResult(CONNECT_STATUS_FAILURE_UNKNOWN);
                }
                return;
            }
            String uniqueId = passpointConfig.getUniqueId();
            for (Pair<WifiConfiguration, Map<Integer, List<ScanResult>>> pairing :
                    mWifiManager.getAllMatchingWifiConfigs(mWifiManager.getScanResults())) {
                WifiConfiguration config = pairing.first;
                if (TextUtils.equals(config.getKey(), uniqueId)) {
                    List<ScanResult> homeScans =
                            pairing.second.get(WifiManager.PASSPOINT_HOME_NETWORK);
                    List<ScanResult> roamingScans =
                            pairing.second.get(WifiManager.PASSPOINT_ROAMING_NETWORK);
                    ScanResult bestScan;
                    if (homeScans != null && !homeScans.isEmpty()) {
                        bestScan = getBestScanResultByLevel(homeScans);
                    } else if (roamingScans != null && !roamingScans.isEmpty()) {
                        bestScan = getBestScanResultByLevel(roamingScans);
                    } else {
                        break;
                    }
                    config.SSID = "\"" + bestScan.SSID + "\"";
                    mWifiManager.connect(config, null /* ActionListener */);
                    return;
                }
            }

            // Failed to find the network we provisioned for
            if (connectCallback != null) {
                connectCallback.onConnectResult(CONNECT_STATUS_FAILURE_UNKNOWN);
            }
        }
    }
}

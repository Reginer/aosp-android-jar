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

import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.DISABLED_AUTHENTICATION_FAILURE;
import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.DISABLED_AUTHENTICATION_NO_CREDENTIALS;
import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.DISABLED_BY_WRONG_PASSWORD;
import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.NETWORK_SELECTION_ENABLED;
import static android.net.wifi.WifiInfo.DEFAULT_MAC_ADDRESS;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_EAP;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_OPEN;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_OWE;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_PASSPOINT_R1_R2;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_PASSPOINT_R3;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_PSK;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_SAE;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_UNKNOWN;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_WEP;
import static android.net.wifi.WifiInfo.sanitizeSsid;

import static com.android.wifitrackerlib.Utils.getAutoConnectDescription;
import static com.android.wifitrackerlib.Utils.getBestScanResultByLevel;
import static com.android.wifitrackerlib.Utils.getConnectedDescription;
import static com.android.wifitrackerlib.Utils.getConnectingDescription;
import static com.android.wifitrackerlib.Utils.getDisconnectedDescription;
import static com.android.wifitrackerlib.Utils.getImsiProtectionDescription;
import static com.android.wifitrackerlib.Utils.getMeteredDescription;
import static com.android.wifitrackerlib.Utils.getSecurityTypesFromScanResult;
import static com.android.wifitrackerlib.Utils.getSecurityTypesFromWifiConfiguration;
import static com.android.wifitrackerlib.Utils.getSingleSecurityTypeFromMultipleSecurityTypes;
import static com.android.wifitrackerlib.Utils.getVerboseLoggingDescription;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.app.admin.WifiSsidPolicy;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.os.BuildCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * WifiEntry representation of a logical Wi-Fi network, uniquely identified by SSID and security.
 *
 * This type of WifiEntry can represent both open and saved networks.
 */
@VisibleForTesting
public class StandardWifiEntry extends WifiEntry {
    static final String TAG = "StandardWifiEntry";
    public static final String KEY_PREFIX = "StandardWifiEntry:";

    @NonNull private final StandardWifiEntryKey mKey;

    @NonNull private final WifiTrackerInjector mInjector;
    @NonNull private final Context mContext;

    // Map of security type to matching scan results
    @NonNull private final Map<Integer, List<ScanResult>> mMatchingScanResults = new HashMap<>();
    // Map of security type to matching WifiConfiguration
    // TODO: Change this to single WifiConfiguration once we can get multiple security type configs.
    @NonNull private final Map<Integer, WifiConfiguration> mMatchingWifiConfigs = new HashMap<>();

    // List of the target scan results to be displayed. This should match the highest available
    // security from all of the matched WifiConfigurations.
    // If no WifiConfigurations are available, then these should match the most appropriate security
    // type (e.g. PSK for an PSK/SAE entry, OWE for an Open/OWE entry).
    @NonNull private final List<ScanResult> mTargetScanResults = new ArrayList<>();
    // Target WifiConfiguration for connection and displaying WifiConfiguration info
    private WifiConfiguration mTargetWifiConfig;
    private List<Integer> mTargetSecurityTypes = new ArrayList<>();

    private boolean mIsUserShareable = false;

    private boolean mShouldAutoOpenCaptivePortal = false;

    private boolean mIsAdminRestricted = false;
    private boolean mHasAddConfigUserRestriction = false;

    private final boolean mIsWpa3SaeSupported;
    private final boolean mIsWpa3SuiteBSupported;
    private final boolean mIsEnhancedOpenSupported;

    private final UserManager mUserManager;
    private final DevicePolicyManager mDevicePolicyManager;

    StandardWifiEntry(
            @NonNull WifiTrackerInjector injector,
            @NonNull Context context, @NonNull Handler callbackHandler,
            @NonNull StandardWifiEntryKey key, @NonNull WifiManager wifiManager,
            boolean forSavedNetworksPage) {
        super(callbackHandler, wifiManager, forSavedNetworksPage);
        mInjector = injector;
        mContext = context;
        mKey = key;
        mIsWpa3SaeSupported = wifiManager.isWpa3SaeSupported();
        mIsWpa3SuiteBSupported = wifiManager.isWpa3SuiteBSupported();
        mIsEnhancedOpenSupported = wifiManager.isEnhancedOpenSupported();
        mUserManager = injector.getUserManager();
        mDevicePolicyManager = injector.getDevicePolicyManager();
        updateSecurityTypes();
        updateAdminRestrictions();
    }

    StandardWifiEntry(
            @NonNull WifiTrackerInjector injector,
            @NonNull Context context, @NonNull Handler callbackHandler,
            @NonNull StandardWifiEntryKey key,
            @Nullable List<WifiConfiguration> configs,
            @Nullable List<ScanResult> scanResults,
            @NonNull WifiManager wifiManager,
            boolean forSavedNetworksPage) throws IllegalArgumentException {
        this(injector, context, callbackHandler, key, wifiManager,
                forSavedNetworksPage);
        if (configs != null && !configs.isEmpty()) {
            updateConfig(configs);
        }
        if (scanResults != null && !scanResults.isEmpty()) {
            updateScanResultInfo(scanResults);
        }
    }

    @Override
    public String getKey() {
        return mKey.toString();
    }

    StandardWifiEntryKey getStandardWifiEntryKey() {
        return mKey;
    }

    @Override
    public String getTitle() {
        return mKey.getScanResultKey().getSsid();
    }

    @Override
    public synchronized String getSummary(boolean concise) {
        if (hasAdminRestrictions()) {
            return mContext.getString(R.string.wifitrackerlib_admin_restricted_network);
        }

        StringJoiner sj = new StringJoiner(mContext.getString(
                R.string.wifitrackerlib_summary_separator));

        final String connectedStateDescription;
        final @ConnectedState int connectedState = getConnectedState();
        switch (connectedState) {
            case CONNECTED_STATE_DISCONNECTED:
                connectedStateDescription = getDisconnectedDescription(mInjector, mContext,
                        mTargetWifiConfig,
                        mForSavedNetworksPage,
                        concise);
                break;
            case CONNECTED_STATE_CONNECTING:
                connectedStateDescription = getConnectingDescription(mContext, mNetworkInfo);
                break;
            case CONNECTED_STATE_CONNECTED:
                connectedStateDescription = getConnectedDescription(mContext,
                        mTargetWifiConfig,
                        mNetworkCapabilities,
                        mIsDefaultNetwork,
                        mIsLowQuality);
                break;
            default:
                Log.e(TAG, "getConnectedState() returned unknown state: " + connectedState);
                connectedStateDescription = null;
        }
        if (!TextUtils.isEmpty(connectedStateDescription)) {
            sj.add(connectedStateDescription);
        }

        final String autoConnectDescription = getAutoConnectDescription(mContext, this);
        if (!TextUtils.isEmpty(autoConnectDescription)) {
            sj.add(autoConnectDescription);
        }

        final String meteredDescription = getMeteredDescription(mContext, this);
        if (!TextUtils.isEmpty(meteredDescription)) {
            sj.add(meteredDescription);
        }

        if (!concise) {
            final String verboseLoggingDescription = getVerboseLoggingDescription(this);
            if (!TextUtils.isEmpty(verboseLoggingDescription)) {
                sj.add(verboseLoggingDescription);
            }
        }

        return sj.toString();
    }

    @Override
    public CharSequence getSecondSummary() {
        return getConnectedState() == CONNECTED_STATE_CONNECTED
                ? getImsiProtectionDescription(mContext, getWifiConfiguration()) : "";
    }

    @Override
    public String getSsid() {
        return mKey.getScanResultKey().getSsid();
    }

    @Override
    public synchronized List<Integer> getSecurityTypes() {
        return new ArrayList<>(mTargetSecurityTypes);
    }

    @Override
    public synchronized String getMacAddress() {
        if (mWifiInfo != null) {
            final String wifiInfoMac = mWifiInfo.getMacAddress();
            if (!TextUtils.isEmpty(wifiInfoMac)
                    && !TextUtils.equals(wifiInfoMac, DEFAULT_MAC_ADDRESS)) {
                return wifiInfoMac;
            }
        }
        if (mTargetWifiConfig == null || getPrivacy() != PRIVACY_RANDOMIZED_MAC) {
            final String[] factoryMacs = mWifiManager.getFactoryMacAddresses();
            if (factoryMacs.length > 0) {
                return factoryMacs[0];
            }
            return null;
        }
        return mTargetWifiConfig.getRandomizedMacAddress().toString();
    }

    @Override
    public synchronized boolean isMetered() {
        return getMeteredChoice() == METERED_CHOICE_METERED
                || (mTargetWifiConfig != null && mTargetWifiConfig.meteredHint);
    }

    @Override
    public synchronized boolean isSaved() {
        return mTargetWifiConfig != null && !mTargetWifiConfig.fromWifiNetworkSuggestion
                && !mTargetWifiConfig.isEphemeral();
    }

    @Override
    public synchronized boolean isSuggestion() {
        return mTargetWifiConfig != null && mTargetWifiConfig.fromWifiNetworkSuggestion;
    }

    @Override
    public synchronized WifiConfiguration getWifiConfiguration() {
        if (!isSaved()) {
            return null;
        }
        return mTargetWifiConfig;
    }

    @Override
    public synchronized boolean canConnect() {
        if (mLevel == WIFI_LEVEL_UNREACHABLE
                || getConnectedState() != CONNECTED_STATE_DISCONNECTED) {
            return false;
        }

        if (hasAdminRestrictions()) return false;

        // Allow connection for EAP SIM dependent methods if the SIM of specified carrier ID is
        // active in the device.
        if (mTargetSecurityTypes.contains(SECURITY_TYPE_EAP) && mTargetWifiConfig != null
                && mTargetWifiConfig.enterpriseConfig != null) {
            if (!mTargetWifiConfig.enterpriseConfig.isAuthenticationSimBased()) {
                return true;
            }
            List<SubscriptionInfo> activeSubscriptionInfos = mContext
                    .getSystemService(SubscriptionManager.class).getActiveSubscriptionInfoList();
            if (activeSubscriptionInfos == null || activeSubscriptionInfos.size() == 0) {
                return false;
            }
            if (mTargetWifiConfig.carrierId == TelephonyManager.UNKNOWN_CARRIER_ID) {
                // To connect via default subscription.
                return true;
            }
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfos) {
                if (subscriptionInfo.getCarrierId() == mTargetWifiConfig.carrierId) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public synchronized void connect(@Nullable ConnectCallback callback) {
        mConnectCallback = callback;
        // We should flag this network to auto-open captive portal since this method represents
        // the user manually connecting to a network (i.e. not auto-join).
        mShouldAutoOpenCaptivePortal = true;
        mWifiManager.stopRestrictingAutoJoinToSubscriptionId();
        if (isSaved() || isSuggestion()) {
            if (Utils.isSimCredential(mTargetWifiConfig)
                    && !Utils.isSimPresent(mContext, mTargetWifiConfig.carrierId)) {
                if (callback != null) {
                    mCallbackHandler.post(() ->
                            callback.onConnectResult(
                                    ConnectCallback.CONNECT_STATUS_FAILURE_SIM_ABSENT));
                }
                return;
            }
            // Saved/suggested network
            mWifiManager.connect(mTargetWifiConfig.networkId, new ConnectActionListener());
        } else {
            if (mTargetSecurityTypes.contains(SECURITY_TYPE_OWE)) {
                // OWE network
                final WifiConfiguration oweConfig = new WifiConfiguration();
                oweConfig.SSID = "\"" + mKey.getScanResultKey().getSsid() + "\"";
                oweConfig.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OWE);
                mWifiManager.connect(oweConfig, new ConnectActionListener());
                if (mTargetSecurityTypes.contains(SECURITY_TYPE_OPEN)) {
                    // Add an extra Open config for OWE transition networks
                    final WifiConfiguration openConfig = new WifiConfiguration();
                    openConfig.SSID = "\"" + mKey.getScanResultKey().getSsid() + "\"";
                    openConfig.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OPEN);
                    mWifiManager.save(openConfig, null);
                }
            } else if (mTargetSecurityTypes.contains(SECURITY_TYPE_OPEN)) {
                // Open network
                final WifiConfiguration openConfig = new WifiConfiguration();
                openConfig.SSID = "\"" + mKey.getScanResultKey().getSsid() + "\"";
                openConfig.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OPEN);
                mWifiManager.connect(openConfig, new ConnectActionListener());
            } else {
                // Secure network
                if (callback != null) {
                    mCallbackHandler.post(() ->
                            callback.onConnectResult(
                                    ConnectCallback.CONNECT_STATUS_FAILURE_NO_CONFIG));
                }
            }
        }
    }

    @Override
    public boolean canDisconnect() {
        return getConnectedState() == CONNECTED_STATE_CONNECTED;
    }

    @Override
    public synchronized void disconnect(@Nullable DisconnectCallback callback) {
        if (canDisconnect()) {
            mCalledDisconnect = true;
            mDisconnectCallback = callback;
            mCallbackHandler.postDelayed(() -> {
                if (callback != null && mCalledDisconnect) {
                    callback.onDisconnectResult(
                            DisconnectCallback.DISCONNECT_STATUS_FAILURE_UNKNOWN);
                }
            }, 10_000 /* delayMillis */);
            mWifiManager.disableEphemeralNetwork("\"" + mKey.getScanResultKey().getSsid() + "\"");
            mWifiManager.disconnect();
        }
    }

    @Override
    public boolean canForget() {
        return getWifiConfiguration() != null;
    }

    @Override
    public synchronized void forget(@Nullable ForgetCallback callback) {
        if (canForget()) {
            mForgetCallback = callback;
            mWifiManager.forget(mTargetWifiConfig.networkId, new ForgetActionListener());
        }
    }

    @Override
    public synchronized boolean canSignIn() {
        return mNetworkCapabilities != null
                && mNetworkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
    }

    @Override
    public void signIn(@Nullable SignInCallback callback) {
        if (canSignIn()) {
            // canSignIn() implies that this WifiEntry is the currently connected network, so use
            // getCurrentNetwork() to start the captive portal app.
            NonSdkApiWrapper.startCaptivePortalApp(
                    mContext.getSystemService(ConnectivityManager.class),
                    mWifiManager.getCurrentNetwork());
        }
    }

    /**
     * Returns whether the network can be shared via QR code.
     * See https://github.com/zxing/zxing/wiki/Barcode-Contents#wi-fi-network-config-android-ios-11
     */
    @Override
    public synchronized boolean canShare() {
        if (mInjector.isDemoMode()) {
            return false;
        }

        WifiConfiguration wifiConfig = getWifiConfiguration();
        if (wifiConfig == null) {
            return false;
        }

        if (BuildCompat.isAtLeastT() && mUserManager.hasUserRestrictionForUser(
                UserManager.DISALLOW_SHARING_ADMIN_CONFIGURED_WIFI,
                UserHandle.getUserHandleForUid(wifiConfig.creatorUid))
                && Utils.isDeviceOrProfileOwner(wifiConfig.creatorUid,
                wifiConfig.creatorName, mContext)) {
            return false;
        }

        for (int securityType : mTargetSecurityTypes) {
            switch (securityType) {
                case SECURITY_TYPE_OPEN:
                case SECURITY_TYPE_OWE:
                case SECURITY_TYPE_WEP:
                case SECURITY_TYPE_PSK:
                case SECURITY_TYPE_SAE:
                    return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the user can use Easy Connect to onboard a device to the network.
     * See https://www.wi-fi.org/discover-wi-fi/wi-fi-easy-connect
     */
    @Override
    public synchronized boolean canEasyConnect() {
        if (mInjector.isDemoMode()) {
            return false;
        }

        WifiConfiguration wifiConfig = getWifiConfiguration();
        if (wifiConfig == null) {
            return false;
        }

        if (!mWifiManager.isEasyConnectSupported()) {
            return false;
        }

        if (BuildCompat.isAtLeastT() && mUserManager.hasUserRestrictionForUser(
                UserManager.DISALLOW_SHARING_ADMIN_CONFIGURED_WIFI,
                UserHandle.getUserHandleForUid(wifiConfig.creatorUid))
                && Utils.isDeviceOrProfileOwner(wifiConfig.creatorUid,
                wifiConfig.creatorName, mContext)) {
            return false;
        }

        // DPP 1.0 only supports WPA2 and WPA3.
        return mTargetSecurityTypes.contains(SECURITY_TYPE_PSK)
                || mTargetSecurityTypes.contains(SECURITY_TYPE_SAE);
    }

    @Override
    @MeteredChoice
    public synchronized int getMeteredChoice() {
        if (!isSuggestion() && mTargetWifiConfig != null) {
            final int meteredOverride = mTargetWifiConfig.meteredOverride;
            if (meteredOverride == WifiConfiguration.METERED_OVERRIDE_METERED) {
                return METERED_CHOICE_METERED;
            } else if (meteredOverride == WifiConfiguration.METERED_OVERRIDE_NOT_METERED) {
                return METERED_CHOICE_UNMETERED;
            }
        }
        return METERED_CHOICE_AUTO;
    }

    @Override
    public boolean canSetMeteredChoice() {
        return getWifiConfiguration() != null;
    }

    @Override
    public synchronized void setMeteredChoice(int meteredChoice) {
        if (!canSetMeteredChoice()) {
            return;
        }

        if (meteredChoice == METERED_CHOICE_AUTO) {
            mTargetWifiConfig.meteredOverride = WifiConfiguration.METERED_OVERRIDE_NONE;
        } else if (meteredChoice == METERED_CHOICE_METERED) {
            mTargetWifiConfig.meteredOverride = WifiConfiguration.METERED_OVERRIDE_METERED;
        } else if (meteredChoice == METERED_CHOICE_UNMETERED) {
            mTargetWifiConfig.meteredOverride = WifiConfiguration.METERED_OVERRIDE_NOT_METERED;
        }
        mWifiManager.save(mTargetWifiConfig, null /* listener */);
    }

    @Override
    public boolean canSetPrivacy() {
        return isSaved();
    }

    @Override
    @Privacy
    public synchronized int getPrivacy() {
        if (mTargetWifiConfig != null
                && mTargetWifiConfig.macRandomizationSetting
                == WifiConfiguration.RANDOMIZATION_NONE) {
            return PRIVACY_DEVICE_MAC;
        } else {
            return PRIVACY_RANDOMIZED_MAC;
        }
    }

    @Override
    public synchronized void setPrivacy(int privacy) {
        if (!canSetPrivacy()) {
            return;
        }

        mTargetWifiConfig.macRandomizationSetting = privacy == PRIVACY_RANDOMIZED_MAC
                ? WifiConfiguration.RANDOMIZATION_AUTO : WifiConfiguration.RANDOMIZATION_NONE;
        mWifiManager.save(mTargetWifiConfig, null /* listener */);
    }

    @Override
    public synchronized boolean isAutoJoinEnabled() {
        if (mTargetWifiConfig == null) {
            return false;
        }

        return mTargetWifiConfig.allowAutojoin;
    }

    @Override
    public boolean canSetAutoJoinEnabled() {
        return isSaved() || isSuggestion();
    }

    @Override
    public synchronized void setAutoJoinEnabled(boolean enabled) {
        if (mTargetWifiConfig == null || !canSetAutoJoinEnabled()) {
            return;
        }

        mWifiManager.allowAutojoin(mTargetWifiConfig.networkId, enabled);
    }

    @Override
    public synchronized String getSecurityString(boolean concise) {
        if (mTargetSecurityTypes.size() == 0) {
            return concise ? "" : mContext.getString(R.string.wifitrackerlib_wifi_security_none);
        }
        if (mTargetSecurityTypes.size() == 1) {
            final int security = mTargetSecurityTypes.get(0);
            switch(security) {
                case SECURITY_TYPE_EAP:
                    return concise ? mContext.getString(
                            R.string.wifitrackerlib_wifi_security_short_eap_wpa_wpa2) :
                            mContext.getString(
                                    R.string.wifitrackerlib_wifi_security_eap_wpa_wpa2);
                case SECURITY_TYPE_EAP_WPA3_ENTERPRISE:
                    return concise ? mContext.getString(
                            R.string.wifitrackerlib_wifi_security_short_eap_wpa3) :
                            mContext.getString(
                                    R.string.wifitrackerlib_wifi_security_eap_wpa3);
                case SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT:
                    return concise ? mContext.getString(
                            R.string.wifitrackerlib_wifi_security_short_eap_suiteb) :
                            mContext.getString(R.string.wifitrackerlib_wifi_security_eap_suiteb);
                case SECURITY_TYPE_PSK:
                    return concise ? mContext.getString(
                            R.string.wifitrackerlib_wifi_security_short_wpa_wpa2) :
                            mContext.getString(
                                    R.string.wifitrackerlib_wifi_security_wpa_wpa2);
                case SECURITY_TYPE_WEP:
                    return mContext.getString(R.string.wifitrackerlib_wifi_security_wep);
                case SECURITY_TYPE_SAE:
                    return concise ? mContext.getString(
                            R.string.wifitrackerlib_wifi_security_short_sae) :
                            mContext.getString(R.string.wifitrackerlib_wifi_security_sae);
                case SECURITY_TYPE_OWE:
                    return concise ? mContext.getString(
                            R.string.wifitrackerlib_wifi_security_short_owe) :
                            mContext.getString(R.string.wifitrackerlib_wifi_security_owe);
                case SECURITY_TYPE_OPEN:
                    return concise ? "" : mContext.getString(
                            R.string.wifitrackerlib_wifi_security_none);
            }
        }
        if (mTargetSecurityTypes.size() == 2) {
            if (mTargetSecurityTypes.contains(SECURITY_TYPE_OPEN)
                    && mTargetSecurityTypes.contains(SECURITY_TYPE_OWE)) {
                StringJoiner sj = new StringJoiner("/");
                sj.add(mContext.getString(R.string.wifitrackerlib_wifi_security_none));
                sj.add(concise ? mContext.getString(
                        R.string.wifitrackerlib_wifi_security_short_owe) :
                        mContext.getString(R.string.wifitrackerlib_wifi_security_owe));
                return sj.toString();
            }
            if (mTargetSecurityTypes.contains(SECURITY_TYPE_PSK)
                    && mTargetSecurityTypes.contains(SECURITY_TYPE_SAE)) {
                return concise ? mContext.getString(
                        R.string.wifitrackerlib_wifi_security_short_wpa_wpa2_wpa3) :
                        mContext.getString(
                                R.string.wifitrackerlib_wifi_security_wpa_wpa2_wpa3);
            }
            if (mTargetSecurityTypes.contains(SECURITY_TYPE_EAP)
                    && mTargetSecurityTypes.contains(SECURITY_TYPE_EAP_WPA3_ENTERPRISE)) {
                return concise ? mContext.getString(
                        R.string.wifitrackerlib_wifi_security_short_eap_wpa_wpa2_wpa3) :
                        mContext.getString(
                                R.string.wifitrackerlib_wifi_security_eap_wpa_wpa2_wpa3);
            }
        }
        // Unknown security types
        Log.e(TAG, "Couldn't get string for security types: " + mTargetSecurityTypes);
        return concise ? "" : mContext.getString(R.string.wifitrackerlib_wifi_security_none);
    }

    @Override
    public synchronized String getStandardString() {
        if (mWifiInfo != null) {
            return Utils.getStandardString(mContext, mWifiInfo.getWifiStandard());
        }
        if (!mTargetScanResults.isEmpty()) {
            return Utils.getStandardString(mContext, mTargetScanResults.get(0).getWifiStandard());
        }
        return "";
    }

    @Override
    public synchronized boolean shouldEditBeforeConnect() {
        WifiConfiguration wifiConfig = getWifiConfiguration();
        if (wifiConfig == null) {
            return false;
        }

        // The network is disabled because of one of the authentication problems.
        NetworkSelectionStatus networkSelectionStatus = wifiConfig.getNetworkSelectionStatus();
        if (networkSelectionStatus.getNetworkSelectionStatus() != NETWORK_SELECTION_ENABLED) {
            if (networkSelectionStatus.getDisableReasonCounter(DISABLED_AUTHENTICATION_FAILURE) > 0
                    || networkSelectionStatus.getDisableReasonCounter(
                    DISABLED_BY_WRONG_PASSWORD) > 0
                    || networkSelectionStatus.getDisableReasonCounter(
                    DISABLED_AUTHENTICATION_NO_CREDENTIALS) > 0) {
                return true;
            }
        }

        return false;
    }

    @WorkerThread
    synchronized void updateScanResultInfo(@Nullable List<ScanResult> scanResults)
            throws IllegalArgumentException {
        if (scanResults == null) scanResults = new ArrayList<>();

        final String ssid = mKey.getScanResultKey().getSsid();
        for (ScanResult scan : scanResults) {
            if (!TextUtils.equals(scan.SSID, ssid)) {
                throw new IllegalArgumentException(
                        "Attempted to update with wrong SSID! Expected: "
                                + ssid + ", Actual: " + scan.SSID + ", ScanResult: " + scan);
            }
        }
        // Populate the cached scan result map
        mMatchingScanResults.clear();
        final Set<Integer> keySecurityTypes = mKey.getScanResultKey().getSecurityTypes();
        for (ScanResult scan : scanResults) {
            for (int security : getSecurityTypesFromScanResult(scan)) {
                if (!keySecurityTypes.contains(security) || !isSecurityTypeSupported(security)) {
                    continue;
                }
                if (!mMatchingScanResults.containsKey(security)) {
                    mMatchingScanResults.put(security, new ArrayList<>());
                }
                mMatchingScanResults.get(security).add(scan);
            }
        }

        updateSecurityTypes();
        updateTargetScanResultInfo();
        notifyOnUpdated();
    }

    private synchronized void updateTargetScanResultInfo() {
        // Update the level using the scans matching the target security type
        final ScanResult bestScanResult = getBestScanResultByLevel(mTargetScanResults);

        if (getConnectedState() == CONNECTED_STATE_DISCONNECTED) {
            mLevel = bestScanResult != null
                    ? mWifiManager.calculateSignalLevel(bestScanResult.level)
                    : WIFI_LEVEL_UNREACHABLE;
        }
    }

    @WorkerThread
    @Override
    synchronized void updateNetworkCapabilities(@Nullable NetworkCapabilities capabilities) {
        super.updateNetworkCapabilities(capabilities);

        // Auto-open an available captive portal if the user manually connected to this network.
        if (canSignIn() && mShouldAutoOpenCaptivePortal) {
            mShouldAutoOpenCaptivePortal = false;
            signIn(null /* callback */);
        }
    }

    @WorkerThread
    synchronized void updateConfig(@Nullable List<WifiConfiguration> wifiConfigs)
            throws IllegalArgumentException {
        if (wifiConfigs == null) {
            wifiConfigs = Collections.emptyList();
        }

        final ScanResultKey scanResultKey = mKey.getScanResultKey();
        final String ssid = scanResultKey.getSsid();
        final Set<Integer> securityTypes = scanResultKey.getSecurityTypes();
        mMatchingWifiConfigs.clear();
        for (WifiConfiguration config : wifiConfigs) {
            if (!TextUtils.equals(ssid, sanitizeSsid(config.SSID))) {
                throw new IllegalArgumentException(
                        "Attempted to update with wrong SSID!"
                                + " Expected: " + ssid
                                + ", Actual: " + sanitizeSsid(config.SSID)
                                + ", Config: " + config);
            }
            for (int securityType : getSecurityTypesFromWifiConfiguration(config)) {
                if (!securityTypes.contains(securityType)) {
                    throw new IllegalArgumentException(
                            "Attempted to update with wrong security!"
                                    + " Expected one of: " + securityTypes
                                    + ", Actual: " + securityType
                                    + ", Config: " + config);
                }
                if (isSecurityTypeSupported(securityType)) {
                    mMatchingWifiConfigs.put(securityType, config);
                }
            }
        }
        updateSecurityTypes();
        updateTargetScanResultInfo();
        notifyOnUpdated();
    }

    private boolean isSecurityTypeSupported(int security) {
        switch (security) {
            case SECURITY_TYPE_SAE:
                return mIsWpa3SaeSupported;
            case SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT:
                return mIsWpa3SuiteBSupported;
            case SECURITY_TYPE_OWE:
                return mIsEnhancedOpenSupported;
            default:
                return true;
        }
    }

    @Override
    protected synchronized void updateSecurityTypes() {
        mTargetSecurityTypes.clear();
        if (mWifiInfo != null) {
            final int wifiInfoSecurity = mWifiInfo.getCurrentSecurityType();
            if (wifiInfoSecurity != SECURITY_TYPE_UNKNOWN) {
                mTargetSecurityTypes.add(mWifiInfo.getCurrentSecurityType());
            }
        }

        Set<Integer> configSecurityTypes = mMatchingWifiConfigs.keySet();
        if (mTargetSecurityTypes.isEmpty() && mKey.isTargetingNewNetworks()) {
            // If we are targeting new networks for configuration, then we should select the
            // security type of all visible scan results if we don't have any configs that
            // can connect to them. This will let us configure this entry as a new network.
            boolean configMatchesScans = false;
            Set<Integer> scanSecurityTypes = mMatchingScanResults.keySet();
            for (int configSecurity : configSecurityTypes) {
                if (scanSecurityTypes.contains(configSecurity)) {
                    configMatchesScans = true;
                    break;
                }
            }
            if (!configMatchesScans) {
                mTargetSecurityTypes.addAll(scanSecurityTypes);
            }
        }

        // Use security types of any configs we have
        if (mTargetSecurityTypes.isEmpty()) {
            mTargetSecurityTypes.addAll(configSecurityTypes);
        }

        // Default to the key security types. This shouldn't happen since we should always have
        // scans or configs.
        if (mTargetSecurityTypes.isEmpty()) {
            mTargetSecurityTypes.addAll(mKey.getScanResultKey().getSecurityTypes());
        }

        // The target wifi config should match the security type we return in getSecurity(), since
        // clients (QR code/DPP, modify network page) may expect them to match.
        mTargetWifiConfig = mMatchingWifiConfigs.get(
                getSingleSecurityTypeFromMultipleSecurityTypes(mTargetSecurityTypes));
        // Collect target scan results in a set to remove duplicates when one scan matches multiple
        // security types.
        Set<ScanResult> targetScanResultSet = new ArraySet<>();
        for (int security : mTargetSecurityTypes) {
            if (mMatchingScanResults.containsKey(security)) {
                targetScanResultSet.addAll(mMatchingScanResults.get(security));
            }
        }
        mTargetScanResults.clear();
        mTargetScanResults.addAll(targetScanResultSet);
    }

    /**
     * Sets whether the suggested config for this entry is shareable to the user or not.
     */
    @WorkerThread
    synchronized void setUserShareable(boolean isUserShareable) {
        mIsUserShareable = isUserShareable;
    }

    /**
     * Returns whether the suggested config for this entry is shareable to the user or not.
     */
    @WorkerThread
    synchronized boolean isUserShareable() {
        return mIsUserShareable;
    }

    @WorkerThread
    protected synchronized boolean connectionInfoMatches(@NonNull WifiInfo wifiInfo,
            @NonNull NetworkInfo networkInfo) {
        if (wifiInfo.isPasspointAp() || wifiInfo.isOsuAp()) {
            return false;
        }
        for (WifiConfiguration config : mMatchingWifiConfigs.values()) {
            if (config.networkId == wifiInfo.getNetworkId()) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    static StandardWifiEntryKey ssidAndSecurityTypeToStandardWifiEntryKey(
            @NonNull String ssid, int security) {
        return ssidAndSecurityTypeToStandardWifiEntryKey(
                ssid, security, false /* isTargetingNewNetworks */);
    }

    @NonNull
    static StandardWifiEntryKey ssidAndSecurityTypeToStandardWifiEntryKey(
            @NonNull String ssid, int security, boolean isTargetingNewNetworks) {
        return new StandardWifiEntryKey(
                new ScanResultKey(ssid, Collections.singletonList(security)),
                isTargetingNewNetworks);
    }

    @Override
    protected synchronized String getScanResultDescription() {
        if (mTargetScanResults.size() == 0) {
            return "";
        }

        final StringBuilder description = new StringBuilder();
        description.append("[");
        description.append(getScanResultDescription(MIN_FREQ_24GHZ, MAX_FREQ_24GHZ)).append(";");
        description.append(getScanResultDescription(MIN_FREQ_5GHZ, MAX_FREQ_5GHZ)).append(";");
        description.append(getScanResultDescription(MIN_FREQ_6GHZ, MAX_FREQ_6GHZ)).append(";");
        description.append(getScanResultDescription(MIN_FREQ_60GHZ, MAX_FREQ_60GHZ));
        description.append("]");
        return description.toString();
    }

    private synchronized String getScanResultDescription(int minFrequency, int maxFrequency) {
        final List<ScanResult> scanResults = mTargetScanResults.stream()
                .filter(scanResult -> scanResult.frequency >= minFrequency
                        && scanResult.frequency <= maxFrequency)
                .sorted(Comparator.comparingInt(scanResult -> -1 * scanResult.level))
                .collect(Collectors.toList());

        final int scanResultCount = scanResults.size();
        if (scanResultCount == 0) {
            return "";
        }

        final StringBuilder description = new StringBuilder();
        description.append("(").append(scanResultCount).append(")");
        if (scanResultCount > MAX_VERBOSE_LOG_DISPLAY_SCANRESULT_COUNT) {
            final int maxLavel = scanResults.stream()
                    .mapToInt(scanResult -> scanResult.level).max().getAsInt();
            description.append("max=").append(maxLavel).append(",");
        }
        final long nowMs = SystemClock.elapsedRealtime();
        scanResults.forEach(scanResult ->
                description.append(getScanResultDescription(scanResult, nowMs)));
        return description.toString();
    }

    // TODO(b/227622961): Remove the suppression once the linter recognizes BuildCompat.isAtLeastT()
    @SuppressLint("NewApi")
    private synchronized String getScanResultDescription(ScanResult scanResult, long nowMs) {
        final StringBuilder description = new StringBuilder();
        description.append(" \n{");
        description.append(scanResult.BSSID);
        if (mWifiInfo != null && scanResult.BSSID.equals(mWifiInfo.getBSSID())) {
            description.append("*");
        }
        description.append("=").append(scanResult.frequency);
        description.append(",").append(scanResult.level);
        int wifiStandard = scanResult.getWifiStandard();
        description.append(",").append(Utils.getStandardString(mContext, wifiStandard));
        if (BuildCompat.isAtLeastT() && wifiStandard == ScanResult.WIFI_STANDARD_11BE) {
            description.append(",mldMac=").append(scanResult.getApMldMacAddress());
            description.append(",linkId=").append(scanResult.getApMloLinkId());
            description.append(",affLinks=").append(scanResult.getAffiliatedMloLinks());
        }
        final int ageSeconds = (int) (nowMs - scanResult.timestamp / 1000) / 1000;
        description.append(",").append(ageSeconds).append("s");
        description.append("}");
        return description.toString();
    }

    @Override
    String getNetworkSelectionDescription() {
        return Utils.getNetworkSelectionDescription(getWifiConfiguration());
    }

    // TODO(b/227622961): Remove the suppression once the linter recognizes BuildCompat.isAtLeastT()
    @SuppressLint("NewApi")
    void updateAdminRestrictions() {
        if (!BuildCompat.isAtLeastT()) {
            return;
        }
        if (mUserManager != null) {
            mHasAddConfigUserRestriction = mUserManager.hasUserRestriction(
                    UserManager.DISALLOW_ADD_WIFI_CONFIG);
        }
        if (mDevicePolicyManager != null) {
            //check minimum security level restriction
            int adminMinimumSecurityLevel =
                    mDevicePolicyManager.getMinimumRequiredWifiSecurityLevel();
            if (adminMinimumSecurityLevel != DevicePolicyManager.WIFI_SECURITY_OPEN) {
                boolean securityRestrictionPassed = false;
                for (int type : getSecurityTypes()) {
                    int securityLevel = Utils.convertSecurityTypeToDpmWifiSecurity(type);

                    // Skip unknown security type since security level cannot be determined.
                    // If all the security types are unknown when the minimum security level
                    // restriction is set, the device cannot connect to this network.
                    if (securityLevel == Utils.DPM_SECURITY_TYPE_UNKNOWN) continue;

                    if (adminMinimumSecurityLevel <= securityLevel) {
                        securityRestrictionPassed = true;
                        break;
                    }
                }
                if (!securityRestrictionPassed) {
                    mIsAdminRestricted = true;
                    return;
                }
            }
            //check SSID restriction
            WifiSsidPolicy policy = mDevicePolicyManager.getWifiSsidPolicy();
            if (policy != null) {
                int policyType = policy.getPolicyType();
                Set<WifiSsid> ssids = policy.getSsids();

                if (policyType == WifiSsidPolicy.WIFI_SSID_POLICY_TYPE_ALLOWLIST
                        && !ssids.contains(
                        WifiSsid.fromBytes(getSsid().getBytes(StandardCharsets.UTF_8)))) {
                    mIsAdminRestricted = true;
                    return;
                }
                if (policyType == WifiSsidPolicy.WIFI_SSID_POLICY_TYPE_DENYLIST
                        && ssids.contains(
                        WifiSsid.fromBytes(getSsid().getBytes(StandardCharsets.UTF_8)))) {
                    mIsAdminRestricted = true;
                    return;
                }
            }
        }
        mIsAdminRestricted = false;
    }

    private boolean hasAdminRestrictions() {
        if ((mHasAddConfigUserRestriction && !(isSaved() || isSuggestion()))
                || mIsAdminRestricted) {
            return true;
        }
        return false;
    }

    /**
     * Class that identifies a unique StandardWifiEntry by the following identifiers
     *     1) ScanResult key (SSID + grouped security types)
     *     2) Suggestion profile key
     *     3) Is network request or not
     *     4) Should prioritize configuring a new network (i.e. target the security type of an
     *     in-range unsaved network, rather than a config that has no scans)
     */
    static class StandardWifiEntryKey {
        private static final String KEY_SCAN_RESULT_KEY = "SCAN_RESULT_KEY";
        private static final String KEY_SUGGESTION_PROFILE_KEY = "SUGGESTION_PROFILE_KEY";
        private static final String KEY_IS_NETWORK_REQUEST = "IS_NETWORK_REQUEST";
        private static final String KEY_IS_TARGETING_NEW_NETWORKS = "IS_TARGETING_NEW_NETWORKS";

        @NonNull private ScanResultKey mScanResultKey;
        @Nullable private String mSuggestionProfileKey;
        private boolean mIsNetworkRequest;
        private boolean mIsTargetingNewNetworks = false;

        /**
         * Creates a StandardWifiEntryKey matching a ScanResultKey
         */
        StandardWifiEntryKey(@NonNull ScanResultKey scanResultKey) {
            this(scanResultKey, false /* isTargetingNewNetworks */);
        }

        /**
         * Creates a StandardWifiEntryKey matching a ScanResultKey and sets whether the entry
         * should target new networks or not.
         */
        StandardWifiEntryKey(@NonNull ScanResultKey scanResultKey, boolean isTargetingNewNetworks) {
            mScanResultKey = scanResultKey;
            mIsTargetingNewNetworks = isTargetingNewNetworks;
        }

        /**
         * Creates a StandardWifiEntryKey matching a WifiConfiguration
         */
        StandardWifiEntryKey(@NonNull WifiConfiguration config) {
            this(config, false /* isTargetingNewNetworks */);
        }

        /**
         * Creates a StandardWifiEntryKey matching a WifiConfiguration and sets whether the entry
         * should target new networks or not.
         */
        StandardWifiEntryKey(@NonNull WifiConfiguration config, boolean isTargetingNewNetworks) {
            mScanResultKey = new ScanResultKey(config);
            if (config.fromWifiNetworkSuggestion) {
                mSuggestionProfileKey = new StringJoiner(",")
                        .add(config.creatorName)
                        .add(String.valueOf(config.carrierId))
                        .add(String.valueOf(config.subscriptionId))
                        .toString();
            } else if (config.fromWifiNetworkSpecifier) {
                mIsNetworkRequest = true;
            }
            mIsTargetingNewNetworks = isTargetingNewNetworks;
        }

        /**
         * Creates a StandardWifiEntryKey from its String representation.
         */
        StandardWifiEntryKey(@NonNull String string) {
            mScanResultKey = new ScanResultKey();
            if (!string.startsWith(KEY_PREFIX)) {
                Log.e(TAG, "String key does not start with key prefix!");
                return;
            }
            try {
                final JSONObject keyJson = new JSONObject(string.substring(KEY_PREFIX.length()));
                if (keyJson.has(KEY_SCAN_RESULT_KEY)) {
                    mScanResultKey = new ScanResultKey(keyJson.getString(KEY_SCAN_RESULT_KEY));
                }
                if (keyJson.has(KEY_SUGGESTION_PROFILE_KEY)) {
                    mSuggestionProfileKey = keyJson.getString(KEY_SUGGESTION_PROFILE_KEY);
                }
                if (keyJson.has(KEY_IS_NETWORK_REQUEST)) {
                    mIsNetworkRequest = keyJson.getBoolean(KEY_IS_NETWORK_REQUEST);
                }
                if (keyJson.has(KEY_IS_TARGETING_NEW_NETWORKS)) {
                    mIsTargetingNewNetworks = keyJson.getBoolean(
                            KEY_IS_TARGETING_NEW_NETWORKS);
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSONException while converting StandardWifiEntryKey to string: " + e);
            }
        }

        /**
         * Returns the JSON String representation of this StandardWifiEntryKey.
         */
        @Override
        public String toString() {
            final JSONObject keyJson = new JSONObject();
            try {
                if (mScanResultKey != null) {
                    keyJson.put(KEY_SCAN_RESULT_KEY, mScanResultKey.toString());
                }
                if (mSuggestionProfileKey != null) {
                    keyJson.put(KEY_SUGGESTION_PROFILE_KEY, mSuggestionProfileKey);
                }
                if (mIsNetworkRequest) {
                    keyJson.put(KEY_IS_NETWORK_REQUEST, mIsNetworkRequest);
                }
                if (mIsTargetingNewNetworks) {
                    keyJson.put(KEY_IS_TARGETING_NEW_NETWORKS, mIsTargetingNewNetworks);
                }
            } catch (JSONException e) {
                Log.wtf(TAG, "JSONException while converting StandardWifiEntryKey to string: " + e);
            }
            return KEY_PREFIX + keyJson.toString();
        }

        /**
         * Returns the ScanResultKey of this StandardWifiEntryKey to match against ScanResults
         */
        @NonNull ScanResultKey getScanResultKey() {
            return mScanResultKey;
        }

        @Nullable String getSuggestionProfileKey() {
            return mSuggestionProfileKey;
        }

        boolean isNetworkRequest() {
            return mIsNetworkRequest;
        }

        boolean isTargetingNewNetworks() {
            return mIsTargetingNewNetworks;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StandardWifiEntryKey that = (StandardWifiEntryKey) o;
            return Objects.equals(mScanResultKey, that.mScanResultKey)
                    && TextUtils.equals(mSuggestionProfileKey, that.mSuggestionProfileKey)
                    && mIsNetworkRequest == that.mIsNetworkRequest;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mScanResultKey, mSuggestionProfileKey, mIsNetworkRequest);
        }
    }

    /**
     * Class for matching ScanResults to StandardWifiEntry by SSID and security type grouping.
     */
    static class ScanResultKey {
        private static final String KEY_SSID = "SSID";
        private static final String KEY_SECURITY_TYPES = "SECURITY_TYPES";

        @Nullable private String mSsid;
        @NonNull private Set<Integer> mSecurityTypes = new ArraySet<>();

        ScanResultKey() {
        }

        ScanResultKey(@Nullable String ssid, List<Integer> securityTypes) {
            mSsid = ssid;
            for (int security : securityTypes) {
                // Add any security types that merge to the same WifiEntry
                switch (security) {
                    case SECURITY_TYPE_PASSPOINT_R1_R2:
                    case SECURITY_TYPE_PASSPOINT_R3:
                        // Filter out Passpoint security type from key.
                        continue;
                    // Group OPEN and OWE networks together
                    case SECURITY_TYPE_OPEN:
                        mSecurityTypes.add(SECURITY_TYPE_OWE);
                        break;
                    case SECURITY_TYPE_OWE:
                        mSecurityTypes.add(SECURITY_TYPE_OPEN);
                        break;
                    // Group PSK and SAE networks together
                    case SECURITY_TYPE_PSK:
                        mSecurityTypes.add(SECURITY_TYPE_SAE);
                        break;
                    case SECURITY_TYPE_SAE:
                        mSecurityTypes.add(SECURITY_TYPE_PSK);
                        break;
                    // Group EAP and EAP_WPA3_ENTERPRISE networks together
                    case SECURITY_TYPE_EAP:
                        mSecurityTypes.add(SECURITY_TYPE_EAP_WPA3_ENTERPRISE);
                        break;
                    case SECURITY_TYPE_EAP_WPA3_ENTERPRISE:
                        mSecurityTypes.add(SECURITY_TYPE_EAP);
                        break;
                }
                mSecurityTypes.add(security);
            }
        }

        /**
         * Creates a ScanResultKey from a ScanResult's SSID and security type grouping.
         * @param scanResult
         */
        ScanResultKey(@NonNull ScanResult scanResult) {
            this(scanResult.SSID, getSecurityTypesFromScanResult(scanResult));
        }

        /**
         * Creates a ScanResultKey from a WifiConfiguration's SSID and security type grouping.
         */
        ScanResultKey(@NonNull WifiConfiguration wifiConfiguration) {
            this(sanitizeSsid(wifiConfiguration.SSID),
                    getSecurityTypesFromWifiConfiguration(wifiConfiguration));
        }

        /**
         * Creates a ScanResultKey from its String representation.
         */
        ScanResultKey(@NonNull String string) {
            try {
                final JSONObject keyJson = new JSONObject(string);
                mSsid = keyJson.getString(KEY_SSID);
                final JSONArray securityTypesJson =
                        keyJson.getJSONArray(KEY_SECURITY_TYPES);
                for (int i = 0; i < securityTypesJson.length(); i++) {
                    mSecurityTypes.add(securityTypesJson.getInt(i));
                }
            } catch (JSONException e) {
                Log.wtf(TAG, "JSONException while constructing ScanResultKey from string: " + e);
            }
        }

        /**
         * Returns the JSON String representation of this ScanResultEntry.
         */
        @Override
        public String toString() {
            final JSONObject keyJson = new JSONObject();
            try {
                if (mSsid != null) {
                    keyJson.put(KEY_SSID, mSsid);
                }
                if (!mSecurityTypes.isEmpty()) {
                    final JSONArray securityTypesJson = new JSONArray();
                    for (int security : mSecurityTypes) {
                        securityTypesJson.put(security);
                    }
                    keyJson.put(KEY_SECURITY_TYPES, securityTypesJson);
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSONException while converting ScanResultKey to string: " + e);
            }
            return keyJson.toString();
        }

        @Nullable String getSsid() {
            return mSsid;
        }

        @NonNull Set<Integer> getSecurityTypes() {
            return mSecurityTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScanResultKey that = (ScanResultKey) o;
            return TextUtils.equals(mSsid, that.mSsid)
                    && mSecurityTypes.equals(that.mSecurityTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mSsid, mSecurityTypes);
        }
    }
}

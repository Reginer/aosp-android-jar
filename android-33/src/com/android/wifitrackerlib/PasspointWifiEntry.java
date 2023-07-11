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

import static android.net.wifi.WifiInfo.DEFAULT_MAC_ADDRESS;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_PASSPOINT_R1_R2;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_PASSPOINT_R3;
import static android.net.wifi.WifiInfo.SECURITY_TYPE_UNKNOWN;
import static android.net.wifi.WifiInfo.sanitizeSsid;

import static androidx.core.util.Preconditions.checkNotNull;

import static com.android.wifitrackerlib.Utils.getAutoConnectDescription;
import static com.android.wifitrackerlib.Utils.getBestScanResultByLevel;
import static com.android.wifitrackerlib.Utils.getConnectedDescription;
import static com.android.wifitrackerlib.Utils.getConnectingDescription;
import static com.android.wifitrackerlib.Utils.getDisconnectedDescription;
import static com.android.wifitrackerlib.Utils.getImsiProtectionDescription;
import static com.android.wifitrackerlib.Utils.getMeteredDescription;
import static com.android.wifitrackerlib.Utils.getVerboseLoggingDescription;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.Handler;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * WifiEntry representation of a subscribed Passpoint network, uniquely identified by FQDN.
 */
@VisibleForTesting
public class PasspointWifiEntry extends WifiEntry implements WifiEntry.WifiEntryCallback {
    static final String TAG = "PasspointWifiEntry";
    public static final String KEY_PREFIX = "PasspointWifiEntry:";

    private final List<ScanResult> mCurrentHomeScanResults = new ArrayList<>();
    private final List<ScanResult> mCurrentRoamingScanResults = new ArrayList<>();

    @NonNull private final String mKey;
    @NonNull private final String mFqdn;
    @NonNull private final String mFriendlyName;
    @NonNull private final WifiTrackerInjector mInjector;
    @NonNull private final Context mContext;
    @Nullable
    private PasspointConfiguration mPasspointConfig;
    @Nullable private WifiConfiguration mWifiConfig;
    private List<Integer> mTargetSecurityTypes =
            List.of(SECURITY_TYPE_PASSPOINT_R1_R2, SECURITY_TYPE_PASSPOINT_R3);

    private boolean mIsRoaming = false;
    private OsuWifiEntry mOsuWifiEntry;
    private boolean mShouldAutoOpenCaptivePortal = false;

    protected long mSubscriptionExpirationTimeInMillis;

    // PasspointConfiguration#setMeteredOverride(int meteredOverride) is a hide API and we can't
    // set it in PasspointWifiEntry#setMeteredChoice(int meteredChoice).
    // For PasspointWifiEntry#getMeteredChoice() to return correct value right after
    // PasspointWifiEntry#setMeteredChoice(int meteredChoice), cache
    // PasspointConfiguration#getMeteredOverride() in this variable.
    private int mMeteredOverride = METERED_CHOICE_AUTO;

    /**
     * Create a PasspointWifiEntry with the associated PasspointConfiguration
     */
    PasspointWifiEntry(
            @NonNull WifiTrackerInjector injector,
            @NonNull Context context, @NonNull Handler callbackHandler,
            @NonNull PasspointConfiguration passpointConfig,
            @NonNull WifiManager wifiManager,
            boolean forSavedNetworksPage) throws IllegalArgumentException {
        super(callbackHandler, wifiManager, forSavedNetworksPage);

        checkNotNull(passpointConfig, "Cannot construct with null PasspointConfiguration!");
        mInjector = injector;
        mContext = context;
        mPasspointConfig = passpointConfig;
        mKey = uniqueIdToPasspointWifiEntryKey(passpointConfig.getUniqueId());
        mFqdn = passpointConfig.getHomeSp().getFqdn();
        checkNotNull(mFqdn, "Cannot construct with null PasspointConfiguration FQDN!");
        mFriendlyName = passpointConfig.getHomeSp().getFriendlyName();
        mSubscriptionExpirationTimeInMillis =
                passpointConfig.getSubscriptionExpirationTimeMillis();
        mMeteredOverride = mPasspointConfig.getMeteredOverride();
    }

    /**
     * Create a PasspointWifiEntry with the associated WifiConfiguration for use with network
     * suggestions, since WifiManager#getAllMatchingWifiConfigs() does not provide a corresponding
     * PasspointConfiguration.
     */
    PasspointWifiEntry(
            @NonNull WifiTrackerInjector injector,
            @NonNull Context context, @NonNull Handler callbackHandler,
            @NonNull WifiConfiguration wifiConfig,
            @NonNull WifiManager wifiManager,
            boolean forSavedNetworksPage) throws IllegalArgumentException {
        super(callbackHandler, wifiManager, forSavedNetworksPage);

        checkNotNull(wifiConfig, "Cannot construct with null WifiConfiguration!");
        if (!wifiConfig.isPasspoint()) {
            throw new IllegalArgumentException("Given WifiConfiguration is not for Passpoint!");
        }
        mInjector = injector;
        mContext = context;
        mWifiConfig = wifiConfig;
        mKey = uniqueIdToPasspointWifiEntryKey(wifiConfig.getKey());
        mFqdn = wifiConfig.FQDN;
        checkNotNull(mFqdn, "Cannot construct with null WifiConfiguration FQDN!");
        mFriendlyName = mWifiConfig.providerFriendlyName;
    }

    @Override
    public String getKey() {
        return mKey;
    }

    @Override
    @ConnectedState
    public synchronized int getConnectedState() {
        if (isExpired()) {
            if (super.getConnectedState() == CONNECTED_STATE_DISCONNECTED
                    && mOsuWifiEntry != null) {
                return mOsuWifiEntry.getConnectedState();
            }
        }
        return super.getConnectedState();
    }

    @Override
    public String getTitle() {
        return mFriendlyName;
    }

    @Override
    public synchronized String getSummary(boolean concise) {
        StringJoiner sj = new StringJoiner(mContext.getString(
                R.string.wifitrackerlib_summary_separator));

        if (isExpired()) {
            if (mOsuWifiEntry != null) {
                sj.add(mOsuWifiEntry.getSummary(concise));
            } else {
                sj.add(mContext.getString(R.string.wifitrackerlib_wifi_passpoint_expired));
            }
        } else {
            final String connectedStateDescription;
            final @ConnectedState int connectedState = getConnectedState();
            switch (connectedState) {
                case CONNECTED_STATE_DISCONNECTED:
                    connectedStateDescription = getDisconnectedDescription(mInjector, mContext,
                            mWifiConfig,
                            mForSavedNetworksPage,
                            concise);
                    break;
                case CONNECTED_STATE_CONNECTING:
                    connectedStateDescription = getConnectingDescription(mContext, mNetworkInfo);
                    break;
                case CONNECTED_STATE_CONNECTED:
                    connectedStateDescription = getConnectedDescription(mContext,
                            mWifiConfig,
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
        }

        String autoConnectDescription = getAutoConnectDescription(mContext, this);
        if (!TextUtils.isEmpty(autoConnectDescription)) {
            sj.add(autoConnectDescription);
        }

        String meteredDescription = getMeteredDescription(mContext, this);
        if (!TextUtils.isEmpty(meteredDescription)) {
            sj.add(meteredDescription);
        }

        if (!concise) {
            String verboseLoggingDescription = getVerboseLoggingDescription(this);
            if (!TextUtils.isEmpty(verboseLoggingDescription)) {
                sj.add(verboseLoggingDescription);
            }
        }

        return sj.toString();
    }

    @Override
    public synchronized CharSequence getSecondSummary() {
        return getConnectedState() == CONNECTED_STATE_CONNECTED
                ? getImsiProtectionDescription(mContext, mWifiConfig) : "";
    }

    @Override
    public synchronized String getSsid() {
        if (mWifiInfo != null) {
            return sanitizeSsid(mWifiInfo.getSSID());
        }

        return mWifiConfig != null ? sanitizeSsid(mWifiConfig.SSID) : null;
    }

    synchronized Set<String> getAllUtf8Ssids() {
        Set<String> allSsids = new ArraySet<>();
        for (ScanResult scan : mCurrentHomeScanResults) {
            allSsids.add(scan.SSID);
        }
        for (ScanResult scan : mCurrentRoamingScanResults) {
            allSsids.add(scan.SSID);
        }
        return allSsids;
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
        if (mWifiConfig == null || getPrivacy() != PRIVACY_RANDOMIZED_MAC) {
            final String[] factoryMacs = mWifiManager.getFactoryMacAddresses();
            if (factoryMacs.length > 0) {
                return factoryMacs[0];
            }
            return null;
        }
        return mWifiConfig.getRandomizedMacAddress().toString();
    }

    @Override
    public synchronized boolean isMetered() {
        return getMeteredChoice() == METERED_CHOICE_METERED
                || (mWifiConfig != null && mWifiConfig.meteredHint);
    }

    @Override
    public synchronized boolean isSuggestion() {
        return mWifiConfig != null && mWifiConfig.fromWifiNetworkSuggestion;
    }

    @Override
    public synchronized boolean isSubscription() {
        return mPasspointConfig != null;
    }

    @Override
    public synchronized boolean canConnect() {
        if (isExpired()) {
            return mOsuWifiEntry != null && mOsuWifiEntry.canConnect();
        }

        return mLevel != WIFI_LEVEL_UNREACHABLE
                && getConnectedState() == CONNECTED_STATE_DISCONNECTED && mWifiConfig != null;
    }

    @Override
    public synchronized void connect(@Nullable ConnectCallback callback) {
        if (isExpired()) {
            if (mOsuWifiEntry != null) {
                mOsuWifiEntry.connect(callback);
                return;
            }
        }
        // We should flag this network to auto-open captive portal since this method represents
        // the user manually connecting to a network (i.e. not auto-join).
        mShouldAutoOpenCaptivePortal = true;
        mConnectCallback = callback;

        if (mWifiConfig == null) {
            // We should not be able to call connect() if mWifiConfig is null
            new ConnectActionListener().onFailure(0);
        }
        mWifiManager.stopRestrictingAutoJoinToSubscriptionId();
        mWifiManager.connect(mWifiConfig, new ConnectActionListener());
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
            mWifiManager.disableEphemeralNetwork(mFqdn);
            mWifiManager.disconnect();
        }
    }

    @Override
    public synchronized boolean canForget() {
        return !isSuggestion() && mPasspointConfig != null;
    }

    @Override
    public synchronized void forget(@Nullable ForgetCallback callback) {
        if (!canForget()) {
            return;
        }

        mForgetCallback = callback;
        mWifiManager.removePasspointConfiguration(mPasspointConfig.getHomeSp().getFqdn());
        new ForgetActionListener().onSuccess();
    }

    @Override
    @MeteredChoice
    public synchronized int getMeteredChoice() {
        if (mMeteredOverride == WifiConfiguration.METERED_OVERRIDE_METERED) {
            return METERED_CHOICE_METERED;
        } else if (mMeteredOverride == WifiConfiguration.METERED_OVERRIDE_NOT_METERED) {
            return METERED_CHOICE_UNMETERED;
        }
        return METERED_CHOICE_AUTO;
    }

    @Override
    public synchronized boolean canSetMeteredChoice() {
        return !isSuggestion() && mPasspointConfig != null;
    }

    @Override
    public synchronized void setMeteredChoice(int meteredChoice) {
        if (mPasspointConfig == null || !canSetMeteredChoice()) {
            return;
        }

        switch (meteredChoice) {
            case METERED_CHOICE_AUTO:
                mMeteredOverride = WifiConfiguration.METERED_OVERRIDE_NONE;
                break;
            case METERED_CHOICE_METERED:
                mMeteredOverride = WifiConfiguration.METERED_OVERRIDE_METERED;
                break;
            case METERED_CHOICE_UNMETERED:
                mMeteredOverride = WifiConfiguration.METERED_OVERRIDE_NOT_METERED;
                break;
            default:
                // Do nothing.
                return;
        }
        mWifiManager.setPasspointMeteredOverride(mPasspointConfig.getHomeSp().getFqdn(),
                mMeteredOverride);
    }

    @Override
    public synchronized boolean canSetPrivacy() {
        return !isSuggestion() && mPasspointConfig != null;
    }

    @Override
    @Privacy
    public synchronized int getPrivacy() {
        if (mPasspointConfig == null) {
            return PRIVACY_RANDOMIZED_MAC;
        }

        return mPasspointConfig.isMacRandomizationEnabled()
                ? PRIVACY_RANDOMIZED_MAC : PRIVACY_DEVICE_MAC;
    }

    @Override
    public synchronized void setPrivacy(int privacy) {
        if (mPasspointConfig == null || !canSetPrivacy()) {
            return;
        }

        mWifiManager.setMacRandomizationSettingPasspointEnabled(
                mPasspointConfig.getHomeSp().getFqdn(),
                privacy == PRIVACY_DEVICE_MAC ? false : true);
    }

    @Override
    public synchronized boolean isAutoJoinEnabled() {
        // Suggestion network; use WifiConfig instead
        if (mPasspointConfig != null) {
            return mPasspointConfig.isAutojoinEnabled();
        }
        if (mWifiConfig != null) {
            return mWifiConfig.allowAutojoin;
        }
        return false;
    }

    @Override
    public synchronized boolean canSetAutoJoinEnabled() {
        return mPasspointConfig != null || mWifiConfig != null;
    }

    @Override
    public synchronized void setAutoJoinEnabled(boolean enabled) {
        if (mPasspointConfig != null) {
            mWifiManager.allowAutojoinPasspoint(mPasspointConfig.getHomeSp().getFqdn(), enabled);
        } else if (mWifiConfig != null) {
            mWifiManager.allowAutojoin(mWifiConfig.networkId, enabled);
        }
    }

    @Override
    public String getSecurityString(boolean concise) {
        return mContext.getString(R.string.wifitrackerlib_wifi_security_passpoint);
    }

    @Override
    public synchronized String getStandardString() {
        if (mWifiInfo != null) {
            return Utils.getStandardString(mContext, mWifiInfo.getWifiStandard());
        }
        if (!mCurrentHomeScanResults.isEmpty()) {
            return Utils.getStandardString(
                    mContext, mCurrentHomeScanResults.get(0).getWifiStandard());
        }
        if (!mCurrentRoamingScanResults.isEmpty()) {
            return Utils.getStandardString(
                    mContext, mCurrentRoamingScanResults.get(0).getWifiStandard());
        }
        return "";
    }

    @Override
    public synchronized boolean isExpired() {
        if (mSubscriptionExpirationTimeInMillis <= 0) {
            // Expiration time not specified.
            return false;
        } else {
            return System.currentTimeMillis() >= mSubscriptionExpirationTimeInMillis;
        }
    }

    @WorkerThread
    synchronized void updatePasspointConfig(@Nullable PasspointConfiguration passpointConfig) {
        mPasspointConfig = passpointConfig;
        if (mPasspointConfig != null) {
            mSubscriptionExpirationTimeInMillis =
                    passpointConfig.getSubscriptionExpirationTimeMillis();
            mMeteredOverride = passpointConfig.getMeteredOverride();
        }
        notifyOnUpdated();
    }

    @WorkerThread
    synchronized void updateScanResultInfo(@Nullable WifiConfiguration wifiConfig,
            @Nullable List<ScanResult> homeScanResults,
            @Nullable List<ScanResult> roamingScanResults)
            throws IllegalArgumentException {
        mIsRoaming = false;
        mWifiConfig = wifiConfig;
        mCurrentHomeScanResults.clear();
        mCurrentRoamingScanResults.clear();
        if (homeScanResults != null) {
            mCurrentHomeScanResults.addAll(homeScanResults);
        }
        if (roamingScanResults != null) {
            mCurrentRoamingScanResults.addAll(roamingScanResults);
        }
        if (mWifiConfig != null) {
            List<ScanResult> currentScanResults = new ArrayList<>();
            ScanResult bestScanResult = null;
            if (homeScanResults != null && !homeScanResults.isEmpty()) {
                currentScanResults.addAll(homeScanResults);
            } else if (roamingScanResults != null && !roamingScanResults.isEmpty()) {
                currentScanResults.addAll(roamingScanResults);
                mIsRoaming = true;
            }
            bestScanResult = getBestScanResultByLevel(currentScanResults);
            if (bestScanResult != null) {
                mWifiConfig.SSID = "\"" + bestScanResult.SSID + "\"";
            }
            if (getConnectedState() == CONNECTED_STATE_DISCONNECTED) {
                mLevel = bestScanResult != null
                        ? mWifiManager.calculateSignalLevel(bestScanResult.level)
                        : WIFI_LEVEL_UNREACHABLE;
            }
        } else {
            mLevel = WIFI_LEVEL_UNREACHABLE;
        }
        notifyOnUpdated();
    }

    @Override
    protected synchronized void updateSecurityTypes() {
        if (mWifiInfo != null) {
            final int wifiInfoSecurity = mWifiInfo.getCurrentSecurityType();
            if (wifiInfoSecurity != SECURITY_TYPE_UNKNOWN) {
                mTargetSecurityTypes = Collections.singletonList(wifiInfoSecurity);
                return;
            }
        }
    }

    @WorkerThread
    @Override
    protected boolean connectionInfoMatches(@NonNull WifiInfo wifiInfo,
            @NonNull NetworkInfo networkInfo) {
        if (!wifiInfo.isPasspointAp()) {
            return false;
        }

        // Match with FQDN until WifiInfo supports returning the passpoint uniqueID
        return TextUtils.equals(wifiInfo.getPasspointFqdn(), mFqdn);
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

    @NonNull
    static String uniqueIdToPasspointWifiEntryKey(@NonNull String uniqueId) {
        checkNotNull(uniqueId, "Cannot create key with null unique id!");
        return KEY_PREFIX + uniqueId;
    }

    @Override
    protected String getScanResultDescription() {
        // TODO(b/70983952): Fill this method in.
        return "";
    }

    @Override
    synchronized String getNetworkSelectionDescription() {
        return Utils.getNetworkSelectionDescription(mWifiConfig);
    }

    /** Pass a reference to a matching OsuWifiEntry for expiration handling */
    synchronized void setOsuWifiEntry(OsuWifiEntry osuWifiEntry) {
        mOsuWifiEntry = osuWifiEntry;
        if (mOsuWifiEntry != null) {
            mOsuWifiEntry.setListener(this);
        }
    }

    /** Callback for updates to the linked OsuWifiEntry */
    @Override
    public void onUpdated() {
        notifyOnUpdated();
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

    /** Get the PasspointConfiguration instance of the entry. */
    public PasspointConfiguration getPasspointConfig() {
        return mPasspointConfig;
    }
}

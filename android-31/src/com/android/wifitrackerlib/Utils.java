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

import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.NETWORK_SELECTION_ENABLED;
import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.NETWORK_SELECTION_PERMANENTLY_DISABLED;

import static com.android.wifitrackerlib.WifiEntry.SPEED_FAST;
import static com.android.wifitrackerlib.WifiEntry.SPEED_MODERATE;
import static com.android.wifitrackerlib.WifiEntry.SPEED_NONE;
import static com.android.wifitrackerlib.WifiEntry.SPEED_SLOW;
import static com.android.wifitrackerlib.WifiEntry.SPEED_VERY_FAST;
import static com.android.wifitrackerlib.WifiEntry.Speed;

import static java.util.Comparator.comparingInt;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkKey;
import android.net.NetworkScoreManager;
import android.net.ScoredNetwork;
import android.net.WifiKey;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiNetworkScoreCache;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ClickableSpan;
import android.util.FeatureFlagUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.HelpUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Utility methods for WifiTrackerLib.
 */
public class Utils {
    /** Copy of the @hide Settings.Global.USE_OPEN_WIFI_PACKAGE constant. */
    static final String SETTINGS_GLOBAL_USE_OPEN_WIFI_PACKAGE = "use_open_wifi_package";

    @VisibleForTesting
    static FeatureFlagUtilsWrapper sFeatureFlagUtilsWrapper = new FeatureFlagUtilsWrapper();

    static class FeatureFlagUtilsWrapper {
        boolean isProviderModelEnabled(Context context) {
            return FeatureFlagUtils.isEnabled(context, FeatureFlagUtils.SETTINGS_PROVIDER_MODEL);
        }
    }

    private static NetworkScoreManager sNetworkScoreManager;

    private static String getActiveScorerPackage(@NonNull Context context) {
        if (sNetworkScoreManager == null) {
            sNetworkScoreManager = context.getSystemService(NetworkScoreManager.class);
        }
        return sNetworkScoreManager.getActiveScorerPackage();
    }

    // Returns the ScanResult with the best RSSI from a list of ScanResults.
    @Nullable
    public static ScanResult getBestScanResultByLevel(@NonNull List<ScanResult> scanResults) {
        if (scanResults.isEmpty()) return null;

        return Collections.max(scanResults, comparingInt(scanResult -> scanResult.level));
    }

    // Returns a list of WifiInfo SECURITY_TYPE_* supported by a ScanResult.
    // TODO(b/187755981): Move to shared static utils class
    @NonNull
    static List<Integer> getSecurityTypesFromScanResult(@NonNull ScanResult scanResult) {
        List<Integer> securityTypes = new ArrayList<>();

        // Open network & its upgradable types
        if (isScanResultForOweTransitionNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_OPEN);
            securityTypes.add(WifiInfo.SECURITY_TYPE_OWE);
            return securityTypes;
        } else if (isScanResultForOweNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_OWE);
            return securityTypes;
        } else if (isScanResultForOpenNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_OPEN);
            return securityTypes;
        }

        // WEP network which has no upgradable type
        if (isScanResultForWepNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_WEP);
            return securityTypes;
        }

        // WAPI PSK network which has no upgradable type
        if (isScanResultForWapiPskNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_WAPI_PSK);
            return securityTypes;
        }

        // WAPI CERT network which has no upgradable type
        if (isScanResultForWapiCertNetwork(scanResult)) {
            securityTypes.add(
                    WifiInfo.SECURITY_TYPE_WAPI_CERT);
            return securityTypes;
        }

        // WPA2 personal network & its upgradable types
        if (isScanResultForPskNetwork(scanResult)
                && isScanResultForSaeNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_PSK);
            securityTypes.add(WifiInfo.SECURITY_TYPE_SAE);
            return securityTypes;
        } else if (isScanResultForPskNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_PSK);
            return securityTypes;
        } else if (isScanResultForSaeNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_SAE);
            return securityTypes;
        }

        // WPA3 Enterprise 192-bit mode, WPA2/WPA3 enterprise network & its upgradable types
        if (isScanResultForEapSuiteBNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT);
        } else if (isScanResultForWpa3EnterpriseTransitionNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_EAP);
            securityTypes.add(WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE);
        } else if (isScanResultForWpa3EnterpriseOnlyNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE);
        } else if (isScanResultForEapNetwork(scanResult)) {
            securityTypes.add(WifiInfo.SECURITY_TYPE_EAP);
        }
        return securityTypes;
    }

    // Returns a list of WifiInfo SECURITY_TYPE_* supported by a WifiConfiguration
    // TODO(b/187755473): Use new public APIs to get the security type instead of relying on the
    //                    legacy allowedKeyManagement bitset.
    static List<Integer> getSecurityTypesFromWifiConfiguration(@NonNull WifiConfiguration config) {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WAPI_CERT)) {
            return Arrays.asList(WifiInfo.SECURITY_TYPE_WAPI_CERT);
        } else if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WAPI_PSK)) {
            return Arrays.asList(WifiInfo.SECURITY_TYPE_WAPI_PSK);
        } else if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.SUITE_B_192)) {
            return Arrays.asList(WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT);
        } else if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.OWE)) {
            return Arrays.asList(WifiInfo.SECURITY_TYPE_OWE);
        } else if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.SAE)) {
            return Arrays.asList(WifiInfo.SECURITY_TYPE_SAE);
        } else if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA2_PSK)) {
            return Arrays.asList(WifiInfo.SECURITY_TYPE_PSK);
        } else if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)) {
            if (config.requirePmf
                    && !config.allowedPairwiseCiphers.get(WifiConfiguration.PairwiseCipher.TKIP)
                    && config.allowedProtocols.get(WifiConfiguration.Protocol.RSN)) {
                return Arrays.asList(WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE);
            } else {
                // WPA2 configs should also be valid for WPA3-Enterprise APs
                return Arrays.asList(
                        WifiInfo.SECURITY_TYPE_EAP, WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE);
            }
        } else if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            return Arrays.asList(WifiInfo.SECURITY_TYPE_PSK);
        } else if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
            if (config.wepKeys != null) {
                for (int i = 0; i < config.wepKeys.length; i++) {
                    if (config.wepKeys[i] != null) {
                        return Arrays.asList(WifiInfo.SECURITY_TYPE_WEP);
                    }
                }
            }
        }
        return Arrays.asList(WifiInfo.SECURITY_TYPE_OPEN);
    }

    /**
     * Returns a single WifiInfo security type from the list of multiple WifiInfo security
     * types supported by an entry.
     *
     * Single security types will have a 1-to-1 mapping.
     * Multiple security type networks will collapse to the lowest security type in the group:
     *     - Open/OWE -> Open
     *     - PSK/SAE -> PSK
     *     - EAP/EAP-WPA3 -> EAP
     */
    static int getSingleSecurityTypeFromMultipleSecurityTypes(
            @NonNull List<Integer> securityTypes) {
        if (securityTypes.size() == 1) {
            return securityTypes.get(0);
        } else if (securityTypes.size() == 2) {
            if (securityTypes.contains(WifiInfo.SECURITY_TYPE_OPEN)) {
                return WifiInfo.SECURITY_TYPE_OPEN;
            }
            if (securityTypes.contains(WifiInfo.SECURITY_TYPE_PSK)) {
                return WifiInfo.SECURITY_TYPE_PSK;
            }
            if (securityTypes.contains(WifiInfo.SECURITY_TYPE_EAP)) {
                return WifiInfo.SECURITY_TYPE_EAP;
            }
        }
        return WifiInfo.SECURITY_TYPE_UNKNOWN;
    }

    @Speed
    public static int getAverageSpeedFromScanResults(@NonNull WifiNetworkScoreCache scoreCache,
            @NonNull List<ScanResult> scanResults) {
        int count = 0;
        int totalSpeed = 0;
        for (ScanResult scanResult : scanResults) {
            ScoredNetwork scoredNetwork = scoreCache.getScoredNetwork(scanResult);
            if (scoredNetwork == null) {
                continue;
            }
            @Speed int speed = scoredNetwork.calculateBadge(scanResult.level);
            if (speed != SPEED_NONE) {
                count++;
                totalSpeed += speed;
            }
        }
        if (count == 0) {
            return SPEED_NONE;
        } else {
            return roundToClosestSpeedEnum(totalSpeed / count);
        }
    }

    @Speed
    public static int getSpeedFromWifiInfo(@NonNull WifiNetworkScoreCache scoreCache,
            @NonNull WifiInfo wifiInfo) {
        final WifiKey wifiKey;
        try {
            wifiKey = new WifiKey(wifiInfo.getSSID(), wifiInfo.getBSSID());
        } catch (IllegalArgumentException e) {
            return SPEED_NONE;
        }
        ScoredNetwork scoredNetwork = scoreCache.getScoredNetwork(
                new NetworkKey(wifiKey));
        if (scoredNetwork == null) {
            return SPEED_NONE;
        }
        return roundToClosestSpeedEnum(scoredNetwork.calculateBadge(wifiInfo.getRssi()));
    }

    @Speed
    private static int roundToClosestSpeedEnum(int speed) {
        if (speed == SPEED_NONE) {
            return SPEED_NONE;
        } else if (speed < (SPEED_SLOW + SPEED_MODERATE) / 2) {
            return SPEED_SLOW;
        } else if (speed < (SPEED_MODERATE + SPEED_FAST) / 2) {
            return SPEED_MODERATE;
        } else if (speed < (SPEED_FAST + SPEED_VERY_FAST) / 2) {
            return SPEED_FAST;
        } else {
            return SPEED_VERY_FAST;
        }
    }

    /**
     * Get the app label for a suggestion/specifier package name, or an empty String if none exist
     */
    static String getAppLabel(Context context, String packageName) {
        try {
            String openWifiPackageName = Settings.Global.getString(context.getContentResolver(),
                    SETTINGS_GLOBAL_USE_OPEN_WIFI_PACKAGE);
            if (!TextUtils.isEmpty(openWifiPackageName) && TextUtils.equals(packageName,
                    getActiveScorerPackage(context))) {
                packageName = openWifiPackageName;
            }

            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    packageName,
                    0 /* flags */);
            return appInfo.loadLabel(context.getPackageManager()).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    static String getConnectedDescription(Context context,
            WifiConfiguration wifiConfiguration,
            NetworkCapabilities networkCapabilities,
            String recommendationServiceLabel,
            boolean isDefaultNetwork,
            boolean isLowQuality) {
        final StringJoiner sj = new StringJoiner(context.getString(
                R.string.wifitrackerlib_summary_separator));
        final boolean hideConnected =
                !isDefaultNetwork && sFeatureFlagUtilsWrapper.isProviderModelEnabled(context);

        if (wifiConfiguration != null) {
            if (wifiConfiguration.fromWifiNetworkSuggestion
                    || wifiConfiguration.fromWifiNetworkSpecifier) {
                // For suggestion or specifier networks to show "Connected via ..."
                final String suggestionOrSpecifierLabel =
                        getSuggestionOrSpecifierLabel(context, wifiConfiguration);
                if (!TextUtils.isEmpty(suggestionOrSpecifierLabel)) {
                    if (hideConnected) {
                        sj.add(context.getString(R.string.wifitrackerlib_available_via_app,
                                suggestionOrSpecifierLabel));
                    } else {
                        sj.add(context.getString(R.string.wifitrackerlib_connected_via_app,
                                suggestionOrSpecifierLabel));
                    }
                }
            } else if (wifiConfiguration.isEphemeral() && !hideConnected) {
                // For ephemeral networks to show "Automatically connected via ..."
                if (!TextUtils.isEmpty(recommendationServiceLabel)) {
                    sj.add(String.format(context.getString(
                            R.string.wifitrackerlib_connected_via_network_scorer),
                            recommendationServiceLabel));
                } else {
                    sj.add(context.getString(
                            R.string.wifitrackerlib_connected_via_network_scorer_default));
                }
            }
        }

        if (isLowQuality) {
            sj.add(context.getString(R.string.wifi_connected_low_quality));
        }

        // For displaying network capability info, such as captive portal or no internet
        String networkCapabilitiesInformation =
                getCurrentNetworkCapabilitiesInformation(context,  networkCapabilities);
        if (!TextUtils.isEmpty(networkCapabilitiesInformation)) {
            sj.add(networkCapabilitiesInformation);
        }

        // Default to "Connected" if nothing else to display
        if (sj.length() == 0 && !hideConnected) {
            return context.getResources().getStringArray(R.array.wifitrackerlib_wifi_status)
                    [DetailedState.CONNECTED.ordinal()];
        }

        return sj.toString();
    }

    static String getConnectingDescription(Context context, NetworkInfo networkInfo) {
        if (context == null || networkInfo == null) {
            return "";
        }
        DetailedState detailedState = networkInfo.getDetailedState();
        if (detailedState == null) {
            return "";
        }

        final String[] wifiStatusArray = context.getResources()
                .getStringArray(R.array.wifitrackerlib_wifi_status);
        final int index = detailedState.ordinal();
        return index >= wifiStatusArray.length ? "" : wifiStatusArray[index];
    }


    static String getDisconnectedDescription(Context context,
            WifiConfiguration wifiConfiguration,
            boolean forSavedNetworksPage,
            boolean concise) {
        if (context == null) {
            return "";
        }
        final StringJoiner sj = new StringJoiner(context.getString(
                R.string.wifitrackerlib_summary_separator));

        // For "Saved", "Saved by ...", and "Available via..."
        if (concise) {
            sj.add(context.getString(R.string.wifitrackerlib_wifi_disconnected));
        } else if (wifiConfiguration != null) {
            if (forSavedNetworksPage && !wifiConfiguration.isPasspoint()) {
                final CharSequence appLabel = getAppLabel(context, wifiConfiguration.creatorName);
                if (!TextUtils.isEmpty(appLabel)) {
                    sj.add(context.getString(R.string.wifitrackerlib_saved_network, appLabel));
                }
            } else {
                if (wifiConfiguration.fromWifiNetworkSuggestion) {
                    final String suggestionOrSpecifierLabel =
                            getSuggestionOrSpecifierLabel(context, wifiConfiguration);
                    if (!TextUtils.isEmpty(suggestionOrSpecifierLabel)) {
                        sj.add(context.getString(
                                R.string.wifitrackerlib_available_via_app,
                                suggestionOrSpecifierLabel));
                    }
                } else {
                    sj.add(context.getString(R.string.wifitrackerlib_wifi_remembered));
                }
            }
        }

        // For failure messages and disabled reasons
        final String wifiConfigFailureMessage =
                getWifiConfigurationFailureMessage(context, wifiConfiguration);
        if (!TextUtils.isEmpty(wifiConfigFailureMessage)) {
            sj.add(wifiConfigFailureMessage);
        }

        return sj.toString();
    }

    private static String getSuggestionOrSpecifierLabel(
            Context context, WifiConfiguration wifiConfiguration) {
        if (context == null || wifiConfiguration == null) {
            return "";
        }

        final String carrierName = getCarrierNameForSubId(context,
                getSubIdForConfig(context, wifiConfiguration));
        if (!TextUtils.isEmpty(carrierName)) {
            return carrierName;
        }
        final String suggestorLabel = getAppLabel(context, wifiConfiguration.creatorName);
        if (!TextUtils.isEmpty(suggestorLabel)) {
            return suggestorLabel;
        }
        // Fall-back to the package name in case the app label is missing
        return wifiConfiguration.creatorName;
    }

    private static String getWifiConfigurationFailureMessage(
            Context context, WifiConfiguration wifiConfiguration) {
        if (context == null || wifiConfiguration == null) {
            return "";
        }

        // Check for any failure messages to display
        if (wifiConfiguration.hasNoInternetAccess()) {
            int messageID =
                    wifiConfiguration.getNetworkSelectionStatus().getNetworkSelectionStatus()
                            == NETWORK_SELECTION_PERMANENTLY_DISABLED
                            ? R.string.wifitrackerlib_wifi_no_internet_no_reconnect
                            : R.string.wifitrackerlib_wifi_no_internet;
            return context.getString(messageID);
        } else if (wifiConfiguration.getNetworkSelectionStatus().getNetworkSelectionStatus()
                != NETWORK_SELECTION_ENABLED) {
            WifiConfiguration.NetworkSelectionStatus networkStatus =
                    wifiConfiguration.getNetworkSelectionStatus();
            switch (networkStatus.getNetworkSelectionDisableReason()) {
                case WifiConfiguration.NetworkSelectionStatus.DISABLED_AUTHENTICATION_FAILURE:
                case WifiConfiguration.NetworkSelectionStatus
                        .DISABLED_AUTHENTICATION_NO_SUBSCRIPTION:
                    return context.getString(
                            R.string.wifitrackerlib_wifi_disabled_password_failure);
                case WifiConfiguration.NetworkSelectionStatus.DISABLED_BY_WRONG_PASSWORD:
                    return context.getString(R.string.wifitrackerlib_wifi_check_password_try_again);
                case WifiConfiguration.NetworkSelectionStatus.DISABLED_DHCP_FAILURE:
                    return context.getString(R.string.wifitrackerlib_wifi_disabled_network_failure);
                case WifiConfiguration.NetworkSelectionStatus.DISABLED_ASSOCIATION_REJECTION:
                    return context.getString(R.string.wifitrackerlib_wifi_disabled_generic);
                case WifiConfiguration.NetworkSelectionStatus.DISABLED_NO_INTERNET_PERMANENT:
                case WifiConfiguration.NetworkSelectionStatus.DISABLED_NO_INTERNET_TEMPORARY:
                    return context.getString(R.string.wifitrackerlib_wifi_no_internet_no_reconnect);
                default:
                    break;
            }
        } else { // In range, not disabled.
            switch (wifiConfiguration.getRecentFailureReason()) {
                case WifiConfiguration.RECENT_FAILURE_AP_UNABLE_TO_HANDLE_NEW_STA:
                case WifiConfiguration.RECENT_FAILURE_REFUSED_TEMPORARILY:
                case WifiConfiguration.RECENT_FAILURE_DISCONNECTION_AP_BUSY:
                    return context.getString(R.string
                            .wifitrackerlib_wifi_ap_unable_to_handle_new_sta);
                case WifiConfiguration.RECENT_FAILURE_POOR_CHANNEL_CONDITIONS:
                    return context.getString(R.string.wifitrackerlib_wifi_poor_channel_conditions);
                case WifiConfiguration.RECENT_FAILURE_MBO_ASSOC_DISALLOWED_UNSPECIFIED:
                case WifiConfiguration.RECENT_FAILURE_MBO_ASSOC_DISALLOWED_AIR_INTERFACE_OVERLOADED:
                case WifiConfiguration.RECENT_FAILURE_MBO_ASSOC_DISALLOWED_AUTH_SERVER_OVERLOADED:
                    return context.getString(R.string
                            .wifitrackerlib_wifi_mbo_assoc_disallowed_cannot_connect);
                case WifiConfiguration.RECENT_FAILURE_MBO_ASSOC_DISALLOWED_MAX_NUM_STA_ASSOCIATED:
                    return context.getString(R.string
                            .wifitrackerlib_wifi_mbo_assoc_disallowed_max_num_sta_associated);
                case WifiConfiguration.RECENT_FAILURE_MBO_ASSOC_DISALLOWED_INSUFFICIENT_RSSI:
                case WifiConfiguration.RECENT_FAILURE_OCE_RSSI_BASED_ASSOCIATION_REJECTION:
                    return context.getString(R.string
                            .wifitrackerlib_wifi_mbo_oce_assoc_disallowed_insufficient_rssi);
                case WifiConfiguration.RECENT_FAILURE_NETWORK_NOT_FOUND:
                    return context.getString(R.string.wifitrackerlib_wifi_network_not_found);
                default:
                    // do nothing
            }
        }
        return "";
    }

    static String getAutoConnectDescription(@NonNull Context context,
            @NonNull WifiEntry wifiEntry) {
        if (context == null || wifiEntry == null || !wifiEntry.canSetAutoJoinEnabled()) {
            return "";
        }

        return wifiEntry.isAutoJoinEnabled()
                ? "" : context.getString(R.string.wifitrackerlib_auto_connect_disable);
    }

    static String getMeteredDescription(@NonNull Context context, @Nullable WifiEntry wifiEntry) {
        if (context == null || wifiEntry == null) {
            return "";
        }

        if (!wifiEntry.canSetMeteredChoice()
                && wifiEntry.getMeteredChoice() != WifiEntry.METERED_CHOICE_METERED) {
            return "";
        }

        if (wifiEntry.getMeteredChoice() == WifiEntry.METERED_CHOICE_METERED) {
            return context.getString(R.string.wifitrackerlib_wifi_metered_label);
        } else if (wifiEntry.getMeteredChoice() == WifiEntry.METERED_CHOICE_UNMETERED) {
            return context.getString(R.string.wifitrackerlib_wifi_unmetered_label);
        } else { // METERED_CHOICE_AUTO
            return wifiEntry.isMetered() ? context.getString(
                    R.string.wifitrackerlib_wifi_metered_label) : "";
        }
    }

    static String getSpeedDescription(@NonNull Context context, @NonNull WifiEntry wifiEntry) {
        if (context == null || wifiEntry == null) {
            return "";
        }

        @Speed int speed = wifiEntry.getSpeed();
        switch (speed) {
            case SPEED_VERY_FAST:
                return context.getString(R.string.wifitrackerlib_speed_label_very_fast);
            case SPEED_FAST:
                return context.getString(R.string.wifitrackerlib_speed_label_fast);
            case SPEED_MODERATE:
                return context.getString(R.string.wifitrackerlib_speed_label_okay);
            case SPEED_SLOW:
                return context.getString(R.string.wifitrackerlib_speed_label_slow);
            case SPEED_NONE:
            default:
                return "";
        }
    }

    static String getVerboseLoggingDescription(@NonNull WifiEntry wifiEntry) {
        if (!BaseWifiTracker.isVerboseLoggingEnabled() || wifiEntry == null) {
            return "";
        }

        final StringJoiner sj = new StringJoiner(" ");

        final String wifiInfoDescription = wifiEntry.getWifiInfoDescription();
        if (!TextUtils.isEmpty(wifiInfoDescription)) {
            sj.add(wifiInfoDescription);
        }

        final String networkCapabilityDescription = wifiEntry.getNetworkCapabilityDescription();
        if (!TextUtils.isEmpty(networkCapabilityDescription)) {
            sj.add(networkCapabilityDescription);
        }

        final String scanResultsDescription = wifiEntry.getScanResultDescription();
        if (!TextUtils.isEmpty(scanResultsDescription)) {
            sj.add(scanResultsDescription);
        }

        final String networkSelectionDescription = wifiEntry.getNetworkSelectionDescription();
        if (!TextUtils.isEmpty(networkSelectionDescription)) {
            sj.add(networkSelectionDescription);
        }

        return sj.toString();
    }

    static String getNetworkSelectionDescription(WifiConfiguration wifiConfig) {
        if (wifiConfig == null) {
            return "";
        }

        StringBuilder description = new StringBuilder();
        NetworkSelectionStatus networkSelectionStatus = wifiConfig.getNetworkSelectionStatus();

        if (networkSelectionStatus.getNetworkSelectionStatus() != NETWORK_SELECTION_ENABLED) {
            description.append(" (" + networkSelectionStatus.getNetworkStatusString());
            if (networkSelectionStatus.getDisableTime() > 0) {
                long now = System.currentTimeMillis();
                long elapsedSeconds = (now - networkSelectionStatus.getDisableTime()) / 1000;
                description.append(" " + DateUtils.formatElapsedTime(elapsedSeconds));
            }
            description.append(")");
        }

        int maxNetworkSelectionDisableReason =
                NetworkSelectionStatus.getMaxNetworkSelectionDisableReason();
        for (int reason = 0; reason <= maxNetworkSelectionDisableReason; reason++) {
            int disableReasonCounter = networkSelectionStatus.getDisableReasonCounter(reason);
            if (disableReasonCounter == 0) {
                continue;
            }
            description.append(" ")
                    .append(NetworkSelectionStatus.getNetworkSelectionDisableReasonString(reason))
                    .append("=")
                    .append(disableReasonCounter);
        }
        return description.toString();
    }

    static String getCurrentNetworkCapabilitiesInformation(Context context,
            NetworkCapabilities networkCapabilities) {
        if (context == null || networkCapabilities == null) {
            return "";
        }

        if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)) {
            return context.getString(context.getResources()
                    .getIdentifier("network_available_sign_in", "string", "android"));
        }

        if (networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY)) {
            return context.getString(R.string.wifitrackerlib_wifi_limited_connection);
        }

        if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            if (networkCapabilities.isPrivateDnsBroken()) {
                return context.getString(R.string.wifitrackerlib_private_dns_broken);
            }
            return context.getString(
                R.string.wifitrackerlib_wifi_connected_cannot_provide_internet);
        }
        return "";
    }

    /**
     * Returns the display string corresponding to the detailed state of the given NetworkInfo
     */
    static String getNetworkDetailedState(Context context, NetworkInfo networkInfo) {
        if (context == null || networkInfo == null) {
            return "";
        }
        DetailedState detailedState = networkInfo.getDetailedState();
        if (detailedState == null) {
            return "";
        }

        String[] wifiStatusArray = context.getResources()
                .getStringArray(R.array.wifitrackerlib_wifi_status);
        int index = detailedState.ordinal();
        return index >= wifiStatusArray.length ? "" : wifiStatusArray[index];
    }

    /**
     * Check if the SIM is present for target carrier Id.
     */
    static boolean isSimPresent(@NonNull Context context, int carrierId) {
        SubscriptionManager subscriptionManager =
                (SubscriptionManager) context.getSystemService(
                        Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subscriptionManager == null) return false;
        List<SubscriptionInfo> subInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList == null || subInfoList.isEmpty()) {
            return false;
        }
        return subInfoList.stream()
                .anyMatch(info -> info.getCarrierId() == carrierId);
    }

    /**
     * Get the SIM carrier name for target subscription Id.
     */
    static @Nullable String getCarrierNameForSubId(@NonNull Context context, int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return null;
        }
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) return null;
        TelephonyManager specifiedTm = telephonyManager.createForSubscriptionId(subId);
        if (specifiedTm == null) {
            return null;
        }
        CharSequence name = specifiedTm.getSimCarrierIdName();
        if (name == null) {
            return null;
        }
        return name.toString();
    }

    static boolean isSimCredential(@NonNull WifiConfiguration config) {
        return config.enterpriseConfig != null
                && config.enterpriseConfig.isAuthenticationSimBased();
    }

    /**
     * Get the best match subscription Id for target WifiConfiguration.
     */
    static int getSubIdForConfig(@NonNull Context context, @NonNull WifiConfiguration config) {
        if (config.carrierId == TelephonyManager.UNKNOWN_CARRIER_ID) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        SubscriptionManager subscriptionManager =
                (SubscriptionManager) context.getSystemService(
                        Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subscriptionManager == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        List<SubscriptionInfo> subInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList == null || subInfoList.isEmpty()) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        int matchSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        int dataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        for (SubscriptionInfo subInfo : subInfoList) {
            if (subInfo.getCarrierId() == config.carrierId) {
                matchSubId = subInfo.getSubscriptionId();
                if (matchSubId == dataSubId) {
                    // Priority of Data sub is higher than non data sub.
                    break;
                }
            }
        }
        return matchSubId;
    }

    /**
     * Check if target subscription Id requires IMSI privacy protection.
     */
    static boolean isImsiPrivacyProtectionProvided(@NonNull Context context, int subId) {
        CarrierConfigManager carrierConfigManager =
                (CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (carrierConfigManager == null) {
            return false;
        }
        PersistableBundle bundle = carrierConfigManager.getConfigForSubId(subId);
        if (bundle == null) {
            return false;
        }
        return (bundle.getInt(CarrierConfigManager.IMSI_KEY_AVAILABILITY_INT)
                & TelephonyManager.KEY_TYPE_WLAN) != 0;
    }

    static CharSequence getImsiProtectionDescription(Context context,
            @Nullable WifiConfiguration wifiConfig) {
        if (context == null || wifiConfig == null || !isSimCredential(wifiConfig)) {
            return "";
        }
        int subId;
        if (wifiConfig.carrierId == TelephonyManager.UNKNOWN_CARRIER_ID) {
            // Config without carrierId use default data subscription.
            subId = SubscriptionManager.getDefaultSubscriptionId();
        } else {
            subId = getSubIdForConfig(context, wifiConfig);
        }
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                || isImsiPrivacyProtectionProvided(context, subId)) {
            return "";
        }

        // IMSI protection is not provided, return warning message.
        return linkifyAnnotation(context, context.getText(
                R.string.wifitrackerlib_imsi_protection_warning), "url",
                context.getString(R.string.wifitrackerlib_help_url_imsi_protection));
    }

    /** Find the annotation of specified id in rawText and linkify it with helpUriString. */
    static CharSequence linkifyAnnotation(Context context, CharSequence rawText, String id,
            String helpUriString) {
        // Return original string when helpUriString is empty.
        if (TextUtils.isEmpty(helpUriString)) {
            return rawText;
        }

        SpannableString spannableText = new SpannableString(rawText);
        Annotation[] annotations = spannableText.getSpans(0, spannableText.length(),
                Annotation.class);

        for (Annotation annotation : annotations) {
            if (TextUtils.equals(annotation.getValue(), id)) {
                SpannableStringBuilder builder = new SpannableStringBuilder(spannableText);
                ClickableSpan link = new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        view.startActivityForResult(HelpUtils.getHelpIntent(context, helpUriString,
                                view.getClass().getName()), 0);
                    }
                };
                builder.setSpan(link, spannableText.getSpanStart(annotation),
                        spannableText.getSpanEnd(annotation), spannableText.getSpanFlags(link));
                return builder;
            }
        }
        return rawText;
    }

    // Various utility methods copied from com.android.server.wifi.util.ScanResultUtils for
    // extracting SecurityType from ScanResult.

    /**
     * Helper method to check if the provided |scanResult| corresponds to a PSK network or not.
     * This checks if the provided capabilities string contains PSK encryption type or not.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForPskNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("PSK");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a WAPI-PSK network or not.
     * This checks if the provided capabilities string contains PSK encryption type or not.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForWapiPskNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WAPI-PSK");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a WAPI-CERT
     * network or not.
     * This checks if the provided capabilities string contains PSK encryption type or not.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForWapiCertNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WAPI-CERT");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a EAP network or not.
     * This checks these conditions:
     * - Enable EAP/SHA1, EAP/SHA256 AKM, FT/EAP, or EAP-FILS.
     * - Not a WPA3 Enterprise only network.
     * - Not a WPA3 Enterprise transition network.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForEapNetwork(ScanResult scanResult) {
        return (scanResult.capabilities.contains("EAP/SHA1")
                || scanResult.capabilities.contains("EAP/SHA256")
                || scanResult.capabilities.contains("FT/EAP")
                || scanResult.capabilities.contains("EAP-FILS"))
                && !isScanResultForWpa3EnterpriseOnlyNetwork(scanResult)
                && !isScanResultForWpa3EnterpriseTransitionNetwork(scanResult);
    }

    // TODO(b/187755981): Move to shared static utils class
    private static boolean isScanResultForPmfMandatoryNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("[MFPR]");
    }

    // TODO(b/187755981): Move to shared static utils class
    private static boolean isScanResultForPmfCapableNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("[MFPC]");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to
     * a WPA3 Enterprise transition network or not.
     *
     * See Section 3.3 WPA3-Enterprise transition mode in WPA3 Specification
     * - Enable at least EAP/SHA1 and EAP/SHA256 AKM suites.
     * - Not enable WPA1 version 1, WEP, and TKIP.
     * - Management Frame Protection Capable is set.
     * - Management Frame Protection Required is not set.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForWpa3EnterpriseTransitionNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("EAP/SHA1")
                && scanResult.capabilities.contains("EAP/SHA256")
                && scanResult.capabilities.contains("RSN")
                && !scanResult.capabilities.contains("WEP")
                && !scanResult.capabilities.contains("TKIP")
                && !isScanResultForPmfMandatoryNetwork(scanResult)
                && isScanResultForPmfCapableNetwork(scanResult);
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to
     * a WPA3 Enterprise only network or not.
     *
     * See Section 3.2 WPA3-Enterprise only mode in WPA3 Specification
     * - Enable at least EAP/SHA256 AKM suite.
     * - Not enable EAP/SHA1 AKM suite.
     * - Not enable WPA1 version 1, WEP, and TKIP.
     * - Management Frame Protection Capable is set.
     * - Management Frame Protection Required is set.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForWpa3EnterpriseOnlyNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("EAP/SHA256")
                && !scanResult.capabilities.contains("EAP/SHA1")
                && scanResult.capabilities.contains("RSN")
                && !scanResult.capabilities.contains("WEP")
                && !scanResult.capabilities.contains("TKIP")
                && isScanResultForPmfMandatoryNetwork(scanResult)
                && isScanResultForPmfCapableNetwork(scanResult);
    }


    /**
     * Helper method to check if the provided |scanResult| corresponds to a WPA3-Enterprise 192-bit
     * mode network or not.
     * This checks if the provided capabilities comply these conditions:
     * - Enable SUITE-B-192 AKM.
     * - Not enable EAP/SHA1 AKM suite.
     * - Not enable WPA1 version 1, WEP, and TKIP.
     * - Management Frame Protection Required is set.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForEapSuiteBNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("SUITE_B_192")
                && scanResult.capabilities.contains("RSN")
                && !scanResult.capabilities.contains("WEP")
                && !scanResult.capabilities.contains("TKIP")
                && isScanResultForPmfMandatoryNetwork(scanResult);
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a WEP network or not.
     * This checks if the provided capabilities string contains WEP encryption type or not.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForWepNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WEP");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to OWE network.
     * This checks if the provided capabilities string contains OWE or not.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForOweNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("OWE");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to OWE transition network.
     * This checks if the provided capabilities string contains OWE_TRANSITION or not.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForOweTransitionNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("OWE_TRANSITION");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to SAE network.
     * This checks if the provided capabilities string contains SAE or not.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForSaeNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("SAE");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to PSK-SAE transition
     * network. This checks if the provided capabilities string contains both PSK and SAE or not.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForPskSaeTransitionNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("PSK") && scanResult.capabilities.contains("SAE");
    }

    /**
     *  Helper method to check if the provided |scanResult| corresponds to an unknown amk network.
     *  This checks if the provided capabilities string contains ? or not.
     *  TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForUnknownAkmNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("?");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to an open network or not.
     * This checks if the provided capabilities string does not contain either of WEP, PSK, SAE
     * EAP, or unknown encryption types or not.
     * TODO(b/187755981): Move to shared static utils class
     */
    public static boolean isScanResultForOpenNetwork(ScanResult scanResult) {
        return (!(isScanResultForWepNetwork(scanResult) || isScanResultForPskNetwork(scanResult)
                || isScanResultForEapNetwork(scanResult) || isScanResultForSaeNetwork(scanResult)
                || isScanResultForWpa3EnterpriseTransitionNetwork(scanResult)
                || isScanResultForWpa3EnterpriseOnlyNetwork(scanResult)
                || isScanResultForWapiPskNetwork(scanResult)
                || isScanResultForWapiCertNetwork(scanResult)
                || isScanResultForEapSuiteBNetwork(scanResult)
                || isScanResultForUnknownAkmNetwork(scanResult)));
    }
}

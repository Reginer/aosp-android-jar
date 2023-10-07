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
import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.NETWORK_SELECTION_ENABLED;
import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.NETWORK_SELECTION_PERMANENTLY_DISABLED;

import static java.util.Comparator.comparingInt;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityDiagnosticsManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.TransportInfo;
import android.net.wifi.MloLink;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.os.Build;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.os.BuildCompat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Utility methods for WifiTrackerLib.
 */
public class Utils {
    // TODO(b/242144920): remove this after publishing this reason in U.
    // This reason is added in U and hidden in T, using a hard-coded value first.
    public static final int DISABLED_TRANSITION_DISABLE_INDICATION = 13;

    // Returns the ScanResult with the best RSSI from a list of ScanResults.
    @Nullable
    public static ScanResult getBestScanResultByLevel(@NonNull List<ScanResult> scanResults) {
        if (scanResults.isEmpty()) return null;

        return Collections.max(scanResults, comparingInt(scanResult -> scanResult.level));
    }

    // Returns a list of WifiInfo SECURITY_TYPE_* supported by a ScanResult.
    @NonNull
    public static List<Integer> getSecurityTypesFromScanResult(@NonNull ScanResult scanResult) {
        List<Integer> securityTypes = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (int securityType : scanResult.getSecurityTypes()) {
                securityTypes.add(securityType);
            }
            return securityTypes;
        }

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
        if (securityTypes.size() == 0) {
            return WifiInfo.SECURITY_TYPE_UNKNOWN;
        }
        if (securityTypes.size() == 1) {
            return securityTypes.get(0);
        }
        if (securityTypes.size() == 2) {
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
        // Default to the first security type if we don't need any special mapping.
        return securityTypes.get(0);
    }

    /**
     * Get the app label for a suggestion/specifier package name, or an empty String if none exist
     */
    static String getAppLabel(Context context, String packageName) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    packageName,
                    0 /* flags */);
            return appInfo.loadLabel(context.getPackageManager()).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    static String getConnectedDescription(@NonNull Context context,
            @Nullable WifiConfiguration wifiConfiguration,
            @Nullable NetworkCapabilities networkCapabilities,
            boolean isDefaultNetwork,
            boolean isLowQuality,
            @Nullable ConnectivityDiagnosticsManager.ConnectivityReport connectivityReport) {
        final StringJoiner sj = new StringJoiner(context.getString(
                R.string.wifitrackerlib_summary_separator));

        boolean shouldShowConnected = isDefaultNetwork;
        if (wifiConfiguration != null
                && (wifiConfiguration.fromWifiNetworkSuggestion
                || wifiConfiguration.fromWifiNetworkSpecifier)) {
            // For suggestion or specifier networks to show "Connected via ..."
            final String suggestionOrSpecifierLabel =
                    getSuggestionOrSpecifierLabel(context, wifiConfiguration);
            if (!TextUtils.isEmpty(suggestionOrSpecifierLabel)) {
                if (isDefaultNetwork || (networkCapabilities != null
                        && NonSdkApiWrapper.isOemCapabilities(networkCapabilities))) {
                    sj.add(context.getString(R.string.wifitrackerlib_connected_via_app,
                            suggestionOrSpecifierLabel));
                } else {
                    // Pretend that non-default, non-OEM networks are unconnected.
                    sj.add(context.getString(R.string.wifitrackerlib_available_via_app,
                            suggestionOrSpecifierLabel));
                }
                shouldShowConnected = false;
            }
        }

        if (isLowQuality) {
            sj.add(context.getString(R.string.wifi_connected_low_quality));
            shouldShowConnected = false;
        }

        // For displaying network capability info, such as captive portal or no internet
        if (networkCapabilities != null) {
            if (networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)) {
                // "Sign in to network"
                sj.add(context.getString(context.getResources()
                        .getIdentifier("network_available_sign_in", "string", "android")));
                shouldShowConnected = false;
            } else if (networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY)) {
                // "Limited connection..."
                sj.add(context.getString(
                        R.string.wifitrackerlib_wifi_limited_connection));
                shouldShowConnected = false;
            } else if (!networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                boolean noInternetExpected = wifiConfiguration != null
                        && wifiConfiguration.isNoInternetAccessExpected();
                if (connectivityReport == null && !noInternetExpected) {
                    // "Checking for internet access..."
                    sj.add(context.getString(R.string.wifitrackerlib_checking_for_internet_access));
                    shouldShowConnected = false;
                } else if (networkCapabilities.isPrivateDnsBroken()) {
                    // "Private DNS server cannot be accessed"
                    sj.add(context.getString(R.string.wifitrackerlib_private_dns_broken));
                    shouldShowConnected = false;
                } else if (noInternetExpected) {
                    // "Connected to device. Can't provide internet."
                    sj.add(context.getString(
                            R.string.wifitrackerlib_wifi_connected_cannot_provide_internet));
                    shouldShowConnected = false;
                } else {
                    // "No internet access"
                    sj.add(context.getString(R.string.wifitrackerlib_wifi_no_internet));
                }
            }
        }

        // Show "Connected" first if we haven't hidden it due to other strings.
        if (shouldShowConnected) {
            return new StringJoiner(context.getString(R.string.wifitrackerlib_summary_separator))
                    .add(context.getResources().getStringArray(R.array.wifitrackerlib_wifi_status)
                            [DetailedState.CONNECTED.ordinal()]).merge(sj).toString();
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


    static String getDisconnectedDescription(
            @NonNull WifiTrackerInjector injector,
            Context context,
            WifiConfiguration wifiConfiguration,
            boolean forSavedNetworksPage,
            boolean concise) {
        if (context == null || wifiConfiguration == null) {
            return "";
        }
        final StringJoiner sj = new StringJoiner(context.getString(
                R.string.wifitrackerlib_summary_separator));

        // For "Saved", "Saved by ...", and "Available via..."
        if (concise) {
            sj.add(context.getString(R.string.wifitrackerlib_wifi_disconnected));
        } else if (forSavedNetworksPage && !wifiConfiguration.isPasspoint()) {
            if (!injector.getNoAttributionAnnotationPackages().contains(
                    wifiConfiguration.creatorName)) {
                final CharSequence appLabel = getAppLabel(context,
                        wifiConfiguration.creatorName);
                if (!TextUtils.isEmpty(appLabel)) {
                    sj.add(context.getString(R.string.wifitrackerlib_saved_network, appLabel));
                }
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
        NetworkSelectionStatus networkSelectionStatus =
                wifiConfiguration.getNetworkSelectionStatus();
        if (wifiConfiguration.hasNoInternetAccess()) {
            if (networkSelectionStatus.getNetworkSelectionStatus()
                    == NETWORK_SELECTION_PERMANENTLY_DISABLED) {
                return context.getString(R.string.wifitrackerlib_wifi_no_internet_no_reconnect);
            }
            return context.getString(R.string.wifitrackerlib_wifi_no_internet);
        }
        if (networkSelectionStatus.getNetworkSelectionStatus() != NETWORK_SELECTION_ENABLED) {
            switch (networkSelectionStatus.getNetworkSelectionDisableReason()) {
                case NetworkSelectionStatus.DISABLED_CONSECUTIVE_FAILURES:
                    if (!networkSelectionStatus.hasEverConnected()
                            && networkSelectionStatus.getDisableReasonCounter(
                                    NetworkSelectionStatus.DISABLED_AUTHENTICATION_FAILURE) > 0) {
                        return context.getString(
                                R.string.wifitrackerlib_wifi_disabled_password_failure);
                    }
                    break;
                case NetworkSelectionStatus.DISABLED_AUTHENTICATION_FAILURE:
                case NetworkSelectionStatus.DISABLED_AUTHENTICATION_NO_CREDENTIALS:
                case NetworkSelectionStatus.DISABLED_AUTHENTICATION_NO_SUBSCRIPTION:
                    return context.getString(
                            R.string.wifitrackerlib_wifi_disabled_password_failure);
                case NetworkSelectionStatus.DISABLED_BY_WRONG_PASSWORD:
                    return context.getString(R.string.wifitrackerlib_wifi_check_password_try_again);
                case NetworkSelectionStatus.DISABLED_DHCP_FAILURE:
                    return context.getString(R.string.wifitrackerlib_wifi_disabled_network_failure);
                case NetworkSelectionStatus.DISABLED_ASSOCIATION_REJECTION:
                    return context.getString(R.string.wifitrackerlib_wifi_disabled_generic);
                case NetworkSelectionStatus.DISABLED_NO_INTERNET_PERMANENT:
                case NetworkSelectionStatus.DISABLED_NO_INTERNET_TEMPORARY:
                    return context.getString(R.string.wifitrackerlib_wifi_no_internet_no_reconnect);
                case DISABLED_TRANSITION_DISABLE_INDICATION:
                    return context.getString(
                            R.string.wifitrackerlib_wifi_disabled_transition_disable_indication);
                default:
                    break;
            }
        } else { // NETWORK_SELECTION_ENABLED
            // Show failure message if we've gotten AUTHENTICATION_FAILURE and have never connected
            // before, which usually indicates the credentials are wrong.
            if (networkSelectionStatus.getDisableReasonCounter(DISABLED_AUTHENTICATION_FAILURE) > 0
                    && !networkSelectionStatus.hasEverConnected()) {
                return context.getString(R.string.wifitrackerlib_wifi_disabled_password_failure);
            }
        }
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
     * Check if the SIM is present for target carrier Id. If the carrierId is
     * {@link TelephonyManager#UNKNOWN_CARRIER_ID}, then this returns true if there is any SIM
     * present.
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
        if (carrierId == TelephonyManager.UNKNOWN_CARRIER_ID) {
            // Return true if any SIM is present for UNKNOWN_CARRIER_ID since the framework will
            // match this to the default data SIM.
            return true;
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

    static boolean isServerCertUsedNetwork(@NonNull WifiConfiguration config) {
        return config.enterpriseConfig != null && config.enterpriseConfig
                .isEapMethodServerCertUsed();
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
        if (context == null || wifiConfig == null || !isSimCredential(wifiConfig)
                || isServerCertUsedNetwork(wifiConfig)) {
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
        return NonSdkApiWrapper.linkifyAnnotation(context, context.getText(
                R.string.wifitrackerlib_imsi_protection_warning), "url",
                context.getString(R.string.wifitrackerlib_help_url_imsi_protection));
    }

    // Various utility methods copied from com.android.server.wifi.util.ScanResultUtils for
    // extracting SecurityType from ScanResult.

    /**
     * Helper method to check if the provided |scanResult| corresponds to a PSK network or not.
     * This checks if the provided capabilities string contains PSK encryption type or not.
     */
    private static boolean isScanResultForPskNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("PSK");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a WAPI-PSK network or not.
     * This checks if the provided capabilities string contains PSK encryption type or not.
     */
    private static boolean isScanResultForWapiPskNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WAPI-PSK");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a WAPI-CERT
     * network or not.
     * This checks if the provided capabilities string contains PSK encryption type or not.
     */
    private static boolean isScanResultForWapiCertNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WAPI-CERT");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a EAP network or not.
     * This checks these conditions:
     * - Enable EAP/SHA1, EAP/SHA256 AKM, FT/EAP, or EAP-FILS.
     * - Not a WPA3 Enterprise only network.
     * - Not a WPA3 Enterprise transition network.
     */
    private static boolean isScanResultForEapNetwork(ScanResult scanResult) {
        return (scanResult.capabilities.contains("EAP/SHA1")
                || scanResult.capabilities.contains("EAP/SHA256")
                || scanResult.capabilities.contains("FT/EAP")
                || scanResult.capabilities.contains("EAP-FILS"))
                && !isScanResultForWpa3EnterpriseOnlyNetwork(scanResult)
                && !isScanResultForWpa3EnterpriseTransitionNetwork(scanResult);
    }

    private static boolean isScanResultForPmfMandatoryNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("[MFPR]");
    }

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
     */
    private static boolean isScanResultForWpa3EnterpriseTransitionNetwork(ScanResult scanResult) {
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
     */
    private static boolean isScanResultForWpa3EnterpriseOnlyNetwork(ScanResult scanResult) {
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
     */
    private static boolean isScanResultForEapSuiteBNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("SUITE_B_192")
                && scanResult.capabilities.contains("RSN")
                && !scanResult.capabilities.contains("WEP")
                && !scanResult.capabilities.contains("TKIP")
                && isScanResultForPmfMandatoryNetwork(scanResult);
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a WEP network or not.
     * This checks if the provided capabilities string contains WEP encryption type or not.
     */
    private static boolean isScanResultForWepNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WEP");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to OWE network.
     * This checks if the provided capabilities string contains OWE or not.
     */
    private static boolean isScanResultForOweNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("OWE");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to OWE transition network.
     * This checks if the provided capabilities string contains OWE_TRANSITION or not.
     */
    private static boolean isScanResultForOweTransitionNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("OWE_TRANSITION");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to SAE network.
     * This checks if the provided capabilities string contains SAE or not.
     */
    private static boolean isScanResultForSaeNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("SAE");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to PSK-SAE transition
     * network. This checks if the provided capabilities string contains both PSK and SAE or not.
     */
    private static boolean isScanResultForPskSaeTransitionNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("PSK") && scanResult.capabilities.contains("SAE");
    }

    /**
     *  Helper method to check if the provided |scanResult| corresponds to an unknown amk network.
     *  This checks if the provided capabilities string contains ? or not.
     */
    private static boolean isScanResultForUnknownAkmNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("?");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to an open network or not.
     * This checks if the provided capabilities string does not contain either of WEP, PSK, SAE
     * EAP, or unknown encryption types or not.
     */
    private static boolean isScanResultForOpenNetwork(ScanResult scanResult) {
        return (!(isScanResultForWepNetwork(scanResult) || isScanResultForPskNetwork(scanResult)
                || isScanResultForEapNetwork(scanResult) || isScanResultForSaeNetwork(scanResult)
                || isScanResultForWpa3EnterpriseTransitionNetwork(scanResult)
                || isScanResultForWpa3EnterpriseOnlyNetwork(scanResult)
                || isScanResultForWapiPskNetwork(scanResult)
                || isScanResultForWapiCertNetwork(scanResult)
                || isScanResultForEapSuiteBNetwork(scanResult)
                || isScanResultForUnknownAkmNetwork(scanResult)));
    }

    /**
     * Get InetAddress masked with prefixLength.  Will never return null.
     * @param address the IP address to mask with
     * @param prefixLength the prefixLength used to mask the IP
     */
    public static InetAddress getNetworkPart(InetAddress address, int prefixLength) {
        byte[] array = address.getAddress();
        maskRawAddress(array, prefixLength);

        InetAddress netPart = null;
        try {
            netPart = InetAddress.getByAddress(array);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("getNetworkPart error - " + e.toString());
        }
        return netPart;
    }

    /**
     *  Masks a raw IP address byte array with the specified prefix length.
     */
    public static void maskRawAddress(byte[] array, int prefixLength) {
        if (prefixLength < 0 || prefixLength > array.length * 8) {
            throw new IllegalArgumentException("IP address with " + array.length
                    + " bytes has invalid prefix length " + prefixLength);
        }

        int offset = prefixLength / 8;
        int remainder = prefixLength % 8;
        byte mask = (byte) (0xFF << (8 - remainder));

        if (offset < array.length) {
            array[offset] = (byte) (array[offset] & mask);
        }

        offset++;

        for (; offset < array.length; offset++) {
            array[offset] = 0;
        }
    }

    @Nullable
    private static Context createPackageContextAsUser(int uid, Context context) {
        Context userContext = null;
        try {
            userContext = context.createPackageContextAsUser(context.getPackageName(), 0,
                    UserHandle.getUserHandleForUid(uid));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        return userContext;
    }

    @Nullable
    private static DevicePolicyManager retrieveDevicePolicyManagerFromUserContext(int uid,
            Context context) {
        Context userContext = createPackageContextAsUser(uid, context);
        if (userContext == null) return null;
        return userContext.getSystemService(DevicePolicyManager.class);
    }

    @Nullable
    private static Pair<UserHandle, ComponentName> getDeviceOwner(Context context) {
        DevicePolicyManager devicePolicyManager =
                context.getSystemService(DevicePolicyManager.class);
        if (devicePolicyManager == null) return null;
        UserHandle deviceOwnerUser = null;
        ComponentName deviceOwnerComponent = null;
        try {
            deviceOwnerUser = devicePolicyManager.getDeviceOwnerUser();
            deviceOwnerComponent = devicePolicyManager.getDeviceOwnerComponentOnAnyUser();
        } catch (Exception e) {
            throw new RuntimeException("getDeviceOwner error - " + e.toString());
        }
        if (deviceOwnerUser == null || deviceOwnerComponent == null) return null;

        if (deviceOwnerComponent.getPackageName() == null) {
            // shouldn't happen
            return null;
        }
        return new Pair<>(deviceOwnerUser, deviceOwnerComponent);
    }

    /**
     * Returns true if the |callingUid|/|callingPackage| is the device owner.
     */
    public static boolean isDeviceOwner(int uid, @Nullable String packageName, Context context) {
        // Cannot determine if the app is DO/PO if packageName is null. So, will return false to be
        // safe.
        if (packageName == null) {
            return false;
        }
        Pair<UserHandle, ComponentName> deviceOwner = getDeviceOwner(context);

        // no device owner
        if (deviceOwner == null) return false;

        return deviceOwner.first.equals(UserHandle.getUserHandleForUid(uid))
                && deviceOwner.second.getPackageName().equals(packageName);
    }

    /**
     * Returns true if the |callingUid|/|callingPackage| is the profile owner.
     */
    public static boolean isProfileOwner(int uid, @Nullable String packageName, Context context) {
        // Cannot determine if the app is DO/PO if packageName is null. So, will return false to be
        // safe.
        if (packageName == null) {
            return false;
        }
        DevicePolicyManager devicePolicyManager =
                retrieveDevicePolicyManagerFromUserContext(uid, context);
        if (devicePolicyManager == null) return false;
        return devicePolicyManager.isProfileOwnerApp(packageName);
    }

    /**
     * Returns true if the |callingUid|/|callingPackage| is the device or profile owner.
     */
    public static boolean isDeviceOrProfileOwner(int uid, String packageName, Context context) {
        return isDeviceOwner(uid, packageName, context)
                || isProfileOwner(uid, packageName, context);
    }

    /**
     * Unknown security type that cannot be converted to
     * DevicePolicyManager.WifiSecurity security type.
     */
    public static final int DPM_SECURITY_TYPE_UNKNOWN = -1;

    /**
     * Utility method to convert WifiInfo.SecurityType to DevicePolicyManager.WifiSecurity
     * @param securityType WifiInfo.SecurityType to convert
     * @return DevicePolicyManager.WifiSecurity security level, or
     * {@link WifiInfo#DPM_SECURITY_TYPE_UNKNOWN} for unknown security types
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public static int convertSecurityTypeToDpmWifiSecurity(int securityType) {
        switch (securityType) {
            case WifiInfo.SECURITY_TYPE_OPEN:
            case WifiInfo.SECURITY_TYPE_OWE:
                return DevicePolicyManager.WIFI_SECURITY_OPEN;
            case WifiInfo.SECURITY_TYPE_WEP:
            case WifiInfo.SECURITY_TYPE_PSK:
            case WifiInfo.SECURITY_TYPE_SAE:
            case WifiInfo.SECURITY_TYPE_WAPI_PSK:
                return DevicePolicyManager.WIFI_SECURITY_PERSONAL;
            case WifiInfo.SECURITY_TYPE_EAP:
            case WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE:
            case WifiInfo.SECURITY_TYPE_PASSPOINT_R1_R2:
            case WifiInfo.SECURITY_TYPE_PASSPOINT_R3:
            case WifiInfo.SECURITY_TYPE_WAPI_CERT:
                return DevicePolicyManager.WIFI_SECURITY_ENTERPRISE_EAP;
            case WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT:
                return DevicePolicyManager.WIFI_SECURITY_ENTERPRISE_192;
            default:
                return DPM_SECURITY_TYPE_UNKNOWN;
        }
    }

    /**
     * Converts a ScanResult.WIFI_STANDARD_ value to a display string.
     */
    public static String getStandardString(@NonNull Context context, int standard) {
        switch (standard) {
            case ScanResult.WIFI_STANDARD_LEGACY:
                return context.getString(R.string.wifitrackerlib_wifi_standard_legacy);
            case ScanResult.WIFI_STANDARD_11N:
                return context.getString(R.string.wifitrackerlib_wifi_standard_11n);
            case ScanResult.WIFI_STANDARD_11AC:
                return context.getString(R.string.wifitrackerlib_wifi_standard_11ac);
            case ScanResult.WIFI_STANDARD_11AX:
                return context.getString(R.string.wifitrackerlib_wifi_standard_11ax);
            case ScanResult.WIFI_STANDARD_11AD:
                return context.getString(R.string.wifitrackerlib_wifi_standard_11ad);
            case ScanResult.WIFI_STANDARD_11BE:
                return context.getString(R.string.wifitrackerlib_wifi_standard_11be);
            default:
                return context.getString(R.string.wifitrackerlib_wifi_standard_unknown);
        }
    }

    /**
     * Converts a frequency in MHz to the display string of the corresponding Wi-Fi band.
     */
    public static String getBandString(@NonNull Context context, int freqMhz) {
        if (freqMhz >= WifiEntry.MIN_FREQ_24GHZ && freqMhz < WifiEntry.MAX_FREQ_24GHZ) {
            return context.getResources().getString(R.string.wifitrackerlib_wifi_band_24_ghz);
        } else if (freqMhz >= WifiEntry.MIN_FREQ_5GHZ && freqMhz < WifiEntry.MAX_FREQ_5GHZ) {
            return context.getResources().getString(R.string.wifitrackerlib_wifi_band_5_ghz);
        } else if (freqMhz >= WifiEntry.MIN_FREQ_6GHZ && freqMhz < WifiEntry.MAX_FREQ_6GHZ) {
            return context.getResources().getString(R.string.wifitrackerlib_wifi_band_6_ghz);
        } else {
            return context.getResources().getString(R.string.wifitrackerlib_wifi_band_unknown);
        }
    }

    /**
     * Converts the band info in WifiInfo to the display string of the corresponding Wi-Fi band(s).
     */
    public static String getBandString(@NonNull Context context, @NonNull WifiInfo wifiInfo) {
        if (!BuildCompat.isAtLeastU()) {
            return getBandString(context, wifiInfo.getFrequency());
        }

        StringJoiner sj = new StringJoiner(
                context.getResources().getString(R.string.wifitrackerlib_multiband_separator));
        wifiInfo.getAssociatedMloLinks().stream()
                .filter((link) -> link.getState() == MloLink.MLO_LINK_STATE_ACTIVE)
                .map(MloLink::getBand)
                .distinct()
                .sorted()
                .forEach((band) -> {
                    switch (band) {
                        case WifiScanner.WIFI_BAND_24_GHZ:
                            sj.add(context.getResources()
                                    .getString(R.string.wifitrackerlib_wifi_band_24_ghz));
                            break;
                        case WifiScanner.WIFI_BAND_5_GHZ:
                            sj.add(context.getResources()
                                    .getString(R.string.wifitrackerlib_wifi_band_5_ghz));
                            break;
                        case WifiScanner.WIFI_BAND_6_GHZ:
                            sj.add(context.getResources()
                                    .getString(R.string.wifitrackerlib_wifi_band_6_ghz));
                            break;
                        default:
                            sj.add(context.getResources()
                                    .getString(R.string.wifitrackerlib_wifi_band_unknown));
                    }
                });
        if (sj.length() == 0) {
            return getBandString(context, wifiInfo.getFrequency());
        }
        return sj.toString();
    }

    /**
     * Gets the WifiInfo from a NetworkCapabilities if there is one.
     */
    public static WifiInfo getWifiInfo(@NonNull NetworkCapabilities capabilities) {
        TransportInfo transportInfo = capabilities.getTransportInfo();
        if (transportInfo instanceof WifiInfo) {
            return (WifiInfo) transportInfo;
        }
        return NonSdkApiWrapper.getVcnWifiInfo(capabilities);
    }
}

/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.internal.telephony.data;

import android.annotation.CurrentTimeMillisLong;
import android.annotation.ElapsedRealtimeLong;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.os.SystemClock;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.AccessNetworkConstants.RadioAccessNetworkType;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.ConnectivityTransport;
import android.telephony.Annotation.DataActivityType;
import android.telephony.Annotation.NetCapability;
import android.telephony.Annotation.NetworkType;
import android.telephony.Annotation.ValidationStatus;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.telephony.data.ApnSetting.ApnType;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataCallResponse.LinkStatus;
import android.telephony.ims.feature.ImsFeature;
import android.util.ArrayMap;

import com.android.internal.telephony.data.DataNetworkController.NetworkRequestList;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.telephony.Rlog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class contains all the utility methods used by telephony data stack.
 */
public class DataUtils {
    public static final int NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED = 37;
    /** The time format for converting time to readable string. */
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static final String TAG = "DataUtils";

    /**
     * Get the network capability from the string.
     *
     * @param capabilityString The capability in string format
     * @return The network capability. -1 if not found.
     */
    @NetCapability
    public static int getNetworkCapabilityFromString(@NonNull String capabilityString) {
        return switch (capabilityString.toUpperCase(Locale.ROOT)) {
            case "MMS" -> NetworkCapabilities.NET_CAPABILITY_MMS;
            case "SUPL" -> NetworkCapabilities.NET_CAPABILITY_SUPL;
            case "DUN" -> NetworkCapabilities.NET_CAPABILITY_DUN;
            case "FOTA" -> NetworkCapabilities.NET_CAPABILITY_FOTA;
            case "IMS" -> NetworkCapabilities.NET_CAPABILITY_IMS;
            case "CBS" -> NetworkCapabilities.NET_CAPABILITY_CBS;
            case "XCAP" -> NetworkCapabilities.NET_CAPABILITY_XCAP;
            case "EIMS" -> NetworkCapabilities.NET_CAPABILITY_EIMS;
            case "INTERNET" -> NetworkCapabilities.NET_CAPABILITY_INTERNET;
            case "MCX" -> NetworkCapabilities.NET_CAPABILITY_MCX;
            case "VSIM" -> NetworkCapabilities.NET_CAPABILITY_VSIM;
            case "BIP" -> NetworkCapabilities.NET_CAPABILITY_BIP;
            case "ENTERPRISE" -> NetworkCapabilities.NET_CAPABILITY_ENTERPRISE;
            case "PRIORITIZE_BANDWIDTH" -> NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_BANDWIDTH;
            case "PRIORITIZE_LATENCY" -> NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_LATENCY;
            case "RCS" -> NetworkCapabilities.NET_CAPABILITY_RCS;
            default -> {
                loge("Illegal network capability: " + capabilityString);
                yield -1;
            }
        };
    }

    /**
     * Get Set of network capabilities from string joined by {@code |}, space is ignored.
     * If input string contains unknown capability or malformatted(e.g. empty string), -1 is
     * included in the returned set.
     *
     * @param capabilitiesString capability strings joined by {@code |}
     * @return Set of capabilities
     */
    @NetCapability
    public static Set<Integer> getNetworkCapabilitiesFromString(
            @NonNull String capabilitiesString) {
        // e.g. "IMS|" is not allowed
        if (!capabilitiesString.matches("(\\s*[a-zA-Z_]+\\s*)(\\|\\s*[a-zA-Z_]+\\s*)*")) {
            return Collections.singleton(-1);
        }
        return Arrays.stream(capabilitiesString.split("\\s*\\|\\s*"))
                .map(String::trim)
                .map(DataUtils::getNetworkCapabilityFromString)
                .collect(Collectors.toSet());
    }

    /**
     * Convert a network capability to string.
     * <p>
     * This is for debugging and logging purposes only.
     *
     * @param netCap Network capability.
     * @return Network capability in string format.
     */
    @NonNull
    public static String networkCapabilityToString(@NetCapability int netCap) {
        return switch (netCap) {
            case NetworkCapabilities.NET_CAPABILITY_MMS -> "MMS";
            case NetworkCapabilities.NET_CAPABILITY_SUPL -> "SUPL";
            case NetworkCapabilities.NET_CAPABILITY_DUN -> "DUN";
            case NetworkCapabilities.NET_CAPABILITY_FOTA -> "FOTA";
            case NetworkCapabilities.NET_CAPABILITY_IMS -> "IMS";
            case NetworkCapabilities.NET_CAPABILITY_CBS -> "CBS";
            case NetworkCapabilities.NET_CAPABILITY_WIFI_P2P -> "WIFI_P2P";
            case NetworkCapabilities.NET_CAPABILITY_IA -> "IA";
            case NetworkCapabilities.NET_CAPABILITY_RCS -> "RCS";
            case NetworkCapabilities.NET_CAPABILITY_XCAP -> "XCAP";
            case NetworkCapabilities.NET_CAPABILITY_EIMS -> "EIMS";
            case NetworkCapabilities.NET_CAPABILITY_NOT_METERED -> "NOT_METERED";
            case NetworkCapabilities.NET_CAPABILITY_INTERNET -> "INTERNET";
            case NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED -> "NOT_RESTRICTED";
            case NetworkCapabilities.NET_CAPABILITY_TRUSTED -> "TRUSTED";
            case NetworkCapabilities.NET_CAPABILITY_NOT_VPN -> "NOT_VPN";
            case NetworkCapabilities.NET_CAPABILITY_VALIDATED -> "VALIDATED";
            case NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL -> "CAPTIVE_PORTAL";
            case NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING -> "NOT_ROAMING";
            case NetworkCapabilities.NET_CAPABILITY_FOREGROUND -> "FOREGROUND";
            case NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED -> "NOT_CONGESTED";
            case NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED -> "NOT_SUSPENDED";
            case NetworkCapabilities.NET_CAPABILITY_OEM_PAID -> "OEM_PAID";
            case NetworkCapabilities.NET_CAPABILITY_MCX -> "MCX";
            case NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY -> "PARTIAL_CONNECTIVITY";
            case NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED ->
                    "TEMPORARILY_NOT_METERED";
            case NetworkCapabilities.NET_CAPABILITY_OEM_PRIVATE -> "OEM_PRIVATE";
            case NetworkCapabilities.NET_CAPABILITY_VEHICLE_INTERNAL -> "VEHICLE_INTERNAL";
            case NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED -> "NOT_VCN_MANAGED";
            case NetworkCapabilities.NET_CAPABILITY_ENTERPRISE -> "ENTERPRISE";
            case NetworkCapabilities.NET_CAPABILITY_VSIM -> "VSIM";
            case NetworkCapabilities.NET_CAPABILITY_BIP -> "BIP";
            case NetworkCapabilities.NET_CAPABILITY_HEAD_UNIT -> "HEAD_UNIT";
            case NetworkCapabilities.NET_CAPABILITY_MMTEL -> "MMTEL";
            case NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_LATENCY -> "PRIORITIZE_LATENCY";
            case NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_BANDWIDTH -> "PRIORITIZE_BANDWIDTH";
            case NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED -> "NOT_BANDWIDTH_CONSTRAINED";
            default -> {
                loge("Unknown network capability(" + netCap + ")");
                yield "Unknown(" + netCap + ")";
            }
        };
    }

    /**
     * Concat an array of {@link NetworkCapabilities.Transport} in string format.
     *
     * @param transports an array of connectivity transports
     * @return a string of the array of transports.
     */
    @NonNull
    public static String connectivityTransportsToString(
            @NonNull @ConnectivityTransport int[] transports) {
        return Arrays.stream(transports).mapToObj(DataUtils::connectivityTransportToString)
                .collect(Collectors.joining("|"));
    }

    /**
     * Convert a {@link NetworkCapabilities.Transport} to a string.
     *
     * @param transport the connectivity transport
     * @return the transport in string
     */
    @NonNull
    public static String connectivityTransportToString(
            @ConnectivityTransport int transport) {
        return switch (transport) {
            case NetworkCapabilities.TRANSPORT_CELLULAR -> "CELLULAR";
            case NetworkCapabilities.TRANSPORT_WIFI -> "WIFI";
            case NetworkCapabilities.TRANSPORT_BLUETOOTH -> "BLUETOOTH";
            case NetworkCapabilities.TRANSPORT_ETHERNET -> "ETHERNET";
            case NetworkCapabilities.TRANSPORT_VPN -> "VPN";
            case NetworkCapabilities.TRANSPORT_WIFI_AWARE -> "WIFI_AWARE";
            case NetworkCapabilities.TRANSPORT_LOWPAN -> "LOWPAN";
            case NetworkCapabilities.TRANSPORT_TEST -> "TEST";
            case NetworkCapabilities.TRANSPORT_USB -> "USB";
            case NetworkCapabilities.TRANSPORT_THREAD -> "THREAD";
            case NetworkCapabilities.TRANSPORT_SATELLITE -> "SATELLITE";
            default -> "Unknown(" + transport + ")";
        };
    }

    /**
     * Convert network capabilities to string.
     * <p>
     * This is for debugging and logging purposes only.
     *
     * @param netCaps Network capabilities.
     * @return Network capabilities in string format.
     */
    @NonNull
    public static String networkCapabilitiesToString(
            @NetCapability @Nullable Collection<Integer> netCaps) {
        if (netCaps == null || netCaps.isEmpty()) return "";
        return "[" + netCaps.stream()
                .map(DataUtils::networkCapabilityToString)
                .collect(Collectors.joining("|")) + "]";
    }

    /**
     * Convert network capabilities to string.
     * <p>
     * This is for debugging and logging purposes only.
     *
     * @param netCaps Network capabilities.
     * @return Network capabilities in string format.
     */
    @NonNull
    public static String networkCapabilitiesToString(@NetCapability int[] netCaps) {
        if (netCaps == null) return "";
        return "[" + Arrays.stream(netCaps)
                .mapToObj(DataUtils::networkCapabilityToString)
                .collect(Collectors.joining("|")) + "]";
    }

    /**
     * Convert the validation status to string.
     *
     * @param status The validation status.
     * @return The validation status in string format.
     */
    @NonNull
    public static String validationStatusToString(@ValidationStatus int status) {
        return switch (status) {
            case NetworkAgent.VALIDATION_STATUS_VALID -> "VALID";
            case NetworkAgent.VALIDATION_STATUS_NOT_VALID -> "INVALID";
            default -> {
                loge("Unknown validation status(" + status + ")");
                yield "UNKNOWN(" + status + ")";
            }
        };
    }

    /**
     * Convert network capability into APN type.
     *
     * @param networkCapability Network capability.
     * @return APN type.
     */
    @ApnType
    public static int networkCapabilityToApnType(@NetCapability int networkCapability) {
        return switch (networkCapability) {
            case NetworkCapabilities.NET_CAPABILITY_MMS -> ApnSetting.TYPE_MMS;
            case NetworkCapabilities.NET_CAPABILITY_SUPL -> ApnSetting.TYPE_SUPL;
            case NetworkCapabilities.NET_CAPABILITY_DUN -> ApnSetting.TYPE_DUN;
            case NetworkCapabilities.NET_CAPABILITY_FOTA -> ApnSetting.TYPE_FOTA;
            case NetworkCapabilities.NET_CAPABILITY_IMS -> ApnSetting.TYPE_IMS;
            case NetworkCapabilities.NET_CAPABILITY_CBS -> ApnSetting.TYPE_CBS;
            case NetworkCapabilities.NET_CAPABILITY_XCAP -> ApnSetting.TYPE_XCAP;
            case NetworkCapabilities.NET_CAPABILITY_EIMS -> ApnSetting.TYPE_EMERGENCY;
            case NetworkCapabilities.NET_CAPABILITY_INTERNET -> ApnSetting.TYPE_DEFAULT;
            case NetworkCapabilities.NET_CAPABILITY_MCX -> ApnSetting.TYPE_MCX;
            case NetworkCapabilities.NET_CAPABILITY_IA -> ApnSetting.TYPE_IA;
            case NetworkCapabilities.NET_CAPABILITY_ENTERPRISE -> ApnSetting.TYPE_ENTERPRISE;
            case NetworkCapabilities.NET_CAPABILITY_VSIM -> ApnSetting.TYPE_VSIM;
            case NetworkCapabilities.NET_CAPABILITY_BIP -> ApnSetting.TYPE_BIP;
            case NetworkCapabilities.NET_CAPABILITY_RCS -> ApnSetting.TYPE_RCS;
            case NetworkCapabilities.NET_CAPABILITY_OEM_PAID -> ApnSetting.TYPE_OEM_PAID;
            case NetworkCapabilities.NET_CAPABILITY_OEM_PRIVATE -> ApnSetting.TYPE_OEM_PRIVATE;
            default -> ApnSetting.TYPE_NONE;
        };
    }

    /**
     * Convert APN type to capability.
     *
     * @param apnType APN type.
     * @return Network capability.
     */
    @NetCapability
    public static int apnTypeToNetworkCapability(@ApnType int apnType) {
        return switch (apnType) {
            case ApnSetting.TYPE_MMS -> NetworkCapabilities.NET_CAPABILITY_MMS;
            case ApnSetting.TYPE_SUPL -> NetworkCapabilities.NET_CAPABILITY_SUPL;
            case ApnSetting.TYPE_DUN -> NetworkCapabilities.NET_CAPABILITY_DUN;
            case ApnSetting.TYPE_FOTA -> NetworkCapabilities.NET_CAPABILITY_FOTA;
            case ApnSetting.TYPE_IMS -> NetworkCapabilities.NET_CAPABILITY_IMS;
            case ApnSetting.TYPE_CBS -> NetworkCapabilities.NET_CAPABILITY_CBS;
            case ApnSetting.TYPE_XCAP -> NetworkCapabilities.NET_CAPABILITY_XCAP;
            case ApnSetting.TYPE_EMERGENCY -> NetworkCapabilities.NET_CAPABILITY_EIMS;
            case ApnSetting.TYPE_DEFAULT -> NetworkCapabilities.NET_CAPABILITY_INTERNET;
            case ApnSetting.TYPE_MCX -> NetworkCapabilities.NET_CAPABILITY_MCX;
            case ApnSetting.TYPE_IA -> NetworkCapabilities.NET_CAPABILITY_IA;
            case ApnSetting.TYPE_BIP -> NetworkCapabilities.NET_CAPABILITY_BIP;
            case ApnSetting.TYPE_VSIM -> NetworkCapabilities.NET_CAPABILITY_VSIM;
            case ApnSetting.TYPE_ENTERPRISE -> NetworkCapabilities.NET_CAPABILITY_ENTERPRISE;
            case ApnSetting.TYPE_RCS -> NetworkCapabilities.NET_CAPABILITY_RCS;
            case ApnSetting.TYPE_OEM_PAID -> NetworkCapabilities.NET_CAPABILITY_OEM_PAID;
            case ApnSetting.TYPE_OEM_PRIVATE -> NetworkCapabilities.NET_CAPABILITY_OEM_PRIVATE;
            default -> -1;
        };
    }

    /**
     * Convert network type to access network type.
     *
     * @param networkType The network type.
     * @return The access network type.
     */
    @RadioAccessNetworkType
    public static int networkTypeToAccessNetworkType(@NetworkType int networkType) {
        return switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyManager.NETWORK_TYPE_GSM ->
                    AccessNetworkType.GERAN;
            case TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA,
                    TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSUPA,
                    TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_TD_SCDMA ->
                    AccessNetworkType.UTRAN;
            case TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_EVDO_0,
                    TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_EVDO_B,
                    TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_EHRPD ->
                    AccessNetworkType.CDMA2000;
            case TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_LTE_CA ->
                    AccessNetworkType.EUTRAN;
            case TelephonyManager.NETWORK_TYPE_IWLAN -> AccessNetworkType.IWLAN;
            case TelephonyManager.NETWORK_TYPE_NR -> AccessNetworkType.NGRAN;
            default -> AccessNetworkType.UNKNOWN;
        };
    }

    /**
     * Convert the elapsed time to the current time with readable time format.
     *
     * @param elapsedTime The elapsed time retrieved from {@link SystemClock#elapsedRealtime()}.
     * @return The string format time.
     */
    @NonNull
    public static String elapsedTimeToString(@ElapsedRealtimeLong long elapsedTime) {
        return (elapsedTime != 0) ? systemTimeToString(System.currentTimeMillis()
                - SystemClock.elapsedRealtime() + elapsedTime) : "never";
    }

    /**
     * Convert the system time to the human readable format.
     *
     * @param systemTime The system time retrieved from {@link System#currentTimeMillis()}.
     * @return The string format time.
     */
    @NonNull
    public static String systemTimeToString(@CurrentTimeMillisLong long systemTime) {
        return (systemTime != 0) ? TIME_FORMAT.format(systemTime) : "never";
    }

    /**
     * Convert the IMS feature to string.
     *
     * @param imsFeature IMS feature.
     * @return IMS feature in string format.
     */
    @NonNull
    public static String imsFeatureToString(@ImsFeature.FeatureType int imsFeature) {
        return switch (imsFeature) {
            case ImsFeature.FEATURE_MMTEL -> "MMTEL";
            case ImsFeature.FEATURE_RCS -> "RCS";
            default -> {
                loge("Unknown IMS feature(" + imsFeature + ")");
                yield "Unknown(" + imsFeature + ")";
            }
        };
    }

    /**
     * Group the network requests into several list that contains the same network capabilities.
     *
     * @param networkRequestList The provided network requests.
     * @param featureFlags The feature flag.
     *
     * @return The network requests after grouping.
     */
    @NonNull
    public static List<NetworkRequestList> getGroupedNetworkRequestList(
            @NonNull NetworkRequestList networkRequestList, @NonNull FeatureFlags featureFlags) {
        List<NetworkRequestList> requests = new ArrayList<>();
        record NetworkCapabilitiesKey(Set<Integer> caps, Set<Integer> enterpriseIds,
                                      Set<Integer> transportTypes) { }

        // Key is the combination of capabilities, enterprise ids, and transport types.
        Map<NetworkCapabilitiesKey, NetworkRequestList> requestsMap = new ArrayMap<>();
        for (TelephonyNetworkRequest networkRequest : networkRequestList) {
            requestsMap.computeIfAbsent(new NetworkCapabilitiesKey(
                            Arrays.stream(networkRequest.getCapabilities())
                                    .boxed().collect(Collectors.toSet()),
                            Arrays.stream(networkRequest.getNativeNetworkRequest()
                                            .getEnterpriseIds())
                                    .boxed().collect(Collectors.toSet()),
                            Arrays.stream(networkRequest.getTransportTypes())
                                    .boxed().collect(Collectors.toSet())
                            ),
                    v -> new NetworkRequestList()).add(networkRequest);
        }
        requests.addAll(requestsMap.values());
        // Sort the requests so the network request list with higher priority will be at the front.
        return requests.stream()
                .sorted((list1, list2) -> Integer.compare(
                        list2.get(0).getPriority(), list1.get(0).getPriority()))
                .collect(Collectors.toList());
    }

    /**
     * Get the target transport from source transport. This is only used for handover between
     * IWLAN and cellular scenario.
     *
     * @param sourceTransport The source transport.
     * @return The target transport.
     */
    @TransportType
    public static int getTargetTransport(@TransportType int sourceTransport) {
        return sourceTransport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                ? AccessNetworkConstants.TRANSPORT_TYPE_WLAN
                : AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
    }

    /**
     * Convert link status to string.
     *
     * @param linkStatus The link status.
     * @return The link status in string format.
     */
    @NonNull
    public static String linkStatusToString(@LinkStatus int linkStatus) {
        return switch (linkStatus) {
            case DataCallResponse.LINK_STATUS_UNKNOWN -> "UNKNOWN";
            case DataCallResponse.LINK_STATUS_INACTIVE -> "INACTIVE";
            case DataCallResponse.LINK_STATUS_ACTIVE -> "ACTIVE";
            case DataCallResponse.LINK_STATUS_DORMANT -> "DORMANT";
            default -> {
                loge("Unknown link status(" + linkStatus + ")");
                yield "UNKNOWN(" + linkStatus + ")";
            }
        };
    }

    /**
     * Check if access network type is valid.
     *
     * @param accessNetworkType The access network type to check.
     * @return {@code true} if the access network type is valid.
     */
    public static boolean isValidAccessNetwork(@RadioAccessNetworkType int accessNetworkType) {
        return switch (accessNetworkType) {
            case AccessNetworkType.GERAN, AccessNetworkType.UTRAN, AccessNetworkType.EUTRAN,
                    AccessNetworkType.CDMA2000, AccessNetworkType.IWLAN, AccessNetworkType.NGRAN ->
                    true;
            default -> false;
        };
    }

    /**
     * Convert data activity to string.
     *
     * @param dataActivity The data activity.
     * @return The data activity in string format.
     */
    @NonNull
    public static String dataActivityToString(@DataActivityType int dataActivity) {
        return switch (dataActivity) {
            case TelephonyManager.DATA_ACTIVITY_NONE -> "NONE";
            case TelephonyManager.DATA_ACTIVITY_IN -> "IN";
            case TelephonyManager.DATA_ACTIVITY_OUT -> "OUT";
            case TelephonyManager.DATA_ACTIVITY_INOUT -> "INOUT";
            case TelephonyManager.DATA_ACTIVITY_DORMANT -> "DORMANT";
            default -> {
                loge("Unknown data activity(" + dataActivity + ")");
                yield "UNKNOWN(" + dataActivity + ")";
            }
        };
    }

    private static void loge(String msg) {
        Rlog.e(TAG, msg);
    }
}

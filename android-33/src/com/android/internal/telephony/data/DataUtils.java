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
import android.telephony.Annotation.DataActivityType;
import android.telephony.Annotation.NetCapability;
import android.telephony.Annotation.NetworkType;
import android.telephony.Annotation.ValidationStatus;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.telephony.data.ApnSetting.ApnType;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataCallResponse.LinkStatus;
import android.telephony.data.DataProfile;
import android.telephony.ims.feature.ImsFeature;
import android.util.ArrayMap;

import com.android.internal.telephony.data.DataNetworkController.NetworkRequestList;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class contains all the utility methods used by telephony data stack.
 */
public class DataUtils {
    /** The time format for converting time to readable string. */
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    /**
     * Get the network capability from the string.
     *
     * @param capabilityString The capability in string format
     * @return The network capability. -1 if not found.
     */
    public static @NetCapability int getNetworkCapabilityFromString(
            @NonNull String capabilityString) {
        switch (capabilityString.toUpperCase(Locale.ROOT)) {
            case "MMS": return NetworkCapabilities.NET_CAPABILITY_MMS;
            case "SUPL": return NetworkCapabilities.NET_CAPABILITY_SUPL;
            case "DUN": return NetworkCapabilities.NET_CAPABILITY_DUN;
            case "FOTA": return NetworkCapabilities.NET_CAPABILITY_FOTA;
            case "IMS": return NetworkCapabilities.NET_CAPABILITY_IMS;
            case "CBS": return NetworkCapabilities.NET_CAPABILITY_CBS;
            case "XCAP": return NetworkCapabilities.NET_CAPABILITY_XCAP;
            case "EIMS": return NetworkCapabilities.NET_CAPABILITY_EIMS;
            case "INTERNET": return NetworkCapabilities.NET_CAPABILITY_INTERNET;
            case "MCX": return NetworkCapabilities.NET_CAPABILITY_MCX;
            case "VSIM": return NetworkCapabilities.NET_CAPABILITY_VSIM;
            case "BIP" : return NetworkCapabilities.NET_CAPABILITY_BIP;
            case "ENTERPRISE": return NetworkCapabilities.NET_CAPABILITY_ENTERPRISE;
            case "PRIORITIZE_BANDWIDTH":
                return NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_BANDWIDTH;
            case "PRIORITIZE_LATENCY":
                return NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_LATENCY;
            default:
                return -1;
        }
    }

    /**
     * Get Set of network capabilities from string joined by {@code |}, space is ignored.
     * If input string contains unknown capability or malformatted(e.g. empty string), -1 is
     * included in the returned set.
     *
     * @param capabilitiesString capability strings joined by {@code |}
     * @return Set of capabilities
     */
    public static @NetCapability Set<Integer> getNetworkCapabilitiesFromString(
            @NonNull String capabilitiesString) {
        // e.g. "IMS|" is not allowed
        if (!capabilitiesString.matches("(\\s*[a-zA-Z]+\\s*)(\\|\\s*[a-zA-Z]+\\s*)*")) {
            return Collections.singleton(-1);
        }
        return Arrays.stream(capabilitiesString.split("\\s*\\|\\s*"))
                .map(String::trim)
                .map(DataUtils::getNetworkCapabilityFromString)
                .collect(Collectors.toSet());
    }

    /**
     * Convert a network capability to string.
     *
     * This is for debugging and logging purposes only.
     *
     * @param netCap Network capability.
     * @return Network capability in string format.
     */
    public static @NonNull String networkCapabilityToString(@NetCapability int netCap) {
        switch (netCap) {
            case NetworkCapabilities.NET_CAPABILITY_MMS:                  return "MMS";
            case NetworkCapabilities.NET_CAPABILITY_SUPL:                 return "SUPL";
            case NetworkCapabilities.NET_CAPABILITY_DUN:                  return "DUN";
            case NetworkCapabilities.NET_CAPABILITY_FOTA:                 return "FOTA";
            case NetworkCapabilities.NET_CAPABILITY_IMS:                  return "IMS";
            case NetworkCapabilities.NET_CAPABILITY_CBS:                  return "CBS";
            case NetworkCapabilities.NET_CAPABILITY_WIFI_P2P:             return "WIFI_P2P";
            case NetworkCapabilities.NET_CAPABILITY_IA:                   return "IA";
            case NetworkCapabilities.NET_CAPABILITY_RCS:                  return "RCS";
            case NetworkCapabilities.NET_CAPABILITY_XCAP:                 return "XCAP";
            case NetworkCapabilities.NET_CAPABILITY_EIMS:                 return "EIMS";
            case NetworkCapabilities.NET_CAPABILITY_NOT_METERED:          return "NOT_METERED";
            case NetworkCapabilities.NET_CAPABILITY_INTERNET:             return "INTERNET";
            case NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED:       return "NOT_RESTRICTED";
            case NetworkCapabilities.NET_CAPABILITY_TRUSTED:              return "TRUSTED";
            case NetworkCapabilities.NET_CAPABILITY_NOT_VPN:              return "NOT_VPN";
            case NetworkCapabilities.NET_CAPABILITY_VALIDATED:            return "VALIDATED";
            case NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL:       return "CAPTIVE_PORTAL";
            case NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING:          return "NOT_ROAMING";
            case NetworkCapabilities.NET_CAPABILITY_FOREGROUND:           return "FOREGROUND";
            case NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED:        return "NOT_CONGESTED";
            case NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED:        return "NOT_SUSPENDED";
            case NetworkCapabilities.NET_CAPABILITY_OEM_PAID:             return "OEM_PAID";
            case NetworkCapabilities.NET_CAPABILITY_MCX:                  return "MCX";
            case NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY:
                return "PARTIAL_CONNECTIVITY";
            case NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED:
                return "TEMPORARILY_NOT_METERED";
            case NetworkCapabilities.NET_CAPABILITY_OEM_PRIVATE:          return "OEM_PRIVATE";
            case NetworkCapabilities.NET_CAPABILITY_VEHICLE_INTERNAL:     return "VEHICLE_INTERNAL";
            case NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED:      return "NOT_VCN_MANAGED";
            case NetworkCapabilities.NET_CAPABILITY_ENTERPRISE:           return "ENTERPRISE";
            case NetworkCapabilities.NET_CAPABILITY_VSIM:                 return "VSIM";
            case NetworkCapabilities.NET_CAPABILITY_BIP:                  return "BIP";
            case NetworkCapabilities.NET_CAPABILITY_HEAD_UNIT:            return "HEAD_UNIT";
            case NetworkCapabilities.NET_CAPABILITY_MMTEL:                return "MMTEL";
            case NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_LATENCY:
                return "PRIORITIZE_LATENCY";
            case NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_BANDWIDTH:
                return "PRIORITIZE_BANDWIDTH";
            default:
                return "Unknown(" + netCap + ")";
        }
    }

    /**
     * Convert network capabilities to string.
     *
     * This is for debugging and logging purposes only.
     *
     * @param netCaps Network capabilities.
     * @return Network capabilities in string format.
     */
    public static @NonNull String networkCapabilitiesToString(
            @NetCapability @Nullable Collection<Integer> netCaps) {
        if (netCaps == null || netCaps.isEmpty()) return "";
        return "[" + netCaps.stream()
                .map(DataUtils::networkCapabilityToString)
                .collect(Collectors.joining("|")) + "]";
    }

    /**
     * Convert network capabilities to string.
     *
     * This is for debugging and logging purposes only.
     *
     * @param netCaps Network capabilities.
     * @return Network capabilities in string format.
     */
    public static @NonNull String networkCapabilitiesToString(@NetCapability int[] netCaps) {
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
    public static @NonNull String validationStatusToString(@ValidationStatus int status) {
        switch (status) {
            case NetworkAgent.VALIDATION_STATUS_VALID: return "VALID";
            case NetworkAgent.VALIDATION_STATUS_NOT_VALID: return "INVALID";
            default: return "UNKNOWN(" + status + ")";
        }
    }

    /**
     * Convert network capability into APN type.
     *
     * @param networkCapability Network capability.
     * @return APN type.
     */
    public static @ApnType int networkCapabilityToApnType(@NetCapability int networkCapability) {
        switch (networkCapability) {
            case NetworkCapabilities.NET_CAPABILITY_MMS:
                return ApnSetting.TYPE_MMS;
            case NetworkCapabilities.NET_CAPABILITY_SUPL:
                return ApnSetting.TYPE_SUPL;
            case NetworkCapabilities.NET_CAPABILITY_DUN:
                return ApnSetting.TYPE_DUN;
            case NetworkCapabilities.NET_CAPABILITY_FOTA:
                return ApnSetting.TYPE_FOTA;
            case NetworkCapabilities.NET_CAPABILITY_IMS:
                return ApnSetting.TYPE_IMS;
            case NetworkCapabilities.NET_CAPABILITY_CBS:
                return ApnSetting.TYPE_CBS;
            case NetworkCapabilities.NET_CAPABILITY_XCAP:
                return ApnSetting.TYPE_XCAP;
            case NetworkCapabilities.NET_CAPABILITY_EIMS:
                return ApnSetting.TYPE_EMERGENCY;
            case NetworkCapabilities.NET_CAPABILITY_INTERNET:
                return ApnSetting.TYPE_DEFAULT;
            case NetworkCapabilities.NET_CAPABILITY_MCX:
                return ApnSetting.TYPE_MCX;
            case NetworkCapabilities.NET_CAPABILITY_IA:
                return ApnSetting.TYPE_IA;
            case NetworkCapabilities.NET_CAPABILITY_ENTERPRISE:
                return ApnSetting.TYPE_ENTERPRISE;
            case NetworkCapabilities.NET_CAPABILITY_VSIM:
                return ApnSetting.TYPE_VSIM;
            case NetworkCapabilities.NET_CAPABILITY_BIP:
                return ApnSetting.TYPE_BIP;
            default:
                return ApnSetting.TYPE_NONE;
        }
    }

    /**
     * Convert APN type to capability.
     *
     * @param apnType APN type.
     * @return Network capability.
     */
    public static @NetCapability int apnTypeToNetworkCapability(@ApnType int apnType) {
        switch (apnType) {
            case ApnSetting.TYPE_MMS:
                return NetworkCapabilities.NET_CAPABILITY_MMS;
            case ApnSetting.TYPE_SUPL:
                return NetworkCapabilities.NET_CAPABILITY_SUPL;
            case ApnSetting.TYPE_DUN:
                return NetworkCapabilities.NET_CAPABILITY_DUN;
            case ApnSetting.TYPE_FOTA:
                return NetworkCapabilities.NET_CAPABILITY_FOTA;
            case ApnSetting.TYPE_IMS:
                return NetworkCapabilities.NET_CAPABILITY_IMS;
            case ApnSetting.TYPE_CBS:
                return NetworkCapabilities.NET_CAPABILITY_CBS;
            case ApnSetting.TYPE_XCAP:
                return NetworkCapabilities.NET_CAPABILITY_XCAP;
            case ApnSetting.TYPE_EMERGENCY:
                return NetworkCapabilities.NET_CAPABILITY_EIMS;
            case ApnSetting.TYPE_DEFAULT:
                return NetworkCapabilities.NET_CAPABILITY_INTERNET;
            case ApnSetting.TYPE_MCX:
                return NetworkCapabilities.NET_CAPABILITY_MCX;
            case ApnSetting.TYPE_IA:
                return NetworkCapabilities.NET_CAPABILITY_IA;
            case ApnSetting.TYPE_BIP:
                return NetworkCapabilities.NET_CAPABILITY_BIP;
            case ApnSetting.TYPE_VSIM:
                return NetworkCapabilities.NET_CAPABILITY_VSIM;
            case ApnSetting.TYPE_ENTERPRISE:
                return NetworkCapabilities.NET_CAPABILITY_ENTERPRISE;
            default:
                return -1;
        }
    }

    /**
     * Convert network type to access network type.
     *
     * @param networkType The network type.
     * @return The access network type.
     */
    public static @RadioAccessNetworkType int networkTypeToAccessNetworkType(
            @NetworkType int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GSM:
                return AccessNetworkType.GERAN;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return AccessNetworkType.UTRAN;
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return AccessNetworkType.CDMA2000;
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_LTE_CA:
                return AccessNetworkType.EUTRAN;
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return AccessNetworkType.IWLAN;
            case TelephonyManager.NETWORK_TYPE_NR:
                return AccessNetworkType.NGRAN;
            default:
                return AccessNetworkType.UNKNOWN;
        }
    }

    /**
     * Convert the elapsed time to the current time with readable time format.
     *
     * @param elapsedTime The elapsed time retrieved from {@link SystemClock#elapsedRealtime()}.
     * @return The string format time.
     */
    public static @NonNull String elapsedTimeToString(@ElapsedRealtimeLong long elapsedTime) {
        return (elapsedTime != 0) ? systemTimeToString(System.currentTimeMillis()
                - SystemClock.elapsedRealtime() + elapsedTime) : "never";
    }

    /**
     * Convert the system time to the human readable format.
     *
     * @param systemTime The system time retrieved from {@link System#currentTimeMillis()}.
     * @return The string format time.
     */
    public static @NonNull String systemTimeToString(@CurrentTimeMillisLong long systemTime) {
        return (systemTime != 0) ? TIME_FORMAT.format(systemTime) : "never";
    }

    /**
     * Convert the IMS feature to string.
     *
     * @param imsFeature IMS feature.
     * @return IMS feature in string format.
     */
    public static @NonNull String imsFeatureToString(@ImsFeature.FeatureType int imsFeature) {
        switch (imsFeature) {
            case ImsFeature.FEATURE_MMTEL: return "MMTEL";
            case ImsFeature.FEATURE_RCS: return "RCS";
            default:
                return "Unknown(" + imsFeature + ")";
        }
    }

    /**
     * Get the highest priority supported network capability from the specified data profile.
     *
     * @param dataConfigManager The data config that contains network priority information.
     * @param dataProfile The data profile
     * @return The highest priority network capability. -1 if cannot find one.
     */
    public static @NetCapability int getHighestPriorityNetworkCapabilityFromDataProfile(
            @NonNull DataConfigManager dataConfigManager, @NonNull DataProfile dataProfile) {
        if (dataProfile.getApnSetting() == null
                || dataProfile.getApnSetting().getApnTypes().isEmpty()) return -1;
        return dataProfile.getApnSetting().getApnTypes().stream()
                .map(DataUtils::apnTypeToNetworkCapability)
                .sorted(Comparator.comparing(dataConfigManager::getNetworkCapabilityPriority)
                        .reversed())
                .collect(Collectors.toList())
                .get(0);
    }

    /**
     * Group the network requests into several list that contains the same network capabilities.
     *
     * @param networkRequestList The provided network requests.
     * @return The network requests after grouping.
     */
    public static @NonNull List<NetworkRequestList> getGroupedNetworkRequestList(
            @NonNull NetworkRequestList networkRequestList) {
        // Key is the capabilities set.
        Map<Set<Integer>, NetworkRequestList> requestsMap = new ArrayMap<>();
        for (TelephonyNetworkRequest networkRequest : networkRequestList) {
            requestsMap.computeIfAbsent(Arrays.stream(networkRequest.getCapabilities())
                            .boxed().collect(Collectors.toSet()),
                    v -> new NetworkRequestList()).add(networkRequest);
        }
        // Sort the list, so the network request list contains higher priority will be in the front
        // of the list.
        return new ArrayList<>(requestsMap.values()).stream()
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
    public static @TransportType int getTargetTransport(@TransportType int sourceTransport) {
        return sourceTransport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                ? AccessNetworkConstants.TRANSPORT_TYPE_WLAN
                : AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
    }

    /**
     * Get the source transport from target transport. This is only used for handover between
     * IWLAN and cellular scenario.
     *
     * @param targetTransport The target transport.
     * @return The source transport.
     */
    public static @TransportType int getSourceTransport(@TransportType int targetTransport) {
        return targetTransport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                ? AccessNetworkConstants.TRANSPORT_TYPE_WLAN
                : AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
    }

    /**
     * Convert link status to string.
     *
     * @param linkStatus The link status.
     * @return The link status in string format.
     */
    public static @NonNull String linkStatusToString(@LinkStatus int linkStatus) {
        switch (linkStatus) {
            case DataCallResponse.LINK_STATUS_UNKNOWN: return "UNKNOWN";
            case DataCallResponse.LINK_STATUS_INACTIVE: return "INACTIVE";
            case DataCallResponse.LINK_STATUS_ACTIVE: return "ACTIVE";
            case DataCallResponse.LINK_STATUS_DORMANT: return "DORMANT";
            default: return "UNKNOWN(" + linkStatus + ")";
        }
    }

    /**
     * Check if access network type is valid.
     *
     * @param accessNetworkType The access network type to check.
     * @return {@code true} if the access network type is valid.
     */
    public static boolean isValidAccessNetwork(@RadioAccessNetworkType int accessNetworkType) {
        switch (accessNetworkType) {
            case AccessNetworkType.GERAN:
            case AccessNetworkType.UTRAN:
            case AccessNetworkType.EUTRAN:
            case AccessNetworkType.CDMA2000:
            case AccessNetworkType.IWLAN:
            case AccessNetworkType.NGRAN:
                return true;
            default:
                return false;
        }
    }

    /**
     * Convert data activity to string.
     *
     * @param dataActivity The data activity.
     * @return The data activity in string format.
     */
    public static @NonNull String dataActivityToString(@DataActivityType int dataActivity) {
        switch (dataActivity) {
            case TelephonyManager.DATA_ACTIVITY_NONE: return "NONE";
            case TelephonyManager.DATA_ACTIVITY_IN: return "IN";
            case TelephonyManager.DATA_ACTIVITY_OUT: return "OUT";
            case TelephonyManager.DATA_ACTIVITY_INOUT: return "INOUT";
            case TelephonyManager.DATA_ACTIVITY_DORMANT: return "DORMANT";
            default: return "UNKNOWN(" + dataActivity + ")";
        }
    }
}

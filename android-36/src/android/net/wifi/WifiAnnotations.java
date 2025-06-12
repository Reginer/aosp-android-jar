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

package android.net.wifi;

import android.annotation.IntDef;
import android.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Wifi annotations meant to be statically linked into client modules, since they cannot be
 * exposed as @SystemApi.
 *
 * e.g. {@link IntDef}, {@link StringDef}
 *
 * @hide
 */
public final class WifiAnnotations {
    private WifiAnnotations() {}

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"SCAN_TYPE_"}, value = {
            WifiScanner.SCAN_TYPE_LOW_LATENCY,
            WifiScanner.SCAN_TYPE_LOW_POWER,
            WifiScanner.SCAN_TYPE_HIGH_ACCURACY})
    public @interface ScanType {}

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"WIFI_BAND_"}, value = {
            WifiScanner.WIFI_BAND_UNSPECIFIED,
            WifiScanner.WIFI_BAND_24_GHZ,
            WifiScanner.WIFI_BAND_5_GHZ,
            WifiScanner.WIFI_BAND_5_GHZ_DFS_ONLY,
            WifiScanner.WIFI_BAND_6_GHZ})
    public @interface WifiBandBasic {}

    @IntDef(prefix = { "CHANNEL_WIDTH_" }, value = {
            SoftApInfo.CHANNEL_WIDTH_INVALID,
            SoftApInfo.CHANNEL_WIDTH_20MHZ_NOHT,
            SoftApInfo.CHANNEL_WIDTH_20MHZ,
            SoftApInfo.CHANNEL_WIDTH_40MHZ,
            SoftApInfo.CHANNEL_WIDTH_80MHZ,
            SoftApInfo.CHANNEL_WIDTH_80MHZ_PLUS_MHZ,
            SoftApInfo.CHANNEL_WIDTH_160MHZ,
            SoftApInfo.CHANNEL_WIDTH_320MHZ,
            SoftApInfo.CHANNEL_WIDTH_2160MHZ,
            SoftApInfo.CHANNEL_WIDTH_4320MHZ,
            SoftApInfo.CHANNEL_WIDTH_6480MHZ,
            SoftApInfo.CHANNEL_WIDTH_8640MHZ,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Bandwidth {}

    @IntDef(prefix = { "CHANNEL_WIDTH_" }, value = {
            ScanResult.CHANNEL_WIDTH_20MHZ,
            ScanResult.CHANNEL_WIDTH_40MHZ,
            ScanResult.CHANNEL_WIDTH_80MHZ,
            ScanResult.CHANNEL_WIDTH_160MHZ,
            ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ,
            ScanResult.CHANNEL_WIDTH_320MHZ,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ChannelWidth{}

    @IntDef(prefix = { "PREAMBLE_" }, value = {
            ScanResult.PREAMBLE_LEGACY,
            ScanResult.PREAMBLE_HT,
            ScanResult.PREAMBLE_VHT,
            ScanResult.PREAMBLE_HE,
            ScanResult.PREAMBLE_EHT,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PreambleType {}

    @IntDef(prefix = { "WIFI_STANDARD_" }, value = {
            ScanResult.WIFI_STANDARD_UNKNOWN,
            ScanResult.WIFI_STANDARD_LEGACY,
            ScanResult.WIFI_STANDARD_11N,
            ScanResult.WIFI_STANDARD_11AC,
            ScanResult.WIFI_STANDARD_11AX,
            ScanResult.WIFI_STANDARD_11AD,
            ScanResult.WIFI_STANDARD_11BE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface WifiStandard{}

    @IntDef(prefix = { "PROTOCOL_" }, value = {
            ScanResult.PROTOCOL_NONE,
            ScanResult.PROTOCOL_WPA,
            ScanResult.PROTOCOL_RSN,
            ScanResult.PROTOCOL_OSEN,
            ScanResult.PROTOCOL_WAPI
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Protocol {}

    @IntDef(prefix = { "KEY_MGMT_" }, value = {
        ScanResult.KEY_MGMT_NONE,
        ScanResult.KEY_MGMT_PSK,
        ScanResult.KEY_MGMT_EAP,
        ScanResult.KEY_MGMT_FT_PSK,
        ScanResult.KEY_MGMT_FT_EAP,
        ScanResult.KEY_MGMT_PSK_SHA256,
        ScanResult.KEY_MGMT_EAP_SHA256,
        ScanResult.KEY_MGMT_OSEN,
        ScanResult.KEY_MGMT_SAE,
        ScanResult.KEY_MGMT_OWE,
        ScanResult.KEY_MGMT_EAP_SUITE_B_192,
        ScanResult.KEY_MGMT_FT_SAE,
        ScanResult.KEY_MGMT_OWE_TRANSITION,
        ScanResult.KEY_MGMT_WAPI_PSK,
        ScanResult.KEY_MGMT_WAPI_CERT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface KeyMgmt {}

    @IntDef(prefix = { "CIPHER_" }, value = {
        ScanResult.CIPHER_NONE,
        ScanResult.CIPHER_NO_GROUP_ADDRESSED,
        ScanResult.CIPHER_TKIP,
        ScanResult.CIPHER_CCMP,
        ScanResult.CIPHER_GCMP_256,
        ScanResult.CIPHER_SMS4,
        ScanResult.CIPHER_GCMP_128,
        ScanResult.CIPHER_BIP_GMAC_128,
        ScanResult.CIPHER_BIP_GMAC_256,
        ScanResult.CIPHER_BIP_CMAC_256,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Cipher {}

    /**
     * Security type of current connection.
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "SECURITY_TYPE_" }, value = {
            WifiInfo.SECURITY_TYPE_UNKNOWN,
            WifiInfo.SECURITY_TYPE_OPEN,
            WifiInfo.SECURITY_TYPE_WEP,
            WifiInfo.SECURITY_TYPE_PSK,
            WifiInfo.SECURITY_TYPE_EAP,
            WifiInfo.SECURITY_TYPE_SAE,
            WifiInfo.SECURITY_TYPE_OWE,
            WifiInfo.SECURITY_TYPE_WAPI_PSK,
            WifiInfo.SECURITY_TYPE_WAPI_CERT,
            WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE,
            WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT,
            WifiInfo.SECURITY_TYPE_PASSPOINT_R1_R2,
            WifiInfo.SECURITY_TYPE_PASSPOINT_R3,
            WifiInfo.SECURITY_TYPE_DPP,
    })
    public @interface SecurityType {}

    /**
     * The type of wifi uri scheme.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"URI_SCHEME_"}, value = {
            UriParserResults.URI_SCHEME_ZXING_WIFI_NETWORK_CONFIG,
            UriParserResults.URI_SCHEME_DPP,
    })
    public @interface UriScheme {}

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"REASON_"}, value = {
            DeauthenticationReasonCode.REASON_UNKNOWN,
            DeauthenticationReasonCode.REASON_UNSPECIFIED,
            DeauthenticationReasonCode.REASON_PREV_AUTH_NOT_VALID,
            DeauthenticationReasonCode.REASON_DEAUTH_LEAVING,
            DeauthenticationReasonCode.REASON_DISASSOC_DUE_TO_INACTIVITY,
            DeauthenticationReasonCode.REASON_DISASSOC_AP_BUSY,
            DeauthenticationReasonCode.REASON_CLASS2_FRAME_FROM_NONAUTH_STA,
            DeauthenticationReasonCode.REASON_CLASS3_FRAME_FROM_NONASSOC_STA,
            DeauthenticationReasonCode.REASON_DISASSOC_STA_HAS_LEFT,
            DeauthenticationReasonCode.REASON_STA_REQ_ASSOC_WITHOUT_AUTH,
            DeauthenticationReasonCode.REASON_PWR_CAPABILITY_NOT_VALID,
            DeauthenticationReasonCode.REASON_SUPPORTED_CHANNEL_NOT_VALID,
            DeauthenticationReasonCode.REASON_BSS_TRANSITION_DISASSOC,
            DeauthenticationReasonCode.REASON_INVALID_IE,
            DeauthenticationReasonCode.REASON_MICHAEL_MIC_FAILURE,
            DeauthenticationReasonCode.REASON_FOURWAY_HANDSHAKE_TIMEOUT,
            DeauthenticationReasonCode.REASON_GROUP_KEY_UPDATE_TIMEOUT,
            DeauthenticationReasonCode.REASON_IE_IN_4WAY_DIFFERS,
            DeauthenticationReasonCode.REASON_GROUP_CIPHER_NOT_VALID,
            DeauthenticationReasonCode.REASON_PAIRWISE_CIPHER_NOT_VALID,
            DeauthenticationReasonCode.REASON_AKMP_NOT_VALID,
            DeauthenticationReasonCode.REASON_UNSUPPORTED_RSN_IE_VERSION,
            DeauthenticationReasonCode.REASON_INVALID_RSN_IE_CAPAB,
            DeauthenticationReasonCode.REASON_IEEE_802_1X_AUTH_FAILED,
            DeauthenticationReasonCode.REASON_CIPHER_SUITE_REJECTED,
            DeauthenticationReasonCode.REASON_TDLS_TEARDOWN_UNREACHABLE,
            DeauthenticationReasonCode.REASON_TDLS_TEARDOWN_UNSPECIFIED,
            DeauthenticationReasonCode.REASON_SSP_REQUESTED_DISASSOC,
            DeauthenticationReasonCode.REASON_NO_SSP_ROAMING_AGREEMENT,
            DeauthenticationReasonCode.REASON_BAD_CIPHER_OR_AKM,
            DeauthenticationReasonCode.REASON_NOT_AUTHORIZED_THIS_LOCATION,
            DeauthenticationReasonCode.REASON_SERVICE_CHANGE_PRECLUDES_TS,
            DeauthenticationReasonCode.REASON_UNSPECIFIED_QOS_REASON,
            DeauthenticationReasonCode.REASON_NOT_ENOUGH_BANDWIDTH,
            DeauthenticationReasonCode.REASON_DISASSOC_LOW_ACK,
            DeauthenticationReasonCode.REASON_EXCEEDED_TXOP,
            DeauthenticationReasonCode.REASON_STA_LEAVING,
            DeauthenticationReasonCode.REASON_END_TS_BA_DLS,
            DeauthenticationReasonCode.REASON_UNKNOWN_TS_BA,
            DeauthenticationReasonCode.REASON_TIMEOUT,
            DeauthenticationReasonCode.REASON_PEERKEY_MISMATCH,
            DeauthenticationReasonCode.REASON_AUTHORIZED_ACCESS_LIMIT_REACHED,
            DeauthenticationReasonCode.REASON_EXTERNAL_SERVICE_REQUIREMENTS,
            DeauthenticationReasonCode.REASON_INVALID_FT_ACTION_FRAME_COUNT,
            DeauthenticationReasonCode.REASON_INVALID_PMKID,
            DeauthenticationReasonCode.REASON_INVALID_MDE,
            DeauthenticationReasonCode.REASON_INVALID_FTE,
            DeauthenticationReasonCode.REASON_MESH_PEERING_CANCELLED,
            DeauthenticationReasonCode.REASON_MESH_MAX_PEERS,
            DeauthenticationReasonCode.REASON_MESH_CONFIG_POLICY_VIOLATION,
            DeauthenticationReasonCode.REASON_MESH_CLOSE_RCVD,
            DeauthenticationReasonCode.REASON_MESH_MAX_RETRIES,
            DeauthenticationReasonCode.REASON_MESH_CONFIRM_TIMEOUT,
            DeauthenticationReasonCode.REASON_MESH_INVALID_GTK,
            DeauthenticationReasonCode.REASON_MESH_INCONSISTENT_PARAMS,
            DeauthenticationReasonCode.REASON_MESH_INVALID_SECURITY_CAP,
            DeauthenticationReasonCode.REASON_MESH_PATH_ERROR_NO_PROXY_INFO,
            DeauthenticationReasonCode.REASON_MESH_PATH_ERROR_NO_FORWARDING_INFO,
            DeauthenticationReasonCode.REASON_MESH_PATH_ERROR_DEST_UNREACHABLE,
            DeauthenticationReasonCode.REASON_MAC_ADDRESS_ALREADY_EXISTS_IN_MBSS,
            DeauthenticationReasonCode.REASON_MESH_CHANNEL_SWITCH_REGULATORY_REQ,
            DeauthenticationReasonCode.REASON_MESH_CHANNEL_SWITCH_UNSPECIFIED,
    })
    public @interface SoftApDisconnectReason {}
}

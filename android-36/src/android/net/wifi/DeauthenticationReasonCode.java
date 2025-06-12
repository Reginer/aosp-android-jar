/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.annotation.FlaggedApi;
import android.annotation.SystemApi;

import com.android.wifi.flags.Flags;

/**
 * Defines integer constants for Soft AP deauthentication reason codes.
 *
 * <p>These reason codes provide information about why a client was disconnected from a Soft AP.
 * Refer to Section 9.4.1.7 and Table 9-45 of the IEEE 802.11-2016 standard for more information.
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
public final class DeauthenticationReasonCode {
    /** Disconnected for an unknown reason. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_UNKNOWN = 0;

    /** Disconnected for an unspecified reason. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_UNSPECIFIED = 1;

    /** Disconnected because the previous authentication is no longer valid. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_PREV_AUTH_NOT_VALID = 2;

    /** Disconnected because the client is being de-authenticated. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_DEAUTH_LEAVING = 3;

    /** Disconnected due to inactivity. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_DISASSOC_DUE_TO_INACTIVITY = 4;

    /** Disconnected because the AP is unable to handle all currently associated stations. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_DISASSOC_AP_BUSY = 5;

    /** Disconnected because of a Class 2 frame received from a non-authenticated station. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_CLASS2_FRAME_FROM_NONAUTH_STA = 6;

    /** Disconnected because of a Class 3 frame received from a non-associated station. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_CLASS3_FRAME_FROM_NONASSOC_STA = 7;

    /** Disconnected because the STA has left the network. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_DISASSOC_STA_HAS_LEFT = 8;

    /** Disconnected because the STA requested association without authentication. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_STA_REQ_ASSOC_WITHOUT_AUTH = 9;

    /** Disconnected because the power capability element is not valid. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_PWR_CAPABILITY_NOT_VALID = 10;

    /** Disconnected because the supported channel element is not valid. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_SUPPORTED_CHANNEL_NOT_VALID = 11;

    /** Disconnected due to a BSS transition disassociation. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_BSS_TRANSITION_DISASSOC = 12;

    /** Disconnected because of an invalid information element. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_INVALID_IE = 13;

    /** Disconnected because of a message integrity code failure. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MICHAEL_MIC_FAILURE = 14;

    /** Disconnected due to a four-way handshake timeout. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_FOURWAY_HANDSHAKE_TIMEOUT = 15;

    /** Disconnected due to a group key handshake timeout. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_GROUP_KEY_UPDATE_TIMEOUT = 16;

    /** Disconnected because an information element in the 4-way handshake differs. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_IE_IN_4WAY_DIFFERS = 17;

    /** Disconnected because the group cipher is not valid. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_GROUP_CIPHER_NOT_VALID = 18;

    /** Disconnected because the pairwise cipher is not valid. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_PAIRWISE_CIPHER_NOT_VALID = 19;

    /** Disconnected because the authentication and key management protocol is not valid. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_AKMP_NOT_VALID = 20;

    /** Disconnected because the robust security network IE version is not supported. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_UNSUPPORTED_RSN_IE_VERSION = 21;

    /** Disconnected because the robust security network IE capabilities are invalid. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_INVALID_RSN_IE_CAPAB = 22;

    /** Disconnected because the IEEE 802.1X authentication failed. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_IEEE_802_1X_AUTH_FAILED = 23;

    /** Disconnected because the cipher suite was rejected. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_CIPHER_SUITE_REJECTED = 24;

    /** Disconnected because the Tunneled Direct Link Setup teardown is unreachable. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_TDLS_TEARDOWN_UNREACHABLE = 25;

    /** Disconnected because of a Tunneled Direct Link Setup teardown for an unspecified reason. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_TDLS_TEARDOWN_UNSPECIFIED = 26;

    /** Disconnected because an session security protocol requested disassociation. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_SSP_REQUESTED_DISASSOC = 27;

    /** Disconnected because there is no session security protocol roaming agreement. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_NO_SSP_ROAMING_AGREEMENT = 28;

    /** Disconnected because of an unsupported cipher or authentication key management method. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_BAD_CIPHER_OR_AKM = 29;

    /** Disconnected because the client is not authorized at this location. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_NOT_AUTHORIZED_THIS_LOCATION = 30;

    /** Disconnected because a service change precludes traffic specification. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_SERVICE_CHANGE_PRECLUDES_TS = 31;

    /** Disconnected for an unspecified quality of service related reason. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_UNSPECIFIED_QOS_REASON = 32;

    /** Disconnected because there is not enough bandwidth. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_NOT_ENOUGH_BANDWIDTH = 33;

    /** Disconnected due to low acknowledgment rate. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_DISASSOC_LOW_ACK = 34;

    /** Disconnected for exceeding the transmission opportunity. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_EXCEEDED_TXOP = 35;

    /** Disconnected because the station is leaving the network. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_STA_LEAVING = 36;

    /** Disconnected because of the end of a traffic specification, block ack, or DLS session. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_END_TS_BA_DLS = 37;

    /** Disconnected because of an unknown traffic specification or block ack. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_UNKNOWN_TS_BA = 38;

    /** Disconnected due to a timeout. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_TIMEOUT = 39;

    /** Disconnected because of a peerkey mismatch. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_PEERKEY_MISMATCH = 45;

    /** Disconnected because the authorized access limit has been reached. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_AUTHORIZED_ACCESS_LIMIT_REACHED = 46;

    /** Disconnected due to external service requirements. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_EXTERNAL_SERVICE_REQUIREMENTS = 47;

    /** Disconnected because of an invalid fast transition action frame count. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_INVALID_FT_ACTION_FRAME_COUNT = 48;

    /** Disconnected because of an invalid pairwise master key identifier. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_INVALID_PMKID = 49;

    /** Disconnected because of an invalid management downlink endpoint. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_INVALID_MDE = 50;

    /** Disconnected because of an invalid fast transition endpoint. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_INVALID_FTE = 51;

    /** Disconnected because mesh peering was cancelled. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_PEERING_CANCELLED = 52;

    /** Disconnected because the maximum number of mesh peers has been reached. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_MAX_PEERS = 53;

    /** Disconnected because of a mesh configuration policy violation. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_CONFIG_POLICY_VIOLATION = 54;

    /** Disconnected because a mesh close message was received. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_CLOSE_RCVD = 55;

    /** Disconnected because the maximum number of mesh retries has been reached. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_MAX_RETRIES = 56;

    /** Disconnected due to a mesh confirmation timeout. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_CONFIRM_TIMEOUT = 57;

    /** Disconnected because of an invalid mesh group temporal key. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_INVALID_GTK = 58;

    /** Disconnected because of inconsistent mesh parameters. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_INCONSISTENT_PARAMS = 59;

    /** Disconnected because of an invalid mesh security capability. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_INVALID_SECURITY_CAP = 60;

    /** Disconnected because of a mesh path error: no proxy information. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_PATH_ERROR_NO_PROXY_INFO = 61;

    /** Disconnected because of a mesh path error: no forwarding information. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_PATH_ERROR_NO_FORWARDING_INFO = 62;

    /** Disconnected because of a mesh path error: destination unreachable. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_PATH_ERROR_DEST_UNREACHABLE = 63;

    /** Disconnected because the MAC address already exists in the mesh basic service set. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MAC_ADDRESS_ALREADY_EXISTS_IN_MBSS = 64;

    /** Disconnected due to a mesh channel switch due to regulatory requirements. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_CHANNEL_SWITCH_REGULATORY_REQ = 65;

    /** Disconnected due to a mesh channel switch for an unspecified reason. */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    public static final int REASON_MESH_CHANNEL_SWITCH_UNSPECIFIED = 66;

    private DeauthenticationReasonCode() {} // Private constructor to prevent instantiation
}

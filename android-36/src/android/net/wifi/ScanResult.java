/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.MacAddress;
import android.net.wifi.WifiAnnotations.ChannelWidth;
import android.net.wifi.WifiAnnotations.WifiStandard;
import android.net.wifi.util.ScanResultUtil;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.Keep;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describes information about a detected access point. In addition
 * to the attributes described here, the supplicant keeps track of
 * {@code quality}, {@code noise}, and {@code maxbitrate} attributes,
 * but does not currently report them to external clients.
 */
public final class ScanResult implements Parcelable {

    private static final String TAG = "ScanResult";
    /**
     * The network name.
     *
     * @deprecated Use {@link #getWifiSsid()} instead.
     */
    @Deprecated
    public String SSID;

    /**
     * Ascii encoded SSID. This will replace SSID when we deprecate it. @hide
     *
     * @deprecated Use {@link #getWifiSsid()} instead.
     */
    @Deprecated
    // TODO(b/231433398): add maxTargetSdk = Build.VERSION_CODES.S
    @UnsupportedAppUsage(publicAlternatives = "{@link #getWifiSsid()}")
    public WifiSsid wifiSsid;

    /**
     * Set the SSID of the access point.
     * @hide
     */
    @SystemApi
    public void setWifiSsid(@NonNull WifiSsid ssid) {
        wifiSsid = ssid;
        CharSequence utf8Text = wifiSsid.getUtf8Text();
        SSID = utf8Text != null ? utf8Text.toString() : WifiManager.UNKNOWN_SSID;
    }

    /**
     * The SSID of the access point.
     */
    @Nullable
    public WifiSsid getWifiSsid() {
        return wifiSsid;
    }

    /**
     * The address of the access point.
     */
    public String BSSID;

    /**
     * The Multi-Link Device (MLD) address of the access point.
     * Only applicable for Wi-Fi 7 access points, null otherwise.
     */
    private MacAddress mApMldMacAddress;

    /**
     * Return the access point Multi-Link Device (MLD) MAC Address for Wi-Fi 7 access points.
     * i.e. {@link #getWifiStandard()} returns {@link #WIFI_STANDARD_11BE}.
     *
     * @return MLD MAC Address for access point if exists (Wi-Fi 7 access points), null otherwise.
     */
    @Nullable
    public MacAddress getApMldMacAddress() {
        return mApMldMacAddress;
    }

    /**
     * Set the access point Multi-Link Device (MLD) MAC Address.
     * @hide
     */
    public void setApMldMacAddress(@Nullable MacAddress address) {
        mApMldMacAddress = address;
    }

    /**
     * The Multi-Link Operation (MLO) link id for the access point.
     * Only applicable for Wi-Fi 7 access points.
     */
    private int mApMloLinkId = MloLink.INVALID_MLO_LINK_ID;

    /**
     * Return the access point Multi-Link Operation (MLO) link-id for Wi-Fi 7 access points.
     * i.e. when {@link #getWifiStandard()} returns {@link #WIFI_STANDARD_11BE}, otherwise return
     * {@link MloLink#INVALID_MLO_LINK_ID}.
     *
     * Valid values are 0-15 as described in IEEE 802.11be Specification, section 9.4.2.295b.2.
     *
     * @return {@link MloLink#INVALID_MLO_LINK_ID} or a valid value (0-15).
     */
    @IntRange(from = MloLink.INVALID_MLO_LINK_ID, to = MloLink.MAX_MLO_LINK_ID)
    public int getApMloLinkId() {
        return mApMloLinkId;
    }

    /**
     * Sets the access point Multi-Link Operation (MLO) link-id
     * @hide
     */
    public void setApMloLinkId(int linkId) {
        mApMloLinkId = linkId;
    }

    /**
     * The Multi-Link Operation (MLO) affiliated Links.
     * Only applicable for Wi-Fi 7 access points.
     * Note: the list of links includes the access point for this ScanResult.
     */
    private List<MloLink> mAffiliatedMloLinks = Collections.emptyList();

    /**
     * Return the Multi-Link Operation (MLO) affiliated Links for Wi-Fi 7 access points.
     * i.e. when {@link #getWifiStandard()} returns {@link #WIFI_STANDARD_11BE}.
     *
     * @return List of affiliated MLO links, or an empty list if access point is not Wi-Fi 7
     */
    @NonNull
    public List<MloLink> getAffiliatedMloLinks() {
        return new ArrayList<MloLink>(mAffiliatedMloLinks);
    }

    /**
     * Set the Multi-Link Operation (MLO) affiliated Links.
     * Only applicable for Wi-Fi 7 access points.
     *
     * @hide
     */
    public void setAffiliatedMloLinks(@NonNull List<MloLink> links) {
        mAffiliatedMloLinks = new ArrayList<MloLink>(links);
    }

    /**
     * The HESSID from the beacon.
     * @hide
     */
    @UnsupportedAppUsage
    public long hessid;

    /**
     * The ANQP Domain ID from the Hotspot 2.0 Indication element, if present.
     * @hide
     */
    @UnsupportedAppUsage
    public int anqpDomainId;

    /*
     * This field is equivalent to the |flags|, rather than the |capabilities| field
     * of the per-BSS scan results returned by WPA supplicant. See the definition of
     * |struct wpa_bss| in wpa_supplicant/bss.h for more details.
     */
    /**
     * Describes the authentication, key management, and encryption schemes
     * supported by the access point.
     */
    public String capabilities;

    /**
     * The interface name on which the scan result was received.
     * @hide
     */
    public String ifaceName;

    /**
     * @hide
     * No security protocol.
     */
    @SystemApi
    public static final int PROTOCOL_NONE = 0;
    /**
     * @hide
     * Security protocol type: WPA version 1.
     */
    @SystemApi
    public static final int PROTOCOL_WPA = 1;
    /**
     * @hide
     * Security protocol type: RSN, for WPA version 2, and version 3.
     */
    @SystemApi
    public static final int PROTOCOL_RSN = 2;
    /**
     * @hide
     * Security protocol type:
     * OSU Server-only authenticated layer 2 Encryption Network.
     * Used for Hotspot 2.0.
     */
    @SystemApi
    public static final int PROTOCOL_OSEN = 3;

    /**
     * @hide
     * Security protocol type: WAPI.
     */
    @SystemApi
    public static final int PROTOCOL_WAPI = 4;

    /**
     * @hide
     * No security key management scheme.
     */
    @SystemApi
    public static final int KEY_MGMT_NONE = 0;
    /**
     * @hide
     * Security key management scheme: PSK.
     */
    @SystemApi
    public static final int KEY_MGMT_PSK = 1;
    /**
     * @hide
     * Security key management scheme: EAP.
     */
    @SystemApi
    public static final int KEY_MGMT_EAP = 2;
    /**
     * @hide
     * Security key management scheme: FT_PSK.
     */
    @SystemApi
    public static final int KEY_MGMT_FT_PSK = 3;
    /**
     * @hide
     * Security key management scheme: FT_EAP.
     */
    @SystemApi
    public static final int KEY_MGMT_FT_EAP = 4;
    /**
     * @hide
     * Security key management scheme: PSK_SHA256
     */
    @SystemApi
    public static final int KEY_MGMT_PSK_SHA256 = 5;
    /**
     * @hide
     * Security key management scheme: EAP_SHA256.
     */
    @SystemApi
    public static final int KEY_MGMT_EAP_SHA256 = 6;
    /**
     * @hide
     * Security key management scheme: OSEN.
     * Used for Hotspot 2.0.
     */
    @SystemApi
    public static final int KEY_MGMT_OSEN = 7;
     /**
     * @hide
     * Security key management scheme: SAE.
     */
    @SystemApi
    public static final int KEY_MGMT_SAE = 8;
    /**
     * @hide
     * Security key management scheme: OWE.
     */
    @SystemApi
    public static final int KEY_MGMT_OWE = 9;
    /**
     * @hide
     * Security key management scheme: SUITE_B_192.
     */
    @SystemApi
    public static final int KEY_MGMT_EAP_SUITE_B_192 = 10;
    /**
     * @hide
     * Security key management scheme: FT_SAE.
     */
    @SystemApi
    public static final int KEY_MGMT_FT_SAE = 11;
    /**
     * @hide
     * Security key management scheme: OWE in transition mode.
     */
    @SystemApi
    public static final int KEY_MGMT_OWE_TRANSITION = 12;
    /**
     * @hide
     * Security key management scheme: WAPI_PSK.
     */
    @SystemApi
    public static final int KEY_MGMT_WAPI_PSK = 13;
    /**
     * @hide
     * Security key management scheme: WAPI_CERT.
     */
    @SystemApi
    public static final int KEY_MGMT_WAPI_CERT = 14;

    /**
     * @hide
     * Security key management scheme: FILS_SHA256.
     */
    public static final int KEY_MGMT_FILS_SHA256 = 15;
    /**
     * @hide
     * Security key management scheme: FILS_SHA384.
     */
    public static final int KEY_MGMT_FILS_SHA384 = 16;
    /**
     * @hide
     * Security key management scheme: DPP.
     */
    public static final int KEY_MGMT_DPP = 17;
    /**
     * @hide
     * Security key management scheme: SAE_EXT_KEY.
     */
    public static final int KEY_MGMT_SAE_EXT_KEY = 18;
    /**
     * @hide
     * Security key management scheme: FT_SAE_EXT_KEY.
     */
    public static final int KEY_MGMT_FT_SAE_EXT_KEY = 19;
    /**
     * @hide
     * Security key management scheme: PASN.
     */
    public static final int KEY_MGMT_PASN = 20;

    /**
     * Security key management scheme: FT authentication negotiated over IEEE Std 802.1X using
     * SHA-384.
     * @hide
     */
    public static final int KEY_MGMT_EAP_FT_SHA384 = 21;
    /**
     * Security key management scheme: FT authentication using PSK (SHA-384).
     * @hide
     */
    public static final int KEY_MGMT_FT_PSK_SHA384 = 22;
    /**
     * @hide
     * Security key management scheme: any unknown AKM.
     */
    public static final int KEY_MGMT_UNKNOWN = 23;
    /**
     * @hide
     * No cipher suite.
     */
    @SystemApi
    public static final int CIPHER_NONE = 0;
    /**
     * @hide
     * No group addressed, only used for group data cipher.
     */
    @SystemApi
    public static final int CIPHER_NO_GROUP_ADDRESSED = 1;
    /**
     * @hide
     * Cipher suite: TKIP
     */
    @SystemApi
    public static final int CIPHER_TKIP = 2;
    /**
     * @hide
     * Cipher suite: CCMP
     */
    @SystemApi
    public static final int CIPHER_CCMP = 3;
    /**
     * @hide
     * Cipher suite: GCMP
     */
    @SystemApi
    public static final int CIPHER_GCMP_256 = 4;
    /**
     * @hide
     * Cipher suite: SMS4
     */
    @SystemApi
    public static final int CIPHER_SMS4 = 5;
    /**
     * @hide
     * Cipher suite: GCMP_128
     */
    @SystemApi
    public static final int CIPHER_GCMP_128 = 6;
    /**
     * @hide
     * Cipher suite: BIP_GMAC_128
     */
    @SystemApi
    public static final int CIPHER_BIP_GMAC_128 = 7;
    /**
     * @hide
     * Cipher suite: BIP_GMAC_256
     */
    @SystemApi
    public static final int CIPHER_BIP_GMAC_256 = 8;
    /**
     * @hide
     * Cipher suite: BIP_CMAC_256
     */
    @SystemApi
    public static final int CIPHER_BIP_CMAC_256 = 9;

    /**
     * The detected signal level in dBm, also known as the RSSI.
     *
     * <p>Use {@link android.net.wifi.WifiManager#calculateSignalLevel} to convert this number into
     * an absolute signal level which can be displayed to a user.
     */
    public int level;

    /**
     * The center frequency of the primary 20 MHz frequency (in MHz) of the channel over which the
     * client is communicating with the access point.
     */
    public int frequency;

   /**
    * AP Channel bandwidth is 20 MHZ
    */
    public static final int CHANNEL_WIDTH_20MHZ = 0;
   /**
    * AP Channel bandwidth is 40 MHZ
    */
    public static final int CHANNEL_WIDTH_40MHZ = 1;
   /**
    * AP Channel bandwidth is 80 MHZ
    */
    public static final int CHANNEL_WIDTH_80MHZ = 2;
   /**
    * AP Channel bandwidth is 160 MHZ
    */
    public static final int CHANNEL_WIDTH_160MHZ = 3;
   /**
    * AP Channel bandwidth is 160 MHZ, but 80MHZ + 80MHZ
    */
    public static final int CHANNEL_WIDTH_80MHZ_PLUS_MHZ = 4;
   /**
    * AP Channel bandwidth is 320 MHZ
    */
    public static final int CHANNEL_WIDTH_320MHZ = 5;

    /**
     * Preamble type: Legacy.
     */
    public static final int PREAMBLE_LEGACY = 0;
    /**
     * Preamble type: HT.
     */
    public static final int PREAMBLE_HT = 1;
    /**
     * Preamble type: VHT.
     */
    public static final int PREAMBLE_VHT = 2;
    /**
     * Preamble type: HE.
     */
    public static final int PREAMBLE_HE = 3;

    /**
     * Preamble type: EHT.
     */
    public static final int PREAMBLE_EHT = 4;

    /**
     * Wi-Fi unknown standard
     */
    public static final int WIFI_STANDARD_UNKNOWN = 0;

    /**
     * Wi-Fi 802.11a/b/g
     */
    public static final int WIFI_STANDARD_LEGACY = 1;

    /**
     * Wi-Fi 802.11n
     */
    public static final int WIFI_STANDARD_11N = 4;

    /**
     * Wi-Fi 802.11ac
     */
    public static final int WIFI_STANDARD_11AC = 5;

    /**
     * Wi-Fi 802.11ax
     */
    public static final int WIFI_STANDARD_11AX = 6;

    /**
     * Wi-Fi 802.11ad
     */
    public static final int WIFI_STANDARD_11AD = 7;

    /**
     * Wi-Fi 802.11be
     */
    public static final int WIFI_STANDARD_11BE = 8;

    /**
     * Wi-Fi 2.4 GHz band.
     */
    public static final int WIFI_BAND_24_GHZ = WifiScanner.WIFI_BAND_24_GHZ;

    /**
     * Wi-Fi 5 GHz band.
     */
    public static final int WIFI_BAND_5_GHZ = WifiScanner.WIFI_BAND_5_GHZ;

    /**
     * Wi-Fi 6 GHz band.
     */
    public static final int WIFI_BAND_6_GHZ = WifiScanner.WIFI_BAND_6_GHZ;

    /**
     * Wi-Fi 60 GHz band.
     */
    public static final int WIFI_BAND_60_GHZ = WifiScanner.WIFI_BAND_60_GHZ;

    /**
     * Constant used for dual 5GHz multi-internet use-case only. Not to be used for regular scan
     * result reporting.
     * @hide
     */
    public static final int WIFI_BAND_5_GHZ_LOW = WifiScanner.WIFI_BAND_5_GHZ_LOW;

    /**
     * Constant used for dual 5GHz multi-internet use-case only. Not to be used for regular scan
     * result reporting.
     * @hide
     */
    public static final int WIFI_BAND_5_GHZ_HIGH = WifiScanner.WIFI_BAND_5_GHZ_HIGH;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"WIFI_BAND_"}, value = {
            UNSPECIFIED,
            WIFI_BAND_24_GHZ,
            WIFI_BAND_5_GHZ,
            WIFI_BAND_6_GHZ,
            WIFI_BAND_60_GHZ})
    public @interface WifiBand {};

    /**
     * AP wifi standard.
     */
    private @WifiStandard int mWifiStandard = WIFI_STANDARD_UNKNOWN;

    /**
     * return the AP wifi standard.
     */
    public @WifiStandard int getWifiStandard() {
        return mWifiStandard;
    }

    /**
     * sets the AP wifi standard.
     * @hide
     */
    public void setWifiStandard(@WifiStandard int standard) {
        mWifiStandard = standard;
    }

    /**
     * Convert Wi-Fi standard to string
     * @hide
     */
    public static @Nullable String wifiStandardToString(@WifiStandard int standard) {
        switch(standard) {
            case WIFI_STANDARD_LEGACY:
                return "legacy";
            case WIFI_STANDARD_11N:
                return "11n";
            case WIFI_STANDARD_11AC:
                return "11ac";
            case WIFI_STANDARD_11AX:
                return "11ax";
            case WIFI_STANDARD_11AD:
                return "11ad";
            case WIFI_STANDARD_11BE:
                return "11be";
            case WIFI_STANDARD_UNKNOWN:
                return "unknown";
        }
        return null;
    }

    /**
     * AP Channel bandwidth; one of {@link #CHANNEL_WIDTH_20MHZ}, {@link #CHANNEL_WIDTH_40MHZ},
     * {@link #CHANNEL_WIDTH_80MHZ}, {@link #CHANNEL_WIDTH_160MHZ}, {@link #CHANNEL_WIDTH_320MHZ},
     * or {@link #CHANNEL_WIDTH_80MHZ_PLUS_MHZ}, or {@link #CHANNEL_WIDTH_320MHZ}
     */
    public @ChannelWidth int channelWidth;

    /**
     * Not used if the AP bandwidth is 20 MHz
     * If the AP use 40, 80, 160 or 320MHz, this is the center frequency (in MHz)
     * if the AP use 80 + 80 MHz, this is the center frequency of the first segment (in MHz)
     */
    public int centerFreq0;

    /**
     * Only used if the AP bandwidth is 80 + 80 MHz
     * if the AP use 80 + 80 MHz, this is the center frequency of the second segment (in MHz)
     */
    public int centerFreq1;

    /**
     * @deprecated use is80211mcResponder() instead
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean is80211McRTTResponder;

    /**
     * timestamp in microseconds (since boot) when
     * this result was last seen.
     */
    public long timestamp;

    /**
     * Timestamp representing date when this result was last seen, in milliseconds from 1970
     * {@hide}
     */
    @UnsupportedAppUsage
    public long seen;

    /**
     * On devices with multiple hardware radio chains, this class provides metadata about
     * each radio chain that was used to receive this scan result (probe response or beacon).
     * {@hide}
     */
    public static class RadioChainInfo {
        /** Vendor defined id for a radio chain. */
        public int id;
        /** Detected signal level in dBm (also known as the RSSI) on this radio chain. */
        public int level;

        @Override
        public String toString() {
            return "RadioChainInfo: id=" + id + ", level=" + level;
        }

        @Override
        public boolean equals(Object otherObj) {
            if (this == otherObj) {
                return true;
            }
            if (!(otherObj instanceof RadioChainInfo)) {
                return false;
            }
            RadioChainInfo other = (RadioChainInfo) otherObj;
            return id == other.id && level == other.level;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, level);
        }
    };

    /**
     * Information about the list of the radio chains used to receive this scan result
     * (probe response or beacon).
     *
     * For Example: On devices with 2 hardware radio chains, this list could hold 1 or 2
     * entries based on whether this scan result was received using one or both the chains.
     * {@hide}
     */
    public RadioChainInfo[] radioChainInfos;

    /**
     * Status indicating the scan result does not correspond to a user's saved configuration
     * @hide
     * @removed
     */
    @SystemApi
    public boolean untrusted;

    /**
     * Number of time autojoin used it
     * @hide
     */
    @UnsupportedAppUsage
    public int numUsage;

    /**
     * The approximate distance to the AP in centimeter, if available.  Else
     * {@link #UNSPECIFIED}.
     * {@hide}
     */
    @UnsupportedAppUsage
    public int distanceCm;

    /**
     * The standard deviation of the distance to the access point, if available.
     * Else {@link #UNSPECIFIED}.
     * {@hide}
     */
    @UnsupportedAppUsage
    public int distanceSdCm;

    /** {@hide} */
    public static final long FLAG_PASSPOINT_NETWORK               = 0x0000000000000001;

    /** {@hide} */
    public static final long FLAG_80211mc_RESPONDER               = 0x0000000000000002;

    /** @hide */
    public static final long FLAG_80211az_NTB_RESPONDER           = 0x0000000000000004;

    /** @hide */
    public static final long FLAG_TWT_RESPONDER                    = 0x0000000000000008;

    /** @hide */
    public static final long FLAG_SECURE_HE_LTF_SUPPORTED           = 0x0000000000000010;

    /** @hide */
    public static final long FLAG_RANGING_FRAME_PROTECTION_REQUIRED = 0x0000000000000020;

    /*
     * These flags are specific to the ScanResult class, and are not related to the |flags|
     * field of the per-BSS scan results from WPA supplicant.
     */
    /**
     * Defines flags; such as {@link #FLAG_PASSPOINT_NETWORK}.
     * {@hide}
     */
    @UnsupportedAppUsage
    public long flags;

    /**
     * sets a flag in {@link #flags} field
     * @param flag flag to set
     * @hide
     */
    public void setFlag(long flag) {
        flags |= flag;
    }

    /**
     * clears a flag in {@link #flags} field
     * @param flag flag to set
     * @hide
     */
    public void clearFlag(long flag) {
        flags &= ~flag;
    }

    public boolean is80211mcResponder() {
        return (flags & FLAG_80211mc_RESPONDER) != 0;
    }

    /**
     * @return whether AP is a IEEE802.11az Non-Trigger based Ranging Responder.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public boolean is80211azNtbResponder() {
        return (flags & FLAG_80211az_NTB_RESPONDER) != 0;
    }

    public boolean isPasspointNetwork() {
        return (flags & FLAG_PASSPOINT_NETWORK) != 0;
    }

    /**
     * @return whether AP is Target Wake Time (TWT) Responder.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public boolean isTwtResponder() {
        return (flags & FLAG_TWT_RESPONDER) != 0;
    }

    /**
     * Returns whether the AP supports secure HE-LTF for ranging measurement.
     *
     * Secure HE-LTF (High Efficiency Long Training Field), is a security enhancement for the HE
     * PHY (High Efficiency Physical Layer) that aims to protect ranging measurements by using
     * randomized HE-LTF sequences. This prevents attackers from exploiting predictable HE-LTF
     * patterns. IEEE 802.11az adds secure HE-LTF support in Extended RSN capabilities.
     */
    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    public boolean isSecureHeLtfSupported() {
        return (flags & FLAG_SECURE_HE_LTF_SUPPORTED) != 0;
    }

    /**
     * Returns whether the AP requires frame protection for ranging measurement.
     *
     * Ranging frame protection in IEEE 802.11az, also known as URNM-MFPR (Unassociated Range
     * Negotiation and Measurement Management Frame Protection Required), is a security policy
     * that dictates whether ranging frames are required to be protected even without a formal
     * association between the Initiating STA (ISTA) and Responding STA (RSTA)
     */
    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    public boolean isRangingFrameProtectionRequired() {
        return (flags & FLAG_RANGING_FRAME_PROTECTION_REQUIRED) != 0;
    }


    /**
     * Indicates venue name (such as 'San Francisco Airport') published by access point; only
     * available on Passpoint network and if published by access point.
     * @deprecated - This information is not provided
     */
    @Deprecated
    public CharSequence venueName;

    /**
     * Indicates Passpoint operator name published by access point.
     * @deprecated - Use {@link WifiInfo#getPasspointProviderFriendlyName()}
     */
    @Deprecated
    public CharSequence operatorFriendlyName;

    /**
     * The unspecified value.
     */
    public final static int UNSPECIFIED = -1;

    /**
     * 2.4 GHz band first channel number
     * @hide
     */
    public static final int BAND_24_GHZ_FIRST_CH_NUM = 1;
    /**
     * 2.4 GHz band last channel number
     * @hide
     */
    public static final int BAND_24_GHZ_LAST_CH_NUM = 14;
    /**
     * 2.4 GHz band frequency of first channel in MHz
     * @hide
     */
    public static final int BAND_24_GHZ_START_FREQ_MHZ = 2412;
    /**
     * 2.4 GHz band frequency of last channel in MHz
     * @hide
     */
    public static final int BAND_24_GHZ_END_FREQ_MHZ = 2484;

    /**
     * 5 GHz band first channel number
     * @hide
     */
    public static final int BAND_5_GHZ_FIRST_CH_NUM = 32;
    /**
     * 5 GHz band last channel number
     * @hide
     */
    public static final int BAND_5_GHZ_LAST_CH_NUM = 177;
    /**
     * 5 GHz band frequency of first channel in MHz
     * @hide
     */
    public static final int BAND_5_GHZ_START_FREQ_MHZ = 5160;
    /**
     * 5 GHz band frequency of last channel in MHz
     * @hide
     */
    public static final int BAND_5_GHZ_END_FREQ_MHZ = 5885;

    /**
     * 6 GHz band first channel number
     * @hide
     */
    public static final int BAND_6_GHZ_FIRST_CH_NUM = 1;
    /**
     * 6 GHz band last channel number
     * @hide
     */
    public static final int BAND_6_GHZ_LAST_CH_NUM = 233;
    /**
     * 6 GHz band frequency of first channel in MHz
     * @hide
     */
    public static final int BAND_6_GHZ_START_FREQ_MHZ = 5955;
    /**
     * 6 GHz band frequency of last channel in MHz
     * @hide
     */
    public static final int BAND_6_GHZ_END_FREQ_MHZ = 7115;
    /**
     * The center frequency of the first 6Ghz preferred scanning channel, as defined by
     * IEEE802.11ax draft 7.0 section 26.17.2.3.3.
     * @hide
     */
    public static final int BAND_6_GHZ_PSC_START_MHZ = 5975;
    /**
     * The number of MHz to increment in order to get the next 6Ghz preferred scanning channel
     * as defined by IEEE802.11ax draft 7.0 section 26.17.2.3.3.
     * @hide
     */
    public static final int BAND_6_GHZ_PSC_STEP_SIZE_MHZ = 80;

    /**
     * 6 GHz band operating class 136 channel 2 center frequency in MHz
     * @hide
     */
    public static final int BAND_6_GHZ_OP_CLASS_136_CH_2_FREQ_MHZ = 5935;

    /**
     * 60 GHz band first channel number
     * @hide
     */
    public static final int BAND_60_GHZ_FIRST_CH_NUM = 1;
    /**
     * 60 GHz band last channel number
     * @hide
     */
    public static final int BAND_60_GHZ_LAST_CH_NUM = 6;
    /**
     * 60 GHz band frequency of first channel in MHz
     * @hide
     */
    public static final int BAND_60_GHZ_START_FREQ_MHZ = 58320;
    /**
     * 60 GHz band frequency of last channel in MHz
     * @hide
     */
    public static final int BAND_60_GHZ_END_FREQ_MHZ = 70200;
    /**
     * The highest frequency in 5GHz low
     * @hide
     */
    public static final int BAND_5_GHZ_LOW_HIGHEST_FREQ_MHZ = 5320;
    /**
     * The lowest frequency in 5GHz high
     * @hide
     */
    public static final int BAND_5_GHZ_HIGH_LOWEST_FREQ_MHZ = 5500;

    /**
     * Utility function to check if a frequency within 2.4 GHz band
     * @param freqMhz frequency in MHz
     * @return true if within 2.4GHz, false otherwise
     *
     * @hide
     */
    @Keep
    public static boolean is24GHz(int freqMhz) {
        return freqMhz >= BAND_24_GHZ_START_FREQ_MHZ && freqMhz <= BAND_24_GHZ_END_FREQ_MHZ;
    }

    /**
     * Utility function to check if a frequency within 5 GHz band
     * @param freqMhz frequency in MHz
     * @return true if within 5GHz, false otherwise
     *
     * @hide
     */
    @Keep
    public static boolean is5GHz(int freqMhz) {
        return freqMhz >=  BAND_5_GHZ_START_FREQ_MHZ && freqMhz <= BAND_5_GHZ_END_FREQ_MHZ;
    }

    /**
     * Utility function to check if a frequency within 6 GHz band
     * @param freqMhz
     * @return true if within 6GHz, false otherwise
     *
     * @hide
     */
    @Keep
    public static boolean is6GHz(int freqMhz) {
        if (freqMhz == BAND_6_GHZ_OP_CLASS_136_CH_2_FREQ_MHZ) {
            return true;
        }
        return (freqMhz >= BAND_6_GHZ_START_FREQ_MHZ && freqMhz <= BAND_6_GHZ_END_FREQ_MHZ);
    }

    /**
     * Utility function to check if a frequency is 6Ghz PSC channel.
     * @param freqMhz
     * @return true if the frequency is 6GHz PSC, false otherwise
     *
     * @hide
     */
    public static boolean is6GHzPsc(int freqMhz) {
        if (!ScanResult.is6GHz(freqMhz)) {
            return false;
        }
        return (freqMhz - BAND_6_GHZ_PSC_START_MHZ) % BAND_6_GHZ_PSC_STEP_SIZE_MHZ == 0;
    }

    /**
     * Utility function to check if a frequency within 60 GHz band
     * @param freqMhz
     * @return true if within 60GHz, false otherwise
     *
     * @hide
     */
    public static boolean is60GHz(int freqMhz) {
        return freqMhz >= BAND_60_GHZ_START_FREQ_MHZ && freqMhz <= BAND_60_GHZ_END_FREQ_MHZ;
    }

    /**
     * Utility function to check whether 2 frequencies are valid for multi-internet connection
     * when dual-5GHz is supported.
     *
     * The allowed combinations are:
     * - 2.4GHz + Any 5GHz
     * - 2.4GHz + 6Ghz
     * - 5GHz low + 5GHz high
     * - 5GHz low + 6GHz
     * @hide
     */
    public static boolean isValidCombinedBandForDual5GHz(int freqMhz1, int freqMhz2) {
        int band1 = toBand(freqMhz1);
        int band2 = toBand(freqMhz2);
        if (band1 == WIFI_BAND_24_GHZ || band2 == WIFI_BAND_24_GHZ) {
            return band1 != band2;
        }

        // 5GHz Low : b1 36-48 b2 52-64(5320)
        // 5GHz High : b3 100(5500)-144 b4 149-165
        if ((freqMhz1 <= BAND_5_GHZ_LOW_HIGHEST_FREQ_MHZ
                && freqMhz2 >= BAND_5_GHZ_HIGH_LOWEST_FREQ_MHZ)
                    || (freqMhz2 <= BAND_5_GHZ_LOW_HIGHEST_FREQ_MHZ
                        && freqMhz1 >= BAND_5_GHZ_HIGH_LOWEST_FREQ_MHZ)) {
            return true;
        }
        return false;
    }

    /**
     * Utility function to convert Wi-Fi channel number to frequency in MHz.
     *
     * Reference the Wi-Fi channel numbering and the channelization in IEEE 802.11-2016
     * specifications, section 17.3.8.4.2, 17.3.8.4.3 and Table 15-6.
     *
     * See also {@link #convertFrequencyMhzToChannelIfSupported(int)}.
     *
     * @param channel number to convert.
     * @param band of channel to convert. One of the following bands:
     *        {@link #WIFI_BAND_24_GHZ},  {@link #WIFI_BAND_5_GHZ},
     *        {@link #WIFI_BAND_6_GHZ},  {@link #WIFI_BAND_60_GHZ}.
     * @return center frequency in Mhz of the channel, {@link #UNSPECIFIED} if no match
     */
    public static int convertChannelToFrequencyMhzIfSupported(int channel, @WifiBand int band) {
        if (band == WIFI_BAND_24_GHZ) {
            // Special case
            if (channel == 14) {
                return 2484;
            } else if (channel >= BAND_24_GHZ_FIRST_CH_NUM && channel <= BAND_24_GHZ_LAST_CH_NUM) {
                return ((channel - BAND_24_GHZ_FIRST_CH_NUM) * 5) + BAND_24_GHZ_START_FREQ_MHZ;
            } else {
                return UNSPECIFIED;
            }
        }
        if (band == WIFI_BAND_5_GHZ) {
            if (channel >= BAND_5_GHZ_FIRST_CH_NUM && channel <= BAND_5_GHZ_LAST_CH_NUM) {
                return ((channel - BAND_5_GHZ_FIRST_CH_NUM) * 5) + BAND_5_GHZ_START_FREQ_MHZ;
            } else {
                return UNSPECIFIED;
            }
        }
        if (band == WIFI_BAND_6_GHZ) {
            if (channel >= BAND_6_GHZ_FIRST_CH_NUM && channel <= BAND_6_GHZ_LAST_CH_NUM) {
                if (channel == 2) {
                    return BAND_6_GHZ_OP_CLASS_136_CH_2_FREQ_MHZ;
                }
                return ((channel - BAND_6_GHZ_FIRST_CH_NUM) * 5) + BAND_6_GHZ_START_FREQ_MHZ;
            } else {
                return UNSPECIFIED;
            }
        }
        if (band == WIFI_BAND_60_GHZ) {
            if (channel >= BAND_60_GHZ_FIRST_CH_NUM && channel <= BAND_60_GHZ_LAST_CH_NUM) {
                return ((channel - BAND_60_GHZ_FIRST_CH_NUM) * 2160) + BAND_60_GHZ_START_FREQ_MHZ;
            } else {
                return UNSPECIFIED;
            }
        }
        return UNSPECIFIED;
    }

    /**
     * Utility function to convert Operating Class into a band
     *
     * Use 802.11 Specification Table E-4: Global Operating Classes for decoding
     *
     * @param opClass operating class
     * @param channel number
     *
     * @return one of {@link WifiScanner.WIFI_BAND_24_GHZ}, {@link WifiScanner.WIFI_BAND_5_GHZ}, or
     *         {@link WifiScanner.WIFI_BAND_6_GHZ} for a valid opClass, channel pair, otherwise
     *         {@link WifiScanner.WIFI_BAND_UNSPECIFIED} is returned.
     *
     * @hide
     */
    public static int getBandFromOpClass(int opClass, int channel) {
        if (opClass >= 81 && opClass <= 84) {
            if (channel >= BAND_24_GHZ_FIRST_CH_NUM && channel <= BAND_24_GHZ_LAST_CH_NUM) {
                return WifiScanner.WIFI_BAND_24_GHZ;
            }
        } else if (opClass >= 115 && opClass <= 130) {
            if (channel >= BAND_5_GHZ_FIRST_CH_NUM && channel <= BAND_5_GHZ_LAST_CH_NUM) {
                return WifiScanner.WIFI_BAND_5_GHZ;
            }
        } else if (opClass >= 131 && opClass <= 137) {
            if (channel >= BAND_6_GHZ_FIRST_CH_NUM && channel <= BAND_6_GHZ_LAST_CH_NUM) {
                return WifiScanner.WIFI_BAND_6_GHZ;
            }
        }

        // If none of the above combinations, then return as invalid band
        return WifiScanner.WIFI_BAND_UNSPECIFIED;
    }

    /**
     * Utility function to convert frequency in MHz to channel number.
     *
     * See also {@link #convertChannelToFrequencyMhzIfSupported(int, int)}.
     *
     * @param freqMhz frequency in MHz
     * @return channel number associated with given frequency, {@link #UNSPECIFIED} if no match
     */
    public static int convertFrequencyMhzToChannelIfSupported(int freqMhz) {
        // Special case
        if (freqMhz == 2484) {
            return 14;
        } else if (is24GHz(freqMhz)) {
            return (freqMhz - BAND_24_GHZ_START_FREQ_MHZ) / 5 + BAND_24_GHZ_FIRST_CH_NUM;
        } else if (is5GHz(freqMhz)) {
            return ((freqMhz - BAND_5_GHZ_START_FREQ_MHZ) / 5) + BAND_5_GHZ_FIRST_CH_NUM;
        } else if (is6GHz(freqMhz)) {
            if (freqMhz == BAND_6_GHZ_OP_CLASS_136_CH_2_FREQ_MHZ) {
                return 2;
            }
            return ((freqMhz - BAND_6_GHZ_START_FREQ_MHZ) / 5) + BAND_6_GHZ_FIRST_CH_NUM;
        } else if (is60GHz(freqMhz)) {
            return ((freqMhz - BAND_60_GHZ_START_FREQ_MHZ) / 2160) + BAND_60_GHZ_FIRST_CH_NUM;
        }

        return UNSPECIFIED;
    }

    /**
     * Returns the band for the ScanResult according to its frequency.
     * @hide
     */
    @WifiBand public static int toBand(int frequency) {
        if (ScanResult.is24GHz(frequency)) {
            return ScanResult.WIFI_BAND_24_GHZ;
        } else if (ScanResult.is5GHz(frequency)) {
            return ScanResult.WIFI_BAND_5_GHZ;
        } else if (ScanResult.is6GHz(frequency)) {
            return ScanResult.WIFI_BAND_6_GHZ;
        } else if (ScanResult.is60GHz(frequency)) {
            return ScanResult.WIFI_BAND_60_GHZ;
        }
        return ScanResult.UNSPECIFIED;
    }

    /**
     * Returns the band for the ScanResult according to its frequency.
     * @hide
     */
    @SystemApi
    @WifiBand public int getBand() {
        return ScanResult.toBand(this.frequency);
    }

    /**
     * @hide
     */
    public boolean is24GHz() {
        return ScanResult.is24GHz(frequency);
    }

    /**
     * @hide
     */
    public boolean is5GHz() {
        return ScanResult.is5GHz(frequency);
    }

    /**
     * @hide
     */
    public boolean is6GHz() {
        return ScanResult.is6GHz(frequency);
    }

    /**
     * @hide
     */
    public boolean is6GhzPsc() {
        return ScanResult.is6GHzPsc(frequency);
    }

    /**
     * @hide
     */
    public boolean is60GHz() {
        return ScanResult.is60GHz(frequency);
    }

    /**
     *  @hide
     * anqp lines from supplicant BSS response
     */
    @UnsupportedAppUsage
    public List<String> anqpLines;

    /**
     * information elements from beacon.
     */
    public static class InformationElement implements Parcelable {
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_SSID = 0;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_SUPPORTED_RATES = 1;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_TIM = 5;
        /** @hide */
        public static final int EID_COUNTRY = 7;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_BSS_LOAD = 11;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_ERP = 42;
        /** @hide */
        public static final int EID_HT_CAPABILITIES = 45;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_RSN = 48;
        /** @hide */
        public static final int EID_RSN_EXTENSION = 244;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_EXTENDED_SUPPORTED_RATES = 50;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_HT_OPERATION = 61;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_INTERWORKING = 107;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_ROAMING_CONSORTIUM = 111;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_EXTENDED_CAPS = 127;
        /** @hide */
        public static final int EID_VHT_CAPABILITIES = 191;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_VHT_OPERATION = 192;
        /** @hide */
        public static final int EID_RNR = 201;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final int EID_VSA = 221;
        /** @hide */
        public static final int EID_EXTENSION_PRESENT = 255;

        // Extension IDs
        /** @hide */
        public static final int EID_EXT_HE_CAPABILITIES = 35;
        /** @hide */
        public static final int EID_EXT_HE_OPERATION = 36;
        /**
         * EHT Operation IE extension id: see IEEE 802.11be Specification section 9.4.2.1
         *
         * @hide
         */
        public static final int EID_EXT_EHT_OPERATION = 106;
        /**
         * Multi-Link IE extension id: see IEEE 802.11be Specification section 9.4.2.1
         *
         * @hide
         */
        public static final int EID_EXT_MULTI_LINK = 107;
        /**
         * Multi-Link IE Fragment sub element ID: see IEEE 802.11be Specification section 9.4.2.312
         * Multi-Link element.
         *
         * @hide
         */
        public static final int EID_FRAGMENT_SUB_ELEMENT_MULTI_LINK = 254;

        /**
         * EHT Capabilities IE extension id: see IEEE 802.11be Specification section 9.4.2.1
         *
         * @hide
         */
        public static final int EID_EXT_EHT_CAPABILITIES = 108;

        /** @hide */
        @UnsupportedAppUsage
        public int id;
        /** @hide */
        public int idExt;

        /** @hide */
        @UnsupportedAppUsage
        public byte[] bytes;

        /** @hide */
        public InformationElement() {
        }

        /**
         * Constructs InformationElements from beacon.
         *
         * @param id element id
         * @param idExt element id extension
         * @param bytes the body of the information element, may contain multiple elements
         */
        public InformationElement(int id, int idExt, @NonNull byte[] bytes) {
            this.id = id;
            this.idExt = idExt;
            this.bytes = bytes.clone();
        }

        public InformationElement(@NonNull InformationElement rhs) {
            this.id = rhs.id;
            this.idExt = rhs.idExt;
            this.bytes = rhs.bytes.clone();
        }

        /**
         * The element ID of the information element. Defined in the IEEE 802.11-2016 spec
         * Table 9-77.
         */
        public int getId() {
            return id;
        }

        /**
         * The element ID Extension of the information element. Defined in the IEEE 802.11-2016 spec
         * Table 9-77.
         */
        public int getIdExt() {
            return idExt;
        }

        /**
         * Get the specific content of the information element.
         */
        @NonNull
        public ByteBuffer getBytes() {
            return ByteBuffer.wrap(bytes).asReadOnlyBuffer();
        }

        /** Implement the Parcelable interface {@hide} */
        public int describeContents() {
            return 0;
        }

        /** Implement the Parcelable interface {@hide} */
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(id);
            dest.writeInt(idExt);
            dest.writeByteArray(bytes);
        }

        /** Implement the Parcelable interface */
        public static final @NonNull Creator<InformationElement> CREATOR =
                new Creator<InformationElement>() {
                    public InformationElement createFromParcel(Parcel in) {
                        InformationElement informationElement = new InformationElement();
                        informationElement.id = in.readInt();
                        informationElement.idExt = in.readInt();
                        informationElement.bytes = in.createByteArray();
                        return informationElement;
                    }

                    public InformationElement[] newArray(int size) {
                        return new InformationElement[size];
                    }
                };

        @Override
        public boolean equals(Object that) {
            if (this == that) return true;

            // Potential API behavior change, so don't change behavior on older devices.
            if (!SdkLevel.isAtLeastS()) return false;

            if (!(that instanceof InformationElement)) return false;

            InformationElement thatIE = (InformationElement) that;
            return id == thatIE.id
                    && idExt == thatIE.idExt
                    && Arrays.equals(bytes, thatIE.bytes);
        }

        @Override
        public int hashCode() {
            // Potential API behavior change, so don't change behavior on older devices.
            if (!SdkLevel.isAtLeastS()) return System.identityHashCode(this);

            return Objects.hash(id, idExt, Arrays.hashCode(bytes));
        }
    }

    /**
     * information elements found in the beacon.
     * @hide
     */
    @UnsupportedAppUsage
    public InformationElement[] informationElements;
    /**
     * Get all information elements found in the beacon.
     */
    @NonNull
    public List<InformationElement> getInformationElements() {
        return Collections.unmodifiableList(Arrays.asList(informationElements));
    }

    /**
     * Get all the security types supported by this ScanResult.
     * @return array of {@code WifiInfo#SECURITY_TYPE_*}.
     */
    @NonNull
    public @WifiAnnotations.SecurityType int[] getSecurityTypes() {
        List<SecurityParams> params = ScanResultUtil.generateSecurityParamsListFromScanResult(this);
        int[] securityTypes = new int[params.size()];
        for (int i = 0; i < securityTypes.length; i++) {
            securityTypes[i] = WifiInfo.convertWifiConfigurationSecurityType(
                    params.get(i).getSecurityType());
        }
        return securityTypes;
    }

    /** ANQP response elements.
     * @hide
     */
    public AnqpInformationElement[] anqpElements;

    /**
     * Returns whether a WifiSsid represents a "hidden" SSID of all zero values.
     */
    private boolean isHiddenSsid(@NonNull WifiSsid wifiSsid) {
        for (byte b : wifiSsid.getBytes()) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builder class used to construct {@link ScanResult} objects.
     *
     * @hide
     */
    public static final class Builder {
        private WifiSsid mWifiSsid;
        private String mBssid;
        private long mHessid = 0;
        private int mAnqpDomainId = 0;
        private byte[] mOsuProviders = null;
        private String mCaps = null;
        private int mRssi = UNSPECIFIED;
        private int mFrequency = UNSPECIFIED;
        private long mTsf = 0;
        private int mDistanceCm = UNSPECIFIED;
        private int mDistanceSdCm = UNSPECIFIED;
        private @ChannelWidth int mChannelWidth = ScanResult.CHANNEL_WIDTH_20MHZ;
        private int mCenterFreq0 = UNSPECIFIED;
        private int mCenterFreq1 = UNSPECIFIED;
        private boolean mIs80211McRTTResponder = false;
        private boolean mIs80211azNtbRTTResponder = false;
        private boolean mIsTwtResponder = false;
        private boolean mIsSecureHeLtfSupported = false;
        private boolean mIsRangingFrameProtectionRequired = false;

        /** @hide */
        @NonNull
        public Builder setHessid(long hessid) {
            mHessid = hessid;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setOsuProviders(@Nullable byte[] osuProviders) {
            mOsuProviders = osuProviders;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setAnqpDomainId(int anqpDomainId) {
            mAnqpDomainId = anqpDomainId;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setCaps(@Nullable String caps) {
            mCaps = caps;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setRssi(int rssi) {
            mRssi = rssi;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setFrequency(int frequency) {
            mFrequency = frequency;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setTsf(long tsf) {
            mTsf = tsf;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setDistanceCm(int distanceCm) {
            mDistanceCm = distanceCm;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setDistanceSdCm(int distanceSdCm) {
            mDistanceSdCm = distanceSdCm;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setChannelWidth(@ChannelWidth int channelWidth) {
            mChannelWidth = channelWidth;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setCenterFreq0(int centerFreq0) {
            mCenterFreq0 = centerFreq0;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setCenterFreq1(int centerFreq1) {
            mCenterFreq1 = centerFreq1;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setIs80211McRTTResponder(boolean is80211McRTTResponder) {
            mIs80211McRTTResponder = is80211McRTTResponder;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setIs80211azNtbRTTResponder(boolean is80211azNtbRTTResponder) {
            mIs80211azNtbRTTResponder = is80211azNtbRTTResponder;
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setIsTwtResponder(boolean isTwtResponder) {
            mIsTwtResponder = isTwtResponder;
            return this;
        }

        /** @hide */
        public Builder(WifiSsid wifiSsid, String bssid) {
            mWifiSsid = wifiSsid;
            mBssid = bssid;
        }

        /**
         * @hide
         *
         */
        public Builder() {

        }

        /**
         * @hide
         */
        public Builder setWifiSsid(WifiSsid wifiSsid) {
            mWifiSsid = wifiSsid;
            return this;
        }

        /**
         * @hide
         */
        public Builder setBssid(String bssid) {
            mBssid = bssid;
            return this;
        }

        /**
         * @hide
         */
        public void clear() {
            mWifiSsid = null;
            mBssid = null;
            mHessid = 0;
            mAnqpDomainId = 0;
            mOsuProviders = null;
            mCaps = null;
            mRssi = UNSPECIFIED;
            mFrequency = UNSPECIFIED;
            mTsf = 0;
            mDistanceCm = UNSPECIFIED;
            mDistanceSdCm = UNSPECIFIED;
            mChannelWidth = ScanResult.CHANNEL_WIDTH_20MHZ;
            mCenterFreq0 = UNSPECIFIED;
            mCenterFreq1 = UNSPECIFIED;
            mIs80211McRTTResponder = false;
            mIs80211azNtbRTTResponder = false;
            mIsTwtResponder = false;
        }

        /** @hide */
        public ScanResult build() {
            return new ScanResult(this);
        }

        /** @hide */
        public Builder setSecureHeLtfSupported(boolean supported) {
            mIsSecureHeLtfSupported = supported;
            return this;
        }

        /** @hide */
        public Builder setRangingFrameProtectionRequired(boolean required) {
            mIsRangingFrameProtectionRequired = required;
            return this;
        }
    }

    /**
     * @hide
     */
    private ScanResult(Builder builder) {
        this.wifiSsid = builder.mWifiSsid;
        if (wifiSsid != null && isHiddenSsid(wifiSsid)) {
            // Retain the legacy behavior of setting SSID to "" if the SSID is all zero values.
            this.SSID = "";
        } else {
            final CharSequence utf8Ssid = (wifiSsid != null) ? wifiSsid.getUtf8Text() : null;
            this.SSID = (utf8Ssid != null) ? utf8Ssid.toString() : WifiManager.UNKNOWN_SSID;
        }
        this.BSSID = builder.mBssid;
        this.hessid = builder.mHessid;
        this.anqpDomainId = builder.mAnqpDomainId;
        if (builder.mOsuProviders != null) {
            this.anqpElements = new AnqpInformationElement[1];
            this.anqpElements[0] = new AnqpInformationElement(
                    AnqpInformationElement.HOTSPOT20_VENDOR_ID,
                    AnqpInformationElement.HS_OSU_PROVIDERS, builder.mOsuProviders);
        }
        this.capabilities = builder.mCaps;
        this.level = builder.mRssi;
        this.frequency = builder.mFrequency;
        this.timestamp = builder.mTsf;
        this.distanceCm = builder.mDistanceCm;
        this.distanceSdCm = builder.mDistanceSdCm;
        this.channelWidth = builder.mChannelWidth;
        this.centerFreq0 = builder.mCenterFreq0;
        this.centerFreq1 = builder.mCenterFreq1;
        this.flags = 0;
        this.flags |= (builder.mIs80211McRTTResponder) ? FLAG_80211mc_RESPONDER : 0;
        this.flags |= (builder.mIs80211azNtbRTTResponder) ? FLAG_80211az_NTB_RESPONDER : 0;
        this.flags |= (builder.mIsTwtResponder) ? FLAG_TWT_RESPONDER : 0;
        this.radioChainInfos = null;
        this.mApMldMacAddress = null;
        this.flags |= (builder.mIsSecureHeLtfSupported) ? FLAG_SECURE_HE_LTF_SUPPORTED : 0;
        this.flags |= (builder.mIsRangingFrameProtectionRequired)
                ? FLAG_RANGING_FRAME_PROTECTION_REQUIRED : 0;
    }

    /**
     * @hide
     * @deprecated Use {@link ScanResult.Builder}
     */
    public ScanResult(WifiSsid wifiSsid, String BSSID, long hessid, int anqpDomainId,
            byte[] osuProviders, String caps, int level, int frequency, long tsf) {
        this.wifiSsid = wifiSsid;
        if (wifiSsid != null && isHiddenSsid(wifiSsid)) {
            // Retain the legacy behavior of setting SSID to "" if the SSID is all zero values.
            this.SSID = "";
        } else {
            final CharSequence utf8Ssid = (wifiSsid != null) ? wifiSsid.getUtf8Text() : null;
            this.SSID = (utf8Ssid != null) ? utf8Ssid.toString() : WifiManager.UNKNOWN_SSID;
        }
        this.BSSID = BSSID;
        this.hessid = hessid;
        this.anqpDomainId = anqpDomainId;
        if (osuProviders != null) {
            this.anqpElements = new AnqpInformationElement[1];
            this.anqpElements[0] =
                    new AnqpInformationElement(AnqpInformationElement.HOTSPOT20_VENDOR_ID,
                            AnqpInformationElement.HS_OSU_PROVIDERS, osuProviders);
        }
        this.capabilities = caps;
        this.level = level;
        this.frequency = frequency;
        this.timestamp = tsf;
        this.distanceCm = UNSPECIFIED;
        this.distanceSdCm = UNSPECIFIED;
        this.channelWidth = UNSPECIFIED;
        this.centerFreq0 = UNSPECIFIED;
        this.centerFreq1 = UNSPECIFIED;
        this.flags = 0;
        this.radioChainInfos = null;
        this.mApMldMacAddress = null;
    }

    /**
     * @hide
     * @deprecated Use {@link ScanResult.Builder}
     */
    public ScanResult(WifiSsid wifiSsid, String BSSID, String caps, int level, int frequency,
            long tsf, int distCm, int distSdCm) {
        this.wifiSsid = wifiSsid;
        if (wifiSsid != null && isHiddenSsid(wifiSsid)) {
            // Retain the legacy behavior of setting SSID to "" if the SSID is all zero values.
            this.SSID = "";
        } else {
            final CharSequence utf8Ssid = (wifiSsid != null) ? wifiSsid.getUtf8Text() : null;
            this.SSID = (utf8Ssid != null) ? utf8Ssid.toString() : WifiManager.UNKNOWN_SSID;
        }
        this.BSSID = BSSID;
        this.capabilities = caps;
        this.level = level;
        this.frequency = frequency;
        this.timestamp = tsf;
        this.distanceCm = distCm;
        this.distanceSdCm = distSdCm;
        this.channelWidth = UNSPECIFIED;
        this.centerFreq0 = UNSPECIFIED;
        this.centerFreq1 = UNSPECIFIED;
        this.flags = 0;
        this.radioChainInfos = null;
        this.mApMldMacAddress = null;
    }

    /**
     * @hide
     * @deprecated Use {@link ScanResult.Builder}
     */
    public ScanResult(String Ssid, String BSSID, long hessid, int anqpDomainId, String caps,
            int level, int frequency,
            long tsf, int distCm, int distSdCm, int channelWidth, int centerFreq0, int centerFreq1,
            boolean is80211McRTTResponder) {
        this.SSID = Ssid;
        this.BSSID = BSSID;
        this.hessid = hessid;
        this.anqpDomainId = anqpDomainId;
        this.capabilities = caps;
        this.level = level;
        this.frequency = frequency;
        this.timestamp = tsf;
        this.distanceCm = distCm;
        this.distanceSdCm = distSdCm;
        this.channelWidth = channelWidth;
        this.centerFreq0 = centerFreq0;
        this.centerFreq1 = centerFreq1;
        if (is80211McRTTResponder) {
            this.flags = FLAG_80211mc_RESPONDER;
        } else {
            this.flags = 0;
        }
        this.radioChainInfos = null;
        this.mApMldMacAddress = null;
    }

    /**
     * @hide
     * @deprecated Use {@link ScanResult.Builder}
     */
    public ScanResult(WifiSsid wifiSsid, String Ssid, String BSSID, long hessid, int anqpDomainId,
                  String caps, int level,
                  int frequency, long tsf, int distCm, int distSdCm, int channelWidth,
                  int centerFreq0, int centerFreq1, boolean is80211McRTTResponder) {
        this(Ssid, BSSID, hessid, anqpDomainId, caps, level, frequency, tsf, distCm,
                distSdCm, channelWidth, centerFreq0, centerFreq1, is80211McRTTResponder);
        this.wifiSsid = wifiSsid;
    }

    /** copy constructor */
    public ScanResult(@NonNull ScanResult source) {
        if (source != null) {
            wifiSsid = source.wifiSsid;
            SSID = source.SSID;
            BSSID = source.BSSID;
            hessid = source.hessid;
            anqpDomainId = source.anqpDomainId;
            informationElements = source.informationElements;
            anqpElements = source.anqpElements;
            capabilities = source.capabilities;
            level = source.level;
            frequency = source.frequency;
            channelWidth = source.channelWidth;
            centerFreq0 = source.centerFreq0;
            centerFreq1 = source.centerFreq1;
            timestamp = source.timestamp;
            distanceCm = source.distanceCm;
            distanceSdCm = source.distanceSdCm;
            seen = source.seen;
            untrusted = source.untrusted;
            numUsage = source.numUsage;
            venueName = source.venueName;
            operatorFriendlyName = source.operatorFriendlyName;
            flags = source.flags;
            radioChainInfos = source.radioChainInfos;
            this.mWifiStandard = source.mWifiStandard;
            this.ifaceName = source.ifaceName;
            this.mApMldMacAddress = source.mApMldMacAddress;
            this.mApMloLinkId = source.mApMloLinkId;
            this.mAffiliatedMloLinks = source.mAffiliatedMloLinks != null
                    ? new ArrayList<>(source.mAffiliatedMloLinks) : Collections.emptyList();
        }
    }

    /** Construct an empty scan result. */
    public ScanResult() {
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String none = "<none>";

        sb.append("SSID: ")
                .append(wifiSsid == null ? WifiManager.UNKNOWN_SSID : wifiSsid)
                .append(", BSSID: ")
                .append(BSSID == null ? none : BSSID)
                .append(", capabilities: ")
                .append(capabilities == null ? none : capabilities)
                .append(", level: ")
                .append(level)
                .append(", frequency: ")
                .append(frequency)
                .append(", timestamp: ")
                .append(timestamp);
        sb.append(", distance: ").append((distanceCm != UNSPECIFIED ? distanceCm : "?")).
                append("(cm)");
        sb.append(", distanceSd: ").append((distanceSdCm != UNSPECIFIED ? distanceSdCm : "?")).
                append("(cm)");

        sb.append(", passpoint: ");
        sb.append(((flags & FLAG_PASSPOINT_NETWORK) != 0) ? "yes" : "no");
        sb.append(", ChannelBandwidth: ").append(channelWidth);
        sb.append(", centerFreq0: ").append(centerFreq0);
        sb.append(", centerFreq1: ").append(centerFreq1);
        sb.append(", standard: ").append(wifiStandardToString(mWifiStandard));
        sb.append(", 80211mcResponder: ");
        sb.append(((flags & FLAG_80211mc_RESPONDER) != 0) ? "is supported" : "is not supported");
        sb.append(", 80211azNtbResponder: ");
        sb.append(
                ((flags & FLAG_80211az_NTB_RESPONDER) != 0) ? "is supported" : "is not supported");
        sb.append(", TWT Responder: ");
        sb.append(((flags & FLAG_TWT_RESPONDER) != 0) ? "yes" : "no");
        sb.append(", Radio Chain Infos: ").append(Arrays.toString(radioChainInfos));
        sb.append(", interface name: ").append(ifaceName);

        if (mApMldMacAddress != null) {
            sb.append(", MLO Info: ")
                    .append(" AP MLD MAC Address: ")
                    .append(mApMldMacAddress.toString())
                    .append(", AP MLO Link-Id: ")
                    .append((mApMloLinkId == MloLink.INVALID_MLO_LINK_ID)
                            ? "Unspecified" : mApMloLinkId)
                    .append(", AP MLO Affiliated Links: ").append(mAffiliatedMloLinks);
        }

        return sb.toString();
    }

    /** Implement the Parcelable interface {@hide} */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface {@hide} */
    public void writeToParcel(Parcel dest, int flags) {
        long start = dest.dataSize();
        if (wifiSsid != null) {
            dest.writeInt(1);
            wifiSsid.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeString(SSID);
        dest.writeString(BSSID);
        dest.writeLong(hessid);
        dest.writeInt(anqpDomainId);
        dest.writeString(capabilities);
        dest.writeInt(level);
        dest.writeInt(frequency);
        dest.writeLong(timestamp);
        dest.writeInt(distanceCm);
        dest.writeInt(distanceSdCm);
        dest.writeInt(channelWidth);
        dest.writeInt(centerFreq0);
        dest.writeInt(centerFreq1);
        dest.writeInt(mWifiStandard);
        dest.writeLong(seen);
        dest.writeInt(untrusted ? 1 : 0);
        dest.writeInt(numUsage);
        dest.writeString((venueName != null) ? venueName.toString() : "");
        dest.writeString((operatorFriendlyName != null) ? operatorFriendlyName.toString() : "");
        dest.writeLong(this.flags);
        dest.writeTypedArray(informationElements, flags);

        if (anqpLines != null) {
            dest.writeInt(anqpLines.size());
            for (int i = 0; i < anqpLines.size(); i++) {
                dest.writeString(anqpLines.get(i));
            }
        }
        else {
            dest.writeInt(0);
        }
        int anqpElementsPayloadSize = 0;
        if (anqpElements != null) {
            dest.writeInt(anqpElements.length);
            for (AnqpInformationElement element : anqpElements) {
                dest.writeInt(element.getVendorId());
                dest.writeInt(element.getElementId());
                dest.writeInt(element.getPayload().length);
                dest.writeByteArray(element.getPayload());
                anqpElementsPayloadSize += element.getPayload().length;
            }
        } else {
            dest.writeInt(0);
        }

        if (radioChainInfos != null) {
            dest.writeInt(radioChainInfos.length);
            for (int i = 0; i < radioChainInfos.length; i++) {
                dest.writeInt(radioChainInfos[i].id);
                dest.writeInt(radioChainInfos[i].level);
            }
        } else {
            dest.writeInt(0);
        }
        dest.writeString((ifaceName != null) ? ifaceName.toString() : "");


        // Add MLO related attributes
        dest.writeParcelable(mApMldMacAddress, flags);
        dest.writeInt(mApMloLinkId);
        dest.writeTypedList(mAffiliatedMloLinks);
        if (dest.dataSize() - start > 10000) {
            Log.e(
                    TAG,
                    " Abnormal ScanResult: "
                            + this
                            + ". The size is "
                            + (dest.dataSize() - start)
                            + ". The informationElements size is "
                            + informationElements.length
                            + ". The anqpPayload size is "
                            + anqpElementsPayloadSize);
        }
    }

    /** Implement the Parcelable interface */
    public static final @NonNull Creator<ScanResult> CREATOR =
            new Creator<ScanResult>() {
                public ScanResult createFromParcel(Parcel in) {
                    WifiSsid wifiSsid = null;
                    if (in.readInt() == 1) {
                        wifiSsid = WifiSsid.CREATOR.createFromParcel(in);
                    }
                    ScanResult sr = new ScanResult(
                            wifiSsid,
                            in.readString(),                    /* SSID  */
                            in.readString(),                    /* BSSID */
                            in.readLong(),                      /* HESSID */
                            in.readInt(),                       /* ANQP Domain ID */
                            in.readString(),                    /* capabilities */
                            in.readInt(),                       /* level */
                            in.readInt(),                       /* frequency */
                            in.readLong(),                      /* timestamp */
                            in.readInt(),                       /* distanceCm */
                            in.readInt(),                       /* distanceSdCm */
                            in.readInt(),                       /* channelWidth */
                            in.readInt(),                       /* centerFreq0 */
                            in.readInt(),                       /* centerFreq1 */
                            false                               /* rtt responder,
                                                               fixed with flags below */
                    );

                    sr.mWifiStandard = in.readInt();
                    sr.seen = in.readLong();
                    sr.untrusted = in.readInt() != 0;
                    sr.numUsage = in.readInt();
                    sr.venueName = in.readString();
                    sr.operatorFriendlyName = in.readString();
                    sr.flags = in.readLong();
                    sr.informationElements = in.createTypedArray(InformationElement.CREATOR);

                    int n = in.readInt();
                    if (n != 0) {
                        sr.anqpLines = new ArrayList<String>();
                        for (int i = 0; i < n; i++) {
                            sr.anqpLines.add(in.readString());
                        }
                    }
                    n = in.readInt();
                    if (n != 0) {
                        sr.anqpElements = new AnqpInformationElement[n];
                        for (int i = 0; i < n; i++) {
                            int vendorId = in.readInt();
                            int elementId = in.readInt();
                            int len = in.readInt();
                            byte[] payload = new byte[len];
                            in.readByteArray(payload);
                            sr.anqpElements[i] =
                                    new AnqpInformationElement(vendorId, elementId, payload);
                        }
                    }
                    n = in.readInt();
                    if (n != 0) {
                        sr.radioChainInfos = new RadioChainInfo[n];
                        for (int i = 0; i < n; i++) {
                            sr.radioChainInfos[i] = new RadioChainInfo();
                            sr.radioChainInfos[i].id = in.readInt();
                            sr.radioChainInfos[i].level = in.readInt();
                        }
                    }
                    sr.ifaceName = in.readString();

                    // Read MLO related attributes
                    sr.mApMldMacAddress = in.readParcelable(MacAddress.class.getClassLoader());
                    sr.mApMloLinkId = in.readInt();
                    sr.mAffiliatedMloLinks = in.createTypedArrayList(MloLink.CREATOR);

                    return sr;
                }
            public ScanResult[] newArray(int size) {
                return new ScanResult[size];
            }
        };
}

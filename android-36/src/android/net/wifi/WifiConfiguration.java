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
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.pm.PackageManager;
import android.net.IpConfiguration;
import android.net.IpConfiguration.ProxySettings;
import android.net.MacAddress;
import android.net.NetworkSpecifier;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;

import com.android.internal.annotations.VisibleForTesting;
import com.android.modules.utils.build.SdkLevel;
import com.android.net.module.util.MacAddressUtils;
import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A class representing a configured Wi-Fi network, including the
 * security configuration.
 *
 * @deprecated Use {@link WifiNetworkSpecifier.Builder} to create {@link NetworkSpecifier} and
 * {@link WifiNetworkSuggestion.Builder} to create {@link WifiNetworkSuggestion}. This class can
 * still be used with privileged APIs such as
 * {@link WifiManager#addNetwork(WifiConfiguration)}.
 */
@Deprecated
public class WifiConfiguration implements Parcelable {
    private static final String TAG = "WifiConfiguration";
    /**
     * Current Version of the Backup Serializer.
    */
    private static final int BACKUP_VERSION = 3;
    /** {@hide} */
    public static final String ssidVarName = "ssid";
    /** {@hide} */
    public static final String bssidVarName = "bssid";
    /** {@hide} */
    public static final String pskVarName = "psk";
    /** {@hide} */
    @Deprecated @UnsupportedAppUsage
    public static final String[] wepKeyVarNames = {"wep_key0", "wep_key1", "wep_key2", "wep_key3"};
    /** {@hide} */
    @Deprecated
    public static final String wepTxKeyIdxVarName = "wep_tx_keyidx";
    /** {@hide} */
    public static final String priorityVarName = "priority";
    /** {@hide} */
    public static final String hiddenSSIDVarName = "scan_ssid";
    /** {@hide} */
    public static final String pmfVarName = "ieee80211w";
    /** {@hide} */
    public static final String updateIdentiferVarName = "update_identifier";
    /**
     * The network ID for an invalid network.
     *
     * @hide
     */
    @SystemApi
    public static final int INVALID_NETWORK_ID = -1;
    /** {@hide} */
    public static final int LOCAL_ONLY_NETWORK_ID = -2;

    /** {@hide} */
    private String mPasspointManagementObjectTree;
    /** {@hide} */
    private static final int MAXIMUM_RANDOM_MAC_GENERATION_RETRY = 3;

    /**
     * Recognized key management schemes.
     */
    public static class KeyMgmt {
        private KeyMgmt() { }

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                NONE,
                WPA_PSK,
                WPA_EAP,
                IEEE8021X,
                WPA2_PSK,
                OSEN,
                FT_PSK,
                FT_EAP,
                SAE,
                OWE,
                SUITE_B_192,
                WPA_PSK_SHA256,
                WPA_EAP_SHA256,
                WAPI_PSK,
                WAPI_CERT,
                FILS_SHA256,
                FILS_SHA384,
                DPP})
        public @interface KeyMgmtScheme {}

        /** WPA is not used; plaintext or static WEP could be used. */
        public static final int NONE = 0;
        /** WPA pre-shared key (requires {@code preSharedKey} to be specified). */
        public static final int WPA_PSK = 1;
        /** WPA using EAP authentication. Generally used with an external authentication server. */
        public static final int WPA_EAP = 2;
        /**
         * IEEE 802.1X using EAP authentication and (optionally) dynamically
         * generated WEP keys.
         */
        public static final int IEEE8021X = 3;

        /**
         * WPA2 pre-shared key for use with soft access point
         * (requires {@code preSharedKey} to be specified).
         */
        public static final int WPA2_PSK = 4;
        /**
         * Hotspot 2.0 r2 OSEN:
         */
        public static final int OSEN = 5;

        /**
         * IEEE 802.11r Fast BSS Transition with PSK authentication.
         */
        public static final int FT_PSK = 6;

        /**
         * IEEE 802.11r Fast BSS Transition with EAP authentication.
         */
        public static final int FT_EAP = 7;

        /**
         * Simultaneous Authentication of Equals
         */
        public static final int SAE = 8;

        /**
         * Opportunististic Wireless Encryption
         */
        public static final int OWE = 9;

        /**
         * SUITE_B_192 192 bit level
         */
        public static final int SUITE_B_192 = 10;

        /**
         * WPA pre-shared key with stronger SHA256-based algorithms.
         */
        public static final int WPA_PSK_SHA256 = 11;

        /**
         * WPA using EAP authentication with stronger SHA256-based algorithms.
         */
        public static final int WPA_EAP_SHA256 = 12;

        /**
         * WAPI pre-shared key (requires {@code preSharedKey} to be specified).
         */
        public static final int WAPI_PSK = 13;

        /**
         * WAPI certificate to be specified.
         */
        public static final int WAPI_CERT = 14;

        /**
        * IEEE 802.11ai FILS SK with SHA256
        */
        public static final int FILS_SHA256 = 15;
        /**
         * IEEE 802.11ai FILS SK with SHA384:
         */
        public static final int FILS_SHA384 = 16;

        /**
         * Easy Connect - AKA Device Provisioning Protocol (DPP)
         * For more details, visit <a href="https://www.wi-fi.org/">https://www.wi-fi.org/</a> and
         * search for "Easy Connect" or "Device Provisioning Protocol specification".
         */
        public static final int DPP = 17;

        public static final String varName = "key_mgmt";

        public static final String[] strings = { "NONE", "WPA_PSK", "WPA_EAP",
                "IEEE8021X", "WPA2_PSK", "OSEN", "FT_PSK", "FT_EAP",
                "SAE", "OWE", "SUITE_B_192", "WPA_PSK_SHA256", "WPA_EAP_SHA256",
                "WAPI_PSK", "WAPI_CERT", "FILS_SHA256", "FILS_SHA384", "DPP" };
    }

    /**
     * Recognized security protocols.
     */
    public static class Protocol {
        private Protocol() { }

        /** WPA/IEEE 802.11i/D3.0
         * @deprecated Due to security and performance limitations, use of WPA-1 networks
         * is discouraged. WPA-2 (RSN) should be used instead. */
        @Deprecated
        public static final int WPA = 0;
        /** RSN WPA2/WPA3/IEEE 802.11i */
        public static final int RSN = 1;
        /** HS2.0 r2 OSEN
         * @hide
         */
        public static final int OSEN = 2;

        /**
         * WAPI Protocol
         */
        public static final int WAPI = 3;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {WPA, RSN, OSEN, WAPI})
        public @interface ProtocolScheme {};

        public static final String varName = "proto";

        public static final String[] strings = { "WPA", "RSN", "OSEN", "WAPI" };
    }

    /**
     * Recognized IEEE 802.11 authentication algorithms.
     */
    public static class AuthAlgorithm {
        private AuthAlgorithm() { }

        /** Open System authentication (required for WPA/WPA2) */
        public static final int OPEN = 0;
        /** Shared Key authentication (requires static WEP keys)
         * @deprecated Due to security and performance limitations, use of WEP networks
         * is discouraged. */
        @Deprecated
        public static final int SHARED = 1;
        /** LEAP/Network EAP (only used with LEAP) */
        public static final int LEAP = 2;

        /** SAE (Used only for WPA3-Personal) */
        public static final int SAE = 3;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {OPEN, SHARED, LEAP, SAE})
        public @interface AuthAlgorithmScheme {};

        public static final String varName = "auth_alg";

        public static final String[] strings = { "OPEN", "SHARED", "LEAP", "SAE" };
    }

    /**
     * Recognized pairwise ciphers for WPA.
     */
    public static class PairwiseCipher {
        private PairwiseCipher() { }

        /** Use only Group keys (deprecated) */
        public static final int NONE = 0;
        /** Temporal Key Integrity Protocol [IEEE 802.11i/D7.0]
         * @deprecated Due to security and performance limitations, use of WPA-1 networks
         * is discouraged. WPA-2 (RSN) should be used instead. */
        @Deprecated
        public static final int TKIP = 1;
        /** AES in Counter mode with CBC-MAC [RFC 3610, IEEE 802.11i/D7.0] */
        public static final int CCMP = 2;
        /**
         * AES in Galois/Counter Mode
         */
        public static final int GCMP_256 = 3;
        /**
         * SMS4 cipher for WAPI
         */
        public static final int SMS4 = 4;

        /**
         * AES in Galois/Counter Mode with a 128-bit integrity key
         */
        public static final int GCMP_128 = 5;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {NONE, TKIP, CCMP, GCMP_256, SMS4, GCMP_128})
        public @interface PairwiseCipherScheme {};

        public static final String varName = "pairwise";

        public static final String[] strings = { "NONE", "TKIP", "CCMP", "GCMP_256", "SMS4",
                "GCMP_128" };
    }

    /**
     * Recognized group ciphers.
     * <pre>
     * CCMP = AES in Counter mode with CBC-MAC [RFC 3610, IEEE 802.11i/D7.0]
     * TKIP = Temporal Key Integrity Protocol [IEEE 802.11i/D7.0]
     * WEP104 = WEP (Wired Equivalent Privacy) with 104-bit key
     * WEP40 = WEP (Wired Equivalent Privacy) with 40-bit key (original 802.11)
     * GCMP_256 = AES in Galois/Counter Mode
     * </pre>
     */
    public static class GroupCipher {
        private GroupCipher() { }

        /** WEP40 = WEP (Wired Equivalent Privacy) with 40-bit key (original 802.11)
         * @deprecated Due to security and performance limitations, use of WEP networks
         * is discouraged. */
        @Deprecated
        public static final int WEP40 = 0;
        /** WEP104 = WEP (Wired Equivalent Privacy) with 104-bit key
         * @deprecated Due to security and performance limitations, use of WEP networks
         * is discouraged. */
        @Deprecated
        public static final int WEP104 = 1;
        /** Temporal Key Integrity Protocol [IEEE 802.11i/D7.0] */
        public static final int TKIP = 2;
        /** AES in Counter mode with CBC-MAC [RFC 3610, IEEE 802.11i/D7.0] */
        public static final int CCMP = 3;
        /** Hotspot 2.0 r2 OSEN
         * @hide
         */
        public static final int GTK_NOT_USED = 4;
        /**
         * AES in Galois/Counter Mode
         */
        public static final int GCMP_256 = 5;
        /**
         * SMS4 cipher for WAPI
         */
        public static final int SMS4 = 6;
        /**
         * AES in Galois/Counter Mode with a 128-bit integrity key
         */
        public static final int GCMP_128 = 7;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {WEP40, WEP104, TKIP, CCMP, GTK_NOT_USED, GCMP_256, SMS4, GCMP_128})
        public @interface GroupCipherScheme {};

        public static final String varName = "group";

        public static final String[] strings =
                { /* deprecated */ "WEP40", /* deprecated */ "WEP104",
                        "TKIP", "CCMP", "GTK_NOT_USED", "GCMP_256",
                        "SMS4", "GCMP_128" };
    }

    /**
     * Recognized group management ciphers.
     * <pre>
     * BIP_CMAC_256 = Cipher-based Message Authentication Code 256 bits
     * BIP_GMAC_128 = Galois Message Authentication Code 128 bits
     * BIP_GMAC_256 = Galois Message Authentication Code 256 bits
     * </pre>
     */
    public static class GroupMgmtCipher {
        private GroupMgmtCipher() { }

        /** CMAC-256 = Cipher-based Message Authentication Code */
        public static final int BIP_CMAC_256 = 0;

        /** GMAC-128 = Galois Message Authentication Code */
        public static final int BIP_GMAC_128 = 1;

        /** GMAC-256 = Galois Message Authentication Code */
        public static final int BIP_GMAC_256 = 2;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {BIP_CMAC_256, BIP_GMAC_128, BIP_GMAC_256})
        public @interface GroupMgmtCipherScheme {};

        private static final String varName = "groupMgmt";

        /** @hide */
        @SuppressLint("AllUpper")
        public static final @NonNull String[] strings = { "BIP_CMAC_256",
                "BIP_GMAC_128", "BIP_GMAC_256"};
    }

    /**
     * Recognized suiteB ciphers.
     * <pre>
     * ECDHE_ECDSA
     * ECDHE_RSA
     * </pre>
     * @hide
     */
    public static class SuiteBCipher {
        private SuiteBCipher() { }

        /** Diffie-Hellman with Elliptic Curve_ECDSA signature */
        public static final int ECDHE_ECDSA = 0;

        /** Diffie-Hellman with_RSA signature */
        public static final int ECDHE_RSA = 1;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {ECDHE_ECDSA, ECDHE_RSA})
        public @interface SuiteBCipherScheme {};

        private static final String varName = "SuiteB";

        /** @hide */
        @SuppressLint("AllUpper")
        public static final String[] strings = { "ECDHE_ECDSA", "ECDHE_RSA" };
    }

    /** Possible status of a network configuration. */
    public static class Status {
        private Status() { }

        /** this is the network we are currently connected to */
        public static final int CURRENT = 0;
        /** supplicant will not attempt to use this network */
        public static final int DISABLED = 1;
        /** supplicant will consider this network available for association */
        public static final int ENABLED = 2;

        public static final String[] strings = { "current", "disabled", "enabled" };
    }

    /** Security type for an open network. */
    public static final int SECURITY_TYPE_OPEN = 0;
    /** Security type for a WEP network. */
    public static final int SECURITY_TYPE_WEP = 1;
    /** Security type for a PSK network. */
    public static final int SECURITY_TYPE_PSK = 2;
    /** Security type for an EAP network. */
    public static final int SECURITY_TYPE_EAP = 3;
    /** Security type for an SAE network. */
    public static final int SECURITY_TYPE_SAE = 4;
    /**
     * Security type for a WPA3-Enterprise in 192-bit security network.
     * This is the same as {@link #SECURITY_TYPE_EAP_SUITE_B} and uses the same value.
     */
    public static final int SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT = 5;
    /**
     * Security type for a WPA3-Enterprise in 192-bit security network.
     * @deprecated Use the {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT} constant
     * (which is the same value).
     */
    @Deprecated
    public static final int SECURITY_TYPE_EAP_SUITE_B =
            SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT;
    /** Security type for an OWE network. */
    public static final int SECURITY_TYPE_OWE = 6;
    /** Security type for a WAPI PSK network. */
    public static final int SECURITY_TYPE_WAPI_PSK = 7;
    /** Security type for a WAPI Certificate network. */
    public static final int SECURITY_TYPE_WAPI_CERT = 8;
    /** Security type for a WPA3-Enterprise network. */
    public static final int SECURITY_TYPE_EAP_WPA3_ENTERPRISE = 9;
    /**
     * Security type for an OSEN network.
     * @hide
     */
    public static final int SECURITY_TYPE_OSEN = 10;
    /**
     * Security type for a Passpoint R1/R2 network.
     * Passpoint R1/R2 uses Enterprise security, where TKIP and WEP are not allowed.
     * @hide
     */
    public static final int SECURITY_TYPE_PASSPOINT_R1_R2 = 11;

    /**
     * Security type for a Passpoint R3 network.
     * Passpoint R3 uses Enterprise security, where TKIP and WEP are not allowed,
     * and PMF must be set to Required.
     * @hide
     */
    public static final int SECURITY_TYPE_PASSPOINT_R3 = 12;

    /** Security type for Easy Connect (DPP) network */
    public static final int SECURITY_TYPE_DPP = 13;

    /**
     * This is used for the boundary check and should be the same as the last type.
     * @hide
     */
    public static final int SECURITY_TYPE_NUM = SECURITY_TYPE_DPP;

    /**
     * Security types we support.
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "SECURITY_TYPE_" }, value = {
            SECURITY_TYPE_OPEN,
            SECURITY_TYPE_WEP,
            SECURITY_TYPE_PSK,
            SECURITY_TYPE_EAP,
            SECURITY_TYPE_SAE,
            SECURITY_TYPE_EAP_SUITE_B,
            SECURITY_TYPE_OWE,
            SECURITY_TYPE_WAPI_PSK,
            SECURITY_TYPE_WAPI_CERT,
            SECURITY_TYPE_EAP_WPA3_ENTERPRISE,
            SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT,
            SECURITY_TYPE_PASSPOINT_R1_R2,
            SECURITY_TYPE_PASSPOINT_R3,
            SECURITY_TYPE_DPP,
    })
    public @interface SecurityType {}

    private static final String[] SECURITY_TYPE_NAMES = {
        "open", "wep", "wpa2-psk", "wpa2-enterprise",
        "wpa3-sae", "wpa3 enterprise 192-bit", "owe",
        "wapi-psk", "wapi-cert", "wpa3 enterprise",
        "wpa3 enterprise 192-bit", "passpoint r1/r2",
        "passpoint r3", "dpp"};

    private List<SecurityParams> mSecurityParamsList = new ArrayList<>();

    private void updateLegacySecurityParams() {
        if (mSecurityParamsList.isEmpty()) return;
        mSecurityParamsList.get(0).updateLegacyWifiConfiguration(this);
    }

    /**
     * Set the various security params to correspond to the provided security type.
     * This is accomplished by setting the various BitSets exposed in WifiConfiguration.
     * <br>
     * This API would clear existing security types and add a default one.
     *
     * Before calling this API with {@link #SECURITY_TYPE_DPP} as securityType,
     * call {@link WifiManager#isEasyConnectDppAkmSupported() to know whether this security type is
     * supported or not.
     *
     * @param securityType One of the following security types:
     * {@link #SECURITY_TYPE_OPEN},
     * {@link #SECURITY_TYPE_WEP},
     * {@link #SECURITY_TYPE_PSK},
     * {@link #SECURITY_TYPE_EAP},
     * {@link #SECURITY_TYPE_SAE},
     * {@link #SECURITY_TYPE_OWE},
     * {@link #SECURITY_TYPE_WAPI_PSK},
     * {@link #SECURITY_TYPE_WAPI_CERT},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT},
     * {@link #SECURITY_TYPE_DPP},
     */
    public void setSecurityParams(@SecurityType int securityType) {
        // Clear existing data.
        mSecurityParamsList.clear();
        addSecurityParams(securityType);
    }

    /**
     * Set security params by the given key management mask.
     *
     * @param givenAllowedKeyManagement the given allowed key management mask.
     * @hide
     */
    public void setSecurityParams(@NonNull BitSet givenAllowedKeyManagement) {
        if (givenAllowedKeyManagement == null) {
            throw new IllegalArgumentException("Invalid allowed key management mask.");
        }
        // Clear existing data.
        mSecurityParamsList.clear();
        allowedKeyManagement = (BitSet) givenAllowedKeyManagement.clone();
        convertLegacyFieldsToSecurityParamsIfNeeded();
    }

    /**
     * Add the various security params.
     * <br>
     * This API would clear existing security types and add a default one.
     * @hide
     */
    public void setSecurityParams(SecurityParams params) {
        // Clear existing data.
        mSecurityParamsList.clear();
        addSecurityParams(params);
    }

    /**
     * Set the security params by the given security params list.
     *
     * This will overwrite existing security params list directly.
     *
     * @param securityParamsList the desired security params list.
     * @hide
     */
    public void setSecurityParams(@NonNull List<SecurityParams> securityParamsList) {
        if (securityParamsList == null || securityParamsList.isEmpty()) {
            throw new IllegalArgumentException("An empty security params list is invalid.");
        }
        mSecurityParamsList = new ArrayList<>(securityParamsList.size());
        securityParamsList.forEach(p -> mSecurityParamsList.add(new SecurityParams(p)));
        updateLegacySecurityParams();
    }

    /**
     * Add the various security params to correspond to the provided security type.
     * This is accomplished by setting the various BitSets exposed in WifiConfiguration.
     *
     * @param securityType One of the following security types:
     * {@link #SECURITY_TYPE_OPEN},
     * {@link #SECURITY_TYPE_WEP},
     * {@link #SECURITY_TYPE_PSK},
     * {@link #SECURITY_TYPE_EAP},
     * {@link #SECURITY_TYPE_SAE},
     * {@link #SECURITY_TYPE_OWE},
     * {@link #SECURITY_TYPE_WAPI_PSK},
     * {@link #SECURITY_TYPE_WAPI_CERT},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT},
     * {@link #SECURITY_TYPE_DPP},
     *
     * @hide
     */
    @Keep
    public void addSecurityParams(@SecurityType int securityType) {
        // This ensures that there won't be duplicate security types.
        for (SecurityParams params : mSecurityParamsList) {
            if (params.isSecurityType(securityType)) {
                throw new IllegalArgumentException("duplicate security type " + securityType);
            }
        }
        addSecurityParams(SecurityParams.createSecurityParamsBySecurityType(securityType));
    }

    /** @hide */
    public void addSecurityParams(@NonNull SecurityParams newParams) {
        // This ensures that there won't be duplicate security types.
        for (SecurityParams params : mSecurityParamsList) {
            if (params.isSameSecurityType(newParams)) {
                throw new IllegalArgumentException("duplicate security params " + newParams);
            }
        }
        if (!mSecurityParamsList.isEmpty()) {
            if (newParams.isEnterpriseSecurityType() && !isEnterprise()) {
                throw new IllegalArgumentException(
                        "An enterprise security type cannot be added to a personal configuation.");
            }
            if (!newParams.isEnterpriseSecurityType() && isEnterprise()) {
                throw new IllegalArgumentException(
                        "A personal security type cannot be added to an enterprise configuation.");
            }
            if (newParams.isOpenSecurityType() && !isOpenNetwork()) {
                throw new IllegalArgumentException(
                        "An open security type cannot be added to a non-open configuation.");
            }
            if (!newParams.isOpenSecurityType() && isOpenNetwork()) {
                throw new IllegalArgumentException(
                        "A non-open security type cannot be added to an open configuation.");
            }
            if (newParams.isSecurityType(SECURITY_TYPE_OSEN)) {
                throw new IllegalArgumentException(
                        "An OSEN security type must be the only one type.");
            }
        }
        mSecurityParamsList.add(new SecurityParams(newParams));
        updateLegacySecurityParams();
    }

    private boolean isWpa3EnterpriseConfiguration() {
        if (!allowedKeyManagement.get(KeyMgmt.WPA_EAP_SHA256)
                && !allowedKeyManagement.get(KeyMgmt.WPA_EAP)
                && !allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return false;
        }
        if (!requirePmf) return false;
        // Only RSN protocol is set.
        if (allowedProtocols.cardinality() > 1) return false;
        if (!allowedProtocols.get(Protocol.RSN)) return false;
        // TKIP is not allowed.
        if (allowedPairwiseCiphers.get(PairwiseCipher.TKIP)) return false;
        if (allowedGroupCiphers.get(GroupCipher.TKIP)) return false;
        return true;
    }

    /**
     * Return whether the configuration is a WPA-Personal network
     * @hide
     */
    public boolean isWpaPersonalOnlyConfiguration() {
        return isSecurityType(SECURITY_TYPE_PSK)
                && allowedProtocols.get(Protocol.WPA)
                && !allowedProtocols.get(Protocol.RSN);
    }

    /**
     * If there is no security params, generate one according to legacy fields.
     * @hide
     */
    @Keep
    public void convertLegacyFieldsToSecurityParamsIfNeeded() {
        if (!mSecurityParamsList.isEmpty()) return;
        if (allowedKeyManagement.get(KeyMgmt.WAPI_CERT)) {
            setSecurityParams(SECURITY_TYPE_WAPI_CERT);
        } else if (allowedKeyManagement.get(KeyMgmt.WAPI_PSK)) {
            setSecurityParams(SECURITY_TYPE_WAPI_PSK);
        } else if (allowedKeyManagement.get(KeyMgmt.SUITE_B_192)) {
            setSecurityParams(SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT);
        } else if (allowedKeyManagement.get(KeyMgmt.DPP)) {
            setSecurityParams(SECURITY_TYPE_DPP);
        } else if (allowedKeyManagement.get(KeyMgmt.OWE)) {
            setSecurityParams(SECURITY_TYPE_OWE);
        } else if (allowedKeyManagement.get(KeyMgmt.SAE)) {
            setSecurityParams(SECURITY_TYPE_SAE);
        } else if (allowedKeyManagement.get(KeyMgmt.OSEN)) {
            setSecurityParams(SECURITY_TYPE_OSEN);
        } else if (allowedKeyManagement.get(KeyMgmt.WPA2_PSK)
                || allowedKeyManagement.get(KeyMgmt.WPA_PSK_SHA256)
                || allowedKeyManagement.get(KeyMgmt.FT_PSK)) {
            setSecurityParams(SECURITY_TYPE_PSK);
        } else if (allowedKeyManagement.get(KeyMgmt.WPA_EAP)
                || allowedKeyManagement.get(KeyMgmt.FT_EAP)
                || allowedKeyManagement.get(KeyMgmt.IEEE8021X)
                || allowedKeyManagement.get(KeyMgmt.WPA_EAP_SHA256)
                || allowedKeyManagement.get(KeyMgmt.FILS_SHA256)
                || allowedKeyManagement.get(KeyMgmt.FILS_SHA384)) {
            if (isWpa3EnterpriseConfiguration()) {
                setSecurityParams(SECURITY_TYPE_EAP_WPA3_ENTERPRISE);
            } else {
                setSecurityParams(SECURITY_TYPE_EAP);
            }
        } else if (allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            setSecurityParams(SECURITY_TYPE_PSK);
        } else if (allowedKeyManagement.get(KeyMgmt.DPP)) {
            setSecurityParams(SECURITY_TYPE_DPP);
        } else if (allowedKeyManagement.get(KeyMgmt.NONE)) {
            if (hasWepKeys()) {
                setSecurityParams(SECURITY_TYPE_WEP);
            } else {
                setSecurityParams(SECURITY_TYPE_OPEN);
            }
        } else {
            setSecurityParams(SECURITY_TYPE_OPEN);
        }
    }

    /**
     * Disable the various security params to correspond to the provided security type.
     * This is accomplished by setting the various BitSets exposed in WifiConfiguration.
     *
     * @param securityType One of the following security types:
     * {@link #SECURITY_TYPE_OPEN},
     * {@link #SECURITY_TYPE_WEP},
     * {@link #SECURITY_TYPE_PSK},
     * {@link #SECURITY_TYPE_EAP},
     * {@link #SECURITY_TYPE_SAE},
     * {@link #SECURITY_TYPE_OWE},
     * {@link #SECURITY_TYPE_WAPI_PSK},
     * {@link #SECURITY_TYPE_WAPI_CERT},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT},
     * {@link #SECURITY_TYPE_DPP},
     *
     * @hide
     */
    public void setSecurityParamsEnabled(@SecurityType int securityType, boolean enable) {
        for (SecurityParams p : mSecurityParamsList) {
            if (p.isSecurityType(securityType)) {
                p.setEnabled(enable);
                return;
            }
        }
    }

    /**
     * Set whether a type is added by auto-upgrade.
     *
     * @param securityType One of the following security types:
     * {@link #SECURITY_TYPE_OPEN},
     * {@link #SECURITY_TYPE_WEP},
     * {@link #SECURITY_TYPE_PSK},
     * {@link #SECURITY_TYPE_EAP},
     * {@link #SECURITY_TYPE_SAE},
     * {@link #SECURITY_TYPE_OWE},
     * {@link #SECURITY_TYPE_WAPI_PSK},
     * {@link #SECURITY_TYPE_WAPI_CERT},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT},
     *
     * @hide
     */
    public void setSecurityParamsIsAddedByAutoUpgrade(
            @SecurityType int securityType, boolean isAddedByAutoUpgrade) {
        for (SecurityParams p : mSecurityParamsList) {
            if (p.isSecurityType(securityType)) {
                p.setIsAddedByAutoUpgrade(isAddedByAutoUpgrade);
                return;
            }
        }
    }

    /**
     * Get the specific security param.
     *
     * @param securityType One of the following security types:
     * {@link #SECURITY_TYPE_OPEN},
     * {@link #SECURITY_TYPE_WEP},
     * {@link #SECURITY_TYPE_PSK},
     * {@link #SECURITY_TYPE_EAP},
     * {@link #SECURITY_TYPE_SAE},
     * {@link #SECURITY_TYPE_OWE},
     * {@link #SECURITY_TYPE_WAPI_PSK},
     * {@link #SECURITY_TYPE_WAPI_CERT},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT},
     * {@link #SECURITY_TYPE_DPP},
     *
     * @return the copy of specific security params if found; otherwise null.
     * @hide
     */
    public @Nullable SecurityParams getSecurityParams(@SecurityType int securityType) {
        for (SecurityParams p : mSecurityParamsList) {
            if (p.isSecurityType(securityType)) {
                return new SecurityParams(p);
            }
        }
        return null;
    }

    /**
     * Indicate whether this configuration is the specific security type.
     *
     * @param securityType One of the following security types:
     * {@link #SECURITY_TYPE_OPEN},
     * {@link #SECURITY_TYPE_WEP},
     * {@link #SECURITY_TYPE_PSK},
     * {@link #SECURITY_TYPE_EAP},
     * {@link #SECURITY_TYPE_SAE},
     * {@link #SECURITY_TYPE_OWE},
     * {@link #SECURITY_TYPE_WAPI_PSK},
     * {@link #SECURITY_TYPE_WAPI_CERT},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT},
     * {@link #SECURITY_TYPE_DPP},
     *
     * @return true if there is a security params matches the type.
     * @hide
     */
    @Keep
    public boolean isSecurityType(@SecurityType int securityType) {
        for (SecurityParams p : mSecurityParamsList) {
            if (p.isSecurityType(securityType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the security params list of this configuration.
     *
     * The returning list is a priority list, the first is the lowest priority and default one.
     *
     * @return this list of security params.
     * @hide
     */
    public List<SecurityParams> getSecurityParamsList() {
        return Collections.unmodifiableList(mSecurityParamsList);
    }

    /**
     * Get the default params which is the same as the legacy fields.
     *
     * @return the default security params.
     * @hide
     */
    @Keep
    public @NonNull SecurityParams getDefaultSecurityParams() {
        return new SecurityParams(mSecurityParamsList.get(0));
    }

    /**
     * Enable the support of Fast Initial Link Set-up (FILS).
     *
     * FILS can be applied to all security types.
     * @param enableFilsSha256 Enable FILS SHA256.
     * @param enableFilsSha384 Enable FILS SHA256.
     * @hide
     */
    public void enableFils(boolean enableFilsSha256, boolean enableFilsSha384) {
        mSecurityParamsList.forEach(params ->
                params.enableFils(enableFilsSha256, enableFilsSha384));
        updateLegacySecurityParams();
    }

    /**
     * Indicate FILS SHA256 is enabled.
     *
     * @return true if FILS SHA256 is enabled.
     * @hide
     */
    public boolean isFilsSha256Enabled() {
        for (SecurityParams p : mSecurityParamsList) {
            if (p.getAllowedKeyManagement().get(KeyMgmt.FILS_SHA256)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicate FILS SHA384 is enabled.
     *
     * @return true if FILS SHA384 is enabled.
     * @hide
     */
    public boolean isFilsSha384Enabled() {
        for (SecurityParams p : mSecurityParamsList) {
            if (p.getAllowedKeyManagement().get(KeyMgmt.FILS_SHA384)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enable Suite-B ciphers.
     *
     * @param enableEcdheEcdsa enable Diffie-Hellman with Elliptic Curve ECDSA cipher support.
     * @param enableEcdheRsa enable Diffie-Hellman with RSA cipher support.
     * @hide
     */
    public void enableSuiteBCiphers(boolean enableEcdheEcdsa, boolean enableEcdheRsa) {
        for (SecurityParams p : mSecurityParamsList) {
            if (p.isSecurityType(SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT)) {
                p.enableSuiteBCiphers(enableEcdheEcdsa, enableEcdheRsa);
                updateLegacySecurityParams();
                return;
            }
        }
    }

    /**
     * Indicate ECDHE_ECDSA is enabled.
     *
     * @return true if enabled.
     * @hide
     */
    public boolean isSuiteBCipherEcdheEcdsaEnabled() {
        for (SecurityParams p : mSecurityParamsList) {
            if (p.getAllowedSuiteBCiphers().get(SuiteBCipher.ECDHE_ECDSA)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicate ECDHE_RSA is enabled.
     *
     * @return true if enabled.
     * @hide
     */
    public boolean isSuiteBCipherEcdheRsaEnabled() {
        for (SecurityParams p : mSecurityParamsList) {
            if (p.getAllowedSuiteBCiphers().get(SuiteBCipher.ECDHE_RSA)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set SAE Hash-toElement only mode enabled.
     * Before calling this API, call {@link WifiManager#isWpa3SaeH2eSupported()
     * to know whether WPA3 SAE Hash-toElement is supported or not.
     *
     * @param enable true if enabled; false otherwise.
     * @hide
     */
    public void enableSaeH2eOnlyMode(boolean enable) {
        for (SecurityParams p : mSecurityParamsList) {
            if (p.isSecurityType(SECURITY_TYPE_SAE)) {
                p.enableSaeH2eOnlyMode(enable);
                return;
            }
        }
    }

    /**
     * Set SAE Public-Key only mode enabled.
     * Before calling this API, call {@link WifiManager#isWpa3SaePkSupported()
     * to know whether WPA3 SAE Public-Key is supported or not.
     *
     * @param enable true if enabled; false otherwise.
     * @hide
     */
    public void enableSaePkOnlyMode(boolean enable) {
        for (SecurityParams p : mSecurityParamsList) {
            if (p.isSecurityType(SECURITY_TYPE_SAE)) {
                p.enableSaePkOnlyMode(enable);
                return;
            }
        }
    }

    /** @hide */
    public static final int UNKNOWN_UID = -1;

    /**
     * The ID number that the supplicant uses to identify this
     * network configuration entry. This must be passed as an argument
     * to most calls into the supplicant.
     */
    public int networkId;

    // TODO (b/235236813): Remove this field and use quality network selection status instead.
    /**
     * The current status of this network configuration entry.
     * @see Status
     */
    public int status;

    /**
     * The network's SSID. Can either be a UTF-8 string,
     * which must be enclosed in double quotation marks
     * (e.g., {@code "MyNetwork"}), or a string of
     * hex digits, which are not enclosed in quotes
     * (e.g., {@code 01a243f405}).
     */
    public String SSID;

    /**
     * When set, this network configuration entry should only be used when
     * associating with the AP having the specified BSSID. The value is
     * a string in the format of an Ethernet MAC address, e.g.,
     * <code>XX:XX:XX:XX:XX:XX</code> where each <code>X</code> is a hex digit.
     */
    public String BSSID;

    private List<MacAddress> mBssidAllowlist;

    private byte[] mEncryptedPreSharedKey;
    private byte[] mEncryptedPreSharedKeyIv;
    private boolean mHasPreSharedKeyChanged;

    /**
     * Set a list of BSSIDs to control if this network configuration entry should be used to
     * associate an AP.
     * <ul>
     * <li>If set with {@code null}, then there are no restrictions on the connection. The
     * configuration will associate to any AP.</li>
     * <li>If set to an empty list then the configuration will not associate to any AP.</li>
     * <li>If set to a non-empty list then the configuration will only associate to APs whose BSSID
     * is on the list.</li>
     * </ul>
     * @param bssidAllowlist A list of {@link MacAddress} representing the BSSID of APs,
     * {@code null} to allow all BSSIDs (no restriction).
     * @hide
     */
    @SystemApi
    public void setBssidAllowlist(@Nullable List<MacAddress> bssidAllowlist) {
        if (bssidAllowlist == null) {
            mBssidAllowlist = null;
            return;
        }
        mBssidAllowlist = new ArrayList<>(bssidAllowlist);
    }

    /**
     * Get a list of BSSIDs specified on this network configuration entry, set by
     * {@link #setBssidAllowlist(List)}.
     * @return A list of {@link MacAddress} representing BSSID to allow associate, {@code null} for
     * allowing all BSSIDs (no restriction).
     * @hide
     */
    @SuppressLint("NullableCollection")
    @SystemApi
    @Nullable
    public List<MacAddress> getBssidAllowlist() {
        if (mBssidAllowlist == null) {
            return null;
        }
        return new ArrayList<>(mBssidAllowlist);
    }

    /**
     * @hide
     */
    public List<MacAddress> getBssidAllowlistInternal() {
        return mBssidAllowlist;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"AP_BAND_"}, value = {
            AP_BAND_2GHZ,
            AP_BAND_5GHZ,
            AP_BAND_ANY})
    public @interface ApBand {}

    /**
     * 2GHz band.
     * @hide
     */
    public static final int AP_BAND_2GHZ = 0;

    /**
     * 5GHz band.
     * @hide
     */
    public static final int AP_BAND_5GHZ = 1;

    /**
     * 60GHz band
     * @hide
     */
    public static final int AP_BAND_60GHZ = 2;

    /**
     * Device is allowed to choose the optimal band (2Ghz or 5Ghz) based on device capability,
     * operating country code and current radio conditions.
     * @hide
     */
    public static final int AP_BAND_ANY = -1;

    /**
     * The band which the AP resides on.
     * One of {@link #AP_BAND_2GHZ}, {@link #AP_BAND_5GHZ}, or {@link #AP_BAND_ANY}.
     * By default, {@link #AP_BAND_2GHZ} is chosen.
     *
     * @hide
     */
    @UnsupportedAppUsage
    @ApBand
    public int apBand = AP_BAND_2GHZ;

    /**
     * The channel which AP resides on,currently, US only
     * 2G  1-11
     * 5G  36,40,44,48,149,153,157,161,165
     * 0 - find a random available channel according to the apBand
     * @hide
     */
    @UnsupportedAppUsage
    public int apChannel = 0;

    /**
     * Pre-shared key for use with WPA-PSK. Either an ASCII string enclosed in
     * double quotation marks (e.g., {@code "abcdefghij"} for PSK passphrase or
     * a string of 64 hex digits for raw PSK.
     * <p/>
     * When the value of this key is read, the actual key is
     * not returned, just a "*" if the key has a value, or the null
     * string otherwise.
     */
    public String preSharedKey;

    /**
     * Four WEP keys. For each of the four values, provide either an ASCII
     * string enclosed in double quotation marks (e.g., {@code "abcdef"})
     * or a string of hex digits (e.g., {@code 0102030405}).
     * <p/>
     * When the value of one of these keys is read, the actual key is
     * not returned, just a "*" if the key has a value, or the null
     * string otherwise.
     * @deprecated Due to security and performance limitations, use of WEP networks
     * is discouraged.
     */
    @Deprecated
    public String[] wepKeys;

    /** Default WEP key index, ranging from 0 to 3.
     * @deprecated Due to security and performance limitations, use of WEP networks
     * is discouraged. */
    @Deprecated
    public int wepTxKeyIndex;

    /**
     * Priority determines the preference given to a network by {@code wpa_supplicant}
     * when choosing an access point with which to associate.
     * @deprecated This field does not exist anymore.
     */
    @Deprecated
    public int priority;

    /**
     * The deletion priority of this configuration.
     *
     * Deletion priority is a non-negative value (default 0) indicating the priority for deletion
     * when auto-pruning the amount of saved configurations. Networks with a lower value will be
     * pruned before networks with a higher value.
     */
    private int mDeletionPriority;

    /**
     * Sets the deletion priority of this configuration.
     *
     * Deletion priority is a non-negative value (default 0) indicating the priority for deletion
     * when auto-pruning the amount of saved configurations. Networks with a lower value will be
     * pruned before networks with a higher value.
     *
     * @param priority non-negative deletion priority
     * @hide
     */
    @SystemApi
    public void setDeletionPriority(int priority) throws IllegalArgumentException {
        if (priority < 0) {
            throw new IllegalArgumentException("Deletion priority must be non-negative");
        }
        mDeletionPriority = priority;
    }

    /**
     * Returns the deletion priority of this configuration.
     *
     * Deletion priority is a non-negative value (default 0) indicating the priority for deletion
     * when auto-pruning the amount of saved configurations. Networks with a lower value will be
     * pruned before networks with a higher value.
     *
     * @hide
     */
    @SystemApi
    public int getDeletionPriority() {
        return mDeletionPriority;
    }

    /**
     * This is a network that does not broadcast its SSID, so an
     * SSID-specific probe request must be used for scans.
     */
    public boolean hiddenSSID;

    /**
     * True if the network requires Protected Management Frames (PMF), false otherwise.
     * @hide
     */
    @SystemApi
    public boolean requirePmf;

    /**
     * Update identifier, for Passpoint network.
     * @hide
     */
    public String updateIdentifier;

    /**
     * The set of key management protocols supported by this configuration.
     * See {@link KeyMgmt} for descriptions of the values.
     * Defaults to WPA-PSK WPA-EAP.
     */
    @NonNull
    public BitSet allowedKeyManagement;
    /**
     * The set of security protocols supported by this configuration.
     * See {@link Protocol} for descriptions of the values.
     * Defaults to WPA RSN.
     */
    @NonNull
    public BitSet allowedProtocols;
    /**
     * The set of authentication protocols supported by this configuration.
     * See {@link AuthAlgorithm} for descriptions of the values.
     * Defaults to automatic selection.
     */
    @NonNull
    public BitSet allowedAuthAlgorithms;
    /**
     * The set of pairwise ciphers for WPA supported by this configuration.
     * See {@link PairwiseCipher} for descriptions of the values.
     * Defaults to CCMP TKIP.
     */
    @NonNull
    public BitSet allowedPairwiseCiphers;
    /**
     * The set of group ciphers supported by this configuration.
     * See {@link GroupCipher} for descriptions of the values.
     * Defaults to CCMP TKIP WEP104 WEP40.
     */
    @NonNull
    public BitSet allowedGroupCiphers;
    /**
     * The set of group management ciphers supported by this configuration.
     * See {@link GroupMgmtCipher} for descriptions of the values.
     */
    @NonNull
    public BitSet allowedGroupManagementCiphers;
    /**
     * The set of SuiteB ciphers supported by this configuration.
     * To be used for WPA3-Enterprise mode. Set automatically by the framework based on the
     * certificate type that is used in this configuration.
     */
    @NonNull
    public BitSet allowedSuiteBCiphers;
    /**
     * The enterprise configuration details specifying the EAP method,
     * certificates and other settings associated with the EAP.
     */
    public WifiEnterpriseConfig enterpriseConfig;

    /**
     * Fully qualified domain name of a Passpoint configuration
     */
    public String FQDN;

    /**
     * Name of Passpoint credential provider
     */
    public String providerFriendlyName;

    /**
     * Flag indicating if this network is provided by a home Passpoint provider or a roaming
     * Passpoint provider.  This flag will be {@code true} if this network is provided by
     * a home Passpoint provider and {@code false} if is provided by a roaming Passpoint provider
     * or is a non-Passpoint network.
     */
    public boolean isHomeProviderNetwork;

    /**
     * Roaming Consortium Id list for Passpoint credential; identifies a set of networks where
     * Passpoint credential will be considered valid
     */
    public long[] roamingConsortiumIds;

    /**
     * True if this network configuration is visible to and usable by other users on the
     * same device, false otherwise.
     *
     * @hide
     */
    @SystemApi
    public boolean shared;

    /**
     * @hide
     */
    @NonNull
    @UnsupportedAppUsage
    private IpConfiguration mIpConfiguration;

    /**
     * dhcp server MAC address if known
     * @hide
     */
    public String dhcpServer;

    /**
     * default Gateway MAC address if known
     * @hide
     */
    @UnsupportedAppUsage
    public String defaultGwMacAddress;

    /**
     * last time we connected, this configuration had validated internet access
     * @hide
     */
    @UnsupportedAppUsage
    public boolean validatedInternetAccess;

    /**
     * The number of beacon intervals between Delivery Traffic Indication Maps (DTIM)
     * This value is populated from scan results that contain Beacon Frames, which are infrequent.
     * The value is not guaranteed to be set or current (Although it SHOULDNT change once set)
     * Valid values are from 1 - 255. Initialized here as 0, use this to check if set.
     * @hide
     */
    public int dtimInterval = 0;

    /**
     * Flag indicating if this configuration represents a legacy Passpoint configuration
     * (Release N or older).  This is used for migrating Passpoint configuration from N to O.
     * This will no longer be needed after O.
     * @hide
     */
    public boolean isLegacyPasspointConfig = false;
    /**
     * Uid of app creating the configuration
     * @hide
     */
    @SystemApi
    public int creatorUid;

    /**
     * Uid of last app issuing a connection related command
     * @hide
     */
    @SystemApi
    public int lastConnectUid;

    /**
     * Uid of last app modifying the configuration
     * @hide
     */
    @SystemApi
    public int lastUpdateUid;

    /**
     * Universal name for app creating the configuration
     *    see {@link PackageManager#getNameForUid(int)}
     * @hide
     */
    @SystemApi
    public String creatorName;

    /**
     * Universal name for app updating the configuration
     *    see {@link PackageManager#getNameForUid(int)}
     * @hide
     */
    @SystemApi
    public String lastUpdateName;

    /**
     * The carrier ID identifies the operator who provides this network configuration.
     *    see {@link TelephonyManager#getSimCarrierId()}
     * @hide
     */
    @SystemApi
    public int carrierId = TelephonyManager.UNKNOWN_CARRIER_ID;

    /**
     * The subscription ID identifies the SIM card for which this network configuration is valid.
     * See {@link SubscriptionInfo#getSubscriptionId()}
     * @hide
     */
    @SystemApi
    public int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    @Nullable
    private ParcelUuid mSubscriptionGroup = null;

    /**
     * Auto-join is allowed by user for this network.
     * Default true.
     * @hide
     */
    @SystemApi
    public boolean allowAutojoin = true;

    /**
     * Wi-Fi7 is enabled by user for this network.
     * Default true.
     */
    private boolean mWifi7Enabled = true;

    /**
     * @hide
     */
    public void setIpProvisioningTimedOut(boolean value) {
        mIpProvisioningTimedOut = value;
    }

    /**
     * @hide
     */
    public boolean isIpProvisioningTimedOut() {
        return mIpProvisioningTimedOut;
    }

    /** @hide **/
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    public static int INVALID_RSSI = -127;

    /**
     * Number of reports indicating no Internet Access
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int numNoInternetAccessReports;

    /**
     * The WiFi configuration had no internet access the last time we connected to it.
     * @hide
     */
    @SystemApi
    public boolean hasNoInternetAccess() {
        return getNetworkSelectionStatus().hasEverConnected() && !validatedInternetAccess;
    }

    /**
     * The WiFi configuration is expected not to have Internet access (e.g., a wireless printer, a
     * Chromecast hotspot, etc.). This will be set if the user explicitly confirms a connection to
     * this configuration and selects "don't ask again".
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean noInternetAccessExpected;

    /**
     * The WiFi configuration is expected not to have Internet access (e.g., a wireless printer, a
     * Chromecast hotspot, etc.). This will be set if the user explicitly confirms a connection to
     * this configuration and selects "don't ask again".
     * @hide
     */
    @SystemApi
    public boolean isNoInternetAccessExpected() {
        return noInternetAccessExpected;
    }

    /**
     * This Wifi configuration is expected for OSU(Online Sign Up) of Passpoint Release 2.
     * @hide
     */
    public boolean osu;

    /**
     * Last time the system was connected to this configuration represented as the difference,
     * measured in milliseconds, between the last connected time and midnight, January 1, 1970 UTC.
     * <P>
     * Note that this information is only in memory will be cleared (reset to 0) for all
     * WifiConfiguration(s) after a reboot.
     * @hide
     */
    @SuppressLint("MutableBareField")
    @SystemApi
    public long lastConnected;

    /**
     * Last time the system was disconnected to this configuration.
     * @hide
     */
    public long lastDisconnected;

    /**
     * Last time this configuration was updated or created.
     * Note: This field only exists in-memory and is not persisted in WifiConfigStore.xml for
     *       privacy reasons.
     * @hide
     */
    public long lastUpdated;

    /**
     * Number of reboots since this config was last used (either connected or updated).
     * @hide
     */
    @SuppressLint("MutableBareField")
    @SystemApi
    public int numRebootsSinceLastUse;

    /**
     * Set if the configuration was self added by the framework
     * This boolean is cleared if we get a connect/save/ update or
     * any wifiManager command that indicate the user interacted with the configuration
     * since we will now consider that the configuration belong to him.
     * @deprecated only kept for @UnsupportedAppUsage
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean selfAdded;

    /**
     * Peer WifiConfiguration this WifiConfiguration was added for
     * @hide
     */
    public String peerWifiConfiguration;

    /**
     * Indicate that a WifiConfiguration is temporary and should not be saved
     * nor considered by AutoJoin.
     * @hide
     */
    public boolean ephemeral;

    /**
     * Indicate that a WifiConfiguration is temporary and should not be saved
     * nor considered by AutoJoin.
     * @hide
     */
    @SystemApi
    public boolean isEphemeral() {
      return ephemeral;
    }

    /**
     * Indicate whether the network is trusted or not. Networks are considered trusted
     * if the user explicitly allowed this network connection.
     * This bit can be used by suggestion network, see
     * {@link WifiNetworkSuggestion.Builder#setUntrusted(boolean)}
     * @hide
     */
    public boolean trusted;

    /**
     * Indicate whether the network is oem paid or not. Networks are considered oem paid
     * if the corresponding connection is only available to system apps.
     *
     * This bit can only be used by suggestion network, see
     * {@link WifiNetworkSuggestion.Builder#setOemPaid(boolean)}
     * @hide
     */
    public boolean oemPaid;


    /**
     * Indicate whether the network is oem private or not. Networks are considered oem private
     * if the corresponding connection is only available to system apps.
     *
     * This bit can only be used by suggestion network, see
     * {@link WifiNetworkSuggestion.Builder#setOemPrivate(boolean)}
     * @hide
     */
    public boolean oemPrivate;

    /**
     * Indicate whether or not the network is a carrier merged network.
     * This bit can only be used by suggestion network, see
     * {@link WifiNetworkSuggestion.Builder#setCarrierMerged(boolean)}
     * @hide
     */
    @SystemApi
    public boolean carrierMerged;

    /**
     * True if this Wifi configuration is created from a {@link WifiNetworkSuggestion},
     * false otherwise.
     *
     * @hide
     */
    @SystemApi
    public boolean fromWifiNetworkSuggestion;

    /**
     * True if this Wifi configuration is created from a {@link WifiNetworkSpecifier},
     * false otherwise.
     *
     * @hide
     */
    @SystemApi
    public boolean fromWifiNetworkSpecifier;

    /**
     * True if the creator of this configuration has expressed that it
     * should be considered metered, false otherwise.
     *
     * @see #isMetered(WifiConfiguration, WifiInfo)
     *
     * @hide
     */
    @SystemApi
    public boolean meteredHint;

    /**
     * True if this configuration is intended to be repeater enabled to expand coverage.
     */
    private boolean mIsRepeaterEnabled;

    /**
     * @hide
     */
    private boolean mIpProvisioningTimedOut;

    /**
     * Sets if this configuration is intended to be repeater enabled for expanded coverage.
     *
     * @param isRepeaterEnabled true if this network is intended to be repeater enabled,
     *        false otherwise.
     *
     * This request is only accepted if the caller is holding
     * {@link android.Manifest.permission#NETWORK_SETTINGS}.
     *
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    @SystemApi
    public void setRepeaterEnabled(boolean isRepeaterEnabled) {
        mIsRepeaterEnabled = isRepeaterEnabled;
    }

    /**
     * Returns if this configuration is intended to be repeater enabled for expanded coverage.
     *
     * @return true if this network is intended to be repeater enabled, false otherwise.
     *
     * @hide
     */
    @SystemApi
    public boolean isRepeaterEnabled() {
        return mIsRepeaterEnabled;
    }

    /**
     * Indicate whether the network is restricted or not.
     *
     * This bit can only be used by suggestion network, see
     * {@link WifiNetworkSuggestion.Builder#setRestricted(boolean)}
     * @hide
     */
    public boolean restricted;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"METERED_OVERRIDE_"}, value = {
            METERED_OVERRIDE_NONE,
            METERED_OVERRIDE_METERED,
            METERED_OVERRIDE_NOT_METERED})
    public @interface MeteredOverride {}

    /**
     * No metered override.
     * @hide
     */
    @SystemApi
    public static final int METERED_OVERRIDE_NONE = 0;
    /**
     * Override network to be metered.
     * @hide
     */
    @SystemApi
    public static final int METERED_OVERRIDE_METERED = 1;
    /**
     * Override network to be unmetered.
     * @hide
     */
    @SystemApi
    public static final int METERED_OVERRIDE_NOT_METERED = 2;

    /**
     * Indicates if the end user has expressed an explicit opinion about the
     * meteredness of this network, such as through the Settings app.
     * This value is one of {@link #METERED_OVERRIDE_NONE}, {@link #METERED_OVERRIDE_METERED},
     * or {@link #METERED_OVERRIDE_NOT_METERED}.
     * <p>
     * This should always override any values from {@link #meteredHint} or
     * {@link WifiInfo#getMeteredHint()}.
     *
     * By default this field is set to {@link #METERED_OVERRIDE_NONE}.
     *
     * @see #isMetered(WifiConfiguration, WifiInfo)
     * @hide
     */
    @SystemApi
    @MeteredOverride
    public int meteredOverride = METERED_OVERRIDE_NONE;

    /**
     * Blend together all the various opinions to decide if the given network
     * should be considered metered or not.
     *
     * @hide
     */
    @SystemApi
    public static boolean isMetered(@Nullable WifiConfiguration config, @Nullable WifiInfo info) {
        boolean metered = false;
        if (info != null && info.getMeteredHint()) {
            metered = true;
        }
        if (config != null && config.meteredHint) {
            metered = true;
        }
        if (config != null
                && config.meteredOverride == WifiConfiguration.METERED_OVERRIDE_METERED) {
            metered = true;
        }
        if (config != null
                && config.meteredOverride == WifiConfiguration.METERED_OVERRIDE_NOT_METERED) {
            metered = false;
        }
        return metered;
    }

    /** Check whether wep keys exist. */
    private boolean hasWepKeys() {
        if (wepKeys == null) return false;
        for (int i = 0; i < wepKeys.length; i++) {
            if (wepKeys[i] != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this WiFi config is for an Open or Enhanced Open network.
     * @hide
     */
    public boolean isOpenNetwork() {
        if (hasWepKeys()) {
            return false;
        }
        for (SecurityParams p : mSecurityParamsList) {
            if (!p.isOpenSecurityType()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Setting this value will force scan results associated with this configuration to
     * be included in the bucket of networks that are externally scored.
     * If not set, associated scan results will be treated as legacy saved networks and
     * will take precedence over networks in the scored category.
     * @hide
     */
    @SystemApi
    public boolean useExternalScores;

    /**
     * Number of time the scorer overrode a the priority based choice, when comparing two
     * WifiConfigurations, note that since comparing WifiConfiguration happens very often
     * potentially at every scan, this number might become very large, even on an idle
     * system.
     * @hide
     */
    @SystemApi
    public int numScorerOverride;

    /**
     * Number of time the scorer overrode a the priority based choice, and the comparison
     * triggered a network switch
     * @hide
     */
    @SystemApi
    public int numScorerOverrideAndSwitchedNetwork;

    /**
     * Number of times we associated to this configuration.
     * @hide
     */
    @SystemApi
    public int numAssociation;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"RANDOMIZATION_"}, value = {
            RANDOMIZATION_NONE,
            RANDOMIZATION_PERSISTENT,
            RANDOMIZATION_NON_PERSISTENT,
            RANDOMIZATION_AUTO})
    public @interface MacRandomizationSetting {}

    /**
     * Use factory MAC when connecting to this network
     */
    public static final int RANDOMIZATION_NONE = 0;

    /**
     * Generate a randomized MAC once and reuse it for all connections to this network
     */
    public static final int RANDOMIZATION_PERSISTENT = 1;

    /**
     * Use a randomly generated MAC address for connections to this network.
     * This option does not persist the randomized MAC address.
     */
    public static final int RANDOMIZATION_NON_PERSISTENT = 2;

    /**
     * Let the wifi framework automatically decide the MAC randomization strategy.
     */
    public static final int RANDOMIZATION_AUTO = 3;

    /**
     * Level of MAC randomization for this network.
     * One of {@link #RANDOMIZATION_NONE}, {@link #RANDOMIZATION_AUTO},
     * {@link #RANDOMIZATION_PERSISTENT} or {@link #RANDOMIZATION_NON_PERSISTENT}.
     * By default this field is set to {@link #RANDOMIZATION_AUTO}.
     * @hide
     */
    @SystemApi
    @MacRandomizationSetting
    public int macRandomizationSetting = RANDOMIZATION_AUTO;

    /**
     * Set the MAC randomization setting for this network.
     * <p>
     * Caller must satify one of the following conditions:
     * </p>
     * <ul>
     * <li>Have {@code android.Manifest.permission#NETWORK_SETTINGS} permission.</li>
     * <li>Have {@code android.Manifest.permission#NETWORK_SETUP_WIZARD} permission.</li>
     * <li>Be in Demo Mode.</li>
     * <li>Be the creator adding or updating a passpoint network.</li>
     * <li>Be an admin updating their own network.</li>
     * </ul>
     */
    public void setMacRandomizationSetting(@MacRandomizationSetting int macRandomizationSetting) {
        this.macRandomizationSetting = macRandomizationSetting;
    }

    /**
     * Get the MAC randomization setting for this network.
     */
    public @MacRandomizationSetting int getMacRandomizationSetting() {
        return this.macRandomizationSetting;
    }

    /**
     * Randomized MAC address to use with this particular network
     * @hide
     */
    @NonNull
    private MacAddress mRandomizedMacAddress;

    /**
     * The wall clock time of when |mRandomizedMacAddress| should be re-randomized in non-persistent
     * MAC randomization mode.
     * @hide
     */
    public long randomizedMacExpirationTimeMs = 0;

    /**
     * The wall clock time of when |mRandomizedMacAddress| is last modified.
     * @hide
     */
    public long randomizedMacLastModifiedTimeMs = 0;

    /**
     * Checks if the given MAC address can be used for Connected Mac Randomization
     * by verifying that it is non-null, unicast, locally assigned, and not default mac.
     * @param mac MacAddress to check
     * @return true if mac is good to use
     * @hide
     */
    public static boolean isValidMacAddressForRandomization(MacAddress mac) {
        return mac != null && !MacAddressUtils.isMulticastAddress(mac) && mac.isLocallyAssigned()
                && !MacAddress.fromString(WifiInfo.DEFAULT_MAC_ADDRESS).equals(mac);
    }

    /**
     * Returns MAC address set to be the local randomized MAC address.
     * Depending on user preference, the device may or may not use the returned MAC address for
     * connections to this network.
     * <p>
     * Information is restricted to Device Owner, Profile Owner, and Carrier apps
     * (which will only obtain addresses for configurations which they create). Other callers
     * will receive a default "02:00:00:00:00:00" MAC address.
     */
    public @NonNull MacAddress getRandomizedMacAddress() {
        return mRandomizedMacAddress;
    }

    /**
     * @param mac MacAddress to change into
     * @hide
     */
    public void setRandomizedMacAddress(@NonNull MacAddress mac) {
        if (mac == null) {
            Log.e(TAG, "setRandomizedMacAddress received null MacAddress.");
            return;
        }
        mRandomizedMacAddress = mac;
    }

    private boolean mIsSendDhcpHostnameEnabled = true;

    /**
     * Set whether to send the hostname of the device to this network's DHCP server.
     *
     * @param enabled {@code true} to send the hostname during DHCP,
     *             {@code false} to not send the hostname during DHCP.
     * @hide
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD})
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @SystemApi
    public void setSendDhcpHostnameEnabled(boolean enabled) {
        mIsSendDhcpHostnameEnabled = enabled;
    }

    /**
     * Whether to send the hostname of the device to this network's DHCP server.
     * @hide
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @SystemApi
    public boolean isSendDhcpHostnameEnabled() {
        return mIsSendDhcpHostnameEnabled;
    }

    /**
     * This network supports DPP AKM and the device is configured to
     * onboard peer enrollee devices with {@link #SECURITY_TYPE_DPP}
     * @hide
     */
    private boolean mIsDppConfigurator;

    /**
     * Private elliptic curve key used by DPP Configurator to generate other DPP Keys
     * for DPP-AKM based network configuration.
     * @hide
     */
    private byte[] mDppPrivateEcKey;

    /**
     * Signed DPP connector. The connector is used by a pair of Enrollee devices to establish
     * a security association using the DPP Introduction Protocol.
     * @hide
     */
    private byte[] mDppConnector;

    /**
     * The public signing key of the DPP configurator.
     * @hide
     */
    private byte[] mDppCSignKey;

    /**
     * DPP network access key (own private key)
     * @hide
     */
    private byte[] mDppNetAccessKey;

    /**
     * Set DPP Connection keys which are used for network access.
     * This is required for SECURITY_TYPE_DPP network connection.
     * @hide
     */
    public void setDppConnectionKeys(byte[] connector, byte[] cSignKey, byte[] netAccessKey) {
        if (connector == null || cSignKey == null || netAccessKey == null) {
            Log.e(TAG, "One of DPP key is null");
            return;
        }
        mDppConnector = connector.clone();
        mDppCSignKey = cSignKey.clone();
        mDppNetAccessKey = netAccessKey.clone();
    }

    /**
     * Allow this profile as configurable DPP profile.
     * This is required to allow SECURITY_TYPE_DPP profile to be eligible for Configuration
     * of DPP-Enrollees.
     * @hide
     */
    public void setDppConfigurator(byte[] ecKey) {
        if (ecKey != null) {
            mDppPrivateEcKey = ecKey.clone();
            mIsDppConfigurator = true;
        }
    }

    /**
     * To check if this WifiConfiguration supports configuring a peer Enrollee device with
     * SECURITY_TYPE_DPP
     */
    public boolean isDppConfigurator() {
        return mIsDppConfigurator;
    }

    /**
     * Get private elliptic curve key used by DPP Configurator to generate other DPP Keys
     * for DPP-AKM based network configuration.
     * @hide
     */
    @SystemApi
    @NonNull public byte[] getDppPrivateEcKey() {
        return mDppPrivateEcKey.clone();
    }

    /**
     * Get DPP signed connector. The connector is used by a pair of Enrollee devices to establish
     * a security association using the DPP Introduction Protocol.
     * @hide
     */
    @SystemApi
    @NonNull public byte[] getDppConnector() {
        return mDppConnector.clone();
    }

    /**
     * Get public signing key of the DPP configurator. This key is used by provisioned devices
     * to verify Connectors of other devices are signed by the same Configurator. The configurator
     * derives and sets the C-sign-key in each DPP Configuration object.
     *
     * @hide
     */
    @SystemApi
    @NonNull public byte[] getDppCSignKey() {
        return mDppCSignKey.clone();
    }

    /**
     * Get DPP network access key. Own private key used to generate common secret, PMK.
     * @hide
     */
    @SystemApi
    @NonNull public byte[] getDppNetAccessKey() {
        return mDppNetAccessKey.clone();
    }

    /** @hide
     * Boost given to RSSI on a home network for the purpose of calculating the score
     * This adds stickiness to home networks, as defined by:
     * - less than 4 known BSSIDs
     * - PSK only
     * - TODO: add a test to verify that all BSSIDs are behind same gateway
     ***/
    public static final int HOME_NETWORK_RSSI_BOOST = 5;

    /**
     * This class is used to contain all the information and API used for quality network selection.
     * @hide
     */
    @SystemApi
    public static class NetworkSelectionStatus {
        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(prefix = "NETWORK_SELECTION_",
                value = {
                NETWORK_SELECTION_ENABLED,
                NETWORK_SELECTION_TEMPORARY_DISABLED,
                NETWORK_SELECTION_PERMANENTLY_DISABLED})
        public @interface NetworkEnabledStatus {}
        /**
         * This network will be considered as a potential candidate to connect to during network
         * selection.
         */
        public static final int NETWORK_SELECTION_ENABLED = 0;
        /**
         * This network was temporary disabled. May be re-enabled after a time out.
         */
        public static final int NETWORK_SELECTION_TEMPORARY_DISABLED = 1;
        /**
         * This network was permanently disabled.
         */
        public static final int NETWORK_SELECTION_PERMANENTLY_DISABLED = 2;
        /**
         * Maximum Network selection status
         * @hide
         */
        public static final int NETWORK_SELECTION_STATUS_MAX = 3;

        /**
         * Quality network selection status String (for debug purpose). Use Quality network
         * selection status value as index to extec the corresponding debug string
         * @hide
         */
        public static final String[] QUALITY_NETWORK_SELECTION_STATUS = {
                "NETWORK_SELECTION_ENABLED",
                "NETWORK_SELECTION_TEMPORARY_DISABLED",
                "NETWORK_SELECTION_PERMANENTLY_DISABLED"};

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(prefix = "DISABLED_", value = {
                DISABLED_NONE,
                DISABLED_ASSOCIATION_REJECTION,
                DISABLED_AUTHENTICATION_FAILURE,
                DISABLED_DHCP_FAILURE,
                DISABLED_NO_INTERNET_TEMPORARY,
                DISABLED_AUTHENTICATION_NO_CREDENTIALS,
                DISABLED_NO_INTERNET_PERMANENT,
                DISABLED_BY_WIFI_MANAGER,
                DISABLED_BY_WRONG_PASSWORD,
                DISABLED_AUTHENTICATION_NO_SUBSCRIPTION,
                DISABLED_AUTHENTICATION_PRIVATE_EAP_ERROR,
                DISABLED_NETWORK_NOT_FOUND,
                DISABLED_CONSECUTIVE_FAILURES,
                DISABLED_UNWANTED_LOW_RSSI,
                DISABLED_REPEATED_NUD_FAILURES})
        public @interface NetworkSelectionDisableReason {}

        // Quality Network disabled reasons
        /** Default value. Means not disabled. */
        public static final int DISABLED_NONE = 0;
        /**
         * The starting index for network selection disabled reasons.
         * @hide
         */
        public static final int NETWORK_SELECTION_DISABLED_STARTING_INDEX = 1;
        /** This network is temporarily disabled because of multiple association rejections. */
        public static final int DISABLED_ASSOCIATION_REJECTION = 1;
        /** This network is temporarily disabled because of multiple authentication failure. */
        public static final int DISABLED_AUTHENTICATION_FAILURE = 2;
        /** This network is temporarily disabled because of multiple DHCP failure. */
        public static final int DISABLED_DHCP_FAILURE = 3;
        /** This network is temporarily disabled because it has no Internet access. */
        public static final int DISABLED_NO_INTERNET_TEMPORARY = 4;
        /** This network is permanently disabled due to absence of user credentials */
        public static final int DISABLED_AUTHENTICATION_NO_CREDENTIALS = 5;
        /**
         * This network is permanently disabled because it has no Internet access and the user does
         * not want to stay connected.
         */
        public static final int DISABLED_NO_INTERNET_PERMANENT = 6;
        /** This network is permanently disabled due to WifiManager disabling it explicitly. */
        public static final int DISABLED_BY_WIFI_MANAGER = 7;
        /** This network is permanently disabled due to wrong password. */
        public static final int DISABLED_BY_WRONG_PASSWORD = 8;
        /** This network is permanently disabled because service is not subscribed. */
        public static final int DISABLED_AUTHENTICATION_NO_SUBSCRIPTION = 9;
        /** This network is disabled due to provider-specific (private) EAP failure. */
        public static final int DISABLED_AUTHENTICATION_PRIVATE_EAP_ERROR = 10;
        /**
         * This network is disabled because supplicant failed to find a network in scan result
         * which matches the network requested by framework for connection
         * (including network capabilities).
         */
        public static final int DISABLED_NETWORK_NOT_FOUND = 11;
        /**
         * This code is used to disable a network when a high number of consecutive connection
         * failures are detected. The exact reasons of why these consecutive failures occurred is
         * included but not limited to the reasons described by failure codes above.
         */
        public static final int DISABLED_CONSECUTIVE_FAILURES = 12;
        /**
         * This code is used to disable a network when a security params is disabled
         * by the transition disable indication.
         */
        public static final int DISABLED_TRANSITION_DISABLE_INDICATION = 13;
        /**
         * This network is temporarily disabled because of unwanted network under sufficient rssi.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public static final int DISABLED_UNWANTED_LOW_RSSI = 14;
        /**
         * This network is temporarily disabled due to repeated IP reachability failures.
         * @hide
         */
        public static final int DISABLED_REPEATED_NUD_FAILURES = 15;
        /**
         * All other disable reasons should be strictly less than this value.
         * @hide
         */
        public static final int NETWORK_SELECTION_DISABLED_MAX = 16;

        /**
         * Get an integer that is equal to the maximum integer value of all the
         * DISABLED_* reasons
         * e.g. {@link #DISABLED_NONE}, {@link #DISABLED_ASSOCIATION_REJECTION}, etc.
         *
         * All DISABLED_* constants will be contiguous in the range
         * 0, 1, 2, 3, ..., getMaxNetworkSelectionDisableReasons()
         *
         * <br />
         * For example, this can be used to iterate through all the network selection
         * disable reasons like so:
         * <pre>{@code
         * for (int reason = 0; reason <= getMaxNetworkSelectionDisableReasons(); reason++) {
         *     ...
         * }
         * }</pre>
         */
        public static int getMaxNetworkSelectionDisableReason() {
            return NETWORK_SELECTION_DISABLED_MAX - 1;
        }

        /**
         * Contains info about disable reasons.
         * @hide
         */
        public static final class DisableReasonInfo {
            /**
             * A special constant which indicates the network should be permanently disabled.
             * @hide
             */
            public static final int PERMANENT_DISABLE_TIMEOUT = -1;
            /**
             * String representation for the disable reason.
             * Note that these strings are persisted in
             * {@link
             * com.android.server.wifi.util.XmlUtil.NetworkSelectionStatusXmlUtil#writeToXml},
             * so do not change the string values to maintain backwards compatibility.
             */
            public final String mReasonStr;
            /**
             * Network Selection disable reason threshold, used to debounce network failures before
             * we disable them.
             */
            public final int mDisableThreshold;
            /**
             * Network Selection disable timeout for the error. After the timeout milliseconds,
             * enable the network again.
             * If this is set to PERMANENT_DISABLE_TIMEOUT, the network will be permanently disabled
             * until the next time the user manually connects to it.
             */
            public final int mDisableTimeoutMillis;

            /**
             * Constructor
             * @param reasonStr string representation of the error
             * @param disableThreshold number of failures before we disable the network
             * @param disableTimeoutMillis the timeout, in milliseconds, before we re-enable the
             *                             network after disabling it
             */
            public DisableReasonInfo(String reasonStr, int disableThreshold,
                    int disableTimeoutMillis) {
                mReasonStr = reasonStr;
                mDisableThreshold = disableThreshold;
                mDisableTimeoutMillis = disableTimeoutMillis;
            }
        }

        /**
         * Quality network selection disable reason infos.
         * @hide
         */
        public static final SparseArray<DisableReasonInfo> DISABLE_REASON_INFOS =
                buildDisableReasonInfos();

        private static SparseArray<DisableReasonInfo> buildDisableReasonInfos() {
            SparseArray<DisableReasonInfo> reasons = new SparseArray<>();

            // Note that some of these disable thresholds are overridden in
            // WifiBlocklistMonitor#loadCustomConfigsForDisableReasonInfos using overlays.
            // TODO(b/180148727): For a few of these disable reasons, we provide defaults here
            //  and in the overlay XML, which is confusing. Clean this up so we only define the
            //  default in one place.

            reasons.append(DISABLED_NONE,
                    new DisableReasonInfo(
                            // Note that these strings are persisted in
                            // XmlUtil.NetworkSelectionStatusXmlUtil#writeToXml,
                            // so do not change the string values to maintain backwards
                            // compatibility.
                            "NETWORK_SELECTION_ENABLE",
                            -1,
                            0));

            reasons.append(DISABLED_ASSOCIATION_REJECTION,
                    new DisableReasonInfo(
                            // Note that there is a space at the end of this string. Cannot fix
                            // since this string is persisted.
                            "NETWORK_SELECTION_DISABLED_ASSOCIATION_REJECTION ",
                            3,
                            5 * 60 * 1000));

            reasons.append(DISABLED_AUTHENTICATION_FAILURE,
                    new DisableReasonInfo(
                            "NETWORK_SELECTION_DISABLED_AUTHENTICATION_FAILURE",
                            3,
                            5 * 60 * 1000));

            reasons.append(DISABLED_DHCP_FAILURE,
                    new DisableReasonInfo(
                            "NETWORK_SELECTION_DISABLED_DHCP_FAILURE",
                            2,
                            5 * 60 * 1000));

            reasons.append(DISABLED_NO_INTERNET_TEMPORARY,
                    new DisableReasonInfo(
                            "NETWORK_SELECTION_DISABLED_NO_INTERNET_TEMPORARY",
                            1,
                            10 * 60 * 1000));

            reasons.append(DISABLED_AUTHENTICATION_NO_CREDENTIALS,
                    new DisableReasonInfo(
                            "NETWORK_SELECTION_DISABLED_AUTHENTICATION_NO_CREDENTIALS",
                            3,
                            DisableReasonInfo.PERMANENT_DISABLE_TIMEOUT));

            reasons.append(DISABLED_NO_INTERNET_PERMANENT,
                    new DisableReasonInfo(
                            "NETWORK_SELECTION_DISABLED_NO_INTERNET_PERMANENT",
                            1,
                            DisableReasonInfo.PERMANENT_DISABLE_TIMEOUT));

            reasons.append(DISABLED_BY_WIFI_MANAGER,
                    new DisableReasonInfo(
                            "NETWORK_SELECTION_DISABLED_BY_WIFI_MANAGER",
                            1,
                            DisableReasonInfo.PERMANENT_DISABLE_TIMEOUT));

            reasons.append(DISABLED_BY_WRONG_PASSWORD,
                    new DisableReasonInfo(
                            "NETWORK_SELECTION_DISABLED_BY_WRONG_PASSWORD",
                            1,
                            DisableReasonInfo.PERMANENT_DISABLE_TIMEOUT));

            reasons.append(DISABLED_AUTHENTICATION_NO_SUBSCRIPTION,
                    new DisableReasonInfo(
                            "NETWORK_SELECTION_DISABLED_AUTHENTICATION_NO_SUBSCRIPTION",
                            1,
                            DisableReasonInfo.PERMANENT_DISABLE_TIMEOUT));

            reasons.append(DISABLED_AUTHENTICATION_PRIVATE_EAP_ERROR,
                    new DisableReasonInfo(
                            "NETWORK_SELECTION_DISABLED_AUTHENTICATION_PRIVATE_EAP_ERROR",
                            1,
                            DisableReasonInfo.PERMANENT_DISABLE_TIMEOUT));

            reasons.append(DISABLED_NETWORK_NOT_FOUND,
                    new DisableReasonInfo(
                            "NETWORK_SELECTION_DISABLED_NETWORK_NOT_FOUND",
                            2,
                            5 * 60 * 1000));

            reasons.append(DISABLED_CONSECUTIVE_FAILURES,
                    new DisableReasonInfo("NETWORK_SELECTION_DISABLED_CONSECUTIVE_FAILURES",
                            1,
                            5 * 60 * 1000));

            reasons.append(DISABLED_TRANSITION_DISABLE_INDICATION,
                    new DisableReasonInfo(
                            "NETWORK_SELECTION_DISABLED_TRANSITION_DISABLE_INDICATION",
                            1,
                            DisableReasonInfo.PERMANENT_DISABLE_TIMEOUT));

            reasons.append(DISABLED_UNWANTED_LOW_RSSI,
                    new DisableReasonInfo("NETWORK_SELECTION_DISABLED_UNWANTED_LOW_RSSI",
                            1,
                            30 * 1000));
            reasons.append(DISABLED_REPEATED_NUD_FAILURES,
                    new DisableReasonInfo("NETWORK_SELECTION_DISABLED_REPEATED_NUD_FAILURES",
                            1,
                            15 * 60 * 1000));
            return reasons;
        }

        /**
         * Get the {@link NetworkSelectionDisableReason} int code by its string value.
         * @return the NetworkSelectionDisableReason int code corresponding to the reason string,
         * or -1 if the reason string is unrecognized.
         * @hide
         */
        @NetworkSelectionDisableReason
        public static int getDisableReasonByString(@NonNull String reasonString) {
            for (int i = 0; i < DISABLE_REASON_INFOS.size(); i++) {
                int key = DISABLE_REASON_INFOS.keyAt(i);
                DisableReasonInfo value = DISABLE_REASON_INFOS.valueAt(i);
                if (value != null && TextUtils.equals(reasonString, value.mReasonStr)) {
                    return key;
                }
            }
            Log.e(TAG, "Unrecognized network disable reason: " + reasonString);
            return -1;
        }

        /**
         * Invalid time stamp for network selection disable
         * @hide
         */
        public static final long INVALID_NETWORK_SELECTION_DISABLE_TIMESTAMP = -1L;

        /**
         * This constant indicates the current configuration has connect choice set
         */
        private static final int CONNECT_CHOICE_EXISTS = 1;

        /**
         * This constant indicates the current configuration does not have connect choice set
         */
        private static final int CONNECT_CHOICE_NOT_EXISTS = -1;

        // fields for QualityNetwork Selection
        /**
         * Network selection status, should be in one of three status: enable, temporaily disabled
         * or permanently disabled
         */
        @NetworkEnabledStatus
        private int mStatus;

        /**
         * Reason for disable this network
         */
        @NetworkSelectionDisableReason
        private int mNetworkSelectionDisableReason;

        /**
         * Last time we temporarily disabled the configuration
         */
        private long mTemporarilyDisabledTimestamp = INVALID_NETWORK_SELECTION_DISABLE_TIMESTAMP;

        private long mTemporarilyDisabledEndTime = INVALID_NETWORK_SELECTION_DISABLE_TIMESTAMP;

        /**
         * counter for each Network selection disable reason
         */
        private int[] mNetworkSeclectionDisableCounter = new int[NETWORK_SELECTION_DISABLED_MAX];

        /**
         * Connect Choice over this configuration
         *
         * When current wifi configuration is visible to the user but user explicitly choose to
         * connect to another network X, the another networks X's configure key will be stored here.
         * We will consider user has a preference of X over this network. And in the future,
         * network selection will always give X a higher preference over this configuration.
         * configKey is : "SSID"-WEP-WPA_PSK-WPA_EAP
         */
        private String mConnectChoice;

        /**
         * The RSSI when the user made the connectChoice.
         */
        private int mConnectChoiceRssi;

        /**
         * Used to cache the temporary candidate during the network selection procedure. It will be
         * kept updating once a new scan result has a higher score than current one
         */
        private ScanResult mCandidate;

        /**
         * Used to cache the score of the current temporary candidate during the network
         * selection procedure.
         */
        private int mCandidateScore;

        /**
         * Used to cache the select security params from the candidate.
         */
        private SecurityParams mCandidateSecurityParams;

        /**
         * Used to cache the last used security params for the candidate.
         */
        private SecurityParams mLastUsedSecurityParams;

        /**
         * Indicate whether this network is visible in latest Qualified Network Selection. This
         * means there is scan result found related to this Configuration and meet the minimum
         * requirement. The saved network need not join latest Qualified Network Selection. For
         * example, it is disabled. True means network is visible in latest Qualified Network
         * Selection and false means network is invisible
         */
        private boolean mSeenInLastQualifiedNetworkSelection;

        /**
         * Boolean indicating if we have ever successfully connected to this network.
         *
         * This value will be set to true upon a successful connection.
         * This value will be set to false if a previous value was not stored in the config or if
         * the credentials are updated (ex. a password change).
         */
        private boolean mHasEverConnected;

        /**
         * Boolean indicating if captive portal has never been detected on this network.
         *
         * This should be true by default, for newly created WifiConfigurations until a captive
         * portal is detected.
         */
        private boolean mHasNeverDetectedCaptivePortal = true;


        /**
         * Boolean tracking whether internet validation have ever completed successfully on this
         * WifiConfiguration.
         */
        private boolean mHasEverValidatedInternetAccess;

        /**
         * set whether this network is visible in latest Qualified Network Selection
         * @param seen value set to candidate
         * @hide
         */
        public void setSeenInLastQualifiedNetworkSelection(boolean seen) {
            mSeenInLastQualifiedNetworkSelection =  seen;
        }

        /**
         * get whether this network is visible in latest Qualified Network Selection
         * @return returns true -- network is visible in latest Qualified Network Selection
         *         false -- network is invisible in latest Qualified Network Selection
         * @hide
         */
        public boolean getSeenInLastQualifiedNetworkSelection() {
            return mSeenInLastQualifiedNetworkSelection;
        }
        /**
         * set the temporary candidate of current network selection procedure
         * @param scanCandidate {@link ScanResult} the candidate set to mCandidate
         * @hide
         */
        public void setCandidate(ScanResult scanCandidate) {
            mCandidate = scanCandidate;
        }

        /**
         * get the temporary candidate of current network selection procedure
         * @return  returns {@link ScanResult} temporary candidate of current network selection
         * procedure
         * @hide
         */
        public ScanResult getCandidate() {
            return mCandidate;
        }

        /**
         * set the score of the temporary candidate of current network selection procedure
         * @param score value set to mCandidateScore
         * @hide
         */
        public void setCandidateScore(int score) {
            mCandidateScore = score;
        }

        /**
         * get the score of the temporary candidate of current network selection procedure
         * @return returns score of the temporary candidate of current network selection procedure
         * @hide
         */
        public int getCandidateScore() {
            return mCandidateScore;
        }

        /**
         * set the security type of the temporary candidate of current network selection procedure
         * @param params value to set to mCandidateSecurityParams
         * @hide
         */
        public void setCandidateSecurityParams(SecurityParams params) {
            mCandidateSecurityParams = params;
        }

        /**
         * get the security type of the temporary candidate of current network selection procedure
         * @return return the security params
         * @hide
         */
        public SecurityParams getCandidateSecurityParams() {
            return mCandidateSecurityParams;
        }

        /**
         * set the last used security type of the network
         * @param params value to set to mLastUsedSecurityParams
         * @hide
         */
        public void setLastUsedSecurityParams(SecurityParams params) {
            mLastUsedSecurityParams = params;
        }

        /**
         * get the last used security type of the network
         * @return return the security params
         * @hide
         */
        public SecurityParams getLastUsedSecurityParams() {
            return mLastUsedSecurityParams;
        }

        /**
         * get user preferred choice over this configuration
         * @return returns configKey of user preferred choice over this configuration
         * @hide
         */
        public String getConnectChoice() {
            return mConnectChoice;
        }

        /**
         * set user preferred choice over this configuration
         * @param newConnectChoice, the configKey of user preferred choice over this configuration
         * @hide
         */
        public void setConnectChoice(String newConnectChoice) {
            mConnectChoice = newConnectChoice;
        }

        /**
         * Associate a RSSI with the user connect choice network.
         * @param rssi signal strength
         * @hide
         */
        public void setConnectChoiceRssi(int rssi) {
            mConnectChoiceRssi = rssi;
        }

        /**
         * @return returns the RSSI of the last time the user made the connect choice.
         * @hide
         */
        public int getConnectChoiceRssi() {
            return mConnectChoiceRssi;
        }

        /** Get the current Quality network selection status as a String (for debugging). */
        @NonNull
        public String getNetworkStatusString() {
            return QUALITY_NETWORK_SELECTION_STATUS[mStatus];
        }

        /** @hide */
        public void setHasEverConnected(boolean value) {
            mHasEverConnected = value;
        }

        /** True if the device has ever connected to this network, false otherwise. */
        public boolean hasEverConnected() {
            return mHasEverConnected;
        }

        /**
         * Set whether a captive portal has never been detected on this network.
         * @hide
         */
        public void setHasNeverDetectedCaptivePortal(boolean value) {
            mHasNeverDetectedCaptivePortal = value;
        }

        /** @hide */
        @Keep
        public boolean hasNeverDetectedCaptivePortal() {
            return mHasNeverDetectedCaptivePortal;
        }


        /**
         * Get whether internet validation was ever successful on this WifiConfiguration.
         * @hide
         */
        public boolean hasEverValidatedInternetAccess() {
            return mHasEverValidatedInternetAccess;
        }

        /**
         * @hide
         */
        public void setHasEverValidatedInternetAccess(boolean everValidated) {
            mHasEverValidatedInternetAccess = everValidated;
        }

        /** @hide */
        public NetworkSelectionStatus() {
            // previously stored configs will not have this parameter, so we default to false.
            mHasEverConnected = false;
        }

        /**
         * NetworkSelectionStatus exports an immutable public API.
         * However, test code has a need to construct a NetworkSelectionStatus in a specific state.
         * (Note that mocking using Mockito does not work if the object needs to be parceled and
         * unparceled.)
         * Export a @SystemApi Builder to allow tests to construct a NetworkSelectionStatus object
         * in the desired state, without sacrificing NetworkSelectionStatus's immutability.
         */
        @VisibleForTesting
        public static final class Builder {
            private final NetworkSelectionStatus mNetworkSelectionStatus =
                    new NetworkSelectionStatus();

            /**
             * Set the current network selection status.
             * One of:
             * {@link #NETWORK_SELECTION_ENABLED},
             * {@link #NETWORK_SELECTION_TEMPORARY_DISABLED},
             * {@link #NETWORK_SELECTION_PERMANENTLY_DISABLED}
             * @see NetworkSelectionStatus#getNetworkSelectionStatus()
             */
            @NonNull
            public Builder setNetworkSelectionStatus(@NetworkEnabledStatus int status) {
                mNetworkSelectionStatus.setNetworkSelectionStatus(status);
                return this;
            }

            /**
             *
             * Set the current network's disable reason.
             * One of the {@link #DISABLED_NONE} or DISABLED_* constants.
             * e.g. {@link #DISABLED_ASSOCIATION_REJECTION}.
             * @see NetworkSelectionStatus#getNetworkSelectionDisableReason()
             */
            @NonNull
            public Builder setNetworkSelectionDisableReason(
                    @NetworkSelectionDisableReason int reason) {
                mNetworkSelectionStatus.setNetworkSelectionDisableReason(reason);
                return this;
            }

            /**
             * Build a NetworkSelectionStatus object.
             */
            @NonNull
            public NetworkSelectionStatus build() {
                NetworkSelectionStatus status = new NetworkSelectionStatus();
                status.copy(mNetworkSelectionStatus);
                return status;
            }
        }

        /**
         * Get the network disable reason string for a reason code (for debugging).
         * @param reason specific error reason. One of the {@link #DISABLED_NONE} or
         *               DISABLED_* constants e.g. {@link #DISABLED_ASSOCIATION_REJECTION}.
         * @return network disable reason string, or null if the reason is invalid.
         */
        @Nullable
        public static String getNetworkSelectionDisableReasonString(
                @NetworkSelectionDisableReason int reason) {
            DisableReasonInfo info = DISABLE_REASON_INFOS.get(reason);
            if (info == null) {
                return null;
            } else {
                return info.mReasonStr;
            }
        }
        /**
         * get current network disable reason
         * @return current network disable reason in String (for debug purpose)
         * @hide
         */
        public String getNetworkSelectionDisableReasonString() {
            return getNetworkSelectionDisableReasonString(mNetworkSelectionDisableReason);
        }

        /**
         * Get the current network network selection status.
         * One of:
         * {@link #NETWORK_SELECTION_ENABLED},
         * {@link #NETWORK_SELECTION_TEMPORARY_DISABLED},
         * {@link #NETWORK_SELECTION_PERMANENTLY_DISABLED}
         */
        @NetworkEnabledStatus
        public int getNetworkSelectionStatus() {
            return mStatus;
        }

        /**
         * True if the current network is enabled to join network selection, false otherwise.
         * @hide
         */
        public boolean isNetworkEnabled() {
            return mStatus == NETWORK_SELECTION_ENABLED;
        }

        /**
         * @return whether current network is temporary disabled
         * @hide
         */
        public boolean isNetworkTemporaryDisabled() {
            return mStatus == NETWORK_SELECTION_TEMPORARY_DISABLED;
        }

        /**
         * True if the current network is permanently disabled, false otherwise.
         * @hide
         */
        public boolean isNetworkPermanentlyDisabled() {
            return mStatus == NETWORK_SELECTION_PERMANENTLY_DISABLED;
        }

        /**
         * set current network selection status
         * @param status network selection status to set
         * @hide
         */
        public void setNetworkSelectionStatus(int status) {
            if (status >= 0 && status < NETWORK_SELECTION_STATUS_MAX) {
                mStatus = status;
            }
        }

        /**
         * Returns the current network's disable reason.
         * One of the {@link #DISABLED_NONE} or DISABLED_* constants
         * e.g. {@link #DISABLED_ASSOCIATION_REJECTION}.
         */
        @NetworkSelectionDisableReason
        public int getNetworkSelectionDisableReason() {
            return mNetworkSelectionDisableReason;
        }

        /**
         * set Network disable reason
         * @param reason Network disable reason
         * @hide
         */
        public void setNetworkSelectionDisableReason(@NetworkSelectionDisableReason int reason) {
            if (reason >= 0 && reason < NETWORK_SELECTION_DISABLED_MAX) {
                mNetworkSelectionDisableReason = reason;
            } else {
                throw new IllegalArgumentException("Illegal reason value: " + reason);
            }
        }

        /**
         * @param timeStamp Set when current network is disabled in millisecond since boot.
         * @hide
         */
        public void setDisableTime(long timeStamp) {
            mTemporarilyDisabledTimestamp = timeStamp;
        }

        /**
         * Returns when the current network was disabled, in milliseconds since boot.
         */
        public long getDisableTime() {
            return mTemporarilyDisabledTimestamp;
        }

        /**
         * Set the expected time for this WifiConfiguration to get re-enabled.
         * Timestamp is in milliseconds since boot.
         * @hide
         */
        public void setDisableEndTime(long timestamp) {
            mTemporarilyDisabledEndTime = timestamp;
        }

        /**
         * Returns the expected time for this WifiConfiguration to get re-enabled.
         * Timestamp is in milliseconds since boot.
         * @hide
         */
        public long getDisableEndTime() {
            return mTemporarilyDisabledEndTime;
        }

        /**
         * Get the disable counter of a specific reason.
         * @param reason specific failure reason. One of the {@link #DISABLED_NONE} or
         *              DISABLED_* constants e.g. {@link #DISABLED_ASSOCIATION_REJECTION}.
         * @exception IllegalArgumentException for invalid reason
         * @return counter number for specific error reason.
         */
        public int getDisableReasonCounter(@NetworkSelectionDisableReason int reason) {
            if (reason >= DISABLED_NONE && reason < NETWORK_SELECTION_DISABLED_MAX) {
                return mNetworkSeclectionDisableCounter[reason];
            } else {
                throw new IllegalArgumentException("Illegal reason value: " + reason);
            }
        }

        /**
         * set the counter of a specific failure reason
         * @param reason reason for disable error
         * @param value the counter value for this specific reason
         * @exception throw IllegalArgumentException for illegal input
         * @hide
         */
        public void setDisableReasonCounter(int reason, int value) {
            if (reason >= DISABLED_NONE && reason < NETWORK_SELECTION_DISABLED_MAX) {
                mNetworkSeclectionDisableCounter[reason] = value;
            } else {
                throw new IllegalArgumentException("Illegal reason value: " + reason);
            }
        }

        /**
         * increment the counter of a specific failure reason
         * @param reason a specific failure reason
         * @exception throw IllegalArgumentException for illegal input
         * @hide
         */
        public void incrementDisableReasonCounter(int reason) {
            if (reason >= DISABLED_NONE && reason < NETWORK_SELECTION_DISABLED_MAX) {
                mNetworkSeclectionDisableCounter[reason]++;
            } else {
                throw new IllegalArgumentException("Illegal reason value: " + reason);
            }
        }

        /**
         * clear the counter of a specific failure reason
         * @param reason a specific failure reason
         * @exception throw IllegalArgumentException for illegal input
         * @hide
         */
        public void clearDisableReasonCounter(int reason) {
            if (reason >= DISABLED_NONE && reason < NETWORK_SELECTION_DISABLED_MAX) {
                mNetworkSeclectionDisableCounter[reason] = DISABLED_NONE;
            } else {
                throw new IllegalArgumentException("Illegal reason value: " + reason);
            }
        }

        /**
         * clear all the failure reason counters
         * @hide
         */
        public void clearDisableReasonCounter() {
            Arrays.fill(mNetworkSeclectionDisableCounter, DISABLED_NONE);
        }

        /**
         * BSSID for connection to this network (through network selection procedure)
         */
        private String mNetworkSelectionBSSID;

        /**
         * get current network Selection BSSID
         * @return current network Selection BSSID
         * @hide
         */
        public String getNetworkSelectionBSSID() {
            return mNetworkSelectionBSSID;
        }

        /**
         * set network Selection BSSID
         * @param bssid The target BSSID for assocaition
         * @hide
         */
        public void setNetworkSelectionBSSID(String bssid) {
            mNetworkSelectionBSSID = bssid;
        }

        /** @hide */
        public void copy(NetworkSelectionStatus source) {
            mStatus = source.mStatus;
            mNetworkSelectionDisableReason = source.mNetworkSelectionDisableReason;
            mNetworkSeclectionDisableCounter = Arrays.copyOf(
                    source.mNetworkSeclectionDisableCounter,
                    source.mNetworkSeclectionDisableCounter.length);
            mTemporarilyDisabledTimestamp = source.mTemporarilyDisabledTimestamp;
            mTemporarilyDisabledEndTime = source.mTemporarilyDisabledEndTime;
            mNetworkSelectionBSSID = source.mNetworkSelectionBSSID;
            setSeenInLastQualifiedNetworkSelection(source.getSeenInLastQualifiedNetworkSelection());
            setCandidate(source.getCandidate());
            setCandidateScore(source.getCandidateScore());
            setCandidateSecurityParams(source.getCandidateSecurityParams());
            setLastUsedSecurityParams(source.getLastUsedSecurityParams());
            setConnectChoice(source.getConnectChoice());
            setConnectChoiceRssi(source.getConnectChoiceRssi());
            setHasEverConnected(source.hasEverConnected());
            setHasNeverDetectedCaptivePortal(source.hasNeverDetectedCaptivePortal());
            setHasEverValidatedInternetAccess(source.hasEverValidatedInternetAccess());
        }

        /** @hide */
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(getNetworkSelectionStatus());
            dest.writeInt(getNetworkSelectionDisableReason());
            for (int index = DISABLED_NONE; index < NETWORK_SELECTION_DISABLED_MAX;
                    index++) {
                dest.writeInt(getDisableReasonCounter(index));
            }
            dest.writeLong(getDisableTime());
            dest.writeLong(getDisableEndTime());
            dest.writeString(getNetworkSelectionBSSID());
            if (getConnectChoice() != null) {
                dest.writeInt(CONNECT_CHOICE_EXISTS);
                dest.writeString(getConnectChoice());
                dest.writeInt(getConnectChoiceRssi());
            } else {
                dest.writeInt(CONNECT_CHOICE_NOT_EXISTS);
            }
            dest.writeInt(hasEverConnected() ? 1 : 0);
            dest.writeInt(hasNeverDetectedCaptivePortal() ? 1 : 0);
            dest.writeBoolean(hasEverValidatedInternetAccess());
            dest.writeParcelable(getCandidateSecurityParams(), flags);
            dest.writeParcelable(getLastUsedSecurityParams(), flags);
        }

        /** @hide */
        public void readFromParcel(Parcel in) {
            setNetworkSelectionStatus(in.readInt());
            setNetworkSelectionDisableReason(in.readInt());
            for (int index = DISABLED_NONE; index < NETWORK_SELECTION_DISABLED_MAX;
                    index++) {
                setDisableReasonCounter(index, in.readInt());
            }
            setDisableTime(in.readLong());
            setDisableEndTime(in.readLong());
            setNetworkSelectionBSSID(in.readString());
            if (in.readInt() == CONNECT_CHOICE_EXISTS) {
                setConnectChoice(in.readString());
                setConnectChoiceRssi(in.readInt());
            } else {
                setConnectChoice(null);
            }
            setHasEverConnected(in.readInt() != 0);
            setHasNeverDetectedCaptivePortal(in.readInt() != 0);
            setHasEverValidatedInternetAccess(in.readBoolean());
            setCandidateSecurityParams((SecurityParams) in.readParcelable(null));
            setLastUsedSecurityParams((SecurityParams) in.readParcelable(null));
        }
    }

    /**
     * network selection related member
     * @hide
     */
    private NetworkSelectionStatus mNetworkSelectionStatus = new NetworkSelectionStatus();

    /**
     * This class is intended to store extra failure reason information for the most recent
     * connection attempt, so that it may be surfaced to the settings UI
     * @hide
     */
    // TODO(b/148626966): called by SUW via reflection, remove once SUW is updated
    public static class RecentFailure {

        private RecentFailure() {}

        /**
         * Association Rejection Status code (NONE for success/non-association-rejection-fail)
         */
        @RecentFailureReason
        private int mAssociationStatus = RECENT_FAILURE_NONE;
        private long mLastUpdateTimeSinceBootMillis;

        /**
         * @param status the association status code for the recent failure
         */
        public void setAssociationStatus(@RecentFailureReason int status,
                long updateTimeSinceBootMs) {
            mAssociationStatus = status;
            mLastUpdateTimeSinceBootMillis = updateTimeSinceBootMs;
        }
        /**
         * Sets the RecentFailure to NONE
         */
        public void clear() {
            mAssociationStatus = RECENT_FAILURE_NONE;
            mLastUpdateTimeSinceBootMillis = 0;
        }
        /**
         * Get the recent failure code. One of {@link #RECENT_FAILURE_NONE},
         * {@link #RECENT_FAILURE_AP_UNABLE_TO_HANDLE_NEW_STA},
         * {@link #RECENT_FAILURE_REFUSED_TEMPORARILY},
         * {@link #RECENT_FAILURE_POOR_CHANNEL_CONDITIONS}.
         * {@link #RECENT_FAILURE_DISCONNECTION_AP_BUSY}
         */
        @RecentFailureReason
        public int getAssociationStatus() {
            return mAssociationStatus;
        }

        /**
         * Get the timestamp the failure status is last updated, in milliseconds since boot.
         */
        public long getLastUpdateTimeSinceBootMillis() {
            return mLastUpdateTimeSinceBootMillis;
        }
    }

    /**
     * RecentFailure member
     * @hide
     */
    // TODO(b/148626966): called by SUW via reflection, once SUW is updated, make private and
    //  rename to mRecentFailure
    @NonNull
    public final RecentFailure recentFailure = new RecentFailure();

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "RECENT_FAILURE_", value = {
            RECENT_FAILURE_NONE,
            RECENT_FAILURE_AP_UNABLE_TO_HANDLE_NEW_STA,
            RECENT_FAILURE_REFUSED_TEMPORARILY,
            RECENT_FAILURE_POOR_CHANNEL_CONDITIONS,
            RECENT_FAILURE_DISCONNECTION_AP_BUSY,
            RECENT_FAILURE_MBO_ASSOC_DISALLOWED_UNSPECIFIED,
            RECENT_FAILURE_MBO_ASSOC_DISALLOWED_MAX_NUM_STA_ASSOCIATED,
            RECENT_FAILURE_MBO_ASSOC_DISALLOWED_AIR_INTERFACE_OVERLOADED,
            RECENT_FAILURE_MBO_ASSOC_DISALLOWED_AUTH_SERVER_OVERLOADED,
            RECENT_FAILURE_MBO_ASSOC_DISALLOWED_INSUFFICIENT_RSSI,
            RECENT_FAILURE_OCE_RSSI_BASED_ASSOCIATION_REJECTION,
            RECENT_FAILURE_NETWORK_NOT_FOUND

    })
    public @interface RecentFailureReason {}

    /**
     * No recent failure, or no specific reason given for the recent connection failure
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_NONE = 0;
    /**
     * Connection to this network recently failed due to Association Rejection Status 17
     * (AP is full)
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_AP_UNABLE_TO_HANDLE_NEW_STA = 17;

    /**
     * Failed to connect because the association is rejected by the AP.
     * IEEE 802.11 association status code 30.
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_REFUSED_TEMPORARILY = 1002;

    /**
     * Failed to connect because of excess frame loss and/or poor channel conditions.
     * IEEE 802.11 association status code 34.
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_POOR_CHANNEL_CONDITIONS = 1003;

    /**
     * Disconnected by the AP because the AP can't handle all the associated stations.
     * IEEE 802.11 disconnection reason code 5.
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_DISCONNECTION_AP_BUSY = 1004;

    /**
     * Failed to connect because the association is rejected by the AP with
     * MBO association disallowed Reason code: 1 - Unspecified or 0/6-255 - Reserved.
     * Details in MBO spec v1.2, 4.2.4 Table 13: MBO Association Disallowed attribute
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_MBO_ASSOC_DISALLOWED_UNSPECIFIED = 1005;

    /**
     * Failed to connect because the association is rejected by the AP with
     * MBO association disallowed Reason code: 2 - Maximum number of associated stations reached.
     * Details in MBO spec v1.2, 4.2.4 Table 13: MBO Association Disallowed attribute
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_MBO_ASSOC_DISALLOWED_MAX_NUM_STA_ASSOCIATED = 1006;

    /**
     * Failed to connect because the association is rejected by the AP with
     * MBO association disallowed Reason code: 3 - Air interface is overloaded.
     * Details in MBO spec v1.2, 4.2.4 Table 13: MBO Association Disallowed attribute
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_MBO_ASSOC_DISALLOWED_AIR_INTERFACE_OVERLOADED = 1007;

    /**
     * Failed to connect because the association is rejected by the AP with
     * MBO association disallowed Reason code: 4 - Authentication server overloaded.
     * Details in MBO spec v1.2, 4.2.4 Table 13: MBO Association Disallowed attribute
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_MBO_ASSOC_DISALLOWED_AUTH_SERVER_OVERLOADED = 1008;

    /**
     * Failed to connect because the association is rejected by the AP with
     * MBO association disallowed Reason code: 5 - Insufficient RSSI.
     * Details in MBO spec v1.2, 4.2.4 Table 13: MBO Association Disallowed attribute
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_MBO_ASSOC_DISALLOWED_INSUFFICIENT_RSSI = 1009;

    /**
     * Failed to connect because the association is rejected by the AP with
     * OCE rssi based association rejection attribute.
     * Details in OCE spec v1.0, 3.14 Presence of OCE rssi based association rejection attribute.
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_OCE_RSSI_BASED_ASSOCIATION_REJECTION = 1010;

    /**
     * Failed to connect because supplicant failed to find a network in scan result which
     * matches the network requested by framework for connection (including network capabilities).
     * @hide
     */
    @SystemApi
    public static final int RECENT_FAILURE_NETWORK_NOT_FOUND = 1011;

    /**
     * Get the failure reason for the most recent connection attempt, or
     * {@link #RECENT_FAILURE_NONE} if there was no failure.
     *
     * Failure reasons include:
     * {@link #RECENT_FAILURE_AP_UNABLE_TO_HANDLE_NEW_STA}
     * {@link #RECENT_FAILURE_REFUSED_TEMPORARILY}
     * {@link #RECENT_FAILURE_POOR_CHANNEL_CONDITIONS}
     * {@link #RECENT_FAILURE_DISCONNECTION_AP_BUSY}
     * {@link #RECENT_FAILURE_MBO_ASSOC_DISALLOWED_UNSPECIFIED}
     * {@link #RECENT_FAILURE_MBO_ASSOC_DISALLOWED_MAX_NUM_STA_ASSOCIATED}
     * {@link #RECENT_FAILURE_MBO_ASSOC_DISALLOWED_AIR_INTERFACE_OVERLOADED}
     * {@link #RECENT_FAILURE_MBO_ASSOC_DISALLOWED_AUTH_SERVER_OVERLOADED}
     * {@link #RECENT_FAILURE_MBO_ASSOC_DISALLOWED_INSUFFICIENT_RSSI}
     * {@link #RECENT_FAILURE_OCE_RSSI_BASED_ASSOCIATION_REJECTION}
     * {@link #RECENT_FAILURE_NETWORK_NOT_FOUND}
     * @hide
     */
    @RecentFailureReason
    @SystemApi
    public int getRecentFailureReason() {
        return recentFailure.getAssociationStatus();
    }

    /**
     * Get the network selection status.
     * @hide
     */
    @NonNull
    @SystemApi
    public NetworkSelectionStatus getNetworkSelectionStatus() {
        return mNetworkSelectionStatus;
    }

    /**
     * Set the network selection status.
     * @hide
     */
    @SystemApi
    public void setNetworkSelectionStatus(@NonNull NetworkSelectionStatus status) {
        mNetworkSelectionStatus = status;
    }

    /**
     * Linked Configurations: represent the set of Wificonfigurations that are equivalent
     * regarding roaming and auto-joining.
     * The linked configuration may or may not have same SSID, and may or may not have same
     * credentials.
     * For instance, linked configurations will have same defaultGwMacAddress or same dhcp server.
     * @hide
     */
    public HashMap<String, Integer>  linkedConfigurations;

    /** List of {@link OuiKeyedData} providing vendor-specific configuration data. */
    private @NonNull List<OuiKeyedData> mVendorData;

    public WifiConfiguration() {
        networkId = INVALID_NETWORK_ID;
        SSID = null;
        BSSID = null;
        FQDN = null;
        roamingConsortiumIds = new long[0];
        priority = 0;
        mDeletionPriority = 0;
        hiddenSSID = false;
        allowedKeyManagement = new BitSet();
        allowedProtocols = new BitSet();
        allowedAuthAlgorithms = new BitSet();
        allowedPairwiseCiphers = new BitSet();
        allowedGroupCiphers = new BitSet();
        allowedGroupManagementCiphers = new BitSet();
        allowedSuiteBCiphers = new BitSet();
        wepKeys = new String[4];
        for (int i = 0; i < wepKeys.length; i++) {
            wepKeys[i] = null;
        }
        enterpriseConfig = new WifiEnterpriseConfig();
        ephemeral = false;
        osu = false;
        trusted = true; // Networks are considered trusted by default.
        oemPaid = false;
        oemPrivate = false;
        carrierMerged = false;
        fromWifiNetworkSuggestion = false;
        fromWifiNetworkSpecifier = false;
        meteredHint = false;
        mIsRepeaterEnabled = false;
        meteredOverride = METERED_OVERRIDE_NONE;
        useExternalScores = false;
        validatedInternetAccess = false;
        mIpConfiguration = new IpConfiguration();
        lastUpdateUid = -1;
        creatorUid = -1;
        shared = true;
        dtimInterval = 0;
        mRandomizedMacAddress = MacAddress.fromString(WifiInfo.DEFAULT_MAC_ADDRESS);
        numRebootsSinceLastUse = 0;
        restricted = false;
        mBssidAllowlist = null;
        mIsDppConfigurator = false;
        mDppPrivateEcKey = new byte[0];
        mDppConnector = new byte[0];
        mDppCSignKey = new byte[0];
        mDppNetAccessKey = new byte[0];
        mHasPreSharedKeyChanged = false;
        mEncryptedPreSharedKey = new byte[0];
        mEncryptedPreSharedKeyIv = new byte[0];
        mIpProvisioningTimedOut = false;
        mVendorData = Collections.emptyList();
    }

    /**
     * Identify if this configuration represents a Passpoint network
     */
    public boolean isPasspoint() {
        return !TextUtils.isEmpty(FQDN)
                && !TextUtils.isEmpty(providerFriendlyName)
                && enterpriseConfig != null
                && enterpriseConfig.getEapMethod() != WifiEnterpriseConfig.Eap.NONE
                && !TextUtils.isEmpty(mPasspointUniqueId);
    }

    /**
     * Helper function, identify if a configuration is linked
     * @hide
     */
    public boolean isLinked(WifiConfiguration config) {
        if (config != null) {
            if (config.linkedConfigurations != null && linkedConfigurations != null) {
                if (config.linkedConfigurations.get(getKey()) != null
                        && linkedConfigurations.get(config.getKey()) != null) {
                    return true;
                }
            }
        }
        return  false;
    }

    /**
     * Helper function, idenfity if a configuration should be treated as an enterprise network
     * @hide
     */
    @UnsupportedAppUsage
    public boolean isEnterprise() {
        if (enterpriseConfig == null
                || enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.NONE) {
            return false;
        }
        for (SecurityParams p : mSecurityParamsList) {
            if (p.isEnterpriseSecurityType()) {
                return true;
            }
        }
        return false;
    }

    private static String logTimeOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        if (millis >= 0) {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis),
                    ZoneId.systemDefault());
            return localDateTime.toString();
        } else {
            return Long.toString(millis);
        }
    }

    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        if (this.status == WifiConfiguration.Status.CURRENT) {
            sbuf.append("* ");
        } else if (this.status == WifiConfiguration.Status.DISABLED) {
            sbuf.append("- DSBLE ");
        }
        sbuf.append("ID: ").append(this.networkId).append(" SSID: ").append(this.SSID).
                append(" PROVIDER-NAME: ").append(this.providerFriendlyName).
                append(" BSSID: ").append(this.BSSID).append(" FQDN: ").append(this.FQDN)
                .append(" HOME-PROVIDER-NETWORK: ").append(this.isHomeProviderNetwork)
                .append(" PRIO: ").append(this.priority)
                .append(" HIDDEN: ").append(this.hiddenSSID)
                .append(" PMF: ").append(this.requirePmf)
                .append(" CarrierId: ").append(this.carrierId)
                .append(" SubscriptionId: ").append(this.subscriptionId)
                .append(" SubscriptionGroup: ").append(this.mSubscriptionGroup)
                .append(" Currently Connected: ").append(this.isCurrentlyConnected)
                .append(" User Selected: ").append(this.mIsUserSelected)
                .append('\n');


        sbuf.append(" NetworkSelectionStatus ")
                .append(mNetworkSelectionStatus.getNetworkStatusString())
                .append("\n");
        if (mNetworkSelectionStatus.getNetworkSelectionDisableReason() > 0) {
            sbuf.append(" mNetworkSelectionDisableReason ")
                    .append(mNetworkSelectionStatus.getNetworkSelectionDisableReasonString())
                    .append("\n");

            for (int index = NetworkSelectionStatus.DISABLED_NONE;
                    index < NetworkSelectionStatus.NETWORK_SELECTION_DISABLED_MAX; index++) {
                if (mNetworkSelectionStatus.getDisableReasonCounter(index) != 0) {
                    sbuf.append(
                            NetworkSelectionStatus.getNetworkSelectionDisableReasonString(index))
                            .append(" counter:")
                            .append(mNetworkSelectionStatus.getDisableReasonCounter(index))
                            .append("\n");
                }
            }
        }
        if (mNetworkSelectionStatus.getConnectChoice() != null) {
            sbuf.append(" connect choice: ").append(mNetworkSelectionStatus.getConnectChoice());
            sbuf.append(" connect choice rssi: ")
                    .append(mNetworkSelectionStatus.getConnectChoiceRssi());
        }
        sbuf.append(" hasEverConnected: ")
                .append(mNetworkSelectionStatus.hasEverConnected()).append("\n");
        sbuf.append(" hasNeverDetectedCaptivePortal: ")
                .append(mNetworkSelectionStatus.hasNeverDetectedCaptivePortal()).append("\n");
        sbuf.append(" hasEverValidatedInternetAccess: ")
                .append(mNetworkSelectionStatus.hasEverValidatedInternetAccess()).append("\n");
        sbuf.append(" mCandidateSecurityParams: ")
                .append(mNetworkSelectionStatus.getCandidateSecurityParams());
        sbuf.append(" mLastUsedSecurityParams: ")
                .append(mNetworkSelectionStatus.getLastUsedSecurityParams());

        if (this.numAssociation > 0) {
            sbuf.append(" numAssociation ").append(this.numAssociation).append("\n");
        }
        if (this.numNoInternetAccessReports > 0) {
            sbuf.append(" numNoInternetAccessReports ");
            sbuf.append(this.numNoInternetAccessReports).append("\n");
        }
        if (this.validatedInternetAccess) sbuf.append(" validatedInternetAccess");
        if (this.shared) {
            sbuf.append(" shared");
        } else {
            sbuf.append(" not-shared");
        }
        if (this.ephemeral) sbuf.append(" ephemeral");
        if (this.osu) sbuf.append(" osu");
        if (this.trusted) sbuf.append(" trusted");
        if (this.restricted) sbuf.append(" restricted");
        if (this.oemPaid) sbuf.append(" oemPaid");
        if (this.oemPrivate) sbuf.append(" oemPrivate");
        if (this.carrierMerged) sbuf.append(" carrierMerged");
        if (this.fromWifiNetworkSuggestion) sbuf.append(" fromWifiNetworkSuggestion");
        if (this.fromWifiNetworkSpecifier) sbuf.append(" fromWifiNetworkSpecifier");
        if (this.meteredHint) sbuf.append(" meteredHint");
        if (this.mIsRepeaterEnabled) sbuf.append(" repeaterEnabled");
        if (this.useExternalScores) sbuf.append(" useExternalScores");
        if (this.validatedInternetAccess || this.ephemeral || this.trusted || this.oemPaid
                || this.oemPrivate || this.carrierMerged || this.fromWifiNetworkSuggestion
                || this.fromWifiNetworkSpecifier || this.meteredHint || this.useExternalScores
                || this.restricted) {
            sbuf.append("\n");
        }
        if (this.meteredOverride != METERED_OVERRIDE_NONE) {
            sbuf.append(" meteredOverride ").append(meteredOverride).append("\n");
        }
        sbuf.append(" macRandomizationSetting: ").append(macRandomizationSetting).append("\n");
        sbuf.append(" mRandomizedMacAddress: ").append(mRandomizedMacAddress).append("\n");
        sbuf.append(" randomizedMacExpirationTimeMs: ")
                .append(randomizedMacExpirationTimeMs == 0 ? "<none>"
                        : logTimeOfDay(randomizedMacExpirationTimeMs)).append("\n");
        sbuf.append(" randomizedMacLastModifiedTimeMs: ")
                .append(randomizedMacLastModifiedTimeMs == 0 ? "<none>"
                        : logTimeOfDay(randomizedMacLastModifiedTimeMs)).append("\n");
        sbuf.append(" mIsSendDhcpHostnameEnabled: ").append(mIsSendDhcpHostnameEnabled)
                .append("\n");
        sbuf.append(" deletionPriority: ").append(mDeletionPriority).append("\n");
        sbuf.append(" KeyMgmt:");
        for (int k = 0; k < this.allowedKeyManagement.size(); k++) {
            if (this.allowedKeyManagement.get(k)) {
                sbuf.append(" ");
                if (k < KeyMgmt.strings.length) {
                    sbuf.append(KeyMgmt.strings[k]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append(" Protocols:");
        for (int p = 0; p < this.allowedProtocols.size(); p++) {
            if (this.allowedProtocols.get(p)) {
                sbuf.append(" ");
                if (p < Protocol.strings.length) {
                    sbuf.append(Protocol.strings[p]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append('\n');
        sbuf.append(" AuthAlgorithms:");
        for (int a = 0; a < this.allowedAuthAlgorithms.size(); a++) {
            if (this.allowedAuthAlgorithms.get(a)) {
                sbuf.append(" ");
                if (a < AuthAlgorithm.strings.length) {
                    sbuf.append(AuthAlgorithm.strings[a]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append('\n');
        sbuf.append(" PairwiseCiphers:");
        for (int pc = 0; pc < this.allowedPairwiseCiphers.size(); pc++) {
            if (this.allowedPairwiseCiphers.get(pc)) {
                sbuf.append(" ");
                if (pc < PairwiseCipher.strings.length) {
                    sbuf.append(PairwiseCipher.strings[pc]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append('\n');
        sbuf.append(" GroupCiphers:");
        for (int gc = 0; gc < this.allowedGroupCiphers.size(); gc++) {
            if (this.allowedGroupCiphers.get(gc)) {
                sbuf.append(" ");
                if (gc < GroupCipher.strings.length) {
                    sbuf.append(GroupCipher.strings[gc]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append('\n');
        sbuf.append(" GroupMgmtCiphers:");
        for (int gmc = 0; gmc < this.allowedGroupManagementCiphers.size(); gmc++) {
            if (this.allowedGroupManagementCiphers.get(gmc)) {
                sbuf.append(" ");
                if (gmc < GroupMgmtCipher.strings.length) {
                    sbuf.append(GroupMgmtCipher.strings[gmc]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append('\n');
        sbuf.append(" SuiteBCiphers:");
        for (int sbc = 0; sbc < this.allowedSuiteBCiphers.size(); sbc++) {
            if (this.allowedSuiteBCiphers.get(sbc)) {
                sbuf.append(" ");
                if (sbc < SuiteBCipher.strings.length) {
                    sbuf.append(SuiteBCipher.strings[sbc]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append('\n').append(" PSK/SAE: ");
        if (this.preSharedKey != null) {
            sbuf.append('*');
        }

        sbuf.append("\nSecurityParams List:\n");
        mSecurityParamsList.forEach(params -> sbuf.append(params.toString()));

        sbuf.append("\nEnterprise config:\n");
        sbuf.append(enterpriseConfig);

        sbuf.append("IP config:\n");
        sbuf.append(mIpConfiguration.toString());

        if (mNetworkSelectionStatus.getNetworkSelectionBSSID() != null) {
            sbuf.append(" networkSelectionBSSID=").append(
                    mNetworkSelectionStatus.getNetworkSelectionBSSID());
        }
        long now_ms = SystemClock.elapsedRealtime();
        if (mNetworkSelectionStatus.getDisableTime() != NetworkSelectionStatus
                .INVALID_NETWORK_SELECTION_DISABLE_TIMESTAMP) {
            sbuf.append('\n');
            long diff = now_ms - mNetworkSelectionStatus.getDisableTime();
            if (diff <= 0) {
                sbuf.append(" blackListed since <incorrect>");
            } else {
                sbuf.append(" blackListed: ").append(Long.toString(diff / 1000)).append("sec ");
            }
        }
        if (mNetworkSelectionStatus.getDisableEndTime()
                != NetworkSelectionStatus.INVALID_NETWORK_SELECTION_DISABLE_TIMESTAMP) {
            sbuf.append('\n');
            long diff = mNetworkSelectionStatus.getDisableEndTime() - now_ms;
            if (diff <= 0) {
                sbuf.append(" blockListed remaining time <incorrect>");
            } else {
                sbuf.append(" blocklist end in: ").append(Long.toString(diff / 1000))
                        .append("sec ");
            }
        }
        if (creatorUid != 0) sbuf.append(" cuid=").append(creatorUid);
        if (creatorName != null) sbuf.append(" cname=").append(creatorName);
        if (lastUpdateUid != 0) sbuf.append(" luid=").append(lastUpdateUid);
        if (lastUpdateName != null) sbuf.append(" lname=").append(lastUpdateName);
        if (updateIdentifier != null) sbuf.append(" updateIdentifier=").append(updateIdentifier);
        sbuf.append(" lcuid=").append(lastConnectUid);
        sbuf.append(" allowAutojoin=").append(allowAutojoin);
        sbuf.append(" noInternetAccessExpected=").append(noInternetAccessExpected);
        sbuf.append(" mostRecentlyConnected=").append(isMostRecentlyConnected);

        sbuf.append(" ");

        if (this.lastConnected != 0) {
            sbuf.append('\n');
            sbuf.append("lastConnected: ").append(logTimeOfDay(this.lastConnected));
            sbuf.append(" ");
        }
        sbuf.append('\n');
        if (this.lastUpdated != 0) {
            sbuf.append('\n');
            sbuf.append("lastUpdated: ").append(logTimeOfDay(this.lastUpdated));
            sbuf.append(" ");
        }
        sbuf.append('\n');
        sbuf.append("numRebootsSinceLastUse: ").append(numRebootsSinceLastUse).append('\n');
        if (this.linkedConfigurations != null) {
            for (String key : this.linkedConfigurations.keySet()) {
                sbuf.append(" linked: ").append(key);
                sbuf.append('\n');
            }
        }
        sbuf.append("recentFailure: ").append("Association Rejection code: ")
                .append(recentFailure.getAssociationStatus()).append(", last update time: ")
                .append(recentFailure.getLastUpdateTimeSinceBootMillis()).append("\n");
        if (mBssidAllowlist != null) {
            sbuf.append("bssidAllowList: [");
            for (MacAddress bssid : mBssidAllowlist) {
                sbuf.append(bssid).append(", ");
            }
            sbuf.append("]");
        } else {
            sbuf.append("bssidAllowlist unset");
        }
        sbuf.append("\n");
        if (mVendorData != null && !mVendorData.isEmpty()) {
            sbuf.append("vendorData: ").append(mVendorData);
        } else {
            sbuf.append("vendorData unset");
        }
        sbuf.append("\n");
        sbuf.append("IsDppConfigurator: ").append(this.mIsDppConfigurator).append("\n");
        sbuf.append("HasEncryptedPreSharedKey: ").append(hasEncryptedPreSharedKey()).append("\n");
        sbuf.append(" setWifi7Enabled=").append(mWifi7Enabled);
        return sbuf.toString();
    }

    /**
     * Get the SSID in a human-readable format, with all additional formatting removed
     * e.g. quotation marks around the SSID, "P" prefix
     * @hide
     */
    @NonNull
    @SystemApi
    public String getPrintableSsid() {
        // TODO(b/136480579): Handle SSIDs with non-UTF-8 encodings.
        return WifiInfo.removeDoubleQuotes(SSID);
    }

    /**
     * Get an identifier for associating credentials with this config
     * @param current configuration contains values for additional fields
     *                that are not part of this configuration. Used
     *                when a config with some fields is passed by an application.
     * @throws IllegalStateException if config is invalid for key id generation
     * @hide
     */
    public String getKeyIdForCredentials(WifiConfiguration current) {
        String keyMgmt = "";

        try {
            // Get current config details for fields that are not initialized
            if (TextUtils.isEmpty(SSID)) SSID = current.SSID;
            if (allowedKeyManagement.cardinality() == 0) {
                allowedKeyManagement = current.allowedKeyManagement;
            }
            if (allowedKeyManagement.get(KeyMgmt.WPA_EAP)) {
                keyMgmt += KeyMgmt.strings[KeyMgmt.WPA_EAP];
            }
            if (allowedKeyManagement.get(KeyMgmt.OSEN)) {
                keyMgmt += KeyMgmt.strings[KeyMgmt.OSEN];
            }
            if (allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
                keyMgmt += KeyMgmt.strings[KeyMgmt.IEEE8021X];
            }
            if (allowedKeyManagement.get(KeyMgmt.SUITE_B_192)) {
                keyMgmt += KeyMgmt.strings[KeyMgmt.SUITE_B_192];
            }
            if (allowedKeyManagement.get(KeyMgmt.WAPI_CERT)) {
                keyMgmt += KeyMgmt.strings[KeyMgmt.WAPI_CERT];
            }

            if (TextUtils.isEmpty(keyMgmt)) {
                throw new IllegalStateException("Not an EAP network");
            }
            String keyId = (!TextUtils.isEmpty(SSID) && SSID.charAt(0) != '\"'
                    ? SSID.toLowerCase() : SSID) + "_" + keyMgmt + "_"
                    + trimStringForKeyId(enterpriseConfig.getKeyId(current != null
                    ? current.enterpriseConfig : null));

            if (!fromWifiNetworkSuggestion) {
                return keyId;
            }
            return keyId + "_" + trimStringForKeyId(BSSID) + "_" + trimStringForKeyId(creatorName);
        } catch (NullPointerException e) {
            throw new IllegalStateException("Invalid config details");
        }
    }

    private String trimStringForKeyId(String string) {
        if (string == null) {
            return "";
        }
        // Remove quotes and spaces
        return string.replace("\"", "").replace(" ", "");
    }

    private static BitSet readBitSet(Parcel src) {
        int cardinality = src.readInt();

        BitSet set = new BitSet();
        for (int i = 0; i < cardinality; i++) {
            set.set(src.readInt());
        }

        return set;
    }

    private static void writeBitSet(Parcel dest, BitSet set) {
        int nextSetBit = -1;

        dest.writeInt(set.cardinality());

        while ((nextSetBit = set.nextSetBit(nextSetBit + 1)) != -1) {
            dest.writeInt(nextSetBit);
        }
    }

    /**
     * Get the authentication type of the network.
     * @return One of the {@link KeyMgmt} constants. e.g. {@link KeyMgmt#WPA2_PSK}.
     * @throws IllegalStateException if config is invalid for authentication type.
     * @hide
     */
    @SystemApi
    @KeyMgmt.KeyMgmtScheme
    public int getAuthType() {
        if (allowedKeyManagement.cardinality() > 1) {
            if (allowedKeyManagement.get(KeyMgmt.WPA_EAP)) {
                if (allowedKeyManagement.cardinality() == 2
                        && allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
                    return KeyMgmt.WPA_EAP;
                }
                if (allowedKeyManagement.cardinality() == 3
                        && allowedKeyManagement.get(KeyMgmt.IEEE8021X)
                        && allowedKeyManagement.get(KeyMgmt.SUITE_B_192)) {
                    return KeyMgmt.SUITE_B_192;
                }
            }
            throw new IllegalStateException("Invalid auth type set: " + allowedKeyManagement);
        }
        if (allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            return KeyMgmt.WPA_PSK;
        } else if (allowedKeyManagement.get(KeyMgmt.WPA2_PSK)) {
            return KeyMgmt.WPA2_PSK;
        } else if (allowedKeyManagement.get(KeyMgmt.WPA_EAP)) {
            return KeyMgmt.WPA_EAP;
        } else if (allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return KeyMgmt.IEEE8021X;
        } else if (allowedKeyManagement.get(KeyMgmt.SAE)) {
            return KeyMgmt.SAE;
        } else if (allowedKeyManagement.get(KeyMgmt.OWE)) {
            return KeyMgmt.OWE;
        } else if (allowedKeyManagement.get(KeyMgmt.SUITE_B_192)) {
            return KeyMgmt.SUITE_B_192;
        } else if (allowedKeyManagement.get(KeyMgmt.WAPI_PSK)) {
            return KeyMgmt.WAPI_PSK;
        } else if (allowedKeyManagement.get(KeyMgmt.WAPI_CERT)) {
            return KeyMgmt.WAPI_CERT;
        } else if (allowedKeyManagement.get(KeyMgmt.DPP)) {
            return KeyMgmt.DPP;
        }
        return KeyMgmt.NONE;
    }

    /**
     * Return a String that can be used to uniquely identify this WifiConfiguration.
     * <br />
     * Note: Do not persist this value! This value is not guaranteed to remain backwards compatible.
     */
    @NonNull
    public String getKey() {
        // Passpoint ephemeral networks have their unique identifier set. Return it as is to be
        // able to match internally.
        if (mPasspointUniqueId != null) {
            return mPasspointUniqueId;
        }

        String key = getSsidAndSecurityTypeString();
        if (!shared) {
            key += "-" + UserHandle.getUserHandleForUid(creatorUid).getIdentifier();
        }

        return key;
    }

    /**
     * Get a unique key which represent this Wi-Fi network. If two profiles are for
     * the same Wi-Fi network, but from different provider, they would have the same key.
     * @hide
     */
    public String getNetworkKey() {
        // Passpoint ephemeral networks have their unique identifier set. Return it as is to be
        // able to match internally.
        if (mPasspointUniqueId != null) {
            return mPasspointUniqueId;
        }

        String key = getSsidAndSecurityTypeString();
        if (!shared) {
            key += "-" + UserHandle.getUserHandleForUid(creatorUid).getIdentifier();
        }

        return key;
    }

    /** @hide
     *  return the SSID + security type in String format.
     */
    public String getSsidAndSecurityTypeString() {
        return (!TextUtils.isEmpty(SSID) && SSID.charAt(0) != '\"' ? SSID.toLowerCase() : SSID)
                + getDefaultSecurityType();
    }

    /**
     * Get the IpConfiguration object associated with this WifiConfiguration.
     * @hide
     */
    @NonNull
    @SystemApi
    public IpConfiguration getIpConfiguration() {
        return new IpConfiguration(mIpConfiguration);
    }

    /**
     * Set the {@link IpConfiguration} for this network.
     *
     * @param ipConfiguration a {@link IpConfiguration} to use for this Wi-Fi configuration, or
     *                        {@code null} to use the default configuration.
     */
    public void setIpConfiguration(@Nullable IpConfiguration ipConfiguration) {
        if (ipConfiguration == null) ipConfiguration = new IpConfiguration();
        mIpConfiguration = ipConfiguration;
    }

    /**
     * Get the {@link StaticIpConfiguration} for this network.
     * @return the {@link StaticIpConfiguration}, or null if unset.
     * @hide
     */
    @Nullable
    @UnsupportedAppUsage
    public StaticIpConfiguration getStaticIpConfiguration() {
        return mIpConfiguration.getStaticIpConfiguration();
    }

    /** @hide */
    @UnsupportedAppUsage
    @Keep
    public void setStaticIpConfiguration(StaticIpConfiguration staticIpConfiguration) {
        mIpConfiguration.setStaticIpConfiguration(staticIpConfiguration);
    }

    /**
     * Get the {@link IpConfiguration.IpAssignment} for this network.
     * @hide
     */
    @NonNull
    @UnsupportedAppUsage
    @Keep
    public IpConfiguration.IpAssignment getIpAssignment() {
        return mIpConfiguration.getIpAssignment();
    }

    /** @hide */
    @UnsupportedAppUsage
    @Keep
    public void setIpAssignment(IpConfiguration.IpAssignment ipAssignment) {
        mIpConfiguration.setIpAssignment(ipAssignment);
    }

    /**
     * Get the {@link IpConfiguration.ProxySettings} for this network.
     * @hide
     */
    @NonNull
    @UnsupportedAppUsage
    public IpConfiguration.ProxySettings getProxySettings() {
        return mIpConfiguration.getProxySettings();
    }

    /** @hide */
    @UnsupportedAppUsage
    public void setProxySettings(IpConfiguration.ProxySettings proxySettings) {
        mIpConfiguration.setProxySettings(proxySettings);
    }

    /**
     * Returns the HTTP proxy used by this object.
     * @return a {@link ProxyInfo httpProxy} representing the proxy specified by this
     *                  WifiConfiguration, or {@code null} if no proxy is specified.
     */
    public ProxyInfo getHttpProxy() {
        if (mIpConfiguration.getProxySettings() == IpConfiguration.ProxySettings.NONE) {
            return null;
        }
        return new ProxyInfo(mIpConfiguration.getHttpProxy());
    }

    /**
     * Set the {@link ProxyInfo} for this WifiConfiguration. This method should only be used by a
     * device owner or profile owner. When other apps attempt to save a {@link WifiConfiguration}
     * with modified proxy settings, the methods {@link WifiManager#addNetwork} and
     * {@link WifiManager#updateNetwork} fail and return {@code -1}.
     *
     * @param httpProxy {@link ProxyInfo} representing the httpProxy to be used by this
     *                  WifiConfiguration. Setting this to {@code null} will explicitly set no
     *                  proxy, removing any proxy that was previously set.
     */
    public void setHttpProxy(ProxyInfo httpProxy) {
        if (httpProxy == null) {
            mIpConfiguration.setProxySettings(IpConfiguration.ProxySettings.NONE);
            mIpConfiguration.setHttpProxy(null);
            return;
        }
        ProxyInfo httpProxyCopy;
        ProxySettings proxySettingCopy;
        if (!Uri.EMPTY.equals(httpProxy.getPacFileUrl())) {
            proxySettingCopy = IpConfiguration.ProxySettings.PAC;
            // Construct a new PAC URL Proxy
            httpProxyCopy = ProxyInfo.buildPacProxy(httpProxy.getPacFileUrl(), httpProxy.getPort());
        } else {
            proxySettingCopy = IpConfiguration.ProxySettings.STATIC;
            // Construct a new HTTP Proxy
            String[] exclusionList = httpProxy.getExclusionList();
            if (exclusionList == null) {
                exclusionList = new String[0];
            }
            httpProxyCopy = ProxyInfo.buildDirectProxy(httpProxy.getHost(), httpProxy.getPort(),
                    Arrays.asList(exclusionList));
        }
        if (!httpProxyCopy.isValid()) {
            Log.w(TAG, "ProxyInfo is not valid: " + httpProxyCopy);
        }
        mIpConfiguration.setProxySettings(proxySettingCopy);
        mIpConfiguration.setHttpProxy(httpProxyCopy);
    }

    /**
     * Set the {@link ProxySettings} and {@link ProxyInfo} for this network.
     * @hide
     */
    @UnsupportedAppUsage
    public void setProxy(@NonNull ProxySettings settings, @NonNull ProxyInfo proxy) {
        mIpConfiguration.setProxySettings(settings);
        mIpConfiguration.setHttpProxy(proxy);
    }

    /** Implement the Parcelable interface {@hide} */
    public int describeContents() {
        return 0;
    }

    /** @hide */
    public void setPasspointManagementObjectTree(String passpointManagementObjectTree) {
        mPasspointManagementObjectTree = passpointManagementObjectTree;
    }

    /** @hide */
    public String getMoTree() {
        return mPasspointManagementObjectTree;
    }

    /** Copy constructor */
    public WifiConfiguration(@NonNull WifiConfiguration source) {
        if (source != null) {
            networkId = source.networkId;
            status = source.status;
            SSID = source.SSID;
            BSSID = source.BSSID;
            FQDN = source.FQDN;
            roamingConsortiumIds = source.roamingConsortiumIds.clone();
            providerFriendlyName = source.providerFriendlyName;
            isHomeProviderNetwork = source.isHomeProviderNetwork;
            preSharedKey = source.preSharedKey;

            mNetworkSelectionStatus.copy(source.getNetworkSelectionStatus());
            apBand = source.apBand;
            apChannel = source.apChannel;

            wepKeys = new String[4];
            for (int i = 0; i < wepKeys.length; i++) {
                wepKeys[i] = source.wepKeys[i];
            }

            wepTxKeyIndex = source.wepTxKeyIndex;
            priority = source.priority;
            mDeletionPriority = source.mDeletionPriority;
            hiddenSSID = source.hiddenSSID;
            allowedKeyManagement   = (BitSet) source.allowedKeyManagement.clone();
            allowedProtocols       = (BitSet) source.allowedProtocols.clone();
            allowedAuthAlgorithms  = (BitSet) source.allowedAuthAlgorithms.clone();
            allowedPairwiseCiphers = (BitSet) source.allowedPairwiseCiphers.clone();
            allowedGroupCiphers    = (BitSet) source.allowedGroupCiphers.clone();
            allowedGroupManagementCiphers = (BitSet) source.allowedGroupManagementCiphers.clone();
            allowedSuiteBCiphers    = (BitSet) source.allowedSuiteBCiphers.clone();
            mSecurityParamsList = new ArrayList<>(source.mSecurityParamsList.size());
            source.mSecurityParamsList.forEach(p -> mSecurityParamsList.add(new SecurityParams(p)));
            enterpriseConfig = new WifiEnterpriseConfig(source.enterpriseConfig);

            defaultGwMacAddress = source.defaultGwMacAddress;

            mIpConfiguration = new IpConfiguration(source.mIpConfiguration);

            if ((source.linkedConfigurations != null)
                    && (source.linkedConfigurations.size() > 0)) {
                linkedConfigurations = new HashMap<String, Integer>();
                linkedConfigurations.putAll(source.linkedConfigurations);
            }
            validatedInternetAccess = source.validatedInternetAccess;
            isLegacyPasspointConfig = source.isLegacyPasspointConfig;
            ephemeral = source.ephemeral;
            osu = source.osu;
            trusted = source.trusted;
            restricted = source.restricted;
            oemPaid = source.oemPaid;
            oemPrivate = source.oemPrivate;
            carrierMerged = source.carrierMerged;
            fromWifiNetworkSuggestion = source.fromWifiNetworkSuggestion;
            fromWifiNetworkSpecifier = source.fromWifiNetworkSpecifier;
            meteredHint = source.meteredHint;
            mIsRepeaterEnabled = source.mIsRepeaterEnabled;
            meteredOverride = source.meteredOverride;
            useExternalScores = source.useExternalScores;

            lastConnectUid = source.lastConnectUid;
            lastUpdateUid = source.lastUpdateUid;
            creatorUid = source.creatorUid;
            creatorName = source.creatorName;
            lastUpdateName = source.lastUpdateName;
            peerWifiConfiguration = source.peerWifiConfiguration;

            lastConnected = source.lastConnected;
            lastDisconnected = source.lastDisconnected;
            lastUpdated = source.lastUpdated;
            numRebootsSinceLastUse = source.numRebootsSinceLastUse;
            numScorerOverride = source.numScorerOverride;
            numScorerOverrideAndSwitchedNetwork = source.numScorerOverrideAndSwitchedNetwork;
            numAssociation = source.numAssociation;
            allowAutojoin = source.allowAutojoin;
            numNoInternetAccessReports = source.numNoInternetAccessReports;
            noInternetAccessExpected = source.noInternetAccessExpected;
            shared = source.shared;
            recentFailure.setAssociationStatus(source.recentFailure.getAssociationStatus(),
                    source.recentFailure.getLastUpdateTimeSinceBootMillis());
            mRandomizedMacAddress = source.mRandomizedMacAddress;
            macRandomizationSetting = source.macRandomizationSetting;
            randomizedMacExpirationTimeMs = source.randomizedMacExpirationTimeMs;
            randomizedMacLastModifiedTimeMs = source.randomizedMacLastModifiedTimeMs;
            mIsSendDhcpHostnameEnabled = source.mIsSendDhcpHostnameEnabled;
            requirePmf = source.requirePmf;
            updateIdentifier = source.updateIdentifier;
            carrierId = source.carrierId;
            subscriptionId = source.subscriptionId;
            mPasspointUniqueId = source.mPasspointUniqueId;
            mSubscriptionGroup = source.mSubscriptionGroup;
            if (source.mBssidAllowlist != null) {
                mBssidAllowlist = new ArrayList<>(source.mBssidAllowlist);
            } else {
                mBssidAllowlist = null;
            }
            mIsDppConfigurator = source.mIsDppConfigurator;
            mDppPrivateEcKey = source.mDppPrivateEcKey.clone();
            mDppConnector = source.mDppConnector.clone();
            mDppCSignKey = source.mDppCSignKey.clone();
            mDppNetAccessKey = source.mDppNetAccessKey.clone();
            isCurrentlyConnected = source.isCurrentlyConnected;
            mIsUserSelected = source.mIsUserSelected;
            mHasPreSharedKeyChanged = source.hasPreSharedKeyChanged();
            mEncryptedPreSharedKey = source.mEncryptedPreSharedKey != null
                    ? source.mEncryptedPreSharedKey.clone() : new byte[0];
            mEncryptedPreSharedKeyIv = source.mEncryptedPreSharedKeyIv != null
                    ? source.mEncryptedPreSharedKeyIv.clone() : new byte[0];
            mIpProvisioningTimedOut = source.mIpProvisioningTimedOut;
            mVendorData = new ArrayList<>(source.mVendorData);
            mWifi7Enabled = source.mWifi7Enabled;
        }
    }

    /** Implement the Parcelable interface {@hide} */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(networkId);
        dest.writeInt(status);
        mNetworkSelectionStatus.writeToParcel(dest, flags);
        dest.writeString(SSID);
        dest.writeString(BSSID);
        dest.writeInt(apBand);
        dest.writeInt(apChannel);
        dest.writeString(FQDN);
        dest.writeString(providerFriendlyName);
        dest.writeInt(isHomeProviderNetwork ? 1 : 0);
        dest.writeInt(roamingConsortiumIds.length);
        for (long roamingConsortiumId : roamingConsortiumIds) {
            dest.writeLong(roamingConsortiumId);
        }
        dest.writeString(preSharedKey);
        for (String wepKey : wepKeys) {
            dest.writeString(wepKey);
        }
        dest.writeInt(wepTxKeyIndex);
        dest.writeInt(priority);
        dest.writeInt(mDeletionPriority);
        dest.writeInt(hiddenSSID ? 1 : 0);
        dest.writeInt(requirePmf ? 1 : 0);
        dest.writeString(updateIdentifier);

        writeBitSet(dest, allowedKeyManagement);
        writeBitSet(dest, allowedProtocols);
        writeBitSet(dest, allowedAuthAlgorithms);
        writeBitSet(dest, allowedPairwiseCiphers);
        writeBitSet(dest, allowedGroupCiphers);
        writeBitSet(dest, allowedGroupManagementCiphers);
        writeBitSet(dest, allowedSuiteBCiphers);

        dest.writeInt(mSecurityParamsList.size());
        mSecurityParamsList.forEach(params -> dest.writeParcelable(params, flags));

        dest.writeParcelable(enterpriseConfig, flags);

        dest.writeParcelable(mIpConfiguration, flags);
        dest.writeString(dhcpServer);
        dest.writeString(defaultGwMacAddress);
        dest.writeInt(validatedInternetAccess ? 1 : 0);
        dest.writeInt(isLegacyPasspointConfig ? 1 : 0);
        dest.writeInt(ephemeral ? 1 : 0);
        dest.writeInt(trusted ? 1 : 0);
        dest.writeInt(oemPaid ? 1 : 0);
        dest.writeInt(oemPrivate ? 1 : 0);
        dest.writeInt(carrierMerged ? 1 : 0);
        dest.writeInt(fromWifiNetworkSuggestion ? 1 : 0);
        dest.writeInt(fromWifiNetworkSpecifier ? 1 : 0);
        dest.writeInt(meteredHint ? 1 : 0);
        dest.writeBoolean(mIsRepeaterEnabled);
        dest.writeInt(meteredOverride);
        dest.writeInt(useExternalScores ? 1 : 0);
        dest.writeInt(creatorUid);
        dest.writeInt(lastConnectUid);
        dest.writeInt(lastUpdateUid);
        dest.writeString(creatorName);
        dest.writeString(lastUpdateName);
        dest.writeInt(numScorerOverride);
        dest.writeInt(numScorerOverrideAndSwitchedNetwork);
        dest.writeInt(numAssociation);
        dest.writeBoolean(allowAutojoin);
        dest.writeInt(numNoInternetAccessReports);
        dest.writeInt(noInternetAccessExpected ? 1 : 0);
        dest.writeInt(shared ? 1 : 0);
        dest.writeString(mPasspointManagementObjectTree);
        dest.writeInt(recentFailure.getAssociationStatus());
        dest.writeLong(recentFailure.getLastUpdateTimeSinceBootMillis());
        dest.writeParcelable(mRandomizedMacAddress, flags);
        dest.writeInt(macRandomizationSetting);
        dest.writeBoolean(mIsSendDhcpHostnameEnabled);
        dest.writeInt(osu ? 1 : 0);
        dest.writeLong(randomizedMacExpirationTimeMs);
        dest.writeLong(randomizedMacLastModifiedTimeMs);
        dest.writeInt(carrierId);
        dest.writeString(mPasspointUniqueId);
        dest.writeInt(subscriptionId);
        dest.writeBoolean(restricted);
        dest.writeParcelable(mSubscriptionGroup, flags);
        dest.writeList(mBssidAllowlist);
        dest.writeBoolean(mIsDppConfigurator);
        dest.writeByteArray(mDppPrivateEcKey);
        dest.writeByteArray(mDppConnector);
        dest.writeByteArray(mDppCSignKey);
        dest.writeByteArray(mDppNetAccessKey);
        dest.writeBoolean(isCurrentlyConnected);
        dest.writeBoolean(mIsUserSelected);
        dest.writeBoolean(mHasPreSharedKeyChanged);
        dest.writeByteArray(mEncryptedPreSharedKey);
        dest.writeByteArray(mEncryptedPreSharedKeyIv);
        dest.writeBoolean(mIpProvisioningTimedOut);
        dest.writeList(mVendorData);
        dest.writeBoolean(mWifi7Enabled);
    }

    /** Implement the Parcelable interface {@hide} */
    @SystemApi
    public static final @android.annotation.NonNull Creator<WifiConfiguration> CREATOR =
            new Creator<WifiConfiguration>() {
                public WifiConfiguration createFromParcel(Parcel in) {
                    WifiConfiguration config = new WifiConfiguration();
                    config.networkId = in.readInt();
                    config.status = in.readInt();
                    config.mNetworkSelectionStatus.readFromParcel(in);
                    config.SSID = in.readString();
                    config.BSSID = in.readString();
                    config.apBand = in.readInt();
                    config.apChannel = in.readInt();
                    config.FQDN = in.readString();
                    config.providerFriendlyName = in.readString();
                    config.isHomeProviderNetwork = in.readInt() != 0;
                    int numRoamingConsortiumIds = in.readInt();
                    config.roamingConsortiumIds = new long[numRoamingConsortiumIds];
                    for (int i = 0; i < numRoamingConsortiumIds; i++) {
                        config.roamingConsortiumIds[i] = in.readLong();
                    }
                    config.preSharedKey = in.readString();
                    for (int i = 0; i < config.wepKeys.length; i++) {
                        config.wepKeys[i] = in.readString();
                    }
                    config.wepTxKeyIndex = in.readInt();
                    config.priority = in.readInt();
                    config.mDeletionPriority = in.readInt();
                    config.hiddenSSID = in.readInt() != 0;
                    config.requirePmf = in.readInt() != 0;
                    config.updateIdentifier = in.readString();

                    config.allowedKeyManagement = readBitSet(in);
                    config.allowedProtocols = readBitSet(in);
                    config.allowedAuthAlgorithms = readBitSet(in);
                    config.allowedPairwiseCiphers = readBitSet(in);
                    config.allowedGroupCiphers = readBitSet(in);
                    config.allowedGroupManagementCiphers = readBitSet(in);
                    config.allowedSuiteBCiphers = readBitSet(in);

                    int numSecurityParams = in.readInt();
                    for (int i = 0; i < numSecurityParams; i++) {
                        config.mSecurityParamsList.add(
                                in.readParcelable(SecurityParams.class.getClassLoader()));
                    }

                    config.enterpriseConfig = in.readParcelable(
                            WifiEnterpriseConfig.class.getClassLoader());
                    config.setIpConfiguration(
                            in.readParcelable(IpConfiguration.class.getClassLoader()));
                    config.dhcpServer = in.readString();
                    config.defaultGwMacAddress = in.readString();
                    config.validatedInternetAccess = in.readInt() != 0;
                    config.isLegacyPasspointConfig = in.readInt() != 0;
                    config.ephemeral = in.readInt() != 0;
                    config.trusted = in.readInt() != 0;
                    config.oemPaid = in.readInt() != 0;
                    config.oemPrivate = in.readInt() != 0;
                    config.carrierMerged = in.readInt() != 0;
                    config.fromWifiNetworkSuggestion = in.readInt() != 0;
                    config.fromWifiNetworkSpecifier = in.readInt() != 0;
                    config.meteredHint = in.readInt() != 0;
                    config.mIsRepeaterEnabled = in.readBoolean();
                    config.meteredOverride = in.readInt();
                    config.useExternalScores = in.readInt() != 0;
                    config.creatorUid = in.readInt();
                    config.lastConnectUid = in.readInt();
                    config.lastUpdateUid = in.readInt();
                    config.creatorName = in.readString();
                    config.lastUpdateName = in.readString();
                    config.numScorerOverride = in.readInt();
                    config.numScorerOverrideAndSwitchedNetwork = in.readInt();
                    config.numAssociation = in.readInt();
                    config.allowAutojoin = in.readBoolean();
                    config.numNoInternetAccessReports = in.readInt();
                    config.noInternetAccessExpected = in.readInt() != 0;
                    config.shared = in.readInt() != 0;
                    config.mPasspointManagementObjectTree = in.readString();
                    config.recentFailure.setAssociationStatus(in.readInt(), in.readLong());
                    config.mRandomizedMacAddress = in.readParcelable(
                            MacAddress.class.getClassLoader());
                    config.macRandomizationSetting = in.readInt();
                    config.mIsSendDhcpHostnameEnabled = in.readBoolean();
                    config.osu = in.readInt() != 0;
                    config.randomizedMacExpirationTimeMs = in.readLong();
                    config.randomizedMacLastModifiedTimeMs = in.readLong();
                    config.carrierId = in.readInt();
                    config.mPasspointUniqueId = in.readString();
                    config.subscriptionId = in.readInt();
                    config.restricted = in.readBoolean();
                    config.mSubscriptionGroup = in.readParcelable(
                            ParcelUuid.class.getClassLoader());
                    config.mBssidAllowlist = in.readArrayList(MacAddress.class.getClassLoader());
                    config.mIsDppConfigurator = in.readBoolean();
                    config.mDppPrivateEcKey = in.createByteArray();
                    if (config.mDppPrivateEcKey == null) {
                        config.mDppPrivateEcKey = new byte[0];
                    }
                    config.mDppConnector = in.createByteArray();
                    if (config.mDppConnector == null) {
                        config.mDppConnector = new byte[0];
                    }
                    config.mDppCSignKey = in.createByteArray();
                    if (config.mDppCSignKey == null) {
                        config.mDppCSignKey = new byte[0];
                    }
                    config.mDppNetAccessKey = in.createByteArray();
                    if (config.mDppNetAccessKey == null) {
                        config.mDppNetAccessKey = new byte[0];
                    }
                    config.isCurrentlyConnected = in.readBoolean();
                    config.mIsUserSelected = in.readBoolean();
                    config.mHasPreSharedKeyChanged = in.readBoolean();
                    config.mEncryptedPreSharedKey = in.createByteArray();
                    if (config.mEncryptedPreSharedKey == null) {
                        config.mEncryptedPreSharedKey = new byte[0];
                    }
                    config.mEncryptedPreSharedKeyIv = in.createByteArray();
                    if (config.mEncryptedPreSharedKeyIv == null) {
                        config.mEncryptedPreSharedKeyIv = new byte[0];
                    }
                    config.mIpProvisioningTimedOut = in.readBoolean();
                    config.mVendorData = ParcelUtil.readOuiKeyedDataList(in);
                    config.mWifi7Enabled = in.readBoolean();
                    return config;
                }

                public WifiConfiguration[] newArray(int size) {
                    return new WifiConfiguration[size];
                }
            };

    /**
     * Passpoint Unique identifier
     * @hide
     */
    private String mPasspointUniqueId = null;

    /**
     * Set the Passpoint unique identifier
     * @param uniqueId Passpoint unique identifier to be set
     * @hide
     */
    public void setPasspointUniqueId(String uniqueId) {
        mPasspointUniqueId = uniqueId;
    }

    /**
     * Set the Passpoint unique identifier
     * @hide
     */
    public String getPasspointUniqueId() {
        return mPasspointUniqueId;
    }

    /**
     * If network is one of the most recently connected.
     * For framework internal use only. Do not parcel.
     * @hide
     */
    public boolean isMostRecentlyConnected = false;

    /**
     * Whether the network is currently connected or not.
     * Note: May be true even if {@link #status} is not CURRENT, since a config
     *       can be connected, but disabled for network selection.
     * TODO (b/235236813): This field may be redundant, since we have information
     *       like {@link #status} and quality network selection status. May need
     *       to clean up the fields used for network selection.
     * @hide
     */
    public boolean isCurrentlyConnected = false;

    private boolean mIsUserSelected = false;

    /**
     * Sets whether the network is connected by user selection or not.
     * @hide
     */
    public boolean setIsUserSelected(boolean isUserSelected) {
        return mIsUserSelected = isUserSelected;
    }

    /**
     * Whether the network is connected by user selection or not.
     * @hide
     */
    public boolean isUserSelected() {
        return mIsUserSelected;
    }

    /**
     * Whether the key mgmt indicates if the WifiConfiguration needs a preSharedKey or not.
     * @return true if preSharedKey is needed, false otherwise.
     * @hide
     */
    @Keep
    public boolean needsPreSharedKey() {
        for (SecurityParams params : mSecurityParamsList) {
            if (params.isSecurityType(SECURITY_TYPE_PSK)
                    || params.isSecurityType(SECURITY_TYPE_SAE)
                    || params.isSecurityType(SECURITY_TYPE_WAPI_PSK)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return if the encrypted data is present
     * @return true if encrypted data is present
     * @hide
     */
    public boolean hasEncryptedPreSharedKey() {
        if (mEncryptedPreSharedKey == null || mEncryptedPreSharedKeyIv == null) return false;
        return !(mEncryptedPreSharedKey.length == 0 && mEncryptedPreSharedKeyIv.length == 0);
    }

    /**
     * Set the encrypted data for preSharedKey
     * @param encryptedPreSharedKey encrypted preSharedKey
     * @param encryptedPreSharedKeyIv encrypted preSharedKey
     * @hide
     */
    public void setEncryptedPreSharedKey(byte[] encryptedPreSharedKey,
            byte[] encryptedPreSharedKeyIv) {
        mEncryptedPreSharedKey = encryptedPreSharedKey;
        mEncryptedPreSharedKeyIv = encryptedPreSharedKeyIv;
    }

    /**
     * Get the encrypted data
     *
     * @return encrypted data of the WifiConfiguration
     * @hide
     */
    public byte[] getEncryptedPreSharedKey() {
        return mEncryptedPreSharedKey;
    }

    /**
     * Get the encrypted data IV
     *
     * @return encrypted data IV of the WifiConfiguration
     * @hide
     */
    public byte[] getEncryptedPreSharedKeyIv() {
        return mEncryptedPreSharedKeyIv;
    }

    /**
     * Check whether the configuration's password has changed.
     * If true, the encrypted data is no longer valid.
     *
     * @return true if preSharedKey encryption is needed, false otherwise.
     * @hide
     */
    @Keep
    public boolean hasPreSharedKeyChanged() {
        return mHasPreSharedKeyChanged;
    }

    /**
     * Set whether the WifiConfiguration needs a preSharedKey encryption.
     *
     * @param changed true if preSharedKey is changed, false otherwise.
     * @hide
     */
    public void setHasPreSharedKeyChanged(boolean changed) {
        mHasPreSharedKeyChanged = changed;
        if (mHasPreSharedKeyChanged) {
            mEncryptedPreSharedKey = new byte[0];
            mEncryptedPreSharedKeyIv = new byte[0];
        }
    }

    /**
     * Get a unique key which represent this Wi-Fi configuration profile. If two profiles are for
     * the same Wi-Fi network, but from different providers (apps, carriers, or data subscriptions),
     * they would have different keys.
     * @return a unique key which represent this profile.
     * @hide
     */
    @SystemApi
    @NonNull public String getProfileKey() {
        if (!SdkLevel.isAtLeastS()) {
            return getKey();
        }
        if (mPasspointUniqueId != null) {
            return mPasspointUniqueId;
        }

        String key = getSsidAndSecurityTypeString();
        if (!shared) {
            key += "-" + UserHandle.getUserHandleForUid(creatorUid).getIdentifier();
        }
        if (fromWifiNetworkSuggestion) {
            key += "_" + creatorName + "-" + carrierId + "-" + subscriptionId;
        }

        return key;
    }

    /**
     * Get the default security type string.
     * @hide
     */
    public String getDefaultSecurityType() {
        String key;
        if (allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            key = KeyMgmt.strings[KeyMgmt.WPA_PSK];
        } else if (allowedKeyManagement.get(KeyMgmt.WPA_EAP)
                || allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            if (isWpa3EnterpriseConfiguration()) {
                key = "WPA3_EAP";
            } else {
                key = KeyMgmt.strings[KeyMgmt.WPA_EAP];
            }
        } else if (wepTxKeyIndex >= 0 && wepTxKeyIndex < wepKeys.length
                && wepKeys[wepTxKeyIndex] != null) {
            key = "WEP";
        } else if (allowedKeyManagement.get(KeyMgmt.OWE)) {
            key = KeyMgmt.strings[KeyMgmt.OWE];
        } else if (allowedKeyManagement.get(KeyMgmt.SAE)) {
            key = KeyMgmt.strings[KeyMgmt.SAE];
        } else if (allowedKeyManagement.get(KeyMgmt.SUITE_B_192)) {
            key = KeyMgmt.strings[KeyMgmt.SUITE_B_192];
        } else if (allowedKeyManagement.get(KeyMgmt.WAPI_PSK)) {
            key = KeyMgmt.strings[KeyMgmt.WAPI_PSK];
        } else if (allowedKeyManagement.get(KeyMgmt.WAPI_CERT)) {
            key = KeyMgmt.strings[KeyMgmt.WAPI_CERT];
        } else if (allowedKeyManagement.get(KeyMgmt.OSEN)) {
            key = KeyMgmt.strings[KeyMgmt.OSEN];
        } else if (allowedKeyManagement.get(KeyMgmt.DPP)) {
            key = KeyMgmt.strings[KeyMgmt.DPP];
        } else {
            key = KeyMgmt.strings[KeyMgmt.NONE];
        }
        return key;
    }

    /**
     * Get the security type name.
     *
     * @param securityType One of the following security types:
     * {@link #SECURITY_TYPE_OPEN},
     * {@link #SECURITY_TYPE_WEP},
     * {@link #SECURITY_TYPE_PSK},
     * {@link #SECURITY_TYPE_EAP},
     * {@link #SECURITY_TYPE_SAE},
     * {@link #SECURITY_TYPE_OWE},
     * {@link #SECURITY_TYPE_WAPI_PSK},
     * {@link #SECURITY_TYPE_WAPI_CERT},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE},
     * {@link #SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT},
     * {@link #SECURITY_TYPE_PASSPOINT_R1_R2},
     * {@link #SECURITY_TYPE_PASSPOINT_R3},
     * or {@link #SECURITY_TYPE_DPP}.
     * @return the name of the given type.
     * @hide
     */
    public static String getSecurityTypeName(@SecurityType int securityType) {
        if (securityType < SECURITY_TYPE_OPEN || SECURITY_TYPE_NUM < securityType) {
            return "unknown";
        }
        return SECURITY_TYPE_NAMES[securityType];
    }

    /**
     * Returns the key for storing the data usage bucket.
     *
     * Note: DO NOT change this function. It is used to be a key to store Wi-Fi data usage data.
     * Create a new function if we plan to change the key for Wi-Fi data usage and add the new key
     * to {@link #getAllNetworkKeys()}.
     *
     * @param securityType the security type corresponding to the target network.
     * @hide
     */
    public String getNetworkKeyFromSecurityType(@SecurityType int securityType) {
        if (mPasspointUniqueId != null) {
            // It might happen that there are two connections which use the same passpoint
            // coniguration but different sim card (maybe same carriers?). Add subscriptionId to be
            // the part of key to separate data in usage bucket.
            // But now we only show one WifiConfiguration entry in Wifi picker for this case.
            // It means that user only have a way to query usage with configuration on default SIM.
            // (We always connect to network with default SIM). So returns the key with associated
            // subscriptionId (the default one) first.
            return subscriptionId + "-" + mPasspointUniqueId;
        } else {
            String key = (!TextUtils.isEmpty(SSID) && SSID.charAt(0) != '\"'
                    ? SSID.toLowerCase() : SSID) + getSecurityTypeName(securityType);
            if (!shared) {
                key += "-" + UserHandle.getUserHandleForUid(creatorUid).getIdentifier();
            }
            if (fromWifiNetworkSuggestion) {
                key += "_" + creatorName + "-" + carrierId + "-" + subscriptionId;
            }
            return key;
        }
    }

    /**
     * Returns a list of all persistable network keys corresponding to this configuration.
     * There may be multiple keys since they are security-type specific and a configuration may
     * support multiple security types. The persistable key of a specific network connection may
     * be obtained from {@link WifiInfo#getNetworkKey()}.
     * An example of usage of such persistable network keys is to query the Wi-Fi data usage
     * corresponding to this configuration. See {@code NetworkTemplate} to know the detail.
     *
     * @hide
     */
    @SystemApi
    @NonNull
    public Set<String> getAllNetworkKeys() {
        Set<String> keys = new HashSet<>();
        for (SecurityParams securityParam : mSecurityParamsList) {
            keys.add(getNetworkKeyFromSecurityType(securityParam.getSecurityType()));
        }
        return keys;
    }

    /**
     * Set the subscription group uuid associated with current configuration.
     * @hide
     */
    public void setSubscriptionGroup(@Nullable ParcelUuid subscriptionGroup) {
        this.mSubscriptionGroup = subscriptionGroup;
    }

    /**
     * Get the subscription group uuid associated with current configuration.
     * @hide
     */
    public @Nullable ParcelUuid getSubscriptionGroup() {
        return this.mSubscriptionGroup;
    }

    /**
     * Return the vendor-provided configuration data, if it exists. See also {@link
     * #setVendorData(List)}
     *
     * @return Vendor configuration data, or empty list if it does not exist.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @NonNull
    @SystemApi
    public List<OuiKeyedData> getVendorData() {
        if (!SdkLevel.isAtLeastV()) {
            throw new UnsupportedOperationException();
        }
        return mVendorData;
    }

    /**
     * Set additional vendor-provided configuration data.
     *
     * Setting this field requires the MANAGE_WIFI_NETWORK_SELECTION permission. Otherwise,
     * if this data is set, the configuration will be rejected upon add or update.
     *
     * @param vendorData List of {@link OuiKeyedData} containing the vendor-provided
     *     configuration data. Note that multiple elements with the same OUI are allowed.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @SystemApi
    public void setVendorData(@NonNull List<OuiKeyedData> vendorData) {
        if (!SdkLevel.isAtLeastV()) {
            throw new UnsupportedOperationException();
        }
        Objects.requireNonNull(vendorData);
        mVendorData = new ArrayList<>(vendorData);
    }

    /**
     * Whether Wi-Fi 7 is enabled for this network.
     *
     * @return true if enabled; false otherwise
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public boolean isWifi7Enabled() {
        return mWifi7Enabled;
    }

    /**
     * Sets whether Wi-Fi 7 is enabled for this network.
     *
     * @param enabled true if enabled; false otherwise
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public void setWifi7Enabled(boolean enabled) {
        mWifi7Enabled = enabled;
    }
}

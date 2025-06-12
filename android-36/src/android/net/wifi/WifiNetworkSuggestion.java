/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.internal.util.Preconditions.checkNotNull;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.net.MacAddress;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.Build;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * The Network Suggestion object is used to provide a Wi-Fi network for consideration when
 * auto-connecting to networks. Apps cannot directly create this object, they must use
 * {@link WifiNetworkSuggestion.Builder#build()} to obtain an instance of this object.
 *<p>
 * Apps can provide a list of such networks to the platform using
 * {@link WifiManager#addNetworkSuggestions(List)}.
 */
public final class WifiNetworkSuggestion implements Parcelable {
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"RANDOMIZATION_"}, value = {
            RANDOMIZATION_PERSISTENT,
            RANDOMIZATION_NON_PERSISTENT})
    public @interface MacRandomizationSetting {}
    /**
     * Generate a randomized MAC from a secret seed and information from the Wi-Fi configuration
     * (SSID or Passpoint profile) and reuse it for all connections to this network. The
     * randomized MAC address for this network will stay the same for each subsequent association
     * until the device undergoes factory reset.
     */
    public static final int RANDOMIZATION_PERSISTENT = 0;
    /**
     * With this option, the randomized MAC address will periodically get re-randomized, and
     * the randomized MAC address will change if the suggestion is removed and then added back.
     */
    public static final int RANDOMIZATION_NON_PERSISTENT = 1;
    /**
     * Builder used to create {@link WifiNetworkSuggestion} objects.
     */
    public static final class Builder {
        private static final int UNASSIGNED_PRIORITY = -1;

        /**
         * Set WPA Enterprise type according to certificate security level.
         * This is for backward compatibility in R.
         */
        private static final int WPA3_ENTERPRISE_AUTO = 0;
        /** Set WPA Enterprise type to standard mode only. */
        private static final int WPA3_ENTERPRISE_STANDARD = 1;
        /** Set WPA Enterprise type to 192 bit mode only. */
        private static final int WPA3_ENTERPRISE_192_BIT = 2;

        /**
         * SSID of the network.
         */
        private WifiSsid mWifiSsid;
        /**
         * Optional BSSID within the network.
         */
        private MacAddress mBssid;
        /**
         * Whether this is an OWE network or not.
         */
        private boolean mIsEnhancedOpen;
        /**
         * Pre-shared key for use with WPA-PSK networks.
         */
        private @Nullable String mWpa2PskPassphrase;
        /**
         * Pre-shared key for use with WPA3-SAE networks.
         */
        private @Nullable String mWpa3SaePassphrase;
        /**
         * The enterprise configuration details specifying the EAP method,
         * certificates and other settings associated with the WPA/WPA2-Enterprise networks.
         */
        private @Nullable WifiEnterpriseConfig mWpa2EnterpriseConfig;
        /**
         * The enterprise configuration details specifying the EAP method,
         * certificates and other settings associated with the WPA3-Enterprise networks.
         */
        private @Nullable WifiEnterpriseConfig mWpa3EnterpriseConfig;
        /**
         * Indicate what type this WPA3-Enterprise network is.
         */
        private int mWpa3EnterpriseType = WPA3_ENTERPRISE_AUTO;
        /**
         * The passpoint config for use with Hotspot 2.0 network
         */
        private @Nullable PasspointConfiguration mPasspointConfiguration;
        /**
         * This is a network that does not broadcast its SSID, so an
         * SSID-specific probe request must be used for scans.
         */
        private boolean mIsHiddenSSID;
        /**
         * Whether app needs to log in to captive portal to obtain Internet access.
         */
        private boolean mIsAppInteractionRequired;
        /**
         * Whether user needs to log in to captive portal to obtain Internet access.
         */
        private boolean mIsUserInteractionRequired;
        /**
         * Whether this network is metered or not.
         */
        private int mMeteredOverride;
        /**
         * Priority of this network among other network suggestions from same priority group
         * provided by the app.
         * The higher the number, the higher the priority (i.e value of 0 = lowest priority).
         */
        private int mPriority;
        /**
         * Priority group ID, while suggestion priority will only effect inside the priority group.
         */
        private int mPriorityGroup;

        /**
         * The carrier ID identifies the operator who provides this network configuration.
         *    see {@link TelephonyManager#getSimCarrierId()}
         */
        private int mCarrierId;

        /**
         * The Subscription ID identifies the SIM card for which this network configuration is
         * valid.
         */
        private int mSubscriptionId;

        /**
         * Whether this network is shared credential with user to allow user manually connect.
         */
        private boolean mIsSharedWithUser;

        /**
         * Whether the setCredentialSharedWithUser have been called.
         */
        private boolean mIsSharedWithUserSet;

        /**
         * Whether this network is initialized with auto-join enabled (the default) or not.
         */
        private boolean mIsInitialAutojoinEnabled;

        /**
         * Pre-shared key for use with WAPI-PSK networks.
         */
        private @Nullable String mWapiPskPassphrase;

        /**
         * The enterprise configuration details specifying the EAP method,
         * certificates and other settings associated with the WAPI networks.
         */
        private @Nullable WifiEnterpriseConfig mWapiEnterpriseConfig;

        /**
         * Whether this network will be brought up as untrusted (TRUSTED capability bit removed).
         */
        private boolean mIsNetworkUntrusted;

        /**
         * Whether this network will be brought up as OEM paid (OEM_PAID capability bit added).
         */
        private boolean mIsNetworkOemPaid;

        /**
         * Whether this network will be brought up as OEM private (OEM_PRIVATE capability bit
         * added).
         */
        private boolean mIsNetworkOemPrivate;

        /**
         * Whether this network is a carrier merged network.
         */
        private boolean mIsCarrierMerged;

        /**
         * The MAC randomization strategy.
         */
        @MacRandomizationSetting
        private int mMacRandomizationSetting;

        /**
         * The SAE Hash-to-Element only mode.
         */
        private boolean mSaeH2eOnlyMode;

        /**
         * Whether this network will be brought up as restricted
         */
        private boolean mIsNetworkRestricted;

        /**
         * Whether enable Wi-Fi 7 for this network
         */
        private boolean mIsWifi7Enabled;


        /**
         * The Subscription group UUID identifies the SIM cards for which this network configuration
         * is valid.
         */
        private ParcelUuid mSubscriptionGroup;

        public Builder() {
            mBssid =  null;
            mIsEnhancedOpen = false;
            mWpa2PskPassphrase = null;
            mWpa3SaePassphrase = null;
            mWpa2EnterpriseConfig = null;
            mWpa3EnterpriseConfig = null;
            mPasspointConfiguration = null;
            mIsHiddenSSID = false;
            mIsAppInteractionRequired = false;
            mIsUserInteractionRequired = false;
            mMeteredOverride = WifiConfiguration.METERED_OVERRIDE_NONE;
            mIsSharedWithUser = true;
            mIsSharedWithUserSet = false;
            mIsInitialAutojoinEnabled = true;
            mPriority = UNASSIGNED_PRIORITY;
            mCarrierId = TelephonyManager.UNKNOWN_CARRIER_ID;
            mWapiPskPassphrase = null;
            mWapiEnterpriseConfig = null;
            mIsNetworkUntrusted = false;
            mIsNetworkOemPaid = false;
            mIsNetworkOemPrivate = false;
            mIsCarrierMerged = false;
            mPriorityGroup = 0;
            mMacRandomizationSetting = RANDOMIZATION_PERSISTENT;
            mSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            mSaeH2eOnlyMode = false;
            mIsNetworkRestricted = false;
            mSubscriptionGroup = null;
            mWifiSsid = null;
            mIsWifi7Enabled = true;
        }

        /**
         * Set the unicode SSID for the network.
         * <p>
         * <li>Overrides any previous value set using {@link #setSsid(String)}.</li>
         *
         * <p>
         * Note: use {@link #setWifiSsid(WifiSsid)} which supports both unicode and non-unicode
         * SSID.
         *
         * @param ssid The SSID of the network. It must be valid Unicode.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if the SSID is not valid unicode.
         */
        public @NonNull Builder setSsid(@NonNull String ssid) {
            checkNotNull(ssid);
            final CharsetEncoder unicodeEncoder = StandardCharsets.UTF_8.newEncoder();
            if (!unicodeEncoder.canEncode(ssid)) {
                throw new IllegalArgumentException("SSID is not a valid unicode string");
            }
            mWifiSsid = WifiSsid.fromUtf8Text(ssid);
            return this;
        }

        /**
         * Set the SSID for the network. {@link WifiSsid} support both unicode and non-unicode SSID.
         * <p>
         * <li>Overrides any previous value set using {@link #setWifiSsid(WifiSsid)}
         * or {@link #setSsid(String)}.</li>
         * <p>
         * Note: this method is the superset of the {@link #setSsid(String)}
         *
         * @param wifiSsid The SSID of the network, in {@link WifiSsid} format.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if the wifiSsid is invalid.
         */
        public @NonNull Builder setWifiSsid(@NonNull WifiSsid wifiSsid) {
            checkNotNull(wifiSsid);
            if (wifiSsid.getBytes().length == 0) {
                throw new IllegalArgumentException("Empty WifiSsid is invalid");
            }
            mWifiSsid = WifiSsid.fromBytes(wifiSsid.getBytes());
            return this;
        }

        /**
         * Set the BSSID to use for filtering networks from scan results. Will only match network
         * whose BSSID is identical to the specified value.
         * <p>
         * <li Sets a specific BSSID for the network suggestion. If set, only the specified BSSID
         * with the specified SSID will be considered for connection.
         * <li>If set, only the specified BSSID with the specified SSID will be considered for
         * connection.</li>
         * <li>If not set, all BSSIDs with the specified SSID will be considered for connection.
         * </li>
         * <li>Overrides any previous value set using {@link #setBssid(MacAddress)}.</li>
         *
         * @param bssid BSSID of the network.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setBssid(@NonNull MacAddress bssid) {
            checkNotNull(bssid);
            mBssid = MacAddress.fromBytes(bssid.toByteArray());
            return this;
        }

        /**
         * Specifies whether this represents an Enhanced Open (OWE) network.
         *
         * @param isEnhancedOpen {@code true} to indicate that the network used enhanced open,
         *                       {@code false} otherwise.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setIsEnhancedOpen(boolean isEnhancedOpen) {
            mIsEnhancedOpen = isEnhancedOpen;
            return this;
        }

        /**
         * Set the ASCII WPA2 passphrase for this network. Needed for authenticating to
         * WPA2-PSK networks.
         *
         * @param passphrase passphrase of the network.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if the passphrase is not ASCII encodable.
         */
        public @NonNull Builder setWpa2Passphrase(@NonNull String passphrase) {
            checkNotNull(passphrase);
            final CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder();
            if (!asciiEncoder.canEncode(passphrase)) {
                throw new IllegalArgumentException("passphrase not ASCII encodable");
            }
            mWpa2PskPassphrase = passphrase;
            return this;
        }

        /**
         * Set the ASCII WPA3 passphrase for this network. Needed for authenticating to WPA3-SAE
         * networks.
         *
         * @param passphrase passphrase of the network.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if the passphrase is not ASCII encodable.
         */
        public @NonNull Builder setWpa3Passphrase(@NonNull String passphrase) {
            checkNotNull(passphrase);
            final CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder();
            if (!asciiEncoder.canEncode(passphrase)) {
                throw new IllegalArgumentException("passphrase not ASCII encodable");
            }
            mWpa3SaePassphrase = passphrase;
            return this;
        }

        /**
         * Set the associated enterprise configuration for this network. Needed for authenticating
         * to WPA2 enterprise networks. See {@link WifiEnterpriseConfig} for description.
         *
         * @param enterpriseConfig Instance of {@link WifiEnterpriseConfig}.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException If configuration uses server certificate but validation
         *                                  is not enabled. See {@link WifiEnterpriseConfig#isServerCertValidationEnabled()}
         */
        public @NonNull Builder setWpa2EnterpriseConfig(
                @NonNull WifiEnterpriseConfig enterpriseConfig) {
            checkNotNull(enterpriseConfig);
            if (enterpriseConfig.isEapMethodServerCertUsed()
                    && !enterpriseConfig.isMandatoryParameterSetForServerCertValidation()) {
                throw new IllegalArgumentException("Enterprise configuration mandates server "
                        + "certificate but validation is not enabled.");
            }
            mWpa2EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
            return this;
        }

        /**
         * Set the associated enterprise configuration for this network. Needed for authenticating
         * to WPA3-Enterprise networks (standard and 192-bit security). See
         * {@link WifiEnterpriseConfig} for description. For 192-bit security networks, both the
         * client and CA certificates must be provided, and must be of type of either
         * sha384WithRSAEncryption (OID 1.2.840.113549.1.1.12) or ecdsa-with-SHA384
         * (OID 1.2.840.10045.4.3.3).
         *
         * @deprecated use {@link #setWpa3EnterpriseStandardModeConfig(WifiEnterpriseConfig)} or
         * {@link #setWpa3Enterprise192BitModeConfig(WifiEnterpriseConfig)} to specify
         * WPA3-Enterprise type explicitly.
         *
         * @param enterpriseConfig Instance of {@link WifiEnterpriseConfig}.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException If configuration uses server certificate but validation
         *                                  is not enabled. See {@link WifiEnterpriseConfig#isServerCertValidationEnabled()}
         */
        @Deprecated
        public @NonNull Builder setWpa3EnterpriseConfig(
                @NonNull WifiEnterpriseConfig enterpriseConfig) {
            checkNotNull(enterpriseConfig);
            if (enterpriseConfig.isEapMethodServerCertUsed()
                    && !enterpriseConfig.isMandatoryParameterSetForServerCertValidation()) {
                throw new IllegalArgumentException("Enterprise configuration mandates server "
                        + "certificate but validation is not enabled.");
            }
            mWpa3EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
            return this;
        }

        /**
         * Set the associated enterprise configuration for this network. Needed for authenticating
         * to WPA3-Enterprise standard networks. See {@link WifiEnterpriseConfig} for description.
         * For WPA3-Enterprise in 192-bit security mode networks,
         * see {@link #setWpa3Enterprise192BitModeConfig(WifiEnterpriseConfig)} for description.
         *
         * @param enterpriseConfig Instance of {@link WifiEnterpriseConfig}.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException If configuration uses server certificate but validation
         *                                  is not enabled. See {@link WifiEnterpriseConfig#isServerCertValidationEnabled()}
         */
        public @NonNull Builder setWpa3EnterpriseStandardModeConfig(
                @NonNull WifiEnterpriseConfig enterpriseConfig) {
            checkNotNull(enterpriseConfig);
            if (enterpriseConfig.isEapMethodServerCertUsed()
                    && !enterpriseConfig.isMandatoryParameterSetForServerCertValidation()) {
                throw new IllegalArgumentException("Enterprise configuration mandates server "
                        + "certificate but validation is not enabled.");
            }
            mWpa3EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
            mWpa3EnterpriseType = WPA3_ENTERPRISE_STANDARD;
            return this;
        }

        /**
         * Set the associated enterprise configuration for this network. Needed for authenticating
         * to WPA3-Enterprise in 192-bit security mode networks. See {@link WifiEnterpriseConfig}
         * for description. Both the client and CA certificates must be provided,
         * and must be of type of either sha384WithRSAEncryption with key length of 3072bit or
         * more (OID 1.2.840.113549.1.1.12), or ecdsa-with-SHA384 with key length of 384bit or
         * more (OID 1.2.840.10045.4.3.3).
         *
         * @param enterpriseConfig Instance of {@link WifiEnterpriseConfig}.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if the EAP type or certificates do not
         *                                  meet 192-bit mode requirements.
         */
        public @NonNull Builder setWpa3Enterprise192BitModeConfig(
                @NonNull WifiEnterpriseConfig enterpriseConfig) {
            checkNotNull(enterpriseConfig);
            if (enterpriseConfig.getEapMethod() != WifiEnterpriseConfig.Eap.TLS) {
                throw new IllegalArgumentException("The 192-bit mode network type must be TLS");
            }
            if (!WifiEnterpriseConfig.isSuiteBCipherCert(
                    enterpriseConfig.getClientCertificate())) {
                throw new IllegalArgumentException(
                    "The client certificate does not meet 192-bit mode requirements.");
            }
            if (!WifiEnterpriseConfig.isSuiteBCipherCert(
                    enterpriseConfig.getCaCertificate())) {
                throw new IllegalArgumentException(
                    "The CA certificate does not meet 192-bit mode requirements.");
            }

            mWpa3EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
            mWpa3EnterpriseType = WPA3_ENTERPRISE_192_BIT;
            return this;
        }

        /**
         * Set the associated Passpoint configuration for this network. Needed for authenticating
         * to Hotspot 2.0 networks. See {@link PasspointConfiguration} for description.
         *
         * @param passpointConfig Instance of {@link PasspointConfiguration}.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if passpoint configuration is invalid.
         */
        public @NonNull Builder setPasspointConfig(
                @NonNull PasspointConfiguration passpointConfig) {
            checkNotNull(passpointConfig);
            if (!passpointConfig.validate()) {
                throw new IllegalArgumentException("Passpoint configuration is invalid");
            }
            mPasspointConfiguration = new PasspointConfiguration(passpointConfig);
            return this;
        }

        /**
         * Set the carrier ID of the network operator. The carrier ID associates a Suggested
         * network with a specific carrier (and therefore SIM). The carrier ID must be provided
         * for any network which uses the SIM-based authentication: e.g. EAP-SIM, EAP-AKA,
         * EAP-AKA', and EAP-PEAP with SIM-based phase 2 authentication.
         * @param carrierId see {@link TelephonyManager#getSimCarrierId()}.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         *
         * @hide
         */
        @SystemApi
        @RequiresPermission(android.Manifest.permission.NETWORK_CARRIER_PROVISIONING)
        public @NonNull Builder setCarrierId(int carrierId) {
            mCarrierId = carrierId;
            return this;
        }

        /**
         * Configure the suggestion to only be used with the SIM identified by the subscription
         * ID specified in this method. The suggested network will only be used by that SIM and
         * no other SIM - even from the same carrier.
         * <p>
         * The caller is restricted to be either of:
         * <li>A carrier provisioning app (which holds the
         * {@code android.Manifest.permission#NETWORK_CARRIER_PROVISIONING} permission).
         * <li>A carrier-privileged app - which is restricted to only specify a subscription ID
         * which belong to the same carrier which signed the app, see
         * {@link TelephonyManager#hasCarrierPrivileges()}.
         * <p>
         * Specifying a subscription ID which doesn't match these restriction will cause the
         * suggestion to be rejected with the error code
         * {@link WifiManager#STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED}.
         *
         * Only one of the {@link #setSubscriptionGroup(ParcelUuid)} and
         * {@link #setSubscriptionId(int)} should be called for a suggestion.
         *
         * @param subscriptionId subscription ID see {@link SubscriptionInfo#getSubscriptionId()}
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if subscriptionId equals to {@link SubscriptionManager#INVALID_SUBSCRIPTION_ID}
         */
        @RequiresApi(Build.VERSION_CODES.S)
        public @NonNull Builder setSubscriptionId(int subscriptionId) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            if (subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                throw new IllegalArgumentException("Subscription Id is invalid");
            }
            mSubscriptionId = subscriptionId;
            return this;
        }

        /**
         * Configure the suggestion to only be used with the SIMs that belong to the Subscription
         * Group specified in this method. The suggested network will only be used by the SIM in
         * this Subscription Group and no other SIMs - even from the same carrier.
         * <p>
         * The caller is restricted to be either of:
         * <li>A carrier provisioning app.
         * <li>A carrier-privileged app - which is restricted to only specify a subscription ID
         * which belong to the same carrier which signed the app, see
         * {@link TelephonyManager#hasCarrierPrivileges()}.
         * <p>
         * Specifying a subscription group which doesn't match these restriction will cause the
         * suggestion to be rejected with the error code
         * {@link WifiManager#STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED}.
         *
         * Only one of the {@link #setSubscriptionGroup(ParcelUuid)} and
         * {@link #setSubscriptionId(int)} should be called for a suggestion.
         *
         * @param groupUuid Subscription group UUID see
         * {@link SubscriptionManager#createSubscriptionGroup(List)}
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if group UUID is {@code null}.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public @NonNull Builder setSubscriptionGroup(@NonNull ParcelUuid groupUuid) {
            if (!SdkLevel.isAtLeastT()) {
                throw new UnsupportedOperationException();
            }
            if (groupUuid == null) {
                throw new IllegalArgumentException("SubscriptionGroup is invalid");
            }
            mSubscriptionGroup = groupUuid;
            return this;
        }

        /**
         * Suggested networks are considered as part of a pool of all suggested networks and other
         * networks (e.g. saved networks) - one of which will be selected.
         * <ul>
         * <li> Any app can suggest any number of networks. </li>
         * <li> Priority: only the highest priority (0 being the lowest) currently visible suggested
         * network or networks (multiple suggested networks may have the same priority) are added to
         * the network selection pool.</li>
         * </ul>
         * <p>
         * However, this restricts a suggesting app to have a single group of networks which can be
         * prioritized. In some circumstances multiple groups are useful: for instance, suggesting
         * networks for 2 different SIMs - each of which may have its own priority order.
         * <p>
         * Priority group: creates a separate priority group. Only the highest priority, currently
         * visible suggested network or networks, within each priority group are included in the
         * network selection pool.
         * <p>
         * Specify an arbitrary integer only used as the priority group. Use with
         * {@link #setPriority(int)}.
         *
         * @param priorityGroup priority group id, if not set default is 0.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setPriorityGroup(@IntRange(from = 0) int priorityGroup) {
            mPriorityGroup = priorityGroup;
            return this;
        }

        /**
         * Set the ASCII WAPI passphrase for this network. Needed for authenticating to
         * WAPI-PSK networks.
         *
         * @param passphrase passphrase of the network.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if the passphrase is not ASCII encodable.
         *
         */
        public @NonNull Builder setWapiPassphrase(@NonNull String passphrase) {
            checkNotNull(passphrase);
            final CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder();
            if (!asciiEncoder.canEncode(passphrase)) {
                throw new IllegalArgumentException("passphrase not ASCII encodable");
            }
            mWapiPskPassphrase = passphrase;
            return this;
        }

        /**
         * Set the associated enterprise configuration for this network. Needed for authenticating
         * to WAPI-CERT networks. See {@link WifiEnterpriseConfig} for description.
         *
         * @param enterpriseConfig Instance of {@link WifiEnterpriseConfig}.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setWapiEnterpriseConfig(
                @NonNull WifiEnterpriseConfig enterpriseConfig) {
            checkNotNull(enterpriseConfig);
            mWapiEnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
            return this;
        }

        /**
         * Specifies whether this represents a hidden network.
         * <p>
         * <li>If not set, defaults to false (i.e not a hidden network).</li>
         *
         * @param isHiddenSsid {@code true} to indicate that the network is hidden, {@code false}
         *                     otherwise.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setIsHiddenSsid(boolean isHiddenSsid) {
            mIsHiddenSSID = isHiddenSsid;
            return this;
        }

        /**
         * Specifies the MAC randomization method.
         * <p>
         * Suggested networks will never use the device (factory) MAC address to associate to the
         * network - instead they use a locally generated random MAC address. This method controls
         * the strategy for generating the random MAC address. If not set, defaults to
         * {@link #RANDOMIZATION_PERSISTENT}.
         *
         * @param macRandomizationSetting - one of {@code RANDOMIZATION_*} values
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setMacRandomizationSetting(
                @MacRandomizationSetting int macRandomizationSetting) {
            switch (macRandomizationSetting) {
                case RANDOMIZATION_PERSISTENT:
                case RANDOMIZATION_NON_PERSISTENT:
                    mMacRandomizationSetting = macRandomizationSetting;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            return this;
        }

        /**
         * Specifies whether the app needs to log in to a captive portal to obtain Internet access.
         * <p>
         * This will dictate if the directed broadcast
         * {@link WifiManager#ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION} will be sent to the
         * app after successfully connecting to the network.
         * Use this for captive portal type networks where the app needs to authenticate the user
         * before the device can access the network.
         * <p>
         * <li>If not set, defaults to false (i.e no app interaction required).</li>
         *
         * @param isAppInteractionRequired {@code true} to indicate that app interaction is
         *                                 required, {@code false} otherwise.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setIsAppInteractionRequired(boolean isAppInteractionRequired) {
            mIsAppInteractionRequired = isAppInteractionRequired;
            return this;
        }

        /**
         * Specifies whether the user needs to log in to a captive portal to obtain Internet access.
         * <p>
         * <li>If not set, defaults to false (i.e no user interaction required).</li>
         *
         * @param isUserInteractionRequired {@code true} to indicate that user interaction is
         *                                  required, {@code false} otherwise.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setIsUserInteractionRequired(boolean isUserInteractionRequired) {
            mIsUserInteractionRequired = isUserInteractionRequired;
            return this;
        }

        /**
         * Specify the priority of this network among other network suggestions provided by the same
         * app and within the same priority group, see {@link #setPriorityGroup(int)}. Priorities
         * have no impact on suggestions by other apps or suggestions from the same app using a
         * different priority group. The higher the number, the higher the priority
         * (i.e value of 0 = lowest priority). If not set, defaults to a lower priority than any
         * assigned priority.
         *
         * @param priority Integer number representing the priority among suggestions by the app.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if the priority value is negative.
         */
        public @NonNull Builder setPriority(@IntRange(from = 0) int priority) {
            if (priority < 0) {
                throw new IllegalArgumentException("Invalid priority value " + priority);
            }
            mPriority = priority;
            return this;
        }

        /**
         * Specifies whether this network is metered.
         * <p>
         * <li>If not set, defaults to detect automatically.</li>
         *
         * @param isMetered {@code true} to indicate that the network is metered, {@code false}
         *                  for not metered.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setIsMetered(boolean isMetered) {
            if (isMetered) {
                mMeteredOverride = WifiConfiguration.METERED_OVERRIDE_METERED;
            } else {
                mMeteredOverride = WifiConfiguration.METERED_OVERRIDE_NOT_METERED;
            }
            return this;
        }

        /**
         * Specifies whether the network credentials provided with this suggestion can be used by
         * the user to explicitly (manually) connect to this network. If true this network will
         * appear in the Wi-Fi Picker (in Settings) and the user will be able to select and connect
         * to it with the provided credentials. If false, the user will need to enter network
         * credentials and the resulting configuration will become a user saved network.
         * <p>
         * <li>Note: Only valid for secure (non-open) networks.
         * <li>If not set, defaults to true (i.e. allow user to manually connect) for secure
         * networks and false for open networks.</li>
         *
         * @param isShared {@code true} to indicate that the credentials may be used by the user to
         *                              manually connect to the network, {@code false} otherwise.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setCredentialSharedWithUser(boolean isShared) {
            mIsSharedWithUser = isShared;
            mIsSharedWithUserSet = true;
            return this;
        }

        /**
         * Specifies whether the suggestion is created with auto-join enabled or disabled. The
         * user may modify the auto-join configuration of a suggestion directly once the device
         * associates to the network.
         * <p>
         * If auto-join is initialized as disabled the user may still be able to manually connect
         * to the network. Therefore, disabling auto-join only makes sense if
         * {@link #setCredentialSharedWithUser(boolean)} is set to true (the default) which
         * itself implies a secure (non-open) network.
         * <p>
         * If not set, defaults to true (i.e. auto-join is initialized as enabled).
         *
         * @param enabled true for initializing with auto-join enabled (the default), false to
         *                initializing with auto-join disabled.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setIsInitialAutojoinEnabled(boolean enabled) {
            mIsInitialAutojoinEnabled = enabled;
            return this;
        }

        /**
         * Specifies whether the system will bring up the network (if selected) as untrusted. An
         * untrusted network has its {@link NetworkCapabilities#NET_CAPABILITY_TRUSTED}
         * capability removed. The Wi-Fi network selection process may use this information to
         * influence priority of the suggested network for Wi-Fi network selection (most likely to
         * reduce it). The connectivity service may use this information to influence the overall
         * network configuration of the device.
         * <p>
         * <li> These suggestions are only considered for network selection if a
         * {@link NetworkRequest} without {@link NetworkCapabilities#NET_CAPABILITY_TRUSTED}
         * capability is filed.
         * <li> An untrusted network's credentials may not be shared with the user using
         * {@link #setCredentialSharedWithUser(boolean)}.</li>
         * <li> If not set, defaults to false (i.e. network is trusted).</li>
         *
         * @param isUntrusted Boolean indicating whether the network should be brought up untrusted
         *                    (if true) or trusted (if false).
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setUntrusted(boolean isUntrusted) {
            mIsNetworkUntrusted = isUntrusted;
            return this;
        }

        /**
         * Specifies whether the system will bring up the network (if selected) as restricted. A
         * restricted network has its {@link NetworkCapabilities#NET_CAPABILITY_NOT_RESTRICTED}
         * capability removed. The Wi-Fi network selection process may use this information to
         * influence priority of the suggested network for Wi-Fi network selection (most likely to
         * reduce it). The connectivity service may use this information to influence the overall
         * network configuration of the device.
         * <p>
         * <li> These suggestions are only considered for network selection if a
         * {@link NetworkRequest} without {@link NetworkCapabilities#NET_CAPABILITY_NOT_RESTRICTED}
         * capability is filed.
         * <li> A restricted network's credentials may not be shared with the user using
         * {@link #setCredentialSharedWithUser(boolean)}.</li>
         * <li> If not set, defaults to false (i.e. network is unrestricted).</li>
         *
         * @param isRestricted Boolean indicating whether the network should be brought up
         *                     restricted (if true) or unrestricted (if false).
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setRestricted(boolean isRestricted) {
            mIsNetworkRestricted = isRestricted;
            return this;
        }

        /**
         * Sets whether Wi-Fi 7 is enabled for this network.
         *
         * @param enabled Enable Wi-Fi 7 if true, otherwise disable Wi-Fi 7
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public @NonNull Builder setWifi7Enabled(boolean enabled) {
            mIsWifi7Enabled = enabled;
            return this;
        }
        /**
         * Specifies whether the system will bring up the network (if selected) as OEM paid. An
         * OEM paid network has {@link NetworkCapabilities#NET_CAPABILITY_OEM_PAID} capability
         * added.
         * Note:
         * <li>The connectivity service may use this information to influence the overall
         * network configuration of the device. This network is typically only available to system
         * apps.
         * <li>On devices which do not support concurrent connection (indicated via
         * {@link WifiManager#isStaConcurrencyForRestrictedConnectionsSupported()}), Wi-Fi
         * network selection process may use this information to influence priority of the
         * suggested network for Wi-Fi network selection (most likely to reduce it).
         * <li>On devices which support concurrent connections (indicated via
         * {@link WifiManager#isStaConcurrencyForRestrictedConnectionsSupported()}), these
         * OEM paid networks may be brought up as a secondary concurrent connection (primary
         * connection will be used for networks available to the user and all apps.
         * <p>
         * <li> An OEM paid network's credentials may not be shared with the user using
         * {@link #setCredentialSharedWithUser(boolean)}.</li>
         * <li> These suggestions are only considered for network selection if a
         * {@link NetworkRequest} with {@link NetworkCapabilities#NET_CAPABILITY_OEM_PAID}
         * capability is filed.
         * <li> Each suggestion can have both {@link #setOemPaid(boolean)} and
         * {@link #setOemPrivate(boolean)} set if the app wants these suggestions considered
         * for creating either an OEM paid network or OEM private network determined based on
         * the {@link NetworkRequest} that is active.
         * <li> If not set, defaults to false (i.e. network is not OEM paid).</li>
         *
         * @param isOemPaid Boolean indicating whether the network should be brought up as OEM paid
         *                  (if true) or not OEM paid (if false).
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @hide
         */
        @SystemApi
        @RequiresApi(Build.VERSION_CODES.S)
        public @NonNull Builder setOemPaid(boolean isOemPaid) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            mIsNetworkOemPaid = isOemPaid;
            return this;
        }

        /**
         * Specifies whether the system will bring up the network (if selected) as OEM private. An
         * OEM private network has {@link NetworkCapabilities#NET_CAPABILITY_OEM_PRIVATE} capability
         * added.
         * Note:
         * <li>The connectivity service may use this information to influence the overall
         * network configuration of the device. This network is typically only available to system
         * apps.
         * <li>On devices which do not support concurrent connection (indicated via
         * {@link WifiManager#isStaConcurrencyForRestrictedConnectionsSupported()}), Wi-Fi
         * network selection process may use this information to influence priority of the suggested
         * network for Wi-Fi network selection (most likely to reduce it).
         * <li>On devices which support concurrent connections (indicated via
         * {@link WifiManager#isStaConcurrencyForRestrictedConnectionsSupported()}), these OEM
         * private networks may be brought up as a secondary concurrent connection (primary
         * connection will be used for networks available to the user and all apps.
         * <p>
         * <li> An OEM private network's credentials may not be shared with the user using
         * {@link #setCredentialSharedWithUser(boolean)}.</li>
         * <li> These suggestions are only considered for network selection if a
         * {@link NetworkRequest} with {@link NetworkCapabilities#NET_CAPABILITY_OEM_PRIVATE}
         * capability is filed.
         * <li> Each suggestion can have both {@link #setOemPaid(boolean)} and
         * {@link #setOemPrivate(boolean)} set if the app wants these suggestions considered
         * for creating either an OEM paid network or OEM private network determined based on
         * the {@link NetworkRequest} that is active.
         * <li> If not set, defaults to false (i.e. network is not OEM private).</li>
         *
         * @param isOemPrivate Boolean indicating whether the network should be brought up as OEM
         *                     private (if true) or not OEM private (if false).
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @hide
         */
        @SystemApi
        @RequiresApi(Build.VERSION_CODES.S)
        public @NonNull Builder setOemPrivate(boolean isOemPrivate) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            mIsNetworkOemPrivate = isOemPrivate;
            return this;
        }

        /**
         * Specifies whether the suggestion represents a carrier merged network. A carrier merged
         * Wi-Fi network is treated as part of the mobile carrier network. Such configuration may
         * impact the user interface and data usage accounting.
         * <p>
         * Only carriers with the
         * {@link android.telephony.CarrierConfigManager#KEY_CARRIER_PROVISIONS_WIFI_MERGED_NETWORKS_BOOL}
         * flag set to {@code true} may use this API.
         * <p>
         * <li>A suggestion marked as carrier merged must be metered enterprise network with a valid
         * subscription Id set.
         * @see #setIsMetered(boolean)
         * @see #setSubscriptionId(int)
         * @see #setWpa2EnterpriseConfig(WifiEnterpriseConfig)
         * @see #setWpa3Enterprise192BitModeConfig(WifiEnterpriseConfig)
         * @see #setWpa3EnterpriseStandardModeConfig(WifiEnterpriseConfig)
         * @see #setPasspointConfig(PasspointConfiguration)
         * </li>
         * <li>If not set, defaults to false (i.e. not a carrier merged network.)</li>
         * </p>
         * @param isCarrierMerged Boolean indicating whether the network is treated a carrier
         *                               merged network (if true) or non-merged network (if false);
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        public @NonNull Builder setCarrierMerged(boolean isCarrierMerged) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            mIsCarrierMerged = isCarrierMerged;
            return this;
        }

        /**
         * Specifies whether the suggestion represents an SAE network which only
         * accepts Hash-to-Element mode.
         * If this is enabled, Hunting & Pecking mode is disabled and only Hash-to-Element
         * mode is used for this network.
         * This is only valid for an SAE network which is configured using the
         * {@link #setWpa3Passphrase}.
         * Before calling this API, the application should check Hash-to-Element support using
         * {@link WifiManager#isWpa3SaeH2eSupported()}.
         *
         * @param enable Boolean indicating whether the network only accepts Hash-to-Element mode,
         *        default is false.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        public @NonNull Builder setIsWpa3SaeH2eOnlyModeEnabled(boolean enable) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            mSaeH2eOnlyMode = enable;
            return this;
        }

        private void setSecurityParamsInWifiConfiguration(
                @NonNull WifiConfiguration configuration) {
            if (!TextUtils.isEmpty(mWpa2PskPassphrase)) { // WPA-PSK network.
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_PSK);
                // WifiConfiguration.preSharedKey needs quotes around ASCII password.
                configuration.preSharedKey = "\"" + mWpa2PskPassphrase + "\"";
            } else if (!TextUtils.isEmpty(mWpa3SaePassphrase)) { // WPA3-SAE network.
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_SAE);
                // WifiConfiguration.preSharedKey needs quotes around ASCII password.
                configuration.preSharedKey = "\"" + mWpa3SaePassphrase + "\"";
                if (mSaeH2eOnlyMode) configuration.enableSaeH2eOnlyMode(mSaeH2eOnlyMode);
            } else if (mWpa2EnterpriseConfig != null) { // WPA-EAP network
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_EAP);
                configuration.enterpriseConfig = mWpa2EnterpriseConfig;
            } else if (mWpa3EnterpriseConfig != null) { // WPA3-Enterprise
                if (mWpa3EnterpriseType == WPA3_ENTERPRISE_AUTO
                        && mWpa3EnterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.TLS
                        && WifiEnterpriseConfig.isSuiteBCipherCert(
                        mWpa3EnterpriseConfig.getClientCertificate())
                        && WifiEnterpriseConfig.isSuiteBCipherCert(
                        mWpa3EnterpriseConfig.getCaCertificate())) {
                    // WPA3-Enterprise in 192-bit security mode
                    configuration.setSecurityParams(
                            WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT);
                } else if (mWpa3EnterpriseType == WPA3_ENTERPRISE_192_BIT) {
                    // WPA3-Enterprise in 192-bit security mode
                    configuration.setSecurityParams(
                            WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT);
                } else {
                    // WPA3-Enterprise
                    configuration.setSecurityParams(
                            WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE);
                }
                configuration.enterpriseConfig = mWpa3EnterpriseConfig;
            } else if (mIsEnhancedOpen) { // OWE network
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OWE);
            } else if (!TextUtils.isEmpty(mWapiPskPassphrase)) { // WAPI-PSK network.
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_WAPI_PSK);
                // WifiConfiguration.preSharedKey needs quotes around ASCII password.
                configuration.preSharedKey = "\"" + mWapiPskPassphrase + "\"";
            } else if (mWapiEnterpriseConfig != null) { // WAPI-CERT network
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_WAPI_CERT);
                configuration.enterpriseConfig = mWapiEnterpriseConfig;
            } else { // Open network
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OPEN);
            }
        }

        /**
         * Helper method to build WifiConfiguration object from the builder.
         * @return Instance of {@link WifiConfiguration}.
         */
        private WifiConfiguration buildWifiConfiguration() {
            final WifiConfiguration wifiConfiguration = new WifiConfiguration();
            // WifiConfiguration.SSID needs quotes around unicode SSID.
            wifiConfiguration.SSID = mWifiSsid.toString();
            if (mBssid != null) {
                wifiConfiguration.BSSID = mBssid.toString();
            }

            setSecurityParamsInWifiConfiguration(wifiConfiguration);

            wifiConfiguration.hiddenSSID = mIsHiddenSSID;
            wifiConfiguration.priority = mPriority;
            wifiConfiguration.meteredOverride = mMeteredOverride;
            wifiConfiguration.carrierId = mCarrierId;
            wifiConfiguration.trusted = !mIsNetworkUntrusted;
            wifiConfiguration.oemPaid = mIsNetworkOemPaid;
            wifiConfiguration.oemPrivate = mIsNetworkOemPrivate;
            wifiConfiguration.carrierMerged = mIsCarrierMerged;
            wifiConfiguration.macRandomizationSetting =
                    mMacRandomizationSetting == RANDOMIZATION_NON_PERSISTENT
                    ? WifiConfiguration.RANDOMIZATION_NON_PERSISTENT
                    : WifiConfiguration.RANDOMIZATION_PERSISTENT;
            wifiConfiguration.subscriptionId = mSubscriptionId;
            wifiConfiguration.restricted = mIsNetworkRestricted;
            wifiConfiguration.setSubscriptionGroup(mSubscriptionGroup);
            wifiConfiguration.setWifi7Enabled(mIsWifi7Enabled);
            return wifiConfiguration;
        }

        private void validateSecurityParams() {
            int numSecurityTypes = 0;
            numSecurityTypes += mIsEnhancedOpen ? 1 : 0;
            numSecurityTypes += !TextUtils.isEmpty(mWpa2PskPassphrase) ? 1 : 0;
            numSecurityTypes += !TextUtils.isEmpty(mWpa3SaePassphrase) ? 1 : 0;
            numSecurityTypes += !TextUtils.isEmpty(mWapiPskPassphrase) ? 1 : 0;
            numSecurityTypes += mWpa2EnterpriseConfig != null ? 1 : 0;
            numSecurityTypes += mWpa3EnterpriseConfig != null ? 1 : 0;
            numSecurityTypes += mWapiEnterpriseConfig != null ? 1 : 0;
            numSecurityTypes += mPasspointConfiguration != null ? 1 : 0;
            if (numSecurityTypes > 1) {
                throw new IllegalStateException("only one of setIsEnhancedOpen, setWpa2Passphrase,"
                        + " setWpa3Passphrase, setWpa2EnterpriseConfig, setWpa3EnterpriseConfig"
                        + " setWapiPassphrase, setWapiCertSuite, setIsWapiCertSuiteAuto"
                        + " or setPasspointConfig can be invoked for network suggestion");
            }
        }

        private WifiConfiguration buildWifiConfigurationForPasspoint() {
            WifiConfiguration wifiConfiguration = new WifiConfiguration();
            wifiConfiguration.FQDN = mPasspointConfiguration.getHomeSp().getFqdn();
            wifiConfiguration.setPasspointUniqueId(mPasspointConfiguration.getUniqueId());
            wifiConfiguration.priority = mPriority;
            wifiConfiguration.meteredOverride = mMeteredOverride;
            wifiConfiguration.trusted = !mIsNetworkUntrusted;
            wifiConfiguration.oemPaid = mIsNetworkOemPaid;
            wifiConfiguration.oemPrivate = mIsNetworkOemPrivate;
            wifiConfiguration.carrierMerged = mIsCarrierMerged;
            wifiConfiguration.carrierId = mCarrierId;
            wifiConfiguration.subscriptionId = mSubscriptionId;
            wifiConfiguration.macRandomizationSetting =
                    mMacRandomizationSetting == RANDOMIZATION_NON_PERSISTENT
                            ? WifiConfiguration.RANDOMIZATION_NON_PERSISTENT
                            : WifiConfiguration.RANDOMIZATION_PERSISTENT;
            wifiConfiguration.restricted = mIsNetworkRestricted;
            wifiConfiguration.setSubscriptionGroup(mSubscriptionGroup);
            wifiConfiguration.setWifi7Enabled(mIsWifi7Enabled);
            mPasspointConfiguration.setCarrierId(mCarrierId);
            mPasspointConfiguration.setSubscriptionId(mSubscriptionId);
            mPasspointConfiguration.setSubscriptionGroup(mSubscriptionGroup);
            mPasspointConfiguration.setMeteredOverride(wifiConfiguration.meteredOverride);
            mPasspointConfiguration.setOemPrivate(mIsNetworkOemPrivate);
            mPasspointConfiguration.setOemPaid(mIsNetworkOemPaid);
            mPasspointConfiguration.setCarrierMerged(mIsCarrierMerged);
            // MAC randomization should always be enabled for passpoint suggestions regardless of
            // the PasspointConfiguration's original setting.
            mPasspointConfiguration.setMacRandomizationEnabled(true);
            mPasspointConfiguration.setNonPersistentMacRandomizationEnabled(
                    mMacRandomizationSetting == RANDOMIZATION_NON_PERSISTENT);
            return wifiConfiguration;
        }

        /**
         * Create a network suggestion object for use in
         * {@link WifiManager#addNetworkSuggestions(List)}.
         *
         *<p class="note">
         * <b>Note:</b> Apps can set a combination of SSID using {@link #setSsid(String)} and BSSID
         * using {@link #setBssid(MacAddress)} to provide more fine grained network suggestions to
         * the platform.
         * </p>
         *
         * For example:
         * To provide credentials for one open, one WPA2, one WPA3 network with their
         * corresponding SSID's and one with Passpoint config:
         *
         * <pre>{@code
         * final WifiNetworkSuggestion suggestion1 =
         *      new Builder()
         *      .setSsid("test111111")
         *      .build();
         * final WifiNetworkSuggestion suggestion2 =
         *      new Builder()
         *      .setSsid("test222222")
         *      .setWpa2Passphrase("test123456")
         *      .build();
         * final WifiNetworkSuggestion suggestion3 =
         *      new Builder()
         *      .setSsid("test333333")
         *      .setWpa3Passphrase("test6789")
         *      .build();
         * final PasspointConfiguration passpointConfig= new PasspointConfiguration();
         * // configure passpointConfig to include a valid Passpoint configuration
         * final WifiNetworkSuggestion suggestion4 =
         *      new Builder()
         *      .setPasspointConfig(passpointConfig)
         *      .build();
         * final List<WifiNetworkSuggestion> suggestionsList =
         *      new ArrayList<WifiNetworkSuggestion> { {
         *          add(suggestion1);
         *          add(suggestion2);
         *          add(suggestion3);
         *          add(suggestion4);
         *      } };
         * final WifiManager wifiManager =
         *      context.getSystemService(Context.WIFI_SERVICE);
         * wifiManager.addNetworkSuggestions(suggestionsList);
         * // ...
         * }</pre>
         *
         * @return Instance of {@link WifiNetworkSuggestion}
         * @throws IllegalStateException on invalid params set
         * @see WifiNetworkSuggestion
         */
        public @NonNull WifiNetworkSuggestion build() {
            validateSecurityParams();
            WifiConfiguration wifiConfiguration;
            if (mPasspointConfiguration != null) {
                if (mWifiSsid != null) {
                    throw new IllegalStateException("setSsid should not be invoked for suggestion "
                            + "with Passpoint configuration");
                }
                if (mIsHiddenSSID) {
                    throw new IllegalStateException("setIsHiddenSsid should not be invoked for "
                            + "suggestion with Passpoint configuration");
                }
                wifiConfiguration = buildWifiConfigurationForPasspoint();
            } else {
                if (mWifiSsid == null) {
                    throw new IllegalStateException("setSsid should be invoked for suggestion");
                }
                if (mWifiSsid.getBytes().length == 0) {
                    throw new IllegalStateException("invalid ssid for suggestion");
                }
                if (mBssid != null
                        && (mBssid.equals(MacAddress.BROADCAST_ADDRESS)
                        || mBssid.equals(WifiManager.ALL_ZEROS_MAC_ADDRESS))) {
                    throw new IllegalStateException("invalid bssid for suggestion");
                }
                if (TextUtils.isEmpty(mWpa3SaePassphrase) && mSaeH2eOnlyMode) {
                    throw new IllegalStateException(
                            "Hash-to-Element only mode is only allowed for the SAE network");
                }

                wifiConfiguration = buildWifiConfiguration();
                if (wifiConfiguration.isOpenNetwork()) {
                    if (mIsSharedWithUserSet && mIsSharedWithUser) {
                        throw new IllegalStateException("Open network should not be "
                                + "setCredentialSharedWithUser to true");
                    }
                    mIsSharedWithUser = false;
                }
            }
            if (!mIsSharedWithUser && !mIsInitialAutojoinEnabled) {
                throw new IllegalStateException("Should have not a network with both "
                        + "setCredentialSharedWithUser and "
                        + "setIsAutojoinEnabled set to false");
            }
            if (mIsNetworkUntrusted || mIsNetworkRestricted) {
                if (mIsSharedWithUserSet && mIsSharedWithUser) {
                    throw new IllegalStateException("Should not be both"
                            + "setCredentialSharedWithUser and +"
                            + "setUntrusted or setRestricted to true");
                }
                mIsSharedWithUser = false;
            }
            if (mIsNetworkOemPaid) {
                if (mIsSharedWithUserSet && mIsSharedWithUser) {
                    throw new IllegalStateException("Should not be both"
                            + "setCredentialSharedWithUser and +"
                            + "setOemPaid to true");
                }
                mIsSharedWithUser = false;
            }
            if (mIsNetworkOemPrivate) {
                if (mIsSharedWithUserSet && mIsSharedWithUser) {
                    throw new IllegalStateException("Should not be both"
                            + "setCredentialSharedWithUser and +"
                            + "setOemPrivate to true");
                }
                mIsSharedWithUser = false;
            }
            if (mIsCarrierMerged) {
                if ((mSubscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                        && mSubscriptionGroup == null)
                        || mMeteredOverride != WifiConfiguration.METERED_OVERRIDE_METERED
                        || !isEnterpriseSuggestion()) {
                    throw new IllegalStateException("A carrier merged network must be a metered, "
                            + "enterprise network with valid subscription Id");
                }
            }
            if (mSubscriptionGroup != null
                    && mSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                throw new IllegalStateException("Should not be set both SubscriptionGroup and "
                        + "SubscriptionId");
            }
            return new WifiNetworkSuggestion(
                    wifiConfiguration,
                    mPasspointConfiguration,
                    mIsAppInteractionRequired,
                    mIsUserInteractionRequired,
                    mIsSharedWithUser,
                    mIsInitialAutojoinEnabled,
                    mPriorityGroup);
        }

        private boolean isEnterpriseSuggestion() {
            return !(mWpa2EnterpriseConfig == null && mWpa3EnterpriseConfig == null
                    && mWapiEnterpriseConfig == null && mPasspointConfiguration == null);
        }
    }



    /**
     * Network configuration for the provided network.
     * @hide
     */
    @NonNull
    public final WifiConfiguration wifiConfiguration;

    /**
     * Passpoint configuration for the provided network.
     * @hide
     */
    @Nullable
    public final PasspointConfiguration passpointConfiguration;

    /**
     * Whether app needs to log in to captive portal to obtain Internet access.
     * @hide
     */
    public final boolean isAppInteractionRequired;

    /**
     * Whether user needs to log in to captive portal to obtain Internet access.
     * @hide
     */
    public final boolean isUserInteractionRequired;

    /**
     * Whether app share credential with the user, allow user use provided credential to
     * connect network manually.
     * @hide
     */
    public final boolean isUserAllowedToManuallyConnect;

    /**
     * Whether the suggestion will be initialized as auto-joined or not.
     * @hide
     */
    public final boolean isInitialAutoJoinEnabled;

    /**
     * Priority group ID.
     * @hide
     */
    public final int priorityGroup;

    /** @hide */
    public WifiNetworkSuggestion() {
        this.wifiConfiguration = new WifiConfiguration();
        this.passpointConfiguration = null;
        this.isAppInteractionRequired = false;
        this.isUserInteractionRequired = false;
        this.isUserAllowedToManuallyConnect = true;
        this.isInitialAutoJoinEnabled = true;
        this.priorityGroup = 0;
    }

    /** @hide */
    public WifiNetworkSuggestion(@NonNull WifiConfiguration networkConfiguration,
                                 @Nullable PasspointConfiguration passpointConfiguration,
                                 boolean isAppInteractionRequired,
                                 boolean isUserInteractionRequired,
                                 boolean isUserAllowedToManuallyConnect,
                                 boolean isInitialAutoJoinEnabled, int priorityGroup) {
        checkNotNull(networkConfiguration);
        this.wifiConfiguration = networkConfiguration;
        this.passpointConfiguration = passpointConfiguration;

        this.isAppInteractionRequired = isAppInteractionRequired;
        this.isUserInteractionRequired = isUserInteractionRequired;
        this.isUserAllowedToManuallyConnect = isUserAllowedToManuallyConnect;
        this.isInitialAutoJoinEnabled = isInitialAutoJoinEnabled;
        this.priorityGroup = priorityGroup;
    }

    public static final @NonNull Creator<WifiNetworkSuggestion> CREATOR =
            new Creator<WifiNetworkSuggestion>() {
                @Override
                public WifiNetworkSuggestion createFromParcel(Parcel in) {
                    return new WifiNetworkSuggestion(
                            in.readParcelable(null), // wifiConfiguration
                            in.readParcelable(null), // PasspointConfiguration
                            in.readBoolean(), // isAppInteractionRequired
                            in.readBoolean(), // isUserInteractionRequired
                            in.readBoolean(), // isSharedCredentialWithUser
                            in.readBoolean(),  // isAutojoinEnabled
                            in.readInt() // priorityGroup
                    );
                }

                @Override
                public WifiNetworkSuggestion[] newArray(int size) {
                    return new WifiNetworkSuggestion[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(wifiConfiguration, flags);
        dest.writeParcelable(passpointConfiguration, flags);
        dest.writeBoolean(isAppInteractionRequired);
        dest.writeBoolean(isUserInteractionRequired);
        dest.writeBoolean(isUserAllowedToManuallyConnect);
        dest.writeBoolean(isInitialAutoJoinEnabled);
        dest.writeInt(priorityGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wifiConfiguration.SSID, wifiConfiguration.BSSID,
                wifiConfiguration.getDefaultSecurityType(),
                wifiConfiguration.getPasspointUniqueId(),
                wifiConfiguration.subscriptionId, wifiConfiguration.carrierId,
                wifiConfiguration.getSubscriptionGroup());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiNetworkSuggestion)) {
            return false;
        }
        WifiNetworkSuggestion lhs = (WifiNetworkSuggestion) obj;
        if (this.passpointConfiguration == null ^ lhs.passpointConfiguration == null) {
            return false;
        }

        return TextUtils.equals(this.wifiConfiguration.SSID, lhs.wifiConfiguration.SSID)
                && TextUtils.equals(this.wifiConfiguration.BSSID, lhs.wifiConfiguration.BSSID)
                && TextUtils.equals(this.wifiConfiguration.getDefaultSecurityType(),
                lhs.wifiConfiguration.getDefaultSecurityType())
                && TextUtils.equals(this.wifiConfiguration.getPasspointUniqueId(),
                lhs.wifiConfiguration.getPasspointUniqueId())
                && this.wifiConfiguration.carrierId == lhs.wifiConfiguration.carrierId
                && this.wifiConfiguration.subscriptionId == lhs.wifiConfiguration.subscriptionId
                && Objects.equals(this.wifiConfiguration.getSubscriptionGroup(),
                lhs.wifiConfiguration.getSubscriptionGroup());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WifiNetworkSuggestion[ ")
                .append("SSID=").append(wifiConfiguration.SSID)
                .append(", BSSID=").append(wifiConfiguration.BSSID)
                .append(", FQDN=").append(wifiConfiguration.FQDN)
                .append(", SecurityParams=");
        wifiConfiguration.getSecurityParamsList().stream()
                .forEach(param -> {
                    sb.append(" ");
                    sb.append(WifiConfiguration.getSecurityTypeName(param.getSecurityType()));
                    if (param.isAddedByAutoUpgrade()) sb.append("^");
                });
        sb.append(", isAppInteractionRequired=").append(isAppInteractionRequired)
                .append(", isUserInteractionRequired=").append(isUserInteractionRequired)
                .append(", isCredentialSharedWithUser=").append(isUserAllowedToManuallyConnect)
                .append(", isInitialAutoJoinEnabled=").append(isInitialAutoJoinEnabled)
                .append(", isUnTrusted=").append(!wifiConfiguration.trusted)
                .append(", isOemPaid=").append(wifiConfiguration.oemPaid)
                .append(", isOemPrivate=").append(wifiConfiguration.oemPrivate)
                .append(", isCarrierMerged=").append(wifiConfiguration.carrierMerged)
                .append(", isHiddenSsid=").append(wifiConfiguration.hiddenSSID)
                .append(", priorityGroup=").append(priorityGroup)
                .append(", subscriptionId=").append(wifiConfiguration.subscriptionId)
                .append(", subscriptionGroup=").append(wifiConfiguration.getSubscriptionGroup())
                .append(", carrierId=").append(wifiConfiguration.carrierId)
                .append(", priority=").append(wifiConfiguration.priority)
                .append(", meteredness=").append(wifiConfiguration.meteredOverride)
                .append(", restricted=").append(wifiConfiguration.restricted)
                .append(" ]");
        return sb.toString();
    }

    /**
     * Get the {@link WifiConfiguration} associated with this Suggestion.
     * @hide
     */
    @SystemApi
    @NonNull
    public WifiConfiguration getWifiConfiguration() {
        return wifiConfiguration;
    }

    /**
     * Get the BSSID, or null if unset.
     * @see Builder#setBssid(MacAddress)
     */
    @Nullable
    public MacAddress getBssid() {
        if (wifiConfiguration.BSSID == null) {
            return null;
        }
        return MacAddress.fromString(wifiConfiguration.BSSID);
    }

    /** @see Builder#setCredentialSharedWithUser(boolean) */
    public boolean isCredentialSharedWithUser() {
        return isUserAllowedToManuallyConnect;
    }

    /** @see Builder#setIsAppInteractionRequired(boolean) */
    public boolean isAppInteractionRequired() {
        return isAppInteractionRequired;
    }

    /** @see Builder#setIsEnhancedOpen(boolean)  */
    public boolean isEnhancedOpen() {
        return wifiConfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.OWE);
    }

    /** @see Builder#setIsHiddenSsid(boolean)  */
    public boolean isHiddenSsid() {
        return wifiConfiguration.hiddenSSID;
    }

    /** @see Builder#setIsInitialAutojoinEnabled(boolean)  */
    public boolean isInitialAutojoinEnabled() {
        return isInitialAutoJoinEnabled;
    }

    /** @see Builder#setIsMetered(boolean)  */
    public boolean isMetered() {
        return wifiConfiguration.meteredOverride == WifiConfiguration.METERED_OVERRIDE_METERED;
    }

    /** @see Builder#setIsUserInteractionRequired(boolean)  */
    public boolean isUserInteractionRequired() {
        return isUserInteractionRequired;
    }

    /**
     * Get the {@link PasspointConfiguration} associated with this Suggestion, or null if this
     * Suggestion is not for a Passpoint network.
     */
    @Nullable
    public PasspointConfiguration getPasspointConfig() {
        return passpointConfiguration;
    }

    /** @see Builder#setPriority(int)  */
    @IntRange(from = 0)
    public int getPriority() {
        return wifiConfiguration.priority;
    }

    /**
     * Return the unicode SSID of the network, or null if this is a Passpoint network or the SSID is
     * non-unicode.
     * <p>
     * Note: use {@link #getWifiSsid()} which supports both unicode and non-unicode SSID.
     * @see Builder#setSsid(String)
     */
    @Nullable
    public String getSsid() {
        if (wifiConfiguration.SSID == null) {
            return null;
        }
        WifiSsid wifiSsid;
        try {
            wifiSsid = WifiSsid.fromString(wifiConfiguration.SSID);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (wifiSsid.getUtf8Text() == null) {
            return null;
        }
        return wifiSsid.getUtf8Text().toString();
    }

    /**
     * Return the {@link WifiSsid} of the network, or null if this is a Passpoint network.
     * @see Builder#setWifiSsid(WifiSsid)
     * @return An object representing the SSID the network. {@code null} for passpoint network.
     */
    @Nullable
    public WifiSsid getWifiSsid() {
        if (wifiConfiguration.SSID == null) {
            return null;
        }
        WifiSsid wifiSsid;
        try {
            wifiSsid = WifiSsid.fromString(wifiConfiguration.SSID);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid SSID in the network suggestion");
        }
        return wifiSsid;
    }

    /** @see Builder#setUntrusted(boolean)  */
    public boolean isUntrusted() {
        return !wifiConfiguration.trusted;
    }

    /**
     * Return if a suggestion is for a restricted network
     * @see Builder#setRestricted(boolean)
     * @return true if the suggestion is restricted, false otherwise
     */
    public boolean isRestricted() {
        return wifiConfiguration.restricted;
    }

    /**
     * @see Builder#setOemPaid(boolean)
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.S)
    public boolean isOemPaid() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return wifiConfiguration.oemPaid;
    }

    /**
     * @see Builder#setOemPrivate(boolean)
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.S)
    public boolean isOemPrivate() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return wifiConfiguration.oemPrivate;
    }

    /**
     * @see Builder#setCarrierMerged(boolean)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public boolean isCarrierMerged() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return wifiConfiguration.carrierMerged;
    }

    /**
     * Get the WifiEnterpriseConfig, or null if unset.
     * @see Builder#setWapiEnterpriseConfig(WifiEnterpriseConfig)
     * @see Builder#setWpa2EnterpriseConfig(WifiEnterpriseConfig)
     * @see Builder#setWpa3EnterpriseConfig(WifiEnterpriseConfig)
     */
    @Nullable
    public WifiEnterpriseConfig getEnterpriseConfig() {
        if (!wifiConfiguration.isEnterprise()) {
            return null;
        }
        return wifiConfiguration.enterpriseConfig;
    }

    /**
     * Get the passphrase, or null if unset.
     * @see Builder#setWapiPassphrase(String)
     * @see Builder#setWpa2Passphrase(String)
     * @see Builder#setWpa3Passphrase(String)
     */
    @Nullable
    public String getPassphrase() {
        if (wifiConfiguration.preSharedKey == null) {
            return null;
        }
        return WifiInfo.removeDoubleQuotes(wifiConfiguration.preSharedKey);
    }

    /**
     * @see Builder#setPriorityGroup(int)
     */
    @IntRange(from = 0)
    public int getPriorityGroup() {
        return priorityGroup;
    }

    /**
     * @see Builder#setSubscriptionId(int)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public int getSubscriptionId() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return wifiConfiguration.subscriptionId;
    }

    /**
     * @see Builder#setCarrierId(int)
     * @hide
     */
    @SystemApi
    public int getCarrierId() {
        return wifiConfiguration.carrierId;
    }

    /**
     * Get the MAC randomization method.
     * @return one of {@code RANDOMIZATION_*} values
     * @see Builder#setMacRandomizationSetting(int)
     */
    public @MacRandomizationSetting int getMacRandomizationSetting() {
        return wifiConfiguration.macRandomizationSetting
                == WifiConfiguration.RANDOMIZATION_NON_PERSISTENT
                ? RANDOMIZATION_NON_PERSISTENT : RANDOMIZATION_PERSISTENT;
    }

    /**
     * Get the subscription Group UUID of the suggestion
     * @see Builder#setSubscriptionGroup(ParcelUuid)
     * @return Uuid represent a Subscription Group
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public @Nullable ParcelUuid getSubscriptionGroup() {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException();
        }
        return wifiConfiguration.getSubscriptionGroup();
    }

    /**
     * See {@link Builder#setWifi7Enabled(boolean)}
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public boolean isWifi7Enabled() {
        return wifiConfiguration.isWifi7Enabled();
    }
}

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

import static android.net.wifi.WifiConfiguration.INVALID_NETWORK_ID;

import android.Manifest;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.app.admin.DevicePolicyManager;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo.DetailedState;
import android.net.TransportInfo;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;
import com.android.net.module.util.Inet4AddressUtils;
import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Describes the state of any Wi-Fi connection that is active or
 * is in the process of being set up.
 *
 * In the connected state, access to location sensitive fields requires
 * the same permissions as {@link WifiManager#getScanResults}. If such access is not allowed,
 * {@link #getSSID} will return {@link WifiManager#UNKNOWN_SSID} and
 * {@link #getBSSID} will return {@code "02:00:00:00:00:00"}.
 * {@link #getApMldMacAddress()} will return null.
 * {@link #getNetworkId()} will return {@code -1}.
 * {@link #getPasspointFqdn()} will return null.
 * {@link #getPasspointProviderFriendlyName()} will return null.
 * {@link #getInformationElements()} will return null.
 * {@link #getMacAddress()} will return {@code "02:00:00:00:00:00"}.
 */
public class WifiInfo implements TransportInfo, Parcelable {
    private static final String TAG = "WifiInfo";

    /**
     * This is the map described in the Javadoc comment above. The positions
     * of the elements of the array must correspond to the ordinal values
     * of <code>DetailedState</code>.
     */
    private static final EnumMap<SupplicantState, DetailedState> stateMap =
            new EnumMap<SupplicantState, DetailedState>(SupplicantState.class);

    /**
     * Default MAC address reported to a client that does not have the
     * android.permission.LOCAL_MAC_ADDRESS permission.
     *
     * @hide
     */
    @SystemApi
    public static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";

    static {
        stateMap.put(SupplicantState.DISCONNECTED, DetailedState.DISCONNECTED);
        stateMap.put(SupplicantState.INTERFACE_DISABLED, DetailedState.DISCONNECTED);
        stateMap.put(SupplicantState.INACTIVE, DetailedState.IDLE);
        stateMap.put(SupplicantState.SCANNING, DetailedState.SCANNING);
        stateMap.put(SupplicantState.AUTHENTICATING, DetailedState.CONNECTING);
        stateMap.put(SupplicantState.ASSOCIATING, DetailedState.CONNECTING);
        stateMap.put(SupplicantState.ASSOCIATED, DetailedState.CONNECTING);
        stateMap.put(SupplicantState.FOUR_WAY_HANDSHAKE, DetailedState.AUTHENTICATING);
        stateMap.put(SupplicantState.GROUP_HANDSHAKE, DetailedState.AUTHENTICATING);
        stateMap.put(SupplicantState.COMPLETED, DetailedState.OBTAINING_IPADDR);
        stateMap.put(SupplicantState.DORMANT, DetailedState.DISCONNECTED);
        stateMap.put(SupplicantState.UNINITIALIZED, DetailedState.IDLE);
        stateMap.put(SupplicantState.INVALID, DetailedState.FAILED);
    }

    private SupplicantState mSupplicantState;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private String mBSSID;
    @UnsupportedAppUsage
    private WifiSsid mWifiSsid;
    private boolean mIsHiddenSsid = false;
    private int mNetworkId;
    private int mSecurityType;

    /**
     * The Multi-Link Device (MLD) MAC Address for the connected access point.
     * Only applicable for Wi-Fi 7 access points, null otherwise.
     * This will be set even if the STA is non-MLD
     */
    private MacAddress mApMldMacAddress;

    /**
     * The Multi-Link Operation (MLO) link-id for the access point.
     * Only applicable for Wi-Fi 7 access points.
     */
    private int mApMloLinkId;

    /** Maps link id to Affiliated MLO links. */
    private SparseArray<MloLink> mAffiliatedMloLinksMap = new SparseArray<>();

    /**
     * The Multi-Link Operation (MLO) affiliated Links.
     * Only applicable for Wi-Fi 7 access points.
     */
    private List<MloLink> mAffiliatedMloLinks;

    /**
     * Used to indicate that the RSSI is invalid, for example if no RSSI measurements are available
     * yet.
     * @hide
     */
    @SystemApi
    public static final int INVALID_RSSI = -127;

    /** @hide **/
    public static final int UNKNOWN_FREQUENCY = -1;

    /** @hide **/
    public static final int MIN_RSSI = -126;

    /** @hide **/
    public static final int MAX_RSSI = 200;

    /** Unknown security type. */
    public static final int SECURITY_TYPE_UNKNOWN = -1;
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
    /** Security type for a WPA3-Enterprise in 192-bit security network. */
    public static final int SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT = 5;
    /** Security type for an OWE network. */
    public static final int SECURITY_TYPE_OWE = 6;
    /** Security type for a WAPI PSK network. */
    public static final int SECURITY_TYPE_WAPI_PSK = 7;
    /** Security type for a WAPI Certificate network. */
    public static final int SECURITY_TYPE_WAPI_CERT = 8;
    /** Security type for a WPA3-Enterprise network. */
    public static final int SECURITY_TYPE_EAP_WPA3_ENTERPRISE = 9;
    /** Security type for an OSEN network. */
    public static final int SECURITY_TYPE_OSEN = 10;
    /** Security type for a Passpoint R1/R2 network, where TKIP and WEP are not allowed. */
    public static final int SECURITY_TYPE_PASSPOINT_R1_R2 = 11;
    /**
     * Security type for a Passpoint R3 network, where TKIP and WEP are not allowed,
     * and PMF must be set to Required.
     */
    public static final int SECURITY_TYPE_PASSPOINT_R3 = 12;
    /** Security type for Easy Connect (DPP) network */
    public static final int SECURITY_TYPE_DPP = 13;

    /**
     * Unknown security type that cannot be converted to
     * DevicePolicyManager.WifiSecurity security type.
     * @hide
     */
    public static final int DPM_SECURITY_TYPE_UNKNOWN = -1;

    /** @see #isPrimary() - No permission to access the field.  */
    private static final int IS_PRIMARY_NO_PERMISSION = -1;
    /** @see #isPrimary() - false */
    private static final int IS_PRIMARY_FALSE = 0;
    /** @see #isPrimary() - true */
    private static final int IS_PRIMARY_TRUE = 1;
    /** Tri state to store {@link #isPrimary()} field. */
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "IS_PRIMARY_" }, value = {
            IS_PRIMARY_NO_PERMISSION, IS_PRIMARY_FALSE, IS_PRIMARY_TRUE
    })
    public @interface IsPrimaryValues {}

    /**
     * Received Signal Strength Indicator
     */
    private int mRssi;
    private long mLastRssiUpdateMillis;

    /**
     * Wi-Fi standard for the connection
     */
    private @WifiAnnotations.WifiStandard int mWifiStandard;

    /**
     * The unit in which links speeds are expressed.
     */
    public static final String LINK_SPEED_UNITS = "Mbps";
    private int mLinkSpeed;

    /**
     * Constant for unknown link speed.
     */
    public static final int LINK_SPEED_UNKNOWN = -1;

    /**
     * Tx(transmit) Link speed in Mbps
     */
    private int mTxLinkSpeed;

    /**
     * Max supported Tx(transmit) link speed in Mbps
     */
    private int mMaxSupportedTxLinkSpeed;

    /**
     * Rx(receive) Link speed in Mbps
     */
    private int mRxLinkSpeed;

    /**
     * Max supported Rx(receive) link speed in Mbps
     */
    private int mMaxSupportedRxLinkSpeed;

    /**
     * Frequency in MHz
     */
    public static final String FREQUENCY_UNITS = "MHz";
    private int mFrequency;

    @UnsupportedAppUsage
    private InetAddress mIpAddress;
    @UnsupportedAppUsage
    private String mMacAddress = DEFAULT_MAC_ADDRESS;

    /**
     * Whether the network is ephemeral or not.
     */
    private boolean mEphemeral;

    /**
     * Whether the network is trusted or not.
     */
    private boolean mTrusted;

    /**
     * Whether the network is restricted or not.
     */
    private boolean mRestricted;

    /**
     * Whether the network is oem paid or not.
     */
    private boolean mOemPaid;

    /**
     * Whether the network is oem private or not.
     */
    private boolean mOemPrivate;

    /**
     * Whether the network is a carrier merged network.
     */
    private boolean mCarrierMerged;

    /**
     * OSU (Online Sign Up) AP for Passpoint R2.
     */
    private boolean mOsuAp;

    /**
     * Fully qualified domain name of a Passpoint configuration
     */
    private String mFqdn;

    /**
     * Name of Passpoint credential provider
     */
    private String mProviderFriendlyName;

    /**
     * If connected to a network suggestion or specifier, store the package name of the app,
     * else null.
     */
    private String mRequestingPackageName;

    /**
     * Identify which Telephony subscription provides this network.
     */
    private int mSubscriptionId;

    /**
     * Running total count of lost (not ACKed) transmitted unicast data packets.
     * @hide
     */
    public long txBad;
    /**
     * Running total count of transmitted unicast data retry packets.
     * @hide
     */
    public long txRetries;
    /**
     * Running total count of successfully transmitted (ACKed) unicast data packets.
     * @hide
     */
    public long txSuccess;
    /**
     * Running total count of received unicast data packets.
     * @hide
     */
    public long rxSuccess;

    private double mLostTxPacketsPerSecond;

    /**
     * TID-to-link mapping negotiation support by the AP.
     */
    private boolean mApTidToLinkMappingNegotiationSupported;

    /**
     * Average rate of lost transmitted packets, in units of packets per second. In case of Multi
     * Link Operation (MLO), returned value is the average rate of lost transmitted packets on all
     * associated links.
     *
     * @hide
     */
    @SystemApi
    public double getLostTxPacketsPerSecond() {
        return mLostTxPacketsPerSecond;
    }

    /** @hide */
    public void setLostTxPacketsPerSecond(double lostTxPacketsPerSecond) {
        mLostTxPacketsPerSecond = lostTxPacketsPerSecond;
    }

    private double mTxRetriedTxPacketsPerSecond;

    /**
     * Average rate of transmitted retry packets, in units of packets per second. In case Multi Link
     * Operation (MLO), the returned value is the average rate of transmitted retry packets on all
     * associated links.
     *
     * @hide
     */
    @SystemApi
    public double getRetriedTxPacketsPerSecond() {
        return mTxRetriedTxPacketsPerSecond;
    }

    /** @hide */
    public void setRetriedTxPacketsRate(double txRetriedTxPacketsPerSecond) {
        mTxRetriedTxPacketsPerSecond = txRetriedTxPacketsPerSecond;
    }

    private double mSuccessfulTxPacketsPerSecond;

    /**
     * Average rate of successfully transmitted unicast packets, in units of packets per second. In
     * case Multi Link Operation (MLO), returned value is the average rate of successfully
     * transmitted unicast packets on all associated links.
     *
     * @hide
     */
    @SystemApi
    public double getSuccessfulTxPacketsPerSecond() {
        return mSuccessfulTxPacketsPerSecond;
    }

    /** @hide */
    public void setSuccessfulTxPacketsPerSecond(double successfulTxPacketsPerSecond) {
        mSuccessfulTxPacketsPerSecond = successfulTxPacketsPerSecond;
    }

    private double mSuccessfulRxPacketsPerSecond;

    /**
     * Average rate of received unicast data packets, in units of packets per second. In case of
     * Multi Link Operation (MLO), the returned value is the average rate of received unicast data
     * packets on all associated links.
     *
     * @hide
     */
    @SystemApi
    public double getSuccessfulRxPacketsPerSecond() {
        return mSuccessfulRxPacketsPerSecond;
    }

    /** @hide */
    public void setSuccessfulRxPacketsPerSecond(double successfulRxPacketsPerSecond) {
        mSuccessfulRxPacketsPerSecond = successfulRxPacketsPerSecond;
    }

    /** @hide */
    @UnsupportedAppUsage
    public int score;

    /**
     * The current Wifi score.
     * NOTE: this value should only be used for debugging purposes. Do not rely on this value for
     * any computations. The meaning of this value can and will change at any time without warning.
     * @hide
     */
    @SystemApi
    public int getScore() {
        return score;
    }

    /** @hide */
    public void setScore(int score) {
        this.score = score;
    }

    /** @hide */
    private boolean mIsUsable = true;

    /** @hide */
    public boolean isUsable() {
        return mIsUsable;
    }

    /**
     * This could be set to false by the external scorer when the network quality is bad.
     * The wifi module could use this information in network selection.
     * @hide
     */
    public void setUsable(boolean isUsable) {
        mIsUsable = isUsable;
    }

    /**
     * Flag indicating that AP has hinted that upstream connection is metered,
     * and sensitive to heavy data transfers.
     */
    private boolean mMeteredHint;

    /**
     * Passpoint unique key
     */
    private String mPasspointUniqueId;

    /**
     * information elements found in the beacon of the connected bssid.
     */
    @Nullable
    private List<ScanResult.InformationElement> mInformationElements;

    /**
     * @see #isPrimary()
     * The field is stored as an int since is a tristate internally -  true, false, no permission.
     */
    private @IsPrimaryValues int mIsPrimary;

    /**
     * Key of the current network.
     */
    private String mNetworkKey;

    /** List of {@link OuiKeyedData} providing vendor-specific configuration data. */
    private @NonNull List<OuiKeyedData> mVendorData;

    /** @hide */
    @UnsupportedAppUsage
    public WifiInfo() {
        mWifiSsid = null;
        mBSSID = null;
        mApMldMacAddress = null;
        mApMloLinkId = 0;
        mAffiliatedMloLinks = Collections.emptyList();
        mNetworkId = -1;
        mSupplicantState = SupplicantState.UNINITIALIZED;
        mRssi = INVALID_RSSI;
        mLinkSpeed = LINK_SPEED_UNKNOWN;
        mFrequency = UNKNOWN_FREQUENCY;
        mSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mSecurityType = -1;
        mIsPrimary = IS_PRIMARY_FALSE;
        mNetworkKey = null;
        mApMloLinkId = MloLink.INVALID_MLO_LINK_ID;
        mVendorData = Collections.emptyList();
    }

    /** @hide */
    public void reset() {
        setInetAddress(null);
        setBSSID(null);
        setSSID(null);
        setHiddenSSID(false);
        setNetworkId(-1);
        setRssi(INVALID_RSSI);
        setLinkSpeed(LINK_SPEED_UNKNOWN);
        setTxLinkSpeedMbps(LINK_SPEED_UNKNOWN);
        setRxLinkSpeedMbps(LINK_SPEED_UNKNOWN);
        setMaxSupportedTxLinkSpeedMbps(LINK_SPEED_UNKNOWN);
        setMaxSupportedRxLinkSpeedMbps(LINK_SPEED_UNKNOWN);
        setFrequency(-1);
        setMeteredHint(false);
        setEphemeral(false);
        setTrusted(false);
        setOemPaid(false);
        setOemPrivate(false);
        setCarrierMerged(false);
        setOsuAp(false);
        setRequestingPackageName(null);
        setFQDN(null);
        setProviderFriendlyName(null);
        setPasspointUniqueId(null);
        setSubscriptionId(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        setInformationElements(null);
        setIsPrimary(false);
        setRestricted(false);
        txBad = 0;
        txSuccess = 0;
        rxSuccess = 0;
        txRetries = 0;
        mLostTxPacketsPerSecond = 0;
        mSuccessfulTxPacketsPerSecond = 0;
        mSuccessfulRxPacketsPerSecond = 0;
        mTxRetriedTxPacketsPerSecond = 0;
        score = 0;
        mIsUsable = true;
        mSecurityType = -1;
        mNetworkKey = null;
        resetMultiLinkInfo();
        enableApTidToLinkMappingNegotiationSupport(false);
        mVendorData = Collections.emptyList();
    }

    /** @hide */
    public void resetMultiLinkInfo() {
        setApMldMacAddress(null);
        mApMloLinkId = MloLink.INVALID_MLO_LINK_ID;
        mAffiliatedMloLinks = Collections.emptyList();
    }

    /**
     * Copy constructor
     * @hide
     */
    public WifiInfo(WifiInfo source) {
        this(source, NetworkCapabilities.REDACT_NONE);
    }

    /**
     * Copy constructor
     * @hide
     */
    private WifiInfo(WifiInfo source, long redactions) {
        if (source != null) {
            mSupplicantState = source.mSupplicantState;
            mBSSID = shouldRedactLocationSensitiveFields(redactions)
                    ? DEFAULT_MAC_ADDRESS : source.mBSSID;
            mApMldMacAddress = shouldRedactLocationSensitiveFields(redactions)
                    ? null : source.mApMldMacAddress;
            mApMloLinkId = source.mApMloLinkId;
            if (source.mApMldMacAddress != null) {
                mAffiliatedMloLinks = new ArrayList<MloLink>();
                for (MloLink link : source.mAffiliatedMloLinks) {
                    mAffiliatedMloLinks.add(new MloLink(link, redactions));
                }
            } else {
                mAffiliatedMloLinks = Collections.emptyList();
            }
            mWifiSsid = shouldRedactLocationSensitiveFields(redactions)
                    ? null : source.mWifiSsid;
            mNetworkId = shouldRedactLocationSensitiveFields(redactions)
                    ? INVALID_NETWORK_ID : source.mNetworkId;
            mRssi = source.mRssi;
            mLinkSpeed = source.mLinkSpeed;
            mTxLinkSpeed = source.mTxLinkSpeed;
            mRxLinkSpeed = source.mRxLinkSpeed;
            mFrequency = source.mFrequency;
            mIpAddress = source.mIpAddress;
            mMacAddress = (shouldRedactLocalMacAddressFields(redactions)
                    || shouldRedactLocationSensitiveFields(redactions))
                            ? DEFAULT_MAC_ADDRESS : source.mMacAddress;
            mMeteredHint = source.mMeteredHint;
            mEphemeral = source.mEphemeral;
            mTrusted = source.mTrusted;
            mRestricted = source.mRestricted;
            mOemPaid = source.mOemPaid;
            mOemPrivate = source.mOemPrivate;
            mCarrierMerged = source.mCarrierMerged;
            mRequestingPackageName = shouldRedactNetworkSettingsFields(redactions) ? null
                    : source.mRequestingPackageName;
            mOsuAp = source.mOsuAp;
            mFqdn = shouldRedactLocationSensitiveFields(redactions)
                    ? null : source.mFqdn;
            mProviderFriendlyName = shouldRedactLocationSensitiveFields(redactions)
                    ? null : source.mProviderFriendlyName;
            mSubscriptionId = source.mSubscriptionId;
            txBad = source.txBad;
            txRetries = source.txRetries;
            txSuccess = source.txSuccess;
            rxSuccess = source.rxSuccess;
            mLostTxPacketsPerSecond = source.mLostTxPacketsPerSecond;
            mTxRetriedTxPacketsPerSecond = source.mTxRetriedTxPacketsPerSecond;
            mSuccessfulTxPacketsPerSecond = source.mSuccessfulTxPacketsPerSecond;
            mSuccessfulRxPacketsPerSecond = source.mSuccessfulRxPacketsPerSecond;
            score = source.score;
            mIsUsable = source.mIsUsable;
            mWifiStandard = source.mWifiStandard;
            mMaxSupportedTxLinkSpeed = source.mMaxSupportedTxLinkSpeed;
            mMaxSupportedRxLinkSpeed = source.mMaxSupportedRxLinkSpeed;
            mPasspointUniqueId = shouldRedactLocationSensitiveFields(redactions)
                    ? null : source.mPasspointUniqueId;
            if (source.mInformationElements != null
                    && !shouldRedactLocationSensitiveFields(redactions)) {
                mInformationElements = new ArrayList<>(source.mInformationElements);
            }
            mIsPrimary = shouldRedactNetworkSettingsFields(redactions)
                    ? IS_PRIMARY_NO_PERMISSION : source.mIsPrimary;
            mSecurityType = source.mSecurityType;
            mNetworkKey = shouldRedactLocationSensitiveFields(redactions)
                    ? null : source.mNetworkKey;
            mApTidToLinkMappingNegotiationSupported =
                    source.mApTidToLinkMappingNegotiationSupported;
            mVendorData = new ArrayList<>(source.mVendorData);
        }
    }

    /** Builder for WifiInfo */
    public static final class Builder {
        private final WifiInfo mWifiInfo = new WifiInfo();

        /**
         * Set the SSID, in the form of a raw byte array.
         * @see WifiInfo#getSSID()
         */
        @NonNull
        public Builder setSsid(@NonNull byte[] ssid) {
            mWifiInfo.setSSID(WifiSsid.fromBytes(ssid));
            return this;
        }

        /**
         * Set the BSSID.
         * @see WifiInfo#getBSSID()
         */
        @NonNull
        public Builder setBssid(@NonNull String bssid) {
            mWifiInfo.setBSSID(bssid);
            return this;
        }

        /**
         * Set the AP MLD (Multi-Link Device) MAC Address.
         * @see WifiInfo#getApMldMacAddress()
         * @hide
         */
        @Nullable
        public Builder setApMldMacAddress(@Nullable MacAddress address) {
            mWifiInfo.setApMldMacAddress(address);
            return this;
        }

        /**
         * Set the access point Multi-Link Operation (MLO) link-id.
         * @see WifiInfo#getApMloLinkId()
         * @hide
         */
        public Builder setApMloLinkId(int linkId) {
            mWifiInfo.setApMloLinkId(linkId);
            return this;
        }

        /**
         * Set the Multi-Link Operation (MLO) affiliated Links.
         * Only applicable for Wi-Fi 7 access points.
         * @see WifiInfo#getAffiliatedMloLinks()
         * @hide
         */
        public Builder setAffiliatedMloLinks(@NonNull List<MloLink> links) {
            mWifiInfo.setAffiliatedMloLinks(links);
            return this;
        }

        /**
         * Set the RSSI, in dBm.
         * @see WifiInfo#getRssi()
         */
        @NonNull
        public Builder setRssi(int rssi) {
            mWifiInfo.setRssi(rssi);
            return this;
        }

        /**
         * Set the network ID.
         * @see WifiInfo#getNetworkId()
         */
        @NonNull
        public Builder setNetworkId(int networkId) {
            mWifiInfo.setNetworkId(networkId);
            return this;
        }

        /**
         * Set the subscription ID.
         * @see WifiInfo#getSubscriptionId()
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setSubscriptionId(int subId) {
            mWifiInfo.setSubscriptionId(subId);
            return this;
        }

        /**
         * Set the current security type
         * @see WifiInfo#getCurrentSecurityType()
         */
        @NonNull
        public Builder setCurrentSecurityType(@WifiConfiguration.SecurityType int securityType) {
            mWifiInfo.setCurrentSecurityType(securityType);
            return this;
        }

        /**
         * Enable or Disable Peer TID-To-Link Mapping Negotiation Capability.
         * See {@link WifiInfo#isApTidToLinkMappingNegotiationSupported()}
         * @hide
         */
        @NonNull
        public Builder enableApTidToLinkMappingNegotiationSupport(boolean enable) {
            mWifiInfo.enableApTidToLinkMappingNegotiationSupport(enable);
            return this;
        }

        /**
         * Build a WifiInfo object.
         */
        @NonNull
        public WifiInfo build() {
            return new WifiInfo(mWifiInfo);
        }
    }

    /** @hide */
    public void setSSID(WifiSsid wifiSsid) {
        mWifiSsid = wifiSsid;
    }

    /**
     * Returns the service set identifier (SSID) of the current 802.11 network.
     * <p>
     * If the SSID can be decoded as UTF-8, it will be returned surrounded by double
     * quotation marks. Otherwise, it is returned as a string of hex digits.
     * The SSID may be {@link WifiManager#UNKNOWN_SSID}, if there is no network currently connected
     * or if the caller has insufficient permissions to access the SSID.
     * </p>
     * <p>
     * Prior to {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR1}, this method
     * always returned the SSID with no quotes around it.
     * </p>
     *
     * @return the SSID.
     */
    public String getSSID() {
        if (mWifiSsid != null) {
            String ssidString = mWifiSsid.toString();
            if (!TextUtils.isEmpty(ssidString)) {
                return ssidString;
            }
        }
        return WifiManager.UNKNOWN_SSID;
    }

    /** @hide */
    @UnsupportedAppUsage
    public WifiSsid getWifiSsid() {
        return mWifiSsid;
    }

    /** @hide */
    @UnsupportedAppUsage
    public void setBSSID(String BSSID) {
        mBSSID = BSSID;
    }

    /**
     * Set the access point Multi-Link Device (MLD) MAC Address.
     * @hide
     */
    public void setApMldMacAddress(@Nullable MacAddress address) {
        mApMldMacAddress = address;
    }

    /**
     * Set the access point Multi-Link Operation (MLO) link-id
     * @hide
     */
    public void setApMloLinkId(int linkId) {
        mApMloLinkId = linkId;
    }

    private void mapAffiliatedMloLinks() {
        mAffiliatedMloLinksMap.clear();
        for (MloLink link : mAffiliatedMloLinks) {
            mAffiliatedMloLinksMap.put(link.getLinkId(), link);
        }
    }

    /**
     * Set the Multi-Link Operation (MLO) affiliated Links.
     * Only applicable for Wi-Fi 7 access points.
     *
     * @hide
     */
    public void setAffiliatedMloLinks(@NonNull List<MloLink> links) {
        mAffiliatedMloLinks = new ArrayList<MloLink>(links);
        mapAffiliatedMloLinks();
    }

    /**
     * Update the MLO link STA MAC Address
     *
     * @param linkId for the link to be updated.
     * @param macAddress value to be set in the link.
     *
     * @return true on success, false on failure
     *
     * @hide
     */
    public boolean updateMloLinkStaAddress(int linkId, MacAddress macAddress) {
        for (MloLink link : mAffiliatedMloLinks) {
            if (link.getLinkId() == linkId) {
                link.setStaMacAddress(macAddress);
                return true;
            }
        }
        return false;
    }

    /**
     * Update the MLO link State
     *
     * @param linkId for the link to be updated.
     * @param state value to be set in the link as one of {@link MloLink.MloLinkState}
     *
     * @return true on success, false on failure
     *
     * @hide
     */
    public boolean updateMloLinkState(int linkId, @MloLink.MloLinkState int state) {
        if (!MloLink.isValidState(state)) {
            return false;
        }

        for (MloLink link : mAffiliatedMloLinks) {
            if (link.getLinkId() == linkId) {
                link.setState(state);
                return true;
            }
        }
        return false;
    }

    /**
     * Return the basic service set identifier (BSSID) of the current access point. In case of
     * Multi Link Operation (MLO), the BSSID corresponds to the BSSID of the link used for
     * association.
     * <p>
     * The BSSID may be
     * <lt>{@code null}, if there is no network currently connected.</lt>
     * <lt>{@code "02:00:00:00:00:00"}, if the caller has insufficient permissions to access the
     * BSSID.<lt>
     * </p>
     *
     * @return the BSSID, in the form of a six-byte MAC address: {@code XX:XX:XX:XX:XX:XX}
     */
    public String getBSSID() {
        return mBSSID;
    }

    /**
     * Return the Multi-Link Device (MLD) MAC Address for the connected access point.
     * <p>
     * The returned MLD MAC Address will be {@code null} in the following cases:
     * <lt>There is no network currently connected</lt>
     * <lt>The connected access point is not an MLD access point,
     * i.e. {@link #getWifiStandard()} returns {@link ScanResult#WIFI_STANDARD_11BE}.</lt>
     * <lt>The caller has insufficient permissions to access the access point MLD MAC Address.<lt>
     * </p>
     *
     * @return the MLD Mac address
     */
    @Nullable
    public MacAddress getApMldMacAddress() {
        return mApMldMacAddress;
    }

    /**
     * Return the access point Multi-Link Operation (MLO) link-id for Wi-Fi 7 access points.
     * i.e. {@link #getWifiStandard()} returns {@link ScanResult#WIFI_STANDARD_11BE},
     * otherwise return {@link MloLink#INVALID_MLO_LINK_ID}.
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
     * Return the Multi-Link Operation (MLO) affiliated Links for Wi-Fi 7 access points.
     * i.e. when {@link #getWifiStandard()} returns {@link ScanResult#WIFI_STANDARD_11BE}.
     *
     * Affiliated links are the links supported by the Access Point Multi Link Device (AP MLD). The
     * Station Multi Link Device (STA MLD) gathers affiliated link information from scan results.
     * Depending on Station's capability, it associates to all or a subset of affiliated links.
     * <p><b>Note:</b>{@link #getAssociatedMloLinks()} returns associated links.
     *
     * @return List of affiliated MLO links, or an empty list if access point is not Wi-Fi 7
     */
    @NonNull
    public List<MloLink> getAffiliatedMloLinks() {
        return new ArrayList<MloLink>(mAffiliatedMloLinks);
    }

    /** @hide */
    public MloLink getAffiliatedMloLink(int linkId) {
        return mAffiliatedMloLinksMap.get(linkId);
    }

    /**
     * Return the associated Multi-Link Operation (MLO) Links for Wi-Fi 7 access points.
     * i.e. when {@link #getWifiStandard()} returns {@link ScanResult#WIFI_STANDARD_11BE}.
     *
     * Affiliated links are the links supported by the Access Point Multi Link Device (AP MLD). The
     * Station Multi Link Device (STA MLD) gathers affiliated link information from scan results.
     * Depending on Station's capability, it associates to all or a subset of affiliated links.
     * <p><b>Note:</b>{@link #getAffiliatedMloLinks()} returns affiliated links.
     *
     * @return List of associated MLO links, or an empty list if access point is not a multi-link
     * device.
     */
    @NonNull
    public List<MloLink> getAssociatedMloLinks() {
        ArrayList associatedMloLinks = new ArrayList<MloLink>();
        for (MloLink link : getAffiliatedMloLinks()) {
            if (link.getState() == MloLink.MLO_LINK_STATE_IDLE
                    || link.getState() == MloLink.MLO_LINK_STATE_ACTIVE) {
                associatedMloLinks.add(link);
            }
        }
        return associatedMloLinks;
    }

    /**
     * Returns the received signal strength indicator of the current 802.11 network, in dBm. In
     * case of Multi Link Operation (MLO), returned RSSI is the highest of all associated links.
     * <p>
     * Use {@link android.net.wifi.WifiManager#calculateSignalLevel} to convert this number into
     * an absolute signal level which can be displayed to a user.
     * </p>
     *
     * @return the RSSI.
     */
    public int getRssi() {
        return mRssi;
    }

    /** @hide */
    @UnsupportedAppUsage
    public void setRssi(int rssi) {
        if (rssi < INVALID_RSSI)
            rssi = INVALID_RSSI;
        if (rssi > MAX_RSSI)
            rssi = MAX_RSSI;
        mRssi = rssi;
        mLastRssiUpdateMillis = SystemClock.elapsedRealtime();
    }

    /**
     * @hide
     */
    public long getLastRssiUpdateMillis() {
        return mLastRssiUpdateMillis;
    }

    /**
     * Sets the Wi-Fi standard
     * @hide
     */
    public void setWifiStandard(@WifiAnnotations.WifiStandard int wifiStandard) {
        mWifiStandard = wifiStandard;
    }

    /**
     * Get connection Wi-Fi standard
     * @return the connection Wi-Fi standard
     */
    public @WifiAnnotations.WifiStandard int getWifiStandard() {
        return mWifiStandard;
    }

    /**
     * Returns the current link speed in {@link #LINK_SPEED_UNITS}. In case of Multi Link Operation
     * (MLO), returned value is the current link speed of the associated link with the highest RSSI.
     *
     * @return the link speed or {@link #LINK_SPEED_UNKNOWN} if link speed is unknown.
     * @see #LINK_SPEED_UNITS
     * @see #LINK_SPEED_UNKNOWN
     */
    public int getLinkSpeed() {
        return mLinkSpeed;
    }

    /** @hide */
    @UnsupportedAppUsage
    public void setLinkSpeed(int linkSpeed) {
        mLinkSpeed = linkSpeed;
    }

    /**
     * Returns the current transmit link speed in Mbps. In case of Multi Link Operation (MLO),
     * returned value is the current transmit link speed of the associated link with the highest
     * RSSI.
     *
     * @return the Tx link speed or {@link #LINK_SPEED_UNKNOWN} if link speed is unknown.
     * @see #LINK_SPEED_UNKNOWN
     */
    @IntRange(from = -1)
    public int getTxLinkSpeedMbps() {
        return mTxLinkSpeed;
    }

    /**
     * Returns the maximum supported transmit link speed in Mbps
     * @return the max supported tx link speed or {@link #LINK_SPEED_UNKNOWN} if link speed is
     * unknown. @see #LINK_SPEED_UNKNOWN
     */
    public int getMaxSupportedTxLinkSpeedMbps() {
        return mMaxSupportedTxLinkSpeed;
    }

    /**
     * Update the last transmitted packet bit rate in Mbps.
     * @hide
     */
    public void setTxLinkSpeedMbps(int txLinkSpeed) {
        mTxLinkSpeed = txLinkSpeed;
    }

    /**
     * Set the maximum supported transmit link speed in Mbps
     * @hide
     */
    public void setMaxSupportedTxLinkSpeedMbps(int maxSupportedTxLinkSpeed) {
        mMaxSupportedTxLinkSpeed = maxSupportedTxLinkSpeed;
    }

    /**
     * Returns the current receive link speed in Mbps. In case of Multi Link Operation (MLO),
     * returned value is the receive link speed of the associated link with the highest RSSI.
     *
     * @return the Rx link speed or {@link #LINK_SPEED_UNKNOWN} if link speed is unknown.
     * @see #LINK_SPEED_UNKNOWN
     */
    @IntRange(from = -1)
    public int getRxLinkSpeedMbps() {
        return mRxLinkSpeed;
    }

    /**
     * Returns the maximum supported receive link speed in Mbps
     * @return the max supported Rx link speed or {@link #LINK_SPEED_UNKNOWN} if link speed is
     * unknown. @see #LINK_SPEED_UNKNOWN
     */
    public int getMaxSupportedRxLinkSpeedMbps() {
        return mMaxSupportedRxLinkSpeed;
    }

    /**
     * Update the last received packet bit rate in Mbps.
     * @hide
     */
    public void setRxLinkSpeedMbps(int rxLinkSpeed) {
        mRxLinkSpeed = rxLinkSpeed;
    }

    /**
     * Set the maximum supported receive link speed in Mbps
     * @hide
     */
    public void setMaxSupportedRxLinkSpeedMbps(int maxSupportedRxLinkSpeed) {
        mMaxSupportedRxLinkSpeed = maxSupportedRxLinkSpeed;
    }

    /**
     * Returns the current frequency in {@link #FREQUENCY_UNITS}. In case of Multi Link Operation
     * (MLO), returned value is the frequency of the associated link with the highest RSSI.
     *
     * @return the frequency.
     * @see #FREQUENCY_UNITS
     */
    public int getFrequency() {
        return mFrequency;
    }

    /** @hide */
    public void setFrequency(int frequency) {
        this.mFrequency = frequency;
    }

    /**
     * @hide
     */
    public boolean is24GHz() {
        return ScanResult.is24GHz(mFrequency);
    }

    /**
     * @hide
     */
    @UnsupportedAppUsage
    public boolean is5GHz() {
        return ScanResult.is5GHz(mFrequency);
    }

    /**
     * @hide
     */
    public boolean is6GHz() {
        return ScanResult.is6GHz(mFrequency);
    }

    /**
     * Record the MAC address of the WLAN interface
     * @param macAddress the MAC address in {@code XX:XX:XX:XX:XX:XX} form
     * @hide
     */
    @UnsupportedAppUsage
    public void setMacAddress(String macAddress) {
        this.mMacAddress = macAddress;
    }

    /**
     * Returns the MAC address used for this connection. In case of Multi Link Operation (MLO),
     * returned value is the Station MLD MAC address.
     *
     * @return MAC address of the connection or {@code "02:00:00:00:00:00"} if the caller has
     * insufficient permission.
     *
     * Requires {@code android.Manifest.permission#LOCAL_MAC_ADDRESS} and
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     */
    public String getMacAddress() {
        return mMacAddress;
    }

    /**
     * @return true if {@link #getMacAddress()} has a real MAC address.
     *
     * @hide
     */
    public boolean hasRealMacAddress() {
        return mMacAddress != null && !DEFAULT_MAC_ADDRESS.equals(mMacAddress);
    }

    /**
     * Indicates if we've dynamically detected this active network connection as
     * being metered.
     *
     * @see WifiConfiguration#isMetered(WifiConfiguration, WifiInfo)
     * @hide
     */
    public void setMeteredHint(boolean meteredHint) {
        mMeteredHint = meteredHint;
    }

    /** @hide */
    @UnsupportedAppUsage
    public boolean getMeteredHint() {
        return mMeteredHint;
    }

    /** @hide */
    public void setEphemeral(boolean ephemeral) {
        mEphemeral = ephemeral;
    }

    /**
     * Returns true if the current Wifi network is ephemeral, false otherwise.
     * An ephemeral network is a network that is temporary and not persisted in the system.
     * Ephemeral networks cannot be forgotten, only disabled with
     * {@link WifiManager#disableEphemeralNetwork(String)}.
     *
     * @hide
     */
    @SystemApi
    public boolean isEphemeral() {
        return mEphemeral;
    }

    /** @hide */
    public void setTrusted(boolean trusted) {
        mTrusted = trusted;
    }

    /**
     * Returns true if the current Wifi network is a trusted network, false otherwise.
     * @see WifiNetworkSuggestion.Builder#setUntrusted(boolean).
     * @hide
     */
    @SystemApi
    public boolean isTrusted() {
        return mTrusted;
    }

    /** @hide */
    public void setRestricted(boolean restricted) {
        mRestricted = restricted;
    }

    /**
     * Returns true if the current Wifi network is a restricted network, false otherwise.
     * A restricted network has its {@link NetworkCapabilities#NET_CAPABILITY_NOT_RESTRICTED}
     * capability removed.
     * @see WifiNetworkSuggestion.Builder#setRestricted(boolean).
     */
    public boolean isRestricted() {
        return mRestricted;
    }

    /** @hide */
    public void setOemPaid(boolean oemPaid) {
        mOemPaid = oemPaid;
    }

    /**
     * Returns true if the current Wifi network is an oem paid network, false otherwise.
     * @see WifiNetworkSuggestion.Builder#setOemPaid(boolean).
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    public boolean isOemPaid() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return mOemPaid;
    }

    /** @hide */
    public void setOemPrivate(boolean oemPrivate) {
        mOemPrivate = oemPrivate;
    }

    /**
     * Returns true if the current Wifi network is an oem private network, false otherwise.
     * @see WifiNetworkSuggestion.Builder#setOemPrivate(boolean).
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    public boolean isOemPrivate() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return mOemPrivate;
    }

    /**
     * @hide
     */
    public void setCarrierMerged(boolean carrierMerged) {
        mCarrierMerged = carrierMerged;
    }

    /**
     * Returns true if the current Wifi network is a carrier merged network, false otherwise.
     * @see WifiNetworkSuggestion.Builder#setCarrierMerged(boolean).
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.S)
    public boolean isCarrierMerged() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return mCarrierMerged;
    }


    /** @hide */
    public void setOsuAp(boolean osuAp) {
        mOsuAp = osuAp;
    }

    /** @hide */
    @SystemApi
    public boolean isOsuAp() {
        return mOsuAp;
    }

    /** @hide */
    @SystemApi
    public boolean isPasspointAp() {
        return mFqdn != null && mProviderFriendlyName != null;
    }

    /** @hide */
    public void setFQDN(@Nullable String fqdn) {
        mFqdn = fqdn;
    }

    /**
     * Returns the Fully Qualified Domain Name of the network if it is a Passpoint network.
     * <p>
     * The FQDN may be
     * <lt>{@code null} if no network currently connected, currently connected network is not
     * passpoint network or the caller has insufficient permissions to access the FQDN.</lt>
     * </p>
     */
    public @Nullable String getPasspointFqdn() {
        return mFqdn;
    }

    /** @hide */
    public void setProviderFriendlyName(@Nullable String providerFriendlyName) {
        mProviderFriendlyName = providerFriendlyName;
    }

    /**
     * Returns the Provider Friendly Name of the network if it is a Passpoint network.
     * <p>
     * The Provider Friendly Name may be
     * <lt>{@code null} if no network currently connected, currently connected network is not
     * passpoint network or the caller has insufficient permissions to access the Provider Friendly
     * Name. </lt>
     * </p>
     */
    public @Nullable String getPasspointProviderFriendlyName() {
        return mProviderFriendlyName;
    }

    /** @hide */
    public void setRequestingPackageName(@Nullable String packageName) {
        mRequestingPackageName = packageName;
    }

    /**
     * If this network was created in response to an app request (e.g. through Network Suggestion
     * or Network Specifier), return the package name of the app that made the request.
     * Null otherwise.
     * @hide
     */
    @SystemApi
    public @Nullable String getRequestingPackageName() {
        return mRequestingPackageName;
    }

    /** @hide */
    public void setSubscriptionId(int subId) {
        mSubscriptionId = subId;
    }

    /**
     * If this network is provisioned by a carrier, returns subscription Id corresponding to the
     * associated SIM on the device. If this network is not provisioned by a carrier, returns
     * {@link android.telephony.SubscriptionManager#INVALID_SUBSCRIPTION_ID}
     *
     * @see WifiNetworkSuggestion.Builder#setSubscriptionId(int)
     * @see android.telephony.SubscriptionInfo#getSubscriptionId()
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public int getSubscriptionId() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return mSubscriptionId;
    }


    /** @hide */
    @UnsupportedAppUsage
    public void setNetworkId(int id) {
        mNetworkId = id;
    }

    /**
     * Each configured network has a unique small integer ID, used to identify
     * the network. This method returns the ID for the currently connected network.
     * <p>
     * The networkId may be {@code -1} if there is no currently connected network or if the caller
     * has insufficient permissions to access the network ID.
     * </p>
     *
     * @return the network ID.
     */
    public int getNetworkId() {
        return mNetworkId;
    }

    /**
     * Return the detailed state of the supplicant's negotiation with an
     * access point, in the form of a {@link SupplicantState SupplicantState} object.
     * @return the current {@link SupplicantState SupplicantState}
     */
    public SupplicantState getSupplicantState() {
        return mSupplicantState;
    }

    /** @hide */
    @UnsupportedAppUsage
    public void setSupplicantState(SupplicantState state) {
        mSupplicantState = state;
    }

    /** @hide */
    public void setInetAddress(InetAddress address) {
        mIpAddress = address;
    }

    /**
     * @deprecated Use the methods on {@link android.net.LinkProperties} which can be obtained
     * either via {@link NetworkCallback#onLinkPropertiesChanged(Network, LinkProperties)} or
     * {@link ConnectivityManager#getLinkProperties(Network)}.
     */
    @Deprecated
    public int getIpAddress() {
        int result = 0;
        if (mIpAddress instanceof Inet4Address) {
            result = Inet4AddressUtils.inet4AddressToIntHTL((Inet4Address) mIpAddress);
        }
        return result;
    }

    /**
     * @return {@code true} if this network does not broadcast its SSID, so an
     * SSID-specific probe request must be used for scans.
     */
    public boolean getHiddenSSID() {
        return mIsHiddenSsid;
    }

    /**
     * Sets whether or not this network is using a hidden SSID. This value should be set from the
     * corresponding {@link WifiConfiguration} of the network.
     * @hide
     */
    public void setHiddenSSID(boolean isHiddenSsid) {
        mIsHiddenSsid = isHiddenSsid;
    }

    /**
     * Map a supplicant state into a fine-grained network connectivity state.
     * @param suppState the supplicant state
     * @return the corresponding {@link DetailedState}
     */
    public static DetailedState getDetailedStateOf(SupplicantState suppState) {
        return stateMap.get(suppState);
    }

    /**
     * Set the <code>SupplicantState</code> from the string name
     * of the state.
     * @param stateName the name of the state, as a <code>String</code> returned
     * in an event sent by {@code wpa_supplicant}.
     */
    @UnsupportedAppUsage
    void setSupplicantState(String stateName) {
        mSupplicantState = valueOf(stateName);
    }

    static SupplicantState valueOf(String stateName) {
        if ("4WAY_HANDSHAKE".equalsIgnoreCase(stateName))
            return SupplicantState.FOUR_WAY_HANDSHAKE;
        else {
            try {
                return SupplicantState.valueOf(stateName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return SupplicantState.INVALID;
            }
        }
    }

    /**
     * Remove double quotes (") surrounding a SSID string, if present. Otherwise, return the
     * string unmodified. Return null if the input string was null.
     * @hide
     */
    @Nullable
    @SystemApi
    public static String sanitizeSsid(@Nullable String string) {
        return removeDoubleQuotes(string);
    }

    /** @hide */
    @UnsupportedAppUsage
    @Nullable
    public static String removeDoubleQuotes(@Nullable String string) {
        if (string == null) return null;
        final int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String none = "<none>";

        sb.append("SSID: ").append(getSSID())
                .append(", BSSID: ").append(mBSSID == null ? none : mBSSID)
                .append(", MAC: ").append(mMacAddress == null ? none : mMacAddress)
                .append(", IP: ").append(mIpAddress)
                .append(", Security type: ").append(mSecurityType)
                .append(", Supplicant state: ")
                .append(mSupplicantState == null ? none : mSupplicantState)
                .append(", Wi-Fi standard: ").append(ScanResult.wifiStandardToString(mWifiStandard))
                .append(", RSSI: ").append(mRssi)
                .append(", Link speed: ").append(mLinkSpeed).append(LINK_SPEED_UNITS)
                .append(", Tx Link speed: ").append(mTxLinkSpeed).append(LINK_SPEED_UNITS)
                .append(", Max Supported Tx Link speed: ")
                .append(mMaxSupportedTxLinkSpeed).append(LINK_SPEED_UNITS)
                .append(", Rx Link speed: ").append(mRxLinkSpeed).append(LINK_SPEED_UNITS)
                .append(", Max Supported Rx Link speed: ")
                .append(mMaxSupportedRxLinkSpeed).append(LINK_SPEED_UNITS)
                .append(", Frequency: ").append(mFrequency).append(FREQUENCY_UNITS)
                .append(", Net ID: ").append(mNetworkId)
                .append(", Metered hint: ").append(mMeteredHint)
                .append(", score: ").append(Integer.toString(score))
                .append(", isUsable: ").append(mIsUsable)
                .append(", CarrierMerged: ").append(mCarrierMerged)
                .append(", SubscriptionId: ").append(mSubscriptionId)
                .append(", IsPrimary: ").append(mIsPrimary)
                .append(", Trusted: ").append(mTrusted)
                .append(", Restricted: ").append(mRestricted)
                .append(", Ephemeral: ").append(mEphemeral)
                .append(", OEM paid: ").append(mOemPaid)
                .append(", OEM private: ").append(mOemPrivate)
                .append(", OSU AP: ").append(mOsuAp)
                .append(", FQDN: ").append(mFqdn == null ? none : mFqdn)
                .append(", Provider friendly name: ")
                .append(mProviderFriendlyName == null ? none : mProviderFriendlyName)
                .append(", Requesting package name: ")
                .append(mRequestingPackageName == null ? none : mRequestingPackageName)
                .append(mNetworkKey == null ? none : mNetworkKey)
                .append("MLO Information: ")
                .append(", Is TID-To-Link negotiation supported by the AP: ")
                .append(mApTidToLinkMappingNegotiationSupported)
                .append(", AP MLD Address: ").append(
                        mApMldMacAddress == null ? none : mApMldMacAddress.toString())
                .append(", AP MLO Link Id: ").append(
                        mApMldMacAddress == null ? none : mApMloLinkId)
                .append(", AP MLO Affiliated links: ").append(
                        mApMldMacAddress == null ? none : mAffiliatedMloLinks)
                .append(", Vendor Data: ").append(
                        mVendorData == null || mVendorData.isEmpty() ? none : mVendorData);

        return sb.toString();
    }

    /** Implement the Parcelable interface {@hide} */
    public int describeContents() {
        return 0;
    }

    private boolean shouldRedactLocationSensitiveFields(long redactions) {
        return (redactions & NetworkCapabilities.REDACT_FOR_ACCESS_FINE_LOCATION) != 0;
    }

    private boolean shouldRedactLocalMacAddressFields(long redactions) {
        return (redactions & NetworkCapabilities.REDACT_FOR_LOCAL_MAC_ADDRESS) != 0;
    }

    private boolean shouldRedactNetworkSettingsFields(long redactions) {
        return (redactions & NetworkCapabilities.REDACT_FOR_NETWORK_SETTINGS) != 0;
    }

    /** Implement the Parcelable interface {@hide} */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mNetworkId);
        dest.writeInt(mRssi);
        dest.writeInt(mLinkSpeed);
        dest.writeInt(mTxLinkSpeed);
        dest.writeInt(mRxLinkSpeed);
        dest.writeInt(mFrequency);
        if (mIpAddress != null) {
            dest.writeByte((byte)1);
            dest.writeByteArray(mIpAddress.getAddress());
        } else {
            dest.writeByte((byte)0);
        }
        if (mWifiSsid != null) {
            dest.writeInt(1);
            mWifiSsid.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeString(mBSSID);
        dest.writeString(mMacAddress);
        dest.writeInt(mMeteredHint ? 1 : 0);
        dest.writeInt(mEphemeral ? 1 : 0);
        dest.writeInt(mTrusted ? 1 : 0);
        dest.writeInt(mOemPaid ? 1 : 0);
        dest.writeInt(mOemPrivate ? 1 : 0);
        dest.writeInt(mCarrierMerged ? 1 : 0);
        dest.writeInt(score);
        dest.writeBoolean(mIsUsable);
        dest.writeLong(txSuccess);
        dest.writeDouble(mSuccessfulTxPacketsPerSecond);
        dest.writeLong(txRetries);
        dest.writeDouble(mTxRetriedTxPacketsPerSecond);
        dest.writeLong(txBad);
        dest.writeDouble(mLostTxPacketsPerSecond);
        dest.writeLong(rxSuccess);
        dest.writeDouble(mSuccessfulRxPacketsPerSecond);
        mSupplicantState.writeToParcel(dest, flags);
        dest.writeInt(mOsuAp ? 1 : 0);
        dest.writeString(mRequestingPackageName);
        dest.writeString(mFqdn);
        dest.writeString(mProviderFriendlyName);
        dest.writeInt(mWifiStandard);
        dest.writeInt(mMaxSupportedTxLinkSpeed);
        dest.writeInt(mMaxSupportedRxLinkSpeed);
        dest.writeString(mPasspointUniqueId);
        dest.writeInt(mSubscriptionId);
        dest.writeTypedList(mInformationElements);
        if (SdkLevel.isAtLeastS()) {
            dest.writeInt(mIsPrimary);
        }
        dest.writeInt(mSecurityType);
        dest.writeInt(mRestricted ? 1 : 0);
        dest.writeString(mNetworkKey);
        dest.writeParcelable(mApMldMacAddress, flags);
        dest.writeInt(mApMloLinkId);
        dest.writeTypedList(mAffiliatedMloLinks);
        dest.writeBoolean(mApTidToLinkMappingNegotiationSupported);
        dest.writeList(mVendorData);
    }

    /** Implement the Parcelable interface {@hide} */
    @UnsupportedAppUsage
    public static final @android.annotation.NonNull Creator<WifiInfo> CREATOR =
        new Creator<WifiInfo>() {
            public WifiInfo createFromParcel(Parcel in) {
                WifiInfo info = new WifiInfo();
                info.setNetworkId(in.readInt());
                info.setRssi(in.readInt());
                info.setLinkSpeed(in.readInt());
                info.setTxLinkSpeedMbps(in.readInt());
                info.setRxLinkSpeedMbps(in.readInt());
                info.setFrequency(in.readInt());
                if (in.readByte() == 1) {
                    try {
                        info.setInetAddress(InetAddress.getByAddress(in.createByteArray()));
                    } catch (UnknownHostException e) {}
                }
                if (in.readInt() == 1) {
                    info.mWifiSsid = WifiSsid.CREATOR.createFromParcel(in);
                }
                info.mBSSID = in.readString();
                info.mMacAddress = in.readString();
                info.mMeteredHint = in.readInt() != 0;
                info.mEphemeral = in.readInt() != 0;
                info.mTrusted = in.readInt() != 0;
                info.mOemPaid = in.readInt() != 0;
                info.mOemPrivate = in.readInt() != 0;
                info.mCarrierMerged = in.readInt() != 0;
                info.score = in.readInt();
                info.mIsUsable = in.readBoolean();
                info.txSuccess = in.readLong();
                info.mSuccessfulTxPacketsPerSecond = in.readDouble();
                info.txRetries = in.readLong();
                info.mTxRetriedTxPacketsPerSecond = in.readDouble();
                info.txBad = in.readLong();
                info.mLostTxPacketsPerSecond = in.readDouble();
                info.rxSuccess = in.readLong();
                info.mSuccessfulRxPacketsPerSecond = in.readDouble();
                info.mSupplicantState = SupplicantState.CREATOR.createFromParcel(in);
                info.mOsuAp = in.readInt() != 0;
                info.mRequestingPackageName = in.readString();
                info.mFqdn = in.readString();
                info.mProviderFriendlyName = in.readString();
                info.mWifiStandard = in.readInt();
                info.mMaxSupportedTxLinkSpeed = in.readInt();
                info.mMaxSupportedRxLinkSpeed = in.readInt();
                info.mPasspointUniqueId = in.readString();
                info.mSubscriptionId = in.readInt();
                info.mInformationElements = in.createTypedArrayList(
                        ScanResult.InformationElement.CREATOR);
                if (SdkLevel.isAtLeastS()) {
                    info.mIsPrimary = in.readInt();
                }
                info.mSecurityType = in.readInt();
                info.mRestricted = in.readInt() != 0;
                info.mNetworkKey = in.readString();

                info.mApMldMacAddress = in.readParcelable(MacAddress.class.getClassLoader());
                info.mApMloLinkId = in.readInt();
                info.mAffiliatedMloLinks = in.createTypedArrayList(MloLink.CREATOR);
                info.mApTidToLinkMappingNegotiationSupported = in.readBoolean();
                info.mVendorData = ParcelUtil.readOuiKeyedDataList(in);
                return info;
            }

            public WifiInfo[] newArray(int size) {
                return new WifiInfo[size];
            }
        };

    /**
     * Set the Passpoint unique identifier for the current connection
     *
     * @param passpointUniqueId Unique identifier
     * @hide
     */
    public void setPasspointUniqueId(@Nullable String passpointUniqueId) {
        mPasspointUniqueId = passpointUniqueId;
    }

    /**
     * Get the Passpoint unique identifier for the current connection
     *
     * @return Passpoint unique identifier, or null if this connection is not Passpoint.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public @Nullable String getPasspointUniqueId() {
        return mPasspointUniqueId;
    }

    /**
     * Set the information elements found in the becaon of the connected bssid.
     * @hide
     */
    public void setInformationElements(@Nullable List<ScanResult.InformationElement> infoElements) {
        if (infoElements == null) {
            mInformationElements = null;
            return;
        }
        mInformationElements = new ArrayList<>(infoElements);
    }

    /**
     * Get all information elements found in the beacon of the connected bssid.
     * <p>
     * The information elements will be {@code null} if there is no network currently connected or
     * if the caller has insufficient permissions to access the info elements.
     * </p>
     *
     * @return List of information elements {@link ScanResult.InformationElement} or null.
     */
    @Nullable
    @SuppressWarnings("NullableCollection")
    public List<ScanResult.InformationElement> getInformationElements() {
        if (mInformationElements == null) return null;
        return new ArrayList<>(mInformationElements);
    }

    /**
     * @see #isPrimary()
     * @hide
     */
    public void setIsPrimary(boolean isPrimary) {
        mIsPrimary = isPrimary ? IS_PRIMARY_TRUE : IS_PRIMARY_FALSE;
    }

    /**
     * Returns whether this is the primary wifi connection or not.
     *
     * Wifi service considers this connection to be the best among all Wifi connections, and this
     * connection should be the one surfaced to the user if only one can be displayed.
     *
     * Note that the default route (chosen by Connectivity Service) may not correspond to the
     * primary Wifi connection e.g. when there exists a better cellular network, or if the
     * primary Wifi connection doesn't have internet access.
     *
     * @return whether this is the primary connection or not.
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.NETWORK_SETTINGS)
    @SystemApi
    public boolean isPrimary() {
        if (!SdkLevel.isAtLeastS()) {
            // Intentional - since we don't support STA + STA on older devices, this field
            // is redundant. Don't allow anyone to use this.
            throw new UnsupportedOperationException();
        }
        if (mIsPrimary == IS_PRIMARY_NO_PERMISSION) {
            throw new SecurityException("Not allowed to access this field");
        }
        return mIsPrimary == IS_PRIMARY_TRUE;
    }

    private List<MloLink> getSortedMloLinkList(List<MloLink> list) {
        List<MloLink> newList = new ArrayList<MloLink>(list);
        Collections.sort(newList, new Comparator<MloLink>() {
            @Override
            public int compare(MloLink lhs, MloLink rhs) {
                return lhs.getLinkId() -  rhs.getLinkId();
            }
        });

        return newList;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;

        // Potential API behavior change, so don't change behavior on older devices.
        if (!SdkLevel.isAtLeastS()) return false;

        if (!(that instanceof WifiInfo)) return false;

        WifiInfo thatWifiInfo = (WifiInfo) that;

        // compare the MLO affiliated links irrespective of the order
        if (!Objects.equals(getSortedMloLinkList(mAffiliatedMloLinks),
                  getSortedMloLinkList(thatWifiInfo.mAffiliatedMloLinks))) {
            return false;
        }

        return Objects.equals(mWifiSsid, thatWifiInfo.mWifiSsid)
                && Objects.equals(mBSSID, thatWifiInfo.mBSSID)
                && Objects.equals(mApMldMacAddress, thatWifiInfo.mApMldMacAddress)
                && mApMloLinkId == thatWifiInfo.mApMloLinkId
                && Objects.equals(mNetworkId, thatWifiInfo.mNetworkId)
                && Objects.equals(mRssi, thatWifiInfo.mRssi)
                && Objects.equals(mSupplicantState, thatWifiInfo.mSupplicantState)
                && Objects.equals(mLinkSpeed, thatWifiInfo.mLinkSpeed)
                && Objects.equals(mTxLinkSpeed, thatWifiInfo.mTxLinkSpeed)
                && Objects.equals(mRxLinkSpeed, thatWifiInfo.mRxLinkSpeed)
                && Objects.equals(mFrequency, thatWifiInfo.mFrequency)
                && Objects.equals(mIpAddress, thatWifiInfo.mIpAddress)
                && Objects.equals(mMacAddress, thatWifiInfo.mMacAddress)
                && Objects.equals(mMeteredHint, thatWifiInfo.mMeteredHint)
                && Objects.equals(mEphemeral, thatWifiInfo.mEphemeral)
                && Objects.equals(mTrusted, thatWifiInfo.mTrusted)
                && Objects.equals(mOemPaid, thatWifiInfo.mOemPaid)
                && Objects.equals(mOemPrivate, thatWifiInfo.mOemPrivate)
                && Objects.equals(mCarrierMerged, thatWifiInfo.mCarrierMerged)
                && Objects.equals(mRequestingPackageName, thatWifiInfo.mRequestingPackageName)
                && Objects.equals(mOsuAp, thatWifiInfo.mOsuAp)
                && Objects.equals(mFqdn, thatWifiInfo.mFqdn)
                && Objects.equals(mProviderFriendlyName, thatWifiInfo.mProviderFriendlyName)
                && Objects.equals(mSubscriptionId, thatWifiInfo.mSubscriptionId)
                && Objects.equals(txBad, thatWifiInfo.txBad)
                && Objects.equals(txRetries, thatWifiInfo.txRetries)
                && Objects.equals(txSuccess, thatWifiInfo.txSuccess)
                && Objects.equals(rxSuccess, thatWifiInfo.rxSuccess)
                && Objects.equals(mLostTxPacketsPerSecond, thatWifiInfo.mLostTxPacketsPerSecond)
                && Objects.equals(mTxRetriedTxPacketsPerSecond,
                thatWifiInfo.mTxRetriedTxPacketsPerSecond)
                && Objects.equals(mSuccessfulTxPacketsPerSecond,
                thatWifiInfo.mSuccessfulTxPacketsPerSecond)
                && Objects.equals(mSuccessfulRxPacketsPerSecond,
                thatWifiInfo.mSuccessfulRxPacketsPerSecond)
                && Objects.equals(score, thatWifiInfo.score)
                && Objects.equals(mIsUsable, thatWifiInfo.mIsUsable)
                && Objects.equals(mWifiStandard, thatWifiInfo.mWifiStandard)
                && Objects.equals(mMaxSupportedTxLinkSpeed, thatWifiInfo.mMaxSupportedTxLinkSpeed)
                && Objects.equals(mMaxSupportedRxLinkSpeed, thatWifiInfo.mMaxSupportedRxLinkSpeed)
                && Objects.equals(mPasspointUniqueId, thatWifiInfo.mPasspointUniqueId)
                && Objects.equals(mInformationElements, thatWifiInfo.mInformationElements)
                && mIsPrimary == thatWifiInfo.mIsPrimary
                && mSecurityType == thatWifiInfo.mSecurityType
                && mRestricted == thatWifiInfo.mRestricted
                && Objects.equals(mNetworkKey, thatWifiInfo.mNetworkKey)
                && mApTidToLinkMappingNegotiationSupported
                == thatWifiInfo.mApTidToLinkMappingNegotiationSupported
                && Objects.equals(mVendorData, thatWifiInfo.mVendorData);
    }

    @Override
    public int hashCode() {
        // Potential API behavior change, so don't change behavior on older devices.
        if (!SdkLevel.isAtLeastS()) return System.identityHashCode(this);

        return Objects.hash(mWifiSsid,
                mBSSID,
                mApMldMacAddress,
                mApMloLinkId,
                mAffiliatedMloLinks,
                mNetworkId,
                mRssi,
                mSupplicantState,
                mLinkSpeed,
                mTxLinkSpeed,
                mRxLinkSpeed,
                mFrequency,
                mIpAddress,
                mMacAddress,
                mMeteredHint,
                mEphemeral,
                mTrusted,
                mOemPaid,
                mOemPrivate,
                mCarrierMerged,
                mRequestingPackageName,
                mOsuAp,
                mFqdn,
                mProviderFriendlyName,
                mSubscriptionId,
                txBad,
                txRetries,
                txSuccess,
                rxSuccess,
                mLostTxPacketsPerSecond,
                mTxRetriedTxPacketsPerSecond,
                mSuccessfulTxPacketsPerSecond,
                mSuccessfulRxPacketsPerSecond,
                score,
                mIsUsable,
                mWifiStandard,
                mMaxSupportedTxLinkSpeed,
                mMaxSupportedRxLinkSpeed,
                mPasspointUniqueId,
                mInformationElements,
                mIsPrimary,
                mSecurityType,
                mRestricted,
                mNetworkKey,
                mApTidToLinkMappingNegotiationSupported,
                mVendorData);
    }

    /**
     * Create a copy of a {@link WifiInfo} with some fields redacted based on the permissions
     * held by the receiving app.
     *
     * @param redactions bitmask of redactions that needs to be performed on this instance.
     * @return Copy of this instance with the necessary redactions.
     */
    @Override
    @NonNull
    public WifiInfo makeCopy(long redactions) {
        return new WifiInfo(this, redactions);
    }

    /**
     * Returns a bitmask of all the applicable redactions (based on the permissions held by the
     * receiving app) to be performed on this TransportInfo.
     *
     * @return bitmask of redactions applicable on this instance.
     */
    @Override
    public long getApplicableRedactions() {
        return NetworkCapabilities.REDACT_FOR_ACCESS_FINE_LOCATION
                | NetworkCapabilities.REDACT_FOR_LOCAL_MAC_ADDRESS
                | NetworkCapabilities.REDACT_FOR_NETWORK_SETTINGS;
    }

    /**
     * Set the security type of the current connection
     * @hide
     */
    public void setCurrentSecurityType(@WifiConfiguration.SecurityType int securityType) {
        mSecurityType = convertWifiConfigurationSecurityType(securityType);
    }

    /**
     * Clear the last set security type
     * @hide
     */
    public void clearCurrentSecurityType() {
        mSecurityType = SECURITY_TYPE_UNKNOWN;
    }

    /**
     * Returns the security type of the current 802.11 network connection.
     *
     * @return the security type, or {@link #SECURITY_TYPE_UNKNOWN} if not currently connected.
     */
    public @WifiAnnotations.SecurityType int getCurrentSecurityType() {
        return mSecurityType;
    }

    /**
     * Converts the WifiConfiguration.SecurityType to a WifiInfo.SecurityType
     * @param wifiConfigSecurity WifiConfiguration.SecurityType to convert
     * @return security type as a WifiInfo.SecurityType
     * @hide
     */
    public static @WifiAnnotations.SecurityType int convertWifiConfigurationSecurityType(
            @WifiConfiguration.SecurityType int wifiConfigSecurity) {
        switch (wifiConfigSecurity) {
            case WifiConfiguration.SECURITY_TYPE_OPEN:
                return SECURITY_TYPE_OPEN;
            case WifiConfiguration.SECURITY_TYPE_WEP:
                return SECURITY_TYPE_WEP;
            case WifiConfiguration.SECURITY_TYPE_PSK:
                return SECURITY_TYPE_PSK;
            case WifiConfiguration.SECURITY_TYPE_EAP:
                return SECURITY_TYPE_EAP;
            case WifiConfiguration.SECURITY_TYPE_SAE:
                return SECURITY_TYPE_SAE;
            case WifiConfiguration.SECURITY_TYPE_OWE:
                return SECURITY_TYPE_OWE;
            case WifiConfiguration.SECURITY_TYPE_WAPI_PSK:
                return SECURITY_TYPE_WAPI_PSK;
            case WifiConfiguration.SECURITY_TYPE_WAPI_CERT:
                return SECURITY_TYPE_WAPI_CERT;
            case WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE:
                return SECURITY_TYPE_EAP_WPA3_ENTERPRISE;
            case WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT:
                return SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT;
            case WifiConfiguration.SECURITY_TYPE_PASSPOINT_R1_R2:
                return SECURITY_TYPE_PASSPOINT_R1_R2;
            case WifiConfiguration.SECURITY_TYPE_PASSPOINT_R3:
                return SECURITY_TYPE_PASSPOINT_R3;
            case WifiConfiguration.SECURITY_TYPE_DPP:
                return SECURITY_TYPE_DPP;
            default:
                return SECURITY_TYPE_UNKNOWN;
        }
    }

    /**
     * Utility method to convert WifiInfo.SecurityType to DevicePolicyManager.WifiSecurity
     * @param securityType WifiInfo.SecurityType to convert
     * @return DevicePolicyManager.WifiSecurity security level, or
     * {@link #DPM_SECURITY_TYPE_UNKNOWN} for unknown security types
     * @hide
     */
    public static int convertSecurityTypeToDpmWifiSecurity(
            @WifiAnnotations.SecurityType int securityType) {
        switch (securityType) {
            case SECURITY_TYPE_OPEN:
            case SECURITY_TYPE_OWE:
                return DevicePolicyManager.WIFI_SECURITY_OPEN;
            case SECURITY_TYPE_WEP:
            case SECURITY_TYPE_PSK:
            case SECURITY_TYPE_SAE:
            case SECURITY_TYPE_WAPI_PSK:
                return DevicePolicyManager.WIFI_SECURITY_PERSONAL;
            case SECURITY_TYPE_EAP:
            case SECURITY_TYPE_EAP_WPA3_ENTERPRISE:
            case SECURITY_TYPE_PASSPOINT_R1_R2:
            case SECURITY_TYPE_PASSPOINT_R3:
            case SECURITY_TYPE_WAPI_CERT:
                return DevicePolicyManager.WIFI_SECURITY_ENTERPRISE_EAP;
            case SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT:
                return DevicePolicyManager.WIFI_SECURITY_ENTERPRISE_192;
            default:
                return DPM_SECURITY_TYPE_UNKNOWN;
        }
    }

    /**
     * Set the network key for the current Wi-Fi network.
     *
     * Now we are using this identity to be a key when storing Wi-Fi data usage data.
     * See: {@link WifiConfiguration#getNetworkKeyFromSecurityType(int)}.
     *
     * @param currentNetworkKey the network key of the current Wi-Fi network.
     * @hide
     */
    public void setNetworkKey(@NonNull String currentNetworkKey) {
        mNetworkKey = currentNetworkKey;
    }

    /**
     * Returns the network key of the current Wi-Fi network.
     *
     * The network key may be {@code null}, if there is no network currently connected
     * or if the caller has insufficient permissions to access the network key.
     *
     * @hide
     */
    @SystemApi
    @Nullable
    public String getNetworkKey() {
        return mNetworkKey;
    }

    /**
     * TID-to-Link mapping negotiation is an optional feature. This API returns whether the feature
     * is supported by the AP.
     *
     * @return Return true if TID-to-Link mapping negotiation is supported by the AP, otherwise
     * false.
     *
     * @hide
     */
    @SystemApi
    public boolean isApTidToLinkMappingNegotiationSupported() {
        return mApTidToLinkMappingNegotiationSupported;
    }

    /** @hide */
    public void enableApTidToLinkMappingNegotiationSupport(boolean enable) {
        mApTidToLinkMappingNegotiationSupported = enable;
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
        if (vendorData == null) {
            throw new IllegalArgumentException("setVendorData received a null value");
        }
        mVendorData = new ArrayList<>(vendorData);
    }
}

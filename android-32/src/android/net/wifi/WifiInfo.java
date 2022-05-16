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
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo.DetailedState;
import android.net.TransportInfo;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;
import com.android.net.module.util.Inet4AddressUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
    private int mNetworkId;
    private int mSecurityType;

    /**
     * Used to indicate that the RSSI is invalid, for example if no RSSI measurements are available
     * yet.
     * @hide
     */
    @SystemApi
    public static final int INVALID_RSSI = -127;

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

    /**
     * Security type of current connection.
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "SECURITY_TYPE_" }, value = {
            SECURITY_TYPE_UNKNOWN,
            SECURITY_TYPE_OPEN,
            SECURITY_TYPE_WEP,
            SECURITY_TYPE_PSK,
            SECURITY_TYPE_EAP,
            SECURITY_TYPE_SAE,
            SECURITY_TYPE_OWE,
            SECURITY_TYPE_WAPI_PSK,
            SECURITY_TYPE_WAPI_CERT,
            SECURITY_TYPE_EAP_WPA3_ENTERPRISE,
            SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT,
            SECURITY_TYPE_PASSPOINT_R1_R2,
            SECURITY_TYPE_PASSPOINT_R3,
    })
    public @interface SecurityType {}

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
     * Average rate of lost transmitted packets, in units of packets per second.
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
     * Average rate of transmitted retry packets, in units of packets per second.
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
     * Average rate of successfully transmitted unicast packets, in units of packets per second.
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
     * Average rate of received unicast data packets, in units of packets per second.
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

    /** @hide */
    @UnsupportedAppUsage
    public WifiInfo() {
        mWifiSsid = null;
        mBSSID = null;
        mNetworkId = -1;
        mSupplicantState = SupplicantState.UNINITIALIZED;
        mRssi = INVALID_RSSI;
        mLinkSpeed = LINK_SPEED_UNKNOWN;
        mFrequency = -1;
        mSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mSecurityType = -1;
        mIsPrimary = IS_PRIMARY_FALSE;
        mNetworkKey = null;
    }

    /** @hide */
    public void reset() {
        setInetAddress(null);
        setBSSID(null);
        setSSID(null);
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
        txBad = 0;
        txSuccess = 0;
        rxSuccess = 0;
        txRetries = 0;
        mLostTxPacketsPerSecond = 0;
        mSuccessfulTxPacketsPerSecond = 0;
        mSuccessfulRxPacketsPerSecond = 0;
        mTxRetriedTxPacketsPerSecond = 0;
        score = 0;
        mSecurityType = -1;
        mNetworkKey = null;
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
            mWifiSsid = shouldRedactLocationSensitiveFields(redactions)
                    ? WifiSsid.createFromHex(null) : source.mWifiSsid;
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
            mOemPaid = source.mOemPaid;
            mOemPrivate = source.mOemPrivate;
            mCarrierMerged = source.mCarrierMerged;
            mRequestingPackageName =
                    source.mRequestingPackageName;
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
            mWifiInfo.setSSID(WifiSsid.createFromByteArray(ssid));
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
         * Set the current security type
         * @see WifiInfo#getCurrentSecurityType()
         */
        @NonNull
        public Builder setCurrentSecurityType(@WifiConfiguration.SecurityType int securityType) {
            mWifiInfo.setCurrentSecurityType(securityType);
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
            String unicode = mWifiSsid.toString();
            if (!TextUtils.isEmpty(unicode)) {
                return "\"" + unicode + "\"";
            } else {
                String hex = mWifiSsid.getHexString();
                return (hex != null) ? hex : WifiManager.UNKNOWN_SSID;
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
     * Return the basic service set identifier (BSSID) of the current access point.
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
     * Returns the received signal strength indicator of the current 802.11
     * network, in dBm.
     *
     * <p>Use {@link android.net.wifi.WifiManager#calculateSignalLevel} to convert this number into
     * an absolute signal level which can be displayed to a user.
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
     * Returns the current link speed in {@link #LINK_SPEED_UNITS}.
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
     * Returns the current transmit link speed in Mbps.
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
     * Returns the current receive link speed in Mbps.
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
     * Returns the current frequency in {@link #FREQUENCY_UNITS}.
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
     * Returns the MAC address used for this connection.
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

    /** {@hide} */
    @UnsupportedAppUsage
    public boolean getMeteredHint() {
        return mMeteredHint;
    }

    /** {@hide} */
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

    /** {@hide} */
    public void setTrusted(boolean trusted) {
        mTrusted = trusted;
    }

    /**
     * Returns true if the current Wifi network is a trusted network, false otherwise.
     * @see WifiNetworkSuggestion.Builder#setUntrusted(boolean).
     * {@hide}
     */
    @SystemApi
    public boolean isTrusted() {
        return mTrusted;
    }

    /** {@hide} */
    public void setOemPaid(boolean oemPaid) {
        mOemPaid = oemPaid;
    }

    /**
     * Returns true if the current Wifi network is an oem paid network, false otherwise.
     * @see WifiNetworkSuggestion.Builder#setOemPaid(boolean).
     * {@hide}
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    public boolean isOemPaid() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return mOemPaid;
    }

    /** {@hide} */
    public void setOemPrivate(boolean oemPrivate) {
        mOemPrivate = oemPrivate;
    }

    /**
     * Returns true if the current Wifi network is an oem private network, false otherwise.
     * @see WifiNetworkSuggestion.Builder#setOemPrivate(boolean).
     * {@hide}
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
     * {@hide}
     */
    public void setCarrierMerged(boolean carrierMerged) {
        mCarrierMerged = carrierMerged;
    }

    /**
     * Returns true if the current Wifi network is a carrier merged network, false otherwise.
     * @see WifiNetworkSuggestion.Builder#setCarrierMerged(boolean).
     * {@hide}
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.S)
    public boolean isCarrierMerged() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return mCarrierMerged;
    }


    /** {@hide} */
    public void setOsuAp(boolean osuAp) {
        mOsuAp = osuAp;
    }

    /** {@hide} */
    @SystemApi
    public boolean isOsuAp() {
        return mOsuAp;
    }

    /** {@hide} */
    @SystemApi
    public boolean isPasspointAp() {
        return mFqdn != null && mProviderFriendlyName != null;
    }

    /** {@hide} */
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

    /** {@hide} */
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

    /** {@hide} */
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

    /** {@hide} */
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
        if (mWifiSsid == null) return false;
        return mWifiSsid.isHidden();
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
                .append(", Security type: ").append(mSecurityType)
                .append(", Supplicant state: ")
                .append(mSupplicantState == null ? none : mSupplicantState)
                .append(", Wi-Fi standard: ").append(mWifiStandard)
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
                .append(", CarrierMerged: ").append(mCarrierMerged)
                .append(", SubscriptionId: ").append(mSubscriptionId)
                .append(", IsPrimary: ").append(mIsPrimary)
                .append(mNetworkKey == null ? none : mNetworkKey);
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
        dest.writeString(mNetworkKey);
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
                info.mNetworkKey = in.readString();
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
     * @return Passpoint unique identifier
     * @hide
     */
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

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;

        // Potential API behavior change, so don't change behavior on older devices.
        if (!SdkLevel.isAtLeastS()) return false;

        if (!(that instanceof WifiInfo)) return false;

        WifiInfo thatWifiInfo = (WifiInfo) that;
        return Objects.equals(mWifiSsid, thatWifiInfo.mWifiSsid)
                && Objects.equals(mBSSID, thatWifiInfo.mBSSID)
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
                && Objects.equals(mWifiStandard, thatWifiInfo.mWifiStandard)
                && Objects.equals(mMaxSupportedTxLinkSpeed, thatWifiInfo.mMaxSupportedTxLinkSpeed)
                && Objects.equals(mMaxSupportedRxLinkSpeed, thatWifiInfo.mMaxSupportedRxLinkSpeed)
                && Objects.equals(mPasspointUniqueId, thatWifiInfo.mPasspointUniqueId)
                && Objects.equals(mInformationElements, thatWifiInfo.mInformationElements)
                && Objects.equals(mIsPrimary, thatWifiInfo.mIsPrimary)
                && Objects.equals(mSecurityType, thatWifiInfo.mSecurityType)
                && Objects.equals(mNetworkKey, thatWifiInfo.mNetworkKey);
    }

    @Override
    public int hashCode() {
        // Potential API behavior change, so don't change behavior on older devices.
        if (!SdkLevel.isAtLeastS()) return System.identityHashCode(this);

        return Objects.hash(mWifiSsid,
                mBSSID,
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
                mWifiStandard,
                mMaxSupportedTxLinkSpeed,
                mMaxSupportedRxLinkSpeed,
                mPasspointUniqueId,
                mInformationElements,
                mIsPrimary,
                mSecurityType,
                mNetworkKey);
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
        mSecurityType = convertSecurityTypeToWifiInfo(securityType);
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
    public @SecurityType int getCurrentSecurityType() {
        return mSecurityType;
    }

    private @SecurityType int convertSecurityTypeToWifiInfo(
            @WifiConfiguration.SecurityType int securityType) {
        switch (securityType) {
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
            default:
                return SECURITY_TYPE_UNKNOWN;
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
}

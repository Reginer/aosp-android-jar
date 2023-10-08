/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.net.wifi.p2p;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.MacAddress;
import android.net.wifi.WpsInfo;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.regex.PatternSyntaxException;

/**
 * A class representing a Wi-Fi P2p configuration for setting up a connection
 *
 * {@see WifiP2pManager}
 */
public class WifiP2pConfig implements Parcelable {

    /**
     * The device MAC address uniquely identifies a Wi-Fi p2p device
     */
    public String deviceAddress = "";

    /**
     * Wi-Fi Protected Setup information
     */
    public WpsInfo wps;

    /** Get the network name of this P2P configuration, or null if unset. */
    @Nullable
    public String getNetworkName() {
        return networkName;
    }

    /** @hide */
    public String networkName = "";

    /** Get the passphrase of this P2P configuration, or null if unset. */
    @Nullable
    public String getPassphrase() {
        return passphrase;
    }

    /** @hide */
    public String passphrase = "";

    /**
     * Get the required band for the group owner.
     * The result will be one of the following:
     * {@link #GROUP_OWNER_BAND_AUTO},
     * {@link #GROUP_OWNER_BAND_2GHZ},
     * {@link #GROUP_OWNER_BAND_5GHZ}
     */
    @GroupOperatingBandType
    public int getGroupOwnerBand() {
        return groupOwnerBand;
    }

    /** @hide */
    @GroupOperatingBandType
    public int groupOwnerBand = GROUP_OWNER_BAND_AUTO;

    /** @hide */
    @IntDef(flag = false, prefix = { "GROUP_OWNER_BAND_" }, value = {
        GROUP_OWNER_BAND_AUTO,
        GROUP_OWNER_BAND_2GHZ,
        GROUP_OWNER_BAND_5GHZ
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface GroupOperatingBandType {}

    /**
     * IP provisioning via IPv4 DHCP, when joining a group as a group client.
     */
    public static final int GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP = 0;

    /**
     * IP provisioning via IPv6 link-local, when joining a group as a group client.
     */
    public static final int GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL = 1;

    /**
     * Allow the system to pick the operating frequency from all supported bands.
     */
    public static final int GROUP_OWNER_BAND_AUTO = 0;
    /**
     * Allow the system to pick the operating frequency from the 2.4 GHz band.
     */
    public static final int GROUP_OWNER_BAND_2GHZ = 1;
    /**
     * Allow the system to pick the operating frequency from the 5 GHz band.
     */
    public static final int GROUP_OWNER_BAND_5GHZ = 2;

    /**
     * The least inclination to be a group owner, to be filled in the field
     * {@link #groupOwnerIntent}.
     */
    public static final int GROUP_OWNER_INTENT_MIN = 0;

    /**
     * The most inclination to be a group owner, to be filled in the field
     * {@link #groupOwnerIntent}.
     */
    public static final int GROUP_OWNER_INTENT_MAX = 15;

    /**
     * The system can choose an appropriate owner intent value, to be filled in the field
     * {@link #groupOwnerIntent}.
     */
    public static final int GROUP_OWNER_INTENT_AUTO = -1;

    /**
     * This is an integer value between {@link #GROUP_OWNER_INTENT_MIN} and
     * {@link #GROUP_OWNER_INTENT_MAX} where
     * {@link #GROUP_OWNER_INTENT_MIN} indicates the least inclination to be a group owner and
     * {@link #GROUP_OWNER_INTENT_MAX} indicates the highest inclination to be a group owner.
     *
     * A value of {@link #GROUP_OWNER_INTENT_AUTO} indicates the system can choose an appropriate
     * value.
     *
     * By default this field is set to {@link #GROUP_OWNER_INTENT_AUTO}.
     */
    @IntRange(from = 0, to = 15)
    public int groupOwnerIntent = GROUP_OWNER_INTENT_AUTO;

    /** @hide */
    @IntDef(prefix = { "GROUP_CLIENT_IP_PROVISIONING_MODE_" }, value = {
            GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP,
            GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface GroupClientIpProvisioningMode {}

    @GroupClientIpProvisioningMode
    private int mGroupClientIpProvisioningMode = GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP;

    /**
     * Query whether or not join existing group is enabled/disabled.
     * @see #setJoinExistingGroup(boolean)
     *
     * @return true if configured to trigger the join existing group logic. False otherwise.
     * @hide
     */
    @SystemApi
    public boolean isJoinExistingGroup() {
        return mJoinExistingGroup;
    }

    /**
     * Join an existing group as a client.
     */
    private boolean mJoinExistingGroup = false;

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int netId = WifiP2pGroup.NETWORK_ID_PERSISTENT;

    /**
     * Get the network ID of this P2P configuration.
     * @return either a non-negative network ID, or one of
     * {@link WifiP2pGroup#NETWORK_ID_PERSISTENT} or {@link WifiP2pGroup#NETWORK_ID_TEMPORARY}.
     */
    public int getNetworkId() {
        return netId;
    }

    public WifiP2pConfig() {
        //set defaults
        wps = new WpsInfo();
        wps.setup = WpsInfo.PBC;
    }

    /** @hide */
    public void invalidate() {
        deviceAddress = "";
    }

    /** P2P-GO-NEG-REQUEST 42:fc:89:a8:96:09 dev_passwd_id=4 {@hide}*/
    @UnsupportedAppUsage
    public WifiP2pConfig(String supplicantEvent) throws IllegalArgumentException {
        String[] tokens = supplicantEvent.split(" ");

        if (tokens.length < 2 || !tokens[0].equals("P2P-GO-NEG-REQUEST")) {
            throw new IllegalArgumentException("Malformed supplicant event");
        }

        deviceAddress = tokens[1];
        wps = new WpsInfo();

        if (tokens.length > 2) {
            String[] nameVal = tokens[2].split("=");
            int devPasswdId;
            try {
                devPasswdId = Integer.parseInt(nameVal[1]);
            } catch (NumberFormatException e) {
                devPasswdId = 0;
            }
            //Based on definitions in wps/wps_defs.h
            switch (devPasswdId) {
                //DEV_PW_USER_SPECIFIED = 0x0001,
                case 0x01:
                    wps.setup = WpsInfo.DISPLAY;
                    break;
                //DEV_PW_PUSHBUTTON = 0x0004,
                case 0x04:
                    wps.setup = WpsInfo.PBC;
                    break;
                //DEV_PW_REGISTRAR_SPECIFIED = 0x0005
                case 0x05:
                    wps.setup = WpsInfo.KEYPAD;
                    break;
                default:
                    wps.setup = WpsInfo.PBC;
                    break;
            }
        }
    }

    /**
     * Get the IP provisioning mode when joining a group as a group client.
     * The result will be one of the following:
     * {@link #GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP},
     * {@link #GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL}
     */
    @GroupClientIpProvisioningMode
    public int getGroupClientIpProvisioningMode() {
        return mGroupClientIpProvisioningMode;
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("\n address: ").append(deviceAddress);
        sbuf.append("\n wps: ").append(wps);
        sbuf.append("\n groupOwnerIntent: ").append(groupOwnerIntent);
        sbuf.append("\n persist: ").append(netId);
        sbuf.append("\n networkName: ").append(networkName);
        sbuf.append("\n passphrase: ").append(
                TextUtils.isEmpty(passphrase) ? "<empty>" : "<non-empty>");
        sbuf.append("\n groupOwnerBand: ").append(groupOwnerBand);
        sbuf.append("\n groupClientIpProvisioningMode: ").append(mGroupClientIpProvisioningMode);
        sbuf.append("\n joinExistingGroup: ").append(mJoinExistingGroup);
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** copy constructor */
    public WifiP2pConfig(WifiP2pConfig source) {
        if (source != null) {
            deviceAddress = source.deviceAddress;
            wps = new WpsInfo(source.wps);
            groupOwnerIntent = source.groupOwnerIntent;
            netId = source.netId;
            networkName = source.networkName;
            passphrase = source.passphrase;
            groupOwnerBand = source.groupOwnerBand;
            mGroupClientIpProvisioningMode = source.mGroupClientIpProvisioningMode;
            mJoinExistingGroup = source.mJoinExistingGroup;
        }
    }

    /** Implement the Parcelable interface */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceAddress);
        dest.writeParcelable(wps, flags);
        dest.writeInt(groupOwnerIntent);
        dest.writeInt(netId);
        dest.writeString(networkName);
        dest.writeString(passphrase);
        dest.writeInt(groupOwnerBand);
        dest.writeInt(mGroupClientIpProvisioningMode);
        dest.writeBoolean(mJoinExistingGroup);
    }

    /** Implement the Parcelable interface */
    @NonNull
    public static final Creator<WifiP2pConfig> CREATOR =
        new Creator<WifiP2pConfig>() {
            public WifiP2pConfig createFromParcel(Parcel in) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = in.readString();
                config.wps = (WpsInfo) in.readParcelable(null);
                config.groupOwnerIntent = in.readInt();
                config.netId = in.readInt();
                config.networkName = in.readString();
                config.passphrase = in.readString();
                config.groupOwnerBand = in.readInt();
                config.mGroupClientIpProvisioningMode = in.readInt();
                config.mJoinExistingGroup = in.readBoolean();
                return config;
            }

            public WifiP2pConfig[] newArray(int size) {
                return new WifiP2pConfig[size];
            }
        };

    /**
     * Builder used to build {@link WifiP2pConfig} objects for
     * creating or joining a group.
     *
     * The WifiP2pConfig can be constructed for two use-cases:
     * <ul>
     * <li>SSID + Passphrase are known: use {@link #setNetworkName(String)} and
     *   {@link #setPassphrase(String)}.</li>
     * <li>SSID or Passphrase is unknown, in such a case the MAC address must be known and
     *   specified using {@link #setDeviceAddress(MacAddress)}.</li>
     * </ul>
     */
    public static final class Builder {

        private static final MacAddress MAC_ANY_ADDRESS =
                MacAddress.fromString("02:00:00:00:00:00");
        /**
         * Maximum number of bytes allowed for a SSID.
         */
        private static final int MAX_SSID_BYTES = 32;

        private MacAddress mDeviceAddress = MAC_ANY_ADDRESS;
        private String mNetworkName = "";
        private String mPassphrase = "";
        private int mGroupOperatingBand = GROUP_OWNER_BAND_AUTO;
        private int mGroupOperatingFrequency = GROUP_OWNER_BAND_AUTO;
        private int mNetId = WifiP2pGroup.NETWORK_ID_TEMPORARY;
        private int mGroupClientIpProvisioningMode = GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP;
        private boolean mJoinExistingGroup = false;

        /**
         * Specify the peer's MAC address. If not set, the device will
         * try to find a peer whose SSID matches the network name as
         * specified by {@link #setNetworkName(String)}. Specifying null will
         * reset the peer's MAC address to "02:00:00:00:00:00".
         * <p>
         *     Optional. "02:00:00:00:00:00" by default.
         *
         * <p> If the network name is not set, the peer's MAC address is mandatory.
         *
         * @param deviceAddress the peer's MAC address.
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setDeviceAddress(@Nullable MacAddress deviceAddress) {
            if (deviceAddress == null) {
                mDeviceAddress = MAC_ANY_ADDRESS;
            } else {
                mDeviceAddress = deviceAddress;
            }
            return this;
        }

        /**
         * Specify the network name, a.k.a. group name,
         * for creating or joining a group.
         * <p>
         * A network name shall begin with "DIRECT-xy". x and y are selected
         * from the following character set: upper case letters, lower case
         * letters and numbers. Any byte values allowed for an SSID according to
         * IEEE802.11-2012 [1] may be included after the string "DIRECT-xy"
         * (including none).
         * <p>
         *     Must be called - an empty network name or an network name
         *     not conforming to the P2P Group ID naming rule is not valid.
         *
         * @param networkName network name of a group.
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setNetworkName(@NonNull String networkName) {
            if (TextUtils.isEmpty(networkName)) {
                throw new IllegalArgumentException(
                        "network name must be non-empty.");
            }
            if (networkName.getBytes(StandardCharsets.UTF_8).length > MAX_SSID_BYTES) {
                throw new IllegalArgumentException(
                        "network name exceeds " + MAX_SSID_BYTES + " bytes.");
            }
            try {
                if (!networkName.matches("^DIRECT-[a-zA-Z0-9]{2}.*")) {
                    throw new IllegalArgumentException(
                            "network name must starts with the prefix DIRECT-xy.");
                }
            } catch (PatternSyntaxException e) {
                // can never happen (fixed pattern)
            }
            mNetworkName = networkName;
            return this;
        }

        /**
         * Specify the passphrase for creating or joining a group.
         * <p>
         * The passphrase must be an ASCII string whose length is between 8
         * and 63.
         * <p>
         *     Must be called - an empty passphrase is not valid.
         *
         * @param passphrase the passphrase of a group.
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setPassphrase(@NonNull String passphrase) {
            if (TextUtils.isEmpty(passphrase)) {
                throw new IllegalArgumentException(
                        "passphrase must be non-empty.");
            }
            if (passphrase.length() < 8 || passphrase.length() > 63) {
                throw new IllegalArgumentException(
                        "The length of a passphrase must be between 8 and 63.");
            }
            mPassphrase = passphrase;
            return this;
        }

        /**
         * Specify the band to use for creating the group or joining the group. The band should
         * be {@link #GROUP_OWNER_BAND_2GHZ}, {@link #GROUP_OWNER_BAND_5GHZ} or
         * {@link #GROUP_OWNER_BAND_AUTO}.
         * <p>
         * When creating a group as Group Owner using {@link
         * WifiP2pManager#createGroup(WifiP2pManager.Channel,
         * WifiP2pConfig, WifiP2pManager.ActionListener)},
         * specifying {@link #GROUP_OWNER_BAND_AUTO} allows the system to pick the operating
         * frequency from all supported bands.
         * Specifying {@link #GROUP_OWNER_BAND_2GHZ} or {@link #GROUP_OWNER_BAND_5GHZ}
         * only allows the system to pick the operating frequency in the specified band.
         * If the Group Owner cannot create a group in the specified band, the operation will fail.
         * <p>
         * When joining a group as Group Client using {@link
         * WifiP2pManager#connect(WifiP2pManager.Channel, WifiP2pConfig,
         * WifiP2pManager.ActionListener)},
         * specifying {@link #GROUP_OWNER_BAND_AUTO} allows the system to scan all supported
         * frequencies to find the desired group. Specifying {@link #GROUP_OWNER_BAND_2GHZ} or
         * {@link #GROUP_OWNER_BAND_5GHZ} only allows the system to scan the specified band.
         * <p>
         *     {@link #setGroupOperatingBand(int)} and {@link #setGroupOperatingFrequency(int)} are
         *     mutually exclusive. Setting operating band and frequency both is invalid.
         * <p>
         *     Optional. {@link #GROUP_OWNER_BAND_AUTO} by default.
         *
         * @param band the operating band of the group.
         *             This should be one of {@link #GROUP_OWNER_BAND_AUTO},
         *             {@link #GROUP_OWNER_BAND_2GHZ}, {@link #GROUP_OWNER_BAND_5GHZ}.
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setGroupOperatingBand(@GroupOperatingBandType int band) {
            switch (band) {
                case GROUP_OWNER_BAND_AUTO:
                case GROUP_OWNER_BAND_2GHZ:
                case GROUP_OWNER_BAND_5GHZ:
                    mGroupOperatingBand = band;
                    break;
                default:
                    throw new IllegalArgumentException(
                        "Invalid constant for the group operating band!");
            }
            return this;
        }

        /**
         * Specify the frequency, in MHz, to use for creating the group or joining the group.
         * <p>
         * When creating a group as Group Owner using {@link WifiP2pManager#createGroup(
         * WifiP2pManager.Channel, WifiP2pConfig, WifiP2pManager.ActionListener)},
         * specifying a frequency only allows the system to pick the specified frequency.
         * If the Group Owner cannot create a group at the specified frequency,
         * the operation will fail.
         * When not specifying a frequency, it allows the system to pick operating frequency
         * from all supported bands.
         * <p>
         * When joining a group as Group Client using {@link WifiP2pManager#connect(
         * WifiP2pManager.Channel, WifiP2pConfig, WifiP2pManager.ActionListener)},
         * specifying a frequency only allows the system to scan the specified frequency.
         * If the frequency is not supported or invalid, the operation will fail.
         * When not specifying a frequency, it allows the system to scan all supported
         * frequencies to find the desired group.
         * <p>
         *     {@link #setGroupOperatingBand(int)} and {@link #setGroupOperatingFrequency(int)} are
         *     mutually exclusive. Setting operating band and frequency both is invalid.
         * <p>
         *     Optional. 0 by default.
         *
         * @param frequency the operating frequency of the group.
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setGroupOperatingFrequency(int frequency) {
            if (frequency < 0) {
                throw new IllegalArgumentException(
                    "Invalid group operating frequency!");
            }
            mGroupOperatingFrequency = frequency;
            return this;
        }

        /**
         * Specify that the group configuration be persisted (i.e. saved).
         * By default the group configuration will not be saved.
         * <p>
         *     Optional. false by default.
         *
         * @param persistent is this group persistent group.
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder enablePersistentMode(boolean persistent) {
            if (persistent) {
                mNetId = WifiP2pGroup.NETWORK_ID_PERSISTENT;
            } else {
                mNetId = WifiP2pGroup.NETWORK_ID_TEMPORARY;
            }
            return this;
        }

        /**
         * Specify the IP provisioning mode when joining a group as a group client. The IP
         * provisioning mode should be {@link #GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP} or
         * {@link #GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL}.
         * <p>
         * When joining a group as group client using {@link
         * WifiP2pManager#connect(WifiP2pManager.Channel, WifiP2pConfig,
         * WifiP2pManager.ActionListener)},
         * specifying {@link #GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP} directs the system to
         * assign a IPv4 to the group client using DHCP. Specifying
         * {@link #GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL} directs the system to assign
         * a link-local IPv6 to the group client.
         * <p>
         *     Optional. {@link #GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP} by default.
         * <p>
         *
         * If {@link WifiP2pManager#isGroupOwnerIPv6LinkLocalAddressProvided()} is {@code true} and
         * {@link #GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL} is used then the system will
         * discover the group owner's IPv6 link-local address and broadcast it using the
         * {@link WifiP2pManager#EXTRA_WIFI_P2P_INFO} extra of the
         * {@link WifiP2pManager#WIFI_P2P_CONNECTION_CHANGED_ACTION} broadcast. Otherwise, if
         * {@link WifiP2pManager#isGroupOwnerIPv6LinkLocalAddressProvided()} is
         * {@code false} then the group owner's IPv6 link-local address is not discovered and it is
         * the responsibility of the caller to obtain it in some other way, e.g. via out-of-band
         * communication.
         *
         * @param groupClientIpProvisioningMode the IP provisioning mode of the group client.
         *             This should be one of {@link #GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP},
         *             {@link #GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL}.
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @see WifiP2pManager#isGroupOwnerIPv6LinkLocalAddressProvided()
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @NonNull
        public Builder setGroupClientIpProvisioningMode(
                @GroupClientIpProvisioningMode int groupClientIpProvisioningMode) {
            // Since group client IP provisioning modes use NetworkStack functionalities introduced
            // in T, hence we need at least T sdk for this to be supported.
            if (!SdkLevel.isAtLeastT()) {
                throw new UnsupportedOperationException(
                        "IPv6 link-local provisioning not supported");
            }
            switch (groupClientIpProvisioningMode) {
                case GROUP_CLIENT_IP_PROVISIONING_MODE_IPV4_DHCP:
                case GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL:
                    mGroupClientIpProvisioningMode = groupClientIpProvisioningMode;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Invalid constant for the group client IP provisioning mode!");
            }
            return this;
        }

        /**
         * Specify that the device wants to join an existing group as client.
         * Usually group owner sets the group owner capability bit in beacons/probe responses. But
         * there are deployed devices which don't set the group owner capability bit.
         * This API is for applications which can get the peer group owner capability via OOB
         * (out of band) mechanisms and forcefully trigger the join existing group logic.
         * <p>
         *     Optional. false by default.
         *
         * @param join true to forcefully trigger the join existing group logic, false to let
         *             device decide whether to join a group or form a group.
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setJoinExistingGroup(boolean join) {
            mJoinExistingGroup = join;
            return this;
        }

        /**
         * Build {@link WifiP2pConfig} given the current requests made on the builder.
         * @return {@link WifiP2pConfig} constructed based on builder method calls.
         */
        @NonNull
        public WifiP2pConfig build() {
            if ((TextUtils.isEmpty(mNetworkName) && !TextUtils.isEmpty(mPassphrase))
                    || (!TextUtils.isEmpty(mNetworkName) && TextUtils.isEmpty(mPassphrase))) {
                throw new IllegalStateException(
                        "network name and passphrase must be non-empty or empty both.");
            }
            if (TextUtils.isEmpty(mNetworkName)
                    && mDeviceAddress.equals(MAC_ANY_ADDRESS)) {
                throw new IllegalStateException(
                        "peer address must be set if network name and pasphrase are not set.");
            }

            if (mGroupOperatingFrequency > 0 && mGroupOperatingBand > 0) {
                throw new IllegalStateException(
                        "Preferred frequency and band are mutually exclusive.");
            }

            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = mDeviceAddress.toString();
            config.networkName = mNetworkName;
            config.passphrase = mPassphrase;
            config.groupOwnerBand = GROUP_OWNER_BAND_AUTO;
            if (mGroupOperatingFrequency > 0) {
                config.groupOwnerBand = mGroupOperatingFrequency;
            } else if (mGroupOperatingBand > 0) {
                config.groupOwnerBand = mGroupOperatingBand;
            }
            config.netId = mNetId;
            config.mGroupClientIpProvisioningMode = mGroupClientIpProvisioningMode;
            config.mJoinExistingGroup = mJoinExistingGroup;
            return config;
        }
    }
}

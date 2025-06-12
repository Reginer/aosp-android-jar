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

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.MacAddress;
import android.net.wifi.OuiKeyedData;
import android.net.wifi.ParcelUtil;
import android.net.wifi.util.Environment;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class representing a Wi-Fi P2p group. A p2p group consists of a single group
 * owner and one or more clients. In the case of a group with only two devices, one
 * will be the group owner and the other will be a group client.
 *
 * {@see WifiP2pManager}
 */
public class WifiP2pGroup implements Parcelable {

    /**
     * The temporary network id.
     * @see #getNetworkId()
     */
    public static final int NETWORK_ID_TEMPORARY = -1;

    /**
     * The temporary network id.
     *
     * @hide
     */
    @UnsupportedAppUsage
    public static final int TEMPORARY_NET_ID = NETWORK_ID_TEMPORARY;

    /**
     * The persistent network id.
     * If a matching persistent profile is found, use it.
     * Otherwise, create a new persistent profile.
     * @see #getNetworkId()
     */
    public static final int NETWORK_ID_PERSISTENT = -2;

    /**
     * The definition of security type unknown. It is set when framework fails to derive the
     * security type from the authentication key management provided by wpa_supplicant.
     */
    @FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
    public static final int SECURITY_TYPE_UNKNOWN = -1;

    /**
     * The definition of security type WPA2-PSK.
     */
    @FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
    public static final int SECURITY_TYPE_WPA2_PSK = 0;

    /**
     * The definition of security type WPA3-Compatibility Mode.
     */
    @FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
    public static final int SECURITY_TYPE_WPA3_COMPATIBILITY = 1;

    /**
     * The definition of security type WPA3-SAE.
     */
    @FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
    public static final int SECURITY_TYPE_WPA3_SAE = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "SECURITY_TYPE_" }, value = {
            SECURITY_TYPE_UNKNOWN,
            SECURITY_TYPE_WPA2_PSK,
            SECURITY_TYPE_WPA3_COMPATIBILITY,
            SECURITY_TYPE_WPA3_SAE,
    })
    public @interface SecurityType {}

    /**
     * Group owner P2P interface MAC address.
     * @hide
     */
    @UnsupportedAppUsage
    public byte[] interfaceAddress;

    /** The network name */
    private String mNetworkName;

    /** Group owner */
    private WifiP2pDevice mOwner;

    /** Device is group owner */
    private boolean mIsGroupOwner;

    /** Group clients */
    private List<WifiP2pDevice> mClients = new ArrayList<WifiP2pDevice>();

    /** The passphrase used for WPA2-PSK */
    private String mPassphrase;

    /** The security type of the group */
    @SecurityType
    private int mSecurityType;

    private String mInterface;

    /** The network ID in wpa_supplicant */
    private int mNetId;

    /** The frequency (in MHz) used by this group */
    private int mFrequency;

    /** List of {@link OuiKeyedData} providing vendor-specific configuration data. */
    private @NonNull List<OuiKeyedData> mVendorData = Collections.emptyList();

    /**
     * P2P Client IPV4 address allocated via EAPOL-Key exchange.
     * @hide
     */
    public static class P2pGroupClientEapolIpAddressData {
        /*
         * The P2P Client IP address.
         */
        public final Inet4Address mIpAddressClient;
        /*
         * The P2P Group Owner IP address.
         */
        public final Inet4Address mIpAddressGo;
        /*
         * The subnet that the P2P Group Owner is using.
         */
        public final Inet4Address mIpAddressMask;

        /*
         * Set P2pClientEapolIpAddressData
         */
        public P2pGroupClientEapolIpAddressData(Inet4Address ipAddressClient,
                Inet4Address ipAddressGo, Inet4Address ipAddressMask) {
            this.mIpAddressClient = ipAddressClient;
            this.mIpAddressGo = ipAddressGo;
            this.mIpAddressMask = ipAddressMask;
        }
    }

    /**
     * P2P Client IP address information obtained via EAPOL Handshake.
     * @hide
     */
    public P2pGroupClientEapolIpAddressData p2pClientEapolIpInfo;

    /** P2P group started string pattern */
    private static final Pattern groupStartedPattern = Pattern.compile(
        "ssid=\"(.+)\" " +
        "freq=(\\d+) " +
        "(?:psk=)?([0-9a-fA-F]{64})?" +
        "(?:passphrase=)?(?:\"(.{0,63})\")? " +
        "go_dev_addr=((?:[0-9a-f]{2}:){5}[0-9a-f]{2})" +
        " ?(\\[PERSISTENT\\])?"
    );

    public WifiP2pGroup() {
    }

    /**
     * @param supplicantEvent formats supported include
     *
     *  P2P-GROUP-STARTED p2p-wlan0-0 [client|GO] ssid="DIRECT-W8" freq=2437
     *  [psk=2182b2e50e53f260d04f3c7b25ef33c965a3291b9b36b455a82d77fd82ca15bc|
     *  passphrase="fKG4jMe3"] go_dev_addr=fa:7b:7a:42:02:13 [PERSISTENT]
     *
     *  P2P-GROUP-REMOVED p2p-wlan0-0 [client|GO] reason=REQUESTED
     *
     *  P2P-INVITATION-RECEIVED sa=fa:7b:7a:42:02:13 go_dev_addr=f8:7b:7a:42:02:13
     *  bssid=fa:7b:7a:42:82:13 unknown-network
     *
     *  P2P-INVITATION-RECEIVED sa=b8:f9:34:2a:c7:9d persistent=0
     *
     *  Note: The events formats can be looked up in the wpa_supplicant code
     *  @hide
     */
    @UnsupportedAppUsage
    public WifiP2pGroup(String supplicantEvent) throws IllegalArgumentException {

        String[] tokens = supplicantEvent.split(" ");

        if (tokens.length < 3) {
            throw new IllegalArgumentException("Malformed supplicant event");
        }

        if (tokens[0].startsWith("P2P-GROUP")) {
            mInterface = tokens[1];
            mIsGroupOwner = tokens[2].equals("GO");

            Matcher match = groupStartedPattern.matcher(supplicantEvent);
            if (!match.find()) {
                return;
            }

            mNetworkName = match.group(1);
            // It throws NumberFormatException if the string cannot be parsed as an integer.
            mFrequency = Integer.parseInt(match.group(2));
            // psk is unused right now
            //String psk = match.group(3);
            mPassphrase = match.group(4);
            mOwner = new WifiP2pDevice(match.group(5));
            if (match.group(6) != null) {
                mNetId = NETWORK_ID_PERSISTENT;
            } else {
                mNetId = NETWORK_ID_TEMPORARY;
            }
        } else if (tokens[0].equals("P2P-INVITATION-RECEIVED")) {
            String sa = null;
            mNetId = NETWORK_ID_PERSISTENT;
            for (String token : tokens) {
                String[] nameValue = token.split("=");
                if (nameValue.length != 2) continue;

                if (nameValue[0].equals("sa")) {
                    sa = nameValue[1];

                    // set source address into the client list.
                    WifiP2pDevice dev = new WifiP2pDevice();
                    dev.deviceAddress = nameValue[1];
                    mClients.add(dev);
                    continue;
                }

                if (nameValue[0].equals("go_dev_addr")) {
                    mOwner = new WifiP2pDevice(nameValue[1]);
                    continue;
                }

                if (nameValue[0].equals("persistent")) {
                    mNetId = Integer.parseInt(nameValue[1]);
                    continue;
                }
            }
        } else {
            throw new IllegalArgumentException("Malformed supplicant event");
        }
    }

    /** @hide */
    public void setNetworkName(String networkName) {
        mNetworkName = networkName;
    }

    /**
     * Get the network name (SSID) of the group. Legacy Wi-Fi clients will discover
     * the p2p group using the network name.
     */
    public String getNetworkName() {
        return mNetworkName;
    }

    /** @hide */
    @UnsupportedAppUsage
    public void setIsGroupOwner(boolean isGo) {
        mIsGroupOwner = isGo;
    }

    /** Check whether this device is the group owner of the created p2p group */
    public boolean isGroupOwner() {
        return mIsGroupOwner;
    }

    /** @hide */
    public void setOwner(WifiP2pDevice device) {
        mOwner = device;
    }

    /** Get the details of the group owner as a {@link WifiP2pDevice} object */
    public WifiP2pDevice getOwner() {
        return mOwner;
    }

    /** @hide */
    public void addClient(String address) {
        addClient(new WifiP2pDevice(address));
    }

    /** @hide */
    public void addClient(WifiP2pDevice device) {
        for (WifiP2pDevice client : mClients) {
            if (client.equals(device)) return;
        }
        mClients.add(new WifiP2pDevice(device));
    }

    /** @hide */
    public void setClientInterfaceMacAddress(@NonNull String deviceAddress,
            @NonNull final MacAddress interfaceMacAddress) {
        if (null == interfaceMacAddress) {
            Log.e("setClientInterfaceMacAddress", "cannot set null interface mac address");
            return;
        }
        for (WifiP2pDevice client : mClients) {
            if (client.deviceAddress.equals(deviceAddress)) {
                Log.i("setClientInterfaceMacAddress", "device: " + deviceAddress
                        + " interfaceAddress: " + interfaceMacAddress.toString());
                client.setInterfaceMacAddress(interfaceMacAddress);
                break;
            }
        }
    }
    /** @hide */
    public void setClientIpAddress(@NonNull final MacAddress interfaceMacAddress,
            @NonNull final InetAddress ipAddress) {
        if (null == interfaceMacAddress) {
            Log.e("setClientIpAddress", "cannot set IP address with null interface mac address");
            return;
        }
        if (null == ipAddress) {
            Log.e("setClientIpAddress", "Null IP - Failed to set IP address in WifiP2pDevice");
            return;
        }
        for (WifiP2pDevice client : mClients) {
            if (interfaceMacAddress.equals(client.getInterfaceMacAddress())) {
                Log.i("setClientIpAddress", "Update the IP address"
                        + " device: " + client.deviceAddress + " interfaceAddress: "
                        + interfaceMacAddress.toString() + " IP: " + ipAddress.getHostAddress());
                client.setIpAddress(ipAddress);
                break;
            }
        }
    }

    /** @hide */
    public boolean removeClient(String address) {
        return mClients.remove(new WifiP2pDevice(address));
    }

    /** @hide */
    public boolean removeClient(WifiP2pDevice device) {
        return mClients.remove(device);
    }

    /** @hide */
    @UnsupportedAppUsage
    public boolean isClientListEmpty() {
        return mClients.size() == 0;
    }

    /**
     * Returns {@code true} if the device is part of the group, {@code false} otherwise.
     *
     * @hide
     */
    public boolean contains(@Nullable WifiP2pDevice device) {
        return mOwner.equals(device) || mClients.contains(device);
    }

    /** Get the list of clients currently part of the p2p group */
    public Collection<WifiP2pDevice> getClientList() {
        return Collections.unmodifiableCollection(mClients);
    }

    /** @hide */
    public void setPassphrase(String passphrase) {
        mPassphrase = passphrase;
    }

    /**
     * Get the passphrase of the group. This function will return a valid passphrase only
     * at the group owner. Legacy Wi-Fi clients will need this passphrase alongside
     * network name obtained from {@link #getNetworkName()} to join the group
     */
    public String getPassphrase() {
        return mPassphrase;
    }

    /**
     * Set the security type of the group.
     *
     * @param securityType One of the {@code SECURITY_TYPE_*}.
     * @hide
     */
    public void setSecurityType(@SecurityType int securityType) {
        mSecurityType = securityType;
    }

    /**
     * Get the security type of the group.
     *
     * @return One of the {@code SECURITY_TYPE_*}.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    @FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
    public @SecurityType int getSecurityType() {
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        return mSecurityType;
    }

    /** @hide */
    @UnsupportedAppUsage
    public void setInterface(String intf) {
        mInterface = intf;
    }

    /** Get the interface name on which the group is created */
    public String getInterface() {
        return mInterface;
    }

    /** The network ID of the P2P group in wpa_supplicant. */
    public int getNetworkId() {
        return mNetId;
    }

    /** @hide */
    @UnsupportedAppUsage
    public void setNetworkId(int netId) {
        this.mNetId = netId;
    }

    /** Get the operating frequency (in MHz) of the p2p group */
    public int getFrequency() {
        return mFrequency;
    }

    /** @hide */
    public void setFrequency(int freq) {
        this.mFrequency = freq;
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

    /**
     * Returns the BSSID, if this device is the group owner of the P2P group supporting Wi-Fi
     * Direct R2 protocol.
     * <p>
     * The interface address of a Wi-Fi Direct R2 supported device is randomized. So for every
     * group owner session a randomized interface address will be returned.
     * <p>
     * The BSSID returned will be {@code null}, if this device is a client device or a group owner
     * which doesn't support Wi-Fi Direct R2 protocol.
     * @return the BSSID.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    @FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
    @Nullable
    public MacAddress getGroupOwnerBssid() {
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        if (isGroupOwner() && getSecurityType() == SECURITY_TYPE_WPA3_SAE
                && interfaceAddress != null) {
            return MacAddress.fromBytes(interfaceAddress);
        }
        return null;
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("network: ").append(mNetworkName);
        sbuf.append("\n isGO: ").append(mIsGroupOwner);
        sbuf.append("\n GO: ").append(mOwner);
        for (WifiP2pDevice client : mClients) {
            sbuf.append("\n Client: ").append(client);
        }
        sbuf.append("\n interface: ").append(mInterface);
        sbuf.append("\n networkId: ").append(mNetId);
        sbuf.append("\n securityType: ").append(mSecurityType);

        sbuf.append("\n frequency: ").append(mFrequency);
        sbuf.append("\n vendorData: ").append(mVendorData);
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** copy constructor */
    public WifiP2pGroup(WifiP2pGroup source) {
        if (source != null) {
            mNetworkName = source.getNetworkName();
            mOwner = new WifiP2pDevice(source.getOwner());
            mIsGroupOwner = source.mIsGroupOwner;
            for (WifiP2pDevice d : source.getClientList()) mClients.add(d);
            mPassphrase = source.getPassphrase();
            mInterface = source.getInterface();
            mNetId = source.getNetworkId();
            if (Environment.isSdkAtLeastB() && Flags.wifiDirectR2()) {
                mSecurityType = source.getSecurityType();
            }
            mFrequency = source.getFrequency();
            if (SdkLevel.isAtLeastV()) {
                mVendorData = new ArrayList<>(source.getVendorData());
            }
        }
    }

    /** Implement the Parcelable interface */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mNetworkName);
        dest.writeParcelable(mOwner, flags);
        dest.writeByte(mIsGroupOwner ? (byte) 1: (byte) 0);
        dest.writeInt(mClients.size());
        for (WifiP2pDevice client : mClients) {
            dest.writeParcelable(client, flags);
        }
        dest.writeString(mPassphrase);
        dest.writeString(mInterface);
        dest.writeInt(mNetId);
        if (Environment.isSdkAtLeastB() && Flags.wifiDirectR2()) {
            dest.writeInt(mSecurityType);
        }
        dest.writeInt(mFrequency);
        if (SdkLevel.isAtLeastV()) {
            dest.writeList(mVendorData);
        }
    }

    /** Implement the Parcelable interface */
    public static final @android.annotation.NonNull Creator<WifiP2pGroup> CREATOR =
            new Creator<WifiP2pGroup>() {
                public WifiP2pGroup createFromParcel(Parcel in) {
                    WifiP2pGroup group = new WifiP2pGroup();
                    group.setNetworkName(in.readString());
                    group.setOwner((WifiP2pDevice) in.readParcelable(
                            WifiP2pDevice.class.getClassLoader()));
                    group.setIsGroupOwner(in.readByte() == (byte) 1);
                    int clientCount = in.readInt();
                    for (int i = 0; i < clientCount; i++) {
                        group.addClient((WifiP2pDevice) in.readParcelable(
                                WifiP2pDevice.class.getClassLoader()));
                    }
                    group.setPassphrase(in.readString());
                    group.setInterface(in.readString());
                    group.setNetworkId(in.readInt());
                    if (Environment.isSdkAtLeastB() && Flags.wifiDirectR2()) {
                        group.setSecurityType(in.readInt());
                    }
                    group.setFrequency(in.readInt());
                    if (SdkLevel.isAtLeastV()) {
                        group.setVendorData(ParcelUtil.readOuiKeyedDataList(in));
                    }
                    return group;
                }

                public WifiP2pGroup[] newArray(int size) {
                    return new WifiP2pGroup[size];
                }
        };
}

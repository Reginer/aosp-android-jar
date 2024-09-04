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

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.app.compat.CompatChanges;
import android.compat.annotation.ChangeId;
import android.compat.annotation.EnabledAfter;
import android.net.MacAddress;
import android.net.wifi.util.HexEncoding;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.android.internal.util.Preconditions;
import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Configuration for a soft access point (a.k.a. Soft AP, SAP, Hotspot).
 *
 * <p>This is input for the framework provided by a client app, i.e. it exposes knobs to instruct
 * the framework how it should configure a hotspot.
 *
 * <p>System apps can use this to configure a tethered hotspot or local-only hotspot.
 *
 * <p>Instances of this class are immutable.
 */
public final class SoftApConfiguration implements Parcelable {

    private static final String TAG = "SoftApConfiguration";

    @VisibleForTesting
    static final int PSK_MIN_LEN = 8;

    @VisibleForTesting
    static final int PSK_MAX_LEN = 63;

    /**
     * 2GHz band.
     * @hide
     */
    @SystemApi
    public static final int BAND_2GHZ = 1 << 0;

    /**
     * 5GHz band.
     * @hide
     */
    @SystemApi
    public static final int BAND_5GHZ = 1 << 1;

    /**
     * 6GHz band.
     * @hide
     */
    @SystemApi
    public static final int BAND_6GHZ = 1 << 2;

    /**
     * 60GHz band.
     * @hide
     */
    @SystemApi
    public static final int BAND_60GHZ = 1 << 3;

    /**
     * Device is allowed to choose the optimal band (2GHz, 5GHz, 6GHz) based on device capability,
     * operating country code and current radio conditions.
     * @hide
     *
     * @deprecated This is no longer supported. The value is fixed at
     * (BAND_2GHZ | BAND_5GHZ | BAND_6GHZ) even if a new band is supported in the future, for
     * instance {@code BAND_60GHZ}. The bands are a bit mask - use any combination of
     * {@code BAND_}, for instance {@code BAND_2GHZ | BAND_5GHZ}.
     */
    @SystemApi
    public static final int BAND_ANY = BAND_2GHZ | BAND_5GHZ | BAND_6GHZ;

    /**
     * A default value used to configure shut down timeout setting to default value.
     * See {@link Builder#setShutdownTimeoutMillis(long)} or
     * {@link Builder#setBridgedModeOpportunisticShutdownTimeoutMillis(long)} for details.
     *
     * @hide
     */
    @SystemApi
    public static final long DEFAULT_TIMEOUT = -1;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, prefix = { "BAND_TYPE_" }, value = {
            BAND_2GHZ,
            BAND_5GHZ,
            BAND_6GHZ,
            BAND_60GHZ,
    })
    public @interface BandType {}

    /**
     * All of the supported band types.
     * @hide
     */
    public static int[] BAND_TYPES = {BAND_2GHZ, BAND_5GHZ, BAND_6GHZ, BAND_60GHZ};

    private static boolean isBandValid(@BandType int band) {
        int bandAny = BAND_2GHZ | BAND_5GHZ | BAND_6GHZ | BAND_60GHZ;
        return ((band != 0) && ((band & ~bandAny) == 0));
    }

    private static final int MIN_CH_2G_BAND = 1;
    private static final int MAX_CH_2G_BAND = 14;
    private static final int MIN_CH_5G_BAND = 34;
    private static final int MAX_CH_5G_BAND = 196;
    private static final int MIN_CH_6G_BAND = 1;
    private static final int MAX_CH_6G_BAND = 253;
    private static final int MIN_CH_60G_BAND = 1;
    private static final int MAX_CH_60G_BAND = 6;

    /**
     * Requires to configure MAC randomization setting to None when configuring BSSID.
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = Build.VERSION_CODES.S)
    private static final long FORCE_MUTUAL_EXCLUSIVE_BSSID_MAC_RAMDONIZATION_SETTING = 215656264L;

    /**
     * Removes zero support on
     * {@link android.net.wifi.SoftApConfiguration.Builder#setShutdownTimeoutMillis(long)}.
     *
     * @hide
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = Build.VERSION_CODES.S)
    public static final long REMOVE_ZERO_FOR_TIMEOUT_SETTING = 213289672L;

    private static boolean isChannelBandPairValid(int channel, @BandType int band) {
        switch (band) {
            case BAND_2GHZ:
                if (channel < MIN_CH_2G_BAND || channel >  MAX_CH_2G_BAND) {
                    return false;
                }
                break;

            case BAND_5GHZ:
                if (channel < MIN_CH_5G_BAND || channel >  MAX_CH_5G_BAND) {
                    return false;
                }
                break;

            case BAND_6GHZ:
                if (channel < MIN_CH_6G_BAND || channel >  MAX_CH_6G_BAND) {
                    return false;
                }
                break;

            case BAND_60GHZ:
                if (channel < MIN_CH_60G_BAND || channel >  MAX_CH_60G_BAND) {
                    return false;
                }
                break;

            default:
                return false;
        }
        return true;
    }

    /**
     * SSID for the AP, or null for a framework-determined SSID.
     */
    private final @Nullable WifiSsid mWifiSsid;

    /**
     * BSSID for the AP, or null to use a framework-determined BSSID.
     */
    private final @Nullable MacAddress mBssid;

    /**
     * Vendor elements for the AP, structured as dd+len+elements
     */
    private final @NonNull List<ScanResult.InformationElement> mVendorElements;

    /**
     * Pre-shared key for WPA2-PSK or WPA3-SAE-Transition or WPA3-SAE encryption which depends on
     * the security type.
     */
    private final @Nullable String mPassphrase;

    /**
     * This is a network that does not broadcast its SSID, so an
     * SSID-specific probe request must be used for scans.
     */
    private final boolean mHiddenSsid;

    /**
     * The operating channels of the dual APs.
     *
     * The SparseIntArray that consists the band and the channel of matching the band.
     */
    @NonNull
    private final SparseIntArray mChannels;

    /**
     * The set of allowed channels in 2.4GHz band to select from using ACS (Automatic Channel
     * Selection) algorithm.
     *
     * Requires the driver to support {@link SoftApCapability#SOFTAP_FEATURE_ACS_OFFLOAD}.
     * Otherwise, this set will be ignored.
     *
     * If the set is empty, then all channels in 2.4GHz band are allowed.
     */
    private final @NonNull Set<Integer> mAllowedAcsChannels2g;

    /**
     * The set of allowed channels in 5GHz band to select from using ACS (Automatic Channel
     * Selection) algorithm.
     *
     * Requires the driver to support {@link SoftApCapability#SOFTAP_FEATURE_ACS_OFFLOAD}.
     * Otherwise, this set will be ignored.
     *
     * If the set is empty, then all channels in 5GHz are allowed.
     */
    private final @NonNull Set<Integer> mAllowedAcsChannels5g;

    /**
     * The set of allowed channels in 6GHz band to select from using ACS (Automatic Channel
     * Selection) algorithm.
     *
     * Requires the driver to support {@link SoftApCapability#SOFTAP_FEATURE_ACS_OFFLOAD}.
     * Otherwise, this set will be ignored.
     *
     * If the set is empty, then all channels in 6GHz are allowed.
     */
    private final @NonNull Set<Integer> mAllowedAcsChannels6g;

    /**
     * The maximum channel bandwidth for SoftAp operation
     *
     * Default value is SoftApInfo#CHANNEL_WIDTH_AUTO which means the channel bandwidth
     * is to be selected by the chip based on device capabilities.
     * <p>
     *
     * Valid values: {@link SoftApInfo#CHANNEL_WIDTH_AUTO},
     * {@link SoftApInfo#CHANNEL_WIDTH_20MHZ}, {@link SoftApInfo#CHANNEL_WIDTH_40MHZ},
     * {@link SoftApInfo#CHANNEL_WIDTH_80MHZ}, {@link SoftApInfo#CHANNEL_WIDTH_160MHZ},
     * {@link SoftApInfo#CHANNEL_WIDTH_320MHZ}
     *
     */
    private final @WifiAnnotations.Bandwidth int mMaxChannelBandwidth;

    /**
     * The maximim allowed number of clients that can associate to the AP.
     */
    private final int mMaxNumberOfClients;

    /**
     * The operating security type of the AP.
     * One of the following security types:
     * {@link #SECURITY_TYPE_OPEN},
     * {@link #SECURITY_TYPE_WPA2_PSK},
     * {@link #SECURITY_TYPE_WPA3_SAE_TRANSITION},
     * {@link #SECURITY_TYPE_WPA3_SAE},
     * {@link #SECURITY_TYPE_WPA3_OWE_TRANSITION},
     * {@link #SECURITY_TYPE_WPA3_OWE}
     */
    private final @SecurityType int mSecurityType;

    /**
     * The flag to indicate client need to authorize by user
     * when client is connecting to AP.
     */
    private final boolean mClientControlByUser;

    /**
     * The list of blocked client that can't associate to the AP.
     */
    private final List<MacAddress> mBlockedClientList;

    /**
     * The list of allowed client that can associate to the AP.
     */
    private final List<MacAddress> mAllowedClientList;

    /**
     * Whether auto shutdown of soft AP is enabled or not.
     */
    private final boolean mAutoShutdownEnabled;

    /**
     * Delay in milliseconds before shutting down soft AP when
     * there are no connected devices.
     */
    private final long mShutdownTimeoutMillis;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"RANDOMIZATION_"}, value = {
            RANDOMIZATION_NONE,
            RANDOMIZATION_PERSISTENT,
            RANDOMIZATION_NON_PERSISTENT})
    public @interface MacRandomizationSetting {}

    /**
     * Use the factory MAC address as the BSSID of the AP.
     *
     * @hide
     */
    @SystemApi
    public static final int RANDOMIZATION_NONE = 0;

    /**
     * Generate a persistent randomized MAC address as the BSSID of the AP.
     * The MAC address is persisted per SSID - i.e. as long as the SSID of the AP doesn't change
     * then it will have a persistent MAC address (which is initially random and is not the factory
     * MAC address).
     *
     * @hide
     */
    @SystemApi
    public static final int RANDOMIZATION_PERSISTENT = 1;

    /**
     * Generate a randomized MAC address as the BSSID of the AP. The MAC address is not persisted
     * - it is re-generated every time the AP is re-enabled.
     * @hide
     */
    @SystemApi
    public static final int RANDOMIZATION_NON_PERSISTENT = 2;

    /**
     * Level of MAC randomization for the AP BSSID.
     */
    @MacRandomizationSetting
    private int mMacRandomizationSetting;


    /**
     * Whether opportunistic shutdown of an instance in bridged AP is enabled or not.
     */
    private boolean mBridgedModeOpportunisticShutdownEnabled;

    /**
     * Whether 802.11ax AP is enabled or not.
     */
    private boolean mIeee80211axEnabled;

    /**
     * Whether 802.11be AP is enabled or not.
     */
    private boolean mIeee80211beEnabled;

    /**
     * Whether the current configuration is configured by user or not.
     */
    private boolean mIsUserConfiguration;

    /**
     * Randomized MAC address to use with this configuration when MAC randomization setting
     * is {@link #RANDOMIZATION_PERSISTENT}.
     */
    private final @Nullable MacAddress mPersistentRandomizedMacAddress;

    /**
     * Delay in milliseconds before shutting down an instance in bridged AP.
     */
    private final long mBridgedModeOpportunisticShutdownTimeoutMillis;

    /** List of {@link OuiKeyedData} providing vendor-specific configuration data. */
    private @NonNull List<OuiKeyedData> mVendorData;

    /**
     * THe definition of security type OPEN.
     */
    public static final int SECURITY_TYPE_OPEN = 0;

    /**
     * The definition of security type WPA2-PSK.
     */
    public static final int SECURITY_TYPE_WPA2_PSK = 1;

    /**
     * The definition of security type WPA3-SAE Transition mode.
     */
    public static final int SECURITY_TYPE_WPA3_SAE_TRANSITION = 2;

    /**
     * The definition of security type WPA3-SAE.
     */
    public static final int SECURITY_TYPE_WPA3_SAE = 3;

    /**
     * The definition of security type WPA3-OWE Transition.
     */
    public static final int SECURITY_TYPE_WPA3_OWE_TRANSITION = 4;

    /**
     * The definition of security type WPA3-OWE.
     */
    public static final int SECURITY_TYPE_WPA3_OWE = 5;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "SECURITY_TYPE_" }, value = {
        SECURITY_TYPE_OPEN,
        SECURITY_TYPE_WPA2_PSK,
        SECURITY_TYPE_WPA3_SAE_TRANSITION,
        SECURITY_TYPE_WPA3_SAE,
        SECURITY_TYPE_WPA3_OWE_TRANSITION,
        SECURITY_TYPE_WPA3_OWE,
    })
    public @interface SecurityType {}

    /** Private constructor for Builder and Parcelable implementation. */
    private SoftApConfiguration(
            @Nullable WifiSsid ssid,
            @Nullable MacAddress bssid,
            @Nullable String passphrase,
            boolean hiddenSsid,
            @NonNull SparseIntArray channels,
            @SecurityType int securityType,
            int maxNumberOfClients,
            boolean shutdownTimeoutEnabled,
            long shutdownTimeoutMillis,
            boolean clientControlByUser,
            @NonNull List<MacAddress> blockedList,
            @NonNull List<MacAddress> allowedList,
            int macRandomizationSetting,
            boolean bridgedModeOpportunisticShutdownEnabled,
            boolean ieee80211axEnabled,
            boolean ieee80211beEnabled,
            boolean isUserConfiguration,
            long bridgedModeOpportunisticShutdownTimeoutMillis,
            @NonNull List<ScanResult.InformationElement> vendorElements,
            @Nullable MacAddress persistentRandomizedMacAddress,
            @NonNull Set<Integer> allowedAcsChannels24g,
            @NonNull Set<Integer> allowedAcsChannels5g,
            @NonNull Set<Integer> allowedAcsChannels6g,
            @WifiAnnotations.Bandwidth int maxChannelBandwidth,
            @Nullable List<OuiKeyedData> vendorData) {
        mWifiSsid = ssid;
        mBssid = bssid;
        mPassphrase = passphrase;
        mHiddenSsid = hiddenSsid;
        if (channels.size() != 0) {
            mChannels = channels.clone();
        } else {
            mChannels = new SparseIntArray(1);
            mChannels.put(BAND_2GHZ, 0);
        }
        mSecurityType = securityType;
        mMaxNumberOfClients = maxNumberOfClients;
        mAutoShutdownEnabled = shutdownTimeoutEnabled;
        mShutdownTimeoutMillis = shutdownTimeoutMillis;
        mClientControlByUser = clientControlByUser;
        mBlockedClientList = new ArrayList<>(blockedList);
        mAllowedClientList = new ArrayList<>(allowedList);
        mMacRandomizationSetting = macRandomizationSetting;
        mBridgedModeOpportunisticShutdownEnabled = bridgedModeOpportunisticShutdownEnabled;
        mIeee80211axEnabled = ieee80211axEnabled;
        mIeee80211beEnabled = ieee80211beEnabled;
        mIsUserConfiguration = isUserConfiguration;
        mBridgedModeOpportunisticShutdownTimeoutMillis =
                bridgedModeOpportunisticShutdownTimeoutMillis;
        mVendorElements = new ArrayList<>(vendorElements);
        mPersistentRandomizedMacAddress = persistentRandomizedMacAddress;
        mAllowedAcsChannels2g = new HashSet<>(allowedAcsChannels24g);
        mAllowedAcsChannels5g = new HashSet<>(allowedAcsChannels5g);
        mAllowedAcsChannels6g = new HashSet<>(allowedAcsChannels6g);
        mMaxChannelBandwidth = maxChannelBandwidth;
        mVendorData = new ArrayList<>(vendorData);
    }

    @Override
    public boolean equals(Object otherObj) {
        if (this == otherObj) {
            return true;
        }
        if (!(otherObj instanceof SoftApConfiguration)) {
            return false;
        }
        SoftApConfiguration other = (SoftApConfiguration) otherObj;
        return Objects.equals(mWifiSsid, other.mWifiSsid)
                && Objects.equals(mBssid, other.mBssid)
                && Objects.equals(mPassphrase, other.mPassphrase)
                && mHiddenSsid == other.mHiddenSsid
                && mChannels.toString().equals(other.mChannels.toString())
                && mSecurityType == other.mSecurityType
                && mMaxNumberOfClients == other.mMaxNumberOfClients
                && mAutoShutdownEnabled == other.mAutoShutdownEnabled
                && mShutdownTimeoutMillis == other.mShutdownTimeoutMillis
                && mClientControlByUser == other.mClientControlByUser
                && Objects.equals(mBlockedClientList, other.mBlockedClientList)
                && Objects.equals(mAllowedClientList, other.mAllowedClientList)
                && mMacRandomizationSetting == other.mMacRandomizationSetting
                && mBridgedModeOpportunisticShutdownEnabled
                        == other.mBridgedModeOpportunisticShutdownEnabled
                && mIeee80211axEnabled == other.mIeee80211axEnabled
                && mIeee80211beEnabled == other.mIeee80211beEnabled
                && mIsUserConfiguration == other.mIsUserConfiguration
                && mBridgedModeOpportunisticShutdownTimeoutMillis
                        == other.mBridgedModeOpportunisticShutdownTimeoutMillis
                && Objects.equals(mVendorElements, other.mVendorElements)
                && Objects.equals(
                        mPersistentRandomizedMacAddress, other.mPersistentRandomizedMacAddress)
                && Objects.equals(mAllowedAcsChannels2g, other.mAllowedAcsChannels2g)
                && Objects.equals(mAllowedAcsChannels5g, other.mAllowedAcsChannels5g)
                && Objects.equals(mAllowedAcsChannels6g, other.mAllowedAcsChannels6g)
                && mMaxChannelBandwidth == other.mMaxChannelBandwidth
                && Objects.equals(mVendorData, other.mVendorData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mWifiSsid,
                mBssid,
                mPassphrase,
                mHiddenSsid,
                mChannels.toString(),
                mSecurityType,
                mMaxNumberOfClients,
                mAutoShutdownEnabled,
                mShutdownTimeoutMillis,
                mClientControlByUser,
                mBlockedClientList,
                mAllowedClientList,
                mMacRandomizationSetting,
                mBridgedModeOpportunisticShutdownEnabled,
                mIeee80211axEnabled,
                mIeee80211beEnabled,
                mIsUserConfiguration,
                mBridgedModeOpportunisticShutdownTimeoutMillis,
                mVendorElements,
                mPersistentRandomizedMacAddress,
                mAllowedAcsChannels2g,
                mAllowedAcsChannels5g,
                mAllowedAcsChannels6g,
                mMaxChannelBandwidth,
                mVendorData);
    }

    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("ssid = ").append(mWifiSsid == null ? null : mWifiSsid.toString());
        if (mBssid != null) sbuf.append(" \n bssid = ").append(mBssid.toString());
        sbuf.append(" \n Passphrase = ").append(
                TextUtils.isEmpty(mPassphrase) ? "<empty>" : "<non-empty>");
        sbuf.append(" \n HiddenSsid = ").append(mHiddenSsid);
        sbuf.append(" \n Channels = ").append(mChannels);
        sbuf.append(" \n SecurityType = ").append(getSecurityType());
        sbuf.append(" \n MaxClient = ").append(mMaxNumberOfClients);
        sbuf.append(" \n AutoShutdownEnabled = ").append(mAutoShutdownEnabled);
        sbuf.append(" \n ShutdownTimeoutMillis = ").append(mShutdownTimeoutMillis);
        sbuf.append(" \n ClientControlByUser = ").append(mClientControlByUser);
        sbuf.append(" \n BlockedClientList = ").append(mBlockedClientList);
        sbuf.append(" \n AllowedClientList= ").append(mAllowedClientList);
        sbuf.append(" \n MacRandomizationSetting = ").append(mMacRandomizationSetting);
        sbuf.append(" \n BridgedModeInstanceOpportunisticEnabled = ")
                .append(mBridgedModeOpportunisticShutdownEnabled);
        sbuf.append(" \n BridgedModeOpportunisticShutdownTimeoutMillis = ")
                .append(mBridgedModeOpportunisticShutdownTimeoutMillis);
        sbuf.append(" \n Ieee80211axEnabled = ").append(mIeee80211axEnabled);
        sbuf.append(" \n Ieee80211beEnabled = ").append(mIeee80211beEnabled);
        sbuf.append(" \n isUserConfiguration = ").append(mIsUserConfiguration);
        sbuf.append(" \n vendorElements = ").append(mVendorElements);
        sbuf.append(" \n mPersistentRandomizedMacAddress = ")
                .append(mPersistentRandomizedMacAddress);
        sbuf.append(" \n mAllowedAcsChannels2g = ").append(mAllowedAcsChannels2g);
        sbuf.append(" \n mAllowedAcsChannels5g = ").append(mAllowedAcsChannels5g);
        sbuf.append(" \n mAllowedAcsChannels6g = ").append(mAllowedAcsChannels6g);
        sbuf.append(" \n mMaxChannelBandwidth = ").append(mMaxChannelBandwidth);
        sbuf.append(" \n mVendorData = ").append(mVendorData);
        return sbuf.toString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mWifiSsid, 0);
        dest.writeParcelable(mBssid, flags);
        dest.writeString(mPassphrase);
        dest.writeBoolean(mHiddenSsid);
        writeSparseIntArray(dest, mChannels);
        dest.writeInt(mSecurityType);
        dest.writeInt(mMaxNumberOfClients);
        dest.writeBoolean(mAutoShutdownEnabled);
        dest.writeLong(mShutdownTimeoutMillis);
        dest.writeBoolean(mClientControlByUser);
        dest.writeTypedList(mBlockedClientList);
        dest.writeTypedList(mAllowedClientList);
        dest.writeInt(mMacRandomizationSetting);
        dest.writeBoolean(mBridgedModeOpportunisticShutdownEnabled);
        dest.writeBoolean(mIeee80211axEnabled);
        dest.writeBoolean(mIeee80211beEnabled);
        dest.writeBoolean(mIsUserConfiguration);
        dest.writeLong(mBridgedModeOpportunisticShutdownTimeoutMillis);
        dest.writeTypedList(mVendorElements);
        dest.writeParcelable(mPersistentRandomizedMacAddress, flags);
        writeHashSetInt(dest, mAllowedAcsChannels2g);
        writeHashSetInt(dest, mAllowedAcsChannels5g);
        writeHashSetInt(dest, mAllowedAcsChannels6g);
        dest.writeInt(mMaxChannelBandwidth);
        dest.writeList(mVendorData);
    }

    /* Reference from frameworks/base/core/java/android/os/Parcel.java */
    private static void writeSparseIntArray(@NonNull Parcel dest,
            @Nullable SparseIntArray val) {
        if (val == null) {
            dest.writeInt(-1);
            return;
        }
        int n = val.size();
        dest.writeInt(n);
        int i = 0;
        while (i < n) {
            dest.writeInt(val.keyAt(i));
            dest.writeInt(val.valueAt(i));
            i++;
        }
    }

    /* Reference from frameworks/base/core/java/android/os/Parcel.java */
    @NonNull
    private static SparseIntArray readSparseIntArray(@NonNull Parcel in) {
        int n = in.readInt();
        if (n < 0) {
            return new SparseIntArray();
        }
        SparseIntArray sa = new SparseIntArray(n);
        while (n > 0) {
            int key = in.readInt();
            int value = in.readInt();
            sa.append(key, value);
            n--;
        }
        return sa;
    }

    /* Write HashSet<Integer> into Parcel */
    private static void writeHashSetInt(@NonNull Parcel dest, @NonNull Set<Integer> set) {
        if (set.isEmpty()) {
            dest.writeInt(-1);
            return;
        }

        dest.writeInt(set.size());
        for (int val : set) {
            dest.writeInt(val);
        }
    }

    /* Read HashSet<Integer> from Parcel */
    @NonNull
    private static Set<Integer> readHashSetInt(@NonNull Parcel in) {
        Set<Integer> set = new HashSet<>();
        int len = in.readInt();
        if (len < 0) {
            return set;
        }

        for (int i = 0; i < len; i++) {
            set.add(in.readInt());
        }
        return set;
    }

    /* Read List<OuiKeyedData> from Parcel */
    @NonNull
    private static List<OuiKeyedData> readOuiKeyedDataList(@NonNull Parcel in) {
        List<OuiKeyedData> dataList = new ArrayList<>();
        if (SdkLevel.isAtLeastT()) {
            in.readList(dataList, OuiKeyedData.class.getClassLoader(), OuiKeyedData.class);
        } else {
            in.readList(dataList, OuiKeyedData.class.getClassLoader());
        }
        return dataList;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<SoftApConfiguration> CREATOR =
            new Creator<SoftApConfiguration>() {
                @Override
                public SoftApConfiguration createFromParcel(Parcel in) {
                    return new SoftApConfiguration(
                            in.readParcelable(WifiSsid.class.getClassLoader()),
                            in.readParcelable(MacAddress.class.getClassLoader()),
                            in.readString(),
                            in.readBoolean(),
                            readSparseIntArray(in),
                            in.readInt(),
                            in.readInt(),
                            in.readBoolean(),
                            in.readLong(),
                            in.readBoolean(),
                            in.createTypedArrayList(MacAddress.CREATOR),
                            in.createTypedArrayList(MacAddress.CREATOR),
                            in.readInt(),
                            in.readBoolean(),
                            in.readBoolean(),
                            in.readBoolean(),
                            in.readBoolean(),
                            in.readLong(),
                            in.createTypedArrayList(ScanResult.InformationElement.CREATOR),
                            in.readParcelable(MacAddress.class.getClassLoader()),
                            readHashSetInt(in),
                            readHashSetInt(in),
                            readHashSetInt(in),
                            in.readInt(),
                            readOuiKeyedDataList(in));
                }

                @Override
                public SoftApConfiguration[] newArray(int size) {
                    return new SoftApConfiguration[size];
                }
            };

    /**
     * Return the UTF-8 String set to be the SSID for the AP. If the SSID cannot be decoded as
     * UTF-8, then this will return {@link WifiManager#UNKNOWN_SSID}.
     *
     * @deprecated Use {@link #getWifiSsid()} instead.
     */
    @Nullable
    @Deprecated
    public String getSsid() {
        if (mWifiSsid == null) {
            return null;
        }
        CharSequence utf8Text = mWifiSsid.getUtf8Text();
        return utf8Text != null ? utf8Text.toString() : WifiManager.UNKNOWN_SSID;
    }

    /**
     * Return WifiSsid set to be the SSID for the AP.
     */
    @Nullable
    public WifiSsid getWifiSsid() {
        return mWifiSsid;
    }

    /**
     * Return VendorElements for the AP.
     * @hide
     */
    @NonNull
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SystemApi
    public List<ScanResult.InformationElement> getVendorElements() {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException();
        }
        return getVendorElementsInternal();
    }

    /**
     * @see #getVendorElements()
     * @hide
     */
    public List<ScanResult.InformationElement> getVendorElementsInternal() {
        return new ArrayList<>(mVendorElements);
    }

    /**
     * Returns MAC address set to be BSSID for the AP.
     */
    @Nullable
    public MacAddress getBssid() {
        return mBssid;
    }

    /**
     * Returns String set to be passphrase for current AP.
     */
    @Nullable
    public String getPassphrase() {
        return mPassphrase;
    }

    /**
     * Returns Boolean set to be indicate hidden (true: doesn't broadcast its SSID) or
     * not (false: broadcasts its SSID) for the AP.
     */
    public boolean isHiddenSsid() {
        return mHiddenSsid;
    }

    /**
     * Returns band type set to be the band for the AP.
     *
     * One or combination of {@code BAND_}, for instance
     * {@link #BAND_2GHZ}, {@link #BAND_5GHZ}, or {@code BAND_2GHZ | BAND_5GHZ}.
     *
     * Note: Returns the lowest band when more than one band is set.
     * Use {@link #getChannels()} to get dual bands setting.
     *
     * See also {@link Builder#setBand(int)}.
     *
     * @deprecated This API is deprecated. Use {@link #getChannels()} instead.
     * @hide
     */
    @Deprecated
    @SystemApi
    public @BandType int getBand() {
        return mChannels.keyAt(0);
    }

    /**
     * Returns a sorted array in ascending order that consists of the configured band types
     * for the APs.
     *
     * The band type is one or combination of {@code BAND_}, for instance
     * {@link #BAND_2GHZ}, {@link #BAND_5GHZ}, or {@code BAND_2GHZ | BAND_5GHZ}.
     *
     * Note: return array may only include one band when current setting is single AP mode.
     * See also {@link Builder#setBands(int[])}.
     *
     * @hide
     */
    public @NonNull int[] getBands() {
        int[] bands = new int[mChannels.size()];
        for (int i = 0; i < bands.length; i++) {
            bands[i] = mChannels.keyAt(i);
        }
        return bands;
    }

    /**
     * Returns Integer set to be the channel for the AP.
     *
     * Note: Returns the channel which associated to the lowest band if more than one channel
     * is set. Use {@link Builder#getChannels()} to get dual channel setting.
     * See also {@link Builder#setChannel(int, int)}.
     *
     * @deprecated This API is deprecated. Use {@link #getChannels()} instead.
     * @hide
     */
    @Deprecated
    @SystemApi
    public int getChannel() {
        return mChannels.valueAt(0);
    }


    /**
     * Returns SparseIntArray (key: {@code BandType} , value: channel) that consists of
     * the configured bands and channels for the AP(s).
     *
     * The returned channel value is Wi-Fi channel numbering.
     * Reference the Wi-Fi channel numbering and the channelization in IEEE 802.11-2016
     * specifications, section 17.3.8.4.2, 17.3.8.4.3 and Table 15-6.
     *
     * Note: return array may only include one channel when current setting is single AP mode.
     * See also {@link Builder#setChannels(SparseIntArray)}.
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    public @NonNull SparseIntArray getChannels() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return mChannels.clone();
    }

    /**
     * Get security type params which depends on which security passphrase to set.
     *
     * @return One of:
     * {@link #SECURITY_TYPE_OPEN},
     * {@link #SECURITY_TYPE_WPA2_PSK},
     * {@link #SECURITY_TYPE_WPA3_SAE_TRANSITION},
     * {@link #SECURITY_TYPE_WPA3_SAE},
     * {@link #SECURITY_TYPE_WPA3_OWE_TRANSITION},
     * {@link #SECURITY_TYPE_WPA3_OWE}
     */
    public @SecurityType int getSecurityType() {
        return mSecurityType;
    }

    /**
     * Returns the maximum number of clients that can associate to the AP.
     * See also {@link Builder#setMaxNumberOfClients(int)}.
     *
     * @hide
     */
    @SystemApi
    public int getMaxNumberOfClients() {
        return mMaxNumberOfClients;
    }

    /**
     * Returns whether auto shutdown is enabled or not.
     * The Soft AP will shutdown when there are no devices associated to it for
     * the timeout duration. See also {@link Builder#setAutoShutdownEnabled(boolean)}.
     *
     * @hide
     */
    @SystemApi
    public boolean isAutoShutdownEnabled() {
        return mAutoShutdownEnabled;
    }

    /**
     * Returns the shutdown timeout in milliseconds.
     * The Soft AP will shutdown when there are no devices associated to it for
     * the timeout duration. See also {@link Builder#setShutdownTimeoutMillis(long)}.
     *
     * @hide
     */
    @SystemApi
    public long getShutdownTimeoutMillis() {
        if (!CompatChanges.isChangeEnabled(
                REMOVE_ZERO_FOR_TIMEOUT_SETTING) && mShutdownTimeoutMillis == DEFAULT_TIMEOUT) {
            // For legacy application, return 0 when setting is DEFAULT_TIMEOUT.
            return 0;
        }
        return mShutdownTimeoutMillis;
    }

    /**
     * Returns a flag indicating whether clients need to be pre-approved by the user.
     * (true: authorization required) or not (false: not required).
     * See also {@link Builder#setClientControlByUserEnabled(Boolean)}.
     *
     * @hide
     */
    @SystemApi
    public boolean isClientControlByUserEnabled() {
        return mClientControlByUser;
    }

    /**
     * Returns List of clients which aren't allowed to associate to the AP.
     *
     * Clients are configured using {@link Builder#setBlockedClientList(List)}
     *
     * @hide
     */
    @NonNull
    @SystemApi
    public List<MacAddress> getBlockedClientList() {
        return mBlockedClientList;
    }

    /**
     * List of clients which are allowed to associate to the AP.
     * Clients are configured using {@link Builder#setAllowedClientList(List)}
     *
     * @hide
     */
    @NonNull
    @SystemApi
    public List<MacAddress> getAllowedClientList() {
        return mAllowedClientList;
    }

    /**
     * Returns the level of MAC randomization for the AP BSSID.
     * See also {@link Builder#setMacRandomizationSetting(int)}.
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    @MacRandomizationSetting
    public int getMacRandomizationSetting() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return getMacRandomizationSettingInternal();
    }

    /**
     * @hide
     */
    @MacRandomizationSetting
    public int getMacRandomizationSettingInternal() {
        return mMacRandomizationSetting;
    }

    /**
     * Returns whether opportunistic shutdown of an instance in bridged AP is enabled or not.
     *
     * See also {@link Builder#setBridgedModeOpportunisticShutdownEnabled(boolean}}
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    public boolean isBridgedModeOpportunisticShutdownEnabled() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return isBridgedModeOpportunisticShutdownEnabledInternal();
    }

    /**
     * @see #isBridgedModeOpportunisticShutdownEnabled()
     * @hide
     */
    public boolean isBridgedModeOpportunisticShutdownEnabledInternal() {
        return mBridgedModeOpportunisticShutdownEnabled;
    }

    /**
     * @see #isIeee80211axEnabled()
     * @hide
     */
    public boolean isIeee80211axEnabledInternal() {
        return mIeee80211axEnabled;
    }

    /**
     * Returns whether or not 802.11ax is enabled on the SoftAP.
     * This is an indication that if the device support 802.11ax AP then to enable or disable
     * that feature. If the device does not support 802.11ax AP then this flag is ignored.
     * See also {@link Builder#setIeee80211axEnabled(boolean}}
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    public boolean isIeee80211axEnabled() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return isIeee80211axEnabledInternal();
    }

    /**
     * Returns whether or not the Soft AP is configured to enable 802.11be.
     * This is an indication that if the device support 802.11be AP then to enable or disable
     * that feature. If the device does not support 802.11be AP then this flag is ignored.
     * See also {@link Builder#setIeee80211beEnabled(boolean}}
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SystemApi
    public boolean isIeee80211beEnabled() {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException();
        }
        return mIeee80211beEnabled;
    }

    /**
     * Returns the allowed channels for ACS in a selected band.
     *
     * If an empty array is returned, then all channels in that band are allowed
     * The channels are configured using {@link Builder#setAllowedAcsChannels(int, int[])}
     *
     * @param band one of the following band types:
     * {@link #BAND_2GHZ}, {@link #BAND_5GHZ}, {@link #BAND_6GHZ}.
     *
     * @return array of the allowed channels for ACS in that band
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @NonNull
    @SystemApi
    public int[] getAllowedAcsChannels(@BandType int band) {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException();
        }
        switch(band) {
            case BAND_2GHZ:
                return mAllowedAcsChannels2g.stream().mapToInt(Integer::intValue).toArray();
            case BAND_5GHZ:
                return mAllowedAcsChannels5g.stream().mapToInt(Integer::intValue).toArray();
            case BAND_6GHZ:
                return mAllowedAcsChannels6g.stream().mapToInt(Integer::intValue).toArray();
            default:
                throw new IllegalArgumentException("getAllowedAcsChannels: Invalid band: " + band);
        }
    }

    /**
     * Returns configured maximum channel bandwidth for the SoftAp connection.
     *
     * If not configured, it will return {@link SoftApInfo#CHANNEL_WIDTH_AUTO}
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SystemApi
    public @WifiAnnotations.Bandwidth int getMaxChannelBandwidth() {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException();
        }
        return mMaxChannelBandwidth;
    }

    /**
     * Returns whether or not the {@link SoftApConfiguration} was configured by the user
     * (as opposed to the default system configuration).
     * <p>
     * The {@link SoftApConfiguration} is considered user edited once the
     * {@link WifiManager#setSoftApConfiguration(SoftApConfiguration)} is called
     * - whether or not that configuration is the same as the default system configuration!
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    public boolean isUserConfiguration() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return isUserConfigurationInternal();
    }

    /**
     * Returns the randomized MAC address to be used by this configuration.
     *
     * The Soft AP may be configured to use a persistent randomized MAC address with
     * {@link Builder#setMacRandomizationSetting(int)}. This method returns the persistent
     * randomized MAC address which will be used for the Soft AP controlled by this configuration.
     *
     * @hide
     */
    @SystemApi
    public @NonNull MacAddress getPersistentRandomizedMacAddress() {
        return mPersistentRandomizedMacAddress;
    }

    /**
     * @hide
     */
    public boolean isUserConfigurationInternal() {
        return mIsUserConfiguration;
    }

    /**
     * Returns the bridged mode opportunistic shutdown timeout in milliseconds.
     * An instance in bridged AP will shutdown when there is no device associated to it for
     * the timeout duration. See also
     * {@link Builder#setBridgedModeOpportunisticShutdownTimeoutMillis(long)}.
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SystemApi
    public long getBridgedModeOpportunisticShutdownTimeoutMillis() {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException();
        }
        return mBridgedModeOpportunisticShutdownTimeoutMillis;
    }


    /**
     * @hide
     */
    public long getBridgedModeOpportunisticShutdownTimeoutMillisInternal() {
        return mBridgedModeOpportunisticShutdownTimeoutMillis;
    }

    /**
     * Return the vendor-provided configuration data, if it exists. See also {@link
     * Builder#setVendorData(List)}
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
     * Returns a {@link WifiConfiguration} representation of this {@link SoftApConfiguration}.
     * Note that SoftApConfiguration may contain configuration which is cannot be represented
     * by the legacy WifiConfiguration, in such cases a null will be returned.
     *
     * To maintain legacy behavior, the SSID of the WifiConfiguration will be the UTF-8
     * representation of the SSID without double quotes, as opposed to the double-quoted UTF-8
     * format documented in {@link WifiConfiguration#SSID}. If the SSID cannot be decoded as UTF-8,
     * then the SSID of the WifiConfiguration will be {@link WifiManager#UNKNOWN_SSID}.
     *
     * <li> SoftAp band in {@link WifiConfiguration.apBand} only supports
     * 2GHz, 5GHz, 2GHz+5GHz bands, so conversion is limited to these bands. </li>
     *
     * <li> SoftAp security type in {@link WifiConfiguration.KeyMgmt} only supports
     * NONE, WPA2_PSK, so conversion is limited to these security type.</li>
     * @hide
     */
    @Nullable
    @SystemApi
    public WifiConfiguration toWifiConfiguration() {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        CharSequence utf8Text = mWifiSsid != null ? mWifiSsid.getUtf8Text() : null;
        wifiConfig.SSID = utf8Text != null ? utf8Text.toString() : WifiManager.UNKNOWN_SSID;
        wifiConfig.preSharedKey = mPassphrase;
        wifiConfig.hiddenSSID = mHiddenSsid;
        wifiConfig.apChannel = getChannel();
        switch (mSecurityType) {
            case SECURITY_TYPE_OPEN:
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            case SECURITY_TYPE_WPA2_PSK:
            case SECURITY_TYPE_WPA3_SAE_TRANSITION:
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA2_PSK);
                break;
            default:
                Log.e(TAG, "Convert fail, unsupported security type :" + mSecurityType);
                return null;
        }

        switch (getBand()) {
            case BAND_2GHZ:
                wifiConfig.apBand  = WifiConfiguration.AP_BAND_2GHZ;
                break;
            case BAND_5GHZ:
                wifiConfig.apBand  = WifiConfiguration.AP_BAND_5GHZ;
                break;
            case BAND_2GHZ | BAND_5GHZ:
                wifiConfig.apBand  = WifiConfiguration.AP_BAND_ANY;
                break;
            case BAND_ANY:
                wifiConfig.apBand  = WifiConfiguration.AP_BAND_ANY;
                break;
            default:
                Log.e(TAG, "Convert fail, unsupported band setting :" + getBand());
                return null;
        }
        return wifiConfig;
    }

    /**
     * Builds a {@link SoftApConfiguration}, which allows an app to configure various aspects of a
     * Soft AP.
     *
     * All fields are optional. By default, SSID and BSSID are automatically chosen by the
     * framework, and an open network is created.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private WifiSsid mWifiSsid;
        private MacAddress mBssid;
        private String mPassphrase;
        private boolean mHiddenSsid;
        private SparseIntArray mChannels;
        private int mMaxNumberOfClients;
        private int mSecurityType;
        private boolean mAutoShutdownEnabled;
        private long mShutdownTimeoutMillis;
        private boolean mClientControlByUser;
        private List<MacAddress> mBlockedClientList;
        private List<MacAddress> mAllowedClientList;
        private int mMacRandomizationSetting;
        private boolean mBridgedModeOpportunisticShutdownEnabled;
        private boolean mIeee80211axEnabled;
        private boolean mIeee80211beEnabled;
        private boolean mIsUserConfiguration;
        private long mBridgedModeOpportunisticShutdownTimeoutMillis;
        private List<ScanResult.InformationElement> mVendorElements;
        private MacAddress mPersistentRandomizedMacAddress;
        private Set<Integer> mAllowedAcsChannels2g;
        private Set<Integer> mAllowedAcsChannels5g;
        private Set<Integer> mAllowedAcsChannels6g;
        private @WifiAnnotations.Bandwidth int mMaxChannelBandwidth;
        private @Nullable List<OuiKeyedData> mVendorData;

        /**
         * Constructs a Builder with default values (see {@link Builder}).
         */
        public Builder() {
            mWifiSsid = null;
            mBssid = null;
            mPassphrase = null;
            mHiddenSsid = false;
            mChannels = new SparseIntArray(1);
            mChannels.put(BAND_2GHZ, 0);
            mMaxNumberOfClients = 0;
            mSecurityType = SECURITY_TYPE_OPEN;
            mAutoShutdownEnabled = true; // enabled by default.
            mShutdownTimeoutMillis = DEFAULT_TIMEOUT;
            mClientControlByUser = false;
            mBlockedClientList = new ArrayList<>();
            mAllowedClientList = new ArrayList<>();
            if (SdkLevel.isAtLeastT()) {
                mMacRandomizationSetting = RANDOMIZATION_NON_PERSISTENT;
            } else {
                mMacRandomizationSetting = RANDOMIZATION_PERSISTENT;
            }
            mBridgedModeOpportunisticShutdownEnabled = true;
            mIeee80211axEnabled = true;
            mIeee80211beEnabled = true;
            mIsUserConfiguration = true;
            mBridgedModeOpportunisticShutdownTimeoutMillis = DEFAULT_TIMEOUT;
            mVendorElements = new ArrayList<>();
            mPersistentRandomizedMacAddress = null;
            mAllowedAcsChannels2g = new HashSet<>();
            mAllowedAcsChannels5g = new HashSet<>();
            mAllowedAcsChannels6g = new HashSet<>();
            mMaxChannelBandwidth = SoftApInfo.CHANNEL_WIDTH_AUTO;
            mVendorData = new ArrayList<>();
        }

        /**
         * Constructs a Builder initialized from an existing {@link SoftApConfiguration} instance.
         */
        public Builder(@NonNull SoftApConfiguration other) {
            if (other == null) {
                Log.e(TAG, "Cannot provide a null SoftApConfiguration");
                return;
            }

            mWifiSsid = other.mWifiSsid;
            mBssid = other.mBssid;
            mPassphrase = other.mPassphrase;
            mHiddenSsid = other.mHiddenSsid;
            mChannels = other.mChannels.clone();
            mMaxNumberOfClients = other.mMaxNumberOfClients;
            mSecurityType = other.mSecurityType;
            mAutoShutdownEnabled = other.mAutoShutdownEnabled;
            mShutdownTimeoutMillis = other.mShutdownTimeoutMillis;
            mClientControlByUser = other.mClientControlByUser;
            mBlockedClientList = new ArrayList<>(other.mBlockedClientList);
            mAllowedClientList = new ArrayList<>(other.mAllowedClientList);
            mMacRandomizationSetting = other.mMacRandomizationSetting;
            mBridgedModeOpportunisticShutdownEnabled =
                    other.mBridgedModeOpportunisticShutdownEnabled;
            mIeee80211axEnabled = other.mIeee80211axEnabled;
            mIeee80211beEnabled = other.mIeee80211beEnabled;
            mIsUserConfiguration = other.mIsUserConfiguration;
            mBridgedModeOpportunisticShutdownTimeoutMillis =
                    other.mBridgedModeOpportunisticShutdownTimeoutMillis;
            mVendorElements = new ArrayList<>(other.mVendorElements);
            mPersistentRandomizedMacAddress = other.mPersistentRandomizedMacAddress;
            mAllowedAcsChannels2g = new HashSet<>(other.mAllowedAcsChannels2g);
            mAllowedAcsChannels5g = new HashSet<>(other.mAllowedAcsChannels5g);
            mAllowedAcsChannels6g = new HashSet<>(other.mAllowedAcsChannels6g);
            mMaxChannelBandwidth = other.mMaxChannelBandwidth;
            if (SdkLevel.isAtLeastS() && mBssid != null) {
                // Auto set correct MAC randomization setting for the legacy SoftApConfiguration
                // to avoid the exception happen when framework (system server) copy
                // SoftApConfiguration.
                mMacRandomizationSetting = RANDOMIZATION_NONE;
            }
            mVendorData = new ArrayList<>(other.mVendorData);
        }

        /**
         * Builds the {@link SoftApConfiguration}.
         *
         * @return A new {@link SoftApConfiguration}, as configured by previous method calls.
         */
        @NonNull
        public SoftApConfiguration build() {
            for (MacAddress client : mAllowedClientList) {
                if (mBlockedClientList.contains(client)) {
                    throw new IllegalArgumentException("A MacAddress exist in both client list");
                }
            }

            // mMacRandomizationSetting supported from S.
            if (SdkLevel.isAtLeastS() && CompatChanges.isChangeEnabled(
                    FORCE_MUTUAL_EXCLUSIVE_BSSID_MAC_RAMDONIZATION_SETTING)
                    && mBssid != null && mMacRandomizationSetting != RANDOMIZATION_NONE) {
                throw new IllegalArgumentException("A BSSID had configured but MAC randomization"
                        + " setting is not NONE");
            }

            if (!CompatChanges.isChangeEnabled(
                    REMOVE_ZERO_FOR_TIMEOUT_SETTING) && mShutdownTimeoutMillis == DEFAULT_TIMEOUT) {
                mShutdownTimeoutMillis = 0; // Use 0 for legacy app.
            }
            return new SoftApConfiguration(
                    mWifiSsid,
                    mBssid,
                    mPassphrase,
                    mHiddenSsid,
                    mChannels,
                    mSecurityType,
                    mMaxNumberOfClients,
                    mAutoShutdownEnabled,
                    mShutdownTimeoutMillis,
                    mClientControlByUser,
                    mBlockedClientList,
                    mAllowedClientList,
                    mMacRandomizationSetting,
                    mBridgedModeOpportunisticShutdownEnabled,
                    mIeee80211axEnabled,
                    mIeee80211beEnabled,
                    mIsUserConfiguration,
                    mBridgedModeOpportunisticShutdownTimeoutMillis,
                    mVendorElements,
                    mPersistentRandomizedMacAddress,
                    mAllowedAcsChannels2g,
                    mAllowedAcsChannels5g,
                    mAllowedAcsChannels6g,
                    mMaxChannelBandwidth,
                    mVendorData);
        }

        /**
         * Specifies a UTF-8 SSID for the AP.
         * <p>
         * Null SSID only support when configure a local-only hotspot.
         * <p>
         * <li>If not set, defaults to null.</li>
         *
         * @param ssid SSID of valid Unicode characters, or null to have the SSID automatically
         *             chosen by the framework.
         * @return Builder for chaining.
         * @throws IllegalArgumentException when the SSID is empty, not unicode, or if the byte
         *                                  representation is longer than 32 bytes.
         *
         * @deprecated Use {@link #setWifiSsid(WifiSsid)} instead.
         */
        @NonNull
        @Deprecated
        public Builder setSsid(@Nullable String ssid) {
            if (ssid == null) {
                mWifiSsid = null;
                return this;
            }

            Preconditions.checkStringNotEmpty(ssid);
            Preconditions.checkArgument(StandardCharsets.UTF_8.newEncoder().canEncode(ssid));
            mWifiSsid = WifiSsid.fromUtf8Text(ssid);
            return this;
        }

        /**
         * Specifies an SSID for the AP in the form of WifiSsid.
         * <p>
         * Null SSID only support when configure a local-only hotspot.
         * <p>
         * <li>If not set, defaults to null.</li>
         *
         * @param wifiSsid SSID, or null ot have the SSID automatically chosen by the framework.
         * @return Builder for chaining.
         */
        @NonNull
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public Builder setWifiSsid(@Nullable WifiSsid wifiSsid) {
            if (!SdkLevel.isAtLeastT()) {
                throw new UnsupportedOperationException();
            }
            mWifiSsid = wifiSsid;
            return this;
        }

        /**
         * Specify vendor-specific information elements for the (Soft) AP to transmit in its beacons
         * and probe responses. Method also validates the structure and throws
         * IllegalArgumentException in cases when ID of IE is not 0xDD (221) or incoming list
         * contain duplicate elements.
         *
         * @param vendorElements VendorElements
         * @return Builder for chaining.
         */
        @NonNull
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public Builder setVendorElements(
                @NonNull List<ScanResult.InformationElement> vendorElements) {
            if (!SdkLevel.isAtLeastT()) {
                throw new UnsupportedOperationException();
            }
            for (ScanResult.InformationElement e : vendorElements) {
                if (e.id != ScanResult.InformationElement.EID_VSA) {
                    throw new IllegalArgumentException("received InformationElement which is not "
                            + "related to VendorElements. VendorElement block should start with "
                            + HexEncoding.encodeToString(
                                    new byte[]{ (byte) ScanResult.InformationElement.EID_VSA }));
                }
            }
            final HashSet<ScanResult.InformationElement> set = new HashSet<>(vendorElements);
            if (set.size() < vendorElements.size()) {
                throw new IllegalArgumentException("vendor elements array contain duplicates. "
                        + "Please avoid passing duplicated and keep structure clean.");
            }
            mVendorElements = new ArrayList<>(vendorElements);
            return this;
        }

        /**
         * Specifies a BSSID for the AP.
         * <p>
         * <li>If not set, defaults to null.</li>
         *
         * When this method is called, the caller needs to configure MAC randomization settings to
         * {@link #RANDOMIZATION_NONE}. See {@link #setMacRandomizationSetting(int)} for details.
         *
         * If multiple bands are requested via {@link #setBands(int[])} or
         * {@link #setChannels(SparseIntArray)}, HAL will derive 2 MAC addresses since framework
         * only sends down 1 MAC address.
         *
         * An example (but different implementation may perform a different mapping):
         * <li>MAC address 1: copy value of MAC address,
         * and set byte 1 = (0xFF - BSSID[1])</li>
         * <li>MAC address 2: copy value of MAC address,
         * and set byte 2 = (0xFF - BSSID[2])</li>
         *
         * Example BSSID argument: e2:38:60:c4:0e:b7
         * Derived MAC address 1: e2:c7:60:c4:0e:b7
         * Derived MAC address 2: e2:38:9f:c4:0e:b7
         *
         * <p>
         * Use {@link WifiManager.SoftApCallback#onCapabilityChanged(SoftApCapability)} and
         * {@link SoftApCapability#areFeaturesSupported(long)}
         * with {@link SoftApCapability.SOFTAP_FEATURE_MAC_ADDRESS_CUSTOMIZATION} to determine
         * whether or not this feature is supported.
         *
         * @param bssid BSSID, or null to have the BSSID chosen by the framework. The caller is
         *              responsible for avoiding collisions.
         * @return Builder for chaining.
         * @throws IllegalArgumentException when the given BSSID is the all-zero
         *                                  , multicast or broadcast MAC address.
         */
        @NonNull
        public Builder setBssid(@Nullable MacAddress bssid) {
            if (bssid != null) {
                Preconditions.checkArgument(!bssid.equals(WifiManager.ALL_ZEROS_MAC_ADDRESS));
                if (bssid.getAddressType() != MacAddress.TYPE_UNICAST) {
                    throw new IllegalArgumentException("bssid doesn't support "
                            + "multicast or broadcast mac address");
                }
            }
            mBssid = bssid;
            return this;
        }

        /**
         * Specifies that this AP should use specific security type with the given ASCII passphrase.
         *
         * @param securityType One of the following security types:
         * {@link #SECURITY_TYPE_OPEN},
         * {@link #SECURITY_TYPE_WPA2_PSK},
         * {@link #SECURITY_TYPE_WPA3_SAE_TRANSITION},
         * {@link #SECURITY_TYPE_WPA3_SAE},
         * {@link #SECURITY_TYPE_WPA3_OWE_TRANSITION},
         * {@link #SECURITY_TYPE_WPA3_OWE}.
         * @param passphrase The passphrase to use for sepcific {@code securityType} configuration
         * or null with {@link #SECURITY_TYPE_OPEN}, {@link #SECURITY_TYPE_WPA3_OWE_TRANSITION},
         * and {@link #SECURITY_TYPE_WPA3_OWE}.
         *
         * @return Builder for chaining.
         * @throws IllegalArgumentException when the passphrase is non-null for
         *             - {@link #SECURITY_TYPE_OPEN}
         *             - {@link #SECURITY_TYPE_WPA3_OWE_TRANSITION}
         *             - {@link #SECURITY_TYPE_WPA3_OWE}
         * @throws IllegalArgumentException when the passphrase is empty for
         *             - {@link #SECURITY_TYPE_WPA2_PSK},
         *             - {@link #SECURITY_TYPE_WPA3_SAE_TRANSITION},
         *             - {@link #SECURITY_TYPE_WPA3_SAE},
         * @throws IllegalArgumentException before {@link android.os.Build.VERSION_CODES#TIRAMISU})
         *         when the passphrase is not between 8 and 63 bytes (inclusive) for
         *             - {@link #SECURITY_TYPE_WPA2_PSK}
         *             - {@link #SECURITY_TYPE_WPA3_SAE_TRANSITION}
         */
        @NonNull
        public Builder setPassphrase(@Nullable String passphrase, @SecurityType int securityType) {
            if (!SdkLevel.isAtLeastT()
                    && (securityType == SECURITY_TYPE_WPA3_OWE_TRANSITION
                            || securityType == SECURITY_TYPE_WPA3_OWE)) {
                throw new UnsupportedOperationException();
            }
            if (securityType == SECURITY_TYPE_OPEN
                    || securityType == SECURITY_TYPE_WPA3_OWE_TRANSITION
                    || securityType == SECURITY_TYPE_WPA3_OWE) {
                if (passphrase != null) {
                    throw new IllegalArgumentException(
                            "passphrase should be null when security type is open");
                }
            } else {
                Preconditions.checkStringNotEmpty(passphrase);
                if (!SdkLevel.isAtLeastT() && (securityType == SECURITY_TYPE_WPA2_PSK
                        || securityType == SECURITY_TYPE_WPA3_SAE_TRANSITION)) {
                    int passphraseByteLength = 0;
                    if (!TextUtils.isEmpty(passphrase)) {
                        passphraseByteLength = passphrase.getBytes(StandardCharsets.UTF_8).length;
                    }
                    if (passphraseByteLength < PSK_MIN_LEN || passphraseByteLength > PSK_MAX_LEN) {
                        throw new IllegalArgumentException(
                                "Passphrase length must be at least " + PSK_MIN_LEN
                                        + " and no more than " + PSK_MAX_LEN
                                        + " for WPA2_PSK and WPA3_SAE_TRANSITION Mode");
                    }
                }
            }
            mSecurityType = securityType;
            mPassphrase = passphrase;
            return this;
        }

        /**
         * Specifies whether the AP is hidden (doesn't broadcast its SSID) or
         * not (broadcasts its SSID).
         * <p>
         * <li>If not set, defaults to false (i.e not a hidden network).</li>
         *
         * @param hiddenSsid true for a hidden SSID, false otherwise.
         * @return Builder for chaining.
         */
        @NonNull
        public Builder setHiddenSsid(boolean hiddenSsid) {
            mHiddenSsid = hiddenSsid;
            return this;
        }

        /**
         * Specifies the band for the AP.
         * <p>
         * <li>If not set, defaults to {@link #BAND_2GHZ}.</li>
         *
         * @param band One or combination of the following band type:
         * {@link #BAND_2GHZ}, {@link #BAND_5GHZ}, {@link #BAND_6GHZ}.
         * @return Builder for chaining.
         * @throws IllegalArgumentException when an invalid band type is provided.
         */
        @NonNull
        public Builder setBand(@BandType int band) {
            if (!isBandValid(band)) {
                throw new IllegalArgumentException("Invalid band type: " + band);
            }
            mChannels = new SparseIntArray(1);
            mChannels.put(band, 0);
            return this;
        }

        /**
         * Specifies the bands for the APs.
         * If more than 1 band is set, this will bring up concurrent APs.
         * on the requested bands (if possible).
         * <p>
         *
         * Use {@link WifiManager#isBridgedApConcurrencySupported()} to determine
         * whether or not concurrent APs are supported.
         *
         * Requires the driver to support {@link SoftApCapability.SOFTAP_FEATURE_ACS_OFFLOAD}
         * when multiple bands are configured. Otherwise,
         * {@link WifiManager#startTetheredHotspot(SoftApConfiguration)} will report error code
         * {@link WifiManager#SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION}.
         *
         * Note: Only supports 2.4GHz + 5GHz bands. If any other band is set, will report error
         * {@link WifiManager#SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION}.
         *
         * @param bands Array of the {@link #BandType}.
         * @return Builder for chaining.
         * @throws IllegalArgumentException when more than 2 bands are set or an invalid band type
         *                                  is provided.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        @NonNull
        public Builder setBands(@NonNull int[] bands) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            if (bands.length == 0 || bands.length > 2) {
                throw new IllegalArgumentException("Unsupported number of bands("
                        + bands.length + ") configured");
            }
            SparseIntArray channels = new SparseIntArray(bands.length);
            for (int val : bands) {
                if (!isBandValid(val)) {
                    throw new IllegalArgumentException("Invalid band type: " + val);
                }
                channels.put(val, 0);
            }
            mChannels = channels;
            return this;
        }


        /**
         * Specifies the channel and associated band for the AP.
         *
         * The channel which AP resides on. Valid channels are country dependent.
         * The {@link SoftApCapability#getSupportedChannelList(int)} can be used to obtain
         * valid channels.
         *
         * <p>
         * If not set, the default for the channel is the special value 0 which has the
         * framework auto-select a valid channel from the band configured with
         * {@link #setBand(int)}.
         *
         * The channel auto selection will be offloaded to driver when
         * {@link SoftApCapability#areFeaturesSupported(long)}
         * with {@link SoftApCapability.SOFTAP_FEATURE_ACS_OFFLOAD}
         * return true. The driver will auto select the best channel (e.g. best performance)
         * based on environment interference. Check {@link SoftApCapability} for more detail.
         *
         * The API contains (band, channel) input since the 6GHz band uses the same channel
         * numbering scheme as is used in the 2.4GHz and 5GHz band. Therefore, both are needed to
         * uniquely identify individual channels.
         *
         * <p>
         * @param channel operating channel of the AP.
         * @param band containing this channel.
         * @return Builder for chaining.
         * @throws IllegalArgumentException when the invalid channel or band type is configured.
         */
        @NonNull
        public Builder setChannel(int channel, @BandType int band) {
            if (!isChannelBandPairValid(channel, band)) {
                throw new IllegalArgumentException("Invalid channel(" + channel
                        + ") & band (" + band + ") configured");
            }
            mChannels = new SparseIntArray(1);
            mChannels.put(band, channel);
            return this;
        }

        /**
         * Specifies the channels and associated bands for the APs.
         *
         * When more than 1 channel is set, this will bring up concurrent APs on the requested
         * channels and bands (if possible).
         *
         * Valid channels are country dependent.
         * The {@link SoftApCapability#getSupportedChannelList(int)} can be used to obtain
         * valid channels in each band.
         *
         * Use {@link WifiManager#isBridgedApConcurrencySupported()} to determine
         * whether or not concurrent APs are supported.
         *
         * <p>
         * If not set, the default for the channel is the special value 0 which has the framework
         * auto-select a valid channel from the band configured with {@link #setBands(int[])}.
         *
         * The channel auto selection will be offloaded to driver when
         * {@link SoftApCapability#areFeaturesSupported(long)}
         * with {@link SoftApCapability.SOFTAP_FEATURE_ACS_OFFLOAD}
         * returns true. The driver will auto select the best channel (e.g. best performance)
         * based on environment interference. Check {@link SoftApCapability} for more detail.
         *
         * Requires the driver to support {@link SoftApCapability.SOFTAP_FEATURE_ACS_OFFLOAD}
         * when multiple bands are configured without specified channel value (i.e. channel is
         * the special value 0). Otherwise,
         * {@link WifiManager#startTetheredHotspot(SoftApConfiguration)} will report error code
         * {@link WifiManager#SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION}.
         *
         * Note: Only supports 2.4GHz + 5GHz bands. If any other band is set, will report error
         * {@link WifiManager#SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION}.
         *
         * The API contains (band, channel) input since the 6GHz band uses the same channel
         * numbering scheme as is used in the 2.4GHz and 5GHz band. Therefore, both are needed to
         * uniquely identify individual channels.
         *
         * Reference the Wi-Fi channel numbering and the channelization in IEEE 802.11-2016
         * specifications, section 17.3.8.4.2, 17.3.8.4.3 and Table 15-6.
         *
         * <p>
         * @param channels SparseIntArray (key: {@code #BandType} , value: channel) consists of
         *                 {@code BAND_} and corresponding channel.
         * @return Builder for chaining.
         * @throws IllegalArgumentException when more than 2 channels are set or the invalid
         *                                  channel or band type is configured.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        @NonNull
        public Builder setChannels(@NonNull SparseIntArray channels) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            if (channels.size() == 0 || channels.size() > 2) {
                throw new IllegalArgumentException("Unsupported number of channels("
                        + channels.size() + ") configured");
            }
            for (int i = 0; i < channels.size(); i++) {
                int channel = channels.valueAt(i);
                int band = channels.keyAt(i);
                if (channel == 0) {
                    if (!isBandValid(band)) {
                        throw new IllegalArgumentException("Invalid band type: " + band);
                    }
                } else {
                    if (!isChannelBandPairValid(channel, band)) {
                        throw new IllegalArgumentException("Invalid channel(" + channel
                                + ") & band (" + band + ") configured");
                    }
                }
            }
            mChannels = channels.clone();
            return this;
        }


        /**
         * Specifies the maximum number of clients that can associate to the AP.
         *
         * The maximum number of clients (STAs) which can associate to the AP.
         * The AP will reject association from any clients above this number.
         * Specify a value of 0 to have the framework automatically use the maximum number
         * which the device can support (based on hardware and carrier constraints).
         * <p>
         * Use {@link WifiManager.SoftApCallback#onCapabilityChanged(SoftApCapability)} and
         * {@link SoftApCapability#getMaxSupportedClients} to get the maximum number of clients
         * which the device supports (based on hardware and carrier constraints).
         *
         * <p>
         * <li>If not set, defaults to 0.</li>
         *
         * This method requires HAL support. If the method is used to set a
         * non-zero {@code maxNumberOfClients} value then
         * {@link WifiManager#startTetheredHotspot(SoftApConfiguration)} will report error code
         * {@link WifiManager#SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION}.
         *
         * <p>
         * Use {@link WifiManager.SoftApCallback#onCapabilityChanged(SoftApCapability)} and
         * {@link SoftApCapability#areFeaturesSupported(long)}
         * with {@link SoftApCapability.SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT} to determine whether
         * or not this feature is supported.
         *
         * @param maxNumberOfClients maximum client number of the AP.
         * @return Builder for chaining.
         */
        @NonNull
        public Builder setMaxNumberOfClients(@IntRange(from = 0) int maxNumberOfClients) {
            if (maxNumberOfClients < 0) {
                throw new IllegalArgumentException("maxNumberOfClients should be not negative");
            }
            mMaxNumberOfClients = maxNumberOfClients;
            return this;
        }

        /**
         * Specifies whether auto shutdown is enabled or not.
         * The Soft AP will shut down when there are no devices connected to it for
         * the timeout duration.
         *
         * <p>
         * <li>If not set, defaults to true</li>
         *
         * @param enable true to enable, false to disable.
         * @return Builder for chaining.
         *
         * @see #setShutdownTimeoutMillis(long)
         */
        @NonNull
        public Builder setAutoShutdownEnabled(boolean enable) {
            mAutoShutdownEnabled = enable;
            return this;
        }

        /**
         * Specifies the shutdown timeout in milliseconds.
         * The Soft AP will shut down when there are no devices connected to it for
         * the timeout duration.
         *
         * Specify a value of {@link #DEFAULT_TIMEOUT} to have the framework automatically use
         * default timeout setting which defined in
         * {@link R.integer.config_wifi_framework_soft_ap_timeout_delay}
         *
         * <p>
         * <li>If not set, defaults to {@link #DEFAULT_TIMEOUT}</li>
         * <li>The shut down timeout will apply when {@link #setAutoShutdownEnabled(boolean)} is
         * set to true</li>
         *
         * @param timeoutMillis milliseconds of the timeout delay. Any value less than 1 is invalid
         *                      except {@link #DEFAULT_TIMEOUT}.
         * @return Builder for chaining.
         *
         * @see #setAutoShutdownEnabled(boolean)
         */
        @NonNull
        public Builder setShutdownTimeoutMillis(@IntRange(from = -1) long timeoutMillis) {
            if (CompatChanges.isChangeEnabled(
                    REMOVE_ZERO_FOR_TIMEOUT_SETTING) && timeoutMillis < 1) {
                if (timeoutMillis != DEFAULT_TIMEOUT) {
                    throw new IllegalArgumentException("Invalid timeout value: " + timeoutMillis);
                }
            } else if (timeoutMillis < 0) {
                throw new IllegalArgumentException("Invalid timeout value from legacy app: "
                        + timeoutMillis);
            }
            mShutdownTimeoutMillis = timeoutMillis;
            return this;
        }

        /**
         * Configure the Soft AP to require manual user control of client association.
         * If disabled (the default) then any client which isn't in the blocked list
         * {@link #getBlockedClientList()} can associate to this Soft AP using the
         * correct credentials until the Soft AP capacity is reached (capacity is hardware, carrier,
         * or user limited - using {@link #setMaxNumberOfClients(int)}).
         *
         * If manual user control is enabled then clients will be accepted, rejected, or require
         * a user approval based on the configuration provided by
         * {@link #setBlockedClientList(List)} and {@link #setAllowedClientList(List)}.
         *
         * <p>
         * This method requires HAL support. HAL support can be determined using
         * {@link WifiManager.SoftApCallback#onCapabilityChanged(SoftApCapability)} and
         * {@link SoftApCapability#areFeaturesSupported(long)}
         * with {@link SoftApCapability.SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT}
         *
         * <p>
         * If the method is called on a device without HAL support then starting the soft AP
         * using {@link WifiManager#startTetheredHotspot(SoftApConfiguration)} will fail with
         * {@link WifiManager#SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION}.
         *
         * <p>
         * <li>If not set, defaults to false (i.e The authoriztion is not required).</li>
         *
         * @param enabled true for enabling the control by user, false otherwise.
         * @return Builder for chaining.
         */
        @NonNull
        public Builder setClientControlByUserEnabled(boolean enabled) {
            mClientControlByUser = enabled;
            return this;
        }

        /**
         * Configures the set of channel numbers in the specified band that are allowed
         * to be selected by the Automatic Channel Selection (ACS) algorithm.
         * <p>
         *
         * Requires the driver to support {@link SoftApCapability#SOFTAP_FEATURE_ACS_OFFLOAD}.
         * Otherwise, these sets will be ignored.
         * <p>
         *
         * @param band one of the following band types:
         * {@link #BAND_2GHZ}, {@link #BAND_5GHZ}, {@link #BAND_6GHZ}.
         *
         * @param channels that are allowed to be used by ACS algorithm in this band. If it is
         * configured to an empty array or not configured, then all channels within that band
         * will be allowed.
         * <p>
         *
         * @return Builder for chaining.
         */
        @NonNull
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public Builder setAllowedAcsChannels(@BandType int band, @NonNull int[] channels) {
            if (!SdkLevel.isAtLeastT()) {
                throw new UnsupportedOperationException();
            }

            if (channels == null) {
                throw new IllegalArgumentException(
                        "Passing a null object to setAllowedAcsChannels");
            }

            if ((band != BAND_2GHZ) && (band != BAND_5GHZ) && (band != BAND_6GHZ)) {
                throw new IllegalArgumentException(
                        "Passing an invalid band to setAllowedAcsChannels");
            }

            for (int channel : channels) {
                if (!isChannelBandPairValid(channel, band)) {
                    throw new IllegalArgumentException(
                            "Invalid channel to setAllowedAcsChannels: band: " + band
                            + "channel: " + channel);
                }
            }

            HashSet<Integer> set = IntStream.of(channels).boxed()
                    .collect(Collectors.toCollection(HashSet::new));
            switch(band) {
                case BAND_2GHZ:
                    mAllowedAcsChannels2g = set;
                    break;
                case BAND_5GHZ:
                    mAllowedAcsChannels5g = set;
                    break;
                case BAND_6GHZ:
                    mAllowedAcsChannels6g = set;
                    break;
            }

            return this;
        }

        /**
         * Sets maximum channel bandwidth for the SoftAp Connection
         *
         * If not set, the SoftAp connection will seek the maximum channel bandwidth achievable on
         * the device. However, in some cases the caller will need to put a cap on the channel
         * bandwidth through this API.
         *
         * @param maxChannelBandwidth one of {@link SoftApInfo#CHANNEL_WIDTH_AUTO},
         * {@link SoftApInfo#CHANNEL_WIDTH_20MHZ}, {@link SoftApInfo#CHANNEL_WIDTH_40MHZ},
         * {@link SoftApInfo#CHANNEL_WIDTH_80MHZ}, {@link SoftApInfo#CHANNEL_WIDTH_160MHZ},
         * or {@link SoftApInfo#CHANNEL_WIDTH_320MHZ}
         *
         * @return builder for chaining
         */
        @NonNull
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public Builder setMaxChannelBandwidth(@WifiAnnotations.Bandwidth int maxChannelBandwidth) {
            if (!SdkLevel.isAtLeastT()) {
                throw new UnsupportedOperationException();
            }

            switch (maxChannelBandwidth) {
                case SoftApInfo.CHANNEL_WIDTH_AUTO:
                case SoftApInfo.CHANNEL_WIDTH_20MHZ:
                case SoftApInfo.CHANNEL_WIDTH_40MHZ:
                case SoftApInfo.CHANNEL_WIDTH_80MHZ:
                case SoftApInfo.CHANNEL_WIDTH_160MHZ:
                case SoftApInfo.CHANNEL_WIDTH_320MHZ:
                    mMaxChannelBandwidth = maxChannelBandwidth;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Invalid channel bandwidth value("
                            + maxChannelBandwidth + ")  configured");
            }
            return this;
        }

        /**
         * This method together with {@link setClientControlByUserEnabled(boolean)} control client
         * connections to the AP. If client control by user is disabled using the above method then
         * this API has no effect and clients are allowed to associate to the AP (within limit of
         * max number of clients).
         *
         * If client control by user is enabled then this API configures the list of clients
         * which are explicitly allowed. These are auto-accepted.
         *
         * All other clients which attempt to associate, whose MAC addresses are on neither list,
         * are:
         * <ul>
         * <li>Rejected</li>
         * <li>A callback {@link WifiManager.SoftApCallback#onBlockedClientConnecting(WifiClient)}
         * is issued (which allows the user to add them to the allowed client list if desired).<li>
         * </ul>
         *
         * @param allowedClientList list of clients which are allowed to associate to the AP
         *                          without user pre-approval.
         * @return Builder for chaining.
         */
        @NonNull
        public Builder setAllowedClientList(@NonNull List<MacAddress> allowedClientList) {
            mAllowedClientList = new ArrayList<>(allowedClientList);
            return this;
        }

        /**
         * This API configures the list of clients which are blocked and cannot associate
         * to the Soft AP.
         *
         * <p>
         * This method requires HAL support. HAL support can be determined using
         * {@link WifiManager.SoftApCallback#onCapabilityChanged(SoftApCapability)} and
         * {@link SoftApCapability#areFeaturesSupported(long)}
         * with {@link SoftApCapability.SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT}
         *
         * <p>
         * If the method is called on a device without HAL support then starting the soft AP
         * using {@link WifiManager#startTetheredHotspot(SoftApConfiguration)} will fail with
         * {@link WifiManager#SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION}.
         *
         * @param blockedClientList list of clients which are not allowed to associate to the AP.
         * @return Builder for chaining.
         */
        @NonNull
        public Builder setBlockedClientList(@NonNull List<MacAddress> blockedClientList) {
            mBlockedClientList = new ArrayList<>(blockedClientList);
            return this;
        }

        /**
         * Specifies the level of MAC randomization for the AP BSSID.
         * The Soft AP BSSID will be randomized only if the BSSID isn't set
         * {@link #setBssid(MacAddress)} and this method is either uncalled
         * or called with {@link #RANDOMIZATION_PERSISTENT} or
         * {@link #RANDOMIZATION_NON_PERSISTENT}. When this method is called with
         * {@link #RANDOMIZATION_PERSISTENT} or {@link #RANDOMIZATION_NON_PERSISTENT}, the caller
         * the caller must not call {@link #setBssid(MacAddress)}.
         *
         * <p>
         * <li>If not set, defaults to {@link #RANDOMIZATION_NON_PERSISTENT}</li>
         *
         * <p>
         * Requires HAL support when set to {@link #RANDOMIZATION_PERSISTENT} or
         * {@link #RANDOMIZATION_NON_PERSISTENT}.
         * Use {@link WifiManager.SoftApCallback#onCapabilityChanged(SoftApCapability)} and
         * {@link SoftApCapability#areFeaturesSupported(long)}
         * with {@link SoftApCapability.SOFTAP_FEATURE_MAC_ADDRESS_CUSTOMIZATION} to determine
         * whether or not this feature is supported.
         *
         * @param macRandomizationSetting One of the following setting:
         * {@link #RANDOMIZATION_NONE}, {@link #RANDOMIZATION_PERSISTENT} or
         * {@link #RANDOMIZATION_NON_PERSISTENT}.
         * @return Builder for chaining.
         *
         * @see #setBssid(MacAddress)
         */
        @RequiresApi(Build.VERSION_CODES.S)
        @NonNull
        public Builder setMacRandomizationSetting(
                @MacRandomizationSetting int macRandomizationSetting) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            mMacRandomizationSetting = macRandomizationSetting;
            return this;
        }


        /**
         * Specifies whether or not opportunistic shut down of an AP instance in bridged mode
         * is enabled.
         *
         * <p>
         * If enabled, the framework will shutdown one of the AP instances if it is idle for
         * the timeout duration - meaning there are no devices connected to it.
         * If both AP instances are idle for the timeout duration then the framework will
         * shut down the AP instance operating on the higher frequency. For instance,
         * if the AP instances operate at 2.4GHz and 5GHz and are both idle for the
         * timeout duration then the 5GHz AP instance will be shut down.
         * <p>
         *
         * Note: the opportunistic timeout only applies to one AP instance of the bridge AP.
         * If one of the AP instances has already been disabled for any reason, including due to
         * an opportunistic timeout or hardware issues or coexistence issues,
         * then the opportunistic timeout is no longer active.
         *
         * <p>
         * The shutdown timer specified by {@link #setShutdownTimeoutMillis(long)} controls the
         * overall shutdown of the bridged AP and is still in use independently of the opportunistic
         * timer controlled by this AP.
         *
         * <p>
         * <li>If not set, defaults to true</li>
         *
         * @param enable true to enable, false to disable.
         * @return Builder for chaining.
         *
         */
        @RequiresApi(Build.VERSION_CODES.S)
        @NonNull
        public Builder setBridgedModeOpportunisticShutdownEnabled(boolean enable) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            mBridgedModeOpportunisticShutdownEnabled = enable;
            return this;
        }

        /**
         * Specifies whether or not to enable 802.11ax on the Soft AP.
         *
         * <p>
         * Note: Only relevant when the device supports 802.11ax on the Soft AP.
         * If enabled on devices that do not support 802.11ax then ignored.
         * Use {@link WifiManager.SoftApCallback#onCapabilityChanged(SoftApCapability)} and
         * {@link SoftApCapability#areFeaturesSupported(long)}
         * with {@link SoftApCapability.SOFTAP_FEATURE_IEEE80211_AX} to determine
         * whether or not 802.11ax is supported on the Soft AP.
         * <p>
         * <li>If not set, defaults to true - which will be ignored on devices
         * which do not support 802.11ax</li>
         *
         * @param enable true to enable, false to disable.
         * @return Builder for chaining.
         *
         */
        @RequiresApi(Build.VERSION_CODES.S)
        @NonNull
        public Builder setIeee80211axEnabled(boolean enable) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            mIeee80211axEnabled = enable;
            return this;
        }

        /**
         * Specifies whether or not to enable 802.11be on the Soft AP.
         *
         * <p>
         * Note: Only relevant when the device supports 802.11be on the Soft AP.
         * If enabled on devices that do not support 802.11be then ignored.
         * Use {@link WifiManager.SoftApCallback#onCapabilityChanged(SoftApCapability)} and
         * {@link SoftApCapability#areFeaturesSupported(long)}
         * with {@link SoftApCapability.SOFTAP_FEATURE_IEEE80211_BE} to determine
         * whether or not 802.11be is supported on the Soft AP.
         * <p>
         * <li>If not set, defaults to true - which will be ignored on devices
         * which do not support 802.11be</li>
         *
         * @param enable true to enable, false to disable.
         * @return Builder for chaining.
         *
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @NonNull
        public Builder setIeee80211beEnabled(boolean enable) {
            if (!SdkLevel.isAtLeastT()) {
                throw new UnsupportedOperationException();
            }
            mIeee80211beEnabled = enable;
            return this;
        }

        /**
         * Specifies whether or not the configuration is configured by user.
         *
         * @param isUserConfigured true to user configuration, false otherwise.
         * @return Builder for chaining.
         *
         * @hide
         */
        @NonNull
        public Builder setUserConfiguration(boolean isUserConfigured) {
            mIsUserConfiguration = isUserConfigured;
            return this;
        }

        /**
         * Specifies bridged mode opportunistic shutdown timeout in milliseconds.
         * An instance of bridged Soft AP will shut down when there is no device connected to it
         * for this timeout duration.
         *
         * Specify a value of {@link DEFAULT_TIMEOUT} to have the framework automatically use
         * default timeout setting defined by
         * {@link
         * R.integer.config_wifiFrameworkSoftApShutDownIdleInstanceInBridgedModeTimeoutMillisecond}
         *
         * <p>
         * <li>If not set, defaults to {@link #DEFAULT_TIMEOUT}</li>
         * <li>The shut down timeout will apply when
         * {@link #setBridgedModeOpportunisticShutdownEnabled(boolean)} is set to true</li>
         *
         * @param timeoutMillis milliseconds of the timeout delay. Any value less than 1 is invalid
         *                      except {@link #DEFAULT_TIMEOUT}.
         * @return Builder for chaining.
         *
         * @see #setBridgedModeOpportunisticShutdownEnabled(boolean)
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @NonNull
        public Builder setBridgedModeOpportunisticShutdownTimeoutMillis(
                @IntRange(from = -1) long timeoutMillis) {
            if (!SdkLevel.isAtLeastT()) {
                throw new UnsupportedOperationException();
            }
            if (timeoutMillis < 1 && timeoutMillis != DEFAULT_TIMEOUT) {
                throw new IllegalArgumentException("Invalid timeout value: " + timeoutMillis);
            }
            mBridgedModeOpportunisticShutdownTimeoutMillis = timeoutMillis;
            return this;
        }

        /**
         * @param mac persistent randomized MacAddress generated by the frameworks.
         * @hide
         */
        @NonNull
        public Builder setRandomizedMacAddress(@NonNull MacAddress mac) {
            if (mac == null) {
                throw new IllegalArgumentException("setRandomizedMacAddress received"
                        + " null MacAddress.");
            }
            mPersistentRandomizedMacAddress = mac;
            return this;
        }

        /**
         * Set additional vendor-provided configuration data.
         *
         * @param vendorData List of {@link OuiKeyedData} containing the vendor-provided
         *     configuration data. Note that multiple elements with the same OUI are allowed.
         * @return Builder for chaining.
         */
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setVendorData(@NonNull List<OuiKeyedData> vendorData) {
            if (!SdkLevel.isAtLeastV()) {
                throw new UnsupportedOperationException();
            }
            if (vendorData == null) {
                throw new IllegalArgumentException("setVendorData received a null value");
            }
            mVendorData = vendorData;
            return this;
        }
    }
}

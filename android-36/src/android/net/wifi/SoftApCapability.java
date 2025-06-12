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
import android.annotation.LongDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.MacAddress;
import android.net.wifi.SoftApConfiguration.BandType;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;

import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;

/**
 * A class representing capability of the SoftAp.
 * {@see WifiManager}
 *
 * @hide
 */
@SystemApi
public final class SoftApCapability implements Parcelable {

    private static final String TAG = "SoftApCapability";
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    /**
     * Support for automatic channel selection in driver (ACS).
     * Driver will auto select best channel based on interference to optimize performance.
     *
     * flag when {@code R.bool.config_wifi_softap_acs_supported} is true.
     *
     * <p>
     * Use {@link WifiManager.SoftApCallback#onInfoChanged(SoftApInfo)} and
     * {@link SoftApInfo#getFrequency()} and {@link SoftApInfo#getBandwidth()} to get
     * driver channel selection result.
     */
    public static final long SOFTAP_FEATURE_ACS_OFFLOAD = 1 << 0;

    /**
     * Support for client force disconnect.
     * flag when {@code R.bool.config_wifiSofapClientForceDisconnectSupported} is true
     *
     * <p>
     * Several Soft AP client control features, e.g. specifying the maximum number of
     * Soft AP clients, only work when this feature support is present.
     * Check feature support before invoking
     * {@link SoftApConfiguration.Builder#setMaxNumberOfClients(int)}
     */
    public static final long SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT = 1 << 1;

    /**
     * Support for WPA3 Simultaneous Authentication of Equals (WPA3-SAE).
     *
     * flag when {@code config_wifi_softap_sae_supported} is true.
     */
    public static final long SOFTAP_FEATURE_WPA3_SAE = 1 << 2;

    /**
     * Support for MAC address customization.
     * flag when {@code R.bool.config_wifiSoftapMacAddressCustomizationSupported} is true
     *
     * <p>
     * Check feature support before invoking
     * {@link SoftApConfiguration.Builder#setBssid(MacAddress)} or
     * {@link SoftApConfiguration.Builder#setMacRandomizationSetting(int)} with
     * {@link SoftApConfiguration#RANDOMIZATION_PERSISTENT}
     */
    public static final long SOFTAP_FEATURE_MAC_ADDRESS_CUSTOMIZATION = 1 << 3;

    /**
     * Support for 802.11ax SAP.
     * flag when {@code R.bool.config_wifiSoftapIeee80211axSupported} is true
     *
     * <p>
     * Check feature support before invoking
     * {@link SoftApConfiguration.Builder#setIeee80211axEnabled(boolean)}
     */
    public static final long SOFTAP_FEATURE_IEEE80211_AX = 1 << 4;

    /**
     * Support for 2.4G Band.
     * flag when {@code R.bool.config_wifiSoftap24ghzSupported} is true
     */
    public static final long SOFTAP_FEATURE_BAND_24G_SUPPORTED = 1 << 5;

    /**
     * Support for 5G Band.
     * flag when {@code R.bool.config_wifiSoftap5ghzSupported} is true
     */
    public static final long SOFTAP_FEATURE_BAND_5G_SUPPORTED = 1 << 6;

    /**
     * Support for 6G Band.
     * flag when {@code R.bool.config_wifiSoftap6ghzSupported} is true
     */
    public static final long SOFTAP_FEATURE_BAND_6G_SUPPORTED = 1 << 7;

    /**
     * Support for 60G Band.
     * flag when {@code R.bool.config_wifiSoftap60ghzSupported} is true
     */
    public static final long SOFTAP_FEATURE_BAND_60G_SUPPORTED = 1 << 8;

    /**
     * Support for 802.11be SAP.
     * flag when {@code R.bool.config_wifiSoftapIeee80211beSupported} is true
     *
     * <p>
     * Use this flag with {@link #areFeaturesSupported(long)}
     * to verify that 802.11be is supported before enabling it using
     * {@link SoftApConfiguration.Builder#setIeee80211beEnabled(boolean)}
     */
    public static final long SOFTAP_FEATURE_IEEE80211_BE = 1 << 9;

    /*
     * Support for WPA3-Opportunistic Wireless Encryption (OWE) transition.
     * flag when {@code R.bool.config_wifiSoftapOweTransitionSupported} is true.
     */
    public static final long SOFTAP_FEATURE_WPA3_OWE_TRANSITION = 1 << 10;

    /*
     * Support for WPA3-Opportunistic Wireless Encryption (OWE).
     * flag when {@code R.bool.config_wifiSoftapOweSupported} is true.
     */
    public static final long SOFTAP_FEATURE_WPA3_OWE = 1 << 11;

    /*
     * Support for multiple link operation on a single multiple link device.
     * Flag when {@code R.Integer.config_wifiSoftApMaxNumberMLDSupported} is configured
     * to non zero value and chip report MLO SoftAP is supported.
     */
    @FlaggedApi(Flags.FLAG_MLO_SAP)
    public static final long SOFTAP_FEATURE_MLO = 1 << 12;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @LongDef(flag = true, prefix = { "SOFTAP_FEATURE_" }, value = {
            SOFTAP_FEATURE_ACS_OFFLOAD,
            SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT,
            SOFTAP_FEATURE_WPA3_SAE,
            SOFTAP_FEATURE_MAC_ADDRESS_CUSTOMIZATION,
            SOFTAP_FEATURE_IEEE80211_AX,
            SOFTAP_FEATURE_IEEE80211_BE,
            SOFTAP_FEATURE_BAND_24G_SUPPORTED,
            SOFTAP_FEATURE_BAND_5G_SUPPORTED,
            SOFTAP_FEATURE_BAND_6G_SUPPORTED,
            SOFTAP_FEATURE_BAND_60G_SUPPORTED,
            SOFTAP_FEATURE_WPA3_OWE_TRANSITION,
            SOFTAP_FEATURE_WPA3_OWE,
            SOFTAP_FEATURE_MLO,
    })
    public @interface HotspotFeatures {}

    private @HotspotFeatures long mSupportedFeatures = 0;

    private int mMaximumSupportedClientNumber;

    /**
     * A list storing supported 2.4G channels.
     */
    private int[] mSupportedChannelListIn24g = EMPTY_INT_ARRAY;

    /**
     * A list storing supported 5G channels.
     */
    private int[] mSupportedChannelListIn5g = EMPTY_INT_ARRAY;

    /**
     * A list storing supported 6G channels.
     */
    private int[] mSupportedChannelListIn6g = EMPTY_INT_ARRAY;

    /**
     * A list storing supported 60G channels.
     */
    private int[] mSupportedChannelListIn60g = EMPTY_INT_ARRAY;

    /**
     * A base country code which is used when querying the supported channel list.
     */
    private String mCountryCodeFromDriver;

    /**
     * Set the country code which is used when querying the supported channel list.
     * @hide
     */
    public void setCountryCode(String countryCode) {
        mCountryCodeFromDriver = countryCode;
    }

    /**
     * Get the country code which is used when querying the supported channel list.
     * @hide
     */
    public String getCountryCode() {
        return mCountryCodeFromDriver;
    }

    /**
     * Get the maximum supported client numbers which AP resides on.
     */
    public int getMaxSupportedClients() {
        return mMaximumSupportedClientNumber;
    }

    /**
     * Set the maximum supported client numbers which AP resides on.
     *
     * @param maxClient maximum supported client numbers for the softap.
     * @hide
     */
    public void setMaxSupportedClients(int maxClient) {
        mMaximumSupportedClientNumber = maxClient;
    }

    /**
     * Returns true when all of the queried features are supported, otherwise false.
     *
     * @param features One or combination of {@code SOFTAP_FEATURE_}, for instance:
     * {@link #SOFTAP_FEATURE_ACS_OFFLOAD}, {@link #SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT} or
     * {@link #SOFTAP_FEATURE_WPA3_SAE}.
     */
    public boolean areFeaturesSupported(@HotspotFeatures long features) {
        return (mSupportedFeatures & features) == features;
    }

    /**
     * Set SoftAp Capabilities
     * @param value Boolean to set value 0 or 1
     * @param features @HotspotFeatures represents which feature to access
     * @hide
     */
    public void setSupportedFeatures(boolean value, @HotspotFeatures long features) {
        if (value) {
            mSupportedFeatures |= features;
        } else {
            mSupportedFeatures &= ~features;
        }
    }

    /**
     * Set supported channel list in target band type.
     *
     * @param band One of the following band types:
     * {@link SoftApConfiguration#BAND_2GHZ}, {@link SoftApConfiguration#BAND_5GHZ},
     * {@link SoftApConfiguration#BAND_6GHZ}, or {@link SoftApConfiguration#BAND_60GHZ}.
     * @param supportedChannelList supported channel list in target band
     * @return true if band and supportedChannelList are valid, otherwise false.
     *
     * @throws IllegalArgumentException when band type is invalid.
     * @hide
     */
    @Keep
    public boolean setSupportedChannelList(@BandType int band,
            @Nullable int[] supportedChannelList) {
        if (supportedChannelList == null)  return false;
        switch (band) {
            case SoftApConfiguration.BAND_2GHZ:
                mSupportedChannelListIn24g = supportedChannelList;
                break;
            case SoftApConfiguration.BAND_5GHZ:
                mSupportedChannelListIn5g = supportedChannelList;
                break;
            case SoftApConfiguration.BAND_6GHZ:
                mSupportedChannelListIn6g = supportedChannelList;
                break;
            case SoftApConfiguration.BAND_60GHZ:
                mSupportedChannelListIn60g = supportedChannelList;
                break;
            default:
                throw new IllegalArgumentException("Invalid band: " + band);
        }
        return true;
    }

    /**
     * Returns a list of the supported channels in the given band.
     * The result depends on the on the country code that has been set.
     * Can be used to set the channel of the AP with the
     * {@link SoftApConfiguration.Builder#setChannel(int, int)} API.
     *
     * @param band One of the following band types:
     * {@link SoftApConfiguration#BAND_2GHZ}, {@link SoftApConfiguration#BAND_5GHZ},
     * {@link SoftApConfiguration#BAND_6GHZ}, {@link SoftApConfiguration#BAND_60GHZ}.
     * @return List of supported channels for the band. An empty list will be returned if the
     * channels are obsolete. This happens when country code has changed but the channels
     * are not updated from HAL when Wifi is disabled.
     *
     * @throws IllegalArgumentException when band type is invalid.
     */
    @NonNull
    public int[] getSupportedChannelList(@BandType int band) {
        switch (band) {
            case SoftApConfiguration.BAND_2GHZ:
                return mSupportedChannelListIn24g;
            case SoftApConfiguration.BAND_5GHZ:
                return mSupportedChannelListIn5g;
            case SoftApConfiguration.BAND_6GHZ:
                return mSupportedChannelListIn6g;
            case SoftApConfiguration.BAND_60GHZ:
                return mSupportedChannelListIn60g;
            default:
                throw new IllegalArgumentException("Invalid band: " + band);
        }
    }

    /**
     * @hide
     */
    public SoftApCapability(@Nullable SoftApCapability source) {
        if (source != null) {
            mSupportedFeatures = source.mSupportedFeatures;
            mMaximumSupportedClientNumber = source.mMaximumSupportedClientNumber;
            mSupportedChannelListIn24g = source.mSupportedChannelListIn24g;
            mSupportedChannelListIn5g = source.mSupportedChannelListIn5g;
            mSupportedChannelListIn6g = source.mSupportedChannelListIn6g;
            mSupportedChannelListIn60g = source.mSupportedChannelListIn60g;
            mCountryCodeFromDriver = source.mCountryCodeFromDriver;
        }
    }

    /**
     * Constructor with combination of the feature.
     * Zero to no supported feature.
     *
     * @param features One or combination of {@code SOFTAP_FEATURE_}, for instance:
     * {@link #SOFTAP_FEATURE_ACS_OFFLOAD}, {@link #SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT} or
     * {@link #SOFTAP_FEATURE_WPA3_SAE}.
     * @hide
     */
    public SoftApCapability(@HotspotFeatures long features) {
        mSupportedFeatures = features;
    }

    @Override
    /** Implement the Parcelable interface. */
    public int describeContents() {
        return 0;
    }

    @Override
    /** Implement the Parcelable interface */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(mSupportedFeatures);
        dest.writeInt(mMaximumSupportedClientNumber);
        dest.writeIntArray(mSupportedChannelListIn24g);
        dest.writeIntArray(mSupportedChannelListIn5g);
        dest.writeIntArray(mSupportedChannelListIn6g);
        dest.writeIntArray(mSupportedChannelListIn60g);
        dest.writeString(mCountryCodeFromDriver);
    }

    @NonNull
    /** Implement the Parcelable interface */
    public static final Creator<SoftApCapability> CREATOR = new Creator<SoftApCapability>() {
        public SoftApCapability createFromParcel(Parcel in) {
            SoftApCapability capability = new SoftApCapability(in.readLong());
            capability.mMaximumSupportedClientNumber = in.readInt();
            capability.setSupportedChannelList(SoftApConfiguration.BAND_2GHZ, in.createIntArray());
            capability.setSupportedChannelList(SoftApConfiguration.BAND_5GHZ, in.createIntArray());
            capability.setSupportedChannelList(SoftApConfiguration.BAND_6GHZ, in.createIntArray());
            capability.setSupportedChannelList(SoftApConfiguration.BAND_60GHZ, in.createIntArray());
            capability.setCountryCode(in.readString());
            return capability;
        }

        public SoftApCapability[] newArray(int size) {
            return new SoftApCapability[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("SupportedFeatures=").append(mSupportedFeatures);
        sbuf.append(" MaximumSupportedClientNumber=").append(mMaximumSupportedClientNumber);
        sbuf.append(" SupportedChannelListIn24g")
                .append(Arrays.toString(mSupportedChannelListIn24g));
        sbuf.append(" SupportedChannelListIn5g").append(Arrays.toString(mSupportedChannelListIn5g));
        sbuf.append(" SupportedChannelListIn6g").append(Arrays.toString(mSupportedChannelListIn6g));
        sbuf.append(" SupportedChannelListIn60g")
                .append(Arrays.toString(mSupportedChannelListIn60g));
        sbuf.append(" mCountryCodeFromDriver").append(mCountryCodeFromDriver);
        return sbuf.toString();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof SoftApCapability)) return false;
        SoftApCapability capability = (SoftApCapability) o;
        return mSupportedFeatures == capability.mSupportedFeatures
                && mMaximumSupportedClientNumber == capability.mMaximumSupportedClientNumber
                && Arrays.equals(mSupportedChannelListIn24g, capability.mSupportedChannelListIn24g)
                && Arrays.equals(mSupportedChannelListIn5g, capability.mSupportedChannelListIn5g)
                && Arrays.equals(mSupportedChannelListIn6g, capability.mSupportedChannelListIn6g)
                && Arrays.equals(mSupportedChannelListIn60g, capability.mSupportedChannelListIn60g)
                && Objects.equals(mCountryCodeFromDriver, capability.mCountryCodeFromDriver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSupportedFeatures, mMaximumSupportedClientNumber,
                Arrays.hashCode(mSupportedChannelListIn24g),
                Arrays.hashCode(mSupportedChannelListIn5g),
                Arrays.hashCode(mSupportedChannelListIn6g),
                Arrays.hashCode(mSupportedChannelListIn60g),
                mCountryCodeFromDriver);
    }
}

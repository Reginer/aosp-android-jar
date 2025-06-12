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
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.MacAddress;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import com.android.internal.util.Preconditions;
import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A class representing information about a specific SoftAP instance. A SoftAP instance may be a
 * single band AP or a bridged AP (across multiple bands). To get the state of the AP interface
 * itself, use {@link android.net.wifi.WifiManager.SoftApCallback#onStateChanged(SoftApState)}.
 * {@see WifiManager}
 *
 * @hide
 */
@SystemApi
public final class SoftApInfo implements Parcelable {

    /**
     * AP Channel bandwidth is automatically selected by the chip.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_AUTO = -1;

   /**
     * AP Channel bandwidth is invalid.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_INVALID = 0;

    /**
     * AP Channel bandwidth is 20 MHZ but no HT.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_20MHZ_NOHT = 1;

    /**
     * AP Channel bandwidth is 20 MHZ.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_20MHZ = 2;

    /**
     * AP Channel bandwidth is 40 MHZ.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_40MHZ = 3;

    /**
     * AP Channel bandwidth is 80 MHZ.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_80MHZ = 4;

    /**
     * AP Channel bandwidth is 160 MHZ, but 80MHZ + 80MHZ.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_80MHZ_PLUS_MHZ = 5;

    /**
     * AP Channel bandwidth is 160 MHZ.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_160MHZ = 6;

    /**
     * AP Channel bandwidth is 2160 MHZ.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_2160MHZ = 7;

    /**
     * AP Channel bandwidth is 4320 MHZ.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_4320MHZ = 8;

    /**
     * AP Channel bandwidth is 6480 MHZ.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_6480MHZ = 9;

    /**
     * AP Channel bandwidth is 8640 MHZ.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_8640MHZ = 10;

    /**
     * AP Channel bandwidth is 320 MHZ.
     *
     * @see #getBandwidth()
     */
    public static final int CHANNEL_WIDTH_320MHZ = 11;


    /** The frequency which AP resides on.  */
    private int mFrequency = 0;

    @WifiAnnotations.Bandwidth
    private int mBandwidth = CHANNEL_WIDTH_INVALID;

    /** The MAC Address which AP resides on. */
    @Nullable
    private MacAddress mBssid;

    /** The identifier of the AP instance which AP resides on with current info. */
    @Nullable
    private String mApInstanceIdentifier;

    /**
     * The operational mode of the AP.
     */
    private @WifiAnnotations.WifiStandard int mWifiStandard = ScanResult.WIFI_STANDARD_UNKNOWN;

    /**
     * The current shutdown timeout millis which applied on Soft AP.
     */
    private long mIdleShutdownTimeoutMillis;

    /** List of {@link OuiKeyedData} containing vendor-specific configuration data. */
    private List<OuiKeyedData> mVendorData = Collections.emptyList();

    /** The multiple link device (MLD) MAC Address which Wi-Fi 7 AP resides on. */
    @Nullable
    private MacAddress mMldAddress;

    /**
     * Get the frequency which AP resides on.
     */
    public int getFrequency() {
        return mFrequency;
    }

    /**
     * Set the frequency which AP resides on.
     * @hide
     */
    public void setFrequency(int freq) {
        mFrequency = freq;
    }

    /**
     * Get AP Channel bandwidth.
     *
     * @return One of {@link #CHANNEL_WIDTH_20MHZ}, {@link #CHANNEL_WIDTH_40MHZ},
     * {@link #CHANNEL_WIDTH_80MHZ}, {@link #CHANNEL_WIDTH_160MHZ},
     * {@link #CHANNEL_WIDTH_80MHZ_PLUS_MHZ}, {@link #CHANNEL_WIDTH_320MHZ},
     * {@link #CHANNEL_WIDTH_2160MHZ}, {@link #CHANNEL_WIDTH_4320MHZ},
     * {@link #CHANNEL_WIDTH_6480MHZ}, {@link #CHANNEL_WIDTH_8640MHZ},
     * {@link #CHANNEL_WIDTH_AUTO} ,or {@link #CHANNEL_WIDTH_INVALID}.
     */
    @WifiAnnotations.Bandwidth
    public int getBandwidth() {
        return mBandwidth;
    }

    /**
     * Set AP Channel bandwidth.
     * @hide
     */
    public void setBandwidth(@WifiAnnotations.Bandwidth int bandwidth) {
        mBandwidth = bandwidth;
    }

    /**
     * Get the MAC address (BSSID) of the AP. Null when AP disabled.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @Nullable
    public MacAddress getBssid() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return getBssidInternal();
    }

    /**
     * @hide
     */
    @Nullable
    public MacAddress getBssidInternal() {
        return mBssid;
    }

    /**
      * Set the MAC address which AP resides on.
      * <p>
      * <li>If not set, defaults to null.</li>
      * @param bssid BSSID, The caller is responsible for avoiding collisions.
      * @throws IllegalArgumentException when the given BSSID is the all-zero or broadcast MAC
      *                                  address.
      *
      * @hide
      */
    public void setBssid(@Nullable MacAddress bssid) {
        if (bssid != null) {
            Preconditions.checkArgument(!bssid.equals(WifiManager.ALL_ZEROS_MAC_ADDRESS));
            Preconditions.checkArgument(!bssid.equals(MacAddress.BROADCAST_ADDRESS));
        }
        mBssid = bssid;
    }

    /**
     * Set the operational mode of the AP.
     *
     * @param wifiStandard values from {@link ScanResult}'s {@code WIFI_STANDARD_}
     * @hide
     */
    public void setWifiStandard(@WifiAnnotations.WifiStandard int wifiStandard) {
        mWifiStandard = wifiStandard;
    }

    /**
     * Get the operational mode of the AP.
     * @return valid values from {@link ScanResult}'s {@code WIFI_STANDARD_}
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public @WifiAnnotations.WifiStandard int getWifiStandard() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return getWifiStandardInternal();
    }

    /**
     * @hide
     */
    public @WifiAnnotations.WifiStandard int getWifiStandardInternal() {
        return mWifiStandard;
    }

    /**
     * Set the AP instance identifier.
     * @hide
     */
    public void setApInstanceIdentifier(@NonNull String apInstanceIdentifier) {
        mApInstanceIdentifier = apInstanceIdentifier;
    }

    /**
     * Get the AP instance identifier.
     *
     * The AP instance identifier is a unique identity which can be used to
     * associate the {@link SoftApInfo} to a specific {@link WifiClient}
     * - see {@link WifiClient#getApInstanceIdentifier()}
     *
     * @hide
     */
    @Nullable
    public String getApInstanceIdentifier() {
        return mApInstanceIdentifier;
    }


    /**
     * Set current shutdown timeout millis which applied on Soft AP.
     * @hide
     */
    public void setAutoShutdownTimeoutMillis(long idleShutdownTimeoutMillis) {
        mIdleShutdownTimeoutMillis = idleShutdownTimeoutMillis;
    }

    /**
     * Get auto shutdown timeout in millis.
     *
     * The shutdown timeout value is configured by
     * {@link SoftApConfiguration.Builder#setAutoShutdownEnabled(int)} or
     * the default timeout setting defined in device overlays.
     *
     * A value of 0 means that auto shutdown is disabled.
     * {@see SoftApConfiguration#isAutoShutdownEnabled()}
     */
    public long getAutoShutdownTimeoutMillis() {
        return mIdleShutdownTimeoutMillis;
    }

    /**
     * Set additional vendor-provided configuration data.
     *
     * @param vendorData List of {@link android.net.wifi.OuiKeyedData} containing the
     *                   vendor-provided configuration data. Note that multiple elements with
     *                   the same OUI are allowed.
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
     * Get the vendor-provided configuration data, if it exists.
     *
     * @return Vendor configuration data, or empty list if it does not exist.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @SystemApi
    @NonNull
    public List<OuiKeyedData> getVendorData() {
        if (!SdkLevel.isAtLeastV()) {
            throw new UnsupportedOperationException();
        }
        return mVendorData;
    }

    /**
     * Get the multiple link device MAC address which Wi-Fi AP resides on.
     * Null when AP disabled or non Wi-Fi 7 AP.
     */
    @FlaggedApi(Flags.FLAG_MLO_SAP)
    @Nullable
    public MacAddress getMldAddress() {
        return mMldAddress;
    }

    /**
     * Set the multi-link address (MLA) of the multi-link device (MLD) on the Wi-Fi AP.
     * <p>
     * <li>If not set (non Wi-Fi 7 Soft AP), defaults to null.</li>
     * @param mldAddress  multiple link device MAC address.
     *
     * @hide
     */
    public void setMldAddress(@Nullable MacAddress mldAddress) {
        mMldAddress = mldAddress;
    }

    /**
     * @hide
     */
    public SoftApInfo(@Nullable SoftApInfo source) {
        if (source != null) {
            mFrequency = source.mFrequency;
            mBandwidth = source.mBandwidth;
            mBssid = source.mBssid;
            mWifiStandard = source.mWifiStandard;
            mApInstanceIdentifier = source.mApInstanceIdentifier;
            mIdleShutdownTimeoutMillis = source.mIdleShutdownTimeoutMillis;
            mVendorData = new ArrayList<>(source.mVendorData);
            mMldAddress = source.mMldAddress;
        }
    }

    /**
     * @hide
     */
    public SoftApInfo() {
    }

    @Override
    /** Implement the Parcelable interface. */
    public int describeContents() {
        return 0;
    }

    @Override
    /** Implement the Parcelable interface */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mFrequency);
        dest.writeInt(mBandwidth);
        dest.writeParcelable(mBssid, flags);
        dest.writeInt(mWifiStandard);
        dest.writeString(mApInstanceIdentifier);
        dest.writeLong(mIdleShutdownTimeoutMillis);
        dest.writeList(mVendorData);
        dest.writeParcelable(mMldAddress, flags);
    }

    @NonNull
    /** Implement the Parcelable interface */
    public static final Creator<SoftApInfo> CREATOR = new Creator<SoftApInfo>() {
        public SoftApInfo createFromParcel(Parcel in) {
            SoftApInfo info = new SoftApInfo();
            info.mFrequency = in.readInt();
            info.mBandwidth = in.readInt();
            info.mBssid = in.readParcelable(MacAddress.class.getClassLoader());
            info.mWifiStandard = in.readInt();
            info.mApInstanceIdentifier = in.readString();
            info.mIdleShutdownTimeoutMillis = in.readLong();
            info.mVendorData = ParcelUtil.readOuiKeyedDataList(in);
            info.mMldAddress = in.readParcelable(MacAddress.class.getClassLoader());
            return info;
        }

        public SoftApInfo[] newArray(int size) {
            return new SoftApInfo[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("SoftApInfo{");
        sbuf.append("bandwidth= ").append(mBandwidth);
        sbuf.append(", frequency= ").append(mFrequency);
        if (mBssid != null) sbuf.append(",bssid=").append(mBssid.toString());
        sbuf.append(", wifiStandard= ").append(mWifiStandard);
        sbuf.append(", mApInstanceIdentifier= ").append(mApInstanceIdentifier);
        sbuf.append(", mIdleShutdownTimeoutMillis= ").append(mIdleShutdownTimeoutMillis);
        sbuf.append(", mVendorData= ").append(mVendorData);
        if (mMldAddress != null) sbuf.append(",mMldAddress=").append(mMldAddress.toString());
        sbuf.append("}");
        return sbuf.toString();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof SoftApInfo)) return false;
        SoftApInfo softApInfo = (SoftApInfo) o;
        return mFrequency == softApInfo.mFrequency
                && mBandwidth == softApInfo.mBandwidth
                && Objects.equals(mBssid, softApInfo.mBssid)
                && mWifiStandard == softApInfo.mWifiStandard
                && Objects.equals(mApInstanceIdentifier, softApInfo.mApInstanceIdentifier)
                && mIdleShutdownTimeoutMillis == softApInfo.mIdleShutdownTimeoutMillis
                && Objects.equals(mVendorData, softApInfo.mVendorData)
                && Objects.equals(mMldAddress, softApInfo.mMldAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mFrequency, mBandwidth, mBssid, mWifiStandard, mApInstanceIdentifier,
                mIdleShutdownTimeoutMillis, mVendorData, mMldAddress);
    }
}

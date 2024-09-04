/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.annotation.SystemApi;
import android.net.wifi.OuiKeyedData;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.util.ArrayList;
import java.util.List;

/**
 * A class representing a Wi-Fi P2P scan configuration for setting up discovery.
 */
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
public final class WifiP2pDiscoveryConfig implements Parcelable {
    /** Type of this P2P scan. */
    private final @WifiP2pManager.WifiP2pScanType int mScanType;

    /** Frequency to scan in MHz. */
    private final int mFrequencyMhz;

    /** List of {@link OuiKeyedData} providing vendor-specific configuration data. */
    private final @NonNull List<OuiKeyedData> mVendorData;

    private WifiP2pDiscoveryConfig(@WifiP2pManager.WifiP2pScanType int scanType, int frequencyMhz,
            @NonNull List<OuiKeyedData> vendorData) {
        mScanType = scanType;
        mFrequencyMhz = frequencyMhz;
        mVendorData = new ArrayList<>(vendorData);
    }

    /**
     * Get the type of this scan. See {@link Builder#Builder(int)}}
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public @WifiP2pManager.WifiP2pScanType int getScanType() {
        return mScanType;
    }

    /**
     * Get the frequency to scan in MHz. See {@link Builder#setFrequencyMhz(int)}
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int getFrequencyMhz() {
        return mFrequencyMhz;
    }

    /**
     * Get the vendor-provided configuration data, if it exists. See {@link
     * Builder#setVendorData(List)}
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

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("\n scanType: ").append(mScanType);
        sbuf.append("\n frequencyMhz: ").append(mFrequencyMhz);
        sbuf.append("\n vendorData: ").append(mVendorData);
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int describeContents() {
        return 0;
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mScanType);
        dest.writeInt(mFrequencyMhz);
        dest.writeList(mVendorData);
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

    /** @hide */
    WifiP2pDiscoveryConfig(@NonNull Parcel in) {
        this.mScanType = in.readInt();
        this.mFrequencyMhz = in.readInt();
        this.mVendorData = readOuiKeyedDataList(in);
    }

    /** Implement the Parcelable interface */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @NonNull
    public static final Creator<WifiP2pDiscoveryConfig> CREATOR =
            new Creator<WifiP2pDiscoveryConfig>() {
                public WifiP2pDiscoveryConfig createFromParcel(Parcel in) {
                    return new WifiP2pDiscoveryConfig(in);
                }

                public WifiP2pDiscoveryConfig[] newArray(int size) {
                    return new WifiP2pDiscoveryConfig[size];
                }
            };

    /**
     * Builder for {@link WifiP2pDiscoveryConfig}.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final class Builder {

        private final int mScanType;
        private int mFrequencyMhz = 0;
        private @NonNull List<OuiKeyedData> mVendorData = new ArrayList<>();

        /**
         * Constructor for {@link Builder}.
         *
         * @param scanType Type of this P2P scan.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public Builder(@WifiP2pManager.WifiP2pScanType int scanType) {
            mScanType = scanType;
        }

        /**
         * Set the frequency to scan. Only applicable if the scan type is
         * {@link WifiP2pManager#WIFI_P2P_SCAN_SINGLE_FREQ}.
         *
         * @param frequencyMhz Frequency to scan in MHz.
         * @return Builder for chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setFrequencyMhz(@IntRange(from = 0) int frequencyMhz) {
            if (frequencyMhz <= 0) {
                throw new IllegalArgumentException("Frequency must be greater than 0");
            }
            mFrequencyMhz = frequencyMhz;
            return this;
        }

        /**
         * Set additional vendor-provided configuration data.
         *
         * @param vendorData List of {@link android.net.wifi.OuiKeyedData} containing the
         *                   vendor-provided configuration data. Note that multiple elements with
         *                   the same OUI are allowed.
         * @return Builder for chaining.
         * @hide
         */
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @SystemApi
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

        /**
         * Build {@link WifiP2pDiscoveryConfig} given the current requests made on the builder.
         * @return {@link WifiP2pDiscoveryConfig} constructed based on builder method calls.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public WifiP2pDiscoveryConfig build() {
            if (mScanType < WifiP2pManager.WIFI_P2P_SCAN_FULL
                    || mScanType > WifiP2pManager.WIFI_P2P_SCAN_SINGLE_FREQ) {
                throw new IllegalArgumentException("Invalid scan type " + mScanType);
            }
            if (mScanType == WifiP2pManager.WIFI_P2P_SCAN_SINGLE_FREQ && mFrequencyMhz == 0) {
                throw new IllegalArgumentException(
                        "Scan type is single frequency, but no frequency was provided");
            }
            return new WifiP2pDiscoveryConfig(mScanType, mFrequencyMhz, mVendorData);
        }
    }
}

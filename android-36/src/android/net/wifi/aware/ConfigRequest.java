/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.net.wifi.aware;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.annotation.SystemApi;
import android.net.wifi.OuiKeyedData;
import android.net.wifi.ParcelUtil;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Defines a request object to configure a Wi-Fi Aware network. Built using
 * {@link ConfigRequest.Builder}. Configuration is requested using
 * {@link WifiAwareManager#attach(AttachCallback, android.os.Handler)}.
 * Note that the actual achieved configuration may be different from the
 * requested configuration - since different applications may request different
 * configurations.
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
@SystemApi
public final class ConfigRequest implements Parcelable {
    /**
     * Lower range of possible cluster ID.
     * @hide
     */
    public static final int CLUSTER_ID_MIN = 0;

    /**
     * Upper range of possible cluster ID.
     * @hide
     */
    public static final int CLUSTER_ID_MAX = 0xFFFF;

    /**
     * Indices for configuration variables which are specified per band.
     * @hide
     */
    public static final int NAN_BAND_24GHZ = 0;

    /** @hide */
    public static final int NAN_BAND_5GHZ = 1;

    /** @hide */
    public static final int NAN_BAND_6GHZ = 2;

    /**
     * Magic values for Discovery Window (DW) interval configuration
     * @hide
     */
    public static final int DW_INTERVAL_NOT_INIT = -1;

    /** @hide */
    public static final int DW_DISABLE = 0; // only valid for 5GHz

    /**
     * Indicates whether 5G band support is requested.
     * @hide
     */
    public final boolean mSupport5gBand;

    /**
     * Indicates whether 6G band support is requested.
     * @hide
     */
    public final boolean mSupport6gBand;

    /**
     * Specifies the desired master preference.
     * @hide
     */
    public int mMasterPreference;

    /**
     * Specifies the desired lower range of the cluster ID. Must be lower than
     * {@link ConfigRequest#mClusterHigh}.
     * @hide
     */
    public final int mClusterLow;

    /**
     * Specifies the desired higher range of the cluster ID. Must be higher than
     * {@link ConfigRequest#mClusterLow}.
     * @hide
     */
    public final int mClusterHigh;

    /**
     * Specifies the discovery window interval for the device on NAN_BAND_*.
     * @hide
     */
    public final int mDiscoveryWindowInterval[];

    /**
     * List of {@link OuiKeyedData} providing vendor-specific configuration data.
     */
    private final List<OuiKeyedData> mVendorData;

    private ConfigRequest(boolean support5gBand, boolean support6gBand, int masterPreference,
            int clusterLow, int clusterHigh, int[] discoveryWindowInterval,
            @NonNull List<OuiKeyedData> vendorData) {
        mSupport5gBand = support5gBand;
        mSupport6gBand = support6gBand;
        mMasterPreference = masterPreference;
        mClusterLow = clusterLow;
        mClusterHigh = clusterHigh;
        mDiscoveryWindowInterval = discoveryWindowInterval;
        mVendorData = vendorData;
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public String toString() {
        return "ConfigRequest [mSupport5gBand=" + mSupport5gBand
                + ", mSupport6gBand=" + mSupport6gBand
                + ", mMasterPreference=" + mMasterPreference
                + ", mClusterLow=" + mClusterLow
                + ", mClusterHigh=" + mClusterHigh
                + ", mDiscoveryWindowInterval=" + Arrays.toString(mDiscoveryWindowInterval)
                + ", mVendorData=" + mVendorData
                + "]";
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public int describeContents() {
        return 0;
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mSupport5gBand ? 1 : 0);
        dest.writeInt(mSupport6gBand ? 1 : 0);
        dest.writeInt(mMasterPreference);
        dest.writeInt(mClusterLow);
        dest.writeInt(mClusterHigh);
        dest.writeIntArray(mDiscoveryWindowInterval);
        dest.writeList(mVendorData);
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final @android.annotation.NonNull Creator<ConfigRequest> CREATOR = new Creator<ConfigRequest>() {
        @Override
        public ConfigRequest[] newArray(int size) {
            return new ConfigRequest[size];
        }

        @Override
        public ConfigRequest createFromParcel(Parcel in) {
            boolean support5gBand = in.readInt() != 0;
            boolean support6gBand = in.readInt() != 0;
            int masterPreference = in.readInt();
            int clusterLow = in.readInt();
            int clusterHigh = in.readInt();
            int discoveryWindowInterval[] = in.createIntArray();
            List<OuiKeyedData> vendorData = ParcelUtil.readOuiKeyedDataList(in);

            return new ConfigRequest(support5gBand, support6gBand, masterPreference, clusterLow,
                    clusterHigh, discoveryWindowInterval, vendorData);
        }
    };

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConfigRequest)) {
            return false;
        }

        ConfigRequest lhs = (ConfigRequest) o;

        return mSupport5gBand == lhs.mSupport5gBand
                && mSupport6gBand == lhs.mSupport6gBand
                && mMasterPreference == lhs.mMasterPreference
                && mClusterLow == lhs.mClusterLow && mClusterHigh == lhs.mClusterHigh
                && Arrays.equals(mDiscoveryWindowInterval, lhs.mDiscoveryWindowInterval)
                && Objects.equals(mVendorData, lhs.mVendorData);
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + (mSupport5gBand ? 1 : 0);
        result = 31 * result + (mSupport6gBand ? 1 : 0);
        result = 31 * result + mMasterPreference;
        result = 31 * result + mClusterLow;
        result = 31 * result + mClusterHigh;
        result = 31 * result + Arrays.hashCode(mDiscoveryWindowInterval);
        result = 31 * result + mVendorData.hashCode();

        return result;
    }

    /**
     * Verifies that the contents of the ConfigRequest are valid. Otherwise
     * throws an IllegalArgumentException.
     * @hide
     */
    public void validate() throws IllegalArgumentException {
        if (mMasterPreference < 0) {
            throw new IllegalArgumentException(
                    "Master Preference specification must be non-negative");
        }
        if (mMasterPreference == 1 || mMasterPreference == 255 || mMasterPreference > 255) {
            throw new IllegalArgumentException("Master Preference specification must not "
                    + "exceed 255 or use 1 or 255 (reserved values)");
        }
        if (mClusterLow < CLUSTER_ID_MIN) {
            throw new IllegalArgumentException("Cluster specification must be non-negative");
        }
        if (mClusterLow > CLUSTER_ID_MAX) {
            throw new IllegalArgumentException("Cluster specification must not exceed 0xFFFF");
        }
        if (mClusterHigh < CLUSTER_ID_MIN) {
            throw new IllegalArgumentException("Cluster specification must be non-negative");
        }
        if (mClusterHigh > CLUSTER_ID_MAX) {
            throw new IllegalArgumentException("Cluster specification must not exceed 0xFFFF");
        }
        if (mClusterLow > mClusterHigh) {
            throw new IllegalArgumentException(
                    "Invalid argument combination - must have Cluster Low <= Cluster High");
        }
        if (mDiscoveryWindowInterval.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid discovery window interval: must have 3 elements (2.4 & 5 & 6");
        }
        if (mDiscoveryWindowInterval[NAN_BAND_24GHZ] != DW_INTERVAL_NOT_INIT &&
                (mDiscoveryWindowInterval[NAN_BAND_24GHZ] < 1 // valid for 2.4GHz: [1-5]
                || mDiscoveryWindowInterval[NAN_BAND_24GHZ] > 5)) {
            throw new IllegalArgumentException(
                    "Invalid discovery window interval for 2.4GHz: valid is UNSET or [1,5]");
        }
        if (mDiscoveryWindowInterval[NAN_BAND_5GHZ] != DW_INTERVAL_NOT_INIT &&
                (mDiscoveryWindowInterval[NAN_BAND_5GHZ] < 0 // valid for 5GHz: [0-5]
                || mDiscoveryWindowInterval[NAN_BAND_5GHZ] > 5)) {
            throw new IllegalArgumentException(
                "Invalid discovery window interval for 5GHz: valid is UNSET or [0,5]");
        }
        if (mDiscoveryWindowInterval[NAN_BAND_6GHZ] != DW_INTERVAL_NOT_INIT
                && (mDiscoveryWindowInterval[NAN_BAND_6GHZ] < 0 // valid for 6GHz: [0-5]
                || mDiscoveryWindowInterval[NAN_BAND_6GHZ] > 5)) {
            throw new IllegalArgumentException(
                "Invalid discovery window interval for 6GHz: valid is UNSET or [0,5]");
        }
        if (mVendorData == null) {
            throw new IllegalArgumentException("Vendor data list must be non-null");
        }
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
        return mVendorData != null ? mVendorData : Collections.emptyList();
    }

    /**
     * Builder used to build {@link ConfigRequest} objects.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final class Builder {
        private boolean mSupport5gBand = true;
        private boolean mSupport6gBand = false;
        private int mMasterPreference = 0;
        private int mClusterLow = CLUSTER_ID_MIN;
        private int mClusterHigh = CLUSTER_ID_MAX;
        private int[] mDiscoveryWindowInterval = {DW_INTERVAL_NOT_INIT, DW_INTERVAL_NOT_INIT,
                DW_INTERVAL_NOT_INIT};
        private List<OuiKeyedData> mVendorData = Collections.emptyList();

        /**
         * Specify whether 5G band support is required in this request. Disabled by default.
         *
         * @param support5gBand Support for 5G band is required.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        public Builder setSupport5gBand(boolean support5gBand) {
            mSupport5gBand = support5gBand;
            return this;
        }

        /**
         * Specify whether 6G band support is required in this request. Disabled by default.
         *
         * @param support6gBand Support for 6G band is required.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        public Builder setSupport6gBand(boolean support6gBand) {
            mSupport6gBand = support6gBand;
            return this;
        }

        /**
         * Specify the Master Preference requested. The permitted range is 0 (the default) to
         * 255 with 1 and 255 excluded (reserved).
         *
         * @param masterPreference The requested master preference
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        public Builder setMasterPreference(int masterPreference) {
            if (masterPreference < 0) {
                throw new IllegalArgumentException(
                        "Master Preference specification must be non-negative");
            }
            if (masterPreference == 1 || masterPreference == 255 || masterPreference > 255) {
                throw new IllegalArgumentException("Master Preference specification must not "
                        + "exceed 255 or use 1 or 255 (reserved values)");
            }

            mMasterPreference = masterPreference;
            return this;
        }

        /**
         * The Cluster ID is generated randomly for new Aware networks. Specify
         * the lower range of the cluster ID. The upper range is specified using
         * the {@link ConfigRequest.Builder#setClusterHigh(int)}. The permitted
         * range is 0 (the default) to the value specified by
         * {@link ConfigRequest.Builder#setClusterHigh(int)}. Equality of Low and High is
         * permitted which restricts the Cluster ID to the specified value.
         *
         * @param clusterLow The lower range of the generated cluster ID.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setClusterLow(..).setClusterHigh(..)}.
         * @hide
         */
        public Builder setClusterLow(int clusterLow) {
            if (clusterLow < CLUSTER_ID_MIN) {
                throw new IllegalArgumentException("Cluster specification must be non-negative");
            }
            if (clusterLow > CLUSTER_ID_MAX) {
                throw new IllegalArgumentException("Cluster specification must not exceed 0xFFFF");
            }

            mClusterLow = clusterLow;
            return this;
        }

        /**
         * The Cluster ID is generated randomly for new Aware networks. Specify
         * the lower upper of the cluster ID. The lower range is specified using
         * the {@link ConfigRequest.Builder#setClusterLow(int)}. The permitted
         * range is the value specified by
         * {@link ConfigRequest.Builder#setClusterLow(int)} to 0xFFFF (the default). Equality of
         * Low and High is permitted which restricts the Cluster ID to the specified value.
         *
         * @param clusterHigh The upper range of the generated cluster ID.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setClusterLow(..).setClusterHigh(..)}.
         * @hide
         */
        public Builder setClusterHigh(int clusterHigh) {
            if (clusterHigh < CLUSTER_ID_MIN) {
                throw new IllegalArgumentException("Cluster specification must be non-negative");
            }
            if (clusterHigh > CLUSTER_ID_MAX) {
                throw new IllegalArgumentException("Cluster specification must not exceed 0xFFFF");
            }

            mClusterHigh = clusterHigh;
            return this;
        }

        /**
         * The discovery window interval specifies the discovery windows in which the device will be
         * awake. The configuration enables trading off latency vs. power (higher interval means
         * higher discovery latency but lower power).
         *
         * @param band Either {@link #NAN_BAND_24GHZ} or {@link #NAN_BAND_5GHZ} or
         *        {@link #NAN_BAND_6GHZ}.
         * @param interval A value of 1, 2, 3, 4, or 5 indicating an interval of 2^(interval-1). For
         *                 the 5GHz band a value of 0 indicates that the device will not be awake
         *                 for any discovery windows.
         *
         * @return The builder itself to facilitate chaining operations
         *         {@code builder.setDiscoveryWindowInterval(...).setMasterPreference(...)}.
         * @hide
         */
        public Builder setDiscoveryWindowInterval(int band, int interval) {
            if (band != NAN_BAND_24GHZ && band != NAN_BAND_5GHZ && band != NAN_BAND_6GHZ) {
                throw new IllegalArgumentException("Invalid band value");
            }
            if ((band == NAN_BAND_24GHZ && (interval < 1 || interval > 5))
                    || (band == NAN_BAND_5GHZ && (interval < 0 || interval > 5))
                    || (band == NAN_BAND_6GHZ && (interval < 0 || interval > 5))) {
                throw new IllegalArgumentException(
                        "Invalid interval value: 2.4 GHz [1,5] or 5GHz/6GHz [0,5]");
            }

            mDiscoveryWindowInterval[band] = interval;
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
         * Build {@link ConfigRequest} given the current requests made on the
         * builder.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public ConfigRequest build() {
            if (mClusterLow > mClusterHigh) {
                throw new IllegalArgumentException(
                        "Invalid argument combination - must have Cluster Low <= Cluster High");
            }

            return new ConfigRequest(mSupport5gBand, mSupport6gBand, mMasterPreference, mClusterLow,
                    mClusterHigh, mDiscoveryWindowInterval, mVendorData);
        }
    }
}

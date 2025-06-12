/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.ranging.wifi.rtt;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.ranging.raw.RawRangingDevice;
import android.ranging.raw.RawRangingDevice.RangingUpdateRate;

import com.android.ranging.flags.Flags;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents the parameters required to perform Wi-Fi Round Trip Time (RTT) ranging.
 *
 */
@FlaggedApi(Flags.FLAG_RANGING_RTT_ENABLED)
public final class RttRangingParams implements Parcelable {

    private RttRangingParams(Parcel in) {
        mServiceName = in.readString();
        mMatchFilter = in.createByteArray();
        mRangingUpdateRate = in.readInt();
        mPeriodicRangingHwFeatureEnabled = in.readBoolean();
    }

    @NonNull
    public static final Creator<RttRangingParams> CREATOR = new Creator<RttRangingParams>() {
        @Override
        public RttRangingParams createFromParcel(Parcel in) {
            return new RttRangingParams(in);
        }

        @Override
        public RttRangingParams[] newArray(int size) {
            return new RttRangingParams[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mServiceName);
        dest.writeByteArray(mMatchFilter);
        dest.writeInt(mRangingUpdateRate);
        dest.writeBoolean(mPeriodicRangingHwFeatureEnabled);
    }

    private final String mServiceName;

    private final byte[] mMatchFilter;

    @RangingUpdateRate
    private final int mRangingUpdateRate;

    private final boolean mPeriodicRangingHwFeatureEnabled;

    /**
     * Returns the service name associated with this RTT ranging session.
     *
     * @return the service name as a {@link String}.
     * @see android.net.wifi.aware.PublishConfig.Builder#setServiceName(String)
     * @see android.net.wifi.aware.SubscribeConfig.Builder#setServiceName(String)
     */
    @NonNull
    public String getServiceName() {
        return mServiceName;
    }

    /**
     * Returns the match filter for this ranging session.
     *
     * @return a byte array representing the match filter.
     * @see android.net.wifi.aware.PublishConfig.Builder#setMatchFilter(List)
     * @see android.net.wifi.aware.SubscribeConfig.Builder#setMatchFilter(List)
     */
    @Nullable
    public byte[] getMatchFilter() {
        return mMatchFilter;
    }

    /**
     * Returns the ranging update rate.
     *
     * @return ranging update rate.
     * <p>Possible values:
     * {@link RangingUpdateRate#UPDATE_RATE_NORMAL}
     * {@link RangingUpdateRate#UPDATE_RATE_INFREQUENT}
     * {@link RangingUpdateRate#UPDATE_RATE_FREQUENT}
     */
    @RangingUpdateRate
    public int getRangingUpdateRate() {
        return mRangingUpdateRate;
    }

    /**
     * Returns whether the periodic ranging hardware feature was enabled.
     *
     * @return returns {@code true} if periodic ranging hardware feature was enabled, {@code false}
     * otherwise
     */
    public boolean isPeriodicRangingHwFeatureEnabled() {
        return mPeriodicRangingHwFeatureEnabled;
    }

    private RttRangingParams(Builder builder) {
        mServiceName = builder.mServiceName;
        mMatchFilter = builder.mMatchFilter;
        mRangingUpdateRate = builder.mRangingUpdateRate;
        mPeriodicRangingHwFeatureEnabled = builder.mPeriodicRangingHwFeatureEnabled;
    }

    /**
     * Builder class for {@link RttRangingParams}.
     */
    public static final class Builder {
        private String mServiceName = "";

        private byte[] mMatchFilter = null;

        private boolean mPeriodicRangingHwFeatureEnabled = false;
        @RawRangingDevice.RangingUpdateRate
        private int mRangingUpdateRate = RawRangingDevice.UPDATE_RATE_NORMAL;

        /**
         * Constructs a new {@link Builder} for creating a Wifi NAN-RTT ranging session.
         *
         * @param serviceName The service name associated with this session
         * @throws IllegalArgumentException if {@code serviceName} is null.
         */
        public Builder(@NonNull String serviceName) {
            Objects.requireNonNull(serviceName);
            mServiceName = serviceName;
        }

        /**
         * Sets the match filter to identify specific devices or services for RTT.
         *
         * @param matchFilter a byte array representing the filter.
         *
         * @return this {@link Builder} instance.
         * @throws NullPointerException if either parameter is {@code matchFilter} is null.
         */
        @NonNull
        public Builder setMatchFilter(@NonNull byte[] matchFilter) {
            Objects.requireNonNull(matchFilter);
            this.mMatchFilter = matchFilter.clone();
            return this;
        }

        /**
         * Sets the update rate for the RTT ranging session.
         * <p>Defaults to {@link RangingUpdateRate#UPDATE_RATE_NORMAL}
         *
         * @param updateRate the reporting frequency.
         *                   <p>Possible values:
         *                   {@link RangingUpdateRate#UPDATE_RATE_NORMAL}
         *                   {@link RangingUpdateRate#UPDATE_RATE_INFREQUENT}
         *                   {@link RangingUpdateRate#UPDATE_RATE_FREQUENT}
         * @return this {@link Builder} instance.
         */
        @NonNull
        public Builder setRangingUpdateRate(@RawRangingDevice.RangingUpdateRate int updateRate) {
            mRangingUpdateRate = updateRate;
            return this;
        }

        /**
         * Sets whether to use hardware supported periodic ranging feature in WiFi Nan-RTT.
         *
         * @param periodicRangingHwFeatureEnabled {@code true} to enable periodic ranging;
         *                                        {@code false} otherwise.
         * @return this {@link Builder} instance.
         */
        @NonNull
        public Builder setPeriodicRangingHwFeatureEnabled(boolean periodicRangingHwFeatureEnabled) {
            mPeriodicRangingHwFeatureEnabled = periodicRangingHwFeatureEnabled;
            return this;
        }

        /**
         * Builds and returns a new {@link RttRangingParams} instance.
         *
         * @return a new {@link RttRangingParams} object configured with the provided parameters.
         */
        @NonNull
        public RttRangingParams build() {
            return new RttRangingParams(this);
        }
    }

    @Override
    public String toString() {
        return "RttRangingParams{ "
                + "mServiceName='"
                + mServiceName
                + ", mMatchFilter="
                + Arrays.toString(mMatchFilter)
                + ", mRangingUpdateRate="
                + mRangingUpdateRate
                + ", mPeriodicRangingHwFeatureEnabled="
                + mPeriodicRangingHwFeatureEnabled
                + " }";
    }
}

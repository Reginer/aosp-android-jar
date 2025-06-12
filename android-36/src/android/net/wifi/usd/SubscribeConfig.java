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

package android.net.wifi.usd;

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.aware.TlvBufferUtils;
import android.net.wifi.aware.WifiAwareUtils;
import android.net.wifi.flags.Flags;
import android.os.Parcel;
import android.os.Parcelable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * Defines the configuration of USD subscribe session.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_USD)
public final class SubscribeConfig extends Config implements Parcelable {

    private SubscribeConfig(Builder builder) {
        super(builder.mServiceName, builder.mTtlSeconds, builder.mServiceProtoType,
                builder.mTxMatchFilterTlv, builder.mRxMatchFilterTlv, builder.mServiceSpecificInfo,
                builder.mOperatingFrequencies);
        mSubscribeType = builder.mSubscribeType;
        mQueryPeriodMillis = builder.mQueryPeriodMillis;
        mRecommendedFrequencies = builder.mRecommendedFrequencies;
    }

    @SubscribeType
    private final int mSubscribeType;
    private final int mQueryPeriodMillis;
    private final int[] mRecommendedFrequencies;

    private SubscribeConfig(Parcel in) {
        super(in.createByteArray(), in.readInt(), in.readInt(), in.createByteArray(),
                in.createByteArray(), in.createByteArray(), in.createIntArray());
        mSubscribeType = in.readInt();
        mQueryPeriodMillis = in.readInt();
        mRecommendedFrequencies = in.createIntArray();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeByteArray(getServiceName());
        dest.writeInt(getTtlSeconds());
        dest.writeInt(getServiceProtoType());
        dest.writeByteArray(getTxMatchFilterTlv());
        dest.writeByteArray(getRxMatchFilterTlv());
        dest.writeByteArray(getServiceSpecificInfo());
        dest.writeIntArray(getOperatingFrequenciesMhz());
        dest.writeInt(mSubscribeType);
        dest.writeInt(mQueryPeriodMillis);
        dest.writeIntArray(mRecommendedFrequencies);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<SubscribeConfig> CREATOR = new Creator<SubscribeConfig>() {

        @Override
        public SubscribeConfig createFromParcel(Parcel in) {
            return new SubscribeConfig(in);
        }

        @Override
        public SubscribeConfig[] newArray(int size) {
            return new SubscribeConfig[size];
        }
    };

    /**
     * Gets the type of subscribe session. See {@code SUBSCRIBE_TYPE_XXX} for different types of
     * subscribe.
     *
     * @return subscribe type
     */
    @SubscribeType
    public int getSubscribeType() {
        return mSubscribeType;
    }

    /**
     * Gets the recommended periodicity of query transmissions for the subscribe session.
     *
     * @return Query period in milliseconds
     */
    @IntRange(from = 0)
    public int getQueryPeriodMillis() {
        return mQueryPeriodMillis;
    }

    /**
     * Gets the recommended frequency list to be used for subscribe operation. See
     * {@link Builder#setRecommendedOperatingFrequenciesMhz(int[])}.
     *
     * @return frequency list or null if not set
     */
    @Nullable
    public int[] getRecommendedOperatingFrequenciesMhz() {
        return mRecommendedFrequencies;
    }

    @Override
    public String toString() {
        return super.toString() + " SubscribeConfig{" + "mSubscribeType=" + mSubscribeType
                + ", mQueryPeriodMillis=" + mQueryPeriodMillis + ", mRecommendedFrequencies="
                + Arrays.toString(mRecommendedFrequencies) + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscribeConfig that)) return false;
        if (!super.equals(o)) return false;
        return mSubscribeType == that.mSubscribeType
                && mQueryPeriodMillis == that.mQueryPeriodMillis
                && Arrays.equals(mRecommendedFrequencies, that.mRecommendedFrequencies);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), mSubscribeType, mQueryPeriodMillis);
        result = 31 * result + Arrays.hashCode(mRecommendedFrequencies);
        return result;
    }

    /**
     * {@code SubscribeConfig} builder static inner class.
     */
    @FlaggedApi(Flags.FLAG_USD)
    public static final class Builder {
        @SubscribeType private int mSubscribeType = SUBSCRIBE_TYPE_ACTIVE;
        private int mQueryPeriodMillis = 100;
        private int[] mRecommendedFrequencies = null;
        private final byte[] mServiceName;
        private int mTtlSeconds = 3000;
        @ServiceProtoType private int mServiceProtoType = SERVICE_PROTO_TYPE_GENERIC;
        private byte[] mTxMatchFilterTlv = null;
        private byte[] mRxMatchFilterTlv = null;
        private byte[] mServiceSpecificInfo = null;
        private int[] mOperatingFrequencies = null;

        /**
         * Builder for {@link SubscribeConfig}
         *
         * @param serviceName Specify the service name of the USD session. The Service Name is a
         *                    UTF-8 encoded string from 1 to
         *                    {@link Characteristics#getMaxServiceNameLength()} bytes in length.
         *                    The only acceptable single-byte UTF-8 symbols for a Service Name are
         *                    alphanumeric values (A-Z, a-z, 0-9), the hyphen ('-'), the period
         *                    ('.') and the underscore ('_'). Allvalid multi-byte UTF-8
         *                    characters are acceptable in a Service Name.
         */
        public Builder(@NonNull String serviceName) {
            Objects.requireNonNull(serviceName, "serviceName must not be null");
            mServiceName = serviceName.getBytes(StandardCharsets.UTF_8);
            WifiAwareUtils.validateServiceName(mServiceName);
        }

        /**
         * Sets the time to live for the USD session and returns a reference to this Builder
         * enabling method chaining. Default value is 3000 seconds.
         *
         * @param ttlSeconds Time to live in seconds. Value 0 indicating the session does not
         *                   terminate on its own.
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setTtlSeconds(@IntRange(from = 0) int ttlSeconds) {
            if (ttlSeconds < 0) {
                throw new IllegalArgumentException("Invalid ttlSec - must be non-negative");
            }
            mTtlSeconds = ttlSeconds;
            return this;
        }

        /**
         * Sets the {@code serviceProtoType} and returns a reference to this Builder enabling method
         * chaining. Supported service protocol is defined as {@code SERVICE_PROTO_TYPE_*}. Default
         * value is {@link #SERVICE_PROTO_TYPE_GENERIC}.
         *
         * @param serviceProtoType the {@code serviceProtoType} to set
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setServiceProtoType(@ServiceProtoType int serviceProtoType) {
            if (serviceProtoType < SERVICE_PROTO_TYPE_GENERIC
                    || serviceProtoType > SERVICE_PROTO_TYPE_CSA_MATTER) {
                throw new IllegalArgumentException("Invalid serviceProtoType - "
                        + serviceProtoType);
            }
            mServiceProtoType = serviceProtoType;
            return this;
        }

        /**
         * Sets the {@code txMatchFilter} and returns a reference to this Builder enabling method
         * chaining. The {@code txMatchFilter} is the ordered sequence of (length, value) pairs to
         * be included in the subscribe frame. If not set, empty by default.
         *
         * <p>See Wi-Fi Aware Specification Version 4.0, section: Appendix H (Informative) Matching
         * filter examples.
         *
         * @param txMatchFilter the {@code txMatchFilter} to set
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setTxMatchFilter(@NonNull List<byte[]> txMatchFilter) {
            Objects.requireNonNull(txMatchFilter, "txMatchFilter must not be null");
            mTxMatchFilterTlv = new TlvBufferUtils.TlvConstructor(0, 1).allocateAndPut(
                    txMatchFilter).getArray();
            if (!TlvBufferUtils.isValid(mTxMatchFilterTlv, 0, 1)) {
                throw new IllegalArgumentException(
                        "Invalid txMatchFilter configuration - LV fields do not match up to "
                                + "length");
            }
            return this;
        }

        /**
         * Sets the {@code rxMatchFilter} and returns a reference to this Builder enabling method
         * chaining. The {@code rxMatchFilter} is the ordered sequence of (length, value) pairs
         * that specify further the matching conditions beyond the service name used to filter
         * the USD discovery messages. When a subscriber receives a publish message, it matches the
         * matching filter field in the publish message against its own matching_filter_rx. If not
         * set, empty by default.
         *
         * <p>See Wi-Fi Aware Specification Version 4.0, section: Appendix H (Informative) Matching
         * filter examples.
         *
         * @param rxMatchFilter the {@code rxMatchFilter} to set
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setRxMatchFilter(@NonNull List<byte[]> rxMatchFilter) {
            Objects.requireNonNull(rxMatchFilter, "rxMatchFilter must not be null");
            mRxMatchFilterTlv = new TlvBufferUtils.TlvConstructor(0, 1).allocateAndPut(
                    rxMatchFilter).getArray();
            if (!TlvBufferUtils.isValid(mRxMatchFilterTlv, 0, 1)) {
                throw new IllegalArgumentException(
                        "Invalid rxMatchFilter configuration - LV fields do not match up to "
                                + "length");
            }
            return this;
        }

        /**
         * Sets the susbcribe type and returns a reference to this Builder enabling method chaining.
         *
         * @param subscribeType the {@code subscribeType} to set
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setSubscribeType(@SubscribeType int subscribeType) {
            if (subscribeType < SUBSCRIBE_TYPE_PASSIVE || subscribeType > SUBSCRIBE_TYPE_ACTIVE) {
                throw new IllegalArgumentException("Invalid subscribeType - " + subscribeType);
            }
            mSubscribeType = subscribeType;
            return this;
        }

        /**
         * Sets the query period and returns a reference to this Builder enabling method chaining.
         * Default value is 100 ms.
         *
         * @param queryPeriodMillis the {@code queryPeriodMillis} to set
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setQueryPeriodMillis(@IntRange(from = 0) int queryPeriodMillis) {
            if (queryPeriodMillis < 0) {
                throw new IllegalArgumentException(
                        "Invalid queryPeriodMillis - must be non-negative");
            }
            mQueryPeriodMillis = queryPeriodMillis;
            return this;
        }

        /**
         * Sets the recommended frequencies to use in case the framework couldn't pick a default
         * channel for the subscriber operation. This will be a no-op if
         * {@link #setOperatingFrequenciesMhz(int[])} is used.
         *
         * <p>Here is the default subscriber channel selection preference order with {@code
         * recommendedFreqList}
         * <ol>
         * <li>Channel 6 in 2.4 Ghz if there is no multichannel concurrency.
         * <li>Station channel if the station connected on non-DFS/Indoor channel.
         * <li>Pick a channel from {@code recommendedFreqList} if regulatory permits.
         * <li>Pick any available channel
         * </ol>
         *
         * <p>Note: If multiple channels are available for the subscriber, the channel having AP
         * with the best RSSI will be picked.
         *
         * @param recommendedFrequencies the {@code recommendedFreqList} to set
         * @return a reference to this Builder
         * @throws IllegalArgumentException if frequencies are invalid or the number frequencies
         * are more than the number of 20 Mhz channels in 2.4 Ghz and 5 Ghz as per regulatory.
         */
        @NonNull
        public Builder setRecommendedOperatingFrequenciesMhz(
                @NonNull int[] recommendedFrequencies) {
            Objects.requireNonNull(recommendedFrequencies,
                    "recommendedFrequencies must not be null");
            if ((recommendedFrequencies.length > MAX_NUM_OF_OPERATING_FREQUENCIES)
                    || !WifiNetworkSpecifier.validateChannelFrequencyInMhz(
                    recommendedFrequencies)) {
                throw new IllegalArgumentException("Invalid recommendedFrequencies");
            }
            this.mRecommendedFrequencies = recommendedFrequencies.clone();
            return this;
        }

        /**
         * Specify service specific information for the publish session. This is a free-form byte
         * array available to the application to send additional information as part of the
         * discovery operation - it will not be used to determine whether a publish/subscribe
         * match occurs. Default value is null;
         *
         * Note: Maximum length is limited by
         * {@link Characteristics#getMaxServiceSpecificInfoLength()}
         *
         * @param serviceSpecificInfo A byte-array for the service-specific
         *            information field.
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setServiceSpecificInfo(@NonNull byte[] serviceSpecificInfo) {
            Objects.requireNonNull(serviceSpecificInfo, "serviceSpecificInfo must not be null");
            mServiceSpecificInfo = serviceSpecificInfo.clone();
            return this;
        }

        /**
         * Sets the frequencies used for subscribe operation. The subscriber picks one of the
         * frequencies from this list. This overrides the default channel selection as described
         * below.
         *
         * <p>If null, here is the default subscriber channel selection preference order,
         * <ol>
         * <li>Channel 6 in 2.4 Ghz if there is no multichannel concurrency.
         * <li>Station channel if the station connected on non-DFS/Indoor channel.
         * <li>Pick a channel from {@link #setRecommendedOperatingFrequenciesMhz(int[])} if
         * regulatory permits.
         * <li>Pick any available channel.
         * </ol>
         * <p>Note: the dwell time for subscriber operation is calculated internally based on
         * existing concurrency operation (e.g. Station + USD).
         *
         * @param operatingFrequencies frequencies used for subscribe operation
         * @return a reference to this Builder
         * @throws IllegalArgumentException if frequencies are invalid or the number frequencies
         * are more than the number of 20 Mhz channels in 2.4 Ghz and 5 Ghz as per regulatory.
         */
        @NonNull
        public Builder setOperatingFrequenciesMhz(@NonNull int[] operatingFrequencies) {
            Objects.requireNonNull(operatingFrequencies, "operatingFrequencies must not be null");
            if ((operatingFrequencies.length > MAX_NUM_OF_OPERATING_FREQUENCIES)
                    || !WifiNetworkSpecifier.validateChannelFrequencyInMhz(operatingFrequencies)) {
                throw new IllegalArgumentException("Invalid operatingFrequencies");
            }
            mOperatingFrequencies = operatingFrequencies.clone();
            return this;
        }

        /**
         * Returns a {@code SubscribeConfig} built from the parameters previously set.
         *
         * @return a {@code SubscribeConfig} built with parameters of this {@code SubscribeConfig
         * .Builder}
         */
        @NonNull
        public SubscribeConfig build() {
            return new SubscribeConfig(this);
        }
    }
}

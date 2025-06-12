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

package android.net.wifi;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Additional QoS parameters as defined in Section 9.4.2.316
 * of the IEEE P802.11be/D4.0 Standard.
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
public final class QosCharacteristics implements Parcelable {
    private static final String TAG = "QosCharacteristics";

    /**
     * Maximum size of an MSDU.
     * @hide
     */
    public static final int MAX_MSDU_SIZE = 1 << 0;

    /**
     * Anticipated time and link ID indicating when traffic starts for the associated TID.
     * @hide
     */
    public static final int SERVICE_START_TIME = 1 << 1;


    /**
     * Data rate specified for transport of MSDUs or A-MSDUs belonging to the traffic flow.
     * @hide
     */
    public static final int MEAN_DATA_RATE = 1 << 2;

    /**
     * Maximum burst of the MSDUs or A-MSDUs belonging to the traffic flow.
     * @hide
     */
    public static final int BURST_SIZE = 1 << 3;

    /**
     * Maximum amount of time beyond which the MSDU is not useful.
     * @hide
     */
    public static final int MSDU_LIFETIME = 1 << 4;

    /**
     * MSDU delivery information.
     * @hide
     */
    public static final int MSDU_DELIVERY_INFO = 1 << 5;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {
            MAX_MSDU_SIZE,
            SERVICE_START_TIME,
            MEAN_DATA_RATE,
            BURST_SIZE,
            MSDU_LIFETIME,
            MSDU_DELIVERY_INFO,
    })
    public @interface QosCharacteristicsOptionalField {}

    /**
     * Indicates that 95% of MSDUs are expected to be delivered.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int DELIVERY_RATIO_95 = 0;

    /**
     * Indicates that 96% of MSDUs are expected to be delivered.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int DELIVERY_RATIO_96 = 1;

    /**
     * Indicates that 97% of MSDUs are expected to be delivered.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int DELIVERY_RATIO_97 = 2;

    /**
     * Indicates that 98% of MSDUs are expected to be delivered.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int DELIVERY_RATIO_98 = 3;

    /**
     * Indicates that 99% of MSDUs are expected to be delivered.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int DELIVERY_RATIO_99 = 5;

    /**
     * Indicates that 99.9% of MSDUs are expected to be delivered.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int DELIVERY_RATIO_99_9 = 6;

    /**
     * Indicates that 99.99% of MSDUs are expected to be delivered.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int DELIVERY_RATIO_99_99 = 7;

    /**
     * Indicates that 99.999% of MSDUs are expected to be delivered.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int DELIVERY_RATIO_99_999 = 8;

    /**
     * Indicates that 99.9999% of MSDUs are expected to be delivered.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int DELIVERY_RATIO_99_9999 = 9;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            DELIVERY_RATIO_95,
            DELIVERY_RATIO_96,
            DELIVERY_RATIO_97,
            DELIVERY_RATIO_98,
            DELIVERY_RATIO_99,
            DELIVERY_RATIO_99_9,
            DELIVERY_RATIO_99_99,
            DELIVERY_RATIO_99_999,
            DELIVERY_RATIO_99_9999,
    })
    public @interface DeliveryRatio {}

    private final int mMinServiceIntervalMicros;
    private final int mMaxServiceIntervalMicros;
    private final int mMinDataRateKbps;
    private final int mDelayBoundMicros;
    private final int mOptionalFieldBitmap;
    private final int mMaxMsduSizeOctets;
    private final int mServiceStartTimeMicros;
    private final int mServiceStartTimeLinkId;
    private final int mMeanDataRateKbps;
    private final int mBurstSizeOctets;
    private final int mMsduLifetimeMillis;
    private final @DeliveryRatio int mDeliveryRatio;
    private final int mCountExponent;

    private QosCharacteristics(int minServiceIntervalMicros, int maxServiceIntervalMicros,
            int minDataRateKbps, int delayBoundMicros, int optionalFieldBitmap,
            int maxMsduSizeOctets, int serviceStartTimeMicros, int serviceStartTimeLinkId,
            int meanDataRateKbps, int burstSizeOctets, int msduLifetimeMillis,
            @DeliveryRatio int deliveryRatio, int countExponent) {
        mMinServiceIntervalMicros = minServiceIntervalMicros;
        mMaxServiceIntervalMicros = maxServiceIntervalMicros;
        mMinDataRateKbps = minDataRateKbps;
        mDelayBoundMicros = delayBoundMicros;
        mOptionalFieldBitmap = optionalFieldBitmap;
        mMaxMsduSizeOctets = maxMsduSizeOctets;
        mServiceStartTimeMicros = serviceStartTimeMicros;
        mServiceStartTimeLinkId = serviceStartTimeLinkId;
        mMeanDataRateKbps = meanDataRateKbps;
        mBurstSizeOctets = burstSizeOctets;
        mMsduLifetimeMillis = msduLifetimeMillis;
        mDeliveryRatio = deliveryRatio;
        mCountExponent = countExponent;
    }

    /**
     * Check whether this instance contains the indicated optional field.
     * @hide
     */
    public boolean containsOptionalField(@QosCharacteristicsOptionalField int field) {
        return (mOptionalFieldBitmap & field) != 0;
    }

    /**
     * Check that the value can be cast to an unsigned integer of size |expectedSizeInBits|.
     */
    private static boolean checkSizeInBits(int value, int expectedSizeInBits) {
        int bitmask = (0x1 << expectedSizeInBits) - 1;
        return (bitmask & value) == value;
    }

    /**
     * Check that the value falls within the provided inclusive range.
     */
    private static boolean checkIntRange(int value, int min, int max) {
        return (value >= min) && (value <= max);
    }

    /**
     * Validate the parameters in this instance.
     * @hide
     */
    public boolean validate() {
        if (mMinServiceIntervalMicros <= 0 || mMaxServiceIntervalMicros <= 0
                || mMinDataRateKbps <= 0 || mDelayBoundMicros <= 0) {
            Log.e(TAG, "All mandatory fields must be positive integers");
            return false;
        }
        if (mMinServiceIntervalMicros > mMaxServiceIntervalMicros) {
            Log.e(TAG, "Minimum service interval must be less than or equal to"
                    + " the maximum service interval");
            return false;
        }
        if (containsOptionalField(MAX_MSDU_SIZE)
                && !checkIntRange(mMaxMsduSizeOctets, 1, Short.MAX_VALUE)) {
            Log.e(TAG, "Invalid value provided for maxMsduSize");
            return false;
        }
        if (containsOptionalField(SERVICE_START_TIME)
                && (mServiceStartTimeMicros < 0
                || !checkSizeInBits(mServiceStartTimeLinkId, 4))) {
            Log.e(TAG, "serviceStartTime information is invalid");
            return false;
        }
        if (containsOptionalField(MEAN_DATA_RATE) && mMeanDataRateKbps <= 0) {
            Log.e(TAG, "meanDataRateKbps must be a positive integer");
            return false;
        }
        if (containsOptionalField(BURST_SIZE) && mBurstSizeOctets == 0) {
            Log.e(TAG, "burstSizeOctets must be non-zero");
            return false;
        }
        if (containsOptionalField(MSDU_LIFETIME)
                && !checkIntRange(mMsduLifetimeMillis, 1, Short.MAX_VALUE)) {
            Log.e(TAG, "Invalid value provided for msduLifetimeMillis");
            return false;
        }
        if (containsOptionalField(MSDU_LIFETIME)
                && (mMsduLifetimeMillis * 1000) < mDelayBoundMicros) {
            Log.e(TAG, "MSDU lifetime must be greater than or equal to the delay bound");
            return false;
        }
        if (containsOptionalField(MSDU_DELIVERY_INFO)
                && !checkIntRange(mDeliveryRatio, DELIVERY_RATIO_95, DELIVERY_RATIO_99_9999)) {
            Log.e(TAG, "MSDU delivery ratio must be a valid enum value");
            return false;
        }
        if (containsOptionalField(MSDU_DELIVERY_INFO)
                && !checkIntRange(mCountExponent, 0, 15)) {
            Log.e(TAG, "MSDU count exponent must be between 0 and 15 (inclusive)");
            return false;
        }
        return true;
    }

    /**
     * Get the minimum service interval in microseconds.
     *
     * See {@link Builder#Builder(int, int, int, int)} for more information.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 1, to = Integer.MAX_VALUE)
    public int getMinServiceIntervalMicros() {
        return mMinServiceIntervalMicros;
    }

    /**
     * Get the maximum service interval in microseconds.
     *
     * See {@link Builder#Builder(int, int, int, int)} for more information.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 1, to = Integer.MAX_VALUE)
    public int getMaxServiceIntervalMicros() {
        return mMaxServiceIntervalMicros;
    }

    /**
     * Get the minimum data rate in kilobits per second.
     *
     * See {@link Builder#Builder(int, int, int, int)} for more information.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 1, to = Integer.MAX_VALUE)
    public int getMinDataRateKbps() {
        return mMinDataRateKbps;
    }

    /**
     * Get the delay bound in microseconds.
     *
     * See {@link Builder#Builder(int, int, int, int)} for more information.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 1, to = Integer.MAX_VALUE)
    public int getDelayBoundMicros() {
        return mDelayBoundMicros;
    }

    /**
     * Get the maximum MSDU size in octets. See {@link Builder#setMaxMsduSizeOctets(int)}
     * for more information.
     *
     * @throws IllegalStateException if this field was not set in the Builder.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 1, to = Short.MAX_VALUE)
    public int getMaxMsduSizeOctets() {
        if (!containsOptionalField(MAX_MSDU_SIZE)) {
            throw new IllegalStateException("maxMsduSize was not provided in the builder");
        }
        return mMaxMsduSizeOctets;
    }

    /**
     * Get the service start time in microseconds.
     * See {@link Builder#setServiceStartTimeInfo(int, int)} for more information.
     *
     * @throws IllegalStateException if this field was not set in the Builder.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public int getServiceStartTimeMicros() {
        if (!containsOptionalField(SERVICE_START_TIME)) {
            throw new IllegalStateException("serviceStartTime was not provided in the builder");
        }
        return mServiceStartTimeMicros;
    }

    /**
     * Get the service start time link ID. See {@link Builder#setServiceStartTimeInfo(int, int)}
     * for more information.
     *
     * @throws IllegalStateException if this field was not set in the Builder.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int getServiceStartTimeLinkId() {
        if (!containsOptionalField(SERVICE_START_TIME)) {
            throw new IllegalStateException("serviceStartTime was not provided in the builder");
        }
        return mServiceStartTimeLinkId;
    }

    /**
     * Get the mean data rate in kilobits per second. See {@link Builder#setMeanDataRateKbps(int)}
     * for more information.
     *
     * @throws IllegalStateException if this field was not set in the Builder.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 1, to = Integer.MAX_VALUE)
    public int getMeanDataRateKbps() {
        if (!containsOptionalField(MEAN_DATA_RATE)) {
            throw new IllegalStateException("meanDataRate was not provided in the builder");
        }
        return mMeanDataRateKbps;
    }

    /**
     * Get the burst size in octets. See {@link Builder#setBurstSizeOctets(int)} for
     * more information.
     *
     * @throws IllegalStateException if this field was not set in the Builder.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 1, to = Integer.MAX_VALUE)
    public int getBurstSizeOctets() {
        if (!containsOptionalField(BURST_SIZE)) {
            throw new IllegalStateException("burstSize was not provided in the builder");
        }
        return mBurstSizeOctets;
    }

    /**
     * Get the MSDU lifetime in milliseconds. See {@link Builder#setMsduLifetimeMillis(int)}
     * for more information.
     *
     * @throws IllegalStateException if this field was not set in the Builder.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 1, to = Short.MAX_VALUE)
    public int getMsduLifetimeMillis() {
        if (!containsOptionalField(MSDU_LIFETIME)) {
            throw new IllegalStateException("msduLifetime was not provided in the builder");
        }
        return mMsduLifetimeMillis;
    }

    /**
     * Get the delivery ratio enum. See {@link Builder#setMsduDeliveryInfo(int, int)} for
     * more information.
     *
     * @throws IllegalStateException if this field was not set in the Builder.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public @DeliveryRatio int getDeliveryRatio() {
        if (!containsOptionalField(MSDU_DELIVERY_INFO)) {
            throw new IllegalStateException("msduDeliveryInfo was not provided in the builder");
        }
        return mDeliveryRatio;
    }

    /**
     * Get the count exponent. See {@link Builder#setMsduDeliveryInfo(int, int)} for
     * more information.
     *
     * @throws IllegalStateException if this field was not set in the Builder.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 0, to = 15)
    public int getCountExponent() {
        if (!containsOptionalField(MSDU_DELIVERY_INFO)) {
            throw new IllegalStateException("msduDeliveryInfo was not provided in the builder");
        }
        return mCountExponent;
    }

    @Override
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QosCharacteristics that = (QosCharacteristics) o;
        return mMinServiceIntervalMicros == that.mMinServiceIntervalMicros
                && mMaxServiceIntervalMicros == that.mMaxServiceIntervalMicros
                && mMinDataRateKbps == that.mMinDataRateKbps
                && mDelayBoundMicros == that.mDelayBoundMicros
                && mOptionalFieldBitmap == that.mOptionalFieldBitmap
                && mMaxMsduSizeOctets == that.mMaxMsduSizeOctets
                && mServiceStartTimeMicros == that.mServiceStartTimeMicros
                && mServiceStartTimeLinkId == that.mServiceStartTimeLinkId
                && mMeanDataRateKbps == that.mMeanDataRateKbps
                && mBurstSizeOctets == that.mBurstSizeOctets
                && mMsduLifetimeMillis == that.mMsduLifetimeMillis
                && mDeliveryRatio == that.mDeliveryRatio
                && mCountExponent == that.mCountExponent;
    }

    @Override
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int hashCode() {
        return Objects.hash(mMinServiceIntervalMicros, mMaxServiceIntervalMicros, mMinDataRateKbps,
                mDelayBoundMicros, mOptionalFieldBitmap, mMaxMsduSizeOctets,
                mServiceStartTimeMicros, mServiceStartTimeLinkId, mMeanDataRateKbps,
                mBurstSizeOctets, mMsduLifetimeMillis, mDeliveryRatio, mCountExponent);
    }

    @Override
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("mMinServiceIntervalMicros=").append(mMinServiceIntervalMicros);
        sb.append(", mMaxServiceIntervalMicros=").append(mMaxServiceIntervalMicros);
        sb.append(", mMinDataRateKbps=").append(mMinDataRateKbps);
        sb.append(", mDelayBoundMicros=").append(mDelayBoundMicros);
        sb.append(", mOptionalFieldBitmap=").append(String.format("%02x", mOptionalFieldBitmap));
        if (containsOptionalField(MAX_MSDU_SIZE)) {
            sb.append(", mMaxMsduSizeOctets=").append(mMaxMsduSizeOctets);
        }
        if (containsOptionalField(SERVICE_START_TIME)) {
            sb.append(", mServiceStartTimeMicros=").append(mServiceStartTimeMicros);
            sb.append(", mServiceStartTimeLinkId=");
            sb.append(String.format("%01x", mServiceStartTimeLinkId));
        }
        if (containsOptionalField(MEAN_DATA_RATE)) {
            sb.append(", mMeanDataRateKbps=").append(mMeanDataRateKbps);
        }
        if (containsOptionalField(BURST_SIZE)) {
            sb.append(", mBurstSizeOctets=").append(mBurstSizeOctets);
        }
        if (containsOptionalField(MSDU_LIFETIME)) {
            sb.append(", mMsduLifetimeMillis=").append(mMsduLifetimeMillis);
        }
        if (containsOptionalField(MSDU_DELIVERY_INFO)) {
            sb.append(", mDeliveryRatio=").append(mDeliveryRatio);
            sb.append(", mCountExponent=").append(mCountExponent);
        }
        return sb.append("}").toString();
    }

    @Override
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mMinServiceIntervalMicros);
        dest.writeInt(mMaxServiceIntervalMicros);
        dest.writeInt(mMinDataRateKbps);
        dest.writeInt(mDelayBoundMicros);
        dest.writeInt(mOptionalFieldBitmap);
        dest.writeInt(mMaxMsduSizeOctets);
        dest.writeInt(mServiceStartTimeMicros);
        dest.writeInt(mServiceStartTimeLinkId);
        dest.writeInt(mMeanDataRateKbps);
        dest.writeInt(mBurstSizeOctets);
        dest.writeInt(mMsduLifetimeMillis);
        dest.writeInt(mDeliveryRatio);
        dest.writeInt(mCountExponent);
    }

    /** @hide */
    QosCharacteristics(@NonNull Parcel in) {
        this.mMinServiceIntervalMicros = in.readInt();
        this.mMaxServiceIntervalMicros = in.readInt();
        this.mMinDataRateKbps = in.readInt();
        this.mDelayBoundMicros = in.readInt();
        this.mOptionalFieldBitmap = in.readInt();
        this.mMaxMsduSizeOctets = in.readInt();
        this.mServiceStartTimeMicros = in.readInt();
        this.mServiceStartTimeLinkId = in.readInt();
        this.mMeanDataRateKbps = in.readInt();
        this.mBurstSizeOctets = in.readInt();
        this.mMsduLifetimeMillis = in.readInt();
        this.mDeliveryRatio = in.readInt();
        this.mCountExponent = in.readInt();
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final @NonNull Parcelable.Creator<QosCharacteristics> CREATOR =
            new Parcelable.Creator<QosCharacteristics>() {
                @Override
                public QosCharacteristics createFromParcel(Parcel in) {
                    return new QosCharacteristics(in);
                }

                @Override
                public QosCharacteristics[] newArray(int size) {
                    return new QosCharacteristics[size];
                }
            };

    /**
     * Builder for {@link QosCharacteristics}.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final class Builder {
        // Mandatory fields
        private final int mMinServiceIntervalMicros;
        private final int mMaxServiceIntervalMicros;
        private final int mMinDataRateKbps;
        private final int mDelayBoundMicros;

        // Optional fields
        private int mOptionalFieldBitmap;
        private int mMaxMsduSizeOctets;
        private int mServiceStartTimeMicros;
        private int mServiceStartTimeLinkId;
        private int mMeanDataRateKbps;
        private int mBurstSizeOctets;
        private int mMsduLifetimeMillis;
        private @DeliveryRatio int mDeliveryRatio;
        private int mCountExponent;

        /**
         * Constructor for {@link Builder}.
         *
         * @param minServiceIntervalMicros Positive integer specifying the minimum interval (in
         *                             microseconds) between the start of two consecutive service
         *                             periods (SPs) that are allocated for frame exchanges.
         * @param maxServiceIntervalMicros Positive integer specifying the maximum interval (in
         *                             microseconds) between the start of two consecutive SPs that
         *                             are allocated for frame exchanges.
         * @param minDataRateKbps Positive integer specifying the lowest data rate (in kilobits/sec)
         *                        for the transport of MSDUs or A-MSDUs belonging to the traffic
         *                        flow.
         * @param delayBoundMicros Positive integer specifying the maximum amount of time (in
         *                     microseconds) targeted to transport an MSDU or A-MSDU belonging to
         *                     the traffic flow.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public Builder(@IntRange(from = 1, to = Integer.MAX_VALUE) int minServiceIntervalMicros,
                @IntRange(from = 1, to = Integer.MAX_VALUE) int maxServiceIntervalMicros,
                @IntRange(from = 1, to = Integer.MAX_VALUE) int minDataRateKbps,
                @IntRange(from = 1, to = Integer.MAX_VALUE) int delayBoundMicros) {
            mMinServiceIntervalMicros = minServiceIntervalMicros;
            mMaxServiceIntervalMicros = maxServiceIntervalMicros;
            mMinDataRateKbps = minDataRateKbps;
            mDelayBoundMicros = delayBoundMicros;
        }

        /**
         * Set the maximum MSDU size in octets.
         *
         * @param maxMsduSizeOctets Positive integer specifying the maximum size (in octets)
         *                          of an MSDU belonging to the traffic flow.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public @NonNull Builder setMaxMsduSizeOctets(
                @IntRange(from = 1, to = Short.MAX_VALUE) int maxMsduSizeOctets) {
            mOptionalFieldBitmap |= MAX_MSDU_SIZE;
            mMaxMsduSizeOctets = maxMsduSizeOctets;
            return this;
        }

        /**
         * Set the service start time information.
         *
         * @param serviceStartTimeMicros Integer specifying the anticipated time (in microseconds)
         *                               when the traffic starts for the associated TID.
         * @param serviceStartTimeLinkId Bitmap in which the four LSBs indicate the link identifier
         *                               that corresponds to the link for which the TSF timer is
         *                               used to indicate the Service Start Time. No other bits
         *                               should be used.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public @NonNull Builder setServiceStartTimeInfo(
                @IntRange(from = 0, to = Integer.MAX_VALUE) int serviceStartTimeMicros,
                int serviceStartTimeLinkId) {
            mOptionalFieldBitmap |= SERVICE_START_TIME;
            mServiceStartTimeMicros = serviceStartTimeMicros;
            mServiceStartTimeLinkId = serviceStartTimeLinkId;
            return this;
        }

        /**
         * Set the mean data rate in kilobits per second.
         *
         * @param meanDataRateKbps Positive integer indicating the data rate specified (in
         *                         kilobits/sec) for transport of MSDUs or A-MSDUs belonging to the
         *                         traffic flow.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public @NonNull Builder setMeanDataRateKbps(
                @IntRange(from = 1, to = Integer.MAX_VALUE) int meanDataRateKbps) {
            mOptionalFieldBitmap |= MEAN_DATA_RATE;
            mMeanDataRateKbps = meanDataRateKbps;
            return this;
        }

        /**
         * Set the burst size in octets.
         *
         * @param burstSizeOctets Positive integer specifying the maximum burst (in octets) of
         *                        the MSDUs or A-MSDUs belonging to the traffic flow that arrive at
         *                        the MAC SAP within any time duration equal to the value specified
         *                        in the |delayBound| field.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public @NonNull Builder setBurstSizeOctets(
                @IntRange(from = 1, to = Integer.MAX_VALUE) int burstSizeOctets) {
            mOptionalFieldBitmap |= BURST_SIZE;
            mBurstSizeOctets = burstSizeOctets;
            return this;
        }

        /**
         * Set the MSDU lifetime in milliseconds.
         *
         * @param msduLifetimeMillis Positive integer specifying the maximum amount of time (in
         *                       milliseconds) since the arrival of the MSDU at the MAC data service
         *                       interface beyond which the MSDU is not useful even if received by
         *                       the receiver. The amount of time specified in this field is larger
         *                       than or equal to the amount of time specified in the |delayBound|
         *                       field, if present.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public @NonNull Builder setMsduLifetimeMillis(
                @IntRange(from = 1, to = Short.MAX_VALUE) int msduLifetimeMillis) {
            mOptionalFieldBitmap |= MSDU_LIFETIME;
            mMsduLifetimeMillis = msduLifetimeMillis;
            return this;
        }

        /**
         * Set the MSDU delivery information.
         *
         * @param deliveryRatio Enum indicating the percentage of the MSDUs that are expected to be
         *                      delivered successfully.
         * @param countExponent Exponent from which the number of incoming MSDUs is computed. The
         *                      number of incoming MSDUs is 10^|countExponent|, and is used to
         *                      determine the MSDU delivery ratio. Must be a number between
         *                      0 and 15 (inclusive).
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public @NonNull Builder setMsduDeliveryInfo(
                @DeliveryRatio int deliveryRatio, @IntRange(from = 0, to = 15) int countExponent) {
            mOptionalFieldBitmap |= MSDU_DELIVERY_INFO;
            mDeliveryRatio = deliveryRatio;
            mCountExponent = countExponent;
            return this;
        }

        /**
         * Construct a QosCharacteristics object with the specified parameters.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public @NonNull QosCharacteristics build() {
            QosCharacteristics qosCharacteristics = new QosCharacteristics(
                    mMinServiceIntervalMicros, mMaxServiceIntervalMicros,
                    mMinDataRateKbps, mDelayBoundMicros, mOptionalFieldBitmap,
                    mMaxMsduSizeOctets, mServiceStartTimeMicros, mServiceStartTimeLinkId,
                    mMeanDataRateKbps, mBurstSizeOctets, mMsduLifetimeMillis,
                    mDeliveryRatio, mCountExponent);
            if (!qosCharacteristics.validate()) {
                throw new IllegalArgumentException("Provided parameters are invalid");
            }
            return qosCharacteristics;
        }
    }
}

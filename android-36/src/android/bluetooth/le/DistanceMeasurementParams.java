/*
 * Copyright 2022 The Android Open Source Project
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

package android.bluetooth.le;

import static java.util.Objects.requireNonNull;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.DistanceMeasurementMethod.DistanceMeasurementMethodId;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The {@link DistanceMeasurementParams} provide a way to adjust distance measurement preferences.
 * Use {@link DistanceMeasurementParams.Builder} to create an instance of this class.
 *
 * @hide
 */
@SystemApi
public final class DistanceMeasurementParams implements Parcelable {

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {REPORT_FREQUENCY_LOW, REPORT_FREQUENCY_MEDIUM, REPORT_FREQUENCY_HIGH})
    @interface ReportFrequency {}

    /**
     * Perform distance measurement in low frequency. This is the default frequency as it consumes
     * the least power.
     *
     * @hide
     */
    @SystemApi public static final int REPORT_FREQUENCY_LOW = 0;

    /**
     * Perform distance measurement in medium frequency. Provides a good trade-off between report
     * frequency and power consumption.
     *
     * @hide
     */
    @SystemApi public static final int REPORT_FREQUENCY_MEDIUM = 1;

    /**
     * Perform distance measurement in high frequency. It's recommended to only use this mode when
     * the application is running in the foreground.
     *
     * @hide
     */
    @SystemApi public static final int REPORT_FREQUENCY_HIGH = 2;

    private static final int REPORT_DURATION_MAX = Integer.MAX_VALUE;
    private static final int REPORT_DURATION_DEFAULT = REPORT_DURATION_MAX;

    private final BluetoothDevice mDevice;
    private final int mDuration;
    private final int mFrequency;
    private final int mMethodId;
    private final ChannelSoundingParams mChannelSoundingParams;

    /** @hide */
    public DistanceMeasurementParams(
            @NonNull BluetoothDevice device,
            int duration,
            int frequency,
            int methodId,
            ChannelSoundingParams channelSoundingParams) {
        mDevice = requireNonNull(device);
        mDuration = duration;
        mFrequency = frequency;
        mMethodId = methodId;
        mChannelSoundingParams = channelSoundingParams;
    }

    /**
     * Returns device of this DistanceMeasurementParams.
     *
     * @hide
     */
    @SystemApi
    public @NonNull BluetoothDevice getDevice() {
        return mDevice;
    }

    /**
     * Returns duration in seconds of this DistanceMeasurementParams. Once the distance measurement
     * successfully started, the Bluetooth process will keep reporting the measurement result until
     * this time has been reached or the session is explicitly stopped with {@link
     * DistanceMeasurementSession#stopSession}
     *
     * @hide
     */
    @SystemApi
    public @IntRange(from = 0) int getDurationSeconds() {
        return mDuration;
    }

    /**
     * Returns frequency of this DistanceMeasurementParams. The Bluetooth process uses this value to
     * determine report frequency of the measurement result.
     *
     * @hide
     */
    @SystemApi
    public @ReportFrequency int getFrequency() {
        return mFrequency;
    }

    /**
     * Returns method id of this DistanceMeasurementParams.
     *
     * @hide
     */
    @SystemApi
    public @DistanceMeasurementMethodId int getMethodId() {
        return mMethodId;
    }

    /**
     * Returns {@link ChannelSoundingParams} of this DistanceMeasurementParams.
     *
     * @hide
     */
    @SystemApi
    public @Nullable ChannelSoundingParams getChannelSoundingParams() {
        return mChannelSoundingParams;
    }

    /**
     * Get the default duration in seconds of the parameter.
     *
     * @hide
     */
    @SystemApi
    public static int getDefaultDurationSeconds() {
        return REPORT_DURATION_DEFAULT;
    }

    /**
     * Get the maximum duration in seconds that can be set for the parameter.
     *
     * @hide
     */
    @SystemApi
    public static int getMaxDurationSeconds() {
        return REPORT_DURATION_MAX;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        mDevice.writeToParcel(out, flags);
        out.writeInt(mDuration);
        out.writeInt(mFrequency);
        out.writeInt(mMethodId);

        out.writeInt(mChannelSoundingParams == null ? 0 : 1);
        if (mChannelSoundingParams != null) {
            mChannelSoundingParams.writeToParcel(out, flags);
        }
    }

    /** A {@link Parcelable.Creator} to create {@link DistanceMeasurementParams} from parcel. */
    public static final @NonNull Parcelable.Creator<DistanceMeasurementParams> CREATOR =
            new Parcelable.Creator<DistanceMeasurementParams>() {
                @Override
                public @NonNull DistanceMeasurementParams createFromParcel(@NonNull Parcel in) {
                    Builder builder = new Builder(BluetoothDevice.CREATOR.createFromParcel(in));
                    builder.setDurationSeconds(in.readInt());
                    builder.setFrequency(in.readInt());
                    builder.setMethodId(in.readInt());
                    if (in.readInt() == 1) {
                        builder.setChannelSoundingParams(
                                ChannelSoundingParams.CREATOR.createFromParcel(in));
                    }
                    return builder.build();
                }

                @Override
                public @NonNull DistanceMeasurementParams[] newArray(int size) {
                    return new DistanceMeasurementParams[size];
                }
            };

    /**
     * Builder for {@link DistanceMeasurementParams}.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private BluetoothDevice mDevice = null;
        private int mDuration = REPORT_DURATION_DEFAULT;
        private int mFrequency = REPORT_FREQUENCY_LOW;
        private int mMethodId = DistanceMeasurementMethod.DISTANCE_MEASUREMENT_METHOD_RSSI;
        private ChannelSoundingParams mChannelSoundingParams = null;

        /**
         * Constructor of the Builder.
         *
         * @param device the remote device for the distance measurement
         */
        public Builder(@NonNull BluetoothDevice device) {
            mDevice = requireNonNull(device);
        }

        /**
         * Set duration in seconds for the DistanceMeasurementParams. Once the distance measurement
         * successfully started, the Bluetooth process will keep reporting the measurement result
         * until this time has been reached or the session is explicitly stopped with {@link
         * DistanceMeasurementSession#stopSession}.
         *
         * @param duration duration in seconds of this DistanceMeasurementParams
         * @return the same Builder instance
         * @throws IllegalArgumentException if duration greater than {@link
         *     DistanceMeasurementParams#getMaxDurationSeconds()} or less than zero.
         * @hide
         */
        @SystemApi
        public @NonNull Builder setDurationSeconds(@IntRange(from = 0) int duration) {
            if (duration < 0 || duration > getMaxDurationSeconds()) {
                throw new IllegalArgumentException("illegal duration " + duration);
            }
            mDuration = duration;
            return this;
        }

        /**
         * Set frequency for the DistanceMeasurementParams. The Bluetooth process uses this value to
         * determine report frequency of the measurement result.
         *
         * @param frequency frequency of this DistanceMeasurementParams
         * @return the same Builder instance
         * @hide
         */
        @SystemApi
        public @NonNull Builder setFrequency(@ReportFrequency int frequency) {
            switch (frequency) {
                case REPORT_FREQUENCY_LOW:
                case REPORT_FREQUENCY_MEDIUM:
                case REPORT_FREQUENCY_HIGH:
                    mFrequency = frequency;
                    break;
                default:
                    throw new IllegalArgumentException("unknown frequency " + frequency);
            }
            return this;
        }

        /**
         * Set method id for the DistanceMeasurementParams.
         *
         * @param methodId method id of this DistanceMeasurementParams
         * @return the same Builder instance
         * @hide
         */
        @SystemApi
        public @NonNull Builder setMethodId(@DistanceMeasurementMethodId int methodId) {
            switch (methodId) {
                case DistanceMeasurementMethod.DISTANCE_MEASUREMENT_METHOD_AUTO:
                case DistanceMeasurementMethod.DISTANCE_MEASUREMENT_METHOD_RSSI:
                case DistanceMeasurementMethod.DISTANCE_MEASUREMENT_METHOD_CHANNEL_SOUNDING:
                    mMethodId = methodId;
                    break;
                default:
                    throw new IllegalArgumentException("unknown method id " + methodId);
            }
            return this;
        }

        /**
         * Set {@link ChannelSoundingParams} for the DistanceMeasurementParams.
         *
         * @param channelSoundingParams parameters for Channel Sounding
         * @return the same Builder instance
         * @hide
         */
        @SystemApi
        public @NonNull Builder setChannelSoundingParams(
                @NonNull ChannelSoundingParams channelSoundingParams) {
            mChannelSoundingParams = channelSoundingParams;
            return this;
        }

        /**
         * Build the {@link DistanceMeasurementParams} object.
         *
         * @hide
         */
        @SystemApi
        public @NonNull DistanceMeasurementParams build() {
            return new DistanceMeasurementParams(
                    mDevice, mDuration, mFrequency, mMethodId, mChannelSoundingParams);
        }
    }
}

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

package android.ranging;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.ranging.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a ranging measurement.
 *
 * <p>This class provides a measurement result, such as a distance or angle.
 *
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class RangingMeasurement implements Parcelable {

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            CONFIDENCE_LOW,
            CONFIDENCE_MEDIUM,
            CONFIDENCE_HIGH
    })
    public @interface Confidence {
    }

    /** Ranging measurement with low confidence.
     *
     */
    public static final int CONFIDENCE_LOW = 0;
    /** Ranging measurement with medium confidence.
     *
     */
    public static final int CONFIDENCE_MEDIUM = 1;
    /** Ranging measurement with high confidence.
     *
     */
    public static final int CONFIDENCE_HIGH = 2;
    private final double mMeasurement;
    @Confidence
    private final int mConfidence;

    private RangingMeasurement(Builder builder) {
        if (Double.isNaN(builder.mMeasurement)) {
            throw new IllegalArgumentException("Missing required parameter: measurement");
        }
        mMeasurement = builder.mMeasurement;
        mConfidence = builder.mConfidence;
    }

    private RangingMeasurement(@NonNull Parcel in) {
        mMeasurement = in.readDouble();
        mConfidence = in.readInt();
    }

    @NonNull
    public static final Creator<RangingMeasurement> CREATOR = new Creator<RangingMeasurement>() {
        @Override
        public RangingMeasurement createFromParcel(Parcel in) {
            return new RangingMeasurement(in);
        }

        @Override
        public RangingMeasurement[] newArray(int size) {
            return new RangingMeasurement[size];
        }
    };

    /**
     * Returns the measurement value.
     *
     * @return The measurement, such as a distance in meters or an angle in degrees.
     */
    public double getMeasurement() {
        return mMeasurement;
    }

    /**
     * Returns the confidence score for this measurement.
     *
     */
    @Confidence
    public int getConfidence() {
        return mConfidence;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeDouble(mMeasurement);
        dest.writeInt(mConfidence);
    }

    /**
     * A builder class for creating instances of {@link RangingMeasurement}.
     *
     * @hide
     */
    public static final class Builder {
        private double mMeasurement = Double.NaN;
        @Confidence private int mConfidence = CONFIDENCE_MEDIUM;

        /**
         * Sets the measurement value.
         *
         * @param measurement The measurement value, such as a distance in meters or angle in
         *                    degrees.
         * @return This {@link Builder} instance.
         */
        @NonNull
        public Builder setMeasurement(double measurement) {
            mMeasurement = measurement;
            return this;
        }

        /**
         * Sets the confidence score for the measurement.
         *
         * @param confidence indicating confidence in the measurement.
         * @return This {@link Builder} instance.
         */
        @NonNull
        public Builder setConfidence(@Confidence int confidence) {
            mConfidence = confidence;
            return this;
        }

        /**
         * Builds a new {@link RangingMeasurement} instance with the specified parameters.
         *
         * @return A new {@link RangingMeasurement} object.
         */
        @NonNull
        public RangingMeasurement build() {
            return new RangingMeasurement(this);
        }
    }

    @Override
    public String toString() {
        return "RangingMeasurement{ "
                + "mMeasurement="
                + mMeasurement
                + ", mConfidence="
                + mConfidence
                + " }";
    }
}

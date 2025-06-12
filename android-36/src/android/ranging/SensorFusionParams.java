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
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.ranging.flags.Flags;

/**
 * Represents the parameters for sensor fusion in ranging operations. Uses IMU sensors to correct
 * HW AOA measurements. Adds a moving average filter for distance measurements to remove outliers.
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class SensorFusionParams implements Parcelable {
    private final boolean mSensorFusionEnabled;

    private SensorFusionParams(Builder builder) {
        mSensorFusionEnabled = builder.mSensorFusionEnabled;
    }

    private SensorFusionParams(@NonNull Parcel in) {
        mSensorFusionEnabled = in.readBoolean();
    }

    /**
     * Gets whether sensor fusion was requested.
     *
     * @return true if sensor fusion is enabled; false otherwise.
     */
    public boolean isSensorFusionEnabled() {
        return mSensorFusionEnabled;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBoolean(mSensorFusionEnabled);
    }

    @NonNull
    public static final Creator<SensorFusionParams> CREATOR = new Creator<>() {
        @Override
        public SensorFusionParams createFromParcel(Parcel in) {
            return new SensorFusionParams(in);
        }

        @Override
        public SensorFusionParams[] newArray(int size) {
            return new SensorFusionParams[size];
        }
    };

    /**
     * Builder class for creating instances of {@link SensorFusionParams}.
     */
    public static final class Builder {
        private boolean mSensorFusionEnabled = true;

        /**
         * Builds a new instance of {@link SensorFusionParams}.
         *
         * @return a new {@link SensorFusionParams} instance created using the current state of
         * the builder.
         */
        @NonNull
        public SensorFusionParams build() {
            return new SensorFusionParams(this);
        }

        /**
         * Sets whether to use sensor fusion.
         * <p> defaults to true.
         *
         * @param sensorFusionEnabled true to enable sensor fusion; false to disable it.
         * @return this Builder instance for method chaining.
         */
        @NonNull
        public Builder setSensorFusionEnabled(boolean sensorFusionEnabled) {
            mSensorFusionEnabled = sensorFusionEnabled;
            return this;
        }
    }

    /** @hide */
    @Override
    public String toString() {
        return "SensorFusionParams{ "
                + "mSensorFusionEnabled="
                + mSensorFusionEnabled
                + " }";
    }
}

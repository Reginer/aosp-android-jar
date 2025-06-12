/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.companion.virtual.sensor;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.companion.virtualdevice.flags.Flags;
import android.hardware.SensorAdditionalInfo;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * An additional information frame for a {@link VirtualSensor}, which is reported through listener
 * callback {@link android.hardware.SensorEventCallback#onSensorAdditionalInfo}.
 *
 * @see SensorAdditionalInfo
 * @see VirtualSensorConfig.Builder#setAdditionalInfoSupported(boolean)
 * @hide
 */
@FlaggedApi(Flags.FLAG_VIRTUAL_SENSOR_ADDITIONAL_INFO)
@SystemApi
public final class VirtualSensorAdditionalInfo implements Parcelable {

    private final int mType;
    @NonNull
    private final List<float[]> mValues;

    /** @hide */
    @IntDef(prefix = "TYPE_", value = {
            SensorAdditionalInfo.TYPE_UNTRACKED_DELAY,
            SensorAdditionalInfo.TYPE_INTERNAL_TEMPERATURE,
            SensorAdditionalInfo.TYPE_VEC3_CALIBRATION,
            SensorAdditionalInfo.TYPE_SENSOR_PLACEMENT,
            SensorAdditionalInfo.TYPE_SAMPLING,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}

    private VirtualSensorAdditionalInfo(int type, @NonNull List<float[]> values) {
        mType = type;
        mValues = values;
    }

    private VirtualSensorAdditionalInfo(@NonNull Parcel parcel) {
        mType = parcel.readInt();
        final int valuesLength = parcel.readInt();
        mValues = new ArrayList<>(valuesLength);
        for (int i = 0; i < valuesLength; ++i) {
            mValues.add(parcel.createFloatArray());
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int parcelableFlags) {
        parcel.writeInt(mType);
        parcel.writeInt(mValues.size());
        for (int i = 0; i < mValues.size(); ++i) {
            parcel.writeFloatArray(mValues.get(i));
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Returns the type of this information frame.
     *
     * @see SensorAdditionalInfo#type
     */
    @VirtualSensorAdditionalInfo.Type
    public int getType() {
        return mType;
    }

    /**
     * Returns the float values of this information frame, if any.
     *
     * @see SensorAdditionalInfo#floatValues
     */
    @NonNull
    public List<float[]> getValues() {
        return mValues;
    }

    /**
     * Builder for {@link VirtualSensorAdditionalInfo}.
     */
    public static final class Builder {

        @VirtualSensorAdditionalInfo.Type
        private final int mType;
        @NonNull
        private final ArrayList<float[]> mValues = new ArrayList<>();

        /** Payload size for {@link SensorAdditionalInfo#TYPE_SAMPLING} */
        private static final int TYPE_SAMPLING_PLAYLOAD_SIZE = 2;

        /** Payload size for {@link SensorAdditionalInfo#TYPE_UNTRACKED_DELAY} */
        private static final int TYPE_UNTRACKED_DELAY_PAYLOAD_SIZE = 2;

        /** Payload size for {@link SensorAdditionalInfo#TYPE_INTERNAL_TEMPERATURE} */
        private static final int TYPE_INTERNAL_TEMPERATURE_PLAYLOAD_SIZE = 1;

        /** Payload size for {@link SensorAdditionalInfo#TYPE_VEC3_CALIBRATION} */
        private static final int TYPE_VEC3_CALIBRATION_PAYLOAD_SIZE = 12;

        /** Payload size for {@link SensorAdditionalInfo#TYPE_SENSOR_PLACEMENT} */
        private static final int TYPE_SENSOR_PLACEMENT_PAYLOAD_SIZE = 12;

        /**
         * Creates a new builder.
         *
         * @param type type of this additional info frame.
         * @see SensorAdditionalInfo
         */
        public Builder(@VirtualSensorAdditionalInfo.Type int type) {
            switch (type) {
                case SensorAdditionalInfo.TYPE_UNTRACKED_DELAY:
                case SensorAdditionalInfo.TYPE_SAMPLING:
                case SensorAdditionalInfo.TYPE_INTERNAL_TEMPERATURE:
                case SensorAdditionalInfo.TYPE_VEC3_CALIBRATION:
                case SensorAdditionalInfo.TYPE_SENSOR_PLACEMENT:
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type " + type);
            }
            mType = type;
        }

        /**
         * Additional info payload data represented in float values.
         *
         * @param values the float values of this additional info frame.
         * @throws IllegalArgumentException if the payload size doesn't match the expectation
         *   for the given type, as documented in {@link SensorAdditionalInfo}.
         * @see SensorAdditionalInfo#floatValues
         */
        @NonNull
        public Builder addValues(@NonNull float[] values) {
            if (mValues.isEmpty()) {
                switch (mType) {
                    case SensorAdditionalInfo.TYPE_UNTRACKED_DELAY:
                        assertValuesLength(values, TYPE_UNTRACKED_DELAY_PAYLOAD_SIZE);
                        break;
                    case SensorAdditionalInfo.TYPE_SAMPLING:
                        assertValuesLength(values, TYPE_SAMPLING_PLAYLOAD_SIZE);
                        break;
                    case SensorAdditionalInfo.TYPE_INTERNAL_TEMPERATURE:
                        assertValuesLength(values, TYPE_INTERNAL_TEMPERATURE_PLAYLOAD_SIZE);
                        break;
                    case SensorAdditionalInfo.TYPE_VEC3_CALIBRATION:
                        assertValuesLength(values, TYPE_VEC3_CALIBRATION_PAYLOAD_SIZE);
                        break;
                    case SensorAdditionalInfo.TYPE_SENSOR_PLACEMENT:
                        assertValuesLength(values, TYPE_SENSOR_PLACEMENT_PAYLOAD_SIZE);
                        break;
                }
            } else if (values.length != mValues.getFirst().length) {
                throw new IllegalArgumentException("All payload values must have the same length");
            }

            mValues.add(values);
            return this;
        }

        private void assertValuesLength(float[] values, int expected) {
            if (values.length != expected) {
                throw new IllegalArgumentException(
                        "Payload values must have size " + expected + " for type " + mType);
            }
        }

        /**
         * Creates a new {@link VirtualSensorAdditionalInfo}.
         *
         * @throws IllegalArgumentException if the payload doesn't match the expectation for the
         *   given type, as documented in {@link SensorAdditionalInfo}.
         */
        @NonNull
        public VirtualSensorAdditionalInfo build() {
            if (mValues.isEmpty()) {
                throw new IllegalArgumentException("Payload is required");
            }
            return new VirtualSensorAdditionalInfo(mType, mValues);
        }
    }

    public static final @NonNull Creator<VirtualSensorAdditionalInfo> CREATOR =
            new Creator<>() {
                public VirtualSensorAdditionalInfo createFromParcel(Parcel source) {
                    return new VirtualSensorAdditionalInfo(source);
                }

                public VirtualSensorAdditionalInfo[] newArray(int size) {
                    return new VirtualSensorAdditionalInfo[size];
                }
            };
}

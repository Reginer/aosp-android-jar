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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Method of distance measurement. A list of this class will be returned by
 * {@link DistanceMeasurementManager#getSupportedMethods()} to indicate the supported methods and
 * their capability about angle measurement.
 *
 * @hide
 */
@SystemApi
public final class DistanceMeasurementMethod implements Parcelable {

    private final int mId;
    private final boolean mIsAzimuthAngleSupported;
    private final boolean mIsAltitudeAngleSupported;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            DISTANCE_MEASUREMENT_METHOD_AUTO,
            DISTANCE_MEASUREMENT_METHOD_RSSI})
    @interface DistanceMeasurementMethodId  {}

    /**
     * Choose method automatically, Bluetooth will use the most accurate method that local
     * device supported to measurement distance.
     *
     * @hide
     */
    @SystemApi
    public static final int DISTANCE_MEASUREMENT_METHOD_AUTO = 0;

    /**
     * Use remote RSSI and transmit power to measure the distance.
     *
     * @hide
     */
    @SystemApi
    public static final int DISTANCE_MEASUREMENT_METHOD_RSSI = 1;

    private DistanceMeasurementMethod(int id, boolean isAzimuthAngleSupported,
            boolean isAltitudeAngleSupported) {
        mId = id;
        mIsAzimuthAngleSupported = isAzimuthAngleSupported;
        mIsAltitudeAngleSupported = isAltitudeAngleSupported;
    }

    /**
     * Id of the method used for {@link DistanceMeasurementParams.Builder#setMethod(int)}
     *
     * @return id of the method
     *
     * @hide
     */
    @SystemApi
    public @DistanceMeasurementMethodId double getId() {
        return mId;
    }

    /**
     * Checks whether the azimuth angle is supported for this method.
     *
     * @return true if azimuth angle is supported, false otherwise
     *
     * @hide
     */
    @SystemApi
    public boolean isAzimuthAngleSupported() {
        return mIsAzimuthAngleSupported;
    }

    /**
     * Checks whether the altitude angle is supported for this method.
     *
     * @return true if altitude angle is supported, false otherwise
     *
     * @hide
     */
    @SystemApi
    public boolean isAltitudeAngleSupported() {
        return mIsAltitudeAngleSupported;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mId);
        out.writeBoolean(mIsAzimuthAngleSupported);
        out.writeBoolean(mIsAltitudeAngleSupported);
    }

    /** @hide **/
    @Override
    public String toString() {
        return "DistanceMeasurementMethod["
                + "id: " + mId
                + ", isAzimuthAngleSupported: " + mIsAzimuthAngleSupported
                + ", isAltitudeAngleSupported: " + mIsAltitudeAngleSupported
                + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        if (!(o instanceof DistanceMeasurementMethod)) return false;

        final DistanceMeasurementMethod u = (DistanceMeasurementMethod) o;

        if (mId != u.getId()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId);
    }

    /**
     * A {@link Parcelable.Creator} to create {@link DistanceMeasurementMethod} from parcel.
     *
     */
    public static final @NonNull Parcelable.Creator<DistanceMeasurementMethod> CREATOR =
            new Parcelable.Creator<DistanceMeasurementMethod>() {
                @Override
                public @NonNull DistanceMeasurementMethod createFromParcel(@NonNull Parcel in) {
                    return new Builder(in.readInt()).setAzimuthAngleSupported(in.readBoolean())
                            .setAltitudeAngleSupported(in.readBoolean()).build();
                }

                @Override
                public @NonNull DistanceMeasurementMethod[] newArray(int size) {
                    return new DistanceMeasurementMethod[size];
                }
        };

    /**
     * Builder for {@link DistanceMeasurementMethod}.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private int mId;
        private boolean mIsAzimuthAngleSupported = false;
        private boolean mIsAltitudeAngleSupported = false;

        /**
         * Constructor of the Builder.
         *
         * @param id id of the method
         */
        public Builder(@DistanceMeasurementMethodId int id) {
            switch (id) {
                case DISTANCE_MEASUREMENT_METHOD_AUTO:
                case DISTANCE_MEASUREMENT_METHOD_RSSI:
                    mId = id;
                    break;
                default:
                    throw new IllegalArgumentException("unknown method id " + id);
            }
        }

         /**
         * Set if azimuth angle supported or not.
         *
         * @param supported {@code true} if azimuth angle supported, {@code false} otherwise
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setAzimuthAngleSupported(boolean supported) {
            mIsAzimuthAngleSupported = supported;
            return this;
        }

        /**
         * Set if altitude angle supported or not.
         *
         * @param supported {@code true} if altitude angle supported, {@code false} otherwise
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setAltitudeAngleSupported(boolean supported) {
            mIsAltitudeAngleSupported = supported;
            return this;
        }

        /**
         * Builds the {@link DistanceMeasurementMethod} object.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public DistanceMeasurementMethod build() {
            return new DistanceMeasurementMethod(mId, mIsAzimuthAngleSupported,
                    mIsAltitudeAngleSupported);
        }
    }
}

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

import android.annotation.FloatRange;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Result of distance measurement.
 *
 * @hide
 */
@SystemApi
public final class DistanceMeasurementResult implements Parcelable {

    private final double mMeters;
    private final double mErrorMeters;
    private final double mAzimuthAngle;
    private final double mErrorAzimuthAngle;
    private final double mAltitudeAngle;
    private final double mErrorAltitudeAngle;

    private DistanceMeasurementResult(double meters, double errorMeters, double azimuthAngle,
            double errorAzimuthAngle, double altitudeAngle, double errorAltitudeAngle) {
        mMeters = meters;
        mErrorMeters = errorMeters;
        mAzimuthAngle = azimuthAngle;
        mErrorAzimuthAngle = errorAzimuthAngle;
        mAltitudeAngle = altitudeAngle;
        mErrorAltitudeAngle = errorAltitudeAngle;
    }

    /**
     * Distance measurement in meters.
     *
     * @return distance in meters
     *
     * @hide
     */
    @SystemApi
    public double getResultMeters() {
        return mMeters;
    }

    /**
     * Error of distance measurement in meters.
     * <p>Must be positive.
     *
     * @return error of distance measurement in meters
     *
     * @hide
     */
    @SystemApi
    @FloatRange(from = 0.0)
    public double getErrorMeters() {
        return mErrorMeters;
    }

    /**
     * Azimuth Angle measurement in degrees.
     *
     * Azimuth of remote device in horizontal coordinate system, this measured from azimuth north
     * and increasing eastward. When the remote device in azimuth north, this angle is 0, whe the
     * remote device in azimuth south, this angle is 180.
     *
     * <p>See: <a href="https://en.wikipedia.org/wiki/Horizontal_coordinate_system">Horizontal
     *  coordinate system</a>for the details
     *
     * <p>On an Android device, azimuth north is defined as the angle perpendicular away from the
     * back of the device when holding it in portrait mode upright.
     *
     * <p>The Azimuth north is defined as the direction in which the top edge of the device is
     * facing when it is placed flat.
     *
     * @return azimuth angle in degrees or Double.NaN if not available
     *
     * @hide
     */
    @SystemApi
    @FloatRange(from = 0.0, to = 360.0)
    public double getAzimuthAngle() {
        return mAzimuthAngle;
    }

    /**
     * Error of azimuth angle measurement in degrees.
     *
     * <p>Must be a positive value.
     *
     * @return azimuth angle measurement error in degrees or Double.NaN if not available
     *
     * @hide
     */
    @SystemApi
    public double getErrorAzimuthAngle() {
        return mErrorAzimuthAngle;
    }

    /**
     * Altitude Angle measurement in degrees.
     *
     * <p>Altitude of remote device in horizontal coordinate system, this is the angle between the
     * remote device and the top edge of local device. When local device is placed flat, the angle
     * of the zenith is 90, the angle of the nadir is -90.
     *
     * <p>See: https://en.wikipedia.org/wiki/Horizontal_coordinate_system
     *
     * @return altitude angle in degrees or Double.NaN if not available
     *
     * @hide
     */
    @SystemApi
    @FloatRange(from = -90.0, to = 90.0)
    public double getAltitudeAngle() {
        return mAltitudeAngle;
    }

    /**
     * Error of altitude angle measurement in degrees.
     *
     * <p>Must be a positive value.
     *
     * @return altitude angle measurement error in degrees or Double.NaN if not available
     *
     * @hide
     */
    @SystemApi
    public double getErrorAltitudeAngle() {
        return mErrorAltitudeAngle;
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
        out.writeDouble(mMeters);
        out.writeDouble(mErrorMeters);
        out.writeDouble(mAzimuthAngle);
        out.writeDouble(mErrorAzimuthAngle);
        out.writeDouble(mAltitudeAngle);
        out.writeDouble(mErrorAltitudeAngle);
    }

    /** @hide **/
    @Override
    public String toString() {
        return "DistanceMeasurement["
                + "meters: " + mMeters
                + ", errorMeters: " + mErrorMeters
                + ", azimuthAngle: " + mAzimuthAngle
                + ", errorAzimuthAngle: " + mErrorAzimuthAngle
                + ", altitudeAngle: " + mAltitudeAngle
                + ", errorAltitudeAngle: " + mErrorAltitudeAngle
                + "]";
    }

    /**
     * A {@link Parcelable.Creator} to create {@link DistanceMeasurementResult} from parcel.
     *
     */
    public static final @NonNull Parcelable.Creator<DistanceMeasurementResult> CREATOR =
            new Parcelable.Creator<DistanceMeasurementResult>() {
                @Override
                public @NonNull DistanceMeasurementResult createFromParcel(@NonNull Parcel in) {
                    return new Builder(in.readDouble(), in.readDouble())
                            .setAzimuthAngle(in.readDouble())
                            .setErrorAzimuthAngle(in.readDouble())
                            .setAltitudeAngle(in.readDouble())
                            .setErrorAltitudeAngle(in.readDouble())
                            .build();
                }

                @Override
                public @NonNull DistanceMeasurementResult[] newArray(int size) {
                    return new DistanceMeasurementResult[size];
                }
        };

    /**
     * Builder for {@link DistanceMeasurementResult}.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private double mMeters = Double.NaN;
        private double mErrorMeters = Double.NaN;
        private double mAzimuthAngle = Double.NaN;
        private double mErrorAzimuthAngle = Double.NaN;
        private double mAltitudeAngle = Double.NaN;
        private double mErrorAltitudeAngle = Double.NaN;

        /**
         * Constructor of the Builder.
         *
         * @param meters distance in meters
         * @param errorMeters distance error in meters
         * @throws IllegalArgumentException if meters is NaN or error is negative or NaN
         */
        public Builder(@FloatRange(from = 0.0) double meters,
                @FloatRange(from = 0.0)double errorMeters) {
            if (Double.isNaN(meters) || meters < 0.0) {
                throw new IllegalArgumentException(
                        "meters must be >= 0.0 and not NaN: " + meters);
            }
            if (Double.isNaN(errorMeters) || errorMeters < 0.0) {
                throw new IllegalArgumentException(
                        "errorMeters must be >= 0.0 and not NaN: " + errorMeters);
            }
            mMeters = meters;
            mErrorMeters = errorMeters;
        }

        /**
         * Set the azimuth angle measurement in degrees.
         *
         * @param angle azimuth angle in degrees
         * @throws IllegalArgumentException if value is invalid
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setAzimuthAngle(@FloatRange(from = 0.0, to = 360.0) double angle) {
            if (angle > 360.0 || angle < 0.0) {
                throw new IllegalArgumentException(
                        "angle must be in the range from 0.0 to 360.0 : " + angle);
            }
            mAzimuthAngle = angle;
            return this;
        }

        /**
         * Set the azimuth angle error in degrees.
         *
         * @param angle azimuth angle error in degrees
         * @throws IllegalArgumentException if value is invalid
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setErrorAzimuthAngle(@FloatRange(from = 0.0, to = 360.0) double angle) {
            if (angle > 360.0 || angle < 0.0) {
                throw new IllegalArgumentException(
                        "error angle must be in the range from 0.0 to 360.0 : " + angle);
            }
            mErrorAzimuthAngle = angle;
            return this;
        }

        /**
         * Set the altitude angle measurement in degrees.
         *
         * @param angle altitude angle in degrees
         * @throws IllegalArgumentException if value is invalid
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setAltitudeAngle(@FloatRange(from = -90.0, to = 90.0) double angle) {
            if (angle > 90.0 || angle < -90.0) {
                throw new IllegalArgumentException(
                        "angle must be in the range from -90.0 to 90.0 : " + angle);
            }
            mAltitudeAngle = angle;
            return this;
        }

        /**
         * Set the altitude angle error in degrees.
         *
         * @param angle altitude angle error in degrees
         * @throws IllegalArgumentException if value is invalid
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setErrorAltitudeAngle(@FloatRange(from = 0.0, to = 180.0) double angle) {
            if (angle > 180.0 || angle < 0.0) {
                throw new IllegalArgumentException(
                        "error angle must be in the range from 0.0 to 180.0 : " + angle);
            }
            mErrorAltitudeAngle = angle;
            return this;
        }

        /**
         * Builds the {@link DistanceMeasurementResult} object.
         *
         * @throws IllegalStateException if meters, error, or confidence are not set
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public DistanceMeasurementResult build() {
            return new DistanceMeasurementResult(mMeters, mErrorMeters, mAzimuthAngle,
                    mErrorAzimuthAngle, mAltitudeAngle, mErrorAltitudeAngle);
        }
    }
}

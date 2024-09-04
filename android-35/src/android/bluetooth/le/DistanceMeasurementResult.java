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

import android.annotation.FlaggedApi;
import android.annotation.FloatRange;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.bluetooth.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Result of distance measurement.
 *
 * @hide
 */
@SystemApi
public final class DistanceMeasurementResult implements Parcelable {

    /**
     * Normalized Attack Detector Metric. See Channel Sounding CR_PR, 3.13.24 for details.
     *
     * <p>Specification: https://www.bluetooth.com/specifications/specs/channel-sounding-cr-pr/
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                NADM_ATTACK_IS_EXTREMELY_UNLIKELY,
                NADM_ATTACK_IS_VERY_UNLIKELY,
                NADM_ATTACK_IS_UNLIKELY,
                NADM_ATTACK_IS_POSSIBLE,
                NADM_ATTACK_IS_LIKELY,
                NADM_ATTACK_IS_VERY_LIKELY,
                NADM_ATTACK_IS_EXTREMELY_LIKELY,
                NADM_UNKNOWN
            })
    @interface Nadm {}

    /**
     * Attack is extremely unlikely.
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    public static final int NADM_ATTACK_IS_EXTREMELY_UNLIKELY = 0;

    /**
     * Attack is very unlikely.
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    public static final int NADM_ATTACK_IS_VERY_UNLIKELY = 1;

    /**
     * Attack is unlikely.
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    public static final int NADM_ATTACK_IS_UNLIKELY = 2;

    /**
     * Attack is possible.
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    public static final int NADM_ATTACK_IS_POSSIBLE = 3;

    /**
     * Attack is likely.
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    public static final int NADM_ATTACK_IS_LIKELY = 4;

    /**
     * Attack is very likely.
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    public static final int NADM_ATTACK_IS_VERY_LIKELY = 5;

    /**
     * Attack is extremely likely.
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    public static final int NADM_ATTACK_IS_EXTREMELY_LIKELY = 6;

    /**
     * Unknown NADM, if a device is unable to determine a NADM value, then it shall report this.
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    public static final int NADM_UNKNOWN = 0xFF;

    private final double mMeters;
    private final double mErrorMeters;
    private final double mAzimuthAngle;
    private final double mErrorAzimuthAngle;
    private final double mAltitudeAngle;
    private final double mErrorAltitudeAngle;
    private final double mDelaySpreadMeters;
    private final double mConfidenceLevel;
    private final int mDetectedAttackLevel;
    private final double mVelocityMetersPerSecond;

    private DistanceMeasurementResult(
            double meters,
            double errorMeters,
            double azimuthAngle,
            double errorAzimuthAngle,
            double altitudeAngle,
            double errorAltitudeAngle,
            double delaySpreadMeters,
            double confidenceLevel,
            @Nadm int detectedAttackLevel,
            double velocityMetersPerSecond) {
        mMeters = meters;
        mErrorMeters = errorMeters;
        mAzimuthAngle = azimuthAngle;
        mErrorAzimuthAngle = errorAzimuthAngle;
        mAltitudeAngle = altitudeAngle;
        mErrorAltitudeAngle = errorAltitudeAngle;
        mDelaySpreadMeters = delaySpreadMeters;
        mConfidenceLevel = confidenceLevel;
        mDetectedAttackLevel = detectedAttackLevel;
        mVelocityMetersPerSecond = velocityMetersPerSecond;
    }

    /**
     * Distance measurement in meters.
     *
     * @return distance in meters
     * @hide
     */
    @SystemApi
    public double getResultMeters() {
        return mMeters;
    }

    /**
     * Error of distance measurement in meters.
     *
     * <p>Must be positive.
     *
     * @return error of distance measurement in meters
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
     * <p>Azimuth of remote device in horizontal coordinate system, this measured from azimuth north
     * and increasing eastward. When the remote device in azimuth north, this angle is 0, whe the
     * remote device in azimuth south, this angle is 180.
     *
     * <p>See: <a href="https://en.wikipedia.org/wiki/Horizontal_coordinate_system">Horizontal
     * coordinate system</a>for the details
     *
     * <p>On an Android device, azimuth north is defined as the angle perpendicular away from the
     * back of the device when holding it in portrait mode upright.
     *
     * <p>The Azimuth north is defined as the direction in which the top edge of the device is
     * facing when it is placed flat.
     *
     * @return azimuth angle in degrees or Double.NaN if not available
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
     * @hide
     */
    @SystemApi
    public double getErrorAltitudeAngle() {
        return mErrorAltitudeAngle;
    }

    /**
     * Get estimated delay spread in meters of the measured channel. This is a measure of multipath
     * richness of the channel.
     *
     * @return delay spread in meters in degrees or Double.NaN if not available
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    public double getDelaySpreadMeters() {
        return mDelaySpreadMeters;
    }

    /**
     * Get a normalized value from 0.0 (low confidence) to 1.0 (high confidence) representing the
     * confidence of estimated distance.
     *
     * @return confidence of estimated distance or Double.NaN if not available
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    @FloatRange(from = 0.0, to = 1.0)
    public double getConfidenceLevel() {
        return mConfidenceLevel;
    }

    /**
     * Get a value that represents the chance of being attacked for the measurement.
     *
     * @return Nadm that represents the chance of being attacked for the measurement.
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    @Nadm
    public int getDetectedAttackLevel() {
        return mDetectedAttackLevel;
    }

    /**
     * Get estimated velocity, in the direction of line between two devices, of the moving object in
     * meters/sec.
     *
     * @return Estimated velocity, in the direction of line between two devices, of the moving
     *     object in meters/sec.
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
    @SystemApi
    public double getVelocityMetersPerSecond() {
        return mVelocityMetersPerSecond;
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
        out.writeDouble(mMeters);
        out.writeDouble(mErrorMeters);
        out.writeDouble(mAzimuthAngle);
        out.writeDouble(mErrorAzimuthAngle);
        out.writeDouble(mAltitudeAngle);
        out.writeDouble(mErrorAltitudeAngle);
        out.writeDouble(mDelaySpreadMeters);
        out.writeDouble(mConfidenceLevel);
        out.writeInt(mDetectedAttackLevel);
        out.writeDouble(mVelocityMetersPerSecond);
    }

    /**
     * @hide *
     */
    @Override
    public String toString() {
        return "DistanceMeasurement["
                + "meters: "
                + mMeters
                + ", errorMeters: "
                + mErrorMeters
                + ", azimuthAngle: "
                + mAzimuthAngle
                + ", errorAzimuthAngle: "
                + mErrorAzimuthAngle
                + ", altitudeAngle: "
                + mAltitudeAngle
                + ", errorAltitudeAngle: "
                + mErrorAltitudeAngle
                + ", delaySpreadMeters: "
                + mDelaySpreadMeters
                + ", confidenceLevel: "
                + mConfidenceLevel
                + ", detectedAttackLevel: "
                + mDetectedAttackLevel
                + ", velocityMetersPerSecond: "
                + mVelocityMetersPerSecond
                + "]";
    }

    /** A {@link Parcelable.Creator} to create {@link DistanceMeasurementResult} from parcel. */
    public static final @NonNull Parcelable.Creator<DistanceMeasurementResult> CREATOR =
            new Parcelable.Creator<DistanceMeasurementResult>() {
                @Override
                public @NonNull DistanceMeasurementResult createFromParcel(@NonNull Parcel in) {
                    return new Builder(in.readDouble(), in.readDouble())
                            .setAzimuthAngle(in.readDouble())
                            .setErrorAzimuthAngle(in.readDouble())
                            .setAltitudeAngle(in.readDouble())
                            .setErrorAltitudeAngle(in.readDouble())
                            .setDelaySpreadMeters(in.readDouble())
                            .setConfidenceLevel(in.readDouble())
                            .setDetectedAttackLevel(in.readInt())
                            .setVelocityMetersPerSecond(in.readDouble())
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
        private double mDelaySpreadMeters = Double.NaN;
        private double mConfidenceLevel = Double.NaN;
        private int mDetectedAttackLevel = NADM_UNKNOWN;
        private double mVelocityMetersPerSecond = Double.NaN;

        /**
         * Constructor of the Builder.
         *
         * @param meters distance in meters
         * @param errorMeters distance error in meters
         * @throws IllegalArgumentException if meters is NaN or error is negative or NaN
         */
        public Builder(
                @FloatRange(from = 0.0) double meters, @FloatRange(from = 0.0) double errorMeters) {
            if (Double.isNaN(meters) || meters < 0.0) {
                throw new IllegalArgumentException("meters must be >= 0.0 and not NaN: " + meters);
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
         * Set the estimated delay spread in meters.
         *
         * @param delaySpreadMeters estimated delay spread in meters
         * @throws IllegalArgumentException if value is invalid
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
        @SystemApi
        @NonNull
        public Builder setDelaySpreadMeters(double delaySpreadMeters) {
            if (delaySpreadMeters < 0.0) {
                throw new IllegalArgumentException("delaySpreadMeters must be > 0.0");
            }
            mDelaySpreadMeters = delaySpreadMeters;
            return this;
        }

        /**
         * Set the confidence of estimated distance.
         *
         * @param confidenceLevel a normalized value from 0.0 (low confidence) to 1.0 (high
         *     confidence) representing the confidence of estimated distance
         * @throws IllegalArgumentException if value is invalid
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
        @SystemApi
        @NonNull
        public Builder setConfidenceLevel(
                @FloatRange(from = 0.0, to = 1.0) double confidenceLevel) {
            if (confidenceLevel > 1.0 || confidenceLevel < 0.0) {
                throw new IllegalArgumentException(
                        "error confidenceLevel must be in the range from 0.0 to 100.0 : "
                                + confidenceLevel);
            }
            mConfidenceLevel = confidenceLevel;
            return this;
        }

        /**
         * Set the value that represents the chance of being attacked for the measurement.
         *
         * @param detectedAttackLevel a value that represents the chance of being attacked for the
         *     measurement.
         * @throws IllegalArgumentException if value is invalid
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
        @SystemApi
        @NonNull
        public Builder setDetectedAttackLevel(@Nadm int detectedAttackLevel) {
            switch (detectedAttackLevel) {
                case NADM_ATTACK_IS_EXTREMELY_UNLIKELY:
                case NADM_ATTACK_IS_VERY_UNLIKELY:
                case NADM_ATTACK_IS_UNLIKELY:
                case NADM_ATTACK_IS_POSSIBLE:
                case NADM_ATTACK_IS_LIKELY:
                case NADM_ATTACK_IS_VERY_LIKELY:
                case NADM_ATTACK_IS_EXTREMELY_LIKELY:
                case NADM_UNKNOWN:
                    mDetectedAttackLevel = detectedAttackLevel;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid value " + detectedAttackLevel);
            }
            return this;
        }

        /**
         * Set estimated velocity, in the direction of line between two devices, of the moving
         * object in meters/sec.
         *
         * @param velocityMetersPerSecond estimated velocity in meters/sec.
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
        @SystemApi
        @NonNull
        public Builder setVelocityMetersPerSecond(double velocityMetersPerSecond) {
            mVelocityMetersPerSecond = velocityMetersPerSecond;
            return this;
        }

        /**
         * Builds the {@link DistanceMeasurementResult} object.
         *
         * @throws IllegalStateException if meters, error, or confidence are not set
         * @hide
         */
        @SystemApi
        @NonNull
        public DistanceMeasurementResult build() {
            return new DistanceMeasurementResult(
                    mMeters,
                    mErrorMeters,
                    mAzimuthAngle,
                    mErrorAzimuthAngle,
                    mAltitudeAngle,
                    mErrorAltitudeAngle,
                    mDelaySpreadMeters,
                    mConfidenceLevel,
                    mDetectedAttackLevel,
                    mVelocityMetersPerSecond);
        }
    }
}

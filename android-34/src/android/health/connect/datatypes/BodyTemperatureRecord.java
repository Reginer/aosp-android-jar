/*
 * Copyright (C) 2023 The Android Open Source Project
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
package android.health.connect.datatypes;

import static android.health.connect.datatypes.validation.ValidationUtils.validateIntDefValue;

import android.annotation.NonNull;
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.BodyTemperatureMeasurementLocations;
import android.health.connect.datatypes.units.Temperature;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.BodyTemperatureRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the body temperature of a user. Each record represents a single instantaneous body
 * temperature measuremen
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BODY_TEMPERATURE)
public final class BodyTemperatureRecord extends InstantRecord {

    private final int mMeasurementLocation;
    private final Temperature mTemperature;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param measurementLocation MeasurementLocation of this activity
     * @param temperature Temperature of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private BodyTemperatureRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @BodyTemperatureMeasurementLocation.BodyTemperatureMeasurementLocations
                    int measurementLocation,
            @NonNull Temperature temperature,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        Objects.requireNonNull(temperature);
        validateIntDefValue(
                measurementLocation,
                BodyTemperatureMeasurementLocation.VALID_TYPES,
                BodyTemperatureMeasurementLocation.class.getSimpleName());
        if (!skipValidation) {
            ValidationUtils.requireInRange(temperature.getInCelsius(), 0.0, 100, "temperature");
        }
        mMeasurementLocation = measurementLocation;
        mTemperature = temperature;
    }

    /**
     * @return measurementLocation
     */
    @BodyTemperatureMeasurementLocations
    public int getMeasurementLocation() {
        return mMeasurementLocation;
    }

    /**
     * @return temperature in {@link Temperature} unit.
     */
    @NonNull
    public Temperature getTemperature() {
        return mTemperature;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        BodyTemperatureRecord that = (BodyTemperatureRecord) o;
        return getMeasurementLocation() == that.getMeasurementLocation()
                && getTemperature().equals(that.getTemperature());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getMeasurementLocation(), getTemperature());
    }

    /** Builder class for {@link BodyTemperatureRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final int mMeasurementLocation;
        private final Temperature mTemperature;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param measurementLocation Where on the user's body the temperature measurement was taken
         *     from. Optional field. Allowed values: {@link BodyTemperatureMeasurementLocation}.
         * @param temperature Temperature in {@link Temperature} unit. Required field. Valid range:
         *     0-100 Celsius degrees.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @BodyTemperatureMeasurementLocation.BodyTemperatureMeasurementLocations
                        int measurementLocation,
                @NonNull Temperature temperature) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            Objects.requireNonNull(temperature);
            mMetadata = metadata;
            mTime = time;
            mMeasurementLocation = measurementLocation;
            mTemperature = temperature;
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(time);
        }

        /** Sets the zone offset of the user when the activity happened */
        @NonNull
        public Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /** Sets the zone offset of this record to system default. */
        @NonNull
        public Builder clearZoneOffset() {
            mZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /**
         * @return Object of {@link BodyTemperatureRecord} without validating the values.
         * @hide
         */
        @NonNull
        public BodyTemperatureRecord buildWithoutValidation() {
            return new BodyTemperatureRecord(
                    mMetadata, mTime, mZoneOffset, mMeasurementLocation, mTemperature, true);
        }

        /**
         * @return Object of {@link BodyTemperatureRecord}
         */
        @NonNull
        public BodyTemperatureRecord build() {
            return new BodyTemperatureRecord(
                    mMetadata, mTime, mZoneOffset, mMeasurementLocation, mTemperature, false);
        }
    }

    /** @hide */
    @Override
    public BodyTemperatureRecordInternal toRecordInternal() {
        BodyTemperatureRecordInternal recordInternal =
                (BodyTemperatureRecordInternal)
                        new BodyTemperatureRecordInternal()
                                .setUuid(getMetadata().getId())
                                .setPackageName(getMetadata().getDataOrigin().getPackageName())
                                .setLastModifiedTime(
                                        getMetadata().getLastModifiedTime().toEpochMilli())
                                .setClientRecordId(getMetadata().getClientRecordId())
                                .setClientRecordVersion(getMetadata().getClientRecordVersion())
                                .setManufacturer(getMetadata().getDevice().getManufacturer())
                                .setModel(getMetadata().getDevice().getModel())
                                .setDeviceType(getMetadata().getDevice().getType())
                                .setRecordingMethod(getMetadata().getRecordingMethod());
        recordInternal.setTime(getTime().toEpochMilli());
        recordInternal.setZoneOffset(getZoneOffset().getTotalSeconds());
        recordInternal.setMeasurementLocation(mMeasurementLocation);
        recordInternal.setTemperature(mTemperature.getInCelsius());
        return recordInternal;
    }
}

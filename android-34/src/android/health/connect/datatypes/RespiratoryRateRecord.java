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

import android.annotation.FloatRange;
import android.annotation.NonNull;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.RespiratoryRateRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the user's respiratory rate. Each record represents a single instantaneous measurement.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE)
public final class RespiratoryRateRecord extends InstantRecord {

    private final double mRate;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param rate Rate of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private RespiratoryRateRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @NonNull double rate,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        if (!skipValidation) {
            ValidationUtils.requireInRange(rate, 0.0, 1000.0, "rate");
        }
        mRate = rate;
    }

    /**
     * @return rate
     */
    public double getRate() {
        return mRate;
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
        RespiratoryRateRecord that = (RespiratoryRateRecord) o;
        return Double.compare(that.getRate(), getRate()) == 0;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getRate());
    }

    /** Builder class for {@link RespiratoryRateRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final double mRate;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param rate Respiratory rate in breaths per minute. Required field. Valid range: 0-1000.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @FloatRange(from = 0, to = 1000) double rate) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            mMetadata = metadata;
            mTime = time;
            mRate = rate;
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
         * @return Object of {@link RespiratoryRateRecord} without validating the values.
         * @hide
         */
        @NonNull
        public RespiratoryRateRecord buildWithoutValidation() {
            return new RespiratoryRateRecord(mMetadata, mTime, mZoneOffset, mRate, true);
        }

        /**
         * @return Object of {@link RespiratoryRateRecord}
         */
        @NonNull
        public RespiratoryRateRecord build() {
            return new RespiratoryRateRecord(mMetadata, mTime, mZoneOffset, mRate, false);
        }
    }

    /** @hide */
    @Override
    public RespiratoryRateRecordInternal toRecordInternal() {
        RespiratoryRateRecordInternal recordInternal =
                (RespiratoryRateRecordInternal)
                        new RespiratoryRateRecordInternal()
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
        recordInternal.setRate(mRate);
        return recordInternal;
    }
}

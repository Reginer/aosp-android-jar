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

import android.annotation.NonNull;
import android.health.connect.datatypes.units.Percentage;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.BodyFatRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the body fat percentage of a user. Each record represents a person's total body fat as a
 * percentage of their total body mass.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BODY_FAT)
public final class BodyFatRecord extends InstantRecord {

    private final Percentage mPercentage;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param percentage Percentage of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private BodyFatRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @NonNull Percentage percentage,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        Objects.requireNonNull(percentage);
        if (!skipValidation) {
            ValidationUtils.requireInRange(percentage.getValue(), 0.0, 100.0, "bodyFatPercentage");
        }
        mPercentage = percentage;
    }
    /**
     * @return percentage in {@link Percentage} unit.
     */
    @NonNull
    public Percentage getPercentage() {
        return mPercentage;
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
        BodyFatRecord that = (BodyFatRecord) o;
        return getPercentage().equals(that.getPercentage());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getPercentage());
    }

    /** Builder class for {@link BodyFatRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final Percentage mPercentage;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param percentage Percentage in {@link Percentage} unit. Required field. Valid range:
         *     0-100.
         */
        public Builder(
                @NonNull Metadata metadata, @NonNull Instant time, @NonNull Percentage percentage) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            Objects.requireNonNull(percentage);
            mMetadata = metadata;
            mTime = time;
            mPercentage = percentage;
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
         * @return Object of {@link BodyFatRecord} without validating the values.
         * @hide
         */
        @NonNull
        public BodyFatRecord buildWithoutValidation() {
            return new BodyFatRecord(mMetadata, mTime, mZoneOffset, mPercentage, true);
        }

        /**
         * @return Object of {@link BodyFatRecord}
         */
        @NonNull
        public BodyFatRecord build() {
            return new BodyFatRecord(mMetadata, mTime, mZoneOffset, mPercentage, false);
        }
    }

    /** @hide */
    @Override
    public BodyFatRecordInternal toRecordInternal() {
        BodyFatRecordInternal recordInternal =
                (BodyFatRecordInternal)
                        new BodyFatRecordInternal()
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
        recordInternal.setPercentage(mPercentage.getValue());
        return recordInternal;
    }
}

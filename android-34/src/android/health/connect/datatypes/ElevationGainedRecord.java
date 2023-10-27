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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED;

import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.ElevationGainedRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Captures the elevation gained by the user since the last reading. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED)
public final class ElevationGainedRecord extends IntervalRecord {
    /** Builder class for {@link ElevationGainedRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final Length mElevation;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param elevation Elevation in {@link Length} unit. Required field. Valid range:
         *     -1000000-1000000 meters.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull Length elevation) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(elevation);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mElevation = elevation;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(startTime);
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(endTime);
        }

        /** Sets the zone offset of the user when the activity started */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            Objects.requireNonNull(startZoneOffset);

            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /** Sets the zone offset of the user when the activity ended */
        @NonNull
        public Builder setEndZoneOffset(@NonNull ZoneOffset endZoneOffset) {
            Objects.requireNonNull(endZoneOffset);

            mEndZoneOffset = endZoneOffset;
            return this;
        }

        /** Sets the start zone offset of this record to system default. */
        @NonNull
        public Builder clearStartZoneOffset() {
            mStartZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /** Sets the start zone offset of this record to system default. */
        @NonNull
        public Builder clearEndZoneOffset() {
            mEndZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /**
         * @return Object of {@link ElevationGainedRecord} without validating the values.
         * @hide
         */
        @NonNull
        public ElevationGainedRecord buildWithoutValidation() {
            return new ElevationGainedRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mElevation,
                    true);
        }

        /**
         * @return Object of {@link ElevationGainedRecord}
         */
        @NonNull
        public ElevationGainedRecord build() {
            return new ElevationGainedRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mElevation,
                    false);
        }
    }

    /**
     * Metric identifier to get total elevation gained using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Length> ELEVATION_GAINED_TOTAL =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier
                            .ELEVATION_RECORD_ELEVATION_GAINED_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_ELEVATION_GAINED,
                    Length.class);

    private final Length mElevation;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param elevation Elevation of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private ElevationGainedRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull Length elevation,
            boolean skipValidation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset, skipValidation);
        Objects.requireNonNull(elevation);
        if (!skipValidation) {
            ValidationUtils.requireInRange(
                    elevation.getInMeters(), -1000000.0, 1000000.0, "elevation");
        }
        mElevation = elevation;
    }

    /**
     * @return elevation in {@link Length} unit.
     */
    @NonNull
    public Length getElevation() {
        return mElevation;
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
        ElevationGainedRecord that = (ElevationGainedRecord) o;
        return getElevation().equals(that.getElevation());
    }

    /**
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getElevation());
    }

    /** @hide */
    @Override
    public ElevationGainedRecordInternal toRecordInternal() {
        ElevationGainedRecordInternal recordInternal =
                (ElevationGainedRecordInternal)
                        new ElevationGainedRecordInternal()
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
        recordInternal.setStartTime(getStartTime().toEpochMilli());
        recordInternal.setEndTime(getEndTime().toEpochMilli());
        recordInternal.setStartZoneOffset(getStartZoneOffset().getTotalSeconds());
        recordInternal.setEndZoneOffset(getEndZoneOffset().getTotalSeconds());
        recordInternal.setElevation(mElevation.getInMeters());
        return recordInternal;
    }
}

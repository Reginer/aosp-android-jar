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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;

import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.DistanceRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures distance travelled by the user since the last reading. The total distance over an
 * interval can be calculated by adding together all the values during the interval. The start time
 * of each record should represent the start of the interval in which the distance was covered.
 *
 * <p>If break downs are preferred in scenario of a long workout, consider writing multiple distance
 * records. The start time of each record should be equal to or greater than the end time of the
 * previous record.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_DISTANCE)
public final class DistanceRecord extends IntervalRecord {
    /** Builder class for {@link DistanceRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final Length mDistance;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param distance Distance in {@link Length} unit. Required field. Valid range: 0-1000000
         *     meters.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull Length distance) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(distance);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mDistance = distance;
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
         * @return Object of {@link DistanceRecord} without validating the values.
         * @hide
         */
        @NonNull
        public DistanceRecord buildWithoutValidation() {
            return new DistanceRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mDistance,
                    true);
        }

        /**
         * @return Object of {@link DistanceRecord}
         */
        @NonNull
        public DistanceRecord build() {
            return new DistanceRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mDistance,
                    false);
        }
    }

    /**
     * Metric identifier to get total distance using aggregate APIs in {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Length> DISTANCE_TOTAL =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.DISTANCE_RECORD_DISTANCE_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_DISTANCE,
                    Length.class);

    private final Length mDistance;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param distance Distance of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private DistanceRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull Length distance,
            boolean skipValidation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset, skipValidation);
        Objects.requireNonNull(distance);
        if (!skipValidation) {
            ValidationUtils.requireInRange(
                    distance.getInMeters(), 0.0, 1000000.0, "revolutionsPerMinute");
        }
        mDistance = distance;
    }

    /**
     * @return distance of this activity in {@link Length} unit
     */
    @NonNull
    public Length getDistance() {
        return mDistance;
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
        DistanceRecord that = (DistanceRecord) o;
        return getDistance().equals(that.getDistance());
    }

    /**
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDistance());
    }

    /** @hide */
    @Override
    public DistanceRecordInternal toRecordInternal() {
        DistanceRecordInternal recordInternal =
                (DistanceRecordInternal)
                        new DistanceRecordInternal()
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
        recordInternal.setDistance(mDistance.getInMeters());
        return recordInternal;
    }
}

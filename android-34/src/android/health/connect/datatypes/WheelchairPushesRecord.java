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

import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.WHEEL_CHAIR_PUSHES_RECORD_COUNT_TOTAL;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.WheelchairPushesRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the number of wheelchair pushes done over a time interval. Each push is only reported
 * once so records shouldn't have overlapping time. The start time of each record should represent
 * the start of the interval in which pushes were made.
 */
@Identifier(recordIdentifier = RECORD_TYPE_WHEELCHAIR_PUSHES)
public final class WheelchairPushesRecord extends IntervalRecord {

    private final long mCount;

    /**
     * Metric identifier to get total wheelchair push count using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Long> WHEEL_CHAIR_PUSHES_COUNT_TOTAL =
            new AggregationType<>(
                    WHEEL_CHAIR_PUSHES_RECORD_COUNT_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_WHEELCHAIR_PUSHES,
                    Long.class);

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param count Count of pushes
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private WheelchairPushesRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            long count,
            boolean skipValidation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(startZoneOffset);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endZoneOffset);
        if (!skipValidation) {
            ValidationUtils.requireInRange(count, 0, 1000000, "count");
        }
        mCount = count;
    }

    /**
     * @return wheelchair pushes count.
     */
    public long getCount() {
        return mCount;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the object argument; {@code false}
     *     otherwise.
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (super.equals(object) && object instanceof WheelchairPushesRecord) {
            WheelchairPushesRecord other = (WheelchairPushesRecord) object;
            return this.getCount() == other.getCount();
        }
        return false;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.getCount());
    }

    /** Builder class for {@link WheelchairPushesRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final long mCount;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param count Count of pushes. Required field. Valid range: 1-1000000.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @IntRange(from = 1, to = 1000000) long count) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mCount = count;
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
         * @return Object of {@link WheelchairPushesRecord} without validating the values.
         * @hide
         */
        @NonNull
        public WheelchairPushesRecord buildWithoutValidation() {
            return new WheelchairPushesRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mCount,
                    true);
        }

        /**
         * @return Object of {@link WheelchairPushesRecord}
         */
        @NonNull
        public WheelchairPushesRecord build() {
            return new WheelchairPushesRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mCount,
                    false);
        }
    }

    /** @hide */
    @Override
    public WheelchairPushesRecordInternal toRecordInternal() {
        WheelchairPushesRecordInternal recordInternal =
                (WheelchairPushesRecordInternal)
                        new WheelchairPushesRecordInternal()
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
        recordInternal.setCount((int) mCount);
        return recordInternal;
    }
}

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
import android.health.connect.internal.datatypes.MenstruationPeriodRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Captures the menstruation period record. Each record contains the start / end of the event. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_PERIOD)
public final class MenstruationPeriodRecord extends IntervalRecord {

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this interval
     * @param startZoneOffset Zone offset of the user when the interval started
     * @param endTime End time of this interval
     * @param endZoneOffset Zone offset of the user when the interval finished
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private MenstruationPeriodRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            boolean skipValidation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset, skipValidation);
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
        return super.equals(object);
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /** Builder class for {@link MenstruationPeriodRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this interval
         * @param endTime End time of this interval
         */
        public Builder(
                @NonNull Metadata metadata, @NonNull Instant startTime, @NonNull Instant endTime) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(startTime);
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(endTime);
        }

        /** Sets the zone offset of the user when the interval of this record started */
        @NonNull
        public MenstruationPeriodRecord.Builder setStartZoneOffset(
                @NonNull ZoneOffset startZoneOffset) {
            Objects.requireNonNull(startZoneOffset);

            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /** Clears start zone offset. */
        @NonNull
        public MenstruationPeriodRecord.Builder clearStartZoneOffset() {
            mStartZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /** Sets the zone offset of the user when the interval of this record ended */
        @NonNull
        public MenstruationPeriodRecord.Builder setEndZoneOffset(
                @NonNull ZoneOffset endZoneOffset) {
            Objects.requireNonNull(endZoneOffset);

            mEndZoneOffset = endZoneOffset;
            return this;
        }

        /** Clears end zone offset. */
        @NonNull
        public MenstruationPeriodRecord.Builder clearEndZoneOffset() {
            mEndZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /**
         * @return Object of {@link MenstruationPeriodRecord} without validating the values.
         * @hide
         */
        @NonNull
        public MenstruationPeriodRecord buildWithoutValidation() {
            return new MenstruationPeriodRecord(
                    mMetadata, mStartTime, mStartZoneOffset, mEndTime, mEndZoneOffset, true);
        }

        /** Builds {@link MenstruationPeriodRecord} */
        @NonNull
        public MenstruationPeriodRecord build() {
            return new MenstruationPeriodRecord(
                    mMetadata, mStartTime, mStartZoneOffset, mEndTime, mEndZoneOffset, false);
        }
    }

    /** @hide */
    @Override
    public MenstruationPeriodRecordInternal toRecordInternal() {
        MenstruationPeriodRecordInternal recordInternal =
                (MenstruationPeriodRecordInternal)
                        new MenstruationPeriodRecordInternal()
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
        return recordInternal;
    }
}

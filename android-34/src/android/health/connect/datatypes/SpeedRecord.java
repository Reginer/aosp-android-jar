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
import android.health.connect.datatypes.units.Velocity;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.SpeedRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Captures the user's speed, e.g. during running or cycling. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SPEED)
public final class SpeedRecord extends IntervalRecord {
    private final List<SpeedRecordSample> mSpeedRecordSamples;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param speedRecordSamples Samples of recorded SpeedRecord
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private SpeedRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull List<SpeedRecordSample> speedRecordSamples,
            boolean skipValidation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset, skipValidation);
        Objects.requireNonNull(speedRecordSamples);
        if (!skipValidation) {
            ValidationUtils.validateSampleStartAndEndTime(
                    startTime,
                    endTime,
                    speedRecordSamples.stream()
                            .map(SpeedRecord.SpeedRecordSample::getTime)
                            .toList());
        }
        mSpeedRecordSamples = speedRecordSamples;
    }

    /**
     * @return SpeedRecord samples corresponding to this record
     */
    @NonNull
    public List<SpeedRecordSample> getSamples() {
        return mSpeedRecordSamples;
    }

    /** Represents a single measurement of the speed, a scalar magnitude. */
    public static final class SpeedRecordSample {
        private final Velocity mSpeed;
        private final Instant mTime;

        /**
         * SpeedRecord sample for entries of {@link SpeedRecord}
         *
         * @param speed Speed in {@link Velocity} unit.
         * @param time The point in time when the measurement was taken.
         */
        public SpeedRecordSample(@NonNull Velocity speed, @NonNull Instant time) {
            this(speed, time, false);
        }

        /**
         * SpeedRecord sample for entries of {@link SpeedRecord}
         *
         * @param speed Speed in {@link Velocity} unit.
         * @param time The point in time when the measurement was taken.
         * @param skipValidation Boolean flag to skip validation of record values.
         * @hide
         */
        public SpeedRecordSample(
                @NonNull Velocity speed, @NonNull Instant time, boolean skipValidation) {
            Objects.requireNonNull(time);
            Objects.requireNonNull(speed);
            if (!skipValidation) {
                ValidationUtils.requireInRange(speed.getInMetersPerSecond(), 0.0, 1000000, "speed");
            }
            mTime = time;
            mSpeed = speed;
        }

        /**
         * @return Speed for this sample
         */
        @NonNull
        public Velocity getSpeed() {
            return mSpeed;
        }

        /**
         * @return time at which this sample was recorded
         */
        @NonNull
        public Instant getTime() {
            return mTime;
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         *
         * @param object the reference object with which to compare.
         * @return {@code true} if this object is the same as the obj
         */
        @Override
        public boolean equals(@NonNull Object object) {
            if (super.equals(object) && object instanceof SpeedRecordSample) {
                SpeedRecordSample other = (SpeedRecordSample) object;
                return getSpeed().equals(other.getSpeed())
                        && getTime().toEpochMilli() == other.getTime().toEpochMilli();
            }
            return false;
        }

        /**
         * Returns a hash code value for the object.
         *
         * @return a hash code value for this object.
         */
        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), getSpeed(), getTime());
        }
    }

    /** Builder class for {@link SpeedRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private final List<SpeedRecordSample> mSpeedRecordSamples;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param speedRecordSamples Samples of recorded SpeedRecord
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull List<SpeedRecordSample> speedRecordSamples) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(speedRecordSamples);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mSpeedRecordSamples = speedRecordSamples;
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
         * @return Object of {@link SpeedRecord} without validating the values.
         * @hide
         */
        @NonNull
        public SpeedRecord buildWithoutValidation() {
            return new SpeedRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mSpeedRecordSamples,
                    true);
        }

        /**
         * @return Object of {@link SpeedRecord}
         */
        @NonNull
        public SpeedRecord build() {
            return new SpeedRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mSpeedRecordSamples,
                    false);
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (super.equals(object) && object instanceof SpeedRecord) {
            SpeedRecord other = (SpeedRecord) object;
            if (getSamples().size() != other.getSamples().size()) return false;
            for (int idx = 0; idx < getSamples().size(); idx++) {
                if (!Objects.equals(
                                getSamples().get(idx).getSpeed(),
                                other.getSamples().get(idx).getSpeed())
                        || getSamples().get(idx).getTime().toEpochMilli()
                                != other.getSamples().get(idx).getTime().toEpochMilli()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getSamples());
    }

    /** @hide */
    @Override
    public SpeedRecordInternal toRecordInternal() {
        SpeedRecordInternal recordInternal =
                (SpeedRecordInternal)
                        new SpeedRecordInternal()
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
        Set<SpeedRecordInternal.SpeedRecordSample> samples = new HashSet<>(getSamples().size());

        for (SpeedRecord.SpeedRecordSample speedRecordSample : getSamples()) {
            samples.add(
                    new SpeedRecordInternal.SpeedRecordSample(
                            speedRecordSample.getSpeed().getInMetersPerSecond(),
                            speedRecordSample.getTime().toEpochMilli()));
        }
        recordInternal.setSamples(samples);
        recordInternal.setStartTime(getStartTime().toEpochMilli());
        recordInternal.setEndTime(getEndTime().toEpochMilli());
        recordInternal.setStartZoneOffset(getStartZoneOffset().getTotalSeconds());
        recordInternal.setEndZoneOffset(getEndZoneOffset().getTotalSeconds());

        return recordInternal;
    }
}

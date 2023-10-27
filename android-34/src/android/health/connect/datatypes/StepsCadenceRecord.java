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
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.StepsCadenceRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Captures the user's steps cadence. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE)
public final class StepsCadenceRecord extends IntervalRecord {
    private final List<StepsCadenceRecordSample> mStepsCadenceRecordSamples;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param stepsCadenceRecordSamples Samples of recorded StepsCadenceRecord
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private StepsCadenceRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull List<StepsCadenceRecordSample> stepsCadenceRecordSamples,
            boolean skipValidation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset, skipValidation);
        Objects.requireNonNull(stepsCadenceRecordSamples);
        if (!skipValidation) {
            ValidationUtils.validateSampleStartAndEndTime(
                    startTime,
                    endTime,
                    stepsCadenceRecordSamples.stream()
                            .map(StepsCadenceRecordSample::getTime)
                            .toList());
        }
        mStepsCadenceRecordSamples = stepsCadenceRecordSamples;
    }

    /**
     * @return StepsCadenceRecord samples corresponding to this record
     */
    @NonNull
    public List<StepsCadenceRecordSample> getSamples() {
        return mStepsCadenceRecordSamples;
    }

    /** Represents a single measurement of the steps cadence. */
    public static final class StepsCadenceRecordSample {
        private final double mRate;
        private final Instant mTime;

        /**
         * StepsCadenceRecord sample for entries of {@link StepsCadenceRecord}
         *
         * @param rate Rate in steps per minute.
         * @param time The point in time when the measurement was taken.
         */
        public StepsCadenceRecordSample(double rate, @NonNull Instant time) {
            this(rate, time, false);
        }

        /**
         * StepsCadenceRecord sample for entries of {@link StepsCadenceRecord}
         *
         * @param rate Rate in steps per minute.
         * @param time The point in time when the measurement was taken.
         * @param skipValidation Boolean flag to skip validation of record values.
         * @hide
         */
        public StepsCadenceRecordSample(
                double rate, @NonNull Instant time, boolean skipValidation) {
            Objects.requireNonNull(time);
            if (!skipValidation) {
                ValidationUtils.requireInRange(rate, 0.0, 10000.0, "rate");
            }
            mTime = time;
            mRate = rate;
        }

        /**
         * @return Rate for this sample
         */
        public double getRate() {
            return mRate;
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
            if (super.equals(object) && object instanceof StepsCadenceRecordSample) {
                StepsCadenceRecordSample other = (StepsCadenceRecordSample) object;
                return getRate() == other.getRate()
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
            return Objects.hash(super.hashCode(), getRate(), getTime());
        }
    }

    /** Builder class for {@link StepsCadenceRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private final List<StepsCadenceRecordSample> mStepsCadenceRecordSamples;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param stepsCadenceRecordSamples Samples of recorded StepsCadenceRecord
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull List<StepsCadenceRecordSample> stepsCadenceRecordSamples) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(stepsCadenceRecordSamples);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStepsCadenceRecordSamples = stepsCadenceRecordSamples;
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
         * @return Object of {@link StepsCadenceRecord} without validating the values.
         * @hide
         */
        @NonNull
        public StepsCadenceRecord buildWithoutValidation() {
            return new StepsCadenceRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mStepsCadenceRecordSamples,
                    true);
        }

        /**
         * @return Object of {@link StepsCadenceRecord}
         */
        @NonNull
        public StepsCadenceRecord build() {
            return new StepsCadenceRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mStepsCadenceRecordSamples,
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
        if (super.equals(object) && object instanceof StepsCadenceRecord) {
            StepsCadenceRecord other = (StepsCadenceRecord) object;
            if (getSamples().size() != other.getSamples().size()) return false;
            for (int idx = 0; idx < getSamples().size(); idx++) {
                if (getSamples().get(idx).getRate() != other.getSamples().get(idx).getRate()
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
    public StepsCadenceRecordInternal toRecordInternal() {
        StepsCadenceRecordInternal recordInternal =
                (StepsCadenceRecordInternal)
                        new StepsCadenceRecordInternal()
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
        Set<StepsCadenceRecordInternal.StepsCadenceRecordSample> samples =
                new HashSet<>(getSamples().size());

        for (StepsCadenceRecord.StepsCadenceRecordSample stepsCadenceRecordSample : getSamples()) {
            samples.add(
                    new StepsCadenceRecordInternal.StepsCadenceRecordSample(
                            stepsCadenceRecordSample.getRate(),
                            stepsCadenceRecordSample.getTime().toEpochMilli()));
        }
        recordInternal.setSamples(samples);
        recordInternal.setStartTime(getStartTime().toEpochMilli());
        recordInternal.setEndTime(getEndTime().toEpochMilli());
        recordInternal.setStartZoneOffset(getStartZoneOffset().getTotalSeconds());
        recordInternal.setEndZoneOffset(getEndZoneOffset().getTotalSeconds());

        return recordInternal;
    }
}

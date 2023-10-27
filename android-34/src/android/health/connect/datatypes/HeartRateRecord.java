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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;

import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.HeartRateRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Captures the user's heart rate. Each record represents a series of measurements. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_HEART_RATE)
public final class HeartRateRecord extends IntervalRecord {
    /**
     * Metric identifier to get max heart rate in beats per minute using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Long> BPM_MAX =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.HEART_RATE_RECORD_BPM_MAX,
                    AggregationType.MAX,
                    RECORD_TYPE_HEART_RATE,
                    Long.class);
    /**
     * Metric identifier to get min heart rate in beats per minute using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Long> BPM_MIN =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.HEART_RATE_RECORD_BPM_MIN,
                    AggregationType.MIN,
                    RECORD_TYPE_HEART_RATE,
                    Long.class);

    /**
     * Metric identifier to get avg heart rate using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Long> BPM_AVG =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.HEART_RATE_RECORD_BPM_AVG,
                    AggregationType.AVG,
                    RECORD_TYPE_HEART_RATE,
                    Long.class);

    /**
     * Metric identifier to retrieve the number of heart rate measurements using aggregate APIs in
     * {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Long> HEART_MEASUREMENTS_COUNT =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.HEART_RATE_RECORD_MEASUREMENTS_COUNT,
                    AggregationType.COUNT,
                    RECORD_TYPE_HEART_RATE,
                    Long.class);

    private final List<HeartRateSample> mHeartRateSamples;

    private HeartRateRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull List<HeartRateSample> heartRateSamples,
            boolean skipValidation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset, skipValidation);
        Objects.requireNonNull(heartRateSamples);
        if (!skipValidation) {
            ValidationUtils.validateSampleStartAndEndTime(
                    startTime,
                    endTime,
                    heartRateSamples.stream().map(HeartRateSample::getTime).toList());
        }
        mHeartRateSamples = heartRateSamples;
    }

    /**
     * @return heart rate samples corresponding to this record
     */
    @NonNull
    public List<HeartRateSample> getSamples() {
        return mHeartRateSamples;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (super.equals(object) && object instanceof HeartRateRecord) {
            HeartRateRecord other = (HeartRateRecord) object;
            if (getSamples().size() != other.getSamples().size()) return false;
            for (int idx = 0; idx < getSamples().size(); idx++) {
                if (getSamples().get(idx).getBeatsPerMinute()
                                != other.getSamples().get(idx).getBeatsPerMinute()
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

    /** A class to represent heart rate samples */
    public static final class HeartRateSample {
        private final long mBeatsPerMinute;
        private final Instant mTime;

        /**
         * Heart rate sample for entries of {@link HeartRateRecord}
         *
         * @param beatsPerMinute Heart beats per minute.
         * @param time The point in time when the measurement was taken.
         */
        public HeartRateSample(long beatsPerMinute, @NonNull Instant time) {
            this(beatsPerMinute, time, false);
        }

        /**
         * Heart rate sample for entries of {@link HeartRateRecord}
         *
         * @param beatsPerMinute Heart beats per minute.
         * @param time The point in time when the measurement was taken.
         * @param skipValidation Boolean flag to skip validation of record values.
         * @hide
         */
        public HeartRateSample(long beatsPerMinute, @NonNull Instant time, boolean skipValidation) {
            Objects.requireNonNull(time);
            if (!skipValidation) {
                ValidationUtils.requireInRange(beatsPerMinute, 1, (long) 300, "beatsPerMinute");
            }

            mBeatsPerMinute = beatsPerMinute;
            mTime = time;
        }

        /**
         * @return beats per minute for this sample
         */
        public long getBeatsPerMinute() {
            return mBeatsPerMinute;
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
            if (super.equals(object) && object instanceof HeartRateSample) {
                HeartRateSample other = (HeartRateSample) object;
                return getBeatsPerMinute() == other.getBeatsPerMinute()
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
            return Objects.hash(super.hashCode(), getBeatsPerMinute(), getTime());
        }
    }

    /**
     * Builder class for {@link HeartRateRecord}
     *
     * @see HeartRateRecord
     */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private final List<HeartRateSample> mHeartRateSamples;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param heartRateSamples Samples of recorded heart rate
         * @throws IllegalArgumentException if {@code heartRateSamples} is empty
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull List<HeartRateSample> heartRateSamples) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(heartRateSamples);
            if (heartRateSamples.isEmpty()) {
                throw new IllegalArgumentException("record samples should not be empty");
            }

            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mHeartRateSamples = heartRateSamples;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(startTime);
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(endTime);
        }

        /**
         * Sets the zone offset of the user when the activity started. By default, the starting zone
         * offset is set the current zone offset.
         */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            Objects.requireNonNull(startZoneOffset);

            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /**
         * Sets the zone offset of the user when the activity ended. By default, the end zone offset
         * is set the current zone offset.
         */
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
         * @return Object of {@link HeartRateRecord} without validating the values.
         * @hide
         */
        @NonNull
        public HeartRateRecord buildWithoutValidation() {
            return new HeartRateRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mHeartRateSamples,
                    true);
        }

        /**
         * @return Object of {@link HeartRateRecord}
         */
        @NonNull
        public HeartRateRecord build() {
            return new HeartRateRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mHeartRateSamples,
                    false);
        }
    }

    /** @hide */
    @Override
    public HeartRateRecordInternal toRecordInternal() {
        HeartRateRecordInternal recordInternal =
                (HeartRateRecordInternal)
                        new HeartRateRecordInternal()
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
        Set<HeartRateRecordInternal.HeartRateSample> samples = new HashSet<>(getSamples().size());

        for (HeartRateRecord.HeartRateSample heartRateSample : getSamples()) {
            samples.add(
                    new HeartRateRecordInternal.HeartRateSample(
                            (int) heartRateSample.getBeatsPerMinute(),
                            heartRateSample.getTime().toEpochMilli()));
        }
        recordInternal.setSamples(samples);
        recordInternal.setStartTime(getStartTime().toEpochMilli());
        recordInternal.setEndTime(getEndTime().toEpochMilli());
        recordInternal.setStartZoneOffset(getStartZoneOffset().getTotalSeconds());
        recordInternal.setEndZoneOffset(getEndZoneOffset().getTotalSeconds());

        return recordInternal;
    }
}

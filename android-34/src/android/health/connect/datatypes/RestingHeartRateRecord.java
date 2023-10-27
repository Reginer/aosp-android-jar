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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.RestingHeartRateRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the user's resting heart rate. Each record represents a single instantaneous
 * measurement.
 */
@Identifier(recordIdentifier = RECORD_TYPE_RESTING_HEART_RATE)
public final class RestingHeartRateRecord extends InstantRecord {

    private final long mBeatsPerMinute;

    /**
     * Metric identifier to get max resting heart rate in beats per minute using aggregate APIs in
     * {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Long> BPM_MAX =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.RESTING_HEART_RATE_RECORD_BPM_MAX,
                    AggregationType.MAX,
                    RECORD_TYPE_RESTING_HEART_RATE,
                    Long.class);

    /**
     * Metric identifier to get min resting heart rate in beats per minute using aggregate APIs in
     * {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Long> BPM_MIN =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.RESTING_HEART_RATE_RECORD_BPM_MIN,
                    AggregationType.MIN,
                    RECORD_TYPE_RESTING_HEART_RATE,
                    Long.class);

    /**
     * Metric identifier to get average resting heart rate in beats per minute using aggregate APIs
     * in {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Long> BPM_AVG =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.RESTING_HEART_RATE_RECORD_BPM_AVG,
                    AggregationType.AVG,
                    RECORD_TYPE_RESTING_HEART_RATE,
                    Long.class);

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param beatsPerMinute BeatsPerMinute of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private RestingHeartRateRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @NonNull long beatsPerMinute,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        if (!skipValidation) {
            ValidationUtils.requireInRange(beatsPerMinute, 1, (long) 300, "beatsPerMinute");
        }
        mBeatsPerMinute = beatsPerMinute;
    }
    /**
     * @return beatsPerMinute
     */
    public long getBeatsPerMinute() {
        return mBeatsPerMinute;
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
        RestingHeartRateRecord that = (RestingHeartRateRecord) o;
        return getBeatsPerMinute() == that.getBeatsPerMinute();
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getBeatsPerMinute());
    }

    /** Builder class for {@link RestingHeartRateRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final long mBeatsPerMinute;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param beatsPerMinute Heart beats per minute. Required field. Validation range: 1-300.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @IntRange(from = 1, to = 300) long beatsPerMinute) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            mMetadata = metadata;
            mTime = time;
            mBeatsPerMinute = beatsPerMinute;
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
         * @return Object of {@link RestingHeartRateRecord} without validating the values.
         * @hide
         */
        @NonNull
        public RestingHeartRateRecord buildWithoutValidation() {
            return new RestingHeartRateRecord(mMetadata, mTime, mZoneOffset, mBeatsPerMinute, true);
        }

        /**
         * @return Object of {@link RestingHeartRateRecord}
         */
        @NonNull
        public RestingHeartRateRecord build() {
            return new RestingHeartRateRecord(
                    mMetadata, mTime, mZoneOffset, mBeatsPerMinute, false);
        }
    }

    /** @hide */
    @Override
    public RestingHeartRateRecordInternal toRecordInternal() {
        RestingHeartRateRecordInternal recordInternal =
                (RestingHeartRateRecordInternal)
                        new RestingHeartRateRecordInternal()
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
        recordInternal.setBeatsPerMinute((int) mBeatsPerMinute);
        return recordInternal;
    }
}

/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVITY_INTENSITY;
import static android.health.connect.datatypes.validation.ValidationUtils.validateIntDefValue;

import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier;
import android.health.connect.internal.datatypes.ActivityIntensityRecordInternal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

/**
 * Represents intensity of an activity.
 *
 * <p>Intensity can be either moderate or vigorous.
 *
 * <p>Each record requires the start time, the end time and the activity intensity type.
 */
@FlaggedApi(FLAG_ACTIVITY_INTENSITY)
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_ACTIVITY_INTENSITY)
public final class ActivityIntensityRecord extends IntervalRecord {

    /** Moderate activity intensity. */
    public static final int ACTIVITY_INTENSITY_TYPE_MODERATE = 0;

    /** Vigorous activity intensity. */
    public static final int ACTIVITY_INTENSITY_TYPE_VIGOROUS = 1;

    /**
     * Metric identifier to retrieve the total duration of moderate activity intensity using
     * aggregate APIs in {@link android.health.connect.HealthConnectManager}.
     */
    @NonNull
    public static final AggregationType<Duration> MODERATE_DURATION_TOTAL =
            new AggregationType<>(
                    AggregationTypeIdentifier.ACTIVITY_INTENSITY_MODERATE_DURATION_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_ACTIVITY_INTENSITY,
                    Duration.class);

    /**
     * Metric identifier to retrieve the total duration of vigorous activity intensity using
     * aggregate APIs in {@link android.health.connect.HealthConnectManager}.
     */
    @NonNull
    public static final AggregationType<Duration> VIGOROUS_DURATION_TOTAL =
            new AggregationType<>(
                    AggregationTypeIdentifier.ACTIVITY_INTENSITY_VIGOROUS_DURATION_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_ACTIVITY_INTENSITY,
                    Duration.class);

    /**
     * Metric identifier to retrieve the total duration of activity intensity regardless of the type
     * using aggregate APIs in {@link android.health.connect.HealthConnectManager}.
     *
     * <p>Equivalent to {@link #MODERATE_DURATION_TOTAL} + {@link #VIGOROUS_DURATION_TOTAL}.
     */
    @NonNull
    public static final AggregationType<Duration> DURATION_TOTAL =
            new AggregationType<>(
                    AggregationTypeIdentifier.ACTIVITY_INTENSITY_DURATION_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_ACTIVITY_INTENSITY,
                    Duration.class);

    /**
     * Metric identifier to retrieve the number of weighted intensity minutes using aggregate APIs
     * in {@link android.health.connect.HealthConnectManager}.
     *
     * <p>Records of type {@link #ACTIVITY_INTENSITY_TYPE_MODERATE} contribute their full duration
     * to the result, while records of type {@link #ACTIVITY_INTENSITY_TYPE_VIGOROUS} contribute
     * double their duration.
     *
     * <p>Equivalent to {@link #MODERATE_DURATION_TOTAL} + 2 * {@link #VIGOROUS_DURATION_TOTAL}
     * rounded to minutes.
     *
     * <p>Calculated in minutes.
     */
    @NonNull
    public static final AggregationType<Long> INTENSITY_MINUTES_TOTAL =
            new AggregationType<>(
                    AggregationTypeIdentifier.ACTIVITY_INTENSITY_MINUTES_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_ACTIVITY_INTENSITY,
                    Long.class);

    /**
     * Valid set of values for {@link ActivityIntensityType}. Update this set when add a new type or
     * deprecate an existing type.
     */
    private static final Set<Integer> VALID_ACTIVITY_INTENSITY_TYPES =
            Set.of(ACTIVITY_INTENSITY_TYPE_MODERATE, ACTIVITY_INTENSITY_TYPE_VIGOROUS);

    private final int mActivityIntensityType;

    /**
     * Builds {@link ActivityIntensityRecord} instance
     *
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the session started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the session finished
     * @param activityIntensityType type of the session.
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private ActivityIntensityRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @ActivityIntensityType int activityIntensityType,
            boolean skipValidation) {
        super(
                metadata,
                startTime,
                startZoneOffset,
                endTime,
                endZoneOffset,
                skipValidation,
                /* enforceFutureTimeRestrictions= */ true);
        if (!skipValidation) {
            validateIntDefValue(
                    activityIntensityType,
                    VALID_ACTIVITY_INTENSITY_TYPES,
                    ActivityIntensityType.class.getSimpleName());
        }
        mActivityIntensityType = activityIntensityType;
    }

    /** Returns the type of the activity intensity. */
    @ActivityIntensityType
    public int getActivityIntensityType() {
        return mActivityIntensityType;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityIntensityRecord)) return false;
        if (!super.equals(o)) return false;
        ActivityIntensityRecord that = (ActivityIntensityRecord) o;
        return getActivityIntensityType() == that.getActivityIntensityType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getActivityIntensityType());
    }

    /** @hide */
    @IntDef({ACTIVITY_INTENSITY_TYPE_MODERATE, ACTIVITY_INTENSITY_TYPE_VIGOROUS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActivityIntensityType {}

    /** Builder class for {@link ActivityIntensityRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final int mActivityIntensityType;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity instensity record.
         * @param endTime End time of this activity intensity record.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @ActivityIntensityType int activityIntensityType) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mActivityIntensityType = activityIntensityType;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(startTime);
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(endTime);
        }

        /**
         * Sets the {@link ZoneOffset} of the user when the activity started.
         *
         * <p>Defaults to the system zone offset if not set.
         */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            Objects.requireNonNull(startZoneOffset);

            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /**
         * Sets the {@link ZoneOffset} of the user when the activity ended.
         *
         * <p>Defaults to the system zone offset if not set.
         */
        @NonNull
        public Builder setEndZoneOffset(@NonNull ZoneOffset endZoneOffset) {
            Objects.requireNonNull(endZoneOffset);
            mEndZoneOffset = endZoneOffset;
            return this;
        }

        /**
         * @return Object of {@link ActivityIntensityRecord} without validating the values.
         * @hide
         */
        @NonNull
        public ActivityIntensityRecord buildWithoutValidation() {
            return new ActivityIntensityRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mActivityIntensityType,
                    true);
        }

        /** Returns {@link ActivityIntensityRecord} */
        @NonNull
        public ActivityIntensityRecord build() {
            return new ActivityIntensityRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mActivityIntensityType,
                    false);
        }
    }

    /** @hide */
    @Override
    public ActivityIntensityRecordInternal toRecordInternal() {
        ActivityIntensityRecordInternal recordInternal =
                (ActivityIntensityRecordInternal)
                        new ActivityIntensityRecordInternal()
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
        recordInternal.setActivityIntensityType(getActivityIntensityType());
        return recordInternal;
    }
}

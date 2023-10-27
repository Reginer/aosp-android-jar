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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION;
import static android.health.connect.datatypes.RecordUtils.isEqualNullableCharSequences;
import static android.health.connect.datatypes.validation.ValidationUtils.sortAndValidateTimeIntervalHolders;
import static android.health.connect.datatypes.validation.ValidationUtils.validateIntDefValue;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.internal.datatypes.SleepSessionRecordInternal;
import android.health.connect.internal.datatypes.SleepStageInternal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Captures user sleep session. Each session requires start and end time and a list of {@link
 * Stage}.
 *
 * <p>Each {@link Stage} interval should be between the start time and the end time of the session.
 * Stages within one session must not overlap.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION)
public final class SleepSessionRecord extends IntervalRecord {

    /**
     * Metric identifier to retrieve total sleep session duration using aggregate APIs in {@link
     * android.health.connect.HealthConnectManager}. Calculated in milliseconds.
     */
    @NonNull
    public static final AggregationType<Long> SLEEP_DURATION_TOTAL =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.SLEEP_SESSION_DURATION_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_SLEEP_SESSION,
                    Long.class);

    private final List<Stage> mStages;
    private final CharSequence mNotes;
    private final CharSequence mTitle;
    /**
     * Builds {@link SleepSessionRecord} instance
     *
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the session started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the session finished
     * @param stages list of {@link Stage} of the sleep sessions.
     * @param notes Additional notes for the session. Optional field.
     * @param title Title of the session. Optional field.
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    @SuppressWarnings("unchecked")
    private SleepSessionRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull List<Stage> stages,
            @Nullable CharSequence notes,
            @Nullable CharSequence title,
            boolean skipValidation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset, skipValidation);
        Objects.requireNonNull(stages);
        mStages =
                Collections.unmodifiableList(
                        (List<Stage>)
                                sortAndValidateTimeIntervalHolders(startTime, endTime, stages));
        mNotes = notes;
        mTitle = title;
    }

    /** Returns notes for the sleep session. Returns null if no notes was specified. */
    @Nullable
    public CharSequence getNotes() {
        return mNotes;
    }

    /** Returns title of the sleep session. Returns null if no notes was specified. */
    @Nullable
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Returns stages of the sleep session. */
    @NonNull
    public List<Stage> getStages() {
        return mStages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SleepSessionRecord)) return false;
        if (!super.equals(o)) return false;
        SleepSessionRecord that = (SleepSessionRecord) o;
        return isEqualNullableCharSequences(getNotes(), that.getNotes())
                && isEqualNullableCharSequences(getTitle(), that.getTitle())
                && Objects.equals(getStages(), that.getStages());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getNotes(), getTitle(), getStages());
    }

    /**
     * Captures the user's length and type of sleep. Each record represents a time interval for a
     * stage of sleep.
     *
     * <p>The start time of the record represents the start and end time of the sleep stage and
     * always need to be included.
     */
    public static class Stage implements TimeInterval.TimeIntervalHolder {
        @NonNull private final TimeInterval mInterval;
        @StageType.StageTypes private final int mStageType;

        /**
         * Builds {@link Stage} instance
         *
         * @param startTime start time of the stage
         * @param endTime end time of the stage. Must not be earlier than start time.
         * @param stageType type of the stage. One of {@link StageType}
         */
        public Stage(
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @StageType.StageTypes int stageType) {
            validateIntDefValue(stageType, StageType.VALID_TYPES, StageType.class.getSimpleName());
            this.mInterval = new TimeInterval(startTime, endTime);
            this.mStageType = stageType;
        }

        /** Returns start time of this stage. */
        @NonNull
        public Instant getStartTime() {
            return mInterval.getStartTime();
        }

        /** Returns end time of this stage. */
        @NonNull
        public Instant getEndTime() {
            return mInterval.getEndTime();
        }

        /** Returns stage type. */
        @StageType.StageTypes
        public int getType() {
            return mStageType;
        }

        /** @hide */
        @Override
        public TimeInterval getInterval() {
            return mInterval;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Stage)) return false;
            Stage that = (Stage) o;
            return getType() == that.getType()
                    && getStartTime().toEpochMilli() == that.getStartTime().toEpochMilli()
                    && getEndTime().toEpochMilli() == that.getEndTime().toEpochMilli();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getStartTime(), getEndTime(), mStageType);
        }

        /** @hide */
        public SleepStageInternal toInternalStage() {
            return new SleepStageInternal()
                    .setStartTime(getStartTime().toEpochMilli())
                    .setEndTime(getEndTime().toEpochMilli())
                    .setStageType(getType());
        }
    }

    /** Identifier for sleeping stage, as returned by {@link Stage#getType()}. */
    public static final class StageType {
        /** Use this type if the stage of sleep is unknown. */
        public static final int STAGE_TYPE_UNKNOWN = 0;

        /**
         * The user is awake and either known to be in bed, or it is unknown whether they are in bed
         * or not.
         */
        public static final int STAGE_TYPE_AWAKE = 1;

        /** The user is asleep but the particular stage of sleep (light, deep or REM) is unknown. */
        public static final int STAGE_TYPE_SLEEPING = 2;

        /** The user is out of bed and assumed to be awake. */
        public static final int STAGE_TYPE_AWAKE_OUT_OF_BED = 3;

        /** The user is in a light sleep stage. */
        public static final int STAGE_TYPE_SLEEPING_LIGHT = 4;

        /** The user is in a deep sleep stage. */
        public static final int STAGE_TYPE_SLEEPING_DEEP = 5;

        /** The user is in a REM sleep stage. */
        public static final int STAGE_TYPE_SLEEPING_REM = 6;

        /** The user is awake and in bed. */
        public static final int STAGE_TYPE_AWAKE_IN_BED = 7;

        /**
         * Valid set of values for this IntDef. Update this set when add new type or deprecate
         * existing type.
         *
         * @hide
         */
        public static final Set<Integer> VALID_TYPES =
                Set.of(
                        STAGE_TYPE_UNKNOWN,
                        STAGE_TYPE_AWAKE,
                        STAGE_TYPE_SLEEPING,
                        STAGE_TYPE_AWAKE_OUT_OF_BED,
                        STAGE_TYPE_SLEEPING_LIGHT,
                        STAGE_TYPE_SLEEPING_DEEP,
                        STAGE_TYPE_SLEEPING_REM,
                        STAGE_TYPE_AWAKE_IN_BED);

        private StageType() {}

        /** @hide */
        @IntDef({
            STAGE_TYPE_UNKNOWN,
            STAGE_TYPE_AWAKE,
            STAGE_TYPE_SLEEPING,
            STAGE_TYPE_AWAKE_OUT_OF_BED,
            STAGE_TYPE_SLEEPING_LIGHT,
            STAGE_TYPE_SLEEPING_DEEP,
            STAGE_TYPE_SLEEPING_REM,
            STAGE_TYPE_AWAKE_IN_BED
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface StageTypes {}

        /**
         * Sleep stage types which are excluded from sleep session duration.
         *
         * @hide
         */
        public static final List<Integer> DURATION_EXCLUDE_TYPES =
                List.of(STAGE_TYPE_AWAKE, STAGE_TYPE_AWAKE_OUT_OF_BED, STAGE_TYPE_AWAKE_IN_BED);
    }

    /** Builder class for {@link SleepSessionRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private final List<Stage> mStages;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private CharSequence mNotes;
        private CharSequence mTitle;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this sleep session
         * @param endTime End time of this sleep session
         */
        public Builder(
                @NonNull Metadata metadata, @NonNull Instant startTime, @NonNull Instant endTime) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStages = new ArrayList<>();
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
         * Sets notes for this activity
         *
         * @param notes Additional notes for the session. Optional field.
         */
        @NonNull
        public Builder setNotes(@Nullable CharSequence notes) {
            mNotes = notes;
            return this;
        }

        /**
         * Sets a title of this activity
         *
         * @param title Title of the session. Optional field.
         */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Set stages to this sleep session. Returns Object with updated stages.
         *
         * @param stages list of stages to set
         */
        @NonNull
        public Builder setStages(@NonNull List<Stage> stages) {
            Objects.requireNonNull(stages);
            mStages.clear();
            mStages.addAll(stages);
            return this;
        }

        /**
         * @return Object of {@link SleepSessionRecord} without validating the values.
         * @hide
         */
        @NonNull
        public SleepSessionRecord buildWithoutValidation() {
            return new SleepSessionRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mStages,
                    mNotes,
                    mTitle,
                    true);
        }

        /** Returns {@link SleepSessionRecord} */
        @NonNull
        public SleepSessionRecord build() {
            return new SleepSessionRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mStages,
                    mNotes,
                    mTitle,
                    false);
        }
    }

    /** @hide */
    @Override
    public SleepSessionRecordInternal toRecordInternal() {
        SleepSessionRecordInternal recordInternal =
                (SleepSessionRecordInternal)
                        new SleepSessionRecordInternal()
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
        recordInternal.setSleepStages(
                getStages().stream().map(Stage::toInternalStage).collect(Collectors.toList()));

        if (getNotes() != null) {
            recordInternal.setNotes(getNotes().toString());
        }

        if (getTitle() != null) {
            recordInternal.setTitle(getTitle().toString());
        }
        return recordInternal;
    }
}

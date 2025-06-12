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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.internal.datatypes.PlannedExerciseSessionRecordInternal;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Captures a planned exercise session, also commonly referred to as a training plan.
 *
 * <p>Each record contains a start time, end time, an exercise type and a list of {@link
 * PlannedExerciseBlock} which describe the details of the planned session. The start and end times
 * may be in the future.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_PLANNED_EXERCISE_SESSION)
@FlaggedApi("com.android.healthconnect.flags.training_plans")
public final class PlannedExerciseSessionRecord extends IntervalRecord {
    private final Boolean mHasExplicitTime;

    @ExerciseSessionType.ExerciseSessionTypes private final int mExerciseType;

    @Nullable private final CharSequence mTitle;

    @Nullable private final CharSequence mNotes;

    private final List<PlannedExerciseBlock> mBlocks;

    @Nullable private final String mCompletedExerciseSessionId;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this planned session. May be in the future.
     * @param startZoneOffset Zone offset of the user when the planned session should start.
     * @param endTime End time of this planned session. May be in the future.
     * @param endZoneOffset Zone offset of the user when the planned session should end.
     * @param hasExplicitTime Whether an explicit time value has been provided.
     * @param title The title of this planned session.
     * @param notes Notes for this planned session.
     * @param exerciseType The exercise type of this planned s.ession. Allowed values: {@link
     *     ExerciseSessionType.ExerciseSessionTypes }.
     * @param blocks The {@link PlannedExerciseBlock} that contain the details of this planned
     *     session.
     * @param completedExerciseSessionId The id of the exercise session that completed this planned
     *     exercise.
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private PlannedExerciseSessionRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull Boolean hasExplicitTime,
            @Nullable CharSequence title,
            @Nullable CharSequence notes,
            @NonNull @ExerciseSessionType.ExerciseSessionTypes int exerciseType,
            @NonNull List<PlannedExerciseBlock> blocks,
            @Nullable String completedExerciseSessionId,
            boolean skipValidation) {
        super(
                metadata,
                startTime,
                startZoneOffset,
                endTime,
                endZoneOffset,
                skipValidation,
                /* enforceFutureTimeRestrictions= */ false);
        mHasExplicitTime = hasExplicitTime;
        mTitle = title;
        mNotes = notes;
        mExerciseType = exerciseType;
        mBlocks = blocks;
        mCompletedExerciseSessionId = completedExerciseSessionId;
    }

    /** Returns the exercise type of this planned session. */
    @ExerciseSessionType.ExerciseSessionTypes
    public int getExerciseType() {
        return mExerciseType;
    }

    /** Returns notes for this planned session. Returns null if it doesn't have notes. */
    @Nullable
    public CharSequence getNotes() {
        return mNotes;
    }

    /** Returns title of this planned session. Returns null if it doesn't have a title. */
    @Nullable
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Returns the start date of the planned session. */
    @NonNull
    public LocalDate getStartDate() {
        return getStartTime().atOffset(getStartZoneOffset()).toLocalDate();
    }

    /** Returns the expected duration of the planned session. */
    @NonNull
    public Duration getDuration() {
        return Duration.between(getStartTime(), getEndTime());
    }

    /**
     * Returns whether this planned session has an explicit time. If only a date was provided this
     * will be false.
     */
    public boolean hasExplicitTime() {
        return mHasExplicitTime;
    }

    /**
     * Returns the id of exercise session that completed this planned session. Returns null if none
     * exists.
     */
    @Nullable
    public String getCompletedExerciseSessionId() {
        return mCompletedExerciseSessionId;
    }

    /**
     * Returns the exercise blocks for this step.
     *
     * @return An unmodifiable list of {@link PlannedExerciseBlock}.
     */
    @NonNull
    public List<PlannedExerciseBlock> getBlocks() {
        return mBlocks;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof PlannedExerciseSessionRecord)) return false;
        if (!super.equals(o)) return false;
        PlannedExerciseSessionRecord that = (PlannedExerciseSessionRecord) o;
        return RecordUtils.isEqualNullableCharSequences(this.getNotes(), that.getNotes())
                && RecordUtils.isEqualNullableCharSequences(this.getTitle(), that.getTitle())
                && this.getExerciseType() == that.getExerciseType()
                && this.hasExplicitTime() == that.hasExplicitTime()
                && Objects.equals(
                        this.getCompletedExerciseSessionId(), that.getCompletedExerciseSessionId())
                && Objects.equals(this.getBlocks(), that.getBlocks());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                getNotes(),
                getTitle(),
                getExerciseType(),
                hasExplicitTime(),
                this.getCompletedExerciseSessionId(),
                this.getBlocks());
    }

    /** Builder class for {@link PlannedExerciseSessionRecord}. */
    public static final class Builder {
        @NonNull private Metadata mMetadata;
        @NonNull private Instant mStartTime;
        @NonNull private Instant mEndTime;
        @NonNull private ZoneOffset mStartZoneOffset;
        @NonNull private ZoneOffset mEndZoneOffset;
        @ExerciseSessionType.ExerciseSessionTypes private int mExerciseType;
        @Nullable private CharSequence mNotes;
        @Nullable private CharSequence mTitle;
        private Boolean mHasExplicitTime;
        private final List<PlannedExerciseBlock> mBlocks = new ArrayList<>();
        @Nullable private String mCompletedExerciseSessionId;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param exerciseType Type of exercise (e.g. walking, swimming). Allowed values: {@link
         *     ExerciseSessionType}.
         * @param startTime Expected start time of this planned session.
         * @param endTime Expected end time of this planned session.
         */
        public Builder(
                @NonNull Metadata metadata,
                @ExerciseSessionType.ExerciseSessionTypes int exerciseType,
                @NonNull Instant startTime,
                @NonNull Instant endTime) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mExerciseType = exerciseType;
            mHasExplicitTime = true;
            mStartTime = startTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(mStartTime);
            mEndTime = endTime;
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(mEndTime);
        }

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param exerciseType Type of exercise (e.g. walking, swimming). Required field. Allowed
         *     values: {@link ExerciseSessionType}.
         * @param startDate The start date of this planned session. The underlying time of the
         *     session will be set to noon of the specified day. The end time will be determined by
         *     adding the duration to this.
         * @param duration The expected duration of the planned session.
         */
        public Builder(
                @NonNull Metadata metadata,
                @ExerciseSessionType.ExerciseSessionTypes int exerciseType,
                @NonNull LocalDate startDate,
                @NonNull Duration duration) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startDate);
            Objects.requireNonNull(duration);
            mMetadata = metadata;
            mExerciseType = exerciseType;
            mHasExplicitTime = false;
            mStartTime =
                    startDate.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant();
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(mStartTime);
            mEndTime = mStartTime.plus(duration);
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(mEndTime);
        }

        /** Set the metadata for the record. */
        @NonNull
        public Builder setMetadata(@NonNull Metadata metadata) {
            Objects.requireNonNull(metadata);
            this.mMetadata = metadata;
            return this;
        }

        /** Sets the exercise type. */
        @NonNull
        public Builder setExerciseType(@ExerciseSessionType.ExerciseSessionTypes int exerciseType) {
            this.mExerciseType = exerciseType;
            return this;
        }

        /** Sets the planned start time of the session. */
        @NonNull
        public Builder setStartTime(@NonNull Instant startTime) {
            Objects.requireNonNull(startTime);
            this.mStartTime = startTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(mStartTime);
            return this;
        }

        /** Sets the planned end time of the session. */
        @NonNull
        public Builder setEndTime(@NonNull Instant endTime) {
            Objects.requireNonNull(endTime);
            this.mEndTime = endTime;
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(mEndTime);
            return this;
        }

        /** Sets the zone offset of when the workout should start. */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            Objects.requireNonNull(startZoneOffset);
            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /** Sets the zone offset of when the workout should end. */
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
         * Sets notes for this activity.
         *
         * @param notes Notes for this activity.
         */
        @NonNull
        public Builder setNotes(@Nullable CharSequence notes) {
            mNotes = notes;
            return this;
        }

        /**
         * Sets a title of this planned session.
         *
         * @param title Title of this activity.
         */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Adds a block to this planned session..
         *
         * @param block An {@link PlannedExerciseBlock} to add to this planned session..
         */
        @NonNull
        public Builder addBlock(@NonNull PlannedExerciseBlock block) {
            Objects.requireNonNull(block);
            mBlocks.add(block);
            return this;
        }

        /**
         * Sets the blocks of this planned session.
         *
         * @param blocks A list of {@link PlannedExerciseBlock} to set for this planned session.
         */
        @NonNull
        public Builder setBlocks(@NonNull List<PlannedExerciseBlock> blocks) {
            Objects.requireNonNull(blocks);
            mBlocks.clear();
            mBlocks.addAll(blocks);
            return this;
        }

        /** Clears the blocks of this planned session. */
        @NonNull
        public Builder clearBlocks() {
            mBlocks.clear();
            return this;
        }

        /**
         * Set exercise session ID of the workout that completed this planned session.
         *
         * @hide
         */
        @NonNull
        public Builder setCompletedExerciseSessionId(@Nullable String id) {
            this.mCompletedExerciseSessionId = id;
            return this;
        }

        /**
         * @return Object of {@link PlannedExerciseSessionRecord} without validating the values.
         * @hide
         */
        @NonNull
        public PlannedExerciseSessionRecord buildWithoutValidation() {
            return new PlannedExerciseSessionRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mHasExplicitTime,
                    mTitle,
                    mNotes,
                    mExerciseType,
                    List.copyOf(mBlocks),
                    mCompletedExerciseSessionId,
                    true);
        }

        /** Returns {@link PlannedExerciseSessionRecord}. */
        @NonNull
        public PlannedExerciseSessionRecord build() {
            if (!ExerciseSessionType.isKnownSessionType(mExerciseType)) {
                throw new IllegalArgumentException("Invalid exercise session type");
            }
            return new PlannedExerciseSessionRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mHasExplicitTime,
                    mTitle,
                    mNotes,
                    mExerciseType,
                    List.copyOf(mBlocks),
                    mCompletedExerciseSessionId,
                    false);
        }
    }

    /** @hide */
    @Override
    public PlannedExerciseSessionRecordInternal toRecordInternal() {
        PlannedExerciseSessionRecordInternal recordInternal =
                (PlannedExerciseSessionRecordInternal)
                        new PlannedExerciseSessionRecordInternal()
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
        if (getNotes() != null) {
            recordInternal.setNotes(getNotes().toString());
        }
        if (getTitle() != null) {
            recordInternal.setTitle(getTitle().toString());
        }
        recordInternal.setExerciseType(getExerciseType());
        recordInternal.setHasExplicitTime(hasExplicitTime());
        // Although not possible to set this via public API, internally we may convert from internal
        // representation to external, then back to internal. Thus, we need to preserve this value
        // during a round trip.
        if (getCompletedExerciseSessionId() != null) {
            recordInternal.setCompletedExerciseSessionId(
                    UUID.fromString(getCompletedExerciseSessionId()));
        }
        recordInternal.setExerciseBlocks(
                getBlocks().stream().map(it -> it.toInternalObject()).collect(Collectors.toList()));
        return recordInternal;
    }
}

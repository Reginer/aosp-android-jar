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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION;
import static android.health.connect.datatypes.validation.ValidationUtils.sortAndValidateTimeIntervalHolders;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.validation.ExerciseSessionTypesValidation;
import android.health.connect.internal.datatypes.ExerciseSessionRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Captures exercise or a sequence of exercises. This can be a playing game like football or a
 * sequence of fitness exercises.
 *
 * <p>Each record needs a start time, end time and session type. In addition, each record has two
 * optional independent lists of time intervals: {@link ExerciseSegment} represents particular
 * exercise within session, {@link ExerciseLap} represents a lap time within session.
 */
@Identifier(recordIdentifier = RECORD_TYPE_EXERCISE_SESSION)
public final class ExerciseSessionRecord extends IntervalRecord {

    /**
     * Metric identifier to retrieve total exercise session duration using aggregate APIs in {@link
     * android.health.connect.HealthConnectManager}. Calculated in milliseconds.
     */
    @NonNull
    public static final AggregationType<Long> EXERCISE_DURATION_TOTAL =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.EXERCISE_SESSION_DURATION_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_EXERCISE_SESSION,
                    Long.class);

    private final int mExerciseType;

    private final CharSequence mNotes;
    private final CharSequence mTitle;
    private final ExerciseRoute mRoute;

    // Represents if the route is recorded for this session, even if mRoute is null.
    private final boolean mHasRoute;

    private final List<ExerciseSegment> mSegments;
    private final List<ExerciseLap> mLaps;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param notes Notes for this activity
     * @param exerciseType Type of exercise (e.g. walking, swimming). Required field. Allowed
     *     values: {@link ExerciseSessionType.ExerciseSessionTypes }
     * @param title Title of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    @SuppressWarnings("unchecked")
    private ExerciseSessionRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @Nullable CharSequence notes,
            @NonNull @ExerciseSessionType.ExerciseSessionTypes int exerciseType,
            @Nullable CharSequence title,
            @Nullable ExerciseRoute route,
            boolean hasRoute,
            @NonNull List<ExerciseSegment> segments,
            @NonNull List<ExerciseLap> laps,
            boolean skipValidation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset, skipValidation);
        mNotes = notes;
        mExerciseType = exerciseType;
        mTitle = title;
        if (route != null && !hasRoute) {
            throw new IllegalArgumentException("HasRoute must be true if the route is not null");
        }
        mRoute = route;
        if (!skipValidation) {
            ExerciseSessionTypesValidation.validateExerciseRouteTimestamps(
                    startTime, endTime, route);
        }
        mHasRoute = hasRoute;
        mSegments =
                Collections.unmodifiableList(
                        (List<ExerciseSegment>)
                                sortAndValidateTimeIntervalHolders(startTime, endTime, segments));
        if (!skipValidation) {
            ExerciseSessionTypesValidation.validateSessionAndSegmentsTypes(exerciseType, mSegments);
        }
        mLaps =
                Collections.unmodifiableList(
                        (List<ExerciseLap>)
                                sortAndValidateTimeIntervalHolders(startTime, endTime, laps));
    }

    /** Returns exerciseType of this session. */
    @ExerciseSessionType.ExerciseSessionTypes
    public int getExerciseType() {
        return mExerciseType;
    }

    /** Returns notes for this activity. Returns null if the session doesn't have notes. */
    @Nullable
    public CharSequence getNotes() {
        return mNotes;
    }

    /** Returns title of this session. Returns null if the session doesn't have title. */
    @Nullable
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Returns route of this session. Returns null if the session doesn't have route. */
    @Nullable
    public ExerciseRoute getRoute() {
        return mRoute;
    }

    /**
     * Returns segments of this session. Returns empty list if the session doesn't have exercise
     * segments.
     */
    @NonNull
    public List<ExerciseSegment> getSegments() {
        return mSegments;
    }

    /**
     * Returns laps of this session. Returns empty list if the session doesn't have exercise laps.
     */
    @NonNull
    public List<ExerciseLap> getLaps() {
        return mLaps;
    }

    /** Returns if this session has recorded route. */
    @NonNull
    public boolean hasRoute() {
        return mHasRoute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExerciseSessionRecord)) return false;
        if (!super.equals(o)) return false;
        ExerciseSessionRecord that = (ExerciseSessionRecord) o;
        return getExerciseType() == that.getExerciseType()
                && RecordUtils.isEqualNullableCharSequences(getNotes(), that.getNotes())
                && RecordUtils.isEqualNullableCharSequences(getTitle(), that.getTitle())
                && Objects.equals(getRoute(), that.getRoute())
                && Objects.equals(getSegments(), that.getSegments())
                && Objects.equals(getLaps(), that.getLaps());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                getExerciseType(),
                getNotes(),
                getTitle(),
                getRoute(),
                getSegments(),
                getLaps());
    }

    /** Builder class for {@link ExerciseSessionRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final int mExerciseType;
        private CharSequence mNotes;
        private CharSequence mTitle;
        private ExerciseRoute mRoute;
        private final List<ExerciseSegment> mSegments;
        private final List<ExerciseLap> mLaps;
        private boolean mHasRoute;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param exerciseType Type of exercise (e.g. walking, swimming). Required field. Allowed
         *     values: {@link ExerciseSessionType}
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @ExerciseSessionType.ExerciseSessionTypes int exerciseType) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mExerciseType = exerciseType;
            mSegments = new ArrayList<>();
            mLaps = new ArrayList<>();
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(startTime);
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(endTime);
        }

        /** Sets the zone offset of the user when the session started */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            Objects.requireNonNull(startZoneOffset);

            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /** Sets the zone offset of the user when the session ended */
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
         * @param notes Notes for this activity
         */
        @NonNull
        public Builder setNotes(@Nullable CharSequence notes) {
            mNotes = notes;
            return this;
        }

        /**
         * Sets a title of this activity
         *
         * @param title Title of this activity
         */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets route for this activity
         *
         * @param route ExerciseRoute for this activity
         */
        @NonNull
        public Builder setRoute(@Nullable ExerciseRoute route) {
            mRoute = route;
            mHasRoute = (route != null);
            return this;
        }

        /**
         * Sets segments for this session.
         *
         * @param laps list of {@link ExerciseLap} of this session
         */
        @NonNull
        public Builder setLaps(@NonNull List<ExerciseLap> laps) {
            Objects.requireNonNull(laps);
            mLaps.clear();
            mLaps.addAll(laps);
            return this;
        }

        /**
         * Sets segments for this session.
         *
         * @param segments list of {@link ExerciseSegment} of this session
         */
        @NonNull
        public Builder setSegments(@NonNull List<ExerciseSegment> segments) {
            Objects.requireNonNull(segments);
            mSegments.clear();
            mSegments.addAll(segments);
            return this;
        }

        /**
         * Sets if the session contains route. Set by platform to indicate whether the route can be
         * requested via UI intent.
         *
         * @param hasRoute flag whether the session has recorded {@link ExerciseRoute}
         * @hide
         */
        @NonNull
        public Builder setHasRoute(boolean hasRoute) {
            mHasRoute = hasRoute;
            return this;
        }

        /**
         * @return Object of {@link ExerciseSessionRecord} without validating the values.
         * @hide
         */
        @NonNull
        public ExerciseSessionRecord buildWithoutValidation() {
            return new ExerciseSessionRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mNotes,
                    mExerciseType,
                    mTitle,
                    mRoute,
                    mHasRoute,
                    mSegments,
                    mLaps,
                    true);
        }

        /** Returns {@link ExerciseSessionRecord} */
        @NonNull
        public ExerciseSessionRecord build() {
            return new ExerciseSessionRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mNotes,
                    mExerciseType,
                    mTitle,
                    mRoute,
                    mHasRoute,
                    mSegments,
                    mLaps,
                    false);
        }
    }

    /** @hide */
    @Override
    public ExerciseSessionRecordInternal toRecordInternal() {
        ExerciseSessionRecordInternal recordInternal =
                (ExerciseSessionRecordInternal)
                        new ExerciseSessionRecordInternal()
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

        if (getRoute() != null) {
            recordInternal.setRoute(getRoute().toRouteInternal());
        }

        if (getLaps() != null && !getLaps().isEmpty()) {
            recordInternal.setExerciseLaps(
                    getLaps().stream()
                            .map(ExerciseLap::toExerciseLapInternal)
                            .collect(Collectors.toList()));
        }

        if (getSegments() != null && !getSegments().isEmpty()) {
            recordInternal.setExerciseSegments(
                    getSegments().stream()
                            .map(ExerciseSegment::toSegmentInternal)
                            .collect(Collectors.toList()));
        }
        recordInternal.setExerciseType(mExerciseType);
        return recordInternal;
    }
}

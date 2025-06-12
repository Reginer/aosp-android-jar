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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_MINDFULNESS_SESSION;
import static android.health.connect.datatypes.RecordUtils.isEqualNullableCharSequences;
import static android.health.connect.datatypes.validation.ValidationUtils.validateIntDefValue;

import static com.android.healthfitness.flags.Flags.FLAG_MINDFULNESS;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.internal.datatypes.MindfulnessSessionRecordInternal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

/**
 * Captures a mindfulness session.
 *
 * <p>For example: yoga, meditation, guided breathing, etc.
 *
 * <p>Each record needs a start time, end time and a mindfulness session type. In addition, each
 * record has an optional title and notes.
 */
@FlaggedApi(FLAG_MINDFULNESS)
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_MINDFULNESS_SESSION)
public final class MindfulnessSessionRecord extends IntervalRecord {

    /**
     * Metric identifier to retrieve total mindfulness session duration using aggregate APIs in
     * {@link android.health.connect.HealthConnectManager}. Calculated in milliseconds.
     */
    @NonNull
    public static final AggregationType<Long> MINDFULNESS_DURATION_TOTAL =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.MINDFULNESS_SESSION_DURATION_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_MINDFULNESS_SESSION,
                    Long.class);

    /** Use this type if the mindfulness session type is unknown. */
    public static final int MINDFULNESS_SESSION_TYPE_UNKNOWN = 0;

    /** Meditation mindfulness session. */
    public static final int MINDFULNESS_SESSION_TYPE_MEDITATION = 1;

    /** Other mindfulness session. */
    public static final int MINDFULNESS_SESSION_TYPE_OTHER = 2;

    /** Guided breathing mindfulness session. */
    public static final int MINDFULNESS_SESSION_TYPE_BREATHING = 3;

    /** Music/soundscapes mindfulness session. */
    public static final int MINDFULNESS_SESSION_TYPE_MUSIC = 4;

    /** Stretches/movement mindfulness session. */
    public static final int MINDFULNESS_SESSION_TYPE_MOVEMENT = 5;

    /** Unguided mindfulness session. */
    public static final int MINDFULNESS_SESSION_TYPE_UNGUIDED = 6;

    /**
     * Valid set of values for this IntDef. Update this set when add new type or deprecate existing
     * type.
     */
    private static final Set<Integer> VALID_MINDFULNESS_SESSION_TYPES =
            Set.of(
                    MINDFULNESS_SESSION_TYPE_UNKNOWN,
                    MINDFULNESS_SESSION_TYPE_MEDITATION,
                    MINDFULNESS_SESSION_TYPE_OTHER,
                    MINDFULNESS_SESSION_TYPE_BREATHING,
                    MINDFULNESS_SESSION_TYPE_MUSIC,
                    MINDFULNESS_SESSION_TYPE_MOVEMENT,
                    MINDFULNESS_SESSION_TYPE_UNGUIDED);

    private final int mMindfulnessSessionType;
    @Nullable private final CharSequence mTitle;
    @Nullable private final CharSequence mNotes;

    /**
     * Builds {@link MindfulnessSessionRecord} instance
     *
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the session started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the session finished
     * @param mindfulnessSessionType type of the session.
     * @param title Title of the session. Optional field.
     * @param notes Additional notes for the session. Optional field.
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private MindfulnessSessionRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @MindfulnessSessionType int mindfulnessSessionType,
            @Nullable CharSequence title,
            @Nullable CharSequence notes,
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
                    mindfulnessSessionType,
                    VALID_MINDFULNESS_SESSION_TYPES,
                    MindfulnessSessionType.class.getSimpleName());
        }
        mMindfulnessSessionType = mindfulnessSessionType;
        mTitle = title;
        mNotes = notes;
    }

    /** Returns type of the mindfulness session. */
    @MindfulnessSessionType
    public int getMindfulnessSessionType() {
        return mMindfulnessSessionType;
    }

    /** Returns title of the mindfulness session. Returns null if no title was specified. */
    @Nullable
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Returns notes for the mindfulness session. Returns null if no notes was specified. */
    @Nullable
    public CharSequence getNotes() {
        return mNotes;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof MindfulnessSessionRecord)) return false;
        if (!super.equals(o)) return false;
        MindfulnessSessionRecord that = (MindfulnessSessionRecord) o;
        return getMindfulnessSessionType() == that.getMindfulnessSessionType()
                && isEqualNullableCharSequences(getTitle(), that.getTitle())
                && isEqualNullableCharSequences(getNotes(), that.getNotes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getMindfulnessSessionType(), getTitle(), getNotes());
    }

    /** @hide */
    @IntDef({
        MINDFULNESS_SESSION_TYPE_UNKNOWN,
        MINDFULNESS_SESSION_TYPE_MEDITATION,
        MINDFULNESS_SESSION_TYPE_OTHER,
        MINDFULNESS_SESSION_TYPE_BREATHING,
        MINDFULNESS_SESSION_TYPE_MUSIC,
        MINDFULNESS_SESSION_TYPE_MOVEMENT,
        MINDFULNESS_SESSION_TYPE_UNGUIDED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MindfulnessSessionType {}

    /** Builder class for {@link MindfulnessSessionRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final int mMindfulnessSessionType;
        @Nullable private CharSequence mTitle;
        @Nullable private CharSequence mNotes;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this mindfulness session
         * @param endTime End time of this mindfulness session
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @MindfulnessSessionType int mindfulnessSessionType) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mMindfulnessSessionType = mindfulnessSessionType;
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
         * @return Object of {@link MindfulnessSessionRecord} without validating the values.
         * @hide
         */
        @NonNull
        public MindfulnessSessionRecord buildWithoutValidation() {
            return new MindfulnessSessionRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mMindfulnessSessionType,
                    mTitle,
                    mNotes,
                    true);
        }

        /** Returns {@link MindfulnessSessionRecord} */
        @NonNull
        public MindfulnessSessionRecord build() {
            return new MindfulnessSessionRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mMindfulnessSessionType,
                    mTitle,
                    mNotes,
                    false);
        }
    }

    /** @hide */
    @Override
    public MindfulnessSessionRecordInternal toRecordInternal() {
        MindfulnessSessionRecordInternal recordInternal =
                (MindfulnessSessionRecordInternal)
                        new MindfulnessSessionRecordInternal()
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
        recordInternal.setMindfulnessSessionType(getMindfulnessSessionType());
        if (getTitle() != null) {
            recordInternal.setTitle(getTitle().toString());
        }
        if (getNotes() != null) {
            recordInternal.setNotes(getNotes().toString());
        }
        return recordInternal;
    }
}

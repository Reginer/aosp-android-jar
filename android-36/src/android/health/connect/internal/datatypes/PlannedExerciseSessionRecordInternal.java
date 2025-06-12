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
package android.health.connect.internal.datatypes;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.PlannedExerciseSessionRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a planned workout that should be completed by the user.
 *
 * @hide
 * @see PlannedExerciseSessionRecord
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_PLANNED_EXERCISE_SESSION)
public final class PlannedExerciseSessionRecordInternal
        extends IntervalRecordInternal<PlannedExerciseSessionRecord> {
    @Nullable private String mNotes;

    private int mExerciseType;

    @Nullable private String mTitle;

    private boolean mHasExplicitTime;

    private List<PlannedExerciseBlockInternal> mExerciseBlocks = Collections.emptyList();

    @Nullable private UUID mCompletedExerciseSessionId;

    @Nullable
    public String getNotes() {
        return mNotes;
    }

    /** returns this object with the specified notes. */
    @NonNull
    public PlannedExerciseSessionRecordInternal setNotes(String notes) {
        this.mNotes = notes;
        return this;
    }

    /** returns the title of this planned workout. */
    @Nullable
    public String getTitle() {
        return mTitle;
    }

    /** returns this object with the specified title. */
    @NonNull
    public PlannedExerciseSessionRecordInternal setTitle(String title) {
        this.mTitle = title;
        return this;
    }

    /** returns this object with the specified notes. */
    @NonNull
    public PlannedExerciseSessionRecordInternal setHasExplicitTime(boolean hasExplicitTime) {
        this.mHasExplicitTime = hasExplicitTime;
        return this;
    }

    /** returns this object with the specified {@link PlannedExerciseBlockInternal} entries. */
    @NonNull
    public PlannedExerciseSessionRecordInternal setExerciseBlocks(
            List<PlannedExerciseBlockInternal> blocks) {
        this.mExerciseBlocks = blocks;
        return this;
    }

    /** returns this object with the specified completed exercise session id. */
    @NonNull
    public PlannedExerciseSessionRecordInternal setCompletedExerciseSessionId(UUID id) {
        this.mCompletedExerciseSessionId = id;
        return this;
    }

    /** returns the {@link PlannedExerciseBlockInternal} entries associated with this object. */
    @NonNull
    public List<PlannedExerciseBlockInternal> getExerciseBlocks() {
        return mExerciseBlocks;
    }

    /** returns the UUID of the exercise session which completed this plan, if it exists. */
    @Nullable
    public UUID getCompletedExerciseSessionId() {
        return mCompletedExerciseSessionId;
    }

    /** returns whether this object has an explicit time set. */
    public boolean getHasExplicitTime() {
        return mHasExplicitTime;
    }

    /** returns the exercise type of this planned workout. */
    @ExerciseSessionType.ExerciseSessionTypes
    public int getExerciseType() {
        return mExerciseType;
    }

    /** returns this object with the specified exercise type. */
    @NonNull
    public PlannedExerciseSessionRecordInternal setExerciseType(int exerciseType) {
        this.mExerciseType = exerciseType;
        return this;
    }

    @Override
    void populateIntervalRecordFrom(Parcel parcel) {
        mNotes = parcel.readString();
        mExerciseType = parcel.readInt();
        mTitle = parcel.readString();
        mHasExplicitTime = parcel.readBoolean();
        mExerciseBlocks = PlannedExerciseBlockInternal.readFromParcel(parcel);
        String uuid = parcel.readString();
        mCompletedExerciseSessionId = uuid == null ? null : UUID.fromString(uuid);
    }

    @Override
    void populateIntervalRecordTo(Parcel parcel) {
        parcel.writeString(mNotes);
        parcel.writeInt(mExerciseType);
        parcel.writeString(mTitle);
        parcel.writeBoolean(mHasExplicitTime);
        PlannedExerciseBlockInternal.writeToParcel(mExerciseBlocks, parcel);
        parcel.writeString(
                mCompletedExerciseSessionId == null
                        ? null
                        : mCompletedExerciseSessionId.toString());
    }

    /** Convert this object to an external representation. */
    @Override
    public PlannedExerciseSessionRecord toExternalRecord() {
        PlannedExerciseSessionRecord.Builder builder =
                mHasExplicitTime
                        ? new PlannedExerciseSessionRecord.Builder(
                                buildMetaData(), getExerciseType(), getStartTime(), getEndTime())
                        : new PlannedExerciseSessionRecord.Builder(
                                buildMetaData(),
                                getExerciseType(),
                                getStartTime().atOffset(getStartZoneOffset()).toLocalDate(),
                                Duration.between(getStartTime(), getEndTime()));

        if (getStartZoneOffset() != null) {
            builder.setStartZoneOffset(getStartZoneOffset());
        }
        if (getEndZoneOffset() != null) {
            builder.setEndZoneOffset(getEndZoneOffset());
        }
        if (getNotes() != null) {
            builder.setNotes(getNotes());
        }
        if (getTitle() != null) {
            builder.setTitle(getTitle());
        }
        for (PlannedExerciseBlockInternal block : mExerciseBlocks) {
            builder.addBlock(block.toExternalObject());
        }
        if (mCompletedExerciseSessionId != null) {
            builder.setCompletedExerciseSessionId(mCompletedExerciseSessionId.toString());
        }
        return builder.buildWithoutValidation();
    }
}

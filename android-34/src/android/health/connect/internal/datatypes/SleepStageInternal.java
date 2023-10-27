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

package android.health.connect.internal.datatypes;

import android.annotation.NonNull;
import android.health.connect.datatypes.SleepSessionRecord;
import android.os.Parcel;

import com.android.internal.annotations.VisibleForTesting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Internal {@link SleepSessionRecord.Stage}. Part of {@link SleepSessionRecordInternal}
 *
 * @hide
 */
public final class SleepStageInternal {
    private long mStartTime;
    private long mEndTime;
    @SleepSessionRecord.StageType.StageTypes private int mStageType;

    /** Reads record from parcel. */
    @VisibleForTesting
    public static SleepStageInternal readFromParcel(Parcel parcel) {
        return new SleepStageInternal()
                .setStartTime(parcel.readLong())
                .setEndTime(parcel.readLong())
                .setStageType(parcel.readInt());
    }

    static List<SleepStageInternal> populateStagesFromParcel(Parcel parcel) {
        int size = parcel.readInt();
        if (size == 0) {
            return null;
        }
        ArrayList<SleepStageInternal> stages = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stages.add(SleepStageInternal.readFromParcel(parcel));
        }
        return stages;
    }

    static List<SleepSessionRecord.Stage> getExternalStages(
            @NonNull List<SleepStageInternal> internalStages) {
        List<SleepSessionRecord.Stage> externalStages = new ArrayList<>(internalStages.size());
        internalStages.forEach((stage) -> externalStages.add(stage.toExternalRecord()));
        return externalStages;
    }

    static void writeStagesToParcel(List<SleepStageInternal> stages, Parcel parcel) {
        if (stages == null) {
            parcel.writeInt(0);
            return;
        }
        parcel.writeInt(stages.size());
        stages.forEach((stage) -> stage.writeToParcel(parcel));
    }

    /** Writes record to parcel. */
    @VisibleForTesting
    public void writeToParcel(Parcel parcel) {
        parcel.writeLong(mStartTime);
        parcel.writeLong(mEndTime);
        parcel.writeInt(mStageType);
    }

    /** Sets stage type. Returns record with type set. */
    @VisibleForTesting
    public SleepSessionRecord.Stage toExternalRecord() {
        return new SleepSessionRecord.Stage(
                Instant.ofEpochMilli(getStartTime()),
                Instant.ofEpochMilli(getEndTime()),
                getStageType());
    }

    /** Sets stage start time. Returns record with start time set. */
    public SleepStageInternal setStartTime(long startTime) {
        mStartTime = startTime;
        return this;
    }

    /** Sets stage end time. Returns record with end time set. */
    public SleepStageInternal setEndTime(long endTime) {
        mEndTime = endTime;
        return this;
    }

    /** Returns stage start time. */
    public long getStartTime() {
        return mStartTime;
    }

    /** Returns stage end time. */
    public long getEndTime() {
        return mEndTime;
    }

    /** Returns stage type. */
    @SleepSessionRecord.StageType.StageTypes
    public int getStageType() {
        return mStageType;
    }

    /** Sets stage type. Returns record with type set. */
    public SleepStageInternal setStageType(@SleepSessionRecord.StageType.StageTypes int stageType) {
        mStageType = stageType;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SleepStageInternal)) return false;
        SleepStageInternal that = (SleepStageInternal) o;
        return mStartTime == that.mStartTime
                && mEndTime == that.mEndTime
                && mStageType == that.mStageType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStartTime, mEndTime, mStageType);
    }
}

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
import android.health.connect.datatypes.ExerciseLap;
import android.health.connect.datatypes.units.Length;
import android.os.Parcel;

import com.android.internal.annotations.VisibleForTesting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Internal ExerciseLap. Part of {@link ExerciseSessionRecordInternal}
 *
 * @hide
 */
public class ExerciseLapInternal {

    private long mStartTime;
    private long mEndTime;
    private double mLength;

    /** Reads lap from parcel. */
    @VisibleForTesting
    public static ExerciseLapInternal readFromParcel(Parcel parcel) {
        return new ExerciseLapInternal()
                .setStarTime((parcel.readLong()))
                .setEndTime(parcel.readLong())
                .setLength(parcel.readDouble());
    }

    static void writeLapsToParcel(List<ExerciseLapInternal> laps, Parcel parcel) {
        if (laps == null) {
            parcel.writeInt(0);
            return;
        }

        parcel.writeInt(laps.size());
        for (ExerciseLapInternal lap : laps) {
            lap.writeToParcel(parcel);
        }
    }

    static List<ExerciseLap> getExternalLaps(@NonNull List<ExerciseLapInternal> internalLaps) {
        List<ExerciseLap> externalLaps = new ArrayList<>(internalLaps.size());
        internalLaps.forEach((lap) -> externalLaps.add(lap.toExternalRecord()));
        return externalLaps;
    }

    static List<ExerciseLapInternal> populateLapsFromParcel(Parcel parcel) {
        int lapsSize = parcel.readInt();
        if (lapsSize == 0) {
            return null;
        }

        ArrayList<ExerciseLapInternal> laps = new ArrayList<>(lapsSize);
        for (int i = 0; i < lapsSize; i++) {
            laps.add(ExerciseLapInternal.readFromParcel(parcel));
        }
        return laps;
    }

    /** Returns laps start time. */
    @VisibleForTesting
    public void writeToParcel(Parcel parcel) {
        parcel.writeLong(mStartTime);
        parcel.writeLong(mEndTime);
        parcel.writeDouble(mLength);
    }

    /** Converts to external record. */
    @VisibleForTesting
    public ExerciseLap toExternalRecord() {
        ExerciseLap.Builder builder =
                new ExerciseLap.Builder(
                        Instant.ofEpochMilli(getStartTime()), Instant.ofEpochMilli(getEndTime()));

        if (getLength() != 0) {
            builder.setLength(Length.fromMeters(getLength()));
        }
        return builder.buildWithoutValidation();
    }

    /** Returns lap length in meters. */
    public double getLength() {
        return mLength;
    }

    /** Set laps length. return record with the length set. */
    public ExerciseLapInternal setLength(double lengthInMeters) {
        mLength = lengthInMeters;
        return this;
    }

    /** Sets lap start time. Returns record with start time set. */
    public ExerciseLapInternal setStarTime(long startTime) {
        mStartTime = startTime;
        return this;
    }

    /** Sets lap end time. Returns record with end time set. */
    public ExerciseLapInternal setEndTime(long endTime) {
        mEndTime = endTime;
        return this;
    }

    /** Returns laps start time. */
    public long getStartTime() {
        return mStartTime;
    }

    /** Returns laps end time. */
    public long getEndTime() {
        return mEndTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExerciseLapInternal)) return false;
        ExerciseLapInternal that = (ExerciseLapInternal) o;
        return mStartTime == that.mStartTime
                && mEndTime == that.mEndTime
                && mLength == that.mLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStartTime, mEndTime, mLength);
    }
}

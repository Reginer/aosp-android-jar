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

import android.annotation.Nullable;
import android.health.connect.datatypes.PlannedExerciseBlock;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal {@link PlannedExerciseBlock}. Part of a {@link PlannedExerciseSessionRecordInternal}.
 *
 * @hide
 */
public final class PlannedExerciseBlockInternal {
    private int mRepetitions;

    @Nullable private String mDescription;

    private List<PlannedExerciseStepInternal> mExerciseSteps = new ArrayList<>();

    /** Serialize to parcel. */
    public static void writeToParcel(List<PlannedExerciseBlockInternal> blocks, Parcel parcel) {
        parcel.writeInt(blocks.size());
        for (PlannedExerciseBlockInternal block : blocks) {
            parcel.writeInt(block.getRepetitions());
            parcel.writeString(block.getDescription());
            PlannedExerciseStepInternal.writeToParcel(block.getExerciseSteps(), parcel);
        }
    }

    /** Deserialize from parcel. */
    public static List<PlannedExerciseBlockInternal> readFromParcel(Parcel parcel) {
        List<PlannedExerciseBlockInternal> result = new ArrayList<>();
        int count = parcel.readInt();
        for (int i = 0; i < count; i++) {
            PlannedExerciseBlockInternal block = new PlannedExerciseBlockInternal(parcel.readInt());
            block.setDescription(parcel.readString());
            block.setExerciseSteps(PlannedExerciseStepInternal.readFromParcel(parcel));
            result.add(block);
        }
        return result;
    }

    public PlannedExerciseBlockInternal(int repetitions) {
        this.mRepetitions = repetitions;
    }

    @Nullable
    public String getDescription() {
        return mDescription;
    }

    public int getRepetitions() {
        return mRepetitions;
    }

    public List<PlannedExerciseStepInternal> getExerciseSteps() {
        return mExerciseSteps;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

    public void setRepetitions(int repetitions) {
        this.mRepetitions = repetitions;
    }

    public void setExerciseSteps(List<PlannedExerciseStepInternal> exerciseSteps) {
        this.mExerciseSteps = exerciseSteps;
    }

    /** Convert to external representation. */
    public PlannedExerciseBlock toExternalObject() {
        PlannedExerciseBlock.Builder builder = new PlannedExerciseBlock.Builder(mRepetitions);
        builder.setDescription(mDescription);
        for (PlannedExerciseStepInternal step : mExerciseSteps) {
            builder.addStep(step.toExternalObject());
        }
        return builder.build();
    }
}

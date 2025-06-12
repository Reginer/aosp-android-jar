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
import android.health.connect.datatypes.ExerciseSegmentType;
import android.health.connect.datatypes.PlannedExerciseStep;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal ExerciseStep. Part of a {@link PlannedExerciseBlockInternal}.
 *
 * @hide
 */
public final class PlannedExerciseStepInternal {
    @ExerciseSegmentType.ExerciseSegmentTypes private int mExerciseType;
    @PlannedExerciseStep.ExerciseCategory private int mExerciseCategory;
    @Nullable private String mDescription;
    private ExerciseCompletionGoalInternal mCompletionGoal;
    private List<ExercisePerformanceGoalInternal> mPerformanceGoals = Collections.emptyList();

    public PlannedExerciseStepInternal(
            int exerciseType, int exerciseCategory, ExerciseCompletionGoalInternal completionGoal) {
        this.mExerciseType = exerciseType;
        this.mExerciseCategory = exerciseCategory;
        this.mCompletionGoal = completionGoal;
    }

    @ExerciseSegmentType.ExerciseSegmentTypes
    public int getExerciseType() {
        return mExerciseType;
    }

    public void setExerciseType(@ExerciseSegmentType.ExerciseSegmentTypes int exerciseType) {
        this.mExerciseType = exerciseType;
    }

    public void setExerciseCategory(@PlannedExerciseStep.ExerciseCategory int exerciseCategory) {
        this.mExerciseCategory = exerciseCategory;
    }

    public void setDescription(@Nullable String description) {
        this.mDescription = description;
    }

    public void setCompletionGoal(ExerciseCompletionGoalInternal completionGoal) {
        this.mCompletionGoal = completionGoal;
    }

    public void setPerformanceGoals(List<ExercisePerformanceGoalInternal> performanceGoals) {
        this.mPerformanceGoals = performanceGoals;
    }

    @PlannedExerciseStep.ExerciseCategory
    public int getExerciseCategory() {
        return mExerciseCategory;
    }

    @Nullable
    public String getDescription() {
        return mDescription;
    }

    public ExerciseCompletionGoalInternal getCompletionGoal() {
        return mCompletionGoal;
    }

    public List<ExercisePerformanceGoalInternal> getPerformanceGoals() {
        return mPerformanceGoals;
    }

    /** Convert to external representation. */
    public PlannedExerciseStep toExternalObject() {
        PlannedExerciseStep.Builder builder =
                new PlannedExerciseStep.Builder(
                        mExerciseType, mExerciseCategory, mCompletionGoal.toExternalObject());
        if (mDescription != null) {
            builder.setDescription(mDescription);
        }
        for (ExercisePerformanceGoalInternal goal : mPerformanceGoals) {
            builder.addPerformanceGoal(goal.toExternalObject());
        }
        return builder.build();
    }

    /** Serialize to parcel. */
    public static void writeToParcel(List<PlannedExerciseStepInternal> steps, Parcel parcel) {
        parcel.writeInt(steps.size());
        for (PlannedExerciseStepInternal step : steps) {
            parcel.writeInt(step.getExerciseType());
            parcel.writeInt(step.getExerciseCategory());
            step.getCompletionGoal().writeToParcel(parcel);
            parcel.writeString(step.getDescription());
            ExercisePerformanceGoalInternal.writeToParcel(step.getPerformanceGoals(), parcel);
        }
    }

    /** Deserialize from parcel. */
    public static List<PlannedExerciseStepInternal> readFromParcel(Parcel parcel) {
        List<PlannedExerciseStepInternal> result = new ArrayList<>();
        int count = parcel.readInt();
        for (int i = 0; i < count; i++) {
            PlannedExerciseStepInternal step =
                    new PlannedExerciseStepInternal(
                            parcel.readInt(),
                            parcel.readInt(),
                            ExerciseCompletionGoalInternal.readFromParcel(parcel));
            step.setDescription(parcel.readString());
            step.setPerformanceGoals(ExercisePerformanceGoalInternal.readFromParcel(parcel));
            result.add(step);
        }
        return result;
    }
}

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

import static android.health.connect.datatypes.ExerciseSegmentType.ExerciseSegmentTypes;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal;
import android.health.connect.internal.datatypes.ExercisePerformanceGoalInternal;
import android.health.connect.internal.datatypes.PlannedExerciseStepInternal;
import android.util.ArraySet;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** A single step within an {@link PlannedExerciseBlock} e.g. 8x 60kg barbell squats. */
@FlaggedApi("com.android.healthconnect.flags.training_plans")
public final class PlannedExerciseStep {
    @ExerciseSegmentTypes private final int mExerciseType;
    @ExerciseCategory private final int mExerciseCategory;
    @Nullable private final CharSequence mDescription;
    private final ExerciseCompletionGoal mCompletionGoal;
    private final List<ExercisePerformanceGoal> mPerformanceGoals;

    private PlannedExerciseStep(
            @ExerciseSegmentTypes int exerciseType,
            @Nullable CharSequence description,
            @ExerciseCategory int exerciseCategory,
            ExerciseCompletionGoal completionGoal,
            List<ExercisePerformanceGoal> performanceGoals) {
        this.mExerciseType = exerciseType;
        this.mDescription = description;
        this.mExerciseCategory = exerciseCategory;
        this.mCompletionGoal = completionGoal;
        this.mPerformanceGoals = performanceGoals;
    }

    /** Returns the exercise type of this step. */
    @ExerciseSegmentTypes
    public int getExerciseType() {
        return mExerciseType;
    }

    /** Returns the description of this step. */
    @Nullable
    public CharSequence getDescription() {
        return mDescription;
    }

    /** Returns the exercise category of this step. */
    @ExerciseCategory
    public int getExerciseCategory() {
        return mExerciseCategory;
    }

    /** Returns the exercise completion goal for this step. */
    @NonNull
    public ExerciseCompletionGoal getCompletionGoal() {
        return mCompletionGoal;
    }

    /**
     * Returns the exercise performance goals for this step.
     *
     * @return An unmodifiable list of {@link ExercisePerformanceGoal}.
     */
    @NonNull
    public List<ExercisePerformanceGoal> getPerformanceGoals() {
        return mPerformanceGoals;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof PlannedExerciseStep)) return false;
        PlannedExerciseStep that = (PlannedExerciseStep) o;
        return RecordUtils.isEqualNullableCharSequences(getDescription(), that.getDescription())
                && this.getExerciseCategory() == that.getExerciseCategory()
                && this.getExerciseType() == that.getExerciseType()
                && Objects.equals(this.getCompletionGoal(), that.getCompletionGoal())
                && Objects.equals(this.getPerformanceGoals(), that.getPerformanceGoals());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getDescription(),
                getExerciseCategory(),
                getExerciseType(),
                getCompletionGoal(),
                getPerformanceGoals());
    }

    /** Builder of {@link PlannedExerciseStep}. */
    public static final class Builder {
        @ExerciseSegmentTypes private int mExerciseType;
        @ExerciseCategory private int mExerciseCategory;
        @Nullable private CharSequence mDescription;
        private ExerciseCompletionGoal mCompletionGoal;
        private List<ExercisePerformanceGoal> mPerformanceGoals = new ArrayList<>();

        /**
         * @param exerciseType The type of exercise to be carried out in this step, e.g. running.
         * @param exerciseCategory The category of exercise to be carried out in this step, e.g.
         *     warmup.
         * @param completionGoal The goal to be met to complete this step.
         */
        public Builder(
                @ExerciseSegmentTypes int exerciseType,
                @ExerciseCategory int exerciseCategory,
                @NonNull ExerciseCompletionGoal completionGoal) {
            this.mExerciseType = exerciseType;
            this.mExerciseCategory = exerciseCategory;
            this.mCompletionGoal = completionGoal;
        }

        /** Sets the exercise type. */
        @NonNull
        public Builder setExerciseType(@ExerciseSegmentTypes int exerciseType) {
            this.mExerciseType = exerciseType;
            return this;
        }

        /** Sets the exercise category. */
        @NonNull
        public Builder setExerciseCategory(@ExerciseCategory int category) {
            this.mExerciseCategory = category;
            return this;
        }

        /** Sets the description. */
        @NonNull
        public Builder setDescription(@Nullable CharSequence description) {
            this.mDescription = description;
            return this;
        }

        /** Sets the {@link ExerciseCompletionGoal}. */
        @NonNull
        public Builder setCompletionGoal(@NonNull ExerciseCompletionGoal completionGoal) {
            Objects.requireNonNull(completionGoal);
            this.mCompletionGoal = completionGoal;
            return this;
        }

        /** Adds a {@link ExercisePerformanceGoal}. */
        @NonNull
        public Builder addPerformanceGoal(@NonNull ExercisePerformanceGoal performanceGoal) {
            Objects.requireNonNull(performanceGoal);
            this.mPerformanceGoals.add(performanceGoal);
            return this;
        }

        /** Sets {@link ExercisePerformanceGoal} entries. */
        @NonNull
        public Builder setPerformanceGoals(
                @NonNull List<ExercisePerformanceGoal> performanceGoals) {
            Objects.requireNonNull(performanceGoals);
            this.mPerformanceGoals.clear();
            this.mPerformanceGoals.addAll(performanceGoals);
            return this;
        }

        /** Clears {@link ExercisePerformanceGoal} entries. */
        @NonNull
        public Builder clearPerformanceGoals() {
            this.mPerformanceGoals.clear();
            return this;
        }

        /** Returns {@link PlannedExerciseStep} instance. */
        @NonNull
        public PlannedExerciseStep build() {
            Set<Class> classes = new ArraySet<>();
            for (ExercisePerformanceGoal goal : mPerformanceGoals) {
                classes.add(goal.getClass());
            }
            if (classes.size() != mPerformanceGoals.size()) {
                throw new IllegalArgumentException(
                        "At most one of each type of performance goal is permitted.");
            }
            if (!isValidExerciseCategory(mExerciseCategory)) {
                throw new IllegalArgumentException("Invalid exercise category.");
            }
            return new PlannedExerciseStep(
                    mExerciseType,
                    mDescription,
                    mExerciseCategory,
                    mCompletionGoal,
                    List.copyOf(mPerformanceGoals));
        }
    }

    /** @hide */
    public PlannedExerciseStepInternal toInternalObject() {
        PlannedExerciseStepInternal result =
                new PlannedExerciseStepInternal(
                        getExerciseType(),
                        getExerciseCategory(),
                        ExerciseCompletionGoalInternal.fromExternalObject(getCompletionGoal()));
        if (mDescription != null) {
            result.setDescription(mDescription.toString());
        }
        result.setPerformanceGoals(
                getPerformanceGoals().stream()
                        .map(ExercisePerformanceGoalInternal::fromExternalObject)
                        .collect(Collectors.toList()));
        return result;
    }

    /** An unknown category of exercise. */
    public static final int EXERCISE_CATEGORY_UNKNOWN = 0;

    /** A warmup. */
    public static final int EXERCISE_CATEGORY_WARMUP = 1;

    /** A rest. */
    public static final int EXERCISE_CATEGORY_REST = 2;

    /** Active exercise. */
    public static final int EXERCISE_CATEGORY_ACTIVE = 3;

    /** Cooldown exercise, typically at the end of a workout. */
    public static final int EXERCISE_CATEGORY_COOLDOWN = 4;

    /** Lower intensity, active exercise. */
    public static final int EXERCISE_CATEGORY_RECOVERY = 5;

    /** @hide */
    @IntDef({
        EXERCISE_CATEGORY_UNKNOWN,
        EXERCISE_CATEGORY_WARMUP,
        EXERCISE_CATEGORY_REST,
        EXERCISE_CATEGORY_ACTIVE,
        EXERCISE_CATEGORY_COOLDOWN,
        EXERCISE_CATEGORY_RECOVERY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ExerciseCategory {}

    private static final Set<Integer> VALID_EXERCISE_CATEGORIES =
            Set.of(
                    EXERCISE_CATEGORY_UNKNOWN,
                    EXERCISE_CATEGORY_WARMUP,
                    EXERCISE_CATEGORY_REST,
                    EXERCISE_CATEGORY_ACTIVE,
                    EXERCISE_CATEGORY_COOLDOWN,
                    EXERCISE_CATEGORY_RECOVERY);

    /**
     * Returns whether given exercise category is known by current module version.
     *
     * @hide
     */
    public static boolean isValidExerciseCategory(int exerciseCategory) {
        return VALID_EXERCISE_CATEGORIES.contains(exerciseCategory);
    }
}

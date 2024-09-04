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
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Length;

import java.time.Duration;
import java.util.Objects;

/** A goal which should be met to complete a {@link PlannedExerciseStep}. */
@FlaggedApi("com.android.healthconnect.flags.training_plans")
public abstract class ExerciseCompletionGoal {
    /** An {@link ExerciseCompletionGoal} that requires covering a specified distance. */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class DistanceGoal extends ExerciseCompletionGoal {
        private final Length mDistance;

        /**
         * @param distance The total distance that must be covered to complete the goal.
         */
        public DistanceGoal(@NonNull Length distance) {
            Objects.requireNonNull(distance);
            this.mDistance = distance;
        }

        /** Returns the total distance that must be covered to complete this goal. */
        @NonNull
        public Length getDistance() {
            return mDistance;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof DistanceGoal)) return false;
            DistanceGoal that = (DistanceGoal) o;
            return this.getDistance().equals(that.getDistance());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mDistance);
        }
    }

    /**
     * An {@link ExerciseCompletionGoal} that requires covering a specified distance. Additionally,
     * the step is not complete until the specified time has elapsed. Time remaining after the
     * specified distance has been completed should be spent resting. In the context of swimming,
     * this is sometimes referred to as 'interval training'.
     *
     * <p>For example, a swimming coach may specify '100m @ 1min40s'. This implies: complete 100m
     * and if you manage it in 1min30s, you will have 10s of rest prior to the next set.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class DistanceWithVariableRestGoal extends ExerciseCompletionGoal {
        private final Length mDistance;
        private final Duration mDuration;

        /**
         * @param distance The total distance that must be covered to complete the goal.
         * @param duration The total duration that must elapse to complete the goal.
         */
        public DistanceWithVariableRestGoal(@NonNull Length distance, @NonNull Duration duration) {
            Objects.requireNonNull(distance);
            Objects.requireNonNull(duration);
            this.mDistance = distance;
            this.mDuration = duration;
        }

        /** Returns the total distance that must be covered to complete this goal. */
        @NonNull
        public Length getDistance() {
            return mDistance;
        }

        /** Returns the total duration that must elapse to complete this goal. */
        @NonNull
        public Duration getDuration() {
            return mDuration;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof DistanceWithVariableRestGoal)) return false;
            DistanceWithVariableRestGoal that = (DistanceWithVariableRestGoal) o;
            return this.getDistance().equals(that.getDistance())
                    && this.getDuration().equals(that.getDuration());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mDistance, mDuration);
        }
    }

    /** An {@link ExerciseCompletionGoal} that requires completing a specified number of steps. */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class StepsGoal extends ExerciseCompletionGoal {
        private final int mSteps;

        /**
         * @param steps The total steps that must be covered to complete the goal.
         */
        public StepsGoal(int steps) {
            this.mSteps = steps;
        }

        /** Returns the total number of steps that must be completed to complete this goal. */
        public int getSteps() {
            return mSteps;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof StepsGoal)) return false;
            StepsGoal that = (StepsGoal) o;
            return getSteps() == that.getSteps();
        }

        @Override
        public int hashCode() {
            return Objects.hash(mSteps);
        }
    }

    /** An {@link ExerciseCompletionGoal} that requires a specified duration to elapse. */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class DurationGoal extends ExerciseCompletionGoal {
        private final Duration mDuration;

        /**
         * @param duration The total duration that must elapse to complete the goal.
         */
        public DurationGoal(@NonNull Duration duration) {
            Objects.requireNonNull(duration);
            this.mDuration = duration;
        }

        /** Returns the total duration that must elapse to complete this goal. */
        @NonNull
        public Duration getDuration() {
            return mDuration;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof DurationGoal)) return false;
            DurationGoal that = (DurationGoal) o;
            return this.getDuration().equals(that.getDuration());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mDuration);
        }
    }

    /**
     * An {@link ExerciseCompletionGoal} that requires a specified number of repetitions to be
     * completed.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class RepetitionsGoal extends ExerciseCompletionGoal {
        private final int mRepetitions;

        /**
         * @param repetitions The total repetitions required to complete the goal.
         */
        public RepetitionsGoal(int repetitions) {
            this.mRepetitions = repetitions;
        }

        /** Returns the total number of repetitions that must be completed to complete this goal. */
        public int getRepetitions() {
            return mRepetitions;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof RepetitionsGoal)) return false;
            RepetitionsGoal that = (RepetitionsGoal) o;
            return this.getRepetitions() == that.getRepetitions();
        }

        @Override
        public int hashCode() {
            return Objects.hash(mRepetitions);
        }
    }

    /**
     * An {@link ExerciseCompletionGoal} that requires a specified number of total calories to be
     * burned.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class TotalCaloriesBurnedGoal extends ExerciseCompletionGoal {
        private final Energy mTotalCalories;

        /**
         * @param totalCalories The total calories that must be burnt to complete the goal.
         */
        public TotalCaloriesBurnedGoal(@NonNull Energy totalCalories) {
            Objects.requireNonNull(totalCalories);
            this.mTotalCalories = totalCalories;
        }

        /** Returns the total calories that must be burned to complete this goal. */
        @NonNull
        public Energy getTotalCalories() {
            return mTotalCalories;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof TotalCaloriesBurnedGoal)) return false;
            TotalCaloriesBurnedGoal that = (TotalCaloriesBurnedGoal) o;
            return this.getTotalCalories().equals(that.getTotalCalories());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mTotalCalories);
        }
    }

    /**
     * An {@link ExerciseCompletionGoal} that requires a specified number of active calories to be
     * burned.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class ActiveCaloriesBurnedGoal extends ExerciseCompletionGoal {
        private final Energy mActiveCalories;

        /**
         * @param activeCalories The active calories that must be burnt to complete the goal.
         */
        public ActiveCaloriesBurnedGoal(@NonNull Energy activeCalories) {
            Objects.requireNonNull(activeCalories);
            this.mActiveCalories = activeCalories;
        }

        /** Returns the active calories that must be burned to complete this goal. */
        @NonNull
        public Energy getActiveCalories() {
            return mActiveCalories;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ActiveCaloriesBurnedGoal)) return false;
            ActiveCaloriesBurnedGoal that = (ActiveCaloriesBurnedGoal) o;
            return this.getActiveCalories().equals(that.getActiveCalories());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mActiveCalories);
        }
    }

    /** An {@link ExerciseCompletionGoal} that is unknown. */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class UnknownGoal extends ExerciseCompletionGoal {
        private UnknownGoal() {}

        @NonNull public static final UnknownGoal INSTANCE = new UnknownGoal();
    }

    /**
     * An {@link ExerciseCompletionGoal} that has no specific target metric. It is up to the user to
     * determine when the associated {@link PlannedExerciseStep} is complete.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class UnspecifiedGoal extends ExerciseCompletionGoal {
        private UnspecifiedGoal() {}

        @NonNull public static final UnspecifiedGoal INSTANCE = new UnspecifiedGoal();
    }

    private ExerciseCompletionGoal() {}
}

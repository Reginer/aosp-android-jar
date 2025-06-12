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
import android.health.connect.datatypes.ExerciseCompletionGoal;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Length;
import android.os.Parcel;

import java.time.Duration;

/**
 * Internal ExerciseBlock. Part of {@link PlannedExerciseSessionRecordInternal}.
 *
 * @hide
 */
public abstract class ExerciseCompletionGoalInternal {

    /** Convert to {@link ExerciseCompletionGoal}. */
    @NonNull
    public abstract ExerciseCompletionGoal toExternalObject();

    /** Subclass identifier used during serialization/deserialization. */
    public abstract int getTypeId();

    abstract void writeFieldsToParcel(@NonNull Parcel parcel);

    /** Convert from external representation. */
    public static ExerciseCompletionGoalInternal fromExternalObject(
            ExerciseCompletionGoal externalObject) {
        if (externalObject instanceof ExerciseCompletionGoal.DistanceGoal) {
            ExerciseCompletionGoal.DistanceGoal goal =
                    (ExerciseCompletionGoal.DistanceGoal) externalObject;
            return new DistanceGoalInternal(goal.getDistance());
        } else if (externalObject instanceof ExerciseCompletionGoal.DistanceWithVariableRestGoal) {
            ExerciseCompletionGoal.DistanceWithVariableRestGoal goal =
                    (ExerciseCompletionGoal.DistanceWithVariableRestGoal) externalObject;
            return new DistanceWithVariableRestGoalInternal(goal.getDistance(), goal.getDuration());
        } else if (externalObject instanceof ExerciseCompletionGoal.StepsGoal) {
            ExerciseCompletionGoal.StepsGoal goal =
                    (ExerciseCompletionGoal.StepsGoal) externalObject;
            return new StepsGoalInternal(goal.getSteps());
        } else if (externalObject instanceof ExerciseCompletionGoal.DurationGoal) {
            ExerciseCompletionGoal.DurationGoal goal =
                    (ExerciseCompletionGoal.DurationGoal) externalObject;
            return new DurationGoalInternal(goal.getDuration());
        } else if (externalObject instanceof ExerciseCompletionGoal.RepetitionsGoal) {
            ExerciseCompletionGoal.RepetitionsGoal goal =
                    (ExerciseCompletionGoal.RepetitionsGoal) externalObject;
            return new RepetitionsGoalInternal(goal.getRepetitions());
        } else if (externalObject instanceof ExerciseCompletionGoal.TotalCaloriesBurnedGoal) {
            ExerciseCompletionGoal.TotalCaloriesBurnedGoal goal =
                    (ExerciseCompletionGoal.TotalCaloriesBurnedGoal) externalObject;
            return new TotalCaloriesBurnedGoalInternal(goal.getTotalCalories());
        } else if (externalObject instanceof ExerciseCompletionGoal.ActiveCaloriesBurnedGoal) {
            ExerciseCompletionGoal.ActiveCaloriesBurnedGoal goal =
                    (ExerciseCompletionGoal.ActiveCaloriesBurnedGoal) externalObject;
            return new ActiveCaloriesBurnedGoalInternal(goal.getActiveCalories());
        } else if (externalObject instanceof ExerciseCompletionGoal.UnspecifiedGoal) {
            return UnspecifiedGoalInternal.INSTANCE;
        } else {
            return UnknownGoalInternal.INSTANCE;
        }
    }

    /** Serialize to parcel. */
    public void writeToParcel(@NonNull Parcel parcel) {
        parcel.writeInt(getTypeId());
        writeFieldsToParcel(parcel);
    }

    /** Deserialize from parcel. */
    @NonNull
    public static ExerciseCompletionGoalInternal readFromParcel(@NonNull Parcel parcel) {
        int goalTypeId = parcel.readInt();
        switch (goalTypeId) {
            case DistanceGoalInternal.DISTANCE_GOAL_TYPE_ID:
                return DistanceGoalInternal.readFieldsFromParcel(parcel);
            case StepsGoalInternal.STEPS_GOAL_TYPE_ID:
                return StepsGoalInternal.readFieldsFromParcel(parcel);
            case DurationGoalInternal.DURATION_GOAL_TYPE_ID:
                return DurationGoalInternal.readFieldsFromParcel(parcel);
            case RepetitionsGoalInternal.REPETITIONS_GOAL_TYPE_ID:
                return RepetitionsGoalInternal.readFieldsFromParcel(parcel);
            case TotalCaloriesBurnedGoalInternal.TOTAL_CALORIES_BURNED_GOAL_TYPE_ID:
                return TotalCaloriesBurnedGoalInternal.readFieldsFromParcel(parcel);
            case ActiveCaloriesBurnedGoalInternal.ACTIVE_CALORIES_BURNED_GOAL_TYPE_ID:
                return ActiveCaloriesBurnedGoalInternal.readFieldsFromParcel(parcel);
            case DistanceWithVariableRestGoalInternal.DISTANCE_WITH_VARIABLE_REST_GOAL_TYPE_ID:
                return DistanceWithVariableRestGoalInternal.readFieldsFromParcel(parcel);
            case UnspecifiedGoalInternal.UNSPECIFIED_GOAL_TYPE_ID:
                return UnspecifiedGoalInternal.INSTANCE;
            case UnknownGoalInternal.UNKNOWN_GOAL_TYPE_ID:
                // Fall through.
            default:
                // Can never happen. Client side and service side always have consistent version.
                return UnknownGoalInternal.INSTANCE;
        }
    }

    public static final class DistanceGoalInternal extends ExerciseCompletionGoalInternal {
        public static final int DISTANCE_GOAL_TYPE_ID = 2;

        @Override
        void writeFieldsToParcel(@NonNull Parcel parcel) {
            parcel.writeDouble(this.mDistance.getInMeters());
        }

        /** Read from parcel. */
        @NonNull
        public static DistanceGoalInternal readFieldsFromParcel(@NonNull Parcel parcel) {
            return new DistanceGoalInternal(Length.fromMeters(parcel.readDouble()));
        }

        private final Length mDistance;

        public DistanceGoalInternal(@NonNull Length distance) {
            this.mDistance = distance;
        }

        @NonNull
        public Length getDistance() {
            return mDistance;
        }

        @Override
        @NonNull
        public ExerciseCompletionGoal toExternalObject() {
            return new ExerciseCompletionGoal.DistanceGoal(mDistance);
        }

        @Override
        public int getTypeId() {
            return DISTANCE_GOAL_TYPE_ID;
        }
    }

    public static final class StepsGoalInternal extends ExerciseCompletionGoalInternal {
        public static final int STEPS_GOAL_TYPE_ID = 3;

        @Override
        void writeFieldsToParcel(@NonNull Parcel parcel) {
            parcel.writeInt(this.mSteps);
        }

        /** Read from parcel. */
        @NonNull
        public static StepsGoalInternal readFieldsFromParcel(@NonNull Parcel parcel) {
            return new StepsGoalInternal(parcel.readInt());
        }

        private final int mSteps;

        public StepsGoalInternal(int steps) {
            this.mSteps = steps;
        }

        public int getSteps() {
            return mSteps;
        }

        @Override
        @NonNull
        public ExerciseCompletionGoal toExternalObject() {
            return new ExerciseCompletionGoal.StepsGoal(mSteps);
        }

        @Override
        public int getTypeId() {
            return STEPS_GOAL_TYPE_ID;
        }
    }

    public static final class DurationGoalInternal extends ExerciseCompletionGoalInternal {
        public static final int DURATION_GOAL_TYPE_ID = 4;

        @Override
        void writeFieldsToParcel(@NonNull Parcel parcel) {
            parcel.writeLong(this.mDuration.toMillis());
        }

        /** Read from parcel. */
        @NonNull
        public static DurationGoalInternal readFieldsFromParcel(@NonNull Parcel parcel) {
            return new DurationGoalInternal(Duration.ofMillis(parcel.readLong()));
        }

        private final Duration mDuration;

        public DurationGoalInternal(@NonNull Duration duration) {
            this.mDuration = duration;
        }

        @NonNull
        public Duration getDuration() {
            return mDuration;
        }

        @Override
        @NonNull
        public ExerciseCompletionGoal toExternalObject() {
            return new ExerciseCompletionGoal.DurationGoal(mDuration);
        }

        @Override
        public int getTypeId() {
            return DURATION_GOAL_TYPE_ID;
        }
    }

    public static final class RepetitionsGoalInternal extends ExerciseCompletionGoalInternal {
        public static final int REPETITIONS_GOAL_TYPE_ID = 5;

        @Override
        void writeFieldsToParcel(@NonNull Parcel parcel) {
            parcel.writeInt(this.mReps);
        }

        /** Read from parcel. */
        @NonNull
        public static RepetitionsGoalInternal readFieldsFromParcel(@NonNull Parcel parcel) {
            return new RepetitionsGoalInternal(parcel.readInt());
        }

        private final int mReps;

        public RepetitionsGoalInternal(int reps) {
            this.mReps = reps;
        }

        public int getReps() {
            return mReps;
        }

        @Override
        @NonNull
        public ExerciseCompletionGoal toExternalObject() {
            return new ExerciseCompletionGoal.RepetitionsGoal(mReps);
        }

        @Override
        public int getTypeId() {
            return REPETITIONS_GOAL_TYPE_ID;
        }
    }

    public static final class TotalCaloriesBurnedGoalInternal
            extends ExerciseCompletionGoalInternal {
        public static final int TOTAL_CALORIES_BURNED_GOAL_TYPE_ID = 6;

        @Override
        void writeFieldsToParcel(@NonNull Parcel parcel) {
            parcel.writeDouble(this.mTotalCalories.getInCalories());
        }

        /** Read from parcel. */
        @NonNull
        public static TotalCaloriesBurnedGoalInternal readFieldsFromParcel(@NonNull Parcel parcel) {
            return new TotalCaloriesBurnedGoalInternal(Energy.fromCalories(parcel.readDouble()));
        }

        private final Energy mTotalCalories;

        public TotalCaloriesBurnedGoalInternal(@NonNull Energy totalCalories) {
            this.mTotalCalories = totalCalories;
        }

        @NonNull
        public Energy getTotalCalories() {
            return mTotalCalories;
        }

        @Override
        public ExerciseCompletionGoal toExternalObject() {
            return new ExerciseCompletionGoal.TotalCaloriesBurnedGoal(mTotalCalories);
        }

        @Override
        public int getTypeId() {
            return TOTAL_CALORIES_BURNED_GOAL_TYPE_ID;
        }
    }

    public static final class ActiveCaloriesBurnedGoalInternal
            extends ExerciseCompletionGoalInternal {
        public static final int ACTIVE_CALORIES_BURNED_GOAL_TYPE_ID = 7;

        @Override
        void writeFieldsToParcel(@NonNull Parcel parcel) {
            parcel.writeDouble(this.mActiveCalories.getInCalories());
        }

        /** Read from parcel. */
        @NonNull
        public static ActiveCaloriesBurnedGoalInternal readFieldsFromParcel(
                @NonNull Parcel parcel) {
            return new ActiveCaloriesBurnedGoalInternal(Energy.fromCalories(parcel.readDouble()));
        }

        private final Energy mActiveCalories;

        public ActiveCaloriesBurnedGoalInternal(@NonNull Energy activeCalories) {
            this.mActiveCalories = activeCalories;
        }

        @NonNull
        public Energy getActiveCalories() {
            return mActiveCalories;
        }

        @Override
        @NonNull
        public ExerciseCompletionGoal toExternalObject() {
            return new ExerciseCompletionGoal.ActiveCaloriesBurnedGoal(mActiveCalories);
        }

        @Override
        public int getTypeId() {
            return ACTIVE_CALORIES_BURNED_GOAL_TYPE_ID;
        }
    }

    public static final class DistanceWithVariableRestGoalInternal
            extends ExerciseCompletionGoalInternal {
        public static final int DISTANCE_WITH_VARIABLE_REST_GOAL_TYPE_ID = 8;

        @Override
        void writeFieldsToParcel(@NonNull Parcel parcel) {
            parcel.writeDouble(this.mDistance.getInMeters());
            parcel.writeLong(this.mDuration.toMillis());
        }

        /** Read from parcel. */
        @NonNull
        public static DistanceWithVariableRestGoalInternal readFieldsFromParcel(
                @NonNull Parcel parcel) {
            return new DistanceWithVariableRestGoalInternal(
                    Length.fromMeters(parcel.readDouble()), Duration.ofMillis(parcel.readLong()));
        }

        private final Length mDistance;

        private final Duration mDuration;

        public DistanceWithVariableRestGoalInternal(
                @NonNull Length distance, @NonNull Duration duration) {
            this.mDistance = distance;
            this.mDuration = duration;
        }

        @NonNull
        public Length getDistance() {
            return mDistance;
        }

        @NonNull
        public Duration getDuration() {
            return mDuration;
        }

        @Override
        @NonNull
        public ExerciseCompletionGoal toExternalObject() {
            return new ExerciseCompletionGoal.DistanceWithVariableRestGoal(mDistance, mDuration);
        }

        @Override
        public int getTypeId() {
            return DISTANCE_WITH_VARIABLE_REST_GOAL_TYPE_ID;
        }
    }

    /**
     * Represents a goal that is not recognised by the platform. This could happen when e.g. a
     * version rollback occurs, i.e. a version is stored in the database that is not supported by
     * the current (older) version.
     */
    public static final class UnknownGoalInternal extends ExerciseCompletionGoalInternal {
        public static final int UNKNOWN_GOAL_TYPE_ID = 0;

        public static final UnknownGoalInternal INSTANCE = new UnknownGoalInternal();

        @Override
        void writeFieldsToParcel(@NonNull Parcel parcel) {
            // No fields to write.
        }

        UnknownGoalInternal() {}

        @Override
        @NonNull
        public ExerciseCompletionGoal toExternalObject() {
            return ExerciseCompletionGoal.UnknownGoal.INSTANCE;
        }

        @Override
        public int getTypeId() {
            return UNKNOWN_GOAL_TYPE_ID;
        }
    }

    /**
     * Represents an open goal that has no specific metric associated with completion. It is up to
     * the user to determine when the goal is complete.
     */
    public static final class UnspecifiedGoalInternal extends ExerciseCompletionGoalInternal {
        public static final int UNSPECIFIED_GOAL_TYPE_ID = 1;

        public static final UnspecifiedGoalInternal INSTANCE = new UnspecifiedGoalInternal();

        @Override
        void writeFieldsToParcel(@NonNull Parcel parcel) {
            // No fields to write.
        }

        UnspecifiedGoalInternal() {}

        @Override
        @NonNull
        public ExerciseCompletionGoal toExternalObject() {
            return ExerciseCompletionGoal.UnspecifiedGoal.INSTANCE;
        }

        @Override
        public int getTypeId() {
            return UNSPECIFIED_GOAL_TYPE_ID;
        }
    }
}

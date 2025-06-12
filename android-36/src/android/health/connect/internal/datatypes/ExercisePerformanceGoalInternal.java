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

import android.health.connect.datatypes.ExercisePerformanceGoal;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.Velocity;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal representation of {@link android.health.connect.datatypes.PlannedExerciseSessionRecord}.
 *
 * @hide
 */
public abstract class ExercisePerformanceGoalInternal {
    /** Convert to external representation. */
    public abstract ExercisePerformanceGoal toExternalObject();

    abstract void writeFieldsToParcel(Parcel parcel);

    /** Subclass identifier used during serialization/deserialization. */
    public abstract int getTypeId();

    /** Serialize to parcel. */
    public static void writeToParcel(
            List<ExercisePerformanceGoalInternal> performanceGoals, Parcel parcel) {
        parcel.writeInt(performanceGoals.size());
        for (ExercisePerformanceGoalInternal performanceGoal : performanceGoals) {
            parcel.writeInt(performanceGoal.getTypeId());
            performanceGoal.writeFieldsToParcel(parcel);
        }
    }

    /** Deserialize from parcel. */
    public static List<ExercisePerformanceGoalInternal> readFromParcel(Parcel parcel) {
        List<ExercisePerformanceGoalInternal> result = new ArrayList<>();
        int count = parcel.readInt();
        for (int i = 0; i < count; i++) {
            int goalTypeId = parcel.readInt();
            switch (goalTypeId) {
                case PowerGoalInternal.POWER_GOAL_TYPE_ID:
                    result.add(PowerGoalInternal.readFieldsFromParcel(parcel));
                    break;
                case SpeedGoalInternal.SPEED_GOAL_TYPE_ID:
                    result.add(SpeedGoalInternal.readFieldsFromParcel(parcel));
                    break;
                case CadenceGoalInternal.CADENCE_GOAL_TYPE_ID:
                    result.add(CadenceGoalInternal.readFieldsFromParcel(parcel));
                    break;
                case HeartRateGoalInternal.HEART_RATE_GOAL_TYPE_ID:
                    result.add(HeartRateGoalInternal.readFieldsFromParcel(parcel));
                    break;
                case WeightGoalInternal.WEIGHT_GOAL_TYPE_ID:
                    result.add(WeightGoalInternal.readFieldsFromParcel(parcel));
                    break;
                case RateOfPerceivedExertionGoalInternal.RATE_OF_PERCEIVED_EXERTION_TYPE_ID:
                    result.add(RateOfPerceivedExertionGoalInternal.readFieldsFromParcel(parcel));
                    break;
                case AmrapGoalInternal.AMRAP_GOAL_TYPE_ID:
                    result.add(AmrapGoalInternal.INSTANCE);
                    break;
                case ExercisePerformanceGoalInternal.UnknownGoalInternal.UNKNOWN_GOAL_TYPE_ID:
                    result.add(UnknownGoalInternal.INSTANCE);
                    break;
                default:
                    // Can never happen. Client side and service side always have consistent
                    // version.
                    break;
            }
        }
        return result;
    }

    /** Convert to internal representation. */
    public static ExercisePerformanceGoalInternal fromExternalObject(
            ExercisePerformanceGoal externalObject) {
        if (externalObject instanceof ExercisePerformanceGoal.PowerGoal) {
            ExercisePerformanceGoal.PowerGoal goal =
                    (ExercisePerformanceGoal.PowerGoal) externalObject;
            return new PowerGoalInternal(goal.getMinPower(), goal.getMaxPower());
        } else if (externalObject instanceof ExercisePerformanceGoal.SpeedGoal) {
            ExercisePerformanceGoal.SpeedGoal goal =
                    (ExercisePerformanceGoal.SpeedGoal) externalObject;
            return new SpeedGoalInternal(goal.getMinSpeed(), goal.getMaxSpeed());
        } else if (externalObject instanceof ExercisePerformanceGoal.CadenceGoal) {
            ExercisePerformanceGoal.CadenceGoal goal =
                    (ExercisePerformanceGoal.CadenceGoal) externalObject;
            return new CadenceGoalInternal(goal.getMinRpm(), goal.getMaxRpm());
        } else if (externalObject instanceof ExercisePerformanceGoal.HeartRateGoal) {
            ExercisePerformanceGoal.HeartRateGoal goal =
                    (ExercisePerformanceGoal.HeartRateGoal) externalObject;
            return new HeartRateGoalInternal(goal.getMinBpm(), goal.getMaxBpm());
        } else if (externalObject instanceof ExercisePerformanceGoal.WeightGoal) {
            ExercisePerformanceGoal.WeightGoal goal =
                    (ExercisePerformanceGoal.WeightGoal) externalObject;
            return new WeightGoalInternal(goal.getMass());
        } else if (externalObject instanceof ExercisePerformanceGoal.RateOfPerceivedExertionGoal) {
            ExercisePerformanceGoal.RateOfPerceivedExertionGoal goal =
                    (ExercisePerformanceGoal.RateOfPerceivedExertionGoal) externalObject;
            return new RateOfPerceivedExertionGoalInternal(goal.getRpe());
        } else if (externalObject instanceof ExercisePerformanceGoal.AmrapGoal) {
            return AmrapGoalInternal.INSTANCE;
        } else {
            return UnknownGoalInternal.INSTANCE;
        }
    }

    /**
     * Represents a goal that is not recognised by the platform. This could happen when e.g. a
     * version rollback occurs, i.e. a version is stored in the database that is not supported by
     * the current (older) version.
     */
    public static final class UnknownGoalInternal extends ExercisePerformanceGoalInternal {
        public static final UnknownGoalInternal INSTANCE = new UnknownGoalInternal();

        public static final int UNKNOWN_GOAL_TYPE_ID = 0;

        @Override
        void writeFieldsToParcel(Parcel parcel) {
            // No fields to write.
        }

        @Override
        public int getTypeId() {
            return UNKNOWN_GOAL_TYPE_ID;
        }

        UnknownGoalInternal() {}

        @Override
        public ExercisePerformanceGoal toExternalObject() {
            return ExercisePerformanceGoal.UnknownGoal.INSTANCE;
        }
    }

    public static final class PowerGoalInternal extends ExercisePerformanceGoalInternal {
        public static final int POWER_GOAL_TYPE_ID = 1;

        @Override
        void writeFieldsToParcel(Parcel parcel) {
            parcel.writeDouble(this.mMinPower.getInWatts());
            parcel.writeDouble(this.mMaxPower.getInWatts());
        }

        static PowerGoalInternal readFieldsFromParcel(Parcel parcel) {
            return new PowerGoalInternal(
                    Power.fromWatts(parcel.readDouble()), Power.fromWatts(parcel.readDouble()));
        }

        @Override
        public int getTypeId() {
            return POWER_GOAL_TYPE_ID;
        }

        private final Power mMinPower;
        private final Power mMaxPower;

        public PowerGoalInternal(Power minPower, Power maxPower) {
            this.mMinPower = minPower;
            this.mMaxPower = maxPower;
        }

        public Power getMinPower() {
            return mMinPower;
        }

        public Power getMaxPower() {
            return mMaxPower;
        }

        @Override
        public ExercisePerformanceGoal toExternalObject() {
            return new ExercisePerformanceGoal.PowerGoal(mMinPower, mMaxPower);
        }
    }

    public static final class SpeedGoalInternal extends ExercisePerformanceGoalInternal {
        public static final int SPEED_GOAL_TYPE_ID = 2;

        @Override
        void writeFieldsToParcel(Parcel parcel) {
            parcel.writeDouble(this.mMinSpeed.getInMetersPerSecond());
            parcel.writeDouble(this.mMaxSpeed.getInMetersPerSecond());
        }

        static SpeedGoalInternal readFieldsFromParcel(Parcel parcel) {
            return new SpeedGoalInternal(
                    Velocity.fromMetersPerSecond(parcel.readDouble()),
                    Velocity.fromMetersPerSecond(parcel.readDouble()));
        }

        @Override
        public int getTypeId() {
            return SPEED_GOAL_TYPE_ID;
        }

        private final Velocity mMinSpeed;
        private final Velocity mMaxSpeed;

        public SpeedGoalInternal(Velocity minSpeed, Velocity maxSpeed) {
            this.mMinSpeed = minSpeed;
            this.mMaxSpeed = maxSpeed;
        }

        public Velocity getMinSpeed() {
            return mMinSpeed;
        }

        public Velocity getMaxSpeed() {
            return mMaxSpeed;
        }

        @Override
        public ExercisePerformanceGoal toExternalObject() {
            return new ExercisePerformanceGoal.SpeedGoal(mMinSpeed, mMaxSpeed);
        }
    }

    public static final class CadenceGoalInternal extends ExercisePerformanceGoalInternal {
        public static final int CADENCE_GOAL_TYPE_ID = 3;

        @Override
        void writeFieldsToParcel(Parcel parcel) {
            parcel.writeDouble(this.mMinRpm);
            parcel.writeDouble(this.mMaxRpm);
        }

        static CadenceGoalInternal readFieldsFromParcel(Parcel parcel) {
            return new CadenceGoalInternal(parcel.readDouble(), parcel.readDouble());
        }

        @Override
        public int getTypeId() {
            return CADENCE_GOAL_TYPE_ID;
        }

        private final double mMinRpm;
        private final double mMaxRpm;

        public CadenceGoalInternal(double minRpm, double maxRpm) {
            this.mMinRpm = minRpm;
            this.mMaxRpm = maxRpm;
        }

        public double getMinRpm() {
            return mMinRpm;
        }

        public double getMaxRpm() {
            return mMaxRpm;
        }

        @Override
        public ExercisePerformanceGoal toExternalObject() {
            return new ExercisePerformanceGoal.CadenceGoal(mMinRpm, mMaxRpm);
        }
    }

    public static final class HeartRateGoalInternal extends ExercisePerformanceGoalInternal {
        public static final int HEART_RATE_GOAL_TYPE_ID = 4;

        @Override
        void writeFieldsToParcel(Parcel parcel) {
            parcel.writeInt(this.mMinBpm);
            parcel.writeInt(this.mMaxBpm);
        }

        static HeartRateGoalInternal readFieldsFromParcel(Parcel parcel) {
            return new HeartRateGoalInternal(parcel.readInt(), parcel.readInt());
        }

        @Override
        public int getTypeId() {
            return HEART_RATE_GOAL_TYPE_ID;
        }

        private final int mMinBpm;
        private final int mMaxBpm;

        public HeartRateGoalInternal(int minBpm, int maxBpm) {
            this.mMinBpm = minBpm;
            this.mMaxBpm = maxBpm;
        }

        public int getMinBpm() {
            return mMinBpm;
        }

        public int getMaxBpm() {
            return mMaxBpm;
        }

        @Override
        public ExercisePerformanceGoal toExternalObject() {
            return new ExercisePerformanceGoal.HeartRateGoal(mMinBpm, mMaxBpm);
        }
    }

    public static final class WeightGoalInternal extends ExercisePerformanceGoalInternal {
        public static final int WEIGHT_GOAL_TYPE_ID = 5;

        @Override
        void writeFieldsToParcel(Parcel parcel) {
            parcel.writeDouble(this.mMass.getInGrams());
        }

        static WeightGoalInternal readFieldsFromParcel(Parcel parcel) {
            return new WeightGoalInternal(Mass.fromGrams(parcel.readDouble()));
        }

        @Override
        public int getTypeId() {
            return WEIGHT_GOAL_TYPE_ID;
        }

        private final Mass mMass;

        public WeightGoalInternal(Mass mass) {
            this.mMass = mass;
        }

        public Mass getMass() {
            return mMass;
        }

        @Override
        public ExercisePerformanceGoal toExternalObject() {
            return new ExercisePerformanceGoal.WeightGoal(mMass);
        }
    }

    public static final class RateOfPerceivedExertionGoalInternal
            extends ExercisePerformanceGoalInternal {
        public static final int RATE_OF_PERCEIVED_EXERTION_TYPE_ID = 6;

        @Override
        void writeFieldsToParcel(Parcel parcel) {
            parcel.writeInt(this.mRpe);
        }

        static RateOfPerceivedExertionGoalInternal readFieldsFromParcel(Parcel parcel) {
            return new RateOfPerceivedExertionGoalInternal(parcel.readInt());
        }

        @Override
        public int getTypeId() {
            return RATE_OF_PERCEIVED_EXERTION_TYPE_ID;
        }

        private final int mRpe;

        public RateOfPerceivedExertionGoalInternal(int rpe) {
            this.mRpe = rpe;
        }

        public int getRpe() {
            return mRpe;
        }

        @Override
        public ExercisePerformanceGoal toExternalObject() {
            return new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(mRpe);
        }
    }

    public static final class AmrapGoalInternal extends ExercisePerformanceGoalInternal {
        public static final AmrapGoalInternal INSTANCE = new AmrapGoalInternal();

        public static final int AMRAP_GOAL_TYPE_ID = 7;

        @Override
        void writeFieldsToParcel(Parcel parcel) {
            // No fields to write.
        }

        @Override
        public int getTypeId() {
            return AMRAP_GOAL_TYPE_ID;
        }

        AmrapGoalInternal() {}

        @Override
        public ExercisePerformanceGoal toExternalObject() {
            return ExercisePerformanceGoal.AmrapGoal.INSTANCE;
        }
    }
}

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
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.Velocity;

import java.util.Objects;

/** A goal which should be aimed for during a {@link PlannedExerciseStep}. */
@FlaggedApi("com.android.healthconnect.flags.training_plans")
public abstract class ExercisePerformanceGoal {
    /**
     * An {@link ExercisePerformanceGoal} that requires a target power range to be met during the
     * associated {@link PlannedExerciseStep}.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class PowerGoal extends ExercisePerformanceGoal {
        private final Power mMinPower;
        private final Power mMaxPower;

        /**
         * @param minPower Minimum power level that should be reached.
         * @param maxPower Maximum power level that should be reached.
         */
        public PowerGoal(@NonNull Power minPower, @NonNull Power maxPower) {
            Objects.requireNonNull(minPower);
            Objects.requireNonNull(maxPower);
            this.mMinPower = minPower;
            this.mMaxPower = maxPower;
        }

        /**
         * @return Minimum power level that should be reached.
         */
        @NonNull
        public Power getMinPower() {
            return mMinPower;
        }

        /**
         * @return Maximum power level that should be reached.
         */
        @NonNull
        public Power getMaxPower() {
            return mMaxPower;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof PowerGoal)) return false;
            PowerGoal that = (PowerGoal) o;
            return this.getMinPower().equals(that.getMinPower())
                    && this.getMaxPower().equals(that.getMaxPower());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mMinPower, mMaxPower);
        }
    }

    /**
     * An {@link ExercisePerformanceGoal} that requires a target speed range to be met during the
     * associated {@link PlannedExerciseStep}. This can be used to represent pace or speed goals.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class SpeedGoal extends ExercisePerformanceGoal {
        private final Velocity mMinSpeed;
        private final Velocity mMaxSpeed;

        /**
         * @param minSpeed Minimum speed that should be reached.
         * @param maxSpeed Maximum speed that should be reached.
         */
        public SpeedGoal(@NonNull Velocity minSpeed, @NonNull Velocity maxSpeed) {
            Objects.requireNonNull(minSpeed);
            Objects.requireNonNull(maxSpeed);
            this.mMinSpeed = minSpeed;
            this.mMaxSpeed = maxSpeed;
        }

        /**
         * @return Minimum speed that should be reached.
         */
        @NonNull
        public Velocity getMinSpeed() {
            return mMinSpeed;
        }

        /**
         * @return Maximum speed that should be reached.
         */
        @NonNull
        public Velocity getMaxSpeed() {
            return mMaxSpeed;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof SpeedGoal)) return false;
            SpeedGoal that = (SpeedGoal) o;
            return this.getMinSpeed().equals(that.getMinSpeed())
                    && this.getMaxSpeed().equals(that.getMaxSpeed());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mMinSpeed, mMaxSpeed);
        }
    }

    /**
     * An {@link ExercisePerformanceGoal} that requires a target cadence range to be met during the
     * associated {@link PlannedExerciseStep}. The value may be interpreted as RPM for e.g. cycling
     * activities, or as steps per minute for e.g. walking/running activities.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class CadenceGoal extends ExercisePerformanceGoal {
        private final double mMinRpm;
        private final double mMaxRpm;

        /**
         * @param minRpm Minimum cadence level, in RPM, that should be reached.
         * @param maxRpm Maximum cadence level, in RPM, that should be reached.
         */
        public CadenceGoal(double minRpm, double maxRpm) {
            this.mMinRpm = minRpm;
            this.mMaxRpm = maxRpm;
        }

        /**
         * @return Minimum cadence level, in RPM, that should be reached.
         */
        public double getMinRpm() {
            return mMinRpm;
        }

        /**
         * @return Maximum cadence level, in RPM, that should be reached.
         */
        public double getMaxRpm() {
            return mMaxRpm;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof CadenceGoal)) return false;
            CadenceGoal that = (CadenceGoal) o;
            return this.getMinRpm() == that.getMinRpm() && this.getMaxRpm() == that.getMaxRpm();
        }

        @Override
        public int hashCode() {
            return Objects.hash(mMinRpm, mMaxRpm);
        }
    }

    /**
     * An {@link ExercisePerformanceGoal} that requires a target heart rate range, in BPM, to be met
     * during the associated {@link PlannedExerciseStep}.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class HeartRateGoal extends ExercisePerformanceGoal {
        private final int mMinBpm;
        private final int mMaxBpm;

        /**
         * @param minBpm Minimum heart rate, in BPM, that should be reached.
         * @param maxBpm Maximum heart rate, in BPM, that should be reached.
         */
        public HeartRateGoal(int minBpm, int maxBpm) {
            this.mMinBpm = minBpm;
            this.mMaxBpm = maxBpm;
        }

        /**
         * @return Minimum heart rate, in BPM, that should be reached.
         */
        public int getMinBpm() {
            return mMinBpm;
        }

        /**
         * @return Maximum heart rate, in BPM, that should be reached.
         */
        public int getMaxBpm() {
            return mMaxBpm;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof HeartRateGoal)) return false;
            HeartRateGoal that = (HeartRateGoal) o;
            return this.getMinBpm() == that.getMinBpm() && this.getMaxBpm() == that.getMaxBpm();
        }

        @Override
        public int hashCode() {
            return Objects.hash(mMinBpm, mMaxBpm);
        }
    }

    /**
     * An {@link ExercisePerformanceGoal} that requires a target weight to be lifted during the
     * associated {@link PlannedExerciseStep}.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class WeightGoal extends ExercisePerformanceGoal {
        private final Mass mMass;

        /**
         * @param mass Target weight that should be lifted.
         */
        public WeightGoal(@NonNull Mass mass) {
            Objects.requireNonNull(mass);
            this.mMass = mass;
        }

        /**
         * @return Target weight that should be lifted.
         */
        @NonNull
        public Mass getMass() {
            return mMass;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof WeightGoal)) return false;
            WeightGoal that = (WeightGoal) o;
            return this.getMass().equals(that.getMass());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mMass);
        }
    }

    /**
     * An {@link ExercisePerformanceGoal} that requires a target RPE (rate of perceived exertion) to
     * be met during the associated {@link PlannedExerciseStep}.
     *
     * <p>Values correspond to the Borg CR10 RPE scale and must be in the range 0 to 10 inclusive.
     * 0: No exertion (at rest) 1: Very light 2-3: Light 4-5: Moderate 6-7: Hard 8-9: Very hard 10:
     * Maximum effort
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class RateOfPerceivedExertionGoal extends ExercisePerformanceGoal {
        private final int mRpe;

        /**
         * @param rpe Target RPE that should be met.
         */
        public RateOfPerceivedExertionGoal(int rpe) {
            if (rpe < 0 || rpe > 10) {
                throw new IllegalArgumentException(
                        "RPE value must be between 0 and 10, inclusive.");
            }
            this.mRpe = rpe;
        }

        /**
         * @return Target RPE that should be met.
         */
        public int getRpe() {
            return mRpe;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof RateOfPerceivedExertionGoal)) return false;
            RateOfPerceivedExertionGoal that = (RateOfPerceivedExertionGoal) o;
            return this.getRpe() == that.getRpe();
        }

        @Override
        public int hashCode() {
            return Objects.hash(mRpe);
        }
    }

    /**
     * An {@link ExercisePerformanceGoal} that requires completing as many repetitions as possible.
     * AMRAP (as many reps as possible) sets are often used in conjunction with a duration based
     * completion goal.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class AmrapGoal extends ExercisePerformanceGoal {
        @NonNull public static final AmrapGoal INSTANCE = new AmrapGoal();

        private AmrapGoal() {}
    }

    /** An {@link ExercisePerformanceGoal} that is unknown. */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final class UnknownGoal extends ExercisePerformanceGoal {
        private UnknownGoal() {}

        @NonNull public static final UnknownGoal INSTANCE = new UnknownGoal();
    }

    private ExercisePerformanceGoal() {}
}

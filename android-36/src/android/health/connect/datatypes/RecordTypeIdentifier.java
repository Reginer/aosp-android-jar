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

package android.health.connect.datatypes;

import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY;
import static com.android.healthfitness.flags.Flags.FLAG_MINDFULNESS;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.SystemApi;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * Identifier for each data type, as returned by {@link Record#getRecordType()}. This is used at
 * various places to efficiently determine operations to perform on a data type.
 *
 * @hide
 */
@SystemApi
public final class RecordTypeIdentifier {
    public static final int RECORD_TYPE_UNKNOWN = 0;

    // Interval Records
    /**
     * Record type to capture the number of steps taken since the last reading. Each step is only
     * reported once so records shouldn't have overlapping time. The start time of each record
     * should represent the start of the interval in which steps were taken.
     *
     * @see StepsRecord
     */
    public static final int RECORD_TYPE_STEPS = 1;

    /**
     * Record Type to capture the estimated active energy burned by the user (in kilocalories),
     * excluding basal metabolic rate (BMR).
     *
     * @see ActiveCaloriesBurnedRecord
     */
    public static final int RECORD_TYPE_ACTIVE_CALORIES_BURNED = 2;

    /**
     * Record type to capture the amount of liquids user had in a single drink.
     *
     * @see HydrationRecord
     */
    public static final int RECORD_TYPE_HYDRATION = 3;

    /**
     * Record type to capture the elevation gained by the user since the last reading.
     *
     * @see ElevationGainedRecord
     */
    public static final int RECORD_TYPE_ELEVATION_GAINED = 4;

    /**
     * Record type to capture the number of floors climbed by the user between the start and end
     * time.
     *
     * @see FloorsClimbedRecord
     */
    public static final int RECORD_TYPE_FLOORS_CLIMBED = 5;

    /**
     * Record type to capture the number of wheelchair pushes done over a time interval. Each push
     * is only reported once so records shouldn't have overlapping time. The start time of each
     * record should represent the start of the interval in which pushes were made.
     *
     * @see WheelchairPushesRecord
     */
    public static final int RECORD_TYPE_WHEELCHAIR_PUSHES = 6;

    /**
     * Record type to capture the distance travelled by the user since the last reading. The total
     * distance over an interval can be calculated by adding together all the values during the
     * interval. The start time of each record should represent the start of the interval in which
     * the distance was covered. If break downs are preferred in scenario of a long workout,
     * consider writing multiple distance records. The start time of each record should be equal to
     * or greater than the end time of the previous record.
     *
     * @see DistanceRecord
     */
    public static final int RECORD_TYPE_DISTANCE = 7;

    /**
     * Record type to capture what nutrients were consumed as part of a meal or a food item.
     *
     * @see NutritionRecord
     */
    public static final int RECORD_TYPE_NUTRITION = 8;

    /**
     * Record type to capture the total energy burned by the user in Energy, including active &
     * basal energy burned (BMR). Each record represents the total kilocalories burned over a time
     * interval.
     *
     * @see TotalCaloriesBurnedRecord
     */
    public static final int RECORD_TYPE_TOTAL_CALORIES_BURNED = 9;

    /**
     * Record type to capture the menstruation period record. Each record contains the start / end
     * of the event.
     *
     * @see MenstruationPeriodRecord
     */
    public static final int RECORD_TYPE_MENSTRUATION_PERIOD = 10;

    // Series Records
    /**
     * Record type to capture the user's heart rate. Each record represents a series of
     * measurements.
     *
     * @see HeartRateRecord
     */
    public static final int RECORD_TYPE_HEART_RATE = 11;

    /**
     * Record type to capture the user's cycling pedaling cadence.
     *
     * @see CyclingPedalingCadenceRecord
     */
    public static final int RECORD_TYPE_CYCLING_PEDALING_CADENCE = 12;

    /**
     * Record type to capture the power generated by the user, e.g. during cycling or rowing with a
     * power meter.
     *
     * @see PowerRecord
     */
    public static final int RECORD_TYPE_POWER = 13;

    /**
     * Record type to capture the user's speed, e.g. during running or cycling.
     *
     * @see SpeedRecord
     */
    public static final int RECORD_TYPE_SPEED = 14;

    /**
     * Record type to capture the user's steps cadence.
     *
     * @see StepsCadenceRecord
     */
    public static final int RECORD_TYPE_STEPS_CADENCE = 15;

    // Instant records
    /**
     * Record type to capture the BMR of a user. Each record represents the energy a user would burn
     * if at rest all day, based on their height and weight.
     *
     * @see BasalMetabolicRateRecord
     */
    public static final int RECORD_TYPE_BASAL_METABOLIC_RATE = 16;

    /**
     * Record type to capture the body fat percentage of a user. Each record represents a person's
     * total body fat as a percentage of their total body mass.
     *
     * @see BodyFatRecord
     */
    public static final int RECORD_TYPE_BODY_FAT = 17;

    /**
     * Record type to capture user's VO2 max score and optionally the measurement method.
     *
     * @see Vo2MaxRecord
     */
    public static final int RECORD_TYPE_VO2_MAX = 18;

    /**
     * Record type to capture the description of cervical mucus. Each record represents a
     * self-assessed description of cervical mucus for a user. All fields are optional and can be
     * used to describe the look and feel of cervical mucus.
     *
     * @see CervicalMucusRecord
     */
    public static final int RECORD_TYPE_CERVICAL_MUCUS = 19;

    /**
     * Record type to capture the body temperature of a user when at rest (for example, immediately
     * after waking up). Can be used for checking the fertility window. Each data point represents a
     * single instantaneous body temperature measurement.
     *
     * @see BasalBodyTemperatureRecord
     */
    public static final int RECORD_TYPE_BASAL_BODY_TEMPERATURE = 20;

    /**
     * Record type for the description of how heavy a user's menstrual flow was (spotting, light,
     * medium, or heavy). Each record represents a description of how heavy the user's menstrual
     * bleeding was.
     *
     * @see MenstruationFlowRecord
     */
    public static final int RECORD_TYPE_MENSTRUATION_FLOW = 21;

    /**
     * Record type to capture the amount of oxygen circulating in the blood, measured as a
     * percentage of oxygen-saturated hemoglobin. Each record represents a single blood oxygen
     * saturation reading at the time of measurement.
     *
     * @see OxygenSaturationRecord
     */
    public static final int RECORD_TYPE_OXYGEN_SATURATION = 22;

    /**
     * Record type to capture the blood pressure of a user. Each record represents a single
     * instantaneous blood pressure reading.
     *
     * @see BloodPressureRecord
     */
    public static final int RECORD_TYPE_BLOOD_PRESSURE = 23;

    /**
     * Record type to capture the user's height.
     *
     * @see HeightRecord
     */
    public static final int RECORD_TYPE_HEIGHT = 24;

    /**
     * Record type to capture the concentration of glucose in the blood. Each record represents a
     * single instantaneous blood glucose reading.
     *
     * @see BloodGlucoseRecord
     */
    public static final int RECORD_TYPE_BLOOD_GLUCOSE = 25;

    /**
     * Record type to capture the user's weight.
     *
     * @see WeightRecord
     */
    public static final int RECORD_TYPE_WEIGHT = 26;

    /**
     * Record type to capture the user's lean body mass. Each record represents a single
     * instantaneous measurement.
     *
     * @see LeanBodyMassRecord
     */
    public static final int RECORD_TYPE_LEAN_BODY_MASS = 27;

    /**
     * Record type to capture an occurrence of sexual activity. Each record is a single occurrence.
     * ProtectionUsed field is optional.
     *
     * @see SexualActivityRecord
     */
    public static final int RECORD_TYPE_SEXUAL_ACTIVITY = 28;

    /**
     * Record type to capture the body temperature of a user. Each record represents a single
     * instantaneous body temperature measurement.
     *
     * @see BodyTemperatureRecord
     */
    public static final int RECORD_TYPE_BODY_TEMPERATURE = 29;

    /**
     * Each record represents the result of an ovulation test.
     *
     * @see OvulationTestRecord
     */
    public static final int RECORD_TYPE_OVULATION_TEST = 30;

    /**
     * Record type to capture the user's respiratory rate. Each record represents a single
     * instantaneous measurement.
     *
     * @see RespiratoryRateRecord
     */
    public static final int RECORD_TYPE_RESPIRATORY_RATE = 31;

    /**
     * Record type to capture the user's bone mass. Each record represents a single instantaneous
     * measurement.
     *
     * @see BoneMassRecord
     */
    public static final int RECORD_TYPE_BONE_MASS = 32;

    /**
     * Record type to capture the user's resting heart rate. Each record represents a single
     * instantaneous measurement.
     *
     * @see RestingHeartRateRecord
     */
    public static final int RECORD_TYPE_RESTING_HEART_RATE = 33;

    /**
     * Record type to capture the user's body water mass. Each record represents a single
     * instantaneous measurement.
     *
     * @see BodyWaterMassRecord
     */
    public static final int RECORD_TYPE_BODY_WATER_MASS = 34;

    /**
     * Record type to capture the user's heart rate variability RMSSD. Each record represents a
     * single instantaneous measurement.
     *
     * @see HeartRateVariabilityRmssdRecord
     */
    public static final int RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD = 35;

    /**
     * Record type to capture the user's inter-menstrual bleeding. Each record represents a single
     * instantaneous measurement.
     *
     * @see IntermenstrualBleedingRecord
     */
    public static final int RECORD_TYPE_INTERMENSTRUAL_BLEEDING = 36;

    // Session records
    /**
     * Record type to capture any other exercise a user does for which there is not a custom record
     * type. This can be common fitness exercise like running or different sports. Each record needs
     * a start time and end time. Records don't need to be back-to-back or directly after each
     * other, there can be gaps in between.
     *
     * @see ExerciseSessionRecord
     */
    public static final int RECORD_TYPE_EXERCISE_SESSION = 37;

    /**
     * Captures the user's sleep length and its stages. Each record represents a time interval for a
     * full sleep session.
     *
     * <p>All {@link SleepSessionRecord.Stage} time intervals should fall within the sleep session
     * interval. Time intervals for stages don't need to be continuous but shouldn't overlap.
     *
     * @see SleepSessionRecord
     */
    public static final int RECORD_TYPE_SLEEP_SESSION = 38;

    @FlaggedApi("com.android.healthconnect.flags.skin_temperature")
    public static final int RECORD_TYPE_SKIN_TEMPERATURE = 39;

    /**
     * Record type to capture planned exercise sessions, i.e. training plans. Each record represents
     * a single planned workout, where the start and end times indicate when the workout should
     * occur. Naturally, these times may be in the future.
     *
     * @see PlannedExerciseSessionRecord
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final int RECORD_TYPE_PLANNED_EXERCISE_SESSION = 40;

    /**
     * Record type to capture mindfulness sessions.
     *
     * <p>Each record represents a time interval for a mindfulness session and its type.
     *
     * @see MindfulnessSessionRecord
     */
    @FlaggedApi(FLAG_MINDFULNESS)
    public static final int RECORD_TYPE_MINDFULNESS_SESSION = 41;

    /**
     * @see ActivityIntensityRecord
     */
    @FlaggedApi(FLAG_ACTIVITY_INTENSITY)
    public static final int RECORD_TYPE_ACTIVITY_INTENSITY = 42;

    /**
     * Valid set of values for this IntDef. Update this set when add new type or deprecate existing
     * type.
     *
     * @deprecated Use {@link HealthConnectMappings#getAllRecordTypeIdentifiers()}
     * @hide
     */
    @Deprecated
    public static final Set<Integer> VALID_TYPES =
            Set.of(
                    RECORD_TYPE_UNKNOWN,
                    RECORD_TYPE_STEPS,
                    RECORD_TYPE_HEART_RATE,
                    RECORD_TYPE_BASAL_METABOLIC_RATE,
                    RECORD_TYPE_CYCLING_PEDALING_CADENCE,
                    RECORD_TYPE_POWER,
                    RECORD_TYPE_SPEED,
                    RECORD_TYPE_STEPS_CADENCE,
                    RECORD_TYPE_DISTANCE,
                    RECORD_TYPE_WHEELCHAIR_PUSHES,
                    RECORD_TYPE_TOTAL_CALORIES_BURNED,
                    RECORD_TYPE_FLOORS_CLIMBED,
                    RECORD_TYPE_ELEVATION_GAINED,
                    RECORD_TYPE_ACTIVE_CALORIES_BURNED,
                    RECORD_TYPE_HYDRATION,
                    RECORD_TYPE_NUTRITION,
                    RECORD_TYPE_RESPIRATORY_RATE,
                    RECORD_TYPE_BONE_MASS,
                    RECORD_TYPE_RESTING_HEART_RATE,
                    RECORD_TYPE_BODY_FAT,
                    RECORD_TYPE_VO2_MAX,
                    RECORD_TYPE_CERVICAL_MUCUS,
                    RECORD_TYPE_BASAL_BODY_TEMPERATURE,
                    RECORD_TYPE_MENSTRUATION_FLOW,
                    RECORD_TYPE_OXYGEN_SATURATION,
                    RECORD_TYPE_BLOOD_PRESSURE,
                    RECORD_TYPE_HEIGHT,
                    RECORD_TYPE_BLOOD_GLUCOSE,
                    RECORD_TYPE_WEIGHT,
                    RECORD_TYPE_LEAN_BODY_MASS,
                    RECORD_TYPE_SEXUAL_ACTIVITY,
                    RECORD_TYPE_BODY_TEMPERATURE,
                    RECORD_TYPE_OVULATION_TEST,
                    RECORD_TYPE_EXERCISE_SESSION,
                    RECORD_TYPE_PLANNED_EXERCISE_SESSION,
                    RECORD_TYPE_BODY_WATER_MASS,
                    RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD,
                    RECORD_TYPE_MENSTRUATION_PERIOD,
                    RECORD_TYPE_INTERMENSTRUAL_BLEEDING,
                    RECORD_TYPE_SLEEP_SESSION,
                    RECORD_TYPE_SKIN_TEMPERATURE,
                    RECORD_TYPE_MINDFULNESS_SESSION);

    private RecordTypeIdentifier() {}

    /** @hide */
    @IntDef({
        RECORD_TYPE_UNKNOWN,
        RECORD_TYPE_STEPS,
        RECORD_TYPE_HEART_RATE,
        RECORD_TYPE_BASAL_METABOLIC_RATE,
        RECORD_TYPE_CYCLING_PEDALING_CADENCE,
        RECORD_TYPE_POWER,
        RECORD_TYPE_SPEED,
        RECORD_TYPE_STEPS_CADENCE,
        RECORD_TYPE_DISTANCE,
        RECORD_TYPE_WHEELCHAIR_PUSHES,
        RECORD_TYPE_TOTAL_CALORIES_BURNED,
        RECORD_TYPE_FLOORS_CLIMBED,
        RECORD_TYPE_ELEVATION_GAINED,
        RECORD_TYPE_ACTIVE_CALORIES_BURNED,
        RECORD_TYPE_HYDRATION,
        RECORD_TYPE_NUTRITION,
        RECORD_TYPE_RESPIRATORY_RATE,
        RECORD_TYPE_BONE_MASS,
        RECORD_TYPE_RESTING_HEART_RATE,
        RECORD_TYPE_BODY_FAT,
        RECORD_TYPE_VO2_MAX,
        RECORD_TYPE_CERVICAL_MUCUS,
        RECORD_TYPE_BASAL_BODY_TEMPERATURE,
        RECORD_TYPE_MENSTRUATION_FLOW,
        RECORD_TYPE_OXYGEN_SATURATION,
        RECORD_TYPE_BLOOD_PRESSURE,
        RECORD_TYPE_HEIGHT,
        RECORD_TYPE_BLOOD_GLUCOSE,
        RECORD_TYPE_WEIGHT,
        RECORD_TYPE_LEAN_BODY_MASS,
        RECORD_TYPE_SEXUAL_ACTIVITY,
        RECORD_TYPE_BODY_TEMPERATURE,
        RECORD_TYPE_OVULATION_TEST,
        RECORD_TYPE_EXERCISE_SESSION,
        RECORD_TYPE_BODY_WATER_MASS,
        RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD,
        RECORD_TYPE_MENSTRUATION_PERIOD,
        RECORD_TYPE_INTERMENSTRUAL_BLEEDING,
        RECORD_TYPE_SLEEP_SESSION,
        RECORD_TYPE_SKIN_TEMPERATURE,
        RECORD_TYPE_PLANNED_EXERCISE_SESSION,
        RECORD_TYPE_MINDFULNESS_SESSION,
        RECORD_TYPE_ACTIVITY_INTENSITY
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecordType {}
}

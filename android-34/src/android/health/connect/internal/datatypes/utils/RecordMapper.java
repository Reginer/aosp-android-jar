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

package android.health.connect.internal.datatypes.utils;

import android.annotation.NonNull;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.BasalBodyTemperatureRecord;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.BloodGlucoseRecord;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.BodyFatRecord;
import android.health.connect.datatypes.BodyTemperatureRecord;
import android.health.connect.datatypes.BodyWaterMassRecord;
import android.health.connect.datatypes.BoneMassRecord;
import android.health.connect.datatypes.CervicalMucusRecord;
import android.health.connect.datatypes.CyclingPedalingCadenceRecord;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.ElevationGainedRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.FloorsClimbedRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.HeartRateVariabilityRmssdRecord;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.HydrationRecord;
import android.health.connect.datatypes.IntermenstrualBleedingRecord;
import android.health.connect.datatypes.LeanBodyMassRecord;
import android.health.connect.datatypes.MenstruationFlowRecord;
import android.health.connect.datatypes.MenstruationPeriodRecord;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.OvulationTestRecord;
import android.health.connect.datatypes.OxygenSaturationRecord;
import android.health.connect.datatypes.PowerRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.RespiratoryRateRecord;
import android.health.connect.datatypes.RestingHeartRateRecord;
import android.health.connect.datatypes.SexualActivityRecord;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.SpeedRecord;
import android.health.connect.datatypes.StepsCadenceRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.Vo2MaxRecord;
import android.health.connect.datatypes.WeightRecord;
import android.health.connect.datatypes.WheelchairPushesRecord;
import android.health.connect.internal.datatypes.ActiveCaloriesBurnedRecordInternal;
import android.health.connect.internal.datatypes.BasalBodyTemperatureRecordInternal;
import android.health.connect.internal.datatypes.BasalMetabolicRateRecordInternal;
import android.health.connect.internal.datatypes.BloodGlucoseRecordInternal;
import android.health.connect.internal.datatypes.BloodPressureRecordInternal;
import android.health.connect.internal.datatypes.BodyFatRecordInternal;
import android.health.connect.internal.datatypes.BodyTemperatureRecordInternal;
import android.health.connect.internal.datatypes.BodyWaterMassRecordInternal;
import android.health.connect.internal.datatypes.BoneMassRecordInternal;
import android.health.connect.internal.datatypes.CervicalMucusRecordInternal;
import android.health.connect.internal.datatypes.CyclingPedalingCadenceRecordInternal;
import android.health.connect.internal.datatypes.DistanceRecordInternal;
import android.health.connect.internal.datatypes.ElevationGainedRecordInternal;
import android.health.connect.internal.datatypes.ExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.FloorsClimbedRecordInternal;
import android.health.connect.internal.datatypes.HeartRateRecordInternal;
import android.health.connect.internal.datatypes.HeartRateVariabilityRmssdRecordInternal;
import android.health.connect.internal.datatypes.HeightRecordInternal;
import android.health.connect.internal.datatypes.HydrationRecordInternal;
import android.health.connect.internal.datatypes.IntermenstrualBleedingRecordInternal;
import android.health.connect.internal.datatypes.LeanBodyMassRecordInternal;
import android.health.connect.internal.datatypes.MenstruationFlowRecordInternal;
import android.health.connect.internal.datatypes.MenstruationPeriodRecordInternal;
import android.health.connect.internal.datatypes.NutritionRecordInternal;
import android.health.connect.internal.datatypes.OvulationTestRecordInternal;
import android.health.connect.internal.datatypes.OxygenSaturationRecordInternal;
import android.health.connect.internal.datatypes.PowerRecordInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.RespiratoryRateRecordInternal;
import android.health.connect.internal.datatypes.RestingHeartRateRecordInternal;
import android.health.connect.internal.datatypes.SexualActivityRecordInternal;
import android.health.connect.internal.datatypes.SleepSessionRecordInternal;
import android.health.connect.internal.datatypes.SpeedRecordInternal;
import android.health.connect.internal.datatypes.StepsCadenceRecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.health.connect.internal.datatypes.TotalCaloriesBurnedRecordInternal;
import android.health.connect.internal.datatypes.Vo2MaxRecordInternal;
import android.health.connect.internal.datatypes.WeightRecordInternal;
import android.health.connect.internal.datatypes.WheelchairPushesRecordInternal;
import android.util.ArrayMap;

import java.util.Map;

/** @hide */
public final class RecordMapper {
    private static final int NUM_ENTRIES = 35;
    private static volatile RecordMapper sRecordMapper;
    private final Map<Integer, Class<? extends RecordInternal<?>>>
            mRecordIdToInternalRecordClassMap;
    private final Map<Integer, Class<? extends Record>> mRecordIdToExternalRecordClassMap;
    private final Map<Class<? extends Record>, Integer> mExternalRecordClassToRecordIdMap;

    private RecordMapper() {
        mRecordIdToInternalRecordClassMap = new ArrayMap<>(NUM_ENTRIES);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_STEPS, StepsRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE, HeartRateRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED, FloorsClimbedRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HYDRATION, HydrationRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED,
                ActiveCaloriesBurnedRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED,
                ElevationGainedRecordInternal.class);

        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES,
                WheelchairPushesRecordInternal.class);

        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED,
                TotalCaloriesBurnedRecordInternal.class);

        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_DISTANCE, DistanceRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE,
                BasalMetabolicRateRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE,
                CyclingPedalingCadenceRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_POWER, PowerRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_NUTRITION, NutritionRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_SPEED, SpeedRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE, StepsCadenceRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_WATER_MASS,
                BodyWaterMassRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD,
                HeartRateVariabilityRmssdRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_PERIOD,
                MenstruationPeriodRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_INTERMENSTRUAL_BLEEDING,
                IntermenstrualBleedingRecordInternal.class);

        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_VO2_MAX, Vo2MaxRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY,
                SexualActivityRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE,
                RestingHeartRateRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_WEIGHT, WeightRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION,
                OxygenSaturationRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE,
                RespiratoryRateRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_TEMPERATURE,
                BodyTemperatureRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BONE_MASS, BoneMassRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE, BloodPressureRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_FAT, BodyFatRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE, BloodGlucoseRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BASAL_BODY_TEMPERATURE,
                BasalBodyTemperatureRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST, OvulationTestRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW,
                MenstruationFlowRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_CERVICAL_MUCUS, CervicalMucusRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEIGHT, HeightRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_LEAN_BODY_MASS, LeanBodyMassRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION,
                ExerciseSessionRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION, SleepSessionRecordInternal.class);

        mRecordIdToExternalRecordClassMap = new ArrayMap<>(NUM_ENTRIES);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_STEPS, StepsRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE, HeartRateRecord.class);

        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HYDRATION, HydrationRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED,
                ActiveCaloriesBurnedRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED, ElevationGainedRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_DISTANCE, DistanceRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_NUTRITION, NutritionRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED, FloorsClimbedRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES, WheelchairPushesRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED,
                TotalCaloriesBurnedRecord.class);

        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE,
                BasalMetabolicRateRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE,
                CyclingPedalingCadenceRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_POWER, PowerRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_SPEED, SpeedRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE, StepsCadenceRecord.class);

        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_TEMPERATURE, BodyTemperatureRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BONE_MASS, BoneMassRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE, BloodPressureRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_FAT, BodyFatRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE, BloodGlucoseRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BASAL_BODY_TEMPERATURE,
                BasalBodyTemperatureRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_CERVICAL_MUCUS, CervicalMucusRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_LEAN_BODY_MASS, LeanBodyMassRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW, MenstruationFlowRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEIGHT, HeightRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST, OvulationTestRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_VO2_MAX, Vo2MaxRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY, SexualActivityRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION, SleepSessionRecord.class);

        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE, RespiratoryRateRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION, OxygenSaturationRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE, RestingHeartRateRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_WEIGHT, WeightRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_WATER_MASS, BodyWaterMassRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD,
                HeartRateVariabilityRmssdRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_PERIOD,
                MenstruationPeriodRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_INTERMENSTRUAL_BLEEDING,
                IntermenstrualBleedingRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION, ExerciseSessionRecord.class);

        mExternalRecordClassToRecordIdMap =
                new ArrayMap<>(mRecordIdToExternalRecordClassMap.size());
        mRecordIdToExternalRecordClassMap.forEach(
                (id, recordClass) -> mExternalRecordClassToRecordIdMap.put(recordClass, id));
    }

    @NonNull
    public static synchronized RecordMapper getInstance() {
        if (sRecordMapper == null) {
            sRecordMapper = new RecordMapper();
        }

        return sRecordMapper;
    }

    @NonNull
    public Map<Integer, Class<? extends RecordInternal<?>>> getRecordIdToInternalRecordClassMap() {
        return mRecordIdToInternalRecordClassMap;
    }

    @NonNull
    public Map<Integer, Class<? extends Record>> getRecordIdToExternalRecordClassMap() {
        return mRecordIdToExternalRecordClassMap;
    }

    @RecordTypeIdentifier.RecordType
    public int getRecordType(Class<? extends Record> recordClass) {
        return mExternalRecordClassToRecordIdMap.get(recordClass);
    }
}

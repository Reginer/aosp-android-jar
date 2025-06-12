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

package android.health.connect.internal.datatypes.utils;

import static android.health.connect.HealthPermissions.READ_ACTIVE_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.READ_ACTIVITY_INTENSITY;
import static android.health.connect.HealthPermissions.READ_BASAL_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.READ_BASAL_METABOLIC_RATE;
import static android.health.connect.HealthPermissions.READ_BLOOD_GLUCOSE;
import static android.health.connect.HealthPermissions.READ_BLOOD_PRESSURE;
import static android.health.connect.HealthPermissions.READ_BODY_FAT;
import static android.health.connect.HealthPermissions.READ_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.READ_BODY_WATER_MASS;
import static android.health.connect.HealthPermissions.READ_BONE_MASS;
import static android.health.connect.HealthPermissions.READ_CERVICAL_MUCUS;
import static android.health.connect.HealthPermissions.READ_DISTANCE;
import static android.health.connect.HealthPermissions.READ_ELEVATION_GAINED;
import static android.health.connect.HealthPermissions.READ_EXERCISE;
import static android.health.connect.HealthPermissions.READ_FLOORS_CLIMBED;
import static android.health.connect.HealthPermissions.READ_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_HEART_RATE_VARIABILITY;
import static android.health.connect.HealthPermissions.READ_HEIGHT;
import static android.health.connect.HealthPermissions.READ_HYDRATION;
import static android.health.connect.HealthPermissions.READ_INTERMENSTRUAL_BLEEDING;
import static android.health.connect.HealthPermissions.READ_LEAN_BODY_MASS;
import static android.health.connect.HealthPermissions.READ_MENSTRUATION;
import static android.health.connect.HealthPermissions.READ_MINDFULNESS;
import static android.health.connect.HealthPermissions.READ_NUTRITION;
import static android.health.connect.HealthPermissions.READ_OVULATION_TEST;
import static android.health.connect.HealthPermissions.READ_OXYGEN_SATURATION;
import static android.health.connect.HealthPermissions.READ_PLANNED_EXERCISE;
import static android.health.connect.HealthPermissions.READ_POWER;
import static android.health.connect.HealthPermissions.READ_RESPIRATORY_RATE;
import static android.health.connect.HealthPermissions.READ_RESTING_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_SEXUAL_ACTIVITY;
import static android.health.connect.HealthPermissions.READ_SKIN_TEMPERATURE;
import static android.health.connect.HealthPermissions.READ_SLEEP;
import static android.health.connect.HealthPermissions.READ_SPEED;
import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.HealthPermissions.READ_TOTAL_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.READ_VO2_MAX;
import static android.health.connect.HealthPermissions.READ_WEIGHT;
import static android.health.connect.HealthPermissions.READ_WHEELCHAIR_PUSHES;
import static android.health.connect.HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.WRITE_ACTIVITY_INTENSITY;
import static android.health.connect.HealthPermissions.WRITE_BASAL_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.WRITE_BASAL_METABOLIC_RATE;
import static android.health.connect.HealthPermissions.WRITE_BLOOD_GLUCOSE;
import static android.health.connect.HealthPermissions.WRITE_BLOOD_PRESSURE;
import static android.health.connect.HealthPermissions.WRITE_BODY_FAT;
import static android.health.connect.HealthPermissions.WRITE_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.WRITE_BODY_WATER_MASS;
import static android.health.connect.HealthPermissions.WRITE_BONE_MASS;
import static android.health.connect.HealthPermissions.WRITE_CERVICAL_MUCUS;
import static android.health.connect.HealthPermissions.WRITE_DISTANCE;
import static android.health.connect.HealthPermissions.WRITE_ELEVATION_GAINED;
import static android.health.connect.HealthPermissions.WRITE_EXERCISE;
import static android.health.connect.HealthPermissions.WRITE_FLOORS_CLIMBED;
import static android.health.connect.HealthPermissions.WRITE_HEART_RATE;
import static android.health.connect.HealthPermissions.WRITE_HEART_RATE_VARIABILITY;
import static android.health.connect.HealthPermissions.WRITE_HEIGHT;
import static android.health.connect.HealthPermissions.WRITE_HYDRATION;
import static android.health.connect.HealthPermissions.WRITE_INTERMENSTRUAL_BLEEDING;
import static android.health.connect.HealthPermissions.WRITE_LEAN_BODY_MASS;
import static android.health.connect.HealthPermissions.WRITE_MENSTRUATION;
import static android.health.connect.HealthPermissions.WRITE_MINDFULNESS;
import static android.health.connect.HealthPermissions.WRITE_NUTRITION;
import static android.health.connect.HealthPermissions.WRITE_OVULATION_TEST;
import static android.health.connect.HealthPermissions.WRITE_OXYGEN_SATURATION;
import static android.health.connect.HealthPermissions.WRITE_PLANNED_EXERCISE;
import static android.health.connect.HealthPermissions.WRITE_POWER;
import static android.health.connect.HealthPermissions.WRITE_RESPIRATORY_RATE;
import static android.health.connect.HealthPermissions.WRITE_RESTING_HEART_RATE;
import static android.health.connect.HealthPermissions.WRITE_SEXUAL_ACTIVITY;
import static android.health.connect.HealthPermissions.WRITE_SKIN_TEMPERATURE;
import static android.health.connect.HealthPermissions.WRITE_SLEEP;
import static android.health.connect.HealthPermissions.WRITE_SPEED;
import static android.health.connect.HealthPermissions.WRITE_STEPS;
import static android.health.connect.HealthPermissions.WRITE_TOTAL_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.WRITE_VO2_MAX;
import static android.health.connect.HealthPermissions.WRITE_WEIGHT;
import static android.health.connect.HealthPermissions.WRITE_WHEELCHAIR_PUSHES;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVITY_INTENSITY;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_BODY_TEMPERATURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BODY_FAT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BODY_TEMPERATURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BODY_WATER_MASS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BONE_MASS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_CERVICAL_MUCUS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEIGHT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HYDRATION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_INTERMENSTRUAL_BLEEDING;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_LEAN_BODY_MASS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_PERIOD;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_MINDFULNESS_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NUTRITION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_PLANNED_EXERCISE_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_POWER;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SPEED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_VO2_MAX;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_WEIGHT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES;

import static com.android.internal.annotations.VisibleForTesting.Visibility.PACKAGE;

import android.health.connect.HealthDataCategory;
import android.health.connect.HealthPermissionCategory;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.ActivityIntensityRecord;
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
import android.health.connect.datatypes.MindfulnessSessionRecord;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.OvulationTestRecord;
import android.health.connect.datatypes.OxygenSaturationRecord;
import android.health.connect.datatypes.PlannedExerciseSessionRecord;
import android.health.connect.datatypes.PowerRecord;
import android.health.connect.datatypes.RespiratoryRateRecord;
import android.health.connect.datatypes.RestingHeartRateRecord;
import android.health.connect.datatypes.SexualActivityRecord;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.SpeedRecord;
import android.health.connect.datatypes.StepsCadenceRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.Vo2MaxRecord;
import android.health.connect.datatypes.WeightRecord;
import android.health.connect.datatypes.WheelchairPushesRecord;
import android.health.connect.internal.datatypes.ActiveCaloriesBurnedRecordInternal;
import android.health.connect.internal.datatypes.ActivityIntensityRecordInternal;
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
import android.health.connect.internal.datatypes.MindfulnessSessionRecordInternal;
import android.health.connect.internal.datatypes.NutritionRecordInternal;
import android.health.connect.internal.datatypes.OvulationTestRecordInternal;
import android.health.connect.internal.datatypes.OxygenSaturationRecordInternal;
import android.health.connect.internal.datatypes.PlannedExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.PowerRecordInternal;
import android.health.connect.internal.datatypes.RespiratoryRateRecordInternal;
import android.health.connect.internal.datatypes.RestingHeartRateRecordInternal;
import android.health.connect.internal.datatypes.SexualActivityRecordInternal;
import android.health.connect.internal.datatypes.SkinTemperatureRecordInternal;
import android.health.connect.internal.datatypes.SleepSessionRecordInternal;
import android.health.connect.internal.datatypes.SpeedRecordInternal;
import android.health.connect.internal.datatypes.StepsCadenceRecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.health.connect.internal.datatypes.TotalCaloriesBurnedRecordInternal;
import android.health.connect.internal.datatypes.Vo2MaxRecordInternal;
import android.health.connect.internal.datatypes.WeightRecordInternal;
import android.health.connect.internal.datatypes.WheelchairPushesRecordInternal;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** @hide */
@VisibleForTesting(visibility = PACKAGE)
public class DataTypeDescriptors {

    /** Returns descriptors for all supported data types. */
    @VisibleForTesting(visibility = PACKAGE)
    public static List<DataTypeDescriptor> getAllDataTypeDescriptors() {
        if (!Flags.healthConnectMappings()) {
            return List.of();
        }

        return Stream.of(
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_ACTIVE_CALORIES_BURNED)
                                .setPermissionCategory(
                                        HealthPermissionCategory.ACTIVE_CALORIES_BURNED)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_ACTIVE_CALORIES_BURNED)
                                .setWritePermission(WRITE_ACTIVE_CALORIES_BURNED)
                                .setRecordClass(ActiveCaloriesBurnedRecord.class)
                                .setRecordInternalClass(ActiveCaloriesBurnedRecordInternal.class)
                                .build(),
                        // Redundantly explicitly checking the flag to satisfy the linter.
                        Flags.activityIntensity() && AconfigFlagHelper.isActivityIntensityEnabled()
                                ? DataTypeDescriptor.builder()
                                        .setRecordTypeIdentifier(RECORD_TYPE_ACTIVITY_INTENSITY)
                                        .setPermissionCategory(
                                                HealthPermissionCategory.ACTIVITY_INTENSITY)
                                        .setDataCategory(HealthDataCategory.ACTIVITY)
                                        .setReadPermission(READ_ACTIVITY_INTENSITY)
                                        .setWritePermission(WRITE_ACTIVITY_INTENSITY)
                                        .setRecordClass(ActivityIntensityRecord.class)
                                        .setRecordInternalClass(
                                                ActivityIntensityRecordInternal.class)
                                        .build()
                                : null,
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BASAL_BODY_TEMPERATURE)
                                .setPermissionCategory(
                                        HealthPermissionCategory.BASAL_BODY_TEMPERATURE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setReadPermission(READ_BASAL_BODY_TEMPERATURE)
                                .setWritePermission(WRITE_BASAL_BODY_TEMPERATURE)
                                .setRecordClass(BasalBodyTemperatureRecord.class)
                                .setRecordInternalClass(BasalBodyTemperatureRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BASAL_METABOLIC_RATE)
                                .setPermissionCategory(
                                        HealthPermissionCategory.BASAL_METABOLIC_RATE)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setReadPermission(READ_BASAL_METABOLIC_RATE)
                                .setWritePermission(WRITE_BASAL_METABOLIC_RATE)
                                .setRecordClass(BasalMetabolicRateRecord.class)
                                .setRecordInternalClass(BasalMetabolicRateRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BLOOD_GLUCOSE)
                                .setPermissionCategory(HealthPermissionCategory.BLOOD_GLUCOSE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setReadPermission(READ_BLOOD_GLUCOSE)
                                .setWritePermission(WRITE_BLOOD_GLUCOSE)
                                .setRecordClass(BloodGlucoseRecord.class)
                                .setRecordInternalClass(BloodGlucoseRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BLOOD_PRESSURE)
                                .setPermissionCategory(HealthPermissionCategory.BLOOD_PRESSURE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setReadPermission(READ_BLOOD_PRESSURE)
                                .setWritePermission(WRITE_BLOOD_PRESSURE)
                                .setRecordClass(BloodPressureRecord.class)
                                .setRecordInternalClass(BloodPressureRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BODY_FAT)
                                .setPermissionCategory(HealthPermissionCategory.BODY_FAT)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setReadPermission(READ_BODY_FAT)
                                .setWritePermission(WRITE_BODY_FAT)
                                .setRecordClass(BodyFatRecord.class)
                                .setRecordInternalClass(BodyFatRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BODY_TEMPERATURE)
                                .setPermissionCategory(HealthPermissionCategory.BODY_TEMPERATURE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setReadPermission(READ_BODY_TEMPERATURE)
                                .setWritePermission(WRITE_BODY_TEMPERATURE)
                                .setRecordClass(BodyTemperatureRecord.class)
                                .setRecordInternalClass(BodyTemperatureRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BODY_WATER_MASS)
                                .setPermissionCategory(HealthPermissionCategory.BODY_WATER_MASS)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setReadPermission(READ_BODY_WATER_MASS)
                                .setWritePermission(WRITE_BODY_WATER_MASS)
                                .setRecordClass(BodyWaterMassRecord.class)
                                .setRecordInternalClass(BodyWaterMassRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BONE_MASS)
                                .setPermissionCategory(HealthPermissionCategory.BONE_MASS)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setReadPermission(READ_BONE_MASS)
                                .setWritePermission(WRITE_BONE_MASS)
                                .setRecordClass(BoneMassRecord.class)
                                .setRecordInternalClass(BoneMassRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_CERVICAL_MUCUS)
                                .setPermissionCategory(HealthPermissionCategory.CERVICAL_MUCUS)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setReadPermission(READ_CERVICAL_MUCUS)
                                .setWritePermission(WRITE_CERVICAL_MUCUS)
                                .setRecordClass(CervicalMucusRecord.class)
                                .setRecordInternalClass(CervicalMucusRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_CYCLING_PEDALING_CADENCE)
                                .setPermissionCategory(HealthPermissionCategory.EXERCISE)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_EXERCISE)
                                .setWritePermission(WRITE_EXERCISE)
                                .setRecordClass(CyclingPedalingCadenceRecord.class)
                                .setRecordInternalClass(CyclingPedalingCadenceRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_DISTANCE)
                                .setPermissionCategory(HealthPermissionCategory.DISTANCE)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_DISTANCE)
                                .setWritePermission(WRITE_DISTANCE)
                                .setRecordClass(DistanceRecord.class)
                                .setRecordInternalClass(DistanceRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_ELEVATION_GAINED)
                                .setPermissionCategory(HealthPermissionCategory.ELEVATION_GAINED)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_ELEVATION_GAINED)
                                .setWritePermission(WRITE_ELEVATION_GAINED)
                                .setRecordClass(ElevationGainedRecord.class)
                                .setRecordInternalClass(ElevationGainedRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_EXERCISE_SESSION)
                                .setPermissionCategory(HealthPermissionCategory.EXERCISE)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_EXERCISE)
                                .setWritePermission(WRITE_EXERCISE)
                                .setRecordClass(ExerciseSessionRecord.class)
                                .setRecordInternalClass(ExerciseSessionRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_FLOORS_CLIMBED)
                                .setPermissionCategory(HealthPermissionCategory.FLOORS_CLIMBED)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_FLOORS_CLIMBED)
                                .setWritePermission(WRITE_FLOORS_CLIMBED)
                                .setRecordClass(FloorsClimbedRecord.class)
                                .setRecordInternalClass(FloorsClimbedRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_HEART_RATE)
                                .setPermissionCategory(HealthPermissionCategory.HEART_RATE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setReadPermission(READ_HEART_RATE)
                                .setWritePermission(WRITE_HEART_RATE)
                                .setRecordClass(HeartRateRecord.class)
                                .setRecordInternalClass(HeartRateRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD)
                                .setPermissionCategory(
                                        HealthPermissionCategory.HEART_RATE_VARIABILITY)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setReadPermission(READ_HEART_RATE_VARIABILITY)
                                .setWritePermission(WRITE_HEART_RATE_VARIABILITY)
                                .setRecordClass(HeartRateVariabilityRmssdRecord.class)
                                .setRecordInternalClass(
                                        HeartRateVariabilityRmssdRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_HEIGHT)
                                .setPermissionCategory(HealthPermissionCategory.HEIGHT)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setReadPermission(READ_HEIGHT)
                                .setWritePermission(WRITE_HEIGHT)
                                .setRecordClass(HeightRecord.class)
                                .setRecordInternalClass(HeightRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_HYDRATION)
                                .setPermissionCategory(HealthPermissionCategory.HYDRATION)
                                .setDataCategory(HealthDataCategory.NUTRITION)
                                .setReadPermission(READ_HYDRATION)
                                .setWritePermission(WRITE_HYDRATION)
                                .setRecordClass(HydrationRecord.class)
                                .setRecordInternalClass(HydrationRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_INTERMENSTRUAL_BLEEDING)
                                .setPermissionCategory(
                                        HealthPermissionCategory.INTERMENSTRUAL_BLEEDING)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setReadPermission(READ_INTERMENSTRUAL_BLEEDING)
                                .setWritePermission(WRITE_INTERMENSTRUAL_BLEEDING)
                                .setRecordClass(IntermenstrualBleedingRecord.class)
                                .setRecordInternalClass(IntermenstrualBleedingRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_LEAN_BODY_MASS)
                                .setPermissionCategory(HealthPermissionCategory.LEAN_BODY_MASS)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setReadPermission(READ_LEAN_BODY_MASS)
                                .setWritePermission(WRITE_LEAN_BODY_MASS)
                                .setRecordClass(LeanBodyMassRecord.class)
                                .setRecordInternalClass(LeanBodyMassRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_MENSTRUATION_FLOW)
                                .setPermissionCategory(HealthPermissionCategory.MENSTRUATION)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setReadPermission(READ_MENSTRUATION)
                                .setWritePermission(WRITE_MENSTRUATION)
                                .setRecordClass(MenstruationFlowRecord.class)
                                .setRecordInternalClass(MenstruationFlowRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_MENSTRUATION_PERIOD)
                                .setPermissionCategory(HealthPermissionCategory.MENSTRUATION)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setReadPermission(READ_MENSTRUATION)
                                .setWritePermission(WRITE_MENSTRUATION)
                                .setRecordClass(MenstruationPeriodRecord.class)
                                .setRecordInternalClass(MenstruationPeriodRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_MINDFULNESS_SESSION)
                                .setPermissionCategory(HealthPermissionCategory.MINDFULNESS)
                                .setDataCategory(HealthDataCategory.WELLNESS)
                                .setReadPermission(READ_MINDFULNESS)
                                .setWritePermission(WRITE_MINDFULNESS)
                                .setRecordClass(MindfulnessSessionRecord.class)
                                .setRecordInternalClass(MindfulnessSessionRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_NUTRITION)
                                .setPermissionCategory(HealthPermissionCategory.NUTRITION)
                                .setDataCategory(HealthDataCategory.NUTRITION)
                                .setReadPermission(READ_NUTRITION)
                                .setWritePermission(WRITE_NUTRITION)
                                .setRecordClass(NutritionRecord.class)
                                .setRecordInternalClass(NutritionRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_OVULATION_TEST)
                                .setPermissionCategory(HealthPermissionCategory.OVULATION_TEST)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setReadPermission(READ_OVULATION_TEST)
                                .setWritePermission(WRITE_OVULATION_TEST)
                                .setRecordClass(OvulationTestRecord.class)
                                .setRecordInternalClass(OvulationTestRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_OXYGEN_SATURATION)
                                .setPermissionCategory(HealthPermissionCategory.OXYGEN_SATURATION)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setReadPermission(READ_OXYGEN_SATURATION)
                                .setWritePermission(WRITE_OXYGEN_SATURATION)
                                .setRecordClass(OxygenSaturationRecord.class)
                                .setRecordInternalClass(OxygenSaturationRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_PLANNED_EXERCISE_SESSION)
                                .setPermissionCategory(HealthPermissionCategory.PLANNED_EXERCISE)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_PLANNED_EXERCISE)
                                .setWritePermission(WRITE_PLANNED_EXERCISE)
                                .setRecordClass(PlannedExerciseSessionRecord.class)
                                .setRecordInternalClass(PlannedExerciseSessionRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_POWER)
                                .setPermissionCategory(HealthPermissionCategory.POWER)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_POWER)
                                .setWritePermission(WRITE_POWER)
                                .setRecordClass(PowerRecord.class)
                                .setRecordInternalClass(PowerRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_RESPIRATORY_RATE)
                                .setPermissionCategory(HealthPermissionCategory.RESPIRATORY_RATE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setReadPermission(READ_RESPIRATORY_RATE)
                                .setWritePermission(WRITE_RESPIRATORY_RATE)
                                .setRecordClass(RespiratoryRateRecord.class)
                                .setRecordInternalClass(RespiratoryRateRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_RESTING_HEART_RATE)
                                .setPermissionCategory(HealthPermissionCategory.RESTING_HEART_RATE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setReadPermission(READ_RESTING_HEART_RATE)
                                .setWritePermission(WRITE_RESTING_HEART_RATE)
                                .setRecordClass(RestingHeartRateRecord.class)
                                .setRecordInternalClass(RestingHeartRateRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_SEXUAL_ACTIVITY)
                                .setPermissionCategory(HealthPermissionCategory.SEXUAL_ACTIVITY)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setReadPermission(READ_SEXUAL_ACTIVITY)
                                .setWritePermission(WRITE_SEXUAL_ACTIVITY)
                                .setRecordClass(SexualActivityRecord.class)
                                .setRecordInternalClass(SexualActivityRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_SKIN_TEMPERATURE)
                                .setPermissionCategory(HealthPermissionCategory.SKIN_TEMPERATURE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setReadPermission(READ_SKIN_TEMPERATURE)
                                .setWritePermission(WRITE_SKIN_TEMPERATURE)
                                .setRecordClass(SkinTemperatureRecord.class)
                                .setRecordInternalClass(SkinTemperatureRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_SLEEP_SESSION)
                                .setPermissionCategory(HealthPermissionCategory.SLEEP)
                                .setDataCategory(HealthDataCategory.SLEEP)
                                .setReadPermission(READ_SLEEP)
                                .setWritePermission(WRITE_SLEEP)
                                .setRecordClass(SleepSessionRecord.class)
                                .setRecordInternalClass(SleepSessionRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_SPEED)
                                .setPermissionCategory(HealthPermissionCategory.SPEED)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_SPEED)
                                .setWritePermission(WRITE_SPEED)
                                .setRecordClass(SpeedRecord.class)
                                .setRecordInternalClass(SpeedRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_STEPS)
                                .setPermissionCategory(HealthPermissionCategory.STEPS)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_STEPS)
                                .setWritePermission(WRITE_STEPS)
                                .setRecordClass(StepsRecord.class)
                                .setRecordInternalClass(StepsRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_STEPS_CADENCE)
                                .setPermissionCategory(HealthPermissionCategory.STEPS)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_STEPS)
                                .setWritePermission(WRITE_STEPS)
                                .setRecordClass(StepsCadenceRecord.class)
                                .setRecordInternalClass(StepsCadenceRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_TOTAL_CALORIES_BURNED)
                                .setPermissionCategory(
                                        HealthPermissionCategory.TOTAL_CALORIES_BURNED)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_TOTAL_CALORIES_BURNED)
                                .setWritePermission(WRITE_TOTAL_CALORIES_BURNED)
                                .setRecordClass(TotalCaloriesBurnedRecord.class)
                                .setRecordInternalClass(TotalCaloriesBurnedRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_VO2_MAX)
                                .setPermissionCategory(HealthPermissionCategory.VO2_MAX)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_VO2_MAX)
                                .setWritePermission(WRITE_VO2_MAX)
                                .setRecordClass(Vo2MaxRecord.class)
                                .setRecordInternalClass(Vo2MaxRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_WEIGHT)
                                .setPermissionCategory(HealthPermissionCategory.WEIGHT)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setReadPermission(READ_WEIGHT)
                                .setWritePermission(WRITE_WEIGHT)
                                .setRecordClass(WeightRecord.class)
                                .setRecordInternalClass(WeightRecordInternal.class)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_WHEELCHAIR_PUSHES)
                                .setPermissionCategory(HealthPermissionCategory.WHEELCHAIR_PUSHES)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setReadPermission(READ_WHEELCHAIR_PUSHES)
                                .setWritePermission(WRITE_WHEELCHAIR_PUSHES)
                                .setRecordClass(WheelchairPushesRecord.class)
                                .setRecordInternalClass(WheelchairPushesRecordInternal.class)
                                .build())
                .filter(Objects::nonNull)
                .toList();
    }
}

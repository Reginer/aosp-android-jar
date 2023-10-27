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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED;
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
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NUTRITION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_POWER;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SPEED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_VO2_MAX;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_WEIGHT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES;

import android.annotation.SuppressLint;
import android.health.connect.HealthDataCategory;
import android.health.connect.HealthPermissionCategory;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.util.SparseIntArray;

import java.util.Objects;

/** @hide */
public final class RecordTypePermissionCategoryMapper {

    private static final String TAG = "RecordTypePermissionGroupMapper";
    private static SparseIntArray sRecordTypeHealthPermissionCategoryMap = new SparseIntArray();

    private RecordTypePermissionCategoryMapper() {}

    private static void populateDataTypeHealthPermissionCategoryMap() {
        sRecordTypeHealthPermissionCategoryMap =
                new SparseIntArray() {
                    {
                        put(RECORD_TYPE_STEPS, HealthPermissionCategory.STEPS);
                        put(RECORD_TYPE_HEART_RATE, HealthPermissionCategory.HEART_RATE);
                        put(
                                RECORD_TYPE_BASAL_METABOLIC_RATE,
                                HealthPermissionCategory.BASAL_METABOLIC_RATE);
                        put(
                                RECORD_TYPE_CYCLING_PEDALING_CADENCE,
                                HealthPermissionCategory.EXERCISE);
                        put(RECORD_TYPE_POWER, HealthPermissionCategory.POWER);
                        put(RECORD_TYPE_SPEED, HealthPermissionCategory.SPEED);
                        put(RECORD_TYPE_STEPS_CADENCE, HealthPermissionCategory.STEPS);
                        put(RECORD_TYPE_DISTANCE, HealthPermissionCategory.DISTANCE);
                        put(
                                RECORD_TYPE_WHEELCHAIR_PUSHES,
                                HealthPermissionCategory.WHEELCHAIR_PUSHES);
                        put(
                                RECORD_TYPE_TOTAL_CALORIES_BURNED,
                                HealthPermissionCategory.TOTAL_CALORIES_BURNED);
                        put(RECORD_TYPE_FLOORS_CLIMBED, HealthPermissionCategory.FLOORS_CLIMBED);
                        put(
                                RECORD_TYPE_ELEVATION_GAINED,
                                HealthPermissionCategory.ELEVATION_GAINED);
                        put(
                                RECORD_TYPE_ACTIVE_CALORIES_BURNED,
                                HealthPermissionCategory.ACTIVE_CALORIES_BURNED);
                        put(RECORD_TYPE_HYDRATION, HealthPermissionCategory.HYDRATION);
                        put(RECORD_TYPE_NUTRITION, HealthPermissionCategory.NUTRITION);
                        put(
                                RECORD_TYPE_BASAL_BODY_TEMPERATURE,
                                HealthPermissionCategory.BASAL_BODY_TEMPERATURE);
                        put(RECORD_TYPE_BLOOD_GLUCOSE, HealthPermissionCategory.BLOOD_GLUCOSE);
                        put(RECORD_TYPE_BLOOD_PRESSURE, HealthPermissionCategory.BLOOD_PRESSURE);
                        put(RECORD_TYPE_BODY_FAT, HealthPermissionCategory.BODY_FAT);
                        put(
                                RECORD_TYPE_BODY_TEMPERATURE,
                                HealthPermissionCategory.BODY_TEMPERATURE);
                        put(RECORD_TYPE_BONE_MASS, HealthPermissionCategory.BONE_MASS);
                        put(RECORD_TYPE_VO2_MAX, HealthPermissionCategory.VO2_MAX);
                        put(RECORD_TYPE_CERVICAL_MUCUS, HealthPermissionCategory.CERVICAL_MUCUS);
                        put(RECORD_TYPE_MENSTRUATION_FLOW, HealthPermissionCategory.MENSTRUATION);
                        put(
                                RECORD_TYPE_OXYGEN_SATURATION,
                                HealthPermissionCategory.OXYGEN_SATURATION);
                        put(RECORD_TYPE_OVULATION_TEST, HealthPermissionCategory.OVULATION_TEST);
                        put(RECORD_TYPE_LEAN_BODY_MASS, HealthPermissionCategory.LEAN_BODY_MASS);
                        put(RECORD_TYPE_SEXUAL_ACTIVITY, HealthPermissionCategory.SEXUAL_ACTIVITY);
                        put(
                                RECORD_TYPE_RESPIRATORY_RATE,
                                HealthPermissionCategory.RESPIRATORY_RATE);
                        put(
                                RECORD_TYPE_RESTING_HEART_RATE,
                                HealthPermissionCategory.RESTING_HEART_RATE);
                        put(RECORD_TYPE_HEIGHT, HealthPermissionCategory.HEIGHT);
                        put(RECORD_TYPE_WEIGHT, HealthPermissionCategory.WEIGHT);
                        put(RECORD_TYPE_BODY_WATER_MASS, HealthPermissionCategory.BODY_WATER_MASS);
                        put(
                                RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD,
                                HealthPermissionCategory.HEART_RATE_VARIABILITY);
                        put(RECORD_TYPE_MENSTRUATION_PERIOD, HealthPermissionCategory.MENSTRUATION);
                        put(
                                RECORD_TYPE_INTERMENSTRUAL_BLEEDING,
                                HealthPermissionCategory.INTERMENSTRUAL_BLEEDING);
                        put(RECORD_TYPE_EXERCISE_SESSION, HealthPermissionCategory.EXERCISE);
                        put(RECORD_TYPE_SLEEP_SESSION, HealthPermissionCategory.SLEEP);
                    }
                };
    }

    /** Returns {@link HealthDataCategory} for the input {@link RecordTypeIdentifier.RecordType}. */
    @SuppressLint("LongLogTag")
    @HealthPermissionCategory.Type
    public static int getHealthPermissionCategoryForRecordType(
            @RecordTypeIdentifier.RecordType int recordType) {
        if (sRecordTypeHealthPermissionCategoryMap.size() == 0) {
            populateDataTypeHealthPermissionCategoryMap();
        }
        @HealthPermissionCategory.Type
        Integer recordPermission = sRecordTypeHealthPermissionCategoryMap.get(recordType);
        Objects.requireNonNull(
                recordPermission,
                "Health Permission Category not found for " + "RecordType :" + recordType);
        return recordPermission;
    }
}

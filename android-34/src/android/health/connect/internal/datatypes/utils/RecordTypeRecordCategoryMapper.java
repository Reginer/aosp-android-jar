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

import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.HealthDataCategory.BODY_MEASUREMENTS;
import static android.health.connect.HealthDataCategory.CYCLE_TRACKING;
import static android.health.connect.HealthDataCategory.NUTRITION;
import static android.health.connect.HealthDataCategory.SLEEP;
import static android.health.connect.HealthDataCategory.VITALS;
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
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.util.SparseIntArray;

import java.util.Objects;

/** @hide */
public final class RecordTypeRecordCategoryMapper {

    private static SparseIntArray sRecordTypeToRecordCategoryMapper = new SparseIntArray();

    private RecordTypeRecordCategoryMapper() {}

    private static void populateRecordTypeToRecordCategoryMap() {
        sRecordTypeToRecordCategoryMapper =
                new SparseIntArray() {
                    {
                        put(RECORD_TYPE_STEPS, ACTIVITY);
                        put(RECORD_TYPE_HEART_RATE, VITALS);
                        put(RECORD_TYPE_BASAL_METABOLIC_RATE, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_CYCLING_PEDALING_CADENCE, ACTIVITY);
                        put(RECORD_TYPE_POWER, ACTIVITY);
                        put(RECORD_TYPE_SPEED, ACTIVITY);
                        put(RECORD_TYPE_STEPS_CADENCE, ACTIVITY);
                        put(RECORD_TYPE_DISTANCE, ACTIVITY);
                        put(RECORD_TYPE_WHEELCHAIR_PUSHES, ACTIVITY);
                        put(RECORD_TYPE_TOTAL_CALORIES_BURNED, ACTIVITY);
                        put(RECORD_TYPE_FLOORS_CLIMBED, ACTIVITY);
                        put(RECORD_TYPE_ELEVATION_GAINED, ACTIVITY);
                        put(RECORD_TYPE_ACTIVE_CALORIES_BURNED, ACTIVITY);
                        put(RECORD_TYPE_HYDRATION, NUTRITION);
                        put(RECORD_TYPE_NUTRITION, NUTRITION);
                        put(RECORD_TYPE_RESPIRATORY_RATE, VITALS);
                        put(RECORD_TYPE_BONE_MASS, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_RESTING_HEART_RATE, VITALS);
                        put(RECORD_TYPE_BODY_FAT, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_VO2_MAX, ACTIVITY);
                        put(RECORD_TYPE_CERVICAL_MUCUS, CYCLE_TRACKING);
                        put(RECORD_TYPE_BASAL_BODY_TEMPERATURE, VITALS);
                        put(RECORD_TYPE_MENSTRUATION_FLOW, CYCLE_TRACKING);
                        put(RECORD_TYPE_OXYGEN_SATURATION, VITALS);
                        put(RECORD_TYPE_BLOOD_PRESSURE, VITALS);
                        put(RECORD_TYPE_HEIGHT, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_BLOOD_GLUCOSE, VITALS);
                        put(RECORD_TYPE_WEIGHT, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_LEAN_BODY_MASS, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_SEXUAL_ACTIVITY, CYCLE_TRACKING);
                        put(RECORD_TYPE_BODY_TEMPERATURE, VITALS);
                        put(RECORD_TYPE_OVULATION_TEST, CYCLE_TRACKING);
                        put(RECORD_TYPE_BODY_WATER_MASS, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD, VITALS);
                        put(RECORD_TYPE_MENSTRUATION_PERIOD, CYCLE_TRACKING);
                        put(RECORD_TYPE_INTERMENSTRUAL_BLEEDING, CYCLE_TRACKING);
                        put(RECORD_TYPE_EXERCISE_SESSION, ACTIVITY);
                        put(RECORD_TYPE_SLEEP_SESSION, SLEEP);
                    }
                };
    }

    /** Returns {@link HealthDataCategory} for the input {@link RecordTypeIdentifier.RecordType}. */
    @SuppressLint("LongLogTag")
    @HealthDataCategory.Type
    public static int getRecordCategoryForRecordType(
            @RecordTypeIdentifier.RecordType int recordType) {
        if (sRecordTypeToRecordCategoryMapper.size() == 0) {
            populateRecordTypeToRecordCategoryMap();
        }
        @HealthDataCategory.Type
        Integer recordCategory = sRecordTypeToRecordCategoryMapper.get(recordType);
        Objects.requireNonNull(
                recordCategory, "Record Category not found for Record Type :" + recordType);
        return recordCategory;
    }
}

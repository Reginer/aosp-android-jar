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

import static android.health.connect.datatypes.validation.ValidationUtils.validateIntDefValue;

import android.annotation.IntDef;
import android.health.connect.HealthConnectManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * Class to represent aggregation types in {@link Record} classes.
 *
 * <p>New objects of this class cannot be created.
 *
 * <p>Pre-created (defined in health {@link Record} types) objects of this class can be used to
 * query and fetch aggregate results using aggregate APIs in {@link HealthConnectManager}
 *
 * @see HealthConnectManager#aggregate
 */
public final class AggregationType<T> {
    /** @hide */
    public static final int MAX = 0;

    /** @hide */
    public static final int MIN = 1;

    /** @hide */
    public static final int AVG = 2;

    /** @hide */
    public static final int SUM = 3;

    /** @hide */
    public static final int COUNT = 4;

    @AggregationTypeIdentifier.Id private final int mId;
    @AggregateOperationType private final int mType;
    @RecordTypeIdentifier.RecordType private final int mApplicableRecordTypeId;
    private final Class<T> mClass;

    /** @hide */
    AggregationType(
            @AggregationTypeIdentifier.Id int id,
            @AggregateOperationType int type,
            @RecordTypeIdentifier.RecordType int applicableRecordTypeId,
            Class<T> templateClass) {
        validateIntDefValue(
                id,
                AggregationTypeIdentifier.IDENTIFIER_VALID_TYPES,
                AggregationTypeIdentifier.class.getSimpleName());
        validateIntDefValue(
                type, OPERATION_VALID_TYPES, AggregateOperationType.class.getSimpleName());

        mId = id;
        mType = type;
        mApplicableRecordTypeId = applicableRecordTypeId;
        mClass = templateClass;
    }

    /** @hide */
    @AggregationTypeIdentifier.Id
    public int getAggregationTypeIdentifier() {
        return mId;
    }

    /** @hide */
    @RecordTypeIdentifier.RecordType
    public int getApplicableRecordTypeId() {
        return mApplicableRecordTypeId;
    }

    /** @hide */
    @AggregateOperationType
    public int getAggregateOperationType() {
        return mType;
    }

    /** @hide */
    public Class<T> getAggregateResultClass() {
        return mClass;
    }

    /**
     * Identifier for each aggregate type, as returned by {@link
     * AggregationType#getAggregationTypeIdentifier()}. This is used at various places to determine
     * operations to perform on aggregate type.
     *
     * @hide
     */
    public @interface AggregationTypeIdentifier {
        int HEART_RATE_RECORD_BPM_MAX = 0;
        int HEART_RATE_RECORD_BPM_MIN = 1;
        int STEPS_RECORD_COUNT_TOTAL = 2;
        int ACTIVE_CALORIES_BURNED_RECORD_ACTIVE_CALORIES_TOTAL = 3;
        int BMR_RECORD_BASAL_CALORIES_TOTAL = 4;
        int DISTANCE_RECORD_DISTANCE_TOTAL = 5;
        int ELEVATION_RECORD_ELEVATION_GAINED_TOTAL = 6;
        int HEART_RATE_RECORD_BPM_AVG = 7;
        int POWER_RECORD_POWER_MIN = 8;
        int POWER_RECORD_POWER_MAX = 9;
        int POWER_RECORD_POWER_AVG = 10;
        int HYDRATION_RECORD_VOLUME_TOTAL = 11;
        int FLOORS_CLIMBED_RECORD_FLOORS_CLIMBED_TOTAL = 12;
        int NUTRITION_RECORD_BIOTIN_TOTAL = 13;
        int NUTRITION_RECORD_CAFFEINE_TOTAL = 14;
        int NUTRITION_RECORD_CALCIUM_TOTAL = 15;
        int NUTRITION_RECORD_CHLORIDE_TOTAL = 16;
        int NUTRITION_RECORD_CHOLESTEROL_TOTAL = 17;
        int NUTRITION_RECORD_CHROMIUM_TOTAL = 18;
        int NUTRITION_RECORD_COPPER_TOTAL = 19;
        int NUTRITION_RECORD_DIETARY_FIBER_TOTAL = 20;
        int NUTRITION_RECORD_ENERGY_TOTAL = 21;
        int NUTRITION_RECORD_ENERGY_FROM_FAT_TOTAL = 22;
        int NUTRITION_RECORD_FOLATE_TOTAL = 23;
        int NUTRITION_RECORD_FOLIC_ACID_TOTAL = 24;
        int NUTRITION_RECORD_IODINE_TOTAL = 25;
        int NUTRITION_RECORD_IRON_TOTAL = 26;
        int NUTRITION_RECORD_MAGNESIUM_TOTAL = 27;
        int NUTRITION_RECORD_MANGANESE_TOTAL = 28;
        int NUTRITION_RECORD_MOLYBDENUM_TOTAL = 29;
        int NUTRITION_RECORD_MONOUNSATURATED_FAT_TOTAL = 30;
        int NUTRITION_RECORD_NIACIN_TOTAL = 31;
        int NUTRITION_RECORD_PANTOTHENIC_ACID_TOTAL = 32;
        int NUTRITION_RECORD_PHOSPHORUS_TOTAL = 33;
        int NUTRITION_RECORD_POLYUNSATURATED_FAT_TOTAL = 34;
        int NUTRITION_RECORD_POTASSIUM_TOTAL = 35;
        int NUTRITION_RECORD_PROTEIN_TOTAL = 36;
        int NUTRITION_RECORD_RIBOFLAVIN_TOTAL = 37;
        int NUTRITION_RECORD_SATURATED_FAT_TOTAL = 38;
        int NUTRITION_RECORD_SELENIUM_TOTAL = 39;
        int NUTRITION_RECORD_SODIUM_TOTAL = 40;
        int NUTRITION_RECORD_SUGAR_TOTAL = 41;
        int NUTRITION_RECORD_THIAMIN_TOTAL = 42;
        int NUTRITION_RECORD_TOTAL_CARBOHYDRATE_TOTAL = 43;
        int NUTRITION_RECORD_TOTAL_FAT_TOTAL = 44;
        int NUTRITION_RECORD_UNSATURATED_FAT_TOTAL = 45;
        int NUTRITION_RECORD_VITAMIN_A_TOTAL = 46;
        int NUTRITION_RECORD_VITAMIN_B12_TOTAL = 47;
        int NUTRITION_RECORD_VITAMIN_B6_TOTAL = 48;
        int NUTRITION_RECORD_VITAMIN_C_TOTAL = 49;
        int NUTRITION_RECORD_VITAMIN_D_TOTAL = 50;
        int NUTRITION_RECORD_VITAMIN_E_TOTAL = 51;
        int NUTRITION_RECORD_VITAMIN_K_TOTAL = 52;
        int NUTRITION_RECORD_ZINC_TOTAL = 53;
        int HEIGHT_RECORD_HEIGHT_AVG = 54;
        int HEIGHT_RECORD_HEIGHT_MAX = 55;
        int HEIGHT_RECORD_HEIGHT_MIN = 56;
        int RESTING_HEART_RATE_RECORD_BPM_MAX = 57;
        int RESTING_HEART_RATE_RECORD_BPM_MIN = 58;
        int TOTAL_CALORIES_BURNED_RECORD_ENERGY_TOTAL = 59;
        int WEIGHT_RECORD_WEIGHT_AVG = 60;
        int WEIGHT_RECORD_WEIGHT_MAX = 61;
        int WEIGHT_RECORD_WEIGHT_MIN = 62;
        int WHEEL_CHAIR_PUSHES_RECORD_COUNT_TOTAL = 63;
        int HEART_RATE_RECORD_MEASUREMENTS_COUNT = 64;
        int RESTING_HEART_RATE_RECORD_BPM_AVG = 65;
        int SLEEP_SESSION_DURATION_TOTAL = 66;
        int EXERCISE_SESSION_DURATION_TOTAL = 67;
        int BLOOD_PRESSURE_RECORD_DIASTOLIC_AVG = 68;
        int BLOOD_PRESSURE_RECORD_DIASTOLIC_MAX = 69;
        int BLOOD_PRESSURE_RECORD_DIASTOLIC_MIN = 70;
        int BLOOD_PRESSURE_RECORD_SYSTOLIC_AVG = 71;
        int BLOOD_PRESSURE_RECORD_SYSTOLIC_MAX = 72;
        int BLOOD_PRESSURE_RECORD_SYSTOLIC_MIN = 73;
        int NUTRITION_RECORD_TRANS_FAT_TOTAL = 74;
        int CYCLING_PEDALING_CADENCE_RECORD_RPM_AVG = 75;
        int CYCLING_PEDALING_CADENCE_RECORD_RPM_MIN = 76;
        int CYCLING_PEDALING_CADENCE_RECORD_RPM_MAX = 77;
        int SPEED_RECORD_SPEED_AVG = 78;
        int SPEED_RECORD_SPEED_MIN = 79;
        int SPEED_RECORD_SPEED_MAX = 80;
        int STEPS_CADENCE_RECORD_RATE_AVG = 81;
        int STEPS_CADENCE_RECORD_RATE_MIN = 82;
        int STEPS_CADENCE_RECORD_RATE_MAX = 83;
        int SKIN_TEMPERATURE_RECORD_DELTA_AVG = 84;
        int SKIN_TEMPERATURE_RECORD_DELTA_MIN = 85;
        int SKIN_TEMPERATURE_RECORD_DELTA_MAX = 86;
        int MINDFULNESS_SESSION_DURATION_TOTAL = 87;
        int ACTIVITY_INTENSITY_MODERATE_DURATION_TOTAL = 88;
        int ACTIVITY_INTENSITY_VIGOROUS_DURATION_TOTAL = 89;
        int ACTIVITY_INTENSITY_DURATION_TOTAL = 90;
        int ACTIVITY_INTENSITY_MINUTES_TOTAL = 91;

        /**
         * Valid set of values for this IntDef. Update this set when add new type or deprecate
         * existing type.
         *
         * @hide
         */
        Set<Integer> IDENTIFIER_VALID_TYPES =
                Set.of(
                        HEART_RATE_RECORD_BPM_MAX,
                        HEART_RATE_RECORD_BPM_MIN,
                        STEPS_RECORD_COUNT_TOTAL,
                        ACTIVE_CALORIES_BURNED_RECORD_ACTIVE_CALORIES_TOTAL,
                        BMR_RECORD_BASAL_CALORIES_TOTAL,
                        DISTANCE_RECORD_DISTANCE_TOTAL,
                        ELEVATION_RECORD_ELEVATION_GAINED_TOTAL,
                        HEART_RATE_RECORD_BPM_AVG,
                        POWER_RECORD_POWER_MIN,
                        POWER_RECORD_POWER_MAX,
                        POWER_RECORD_POWER_AVG,
                        HYDRATION_RECORD_VOLUME_TOTAL,
                        FLOORS_CLIMBED_RECORD_FLOORS_CLIMBED_TOTAL,
                        CYCLING_PEDALING_CADENCE_RECORD_RPM_AVG,
                        CYCLING_PEDALING_CADENCE_RECORD_RPM_MIN,
                        CYCLING_PEDALING_CADENCE_RECORD_RPM_MAX,
                        NUTRITION_RECORD_BIOTIN_TOTAL,
                        NUTRITION_RECORD_CAFFEINE_TOTAL,
                        NUTRITION_RECORD_CALCIUM_TOTAL,
                        NUTRITION_RECORD_CHLORIDE_TOTAL,
                        NUTRITION_RECORD_CHOLESTEROL_TOTAL,
                        NUTRITION_RECORD_CHROMIUM_TOTAL,
                        NUTRITION_RECORD_COPPER_TOTAL,
                        NUTRITION_RECORD_DIETARY_FIBER_TOTAL,
                        NUTRITION_RECORD_ENERGY_TOTAL,
                        NUTRITION_RECORD_ENERGY_FROM_FAT_TOTAL,
                        NUTRITION_RECORD_FOLATE_TOTAL,
                        NUTRITION_RECORD_FOLIC_ACID_TOTAL,
                        NUTRITION_RECORD_IODINE_TOTAL,
                        NUTRITION_RECORD_IRON_TOTAL,
                        NUTRITION_RECORD_MAGNESIUM_TOTAL,
                        NUTRITION_RECORD_MANGANESE_TOTAL,
                        NUTRITION_RECORD_MOLYBDENUM_TOTAL,
                        NUTRITION_RECORD_MONOUNSATURATED_FAT_TOTAL,
                        NUTRITION_RECORD_NIACIN_TOTAL,
                        NUTRITION_RECORD_PANTOTHENIC_ACID_TOTAL,
                        NUTRITION_RECORD_PHOSPHORUS_TOTAL,
                        NUTRITION_RECORD_POLYUNSATURATED_FAT_TOTAL,
                        NUTRITION_RECORD_POTASSIUM_TOTAL,
                        NUTRITION_RECORD_PROTEIN_TOTAL,
                        NUTRITION_RECORD_RIBOFLAVIN_TOTAL,
                        NUTRITION_RECORD_SATURATED_FAT_TOTAL,
                        NUTRITION_RECORD_SELENIUM_TOTAL,
                        NUTRITION_RECORD_SODIUM_TOTAL,
                        NUTRITION_RECORD_SUGAR_TOTAL,
                        NUTRITION_RECORD_THIAMIN_TOTAL,
                        NUTRITION_RECORD_TOTAL_CARBOHYDRATE_TOTAL,
                        NUTRITION_RECORD_TOTAL_FAT_TOTAL,
                        NUTRITION_RECORD_TRANS_FAT_TOTAL,
                        NUTRITION_RECORD_UNSATURATED_FAT_TOTAL,
                        NUTRITION_RECORD_VITAMIN_A_TOTAL,
                        NUTRITION_RECORD_VITAMIN_B12_TOTAL,
                        NUTRITION_RECORD_VITAMIN_B6_TOTAL,
                        NUTRITION_RECORD_VITAMIN_C_TOTAL,
                        NUTRITION_RECORD_VITAMIN_D_TOTAL,
                        NUTRITION_RECORD_VITAMIN_E_TOTAL,
                        NUTRITION_RECORD_VITAMIN_K_TOTAL,
                        NUTRITION_RECORD_ZINC_TOTAL,
                        HEIGHT_RECORD_HEIGHT_AVG,
                        HEIGHT_RECORD_HEIGHT_MAX,
                        HEIGHT_RECORD_HEIGHT_MIN,
                        RESTING_HEART_RATE_RECORD_BPM_MAX,
                        RESTING_HEART_RATE_RECORD_BPM_MIN,
                        TOTAL_CALORIES_BURNED_RECORD_ENERGY_TOTAL,
                        WEIGHT_RECORD_WEIGHT_AVG,
                        WEIGHT_RECORD_WEIGHT_MAX,
                        WEIGHT_RECORD_WEIGHT_MIN,
                        WHEEL_CHAIR_PUSHES_RECORD_COUNT_TOTAL,
                        HEART_RATE_RECORD_MEASUREMENTS_COUNT,
                        RESTING_HEART_RATE_RECORD_BPM_AVG,
                        SLEEP_SESSION_DURATION_TOTAL,
                        EXERCISE_SESSION_DURATION_TOTAL,
                        BLOOD_PRESSURE_RECORD_DIASTOLIC_AVG,
                        BLOOD_PRESSURE_RECORD_DIASTOLIC_MAX,
                        BLOOD_PRESSURE_RECORD_DIASTOLIC_MIN,
                        BLOOD_PRESSURE_RECORD_SYSTOLIC_AVG,
                        BLOOD_PRESSURE_RECORD_SYSTOLIC_MAX,
                        BLOOD_PRESSURE_RECORD_SYSTOLIC_MIN,
                        SPEED_RECORD_SPEED_AVG,
                        SPEED_RECORD_SPEED_MIN,
                        SPEED_RECORD_SPEED_MAX,
                        STEPS_CADENCE_RECORD_RATE_AVG,
                        STEPS_CADENCE_RECORD_RATE_MIN,
                        STEPS_CADENCE_RECORD_RATE_MAX,
                        SKIN_TEMPERATURE_RECORD_DELTA_AVG,
                        SKIN_TEMPERATURE_RECORD_DELTA_MIN,
                        SKIN_TEMPERATURE_RECORD_DELTA_MAX,
                        MINDFULNESS_SESSION_DURATION_TOTAL,
                        ACTIVITY_INTENSITY_MODERATE_DURATION_TOTAL,
                        ACTIVITY_INTENSITY_VIGOROUS_DURATION_TOTAL,
                        ACTIVITY_INTENSITY_DURATION_TOTAL,
                        ACTIVITY_INTENSITY_MINUTES_TOTAL);

        /** @hide */
        @IntDef({
            HEART_RATE_RECORD_BPM_MAX,
            HEART_RATE_RECORD_BPM_MIN,
            STEPS_RECORD_COUNT_TOTAL,
            ACTIVE_CALORIES_BURNED_RECORD_ACTIVE_CALORIES_TOTAL,
            BMR_RECORD_BASAL_CALORIES_TOTAL,
            DISTANCE_RECORD_DISTANCE_TOTAL,
            ELEVATION_RECORD_ELEVATION_GAINED_TOTAL,
            HEART_RATE_RECORD_BPM_AVG,
            POWER_RECORD_POWER_MIN,
            POWER_RECORD_POWER_MAX,
            POWER_RECORD_POWER_AVG,
            HYDRATION_RECORD_VOLUME_TOTAL,
            FLOORS_CLIMBED_RECORD_FLOORS_CLIMBED_TOTAL,
            NUTRITION_RECORD_BIOTIN_TOTAL,
            NUTRITION_RECORD_CAFFEINE_TOTAL,
            NUTRITION_RECORD_CALCIUM_TOTAL,
            NUTRITION_RECORD_CHLORIDE_TOTAL,
            NUTRITION_RECORD_CHOLESTEROL_TOTAL,
            NUTRITION_RECORD_CHROMIUM_TOTAL,
            NUTRITION_RECORD_COPPER_TOTAL,
            NUTRITION_RECORD_DIETARY_FIBER_TOTAL,
            NUTRITION_RECORD_ENERGY_TOTAL,
            NUTRITION_RECORD_ENERGY_FROM_FAT_TOTAL,
            NUTRITION_RECORD_FOLATE_TOTAL,
            NUTRITION_RECORD_FOLIC_ACID_TOTAL,
            NUTRITION_RECORD_IODINE_TOTAL,
            NUTRITION_RECORD_IRON_TOTAL,
            NUTRITION_RECORD_MAGNESIUM_TOTAL,
            NUTRITION_RECORD_MANGANESE_TOTAL,
            NUTRITION_RECORD_MOLYBDENUM_TOTAL,
            NUTRITION_RECORD_MONOUNSATURATED_FAT_TOTAL,
            NUTRITION_RECORD_NIACIN_TOTAL,
            NUTRITION_RECORD_PANTOTHENIC_ACID_TOTAL,
            NUTRITION_RECORD_PHOSPHORUS_TOTAL,
            NUTRITION_RECORD_POLYUNSATURATED_FAT_TOTAL,
            NUTRITION_RECORD_POTASSIUM_TOTAL,
            NUTRITION_RECORD_PROTEIN_TOTAL,
            NUTRITION_RECORD_RIBOFLAVIN_TOTAL,
            NUTRITION_RECORD_SATURATED_FAT_TOTAL,
            NUTRITION_RECORD_SELENIUM_TOTAL,
            NUTRITION_RECORD_SODIUM_TOTAL,
            NUTRITION_RECORD_SUGAR_TOTAL,
            NUTRITION_RECORD_THIAMIN_TOTAL,
            NUTRITION_RECORD_TOTAL_CARBOHYDRATE_TOTAL,
            NUTRITION_RECORD_TOTAL_FAT_TOTAL,
            NUTRITION_RECORD_UNSATURATED_FAT_TOTAL,
            NUTRITION_RECORD_VITAMIN_A_TOTAL,
            NUTRITION_RECORD_VITAMIN_B12_TOTAL,
            NUTRITION_RECORD_VITAMIN_B6_TOTAL,
            NUTRITION_RECORD_VITAMIN_C_TOTAL,
            NUTRITION_RECORD_VITAMIN_D_TOTAL,
            NUTRITION_RECORD_VITAMIN_E_TOTAL,
            NUTRITION_RECORD_VITAMIN_K_TOTAL,
            NUTRITION_RECORD_ZINC_TOTAL,
            HEIGHT_RECORD_HEIGHT_AVG,
            HEIGHT_RECORD_HEIGHT_MAX,
            HEIGHT_RECORD_HEIGHT_MIN,
            RESTING_HEART_RATE_RECORD_BPM_MAX,
            RESTING_HEART_RATE_RECORD_BPM_MIN,
            TOTAL_CALORIES_BURNED_RECORD_ENERGY_TOTAL,
            WEIGHT_RECORD_WEIGHT_AVG,
            WEIGHT_RECORD_WEIGHT_MAX,
            WEIGHT_RECORD_WEIGHT_MIN,
            WHEEL_CHAIR_PUSHES_RECORD_COUNT_TOTAL,
            HEART_RATE_RECORD_MEASUREMENTS_COUNT,
            RESTING_HEART_RATE_RECORD_BPM_AVG,
            SLEEP_SESSION_DURATION_TOTAL,
            EXERCISE_SESSION_DURATION_TOTAL,
            NUTRITION_RECORD_TRANS_FAT_TOTAL,
            CYCLING_PEDALING_CADENCE_RECORD_RPM_AVG,
            CYCLING_PEDALING_CADENCE_RECORD_RPM_MIN,
            CYCLING_PEDALING_CADENCE_RECORD_RPM_MAX,
            BLOOD_PRESSURE_RECORD_DIASTOLIC_AVG,
            BLOOD_PRESSURE_RECORD_DIASTOLIC_MAX,
            BLOOD_PRESSURE_RECORD_DIASTOLIC_MIN,
            BLOOD_PRESSURE_RECORD_SYSTOLIC_AVG,
            BLOOD_PRESSURE_RECORD_SYSTOLIC_MAX,
            BLOOD_PRESSURE_RECORD_SYSTOLIC_MIN,
            SPEED_RECORD_SPEED_AVG,
            SPEED_RECORD_SPEED_MIN,
            SPEED_RECORD_SPEED_MAX,
            STEPS_CADENCE_RECORD_RATE_AVG,
            STEPS_CADENCE_RECORD_RATE_MIN,
            STEPS_CADENCE_RECORD_RATE_MAX,
            SKIN_TEMPERATURE_RECORD_DELTA_AVG,
            SKIN_TEMPERATURE_RECORD_DELTA_MIN,
            SKIN_TEMPERATURE_RECORD_DELTA_MAX,
            MINDFULNESS_SESSION_DURATION_TOTAL,
            ACTIVITY_INTENSITY_MODERATE_DURATION_TOTAL,
            ACTIVITY_INTENSITY_VIGOROUS_DURATION_TOTAL,
            ACTIVITY_INTENSITY_DURATION_TOTAL,
            ACTIVITY_INTENSITY_MINUTES_TOTAL
        })
        @Retention(RetentionPolicy.SOURCE)
        @interface Id {}
    }

    /**
     * Valid set of values for this IntDef. Update this set when add new type or deprecate existing
     * type.
     *
     * @hide
     */
    public static final Set<Integer> OPERATION_VALID_TYPES = Set.of(MAX, MIN, AVG, SUM, COUNT);

    /** @hide */
    @IntDef({MAX, MIN, AVG, SUM, COUNT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AggregateOperationType {}
}

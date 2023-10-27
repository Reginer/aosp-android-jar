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
import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
    private final List<Integer> mApplicableRecordTypes;
    private final Class<T> mClass;
    /** @hide */
    AggregationType(
            @AggregationTypeIdentifier.Id int id,
            @AggregateOperationType int type,
            @NonNull List<Integer> applicableRecordTypes,
            Class<T> templateClass) {
        Objects.requireNonNull(applicableRecordTypes);

        mId = id;
        mType = type;
        mApplicableRecordTypes = applicableRecordTypes;
        mClass = templateClass;
    }

    /** @hide */
    AggregationType(
            @AggregationTypeIdentifier.Id int id,
            @AggregateOperationType int type,
            @NonNull @RecordTypeIdentifier.RecordType int applicableRecordType,
            Class<T> templateClass) {
        validateIntDefValue(
                id,
                AggregationTypeIdentifier.IDENTIFIER_VALID_TYPES,
                AggregationTypeIdentifier.class.getSimpleName());
        validateIntDefValue(
                type, OPERATION_VALID_TYPES, AggregateOperationType.class.getSimpleName());

        mId = id;
        mType = type;
        mApplicableRecordTypes = Collections.singletonList(applicableRecordType);
        mClass = templateClass;
    }

    /** @hide */
    @AggregationTypeIdentifier.Id
    public int getAggregationTypeIdentifier() {
        return mId;
    }

    /** @hide */
    @NonNull
    public List<Integer> getApplicableRecordTypeIds() {
        return mApplicableRecordTypes;
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
                        EXERCISE_SESSION_DURATION_TOTAL);

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
            EXERCISE_SESSION_DURATION_TOTAL
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

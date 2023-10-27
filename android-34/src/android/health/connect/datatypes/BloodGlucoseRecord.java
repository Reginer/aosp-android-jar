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
import android.health.connect.datatypes.units.BloodGlucose;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.BloodGlucoseRecordInternal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

/**
 * Captures the concentration of glucose in the blood. Each record represents a single instantaneous
 * blood glucose reading.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE)
public final class BloodGlucoseRecord extends InstantRecord {
    private final int mSpecimenSource;
    private final BloodGlucose mLevel;
    private final int mRelationToMeal;
    private final int mMealType;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param specimenSource SpecimenSource of this activity
     * @param level Level of this activity
     * @param relationToMeal RelationToMeal of this activity
     * @param mealType MealType of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private BloodGlucoseRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @SpecimenSource.SpecimenSourceType int specimenSource,
            @NonNull BloodGlucose level,
            @RelationToMealType.RelationToMealTypes int relationToMeal,
            @MealType.MealTypes int mealType,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        Objects.requireNonNull(level);
        if (!skipValidation) {
            ValidationUtils.requireInRange(level.getInMillimolesPerLiter(), 0.0, 50.0, "level");
        }
        validateIntDefValue(
                specimenSource, SpecimenSource.VALID_TYPES, SpecimenSource.class.getSimpleName());
        validateIntDefValue(
                relationToMeal,
                RelationToMealType.VALID_TYPES,
                RelationToMealType.class.getSimpleName());
        validateIntDefValue(mealType, MealType.VALID_TYPES, MealType.class.getSimpleName());
        mSpecimenSource = specimenSource;
        mLevel = level;
        mRelationToMeal = relationToMeal;
        mMealType = mealType;
    }

    /**
     * @return specimenSource
     */
    @SpecimenSource.SpecimenSourceType
    public int getSpecimenSource() {
        return mSpecimenSource;
    }

    /**
     * @return level
     */
    @NonNull
    public BloodGlucose getLevel() {
        return mLevel;
    }

    /**
     * @return relationToMeal
     */
    @RelationToMealType.RelationToMealTypes
    public int getRelationToMeal() {
        return mRelationToMeal;
    }

    /**
     * @return mealType
     */
    @MealType.MealTypes
    public int getMealType() {
        return mMealType;
    }

    /** Relationship of the meal to the blood glucose measurement. */
    public static final class RelationToMealType {

        /** Represents a blood glucose which relationship to any meal is unknown / not defined. */
        public static final int RELATION_TO_MEAL_UNKNOWN = 0;

        /** Reading was not taken immediately before or after eating. */
        public static final int RELATION_TO_MEAL_GENERAL = 1;

        /** Reading was taken during a fasting period. */
        public static final int RELATION_TO_MEAL_FASTING = 2;

        /** Reading was taken before an unspecified meal. */
        public static final int RELATION_TO_MEAL_BEFORE_MEAL = 3;

        /** Reading was taken after an unspecified meal. */
        public static final int RELATION_TO_MEAL_AFTER_MEAL = 4;

        /**
         * Valid set of values for this IntDef. Update this set when add new type or deprecate
         * existing type.
         *
         * @hide
         */
        public static final Set<Integer> VALID_TYPES =
                Set.of(
                        RELATION_TO_MEAL_UNKNOWN,
                        RELATION_TO_MEAL_GENERAL,
                        RELATION_TO_MEAL_FASTING,
                        RELATION_TO_MEAL_BEFORE_MEAL,
                        RELATION_TO_MEAL_AFTER_MEAL);

        private RelationToMealType() {}

        /** @hide */
        @IntDef({
            RELATION_TO_MEAL_UNKNOWN,
            RELATION_TO_MEAL_GENERAL,
            RELATION_TO_MEAL_FASTING,
            RELATION_TO_MEAL_BEFORE_MEAL,
            RELATION_TO_MEAL_AFTER_MEAL
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface RelationToMealTypes {}
    }

    /** Type of body fluid used to measure the blood glucose. */
    public static final class SpecimenSource {
        /** Fluid used to measure glucose is not identified. */
        public static final int SPECIMEN_SOURCE_UNKNOWN = 0;
        /** Glucose was measured in interstitial fluid. */
        public static final int SPECIMEN_SOURCE_INTERSTITIAL_FLUID = 1;
        /** Glucose was measured in capillary blood. */
        public static final int SPECIMEN_SOURCE_CAPILLARY_BLOOD = 2;
        /** Glucose was measured in plasma. */
        public static final int SPECIMEN_SOURCE_PLASMA = 3;
        /** Glucose was measured in serum. */
        public static final int SPECIMEN_SOURCE_SERUM = 4;
        /** Glucose was measured in tears. */
        public static final int SPECIMEN_SOURCE_TEARS = 5;
        /** Glucose was measured from whole blood. */
        public static final int SPECIMEN_SOURCE_WHOLE_BLOOD = 6;

        /**
         * Valid set of values for this IntDef. Update this set when add new type or deprecate
         * existing type.
         *
         * @hide
         */
        public static final Set<Integer> VALID_TYPES =
                Set.of(
                        SPECIMEN_SOURCE_UNKNOWN,
                        SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
                        SPECIMEN_SOURCE_CAPILLARY_BLOOD,
                        SPECIMEN_SOURCE_PLASMA,
                        SPECIMEN_SOURCE_SERUM,
                        SPECIMEN_SOURCE_TEARS,
                        SPECIMEN_SOURCE_WHOLE_BLOOD);

        private SpecimenSource() {}

        /** @hide */
        @IntDef({
            SPECIMEN_SOURCE_UNKNOWN,
            SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
            SPECIMEN_SOURCE_CAPILLARY_BLOOD,
            SPECIMEN_SOURCE_PLASMA,
            SPECIMEN_SOURCE_SERUM,
            SPECIMEN_SOURCE_TEARS,
            SPECIMEN_SOURCE_WHOLE_BLOOD
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface SpecimenSourceType {}
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        BloodGlucoseRecord that = (BloodGlucoseRecord) o;
        return getSpecimenSource() == that.getSpecimenSource()
                && getRelationToMeal() == that.getRelationToMeal()
                && getMealType() == that.getMealType()
                && Objects.equals(getLevel(), that.getLevel());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                getSpecimenSource(),
                getLevel(),
                getRelationToMeal(),
                getMealType());
    }

    /** Builder class for {@link BloodGlucoseRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final int mSpecimenSource;
        private final BloodGlucose mLevel;
        private final int mRelationToMeal;
        private final int mMealType;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param specimenSource Type of body fluid used to measure the blood glucose. Optional,
         *     enum field. Allowed values: {@link SpecimenSource}.
         * @param level Blood glucose level or concentration in {@link BloodGlucose} unit. Required
         *     field. Valid range: 0-50 mmol/L.
         * @param relationToMeal Relationship of the meal to the blood glucose measurement.
         *     Optional, enum field. Allowed values: {@link RelationToMealType}.
         * @param mealType Type of meal related to the blood glucose measurement. Optional, enum
         *     field. Allowed values: {@link MealType}.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @SpecimenSource.SpecimenSourceType int specimenSource,
                @NonNull BloodGlucose level,
                @RelationToMealType.RelationToMealTypes int relationToMeal,
                @MealType.MealTypes int mealType) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            Objects.requireNonNull(level);
            mMetadata = metadata;
            mTime = time;
            mSpecimenSource = specimenSource;
            mLevel = level;
            mRelationToMeal = relationToMeal;
            mMealType = mealType;
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(time);
        }

        /** Sets the zone offset of the user when the activity happened */
        @NonNull
        public Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /** Sets the zone offset of this record to system default. */
        @NonNull
        public Builder clearZoneOffset() {
            mZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }
        /**
         * @return Object of {@link BloodGlucoseRecord} without validating the values.
         * @hide
         */
        @NonNull
        public BloodGlucoseRecord buildWithoutValidation() {
            return new BloodGlucoseRecord(
                    mMetadata,
                    mTime,
                    mZoneOffset,
                    mSpecimenSource,
                    mLevel,
                    mRelationToMeal,
                    mMealType,
                    true);
        }

        /**
         * @return Object of {@link BloodGlucoseRecord}
         */
        @NonNull
        public BloodGlucoseRecord build() {
            return new BloodGlucoseRecord(
                    mMetadata,
                    mTime,
                    mZoneOffset,
                    mSpecimenSource,
                    mLevel,
                    mRelationToMeal,
                    mMealType,
                    false);
        }
    }

    /** @hide */
    @Override
    public BloodGlucoseRecordInternal toRecordInternal() {
        BloodGlucoseRecordInternal recordInternal =
                (BloodGlucoseRecordInternal)
                        new BloodGlucoseRecordInternal()
                                .setUuid(getMetadata().getId())
                                .setPackageName(getMetadata().getDataOrigin().getPackageName())
                                .setLastModifiedTime(
                                        getMetadata().getLastModifiedTime().toEpochMilli())
                                .setClientRecordId(getMetadata().getClientRecordId())
                                .setClientRecordVersion(getMetadata().getClientRecordVersion())
                                .setManufacturer(getMetadata().getDevice().getManufacturer())
                                .setModel(getMetadata().getDevice().getModel())
                                .setDeviceType(getMetadata().getDevice().getType())
                                .setRecordingMethod(getMetadata().getRecordingMethod());
        recordInternal.setTime(getTime().toEpochMilli());
        recordInternal.setZoneOffset(getZoneOffset().getTotalSeconds());
        recordInternal.setSpecimenSource(mSpecimenSource);
        recordInternal.setLevel(mLevel.getInMillimolesPerLiter());
        recordInternal.setRelationToMeal(mRelationToMeal);
        recordInternal.setMealType(mMealType);
        return recordInternal;
    }
}

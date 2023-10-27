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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED;

import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.ActiveCaloriesBurnedRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the estimated active energy burned by the user (in kilocalories), excluding basal
 * metabolic rate (BMR).
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED)
public final class ActiveCaloriesBurnedRecord extends IntervalRecord {
    /** Builder class for {@link ActiveCaloriesBurnedRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final Energy mEnergy;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param energy Energy in {@link Energy} unit. Required field. Valid range: 0-1000000 kcal.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull Energy energy) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(energy);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mEnergy = energy;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(startTime);
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(endTime);
        }

        /** Sets the zone offset of the user when the activity started */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            Objects.requireNonNull(startZoneOffset);

            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /** Sets the zone offset of the user when the activity ended */
        @NonNull
        public Builder setEndZoneOffset(@NonNull ZoneOffset endZoneOffset) {
            Objects.requireNonNull(endZoneOffset);

            mEndZoneOffset = endZoneOffset;
            return this;
        }

        /** Sets the start zone offset of this record to system default. */
        @NonNull
        public Builder clearStartZoneOffset() {
            mStartZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /** Sets the start zone offset of this record to system default. */
        @NonNull
        public Builder clearEndZoneOffset() {
            mEndZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /**
         * @return Object of {@link ActiveCaloriesBurnedRecord} without validating the values.
         * @hide
         */
        @NonNull
        public ActiveCaloriesBurnedRecord buildWithoutValidation() {
            return new ActiveCaloriesBurnedRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mEnergy,
                    true);
        }

        /**
         * @return Object of {@link ActiveCaloriesBurnedRecord}
         */
        @NonNull
        public ActiveCaloriesBurnedRecord build() {
            return new ActiveCaloriesBurnedRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mEnergy,
                    false);
        }
    }

    /**
     * Metric identifier to get total active calories burnt using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Energy> ACTIVE_CALORIES_TOTAL =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier
                            .ACTIVE_CALORIES_BURNED_RECORD_ACTIVE_CALORIES_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_ACTIVE_CALORIES_BURNED,
                    Energy.class);

    private final Energy mEnergy;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param energy Energy of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private ActiveCaloriesBurnedRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull Energy energy,
            boolean skipValidation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset, skipValidation);
        Objects.requireNonNull(energy);
        if (!skipValidation) {
            ValidationUtils.requireInRange(energy.getInCalories(), 0.0, 1000000000.0, "energy");
        }
        mEnergy = energy;
    }

    /**
     * @return energy in {@link Energy} unit.
     */
    @NonNull
    public Energy getEnergy() {
        return mEnergy;
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
        ActiveCaloriesBurnedRecord that = (ActiveCaloriesBurnedRecord) o;
        return getEnergy().equals(that.getEnergy());
    }

    /**
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getEnergy());
    }

    /** @hide */
    @Override
    public ActiveCaloriesBurnedRecordInternal toRecordInternal() {
        ActiveCaloriesBurnedRecordInternal recordInternal =
                (ActiveCaloriesBurnedRecordInternal)
                        new ActiveCaloriesBurnedRecordInternal()
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
        recordInternal.setStartTime(getStartTime().toEpochMilli());
        recordInternal.setEndTime(getEndTime().toEpochMilli());
        recordInternal.setStartZoneOffset(getStartZoneOffset().getTotalSeconds());
        recordInternal.setEndZoneOffset(getEndZoneOffset().getTotalSeconds());
        recordInternal.setEnergy(mEnergy.getInCalories());
        return recordInternal;
    }
}

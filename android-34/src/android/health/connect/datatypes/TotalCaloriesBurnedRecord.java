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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED;

import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.TotalCaloriesBurnedRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Total energy burned by the user in {@link Energy}, including active & basal energy burned (BMR).
 * Each record represents the total kilocalories burned over a time interval.
 */
@Identifier(recordIdentifier = RECORD_TYPE_TOTAL_CALORIES_BURNED)
public final class TotalCaloriesBurnedRecord extends IntervalRecord {

    private final Energy mEnergy;

    /**
     * Metric identifier to get total energy from total calories burned using aggregate APIs in
     * {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Energy> ENERGY_TOTAL =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier
                            .TOTAL_CALORIES_BURNED_RECORD_ENERGY_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_TOTAL_CALORIES_BURNED,
                    Energy.class);

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param energy Energy burned during this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private TotalCaloriesBurnedRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull Energy energy,
            boolean skipValidation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(startZoneOffset);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endZoneOffset);
        Objects.requireNonNull(energy);
        if (!skipValidation) {
            ValidationUtils.requireInRange(energy.getInCalories(), 0.0, 1000000000.0, "energy");
        }
        mEnergy = energy;
    }

    /**
     * @return energy burned during this activity in {@link Energy} unit.
     */
    @NonNull
    public Energy getEnergy() {
        return mEnergy;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the object argument; {@code false}
     *     otherwise.
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (super.equals(object) && object instanceof TotalCaloriesBurnedRecord) {
            TotalCaloriesBurnedRecord other = (TotalCaloriesBurnedRecord) object;
            return this.getEnergy().equals(other.getEnergy());
        }
        return false;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.getEnergy());
    }

    /** Builder class for {@link TotalCaloriesBurnedRecord} */
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
         * @param energy Energy burned during this activity in {@link Energy} unit. Required field.
         *     Valid range: 0-1000000 kcal.
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
         * @return Object of {@link TotalCaloriesBurnedRecord} without validating the values.
         * @hide
         */
        @NonNull
        public TotalCaloriesBurnedRecord buildWithoutValidation() {
            return new TotalCaloriesBurnedRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mEnergy,
                    true);
        }

        /**
         * @return Object of {@link TotalCaloriesBurnedRecord}
         */
        @NonNull
        public TotalCaloriesBurnedRecord build() {
            return new TotalCaloriesBurnedRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mEnergy,
                    false);
        }
    }

    /** @hide */
    @Override
    public TotalCaloriesBurnedRecordInternal toRecordInternal() {
        TotalCaloriesBurnedRecordInternal recordInternal =
                (TotalCaloriesBurnedRecordInternal)
                        new TotalCaloriesBurnedRecordInternal()
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

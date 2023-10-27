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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_WEIGHT;

import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.WeightRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Captures the user's weight. */
@Identifier(recordIdentifier = RECORD_TYPE_WEIGHT)
public final class WeightRecord extends InstantRecord {
    private final Mass mWeight;

    /**
     * Metric identifier to get average weight using aggregate APIs in {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Mass> WEIGHT_AVG =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.WEIGHT_RECORD_WEIGHT_AVG,
                    AggregationType.AVG,
                    RECORD_TYPE_WEIGHT,
                    Mass.class);

    /**
     * Metric identifier to get maximum weight using aggregate APIs in {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Mass> WEIGHT_MAX =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.WEIGHT_RECORD_WEIGHT_MAX,
                    AggregationType.MAX,
                    RECORD_TYPE_WEIGHT,
                    Mass.class);

    /**
     * Metric identifier to get minimum weight using aggregate APIs in {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Mass> WEIGHT_MIN =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.WEIGHT_RECORD_WEIGHT_MIN,
                    AggregationType.MIN,
                    RECORD_TYPE_WEIGHT,
                    Mass.class);

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param weight Weight of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private WeightRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @NonNull Mass weight,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        Objects.requireNonNull(weight);
        if (!skipValidation) {
            ValidationUtils.requireInRange(weight.getInGrams(), 0.0, 1000000.0, "weight");
        }
        mWeight = weight;
    }
    /**
     * @return weight in {@link Mass} unit.
     */
    @NonNull
    public Mass getWeight() {
        return mWeight;
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
        WeightRecord that = (WeightRecord) o;
        return getWeight().equals(that.getWeight());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getWeight());
    }

    /** Builder class for {@link WeightRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final Mass mWeight;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param weight User's weight in {@link Mass} unit. Required field. Valid range: 0-1000000
         *     grams.
         */
        public Builder(@NonNull Metadata metadata, @NonNull Instant time, @NonNull Mass weight) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            Objects.requireNonNull(weight);
            mMetadata = metadata;
            mTime = time;
            mWeight = weight;
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
         * @return Object of {@link WeightRecord} without validating the values.
         * @hide
         */
        @NonNull
        public WeightRecord buildWithoutValidation() {
            return new WeightRecord(mMetadata, mTime, mZoneOffset, mWeight, true);
        }

        /**
         * @return Object of {@link WeightRecord}
         */
        @NonNull
        public WeightRecord build() {
            return new WeightRecord(mMetadata, mTime, mZoneOffset, mWeight, false);
        }
    }

    /** @hide */
    @Override
    public WeightRecordInternal toRecordInternal() {
        WeightRecordInternal recordInternal =
                (WeightRecordInternal)
                        new WeightRecordInternal()
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
        recordInternal.setWeight(mWeight.getInGrams());
        return recordInternal;
    }
}

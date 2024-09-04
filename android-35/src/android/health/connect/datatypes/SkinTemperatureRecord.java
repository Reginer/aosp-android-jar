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

package android.health.connect.datatypes;

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE;
import static android.health.connect.datatypes.validation.ValidationUtils.validateIntDefValue;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.units.Temperature;
import android.health.connect.datatypes.units.TemperatureDelta;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.SkinTemperatureRecordInternal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Captures the skin temperature of a user. Each record can represent a series of measurements of
 * temperature differences.
 */
@Identifier(recordIdentifier = RECORD_TYPE_SKIN_TEMPERATURE)
@FlaggedApi("com.android.healthconnect.flags.skin_temperature")
public final class SkinTemperatureRecord extends IntervalRecord {
    /** Skin temperature measurement location unknown. */
    public static final int MEASUREMENT_LOCATION_UNKNOWN = 0;
    /** Skin temperature measurement location finger. */
    public static final int MEASUREMENT_LOCATION_FINGER = 1;
    /** Skin temperature measurement location toe. */
    public static final int MEASUREMENT_LOCATION_TOE = 2;
    /** Skin temperature measurement location wrist. */
    public static final int MEASUREMENT_LOCATION_WRIST = 3;
    /**
     * Metric identifier to retrieve average skin temperature delta using aggregate APIs in {@link
     * HealthConnectManager}.
     */
    @NonNull
    public static final AggregationType<TemperatureDelta> SKIN_TEMPERATURE_DELTA_AVG =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.SKIN_TEMPERATURE_RECORD_DELTA_AVG,
                    AggregationType.AVG,
                    RECORD_TYPE_SKIN_TEMPERATURE,
                    TemperatureDelta.class);
    /**
     * Metric identifier to retrieve minimum skin temperature delta using aggregate APIs in {@link
     * HealthConnectManager}.
     */
    @NonNull
    public static final AggregationType<TemperatureDelta> SKIN_TEMPERATURE_DELTA_MIN =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.SKIN_TEMPERATURE_RECORD_DELTA_MIN,
                    AggregationType.MIN,
                    RECORD_TYPE_SKIN_TEMPERATURE,
                    TemperatureDelta.class);
    /**
     * Metric identifier to retrieve maximum skin temperature delta using aggregate APIs in {@link
     * HealthConnectManager}.
     */
    @NonNull
    public static final AggregationType<TemperatureDelta> SKIN_TEMPERATURE_DELTA_MAX =
            new AggregationType<>(
                    AggregationType.AggregationTypeIdentifier.SKIN_TEMPERATURE_RECORD_DELTA_MAX,
                    AggregationType.MAX,
                    RECORD_TYPE_SKIN_TEMPERATURE,
                    TemperatureDelta.class);

    @Nullable private final Temperature mBaseline;
    @NonNull private final List<Delta> mDeltas;
    private @SkinTemperatureMeasurementLocation int mMeasurementLocation;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity.
     * @param startZoneOffset Zone offset of the user when the activity started.
     * @param endTime End time of this activity.
     * @param endZoneOffset Zone offset of the user when the activity finished.
     * @param baseline Baseline temperature of a skin temperature.
     * @param deltas List of skin temperature deltas.
     * @param measurementLocation Measurement location of the skin temperature.
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private SkinTemperatureRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @Nullable Temperature baseline,
            @NonNull List<Delta> deltas,
            @SkinTemperatureMeasurementLocation int measurementLocation,
            boolean skipValidation) {
        super(
                metadata,
                startTime,
                startZoneOffset,
                endTime,
                endZoneOffset,
                skipValidation,
                /* enforceFutureTimeRestrictions= */ true);
        if (!skipValidation) {
            if (baseline != null) {
                ValidationUtils.requireInRange(baseline.getInCelsius(), 0.0, 100, "temperature");
            }
            validateIntDefValue(
                    measurementLocation,
                    VALID_MEASUREMENT_LOCATIONS,
                    SkinTemperatureMeasurementLocation.class.getSimpleName());
            ValidationUtils.validateSampleStartAndEndTime(
                    startTime, endTime, deltas.stream().map(Delta::getTime).toList());
        }
        mBaseline = baseline;
        mDeltas =
                Collections.unmodifiableList(
                        deltas.stream()
                                .sorted(Comparator.comparing(Delta::getTime))
                                .collect(toList()));
        mMeasurementLocation = measurementLocation;
    }

    /**
     * @return baseline skin temperature in {@link Temperature} unit.
     */
    @Nullable
    public Temperature getBaseline() {
        return mBaseline;
    }

    /**
     * @return an unmodified list of skin temperature deltas in {@link TemperatureDelta} unit.
     */
    @NonNull
    public List<Delta> getDeltas() {
        return mDeltas;
    }

    /**
     * @return measurementLocation
     */
    @SkinTemperatureMeasurementLocation
    public int getMeasurementLocation() {
        return mMeasurementLocation;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the object.
     */
    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof SkinTemperatureRecord)) return false;
        if (!super.equals(object)) return false;
        SkinTemperatureRecord that = (SkinTemperatureRecord) object;
        if (!Objects.equals(getBaseline(), that.getBaseline())
                || getMeasurementLocation() != that.getMeasurementLocation()) return false;
        return Objects.equals(getDeltas(), that.getDeltas());
    }

    /**
     * @return a hash code value for the object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getBaseline(), getDeltas());
    }

    /** @hide */
    @Override
    public SkinTemperatureRecordInternal toRecordInternal() {
        SkinTemperatureRecordInternal recordInternal =
                (SkinTemperatureRecordInternal)
                        new SkinTemperatureRecordInternal()
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
        if (getBaseline() != null) {
            recordInternal.setBaseline(getBaseline());
        }

        recordInternal.setSamples(
                getDeltas().stream()
                        .map(
                                delta ->
                                        new SkinTemperatureRecordInternal
                                                .SkinTemperatureDeltaSample(
                                                delta.getDelta().getInCelsius(),
                                                delta.getTime().toEpochMilli()))
                        .collect(Collectors.toSet()));
        recordInternal.setMeasurementLocation(getMeasurementLocation());
        recordInternal.setStartTime(getStartTime().toEpochMilli());
        recordInternal.setEndTime(getEndTime().toEpochMilli());
        recordInternal.setStartZoneOffset(getStartZoneOffset().getTotalSeconds());
        recordInternal.setEndZoneOffset(getEndZoneOffset().getTotalSeconds());

        return recordInternal;
    }

    /** Skin temperature delta entry of {@link SkinTemperatureRecord}. */
    public static final class Delta {
        private final TemperatureDelta mDelta;
        private final Instant mTime;

        /**
         * Skin temperature delta entry of {@link SkinTemperatureRecord}.
         *
         * @param delta Temperature difference.
         * @param time The point in time when the measurement was taken.
         */
        public Delta(@NonNull TemperatureDelta delta, @NonNull Instant time) {
            this(delta, time, false);
        }

        /**
         * Skin temperature delta entry of {@link SkinTemperatureRecord}.
         *
         * @param delta Temperature difference.
         * @param time The point in time when the measurement was taken.
         * @param skipValidation Boolean flag to skip validation of record values.
         * @hide
         */
        public Delta(
                @NonNull TemperatureDelta delta, @NonNull Instant time, boolean skipValidation) {
            requireNonNull(delta);
            requireNonNull(time);
            if (!skipValidation) {
                ValidationUtils.requireInRange(delta.getInCelsius(), -30, 30, "temperature delta");
            }
            mDelta = delta;
            mTime = time;
        }

        /**
         * @return delta of the skin temperature.
         */
        @NonNull
        public TemperatureDelta getDelta() {
            return mDelta;
        }

        /**
         * @return time at which this measurement was recorded.
         */
        @NonNull
        public Instant getTime() {
            return mTime;
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         *
         * @param object the reference object with which to compare.
         * @return {@code true} if this object is the same as the object.
         */
        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) return true;
            if (!(object instanceof Delta)) return false;
            Delta that = (Delta) object;
            return Objects.equals(getDelta(), that.getDelta())
                    && getTime().toEpochMilli() == that.getTime().toEpochMilli();
        }

        /**
         * @return a hash code value for the object.
         */
        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), getDelta(), getTime());
        }
    }

    /**
     * Valid set of values for this IntDef. Update this set when add new type or deprecate existing
     * type.
     *
     * @hide
     */
    public static final Set<Integer> VALID_MEASUREMENT_LOCATIONS =
            Set.of(
                    MEASUREMENT_LOCATION_UNKNOWN,
                    MEASUREMENT_LOCATION_FINGER,
                    MEASUREMENT_LOCATION_TOE,
                    MEASUREMENT_LOCATION_WRIST);

    /** @hide */
    @IntDef({
        MEASUREMENT_LOCATION_UNKNOWN,
        MEASUREMENT_LOCATION_FINGER,
        MEASUREMENT_LOCATION_TOE,
        MEASUREMENT_LOCATION_WRIST,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SkinTemperatureMeasurementLocation {}

    /** Builder class for {@link SkinTemperatureRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        @Nullable private Temperature mBaseline;
        private final List<Delta> mDeltas;
        private int mMeasurementLocation;

        /**
         * @param metadata Metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity.
         * @param endTime End time of this activity.
         */
        public Builder(
                @NonNull Metadata metadata, @NonNull Instant startTime, @NonNull Instant endTime) {
            requireNonNull(metadata);
            requireNonNull(startTime);
            requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mDeltas = new ArrayList<>();
            mMeasurementLocation = 0;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(startTime);
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(endTime);
        }

        /** Sets the zone offset of the user when the activity started. */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            requireNonNull(startZoneOffset);
            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /** Sets the zone offset of the user when the activity ended. */
        @NonNull
        public Builder setEndZoneOffset(@NonNull ZoneOffset endZoneOffset) {
            requireNonNull(endZoneOffset);
            mEndZoneOffset = endZoneOffset;
            return this;
        }

        /** Clears the zone offset of the user when the activity started. */
        @NonNull
        public Builder clearStartZoneOffset() {
            mStartZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /** Clears the zone offset of the user when the activity ended. */
        @NonNull
        public Builder clearEndZoneOffset() {
            mEndZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /** Sets the baseline skin temperature for the user. */
        @NonNull
        public Builder setBaseline(@Nullable Temperature baseline) {
            mBaseline = baseline;
            return this;
        }

        /** Sets a list of skin temperature deltas. */
        @NonNull
        public Builder setDeltas(@NonNull List<Delta> deltas) {
            requireNonNull(deltas);
            mDeltas.clear();
            mDeltas.addAll(deltas);
            return this;
        }

        /** Sets the measurement location of the skin temperature. */
        @NonNull
        public Builder setMeasurementLocation(
                @SkinTemperatureMeasurementLocation int measurementLocation) {
            mMeasurementLocation = measurementLocation;
            return this;
        }

        /**
         * @return Object of {@link SkinTemperatureRecord} without validating the values.
         * @hide
         */
        @NonNull
        public SkinTemperatureRecord buildWithoutValidation() {
            return new SkinTemperatureRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mBaseline,
                    mDeltas,
                    mMeasurementLocation,
                    true);
        }

        /**
         * @return Object of {@link SkinTemperatureRecord}.
         */
        @NonNull
        public SkinTemperatureRecord build() {
            return new SkinTemperatureRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mBaseline,
                    mDeltas,
                    mMeasurementLocation,
                    false);
        }
    }
}

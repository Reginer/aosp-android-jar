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

import android.annotation.NonNull;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.BoneMassRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Captures the user's bone mass. Each record represents a single instantaneous measurement. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BONE_MASS)
public final class BoneMassRecord extends InstantRecord {

    private final Mass mMass;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param mass Mass of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private BoneMassRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @NonNull Mass mass,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        Objects.requireNonNull(mass);
        if (!skipValidation) {
            ValidationUtils.requireInRange(mass.getInGrams(), 0.0, 1000000.0, "mass");
        }
        mMass = mass;
    }
    /**
     * @return mass in {@link Mass} unit.
     */
    @NonNull
    public Mass getMass() {
        return mMass;
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
        BoneMassRecord that = (BoneMassRecord) o;
        return getMass().equals(that.getMass());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getMass());
    }

    /** Builder class for {@link BoneMassRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final Mass mMass;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param mass Mass in {@link Mass} unit. Required field. Valid range: 0-1000000 grams.
         */
        public Builder(@NonNull Metadata metadata, @NonNull Instant time, @NonNull Mass mass) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            Objects.requireNonNull(mass);
            mMetadata = metadata;
            mTime = time;
            mMass = mass;
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
         * @return Object of {@link BoneMassRecord} without validating the values.
         * @hide
         */
        @NonNull
        public BoneMassRecord buildWithoutValidation() {
            return new BoneMassRecord(mMetadata, mTime, mZoneOffset, mMass, true);
        }

        /**
         * @return Object of {@link BoneMassRecord}
         */
        @NonNull
        public BoneMassRecord build() {
            return new BoneMassRecord(mMetadata, mTime, mZoneOffset, mMass, false);
        }
    }

    /** @hide */
    @Override
    public BoneMassRecordInternal toRecordInternal() {
        BoneMassRecordInternal recordInternal =
                (BoneMassRecordInternal)
                        new BoneMassRecordInternal()
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
        recordInternal.setMass(mMass.getInGrams());
        return recordInternal;
    }
}

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
import android.health.connect.internal.datatypes.CervicalMucusRecordInternal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

/**
 * Captures the description of cervical mucus. Each record represents a self-assessed description of
 * cervical mucus for a user. All fields are optional and can be used to describe the look and feel
 * of cervical mucus.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_CERVICAL_MUCUS)
public final class CervicalMucusRecord extends InstantRecord {

    private final int mSensation;
    private final int mAppearance;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param sensation Sensation of this activity
     * @param appearance Appearance of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private CervicalMucusRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @CervicalMucusSensation.CervicalMucusSensations int sensation,
            @CervicalMucusAppearance.CervicalMucusAppearances int appearance,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        validateIntDefValue(
                sensation,
                CervicalMucusSensation.VALID_TYPES,
                CervicalMucusSensation.class.getSimpleName());
        validateIntDefValue(
                appearance,
                CervicalMucusAppearance.VALID_TYPES,
                CervicalMucusAppearance.class.getSimpleName());
        mSensation = sensation;
        mAppearance = appearance;
    }

    /**
     * @return sensation
     */
    @CervicalMucusSensation.CervicalMucusSensations
    public int getSensation() {
        return mSensation;
    }

    /**
     * @return appearance
     */
    @CervicalMucusAppearance.CervicalMucusAppearances
    public int getAppearance() {
        return mAppearance;
    }

    /** Identifier for Cervical Mucus Appearance */
    public static final class CervicalMucusAppearance {
        /** A constant describing cervical mucus which appearance is unknown. */
        public static final int APPEARANCE_UNKNOWN = 0;

        /** A constant describing a dry cervical mucus. */
        public static final int APPEARANCE_DRY = 1;

        /** A constant describing a sticky cervical mucus. */
        public static final int APPEARANCE_STICKY = 2;

        /** A constant describing creamy like looking cervical mucus. */
        public static final int APPEARANCE_CREAMY = 3;

        /** A constant describing watery like looking cervical mucus. */
        public static final int APPEARANCE_WATERY = 4;

        /** A constant describing clear or egg white like looking cervical mucus. */
        public static final int APPEARANCE_EGG_WHITE = 5;

        /** A constant describing an unusual (worth attention) kind of cervical mucus. */
        public static final int APPEARANCE_UNUSUAL = 6;

        /**
         * Valid set of values for this IntDef. Update this set when add new type or deprecate
         * existing type.
         *
         * @hide
         */
        public static final Set<Integer> VALID_TYPES =
                Set.of(
                        APPEARANCE_UNKNOWN,
                        APPEARANCE_DRY,
                        APPEARANCE_STICKY,
                        APPEARANCE_CREAMY,
                        APPEARANCE_WATERY,
                        APPEARANCE_EGG_WHITE,
                        APPEARANCE_UNUSUAL);

        CervicalMucusAppearance() {}

        /** @hide */
        @IntDef({
            APPEARANCE_UNKNOWN,
            APPEARANCE_DRY,
            APPEARANCE_STICKY,
            APPEARANCE_CREAMY,
            APPEARANCE_WATERY,
            APPEARANCE_EGG_WHITE,
            APPEARANCE_UNUSUAL
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface CervicalMucusAppearances {}
    }

    /** Identifier for Cervical Mucus Sensation */
    public static final class CervicalMucusSensation {
        public static final int SENSATION_UNKNOWN = 0;
        public static final int SENSATION_LIGHT = 1;
        public static final int SENSATION_MEDIUM = 2;
        public static final int SENSATION_HEAVY = 3;

        /**
         * Valid set of values for this IntDef. Update this set when add new type or deprecate
         * existing type.
         *
         * @hide
         */
        public static final Set<Integer> VALID_TYPES =
                Set.of(SENSATION_UNKNOWN, SENSATION_LIGHT, SENSATION_MEDIUM, SENSATION_HEAVY);

        CervicalMucusSensation() {}

        /** @hide */
        @IntDef({SENSATION_UNKNOWN, SENSATION_LIGHT, SENSATION_MEDIUM, SENSATION_HEAVY})
        @Retention(RetentionPolicy.SOURCE)
        public @interface CervicalMucusSensations {}
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
        CervicalMucusRecord that = (CervicalMucusRecord) o;
        return getSensation() == that.getSensation() && getAppearance() == that.getAppearance();
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getSensation(), getAppearance());
    }

    /** Builder class for {@link CervicalMucusRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final int mSensation;
        private final int mAppearance;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param sensation The feel of the user's cervical mucus. Optional field. Allowed values:
         *     {@link CervicalMucusSensation}.
         * @param appearance The consistency of the user's cervical mucus. Optional field. Allowed
         *     values: {@link CervicalMucusAppearance}.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @CervicalMucusSensation.CervicalMucusSensations int sensation,
                @CervicalMucusAppearance.CervicalMucusAppearances int appearance) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            mMetadata = metadata;
            mTime = time;
            mSensation = sensation;
            mAppearance = appearance;
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
         * @return Object of {@link CervicalMucusRecord} without validating the values.
         * @hide
         */
        @NonNull
        public CervicalMucusRecord buildWithoutValidation() {
            return new CervicalMucusRecord(
                    mMetadata, mTime, mZoneOffset, mSensation, mAppearance, true);
        }

        /**
         * @return Object of {@link CervicalMucusRecord}
         */
        @NonNull
        public CervicalMucusRecord build() {
            return new CervicalMucusRecord(
                    mMetadata, mTime, mZoneOffset, mSensation, mAppearance, false);
        }
    }

    /** @hide */
    @Override
    public CervicalMucusRecordInternal toRecordInternal() {
        CervicalMucusRecordInternal recordInternal =
                (CervicalMucusRecordInternal)
                        new CervicalMucusRecordInternal()
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
        recordInternal.setSensation(mSensation);
        recordInternal.setAppearance(mAppearance);
        return recordInternal;
    }
}

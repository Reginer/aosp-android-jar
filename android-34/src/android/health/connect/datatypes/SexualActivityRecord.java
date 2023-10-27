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
import android.health.connect.internal.datatypes.SexualActivityRecordInternal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

/**
 * Captures an occurrence of sexual activity. Each record is a single occurrence. ProtectionUsed
 * field is optional.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY)
public final class SexualActivityRecord extends InstantRecord {

    private final int mProtectionUsed;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param protectionUsed ProtectionUsed of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private SexualActivityRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @SexualActivityProtectionUsed.SexualActivityProtectionUsedTypes int protectionUsed,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        validateIntDefValue(
                protectionUsed,
                SexualActivityProtectionUsed.VALID_TYPES,
                SexualActivityProtectionUsed.class.getSimpleName());
        mProtectionUsed = protectionUsed;
    }

    /**
     * @return protectionUsed
     */
    @SexualActivityProtectionUsed.SexualActivityProtectionUsedTypes
    public int getProtectionUsed() {
        return mProtectionUsed;
    }

    /** Identifier for sexual activity protection used */
    public static final class SexualActivityProtectionUsed {
        public static final int PROTECTION_USED_UNKNOWN = 0;
        public static final int PROTECTION_USED_PROTECTED = 1;
        public static final int PROTECTION_USED_UNPROTECTED = 2;

        /**
         * Valid set of values for this IntDef. Update this set when add new type or deprecate
         * existing type.
         *
         * @hide
         */
        public static final Set<Integer> VALID_TYPES =
                Set.of(
                        PROTECTION_USED_UNKNOWN,
                        PROTECTION_USED_PROTECTED,
                        PROTECTION_USED_UNPROTECTED);

        SexualActivityProtectionUsed() {}

        /** @hide */
        @IntDef({PROTECTION_USED_UNKNOWN, PROTECTION_USED_PROTECTED, PROTECTION_USED_UNPROTECTED})
        @Retention(RetentionPolicy.SOURCE)
        public @interface SexualActivityProtectionUsedTypes {}
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
        SexualActivityRecord that = (SexualActivityRecord) o;
        return getProtectionUsed() == that.getProtectionUsed();
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getProtectionUsed());
    }

    /** Builder class for {@link SexualActivityRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final int mProtectionUsed;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param protectionUsed Whether protection was used during sexual activity. Optional field,
         *     null if unknown. Allowed values: Protection.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @SexualActivityProtectionUsed.SexualActivityProtectionUsedTypes
                        int protectionUsed) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            mMetadata = metadata;
            mTime = time;
            mProtectionUsed = protectionUsed;
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
         * @return Object of {@link SexualActivityRecord} without validating the values.
         * @hide
         */
        @NonNull
        public SexualActivityRecord buildWithoutValidation() {
            return new SexualActivityRecord(mMetadata, mTime, mZoneOffset, mProtectionUsed, true);
        }

        /**
         * @return Object of {@link SexualActivityRecord}
         */
        @NonNull
        public SexualActivityRecord build() {
            return new SexualActivityRecord(mMetadata, mTime, mZoneOffset, mProtectionUsed, false);
        }
    }

    /** @hide */
    @Override
    public SexualActivityRecordInternal toRecordInternal() {
        SexualActivityRecordInternal recordInternal =
                (SexualActivityRecordInternal)
                        new SexualActivityRecordInternal()
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
        recordInternal.setProtectionUsed(mProtectionUsed);
        return recordInternal;
    }
}

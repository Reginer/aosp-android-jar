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
import android.health.connect.internal.datatypes.IntermenstrualBleedingRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Captures the user's inter-menstrual bleeding. Each record represents a single instance. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_INTERMENSTRUAL_BLEEDING)
public final class IntermenstrualBleedingRecord extends InstantRecord {
    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time time for this record
     * @param zoneOffset Zone offset of the record
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private IntermenstrualBleedingRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the object
     */
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /** Builder class for {@link IntermenstrualBleedingRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time time for this record
         */
        public Builder(@NonNull Metadata metadata, @NonNull Instant time) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            mMetadata = metadata;
            mTime = time;
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(time);
        }

        /** Sets the zone offset for the record. */
        @NonNull
        public IntermenstrualBleedingRecord.Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /** Clears zone offset. */
        @NonNull
        public IntermenstrualBleedingRecord.Builder clearZoneOffset() {
            mZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /**
         * @return Object of {@link IntermenstrualBleedingRecord} without validating the values.
         * @hide
         */
        @NonNull
        public IntermenstrualBleedingRecord buildWithoutValidation() {
            return new IntermenstrualBleedingRecord(mMetadata, mTime, mZoneOffset, true);
        }

        /** Builds {@link IntermenstrualBleedingRecord} */
        @NonNull
        public IntermenstrualBleedingRecord build() {
            return new IntermenstrualBleedingRecord(mMetadata, mTime, mZoneOffset, false);
        }
    }

    /** @hide */
    @Override
    public IntermenstrualBleedingRecordInternal toRecordInternal() {
        IntermenstrualBleedingRecordInternal recordInternal =
                (IntermenstrualBleedingRecordInternal)
                        new IntermenstrualBleedingRecordInternal()
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
        return recordInternal;
    }
}

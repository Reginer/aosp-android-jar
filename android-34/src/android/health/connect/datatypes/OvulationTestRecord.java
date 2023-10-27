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
import android.health.connect.internal.datatypes.OvulationTestRecordInternal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

/** Each record represents the result of an ovulation test. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST)
public final class OvulationTestRecord extends InstantRecord {

    private final int mResult;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param result Result of this activity
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    private OvulationTestRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @OvulationTestResult.OvulationTestResults int result,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        validateIntDefValue(
                result, OvulationTestResult.VALID_TYPES, OvulationTestResult.class.getSimpleName());
        mResult = result;
    }

    /**
     * @return test result
     */
    @OvulationTestResult.OvulationTestResults
    public int getResult() {
        return mResult;
    }

    /** Identifier for Ovulation Test Result */
    public static final class OvulationTestResult {
        /**
         * Inconclusive result. Refers to ovulation test results that are indeterminate (e.g. may be
         * testing malfunction, user error, etc.). ". Any unknown value will also be returned as
         * [RESULT_INCONCLUSIVE].
         */
        public static final int RESULT_INCONCLUSIVE = 0;

        /**
         * Positive fertility (may also be referred as "peak" fertility). Refers to the peak of the
         * luteinizing hormone (LH) surge and ovulation is expected to occur in 10-36 hours.
         */
        public static final int RESULT_POSITIVE = 1;

        /**
         * High fertility. Refers to a rise in estrogen or luteinizing hormone that may signal the
         * fertile window (time in the menstrual cycle when conception is likely to occur).
         */
        public static final int RESULT_HIGH = 2;

        /**
         * Negative fertility (may also be referred as "low" fertility). Refers to the time in the
         * cycle where fertility/conception is expected to be low.
         */
        public static final int RESULT_NEGATIVE = 3;

        /**
         * Valid set of values for this IntDef. Update this set when add new type or deprecate
         * existing type.
         *
         * @hide
         */
        public static final Set<Integer> VALID_TYPES =
                Set.of(RESULT_INCONCLUSIVE, RESULT_POSITIVE, RESULT_HIGH, RESULT_NEGATIVE);

        OvulationTestResult() {}

        /** @hide */
        @IntDef({RESULT_INCONCLUSIVE, RESULT_POSITIVE, RESULT_HIGH, RESULT_NEGATIVE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface OvulationTestResults {}
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
        OvulationTestRecord that = (OvulationTestRecord) o;
        return getResult() == that.getResult();
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getResult());
    }

    /** Builder class for {@link OvulationTestRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final int mResult;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param result The result of a user's ovulation test, which shows if they're ovulating or
         *     not. Required field. Allowed values: {@link OvulationTestResult}.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @OvulationTestResult.OvulationTestResults int result) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            mMetadata = metadata;
            mTime = time;
            mResult = result;
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
         * @return Object of {@link OvulationTestRecord} without validating the values.
         * @hide
         */
        @NonNull
        public OvulationTestRecord buildWithoutValidation() {
            return new OvulationTestRecord(mMetadata, mTime, mZoneOffset, mResult, true);
        }

        /**
         * @return Object of {@link OvulationTestRecord}
         */
        @NonNull
        public OvulationTestRecord build() {
            return new OvulationTestRecord(mMetadata, mTime, mZoneOffset, mResult, false);
        }
    }

    /** @hide */
    @Override
    public OvulationTestRecordInternal toRecordInternal() {
        OvulationTestRecordInternal recordInternal =
                (OvulationTestRecordInternal)
                        new OvulationTestRecordInternal()
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
        recordInternal.setResult(mResult);
        return recordInternal;
    }
}

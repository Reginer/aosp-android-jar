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

import static android.health.connect.datatypes.validation.ValidationUtils.validateIntDefValue;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * Captures the user's medical data. This is the class used for all medical resource types, and the
 * type is specified via {@link MedicalResourceType}.
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class MedicalResource {
    /** Unknown medical resource type. */
    public static final int MEDICAL_RESOURCE_TYPE_UNKNOWN = 0;

    /** @hide */
    @IntDef({MEDICAL_RESOURCE_TYPE_UNKNOWN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MedicalResourceType {}

    @NonNull private final String mId;
    @MedicalResourceType private final int mType;
    @NonNull private final String mDataSourceId;
    @NonNull private final String mData;

    /**
     * @param id The unique identifier of this data, assigned by the Android Health Platform at
     *     insertion time.
     * @param type The medical resource type assigned by the Android Health Platform at insertion
     *     time.
     * @param dataSourceId Where the data comes from.
     * @param data The FHIR resource data in JSON representation.
     */
    private MedicalResource(
            @NonNull String id,
            @MedicalResourceType int type,
            @NonNull String dataSourceId,
            @NonNull String data) {
        requireNonNull(id);
        requireNonNull(dataSourceId);
        requireNonNull(data);
        validateIntDefValue(type, VALID_TYPES, MedicalResourceType.class.getSimpleName());

        mId = id;
        mType = type;
        mDataSourceId = dataSourceId;
        mData = data;
    }

    /** Returns the unique identifier of this data. */
    @NonNull
    public String getId() {
        return mId;
    }

    /** Returns the medical resource type. */
    @MedicalResourceType
    public int getType() {
        return mType;
    }

    /** Returns The data source ID where the data comes from. */
    @NonNull
    public String getDataSourceId() {
        return mDataSourceId;
    }

    /** Returns the FHIR resource data in JSON representation. */
    @NonNull
    public String getData() {
        return mData;
    }

    /**
     * Valid set of values for this IntDef. Update this set when add new type or deprecate existing
     * type.
     *
     * @hide
     */
    public static final Set<Integer> VALID_TYPES = Set.of(MEDICAL_RESOURCE_TYPE_UNKNOWN);

    /** Indicates whether some other object is "equal to" this one. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicalResource that)) return false;
        return getId().equals(that.getId())
                && getType() == that.getType()
                && getDataSourceId().equals(that.getDataSourceId())
                && getData().equals(that.getData());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return hash(getId(), getType(), getDataSourceId(), getData());
    }

    /** Returns a string representation of this {@link MedicalResource}. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append("id=").append(getId());
        sb.append(",type=").append(getType());
        sb.append(",dataSourceId=").append(getDataSourceId());
        sb.append(",data=").append(getData());
        sb.append("}");
        return sb.toString();
    }

    /** Builder class for {@link MedicalResource} */
    public static final class Builder {
        @NonNull private String mId;
        @MedicalResourceType private int mType;
        @NonNull private String mDataSourceId;
        @NonNull private String mData;

        public Builder(
                @NonNull String id,
                @MedicalResourceType int type,
                @NonNull String dataSourceId,
                @NonNull String data) {
            requireNonNull(id);
            requireNonNull(dataSourceId);
            requireNonNull(data);
            validateIntDefValue(type, VALID_TYPES, MedicalResourceType.class.getSimpleName());

            mId = id;
            mType = type;
            mDataSourceId = dataSourceId;
            mData = data;
        }

        public Builder(@NonNull Builder original) {
            requireNonNull(original);
            mId = original.mId;
            mType = original.mType;
            mDataSourceId = original.mDataSourceId;
            mData = original.mData;
        }

        public Builder(@NonNull MedicalResource original) {
            requireNonNull(original);
            mId = original.getId();
            mType = original.getType();
            mDataSourceId = original.getDataSourceId();
            mData = original.getData();
        }

        /**
         * Sets the unique identifier of this data, assigned by the Android Health Platform at
         * insertion time.
         */
        @NonNull
        public Builder setId(@NonNull String id) {
            requireNonNull(id);
            mId = id;
            return this;
        }

        /**
         * Sets the medical resource type, assigned by the Android Health Platform at insertion
         * time.
         */
        @NonNull
        public Builder setType(@MedicalResourceType int type) {
            validateIntDefValue(type, VALID_TYPES, MedicalResourceType.class.getSimpleName());
            mType = type;
            return this;
        }

        /** Sets the data source ID where the data comes from. */
        @NonNull
        public Builder setDataSourceId(@NonNull String dataSourceId) {
            requireNonNull(dataSourceId);
            mDataSourceId = dataSourceId;
            return this;
        }

        /** Sets the FHIR resource data in JSON representation. */
        @NonNull
        public Builder setData(@NonNull String data) {
            requireNonNull(data);
            mData = data;
            return this;
        }

        /** Returns a new instance of {@link MedicalResource} with the specified parameters. */
        @NonNull
        public MedicalResource build() {
            return new MedicalResource(mId, mType, mDataSourceId, mData);
        }
    }
}

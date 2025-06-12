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

import static android.health.connect.datatypes.MedicalDataSource.validateMedicalDataSourceIds;
import static android.health.connect.datatypes.validation.ValidationUtils.validateIntDefValue;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.health.connect.MedicalResourceId;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * A class to capture the user's medical data. This is the class used for all medical resource
 * types.
 *
 * <p>The data representation follows the <a href="https://hl7.org/fhir/">Fast Healthcare
 * Interoperability Resources (FHIR)</a> standard.
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class MedicalResource implements Parcelable {

    /** Medical resource type labelling data as vaccines. */
    public static final int MEDICAL_RESOURCE_TYPE_VACCINES = 1;

    /** Medical resource type labelling data as allergies or intolerances. */
    public static final int MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES = 2;

    /** Medical resource type labelling data as to do with pregnancy. */
    public static final int MEDICAL_RESOURCE_TYPE_PREGNANCY = 3;

    /** Medical resource type labelling data as social history. */
    public static final int MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY = 4;

    /** Medical resource type labelling data as vital signs. */
    public static final int MEDICAL_RESOURCE_TYPE_VITAL_SIGNS = 5;

    /** Medical resource type labelling data as results (Laboratory or pathology). */
    public static final int MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS = 6;

    /**
     * Medical resource type labelling data as medical conditions (clinical condition, problem,
     * diagnosis etc).
     */
    public static final int MEDICAL_RESOURCE_TYPE_CONDITIONS = 7;

    /** Medical resource type labelling data as procedures (actions taken on or for a patient). */
    public static final int MEDICAL_RESOURCE_TYPE_PROCEDURES = 8;

    /** Medical resource type labelling data as medication related. */
    public static final int MEDICAL_RESOURCE_TYPE_MEDICATIONS = 9;

    /**
     * Medical resource type labelling data as related to personal details, including demographic
     * information such as name, date of birth, and contact details such as address or telephone
     * numbers.
     */
    public static final int MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS = 10;

    /**
     * Medical resource type labelling data as related to practitioners. This is information about
     * the doctors, nurses, masseurs, physios, etc who have been involved with the user.
     */
    public static final int MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS = 11;

    /**
     * Medical resource type labelling data as related to an encounter with a practitioner. This
     * includes visits to healthcare providers and remote encounters such as telephone and
     * videoconference appointments, and information about the time, location and organization who
     * is being met.
     */
    public static final int MEDICAL_RESOURCE_TYPE_VISITS = 12;

    /** @hide */
    @IntDef({
        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
        MEDICAL_RESOURCE_TYPE_CONDITIONS,
        MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS,
        MEDICAL_RESOURCE_TYPE_MEDICATIONS,
        MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS,
        MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS,
        MEDICAL_RESOURCE_TYPE_PREGNANCY,
        MEDICAL_RESOURCE_TYPE_PROCEDURES,
        MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY,
        MEDICAL_RESOURCE_TYPE_VACCINES,
        MEDICAL_RESOURCE_TYPE_VISITS,
        MEDICAL_RESOURCE_TYPE_VITAL_SIGNS,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MedicalResourceType {}

    @MedicalResourceType private final int mType;
    @NonNull private final MedicalResourceId mId;
    @NonNull private final String mDataSourceId;
    @NonNull private final FhirVersion mFhirVersion;
    @NonNull private final FhirResource mFhirResource;

    /** @hide */
    private long mLastModifiedTimestamp;

    /**
     * Creates a new instance of {@link MedicalResource} which takes in {@code
     * lastModifiedTimestamp} as a parameter as well. The {@code lastModifiedTimestamp} is currently
     * only used internally to ensure D2D merge process, copies over the exact timestamp of when the
     * {@link MedicalResource} was modified.
     *
     * @hide
     */
    public MedicalResource(
            @MedicalResourceType int type,
            @NonNull String dataSourceId,
            @NonNull FhirVersion fhirVersion,
            @NonNull FhirResource fhirResource,
            long lastModifiedTimestamp) {
        this(type, dataSourceId, fhirVersion, fhirResource);
        mLastModifiedTimestamp = lastModifiedTimestamp;
    }

    /**
     * Creates a new instance of {@link MedicalResource}. Please see {@link MedicalResource.Builder}
     * for more detailed parameters information.
     */
    private MedicalResource(
            @MedicalResourceType int type,
            @NonNull String dataSourceId,
            @NonNull FhirVersion fhirVersion,
            @NonNull FhirResource fhirResource) {
        requireNonNull(dataSourceId);
        requireNonNull(fhirVersion);
        requireNonNull(fhirResource);
        validateMedicalResourceType(type);
        validateMedicalDataSourceIds(Set.of(dataSourceId));

        mType = type;
        mDataSourceId = dataSourceId;
        mFhirVersion = fhirVersion;
        mFhirResource = fhirResource;
        mId = new MedicalResourceId(dataSourceId, fhirResource.getType(), fhirResource.getId());
    }

    /**
     * Constructs this object with the data present in {@code parcel}. It should be in the same
     * order as {@link MedicalResource#writeToParcel}.
     */
    private MedicalResource(@NonNull Parcel in) {
        requireNonNull(in);
        mType = in.readInt();
        validateMedicalResourceType(mType);
        mDataSourceId = requireNonNull(in.readString());
        validateMedicalDataSourceIds(Set.of(mDataSourceId));
        mFhirVersion =
                requireNonNull(
                        in.readParcelable(FhirVersion.class.getClassLoader(), FhirVersion.class));
        mFhirResource =
                requireNonNull(
                        in.readParcelable(FhirResource.class.getClassLoader(), FhirResource.class));
        mId = new MedicalResourceId(mDataSourceId, mFhirResource.getType(), mFhirResource.getId());
    }

    @NonNull
    public static final Creator<MedicalResource> CREATOR =
            new Creator<>() {
                @Override
                public MedicalResource createFromParcel(Parcel in) {
                    return new MedicalResource(in);
                }

                @Override
                public MedicalResource[] newArray(int size) {
                    return new MedicalResource[size];
                }
            };

    /**
     * Returns the medical resource type, assigned by the Android Health Platform at insertion time.
     *
     * <p>For a list of supported types, see the {@link MedicalResource} type constants, such as
     * {@link #MEDICAL_RESOURCE_TYPE_VACCINES}. Clients should be aware that this list is non
     * exhaustive and may increase in future releases when additional types will need to be handled.
     */
    @MedicalResourceType
    public int getType() {
        return mType;
    }

    /** Returns the ID of this {@link MedicalResource} as {@link MedicalResourceId}. */
    @NonNull
    public MedicalResourceId getId() {
        return mId;
    }

    /** Returns the unique {@link MedicalDataSource} ID of where the data comes from. */
    @NonNull
    public String getDataSourceId() {
        return mDataSourceId;
    }

    /** Returns the FHIR version being used for {@code mFhirResource} */
    @NonNull
    public FhirVersion getFhirVersion() {
        return mFhirVersion;
    }

    /** Returns the enclosed {@link FhirResource} object. */
    @NonNull
    public FhirResource getFhirResource() {
        return mFhirResource;
    }

    /**
     * Returns the last modified timestamp for this {@link MedicalResource}.
     *
     * @hide
     */
    public long getLastModifiedTimestamp() {
        return mLastModifiedTimestamp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        requireNonNull(dest);
        dest.writeInt(getType());
        dest.writeString(getDataSourceId());
        dest.writeParcelable(getFhirVersion(), 0);
        dest.writeParcelable(getFhirResource(), 0);
    }

    /**
     * Valid set of values for this IntDef. Update this set when add new type or deprecate existing
     * type.
     *
     * @hide
     */
    public static final Set<Integer> VALID_TYPES =
            Set.of(
                    MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                    MEDICAL_RESOURCE_TYPE_CONDITIONS,
                    MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS,
                    MEDICAL_RESOURCE_TYPE_MEDICATIONS,
                    MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS,
                    MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS,
                    MEDICAL_RESOURCE_TYPE_PREGNANCY,
                    MEDICAL_RESOURCE_TYPE_PROCEDURES,
                    MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY,
                    MEDICAL_RESOURCE_TYPE_VACCINES,
                    MEDICAL_RESOURCE_TYPE_VISITS,
                    MEDICAL_RESOURCE_TYPE_VITAL_SIGNS);

    /**
     * Validates the provided {@code medicalResourceType} is in the {@link
     * MedicalResource#VALID_TYPES} set.
     *
     * <p>Throws {@link IllegalArgumentException} if not.
     *
     * @hide
     */
    public static void validateMedicalResourceType(@MedicalResourceType int medicalResourceType) {
        validateIntDefValue(
                medicalResourceType, VALID_TYPES, MedicalResourceType.class.getSimpleName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicalResource that)) return false;
        return getType() == that.getType()
                && getDataSourceId().equals(that.getDataSourceId())
                && getFhirVersion().equals(that.getFhirVersion())
                && getFhirResource().equals(that.getFhirResource());
    }

    @Override
    public int hashCode() {
        return hash(getType(), getDataSourceId(), getFhirVersion(), getFhirResource());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append("type=").append(getType());
        sb.append(",dataSourceId=").append(getDataSourceId());
        sb.append(",fhirVersion=").append(getFhirVersion());
        sb.append(",fhirResource=").append(getFhirResource());
        sb.append("}");
        return sb.toString();
    }

    /** Builder class for {@link MedicalResource}. */
    public static final class Builder {
        @MedicalResourceType private int mType;
        @NonNull private String mDataSourceId;
        @NonNull private FhirVersion mFhirVersion;
        @NonNull private FhirResource mFhirResource;

        /**
         * Constructs a new {@link MedicalResource.Builder} instance.
         *
         * @param type The medical resource type.
         * @param dataSourceId The unique {@link MedicalDataSource} ID of where the data comes from.
         * @param fhirVersion the FHIR version being used for {@code fhirResource}.
         * @param fhirResource The enclosed {@link FhirResource} object.
         * @throws IllegalArgumentException if the provided medical resource {@code type} is not a
         *     valid supported type, or {@code dataSourceId} is not a valid ID.
         */
        public Builder(
                @MedicalResourceType int type,
                @NonNull String dataSourceId,
                @NonNull FhirVersion fhirVersion,
                @NonNull FhirResource fhirResource) {
            requireNonNull(dataSourceId);
            requireNonNull(fhirVersion);
            requireNonNull(fhirResource);
            validateMedicalResourceType(type);
            validateMedicalDataSourceIds(Set.of(dataSourceId));

            mType = type;
            mDataSourceId = dataSourceId;
            mFhirVersion = fhirVersion;
            mFhirResource = fhirResource;
        }

        /** Constructs a clone of the other {@link MedicalResource.Builder}. */
        public Builder(@NonNull Builder other) {
            requireNonNull(other);
            mType = other.mType;
            mDataSourceId = other.mDataSourceId;
            mFhirVersion = other.mFhirVersion;
            mFhirResource = other.mFhirResource;
        }

        /** Constructs a clone of the other {@link MedicalResource} instance. */
        public Builder(@NonNull MedicalResource other) {
            requireNonNull(other);
            mType = other.getType();
            mDataSourceId = other.getDataSourceId();
            mFhirVersion = other.getFhirVersion();
            mFhirResource = other.getFhirResource();
        }

        /**
         * Sets the medical resource type.
         *
         * @throws IllegalArgumentException if the provided medical resource {@code type} is not a
         *     valid supported type.
         */
        @NonNull
        public Builder setType(@MedicalResourceType int type) {
            validateMedicalResourceType(type);
            mType = type;
            return this;
        }

        /**
         * Sets the unique {@link MedicalDataSource} ID of where the data comes from.
         *
         * @throws IllegalArgumentException if the provided {@code dataSourceId} is not a valid ID.
         */
        @NonNull
        public Builder setDataSourceId(@NonNull String dataSourceId) {
            requireNonNull(dataSourceId);
            validateMedicalDataSourceIds(Set.of(dataSourceId));
            mDataSourceId = dataSourceId;
            return this;
        }

        /** Sets the FHIR version being used for {@code fhirResource}. */
        @NonNull
        public Builder setFhirVersion(@NonNull FhirVersion fhirVersion) {
            requireNonNull(fhirVersion);
            mFhirVersion = fhirVersion;
            return this;
        }

        /** Sets the enclosed {@link FhirResource} object. */
        @NonNull
        public Builder setFhirResource(@NonNull FhirResource fhirResource) {
            requireNonNull(fhirResource);
            mFhirResource = fhirResource;
            return this;
        }

        /** Returns a new instance of {@link MedicalResource} with the specified parameters. */
        @NonNull
        public MedicalResource build() {
            return new MedicalResource(mType, mDataSourceId, mFhirVersion, mFhirResource);
        }
    }
}

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
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * A class to capture the FHIR resource data. This is the class used for all supported FHIR resource
 * types, which is a subset of the resource list on <a
 * href="https://build.fhir.org/resourcelist.html">the official FHIR website</a>. The list of
 * supported types will likely expand in future releases.
 *
 * <p>FHIR stands for the <a href="https://hl7.org/fhir/">Fast Healthcare Interoperability Resources
 * </a> standard.
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class FhirResource implements Parcelable {
    // LINT.IfChange
    /**
     * FHIR resource type for <a href="https://www.hl7.org/fhir/immunization.html">Immunization</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_IMMUNIZATION = 1;

    /**
     * FHIR resource type for <a
     * href="https://www.hl7.org/fhir/allergyintolerance.html">AllergyIntolerance</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE = 2;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/observation.html">FHIR
     * Observation</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_OBSERVATION = 3;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/condition.html">FHIR
     * Condition</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_CONDITION = 4;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/procedure.html">FHIR
     * Procedure</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_PROCEDURE = 5;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/medication.html">FHIR
     * Medication</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_MEDICATION = 6;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/medicationrequest.html">FHIR
     * MedicationRequest</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_MEDICATION_REQUEST = 7;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/medicationstatement.html">FHIR
     * MedicationStatement</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT = 8;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/patient.html">FHIR Patient</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_PATIENT = 9;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/practitioner.html">FHIR
     * Practitioner</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_PRACTITIONER = 10;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/practitionerrole.html">FHIR
     * PractitionerRole</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE = 11;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/encounter.html">FHIR
     * Encounter</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_ENCOUNTER = 12;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/location.html">FHIR Location</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_LOCATION = 13;

    /**
     * FHIR resource type for a <a href="https://www.hl7.org/fhir/organization.html">FHIR
     * Organization</a>.
     */
    public static final int FHIR_RESOURCE_TYPE_ORGANIZATION = 14;

    // LINT.ThenChange(/service/proto/phr/fhir_spec_utils.py:fhir_resource_type_mapping)

    /** @hide */
    @IntDef({
        FHIR_RESOURCE_TYPE_IMMUNIZATION,
        FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE,
        FHIR_RESOURCE_TYPE_OBSERVATION,
        FHIR_RESOURCE_TYPE_CONDITION,
        FHIR_RESOURCE_TYPE_PROCEDURE,
        FHIR_RESOURCE_TYPE_MEDICATION,
        FHIR_RESOURCE_TYPE_MEDICATION_REQUEST,
        FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT,
        FHIR_RESOURCE_TYPE_PATIENT,
        FHIR_RESOURCE_TYPE_PRACTITIONER,
        FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE,
        FHIR_RESOURCE_TYPE_ENCOUNTER,
        FHIR_RESOURCE_TYPE_LOCATION,
        FHIR_RESOURCE_TYPE_ORGANIZATION,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FhirResourceType {}

    @FhirResourceType private final int mType;
    @NonNull private final String mId;
    @NonNull private final String mData;

    /**
     * Creates a new instance of {@link FhirResource}. Please see {@link FhirResource.Builder} for
     * more detailed parameters information.
     */
    private FhirResource(@FhirResourceType int type, @NonNull String id, @NonNull String data) {
        validateFhirResourceType(type);
        requireNonNull(id);
        requireNonNull(data);

        mType = type;
        mId = id;
        mData = data;
    }

    /**
     * Constructs this object with the data present in {@code parcel}. It should be in the same
     * order as {@link FhirResource#writeToParcel}.
     */
    private FhirResource(@NonNull Parcel in) {
        requireNonNull(in);
        mType = in.readInt();
        validateFhirResourceType(mType);
        mId = requireNonNull(in.readString());
        mData = requireNonNull(in.readString());
    }

    @NonNull
    public static final Creator<FhirResource> CREATOR =
            new Creator<>() {
                @Override
                public FhirResource createFromParcel(Parcel in) {
                    return new FhirResource(in);
                }

                @Override
                public FhirResource[] newArray(int size) {
                    return new FhirResource[size];
                }
            };

    /**
     * Returns the FHIR resource type. This is extracted from the "resourceType" field in {@link
     * #getData}.
     *
     * <p>The list of supported types is a subset of the resource list on <a
     * href="https://build.fhir.org/resourcelist.html">the official FHIR website</a>. For a list of
     * supported types, see the {@link FhirResource} constants, such as {@link
     * #FHIR_RESOURCE_TYPE_IMMUNIZATION}. Clients should be aware that this list is non exhaustive
     * and may increase in future releases when additional types will need to be handled.
     */
    @FhirResourceType
    public int getType() {
        return mType;
    }

    /**
     * Returns the FHIR resource ID. This is extracted from the "id" field in {@code data}. This is
     * NOT a unique identifier among all {@link FhirResource}s.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /** Returns the FHIR resource data in JSON representation. */
    @NonNull
    public String getData() {
        return mData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        requireNonNull(dest);
        dest.writeInt(getType());
        dest.writeString(getId());
        dest.writeString(getData());
    }

    /**
     * Valid set of values for this IntDef. Update this set when add new type or deprecate existing
     * type.
     */
    private static final Set<Integer> VALID_TYPES =
            Set.of(
                    FHIR_RESOURCE_TYPE_IMMUNIZATION,
                    FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE,
                    FHIR_RESOURCE_TYPE_OBSERVATION,
                    FHIR_RESOURCE_TYPE_CONDITION,
                    FHIR_RESOURCE_TYPE_PROCEDURE,
                    FHIR_RESOURCE_TYPE_MEDICATION,
                    FHIR_RESOURCE_TYPE_MEDICATION_REQUEST,
                    FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT,
                    FHIR_RESOURCE_TYPE_PATIENT,
                    FHIR_RESOURCE_TYPE_PRACTITIONER,
                    FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE,
                    FHIR_RESOURCE_TYPE_ENCOUNTER,
                    FHIR_RESOURCE_TYPE_LOCATION,
                    FHIR_RESOURCE_TYPE_ORGANIZATION);

    /**
     * Validates the provided {@code fhirResourceType} is in the {@link FhirResource#VALID_TYPES}
     * set.
     *
     * <p>Throws {@link IllegalArgumentException} if not.
     *
     * @hide
     */
    public static void validateFhirResourceType(@FhirResourceType int fhirResourceType) {
        validateIntDefValue(fhirResourceType, VALID_TYPES, FhirResourceType.class.getSimpleName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FhirResource that)) return false;
        return getType() == that.getType()
                && getId().equals(that.getId())
                && getData().equals(that.getData());
    }

    @Override
    public int hashCode() {
        return hash(getType(), getId(), getData());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append("type=").append(getType());
        sb.append(",id=").append(getId());
        sb.append(",data=").append(getData());
        sb.append("}");
        return sb.toString();
    }

    /** Builder class for {@link FhirResource}. */
    public static final class Builder {
        @FhirResourceType private int mType;
        @NonNull private String mId;
        @NonNull private String mData;

        /**
         * Constructs a new {@link FhirResource.Builder} instance.
         *
         * @param type The FHIR resource type extracted from the "resourceType" field in {@code
         *     data}.
         * @param id The FHIR resource ID extracted from the "id" field in {@code data}.
         * @param data The FHIR resource data in JSON representation.
         * @throws IllegalArgumentException if the provided FHIR resource {@code type} is not a
         *     valid supported type.
         */
        public Builder(@FhirResourceType int type, @NonNull String id, @NonNull String data) {
            validateFhirResourceType(type);
            requireNonNull(id);
            requireNonNull(data);

            mType = type;
            mId = id;
            mData = data;
        }

        /** Constructs a clone of the other {@link FhirResource.Builder}. */
        public Builder(@NonNull Builder other) {
            requireNonNull(other);
            mType = other.mType;
            mId = other.mId;
            mData = other.mData;
        }

        /** Constructs a clone of the other {@link FhirResource} instance. */
        public Builder(@NonNull FhirResource other) {
            requireNonNull(other);
            mType = other.getType();
            mId = other.getId();
            mData = other.getData();
        }

        /**
         * Sets the FHIR resource type. This is extracted from the "resourceType" field in {@code
         * data}.
         *
         * @throws IllegalArgumentException if the provided FHIR resource {@code type} is not a
         *     valid supported type.
         */
        @NonNull
        public Builder setType(@FhirResourceType int type) {
            validateFhirResourceType(type);
            mType = type;
            return this;
        }

        /**
         * Sets the FHIR resource ID. This is extracted from the "id" field in {@code data}. This is
         * NOT a unique identifier among all {@link FhirResource}s.
         */
        @NonNull
        public Builder setId(@NonNull String id) {
            requireNonNull(id);
            mId = id;
            return this;
        }

        /** Sets the FHIR resource data in JSON representation. */
        @NonNull
        public Builder setData(@NonNull String data) {
            requireNonNull(data);
            mData = data;
            return this;
        }

        /** Returns a new instance of {@link FhirResource} with the specified parameters. */
        @NonNull
        public FhirResource build() {
            return new FhirResource(mType, mId, mData);
        }
    }
}

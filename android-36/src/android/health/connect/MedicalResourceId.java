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

package android.health.connect;

import static android.health.connect.datatypes.FhirResource.validateFhirResourceType;
import static android.health.connect.datatypes.MedicalDataSource.validateMedicalDataSourceIds;
import static android.health.connect.internal.datatypes.utils.FhirResourceTypeStringToIntMapper.getFhirResourceTypeInt;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.FhirResource.FhirResourceType;
import android.health.connect.datatypes.MedicalDataSource;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to represent a unique identifier of a medical resource.
 *
 * <p>This class contains a set of properties that together represent a unique identifier of a
 * medical resource.
 *
 * <p>The medical resource data representation follows the <a href="https://hl7.org/fhir/">Fast
 * Healthcare Interoperability Resources (FHIR)</a> standard.
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class MedicalResourceId implements Parcelable {
    @NonNull private final String mDataSourceId;
    @FhirResourceType private final int mFhirResourceType;
    @NonNull private final String mFhirResourceId;

    // Regex of a FHIR resource id is referenced from <a
    // href="https://build.fhir.org/datatypes.html#id">the official FHIR datatypes</a>.
    private static final String FHIR_REFERENCE_REGEX = "([A-Za-z]+)/([A-Za-z0-9-.]+)";

    /**
     * Constructs a new {@link MedicalResourceId} instance.
     *
     * @param dataSourceId The unique identifier of the existing {@link MedicalDataSource},
     *     representing where the data comes from.
     * @param fhirResourceType The FHIR resource type. This is the "resourceType" field from a JSON
     *     representation of FHIR resource data.
     * @param fhirResourceId The FHIR resource ID. This is the "id" field from a JSON representation
     *     of FHIR resource data.
     * @throws IllegalArgumentException if the provided {@code dataSourceId} is not a valid ID, or
     *     {@code fhirResourceType} is not a valid supported type.
     */
    public MedicalResourceId(
            @NonNull String dataSourceId,
            @FhirResourceType int fhirResourceType,
            @NonNull String fhirResourceId) {
        requireNonNull(dataSourceId);
        requireNonNull(fhirResourceId);
        validateFhirResourceType(fhirResourceType);
        validateMedicalDataSourceIds(Set.of(dataSourceId));
        mDataSourceId = dataSourceId;
        mFhirResourceType = fhirResourceType;
        mFhirResourceId = fhirResourceId;
    }

    /**
     * Creates a {@link MedicalResourceId} instance from {@code dataSourceId} and {@code
     * fhirReference}.
     *
     * @param dataSourceId The unique identifier of the existing {@link MedicalDataSource},
     *     representing where the data comes from.
     * @param fhirReference The FHIR reference string typically extracted from the "reference" field
     *     in one FHIR resource (source), pointing to another FHIR resource (target) within the same
     *     data source, for example "Patient/034AB16".
     * @throws IllegalArgumentException if the provided {@code dataSourceId} is not a valid ID, the
     *     referenced resource type is not a valid {@link FhirResource} type supported by Health
     *     Connect, or {@code fhirReference} does not match with the pattern of {@code
     *     $fhir_resource_type/$fhir_resource_id}, where the FHIR resource type should align with
     *     the resource list in <a href="https://build.fhir.org/resourcelist.html">the official FHIR
     *     website</a>, and the FHIR resource ID should also follow the pattern described in <a
     *     href="https://build.fhir.org/datatypes.html#id">the official FHIR datatypes</a>.
     */
    @NonNull
    public static MedicalResourceId fromFhirReference(
            @NonNull String dataSourceId, @NonNull String fhirReference) {
        requireNonNull(dataSourceId);
        requireNonNull(fhirReference);
        validateMedicalDataSourceIds(Set.of(dataSourceId));
        Pattern pattern = Pattern.compile(FHIR_REFERENCE_REGEX);
        Matcher matcher = pattern.matcher(fhirReference);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid FHIR reference. Provided "
                            + fhirReference
                            + "does not match "
                            + FHIR_REFERENCE_REGEX);
        }
        @FhirResourceType int fhirResourceType = getFhirResourceTypeInt(matcher.group(1));
        String fhirResourceId = matcher.group(2);
        return new MedicalResourceId(dataSourceId, fhirResourceType, fhirResourceId);
    }

    /**
     * Constructs this object with the data present in {@code parcel}. It should be in the same
     * order as {@link MedicalResourceId#writeToParcel}.
     */
    private MedicalResourceId(@NonNull Parcel in) {
        requireNonNull(in);
        mDataSourceId = requireNonNull(in.readString());
        validateMedicalDataSourceIds(Set.of(mDataSourceId));
        mFhirResourceType = in.readInt();
        validateFhirResourceType(mFhirResourceType);
        mFhirResourceId = requireNonNull(in.readString());
    }

    @NonNull
    public static final Creator<MedicalResourceId> CREATOR =
            new Creator<>() {
                /**
                 * Reading from the {@link Parcel} should have the same order as {@link
                 * MedicalResourceId#writeToParcel}.
                 */
                @Override
                public MedicalResourceId createFromParcel(Parcel in) {
                    return new MedicalResourceId(in);
                }

                @Override
                public MedicalResourceId[] newArray(int size) {
                    return new MedicalResourceId[size];
                }
            };

    /** Returns the unique {@link MedicalDataSource} ID of where the data comes from. */
    @NonNull
    public String getDataSourceId() {
        return mDataSourceId;
    }

    /** Returns the FHIR resource type. */
    @FhirResourceType
    public int getFhirResourceType() {
        return mFhirResourceType;
    }

    /** Returns the FHIR resource ID. */
    @NonNull
    public String getFhirResourceId() {
        return mFhirResourceId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        requireNonNull(dest);
        dest.writeString(getDataSourceId());
        dest.writeInt(getFhirResourceType());
        dest.writeString(getFhirResourceId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicalResourceId that)) return false;
        return getDataSourceId().equals(that.getDataSourceId())
                && getFhirResourceType() == that.getFhirResourceType()
                && getFhirResourceId().equals(that.getFhirResourceId());
    }

    @Override
    public int hashCode() {
        return hash(getDataSourceId(), getFhirResourceType(), getFhirResourceId());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append("dataSourceId=").append(getDataSourceId());
        sb.append(",fhirResourceType=").append(getFhirResourceType());
        sb.append(",fhirResourceId=").append(getFhirResourceId());
        sb.append("}");
        return sb.toString();
    }
}

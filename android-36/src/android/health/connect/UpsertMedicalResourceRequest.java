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

import static android.health.connect.datatypes.MedicalDataSource.validateMedicalDataSourceIds;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.health.connect.datatypes.FhirVersion;
import android.health.connect.datatypes.MedicalDataSource;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.healthfitness.flags.Flags;

import java.util.Set;

/**
 * An upsert request for {@link HealthConnectManager#upsertMedicalResources}.
 *
 * <p>Medical data is represented using the <a href="https://hl7.org/fhir/">Fast Healthcare
 * Interoperability Resources (FHIR)</a> standard.
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class UpsertMedicalResourceRequest implements Parcelable {
    @NonNull private final String mDataSourceId;
    @NonNull private final FhirVersion mFhirVersion;
    @NonNull private final String mData;
    private int mDataSize;

    @NonNull
    public static final Creator<UpsertMedicalResourceRequest> CREATOR =
            new Creator<>() {
                @Override
                public UpsertMedicalResourceRequest createFromParcel(Parcel in) {
                    return new UpsertMedicalResourceRequest(in);
                }

                @Override
                public UpsertMedicalResourceRequest[] newArray(int size) {
                    return new UpsertMedicalResourceRequest[size];
                }
            };

    /**
     * Creates a new instance of {@link UpsertMedicalResourceRequest}. Please see {@link
     * UpsertMedicalResourceRequest.Builder} for more detailed parameters information.
     */
    private UpsertMedicalResourceRequest(
            @NonNull String dataSourceId, @NonNull FhirVersion fhirVersion, @NonNull String data) {
        requireNonNull(dataSourceId);
        requireNonNull(fhirVersion);
        requireNonNull(data);
        validateMedicalDataSourceIds(Set.of(dataSourceId));

        mDataSourceId = dataSourceId;
        mFhirVersion = fhirVersion;
        mData = data;
    }

    private UpsertMedicalResourceRequest(@NonNull Parcel in) {
        requireNonNull(in);
        int dataAvailStartPosition = in.dataAvail();

        mDataSourceId = requireNonNull(in.readString());
        validateMedicalDataSourceIds(Set.of(mDataSourceId));
        mFhirVersion =
                requireNonNull(
                        in.readParcelable(FhirVersion.class.getClassLoader(), FhirVersion.class));
        mData = requireNonNull(in.readString());

        if (Flags.phrUpsertFixParcelSizeCalculation()) {
            mDataSize = dataAvailStartPosition - in.dataAvail();
        } else {
            mDataSize = in.dataSize();
        }
    }

    /**
     * Returns the unique ID of the existing {@link MedicalDataSource}, to represent where the
     * {@code data} is coming from.
     */
    @NonNull
    public String getDataSourceId() {
        return mDataSourceId;
    }

    /**
     * Returns the FHIR version being used for {@code data}. For the request to succeed this must
     * match the {@link MedicalDataSource#getFhirVersion()} FHIR version of the {@link
     * MedicalDataSource} with the provided {@code dataSourceId}.
     */
    @NonNull
    public FhirVersion getFhirVersion() {
        return mFhirVersion;
    }

    /** Returns the FHIR resource data in JSON representation. */
    @NonNull
    public String getData() {
        return mData;
    }

    /**
     * Returns the size of the parcel when the class was created from Parcel.
     *
     * @hide
     */
    public long getDataSize() {
        return mDataSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        requireNonNull(dest);
        dest.writeString(mDataSourceId);
        dest.writeParcelable(mFhirVersion, 0);
        dest.writeString(mData);
    }

    @Override
    public int hashCode() {
        return hash(getDataSourceId(), getFhirVersion(), getData());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpsertMedicalResourceRequest that)) return false;
        return getDataSourceId().equals(that.getDataSourceId())
                && getFhirVersion().equals(that.getFhirVersion())
                && getData().equals(that.getData());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append("dataSourceId=").append(mDataSourceId);
        sb.append(",fhirVersion=").append(mFhirVersion);
        sb.append(",data=").append(mData);
        sb.append("}");
        return sb.toString();
    }

    /** Builder class for {@link UpsertMedicalResourceRequest}. */
    public static final class Builder {
        private String mDataSourceId;
        private FhirVersion mFhirVersion;
        private String mData;

        /**
         * Constructs a new {@link UpsertMedicalResourceRequest.Builder} instance.
         *
         * @param dataSourceId The unique identifier of the existing {@link MedicalDataSource},
         *     representing where the data comes from.
         * @param fhirVersion The {@link FhirVersion} object that represents the FHIR version being
         *     used for {@code data}. This has to match the FHIR version of the {@link
         *     MedicalDataSource}.
         * @param data The FHIR resource data in JSON representation.
         * @throws IllegalArgumentException if the provided {@code dataSourceId} is not a valid ID.
         */
        public Builder(
                @NonNull String dataSourceId,
                @NonNull FhirVersion fhirVersion,
                @NonNull String data) {
            requireNonNull(dataSourceId);
            requireNonNull(fhirVersion);
            requireNonNull(data);
            validateMedicalDataSourceIds(Set.of(dataSourceId));

            mDataSourceId = dataSourceId;
            mFhirVersion = fhirVersion;
            mData = data;
        }

        /** Constructs a clone of the other {@link UpsertMedicalResourceRequest.Builder}. */
        public Builder(@NonNull Builder other) {
            requireNonNull(other);
            mDataSourceId = other.mDataSourceId;
            mFhirVersion = other.mFhirVersion;
            mData = other.mData;
        }

        /** Constructs a clone of the other {@link UpsertMedicalResourceRequest} instance. */
        public Builder(@NonNull UpsertMedicalResourceRequest other) {
            requireNonNull(other);
            mDataSourceId = other.getDataSourceId();
            mFhirVersion = other.getFhirVersion();
            mData = other.getData();
        }

        /**
         * Sets the unique ID of the existing {@link MedicalDataSource}, to represent where the
         * {@code data} is coming from.
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

        /**
         * Sets the FHIR version being used for {@code data}. For the request to succeed this must
         * match the {@link MedicalDataSource#getFhirVersion()} FHIR version} of the {@link
         * MedicalDataSource} with the provided {@code dataSourceId}.
         */
        @NonNull
        public Builder setFhirVersion(@NonNull FhirVersion fhirVersion) {
            requireNonNull(fhirVersion);
            mFhirVersion = fhirVersion;
            return this;
        }

        /** Sets the FHIR resource data in JSON format. */
        @NonNull
        public Builder setData(@NonNull String data) {
            requireNonNull(data);
            mData = data;
            return this;
        }

        /**
         * Returns a new instance of {@link UpsertMedicalResourceRequest} with the specified
         * parameters.
         */
        @NonNull
        public UpsertMedicalResourceRequest build() {
            return new UpsertMedicalResourceRequest(mDataSourceId, mFhirVersion, mData);
        }
    }
}

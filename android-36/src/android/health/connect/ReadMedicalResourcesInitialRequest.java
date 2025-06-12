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

import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;
import static android.health.connect.Constants.MINIMUM_PAGE_SIZE;
import static android.health.connect.datatypes.MedicalDataSource.validateMedicalDataSourceIds;
import static android.health.connect.datatypes.MedicalResource.MedicalResourceType;
import static android.health.connect.datatypes.MedicalResource.validateMedicalResourceType;
import static android.health.connect.datatypes.validation.ValidationUtils.requireInRange;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.health.connect.aidl.ReadMedicalResourcesRequestParcel;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.util.ArraySet;

import java.util.HashSet;
import java.util.Set;

/**
 * An initial read request with specified filters for {@link
 * HealthConnectManager#readMedicalResources}.
 *
 * <p>On receiving the response, if {@link ReadMedicalResourcesResponse#getNextPageToken()} is not
 * {@code null}, then use the next token with {@link ReadMedicalResourcesPageRequest} to read the
 * next page.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ReadMedicalResourcesInitialRequest initialRequest
 *     = new ReadMedicalResourcesInitialRequest.Builder(...).build();
 * ReadMedicalResourcesResponse response = makeRequest(initialRequest);
 * String pageToken = response.getNextPageToken();
 *
 * while (pageToken != null) {
 *     ReadMedicalResourcesPageRequest pageRequest = new ReadMedicalResourcesPageRequest(pageToken);
 *     response = makeRequest(pageRequest);
 *     pageToken = response.getNextPageToken();
 * }
 * }</pre>
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class ReadMedicalResourcesInitialRequest extends ReadMedicalResourcesRequest {
    @MedicalResourceType private final int mMedicalResourceType;
    @NonNull private final Set<String> mDataSourceIds;

    /**
     * Creates a new instance of {@link ReadMedicalResourcesInitialRequest}. Please see {@link
     * ReadMedicalResourcesInitialRequest.Builder} for more detailed parameters information.
     */
    private ReadMedicalResourcesInitialRequest(
            @MedicalResourceType int medicalResourceType,
            @NonNull Set<String> dataSourceIds,
            @IntRange(from = MINIMUM_PAGE_SIZE, to = MAXIMUM_PAGE_SIZE) int pageSize) {
        super(pageSize);
        validateMedicalResourceType(medicalResourceType);
        requireNonNull(dataSourceIds);
        validateMedicalDataSourceIds(dataSourceIds);

        mMedicalResourceType = medicalResourceType;
        mDataSourceIds = dataSourceIds;
    }

    /** Returns the medical resource type. */
    @MedicalResourceType
    public int getMedicalResourceType() {
        return mMedicalResourceType;
    }

    /**
     * Returns the set of IDs of the {@link MedicalDataSource} filter to read from, or an empty set
     * for no filter.
     */
    @NonNull
    public Set<String> getDataSourceIds() {
        return new ArraySet<>(mDataSourceIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReadMedicalResourcesInitialRequest that)) return false;

        return getMedicalResourceType() == that.getMedicalResourceType()
                && getDataSourceIds().equals(that.getDataSourceIds())
                && getPageSize() == that.getPageSize();
    }

    @Override
    public int hashCode() {
        return hash(getMedicalResourceType(), getDataSourceIds(), getPageSize());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + "{"
                + "medicalResourceType="
                + getMedicalResourceType()
                + ",dataSourceIds="
                + getDataSourceIds()
                + ",pageSize="
                + getPageSize()
                + "}";
    }

    /**
     * Returns an instance of {@link ReadMedicalResourcesRequestParcel} to carry the request.
     *
     * @hide
     */
    public ReadMedicalResourcesRequestParcel toParcel() {
        return new ReadMedicalResourcesRequestParcel(this);
    }

    /** Builder class for {@link ReadMedicalResourcesInitialRequest}. */
    public static final class Builder {
        @MedicalResourceType private int mMedicalResourceType;
        @NonNull private Set<String> mDataSourceIds = new HashSet<>();
        private int mPageSize = DEFAULT_PAGE_SIZE;

        /**
         * Constructs a new {@link ReadMedicalResourcesInitialRequest.Builder} instance.
         *
         * @param medicalResourceType The medical resource type.
         * @throws IllegalArgumentException if the provided {@code medicalResourceType} is not a
         *     supported type.
         */
        public Builder(@MedicalResourceType int medicalResourceType) {
            validateMedicalResourceType(medicalResourceType);
            mMedicalResourceType = medicalResourceType;
        }

        /** Constructs a clone of the other {@link ReadMedicalResourcesInitialRequest.Builder}. */
        public Builder(@NonNull Builder other) {
            mMedicalResourceType = other.mMedicalResourceType;
            mDataSourceIds.addAll(other.mDataSourceIds);
            mPageSize = other.mPageSize;
        }

        /** Constructs a clone of the other {@link ReadMedicalResourcesInitialRequest} instance. */
        public Builder(@NonNull ReadMedicalResourcesInitialRequest other) {
            mMedicalResourceType = other.getMedicalResourceType();
            mDataSourceIds.addAll(other.getDataSourceIds());
            mPageSize = other.getPageSize();
        }

        /**
         * Sets the medical resource type.
         *
         * @throws IllegalArgumentException if the provided {@code medicalResourceType} is not a
         *     supported type.
         */
        @NonNull
        public Builder setMedicalResourceType(@MedicalResourceType int medicalResourceType) {
            validateMedicalResourceType(medicalResourceType);
            mMedicalResourceType = medicalResourceType;
            return this;
        }

        /**
         * Adds the data source ID based on which the read operation is to be performed. This should
         * be an ID of the existing {@link MedicalDataSource}.
         *
         * <p>If no {@link MedicalDataSource} ID is added, then {@link MedicalResource}s from all
         * {@link MedicalDataSource}s will be read.
         *
         * @throws IllegalArgumentException if the provided {@code dataSourceId} is not a valid ID.
         */
        @NonNull
        public Builder addDataSourceId(@NonNull String dataSourceId) {
            requireNonNull(dataSourceId);
            validateMedicalDataSourceIds(Set.of(dataSourceId));
            mDataSourceIds.add(dataSourceId);
            return this;
        }

        /**
         * Adds all data source IDs based on which the read operation is to be performed. This
         * should all be IDs of existing {@link MedicalDataSource}s.
         *
         * <p>If no {@link MedicalDataSource} IDs are added, then {@link MedicalResource}s from all
         * {@link MedicalDataSource}s will be read.
         *
         * @throws IllegalArgumentException if the provided {@code dataSourceIds} is null, or any ID
         *     in it is not valid.
         */
        @NonNull
        public Builder addDataSourceIds(@NonNull Set<String> dataSourceIds) {
            requireNonNull(dataSourceIds);
            validateMedicalDataSourceIds(dataSourceIds);
            mDataSourceIds.addAll(dataSourceIds);
            return this;
        }

        /** Clears all the {@link MedicalDataSource} filters for this builder. */
        @NonNull
        public Builder clearDataSourceIds() {
            mDataSourceIds.clear();
            return this;
        }

        /**
         * Sets the maximum number of {@code MedicalResource}s to be returned by the read operation.
         *
         * <p>If not set, default to 1000.
         *
         * @throws IllegalArgumentException if the provided {@code pageSize} is less than 1 or more
         *     than 5000.
         */
        @NonNull
        public Builder setPageSize(
                @IntRange(from = MINIMUM_PAGE_SIZE, to = MAXIMUM_PAGE_SIZE) int pageSize) {
            requireInRange(pageSize, MINIMUM_PAGE_SIZE, MAXIMUM_PAGE_SIZE, "pageSize");
            mPageSize = pageSize;
            return this;
        }

        /**
         * Returns a new instance of {@link ReadMedicalResourcesInitialRequest} with the specified
         * parameters.
         */
        @NonNull
        public ReadMedicalResourcesInitialRequest build() {
            return new ReadMedicalResourcesInitialRequest(
                    mMedicalResourceType, mDataSourceIds, mPageSize);
        }
    }
}

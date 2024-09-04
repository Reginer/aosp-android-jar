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

package android.health.connect;

import android.annotation.NonNull;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.DataOrigin;
import android.util.ArraySet;

import java.util.Objects;
import java.util.Set;

/** A class to create requests for {@link HealthConnectManager#aggregate} */
public final class AggregateRecordsRequest<T> {
    private final TimeRangeFilter mTimeRangeFilter;
    private final Set<AggregationType<T>> mAggregationTypes;
    private final Set<DataOrigin> mDataOriginsFilter;
    /**
     * @param timeRangeFilter Time range b/w which the aggregate operation is to be performed
     * @param aggregationTypes Set of {@link AggregationType} to aggregate
     * @param dataOriginsFilter Set of {@link DataOrigin}s to read from, or empty set for no filter.
     *     <p>Filters applies to all the requests in {@code aggregationTypes}
     *     <p>Here no filters means that data from all data origins will be considered for this
     *     operation
     */
    private AggregateRecordsRequest(
            @NonNull Set<AggregationType<T>> aggregationTypes,
            @NonNull TimeRangeFilter timeRangeFilter,
            @NonNull Set<DataOrigin> dataOriginsFilter) {
        Objects.requireNonNull(timeRangeFilter);
        Objects.requireNonNull(aggregationTypes);
        Objects.requireNonNull(dataOriginsFilter);

        mTimeRangeFilter = timeRangeFilter;
        mAggregationTypes = aggregationTypes;
        mDataOriginsFilter = dataOriginsFilter;
    }

    /**
     * @return time range b/w which the aggregate operation is to be performed
     */
    @NonNull
    public TimeRangeFilter getTimeRangeFilter() {
        return mTimeRangeFilter;
    }

    /**
     * @return Set of integers from {@link AggregationType} to aggregate
     */
    @NonNull
    public Set<AggregationType<T>> getAggregationTypes() {
        return mAggregationTypes;
    }

    /**
     * @return Set of {@link DataOrigin}s to read from, or empty set for no filter
     */
    @NonNull
    public Set<DataOrigin> getDataOriginsFilters() {
        return mDataOriginsFilter;
    }

    public static final class Builder<T> {
        private final TimeRangeFilter mTimeRangeFilter;
        private final Set<AggregationType<T>> mAggregationTypes = new ArraySet<>();
        private final Set<DataOrigin> mDataOriginsFilter = new ArraySet<>();

        /**
         * @param timeRangeFilter Time range b/w which the aggregate operation is to be performed
         *     <p>Filters applies to all the aggregate requests.
         */
        public Builder(@NonNull TimeRangeFilter timeRangeFilter) {
            Objects.requireNonNull(timeRangeFilter);

            mTimeRangeFilter = timeRangeFilter;
        }

        /**
         * @param aggregationType {@link AggregationType} to aggregate.
         */
        @NonNull
        public Builder<T> addAggregationType(@NonNull AggregationType<T> aggregationType) {
            mAggregationTypes.add(aggregationType);

            return this;
        }

        /**
         * Adds {@code dataOriginsFilter} to the set of {@link DataOrigin} to filter for this
         * aggregation.
         *
         * <p>If not set data from all data origins will be considered for this operation
         */
        @NonNull
        public Builder<T> addDataOriginsFilter(@NonNull DataOrigin dataOriginsFilter) {
            Objects.requireNonNull(dataOriginsFilter);

            mDataOriginsFilter.add(dataOriginsFilter);
            return this;
        }

        /**
         * @return Object of {@link AggregateRecordsRequest}
         */
        @NonNull
        public AggregateRecordsRequest<T> build() {
            if (mAggregationTypes.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one of the aggregation types must be set");
            }

            return new AggregateRecordsRequest<T>(
                    mAggregationTypes, mTimeRangeFilter, mDataOriginsFilter);
        }
    }
}

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
import android.annotation.Nullable;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.internal.datatypes.utils.AggregationTypeIdMapper;
import android.util.ArrayMap;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

/**
 * Class to represent the response from {@link HealthConnectManager#aggregateGroupByPeriod}
 *
 * @see HealthConnectManager#aggregateGroupByPeriod
 */
public final class AggregateRecordsGroupedByPeriodResponse<T> {
    private final Map<AggregationType<T>, AggregateResult<T>> mResult;
    private LocalDateTime mStartTime;
    private LocalDateTime mEndTime;

    /**
     * @param startTime Start time of the window for the underlying aggregation
     * @param endTime End time of the window for the underlying aggregation
     * @param result Map representing the result of this window for various aggregations requested
     * @hide
     */
    @SuppressWarnings("unchecked")
    public AggregateRecordsGroupedByPeriodResponse(
            @NonNull LocalDateTime startTime,
            @NonNull LocalDateTime endTime,
            @NonNull Map<Integer, AggregateResult<?>> result) {
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);
        Objects.requireNonNull(result);

        mStartTime = startTime;
        mEndTime = endTime;
        mResult = new ArrayMap<>(result.size());
        result.forEach(
                (key, value) ->
                        mResult.put(
                                (AggregationType<T>)
                                        AggregationTypeIdMapper.getInstance()
                                                .getAggregationTypeFor(key),
                                (AggregateResult<T>) value));
    }

    /**
     * @return Start {@link LocalDateTime} of the window for the underlying aggregation
     */
    @NonNull
    public LocalDateTime getStartTime() {
        return mStartTime;
    }

    /** @hide */
    public AggregateRecordsGroupedByPeriodResponse<T> setStartTime(LocalDateTime startTime) {
        mStartTime = startTime;
        return this;
    }

    /**
     * @return End {@link LocalDateTime} of the window for the underlying aggregation
     */
    @NonNull
    public LocalDateTime getEndTime() {
        return mEndTime;
    }

    /** @hide */
    public AggregateRecordsGroupedByPeriodResponse<T> setEndTime(LocalDateTime endTime) {
        mEndTime = endTime;
        return this;
    }

    /**
     * @return An aggregation result for {@code aggregationType}, and null if one doesn't exist
     */
    @Nullable
    public T get(@NonNull AggregationType<T> aggregationType) {
        return AggregateRecordsResponse.getInternal(aggregationType, mResult);
    }

    /**
     * @return {@link ZoneOffset} for the underlying aggregation record, null if the corresponding
     *     aggregation doesn't exist or if multiple records were present.
     */
    @Nullable
    public ZoneOffset getZoneOffset(@NonNull AggregationType<T> aggregationType) {
        return AggregateRecordsResponse.getZoneOffsetInternal(aggregationType, mResult);
    }
}

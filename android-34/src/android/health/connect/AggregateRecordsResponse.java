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
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.internal.datatypes.utils.AggregationTypeIdMapper;
import android.util.ArrayMap;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** A class representing response for {@link HealthConnectManager#aggregate} */
public final class AggregateRecordsResponse<T> {
    private final Map<AggregationType<T>, AggregateResult<T>> mAggregateResults;

    /**
     * We only support querying and fetching same type of aggregations, so we can cast blindly
     *
     * @hide
     */
    @SuppressWarnings("unchecked")
    public AggregateRecordsResponse(@NonNull Map<Integer, AggregateResult<?>> aggregateResults) {
        Objects.requireNonNull(aggregateResults);

        mAggregateResults = new ArrayMap<>(aggregateResults.size());
        aggregateResults.forEach(
                (key, value) ->
                        mAggregateResults.put(
                                (AggregationType<T>)
                                        AggregationTypeIdMapper.getInstance()
                                                .getAggregationTypeFor(key),
                                (AggregateResult<T>) value));
    }

    /** @hide */
    public static <U> ZoneOffset getZoneOffsetInternal(
            @NonNull AggregationType<U> aggregationType,
            Map<AggregationType<U>, AggregateResult<U>> mAggregateResults) {
        Objects.requireNonNull(aggregationType);

        AggregateResult<U> result = mAggregateResults.get(aggregationType);

        if (result == null) {
            return null;
        }

        return result.getZoneOffset();
    }

    /** @hide */
    public static <U> Set<DataOrigin> getDataOriginsInternal(
            @NonNull AggregationType<U> aggregationType,
            Map<AggregationType<U>, AggregateResult<U>> mAggregateResults) {
        Objects.requireNonNull(aggregationType);

        AggregateResult<U> result = mAggregateResults.get(aggregationType);

        if (result == null) {
            return Collections.emptySet();
        }

        return result.getDataOrigins();
    }

    /** @hide */
    public static <U> U getInternal(
            @NonNull AggregationType<U> aggregationType,
            Map<AggregationType<U>, AggregateResult<U>> mAggregateResults) {
        Objects.requireNonNull(aggregationType);

        AggregateResult<U> result = mAggregateResults.get(aggregationType);

        if (result == null) {
            return null;
        }

        return result.getResult();
    }

    /**
     * @return a map of {@link AggregationType} -> {@link AggregateResult}
     * @hide
     */
    @NonNull
    public Map<Integer, AggregateResult<?>> getAggregateResults() {
        Map<Integer, AggregateResult<?>> aggregateResultMap = new ArrayMap<>();
        mAggregateResults.forEach(
                (key, value) -> {
                    aggregateResultMap.put(
                            AggregationTypeIdMapper.getInstance().getIdFor(key), value);
                });
        return aggregateResultMap;
    }

    /**
     * @return an aggregation result for {@code aggregationType}. *
     * @param aggregationType {@link AggregationType} for which to get the result
     */
    @Nullable
    public T get(@NonNull AggregationType<T> aggregationType) {
        return getInternal(aggregationType, mAggregateResults);
    }

    /**
     * @return {@link ZoneOffset} for the underlying aggregation record, null if the corresponding
     *     aggregation doesn't exist and or if multiple records were present.
     */
    @Nullable
    public ZoneOffset getZoneOffset(@NonNull AggregationType<T> aggregationType) {
        return getZoneOffsetInternal(aggregationType, mAggregateResults);
    }

    /**
     * Returns a set of {@link DataOrigin}s for the underlying aggregation record, empty set if the
     * corresponding aggregation doesn't exist and or if multiple records were present.
     */
    @NonNull
    public Set<DataOrigin> getDataOrigins(@NonNull AggregationType<T> aggregationType) {
        return getDataOriginsInternal(aggregationType, mAggregateResults);
    }
}

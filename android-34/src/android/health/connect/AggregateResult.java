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
import android.health.connect.datatypes.DataOrigin;
import android.os.Parcel;
import android.util.ArraySet;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A class to represent the results of {@link HealthConnectManager} aggregate APIs
 *
 * @hide
 */
public final class AggregateResult<T> {
    private final T mResult;
    private ZoneOffset mZoneOffset;
    private Set<DataOrigin> mDataOrigins;

    public AggregateResult(T result) {
        mResult = result;
    }

    public void putToParcel(@NonNull Parcel parcel) {
        if (mResult instanceof Long) {
            parcel.writeLong((Long) mResult);
        } else if (mResult instanceof Double) {
            parcel.writeDouble((Double) mResult);
        }
    }

    /**
     * @return {@link ZoneOffset} for the underlying record, null if aggregation was derived from
     *     multiple records
     */
    public ZoneOffset getZoneOffset() {
        return mZoneOffset;
    }

    /** Sets the {@link ZoneOffset} for the underlying record. */
    public AggregateResult<T> setZoneOffset(ZoneOffset zoneOffset) {
        mZoneOffset = zoneOffset;
        return this;
    }

    /** Returns set of {@link DataOrigin} that contributed to the aggregation result */
    @NonNull
    public Set<DataOrigin> getDataOrigins() {
        return mDataOrigins;
    }

    /** Sets a Set of {@link DataOrigin} that contributed to the aggregation result. */
    public AggregateResult<T> setDataOrigins(@NonNull List<String> packageNameList) {
        Objects.requireNonNull(packageNameList);

        mDataOrigins = new ArraySet<>();
        for (String packageName : packageNameList) {
            mDataOrigins.add(new DataOrigin.Builder().setPackageName(packageName).build());
        }
        return this;
    }

    /**
     * @return an Object representing the result of an aggregation.
     */
    @NonNull
    T getResult() {
        return mResult;
    }
}

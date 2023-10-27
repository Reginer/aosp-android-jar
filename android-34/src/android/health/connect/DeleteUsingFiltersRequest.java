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
import android.annotation.SystemApi;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Record;
import android.util.ArraySet;

import java.util.Objects;
import java.util.Set;

/**
 * A delete request based on record type and/or time and/or data origin. This is only for controller
 * APK to use
 *
 * @hide
 */
@SystemApi
public final class DeleteUsingFiltersRequest {
    private final TimeRangeFilter mTimeRangeFilter;
    private final Set<Class<? extends Record>> mRecordTypes;
    private final Set<DataOrigin> mDataOrigins;

    /**
     * @see Builder
     */
    private DeleteUsingFiltersRequest(
            @Nullable TimeRangeFilter timeRangeFilter,
            @NonNull Set<Class<? extends Record>> recordTypes,
            @NonNull Set<DataOrigin> dataOrigins) {
        Objects.requireNonNull(recordTypes);
        Objects.requireNonNull(dataOrigins);

        mTimeRangeFilter = timeRangeFilter;
        mRecordTypes = recordTypes;
        mDataOrigins = dataOrigins;
    }

    /** Returns record types on which delete is to be performed */
    @NonNull
    public Set<Class<? extends Record>> getRecordTypes() {
        return mRecordTypes;
    }

    /**
     * @return time range b/w which the delete operation is to be performed
     */
    @Nullable
    public TimeRangeFilter getTimeRangeFilter() {
        return mTimeRangeFilter;
    }

    /**
     * @return list of {@link DataOrigin}s to delete, or empty list for no filter
     */
    @NonNull
    public Set<DataOrigin> getDataOrigins() {
        return mDataOrigins;
    }

    /** Builder class for {@link DeleteUsingFiltersRequest} */
    public static final class Builder {
        private final Set<DataOrigin> mDataOrigins = new ArraySet<>();
        private final Set<Class<? extends Record>> mRecordTypes = new ArraySet<>();
        private TimeRangeFilter mTimeRangeFilter;

        /**
         * Sets {@link DataOrigin} to be deleted for this builder. If not set all the data origins
         * will be deleted
         *
         * @param dataOrigin {@link DataOrigin} to be deleted.
         * @return Same {@link Builder} with the dataOrigin field set
         */
        @NonNull
        public Builder addDataOrigin(@NonNull DataOrigin dataOrigin) {
            Objects.requireNonNull(dataOrigin);

            mDataOrigins.add(dataOrigin);
            return this;
        }

        /**
         * Clears all the {@link DataOrigin} set for this builder.
         *
         * @return {@link Builder}
         */
        @NonNull
        public Builder clearDataOrigins() {
            mDataOrigins.clear();
            return this;
        }

        /**
         * Sets {@link TimeRangeFilter} for this request. If not set, no time based filter will be
         * applied.
         *
         * @param timeRangeFilter Time range b/w which the delete operation is to be performed
         * @return Same {@link Builder} with the timeRangeFilter field set
         */
        @NonNull
        public Builder setTimeRangeFilter(@Nullable TimeRangeFilter timeRangeFilter) {
            mTimeRangeFilter = timeRangeFilter;
            return this;
        }

        /**
         * Adds {@link Record} type to be deleted. If not set, this request will apply to all the
         * records.
         *
         * @param recordType {@link Record} to be added to this filer.
         * @return Same {@link Builder} with the recordType field set
         */
        @NonNull
        public Builder addRecordType(@NonNull Class<? extends Record> recordType) {
            Objects.requireNonNull(recordType);

            mRecordTypes.add(recordType);
            return this;
        }

        /**
         * Clears all the {@link Record} set for this builder.
         *
         * @return {@link Builder}
         */
        @NonNull
        public Builder clearRecordTypes() {
            mRecordTypes.clear();
            return this;
        }

        /**
         * Creates {@link DeleteUsingFiltersRequest} object.
         *
         * @return Object of {@link DeleteUsingFiltersRequest}
         */
        @NonNull
        public DeleteUsingFiltersRequest build() {
            return new DeleteUsingFiltersRequest(mTimeRangeFilter, mRecordTypes, mDataOrigins);
        }
    }
}

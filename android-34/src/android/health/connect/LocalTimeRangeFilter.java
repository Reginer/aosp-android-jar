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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/** Specification of local time range for health connect requests. */
public final class LocalTimeRangeFilter implements TimeRangeFilter {
    private final LocalDateTime mLocalStartTime;
    private final LocalDateTime mLocalEndTime;

    /**
     * @param localStartTime represents local start time of this filter. If the value is null, the
     *     Instant.Epoch with min zoneOffset is set as default value.
     * @param localEndTime represents local end time of this filter. If the value is null,
     *     Instant.now() + 1 day with max zoneOffset is set as default value.
     * @hide
     */
    private LocalTimeRangeFilter(
            @Nullable LocalDateTime localStartTime, @Nullable LocalDateTime localEndTime) {
        if (localStartTime == null && localEndTime == null) {
            throw new IllegalArgumentException("Both start time and end time cannot be null.");
        }
        if (localStartTime != null && localEndTime != null) {
            if (!localEndTime.isAfter(localStartTime)) {
                throw new IllegalArgumentException("end time needs to be after start time.");
            }
        }

        mLocalStartTime =
                localStartTime != null
                        ? localStartTime
                        : LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.MIN);
        mLocalEndTime =
                localEndTime != null
                        ? localEndTime
                        : LocalDateTime.ofInstant(
                                Instant.now().plus(1, ChronoUnit.DAYS), ZoneOffset.MAX);
    }

    /**
     * @return local start time of this filter
     */
    @Nullable
    public LocalDateTime getStartTime() {
        return mLocalStartTime;
    }

    /**
     * @return local end time of this filter
     */
    @Nullable
    public LocalDateTime getEndTime() {
        return mLocalEndTime;
    }

    /**
     * @return a boolean value indicating if the filter is bound or not.
     */
    @NonNull
    public boolean isBounded() {
        return mLocalStartTime != null && mLocalEndTime != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o.getClass() != this.getClass()) return false;

        if (!mLocalStartTime.equals(((LocalTimeRangeFilter) o).mLocalStartTime)) return false;
        if (!mLocalEndTime.equals(((LocalTimeRangeFilter) o).mLocalEndTime)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (mLocalStartTime == null ? 0 : mLocalStartTime.hashCode());
        result = 31 * result + (mLocalEndTime == null ? 0 : mLocalEndTime.hashCode());
        return result;
    }

    /** Builder class for {@link LocalTimeRangeFilter} */
    public static final class Builder {
        private LocalDateTime mLocalStartTime;
        private LocalDateTime mLocalEndTime;

        /**
         * @param localStartTime represents local start time of this filter
         */
        @NonNull
        public LocalTimeRangeFilter.Builder setStartTime(@Nullable LocalDateTime localStartTime) {
            mLocalStartTime = localStartTime;
            return this;
        }

        /**
         * @param localEndTime represents local end time of this filter
         */
        @NonNull
        public LocalTimeRangeFilter.Builder setEndTime(@Nullable LocalDateTime localEndTime) {
            mLocalEndTime = localEndTime;
            return this;
        }

        /** Builds {@link TimeRangeFilter} */
        @NonNull
        public LocalTimeRangeFilter build() {
            return new LocalTimeRangeFilter(mLocalStartTime, mLocalEndTime);
        }
    }
}

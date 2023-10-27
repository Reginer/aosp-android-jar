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
import java.time.temporal.ChronoUnit;

/**
 * Specification of time range for read and delete requests. Internally represents a SQLLite
 * argument that specifies start and end time to put in as SQLLite parameters. The filter must be
 * bound at least at one of the ends, i.e., either the start or end time must be set, or else an
 * IllegalArgumentException is thrown.
 */
public final class TimeInstantRangeFilter implements TimeRangeFilter {

    private final Instant mStartTime;
    private final Instant mEndTime;

    /**
     * @param startTime represents start time of this filter. If the value is null, Instant.Epoch is
     *     set as default value.
     * @param endTime represents end time of this filter If the value is null, Instant.now() + 1 day
     *     is set as default value.
     * @hide
     */
    private TimeInstantRangeFilter(@Nullable Instant startTime, @Nullable Instant endTime) {
        if (startTime == null && endTime == null) {
            throw new IllegalArgumentException("Both start time and end time cannot be null.");
        }
        if (startTime != null && endTime != null) {
            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("end time needs to be after start time.");
            }
        }
        mStartTime = startTime != null ? startTime : Instant.EPOCH;
        mEndTime = endTime != null ? endTime : Instant.now().plus(1, ChronoUnit.DAYS);
    }

    /**
     * @return start time instant of this filter
     */
    @Nullable
    public Instant getStartTime() {
        return mStartTime;
    }

    /**
     * @return end time instant of this filter
     */
    @Nullable
    public Instant getEndTime() {
        return mEndTime;
    }

    /**
     * @return a boolean value indicating if the filter is bound or not.
     */
    @NonNull
    public boolean isBounded() {
        return mStartTime != null && mEndTime != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o.getClass() != this.getClass()) return false;

        if (!mStartTime.equals(((TimeInstantRangeFilter) o).mStartTime)) return false;
        if (!mEndTime.equals(((TimeInstantRangeFilter) o).mEndTime)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (mStartTime == null ? 0 : mStartTime.hashCode());
        result = 31 * result + (mEndTime == null ? 0 : mEndTime.hashCode());
        return result;
    }

    /** Builder class for {@link TimeInstantRangeFilter} */
    public static final class Builder {
        private Instant mStartTime;
        private Instant mEndTime;

        /**
         * @param startTime represents start time of this filter
         */
        @NonNull
        public Builder setStartTime(@Nullable Instant startTime) {
            mStartTime = startTime;
            return this;
        }

        /**
         * @param endTime end time of this filter
         */
        @NonNull
        public Builder setEndTime(@Nullable Instant endTime) {
            mEndTime = endTime;
            return this;
        }

        /** Builds {@link TimeInstantRangeFilter} */
        @NonNull
        public TimeInstantRangeFilter build() {
            return new TimeInstantRangeFilter(mStartTime, mEndTime);
        }
    }
}

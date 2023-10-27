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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * A helper class for {@link TimeRangeFilter} to handle possible time filter types.
 *
 * @hide
 */
public final class TimeRangeFilterHelper {

    private static final ZoneOffset LOCAL_TIME_ZERO_OFFSET = ZoneOffset.UTC;

    public static boolean isLocalTimeFilter(@NonNull TimeRangeFilter timeRangeFilter) {
        return (timeRangeFilter instanceof LocalTimeRangeFilter);
    }

    /**
     * @return start time epoch milliseconds for Instant time filter and epoch milliseconds using
     *     system zoneOffset for LocalTime filter
     */
    public static long getFilterStartTimeMillis(@NonNull TimeRangeFilter timeRangeFilter) {
        if (isLocalTimeFilter(timeRangeFilter)) {
            return getMillisOfLocalTime(((LocalTimeRangeFilter) timeRangeFilter).getStartTime());
        } else if (timeRangeFilter instanceof TimeInstantRangeFilter) {
            return ((TimeInstantRangeFilter) timeRangeFilter).getStartTime().toEpochMilli();
        } else {
            throw new IllegalArgumentException(
                    "Invalid time filter object. Object should be either "
                            + "TimeInstantRangeFilter or LocalTimeRangeFilter.");
        }
    }

    /**
     * @return end time epoch milliseconds for Instant time filter and epoch milliseconds using
     *     system zoneOffset for LocalTime filter
     */
    public static long getFilterEndTimeMillis(@NonNull TimeRangeFilter timeRangeFilter) {
        if (isLocalTimeFilter(timeRangeFilter)) {
            return getMillisOfLocalTime(((LocalTimeRangeFilter) timeRangeFilter).getEndTime());
        } else if (timeRangeFilter instanceof TimeInstantRangeFilter) {
            return ((TimeInstantRangeFilter) timeRangeFilter).getEndTime().toEpochMilli();
        } else {
            throw new IllegalArgumentException(
                    "Invalid time filter object. Object should be either "
                            + "TimeInstantRangeFilter or LocalTimeRangeFilter.");
        }
    }

    public static LocalDateTime getLocalTimeFromMillis(Long localDateTimeMillis) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(localDateTimeMillis), LOCAL_TIME_ZERO_OFFSET);
    }

    public static long getMillisOfLocalTime(LocalDateTime time) {
        return time.toInstant(LOCAL_TIME_ZERO_OFFSET).toEpochMilli();
    }
}

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

package android.health.connect.datatypes.validation;

import android.health.connect.datatypes.TimeInterval;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * An util class for HC datatype validation
 *
 * @hide
 */
public final class ValidationUtils {
    public static final String INTDEF_VALIDATION_ERROR_PREFIX = "Unknown Intdef value";

    /** Requires long value to be within the range. */
    public static void requireInRange(long value, long lowerBound, long upperBound, String name) {
        if (value < lowerBound) {
            throw new IllegalArgumentException(
                    name + " must not be less than " + lowerBound + ", currently " + value);
        }

        if (value > upperBound) {
            throw new IllegalArgumentException(
                    name + " must not be more than " + upperBound + ", currently " + value);
        }
    }

    /** Requires double value to be non negative. */
    public static void requireNonNegative(double value, String name) {
        if (value < 0.0) {
            throw new IllegalArgumentException(name + "must be non-negative, currently " + value);
        }
    }

    /** Requires double value to be within the range. */
    public static void requireInRange(
            double value, double lowerBound, double upperBound, String name) {
        if (value < lowerBound) {
            throw new IllegalArgumentException(
                    name + " must not be less than " + lowerBound + ", currently " + value);
        }

        if (value > upperBound) {
            throw new IllegalArgumentException(
                    name + " must not be more than " + upperBound + ", currently " + value);
        }
    }

    /** Requires an integer value to be among the set of allowed values. */
    public static void validateIntDefValue(int value, Set<Integer> allowedValues, String name) {
        if (!allowedValues.contains(value)) {
            throw new IllegalArgumentException(
                    INTDEF_VALIDATION_ERROR_PREFIX + ": " + value + " for Intdef: " + name + ".");
        }
    }

    /** Requires list of times to be within the range. */
    public static void validateSampleStartAndEndTime(
            Instant sessionStartTime, Instant sessionEndTime, List<Instant> timeInstants) {
        if (timeInstants.size() > 0) {
            Instant minTime = timeInstants.get(0);
            Instant maxTime = timeInstants.get(0);
            for (Instant instant : timeInstants) {
                if (instant.isBefore(minTime)) {
                    minTime = instant;
                }
                if (instant.isAfter(maxTime)) {
                    maxTime = instant;
                }
            }
            if (minTime.isBefore(sessionStartTime) || maxTime.isAfter(sessionEndTime)) {
                throw new IllegalArgumentException(
                        "Time instant values must be within session interval");
            }
        }
    }

    /** Requires comparable class to be within the range. */
    public static <T extends Comparable<T>> void requireInRangeIfExists(
            Comparable<T> value, T threshold, T limit, String name) {
        if (value != null && value.compareTo(threshold) < 0) {
            throw new IllegalArgumentException(
                    name + " must not be less than " + threshold + ", currently " + value);
        }

        if (value != null && value.compareTo(limit) > 0) {
            throw new IllegalArgumentException(
                    name + " must not be more than " + limit + ", currently " + value);
        }
    }

    /**
     * Sorts time interval holders by time intervals. Validates that time intervals do not overlap
     * and within parent start and end times.
     */
    public static List<? extends TimeInterval.TimeIntervalHolder>
            sortAndValidateTimeIntervalHolders(
                    Instant parentStartTime,
                    Instant parentEndTime,
                    List<? extends TimeInterval.TimeIntervalHolder> intervalHolders) {
        if (intervalHolders.isEmpty()) {
            return intervalHolders;
        }

        String intervalsName = intervalHolders.get(0).getClass().getSimpleName();
        intervalHolders.sort(Comparator.comparing(TimeInterval.TimeIntervalHolder::getInterval));
        TimeInterval currentInterval, previousInterval;
        for (int i = 0; i < intervalHolders.size(); i++) {
            currentInterval = intervalHolders.get(i).getInterval();
            if (currentInterval.getStartTime().isBefore(parentStartTime)
                    || currentInterval.getEndTime().isAfter(parentEndTime)) {
                throw new IllegalArgumentException(
                        intervalsName
                                + ": time intervals must be within parent session time interval.");
            }

            if (i != 0) {
                previousInterval = intervalHolders.get(i - 1).getInterval();
                if (previousInterval.getEndTime().isAfter(currentInterval.getStartTime())) {
                    throw new IllegalArgumentException(
                            intervalsName + ": time intervals must not overlap.");
                }
            }
        }
        return intervalHolders;
    }
}

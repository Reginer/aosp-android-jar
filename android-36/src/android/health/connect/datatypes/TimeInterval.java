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

package android.health.connect.datatypes;

import android.annotation.NonNull;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents time interval.
 *
 * <p>Each object has start time and end time. End time must be after start time.
 *
 * @hide
 */
public final class TimeInterval implements Comparable<TimeInterval> {

    @Override
    public int compareTo(TimeInterval interval) {
        if (getStartTime().compareTo(interval.getStartTime()) != 0) {
            return getStartTime().compareTo(interval.getStartTime());
        }
        return getEndTime().compareTo(interval.getEndTime());
    }

    /**
     * Interface of the class which holds TimeInterval
     *
     * @hide
     */
    public interface TimeIntervalHolder {
        /** Returns time interval. */
        TimeInterval getInterval();
    }

    private final Instant mStartTime;
    private final Instant mEndTime;

    public TimeInterval(@NonNull Instant startTime, @NonNull Instant endTime) {
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        mStartTime = startTime;
        mEndTime = endTime;
    }

    /*
     * Returns start time of the interval.
     */
    @NonNull
    public Instant getStartTime() {
        return mStartTime;
    }

    /*
     * Returns end time of the interval.
     */
    @NonNull
    public Instant getEndTime() {
        return mEndTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeInterval)) return false;
        TimeInterval that = (TimeInterval) o;
        return getStartTime().toEpochMilli() == that.getStartTime().toEpochMilli()
                && getEndTime().toEpochMilli() == that.getEndTime().toEpochMilli();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStartTime(), getEndTime());
    }
}

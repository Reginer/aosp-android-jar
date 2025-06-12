/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.internal.telephony;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.os.SystemClock;
import android.util.LongArrayQueue;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Tracks whether an event occurs mNumOccurrences times within the time span mWindowSizeMillis.
 * <p/>
 * Stores all events in memory, use a small number of required occurrences when using.
 */
public class SlidingWindowEventCounter {
    private final long mWindowSizeMillis;
    private final int mNumOccurrences;
    private final LongArrayQueue mTimestampQueueMillis;

    public SlidingWindowEventCounter(@IntRange(from = 0) final long windowSizeMillis,
            @IntRange(from = 2) final int numOccurrences) {
        if (windowSizeMillis < 0) {
            throw new IllegalArgumentException("windowSizeMillis must be greater or equal to 0");
        }
        if (numOccurrences <= 1) {
            throw new IllegalArgumentException("numOccurrences must be greater than 1");
        }

        mWindowSizeMillis = windowSizeMillis;
        mNumOccurrences = numOccurrences;
        mTimestampQueueMillis = new LongArrayQueue(numOccurrences);
    }

    /**
     * Increments the occurrence counter.
     *
     * Returns true if an event has occurred at least mNumOccurrences times within the
     * time span mWindowSizeMillis.
     */
    public synchronized boolean addOccurrence() {
        return addOccurrence(SystemClock.elapsedRealtime());
    }

    /**
     * Increments the occurrence counter.
     *
     * Returns true if an event has occurred at least mNumOccurrences times within the
     * time span mWindowSizeMillis.
     * @param timestampMillis
     */
    public synchronized boolean addOccurrence(long timestampMillis) {
        mTimestampQueueMillis.addLast(timestampMillis);
        if (mTimestampQueueMillis.size() > mNumOccurrences) {
            mTimestampQueueMillis.removeFirst();
        }
        return isInWindow();
    }

    /**
     * Returns true if the conditions is satisfied.
     */
    public synchronized boolean isInWindow() {
        return (mTimestampQueueMillis.size() == mNumOccurrences)
                && mTimestampQueueMillis.peekFirst()
                + mWindowSizeMillis > mTimestampQueueMillis.peekLast();
    }

    @VisibleForTesting
    int getQueuedNumOccurrences() {
        return mTimestampQueueMillis.size();
    }

    /**
     * @return the time span in ms of the sliding window.
     */
    public synchronized long getWindowSizeMillis() {
        return mWindowSizeMillis;
    }

    /**
     * @return the least number of occurrences for {@link #isInWindow} to be true.
     */
    public synchronized int getNumOccurrences() {
        return mNumOccurrences;
    }

    /**
     * Get the event frequency description.
     *
     * @return A string describing the anomaly event
     */
    public @NonNull String getFrequencyString() {
        if (mWindowSizeMillis >= 1000L) {
            return mNumOccurrences + " times within " + mWindowSizeMillis / 1000L + " seconds";
        } else {
            return mNumOccurrences + " times within " + mWindowSizeMillis + "ms";
        }
    }

    @Override
    public String toString() {
        return String.format("SlidingWindowEventCounter=[windowSizeMillis=" + mWindowSizeMillis
                + ", numOccurrences=" + mNumOccurrences
                + ", timestampQueueMillis=" + mTimestampQueueMillis + "]");
    }
}

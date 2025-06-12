/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.net.module.util;

import android.util.SparseIntArray;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Keeps track of the counters under different uid, fire exception if the counter
 * exceeded the specified maximum value.
 *
 * @hide
 */
public class PerUidCounter {
    private final int mMaxCountPerUid;

    // Map from UID to count that UID has filed.
    @VisibleForTesting
    @GuardedBy("this")
    final SparseIntArray mUidToCount = new SparseIntArray();

    /**
     * Constructor
     *
     * @param maxCountPerUid the maximum count per uid allowed
     */
    public PerUidCounter(final int maxCountPerUid) {
        if (maxCountPerUid <= 0) {
            throw new IllegalArgumentException("Maximum counter value must be positive");
        }
        mMaxCountPerUid = maxCountPerUid;
    }

    /**
     * Increments the count of the given uid.  Throws an exception if the number
     * of the counter for the uid exceeds the value of maxCounterPerUid which is the value
     * passed into the constructor. see: {@link #PerUidCounter(int)}.
     *
     * @throws IllegalStateException if the number of counter for the uid exceed
     *         the allowed number.
     *
     * @param uid the uid that the counter was made under
     */
    public synchronized void incrementCountOrThrow(final int uid) {
        final long newCount = ((long) mUidToCount.get(uid, 0)) + 1;
        if (newCount > mMaxCountPerUid) {
            throw new IllegalStateException("Uid " + uid + " exceeded its allowed limit");
        }
        // Since the count cannot be greater than Integer.MAX_VALUE here since mMaxCountPerUid
        // is an integer, it is safe to cast to int.
        mUidToCount.put(uid, (int) newCount);
    }

    /**
     * Decrements the count of the given uid. Throws an exception if the number
     * of the counter goes below zero.
     *
     * @throws IllegalStateException if the number of counter for the uid goes below
     *         zero.
     *
     * @param uid the uid that the count was made under
     */
    public synchronized void decrementCountOrThrow(final int uid) {
        final int newCount = mUidToCount.get(uid, 0) - 1;
        if (newCount < 0) {
            throw new IllegalStateException("BUG: too small count " + newCount + " for UID " + uid);
        } else if (newCount == 0) {
            mUidToCount.delete(uid);
        } else {
            mUidToCount.put(uid, newCount);
        }
    }

    /**
     * Get the current counter value for the given uid.
     */
    public synchronized int get(int uid) {
        return mUidToCount.get(uid, 0);
    }
}

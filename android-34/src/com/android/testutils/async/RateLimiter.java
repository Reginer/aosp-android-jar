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

package com.android.testutils.async;

import com.android.net.module.util.async.OsAccess;

import java.util.Arrays;

/**
 * Limits the number of bytes processed to the given maximum of bytes per second.
 *
 * The limiter tracks the total for the past second, along with sums for each 10ms
 * in the past second, allowing the total to be adjusted as the time passes.
 */
public final class RateLimiter {
    private static final int PERIOD_DURATION_MS = 1000;
    private static final int BUCKET_COUNT = 100;

    public static final int BUCKET_DURATION_MS = PERIOD_DURATION_MS / BUCKET_COUNT;

    private final OsAccess mOsAccess;
    private final int[] mStatBuckets = new int[BUCKET_COUNT];
    private int mMaxPerPeriodBytes;
    private int mMaxPerBucketBytes;
    private int mRecordedPeriodBytes;
    private long mLastLimitTimestamp;
    private int mLastRequestReduction;

    public RateLimiter(OsAccess osAccess, int bytesPerSecond) {
        mOsAccess = osAccess;
        setBytesPerSecond(bytesPerSecond);
        clear();
    }

    public int getBytesPerSecond() {
        return mMaxPerPeriodBytes;
    }

    public void setBytesPerSecond(int bytesPerSecond) {
        mMaxPerPeriodBytes = bytesPerSecond;
        mMaxPerBucketBytes = Math.max(1, (mMaxPerPeriodBytes / BUCKET_COUNT) * 2);
    }

    public void clear() {
        mLastLimitTimestamp = mOsAccess.monotonicTimeMillis();
        mRecordedPeriodBytes = 0;
        Arrays.fill(mStatBuckets, 0);
    }

    public static int limit(RateLimiter limiter1, RateLimiter limiter2, int requestedBytes) {
        final long now = limiter1.mOsAccess.monotonicTimeMillis();
        final int allowedCount = Math.min(limiter1.calculateLimit(now, requestedBytes),
            limiter2.calculateLimit(now, requestedBytes));
        limiter1.recordBytes(now, requestedBytes, allowedCount);
        limiter2.recordBytes(now, requestedBytes, allowedCount);
        return allowedCount;
    }

    public int limit(int requestedBytes) {
        final long now = mOsAccess.monotonicTimeMillis();
        final int allowedCount = calculateLimit(now, requestedBytes);
        recordBytes(now, requestedBytes, allowedCount);
        return allowedCount;
    }

    public int getLastRequestReduction() {
        return mLastRequestReduction;
    }

    public boolean acceptAllOrNone(int requestedBytes) {
        final long now = mOsAccess.monotonicTimeMillis();
        final int allowedCount = calculateLimit(now, requestedBytes);
        if (allowedCount < requestedBytes) {
            return false;
        }
        recordBytes(now, requestedBytes, allowedCount);
        return true;
    }

    private int calculateLimit(long now, int requestedBytes) {
        // First remove all stale bucket data and adjust the total.
        final long currentBucketAbsIdx = now / BUCKET_DURATION_MS;
        final long staleCutoffIdx = currentBucketAbsIdx - BUCKET_COUNT;
        for (long i = mLastLimitTimestamp / BUCKET_DURATION_MS; i < staleCutoffIdx; i++) {
            final int idx = (int) (i % BUCKET_COUNT);
            mRecordedPeriodBytes -= mStatBuckets[idx];
            mStatBuckets[idx] = 0;
        }

        final int bucketIdx = (int) (currentBucketAbsIdx % BUCKET_COUNT);
        final int maxAllowed = Math.min(mMaxPerPeriodBytes - mRecordedPeriodBytes,
            Math.min(mMaxPerBucketBytes - mStatBuckets[bucketIdx], requestedBytes));
        return Math.max(0, maxAllowed);
    }

    private void recordBytes(long now, int requestedBytes, int actualBytes) {
        mStatBuckets[(int) ((now / BUCKET_DURATION_MS) % BUCKET_COUNT)] += actualBytes;
        mRecordedPeriodBytes += actualBytes;
        mLastRequestReduction = requestedBytes - actualBytes;
        mLastLimitTimestamp = now;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{max=");
        sb.append(mMaxPerPeriodBytes);
        sb.append(",max_bucket=");
        sb.append(mMaxPerBucketBytes);
        sb.append(",total=");
        sb.append(mRecordedPeriodBytes);
        sb.append(",last_red=");
        sb.append(mLastRequestReduction);
        sb.append('}');
        return sb.toString();
    }
}

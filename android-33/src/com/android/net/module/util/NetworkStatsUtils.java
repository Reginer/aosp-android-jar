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

package com.android.net.module.util;

import android.app.usage.NetworkStats;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Various utilities used for NetworkStats related code.
 *
 * @hide
 */
public class NetworkStatsUtils {
    // These constants must be synced with the definition in android.net.NetworkStats.
    // TODO: update to formal APIs once all downstreams have these APIs.
    private static final int SET_ALL = -1;
    private static final int METERED_ALL = -1;
    private static final int ROAMING_ALL = -1;
    private static final int DEFAULT_NETWORK_ALL = -1;

    /**
     * Safely multiple a value by a rational.
     * <p>
     * Internally it uses integer-based math whenever possible, but switches
     * over to double-based math if values would overflow.
     * @hide
     */
    public static long multiplySafeByRational(long value, long num, long den) {
        if (den == 0) {
            throw new ArithmeticException("Invalid Denominator");
        }
        long x = value;
        long y = num;

        // Logic shamelessly borrowed from Math.multiplyExact()
        long r = x * y;
        long ax = Math.abs(x);
        long ay = Math.abs(y);
        if (((ax | ay) >>> 31 != 0)) {
            // Some bits greater than 2^31 that might cause overflow
            // Check the result using the divide operator
            // and check for the special case of Long.MIN_VALUE * -1
            if (((y != 0) && (r / y != x))
                    || (x == Long.MIN_VALUE && y == -1)) {
                // Use double math to avoid overflowing
                return (long) (((double) num / den) * value);
            }
        }
        return r / den;
    }

    /**
     * Value of the match rule of the subscriberId to match networks with specific subscriberId.
     *
     * @hide
     */
    public static final int SUBSCRIBER_ID_MATCH_RULE_EXACT = 0;
    /**
     * Value of the match rule of the subscriberId to match networks with any subscriberId which
     * includes null and non-null.
     *
     * @hide
     */
    public static final int SUBSCRIBER_ID_MATCH_RULE_ALL = 1;

    /**
     * Name representing {@link #bandwidthSetGlobalAlert(long)} limit when delivered to
     * {@link AlertObserver#onQuotaLimitReached(String, String)}.
     */
    public static final String LIMIT_GLOBAL_ALERT = "globalAlert";

    /**
     * Return the constrained value by given the lower and upper bounds.
     */
    public static int constrain(int amount, int low, int high) {
        if (low > high) throw new IllegalArgumentException("low(" + low + ") > high(" + high + ")");
        return amount < low ? low : (amount > high ? high : amount);
    }

    /**
     * Return the constrained value by given the lower and upper bounds.
     */
    public static long constrain(long amount, long low, long high) {
        if (low > high) throw new IllegalArgumentException("low(" + low + ") > high(" + high + ")");
        return amount < low ? low : (amount > high ? high : amount);
    }

    /**
     * Convert structure from android.app.usage.NetworkStats to android.net.NetworkStats.
     */
    public static android.net.NetworkStats fromPublicNetworkStats(
            NetworkStats publiceNetworkStats) {
        android.net.NetworkStats stats = new android.net.NetworkStats(0L, 0);
        while (publiceNetworkStats.hasNextBucket()) {
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            publiceNetworkStats.getNextBucket(bucket);
            final android.net.NetworkStats.Entry entry = fromBucket(bucket);
            stats = stats.addEntry(entry);
        }
        return stats;
    }

    @VisibleForTesting
    public static android.net.NetworkStats.Entry fromBucket(NetworkStats.Bucket bucket) {
        return new android.net.NetworkStats.Entry(
                null /* IFACE_ALL */, bucket.getUid(), convertBucketState(bucket.getState()),
                convertBucketTag(bucket.getTag()), convertBucketMetered(bucket.getMetered()),
                convertBucketRoaming(bucket.getRoaming()),
                convertBucketDefaultNetworkStatus(bucket.getDefaultNetworkStatus()),
                bucket.getRxBytes(), bucket.getRxPackets(),
                bucket.getTxBytes(), bucket.getTxPackets(), 0 /* operations */);
    }

    private static int convertBucketState(int networkStatsSet) {
        switch (networkStatsSet) {
            case NetworkStats.Bucket.STATE_ALL: return SET_ALL;
            case NetworkStats.Bucket.STATE_DEFAULT: return android.net.NetworkStats.SET_DEFAULT;
            case NetworkStats.Bucket.STATE_FOREGROUND:
                return android.net.NetworkStats.SET_FOREGROUND;
        }
        return 0;
    }

    private static int convertBucketTag(int tag) {
        switch (tag) {
            case NetworkStats.Bucket.TAG_NONE: return android.net.NetworkStats.TAG_NONE;
        }
        return tag;
    }

    private static int convertBucketMetered(int metered) {
        switch (metered) {
            case NetworkStats.Bucket.METERED_ALL: return METERED_ALL;
            case NetworkStats.Bucket.METERED_NO: return android.net.NetworkStats.METERED_NO;
            case NetworkStats.Bucket.METERED_YES: return android.net.NetworkStats.METERED_YES;
        }
        return 0;
    }

    private static int convertBucketRoaming(int roaming) {
        switch (roaming) {
            case NetworkStats.Bucket.ROAMING_ALL: return ROAMING_ALL;
            case NetworkStats.Bucket.ROAMING_NO: return android.net.NetworkStats.ROAMING_NO;
            case NetworkStats.Bucket.ROAMING_YES: return android.net.NetworkStats.ROAMING_YES;
        }
        return 0;
    }

    private static int convertBucketDefaultNetworkStatus(int defaultNetworkStatus) {
        switch (defaultNetworkStatus) {
            case NetworkStats.Bucket.DEFAULT_NETWORK_ALL:
                return DEFAULT_NETWORK_ALL;
            case NetworkStats.Bucket.DEFAULT_NETWORK_NO:
                return android.net.NetworkStats.DEFAULT_NETWORK_NO;
            case NetworkStats.Bucket.DEFAULT_NETWORK_YES:
                return android.net.NetworkStats.DEFAULT_NETWORK_YES;
        }
        return 0;
    }
}

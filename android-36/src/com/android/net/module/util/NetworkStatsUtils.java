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

/**
 * Various utilities used for NetworkStats related code.
 *
 * @hide
 */
public class NetworkStatsUtils {
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
}

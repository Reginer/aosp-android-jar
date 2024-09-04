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

package android.health.connect.ratelimiter;

import android.annotation.Nullable;
import android.health.connect.HealthConnectException;

/** @hide */
public class RateLimiterException extends HealthConnectException {
    @RateLimiter.QuotaBucket.Type private final int mQuotaBucket;
    private final float mQuotaLimit;

    /**
     * @param message The detailed error message.
     * @param quotaBucket The rate limit bucket in which quota is exceeded.
     * @param quotaLimit Api call limit for the bucket.
     * @hide
     */
    public RateLimiterException(
            @Nullable String message,
            @RateLimiter.QuotaBucket.Type int quotaBucket,
            float quotaLimit) {
        super(HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED, message);
        mQuotaBucket = quotaBucket;
        mQuotaLimit = quotaLimit;
    }

    /**
     * Returns rateLimiterQuotaBucket in which the quota is exceeded. Default of value zero
     * represents an undefined quota bucket.
     */
    public int getRateLimiterQuotaBucket() {
        return mQuotaBucket;
    }

    /** Returns rateLimiter API call limit for the bucket. */
    public float getRateLimiterQuotaLimit() {
        return mQuotaLimit;
    }
}

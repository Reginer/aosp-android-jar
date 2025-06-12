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

/**
 * Object to store the last update time and remaining quota for quota buckets as used in {@link
 * RateLimiter}.
 *
 * @hide
 */
public final class Quota {
    private long mLastUpdatedTimeMillis;
    private float mRemainingQuota;

    public Quota(long lastUpdatedTimeMillis, float remainingQuota) {
        mLastUpdatedTimeMillis = lastUpdatedTimeMillis;
        mRemainingQuota = remainingQuota;
    }

    public long getLastUpdatedTimeMillis() {
        return mLastUpdatedTimeMillis;
    }

    public float getRemainingQuota() {
        return mRemainingQuota;
    }

    public void setLastUpdatedTimeMillis(long lastUpdatedTimeMillis) {
        mLastUpdatedTimeMillis = lastUpdatedTimeMillis;
    }

    public void setRemainingQuota(Float remainingQuota) {
        mRemainingQuota = remainingQuota;
    }
}

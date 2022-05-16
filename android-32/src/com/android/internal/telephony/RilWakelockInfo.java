/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.annotation.TargetApi;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

@TargetApi(8)
public class RilWakelockInfo {
    private final String LOG_TAG = RilWakelockInfo.class.getSimpleName();
    private int mRilRequestSent;
    private int mTokenNumber;
    private long mRequestTime;
    private long mResponseTime;

    /* If there are n requests waiting for a response for time t, the time attributed to
    each request will be t/n. If the number of outstanding requests changes at time t1,
    then we will compute the wakelock time till t1 and store it in mWakelockTimeAttributedSoFar
    and update mConcurrentRequests. mLastAggregatedTime will be set to t1 and used to
    compute the time taken for this request using the new mConcurrentRequests
     */
    private long mWakelockTimeAttributedSoFar;
    private long mLastAggregatedTime;
    private int mConcurrentRequests;

    @VisibleForTesting
    public int getConcurrentRequests() {
        return mConcurrentRequests;
    }

    RilWakelockInfo(int rilRequest, int tokenNumber, int concurrentRequests, long requestTime) {
        concurrentRequests = validateConcurrentRequests(concurrentRequests);
        this.mRilRequestSent = rilRequest;
        this.mTokenNumber = tokenNumber;
        this.mConcurrentRequests = concurrentRequests;
        this.mRequestTime = requestTime;
        this.mWakelockTimeAttributedSoFar = 0;
        this.mLastAggregatedTime = requestTime;
    }

    private int validateConcurrentRequests(int concurrentRequests) {
        if(concurrentRequests <= 0) {
            if (TelephonyUtils.IS_DEBUGGABLE) {
                IllegalArgumentException e = new IllegalArgumentException(
                    "concurrentRequests should always be greater than 0.");
                Rlog.e(LOG_TAG, e.toString());
                throw e;
            } else {
                concurrentRequests = 1;
            }
        }
        return concurrentRequests;
    }

    int getTokenNumber() {
        return mTokenNumber;
    }

    int getRilRequestSent() {
        return mRilRequestSent;
    }

    void setResponseTime(long responseTime) {
        updateTime(responseTime);
        this.mResponseTime = responseTime;
    }

    void updateConcurrentRequests(int concurrentRequests, long time) {
        concurrentRequests = validateConcurrentRequests(concurrentRequests);
        updateTime(time);
        mConcurrentRequests = concurrentRequests;
    }

    synchronized void updateTime(long time) {
        mWakelockTimeAttributedSoFar += (time - mLastAggregatedTime) / mConcurrentRequests;
        mLastAggregatedTime = time;
    }

    long getWakelockTimeAttributedToClient() {
        return mWakelockTimeAttributedSoFar;
    }

    @Override
    public String toString() {
        return "WakelockInfo{" +
                "rilRequestSent=" + mRilRequestSent +
                ", tokenNumber=" + mTokenNumber +
                ", requestTime=" + mRequestTime +
                ", responseTime=" + mResponseTime +
                ", mWakelockTimeAttributed=" + mWakelockTimeAttributedSoFar +
                '}';
    }
}

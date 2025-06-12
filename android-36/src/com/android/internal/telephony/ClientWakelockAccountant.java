/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.telephony.ClientRequestStats;

import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.util.ArrayList;

public class ClientWakelockAccountant {
    public static final String LOG_TAG = "ClientWakelockAccountant: ";

    @VisibleForTesting
    public ClientRequestStats mRequestStats = new ClientRequestStats();
    @VisibleForTesting
    public ArrayList<RilWakelockInfo> mPendingRilWakelocks = new ArrayList<>();

    @VisibleForTesting
    public ClientWakelockAccountant(String callingPackage) {
        mRequestStats.setCallingPackage(callingPackage);
    }

    @VisibleForTesting
    public void startAttributingWakelock(int request,
            int token, int concurrentRequests, long time) {

        RilWakelockInfo wlInfo = new RilWakelockInfo(request, token, concurrentRequests, time);
        synchronized (mPendingRilWakelocks) {
            mPendingRilWakelocks.add(wlInfo);
        }
    }

    @VisibleForTesting
    public void stopAttributingWakelock(int request, int token, long time) {
        RilWakelockInfo wlInfo = removePendingWakelock(request, token);
        if (wlInfo != null) {
            completeRequest(wlInfo, time);
        }
    }

    @VisibleForTesting
    public void stopAllPendingRequests(long time) {
        synchronized (mPendingRilWakelocks) {
            for (RilWakelockInfo wlInfo : mPendingRilWakelocks) {
                completeRequest(wlInfo, time);
            }
            mPendingRilWakelocks.clear();
        }
    }

    @VisibleForTesting
    public void changeConcurrentRequests(int concurrentRequests, long time) {
        synchronized (mPendingRilWakelocks) {
            for (RilWakelockInfo wlInfo : mPendingRilWakelocks) {
                wlInfo.updateConcurrentRequests(concurrentRequests, time);
            }
        }
    }

    private void completeRequest(RilWakelockInfo wlInfo, long time) {
        wlInfo.setResponseTime(time);
        synchronized (mRequestStats) {
            mRequestStats.addCompletedWakelockTime(wlInfo.getWakelockTimeAttributedToClient());
            mRequestStats.incrementCompletedRequestsCount();
            mRequestStats.updateRequestHistograms(wlInfo.getRilRequestSent(),
                    (int) wlInfo.getWakelockTimeAttributedToClient());
        }
    }

    @VisibleForTesting
    public int getPendingRequestCount() {
        return mPendingRilWakelocks.size();
    }

    @VisibleForTesting
    public synchronized long updatePendingRequestWakelockTime(long uptime) {
        long totalPendingWakelockTime = 0;
        synchronized (mPendingRilWakelocks) {
            for (RilWakelockInfo wlInfo : mPendingRilWakelocks) {
                wlInfo.updateTime(uptime);
                totalPendingWakelockTime += wlInfo.getWakelockTimeAttributedToClient();
            }
        }
        synchronized (mRequestStats) {
            mRequestStats.setPendingRequestsCount(getPendingRequestCount());
            mRequestStats.setPendingRequestsWakelockTime(totalPendingWakelockTime);
        }
        return totalPendingWakelockTime;
    }

    private RilWakelockInfo removePendingWakelock(int request, int token) {
        RilWakelockInfo result = null;
        synchronized (mPendingRilWakelocks) {
            for (RilWakelockInfo wlInfo : mPendingRilWakelocks) {
                if ((wlInfo.getTokenNumber() == token) &&
                    (wlInfo.getRilRequestSent() == request)) {
                    result = wlInfo;
                }
            }
            if( result != null ) {
                mPendingRilWakelocks.remove(result);
            }
        }
        if(result == null) {
            Rlog.w(LOG_TAG, "Looking for Request<" + request + "," + token + "> in "
                + mPendingRilWakelocks);
        }
        return result;
    }

    @Override
    public String toString() {
        return "ClientWakelockAccountant{" +
                "mRequestStats=" + mRequestStats +
                ", mPendingRilWakelocks=" + mPendingRilWakelocks +
                '}';
    }
}

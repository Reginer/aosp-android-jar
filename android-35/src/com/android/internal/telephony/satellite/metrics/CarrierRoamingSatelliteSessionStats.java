/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.internal.telephony.satellite.metrics;

import android.annotation.NonNull;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.metrics.SatelliteStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CarrierRoamingSatelliteSessionStats {
    private static final String TAG = CarrierRoamingSatelliteSessionStats.class.getSimpleName();
    private final Context mContext;

    private int mCarrierId;
    private boolean mIsNtnRoamingInHomeCountry;
    private int mRsrpAvg;
    private int mRsrpMedian;
    private int mRssnrAvg;
    private int mRssnrMedian;
    private int mCountOfIncomingSms;
    private int mCountOfOutgoingSms;
    private int mCountOfIncomingMms;
    private int mCountOfOutgoingMms;

    private int mSessionStartTimeSec;
    private List<Long> mConnectionStartTimeList;
    private List<Long> mConnectionEndTimeList;

    public CarrierRoamingSatelliteSessionStats(@NonNull Context context, int carrierId) {
        logd("Create new CarrierRoamingSatelliteSessionStats.");
        initializeParams();

        mContext = context;
        mCarrierId = carrierId;
    }

    /** Log carrier roaming satellite session start */
    public void onSessionStart() {
        mSessionStartTimeSec = getCurrentTimeInSec();
        onConnectionStart();
    }

    /** Log carrier roaming satellite connection start */
    public void onConnectionStart() {
        mConnectionStartTimeList.add(getCurrentTime());
    }

    /** Log carrier roaming satellite session end */
    public void onSessionEnd() {
        onConnectionEnd();
        reportMetrics();
    }

    /** Log carrier roaming satellite connection end */
    public void onConnectionEnd() {
        mConnectionEndTimeList.add(getCurrentTime());
    }

    private void reportMetrics() {
        int totalSatelliteModeTimeSec = mSessionStartTimeSec > 0
                ? getCurrentTimeInSec() - mSessionStartTimeSec : 0;
        int numberOfSatelliteConnections = getNumberOfSatelliteConnections();
        int avgDurationOfSatelliteConnectionSec = getAvgDurationOfSatelliteConnection(
                numberOfSatelliteConnections);

        List<Integer> connectionGapList = getSatelliteConnectionGapList(
                numberOfSatelliteConnections);
        int satelliteConnectionGapMinSec = 0;
        int satelliteConnectionGapMaxSec = 0;
        if (!connectionGapList.isEmpty()) {
            satelliteConnectionGapMinSec = Collections.min(connectionGapList);
            satelliteConnectionGapMaxSec = Collections.max(connectionGapList);
        }

        SatelliteStats.CarrierRoamingSatelliteSessionParams params =
                new SatelliteStats.CarrierRoamingSatelliteSessionParams.Builder()
                        .setCarrierId(mCarrierId)
                        .setIsNtnRoamingInHomeCountry(mIsNtnRoamingInHomeCountry)
                        .setTotalSatelliteModeTimeSec(totalSatelliteModeTimeSec)
                        .setNumberOfSatelliteConnections(numberOfSatelliteConnections)
                        .setAvgDurationOfSatelliteConnectionSec(avgDurationOfSatelliteConnectionSec)
                        .setSatelliteConnectionGapMinSec(satelliteConnectionGapMinSec)
                        .setSatelliteConnectionGapAvgSec(getAvgConnectionGapSec(connectionGapList))
                        .setSatelliteConnectionGapMaxSec(satelliteConnectionGapMaxSec)
                        .setRsrpAvg(mRsrpAvg)
                        .setRsrpMedian(mRsrpMedian)
                        .setRssnrAvg(mRssnrAvg)
                        .setRssnrMedian(mRssnrMedian)
                        .setCountOfIncomingSms(mCountOfIncomingSms)
                        .setCountOfOutgoingSms(mCountOfOutgoingSms)
                        .setCountOfIncomingMms(mCountOfIncomingMms)
                        .setCountOfOutgoingMms(mCountOfOutgoingMms)
                        .build();
        SatelliteStats.getInstance().onCarrierRoamingSatelliteSessionMetrics(params);
        logd("reportMetrics: " + params);
        initializeParams();
    }

    private void initializeParams() {
        mCarrierId = TelephonyManager.UNKNOWN_CARRIER_ID;
        mIsNtnRoamingInHomeCountry = false;
        mRsrpAvg = 0;
        mRsrpMedian = 0;
        mRssnrAvg = 0;
        mRssnrMedian = 0;
        mCountOfIncomingSms = 0;
        mCountOfOutgoingSms = 0;
        mCountOfIncomingMms = 0;
        mCountOfOutgoingMms = 0;

        mSessionStartTimeSec = 0;
        mConnectionStartTimeList = new ArrayList<>();
        mConnectionEndTimeList = new ArrayList<>();
    }

    private int getNumberOfSatelliteConnections() {
        return Math.min(mConnectionStartTimeList.size(), mConnectionEndTimeList.size());
    }

    private int getAvgDurationOfSatelliteConnection(int numberOfSatelliteConnections) {
        if (numberOfSatelliteConnections == 0) {
            return 0;
        }

        long totalConnectionsDuration = 0;
        for (int i = 0; i < numberOfSatelliteConnections; i++) {
            long endTime = mConnectionEndTimeList.get(i);
            long startTime = mConnectionStartTimeList.get(i);
            if (endTime >= startTime && startTime > 0) {
                totalConnectionsDuration += endTime - startTime;
            }
        }

        long avgConnectionDuration = totalConnectionsDuration / numberOfSatelliteConnections;
        return (int) (avgConnectionDuration / 1000L);
    }

    private List<Integer> getSatelliteConnectionGapList(int numberOfSatelliteConnections) {
        if (numberOfSatelliteConnections == 0) {
            return new ArrayList<>();
        }

        List<Integer> connectionGapList = new ArrayList<>();
        for (int i = 1; i < numberOfSatelliteConnections; i++) {
            long prevConnectionEndTime = mConnectionEndTimeList.get(i - 1);
            long currentConnectionStartTime = mConnectionStartTimeList.get(i);
            if (currentConnectionStartTime > prevConnectionEndTime && prevConnectionEndTime > 0) {
                connectionGapList.add((int) (
                        (currentConnectionStartTime - prevConnectionEndTime) / 1000));
            }
        }
        return connectionGapList;
    }

    private int getAvgConnectionGapSec(@NonNull List<Integer> connectionGapList) {
        if (connectionGapList.isEmpty()) {
            return 0;
        }

        int totalConnectionGap = 0;
        for (int gap : connectionGapList) {
            totalConnectionGap += gap;
        }

        return (totalConnectionGap / connectionGapList.size());
    }

    private int getCurrentTimeInSec() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    private void logd(@NonNull String log) {
        Log.d(TAG, log);
    }

    private void loge(@NonNull String log) {
        Log.e(TAG, log);
    }
}

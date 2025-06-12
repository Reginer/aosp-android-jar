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
import android.telephony.CellInfo;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.metrics.SatelliteStats;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.subscription.SubscriptionManagerService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CarrierRoamingSatelliteSessionStats {
    private static final String TAG = CarrierRoamingSatelliteSessionStats.class.getSimpleName();
    private static final SparseArray<CarrierRoamingSatelliteSessionStats>
            sCarrierRoamingSatelliteSessionStats = new SparseArray<>();
    @NonNull private final SubscriptionManagerService mSubscriptionManagerService;
    private int mCarrierId;
    private boolean mIsNtnRoamingInHomeCountry;
    private int mCountOfIncomingSms;
    private int mCountOfOutgoingSms;
    private int mCountOfIncomingMms;
    private int mCountOfOutgoingMms;
    private long mIncomingMessageId;

    private int mSessionStartTimeSec;
    private List<Long> mConnectionStartTimeList;
    private List<Long> mConnectionEndTimeList;
    private List<Integer> mRsrpList;
    private List<Integer> mRssnrList;

    public CarrierRoamingSatelliteSessionStats(int subId) {
        logd("Create new CarrierRoamingSatelliteSessionStats. subId=" + subId);
        initializeParams();
        mSubscriptionManagerService = SubscriptionManagerService.getInstance();
    }

    /** Gets a CarrierRoamingSatelliteSessionStats instance. */
    public static CarrierRoamingSatelliteSessionStats getInstance(int subId) {
        synchronized (sCarrierRoamingSatelliteSessionStats) {
            if (sCarrierRoamingSatelliteSessionStats.get(subId) == null) {
                sCarrierRoamingSatelliteSessionStats.put(subId,
                        new CarrierRoamingSatelliteSessionStats(subId));
            }
            return sCarrierRoamingSatelliteSessionStats.get(subId);
        }
    }

    /** Log carrier roaming satellite session start */
    public void onSessionStart(int carrierId, Phone phone) {
        mCarrierId = carrierId;
        mSessionStartTimeSec = getCurrentTimeInSec();
        mIsNtnRoamingInHomeCountry = false;
        onConnectionStart(phone);
    }

    /** Log carrier roaming satellite connection start */
    public void onConnectionStart(Phone phone) {
        mConnectionStartTimeList.add(getCurrentTime());
        updateNtnRoamingInHomeCountry(phone);
    }

    /** Log carrier roaming satellite session end */
    public void onSessionEnd() {
        onConnectionEnd();
        reportMetrics();
        mIsNtnRoamingInHomeCountry = false;
    }

    /** Log carrier roaming satellite connection end */
    public void onConnectionEnd() {
        mConnectionEndTimeList.add(getCurrentTime());
    }

    /** Log rsrp and rssnr when occurred the service state change with NTN is connected. */
    public void onSignalStrength(Phone phone) {
        CellSignalStrengthLte cellSignalStrengthLte = getCellSignalStrengthLte(phone);
        int rsrp = cellSignalStrengthLte.getRsrp();
        int rssnr = cellSignalStrengthLte.getRssnr();
        if (rsrp == CellInfo.UNAVAILABLE) {
            logd("onSignalStrength: rsrp unavailable");
            return;
        }
        if (rssnr == CellInfo.UNAVAILABLE) {
            logd("onSignalStrength: rssnr unavailable");
            return;
        }
        mRsrpList.add(rsrp);
        mRssnrList.add(rssnr);
        logd("onSignalStrength : rsrp=" + rsrp + ", rssnr=" + rssnr);
    }

    /** Log incoming sms success case */
    public void onIncomingSms(int subId) {
        if (!isNtnConnected()) {
            return;
        }
        mCountOfIncomingSms += 1;
        logd("onIncomingSms: subId=" + subId + ", count=" + mCountOfIncomingSms);
    }

    /** Log outgoing sms success case */
    public void onOutgoingSms(int subId) {
        if (!isNtnConnected()) {
            return;
        }
        mCountOfOutgoingSms += 1;
        logd("onOutgoingSms: subId=" + subId + ", count=" + mCountOfOutgoingSms);
    }

    /** Log incoming or outgoing mms success case */
    public void onMms(boolean isIncomingMms, long messageId) {
        if (!isNtnConnected()) {
            return;
        }
        if (isIncomingMms) {
            mIncomingMessageId = messageId;
            mCountOfIncomingMms += 1;
            logd("onMms: messageId=" + messageId + ", countOfIncomingMms=" + mCountOfIncomingMms);
        } else {
            if (mIncomingMessageId == messageId) {
                logd("onMms: NotifyResponse ignore it.");
                mIncomingMessageId = 0;
                return;
            }
            mCountOfOutgoingMms += 1;
            logd("onMms: countOfOutgoingMms=" + mCountOfOutgoingMms);
        }
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
                        .setSatelliteConnectionGapAvgSec(getAvg(connectionGapList))
                        .setSatelliteConnectionGapMaxSec(satelliteConnectionGapMaxSec)
                        .setRsrpAvg(getAvg(mRsrpList))
                        .setRsrpMedian(getMedian(mRsrpList))
                        .setRssnrAvg(getAvg(mRssnrList))
                        .setRssnrMedian(getMedian(mRssnrList))
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
        mCountOfIncomingSms = 0;
        mCountOfOutgoingSms = 0;
        mCountOfIncomingMms = 0;
        mCountOfOutgoingMms = 0;
        mIncomingMessageId = 0;

        mSessionStartTimeSec = 0;
        mConnectionStartTimeList = new ArrayList<>();
        mConnectionEndTimeList = new ArrayList<>();
        mRsrpList = new ArrayList<>();
        mRssnrList = new ArrayList<>();
        logd("initializeParams");
    }

    private CellSignalStrengthLte getCellSignalStrengthLte(Phone phone) {
        SignalStrength signalStrength = phone.getSignalStrength();
        List<CellSignalStrength> cellSignalStrengths = signalStrength.getCellSignalStrengths();
        for (CellSignalStrength cellSignalStrength : cellSignalStrengths) {
            if (cellSignalStrength instanceof CellSignalStrengthLte) {
                return (CellSignalStrengthLte) cellSignalStrength;
            }
        }

        return new CellSignalStrengthLte();
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

    private int getAvg(@NonNull List<Integer> list) {
        if (list.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (int num : list) {
            total += num;
        }

        return total / list.size();
    }

    private int getMedian(@NonNull List<Integer> list) {
        if (list.isEmpty()) {
            return 0;
        }
        int size = list.size();
        if (size == 1) {
            return list.get(0);
        }

        Collections.sort(list);
        return size % 2 == 0 ? (list.get(size / 2 - 1) + list.get(size / 2)) / 2
                : list.get(size / 2);
    }

    private int getCurrentTimeInSec() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    private boolean isNtnConnected() {
        return mSessionStartTimeSec != 0;
    }

    private void updateNtnRoamingInHomeCountry(Phone phone) {
        int subId = phone.getSubId();
        ServiceState serviceState = phone.getServiceState();
        if (serviceState == null) {
            logd("ServiceState is null");
            return;
        }

        String satelliteRegisteredPlmn = "";
        for (NetworkRegistrationInfo nri
                : serviceState.getNetworkRegistrationInfoList()) {
            if (nri.isNonTerrestrialNetwork()) {
                satelliteRegisteredPlmn = nri.getRegisteredPlmn();
            }
        }

        SubscriptionInfoInternal subscriptionInfoInternal =
                mSubscriptionManagerService.getSubscriptionInfoInternal(subId);
        if (subscriptionInfoInternal == null) {
            logd("SubscriptionInfoInternal is null");
            return;
        }
        String simCountry = MccTable.countryCodeForMcc(subscriptionInfoInternal.getMcc());
        mIsNtnRoamingInHomeCountry = true;
        if (satelliteRegisteredPlmn != null
                && satelliteRegisteredPlmn.length() >= 3) {
            String satelliteRegisteredCountry = MccTable.countryCodeForMcc(
                    satelliteRegisteredPlmn.substring(0, 3));
            if (simCountry.equalsIgnoreCase(satelliteRegisteredCountry)) {
                mIsNtnRoamingInHomeCountry = true;
            } else {
                // If device is connected to roaming non-terrestrial network, then marking as
                // roaming in external country
                mIsNtnRoamingInHomeCountry = false;
            }
        }
        logd("updateNtnRoamingInHomeCountry: mIsNtnRoamingInHomeCountry="
                + mIsNtnRoamingInHomeCountry);
    }

    private void logd(@NonNull String log) {
        Log.d(TAG, log);
    }

    private void loge(@NonNull String log) {
        Log.e(TAG, log);
    }
}

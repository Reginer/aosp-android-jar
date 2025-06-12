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

package com.android.internal.telephony.satellite.metrics;

import static android.telephony.TelephonyManager.UNKNOWN_CARRIER_ID;
import static android.telephony.satellite.NtnSignalStrength.NTN_SIGNAL_STRENGTH_NONE;
import static android.telephony.satellite.SatelliteManager.KEY_SESSION_STATS_V2;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_SUCCESS;

import android.annotation.NonNull;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.telephony.satellite.NtnSignalStrength;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.SatelliteSessionStats;
import android.util.Log;

import com.android.internal.telephony.metrics.SatelliteStats;
import com.android.internal.telephony.satellite.DatagramDispatcher;

/**
 * Stats to log to satellite session metrics
 */
public class SessionMetricsStats {
    private static final String TAG = SessionMetricsStats.class.getSimpleName();

    private static SessionMetricsStats sInstance = null;
    private @SatelliteManager.SatelliteResult int mInitializationResult;
    private @SatelliteManager.NTRadioTechnology int mRadioTechnology;
    private @SatelliteManager.SatelliteResult int mTerminationResult;
    private long mInitializationProcessingTimeMillis;
    private long mTerminationProcessingTimeMillis;
    private int mSessionDurationSec;
    private int mCountOfSuccessfulOutgoingDatagram;
    private int mShadowCountOfSuccessfulOutgoingDatagram;
    private int mCountOfFailedOutgoingDatagram;
    private int mShadowCountOfFailedOutgoingDatagram;
    private int mCountOfTimedOutUserMessagesWaitingForConnection;
    private int mShadowCountOfTimedOutUserMessagesWaitingForConnection;
    private int mCountOfTimedOutUserMessagesWaitingForAck;
    private int mShadowCountOfTimedOutUserMessagesWaitingForAck;
    private int mCountOfSuccessfulIncomingDatagram;
    private int mCountOfIncomingDatagramFailed;
    private boolean mIsDemoMode;
    private @NtnSignalStrength.NtnSignalStrengthLevel int mMaxNtnSignalStrengthLevel;
    private int mCarrierId;
    private int mCountOfSatelliteNotificationDisplayed;
    private int mCountOfAutoExitDueToScreenOff;
    private int mCountOfAutoExitDueToTnNetwork;
    private boolean mIsEmergency;
    private boolean mIsNtnOnlyCarrier;
    private int mMaxInactivityDurationSec;
    private SatelliteSessionStats mDatagramStats;

    private SessionMetricsStats() {
        initializeSessionMetricsParam();
        mDatagramStats = new SatelliteSessionStats();
    }

    /**
     * Returns the Singleton instance of SessionMetricsStats class.
     * If an instance of the Singleton class has not been created,
     * it creates a new instance and returns it. Otherwise, it returns
     * the existing instance.
     * @return the Singleton instance of SessionMetricsStats.
     */
    public static SessionMetricsStats getInstance() {
        if (sInstance == null) {
            loge("create new SessionMetricsStats.");
            sInstance = new SessionMetricsStats();
        }
        return sInstance;
    }

    /** Sets the satellite initialization result. */
    public SessionMetricsStats setInitializationResult(
            @SatelliteManager.SatelliteResult int result) {
        logd("setInitializationResult(" + result + ")");
        mInitializationResult = result;
        return this;
    }

    /** Sets the satellite ratio technology. */
    public SessionMetricsStats setSatelliteTechnology(
            @SatelliteManager.NTRadioTechnology int radioTechnology) {
        logd("setSatelliteTechnology(" + radioTechnology + ")");
        mRadioTechnology = radioTechnology;
        return this;
    }

    /** Sets the satellite de-initialization result. */
    public SessionMetricsStats setTerminationResult(
            @SatelliteManager.SatelliteResult int result) {
        logd("setTerminationResult(" + result + ")");
        mTerminationResult = result;
        return this;
    }

    /** Sets the satellite initialization processing time. */
    public SessionMetricsStats setInitializationProcessingTime(long processingTime) {
        logd("setInitializationProcessingTime(" + processingTime + ")");
        mInitializationProcessingTimeMillis = processingTime;
        return this;
    }

    /** Sets the satellite de-initialization processing time. */
    public SessionMetricsStats setTerminationProcessingTime(long processingTime) {
        logd("setTerminationProcessingTime(" + processingTime + ")");
        mTerminationProcessingTimeMillis = processingTime;
        return this;
    }

    /** Sets the total enabled time for the satellite session. */
    public SessionMetricsStats setSessionDurationSec(int sessionDurationSec) {
        logd("setSessionDuration(" + sessionDurationSec + ")");
        mSessionDurationSec = sessionDurationSec;
        return this;
    }

    /** Increase the count of successful outgoing datagram transmission. */
    public SessionMetricsStats addCountOfSuccessfulOutgoingDatagram(
            @NonNull @SatelliteManager.DatagramType int datagramType,
            long datagramTransmissionTime) {
        logd("addCountOfSuccessfulOutgoingDatagram: datagramType=" + datagramType);
        mDatagramStats.recordSuccessfulOutgoingDatagramStats(datagramType,
                datagramTransmissionTime);
        if (datagramType == SatelliteManager.DATAGRAM_TYPE_KEEP_ALIVE) {
            // Ignore KEEP_ALIVE messages
            return this;
        }

        mCountOfSuccessfulOutgoingDatagram++;
        mShadowCountOfSuccessfulOutgoingDatagram++;
        return this;
    }

    /** Increase the count of failed outgoing datagram transmission. */
    public SessionMetricsStats addCountOfFailedOutgoingDatagram(
            @NonNull @SatelliteManager.DatagramType int datagramType,
            @NonNull @SatelliteManager.SatelliteResult int resultCode) {
        logd("addCountOfFailedOutgoingDatagram: datagramType=" + datagramType + "  resultCode = "
                + resultCode);
        mDatagramStats.addCountOfUnsuccessfulUserMessages(datagramType, resultCode);
        if (datagramType == SatelliteManager.DATAGRAM_TYPE_KEEP_ALIVE) {
            // Ignore KEEP_ALIVE messages
            return this;
        }

        mCountOfFailedOutgoingDatagram++;
        mShadowCountOfFailedOutgoingDatagram++;
        if (resultCode == SatelliteManager.SATELLITE_RESULT_NOT_REACHABLE) {
            addCountOfTimedOutUserMessagesWaitingForConnection(datagramType);
        } else if (resultCode == SatelliteManager.SATELLITE_RESULT_MODEM_TIMEOUT) {
            addCountOfTimedOutUserMessagesWaitingForAck(datagramType);
        }
        return this;
    }

    /** Increase the count of user messages that timed out waiting for connection. */
    private SessionMetricsStats addCountOfTimedOutUserMessagesWaitingForConnection(
            @NonNull @SatelliteManager.DatagramType int datagramType) {
        if (datagramType == SatelliteManager.DATAGRAM_TYPE_KEEP_ALIVE) {
            // Ignore KEEP_ALIVE messages
            return this;
        }

        mCountOfTimedOutUserMessagesWaitingForConnection++;
        mShadowCountOfTimedOutUserMessagesWaitingForConnection++;
        logd("addCountOfTimedOutUserMessagesWaitingForConnection: current count="
                + mCountOfTimedOutUserMessagesWaitingForConnection);
        return this;
    }

    /** Increase the count of user messages that timed out waiting for ack. */
    private SessionMetricsStats addCountOfTimedOutUserMessagesWaitingForAck(
            @NonNull @SatelliteManager.DatagramType int datagramType) {
        if (datagramType == SatelliteManager.DATAGRAM_TYPE_KEEP_ALIVE) {
            // Ignore KEEP_ALIVE messages
            return this;
        }

        mCountOfTimedOutUserMessagesWaitingForAck++;
        mShadowCountOfTimedOutUserMessagesWaitingForAck++;
        logd("addCountOfTimedOutUserMessagesWaitingForAck: current count="
                + mCountOfTimedOutUserMessagesWaitingForAck);
        return this;
    }

    /** Increase the count of successful incoming datagram transmission. */
    public SessionMetricsStats addCountOfSuccessfulIncomingDatagram() {
        mCountOfSuccessfulIncomingDatagram++;
        logd("addCountOfSuccessfulIncomingDatagram: current count="
                + mCountOfSuccessfulIncomingDatagram);
        return this;
    }

    /** Increase the count of failed incoming datagram transmission. */
    public SessionMetricsStats addCountOfFailedIncomingDatagram() {
        mCountOfIncomingDatagramFailed++;
        logd("addCountOfFailedIncomingDatagram: current count=" + mCountOfIncomingDatagramFailed);
        return this;
    }

    /** Sets whether the session is enabled for demo mode or not. */
    public SessionMetricsStats setIsDemoMode(boolean isDemoMode) {
        mIsDemoMode = isDemoMode;
        logd("setIsDemoMode(" + mIsDemoMode + ")");
        return this;
    }

    /** Updates the max Ntn signal strength level for the session. */
    public SessionMetricsStats updateMaxNtnSignalStrengthLevel(
            @NtnSignalStrength.NtnSignalStrengthLevel int latestNtnSignalStrengthLevel) {
        if (latestNtnSignalStrengthLevel > mMaxNtnSignalStrengthLevel) {
            mMaxNtnSignalStrengthLevel = latestNtnSignalStrengthLevel;
        }
        logd("updateMaxNtnSignalsStrength: latest signal strength=" + latestNtnSignalStrengthLevel
                + ", max signal strength=" + mMaxNtnSignalStrengthLevel);
        return this;
    }

    /** Sets the Carrier ID of this NTN session. */
    public SessionMetricsStats setCarrierId(int carrierId) {
        mCarrierId = carrierId;
        logd("setCarrierId(" + carrierId + ")");
        return this;
    }

    /** Increase the count of Satellite Notification Display. */
    public SessionMetricsStats addCountOfSatelliteNotificationDisplayed() {
        mCountOfSatelliteNotificationDisplayed++;
        logd("addCountOfSatelliteNotificationDisplayed: current count="
                + mCountOfSatelliteNotificationDisplayed);
        return this;
    }

    /** Increase the count of auto exit from P2P satellite messaging due to screen off. */
    public SessionMetricsStats addCountOfAutoExitDueToScreenOff() {
        mCountOfAutoExitDueToScreenOff++;
        logd("addCountOfAutoExitDueToScreenOff: current count=" + mCountOfAutoExitDueToScreenOff);
        return this;
    }

    /** Increase the count of auto exit from P2P satellite messaging due to scan TN network. */
    public SessionMetricsStats addCountOfAutoExitDueToTnNetwork() {
        mCountOfAutoExitDueToTnNetwork++;
        logd("addCountOfAutoExitDueToTnNetwork: current count=" + mCountOfAutoExitDueToTnNetwork);
        return this;
    }

    /** Sets whether the session is enabled for emergency or not. */
    public SessionMetricsStats setIsEmergency(boolean isEmergency) {
        mIsEmergency = isEmergency;
        logd("setIsEmergency(" + mIsEmergency + ")");
        return this;
    }

    /** Capture the latest provisioned state for satellite service */
    public SessionMetricsStats setIsNtnOnlyCarrier(boolean isNtnOnlyCarrier) {
        mIsNtnOnlyCarrier = isNtnOnlyCarrier;
        logd("setIsNtnOnlyCarrier(" + mIsNtnOnlyCarrier + ")");
        return this;
    }

    /** Updates the max inactivity duration session metric. */
    public SessionMetricsStats updateMaxInactivityDurationSec(int inactivityDurationSec) {
        if (inactivityDurationSec > mMaxInactivityDurationSec) {
            mMaxInactivityDurationSec = inactivityDurationSec;
        }
        logd("updateMaxInactivityDurationSec: latest inactivty duration (sec)="
                + inactivityDurationSec
                + ", max inactivity duration="
                + mMaxInactivityDurationSec);
        return this;
    }

    /** Report the session metrics atoms to PersistAtomsStorage in telephony. */
    public void reportSessionMetrics() {
        SatelliteStats.SatelliteSessionParams sessionParams =
                new SatelliteStats.SatelliteSessionParams.Builder()
                        .setSatelliteServiceInitializationResult(mInitializationResult)
                        .setSatelliteTechnology(mRadioTechnology)
                        .setTerminationResult(mTerminationResult)
                        .setInitializationProcessingTime(mInitializationProcessingTimeMillis)
                        .setTerminationProcessingTime(mTerminationProcessingTimeMillis)
                        .setSessionDuration(mSessionDurationSec)
                        .setCountOfOutgoingDatagramSuccess(mCountOfSuccessfulOutgoingDatagram)
                        .setCountOfOutgoingDatagramFailed(mCountOfFailedOutgoingDatagram)
                        .setCountOfIncomingDatagramSuccess(mCountOfSuccessfulIncomingDatagram)
                        .setCountOfIncomingDatagramFailed(mCountOfIncomingDatagramFailed)
                        .setIsDemoMode(mIsDemoMode)
                        .setMaxNtnSignalStrengthLevel(mMaxNtnSignalStrengthLevel)
                        .setCarrierId(mCarrierId)
                        .setCountOfSatelliteNotificationDisplayed(
                                mCountOfSatelliteNotificationDisplayed)
                        .setCountOfAutoExitDueToScreenOff(mCountOfAutoExitDueToScreenOff)
                        .setCountOfAutoExitDueToTnNetwork(mCountOfAutoExitDueToTnNetwork)
                        .setIsEmergency(mIsEmergency)
                        .setIsNtnOnlyCarrier(mIsNtnOnlyCarrier)
                        .setMaxInactivityDurationSec(mMaxInactivityDurationSec)
                        .build();
        logd("reportSessionMetrics: " + sessionParams.toString());
        SatelliteStats.getInstance().onSatelliteSessionMetrics(sessionParams);
        initializeSessionMetricsParam();
    }

    /** Returns {@link SatelliteSessionStats} of the satellite service. */
    public void requestSatelliteSessionStats(int subId, @NonNull ResultReceiver result) {
        Log.i(TAG, "requestSatelliteSessionStats called");
        Bundle bundle = new Bundle();
        SatelliteSessionStats sessionStats = new SatelliteSessionStats.Builder()
                .setCountOfSuccessfulUserMessages(mShadowCountOfSuccessfulOutgoingDatagram)
                .setCountOfUnsuccessfulUserMessages(mShadowCountOfFailedOutgoingDatagram)
                .setCountOfTimedOutUserMessagesWaitingForConnection(
                        mShadowCountOfTimedOutUserMessagesWaitingForConnection)
                .setCountOfTimedOutUserMessagesWaitingForAck(
                        mShadowCountOfTimedOutUserMessagesWaitingForAck)
                .setCountOfUserMessagesInQueueToBeSent(
                        DatagramDispatcher.getInstance().getPendingUserMessagesCount())
                .build();
        bundle.putParcelable(SatelliteManager.KEY_SESSION_STATS, sessionStats);

        // Reset countOfUserMessagesInQueueToBeSent for each datagramType to 0.
        mDatagramStats.resetCountOfUserMessagesInQueueToBeSent();

        DatagramDispatcher.getInstance().updateSessionStatsWithPendingUserMsgCount(mDatagramStats);
        bundle.putParcelable(KEY_SESSION_STATS_V2, mDatagramStats);
        Log.i(TAG, "[END] DatagramStats = " + mDatagramStats);
        result.send(SATELLITE_RESULT_SUCCESS, bundle);
    }

    /** Returns the processing time for satellite session initialization. */
    public long getSessionInitializationProcessingTimeMillis() {
        return mInitializationProcessingTimeMillis;
    }

    /** Returns the processing time for satellite session termination. */
    public long getSessionTerminationProcessingTimeMillis() {
        return mTerminationProcessingTimeMillis;
    }

    private void initializeSessionMetricsParam() {
        mInitializationResult = SATELLITE_RESULT_SUCCESS;
        mRadioTechnology = SatelliteManager.NT_RADIO_TECHNOLOGY_UNKNOWN;
        mTerminationResult = SATELLITE_RESULT_SUCCESS;
        mInitializationProcessingTimeMillis = 0;
        mTerminationProcessingTimeMillis = 0;
        mSessionDurationSec = 0;
        mCountOfSuccessfulOutgoingDatagram = 0;
        mCountOfFailedOutgoingDatagram = 0;
        mCountOfTimedOutUserMessagesWaitingForConnection = 0;
        mCountOfTimedOutUserMessagesWaitingForAck = 0;
        mCountOfSuccessfulIncomingDatagram = 0;
        mCountOfIncomingDatagramFailed = 0;
        mIsDemoMode = false;
        mMaxNtnSignalStrengthLevel = NTN_SIGNAL_STRENGTH_NONE;
        mCarrierId = UNKNOWN_CARRIER_ID;
        mCountOfSatelliteNotificationDisplayed = 0;
        mCountOfAutoExitDueToScreenOff = 0;
        mCountOfAutoExitDueToTnNetwork = 0;
        mIsEmergency = false;
        mIsNtnOnlyCarrier = false;
        mMaxInactivityDurationSec = 0;
    }

    public void resetSessionStatsShadowCounters() {
        logd("resetTheStatsCounters");
        mShadowCountOfSuccessfulOutgoingDatagram = 0;
        mShadowCountOfFailedOutgoingDatagram = 0;
        mShadowCountOfTimedOutUserMessagesWaitingForConnection = 0;
        mShadowCountOfTimedOutUserMessagesWaitingForAck = 0;
        mDatagramStats.clear();
    }

    private static void logd(@NonNull String log) {
        Log.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Log.e(TAG, log);
    }
}

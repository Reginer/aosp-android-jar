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

import android.annotation.NonNull;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import com.android.internal.telephony.metrics.SatelliteStats;

/**
 * Stats to log to satellite session metrics
 */
public class SessionMetricsStats {
    private static final String TAG = SessionMetricsStats.class.getSimpleName();
    private static final boolean DBG = false;

    private static SessionMetricsStats sInstance = null;
    private @SatelliteManager.SatelliteResult int mInitializationResult;
    private @SatelliteManager.NTRadioTechnology int mRadioTechnology;
    private @SatelliteManager.SatelliteResult int mTerminationResult;
    private long mInitializationProcessingTimeMillis;
    private long mTerminationProcessingTimeMillis;
    private int mSessionDurationSec;
    private int mCountOfSuccessfulOutgoingDatagram;
    private int mCountOfFailedOutgoingDatagram;
    private int mCountOfSuccessfulIncomingDatagram;
    private int mCountOfIncomingDatagramFailed;
    private boolean mIsDemoMode;


    private SessionMetricsStats() {
        initializeSessionMetricsParam();
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
    public SessionMetricsStats addCountOfSuccessfulOutgoingDatagram() {
        mCountOfSuccessfulOutgoingDatagram++;
        logd("addCountOfSuccessfulOutgoingDatagram: current count="
                + mCountOfSuccessfulOutgoingDatagram);
        return this;
    }

    /** Increase the count of failed outgoing datagram transmission. */
    public SessionMetricsStats addCountOfFailedOutgoingDatagram() {
        mCountOfFailedOutgoingDatagram++;
        logd("addCountOfFailedOutgoingDatagram: current count=" + mCountOfFailedOutgoingDatagram);
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
                        .build();
        logd("reportSessionMetrics: " + sessionParams.toString());
        SatelliteStats.getInstance().onSatelliteSessionMetrics(sessionParams);
        initializeSessionMetricsParam();
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
        mInitializationResult = SatelliteManager.SATELLITE_RESULT_SUCCESS;
        mRadioTechnology = SatelliteManager.NT_RADIO_TECHNOLOGY_UNKNOWN;
        mTerminationResult = SatelliteManager.SATELLITE_RESULT_SUCCESS;
        mInitializationProcessingTimeMillis = 0;
        mTerminationProcessingTimeMillis = 0;
        mSessionDurationSec = 0;
        mCountOfSuccessfulOutgoingDatagram = 0;
        mCountOfFailedOutgoingDatagram = 0;
        mCountOfSuccessfulIncomingDatagram = 0;
        mCountOfIncomingDatagramFailed = 0;
        mIsDemoMode = false;
    }

    private static void logd(@NonNull String log) {
        if (DBG) {
            Log.d(TAG, log);
        }
    }

    private static void loge(@NonNull String log) {
        Log.e(TAG, log);
    }
}

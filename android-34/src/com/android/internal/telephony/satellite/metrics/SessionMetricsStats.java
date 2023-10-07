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
    private @SatelliteManager.SatelliteError int mInitializationResult;
    private @SatelliteManager.NTRadioTechnology int mRadioTechnology;

    private SessionMetricsStats() {
        initializeSessionMetricsParam();
    }

    /**
     * Returns the Singleton instance of SessionMetricsStats class.
     * If an instance of the Singleton class has not been created,
     * it creates a new instance and returns it. Otherwise, it returns
     * the existing instance.
     * @return the Singleton instance of SessionMetricsStats
     */
    public static SessionMetricsStats getInstance() {
        if (sInstance == null) {
            loge("create new SessionMetricsStats.");
            sInstance = new SessionMetricsStats();
        }
        return sInstance;
    }

    /** Sets the satellite initialization result */
    public SessionMetricsStats setInitializationResult(
            @SatelliteManager.SatelliteError int result) {
        logd("setInitializationResult(" + result + ")");
        mInitializationResult = result;
        return this;
    }

    /** Sets the satellite ratio technology */
    public SessionMetricsStats setRadioTechnology(
            @SatelliteManager.NTRadioTechnology int radioTechnology) {
        logd("setRadioTechnology(" + radioTechnology + ")");
        mRadioTechnology = radioTechnology;
        return this;
    }

    /** Report the session metrics atoms to PersistAtomsStorage in telephony */
    public void reportSessionMetrics() {
        SatelliteStats.SatelliteSessionParams sessionParams =
                new SatelliteStats.SatelliteSessionParams.Builder()
                        .setSatelliteServiceInitializationResult(mInitializationResult)
                        .setSatelliteTechnology(mRadioTechnology)
                        .build();
        logd(sessionParams.toString());
        SatelliteStats.getInstance().onSatelliteSessionMetrics(sessionParams);
        initializeSessionMetricsParam();
    }

    private void initializeSessionMetricsParam() {
        mInitializationResult = SatelliteManager.SATELLITE_ERROR_NONE;
        mRadioTechnology = SatelliteManager.NT_RADIO_TECHNOLOGY_UNKNOWN;
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

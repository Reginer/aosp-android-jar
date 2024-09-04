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
import android.util.Log;

import com.android.internal.telephony.metrics.SatelliteStats;
import com.android.internal.telephony.satellite.SatelliteConstants;

public class CarrierRoamingSatelliteControllerStats {
    private static final String TAG = CarrierRoamingSatelliteControllerStats.class.getSimpleName();
    private static CarrierRoamingSatelliteControllerStats sInstance = null;
    private static final int ADD_COUNT = 1;

    private SatelliteStats mSatelliteStats;

    private CarrierRoamingSatelliteControllerStats() {
        mSatelliteStats = SatelliteStats.getInstance();
    }

    /**
     * Returns the Singleton instance of CarrierRoamingSatelliteControllerStats class.
     * If an instance of the Singleton class has not been created,
     * it creates a new instance and returns it. Otherwise, it returns
     * the existing instance.
     * @return the Singleton instance of CarrierRoamingSatelliteControllerStats
     */
    public static CarrierRoamingSatelliteControllerStats getOrCreateInstance() {
        if (sInstance == null) {
            logd("Create new CarrierRoamingSatelliteControllerStats.");
            sInstance = new CarrierRoamingSatelliteControllerStats();
        }
        return sInstance;
    }

    /** Report config data source */
    public void reportConfigDataSource(@SatelliteConstants.ConfigDataSource int configDataSource) {
        mSatelliteStats.onCarrierRoamingSatelliteControllerStatsMetrics(
                new SatelliteStats.CarrierRoamingSatelliteControllerStatsParams.Builder()
                        .setConfigDataSource(configDataSource)
                        .build());
    }

    /** Report count of entitlement status query request */
    public void reportCountOfEntitlementStatusQueryRequest() {
        mSatelliteStats.onCarrierRoamingSatelliteControllerStatsMetrics(
                new SatelliteStats.CarrierRoamingSatelliteControllerStatsParams.Builder()
                        .setCountOfEntitlementStatusQueryRequest(ADD_COUNT)
                        .build());
    }

    /** Report count of satellite config update request */
    public void reportCountOfSatelliteConfigUpdateRequest() {
        mSatelliteStats.onCarrierRoamingSatelliteControllerStatsMetrics(
                new SatelliteStats.CarrierRoamingSatelliteControllerStatsParams.Builder()
                        .setCountOfSatelliteConfigUpdateRequest(ADD_COUNT)
                        .build());
    }

    /** Report count of satellite notification displayed */
    public void reportCountOfSatelliteNotificationDisplayed() {
        mSatelliteStats.onCarrierRoamingSatelliteControllerStatsMetrics(
                new SatelliteStats.CarrierRoamingSatelliteControllerStatsParams.Builder()
                        .setCountOfSatelliteNotificationDisplayed(ADD_COUNT)
                        .build());
    }

    private static void logd(@NonNull String log) {
        Log.d(TAG, log);
    }
}

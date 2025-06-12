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

public class ConfigUpdaterMetricsStats {
    private static final String TAG = ConfigUpdaterMetricsStats.class.getSimpleName();
    private static ConfigUpdaterMetricsStats sInstance = null;

    private int mConfigVersion;
    private int mOemConfigResult;
    private int mCarrierConfigResult;

    private ConfigUpdaterMetricsStats() {
        initializeConfigUpdaterParams();
    }

    /**
     * Returns the Singleton instance of ConfigUpdaterMetricsStats class.
     * If an instance of the Singleton class has not been created,
     * it creates a new instance and returns it. Otherwise, it returns
     * the existing instance.
     * @return the Singleton instance of ConfigUpdaterMetricsStats
     */
    public static ConfigUpdaterMetricsStats getOrCreateInstance() {
        if (sInstance == null) {
            logd("Create new ConfigUpdaterMetricsStats.");
            sInstance = new ConfigUpdaterMetricsStats();
        }
        return sInstance;
    }

    /** Set config version for config updater metrics */
    public ConfigUpdaterMetricsStats setConfigVersion(int configVersion) {
        mConfigVersion = configVersion;
        return this;
    }

    /** Set oem config result for config updater metrics */
    public ConfigUpdaterMetricsStats setOemConfigResult(int oemConfigResult) {
        mOemConfigResult = oemConfigResult;
        return this;
    }

    /** Set carrier config result for config updater metrics */
    public ConfigUpdaterMetricsStats setCarrierConfigResult(int carrierConfigResult) {
        mCarrierConfigResult = carrierConfigResult;
        return this;
    }

    /** Report metrics on oem config update error */
    public void reportOemConfigError(int error) {
        mOemConfigResult = error;
        reportConfigUpdaterMetrics();
    }

    /** Report metrics on carrier config update error */
    public void reportCarrierConfigError(int error) {
        mCarrierConfigResult = error;
        reportConfigUpdaterMetrics();
    }

    /** Report metrics on config update error */
    public void reportOemAndCarrierConfigError(int error) {
        mOemConfigResult = error;
        mCarrierConfigResult = error;
        reportConfigUpdaterMetrics();
    }

    /** Report metrics on config update success */
    public void reportConfigUpdateSuccess() {
        mOemConfigResult = SatelliteConstants.CONFIG_UPDATE_RESULT_SUCCESS;
        mCarrierConfigResult = SatelliteConstants.CONFIG_UPDATE_RESULT_SUCCESS;
        reportConfigUpdaterMetrics();
    }


    /** Report config updater metrics atom to PersistAtomsStorage in telephony */
    private void reportConfigUpdaterMetrics() {
        SatelliteStats.SatelliteConfigUpdaterParams configUpdaterParams =
                new SatelliteStats.SatelliteConfigUpdaterParams.Builder()
                        .setConfigVersion(mConfigVersion)
                        .setOemConfigResult(mOemConfigResult)
                        .setCarrierConfigResult(mCarrierConfigResult)
                        .setCount(1)
                        .build();
        SatelliteStats.getInstance().onSatelliteConfigUpdaterMetrics(configUpdaterParams);
        logd("reportConfigUpdaterMetrics: " + configUpdaterParams);

        CarrierRoamingSatelliteControllerStats.getOrCreateInstance()
                .reportCountOfSatelliteConfigUpdateRequest();

        initializeConfigUpdaterParams();
    }

    private void initializeConfigUpdaterParams() {
        mConfigVersion = -1;
        mOemConfigResult = SatelliteConstants.CONFIG_UPDATE_RESULT_UNKNOWN;
        mCarrierConfigResult = SatelliteConstants.CONFIG_UPDATE_RESULT_UNKNOWN;
    }

    private static void logd(@NonNull String log) {
        Log.d(TAG, log);
    }
}

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
 * Stats to log to satellite metrics
 */
public class ProvisionMetricsStats {
    private static final String TAG = ProvisionMetricsStats.class.getSimpleName();
    private static final boolean DBG = false;

    private static ProvisionMetricsStats sInstance = null;

    public static final int INVALID_TIME = -1;

    private int mResultCode;
    private int mProvisioningStartTimeSec;
    private boolean mIsProvisionRequest;
    private boolean mIsCanceled;

    private ProvisionMetricsStats() {
        initializeProvisionParams();
    }

    /**
     * Returns the Singleton instance of ProvisionMetricsStats class.
     * If an instance of the Singleton class has not been created,
     * it creates a new instance and returns it. Otherwise, it returns
     * the existing instance.
     * @return the Singleton instance of ProvisionMetricsStats
     */
    public static ProvisionMetricsStats getOrCreateInstance() {
        if (sInstance == null) {
            logd("Create new ProvisionMetricsStats.");
            sInstance = new ProvisionMetricsStats();
        }
        return sInstance;
    }

    /** Sets the resultCode for provision metrics */
    public ProvisionMetricsStats setResultCode(@SatelliteManager.SatelliteError int error) {
        mResultCode = error;
        return this;
    }

    /** Sets the start time of provisioning */
    public void setProvisioningStartTime() {
        mProvisioningStartTimeSec = (int) (System.currentTimeMillis() / 1000);
    }

    /** Sets the isProvisionRequest to indicate whether provision or de-provision */
    public ProvisionMetricsStats setIsProvisionRequest(boolean isProvisionRequest) {
        mIsProvisionRequest = isProvisionRequest;
        return this;
    }

    /** Sets the isCanceled to know whether the provision is canceled */
    public ProvisionMetricsStats setIsCanceled(boolean isCanceled) {
        mIsCanceled = isCanceled;
        return this;
    }

    /** Report the provision metrics atoms to PersistAtomsStorage in telephony */
    public void reportProvisionMetrics() {
        SatelliteStats.SatelliteProvisionParams provisionParams =
                new SatelliteStats.SatelliteProvisionParams.Builder()
                        .setResultCode(mResultCode)
                        .setProvisioningTimeSec((int)
                                (System.currentTimeMillis() / 1000) - mProvisioningStartTimeSec)
                        .setIsProvisionRequest(mIsProvisionRequest)
                        .setIsCanceled(mIsCanceled)
                        .build();
        SatelliteStats.getInstance().onSatelliteProvisionMetrics(provisionParams);
        logd(provisionParams.toString());
        initializeProvisionParams();
    }

    private void initializeProvisionParams() {
        mResultCode = -1;
        mProvisioningStartTimeSec = INVALID_TIME;
        mIsProvisionRequest = false;
        mIsCanceled = false;
    }

    private static void logd(@NonNull String log) {
        if (DBG) {
            Log.d(TAG, log);
        }
    }
}

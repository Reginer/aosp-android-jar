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
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.metrics.SatelliteStats;
import com.android.internal.telephony.satellite.SatelliteConstants;

public class EntitlementMetricsStats {
    private static final String TAG = EntitlementMetricsStats.class.getSimpleName();
    private static EntitlementMetricsStats sInstance = null;
    private static final int RESULT_SUCCESS = 200;

    private int mSubId;
    private int mResult;
    private int mEntitlementStatus;
    private boolean mIsRetry;

    private EntitlementMetricsStats() {}

    /**
     * Returns the Singleton instance of EntitlementMetricsStats class.
     * If an instance of the Singleton class has not been created,
     * it creates a new instance and returns it. Otherwise, it returns
     * the existing instance.
     * @return the Singleton instance of EntitlementMetricsStats
     */
    public static EntitlementMetricsStats getOrCreateInstance() {
        if (sInstance == null) {
            logd("Create new EntitlementMetricsStats.");
            sInstance = new EntitlementMetricsStats();
        }
        return sInstance;
    }

    /** Report metrics on entitlement query request success */
    public void reportSuccess(int subId,
            @SatelliteConstants.SatelliteEntitlementStatus int entitlementStatus,
            boolean isRetry) {
        mSubId = subId;
        mResult = RESULT_SUCCESS;
        mEntitlementStatus = entitlementStatus;
        mIsRetry = isRetry;
        reportEntitlementMetrics();
    }

    /** Report metrics on entitlement query request error */
    public void reportError(int subId, int result, boolean isRetry) {
        mSubId = subId;
        mResult = result;
        mIsRetry = isRetry;
        mEntitlementStatus = SatelliteConstants.SATELLITE_ENTITLEMENT_STATUS_UNKNOWN;
        reportEntitlementMetrics();
    }

    /** Report entitlement metrics atom to PersistAtomsStorage in telephony */
    private void reportEntitlementMetrics() {
        SatelliteStats.SatelliteEntitlementParams entitlementParams =
                new SatelliteStats.SatelliteEntitlementParams.Builder()
                        .setCarrierId(getCarrierId(mSubId))
                        .setResult(mResult)
                        .setEntitlementStatus(mEntitlementStatus)
                        .setIsRetry(mIsRetry)
                        .setCount(1)
                        .build();
        SatelliteStats.getInstance().onSatelliteEntitlementMetrics(entitlementParams);
        logd("reportEntitlementMetrics: " + entitlementParams);

        CarrierRoamingSatelliteControllerStats.getOrCreateInstance()
                .reportCountOfEntitlementStatusQueryRequest();
    }

    /** Returns the carrier ID of the given subscription id. */
    private int getCarrierId(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        Phone phone = PhoneFactory.getPhone(phoneId);
        return phone != null ? phone.getCarrierId() : TelephonyManager.UNKNOWN_CARRIER_ID;
    }

    private static void logd(@NonNull String log) {
        Log.d(TAG, log);
    }
}

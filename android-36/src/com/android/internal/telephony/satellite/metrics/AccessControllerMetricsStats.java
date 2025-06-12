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

import static android.telephony.TelephonyManager.UNKNOWN_CARRIER_ID;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_SUCCESS;

import static com.android.internal.telephony.satellite.SatelliteConstants.ACCESS_CONTROL_TYPE_UNKNOWN;
import static com.android.internal.telephony.satellite.SatelliteConstants.CONFIG_DATA_SOURCE_UNKNOWN;
import static com.android.internal.telephony.satellite.SatelliteConstants.TRIGGERING_EVENT_UNKNOWN;

import android.annotation.NonNull;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.metrics.SatelliteStats;
import com.android.internal.telephony.satellite.SatelliteConstants;

import java.util.Arrays;
import java.util.List;
public class AccessControllerMetricsStats {
    private static final String TAG = AccessControllerMetricsStats.class.getSimpleName();
    private static AccessControllerMetricsStats sInstance = null;

    private @SatelliteConstants.AccessControlType int mAccessControlType;
    private long mLocationQueryTimeMillis;
    private long mOnDeviceLookupTimeMillis;
    private long mTotalCheckingTimeMillis;
    private Boolean mIsAllowed;
    private Boolean mIsEmergency;
    private @SatelliteManager.SatelliteResult int mResultCode;
    private String[] mCountryCodes;
    private @SatelliteConstants.ConfigDataSource int mConfigDataSource;
    private int mCarrierId;
    private @SatelliteConstants.TriggeringEvent int mTriggeringEvent;
    private boolean mIsNtnOnlyCarrier;

    private AccessControllerMetricsStats() {
        initializeAccessControllerMetricsParam();
    }

    /**
     * Returns the Singleton instance of AccessControllerMetricsStats class.
     * If an instance of the Singleton class has not been created,
     * it creates a new instance and returns it. Otherwise, it returns
     * the existing instance.
     * @return the Singleton instance of AccessControllerMetricsStats.
     */
    public static AccessControllerMetricsStats getInstance() {
        if (sInstance == null) {
            loge("create new AccessControllerMetricsStats.");
            sInstance = new AccessControllerMetricsStats();
        }
        return sInstance;
    }
    private void initializeAccessControllerMetricsParam() {
        mAccessControlType = ACCESS_CONTROL_TYPE_UNKNOWN;
        mLocationQueryTimeMillis = 0;
        mOnDeviceLookupTimeMillis = 0;
        mTotalCheckingTimeMillis = 0;
        mIsAllowed = null;
        mIsEmergency = null;
        mResultCode = SATELLITE_RESULT_SUCCESS;
        mCountryCodes = new String[0];
        mConfigDataSource = CONFIG_DATA_SOURCE_UNKNOWN;
        mCarrierId = UNKNOWN_CARRIER_ID;
        mTriggeringEvent = TRIGGERING_EVENT_UNKNOWN;
        mIsNtnOnlyCarrier = false;
    }
    /**
     * Sets the Access Control Type for current satellite enablement.
     * @param accessControlType access control type of location query is attempted.
     */
    public AccessControllerMetricsStats setAccessControlType(
            @SatelliteConstants.AccessControlType int accessControlType) {
        mAccessControlType = accessControlType;
        logd("setAccessControlType: access control type = " + mAccessControlType);
        return this;
    }
    /**
     * Sets the location query time for current satellite enablement.
     * @param queryStartTime the time location query is attempted.
     */
    public AccessControllerMetricsStats setLocationQueryTime(long queryStartTime) {
        mLocationQueryTimeMillis =
                (queryStartTime > 0) ? (getCurrentTime() - queryStartTime) : 0;
        logd("setLocationQueryTimeMillis: location query time = " + mLocationQueryTimeMillis);
        return this;
    }
    /**
     * Sets the on device lookup time for current satellite enablement.
     * @param onDeviceLookupStartTime the time on device lookup is attempted.
     */
    public AccessControllerMetricsStats setOnDeviceLookupTime(long onDeviceLookupStartTime) {
        mOnDeviceLookupTimeMillis =
                (onDeviceLookupStartTime > 0) ? (getCurrentTime() - onDeviceLookupStartTime) : 0;
        logd("setLocationQueryTime: on device lookup time = " + mOnDeviceLookupTimeMillis);
        return this;
    }
    /**
     * Sets the total checking time for current satellite enablement.
     * @param queryStartTime the time location query is attempted.
     */
    public AccessControllerMetricsStats setTotalCheckingTime(long queryStartTime) {
        mTotalCheckingTimeMillis =
                (queryStartTime > 0) ? (getCurrentTime() - queryStartTime) : 0;
        logd("setTotalCheckingTime: location query time = " + mTotalCheckingTimeMillis);
        return this;
    }
    /**
     * Sets whether the satellite communication is allowed from current location.
     * @param isAllowed {@code true} if satellite communication is allowed from current location
     *        {@code false} otherwise.
     */
    public AccessControllerMetricsStats setIsAllowed(boolean isAllowed) {
        mIsAllowed = isAllowed;
        logd("setIsAllowed: allowed=" + mIsAllowed);
        return this;
    }
    /**
     * Sets whether the current satellite enablement is for emergency or not.
     * @param isEmergency {@code true} if current satellite enablement is for emergency SOS message
     *        {@code false} otherwise.
     */
    public AccessControllerMetricsStats setIsEmergency(boolean isEmergency) {
        mIsEmergency = isEmergency;
        logd("setIsEmergency: emergency =" + mIsEmergency);
        return this;
    }
    /**
     * Sets the result code for checking whether satellite service is allowed from current
     * location.
     * @param result result code for checking process.
     */
    public AccessControllerMetricsStats setResult(@SatelliteManager.SatelliteResult int result) {
        mResultCode = result;
        logd("setResult: result = " + mResultCode);
        return this;
    }
    /**
     * Sets the country code for current location while attempting satellite enablement.
     * @param countryCodes Country code the user is located in
     */
    public AccessControllerMetricsStats setCountryCodes(List<String> countryCodes) {
        mCountryCodes = countryCodes.stream().toArray(String[]::new);
        logd("setCountryCodes: country code is " + Arrays.toString(mCountryCodes));
        return this;
    }
    /**
     * Sets the config data source for checking whether satellite service is allowed from current
     * location.
     * @param configDatasource configuration data source.
     */
    public AccessControllerMetricsStats setConfigDataSource(
            @SatelliteConstants.ConfigDataSource int configDatasource) {
        mConfigDataSource = configDatasource;
        logd("setConfigDataSource: config data source = " + mConfigDataSource);
        return this;
    }
    /**
     * Sets the carrier id for NTN satellite service.
     * @param carrierId Carrier ID of currently available NTN Satellite Network.
     */
    public AccessControllerMetricsStats setCarrierId(int carrierId) {
        mCarrierId = carrierId;
        logd("setCarrierId: Carrier ID = " + mCarrierId);
        return this;
    }
    /**
     * Sets the triggering event for satellite access controller operation.
     * @param triggeringEvent triggering event.
     */
    public AccessControllerMetricsStats setTriggeringEvent(
            @SatelliteConstants.TriggeringEvent int triggeringEvent) {
        mTriggeringEvent = triggeringEvent;
        logd("setTriggeringEvent: triggering event = " + mTriggeringEvent);
        return this;
    }

    /**
     * Sets the value of isNtnOnlyCarrier for current satellite enablement.
     * @param isNtnOnlyCarrier {@code true} if the carrier is NTN only carrier.
    */
    public AccessControllerMetricsStats setIsNtnOnlyCarrier(boolean isNtnOnlyCarrier) {
        mIsNtnOnlyCarrier = isNtnOnlyCarrier;
        logd("setIsNtnOnlyCarrier: isNtnOnlyCarrier = " + mIsNtnOnlyCarrier);
        return this;
    }

    /** Report the access controller metrics atoms to PersistAtomsStorage in telephony. */
    public void reportAccessControllerMetrics() {
        SatelliteStats.SatelliteAccessControllerParams accessControllerParams =
                new SatelliteStats.SatelliteAccessControllerParams.Builder()
                        .setAccessControlType(mAccessControlType)
                        .setLocationQueryTime(mLocationQueryTimeMillis)
                        .setOnDeviceLookupTime(mOnDeviceLookupTimeMillis)
                        .setTotalCheckingTime(mTotalCheckingTimeMillis)
                        .setIsAllowed(mIsAllowed)
                        .setIsEmergency(mIsEmergency)
                        .setResult(mResultCode)
                        .setCountryCodes(mCountryCodes)
                        .setConfigDatasource(mConfigDataSource)
                        .setCarrierId(mCarrierId)
                        .setTriggeringEvent(mTriggeringEvent)
                        .setIsNtnOnlyCarrier(mIsNtnOnlyCarrier)
                        .build();
        logd("reportAccessControllerMetrics: " + accessControllerParams.toString());
        SatelliteStats.getInstance().onSatelliteAccessControllerMetrics(accessControllerParams);
        initializeAccessControllerMetricsParam();
    }
    @VisibleForTesting
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }
    private static void logd(@NonNull String log) {
        Log.d(TAG, log);
    }
    private static void loge(@NonNull String log) {
        Log.e(TAG, log);
    }
}

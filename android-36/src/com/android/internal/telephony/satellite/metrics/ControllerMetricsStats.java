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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.metrics.SatelliteStats;
import com.android.internal.telephony.satellite.SatelliteServiceUtils;

/**
 * Stats to log to satellite metrics
 */
public class ControllerMetricsStats {
    private static final int ADD_COUNT = 1;
    private static final String TAG = ControllerMetricsStats.class.getSimpleName();

    private static ControllerMetricsStats sInstance;

    private final Context mContext;
    private SatelliteStats mSatelliteStats;

    private long mSatelliteOnTimeMillis;
    private int mBatteryLevelWhenServiceOn;
    private boolean mIsSatelliteModemOn;
    private Boolean mIsBatteryCharged = null;
    private int mBatteryChargedStartTimeSec;
    private int mTotalBatteryChargeTimeSec;

    /**
     * @return The singleton instance of ControllerMetricsStats.
     */
    public static ControllerMetricsStats getInstance() {
        if (sInstance == null) {
            loge("ControllerMetricsStats was not yet initialized.");
        }
        return sInstance;
    }

    /**
     * Create the ControllerMetricsStats singleton instance.
     *
     * @param context   The Context for the ControllerMetricsStats.
     * @return          The singleton instance of ControllerMetricsStats.
     */
    public static ControllerMetricsStats make(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new ControllerMetricsStats(context);
        }
        return sInstance;
    }

    /**
     * Create the ControllerMetricsStats singleton instance, testing purpose only.
     *
     * @param context   The Context for the ControllerMetricsStats.
     * @param satelliteStats SatelliteStats instance to test
     * @return          The singleton instance of ControllerMetricsStats.
     */
    @VisibleForTesting
    public static ControllerMetricsStats make(@NonNull Context context,
            @NonNull SatelliteStats satelliteStats) {
        if (sInstance == null) {
            sInstance = new ControllerMetricsStats(context, satelliteStats);
        }
        return sInstance;
    }

    /**
     * Create the ControllerMetricsStats to manage metrics report for
     * {@link SatelliteStats.SatelliteControllerParams}
     * @param context The Context for the ControllerMetricsStats.
     */
    ControllerMetricsStats(@NonNull Context context) {
        mContext = context;
        mSatelliteStats = SatelliteStats.getInstance();
    }

    /**
     * Create the ControllerMetricsStats to manage metrics report for
     * {@link SatelliteStats.SatelliteControllerParams}
     *
     * @param context           The Context for the ControllerMetricsStats.
     * @param satelliteStats    SatelliteStats object used for testing purpose
     */
    @VisibleForTesting
    protected ControllerMetricsStats(@NonNull Context context,
            @NonNull SatelliteStats satelliteStats) {
        mContext = context;
        mSatelliteStats = satelliteStats;
    }

    /** Report a counter when an attempt for satellite service on is successfully done */
    public void reportServiceEnablementSuccessCount() {
        logd("reportServiceEnablementSuccessCount()");
        mSatelliteStats.onSatelliteControllerMetrics(
                new SatelliteStats.SatelliteControllerParams.Builder()
                        .setCountOfSatelliteServiceEnablementsSuccess(ADD_COUNT)
                        .build());
    }

    /** Report a counter when an attempt for satellite service on is failed */
    public void reportServiceEnablementFailCount() {
        logd("reportServiceEnablementFailCount()");
        mSatelliteStats.onSatelliteControllerMetrics(
                new SatelliteStats.SatelliteControllerParams.Builder()
                        .setCountOfSatelliteServiceEnablementsFail(ADD_COUNT)
                        .build());
    }

    /** Report a counter when an attempt for outgoing datagram is successfully done */
    public void reportOutgoingDatagramSuccessCount(
            @NonNull @SatelliteManager.DatagramType int datagramType, boolean isDemoMode) {
        SatelliteStats.SatelliteControllerParams.Builder builder =
                new SatelliteStats.SatelliteControllerParams.Builder();

        if (isDemoMode) {
            builder.setCountOfDemoModeOutgoingDatagramSuccess(ADD_COUNT);
        } else {
            builder.setCountOfOutgoingDatagramSuccess(ADD_COUNT);
            if (SatelliteServiceUtils.isSosMessage(datagramType)) {
                builder.setCountOfDatagramTypeSosSmsSuccess(ADD_COUNT);
            } else if (datagramType == SatelliteManager.DATAGRAM_TYPE_LOCATION_SHARING) {
                builder.setCountOfDatagramTypeLocationSharingSuccess(ADD_COUNT);
            } else if (datagramType == SatelliteManager.DATAGRAM_TYPE_KEEP_ALIVE) {
                builder.setCountOfDatagramTypeKeepAliveSuccess(ADD_COUNT).build();
            }
        }

        SatelliteStats.SatelliteControllerParams controllerParam = builder.build();
        logd("reportServiceEnablementSuccessCount(): " + controllerParam);
        mSatelliteStats.onSatelliteControllerMetrics(controllerParam);
    }

    /** Report a counter when an attempt for outgoing datagram is failed */
    public void reportOutgoingDatagramFailCount(
            @NonNull @SatelliteManager.DatagramType int datagramType, boolean isDemoMode) {
        SatelliteStats.SatelliteControllerParams.Builder builder =
                new SatelliteStats.SatelliteControllerParams.Builder();

        if (isDemoMode) {
            builder.setCountOfDemoModeOutgoingDatagramFail(ADD_COUNT);
        } else {
            builder.setCountOfOutgoingDatagramFail(ADD_COUNT);
            if (SatelliteServiceUtils.isSosMessage(datagramType)) {
                builder.setCountOfDatagramTypeSosSmsFail(ADD_COUNT);
            } else if (datagramType == SatelliteManager.DATAGRAM_TYPE_LOCATION_SHARING) {
                builder.setCountOfDatagramTypeLocationSharingFail(ADD_COUNT);
            } else if (datagramType == SatelliteManager.DATAGRAM_TYPE_KEEP_ALIVE) {
                builder.setCountOfDatagramTypeKeepAliveFail(ADD_COUNT);
            }
        }

        SatelliteStats.SatelliteControllerParams controllerParam = builder.build();
        logd("reportOutgoingDatagramFailCount(): " + controllerParam);
        mSatelliteStats.onSatelliteControllerMetrics(controllerParam);
    }

    /** Report a counter when an attempt for incoming datagram is failed */
    public void reportIncomingDatagramCount(
            @NonNull @SatelliteManager.SatelliteResult int result, boolean isDemoMode) {
        SatelliteStats.SatelliteControllerParams.Builder builder =
                new SatelliteStats.SatelliteControllerParams.Builder();
        if (isDemoMode) {
            if (result == SatelliteManager.SATELLITE_RESULT_SUCCESS) {
                builder.setCountOfDemoModeIncomingDatagramSuccess(ADD_COUNT);
            } else {
                builder.setCountOfDemoModeIncomingDatagramFail(ADD_COUNT);
            }
        } else {
            if (result == SatelliteManager.SATELLITE_RESULT_SUCCESS) {
                builder.setCountOfIncomingDatagramSuccess(ADD_COUNT);
            } else {
                builder.setCountOfIncomingDatagramFail(ADD_COUNT);
            }
        }
        SatelliteStats.SatelliteControllerParams  controllerParam = builder.build();
        logd("reportIncomingDatagramCount(): " + controllerParam);
        mSatelliteStats.onSatelliteControllerMetrics(controllerParam);
    }

    /** Report a counter when an attempt for de-provision is success or not */
    public void reportProvisionCount(@NonNull @SatelliteManager.SatelliteResult int result) {
        SatelliteStats.SatelliteControllerParams controllerParam;
        if (result == SatelliteManager.SATELLITE_RESULT_SUCCESS) {
            controllerParam = new SatelliteStats.SatelliteControllerParams.Builder()
                    .setCountOfProvisionSuccess(ADD_COUNT)
                    .build();
        } else {
            controllerParam = new SatelliteStats.SatelliteControllerParams.Builder()
                    .setCountOfProvisionFail(ADD_COUNT)
                    .build();
        }
        logd("reportProvisionCount(): " + controllerParam);
        mSatelliteStats.onSatelliteControllerMetrics(controllerParam);
    }

    /** Report a counter when an attempt for de-provision is success or not */
    public void reportDeprovisionCount(@NonNull @SatelliteManager.SatelliteResult int result) {
        SatelliteStats.SatelliteControllerParams controllerParam;
        if (result == SatelliteManager.SATELLITE_RESULT_SUCCESS) {
            controllerParam = new SatelliteStats.SatelliteControllerParams.Builder()
                    .setCountOfDeprovisionSuccess(ADD_COUNT)
                    .build();
        } else {
            controllerParam = new SatelliteStats.SatelliteControllerParams.Builder()
                    .setCountOfDeprovisionFail(ADD_COUNT)
                    .build();
        }
        logd("reportDeprovisionCount(): " + controllerParam);
        mSatelliteStats.onSatelliteControllerMetrics(controllerParam);
    }

    /**
     * Report a counter when checking result whether satellite communication is allowed or not for
     * current location.
     */
    public void reportAllowedSatelliteAccessCount(boolean isAllowed) {
        SatelliteStats.SatelliteControllerParams.Builder builder;
        if (isAllowed) {
            builder = new SatelliteStats.SatelliteControllerParams.Builder()
                    .setCountOfAllowedSatelliteAccess(ADD_COUNT);
        } else {
            builder = new SatelliteStats.SatelliteControllerParams.Builder()
                    .setCountOfDisallowedSatelliteAccess(ADD_COUNT);
        }
        SatelliteStats.SatelliteControllerParams controllerParam = builder.build();
        logd("reportAllowedSatelliteAccessCount:" + controllerParam);
        mSatelliteStats.onSatelliteControllerMetrics(controllerParam);
    }

    /**
     * Report a counter when checking whether satellite communication for current location is
     * allowed has failed.
     */
    public void reportFailedSatelliteAccessCheckCount() {
        SatelliteStats.SatelliteControllerParams controllerParam =
                new SatelliteStats.SatelliteControllerParams.Builder()
                        .setCountOfSatelliteAccessCheckFail(ADD_COUNT).build();
        logd("reportFailedSatelliteAccessCheckCount:" + controllerParam);
        mSatelliteStats.onSatelliteControllerMetrics(controllerParam);
    }

    /** Return the total service up time for satellite service */
    @VisibleForTesting
    public int captureTotalServiceUpTimeSec() {
        long totalTimeMillis = getCurrentTime() - mSatelliteOnTimeMillis;
        mSatelliteOnTimeMillis = 0;
        return (int) (totalTimeMillis / 1000);
    }

    /** Return the total battery charge time while satellite service is on */
    @VisibleForTesting
    public int captureTotalBatteryChargeTimeSec() {
        int totalTime = mTotalBatteryChargeTimeSec;
        mTotalBatteryChargeTimeSec = 0;
        return totalTime;
    }

    /** Capture the satellite service on time and register battery monitor */
    public void onSatelliteEnabled() {
        if (!isSatelliteModemOn()) {
            mIsSatelliteModemOn = true;

            startCaptureBatteryLevel();

            // log the timestamp of the satellite modem power on
            mSatelliteOnTimeMillis = getCurrentTime();

            // register broadcast receiver for monitoring battery status change
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

            logd("register BatteryStatusReceiver");
            mContext.registerReceiver(mBatteryStatusReceiver, filter);
        }
    }

    /** Capture the satellite service off time and de-register battery monitor */
    public void onSatelliteDisabled() {
        if (isSatelliteModemOn()) {
            mIsSatelliteModemOn = false;

            logd("unregister BatteryStatusReceiver");
            mContext.unregisterReceiver(mBatteryStatusReceiver);

            int totalServiceUpTime = captureTotalServiceUpTimeSec();
            int batteryConsumptionPercent = captureTotalBatteryConsumptionPercent(mContext);
            int totalBatteryChargeTime = captureTotalBatteryChargeTimeSec();

            // report metrics about service up time and battery
            SatelliteStats.SatelliteControllerParams controllerParam =
                    new SatelliteStats.SatelliteControllerParams.Builder()
                            .setTotalServiceUptimeSec(totalServiceUpTime)
                            .setTotalBatteryConsumptionPercent(batteryConsumptionPercent)
                            .setTotalBatteryChargedTimeSec(totalBatteryChargeTime)
                            .build();
            logd("onSatelliteDisabled(): " + controllerParam);
            mSatelliteStats.onSatelliteControllerMetrics(controllerParam);
        }
    }

    /** Log the total battery charging time when satellite service is on */
    private void updateSatelliteBatteryChargeTime(boolean isCharged) {
        logd("updateSatelliteBatteryChargeTime(" + isCharged + ")");
        // update only when the charge state has changed
        if (mIsBatteryCharged == null || isCharged != mIsBatteryCharged) {
            mIsBatteryCharged = isCharged;

            // When charged, log the start time of battery charging
            if (isCharged) {
                mBatteryChargedStartTimeSec = (int) (getCurrentTime() / 1000);
                // When discharged, log the accumulated total battery charging time.
            } else {
                mTotalBatteryChargeTimeSec +=
                        (int) (getCurrentTime() / 1000)
                                - mBatteryChargedStartTimeSec;
                mBatteryChargedStartTimeSec = 0;
            }
        }
    }

    /** Capture the battery level when satellite service is on */
    @VisibleForTesting
    public void startCaptureBatteryLevel() {
        try {
            BatteryManager batteryManager = mContext.getSystemService(BatteryManager.class);
            mBatteryLevelWhenServiceOn =
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            logd("sBatteryLevelWhenServiceOn = " + mBatteryLevelWhenServiceOn);
        } catch (NullPointerException e) {
            loge("BatteryManager is null");
        }
    }

    /** Capture the total consumption level when service is off */
    @VisibleForTesting
    public int captureTotalBatteryConsumptionPercent(Context context) {
        try {
            BatteryManager batteryManager = context.getSystemService(BatteryManager.class);
            int currentLevel =
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            return Math.max((mBatteryLevelWhenServiceOn - currentLevel), 0);
        } catch (NullPointerException e) {
            loge("BatteryManager is null");
            return 0;
        }
    }

    /** Capture the latest provisioned state for satellite service */
    @VisibleForTesting
    public void setIsProvisioned(boolean isProvisioned) {
        logd("setIsProvisioned:" + isProvisioned);
        mSatelliteStats.onSatelliteControllerMetrics(
                new SatelliteStats.SatelliteControllerParams.Builder()
                        .setIsProvisioned(isProvisioned)
                        .build());
    }

    /** Capture the NB-IoT NTN carrier ID */
    public void setCarrierId(int carrierId) {
        logd("setCarrierId:" + carrierId);
        mSatelliteStats.onSatelliteControllerMetrics(
                new SatelliteStats.SatelliteControllerParams.Builder()
                        .setCarrierId(carrierId)
                        .build());
    }

    /**
     * Report a counter when allowed state has changed.
     */
    public void reportAllowedStateChanged() {
        logd("reportAllowedStateChanged:");
        mSatelliteStats.onSatelliteControllerMetrics(
                new SatelliteStats.SatelliteControllerParams.Builder()
                        .setCountOfSatelliteAllowedStateChangedEvents(ADD_COUNT)
                        .build());
    }

    /**
     * Report a counter when location query was successful or failed.
     */
    public void reportLocationQuerySuccessful(boolean result) {
        SatelliteStats.SatelliteControllerParams.Builder builder;
        if (result) {
            builder = new SatelliteStats.SatelliteControllerParams.Builder()
                    .setCountOfSuccessfulLocationQueries(ADD_COUNT);
        } else {
            builder = new SatelliteStats.SatelliteControllerParams.Builder()
                    .setCountOfFailedLocationQueries(ADD_COUNT);
        }
        SatelliteStats.SatelliteControllerParams controllerParam = builder.build();
        logd("reportLocationQuerySuccessful:" + controllerParam);
        mSatelliteStats.onSatelliteControllerMetrics(controllerParam);
    }

    /**
     * Report a current version of satellite access config.
     */
    public void reportCurrentVersionOfSatelliteAccessConfig(int version) {
        logd("reportCurrentVersionOfSatelliteAccessConfig:" + version);
        mSatelliteStats.onSatelliteControllerMetrics(
                new SatelliteStats.SatelliteControllerParams.Builder()
                        .setVersionOfSatelliteAccessControl(version)
                        .build());
    }

    /**
     * Add count when the notification for P2P SMS over satellite avaibility is shown or removed.
     */
    public void reportP2PSmsEligibilityNotificationsCount(boolean isEligible) {
        SatelliteStats.SatelliteControllerParams.Builder builder;
        if (isEligible) {
            builder = new SatelliteStats.SatelliteControllerParams.Builder()
                    .setCountOfP2PSmsAvailableNotificationShown(ADD_COUNT);
        } else {
            builder = new SatelliteStats.SatelliteControllerParams.Builder()
                    .setCountOfP2PSmsAvailableNotificationRemoved(ADD_COUNT);

        }
        SatelliteStats.SatelliteControllerParams controllerParam = builder.build();
        logd("reportP2PSmsEligibilityNotificationsCount:" + controllerParam);
        mSatelliteStats.onSatelliteControllerMetrics(controllerParam);
    }

    /** Capture the latest provisioned state for satellite service */
    public void setIsNtnOnlyCarrier(boolean isNtnOnlyCarrier) {
        logd("setIsNtnOnlyCarrier:" + isNtnOnlyCarrier);
        mSatelliteStats.onSatelliteControllerMetrics(
                new SatelliteStats.SatelliteControllerParams.Builder()
                        .setIsNtnOnlyCarrier(isNtnOnlyCarrier)
                        .build());
    }

    /** Receives the battery status whether it is in charging or not, update interval is 60 sec. */
    private final BroadcastReceiver mBatteryStatusReceiver = new BroadcastReceiver() {
        private long mLastUpdatedTime = 0;
        private static final long UPDATE_INTERVAL = 60 * 1000;

        @Override
        public void onReceive(Context context, Intent intent) {
            long currentTime = getCurrentTime();
            if (currentTime - mLastUpdatedTime > UPDATE_INTERVAL) {
                mLastUpdatedTime = currentTime;
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharged = (status == BatteryManager.BATTERY_STATUS_CHARGING);
                logd("Battery is charged(" + isCharged + ")");
                updateSatelliteBatteryChargeTime(isCharged);
            }
        }
    };

    @VisibleForTesting
    public boolean isSatelliteModemOn() {
        return mIsSatelliteModemOn;
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

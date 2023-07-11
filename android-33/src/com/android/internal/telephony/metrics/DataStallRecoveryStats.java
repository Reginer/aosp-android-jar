/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.telephony.metrics;

import android.telephony.Annotation.NetworkType;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.internal.telephony.data.DataStallRecoveryManager;
import com.android.internal.telephony.dataconnection.DcTracker;

/** Generates metrics related to data stall recovery events per phone ID for the pushed atom. */
public class DataStallRecoveryStats {
    /**
     * Create and push new atom when there is a data stall recovery event
     *
     * @param recoveryAction Data stall recovery action
     * @param phone
     */

    /* Since the Enum has been extended in Android T, we are mapping it to the correct number. */
    private static final int RECOVERY_ACTION_RADIO_RESTART_MAPPING = 3;
    private static final int RECOVERY_ACTION_RESET_MODEM_MAPPING = 4;


    /** TODO: b/214044479 : Remove this function when new data design(Android T) start. */
    public static void onDataStallEvent(
            @DcTracker.RecoveryAction int recoveryAction,
            Phone phone,
            boolean isRecovered,
            int durationMillis) {
        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_IMS) {
            phone = phone.getDefaultPhone();
        }

        int carrierId = phone.getCarrierId();
        int rat = getRat(phone);
        int band =
                (rat == TelephonyManager.NETWORK_TYPE_IWLAN) ? 0 : ServiceStateStats.getBand(phone);
        // the number returned here matches the SignalStrength enum we have
        int signalStrength = phone.getSignalStrength().getLevel();
        boolean isOpportunistic = getIsOpportunistic(phone);
        boolean isMultiSim = SimSlotState.getCurrentState().numActiveSims > 1;

        // Not use this field in Android S, so we send RECOVERED_REASON_NONE for default value.
        int recoveryReason = 0;
        TelephonyStatsLog.write(
                TelephonyStatsLog.DATA_STALL_RECOVERY_REPORTED,
                carrierId,
                rat,
                signalStrength,
                recoveryAction,
                isOpportunistic,
                isMultiSim,
                band,
                isRecovered,
                durationMillis,
                recoveryReason);
    }

    /**
     * Called when data stall happened.
     *
     * @param recoveryAction The recovery action.
     * @param phone The phone instance.
     * @param isRecovered The data stall symptom recovered or not.
     * @param durationMillis The duration from data stall symptom occurred.
     * @param reason The recovered(data resume) reason.
     */
    public static void onDataStallEvent(
            @DataStallRecoveryManager.RecoveryAction int recoveryAction,
            Phone phone,
            boolean isRecovered,
            int durationMillis,
            @DataStallRecoveryManager.RecoveredReason int reason) {
        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_IMS) {
            phone = phone.getDefaultPhone();
        }

        int carrierId = phone.getCarrierId();
        int rat = getRat(phone);
        int band =
                (rat == TelephonyManager.NETWORK_TYPE_IWLAN) ? 0 : ServiceStateStats.getBand(phone);
        // the number returned here matches the SignalStrength enum we have
        int signalStrength = phone.getSignalStrength().getLevel();
        boolean isOpportunistic = getIsOpportunistic(phone);
        boolean isMultiSim = SimSlotState.getCurrentState().numActiveSims > 1;

        if (recoveryAction == DataStallRecoveryManager.RECOVERY_ACTION_RADIO_RESTART) {
            recoveryAction = RECOVERY_ACTION_RADIO_RESTART_MAPPING;
        } else if (recoveryAction == DataStallRecoveryManager.RECOVERY_ACTION_RESET_MODEM) {
            recoveryAction = RECOVERY_ACTION_RESET_MODEM_MAPPING;
        }

        TelephonyStatsLog.write(
                TelephonyStatsLog.DATA_STALL_RECOVERY_REPORTED,
                carrierId,
                rat,
                signalStrength,
                recoveryAction,
                isOpportunistic,
                isMultiSim,
                band,
                isRecovered,
                durationMillis,
                reason);
    }

    /** Returns the RAT used for data (including IWLAN). */
    private static @NetworkType int getRat(Phone phone) {
        ServiceStateTracker serviceStateTracker = phone.getServiceStateTracker();
        ServiceState serviceState =
                serviceStateTracker != null ? serviceStateTracker.getServiceState() : null;
        return serviceState != null
                ? serviceState.getDataNetworkType()
                : TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }

    private static boolean getIsOpportunistic(Phone phone) {
        SubscriptionController subController = SubscriptionController.getInstance();
        return subController != null ? subController.isOpportunistic(phone.getSubId()) : false;
    }
}

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
import com.android.internal.telephony.dataconnection.DcTracker;

/** Generates metrics related to data stall recovery events per phone ID for the pushed atom. */
public class DataStallRecoveryStats {
    /**
     * Create and push new atom when there is a data stall recovery event
     *
     * @param recoveryAction Data stall recovery action
     * @param phone
     */
    public static void onDataStallEvent(@DcTracker.RecoveryAction int recoveryAction,
            Phone phone, boolean isRecovered, int durationMillis) {
        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_IMS) {
            phone = phone.getDefaultPhone();
        }

        int carrierId = phone.getCarrierId();
        int rat = getRat(phone);
        int band = ServiceStateStats.getBand(phone, rat);
        // the number returned here matches the SignalStrength enum we have
        int signalStrength = phone.getSignalStrength().getLevel();
        boolean isOpportunistic = getIsOpportunistic(phone);
        boolean isMultiSim = SimSlotState.getCurrentState().numActiveSims > 1;

        TelephonyStatsLog.write(TelephonyStatsLog.DATA_STALL_RECOVERY_REPORTED, carrierId, rat,
                signalStrength, recoveryAction, isOpportunistic, isMultiSim, band, isRecovered,
                durationMillis);
    }

    /** Returns the RAT used for data (including IWLAN). */
    private static @NetworkType int getRat(Phone phone) {
        ServiceStateTracker serviceStateTracker = phone.getServiceStateTracker();
        ServiceState serviceState =
                serviceStateTracker != null ? serviceStateTracker.getServiceState() : null;
        return serviceState != null ? serviceState.getDataNetworkType()
                : TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }

    private static boolean getIsOpportunistic(Phone phone) {
        SubscriptionController subController = SubscriptionController.getInstance();
        return subController != null ? subController.isOpportunistic(phone.getSubId()) : false;
    }
}

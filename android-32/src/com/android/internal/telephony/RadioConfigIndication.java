/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.telephony;

import android.hardware.radio.config.V1_2.IRadioConfigIndication;
import android.os.AsyncResult;

import com.android.internal.telephony.uicc.IccSlotStatus;
import com.android.telephony.Rlog;

import java.util.ArrayList;

/**
 * This class is the implementation of IRadioConfigIndication interface.
 */
public class RadioConfigIndication extends IRadioConfigIndication.Stub {
    private final RadioConfig mRadioConfig;
    private static final String TAG = "RadioConfigIndication";

    public RadioConfigIndication(RadioConfig radioConfig) {
        mRadioConfig = radioConfig;
    }

    /**
     * Unsolicited indication for slot status changed
     */
    public void simSlotsStatusChanged(int indicationType,
            ArrayList<android.hardware.radio.config.V1_0.SimSlotStatus> slotStatus) {
        ArrayList<IccSlotStatus> ret = RadioConfig.convertHalSlotStatus(slotStatus);
        Rlog.d(TAG, "[UNSL]< " + " UNSOL_SIM_SLOT_STATUS_CHANGED " + ret.toString());
        if (mRadioConfig.mSimSlotStatusRegistrant != null) {
            mRadioConfig.mSimSlotStatusRegistrant.notifyRegistrant(
                    new AsyncResult(null, ret, null));
        }
    }

    /**
     * Unsolicited indication for slot status changed
     */
    public void simSlotsStatusChanged_1_2(int indicationType,
            ArrayList<android.hardware.radio.config.V1_2.SimSlotStatus> slotStatus) {
        ArrayList<IccSlotStatus> ret = RadioConfig.convertHalSlotStatus_1_2(slotStatus);
        Rlog.d(TAG, "[UNSL]< " + " UNSOL_SIM_SLOT_STATUS_CHANGED " + ret.toString());
        if (mRadioConfig.mSimSlotStatusRegistrant != null) {
            mRadioConfig.mSimSlotStatusRegistrant.notifyRegistrant(
                    new AsyncResult(null, ret, null));
        }
    }
}

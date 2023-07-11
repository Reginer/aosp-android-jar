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

import android.os.AsyncResult;

import com.android.internal.telephony.uicc.IccSlotStatus;
import com.android.telephony.Rlog;

import java.util.ArrayList;

/**
 * This class is the AIDL implementation of IRadioConfigIndication interface.
 */
public class RadioConfigIndicationAidl extends
        android.hardware.radio.config.IRadioConfigIndication.Stub {
    private static final String TAG = "RadioConfigIndicationAidl";

    private final RadioConfig mRadioConfig;

    public RadioConfigIndicationAidl(RadioConfig radioConfig) {
        mRadioConfig = radioConfig;
    }

    /**
     * Unsolicited indication for slot status changed
     */
    @Override
    public void simSlotsStatusChanged(
            int type, android.hardware.radio.config.SimSlotStatus[] slotStatus) {
        ArrayList<IccSlotStatus> ret = RILUtils.convertHalSlotStatus(slotStatus);
        logd("[UNSL]< UNSOL_SIM_SLOT_STATUS_CHANGED " + ret.toString());
        if (mRadioConfig.mSimSlotStatusRegistrant != null) {
            mRadioConfig.mSimSlotStatusRegistrant.notifyRegistrant(
                    new AsyncResult(null, ret, null));
        }
    }

    private static void logd(String log) {
        Rlog.d(TAG, log);
    }

    @Override
    public String getInterfaceHash() {
        return android.hardware.radio.config.IRadioConfigIndication.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return android.hardware.radio.config.IRadioConfigIndication.VERSION;
    }
}

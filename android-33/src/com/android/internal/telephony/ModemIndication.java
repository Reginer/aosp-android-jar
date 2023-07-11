/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.internal.telephony.RILConstants.RIL_UNSOL_HARDWARE_CONFIG_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_MODEM_RESTART;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RADIO_CAPABILITY;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RIL_CONNECTED;

import android.hardware.radio.modem.IRadioModemIndication;
import android.os.AsyncResult;

import java.util.ArrayList;

/**
 * Interface declaring unsolicited radio indications for modem APIs.
 */
public class ModemIndication extends IRadioModemIndication.Stub {
    private final RIL mRil;

    public ModemIndication(RIL ril) {
        mRil = ril;
    }

    /**
     * Indicates when the hardware configuration associated with the RILd changes.
     * @param indicationType Type of radio indication
     * @param configs Array of hardware configs
     */
    public void hardwareConfigChanged(int indicationType,
            android.hardware.radio.modem.HardwareConfig[] configs) {
        mRil.processIndication(RIL.MODEM_SERVICE, indicationType);

        ArrayList<HardwareConfig> response = RILUtils.convertHalHardwareConfigList(configs);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_HARDWARE_CONFIG_CHANGED, response);

        mRil.mHardwareConfigChangeRegistrants.notifyRegistrants(
                new AsyncResult(null, response, null));
    }

    /**
     * Indicates when there is a modem reset.
     * @param indicationType Type of radio indication
     * @param reason The reason for the reset. It may be a crash signature if the restart was due to
     *        a crash or some string such as "user-initiated restart" or "AT command initiated
     *        restart" that explains the cause of the modem restart
     */
    public void modemReset(int indicationType, String reason) {
        mRil.processIndication(RIL.MODEM_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_MODEM_RESTART, reason);

        mRil.writeMetricsModemRestartEvent(reason);
        mRil.mModemResetRegistrants.notifyRegistrants(new AsyncResult(null, reason, null));
    }

    /**
     * Sent when setRadioCapability() completes. Returns the same RadioCapability as
     * getRadioCapability() and is the same as the one sent by setRadioCapability().
     * @param indicationType Type of radio indication
     * @param radioCapability Current radio capability
     */
    public void radioCapabilityIndication(int indicationType,
            android.hardware.radio.modem.RadioCapability radioCapability) {
        mRil.processIndication(RIL.MODEM_SERVICE, indicationType);

        RadioCapability response = RILUtils.convertHalRadioCapability(radioCapability, mRil);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_RADIO_CAPABILITY, response);

        mRil.mPhoneRadioCapabilityChangedRegistrants.notifyRegistrants(
                new AsyncResult(null, response, null));
    }

    /**
     * Indicates when radio state changes.
     * @param indicationType Type of radio indication
     * @param radioState Current radio state
     */
    public void radioStateChanged(int indicationType, int radioState) {
        mRil.processIndication(RIL.MODEM_SERVICE, indicationType);

        int state = RILUtils.convertHalRadioState(radioState);
        if (RIL.RILJ_LOGD) {
            mRil.unsljLogMore(RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED, "radioStateChanged: "
                    + state);
        }

        mRil.setRadioState(state, false /* forceNotifyRegistrants */);
    }

    /**
     * Indicates the ril connects and returns the version.
     * @param indicationType Type of radio indication
     */
    public void rilConnected(int indicationType) {
        mRil.processIndication(RIL.MODEM_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RIL_CONNECTED);

        // Initial conditions
        mRil.setRadioPower(false, null);
        mRil.setCdmaSubscriptionSource(mRil.mCdmaSubscription, null);
        // TODO: This should not require a version number. Setting it to latest RIL version for now.
        mRil.notifyRegistrantsRilConnectionChanged(15);
    }

    @Override
    public String getInterfaceHash() {
        return IRadioModemIndication.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return IRadioModemIndication.VERSION;
    }
}

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

import android.hardware.radio.RadioError;
import android.hardware.radio.RadioResponseInfo;
import android.hardware.radio.modem.IRadioModemResponse;
import android.os.SystemClock;
import android.telephony.ActivityStatsTechSpecificInfo;
import android.telephony.AnomalyReporter;
import android.telephony.ModemActivityInfo;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Interface declaring response functions to solicited radio requests for modem APIs.
 */
public class ModemResponse extends IRadioModemResponse.Stub {
    private final RIL mRil;

    public ModemResponse(RIL ril) {
        mRil = ril;
    }

    /**
     * Acknowledge the receipt of radio request sent to the vendor. This must be sent only for
     * radio request which take long time to respond.
     * For more details, refer https://source.android.com/devices/tech/connect/ril.html
     * @param serial Serial no. of the request whose acknowledgement is sent.
     */
    public void acknowledgeRequest(int serial) {
        mRil.processRequestAck(serial);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial number and error.
     */
    public void enableModemResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MODEM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param version String containing version string for log reporting
     */
    public void getBasebandVersionResponse(RadioResponseInfo responseInfo, String version) {
        RadioResponse.responseString(RIL.MODEM_SERVICE, mRil, responseInfo, version);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param imei IMEI if GSM subscription is available
     * @param imeisv IMEISV if GSM subscription is available
     * @param esn ESN if CDMA subscription is available
     * @param meid MEID if CDMA subscription is available
     */
    public void getDeviceIdentityResponse(RadioResponseInfo responseInfo, String imei,
            String imeisv, String esn, String meid) {
        RadioResponse.responseStrings(
                RIL.MODEM_SERVICE, mRil, responseInfo, imei, imeisv, esn, meid);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param config Array of HardwareConfig of the radio
     */
    public void getHardwareConfigResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.modem.HardwareConfig[] config) {
        RILRequest rr = mRil.processResponse(RIL.MODEM_SERVICE, responseInfo);

        if (rr != null) {
            ArrayList<HardwareConfig> ret = RILUtils.convertHalHardwareConfigList(config);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param activityInfo modem activity information
     */
    public void getModemActivityInfoResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.modem.ActivityStatsInfo activityInfo) {
        RILRequest rr = mRil.processResponse(RIL.MODEM_SERVICE, responseInfo);

        if (rr != null) {
            ModemActivityInfo ret = null;
            ActivityStatsTechSpecificInfo[] astsi = null;
            if (responseInfo.error == RadioError.NONE) {
                final int sleepModeTimeMs = activityInfo.sleepModeTimeMs;
                final int idleModeTimeMs = activityInfo.idleModeTimeMs;
                int size = activityInfo.techSpecificInfo.length;
                astsi = new ActivityStatsTechSpecificInfo[size];
                for (int s = 0; s < size; s++) {
                    int rat = activityInfo.techSpecificInfo[s].rat;
                    int frequencyRange = activityInfo.techSpecificInfo[s].frequencyRange;
                    int [] txModeTimeMs = new int[ModemActivityInfo.getNumTxPowerLevels()];
                    int rxModeTimeMs = activityInfo.techSpecificInfo[s].rxModeTimeMs;
                    for (int i = 0; i < ModemActivityInfo.getNumTxPowerLevels(); i++) {
                        txModeTimeMs[i] = activityInfo.techSpecificInfo[s].txmModetimeMs[i];
                    }
                    astsi[s] = new ActivityStatsTechSpecificInfo(
                                    rat,
                                    frequencyRange,
                                    txModeTimeMs,
                                    rxModeTimeMs);
                }
                ret = new ModemActivityInfo(SystemClock.elapsedRealtime(), sleepModeTimeMs,
                        idleModeTimeMs, astsi);
            } else {
                astsi = new ActivityStatsTechSpecificInfo[1];
                astsi[0] = new ActivityStatsTechSpecificInfo(0, 0,
                        new int[ModemActivityInfo.getNumTxPowerLevels()], 0);
                ret = new ModemActivityInfo(SystemClock.elapsedRealtime(), 0, 0, astsi);
                responseInfo.error = RadioError.NONE;
            }
            RadioResponse.sendMessageResponse(rr.mResult, ret);
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param isEnabled whether the modem stack is enabled.
     */
    public void getModemStackStatusResponse(RadioResponseInfo responseInfo, boolean isEnabled) {
        RILRequest rr = mRil.processResponse(RIL.MODEM_SERVICE, responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, isEnabled);
            }
            mRil.processResponseDone(rr, responseInfo, isEnabled);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param radioCapability RadioCapability from the modem
     */
    public void getRadioCapabilityResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.modem.RadioCapability radioCapability) {
        RILRequest rr = mRil.processResponse(RIL.MODEM_SERVICE, responseInfo);

        if (rr != null) {
            RadioCapability ret = RILUtils.convertHalRadioCapability(radioCapability, mRil);
            if (responseInfo.error == RadioError.REQUEST_NOT_SUPPORTED
                    || responseInfo.error == RadioError.GENERIC_FAILURE) {
                // TODO: Construct the supported RAF bitmask based on preferred network bitmasks
                ret = mRil.makeStaticRadioCapability();
                responseInfo.error = RadioError.NONE;
            }
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result String containing the contents of the NV item
     */
    public void nvReadItemResponse(RadioResponseInfo responseInfo, String result) {
        RadioResponse.responseString(RIL.MODEM_SERVICE, mRil, responseInfo, result);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvResetConfigResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MODEM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvWriteCdmaPrlResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MODEM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvWriteItemResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MODEM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void requestShutdownResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MODEM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendDeviceStateResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MODEM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param radioCapability RadioCapability to set
     */
    public void setRadioCapabilityResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.modem.RadioCapability radioCapability) {
        RILRequest rr = mRil.processResponse(RIL.MODEM_SERVICE, responseInfo);

        if (rr != null) {
            RadioCapability ret = RILUtils.convertHalRadioCapability(radioCapability, mRil);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     */
    public void setRadioPowerResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MODEM_SERVICE, mRil, responseInfo);
        mRil.mLastRadioPowerResult = responseInfo.error;
        if (responseInfo.error == RadioError.RF_HARDWARE_ISSUE) {
            AnomalyReporter.reportAnomaly(
                    UUID.fromString(RILUtils.RADIO_POWER_FAILURE_RF_HARDWARE_ISSUE_UUID),
                    "RF HW damaged");
        } else if (responseInfo.error == RadioError.NO_RF_CALIBRATION_INFO) {
            AnomalyReporter.reportAnomaly(
                    UUID.fromString(RILUtils.RADIO_POWER_FAILURE_NO_RF_CALIBRATION_UUID),
                    "No RF calibration data");
        } else if (responseInfo.error != RadioError.RADIO_NOT_AVAILABLE
                && responseInfo.error != RadioError.NONE) {
            AnomalyReporter.reportAnomaly(
                    UUID.fromString(RILUtils.RADIO_POWER_FAILURE_BUGREPORT_UUID),
                    "Radio power failure");
        }
    }

    @Override
    public String getInterfaceHash() {
        return IRadioModemResponse.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return IRadioModemResponse.VERSION;
    }
}

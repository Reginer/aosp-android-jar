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
import android.hardware.radio.sim.IRadioSimResponse;
import android.telephony.CarrierRestrictionRules;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.uicc.AdnCapacity;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccIoResult;

import java.util.ArrayList;

/**
 * Interface declaring response functions to solicited radio requests for SIM APIs.
 */
public class SimResponse extends IRadioSimResponse.Stub {
    private final RIL mRil;

    public SimResponse(RIL ril) {
        mRil = ril;
    }

    private void responseIccIo(RadioResponseInfo responseInfo,
            android.hardware.radio.sim.IccIoResult result) {
        RILRequest rr = mRil.processResponse(RIL.SIM_SERVICE, responseInfo);

        if (rr != null) {
            IccIoResult ret = new IccIoResult(result.sw1, result.sw2, result.simResponse);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
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
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param enabled whether UICC applications are enabled.
     */
    public void areUiccApplicationsEnabledResponse(RadioResponseInfo responseInfo,
            boolean enabled) {
        RILRequest rr = mRil.processResponse(RIL.SIM_SERVICE, responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, enabled);
            }
            mRil.processResponseDone(rr, responseInfo, enabled);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void changeIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        RadioResponse.responseInts(RIL.SIM_SERVICE, mRil, responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void changeIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        RadioResponse.responseInts(RIL.SIM_SERVICE, mRil, responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     */
    public void enableUiccApplicationsResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.SIM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param carrierRestrictions Carrier restriction information.
     * @param multiSimPolicy Policy for multi-sim devices.
     */
    public void getAllowedCarriersResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.sim.CarrierRestrictions carrierRestrictions,
            int multiSimPolicy) {
        RILRequest rr = mRil.processResponse(RIL.SIM_SERVICE, responseInfo);
        if (rr == null) {
            return;
        }
        CarrierRestrictionRules ret;
        int policy = CarrierRestrictionRules.MULTISIM_POLICY_NONE;
        if (multiSimPolicy
                == android.hardware.radio.sim.SimLockMultiSimPolicy.ONE_VALID_SIM_MUST_BE_PRESENT) {
            policy = CarrierRestrictionRules.MULTISIM_POLICY_ONE_VALID_SIM_MUST_BE_PRESENT;
        }

        int carrierRestrictionDefault =
                CarrierRestrictionRules.CARRIER_RESTRICTION_DEFAULT_NOT_ALLOWED;
        if (!carrierRestrictions.allowedCarriersPrioritized) {
            carrierRestrictionDefault = CarrierRestrictionRules.CARRIER_RESTRICTION_DEFAULT_ALLOWED;
        }

        ret = CarrierRestrictionRules.newBuilder()
                .setAllowedCarriers(RILUtils.convertHalCarrierList(
                        carrierRestrictions.allowedCarriers))
                .setExcludedCarriers(RILUtils.convertHalCarrierList(
                        carrierRestrictions.excludedCarriers))
                .setDefaultCarrierRestriction(carrierRestrictionDefault)
                .setMultiSimPolicy(policy)
                .build();

        if (responseInfo.error == RadioError.NONE) {
            RadioResponse.sendMessageResponse(rr.mResult, ret);
        }
        mRil.processResponseDone(rr, responseInfo, ret);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param mdn MDN if CDMA subscription is available
     * @param hSid is a comma separated list of H_SID (Home SID) if
     *        CDMA subscription is available, in decimal format
     * @param hNid is a comma separated list of H_NID (Home NID) if
     *        CDMA subscription is available, in decimal format
     * @param min MIN (10 digits, MIN2+MIN1) if CDMA subscription is available
     * @param prl PRL version if CDMA subscription is available
     */
    public void getCdmaSubscriptionResponse(RadioResponseInfo responseInfo, String mdn,
            String hSid, String hNid, String min, String prl) {
        RadioResponse.responseStrings(
                RIL.SIM_SERVICE, mRil, responseInfo, mdn, hSid, hNid, min, prl);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param source CDMA subscription source
     */
    public void getCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo, int source) {
        RadioResponse.responseInts(RIL.SIM_SERVICE, mRil, responseInfo, source);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param response 0 is the TS 27.007 service class bit vector of services for which the
     *        specified barring facility is active. "0" means "disabled for all"
     */
    public void getFacilityLockForAppResponse(RadioResponseInfo responseInfo, int response) {
        RadioResponse.responseInts(RIL.SIM_SERVICE, mRil, responseInfo, response);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cardStatus ICC card status as defined by CardStatus
     */
    public void getIccCardStatusResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.sim.CardStatus cardStatus) {
        RILRequest rr = mRil.processResponse(RIL.SIM_SERVICE, responseInfo);

        if (rr != null) {
            IccCardStatus iccCardStatus = RILUtils.convertHalCardStatus(cardStatus);
            mRil.riljLog("responseIccCardStatus: from AIDL: " + iccCardStatus);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, iccCardStatus);
            }
            mRil.processResponseDone(rr, responseInfo, iccCardStatus);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param imsi String containing the IMSI
     */
    public void getImsiForAppResponse(RadioResponseInfo responseInfo, String imsi) {
        RadioResponse.responseString(RIL.SIM_SERVICE, mRil, responseInfo, imsi);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param pbCapacity Contains the adn, email, anr capacities in the sim card.
     */
    public void getSimPhonebookCapacityResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.sim.PhonebookCapacity pbCapacity) {
        AdnCapacity capacity = RILUtils.convertHalPhonebookCapacity(pbCapacity);
        RILRequest rr = mRil.processResponse(RIL.SIM_SERVICE, responseInfo);
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, capacity);
            }
            mRil.processResponseDone(rr, responseInfo, capacity);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     */
    public void getSimPhonebookRecordsResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.SIM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void iccCloseLogicalChannelResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.SIM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param iccIo ICC IO operation response as defined by IccIoResult
     */
    public void iccIoForAppResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.sim.IccIoResult iccIo) {
        responseIccIo(responseInfo, iccIo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param channelId session id of the logical channel.
     * @param selectResponse Contains the select response for the open channel command with one
     *        byte per integer
     */
    public void iccOpenLogicalChannelResponse(RadioResponseInfo responseInfo, int channelId,
            byte[] selectResponse) {
        ArrayList<Integer> arr = new ArrayList<>();
        arr.add(channelId);
        for (int i = 0; i < selectResponse.length; i++) {
            arr.add((int) selectResponse[i]);
        }
        RadioResponse.responseIntArrayList(RIL.SIM_SERVICE, mRil, responseInfo, arr);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param iccIo ICC IO operation response as defined by IccIoResult
     */
    public void iccTransmitApduBasicChannelResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.sim.IccIoResult iccIo) {
        responseIccIo(responseInfo, iccIo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param iccIo ICC IO operation response as defined by IccIoResult
     */
    public void iccTransmitApduLogicalChannelResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.sim.IccIoResult iccIo) {
        responseIccIo(responseInfo, iccIo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void reportStkServiceIsRunningResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.SIM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param iccIo ICC IO operation response as defined by IccIoResult
     */
    public void requestIccSimAuthenticationResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.sim.IccIoResult iccIo) {
        RILRequest rr = mRil.processResponse(RIL.SIM_SERVICE, responseInfo);

        if (rr != null) {
            IccIoResult ret = new IccIoResult(iccIo.sw1, iccIo.sw2,
                    TextUtils.isEmpty(iccIo.simResponse) ? null : iccIo.simResponse.getBytes());
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * This method is deprecated and should not be used.
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param response response string of the challenge/response algo for ISIM auth in base64 format
     */
    public void requestIsimAuthenticationResponse(RadioResponseInfo responseInfo, String response) {
        // TODO (b/199433581): remove this method
        throw new RuntimeException("Inexplicable response received for requestIsimAuthentication");
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param commandResponse SAT/USAT response in hexadecimal format
     *        string starting with first byte of response
     */
    public void sendEnvelopeResponse(RadioResponseInfo responseInfo, String commandResponse) {
        RadioResponse.responseString(RIL.SIM_SERVICE, mRil, responseInfo, commandResponse);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param iccIo ICC IO operation response as defined by IccIoResult
     */
    public void sendEnvelopeWithStatusResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.sim.IccIoResult iccIo) {
        responseIccIo(responseInfo, iccIo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendTerminalResponseToSimResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.SIM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setAllowedCarriersResponse(RadioResponseInfo responseInfo) {
        int ret = TelephonyManager.SET_CARRIER_RESTRICTION_ERROR;
        RILRequest rr = mRil.processResponse(RIL.SIM_SERVICE, responseInfo);
        if (rr != null) {
            mRil.riljLog("setAllowedCarriersResponse - error = " + responseInfo.error);

            if (responseInfo.error == RadioError.NONE) {
                ret = TelephonyManager.SET_CARRIER_RESTRICTION_SUCCESS;
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCarrierInfoForImsiEncryptionResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.SIM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.SIM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param retry 0 is the number of retries remaining, or -1 if unknown
     */
    public void setFacilityLockForAppResponse(RadioResponseInfo responseInfo, int retry) {
        RadioResponse.responseInts(RIL.SIM_SERVICE, mRil, responseInfo, retry);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSimCardPowerResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.SIM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setUiccSubscriptionResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.SIM_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        RadioResponse.responseInts(RIL.SIM_SERVICE, mRil, responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        RadioResponse.responseInts(RIL.SIM_SERVICE, mRil, responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPuk2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        RadioResponse.responseInts(RIL.SIM_SERVICE, mRil, responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPukForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        RadioResponse.responseInts(RIL.SIM_SERVICE, mRil, responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param persoType SIM Personalisation type
     * @param remainingRetries postiive values indicates number of retries remaining,
     * must be equal to -1 if number of retries is infinite.
     */
    public void supplySimDepersonalizationResponse(RadioResponseInfo responseInfo, int persoType,
            int remainingRetries) {
        RadioResponse.responseInts(
                RIL.SIM_SERVICE, mRil, responseInfo, persoType, remainingRetries);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param updatedRecordIndex The index of the updated record.
     */
    public void updateSimPhonebookRecordsResponse(RadioResponseInfo responseInfo,
            int updatedRecordIndex) {
        RadioResponse.responseInts(RIL.SIM_SERVICE, mRil, responseInfo, updatedRecordIndex);
    }

    @Override
    public String getInterfaceHash() {
        return IRadioSimResponse.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return IRadioSimResponse.VERSION;
    }
}

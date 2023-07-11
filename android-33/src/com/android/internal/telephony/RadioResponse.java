/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.hardware.radio.V1_0.ActivityStatsInfo;
import android.hardware.radio.V1_0.CardStatus;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.LastCallFailCauseInfo;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.LceStatusInfo;
import android.hardware.radio.V1_0.NeighboringCell;
import android.hardware.radio.V1_0.RadioError;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.RadioTechnologyFamily;
import android.hardware.radio.V1_0.SendSmsResult;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.hardware.radio.V1_4.CarrierRestrictionsWithPriority;
import android.hardware.radio.V1_4.SimLockMultiSimPolicy;
import android.hardware.radio.V1_6.IRadioResponse;
import android.hardware.radio.V1_6.SetupDataCallResult;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.AnomalyReporter;
import android.telephony.BarringInfo;
import android.telephony.CarrierRestrictionRules;
import android.telephony.CellInfo;
import android.telephony.LinkCapacityEstimate;
import android.telephony.ModemActivityInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessFamily;
import android.telephony.RadioAccessSpecifier;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.DataCallResponse;
import android.telephony.data.NetworkSlicingConfig;
import android.text.TextUtils;

import com.android.internal.telephony.data.KeepaliveStatus;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.uicc.AdnCapacity;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccSlotPortMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class RadioResponse extends IRadioResponse.Stub {
    RIL mRil;

    public RadioResponse(RIL ril) {
        mRil = ril;
    }

    /**
     * Helper function to send response msg
     * @param msg Response message to be sent
     * @param ret Return object to be included in the response message
     */
    static void sendMessageResponse(Message msg, Object ret) {
        if (msg != null) {
            AsyncResult.forMessage(msg, ret, null);
            msg.sendToTarget();
        }
    }

    /**
     * Acknowledge the receipt of radio request sent to the vendor. This must be sent only for
     * radio request which take long time to respond.
     * For more details, refer https://source.android.com/devices/tech/connect/ril.html
     *
     * @param serial Serial no. of the request whose acknowledgement is sent.
     */
    public void acknowledgeRequest(int serial) {
        mRil.processRequestAck(serial);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cardStatus ICC card status as defined by CardStatus in types.hal
     */
    public void getIccCardStatusResponse(RadioResponseInfo responseInfo, CardStatus cardStatus) {
        responseIccCardStatus(responseInfo, cardStatus);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cardStatus ICC card status as defined by CardStatus in 1.2/types.hal
     */
    public void getIccCardStatusResponse_1_2(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_2.CardStatus cardStatus) {
        responseIccCardStatus_1_2(responseInfo, cardStatus);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cardStatus ICC card status as defined by CardStatus in 1.4/types.hal
     */
    public void getIccCardStatusResponse_1_4(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_4.CardStatus cardStatus) {
        responseIccCardStatus_1_4(responseInfo, cardStatus);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cardStatus ICC card status as defined by CardStatus in 1.5/types.hal
     */
    public void getIccCardStatusResponse_1_5(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_5.CardStatus cardStatus) {
        responseIccCardStatus_1_5(responseInfo, cardStatus);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPukForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyIccPuk2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void changeIccPinForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param remainingAttempts Number of retries remaining, must be equal to -1 if unknown.
     */
    public void changeIccPin2ForAppResponse(RadioResponseInfo responseInfo, int remainingAttempts) {
        responseInts(responseInfo, remainingAttempts);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param retriesRemaining Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyNetworkDepersonalizationResponse(RadioResponseInfo responseInfo,
            int retriesRemaining) {
        responseInts(responseInfo, retriesRemaining);
    }


    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param persoType SIM Personalisation type
     * @param remainingRetries postiive values indicates number of retries remaining,
     * must be equal to -1 if number of retries is infinite.
     */
    public void supplySimDepersonalizationResponse(RadioResponseInfo info,
            int persoType, int remainingRetries) {
        responseInts(info, persoType, remainingRetries);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param calls Current call list
     */
    public void getCurrentCallsResponse(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.Call> calls) {
        responseCurrentCalls(responseInfo, calls);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param calls Current call list
     */
    public void getCurrentCallsResponse_1_2(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_2.Call> calls) {
        responseCurrentCalls_1_2(responseInfo, calls);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param calls Current call list
     */
    public void getCurrentCallsResponse_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_6.Call> calls) {
        responseCurrentCalls_1_6(responseInfo, calls);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void dialResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param imsi String containing the IMSI
     */
    public void getIMSIForAppResponse(RadioResponseInfo responseInfo, String imsi) {
        responseString(responseInfo, imsi);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupConnectionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupWaitingOrBackgroundResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void conferenceResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void rejectCallResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param fcInfo Contains LastCallFailCause and vendor cause code. GSM failure reasons
     *        are mapped to cause codes defined in TS 24.008 Annex H where possible. CDMA
     *        failure reasons are derived from the possible call failure scenarios
     *        described in the "CDMA IS-2000 Release A (C.S0005-A v6.0)" standard.
     */
    public void getLastCallFailCauseResponse(RadioResponseInfo responseInfo,
            LastCallFailCauseInfo fcInfo) {
        responseLastCallFailCauseInfo(responseInfo, fcInfo);
    }

    public void getSignalStrengthResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.SignalStrength sigStrength) {
        responseSignalStrength(responseInfo, sigStrength);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param signalStrength Current signal strength of camped/connected cells
     */
    public void getSignalStrengthResponse_1_2(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_2.SignalStrength signalStrength) {
        responseSignalStrength_1_2(responseInfo, signalStrength);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param signalStrength Current signal strength of camped/connected cells
     */
    public void getSignalStrengthResponse_1_4(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_4.SignalStrength signalStrength) {
        responseSignalStrength_1_4(responseInfo, signalStrength);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param signalStrength Current signal strength of camped/connected cells
     */
    public void getSignalStrengthResponse_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            android.hardware.radio.V1_6.SignalStrength signalStrength) {
        responseSignalStrength_1_6(responseInfo, signalStrength);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param voiceRegResponse Current Voice registration response as defined by VoiceRegStateResult
     *        in types.hal
     */
    public void getVoiceRegistrationStateResponse(RadioResponseInfo responseInfo,
            VoiceRegStateResult voiceRegResponse) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, voiceRegResponse);
            }
            mRil.processResponseDone(rr, responseInfo, voiceRegResponse);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param voiceRegResponse Current Voice registration response as defined by VoiceRegStateResult
     *        in 1.2/types.hal
     */
    public void getVoiceRegistrationStateResponse_1_2(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_2.VoiceRegStateResult voiceRegResponse) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, voiceRegResponse);
            }
            mRil.processResponseDone(rr, responseInfo, voiceRegResponse);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param voiceRegResponse Current Voice registration response as defined by VoiceRegStateResult
     *        in 1.5/types.hal
     */
    public void getVoiceRegistrationStateResponse_1_5(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_5.RegStateResult voiceRegResponse) {
        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr == null) {
            return;
        }

        if (responseInfo.error == RadioError.REQUEST_NOT_SUPPORTED) {
            // Move the data needed for fallback call from rr which will be released soon
            final int request = rr.getRequest();
            final Message result = rr.getResult();

            mRil.mRilHandler.post(() -> {
                mRil.setCompatVersion(request, RIL.RADIO_HAL_VERSION_1_4);
                mRil.getVoiceRegistrationState(result);
            });

            mRil.processResponseFallback(rr, responseInfo, voiceRegResponse);
            return;
        } else if (responseInfo.error == RadioError.NONE) {
            sendMessageResponse(rr.mResult, voiceRegResponse);
        }
        mRil.processResponseDone(rr, responseInfo, voiceRegResponse);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param voiceRegResponse Current Voice registration response as defined by VoiceRegStateResult
     *        in 1.6/types.hal
     */
    public void getVoiceRegistrationStateResponse_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            android.hardware.radio.V1_6.RegStateResult voiceRegResponse) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, voiceRegResponse);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, voiceRegResponse);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataRegResponse Current Data registration response as defined by DataRegStateResult in
     *        types.hal
     */
    public void getDataRegistrationStateResponse(RadioResponseInfo responseInfo,
            DataRegStateResult dataRegResponse) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, dataRegResponse);
            }
            mRil.processResponseDone(rr, responseInfo, dataRegResponse);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataRegResponse Current Data registration response as defined by DataRegStateResult in
     *        1.2/types.hal
     */
    public void getDataRegistrationStateResponse_1_2(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_2.DataRegStateResult dataRegResponse) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, dataRegResponse);
            }
            mRil.processResponseDone(rr, responseInfo, dataRegResponse);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataRegResponse Current Data registration response as defined by DataRegStateResult in
     *        1.4/types.hal
     */
    public void getDataRegistrationStateResponse_1_4(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_4.DataRegStateResult dataRegResponse) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, dataRegResponse);
            }
            mRil.processResponseDone(rr, responseInfo, dataRegResponse);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataRegResponse Current Data registration response as defined by DataRegStateResult in
     *        1.5/types.hal
     */
    public void getDataRegistrationStateResponse_1_5(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_5.RegStateResult dataRegResponse) {
        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr == null) {
            return;
        }

        if (responseInfo.error == RadioError.REQUEST_NOT_SUPPORTED) {
            // Move the data needed for fallback call from rr which will be released soon
            final int request = rr.getRequest();
            final Message result = rr.getResult();

            mRil.mRilHandler.post(() -> {
                mRil.setCompatVersion(request, RIL.RADIO_HAL_VERSION_1_4);
                mRil.getDataRegistrationState(result);
            });

            mRil.processResponseFallback(rr, responseInfo, dataRegResponse);
            return;
        } else if (responseInfo.error == RadioError.NONE) {
            sendMessageResponse(rr.mResult, dataRegResponse);
        }
        mRil.processResponseDone(rr, responseInfo, dataRegResponse);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataRegResponse Current Data registration response as defined by DataRegStateResult in
     *        1.6/types.hal
     */
    public void getDataRegistrationStateResponse_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            android.hardware.radio.V1_6.RegStateResult dataRegResponse) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, dataRegResponse);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, dataRegResponse);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param longName is long alpha ONS or EONS or empty string if unregistered
     * @param shortName is short alpha ONS or EONS or empty string if unregistered
     * @param numeric is 5 or 6 digit numeric code (MCC + MNC) or empty string if unregistered
     */
    public void getOperatorResponse(RadioResponseInfo responseInfo,
            String longName, String shortName, String numeric) {
        responseStrings(responseInfo, longName, shortName, numeric);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setRadioPowerResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
        mRil.mLastRadioPowerResult = responseInfo.error;
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error which
     *                     is defined in 1.6/types.hal
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendSmsResponse_1_6(android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            SendSmsResult sms) {
        responseSms_1_6(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendSMSExpectMoreResponse(RadioResponseInfo responseInfo,
            SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error which
     *                     is defined in 1.6/types.hal
     * @param sms Response to sms sent as defined by SendSmsResult in 1.6/types.hal
     */
    public void sendSmsExpectMoreResponse_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms_1_6(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param setupDataCallResult Response to data call setup as defined by setupDataCallResult in
     *                            types.hal
     */
    public void setupDataCallResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.SetupDataCallResult setupDataCallResult) {
        responseSetupDataCall(responseInfo, setupDataCallResult);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param setupDataCallResult Response to data call setup as defined by setupDataCallResult in
     *                            1.4/types.hal
     */
    public void setupDataCallResponse_1_4(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_4.SetupDataCallResult setupDataCallResult) {
        responseSetupDataCall(responseInfo, setupDataCallResult);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param setupDataCallResult Response to data call setup as defined by setupDataCallResult in
     *                            1.5/types.hal
     */
    public void setupDataCallResponse_1_5(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_5.SetupDataCallResult setupDataCallResult) {
        responseSetupDataCall(responseInfo, setupDataCallResult);
    }


    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param setupDataCallResult Response to data call setup as defined by setupDataCallResult in
     *                            1.6/types.hal
     */
    public void setupDataCallResponse_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            android.hardware.radio.V1_6.SetupDataCallResult setupDataCallResult) {
        responseSetupDataCall_1_6(responseInfo, setupDataCallResult);
    }

    @Override
    public void getDataCallListResponse_1_6(android.hardware.radio.V1_6.RadioResponseInfo info,
            ArrayList<SetupDataCallResult> dcResponse) {
        responseDataCallList(info, dcResponse);
    }

    @Override
    public void setSimCardPowerResponse_1_6(android.hardware.radio.V1_6.RadioResponseInfo info) {
        responseVoid_1_6(info);
    }

    @Override
    public void setAllowedNetworkTypesBitmapResponse(
            android.hardware.radio.V1_6.RadioResponseInfo info) {
        responseVoid_1_6(info);
    }

    @Override
    public void getAllowedNetworkTypesBitmapResponse(
            android.hardware.radio.V1_6.RadioResponseInfo info, int halRadioAccessFamilyBitmap) {
        int networkTypeBitmask = RILUtils.convertHalNetworkTypeBitMask(halRadioAccessFamilyBitmap);
        mRil.mAllowedNetworkTypesBitmask = networkTypeBitmask;
        responseInts_1_6(info, networkTypeBitmask);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param iccIo ICC io operation response as defined by IccIoResult in types.hal
     */
    public void iccIOForAppResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.IccIoResult iccIo) {
        responseIccIo(responseInfo, iccIo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendUssdResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void cancelPendingUssdResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param n is "n" parameter from TS 27.007 7.7
     * @param m is "m" parameter from TS 27.007 7.7
     */
    public void getClirResponse(RadioResponseInfo responseInfo, int n, int m) {
        responseInts(responseInfo, n, m);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setClirResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param callForwardInfos points to a vector of CallForwardInfo, one for
     *        each distinct registered phone number.
     */
    public void getCallForwardStatusResponse(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.CallForwardInfo> callForwardInfos) {
        responseCallForwardInfo(responseInfo, callForwardInfos);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCallForwardResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable If current call waiting state is disabled, enable = false else true
     * @param serviceClass If enable, then callWaitingResp[1]
     *        must follow, with the TS 27.007 service class bit vector of services
     *        for which call waiting is enabled.
     *        For example, if callWaitingResp[0] is 1 and
     *        callWaitingResp[1] is 3, then call waiting is enabled for data
     *        and voice and disabled for everything else.
     */
    public void getCallWaitingResponse(RadioResponseInfo responseInfo,
            boolean enable, int serviceClass) {
        responseInts(responseInfo, enable ? 1 : 0, serviceClass);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCallWaitingResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acceptCallResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deactivateDataCallResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param response 0 is the TS 27.007 service class bit vector of
     *        services for which the specified barring facility
     *        is active. "0" means "disabled for all"
     */
    public void getFacilityLockForAppResponse(RadioResponseInfo responseInfo, int response) {
        responseInts(responseInfo, response);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param retry 0 is the number of retries remaining, or -1 if unknown
     */
    public void setFacilityLockForAppResponse(RadioResponseInfo responseInfo, int retry) {
        responseInts(responseInfo, retry);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setBarringPasswordResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param selection false for automatic selection, true for manual selection
     */
    public void getNetworkSelectionModeResponse(RadioResponseInfo responseInfo, boolean selection) {
        responseInts(responseInfo, selection ? 1 : 0);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNetworkSelectionModeManualResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNetworkSelectionModeManualResponse_1_5(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param networkInfos List of network operator information as OperatorInfos defined in
     *                     types.hal
     */
    public void getAvailableNetworksResponse(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.OperatorInfo> networkInfos) {
        responseOperatorInfos(responseInfo, networkInfos);
    }

    /**
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void startNetworkScanResponse(RadioResponseInfo responseInfo) {
        responseScanStatus(responseInfo, null /*fallbackHalVersion*/);
    }

    /**
     * The same method as startNetworkScanResponse, except disallowing error codes
     * OPERATION_NOT_ALLOWED.
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void startNetworkScanResponse_1_4(RadioResponseInfo responseInfo) {
        responseScanStatus(responseInfo, null /*fallbackHalVersion*/);
    }

    /**
     * The same method as startNetworkScanResponse_1_4.
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void startNetworkScanResponse_1_5(RadioResponseInfo responseInfo) {
        responseScanStatus(responseInfo, RIL.RADIO_HAL_VERSION_1_4);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void stopNetworkScanResponse(RadioResponseInfo responseInfo) {
        responseScanStatus(responseInfo, null /*fallbackHalVersion*/);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void startDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void stopDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param version string containing version string for log reporting
     */
    public void getBasebandVersionResponse(RadioResponseInfo responseInfo, String version) {
        responseString(responseInfo, version);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void separateConnectionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setMuteResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable true for "mute enabled" and false for "mute disabled"
     */
    public void getMuteResponse(RadioResponseInfo responseInfo, boolean enable) {
        responseInts(responseInfo, enable ? 1 : 0);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param status indicates CLIP status
     */
    public void getClipResponse(RadioResponseInfo responseInfo, int status) {
        responseInts(responseInfo, status);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataCallResultList Response to get data call list as defined by setupDataCallResult in
     *                           types.hal
     */
    public void getDataCallListResponse(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.SetupDataCallResult> dataCallResultList) {
        responseDataCallList(responseInfo, dataCallResultList);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataCallResultList Response to get data call list as defined by setupDataCallResult in
     *                           1.4/types.hal
     */
    public void getDataCallListResponse_1_4(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_4.SetupDataCallResult> dataCallResultList) {
        responseDataCallList(responseInfo, dataCallResultList);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataCallResultList Response to get data call list as defined by setupDataCallResult in
     *                           1.5/types.hal
     */
    public void getDataCallListResponse_1_5(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_5.SetupDataCallResult> dataCallResultList) {
        responseDataCallList(responseInfo, dataCallResultList);
    }

    public void sendOemRilRequestRawResponse(RadioResponseInfo responseInfo,
            ArrayList<Byte> var2) {}

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSuppServiceNotificationsResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param index record index where the message is stored
     */
    public void writeSmsToSimResponse(RadioResponseInfo responseInfo, int index) {
        responseInts(responseInfo, index);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deleteSmsOnSimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setBandModeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param bandModes List of RadioBandMode listing supported modes
     */
    public void getAvailableBandModesResponse(RadioResponseInfo responseInfo,
            ArrayList<Integer> bandModes) {
        responseIntArrayList(responseInfo, bandModes);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param commandResponse SAT/USAT response in hexadecimal format
     *        string starting with first byte of response
     */
    public void sendEnvelopeResponse(RadioResponseInfo responseInfo, String commandResponse) {
        responseString(responseInfo, commandResponse);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendTerminalResponseToSimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void explicitCallTransferResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPreferredNetworkTypeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * Callback of setPreferredNetworkTypeBitmap defined in IRadio.hal.
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPreferredNetworkTypeBitmapResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param nwType RadioPreferredNetworkType defined in types.hal
     */
    public void getPreferredNetworkTypeResponse(RadioResponseInfo responseInfo, int nwType) {
        mRil.mAllowedNetworkTypesBitmask = RadioAccessFamily.getRafFromNetworkType(nwType);
        responseInts(responseInfo, RadioAccessFamily.getRafFromNetworkType(nwType));
    }

    /**
     * Callback of the getPreferredNetworkTypeBitmap defined in the IRadio.hal.
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param halRadioAccessFamilyBitmap a 32-bit bitmap of
     * {@link android.hardware.radio.V1_0.RadioAccessFamily}.
     */
    public void getPreferredNetworkTypeBitmapResponse(
            RadioResponseInfo responseInfo, int halRadioAccessFamilyBitmap) {

        int networkTypeBitmask = RILUtils.convertHalNetworkTypeBitMask(halRadioAccessFamilyBitmap);
        mRil.mAllowedNetworkTypesBitmask = networkTypeBitmask;
        responseInts(responseInfo, networkTypeBitmask);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cells Vector of neighboring radio cell information
     */
    public void getNeighboringCidsResponse(RadioResponseInfo responseInfo,
            ArrayList<NeighboringCell> cells) {
        responseCellList(responseInfo, cells);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setLocationUpdatesResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param type CdmaRoamingType defined in types.hal
     */
    public void getCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo, int type) {
        responseInts(responseInfo, type);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setTTYModeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param mode TTY mode
     */
    public void getTTYModeResponse(RadioResponseInfo responseInfo, int mode) {
        responseInts(responseInfo, mode);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable false for Standard Privacy Mode (Public Long Code Mask)
     *        true for Enhanced Privacy Mode (Private Long Code Mask)
     */
    public void getPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo, boolean enable) {
        responseInts(responseInfo, enable ? 1 : 0);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendCDMAFeatureCodeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendBurstDtmfResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Sms result struct as defined by SendSmsResult in types.hal
     */
    public void sendCdmaSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error which
     *                     is defined in 1.6/types.hal
     * @param sms Sms result struct as defined by SendSmsResult in types.hal
     */
    public void sendCdmaSmsResponse_1_6(android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            SendSmsResult sms) {
        responseSms_1_6(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendCdmaSmsExpectMoreResponse(RadioResponseInfo responseInfo,
            SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error which
     *                     is defined in 1.6/types.hal
     * @param sms Sms result struct as defined by SendSmsResult in types.hal
     */
    public void sendCdmaSmsExpectMoreResponse_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms_1_6(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setDataThrottlingResponse(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo) {
        responseVoid_1_6(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param configs Vector of GSM/WCDMA Cell broadcast configs
     */
    public void getGsmBroadcastConfigResponse(RadioResponseInfo responseInfo,
            ArrayList<GsmBroadcastSmsConfigInfo> configs) {
        responseGmsBroadcastConfig(responseInfo, configs);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setGsmBroadcastConfigResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setGsmBroadcastActivationResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param configs Vector of CDMA Broadcast SMS configs.
     */
    public void getCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo,
            ArrayList<CdmaBroadcastSmsConfigInfo> configs) {
        responseCdmaBroadcastConfig(responseInfo, configs);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaBroadcastActivationResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
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
    public void getCDMASubscriptionResponse(RadioResponseInfo responseInfo, String mdn,
            String hSid, String hNid, String min, String prl) {
        responseStrings(responseInfo, mdn, hSid, hNid, min, prl);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param index record index where the cmda sms message is stored
     */
    public void writeSmsToRuimResponse(RadioResponseInfo responseInfo, int index) {
        responseInts(responseInfo, index);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deleteSmsOnRuimResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
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
        responseStrings(responseInfo, imei, imeisv, esn, meid);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void exitEmergencyCallbackModeResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param smsc Short Message Service Center address on the device
     */
    public void getSmscAddressResponse(RadioResponseInfo responseInfo, String smsc) {
        responseString(responseInfo, smsc);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSmscAddressResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void reportSmsMemoryStatusResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void reportStkServiceIsRunningResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param source CDMA subscription source
     */
    public void getCdmaSubscriptionSourceResponse(RadioResponseInfo responseInfo, int source) {
        responseInts(responseInfo, source);
    }

    /**
     * This method is deprecated and should not be used.
     *
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param response response string of the challenge/response algo for ISIM auth in base64 format
     */
    public void requestIsimAuthenticationResponse(RadioResponseInfo responseInfo, String response) {
        throw new RuntimeException("Inexplicable response received for requestIsimAuthentication");
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param iccIo IccIoResult as defined in types.hal corresponding to ICC IO response
     */
    public void sendEnvelopeWithStatusResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.IccIoResult iccIo) {
        responseIccIo(responseInfo, iccIo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param rat Current voice RAT
     */
    public void getVoiceRadioTechnologyResponse(RadioResponseInfo responseInfo, int rat) {
        responseInts(responseInfo, rat);
    }

    public void getCellInfoListResponse(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.CellInfo> cellInfo) {
        responseCellInfoList(responseInfo, cellInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cellInfo List of current cell information known to radio
     */
    public void getCellInfoListResponse_1_2(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_2.CellInfo> cellInfo) {
        responseCellInfoList(responseInfo, cellInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param cellInfo List of current cell information known to radio.
     */
    public void getCellInfoListResponse_1_4(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_4.CellInfo> cellInfo) {
        responseCellInfoList(responseInfo, cellInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param cellInfo List of current cell information known to radio.
     */
    public void getCellInfoListResponse_1_5(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_5.CellInfo> cellInfo) {
        responseCellInfoList(responseInfo, cellInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param cellInfo List of current cell information known to radio.
     */
    public void getCellInfoListResponse_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_6.CellInfo> cellInfo) {
        responseCellInfoList_1_6(responseInfo, cellInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCellInfoListRateResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setInitialAttachApnResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setInitialAttachApnResponse_1_5(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param isRegistered false = not registered, true = registered
     * @param ratFamily RadioTechnologyFamily as defined in types.hal. This value is valid only if
     *        isRegistered is true.
     */
    public void getImsRegistrationStateResponse(RadioResponseInfo responseInfo,
            boolean isRegistered, int ratFamily) {
        responseInts(responseInfo, isRegistered ? 1 : 0,
                ratFamily == RadioTechnologyFamily.THREE_GPP ? PhoneConstants.PHONE_TYPE_GSM
                        : PhoneConstants.PHONE_TYPE_CDMA);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult in types.hal
     */
    public void sendImsSmsResponse(RadioResponseInfo responseInfo, SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result IccIoResult as defined in types.hal
     */
    public void iccTransmitApduBasicChannelResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.IccIoResult result) {
        responseIccIo(responseInfo, result);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param channelId session id of the logical channel.
     * @param selectResponse Contains the select response for the open channel command with one
     *        byte per integer
     */
    public void iccOpenLogicalChannelResponse(RadioResponseInfo responseInfo, int channelId,
            ArrayList<Byte> selectResponse) {
        ArrayList<Integer> arr = new ArrayList<>();
        arr.add(channelId);
        for (int i = 0; i < selectResponse.size(); i++) {
            arr.add((int) selectResponse.get(i));
        }
        responseIntArrayList(responseInfo, arr);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void iccCloseLogicalChannelResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result IccIoResult as defined in types.hal
     */
    public void iccTransmitApduLogicalChannelResponse(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.IccIoResult result) {
        responseIccIo(responseInfo, result);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result string containing the contents of the NV item
     */
    public void nvReadItemResponse(RadioResponseInfo responseInfo, String result) {
        responseString(responseInfo, result);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvWriteItemResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvWriteCdmaPrlResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void nvResetConfigResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setUiccSubscriptionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setDataAllowedResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getHardwareConfigResponse(
            RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.HardwareConfig> config) {
        responseHardwareConfig(responseInfo, config);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param result IccIoResult as defined in types.hal
     */
    public void requestIccSimAuthenticationResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.IccIoResult result) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            IccIoResult ret = new IccIoResult(result.sw1, result.sw2,
                    TextUtils.isEmpty(result.simResponse) ? null : result.simResponse.getBytes());
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setDataProfileResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setDataProfileResponse_1_5(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void requestShutdownResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    public void getRadioCapabilityResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.RadioCapability rc) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            RadioCapability ret = RILUtils.convertHalRadioCapability(rc, mRil);
            if (responseInfo.error == RadioError.REQUEST_NOT_SUPPORTED
                    || responseInfo.error == RadioError.GENERIC_FAILURE) {
                // we should construct the RAF bitmask the radio
                // supports based on preferred network bitmasks
                ret = mRil.makeStaticRadioCapability();
                responseInfo.error = RadioError.NONE;
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    public void setRadioCapabilityResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.RadioCapability rc) {
        responseRadioCapability(responseInfo, rc);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param statusInfo LceStatusInfo indicating LCE status
     */
    public void startLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
        responseLceStatus(responseInfo, statusInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param statusInfo LceStatusInfo indicating LCE status
     */
    public void stopLceServiceResponse(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
        responseLceStatus(responseInfo, statusInfo);
    }

    public void pullLceDataResponse(RadioResponseInfo responseInfo, LceDataInfo lceInfo) {
        responseLceData(responseInfo, lceInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param activityInfo modem activity information
     */
    public void getModemActivityInfoResponse(RadioResponseInfo responseInfo,
            ActivityStatsInfo activityInfo) {
        responseActivityData(responseInfo, activityInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param isEnabled Indicates whether NR dual connectivity is enabled or not, True if enabled
     *               else false.
     */
    public void isNrDualConnectivityEnabledResponse(
            android.hardware.radio.V1_6.RadioResponseInfo  responseInfo,
            boolean isEnabled) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, isEnabled);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, isEnabled);
        }
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     */
    public void setNrDualConnectivityStateResponse(
            android.hardware.radio.V1_6.RadioResponseInfo info) {
        responseVoid_1_6(info);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param numAllowed number of allowed carriers which have been set correctly.
     *        On success, it must match the length of list Carriers->allowedCarriers.
     *        if Length of allowed carriers list is 0, numAllowed = 0.
     */
    public void setAllowedCarriersResponse(RadioResponseInfo responseInfo, int numAllowed) {
        // The number of allowed carriers set correctly is not really useful. Even if one is
        // missing, the operation has failed, as the list should be treated as a single
        // configuration item. So, ignoring the value of numAllowed and considering only the
        // value of the responseInfo.error.
        int ret = TelephonyManager.SET_CARRIER_RESTRICTION_ERROR;
        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr != null) {
            mRil.riljLog("setAllowedCarriersResponse - error = " + responseInfo.error);

            if (responseInfo.error == RadioError.NONE) {
                ret = TelephonyManager.SET_CARRIER_RESTRICTION_SUCCESS;
                sendMessageResponse(rr.mResult, ret);
            } else if (responseInfo.error == RadioError.REQUEST_NOT_SUPPORTED) {
                // Handle the case REQUEST_NOT_SUPPORTED as a valid response
                responseInfo.error = RadioError.NONE;
                ret = TelephonyManager.SET_CARRIER_RESTRICTION_NOT_SUPPORTED;
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setAllowedCarriersResponse_1_4(RadioResponseInfo responseInfo) {
        int ret = TelephonyManager.SET_CARRIER_RESTRICTION_ERROR;
        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr != null) {
            mRil.riljLog("setAllowedCarriersResponse_1_4 - error = " + responseInfo.error);

            if (responseInfo.error == RadioError.NONE) {
                ret = TelephonyManager.SET_CARRIER_RESTRICTION_SUCCESS;
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param allAllowed true only when all carriers are allowed. Ignore "carriers" struct.
     *                   If false, consider "carriers" struct
     * @param carriers Carrier restriction information.
     */
    public void getAllowedCarriersResponse(RadioResponseInfo responseInfo, boolean allAllowed,
            CarrierRestrictions carriers) {
        CarrierRestrictionsWithPriority carrierRestrictions = new CarrierRestrictionsWithPriority();
        carrierRestrictions.allowedCarriers = carriers.allowedCarriers;
        carrierRestrictions.excludedCarriers = carriers.excludedCarriers;
        carrierRestrictions.allowedCarriersPrioritized = true;

        responseCarrierRestrictions(responseInfo, allAllowed, carrierRestrictions,
                SimLockMultiSimPolicy.NO_MULTISIM_POLICY);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param carrierRestrictions Carrier restriction information.
     * @param multiSimPolicy Policy for multi-sim devices.
     */
    public void getAllowedCarriersResponse_1_4(RadioResponseInfo responseInfo,
            CarrierRestrictionsWithPriority carrierRestrictions,
            int multiSimPolicy) {
        // The API in IRadio 1.4 does not support the flag allAllowed, so setting it to false, so
        // that values in carrierRestrictions are used.
        responseCarrierRestrictions(responseInfo, false, carrierRestrictions, multiSimPolicy);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendDeviceStateResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCarrierInfoForImsiEncryptionResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setIndicationFilterResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setIndicationFilterResponse_1_5(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSimCardPowerResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSignalStrengthReportingCriteriaResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSignalStrengthReportingCriteriaResponse_1_5(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setLinkCapacityReportingCriteriaResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setLinkCapacityReportingCriteriaResponse_1_5(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSimCardPowerResponse_1_1(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param keepaliveStatus status of the keepalive with a handle for the session
     */
    public void startKeepaliveResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_1.KeepaliveStatus keepaliveStatus) {

        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr == null) return;

        KeepaliveStatus ret = null;
        try {
            switch(responseInfo.error) {
                case RadioError.NONE:
                    int convertedStatus = RILUtils.convertHalKeepaliveStatusCode(
                            keepaliveStatus.code);
                    if (convertedStatus < 0) {
                        ret = new KeepaliveStatus(KeepaliveStatus.ERROR_UNSUPPORTED);
                    } else {
                        ret = new KeepaliveStatus(
                                keepaliveStatus.sessionHandle, convertedStatus);
                    }
                    // If responseInfo.error is NONE, response function sends the response message
                    // even if result is actually an error.
                    sendMessageResponse(rr.mResult, ret);
                    break;
                case RadioError.REQUEST_NOT_SUPPORTED:
                    ret = new KeepaliveStatus(KeepaliveStatus.ERROR_UNSUPPORTED);
                    break;
                case RadioError.NO_RESOURCES:
                    ret = new KeepaliveStatus(KeepaliveStatus.ERROR_NO_RESOURCES);
                    break;
                default:
                    ret = new KeepaliveStatus(KeepaliveStatus.ERROR_UNKNOWN);
                    break;
            }
        } finally {
            // If responseInfo.error != NONE, the processResponseDone sends the response message.
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void stopKeepaliveResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr == null) return;

        try {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, null);
            } else {
                //TODO: Error code translation
            }
        } finally {
            mRil.processResponseDone(rr, responseInfo, null);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     */
    public void getSimPhonebookRecordsResponse(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo) {
        responseVoid_1_6(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param pbCapacity Contains the adn, email, anr capacities in the sim card.
     */
    public void getSimPhonebookCapacityResponse(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            android.hardware.radio.V1_6.PhonebookCapacity pbCapacity) {
        AdnCapacity capacity = RILUtils.convertHalPhonebookCapacity(pbCapacity);
        responseAdnCapacity(responseInfo, capacity);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param updatedRecordIndex The index of the updated record.
     */
    public void updateSimPhonebookRecordsResponse(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            int updatedRecordIndex) {
        responseInts_1_6(responseInfo, updatedRecordIndex);
    }

    private void responseAdnCapacity(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            AdnCapacity capacity) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, capacity);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, capacity);
        }
    }

    private void responseIccCardStatus(RadioResponseInfo responseInfo, CardStatus cardStatus) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            IccCardStatus iccCardStatus = RILUtils.convertHalCardStatus(cardStatus);
            mRil.riljLog("responseIccCardStatus: from HIDL: " + iccCardStatus);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, iccCardStatus);
            }
            mRil.processResponseDone(rr, responseInfo, iccCardStatus);
        }
    }

    private void responseIccCardStatus_1_2(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_2.CardStatus cardStatus) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            IccCardStatus iccCardStatus = RILUtils.convertHalCardStatus(cardStatus.base);
            IccSlotPortMapping slotPortMapping = new IccSlotPortMapping();
            slotPortMapping.mPhysicalSlotIndex = cardStatus.physicalSlotId;
            iccCardStatus.mSlotPortMapping = slotPortMapping;
            iccCardStatus.atr = cardStatus.atr;
            iccCardStatus.iccid = cardStatus.iccid;
            mRil.riljLog("responseIccCardStatus: from HIDL: " + iccCardStatus);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, iccCardStatus);
            }
            mRil.processResponseDone(rr, responseInfo, iccCardStatus);
        }
    }

    private void responseIccCardStatus_1_4(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_4.CardStatus cardStatus) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            IccCardStatus iccCardStatus = RILUtils.convertHalCardStatus(cardStatus.base.base);
            IccSlotPortMapping slotPortMapping = new IccSlotPortMapping();
            slotPortMapping.mPhysicalSlotIndex = cardStatus.base.physicalSlotId;
            iccCardStatus.mSlotPortMapping = slotPortMapping;
            iccCardStatus.atr = cardStatus.base.atr;
            iccCardStatus.iccid = cardStatus.base.iccid;
            iccCardStatus.eid = cardStatus.eid;
            mRil.riljLog("responseIccCardStatus: from HIDL: " + iccCardStatus);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, iccCardStatus);
            }
            mRil.processResponseDone(rr, responseInfo, iccCardStatus);
        }
    }

    private void responseIccCardStatus_1_5(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_5.CardStatus cardStatus) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            IccCardStatus iccCardStatus = RILUtils.convertHalCardStatus(cardStatus);
            mRil.riljLog("responseIccCardStatus: from HIDL: " + iccCardStatus);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, iccCardStatus);
            }
            mRil.processResponseDone(rr, responseInfo, iccCardStatus);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void emergencyDialResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    private void responseInts(RadioResponseInfo responseInfo, int ...var) {
        final ArrayList<Integer> ints = new ArrayList<>();
        for (int i = 0; i < var.length; i++) {
            ints.add(var[i]);
        }
        responseIntArrayList(responseInfo, ints);
    }

    private void responseInts_1_6(android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            int ...var) {
        final ArrayList<Integer> ints = new ArrayList<>();
        for (int i = 0; i < var.length; i++) {
            ints.add(var[i]);
        }
        responseIntArrayList_1_6(responseInfo, ints);
    }

    /**
     * Send int array response
     * @param service radio service that received the response
     * @param ril RIL to send response
     * @param responseInfo responseInfo
     * @param var response int array
     */
    public static void responseInts(int service, RIL ril,
            android.hardware.radio.RadioResponseInfo responseInfo, int ...var) {
        final ArrayList<Integer> ints = new ArrayList<>();
        for (int i = 0; i < var.length; i++) {
            ints.add(var[i]);
        }
        responseIntArrayList(service, ril, responseInfo, ints);
    }

    private void responseIntArrayList(RadioResponseInfo responseInfo, ArrayList<Integer> var) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            int[] ret = new int[var.size()];
            for (int i = 0; i < var.size(); i++) {
                ret[i] = var.get(i);
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseIntArrayList_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo, ArrayList<Integer> var) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);

        if (rr != null) {
            int[] ret = new int[var.size()];
            for (int i = 0; i < var.size(); i++) {
                ret[i] = var.get(i);
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, ret);
        }
    }

    /**
     * Send int array list response
     * @param service radio service that received the response
     * @param ril RIL to send response
     * @param responseInfo responseInfo
     * @param var response int array list
     */
    public static void responseIntArrayList(int service, RIL ril,
            android.hardware.radio.RadioResponseInfo responseInfo, ArrayList<Integer> var) {
        RILRequest rr = ril.processResponse(service, responseInfo);

        if (rr != null) {
            int[] ret = new int[var.size()];
            for (int i = 0; i < var.size(); i++) {
                ret[i] = var.get(i);
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            ril.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCurrentCalls(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.Call> calls) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            int num = calls.size();
            ArrayList<DriverCall> dcCalls = new ArrayList<DriverCall>(num);
            DriverCall dc;

            for (int i = 0; i < num; i++) {
                dc = RILUtils.convertToDriverCall(calls.get(i));

                dcCalls.add(dc);

                if (dc.isVoicePrivacy) {
                    mRil.mVoicePrivacyOnRegistrants.notifyRegistrants();
                    mRil.riljLog("InCall VoicePrivacy is enabled");
                } else {
                    mRil.mVoicePrivacyOffRegistrants.notifyRegistrants();
                    mRil.riljLog("InCall VoicePrivacy is disabled");
                }
            }

            Collections.sort(dcCalls);

            if ((num == 0) && mRil.mTestingEmergencyCall.getAndSet(false)) {
                if (mRil.mEmergencyCallbackModeRegistrant != null) {
                    mRil.riljLog("responseCurrentCalls: call ended, testing emergency call,"
                            + " notify ECM Registrants");
                    mRil.mEmergencyCallbackModeRegistrant.notifyRegistrant();
                }
            }

            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, dcCalls);
            }
            mRil.processResponseDone(rr, responseInfo, dcCalls);
        }
    }

    private void responseCurrentCalls_1_2(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_2.Call> calls) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            int num = calls.size();
            ArrayList<DriverCall> dcCalls = new ArrayList<DriverCall>(num);
            DriverCall dc;

            for (int i = 0; i < num; i++) {
                dc = RILUtils.convertToDriverCall(calls.get(i));

                dcCalls.add(dc);

                if (dc.isVoicePrivacy) {
                    mRil.mVoicePrivacyOnRegistrants.notifyRegistrants();
                    mRil.riljLog("InCall VoicePrivacy is enabled");
                } else {
                    mRil.mVoicePrivacyOffRegistrants.notifyRegistrants();
                    mRil.riljLog("InCall VoicePrivacy is disabled");
                }
            }

            Collections.sort(dcCalls);

            if ((num == 0) && mRil.mTestingEmergencyCall.getAndSet(false)) {
                if (mRil.mEmergencyCallbackModeRegistrant != null) {
                    mRil.riljLog("responseCurrentCalls: call ended, testing emergency call,"
                            + " notify ECM Registrants");
                    mRil.mEmergencyCallbackModeRegistrant.notifyRegistrant();
                }
            }

            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, dcCalls);
            }
            mRil.processResponseDone(rr, responseInfo, dcCalls);
        }
    }

    private void responseCurrentCalls_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_6.Call> calls) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);

        if (rr != null) {
            int num = calls.size();
            ArrayList<DriverCall> dcCalls = new ArrayList<DriverCall>(num);
            DriverCall dc;

            for (int i = 0; i < num; i++) {
                dc = RILUtils.convertToDriverCall(calls.get(i));

                dcCalls.add(dc);

                if (dc.isVoicePrivacy) {
                    mRil.mVoicePrivacyOnRegistrants.notifyRegistrants();
                    mRil.riljLog("InCall VoicePrivacy is enabled");
                } else {
                    mRil.mVoicePrivacyOffRegistrants.notifyRegistrants();
                    mRil.riljLog("InCall VoicePrivacy is disabled");
                }
            }

            Collections.sort(dcCalls);

            if ((num == 0) && mRil.mTestingEmergencyCall.getAndSet(false)) {
                if (mRil.mEmergencyCallbackModeRegistrant != null) {
                    mRil.riljLog("responseCurrentCalls: call ended, testing emergency call,"
                            + " notify ECM Registrants");
                    mRil.mEmergencyCallbackModeRegistrant.notifyRegistrant();
                }
            }

            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, dcCalls);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, dcCalls);
        }
    }

    private void responseVoid(RadioResponseInfo responseInfo) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            Object ret = null;
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseVoid_1_6(android.hardware.radio.V1_6.RadioResponseInfo responseInfo) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);

        if (rr != null) {
            Object ret = null;
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, ret);
        }
    }

    /**
     * Send void response
     * @param service radio service that received the response
     * @param ril RIL to send response
     * @param responseInfo response void
     */
    public static void responseVoid(int service, RIL ril,
            android.hardware.radio.RadioResponseInfo responseInfo) {
        RILRequest rr = ril.processResponse(service, responseInfo);

        if (rr != null) {
            Object ret = null;
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            ril.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseString(RadioResponseInfo responseInfo, String str) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, str);
            }
            mRil.processResponseDone(rr, responseInfo, str);
        }
    }

    /**
     * Send string response
     * @param service radio service that received the response
     * @param ril RIL to send response
     * @param responseInfo responseInfo
     * @param str response string
     */
    public static void responseString(int service, RIL ril,
            android.hardware.radio.RadioResponseInfo responseInfo, String str) {
        RILRequest rr = ril.processResponse(service, responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, str);
            }
            ril.processResponseDone(rr, responseInfo, str);
        }
    }

    private void responseStrings(RadioResponseInfo responseInfo, String ...str) {
        ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < str.length; i++) {
            strings.add(str[i]);
        }
        responseStringArrayList(mRil, responseInfo, strings);
    }

    /**
     * Send String array response
     * @param service radio service that received the response
     * @param ril RIL to send response
     * @param responseInfo responseInfo
     * @param str String array
     */
    public static void responseStrings(int service, RIL ril,
            android.hardware.radio.RadioResponseInfo responseInfo, String ...str) {
        ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < str.length; i++) {
            strings.add(str[i]);
        }
        responseStringArrayList(service, ril, responseInfo, strings);
    }

    static void responseStringArrayList(RIL ril, RadioResponseInfo responseInfo,
            ArrayList<String> strings) {
        RILRequest rr = ril.processResponse(responseInfo);

        if (rr != null) {
            String[] ret = new String[strings.size()];
            for (int i = 0; i < strings.size(); i++) {
                ret[i] = strings.get(i);
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            ril.processResponseDone(rr, responseInfo, ret);
        }
    }

    private static void responseStringArrayList(int service, RIL ril,
            android.hardware.radio.RadioResponseInfo responseInfo, ArrayList<String> strings) {
        RILRequest rr = ril.processResponse(service, responseInfo);

        if (rr != null) {
            String[] ret = new String[strings.size()];
            for (int i = 0; i < strings.size(); i++) {
                ret[i] = strings.get(i);
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            ril.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseLastCallFailCauseInfo(RadioResponseInfo responseInfo,
            LastCallFailCauseInfo fcInfo) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            LastCallFailCause ret = new LastCallFailCause();
            ret.causeCode = fcInfo.causeCode;
            ret.vendorCause = fcInfo.vendorCause;
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSignalStrength(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.SignalStrength signalStrength) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            SignalStrength ret = RILUtils.convertHalSignalStrength(signalStrength);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSignalStrength_1_2(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_2.SignalStrength signalStrength) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            SignalStrength ret = RILUtils.convertHalSignalStrength(signalStrength);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSignalStrength_1_4(
            RadioResponseInfo responseInfo,
            android.hardware.radio.V1_4.SignalStrength signalStrength) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            SignalStrength ret = RILUtils.convertHalSignalStrength(signalStrength);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSignalStrength_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            android.hardware.radio.V1_6.SignalStrength signalStrength) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);

        if (rr != null) {
            SignalStrength ret = RILUtils.convertHalSignalStrength(signalStrength);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, ret);
        }
    }

    private void responseSms(RadioResponseInfo responseInfo, SendSmsResult sms) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            long messageId = RIL.getOutgoingSmsMessageId(rr.mResult);
            SmsResponse ret = new SmsResponse(sms.messageRef, sms.ackPDU, sms.errorCode, messageId);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseSms_1_6(android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            SendSmsResult sms) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);

        if (rr != null) {
            long messageId = RIL.getOutgoingSmsMessageId(rr.mResult);
            SmsResponse ret = new SmsResponse(sms.messageRef, sms.ackPDU, sms.errorCode, messageId);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, ret);
        }
    }

    private void responseSetupDataCall(RadioResponseInfo responseInfo,
            Object setupDataCallResult) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            DataCallResponse response = RILUtils.convertHalDataCallResult(setupDataCallResult);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, response);
            }
            mRil.processResponseDone(rr, responseInfo, response);
        }
    }

    private void responseSetupDataCall_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            Object setupDataCallResult) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);

        if (rr != null) {
            DataCallResponse response = RILUtils.convertHalDataCallResult(setupDataCallResult);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, response);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, response);
        }
    }

    private void responseIccIo(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.IccIoResult result) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            IccIoResult ret = new IccIoResult(result.sw1, result.sw2, result.simResponse);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCallForwardInfo(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.CallForwardInfo> callForwardInfos) {
        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr != null) {
            CallForwardInfo[] ret = new CallForwardInfo[callForwardInfos.size()];
            for (int i = 0; i < callForwardInfos.size(); i++) {
                ret[i] = new CallForwardInfo();
                ret[i].status = callForwardInfos.get(i).status;
                ret[i].reason = callForwardInfos.get(i).reason;
                ret[i].serviceClass = callForwardInfos.get(i).serviceClass;
                ret[i].toa = callForwardInfos.get(i).toa;
                ret[i].number = callForwardInfos.get(i).number;
                ret[i].timeSeconds = callForwardInfos.get(i).timeSeconds;
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseOperatorInfos(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.OperatorInfo> networkInfos) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            ArrayList<OperatorInfo> ret = new ArrayList<OperatorInfo>();
            for (int i = 0; i < networkInfos.size(); i++) {
                ret.add(new OperatorInfo(networkInfos.get(i).alphaLong,
                        networkInfos.get(i).alphaShort, networkInfos.get(i).operatorNumeric,
                        RILUtils.convertHalOperatorStatus(networkInfos.get(i).status)));
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseScanStatus(RadioResponseInfo responseInfo, HalVersion fallbackHalVersion) {
        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr == null) {
            return;
        }

        final boolean needFallback = responseInfo.error == RadioError.REQUEST_NOT_SUPPORTED
                && fallbackHalVersion != null && rr.mArguments != null && rr.mArguments.length > 0
                && rr.mArguments[0] instanceof NetworkScanRequest;
        if (needFallback) {
            // Move the data needed for fallback call from rr which will be released soon
            final int request = rr.getRequest();
            final Message result = rr.getResult();
            final NetworkScanRequest scanRequest = (NetworkScanRequest) rr.mArguments[0];

            mRil.mRilHandler.post(() -> {
                mRil.setCompatVersion(request, RIL.RADIO_HAL_VERSION_1_4);
                mRil.startNetworkScan(scanRequest, result);
            });

            mRil.processResponseFallback(rr, responseInfo, null);
            return;
        }

        NetworkScanResult nsr = null;
        if (responseInfo.error == RadioError.NONE) {
            nsr = new NetworkScanResult(
                    NetworkScanResult.SCAN_STATUS_PARTIAL, RadioError.NONE, null);
            sendMessageResponse(rr.mResult, nsr);
        }
        mRil.processResponseDone(rr, responseInfo, nsr);
    }

    private void responseDataCallList(RadioResponseInfo responseInfo,
            List<? extends Object> dataCallResultList) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            ArrayList<DataCallResponse> response =
                    RILUtils.convertHalDataCallResultList(dataCallResultList);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, response);
            }
            mRil.processResponseDone(rr, responseInfo, response);
        }
    }

    private void responseDataCallList(android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            List<? extends Object> dataCallResultList) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);

        if (rr != null) {
            ArrayList<DataCallResponse> response =
                    RILUtils.convertHalDataCallResultList(dataCallResultList);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, response);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, response);
        }
    }

    private void responseCellList(RadioResponseInfo responseInfo,
            ArrayList<NeighboringCell> cells) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            int rssi;
            String location;
            ArrayList<NeighboringCellInfo> ret = new ArrayList<NeighboringCellInfo>();
            NeighboringCellInfo cell;

            int[] subId = SubscriptionManager.getSubId(mRil.mPhoneId);
            int radioType =
                    ((TelephonyManager) mRil.mContext.getSystemService(
                            Context.TELEPHONY_SERVICE)).getDataNetworkType(subId[0]);

            if (radioType != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                for (int i = 0; i < cells.size(); i++) {
                    rssi = cells.get(i).rssi;
                    location = cells.get(i).cid;
                    cell = new NeighboringCellInfo(rssi, location, radioType);
                    ret.add(cell);
                }
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseGmsBroadcastConfig(RadioResponseInfo responseInfo,
            ArrayList<GsmBroadcastSmsConfigInfo> configs) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            ArrayList<SmsBroadcastConfigInfo> ret = new ArrayList<>();
            for (int i = 0; i < configs.size(); i++) {
                ret.add(new SmsBroadcastConfigInfo(configs.get(i).fromServiceId,
                        configs.get(i).toServiceId, configs.get(i).fromCodeScheme,
                        configs.get(i).toCodeScheme, configs.get(i).selected));
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCdmaBroadcastConfig(RadioResponseInfo responseInfo,
            ArrayList<CdmaBroadcastSmsConfigInfo> configs) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            int[] ret = null;

            int numServiceCategories = configs.size();

            if (numServiceCategories == 0) {
                // TODO: The logic of providing default values should
                // not be done by this transport layer. And needs to
                // be done by the vendor ril or application logic.
                int numInts;
                numInts = RILUtils.CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES
                        * RILUtils.CDMA_BSI_NO_OF_INTS_STRUCT + 1;
                ret = new int[numInts];

                // Faking a default record for all possible records.
                ret[0] = RILUtils.CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES;

                // Loop over CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES set 'english' as
                // default language and selection status to false for all.
                for (int i = 1; i < numInts; i += RILUtils.CDMA_BSI_NO_OF_INTS_STRUCT) {
                    ret[i + 0] = i / RILUtils.CDMA_BSI_NO_OF_INTS_STRUCT;
                    ret[i + 1] = 1;
                    ret[i + 2] = 0;
                }
            } else {
                int numInts;
                numInts = (numServiceCategories * RILUtils.CDMA_BSI_NO_OF_INTS_STRUCT) + 1;
                ret = new int[numInts];

                ret[0] = numServiceCategories;
                for (int i = 1, j = 0; j < configs.size();
                        j++, i = i + RILUtils.CDMA_BSI_NO_OF_INTS_STRUCT) {
                    ret[i] = configs.get(j).serviceCategory;
                    ret[i + 1] = configs.get(j).language;
                    ret[i + 2] = configs.get(j).selected ? 1 : 0;
                }
            }
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCellInfoList(RadioResponseInfo responseInfo,
            ArrayList<? extends Object> cellInfo) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            ArrayList<CellInfo> ret = RILUtils.convertHalCellInfoList((ArrayList<Object>) cellInfo);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCellInfoList_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            ArrayList<? extends Object> cellInfo) {
        RILRequest rr = mRil.processResponse_1_6(responseInfo);

        if (rr != null) {
            ArrayList<CellInfo> ret = RILUtils.convertHalCellInfoList((ArrayList<Object>) cellInfo);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone_1_6(rr, responseInfo, ret);
        }
    }

    private void responseActivityData(RadioResponseInfo responseInfo,
            ActivityStatsInfo activityInfo) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            ModemActivityInfo ret = null;
            if (responseInfo.error == RadioError.NONE) {
                final int sleepModeTimeMs = activityInfo.sleepModeTimeMs;
                final int idleModeTimeMs = activityInfo.idleModeTimeMs;
                int [] txModeTimeMs = new int[ModemActivityInfo.getNumTxPowerLevels()];
                for (int i = 0; i < ModemActivityInfo.getNumTxPowerLevels(); i++) {
                    txModeTimeMs[i] = activityInfo.txmModetimeMs[i];
                }
                final int rxModeTimeMs = activityInfo.rxModeTimeMs;
                ret = new ModemActivityInfo(SystemClock.elapsedRealtime(), sleepModeTimeMs,
                        idleModeTimeMs, txModeTimeMs, rxModeTimeMs);
            } else {
                ret = new ModemActivityInfo(0, 0, 0,
                        new int[ModemActivityInfo.getNumTxPowerLevels()], 0);
                responseInfo.error = RadioError.NONE;
            }
            sendMessageResponse(rr.mResult, ret);
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseHardwareConfig(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.V1_0.HardwareConfig> config) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            ArrayList<HardwareConfig> ret = RILUtils.convertHalHardwareConfigList(config);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseRadioCapability(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_0.RadioCapability rc) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            RadioCapability ret = RILUtils.convertHalRadioCapability(rc, mRil);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseLceStatus(RadioResponseInfo responseInfo, LceStatusInfo statusInfo) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            ArrayList<Integer> ret = new ArrayList<>();
            ret.add(statusInfo.lceStatus);
            ret.add(Byte.toUnsignedInt(statusInfo.actualIntervalMs));
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseLceData(RadioResponseInfo responseInfo, LceDataInfo lceInfo) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            List<LinkCapacityEstimate> ret = RILUtils.convertHalLceData(lceInfo);
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    private void responseCarrierRestrictions(RadioResponseInfo responseInfo, boolean allAllowed,
            CarrierRestrictionsWithPriority carriers, int multiSimPolicy) {
        RILRequest rr = mRil.processResponse(responseInfo);
        if (rr == null) {
            return;
        }
        CarrierRestrictionRules ret;

        if (allAllowed) {
            ret = CarrierRestrictionRules.newBuilder().setAllCarriersAllowed().build();
        } else {
            int policy = CarrierRestrictionRules.MULTISIM_POLICY_NONE;
            if (multiSimPolicy == SimLockMultiSimPolicy.ONE_VALID_SIM_MUST_BE_PRESENT) {
                policy = CarrierRestrictionRules.MULTISIM_POLICY_ONE_VALID_SIM_MUST_BE_PRESENT;
            }

            int carrierRestrictionDefault =
                    CarrierRestrictionRules.CARRIER_RESTRICTION_DEFAULT_NOT_ALLOWED;
            if (!carriers.allowedCarriersPrioritized) {
                carrierRestrictionDefault =
                        CarrierRestrictionRules.CARRIER_RESTRICTION_DEFAULT_ALLOWED;
            }

            ret = CarrierRestrictionRules.newBuilder()
                    .setAllowedCarriers(RILUtils.convertHalCarrierList(carriers.allowedCarriers))
                    .setExcludedCarriers(RILUtils.convertHalCarrierList(carriers.excludedCarriers))
                    .setDefaultCarrierRestriction(carrierRestrictionDefault)
                    .setMultiSimPolicy(policy)
                    .build();
        }

        if (responseInfo.error == RadioError.NONE) {
            sendMessageResponse(rr.mResult, ret);
        }
        mRil.processResponseDone(rr, responseInfo, ret);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial number and error.
     */
    public void enableModemResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param isEnabled whether the modem stack is enabled.
     */
    public void getModemStackStatusResponse(RadioResponseInfo responseInfo, boolean isEnabled) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, isEnabled);
            }
            mRil.processResponseDone(rr, responseInfo, isEnabled);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial number and error.
     */
    public void setSystemSelectionChannelsResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     */
    public void enableUiccApplicationsResponse(RadioResponseInfo responseInfo) {
        responseVoid(responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param enabled whether Uicc applications are enabled.
     */
    public void areUiccApplicationsEnabledResponse(RadioResponseInfo responseInfo,
            boolean enabled) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, enabled);
            }
            mRil.processResponseDone(rr, responseInfo, enabled);
        }
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void setRadioPowerResponse_1_5(RadioResponseInfo info) {
        responseVoid(info);
        mRil.mLastRadioPowerResult = info.error;
        if (info.error != RadioError.RADIO_NOT_AVAILABLE && info.error != RadioError.NONE) {
            AnomalyReporter.reportAnomaly(
                    UUID.fromString(RILUtils.RADIO_POWER_FAILURE_BUGREPORT_UUID),
                    "Radio power failure");
        }
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void setRadioPowerResponse_1_6(android.hardware.radio.V1_6.RadioResponseInfo info) {
        responseVoid_1_6(info);
        mRil.mLastRadioPowerResult = info.error;
        if (info.error == android.hardware.radio.V1_6.RadioError.RF_HARDWARE_ISSUE) {
            AnomalyReporter.reportAnomaly(
                    UUID.fromString(RILUtils.RADIO_POWER_FAILURE_RF_HARDWARE_ISSUE_UUID),
                    "RF HW damaged");
        } else if (info.error == android.hardware.radio.V1_6.RadioError.NO_RF_CALIBRATION_INFO) {
            AnomalyReporter.reportAnomaly(
                    UUID.fromString(RILUtils.RADIO_POWER_FAILURE_NO_RF_CALIBRATION_UUID),
                    "No RF calibration data");
        } else if (info.error != android.hardware.radio.V1_6.RadioError.RADIO_NOT_AVAILABLE
                && info.error != android.hardware.radio.V1_6.RadioError.NONE) {
            AnomalyReporter.reportAnomaly(
                    UUID.fromString(RILUtils.RADIO_POWER_FAILURE_BUGREPORT_UUID),
                    "Radio power failure");
        }
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void setSystemSelectionChannelsResponse_1_5(RadioResponseInfo info) {
        responseVoid(info);
    }


    /**
     * @param info Response info struct containing response type, serial no. and error.
     * @param halSpecifiers List of RadioAccessSpecifiers that are scanned.
     */
    public void getSystemSelectionChannelsResponse(
            android.hardware.radio.V1_6.RadioResponseInfo info,
            ArrayList<android.hardware.radio.V1_5.RadioAccessSpecifier> halSpecifiers) {
        RILRequest rr = mRil.processResponse_1_6(info);

        if (rr != null) {
            ArrayList<RadioAccessSpecifier> specifiers = new ArrayList<>();
            for (android.hardware.radio.V1_5.RadioAccessSpecifier specifier : halSpecifiers) {
                specifiers.add(RILUtils.convertHalRadioAccessSpecifier(specifier));
            }
            mRil.riljLog("getSystemSelectionChannelsResponse: from HIDL: " + specifiers);
            if (info.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, specifiers);
            }
            mRil.processResponseDone_1_6(rr, info, specifiers);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param cellIdentity CellIdentity for the barringInfos.
     * @param barringInfos List of BarringInfo for all the barring service types.
     */
    public void getBarringInfoResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.V1_5.CellIdentity cellIdentity,
            ArrayList<android.hardware.radio.V1_5.BarringInfo> barringInfos) {
        RILRequest rr = mRil.processResponse(responseInfo);

        if (rr != null) {
            BarringInfo bi = new BarringInfo(RILUtils.convertHalCellIdentity(cellIdentity),
                    RILUtils.convertHalBarringInfoList(barringInfos));
            if (responseInfo.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, bi);
                // notify all registrants for the possible barring info change
                mRil.mBarringInfoChangedRegistrants.notifyRegistrants(
                        new AsyncResult(null, bi, null));
            }
            mRil.processResponseDone(rr, responseInfo, bi);
        }
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param id The pdu session id allocated
     */
    public void allocatePduSessionIdResponse(android.hardware.radio.V1_6.RadioResponseInfo info,
            int id) {
        RILRequest rr = mRil.processResponse_1_6(info);
        if (rr != null) {
            if (info.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, id);
            }
            mRil.processResponseDone_1_6(rr, info, id);
        }
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     */
    public void releasePduSessionIdResponse(android.hardware.radio.V1_6.RadioResponseInfo info) {
        responseVoid_1_6(info);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     */
    public void startHandoverResponse(android.hardware.radio.V1_6.RadioResponseInfo info) {
        responseVoid_1_6(info);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     */
    public void cancelHandoverResponse(android.hardware.radio.V1_6.RadioResponseInfo info) {
        responseVoid_1_6(info);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param slicingConfig Current slicing configuration
     */
    public void getSlicingConfigResponse(android.hardware.radio.V1_6.RadioResponseInfo info,
            android.hardware.radio.V1_6.SlicingConfig slicingConfig) {
        RILRequest rr = mRil.processResponse_1_6(info);

        if (rr != null) {
            NetworkSlicingConfig ret = RILUtils.convertHalSlicingConfig(slicingConfig);
            if (info.error == RadioError.NONE) {
                sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone_1_6(rr, info, ret);
        }
    }
}

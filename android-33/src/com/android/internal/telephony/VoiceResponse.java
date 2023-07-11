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
import android.hardware.radio.voice.IRadioVoiceResponse;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Interface declaring response functions to solicited radio requests for SIM APIs.
 */
public class VoiceResponse extends IRadioVoiceResponse.Stub {
    private final RIL mRil;

    public VoiceResponse(RIL ril) {
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
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acceptCallResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void cancelPendingUssdResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void conferenceResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void dialResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void emergencyDialResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void exitEmergencyCallbackModeResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void explicitCallTransferResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param callForwardInfos points to a vector of CallForwardInfo, one for
     *        each distinct registered phone number.
     */
    public void getCallForwardStatusResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.voice.CallForwardInfo[] callForwardInfos) {
        RILRequest rr = mRil.processResponse(RIL.VOICE_SERVICE, responseInfo);
        if (rr != null) {
            CallForwardInfo[] ret = new CallForwardInfo[callForwardInfos.length];
            for (int i = 0; i < callForwardInfos.length; i++) {
                ret[i] = new CallForwardInfo();
                ret[i].status = callForwardInfos[i].status;
                ret[i].reason = callForwardInfos[i].reason;
                ret[i].serviceClass = callForwardInfos[i].serviceClass;
                ret[i].toa = callForwardInfos[i].toa;
                ret[i].number = callForwardInfos[i].number;
                ret[i].timeSeconds = callForwardInfos[i].timeSeconds;
            }
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable If current call waiting state is disabled, enable = false else true
     * @param serviceClass If enable, then callWaitingResp[1] must follow, with the TS 27.007
     *        service class bit vector of services for which call waiting is enabled.
     *        For example, if callWaitingResp[0] is 1 and callWaitingResp[1] is 3, then call waiting
     *        is enabled for data and voice and disabled for everything else.
     */
    public void getCallWaitingResponse(RadioResponseInfo responseInfo, boolean enable,
            int serviceClass) {
        RadioResponse.responseInts(
                RIL.VOICE_SERVICE, mRil, responseInfo, enable ? 1 : 0, serviceClass);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param status indicates CLIP status
     */
    public void getClipResponse(RadioResponseInfo responseInfo, int status) {
        RadioResponse.responseInts(RIL.VOICE_SERVICE, mRil, responseInfo, status);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param n is "n" parameter from TS 27.007 7.7
     * @param m is "m" parameter from TS 27.007 7.7
     */
    public void getClirResponse(RadioResponseInfo responseInfo, int n, int m) {
        RadioResponse.responseInts(RIL.VOICE_SERVICE, mRil, responseInfo, n, m);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param calls Current call list
     */
    public void getCurrentCallsResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.voice.Call[] calls) {
        RILRequest rr = mRil.processResponse(RIL.VOICE_SERVICE, responseInfo);

        if (rr != null) {
            int num = calls.length;
            ArrayList<DriverCall> dcCalls = new ArrayList<>(num);
            DriverCall dc;
            for (int i = 0; i < num; i++) {
                dc = RILUtils.convertToDriverCall(calls[i]);
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
                RadioResponse.sendMessageResponse(rr.mResult, dcCalls);
            }
            mRil.processResponseDone(rr, responseInfo, dcCalls);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param fcInfo Contains LastCallFailCause and vendor cause code. GSM failure reasons
     *        are mapped to cause codes defined in TS 24.008 Annex H where possible. CDMA failure
     *        reasons are derived from the possible call failure scenarios described in the
     *        "CDMA IS-2000 Release A (C.S0005-A v6.0)" standard.
     */
    public void getLastCallFailCauseResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.voice.LastCallFailCauseInfo fcInfo) {
        RILRequest rr = mRil.processResponse(RIL.VOICE_SERVICE, responseInfo);

        if (rr != null) {
            LastCallFailCause ret = new LastCallFailCause();
            ret.causeCode = fcInfo.causeCode;
            ret.vendorCause = fcInfo.vendorCause;
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable true for "mute enabled" and false for "mute disabled"
     */
    public void getMuteResponse(RadioResponseInfo responseInfo, boolean enable) {
        RadioResponse.responseInts(RIL.VOICE_SERVICE, mRil, responseInfo, enable ? 1 : 0);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable false for Standard Privacy Mode (Public Long Code Mask)
     *        true for Enhanced Privacy Mode (Private Long Code Mask)
     */
    public void getPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo, boolean enable) {
        RadioResponse.responseInts(RIL.VOICE_SERVICE, mRil, responseInfo, enable ? 1 : 0);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param mode TTY mode
     */
    public void getTtyModeResponse(RadioResponseInfo responseInfo, int mode) {
        RadioResponse.responseInts(RIL.VOICE_SERVICE, mRil, responseInfo, mode);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupConnectionResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void hangupWaitingOrBackgroundResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param enable true for "vonr enabled" and false for "vonr disabled"
     */
    public void isVoNrEnabledResponse(RadioResponseInfo responseInfo, boolean enable) {
        RILRequest rr = mRil.processResponse(RIL.VOICE_SERVICE, responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, enable);
            }
            mRil.processResponseDone(rr, responseInfo, enable);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void rejectCallResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendBurstDtmfResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendCdmaFeatureCodeResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendDtmfResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void sendUssdResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void separateConnectionResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCallForwardResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCallWaitingResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setClirResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setMuteResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setPreferredVoicePrivacyResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setTtyModeResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setVoNrEnabledResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void startDtmfResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void stopDtmfResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.VOICE_SERVICE, mRil, responseInfo);
    }

    @Override
    public String getInterfaceHash() {
        return IRadioVoiceResponse.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return IRadioVoiceResponse.VERSION;
    }
}

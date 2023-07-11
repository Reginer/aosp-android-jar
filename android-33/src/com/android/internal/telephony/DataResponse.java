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
import android.hardware.radio.data.IRadioDataResponse;
import android.telephony.data.DataCallResponse;
import android.telephony.data.NetworkSlicingConfig;

import com.android.internal.telephony.data.KeepaliveStatus;

import java.util.ArrayList;

/**
 * Interface declaring response functions to solicited radio requests for data APIs.
 */
public class DataResponse extends IRadioDataResponse.Stub {
    private final RIL mRil;

    public DataResponse(RIL ril) {
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
     * @param id The pdu session id allocated
     */
    public void allocatePduSessionIdResponse(RadioResponseInfo responseInfo, int id) {
        RILRequest rr = mRil.processResponse(RIL.DATA_SERVICE, responseInfo);
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, id);
            }
            mRil.processResponseDone(rr, responseInfo, id);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void cancelHandoverResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.DATA_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deactivateDataCallResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.DATA_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataCallResultList Response to get data call list as defined by SetupDataCallResult
     */
    public void getDataCallListResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.data.SetupDataCallResult[] dataCallResultList) {
        RILRequest rr = mRil.processResponse(RIL.DATA_SERVICE, responseInfo);

        if (rr != null) {
            ArrayList<DataCallResponse> response =
                    RILUtils.convertHalDataCallResultList(dataCallResultList);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, response);
            }
            mRil.processResponseDone(rr, responseInfo, response);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param slicingConfig Current slicing configuration
     */
    public void getSlicingConfigResponse(RadioResponseInfo responseInfo,
                android.hardware.radio.data.SlicingConfig slicingConfig) {
        RILRequest rr = mRil.processResponse(RIL.DATA_SERVICE, responseInfo);

        if (rr != null) {
            NetworkSlicingConfig ret = RILUtils.convertHalSlicingConfig(slicingConfig);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void releasePduSessionIdResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.DATA_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setDataAllowedResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.DATA_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setDataProfileResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.DATA_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setDataThrottlingResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.DATA_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setInitialAttachApnResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.DATA_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param setupDataCallResult Response to data call setup as defined by SetupDataCallResult
     */
    public void setupDataCallResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.data.SetupDataCallResult setupDataCallResult) {
        RILRequest rr = mRil.processResponse(RIL.DATA_SERVICE, responseInfo);

        if (rr != null) {
            DataCallResponse response = RILUtils.convertHalDataCallResult(setupDataCallResult);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, response);
            }
            mRil.processResponseDone(rr, responseInfo, response);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void startHandoverResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.DATA_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param keepaliveStatus status of the keepalive with a handle for the session
     */
    public void startKeepaliveResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.data.KeepaliveStatus keepaliveStatus) {

        RILRequest rr = mRil.processResponse(RIL.DATA_SERVICE, responseInfo);
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
                    RadioResponse.sendMessageResponse(rr.mResult, ret);
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
        RILRequest rr = mRil.processResponse(RIL.DATA_SERVICE, responseInfo);
        if (rr == null) return;

        try {
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, null);
            } else {
                //TODO: Error code translation
            }
        } finally {
            mRil.processResponseDone(rr, responseInfo, null);
        }
    }

    @Override
    public String getInterfaceHash() {
        return IRadioDataResponse.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return IRadioDataResponse.VERSION;
    }
}

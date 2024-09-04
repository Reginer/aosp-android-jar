/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.telephony.TelephonyManager.HAL_SERVICE_IMS;
import static android.telephony.ims.feature.MmTelFeature.ImsTrafficSessionCallbackWrapper.INVALID_TOKEN;

import android.hardware.radio.RadioError;
import android.hardware.radio.RadioResponseInfo;
import android.hardware.radio.ims.IRadioImsResponse;
import android.telephony.ims.feature.ConnectionFailureInfo;

/**
 * Interface declaring response functions to solicited radio requests for IMS APIs.
 */
public class ImsResponse extends IRadioImsResponse.Stub {
    private final RIL mRil;

    public ImsResponse(RIL ril) {
        mRil = ril;
    }

    @Override
    public String getInterfaceHash() {
        return IRadioImsResponse.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return IRadioImsResponse.VERSION;
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void setSrvccCallInfoResponse(RadioResponseInfo info) {
        RadioResponse.responseVoid(HAL_SERVICE_IMS, mRil, info);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void updateImsRegistrationInfoResponse(RadioResponseInfo info) {
        RadioResponse.responseVoid(HAL_SERVICE_IMS, mRil, info);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param failureInfo Failure information.
     */
    public void startImsTrafficResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.ims.ConnectionFailureInfo failureInfo) {
        RILRequest rr = mRil.processResponse(HAL_SERVICE_IMS, responseInfo);

        if (rr != null) {
            Object[] response = { new Integer(INVALID_TOKEN), null };
            if (responseInfo.error == RadioError.NONE) {
                if (failureInfo != null) {
                    response[1] = new ConnectionFailureInfo(
                            RILUtils.convertHalConnectionFailureReason(failureInfo.failureReason),
                            failureInfo.causeCode, failureInfo.waitTimeMillis);
                }
                RadioResponse.sendMessageResponse(rr.mResult, response);
            }
            mRil.processResponseDone(rr, responseInfo, response);
        }
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void stopImsTrafficResponse(RadioResponseInfo info) {
        RadioResponse.responseVoid(HAL_SERVICE_IMS, mRil, info);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void triggerEpsFallbackResponse(RadioResponseInfo info) {
        RadioResponse.responseVoid(HAL_SERVICE_IMS, mRil, info);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void sendAnbrQueryResponse(RadioResponseInfo info) {
        RadioResponse.responseVoid(HAL_SERVICE_IMS, mRil, info);
    }

    /**
     * @param info Response info struct containing response type, serial no. and error.
     */
    public void updateImsCallStatusResponse(RadioResponseInfo info) {
        RadioResponse.responseVoid(HAL_SERVICE_IMS, mRil, info);
    }
}

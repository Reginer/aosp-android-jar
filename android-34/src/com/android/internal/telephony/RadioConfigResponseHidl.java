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

import android.os.RemoteException;
import android.telephony.PhoneCapability;

import com.android.internal.telephony.uicc.IccSlotStatus;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.Set;

/**
 * This class is the AIDL implementation of IRadioConfigResponse interface.
 */
public class RadioConfigResponseHidl extends
        android.hardware.radio.config.V1_3.IRadioConfigResponse.Stub {
    private static final String TAG = "RadioConfigResponse";

    private final RadioConfig mRadioConfig;
    private final HalVersion mHalVersion;

    public RadioConfigResponseHidl(RadioConfig radioConfig, HalVersion halVersion) {
        mRadioConfig = radioConfig;
        mHalVersion = halVersion;
    }

    /**
     * Response function for IRadioConfig.getSimSlotsStatus().
     */
    @Override
    public void getSimSlotsStatusResponse(
            android.hardware.radio.V1_0.RadioResponseInfo info,
            ArrayList<android.hardware.radio.config.V1_0.SimSlotStatus> slotStatus)
            throws RemoteException {
        RILRequest rr = mRadioConfig.processResponse(info);

        if (rr != null) {
            ArrayList<IccSlotStatus> ret = RILUtils.convertHalSlotStatus(slotStatus);
            if (info.error == android.hardware.radio.V1_0.RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, ret);
                logd(rr, RILUtils.requestToString(rr.mRequest) + " " + ret.toString());
            } else {
                rr.onError(info.error, ret);
                loge(rr, RILUtils.requestToString(rr.mRequest) + " error " + info.error);
            }
        } else {
            loge("getSimSlotsStatusResponse: Error " + info.toString());
        }
    }

    /**
     * Response function for IRadioConfig.setSimSlotsMapping().
     */
    @Override
    public void setSimSlotsMappingResponse(android.hardware.radio.V1_0.RadioResponseInfo info)
            throws RemoteException {
        RILRequest rr = mRadioConfig.processResponse(info);
        if (rr != null) {
            if (info.error == android.hardware.radio.V1_0.RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, null);
                logd(rr, RILUtils.requestToString(rr.mRequest));
            } else {
                rr.onError(info.error, null);
                loge(rr, RILUtils.requestToString(rr.mRequest) + " error " + info.error);
            }
        } else {
            loge("setSimSlotsMappingResponse: Error " + info.toString());
        }
    }

    /**
     * Response function for IRadioConfig.getPhoneCapability().
     */
    @Override
    public void getPhoneCapabilityResponse(android.hardware.radio.V1_0.RadioResponseInfo info,
            android.hardware.radio.config.V1_1.PhoneCapability phoneCapability)
            throws RemoteException {
        RILRequest rr = mRadioConfig.processResponse(info);
        if (rr != null) {
            PhoneCapability ret = RILUtils.convertHalPhoneCapability(
                    mRadioConfig.getDeviceNrCapabilities(), phoneCapability);
            if (info.error == android.hardware.radio.V1_0.RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, ret);
                logd(rr, RILUtils.requestToString(rr.mRequest) + " " + ret.toString());
            } else {
                rr.onError(info.error, ret);
                loge(rr, RILUtils.requestToString(rr.mRequest) + " error " + info.error);
            }
        } else {
            loge("getPhoneCapabilityResponse: Error " + info.toString());
        }
    }

    /**
     * Response function for IRadioConfig.setPreferredDataModem().
     */
    @Override
    public void setPreferredDataModemResponse(
            android.hardware.radio.V1_0.RadioResponseInfo info) throws RemoteException {
        RILRequest rr = mRadioConfig.processResponse(info);
        if (rr != null) {
            if (info.error == android.hardware.radio.V1_0.RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, null);
                logd(rr, RILUtils.requestToString(rr.mRequest));
            } else {
                rr.onError(info.error, null);
                loge(rr, RILUtils.requestToString(rr.mRequest) + " error " + info.error);
            }
        } else {
            loge("setPreferredDataModemResponse: Error " + info.toString());
        }
    }

    /**
     * Response function for IRadioConfig.setModemsConfigResponse()
     * Currently this is being used as the callback for RadioConfig.setModemsConfig() method
     */
    @Override
    public void setModemsConfigResponse(android.hardware.radio.V1_0.RadioResponseInfo info)
            throws RemoteException {
        RILRequest rr = mRadioConfig.processResponse(info);
        if (rr != null) {
            if (info.error == android.hardware.radio.V1_0.RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, rr.mRequest);
                logd(rr, RILUtils.requestToString(rr.mRequest));
            } else {
                rr.onError(info.error, null);
                loge(rr, RILUtils.requestToString(rr.mRequest) + " error " + info.error);
            }
        } else {
            loge("setModemsConfigResponse: Error " + info.toString());
        }
    }

    /**
     * Response function for IRadioConfig.getModemsConfigResponse()
     */
    @Override
    public void getModemsConfigResponse(android.hardware.radio.V1_0.RadioResponseInfo info,
            android.hardware.radio.config.V1_1.ModemsConfig modemsConfig)
            throws RemoteException {
        RILRequest rr = mRadioConfig.processResponse(info);
        if (rr != null) {
            if (info.error == android.hardware.radio.V1_0.RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, modemsConfig);
                logd(rr, RILUtils.requestToString(rr.mRequest));
            } else {
                rr.onError(info.error, modemsConfig);
                loge(rr, RILUtils.requestToString(rr.mRequest) + " error " + info.error);
            }
        } else {
            loge("getModemsConfigResponse: Error " + info.toString());
        }
    }

    /**
     * Response function for IRadioConfig.getSimSlotsStatus().
     */
    @Override
    public void getSimSlotsStatusResponse_1_2(
            android.hardware.radio.V1_0.RadioResponseInfo info,
            ArrayList<android.hardware.radio.config.V1_2.SimSlotStatus> slotStatus)
            throws RemoteException {
        RILRequest rr = mRadioConfig.processResponse(info);
        if (rr != null) {
            ArrayList<IccSlotStatus> ret = RILUtils.convertHalSlotStatus(slotStatus);
            if (info.error == android.hardware.radio.V1_0.RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, ret);
                logd(rr, RILUtils.requestToString(rr.mRequest) + " " + ret.toString());
            } else {
                rr.onError(info.error, ret);
                loge(rr, RILUtils.requestToString(rr.mRequest) + " error " + info.error);
            }
        } else {
            loge("getSimSlotsStatusResponse_1_2: Error " + info.toString());
        }
    }

    /**
     * Response function IRadioConfig.getHalDeviceCapabilities()
     */
    @Override
    public void getHalDeviceCapabilitiesResponse(
            android.hardware.radio.V1_6.RadioResponseInfo info, boolean modemReducedFeatureSet1)
            throws RemoteException {
        // convert hal device capabilities to RadioInterfaceCapabilities
        RILRequest rr = mRadioConfig.processResponse_1_6(info);
        if (rr != null) {
            // The response is compatible with Radio 1.6, it means the modem
            // supports setAllowedNetworkTypeBitmap.
            final Set<String> ret = RILUtils.getCaps(mHalVersion,
                    modemReducedFeatureSet1);
            if (info.error == android.hardware.radio.V1_0.RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, ret);
                logd(rr, RILUtils.requestToString(rr.mRequest));
            } else {
                rr.onError(info.error, ret);
                loge(rr, RILUtils.requestToString(rr.mRequest) + " error " + info.error);
            }
        } else {
            loge("getHalDeviceCapabilities: Error " + info.toString());
        }
    }

    private static void logd(String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(String log) {
        Rlog.e(TAG, log);
    }

    private static void logd(RILRequest rr, String log) {
        logd(rr.serialString() + "< " + log);
    }

    private static void loge(RILRequest rr, String log) {
        loge(rr.serialString() + "< " + log);
    }
}

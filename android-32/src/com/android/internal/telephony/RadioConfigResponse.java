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

import static android.telephony.TelephonyManager
        .CAPABILITY_NR_DUAL_CONNECTIVITY_CONFIGURATION_AVAILABLE;
import static android.telephony.TelephonyManager.CAPABILITY_PHYSICAL_CHANNEL_CONFIG_1_6_SUPPORTED;
import static android.telephony.TelephonyManager.CAPABILITY_SECONDARY_LINK_BANDWIDTH_VISIBLE;
import static android.telephony.TelephonyManager.CAPABILITY_SIM_PHONEBOOK_IN_MODEM;
import static android.telephony.TelephonyManager.CAPABILITY_SLICING_CONFIG_SUPPORTED;
import static android.telephony.TelephonyManager.CAPABILITY_THERMAL_MITIGATION_DATA_THROTTLING;
import static android.telephony.TelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK;
import static android.telephony.TelephonyManager.RadioInterfaceCapability;

import android.hardware.radio.V1_0.RadioError;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.config.V1_1.ModemsConfig;
import android.hardware.radio.config.V1_3.IRadioConfigResponse;
import android.telephony.ModemInfo;
import android.telephony.PhoneCapability;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.uicc.IccSlotStatus;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is the implementation of IRadioConfigResponse interface.
 */
public class RadioConfigResponse extends IRadioConfigResponse.Stub {
    private static final String TAG = "RadioConfigResponse";

    private final RadioConfig mRadioConfig;
    private final HalVersion mRadioHalVersion;

    public RadioConfigResponse(RadioConfig radioConfig, HalVersion radioHalVersion) {
        mRadioConfig = radioConfig;
        mRadioHalVersion = radioHalVersion;
    }

    /**
     * Response function for IRadioConfig.getSimSlotsStatus().
     */
    public void getSimSlotsStatusResponse(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.config.V1_0.SimSlotStatus> slotStatus) {
        RILRequest rr = mRadioConfig.processResponse(responseInfo);

        if (rr != null) {
            ArrayList<IccSlotStatus> ret = RadioConfig.convertHalSlotStatus(slotStatus);
            if (responseInfo.error == RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, ret);
                Rlog.d(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest) + " " + ret.toString());
            } else {
                rr.onError(responseInfo.error, ret);
                Rlog.e(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest) + " error "
                        + responseInfo.error);
            }

        } else {
            Rlog.e(TAG, "getSimSlotsStatusResponse: Error " + responseInfo.toString());
        }
    }

    /**
     * Response function for IRadioConfig.getSimSlotsStatus().
     */
    public void getSimSlotsStatusResponse_1_2(RadioResponseInfo responseInfo,
            ArrayList<android.hardware.radio.config.V1_2.SimSlotStatus> slotStatus) {
        RILRequest rr = mRadioConfig.processResponse(responseInfo);

        if (rr != null) {
            ArrayList<IccSlotStatus> ret = RadioConfig.convertHalSlotStatus_1_2(slotStatus);
            if (responseInfo.error == RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, ret);
                Rlog.d(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest) + " " + ret.toString());
            } else {
                rr.onError(responseInfo.error, ret);
                Rlog.e(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest) + " error "
                        + responseInfo.error);
            }
        } else {
            Rlog.e(TAG, "getSimSlotsStatusResponse_1_2: Error " + responseInfo.toString());
        }
    }

    /**
     * Response function for IRadioConfig.setSimSlotsMapping().
     */
    public void setSimSlotsMappingResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mRadioConfig.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, null);
                Rlog.d(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest));
            } else {
                rr.onError(responseInfo.error, null);
                Rlog.e(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest) + " error "
                        + responseInfo.error);
            }
        } else {
            Rlog.e(TAG, "setSimSlotsMappingResponse: Error " + responseInfo.toString());
        }
    }

    private PhoneCapability convertHalPhoneCapability(
            android.hardware.radio.config.V1_1.PhoneCapability phoneCapability) {
        // TODO b/121394331: clean up V1_1.PhoneCapability fields.
        int maxActiveVoiceCalls = 0;
        int maxActiveData = phoneCapability.maxActiveData;
        boolean validationBeforeSwitchSupported = phoneCapability.isInternetLingeringSupported;
        List<ModemInfo> logicalModemList = new ArrayList();

        for (android.hardware.radio.config.V1_1.ModemInfo
                modemInfo : phoneCapability.logicalModemList) {
            logicalModemList.add(new ModemInfo(modemInfo.modemId));
        }

        return new PhoneCapability(maxActiveVoiceCalls, maxActiveData, logicalModemList,
                validationBeforeSwitchSupported, mRadioConfig.getDeviceNrCapabilities());
    }
    /**
     * Response function for IRadioConfig.getPhoneCapability().
     */
    public void getPhoneCapabilityResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.config.V1_1.PhoneCapability phoneCapability) {
        RILRequest rr = mRadioConfig.processResponse(responseInfo);

        if (rr != null) {
            PhoneCapability ret = convertHalPhoneCapability(phoneCapability);
            if (responseInfo.error == RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, ret);
                Rlog.d(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest) + " " + ret.toString());
            } else {
                rr.onError(responseInfo.error, ret);
                Rlog.e(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest) + " error "
                        + responseInfo.error);
            }
        } else {
            Rlog.e(TAG, "getPhoneCapabilityResponse: Error " + responseInfo.toString());
        }
    }

    /**
     * Response function for IRadioConfig.setPreferredDataModem().
     */
    public void setPreferredDataModemResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mRadioConfig.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, null);
                Rlog.d(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest));
            } else {
                rr.onError(responseInfo.error, null);
                Rlog.e(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest) + " error "
                        + responseInfo.error);
            }
        } else {
            Rlog.e(TAG, "setPreferredDataModemResponse: Error " + responseInfo.toString());
        }
    }

    /**
     * Response function for IRadioConfig.setModemsConfigResponse()
     * Currently this is being used as the callback for RadioConfig.setModemsConfig() method
     */
    public void setModemsConfigResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mRadioConfig.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, rr.mRequest);
                Rlog.d(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest));
            } else {
                rr.onError(responseInfo.error, null);
                Rlog.e(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest) + " error "
                        + responseInfo.error);
            }
        } else {
            Rlog.e(TAG, "setModemsConfigResponse: Error " + responseInfo.toString());
        }
    }

    /**
     * Response function for IRadioConfig.getModemsConfigResponse()
     */
    public void getModemsConfigResponse(RadioResponseInfo responseInfo, ModemsConfig modemsConfig) {
        RILRequest rr = mRadioConfig.processResponse(responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, modemsConfig);
                Rlog.d(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest));
            } else {
                rr.onError(responseInfo.error, modemsConfig);
                Rlog.e(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest) + " error "
                        + responseInfo.error);
            }
        } else {
            Rlog.e(TAG, "getModemsConfigResponse: Error " + responseInfo.toString());
        }
    }

    /**
     * Response function IRadioConfig.getHalDeviceCapabilities()
     */
    public void getHalDeviceCapabilitiesResponse(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
            boolean modemReducedFeatureSet1) {

        // convert hal device capabilities to RadioInterfaceCapabilities

        RILRequest rr = mRadioConfig.processResponse_1_6(responseInfo);
        if (rr != null) {
            // The response is compatible with Radio 1.6, it means the modem
            // supports setAllowedNetworkTypeBitmap.

            final Set<String> ret = getCaps(mRadioHalVersion, modemReducedFeatureSet1);

            if (responseInfo.error == RadioError.NONE) {
                // send response
                RadioResponse.sendMessageResponse(rr.mResult, ret);
                Rlog.d(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest));
            } else {
                rr.onError(responseInfo.error, ret);
                Rlog.e(TAG, rr.serialString() + "< "
                        + mRadioConfig.requestToString(rr.mRequest) + " error "
                        + responseInfo.error);
            }
        } else {
            Rlog.e(TAG, "getHalDeviceCapabilities: Error " + responseInfo.toString());
        }
    }

    /**
     * Returns all capabilities supported in the most recent radio hal version.
     * <p/>
     * Used in the {@link RILConstants.REQUEST_NOT_SUPPORTED} case.
     *
     * @return all capabilities
     */
    @RadioInterfaceCapability
    public Set<String> getFullCapabilitySet() {
        return getCaps(mRadioHalVersion, false);
    }

    /**
     * Create capabilities based off of the radio hal version and feature set configurations.
     */
    @VisibleForTesting
    public static Set<String> getCaps(HalVersion radioHalVersion,
            boolean modemReducedFeatureSet1) {
        final Set<String> caps = new HashSet<>();

        if (radioHalVersion.equals(RIL.RADIO_HAL_VERSION_UNKNOWN)) {
            // If the Radio HAL is UNKNOWN, no capabilities will present themselves.
            Rlog.e(TAG, "Radio Hal Version is UNKNOWN!");
        }

        Rlog.d(TAG, "Radio Hal Version = " + radioHalVersion.toString());
        if (radioHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            caps.add(CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);
            Rlog.d(TAG, "CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK");

            if (!modemReducedFeatureSet1) {
                caps.add(CAPABILITY_SECONDARY_LINK_BANDWIDTH_VISIBLE);
                Rlog.d(TAG, "CAPABILITY_SECONDARY_LINK_BANDWIDTH_VISIBLE");
                caps.add(CAPABILITY_NR_DUAL_CONNECTIVITY_CONFIGURATION_AVAILABLE);
                Rlog.d(TAG, "CAPABILITY_NR_DUAL_CONNECTIVITY_CONFIGURATION_AVAILABLE");
                caps.add(CAPABILITY_THERMAL_MITIGATION_DATA_THROTTLING);
                Rlog.d(TAG, "CAPABILITY_THERMAL_MITIGATION_DATA_THROTTLING");
                caps.add(CAPABILITY_SLICING_CONFIG_SUPPORTED);
                Rlog.d(TAG, "CAPABILITY_SLICING_CONFIG_SUPPORTED");
                caps.add(CAPABILITY_PHYSICAL_CHANNEL_CONFIG_1_6_SUPPORTED);
                Rlog.d(TAG, "CAPABILITY_PHYSICAL_CHANNEL_CONFIG_1_6_SUPPORTED");
            } else {
                caps.add(CAPABILITY_SIM_PHONEBOOK_IN_MODEM);
                Rlog.d(TAG, "CAPABILITY_SIM_PHONEBOOK_IN_MODEM");
            }
        }
        return caps;
    }
}

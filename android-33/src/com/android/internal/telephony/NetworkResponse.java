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
import android.hardware.radio.network.IRadioNetworkResponse;
import android.os.AsyncResult;
import android.telephony.BarringInfo;
import android.telephony.CellInfo;
import android.telephony.LinkCapacityEstimate;
import android.telephony.RadioAccessSpecifier;
import android.telephony.SignalStrength;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface declaring response functions to solicited radio requests for network APIs.
 */
public class NetworkResponse extends IRadioNetworkResponse.Stub {
    private final RIL mRil;

    public NetworkResponse(RIL ril) {
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
     * @param halRadioAccessFamilyBitmap a 32-bit bitmap of RadioAccessFamily.
     */
    public void getAllowedNetworkTypesBitmapResponse(RadioResponseInfo responseInfo,
            int halRadioAccessFamilyBitmap) {
        int networkTypeBitmask = RILUtils.convertHalNetworkTypeBitMask(halRadioAccessFamilyBitmap);
        mRil.mAllowedNetworkTypesBitmask = networkTypeBitmask;
        RadioResponse.responseInts(RIL.NETWORK_SERVICE, mRil, responseInfo, networkTypeBitmask);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param bandModes List of RadioBandMode listing supported modes
     */
    public void getAvailableBandModesResponse(RadioResponseInfo responseInfo, int[] bandModes) {
        RadioResponse.responseIntArrayList(RIL.NETWORK_SERVICE, mRil, responseInfo,
                RILUtils.primitiveArrayToArrayList(bandModes));
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param networkInfos List of network operator information as OperatorInfos
     */
    public void getAvailableNetworksResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.network.OperatorInfo[] networkInfos) {
        RILRequest rr = mRil.processResponse(RIL.NETWORK_SERVICE, responseInfo);

        if (rr != null) {
            ArrayList<OperatorInfo> ret = new ArrayList<>();
            for (android.hardware.radio.network.OperatorInfo info : networkInfos) {
                ret.add(new OperatorInfo(info.alphaLong, info.alphaShort, info.operatorNumeric,
                        RILUtils.convertHalOperatorStatus(info.status)));
            }
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param cellIdentity CellIdentity for the barringInfos.
     * @param barringInfos List of BarringInfo for all the barring service types.
     */
    public void getBarringInfoResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.network.CellIdentity cellIdentity,
            android.hardware.radio.network.BarringInfo[] barringInfos) {
        RILRequest rr = mRil.processResponse(RIL.NETWORK_SERVICE, responseInfo);

        if (rr != null) {
            BarringInfo bi = new BarringInfo(RILUtils.convertHalCellIdentity(cellIdentity),
                    RILUtils.convertHalBarringInfoList(barringInfos));
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, bi);
                // notify all registrants for the possible barring info change
                mRil.mBarringInfoChangedRegistrants.notifyRegistrants(
                        new AsyncResult(null, bi, null));
            }
            mRil.processResponseDone(rr, responseInfo, bi);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param type CdmaRoamingType defined in types.hal
     */
    public void getCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo, int type) {
        RadioResponse.responseInts(RIL.NETWORK_SERVICE, mRil, responseInfo, type);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param cellInfo List of current cell information known to radio
     */
    public void getCellInfoListResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.network.CellInfo[] cellInfo) {
        RILRequest rr = mRil.processResponse(RIL.NETWORK_SERVICE, responseInfo);

        if (rr != null) {
            ArrayList<CellInfo> ret = RILUtils.convertHalCellInfoList(cellInfo);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param dataRegResponse Current data registration response as defined by DataRegStateResult
     */
    public void getDataRegistrationStateResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.network.RegStateResult dataRegResponse) {
        RILRequest rr = mRil.processResponse(RIL.NETWORK_SERVICE, responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, dataRegResponse);
            }
            mRil.processResponseDone(rr, responseInfo, dataRegResponse);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param isRegistered false = not registered, true = registered
     * @param ratFamily RadioTechnologyFamily. This value is valid only if isRegistered is true.
     */
    public void getImsRegistrationStateResponse(RadioResponseInfo responseInfo,
            boolean isRegistered, int ratFamily) {
        RadioResponse.responseInts(RIL.NETWORK_SERVICE, mRil, responseInfo, isRegistered ? 1 : 0,
                ratFamily == android.hardware.radio.RadioTechnologyFamily.THREE_GPP
                        ? PhoneConstants.PHONE_TYPE_GSM : PhoneConstants.PHONE_TYPE_CDMA);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param selection false for automatic selection, true for manual selection
     */
    public void getNetworkSelectionModeResponse(RadioResponseInfo responseInfo, boolean selection) {
        RadioResponse.responseInts(RIL.NETWORK_SERVICE, mRil, responseInfo, selection ? 1 : 0);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param longName is long alpha ONS or EONS or empty string if unregistered
     * @param shortName is short alpha ONS or EONS or empty string if unregistered
     * @param numeric is 5 or 6 digit numeric code (MCC + MNC) or empty string if unregistered
     */
    public void getOperatorResponse(RadioResponseInfo responseInfo, String longName,
            String shortName, String numeric) {
        RadioResponse.responseStrings(
                RIL.NETWORK_SERVICE, mRil, responseInfo, longName, shortName, numeric);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param signalStrength Current signal strength of camped/connected cells
     */
    public void getSignalStrengthResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.network.SignalStrength signalStrength) {
        RILRequest rr = mRil.processResponse(RIL.NETWORK_SERVICE, responseInfo);

        if (rr != null) {
            SignalStrength ret = RILUtils.convertHalSignalStrength(signalStrength);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error.
     * @param halSpecifiers List of RadioAccessSpecifiers that are scanned.
     */
    public void getSystemSelectionChannelsResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.network.RadioAccessSpecifier[] halSpecifiers) {
        RILRequest rr = mRil.processResponse(RIL.NETWORK_SERVICE, responseInfo);

        if (rr != null) {
            ArrayList<RadioAccessSpecifier> specifiers = new ArrayList<>();
            for (android.hardware.radio.network.RadioAccessSpecifier specifier : halSpecifiers) {
                specifiers.add(RILUtils.convertHalRadioAccessSpecifier(specifier));
            }
            mRil.riljLog("getSystemSelectionChannelsResponse: from AIDL: " + specifiers);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, specifiers);
            }
            mRil.processResponseDone(rr, responseInfo, specifiers);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param rat Current voice RAT
     */
    public void getVoiceRadioTechnologyResponse(RadioResponseInfo responseInfo, int rat) {
        RadioResponse.responseInts(RIL.NETWORK_SERVICE, mRil, responseInfo, rat);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param voiceRegResponse Current Voice registration response as defined by VoiceRegStateResult
     */
    public void getVoiceRegistrationStateResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.network.RegStateResult voiceRegResponse) {
        RILRequest rr = mRil.processResponse(RIL.NETWORK_SERVICE, responseInfo);
        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, voiceRegResponse);
            }
            mRil.processResponseDone(rr, responseInfo, voiceRegResponse);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param isEnabled Indicates whether NR dual connectivity is enabled or not, True if enabled
     *        else false.
     */
    public void isNrDualConnectivityEnabledResponse(RadioResponseInfo responseInfo,
            boolean isEnabled) {
        RILRequest rr = mRil.processResponse(RIL.NETWORK_SERVICE, responseInfo);

        if (rr != null) {
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, isEnabled);
            }
            mRil.processResponseDone(rr, responseInfo, isEnabled);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param lceInfo LceDataInfo indicating LCE data
     */
    public void pullLceDataResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.network.LceDataInfo lceInfo) {
        RILRequest rr = mRil.processResponse(RIL.NETWORK_SERVICE, responseInfo);

        if (rr != null) {
            List<LinkCapacityEstimate> ret = RILUtils.convertHalLceData(lceInfo);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setAllowedNetworkTypesBitmapResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setBandModeResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setBarringPasswordResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaRoamingPreferenceResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCellInfoListRateResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setIndicationFilterResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setLinkCapacityReportingCriteriaResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setLocationUpdatesResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNetworkSelectionModeManualResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setNrDualConnectivityStateResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSignalStrengthReportingCriteriaResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSuppServiceNotificationsResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial number and error.
     */
    public void setSystemSelectionChannelsResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void startNetworkScanResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mRil.processResponse(RIL.NETWORK_SERVICE, responseInfo);
        if (rr != null) {
            NetworkScanResult nsr = null;
            if (responseInfo.error == RadioError.NONE) {
                nsr = new NetworkScanResult(NetworkScanResult.SCAN_STATUS_PARTIAL,
                        RadioError.NONE, null);
                RadioResponse.sendMessageResponse(rr.mResult, nsr);
            }
            mRil.processResponseDone(rr, responseInfo, nsr);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void stopNetworkScanResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = mRil.processResponse(RIL.NETWORK_SERVICE, responseInfo);
        if (rr != null) {
            NetworkScanResult nsr = null;
            if (responseInfo.error == RadioError.NONE) {
                nsr = new NetworkScanResult(NetworkScanResult.SCAN_STATUS_PARTIAL,
                        RadioError.NONE, null);
                RadioResponse.sendMessageResponse(rr.mResult, nsr);
            }
            mRil.processResponseDone(rr, responseInfo, nsr);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param retriesRemaining Number of retries remaining, must be equal to -1 if unknown.
     */
    public void supplyNetworkDepersonalizationResponse(RadioResponseInfo responseInfo,
            int retriesRemaining) {
        RadioResponse.responseInts(RIL.NETWORK_SERVICE, mRil, responseInfo, retriesRemaining);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setUsageSettingResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.NETWORK_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param usageSetting the cellular usage setting
     */
    public void getUsageSettingResponse(RadioResponseInfo responseInfo,
            /* @TelephonyManager.UsageSetting */ int usageSetting) {
        RadioResponse.responseInts(RIL.NETWORK_SERVICE, mRil, responseInfo, usageSetting);
    }

    @Override
    public String getInterfaceHash() {
        return IRadioNetworkResponse.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return IRadioNetworkResponse.VERSION;
    }
}

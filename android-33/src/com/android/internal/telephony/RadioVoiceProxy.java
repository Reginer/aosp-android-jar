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

import android.os.RemoteException;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.emergency.EmergencyNumber;

import java.util.ArrayList;

/**
 * A holder for IRadioVoice. Use getHidl to get IRadio 1.0 and call the HIDL implementations or
 * getAidl to get IRadioVoice and call the AIDL implementations of the HAL APIs.
 */
public class RadioVoiceProxy extends RadioServiceProxy {
    private static final String TAG = "RadioVoiceProxy";
    private volatile android.hardware.radio.voice.IRadioVoice mVoiceProxy = null;

    /**
     * Set IRadioVoice as the AIDL implementation for RadioServiceProxy
     * @param halVersion Radio HAL version
     * @param voice IRadioVoice implementation
     */
    public void setAidl(HalVersion halVersion, android.hardware.radio.voice.IRadioVoice voice) {
        mHalVersion = halVersion;
        mVoiceProxy = voice;
        mIsAidl = true;
        Rlog.d(TAG, "AIDL initialized");
    }

    /**
     * Get the AIDL implementation of RadioVoiceProxy
     * @return IRadioVoice implementation
     */
    public android.hardware.radio.voice.IRadioVoice getAidl() {
        return mVoiceProxy;
    }

    /**
     * Reset RadioVoiceProxy
     */
    @Override
    public void clear() {
        super.clear();
        mVoiceProxy = null;
    }

    /**
     * Check whether a RadioVoice implementation exists
     * @return true if there is neither a HIDL nor AIDL implementation
     */
    @Override
    public boolean isEmpty() {
        return mRadioProxy == null && mVoiceProxy == null;
    }

    /**
     * Call IRadioVoice#acceptCall
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void acceptCall(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.acceptCall(serial);
        } else {
            mRadioProxy.acceptCall(serial);
        }
    }

    /**
     * Call IRadioVoice#cancelPendingUssd
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void cancelPendingUssd(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.cancelPendingUssd(serial);
        } else {
            mRadioProxy.cancelPendingUssd(serial);
        }
    }

    /**
     * Call IRadioVoice#conference
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void conference(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.conference(serial);
        } else {
            mRadioProxy.conference(serial);
        }
    }

    /**
     * Call IRadioVoice#dial
     * @param serial Serial number of request
     * @param address Address
     * @param clirMode CLIR mode
     * @param uusInfo UUS info
     * @throws RemoteException
     */
    public void dial(int serial, String address, int clirMode, UUSInfo uusInfo)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.dial(serial, RILUtils.convertToHalDialAidl(address, clirMode, uusInfo));
        } else {
            mRadioProxy.dial(serial, RILUtils.convertToHalDial(address, clirMode, uusInfo));
        }
    }

    /**
     * Call IRadioVoice#emergencyDial
     * @param serial Serial number of request
     * @param address Address
     * @param emergencyNumberInfo Emergency number information
     * @param hasKnownUserIntentEmergency Whether or not the request has known user intent emergency
     * @param clirMode CLIR mode
     * @param uusInfo UUS info
     * @throws RemoteException
     */
    public void emergencyDial(int serial, String address, EmergencyNumber emergencyNumberInfo,
            boolean hasKnownUserIntentEmergency, int clirMode, UUSInfo uusInfo)
            throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_4)) return;
        if (isAidl()) {
            mVoiceProxy.emergencyDial(serial,
                    RILUtils.convertToHalDialAidl(address, clirMode, uusInfo),
                    emergencyNumberInfo.getEmergencyServiceCategoryBitmaskInternalDial(),
                    emergencyNumberInfo.getEmergencyUrns() != null
                            ? emergencyNumberInfo.getEmergencyUrns().stream().toArray(String[]::new)
                            : new String[0],
                    emergencyNumberInfo.getEmergencyCallRouting(),
                    hasKnownUserIntentEmergency,
                    emergencyNumberInfo.getEmergencyNumberSourceBitmask()
                            == EmergencyNumber.EMERGENCY_NUMBER_SOURCE_TEST);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).emergencyDial_1_6(serial,
                    RILUtils.convertToHalDial(address, clirMode, uusInfo),
                    emergencyNumberInfo.getEmergencyServiceCategoryBitmaskInternalDial(),
                    emergencyNumberInfo.getEmergencyUrns() != null
                            ? new ArrayList(emergencyNumberInfo.getEmergencyUrns())
                            : new ArrayList<>(),
                    emergencyNumberInfo.getEmergencyCallRouting(),
                    hasKnownUserIntentEmergency,
                    emergencyNumberInfo.getEmergencyNumberSourceBitmask()
                            == EmergencyNumber.EMERGENCY_NUMBER_SOURCE_TEST);
        } else {
            ((android.hardware.radio.V1_4.IRadio) mRadioProxy).emergencyDial(serial,
                    RILUtils.convertToHalDial(address, clirMode, uusInfo),
                    emergencyNumberInfo.getEmergencyServiceCategoryBitmaskInternalDial(),
                    emergencyNumberInfo.getEmergencyUrns() != null
                            ? new ArrayList(emergencyNumberInfo.getEmergencyUrns())
                            : new ArrayList<>(),
                    emergencyNumberInfo.getEmergencyCallRouting(),
                    hasKnownUserIntentEmergency,
                    emergencyNumberInfo.getEmergencyNumberSourceBitmask()
                            == EmergencyNumber.EMERGENCY_NUMBER_SOURCE_TEST);
        }
    }

    /**
     * Call IRadioVoice#exitEmergencyCallbackMode
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void exitEmergencyCallbackMode(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.exitEmergencyCallbackMode(serial);
        } else {
            mRadioProxy.exitEmergencyCallbackMode(serial);
        }
    }

    /**
     * Call IRadioVoice#explicitCallTransfer
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void explicitCallTransfer(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.explicitCallTransfer(serial);
        } else {
            mRadioProxy.explicitCallTransfer(serial);
        }
    }

    /**
     * Call IRadioVoice#getCallForwardStatus
     * @param serial Serial number of request
     * @param cfReason One of CF_REASON_*
     * @param serviceClass Sum of SERVICE_CLASS_*
     * @param number Number
     * @throws RemoteException
     */
    public void getCallForwardStatus(int serial, int cfReason, int serviceClass, String number)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.voice.CallForwardInfo cfInfo =
                    new android.hardware.radio.voice.CallForwardInfo();
            cfInfo.reason = cfReason;
            cfInfo.serviceClass = serviceClass;
            cfInfo.toa = PhoneNumberUtils.toaFromString(number);
            cfInfo.number = RILUtils.convertNullToEmptyString(number);
            cfInfo.timeSeconds = 0;
            mVoiceProxy.getCallForwardStatus(serial, cfInfo);
        } else {
            android.hardware.radio.V1_0.CallForwardInfo cfInfo =
                    new android.hardware.radio.V1_0.CallForwardInfo();
            cfInfo.reason = cfReason;
            cfInfo.serviceClass = serviceClass;
            cfInfo.toa = PhoneNumberUtils.toaFromString(number);
            cfInfo.number = RILUtils.convertNullToEmptyString(number);
            cfInfo.timeSeconds = 0;
            mRadioProxy.getCallForwardStatus(serial, cfInfo);
        }
    }

    /**
     * Call IRadioVoice#getCallWaiting
     * @param serial Serial number of request
     * @param serviceClass Sum of SERVICE_CLASS_*
     * @throws RemoteException
     */
    public void getCallWaiting(int serial, int serviceClass) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.getCallWaiting(serial, serviceClass);
        } else {
            mRadioProxy.getCallWaiting(serial, serviceClass);
        }
    }

    /**
     * Call IRadioVoice#getClip
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getClip(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.getClip(serial);
        } else {
            mRadioProxy.getClip(serial);
        }
    }

    /**
     * Call IRadioVoice#getClir
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getClir(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.getClir(serial);
        } else {
            mRadioProxy.getClir(serial);
        }
    }

    /**
     * Call IRadioVoice#getCurrentCalls
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getCurrentCalls(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.getCurrentCalls(serial);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).getCurrentCalls_1_6(serial);
        } else {
            mRadioProxy.getCurrentCalls(serial);
        }
    }

    /**
     * Call IRadioVoice#getLastCallFailCause
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getLastCallFailCause(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.getLastCallFailCause(serial);
        } else {
            mRadioProxy.getLastCallFailCause(serial);
        }
    }

    /**
     * Call IRadioVoice#getMute
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getMute(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.getMute(serial);
        } else {
            mRadioProxy.getMute(serial);
        }
    }

    /**
     * Call IRadioVoice#getPreferredVoicePrivacy
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getPreferredVoicePrivacy(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.getPreferredVoicePrivacy(serial);
        } else {
            mRadioProxy.getPreferredVoicePrivacy(serial);
        }
    }

    /**
     * Call IRadioVoice#getTtyMode
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getTtyMode(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.getTtyMode(serial);
        } else {
            mRadioProxy.getTTYMode(serial);
        }
    }

    /**
     * Call IRadioVoice#handleStkCallSetupRequestFromSim
     * @param serial Serial number of request
     * @param accept Whether or not the call is to be accepted
     * @throws RemoteException
     */
    public void handleStkCallSetupRequestFromSim(int serial, boolean accept)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.handleStkCallSetupRequestFromSim(serial, accept);
        } else {
            mRadioProxy.handleStkCallSetupRequestFromSim(serial, accept);
        }
    }

    /**
     * Call IRadioVoice#hangup
     * @param serial Serial number of request
     * @param gsmIndex Connection index
     * @throws RemoteException
     */
    public void hangup(int serial, int gsmIndex) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.hangup(serial, gsmIndex);
        } else {
            mRadioProxy.hangup(serial, gsmIndex);
        }
    }

    /**
     * Call IRadioVoice#hangupForegroundResumeBackground
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void hangupForegroundResumeBackground(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.hangupForegroundResumeBackground(serial);
        } else {
            mRadioProxy.hangupForegroundResumeBackground(serial);
        }
    }

    /**
     * Call IRadioVoice#hangupWaitingOrBackground
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void hangupWaitingOrBackground(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.hangupWaitingOrBackground(serial);
        } else {
            mRadioProxy.hangupWaitingOrBackground(serial);
        }
    }

    /**
     * Call IRadioVoice#isVoNrEnabled
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void isVoNrEnabled(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.isVoNrEnabled(serial);
        }
    }

    /**
     * Call IRadioVoice#rejectCall
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void rejectCall(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.rejectCall(serial);
        } else {
            mRadioProxy.rejectCall(serial);
        }
    }

    /**
     * Call IRadioVoice#responseAcknowledgement
     * @throws RemoteException
     */
    @Override
    public void responseAcknowledgement() throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.responseAcknowledgement();
        } else {
            mRadioProxy.responseAcknowledgement();
        }
    }

    /**
     * Call IRadioVoice#sendBurstDtmf
     * @param serial Serial number of request
     * @param dtmf DTMF string
     * @param on DTMF ON length in milliseconds, or 0 to use default
     * @param off DTMF OFF length in milliseconds, or 0 to use default
     * @throws RemoteException
     */
    public void sendBurstDtmf(int serial, String dtmf, int on, int off) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.sendBurstDtmf(serial, dtmf, on, off);
        } else {
            mRadioProxy.sendBurstDtmf(serial, dtmf, on, off);
        }
    }

    /**
     * Call IRadioVoice#sendCdmaFeatureCode
     * @param serial Serial number of request
     * @param featureCode String associated with FLASH command
     * @throws RemoteException
     */
    public void sendCdmaFeatureCode(int serial, String featureCode) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.sendCdmaFeatureCode(serial, featureCode);
        } else {
            mRadioProxy.sendCDMAFeatureCode(serial, featureCode);
        }
    }

    /**
     * Call IRadioVoice#sendDtmf
     * @param serial Serial number of request
     * @param s String with single char having one of 12 values: 0-0, *, #
     * @throws RemoteException
     */
    public void sendDtmf(int serial, String s) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.sendDtmf(serial, s);
        } else {
            mRadioProxy.sendDtmf(serial, s);
        }
    }

    /**
     * Call IRadioVoice#sendUssd
     * @param serial Serial number of request
     * @param ussd String containing the USSD request in UTF-8 format
     * @throws RemoteException
     */
    public void sendUssd(int serial, String ussd) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.sendUssd(serial, ussd);
        } else {
            mRadioProxy.sendUssd(serial, ussd);
        }
    }

    /**
     * Call IRadioVoice#separateConnection
     * @param serial Serial number of request
     * @param gsmIndex Connection index
     * @throws RemoteException
     */
    public void separateConnection(int serial, int gsmIndex) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.separateConnection(serial, gsmIndex);
        } else {
            mRadioProxy.separateConnection(serial, gsmIndex);
        }
    }

    /**
     * Call IRadioVoice#setCallForward
     * @param serial Serial number of request
     * @param action One of CF_ACTION_*
     * @param cfReason One of CF_REASON_*
     * @param serviceClass Sum of SERVICE_CLASSS_*
     * @param number Number
     * @param timeSeconds Time in seconds
     * @throws RemoteException
     */
    public void setCallForward(int serial, int action, int cfReason, int serviceClass,
            String number, int timeSeconds) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.voice.CallForwardInfo cfInfo =
                    new android.hardware.radio.voice.CallForwardInfo();
            cfInfo.status = action;
            cfInfo.reason = cfReason;
            cfInfo.serviceClass = serviceClass;
            cfInfo.toa = PhoneNumberUtils.toaFromString(number);
            cfInfo.number = RILUtils.convertNullToEmptyString(number);
            cfInfo.timeSeconds = timeSeconds;
            mVoiceProxy.setCallForward(serial, cfInfo);
        } else {
            android.hardware.radio.V1_0.CallForwardInfo cfInfo =
                    new android.hardware.radio.V1_0.CallForwardInfo();
            cfInfo.status = action;
            cfInfo.reason = cfReason;
            cfInfo.serviceClass = serviceClass;
            cfInfo.toa = PhoneNumberUtils.toaFromString(number);
            cfInfo.number = RILUtils.convertNullToEmptyString(number);
            cfInfo.timeSeconds = timeSeconds;
            mRadioProxy.setCallForward(serial, cfInfo);
        }
    }

    /**
     * Call IRadioVoice#setCallWaiting
     * @param serial Serial number of request
     * @param enable True to enable, false to disable
     * @param serviceClass Sum of SERVICE_CLASS_*
     * @throws RemoteException
     */
    public void setCallWaiting(int serial, boolean enable, int serviceClass)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.setCallWaiting(serial, enable, serviceClass);
        } else {
            mRadioProxy.setCallWaiting(serial, enable, serviceClass);
        }
    }

    /**
     * Call IRadioVoice#setClir
     * @param serial Serial number of request
     * @param status One of CLIR_*
     * @throws RemoteException
     */
    public void setClir(int serial, int status) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.setClir(serial, status);
        } else {
            mRadioProxy.setClir(serial, status);
        }
    }

    /**
     * Call IRadioVoice#setMute
     * @param serial Serial number of request
     * @param enable True to enable, false to disable
     * @throws RemoteException
     */
    public void setMute(int serial, boolean enable) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.setMute(serial, enable);
        } else {
            mRadioProxy.setMute(serial, enable);
        }
    }

    /**
     * Call IRadioVoice#setPreferredVoicePrivacy
     * @param serial Serial number of request
     * @param enable True is enhanced, false is normal voice privacy
     * @throws RemoteException
     */
    public void setPreferredVoicePrivacy(int serial, boolean enable) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.setPreferredVoicePrivacy(serial, enable);
        } else {
            mRadioProxy.setPreferredVoicePrivacy(serial, enable);
        }
    }

    /**
     * Call IRadioVoice#setTtyMode
     * @param serial Serial number of request
     * @param mode One of TTY_MODE_*
     * @throws RemoteException
     */
    public void setTtyMode(int serial, int mode) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.setTtyMode(serial, mode);
        } else {
            mRadioProxy.setTTYMode(serial, mode);
        }
    }

    /**
     * Call IRadioVoice#setVoNrEnabled
     * @param serial Serial number of request
     * @param enable True to enable, false to disable
     * @throws RemoteException
     */
    public void setVoNrEnabled(int serial, boolean enable) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.setVoNrEnabled(serial, enable);
        }
    }

    /**
     * Call IRadioVoice#startDtmf
     * @param serial Serial number of request
     * @param s String having a single character with one of 12 values: 0-9, *, #
     * @throws RemoteException
     */
    public void startDtmf(int serial, String s) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.startDtmf(serial, s);
        } else {
            mRadioProxy.startDtmf(serial, s);
        }
    }

    /**
     * Call IRadioVoice#stopDtmf
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void stopDtmf(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.stopDtmf(serial);
        } else {
            mRadioProxy.stopDtmf(serial);
        }
    }

    /**
     * Call IRadioVoice#switchWaitingOrHoldingAndActive
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void switchWaitingOrHoldingAndActive(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mVoiceProxy.switchWaitingOrHoldingAndActive(serial);
        } else {
            mRadioProxy.switchWaitingOrHoldingAndActive(serial);
        }
    }
}

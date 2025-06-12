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

import android.os.RemoteException;
import android.telephony.Rlog;

/**
 * A holder for IRadioIms.
 * Use getAidl to get IRadioIms and call the AIDL implementations of the HAL APIs.
 */
public class RadioImsProxy extends RadioServiceProxy {
    private static final String TAG = "RadioImsProxy";
    private volatile android.hardware.radio.ims.IRadioIms mImsProxy = null;

    /**
     * Sets IRadioIms as the AIDL implementation for RadioServiceProxy.
     * @param halVersion Radio HAL version.
     * @param ims IRadioIms implementation.
     *
     * @return updated HAL version.
     */
    public HalVersion setAidl(HalVersion halVersion, android.hardware.radio.ims.IRadioIms ims) {
        HalVersion version = halVersion;
        try {
            version = RIL.getServiceHalVersion(ims.getInterfaceVersion());
        } catch (RemoteException e) {
            Rlog.e(TAG, "setAidl: " + e);
        }
        mHalVersion = version;
        mImsProxy = ims;
        mIsAidl = true;

        Rlog.d(TAG, "AIDL initialized mHalVersion=" + mHalVersion);
        return mHalVersion;
    }

    /**
     * Gets the AIDL implementation of RadioImsProxy.
     * @return IRadioIms implementation.
     */
    public android.hardware.radio.ims.IRadioIms getAidl() {
        return mImsProxy;
    }

    /**
     * Resets RadioImsProxy.
     */
    @Override
    public void clear() {
        super.clear();
        mImsProxy = null;
    }

    /**
     * Checks whether a RadioIms implementation exists.
     * @return true if there is neither a HIDL nor AIDL implementation.
     */
    @Override
    public boolean isEmpty() {
        return mRadioProxy == null && mImsProxy == null;
    }

    /**
     * No implementation in IRadioIms.
     * @throws RemoteException.
     */
    @Override
    public void responseAcknowledgement() throws RemoteException {
        /* Currently, IRadioIms doesn't support the following response types:
         * - RadioIndicationType.UNSOLICITED_ACK_EXP
         * - RadioResponseType.SOLICITED_ACK_EXP */
        // no-op
    }

    /**
     * Calls IRadioIms#setSrvccCallInfo.
     * @param serial Serial number of request.
     * @param srvccCalls The list of call information.
     * @throws RemoteException.
     */
    public void setSrvccCallInfo(int serial,
            android.hardware.radio.ims.SrvccCall[] srvccCalls) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mImsProxy.setSrvccCallInfo(serial, srvccCalls);
        }
    }

    /**
     * Calls IRadioIms#updateImsRegistrationInfo.
     * @param serial Serial number of request.
     * @param registrationInfo The registration state information.
     * @throws RemoteException.
     */
    public void updateImsRegistrationInfo(int serial,
            android.hardware.radio.ims.ImsRegistration registrationInfo) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mImsProxy.updateImsRegistrationInfo(serial, registrationInfo);
        }
    }

    /**
     * Calls IRadioIms#startImsTraffic.
     * @param serial Serial number of request.
     * @param token A nonce to identify the request.
     * @param trafficType IMS traffic type like registration, voice, video, SMS, emergency, and etc.
     * @param accessNetworkType The type of underlying radio access network used.
     * @param trafficDirection Indicates whether traffic is originated by mobile originated or
     *        mobile terminated use case eg. MO/MT call/SMS etc.
     * @throws RemoteException.
     */
    public void startImsTraffic(int serial, int token, int trafficType, int accessNetworkType,
            int trafficDirection) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mImsProxy.startImsTraffic(serial,
                    token, trafficType, accessNetworkType, trafficDirection);
        }
    }

    /**
     * Calls IRadioIms#stopImsTraffic.
     * @param serial Serial number of request.
     * @param token The token assigned by startImsTraffic.
     * @throws RemoteException.
     */
    public void stopImsTraffic(int serial, int token)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mImsProxy.stopImsTraffic(serial, token);
        }
    }

    /**
     * Calls IRadioIms#triggerEpsFallback.
     * @param serial Serial number of request.
     * @param reason Specifies the reason for EPS fallback.
     * @throws RemoteException.
     */
    public void triggerEpsFallback(int serial, int reason)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mImsProxy.triggerEpsFallback(serial, reason);
        }
    }

    /**
     * Calls IRadioIms#sendAnbrQuery.
     * @param serial Serial number of request.
     * @param mediaType Media type is used to identify media stream such as audio or video.
     * @param direction Direction of this packet stream (e.g. uplink or downlink).
     * @param bitsPerSecond The bit rate requested by the opponent UE.
     * @throws RemoteException.
     */
    public void sendAnbrQuery(int serial, int mediaType, int direction, int bitsPerSecond)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mImsProxy.sendAnbrQuery(serial, mediaType, direction, bitsPerSecond);
        }
    }

    /**
     * Call IRadioIms#updateImsCallStatus
     * @param serial Serial number of request.
     * @param imsCalls The list of call status information.
     * @throws RemoteException.
     */
    public void updateImsCallStatus(int serial,
            android.hardware.radio.ims.ImsCall[] imsCalls) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mImsProxy.updateImsCallStatus(serial, imsCalls);
        }
    }
}

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
import android.telephony.Rlog;

/**
 * A holder for IRadioModem. Use getHidl to get IRadio 1.0 and call the HIDL implementations or
 * getAidl to get IRadioModem and call the AIDL implementations of the HAL APIs.
 */
public class RadioModemProxy extends RadioServiceProxy {
    private static final String TAG = "RadioModemProxy";
    private volatile android.hardware.radio.modem.IRadioModem mModemProxy = null;

    /**
     * Set IRadioModem as the AIDL implementation for RadioServiceProxy
     * @param halVersion Radio HAL version
     * @param modem IRadioModem implementation
     */
    public void setAidl(HalVersion halVersion,
            android.hardware.radio.modem.IRadioModem modem) {
        mHalVersion = halVersion;
        mModemProxy = modem;
        mIsAidl = true;
        Rlog.d(TAG, "AIDL initialized");
    }

    /**
     * Get the AIDL implementation of RadioModemProxy
     * @return IRadioModem implementation
     */
    public android.hardware.radio.modem.IRadioModem getAidl() {
        return mModemProxy;
    }

    /**
     * Reset RadioModemProxy
     */
    @Override
    public void clear() {
        super.clear();
        mModemProxy = null;
    }

    /**
     * Check whether a RadioModem implementation exists
     * @return true if there is neither a HIDL nor AIDL implementation
     */
    @Override
    public boolean isEmpty() {
        return mRadioProxy == null && mModemProxy == null;
    }

    /**
     * Call IRadioModem#enableModem
     * @param serial Serial number of request
     * @param on Whether to enable or disable the modem
     * @throws RemoteException
     */
    public void enableModem(int serial, boolean on) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_3)) return;
        if (isAidl()) {
            mModemProxy.enableModem(serial, on);
        } else {
            ((android.hardware.radio.V1_3.IRadio) mRadioProxy).enableModem(serial, on);
        }
    }

    /**
     * Call IRadioModem#getBasebandVersion
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getBasebandVersion(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.getBasebandVersion(serial);
        } else {
            mRadioProxy.getBasebandVersion(serial);
        }
    }

    /**
     * Call IRadioModem#getDeviceIdentity
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getDeviceIdentity(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.getDeviceIdentity(serial);
        } else {
            mRadioProxy.getDeviceIdentity(serial);
        }
    }

    /**
     * Call IRadioModem#getHardwareConfig
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getHardwareConfig(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.getHardwareConfig(serial);
        } else {
            mRadioProxy.getHardwareConfig(serial);
        }
    }

    /**
     * Call IRadioModem#getModemActivityInfo
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getModemActivityInfo(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.getModemActivityInfo(serial);
        } else {
            mRadioProxy.getModemActivityInfo(serial);
        }
    }

    /**
     * Call IRadioModem#getModemStackStatus
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getModemStackStatus(int serial) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_3)) return;
        if (isAidl()) {
            mModemProxy.getModemStackStatus(serial);
        } else {
            ((android.hardware.radio.V1_3.IRadio) mRadioProxy).getModemStackStatus(serial);
        }
    }

    /**
     * Call IRadioModem#getRadioCapability
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getRadioCapability(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.getRadioCapability(serial);
        } else {
            mRadioProxy.getRadioCapability(serial);
        }
    }

    /**
     * Call IRadioModem#nvReadItem
     * @param serial Serial number of request
     * @param itemId ID of the item to read
     * @throws RemoteException
     */
    public void nvReadItem(int serial, int itemId) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.nvReadItem(serial, itemId);
        } else {
            mRadioProxy.nvReadItem(serial, itemId);
        }
    }

    /**
     * Call IRadioModem#nvResetConfig
     * @param serial Serial number of request
     * @param resetType Reset type; 1: reload NV reset, 2: erase NV reset, 3: factory NV reset
     * @throws RemoteException
     */
    public void nvResetConfig(int serial, int resetType) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.nvResetConfig(serial, RILUtils.convertToHalResetNvTypeAidl(resetType));
        } else {
            mRadioProxy.nvResetConfig(serial, RILUtils.convertToHalResetNvType(resetType));
        }
    }

    /**
     * Call IRadioModem#nvWriteCdmaPrl
     * @param serial Serial number of request
     * @param prl Preferred roaming list as a byte array
     * @throws RemoteException
     */
    public void nvWriteCdmaPrl(int serial, byte[] prl) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.nvWriteCdmaPrl(serial, prl);
        } else {
            mRadioProxy.nvWriteCdmaPrl(serial, RILUtils.primitiveArrayToArrayList(prl));
        }
    }

    /**
     * Call IRadioModem#nvWriteItem
     * @param serial Serial number of request
     * @param itemId ID of the item to write
     * @param itemValue Value to write as a String
     * @throws RemoteException
     */
    public void nvWriteItem(int serial, int itemId, String itemValue) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.modem.NvWriteItem item =
                    new android.hardware.radio.modem.NvWriteItem();
            item.itemId = itemId;
            item.value = itemValue;
            mModemProxy.nvWriteItem(serial, item);
        } else {
            android.hardware.radio.V1_0.NvWriteItem item =
                    new android.hardware.radio.V1_0.NvWriteItem();
            item.itemId = itemId;
            item.value = itemValue;
            mRadioProxy.nvWriteItem(serial, item);
        }
    }

    /**
     * Call IRadioModem#requestShutdown
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void requestShutdown(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.requestShutdown(serial);
        } else {
            mRadioProxy.requestShutdown(serial);
        }
    }

    /**
     * Call IRadioModem#responseAcknowledgement
     * @throws RemoteException
     */
    @Override
    public void responseAcknowledgement() throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.responseAcknowledgement();
        } else {
            mRadioProxy.responseAcknowledgement();
        }
    }

    /**
     * Call IRadioModem#sendDeviceState
     * @param serial Serial number of request
     * @param deviceStateType Device state type
     * @param state True if enabled and false if disabled
     * @throws RemoteException
     */
    public void sendDeviceState(int serial, int deviceStateType, boolean state)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.sendDeviceState(serial, deviceStateType, state);
        } else {
            mRadioProxy.sendDeviceState(serial, deviceStateType, state);
        }
    }

    /**
     * Call IRadioModem#setRadioCapability
     * @param serial Serial number of request
     * @param rc The phone radio capability defined in RadioCapability
     *           It's a input object used to transfer parameter to logic modem
     * @throws RemoteException
     */
    public void setRadioCapability(int serial, RadioCapability rc) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.modem.RadioCapability halRc =
                    new android.hardware.radio.modem.RadioCapability();
            halRc.session = rc.getSession();
            halRc.phase = rc.getPhase();
            halRc.raf = rc.getRadioAccessFamily();
            halRc.logicalModemUuid = RILUtils.convertNullToEmptyString(rc.getLogicalModemUuid());
            halRc.status = rc.getStatus();
            mModemProxy.setRadioCapability(serial, halRc);
        } else {
            android.hardware.radio.V1_0.RadioCapability halRc =
                    new android.hardware.radio.V1_0.RadioCapability();
            halRc.session = rc.getSession();
            halRc.phase = rc.getPhase();
            halRc.raf = rc.getRadioAccessFamily();
            halRc.logicalModemUuid = RILUtils.convertNullToEmptyString(rc.getLogicalModemUuid());
            halRc.status = rc.getStatus();
            mRadioProxy.setRadioCapability(serial, halRc);
        }
    }

    /**
     * Call IRadioModem#setRadioPower
     * @param serial Serial number of request
     * @param powerOn True to turn on radio and false to turn off
     * @param forEmergencyCall Indicates that this request is due to emergency call
     *                         No effect if powerOn is false
     * @param preferredForEmergencyCall Whether or not the following emergency call will be sent
     *                                  on this modem
     *                                  No effect if powerOn or forEmergencyCall is false
     * @throws RemoteException
     */
    public void setRadioPower(int serial, boolean powerOn, boolean forEmergencyCall,
            boolean preferredForEmergencyCall) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mModemProxy.setRadioPower(serial, powerOn, forEmergencyCall, preferredForEmergencyCall);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).setRadioPower_1_6(serial, powerOn,
                    forEmergencyCall, preferredForEmergencyCall);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).setRadioPower_1_5(serial, powerOn,
                    forEmergencyCall, preferredForEmergencyCall);
        } else {
            mRadioProxy.setRadioPower(serial, powerOn);
        }
    }
}

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

import static com.android.internal.telephony.RILConstants.REQUEST_NOT_SUPPORTED;

import android.os.AsyncResult;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.CarrierRestrictionRules;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.uicc.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.uicc.SimPhonebookRecord;

/**
 * A holder for IRadioSim. Use getHidl to get IRadio 1.0 and call the HIDL implementations or
 * getAidl to get IRadioSim and call the AIDL implementations of the HAL APIs.
 */
public class RadioSimProxy extends RadioServiceProxy {
    private static final String TAG = "RadioSimProxy";
    private volatile android.hardware.radio.sim.IRadioSim mSimProxy = null;

    /**
     * Set IRadioSim as the AIDL implementation for RadioServiceProxy
     * @param halVersion Radio HAL version
     * @param sim IRadioSim implementation
     */
    public void setAidl(HalVersion halVersion, android.hardware.radio.sim.IRadioSim sim) {
        mHalVersion = halVersion;
        mSimProxy = sim;
        mIsAidl = true;
        Rlog.d(TAG, "AIDL initialized");
    }

    /**
     * Get the AIDL implementation of RadioSimProxy
     * @return IRadioSim implementation
     */
    public android.hardware.radio.sim.IRadioSim getAidl() {
        return mSimProxy;
    }

    /**
     * Reset RadioSimProxy
     */
    @Override
    public void clear() {
        super.clear();
        mSimProxy = null;
    }

    /**
     * Check whether a RadioSim implementation exists
     * @return true if there is neither a HIDL nor AIDL implementation
     */
    @Override
    public boolean isEmpty() {
        return mRadioProxy == null && mSimProxy == null;
    }

    /**
     * Call IRadioSim#areUiccApplicationsEnabled
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void areUiccApplicationsEnabled(int serial) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_5)) return;
        if (isAidl()) {
            mSimProxy.areUiccApplicationsEnabled(serial);
        } else {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).areUiccApplicationsEnabled(serial);
        }
    }

    /**
     * Call IRadioSim#changeIccPin2ForApp
     * @param serial Serial number of request
     * @param oldPin2 Old PIN value
     * @param newPin2 New PIN value
     * @param aid Application ID
     * @throws RemoteException
     */
    public void changeIccPin2ForApp(int serial, String oldPin2, String newPin2, String aid)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.changeIccPin2ForApp(serial, oldPin2, newPin2, aid);
        } else {
            mRadioProxy.changeIccPin2ForApp(serial, oldPin2, newPin2, aid);
        }
    }

    /**
     * Call IRadioSim#changeIccPinForApp
     * @param serial Serial number of request
     * @param oldPin Old PIN value
     * @param newPin New PIN value
     * @param aid Application ID
     * @throws RemoteException
     */
    public void changeIccPinForApp(int serial, String oldPin, String newPin, String aid)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.changeIccPinForApp(serial, oldPin, newPin, aid);
        } else {
            mRadioProxy.changeIccPinForApp(serial, oldPin, newPin, aid);
        }
    }

    /**
     * Call IRadioSim#enableUiccApplications
     * @param serial Serial number of request
     * @param enable Whether or not to enable UiccApplications on the SIM
     * @throws RemoteException
     */
    public void enableUiccApplications(int serial, boolean enable) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_5)) return;
        if (isAidl()) {
            mSimProxy.enableUiccApplications(serial, enable);
        } else {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).enableUiccApplications(
                    serial, enable);
        }
    }

    /**
     * Call IRadioSim#getAllowedCarriers
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getAllowedCarriers(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.getAllowedCarriers(serial);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_4)) {
            ((android.hardware.radio.V1_4.IRadio) mRadioProxy).getAllowedCarriers_1_4(serial);
        } else {
            mRadioProxy.getAllowedCarriers(serial);
        }
    }

    /**
     * Call IRadioSim#getCdmaSubscription
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getCdmaSubscription(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.getCdmaSubscription(serial);
        } else {
            mRadioProxy.getCDMASubscription(serial);
        }
    }

    /**
     * Call IRadioSim#getCdmaSubscriptionSource
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getCdmaSubscriptionSource(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.getCdmaSubscriptionSource(serial);
        } else {
            mRadioProxy.getCdmaSubscriptionSource(serial);
        }
    }

    /**
     * Call IRadioSim#getFacilityLockForApp
     * @param serial Serial number of request
     * @param facility One of CB_FACILTY_*
     * @param password Password or "" if not required
     * @param serviceClass Sum of SERVICE_CLASS_*
     * @param appId Application ID or null if none
     * @throws RemoteException
     */
    public void getFacilityLockForApp(int serial, String facility, String password,
            int serviceClass, String appId) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.getFacilityLockForApp(serial, facility, password, serviceClass, appId);
        } else {
            mRadioProxy.getFacilityLockForApp(serial, facility, password, serviceClass, appId);
        }
    }

    /**
     * Call IRadioSim#getIccCardStatus
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getIccCardStatus(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.getIccCardStatus(serial);
        } else {
            mRadioProxy.getIccCardStatus(serial);
        }
    }

    /**
     * Call IRadioSim#getImsiForApp
     * @param serial Serial number of request
     * @param aid Application ID
     * @throws RemoteException
     */
    public void getImsiForApp(int serial, String aid) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.getImsiForApp(serial, aid);
        } else {
            mRadioProxy.getImsiForApp(serial, aid);
        }
    }

    /**
     * Call IRadioSim#getSimPhonebookCapacity
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getSimPhonebookCapacity(int serial) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mSimProxy.getSimPhonebookCapacity(serial);
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).getSimPhonebookCapacity(serial);
        }
    }

    /**
     * Call IRadioSim#getSimPhonebookRecords
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getSimPhonebookRecords(int serial) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mSimProxy.getSimPhonebookRecords(serial);
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).getSimPhonebookRecords(serial);
        }
    }

    /**
     * Call IRadioSim#iccCloseLogicalChannel
     * @param serial Serial number of request
     * @param channelId Channel ID of the channel to be closed
     * @throws RemoteException
     */
    public void iccCloseLogicalChannel(int serial, int channelId) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.iccCloseLogicalChannel(serial, channelId);
        } else {
            mRadioProxy.iccCloseLogicalChannel(serial, channelId);
        }
    }

    /**
     * Call IRadioSim#iccIoForApp
     * @param serial Serial number of request
     * @param command Command
     * @param fileId File ID
     * @param path Path
     * @param p1 P1 value of the command
     * @param p2 P2 value of the command
     * @param p3 P3 value of the command
     * @param data Data to be sent
     * @param pin2 PIN 2 value
     * @param aid Application ID
     * @throws RemoteException
     */
    public void iccIoForApp(int serial, int command, int fileId, String path, int p1, int p2,
            int p3, String data, String pin2, String aid) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.sim.IccIo iccIo = new android.hardware.radio.sim.IccIo();
            iccIo.command = command;
            iccIo.fileId = fileId;
            iccIo.path = path;
            iccIo.p1 = p1;
            iccIo.p2 = p2;
            iccIo.p3 = p3;
            iccIo.data = data;
            iccIo.pin2 = pin2;
            iccIo.aid = aid;
            mSimProxy.iccIoForApp(serial, iccIo);
        } else {
            android.hardware.radio.V1_0.IccIo iccIo = new android.hardware.radio.V1_0.IccIo();
            iccIo.command = command;
            iccIo.fileId = fileId;
            iccIo.path = path;
            iccIo.p1 = p1;
            iccIo.p2 = p2;
            iccIo.p3 = p3;
            iccIo.data = data;
            iccIo.pin2 = pin2;
            iccIo.aid = aid;
            mRadioProxy.iccIOForApp(serial, iccIo);
        }
    }

    /**
     * Call IRadioSim#iccOpenLogicalChannel
     * @param serial Serial number of request
     * @param aid Application ID
     * @param p2 P2 value of the command
     * @throws RemoteException
     */
    public void iccOpenLogicalChannel(int serial, String aid, int p2) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.iccOpenLogicalChannel(serial, aid, p2);
        } else {
            mRadioProxy.iccOpenLogicalChannel(serial, aid, p2);
        }
    }

    /**
     * Call IRadioSim#iccTransmitApduBasicChannel
     * @param serial Serial number of request
     * @param cla Class of the command
     * @param instruction Instruction of the command
     * @param p1 P1 value of the command
     * @param p2 P2 value of the command
     * @param p3 P3 value of the command
     * @param data Data to be sent
     * @throws RemoteException
     */
    public void iccTransmitApduBasicChannel(int serial, int cla, int instruction, int p1, int p2,
            int p3, String data) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.iccTransmitApduBasicChannel(serial,
                    RILUtils.convertToHalSimApduAidl(0, cla, instruction, p1, p2, p3, data));
        } else {
            mRadioProxy.iccTransmitApduBasicChannel(serial,
                    RILUtils.convertToHalSimApdu(0, cla, instruction, p1, p2, p3, data));
        }
    }

    /**
     * Call IRadioSim#iccTransmitApduLogicalChannel
     * @param serial Serial number of request
     * @param channel Channel ID of the channel to use for communication
     * @param cla Class of the command
     * @param instruction Instruction of the command
     * @param p1 P1 value of the command
     * @param p2 P2 value of the command
     * @param p3 P3 value of the command
     * @param data Data to be sent
     * @throws RemoteException
     */
    public void iccTransmitApduLogicalChannel(int serial, int channel, int cla, int instruction,
            int p1, int p2, int p3, String data) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.iccTransmitApduLogicalChannel(serial,
                    RILUtils.convertToHalSimApduAidl(channel, cla, instruction, p1, p2, p3, data));
        } else {
            mRadioProxy.iccTransmitApduLogicalChannel(serial,
                    RILUtils.convertToHalSimApdu(channel, cla, instruction, p1, p2, p3, data));
        }
    }

    /**
     * Call IRadioSim#reportStkServiceIsRunning
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void reportStkServiceIsRunning(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.reportStkServiceIsRunning(serial);
        } else {
            mRadioProxy.reportStkServiceIsRunning(serial);
        }
    }

    /**
     * Call IRadioSim#requestIccSimAuthentication
     * @param serial Serial number of request
     * @param authContext P2 parameter that specifies the authentication context
     * @param authData Authentication challenge data
     * @param aid Application ID of the application/slot to send the auth command to
     * @throws RemoteException
     */
    public void requestIccSimAuthentication(int serial, int authContext, String authData,
            String aid) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.requestIccSimAuthentication(serial, authContext, authData, aid);
        } else {
            mRadioProxy.requestIccSimAuthentication(serial, authContext, authData, aid);
        }
    }

    /**
     * Call IRadioSim#responseAcknowledgement
     * @throws RemoteException
     */
    @Override
    public void responseAcknowledgement() throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.responseAcknowledgement();
        } else {
            mRadioProxy.responseAcknowledgement();
        }
    }

    /**
     * Call IRadioSim#sendEnvelope
     * @param serial Serial number of request
     * @param contents String containing SAT/USAT response in hexadecimal format starting with
     *                 command tag
     * @throws RemoteException
     */
    public void sendEnvelope(int serial, String contents) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.sendEnvelope(serial, contents);
        } else {
            mRadioProxy.sendEnvelope(serial, contents);
        }
    }

    /**
     * Call IRadioSim#sendEnvelopeWithStatus
     * @param serial Serial number of request
     * @param contents String containing SAT/USAT response in hexadecimal format starting with
     *                 command tag
     * @throws RemoteException
     */
    public void sendEnvelopeWithStatus(int serial, String contents) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.sendEnvelopeWithStatus(serial, contents);
        } else {
            mRadioProxy.sendEnvelopeWithStatus(serial, contents);
        }
    }

    /**
     * Call IRadioSim#sendTerminalResponseToSim
     * @param serial Serial number of request
     * @param contents String containing SAT/USAT response in hexadecimal format starting with
     *                 first byte of response data
     * @throws RemoteException
     */
    public void sendTerminalResponseToSim(int serial, String contents) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.sendTerminalResponseToSim(serial, contents);
        } else {
            mRadioProxy.sendTerminalResponseToSim(serial, contents);
        }
    }

    /**
     * Call IRadioSim#setAllowedCarriers
     * @param serial Serial number of request
     * @param carrierRestrictionRules Allowed carriers
     * @param result Result to return in case of error
     * @throws RemoteException
     */
    public void setAllowedCarriers(int serial, CarrierRestrictionRules carrierRestrictionRules,
            Message result) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            // Prepare structure with allowed list, excluded list and priority
            android.hardware.radio.sim.CarrierRestrictions carrierRestrictions =
                    new android.hardware.radio.sim.CarrierRestrictions();
            carrierRestrictions.allowedCarriers = RILUtils.convertToHalCarrierRestrictionListAidl(
                    carrierRestrictionRules.getAllowedCarriers());
            carrierRestrictions.excludedCarriers = RILUtils.convertToHalCarrierRestrictionListAidl(
                    carrierRestrictionRules.getExcludedCarriers());
            carrierRestrictions.allowedCarriersPrioritized =
                    (carrierRestrictionRules.getDefaultCarrierRestriction()
                            == CarrierRestrictionRules.CARRIER_RESTRICTION_DEFAULT_NOT_ALLOWED);
            mSimProxy.setAllowedCarriers(serial, carrierRestrictions,
                    RILUtils.convertToHalSimLockMultiSimPolicyAidl(
                            carrierRestrictionRules.getMultiSimPolicy()));
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_4)) {
            // Prepare structure with allowed list, excluded list and priority
            android.hardware.radio.V1_4.CarrierRestrictionsWithPriority carrierRestrictions =
                    new android.hardware.radio.V1_4.CarrierRestrictionsWithPriority();
            carrierRestrictions.allowedCarriers = RILUtils.convertToHalCarrierRestrictionList(
                    carrierRestrictionRules.getAllowedCarriers());
            carrierRestrictions.excludedCarriers = RILUtils.convertToHalCarrierRestrictionList(
                    carrierRestrictionRules.getExcludedCarriers());
            carrierRestrictions.allowedCarriersPrioritized =
                    (carrierRestrictionRules.getDefaultCarrierRestriction()
                            == CarrierRestrictionRules.CARRIER_RESTRICTION_DEFAULT_NOT_ALLOWED);
            ((android.hardware.radio.V1_4.IRadio) mRadioProxy).setAllowedCarriers_1_4(
                    serial, carrierRestrictions, RILUtils.convertToHalSimLockMultiSimPolicy(
                            carrierRestrictionRules.getMultiSimPolicy()));
        } else {
            boolean isAllCarriersAllowed = carrierRestrictionRules.isAllCarriersAllowed();
            boolean supported = (isAllCarriersAllowed
                    || (carrierRestrictionRules.getExcludedCarriers().isEmpty()
                    && (carrierRestrictionRules.getDefaultCarrierRestriction()
                    == CarrierRestrictionRules.CARRIER_RESTRICTION_DEFAULT_NOT_ALLOWED)))
                    && (RILUtils.convertToHalSimLockMultiSimPolicy(
                    carrierRestrictionRules.getMultiSimPolicy())
                    == android.hardware.radio.V1_4.SimLockMultiSimPolicy.NO_MULTISIM_POLICY);

            if (!supported) {
                // Feature is not supported by IRadio interface
                if (result != null) {
                    AsyncResult.forMessage(result, null,
                            CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                    result.sendToTarget();
                }
                return;
            }

            // Prepare structure with allowed list
            android.hardware.radio.V1_0.CarrierRestrictions carrierRestrictions =
                    new android.hardware.radio.V1_0.CarrierRestrictions();
            carrierRestrictions.allowedCarriers = RILUtils.convertToHalCarrierRestrictionList(
                    carrierRestrictionRules.getAllowedCarriers());
            mRadioProxy.setAllowedCarriers(serial, isAllCarriersAllowed, carrierRestrictions);
        }
    }

    /**
     * Call IRadioSim#setCarrierInfoForImsiEncryption
     * @param serial Serial number of request
     * @param imsiEncryptionInfo ImsiEncryptionInfo
     * @throws RemoteException
     */
    public void setCarrierInfoForImsiEncryption(int serial, ImsiEncryptionInfo imsiEncryptionInfo)
            throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_1)) return;
        if (isAidl()) {
            android.hardware.radio.sim.ImsiEncryptionInfo halImsiInfo =
                    new android.hardware.radio.sim.ImsiEncryptionInfo();
            halImsiInfo.mnc = imsiEncryptionInfo.getMnc();
            halImsiInfo.mcc = imsiEncryptionInfo.getMcc();
            halImsiInfo.keyIdentifier = imsiEncryptionInfo.getKeyIdentifier();
            if (imsiEncryptionInfo.getExpirationTime() != null) {
                halImsiInfo.expirationTime = imsiEncryptionInfo.getExpirationTime().getTime();
            }
            halImsiInfo.carrierKey = imsiEncryptionInfo.getPublicKey().getEncoded();
            halImsiInfo.keyType = (byte) imsiEncryptionInfo.getKeyType();

            mSimProxy.setCarrierInfoForImsiEncryption(serial, halImsiInfo);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            android.hardware.radio.V1_6.ImsiEncryptionInfo halImsiInfo =
                    new android.hardware.radio.V1_6.ImsiEncryptionInfo();
            halImsiInfo.base.mnc = imsiEncryptionInfo.getMnc();
            halImsiInfo.base.mcc = imsiEncryptionInfo.getMcc();
            halImsiInfo.base.keyIdentifier = imsiEncryptionInfo.getKeyIdentifier();
            if (imsiEncryptionInfo.getExpirationTime() != null) {
                halImsiInfo.base.expirationTime = imsiEncryptionInfo.getExpirationTime().getTime();
            }
            for (byte b : imsiEncryptionInfo.getPublicKey().getEncoded()) {
                halImsiInfo.base.carrierKey.add(new Byte(b));
            }
            halImsiInfo.keyType = (byte) imsiEncryptionInfo.getKeyType();

            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).setCarrierInfoForImsiEncryption_1_6(
                    serial, halImsiInfo);
        } else {
            android.hardware.radio.V1_1.ImsiEncryptionInfo halImsiInfo =
                    new android.hardware.radio.V1_1.ImsiEncryptionInfo();
            halImsiInfo.mnc = imsiEncryptionInfo.getMnc();
            halImsiInfo.mcc = imsiEncryptionInfo.getMcc();
            halImsiInfo.keyIdentifier = imsiEncryptionInfo.getKeyIdentifier();
            if (imsiEncryptionInfo.getExpirationTime() != null) {
                halImsiInfo.expirationTime = imsiEncryptionInfo.getExpirationTime().getTime();
            }
            for (byte b : imsiEncryptionInfo.getPublicKey().getEncoded()) {
                halImsiInfo.carrierKey.add(new Byte(b));
            }

            ((android.hardware.radio.V1_1.IRadio) mRadioProxy).setCarrierInfoForImsiEncryption(
                    serial, halImsiInfo);
        }
    }

    /**
     * Call IRadioSim#setCdmaSubscriptionSource
     * @param serial Serial number of request
     * @param cdmaSub One of  CDMA_SUBSCRIPTION_*
     * @throws RemoteException
     */
    public void setCdmaSubscriptionSource(int serial, int cdmaSub) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.setCdmaSubscriptionSource(serial, cdmaSub);
        } else {
            mRadioProxy.setCdmaSubscriptionSource(serial, cdmaSub);
        }
    }

    /**
     * Call IRadioSim#setFacilityLockForApp
     * @param serial Serial number of request
     * @param facility One of CB_FACILTY_*
     * @param lockState True means lock, false means unlock
     * @param password Password or "" if not required
     * @param serviceClass Sum of SERVICE_CLASS_*
     * @param appId Application ID or null if none
     * @throws RemoteException
     */
    public void setFacilityLockForApp(int serial, String facility, boolean lockState,
            String password, int serviceClass, String appId) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.setFacilityLockForApp(
                    serial, facility, lockState, password, serviceClass, appId);
        } else {
            mRadioProxy.setFacilityLockForApp(
                    serial, facility, lockState, password, serviceClass, appId);
        }
    }

    /**
     * Call IRadioSim#setSimCardPower
     * @param serial Serial number of request
     * @param state SIM state (power down, power up, pass through)
     * @param result Result to return in case of error
     * @throws RemoteException
     */
    public void setSimCardPower(int serial, int state, Message result) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.setSimCardPower(serial, state);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).setSimCardPower_1_6(serial, state);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_1)) {
            ((android.hardware.radio.V1_1.IRadio) mRadioProxy).setSimCardPower_1_1(serial, state);
        } else {
            switch (state) {
                case TelephonyManager.CARD_POWER_DOWN: {
                    mRadioProxy.setSimCardPower(serial, false);
                    break;
                }
                case TelephonyManager.CARD_POWER_UP: {
                    mRadioProxy.setSimCardPower(serial, true);
                    break;
                }
                default: {
                    if (result != null) {
                        AsyncResult.forMessage(result, null,
                                CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                        result.sendToTarget();
                    }
                }
            }
        }
    }

    /**
     * Call IRadioSim#setUiccSubscription
     * @param serial Serial number of request
     * @param slotId Slot ID
     * @param appIndex Application index in the card
     * @param subId Subscription ID
     * @param subStatus Activation status; 1 = activate and 0 = deactivate
     * @throws RemoteException
     */
    public void setUiccSubscription(int serial, int slotId, int appIndex, int subId, int subStatus)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.sim.SelectUiccSub info =
                    new android.hardware.radio.sim.SelectUiccSub();
            info.slot = slotId;
            info.appIndex = appIndex;
            info.subType = subId;
            info.actStatus = subStatus;
            mSimProxy.setUiccSubscription(serial, info);
        } else {
            android.hardware.radio.V1_0.SelectUiccSub info =
                    new android.hardware.radio.V1_0.SelectUiccSub();
            info.slot = slotId;
            info.appIndex = appIndex;
            info.subType = subId;
            info.actStatus = subStatus;
            mRadioProxy.setUiccSubscription(serial, info);
        }
    }

    /**
     * Call IRadioSim#supplyIccPin2ForApp
     * @param serial Serial number of request
     * @param pin2 PIN 2 value
     * @param aid Application ID
     * @throws RemoteException
     */
    public void supplyIccPin2ForApp(int serial, String pin2, String aid) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.supplyIccPin2ForApp(serial, pin2, aid);
        } else {
            mRadioProxy.supplyIccPin2ForApp(serial, pin2, aid);
        }
    }

    /**
     * Call IRadioSim#supplyIccPinForApp
     * @param serial Serial number of request
     * @param pin PIN value
     * @param aid Application ID
     * @throws RemoteException
     */
    public void supplyIccPinForApp(int serial, String pin, String aid) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.supplyIccPinForApp(serial, pin, aid);
        } else {
            mRadioProxy.supplyIccPinForApp(serial, pin, aid);
        }
    }

    /**
     * Call IRadioSim#supplyIccPuk2ForApp
     * @param serial Serial number of request
     * @param puk2 PUK 2 value
     * @param pin2 PIN 2 value
     * @param aid Application ID
     * @throws RemoteException
     */
    public void supplyIccPuk2ForApp(int serial, String puk2, String pin2, String aid)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.supplyIccPuk2ForApp(serial, puk2, pin2, aid);
        } else {
            mRadioProxy.supplyIccPuk2ForApp(serial, puk2, pin2, aid);
        }
    }

    /**
     * Call IRadioSim#supplyIccPukForApp
     * @param serial Serial number of request
     * @param puk PUK value
     * @param pin PIN value
     * @param aid Application ID
     * @throws RemoteException
     */
    public void supplyIccPukForApp(int serial, String puk, String pin, String aid)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mSimProxy.supplyIccPukForApp(serial, puk, pin, aid);
        } else {
            mRadioProxy.supplyIccPukForApp(serial, puk, pin, aid);
        }
    }

    /**
     * Call IRadioSim#supplySimDepersonalization
     * @param serial Serial number of request
     * @param persoType SIM personalization type
     * @param controlKey Unlock code for removing SIM personalization from this device
     * @throws RemoteException
     */
    public void supplySimDepersonalization(int serial, PersoSubState persoType, String controlKey)
            throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_5)) return;
        if (isAidl()) {
            mSimProxy.supplySimDepersonalization(serial,
                    RILUtils.convertToHalPersoTypeAidl(persoType), controlKey);
        } else {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).supplySimDepersonalization(serial,
                    RILUtils.convertToHalPersoType(persoType), controlKey);
        }
    }

    /**
     * Call IRadioSim#updateSimPhonebookRecords
     * @param serial Serial number of request
     * @param recordInfo ADN record information to be updated
     * @throws RemoteException
     */
    public void updateSimPhonebookRecords(int serial, SimPhonebookRecord recordInfo)
            throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mSimProxy.updateSimPhonebookRecords(serial,
                    RILUtils.convertToHalPhonebookRecordInfoAidl(recordInfo));
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).updateSimPhonebookRecords(serial,
                    RILUtils.convertToHalPhonebookRecordInfo(recordInfo));
        }
    }
}

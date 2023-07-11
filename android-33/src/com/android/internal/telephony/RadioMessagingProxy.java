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

import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;

import java.util.ArrayList;

/**
 * A holder for IRadioMessaging. Use getHidl to get IRadio 1.0 and call the HIDL implementations or
 * getAidl to get IRadioMessaging and call the AIDL implementations of the HAL APIs.
 */
public class RadioMessagingProxy extends RadioServiceProxy {
    private static final String TAG = "RadioMessagingProxy";
    private volatile android.hardware.radio.messaging.IRadioMessaging mMessagingProxy = null;

    /**
     * Set IRadioMessaging as the AIDL implementation for RadioServiceProxy
     * @param halVersion Radio HAL version
     * @param messaging IRadioMessaging implementation
     */
    public void setAidl(HalVersion halVersion,
            android.hardware.radio.messaging.IRadioMessaging messaging) {
        mHalVersion = halVersion;
        mMessagingProxy = messaging;
        mIsAidl = true;
        Rlog.d(TAG, "AIDL initialized");
    }

    /**
     * Get the AIDL implementation of RadioMessagingProxy
     * @return IRadioMessaging implementation
     */
    public android.hardware.radio.messaging.IRadioMessaging getAidl() {
        return mMessagingProxy;
    }

    /**
     * Reset RadioMessagingProxy
     */
    @Override
    public void clear() {
        super.clear();
        mMessagingProxy = null;
    }

    /**
     * Check whether a RadioMessaging implementation exists
     * @return true if there is neither a HIDL nor AIDL implementation
     */
    @Override
    public boolean isEmpty() {
        return mRadioProxy == null && mMessagingProxy == null;
    }

    /**
     * Call IRadioMessaging#acknowledgeIncomingGsmSmsWithPdu
     * @param serial Serial number of request
     * @param success True on successful receipt (RP-ACK) and false on failed receipt (RP-ERROR)
     * @param ackPdu Acknowledgement TPDU in hexadecimal format
     * @throws RemoteException
     */
    public void acknowledgeIncomingGsmSmsWithPdu(int serial, boolean success, String ackPdu)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.acknowledgeIncomingGsmSmsWithPdu(serial, success, ackPdu);
        } else {
            mRadioProxy.acknowledgeIncomingGsmSmsWithPdu(serial, success, ackPdu);
        }
    }

    /**
     * Calls IRadioMessaging#acknowledgeLastIncomingCdmaSms
     * @param serial Serial number of request
     * @param success True on successful receipt
     * @param cause Failure cause if success is false
     * @throws RemoteException
     */
    public void acknowledgeLastIncomingCdmaSms(int serial, boolean success, int cause)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.messaging.CdmaSmsAck msg =
                    new android.hardware.radio.messaging.CdmaSmsAck();
            msg.errorClass = success;
            msg.smsCauseCode = cause;
            mMessagingProxy.acknowledgeLastIncomingCdmaSms(serial, msg);
        } else {
            android.hardware.radio.V1_0.CdmaSmsAck msg =
                    new android.hardware.radio.V1_0.CdmaSmsAck();
            msg.errorClass = success ? 0 : 1;
            msg.smsCauseCode = cause;
            mRadioProxy.acknowledgeLastIncomingCdmaSms(serial, msg);
        }
    }

    /**
     * Calls IRadioMessaging#acknowledgeLastIncomingGsmSms
     * @param serial Serial number of request
     * @param success True on successful receipt
     * @param cause Failure cause if success is false
     * @throws RemoteException
     */
    public void acknowledgeLastIncomingGsmSms(int serial, boolean success, int cause)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.acknowledgeLastIncomingGsmSms(serial, success, cause);
        } else {
            mRadioProxy.acknowledgeLastIncomingGsmSms(serial, success, cause);
        }
    }

    /**
     * Call IRadioMessaging#deleteSmsOnRuim
     * @param serial Serial number of request
     * @param index Record index of the message to delete
     * @throws RemoteException
     */
    public void deleteSmsOnRuim(int serial, int index) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.deleteSmsOnRuim(serial, index);
        } else {
            mRadioProxy.deleteSmsOnRuim(serial, index);
        }
    }

    /**
     * Call IRadioMessaging#deleteSmsOnSim
     * @param serial Serial number of request
     * @param index Record index of the message to delete
     * @throws RemoteException
     */
    public void deleteSmsOnSim(int serial, int index) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.deleteSmsOnSim(serial, index);
        } else {
            mRadioProxy.deleteSmsOnSim(serial, index);
        }
    }

    /**
     * Call IRadioMessaging#getCdmaBroadcastConfig
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getCdmaBroadcastConfig(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.getCdmaBroadcastConfig(serial);
        } else {
            mRadioProxy.getCdmaBroadcastConfig(serial);
        }
    }

    /**
     * Call IRadioMessaging#getGsmBroadcastConfig
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getGsmBroadcastConfig(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.getGsmBroadcastConfig(serial);
        } else {
            mRadioProxy.getGsmBroadcastConfig(serial);
        }
    }

    /**
     * Call IRadioMessaging#getSmscAddress
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getSmscAddress(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.getSmscAddress(serial);
        } else {
            mRadioProxy.getSmscAddress(serial);
        }
    }

    /**
     * Call IRadioMessaging#reportSmsMemoryStatus
     * @param serial Serial number of request
     * @param available Whether or not storage is available
     * @throws RemoteException
     */
    public void reportSmsMemoryStatus(int serial, boolean available) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.reportSmsMemoryStatus(serial, available);
        } else {
            mRadioProxy.reportSmsMemoryStatus(serial, available);
        }
    }

    /**
     * Call IRadioMessaging#responseAcknowledgement
     * @throws RemoteException
     */
    @Override
    public void responseAcknowledgement() throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.responseAcknowledgement();
        } else {
            mRadioProxy.responseAcknowledgement();
        }
    }

    /**
     * Call IRadioMessaging#sendCdmaSms
     * @param serial Serial number of request
     * @param pdu CDMA-SMS in internal pseudo-PDU format
     * @throws RemoteException
     */
    public void sendCdmaSms(int serial, byte[] pdu) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.sendCdmaSms(serial, RILUtils.convertToHalCdmaSmsMessageAidl(pdu));
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).sendCdmaSms_1_6(
                    serial, RILUtils.convertToHalCdmaSmsMessage(pdu));
        } else {
            mRadioProxy.sendCdmaSms(serial, RILUtils.convertToHalCdmaSmsMessage(pdu));
        }
    }

    /**
     * Call IRadioMessaging#sendCdmaSmsExpectMore
     * @param serial Serial number of request
     * @param pdu CDMA-SMS in internal pseudo-PDU format
     * @throws RemoteException
     */
    public void sendCdmaSmsExpectMore(int serial, byte[] pdu) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.sendCdmaSmsExpectMore(
                    serial, RILUtils.convertToHalCdmaSmsMessageAidl(pdu));
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).sendCdmaSmsExpectMore_1_6(
                    serial, RILUtils.convertToHalCdmaSmsMessage(pdu));
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).sendCdmaSmsExpectMore(
                    serial, RILUtils.convertToHalCdmaSmsMessage(pdu));
        } else {
            mRadioProxy.sendCdmaSms(serial, RILUtils.convertToHalCdmaSmsMessage(pdu));
        }
    }

    /**
     * Call IRadioMessaging#sendImsSms
     * @param serial Serial number of request
     * @param smscPdu SMSC address in PDU form GSM BCD format prefixed by a length byte
     *                or NULL for default SMSC
     * @param gsmPdu SMS in PDU format as an ASCII hex string less the SMSC address
     * @param cdmaPdu CDMA-SMS in internal pseudo-PDU format
     * @param retry Whether this is a retry; 0 == not retry, nonzero = retry
     * @param messageRef MessageRef from RIL_SMS_RESPONSE corresponding to failed MO SMS
     *                   if retry is nonzero
     * @throws RemoteException
     */
    public void sendImsSms(int serial, String smscPdu, String gsmPdu, byte[] cdmaPdu, int retry,
            int messageRef) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.messaging.ImsSmsMessage msg =
                    new android.hardware.radio.messaging.ImsSmsMessage();
            msg.tech = android.hardware.radio.RadioTechnologyFamily.THREE_GPP;
            msg.retry = (byte) retry >= 1;
            msg.messageRef = messageRef;
            if (gsmPdu != null) {
                msg.gsmMessage = new android.hardware.radio.messaging.GsmSmsMessage[]{
                        RILUtils.convertToHalGsmSmsMessageAidl(smscPdu, gsmPdu)};
                msg.cdmaMessage = new android.hardware.radio.messaging.CdmaSmsMessage[0];
            }
            if (cdmaPdu != null) {
                msg.gsmMessage = new android.hardware.radio.messaging.GsmSmsMessage[0];
                msg.cdmaMessage = new android.hardware.radio.messaging.CdmaSmsMessage[]{
                        RILUtils.convertToHalCdmaSmsMessageAidl(cdmaPdu)};
            }
            mMessagingProxy.sendImsSms(serial, msg);
        } else {
            android.hardware.radio.V1_0.ImsSmsMessage msg =
                    new android.hardware.radio.V1_0.ImsSmsMessage();
            msg.tech = android.hardware.radio.V1_0.RadioTechnologyFamily.THREE_GPP;
            msg.retry = (byte) retry >= 1;
            msg.messageRef = messageRef;
            if (gsmPdu != null) {
                msg.gsmMessage.add(RILUtils.convertToHalGsmSmsMessage(smscPdu, gsmPdu));
            }
            if (cdmaPdu != null) {
                msg.cdmaMessage.add(RILUtils.convertToHalCdmaSmsMessage(cdmaPdu));
            }
            mRadioProxy.sendImsSms(serial, msg);
        }
    }

    /**
     * Call IRadioMessaging#sendSms
     * @param serial Serial number of request
     * @param smscPdu SMSC address in PDU form GSM BCD format prefixed by a length byte
     *                or NULL for default SMSC
     * @param pdu SMS in PDU format as an ASCII hex string less the SMSC address
     * @throws RemoteException
     */
    public void sendSms(int serial, String smscPdu, String pdu) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.sendSms(serial, RILUtils.convertToHalGsmSmsMessageAidl(smscPdu, pdu));
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).sendSms_1_6(
                    serial, RILUtils.convertToHalGsmSmsMessage(smscPdu, pdu));
        } else {
            mRadioProxy.sendSms(serial, RILUtils.convertToHalGsmSmsMessage(smscPdu, pdu));
        }
    }

    /**
     * Call IRadioMessaging#sendSmsExpectMore
     * @param serial Serial number of request
     * @param smscPdu SMSC address in PDU form GSM BCD format prefixed by a length byte
     *                or NULL for default SMSC
     * @param pdu SMS in PDU format as an ASCII hex string less the SMSC address
     * @throws RemoteException
     */
    public void sendSmsExpectMore(int serial, String smscPdu, String pdu) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.sendSmsExpectMore(serial,
                    RILUtils.convertToHalGsmSmsMessageAidl(smscPdu, pdu));
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).sendSmsExpectMore_1_6(serial,
                    RILUtils.convertToHalGsmSmsMessage(smscPdu, pdu));
        } else {
            mRadioProxy.sendSMSExpectMore(serial, RILUtils.convertToHalGsmSmsMessage(smscPdu, pdu));
        }
    }

    /**
     * Call IRadioMessaging#setCdmaBroadcastActivation
     * @param serial Serial number of request
     * @param activate Whether to activate or turn off the reception of CDMA Cell Broadcast SMS;
     *                 true = activate, false = turn off
     * @throws RemoteException
     */
    public void setCdmaBroadcastActivation(int serial, boolean activate) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.setCdmaBroadcastActivation(serial, activate);
        } else {
            mRadioProxy.setCdmaBroadcastActivation(serial, activate);
        }
    }

    /**
     * Call IRadioMessaging#setCdmaBroadcastConfig
     * @param serial Serial number of request
     * @param configs Setting of CDMA cell broadcast config
     * @throws RemoteException
     */
    public void setCdmaBroadcastConfig(int serial, CdmaSmsBroadcastConfigInfo[] configs)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            ArrayList<android.hardware.radio.messaging.CdmaBroadcastSmsConfigInfo> halConfigs =
                    new ArrayList<>();
            for (CdmaSmsBroadcastConfigInfo config: configs) {
                for (int i = config.getFromServiceCategory(); i <= config.getToServiceCategory();
                        i++) {
                    android.hardware.radio.messaging.CdmaBroadcastSmsConfigInfo info =
                            new android.hardware.radio.messaging.CdmaBroadcastSmsConfigInfo();
                    info.serviceCategory = i;
                    info.language = config.getLanguage();
                    info.selected = config.isSelected();
                    halConfigs.add(info);
                }
            }
            mMessagingProxy.setCdmaBroadcastConfig(serial, halConfigs.stream().toArray(
                    android.hardware.radio.messaging.CdmaBroadcastSmsConfigInfo[]::new));
        } else {
            ArrayList<android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo> halConfigs =
                    new ArrayList<>();
            for (CdmaSmsBroadcastConfigInfo config: configs) {
                for (int i = config.getFromServiceCategory(); i <= config.getToServiceCategory();
                        i++) {
                    android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo info =
                            new android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo();
                    info.serviceCategory = i;
                    info.language = config.getLanguage();
                    info.selected = config.isSelected();
                    halConfigs.add(info);
                }
            }
            mRadioProxy.setCdmaBroadcastConfig(serial, halConfigs);
        }
    }

    /**
     * Call IRadioMessaging#setGsmBroadcastActivation
     * @param serial Serial number of request
     * @param activate Whether to activate or turn off the reception of GSM/WCDMA Cell Broadcast
     *                 SMS; true = activate, false = turn off
     * @throws RemoteException
     */
    public void setGsmBroadcastActivation(int serial, boolean activate) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.setGsmBroadcastActivation(serial, activate);
        } else {
            mRadioProxy.setGsmBroadcastActivation(serial, activate);
        }
    }

    /**
     * Call IRadioMessaging#setGsmBroadcastConfig
     * @param serial Serial number of request
     * @param configInfo Setting of GSM/WCDMA cell broadcast config
     * @throws RemoteException
     */
    public void setGsmBroadcastConfig(int serial, SmsBroadcastConfigInfo[] configInfo)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo[] configs =
                    new android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo[
                            configInfo.length];
            android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo info;
            for (int i = 0; i < configInfo.length; i++) {
                info = new android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo();
                info.fromServiceId = configInfo[i].getFromServiceId();
                info.toServiceId = configInfo[i].getToServiceId();
                info.fromCodeScheme = configInfo[i].getFromCodeScheme();
                info.toCodeScheme = configInfo[i].getToCodeScheme();
                info.selected = configInfo[i].isSelected();
                configs[i] = info;
            }
            mMessagingProxy.setGsmBroadcastConfig(serial, configs);
        } else {
            ArrayList<android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo> configs =
                    new ArrayList<>();
            android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo info;
            for (int i = 0; i < configInfo.length; i++) {
                info = new android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo();
                info.fromServiceId = configInfo[i].getFromServiceId();
                info.toServiceId = configInfo[i].getToServiceId();
                info.fromCodeScheme = configInfo[i].getFromCodeScheme();
                info.toCodeScheme = configInfo[i].getToCodeScheme();
                info.selected = configInfo[i].isSelected();
                configs.add(info);
            }
            mRadioProxy.setGsmBroadcastConfig(serial, configs);
        }
    }

    /**
     * Call IRadioMessaging#setSmscAddress
     * @param serial Serial number of request
     * @param smsc Short Message Service Center address to set
     * @throws RemoteException
     */
    public void setSmscAddress(int serial, String smsc) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mMessagingProxy.setSmscAddress(serial, smsc);
        } else {
            mRadioProxy.setSmscAddress(serial, smsc);
        }
    }

    /**
     * Call IRadioMessaging#writeSmsToRuim
     * @param serial Serial number of request
     * @param status Status of message on SIM. One of:
     *               SmsManager.STATUS_ON_ICC_READ
     *               SmsManager.STATUS_ON_ICC_UNREAD
     *               SmsManager.STATUS_ON_ICC_SENT
     *               SmsManager.STATUS_ON_ICC_UNSENT
     * @param pdu SMS in PDU format as a byte array
     * @throws RemoteException
     */
    public void writeSmsToRuim(int serial, int status, byte[] pdu) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.messaging.CdmaSmsWriteArgs args =
                    new android.hardware.radio.messaging.CdmaSmsWriteArgs();
            args.status = RILUtils.convertToHalSmsWriteArgsStatusAidl(status);
            args.message = RILUtils.convertToHalCdmaSmsMessageAidl(pdu);
            mMessagingProxy.writeSmsToRuim(serial, args);
        } else {
            android.hardware.radio.V1_0.CdmaSmsWriteArgs args =
                    new android.hardware.radio.V1_0.CdmaSmsWriteArgs();
            args.status = RILUtils.convertToHalSmsWriteArgsStatus(status);
            args.message = RILUtils.convertToHalCdmaSmsMessage(pdu);
            mRadioProxy.writeSmsToRuim(serial, args);
        }
    }

    /**
     * Call IRadioMessaging#writeSmsToSim
     * @param serial Serial number of request
     * @param status Status of message on SIM. One of:
     *               SmsManager.STATUS_ON_ICC_READ
     *               SmsManager.STATUS_ON_ICC_UNREAD
     *               SmsManager.STATUS_ON_ICC_SENT
     *               SmsManager.STATUS_ON_ICC_UNSENT
     * @param smsc SMSC address
     * @param pdu SMS in PDU format as an ASCII hex string less the SMSC address
     * @throws RemoteException
     */
    public void writeSmsToSim(int serial, int status, String smsc, String pdu)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.messaging.SmsWriteArgs args =
                    new android.hardware.radio.messaging.SmsWriteArgs();
            args.status = RILUtils.convertToHalSmsWriteArgsStatusAidl(status);
            args.smsc = smsc;
            args.pdu = pdu;
            mMessagingProxy.writeSmsToSim(serial, args);
        } else {
            android.hardware.radio.V1_0.SmsWriteArgs args =
                    new android.hardware.radio.V1_0.SmsWriteArgs();
            args.status = RILUtils.convertToHalSmsWriteArgsStatus(status);
            args.smsc = smsc;
            args.pdu = pdu;
            mRadioProxy.writeSmsToSim(serial, args);
        }
    }
}

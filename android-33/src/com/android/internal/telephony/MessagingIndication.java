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

import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_CDMA_NEW_SMS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_SMS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SIM_SMS_STORAGE_FULL;

import android.hardware.radio.messaging.IRadioMessagingIndication;
import android.os.AsyncResult;
import android.telephony.SmsMessage;

import com.android.internal.telephony.uicc.IccUtils;

/**
 * Interface declaring unsolicited radio indications for messaging APIs.
 */
public class MessagingIndication extends IRadioMessagingIndication.Stub {
    private final RIL mRil;

    public MessagingIndication(RIL ril) {
        mRil = ril;
    }

    /**
     * Indicates when new CDMA SMS is received.
     * @param indicationType Type of radio indication
     * @param msg CdmaSmsMessage
     */
    public void cdmaNewSms(int indicationType,
            android.hardware.radio.messaging.CdmaSmsMessage msg) {
        mRil.processIndication(RIL.MESSAGING_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_CDMA_NEW_SMS);

        SmsMessage sms = new SmsMessage(RILUtils.convertHalCdmaSmsMessage(msg));
        if (mRil.mCdmaSmsRegistrant != null) {
            mRil.mCdmaSmsRegistrant.notifyRegistrant(new AsyncResult(null, sms, null));
        }
    }

    /**
     * Indicates that SMS storage on the RUIM is full. Messages cannot be saved on the RUIM until
     * space is freed.
     * @param indicationType Type of radio indication
     */
    public void cdmaRuimSmsStorageFull(int indicationType) {
        mRil.processIndication(RIL.MESSAGING_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL);

        if (mRil.mIccSmsFullRegistrant != null) {
            mRil.mIccSmsFullRegistrant.notifyRegistrant();
        }
    }

    /**
     * Indicates when new Broadcast SMS is received
     * @param indicationType Type of radio indication
     * @param data If received from GSM network, "data" is byte array of 88 bytes which indicates
     *        each page of a CBS Message sent to the MS by the BTS as coded in 3GPP 23.041 Section
     *        9.4.1.2. If received from UMTS network, "data" is byte array of 90 up to 1252 bytes
     *        which contain between 1 and 15 CBS Message pages sent as one packet to the MS by the
     *        BTS as coded in 3GPP 23.041 Section 9.4.2.2
     */
    public void newBroadcastSms(int indicationType, byte[] data) {
        mRil.processIndication(RIL.MESSAGING_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) {
            mRil.unsljLogvRet(RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS,
                    IccUtils.bytesToHexString(data));
        }

        if (mRil.mGsmBroadcastSmsRegistrant != null) {
            mRil.mGsmBroadcastSmsRegistrant.notifyRegistrant(new AsyncResult(null, data, null));
        }
    }

    /**
     * Indicates when new SMS is received.
     * @param indicationType Type of radio indication
     * @param pdu PDU of SMS-DELIVER represented as byte array.
     *        The PDU starts with the SMSC address per TS 27.005 (+CMT:)
     */
    public void newSms(int indicationType, byte[] pdu) {
        mRil.processIndication(RIL.MESSAGING_SERVICE, indicationType);
        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_NEW_SMS);

        SmsMessageBase smsb = com.android.internal.telephony.gsm.SmsMessage.createFromPdu(pdu);
        if (mRil.mGsmSmsRegistrant != null) {
            mRil.mGsmSmsRegistrant.notifyRegistrant(
                    new AsyncResult(null, smsb == null ? null : new SmsMessage(smsb), null));
        }
    }

    /**
     * Indicates when new SMS has been stored on SIM card.
     * @param indicationType Type of radio indication
     * @param recordNumber Record number on the SIM
     */
    public void newSmsOnSim(int indicationType, int recordNumber) {
        mRil.processIndication(RIL.MESSAGING_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM);

        if (mRil.mSmsOnSimRegistrant != null) {
            mRil.mSmsOnSimRegistrant.notifyRegistrant(new AsyncResult(null, recordNumber, null));
        }
    }

    /**
     * Indicates when new SMS Status Report is received.
     * @param indicationType Type of radio indication
     * @param pdu PDU of SMS-STATUS-REPORT represented as byte array.
     *        The PDU starts with the SMSC address per TS 27.005 (+CMT:)
     */
    public void newSmsStatusReport(int indicationType, byte[] pdu) {
        mRil.processIndication(RIL.MESSAGING_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT);

        if (mRil.mSmsStatusRegistrant != null) {
            mRil.mSmsStatusRegistrant.notifyRegistrant(new AsyncResult(null, pdu, null));
        }
    }

    /**
     * Indicates that SMS storage on the SIM is full. Sent when the network attempts to deliver a
     * new SMS message. Messages cannot be saved on the SIM until space is freed. In particular,
     * incoming Class 2 messages must not be stored.
     * @param indicationType Type of radio indication
     */
    public void simSmsStorageFull(int indicationType) {
        mRil.processIndication(RIL.MESSAGING_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_SIM_SMS_STORAGE_FULL);

        if (mRil.mIccSmsFullRegistrant != null) {
            mRil.mIccSmsFullRegistrant.notifyRegistrant();
        }
    }

    @Override
    public String getInterfaceHash() {
        return IRadioMessagingIndication.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return IRadioMessagingIndication.VERSION;
    }
}

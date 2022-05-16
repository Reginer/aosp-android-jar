/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony.gsm;

import static com.android.internal.telephony.SmsResponse.NO_ERROR_CODE;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Message;
import android.telephony.ServiceState;

import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsController;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.util.SMSDispatcherUtil;
import com.android.telephony.Rlog;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class GsmSMSDispatcher extends SMSDispatcher {
    private static final String TAG = "GsmSMSDispatcher";
    protected UiccController mUiccController = null;
    private AtomicReference<IccRecords> mIccRecords = new AtomicReference<IccRecords>();
    private AtomicReference<UiccCardApplication> mUiccApplication =
            new AtomicReference<UiccCardApplication>();
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private GsmInboundSmsHandler mGsmInboundSmsHandler;

    /** Status report received */
    private static final int EVENT_NEW_SMS_STATUS_REPORT = 100;

    public GsmSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController,
            GsmInboundSmsHandler gsmInboundSmsHandler) {
        super(phone, smsDispatchersController);
        mCi.setOnSmsStatus(this, EVENT_NEW_SMS_STATUS_REPORT, null);
        mGsmInboundSmsHandler = gsmInboundSmsHandler;
        mUiccController = UiccController.getInstance();
        mUiccController.registerForIccChanged(this, EVENT_ICC_CHANGED, null);
        Rlog.d(TAG, "GsmSMSDispatcher created");
    }

    @Override
    public void dispose() {
        super.dispose();
        mCi.unSetOnSmsStatus(this);
        mUiccController.unregisterForIccChanged(this);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    protected String getFormat() {
        return SmsConstants.FORMAT_3GPP;
    }

    /**
     * Handles 3GPP format-specific events coming from the phone stack.
     * Other events are handled by {@link SMSDispatcher#handleMessage}.
     *
     * @param msg the message to handle
     */
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
        case EVENT_NEW_SMS_STATUS_REPORT:
            handleStatusReport((AsyncResult) msg.obj);
            break;

        case EVENT_NEW_ICC_SMS:
        // pass to InboundSmsHandler to process
        mGsmInboundSmsHandler.sendMessage(InboundSmsHandler.EVENT_NEW_SMS, msg.obj);
        break;

        case EVENT_ICC_CHANGED:
            onUpdateIccAvailability();
            break;

        default:
            super.handleMessage(msg);
        }
    }

    @Override
    protected boolean shouldBlockSmsForEcbm() {
        // There is no such thing as ECBM for GSM. This only applies to CDMA.
        return false;
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String scAddr, String destAddr,
            String message, boolean statusReportRequested, SmsHeader smsHeader, int priority,
            int validityPeriod) {
        return SMSDispatcherUtil.getSubmitPduGsm(scAddr, destAddr, message, statusReportRequested,
                validityPeriod);
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String scAddr, String destAddr,
            int destPort, byte[] message, boolean statusReportRequested) {
        return SMSDispatcherUtil.getSubmitPduGsm(scAddr, destAddr, destPort, message,
                statusReportRequested);
    }

    @Override
    protected TextEncodingDetails calculateLength(CharSequence messageBody, boolean use7bitOnly) {
        return SMSDispatcherUtil.calculateLengthGsm(messageBody, use7bitOnly);
    }

    /**
     * Called when a status report is received. This should correspond to a previously successful
     * SEND.
     *
     * @param ar AsyncResult passed into the message handler. ar.result should be a byte array for
     *           the status report PDU.
     */
    private void handleStatusReport(AsyncResult ar) {
        byte[] pdu = (byte[]) ar.result;
        mSmsDispatchersController.handleSmsStatusReport(SmsConstants.FORMAT_3GPP, pdu);
        mCi.acknowledgeLastIncomingGsmSms(true, 0 /* cause */, null);
    }

    /** {@inheritDoc} */
    @UnsupportedAppUsage
    @Override
    protected void sendSms(SmsTracker tracker) {
        int ss = mPhone.getServiceState().getState();

        Rlog.d(TAG, "sendSms: "
                + " isIms()=" + isIms()
                + " mRetryCount=" + tracker.mRetryCount
                + " mImsRetry=" + tracker.mImsRetry
                + " mMessageRef=" + tracker.mMessageRef
                + " mUsesImsServiceForIms=" + tracker.mUsesImsServiceForIms
                + " SS=" + ss
                + " " + SmsController.formatCrossStackMessageId(tracker.mMessageId));

        // if sms over IMS is not supported on data and voice is not available...
        if (!isIms() && ss != ServiceState.STATE_IN_SERVICE) {
        //In 5G case only Data Rat is reported.
            if(mPhone.getServiceState().getRilDataRadioTechnology()
                    != ServiceState.RIL_RADIO_TECHNOLOGY_NR) {
                tracker.onFailed(mContext, getNotInServiceError(ss), NO_ERROR_CODE);
                return;
            }
        }

        Message reply = obtainMessage(EVENT_SEND_SMS_COMPLETE, tracker);
        HashMap<String, Object> map = tracker.getData();
        byte pdu[] = (byte[]) map.get("pdu");
        byte smsc[] = (byte[]) map.get("smsc");
        if (tracker.mRetryCount > 0) {
            // per TS 23.040 Section 9.2.3.6:  If TP-MTI SMS-SUBMIT (0x01) type
            //   TP-RD (bit 2) is 1 for retry
            //   and TP-MR is set to previously failed sms TP-MR
            if (((0x01 & pdu[0]) == 0x01)) {
                pdu[0] |= 0x04; // TP-RD
                pdu[1] = (byte) tracker.mMessageRef; // TP-MR
            }
        }

        // sms over gsm is used:
        //   if sms over IMS is not supported AND
        //   this is not a retry case after sms over IMS failed
        //     indicated by mImsRetry > 0 OR
        //   this tracker uses ImsSmsDispatcher to handle SMS over IMS. This dispatcher has received
        //     this message because the ImsSmsDispatcher has indicated that the message needs to
        //     fall back to sending over CS.
        if (0 == tracker.mImsRetry && !isIms() || tracker.mUsesImsServiceForIms) {
            if (tracker.mRetryCount == 0 && tracker.mExpectMore) {
                mCi.sendSMSExpectMore(IccUtils.bytesToHexString(smsc),
                        IccUtils.bytesToHexString(pdu), reply);
            } else {
                mCi.sendSMS(IccUtils.bytesToHexString(smsc),
                        IccUtils.bytesToHexString(pdu), reply);
            }
        } else {
            mCi.sendImsGsmSms(IccUtils.bytesToHexString(smsc),
                    IccUtils.bytesToHexString(pdu), tracker.mImsRetry,
                    tracker.mMessageRef, reply);
            // increment it here, so in case of SMS_FAIL_RETRY over IMS
            // next retry will be sent using IMS request again.
            tracker.mImsRetry++;
        }
    }

    protected UiccCardApplication getUiccCardApplication() {
            Rlog.d(TAG, "GsmSMSDispatcher: subId = " + mPhone.getSubId()
                    + " slotId = " + mPhone.getPhoneId());
                return mUiccController.getUiccCardApplication(mPhone.getPhoneId(),
                        UiccController.APP_FAM_3GPP);
    }

    private void onUpdateIccAvailability() {
        if (mUiccController == null ) {
            return;
        }

        UiccCardApplication newUiccApplication = getUiccCardApplication();

        UiccCardApplication app = mUiccApplication.get();
        if (app != newUiccApplication) {
            if (app != null) {
                Rlog.d(TAG, "Removing stale icc objects.");
                if (mIccRecords.get() != null) {
                    mIccRecords.get().unregisterForNewSms(this);
                }
                mIccRecords.set(null);
                mUiccApplication.set(null);
            }
            if (newUiccApplication != null) {
                Rlog.d(TAG, "New Uicc application found");
                mUiccApplication.set(newUiccApplication);
                mIccRecords.set(newUiccApplication.getIccRecords());
                if (mIccRecords.get() != null) {
                    mIccRecords.get().registerForNewSms(this, EVENT_NEW_ICC_SMS, null);
                }
            }
        }
    }
}

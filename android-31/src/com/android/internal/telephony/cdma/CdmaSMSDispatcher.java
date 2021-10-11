/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.internal.telephony.cdma;

import static com.android.internal.telephony.SmsResponse.NO_ERROR_CODE;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.Message;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsController;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.util.SMSDispatcherUtil;
import com.android.telephony.Rlog;

public class CdmaSMSDispatcher extends SMSDispatcher {
    private static final String TAG = "CdmaSMSDispatcher";
    private static final boolean VDBG = false;

    public CdmaSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        super(phone, smsDispatchersController);
        Rlog.d(TAG, "CdmaSMSDispatcher created");
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public String getFormat() {
        return SmsConstants.FORMAT_3GPP2;
    }

    /**
     * Send the SMS status report to the dispatcher thread to process.
     * @param sms the CDMA SMS message containing the status report
     */
    public void sendStatusReportMessage(SmsMessage sms) {
        if (VDBG) Rlog.d(TAG, "sending EVENT_HANDLE_STATUS_REPORT message");
        sendMessage(obtainMessage(EVENT_HANDLE_STATUS_REPORT, sms));
    }

    @Override
    protected void handleStatusReport(Object o) {
        if (o instanceof SmsMessage) {
            if (VDBG) Rlog.d(TAG, "calling handleSmsStatusReport()");
            byte[] pdu = ((SmsMessage) o).getPdu();
            mSmsDispatchersController.handleSmsStatusReport(SmsConstants.FORMAT_3GPP2, pdu);
        } else {
            Rlog.e(TAG, "handleStatusReport() called for object type " + o.getClass().getName());
        }
    }

    @Override
    protected boolean shouldBlockSmsForEcbm() {
        // We only block outgoing SMS during ECBM when using CDMA.
        return mPhone.isInEcm() && isCdmaMo() && !isIms();
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String scAddr, String destAddr,
            String message, boolean statusReportRequested, SmsHeader smsHeader, int priority,
            int validityPeriod) {
        return SMSDispatcherUtil.getSubmitPduCdma(scAddr, destAddr, message,
                statusReportRequested, smsHeader, priority);
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String scAddr, String destAddr,
            int destPort, byte[] message, boolean statusReportRequested) {
        return SMSDispatcherUtil.getSubmitPduCdma(scAddr, destAddr, destPort, message,
                statusReportRequested);
    }

    @Override
    protected TextEncodingDetails calculateLength(CharSequence messageBody, boolean use7bitOnly) {
        return SMSDispatcherUtil.calculateLengthCdma(messageBody, use7bitOnly);
    }

    /** {@inheritDoc} */
    @Override
    public void sendSms(SmsTracker tracker) {
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
            tracker.onFailed(mContext, getNotInServiceError(ss), NO_ERROR_CODE);
            return;
        }

        Message reply = obtainMessage(EVENT_SEND_SMS_COMPLETE, tracker);
        byte[] pdu = (byte[]) tracker.getData().get("pdu");

        int currentDataNetwork = mPhone.getServiceState().getDataNetworkType();
        boolean imsSmsDisabled = (currentDataNetwork == TelephonyManager.NETWORK_TYPE_EHRPD
                || (currentDataNetwork == TelephonyManager.NETWORK_TYPE_LTE
                || currentDataNetwork == TelephonyManager.NETWORK_TYPE_LTE_CA
                || currentDataNetwork == TelephonyManager.NETWORK_TYPE_NR)
                && !mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed())
                && mPhone.getServiceState().getVoiceNetworkType()
                        == TelephonyManager.NETWORK_TYPE_1xRTT
                && ((GsmCdmaPhone) mPhone).mCT.mState != PhoneConstants.State.IDLE;

        // sms over cdma is used:
        //   if sms over IMS is not supported AND
        //   this is not a retry case after sms over IMS failed
        //     indicated by mImsRetry > 0 OR
        //   SMS over IMS is disabled because of the network type OR
        //   SMS over IMS is being handled by the ImsSmsDispatcher implementation and has indicated
        //   that the message should fall back to sending over CS.
        if (0 == tracker.mImsRetry && !isIms() || imsSmsDisabled || tracker.mUsesImsServiceForIms) {
            if (tracker.mRetryCount == 0 && tracker.mExpectMore) {
                mCi.sendCdmaSMSExpectMore(pdu, reply);
            } else {
                mCi.sendCdmaSms(pdu, reply);
            }
        } else {
            mCi.sendImsCdmaSms(pdu, tracker.mImsRetry, tracker.mMessageRef, reply);
            // increment it here, so in case of SMS_FAIL_RETRY over IMS
            // next retry will be sent using IMS request again.
            tracker.mImsRetry++;
        }
    }
}

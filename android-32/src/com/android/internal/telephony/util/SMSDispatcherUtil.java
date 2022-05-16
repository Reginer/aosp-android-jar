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
 * limitations under the License
 */

package com.android.internal.telephony.util;

import com.android.internal.telephony.ImsSmsDispatcher;
import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.gsm.GsmSMSDispatcher;

/**
 * Utilities used by {@link com.android.internal.telephony.SMSDispatcher}'s subclasses.
 *
 * These methods can not be moved to {@link CdmaSMSDispatcher} and {@link GsmSMSDispatcher} because
 * they also need to be called from {@link ImsSmsDispatcher} and the utilities will invoke the cdma
 * or gsm version of the method based on the format.
 */
public final class SMSDispatcherUtil {
    // Prevent instantiation.
    private SMSDispatcherUtil() {}

    /**
     * Trigger the proper implementation for getting submit pdu for text sms based on format.
     *
     * @param isCdma true if cdma format should be used.
     * @param scAddr is the service center address or null to use the current default SMSC
     * @param destAddr the address to send the message to
     * @param message the body of the message.
     * @param statusReportRequested whether or not a status report is requested.
     * @param smsHeader message header.
     * @return the submit pdu.
     */
    public static SmsMessageBase.SubmitPduBase getSubmitPdu(boolean isCdma, String scAddr,
            String destAddr, String message, boolean statusReportRequested, SmsHeader smsHeader) {
        if (isCdma) {
            return getSubmitPduCdma(scAddr, destAddr, message, statusReportRequested, smsHeader);
        } else {
            return getSubmitPduGsm(scAddr, destAddr, message, statusReportRequested);
        }
    }

    /**
     * Trigger the proper implementation for getting submit pdu for text sms based on format.
     *
     * @param isCdma true if cdma format should be used.
     * @param scAddr is the service center address or null to use the current default SMSC
     * @param destAddr the address to send the message to
     * @param message the body of the message.
     * @param statusReportRequested whether or not a status report is requested.
     * @param smsHeader message header.
     * @param priority Priority level of the message
     *  Refer specification See 3GPP2 C.S0015-B, v2.0, table 4.5.9-1
     *  ---------------------------------
     *  PRIORITY      | Level of Priority
     *  ---------------------------------
     *      '00'      |     Normal
     *      '01'      |     Interactive
     *      '10'      |     Urgent
     *      '11'      |     Emergency
     *  ----------------------------------
     *  Any Other values included Negative considered as Invalid Priority Indicator of the message.
     * @param validityPeriod Validity Period of the message in mins.
     *  Refer specification 3GPP TS 23.040 V6.8.1 section 9.2.3.12.1.
     *  Validity Period(Minimum) -> 5 mins
     *  Validity Period(Maximum) -> 635040 mins(i.e.63 weeks).
     *  Any Other values included Negative considered as Invalid Validity Period of the message.
     * @return the submit pdu.
     */
    public static SmsMessageBase.SubmitPduBase getSubmitPdu(boolean isCdma, String scAddr,
            String destAddr, String message, boolean statusReportRequested, SmsHeader smsHeader,
            int priority, int validityPeriod) {
        if (isCdma) {
            return getSubmitPduCdma(scAddr, destAddr, message, statusReportRequested, smsHeader,
                    priority);
        } else {
            return getSubmitPduGsm(scAddr, destAddr, message, statusReportRequested,
                    validityPeriod);
        }
    }

    /**
     * Gsm implementation for
     * {@link #getSubmitPdu(boolean, String, String, String, boolean)}
     *
     * @param scAddr is the service center address or null to use the current default SMSC
     * @param destAddr the address to send the message to
     * @param message the body of the message.
     * @param statusReportRequested whether or not a status report is requested.
     * @return the submit pdu.
     */
    public static SmsMessageBase.SubmitPduBase getSubmitPduGsm(String scAddr, String destAddr,
            String message, boolean statusReportRequested) {
        return com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddr, destAddr, message,
                statusReportRequested);
    }

    /**
     * Gsm implementation for
     * {@link #getSubmitPdu(boolean, String, String, String, boolean, int)}
     *
     * @param scAddr is the service center address or null to use the current default SMSC
     * @param destAddr the address to send the message to
     * @param message the body of the message.
     * @param statusReportRequested whether or not a status report is requested.
     * @param validityPeriod Validity Period of the message in mins.
     *  Refer specification 3GPP TS 23.040 V6.8.1 section 9.2.3.12.1.
     *  Validity Period(Minimum) -> 5 mins
     *  Validity Period(Maximum) -> 635040 mins(i.e.63 weeks).
     *  Any Other values included Negative considered as Invalid Validity Period of the message.
     * @return the submit pdu.
     */
    public static SmsMessageBase.SubmitPduBase getSubmitPduGsm(String scAddr, String destAddr,
            String message, boolean statusReportRequested, int validityPeriod) {
        return com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddr, destAddr, message,
                statusReportRequested, validityPeriod);
    }

    /**
     * Cdma implementation for
     * {@link #getSubmitPdu(boolean, String, String, String, boolean, SmsHeader)}
     *
     *  @param scAddr is the service center address or null to use the current default SMSC
     * @param destAddr the address to send the message to
     * @param message the body of the message.
     * @param statusReportRequested whether or not a status report is requested.
     * @param smsHeader message header.
     * @return the submit pdu.
     */
    public static SmsMessageBase.SubmitPduBase getSubmitPduCdma(String scAddr, String destAddr,
            String message, boolean statusReportRequested, SmsHeader smsHeader) {
        return com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddr, destAddr,
                message, statusReportRequested, smsHeader);
    }

    /**
     * Cdma implementation for
     * {@link #getSubmitPdu(boolean, String, String, String, boolean, SmsHeader)}
     *
     *  @param scAddr is the service center address or null to use the current default SMSC
     * @param destAddr the address to send the message to
     * @param message the body of the message.
     * @param statusReportRequested whether or not a status report is requested.
     * @param smsHeader message header.
     * @param priority Priority level of the message
     *  Refer specification See 3GPP2 C.S0015-B, v2.0, table 4.5.9-1
     *  ---------------------------------
     *  PRIORITY      | Level of Priority
     *  ---------------------------------
     *      '00'      |     Normal
     *      '01'      |     Interactive
     *      '10'      |     Urgent
     *      '11'      |     Emergency
     *  ----------------------------------
     *  Any Other values included Negative considered as Invalid Priority Indicator of the message.
     * @return the submit pdu.
     */
    public static SmsMessageBase.SubmitPduBase getSubmitPduCdma(String scAddr, String destAddr,
            String message, boolean statusReportRequested, SmsHeader smsHeader, int priority) {
        return com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddr, destAddr,
                message, statusReportRequested, smsHeader, priority);
    }

    /**
     * Trigger the proper implementation for getting submit pdu for data sms based on format.
     *
     * @param isCdma true if cdma format should be used.
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use the current default SMSC
     * @param destPort the port to deliver the message to
     * @param message the body of the message to send
     * @param statusReportRequested whether or not a status report is requested.
     * @return the submit pdu.
     */
    public static SmsMessageBase.SubmitPduBase getSubmitPdu(boolean isCdma, String scAddr,
            String destAddr, int destPort, byte[] message, boolean statusReportRequested) {
        if (isCdma) {
            return getSubmitPduCdma(scAddr, destAddr, destPort, message, statusReportRequested);
        } else {
            return getSubmitPduGsm(scAddr, destAddr, destPort, message, statusReportRequested);
        }
    }

    /**
     * Cdma implementation of {@link #getSubmitPdu(boolean, String, String, int, byte[], boolean)}

     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use the current default SMSC
     * @param destPort the port to deliver the message to
     * @param message the body of the message to send
     * @param statusReportRequested whether or not a status report is requested.
     * @return the submit pdu.
     */
    public static SmsMessageBase.SubmitPduBase getSubmitPduCdma(String scAddr, String destAddr,
            int destPort, byte[] message, boolean statusReportRequested) {
        return com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddr, destAddr,
                destPort, message, statusReportRequested);
    }

    /**
     * Gsm implementation of {@link #getSubmitPdu(boolean, String, String, int, byte[], boolean)}
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use the current default SMSC
     * @param destPort the port to deliver the message to
     * @param message the body of the message to send
     * @param statusReportRequested whether or not a status report is requested.
     * @return the submit pdu.
     */
    public static SmsMessageBase.SubmitPduBase getSubmitPduGsm(String scAddr, String destAddr,
            int destPort, byte[] message, boolean statusReportRequested) {
        return com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddr, destAddr,
                destPort, message, statusReportRequested);

    }

    /**
     * Calculate the number of septets needed to encode the message. This function should only be
     * called for individual segments of multipart message.
     *
     * @param isCdma  true if cdma format should be used.
     * @param messageBody the message to encode
     * @param use7bitOnly ignore (but still count) illegal characters if true
     * @return TextEncodingDetails
     */
    public static TextEncodingDetails calculateLength(boolean isCdma, CharSequence messageBody,
            boolean use7bitOnly) {
        if (isCdma) {
            return calculateLengthCdma(messageBody, use7bitOnly);
        } else {
            return calculateLengthGsm(messageBody, use7bitOnly);
        }
    }

    /**
     * Gsm implementation for {@link #calculateLength(boolean, CharSequence, boolean)}
     *
     * @param messageBody the message to encode
     * @param use7bitOnly ignore (but still count) illegal characters if true
     * @return TextEncodingDetails
     */
    public static TextEncodingDetails calculateLengthGsm(CharSequence messageBody,
            boolean use7bitOnly) {
        return com.android.internal.telephony.gsm.SmsMessage.calculateLength(messageBody,
                use7bitOnly);

    }

    /**
     * Cdma implementation for {@link #calculateLength(boolean, CharSequence, boolean)}
     *
     * @param messageBody the message to encode
     * @param use7bitOnly ignore (but still count) illegal characters if true
     * @return TextEncodingDetails
     */
    public static TextEncodingDetails calculateLengthCdma(CharSequence messageBody,
            boolean use7bitOnly) {
        return com.android.internal.telephony.cdma.SmsMessage.calculateLength(messageBody,
                use7bitOnly, false);
    }
}

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

package com.android.internal.telephony;

import static android.telephony.SmsManager.STATUS_ON_ICC_FREE;
import static android.telephony.SmsManager.STATUS_ON_ICC_READ;
import static android.telephony.SmsManager.STATUS_ON_ICC_UNREAD;

import android.Manifest;
import android.annotation.RequiresPermission;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Telephony;
import android.telephony.SmsCbMessage;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.emergency.EmergencyNumber;
import android.util.LocalLog;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.internal.util.HexDump;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IccSmsInterfaceManager to provide an inter-process communication to
 * access Sms in Icc.
 */
public class IccSmsInterfaceManager {
    static final String LOG_TAG = "IccSmsInterfaceManager";
    static final boolean DBG = true;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private CellBroadcastRangeManager mCellBroadcastRangeManager =
            new CellBroadcastRangeManager();
    private CdmaBroadcastRangeManager mCdmaBroadcastRangeManager =
            new CdmaBroadcastRangeManager();

    private static final int EVENT_LOAD_DONE = 1;
    private static final int EVENT_UPDATE_DONE = 2;
    protected static final int EVENT_SET_BROADCAST_ACTIVATION_DONE = 3;
    protected static final int EVENT_SET_BROADCAST_CONFIG_DONE = 4;
    private static final int EVENT_GET_SMSC_DONE = 5;
    private static final int EVENT_SET_SMSC_DONE = 6;
    private static final int SMS_CB_CODE_SCHEME_MIN = 0;
    private static final int SMS_CB_CODE_SCHEME_MAX = 255;
    public static final int SMS_MESSAGE_PRIORITY_NOT_SPECIFIED = -1;
    public static final int SMS_MESSAGE_PERIOD_NOT_SPECIFIED = -1;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected Phone mPhone;
    @UnsupportedAppUsage
    final protected Context mContext;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    final protected AppOpsManager mAppOps;
    @VisibleForTesting
    public SmsDispatchersController mDispatchersController;
    private SmsPermissions mSmsPermissions;

    private final LocalLog mCellBroadcastLocalLog = new LocalLog(64);

    private static final class Request {
        AtomicBoolean mStatus = new AtomicBoolean(false);
        Object mResult = null;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            Request request = (Request) ar.userObj;

            switch (msg.what) {
                case EVENT_UPDATE_DONE:
                case EVENT_SET_BROADCAST_ACTIVATION_DONE:
                case EVENT_SET_BROADCAST_CONFIG_DONE:
                case EVENT_SET_SMSC_DONE:
                    notifyPending(request, ar.exception == null);
                    break;
                case EVENT_LOAD_DONE:
                    List<SmsRawData> smsRawDataList = null;
                    if (ar.exception == null) {
                        smsRawDataList = buildValidRawData((ArrayList<byte[]>) ar.result);
                        //Mark SMS as read after importing it from card.
                        markMessagesAsRead((ArrayList<byte[]>) ar.result);
                    } else {
                        if (Rlog.isLoggable("SMS", Log.DEBUG)) {
                            loge("Cannot load Sms records");
                        }
                    }
                    notifyPending(request, smsRawDataList);
                    break;
                case EVENT_GET_SMSC_DONE:
                    String smsc = null;
                    if (ar.exception == null) {
                        smsc = (String) ar.result;
                    } else {
                        loge("Cannot read SMSC");
                    }
                    notifyPending(request, smsc);
                    break;
            }
        }

        private void notifyPending(Request request, Object result) {
            if (request != null) {
                synchronized (request) {
                    request.mResult = result;
                    request.mStatus.set(true);
                    request.notifyAll();
                }
            }
        }
    };

    protected IccSmsInterfaceManager(Phone phone) {
        this(phone, phone.getContext(),
                (AppOpsManager) phone.getContext().getSystemService(Context.APP_OPS_SERVICE),
                new SmsDispatchersController(
                        phone, phone.mSmsStorageMonitor, phone.mSmsUsageMonitor),
                new SmsPermissions(phone, phone.getContext(),
                        (AppOpsManager) phone.getContext().getSystemService(
                                Context.APP_OPS_SERVICE)));
    }

    @VisibleForTesting
    public IccSmsInterfaceManager(
            Phone phone, Context context, AppOpsManager appOps,
            SmsDispatchersController dispatchersController, SmsPermissions smsPermissions) {
        mPhone = phone;
        mContext = context;
        mAppOps = appOps;
        mDispatchersController = dispatchersController;
        mSmsPermissions = smsPermissions;
    }

    private void enforceNotOnHandlerThread(String methodName) {
        if (Looper.myLooper() == mHandler.getLooper()) {
            throw new RuntimeException("This method " + methodName + " will deadlock if called from"
                    + " the handler's thread.");
        }
    }

    protected void markMessagesAsRead(ArrayList<byte[]> messages) {
        if (messages == null) {
            return;
        }

        //IccFileHandler can be null, if icc card is absent.
        IccFileHandler fh = mPhone.getIccFileHandler();
        if (fh == null) {
            //shouldn't really happen, as messages are marked as read, only
            //after importing it from icc.
            if (Rlog.isLoggable("SMS", Log.DEBUG)) {
                loge("markMessagesAsRead - aborting, no icc card present.");
            }
            return;
        }

        int count = messages.size();

        for (int i = 0; i < count; i++) {
            byte[] ba = messages.get(i);
            if ((ba[0] & 0x07) == STATUS_ON_ICC_UNREAD) {
                int n = ba.length;
                byte[] nba = new byte[n - 1];
                System.arraycopy(ba, 1, nba, 0, n - 1);
                byte[] record = makeSmsRecordData(STATUS_ON_ICC_READ, nba);
                fh.updateEFLinearFixed(IccConstants.EF_SMS, i + 1, record, null, null);
                if (Rlog.isLoggable("SMS", Log.DEBUG)) {
                    log("SMS " + (i + 1) + " marked as read");
                }
            }
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void enforceReceiveAndSend(String message) {
        mContext.enforceCallingOrSelfPermission(
                Manifest.permission.RECEIVE_SMS, message);
        mContext.enforceCallingOrSelfPermission(
                Manifest.permission.SEND_SMS, message);
    }

    /**
     * Enforce the permission for access messages on ICC
     */
    private void enforceAccessMessageOnICC(String message) {
        mContext.enforceCallingOrSelfPermission(
                Manifest.permission.ACCESS_MESSAGES_ON_ICC, message);
    }

    /**
     * Update the specified message on the Icc.
     *
     * @param index record index of message to update
     * @param status new message status (STATUS_ON_ICC_READ,
     *                  STATUS_ON_ICC_UNREAD, STATUS_ON_ICC_SENT,
     *                  STATUS_ON_ICC_UNSENT, STATUS_ON_ICC_FREE)
     * @param pdu the raw PDU to store
     * @return success or not
     *
     */

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean
    updateMessageOnIccEf(String callingPackage, int index, int status, byte[] pdu) {
        if (DBG) log("updateMessageOnIccEf: index=" + index +
                " status=" + status + " ==> " +
                "("+ Arrays.toString(pdu) + ")");
        enforceReceiveAndSend("Updating message on Icc");
        enforceAccessMessageOnICC("Updating message on Icc");
        enforceNotOnHandlerThread("updateMessageOnIccEf");
        if (mAppOps.noteOp(AppOpsManager.OPSTR_WRITE_ICC_SMS, Binder.getCallingUid(),
                callingPackage) != AppOpsManager.MODE_ALLOWED) {
            return false;
        }
        Request updateRequest = new Request();
        synchronized (updateRequest) {
            Message response = mHandler.obtainMessage(EVENT_UPDATE_DONE, updateRequest);

            if ((status & 0x01) == STATUS_ON_ICC_FREE) {
                // RIL_REQUEST_DELETE_SMS_ON_SIM vs RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM
                // Special case FREE: call deleteSmsOnSim/Ruim instead of
                // manipulating the record
                // Will eventually fail if icc card is not present.
                if (PhoneConstants.PHONE_TYPE_GSM == mPhone.getPhoneType()) {
                    mPhone.mCi.deleteSmsOnSim(index, response);
                } else {
                    mPhone.mCi.deleteSmsOnRuim(index, response);
                }
            } else {
                //IccFilehandler can be null if ICC card is not present.
                IccFileHandler fh = mPhone.getIccFileHandler();
                if (fh == null) {
                    response.recycle();
                    return false; /* is false */
                }
                byte[] record = makeSmsRecordData(status, pdu);
                fh.updateEFLinearFixed(
                        IccConstants.EF_SMS,
                        index, record, null, response);
            }
            waitForResult(updateRequest);
        }
        return (boolean) updateRequest.mResult;
    }

    /**
     * Copies a raw SMS PDU to the ICC.
     *
     * @param callingPackage the package name of the calling app.
     * @param status message status. One of these status:
     *               <code>STATUS_ON_ICC_READ</code>
     *               <code>STATUS_ON_ICC_UNREAD</code>
     *               <code>STATUS_ON_ICC_SENT</code>
     *               <code>STATUS_ON_ICC_UNSENT</code>
     * @param pdu the raw PDU to store.
     * @param smsc the SMSC for this message. Null means use default.
     * @return true for success. Otherwise false.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean copyMessageToIccEf(String callingPackage, int status, byte[] pdu, byte[] smsc) {
        //NOTE smsc not used in RUIM
        if (DBG) log("copyMessageToIccEf: status=" + status + " ==> " +
                "pdu=("+ Arrays.toString(pdu) +
                "), smsc=(" + Arrays.toString(smsc) +")");
        enforceReceiveAndSend("Copying message to Icc");
        enforceNotOnHandlerThread("copyMessageToIccEf");
        if (mAppOps.noteOp(AppOpsManager.OPSTR_WRITE_ICC_SMS, Binder.getCallingUid(),
                callingPackage) != AppOpsManager.MODE_ALLOWED) {
            return false;
        }
        Request copyRequest = new Request();
        synchronized (copyRequest) {
            Message response = mHandler.obtainMessage(EVENT_UPDATE_DONE, copyRequest);

            //RIL_REQUEST_WRITE_SMS_TO_SIM vs RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM
            if (PhoneConstants.PHONE_TYPE_GSM == mPhone.getPhoneType()) {
                mPhone.mCi.writeSmsToSim(status, IccUtils.bytesToHexString(smsc),
                        IccUtils.bytesToHexString(pdu), response);
            } else {
                mPhone.mCi.writeSmsToRuim(status, pdu, response);
            }

            waitForResult(copyRequest);
        }
        return (boolean) copyRequest.mResult;
    }

    /**
     * Retrieves all messages currently stored on Icc.
     *
     * @return list of SmsRawData of all sms on Icc
     */

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public List<SmsRawData> getAllMessagesFromIccEf(String callingPackage) {
        if (DBG) log("getAllMessagesFromEF");

        mContext.enforceCallingOrSelfPermission(
                Manifest.permission.RECEIVE_SMS,
                "Reading messages from Icc");
        enforceAccessMessageOnICC("Reading messages from Icc");
        enforceNotOnHandlerThread("getAllMessagesFromIccEf");
        if (mAppOps.noteOp(AppOpsManager.OPSTR_READ_ICC_SMS, Binder.getCallingUid(),
                callingPackage) != AppOpsManager.MODE_ALLOWED) {
            return new ArrayList<SmsRawData>();
        }
        Request getRequest = new Request();
        synchronized (getRequest) {

            IccFileHandler fh = mPhone.getIccFileHandler();
            if (fh == null) {
                loge("Cannot load Sms records. No icc card?");
                return null;
            }

            Message response = mHandler.obtainMessage(EVENT_LOAD_DONE, getRequest);
            fh.loadEFLinearFixedAll(IccConstants.EF_SMS, response);

            waitForResult(getRequest);
        }
        return (List<SmsRawData>) getRequest.mResult;
    }

    /**
     * A permissions check before passing to {@link IccSmsInterfaceManager#sendDataInternal}.
     * This method checks if the calling package or itself has the permission to send the data sms.
     */
    public void sendDataWithSelfPermissions(String callingPackage, String callingAttributionTag,
            String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent,
            PendingIntent deliveryIntent, boolean isForVvm) {
        if (!mSmsPermissions.checkCallingOrSelfCanSendSms(callingPackage, callingAttributionTag,
                "Sending SMS message")) {
            returnUnspecifiedFailure(sentIntent);
            return;
        }
        sendDataInternal(callingPackage, destAddr, scAddr, destPort, data, sentIntent,
                deliveryIntent, isForVvm);
    }

    /**
     * @deprecated Use {@link #sendData(String, String, String, String, int, byte[], PendingIntent,
     * PendingIntent)} instead.
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void sendData(String callingPackage, String destAddr, String scAddr, int destPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        sendData(callingPackage, null, destAddr, scAddr, destPort, data,
                sentIntent, deliveryIntent);
    }

    /**
     * A permissions check before passing to {@link IccSmsInterfaceManager#sendDataInternal}.
     * This method checks only if the calling package has the permission to send the data sms.
     */
    public void sendData(String callingPackage, String callingAttributionTag,
            String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        if (!mSmsPermissions.checkCallingCanSendSms(callingPackage, callingAttributionTag,
                "Sending SMS message")) {
            returnUnspecifiedFailure(sentIntent);
            return;
        }
        sendDataInternal(callingPackage, destAddr, scAddr, destPort, data, sentIntent,
                deliveryIntent, false /* isForVvm */);
    }

    /**
     * Send a data based SMS to a specific application port.
     *
     * @param callingPackage the package name of the calling app
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param destPort the port to deliver the message to
     * @param data the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     */

    private void sendDataInternal(String callingPackage, String destAddr, String scAddr,
            int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent,
            boolean isForVvm) {
        if (Rlog.isLoggable("SMS", Log.VERBOSE)) {
            log("sendData: destAddr=" + destAddr + " scAddr=" + scAddr + " destPort="
                    + destPort + " data='" + HexDump.toHexString(data)  + "' sentIntent="
                    + sentIntent + " deliveryIntent=" + deliveryIntent + " isForVVM=" + isForVvm);
        }
        destAddr = filterDestAddress(destAddr);
        mDispatchersController.sendData(callingPackage, destAddr, scAddr, destPort, data,
                sentIntent, deliveryIntent, isForVvm);
    }

    /**
     * A permissions check before passing to {@link IccSmsInterfaceManager#sendTextInternal}.
     * This method checks only if the calling package has the permission to send the sms.
     * Note: SEND_SMS permission should be checked by the caller of this method
     */
    public void sendText(String callingPackage, String destAddr, String scAddr,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent,
            boolean persistMessageForNonDefaultSmsApp, long messageId) {
        sendTextInternal(callingPackage, destAddr, scAddr, text, sentIntent, deliveryIntent,
                persistMessageForNonDefaultSmsApp, SMS_MESSAGE_PRIORITY_NOT_SPECIFIED,
                false /* expectMore */, SMS_MESSAGE_PERIOD_NOT_SPECIFIED, false /* isForVvm */,
                messageId);
    }

    /**
     * A permissions check before passing to {@link IccSmsInterfaceManager#sendTextInternal}.
     * This method checks if the calling package or itself has the permission to send the sms.
     */
    public void sendTextWithSelfPermissions(String callingPackage, String callingAttributeTag,
            String destAddr, String scAddr, String text, PendingIntent sentIntent,
            PendingIntent deliveryIntent, boolean persistMessage, boolean isForVvm) {
        if (!mSmsPermissions.checkCallingOrSelfCanSendSms(callingPackage, callingAttributeTag,
                "Sending SMS message")) {
            returnUnspecifiedFailure(sentIntent);
            return;
        }
        sendTextInternal(callingPackage, destAddr, scAddr, text, sentIntent, deliveryIntent,
                persistMessage, SMS_MESSAGE_PRIORITY_NOT_SPECIFIED, false /* expectMore */,
                SMS_MESSAGE_PERIOD_NOT_SPECIFIED, isForVvm, 0L /* messageId */);
    }

    /**
     * Send a text based SMS.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @param persistMessageForNonDefaultSmsApp whether the sent message should
     *  be automatically persisted in the SMS db. It only affects messages sent
     *  by a non-default SMS app. Currently only the carrier app can set this
     *  parameter to false to skip auto message persistence.
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
     *  Any Other values including negative considered as Invalid Priority Indicator of the message.
     * @param expectMore is a boolean to indicate the sending messages through same link or not.
     * @param validityPeriod Validity Period of the message in mins.
     *  Refer specification 3GPP TS 23.040 V6.8.1 section 9.2.3.12.1.
     *  Validity Period(Minimum) -> 5 mins
     *  Validity Period(Maximum) -> 635040 mins(i.e.63 weeks).
     *  Any Other values including negative considered as Invalid Validity Period of the message.
     * @param messageId An id that uniquely identifies the message requested to be sent.
     *                 Used for logging and diagnostics purposes. The id may be 0.
     */

    private void sendTextInternal(String callingPackage, String destAddr, String scAddr,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent,
            boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore,
            int validityPeriod, boolean isForVvm, long messageId) {
        if (Rlog.isLoggable("SMS", Log.VERBOSE)) {
            log("sendText: destAddr=" + destAddr + " scAddr=" + scAddr
                    + " text='" + text + "' sentIntent=" + sentIntent + " deliveryIntent="
                    + deliveryIntent + " priority=" + priority + " expectMore=" + expectMore
                    + " validityPeriod=" + validityPeriod + " isForVVM=" + isForVvm
                    + " " + SmsController.formatCrossStackMessageId(messageId));
        }
        notifyIfOutgoingEmergencySms(destAddr);
        destAddr = filterDestAddress(destAddr);
        mDispatchersController.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent,
                null/*messageUri*/, callingPackage, persistMessageForNonDefaultSmsApp,
                priority, expectMore, validityPeriod, isForVvm, messageId);
    }

    /**
     * Send a text based SMS with Messaging Options.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @param persistMessageForNonDefaultSmsApp whether the sent message should
     *  be automatically persisted in the SMS db. It only affects messages sent
     *  by a non-default SMS app. Currently only the carrier app can set this
     *  parameter to false to skip auto message persistence.
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
     *  Any Other values including negative considered as Invalid Priority Indicator of the message.
     * @param expectMore is a boolean to indicate the sending messages through same link or not.
     * @param validityPeriod Validity Period of the message in mins.
     *  Refer specification 3GPP TS 23.040 V6.8.1 section 9.2.3.12.1.
     *  Validity Period(Minimum) -> 5 mins
     *  Validity Period(Maximum) -> 635040 mins(i.e.63 weeks).
     *  Any Other values including negative considered as Invalid Validity Period of the message.
     */

    public void sendTextWithOptions(String callingPackage, String callingAttributionTag,
            String destAddr, String scAddr, String text, PendingIntent sentIntent,
            PendingIntent deliveryIntent, boolean persistMessageForNonDefaultSmsApp, int priority,
            boolean expectMore, int validityPeriod) {
        if (!mSmsPermissions.checkCallingCanSendText(persistMessageForNonDefaultSmsApp,
                    callingPackage, callingAttributionTag, "Sending SMS message")) {
            returnUnspecifiedFailure(sentIntent);
            return;
        }
        sendTextInternal(callingPackage, destAddr, scAddr, text, sentIntent, deliveryIntent,
                persistMessageForNonDefaultSmsApp, priority, expectMore, validityPeriod,
                false /* isForVvm */, 0L /* messageId */);
    }

    /**
     * Inject an SMS PDU into the android application framework.
     *
     * @param pdu is the byte array of pdu to be injected into android application framework
     * @param format is the format of SMS pdu (3gpp or 3gpp2)
     * @param receivedIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully received by the
     *  android application framework. This intent is broadcasted at
     *  the same time an SMS received from radio is acknowledged back.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void injectSmsPdu(byte[] pdu, String format, PendingIntent receivedIntent) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            mSmsPermissions.enforceCallerIsImsAppOrCarrierApp("injectSmsPdu");
        }

        if (Rlog.isLoggable("SMS", Log.VERBOSE)) {
            log("pdu: " + pdu +
                "\n format=" + format +
                "\n receivedIntent=" + receivedIntent);
        }
        mDispatchersController.injectSmsPdu(pdu, format, false /* isOverIms */,
                result -> {
                    if (receivedIntent != null) {
                        try {
                            receivedIntent.send(result);
                        } catch (PendingIntent.CanceledException e) {
                            loge("receivedIntent cancelled.");
                        }
                    }
                }
        );
    }

    /**
     * Send a multi-part text based SMS.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code>
     *   <code>RESULT_ERROR_RADIO_OFF</code>
     *   <code>RESULT_ERROR_NULL_PDU</code>.
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     * @param messageId An id that uniquely identifies the message requested to be sent.
     *                 Used for logging and diagnostics purposes. The id may be 0.
     */

    public void sendMultipartText(String callingPackage, String callingAttributionTag,
            String destAddr, String scAddr, List<String> parts, List<PendingIntent> sentIntents,
            List<PendingIntent> deliveryIntents, boolean persistMessageForNonDefaultSmsApp,
            long messageId) {
        sendMultipartTextWithOptions(callingPackage, callingAttributionTag, destAddr, scAddr, parts,
                sentIntents, deliveryIntents, persistMessageForNonDefaultSmsApp,
                SMS_MESSAGE_PRIORITY_NOT_SPECIFIED, false /* expectMore */,
                SMS_MESSAGE_PERIOD_NOT_SPECIFIED,
                messageId);
    }

    /**
     * Send a multi-part text based SMS with Messaging Options.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code>
     *   <code>RESULT_ERROR_RADIO_OFF</code>
     *   <code>RESULT_ERROR_NULL_PDU</code>.
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     * @param persistMessageForNonDefaultSmsApp whether the sent message should
     *   be automatically persisted in the SMS db. It only affects messages sent
     *   by a non-default SMS app. Currently only the carrier app can set this
     *   parameter to false to skip auto message persistence.
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
     *  Any Other values including negative considered as Invalid Priority Indicator of the message.
     * @param expectMore is a boolean to indicate the sending messages through same link or not.
     * @param validityPeriod Validity Period of the message in mins.
     *  Refer specification 3GPP TS 23.040 V6.8.1 section 9.2.3.12.1.
     *  Validity Period(Minimum) -> 5 mins
     *  Validity Period(Maximum) -> 635040 mins(i.e.63 weeks).
     *  Any Other values including negative considered as Invalid Validity Period of the message.
     * @param messageId An id that uniquely identifies the message requested to be sent.
     *                 Used for logging and diagnostics purposes. The id may be 0.
     */

    public void sendMultipartTextWithOptions(String callingPackage, String callingAttributionTag,
            String destAddr, String scAddr, List<String> parts, List<PendingIntent> sentIntents,
            List<PendingIntent> deliveryIntents, boolean persistMessageForNonDefaultSmsApp,
            int priority, boolean expectMore, int validityPeriod, long messageId) {
        if (!mSmsPermissions.checkCallingCanSendText(persistMessageForNonDefaultSmsApp,
                callingPackage, callingAttributionTag, "Sending SMS message")) {
            returnUnspecifiedFailure(sentIntents);
            return;
        }
        if (Rlog.isLoggable("SMS", Log.VERBOSE)) {
            int i = 0;
            for (String part : parts) {
                log("sendMultipartTextWithOptions: destAddr=" + destAddr + ", srAddr=" + scAddr
                        + ", part[" + (i++) + "]=" + part
                        + " " + SmsController.formatCrossStackMessageId(messageId));
            }
        }
        notifyIfOutgoingEmergencySms(destAddr);
        destAddr = filterDestAddress(destAddr);

        if (parts.size() > 1 && parts.size() < 10 && !SmsMessage.hasEmsSupport()) {
            for (int i = 0; i < parts.size(); i++) {
                // If EMS is not supported, we have to break down EMS into single segment SMS
                // and add page info " x/y".
                String singlePart = parts.get(i);
                if (SmsMessage.shouldAppendPageNumberAsPrefix()) {
                    singlePart = String.valueOf(i + 1) + '/' + parts.size() + ' ' + singlePart;
                } else {
                    singlePart = singlePart.concat(' ' + String.valueOf(i + 1) + '/'
                            + parts.size());
                }

                PendingIntent singleSentIntent = null;
                if (sentIntents != null && sentIntents.size() > i) {
                    singleSentIntent = sentIntents.get(i);
                }

                PendingIntent singleDeliveryIntent = null;
                if (deliveryIntents != null && deliveryIntents.size() > i) {
                    singleDeliveryIntent = deliveryIntents.get(i);
                }

                mDispatchersController.sendText(destAddr, scAddr, singlePart, singleSentIntent,
                        singleDeliveryIntent, null /* messageUri */, callingPackage,
                        persistMessageForNonDefaultSmsApp, priority, expectMore, validityPeriod,
                        false /* isForVvm */, messageId);
            }
            return;
        }

        mDispatchersController.sendMultipartText(destAddr,
                                      scAddr,
                                      (ArrayList<String>) parts,
                                      (ArrayList<PendingIntent>) sentIntents,
                                      (ArrayList<PendingIntent>) deliveryIntents,
                                      null, callingPackage, persistMessageForNonDefaultSmsApp,
                                          priority, expectMore, validityPeriod, messageId);

    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int getPremiumSmsPermission(String packageName) {
        return mDispatchersController.getPremiumSmsPermission(packageName);
    }


    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void setPremiumSmsPermission(String packageName, int permission) {
        mDispatchersController.setPremiumSmsPermission(packageName, permission);
    }

    /**
     * create SmsRawData lists from all sms record byte[]
     * Use null to indicate "free" record
     *
     * @param messages List of message records from EF_SMS.
     * @return SmsRawData list of all in-used records
     */
    protected ArrayList<SmsRawData> buildValidRawData(ArrayList<byte[]> messages) {
        int count = messages.size();
        ArrayList<SmsRawData> ret;

        ret = new ArrayList<SmsRawData>(count);

        for (int i = 0; i < count; i++) {
            byte[] ba = messages.get(i);
            if ((ba[0] & 0x01) == STATUS_ON_ICC_FREE) {
                ret.add(null);
            } else {
                ret.add(new SmsRawData(messages.get(i)));
            }
        }

        return ret;
    }

    /**
     * Generates an EF_SMS record from status and raw PDU.
     *
     * @param status Message status.  See TS 51.011 10.5.3.
     * @param pdu Raw message PDU.
     * @return byte array for the record.
     */
    protected byte[] makeSmsRecordData(int status, byte[] pdu) {
        byte[] data;
        if (PhoneConstants.PHONE_TYPE_GSM == mPhone.getPhoneType()) {
            data = new byte[SmsManager.SMS_RECORD_LENGTH];
        } else {
            data = new byte[SmsManager.CDMA_SMS_RECORD_LENGTH];
        }

        // Status bits for this record.  See TS 51.011 10.5.3
        data[0] = (byte) (status & 0x07);

        System.arraycopy(pdu, 0, data, 1, pdu.length);

        // Pad out with 0xFF's.
        for (int j = pdu.length+1; j < data.length; j++) {
            data[j] = -1;
        }

        return data;
    }

    /**
     * Gets the SMSC address from (U)SIM.
     *
     * @return the SMSC address string, null if failed.
     */
    public String getSmscAddressFromIccEf(String callingPackage) {
        if (!mSmsPermissions.checkCallingOrSelfCanGetSmscAddress(
                callingPackage, "getSmscAddressFromIccEf")) {
            return null;
        }
        enforceNotOnHandlerThread("getSmscAddressFromIccEf");
        Request getRequest = new Request();
        synchronized (getRequest) {
            Message response = mHandler.obtainMessage(EVENT_GET_SMSC_DONE, getRequest);
            mPhone.mCi.getSmscAddress(response);
            waitForResult(getRequest);
        }
        return (String) getRequest.mResult;
    }

    /**
     * Sets the SMSC address on (U)SIM.
     *
     * @param smsc the SMSC address string.
     * @return true for success, false otherwise.
     */
    public boolean setSmscAddressOnIccEf(String callingPackage, String smsc) {
        if (!mSmsPermissions.checkCallingOrSelfCanSetSmscAddress(
                callingPackage, "setSmscAddressOnIccEf")) {
            return false;
        }
        enforceNotOnHandlerThread("setSmscAddressOnIccEf");
        Request setRequest = new Request();
        synchronized (setRequest) {
            Message response = mHandler.obtainMessage(EVENT_SET_SMSC_DONE, setRequest);
            mPhone.mCi.setSmscAddress(smsc, response);
            waitForResult(setRequest);
        }
        return (boolean) setRequest.mResult;
    }

    public boolean enableCellBroadcast(int messageIdentifier, int ranType) {
        return enableCellBroadcastRange(messageIdentifier, messageIdentifier, ranType);
    }

    public boolean disableCellBroadcast(int messageIdentifier, int ranType) {
        return disableCellBroadcastRange(messageIdentifier, messageIdentifier, ranType);
    }

    public boolean enableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) {
        mContext.enforceCallingPermission(android.Manifest.permission.RECEIVE_EMERGENCY_BROADCAST,
                "enabling cell broadcast range [" + startMessageId + "-" + endMessageId + "]. "
                        + "ranType=" + ranType);
        if (ranType == SmsCbMessage.MESSAGE_FORMAT_3GPP) {
            return enableGsmBroadcastRange(startMessageId, endMessageId);
        } else if (ranType == SmsCbMessage.MESSAGE_FORMAT_3GPP2) {
            return enableCdmaBroadcastRange(startMessageId, endMessageId);
        } else {
            throw new IllegalArgumentException("Not a supported RAN Type");
        }
    }

    public boolean disableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) {
        mContext.enforceCallingPermission(android.Manifest.permission.RECEIVE_EMERGENCY_BROADCAST,
                "disabling cell broadcast range [" + startMessageId + "-" + endMessageId
                        + "]. ranType=" + ranType);
        if (ranType == SmsCbMessage.MESSAGE_FORMAT_3GPP) {
            return disableGsmBroadcastRange(startMessageId, endMessageId);
        } else if (ranType == SmsCbMessage.MESSAGE_FORMAT_3GPP2)  {
            return disableCdmaBroadcastRange(startMessageId, endMessageId);
        } else {
            throw new IllegalArgumentException("Not a supported RAN Type");
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    synchronized public boolean enableGsmBroadcastRange(int startMessageId, int endMessageId) {

        mContext.enforceCallingPermission(android.Manifest.permission.RECEIVE_EMERGENCY_BROADCAST,
                "Enabling cell broadcast SMS");

        String client = mContext.getPackageManager().getNameForUid(
                Binder.getCallingUid());

        String msg;
        if (!mCellBroadcastRangeManager.enableRange(startMessageId, endMessageId, client)) {
            msg = "Failed to add GSM cell broadcast channels range " + startMessageId
                    + " to " + endMessageId;
            log(msg);
            mCellBroadcastLocalLog.log(msg);
            return false;
        }

        if (DBG) {
            msg = "Added GSM cell broadcast channels range " + startMessageId
                    + " to " + endMessageId;
            log(msg);
            mCellBroadcastLocalLog.log(msg);
        }

        setCellBroadcastActivation(!mCellBroadcastRangeManager.isEmpty());

        return true;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    synchronized public boolean disableGsmBroadcastRange(int startMessageId, int endMessageId) {

        mContext.enforceCallingPermission(android.Manifest.permission.RECEIVE_EMERGENCY_BROADCAST,
                "Disabling cell broadcast SMS");

        String client = mContext.getPackageManager().getNameForUid(
                Binder.getCallingUid());

        String msg;
        if (!mCellBroadcastRangeManager.disableRange(startMessageId, endMessageId, client)) {
            msg = "Failed to remove GSM cell broadcast channels range " + startMessageId
                    + " to " + endMessageId;
            log(msg);
            mCellBroadcastLocalLog.log(msg);
            return false;
        }

        if (DBG) {
            msg = "Removed GSM cell broadcast channels range " + startMessageId
                    + " to " + endMessageId;
            log(msg);
            mCellBroadcastLocalLog.log(msg);
        }

        setCellBroadcastActivation(!mCellBroadcastRangeManager.isEmpty());

        return true;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    synchronized public boolean enableCdmaBroadcastRange(int startMessageId, int endMessageId) {

        mContext.enforceCallingPermission(android.Manifest.permission.RECEIVE_EMERGENCY_BROADCAST,
                "Enabling cdma broadcast SMS");

        String client = mContext.getPackageManager().getNameForUid(
                Binder.getCallingUid());

        String msg;
        if (!mCdmaBroadcastRangeManager.enableRange(startMessageId, endMessageId, client)) {
            msg = "Failed to add cdma broadcast channels range " + startMessageId + " to "
                    + endMessageId;
            log(msg);
            mCellBroadcastLocalLog.log(msg);
            return false;
        }

        if (DBG) {
            msg = "Added cdma broadcast channels range " + startMessageId + " to " + endMessageId;
            log(msg);
            mCellBroadcastLocalLog.log(msg);
        }

        setCdmaBroadcastActivation(!mCdmaBroadcastRangeManager.isEmpty());

        return true;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    synchronized public boolean disableCdmaBroadcastRange(int startMessageId, int endMessageId) {

        mContext.enforceCallingPermission(android.Manifest.permission.RECEIVE_EMERGENCY_BROADCAST,
                "Disabling cell broadcast SMS");

        String client = mContext.getPackageManager().getNameForUid(
                Binder.getCallingUid());

        String msg;
        if (!mCdmaBroadcastRangeManager.disableRange(startMessageId, endMessageId, client)) {
            msg = "Failed to remove cdma broadcast channels range " + startMessageId + " to "
                    + endMessageId;
            log(msg);
            mCellBroadcastLocalLog.log(msg);
            return false;
        }

        if (DBG) {
            msg = "Removed cdma broadcast channels range " + startMessageId + " to " + endMessageId;
            log(msg);
            mCellBroadcastLocalLog.log(msg);
        }

        setCdmaBroadcastActivation(!mCdmaBroadcastRangeManager.isEmpty());

        return true;
    }

    /**
     * Reset all cell broadcast ranges. Previously enabled ranges will become invalid after this.
     */
    @RequiresPermission(android.Manifest.permission.MODIFY_CELL_BROADCASTS)
    public void resetAllCellBroadcastRanges() {
        mContext.enforceCallingPermission(android.Manifest.permission.MODIFY_CELL_BROADCASTS,
                "resetAllCellBroadcastRanges");
        mCdmaBroadcastRangeManager.clearRanges();
        mCellBroadcastRangeManager.clearRanges();
        log("Cell broadcast ranges reset.");
    }

    class CellBroadcastRangeManager extends IntRangeManager {
        private ArrayList<SmsBroadcastConfigInfo> mConfigList =
                new ArrayList<SmsBroadcastConfigInfo>();

        /**
         * Called when the list of enabled ranges has changed. This will be
         * followed by zero or more calls to {@link #addRange} followed by
         * a call to {@link #finishUpdate}.
         */
        protected void startUpdate() {
            mConfigList.clear();
        }

        /**
         * Called after {@link #startUpdate} to indicate a range of enabled
         * values.
         * @param startId the first id included in the range
         * @param endId the last id included in the range
         */
        protected void addRange(int startId, int endId, boolean selected) {
            mConfigList.add(new SmsBroadcastConfigInfo(startId, endId,
                        SMS_CB_CODE_SCHEME_MIN, SMS_CB_CODE_SCHEME_MAX, selected));
        }

        /**
         * Called to indicate the end of a range update started by the
         * previous call to {@link #startUpdate}.
         * @return true if successful, false otherwise
         */
        protected boolean finishUpdate() {
            if (mConfigList.isEmpty()) {
                return true;
            } else {
                SmsBroadcastConfigInfo[] configs =
                        mConfigList.toArray(new SmsBroadcastConfigInfo[mConfigList.size()]);
                return setCellBroadcastConfig(configs);
            }
        }
    }

    class CdmaBroadcastRangeManager extends IntRangeManager {
        private ArrayList<CdmaSmsBroadcastConfigInfo> mConfigList =
                new ArrayList<CdmaSmsBroadcastConfigInfo>();

        /**
         * Called when the list of enabled ranges has changed. This will be
         * followed by zero or more calls to {@link #addRange} followed by a
         * call to {@link #finishUpdate}.
         */
        protected void startUpdate() {
            mConfigList.clear();
        }

        /**
         * Called after {@link #startUpdate} to indicate a range of enabled
         * values.
         * @param startId the first id included in the range
         * @param endId the last id included in the range
         */
        protected void addRange(int startId, int endId, boolean selected) {
            mConfigList.add(new CdmaSmsBroadcastConfigInfo(startId, endId,
                    1, selected));
        }

        /**
         * Called to indicate the end of a range update started by the previous
         * call to {@link #startUpdate}.
         * @return true if successful, false otherwise
         */
        protected boolean finishUpdate() {
            if (mConfigList.isEmpty()) {
                return true;
            } else {
                CdmaSmsBroadcastConfigInfo[] configs =
                        mConfigList.toArray(new CdmaSmsBroadcastConfigInfo[mConfigList.size()]);
                return setCdmaBroadcastConfig(configs);
            }
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean setCellBroadcastConfig(SmsBroadcastConfigInfo[] configs) {
        if (DBG) {
            log("Calling setGsmBroadcastConfig with " + configs.length + " configurations");
        }
        enforceNotOnHandlerThread("setCellBroadcastConfig");
        Request setRequest = new Request();
        synchronized (setRequest) {
            Message response = mHandler.obtainMessage(EVENT_SET_BROADCAST_CONFIG_DONE, setRequest);

            mPhone.mCi.setGsmBroadcastConfig(configs, response);

            waitForResult(setRequest);
        }

        return (boolean) setRequest.mResult;
    }

    private boolean setCellBroadcastActivation(boolean activate) {
        if (DBG) {
            log("Calling setCellBroadcastActivation(" + activate + ')');
        }

        enforceNotOnHandlerThread("setCellBroadcastConfig");
        Request setRequest = new Request();
        synchronized (setRequest) {
            Message response = mHandler.obtainMessage(EVENT_SET_BROADCAST_ACTIVATION_DONE,
                    setRequest);

            mPhone.mCi.setGsmBroadcastActivation(activate, response);
            waitForResult(setRequest);
        }

        return (boolean) setRequest.mResult;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs) {
        if (DBG) {
            log("Calling setCdmaBroadcastConfig with " + configs.length + " configurations");
        }

        enforceNotOnHandlerThread("setCdmaBroadcastConfig");
        Request setRequest = new Request();
        synchronized (setRequest) {
            Message response = mHandler.obtainMessage(EVENT_SET_BROADCAST_CONFIG_DONE, setRequest);

            mPhone.mCi.setCdmaBroadcastConfig(configs, response);

            waitForResult(setRequest);
        }

        return (boolean) setRequest.mResult;
    }

    private boolean setCdmaBroadcastActivation(boolean activate) {
        if (DBG) {
            log("Calling setCdmaBroadcastActivation(" + activate + ")");
        }

        enforceNotOnHandlerThread("setCdmaBroadcastActivation");
        Request setRequest = new Request();
        synchronized (setRequest) {
            Message response = mHandler.obtainMessage(EVENT_SET_BROADCAST_ACTIVATION_DONE,
                    setRequest);

            mPhone.mCi.setCdmaBroadcastActivation(activate, response);

            waitForResult(setRequest);
        }

        return (boolean) setRequest.mResult;
    }

    @UnsupportedAppUsage
    protected void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    protected void loge(String msg, Throwable e) {
        Rlog.e(LOG_TAG, msg, e);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isImsSmsSupported() {
        return mDispatchersController.isIms();
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public String getImsSmsFormat() {
        return mDispatchersController.getImsSmsFormat();
    }

    /**
     * @deprecated Use {@link #sendStoredText(String, String, Uri, String, PendingIntent,
     * PendingIntent)} instead
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void sendStoredText(String callingPkg, Uri messageUri, String scAddress,
            PendingIntent sentIntent, PendingIntent deliveryIntent) {
        sendStoredText(callingPkg, null, messageUri, scAddress, sentIntent, deliveryIntent);
    }

    public void sendStoredText(String callingPkg, String callingAttributionTag,
            Uri messageUri, String scAddress, PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        if (!mSmsPermissions.checkCallingCanSendSms(callingPkg, callingAttributionTag,
                "Sending SMS message")) {
            returnUnspecifiedFailure(sentIntent);
            return;
        }
        if (Rlog.isLoggable("SMS", Log.VERBOSE)) {
            log("sendStoredText: scAddr=" + scAddress + " messageUri=" + messageUri
                    + " sentIntent=" + sentIntent + " deliveryIntent=" + deliveryIntent);
        }
        final ContentResolver resolver = mContext.getContentResolver();
        if (!isFailedOrDraft(resolver, messageUri)) {
            loge("sendStoredText: not FAILED or DRAFT message");
            returnUnspecifiedFailure(sentIntent);
            return;
        }
        final String[] textAndAddress = loadTextAndAddress(resolver, messageUri);
        if (textAndAddress == null) {
            loge("sendStoredText: can not load text");
            returnUnspecifiedFailure(sentIntent);
            return;
        }
        notifyIfOutgoingEmergencySms(textAndAddress[1]);
        textAndAddress[1] = filterDestAddress(textAndAddress[1]);
        mDispatchersController.sendText(textAndAddress[1], scAddress, textAndAddress[0],
                sentIntent, deliveryIntent, messageUri, callingPkg,
                true /* persistMessageForNonDefaultSmsApp */, SMS_MESSAGE_PRIORITY_NOT_SPECIFIED,
                false /* expectMore */, SMS_MESSAGE_PERIOD_NOT_SPECIFIED, false /* isForVvm */,
                0L /* messageId */);
    }

    /**
     * @deprecated Use {@link #sendStoredMultipartText(String, String, Uri, String, List, List)}
     * instead
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void sendStoredMultipartText(String callingPkg, Uri messageUri, String scAddress,
            List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents) {
        sendStoredMultipartText(callingPkg, null, messageUri, scAddress, sentIntents,
                deliveryIntents);
    }

    public void sendStoredMultipartText(String callingPkg,
            String callingAttributionTag, Uri messageUri, String scAddress,
            List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents) {
        if (!mSmsPermissions.checkCallingCanSendSms(callingPkg, callingAttributionTag,
                "Sending SMS message")) {
            returnUnspecifiedFailure(sentIntents);
            return;
        }
        final ContentResolver resolver = mContext.getContentResolver();
        if (!isFailedOrDraft(resolver, messageUri)) {
            loge("sendStoredMultipartText: not FAILED or DRAFT message");
            returnUnspecifiedFailure(sentIntents);
            return;
        }
        final String[] textAndAddress = loadTextAndAddress(resolver, messageUri);
        if (textAndAddress == null) {
            loge("sendStoredMultipartText: can not load text");
            returnUnspecifiedFailure(sentIntents);
            return;
        }
        final ArrayList<String> parts = SmsManager.getDefault().divideMessage(textAndAddress[0]);
        if (parts == null || parts.size() < 1) {
            loge("sendStoredMultipartText: can not divide text");
            returnUnspecifiedFailure(sentIntents);
            return;
        }
        notifyIfOutgoingEmergencySms(textAndAddress[1]);
        textAndAddress[1] = filterDestAddress(textAndAddress[1]);

        if (parts.size() > 1 && parts.size() < 10 && !SmsMessage.hasEmsSupport()) {
            for (int i = 0; i < parts.size(); i++) {
                // If EMS is not supported, we have to break down EMS into single segment SMS
                // and add page info " x/y".
                String singlePart = parts.get(i);
                if (SmsMessage.shouldAppendPageNumberAsPrefix()) {
                    singlePart = String.valueOf(i + 1) + '/' + parts.size() + ' ' + singlePart;
                } else {
                    singlePart = singlePart.concat(' ' + String.valueOf(i + 1) + '/'
                            + parts.size());
                }

                PendingIntent singleSentIntent = null;
                if (sentIntents != null && sentIntents.size() > i) {
                    singleSentIntent = sentIntents.get(i);
                }

                PendingIntent singleDeliveryIntent = null;
                if (deliveryIntents != null && deliveryIntents.size() > i) {
                    singleDeliveryIntent = deliveryIntents.get(i);
                }

                mDispatchersController.sendText(textAndAddress[1], scAddress, singlePart,
                        singleSentIntent, singleDeliveryIntent, messageUri, callingPkg,
                        true  /* persistMessageForNonDefaultSmsApp */,
                        SMS_MESSAGE_PRIORITY_NOT_SPECIFIED,
                        false /* expectMore */, SMS_MESSAGE_PERIOD_NOT_SPECIFIED,
                        false /* isForVvm */, 0L /* messageId */);
            }
            return;
        }

        mDispatchersController.sendMultipartText(
                textAndAddress[1], // destAddress
                scAddress,
                parts,
                (ArrayList<PendingIntent>) sentIntents,
                (ArrayList<PendingIntent>) deliveryIntents,
                messageUri,
                callingPkg,
                true  /* persistMessageForNonDefaultSmsApp */,
                SMS_MESSAGE_PRIORITY_NOT_SPECIFIED,
                false /* expectMore */,
                SMS_MESSAGE_PERIOD_NOT_SPECIFIED,
                0L /* messageId */);
    }

    public int getSmsCapacityOnIcc(String callingPackage, String callingFeatureId) {
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(
                mContext, mPhone.getSubId(), callingPackage, callingFeatureId,
                "getSmsCapacityOnIcc")) {
            return 0;
        }

        int numberOnIcc = 0;
        if (mPhone.getIccRecordsLoaded()) {
            final UiccProfile uiccProfile = UiccController.getInstance()
                    .getUiccProfileForPhone(mPhone.getPhoneId());
            if(uiccProfile != null) {
                numberOnIcc = uiccProfile.getIccRecords().getSmsCapacityOnIcc();
            } else {
                loge("uiccProfile is null");
            }
        } else {
            loge("getSmsCapacityOnIcc - aborting, no icc card present.");
        }

        log("getSmsCapacityOnIcc().numberOnIcc = " + numberOnIcc);
        return numberOnIcc;
    }

    private boolean isFailedOrDraft(ContentResolver resolver, Uri messageUri) {
        // Clear the calling identity and query the database using the phone user id
        // Otherwise the AppOps check in TelephonyProvider would complain about mismatch
        // between the calling uid and the package uid
        final long identity = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            cursor = resolver.query(
                    messageUri,
                    new String[]{ Telephony.Sms.TYPE },
                    null/*selection*/,
                    null/*selectionArgs*/,
                    null/*sortOrder*/);
            if (cursor != null && cursor.moveToFirst()) {
                final int type = cursor.getInt(0);
                return type == Telephony.Sms.MESSAGE_TYPE_DRAFT
                        || type == Telephony.Sms.MESSAGE_TYPE_FAILED;
            }
        } catch (SQLiteException e) {
            loge("isFailedOrDraft: query message type failed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            Binder.restoreCallingIdentity(identity);
        }
        return false;
    }

    // Return an array including both the SMS text (0) and address (1)
    private String[] loadTextAndAddress(ContentResolver resolver, Uri messageUri) {
        // Clear the calling identity and query the database using the phone user id
        // Otherwise the AppOps check in TelephonyProvider would complain about mismatch
        // between the calling uid and the package uid
        final long identity = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            cursor = resolver.query(
                    messageUri,
                    new String[]{
                            Telephony.Sms.BODY,
                            Telephony.Sms.ADDRESS
                    },
                    null/*selection*/,
                    null/*selectionArgs*/,
                    null/*sortOrder*/);
            if (cursor != null && cursor.moveToFirst()) {
                return new String[]{ cursor.getString(0), cursor.getString(1) };
            }
        } catch (SQLiteException e) {
            loge("loadText: query message text failed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            Binder.restoreCallingIdentity(identity);
        }
        return null;
    }

    private void notifyIfOutgoingEmergencySms(String destAddr) {
        EmergencyNumber emergencyNumber = mPhone.getEmergencyNumberTracker().getEmergencyNumber(
                destAddr);
        if (emergencyNumber != null) {
            mPhone.notifyOutgoingEmergencySms(emergencyNumber);
        }
    }

    private void returnUnspecifiedFailure(PendingIntent pi) {
        if (pi != null) {
            try {
                pi.send(SmsManager.RESULT_ERROR_GENERIC_FAILURE);
            } catch (PendingIntent.CanceledException e) {
                // ignore
            }
        }
    }

    private void returnUnspecifiedFailure(List<PendingIntent> pis) {
        if (pis == null) {
            return;
        }
        for (PendingIntent pi : pis) {
            returnUnspecifiedFailure(pi);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String filterDestAddress(String destAddr) {
        String result = SmsNumberUtils.filterDestAddr(mContext, mPhone.getSubId(), destAddr);
        return result != null ? result : destAddr;
    }

    private void waitForResult(Request request) {
        synchronized (request) {
            while (!request.mStatus.get()) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    log("Interrupted while waiting for result");
                }
            }
        }
    }

    /**
     * Get InboundSmsHandler for the phone.
     */
    public InboundSmsHandler getInboundSmsHandler(boolean is3gpp2) {
        return mDispatchersController.getInboundSmsHandler(is3gpp2);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Enabled GSM channels: " + mCellBroadcastRangeManager);
        pw.println("Enabled CDMA channels: " + mCdmaBroadcastRangeManager);
        pw.println("CellBroadcast log:");
        mCellBroadcastLocalLog.dump(fd, pw, args);
        pw.println("SMS dispatcher controller log:");
        mDispatchersController.dump(fd, pw, args);
        pw.flush();
    }
}

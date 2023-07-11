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

package com.android.internal.telephony;

import static android.Manifest.permission.SEND_SMS_NO_CONFIRMATION;

import static com.android.internal.telephony.IccSmsInterfaceManager.SMS_MESSAGE_PERIOD_NOT_SPECIFIED;
import static com.android.internal.telephony.IccSmsInterfaceManager.SMS_MESSAGE_PRIORITY_NOT_SPECIFIED;
import static com.android.internal.telephony.SmsResponse.NO_ERROR_CODE;

import android.annotation.UserIdInt;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.service.carrier.CarrierMessagingService;
import android.service.carrier.CarrierMessagingServiceWrapper;
import android.service.carrier.CarrierMessagingServiceWrapper.CarrierMessagingCallback;
import android.telephony.AnomalyReporter;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.cdma.sms.UserData;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SMSDispatcher extends Handler {
    static final String TAG = "SMSDispatcher";    // accessed from inner class
    static final boolean DBG = false;
    private static final String SEND_NEXT_MSG_EXTRA = "SendNextMsg";
    private static final String MESSAGE_ID_EXTRA = "MessageId";
    protected static final String MAP_KEY_PDU = "pdu";
    protected static final String MAP_KEY_SMSC = "smsc";
    protected static final String MAP_KEY_DEST_ADDR = "destAddr";
    protected static final String MAP_KEY_SC_ADDR = "scAddr";
    protected static final String MAP_KEY_DEST_PORT = "destPort";
    protected static final String MAP_KEY_DATA = "data";
    protected static final String MAP_KEY_TEXT = "text";

    private static final int PREMIUM_RULE_USE_SIM = 1;
    private static final int PREMIUM_RULE_USE_NETWORK = 2;
    private static final int PREMIUM_RULE_USE_BOTH = 3;
    private final AtomicInteger mPremiumSmsRule = new AtomicInteger(PREMIUM_RULE_USE_SIM);
    private final SettingsObserver mSettingsObserver;

    /** SMS send complete. */
    protected static final int EVENT_SEND_SMS_COMPLETE = 2;

    /** Retry sending a previously failed SMS message */
    protected static final int EVENT_SEND_RETRY = 3;

    /** Confirmation required for sending a large number of messages. */
    private static final int EVENT_SEND_LIMIT_REACHED_CONFIRMATION = 4;

    /** Send the user confirmed SMS */
    static final int EVENT_SEND_CONFIRMED_SMS = 5; // accessed from inner class

    /** Don't send SMS (user did not confirm). */
    static final int EVENT_STOP_SENDING = 6; // accessed from inner class

    /** Don't send SMS for this app (User had already denied eariler.) */
    static final int EVENT_SENDING_NOT_ALLOWED = 7;

    /** Confirmation required for third-party apps sending to an SMS short code. */
    private static final int EVENT_CONFIRM_SEND_TO_POSSIBLE_PREMIUM_SHORT_CODE = 8;

    /** Confirmation required for third-party apps sending to an SMS short code. */
    private static final int EVENT_CONFIRM_SEND_TO_PREMIUM_SHORT_CODE = 9;

    /** New status report received. */
    protected static final int EVENT_NEW_SMS_STATUS_REPORT = 10;

    // other
    protected static final int EVENT_NEW_ICC_SMS = 14;
    protected static final int EVENT_ICC_CHANGED = 15;
    protected static final int EVENT_GET_IMS_SERVICE = 16;

    @UnsupportedAppUsage
    protected Phone mPhone;
    @UnsupportedAppUsage
    protected final Context mContext;
    @UnsupportedAppUsage
    protected final ContentResolver mResolver;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected final CommandsInterface mCi;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected final TelephonyManager mTelephonyManager;
    protected final LocalLog mLocalLog = new LocalLog(16);

    /** Maximum number of times to retry sending a failed SMS. */
    protected static final int MAX_SEND_RETRIES = 3;
    /** Delay before next send attempt on a failed SMS, in milliseconds. */
    @VisibleForTesting
    public static final int SEND_RETRY_DELAY = 2000;
    /** Message sending queue limit */
    private static final int MO_MSG_QUEUE_LIMIT = 5;
    /** SMS anomaly uuid -- CarrierMessagingService did not respond */
    private static final UUID sAnomalyNoResponseFromCarrierMessagingService =
            UUID.fromString("279d9fbc-462d-4fc2-802c-bf21ddd9dd90");
    /** SMS anomaly uuid -- CarrierMessagingService unexpected callback */
    private static final UUID sAnomalyUnexpectedCallback =
            UUID.fromString("0103b6d2-ad07-4d86-9102-14341b9074ef");

    /**
     * Message reference for a CONCATENATED_8_BIT_REFERENCE or
     * CONCATENATED_16_BIT_REFERENCE message set.  Should be
     * incremented for each set of concatenated messages.
     * Static field shared by all dispatcher objects.
     */
    private static int sConcatenatedRef = new Random().nextInt(256);

    protected SmsDispatchersController mSmsDispatchersController;

    /** Number of outgoing SmsTrackers waiting for user confirmation. */
    private int mPendingTrackerCount;

    /* Flags indicating whether the current device allows sms service */
    protected boolean mSmsCapable = true;
    protected boolean mSmsSendDisabled;

    @VisibleForTesting
    public int mCarrierMessagingTimeout = 10 * 60 * 1000; //10 minutes

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected static int getNextConcatenatedRef() {
        sConcatenatedRef += 1;
        return sConcatenatedRef;
    }

    /**
     * Create a new SMS dispatcher.
     * @param phone the Phone to use
     */
    protected SMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        mPhone = phone;
        mSmsDispatchersController = smsDispatchersController;
        mContext = phone.getContext();
        mResolver = mContext.getContentResolver();
        mCi = phone.mCi;
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mSettingsObserver = new SettingsObserver(this, mPremiumSmsRule, mContext);
        mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.SMS_SHORT_CODE_RULE), false, mSettingsObserver);

        mSmsCapable = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_sms_capable);
        mSmsSendDisabled = !mTelephonyManager.getSmsSendCapableForPhone(
                mPhone.getPhoneId(), mSmsCapable);
        Rlog.d(TAG, "SMSDispatcher: ctor mSmsCapable=" + mSmsCapable + " format=" + getFormat()
                + " mSmsSendDisabled=" + mSmsSendDisabled);
    }

    /**
     * Observe the secure setting for updated premium sms determination rules
     */
    private static class SettingsObserver extends ContentObserver {
        private final AtomicInteger mPremiumSmsRule;
        private final Context mContext;
        SettingsObserver(Handler handler, AtomicInteger premiumSmsRule, Context context) {
            super(handler);
            mPremiumSmsRule = premiumSmsRule;
            mContext = context;
            onChange(false); // load initial value;
        }

        @Override
        public void onChange(boolean selfChange) {
            mPremiumSmsRule.set(Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.SMS_SHORT_CODE_RULE, PREMIUM_RULE_USE_SIM));
        }
    }

    /** Unregister for incoming SMS events. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void dispose() {
        mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    /**
     * The format of the message PDU in the associated broadcast intent.
     * This will be either "3gpp" for GSM/UMTS/LTE messages in 3GPP format
     * or "3gpp2" for CDMA/LTE messages in 3GPP2 format.
     *
     * Note: All applications which handle incoming SMS messages by processing the
     * SMS_RECEIVED_ACTION broadcast intent MUST pass the "format" extra from the intent
     * into the new methods in {@link android.telephony.SmsMessage} which take an
     * extra format parameter. This is required in order to correctly decode the PDU on
     * devices which require support for both 3GPP and 3GPP2 formats at the same time,
     * such as CDMA/LTE devices and GSM/CDMA world phones.
     *
     * @return the format of the message PDU
     */
    protected abstract String getFormat();

    /**
     * Called when a status report is received. This should correspond to a previously successful
     * SEND.
     *
     * @param o AsyncResult object including a byte array for 3GPP status report PDU or SmsMessage
     *          object for 3GPP2 status report.
     */
    protected void handleStatusReport(Object o) {
        Rlog.d(TAG, "handleStatusReport() called with no subclass.");
    }

    /**
     * Handles events coming from the phone stack. Overridden from handler.
     *
     * @param msg the message to handle
     */
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
        case EVENT_SEND_SMS_COMPLETE:
            // An outbound SMS has been successfully transferred, or failed.
            handleSendComplete((AsyncResult) msg.obj);
            break;

        case EVENT_SEND_RETRY:
            Rlog.d(TAG, "SMS retry..");
            sendRetrySms((SmsTracker) msg.obj);
            break;

        case EVENT_SEND_LIMIT_REACHED_CONFIRMATION:
            handleReachSentLimit((SmsTracker[]) (msg.obj));
            break;

        case EVENT_CONFIRM_SEND_TO_POSSIBLE_PREMIUM_SHORT_CODE:
            handleConfirmShortCode(false, (SmsTracker[]) (msg.obj));
            break;

        case EVENT_CONFIRM_SEND_TO_PREMIUM_SHORT_CODE:
            handleConfirmShortCode(true, (SmsTracker[]) (msg.obj));
            break;

        case EVENT_SEND_CONFIRMED_SMS:
        {
            SmsTracker[] trackers = (SmsTracker[]) msg.obj;
            for (SmsTracker tracker : trackers) {
                sendSms(tracker);
            }
            mPendingTrackerCount--;
            break;
        }

        case EVENT_SENDING_NOT_ALLOWED:
        {
            SmsTracker[] trackers = (SmsTracker[]) msg.obj;
            Rlog.d(TAG, "SMSDispatcher: EVENT_SENDING_NOT_ALLOWED - "
                    + "sending SHORT_CODE_NEVER_ALLOWED error code.");
            handleSmsTrackersFailure(
                    trackers, SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED, NO_ERROR_CODE);
            break;
        }

        case EVENT_STOP_SENDING:
        {
            SmsTracker[] trackers = (SmsTracker[]) msg.obj;
            int error;
            if (msg.arg1 == ConfirmDialogListener.SHORT_CODE_MSG) {
                if (msg.arg2 == ConfirmDialogListener.NEVER_ALLOW) {
                    error = SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED;
                    Rlog.d(TAG, "SMSDispatcher: EVENT_STOP_SENDING - "
                            + "sending SHORT_CODE_NEVER_ALLOWED error code.");
                } else {
                    error = SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED;
                    Rlog.d(TAG, "SMSDispatcher: EVENT_STOP_SENDING - "
                            + "sending SHORT_CODE_NOT_ALLOWED error code.");
                }
            } else if (msg.arg1 == ConfirmDialogListener.RATE_LIMIT) {
                error = SmsManager.RESULT_ERROR_LIMIT_EXCEEDED;
                Rlog.d(TAG, "SMSDispatcher: EVENT_STOP_SENDING - "
                        + "sending LIMIT_EXCEEDED error code.");
            } else {
                error = SmsManager.RESULT_UNEXPECTED_EVENT_STOP_SENDING;
                Rlog.e(TAG, "SMSDispatcher: EVENT_STOP_SENDING - unexpected cases.");
            }

            handleSmsTrackersFailure(trackers, error, NO_ERROR_CODE);
            mPendingTrackerCount--;
            break;
        }

        case EVENT_NEW_SMS_STATUS_REPORT:
            handleStatusReport(msg.obj);
            break;

        default:
            Rlog.e(TAG, "handleMessage() ignoring message of unexpected type " + msg.what);
        }
    }

    /**
     * Use the carrier messaging service to send a data or text SMS.
     */
    protected abstract class SmsSender extends Handler {
        private static final int EVENT_TIMEOUT = 1;
        // Initialized in sendSmsByCarrierApp
        protected volatile CarrierMessagingCallback mSenderCallback;
        protected final CarrierMessagingServiceWrapper mCarrierMessagingServiceWrapper =
                new CarrierMessagingServiceWrapper();
        private String mCarrierPackageName;

        protected SmsSender() {
            super(Looper.getMainLooper());
        }

        /**
         * Bind to carrierPackageName to send message through it
         */
        public synchronized void sendSmsByCarrierApp(String carrierPackageName,
                CarrierMessagingCallback senderCallback) {
            mCarrierPackageName = carrierPackageName;
            mSenderCallback = senderCallback;
            if (!mCarrierMessagingServiceWrapper.bindToCarrierMessagingService(
                    mContext, carrierPackageName, runnable -> runnable.run(),
                    ()->onServiceReady())) {
                Rlog.e(TAG, "bindService() for carrier messaging service failed");
                onSendComplete(CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK);
            } else {
                Rlog.d(TAG, "bindService() for carrier messaging service succeeded");
                sendMessageDelayed(obtainMessage(EVENT_TIMEOUT), mCarrierMessagingTimeout);
            }
        }

        /**
         * Callback received from mCarrierPackageName on binding to it is done.
         * NOTE: the implementations of this method must be synchronized to make sure it does not
         * get called before {@link #sendSmsByCarrierApp} completes and {@link #EVENT_TIMEOUT} is
         * posted
         */
        public abstract void onServiceReady();

        /**
         * Method to call message send callback with passed in result and default parameters
         */
        public abstract void onSendComplete(@CarrierMessagingService.SendResult int result);

        /**
         * Used to get the SmsTracker for single part messages
         */
        public abstract SmsTracker getSmsTracker();

        /**
         * Used to get the SmsTrackers for multi part messages
         */
        public abstract SmsTracker[] getSmsTrackers();

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == EVENT_TIMEOUT) {
                logWithLocalLog("handleMessage: No response from " + mCarrierPackageName
                        + " for " + mCarrierMessagingTimeout + " ms");
                AnomalyReporter.reportAnomaly(sAnomalyNoResponseFromCarrierMessagingService,
                        "No response from " + mCarrierPackageName, mPhone.getCarrierId());
                onSendComplete(CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK);
            } else {
                logWithLocalLog("handleMessage: received unexpected message " + msg.what);
            }
        }

        public void removeTimeout() {
            removeMessages(EVENT_TIMEOUT);
        }
    }

    private void logWithLocalLog(String logStr) {
        mLocalLog.log(logStr);
        Rlog.d(TAG, logStr);
    }

    /**
     * Use the carrier messaging service to send a text SMS.
     */
    protected final class TextSmsSender extends SmsSender {
        private final SmsTracker mTracker;
        public TextSmsSender(SmsTracker tracker) {
            super();
            mTracker = tracker;
        }

        @Override
        public synchronized void onServiceReady() {
            Rlog.d(TAG, "TextSmsSender::onServiceReady");
            HashMap<String, Object> map = mTracker.getData();
            String text = (String) map.get(MAP_KEY_TEXT);

            if (text != null) {
                try {
                    mCarrierMessagingServiceWrapper.sendTextSms(
                            text,
                            getSubId(),
                            mTracker.mDestAddress,
                            (mTracker.mDeliveryIntent != null)
                                    ? CarrierMessagingService.SEND_FLAG_REQUEST_DELIVERY_STATUS
                                    : 0,
                            runnable -> runnable.run(),
                            mSenderCallback);
                } catch (RuntimeException e) {
                    Rlog.e(TAG, "TextSmsSender::onServiceReady: Exception sending the SMS: "
                            + e.getMessage());
                    onSendComplete(CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK);
                }
            } else {
                Rlog.d(TAG, "TextSmsSender::onServiceReady: text == null");
                onSendComplete(CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK);
            }
        }

        @Override
        public void onSendComplete(int result) {
            mSenderCallback.onSendSmsComplete(result, 0 /* messageRef */);
        }

        @Override
        public SmsTracker getSmsTracker() {
            return mTracker;
        }

        @Override
        public SmsTracker[] getSmsTrackers() {
            Rlog.e(TAG, "getSmsTrackers: Unexpected call for TextSmsSender");
            return null;
        }
    }

    /**
     * Use the carrier messaging service to send a data SMS.
     */
    protected final class DataSmsSender extends SmsSender {
        private final SmsTracker mTracker;
        public DataSmsSender(SmsTracker tracker) {
            super();
            mTracker = tracker;
        }

        @Override
        public synchronized void onServiceReady() {
            Rlog.d(TAG, "DataSmsSender::onServiceReady");
            HashMap<String, Object> map = mTracker.getData();
            byte[] data = (byte[]) map.get(MAP_KEY_DATA);
            int destPort = (int) map.get(MAP_KEY_DEST_PORT);

            if (data != null) {
                try {
                    mCarrierMessagingServiceWrapper.sendDataSms(
                            data,
                            getSubId(),
                            mTracker.mDestAddress,
                            destPort,
                            (mTracker.mDeliveryIntent != null)
                                    ? CarrierMessagingService.SEND_FLAG_REQUEST_DELIVERY_STATUS
                                    : 0,
                            runnable -> runnable.run(),
                            mSenderCallback);
                } catch (RuntimeException e) {
                    Rlog.e(TAG, "DataSmsSender::onServiceReady: Exception sending the SMS: "
                            + e
                            + " " + SmsController.formatCrossStackMessageId(mTracker.mMessageId));
                    onSendComplete(CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK);
                }
            } else {
                Rlog.d(TAG, "DataSmsSender::onServiceReady: data == null");
                onSendComplete(CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK);
            }
        }

        @Override
        public void onSendComplete(int result) {
            mSenderCallback.onSendSmsComplete(result, 0 /* messageRef */);
        }

        @Override
        public SmsTracker getSmsTracker() {
            return mTracker;
        }

        @Override
        public SmsTracker[] getSmsTrackers() {
            Rlog.e(TAG, "getSmsTrackers: Unexpected call for DataSmsSender");
            return null;
        }
    }

    /**
     * Callback for TextSmsSender and DataSmsSender from the carrier messaging service.
     * Once the result is ready, the carrier messaging service connection is disposed.
     */
    protected final class SmsSenderCallback implements CarrierMessagingCallback {
        private final SmsSender mSmsSender;
        private boolean mCallbackCalled = false;

        public SmsSenderCallback(SmsSender smsSender) {
            mSmsSender = smsSender;
        }

        /**
         * This method should be called only once.
         */
        @Override
        public void onSendSmsComplete(int result, int messageRef) {
            Rlog.d(TAG, "onSendSmsComplete: result=" + result + " messageRef=" + messageRef);
            if (mCallbackCalled) {
                logWithLocalLog("onSendSmsComplete: unexpected call");
                AnomalyReporter.reportAnomaly(sAnomalyUnexpectedCallback,
                        "Unexpected onSendSmsComplete", mPhone.getCarrierId());
                return;
            }
            mCallbackCalled = true;
            final long identity = Binder.clearCallingIdentity();
            try {
                mSmsSender.mCarrierMessagingServiceWrapper.disconnect();
                processSendSmsResponse(mSmsSender.getSmsTracker(), result, messageRef);
                mSmsSender.removeTimeout();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onSendMultipartSmsComplete(int result, int[] messageRefs) {
            Rlog.e(TAG, "Unexpected onSendMultipartSmsComplete call with result: " + result);
        }

        @Override
        public void onReceiveSmsComplete(int result) {
            Rlog.e(TAG, "Unexpected onReceiveSmsComplete call with result: " + result);
        }

        @Override
        public void onSendMmsComplete(int result, byte[] sendConfPdu) {
            Rlog.e(TAG, "Unexpected onSendMmsComplete call with result: " + result);
        }

        @Override
        public void onDownloadMmsComplete(int result) {
            Rlog.e(TAG, "Unexpected onDownloadMmsComplete call with result: " + result);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void processSendSmsResponse(SmsTracker tracker, int result, int messageRef) {
        if (tracker == null) {
            Rlog.e(TAG, "processSendSmsResponse: null tracker");
            return;
        }

        SmsResponse smsResponse = new SmsResponse(messageRef, null /* ackPdu */, NO_ERROR_CODE,
                tracker.mMessageId);

        switch (result) {
            case CarrierMessagingService.SEND_STATUS_OK:
                Rlog.d(TAG, "processSendSmsResponse: Sending SMS by CarrierMessagingService "
                        + "succeeded. "
                        + SmsController.formatCrossStackMessageId(tracker.mMessageId));
                sendMessage(obtainMessage(EVENT_SEND_SMS_COMPLETE,
                                          new AsyncResult(tracker,
                                                          smsResponse,
                                                          null /* exception*/)));
                break;
            case CarrierMessagingService.SEND_STATUS_ERROR:
                Rlog.d(TAG, "processSendSmsResponse: Sending SMS by CarrierMessagingService"
                        + " failed. "
                        + SmsController.formatCrossStackMessageId(tracker.mMessageId));
                sendMessage(obtainMessage(EVENT_SEND_SMS_COMPLETE,
                        new AsyncResult(tracker, smsResponse,
                                new CommandException(CommandException.Error.GENERIC_FAILURE))));
                break;
            case CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK:
                Rlog.d(TAG, "processSendSmsResponse: Sending SMS by CarrierMessagingService failed."
                        + " Retry on carrier network. "
                        + SmsController.formatCrossStackMessageId(tracker.mMessageId));
                sendSubmitPdu(tracker);
                break;
            default:
                Rlog.d(TAG, "processSendSmsResponse: Unknown result " + result + " Retry on carrier"
                        + " network. "
                        + SmsController.formatCrossStackMessageId(tracker.mMessageId));
                sendSubmitPdu(tracker);
        }
    }

    /**
     * Use the carrier messaging service to send a multipart text SMS.
     */
    private final class MultipartSmsSender extends SmsSender {
        private final List<String> mParts;
        public final SmsTracker[] mTrackers;

        MultipartSmsSender(ArrayList<String> parts, SmsTracker[] trackers) {
            super();
            mParts = parts;
            mTrackers = trackers;
        }

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        void sendSmsByCarrierApp(String carrierPackageName,
                                 MultipartSmsSenderCallback senderCallback) {
            super.sendSmsByCarrierApp(carrierPackageName, senderCallback);
        }

        @Override
        public synchronized void onServiceReady() {
            Rlog.d(TAG, "MultipartSmsSender::onServiceReady");
            boolean statusReportRequested = false;
            for (SmsTracker tracker : mTrackers) {
                if (tracker.mDeliveryIntent != null) {
                    statusReportRequested = true;
                    break;
                }
            }

            try {
                mCarrierMessagingServiceWrapper.sendMultipartTextSms(
                        mParts,
                        getSubId(),
                        mTrackers[0].mDestAddress,
                        statusReportRequested
                                ? CarrierMessagingService.SEND_FLAG_REQUEST_DELIVERY_STATUS
                                : 0,
                        runnable -> runnable.run(),
                        mSenderCallback);
            } catch (RuntimeException e) {
                Rlog.e(TAG, "MultipartSmsSender::onServiceReady: Exception sending the SMS: " + e);
                onSendComplete(CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK);
            }
        }

        @Override
        public void onSendComplete(int result) {
            mSenderCallback.onSendMultipartSmsComplete(result, null /* messageRefs */);
        }

        @Override
        public SmsTracker getSmsTracker() {
            Rlog.e(TAG, "getSmsTracker: Unexpected call for MultipartSmsSender");
            return null;
        }

        @Override
        public SmsTracker[] getSmsTrackers() {
            return mTrackers;
        }
    }

    /**
     * Callback for MultipartSmsSender from the carrier messaging service.
     * Once the result is ready, the carrier messaging service connection is disposed.
     */
    private final class MultipartSmsSenderCallback implements CarrierMessagingCallback {
        private final MultipartSmsSender mSmsSender;
        private boolean mCallbackCalled = false;

        MultipartSmsSenderCallback(MultipartSmsSender smsSender) {
            mSmsSender = smsSender;
        }

        @Override
        public void onSendSmsComplete(int result, int messageRef) {
            Rlog.e(TAG, "Unexpected onSendSmsComplete call with result: " + result);
        }

        /**
         * This method should be called only once.
         */
        @Override
        public void onSendMultipartSmsComplete(int result, int[] messageRefs) {
            Rlog.d(TAG, "onSendMultipartSmsComplete: result=" + result + " messageRefs="
                    + Arrays.toString(messageRefs));
            if (mCallbackCalled) {
                logWithLocalLog("onSendMultipartSmsComplete: unexpected call");
                AnomalyReporter.reportAnomaly(sAnomalyUnexpectedCallback,
                        "Unexpected onSendMultipartSmsComplete", mPhone.getCarrierId());
                return;
            }
            mCallbackCalled = true;
            mSmsSender.removeTimeout();
            mSmsSender.mCarrierMessagingServiceWrapper.disconnect();

            if (mSmsSender.mTrackers == null) {
                Rlog.e(TAG, "Unexpected onSendMultipartSmsComplete call with null trackers.");
                return;
            }

            final long identity = Binder.clearCallingIdentity();
            try {
                processSendMultipartSmsResponse(mSmsSender.mTrackers, result, messageRefs);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onReceiveSmsComplete(int result) {
            Rlog.e(TAG, "Unexpected onReceiveSmsComplete call with result: " + result);
        }

        @Override
        public void onSendMmsComplete(int result, byte[] sendConfPdu) {
            Rlog.e(TAG, "Unexpected onSendMmsComplete call with result: " + result);
        }

        @Override
        public void onDownloadMmsComplete(int result) {
            Rlog.e(TAG, "Unexpected onDownloadMmsComplete call with result: " + result);
        }
    }

    private void processSendMultipartSmsResponse(
            SmsTracker[] trackers, int result, int[] messageRefs) {
        if (trackers == null) {
            Rlog.e(TAG, "processSendMultipartSmsResponse: null trackers");
            return;
        }

        switch (result) {
            case CarrierMessagingService.SEND_STATUS_OK:
                Rlog.d(TAG, "processSendMultipartSmsResponse: Sending SMS by "
                        + "CarrierMessagingService succeeded. "
                        + SmsController.formatCrossStackMessageId(trackers[0].mMessageId));
                // Sending a multi-part SMS by CarrierMessagingService successfully completed.
                // Send EVENT_SEND_SMS_COMPLETE for all the parts one by one.
                for (int i = 0; i < trackers.length; i++) {
                    int messageRef = 0;
                    if (messageRefs != null && messageRefs.length > i) {
                        messageRef = messageRefs[i];
                    }
                    sendMessage(
                            obtainMessage(
                                    EVENT_SEND_SMS_COMPLETE,
                                    new AsyncResult(
                                            trackers[i],
                                            new SmsResponse(
                                                    messageRef, null /* ackPdu */, NO_ERROR_CODE),
                                            null /* exception */)));
                }
                break;
            case CarrierMessagingService.SEND_STATUS_ERROR:
                Rlog.d(TAG, "processSendMultipartSmsResponse: Sending SMS by "
                        + "CarrierMessagingService failed. "
                        + SmsController.formatCrossStackMessageId(trackers[0].mMessageId));
                // Sending a multi-part SMS by CarrierMessagingService failed.
                // Send EVENT_SEND_SMS_COMPLETE with GENERIC_FAILURE for all the parts one by one.
                for (int i = 0; i < trackers.length; i++) {
                    int messageRef = 0;
                    if (messageRefs != null && messageRefs.length > i) {
                        messageRef = messageRefs[i];
                    }
                    sendMessage(
                            obtainMessage(
                                    EVENT_SEND_SMS_COMPLETE,
                                    new AsyncResult(
                                            trackers[i],
                                            new SmsResponse(
                                                    messageRef, null /* ackPdu */, NO_ERROR_CODE),
                                            new CommandException(
                                                    CommandException.Error.GENERIC_FAILURE))));
                }
                break;
            case CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK:
                Rlog.d(TAG, "processSendMultipartSmsResponse: Sending SMS by "
                        + "CarrierMessagingService failed. Retry on carrier network. "
                        + SmsController.formatCrossStackMessageId(trackers[0].mMessageId));
                // All the parts for a multi-part SMS are handled together for retry. It helps to
                // check user confirmation once also if needed.
                sendSubmitPdu(trackers);
                break;
            default:
                Rlog.d(TAG, "processSendMultipartSmsResponse: Unknown result " + result
                        + ". Retry on carrier network. "
                        + SmsController.formatCrossStackMessageId(trackers[0].mMessageId));
                sendSubmitPdu(trackers);
        }
    }

    /** Send a single SMS PDU. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void sendSubmitPdu(SmsTracker tracker) {
        sendSubmitPdu(new SmsTracker[] {tracker});
    }

    /** Send a multi-part SMS PDU. Usually just calls {@link sendRawPdu}. */
    private void sendSubmitPdu(SmsTracker[] trackers) {
        if (shouldBlockSmsForEcbm()) {
            Rlog.d(TAG, "Block SMS in Emergency Callback mode");
            handleSmsTrackersFailure(trackers, SmsManager.RESULT_SMS_BLOCKED_DURING_EMERGENCY,
                    NO_ERROR_CODE);
        } else {
            sendRawPdu(trackers);
        }
    }

    /**
     * @return true if MO SMS should be blocked for Emergency Callback Mode.
     */
    protected abstract boolean shouldBlockSmsForEcbm();

    /**
     * Called when SMS send completes. Broadcasts a sentIntent on success.
     * On failure, either sets up retries or broadcasts a sentIntent with
     * the failure in the result code.
     *
     * @param ar AsyncResult passed into the message handler.  ar.result should
     *           an SmsResponse instance if send was successful.  ar.userObj
     *           should be an SmsTracker instance.
     */
    protected void handleSendComplete(AsyncResult ar) {
        SmsTracker tracker = (SmsTracker) ar.userObj;
        PendingIntent sentIntent = tracker.mSentIntent;
        SmsResponse smsResponse = (SmsResponse) ar.result;

        if (smsResponse != null) {
            tracker.mMessageRef = smsResponse.mMessageRef;
        } else {
            Rlog.d(TAG, "SmsResponse was null");
        }

        if (ar.exception == null) {
            if (DBG) {
                Rlog.d(TAG, "SMS send complete. Broadcasting intent: " + sentIntent
                        + " " + SmsController.formatCrossStackMessageId(tracker.mMessageId));
            }

            if (tracker.mDeliveryIntent != null) {
                // Expecting a status report. Put this tracker to the map.
                mSmsDispatchersController.putDeliveryPendingTracker(tracker);
            }
            tracker.onSent(mContext);
            mPhone.notifySmsSent(tracker.mDestAddress);

            mPhone.getSmsStats().onOutgoingSms(
                    tracker.mImsRetry > 0 /* isOverIms */,
                    SmsConstants.FORMAT_3GPP2.equals(getFormat()),
                    false /* fallbackToCs */,
                    SmsManager.RESULT_ERROR_NONE,
                    tracker.mMessageId,
                    tracker.isFromDefaultSmsApplication(mContext),
                    tracker.getInterval());
        } else {
            if (DBG) {
                Rlog.d(TAG, "SMS send failed "
                        + SmsController.formatCrossStackMessageId(tracker.mMessageId));
            }

            int ss = mPhone.getServiceState().getState();
            int error = rilErrorToSmsManagerResult(
                    ((CommandException) (ar.exception)).getCommandError(), tracker);

            if (tracker.mImsRetry > 0 && ss != ServiceState.STATE_IN_SERVICE) {
                // This is retry after failure over IMS but voice is not available.
                // Set retry to max allowed, so no retry is sent and cause
                // SmsManager.RESULT_ERROR_GENERIC_FAILURE to be returned to app.
                tracker.mRetryCount = MAX_SEND_RETRIES;

                Rlog.d(TAG, "handleSendComplete: Skipping retry: "
                        + " isIms()=" + isIms()
                        + " mRetryCount=" + tracker.mRetryCount
                        + " mImsRetry=" + tracker.mImsRetry
                        + " mMessageRef=" + tracker.mMessageRef
                        + " SS= " + mPhone.getServiceState().getState()
                        + " " + SmsController.formatCrossStackMessageId(tracker.mMessageId));
            }

            // if sms over IMS is not supported on data and voice is not available...
            if (!isIms() && ss != ServiceState.STATE_IN_SERVICE) {
                tracker.onFailed(mContext, getNotInServiceError(ss), NO_ERROR_CODE);
                mPhone.getSmsStats().onOutgoingSms(
                        tracker.mImsRetry > 0 /* isOverIms */,
                        SmsConstants.FORMAT_3GPP2.equals(getFormat()),
                        false /* fallbackToCs */,
                        getNotInServiceError(ss),
                        tracker.mMessageId,
                        tracker.isFromDefaultSmsApplication(mContext),
                        tracker.getInterval());
            } else if (error == SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY
                    && tracker.mRetryCount < MAX_SEND_RETRIES) {
                // Retry after a delay if needed.
                // TODO: According to TS 23.040, 9.2.3.6, we should resend
                //       with the same TP-MR as the failed message, and
                //       TP-RD set to 1.  However, we don't have a means of
                //       knowing the MR for the failed message (EF_SMSstatus
                //       may or may not have the MR corresponding to this
                //       message, depending on the failure).  Also, in some
                //       implementations this retry is handled by the baseband.
                tracker.mRetryCount++;
                int errorCode = (smsResponse != null) ? smsResponse.mErrorCode : NO_ERROR_CODE;
                Message retryMsg = obtainMessage(EVENT_SEND_RETRY, tracker);
                sendMessageDelayed(retryMsg, SEND_RETRY_DELAY);
                mPhone.getSmsStats().onOutgoingSms(
                        tracker.mImsRetry > 0 /* isOverIms */,
                        SmsConstants.FORMAT_3GPP2.equals(getFormat()),
                        false /* fallbackToCs */,
                        SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY,
                        errorCode,
                        tracker.mMessageId,
                        tracker.isFromDefaultSmsApplication(mContext),
                        tracker.getInterval());
            } else {
                int errorCode = (smsResponse != null) ? smsResponse.mErrorCode : NO_ERROR_CODE;
                tracker.onFailed(mContext, error, errorCode);
                mPhone.getSmsStats().onOutgoingSms(
                        tracker.mImsRetry > 0 /* isOverIms */,
                        SmsConstants.FORMAT_3GPP2.equals(getFormat()),
                        false /* fallbackToCs */,
                        error,
                        errorCode,
                        tracker.mMessageId,
                        tracker.isFromDefaultSmsApplication(mContext),
                        tracker.getInterval());
            }
        }
    }

    @SmsManager.Result
    private static int rilErrorToSmsManagerResult(CommandException.Error rilError,
            SmsTracker tracker) {
        switch (rilError) {
            case RADIO_NOT_AVAILABLE:
                return SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE;
            case SMS_FAIL_RETRY:
                return SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY;
            case NETWORK_REJECT:
                return SmsManager.RESULT_RIL_NETWORK_REJECT;
            case INVALID_STATE:
                return SmsManager.RESULT_RIL_INVALID_STATE;
            case INVALID_ARGUMENTS:
                return SmsManager.RESULT_RIL_INVALID_ARGUMENTS;
            case NO_MEMORY:
                return SmsManager.RESULT_RIL_NO_MEMORY;
            case REQUEST_RATE_LIMITED:
                return SmsManager.RESULT_RIL_REQUEST_RATE_LIMITED;
            case INVALID_SMS_FORMAT:
                return SmsManager.RESULT_RIL_INVALID_SMS_FORMAT;
            case SYSTEM_ERR:
                return SmsManager.RESULT_RIL_SYSTEM_ERR;
            case ENCODING_ERR:
                return SmsManager.RESULT_RIL_ENCODING_ERR;
            case MODEM_ERR:
                return SmsManager.RESULT_RIL_MODEM_ERR;
            case NETWORK_ERR:
                return SmsManager.RESULT_RIL_NETWORK_ERR;
            case INTERNAL_ERR:
                return SmsManager.RESULT_RIL_INTERNAL_ERR;
            case REQUEST_NOT_SUPPORTED:
                return SmsManager.RESULT_RIL_REQUEST_NOT_SUPPORTED;
            case INVALID_MODEM_STATE:
                return SmsManager.RESULT_RIL_INVALID_MODEM_STATE;
            case NETWORK_NOT_READY:
                return SmsManager.RESULT_RIL_NETWORK_NOT_READY;
            case OPERATION_NOT_ALLOWED:
                return SmsManager.RESULT_RIL_OPERATION_NOT_ALLOWED;
            case NO_RESOURCES:
                return SmsManager.RESULT_RIL_NO_RESOURCES;
            case REQUEST_CANCELLED:
                return SmsManager.RESULT_RIL_CANCELLED;
            case SIM_ABSENT:
                return SmsManager.RESULT_RIL_SIM_ABSENT;
            case FDN_CHECK_FAILURE:
                return SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE;
            case SIMULTANEOUS_SMS_AND_CALL_NOT_ALLOWED:
                return SmsManager.RESULT_RIL_SIMULTANEOUS_SMS_AND_CALL_NOT_ALLOWED;
            case ACCESS_BARRED:
                return SmsManager.RESULT_RIL_ACCESS_BARRED;
            case BLOCKED_DUE_TO_CALL:
                return SmsManager.RESULT_RIL_BLOCKED_DUE_TO_CALL;
            default:
                Rlog.d(TAG, "rilErrorToSmsManagerResult: " + rilError + " "
                        + SmsController.formatCrossStackMessageId(tracker.mMessageId));
                return SmsManager.RESULT_RIL_GENERIC_ERROR;
        }
    }

    /**
     * @param ss service state
     * @return The result error based on input service state for not in service error
     */
    @SmsManager.Result
    protected static int getNotInServiceError(int ss) {
        if (ss == ServiceState.STATE_POWER_OFF) {
            return SmsManager.RESULT_ERROR_RADIO_OFF;
        }
        return SmsManager.RESULT_ERROR_NO_SERVICE;
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
     *  <code>SmsManager.RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>SmsManager.RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>SmsManager.RESULT_ERROR_NULL_PDU</code><br>
     *  <code>SmsManager.RESULT_ERROR_NO_SERVICE</code><br>
     *  <code>SmsManager.RESULT_ERROR_LIMIT_EXCEEDED</code><br>
     *  <code>SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE</code><br>
     *  <code>SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RADIO_NOT_AVAILABLE</code><br>
     *  <code>SmsManager.RESULT_NETWORK_REJECT</code><br>
     *  <code>SmsManager.RESULT_INVALID_ARGUMENTS</code><br>
     *  <code>SmsManager.RESULT_INVALID_STATE</code><br>
     *  <code>SmsManager.RESULT_NO_MEMORY</code><br>
     *  <code>SmsManager.RESULT_INVALID_SMS_FORMAT</code><br>
     *  <code>SmsManager.RESULT_SYSTEM_ERROR</code><br>
     *  <code>SmsManager.RESULT_MODEM_ERROR</code><br>
     *  <code>SmsManager.RESULT_NETWORK_ERROR</code><br>
     *  <code>SmsManager.RESULT_ENCODING_ERROR</code><br>
     *  <code>SmsManager.RESULT_INVALID_SMSC_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_OPERATION_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_INTERNAL_ERROR</code><br>
     *  <code>SmsManager.RESULT_NO_RESOURCES</code><br>
     *  <code>SmsManager.RESULT_CANCELLED</code><br>
     *  <code>SmsManager.RESULT_REQUEST_NOT_SUPPORTED</code><br>
     *  <code>SmsManager.RESULT_NO_BLUETOOTH_SERVICE</code><br>
     *  <code>SmsManager.RESULT_INVALID_BLUETOOTH_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_BLUETOOTH_DISCONNECTED</code><br>
     *  <code>SmsManager.RESULT_UNEXPECTED_EVENT_STOP_SENDING</code><br>
     *  <code>SmsManager.RESULT_SMS_BLOCKED_DURING_EMERGENCY</code><br>
     *  <code>SmsManager.RESULT_SMS_SEND_RETRY_FAILED</code><br>
     *  <code>SmsManager.RESULT_REMOTE_EXCEPTION</code><br>
     *  <code>SmsManager.RESULT_NO_DEFAULT_SMS_APP</code><br>
     *  <code>SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE</code><br>
     *  <code>SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_REJECT</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_STATE</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_ARGUMENTS</code><br>
     *  <code>SmsManager.RESULT_RIL_NO_MEMORY</code><br>
     *  <code>SmsManager.RESULT_RIL_REQUEST_RATE_LIMITED</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_SMS_FORMAT</code><br>
     *  <code>SmsManager.RESULT_RIL_SYSTEM_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_ENCODING_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_SMSC_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_RIL_MODEM_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_INTERNAL_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_REQUEST_NOT_SUPPORTED</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_MODEM_STATE</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_NOT_READY</code><br>
     *  <code>SmsManager.RESULT_RIL_OPERATION_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RIL_NO_RESOURCES</code><br>
     *  <code>SmsManager.RESULT_RIL_CANCELLED</code><br>
     *  <code>SmsManager.RESULT_RIL_SIM_ABSENT</code><br>
     *  <code>SmsManager.RESULT_RIL_SIMULTANEOUS_SMS_AND_CALL_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RIL_ACCESS_BARRED</code><br>
     *  <code>SmsManager.RESULT_RIL_BLOCKED_DUE_TO_CALL</code><br>
     *  For <code>SmsManager.RESULT_ERROR_GENERIC_FAILURE</code> or any of the RESULT_RIL errors,
     *  the sentIntent may include the extra "errorCode" containing a radio technology specific
     *  value, generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     */
    @UnsupportedAppUsage
    protected void sendData(String callingPackage, String destAddr, String scAddr, int destPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean isForVvm) {
        SmsMessageBase.SubmitPduBase pdu = getSubmitPdu(
                scAddr, destAddr, destPort, data, (deliveryIntent != null));
        if (pdu != null) {
            HashMap map = getSmsTrackerMap(destAddr, scAddr, destPort, data, pdu);
            SmsTracker tracker = getSmsTracker(callingPackage, map, sentIntent, deliveryIntent,
                    getFormat(), null /*messageUri*/, false /*expectMore*/,
                    null /*fullMessageText*/, false /*isText*/,
                    true /*persistMessage*/, isForVvm, 0L /* messageId */);

            if (!sendSmsByCarrierApp(true /* isDataSms */, tracker)) {
                sendSubmitPdu(tracker);
            }
        } else {
            Rlog.e(TAG, "SMSDispatcher.sendData(): getSubmitPdu() returned null");
            triggerSentIntentForFailure(sentIntent);
        }
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
     *  <code>SmsManager.RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>SmsManager.RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>SmsManager.RESULT_ERROR_NULL_PDU</code><br>
     *  <code>SmsManager.RESULT_ERROR_NO_SERVICE</code><br>
     *  <code>SmsManager.RESULT_ERROR_LIMIT_EXCEEDED</code><br>
     *  <code>SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE</code><br>
     *  <code>SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RADIO_NOT_AVAILABLE</code><br>
     *  <code>SmsManager.RESULT_NETWORK_REJECT</code><br>
     *  <code>SmsManager.RESULT_INVALID_ARGUMENTS</code><br>
     *  <code>SmsManager.RESULT_INVALID_STATE</code><br>
     *  <code>SmsManager.RESULT_NO_MEMORY</code><br>
     *  <code>SmsManager.RESULT_INVALID_SMS_FORMAT</code><br>
     *  <code>SmsManager.RESULT_SYSTEM_ERROR</code><br>
     *  <code>SmsManager.RESULT_MODEM_ERROR</code><br>
     *  <code>SmsManager.RESULT_NETWORK_ERROR</code><br>
     *  <code>SmsManager.RESULT_ENCODING_ERROR</code><br>
     *  <code>SmsManager.RESULT_INVALID_SMSC_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_OPERATION_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_INTERNAL_ERROR</code><br>
     *  <code>SmsManager.RESULT_NO_RESOURCES</code><br>
     *  <code>SmsManager.RESULT_CANCELLED</code><br>
     *  <code>SmsManager.RESULT_REQUEST_NOT_SUPPORTED</code><br>
     *  <code>SmsManager.RESULT_NO_BLUETOOTH_SERVICE</code><br>
     *  <code>SmsManager.RESULT_INVALID_BLUETOOTH_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_BLUETOOTH_DISCONNECTED</code><br>
     *  <code>SmsManager.RESULT_UNEXPECTED_EVENT_STOP_SENDING</code><br>
     *  <code>SmsManager.RESULT_SMS_BLOCKED_DURING_EMERGENCY</code><br>
     *  <code>SmsManager.RESULT_SMS_SEND_RETRY_FAILED</code><br>
     *  <code>SmsManager.RESULT_REMOTE_EXCEPTION</code><br>
     *  <code>SmsManager.RESULT_NO_DEFAULT_SMS_APP</code><br>
     *  <code>SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE</code><br>
     *  <code>SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_REJECT</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_STATE</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_ARGUMENTS</code><br>
     *  <code>SmsManager.RESULT_RIL_NO_MEMORY</code><br>
     *  <code>SmsManager.RESULT_RIL_REQUEST_RATE_LIMITED</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_SMS_FORMAT</code><br>
     *  <code>SmsManager.RESULT_RIL_SYSTEM_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_ENCODING_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_SMSC_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_RIL_MODEM_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_INTERNAL_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_REQUEST_NOT_SUPPORTED</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_MODEM_STATE</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_NOT_READY</code><br>
     *  <code>SmsManager.RESULT_RIL_OPERATION_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RIL_NO_RESOURCES</code><br>
     *  <code>SmsManager.RESULT_RIL_CANCELLED</code><br>
     *  <code>SmsManager.RESULT_RIL_SIM_ABSENT</code><br>
     *  <code>SmsManager.RESULT_RIL_SIMULTANEOUS_SMS_AND_CALL_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RIL_ACCESS_BARRED</code><br>
     *  <code>SmsManager.RESULT_RIL_BLOCKED_DUE_TO_CALL</code><br>
     *  For <code>SmsManager.RESULT_ERROR_GENERIC_FAILURE</code> or any of the RESULT_RIL errors,
     *  the sentIntent may include the extra "errorCode" containing a radio technology specific
     *  value, generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     * @param messageUri optional URI of the message if it is already stored in the system
     * @param callingPkg the calling package name
     * @param persistMessage whether to save the sent message into SMS DB for a
     *  non-default SMS app.
     *
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
     * @param expectMore is a boolean to indicate the sending messages through same link or not.
     * @param validityPeriod Validity Period of the message in mins.
     *  Refer specification 3GPP TS 23.040 V6.8.1 section 9.2.3.12.1.
     *  Validity Period(Minimum) -> 5 mins
     *  Validity Period(Maximum) -> 635040 mins(i.e.63 weeks).
     *  Any Other values included Negative considered as Invalid Validity Period of the message.
     * @param messageId An id that uniquely identifies the message requested to be sent.
     *                 Used for logging and diagnostics purposes. The id may be NULL.
     */
    public void sendText(String destAddr, String scAddr, String text,
                         PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri,
                         String callingPkg, boolean persistMessage, int priority,
                         boolean expectMore, int validityPeriod, boolean isForVvm,
                         long messageId) {
        Rlog.d(TAG, "sendText id: " + SmsController.formatCrossStackMessageId(messageId));
        SmsMessageBase.SubmitPduBase pdu = getSubmitPdu(
                scAddr, destAddr, text, (deliveryIntent != null), null, priority, validityPeriod);
        if (pdu != null) {
            HashMap map = getSmsTrackerMap(destAddr, scAddr, text, pdu);
            SmsTracker tracker = getSmsTracker(callingPkg, map, sentIntent, deliveryIntent,
                    getFormat(), messageUri, expectMore, text, true /*isText*/,
                    persistMessage, priority, validityPeriod, isForVvm, messageId);

            if (!sendSmsByCarrierApp(false /* isDataSms */, tracker)) {
                sendSubmitPdu(tracker);
            }
        } else {
            Rlog.e(TAG, "SmsDispatcher.sendText(): getSubmitPdu() returned null "
                    + SmsController.formatCrossStackMessageId(messageId));
            triggerSentIntentForFailure(sentIntent);
        }
    }

    private void triggerSentIntentForFailure(PendingIntent sentIntent) {
        if (sentIntent != null) {
            try {
                sentIntent.send(SmsManager.RESULT_ERROR_GENERIC_FAILURE);
            } catch (CanceledException ex) {
                Rlog.e(TAG, "Intent has been canceled!");
            }
        }
    }

    private void triggerSentIntentForFailure(List<PendingIntent> sentIntents) {
        if (sentIntents == null) {
            return;
        }

        for (PendingIntent sentIntent : sentIntents) {
            triggerSentIntentForFailure(sentIntent);
        }
    }

    private boolean sendSmsByCarrierApp(boolean isDataSms, SmsTracker tracker ) {
        String carrierPackage = getCarrierAppPackageName();
        if (carrierPackage != null) {
            Rlog.d(TAG, "Found carrier package " + carrierPackage);
            SmsSender smsSender;
            if (isDataSms) {
                smsSender = new DataSmsSender(tracker);
            } else {
                smsSender = new TextSmsSender(tracker);
            }
            smsSender.sendSmsByCarrierApp(carrierPackage, new SmsSenderCallback(smsSender));
            return true;
        }

        return false;
    }

    protected abstract SmsMessageBase.SubmitPduBase getSubmitPdu(String scAddr, String destAddr,
            String message, boolean statusReportRequested, SmsHeader smsHeader,
            int priority, int validityPeriod);

    protected abstract SmsMessageBase.SubmitPduBase getSubmitPdu(String scAddr, String destAddr,
            int destPort, byte[] message, boolean statusReportRequested);

    /**
     * Calculate the number of septets needed to encode the message. This function should only be
     * called for individual segments of multipart message.
     *
     * @param messageBody the message to encode
     * @param use7bitOnly ignore (but still count) illegal characters if true
     * @return TextEncodingDetails
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected abstract TextEncodingDetails calculateLength(CharSequence messageBody,
            boolean use7bitOnly);

    /**
     * Send a multi-part text based SMS.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *  comprise the original message
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *  <code>PendingIntent</code>s (one for each message part) that is
     *  broadcast when the corresponding message part has been sent.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:
     *  <code>SmsManager.RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>SmsManager.RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>SmsManager.RESULT_ERROR_NULL_PDU</code><br>
     *  <code>SmsManager.RESULT_ERROR_NO_SERVICE</code><br>
     *  <code>SmsManager.RESULT_ERROR_LIMIT_EXCEEDED</code><br>
     *  <code>SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE</code><br>
     *  <code>SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RADIO_NOT_AVAILABLE</code><br>
     *  <code>SmsManager.RESULT_NETWORK_REJECT</code><br>
     *  <code>SmsManager.RESULT_INVALID_ARGUMENTS</code><br>
     *  <code>SmsManager.RESULT_INVALID_STATE</code><br>
     *  <code>SmsManager.RESULT_NO_MEMORY</code><br>
     *  <code>SmsManager.RESULT_INVALID_SMS_FORMAT</code><br>
     *  <code>SmsManager.RESULT_SYSTEM_ERROR</code><br>
     *  <code>SmsManager.RESULT_MODEM_ERROR</code><br>
     *  <code>SmsManager.RESULT_NETWORK_ERROR</code><br>
     *  <code>SmsManager.RESULT_ENCODING_ERROR</code><br>
     *  <code>SmsManager.RESULT_INVALID_SMSC_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_OPERATION_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_INTERNAL_ERROR</code><br>
     *  <code>SmsManager.RESULT_NO_RESOURCES</code><br>
     *  <code>SmsManager.RESULT_CANCELLED</code><br>
     *  <code>SmsManager.RESULT_REQUEST_NOT_SUPPORTED</code><br>
     *  <code>SmsManager.RESULT_NO_BLUETOOTH_SERVICE</code><br>
     *  <code>SmsManager.RESULT_INVALID_BLUETOOTH_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_BLUETOOTH_DISCONNECTED</code><br>
     *  <code>SmsManager.RESULT_UNEXPECTED_EVENT_STOP_SENDING</code><br>
     *  <code>SmsManager.RESULT_SMS_BLOCKED_DURING_EMERGENCY</code><br>
     *  <code>SmsManager.RESULT_SMS_SEND_RETRY_FAILED</code><br>
     *  <code>SmsManager.RESULT_REMOTE_EXCEPTION</code><br>
     *  <code>SmsManager.RESULT_NO_DEFAULT_SMS_APP</code><br>
     *  <code>SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE</code><br>
     *  <code>SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_REJECT</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_STATE</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_ARGUMENTS</code><br>
     *  <code>SmsManager.RESULT_RIL_NO_MEMORY</code><br>
     *  <code>SmsManager.RESULT_RIL_REQUEST_RATE_LIMITED</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_SMS_FORMAT</code><br>
     *  <code>SmsManager.RESULT_RIL_SYSTEM_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_ENCODING_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_SMSC_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_RIL_MODEM_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_INTERNAL_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_REQUEST_NOT_SUPPORTED</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_MODEM_STATE</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_NOT_READY</code><br>
     *  <code>SmsManager.RESULT_RIL_OPERATION_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RIL_NO_RESOURCES</code><br>
     *  <code>SmsManager.RESULT_RIL_CANCELLED</code><br>
     *  <code>SmsManager.RESULT_RIL_SIM_ABSENT</code><br>
     *  <code>SmsManager.RESULT_RIL_SIMULTANEOUS_SMS_AND_CALL_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RIL_ACCESS_BARRED</code><br>
     *  <code>SmsManager.RESULT_RIL_BLOCKED_DUE_TO_CALL</code><br>
     *  For <code>SmsManager.RESULT_ERROR_GENERIC_FAILURE</code> or any of the RESULT_RIL errors,
     *  the sentIntent may include the extra "errorCode" containing a radio technology specific
     *  value, generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *  <code>PendingIntent</code>s (one for each message part) that is
     *  broadcast when the corresponding message part has been delivered
     *  to the recipient.  The raw pdu of the status report is in the
     * @param messageUri optional URI of the message if it is already stored in the system
     * @param callingPkg the calling package name
     * @param persistMessage whether to save the sent message into SMS DB for a
     *   non-default SMS app.
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
     * @param expectMore is a boolean to indicate the sending messages through same link or not.
     * @param validityPeriod Validity Period of the message in mins.
     *  Refer specification 3GPP TS 23.040 V6.8.1 section 9.2.3.12.1.
     *  Validity Period(Minimum) -> 5 mins
     *  Validity Period(Maximum) -> 635040 mins(i.e.63 weeks).
     *  Any Other values included Negative considered as Invalid Validity Period of the message.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PROTECTED)
    public void sendMultipartText(String destAddr, String scAddr,
            ArrayList<String> parts, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents, Uri messageUri, String callingPkg,
            boolean persistMessage, int priority, boolean expectMore, int validityPeriod,
            long messageId) {
        final String fullMessageText = getMultipartMessageText(parts);
        int refNumber = getNextConcatenatedRef() & 0x00FF;
        int encoding = SmsConstants.ENCODING_UNKNOWN;
        int msgCount = parts.size();
        if (msgCount < 1) {
            triggerSentIntentForFailure(sentIntents);
            return;
        }

        TextEncodingDetails[] encodingForParts = new TextEncodingDetails[msgCount];
        for (int i = 0; i < msgCount; i++) {
            TextEncodingDetails details = calculateLength(parts.get(i), false);
            if (encoding != details.codeUnitSize
                    && (encoding == SmsConstants.ENCODING_UNKNOWN
                            || encoding == SmsConstants.ENCODING_7BIT)) {
                encoding = details.codeUnitSize;
            }
            encodingForParts[i] = details;
        }

        SmsTracker[] trackers = new SmsTracker[msgCount];

        // States to track at the message level (for all parts)
        final AtomicInteger unsentPartCount = new AtomicInteger(msgCount);
        final AtomicBoolean anyPartFailed = new AtomicBoolean(false);

        for (int i = 0; i < msgCount; i++) {
            SmsHeader.ConcatRef concatRef = new SmsHeader.ConcatRef();
            concatRef.refNumber = refNumber;
            concatRef.seqNumber = i + 1;  // 1-based sequence
            concatRef.msgCount = msgCount;
            // TODO: We currently set this to true since our messaging app will never
            // send more than 255 parts (it converts the message to MMS well before that).
            // However, we should support 3rd party messaging apps that might need 16-bit
            // references
            // Note:  It's not sufficient to just flip this bit to true; it will have
            // ripple effects (several calculations assume 8-bit ref).
            concatRef.isEightBits = true;
            SmsHeader smsHeader = new SmsHeader();
            smsHeader.concatRef = concatRef;

            // Set the national language tables for 3GPP 7-bit encoding, if enabled.
            if (encoding == SmsConstants.ENCODING_7BIT) {
                smsHeader.languageTable = encodingForParts[i].languageTable;
                smsHeader.languageShiftTable = encodingForParts[i].languageShiftTable;
            }

            PendingIntent sentIntent = null;
            if (sentIntents != null && sentIntents.size() > i) {
                sentIntent = sentIntents.get(i);
            }

            PendingIntent deliveryIntent = null;
            if (deliveryIntents != null && deliveryIntents.size() > i) {
                deliveryIntent = deliveryIntents.get(i);
            }

            trackers[i] =
                getNewSubmitPduTracker(callingPkg, destAddr, scAddr, parts.get(i), smsHeader,
                        encoding, sentIntent, deliveryIntent, (i == (msgCount - 1)),
                        unsentPartCount, anyPartFailed, messageUri,
                        fullMessageText, priority, expectMore, validityPeriod, messageId);
            if (trackers[i] == null) {
                triggerSentIntentForFailure(sentIntents);
                return;
            }
            trackers[i].mPersistMessage = persistMessage;
        }

        String carrierPackage = getCarrierAppPackageName();
        if (carrierPackage != null) {
            Rlog.d(TAG, "Found carrier package " + carrierPackage
                    + " "
                    + SmsController.formatCrossStackMessageId(getMultiTrackermessageId(trackers)));
            MultipartSmsSender smsSender = new MultipartSmsSender(parts, trackers);
            smsSender.sendSmsByCarrierApp(carrierPackage,
                    new MultipartSmsSenderCallback(smsSender));
        } else {
            Rlog.v(TAG, "No carrier package. "
                    + SmsController.formatCrossStackMessageId(getMultiTrackermessageId(trackers)));
            sendSubmitPdu(trackers);
        }
    }

    private long getMultiTrackermessageId(SmsTracker[] trackers) {
        if (trackers.length == 0) {
            return 0L;
        }
        return trackers[0].mMessageId;
    }

    /**
     * Create a new SubmitPdu and return the SMS tracker.
     */
    private SmsTracker getNewSubmitPduTracker(String callingPackage, String destinationAddress,
            String scAddress, String message, SmsHeader smsHeader, int encoding,
            PendingIntent sentIntent, PendingIntent deliveryIntent, boolean lastPart,
            AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri,
            String fullMessageText, int priority, boolean expectMore, int validityPeriod,
            long messageId) {
        if (isCdmaMo()) {
            UserData uData = new UserData();
            uData.payloadStr = message;
            uData.userDataHeader = smsHeader;
            if (encoding == SmsConstants.ENCODING_7BIT) {
                uData.msgEncoding = isAscii7bitSupportedForLongMessage()
                        ? UserData.ENCODING_7BIT_ASCII : UserData.ENCODING_GSM_7BIT_ALPHABET;
                Rlog.d(TAG, "Message encoding for proper 7 bit: " + uData.msgEncoding);
            } else { // assume UTF-16
                uData.msgEncoding = UserData.ENCODING_UNICODE_16;
            }
            uData.msgEncodingSet = true;

            /* By setting the statusReportRequested bit only for the
             * last message fragment, this will result in only one
             * callback to the sender when that last fragment delivery
             * has been acknowledged. */
            //TODO FIX
            SmsMessageBase.SubmitPduBase submitPdu =
                    com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(destinationAddress,
                            uData, (deliveryIntent != null) && lastPart, priority);

            if (submitPdu != null) {
                HashMap map = getSmsTrackerMap(destinationAddress, scAddress,
                        message, submitPdu);
                return getSmsTracker(callingPackage, map, sentIntent, deliveryIntent,
                        getFormat(), unsentPartCount, anyPartFailed, messageUri, smsHeader,
                        (!lastPart || expectMore), fullMessageText, true /*isText*/,
                        true /*persistMessage*/, priority, validityPeriod, false /* isForVvm */,
                        messageId);
            } else {
                Rlog.e(TAG, "CdmaSMSDispatcher.getNewSubmitPduTracker(): getSubmitPdu() returned "
                        + "null " + SmsController.formatCrossStackMessageId(messageId));
                return null;
            }
        } else {
            SmsMessageBase.SubmitPduBase pdu =
                    com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddress,
                            destinationAddress, message, deliveryIntent != null,
                            SmsHeader.toByteArray(smsHeader), encoding, smsHeader.languageTable,
                            smsHeader.languageShiftTable, validityPeriod);
            if (pdu != null) {
                HashMap map =  getSmsTrackerMap(destinationAddress, scAddress,
                        message, pdu);
                return getSmsTracker(callingPackage, map, sentIntent,
                        deliveryIntent, getFormat(), unsentPartCount, anyPartFailed, messageUri,
                        smsHeader, (!lastPart || expectMore), fullMessageText, true /*isText*/,
                        false /*persistMessage*/, priority, validityPeriod, false /* isForVvm */,
                        messageId);
            } else {
                Rlog.e(TAG, "GsmSMSDispatcher.getNewSubmitPduTracker(): getSubmitPdu() returned "
                        + "null " + SmsController.formatCrossStackMessageId(messageId));
                return null;
            }
        }
    }

    /**
     * Send a single or a multi-part SMS
     *
     * @param trackers each tracker will contain:
     * -smsc the SMSC to send the message through, or NULL for the
     *  default SMSC
     * -pdu the raw PDU to send
     * -sentIntent if not NULL this <code>Intent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:
     *  <code>SmsManager.RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>SmsManager.RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>SmsManager.RESULT_ERROR_NULL_PDU</code><br>
     *  <code>SmsManager.RESULT_ERROR_NO_SERVICE</code><br>
     *  <code>SmsManager.RESULT_ERROR_LIMIT_EXCEEDED</code><br>
     *  <code>SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE</code><br>
     *  <code>SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RADIO_NOT_AVAILABLE</code><br>
     *  <code>SmsManager.RESULT_NETWORK_REJECT</code><br>
     *  <code>SmsManager.RESULT_INVALID_ARGUMENTS</code><br>
     *  <code>SmsManager.RESULT_INVALID_STATE</code><br>
     *  <code>SmsManager.RESULT_NO_MEMORY</code><br>
     *  <code>SmsManager.RESULT_INVALID_SMS_FORMAT</code><br>
     *  <code>SmsManager.RESULT_SYSTEM_ERROR</code><br>
     *  <code>SmsManager.RESULT_MODEM_ERROR</code><br>
     *  <code>SmsManager.RESULT_NETWORK_ERROR</code><br>
     *  <code>SmsManager.RESULT_ENCODING_ERROR</code><br>
     *  <code>SmsManager.RESULT_INVALID_SMSC_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_OPERATION_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_INTERNAL_ERROR</code><br>
     *  <code>SmsManager.RESULT_NO_RESOURCES</code><br>
     *  <code>SmsManager.RESULT_CANCELLED</code><br>
     *  <code>SmsManager.RESULT_REQUEST_NOT_SUPPORTED</code><br>
     *  <code>SmsManager.RESULT_NO_BLUETOOTH_SERVICE</code><br>
     *  <code>SmsManager.RESULT_INVALID_BLUETOOTH_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_BLUETOOTH_DISCONNECTED</code><br>
     *  <code>SmsManager.RESULT_UNEXPECTED_EVENT_STOP_SENDING</code><br>
     *  <code>SmsManager.RESULT_SMS_BLOCKED_DURING_EMERGENCY</code><br>
     *  <code>SmsManager.RESULT_SMS_SEND_RETRY_FAILED</code><br>
     *  <code>SmsManager.RESULT_REMOTE_EXCEPTION</code><br>
     *  <code>SmsManager.RESULT_NO_DEFAULT_SMS_APP</code><br>
     *  <code>SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE</code><br>
     *  <code>SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_REJECT</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_STATE</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_ARGUMENTS</code><br>
     *  <code>SmsManager.RESULT_RIL_NO_MEMORY</code><br>
     *  <code>SmsManager.RESULT_RIL_REQUEST_RATE_LIMITED</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_SMS_FORMAT</code><br>
     *  <code>SmsManager.RESULT_RIL_SYSTEM_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_ENCODING_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_SMSC_ADDRESS</code><br>
     *  <code>SmsManager.RESULT_RIL_MODEM_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_INTERNAL_ERR</code><br>
     *  <code>SmsManager.RESULT_RIL_REQUEST_NOT_SUPPORTED</code><br>
     *  <code>SmsManager.RESULT_RIL_INVALID_MODEM_STATE</code><br>
     *  <code>SmsManager.RESULT_RIL_NETWORK_NOT_READY</code><br>
     *  <code>SmsManager.RESULT_RIL_OPERATION_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RIL_NO_RESOURCES</code><br>
     *  <code>SmsManager.RESULT_RIL_CANCELLED</code><br>
     *  <code>SmsManager.RESULT_RIL_SIM_ABSENT</code><br>
     *  <code>SmsManager.RESULT_RIL_SIMULTANEOUS_SMS_AND_CALL_NOT_ALLOWED</code><br>
     *  <code>SmsManager.RESULT_RIL_ACCESS_BARRED</code><br>
     *  <code>SmsManager.RESULT_RIL_BLOCKED_DUE_TO_CALL</code><br>
     *  For <code>SmsManager.RESULT_ERROR_GENERIC_FAILURE</code> or any of the RESULT_RIL errors,
     *  the sentIntent may include the extra "errorCode" containing a radio technology specific
     *  value, generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * -deliveryIntent if not NULL this <code>Intent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * -param destAddr the destination phone number (for short code confirmation)
     */
    @VisibleForTesting
    public void sendRawPdu(SmsTracker[] trackers) {
        @SmsManager.Result int error = SmsManager.RESULT_ERROR_NONE;
        PackageInfo appInfo = null;
        if (mSmsSendDisabled) {
            Rlog.e(TAG, "Device does not support sending sms.");
            error = SmsManager.RESULT_ERROR_NO_SERVICE;
        } else {
            for (SmsTracker tracker : trackers) {
                if (tracker.getData().get(MAP_KEY_PDU) == null) {
                    Rlog.e(TAG, "Empty PDU");
                    error = SmsManager.RESULT_ERROR_NULL_PDU;
                    break;
                }
            }

            if (error == SmsManager.RESULT_ERROR_NONE) {
                UserHandle userHandle = UserHandle.of(trackers[0].mUserId);
                PackageManager pm = mContext.createContextAsUser(userHandle, 0).getPackageManager();

                try {
                    // Get package info via packagemanager
                    appInfo =
                            pm.getPackageInfo(
                                    trackers[0].getAppPackageName(),
                                    PackageManager.GET_SIGNATURES);
                } catch (PackageManager.NameNotFoundException e) {
                    Rlog.e(TAG, "Can't get calling app package info: refusing to send SMS "
                            + SmsController.formatCrossStackMessageId(
                                    getMultiTrackermessageId(trackers)));
                    error = SmsManager.RESULT_ERROR_GENERIC_FAILURE;
                }
            }
        }

        if (error != SmsManager.RESULT_ERROR_NONE) {
            handleSmsTrackersFailure(trackers, error, NO_ERROR_CODE);
            return;
        }

        // checkDestination() returns true if the destination is not a premium short code or the
        // sending app is approved to send to short codes. Otherwise, a message is sent to our
        // handler with the SmsTracker to request user confirmation before sending.
        if (checkDestination(trackers)) {
            // check for excessive outgoing SMS usage by this app
            if (!mSmsDispatchersController
                    .getUsageMonitor()
                    .check(appInfo.packageName, trackers.length)) {
                sendMessage(obtainMessage(EVENT_SEND_LIMIT_REACHED_CONFIRMATION, trackers));
                return;
            }

            for (SmsTracker tracker : trackers) {
                sendSms(tracker);
            }
        }

        if (mTelephonyManager.isEmergencyNumber(trackers[0].mDestAddress)) {
            new AsyncEmergencyContactNotifier(mContext).execute();
        }
    }

    /**
     * Check if destination is a potential premium short code and sender is not pre-approved to send
     * to short codes.
     *
     * @param trackers the trackers for a single or a multi-part SMS to send
     * @return true if the destination is approved; false if user confirmation event was sent
     */
    boolean checkDestination(SmsTracker[] trackers) {
        if (mContext.checkCallingOrSelfPermission(SEND_SMS_NO_CONFIRMATION)
                == PackageManager.PERMISSION_GRANTED || trackers[0].mIsForVvm) {
            return true;            // app is pre-approved to send to short codes
        } else {
            int rule = mPremiumSmsRule.get();
            int smsCategory = SmsManager.SMS_CATEGORY_NOT_SHORT_CODE;
            if (rule == PREMIUM_RULE_USE_SIM || rule == PREMIUM_RULE_USE_BOTH) {
                String simCountryIso =
                        mTelephonyManager.getSimCountryIsoForPhone(mPhone.getPhoneId());
                if (simCountryIso == null || simCountryIso.length() != 2) {
                    Rlog.e(TAG, "Can't get SIM country Iso: trying network country Iso "
                            + SmsController.formatCrossStackMessageId(
                                    getMultiTrackermessageId(trackers)));
                    simCountryIso =
                            mTelephonyManager.getNetworkCountryIso(mPhone.getPhoneId());
                }

                smsCategory =
                        mSmsDispatchersController
                                .getUsageMonitor()
                                .checkDestination(trackers[0].mDestAddress, simCountryIso);
            }
            if (rule == PREMIUM_RULE_USE_NETWORK || rule == PREMIUM_RULE_USE_BOTH) {
                String networkCountryIso =
                        mTelephonyManager.getNetworkCountryIso(mPhone.getPhoneId());
                if (networkCountryIso == null || networkCountryIso.length() != 2) {
                    Rlog.e(TAG, "Can't get Network country Iso: trying SIM country Iso "
                            + SmsController.formatCrossStackMessageId(
                                    getMultiTrackermessageId(trackers)));
                    networkCountryIso =
                            mTelephonyManager.getSimCountryIsoForPhone(mPhone.getPhoneId());
                }

                smsCategory =
                        SmsUsageMonitor.mergeShortCodeCategories(
                                smsCategory,
                                mSmsDispatchersController
                                        .getUsageMonitor()
                                        .checkDestination(
                                                trackers[0].mDestAddress, networkCountryIso));
            }

            if (smsCategory == SmsManager.SMS_CATEGORY_NOT_SHORT_CODE
                    || smsCategory == SmsManager.SMS_CATEGORY_FREE_SHORT_CODE
                    || smsCategory == SmsManager.SMS_CATEGORY_STANDARD_SHORT_CODE) {
                return true;    // not a premium short code
            }

            // Do not allow any premium sms during SuW
            if (Settings.Global.getInt(mResolver, Settings.Global.DEVICE_PROVISIONED, 0) == 0) {
                Rlog.e(TAG, "Can't send premium sms during Setup Wizard "
                        + SmsController.formatCrossStackMessageId(
                                getMultiTrackermessageId(trackers)));
                return false;
            }

            // Wait for user confirmation unless the user has set permission to always allow/deny
            int premiumSmsPermission =
                    mSmsDispatchersController
                            .getUsageMonitor()
                            .getPremiumSmsPermission(trackers[0].getAppPackageName());
            if (premiumSmsPermission == SmsUsageMonitor.PREMIUM_SMS_PERMISSION_UNKNOWN) {
                // First time trying to send to premium SMS.
                premiumSmsPermission = SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ASK_USER;
            }

            switch (premiumSmsPermission) {
                case SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW:
                    Rlog.d(TAG, "User approved this app to send to premium SMS "
                            + SmsController.formatCrossStackMessageId(
                                    getMultiTrackermessageId(trackers)));
                    return true;

                case SmsUsageMonitor.PREMIUM_SMS_PERMISSION_NEVER_ALLOW:
                    Rlog.w(TAG, "User denied this app from sending to premium SMS "
                            + SmsController.formatCrossStackMessageId(
                                    getMultiTrackermessageId(trackers)));
                    Message msg = obtainMessage(EVENT_SENDING_NOT_ALLOWED, trackers);
                    sendMessage(msg);
                    return false;   // reject this message

                case SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ASK_USER:
                default:
                    int event;
                    if (smsCategory == SmsManager.SMS_CATEGORY_POSSIBLE_PREMIUM_SHORT_CODE) {
                        event = EVENT_CONFIRM_SEND_TO_POSSIBLE_PREMIUM_SHORT_CODE;
                    } else {
                        event = EVENT_CONFIRM_SEND_TO_PREMIUM_SHORT_CODE;
                    }
                    sendMessage(obtainMessage(event, trackers));
                    return false;   // wait for user confirmation
            }
        }
    }

    /**
     * Deny sending a single or a multi-part SMS if the outgoing queue limit is reached. Used when
     * the message must be confirmed by the user due to excessive usage or potential premium SMS
     * detected.
     *
     * @param trackers the SmsTracker array for the message to send
     * @return true if the message was denied; false to continue with send confirmation
     */
    private boolean denyIfQueueLimitReached(SmsTracker[] trackers) {
        // one SmsTracker array is treated as one message for checking queue limit.
        if (mPendingTrackerCount >= MO_MSG_QUEUE_LIMIT) {
            // Deny sending message when the queue limit is reached.
            Rlog.e(TAG, "Denied because queue limit reached "
                    + SmsController.formatCrossStackMessageId(getMultiTrackermessageId(trackers)));
            handleSmsTrackersFailure(
                    trackers, SmsManager.RESULT_ERROR_LIMIT_EXCEEDED, NO_ERROR_CODE);
            return true;
        }
        mPendingTrackerCount++;
        return false;
    }

    /**
     * Returns the label for the specified app package name.
     * @param appPackage the package name of the app requesting to send an SMS
     * @return the label for the specified app, or the package name if getApplicationInfo() fails
     */
    private CharSequence getAppLabel(String appPackage, @UserIdInt int userId) {
        PackageManager pm = mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfoAsUser(appPackage, 0,
                    UserHandle.of(userId));
            return appInfo.loadSafeLabel(pm);
        } catch (PackageManager.NameNotFoundException e) {
            Rlog.e(TAG, "PackageManager Name Not Found for package " + appPackage);
            return appPackage;  // fall back to package name if we can't get app label
        }
    }

    /**
     * Post an alert when SMS needs confirmation due to excessive usage.
     *
     * @param trackers the SmsTracker array for the current message.
     */
    protected void handleReachSentLimit(SmsTracker[] trackers) {
        if (denyIfQueueLimitReached(trackers)) {
            return;     // queue limit reached; error was returned to caller
        }

        CharSequence appLabel = getAppLabel(trackers[0].getAppPackageName(), trackers[0].mUserId);
        Resources r = Resources.getSystem();
        Spanned messageText = Html.fromHtml(r.getString(R.string.sms_control_message, appLabel));

        // Construct ConfirmDialogListenter for Rate Limit handling
        ConfirmDialogListener listener =
                new ConfirmDialogListener(trackers, null, ConfirmDialogListener.RATE_LIMIT);

        AlertDialog d = new AlertDialog.Builder(mContext)
                .setTitle(R.string.sms_control_title)
                .setIcon(R.drawable.stat_sys_warning)
                .setMessage(messageText)
                .setPositiveButton(r.getString(R.string.sms_control_yes), listener)
                .setNegativeButton(r.getString(R.string.sms_control_no), listener)
                .setOnCancelListener(listener)
                .create();

        d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        d.show();
    }

    /**
     * Post an alert for user confirmation when sending to a potential short code.
     *
     * @param isPremium true if the destination is known to be a premium short code
     * @param trackers the SmsTracker array for the current message.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void handleConfirmShortCode(boolean isPremium, SmsTracker[] trackers) {
        if (denyIfQueueLimitReached(trackers)) {
            return;     // queue limit reached; error was returned to caller
        }

        int detailsId;
        if (isPremium) {
            detailsId = R.string.sms_premium_short_code_details;
        } else {
            detailsId = R.string.sms_short_code_details;
        }

        CharSequence appLabel = getAppLabel(trackers[0].getAppPackageName(), trackers[0].mUserId);
        Resources r = Resources.getSystem();
        Spanned messageText =
                Html.fromHtml(
                        r.getString(
                                R.string.sms_short_code_confirm_message,
                                appLabel,
                                trackers[0].mDestAddress));

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.sms_short_code_confirmation_dialog, null);

        // Construct ConfirmDialogListenter for short code message sending
        ConfirmDialogListener listener =
                new ConfirmDialogListener(
                        trackers,
                        (TextView)
                                layout.findViewById(R.id.sms_short_code_remember_undo_instruction),
                        ConfirmDialogListener.SHORT_CODE_MSG);

        TextView messageView = (TextView) layout.findViewById(R.id.sms_short_code_confirm_message);
        messageView.setText(messageText);

        ViewGroup detailsLayout = (ViewGroup) layout.findViewById(
                R.id.sms_short_code_detail_layout);
        TextView detailsView = (TextView) detailsLayout.findViewById(
                R.id.sms_short_code_detail_message);
        detailsView.setText(detailsId);

        CheckBox rememberChoice = (CheckBox) layout.findViewById(
                R.id.sms_short_code_remember_choice_checkbox);
        rememberChoice.setOnCheckedChangeListener(listener);

        AlertDialog d = new AlertDialog.Builder(mContext)
                .setView(layout)
                .setPositiveButton(r.getString(R.string.sms_short_code_confirm_allow), listener)
                .setNegativeButton(r.getString(R.string.sms_short_code_confirm_deny), listener)
                .setOnCancelListener(listener)
                .create();

        d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        d.show();

        listener.setPositiveButton(d.getButton(DialogInterface.BUTTON_POSITIVE));
        listener.setNegativeButton(d.getButton(DialogInterface.BUTTON_NEGATIVE));
    }

    /**
     * Send the message along to the radio.
     *
     * @param tracker holds the SMS message to send
     */
    @UnsupportedAppUsage
    protected abstract void sendSms(SmsTracker tracker);

    /**
     * Retry the message along to the radio.
     *
     * @param tracker holds the SMS message to send
     */
    public void sendRetrySms(SmsTracker tracker) {
        // re-routing to SmsDispatchersController
        if (mSmsDispatchersController != null) {
            mSmsDispatchersController.sendRetrySms(tracker);
        } else {
            Rlog.e(TAG, mSmsDispatchersController + " is null. Retry failed "
                    + SmsController.formatCrossStackMessageId(tracker.mMessageId));
        }
    }

    private void handleSmsTrackersFailure(SmsTracker[] trackers, @SmsManager.Result int error,
            int errorCode) {
        for (SmsTracker tracker : trackers) {
            tracker.onFailed(mContext, error, errorCode);
        }
        if (trackers.length > 0) {
            // This error occurs before the SMS is sent. Make an assumption if it would have
            // been sent over IMS or not.
            mPhone.getSmsStats().onOutgoingSms(
                    isIms(),
                    SmsConstants.FORMAT_3GPP2.equals(getFormat()),
                    false /* fallbackToCs */,
                    error,
                    trackers[0].mMessageId,
                    trackers[0].isFromDefaultSmsApplication(mContext),
                    trackers[0].getInterval());
        }
    }

    /**
     * Keeps track of an SMS that has been sent to the RIL, until it has
     * successfully been sent, or we're done trying.
     */
    public static class SmsTracker {
        // fields need to be public for derived SmsDispatchers
        @UnsupportedAppUsage
        private final HashMap<String, Object> mData;
        public int mRetryCount;
        // IMS retry counter. Nonzero indicates initial message was sent over IMS channel in RIL and
        // counts how many retries have been made on the IMS channel.
        // Used in older implementations where the message is sent over IMS using the RIL.
        public int mImsRetry;
        // Tag indicating that this SMS is being handled by the ImsSmsDispatcher. This tracker
        // should not try to use SMS over IMS over the RIL interface in this case when falling back.
        public boolean mUsesImsServiceForIms;
        @UnsupportedAppUsage
        public int mMessageRef;
        public boolean mExpectMore;
        public int mValidityPeriod;
        public int mPriority;
        String mFormat;

        @UnsupportedAppUsage
        public final PendingIntent mSentIntent;
        @UnsupportedAppUsage
        public final PendingIntent mDeliveryIntent;

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public final PackageInfo mAppInfo;
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public final String mDestAddress;

        public final SmsHeader mSmsHeader;

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private long mTimestamp = SystemClock.elapsedRealtime();
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public Uri mMessageUri; // Uri of persisted message if we wrote one

        // Reference to states of a multipart message that this part belongs to
        private AtomicInteger mUnsentPartCount;
        private AtomicBoolean mAnyPartFailed;
        // The full message content of a single part message
        // or a multipart message that this part belongs to
        private String mFullMessageText;

        private int mSubId;

        // If this is a text message (instead of data message)
        private boolean mIsText;

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private boolean mPersistMessage;

        // User who sends the SMS.
        private final @UserIdInt int mUserId;

        private final boolean mIsForVvm;

        public final long mMessageId;

        private Boolean mIsFromDefaultSmsApplication;

        private int mCarrierId;
        // SMS anomaly uuid -- unexpected error from RIL
        private final UUID mAnomalyUnexpectedErrorFromRilUUID =
                UUID.fromString("43043600-ea7a-44d2-9ae6-a58567ac7886");

        private SmsTracker(HashMap<String, Object> data, PendingIntent sentIntent,
                PendingIntent deliveryIntent, PackageInfo appInfo, String destAddr, String format,
                AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri,
                SmsHeader smsHeader, boolean expectMore, String fullMessageText, int subId,
                boolean isText, boolean persistMessage, int userId, int priority,
                int validityPeriod, boolean isForVvm, long messageId, int carrierId) {
            mData = data;
            mSentIntent = sentIntent;
            mDeliveryIntent = deliveryIntent;
            mRetryCount = 0;
            mAppInfo = appInfo;
            mDestAddress = destAddr;
            mFormat = format;
            mExpectMore = expectMore;
            mImsRetry = 0;
            mUsesImsServiceForIms = false;
            mMessageRef = 0;
            mUnsentPartCount = unsentPartCount;
            mAnyPartFailed = anyPartFailed;
            mMessageUri = messageUri;
            mSmsHeader = smsHeader;
            mFullMessageText = fullMessageText;
            mSubId = subId;
            mIsText = isText;
            mPersistMessage = persistMessage;
            mUserId = userId;
            mPriority = priority;
            mValidityPeriod = validityPeriod;
            mIsForVvm = isForVvm;
            mMessageId = messageId;
            mCarrierId = carrierId;
        }

        public HashMap<String, Object> getData() {
            return mData;
        }

        /**
         * Get the App package name
         * @return App package name info
         */
        public String getAppPackageName() {
            return mAppInfo != null ? mAppInfo.packageName : null;
        }

        /** Return if the SMS was originated from the default SMS application. */
        public boolean isFromDefaultSmsApplication(Context context) {
            if (mIsFromDefaultSmsApplication == null) {
                // Perform a lazy initialization, due to the cost of the operation.
                mIsFromDefaultSmsApplication =
                        SmsApplication.isDefaultSmsApplication(context, getAppPackageName());
            }
            return mIsFromDefaultSmsApplication;
        }

        /**
         * Update the status of this message if we persisted it
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public void updateSentMessageStatus(Context context, int status) {
            if (mMessageUri != null) {
                // If we wrote this message in writeSentMessage, update it now
                ContentValues values = new ContentValues(1);
                values.put(Sms.STATUS, status);
                context.getContentResolver().update(mMessageUri, values, null, null);
            }
        }

        /**
         * Set the final state of a message: FAILED or SENT
         *
         * @param context The Context
         * @param messageType The final message type
         * @param errorCode The error code
         */
        private void updateMessageState(Context context, int messageType, int errorCode) {
            if (mMessageUri == null) {
                return;
            }
            final ContentValues values = new ContentValues(2);
            values.put(Sms.TYPE, messageType);
            values.put(Sms.ERROR_CODE, errorCode);
            final long identity = Binder.clearCallingIdentity();
            try {
                if (context.getContentResolver().update(mMessageUri, values,
                        null/*where*/, null/*selectionArgs*/) != 1) {
                    Rlog.e(TAG, "Failed to move message to " + messageType);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /**
         * Returns the interval in milliseconds between sending the message out and current time.
         * Called after receiving success/failure response to calculate the time
         * to complete the SMS send to the network.
         */
        protected long getInterval() {
            return SystemClock.elapsedRealtime() - mTimestamp;
        }

        /**
         * Persist a sent SMS if required:
         * 1. It is a text message
         * 2. SmsApplication tells us to persist: sent from apps that are not default-SMS app or
         *    bluetooth
         *
         * @param context
         * @param messageType The folder to store (FAILED or SENT)
         * @param errorCode The current error code for this SMS or SMS part
         * @return The telephony provider URI if stored
         */
        private Uri persistSentMessageIfRequired(Context context, int messageType, int errorCode) {
            if (!mIsText || !mPersistMessage || isFromDefaultSmsApplication(context)) {
                return null;
            }
            Rlog.d(TAG, "Persist SMS into "
                    + (messageType == Sms.MESSAGE_TYPE_FAILED ? "FAILED" : "SENT"));
            final ContentValues values = new ContentValues();
            values.put(Sms.SUBSCRIPTION_ID, mSubId);
            values.put(Sms.ADDRESS, mDestAddress);
            values.put(Sms.BODY, mFullMessageText);
            values.put(Sms.DATE, System.currentTimeMillis()); // milliseconds
            values.put(Sms.SEEN, 1);
            values.put(Sms.READ, 1);
            final String creator = mAppInfo != null ? mAppInfo.packageName : null;
            if (!TextUtils.isEmpty(creator)) {
                values.put(Sms.CREATOR, creator);
            }
            if (mDeliveryIntent != null) {
                values.put(Sms.STATUS, Telephony.Sms.STATUS_PENDING);
            }
            if (errorCode != NO_ERROR_CODE) {
                values.put(Sms.ERROR_CODE, errorCode);
            }
            final long identity = Binder.clearCallingIdentity();
            final ContentResolver resolver = context.getContentResolver();
            try {
                final Uri uri =  resolver.insert(Telephony.Sms.Sent.CONTENT_URI, values);
                if (uri != null && messageType == Sms.MESSAGE_TYPE_FAILED) {
                    // Since we can't persist a message directly into FAILED box,
                    // we have to update the column after we persist it into SENT box.
                    // The gap between the state change is tiny so I would not expect
                    // it to cause any serious problem
                    // TODO: we should add a "failed" URI for this in SmsProvider?
                    final ContentValues updateValues = new ContentValues(1);
                    updateValues.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
                    resolver.update(uri, updateValues, null/*where*/, null/*selectionArgs*/);
                }
                return uri;
            } catch (Exception e) {
                Rlog.e(TAG, "writeOutboxMessage: Failed to persist outbox message", e);
                return null;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /**
         * Persist or update an SMS depending on if we send a new message or a stored message
         *
         * @param context
         * @param messageType The message folder for this SMS, FAILED or SENT
         * @param errorCode The current error code for this SMS or SMS part
         */
        private void persistOrUpdateMessage(Context context, int messageType, int errorCode) {
            if (mMessageUri != null) {
                updateMessageState(context, messageType, errorCode);
            } else {
                mMessageUri = persistSentMessageIfRequired(context, messageType, errorCode);
            }
        }

        /**
         * Handle a failure of a single part message or a part of a multipart message
         *
         * @param context The Context
         * @param error The error to send back with
         * @param errorCode
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public void onFailed(Context context, int error, int errorCode) {
            if (mAnyPartFailed != null) {
                mAnyPartFailed.set(true);
            }
            // is single part or last part of multipart message
            boolean isSinglePartOrLastPart = true;
            if (mUnsentPartCount != null) {
                isSinglePartOrLastPart = mUnsentPartCount.decrementAndGet() == 0;
            }
            if (isSinglePartOrLastPart) {
                persistOrUpdateMessage(context, Sms.MESSAGE_TYPE_FAILED, errorCode);
            }
            if (mSentIntent != null) {
                try {
                    // Extra information to send with the sent intent
                    Intent fillIn = new Intent();
                    if (mMessageUri != null) {
                        // Pass this to SMS apps so that they know where it is stored
                        fillIn.putExtra("uri", mMessageUri.toString());
                    }
                    if (errorCode != NO_ERROR_CODE) {
                        fillIn.putExtra("errorCode", errorCode);
                    }
                    if (mUnsentPartCount != null && isSinglePartOrLastPart) {
                        // Is multipart and last part
                        fillIn.putExtra(SEND_NEXT_MSG_EXTRA, true);
                    }
                    if (mMessageId != 0L) {
                        // Send the id back to the caller so they can verify the message id
                        // with the one they passed to SmsManager.
                        fillIn.putExtra(MESSAGE_ID_EXTRA, mMessageId);
                    }
                    fillIn.putExtra("format", mFormat);
                    fillIn.putExtra("ims", mUsesImsServiceForIms);
                    mSentIntent.send(context, error, fillIn);
                } catch (CanceledException ex) {
                    Rlog.e(TAG, "Failed to send result "
                            + SmsController.formatCrossStackMessageId(mMessageId));
                }
            }
            reportAnomaly(error, errorCode);
        }

        private void reportAnomaly(int error, int errorCode) {
            switch (error) {
                // Exclude known failed reason
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:
                case SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED:
                case SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED:
                case SmsManager.RESULT_SMS_BLOCKED_DURING_EMERGENCY:
                    break;
                // Dump bugreport for analysis
                default:
                    String message = "SMS failed";
                    Rlog.d(TAG, message + " with error " + error + ", errorCode " + errorCode);
                    AnomalyReporter.reportAnomaly(
                            generateUUID(error, errorCode), message, mCarrierId);
            }
        }

        private UUID generateUUID(int error, int errorCode) {
            long lerror = error;
            long lerrorCode = errorCode;
            return new UUID(mAnomalyUnexpectedErrorFromRilUUID.getMostSignificantBits(),
                    mAnomalyUnexpectedErrorFromRilUUID.getLeastSignificantBits()
                            + ((lerrorCode << 32) + lerror));
        }

        /**
         * Handle the sent of a single part message or a part of a multipart message
         *
         * @param context The Context
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public void onSent(Context context) {
            // is single part or last part of multipart message
            boolean isSinglePartOrLastPart = true;
            if (mUnsentPartCount != null) {
                isSinglePartOrLastPart = mUnsentPartCount.decrementAndGet() == 0;
            }
            if (isSinglePartOrLastPart) {
                int messageType = Sms.MESSAGE_TYPE_SENT;
                if (mAnyPartFailed != null && mAnyPartFailed.get()) {
                    messageType = Sms.MESSAGE_TYPE_FAILED;
                }
                persistOrUpdateMessage(context, messageType, NO_ERROR_CODE);
            }
            if (mSentIntent != null) {
                try {
                    // Extra information to send with the sent intent
                    Intent fillIn = new Intent();
                    if (mMessageUri != null) {
                        // Pass this to SMS apps so that they know where it is stored
                        fillIn.putExtra("uri", mMessageUri.toString());
                    }
                    if (mUnsentPartCount != null && isSinglePartOrLastPart) {
                        // Is multipart and last part
                        fillIn.putExtra(SEND_NEXT_MSG_EXTRA, true);
                    }
                    fillIn.putExtra("format", mFormat);
                    fillIn.putExtra("ims", mUsesImsServiceForIms);
                    mSentIntent.send(context, Activity.RESULT_OK, fillIn);
                } catch (CanceledException ex) {
                    Rlog.e(TAG, "Failed to send result");
                }
            }
        }
    }

    protected SmsTracker getSmsTracker(String callingPackage, HashMap<String, Object> data,
            PendingIntent sentIntent, PendingIntent deliveryIntent, String format,
            AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri,
            SmsHeader smsHeader, boolean expectMore, String fullMessageText, boolean isText,
            boolean persistMessage, int priority, int validityPeriod, boolean isForVvm,
            long messageId) {
        // Get package info via packagemanager
        UserHandle callingUser = UserHandle.getUserHandleForUid(Binder.getCallingUid());
        final int userId = callingUser.getIdentifier();
        PackageManager pm = mContext.createContextAsUser(callingUser, 0).getPackageManager();
        PackageInfo appInfo = null;
        try {
            appInfo = pm.getPackageInfo(callingPackage, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            // error will be logged in sendRawPdu
        }
        // Strip non-digits from destination phone number before checking for short codes
        // and before displaying the number to the user if confirmation is required.
        String destAddr = PhoneNumberUtils.extractNetworkPortion((String) data.get("destAddr"));
        return new SmsTracker(data, sentIntent, deliveryIntent, appInfo, destAddr, format,
                unsentPartCount, anyPartFailed, messageUri, smsHeader, expectMore,
                fullMessageText, getSubId(), isText, persistMessage, userId, priority,
                validityPeriod, isForVvm, messageId, mPhone.getCarrierId());
    }

    protected SmsTracker getSmsTracker(String callingPackage, HashMap<String, Object> data,
            PendingIntent sentIntent, PendingIntent deliveryIntent, String format, Uri messageUri,
            boolean expectMore, String fullMessageText, boolean isText, boolean persistMessage,
            boolean isForVvm, long messageId) {
        return getSmsTracker(callingPackage, data, sentIntent, deliveryIntent, format,
                null/*unsentPartCount*/, null/*anyPartFailed*/, messageUri, null/*smsHeader*/,
                expectMore, fullMessageText, isText, persistMessage,
                SMS_MESSAGE_PRIORITY_NOT_SPECIFIED, SMS_MESSAGE_PERIOD_NOT_SPECIFIED, isForVvm,
                messageId);
    }

    protected SmsTracker getSmsTracker(String callingPackage, HashMap<String, Object> data,
            PendingIntent sentIntent, PendingIntent deliveryIntent, String format, Uri messageUri,
            boolean expectMore, String fullMessageText, boolean isText, boolean persistMessage,
            int priority, int validityPeriod, boolean isForVvm, long messageId) {
        return getSmsTracker(callingPackage, data, sentIntent, deliveryIntent, format,
                null/*unsentPartCount*/, null/*anyPartFailed*/, messageUri, null/*smsHeader*/,
                expectMore, fullMessageText, isText, persistMessage, priority, validityPeriod,
                isForVvm, messageId);
    }

    protected HashMap<String, Object> getSmsTrackerMap(String destAddr, String scAddr,
            String text, SmsMessageBase.SubmitPduBase pdu) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(MAP_KEY_DEST_ADDR, destAddr);
        map.put(MAP_KEY_SC_ADDR, scAddr);
        map.put(MAP_KEY_TEXT, text);
        map.put(MAP_KEY_SMSC, pdu.encodedScAddress);
        map.put(MAP_KEY_PDU, pdu.encodedMessage);
        return map;
    }

    protected HashMap<String, Object> getSmsTrackerMap(String destAddr, String scAddr,
            int destPort, byte[] data, SmsMessageBase.SubmitPduBase pdu) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(MAP_KEY_DEST_ADDR, destAddr);
        map.put(MAP_KEY_SC_ADDR, scAddr);
        map.put(MAP_KEY_DEST_PORT, destPort);
        map.put(MAP_KEY_DATA, data);
        map.put(MAP_KEY_SMSC, pdu.encodedScAddress);
        map.put(MAP_KEY_PDU, pdu.encodedMessage);
        return map;
    }

    /**
     * Dialog listener for SMS confirmation dialog.
     */
    private final class ConfirmDialogListener
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
            CompoundButton.OnCheckedChangeListener {

        private final SmsTracker[] mTrackers;
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private Button mPositiveButton;
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private Button mNegativeButton;
        private boolean mRememberChoice;    // default is unchecked
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private final TextView mRememberUndoInstruction;
        private int mConfirmationType;  // 0 - Short Code Msg Sending; 1 - Rate Limit Exceeded
        private static final int SHORT_CODE_MSG = 0; // Short Code Msg
        private static final int RATE_LIMIT = 1; // Rate Limit Exceeded
        private static final int NEVER_ALLOW = 1; // Never Allow

        ConfirmDialogListener(SmsTracker[] trackers, TextView textView, int confirmationType) {
            mTrackers = trackers;
            mRememberUndoInstruction = textView;
            mConfirmationType = confirmationType;
        }

        void setPositiveButton(Button button) {
            mPositiveButton = button;
        }

        void setNegativeButton(Button button) {
            mNegativeButton = button;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            // Always set the SMS permission so that Settings will show a permission setting
            // for the app (it won't be shown until after the app tries to send to a short code).
            int newSmsPermission = SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ASK_USER;

            if (which == DialogInterface.BUTTON_POSITIVE) {
                Rlog.d(TAG, "CONFIRM sending SMS");
                // XXX this is lossy- apps can have more than one signature
                EventLog.writeEvent(
                        EventLogTags.EXP_DET_SMS_SENT_BY_USER,
                        mTrackers[0].mAppInfo.applicationInfo == null
                                ? -1
                                : mTrackers[0].mAppInfo.applicationInfo.uid);
                sendMessage(obtainMessage(EVENT_SEND_CONFIRMED_SMS, mTrackers));
                if (mRememberChoice) {
                    newSmsPermission = SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW;
                }
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                Rlog.d(TAG, "DENY sending SMS");
                // XXX this is lossy- apps can have more than one signature
                EventLog.writeEvent(
                        EventLogTags.EXP_DET_SMS_DENIED_BY_USER,
                        mTrackers[0].mAppInfo.applicationInfo == null
                                ? -1
                                : mTrackers[0].mAppInfo.applicationInfo.uid);
                Message msg = obtainMessage(EVENT_STOP_SENDING, mTrackers);
                msg.arg1 = mConfirmationType;
                if (mRememberChoice) {
                    newSmsPermission = SmsUsageMonitor.PREMIUM_SMS_PERMISSION_NEVER_ALLOW;
                    msg.arg2 = ConfirmDialogListener.NEVER_ALLOW;
                }
                sendMessage(msg);
            }
            mSmsDispatchersController.setPremiumSmsPermission(
                    mTrackers[0].getAppPackageName(), newSmsPermission);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            Rlog.d(TAG, "dialog dismissed: don't send SMS");
            Message msg = obtainMessage(EVENT_STOP_SENDING, mTrackers);
            msg.arg1 = mConfirmationType;
            sendMessage(msg);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Rlog.d(TAG, "remember this choice: " + isChecked);
            mRememberChoice = isChecked;
            if (isChecked) {
                mPositiveButton.setText(R.string.sms_short_code_confirm_always_allow);
                mNegativeButton.setText(R.string.sms_short_code_confirm_never_allow);
                if (mRememberUndoInstruction != null) {
                    mRememberUndoInstruction
                            .setText(R.string.sms_short_code_remember_undo_instruction);
                    mRememberUndoInstruction.setPadding(0,0,0,32);
                }
            } else {
                mPositiveButton.setText(R.string.sms_short_code_confirm_allow);
                mNegativeButton.setText(R.string.sms_short_code_confirm_deny);
                if (mRememberUndoInstruction != null) {
                    mRememberUndoInstruction.setText("");
                    mRememberUndoInstruction.setPadding(0,0,0,0);
                }
            }
        }
    }

    public boolean isIms() {
        if (mSmsDispatchersController != null) {
            return mSmsDispatchersController.isIms();
        } else {
            Rlog.e(TAG, "mSmsDispatchersController is null");
            return false;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String getMultipartMessageText(ArrayList<String> parts) {
        final StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null) {
                sb.append(part);
            }
        }
        return sb.toString();
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected String getCarrierAppPackageName() {
        CarrierPrivilegesTracker cpt = mPhone.getCarrierPrivilegesTracker();
        if (cpt == null) {
            return null;
        }
        List<String> carrierPackages =
                cpt.getCarrierPackageNamesForIntent(
                        new Intent(CarrierMessagingService.SERVICE_INTERFACE));
        if (carrierPackages != null && carrierPackages.size() == 1) {
            return carrierPackages.get(0);
        }
        // If there is no carrier package which implements CarrierMessagingService, then lookup
        // an ImsService implementing RCS that also implements CarrierMessagingService.
        return CarrierSmsUtils.getImsRcsPackageForIntent(mContext, mPhone,
                new Intent(CarrierMessagingService.SERVICE_INTERFACE));
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected int getSubId() {
        return SubscriptionController.getInstance().getSubIdUsingPhoneId(mPhone.getPhoneId());
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void checkCallerIsPhoneOrCarrierApp() {
        int uid = Binder.getCallingUid();
        int appId = UserHandle.getAppId(uid);
        if (appId == Process.PHONE_UID || uid == 0) {
            return;
        }
        try {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(getCarrierAppPackageName(), 0);
            if (UserHandle.getAppId(ai.uid) != UserHandle.getAppId(Binder.getCallingUid())) {
                throw new SecurityException("Caller is not phone or carrier app!");
            }
        } catch (PackageManager.NameNotFoundException re) {
            throw new SecurityException("Caller is not phone or carrier app!");
        }
    }

    protected boolean isCdmaMo() {
        return mSmsDispatchersController.isCdmaMo();
    }

    private boolean isAscii7bitSupportedForLongMessage() {
        //TODO: Do not rely on calling identity here, we should store UID & clear identity earlier.
        long token = Binder.clearCallingIdentity();
        try {
            CarrierConfigManager configManager = (CarrierConfigManager) mContext.getSystemService(
                    Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle pb = null;
            pb = configManager.getConfigForSubId(mPhone.getSubId());
            if (pb != null) {
                return pb.getBoolean(CarrierConfigManager
                        .KEY_ASCII_7_BIT_SUPPORT_FOR_LONG_MESSAGE_BOOL);
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * Dump local logs
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(TAG);
        pw.increaseIndent();
        pw.println("mLocalLog:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
    }
}

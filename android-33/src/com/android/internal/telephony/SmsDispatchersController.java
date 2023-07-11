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

import static com.android.internal.telephony.SmsResponse.NO_ERROR_CODE;
import static com.android.internal.telephony.cdma.sms.BearerData.ERROR_NONE;
import static com.android.internal.telephony.cdma.sms.BearerData.ERROR_TEMPORARY;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Intents;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.android.internal.telephony.gsm.GsmSMSDispatcher;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 */
public class SmsDispatchersController extends Handler {
    private static final String TAG = "SmsDispatchersController";
    private static final boolean VDBG = false; // STOPSHIP if true

    /** Radio is ON */
    private static final int EVENT_RADIO_ON = 11;

    /** IMS registration/SMS format changed */
    private static final int EVENT_IMS_STATE_CHANGED = 12;

    /** Callback from RIL_REQUEST_IMS_REGISTRATION_STATE */
    private static final int EVENT_IMS_STATE_DONE = 13;

    /** Service state changed */
    private static final int EVENT_SERVICE_STATE_CHANGED = 14;

    /** Purge old message segments */
    private static final int EVENT_PARTIAL_SEGMENT_TIMER_EXPIRY = 15;

    /** User unlocked the device */
    private static final int EVENT_USER_UNLOCKED = 16;

    /** InboundSmsHandler exited WaitingState */
    protected static final int EVENT_SMS_HANDLER_EXITING_WAITING_STATE = 17;

    /** Delete any partial message segments after being IN_SERVICE for 1 day. */
    private static final long PARTIAL_SEGMENT_WAIT_DURATION = (long) (60 * 60 * 1000) * 24;
    /** Constant for invalid time */
    private static final long INVALID_TIME = -1;
    /** Time at which last IN_SERVICE event was received */
    private long mLastInServiceTime = INVALID_TIME;
    /** Current IN_SERVICE duration */
    private long mCurrentWaitElapsedDuration = 0;
    /** Time at which the current PARTIAL_SEGMENT_WAIT_DURATION timer was started */
    private long mCurrentWaitStartTime = INVALID_TIME;

    private SMSDispatcher mCdmaDispatcher;
    private SMSDispatcher mGsmDispatcher;
    private ImsSmsDispatcher mImsSmsDispatcher;

    private GsmInboundSmsHandler mGsmInboundSmsHandler;
    private CdmaInboundSmsHandler mCdmaInboundSmsHandler;

    private Phone mPhone;
    /** Outgoing message counter. Shared by all dispatchers. */
    private final SmsUsageMonitor mUsageMonitor;
    private final CommandsInterface mCi;
    private final Context mContext;

    /** true if IMS is registered and sms is supported, false otherwise.*/
    private boolean mIms = false;
    private String mImsSmsFormat = SmsConstants.FORMAT_UNKNOWN;

    /** 3GPP format sent messages awaiting a delivery status report. */
    private HashMap<Integer, SMSDispatcher.SmsTracker> mDeliveryPendingMapFor3GPP = new HashMap<>();

    /** 3GPP2 format sent messages awaiting a delivery status report. */
    private HashMap<Integer, SMSDispatcher.SmsTracker> mDeliveryPendingMapFor3GPP2 =
            new HashMap<>();

    /**
     * Puts a delivery pending tracker to the map based on the format.
     *
     * @param tracker the tracker awaiting a delivery status report.
     */
    public void putDeliveryPendingTracker(SMSDispatcher.SmsTracker tracker) {
        if (isCdmaFormat(tracker.mFormat)) {
            mDeliveryPendingMapFor3GPP2.put(tracker.mMessageRef, tracker);
        } else {
            mDeliveryPendingMapFor3GPP.put(tracker.mMessageRef, tracker);
        }
    }

    public SmsDispatchersController(Phone phone, SmsStorageMonitor storageMonitor,
            SmsUsageMonitor usageMonitor) {
        Rlog.d(TAG, "SmsDispatchersController created");

        mContext = phone.getContext();
        mUsageMonitor = usageMonitor;
        mCi = phone.mCi;
        mPhone = phone;

        // Create dispatchers, inbound SMS handlers and
        // broadcast undelivered messages in raw table.
        mImsSmsDispatcher = new ImsSmsDispatcher(phone, this, ImsManager::getConnector);
        mCdmaDispatcher = new CdmaSMSDispatcher(phone, this);
        mGsmInboundSmsHandler = GsmInboundSmsHandler.makeInboundSmsHandler(phone.getContext(),
                storageMonitor, phone);
        mCdmaInboundSmsHandler = CdmaInboundSmsHandler.makeInboundSmsHandler(phone.getContext(),
                storageMonitor, phone, (CdmaSMSDispatcher) mCdmaDispatcher);
        mGsmDispatcher = new GsmSMSDispatcher(phone, this, mGsmInboundSmsHandler);
        SmsBroadcastUndelivered.initialize(phone.getContext(),
                mGsmInboundSmsHandler, mCdmaInboundSmsHandler);
        InboundSmsHandler.registerNewMessageNotificationActionHandler(phone.getContext());

        mCi.registerForOn(this, EVENT_RADIO_ON, null);
        mCi.registerForImsNetworkStateChanged(this, EVENT_IMS_STATE_CHANGED, null);

        UserManager userManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        if (userManager.isUserUnlocked()) {
            if (VDBG) {
                logd("SmsDispatchersController: user unlocked; registering for service"
                        + "state changed");
            }
            mPhone.registerForServiceStateChanged(this, EVENT_SERVICE_STATE_CHANGED, null);
            resetPartialSegmentWaitTimer();
        } else {
            if (VDBG) {
                logd("SmsDispatchersController: user locked; waiting for USER_UNLOCKED");
            }
            IntentFilter userFilter = new IntentFilter();
            userFilter.addAction(Intent.ACTION_USER_UNLOCKED);
            mContext.registerReceiver(mBroadcastReceiver, userFilter);
        }
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Rlog.d(TAG, "Received broadcast " + intent.getAction());
            if (Intent.ACTION_USER_UNLOCKED.equals(intent.getAction())) {
                sendMessage(obtainMessage(EVENT_USER_UNLOCKED));
            }
        }
    };

    public void dispose() {
        mCi.unregisterForOn(this);
        mCi.unregisterForImsNetworkStateChanged(this);
        mPhone.unregisterForServiceStateChanged(this);
        mGsmDispatcher.dispose();
        mCdmaDispatcher.dispose();
        mGsmInboundSmsHandler.dispose();
        mCdmaInboundSmsHandler.dispose();
    }

    /**
     * Handles events coming from the phone stack. Overridden from handler.
     *
     * @param msg the message to handle
     */
    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;

        switch (msg.what) {
            case EVENT_RADIO_ON:
            case EVENT_IMS_STATE_CHANGED: // received unsol
                mCi.getImsRegistrationState(this.obtainMessage(EVENT_IMS_STATE_DONE));
                break;

            case EVENT_IMS_STATE_DONE:
                ar = (AsyncResult) msg.obj;

                if (ar.exception == null) {
                    updateImsInfo(ar);
                } else {
                    Rlog.e(TAG, "IMS State query failed with exp "
                            + ar.exception);
                }
                break;

            case EVENT_SERVICE_STATE_CHANGED:
            case EVENT_SMS_HANDLER_EXITING_WAITING_STATE:
                reevaluateTimerStatus();
                break;

            case EVENT_PARTIAL_SEGMENT_TIMER_EXPIRY:
                handlePartialSegmentTimerExpiry((Long) msg.obj);
                break;

            case EVENT_USER_UNLOCKED:
                if (VDBG) {
                    logd("handleMessage: EVENT_USER_UNLOCKED");
                }
                mPhone.registerForServiceStateChanged(this, EVENT_SERVICE_STATE_CHANGED, null);
                resetPartialSegmentWaitTimer();
                break;

            default:
                if (isCdmaMo()) {
                    mCdmaDispatcher.handleMessage(msg);
                } else {
                    mGsmDispatcher.handleMessage(msg);
                }
        }
    }

    private void reevaluateTimerStatus() {
        long currentTime = System.currentTimeMillis();

        // Remove unhandled timer expiry message. A new message will be posted if needed.
        removeMessages(EVENT_PARTIAL_SEGMENT_TIMER_EXPIRY);
        // Update timer duration elapsed time (add time since last IN_SERVICE to now).
        // This is needed for IN_SERVICE as well as OUT_OF_SERVICE because same events can be
        // received back to back
        if (mLastInServiceTime != INVALID_TIME) {
            mCurrentWaitElapsedDuration += (currentTime - mLastInServiceTime);
        }

        if (VDBG) {
            logd("reevaluateTimerStatus: currentTime: " + currentTime
                    + " mCurrentWaitElapsedDuration: " + mCurrentWaitElapsedDuration);
        }

        if (mCurrentWaitElapsedDuration > PARTIAL_SEGMENT_WAIT_DURATION) {
            // handle this event as timer expiry
            handlePartialSegmentTimerExpiry(mCurrentWaitStartTime);
        } else {
            if (isInService()) {
                handleInService(currentTime);
            } else {
                handleOutOfService(currentTime);
            }
        }
    }

    private void handleInService(long currentTime) {
        if (VDBG) {
            logd("handleInService: timer expiry in "
                    + (PARTIAL_SEGMENT_WAIT_DURATION - mCurrentWaitElapsedDuration) + "ms");
        }

        // initialize mCurrentWaitStartTime if needed
        if (mCurrentWaitStartTime == INVALID_TIME) mCurrentWaitStartTime = currentTime;

        // Post a message for timer expiry time. mCurrentWaitElapsedDuration is the duration already
        // elapsed from the timer.
        sendMessageDelayed(
                obtainMessage(EVENT_PARTIAL_SEGMENT_TIMER_EXPIRY, mCurrentWaitStartTime),
                PARTIAL_SEGMENT_WAIT_DURATION - mCurrentWaitElapsedDuration);

        // update mLastInServiceTime as the current time
        mLastInServiceTime = currentTime;
    }

    private void handleOutOfService(long currentTime) {
        if (VDBG) {
            logd("handleOutOfService: currentTime: " + currentTime
                    + " mCurrentWaitElapsedDuration: " + mCurrentWaitElapsedDuration);
        }

        // mLastInServiceTime is not relevant now since state is OUT_OF_SERVICE; set it to INVALID
        mLastInServiceTime = INVALID_TIME;
    }

    private void handlePartialSegmentTimerExpiry(long waitTimerStart) {
        if (mGsmInboundSmsHandler.getCurrentState().getName().equals("WaitingState")
                || mCdmaInboundSmsHandler.getCurrentState().getName().equals("WaitingState")) {
            logd("handlePartialSegmentTimerExpiry: ignoring timer expiry as InboundSmsHandler is"
                    + " in WaitingState");
            return;
        }

        if (VDBG) {
            logd("handlePartialSegmentTimerExpiry: calling scanRawTable()");
        }
        // Timer expired. This indicates that device has been in service for
        // PARTIAL_SEGMENT_WAIT_DURATION since waitTimerStart. Delete orphaned message segments
        // older than waitTimerStart.
        SmsBroadcastUndelivered.scanRawTable(mContext, waitTimerStart);
        if (VDBG) {
            logd("handlePartialSegmentTimerExpiry: scanRawTable() done");
        }

        resetPartialSegmentWaitTimer();
    }

    private void resetPartialSegmentWaitTimer() {
        long currentTime = System.currentTimeMillis();

        removeMessages(EVENT_PARTIAL_SEGMENT_TIMER_EXPIRY);
        if (isInService()) {
            if (VDBG) {
                logd("resetPartialSegmentWaitTimer: currentTime: " + currentTime
                        + " IN_SERVICE");
            }
            mCurrentWaitStartTime = currentTime;
            mLastInServiceTime = currentTime;
            sendMessageDelayed(
                    obtainMessage(EVENT_PARTIAL_SEGMENT_TIMER_EXPIRY, mCurrentWaitStartTime),
                    PARTIAL_SEGMENT_WAIT_DURATION);
        } else {
            if (VDBG) {
                logd("resetPartialSegmentWaitTimer: currentTime: " + currentTime
                        + " not IN_SERVICE");
            }
            mCurrentWaitStartTime = INVALID_TIME;
            mLastInServiceTime = INVALID_TIME;
        }

        mCurrentWaitElapsedDuration = 0;
    }

    private boolean isInService() {
        ServiceState serviceState = mPhone.getServiceState();
        return serviceState != null && serviceState.getState() == ServiceState.STATE_IN_SERVICE;
    }

    private void setImsSmsFormat(int format) {
        switch (format) {
            case PhoneConstants.PHONE_TYPE_GSM:
                mImsSmsFormat = SmsConstants.FORMAT_3GPP;
                break;
            case PhoneConstants.PHONE_TYPE_CDMA:
                mImsSmsFormat = SmsConstants.FORMAT_3GPP2;
                break;
            default:
                mImsSmsFormat = SmsConstants.FORMAT_UNKNOWN;
                break;
        }
    }

    private void updateImsInfo(AsyncResult ar) {
        int[] responseArray = (int[]) ar.result;
        setImsSmsFormat(responseArray[1]);
        mIms = responseArray[0] == 1 && !SmsConstants.FORMAT_UNKNOWN.equals(mImsSmsFormat);
        Rlog.d(TAG, "IMS registration state: " + mIms + " format: " + mImsSmsFormat);
    }

    /**
     * Inject an SMS PDU into the android platform only if it is class 1.
     *
     * @param pdu is the byte array of pdu to be injected into android telephony layer
     * @param format is the format of SMS pdu (3gpp or 3gpp2)
     * @param callback if not NULL this callback is triggered when the message is successfully
     *                 received by the android telephony layer. This callback is triggered at
     *                 the same time an SMS received from radio is responded back.
     */
    @VisibleForTesting
    public void injectSmsPdu(byte[] pdu, String format, boolean isOverIms,
            SmsInjectionCallback callback) {
        // TODO We need to decide whether we should allow injecting GSM(3gpp)
        // SMS pdus when the phone is camping on CDMA(3gpp2) network and vice versa.
        android.telephony.SmsMessage msg =
                android.telephony.SmsMessage.createFromPdu(pdu, format);
        injectSmsPdu(msg, format, callback, false /* ignoreClass */, isOverIms);
    }

    /**
     * Inject an SMS PDU into the android platform.
     *
     * @param msg is the {@link SmsMessage} to be injected into android telephony layer
     * @param format is the format of SMS pdu (3gpp or 3gpp2)
     * @param callback if not NULL this callback is triggered when the message is successfully
     *                 received by the android telephony layer. This callback is triggered at
     *                 the same time an SMS received from radio is responded back.
     * @param ignoreClass if set to false, this method will inject class 1 sms only.
     */
    @VisibleForTesting
    public void injectSmsPdu(SmsMessage msg, String format, SmsInjectionCallback callback,
            boolean ignoreClass, boolean isOverIms) {
        Rlog.d(TAG, "SmsDispatchersController:injectSmsPdu");
        try {
            if (msg == null) {
                Rlog.e(TAG, "injectSmsPdu: createFromPdu returned null");
                callback.onSmsInjectedResult(Intents.RESULT_SMS_GENERIC_ERROR);
                return;
            }

            if (!ignoreClass
                    && msg.getMessageClass() != android.telephony.SmsMessage.MessageClass.CLASS_1) {
                Rlog.e(TAG, "injectSmsPdu: not class 1");
                callback.onSmsInjectedResult(Intents.RESULT_SMS_GENERIC_ERROR);
                return;
            }

            AsyncResult ar = new AsyncResult(callback, msg, null);

            if (format.equals(SmsConstants.FORMAT_3GPP)) {
                Rlog.i(TAG, "SmsDispatchersController:injectSmsText Sending msg=" + msg
                        + ", format=" + format + "to mGsmInboundSmsHandler");
                mGsmInboundSmsHandler.sendMessage(
                        InboundSmsHandler.EVENT_INJECT_SMS, isOverIms ? 1 : 0, 0, ar);
            } else if (format.equals(SmsConstants.FORMAT_3GPP2)) {
                Rlog.i(TAG, "SmsDispatchersController:injectSmsText Sending msg=" + msg
                        + ", format=" + format + "to mCdmaInboundSmsHandler");
                mCdmaInboundSmsHandler.sendMessage(
                        InboundSmsHandler.EVENT_INJECT_SMS, isOverIms ? 1 : 0, 0, ar);
            } else {
                // Invalid pdu format.
                Rlog.e(TAG, "Invalid pdu format: " + format);
                callback.onSmsInjectedResult(Intents.RESULT_SMS_GENERIC_ERROR);
            }
        } catch (Exception e) {
            Rlog.e(TAG, "injectSmsPdu failed: ", e);
            callback.onSmsInjectedResult(Intents.RESULT_SMS_GENERIC_ERROR);
        }
    }

    /**
     * Retry the message along to the radio.
     *
     * @param tracker holds the SMS message to send
     */
    public void sendRetrySms(SMSDispatcher.SmsTracker tracker) {
        String oldFormat = tracker.mFormat;
        boolean retryUsingImsService = false;

        if (!tracker.mUsesImsServiceForIms && mImsSmsDispatcher.isAvailable()) {
            // If this tracker has not been handled by ImsSmsDispatcher yet and IMS Service is
            // available now, retry this failed tracker using IMS Service.
            retryUsingImsService = true;
        }

        // If retryUsingImsService is true, newFormat will be IMS SMS format. Otherwise, newFormat
        // will be based on voice technology.
        String newFormat =
                retryUsingImsService
                        ? mImsSmsDispatcher.getFormat()
                        : (PhoneConstants.PHONE_TYPE_CDMA == mPhone.getPhoneType())
                                ? mCdmaDispatcher.getFormat()
                                : mGsmDispatcher.getFormat();

        Rlog.d(TAG, "old format(" + oldFormat + ") ==> new format (" + newFormat + ")");
        if (!oldFormat.equals(newFormat)) {
            // format didn't match, need to re-encode.
            HashMap map = tracker.getData();

            // to re-encode, fields needed are: scAddr, destAddr and text if originally sent as
            // sendText or data and destPort if originally sent as sendData.
            if (!(map.containsKey("scAddr") && map.containsKey("destAddr")
                    && (map.containsKey("text")
                    || (map.containsKey("data") && map.containsKey("destPort"))))) {
                // should never come here...
                Rlog.e(TAG, "sendRetrySms failed to re-encode per missing fields!");
                tracker.onFailed(mContext, SmsManager.RESULT_SMS_SEND_RETRY_FAILED, NO_ERROR_CODE);
                return;
            }
            String scAddr = (String) map.get("scAddr");
            String destAddr = (String) map.get("destAddr");
            if (destAddr == null) {
                Rlog.e(TAG, "sendRetrySms failed due to null destAddr");
                tracker.onFailed(mContext, SmsManager.RESULT_SMS_SEND_RETRY_FAILED, NO_ERROR_CODE);
                return;
            }

            SmsMessageBase.SubmitPduBase pdu = null;
            // figure out from tracker if this was sendText/Data
            if (map.containsKey("text")) {
                String text = (String) map.get("text");
                Rlog.d(TAG, "sms failed was text with length: "
                        + (text == null ? null : text.length()));

                if (isCdmaFormat(newFormat)) {
                    pdu = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(
                            scAddr, destAddr, text, (tracker.mDeliveryIntent != null), null);
                } else {
                    pdu = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(
                            scAddr, destAddr, text, (tracker.mDeliveryIntent != null), null);
                }
            } else if (map.containsKey("data")) {
                byte[] data = (byte[]) map.get("data");
                Integer destPort = (Integer) map.get("destPort");
                Rlog.d(TAG, "sms failed was data with length: "
                        + (data == null ? null : data.length));

                if (isCdmaFormat(newFormat)) {
                    pdu = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(
                            scAddr, destAddr, destPort.intValue(), data,
                            (tracker.mDeliveryIntent != null));
                } else {
                    pdu = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(
                            scAddr, destAddr, destPort.intValue(), data,
                            (tracker.mDeliveryIntent != null));
                }
            }

            if (pdu == null) {
                Rlog.e(TAG, String.format("sendRetrySms failed to encode message."
                        + "scAddr: %s, "
                        + "destPort: %s", scAddr, map.get("destPort")));
                tracker.onFailed(mContext, SmsManager.RESULT_SMS_SEND_RETRY_FAILED, NO_ERROR_CODE);
                return;
            }
            // replace old smsc and pdu with newly encoded ones
            map.put("smsc", pdu.encodedScAddress);
            map.put("pdu", pdu.encodedMessage);
            tracker.mFormat = newFormat;
        }

        SMSDispatcher dispatcher =
                retryUsingImsService
                        ? mImsSmsDispatcher
                        : (isCdmaFormat(newFormat)) ? mCdmaDispatcher : mGsmDispatcher;

        dispatcher.sendSms(tracker);
    }

    /**
     * SMS over IMS is supported if IMS is registered and SMS is supported on IMS.
     *
     * @return true if SMS over IMS is supported via an IMS Service or mIms is true for the older
     *         implementation. Otherwise, false.
     */
    public boolean isIms() {
        return mImsSmsDispatcher.isAvailable() ? true : mIms;
    }

    /**
     * Gets SMS format supported on IMS.
     *
     * @return the SMS format from an IMS Service if available. Otherwise, mImsSmsFormat for the
     *         older implementation.
     */
    public String getImsSmsFormat() {
        return mImsSmsDispatcher.isAvailable() ? mImsSmsDispatcher.getFormat() : mImsSmsFormat;
    }

    /**
     * Determines whether or not to use CDMA format for MO SMS.
     * If SMS over IMS is supported, then format is based on IMS SMS format,
     * otherwise format is based on current phone type.
     *
     * @return true if Cdma format should be used for MO SMS, false otherwise.
     */
    protected boolean isCdmaMo() {
        if (!isIms()) {
            // IMS is not registered, use Voice technology to determine SMS format.
            return (PhoneConstants.PHONE_TYPE_CDMA == mPhone.getPhoneType());
        }
        // IMS is registered with SMS support
        return isCdmaFormat(getImsSmsFormat());
    }

    /**
     * Determines whether or not format given is CDMA format.
     *
     * @param format
     * @return true if format given is CDMA format, false otherwise.
     */
    public boolean isCdmaFormat(String format) {
        return (mCdmaDispatcher.getFormat().equals(format));
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
    protected void sendData(String callingPackage, String destAddr, String scAddr, int destPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean isForVvm) {
        if (mImsSmsDispatcher.isAvailable()) {
            mImsSmsDispatcher.sendData(callingPackage, destAddr, scAddr, destPort, data, sentIntent,
                    deliveryIntent, isForVvm);
        } else if (isCdmaMo()) {
            mCdmaDispatcher.sendData(callingPackage, destAddr, scAddr, destPort, data, sentIntent,
                    deliveryIntent, isForVvm);
        } else {
            mGsmDispatcher.sendData(callingPackage, destAddr, scAddr, destPort, data, sentIntent,
                    deliveryIntent, isForVvm);
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
    public void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent,
            PendingIntent deliveryIntent, Uri messageUri, String callingPkg, boolean persistMessage,
            int priority, boolean expectMore, int validityPeriod, boolean isForVvm,
            long messageId) {
        if (mImsSmsDispatcher.isAvailable() || mImsSmsDispatcher.isEmergencySmsSupport(destAddr)) {
            mImsSmsDispatcher.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent,
                    messageUri, callingPkg, persistMessage, priority, false /*expectMore*/,
                    validityPeriod, isForVvm, messageId);
        } else {
            if (isCdmaMo()) {
                mCdmaDispatcher.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent,
                        messageUri, callingPkg, persistMessage, priority, expectMore,
                        validityPeriod, isForVvm, messageId);
            } else {
                mGsmDispatcher.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent,
                        messageUri, callingPkg, persistMessage, priority, expectMore,
                        validityPeriod, isForVvm, messageId);
            }
        }
    }

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
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *  <code>PendingIntent</code>s (one for each message part) that is
     *  broadcast when the corresponding message part has been delivered
     *  to the recipient.  The raw pdu of the status report is in the
     * @param messageUri optional URI of the message if it is already stored in the system
     * @param callingPkg the calling package name
     * @param persistMessage whether to save the sent message into SMS DB for a
     *  non-default SMS app.
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
     *                 Used for logging and diagnostics purposes. The id may be 0.
     *
     */
    protected void sendMultipartText(String destAddr, String scAddr,
            ArrayList<String> parts, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents, Uri messageUri, String callingPkg,
            boolean persistMessage, int priority, boolean expectMore, int validityPeriod,
            long messageId) {
        if (mImsSmsDispatcher.isAvailable()) {
            mImsSmsDispatcher.sendMultipartText(destAddr, scAddr, parts, sentIntents,
                    deliveryIntents, messageUri, callingPkg, persistMessage, priority,
                    false /*expectMore*/, validityPeriod, messageId);
        } else {
            if (isCdmaMo()) {
                mCdmaDispatcher.sendMultipartText(destAddr, scAddr, parts, sentIntents,
                        deliveryIntents, messageUri, callingPkg, persistMessage, priority,
                        expectMore, validityPeriod, messageId);
            } else {
                mGsmDispatcher.sendMultipartText(destAddr, scAddr, parts, sentIntents,
                        deliveryIntents, messageUri, callingPkg, persistMessage, priority,
                        expectMore, validityPeriod, messageId);
            }
        }
    }

    /**
     * Returns the premium SMS permission for the specified package. If the package has never
     * been seen before, the default {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_UNKNOWN}
     * will be returned.
     * @param packageName the name of the package to query permission
     * @return one of {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_UNKNOWN},
     *  {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_ASK_USER},
     *  {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_NEVER_ALLOW}, or
     *  {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW}
     */
    public int getPremiumSmsPermission(String packageName) {
        return mUsageMonitor.getPremiumSmsPermission(packageName);
    }

    /**
     * Sets the premium SMS permission for the specified package and save the value asynchronously
     * to persistent storage.
     * @param packageName the name of the package to set permission
     * @param permission one of {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_ASK_USER},
     *  {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_NEVER_ALLOW}, or
     *  {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW}
     */
    public void setPremiumSmsPermission(String packageName, int permission) {
        mUsageMonitor.setPremiumSmsPermission(packageName, permission);
    }

    public SmsUsageMonitor getUsageMonitor() {
        return mUsageMonitor;
    }

    /**
     * Handles the sms status report based on the format.
     *
     * @param format the format.
     * @param pdu the pdu of the report.
     */
    public void handleSmsStatusReport(String format, byte[] pdu) {
        int messageRef;
        SMSDispatcher.SmsTracker tracker;
        boolean handled = false;
        if (isCdmaFormat(format)) {
            com.android.internal.telephony.cdma.SmsMessage sms =
                    com.android.internal.telephony.cdma.SmsMessage.createFromPdu(pdu);
            if (sms != null) {
                boolean foundIn3GPPMap = false;
                messageRef = sms.mMessageRef;
                tracker = mDeliveryPendingMapFor3GPP2.get(messageRef);
                if (tracker == null) {
                    // A tracker for this 3GPP2 report may be in the 3GPP map instead if the
                    // previously submitted SMS was 3GPP format.
                    // (i.e. Some carriers require that devices receive 3GPP2 SMS also even if IMS
                    // SMS format is 3GGP.)
                    tracker = mDeliveryPendingMapFor3GPP.get(messageRef);
                    if (tracker != null) {
                        foundIn3GPPMap = true;
                    }
                }
                if (tracker != null) {
                    // The status is composed of an error class (bits 25-24) and a status code
                    // (bits 23-16).
                    int errorClass = (sms.getStatus() >> 24) & 0x03;
                    if (errorClass != ERROR_TEMPORARY) {
                        // Update the message status (COMPLETE or FAILED)
                        tracker.updateSentMessageStatus(
                                mContext,
                                (errorClass == ERROR_NONE)
                                        ? Sms.STATUS_COMPLETE
                                        : Sms.STATUS_FAILED);
                        // No longer need to be kept.
                        if (foundIn3GPPMap) {
                            mDeliveryPendingMapFor3GPP.remove(messageRef);
                        } else {
                            mDeliveryPendingMapFor3GPP2.remove(messageRef);
                        }
                    }
                    handled = triggerDeliveryIntent(tracker, format, pdu);
                }
            }
        } else {
            com.android.internal.telephony.gsm.SmsMessage sms =
                    com.android.internal.telephony.gsm.SmsMessage.createFromPdu(pdu);
            if (sms != null) {
                messageRef = sms.mMessageRef;
                tracker = mDeliveryPendingMapFor3GPP.get(messageRef);
                if (tracker != null) {
                    int tpStatus = sms.getStatus();
                    if (tpStatus >= Sms.STATUS_FAILED || tpStatus < Sms.STATUS_PENDING) {
                        // Update the message status (COMPLETE or FAILED)
                        tracker.updateSentMessageStatus(mContext, tpStatus);
                        // No longer need to be kept.
                        mDeliveryPendingMapFor3GPP.remove(messageRef);
                    }
                    handled = triggerDeliveryIntent(tracker, format, pdu);
                }
            }
        }

        if (!handled) {
            Rlog.e(TAG, "handleSmsStatusReport: can not handle the status report!");
        }
    }

    private boolean triggerDeliveryIntent(SMSDispatcher.SmsTracker tracker, String format,
                                          byte[] pdu) {
        PendingIntent intent = tracker.mDeliveryIntent;
        Intent fillIn = new Intent();
        fillIn.putExtra("pdu", pdu);
        fillIn.putExtra("format", format);
        try {
            intent.send(mContext, Activity.RESULT_OK, fillIn);
            return true;
        } catch (CanceledException ex) {
            return false;
        }
    }

    /**
     * Get InboundSmsHandler for the phone.
     */
    public InboundSmsHandler getInboundSmsHandler(boolean is3gpp2) {
        if (is3gpp2) return mCdmaInboundSmsHandler;
        else return mGsmInboundSmsHandler;
    }

    public interface SmsInjectionCallback {
        void onSmsInjectedResult(int result);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        mGsmInboundSmsHandler.dump(fd, pw, args);
        mCdmaInboundSmsHandler.dump(fd, pw, args);
        mGsmDispatcher.dump(fd, pw, args);
        mCdmaDispatcher.dump(fd, pw, args);
        mImsSmsDispatcher.dump(fd, pw, args);
    }

    private void logd(String msg) {
        Rlog.d(TAG, msg);
    }
}

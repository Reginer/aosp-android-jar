/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static android.os.PowerWhitelistManager.REASON_EVENT_MMS;
import static android.os.PowerWhitelistManager.REASON_EVENT_SMS;
import static android.os.PowerWhitelistManager.TEMPORARY_ALLOWLIST_TYPE_FOREGROUND_SERVICE_ALLOWED;
import static android.provider.Telephony.Sms.Intents.RESULT_SMS_DATABASE_ERROR;
import static android.provider.Telephony.Sms.Intents.RESULT_SMS_DISPATCH_FAILURE;
import static android.provider.Telephony.Sms.Intents.RESULT_SMS_INVALID_URI;
import static android.provider.Telephony.Sms.Intents.RESULT_SMS_NULL_MESSAGE;
import static android.provider.Telephony.Sms.Intents.RESULT_SMS_NULL_PDU;
import static android.service.carrier.CarrierMessagingService.RECEIVE_OPTIONS_SKIP_NOTIFY_WHEN_CREDENTIAL_PROTECTED_STORAGE_UNAVAILABLE;
import static android.telephony.TelephonyManager.PHONE_TYPE_CDMA;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerWhitelistManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.Telephony;
import android.provider.Telephony.Sms.Intents;
import android.service.carrier.CarrierMessagingService;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import android.util.Pair;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.SmsConstants.MessageClass;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.HexDump;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.telephony.Rlog;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * This class broadcasts incoming SMS messages to interested apps after storing them in the
 * SmsProvider "raw" table and ACKing them to the SMSC. After each message has been broadcast, its
 * parts are removed from the raw table. If the device crashes after ACKing but before the broadcast
 * completes, the pending messages will be rebroadcast on the next boot.
 *
 * <p>The state machine starts in {@link IdleState} state. When we receive a new SMS from the radio,
 * the wakelock is acquired, then transition to {@link DeliveringState} state, where the message is
 * saved to the raw table, then acknowledged to the modem which in turn acknowledges it to the SMSC.
 *
 * <p>After saving the SMS, if the message is complete (either single-part or the final segment of a
 * multi-part SMS), we broadcast the completed PDUs as an ordered broadcast, then transition to
 * {@link WaitingState} state to wait for the broadcast to complete. When the local
 * {@link BroadcastReceiver} is called with the result, it sends {@link #EVENT_BROADCAST_COMPLETE}
 * to the state machine, causing us to either broadcast the next pending message (if one has arrived
 * while waiting for the broadcast to complete), or to transition back to the halted state after all
 * messages are processed. Then the wakelock is released and we wait for the next SMS.
 */
public abstract class InboundSmsHandler extends StateMachine {
    protected static final boolean DBG = true;
    protected static final boolean VDBG = false; // STOPSHIP if true, logs user data

    /** Query projection for checking for duplicate message segments. */
    private static final String[] PDU_DELETED_FLAG_PROJECTION = {
            "pdu",
            "deleted"
    };

    /** Mapping from DB COLUMN to PDU_SEQUENCE_PORT PROJECTION index */
    private static final Map<Integer, Integer> PDU_DELETED_FLAG_PROJECTION_INDEX_MAPPING =
            new HashMap<Integer, Integer>() {{
            put(PDU_COLUMN, 0);
            put(DELETED_FLAG_COLUMN, 1);
            }};

    /** Query projection for combining concatenated message segments. */
    private static final String[] PDU_SEQUENCE_PORT_PROJECTION = {
            "pdu",
            "sequence",
            "destination_port",
            "display_originating_addr",
            "date"
    };

    /** Mapping from DB COLUMN to PDU_SEQUENCE_PORT PROJECTION index */
    private static final Map<Integer, Integer> PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING =
            new HashMap<Integer, Integer>() {{
                put(PDU_COLUMN, 0);
                put(SEQUENCE_COLUMN, 1);
                put(DESTINATION_PORT_COLUMN, 2);
                put(DISPLAY_ADDRESS_COLUMN, 3);
                put(DATE_COLUMN, 4);
    }};

    public static final int PDU_COLUMN = 0;
    public static final int SEQUENCE_COLUMN = 1;
    public static final int DESTINATION_PORT_COLUMN = 2;
    public static final int DATE_COLUMN = 3;
    public static final int REFERENCE_NUMBER_COLUMN = 4;
    public static final int COUNT_COLUMN = 5;
    public static final int ADDRESS_COLUMN = 6;
    public static final int ID_COLUMN = 7;
    public static final int MESSAGE_BODY_COLUMN = 8;
    public static final int DISPLAY_ADDRESS_COLUMN = 9;
    public static final int DELETED_FLAG_COLUMN = 10;
    public static final int SUBID_COLUMN = 11;

    public static final String SELECT_BY_ID = "_id=?";

    /** New SMS received as an AsyncResult. */
    public static final int EVENT_NEW_SMS = 1;

    /** Message type containing a {@link InboundSmsTracker} ready to broadcast to listeners. */
    public static final int EVENT_BROADCAST_SMS = 2;

    /** Message from resultReceiver notifying {@link WaitingState} of a completed broadcast. */
    public static final int EVENT_BROADCAST_COMPLETE = 3;

    /** Sent on exit from {@link WaitingState} to return to idle after sending all broadcasts. */
    private static final int EVENT_RETURN_TO_IDLE = 4;

    /** Release wakelock after {@link #mWakeLockTimeout} when returning to idle state. */
    private static final int EVENT_RELEASE_WAKELOCK = 5;

    /** Sent by {@link SmsBroadcastUndelivered} after cleaning the raw table. */
    public static final int EVENT_START_ACCEPTING_SMS = 6;

    /** New SMS received as an AsyncResult. */
    public static final int EVENT_INJECT_SMS = 7;

    /** Update the sms tracker */
    public static final int EVENT_UPDATE_TRACKER = 8;

    /** BroadcastReceiver timed out waiting for an intent */
    public static final int EVENT_RECEIVER_TIMEOUT = 9;

    /** Wakelock release delay when returning to idle state. */
    private static final int WAKELOCK_TIMEOUT = 3000;

    /** Received SMS was not injected. */
    public static final int SOURCE_NOT_INJECTED = 0;

    /** Received SMS was received over IMS and injected. */
    public static final int SOURCE_INJECTED_FROM_IMS = 1;

    /** Received SMS was injected from source different than IMS. */
    public static final int SOURCE_INJECTED_FROM_UNKNOWN = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"SOURCE_"},
            value = {
                SOURCE_NOT_INJECTED,
                SOURCE_INJECTED_FROM_IMS,
                SOURCE_INJECTED_FROM_UNKNOWN
    })
    public @interface SmsSource {}

    // The notitfication tag used when showing a notification. The combination of notification tag
    // and notification id should be unique within the phone app.
    @VisibleForTesting
    public static final String NOTIFICATION_TAG = "InboundSmsHandler";
    @VisibleForTesting
    public static final int NOTIFICATION_ID_NEW_MESSAGE = 1;

    /** URI for raw table of SMS provider. */
    protected static final Uri sRawUri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "raw");
    protected static final Uri sRawUriPermanentDelete =
            Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "raw/permanentDelete");

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected final Context mContext;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final ContentResolver mResolver;

    /** Special handler for WAP push messages. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final WapPushOverSms mWapPush;

    /** Wake lock to ensure device stays awake while dispatching the SMS intents. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final PowerManager.WakeLock mWakeLock;

    /** DefaultState throws an exception or logs an error for unhandled message types. */
    private final DefaultState mDefaultState = new DefaultState();

    /** Startup state. Waiting for {@link SmsBroadcastUndelivered} to complete. */
    private final StartupState mStartupState = new StartupState();

    /** Idle state. Waiting for messages to process. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final IdleState mIdleState = new IdleState();

    /** Delivering state. Saves the PDU in the raw table and acknowledges to SMSC. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final DeliveringState mDeliveringState = new DeliveringState();

    /** Broadcasting state. Waits for current broadcast to complete before delivering next. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final WaitingState mWaitingState = new WaitingState();

    /** Helper class to check whether storage is available for incoming messages. */
    protected SmsStorageMonitor mStorageMonitor;

    private final boolean mSmsReceiveDisabled;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected Phone mPhone;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private UserManager mUserManager;

    protected TelephonyMetrics mMetrics = TelephonyMetrics.getInstance();

    private LocalLog mLocalLog = new LocalLog(64);
    private LocalLog mCarrierServiceLocalLog = new LocalLog(8);

    PowerWhitelistManager mPowerWhitelistManager;

    protected CellBroadcastServiceManager mCellBroadcastServiceManager;

    // Delete permanently from raw table
    private final int DELETE_PERMANENTLY = 1;
    // Only mark deleted, but keep in db for message de-duping
    private final int MARK_DELETED = 2;

    private static String ACTION_OPEN_SMS_APP =
        "com.android.internal.telephony.OPEN_DEFAULT_SMS_APP";

    /** Timeout for releasing wakelock */
    private int mWakeLockTimeout;

    private List<SmsFilter> mSmsFilters;

    /**
     * Create a new SMS broadcast helper.
     * @param name the class name for logging
     * @param context the context of the phone app
     * @param storageMonitor the SmsStorageMonitor to check for storage availability
     */
    protected InboundSmsHandler(String name, Context context, SmsStorageMonitor storageMonitor,
            Phone phone) {
        super(name);

        mContext = context;
        mStorageMonitor = storageMonitor;
        mPhone = phone;
        mResolver = context.getContentResolver();
        mWapPush = new WapPushOverSms(context);

        boolean smsCapable = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_sms_capable);
        mSmsReceiveDisabled = !TelephonyManager.from(mContext).getSmsReceiveCapableForPhone(
                mPhone.getPhoneId(), smsCapable);

        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, name);
        mWakeLock.acquire();    // wake lock released after we enter idle state
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mPowerWhitelistManager =
                (PowerWhitelistManager) mContext.getSystemService(Context.POWER_WHITELIST_MANAGER);
        mCellBroadcastServiceManager = new CellBroadcastServiceManager(context, phone);

        mSmsFilters = createDefaultSmsFilters();

        addState(mDefaultState);
        addState(mStartupState, mDefaultState);
        addState(mIdleState, mDefaultState);
        addState(mDeliveringState, mDefaultState);
            addState(mWaitingState, mDeliveringState);

        setInitialState(mStartupState);
        if (DBG) log("created InboundSmsHandler");
    }

    /**
     * Tell the state machine to quit after processing all messages.
     */
    public void dispose() {
        quit();
    }

    /**
     * Dispose of the WAP push object and release the wakelock.
     */
    @Override
    protected void onQuitting() {
        mWapPush.dispose();
        mCellBroadcastServiceManager.disable();

        while (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    // CAF_MSIM Is this used anywhere ? if not remove it
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public Phone getPhone() {
        return mPhone;
    }

    @Override
    protected String getWhatToString(int what) {
        String whatString;
        switch (what) {
            case EVENT_NEW_SMS:
                whatString = "EVENT_NEW_SMS";
                break;
            case EVENT_BROADCAST_SMS:
                whatString = "EVENT_BROADCAST_SMS";
                break;
            case EVENT_BROADCAST_COMPLETE:
                whatString = "EVENT_BROADCAST_COMPLETE";
                break;
            case EVENT_RETURN_TO_IDLE:
                whatString = "EVENT_RETURN_TO_IDLE";
                break;
            case EVENT_RELEASE_WAKELOCK:
                whatString = "EVENT_RELEASE_WAKELOCK";
                break;
            case EVENT_START_ACCEPTING_SMS:
                whatString = "EVENT_START_ACCEPTING_SMS";
                break;
            case EVENT_INJECT_SMS:
                whatString = "EVENT_INJECT_SMS";
                break;
            case EVENT_UPDATE_TRACKER:
                whatString = "EVENT_UPDATE_TRACKER";
                break;
            case EVENT_RECEIVER_TIMEOUT:
                whatString = "EVENT_RECEIVER_TIMEOUT";
                break;
            default:
                whatString = "UNKNOWN EVENT " + what;
        }
        return whatString;
    }

    /**
     * This parent state throws an exception (for debug builds) or prints an error for unhandled
     * message types.
     */
    private class DefaultState extends State {
        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                default: {
                    String errorText = "processMessage: unhandled message type "
                            + getWhatToString(msg.what) + " currState="
                            + getCurrentState().getName();
                    if (TelephonyUtils.IS_DEBUGGABLE) {
                        loge("---- Dumping InboundSmsHandler ----");
                        loge("Total records=" + getLogRecCount());
                        for (int i = Math.max(getLogRecSize() - 20, 0); i < getLogRecSize(); i++) {
                            // getLogRec(i).toString() will call the overridden getWhatToString
                            // method which has more information
                            loge("Rec[%d]: %s\n" + i + getLogRec(i).toString());
                        }
                        loge("---- Dumped InboundSmsHandler ----");

                        throw new RuntimeException(errorText);
                    } else {
                        loge(errorText);
                    }
                    break;
                }
            }
            return HANDLED;
        }
    }

    /**
     * The Startup state waits for {@link SmsBroadcastUndelivered} to process the raw table and
     * notify the state machine to broadcast any complete PDUs that might not have been broadcast.
     */
    private class StartupState extends State {
        @Override
        public void enter() {
            if (DBG) log("StartupState.enter: entering StartupState");
            // Set wakelock timeout to 0 during startup, this will ensure that the wakelock is not
            // held if there are no pending messages to be handled.
            setWakeLockTimeout(0);
        }

        @Override
        public boolean processMessage(Message msg) {
            log("StartupState.processMessage: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_NEW_SMS:
                case EVENT_INJECT_SMS:
                case EVENT_BROADCAST_SMS:
                    deferMessage(msg);
                    return HANDLED;

                case EVENT_START_ACCEPTING_SMS:
                    transitionTo(mIdleState);
                    return HANDLED;

                case EVENT_BROADCAST_COMPLETE:
                case EVENT_RETURN_TO_IDLE:
                case EVENT_RELEASE_WAKELOCK:
                default:
                    // let DefaultState handle these unexpected message types
                    return NOT_HANDLED;
            }
        }
    }

    /**
     * In the idle state the wakelock is released until a new SM arrives, then we transition
     * to Delivering mode to handle it, acquiring the wakelock on exit.
     */
    private class IdleState extends State {
        @Override
        public void enter() {
            if (DBG) log("IdleState.enter: entering IdleState");
            sendMessageDelayed(EVENT_RELEASE_WAKELOCK, getWakeLockTimeout());
        }

        @Override
        public void exit() {
            mWakeLock.acquire();
            if (DBG) log("IdleState.exit: acquired wakelock, leaving IdleState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("IdleState.processMessage: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_NEW_SMS:
                case EVENT_INJECT_SMS:
                case EVENT_BROADCAST_SMS:
                    deferMessage(msg);
                    transitionTo(mDeliveringState);
                    return HANDLED;

                case EVENT_RELEASE_WAKELOCK:
                    mWakeLock.release();
                    if (DBG) {
                        if (mWakeLock.isHeld()) {
                            // this is okay as long as we call release() for every acquire()
                            log("IdleState.processMessage: EVENT_RELEASE_WAKELOCK: mWakeLock is "
                                    + "still held after release");
                        } else {
                            log("IdleState.processMessage: EVENT_RELEASE_WAKELOCK: mWakeLock "
                                    + "released");
                        }
                    }
                    return HANDLED;

                case EVENT_RETURN_TO_IDLE:
                    // already in idle state; ignore
                    return HANDLED;

                case EVENT_BROADCAST_COMPLETE:
                case EVENT_START_ACCEPTING_SMS:
                default:
                    // let DefaultState handle these unexpected message types
                    return NOT_HANDLED;
            }
        }
    }

    /**
     * In the delivering state, the inbound SMS is processed and stored in the raw table.
     * The message is acknowledged before we exit this state. If there is a message to broadcast,
     * transition to {@link WaitingState} state to send the ordered broadcast and wait for the
     * results. When all messages have been processed, the halting state will release the wakelock.
     */
    private class DeliveringState extends State {
        @Override
        public void enter() {
            if (DBG) log("DeliveringState.enter: entering DeliveringState");
        }

        @Override
        public void exit() {
            if (DBG) log("DeliveringState.exit: leaving DeliveringState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("DeliveringState.processMessage: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_NEW_SMS:
                    // handle new SMS from RIL
                    handleNewSms((AsyncResult) msg.obj);
                    sendMessage(EVENT_RETURN_TO_IDLE);
                    return HANDLED;

                case EVENT_INJECT_SMS:
                    // handle new injected SMS
                    handleInjectSms((AsyncResult) msg.obj, msg.arg1 == 1 /* isOverIms */);
                    sendMessage(EVENT_RETURN_TO_IDLE);
                    return HANDLED;

                case EVENT_BROADCAST_SMS:
                    // if any broadcasts were sent, transition to waiting state
                    InboundSmsTracker inboundSmsTracker = (InboundSmsTracker) msg.obj;
                    if (processMessagePart(inboundSmsTracker)) {
                        sendMessage(obtainMessage(EVENT_UPDATE_TRACKER, msg.obj));
                        transitionTo(mWaitingState);
                    } else {
                        // if event is sent from SmsBroadcastUndelivered.broadcastSms(), and
                        // processMessagePart() returns false, the state machine will be stuck in
                        // DeliveringState until next message is received. Send message to
                        // transition to idle to avoid that so that wakelock can be released
                        log("DeliveringState.processMessage: EVENT_BROADCAST_SMS: No broadcast "
                                + "sent. Return to IdleState");
                        sendMessage(EVENT_RETURN_TO_IDLE);
                    }
                    return HANDLED;

                case EVENT_RETURN_TO_IDLE:
                    // return to idle after processing all other messages
                    transitionTo(mIdleState);
                    return HANDLED;

                case EVENT_RELEASE_WAKELOCK:
                    mWakeLock.release();    // decrement wakelock from previous entry to Idle
                    if (!mWakeLock.isHeld()) {
                        // wakelock should still be held until 3 seconds after we enter Idle
                        loge("mWakeLock released while delivering/broadcasting!");
                    }
                    return HANDLED;

                case EVENT_UPDATE_TRACKER:
                    logd("process tracker message in DeliveringState " + msg.arg1);
                    return HANDLED;

                // we shouldn't get this message type in this state, log error and halt.
                case EVENT_BROADCAST_COMPLETE:
                case EVENT_START_ACCEPTING_SMS:
                default:
                    logeWithLocalLog("Unhandled msg in delivering state, msg.what = "
                            + getWhatToString(msg.what));
                    // let DefaultState handle these unexpected message types
                    return NOT_HANDLED;
            }
        }
    }

    /**
     * The waiting state delegates handling of new SMS to parent {@link DeliveringState}, but
     * defers handling of the {@link #EVENT_BROADCAST_SMS} phase until after the current
     * result receiver sends {@link #EVENT_BROADCAST_COMPLETE}. Before transitioning to
     * {@link DeliveringState}, {@link #EVENT_RETURN_TO_IDLE} is sent to transition to
     * {@link IdleState} after any deferred {@link #EVENT_BROADCAST_SMS} messages are handled.
     */
    private class WaitingState extends State {

        private InboundSmsTracker mLastDeliveredSmsTracker;

        @Override
        public void enter() {
            if (DBG) log("WaitingState.enter: entering WaitingState");
        }

        @Override
        public void exit() {
            if (DBG) log("WaitingState.exit: leaving WaitingState");
            // Before moving to idle state, set wakelock timeout to WAKE_LOCK_TIMEOUT milliseconds
            // to give any receivers time to take their own wake locks
            setWakeLockTimeout(WAKELOCK_TIMEOUT);
            mPhone.getIccSmsInterfaceManager().mDispatchersController.sendEmptyMessage(
                    SmsDispatchersController.EVENT_SMS_HANDLER_EXITING_WAITING_STATE);
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("WaitingState.processMessage: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_BROADCAST_SMS:
                    // defer until the current broadcast completes
                    if (mLastDeliveredSmsTracker != null) {
                        String str = "Defer sms broadcast due to undelivered sms, "
                                + " messageCount = " + mLastDeliveredSmsTracker.getMessageCount()
                                + " destPort = " + mLastDeliveredSmsTracker.getDestPort()
                                + " timestamp = " + mLastDeliveredSmsTracker.getTimestamp()
                                + " currentTimestamp = " + System.currentTimeMillis();
                        logWithLocalLog(str, mLastDeliveredSmsTracker.getMessageId());
                    }
                    deferMessage(msg);
                    return HANDLED;

                case EVENT_RECEIVER_TIMEOUT:
                    logeWithLocalLog("WaitingState.processMessage: received "
                            + "EVENT_RECEIVER_TIMEOUT");
                    if (mLastDeliveredSmsTracker != null) {
                        mLastDeliveredSmsTracker.getSmsBroadcastReceiver(InboundSmsHandler.this)
                                .fakeNextAction();
                    }
                    return HANDLED;

                case EVENT_BROADCAST_COMPLETE:
                    mLastDeliveredSmsTracker = null;
                    // return to idle after handling all deferred messages
                    sendMessage(EVENT_RETURN_TO_IDLE);
                    transitionTo(mDeliveringState);
                    return HANDLED;

                case EVENT_RETURN_TO_IDLE:
                    // not ready to return to idle; ignore
                    return HANDLED;

                case EVENT_UPDATE_TRACKER:
                    mLastDeliveredSmsTracker = (InboundSmsTracker) msg.obj;
                    return HANDLED;

                default:
                    // parent state handles the other message types
                    return NOT_HANDLED;
            }
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void handleNewSms(AsyncResult ar) {
        if (ar.exception != null) {
            loge("Exception processing incoming SMS: " + ar.exception);
            return;
        }

        int result;
        try {
            SmsMessage sms = (SmsMessage) ar.result;
            result = dispatchMessage(sms.mWrappedSmsMessage, SOURCE_NOT_INJECTED);
        } catch (RuntimeException ex) {
            loge("Exception dispatching message", ex);
            result = RESULT_SMS_DISPATCH_FAILURE;
        }

        // RESULT_OK means that the SMS will be acknowledged by special handling,
        // e.g. for SMS-PP data download. Any other result, we should ack here.
        if (result != Activity.RESULT_OK) {
            boolean handled = (result == Intents.RESULT_SMS_HANDLED);
            notifyAndAcknowledgeLastIncomingSms(handled, result, null);
        }
    }

    /**
     * This method is called when a new SMS PDU is injected into application framework.
     * @param ar is the AsyncResult that has the SMS PDU to be injected.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void handleInjectSms(AsyncResult ar, boolean isOverIms) {
        int result;
        SmsDispatchersController.SmsInjectionCallback callback = null;
        try {
            callback = (SmsDispatchersController.SmsInjectionCallback) ar.userObj;
            SmsMessage sms = (SmsMessage) ar.result;
            if (sms == null) {
                loge("Null injected sms");
                result = RESULT_SMS_NULL_PDU;
            } else {
                @SmsSource int smsSource =
                        isOverIms ? SOURCE_INJECTED_FROM_IMS : SOURCE_INJECTED_FROM_UNKNOWN;
                result = dispatchMessage(sms.mWrappedSmsMessage, smsSource);
            }
        } catch (RuntimeException ex) {
            loge("Exception dispatching message", ex);
            result = RESULT_SMS_DISPATCH_FAILURE;
        }

        if (callback != null) {
            callback.onSmsInjectedResult(result);
        }
    }

    /**
     * Process an SMS message from the RIL, calling subclass methods to handle 3GPP and
     * 3GPP2-specific message types.
     *
     * @param smsb the SmsMessageBase object from the RIL
     * @param smsSource the source of the SMS message
     * @return a result code from {@link android.provider.Telephony.Sms.Intents},
     *  or {@link Activity#RESULT_OK} for delayed acknowledgment to SMSC
     */
    private int dispatchMessage(SmsMessageBase smsb, @SmsSource int smsSource) {
        // If sms is null, there was a parsing error.
        if (smsb == null) {
            loge("dispatchSmsMessage: message is null");
            return RESULT_SMS_NULL_MESSAGE;
        }

        if (mSmsReceiveDisabled) {
            // Device doesn't support receiving SMS,
            log("Received short message on device which doesn't support "
                    + "receiving SMS. Ignored.");
            return Intents.RESULT_SMS_HANDLED;
        }

        // onlyCore indicates if the device is in cryptkeeper
        boolean onlyCore = false;
        try {
            onlyCore = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
                    .isOnlyCoreApps();
        } catch (RemoteException e) {
        }
        if (onlyCore) {
            // Device is unable to receive SMS in encrypted state
            log("Received a short message in encrypted state. Rejecting.");
            return Intents.RESULT_SMS_RECEIVED_WHILE_ENCRYPTED;
        }

        int result = dispatchMessageRadioSpecific(smsb, smsSource);

        // In case of error, add to metrics. This is not required in case of success, as the
        // data will be tracked when the message is processed (processMessagePart).
        if (result != Intents.RESULT_SMS_HANDLED && result != Activity.RESULT_OK) {
            mMetrics.writeIncomingSmsError(mPhone.getPhoneId(), is3gpp2(), smsSource, result);
            mPhone.getSmsStats().onIncomingSmsError(is3gpp2(), smsSource, result);
        }
        return result;
    }

    /**
     * Process voicemail notification, SMS-PP data download, CDMA CMAS, CDMA WAP push, and other
     * 3GPP/3GPP2-specific messages. Regular SMS messages are handled by calling the shared
     * {@link #dispatchNormalMessage} from this class.
     *
     * @param smsb the SmsMessageBase object from the RIL
     * @param smsSource the source of the SMS message
     * @return a result code from {@link android.provider.Telephony.Sms.Intents},
     *  or {@link Activity#RESULT_OK} for delayed acknowledgment to SMSC
     */
    protected abstract int dispatchMessageRadioSpecific(SmsMessageBase smsb,
            @SmsSource int smsSource);

    /**
     * Send an acknowledge message to the SMSC.
     * @param success indicates that last message was successfully received.
     * @param result result code indicating any error
     * @param response callback message sent when operation completes.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected abstract void acknowledgeLastIncomingSms(boolean success,
            int result, Message response);

    /**
     * Notify interested apps if the framework has rejected an incoming SMS,
     * and send an acknowledge message to the network.
     * @param success indicates that last message was successfully received.
     * @param result result code indicating any error
     * @param response callback message sent when operation completes.
     */
    private void notifyAndAcknowledgeLastIncomingSms(boolean success,
            int result, Message response) {
        if (!success) {
            // broadcast SMS_REJECTED_ACTION intent
            Intent intent = new Intent(Intents.SMS_REJECTED_ACTION);
            intent.putExtra("result", result);
            intent.putExtra("subId", mPhone.getSubId());
            mContext.sendBroadcast(intent, android.Manifest.permission.RECEIVE_SMS);
        }
        acknowledgeLastIncomingSms(success, result, response);
    }

    /**
     * Return true if this handler is for 3GPP2 messages; false for 3GPP format.
     * @return true for the 3GPP2 handler; false for the 3GPP handler
     */
    protected abstract boolean is3gpp2();

    /**
     * Dispatch a normal incoming SMS. This is called from {@link #dispatchMessageRadioSpecific}
     * if no format-specific handling was required. Saves the PDU to the SMS provider raw table,
     * creates an {@link InboundSmsTracker}, then sends it to the state machine as an
     * {@link #EVENT_BROADCAST_SMS}. Returns {@link Intents#RESULT_SMS_HANDLED} or an error value.
     *
     * @param sms the message to dispatch
     * @param smsSource the source of the SMS message
     * @return {@link Intents#RESULT_SMS_HANDLED} if the message was accepted, or an error status
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected int dispatchNormalMessage(SmsMessageBase sms, @SmsSource int smsSource) {
        SmsHeader smsHeader = sms.getUserDataHeader();
        InboundSmsTracker tracker;

        if ((smsHeader == null) || (smsHeader.concatRef == null)) {
            // Message is not concatenated.
            int destPort = -1;
            if (smsHeader != null && smsHeader.portAddrs != null) {
                // The message was sent to a port.
                destPort = smsHeader.portAddrs.destPort;
                if (DBG) log("destination port: " + destPort);
            }
            tracker = TelephonyComponentFactory.getInstance()
                    .inject(InboundSmsTracker.class.getName())
                    .makeInboundSmsTracker(mContext, sms.getPdu(),
                            sms.getTimestampMillis(), destPort, is3gpp2(), false,
                            sms.getOriginatingAddress(), sms.getDisplayOriginatingAddress(),
                            sms.getMessageBody(), sms.getMessageClass() == MessageClass.CLASS_0,
                            mPhone.getSubId(), smsSource);
        } else {
            // Create a tracker for this message segment.
            SmsHeader.ConcatRef concatRef = smsHeader.concatRef;
            SmsHeader.PortAddrs portAddrs = smsHeader.portAddrs;
            int destPort = (portAddrs != null ? portAddrs.destPort : -1);
            tracker = TelephonyComponentFactory.getInstance()
                    .inject(InboundSmsTracker.class.getName())
                    .makeInboundSmsTracker(mContext, sms.getPdu(),
                            sms.getTimestampMillis(), destPort, is3gpp2(),
                            sms.getOriginatingAddress(), sms.getDisplayOriginatingAddress(),
                            concatRef.refNumber, concatRef.seqNumber, concatRef.msgCount, false,
                            sms.getMessageBody(), sms.getMessageClass() == MessageClass.CLASS_0,
                            mPhone.getSubId(), smsSource);
        }

        if (VDBG) log("created tracker: " + tracker);

        // de-duping is done only for text messages
        // destPort = -1 indicates text messages, otherwise it's a data sms
        return addTrackerToRawTableAndSendMessage(tracker,
                tracker.getDestPort() == -1 /* de-dup if text message */);
    }

    /**
     * Helper to add the tracker to the raw table and then send a message to broadcast it, if
     * successful. Returns the SMS intent status to return to the SMSC.
     * @param tracker the tracker to save to the raw table and then deliver
     * @return {@link Intents#RESULT_SMS_HANDLED} or one of these errors:<br>
     * <code>RESULT_SMS_UNSUPPORTED</code><br>
     * <code>RESULT_SMS_DUPLICATED</code><br>
     * <code>RESULT_SMS_DISPATCH_FAILURE</code><br>
     * <code>RESULT_SMS_NULL_PDU</code><br>
     * <code>RESULT_SMS_NULL_MESSAGE</code><br>
     * <code>RESULT_SMS_RECEIVED_WHILE_ENCRYPTED</code><br>
     * <code>RESULT_SMS_DATABASE_ERROR</code><br>
     * <code>RESULT_SMS_INVALID_URI</code><br>
     */
    protected int addTrackerToRawTableAndSendMessage(InboundSmsTracker tracker, boolean deDup) {
        int result = addTrackerToRawTable(tracker, deDup);
        switch(result) {
            case Intents.RESULT_SMS_HANDLED:
                sendMessage(EVENT_BROADCAST_SMS, tracker);
                return Intents.RESULT_SMS_HANDLED;

            case Intents.RESULT_SMS_DUPLICATED:
                return Intents.RESULT_SMS_HANDLED;

            default:
                return result;
        }
    }

    /**
     * Process the inbound SMS segment. If the message is complete, send it as an ordered
     * broadcast to interested receivers and return true. If the message is a segment of an
     * incomplete multi-part SMS, return false.
     * @param tracker the tracker containing the message segment to process
     * @return true if an ordered broadcast was sent; false if waiting for more message segments
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean processMessagePart(InboundSmsTracker tracker) {
        int messageCount = tracker.getMessageCount();
        byte[][] pdus;
        long[] timestamps;
        int destPort = tracker.getDestPort();
        boolean block = false;
        String address = tracker.getAddress();

        // Do not process when the message count is invalid.
        if (messageCount <= 0) {
            loge("processMessagePart: returning false due to invalid message count "
                    + messageCount, tracker.getMessageId());
            return false;
        }

        if (messageCount == 1) {
            // single-part message
            pdus = new byte[][]{tracker.getPdu()};
            timestamps = new long[]{tracker.getTimestamp()};
            block = BlockChecker.isBlocked(mContext, tracker.getDisplayAddress(), null);
        } else {
            // multi-part message
            Cursor cursor = null;
            try {
                // used by several query selection arguments
                String refNumber = Integer.toString(tracker.getReferenceNumber());
                String count = Integer.toString(tracker.getMessageCount());

                // query for all segments and broadcast message if we have all the parts
                String[] whereArgs = {address, refNumber, count};
                cursor = mResolver.query(sRawUri, PDU_SEQUENCE_PORT_PROJECTION,
                        tracker.getQueryForSegments(), whereArgs, null);

                int cursorCount = cursor.getCount();
                if (cursorCount < messageCount) {
                    // Wait for the other message parts to arrive. It's also possible for the last
                    // segment to arrive before processing the EVENT_BROADCAST_SMS for one of the
                    // earlier segments. In that case, the broadcast will be sent as soon as all
                    // segments are in the table, and any later EVENT_BROADCAST_SMS messages will
                    // get a row count of 0 and return.
                    log("processMessagePart: returning false. Only " + cursorCount + " of "
                            + messageCount + " segments " + " have arrived. refNumber: "
                            + refNumber, tracker.getMessageId());
                    return false;
                }

                // All the parts are in place, deal with them
                pdus = new byte[messageCount][];
                timestamps = new long[messageCount];
                while (cursor.moveToNext()) {
                    // subtract offset to convert sequence to 0-based array index
                    int index = cursor.getInt(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING
                            .get(SEQUENCE_COLUMN)) - tracker.getIndexOffset();

                    // The invalid PDUs can be received and stored in the raw table. The range
                    // check ensures the process not crash even if the seqNumber in the
                    // UserDataHeader is invalid.
                    if (index >= pdus.length || index < 0) {
                        loge(String.format(
                                "processMessagePart: invalid seqNumber = %d, messageCount = %d",
                                index + tracker.getIndexOffset(),
                                messageCount),
                                tracker.getMessageId());
                        continue;
                    }

                    pdus[index] = HexDump.hexStringToByteArray(cursor.getString(
                            PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(PDU_COLUMN)));

                    // Read the destination port from the first segment (needed for CDMA WAP PDU).
                    // It's not a bad idea to prefer the port from the first segment in other cases.
                    if (index == 0 && !cursor.isNull(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING
                            .get(DESTINATION_PORT_COLUMN))) {
                        int port = cursor.getInt(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING
                                .get(DESTINATION_PORT_COLUMN));
                        // strip format flags and convert to real port number, or -1
                        port = InboundSmsTracker.getRealDestPort(port);
                        if (port != -1) {
                            destPort = port;
                        }
                    }

                    timestamps[index] = cursor.getLong(
                            PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(DATE_COLUMN));

                    // check if display address should be blocked or not
                    if (!block) {
                        // Depending on the nature of the gateway, the display origination address
                        // is either derived from the content of the SMS TP-OA field, or the TP-OA
                        // field contains a generic gateway address and the from address is added
                        // at the beginning in the message body. In that case only the first SMS
                        // (part of Multi-SMS) comes with the display originating address which
                        // could be used for block checking purpose.
                        block = BlockChecker.isBlocked(mContext,
                                cursor.getString(PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING
                                        .get(DISPLAY_ADDRESS_COLUMN)), null);
                    }
                }
                log("processMessagePart: all " + messageCount + " segments "
                        + " received. refNumber: " + refNumber, tracker.getMessageId());
            } catch (SQLException e) {
                loge("processMessagePart: Can't access multipart SMS database, "
                        + SmsController.formatCrossStackMessageId(tracker.getMessageId()), e);
                return false;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        final boolean isWapPush = (destPort == SmsHeader.PORT_WAP_PUSH);
        String format = tracker.getFormat();

        // Do not process null pdu(s). Check for that and return false in that case.
        List<byte[]> pduList = Arrays.asList(pdus);
        if (pduList.size() == 0 || pduList.contains(null)) {
            String errorMsg = "processMessagePart: returning false due to "
                    + (pduList.size() == 0 ? "pduList.size() == 0" : "pduList.contains(null)");
            logeWithLocalLog(errorMsg, tracker.getMessageId());
            mPhone.getSmsStats().onIncomingSmsError(
                    is3gpp2(), tracker.getSource(), RESULT_SMS_NULL_PDU);
            return false;
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (isWapPush) {
            for (byte[] pdu : pdus) {
                // 3GPP needs to extract the User Data from the PDU; 3GPP2 has already done this
                if (format == SmsConstants.FORMAT_3GPP) {
                    SmsMessage msg = SmsMessage.createFromPdu(pdu, SmsConstants.FORMAT_3GPP);
                    if (msg != null) {
                        pdu = msg.getUserData();
                    } else {
                        loge("processMessagePart: SmsMessage.createFromPdu returned null",
                                tracker.getMessageId());
                        mMetrics.writeIncomingWapPush(mPhone.getPhoneId(), tracker.getSource(),
                                SmsConstants.FORMAT_3GPP, timestamps, false,
                                tracker.getMessageId());
                        mPhone.getSmsStats().onIncomingSmsWapPush(tracker.getSource(),
                                messageCount, RESULT_SMS_NULL_MESSAGE, tracker.getMessageId());
                        return false;
                    }
                }
                output.write(pdu, 0, pdu.length);
            }
        }

        SmsBroadcastReceiver resultReceiver = tracker.getSmsBroadcastReceiver(this);

        if (!mUserManager.isUserUnlocked()) {
            log("processMessagePart: !isUserUnlocked; calling processMessagePartWithUserLocked. "
                    + "Port: " + destPort, tracker.getMessageId());
            return processMessagePartWithUserLocked(
                    tracker,
                    (isWapPush ? new byte[][] {output.toByteArray()} : pdus),
                    destPort,
                    resultReceiver,
                    block);
        }

        if (isWapPush) {
            int result = mWapPush.dispatchWapPdu(output.toByteArray(), resultReceiver,
                    this, address, tracker.getSubId(), tracker.getMessageId());
            if (DBG) {
                log("processMessagePart: dispatchWapPdu() returned " + result,
                        tracker.getMessageId());
            }
            // Add result of WAP-PUSH into metrics. RESULT_SMS_HANDLED indicates that the WAP-PUSH
            // needs to be ignored, so treating it as a success case.
            boolean wapPushResult =
                    result == Activity.RESULT_OK || result == Intents.RESULT_SMS_HANDLED;
            mMetrics.writeIncomingWapPush(mPhone.getPhoneId(), tracker.getSource(),
                    format, timestamps, wapPushResult, tracker.getMessageId());
            mPhone.getSmsStats().onIncomingSmsWapPush(tracker.getSource(), messageCount,
                    result, tracker.getMessageId());
            // result is Activity.RESULT_OK if an ordered broadcast was sent
            if (result == Activity.RESULT_OK) {
                return true;
            } else {
                deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(),
                        MARK_DELETED);
                loge("processMessagePart: returning false as the ordered broadcast for WAP push "
                        + "was not sent", tracker.getMessageId());
                return false;
            }
        }

        // All parts of SMS are received. Update metrics for incoming SMS.
        // The metrics are generated before SMS filters are invoked.
        // For messages composed by multiple parts, the metrics are generated considering the
        // characteristics of the last one.
        mMetrics.writeIncomingSmsSession(mPhone.getPhoneId(), tracker.getSource(),
                format, timestamps, block, tracker.getMessageId());
        mPhone.getSmsStats().onIncomingSmsSuccess(is3gpp2(), tracker.getSource(),
                messageCount, block, tracker.getMessageId());

        // Always invoke SMS filters, even if the number ends up being blocked, to prevent
        // surprising bugs due to blocking numbers that happen to be used for visual voicemail SMS
        // or other carrier system messages.
        boolean filterInvoked = filterSms(
                pdus, destPort, tracker, resultReceiver, true /* userUnlocked */, block);

        if (!filterInvoked) {
            // Block now if the filter wasn't invoked. Otherwise, it will be the responsibility of
            // the filter to delete the SMS once processing completes.
            if (block) {
                deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(),
                        DELETE_PERMANENTLY);
                log("processMessagePart: returning false as the phone number is blocked",
                        tracker.getMessageId());
                return false;
            }

            dispatchSmsDeliveryIntent(pdus, format, destPort, resultReceiver,
                    tracker.isClass0(), tracker.getSubId(), tracker.getMessageId());
        }

        return true;
    }

    /**
     * Processes the message part while the credential-encrypted storage is still locked.
     *
     * <p>If the message is a regular MMS, show a new message notification. If the message is a
     * SMS, ask the carrier app to filter it and show the new message notification if the carrier
     * app asks to keep the message.
     *
     * @return true if an ordered broadcast was sent to the carrier app; false otherwise.
     */
    private boolean processMessagePartWithUserLocked(InboundSmsTracker tracker,
            byte[][] pdus, int destPort, SmsBroadcastReceiver resultReceiver, boolean block) {
        if (destPort == SmsHeader.PORT_WAP_PUSH && mWapPush.isWapPushForMms(pdus[0], this)) {
            showNewMessageNotification();
            return false;
        }
        if (destPort == -1) {
            // This is a regular SMS - hand it to the carrier or system app for filtering.
            boolean filterInvoked = filterSms(
                    pdus, destPort, tracker, resultReceiver, false /* userUnlocked */,
                    block);
            if (filterInvoked) {
                // filter invoked, wait for it to return the result.
                return true;
            } else if (!block) {
                // filter not invoked and message not blocked, show the notification and do nothing
                // further. Even if the message is blocked, we keep it in the database so it can be
                // reprocessed by filters once credential-encrypted storage is available.
                showNewMessageNotification();
            }
        }
        return false;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void showNewMessageNotification() {
        // Do not show the notification on non-FBE devices.
        if (!StorageManager.isFileEncryptedNativeOrEmulated()) {
            return;
        }
        log("Show new message notification.");
        PendingIntent intent = PendingIntent.getBroadcast(
            mContext,
            0,
            new Intent(ACTION_OPEN_SMS_APP),
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder mBuilder = new Notification.Builder(mContext)
                .setSmallIcon(com.android.internal.R.drawable.sym_action_chat)
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentTitle(mContext.getString(R.string.new_sms_notification_title))
                .setContentText(mContext.getString(R.string.new_sms_notification_content))
                .setContentIntent(intent)
                .setChannelId(NotificationChannelController.CHANNEL_ID_SMS);
        NotificationManager mNotificationManager =
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(
                NOTIFICATION_TAG, NOTIFICATION_ID_NEW_MESSAGE, mBuilder.build());
    }

    static void cancelNewMessageNotification(Context context) {
        NotificationManager mNotificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(InboundSmsHandler.NOTIFICATION_TAG,
            InboundSmsHandler.NOTIFICATION_ID_NEW_MESSAGE);
    }

    /**
     * Creates the default filters used to filter SMS messages.
     *
     * <p>Currently 3 filters exist: the carrier package, the VisualVoicemailSmsFilter, and the
     * missed incoming call SMS filter.
     *
     * <p>Since the carrier filter is asynchronous, if a message passes through the carrier filter,
     * the remaining filters will be applied in the callback.
     */
    private List<SmsFilter> createDefaultSmsFilters() {
        List<SmsFilter> smsFilters = new ArrayList<>(3);
        smsFilters.add(
                (pdus, destPort, tracker, resultReceiver, userUnlocked, block, remainingFilters)
                        -> {
                    CarrierServicesSmsFilterCallback filterCallback =
                            new CarrierServicesSmsFilterCallback(
                                    pdus, destPort, tracker, tracker.getFormat(), resultReceiver,
                                    userUnlocked,
                                    tracker.isClass0(), tracker.getSubId(), tracker.getMessageId(),
                                    block, remainingFilters);
                    CarrierServicesSmsFilter carrierServicesFilter = new CarrierServicesSmsFilter(
                            mContext, mPhone, pdus, destPort, tracker.getFormat(),
                            filterCallback, getName() + "::CarrierServicesSmsFilter",
                            mCarrierServiceLocalLog, tracker.getMessageId());
                    if (carrierServicesFilter.filter()) {
                        log("SMS is being handled by carrier service", tracker.getMessageId());
                        return true;
                    } else {
                        return false;
                    }
                });
        smsFilters.add(
                (pdus, destPort, tracker, resultReceiver, userUnlocked, block, remainingFilters)
                        -> {
                    if (VisualVoicemailSmsFilter.filter(
                            mContext, pdus, tracker.getFormat(), destPort, tracker.getSubId())) {
                        logWithLocalLog("Visual voicemail SMS dropped", tracker.getMessageId());
                        dropFilteredSms(tracker, resultReceiver, block);
                        return true;
                    }
                    return false;
                });
        smsFilters.add(
                (pdus, destPort, tracker, resultReceiver, userUnlocked, block, remainingFilters)
                        -> {
                    MissedIncomingCallSmsFilter missedIncomingCallSmsFilter =
                            new MissedIncomingCallSmsFilter(mPhone);
                    if (missedIncomingCallSmsFilter.filter(pdus, tracker.getFormat())) {
                        logWithLocalLog("Missed incoming call SMS received",
                                tracker.getMessageId());
                        dropFilteredSms(tracker, resultReceiver, block);
                        return true;
                    }
                    return false;
                });
        return smsFilters;
    }

    private void dropFilteredSms(
            InboundSmsTracker tracker, SmsBroadcastReceiver resultReceiver, boolean block) {
        if (block) {
            deleteFromRawTable(
                    tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(),
                    DELETE_PERMANENTLY);
            sendMessage(EVENT_BROADCAST_COMPLETE);
        } else {
            dropSms(resultReceiver);
        }
    }

    /**
     * Filters the SMS.
     *
     * <p>Each filter in {@link #mSmsFilters} is invoked sequentially. If any filter returns true,
     * this method returns true and subsequent filters are ignored.
     *
     * @return true if a filter is invoked and the SMS processing flow is diverted, false otherwise.
     */
    private boolean filterSms(byte[][] pdus, int destPort,
            InboundSmsTracker tracker, SmsBroadcastReceiver resultReceiver, boolean userUnlocked,
            boolean block) {
        return filterSms(pdus, destPort, tracker, resultReceiver, userUnlocked, block, mSmsFilters);
    }

    private static boolean filterSms(byte[][] pdus, int destPort,
            InboundSmsTracker tracker, SmsBroadcastReceiver resultReceiver, boolean userUnlocked,
            boolean block, List<SmsFilter> filters) {
        ListIterator<SmsFilter> iterator = filters.listIterator();
        while (iterator.hasNext()) {
            SmsFilter smsFilter = iterator.next();
            if (smsFilter.filterSms(pdus, destPort, tracker, resultReceiver, userUnlocked, block,
                    filters.subList(iterator.nextIndex(), filters.size()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatch the intent with the specified permission, appOp, and result receiver, using
     * this state machine's handler thread to run the result receiver.
     *
     * @param intent the intent to broadcast
     * @param permission receivers are required to have this permission
     * @param appOp app op that is being performed when dispatching to a receiver
     * @param user user to deliver the intent to
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void dispatchIntent(Intent intent, String permission, String appOp,
            Bundle opts, SmsBroadcastReceiver resultReceiver, UserHandle user, int subId) {
        intent.addFlags(Intent.FLAG_RECEIVER_NO_ABORT);
        final String action = intent.getAction();
        if (Intents.SMS_DELIVER_ACTION.equals(action)
                || Intents.SMS_RECEIVED_ACTION.equals(action)
                || Intents.WAP_PUSH_DELIVER_ACTION.equals(action)
                || Intents.WAP_PUSH_RECEIVED_ACTION.equals(action)) {
            // Some intents need to be delivered with high priority:
            // SMS_DELIVER, SMS_RECEIVED, WAP_PUSH_DELIVER, WAP_PUSH_RECEIVED
            // In some situations, like after boot up or system under load, normal
            // intent delivery could take a long time.
            // This flag should only be set for intents for visible, timely operations
            // which is true for the intents above.
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());

        // override the subId value in the intent with the values from tracker as they can be
        // different, specifically if the message is coming from SmsBroadcastUndelivered
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            SubscriptionManager.putSubscriptionIdExtra(intent, subId);
        }

        if (user.equals(UserHandle.ALL)) {
            // Get a list of currently started users.
            int[] users = null;
            final List<UserHandle> userHandles = mUserManager.getUserHandles(false);
            final List<UserHandle> runningUserHandles = new ArrayList();
            for (UserHandle handle : userHandles) {
                if (mUserManager.isUserRunning(handle)) {
                    runningUserHandles.add(handle);
                } else {
                    if (handle.equals(UserHandle.SYSTEM)) {
                        logeWithLocalLog("dispatchIntent: SYSTEM user is not running",
                                resultReceiver.mInboundSmsTracker.getMessageId());
                    }
                }
            }
            if (runningUserHandles.isEmpty()) {
                users = new int[] {user.getIdentifier()};
            } else {
                users = new int[runningUserHandles.size()];
                for (int i = 0; i < runningUserHandles.size(); i++) {
                    users[i] = runningUserHandles.get(i).getIdentifier();
                }
            }
            // Deliver the broadcast only to those running users that are permitted
            // by user policy.
            for (int i = users.length - 1; i >= 0; i--) {
                UserHandle targetUser = UserHandle.of(users[i]);
                if (users[i] != UserHandle.SYSTEM.getIdentifier()) {
                    // Is the user not allowed to use SMS?
                    if (hasUserRestriction(UserManager.DISALLOW_SMS, targetUser)) {
                        continue;
                    }
                    // Skip unknown users and managed profiles as well
                    if (mUserManager.isManagedProfile(users[i])) {
                        continue;
                    }
                }
                // Only pass in the resultReceiver when the user SYSTEM is processed.
                try {
                    if (users[i] == UserHandle.SYSTEM.getIdentifier()) {
                        resultReceiver.setWaitingForIntent(intent);
                    }
                    mContext.createPackageContextAsUser(mContext.getPackageName(), 0, targetUser)
                            .sendOrderedBroadcast(intent, Activity.RESULT_OK, permission, appOp,
                                    users[i] == UserHandle.SYSTEM.getIdentifier()
                                            ? resultReceiver : null, getHandler(),
                                    null /* initialData */, null /* initialExtras */, opts);
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
        } else {
            try {
                resultReceiver.setWaitingForIntent(intent);
                mContext.createPackageContextAsUser(mContext.getPackageName(), 0, user)
                        .sendOrderedBroadcast(intent, Activity.RESULT_OK, permission, appOp,
                                resultReceiver, getHandler(), null /* initialData */,
                                null /* initialExtras */, opts);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
    }

    private boolean hasUserRestriction(String restrictionKey, UserHandle userHandle) {
        final List<UserManager.EnforcingUser> sources = mUserManager
                .getUserRestrictionSources(restrictionKey, userHandle);
        return (sources != null && !sources.isEmpty());
    }

    /**
     * Helper for {@link SmsBroadcastUndelivered} to delete an old message in the raw table.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void deleteFromRawTable(String deleteWhere, String[] deleteWhereArgs,
                                    int deleteType) {
        Uri uri = deleteType == DELETE_PERMANENTLY ? sRawUriPermanentDelete : sRawUri;
        int rows = mResolver.delete(uri, deleteWhere, deleteWhereArgs);
        if (rows == 0) {
            loge("No rows were deleted from raw table!");
        } else if (DBG) {
            log("Deleted " + rows + " rows from raw table.");
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private Bundle handleSmsWhitelisting(ComponentName target, boolean bgActivityStartAllowed) {
        String pkgName;
        String reason;
        if (target != null) {
            pkgName = target.getPackageName();
            reason = "sms-app";
        } else {
            pkgName = mContext.getPackageName();
            reason = "sms-broadcast";
        }
        BroadcastOptions bopts = null;
        Bundle bundle = null;
        if (bgActivityStartAllowed) {
            bopts = BroadcastOptions.makeBasic();
            bopts.setBackgroundActivityStartsAllowed(true);
            bundle = bopts.toBundle();
        }
        long duration = mPowerWhitelistManager.whitelistAppTemporarilyForEvent(
                pkgName, PowerWhitelistManager.EVENT_SMS, REASON_EVENT_SMS, reason);
        if (bopts == null) bopts = BroadcastOptions.makeBasic();
        bopts.setTemporaryAppAllowlist(duration,
                TEMPORARY_ALLOWLIST_TYPE_FOREGROUND_SERVICE_ALLOWED,
                REASON_EVENT_SMS,
                "");
        bundle = bopts.toBundle();

        return bundle;
    }

    /**
     * Creates and dispatches the intent to the default SMS app, appropriate port or via the {@link
     * AppSmsManager}.
     *
     * @param pdus message pdus
     * @param format the message format, typically "3gpp" or "3gpp2"
     * @param destPort the destination port
     * @param resultReceiver the receiver handling the delivery result
     */
    private void dispatchSmsDeliveryIntent(byte[][] pdus, String format, int destPort,
            SmsBroadcastReceiver resultReceiver, boolean isClass0, int subId, long messageId) {
        Intent intent = new Intent();
        intent.putExtra("pdus", pdus);
        intent.putExtra("format", format);
        if (messageId != 0L) {
            intent.putExtra("messageId", messageId);
        }

        if (destPort == -1) {
            intent.setAction(Intents.SMS_DELIVER_ACTION);
            // Direct the intent to only the default SMS app. If we can't find a default SMS app
            // then sent it to all broadcast receivers.
            // We are deliberately delivering to the primary user's default SMS App.
            ComponentName componentName = SmsApplication.getDefaultSmsApplication(mContext, true);
            if (componentName != null) {
                // Deliver SMS message only to this receiver.
                intent.setComponent(componentName);
                logWithLocalLog("Delivering SMS to: " + componentName.getPackageName()
                        + " " + componentName.getClassName(), messageId);
            } else {
                intent.setComponent(null);
            }

            // Handle app specific sms messages.
            AppSmsManager appManager = mPhone.getAppSmsManager();
            if (appManager.handleSmsReceivedIntent(intent)) {
                // The AppSmsManager handled this intent, we're done.
                dropSms(resultReceiver);
                return;
            }
        } else {
            intent.setAction(Intents.DATA_SMS_RECEIVED_ACTION);
            Uri uri = Uri.parse("sms://localhost:" + destPort);
            intent.setData(uri);
            intent.setComponent(null);
        }

        Bundle options = handleSmsWhitelisting(intent.getComponent(), isClass0);
        dispatchIntent(intent, android.Manifest.permission.RECEIVE_SMS,
                AppOpsManager.OPSTR_RECEIVE_SMS, options, resultReceiver, UserHandle.SYSTEM, subId);
    }

    /**
     * Function to detect and handle duplicate messages. If the received message should replace an
     * existing message in the raw db, this function deletes the existing message. If an existing
     * message takes priority (for eg, existing message has already been broadcast), then this new
     * message should be dropped.
     * @return true if the message represented by the passed in tracker should be dropped,
     * false otherwise
     */
    private boolean checkAndHandleDuplicate(InboundSmsTracker tracker) throws SQLException {
        Pair<String, String[]> exactMatchQuery = tracker.getExactMatchDupDetectQuery();

        Cursor cursor = null;
        try {
            // Check for duplicate message segments
            cursor = mResolver.query(sRawUri, PDU_DELETED_FLAG_PROJECTION, exactMatchQuery.first,
                    exactMatchQuery.second, null);

            // moveToNext() returns false if no duplicates were found
            if (cursor != null && cursor.moveToNext()) {
                if (cursor.getCount() != 1) {
                    logeWithLocalLog("checkAndHandleDuplicate: Exact match query returned "
                            + cursor.getCount() + " rows", tracker.getMessageId());
                }

                // if the exact matching row is marked deleted, that means this message has already
                // been received and processed, and can be discarded as dup
                if (cursor.getInt(
                        PDU_DELETED_FLAG_PROJECTION_INDEX_MAPPING.get(DELETED_FLAG_COLUMN)) == 1) {
                    logWithLocalLog("checkAndHandleDuplicate: Discarding duplicate "
                            + "message/segment: " + tracker);
                    logDupPduMismatch(cursor, tracker);
                    return true;   // reject message
                } else {
                    // exact match duplicate is not marked deleted. If it is a multi-part segment,
                    // the code below for inexact match will take care of it. If it is a single
                    // part message, handle it here.
                    if (tracker.getMessageCount() == 1) {
                        // delete the old message segment permanently
                        deleteFromRawTable(exactMatchQuery.first, exactMatchQuery.second,
                                DELETE_PERMANENTLY);
                        logWithLocalLog("checkAndHandleDuplicate: Replacing duplicate message: "
                                + tracker);
                        logDupPduMismatch(cursor, tracker);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // The code above does an exact match. Multi-part message segments need an additional check
        // on top of that: if there is a message segment that conflicts this new one (may not be an
        // exact match), replace the old message segment with this one.
        if (tracker.getMessageCount() > 1) {
            Pair<String, String[]> inexactMatchQuery = tracker.getInexactMatchDupDetectQuery();
            cursor = null;
            try {
                // Check for duplicate message segments
                cursor = mResolver.query(sRawUri, PDU_DELETED_FLAG_PROJECTION,
                        inexactMatchQuery.first, inexactMatchQuery.second, null);

                // moveToNext() returns false if no duplicates were found
                if (cursor != null && cursor.moveToNext()) {
                    if (cursor.getCount() != 1) {
                        logeWithLocalLog("checkAndHandleDuplicate: Inexact match query returned "
                                + cursor.getCount() + " rows", tracker.getMessageId());
                    }
                    // delete the old message segment permanently
                    deleteFromRawTable(inexactMatchQuery.first, inexactMatchQuery.second,
                            DELETE_PERMANENTLY);
                    logWithLocalLog("checkAndHandleDuplicate: Replacing duplicate message segment: "
                            + tracker);
                    logDupPduMismatch(cursor, tracker);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return false;
    }

    private void logDupPduMismatch(Cursor cursor, InboundSmsTracker tracker) {
        String oldPduString = cursor.getString(
                PDU_DELETED_FLAG_PROJECTION_INDEX_MAPPING.get(PDU_COLUMN));
        byte[] pdu = tracker.getPdu();
        byte[] oldPdu = HexDump.hexStringToByteArray(oldPduString);
        if (!Arrays.equals(oldPdu, tracker.getPdu())) {
            logeWithLocalLog("Warning: dup message PDU of length " + pdu.length
                    + " is different from existing PDU of length " + oldPdu.length,
                    tracker.getMessageId());
        }
    }

    /**
     * Insert a message PDU into the raw table so we can acknowledge it immediately.
     * If the device crashes before the broadcast to listeners completes, it will be delivered
     * from the raw table on the next device boot. For single-part messages, the deleteWhere
     * and deleteWhereArgs fields of the tracker will be set to delete the correct row after
     * the ordered broadcast completes.
     *
     * @param tracker the tracker to add to the raw table
     * @return true on success; false on failure to write to database
     */
    private int addTrackerToRawTable(InboundSmsTracker tracker, boolean deDup) {
        if (deDup) {
            try {
                if (checkAndHandleDuplicate(tracker)) {
                    return Intents.RESULT_SMS_DUPLICATED;   // reject message
                }
            } catch (SQLException e) {
                loge("addTrackerToRawTable: Can't access SMS database, "
                        + SmsController.formatCrossStackMessageId(tracker.getMessageId()), e);
                return RESULT_SMS_DATABASE_ERROR;    // reject message
            }
        } else {
            log("addTrackerToRawTable: Skipped message de-duping logic", tracker.getMessageId());
        }

        String address = tracker.getAddress();
        String refNumber = Integer.toString(tracker.getReferenceNumber());
        String count = Integer.toString(tracker.getMessageCount());
        ContentValues values = tracker.getContentValues();

        if (VDBG) {
            log("addTrackerToRawTable: adding content values to raw table: " + values.toString(),
                    tracker.getMessageId());
        }
        Uri newUri = mResolver.insert(sRawUri, values);
        if (DBG) log("addTrackerToRawTable: URI of new row: " + newUri, tracker.getMessageId());

        try {
            long rowId = ContentUris.parseId(newUri);
            if (tracker.getMessageCount() == 1) {
                // set the delete selection args for single-part message
                tracker.setDeleteWhere(SELECT_BY_ID, new String[]{Long.toString(rowId)});
            } else {
                // set the delete selection args for multi-part message
                String[] deleteWhereArgs = {address, refNumber, count};
                tracker.setDeleteWhere(tracker.getQueryForSegments(), deleteWhereArgs);
            }
            return Intents.RESULT_SMS_HANDLED;
        } catch (Exception e) {
            loge("addTrackerToRawTable: error parsing URI for new row: " + newUri
                    + " " + SmsController.formatCrossStackMessageId(tracker.getMessageId()), e);
            return RESULT_SMS_INVALID_URI;
        }
    }

    /**
     * Returns whether the default message format for the current radio technology is 3GPP2.
     * @return true if the radio technology uses 3GPP2 format by default, false for 3GPP format
     */
    static boolean isCurrentFormat3gpp2() {
        int activePhone = TelephonyManager.getDefault().getCurrentPhoneType();
        return (PHONE_TYPE_CDMA == activePhone);
    }

    @VisibleForTesting
    public static int sTimeoutDurationMillis = 10 * 60 * 1000; // 10 minutes

    /**
     * Handler for an {@link InboundSmsTracker} broadcast. Deletes PDUs from the raw table and
     * logs the broadcast duration (as an error if the other receivers were especially slow).
     */
    public final class SmsBroadcastReceiver extends BroadcastReceiver {
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private final String mDeleteWhere;
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private final String[] mDeleteWhereArgs;
        private long mBroadcastTimeMillis;
        public Intent mWaitingForIntent;
        private final InboundSmsTracker mInboundSmsTracker;

        /**
         * This method must be called anytime an ordered broadcast is sent that is expected to be
         * received by this receiver.
         */
        public synchronized void setWaitingForIntent(Intent intent) {
            mWaitingForIntent = intent;
            mBroadcastTimeMillis = System.currentTimeMillis();
            removeMessages(EVENT_RECEIVER_TIMEOUT);
            sendMessageDelayed(EVENT_RECEIVER_TIMEOUT, sTimeoutDurationMillis);
        }

        public SmsBroadcastReceiver(InboundSmsTracker tracker) {
            mDeleteWhere = tracker.getDeleteWhere();
            mDeleteWhereArgs = tracker.getDeleteWhereArgs();
            mInboundSmsTracker = tracker;
        }

        /**
         * This method is called if the expected intent (mWaitingForIntent) is not received and
         * the timer for it expires. It fakes the receipt of the intent to unblock the state
         * machine.
         */
        public void fakeNextAction() {
            if (mWaitingForIntent != null) {
                logeWithLocalLog("fakeNextAction: " + mWaitingForIntent.getAction(),
                        mInboundSmsTracker.getMessageId());
                handleAction(mWaitingForIntent, false);
            } else {
                logeWithLocalLog("fakeNextAction: mWaitingForIntent is null",
                        mInboundSmsTracker.getMessageId());
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                logeWithLocalLog("onReceive: received null intent, faking " + mWaitingForIntent,
                        mInboundSmsTracker.getMessageId());
                return;
            }
            handleAction(intent, true);
        }

        private synchronized void handleAction(@NonNull Intent intent, boolean onReceive) {
            String action = intent.getAction();
            if (mWaitingForIntent == null || !mWaitingForIntent.getAction().equals(action)) {
                logeWithLocalLog("handleAction: Received " + action + " when expecting "
                        + mWaitingForIntent == null ? "none" : mWaitingForIntent.getAction(),
                        mInboundSmsTracker.getMessageId());
                return;
            }

            if (onReceive) {
                int durationMillis = (int) (System.currentTimeMillis() - mBroadcastTimeMillis);
                if (durationMillis >= 5000) {
                    loge("Slow ordered broadcast completion time for " + action + ": "
                            + durationMillis + " ms");
                } else if (DBG) {
                    log("Ordered broadcast completed for " + action + " in: "
                            + durationMillis + " ms");
                }
            }

            int subId = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            if (action.equals(Intents.SMS_DELIVER_ACTION)) {
                // Now dispatch the notification only intent
                intent.setAction(Intents.SMS_RECEIVED_ACTION);
                // Allow registered broadcast receivers to get this intent even
                // when they are in the background.
                intent.setComponent(null);
                // All running users will be notified of the received sms.
                Bundle options = handleSmsWhitelisting(null, false /* bgActivityStartAllowed */);

                setWaitingForIntent(intent);
                dispatchIntent(intent, android.Manifest.permission.RECEIVE_SMS,
                        AppOpsManager.OPSTR_RECEIVE_SMS,
                        options, this, UserHandle.ALL, subId);
            } else if (action.equals(Intents.WAP_PUSH_DELIVER_ACTION)) {
                // Now dispatch the notification only intent
                intent.setAction(Intents.WAP_PUSH_RECEIVED_ACTION);
                intent.setComponent(null);
                // Only the primary user will receive notification of incoming mms.
                // That app will do the actual downloading of the mms.
                long duration = mPowerWhitelistManager.whitelistAppTemporarilyForEvent(
                        mContext.getPackageName(),
                        PowerWhitelistManager.EVENT_MMS,
                        REASON_EVENT_MMS,
                        "mms-broadcast");
                BroadcastOptions bopts = BroadcastOptions.makeBasic();
                bopts.setTemporaryAppAllowlist(duration,
                        TEMPORARY_ALLOWLIST_TYPE_FOREGROUND_SERVICE_ALLOWED,
                        REASON_EVENT_MMS,
                        "");
                Bundle options = bopts.toBundle();

                String mimeType = intent.getType();

                setWaitingForIntent(intent);
                dispatchIntent(intent, WapPushOverSms.getPermissionForType(mimeType),
                        WapPushOverSms.getAppOpsStringPermissionForIntent(mimeType), options, this,
                        UserHandle.SYSTEM, subId);
            } else {
                // Now that the intents have been deleted we can clean up the PDU data.
                if (!Intents.DATA_SMS_RECEIVED_ACTION.equals(action)
                        && !Intents.SMS_RECEIVED_ACTION.equals(action)
                        && !Intents.WAP_PUSH_RECEIVED_ACTION.equals(action)) {
                    loge("unexpected BroadcastReceiver action: " + action);
                }

                if (onReceive) {
                    int rc = getResultCode();
                    if ((rc != Activity.RESULT_OK) && (rc != Intents.RESULT_SMS_HANDLED)) {
                        loge("a broadcast receiver set the result code to " + rc
                                + ", deleting from raw table anyway!");
                    } else if (DBG) {
                        log("successful broadcast, deleting from raw table.");
                    }
                }

                deleteFromRawTable(mDeleteWhere, mDeleteWhereArgs, MARK_DELETED);
                mWaitingForIntent = null;
                removeMessages(EVENT_RECEIVER_TIMEOUT);
                sendMessage(EVENT_BROADCAST_COMPLETE);
            }
        }
    }

    /**
     * Callback that handles filtering results by carrier services.
     */
    private final class CarrierServicesSmsFilterCallback implements
            CarrierServicesSmsFilter.CarrierServicesSmsFilterCallbackInterface {
        private final byte[][] mPdus;
        private final int mDestPort;
        private final InboundSmsTracker mTracker;
        private final String mSmsFormat;
        private final SmsBroadcastReceiver mSmsBroadcastReceiver;
        private final boolean mUserUnlocked;
        private final boolean mIsClass0;
        private final int mSubId;
        private final long mMessageId;
        private final boolean mBlock;
        private final List<SmsFilter> mRemainingFilters;

        CarrierServicesSmsFilterCallback(byte[][] pdus, int destPort, InboundSmsTracker tracker,
                String smsFormat, SmsBroadcastReceiver smsBroadcastReceiver, boolean userUnlocked,
                boolean isClass0, int subId, long messageId, boolean block,
                List<SmsFilter> remainingFilters) {
            mPdus = pdus;
            mDestPort = destPort;
            mTracker = tracker;
            mSmsFormat = smsFormat;
            mSmsBroadcastReceiver = smsBroadcastReceiver;
            mUserUnlocked = userUnlocked;
            mIsClass0 = isClass0;
            mSubId = subId;
            mMessageId = messageId;
            mBlock = block;
            mRemainingFilters = remainingFilters;
        }

        @Override
        public void onFilterComplete(int result) {
            log("onFilterComplete: result is " + result, mTracker.getMessageId());

            boolean carrierRequestedDrop =
                    (result & CarrierMessagingService.RECEIVE_OPTIONS_DROP) != 0;
            if (carrierRequestedDrop) {
                // Carrier app asked the platform to drop the SMS. Drop it from the database and
                // complete processing.
                dropFilteredSms(mTracker, mSmsBroadcastReceiver, mBlock);
                return;
            }

            boolean filterInvoked = filterSms(mPdus, mDestPort, mTracker, mSmsBroadcastReceiver,
                    mUserUnlocked, mBlock, mRemainingFilters);
            if (filterInvoked) {
                // A remaining filter has assumed responsibility for further message processing.
                return;
            }

            // Now that all filters have been invoked, drop the message if it is blocked.
            if (mBlock) {
                // Only delete the message if the user is unlocked. Otherwise, we should reprocess
                // the message after unlock so the filter has a chance to run while credential-
                // encrypted storage is available.
                if (mUserUnlocked) {
                    log("onFilterComplete: dropping message as the sender is blocked",
                            mTracker.getMessageId());
                    dropFilteredSms(mTracker, mSmsBroadcastReceiver, mBlock);
                } else {
                    // Just complete handling of the message without dropping it.
                    sendMessage(EVENT_BROADCAST_COMPLETE);
                }
                return;
            }

            // Message matched no filters and is not blocked, so complete processing.
            if (mUserUnlocked) {
                dispatchSmsDeliveryIntent(
                        mPdus, mSmsFormat, mDestPort, mSmsBroadcastReceiver, mIsClass0, mSubId,
                        mMessageId);
            } else {
                // Don't do anything further, leave the message in the raw table if the
                // credential-encrypted storage is still locked and show the new message
                // notification if the message is visible to the user.
                if (!isSkipNotifyFlagSet(result)) {
                    showNewMessageNotification();
                }
                sendMessage(EVENT_BROADCAST_COMPLETE);
            }
        }
    }

    private void dropSms(SmsBroadcastReceiver receiver) {
        // Needs phone package permissions.
        deleteFromRawTable(receiver.mDeleteWhere, receiver.mDeleteWhereArgs, MARK_DELETED);
        sendMessage(EVENT_BROADCAST_COMPLETE);
    }

    /** Checks whether the flag to skip new message notification is set in the bitmask returned
     *  from the carrier app.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isSkipNotifyFlagSet(int callbackResult) {
        return (callbackResult
            & RECEIVE_OPTIONS_SKIP_NOTIFY_WHEN_CREDENTIAL_PROTECTED_STORAGE_UNAVAILABLE) > 0;
    }

    /**
     * Log with debug level in logcat and LocalLog
     * @param logMsg msg to log
     */
    protected void logWithLocalLog(String logMsg) {
        log(logMsg);
        mLocalLog.log(logMsg);
    }

    /**
     * Log with debug level in logcat and LocalLog
     * @param logMsg msg to log
     * @param id unique message id
     */
    protected void logWithLocalLog(String logMsg, long id) {
        log(logMsg, id);
        mLocalLog.log(logMsg + ", " + SmsController.formatCrossStackMessageId(id));
    }

    /**
     * Log with error level in logcat and LocalLog
     * @param logMsg msg to log
     */
    protected void logeWithLocalLog(String logMsg) {
        loge(logMsg);
        mLocalLog.log(logMsg);
    }

    /**
     * Log with error level in logcat and LocalLog
     * @param logMsg msg to log
     * @param id unique message id
     */
    protected void logeWithLocalLog(String logMsg, long id) {
        loge(logMsg, id);
        mLocalLog.log(logMsg + ", " + SmsController.formatCrossStackMessageId(id));
    }

    /**
     * Log with debug level.
     * @param s the string to log
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    protected void log(String s) {
        Rlog.d(getName(), s);
    }

    /**
     * Log with debug level.
     * @param s the string to log
     * @param id unique message id
     */
    protected void log(String s, long id) {
        log(s + ", " + SmsController.formatCrossStackMessageId(id));
    }

    /**
     * Log with error level.
     * @param s the string to log
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    protected void loge(String s) {
        Rlog.e(getName(), s);
    }

    /**
     * Log with error level.
     * @param s the string to log
     * @param id unique message id
     */
    protected void loge(String s, long id) {
        loge(s + ", " + SmsController.formatCrossStackMessageId(id));
    }

    /**
     * Log with error level.
     * @param s the string to log
     * @param e is a Throwable which logs additional information.
     */
    @Override
    protected void loge(String s, Throwable e) {
        Rlog.e(getName(), s, e);
    }

    /**
     * Build up the SMS message body from the SmsMessage array of received SMS
     *
     * @param msgs The SmsMessage array of the received SMS
     * @return The text message body
     */
    private static String buildMessageBodyFromPdus(SmsMessage[] msgs) {
        if (msgs.length == 1) {
            // There is only one part, so grab the body directly.
            return replaceFormFeeds(msgs[0].getDisplayMessageBody());
        } else {
            // Build up the body from the parts.
            StringBuilder body = new StringBuilder();
            for (SmsMessage msg: msgs) {
                // getDisplayMessageBody() can NPE if mWrappedMessage inside is null.
                body.append(msg.getDisplayMessageBody());
            }
            return replaceFormFeeds(body.toString());
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(getName() + " extends StateMachine:");
        pw.increaseIndent();
        super.dump(fd, pw, args);
        if (mCellBroadcastServiceManager != null) {
            mCellBroadcastServiceManager.dump(fd, pw, args);
        }
        pw.println("mLocalLog:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.println("mCarrierServiceLocalLog:");
        pw.increaseIndent();
        mCarrierServiceLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
    }

    // Some providers send formfeeds in their messages. Convert those formfeeds to newlines.
    private static String replaceFormFeeds(String s) {
        return s == null ? "" : s.replace('\f', '\n');
    }

    @VisibleForTesting
    public PowerManager.WakeLock getWakeLock() {
        return mWakeLock;
    }

    @VisibleForTesting
    public int getWakeLockTimeout() {
        return mWakeLockTimeout;
    }

    /**
    * Sets the wakelock timeout to {@link timeOut} milliseconds
    */
    private void setWakeLockTimeout(int timeOut) {
        mWakeLockTimeout = timeOut;
    }

    /**
     * Set the SMS filters used by {@link #filterSms} for testing purposes.
     *
     * @param smsFilters List of SMS filters, or null to restore the default filters.
     */
    @VisibleForTesting
    public void setSmsFiltersForTesting(@Nullable List<SmsFilter> smsFilters) {
        if (smsFilters == null) {
            mSmsFilters = createDefaultSmsFilters();
        } else {
            mSmsFilters = smsFilters;
        }
    }

    /**
     * Handler for the broadcast sent when the new message notification is clicked. It launches the
     * default SMS app.
     */
    private static class NewMessageNotificationActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_OPEN_SMS_APP.equals(intent.getAction())) {
                // do nothing if the user had not unlocked the device yet
                UserManager userManager =
                        (UserManager) context.getSystemService(Context.USER_SERVICE);
                if (userManager.isUserUnlocked()) {
                    context.startActivity(context.getPackageManager().getLaunchIntentForPackage(
                            Telephony.Sms.getDefaultSmsPackage(context)));
                }
            }
        }
    }

    protected byte[] decodeHexString(String hexString) {
        if (hexString == null || hexString.length() % 2 == 1) {
            return null;
        }
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

    private byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    private int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if (digit == -1) {
            return 0;
        }
        return digit;
    }


    /**
     * Registers the broadcast receiver to launch the default SMS app when the user clicks the
     * new message notification.
     */
    static void registerNewMessageNotificationActionHandler(Context context) {
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction(ACTION_OPEN_SMS_APP);
        context.registerReceiver(new NewMessageNotificationActionReceiver(), userFilter,
                Context.RECEIVER_NOT_EXPORTED);
    }

    protected abstract class CbTestBroadcastReceiver extends BroadcastReceiver {

        protected abstract void handleTestAction(Intent intent);

        protected final String mTestAction;

        public CbTestBroadcastReceiver(String testAction) {
            mTestAction = testAction;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            logd("Received test intent action=" + intent.getAction());
            if (intent.getAction().equals(mTestAction)) {
                // Return early if phone_id is explicilty included and does not match mPhone.
                // If phone_id extra is not included, continue.
                int phoneId = mPhone.getPhoneId();
                if (intent.getIntExtra("phone_id", phoneId) != phoneId) {
                    return;
                }
                handleTestAction(intent);
            }
        }
    }

    /** A filter for incoming messages allowing the normal processing flow to be skipped. */
    @VisibleForTesting
    public interface SmsFilter {
        /**
         * Returns true if a filter is invoked and the SMS processing flow should be diverted, false
         * otherwise.
         *
         * <p>If the filter can immediately determine that the message matches, it must call
         * {@link #dropFilteredSms} to drop the message from the database once it has been
         * processed.
         *
         * <p>If the filter must perform some asynchronous work to determine if the message matches,
         * it should return true to defer processing. Once it has made a determination, if it finds
         * the message matches, it must call {@link #dropFilteredSms}. If the message does not
         * match, it must be passed through {@code remainingFilters} and either dropped if the
         * remaining filters all return false or if {@code block} is true, or else it must be
         * broadcast.
         */
        boolean filterSms(byte[][] pdus, int destPort, InboundSmsTracker tracker,
                SmsBroadcastReceiver resultReceiver, boolean userUnlocked, boolean block,
                List<SmsFilter> remainingFilters);
    }
}

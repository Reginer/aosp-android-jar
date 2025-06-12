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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Intents;
import android.telephony.Annotation.DisconnectCauses;
import android.telephony.DomainSelectionService;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.satellite.SatelliteManager;
import android.text.TextUtils;

import com.android.ims.ImsManager;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.domainselection.DomainSelectionConnection;
import com.android.internal.telephony.domainselection.DomainSelectionResolver;
import com.android.internal.telephony.domainselection.EmergencySmsDomainSelectionConnection;
import com.android.internal.telephony.domainselection.SmsDomainSelectionConnection;
import com.android.internal.telephony.emergency.EmergencyStateTracker;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.android.internal.telephony.gsm.GsmSMSDispatcher;
import com.android.internal.telephony.satellite.DatagramDispatcher;
import com.android.internal.telephony.satellite.SatelliteController;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class SmsDispatchersController extends Handler {
    private static final String TAG = "SmsDispatchersController";
    private static final boolean VDBG = false; // STOPSHIP if true
    private static final boolean ENABLE_CDMA_DISPATCHER = true; // see b/388540508

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

    /** Called when SMS should be sent using AP domain selection. */
    private static final int EVENT_SEND_SMS_USING_DOMAIN_SELECTION = 18;

    /** Called when SMS is completely sent using AP domain selection regardless of the result. */
    private static final int EVENT_SMS_SENT_COMPLETED_USING_DOMAIN_SELECTION = 19;

    /** Called when AP domain selection is abnormally terminated. */
    private static final int EVENT_DOMAIN_SELECTION_TERMINATED_ABNORMALLY = 20;

    /** Called when MT SMS is received via IMS. */
    private static final int EVENT_SMS_RECEIVED_VIA_IMS = 21;

    /** Called when the domain selection should be performed. */
    private static final int EVENT_REQUEST_DOMAIN_SELECTION = 22;

    /** Called when {@link DatagramDispatcher} informs to send carrier roaming nb iot ntn sms. */
    private static final int CMD_SEND_TEXT = 23;

    /** Called when {@link DatagramDispatcher} informs sms cannot be sent over ntn due to error. */
    private static final int EVENT_SEND_TEXT_OVER_NTN_ERROR = 24;

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

    private SMSDispatcher mCdmaDispatcher = null;
    private SMSDispatcher mGsmDispatcher;
    private ImsSmsDispatcher mImsSmsDispatcher;

    private GsmInboundSmsHandler mGsmInboundSmsHandler;
    private CdmaInboundSmsHandler mCdmaInboundSmsHandler = null;

    private Phone mPhone;
    /** Outgoing message counter. Shared by all dispatchers. */
    private final SmsUsageMonitor mUsageMonitor;
    private final CommandsInterface mCi;
    private final Context mContext;
    private final @NonNull FeatureFlags mFeatureFlags;

    /** true if IMS is registered and sms is supported, false otherwise.*/
    private boolean mIms = false;
    private String mImsSmsFormat = SmsConstants.FORMAT_UNKNOWN;

    /** 3GPP format sent messages awaiting a delivery status report. */
    private HashMap<Integer, SMSDispatcher.SmsTracker> mDeliveryPendingMapFor3GPP = new HashMap<>();

    /** 3GPP2 format sent messages awaiting a delivery status report. */
    private HashMap<Integer, SMSDispatcher.SmsTracker> mDeliveryPendingMapFor3GPP2 =
            new HashMap<>();

    /**
     * Testing interface for injecting mock DomainSelectionConnection and a flag to indicate
     * whether the domain selection is supported.
     */
    @VisibleForTesting
    public interface DomainSelectionResolverProxy {
        /**
         * Returns a {@link DomainSelectionConnection} created using the specified
         * context and callback.
         *
         * @param phone The {@link Phone} instance.
         * @param selectorType The domain selector type to identify the domain selection connection.
         *                     A {@link DomainSelectionService#SELECTOR_TYPE_SMS} is used for SMS.
         * @param isEmergency A flag to indicate whether this connection is
         *                    for an emergency SMS or not.
         */
        @Nullable DomainSelectionConnection getDomainSelectionConnection(Phone phone,
                @DomainSelectionService.SelectorType int selectorType, boolean isEmergency);

        /**
         * Checks if the device supports the domain selection service to route the call / SMS /
         * supplementary services to the appropriate domain.
         *
         * @return {@code true} if the domain selection is supported on the device,
         *         {@code false} otherwise.
         */
        boolean isDomainSelectionSupported();
    }

    private DomainSelectionResolverProxy mDomainSelectionResolverProxy =
            new DomainSelectionResolverProxy() {
                @Override
                @Nullable
                public DomainSelectionConnection getDomainSelectionConnection(Phone phone,
                        @DomainSelectionService.SelectorType int selectorType,
                        boolean isEmergency) {
                    try {
                        return DomainSelectionResolver.getInstance().getDomainSelectionConnection(
                                phone, selectorType, isEmergency);
                    } catch (IllegalStateException e) {
                        // In general, DomainSelectionResolver is always initialized by TeleService,
                        // but if it's not initialized (like in unit tests),
                        // it returns null to perform the legacy behavior in this case.
                        return null;
                    }
                }

                @Override
                public boolean isDomainSelectionSupported() {
                    return DomainSelectionResolver.getInstance().isDomainSelectionSupported();
                }
            };

    /** Stores the sending SMS information for a pending request. */
    public static class PendingRequest {
        public static final int TYPE_DATA = 1;
        public static final int TYPE_TEXT = 2;
        public static final int TYPE_MULTIPART_TEXT = 3;
        public static final int TYPE_RETRY_SMS = 4;
        private static final AtomicLong sNextUniqueMessageId = new AtomicLong(0);

        public final int type;
        public final SMSDispatcher.SmsTracker tracker;
        public final String callingPackage;
        public final int callingUser;
        public final String destAddr;
        public final String scAddr;
        public final ArrayList<PendingIntent> sentIntents;
        public final ArrayList<PendingIntent> deliveryIntents;
        public final boolean isForVvm;
        // sendData specific
        public final byte[] data;
        public final int destPort;
        // sendText / sendMultipartText specific
        public final ArrayList<String> texts;
        public final Uri messageUri;
        public final boolean persistMessage;
        public final int priority;
        public final boolean expectMore;
        public final int validityPeriod;
        public final long messageId;
        public final boolean skipShortCodeCheck;
        public final long uniqueMessageId;
        public final boolean isMtSmsPolling;

        public PendingRequest(int type, SMSDispatcher.SmsTracker tracker, String callingPackage,
                int callingUser, String destAddr, String scAddr,
                ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents,
                boolean isForVvm, byte[] data, int destPort, ArrayList<String> texts,
                Uri messageUri, boolean persistMessage, int priority, boolean expectMore,
                int validityPeriod, long messageId, boolean skipShortCodeCheck,
                boolean isMtSmsPolling) {
            this.type = type;
            this.tracker = tracker;
            this.callingPackage = callingPackage;
            this.callingUser = callingUser;
            this.destAddr = destAddr;
            this.scAddr = scAddr;
            this.sentIntents = sentIntents;
            this.deliveryIntents = deliveryIntents;
            this.isForVvm = isForVvm;

            this.data = data;
            this.destPort = destPort;

            this.texts = texts;
            this.messageUri = messageUri;
            this.persistMessage = persistMessage;
            this.priority = priority;
            this.expectMore = expectMore;
            this.validityPeriod = validityPeriod;
            this.messageId = messageId;
            this.skipShortCodeCheck = skipShortCodeCheck;
            if (tracker != null) {
                this.uniqueMessageId = tracker.mUniqueMessageId;
            } else {
                this.uniqueMessageId = getNextUniqueMessageId();
            }
            this.isMtSmsPolling = isMtSmsPolling;
        }

        public static long getNextUniqueMessageId() {
            return sNextUniqueMessageId.getAndUpdate(
                id -> ((id + 1) % Long.MAX_VALUE));
        }
    }

    /**
     * Manages the {@link DomainSelectionConnection} instance and its related information.
     */
    @VisibleForTesting
    protected class DomainSelectionConnectionHolder
            implements DomainSelectionConnection.DomainSelectionConnectionCallback {
        private final boolean mEmergency;
        // Manages the pending request while selecting a proper domain.
        private final List<PendingRequest> mPendingRequests = new ArrayList<>();
        // Manages the domain selection connections: MO SMS or emergency SMS.
        private DomainSelectionConnection mConnection;

        DomainSelectionConnectionHolder(boolean emergency) {
            mEmergency = emergency;
        }

        /**
         * Returns a {@link DomainSelectionConnection} instance.
         */
        public DomainSelectionConnection getConnection() {
            return mConnection;
        }

        /**
         * Returns a list of {@link PendingRequest} that was added
         * while the domain selection is performed.
         */
        public List<PendingRequest> getPendingRequests() {
            return mPendingRequests;
        }

        /**
         * Checks whether or not the domain selection is requested.
         * If there is no pending request, the domain selection request is needed to
         * select a proper domain for MO SMS.
         */
        public boolean isDomainSelectionRequested() {
            return !mPendingRequests.isEmpty();
        }

        /**
         * Checks whether or not this holder is for an emergency SMS.
         */
        public boolean isEmergency() {
            return mEmergency;
        }

        /**
         * Clears all pending requests.
         */
        public void clearAllRequests() {
            mPendingRequests.clear();
        }

        /**
         * Add a new pending request.
         */
        public void addRequest(@NonNull PendingRequest request) {
            mPendingRequests.add(request);
        }

        /**
         * Sets a {@link DomainSelectionConnection} instance.
         */
        public void setConnection(DomainSelectionConnection connection) {
            mConnection = connection;
        }


        @Override
        public void onSelectionTerminated(@DisconnectCauses int cause) {
            logd("onSelectionTerminated: emergency=" + mEmergency + ", cause=" + cause);
            // This callback is invoked by another thread, so this operation is posted and handled
            // through the execution flow of SmsDispatchersController.
            SmsDispatchersController.this.sendMessage(
                    obtainMessage(EVENT_DOMAIN_SELECTION_TERMINATED_ABNORMALLY, this));
        }
    }

    /** Manages the domain selection connections: MO SMS or emergency SMS. */
    private DomainSelectionConnectionHolder mDscHolder;
    private DomainSelectionConnectionHolder mEmergencyDscHolder;
    private EmergencyStateTracker mEmergencyStateTracker;

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
            SmsUsageMonitor usageMonitor, @NonNull FeatureFlags featureFlags) {
        this(phone, storageMonitor, usageMonitor, phone.getLooper(), featureFlags);
    }

    @VisibleForTesting
    public SmsDispatchersController(Phone phone, SmsStorageMonitor storageMonitor,
            SmsUsageMonitor usageMonitor, Looper looper, @NonNull FeatureFlags featureFlags) {
        super(looper);

        Rlog.d(TAG, "SmsDispatchersController created");

        mContext = phone.getContext();
        mUsageMonitor = usageMonitor;
        mCi = phone.mCi;
        mFeatureFlags = featureFlags;
        mPhone = phone;

        // Create dispatchers, inbound SMS handlers and
        // broadcast undelivered messages in raw table.
        mImsSmsDispatcher = new ImsSmsDispatcher(phone, this, ImsManager::getConnector);
        mGsmInboundSmsHandler = GsmInboundSmsHandler.makeInboundSmsHandler(phone.getContext(),
                storageMonitor, phone, looper, mFeatureFlags);
        if (ENABLE_CDMA_DISPATCHER) {
            mCdmaDispatcher = new CdmaSMSDispatcher(phone, this);
            mCdmaInboundSmsHandler = CdmaInboundSmsHandler.makeInboundSmsHandler(phone.getContext(),
                    storageMonitor, phone, (CdmaSMSDispatcher) mCdmaDispatcher, looper,
                    mFeatureFlags);
        }
        mGsmDispatcher = new GsmSMSDispatcher(phone, this, mGsmInboundSmsHandler);
        SmsBroadcastUndelivered.initialize(phone.getContext(),
                mGsmInboundSmsHandler, mCdmaInboundSmsHandler, mFeatureFlags);
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
        if (mCdmaDispatcher != null) mCdmaDispatcher.dispose();
        mGsmInboundSmsHandler.dispose();
        if (mCdmaInboundSmsHandler != null) mCdmaInboundSmsHandler.dispose();
        // Cancels the domain selection request if it's still in progress.
        finishDomainSelection(mDscHolder);
        finishDomainSelection(mEmergencyDscHolder);
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
            case EVENT_SEND_SMS_USING_DOMAIN_SELECTION: {
                SomeArgs args = (SomeArgs) msg.obj;
                DomainSelectionConnectionHolder holder =
                        (DomainSelectionConnectionHolder) args.arg1;
                PendingRequest request = (PendingRequest) args.arg2;
                String logTag = (String) args.arg3;
                try {
                    handleSendSmsUsingDomainSelection(holder, request, logTag);
                } finally {
                    args.recycle();
                }
                break;
            }
            case EVENT_SMS_SENT_COMPLETED_USING_DOMAIN_SELECTION: {
                SomeArgs args = (SomeArgs) msg.obj;
                String destAddr = (String) args.arg1;
                Long messageId = (Long) args.arg2;
                Boolean success = (Boolean) args.arg3;
                Boolean isOverIms = (Boolean) args.arg4;
                Boolean isLastSmsPart = (Boolean) args.arg5;
                try {
                    handleSmsSentCompletedUsingDomainSelection(
                            destAddr, messageId, success, isOverIms, isLastSmsPart);
                } finally {
                    args.recycle();
                }
                break;
            }
            case EVENT_DOMAIN_SELECTION_TERMINATED_ABNORMALLY: {
                handleDomainSelectionTerminatedAbnormally(
                        (DomainSelectionConnectionHolder) msg.obj);
                break;
            }
            case EVENT_SMS_RECEIVED_VIA_IMS: {
                handleSmsReceivedViaIms((String) msg.obj);
                break;
            }
            case EVENT_REQUEST_DOMAIN_SELECTION: {
                SomeArgs args = (SomeArgs) msg.obj;
                DomainSelectionConnectionHolder holder =
                        (DomainSelectionConnectionHolder) args.arg1;
                PendingRequest request = (PendingRequest) args.arg2;
                String logTag = (String) args.arg3;
                try {
                    requestDomainSelection(holder, request, logTag);
                } finally {
                    args.recycle();
                }
                break;
            }
            case CMD_SEND_TEXT: {
                PendingRequest request = (PendingRequest) msg.obj;
                if (request.type == PendingRequest.TYPE_TEXT) {
                    sendTextInternal(request);
                } else if (request.type == PendingRequest.TYPE_MULTIPART_TEXT) {
                    sendMultipartTextInternal(request);
                } else {
                    logd("CMD_SEND_TEXT: type=" + request.type
                            + " messageId=" + request.messageId);
                }
                break;
            }
            case EVENT_SEND_TEXT_OVER_NTN_ERROR: {
                PendingRequest request = (PendingRequest) msg.obj;
                logd("EVENT_SEND_TEXT_OVER_NTN_ERROR: type=" + request.type
                        + " messageId=" + request.messageId);
                triggerSentIntentForFailure(request.sentIntents);
                break;
            }

            default:
                if (isCdmaMo()) {
                    if (mCdmaDispatcher != null) mCdmaDispatcher.handleMessage(msg);
                } else {
                    mGsmDispatcher.handleMessage(msg);
                }
        }
    }

    private String getSmscAddressFromUSIMWithPhoneIdentity(String callingPkg) {
        final long identity = Binder.clearCallingIdentity();
        try {
            IccSmsInterfaceManager iccSmsIntMgr = mPhone.getIccSmsInterfaceManager();
            if (iccSmsIntMgr != null) {
                return iccSmsIntMgr.getSmscAddressFromIccEf(callingPkg);
            } else {
                Rlog.d(TAG, "getSmscAddressFromIccEf iccSmsIntMgr is null");
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
        return null;
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
                || (mCdmaInboundSmsHandler != null
                && mCdmaInboundSmsHandler.getCurrentState().getName().equals("WaitingState"))) {
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
        injectSmsPdu(msg, format, callback, false /* ignoreClass */, isOverIms, 0 /* unused */);
    }

    @VisibleForTesting
    public void setImsSmsDispatcher(ImsSmsDispatcher imsSmsDispatcher) {
        mImsSmsDispatcher = imsSmsDispatcher;
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
            boolean ignoreClass, boolean isOverIms, int token) {
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
                        InboundSmsHandler.EVENT_INJECT_SMS, isOverIms ? 1 : 0, token, ar);
            } else if (format.equals(SmsConstants.FORMAT_3GPP2) && mCdmaInboundSmsHandler != null) {
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
     * sets ImsManager object.
     *
     * @param imsManager holds a valid object or a null for setting
     */
    public boolean setImsManager(ImsManager imsManager) {
        if (mGsmInboundSmsHandler != null) {
            mGsmInboundSmsHandler.setImsManager(imsManager);
            return true;
        }
        return false;
    }

    /**
     * Retry the message along to the radio.
     *
     * @param tracker holds the SMS message to send
     */
    public void sendRetrySms(SMSDispatcher.SmsTracker tracker) {
        boolean retryUsingImsService = false;

        if (!tracker.mUsesImsServiceForIms) {
            if (isSmsDomainSelectionEnabled()) {
                boolean isEmergency = isEmergencyNumber(tracker.mDestAddress);
                // This may be invoked by another thread, so this operation is posted and
                // handled through the execution flow of SmsDispatchersController.
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = getDomainSelectionConnectionHolder(isEmergency);
                args.arg2 = new PendingRequest(PendingRequest.TYPE_RETRY_SMS, tracker,
                        null, UserHandle.USER_NULL, null, null,
                        null, null, false, null, 0,
                        null, null, false,
                        0, false, 0, 0L, false, false);
                args.arg3 = "sendRetrySms";
                sendMessage(obtainMessage(EVENT_REQUEST_DOMAIN_SELECTION, args));
                return;
            }

            if (mImsSmsDispatcher.isAvailable()) {
                // If this tracker has not been handled by ImsSmsDispatcher yet and IMS Service is
                // available now, retry this failed tracker using IMS Service.
                retryUsingImsService = true;
            }
        }

        sendRetrySms(tracker, retryUsingImsService);
    }

    /**
     * Retry the message along to the radio.
     *
     * @param tracker holds the SMS message to send
     * @param retryUsingImsService a flag to indicate whether the retry SMS can use the ImsService
     */
    public void sendRetrySms(SMSDispatcher.SmsTracker tracker, boolean retryUsingImsService) {
        String oldFormat = tracker.mFormat;
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
                notifySmsSent(tracker, !retryUsingImsService,
                        true /*isLastSmsPart*/, false /*success*/);
                return;
            }
            String scAddr = (String) map.get("scAddr");
            String destAddr = (String) map.get("destAddr");
            if (destAddr == null) {
                Rlog.e(TAG, "sendRetrySms failed due to null destAddr");
                tracker.onFailed(mContext, SmsManager.RESULT_SMS_SEND_RETRY_FAILED, NO_ERROR_CODE);
                notifySmsSent(tracker, !retryUsingImsService,
                        true /*isLastSmsPart*/, false /*success*/);
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
                            scAddr, destAddr, text, (tracker.mDeliveryIntent != null), null,
                            0, 0, 0, -1, tracker.mMessageRef);
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
                            (tracker.mDeliveryIntent != null), tracker.mMessageRef);
                }
            }

            if (pdu == null) {
                Rlog.e(TAG, String.format("sendRetrySms failed to encode message."
                        + "scAddr: %s, "
                        + "destPort: %s", scAddr, map.get("destPort")));
                tracker.onFailed(mContext, SmsManager.RESULT_SMS_SEND_RETRY_FAILED, NO_ERROR_CODE);
                notifySmsSent(tracker, !retryUsingImsService,
                    true /*isLastSmsPart*/, false /*success*/);
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
     * Memory Available Event
     * @param result callback message
     */
    public void reportSmsMemoryStatus(Message result) {
        Rlog.d(TAG, "reportSmsMemoryStatus: ");
        try {
            mImsSmsDispatcher.onMemoryAvailable();
            AsyncResult.forMessage(result, null, null);
            result.sendToTarget();
        } catch (Exception e) {
            Rlog.e(TAG, "reportSmsMemoryStatus Failed ", e);
            AsyncResult.forMessage(result, null, e);
            result.sendToTarget();
        }
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
        if (!ENABLE_CDMA_DISPATCHER) return false;
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
        if (!ENABLE_CDMA_DISPATCHER) return false;
        return (mCdmaDispatcher.getFormat().equals(format));
    }

    /** Sets a proxy interface for accessing the methods of {@link DomainSelectionResolver}. */
    @VisibleForTesting
    public void setDomainSelectionResolverProxy(@NonNull DomainSelectionResolverProxy proxy) {
        mDomainSelectionResolverProxy = proxy;
    }

    /**
     * Checks whether the SMS domain selection is enabled or not.
     *
     * @return {@code true} if the SMS domain selection is enabled, {@code false} otherwise.
     */
    private boolean isSmsDomainSelectionEnabled() {
        return mFeatureFlags.smsDomainSelectionEnabled()
                && mDomainSelectionResolverProxy.isDomainSelectionSupported();
    }

    /**
     * Determines whether or not to use CDMA format for MO SMS when the domain selection uses.
     * If the domain is {@link NetworkRegistrationInfo#DOMAIN_PS}, then format is based on
     * IMS SMS format, otherwise format is based on current phone type.
     *
     * @return {@code true} if CDMA format should be used for MO SMS, {@code false} otherwise.
     */
    private boolean isCdmaMo(@NetworkRegistrationInfo.Domain int domain) {
        if (domain != NetworkRegistrationInfo.DOMAIN_PS) {
            // IMS is not registered, use voice technology to determine SMS format.
            return (PhoneConstants.PHONE_TYPE_CDMA == mPhone.getPhoneType());
        }
        // IMS is registered with SMS support
        return isCdmaFormat(mImsSmsDispatcher.getFormat());
    }

    /**
     * Returns a {@link DomainSelectionConnectionHolder} according to the flag specified.
     *
     * @param emergency The flag to indicate that the domain selection is for an emergency SMS.
     * @return A {@link DomainSelectionConnectionHolder} instance.
     */
    @VisibleForTesting
    @Nullable
    protected DomainSelectionConnectionHolder getDomainSelectionConnectionHolder(
            boolean emergency) {
        if (emergency) {
            if (mEmergencyDscHolder == null) {
                mEmergencyDscHolder = new DomainSelectionConnectionHolder(emergency);
            }
            return mEmergencyDscHolder;
        } else {
            if (mDscHolder == null) {
                mDscHolder = new DomainSelectionConnectionHolder(emergency);
            }
            return mDscHolder;
        }
    }

    /**
     * Requests the domain selection for MO SMS.
     *
     * @param holder The {@link DomainSelectionConnectionHolder} that contains the
     *               {@link DomainSelectionConnection} and its related information.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    private void requestDomainSelection(@NonNull DomainSelectionConnectionHolder holder) {
        DomainSelectionService.SelectionAttributes attr =
                new DomainSelectionService.SelectionAttributes.Builder(mPhone.getPhoneId(),
                        mPhone.getSubId(), DomainSelectionService.SELECTOR_TYPE_SMS)
                .setEmergency(holder.isEmergency())
                .build();

        if (holder.isEmergency()) {
            EmergencySmsDomainSelectionConnection emergencyConnection =
                    (EmergencySmsDomainSelectionConnection) holder.getConnection();
            CompletableFuture<Integer> future =
                    emergencyConnection.requestDomainSelection(attr, holder);
            future.thenAcceptAsync((domain) -> {
                if (VDBG) {
                    logd("requestDomainSelection(emergency): domain="
                            + DomainSelectionService.getDomainName(domain));
                }
                sendAllPendingRequests(holder, domain);
                finishDomainSelection(holder);
            }, this::post);
        } else {
            SmsDomainSelectionConnection connection =
                    (SmsDomainSelectionConnection) holder.getConnection();
            CompletableFuture<Integer> future = connection.requestDomainSelection(attr, holder);
            future.thenAcceptAsync((domain) -> {
                if (VDBG) {
                    logd("requestDomainSelection: domain="
                            + DomainSelectionService.getDomainName(domain));
                }
                sendAllPendingRequests(holder, domain);
                finishDomainSelection(holder);
            }, this::post);
        }
    }

    /**
     * Requests the domain selection for MO SMS.
     *
     * @param holder The {@link DomainSelectionConnectionHolder} that contains the
     *               {@link DomainSelectionConnection} and its related information.
     * @param request The {@link PendingRequest} that stores the SMS request
     *                (data, text, multipart text) to be sent.
     * @param logTag The log string.
     */
    private void requestDomainSelection(@NonNull DomainSelectionConnectionHolder holder,
            @NonNull PendingRequest request, String logTag) {
        boolean isDomainSelectionRequested = holder.isDomainSelectionRequested();
        // The domain selection is in progress so waits for the result of
        // the domain selection by adding this request to the pending list.
        holder.addRequest(request);

        if (holder.getConnection() == null) {
            DomainSelectionConnection connection =
                    mDomainSelectionResolverProxy.getDomainSelectionConnection(
                            mPhone, DomainSelectionService.SELECTOR_TYPE_SMS, holder.isEmergency());
            if (connection == null) {
                logd("requestDomainSelection: fallback for " + logTag);
                // If the domain selection connection is not available,
                // fallback to the legacy implementation.
                sendAllPendingRequests(holder, NetworkRegistrationInfo.DOMAIN_UNKNOWN);
                return;
            } else {
                holder.setConnection(connection);
            }
        }

        if (!isDomainSelectionRequested) {
            if (VDBG) {
                logd("requestDomainSelection: " + logTag);
            }
            requestDomainSelection(holder);
        }
    }

    /**
     * Handles an event for sending a SMS after selecting the domain via the domain selection
     * service.
     *
     * @param holder The {@link DomainSelectionConnectionHolder} that contains the
     *               {@link DomainSelectionConnection} and its related information.
     * @param request The {@link PendingRequest} that stores the SMS request
     *                (data, text, multipart text) to be sent.
     * @param logTag The log tag to display which method called this method.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    private void handleSendSmsUsingDomainSelection(@NonNull DomainSelectionConnectionHolder holder,
            @NonNull PendingRequest request, @NonNull String logTag) {
        if (holder.isEmergency()) {
            if (mEmergencyStateTracker == null) {
                mEmergencyStateTracker = EmergencyStateTracker.getInstance();
            }

            CompletableFuture<Integer> future = mEmergencyStateTracker.startEmergencySms(mPhone,
                    String.valueOf(request.messageId),
                    isTestEmergencyNumber(request.destAddr));
            future.thenAccept((result) -> {
                logi("startEmergencySms(" + logTag + "): messageId=" + request.messageId
                        + ", result=" + result);
                // An emergency SMS should be proceeded regardless of the result of the
                // EmergencyStateTracker.
                // So the domain selection request should be invoked without checking the result.
                requestDomainSelection(holder, request, logTag);
            });
        } else {
            requestDomainSelection(holder, request, logTag);
        }
    }

    /**
     * Sends a SMS after selecting the domain via the domain selection service.
     *
     * @param holder The {@link DomainSelectionConnectionHolder} that contains the
     *               {@link DomainSelectionConnection} and its related information.
     * @param request The {@link PendingRequest} that stores the SMS request
     *                (data, text, multipart text) to be sent.
     * @param logTag The log tag to display which method called this method.
     */
    private void sendSmsUsingDomainSelection(@NonNull DomainSelectionConnectionHolder holder,
            @NonNull PendingRequest request, @NonNull String logTag) {
        // Run on main thread for interworking with EmergencyStateTracker
        // and adding the pending request.
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = holder;
        args.arg2 = request;
        args.arg3 = logTag;
        sendMessage(obtainMessage(EVENT_SEND_SMS_USING_DOMAIN_SELECTION, args));
    }

    /**
     * Called when sending MO SMS is complete regardless of the sent result.
     *
     * @param destAddr The destination address for SMS.
     * @param messageId The message id for SMS.
     * @param success A flag specifying whether MO SMS is successfully sent or not.
     * @param isOverIms A flag specifying whether MO SMS is sent over IMS or not.
     * @param isLastSmsPart A flag specifying whether this result is for the last SMS part or not.
     */
    private void handleSmsSentCompletedUsingDomainSelection(@NonNull String destAddr,
            long messageId, boolean success, boolean isOverIms, boolean isLastSmsPart) {
        if (mEmergencyStateTracker != null) {
            if (isEmergencyNumber(destAddr)) {
                mEmergencyStateTracker.endSms(String.valueOf(messageId), success,
                        isOverIms ? NetworkRegistrationInfo.DOMAIN_PS
                                  : NetworkRegistrationInfo.DOMAIN_CS,
                        isLastSmsPart);
            }
        }
    }

    /**
     * Called when MO SMS is sent.
     */
    protected void notifySmsSent(@NonNull SMSDispatcher.SmsTracker tracker,
            boolean isOverIms, boolean isLastSmsPart, boolean success) {
        notifySmsSentToEmergencyStateTracker(tracker.mDestAddress,
            tracker.mMessageId, isOverIms, isLastSmsPart, success);
        notifySmsSentToDatagramDispatcher(tracker.mUniqueMessageId,
                tracker.isSinglePartOrLastPart(), success && !tracker.isAnyPartFailed());
    }

    /**
     * Called when MO SMS is sent.
     */
    private void notifySmsSentToEmergencyStateTracker(@NonNull String destAddr, long messageId,
            boolean isOverIms, boolean isLastSmsPart, boolean success) {
        if (isSmsDomainSelectionEnabled()) {
            // Run on main thread for interworking with EmergencyStateTracker.
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = destAddr;
            args.arg2 = Long.valueOf(messageId);
            args.arg3 = Boolean.valueOf(success);
            args.arg4 = Boolean.valueOf(isOverIms);
            args.arg5 = Boolean.valueOf(isLastSmsPart);
            sendMessage(obtainMessage(EVENT_SMS_SENT_COMPLETED_USING_DOMAIN_SELECTION, args));
        }
    }

    private void notifySmsSentToDatagramDispatcher(
            long messageId, boolean isLastSmsPart, boolean success) {
        if (SatelliteController.getInstance().shouldSendSmsToDatagramDispatcher(mPhone)
                && isLastSmsPart) {
            DatagramDispatcher.getInstance().onSendSmsDone(
                    mPhone.getSubId(), messageId, success);
        }
    }

    /**
     * Called when MT SMS is received via IMS.
     *
     * @param origAddr The originating address of MT SMS.
     */
    private void handleSmsReceivedViaIms(@Nullable String origAddr) {
        if (mEmergencyStateTracker != null) {
            if (origAddr != null && isEmergencyNumber(origAddr)) {
                mEmergencyStateTracker.onEmergencySmsReceived();
            }
        }
    }

    /**
     * Called when MT SMS is received via IMS.
     */
    protected void notifySmsReceivedViaImsToEmergencyStateTracker(@Nullable String origAddr) {
        if (isSmsDomainSelectionEnabled()) {
            // Run on main thread for interworking with EmergencyStateTracker.
            sendMessage(obtainMessage(EVENT_SMS_RECEIVED_VIA_IMS, origAddr));
        }
    }

    private boolean isTestEmergencyNumber(String number) {
        try {
            if (!mPhone.hasCalling()) return false;
            TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
            if (tm == null) return false;
            Map<Integer, List<EmergencyNumber>> eMap = tm.getEmergencyNumberList();
            return eMap.values().stream().flatMap(Collection::stream).anyMatch(eNumber ->
                    eNumber.isFromSources(EmergencyNumber.EMERGENCY_NUMBER_SOURCE_TEST)
                    && number.equals(eNumber.getNumber()));
        } catch (IllegalStateException ise) {
            return false;
        } catch (RuntimeException r) {
            return false;
        }
    }

    /**
     * Finishes the domain selection for MO SMS.
     *
     * @param holder The {@link DomainSelectionConnectionHolder} object that is being finished.
     */
    private void finishDomainSelection(DomainSelectionConnectionHolder holder) {
        DomainSelectionConnection connection = (holder != null) ? holder.getConnection() : null;

        if (connection != null) {
            // After this method is called, the domain selection service will clean up
            // its resources and finish the procedure that are related to the current domain
            // selection request.
            connection.finishSelection();
        }

        if (holder != null) {
            final List<PendingRequest> pendingRequests = holder.getPendingRequests();

            logd("finishDomainSelection: pendingRequests=" + pendingRequests.size());

            for (PendingRequest r : pendingRequests) {
                triggerSentIntentForFailure(r.sentIntents);
            }

            holder.clearAllRequests();
            holder.setConnection(null);
        }
    }

    /**
     * Called when MO SMS is not sent by the error of domain selection.
     *
     * @param holder The {@link DomainSelectionConnectionHolder} object that is being terminated.
     */
    private void handleDomainSelectionTerminatedAbnormally(
            @NonNull DomainSelectionConnectionHolder holder) {
        logd("handleDomainSelectionTerminatedAbnormally: pendingRequests="
                + holder.getPendingRequests().size());
        sendAllPendingRequests(holder, NetworkRegistrationInfo.DOMAIN_UNKNOWN);
        holder.setConnection(null);
    }

    /**
     * Sends all pending requests for MO SMS.
     *
     * @param holder The {@link DomainSelectionConnectionHolder} object that all the pending
     *               requests are handled.
     * @param domain The domain where the SMS is being sent, which can be one of the following:
     *               - {@link NetworkRegistrationInfo#DOMAIN_PS}
     *               - {@link NetworkRegistrationInfo#DOMAIN_CS}
     */
    private void sendAllPendingRequests(@NonNull DomainSelectionConnectionHolder holder,
            @NetworkRegistrationInfo.Domain int domain) {
        final List<PendingRequest> pendingRequests = holder.getPendingRequests();

        if (VDBG) {
            logd("sendAllPendingRequests: domain=" + DomainSelectionService.getDomainName(domain)
                    + ", size=" + pendingRequests.size());
        }

        // When the domain selection request is failed, SMS should be fallback
        // to the legacy implementation.
        boolean wasDomainUnknown = false;

        if (domain == NetworkRegistrationInfo.DOMAIN_UNKNOWN) {
            logd("sendAllPendingRequests: fallback - imsAvailable="
                    + mImsSmsDispatcher.isAvailable());

            wasDomainUnknown = true;

            if (mImsSmsDispatcher.isAvailable()) {
                domain = NetworkRegistrationInfo.DOMAIN_PS;
            } else {
                domain = NetworkRegistrationInfo.DOMAIN_CS;
            }
        }

        for (PendingRequest r : pendingRequests) {
            switch (r.type) {
                case PendingRequest.TYPE_DATA:
                    sendData(domain, r);
                    break;
                case PendingRequest.TYPE_TEXT:
                    // When the domain selection request is failed, emergency SMS should be fallback
                    // to the legacy implementation.
                    if (wasDomainUnknown
                            && domain != NetworkRegistrationInfo.DOMAIN_PS
                            && mImsSmsDispatcher.isEmergencySmsSupport(r.destAddr)) {
                        domain = NetworkRegistrationInfo.DOMAIN_PS;
                    }
                    sendText(domain, r);
                    break;
                case PendingRequest.TYPE_MULTIPART_TEXT:
                    sendMultipartText(domain, r);
                    break;
                case PendingRequest.TYPE_RETRY_SMS:
                    sendRetrySms(r.tracker, (domain == NetworkRegistrationInfo.DOMAIN_PS));
                    break;
                default:
                    // Not reachable.
                    break;
            }
        }

        holder.clearAllRequests();
    }

    /**
     * Sends a data based SMS to a specific application port.
     *
     * @param domain The domain where the SMS is being sent, which can be one of the following:
     *               - {@link NetworkRegistrationInfo#DOMAIN_PS}
     *               - {@link NetworkRegistrationInfo#DOMAIN_CS}
     * @param request The pending request for MO SMS.
     */
    private void sendData(@NetworkRegistrationInfo.Domain int domain,
            @NonNull PendingRequest request) {
        if (domain == NetworkRegistrationInfo.DOMAIN_PS) {
            mImsSmsDispatcher.sendData(request.callingPackage, request.callingUser,
                    request.destAddr, request.scAddr, request.destPort, request.data,
                    request.sentIntents.get(0), request.deliveryIntents.get(0), request.isForVvm,
                    request.uniqueMessageId);
        } else if (isCdmaMo(domain)) {
            mCdmaDispatcher.sendData(request.callingPackage, request.callingUser, request.destAddr,
                    request.scAddr, request.destPort, request.data,
                    request.sentIntents.get(0), request.deliveryIntents.get(0), request.isForVvm,
                    request.uniqueMessageId);
        } else {
            mGsmDispatcher.sendData(request.callingPackage, request.callingUser, request.destAddr,
                    request.scAddr, request.destPort, request.data,
                    request.sentIntents.get(0), request.deliveryIntents.get(0), request.isForVvm,
                    request.uniqueMessageId);
        }
    }

    /**
     * Sends a text based SMS.
     *
     * @param domain The domain where the SMS is being sent, which can be one of the following:
     *               - {@link NetworkRegistrationInfo#DOMAIN_PS}
     *               - {@link NetworkRegistrationInfo#DOMAIN_CS}
     * @param request The pending request for MO SMS.
     */
    private void sendText(@NetworkRegistrationInfo.Domain int domain,
            @NonNull PendingRequest request) {
        if (domain == NetworkRegistrationInfo.DOMAIN_PS) {
            mImsSmsDispatcher.sendText(request.destAddr, request.scAddr, request.texts.get(0),
                    request.sentIntents.get(0), request.deliveryIntents.get(0),
                    request.messageUri, request.callingPackage, request.callingUser,
                    request.persistMessage, request.priority,  /*request.expectMore*/ false,
                    request.validityPeriod, request.isForVvm, request.messageId,
                    request.skipShortCodeCheck, request.uniqueMessageId);
        } else {
            if (isCdmaMo(domain)) {
                mCdmaDispatcher.sendText(request.destAddr, request.scAddr, request.texts.get(0),
                        request.sentIntents.get(0), request.deliveryIntents.get(0),
                        request.messageUri, request.callingPackage, request.callingUser,
                        request.persistMessage, request.priority, request.expectMore,
                        request.validityPeriod, request.isForVvm, request.messageId,
                        request.skipShortCodeCheck, request.uniqueMessageId);
            } else {
                mGsmDispatcher.sendText(request.destAddr, request.scAddr, request.texts.get(0),
                        request.sentIntents.get(0), request.deliveryIntents.get(0),
                        request.messageUri, request.callingPackage, request.callingUser,
                        request.persistMessage, request.priority, request.expectMore,
                        request.validityPeriod, request.isForVvm, request.messageId,
                        request.skipShortCodeCheck, request.uniqueMessageId);
            }
        }
    }

    /**
     * Sends a multi-part text based SMS.
     *
     * @param domain The domain where the SMS is being sent, which can be one of the following:
     *               - {@link NetworkRegistrationInfo#DOMAIN_PS}
     *               - {@link NetworkRegistrationInfo#DOMAIN_CS}
     * @param request The pending request for MO SMS.
     */
    private void sendMultipartText(@NetworkRegistrationInfo.Domain int domain,
            @NonNull PendingRequest request) {
        if (domain == NetworkRegistrationInfo.DOMAIN_PS) {
            mImsSmsDispatcher.sendMultipartText(request.destAddr, request.scAddr, request.texts,
                    request.sentIntents, request.deliveryIntents, request.messageUri,
                    request.callingPackage, request.callingUser, request.persistMessage,
                    request.priority, false /*request.expectMore*/, request.validityPeriod,
                    request.messageId, request.uniqueMessageId);
        } else {
            if (isCdmaMo(domain)) {
                mCdmaDispatcher.sendMultipartText(request.destAddr, request.scAddr, request.texts,
                        request.sentIntents, request.deliveryIntents, request.messageUri,
                        request.callingPackage, request.callingUser, request.persistMessage,
                        request.priority, request.expectMore, request.validityPeriod,
                        request.messageId, request.uniqueMessageId);
            } else {
                mGsmDispatcher.sendMultipartText(request.destAddr, request.scAddr, request.texts,
                        request.sentIntents, request.deliveryIntents, request.messageUri,
                        request.callingPackage, request.callingUser, request.persistMessage,
                        request.priority, request.expectMore, request.validityPeriod,
                        request.messageId, request.uniqueMessageId);
            }
        }
    }

    private void triggerSentIntentForFailure(PendingIntent sentIntent) {
        if (sentIntent == null) {
            logd("sentIntent is null");
            return;
        }
        try {
            sentIntent.send(SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        } catch (CanceledException e) {
            logd("Intent has been canceled!");
        }
    }

    private void triggerSentIntentForFailure(List<PendingIntent> sentIntents) {
        if (sentIntents == null) {
            logd("sentIntents is null");
            return;
        }
        for (PendingIntent sentIntent : sentIntents) {
            triggerSentIntentForFailure(sentIntent);
        }
    }

    /**
     * Creates an ArrayList object from any object.
     */
    private static <T> ArrayList<T> asArrayList(T object) {
        ArrayList<T> list = new ArrayList<>();
        list.add(object);
        return list;
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
    protected void sendData(String callingPackage, int callingUser, String destAddr, String scAddr,
            int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent,
            boolean isForVvm) {
        if (TextUtils.isEmpty(scAddr)) {
            scAddr = getSmscAddressFromUSIMWithPhoneIdentity(callingPackage);
        }

        if (isSmsDomainSelectionEnabled()) {
            sendSmsUsingDomainSelection(getDomainSelectionConnectionHolder(false),
                    new PendingRequest(PendingRequest.TYPE_DATA, null, callingPackage, callingUser,
                            destAddr, scAddr, asArrayList(sentIntent),
                            asArrayList(deliveryIntent), isForVvm, data, destPort, null,
                            null, false, 0, false, 0,
                            0L, false, false),
                    "sendData");
            return;
        }

        if (mImsSmsDispatcher.isAvailable()) {
            mImsSmsDispatcher.sendData(callingPackage, callingUser, destAddr, scAddr, destPort,
                    data, sentIntent, deliveryIntent, isForVvm,
                    PendingRequest.getNextUniqueMessageId());
        } else if (isCdmaMo()) {
            mCdmaDispatcher.sendData(callingPackage, callingUser, destAddr, scAddr, destPort, data,
                    sentIntent, deliveryIntent, isForVvm,
                    PendingRequest.getNextUniqueMessageId());
        } else {
            mGsmDispatcher.sendData(callingPackage, callingUser, destAddr, scAddr, destPort, data,
                    sentIntent, deliveryIntent, isForVvm,
                    PendingRequest.getNextUniqueMessageId());
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
            PendingIntent deliveryIntent, Uri messageUri, String callingPkg, int callingUser,
            boolean persistMessage, int priority, boolean expectMore, int validityPeriod,
            boolean isForVvm, long messageId) {
        sendText(destAddr, scAddr, text, sentIntent, deliveryIntent, messageUri, callingPkg,
                callingUser, persistMessage, priority, expectMore, validityPeriod, isForVvm,
                messageId, false);
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
     * @param skipShortCodeCheck Skip check for short code type destination address.
     */
    public void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent,
            PendingIntent deliveryIntent, Uri messageUri, String callingPkg, int callingUser,
            boolean persistMessage, int priority, boolean expectMore, int validityPeriod,
            boolean isForVvm, long messageId, boolean skipShortCodeCheck) {
        if (TextUtils.isEmpty(scAddr)) {
            scAddr = getSmscAddressFromUSIMWithPhoneIdentity(callingPkg);
        }

        PendingRequest pendingRequest = new PendingRequest(PendingRequest.TYPE_TEXT, null,
                callingPkg, callingUser, destAddr, scAddr, asArrayList(sentIntent),
                asArrayList(deliveryIntent), isForVvm, null, 0, asArrayList(text),
                messageUri, persistMessage, priority, expectMore, validityPeriod, messageId,
                skipShortCodeCheck, false);

        if (SatelliteController.getInstance().shouldSendSmsToDatagramDispatcher(mPhone)) {
            // Send P2P SMS using carrier roaming NB IOT NTN
            DatagramDispatcher.getInstance().sendSms(pendingRequest);
            return;
        } else if (SatelliteController.getInstance().isInCarrierRoamingNbIotNtn()) {
            Rlog.d(TAG, "Block SMS in carrier roaming NB IOT NTN mode.");
            // Block SMS in satellite mode if P2P SMS is not supported.
            triggerSentIntentForFailure(pendingRequest.sentIntents);
            return;
        }

        sendTextInternal(pendingRequest);
    }

    private void sendTextInternal(PendingRequest request) {
        logd("sendTextInternal: messageId=" + request.messageId
                 + ", uniqueMessageId=" + request.uniqueMessageId);
        if (isSmsDomainSelectionEnabled()) {
            boolean isEmergency = isEmergencyNumber(request.destAddr);
            sendSmsUsingDomainSelection(getDomainSelectionConnectionHolder(isEmergency),
                    request, "sendText");
            return;
        }

        if (mImsSmsDispatcher.isAvailable() || mImsSmsDispatcher.isEmergencySmsSupport(
                request.destAddr)) {
            mImsSmsDispatcher.sendText(request.destAddr, request.scAddr, request.texts.get(0),
                    request.sentIntents.get(0), request.deliveryIntents.get(0),
                    request.messageUri, request.callingPackage, request.callingUser,
                    request.persistMessage, request.priority, false /*expectMore*/,
                    request.validityPeriod, request.isForVvm, request.messageId,
                    request.skipShortCodeCheck, request.uniqueMessageId);
        } else {
            if (isCdmaMo()) {
                mCdmaDispatcher.sendText(request.destAddr, request.scAddr, request.texts.get(0),
                        request.sentIntents.get(0), request.deliveryIntents.get(0),
                        request.messageUri, request.callingPackage, request.callingUser,
                        request.persistMessage, request.priority, request.expectMore,
                        request.validityPeriod, request.isForVvm, request.messageId,
                        request.skipShortCodeCheck, request.uniqueMessageId);
            } else {
                mGsmDispatcher.sendText(request.destAddr, request.scAddr, request.texts.get(0),
                        request.sentIntents.get(0), request.deliveryIntents.get(0),
                        request.messageUri, request.callingPackage, request.callingUser,
                        request.persistMessage, request.priority, request.expectMore,
                        request.validityPeriod, request.isForVvm, request.messageId,
                        request.skipShortCodeCheck, request.uniqueMessageId);
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
            int callingUser, boolean persistMessage, int priority, boolean expectMore,
            int validityPeriod, long messageId) {
        if (TextUtils.isEmpty(scAddr)) {
            scAddr = getSmscAddressFromUSIMWithPhoneIdentity(callingPkg);
        }

        PendingRequest pendingRequest = new PendingRequest(PendingRequest.TYPE_MULTIPART_TEXT, null,
                callingPkg, callingUser, destAddr, scAddr, sentIntents, deliveryIntents, false,
                null, 0, parts, messageUri, persistMessage, priority, expectMore,
                validityPeriod, messageId, false, false);

        if (SatelliteController.getInstance().shouldSendSmsToDatagramDispatcher(mPhone)) {
            // Send multipart P2P SMS using carrier roaming NB IOT NTN
            DatagramDispatcher.getInstance().sendSms(pendingRequest);
            return;
        } else if (SatelliteController.getInstance().isInCarrierRoamingNbIotNtn()) {
            Rlog.d(TAG, "Block SMS in carrier roaming NB IOT NTN mode.");
            // Block SMS in satellite mode if P2P SMS is not supported.
            triggerSentIntentForFailure(pendingRequest.sentIntents);
            return;
        }

        sendMultipartTextInternal(pendingRequest);
    }

    private boolean isEmergencyNumber(String number) {
        if (!mPhone.hasCalling()) return false;
        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        if (tm == null) return false;
        return tm.isEmergencyNumber(number);
    }

    private void sendMultipartTextInternal(PendingRequest request) {
        logd("sendMultipartTextInternal: messageId=" + request.messageId);
        if (isSmsDomainSelectionEnabled()) {
            boolean isEmergency = isEmergencyNumber(request.destAddr);
            sendSmsUsingDomainSelection(getDomainSelectionConnectionHolder(isEmergency),
                    request, "sendMultipartText");
            return;
        }

        if (mImsSmsDispatcher.isAvailable()) {
            mImsSmsDispatcher.sendMultipartText(request.destAddr, request.scAddr, request.texts,
                    request.sentIntents, request.deliveryIntents, request.messageUri,
                    request.callingPackage, request.callingUser, request.persistMessage,
                    request.priority, false /*expectMore*/, request.validityPeriod,
                    request.messageId, request.uniqueMessageId);
        } else {
            if (isCdmaMo()) {
                mCdmaDispatcher.sendMultipartText(request.destAddr, request.scAddr, request.texts,
                        request.sentIntents, request.deliveryIntents, request.messageUri,
                        request.callingPackage, request.callingUser, request.persistMessage,
                        request.priority, request.expectMore, request.validityPeriod,
                        request.messageId, request.uniqueMessageId);
            } else {
                mGsmDispatcher.sendMultipartText(request.destAddr, request.scAddr, request.texts,
                        request.sentIntents, request.deliveryIntents, request.messageUri,
                        request.callingPackage, request.callingUser, request.persistMessage,
                        request.priority, request.expectMore, request.validityPeriod,
                        request.messageId, request.uniqueMessageId);
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

    /**
     * This API should be used only by {@link DatagramDispatcher} to send SMS over
     * non-terrestrial network.
     *
     * @param request {@link PendingRequest} object that contains all the information required to
     *                send MO SMS.
     */
    public void sendCarrierRoamingNbIotNtnText(@NonNull PendingRequest request) {
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) {
            logd("sendCarrierRoamingNbIotNtnText: carrier roaming nb iot ntn "
                    + "feature flag is disabled");
            return;
        }

        sendMessage(obtainMessage(CMD_SEND_TEXT, request));
    }

    /**
     * Send error code to pending MO SMS request.
     *
     * @param pendingRequest {@link PendingRequest} object that contains all the information
     *                       related to MO SMS.
     * @param errorCode error code to be returned.
     */
    public void onSendCarrierRoamingNbIotNtnTextError(@NonNull PendingRequest pendingRequest,
            @SatelliteManager.SatelliteResult int errorCode) {
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) {
            logd("onSendCarrierRoamingNbIotNtnTextError: carrier roaming nb iot ntn "
                    + "feature flag is disabled");
            return;
        }

        logd("onSendCarrierRoamingNbIotNtnTextError: messageId=" + pendingRequest.messageId
                + " errorCode=" + errorCode);
        sendMessage(obtainMessage(EVENT_SEND_TEXT_OVER_NTN_ERROR, pendingRequest));
    }

    /**
     * This API should be used only by {@link DatagramDispatcher} to send MT SMS Polling message
     * over non-terrestrial network.
     * To enable users to receive incoming messages, the device needs to send an MO SMS to itself
     * to trigger SMSC to send all pending SMS to the particular subscription.
     */
    public void sendMtSmsPollingMessage() {
        if (!SatelliteController.getInstance().shouldSendSmsToDatagramDispatcher(mPhone)) {
            logd("sendMtSmsPollingMessage: not in roaming nb iot ntn");
            return;
        }

        SubscriptionManager subscriptionManager = (SubscriptionManager) mContext
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        String destAddr = subscriptionManager.getPhoneNumber(mPhone.getSubId());
        if (TextUtils.isEmpty(destAddr)) {
            logd("sendMtSmsPollingMessage: destAddr is null or empty.");
            return;
        }

        String mtSmsPollingText = mContext.getResources()
                .getString(R.string.config_mt_sms_polling_text);
        if (TextUtils.isEmpty(mtSmsPollingText)) {
            logd("sendMtSmsPollingMessage: mtSmsPollingText is null or empty.");
            return;
        }

        String callingPackage = mContext.getPackageName();
        PendingRequest pendingRequest = new PendingRequest(PendingRequest.TYPE_TEXT, null,
                callingPackage, Binder.getCallingUserHandle().getIdentifier(), destAddr,
                getSmscAddressFromUSIMWithPhoneIdentity(callingPackage), asArrayList(null),
                asArrayList(null), false, null, 0, asArrayList(mtSmsPollingText), null, false, 0,
                false, 5, 0L, true, true);

        if (SatelliteController.getInstance().shouldSendSmsToDatagramDispatcher(mPhone)) {
            DatagramDispatcher.getInstance().sendSms(pendingRequest);
        }
    }

    public interface SmsInjectionCallback {
        void onSmsInjectedResult(int result);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        mGsmInboundSmsHandler.dump(fd, pw, args);
        if (mCdmaInboundSmsHandler != null) mCdmaInboundSmsHandler.dump(fd, pw, args);
        mGsmDispatcher.dump(fd, pw, args);
        if (mCdmaDispatcher != null) mCdmaDispatcher.dump(fd, pw, args);
        mImsSmsDispatcher.dump(fd, pw, args);
    }

    private void logd(String msg) {
        Rlog.d(TAG, msg);
    }

    private void logi(String s) {
        Rlog.i(TAG + " [" + mPhone.getPhoneId() + "]", s);
    }
}

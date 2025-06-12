/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.internal.telephony.satellite;

import static android.telephony.satellite.SatelliteManager.DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS;
import static android.telephony.satellite.SatelliteManager.DATAGRAM_TYPE_SMS;
import static android.telephony.satellite.SatelliteManager.DATAGRAM_TYPE_UNKNOWN;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_UNKNOWN;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_MODEM_TIMEOUT;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_NETWORK_ERROR;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_NOT_REACHABLE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_SUCCESS;

import static com.android.internal.telephony.SmsDispatchersController.PendingRequest;
import static com.android.internal.telephony.satellite.DatagramController.ROUNDING_UNIT;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.PersistentLogger;
import android.telephony.Rlog;
import android.telephony.satellite.SatelliteDatagram;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.SatelliteSessionStats;
import android.util.Log;

import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.metrics.SatelliteStats;
import com.android.internal.telephony.satellite.metrics.ControllerMetricsStats;
import com.android.internal.telephony.satellite.metrics.SessionMetricsStats;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Datagram dispatcher used to send satellite datagrams.
 */
public class DatagramDispatcher extends Handler {
    private static final String TAG = "DatagramDispatcher";

    private static final int CMD_SEND_SATELLITE_DATAGRAM = 1;
    private static final int EVENT_SEND_SATELLITE_DATAGRAM_DONE = 2;
    private static final int EVENT_WAIT_FOR_DEVICE_ALIGNMENT_IN_DEMO_MODE_TIMED_OUT = 3;
    private static final int EVENT_DATAGRAM_WAIT_FOR_CONNECTED_STATE_TIMED_OUT = 4;
    private static final int EVENT_WAIT_FOR_DATAGRAM_SENDING_RESPONSE_TIMED_OUT = 5;
    private static final int EVENT_ABORT_SENDING_SATELLITE_DATAGRAMS_DONE = 6;
    private static final int EVENT_WAIT_FOR_SIMULATED_POLL_DATAGRAMS_DELAY_TIMED_OUT = 7;
    private static final int CMD_SEND_SMS = 8;
    private static final int EVENT_SEND_SMS_DONE = 9;
    private static final int EVENT_MT_SMS_POLLING_THROTTLE_TIMED_OUT = 10;
    private static final Long TIMEOUT_DATAGRAM_DELAY_IN_DEMO_MODE = TimeUnit.SECONDS.toMillis(10);
    @NonNull private static DatagramDispatcher sInstance;
    @NonNull private final Context mContext;
    @NonNull private final DatagramController mDatagramController;
    @NonNull private final ControllerMetricsStats mControllerMetricsStats;
    @NonNull private final SessionMetricsStats mSessionMetricsStats;
    @NonNull private final FeatureFlags mFeatureFlags;

    private boolean mIsDemoMode = false;
    private boolean mIsAligned = false;
    private DatagramDispatcherHandlerRequest mSendSatelliteDatagramRequest = null;

    private static AtomicLong mNextDatagramId = new AtomicLong(0);

    private AtomicBoolean mShouldSendDatagramToModemInDemoMode = null;

    private final Object mLock = new Object();
    private long mDemoTimeoutDuration = TIMEOUT_DATAGRAM_DELAY_IN_DEMO_MODE;

    /** {@code true} if already sent an emergency datagram during a session */
    @GuardedBy("mLock")
    private boolean mIsEmergencyCommunicationEstablished = false;

    @GuardedBy("mLock")
    private boolean mSendingInProgress;

    /**
     * Map key: datagramId, value: SendSatelliteDatagramArgument to retry sending emergency
     * datagrams.
     */
    @GuardedBy("mLock")
    private final LinkedHashMap<Long, SendSatelliteDatagramArgument>
            mPendingEmergencyDatagramsMap = new LinkedHashMap<>();

    /**
     * Map key: datagramId, value: SendSatelliteDatagramArgument to retry sending non-emergency
     * datagrams.
     */
    @GuardedBy("mLock")
    private final LinkedHashMap<Long, SendSatelliteDatagramArgument>
            mPendingNonEmergencyDatagramsMap = new LinkedHashMap<>();

    /**
     * Map key: messageId, value: {@link PendingRequest} which contains all the information to send
     * carrier roaming nb iot ntn SMS.
     */
    @GuardedBy("mLock")
    private final LinkedHashMap<Long, PendingRequest> mPendingSmsMap = new LinkedHashMap<>();

    private long mWaitTimeForDatagramSendingResponse;
    private long mWaitTimeForDatagramSendingForLastMessageResponse;
    @SatelliteManager.DatagramType
    private int mLastSendRequestDatagramType = DATAGRAM_TYPE_UNKNOWN;
    @Nullable private PersistentLogger mPersistentLogger = null;

    @GuardedBy("mLock")
    private int mModemState = SATELLITE_MODEM_STATE_UNKNOWN;
    @GuardedBy("mLock")
    private boolean mHasEnteredConnectedState = false;
    @GuardedBy("mLock")
    private boolean mShouldPollMtSms = false;
    @GuardedBy("mLock")
    private boolean mIsMtSmsPollingThrottled = false;
    @GuardedBy("mLock")
    private int mConnectedStateCounter = 0;
    private long mSmsTransmissionStartTime = 0;

    /**
     * Create the DatagramDispatcher singleton instance.
     * @param context The Context to use to create the DatagramDispatcher.
     * @param looper The looper for the handler.
     * @param featureFlags The telephony feature flags.
     * @param datagramController DatagramController which is used to update datagram transfer state.
     * @return The singleton instance of DatagramDispatcher.
     */
    public static DatagramDispatcher make(@NonNull Context context, @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags,
            @NonNull DatagramController datagramController) {
        if (sInstance == null) {
            sInstance = new DatagramDispatcher(context, looper, featureFlags, datagramController);
        }
        return sInstance;
    }

    /**
     * @return The singleton instance of DatagramDispatcher.
     */
    public static DatagramDispatcher getInstance() {
        if (sInstance == null) {
            loge("DatagramDispatcher was not yet initialized.");
        }
        return sInstance;
    }

    /**
     * Create a DatagramDispatcher to send satellite datagrams.
     *
     * @param context The Context for the DatagramDispatcher.
     * @param looper The looper for the handler.
     * @param featureFlags The telephony feature flags.
     * @param datagramController DatagramController which is used to update datagram transfer state.
     */
    @VisibleForTesting
    protected DatagramDispatcher(@NonNull Context context, @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags,
            @NonNull DatagramController datagramController) {
        super(looper);
        mContext = context;
        mFeatureFlags = featureFlags;
        mDatagramController = datagramController;
        mControllerMetricsStats = ControllerMetricsStats.getInstance();
        mSessionMetricsStats = SessionMetricsStats.getInstance();
        mPersistentLogger = SatelliteServiceUtils.getPersistentLogger(context);

        synchronized (mLock) {
            mSendingInProgress = false;
        }
        mWaitTimeForDatagramSendingResponse = getWaitForDatagramSendingResponseTimeoutMillis();
        mWaitTimeForDatagramSendingForLastMessageResponse =
                getWaitForDatagramSendingResponseForLastMessageTimeoutMillis();
    }

    private static final class DatagramDispatcherHandlerRequest {
        /** The argument to use for the request */
        public @NonNull Object argument;
        /** The caller needs to specify the phone to be used for the request */
        public @NonNull Phone phone;
        /** The result of the request that is run on the main thread */
        public @Nullable Object result;

        DatagramDispatcherHandlerRequest(Object argument, Phone phone) {
            this.argument = argument;
            this.phone = phone;
        }
    }

    private static final class SendSatelliteDatagramArgument {
        public int subId;
        public long datagramId;
        public @SatelliteManager.DatagramType int datagramType;
        public @NonNull SatelliteDatagram datagram;
        public boolean needFullScreenPointingUI;
        public @NonNull Consumer<Integer> callback;
        public long datagramStartTime;
        public boolean skipCheckingSatelliteAligned = false;

        SendSatelliteDatagramArgument(int subId, long datagramId,
                @SatelliteManager.DatagramType int datagramType,
                @NonNull SatelliteDatagram datagram, boolean needFullScreenPointingUI,
                @NonNull Consumer<Integer> callback) {
            this.subId = subId;
            this.datagramId = datagramId;
            this.datagramType = datagramType;
            this.datagram = datagram;
            this.needFullScreenPointingUI = needFullScreenPointingUI;
            this.callback = callback;
        }

        /** returns the size of outgoing SMS, rounded by 10 bytes */
        public int getDatagramRoundedSizeBytes() {
            if (datagram.getSatelliteDatagram() != null) {
                int sizeBytes = datagram.getSatelliteDatagram().length;
                // rounded by ROUNDING_UNIT
                return (int) (Math.round((double) sizeBytes / ROUNDING_UNIT) * ROUNDING_UNIT);
            } else {
                return 0;
            }
        }

        /** sets the start time at datagram is sent out */
        public void setDatagramStartTime() {
            datagramStartTime =
                    datagramStartTime == 0 ? System.currentTimeMillis() : datagramStartTime;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        DatagramDispatcherHandlerRequest request;
        Message onCompleted;
        AsyncResult ar;

        switch(msg.what) {
            case CMD_SEND_SATELLITE_DATAGRAM: {
                plogd("CMD_SEND_SATELLITE_DATAGRAM mIsDemoMode=" + mIsDemoMode
                        + ", shouldSendDatagramToModemInDemoMode="
                        + shouldSendDatagramToModemInDemoMode());
                request = (DatagramDispatcherHandlerRequest) msg.obj;
                SendSatelliteDatagramArgument argument =
                        (SendSatelliteDatagramArgument) request.argument;
                argument.setDatagramStartTime();
                onCompleted = obtainMessage(EVENT_SEND_SATELLITE_DATAGRAM_DONE, request);

                synchronized (mLock) {
                    if (mIsDemoMode && !shouldSendDatagramToModemInDemoMode()) {
                        AsyncResult.forMessage(onCompleted, SATELLITE_RESULT_SUCCESS, null);
                        sendMessageDelayed(onCompleted, getDemoTimeoutDuration());
                    } else {
                        SatelliteModemInterface.getInstance().sendSatelliteDatagram(
                                argument.datagram,
                                SatelliteServiceUtils.isSosMessage(argument.datagramType),
                                argument.needFullScreenPointingUI, onCompleted);
                        startWaitForDatagramSendingResponseTimer(argument);
                    }
                }
                break;
            }
            case EVENT_SEND_SATELLITE_DATAGRAM_DONE: {
                ar = (AsyncResult) msg.obj;
                request = (DatagramDispatcherHandlerRequest) ar.userObj;
                int error = SatelliteServiceUtils.getSatelliteError(ar, "sendDatagram");
                SendSatelliteDatagramArgument argument =
                        (SendSatelliteDatagramArgument) request.argument;

                synchronized (mLock) {
                    if (mIsDemoMode && (error == SatelliteManager.SATELLITE_RESULT_SUCCESS)) {
                        if (argument.skipCheckingSatelliteAligned) {
                            plogd("Satellite was already aligned. "
                                + "No need to check alignment again");
                        } else if (mDatagramController.waitForAligningToSatellite(mIsAligned)) {
                            plogd("Satellite is not aligned in demo mode, wait for the alignment.");
                            startSatelliteAlignedTimer(request);
                            break;
                        }
                    }
                    plogd("EVENT_SEND_SATELLITE_DATAGRAM_DONE error: " + error
                            + ", mIsDemoMode=" + mIsDemoMode);

                    /*
                     * The response should be ignored if either of the following hold
                     * 1) Framework has already received this response from the vendor service.
                     * 2) Framework has timed out to wait for the response from vendor service for
                     *    the send request.
                     * 3) All pending send requests have been aborted due to some error.
                     */
                    if (!shouldProcessEventSendSatelliteDatagramDone(argument)) {
                        plogw("The message " + argument.datagramId + " was already processed");
                        break;
                    }

                    stopWaitForDatagramSendingResponseTimer();
                    mSendingInProgress = false;

                    // Log metrics about the outgoing datagram
                    reportSendDatagramCompleted(argument, error);
                    // Remove current datagram from pending map.
                    if (SatelliteServiceUtils.isSosMessage(argument.datagramType)) {
                        mPendingEmergencyDatagramsMap.remove(argument.datagramId);
                        if (error == SATELLITE_RESULT_SUCCESS) {
                            mIsEmergencyCommunicationEstablished = true;
                        }
                    } else {
                        mPendingNonEmergencyDatagramsMap.remove(argument.datagramId);
                    }

                    if (error == SATELLITE_RESULT_SUCCESS) {
                        // Update send status for current datagram
                        mDatagramController.updateSendStatus(argument.subId, argument.datagramType,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS,
                                getPendingMessagesCount(), error);
                        startWaitForSimulatedPollDatagramsDelayTimer(request);
                    } else {
                        // Update send status
                        mDatagramController.updateSendStatus(argument.subId, argument.datagramType,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                                getPendingMessagesCount(), error);
                    }

                    if (getPendingMessagesCount() > 0) {
                        // Send response for current datagram
                        argument.callback.accept(error);
                        // Send pending datagrams
                        sendPendingMessages();
                    } else {
                        mDatagramController.updateSendStatus(argument.subId,
                                argument.datagramType,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE, 0,
                                SatelliteManager.SATELLITE_RESULT_SUCCESS);
                        // Send response for current datagram
                        argument.callback.accept(error);
                    }
                }
                break;
            }

            case EVENT_WAIT_FOR_DATAGRAM_SENDING_RESPONSE_TIMED_OUT:
                handleEventWaitForDatagramSendingResponseTimedOut(
                        (SendSatelliteDatagramArgument) msg.obj);
                break;

            case EVENT_WAIT_FOR_DEVICE_ALIGNMENT_IN_DEMO_MODE_TIMED_OUT: {
                handleEventSatelliteAlignedTimeout((DatagramDispatcherHandlerRequest) msg.obj);
                break;
            }

            case EVENT_DATAGRAM_WAIT_FOR_CONNECTED_STATE_TIMED_OUT:
                handleEventDatagramWaitForConnectedStateTimedOut((int) msg.obj);
                break;

            case EVENT_WAIT_FOR_SIMULATED_POLL_DATAGRAMS_DELAY_TIMED_OUT:
                request = (DatagramDispatcherHandlerRequest) msg.obj;
                handleEventWaitForSimulatedPollDatagramsDelayTimedOut(
                        (SendSatelliteDatagramArgument) request.argument);
                break;

            case CMD_SEND_SMS: {
                PendingRequest pendingRequest = (PendingRequest) msg.obj;
                Phone satellitePhone = SatelliteController.getInstance().getSatellitePhone();
                if (satellitePhone == null) {
                    ploge("CMD_SEND_SMS: satellitePhone is null.");
                    return;
                }

                SmsDispatchersController smsDispatchersController =
                        satellitePhone.getSmsDispatchersController();
                if (smsDispatchersController == null) {
                    ploge("CMD_SEND_SMS: smsDispatchersController is null.");
                    return;
                }

                mSmsTransmissionStartTime = System.currentTimeMillis();
                smsDispatchersController.sendCarrierRoamingNbIotNtnText(pendingRequest);
                break;
            }

            case EVENT_SEND_SMS_DONE: {
                SomeArgs args = (SomeArgs) msg.obj;
                int subId = (int) args.arg1;
                long messageId = (long) args.arg2;
                boolean success = (boolean) args.arg3;
                try {
                    handleEventSendSmsDone(subId, messageId, success);
                } finally {
                    args.recycle();
                }
                break;
            }

            case EVENT_MT_SMS_POLLING_THROTTLE_TIMED_OUT: {
                synchronized (mLock) {
                    mIsMtSmsPollingThrottled = false;
                    if (allowMtSmsPolling()) {
                        sendMtSmsPollingMessage();
                    }
                }
                break;
            }

            default:
                plogw("DatagramDispatcherHandler: unexpected message code: " + msg.what);
                break;
        }
    }

    /**
     * Send datagram over satellite.
     *
     * Gateway encodes SOS message or location sharing message into a datagram and passes it as
     * input to this method. Datagram received here will be passed down to modem without any
     * encoding or encryption.
     *
     * @param subId The subId of the subscription to send satellite datagrams for.
     * @param datagramType datagram type indicating whether the datagram is of type
     *                     SOS_SMS or LOCATION_SHARING.
     * @param datagram encoded gateway datagram which is encrypted by the caller.
     *                 Datagram will be passed down to modem without any encoding or encryption.
     * @param needFullScreenPointingUI this is used to indicate pointingUI app to open in
     *                                 full screen mode.
     * @param callback The callback to get {@link SatelliteManager.SatelliteResult} of the request.
     */
    public void sendSatelliteDatagram(int subId, @SatelliteManager.DatagramType int datagramType,
            @NonNull SatelliteDatagram datagram, boolean needFullScreenPointingUI,
            @NonNull Consumer<Integer> callback) {
        Phone phone = SatelliteServiceUtils.getPhone();

        long datagramId = mNextDatagramId.getAndUpdate(
                n -> ((n + 1) % DatagramController.MAX_DATAGRAM_ID));
        SendSatelliteDatagramArgument datagramArgs =
                new SendSatelliteDatagramArgument(subId, datagramId, datagramType, datagram,
                        needFullScreenPointingUI, callback);
        mLastSendRequestDatagramType = datagramType;

        synchronized (mLock) {
            // Add datagram to pending datagram map
            if (SatelliteServiceUtils.isSosMessage(datagramType)) {
                mPendingEmergencyDatagramsMap.put(datagramId, datagramArgs);
            } else {
                mPendingNonEmergencyDatagramsMap.put(datagramId, datagramArgs);
            }

            if (mDatagramController.needsWaitingForSatelliteConnected(datagramType)) {
                plogd("sendDatagram: wait for satellite connected");
                mDatagramController.updateSendStatus(subId, datagramType,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT,
                        getPendingMessagesCount(), SatelliteManager.SATELLITE_RESULT_SUCCESS);
                startDatagramWaitForConnectedStateTimer(datagramArgs.datagramType);
            } else if (!mSendingInProgress && mDatagramController.isPollingInIdleState()) {
                // Modem can be busy receiving datagrams, so send datagram only when modem is
                // not busy.
                mSendingInProgress = true;
                mDatagramController.updateSendStatus(subId, datagramType,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING,
                        getPendingMessagesCount(), SatelliteManager.SATELLITE_RESULT_SUCCESS);
                sendRequestAsync(CMD_SEND_SATELLITE_DATAGRAM, datagramArgs, phone);
            } else {
                plogd("sendDatagram: mSendingInProgress=" + mSendingInProgress
                        + ", isPollingInIdleState=" + mDatagramController.isPollingInIdleState());
            }
        }
    }

    public void retrySendingDatagrams() {
        synchronized (mLock) {
            sendPendingMessages();
        }
    }

    /** Set demo mode
     *
     * @param isDemoMode {@code true} means demo mode is on, {@code false} otherwise.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    protected void setDemoMode(boolean isDemoMode) {
        mIsDemoMode = isDemoMode;
        plogd("setDemoMode: mIsDemoMode=" + mIsDemoMode);
    }

    /**
     * Set whether the device is aligned with the satellite.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void setDeviceAlignedWithSatellite(boolean isAligned) {
        synchronized (mLock) {
            mIsAligned = isAligned;
            plogd("setDeviceAlignedWithSatellite: " + mIsAligned);
            if (isAligned && mIsDemoMode) handleEventSatelliteAligned();
            if (allowMtSmsPolling()) {
                sendMtSmsPollingMessage();
            }
        }
    }

    private void startSatelliteAlignedTimer(@NonNull DatagramDispatcherHandlerRequest request) {
        if (isSatelliteAlignedTimerStarted()) {
            plogd("Satellite aligned timer was already started");
            return;
        }
        mSendSatelliteDatagramRequest = request;
        sendMessageDelayed(
                obtainMessage(EVENT_WAIT_FOR_DEVICE_ALIGNMENT_IN_DEMO_MODE_TIMED_OUT, request),
                getSatelliteAlignedTimeoutDuration());
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected long getSatelliteAlignedTimeoutDuration() {
        return mDatagramController.getSatelliteAlignedTimeoutDuration();
    }

    private void handleEventSatelliteAligned() {
        if (isSatelliteAlignedTimerStarted()) {
            stopSatelliteAlignedTimer();

            if (mSendSatelliteDatagramRequest == null) {
                ploge("handleEventSatelliteAligned: mSendSatelliteDatagramRequest is null");
            } else {
                SendSatelliteDatagramArgument argument =
                        (SendSatelliteDatagramArgument) mSendSatelliteDatagramRequest.argument;
                argument.skipCheckingSatelliteAligned = true;
                Message message = obtainMessage(
                        EVENT_SEND_SATELLITE_DATAGRAM_DONE, mSendSatelliteDatagramRequest);
                mSendSatelliteDatagramRequest = null;
                AsyncResult.forMessage(message, null, null);
                message.sendToTarget();
                plogd("handleEventSatelliteAligned: EVENT_SEND_SATELLITE_DATAGRAM_DONE");
            }
        }
    }

    private void handleEventSatelliteAlignedTimeout(
            @NonNull DatagramDispatcherHandlerRequest request) {
        plogd("handleEventSatelliteAlignedTimeout");
        mSendSatelliteDatagramRequest = null;
        SatelliteManager.SatelliteException exception =
                new SatelliteManager.SatelliteException(
                        SATELLITE_RESULT_NOT_REACHABLE);
        Message message = obtainMessage(EVENT_SEND_SATELLITE_DATAGRAM_DONE, request);
        AsyncResult.forMessage(message, null, exception);
        message.sendToTarget();
    }

    private boolean isSatelliteAlignedTimerStarted() {
        return hasMessages(EVENT_WAIT_FOR_DEVICE_ALIGNMENT_IN_DEMO_MODE_TIMED_OUT);
    }

    private void stopSatelliteAlignedTimer() {
        removeMessages(EVENT_WAIT_FOR_DEVICE_ALIGNMENT_IN_DEMO_MODE_TIMED_OUT);
    }

    @GuardedBy("mLock")
    private void sendPendingMessages() {
        plogd("sendPendingMessages");

        // Pending datagrams are prioritized over pending SMS.
        if (getPendingDatagramCount() > 0) {
            sendPendingDatagrams();
            return;
        }

        if (mFeatureFlags.carrierRoamingNbIotNtn()) {
            if (getPendingSmsCount() > 0) {
                sendPendingSms();
            }
        }
    }

    /**
     * Send pending satellite datagrams. Emergency datagrams are given priority over
     * non-emergency datagrams.
     */
    @GuardedBy("mLock")
    private void sendPendingDatagrams() {
        plogd("sendPendingDatagrams()");
        if (!mDatagramController.isPollingInIdleState()) {
            // Datagram should be sent to satellite modem when modem is free.
            plogd("sendPendingDatagrams: modem is receiving datagrams");
            return;
        }

        if (getPendingDatagramCount() <= 0) {
            plogd("sendPendingDatagrams: no pending datagrams to send");
            return;
        }

        Phone phone = SatelliteServiceUtils.getPhone();
        Set<Entry<Long, SendSatelliteDatagramArgument>> pendingDatagram = null;
        if (!mSendingInProgress && !mPendingEmergencyDatagramsMap.isEmpty()) {
            pendingDatagram = mPendingEmergencyDatagramsMap.entrySet();
        } else if (!mSendingInProgress && !mPendingNonEmergencyDatagramsMap.isEmpty()) {
            pendingDatagram = mPendingNonEmergencyDatagramsMap.entrySet();
        }

        if ((pendingDatagram != null) && pendingDatagram.iterator().hasNext()) {
            SendSatelliteDatagramArgument datagramArg =
                    pendingDatagram.iterator().next().getValue();
            if (mDatagramController.needsWaitingForSatelliteConnected(datagramArg.datagramType)) {
                plogd("sendPendingDatagrams: wait for satellite connected");
                mDatagramController.updateSendStatus(datagramArg.subId,
                        datagramArg.datagramType,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT,
                        getPendingMessagesCount(),
                        SatelliteManager.SATELLITE_RESULT_SUCCESS);
                startDatagramWaitForConnectedStateTimer(
                        datagramArg.datagramType);
                return;
            }

            mSendingInProgress = true;
            // Sets the trigger time for getting pending datagrams
            mDatagramController.updateSendStatus(datagramArg.subId, datagramArg.datagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING,
                    getPendingMessagesCount(), SatelliteManager.SATELLITE_RESULT_SUCCESS);
            sendRequestAsync(CMD_SEND_SATELLITE_DATAGRAM, datagramArg, phone);
        }
    }

    /**
     * Send error code to all the pending datagrams
     *
     * @param pendingDatagramsMap The pending datagrams map to be cleaned up.
     * @param errorCode error code to be returned.
     */
    @GuardedBy("mLock")
    private void sendErrorCodeAndCleanupPendingDatagrams(
            LinkedHashMap<Long, SendSatelliteDatagramArgument> pendingDatagramsMap,
            @SatelliteManager.SatelliteResult int errorCode) {
        if (pendingDatagramsMap.size() == 0) {
            return;
        }
        ploge("sendErrorCodeAndCleanupPendingDatagrams: cleaning up resources");

        // Send error code to all the pending datagrams
        for (Entry<Long, SendSatelliteDatagramArgument> entry :
                pendingDatagramsMap.entrySet()) {
            SendSatelliteDatagramArgument argument = entry.getValue();
            reportSendDatagramCompleted(argument, errorCode);
            argument.callback.accept(errorCode);
        }

        // Clear pending datagram maps
        pendingDatagramsMap.clear();
    }

    /**
     * Abort sending all the pending datagrams.
     *
     * @param subId The subId of the subscription used to send datagram
     * @param errorCode The error code that resulted in abort.
     */
    @GuardedBy("mLock")
    private void abortSendingPendingDatagrams(int subId,
            @SatelliteManager.SatelliteResult int errorCode) {
        plogd("abortSendingPendingDatagrams()");
        sendErrorCodeAndCleanupPendingDatagrams(mPendingEmergencyDatagramsMap, errorCode);
        sendErrorCodeAndCleanupPendingDatagrams(mPendingNonEmergencyDatagramsMap, errorCode);
        sendErrorCodeAndCleanupPendingSms(mPendingSmsMap, errorCode);
    }

    /**
     * Return pending datagram and SMS count
     * @return pending messages count
     */
    public int getPendingMessagesCount() {
        synchronized (mLock) {
            if (mFeatureFlags.carrierRoamingNbIotNtn()) {
                return getPendingDatagramCount() + getPendingSmsCount();
            } else {
                return getPendingDatagramCount();
            }
        }
    }

    /**
     * Return pending datagram count
     * @return pending datagram count
     */
    public int getPendingDatagramCount() {
        synchronized (mLock) {
            return mPendingEmergencyDatagramsMap.size() + mPendingNonEmergencyDatagramsMap.size();
        }
    }

    /**
     * Return pending SMS count
     * @return pending SMS count
     */
    public int getPendingSmsCount() {
        synchronized (mLock) {
            return mPendingSmsMap.size();
        }
    }

    /** Return pending user messages count */
    public int getPendingUserMessagesCount() {
        synchronized (mLock) {
            int pendingUserMessagesCount = 0;
            for (Entry<Long, SendSatelliteDatagramArgument> entry :
                    mPendingNonEmergencyDatagramsMap.entrySet()) {
                SendSatelliteDatagramArgument argument = entry.getValue();
                if (argument.datagramType != SatelliteManager.DATAGRAM_TYPE_KEEP_ALIVE) {
                    pendingUserMessagesCount += 1;
                }
            }
            pendingUserMessagesCount += mPendingEmergencyDatagramsMap.size();
            return pendingUserMessagesCount;
        }
    }

    /**
     * Posts the specified command to be executed on the main thread and returns immediately.
     *
     * @param command command to be executed on the main thread
     * @param argument additional parameters required to perform of the operation
     * @param phone phone object used to perform the operation.
     */
    private void sendRequestAsync(int command, @NonNull Object argument, @Nullable Phone phone) {
        DatagramDispatcherHandlerRequest request = new DatagramDispatcherHandlerRequest(
                argument, phone);
        Message msg = this.obtainMessage(command, request);
        msg.sendToTarget();
    }

    private void reportSendSmsCompleted(@NonNull PendingRequest pendingRequest,
            @SatelliteManager.SatelliteResult int resultCode) {
        int datagramType = pendingRequest.isMtSmsPolling
                ? DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS : DATAGRAM_TYPE_SMS;
        if (resultCode == SATELLITE_RESULT_SUCCESS) {
            long smsTransmissionTime = mSmsTransmissionStartTime > 0
                    ? (System.currentTimeMillis() - mSmsTransmissionStartTime) : 0;
            mSessionMetricsStats.addCountOfSuccessfulOutgoingDatagram(
                    datagramType, smsTransmissionTime);
        } else {
            mSessionMetricsStats.addCountOfFailedOutgoingDatagram(
                    datagramType, resultCode);
        }
    }

    private void reportSendDatagramCompleted(@NonNull SendSatelliteDatagramArgument argument,
            @NonNull @SatelliteManager.SatelliteResult int resultCode) {
        long datagramTransmissionTime = argument.datagramStartTime > 0
                ? (System.currentTimeMillis() - argument.datagramStartTime) : 0;
        SatelliteStats.getInstance().onSatelliteOutgoingDatagramMetrics(
                new SatelliteStats.SatelliteOutgoingDatagramParams.Builder()
                        .setDatagramType(argument.datagramType)
                        .setResultCode(resultCode)
                        .setDatagramSizeBytes(argument.getDatagramRoundedSizeBytes())
                        /* In case pending datagram has not been attempted to send to modem
                        interface. transfer time will be 0. */
                        .setDatagramTransferTimeMillis(datagramTransmissionTime)
                        .setIsDemoMode(mIsDemoMode)
                        .setCarrierId(SatelliteController.getInstance().getSatelliteCarrierId())
                        .setIsNtnOnlyCarrier(SatelliteController.getInstance().isNtnOnlyCarrier())
                        .build());
        if (resultCode == SatelliteManager.SATELLITE_RESULT_SUCCESS) {
            mControllerMetricsStats.reportOutgoingDatagramSuccessCount(argument.datagramType,
                    mIsDemoMode);
            mSessionMetricsStats.addCountOfSuccessfulOutgoingDatagram(argument.datagramType,
                    datagramTransmissionTime);
        } else {
            mControllerMetricsStats.reportOutgoingDatagramFailCount(argument.datagramType,
                    mIsDemoMode);
            mSessionMetricsStats.addCountOfFailedOutgoingDatagram(argument.datagramType,
                    resultCode);
        }
    }

    /**
     * Destroys this DatagramDispatcher. Used for tearing down static resources during testing.
     */
    @VisibleForTesting
    public void destroy() {
        sInstance = null;
    }

    /**
     * This function is used by {@link DatagramController} to notify {@link DatagramDispatcher}
     * that satellite modem state has changed.
     *
     * @param state Current satellite modem state.
     */
    public void onSatelliteModemStateChanged(@SatelliteManager.SatelliteModemState int state) {
        synchronized (mLock) {
            mModemState = state;
            if (state == SatelliteManager.SATELLITE_MODEM_STATE_OFF
                    || state == SatelliteManager.SATELLITE_MODEM_STATE_UNAVAILABLE) {
                plogd("onSatelliteModemStateChanged: cleaning up resources");
                cleanUpResources();
            } else if (state == SatelliteManager.SATELLITE_MODEM_STATE_IDLE) {
                sendPendingMessages();
            }

            if (state == SATELLITE_MODEM_STATE_CONNECTED) {
                mHasEnteredConnectedState = true;

                mConnectedStateCounter++;
                if (isFirstConnected()) {
                    mShouldPollMtSms = shouldPollMtSms();
                }

                if (isDatagramWaitForConnectedStateTimerStarted()) {
                    stopDatagramWaitForConnectedStateTimer();
                    sendPendingMessages();
                }
            }

            if (state == SATELLITE_MODEM_STATE_NOT_CONNECTED) {
                if (mHasEnteredConnectedState) {
                    mHasEnteredConnectedState = false;
                    mShouldPollMtSms = shouldPollMtSms();
                }
            }

            if (allowMtSmsPolling()) {
                sendMtSmsPollingMessage();
            }
        }
    }

    /** Returns true if this is the first time the satellite modem is connected. */
    private boolean isFirstConnected() {
        return mConnectedStateCounter == 1;
    }

    @GuardedBy("mLock")
    private void cleanUpResources() {
        plogd("cleanUpResources");
        mSendingInProgress = false;
        mIsEmergencyCommunicationEstablished = false;

        int subId = SatelliteController.getInstance().getSelectedSatelliteSubId();
        if (getPendingMessagesCount() > 0) {
            mDatagramController.updateSendStatus(subId,
                    mLastSendRequestDatagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                    getPendingMessagesCount(), SatelliteManager.SATELLITE_RESULT_REQUEST_ABORTED);
        }
        mDatagramController.updateSendStatus(subId,
                mLastSendRequestDatagramType,
                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                0, SatelliteManager.SATELLITE_RESULT_SUCCESS);
        abortSendingPendingDatagrams(subId,
                SatelliteManager.SATELLITE_RESULT_REQUEST_ABORTED);

        stopSatelliteAlignedTimer();
        stopDatagramWaitForConnectedStateTimer();
        stopWaitForDatagramSendingResponseTimer();
        stopWaitForSimulatedPollDatagramsDelayTimer();
        mIsDemoMode = false;
        mSendSatelliteDatagramRequest = null;
        mIsAligned = false;
        mLastSendRequestDatagramType = DATAGRAM_TYPE_UNKNOWN;
        mModemState = SATELLITE_MODEM_STATE_UNKNOWN;
        mHasEnteredConnectedState = false;
        mShouldPollMtSms = false;
        mConnectedStateCounter = 0;
        stopMtSmsPollingThrottle();
    }

    /** @return {@code true} if already sent an emergency datagram during a session. */
    public boolean isEmergencyCommunicationEstablished() {
        synchronized (mLock) {
            return mIsEmergencyCommunicationEstablished;
        }
    }

    private void startDatagramWaitForConnectedStateTimer(
            @SatelliteManager.DatagramType int datagramType) {
        if (isDatagramWaitForConnectedStateTimerStarted()) {
            plogd("DatagramWaitForConnectedStateTimer is already started");
            return;
        }
        sendMessageDelayed(obtainMessage(
                        EVENT_DATAGRAM_WAIT_FOR_CONNECTED_STATE_TIMED_OUT, datagramType),
                mDatagramController.getDatagramWaitTimeForConnectedState(
                        SatelliteServiceUtils.isLastSosMessage(datagramType)));
    }

    private void stopDatagramWaitForConnectedStateTimer() {
        removeMessages(EVENT_DATAGRAM_WAIT_FOR_CONNECTED_STATE_TIMED_OUT);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public boolean isDatagramWaitForConnectedStateTimerStarted() {
        return hasMessages(EVENT_DATAGRAM_WAIT_FOR_CONNECTED_STATE_TIMED_OUT);
    }

    /**
     * This API is used by CTS tests to override the mWaitTimeForDatagramSendingResponse.
     */
    void setWaitTimeForDatagramSendingResponse(boolean reset, long timeoutMillis) {
        if (reset) {
            mWaitTimeForDatagramSendingResponse = getWaitForDatagramSendingResponseTimeoutMillis();
        } else {
            mWaitTimeForDatagramSendingResponse = timeoutMillis;
        }
    }

    private void startWaitForDatagramSendingResponseTimer(
            @NonNull SendSatelliteDatagramArgument argument) {
        if (hasMessages(EVENT_WAIT_FOR_DATAGRAM_SENDING_RESPONSE_TIMED_OUT)) {
            plogd("WaitForDatagramSendingResponseTimer was already started");
            return;
        }
        long waitTime = SatelliteServiceUtils.isLastSosMessage(argument.datagramType)
                ? mWaitTimeForDatagramSendingForLastMessageResponse
                : mWaitTimeForDatagramSendingResponse;
        logd("startWaitForDatagramSendingResponseTimer: datagramType=" + argument.datagramType
                + ", waitTime=" + waitTime);
        sendMessageDelayed(obtainMessage(
                EVENT_WAIT_FOR_DATAGRAM_SENDING_RESPONSE_TIMED_OUT, argument), waitTime);
    }

    private void stopWaitForDatagramSendingResponseTimer() {
        removeMessages(EVENT_WAIT_FOR_DATAGRAM_SENDING_RESPONSE_TIMED_OUT);
    }

    private void handleEventDatagramWaitForConnectedStateTimedOut(
            @SatelliteManager.DatagramType int datagramType) {
        plogw("Timed out to wait for satellite connected before sending datagrams");
        synchronized (mLock) {
            int subId = SatelliteController.getInstance().getSelectedSatelliteSubId();
            // Update send status
            mDatagramController.updateSendStatus(subId,
                    datagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                    getPendingMessagesCount(),
                    SATELLITE_RESULT_NOT_REACHABLE);
            mDatagramController.updateSendStatus(subId,
                    datagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                    0, SatelliteManager.SATELLITE_RESULT_SUCCESS);
            abortSendingPendingDatagrams(subId,
                    SATELLITE_RESULT_NOT_REACHABLE);
        }
    }

    private boolean shouldSendDatagramToModemInDemoMode() {
        if (mShouldSendDatagramToModemInDemoMode != null) {
            return mShouldSendDatagramToModemInDemoMode.get();
        }

        try {
            mShouldSendDatagramToModemInDemoMode = new AtomicBoolean(
                    mContext.getResources().getBoolean(
                            R.bool.config_send_satellite_datagram_to_modem_in_demo_mode));
            return mShouldSendDatagramToModemInDemoMode.get();

        } catch (Resources.NotFoundException ex) {
            ploge("shouldSendDatagramToModemInDemoMode: id= "
                    + R.bool.config_send_satellite_datagram_to_modem_in_demo_mode + ", ex=" + ex);
            return false;
        }
    }

    private long getWaitForDatagramSendingResponseTimeoutMillis() {
        return mContext.getResources().getInteger(
                R.integer.config_wait_for_datagram_sending_response_timeout_millis);
    }

    private long getWaitForDatagramSendingResponseForLastMessageTimeoutMillis() {
        return mContext.getResources().getInteger(R.integer
                .config_wait_for_datagram_sending_response_for_last_message_timeout_millis);
    }

    private boolean shouldProcessEventSendSatelliteDatagramDone(
            @NonNull SendSatelliteDatagramArgument argument) {
        synchronized (mLock) {
            if (SatelliteServiceUtils.isSosMessage(argument.datagramType)) {
                return mPendingEmergencyDatagramsMap.containsKey(argument.datagramId);
            } else {
                return mPendingNonEmergencyDatagramsMap.containsKey(argument.datagramId);
            }
        }
    }

    private void handleEventWaitForDatagramSendingResponseTimedOut(
            @NonNull SendSatelliteDatagramArgument argument) {
        synchronized (mLock) {
            plogw("Timed out to wait for the response of the request to send the datagram "
                    + argument.datagramId);

            // Ask vendor service to abort all datagram-sending requests
            SatelliteModemInterface.getInstance().abortSendingSatelliteDatagrams(
                    obtainMessage(EVENT_ABORT_SENDING_SATELLITE_DATAGRAMS_DONE, argument));
            mSendingInProgress = false;

            // Update send status
            mDatagramController.updateSendStatus(argument.subId, argument.datagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                    getPendingMessagesCount(), SATELLITE_RESULT_MODEM_TIMEOUT);
            mDatagramController.updateSendStatus(argument.subId, argument.datagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                    0, SatelliteManager.SATELLITE_RESULT_SUCCESS);

            // Send response for current datagram after updating datagram transfer state
            // internally.
            argument.callback.accept(SATELLITE_RESULT_MODEM_TIMEOUT);

            // Log metrics about the outgoing datagram
            reportSendDatagramCompleted(argument, SATELLITE_RESULT_MODEM_TIMEOUT);
            // Remove current datagram from pending map.
            if (SatelliteServiceUtils.isSosMessage(argument.datagramType)) {
                mPendingEmergencyDatagramsMap.remove(argument.datagramId);
            } else {
                mPendingNonEmergencyDatagramsMap.remove(argument.datagramId);
            }

            // Abort sending all the pending datagrams
            abortSendingPendingDatagrams(argument.subId, SATELLITE_RESULT_MODEM_TIMEOUT);
        }
    }

    /**
     * This API can be used by only CTS to override the cached value for the device overlay config
     * value : config_send_satellite_datagram_to_modem_in_demo_mode, which determines whether
     * outgoing satellite datagrams should be sent to modem in demo mode.
     *
     * @param shouldSendToModemInDemoMode Whether send datagram in demo mode should be sent to
     * satellite modem or not. If it is null, the cache will be cleared.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    protected void setShouldSendDatagramToModemInDemoMode(
            @Nullable Boolean shouldSendToModemInDemoMode) {
        plogd("setShouldSendDatagramToModemInDemoMode(" + (shouldSendToModemInDemoMode == null
                ? "null" : shouldSendToModemInDemoMode) + ")");

        if (shouldSendToModemInDemoMode == null) {
            mShouldSendDatagramToModemInDemoMode = null;
        } else {
            if (mShouldSendDatagramToModemInDemoMode == null) {
                mShouldSendDatagramToModemInDemoMode = new AtomicBoolean(
                        shouldSendToModemInDemoMode);
            } else {
                mShouldSendDatagramToModemInDemoMode.set(shouldSendToModemInDemoMode);
            }
        }
    }

    private void startWaitForSimulatedPollDatagramsDelayTimer(
            @NonNull DatagramDispatcherHandlerRequest request) {
        if (mIsDemoMode) {
            plogd("startWaitForSimulatedPollDatagramsDelayTimer");
            sendMessageDelayed(
                    obtainMessage(EVENT_WAIT_FOR_SIMULATED_POLL_DATAGRAMS_DELAY_TIMED_OUT, request),
                    getDemoTimeoutDuration());
        } else {
            plogd("Should not start WaitForSimulatedPollDatagramsDelayTimer in non-demo mode");
        }
    }

    private void stopWaitForSimulatedPollDatagramsDelayTimer() {
        removeMessages(EVENT_WAIT_FOR_SIMULATED_POLL_DATAGRAMS_DELAY_TIMED_OUT);
    }

    private void handleEventWaitForSimulatedPollDatagramsDelayTimedOut(
            @NonNull SendSatelliteDatagramArgument argument) {
        if (mIsDemoMode) {
            plogd("handleEventWaitForSimulatedPollDatagramsDelayTimedOut");
            mDatagramController.pushDemoModeDatagram(argument.datagramType, argument.datagram);
            Consumer<Integer> internalCallback = new Consumer<Integer>() {
                @Override
                public void accept(Integer result) {
                    plogd("pollPendingSatelliteDatagrams result: " + result);
                }
            };
            mDatagramController.pollPendingSatelliteDatagrams(argument.subId, internalCallback);
        } else {
            plogd("Unexpected EVENT_WAIT_FOR_SIMULATED_POLL_DATAGRAMS_DELAY_TIMED_OUT in "
                    + "non-demo mode");
        }
    }

    long getDemoTimeoutDuration() {
        return mDemoTimeoutDuration;
    }

    /**
     * This API is used by CTS tests to override the mDemoTimeoutDuration.
     */
    void setTimeoutDatagramDelayInDemoMode(boolean reset, long timeoutMillis) {
        if (!mIsDemoMode) {
            return;
        }
        if (reset) {
            mDemoTimeoutDuration = TIMEOUT_DATAGRAM_DELAY_IN_DEMO_MODE;
        } else {
            mDemoTimeoutDuration = timeoutMillis;
        }
        plogd("setTimeoutDatagramDelayInDemoMode " + mDemoTimeoutDuration + " reset=" + reset);
    }

    /**
     * Send carrier roaming nb iot ntn sms.
     *
     * Store SMS in a pending list until following conditions are met:
     * - If messages can be sent only when satellite is connected, then wait until modem state
     * becomes {@link SatelliteManager#SATELLITE_MODEM_STATE_CONNECTED}
     * - If modem is already sending datagrms/SMS or receiving datagrams, then wait until modem
     * becomes IDLE to send current SMS.
     *
     * @param pendingSms {@link PendingRequest} that contains all the information required to send
     *                    carrier roaming nb iot ntn SMS.
     */
    public void sendSms(@NonNull PendingRequest pendingSms) {
        SatelliteController.getInstance().startPointingUI();

        int subId = SatelliteController.getInstance().getSelectedSatelliteSubId();
        long messageId = pendingSms.uniqueMessageId;
        plogd("sendSms: subId=" + subId + " messageId:" + messageId);

        synchronized (mLock) {
            // Add SMS to pending list
            mPendingSmsMap.put(messageId, pendingSms);
            int datagramType = pendingSms.isMtSmsPolling ?
                    DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS : DATAGRAM_TYPE_SMS;
            mLastSendRequestDatagramType = datagramType;

            if (mDatagramController.needsWaitingForSatelliteConnected(datagramType)) {
                plogd("sendSms: wait for satellite connected");
                mDatagramController.updateSendStatus(subId, datagramType,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT,
                        getPendingMessagesCount(), SatelliteManager.SATELLITE_RESULT_SUCCESS);
                startDatagramWaitForConnectedStateTimer(datagramType);
            } else if (!mSendingInProgress && mDatagramController.isPollingInIdleState()) {
                mSendingInProgress = true;
                mDatagramController.updateSendStatus(subId, datagramType,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING,
                        getPendingMessagesCount(), SatelliteManager.SATELLITE_RESULT_SUCCESS);

                sendMessage(obtainMessage(CMD_SEND_SMS, pendingSms));
            } else {
                plogd("sendSms: mSendingInProgress=" + mSendingInProgress
                        + ", isPollingInIdleState=" + mDatagramController.isPollingInIdleState());
            }
        }
    }

    @GuardedBy("mLock")
    private void sendPendingSms() {
        plogd("sendPendingSms");
        if (!mDatagramController.isPollingInIdleState()) {
            // Datagram or SMS should be sent to satellite modem when modem is free.
            plogd("sendPendingSms: modem is receiving datagrams");
            return;
        }

        int subId = SatelliteController.getInstance().getSelectedSatelliteSubId();
        Set<Entry<Long, PendingRequest>> pendingSms = null;
        if (!mSendingInProgress) {
            pendingSms = mPendingSmsMap.entrySet();
        }

        if (pendingSms != null && pendingSms.iterator().hasNext()) {
            PendingRequest pendingRequest = pendingSms.iterator().next().getValue();
            int datagramType = pendingRequest.isMtSmsPolling
                    ? DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS : DATAGRAM_TYPE_SMS;
            if (mDatagramController.needsWaitingForSatelliteConnected(DATAGRAM_TYPE_SMS)) {
                plogd("sendPendingSms: wait for satellite connected");
                mDatagramController.updateSendStatus(subId,
                        datagramType,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT,
                        getPendingMessagesCount(),
                        SatelliteManager.SATELLITE_RESULT_SUCCESS);
                startDatagramWaitForConnectedStateTimer(datagramType);
                return;
            }

            mSendingInProgress = true;
            mDatagramController.updateSendStatus(subId,
                    datagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING,
                    getPendingMessagesCount(), SATELLITE_RESULT_SUCCESS);
            sendMessage(obtainMessage(CMD_SEND_SMS, pendingRequest));
        } else {
            plogd("sendPendingSms: mSendingInProgress=" + mSendingInProgress
                    + " pendingSmsCount=" + getPendingSmsCount());
        }
    }

    /**
     * Sending MO SMS is completed.
     * @param subId subscription ID
     * @param messageId message ID of MO SMS
     * @param success boolean specifying whether MO SMS is successfully sent or not.
     */
    public void onSendSmsDone(int subId, long messageId, boolean success) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = subId;
        args.arg2 = messageId;
        args.arg3 = success;
        sendMessage(obtainMessage(EVENT_SEND_SMS_DONE, args));
    }

    @GuardedBy("mLock")
    private void sendErrorCodeAndCleanupPendingSms(
            LinkedHashMap<Long, PendingRequest> pendingSmsMap,
            @SatelliteManager.SatelliteResult int errorCode) {
        if (pendingSmsMap.size() == 0) {
            plogd("sendErrorCodeAndCleanupPendingSms: pendingSmsMap is empty.");
            return;
        }
        ploge("sendErrorCodeAndCleanupPendingSms: cleaning up resources. "
                + "pendingSmsMap size=" + getPendingSmsCount());

        Phone satellitePhone = SatelliteController.getInstance().getSatellitePhone();
        if (satellitePhone == null) {
            ploge("sendErrorCodeAndCleanupPendingSms: satellitePhone is null.");
            pendingSmsMap.clear();
            return;
        }

        SmsDispatchersController smsDispatchersController =
                satellitePhone.getSmsDispatchersController();
        if (smsDispatchersController == null) {
            ploge("sendErrorCodeAndCleanupPendingSms: smsDispatchersController is null.");
            pendingSmsMap.clear();
            return;
        }

        // Send error code to all the pending text
        for (Entry<Long, PendingRequest> entry : pendingSmsMap.entrySet()) {
            PendingRequest pendingRequest = entry.getValue();
            smsDispatchersController.onSendCarrierRoamingNbIotNtnTextError(
                    pendingRequest, errorCode);
            reportSendSmsCompleted(pendingRequest, errorCode);
        }

        // Clear pending text map
        pendingSmsMap.clear();
    }

    private void handleEventSendSmsDone(int subId, long messageId, boolean success) {
        synchronized (mLock) {
            PendingRequest pendingSms = mPendingSmsMap.remove(messageId);
            if (pendingSms == null) {
                // Just return, the SMS is not sent by DatagramDispatcher such as Data SMS
                plogd("handleEventSendSmsDone there is no pendingSms for messageId=" + messageId);
                return;
            }

            mSendingInProgress = false;
            int datagramType = pendingSms.isMtSmsPolling
                    ? DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS  : DATAGRAM_TYPE_SMS;

            plogd("handleEventSendSmsDone subId=" + subId + " messageId=" + messageId
                    + " success=" + success + " datagramType=" + datagramType);

            if (success) {
                // Update send status
                mDatagramController.updateSendStatus(subId, datagramType,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS,
                        getPendingMessagesCount(), SATELLITE_RESULT_SUCCESS);
                reportSendSmsCompleted(pendingSms, SATELLITE_RESULT_SUCCESS);
                if (datagramType == DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS) {
                    startMtSmsPollingThrottle();
                    mShouldPollMtSms = false;
                }
            } else {
                // Update send status
                mDatagramController.updateSendStatus(subId, datagramType,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                        getPendingMessagesCount(), SATELLITE_RESULT_NETWORK_ERROR);
                reportSendSmsCompleted(pendingSms, SATELLITE_RESULT_NETWORK_ERROR);
            }

            if (getPendingMessagesCount() > 0) {
                sendPendingMessages();
            } else {
                mDatagramController.updateSendStatus(subId, datagramType,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE, 0,
                        SatelliteManager.SATELLITE_RESULT_SUCCESS);
            }
        }
    }

    private boolean isEnabledMtSmsPolling() {
        return mContext.getResources().getBoolean(R.bool.config_enabled_mt_sms_polling);
    }

    private long getMtSmsPollingThrottleMillis() {
        return mContext.getResources().getInteger(
                R.integer.config_mt_sms_polling_throttle_millis);
    }

    private boolean shouldPollMtSms() {
        SatelliteController satelliteController = SatelliteController.getInstance();
        Phone satellitePhone = satelliteController.getSatellitePhone();
        return isEnabledMtSmsPolling()
                && satelliteController.shouldSendSmsToDatagramDispatcher(satellitePhone);
    }

    @GuardedBy("mLock")
    private void sendMtSmsPollingMessage() {
        if (!mShouldPollMtSms) {
            return;
        }

        plogd("sendMtSmsPollingMessage");
        if (!allowCheckMessageInNotConnected()) {
            mShouldPollMtSms = false;
        }

        for (Entry<Long, PendingRequest> entry : mPendingSmsMap.entrySet()) {
            PendingRequest pendingRequest = entry.getValue();
            if (pendingRequest.isMtSmsPolling) {
                plogd("sendMtSmsPollingMessage: mPendingSmsMap already has the polling message.");
                return;
            }
        }

        Phone satellitePhone = SatelliteController.getInstance().getSatellitePhone();
        if (satellitePhone == null) {
            ploge("sendMtSmsPollingMessage: satellitePhone is null.");
            return;
        }

        SmsDispatchersController smsDispatchersController =
                satellitePhone.getSmsDispatchersController();
        if (smsDispatchersController == null) {
            ploge("sendMtSmsPollingMessage: smsDispatchersController is null.");
            return;
        }

        smsDispatchersController.sendMtSmsPollingMessage();
    }

    @GuardedBy("mLock")
    private void startMtSmsPollingThrottle() {
        plogd("startMtSmsPollingThrottle");
        mIsMtSmsPollingThrottled = true;
        sendMessageDelayed(obtainMessage(EVENT_MT_SMS_POLLING_THROTTLE_TIMED_OUT),
                getMtSmsPollingThrottleMillis());
    }

    @GuardedBy("mLock")
    private void stopMtSmsPollingThrottle() {
        mIsMtSmsPollingThrottled = false;
        removeMessages(EVENT_MT_SMS_POLLING_THROTTLE_TIMED_OUT);
    }

    @GuardedBy("mLock")
    private boolean allowMtSmsPolling() {
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) return false;

        if (mIsMtSmsPollingThrottled) return false;

        if (!mIsAligned) return false;

        boolean isModemStateConnectedOrTransferring =
                mModemState == SATELLITE_MODEM_STATE_CONNECTED
                        || mModemState == SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING;
        if (!isModemStateConnectedOrTransferring && !allowCheckMessageInNotConnected()) {
            plogd("EVENT_MT_SMS_POLLING_THROTTLE_TIMED_OUT:"
                    + " allow_check_message_in_not_connected is disabled");
            return false;
        }

        return true;
    }

    private boolean allowCheckMessageInNotConnected() {
        return mContext.getResources()
                .getBoolean(R.bool.config_satellite_allow_check_message_in_not_connected);
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }

    private static void logw(@NonNull String log) { Rlog.w(TAG, log); }

    private void plogd(@NonNull String log) {
        Rlog.d(TAG, log);
        if (mPersistentLogger != null) {
            mPersistentLogger.debug(TAG, log);
        }
    }

    private void plogw(@NonNull String log) {
        Rlog.w(TAG, log);
        if (mPersistentLogger != null) {
            mPersistentLogger.warn(TAG, log);
        }
    }

    private void ploge(@NonNull String log) {
        Rlog.e(TAG, log);
        if (mPersistentLogger != null) {
            mPersistentLogger.error(TAG, log);
        }
    }

    public void updateSessionStatsWithPendingUserMsgCount(SatelliteSessionStats datagramStats) {
        synchronized (mLock) {
            Log.d("SessionMetricsStats1",
                    " mPendingEmergencyDatagramsMap size = "
                            + mPendingEmergencyDatagramsMap.size());
            Log.d("SessionMetricsStats1", " mPendingNonEmergencyDatagramsMap size = "
                    + mPendingNonEmergencyDatagramsMap.size());
            Log.d("SessionMetricsStats1", " mPendingSmsMap size = "
                    + mPendingSmsMap.size());
            for (Entry<Long, SendSatelliteDatagramArgument> entry :
                    mPendingEmergencyDatagramsMap.entrySet()) {
                SendSatelliteDatagramArgument argument = entry.getValue();
                Log.d("SessionMetricsStats1", "DataGramType1 =  "
                        + argument.datagramType);
                datagramStats.updateCountOfUserMessagesInQueueToBeSent(argument.datagramType);
            }
            for (Entry<Long, SendSatelliteDatagramArgument> entry :
                    mPendingNonEmergencyDatagramsMap.entrySet()) {
                SendSatelliteDatagramArgument argument = entry.getValue();
                Log.d("SessionMetricsStats1", "DataGramType2 =  "
                        + argument.datagramType);
                datagramStats.updateCountOfUserMessagesInQueueToBeSent(argument.datagramType);
            }
            for (Entry<Long, PendingRequest> entry : mPendingSmsMap.entrySet()) {
                PendingRequest pendingRequest = entry.getValue();
                int datagramType = pendingRequest.isMtSmsPolling
                        ? DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS : DATAGRAM_TYPE_SMS;
                Log.d("SessionMetricsStats1", "DataGramType3 =  " + datagramType);
                datagramStats.updateCountOfUserMessagesInQueueToBeSent(datagramType);
            }
        }
    }
}

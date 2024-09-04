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

import static android.telephony.satellite.SatelliteManager.DATAGRAM_TYPE_UNKNOWN;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_MODEM_TIMEOUT;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_NOT_REACHABLE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_SUCCESS;

import static com.android.internal.telephony.satellite.DatagramController.ROUNDING_UNIT;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.satellite.SatelliteDatagram;
import android.telephony.satellite.SatelliteManager;

import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
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
    private static final Long TIMEOUT_DATAGRAM_DELAY_IN_DEMO_MODE = TimeUnit.SECONDS.toMillis(10);
    @NonNull private static DatagramDispatcher sInstance;
    @NonNull private final Context mContext;
    @NonNull private final DatagramController mDatagramController;
    @NonNull private final ControllerMetricsStats mControllerMetricsStats;
    @NonNull private final SessionMetricsStats mSessionMetricsStats;

    private boolean mIsDemoMode = false;
    private boolean mIsAligned = false;
    private DatagramDispatcherHandlerRequest mSendSatelliteDatagramRequest = null;

    private static AtomicLong mNextDatagramId = new AtomicLong(0);

    private AtomicBoolean mShouldSendDatagramToModemInDemoMode = null;

    private final Object mLock = new Object();
    private long mDemoTimeoutDuration = TIMEOUT_DATAGRAM_DELAY_IN_DEMO_MODE;

    @GuardedBy("mLock")
    private boolean mSendingDatagramInProgress;

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

    private long mWaitTimeForDatagramSendingResponse;
    @SatelliteManager.DatagramType
    private int mLastSendRequestDatagramType = DATAGRAM_TYPE_UNKNOWN;

    /**
     * Create the DatagramDispatcher singleton instance.
     * @param context The Context to use to create the DatagramDispatcher.
     * @param looper The looper for the handler.
     * @param datagramController DatagramController which is used to update datagram transfer state.
     * @return The singleton instance of DatagramDispatcher.
     */
    public static DatagramDispatcher make(@NonNull Context context, @NonNull Looper looper,
            @NonNull DatagramController datagramController) {
        if (sInstance == null) {
            sInstance = new DatagramDispatcher(context, looper, datagramController);
        }
        return sInstance;
    }

    /**
     * Create a DatagramDispatcher to send satellite datagrams.
     *
     * @param context The Context for the DatagramDispatcher.
     * @param looper The looper for the handler.
     * @param datagramController DatagramController which is used to update datagram transfer state.
     */
    @VisibleForTesting
    protected DatagramDispatcher(@NonNull Context context, @NonNull Looper looper,
            @NonNull DatagramController datagramController) {
        super(looper);
        mContext = context;
        mDatagramController = datagramController;
        mControllerMetricsStats = ControllerMetricsStats.getInstance();
        mSessionMetricsStats = SessionMetricsStats.getInstance();

        synchronized (mLock) {
            mSendingDatagramInProgress = false;
        }
        mWaitTimeForDatagramSendingResponse = getWaitForDatagramSendingResponseTimeoutMillis();
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
                logd("CMD_SEND_SATELLITE_DATAGRAM mIsDemoMode=" + mIsDemoMode
                        + ", shouldSendDatagramToModemInDemoMode="
                        + shouldSendDatagramToModemInDemoMode());
                request = (DatagramDispatcherHandlerRequest) msg.obj;
                SendSatelliteDatagramArgument argument =
                        (SendSatelliteDatagramArgument) request.argument;
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
                            logd("Satellite was already aligned. No need to check alignment again");
                        } else if (!mIsAligned) {
                            logd("Satellite is not aligned in demo mode, wait for the alignment.");
                            startSatelliteAlignedTimer(request);
                            break;
                        }
                    }
                    logd("EVENT_SEND_SATELLITE_DATAGRAM_DONE error: " + error
                            + ", mIsDemoMode=" + mIsDemoMode);

                    /*
                     * The response should be ignored if either of the following hold
                     * 1) Framework has already received this response from the vendor service.
                     * 2) Framework has timed out to wait for the response from vendor service for
                     *    the send request.
                     * 3) All pending send requests have been aborted due to some error.
                     */
                    if (!shouldProcessEventSendSatelliteDatagramDone(argument)) {
                        logw("The message " + argument.datagramId + " was already processed");
                        break;
                    }

                    stopWaitForDatagramSendingResponseTimer();
                    mSendingDatagramInProgress = false;

                    // Log metrics about the outgoing datagram
                    reportSendDatagramCompleted(argument, error);
                    // Remove current datagram from pending map.
                    if (SatelliteServiceUtils.isSosMessage(argument.datagramType)) {
                        mPendingEmergencyDatagramsMap.remove(argument.datagramId);
                    } else {
                        mPendingNonEmergencyDatagramsMap.remove(argument.datagramId);
                    }

                    if (error == SatelliteManager.SATELLITE_RESULT_SUCCESS) {
                        // Update send status for current datagram
                        mDatagramController.updateSendStatus(argument.subId, argument.datagramType,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS,
                                getPendingDatagramCount(), error);
                        startWaitForSimulatedPollDatagramsDelayTimer(request);
                        if (getPendingDatagramCount() > 0) {
                            // Send response for current datagram
                            argument.callback.accept(error);
                            // Send pending datagrams
                            sendPendingDatagrams();
                        } else {
                            mDatagramController.updateSendStatus(argument.subId,
                                    argument.datagramType,
                                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE, 0,
                                    SatelliteManager.SATELLITE_RESULT_SUCCESS);
                            // Send response for current datagram
                            argument.callback.accept(error);
                        }
                    } else {
                        // Update send status
                        mDatagramController.updateSendStatus(argument.subId, argument.datagramType,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                                getPendingDatagramCount(), error);
                        mDatagramController.updateSendStatus(argument.subId, argument.datagramType,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                                0, SatelliteManager.SATELLITE_RESULT_SUCCESS);
                        // Send response for current datagram
                        // after updating datagram transfer state internally.
                        argument.callback.accept(error);
                        // Abort sending all the pending datagrams
                        abortSendingPendingDatagrams(argument.subId,
                                SatelliteManager.SATELLITE_RESULT_REQUEST_ABORTED);
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
                handleEventDatagramWaitForConnectedStateTimedOut(
                        (SendSatelliteDatagramArgument) msg.obj);
                break;

            case EVENT_WAIT_FOR_SIMULATED_POLL_DATAGRAMS_DELAY_TIMED_OUT:
                request = (DatagramDispatcherHandlerRequest) msg.obj;
                handleEventWaitForSimulatedPollDatagramsDelayTimedOut(
                        (SendSatelliteDatagramArgument) request.argument);
                break;

            default:
                logw("DatagramDispatcherHandler: unexpected message code: " + msg.what);
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
                logd("sendDatagram: wait for satellite connected");
                mDatagramController.updateSendStatus(subId, datagramType,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT,
                        getPendingDatagramCount(), SatelliteManager.SATELLITE_RESULT_SUCCESS);
                startDatagramWaitForConnectedStateTimer(datagramArgs);
            } else if (!mSendingDatagramInProgress && mDatagramController.isPollingInIdleState()) {
                // Modem can be busy receiving datagrams, so send datagram only when modem is
                // not busy.
                mSendingDatagramInProgress = true;
                datagramArgs.setDatagramStartTime();
                mDatagramController.updateSendStatus(subId, datagramType,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING,
                        getPendingDatagramCount(), SatelliteManager.SATELLITE_RESULT_SUCCESS);
                sendRequestAsync(CMD_SEND_SATELLITE_DATAGRAM, datagramArgs, phone);
            } else {
                logd("sendDatagram: mSendingDatagramInProgress="
                        + mSendingDatagramInProgress + ", isPollingInIdleState="
                        + mDatagramController.isPollingInIdleState());
            }
        }
    }

    public void retrySendingDatagrams() {
        synchronized (mLock) {
            sendPendingDatagrams();
        }
    }

    /** Set demo mode
     *
     * @param isDemoMode {@code true} means demo mode is on, {@code false} otherwise.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    protected void setDemoMode(boolean isDemoMode) {
        mIsDemoMode = isDemoMode;
        logd("setDemoMode: mIsDemoMode=" + mIsDemoMode);
    }

    /**
     * Set whether the device is aligned with the satellite.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void setDeviceAlignedWithSatellite(boolean isAligned) {
        synchronized (mLock) {
            mIsAligned = isAligned;
            logd("setDeviceAlignedWithSatellite: " + mIsAligned);
            if (isAligned && mIsDemoMode) handleEventSatelliteAligned();
        }
    }

    private void startSatelliteAlignedTimer(@NonNull DatagramDispatcherHandlerRequest request) {
        if (isSatelliteAlignedTimerStarted()) {
            logd("Satellite aligned timer was already started");
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
                loge("handleEventSatelliteAligned: mSendSatelliteDatagramRequest is null");
            } else {
                SendSatelliteDatagramArgument argument =
                        (SendSatelliteDatagramArgument) mSendSatelliteDatagramRequest.argument;
                argument.skipCheckingSatelliteAligned = true;
                Message message = obtainMessage(
                        EVENT_SEND_SATELLITE_DATAGRAM_DONE, mSendSatelliteDatagramRequest);
                mSendSatelliteDatagramRequest = null;
                AsyncResult.forMessage(message, null, null);
                message.sendToTarget();
                logd("handleEventSatelliteAligned: EVENT_SEND_SATELLITE_DATAGRAM_DONE");
            }
        }
    }

    private void handleEventSatelliteAlignedTimeout(
            @NonNull DatagramDispatcherHandlerRequest request) {
        logd("handleEventSatelliteAlignedTimeout");
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

    /**
     * Send pending satellite datagrams. Emergency datagrams are given priority over
     * non-emergency datagrams.
     */
    @GuardedBy("mLock")
    private void sendPendingDatagrams() {
        logd("sendPendingDatagrams()");
        if (!mDatagramController.isPollingInIdleState()) {
            // Datagram should be sent to satellite modem when modem is free.
            logd("sendPendingDatagrams: modem is receiving datagrams");
            return;
        }

        if (getPendingDatagramCount() <= 0) {
            logd("sendPendingDatagrams: no pending datagrams to send");
            return;
        }

        Phone phone = SatelliteServiceUtils.getPhone();
        Set<Entry<Long, SendSatelliteDatagramArgument>> pendingDatagram = null;
        if (!mSendingDatagramInProgress && !mPendingEmergencyDatagramsMap.isEmpty()) {
            pendingDatagram = mPendingEmergencyDatagramsMap.entrySet();
        } else if (!mSendingDatagramInProgress && !mPendingNonEmergencyDatagramsMap.isEmpty()) {
            pendingDatagram = mPendingNonEmergencyDatagramsMap.entrySet();
        }

        if ((pendingDatagram != null) && pendingDatagram.iterator().hasNext()) {
            SendSatelliteDatagramArgument datagramArg =
                    pendingDatagram.iterator().next().getValue();
            if (mDatagramController.needsWaitingForSatelliteConnected(datagramArg.datagramType)) {
                logd("sendPendingDatagrams: wait for satellite connected");
                return;
            }

            mSendingDatagramInProgress = true;
            // Sets the trigger time for getting pending datagrams
            datagramArg.setDatagramStartTime();
            mDatagramController.updateSendStatus(datagramArg.subId, datagramArg.datagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING,
                    getPendingDatagramCount(), SatelliteManager.SATELLITE_RESULT_SUCCESS);
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
        loge("sendErrorCodeAndCleanupPendingDatagrams: cleaning up resources");

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
        logd("abortSendingPendingDatagrams()");
        sendErrorCodeAndCleanupPendingDatagrams(mPendingEmergencyDatagramsMap, errorCode);
        sendErrorCodeAndCleanupPendingDatagrams(mPendingNonEmergencyDatagramsMap, errorCode);
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

    private void reportSendDatagramCompleted(@NonNull SendSatelliteDatagramArgument argument,
            @NonNull @SatelliteManager.SatelliteResult int resultCode) {
        SatelliteStats.getInstance().onSatelliteOutgoingDatagramMetrics(
                new SatelliteStats.SatelliteOutgoingDatagramParams.Builder()
                        .setDatagramType(argument.datagramType)
                        .setResultCode(resultCode)
                        .setDatagramSizeBytes(argument.getDatagramRoundedSizeBytes())
                        /* In case pending datagram has not been attempted to send to modem
                        interface. transfer time will be 0. */
                        .setDatagramTransferTimeMillis(argument.datagramStartTime > 0
                                ? (System.currentTimeMillis() - argument.datagramStartTime) : 0)
                        .setIsDemoMode(mIsDemoMode)
                        .build());
        if (resultCode == SatelliteManager.SATELLITE_RESULT_SUCCESS) {
            mControllerMetricsStats.reportOutgoingDatagramSuccessCount(argument.datagramType,
                    mIsDemoMode);
            mSessionMetricsStats.addCountOfSuccessfulOutgoingDatagram();
        } else {
            mControllerMetricsStats.reportOutgoingDatagramFailCount(argument.datagramType,
                    mIsDemoMode);
            mSessionMetricsStats.addCountOfFailedOutgoingDatagram();
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
            if (state == SatelliteManager.SATELLITE_MODEM_STATE_OFF
                    || state == SatelliteManager.SATELLITE_MODEM_STATE_UNAVAILABLE) {
                logd("onSatelliteModemStateChanged: cleaning up resources");
                cleanUpResources();
            } else if (state == SatelliteManager.SATELLITE_MODEM_STATE_IDLE) {
                sendPendingDatagrams();
            }

            if (state == SATELLITE_MODEM_STATE_CONNECTED
                    && isDatagramWaitForConnectedStateTimerStarted()) {
                stopDatagramWaitForConnectedStateTimer();
                sendPendingDatagrams();
            }
        }
    }

    @GuardedBy("mLock")
    private void cleanUpResources() {
        logd("cleanUpResources");
        mSendingDatagramInProgress = false;
        if (getPendingDatagramCount() > 0) {
            mDatagramController.updateSendStatus(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                    mLastSendRequestDatagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                    getPendingDatagramCount(), SatelliteManager.SATELLITE_RESULT_REQUEST_ABORTED);
        }
        mDatagramController.updateSendStatus(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                mLastSendRequestDatagramType,
                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                0, SatelliteManager.SATELLITE_RESULT_SUCCESS);
        abortSendingPendingDatagrams(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                SatelliteManager.SATELLITE_RESULT_REQUEST_ABORTED);

        stopSatelliteAlignedTimer();
        stopDatagramWaitForConnectedStateTimer();
        stopWaitForDatagramSendingResponseTimer();
        stopWaitForSimulatedPollDatagramsDelayTimer();
        mIsDemoMode = false;
        mSendSatelliteDatagramRequest = null;
        mIsAligned = false;
        mLastSendRequestDatagramType = DATAGRAM_TYPE_UNKNOWN;
    }

    private void startDatagramWaitForConnectedStateTimer(
            @NonNull SendSatelliteDatagramArgument datagramArgs) {
        if (isDatagramWaitForConnectedStateTimerStarted()) {
            logd("DatagramWaitForConnectedStateTimer is already started");
            return;
        }
        sendMessageDelayed(obtainMessage(
                        EVENT_DATAGRAM_WAIT_FOR_CONNECTED_STATE_TIMED_OUT, datagramArgs),
                mDatagramController.getDatagramWaitTimeForConnectedState());
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
            logd("WaitForDatagramSendingResponseTimer was already started");
            return;
        }
        sendMessageDelayed(obtainMessage(
                EVENT_WAIT_FOR_DATAGRAM_SENDING_RESPONSE_TIMED_OUT, argument),
                mWaitTimeForDatagramSendingResponse);
    }

    private void stopWaitForDatagramSendingResponseTimer() {
        removeMessages(EVENT_WAIT_FOR_DATAGRAM_SENDING_RESPONSE_TIMED_OUT);
    }

    private void handleEventDatagramWaitForConnectedStateTimedOut(
            @NonNull SendSatelliteDatagramArgument argument) {
        logw("Timed out to wait for satellite connected before sending datagrams");
        synchronized (mLock) {
            // Update send status
            mDatagramController.updateSendStatus(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                    argument.datagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                    getPendingDatagramCount(),
                    SATELLITE_RESULT_NOT_REACHABLE);
            mDatagramController.updateSendStatus(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                    argument.datagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                    0, SatelliteManager.SATELLITE_RESULT_SUCCESS);
            abortSendingPendingDatagrams(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
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
            loge("shouldSendDatagramToModemInDemoMode: id= "
                    + R.bool.config_send_satellite_datagram_to_modem_in_demo_mode + ", ex=" + ex);
            return false;
        }
    }

    private long getWaitForDatagramSendingResponseTimeoutMillis() {
        return mContext.getResources().getInteger(
                R.integer.config_wait_for_datagram_sending_response_timeout_millis);
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
            logw("Timed out to wait for the response of the request to send the datagram "
                    + argument.datagramId);

            // Ask vendor service to abort all datagram-sending requests
            SatelliteModemInterface.getInstance().abortSendingSatelliteDatagrams(
                    obtainMessage(EVENT_ABORT_SENDING_SATELLITE_DATAGRAMS_DONE, argument));
            mSendingDatagramInProgress = false;

            // Update send status
            mDatagramController.updateSendStatus(argument.subId, argument.datagramType,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                    getPendingDatagramCount(), SATELLITE_RESULT_MODEM_TIMEOUT);
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
            abortSendingPendingDatagrams(argument.subId,
                    SatelliteManager.SATELLITE_RESULT_REQUEST_ABORTED);
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
        logd("setShouldSendDatagramToModemInDemoMode(" + (shouldSendToModemInDemoMode == null
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
            logd("startWaitForSimulatedPollDatagramsDelayTimer");
            sendMessageDelayed(
                    obtainMessage(EVENT_WAIT_FOR_SIMULATED_POLL_DATAGRAMS_DELAY_TIMED_OUT, request),
                    getDemoTimeoutDuration());
        } else {
            logd("Should not start WaitForSimulatedPollDatagramsDelayTimer in non-demo mode");
        }
    }

    private void stopWaitForSimulatedPollDatagramsDelayTimer() {
        removeMessages(EVENT_WAIT_FOR_SIMULATED_POLL_DATAGRAMS_DELAY_TIMED_OUT);
    }

    private void handleEventWaitForSimulatedPollDatagramsDelayTimedOut(
            @NonNull SendSatelliteDatagramArgument argument) {
        if (mIsDemoMode) {
            logd("handleEventWaitForSimulatedPollDatagramsDelayTimedOut");
            mDatagramController.pushDemoModeDatagram(argument.datagramType, argument.datagram);
            Consumer<Integer> internalCallback = new Consumer<Integer>() {
                @Override
                public void accept(Integer result) {
                    logd("pollPendingSatelliteDatagrams result: " + result);
                }
            };
            mDatagramController.pollPendingSatelliteDatagrams(argument.subId, internalCallback);
        } else {
            logd("Unexpected EVENT_WAIT_FOR_SIMULATED_POLL_DATAGRAMS_DELAY_TIMED_OUT in "
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
        logd("setTimeoutDatagramDelayInDemoMode " + mDemoTimeoutDuration + " reset=" + reset);
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }

    private static void logw(@NonNull String log) { Rlog.w(TAG, log); }
}

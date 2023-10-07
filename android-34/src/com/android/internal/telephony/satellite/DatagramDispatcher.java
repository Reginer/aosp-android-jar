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

import static com.android.internal.telephony.satellite.DatagramController.ROUNDING_UNIT;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.satellite.SatelliteDatagram;
import android.telephony.satellite.SatelliteManager;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.metrics.SatelliteStats;
import com.android.internal.telephony.satellite.metrics.ControllerMetricsStats;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
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

    @NonNull private static DatagramDispatcher sInstance;
    @NonNull private final Context mContext;
    @NonNull private final DatagramController mDatagramController;
    @NonNull private final ControllerMetricsStats mControllerMetricsStats;

    private boolean mIsDemoMode = false;
    private boolean mIsAligned = false;
    private DatagramDispatcherHandlerRequest mSendSatelliteDatagramRequest = null;

    private static AtomicLong mNextDatagramId = new AtomicLong(0);

    private final Object mLock = new Object();

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

        synchronized (mLock) {
            mSendingDatagramInProgress = false;
        }
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
                logd("CMD_SEND_SATELLITE_DATAGRAM");
                request = (DatagramDispatcherHandlerRequest) msg.obj;
                SendSatelliteDatagramArgument argument =
                        (SendSatelliteDatagramArgument) request.argument;
                onCompleted = obtainMessage(EVENT_SEND_SATELLITE_DATAGRAM_DONE, request);

                if (SatelliteModemInterface.getInstance().isSatelliteServiceSupported()) {
                    SatelliteModemInterface.getInstance().sendSatelliteDatagram(argument.datagram,
                            argument.datagramType == SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE,
                            argument.needFullScreenPointingUI, onCompleted);
                    break;
                }

                Phone phone = request.phone;
                if (phone != null) {
                    phone.sendSatelliteDatagram(onCompleted, argument.datagram,
                            argument.needFullScreenPointingUI);
                } else {
                    loge("sendSatelliteDatagram: No phone object");
                    synchronized (mLock) {
                        // Remove current datagram from pending map
                        if (argument.datagramType == SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE) {
                            mPendingEmergencyDatagramsMap.remove(argument.datagramId);
                        } else {
                            mPendingNonEmergencyDatagramsMap.remove(argument.datagramId);
                        }

                        // Update send status
                        mDatagramController.updateSendStatus(argument.subId,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                                getPendingDatagramCount(),
                                SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE);
                        mDatagramController.updateSendStatus(argument.subId,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                                0, SatelliteManager.SATELLITE_ERROR_NONE);

                        // report phone == null case
                        reportSendDatagramCompleted(argument,
                                SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE);
                        argument.callback.accept(
                                SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE);

                        // Abort sending all the pending datagrams
                        abortSendingPendingDatagrams(argument.subId,
                                SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE);
                    }
                }
                break;
            }

            case EVENT_SEND_SATELLITE_DATAGRAM_DONE: {
                ar = (AsyncResult) msg.obj;
                request = (DatagramDispatcherHandlerRequest) ar.userObj;
                int error = SatelliteServiceUtils.getSatelliteError(ar, "sendSatelliteDatagram");
                SendSatelliteDatagramArgument argument =
                        (SendSatelliteDatagramArgument) request.argument;

                synchronized (mLock) {
                    if (mIsDemoMode && (error == SatelliteManager.SATELLITE_ERROR_NONE)) {
                        if (argument.skipCheckingSatelliteAligned) {
                            logd("Satellite was already aligned. No need to check alignment again");
                        } else if (!mIsAligned) {
                            logd("Satellite is not aligned in demo mode, wait for the alignment.");
                            startSatelliteAlignedTimer(request);
                            break;
                        }
                    }

                    logd("EVENT_SEND_SATELLITE_DATAGRAM_DONE error: " + error);
                    // log metrics about the outgoing datagram
                    reportSendDatagramCompleted(argument, error);

                    mSendingDatagramInProgress = false;

                    // Remove current datagram from pending map.
                    if (argument.datagramType == SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE) {
                        mPendingEmergencyDatagramsMap.remove(argument.datagramId);
                    } else {
                        mPendingNonEmergencyDatagramsMap.remove(argument.datagramId);
                    }

                    if (error == SatelliteManager.SATELLITE_ERROR_NONE) {
                        // Update send status for current datagram
                        mDatagramController.updateSendStatus(argument.subId,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS,
                                getPendingDatagramCount(), error);
                        mControllerMetricsStats.reportOutgoingDatagramSuccessCount(
                                argument.datagramType);

                        if (getPendingDatagramCount() > 0) {
                            // Send response for current datagram
                            argument.callback.accept(error);
                            // Send pending datagrams
                            sendPendingDatagrams();
                        } else {
                            mDatagramController.updateSendStatus(argument.subId,
                                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                                    0, SatelliteManager.SATELLITE_ERROR_NONE);
                            // Send response for current datagram
                            argument.callback.accept(error);
                        }
                    } else {
                        // Update send status
                        mDatagramController.updateSendStatus(argument.subId,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                                getPendingDatagramCount(), error);
                        mDatagramController.updateSendStatus(argument.subId,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                                0, SatelliteManager.SATELLITE_ERROR_NONE);
                        // Send response for current datagram
                        // after updating datagram transfer state internally.
                        argument.callback.accept(error);
                        // Abort sending all the pending datagrams
                        mControllerMetricsStats.reportOutgoingDatagramFailCount(
                                argument.datagramType);
                        abortSendingPendingDatagrams(argument.subId,
                                SatelliteManager.SATELLITE_REQUEST_ABORTED);
                    }
                }
                break;
            }

            case EVENT_WAIT_FOR_DEVICE_ALIGNMENT_IN_DEMO_MODE_TIMED_OUT: {
                handleEventSatelliteAlignedTimeout((DatagramDispatcherHandlerRequest) msg.obj);
                break;
            }

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
     * @param callback The callback to get {@link SatelliteManager.SatelliteError} of the request.
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

        synchronized (mLock) {
            // Add datagram to pending datagram map
            if (datagramType == SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE) {
                mPendingEmergencyDatagramsMap.put(datagramId, datagramArgs);
            } else {
                mPendingNonEmergencyDatagramsMap.put(datagramId, datagramArgs);
            }

            // Modem can be busy receiving datagrams, so send datagram only when modem is not busy.
            if (!mSendingDatagramInProgress && mDatagramController.isPollingInIdleState()) {
                mSendingDatagramInProgress = true;
                datagramArgs.setDatagramStartTime();
                mDatagramController.updateSendStatus(subId,
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING,
                        getPendingDatagramCount(), SatelliteManager.SATELLITE_ERROR_NONE);
                sendRequestAsync(CMD_SEND_SATELLITE_DATAGRAM, datagramArgs, phone);
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
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    protected void onDeviceAlignedWithSatellite(boolean isAligned) {
        if (mIsDemoMode) {
            synchronized (mLock) {
                mIsAligned = isAligned;
                if (isAligned) handleEventSatelliteAligned();
            }
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
            }
        }
    }

    private void handleEventSatelliteAlignedTimeout(
            @NonNull DatagramDispatcherHandlerRequest request) {
        SatelliteManager.SatelliteException exception =
                new SatelliteManager.SatelliteException(
                        SatelliteManager.SATELLITE_NOT_REACHABLE);
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
            mSendingDatagramInProgress = true;
            SendSatelliteDatagramArgument datagramArg =
                    pendingDatagram.iterator().next().getValue();
            // Sets the trigger time for getting pending datagrams
            datagramArg.setDatagramStartTime();
            mDatagramController.updateSendStatus(datagramArg.subId,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING,
                    getPendingDatagramCount(), SatelliteManager.SATELLITE_ERROR_NONE);
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
            @SatelliteManager.SatelliteError int errorCode) {
        if (pendingDatagramsMap.size() == 0) {
            return;
        }
        loge("sendErrorCodeAndCleanupPendingDatagrams: cleaning up resources");

        // Send error code to all the pending datagrams
        for (Entry<Long, SendSatelliteDatagramArgument> entry :
                pendingDatagramsMap.entrySet()) {
            SendSatelliteDatagramArgument argument = entry.getValue();
            reportSendDatagramCompleted(argument, errorCode);
            mControllerMetricsStats.reportOutgoingDatagramFailCount(argument.datagramType);
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
            @SatelliteManager.SatelliteError int errorCode) {
        logd("abortSendingPendingDatagrams()");
        sendErrorCodeAndCleanupPendingDatagrams(mPendingEmergencyDatagramsMap, errorCode);
        sendErrorCodeAndCleanupPendingDatagrams(mPendingNonEmergencyDatagramsMap, errorCode);
    }

    /**
     * Return pending datagram count
     * @return pending datagram count
     */
    @GuardedBy("mLock")
    private int getPendingDatagramCount() {
        return mPendingEmergencyDatagramsMap.size() + mPendingNonEmergencyDatagramsMap.size();
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
            @NonNull @SatelliteManager.SatelliteError int resultCode) {
        SatelliteStats.getInstance().onSatelliteOutgoingDatagramMetrics(
                new SatelliteStats.SatelliteOutgoingDatagramParams.Builder()
                        .setDatagramType(argument.datagramType)
                        .setResultCode(resultCode)
                        .setDatagramSizeBytes(argument.getDatagramRoundedSizeBytes())
                        .setDatagramTransferTimeMillis(
                                System.currentTimeMillis() - argument.datagramStartTime)
                        .build());
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
        }
    }

    @GuardedBy("mLock")
    private void cleanUpResources() {
        mSendingDatagramInProgress = false;
        if (getPendingDatagramCount() > 0) {
            mDatagramController.updateSendStatus(
                    SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
                    getPendingDatagramCount(), SatelliteManager.SATELLITE_REQUEST_ABORTED);
        }
        mDatagramController.updateSendStatus(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                0, SatelliteManager.SATELLITE_ERROR_NONE);
        abortSendingPendingDatagrams(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                SatelliteManager.SATELLITE_REQUEST_ABORTED);

        stopSatelliteAlignedTimer();
        mIsDemoMode = false;
        mSendSatelliteDatagramRequest = null;
        mIsAligned = false;
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }

    private static void logw(@NonNull String log) { Rlog.w(TAG, log); }
}

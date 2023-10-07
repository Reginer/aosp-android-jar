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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.satellite.ISatelliteDatagramCallback;
import android.telephony.satellite.SatelliteDatagram;
import android.telephony.satellite.SatelliteManager;
import android.util.Pair;

import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.IIntegerConsumer;
import com.android.internal.telephony.IVoidConsumer;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.metrics.SatelliteStats;
import com.android.internal.telephony.satellite.metrics.ControllerMetricsStats;
import com.android.internal.util.FunctionalUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Datagram receiver used to receive satellite datagrams and then,
 * deliver received datagrams to messaging apps.
 */
public class DatagramReceiver extends Handler {
    private static final String TAG = "DatagramReceiver";

    private static final int CMD_POLL_PENDING_SATELLITE_DATAGRAMS = 1;
    private static final int EVENT_POLL_PENDING_SATELLITE_DATAGRAMS_DONE = 2;
    private static final int EVENT_WAIT_FOR_DEVICE_ALIGNMENT_IN_DEMO_MODE_TIMED_OUT = 3;

    /** Key used to read/write satellite datagramId in shared preferences. */
    private static final String SATELLITE_DATAGRAM_ID_KEY = "satellite_datagram_id_key";
    private static AtomicLong mNextDatagramId = new AtomicLong(0);

    @NonNull private static DatagramReceiver sInstance;
    @NonNull private final Context mContext;
    @NonNull private final ContentResolver mContentResolver;
    @NonNull private SharedPreferences mSharedPreferences = null;
    @NonNull private final DatagramController mDatagramController;
    @NonNull private final ControllerMetricsStats mControllerMetricsStats;
    @NonNull private final Looper mLooper;

    private long mDatagramTransferStartTime = 0;
    private boolean mIsDemoMode = false;
    @GuardedBy("mLock")
    private boolean mIsAligned = false;
    private DatagramReceiverHandlerRequest mPollPendingSatelliteDatagramsRequest = null;
    private final Object mLock = new Object();

    /**
     * Map key: subId, value: SatelliteDatagramListenerHandler to notify registrants.
     */
    private final ConcurrentHashMap<Integer, SatelliteDatagramListenerHandler>
            mSatelliteDatagramListenerHandlers = new ConcurrentHashMap<>();

    /**
     * Map key: DatagramId, value: pendingAckCount
     * This map is used to track number of listeners that are yet to send ack for a particular
     * datagram.
     */
    private final ConcurrentHashMap<Long, Integer>
            mPendingAckCountHashMap = new ConcurrentHashMap<>();

    /**
     * Create the DatagramReceiver singleton instance.
     * @param context The Context to use to create the DatagramReceiver.
     * @param looper The looper for the handler.
     * @param datagramController DatagramController which is used to update datagram transfer state.
     * @return The singleton instance of DatagramReceiver.
     */
    public static DatagramReceiver make(@NonNull Context context, @NonNull Looper looper,
            @NonNull DatagramController datagramController) {
        if (sInstance == null) {
            sInstance = new DatagramReceiver(context, looper, datagramController);
        }
        return sInstance;
    }

    /**
     * Create a DatagramReceiver to received satellite datagrams.
     * The received datagrams will be delivered to respective messaging apps.
     *
     * @param context The Context for the DatagramReceiver.
     * @param looper The looper for the handler.
     * @param datagramController DatagramController which is used to update datagram transfer state.
     */
    @VisibleForTesting
    protected DatagramReceiver(@NonNull Context context, @NonNull Looper looper,
            @NonNull DatagramController datagramController) {
        super(looper);
        mContext = context;
        mLooper = looper;
        mContentResolver = context.getContentResolver();
        mDatagramController = datagramController;
        mControllerMetricsStats = ControllerMetricsStats.getInstance();

        try {
            mSharedPreferences =
                    mContext.getSharedPreferences(SatelliteController.SATELLITE_SHARED_PREF,
                            Context.MODE_PRIVATE);
        } catch (Exception e) {
            loge("Cannot get default shared preferences: " + e);
        }

        if ((mSharedPreferences != null) &&
                (!mSharedPreferences.contains(SATELLITE_DATAGRAM_ID_KEY))) {
            mSharedPreferences.edit().putLong(SATELLITE_DATAGRAM_ID_KEY, mNextDatagramId.get())
                    .commit();
        }
    }

    private static final class DatagramReceiverHandlerRequest {
        /** The argument to use for the request */
        public @NonNull Object argument;
        /** The caller needs to specify the phone to be used for the request */
        public @NonNull Phone phone;
        /** The subId of the subscription used for the request. */
        public int subId;
        /** The result of the request that is run on the main thread */
        public @Nullable Object result;

        DatagramReceiverHandlerRequest(Object argument, Phone phone, int subId) {
            this.argument = argument;
            this.phone = phone;
            this.subId = subId;
        }
    }

    /**
     * Listeners are updated about incoming datagrams using a backgroundThread.
     */
    @VisibleForTesting
    public static final class SatelliteDatagramListenerHandler extends Handler {
        public static final int EVENT_SATELLITE_DATAGRAM_RECEIVED = 1;
        public static final int EVENT_RETRY_DELIVERING_RECEIVED_DATAGRAM = 2;
        public static final int EVENT_RECEIVED_ACK = 3;

        @NonNull private final ConcurrentHashMap<IBinder, ISatelliteDatagramCallback> mListeners;
        private final int mSubId;

        private static final class DatagramRetryArgument {
            public long datagramId;
            @NonNull public SatelliteDatagram datagram;
            public int pendingCount;
            @NonNull public ISatelliteDatagramCallback listener;

            DatagramRetryArgument(long datagramId, @NonNull SatelliteDatagram datagram,
                    int pendingCount, @NonNull ISatelliteDatagramCallback listener) {
                this.datagramId = datagramId;
                this.datagram = datagram;
                this.pendingCount = pendingCount;
                this.listener = listener;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                DatagramRetryArgument that = (DatagramRetryArgument) other;
                return datagramId == that.datagramId
                        && datagram.equals(that.datagram)
                        && pendingCount  == that.pendingCount
                        && listener.equals(that.listener);
            }
        }

        @VisibleForTesting
        public SatelliteDatagramListenerHandler(@NonNull Looper looper, int subId) {
            super(looper);
            mSubId = subId;
            mListeners = new ConcurrentHashMap<>();
        }

        public void addListener(@NonNull ISatelliteDatagramCallback listener) {
            mListeners.put(listener.asBinder(), listener);
        }

        public void removeListener(@NonNull ISatelliteDatagramCallback listener) {
            mListeners.remove(listener.asBinder());
        }

        public boolean hasListeners() {
            return !mListeners.isEmpty();
        }

        public int getNumOfListeners() {
            return mListeners.size();
        }

        private int getTimeoutToReceiveAck() {
            return sInstance.mContext.getResources().getInteger(
                    R.integer.config_timeout_to_receive_delivered_ack_millis);
        }

        private long getDatagramId() {
            long datagramId = 0;
            if (sInstance.mSharedPreferences == null) {
                try {
                    // Try to recreate if it is null
                    sInstance.mSharedPreferences = sInstance.mContext
                            .getSharedPreferences(SatelliteController.SATELLITE_SHARED_PREF,
                            Context.MODE_PRIVATE);
                } catch (Exception e) {
                    loge("Cannot get default shared preferences: " + e);
                }
            }

            if (sInstance.mSharedPreferences != null) {
                long prevDatagramId = sInstance.mSharedPreferences
                        .getLong(SATELLITE_DATAGRAM_ID_KEY, mNextDatagramId.get());
                datagramId = (prevDatagramId + 1) % DatagramController.MAX_DATAGRAM_ID;
                mNextDatagramId.set(datagramId);
                sInstance.mSharedPreferences.edit().putLong(SATELLITE_DATAGRAM_ID_KEY, datagramId)
                        .commit();
            } else {
                loge("Shared preferences is null - returning default datagramId");
                datagramId = mNextDatagramId.getAndUpdate(
                        n -> ((n + 1) % DatagramController.MAX_DATAGRAM_ID));
            }

            return datagramId;
        }

        private void insertDatagram(long datagramId, @NonNull SatelliteDatagram datagram) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(
                    Telephony.SatelliteDatagrams.COLUMN_UNIQUE_KEY_DATAGRAM_ID, datagramId);
            contentValues.put(
                    Telephony.SatelliteDatagrams.COLUMN_DATAGRAM, datagram.getSatelliteDatagram());
            Uri uri = sInstance.mContentResolver.insert(Telephony.SatelliteDatagrams.CONTENT_URI,
                    contentValues);
            if (uri == null) {
                loge("Cannot insert datagram with datagramId: " + datagramId);
            } else {
                logd("Inserted datagram with datagramId: " + datagramId);
            }
        }

        private void deleteDatagram(long datagramId) {
            String whereClause = (Telephony.SatelliteDatagrams.COLUMN_UNIQUE_KEY_DATAGRAM_ID
                    + "=" + datagramId);
            try (Cursor cursor = sInstance.mContentResolver.query(
                    Telephony.SatelliteDatagrams.CONTENT_URI,
                    null, whereClause, null, null)) {
                if ((cursor != null) && (cursor.getCount() == 1)) {
                    int numRowsDeleted = sInstance.mContentResolver.delete(
                            Telephony.SatelliteDatagrams.CONTENT_URI, whereClause, null);
                    if (numRowsDeleted == 0) {
                        loge("Cannot delete datagram with datagramId: " + datagramId);
                    } else {
                        logd("Deleted datagram with datagramId: " + datagramId);
                    }
                } else {
                    loge("Datagram with datagramId: " + datagramId + " is not present in DB.");
                }
            } catch(SQLException e) {
                loge("deleteDatagram SQLException e:" + e);
            }
        }

        private void onSatelliteDatagramReceived(@NonNull DatagramRetryArgument argument) {
            try {
                IVoidConsumer internalAck = new IVoidConsumer.Stub() {
                    /**
                     * This callback will be used by datagram receiver app
                     * to send ack back to Telephony. If the callback is not
                     * received within five minutes, then Telephony will
                     * resend the datagram again.
                     */
                    @Override
                    public void accept() {
                        logd("acknowledgeSatelliteDatagramReceived: "
                                + "datagramId=" + argument.datagramId);
                        sendMessage(obtainMessage(EVENT_RECEIVED_ACK, argument));
                    }
                };

                argument.listener.onSatelliteDatagramReceived(argument.datagramId,
                        argument.datagram, argument.pendingCount, internalAck);
            } catch (RemoteException e) {
                logd("EVENT_SATELLITE_DATAGRAM_RECEIVED RemoteException: " + e);
            }
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case EVENT_SATELLITE_DATAGRAM_RECEIVED: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Pair<SatelliteDatagram, Integer> result =
                            (Pair<SatelliteDatagram, Integer>) ar.result;
                    SatelliteDatagram satelliteDatagram = result.first;
                    int pendingCount = result.second;
                    logd("Received EVENT_SATELLITE_DATAGRAM_RECEIVED for subId=" + mSubId
                            + " pendingCount:" + pendingCount);

                    if (pendingCount <= 0 && satelliteDatagram == null) {
                        sInstance.mDatagramController.updateReceiveStatus(mSubId,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_NONE,
                                pendingCount, SatelliteManager.SATELLITE_ERROR_NONE);
                    } else if (satelliteDatagram != null) {
                        sInstance.mDatagramController.updateReceiveStatus(mSubId,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_SUCCESS,
                                pendingCount, SatelliteManager.SATELLITE_ERROR_NONE);

                        long datagramId = getDatagramId();
                        sInstance.mPendingAckCountHashMap.put(datagramId, getNumOfListeners());
                        insertDatagram(datagramId, satelliteDatagram);

                        mListeners.values().forEach(listener -> {
                            DatagramRetryArgument argument = new DatagramRetryArgument(datagramId,
                                    satelliteDatagram, pendingCount, listener);
                            onSatelliteDatagramReceived(argument);
                            // wait for ack and retry after the timeout specified.
                            sendMessageDelayed(
                                    obtainMessage(EVENT_RETRY_DELIVERING_RECEIVED_DATAGRAM,
                                            argument), getTimeoutToReceiveAck());
                        });

                        sInstance.mControllerMetricsStats.reportIncomingDatagramCount(
                                SatelliteManager.SATELLITE_ERROR_NONE);
                    }

                    if (pendingCount <= 0) {
                        sInstance.mDatagramController.updateReceiveStatus(mSubId,
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                                pendingCount, SatelliteManager.SATELLITE_ERROR_NONE);
                    } else {
                        // Poll for pending datagrams
                        IIntegerConsumer internalCallback = new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                logd("pollPendingSatelliteDatagram result: " + result);
                            }
                        };
                        Consumer<Integer> callback = FunctionalUtils.ignoreRemoteException(
                                internalCallback::accept);
                        sInstance.pollPendingSatelliteDatagramsInternal(mSubId, callback);
                    }

                    // Send the captured data about incoming datagram to metric
                    sInstance.reportMetrics(
                            satelliteDatagram, SatelliteManager.SATELLITE_ERROR_NONE);
                    break;
                }

                case EVENT_RETRY_DELIVERING_RECEIVED_DATAGRAM: {
                    DatagramRetryArgument argument = (DatagramRetryArgument) msg.obj;
                    logd("Received EVENT_RETRY_DELIVERING_RECEIVED_DATAGRAM datagramId:"
                            + argument.datagramId);
                    onSatelliteDatagramReceived(argument);
                    break;
                }

                case EVENT_RECEIVED_ACK: {
                    DatagramRetryArgument argument = (DatagramRetryArgument) msg.obj;
                    int pendingAckCount = sInstance.mPendingAckCountHashMap
                            .get(argument.datagramId);
                    pendingAckCount -= 1;
                    sInstance.mPendingAckCountHashMap.put(argument.datagramId, pendingAckCount);
                    logd("Received EVENT_RECEIVED_ACK datagramId:" + argument.datagramId);
                    removeMessages(EVENT_RETRY_DELIVERING_RECEIVED_DATAGRAM, argument);

                    if (pendingAckCount <= 0) {
                        // Delete datagram from DB after receiving ack from all listeners
                        deleteDatagram(argument.datagramId);
                        sInstance.mPendingAckCountHashMap.remove(argument.datagramId);
                    }
                    break;
                }

                default:
                    loge("SatelliteDatagramListenerHandler unknown event: " + msg.what);
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        DatagramReceiverHandlerRequest request;
        Message onCompleted;
        AsyncResult ar;

        switch (msg.what) {
            case CMD_POLL_PENDING_SATELLITE_DATAGRAMS: {
                request = (DatagramReceiverHandlerRequest) msg.obj;
                onCompleted =
                        obtainMessage(EVENT_POLL_PENDING_SATELLITE_DATAGRAMS_DONE, request);

                if (SatelliteModemInterface.getInstance().isSatelliteServiceSupported()) {
                    SatelliteModemInterface.getInstance()
                            .pollPendingSatelliteDatagrams(onCompleted);
                    break;
                }

                Phone phone = request.phone;
                if (phone != null) {
                    phone.pollPendingSatelliteDatagrams(onCompleted);
                } else {
                    loge("pollPendingSatelliteDatagrams: No phone object");
                    mDatagramController.updateReceiveStatus(request.subId,
                            SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED,
                            mDatagramController.getReceivePendingCount(),
                            SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE);

                    mDatagramController.updateReceiveStatus(request.subId,
                            SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                            mDatagramController.getReceivePendingCount(),
                            SatelliteManager.SATELLITE_ERROR_NONE);

                    reportMetrics(null, SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE);
                    mControllerMetricsStats.reportIncomingDatagramCount(
                                    SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE);
                    // Send response for current request
                    ((Consumer<Integer>) request.argument)
                            .accept(SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE);
                }
                break;
            }

            case EVENT_POLL_PENDING_SATELLITE_DATAGRAMS_DONE: {
                ar = (AsyncResult) msg.obj;
                request = (DatagramReceiverHandlerRequest) ar.userObj;
                int error = SatelliteServiceUtils.getSatelliteError(ar,
                        "pollPendingSatelliteDatagrams");

                if (mIsDemoMode && error == SatelliteManager.SATELLITE_ERROR_NONE) {
                    SatelliteDatagram datagram = mDatagramController.getDemoModeDatagram();
                    final int validSubId = SatelliteServiceUtils.getValidSatelliteSubId(
                            request.subId, mContext);
                    SatelliteDatagramListenerHandler listenerHandler =
                            mSatelliteDatagramListenerHandlers.get(validSubId);
                    if (listenerHandler != null) {
                        Pair<SatelliteDatagram, Integer> pair = new Pair<>(datagram, 0);
                        ar = new AsyncResult(null, pair, null);
                        Message message = listenerHandler.obtainMessage(
                                SatelliteDatagramListenerHandler.EVENT_SATELLITE_DATAGRAM_RECEIVED,
                                ar);
                        listenerHandler.sendMessage(message);
                    } else {
                        error = SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE;
                    }
                }

                logd("EVENT_POLL_PENDING_SATELLITE_DATAGRAMS_DONE error: " + error);
                if (error != SatelliteManager.SATELLITE_ERROR_NONE) {
                    mDatagramController.updateReceiveStatus(request.subId,
                            SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED,
                            mDatagramController.getReceivePendingCount(), error);

                    mDatagramController.updateReceiveStatus(request.subId,
                            SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                            mDatagramController.getReceivePendingCount(),
                            SatelliteManager.SATELLITE_ERROR_NONE);

                    reportMetrics(null, error);
                    mControllerMetricsStats.reportIncomingDatagramCount(error);
                }
                // Send response for current request
                ((Consumer<Integer>) request.argument).accept(error);
                break;
            }

            case EVENT_WAIT_FOR_DEVICE_ALIGNMENT_IN_DEMO_MODE_TIMED_OUT: {
                handleEventSatelliteAlignedTimeout((DatagramReceiverHandlerRequest) msg.obj);
                break;
            }
        }
    }

    /**
     * Register to receive incoming datagrams over satellite.
     *
     * @param subId The subId of the subscription to register for incoming satellite datagrams.
     * @param callback The callback to handle incoming datagrams over satellite.
     *
     * @return The {@link SatelliteManager.SatelliteError} result of the operation.
     */
    @SatelliteManager.SatelliteError public int registerForSatelliteDatagram(int subId,
            @NonNull ISatelliteDatagramCallback callback) {
        if (!SatelliteController.getInstance().isSatelliteSupported()) {
            return SatelliteManager.SATELLITE_NOT_SUPPORTED;
        }

        final int validSubId = SatelliteServiceUtils.getValidSatelliteSubId(subId, mContext);
        SatelliteDatagramListenerHandler satelliteDatagramListenerHandler =
                mSatelliteDatagramListenerHandlers.get(validSubId);
        if (satelliteDatagramListenerHandler == null) {
            satelliteDatagramListenerHandler = new SatelliteDatagramListenerHandler(
                    mLooper, validSubId);
            if (SatelliteModemInterface.getInstance().isSatelliteServiceSupported()) {
                SatelliteModemInterface.getInstance().registerForSatelliteDatagramsReceived(
                        satelliteDatagramListenerHandler,
                        SatelliteDatagramListenerHandler.EVENT_SATELLITE_DATAGRAM_RECEIVED, null);
            } else {
                Phone phone = SatelliteServiceUtils.getPhone();
                phone.registerForSatelliteDatagramsReceived(satelliteDatagramListenerHandler,
                        SatelliteDatagramListenerHandler.EVENT_SATELLITE_DATAGRAM_RECEIVED, null);
            }
        }

        satelliteDatagramListenerHandler.addListener(callback);
        mSatelliteDatagramListenerHandlers.put(validSubId, satelliteDatagramListenerHandler);
        return SatelliteManager.SATELLITE_ERROR_NONE;
    }

    /**
     * Unregister to stop receiving incoming datagrams over satellite.
     * If callback was not registered before, the request will be ignored.
     *
     * @param subId The subId of the subscription to unregister for incoming satellite datagrams.
     * @param callback The callback that was passed to
     *                 {@link #registerForSatelliteDatagram(int, ISatelliteDatagramCallback)}.
     */
    public void unregisterForSatelliteDatagram(int subId,
            @NonNull ISatelliteDatagramCallback callback) {
        final int validSubId = SatelliteServiceUtils.getValidSatelliteSubId(subId, mContext);
        SatelliteDatagramListenerHandler handler =
                mSatelliteDatagramListenerHandlers.get(validSubId);
        if (handler != null) {
            handler.removeListener(callback);

            if (!handler.hasListeners()) {
                mSatelliteDatagramListenerHandlers.remove(validSubId);
                if (SatelliteModemInterface.getInstance().isSatelliteServiceSupported()) {
                    SatelliteModemInterface.getInstance()
                            .unregisterForSatelliteDatagramsReceived(handler);
                } else {
                    Phone phone = SatelliteServiceUtils.getPhone();
                    if (phone != null) {
                        phone.unregisterForSatelliteDatagramsReceived(handler);
                    }
                }
            }
        }
    }

    /**
     * Poll pending satellite datagrams over satellite.
     *
     * This method requests modem to check if there are any pending datagrams to be received over
     * satellite. If there are any incoming datagrams, they will be received via
     * {@link android.telephony.satellite.SatelliteDatagramCallback
     * #onSatelliteDatagramReceived(long, SatelliteDatagram, int, Consumer)}
     *
     * @param subId The subId of the subscription used for receiving datagrams.
     * @param callback The callback to get {@link SatelliteManager.SatelliteError} of the request.
     */
    public void pollPendingSatelliteDatagrams(int subId, @NonNull Consumer<Integer> callback) {
        if (!mDatagramController.isPollingInIdleState()) {
            // Poll request should be sent to satellite modem only when it is free.
            logd("pollPendingSatelliteDatagrams: satellite modem is busy receiving datagrams.");
            callback.accept(SatelliteManager.SATELLITE_MODEM_BUSY);
            return;
        }

        pollPendingSatelliteDatagramsInternal(subId, callback);
    }

    private void pollPendingSatelliteDatagramsInternal(int subId,
            @NonNull Consumer<Integer> callback) {
        if (!mDatagramController.isSendingInIdleState()) {
            // Poll request should be sent to satellite modem only when it is free.
            logd("pollPendingSatelliteDatagrams: satellite modem is busy sending datagrams.");
            callback.accept(SatelliteManager.SATELLITE_MODEM_BUSY);
            return;
        }

        mDatagramController.updateReceiveStatus(subId,
                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING,
                mDatagramController.getReceivePendingCount(),
                SatelliteManager.SATELLITE_ERROR_NONE);
        mDatagramTransferStartTime = System.currentTimeMillis();
        Phone phone = SatelliteServiceUtils.getPhone();

        if (mIsDemoMode) {
            DatagramReceiverHandlerRequest request = new DatagramReceiverHandlerRequest(
                    callback, phone, subId);
            synchronized (mLock) {
                if (mIsAligned) {
                    Message msg = obtainMessage(EVENT_POLL_PENDING_SATELLITE_DATAGRAMS_DONE,
                            request);
                    AsyncResult.forMessage(msg, null, null);
                    msg.sendToTarget();
                } else {
                    startSatelliteAlignedTimer(request);
                }
            }
        } else {
            sendRequestAsync(CMD_POLL_PENDING_SATELLITE_DATAGRAMS, callback, phone, subId);
        }
    }

    /**
     * This function is used by {@link DatagramController} to notify {@link DatagramReceiver}
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
            }
        }
    }

    @GuardedBy("mLock")
    private void cleanupDemoModeResources() {
        if (isSatelliteAlignedTimerStarted()) {
            stopSatelliteAlignedTimer();
            if (mPollPendingSatelliteDatagramsRequest == null) {
                loge("Satellite aligned timer was started "
                        + "but mPollPendingSatelliteDatagramsRequest is null");
            } else {
                Consumer<Integer> callback =
                        (Consumer<Integer>) mPollPendingSatelliteDatagramsRequest.argument;
                callback.accept(SatelliteManager.SATELLITE_REQUEST_ABORTED);
            }
        }
        mIsDemoMode = false;
        mPollPendingSatelliteDatagramsRequest = null;
        mIsAligned = false;
    }

    @GuardedBy("mLock")
    private void cleanUpResources() {
        if (mDatagramController.isReceivingDatagrams()) {
            mDatagramController.updateReceiveStatus(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                    SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED,
                    mDatagramController.getReceivePendingCount(),
                    SatelliteManager.SATELLITE_REQUEST_ABORTED);
        }
        mDatagramController.updateReceiveStatus(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE, 0,
                SatelliteManager.SATELLITE_ERROR_NONE);
        cleanupDemoModeResources();
    }

    /**
     * Posts the specified command to be executed on the main thread and returns immediately.
     *
     * @param command command to be executed on the main thread
     * @param argument additional parameters required to perform of the operation
     * @param phone phone object used to perform the operation
     * @param subId The subId of the subscription used for the request.
     */
    private void sendRequestAsync(int command, @NonNull Object argument, @Nullable Phone phone,
            int subId) {
        DatagramReceiverHandlerRequest request = new DatagramReceiverHandlerRequest(
                argument, phone, subId);
        Message msg = this.obtainMessage(command, request);
        msg.sendToTarget();
    }

    /** Report incoming datagram related metrics */
    private void reportMetrics(@Nullable SatelliteDatagram satelliteDatagram,
            @NonNull @SatelliteManager.SatelliteError int resultCode) {
        int datagramSizeRoundedBytes = -1;
        int datagramTransferTime = 0;

        if (satelliteDatagram != null) {
            if (satelliteDatagram.getSatelliteDatagram() != null) {
                int sizeBytes = satelliteDatagram.getSatelliteDatagram().length;
                // rounded by 10 bytes
                datagramSizeRoundedBytes =
                        (int) (Math.round((double) sizeBytes / ROUNDING_UNIT) * ROUNDING_UNIT);
            }
            datagramTransferTime = (int) (System.currentTimeMillis() - mDatagramTransferStartTime);
            mDatagramTransferStartTime = 0;
        }

        SatelliteStats.getInstance().onSatelliteIncomingDatagramMetrics(
                new SatelliteStats.SatelliteIncomingDatagramParams.Builder()
                        .setResultCode(resultCode)
                        .setDatagramSizeBytes(datagramSizeRoundedBytes)
                        .setDatagramTransferTimeMillis(datagramTransferTime)
                        .build());
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

    private void startSatelliteAlignedTimer(DatagramReceiverHandlerRequest request) {
        if (isSatelliteAlignedTimerStarted()) {
            logd("Satellite aligned timer was already started");
            return;
        }
        mPollPendingSatelliteDatagramsRequest = request;
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

            if (mPollPendingSatelliteDatagramsRequest == null) {
                loge("handleSatelliteAlignedTimer: mPollPendingSatelliteDatagramsRequest is null");
            } else {
                Message message = obtainMessage(
                        EVENT_POLL_PENDING_SATELLITE_DATAGRAMS_DONE,
                        mPollPendingSatelliteDatagramsRequest);
                mPollPendingSatelliteDatagramsRequest = null;
                AsyncResult.forMessage(message, null, null);
                message.sendToTarget();
            }
        }
    }

    private void handleEventSatelliteAlignedTimeout(DatagramReceiverHandlerRequest request) {
        SatelliteManager.SatelliteException exception =
                new SatelliteManager.SatelliteException(
                        SatelliteManager.SATELLITE_NOT_REACHABLE);
        Message message = obtainMessage(EVENT_POLL_PENDING_SATELLITE_DATAGRAMS_DONE, request);
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
     * Destroys this DatagramDispatcher. Used for tearing down static resources during testing.
     */
    @VisibleForTesting
    public void destroy() {
        sInstance = null;
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }
}

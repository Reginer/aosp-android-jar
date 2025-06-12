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
import static android.telephony.satellite.SatelliteManager.DATAGRAM_TYPE_KEEP_ALIVE;
import static android.telephony.satellite.SatelliteManager.DATAGRAM_TYPE_UNKNOWN;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_IDLE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_OFF;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_SUCCESS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Looper;
import android.os.SystemProperties;
import android.telephony.PersistentLogger;
import android.telephony.Rlog;
import android.telephony.satellite.ISatelliteDatagramCallback;
import android.telephony.satellite.SatelliteDatagram;
import android.telephony.satellite.SatelliteManager;

import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.flags.FeatureFlags;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Datagram controller used for sending and receiving satellite datagrams.
 */
public class DatagramController {
    private static final String TAG = "DatagramController";

    @NonNull private static DatagramController sInstance;
    @NonNull private final Context mContext;
    @NonNull private final FeatureFlags mFeatureFlags;
    @NonNull private final PointingAppController mPointingAppController;
    @NonNull private final DatagramDispatcher mDatagramDispatcher;
    @NonNull private final DatagramReceiver mDatagramReceiver;

    public static final long MAX_DATAGRAM_ID = (long) Math.pow(2, 16);
    public static final int ROUNDING_UNIT = 10;
    public static final long SATELLITE_ALIGN_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    /** This type is used by CTS to override the satellite align timeout */
    public static final int TIMEOUT_TYPE_ALIGN = 1;
    /** This type is used by CTS to override the time to wait for connected state */
    public static final int TIMEOUT_TYPE_DATAGRAM_WAIT_FOR_CONNECTED_STATE = 2;
    /** This type is used by CTS to override the time to wait for response of the send request */
    public static final int TIMEOUT_TYPE_WAIT_FOR_DATAGRAM_SENDING_RESPONSE = 3;
    /** This type is used by CTS to override the time to datagram delay in demo mode */
    public static final int TIMEOUT_TYPE_DATAGRAM_DELAY_IN_DEMO_MODE = 4;
    /** This type is used by CTS to override wait for device alignment in demo datagram boolean */
    public static final int BOOLEAN_TYPE_WAIT_FOR_DEVICE_ALIGNMENT_IN_DEMO_DATAGRAM = 1;
    private static final String ALLOW_MOCK_MODEM_PROPERTY = "persist.radio.allow_mock_modem";
    private static final boolean DEBUG = !"user".equals(Build.TYPE);

    /** Variables used to update onSendDatagramStateChanged(). */
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private int mSendSubId;
    @GuardedBy("mLock")
    private @SatelliteManager.DatagramType int mDatagramType = DATAGRAM_TYPE_UNKNOWN;
    @GuardedBy("mLock")
    private @SatelliteManager.SatelliteDatagramTransferState int mSendDatagramTransferState =
            SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE;
    @GuardedBy("mLock")
    private int mSendPendingCount = 0;
    @GuardedBy("mLock")
    private int mSendErrorCode = SatelliteManager.SATELLITE_RESULT_SUCCESS;
    /** Variables used to update onReceiveDatagramStateChanged(). */
    @GuardedBy("mLock")
    private int mReceiveSubId;
    @GuardedBy("mLock")
    private @SatelliteManager.SatelliteDatagramTransferState int mReceiveDatagramTransferState =
            SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE;
    @GuardedBy("mLock")
    private int mReceivePendingCount = 0;
    @GuardedBy("mLock")
    private int mReceiveErrorCode = SatelliteManager.SATELLITE_RESULT_SUCCESS;
    @GuardedBy("mLock")
    private final List<SatelliteDatagram> mDemoModeDatagramList;
    private boolean mIsDemoMode = false;
    private long mAlignTimeoutDuration = SATELLITE_ALIGN_TIMEOUT;
    private long mDatagramWaitTimeForConnectedState;
    private long mModemImageSwitchingDuration;
    private boolean mWaitForDeviceAlignmentInDemoDatagram;
    private long mDatagramWaitTimeForConnectedStateForLastMessage;
    @GuardedBy("mLock")
    @SatelliteManager.SatelliteModemState
    private int mSatelltieModemState = SatelliteManager.SATELLITE_MODEM_STATE_UNKNOWN;
    @Nullable
    private PersistentLogger mPersistentLogger = null;

    /**
     * @return The singleton instance of DatagramController.
     */
    public static DatagramController getInstance() {
        if (sInstance == null) {
            loge("DatagramController was not yet initialized.");
        }
        return sInstance;
    }

    /**
     * Create the DatagramController singleton instance.
     * @param context The Context to use to create the DatagramController.
     * @param looper The looper for the handler.
     * @param featureFlags The telephony feature flags.
     * @param pointingAppController PointingAppController is used to update
     *                              PointingApp about datagram transfer state changes.
     * @return The singleton instance of DatagramController.
     */
    public static DatagramController make(@NonNull Context context, @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags,
            @NonNull PointingAppController pointingAppController) {
        if (sInstance == null) {
            sInstance = new DatagramController(
                    context, looper, featureFlags, pointingAppController);
        }
        return sInstance;
    }

    /**
     * Create a DatagramController to send and receive satellite datagrams.
     *
     * @param context The Context for the DatagramController.
     * @param looper The looper for the handler
     * @param featureFlags The telephony feature flags.
     * @param pointingAppController PointingAppController is used to update PointingApp
     *                              about datagram transfer state changes.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public DatagramController(@NonNull Context context, @NonNull Looper  looper,
            @NonNull FeatureFlags featureFlags,
            @NonNull PointingAppController pointingAppController) {
        mContext = context;
        mFeatureFlags = featureFlags;
        mPointingAppController = pointingAppController;

        // Create the DatagramDispatcher singleton,
        // which is used to send satellite datagrams.
        mDatagramDispatcher = DatagramDispatcher.make(
                mContext, looper, mFeatureFlags, this);

        // Create the DatagramReceiver singleton,
        // which is used to receive satellite datagrams.
        mDatagramReceiver = DatagramReceiver.make(
                mContext, looper, mFeatureFlags, this);

        mDatagramWaitTimeForConnectedState = getDatagramWaitForConnectedStateTimeoutMillis();
        mModemImageSwitchingDuration = getSatelliteModemImageSwitchingDurationMillis();
        mWaitForDeviceAlignmentInDemoDatagram =
                getWaitForDeviceAlignmentInDemoDatagramFromResources();
        mDatagramWaitTimeForConnectedStateForLastMessage =
                getDatagramWaitForConnectedStateForLastMessageTimeoutMillis();
        mDemoModeDatagramList = new ArrayList<>();
        mPersistentLogger = SatelliteServiceUtils.getPersistentLogger(context);
    }

    /**
     * Register to receive incoming datagrams over satellite.
     *
     * @param subId The subId of the subscription to register for incoming satellite datagrams.
     * @param callback The callback to handle incoming datagrams over satellite.
     *
     * @return The {@link SatelliteManager.SatelliteResult} result of the operation.
     */
    @SatelliteManager.SatelliteResult public int registerForSatelliteDatagram(int subId,
            @NonNull ISatelliteDatagramCallback callback) {
        return mDatagramReceiver.registerForSatelliteDatagram(subId, callback);
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
        mDatagramReceiver.unregisterForSatelliteDatagram(subId, callback);
    }

    /**
     * Poll pending satellite datagrams over satellite.
     *
     * This method requests modem to check if there are any pending datagrams to be received over
     * satellite. If there are any incoming datagrams, they will be received via
     * {@link android.telephony.satellite.SatelliteDatagramCallback#onSatelliteDatagramReceived(
     * long, SatelliteDatagram, int, Consumer)}
     *
     * @param subId The subId of the subscription used for receiving datagrams.
     * @param callback The callback to get {@link SatelliteManager.SatelliteResult} of the request.
     */
    public void pollPendingSatelliteDatagrams(int subId, @NonNull Consumer<Integer> callback) {
        plogd("pollPendingSatelliteDatagrams");
        mDatagramReceiver.pollPendingSatelliteDatagrams(subId, callback);
    }

    /**
     * Send datagram over satellite.
     *
     * Gateway encodes SOS message or location sharing message into a datagram and passes it as
     * input to this method. Datagram received here will be passed down to modem without any
     * encoding or encryption.
     *
     * When demo mode is on, save the sent datagram and this datagram will be used as a received
     * datagram.
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
        mDatagramDispatcher.sendSatelliteDatagram(subId, datagramType, datagram,
                needFullScreenPointingUI, callback);
        mPointingAppController.onSendDatagramRequested(subId, datagramType);
    }

    /**
     * Update send status to {@link PointingAppController}.
     *
     * @param subId The subId of the subscription to send satellite datagrams for
     * @param datagramTransferState The new send datagram transfer state.
     * @param sendPendingCount number of datagrams that are currently being sent
     * @param errorCode If datagram transfer failed, the reason for failure.
     */
    public void updateSendStatus(int subId, @SatelliteManager.DatagramType int datagramType,
            @SatelliteManager.SatelliteDatagramTransferState int datagramTransferState,
            int sendPendingCount, int errorCode) {
        synchronized (mLock) {
            plogd("updateSendStatus"
                    + " subId: " + subId
                    + " datagramType: " + datagramType
                    + " datagramTransferState: " + datagramTransferState
                    + " sendPendingCount: " + sendPendingCount + " errorCode: " + errorCode);
            if (shouldSuppressDatagramTransferStateUpdate(datagramType)) {
                plogd("Ignore the request to update send status");
                return;
            }

            mSendSubId = subId;
            mDatagramType = datagramType;
            mSendDatagramTransferState = datagramTransferState;
            mSendPendingCount = sendPendingCount;
            mSendErrorCode = errorCode;
            notifyDatagramTransferStateChangedToSessionController(mDatagramType);
            mPointingAppController.updateSendDatagramTransferState(mSendSubId, mDatagramType,
                    mSendDatagramTransferState, mSendPendingCount, mSendErrorCode);
            retryPollPendingDatagramsInDemoMode();
        }
    }

    private boolean shouldSuppressDatagramTransferStateUpdate(
            @SatelliteManager.DatagramType int datagramType) {
        synchronized (mLock) {
            if (!SatelliteController.getInstance().isSatelliteAttachRequired()) {
                return false;
            }
            if (datagramType == DATAGRAM_TYPE_KEEP_ALIVE
                    && mSatelltieModemState == SATELLITE_MODEM_STATE_NOT_CONNECTED) {
                return true;
            }
            return false;
        }
    }

    /**
     * Update receive status to {@link PointingAppController}.
     *
     * @param subId The subId of the subscription used to receive datagrams
     * @param datagramTransferState The new receive datagram transfer state.
     * @param receivePendingCount The number of datagrams that are currently pending to be received.
     * @param errorCode If datagram transfer failed, the reason for failure.
     */
    public void updateReceiveStatus(int subId, @SatelliteManager.DatagramType int datagramType,
            @SatelliteManager.SatelliteDatagramTransferState int datagramTransferState,
            int receivePendingCount, int errorCode) {
        synchronized (mLock) {
            plogd("updateReceiveStatus"
                    + " subId: " + subId
                    + " datagramType: " + datagramType
                    + " datagramTransferState: " + datagramTransferState
                    + " receivePendingCount: " + receivePendingCount + " errorCode: " + errorCode);

            mReceiveSubId = subId;
            mDatagramType = datagramType;
            mReceiveDatagramTransferState = datagramTransferState;
            mReceivePendingCount = receivePendingCount;
            mReceiveErrorCode = errorCode;

            notifyDatagramTransferStateChangedToSessionController(mDatagramType);
            mPointingAppController.updateReceiveDatagramTransferState(mReceiveSubId,
                    mReceiveDatagramTransferState, mReceivePendingCount, mReceiveErrorCode);
            retryPollPendingDatagramsInDemoMode();
        }

        if (isPollingInIdleState()) {
            mDatagramDispatcher.retrySendingDatagrams();
        }
    }

    /**
     * Return receive pending datagram count
     * @return receive pending datagram count.
     */
    public int getReceivePendingCount() {
        return mReceivePendingCount;
    }


    /** @return {@code true} if already sent an emergency datagram during a session. */
    public boolean isEmergencyCommunicationEstablished() {
        return mDatagramDispatcher.isEmergencyCommunicationEstablished();
    }

    /**
     * This function is used by {@link SatelliteController} to notify {@link DatagramController}
     * that satellite modem state has changed.
     *
     * @param state Current satellite modem state.
     */
    public void onSatelliteModemStateChanged(@SatelliteManager.SatelliteModemState int state) {
        synchronized (mLock) {
            mSatelltieModemState = state;
        }
        mDatagramDispatcher.onSatelliteModemStateChanged(state);
        mDatagramReceiver.onSatelliteModemStateChanged(state);
    }

    /**
     * Notify SMS received.
     *
     * @param subId The subId of the subscription used to receive SMS
     */
    public void onSmsReceived(int subId) {
        // To keep exist notification flow, need to call with each state.
        updateReceiveStatus(subId, SatelliteManager.DATAGRAM_TYPE_SMS,
                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING,
                getReceivePendingCount(), SatelliteManager.SATELLITE_RESULT_SUCCESS);
        updateReceiveStatus(subId, SatelliteManager.DATAGRAM_TYPE_SMS,
                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_SUCCESS,
                getReceivePendingCount(), SatelliteManager.SATELLITE_RESULT_SUCCESS);
        updateReceiveStatus(subId, SatelliteManager.DATAGRAM_TYPE_SMS,
                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
                getReceivePendingCount(), SatelliteManager.SATELLITE_RESULT_SUCCESS);
    }

    /**
     * Set whether the device is aligned with the satellite.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void setDeviceAlignedWithSatellite(boolean isAligned) {
        mDatagramDispatcher.setDeviceAlignedWithSatellite(isAligned);
        mDatagramReceiver.setDeviceAlignedWithSatellite(isAligned);
        if (isAligned) {
            retryPollPendingDatagramsInDemoMode();
        }
    }

    @VisibleForTesting
    public boolean isReceivingDatagrams() {
        synchronized (mLock) {
            return (mReceiveDatagramTransferState
                    == SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING);
        }
    }

    /**
     * Check if Telephony needs to wait for the modem satellite connected to a satellite network
     * before transferring datagrams via satellite.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean needsWaitingForSatelliteConnected(
            @SatelliteManager.DatagramType int datagramType) {
        synchronized (mLock) {
            if (!SatelliteController.getInstance().isSatelliteAttachRequired()) {
                return false;
            }
            if (datagramType == DATAGRAM_TYPE_KEEP_ALIVE
                    && mSatelltieModemState == SATELLITE_MODEM_STATE_NOT_CONNECTED) {
                return false;
            }
            boolean allowCheckMessageInNotConnected =
                    mContext.getResources().getBoolean(
                            R.bool.config_satellite_allow_check_message_in_not_connected);
            if (datagramType == DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS
                    && mSatelltieModemState == SATELLITE_MODEM_STATE_NOT_CONNECTED
                    && allowCheckMessageInNotConnected
                    && mFeatureFlags.carrierRoamingNbIotNtn()) {
                return false;
            }
            if (mSatelltieModemState != SATELLITE_MODEM_STATE_CONNECTED
                    && mSatelltieModemState != SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING) {
                return true;
            }
            return false;
        }
    }

    public boolean isSendingInIdleState() {
        synchronized (mLock) {
            return (mSendDatagramTransferState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE);
        }
    }

    public boolean isPollingInIdleState() {
        synchronized (mLock) {
            return (mReceiveDatagramTransferState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE);
        }
    }

    /**
     * Set variables for {@link DatagramDispatcher} and {@link DatagramReceiver} to run demo mode
     * @param isDemoMode {@code true} means demo mode is on, {@code false} otherwise.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void setDemoMode(boolean isDemoMode) {
        mIsDemoMode = isDemoMode;
        mDatagramDispatcher.setDemoMode(isDemoMode);
        mDatagramReceiver.setDemoMode(isDemoMode);

        if (!isDemoMode) {
            synchronized (mLock) {
                mDemoModeDatagramList.clear();
            }
            setDeviceAlignedWithSatellite(false);
        }
        plogd("setDemoMode: mIsDemoMode=" + mIsDemoMode);
    }

    /** Get the last sent datagram for demo mode */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public SatelliteDatagram popDemoModeDatagram() {
        if (!mIsDemoMode) {
            return null;
        }

        synchronized (mLock) {
            plogd("popDemoModeDatagram");
            return mDemoModeDatagramList.size() > 0 ? mDemoModeDatagramList.remove(0) : null;
        }
    }

    /**
     * Set last sent datagram for demo mode
     * @param datagramType datagram type, DATAGRAM_TYPE_SOS_MESSAGE,
     *                     DATAGRAM_TYPE_LAST_SOS_MESSAGE_STILL_NEED_HELP,
     *                     DATAGRAM_TYPE_LAST_SOS_MESSAGE_NO_HELP_NEEDED will be saved
     * @param datagram datagram The last datagram saved when sendSatelliteDatagramForDemo is called
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void pushDemoModeDatagram(@SatelliteManager.DatagramType int datagramType,
            SatelliteDatagram datagram) {
        if (mIsDemoMode && SatelliteServiceUtils.isSosMessage(datagramType)) {
            synchronized (mLock) {
                mDemoModeDatagramList.add(datagram);
                plogd("pushDemoModeDatagram size=" + mDemoModeDatagramList.size());
            }
        }
    }

    long getSatelliteAlignedTimeoutDuration() {
        return mAlignTimeoutDuration;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public long getDatagramWaitTimeForConnectedState(boolean isLastSosMessage) {
        synchronized (mLock) {
            long timeout = isLastSosMessage ? mDatagramWaitTimeForConnectedStateForLastMessage :
                    mDatagramWaitTimeForConnectedState;
            logd("getDatagramWaitTimeForConnectedState: isLastSosMessage=" + isLastSosMessage
                    + ", timeout=" + timeout + ", modemState=" + mSatelltieModemState);
            if (mSatelltieModemState == SATELLITE_MODEM_STATE_OFF
                    || mSatelltieModemState == SATELLITE_MODEM_STATE_IDLE) {
                return (timeout + mModemImageSwitchingDuration);
            }
            return timeout;
        }
    }

    /**
     * This API can be used by only CTS to timeout durations used by DatagramController module.
     *
     * @param timeoutMillis The timeout duration in millisecond.
     * @return {@code true} if the timeout duration is set successfully, {@code false} otherwise.
     */
    boolean setDatagramControllerTimeoutDuration(
            boolean reset, int timeoutType, long timeoutMillis) {
        if (!isMockModemAllowed()) {
            ploge("Updating timeout duration is not allowed");
            return false;
        }

        plogd("setDatagramControllerTimeoutDuration: timeoutMillis=" + timeoutMillis
                + ", reset=" + reset + ", timeoutType=" + timeoutType);
        if (timeoutType == TIMEOUT_TYPE_ALIGN) {
            if (reset) {
                mAlignTimeoutDuration = SATELLITE_ALIGN_TIMEOUT;
            } else {
                mAlignTimeoutDuration = timeoutMillis;
            }
        } else if (timeoutType == TIMEOUT_TYPE_DATAGRAM_WAIT_FOR_CONNECTED_STATE) {
            if (reset) {
                mDatagramWaitTimeForConnectedState =
                        getDatagramWaitForConnectedStateTimeoutMillis();
                mModemImageSwitchingDuration = getSatelliteModemImageSwitchingDurationMillis();
            } else {
                mDatagramWaitTimeForConnectedState = timeoutMillis;
                mModemImageSwitchingDuration = 0;
            }
        } else if (timeoutType == TIMEOUT_TYPE_WAIT_FOR_DATAGRAM_SENDING_RESPONSE) {
            mDatagramDispatcher.setWaitTimeForDatagramSendingResponse(reset, timeoutMillis);
        } else if (timeoutType == TIMEOUT_TYPE_DATAGRAM_DELAY_IN_DEMO_MODE) {
            mDatagramDispatcher.setTimeoutDatagramDelayInDemoMode(reset, timeoutMillis);
        } else {
            ploge("Invalid timeout type " + timeoutType);
            return false;
        }
        return true;
    }

    /**
     * This API can be used by only CTS to override the boolean configs used by the
     * DatagramController module.
     *
     * @param enable Whether to enable or disable boolean config.
     * @return {@code true} if the boolean config is set successfully, {@code false} otherwise.
     */
    boolean setDatagramControllerBooleanConfig(
            boolean reset, int booleanType, boolean enable) {
        if (!isMockModemAllowed()) {
            loge("Updating boolean config is not allowed");
            return false;
        }

        logd("setDatagramControllerTimeoutDuration: booleanType=" + booleanType
                + ", reset=" + reset + ", enable=" + enable);
        if (booleanType == BOOLEAN_TYPE_WAIT_FOR_DEVICE_ALIGNMENT_IN_DEMO_DATAGRAM) {
            if (reset) {
                mWaitForDeviceAlignmentInDemoDatagram =
                        getWaitForDeviceAlignmentInDemoDatagramFromResources();
            } else {
                mWaitForDeviceAlignmentInDemoDatagram = enable;
            }
        } else {
            loge("Invalid boolean type " + booleanType);
            return false;
        }
        return true;
    }

    private boolean isMockModemAllowed() {
        return (DEBUG || SystemProperties.getBoolean(ALLOW_MOCK_MODEM_PROPERTY, false));
    }

    private void notifyDatagramTransferStateChangedToSessionController(int datagramType) {
        SatelliteSessionController sessionController = SatelliteSessionController.getInstance();
        if (sessionController == null) {
            ploge("notifyDatagramTransferStateChangeToSessionController: SatelliteSessionController"
                    + " is not initialized yet");
        } else {
            synchronized (mLock) {
                sessionController.onDatagramTransferStateChanged(
                        mSendDatagramTransferState, mReceiveDatagramTransferState, datagramType);
            }
        }
    }

    private long getDatagramWaitForConnectedStateTimeoutMillis() {
        return mContext.getResources().getInteger(
                R.integer.config_datagram_wait_for_connected_state_timeout_millis);
    }

    private long getSatelliteModemImageSwitchingDurationMillis() {
        return mContext.getResources().getInteger(
                R.integer.config_satellite_modem_image_switching_duration_millis);
    }

    private long getDatagramWaitForConnectedStateForLastMessageTimeoutMillis() {
        return mContext.getResources().getInteger(
                R.integer.config_datagram_wait_for_connected_state_for_last_message_timeout_millis);
    }

    /**
     * This API can be used by only CTS to override the cached value for the device overlay config
     * value : config_send_satellite_datagram_to_modem_in_demo_mode, which determines whether
     * outgoing satellite datagrams should be sent to modem in demo mode.
     *
     * @param shouldSendToModemInDemoMode Whether send datagram in demo mode should be sent to
     * satellite modem or not.
     */
    void setShouldSendDatagramToModemInDemoMode(boolean shouldSendToModemInDemoMode) {
        mDatagramDispatcher.setShouldSendDatagramToModemInDemoMode(shouldSendToModemInDemoMode);
    }

    private void retryPollPendingDatagramsInDemoMode() {
        synchronized (mLock) {
            if (mIsDemoMode && isSendingInIdleState() && isPollingInIdleState()
                    && !mDemoModeDatagramList.isEmpty()) {
                Consumer<Integer> internalCallback = new Consumer<Integer>() {
                    @Override
                    public void accept(Integer result) {
                        if (result != SATELLITE_RESULT_SUCCESS) {
                            plogd("retryPollPendingDatagramsInDemoMode result: " + result);
                        }
                    }
                };
                pollPendingSatelliteDatagrams(
                        SatelliteController.getInstance().getSelectedSatelliteSubId(),
                        internalCallback);
            }
        }
    }

    /**
     * Get whether to wait for device alignment with satellite before sending datagrams.
     *
     * @param isAligned if the device is aligned with satellite or not
     * @return {@code true} if device is not aligned to satellite,
     * and it is required to wait for alignment else {@code false}
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean waitForAligningToSatellite(boolean isAligned) {
        if (isAligned) {
            return false;
        }

        return getWaitForDeviceAlignmentInDemoDatagram();
    }

    private boolean getWaitForDeviceAlignmentInDemoDatagram() {
        return mWaitForDeviceAlignmentInDemoDatagram;
    }

    private boolean getWaitForDeviceAlignmentInDemoDatagramFromResources() {
        boolean waitForDeviceAlignmentInDemoDatagram = false;
        try {
            waitForDeviceAlignmentInDemoDatagram = mContext.getResources().getBoolean(
                    R.bool.config_wait_for_device_alignment_in_demo_datagram);
        } catch (Resources.NotFoundException ex) {
            loge("getWaitForDeviceAlignmentInDemoDatagram: ex=" + ex);
        }

        return waitForDeviceAlignmentInDemoDatagram;
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }

    private void plogd(@NonNull String log) {
        Rlog.d(TAG, log);
        if (mPersistentLogger != null) {
            mPersistentLogger.debug(TAG, log);
        }
    }

    private void ploge(@NonNull String log) {
        Rlog.e(TAG, log);
        if (mPersistentLogger != null) {
            mPersistentLogger.error(TAG, log);
        }
    }
}

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

import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_NONE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_SUCCESS;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.satellite.ISatelliteModemStateCallback;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.stub.ISatelliteGateway;
import android.telephony.satellite.stub.SatelliteGatewayService;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.ExponentialBackoff;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This module is responsible for managing session state transition and inform listeners of modem
 * state changed events accordingly.
 */
public class SatelliteSessionController extends StateMachine {
    private static final String TAG = "SatelliteSessionController";
    private static final boolean DBG = true;
    private static final String ALLOW_MOCK_MODEM_PROPERTY = "persist.radio.allow_mock_modem";
    private static final boolean DEBUG = !"user".equals(Build.TYPE);

    /**
     * The time duration in millis that the satellite will stay at listening mode to wait for the
     * next incoming page before disabling listening mode when transitioning from sending mode.
     */
    public static final String SATELLITE_STAY_AT_LISTENING_FROM_SENDING_MILLIS =
            "satellite_stay_at_listening_from_sending_millis";
    /**
     * The default value of {@link #SATELLITE_STAY_AT_LISTENING_FROM_SENDING_MILLIS}.
     */
    public static final long DEFAULT_SATELLITE_STAY_AT_LISTENING_FROM_SENDING_MILLIS = 180000;
    /**
     * The time duration in millis that the satellite will stay at listening mode to wait for the
     * next incoming page before disabling listening mode when transitioning from receiving mode.
     */
    public static final String SATELLITE_STAY_AT_LISTENING_FROM_RECEIVING_MILLIS =
            "satellite_stay_at_listening_from_receiving_millis";
    /**
     * The default value of {@link #SATELLITE_STAY_AT_LISTENING_FROM_RECEIVING_MILLIS}
     */
    public static final long DEFAULT_SATELLITE_STAY_AT_LISTENING_FROM_RECEIVING_MILLIS = 30000;
    /**
     * The default value of {@link #SATELLITE_STAY_AT_LISTENING_FROM_SENDING_MILLIS},
     * and {@link #SATELLITE_STAY_AT_LISTENING_FROM_RECEIVING_MILLIS} for demo mode
     */
    public static final long DEMO_MODE_SATELLITE_STAY_AT_LISTENING_MILLIS = 3000;

    private static final int EVENT_DATAGRAM_TRANSFER_STATE_CHANGED = 1;
    private static final int EVENT_LISTENING_TIMER_TIMEOUT = 2;
    private static final int EVENT_SATELLITE_ENABLED_STATE_CHANGED = 3;
    private static final int EVENT_SATELLITE_MODEM_STATE_CHANGED = 4;
    private static final int EVENT_DISABLE_CELLULAR_MODEM_WHILE_SATELLITE_MODE_IS_ON_DONE = 5;
    protected static final int EVENT_NB_IOT_INACTIVITY_TIMER_TIMED_OUT = 6;
    private static final int EVENT_SATELLITE_ENABLEMENT_STARTED = 7;
    private static final int EVENT_SATELLITE_ENABLEMENT_FAILED = 8;

    private static final long REBIND_INITIAL_DELAY = 2 * 1000; // 2 seconds
    private static final long REBIND_MAXIMUM_DELAY = 64 * 1000; // 1 minute
    private static final int REBIND_MULTIPLIER = 2;
    @NonNull private final ExponentialBackoff mExponentialBackoff;
    @NonNull private final Object mLock = new Object();
    @Nullable
    private ISatelliteGateway mSatelliteGatewayService;
    private String mSatelliteGatewayServicePackageName = "";
    @Nullable private SatelliteGatewayServiceConnection mSatelliteGatewayServiceConnection;
    private boolean mIsBound;
    private boolean mIsBinding;

    @NonNull private static SatelliteSessionController sInstance;

    @NonNull private final Context mContext;
    @NonNull private final SatelliteModemInterface mSatelliteModemInterface;
    @NonNull private final UnavailableState mUnavailableState = new UnavailableState();
    @NonNull private final PowerOffState mPowerOffState = new PowerOffState();
    @NonNull private final EnablingState mEnablingState = new EnablingState();
    @NonNull private final DisablingState mDisablingState = new DisablingState();
    @NonNull private final IdleState mIdleState = new IdleState();
    @NonNull private final TransferringState mTransferringState = new TransferringState();
    @NonNull private final ListeningState mListeningState = new ListeningState();
    @NonNull private final NotConnectedState mNotConnectedState = new NotConnectedState();
    @NonNull private final ConnectedState mConnectedState = new ConnectedState();
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected AtomicBoolean mIsSendingTriggeredDuringTransferringState;
    private long mSatelliteStayAtListeningFromSendingMillis;
    private long mSatelliteStayAtListeningFromReceivingMillis;
    private long mSatelliteNbIotInactivityTimeoutMillis;
    private final ConcurrentHashMap<IBinder, ISatelliteModemStateCallback> mListeners;
    @SatelliteManager.SatelliteModemState private int mCurrentState;
    final boolean mIsSatelliteSupported;
    private boolean mIsDemoMode = false;
    @GuardedBy("mLock")
    @NonNull private boolean mIsDisableCellularModemInProgress = false;
    @NonNull private final SatelliteController mSatelliteController;
    @NonNull private final DatagramController mDatagramController;

    /**
     * @return The singleton instance of SatelliteSessionController.
     */
    public static SatelliteSessionController getInstance() {
        if (sInstance == null) {
            Log.e(TAG, "SatelliteSessionController was not yet initialized.");
        }
        return sInstance;
    }

    /**
     * Create the SatelliteSessionController singleton instance.
     *
     * @param context The Context for the SatelliteSessionController.
     * @param looper The looper associated with the handler of this class.
     * @param isSatelliteSupported Whether satellite is supported on the device.
     * @return The singleton instance of SatelliteSessionController.
     */
    public static SatelliteSessionController make(
            @NonNull Context context, @NonNull Looper looper, boolean isSatelliteSupported) {
        if (sInstance == null) {
            sInstance = new SatelliteSessionController(context, looper, isSatelliteSupported,
                    SatelliteModemInterface.getInstance());
        } else {
            if (isSatelliteSupported != sInstance.mIsSatelliteSupported) {
                Rlog.e(TAG, "New satellite support state " + isSatelliteSupported
                        + " is different from existing state " + sInstance.mIsSatelliteSupported
                        + ". Ignore the new state.");
            }
        }
        return sInstance;
    }

    /**
     * Create a SatelliteSessionController to manage satellite session.
     *
     * @param context The Context for the SatelliteSessionController.
     * @param looper The looper associated with the handler of this class.
     * @param isSatelliteSupported Whether satellite is supported on the device.
     * @param satelliteModemInterface The singleton of SatelliteModemInterface.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected SatelliteSessionController(@NonNull Context context, @NonNull Looper looper,
            boolean isSatelliteSupported,
            @NonNull SatelliteModemInterface satelliteModemInterface) {
        super(TAG, looper);

        mContext = context;
        mSatelliteModemInterface = satelliteModemInterface;
        mSatelliteController = SatelliteController.getInstance();
        mDatagramController = DatagramController.getInstance();
        mSatelliteStayAtListeningFromSendingMillis = getSatelliteStayAtListeningFromSendingMillis();
        mSatelliteStayAtListeningFromReceivingMillis =
                getSatelliteStayAtListeningFromReceivingMillis();
        mSatelliteNbIotInactivityTimeoutMillis =
                getSatelliteNbIotInactivityTimeoutMillis();
        mListeners = new ConcurrentHashMap<>();
        mIsSendingTriggeredDuringTransferringState = new AtomicBoolean(false);
        mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_UNKNOWN;
        mIsSatelliteSupported = isSatelliteSupported;
        mExponentialBackoff = new ExponentialBackoff(REBIND_INITIAL_DELAY, REBIND_MAXIMUM_DELAY,
                REBIND_MULTIPLIER, looper, () -> {
            synchronized (mLock) {
                if ((mIsBound && mSatelliteGatewayService != null) || mIsBinding) {
                    return;
                }
            }
            if (mSatelliteGatewayServiceConnection != null) {
                synchronized (mLock) {
                    mIsBound = false;
                    mIsBinding = false;
                }
                unbindService();
            }
            bindService();
        });

        addState(mUnavailableState);
        addState(mPowerOffState);
        addState(mEnablingState);
        addState(mDisablingState);
        addState(mIdleState);
        addState(mTransferringState);
        addState(mListeningState);
        addState(mNotConnectedState);
        addState(mConnectedState);
        setInitialState(isSatelliteSupported);
        start();
    }

    /**
     * {@link DatagramController} uses this function to notify {@link SatelliteSessionController}
     * that its datagram transfer state has changed.
     *
     * @param sendState The current datagram send state of {@link DatagramController}.
     * @param receiveState The current datagram receive state of {@link DatagramController}.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void onDatagramTransferStateChanged(
            @SatelliteManager.SatelliteDatagramTransferState int sendState,
            @SatelliteManager.SatelliteDatagramTransferState int receiveState) {
        sendMessage(EVENT_DATAGRAM_TRANSFER_STATE_CHANGED,
                new DatagramTransferState(sendState, receiveState));
        if (sendState == SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING) {
            mIsSendingTriggeredDuringTransferringState.set(true);
        }
    }

    /**
     * {@link SatelliteController} uses this function to notify {@link SatelliteSessionController}
     * that the satellite enabled state has changed.
     *
     * @param enabled {@code true} means enabled and {@code false} means disabled.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void onSatelliteEnabledStateChanged(boolean enabled) {
        sendMessage(EVENT_SATELLITE_ENABLED_STATE_CHANGED, enabled);
    }

    /**
     * {@link SatelliteController} uses this function to notify {@link SatelliteSessionController}
     * that the satellite enablement has just started.
     *
     * @param enabled {@code true} means being enabled and {@code false} means being disabled.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void onSatelliteEnablementStarted(boolean enabled) {
        sendMessage(EVENT_SATELLITE_ENABLEMENT_STARTED, enabled);
    }

    /**
     * {@link SatelliteController} uses this function to notify {@link SatelliteSessionController}
     * that the satellite enablement has just failed.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void onSatelliteEnablementFailed() {
        sendMessage(EVENT_SATELLITE_ENABLEMENT_FAILED);
    }

    /**
     * {@link SatelliteController} uses this function to notify {@link SatelliteSessionController}
     * that the satellite modem state has changed.
     *
     * @param state The current state of the satellite modem.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void onSatelliteModemStateChanged(@SatelliteManager.SatelliteModemState int state) {
        sendMessage(EVENT_SATELLITE_MODEM_STATE_CHANGED, state);
    }

    /**
     * Registers for modem state changed from satellite modem.
     *
     * @param callback The callback to handle the satellite modem state changed event.
     */
    public void registerForSatelliteModemStateChanged(
            @NonNull ISatelliteModemStateCallback callback) {
        try {
            callback.onSatelliteModemStateChanged(mCurrentState);
            mListeners.put(callback.asBinder(), callback);
        } catch (RemoteException ex) {
            loge("registerForSatelliteModemStateChanged: Got RemoteException ex=" + ex);
        }
    }

    /**
     * Unregisters for modem state changed from satellite modem.
     * If callback was not registered before, the request will be ignored.
     *
     * @param callback The callback that was passed to
     * {@link #registerForSatelliteModemStateChanged(ISatelliteModemStateCallback)}.
     */
    public void unregisterForSatelliteModemStateChanged(
            @NonNull ISatelliteModemStateCallback callback) {
        mListeners.remove(callback.asBinder());
    }

    /**
     * This API can be used by only CTS to update the timeout duration in milliseconds that
     * satellite should stay at listening mode to wait for the next incoming page before disabling
     * listening mode.
     *
     * @param timeoutMillis The timeout duration in millisecond.
     * @return {@code true} if the timeout duration is set successfully, {@code false} otherwise.
     */
    boolean setSatelliteListeningTimeoutDuration(long timeoutMillis) {
        if (!isMockModemAllowed()) {
            loge("Updating listening timeout duration is not allowed");
            return false;
        }

        logd("setSatelliteListeningTimeoutDuration: timeoutMillis=" + timeoutMillis);
        if (timeoutMillis == 0) {
            mSatelliteStayAtListeningFromSendingMillis =
                    getSatelliteStayAtListeningFromSendingMillis();
            mSatelliteStayAtListeningFromReceivingMillis =
                    getSatelliteStayAtListeningFromReceivingMillis();
            mSatelliteNbIotInactivityTimeoutMillis =
                    getSatelliteNbIotInactivityTimeoutMillis();
        } else {
            mSatelliteStayAtListeningFromSendingMillis = timeoutMillis;
            mSatelliteStayAtListeningFromReceivingMillis = timeoutMillis;
            mSatelliteNbIotInactivityTimeoutMillis = timeoutMillis;
        }

        return true;
    }

    /**
     * This API can be used by only CTS to update satellite gateway service package name.
     *
     * @param servicePackageName The package name of the satellite gateway service.
     * @return {@code true} if the satellite gateway service is set successfully,
     * {@code false} otherwise.
     */
    boolean setSatelliteGatewayServicePackageName(@Nullable String servicePackageName) {
        if (!isMockModemAllowed()) {
            loge("setSatelliteGatewayServicePackageName: modifying satellite gateway service "
                    + "package name is not allowed");
            return false;
        }

        logd("setSatelliteGatewayServicePackageName: config_satellite_gateway_service_package is "
                + "updated, new packageName=" + servicePackageName);

        if (servicePackageName == null || servicePackageName.equals("null")) {
            mSatelliteGatewayServicePackageName = "";
        } else {
            mSatelliteGatewayServicePackageName = servicePackageName;
        }

        if (mSatelliteGatewayServiceConnection != null) {
            synchronized (mLock) {
                mIsBound = false;
                mIsBinding = false;
            }
            unbindService();
            bindService();
        }
        return true;
    }

    /**
     * Adjusts listening timeout duration when demo mode is on
     *
     * @param isDemoMode {@code true} : The listening timeout durations will be set to
     *                   {@link #DEMO_MODE_SATELLITE_STAY_AT_LISTENING_MILLIS}
     *                   {@code false} : The listening timeout durations will be restored to
     *                   production mode
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void setDemoMode(boolean isDemoMode) {
        mIsDemoMode = isDemoMode;
    }

    /**
     * Get whether state machine is in enabling state.
     *
     * @return {@code true} if state machine is in enabling state and {@code false} otherwise.
     */
    public boolean isInEnablingState() {
        if (DBG) logd("isInEnablingState: getCurrentState=" + getCurrentState());
        return getCurrentState() == mEnablingState;
    }

    private boolean isDemoMode() {
        return mIsDemoMode;
    }

    private static class DatagramTransferState {
        @SatelliteManager.SatelliteDatagramTransferState public int sendState;
        @SatelliteManager.SatelliteDatagramTransferState public int receiveState;

        DatagramTransferState(@SatelliteManager.SatelliteDatagramTransferState int sendState,
                @SatelliteManager.SatelliteDatagramTransferState int receiveState) {
            this.sendState = sendState;
            this.receiveState = receiveState;
        }
    }

    private class UnavailableState extends State {
        @Override
        public void enter() {
            if (DBG) logd("Entering UnavailableState");
            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_UNAVAILABLE;
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_UNAVAILABLE);
        }

        @Override
        public boolean processMessage(Message msg) {
            loge("UnavailableState: receive msg " + getWhatToString(msg.what) + " unexpectedly");
            return HANDLED;
        }
    }

    private class PowerOffState extends State {
        @Override
        public void enter() {
            if (DBG) logd("Entering PowerOffState");

            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_OFF;
            mIsSendingTriggeredDuringTransferringState.set(false);
            synchronized (mLock) {
                mIsDisableCellularModemInProgress = false;
            }
            unbindService();
            stopNbIotInactivityTimer();
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_OFF);
        }

        @Override
        public void exit() {
            if (DBG) logd("Exiting PowerOffState");
            logd("Attempting to bind to SatelliteGatewayService.");
            bindService();
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("PowerOffState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_SATELLITE_ENABLEMENT_STARTED:
                    handleSatelliteEnablementStarted((boolean) msg.obj);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleSatelliteEnablementStarted(boolean enabled) {
            if (enabled) {
                transitionTo(mEnablingState);
            } else {
                logw("Unexpected satellite disablement started in PowerOff state");
            }
        }
    }

    private class EnablingState extends State {
        @Override
        public void enter() {
            if (DBG) logd("Entering EnablingState");

            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_ENABLING_SATELLITE;
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_ENABLING_SATELLITE);
        }

        @Override
        public void exit() {
            if (DBG) logd("Exiting EnablingState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("EnablingState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_SATELLITE_ENABLED_STATE_CHANGED:
                    handleSatelliteEnabledStateChanged((boolean) msg.obj);
                    break;
                case EVENT_SATELLITE_ENABLEMENT_FAILED:
                    transitionTo(mPowerOffState);
                    break;
                case EVENT_SATELLITE_MODEM_STATE_CHANGED:
                    deferMessage(msg);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleSatelliteEnabledStateChanged(boolean on) {
            if (on) {
                if (mSatelliteController.isSatelliteAttachRequired()) {
                    transitionTo(mNotConnectedState);
                } else {
                    transitionTo(mIdleState);
                }
            } else {
                /*
                 * During the state transition from ENABLING to NOT_CONNECTED, modem might be
                 * reset. In such cases, we need to remove all deferred
                 * EVENT_SATELLITE_MODEM_STATE_CHANGED events so that they will not mess up our
                 * state machine later.
                 */
                removeDeferredMessages(EVENT_SATELLITE_MODEM_STATE_CHANGED);
                transitionTo(mPowerOffState);
            }
        }
    }

    private class DisablingState extends State {
        @Override
        public void enter() {
            if (DBG) logd("Entering DisablingState");

            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_DISABLING_SATELLITE;
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_DISABLING_SATELLITE);
        }

        @Override
        public void exit() {
            if (DBG) logd("Exiting DisablingState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("DisablingState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_SATELLITE_ENABLED_STATE_CHANGED:
                    handleSatelliteEnabledStateChanged((boolean) msg.obj);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleSatelliteEnabledStateChanged(boolean on) {
            if (on) {
                logw("Unexpected power on event while disabling satellite");
            } else {
                transitionTo(mPowerOffState);
            }
        }
    }

    private class IdleState extends State {
        @Override
        public void enter() {
            if (DBG) logd("Entering IdleState");
            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_IDLE;
            mIsSendingTriggeredDuringTransferringState.set(false);
            stopNbIotInactivityTimer();
            //Enable Cellular Modem scanning
            mSatelliteModemInterface.enableCellularModemWhileSatelliteModeIsOn(true, null);
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_IDLE);
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("IdleState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_DATAGRAM_TRANSFER_STATE_CHANGED:
                    handleEventDatagramTransferStateChanged((DatagramTransferState) msg.obj);
                    break;
                case EVENT_SATELLITE_ENABLED_STATE_CHANGED:
                    handleSatelliteEnabledStateChanged(!(boolean) msg.obj, "IdleState");
                    break;
                case EVENT_DISABLE_CELLULAR_MODEM_WHILE_SATELLITE_MODE_IS_ON_DONE:
                    handleEventDisableCellularModemWhileSatelliteModeIsOnDone(
                            (AsyncResult) msg.obj);
                    break;
                case EVENT_SATELLITE_ENABLEMENT_STARTED:
                    handleSatelliteEnablementStarted((boolean) msg.obj);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleEventDatagramTransferStateChanged(
                @NonNull DatagramTransferState datagramTransferState) {
            if ((datagramTransferState.sendState == SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING)
                    || (datagramTransferState.receiveState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING)) {
                if (mSatelliteController.isSatelliteAttachRequired()) {
                    loge("Unexpected transferring state received for NB-IOT NTN");
                } else {
                    transitionTo(mTransferringState);
                }
            } else if ((datagramTransferState.sendState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT)
                    || (datagramTransferState.receiveState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT)) {
                if (mSatelliteController.isSatelliteAttachRequired()) {
                    disableCellularModemWhileSatelliteModeIsOn();
                } else {
                    loge("Unexpected transferring state received for non-NB-IOT NTN");
                }
            }
        }

        private void handleEventDisableCellularModemWhileSatelliteModeIsOnDone(
                @NonNull AsyncResult result) {
            synchronized (mLock) {
                if (mIsDisableCellularModemInProgress) {
                    int error = SatelliteServiceUtils.getSatelliteError(
                            result, "DisableCellularModemWhileSatelliteModeIsOnDone");
                    if (error == SatelliteManager.SATELLITE_RESULT_SUCCESS) {
                        transitionTo(mNotConnectedState);
                    }
                    mIsDisableCellularModemInProgress = false;
                } else {
                    loge("DisableCellularModemWhileSatelliteModeIsOn is not in progress");
                }
            }
        }

        private void disableCellularModemWhileSatelliteModeIsOn() {
            synchronized (mLock) {
                if (mIsDisableCellularModemInProgress) {
                    logd("Cellular scanning is already being disabled");
                    return;
                }

                mIsDisableCellularModemInProgress = true;
                Message onCompleted =
                        obtainMessage(EVENT_DISABLE_CELLULAR_MODEM_WHILE_SATELLITE_MODE_IS_ON_DONE);
                mSatelliteModemInterface.enableCellularModemWhileSatelliteModeIsOn(false,
                        onCompleted);
            }
        }

        @Override
        public void exit() {
            if (DBG) logd("Exiting IdleState");
            if (!mSatelliteController.isSatelliteAttachRequired()) {
                // Disable cellular modem scanning
                mSatelliteModemInterface.enableCellularModemWhileSatelliteModeIsOn(false, null);
            }
        }
    }

    private class TransferringState extends State {
        @Override
        public void enter() {
            if (DBG) logd("Entering TransferringState");
            stopNbIotInactivityTimer();
            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING;
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING);
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("TransferringState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_DATAGRAM_TRANSFER_STATE_CHANGED:
                    handleEventDatagramTransferStateChanged((DatagramTransferState) msg.obj);
                    return HANDLED;
                case EVENT_SATELLITE_ENABLED_STATE_CHANGED:
                    handleSatelliteEnabledStateChanged(!(boolean) msg.obj, "TransferringState");
                    break;
                case EVENT_SATELLITE_MODEM_STATE_CHANGED:
                    handleEventSatelliteModemStateChange(msg.arg1);
                    break;
                case EVENT_SATELLITE_ENABLEMENT_STARTED:
                    handleSatelliteEnablementStarted((boolean) msg.obj);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleEventDatagramTransferStateChanged(
                @NonNull DatagramTransferState datagramTransferState) {
            if (isSending(datagramTransferState.sendState) || isReceiving(
                    datagramTransferState.receiveState)) {
                // Stay at transferring state.
            } else {
                if (mSatelliteController.isSatelliteAttachRequired()) {
                    transitionTo(mConnectedState);
                } else {
                    if ((datagramTransferState.sendState
                            == SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED)
                            || (datagramTransferState.receiveState
                            == SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED)) {
                        transitionTo(mIdleState);
                    } else {
                        transitionTo(mListeningState);
                    }
                }
            }
        }

        private void handleEventSatelliteModemStateChange(
                @SatelliteManager.SatelliteModemState int state) {
            if (state == SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED) {
                transitionTo(mNotConnectedState);
            }
        }
    }

    private class ListeningState extends State {
        @Override
        public void enter() {
            if (DBG) logd("Entering ListeningState");

            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_LISTENING;
            long timeoutMillis = updateListeningMode(true);
            sendMessageDelayed(EVENT_LISTENING_TIMER_TIMEOUT, timeoutMillis);
            mIsSendingTriggeredDuringTransferringState.set(false);
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_LISTENING);
        }

        @Override
        public void exit() {
            if (DBG) logd("Exiting ListeningState");

            removeMessages(EVENT_LISTENING_TIMER_TIMEOUT);
            updateListeningMode(false);
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("ListeningState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_LISTENING_TIMER_TIMEOUT:
                    transitionTo(mIdleState);
                    break;
                case EVENT_DATAGRAM_TRANSFER_STATE_CHANGED:
                    handleEventDatagramTransferStateChanged((DatagramTransferState) msg.obj);
                    break;
                case EVENT_SATELLITE_ENABLED_STATE_CHANGED:
                    handleSatelliteEnabledStateChanged(!(boolean) msg.obj, "ListeningState");
                    break;
                case EVENT_SATELLITE_ENABLEMENT_STARTED:
                    handleSatelliteEnablementStarted((boolean) msg.obj);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private long updateListeningMode(boolean enabled) {
            long timeoutMillis;
            if (mIsSendingTriggeredDuringTransferringState.get()) {
                timeoutMillis = mSatelliteStayAtListeningFromSendingMillis;
            } else {
                timeoutMillis = mSatelliteStayAtListeningFromReceivingMillis;
            }
            mSatelliteModemInterface.requestSatelliteListeningEnabled(
                    enabled, (int) timeoutMillis, null);
            return timeoutMillis;
        }

        private void handleEventDatagramTransferStateChanged(
                @NonNull DatagramTransferState datagramTransferState) {
            if (datagramTransferState.sendState == SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING
                    || datagramTransferState.receiveState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING) {
                transitionTo(mTransferringState);
            }
        }
    }

    private class NotConnectedState extends State {
        @Override
        public void enter() {
            if (DBG) logd("Entering NotConnectedState");

            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED;
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED);
            startNbIotInactivityTimer();
        }

        @Override
        public void exit() {
            if (DBG) logd("Exiting NotConnectedState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("NotConnectedState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_SATELLITE_ENABLED_STATE_CHANGED:
                    handleSatelliteEnabledStateChanged(
                            !(boolean) msg.obj, "NotConnectedState");
                    break;
                case EVENT_SATELLITE_MODEM_STATE_CHANGED:
                    handleEventSatelliteModemStateChanged(msg.arg1);
                    break;
                case EVENT_NB_IOT_INACTIVITY_TIMER_TIMED_OUT:
                    transitionTo(mIdleState);
                    break;
                case EVENT_DATAGRAM_TRANSFER_STATE_CHANGED:
                    handleEventDatagramTransferStateChanged((DatagramTransferState) msg.obj);
                    break;
                case EVENT_SATELLITE_ENABLEMENT_STARTED:
                    handleSatelliteEnablementStarted((boolean) msg.obj);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleEventSatelliteModemStateChanged(
                @SatelliteManager.SatelliteModemState int state) {
            if (state == SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED) {
                transitionTo(mConnectedState);
            }
        }

        private void handleEventDatagramTransferStateChanged(
                @NonNull DatagramTransferState datagramTransferState) {
            if (datagramTransferState.sendState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT
                    || datagramTransferState.receiveState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT) {
                stopNbIotInactivityTimer();
            } else if (datagramTransferState.sendState == SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE
                    && datagramTransferState.receiveState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE) {
                startNbIotInactivityTimer();
            } else if (isSending(datagramTransferState.sendState)
                    || isReceiving(datagramTransferState.receiveState)) {
                restartNbIotInactivityTimer();
            }
        }
    }

    private class ConnectedState extends State {
        @Override
        public void enter() {
            if (DBG) logd("Entering ConnectedState");

            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED;
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED);
            startNbIotInactivityTimer();
        }

        @Override
        public void exit() {
            if (DBG) logd("Exiting ConnectedState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("ConnectedState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_SATELLITE_ENABLED_STATE_CHANGED:
                    handleSatelliteEnabledStateChanged(
                            !(boolean) msg.obj, "ConnectedState");
                    break;
                case EVENT_SATELLITE_MODEM_STATE_CHANGED:
                    handleEventSatelliteModemStateChanged(msg.arg1);
                    break;
                case EVENT_NB_IOT_INACTIVITY_TIMER_TIMED_OUT:
                    transitionTo(mIdleState);
                    break;
                case EVENT_DATAGRAM_TRANSFER_STATE_CHANGED:
                    handleEventDatagramTransferStateChanged((DatagramTransferState) msg.obj);
                    break;
                case EVENT_SATELLITE_ENABLEMENT_STARTED:
                    handleSatelliteEnablementStarted((boolean) msg.obj);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleEventSatelliteModemStateChanged(
                @SatelliteManager.SatelliteModemState int state) {
            if (state == SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED) {
                transitionTo(mNotConnectedState);
            }
        }

        private void handleEventDatagramTransferStateChanged(
                @NonNull DatagramTransferState datagramTransferState) {
            if (isSending(datagramTransferState.sendState)
                    || isReceiving(datagramTransferState.receiveState)) {
                transitionTo(mTransferringState);
            }
        }
    }

    /**
     * @return the string for msg.what
     */
    @Override
    protected String getWhatToString(int what) {
        String whatString;
        switch (what) {
            case EVENT_DATAGRAM_TRANSFER_STATE_CHANGED:
                whatString = "EVENT_DATAGRAM_TRANSFER_STATE_CHANGED";
                break;
            case EVENT_LISTENING_TIMER_TIMEOUT:
                whatString = "EVENT_LISTENING_TIMER_TIMEOUT";
                break;
            case EVENT_SATELLITE_ENABLED_STATE_CHANGED:
                whatString = "EVENT_SATELLITE_ENABLED_STATE_CHANGED";
                break;
            case EVENT_DISABLE_CELLULAR_MODEM_WHILE_SATELLITE_MODE_IS_ON_DONE:
                whatString = "EVENT_DISABLE_CELLULAR_MODEM_WHILE_SATELLITE_MODE_IS_ON_DONE";
                break;
            case EVENT_SATELLITE_MODEM_STATE_CHANGED:
                whatString = "EVENT_SATELLITE_MODEM_STATE_CHANGED";
                break;
            case EVENT_NB_IOT_INACTIVITY_TIMER_TIMED_OUT:
                whatString = "EVENT_NB_IOT_INACTIVITY_TIMER_TIMED_OUT";
                break;
            case EVENT_SATELLITE_ENABLEMENT_STARTED:
                whatString = "EVENT_SATELLITE_ENABLEMENT_STARTED";
                break;
            case EVENT_SATELLITE_ENABLEMENT_FAILED:
                whatString = "EVENT_SATELLITE_ENABLEMENT_FAILED";
                break;
            default:
                whatString = "UNKNOWN EVENT " + what;
        }
        return whatString;
    }

    private void setInitialState(boolean isSatelliteSupported) {
        if (isSatelliteSupported) {
            setInitialState(mPowerOffState);
        } else {
            setInitialState(mUnavailableState);
        }
    }

    private void notifyStateChangedEvent(@SatelliteManager.SatelliteModemState int state) {
        mDatagramController.onSatelliteModemStateChanged(state);

        List<ISatelliteModemStateCallback> toBeRemoved = new ArrayList<>();
        mListeners.values().forEach(listener -> {
            try {
                listener.onSatelliteModemStateChanged(state);
            } catch (RemoteException e) {
                logd("notifyStateChangedEvent RemoteException: " + e);
                toBeRemoved.add(listener);
            }
        });

        toBeRemoved.forEach(listener -> {
            mListeners.remove(listener.asBinder());
        });
    }

    private void handleSatelliteEnabledStateChanged(boolean off, String caller) {
        if (off) {
            transitionTo(mPowerOffState);
        } else {
            loge(caller + ": Unexpected satellite radio powered-on state changed event");
        }
    }

    private boolean isSending(@SatelliteManager.SatelliteDatagramTransferState int sendState) {
        return (sendState == SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING
                || sendState == SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS);
    }

    private boolean isReceiving(@SatelliteManager.SatelliteDatagramTransferState int receiveState) {
        return (receiveState == SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING
                || receiveState == SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_SUCCESS
                || receiveState == SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_NONE);
    }

    @NonNull
    private String getSatelliteGatewayPackageName() {
        if (!TextUtils.isEmpty(mSatelliteGatewayServicePackageName)) {
            return mSatelliteGatewayServicePackageName;
        }
        return TextUtils.emptyIfNull(mContext.getResources().getString(
                R.string.config_satellite_gateway_service_package));
    }

    private void bindService() {
        synchronized (mLock) {
            if (mIsBinding || mIsBound) return;
            mIsBinding = true;
        }
        mExponentialBackoff.start();

        String packageName = getSatelliteGatewayPackageName();
        if (TextUtils.isEmpty(packageName)) {
            loge("Unable to bind to the satellite gateway service because the package is"
                    + " undefined.");
            // Since the package name comes from static device configs, stop retry because
            // rebind will continue to fail without a valid package name.
            synchronized (mLock) {
                mIsBinding = false;
            }
            mExponentialBackoff.stop();
            return;
        }
        Intent intent = new Intent(SatelliteGatewayService.SERVICE_INTERFACE);
        intent.setPackage(packageName);

        mSatelliteGatewayServiceConnection = new SatelliteGatewayServiceConnection();
        try {
            boolean success = mContext.bindService(
                    intent, mSatelliteGatewayServiceConnection, Context.BIND_AUTO_CREATE);
            if (success) {
                logd("Successfully bound to the satellite gateway service.");
            } else {
                synchronized (mLock) {
                    mIsBinding = false;
                }
                mExponentialBackoff.notifyFailed();
                loge("Error binding to the satellite gateway service. Retrying in "
                        + mExponentialBackoff.getCurrentDelay() + " ms.");
            }
        } catch (Exception e) {
            synchronized (mLock) {
                mIsBinding = false;
            }
            mExponentialBackoff.notifyFailed();
            loge("Exception binding to the satellite gateway service. Retrying in "
                    + mExponentialBackoff.getCurrentDelay() + " ms. Exception: " + e);
        }
    }

    private void unbindService() {
        logd("unbindService");
        mExponentialBackoff.stop();
        mSatelliteGatewayService = null;
        synchronized (mLock) {
            mIsBinding = false;
            mIsBound = false;
        }
        if (mSatelliteGatewayServiceConnection != null) {
            mContext.unbindService(mSatelliteGatewayServiceConnection);
            mSatelliteGatewayServiceConnection = null;
        }
    }
    private class SatelliteGatewayServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            logd("onServiceConnected: ComponentName=" + name);
            synchronized (mLock) {
                mIsBound = true;
                mIsBinding = false;
            }
            mSatelliteGatewayService = ISatelliteGateway.Stub.asInterface(service);
            mExponentialBackoff.stop();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            loge("onServiceDisconnected: Waiting for reconnect.");
            synchronized (mLock) {
                mIsBinding = false;
                mIsBound = false;
            }
            mSatelliteGatewayService = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            loge("onBindingDied: Unbinding and rebinding service.");
            synchronized (mLock) {
                mIsBound = false;
                mIsBinding = false;
            }
            unbindService();
            mExponentialBackoff.start();
        }
    }

    private void handleSatelliteEnablementStarted(boolean enabled) {
        if (!enabled) {
            transitionTo(mDisablingState);
        }
    }

    private boolean isMockModemAllowed() {
        return (DEBUG || SystemProperties.getBoolean(ALLOW_MOCK_MODEM_PROPERTY, false));
    }

    private long getSatelliteStayAtListeningFromSendingMillis() {
        if (isDemoMode()) {
            return DEMO_MODE_SATELLITE_STAY_AT_LISTENING_MILLIS;
        } else {
            return mContext.getResources().getInteger(
                    R.integer.config_satellite_stay_at_listening_from_sending_millis);
        }
    }

    private long getSatelliteStayAtListeningFromReceivingMillis() {
        if (isDemoMode()) {
            return DEMO_MODE_SATELLITE_STAY_AT_LISTENING_MILLIS;
        } else {
            return mContext.getResources().getInteger(
                    R.integer.config_satellite_stay_at_listening_from_receiving_millis);
        }
    }

    private long getSatelliteNbIotInactivityTimeoutMillis() {
        if (isDemoMode()) {
            return mContext.getResources().getInteger(
                    R.integer.config_satellite_demo_mode_nb_iot_inactivity_timeout_millis);
        } else {
            return mContext.getResources().getInteger(
                    R.integer.config_satellite_nb_iot_inactivity_timeout_millis);
        }
    }

    private void restartNbIotInactivityTimer() {
        stopNbIotInactivityTimer();
        startNbIotInactivityTimer();
    }

    private void startNbIotInactivityTimer() {
        if (isNbIotInactivityTimerStarted()) {
            logd("NB IOT inactivity timer is already started");
            return;
        }

        DatagramController datagramController = DatagramController.getInstance();
        if (datagramController.isSendingInIdleState()
                && datagramController.isPollingInIdleState()) {
            sendMessageDelayed(
                    EVENT_NB_IOT_INACTIVITY_TIMER_TIMED_OUT,
                    mSatelliteNbIotInactivityTimeoutMillis);
        }
    }

    private void stopNbIotInactivityTimer() {
        removeMessages(EVENT_NB_IOT_INACTIVITY_TIMER_TIMED_OUT);
    }

    private boolean isNbIotInactivityTimerStarted() {
        return hasMessages(EVENT_NB_IOT_INACTIVITY_TIMER_TIMED_OUT);
    }
}

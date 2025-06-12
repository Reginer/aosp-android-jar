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

import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ROAMING_ESOS_INACTIVITY_TIMEOUT_SEC_INT;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ROAMING_P2P_SMS_INACTIVITY_TIMEOUT_SEC_INT;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ROAMING_SCREEN_OFF_INACTIVITY_TIMEOUT_SEC_INT;
import static android.telephony.satellite.SatelliteManager.DATAGRAM_TYPE_SMS;
import static android.telephony.satellite.SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE;
import static android.telephony.satellite.SatelliteManager.DATAGRAM_TYPE_UNKNOWN;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_NONE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_SUCCESS;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_UNKNOWN;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_ENABLING_SATELLITE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_IDLE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_LISTENING;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_UNKNOWN;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_REQUEST_ABORTED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_SUCCESS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Build;
import android.os.HandlerExecutor;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PersistentLogger;
import android.telephony.ServiceState;
import android.telephony.satellite.ISatelliteModemStateCallback;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.stub.ISatelliteGateway;
import android.telephony.satellite.stub.SatelliteGatewayService;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.DeviceStateMonitor;
import com.android.internal.telephony.ExponentialBackoff;
import com.android.internal.telephony.IIntegerConsumer;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.satellite.metrics.SessionMetricsStats;
import com.android.internal.telephony.util.ArrayUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.telephony.Rlog;

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
    private static final int EVENT_SCREEN_STATE_CHANGED = 9;
    protected static final int EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT = 10;
    protected static final int EVENT_ESOS_INACTIVITY_TIMER_TIMED_OUT = 11;
    private static final int EVENT_ENABLE_CELLULAR_MODEM_WHILE_SATELLITE_MODE_IS_ON_DONE = 12;
    private static final int EVENT_SERVICE_STATE_CHANGED = 13;
    protected static final int EVENT_P2P_SMS_INACTIVITY_TIMER_TIMED_OUT = 14;

    private static final long REBIND_INITIAL_DELAY = 2 * 1000; // 2 seconds
    private static final long REBIND_MAXIMUM_DELAY = 64 * 1000; // 1 minute
    private static final int REBIND_MULTIPLIER = 2;
    private static final int DEFAULT_SCREEN_OFF_INACTIVITY_TIMEOUT_SEC = 30;
    private static final int DEFAULT_P2P_SMS_INACTIVITY_TIMEOUT_SEC = 180;
    private static final int DEFAULT_ESOS_INACTIVITY_TIMEOUT_SEC = 600;
    private static final long UNDEFINED_TIMESTAMP = 0L;

    @NonNull private final ExponentialBackoff mExponentialBackoff;
    @NonNull private final Object mLock = new Object();
    @Nullable
    private ISatelliteGateway mSatelliteGatewayService;
    private String mSatelliteGatewayServicePackageName = "";
    @Nullable private SatelliteGatewayServiceConnection mSatelliteGatewayServiceConnection;
    private boolean mIsBound;
    private boolean mIsBinding;
    private boolean mIsRegisteredScreenStateChanged = false;

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
    private boolean mIgnoreCellularServiceState = false;
    private final ConcurrentHashMap<IBinder, ISatelliteModemStateCallback> mListeners;
    @SatelliteManager.SatelliteModemState private int mCurrentState;
    @SatelliteManager.SatelliteModemState private int mPreviousState;
    final boolean mIsSatelliteSupported;
    private boolean mIsDemoMode = false;
    // Interested in screen off, so use default value true
    boolean mIsScreenOn = true;
    private boolean mIsDeviceAlignedWithSatellite = false;
    private long mInactivityStartTimestamp = UNDEFINED_TIMESTAMP;
    private DatagramTransferState mLastDatagramTransferState =
            new DatagramTransferState(
                    SATELLITE_DATAGRAM_TRANSFER_STATE_UNKNOWN,
                    SATELLITE_DATAGRAM_TRANSFER_STATE_UNKNOWN,
                    DATAGRAM_TYPE_UNKNOWN);

    @NonNull private final SatelliteController mSatelliteController;
    @NonNull private final DatagramController mDatagramController;
    @Nullable private PersistentLogger mPersistentLogger = null;
    @Nullable private DeviceStateMonitor mDeviceStateMonitor;
    @NonNull private SessionMetricsStats mSessionMetricsStats;
    @NonNull private FeatureFlags mFeatureFlags;
    @NonNull private AlarmManager mAlarmManager;
    private final AlarmManager.OnAlarmListener mAlarmListener = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            plogd("onAlarm: screen off timer expired");
            sendMessage(EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT);
        }
    };

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
     * @param featureFlags The telephony feature flags.
     * @param isSatelliteSupported Whether satellite is supported on the device.
     * @return The singleton instance of SatelliteSessionController.
     */
    public static SatelliteSessionController make(
            @NonNull Context context,
            @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags,
            boolean isSatelliteSupported) {
        if (sInstance == null || isSatelliteSupported != sInstance.mIsSatelliteSupported) {
            ConcurrentHashMap<IBinder, ISatelliteModemStateCallback> existingListeners = null;
            boolean existIgnoreCellularServiceState = false;
            if (sInstance != null) {
                existingListeners = sInstance.mListeners;
                existIgnoreCellularServiceState = sInstance.mIgnoreCellularServiceState;
                sInstance.cleanUpResource();
            }

            sInstance = new SatelliteSessionController(
                    context,
                    looper,
                    featureFlags,
                    isSatelliteSupported,
                    SatelliteModemInterface.getInstance());
            if (existingListeners != null) {
                Log.d(TAG, "make() existingListeners: " + existingListeners.size());
                sInstance.mListeners.putAll(existingListeners);
            }
            if (existIgnoreCellularServiceState) {
                Log.d(TAG, "make() existIgnoreCellularServiceState is true");
                sInstance.mIgnoreCellularServiceState = true;
            }
        }
        return sInstance;
    }

    /**
     * Create a SatelliteSessionController to manage satellite session.
     *
     * @param context The Context for the SatelliteSessionController.
     * @param looper The looper associated with the handler of this class.
     * @param featureFlags The telephony feature flags.
     * @param isSatelliteSupported Whether satellite is supported on the device.
     * @param satelliteModemInterface The singleton of SatelliteModemInterface.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected SatelliteSessionController(@NonNull Context context, @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags,
            boolean isSatelliteSupported,
            @NonNull SatelliteModemInterface satelliteModemInterface) {
        super(TAG, looper);

        mPersistentLogger = SatelliteServiceUtils.getPersistentLogger(context);
        mContext = context;
        mFeatureFlags = featureFlags;
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
        mPreviousState = SATELLITE_MODEM_STATE_UNKNOWN;
        mCurrentState = SATELLITE_MODEM_STATE_UNKNOWN;
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

        Phone satellitePhone = mSatelliteController.getSatellitePhone();
        if (satellitePhone == null) {
            satellitePhone = SatelliteServiceUtils.getPhone();
        }
        mDeviceStateMonitor = satellitePhone.getDeviceStateMonitor();
        mSessionMetricsStats = SessionMetricsStats.getInstance();
        mAlarmManager = mContext.getSystemService(AlarmManager.class);

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
            @SatelliteManager.SatelliteDatagramTransferState int receiveState,
            @SatelliteManager.DatagramType int datagramType) {
        DatagramTransferState datagramTransferState =
                new DatagramTransferState(sendState, receiveState, datagramType);
        mLastDatagramTransferState = datagramTransferState;

        sendMessage(EVENT_DATAGRAM_TRANSFER_STATE_CHANGED, datagramTransferState);

        if (sendState == SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING) {
            mIsSendingTriggeredDuringTransferringState.set(true);
        }

        if (datagramTransferState.isIdle()) {
            checkForInactivity();
        } else {
            endUserInactivity();
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
     *
     * @param enabled {@code true} if satellite is enabled, {@code false} satellite is disabled.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void onSatelliteEnablementFailed(boolean enabled) {
        sendMessage(EVENT_SATELLITE_ENABLEMENT_FAILED, enabled);
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
     * {@link SatelliteController} uses this function to notify {@link SatelliteSessionController}
     * that the satellite emergency mode has changed.
     *
     * @param isEmergencyMode The satellite emergency mode.
     */
    public void onEmergencyModeChanged(boolean isEmergencyMode) {
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) {
            plogd("onEmergencyModeChanged: carrierRoamingNbIotNtn is disabled");
            return;
        }

        plogd("onEmergencyModeChanged " + isEmergencyMode);

        List<ISatelliteModemStateCallback> toBeRemoved = new ArrayList<>();
        mListeners.values().forEach(listener -> {
            try {
                listener.onEmergencyModeChanged(isEmergencyMode);
            } catch (RemoteException e) {
                plogd("onEmergencyModeChanged RemoteException: " + e);
                toBeRemoved.add(listener);
            }
        });

        toBeRemoved.forEach(listener -> {
            mListeners.remove(listener.asBinder());
        });
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
            if (mFeatureFlags.carrierRoamingNbIotNtn()) {
                callback.onEmergencyModeChanged(mSatelliteController.getRequestIsEmergency());
            }
            mListeners.put(callback.asBinder(), callback);
        } catch (RemoteException ex) {
            ploge("registerForSatelliteModemStateChanged: Got RemoteException ex=" + ex);
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
            ploge("Updating listening timeout duration is not allowed");
            return false;
        }

        plogd("setSatelliteListeningTimeoutDuration: timeoutMillis=" + timeoutMillis);
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
     * This API can be used by only CTS to control ingoring cellular service state event.
     *
     * @param enabled Whether to enable boolean config.
     * @return {@code true} if the value is set successfully, {@code false} otherwise.
     */
    public boolean setSatelliteIgnoreCellularServiceState(boolean enabled) {
        plogd("setSatelliteIgnoreCellularServiceState : "
                + "old = " + mIgnoreCellularServiceState + " new : " + enabled);
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) {
            return false;
        }

        mIgnoreCellularServiceState = enabled;
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
            ploge("setSatelliteGatewayServicePackageName: modifying satellite gateway service "
                    + "package name is not allowed");
            return false;
        }

        plogd("setSatelliteGatewayServicePackageName: config_satellite_gateway_service_package is "
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
     * Notify whether the device is aligned with the satellite
     *
     * @param isAligned {@code true} Device is aligned with the satellite,
     *                  {@code false} otherwise.
     */
    public void setDeviceAlignedWithSatellite(boolean isAligned) {
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) {
            plogd("setDeviceAlignedWithSatellite: carrierRoamingNbIotNtn is disabled");
            return;
        }

        mIsDeviceAlignedWithSatellite = isAligned;
        plogd("setDeviceAlignedWithSatellite: isAligned " +  isAligned);

        if (mIsDeviceAlignedWithSatellite) {
            stopEsosInactivityTimer();
            stopP2pSmsInactivityTimer();
            endUserInactivity();
        } else {
            if (mCurrentState == SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED
                    || mCurrentState == SATELLITE_MODEM_STATE_CONNECTED
                    || mCurrentState == SATELLITE_MODEM_STATE_IDLE) {
                evaluateStartingEsosInactivityTimer();
                evaluateStartingP2pSmsInactivityTimer();
            }
            checkForInactivity();
        }
    }

    /**
     * Get whether state machine is in enabling state.
     *
     * @return {@code true} if state machine is in enabling state and {@code false} otherwise.
     */
    public boolean isInEnablingState() {
        if (DBG) plogd("isInEnablingState: getCurrentState=" + getCurrentState());
        return getCurrentState() == mEnablingState;
    }

    /**
     * Get whether state machine is in disabling state.
     *
     * @return {@code true} if state machine is in disabling state and {@code false} otherwise.
     */
    public boolean isInDisablingState() {
        if (DBG) plogd("isInDisablingState: getCurrentState=" + getCurrentState());
        return getCurrentState() == mDisablingState;
    }

    /**
     * Release all resource.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void cleanUpResource() {
        plogd("cleanUpResource");
        mIsDeviceAlignedWithSatellite = false;
        mInactivityStartTimestamp = UNDEFINED_TIMESTAMP;
        unregisterForScreenStateChanged();
        if (mAlarmManager != null) {
            mAlarmManager.cancel(mAlarmListener);
        }

        if (mFeatureFlags.carrierRoamingNbIotNtn()) {
            // Register to received Cellular service state
            for (Phone phone : PhoneFactory.getPhones()) {
                if (phone == null) continue;

                phone.unregisterForServiceStateChanged(getHandler());
                if (DBG) {
                    plogd("cleanUpResource: unregisterForServiceStateChanged phoneId "
                            + phone.getPhoneId());
                }
            }
        }

        quitNow();
    }

    /**
     * Uses this function to notify that cellular service state has changed
     *
     * @param serviceState The state of the cellular service.
     */
    @VisibleForTesting
    public void onCellularServiceStateChanged(ServiceState serviceState) {
        sendMessage(EVENT_SERVICE_STATE_CHANGED, new AsyncResult(null, serviceState, null));
    }

    /**
     * Uses this function to set AlarmManager object for testing.
     *
     * @param alarmManager The instance of AlarmManager.
     */
    @VisibleForTesting
    public void setAlarmManager(AlarmManager alarmManager) {
        mAlarmManager = alarmManager;
    }

    private boolean isDemoMode() {
        return mIsDemoMode;
    }

    private static class DatagramTransferState {
        @SatelliteManager.SatelliteDatagramTransferState public int sendState;
        @SatelliteManager.SatelliteDatagramTransferState public int receiveState;
        @SatelliteManager.DatagramType public int datagramType;

        DatagramTransferState(@SatelliteManager.SatelliteDatagramTransferState int sendState,
                @SatelliteManager.SatelliteDatagramTransferState int receiveState,
                @SatelliteManager.DatagramType int datagramType) {
            this.sendState = sendState;
            this.receiveState = receiveState;
            this.datagramType = datagramType;
        }

        public boolean isIdle() {
            return (sendState == SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE
                    && receiveState == SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE);
        }
    }

    private class UnavailableState extends State {
        @Override
        public void enter() {
            if (DBG) plogd("Entering UnavailableState");
            mPreviousState = mCurrentState;
            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_UNAVAILABLE;
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_UNAVAILABLE);
        }

        @Override
        public boolean processMessage(Message msg) {
            ploge("UnavailableState: receive msg " + getWhatToString(msg.what) + " unexpectedly");
            return HANDLED;
        }
    }

    private class PowerOffState extends State {
        @Override
        public void enter() {
            if (DBG) plogd("Entering PowerOffState");

            mSatelliteController.moveSatelliteToOffStateAndCleanUpResources(
                    SATELLITE_RESULT_REQUEST_ABORTED);
            mPreviousState = mCurrentState;
            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_OFF;
            mIsSendingTriggeredDuringTransferringState.set(false);
            unbindService();
            stopNbIotInactivityTimer();
            stopEsosInactivityTimer();
            stopP2pSmsInactivityTimer();
            endUserInactivity();
            DemoSimulator.getInstance().onSatelliteModeOff();
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_OFF);

            unregisterForScreenStateChanged();
        }

        @Override
        public void exit() {
            if (DBG) plogd("Exiting PowerOffState");
            plogd("Attempting to bind to SatelliteGatewayService.");
            bindService();
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) plogd("PowerOffState: processing " + getWhatToString(msg.what));
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
                plogw("Unexpected satellite disablement started in PowerOff state");
            }
        }
    }

    private class EnablingState extends State {
        @Override
        public void enter() {
            if (DBG) plogd("Entering EnablingState");

            mPreviousState = mCurrentState;
            mCurrentState = SATELLITE_MODEM_STATE_ENABLING_SATELLITE;
            notifyStateChangedEvent(SATELLITE_MODEM_STATE_ENABLING_SATELLITE);
        }

        @Override
        public void exit() {
            if (DBG) plogd("Exiting EnablingState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) plogd("EnablingState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_SATELLITE_ENABLED_STATE_CHANGED:
                    handleSatelliteEnabledStateChanged((boolean) msg.obj);
                    break;
                case EVENT_SATELLITE_ENABLEMENT_FAILED:
                    if ((boolean) msg.obj) {
                        transitionTo(mPowerOffState);
                    } else {
                        ploge("Unexpected failed disable event in EnablingState");
                    }
                    break;
                case EVENT_SATELLITE_MODEM_STATE_CHANGED:
                    handleSatelliteModemStateChanged(msg);
                    break;
                case EVENT_SATELLITE_ENABLEMENT_STARTED:
                    handleSatelliteEnablementStarted((boolean) msg.obj);
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
                DemoSimulator.getInstance().onSatelliteModeOn();

                registerForScreenStateChanged();
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

        private void handleSatelliteModemStateChanged(@NonNull Message msg) {
            int state = msg.arg1;
            if (state == SatelliteManager.SATELLITE_MODEM_STATE_OFF) {
                transitionTo(mPowerOffState);
            } else {
                deferMessage(msg);
            }
        }
    }

    private class DisablingState extends State {
        @Override
        public void enter() {
            if (DBG) plogd("Entering DisablingState");

            mPreviousState = mCurrentState;
            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_DISABLING_SATELLITE;
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_DISABLING_SATELLITE);

            unregisterForScreenStateChanged();
        }

        @Override
        public void exit() {
            if (DBG) plogd("Exiting DisablingState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) plogd("DisablingState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_SATELLITE_ENABLED_STATE_CHANGED:
                    handleSatelliteEnabledStateChanged((boolean) msg.obj);
                    break;
                case EVENT_SATELLITE_ENABLEMENT_FAILED:
                    boolean enablingSatellite = (boolean) msg.obj;
                    if (enablingSatellite) {
                        /* Framework received a disable request when the enable request was in
                         * progress. We need to set the previous state to OFF since the enable has
                         * failed so that when disable fail, we can move to OFF state properly.
                         */
                        mPreviousState = SatelliteManager.SATELLITE_MODEM_STATE_OFF;
                        plogd("Enable request has failed. Set mPreviousState to OFF");
                        break;
                    }
                    if (mPreviousState == SATELLITE_MODEM_STATE_CONNECTED
                            || mPreviousState == SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING
                            || mPreviousState == SATELLITE_MODEM_STATE_LISTENING) {
                        transitionTo(mConnectedState);
                    } else if (mPreviousState == SATELLITE_MODEM_STATE_ENABLING_SATELLITE) {
                        transitionTo(mEnablingState);
                    } else if (mPreviousState == SatelliteManager.SATELLITE_MODEM_STATE_OFF) {
                        transitionTo(mPowerOffState);
                    } else {
                        transitionTo(mNotConnectedState);
                    }
                    break;
                case EVENT_SATELLITE_MODEM_STATE_CHANGED:
                    handleEventSatelliteModemStateChanged(msg);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleSatelliteEnabledStateChanged(boolean on) {
            if (on) {
                /* Framework received a disable request when the enable request was in progress.
                 * We need to set the previous state to NOT_CONNECTED since the enable has
                 * succeeded so that when disable fail, we can move to NOT_CONNECTED state properly.
                 */
                mPreviousState = SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED;
                plogd("Enable request has succeeded. Set mPreviousState to NOT_CONNECTED");
            } else {
                transitionTo(mPowerOffState);
            }
        }

        private void handleEventSatelliteModemStateChanged(@NonNull Message msg) {
            int state = msg.arg1;
            if (state == SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED) {
                mPreviousState = state;
            } else if (state == SatelliteManager.SATELLITE_MODEM_STATE_OFF) {
                plogd("Modem OFF state will be processed after getting the confirmation of the"
                        + " disable request");
                deferMessage(msg);
            }
        }
    }

    private class IdleState extends State {
        @Override
        public void enter() {
            if (DBG) plogd("Entering IdleState");
            mPreviousState = mCurrentState;
            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_IDLE;
            mIsSendingTriggeredDuringTransferringState.set(false);
            stopNbIotInactivityTimer();

            //Enable Cellular Modem scanning
            boolean configSatelliteAllowTnScanningDuringSatelliteSession =
                    isTnScanningAllowedDuringSatelliteSession();
            if (configSatelliteAllowTnScanningDuringSatelliteSession) {
                Message onCompleted =
                    obtainMessage(EVENT_ENABLE_CELLULAR_MODEM_WHILE_SATELLITE_MODE_IS_ON_DONE);
                mSatelliteModemInterface
                    .enableCellularModemWhileSatelliteModeIsOn(true, onCompleted);
            } else {
                plogd("Device does not allow cellular modem scanning");
            }
            if (isConcurrentTnScanningSupported()
                    || !configSatelliteAllowTnScanningDuringSatelliteSession) {
                plogd("IDLE state is hidden from clients");
            } else {
                notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_IDLE);
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) plogd("IdleState: processing " + getWhatToString(msg.what));
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
                case EVENT_SCREEN_STATE_CHANGED:
                    handleEventScreenStateChanged((AsyncResult) msg.obj);
                    break;
                case EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT:
                    handleEventScreenOffInactivityTimerTimedOut();
                    break;
                case EVENT_SATELLITE_MODEM_STATE_CHANGED:
                    handleSatelliteModemStateChanged(msg);
                    break;
                case EVENT_ENABLE_CELLULAR_MODEM_WHILE_SATELLITE_MODE_IS_ON_DONE:
                    plogd("EVENT_ENABLE_CELLULAR_MODEM_WHILE_SATELLITE_MODE_IS_ON_DONE");
                    break;
                case EVENT_SERVICE_STATE_CHANGED:
                    if (!mIgnoreCellularServiceState) {
                        AsyncResult ar = (msg.obj != null) ? (AsyncResult) msg.obj : null;
                        if (ar == null || ar.result == null) {
                            plogd("IdleState: processing: can't access ServiceState");
                        } else {
                            ServiceState newServiceState = (ServiceState) ar.result;
                            handleEventServiceStateChanged(newServiceState);
                        }
                    } else {
                        plogd("IdleState: processing: ignore EVENT_SERVICE_STATE_CHANGED");
                    }
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
                transitionTo(mTransferringState);
            } else if ((datagramTransferState.sendState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT)
                    || (datagramTransferState.receiveState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT)) {
                if (mSatelliteController.isSatelliteAttachRequired()) {
                    transitionTo(mNotConnectedState);
                } else {
                    ploge("Unexpected transferring state received for non-NB-IOT NTN");
                }
            }
        }

        private void handleEventServiceStateChanged(ServiceState serviceState) {
            boolean isInServiceOrEmergency =
                    serviceState.getVoiceRegState() == ServiceState.STATE_IN_SERVICE
                    || serviceState.getDataRegState() == ServiceState.STATE_IN_SERVICE
                    || serviceState.isEmergencyOnly();
            if (!isInServiceOrEmergency) {
                plogd("handleEventServiceStateChanged: is not IN_SERVICE or EMERGENCY_ONLY");
                return;
            }

            // In emergency
            boolean isEmergency = mSatelliteController.getRequestIsEmergency();
            if (isEmergency) {
                boolean isEmergencyCommunicationEstablished = (mDatagramController == null)
                        ? false : mDatagramController.isEmergencyCommunicationEstablished();
                boolean isTurnOffAllowed =
                        mSatelliteController.turnOffSatelliteSessionForEmergencyCall(getSubId());
                if (isEmergencyCommunicationEstablished || !isTurnOffAllowed) {
                    logd("handleEventServiceStateChanged: "
                            + "can't disable emergency satellite session");
                    return;
                }
            }

            mSatelliteController.requestSatelliteEnabled(
                    false /*enableSatellite*/,
                    false /*enableDemoMode*/,
                    isEmergency /*isEmergency*/,
                    new IIntegerConsumer.Stub() {
                        @Override
                        public void accept(int result) {
                            plogd("requestSatelliteEnabled result=" + result);
                            if (result == SatelliteManager.SATELLITE_RESULT_SUCCESS) {
                                mSessionMetricsStats.addCountOfAutoExitDueToTnNetwork();
                            }
                        }
                    });
        }

        private void handleEventDisableCellularModemWhileSatelliteModeIsOnDone(
                @NonNull AsyncResult result) {
            int error = SatelliteServiceUtils.getSatelliteError(
                        result, "DisableCellularModemWhileSatelliteModeIsOnDone");
            plogd("Disable TN scanning done with result: " + error);
        }

        private void handleSatelliteModemStateChanged(@NonNull Message msg) {
            int state = msg.arg1;
            if (state == SatelliteManager.SATELLITE_MODEM_STATE_OFF) {
                transitionTo(mPowerOffState);
            } else if (state == SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED
                           || state == SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED) {
                if (isConcurrentTnScanningSupported()) {
                    plogd("Notifying the new state " + state + " to clients but still"
                            + " stay at IDLE state internally");
                    notifyStateChangedEvent(state);
                } else {
                    plogd("Ignoring the modem state " + state);
                }
            }
        }

        @Override
        public void exit() {
            if (DBG) plogd("Exiting IdleState");
            // Disable cellular modem scanning
            mSatelliteModemInterface.enableCellularModemWhileSatelliteModeIsOn(false, null);
        }
    }

    private class TransferringState extends State {
        @Override
        public void enter() {
            if (DBG) plogd("Entering TransferringState");
            stopNbIotInactivityTimer();
            stopEsosInactivityTimer();
            stopP2pSmsInactivityTimer();

            mPreviousState = mCurrentState;
            mCurrentState = SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING;
            notifyStateChangedEvent(SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING);
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) plogd("TransferringState: processing " + getWhatToString(msg.what));
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
                case EVENT_SCREEN_STATE_CHANGED:
                    handleEventScreenStateChanged((AsyncResult) msg.obj);
                    break;
                case EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT:
                    handleEventScreenOffInactivityTimerTimedOut();
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
            } else if (state == SatelliteManager.SATELLITE_MODEM_STATE_OFF) {
                transitionTo(mPowerOffState);
            }
        }
    }

    private class ListeningState extends State {
        @Override
        public void enter() {
            if (DBG) plogd("Entering ListeningState");

            mPreviousState = mCurrentState;
            mCurrentState = SATELLITE_MODEM_STATE_LISTENING;
            long timeoutMillis = updateListeningMode(true);
            sendMessageDelayed(EVENT_LISTENING_TIMER_TIMEOUT, timeoutMillis);
            mIsSendingTriggeredDuringTransferringState.set(false);
            notifyStateChangedEvent(SATELLITE_MODEM_STATE_LISTENING);
        }

        @Override
        public void exit() {
            if (DBG) plogd("Exiting ListeningState");

            removeMessages(EVENT_LISTENING_TIMER_TIMEOUT);
            updateListeningMode(false);
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) plogd("ListeningState: processing " + getWhatToString(msg.what));
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
                case EVENT_SCREEN_STATE_CHANGED:
                    handleEventScreenStateChanged((AsyncResult) msg.obj);
                    break;
                case EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT:
                    handleEventScreenOffInactivityTimerTimedOut();
                    break;
                case EVENT_SATELLITE_MODEM_STATE_CHANGED:
                    handleEventSatelliteModemStateChange(msg);
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

        private void handleEventSatelliteModemStateChange(@NonNull Message msg) {
            int state = msg.arg1;
            if (state == SatelliteManager.SATELLITE_MODEM_STATE_OFF) {
                transitionTo(mPowerOffState);
            }
        }
    }

    private class NotConnectedState extends State {
        @Override
        public void enter() {
            if (DBG) plogd("Entering NotConnectedState");

            mPreviousState = mCurrentState;
            mCurrentState = SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED;
            notifyStateChangedEvent(SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED);
            startNbIotInactivityTimer();
            evaluateStartingEsosInactivityTimer();
            evaluateStartingP2pSmsInactivityTimer();
        }

        @Override
        public void exit() {
            if (DBG) plogd("Exiting NotConnectedState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) plogd("NotConnectedState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_SATELLITE_ENABLED_STATE_CHANGED:
                    handleSatelliteEnabledStateChanged(
                            !(boolean) msg.obj, "NotConnectedState");
                    break;
                case EVENT_SATELLITE_MODEM_STATE_CHANGED:
                    handleEventSatelliteModemStateChanged(msg.arg1);
                    break;
                case EVENT_ESOS_INACTIVITY_TIMER_TIMED_OUT:
                    if (isP2pSmsInActivityTimerStarted()) {
                        plogd("NotConnectedState: processing: P2P_SMS inactivity timer running "
                                + "can not move to IDLE");
                    } else {
                        transitionTo(mIdleState);
                    }
                    break;
                case EVENT_P2P_SMS_INACTIVITY_TIMER_TIMED_OUT:
                    handleEventP2pSmsInactivityTimerTimedOut();
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
                case EVENT_SCREEN_STATE_CHANGED:
                    handleEventScreenStateChanged((AsyncResult) msg.obj);
                    break;
                case EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT:
                    handleEventScreenOffInactivityTimerTimedOut();
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleEventSatelliteModemStateChanged(
                @SatelliteManager.SatelliteModemState int state) {
            if (state == SATELLITE_MODEM_STATE_CONNECTED) {
                transitionTo(mConnectedState);
            } else if (state == SatelliteManager.SATELLITE_MODEM_STATE_OFF) {
                transitionTo(mPowerOffState);
            }
        }

        private void handleEventDatagramTransferStateChanged(
                @NonNull DatagramTransferState datagramTransferState) {
            if (datagramTransferState.sendState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT
                    || datagramTransferState.receiveState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT) {
                stopNbIotInactivityTimer();

                if (mSatelliteController.getRequestIsEmergency()) {
                    stopEsosInactivityTimer();
                }
                stopP2pSmsInactivityTimer();
            } else if (datagramTransferState.sendState == SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE
                    && datagramTransferState.receiveState
                    == SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE) {
                startNbIotInactivityTimer();
                evaluateStartingEsosInactivityTimer();
                evaluateStartingP2pSmsInactivityTimer();
            } else if (isSending(datagramTransferState.sendState)
                    || isReceiving(datagramTransferState.receiveState)) {
                stopNbIotInactivityTimer();

                int datagramType = datagramTransferState.datagramType;
                if (datagramType == DATAGRAM_TYPE_SOS_MESSAGE) {
                    stopEsosInactivityTimer();
                } else if (datagramType == DATAGRAM_TYPE_SMS) {
                    stopP2pSmsInactivityTimer();
                } else {
                    plogd("datagram type is not SOS_Message and SMS " + datagramType);
                }
            }
        }
    }

    private class ConnectedState extends State {
        @Override
        public void enter() {
            if (DBG) plogd("Entering ConnectedState");

            mPreviousState = mCurrentState;
            mCurrentState = SATELLITE_MODEM_STATE_CONNECTED;
            notifyStateChangedEvent(SATELLITE_MODEM_STATE_CONNECTED);
            startNbIotInactivityTimer();
            evaluateStartingEsosInactivityTimer();
            evaluateStartingP2pSmsInactivityTimer();
            mSatelliteController.updateLastNotifiedCarrierRoamingNtnSignalStrengthAndNotify(
                    mSatelliteController.getSatellitePhone());
        }

        @Override
        public void exit() {
            if (DBG) plogd("Exiting ConnectedState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) plogd("ConnectedState: processing " + getWhatToString(msg.what));
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
                case EVENT_SCREEN_STATE_CHANGED:
                    handleEventScreenStateChanged((AsyncResult) msg.obj);
                    break;
                case EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT:
                    handleEventScreenOffInactivityTimerTimedOut();
                    break;
                case EVENT_ESOS_INACTIVITY_TIMER_TIMED_OUT:
                    if (isP2pSmsInActivityTimerStarted()) {
                        plogd("ConnectedState: processing: P2P_SMS inactivity timer running "
                                + "can not move to IDLE");
                    } else {
                        transitionTo(mIdleState);
                    }
                    break;
                case EVENT_P2P_SMS_INACTIVITY_TIMER_TIMED_OUT:
                    handleEventP2pSmsInactivityTimerTimedOut();
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleEventSatelliteModemStateChanged(
                @SatelliteManager.SatelliteModemState int state) {
            if (state == SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED) {
                transitionTo(mNotConnectedState);
            } else if (state == SatelliteManager.SATELLITE_MODEM_STATE_OFF) {
                transitionTo(mPowerOffState);
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
            case EVENT_SCREEN_STATE_CHANGED:
                whatString = "EVENT_SCREEN_STATE_CHANGED";
                break;
            case EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT:
                whatString = "EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT";
                break;
            case EVENT_ESOS_INACTIVITY_TIMER_TIMED_OUT:
                whatString = "EVENT_ESOS_INACTIVITY_TIMER_TIMED_OUT";
                break;
            case EVENT_P2P_SMS_INACTIVITY_TIMER_TIMED_OUT:
                whatString = "EVENT_P2P_SMS_INACTIVITY_TIMER_TIMED_OUT";
                break;
            case EVENT_ENABLE_CELLULAR_MODEM_WHILE_SATELLITE_MODE_IS_ON_DONE:
                whatString = "EVENT_ENABLE_CELLULAR_MODEM_WHILE_SATELLITE_MODE_IS_ON_DONE";
                break;
            case EVENT_SERVICE_STATE_CHANGED:
                whatString = "EVENT_SERVICE_STATE_CHANGED";
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

    private int getSubId() {
        return mSatelliteController.getSelectedSatelliteSubId();
    }

    private void notifyStateChangedEvent(@SatelliteManager.SatelliteModemState int state) {
        mDatagramController.onSatelliteModemStateChanged(state);

        List<ISatelliteModemStateCallback> toBeRemoved = new ArrayList<>();
        mListeners.values().forEach(listener -> {
            try {
                listener.onSatelliteModemStateChanged(state);
            } catch (RemoteException e) {
                plogd("notifyStateChangedEvent RemoteException: " + e);
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
            ploge(caller + ": Unexpected satellite radio powered-on state changed event");
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
            ploge("Unable to bind to the satellite gateway service because the package is"
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
                plogd("Successfully bound to the satellite gateway service.");
            } else {
                synchronized (mLock) {
                    mIsBinding = false;
                }
                mExponentialBackoff.notifyFailed();
                ploge("Error binding to the satellite gateway service. Retrying in "
                        + mExponentialBackoff.getCurrentDelay() + " ms.");
            }
        } catch (Exception e) {
            synchronized (mLock) {
                mIsBinding = false;
            }
            mExponentialBackoff.notifyFailed();
            ploge("Exception binding to the satellite gateway service. Retrying in "
                    + mExponentialBackoff.getCurrentDelay() + " ms. Exception: " + e);
        }
    }

    private void unbindService() {
        plogd("unbindService");
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
            plogd("onServiceConnected: ComponentName=" + name);
            synchronized (mLock) {
                mIsBound = true;
                mIsBinding = false;
            }
            mSatelliteGatewayService = ISatelliteGateway.Stub.asInterface(service);
            mExponentialBackoff.stop();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ploge("onServiceDisconnected: Waiting for reconnect.");
            synchronized (mLock) {
                mIsBinding = false;
                mIsBound = false;
            }
            mSatelliteGatewayService = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            ploge("onBindingDied: Unbinding and rebinding service.");
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

    private void registerForScreenStateChanged() {
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) {
            Rlog.d(TAG, "registerForScreenStateChanged: carrierRoamingNbIotNtn is disabled");
            return;
        }

        if (!mIsRegisteredScreenStateChanged && mDeviceStateMonitor != null) {
            mDeviceStateMonitor.registerForScreenStateChanged(
                    getHandler(), EVENT_SCREEN_STATE_CHANGED, null);

            mIsRegisteredScreenStateChanged = true;
            plogd("registerForScreenStateChanged: registered");
        } else {
            plogw("registerForScreenStateChanged: skip register, mIsRegisteredScreenStateChanged="
                    + mIsRegisteredScreenStateChanged + ","
                    + " mDeviceStateMonitor=" + (mDeviceStateMonitor != null));
        }
    }

    private void unregisterForScreenStateChanged() {
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) {
            Rlog.d(TAG, "unregisterForScreenStateChanged: carrierRoamingNbIotNtn is disabled");
            return;
        }

        if (mIsRegisteredScreenStateChanged && mDeviceStateMonitor != null) {
            mDeviceStateMonitor.unregisterForScreenStateChanged(getHandler());
            removeMessages(EVENT_SCREEN_STATE_CHANGED);
            removeMessages(EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT);

            removeDeferredMessages(EVENT_SCREEN_STATE_CHANGED);
            removeDeferredMessages(EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT);

            mIsRegisteredScreenStateChanged = false;
            plogd("unregisterForScreenStateChanged: unregistered");
        }
    }

    private void handleEventScreenStateChanged(AsyncResult asyncResult) {
        if (asyncResult == null) {
            ploge("handleEventScreenStateChanged: asyncResult is null");
            return;
        }

        boolean screenOn = (boolean) asyncResult.result;
        if (mIsScreenOn == screenOn) {
            if (DBG) plogd("handleEventScreenStateChanged: screen state is not changed");
            return;
        }
        mIsScreenOn = screenOn;

        if (!mSatelliteController.isInCarrierRoamingNbIotNtn()) {
            logd("handleEventScreenStateChanged: device is not in CarrierRoamingNbIotNtn");
            return;
        }

        if (mSatelliteController.getRequestIsEmergency()) {
            if (DBG) logd("handleEventScreenStateChanged: Emergency mode");
            // This is for coexistence
            // emergency mode can be set after registerForScreenStateChanged() called for P2P-sms
            return;
        }

        int subId = getSubId();
        if (!isP2pSmsSupportedOnCarrierRoamingNtn(subId)) {
            if (DBG) plogd("handleEventScreenStateChanged: P2P_SMS is not supported");
            return;
        }

        if (!screenOn) {
            // Screen off, start timer
            int timeoutMillis = getScreenOffInactivityTimeoutDurationSec() * 1000;

            if (mAlarmManager == null) {
                plogd("handleEventScreenStateChanged: can not access AlarmManager to start timer");
                return;
            }

            mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + timeoutMillis,
                    TAG, new HandlerExecutor(getHandler()), new WorkSource(), mAlarmListener);
            plogd("handleEventScreenStateChanged: start timer " + timeoutMillis);
        } else {
            // Screen on, stop timer
            removeMessages(EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT);

            if (mAlarmManager == null) {
                plogd("handleEventScreenStateChanged: can not access AlarmManager to stop timer");
                return;
            }

            mAlarmManager.cancel(mAlarmListener);
            plogd("handleEventScreenStateChanged: stop timer");
        }
    }

    private void handleEventP2pSmsInactivityTimerTimedOut() {
        if (isEsosInActivityTimerStarted()) {
            plogd("handleEventP2pSmsInactivityTimerTimedOut: processing: ESOS inactivity timer "
                    + "running can not move to IDLE");
        } else {
            if (isTnScanningAllowedDuringSatelliteSession()) {
                plogd("handleEventP2pSmsInactivityTimerTimedOut: Transition to IDLE state");
                transitionTo(mIdleState);
            } else {
                if (mSatelliteController.getRequestIsEmergency()) {
                    plogd("handleEventP2pSmsInactivityTimerTimedOut: Emergency mode");
                    return;
                }

                plogd("handleEventP2pSmsInactivityTimerTimedOut: request disable satellite");
                mSatelliteController.requestSatelliteEnabled(
                        false /*enableSatellite*/,
                        false /*enableDemoMode*/,
                        mSatelliteController.getRequestIsEmergency() /*isEmergency*/,
                        new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                plogd("requestSatelliteEnabled result=" + result);
                            }
                        });
            }
        }
    }

    private int getScreenOffInactivityTimeoutDurationSec() {
        PersistableBundle config = mSatelliteController.getPersistableBundle(getSubId());

        return config.getInt(KEY_SATELLITE_ROAMING_SCREEN_OFF_INACTIVITY_TIMEOUT_SEC_INT,
                DEFAULT_SCREEN_OFF_INACTIVITY_TIMEOUT_SEC);
    }

    private int getEsosInactivityTimeoutDurationSec() {
        PersistableBundle config = mSatelliteController.getPersistableBundle(getSubId());

        return config.getInt(KEY_SATELLITE_ROAMING_ESOS_INACTIVITY_TIMEOUT_SEC_INT,
                DEFAULT_ESOS_INACTIVITY_TIMEOUT_SEC);
    }

    private void evaluateStartingEsosInactivityTimer() {
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) {
            plogd("evaluateStartingEsosInactivityTimer: "
                    + "carrierRoamingNbIotNtn is disabled");
            return;
        }

        if (isEsosInActivityTimerStarted()) {
            plogd("isEsosInActivityTimerStarted: "
                    + "ESOS inactivity timer already started");
            return;
        }

        int subId = getSubId();
        if (!mSatelliteController.isSatelliteEsosSupported(subId)) {
            plogd("evaluateStartingEsosInactivityTimer: ESOS is not supported");
            return;
        }

        if (!mSatelliteController.getRequestIsEmergency()) {
            plogd("evaluateStartingEsosInactivityTimer: request is not emergency");
            return;
        }

        if (mIsDeviceAlignedWithSatellite) {
            plogd("evaluateStartingEsosInactivityTimer: "
                    + "can't start ESOS inactivity timer due to device aligned satellite");
            return;
        }

        int timeOutMillis = getEsosInactivityTimeoutDurationSec() * 1000;
        DatagramController datagramController = DatagramController.getInstance();
        if (datagramController.isSendingInIdleState()
                && datagramController.isPollingInIdleState()) {
            sendMessageDelayed(EVENT_ESOS_INACTIVITY_TIMER_TIMED_OUT, timeOutMillis);
            plogd("evaluateStartingEsosInactivityTimer: start ESOS inactivity timer "
                    + timeOutMillis);
        } else {
            plogd("evaluateStartingEsosInactivityTimer: "
                    + "can't start ESOS inactivity timer");
        }
    }

    private void stopEsosInactivityTimer() {
        if (isEsosInActivityTimerStarted()) {
            removeMessages(EVENT_ESOS_INACTIVITY_TIMER_TIMED_OUT);
            plogd("stopEsosInactivityTimer: ESOS inactivity timer stopped");
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public boolean isEsosInActivityTimerStarted() {
        return hasMessages(EVENT_ESOS_INACTIVITY_TIMER_TIMED_OUT);
    }

    private int getP2pSmsInactivityTimeoutDurationSec() {
        PersistableBundle config = mSatelliteController.getPersistableBundle(getSubId());

        return config.getInt(KEY_SATELLITE_ROAMING_P2P_SMS_INACTIVITY_TIMEOUT_SEC_INT,
                DEFAULT_P2P_SMS_INACTIVITY_TIMEOUT_SEC);
    }

    private void evaluateStartingP2pSmsInactivityTimer() {
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) {
            plogd("evaluateStartingP2pSmsInactivityTimer: "
                    + "carrierRoamingNbIotNtn is disabled");
            return;
        }

        if (isP2pSmsInActivityTimerStarted()) {
            plogd("isP2pSmsInActivityTimerStarted: "
                    + "P2P_SMS inactivity timer already started");
            return;
        }

        int subId = getSubId();
        if (!isP2pSmsSupportedOnCarrierRoamingNtn(subId)) {
            if (DBG) plogd("evaluateStartingP2pSmsInactivityTimer: P2P_SMS is not supported");
            return;
        }

        if (mIsDeviceAlignedWithSatellite) {
            plogd("evaluateStartingP2pSmsInactivityTimer: "
                    + "can't start P2P_SMS inactivity timer due to device aligned satellite");
            return;
        }

        int timeOutMillis = getP2pSmsInactivityTimeoutDurationSec() * 1000;
        DatagramController datagramController = DatagramController.getInstance();
        if (datagramController.isSendingInIdleState()
                && datagramController.isPollingInIdleState()) {
            sendMessageDelayed(EVENT_P2P_SMS_INACTIVITY_TIMER_TIMED_OUT, timeOutMillis);
            plogd("evaluateStartingP2pSmsInactivityTimer: start P2P_SMS inactivity timer "
                    + timeOutMillis);
        } else {
            plogd("evaluateStartingP2pSmsInactivityTimer: "
                    + "can't start P2P_SMS inactivity timer");
        }
    }

    private void stopP2pSmsInactivityTimer() {
        if (isP2pSmsInActivityTimerStarted()) {
            removeMessages(EVENT_P2P_SMS_INACTIVITY_TIMER_TIMED_OUT);
            plogd("stopP2pSmsInactivityTimer: P2P_SMS inactivity timer stopped");
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public boolean isP2pSmsInActivityTimerStarted() {
        return hasMessages(EVENT_P2P_SMS_INACTIVITY_TIMER_TIMED_OUT);
    }

    /**
     * Initializes the inactivity start timestamp.
     *
     * <p>This method is called when 1) the datagram transfer state changes to idle or 2) the
     * device is unaligned with the satellite.
     */
    private void checkForInactivity() {
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) {
            return;
        }

        // If the inactivity start timestamp is not undefined, it means the inactivity has already
        // started.
        if (mInactivityStartTimestamp != UNDEFINED_TIMESTAMP) {
            return;
        }

        boolean isInactive = mLastDatagramTransferState.isIdle() && !mIsDeviceAlignedWithSatellite;
        if (isInactive) {
            mInactivityStartTimestamp = SystemClock.elapsedRealtime();
        }
    }

    /**
     * Updates the max inactivity duration session metric.
     *
     * <p>This method is called when 1) the datagram transfer state changes to not idle, 2) the
     * device is aligned with the satellite, or 3) modem state moves to PowerOffState.
     */
    private void endUserInactivity() {
        if (!mFeatureFlags.carrierRoamingNbIotNtn()) {
            plogd("endUserInactivity: carrierRoamingNbIotNtn is disabled");
            return;
        }

        if (mInactivityStartTimestamp != UNDEFINED_TIMESTAMP) {
            long inactivityDurationMs = SystemClock.elapsedRealtime() - mInactivityStartTimestamp;
            int inactivityDurationSec = (int) (inactivityDurationMs / 1000);
            mSessionMetricsStats.updateMaxInactivityDurationSec(inactivityDurationSec);

            mInactivityStartTimestamp = UNDEFINED_TIMESTAMP;
        }
    }

    private void handleEventScreenOffInactivityTimerTimedOut() {
        if (mSatelliteController.getRequestIsEmergency()) {
            loge("handleEventScreenOffInactivityTimerTimedOut: Emergency mode");
            /* This is for coexistence
             * mIsEmergency can be set after
             * EVENT_SCREEN_OFF_INACTIVITY_TIMER_TIMED_OUT timer started
             */
            return;
        }

        plogd("handleEventScreenOffInactivityTimerTimedOut: request disable satellite");

        mSatelliteController.requestSatelliteEnabled(
                false /*enableSatellite*/,
                false /*enableDemoMode*/,
                mSatelliteController.getRequestIsEmergency() /*isEmergency*/,
                new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        plogd("requestSatelliteEnabled result=" + result);
                        if (result == SATELLITE_RESULT_SUCCESS) {
                            mSessionMetricsStats.addCountOfAutoExitDueToScreenOff();
                        }
                    }
                });
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
        if (!isSatelliteEnabledForNtnOnlySubscription()) {
            plogd("startNbIotInactivityTimer: Can't start timer "
                    + "because satellite was not enabled for OEM based NB IOT");
            return;
        }

        if (isNbIotInactivityTimerStarted()) {
            plogd("NB IOT inactivity timer is already started");
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

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected boolean isSatelliteEnabledForNtnOnlySubscription() {
        if (SatelliteServiceUtils.getNtnOnlySubscriptionId(mContext)
                != getSubId()) {
            plogd("isSatelliteEnabledForOemBasedNbIot: highest priority satellite subscription "
                    + "is not NTN-only subscription");
            return false;
        }

        return true;
    }

    private boolean isP2pSmsSupportedOnCarrierRoamingNtn(int subId) {
        if (!mSatelliteController.isSatelliteRoamingP2pSmSSupported(subId)) {
            if (DBG) plogd("isP2pSmsSupportedOnCarrierRoamingNtn: P2P_SMS is not supported");
            return false;
        }

        int[] services = mSatelliteController.getSupportedServicesOnCarrierRoamingNtn(subId);
        if (!ArrayUtils.contains(services, NetworkRegistrationInfo.SERVICE_TYPE_SMS)) {
            if (DBG) {
                plogd("isP2pSmsSupportedOnCarrierRoamingNtn: P2P_SMS service is not supported "
                        + "on carrier roaming ntn.");
            }
            return false;
        }

        if (DBG) plogd("isP2pSmsSupportedOnCarrierRoamingNtn: P2_SMS is supported");
        return true;
    }

    private boolean isConcurrentTnScanningSupported() {
        try {
            return mContext.getResources().getBoolean(
                R.bool.config_satellite_modem_support_concurrent_tn_scanning);
        } catch (RuntimeException e) {
            plogd("isConcurrentTnScanningSupported: ex=" + e);
            return false;
        }
    }

    private boolean isTnScanningAllowedDuringSatelliteSession() {
        try {
            return mContext.getResources().getBoolean(
                    R.bool.config_satellite_allow_tn_scanning_during_satellite_session);
        } catch (RuntimeException e) {
            plogd("isTnScanningAllowedDuringSatelliteSession: ex=" + e);
            return false;
        }
    }

    private void plogd(@NonNull String log) {
        logd(log);
        if (mPersistentLogger != null) {
            mPersistentLogger.debug(TAG, log);
        }
    }

    private void plogw(@NonNull String log) {
        logw(log);
        if (mPersistentLogger != null) {
            mPersistentLogger.warn(TAG, log);
        }
    }

    private void ploge(@NonNull String log) {
        loge(log);
        if (mPersistentLogger != null) {
            mPersistentLogger.error(TAG, log);
        }
    }
}

/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.clockwork.cellular;

import static com.android.clockwork.common.ThermalEmergencyTracker.ThermalEmergencyMode;

import static com.google.android.clockwork.signaldetector.SignalStateModel.STATE_NO_SIGNAL;
import static com.google.android.clockwork.signaldetector.SignalStateModel.STATE_OK_SIGNAL;
import static com.google.android.clockwork.signaldetector.SignalStateModel.STATE_UNSTABLE_SIGNAL;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;
import android.util.EventLog;
import android.util.Log;

import com.android.clockwork.common.DeviceEnableSetting;
import com.android.clockwork.common.EventHistory;
import com.android.clockwork.common.ProxyConnectivityDebounce;
import com.android.clockwork.connectivity.WearConnectivityPackageManager;
import com.android.clockwork.flags.BooleanFlag;
import com.android.clockwork.power.PowerTracker;
import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.util.IndentingPrintWriter;

import com.google.android.clockwork.signaldetector.SignalStateDetector;
import com.google.android.clockwork.signaldetector.SignalStateModel;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The backing logic of the WearCellularMediatorService.
 */
public class WearCellularMediator implements
        DeviceEnableSetting.Listener,
        PowerTracker.Listener,
        ProxyConnectivityDebounce.Listener,
        SignalStateDetector.Listener {
    public static final String TAG = "WearCellularMediator";
    // Whether cell is turned off when around the phone or not.
    // Valid values for this key are 0 and 1
    public static final String CELL_AUTO_SETTING_KEY = "clockwork_cell_auto_setting";
    public static final int CELL_AUTO_OFF = 0;
    public static final int CELL_AUTO_ON = 1;
    // Default value for cell auto on/off setting
    public static final int CELL_AUTO_SETTING_DEFAULT = CELL_AUTO_ON;

    public static final Uri CELL_AUTO_SETTING_URI =
            Settings.System.getUriFor(CELL_AUTO_SETTING_KEY);

    public static final Uri CELL_ON_URI = Settings.Global.getUriFor(Settings.Global.CELL_ON);
    public static final Uri ENABLE_CELLULAR_ON_BOOT_URI =
            Settings.Global.getUriFor(Global.ENABLE_CELLULAR_ON_BOOT);

    // Used by WearCellularMediatorSettings.getRadioOnState()
    public static final int RADIO_ON_STATE_UNKNOWN = -1;
    public static final int RADIO_ON_STATE_ON = 1;
    public static final int RADIO_ON_STATE_OFF = 0;
    /**
     * Broadcast sent by Euicc LPA when Test Mode is entered or exited.
     */
    public static final String ACTION_ESIM_TEST_MODE = "com.google.android.euicc.ESIM_TEST_MODE";
    /** Boolean extra of whether the LPA is in test mode. */
    public static final String EXTRA_IN_ESIM_TEST_MODE =
            "com.google.android.euicc.IN_ESIM_TEST_MODE";
    @VisibleForTesting
    static final int MSG_DISABLE_CELL = 0;
    @VisibleForTesting
    static final int MSG_ENABLE_CELL = 1;
    static final int MSG_EMERGENCY_RADIO_ON_TIMEOUT = 2;
    static final String ACTION_EXIT_CELL_LINGER =
            "com.android.clockwork.connectivity.action.ACTION_EXIT_CELL_LINGER";
    static final String ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED =
            "android.intent.action.SUBSCRIPTION_PHONE_STATE";
    static final Uri MOBILE_SIGNAL_DETECTOR_URI =
            Settings.Global.getUriFor(Settings.Global.Wearable.MOBILE_SIGNAL_DETECTOR);
    @VisibleForTesting
    static final String ICC_WEAR_INITIAL_BOOT = "ICC_WEAR_INITIAL_BOOT";
    /**
     * Enforces a delay every time bluetooth sysproxy connects.
     * Prevents Cell from thrashing when we very quickly transition between desired cell states.
     *
     * We use AlarmManager to enforce the turning off of Cell after the linger period.
     * The constants below define the default linger window to be 30-60 seconds.
     */
    private static final long DEFAULT_CELL_LINGER_DURATION_MS = TimeUnit.SECONDS.toMillis(30);
    private static final long MAX_ACCEPTABLE_LINGER_DELAY_MS = TimeUnit.SECONDS.toMillis(30);
    private static final long CELL_DELAY_MS = TimeUnit.SECONDS.toMillis(30);

    private static final long WAIT_FOR_SET_RADIO_POWER_IN_MS = TimeUnit.SECONDS.toMillis(5);
    static final String ACTION_EXIT_CELL_DELAY =
            "com.android.clockwork.connectivity.action.ACTION_EXIT_CELL_DELAY";

    // Keep radio on after an IMS emergency call ends in order to release emergency service
    // gracefully.
    // According to TS 36.523-1_TC 11.2.4, we should keep radio on before expiry of T3412 which is
    // a 6-min timer. We add extra 3-sec as a tolerance in case of TE verdict by its owner timer.
    private static final long EMERGENCY_RADIO_ON_LINGER_IN_MS = TimeUnit.SECONDS.toMillis(363);
    @VisibleForTesting
    final PendingIntent exitCellLingerIntent;
    private final Context mContext;
    private final AlarmManager mAlarmManager;
    private final TelephonyManager mTelephonyManager;
    private final EuiccManager mEuiccManager;
    private final SubscriptionManager mSubscriptionManager;
    private final WearCellularMediatorSettings mSettings;
    private final SignalStateDetector mSignalStateDetector;
    private final PowerTracker mPowerTracker;
    private final DeviceEnableSetting mDeviceEnableSetting;
    private final WearConnectivityPackageManager mWearConnectivityPackageManager;
    private final BooleanFlag mUserAbsentRadiosOff;
    private final Object mLock = new Object();
    private final EventHistory<CellDecision> mHistory =
            new EventHistory<>("Cell Radio Power History", 30, false);
    @VisibleForTesting
    Handler mHandler;
    private TwinnedNumberBlocker mTwinnedNumberBlocker;
    private int mCellState;
    private int mCellAuto;
    private boolean mHasActiveEsimSubscription;
    private SubscriptionInfo mCurrentActiveSubscription;
    private boolean mIsEsimProfileDeactivated;
    private boolean mIsInEsimTestMode;
    private boolean mIsProxyConnected;
    private boolean mIsUserUnlocked;
    private boolean mIsInTelephonyCall;
    private boolean mIsInEcbm;
    private boolean mIsInEmergencyCall;
    private boolean mIsWifiConnected;
    private boolean mIsVoWifiEnabled = false;
    // ECBM is only used for Verizon only to keep radio on. So we need another way to keep radio on
    // for a while after emergency call ends in order to pass PTCRB certification.
    private boolean mIsRadioOnAfterEmergencyCall;
    private String mIccState = ICC_WEAR_INITIAL_BOOT;
    private boolean mBooted;
    private int mNumHighBandwidthRequests;
    private int mNumCellularRequests;
    private int mSignalState = STATE_OK_SIGNAL;
    private boolean mActivityMode = false;
    private boolean mCellOnlyMode = false;
    private boolean mIsThermalEmergency;
    private int mLastServiceState = ServiceState.STATE_POWER_OFF;
    private boolean mCellLingering;
    private Reason mCellLingeringReason;
    private TriggerEvent mCellLingeringTriggerEvent;
    @VisibleForTesting ImsManager mImsManagerForTesting;

    @VisibleForTesting
    BroadcastReceiver exitCellLingerReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (ACTION_EXIT_CELL_LINGER.equals(intent.getAction())) {
                        if (mCellLingering) {
                            Log.d(
                                    TAG,
                                    "Leaving cell linger state: "
                                            + mCellLingeringReason
                                            + ":"
                                            + mCellLingeringTriggerEvent);
                            mCellLingering = false;
                            mHandler.sendMessage(
                                    Message.obtain(
                                            mHandler,
                                            MSG_DISABLE_CELL,
                                            new RadioStateChangeReqInfo(
                                                    mCellLingeringReason,
                                                    mCellLingeringTriggerEvent)));
                            mCellLingeringReason = null;
                        }
                    }
                }
            };

    private long mCellLingerDurationMs;
    private boolean mShouldDelayNextCellOn = false;
    private AlarmManager.OnAlarmListener mExitCellDelayAlarm;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onReceive: " + intent);
            }
            switch (intent.getAction()) {
                case ACTION_ESIM_TEST_MODE:
                    Log.d(TAG, "Esim Test Mode broadcast received.");
                    mIsInEsimTestMode = intent.getBooleanExtra(EXTRA_IN_ESIM_TEST_MODE, false);
                    Log.d(TAG, "isInEsimTestMode: " + mIsInEsimTestMode);
                    mSettings.setEsimTestModeState(mIsInEsimTestMode);
                    updateRadioPower(TriggerEvent.ESIM_TEST_MODE);
                    break;
                case ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED:
                    final String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    mIsInTelephonyCall = TelephonyManager.EXTRA_STATE_OFFHOOK.equals(phoneState)
                            || TelephonyManager.EXTRA_STATE_RINGING.equals(phoneState);
                    updateRadioPower(TriggerEvent.SUBSCRIPTION_PHONE_STATE_CHANGED);
                    break;
                case Intent.ACTION_NEW_OUTGOING_CALL:
                    String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                    mIsInEmergencyCall = PhoneNumberUtils.isEmergencyNumber(phoneNumber);
                    Log.d(TAG, "mIsInEmergencyCall: " + mIsInEmergencyCall);
                    // b/195536863: intentionally do not call updateRadioPower here to avoid
                    // pre-empting TelephonyConnectionService in the Phone process, which will
                    // enable the radio and pass the correct emergency flags to the modem
                    break;
                case TelephonyManager.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED:
                    mIsInEcbm = intent.getBooleanExtra(
                            TelephonyManager.EXTRA_PHONE_IN_ECM_STATE, false);
                    Log.d(TAG, "mIsInEcbm: " + mIsInEcbm);
                    // b/195536863: intentionally do not call updateRadioPower here to avoid
                    // pre-empting TelephonyConnectionService in the Phone process, which will
                    // enable the radio and pass the correct emergency flags to the modem
                    break;
                case TelephonyIntents.ACTION_SIM_STATE_CHANGED:
                    mIccState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                    updateRadioPower(TriggerEvent.SIM_STATE_CHANGED);
                    break;
                case CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED:
                    // b/234160036 carriers may override PowerTracker features
                    Log.d(TAG, "Carrier config changed, updating features.");
                    mPowerTracker.repopulateFeatures();
                    break;
                default:
                    Log.e(TAG, "Unknown intent: " + intent);
                    break;
            }
        }
    };
    private final OnSubscriptionsChangedListener mSubscriptionsChangedListener =
            new OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                    boolean oldVal = mHasActiveEsimSubscription;
                    updateActiveEsimSubscriptionStatus();
                    if (mHasActiveEsimSubscription != oldVal) {
                        Log.d(TAG, "eSIM Active Subscription Status changed: "
                                + mHasActiveEsimSubscription);
                        updateRadioPower(TriggerEvent.SUBSCRIPTIONS_CHANGED);
                        mTwinnedNumberBlocker.onActiveSubscriptionChanged(
                                mCurrentActiveSubscription);
                    }
                }
            };
    private final ContentObserver mCellSettingsObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    if (CELL_AUTO_SETTING_URI.equals(uri)) {
                        mCellAuto = mSettings.getCellAutoSetting();
                        updateRadioPower(TriggerEvent.CELL_AUTO_SETTING);
                    } else if (WearCellularMediatorSettings.ESIM_PROFILE_ACTIVATION_SETTING_URI
                            .equals(uri)) {
                        mIsEsimProfileDeactivated = mSettings.isEsimProfileDeactivated();
                        updateRadioPower(TriggerEvent.ESIM_PROFILE_ACTIVATION_SETTING);
                    } else if (CELL_ON_URI.equals(uri)) {
                        mCellState = mSettings.getCellState();
                        updateRadioPower(TriggerEvent.CELL_ON_SETTING);
                    } else if (MOBILE_SIGNAL_DETECTOR_URI.equals(uri)) {
                        updateDetectorState(mSettings.getRadioOnState());
                        updateRadioPower(TriggerEvent.MOBILE_SIGNAL_DETECTOR_SETTING);
                    } else if (ENABLE_CELLULAR_ON_BOOT_URI.equals(uri)) {
                        if (!selfChange) {
                            Log.w(TAG, "enable_cellular_on_boot setting was updated unexpectedly.");
                            ensureCellOnRebootBehaviorIsCorrect(mCellAuto, mCellState);
                        }
                    } else if (SubscriptionManager.WFC_ENABLED_CONTENT_URI.equals(uri)) {
                        mIsVoWifiEnabled =
                                mHasActiveEsimSubscription
                                        ? isVoWifiEnabled(
                                                mCurrentActiveSubscription.getSimSlotIndex())
                                        : false;
                        Log.d(TAG, "VoWIFI status update: enabled=" + mIsVoWifiEnabled);
                        updateRadioPower(TriggerEvent.VOWIFI_STATE_UPDATE);
                    } else {
                        Log.e(TAG, "Unknown ContentObserver onChange uri: " + uri);
                    }
                }
            };
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            if (isServiceStatePowerToggle(serviceState.getState())) {
                synchronized (mLock) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "mLock.notify() serviceState: " + serviceState.getState());
                    }
                    mLock.notify();
                }
            }

            mLastServiceState = serviceState.getState();
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (mIsInEmergencyCall && state == TelephonyManager.CALL_STATE_IDLE) {
                Log.d(TAG, "Keep radio on after emergency call ended.");
                mIsRadioOnAfterEmergencyCall = true;
                mHandler.removeMessages(MSG_EMERGENCY_RADIO_ON_TIMEOUT);
                // sendMessageDelayed does not guarantee to fire this message after
                // EMERGENCY_RADIO_ON_LINGER_IN_MS exactly.
                mHandler.sendMessageDelayed(
                        Message.obtain(mHandler, MSG_EMERGENCY_RADIO_ON_TIMEOUT,
                                new RadioStateChangeReqInfo(Reason.ON_EMERGENCY,
                                        TriggerEvent.CALL_STATE_CHANGED_EMERGENCY_ENDED)),
                        EMERGENCY_RADIO_ON_LINGER_IN_MS);
                mIsInEmergencyCall = false;
                updateRadioPower(TriggerEvent.CALL_STATE_CHANGED_EMERGENCY_ENDED);
            }
        }

        // As mentioned in 3GPP 24.229 Release 12 section L.2.2.6, E-PDN is required for emergency
        // registration. When the emergency registration expires, the UE should disconnect the PDN
        // connection.
        // Some carriers perfer to terminate E-PDN right away after an emergency call ends.
        // This is also a signal of emergency service de-registered so that we don't need to keep
        // radio on until EMERGENCY_RADIO_ON_LINGER_IN_MS timeout.
        @Override
        public void onPreciseDataConnectionStateChanged(
                PreciseDataConnectionState connectionState) {
            if (mIsRadioOnAfterEmergencyCall
                    && connectionState != null
                    && connectionState.getApnSetting() != null
                    && connectionState.getApnSetting().isEmergencyApn()
                    && connectionState.getState() == TelephonyManager.DATA_DISCONNECTED) {
                Log.d(TAG, "E-PDN disconnected");
                mHandler.removeMessages(MSG_EMERGENCY_RADIO_ON_TIMEOUT);
                mIsRadioOnAfterEmergencyCall = false;
                updateRadioPower(TriggerEvent.CONNECTION_STATE_CHANGED_PDN_DISCONNECTED);
            }
        }
    };

    public WearCellularMediator(
            Context context,
            AlarmManager alarmManager,
            TelephonyManager telephonyManager,
            EuiccManager euiccManager,
            SubscriptionManager subscriptionManager,
            WearCellularMediatorSettings settings,
            PowerTracker powerTracker,
            DeviceEnableSetting deviceEnableSetting,
            WearConnectivityPackageManager wearConnectivityPackageManager,
            BooleanFlag userAbsentRadiosOff) {
        this(context,
                context.getContentResolver(),
                alarmManager,
                telephonyManager,
                euiccManager,
                subscriptionManager,
                settings,
                powerTracker,
                deviceEnableSetting,
                wearConnectivityPackageManager,
                userAbsentRadiosOff,
                new SignalStateDetector(context, new SignalStateModel(settings), settings),
                new TwinnedNumberBlocker(context));
    }

    @VisibleForTesting
    WearCellularMediator(
            Context context,
            ContentResolver contentResolver,
            AlarmManager alarmManager,
            TelephonyManager telephonyManager,
            EuiccManager euiccManager,
            SubscriptionManager subscriptionManager,
            WearCellularMediatorSettings wearCellularMediatorSettings,
            PowerTracker powerTracker,
            DeviceEnableSetting deviceEnableSetting,
            WearConnectivityPackageManager wearConnectivityPackageManager,
            BooleanFlag userAbsentRadiosOff,
            SignalStateDetector signalStateDetector,
            TwinnedNumberBlocker twinnedNumberBlocker) {
        mContext = context;
        mAlarmManager = alarmManager;
        mTelephonyManager = telephonyManager;
        mEuiccManager = euiccManager;
        mSubscriptionManager = subscriptionManager;
        mSettings = wearCellularMediatorSettings;
        mPowerTracker = powerTracker;
        mDeviceEnableSetting = deviceEnableSetting;
        mWearConnectivityPackageManager = wearConnectivityPackageManager;
        mTwinnedNumberBlocker = twinnedNumberBlocker;

        mUserAbsentRadiosOff = userAbsentRadiosOff;

        mSignalStateDetector = signalStateDetector;

        HandlerThread thread = new HandlerThread(TAG + ".RadioPowerHandler");
        thread.start();
        mHandler = new RadioPowerHandler(thread.getLooper());

        mPowerTracker.addListener(this);
        mDeviceEnableSetting.addListener(this);
        mUserAbsentRadiosOff.addListener(this::onUserAbsentRadiosOffChanged);

        // Register broadcast receivers and content observers.
        IntentFilter filter = new IntentFilter();
        // There are two methods in TelephonyRegistry to notify the downstream about the
        // call state:
        // 1. notifyCallState()
        // 2. notifyCallStateForPhoneId()
        // notifyCallState() is used by Telecom's PhoneStateBroadcaster which treats BT
        // HFP calls same as Telephony call.
        // notifyCallStateForPhoneId() is used by Telephony's DefaultPhoneNotifier and
        // is only used for Telephony calls.
        // The cellular mediator should not turn on radio power for a non-telephony call
        // obviously. So we listen to the ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED intent
        // instead of ACTION_PHONE_STATE_CHANGED.
        filter.addAction(ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        filter.addAction(ACTION_ESIM_TEST_MODE);
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        mContext.registerReceiver(mIntentReceiver, filter, Context.RECEIVER_EXPORTED);

        contentResolver.registerContentObserver(
                CELL_AUTO_SETTING_URI, false, mCellSettingsObserver);
        contentResolver.registerContentObserver(
                WearCellularMediatorSettings.ESIM_PROFILE_ACTIVATION_SETTING_URI,
                false,
                mCellSettingsObserver);
        contentResolver.registerContentObserver(
                CELL_ON_URI, false, mCellSettingsObserver);
        contentResolver.registerContentObserver(
                MOBILE_SIGNAL_DETECTOR_URI, false, mCellSettingsObserver);
        contentResolver.registerContentObserver(
                ENABLE_CELLULAR_ON_BOOT_URI, false, mCellSettingsObserver);
        contentResolver.registerContentObserver(
                SubscriptionManager.WFC_ENABLED_CONTENT_URI, false, mCellSettingsObserver);
        mTelephonyManager.listen(
                mPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_PRECISE_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_SERVICE_STATE);

        mSignalStateDetector.setListener(this);

        mCellLingerDurationMs = DEFAULT_CELL_LINGER_DURATION_MS;
        exitCellLingerIntent =
                PendingIntent.getBroadcast(
                        context,
                        0,
                        new Intent(ACTION_EXIT_CELL_LINGER),
                        PendingIntent.FLAG_IMMUTABLE);
    }

    private boolean isServiceStatePowerToggle(int serviceState) {
        return (serviceState == ServiceState.STATE_POWER_OFF
                && mLastServiceState != ServiceState.STATE_POWER_OFF)
                || (serviceState != ServiceState.STATE_POWER_OFF
                && mLastServiceState == ServiceState.STATE_POWER_OFF);
    }

    // Called when boot complete.
    public void onBootCompleted(boolean proxyConnected) {
        mContext.registerReceiver(exitCellLingerReceiver,
                new IntentFilter(ACTION_EXIT_CELL_LINGER),
                Context.RECEIVER_NOT_EXPORTED);

        mIsProxyConnected = proxyConnected;
        mCellAuto = mSettings.getCellAutoSetting();
        mCellState = mSettings.getCellState();
        ensureCellOnRebootBehaviorIsCorrect(mCellAuto, mCellState);
        updateDetectorState(mSettings.getRadioOnState());
        mBooted = true;
        mIsEsimProfileDeactivated = mSettings.isEsimProfileDeactivated();
        mIsInEsimTestMode = mSettings.getEsimTestModeState();
        mIsWifiConnected = false;
        mSettings.initializeTwinningSettings();
        mSubscriptionManager.addOnSubscriptionsChangedListener(mSubscriptionsChangedListener);
        updateActiveEsimSubscriptionStatus();

        updateRadioPower(TriggerEvent.ON_BOOT_COMPLETED);
    }

    /** Called when exit direct boot, user unlocked device. */
    public void onUserUnlocked() {
        mIsUserUnlocked = true;
        // Next attempt to turn on radion for cell in auto and proxy disconnected
        // should be delayed
        Log.d(TAG, String.format("onUserUnlocked: %s %s", mIsProxyConnected, mCellAuto));
        if (!mIsProxyConnected && mCellAuto == CELL_AUTO_ON) {
            delayCellEnable(Reason.ON_PROXY_DISCONNECTED, TriggerEvent.ON_USER_UNLOCKED);
        }
        Log.d(TAG, "onUserUnlocked: call updateRadioPower");
        updateRadioPower(TriggerEvent.ON_USER_UNLOCKED);
    }

    @VisibleForTesting
    void updateActiveEsimSubscriptionStatus() {
        List<SubscriptionInfo> subInfos = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfos != null && !subInfos.isEmpty()) {
            for (SubscriptionInfo subInfo : subInfos) {
                if (subInfo.isEmbedded()) {
                    mHasActiveEsimSubscription = true;
                    mCurrentActiveSubscription = subInfo;
                    mIsVoWifiEnabled =
                            isVoWifiEnabled(mCurrentActiveSubscription.getSimSlotIndex());
                    return;
                }
            }
        }
        mHasActiveEsimSubscription = false;
    }

    @VisibleForTesting
    void setCellLingerDuration(long durationMs) {
        mCellLingerDurationMs = durationMs;
    }

    @VisibleForTesting
    EventHistory<CellDecision> getDecisionHistory() {
        return mHistory;
    }

    @Override
    public void onPowerSaveModeChanged() {
        updateRadioPower(TriggerEvent.POWER_SAVE_MODE_CHANGED);
    }

    @Override
    public void onChargingStateChanged() {
        // do nothing
    }

    @Override
    public void onDeviceIdleModeChanged() {
        if (!mPowerTracker.getDozeModeAllowListedFeatures()
                .get(PowerTracker.DOZE_MODE_CELLULAR_INDEX)) {
            updateRadioPower(TriggerEvent.DEVICE_IDLE_MODE_CHANGED);
        } else {
            Log.d(TAG, "Ignoring doze mode intent as cellular is being kept enabled during doze.");
        }
    }

    public void updateActivityMode(boolean activeMode) {
        if (mActivityMode != activeMode) {
            mActivityMode = activeMode;
            updateRadioPower(TriggerEvent.ACTIVITY_MODE_UPDATE);
        }
    }

    public void updateCellOnlyMode(boolean cellOnlyMode) {
        if (mCellOnlyMode != cellOnlyMode) {
            mCellOnlyMode = cellOnlyMode;
            updateRadioPower(TriggerEvent.CELL_ONLY_MODE_UPDATE);
        }
    }

    /** Trigger mediator update due to change in connectivity thermal manager. */
    public void updateThermalEmergencyMode(ThermalEmergencyMode mode) {
        boolean enabled = mode.isEnabled() && mode.isCellEffected();
        if (mIsThermalEmergency != enabled) {
            mIsThermalEmergency = enabled;
            updateRadioPower(TriggerEvent.THERMAL_EMERGENCY_MODE_UPDATE);
        }
    }

    public void onUserAbsentRadiosOffChanged(boolean isEnabled) {
        updateRadioPower(TriggerEvent.USER_ABSENT_RADIOS_OFF_CHANGED);
    }

    @Override
    public void onDeviceEnableChanged() {
        if (mDeviceEnableSetting.affectsCellular()) {
            updateRadioPower(TriggerEvent.DEVICE_ENABLE_CHANGED);
        }
    }
    @Override
    public void onProxyConnectedChange(boolean isProxyConnected) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "updateProxyConnected: " + isProxyConnected);
        }
        mIsProxyConnected = isProxyConnected;
        updateRadioPower(TriggerEvent.PROXY_CONNECTED_UPDATE);
    }

    /** Trigger mediator update due to Wifi connection status change. */
    public void onWifiConnectedChange(boolean isWifiConnected) {
        Log.d(TAG, "WIFI connection state changed: isWifiConnected=" + isWifiConnected);
        mIsWifiConnected = isWifiConnected;
        if (shouldTurnCellularOffWhenWifiConnected()) {
            TriggerEvent triggerEvent =
                    isWifiConnected
                            ? TriggerEvent.WIFI_CONNECTED_UPDATE
                            : TriggerEvent.WIFI_DISCONNECTED_UPDATE;
            updateRadioPower(triggerEvent);
        }
    }

    public void updateNumHighBandwidthRequests(int numHighBandwidthRequests) {
        mNumHighBandwidthRequests = numHighBandwidthRequests;
        updateRadioPower(TriggerEvent.NUM_HIGH_BANDWIDTH_REQ_UPDATE);
    }

    public void updateNumCellularRequests(int numCellularRequests) {
        mNumCellularRequests = numCellularRequests;
        updateRadioPower(TriggerEvent.NUM_CELLULAR_REQ_UPDATE);
    }

    private void updateDetectorState(int radioOnState) {
        // Turn off signal strength tracking when cell radio is off
        // If RADIO_ON_STATE_UNKNOWN, should assume it's ON.
        if (radioOnState != RADIO_ON_STATE_OFF && mSettings.getMobileSignalDetectorAllowed()) {
            mSignalStateDetector.startDetector();
        } else {
            mSignalStateDetector.stopDetector();
            // Reset back to the default state.
            mSignalState = SignalStateModel.STATE_OK_SIGNAL;
        }
    }

    @Override
    public void onSignalStateChanged(int signalState) {
        mSignalState = signalState;
        updateRadioPower(TriggerEvent.SIGNAL_STATE_CHANGED);
    }

    private void updateRadioPower(TriggerEvent triggerEvent) {
        if (!mBooted) {
            Log.d(TAG, "Ignoring request to update radio power, device not fully booted");
            return;
        }

        if (mIsInTelephonyCall) {
            changeRadioPower(true, Reason.ON_PHONE_CALL, triggerEvent);
        } else if (mIsInEcbm || mIsInEmergencyCall || mIsRadioOnAfterEmergencyCall) {
            changeRadioPower(true, Reason.ON_EMERGENCY, triggerEvent);
        } else if (!mIsUserUnlocked) {
            changeRadioPower(false, Reason.OFF_DIRECTBOOT, triggerEvent);
        } else if (mSettings.isWearEsimDevice() && mIsInEsimTestMode) {
            changeRadioPower(true, Reason.ON_ESIM_TEST_MODE, triggerEvent);
        } else if (mSettings.isWearEsimDevice() && !mSettings.isLocalEditionDevice()
                && mIsEsimProfileDeactivated) {
            changeRadioPower(false, Reason.OFF_ESIM_DEACTIVATED, triggerEvent);
        } else if (mCellOnlyMode) {
            changeRadioPower(true, Reason.ON_CELL_ONLY_MODE, triggerEvent);
        } else if (mDeviceEnableSetting.affectsCellular()
                && !mDeviceEnableSetting.isDeviceEnabled()) {
            changeRadioPower(false, Reason.OFF_DEVICE_DISABLED, triggerEvent);
        } else if (mIsThermalEmergency) {
            changeRadioPower(false, Reason.OFF_THERMAL_EMERGENCY, triggerEvent);
        } else if (mPowerTracker.isDeviceIdle() && mUserAbsentRadiosOff.isEnabled()
                && !mPowerTracker.getDozeModeAllowListedFeatures().get(
                    PowerTracker.DOZE_MODE_CELLULAR_INDEX)) {
            changeRadioPower(false, Reason.OFF_USER_ABSENT, triggerEvent);
        } else if (mCellState != PhoneConstants.CELL_ON_FLAG) {
            changeRadioPower(false, Reason.OFF_CELL_SETTING, triggerEvent);
        } else if (mCellAuto != CELL_AUTO_ON && mCellState == PhoneConstants.CELL_ON_FLAG) {
            changeRadioPower(true, Reason.ON_NO_CELL_AUTO, triggerEvent);
        } else if (mSettings.isWearEsimDevice() && ICC_WEAR_INITIAL_BOOT.equals(mIccState)) {
            changeRadioPower(false, Reason.OFF_INITIAL_BOOT, triggerEvent);
        } else if (mSettings.isWearEsimDevice() && !mHasActiveEsimSubscription &&
                !deviceHasPsimLoaded()) {
            // when there's no active eSIM, shut off the radio unless there's an active pSIM
            changeRadioPower(false, Reason.OFF_SIM_ABSENT, triggerEvent);
        } else if (!mSettings.isWearEsimDevice() && IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(
                mIccState)) {
            changeRadioPower(false, Reason.OFF_SIM_ABSENT, triggerEvent);
        } else if (mActivityMode) {
            changeRadioPower(false, Reason.OFF_ACTIVITY_MODE, triggerEvent);
        } else if (mSettings.shouldTurnCellularOffDuringPowerSave()
                && mPowerTracker.isInPowerSave()) {
            changeRadioPower(false, Reason.OFF_POWER_SAVE, triggerEvent);
        } else if (mNumHighBandwidthRequests > 0 || mNumCellularRequests > 0) {
            changeRadioPower(true, Reason.ON_NETWORK_REQUEST, triggerEvent);
        } else if (mSignalStateDetector.isStarted() && mSignalState == STATE_NO_SIGNAL) {
            changeRadioPower(false, Reason.OFF_NO_SIGNAL, triggerEvent);
        } else if (mSignalStateDetector.isStarted() && mSignalState == STATE_UNSTABLE_SIGNAL) {
            changeRadioPower(false, Reason.OFF_UNSTABLE_SIGNAL, triggerEvent);
        } else if (!mIsProxyConnected
                && shouldTurnCellularOffWhenWifiConnected()
                && mIsWifiConnected) {
            changeRadioPower(false, Reason.OFF_WIFI_CONNECTED, triggerEvent);
        } else if (!mIsProxyConnected) {
            if (mShouldDelayNextCellOn && mCellAuto == CELL_AUTO_ON) {
                delayCellEnable(Reason.ON_PROXY_DISCONNECTED, triggerEvent);
            } else {
                changeRadioPower(true, Reason.ON_PROXY_DISCONNECTED, triggerEvent);
            }
        } else {
            changeRadioPower(false, Reason.OFF_PROXY_CONNECTED, triggerEvent);
        }
    }

    /**
     * Even though Wear devices ship with eSIM only and are accordingly marked as isWearEsimDevice,
     * a subset of these devices in labs have pSIM AOBs that the software needs to support.
     *
     * This condition checks for the presence of a pSIM so that the radio doesn't get forced off
     * when no eSIM profile is installed.
     */
    private boolean deviceHasPsimLoaded() {
        return IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(mIccState)
                || IccCardConstants.INTENT_VALUE_ICC_READY.equals(mIccState);
    }

    private void delayCellEnable(Reason reason, TriggerEvent triggerEvent) {
        mShouldDelayNextCellOn = false;

        Log.d(TAG, String.format("Delay cell %sms for reason: %s:%s",
                CELL_DELAY_MS, reason, triggerEvent));

        mExitCellDelayAlarm = () -> {
            if (mExitCellDelayAlarm == null) {
                // delay just cancelled but callback already queued
                return;
            }
            Log.d(TAG, "Leaving cell delay state");
            mAlarmManager.cancel(mExitCellDelayAlarm);
            mExitCellDelayAlarm = null;
            mHandler.sendMessage(Message.obtain(mHandler, MSG_ENABLE_CELL,
                    new RadioStateChangeReqInfo(Reason.ON_PROXY_DISCONNECTED,
                    triggerEvent)));
        };

        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + CELL_DELAY_MS, "CellMediatorDelay",
                mExitCellDelayAlarm, null);
    }

    private void changeRadioPower(boolean enable, Reason reason, TriggerEvent triggerEvent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, String.format("changeRadioPower: %s %s:%s",
                    enable, reason.name(), triggerEvent.name()));
        }

        if (mExitCellDelayAlarm != null && reason != Reason.OFF_INITIAL_BOOT
                && reason != Reason.ON_PROXY_DISCONNECTED) {
            mAlarmManager.cancel(mExitCellDelayAlarm);
            mExitCellDelayAlarm = null;
        }

        if (enable) {
            mAlarmManager.cancel(exitCellLingerIntent);
            mCellLingering = false;
            mCellLingeringReason = null;
            mCellLingeringTriggerEvent = null;
            if (mExitCellDelayAlarm != null) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "changeRadioPower: inside delay");
                }
            } else {
                mHandler.sendMessage(Message.obtain(mHandler, MSG_ENABLE_CELL,
                        new RadioStateChangeReqInfo(reason, triggerEvent)));
            }
        } else if (shouldLingerCellRadio(reason)) {
            // if we're already lingering, then scheduling another alarm is redundant
            if (!mCellLingering) {
                Log.d(TAG, "Entering cell linger state for reason: " + reason + ":" + triggerEvent);
                mAlarmManager.cancel(exitCellLingerIntent);
                mCellLingering = true;
                mCellLingeringReason = reason;
                mCellLingeringTriggerEvent = triggerEvent;
                mAlarmManager.setWindow(AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + mCellLingerDurationMs,
                        MAX_ACCEPTABLE_LINGER_DELAY_MS,
                        exitCellLingerIntent);
            }
        } else {
            mHandler.sendMessage(Message.obtain(mHandler, MSG_DISABLE_CELL,
                    new RadioStateChangeReqInfo(reason, triggerEvent)));
        }

        mShouldDelayNextCellOn =
            reason == Reason.OFF_POWER_SAVE
            || reason == Reason.OFF_THERMAL_EMERGENCY
            || reason == Reason.OFF_USER_ABSENT
            || (reason == Reason.OFF_CELL_SETTING && mCellAuto == CELL_AUTO_ON);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "changeRadioPower: next "
                    + (mShouldDelayNextCellOn ? "should delay" : "no delay"));
        }
    }

    // for now, only linger cell radio for BT proxy disconnects
    private boolean shouldLingerCellRadio(Reason reason) {
        return mCellLingerDurationMs > 0 && Reason.OFF_PROXY_CONNECTED.equals(reason);
    }

    private boolean shouldTurnCellularOffWhenWifiConnected() {
        return mSettings.shouldTurnCellularOffWhenWifiConnected() || mIsVoWifiEnabled;
    }

    private boolean isVoWifiEnabled(int slotIndex) {
        ImsManager imsManager =
                mImsManagerForTesting == null
                        ? ImsManager.getInstance(mContext, slotIndex)
                        : mImsManagerForTesting;
        return imsManager.isWfcEnabledByPlatform()
                && imsManager.isWfcEnabledByUser()
                && imsManager.isWfcProvisionedOnDevice();
    }

    /**
     * Ensure that the default value for ENABLE_CELLULAR_ON_BOOT is set to false if Cell Mediator
     * is enabled or if cell setting is off.  See b/195719623
     */
    private void ensureCellOnRebootBehaviorIsCorrect(int cellAutoState, int cellOnState) {
        if (cellAutoState == CELL_AUTO_ON || cellOnState == PhoneConstants.CELL_OFF_FLAG) {
            Settings.Global.putInt(
                    mContext.getContentResolver(), Global.ENABLE_CELLULAR_ON_BOOT, 0);
        }
    }

    public void dump(IndentingPrintWriter ipw) {
        ipw.println("======== WearCellularMediator ========");
        ipw.printPair("radioOnState", mSettings.getRadioOnState());
        ipw.printPair("mCellState", mCellState);
        ipw.printPair("mCellAuto", mCellAuto);
        ipw.println();
        ipw.printPair("mDeviceEnabled", mDeviceEnableSetting.isDeviceEnabled());
        ipw.printPair("deviceEnableAffectsCellular", mDeviceEnableSetting.affectsCellular());
        ipw.println();
        ipw.printPair("mCellOnlyMode", mCellOnlyMode);
        ipw.printPair("isEsimDevice", mSettings.isWearEsimDevice());
        if (mSettings.isWearEsimDevice()) {
            ipw.increaseIndent();
            ipw.println("eSIM Device Info");
            ipw.printPair("mEid", mEuiccManager.getEid());
            ipw.printPair("mHasActiveEsimSubscription", mHasActiveEsimSubscription);
            ipw.println();
            ipw.printPair("mEsimProfileDeactivated", mIsEsimProfileDeactivated);
            ipw.printPair("mIsInEsimTestMode", mIsInEsimTestMode);
            ipw.decreaseIndent();
        }
        ipw.println();
        ipw.printPair("mIsInTelephonyCall", mIsInTelephonyCall);
        ipw.printPair("mIsInEcbm", mIsInEcbm);
        ipw.printPair("mIsInEmergencyCall", mIsInEmergencyCall);
        ipw.printPair("mIsRadioOnAfterEmergencyCall", mIsRadioOnAfterEmergencyCall);
        ipw.printPair("mIccState", mIccState);
        ipw.printPair("mIsWifiConnected", mIsWifiConnected);
        ipw.printPair("mIsVoWifiEnabled", mIsVoWifiEnabled);
        ipw.println();
        ipw.printPair("Voice Twinning", mSettings.isVoiceTwinningEnabled());
        ipw.printPair("Text Twinning", mSettings.isTextTwinningEnabled());
        ipw.println();
        ipw.printPair("mActivityMode", mActivityMode);
        ipw.printPair("mIsThermalEmergency", mIsThermalEmergency);
        ipw.printPair("mCellLingering", mCellLingering);
        ipw.printPair("mCellLingerDurationMs", mCellLingerDurationMs);
        ipw.println();
        ipw.printPair("Allowed during doze mode",
                mPowerTracker.getDozeModeAllowListedFeatures()
                        .get(PowerTracker.DOZE_MODE_CELLULAR_INDEX));
        ipw.println();

        mSignalStateDetector.dump(ipw);
        ipw.println();
        mHistory.dump(ipw);
        ipw.println();
    }

    /** The reason that cellular radio power changed */
    public enum Reason {
        UNKNOWN,
        OFF_INITIAL_BOOT,
        OFF_ACTIVITY_MODE,
        OFF_CELL_SETTING,
        OFF_ESIM_DEACTIVATED,
        OFF_NO_SIGNAL,
        OFF_POWER_SAVE,
        OFF_PROXY_CONNECTED,
        OFF_SIM_ABSENT,
        OFF_UNSTABLE_SIGNAL,
        OFF_USER_ABSENT,
        OFF_DEVICE_DISABLED,
        OFF_THERMAL_EMERGENCY,
        OFF_DIRECTBOOT,
        OFF_WIFI_CONNECTED,
        ON_NETWORK_REQUEST,
        ON_NO_CELL_AUTO,
        ON_PHONE_CALL,
        ON_PROXY_DISCONNECTED,
        ON_CELL_ONLY_MODE,
        ON_ESIM_TEST_MODE,
        ON_EMERGENCY,
    }

    public enum TriggerEvent {
        UNKNOWN,
        ACTIVITY_MODE_UPDATE,
        CALL_STATE_CHANGED_EMERGENCY_ENDED,
        CALL_STATE_CHANGED_EMERGENCY_ENDED_DELAYED,
        CELL_AUTO_SETTING,
        CELL_ON_SETTING,
        CELL_ONLY_MODE_UPDATE,
        CONNECTION_STATE_CHANGED_PDN_DISCONNECTED,
        DEVICE_ENABLE_CHANGED,
        DEVICE_IDLE_MODE_CHANGED,
        EMERGENCY_CALLBACK_MODE_CHANGED,
        EMERGENCY_RADIO_ON_TIMEOUT,
        ENABLE_CELLULAR_ON_BOOT_SETTING,
        ESIM_PROFILE_ACTIVATION_SETTING,
        ESIM_TEST_MODE,
        MOBILE_SIGNAL_DETECTOR_SETTING,
        NEW_OUTGOING_CALL,
        NUM_CELLULAR_REQ_UPDATE,
        NUM_HIGH_BANDWIDTH_REQ_UPDATE,
        ON_BOOT_COMPLETED,
        PROXY_CONNECTED_UPDATE,
        SIGNAL_STATE_CHANGED,
        SIM_STATE_CHANGED,
        SUBSCRIPTION_PHONE_STATE_CHANGED,
        SUBSCRIPTIONS_CHANGED,
        POWER_SAVE_MODE_CHANGED,
        USER_ABSENT_RADIOS_OFF_CHANGED,
        THERMAL_EMERGENCY_MODE_UPDATE,
        ON_USER_UNLOCKED,
        WIFI_CONNECTED_UPDATE,
        WIFI_DISCONNECTED_UPDATE,
        VOWIFI_STATE_UPDATE,
    }

    /** The information regarding why a radio power change was requested. */
    public static class RadioStateChangeReqInfo {
        public final Reason mReason;
        public final TriggerEvent mTriggerEvent;

        public RadioStateChangeReqInfo(Reason reason, TriggerEvent triggerEvent) {
            mReason = reason;
            mTriggerEvent = triggerEvent;
        }
    }

    /** The decision reason cellular radio power changes */
    public static class CellDecision extends EventHistory.Event {
        public final Reason reason;
        public final TriggerEvent triggerEvent;

        public CellDecision(Reason reason, TriggerEvent triggerEvent) {
            this.reason = reason;
            this.triggerEvent = triggerEvent;
        }

        @Override
        public String getName() {
            return reason.name() + ":" + triggerEvent.name();
        }

        @Override
        public boolean isDuplicateOf(EventHistory.Event event) {
            if (!(event instanceof CellDecision)) {
                return false;
            }
            CellDecision that = (CellDecision) event;
            // Only compare the reason and not the trigger event.
            return reason == that.reason;
        }
    }

    private class RadioPowerHandler extends Handler {
        public RadioPowerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "handleMessage: " + msg);
            }

            switch (msg.what) {
                case MSG_DISABLE_CELL:
                    // fall through
                case MSG_ENABLE_CELL:
                    boolean enable = (msg.what == MSG_ENABLE_CELL);
                    RadioStateChangeReqInfo req = (RadioStateChangeReqInfo) msg.obj;
                    Reason reason = Reason.UNKNOWN;
                    TriggerEvent triggerEvent = TriggerEvent.UNKNOWN;
                    if (req != null) {
                        reason = req.mReason;
                        triggerEvent = req.mTriggerEvent;
                    }

                    // Ensure that the cellular package state matches the cellular radio state.
                    mWearConnectivityPackageManager.onCellularRadioState(enable);

                    // It could be tempting to check current radio state and skip the request
                    // to Telephony for changing radio power. But see b/256281886 for issues.
                    mTelephonyManager.setRadioPower(enable);
                    // Log the radio change event.
                    final CellDecision decision = new CellDecision(reason, triggerEvent);
                    EventLog.writeEvent(
                            EventLogTags.CELL_RADIO_POWER_CHANGE_EVENT,
                            enable ? RADIO_ON_STATE_ON : RADIO_ON_STATE_OFF,
                            decision.getName(),
                            decision.getTimestampMs());
                    Log.i(TAG, decision.getName() + " changed radio power: " + enable);
                    mHistory.recordEvent(decision);

                    // don't use getRadioOnState here - not waiting till setRadioPower actually turns
                    // radio on/off
                    updateDetectorState(enable ? RADIO_ON_STATE_ON : RADIO_ON_STATE_OFF);

                    try {
                        synchronized (mLock) {
                            // Block the thread to ensure the service state is changed.
                            // Short timeout that is enough for the radio power toggle.
                            mLock.wait(WAIT_FOR_SET_RADIO_POWER_IN_MS);
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, "wait() interrupted!", e);
                    }
                    break;
                case MSG_EMERGENCY_RADIO_ON_TIMEOUT:
                    Log.d(TAG, "evaluate radio state after timeout after emergency call ends.");
                    mIsRadioOnAfterEmergencyCall = false;
                    updateRadioPower(TriggerEvent.EMERGENCY_RADIO_ON_TIMEOUT);
                    break;
                default: // fall out
            }
        }
    }
}

/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.telephony.emergency;

import static android.telecom.Connection.STATE_ACTIVE;
import static android.telecom.Connection.STATE_DISCONNECTED;
import static android.telephony.CarrierConfigManager.ImsEmergency.KEY_EMERGENCY_CALLBACK_MODE_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_BROADCAST_EMERGENCY_CALL_STATE_CHANGES_BOOL;

import static com.android.internal.telephony.TelephonyIntents.ACTION_EMERGENCY_CALL_STATE_CHANGED;
import static com.android.internal.telephony.emergency.EmergencyConstants.MODE_EMERGENCY_CALLBACK;
import static com.android.internal.telephony.emergency.EmergencyConstants.MODE_EMERGENCY_NONE;
import static com.android.internal.telephony.emergency.EmergencyConstants.MODE_EMERGENCY_WWAN;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.sysprop.TelephonyProperties;
import android.telephony.AccessNetworkConstants;
import android.telephony.Annotation.DisconnectCauses;
import android.telephony.CarrierConfigManager;
import android.telephony.DisconnectCause;
import android.telephony.EmergencyRegistrationResult;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArraySet;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.data.PhoneSwitcher;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.internal.telephony.satellite.SatelliteController;
import com.android.telephony.Rlog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Tracks the emergency call state and notifies listeners of changes to the emergency mode.
 */
public class EmergencyStateTracker {

    private static final String TAG = "EmergencyStateTracker";

    private static class OnDisconnectListener extends Connection.ListenerBase {
        private final CompletableFuture<Boolean> mFuture;

        OnDisconnectListener(CompletableFuture<Boolean> future) {
            mFuture = future;
        }

        @Override
        public void onDisconnect(int cause) {
            mFuture.complete(true);
        }
    };

    /**
     * Timeout before we continue with the emergency call without waiting for DDS switch response
     * from the modem.
     */
    private static final int DEFAULT_DATA_SWITCH_TIMEOUT_MS = 1 * 1000;
    @VisibleForTesting
    public static final int DEFAULT_WAIT_FOR_IN_SERVICE_TIMEOUT_MS = 3 * 1000;
    /** Default value for if Emergency Callback Mode is supported. */
    private static final boolean DEFAULT_EMERGENCY_CALLBACK_MODE_SUPPORTED = true;
    /** Default Emergency Callback Mode exit timeout value. */
    private static final long DEFAULT_ECM_EXIT_TIMEOUT_MS = 300000;

    private static final int DEFAULT_TRANSPORT_CHANGE_TIMEOUT_MS = 1 * 1000;

    // Timeout to wait for the termination of incoming call before continue with the emergency call.
    private static final int DEFAULT_REJECT_INCOMING_CALL_TIMEOUT_MS = 3 * 1000; // 3 seconds.

    /** The emergency types used when setting the emergency mode on modem. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "EMERGENCY_TYPE_",
            value = {
                    EMERGENCY_TYPE_CALL,
                    EMERGENCY_TYPE_SMS})
    public @interface EmergencyType {}

    /** Indicates the emergency type is call. */
    public static final int EMERGENCY_TYPE_CALL = 1;
    /** Indicates the emergency type is SMS. */
    public static final int EMERGENCY_TYPE_SMS = 2;

    private static final String KEY_NO_SIM_ECBM_SUPPORT = "no_sim_ecbm_support";

    private static EmergencyStateTracker INSTANCE = null;

    private final Context mContext;
    private final CarrierConfigManager mConfigManager;
    private final Handler mHandler;
    private final boolean mIsSuplDdsSwitchRequiredForEmergencyCall;
    private final PowerManager.WakeLock mWakeLock;
    private RadioOnHelper mRadioOnHelper;
    @EmergencyConstants.EmergencyMode
    private int mEmergencyMode = MODE_EMERGENCY_NONE;
    private boolean mWasEmergencyModeSetOnModem;
    private EmergencyRegistrationResult mLastEmergencyRegistrationResult;
    private boolean mIsEmergencyModeInProgress;
    private boolean mIsEmergencyCallStartedDuringEmergencySms;
    private boolean mIsWaitingForRadioOff;

    /** For emergency calls */
    private final long mEcmExitTimeoutMs;
    // A runnable which is used to automatically exit from Ecm after a period of time.
    private final Runnable mExitEcmRunnable = this::exitEmergencyCallbackMode;
    // Tracks emergency calls by callId that have reached {@link Call.State#ACTIVE}.
    private final Set<android.telecom.Connection> mActiveEmergencyCalls = new ArraySet<>();
    private Phone mPhone;
    // Tracks ongoing emergency connection to handle a second emergency call
    private android.telecom.Connection mOngoingConnection;
    // Domain of the active emergency call. Assuming here that there will only be one domain active.
    private int mEmergencyCallDomain = NetworkRegistrationInfo.DOMAIN_UNKNOWN;
    private CompletableFuture<Integer> mCallEmergencyModeFuture;
    private boolean mIsInEmergencyCall;
    private boolean mIsInEcm;
    private boolean mIsTestEmergencyNumber;
    private Runnable mOnEcmExitCompleteRunnable;
    private int mOngoingCallProperties;
    private boolean mSentEmergencyCallState;
    private android.telecom.Connection mNormalRoutingEmergencyConnection;

    /** For emergency SMS */
    private final Set<String> mOngoingEmergencySmsIds = new ArraySet<>();
    private Phone mSmsPhone;
    private CompletableFuture<Integer> mSmsEmergencyModeFuture;
    private boolean mIsTestEmergencyNumberForSms;
    // For tracking the emergency SMS callback mode.
    private boolean mIsInScbm;
    private boolean mIsEmergencySmsStartedDuringScbm;

    private CompletableFuture<Boolean> mEmergencyTransportChangedFuture;
    private final Object mRegistrantidentifier = new Object();

    private final android.util.ArrayMap<Integer, Boolean> mNoSimEcbmSupported =
            new android.util.ArrayMap<>();
    private final android.util.ArrayMap<Integer, Boolean> mBroadcastEmergencyCallStateChanges =
            new android.util.ArrayMap<>();
    private final CarrierConfigManager.CarrierConfigChangeListener mCarrierConfigChangeListener =
            (slotIndex, subId, carrierId, specificCarrierId) -> onCarrierConfigurationChanged(
                    slotIndex, subId);

    /**
     * Listens for Emergency Callback Mode state change intents
     */
    private final BroadcastReceiver mEcmExitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED)) {

                boolean isInEcm = intent.getBooleanExtra(
                        TelephonyManager.EXTRA_PHONE_IN_ECM_STATE, false);
                Rlog.d(TAG, "Received ACTION_EMERGENCY_CALLBACK_MODE_CHANGED isInEcm = " + isInEcm);

                // If we exit ECM mode, notify all connections.
                if (!isInEcm) {
                    exitEmergencyCallbackMode();
                }
            }
        }
    };

    /** PhoneFactory Dependencies for testing. */
    @VisibleForTesting
    public interface PhoneFactoryProxy {
        Phone[] getPhones();
    }

    private PhoneFactoryProxy mPhoneFactoryProxy = PhoneFactory::getPhones;

    /** PhoneSwitcher dependencies for testing. */
    @VisibleForTesting
    public interface PhoneSwitcherProxy {

        PhoneSwitcher getPhoneSwitcher();
    }

    private PhoneSwitcherProxy mPhoneSwitcherProxy = PhoneSwitcher::getInstance;

    /**
     * TelephonyManager dependencies for testing.
     */
    @VisibleForTesting
    public interface TelephonyManagerProxy {
        int getPhoneCount();
        int getSimState(int slotIndex);
    }

    private final TelephonyManagerProxy mTelephonyManagerProxy;

    private static class TelephonyManagerProxyImpl implements TelephonyManagerProxy {
        private final TelephonyManager mTelephonyManager;

        TelephonyManagerProxyImpl(Context context) {
            mTelephonyManager = new TelephonyManager(context);
        }

        @Override
        public int getPhoneCount() {
            return mTelephonyManager.getActiveModemCount();
        }

        @Override
        public int getSimState(int slotIndex) {
            return mTelephonyManager.getSimState(slotIndex);
        }
    }

    /**
     * Return the handler for testing.
     */
    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }

    @VisibleForTesting
    public static final int MSG_SET_EMERGENCY_MODE_DONE = 1;
    @VisibleForTesting
    public static final int MSG_EXIT_EMERGENCY_MODE_DONE = 2;
    @VisibleForTesting
    public static final int MSG_SET_EMERGENCY_CALLBACK_MODE_DONE = 3;
    /** A message which is used to automatically exit from SCBM after a period of time. */
    private static final int MSG_EXIT_SCBM = 4;
    @VisibleForTesting
    public static final int MSG_NEW_RINGING_CONNECTION = 5;
    @VisibleForTesting
    public static final int MSG_VOICE_REG_STATE_CHANGED = 6;

    private class MyHandler extends Handler {

        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_EMERGENCY_MODE_DONE: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Integer emergencyType = (Integer) ar.userObj;
                    Rlog.v(TAG, "MSG_SET_EMERGENCY_MODE_DONE for "
                            + emergencyTypeToString(emergencyType));
                    if (ar.exception == null) {
                        mLastEmergencyRegistrationResult = (EmergencyRegistrationResult) ar.result;
                    } else {
                        mLastEmergencyRegistrationResult = null;
                        Rlog.w(TAG,
                                "LastEmergencyRegistrationResult not set. AsyncResult.exception: "
                                + ar.exception);
                    }
                    setEmergencyModeInProgress(false);

                    // Transport changed from WLAN to WWAN or CALLBACK to WWAN
                    maybeNotifyTransportChangeCompleted(emergencyType, false);

                    if (emergencyType == EMERGENCY_TYPE_CALL) {
                        setIsInEmergencyCall(true);
                        completeEmergencyMode(emergencyType);

                        // Case 1) When the emergency call is setting the emergency mode and
                        // the emergency SMS is being sent, completes the SMS future also.
                        // Case 2) When the emergency SMS is setting the emergency mode and
                        // the emergency call is being started, the SMS request is cancelled and
                        // the call request will be handled.
                        if (mSmsPhone != null) {
                            completeEmergencyMode(EMERGENCY_TYPE_SMS);
                        }
                    } else if (emergencyType == EMERGENCY_TYPE_SMS) {
                        if (mPhone != null && mSmsPhone != null) {
                            if (mIsEmergencyCallStartedDuringEmergencySms) {
                                if (!isSamePhone(mPhone, mSmsPhone) || !isInScbm()) {
                                    // Clear call phone temporarily to exit the emergency mode
                                    // if the emergency call is started.
                                    Phone phone = mPhone;
                                    mPhone = null;
                                    exitEmergencyMode(mSmsPhone, emergencyType);
                                    // Restore call phone for further use.
                                    mPhone = phone;
                                    if (!isSamePhone(mPhone, mSmsPhone)) {
                                        completeEmergencyMode(emergencyType,
                                                DisconnectCause.OUTGOING_EMERGENCY_CALL_PLACED);
                                        exitEmergencySmsCallbackMode();
                                    }
                                } else {
                                    completeEmergencyMode(emergencyType);
                                    mIsEmergencyCallStartedDuringEmergencySms = false;
                                    exitEmergencySmsCallbackMode();
                                    turnOnRadioAndSwitchDds(mPhone, EMERGENCY_TYPE_CALL,
                                            mIsTestEmergencyNumber);
                                }
                            } else {
                                completeEmergencyMode(emergencyType);
                            }
                        } else {
                            completeEmergencyMode(emergencyType);

                            if (mIsEmergencyCallStartedDuringEmergencySms) {
                                mIsEmergencyCallStartedDuringEmergencySms = false;
                                exitEmergencySmsCallbackMode();
                                turnOnRadioAndSwitchDds(mPhone, EMERGENCY_TYPE_CALL,
                                        mIsTestEmergencyNumber);
                            }
                        }
                    }
                    break;
                }
                case MSG_EXIT_EMERGENCY_MODE_DONE: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Integer emergencyType = (Integer) ar.userObj;
                    Rlog.v(TAG, "MSG_EXIT_EMERGENCY_MODE_DONE for "
                            + emergencyTypeToString(emergencyType));
                    setEmergencyModeInProgress(false);

                    if (emergencyType == EMERGENCY_TYPE_CALL) {
                        setIsInEmergencyCall(false);
                        if (mOnEcmExitCompleteRunnable != null) {
                            mOnEcmExitCompleteRunnable.run();
                            mOnEcmExitCompleteRunnable = null;
                        }
                        if (mPhone != null && mEmergencyMode == MODE_EMERGENCY_WWAN) {
                            // In cross sim redialing.
                            setEmergencyModeInProgress(true);
                            mWasEmergencyModeSetOnModem = true;
                            mPhone.setEmergencyMode(MODE_EMERGENCY_WWAN,
                                    mHandler.obtainMessage(MSG_SET_EMERGENCY_MODE_DONE,
                                    Integer.valueOf(EMERGENCY_TYPE_CALL)));
                        }
                    } else if (emergencyType == EMERGENCY_TYPE_SMS) {
                        if (mIsEmergencyCallStartedDuringEmergencySms) {
                            mIsEmergencyCallStartedDuringEmergencySms = false;
                            turnOnRadioAndSwitchDds(mPhone, EMERGENCY_TYPE_CALL,
                                    mIsTestEmergencyNumber);
                        } else if (mPhone != null && mEmergencyMode == MODE_EMERGENCY_WWAN) {
                            // Starting emergency call while exiting emergency mode
                            setEmergencyModeInProgress(true);
                            mWasEmergencyModeSetOnModem = true;
                            mPhone.setEmergencyMode(MODE_EMERGENCY_WWAN,
                                    mHandler.obtainMessage(MSG_SET_EMERGENCY_MODE_DONE,
                                    Integer.valueOf(EMERGENCY_TYPE_CALL)));
                        } else if (mIsEmergencySmsStartedDuringScbm) {
                            mIsEmergencySmsStartedDuringScbm = false;
                            setEmergencyMode(mSmsPhone, emergencyType,
                                    MODE_EMERGENCY_WWAN, MSG_SET_EMERGENCY_MODE_DONE);
                        }
                    }
                    break;
                }
                case MSG_SET_EMERGENCY_CALLBACK_MODE_DONE: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Integer emergencyType = (Integer) ar.userObj;
                    Rlog.v(TAG, "MSG_SET_EMERGENCY_CALLBACK_MODE_DONE for "
                            + emergencyTypeToString(emergencyType));
                    setEmergencyModeInProgress(false);
                    // When the emergency callback mode is in progress and the emergency SMS is
                    // started, it needs to be completed here for the emergency SMS.
                    if (emergencyType == EMERGENCY_TYPE_CALL) {
                        if (mSmsPhone != null) {
                            completeEmergencyMode(EMERGENCY_TYPE_SMS);
                        }
                    } else if (emergencyType == EMERGENCY_TYPE_SMS) {
                        // When the emergency SMS callback mode is in progress on other phone and
                        // the emergency call was started, needs to exit the emergency mode first.
                        if (mIsEmergencyCallStartedDuringEmergencySms) {
                            final Phone smsPhone = mSmsPhone;
                            exitEmergencySmsCallbackMode();

                            if (mPhone != null && smsPhone != null
                                    && !isSamePhone(mPhone, smsPhone)) {
                                Phone phone = mPhone;
                                mPhone = null;
                                exitEmergencyMode(smsPhone, emergencyType);
                                // Restore call phone for further use.
                                mPhone = phone;
                            } else {
                                mIsEmergencyCallStartedDuringEmergencySms = false;
                                turnOnRadioAndSwitchDds(mPhone, EMERGENCY_TYPE_CALL,
                                        mIsTestEmergencyNumber);
                            }
                        }
                    }
                    break;
                }
                case MSG_EXIT_SCBM: {
                    exitEmergencySmsCallbackModeAndEmergencyMode();
                    break;
                }
                case MSG_NEW_RINGING_CONNECTION: {
                    handleNewRingingConnection(msg);
                    break;
                }
                case MSG_VOICE_REG_STATE_CHANGED: {
                    if (mIsWaitingForRadioOff && isPowerOff()) {
                        unregisterForVoiceRegStateOrRatChanged();
                        if (mPhone != null) {
                            turnOnRadioAndSwitchDds(mPhone, EMERGENCY_TYPE_CALL,
                                    mIsTestEmergencyNumber);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    /**
     * Creates the EmergencyStateTracker singleton instance.
     *
     * @param context                                 The context of the application.
     * @param isSuplDdsSwitchRequiredForEmergencyCall Whether gnss supl requires default data for
     *                                                emergency call.
     */
    public static void make(Context context, boolean isSuplDdsSwitchRequiredForEmergencyCall) {
        if (INSTANCE == null) {
            INSTANCE = new EmergencyStateTracker(context, Looper.myLooper(),
                    isSuplDdsSwitchRequiredForEmergencyCall);
        }
    }

    /**
     * Returns the singleton instance of EmergencyStateTracker.
     *
     * @return {@link EmergencyStateTracker} instance.
     */
    public static EmergencyStateTracker getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("EmergencyStateTracker is not ready!");
        }
        return INSTANCE;
    }

    /**
     * Initializes EmergencyStateTracker.
     */
    private EmergencyStateTracker(Context context, Looper looper,
            boolean isSuplDdsSwitchRequiredForEmergencyCall) {
        mEcmExitTimeoutMs = DEFAULT_ECM_EXIT_TIMEOUT_MS;
        mContext = context;
        mHandler = new MyHandler(looper);
        mIsSuplDdsSwitchRequiredForEmergencyCall = isSuplDdsSwitchRequiredForEmergencyCall;

        PowerManager pm = context.getSystemService(PowerManager.class);
        mWakeLock = (pm != null) ? pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "telephony:" + TAG) : null;
        mConfigManager = context.getSystemService(CarrierConfigManager.class);
        if (mConfigManager != null) {
            // Carrier config changed callback should be executed in handler thread
            mConfigManager.registerCarrierConfigChangeListener(mHandler::post,
                    mCarrierConfigChangeListener);
        } else {
            Rlog.e(TAG, "CarrierConfigLoader is not available.");
        }

        // Register receiver for ECM exit.
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        context.registerReceiver(mEcmExitReceiver, filter, null, mHandler);
        mTelephonyManagerProxy = new TelephonyManagerProxyImpl(context);

        registerForNewRingingConnection();

        // To recover the abnormal state after crash of com.android.phone process
        maybeResetEmergencyCallStateChangedIntent();
    }

    /**
     * Initializes EmergencyStateTracker with injections for testing.
     *
     * @param context                                 The context of the application.
     * @param looper                                  The {@link Looper} of the application.
     * @param isSuplDdsSwitchRequiredForEmergencyCall Whether gnss supl requires default data for
     *                                                emergency call.
     * @param phoneFactoryProxy                       The {@link PhoneFactoryProxy} to be injected.
     * @param phoneSwitcherProxy                      The {@link PhoneSwitcherProxy} to be injected.
     * @param telephonyManagerProxy                   The {@link TelephonyManagerProxy} to be
     *                                                injected.
     * @param radioOnHelper                           The {@link RadioOnHelper} to be injected.
     */
    @VisibleForTesting
    public EmergencyStateTracker(Context context, Looper looper,
            boolean isSuplDdsSwitchRequiredForEmergencyCall, PhoneFactoryProxy phoneFactoryProxy,
            PhoneSwitcherProxy phoneSwitcherProxy, TelephonyManagerProxy telephonyManagerProxy,
            RadioOnHelper radioOnHelper, long ecmExitTimeoutMs) {
        mContext = context;
        mHandler = new MyHandler(looper);
        mIsSuplDdsSwitchRequiredForEmergencyCall = isSuplDdsSwitchRequiredForEmergencyCall;
        mPhoneFactoryProxy = phoneFactoryProxy;
        mPhoneSwitcherProxy = phoneSwitcherProxy;
        mTelephonyManagerProxy = telephonyManagerProxy;
        mRadioOnHelper = radioOnHelper;
        mEcmExitTimeoutMs = ecmExitTimeoutMs;
        mWakeLock = null; // Don't declare a wakelock in tests
        mConfigManager = context.getSystemService(CarrierConfigManager.class);
        mConfigManager.registerCarrierConfigChangeListener(mHandler::post,
                mCarrierConfigChangeListener);
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        context.registerReceiver(mEcmExitReceiver, filter, null, mHandler);
        registerForNewRingingConnection();
    }

    /**
     * Starts the process of an emergency call.
     *
     * <p>
     * Handles turning on radio and switching DDS.
     *
     * @param phone                 the {@code Phone} on which to process the emergency call.
     * @param c                     the {@code Connection} on which to process the emergency call.
     * @param isTestEmergencyNumber whether this is a test emergency number.
     * @return a {@code CompletableFuture} that results in {@code DisconnectCause.NOT_DISCONNECTED}
     *         if emergency call successfully started.
     */
    public CompletableFuture<Integer> startEmergencyCall(@NonNull Phone phone,
            @NonNull android.telecom.Connection c, boolean isTestEmergencyNumber) {
        Rlog.i(TAG, "startEmergencyCall: phoneId=" + phone.getPhoneId()
                + ", callId=" + c.getTelecomCallId());

        if (mPhone != null) {
            // Create new future to return as to not interfere with any uncompleted futures.
            // Case1) When 2nd emergency call is initiated during an active call on the same phone.
            // Case2) While the device is in ECBM, an emergency call is initiated on the same phone.
            if (isSamePhone(mPhone, phone) && (!mActiveEmergencyCalls.isEmpty() || isInEcm())) {
                exitEmergencySmsCallbackMode();
                mOngoingConnection = c;
                mIsTestEmergencyNumber = isTestEmergencyNumber;
                if (isInEcm()) {
                    // Remove pending exit ECM runnable.
                    mHandler.removeCallbacks(mExitEcmRunnable);
                    releaseWakeLock();
                    ((GsmCdmaPhone) mPhone).notifyEcbmTimerReset(Boolean.TRUE);

                    mOngoingCallProperties = 0;
                    mCallEmergencyModeFuture = new CompletableFuture<>();
                    setEmergencyMode(mPhone, EMERGENCY_TYPE_CALL, MODE_EMERGENCY_WWAN,
                            MSG_SET_EMERGENCY_MODE_DONE);
                    return mCallEmergencyModeFuture;
                }
                // Ensure that domain selector requests scan.
                mLastEmergencyRegistrationResult = new EmergencyRegistrationResult(
                        AccessNetworkConstants.AccessNetworkType.UNKNOWN,
                        NetworkRegistrationInfo.REGISTRATION_STATE_UNKNOWN,
                        NetworkRegistrationInfo.DOMAIN_UNKNOWN, false, false, 0, 0, "", "", "");
                return CompletableFuture.completedFuture(DisconnectCause.NOT_DISCONNECTED);
            }

            Rlog.e(TAG, "startEmergencyCall failed. Existing emergency call in progress.");
            return CompletableFuture.completedFuture(DisconnectCause.ERROR_UNSPECIFIED);
        }

        mOngoingCallProperties = 0;
        mCallEmergencyModeFuture = new CompletableFuture<>();

        if (mSmsPhone != null) {
            mIsEmergencyCallStartedDuringEmergencySms = true;
            // Case1) While exiting the emergency mode on the other phone,
            // the emergency mode for this call will be restarted after the exit complete.
            // Case2) While entering the emergency mode on the other phone,
            // exit the emergency mode when receiving the result of setting the emergency mode and
            // the emergency mode for this call will be restarted after the exit complete.
            if (isInEmergencyMode() && !isEmergencyModeInProgress()) {
                if (!isSamePhone(mSmsPhone, phone)) {
                    exitEmergencyMode(mSmsPhone, EMERGENCY_TYPE_SMS);
                } else {
                    // If the device is already in the emergency mode on the same phone,
                    // the general emergency call procedure can be immediately performed.
                    // And, if the emergency PDN is already connected, then we need to keep
                    // this PDN active while initating the emergency call.
                    mIsEmergencyCallStartedDuringEmergencySms = false;
                }

                exitEmergencySmsCallbackMode();
            }

            if (mIsEmergencyCallStartedDuringEmergencySms) {
                mPhone = phone;
                mOngoingConnection = c;
                mIsTestEmergencyNumber = isTestEmergencyNumber;
                sendEmergencyCallStateChange(mPhone, true);
                maybeRejectIncomingCall(null);
                return mCallEmergencyModeFuture;
            }
        }

        mPhone = phone;
        mOngoingConnection = c;
        mIsTestEmergencyNumber = isTestEmergencyNumber;
        sendEmergencyCallStateChange(mPhone, true);
        final android.telecom.Connection expectedConnection = mOngoingConnection;
        maybeRejectIncomingCall(result -> {
            Rlog.i(TAG, "maybeRejectIncomingCall : result = " + result);
            if (!Objects.equals(mOngoingConnection, expectedConnection)) {
                Rlog.i(TAG, "maybeRejectIncomingCall "
                        + expectedConnection.getTelecomCallId() + " canceled.");
                return;
            }
            turnOnRadioAndSwitchDds(mPhone, EMERGENCY_TYPE_CALL, mIsTestEmergencyNumber);
        });
        return mCallEmergencyModeFuture;
    }

    /**
     * Ends emergency call.
     *
     * <p>
     * Enter ECM only once all active emergency calls have ended. If a call never reached
     * {@link Call.State#ACTIVE}, then no need to enter ECM.
     *
     * @param c the emergency call disconnected.
     */
    public void endCall(@NonNull android.telecom.Connection c) {
        boolean wasActive = mActiveEmergencyCalls.remove(c);

        if (Objects.equals(mOngoingConnection, c)) {
            mOngoingConnection = null;
            mOngoingCallProperties = 0;
            sendEmergencyCallStateChange(mPhone, false);
            unregisterForVoiceRegStateOrRatChanged();
        }

        if (wasActive && mActiveEmergencyCalls.isEmpty()
                && isEmergencyCallbackModeSupported(mPhone)) {
            enterEmergencyCallbackMode();

            if (mOngoingConnection == null) {
                mIsEmergencyCallStartedDuringEmergencySms = false;
                mCallEmergencyModeFuture = null;
            }
        } else if (mOngoingConnection == null) {
            if (isInEcm()) {
                mIsEmergencyCallStartedDuringEmergencySms = false;
                mCallEmergencyModeFuture = null;
                // If the emergency call was initiated during the emergency callback mode,
                // the emergency callback mode should be restored when the emergency call is ended.
                if (mActiveEmergencyCalls.isEmpty()) {
                    enterEmergencyCallbackMode();
                }
            } else {
                if (isInScbm()) {
                    setIsInEmergencyCall(false);
                    setEmergencyCallbackMode(mSmsPhone, EMERGENCY_TYPE_SMS);
                } else {
                    exitEmergencyMode(mPhone, EMERGENCY_TYPE_CALL);
                }
                clearEmergencyCallInfo();
            }
        }

        // Release any blocked thread immediately
        maybeNotifyTransportChangeCompleted(EMERGENCY_TYPE_CALL, true);
    }

    private void clearEmergencyCallInfo() {
        mEmergencyCallDomain = NetworkRegistrationInfo.DOMAIN_UNKNOWN;
        mIsTestEmergencyNumber = false;
        mIsEmergencyCallStartedDuringEmergencySms = false;
        mCallEmergencyModeFuture = null;
        mOngoingConnection = null;
        mOngoingCallProperties = 0;
        mPhone = null;
    }

    private void switchDdsAndSetEmergencyMode(Phone phone, @EmergencyType int emergencyType) {
        switchDdsDelayed(phone, result -> {
            Rlog.i(TAG, "switchDdsDelayed: result = " + result);
            if (!result) {
                // DDS Switch timed out/failed, but continue with call as it may still succeed.
                Rlog.e(TAG, "DDS Switch failed.");
            }
            // Once radio is on and DDS switched, must call setEmergencyMode() before selecting
            // emergency domain. EmergencyRegistrationResult is required to determine domain and
            // this is the only API that can receive it before starting domain selection.
            // Once domain selection is finished, the actual emergency mode will be set when
            // onEmergencyTransportChanged() is called.
            if (mEmergencyMode != MODE_EMERGENCY_WWAN) {
                setEmergencyMode(phone, emergencyType, MODE_EMERGENCY_WWAN,
                        MSG_SET_EMERGENCY_MODE_DONE);
            } else {
                // Ensure that domain selector requests the network scan.
                mLastEmergencyRegistrationResult = new EmergencyRegistrationResult(
                        AccessNetworkConstants.AccessNetworkType.UNKNOWN,
                        NetworkRegistrationInfo.REGISTRATION_STATE_UNKNOWN,
                        NetworkRegistrationInfo.DOMAIN_UNKNOWN, false, false, 0, 0, "", "", "");
                if (emergencyType == EMERGENCY_TYPE_CALL) {
                    setIsInEmergencyCall(true);
                }
                completeEmergencyMode(emergencyType);
            }
        });
    }

    /**
     * Triggers modem to set new emergency mode.
     *
     * @param phone the {@code Phone} to set the emergency mode on modem.
     * @param emergencyType the emergency type to identify an emergency call or SMS.
     * @param mode the new emergency mode.
     * @param msg the message to be sent once mode has been set.
     */
    private void setEmergencyMode(Phone phone, @EmergencyType int emergencyType,
            @EmergencyConstants.EmergencyMode int mode, int msg) {
        Rlog.i(TAG, "setEmergencyMode from " + mEmergencyMode + " to " + mode + " for "
                + emergencyTypeToString(emergencyType));

        if (mEmergencyMode == mode) {
            // Initial transport selection of DomainSelector
            maybeNotifyTransportChangeCompleted(emergencyType, false);
            return;
        }

        if (emergencyType == EMERGENCY_TYPE_CALL
                && mode == MODE_EMERGENCY_WWAN
                && isEmergencyModeInProgress() && !isInEmergencyMode()) {
            // In cross sim redialing or ending emergency SMS, exitEmergencyMode is not completed.
            mEmergencyMode = mode;
            Rlog.i(TAG, "setEmergencyMode wait for the completion of exitEmergencyMode");
            return;
        }

        mEmergencyMode = mode;
        setEmergencyModeInProgress(true);

        Message m = mHandler.obtainMessage(msg, Integer.valueOf(emergencyType));
        if (mIsTestEmergencyNumberForSms && emergencyType == EMERGENCY_TYPE_SMS) {
            Rlog.d(TAG, "TestEmergencyNumber for " + emergencyTypeToString(emergencyType)
                    + ": Skipping setting emergency mode on modem.");
            // Send back a response for the command, but with null information
            AsyncResult.forMessage(m, null, null);
            // Ensure that we do not accidentally block indefinitely when trying to validate test
            // emergency numbers
            m.sendToTarget();
            return;
        }

        mWasEmergencyModeSetOnModem = true;
        phone.setEmergencyMode(mode, m);
    }

    /**
     * Sets the emergency callback mode on modem.
     *
     * @param phone the {@code Phone} to set the emergency mode on modem.
     * @param emergencyType the emergency type to identify an emergency call or SMS.
     */
    private void setEmergencyCallbackMode(Phone phone, @EmergencyType int emergencyType) {
        boolean needToSetCallbackMode = false;

        if (emergencyType == EMERGENCY_TYPE_CALL) {
            needToSetCallbackMode = true;
        } else if (emergencyType == EMERGENCY_TYPE_SMS) {
            // Ensure that no emergency call is in progress.
            if (mActiveEmergencyCalls.isEmpty() && mOngoingConnection == null
                    && mOngoingEmergencySmsIds.isEmpty()) {
                needToSetCallbackMode = true;
            }
        }

        if (needToSetCallbackMode) {
            // Set emergency mode on modem.
            setEmergencyMode(phone, emergencyType, MODE_EMERGENCY_CALLBACK,
                    MSG_SET_EMERGENCY_CALLBACK_MODE_DONE);
        }
    }

    private void completeEmergencyMode(@EmergencyType int emergencyType) {
        completeEmergencyMode(emergencyType, DisconnectCause.NOT_DISCONNECTED);
    }

    private void completeEmergencyMode(@EmergencyType int emergencyType,
            @DisconnectCauses int result) {
        if (emergencyType == EMERGENCY_TYPE_CALL) {
            if (mCallEmergencyModeFuture != null && !mCallEmergencyModeFuture.isDone()) {
                mCallEmergencyModeFuture.complete(result);
            }

            if (result != DisconnectCause.NOT_DISCONNECTED) {
                clearEmergencyCallInfo();
            }
        } else if (emergencyType == EMERGENCY_TYPE_SMS) {
            if (mSmsEmergencyModeFuture != null && !mSmsEmergencyModeFuture.isDone()) {
                mSmsEmergencyModeFuture.complete(result);
            }

            if (result != DisconnectCause.NOT_DISCONNECTED) {
                clearEmergencySmsInfo();
            }
        }
    }

    /**
     * Checks if the device is currently in the emergency mode or not.
     */
    @VisibleForTesting
    public boolean isInEmergencyMode() {
        return mEmergencyMode != MODE_EMERGENCY_NONE;
    }

    /**
     * Sets the flag to inidicate whether setting the emergency mode on modem is in progress or not.
     */
    private void setEmergencyModeInProgress(boolean isEmergencyModeInProgress) {
        mIsEmergencyModeInProgress = isEmergencyModeInProgress;
    }

    /**
     * Checks whether setting the emergency mode on modem is in progress or not.
     */
    private boolean isEmergencyModeInProgress() {
        return mIsEmergencyModeInProgress;
    }

    /**
     * Notifies external app listeners of emergency mode changes.
     *
     * @param isInEmergencyCall a flag to indicate whether there is an active emergency call.
     */
    private void setIsInEmergencyCall(boolean isInEmergencyCall) {
        mIsInEmergencyCall = isInEmergencyCall;
    }

    /**
     * Checks if there is an ongoing emergency call.
     *
     * @return true if in emergency call
     */
    public boolean isInEmergencyCall() {
        return mIsInEmergencyCall;
    }

    /**
     * Triggers modem to exit emergency mode.
     *
     * @param phone the {@code Phone} to exit the emergency mode.
     * @param emergencyType the emergency type to identify an emergency call or SMS.
     */
    private void exitEmergencyMode(Phone phone, @EmergencyType int emergencyType) {
        Rlog.i(TAG, "exitEmergencyMode for " + emergencyTypeToString(emergencyType));

        if (emergencyType == EMERGENCY_TYPE_CALL) {
            if (mSmsPhone != null && isSamePhone(phone, mSmsPhone)) {
                // Waits for exiting the emergency mode until the emergency SMS is ended.
                Rlog.i(TAG, "exitEmergencyMode: waits for emergency SMS end.");
                setIsInEmergencyCall(false);
                return;
            }
        } else if (emergencyType == EMERGENCY_TYPE_SMS) {
            if (mPhone != null && isSamePhone(phone, mPhone)) {
                // Waits for exiting the emergency mode until the emergency call is ended.
                Rlog.i(TAG, "exitEmergencyMode: waits for emergency call end.");
                return;
            }
        }

        if (mEmergencyMode == MODE_EMERGENCY_NONE) {
            return;
        }
        mEmergencyMode = MODE_EMERGENCY_NONE;
        setEmergencyModeInProgress(true);

        Message m = mHandler.obtainMessage(
                MSG_EXIT_EMERGENCY_MODE_DONE, Integer.valueOf(emergencyType));
        if (!mWasEmergencyModeSetOnModem) {
            Rlog.d(TAG, "Emergency mode was not set on modem: Skipping exiting emergency mode.");
            // Send back a response for the command, but with null information
            AsyncResult.forMessage(m, null, null);
            // Ensure that we do not accidentally block indefinitely when trying to validate
            // the exit condition.
            m.sendToTarget();
            return;
        }

        mWasEmergencyModeSetOnModem = false;
        phone.exitEmergencyMode(m);
    }

    /** Returns last {@link EmergencyRegistrationResult} as set by {@code setEmergencyMode()}. */
    public EmergencyRegistrationResult getEmergencyRegistrationResult() {
        return mLastEmergencyRegistrationResult;
    }

    private void waitForTransportChangeCompleted(CompletableFuture<Boolean> future) {
        if (future != null) {
            synchronized (future) {
                if ((mEmergencyMode == MODE_EMERGENCY_NONE)
                        || mHandler.getLooper().isCurrentThread()) {
                    // Do not block the Handler's thread
                    return;
                }
                long now = SystemClock.elapsedRealtime();
                long deadline = now + DEFAULT_TRANSPORT_CHANGE_TIMEOUT_MS;
                // Guard with while loop to handle spurious wakeups
                while (!future.isDone() && now < deadline) {
                    try {
                        future.wait(deadline - now);
                    } catch (Exception e) {
                        Rlog.e(TAG, "waitForTransportChangeCompleted wait e=" + e);
                    }
                    now = SystemClock.elapsedRealtime();
                }
            }
        }
    }

    private void maybeNotifyTransportChangeCompleted(@EmergencyType int emergencyType,
            boolean enforced) {
        if (emergencyType != EMERGENCY_TYPE_CALL) {
            // It's not for the emergency call
            return;
        }
        CompletableFuture<Boolean> future = mEmergencyTransportChangedFuture;
        if (future != null) {
            synchronized (future) {
                if (!future.isDone()
                        && ((!isEmergencyModeInProgress() && mEmergencyMode == MODE_EMERGENCY_WWAN)
                                || enforced)) {
                    future.complete(Boolean.TRUE);
                    future.notifyAll();
                }
            }
        }
    }

    /**
     * Handles emergency transport change by setting new emergency mode.
     *
     * @param emergencyType the emergency type to identify an emergency call or SMS
     * @param mode the new emergency mode
     */
    public void onEmergencyTransportChangedAndWait(@EmergencyType int emergencyType,
            @EmergencyConstants.EmergencyMode int mode) {
        // Wait for the completion of setting MODE_EMERGENCY_WWAN only for emergency calls
        if (emergencyType == EMERGENCY_TYPE_CALL && mode == MODE_EMERGENCY_WWAN) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            synchronized (future) {
                mEmergencyTransportChangedFuture = future;
                onEmergencyTransportChanged(emergencyType, mode);
                waitForTransportChangeCompleted(future);
            }
            return;
        }
        onEmergencyTransportChanged(emergencyType, mode);
    }

    /**
     * Handles emergency transport change by setting new emergency mode.
     *
     * @param emergencyType the emergency type to identify an emergency call or SMS
     * @param mode the new emergency mode
     */
    public void onEmergencyTransportChanged(@EmergencyType int emergencyType,
            @EmergencyConstants.EmergencyMode int mode) {
        if (mHandler.getLooper().isCurrentThread()) {
            Phone phone = null;
            if (emergencyType == EMERGENCY_TYPE_CALL) {
                phone = mPhone;
            } else if (emergencyType == EMERGENCY_TYPE_SMS) {
                phone = mSmsPhone;
            }

            if (phone != null) {
                setEmergencyMode(phone, emergencyType, mode, MSG_SET_EMERGENCY_MODE_DONE);
            }
        } else {
            mHandler.post(() -> {
                onEmergencyTransportChanged(emergencyType, mode);
            });
        }
    }

    /**
     * Notify the tracker that the emergency call domain has been updated.
     * @param phoneType The new PHONE_TYPE_* of the call.
     * @param c The connection of the call
     */
    public void onEmergencyCallDomainUpdated(int phoneType, android.telecom.Connection c) {
        Rlog.d(TAG, "domain update for callId: " + c.getTelecomCallId());
        int domain = -1;
        switch(phoneType) {
            case (PhoneConstants.PHONE_TYPE_CDMA_LTE):
                //fallthrough
            case (PhoneConstants.PHONE_TYPE_GSM):
                //fallthrough
            case (PhoneConstants.PHONE_TYPE_CDMA): {
                domain = NetworkRegistrationInfo.DOMAIN_CS;
                break;
            }
            case (PhoneConstants.PHONE_TYPE_IMS): {
                domain = NetworkRegistrationInfo.DOMAIN_PS;
                break;
            }
            default: {
                Rlog.w(TAG, "domain updated: Unexpected phoneType:" + phoneType);
            }
        }
        if (mEmergencyCallDomain == domain) return;
        Rlog.i(TAG, "domain updated: from " + mEmergencyCallDomain + " to " + domain);
        mEmergencyCallDomain = domain;
    }

    /**
     * Handles emergency call state change.
     *
     * @param state the new call state
     * @param c the call whose state has changed
     */
    public void onEmergencyCallStateChanged(Call.State state, android.telecom.Connection c) {
        if (state == Call.State.ACTIVE) {
            mActiveEmergencyCalls.add(c);
            if (Objects.equals(mOngoingConnection, c)) {
                Rlog.i(TAG, "call connected " + c.getTelecomCallId());
                if (mPhone != null
                        && isVoWiFi(mOngoingCallProperties)
                        && mEmergencyMode == EmergencyConstants.MODE_EMERGENCY_WLAN) {
                    // Recover normal service in cellular when VoWiFi is connected
                    mPhone.cancelEmergencyNetworkScan(true, null);
                }
            }
        }
    }

    /**
     * Handles the change of emergency call properties.
     *
     * @param properties the new call properties.
     * @param c the call whose state has changed.
     */
    public void onEmergencyCallPropertiesChanged(int properties, android.telecom.Connection c) {
        if (Objects.equals(mOngoingConnection, c)) {
            mOngoingCallProperties = properties;
        }
    }

    /**
     * Handles the radio power off request.
     */
    public void onCellularRadioPowerOffRequested() {
        exitEmergencySmsCallbackModeAndEmergencyMode();
        exitEmergencyCallbackMode();
    }

    private static boolean isVoWiFi(int properties) {
        return (properties & android.telecom.Connection.PROPERTY_WIFI) > 0
                || (properties & android.telecom.Connection.PROPERTY_CROSS_SIM) > 0;
    }

    /**
     * Returns {@code true} if device and carrier support emergency callback mode.
     *
     * @param phone The {@link Phone} instance to be checked.
     */
    @VisibleForTesting
    public boolean isEmergencyCallbackModeSupported(Phone phone) {
        if (phone == null) {
            return false;
        }
        int subId = phone.getSubId();
        int phoneId = phone.getPhoneId();
        if (!isSimReady(phoneId, subId)) {
            // If there is no SIM, refer to the saved last carrier configuration with valid
            // subscription.
            Boolean savedConfig = mNoSimEcbmSupported.get(Integer.valueOf(phoneId));
            if (savedConfig == null) {
                // Exceptional case such as with poor boot performance.
                // Usually, the first carrier config change will update the cache.
                // But with poor boot performance, the carrier config change
                // can be delayed for a long time.
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
                savedConfig = Boolean.valueOf(
                        sp.getBoolean(KEY_NO_SIM_ECBM_SUPPORT + phoneId, false));
                Rlog.i(TAG, "ECBM value not cached, load from preference");
                mNoSimEcbmSupported.put(Integer.valueOf(phoneId), savedConfig);
            }
            Rlog.i(TAG, "isEmergencyCallbackModeSupported savedConfig=" + savedConfig);
            return savedConfig;
        } else {
            return getConfig(subId,
                    CarrierConfigManager.ImsEmergency.KEY_EMERGENCY_CALLBACK_MODE_SUPPORTED_BOOL,
                    DEFAULT_EMERGENCY_CALLBACK_MODE_SUPPORTED);
        }
    }

    /**
     * Trigger entry into emergency callback mode.
     */
    private void enterEmergencyCallbackMode() {
        Rlog.d(TAG, "enter ECBM");
        setIsInEmergencyCall(false);
        // Check if not in ECM already.
        if (!isInEcm()) {
            setIsInEcm(true);
            if (!mPhone.getUnitTestMode()) {
                TelephonyProperties.in_ecm_mode(true);
            }

            // Notify listeners of the entrance to ECM.
            sendEmergencyCallbackModeChange();
            if (isInImsEcm()) {
                // emergency call registrants are not notified of new emergency call until entering
                // ECBM (see ImsPhone#handleEnterEmergencyCallbackMode)
                ((GsmCdmaPhone) mPhone).notifyEmergencyCallRegistrants(true);
            }
        } else {
            // Inform to reset the ECBM timer.
            ((GsmCdmaPhone) mPhone).notifyEcbmTimerReset(Boolean.FALSE);
        }

        setEmergencyCallbackMode(mPhone, EMERGENCY_TYPE_CALL);

        // Post this runnable so we will automatically exit if no one invokes
        // exitEmergencyCallbackMode() directly.
        long delayInMillis = TelephonyProperties.ecm_exit_timer()
                .orElse(mEcmExitTimeoutMs);
        mHandler.postDelayed(mExitEcmRunnable, delayInMillis);

        // We don't want to go to sleep while in ECM.
        if (mWakeLock != null) mWakeLock.acquire(delayInMillis);
    }

    /**
     * Exits emergency callback mode and notifies relevant listeners.
     */
    public void exitEmergencyCallbackMode() {
        Rlog.d(TAG, "exit ECBM");
        // Remove pending exit ECM runnable, if any.
        mHandler.removeCallbacks(mExitEcmRunnable);

        if (isInEcm()) {
            setIsInEcm(false);
            if (!mPhone.getUnitTestMode()) {
                TelephonyProperties.in_ecm_mode(false);
            }

            // Release wakeLock.
            releaseWakeLock();

            GsmCdmaPhone gsmCdmaPhone = (GsmCdmaPhone) mPhone;
            // Send intents that ECM has changed.
            sendEmergencyCallbackModeChange();
            gsmCdmaPhone.notifyEmergencyCallRegistrants(false);

            // Exit emergency mode on modem.
            exitEmergencyMode(gsmCdmaPhone, EMERGENCY_TYPE_CALL);
        }

        mEmergencyCallDomain = NetworkRegistrationInfo.DOMAIN_UNKNOWN;
        mIsTestEmergencyNumber = false;
        mPhone = null;
    }

    private void releaseWakeLock() {
        // Release wakeLock.
        if (mWakeLock != null && mWakeLock.isHeld()) {
            try {
                mWakeLock.release();
            } catch (Exception e) {
                // Ignore the exception if the system has already released this WakeLock.
                Rlog.d(TAG, "WakeLock already released: " + e.toString());
            }
        }
    }

    /**
     * Exits emergency callback mode and triggers runnable after exit response is received.
     */
    public void exitEmergencyCallbackMode(Runnable onComplete) {
        mOnEcmExitCompleteRunnable = onComplete;
        exitEmergencyCallbackMode();
    }

    /**
     * Sends intents that emergency callback mode changed.
     */
    private void sendEmergencyCallbackModeChange() {
        Rlog.d(TAG, "sendEmergencyCallbackModeChange: isInEcm=" + isInEcm());

        Intent intent = new Intent(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        intent.putExtra(TelephonyManager.EXTRA_PHONE_IN_ECM_STATE, isInEcm());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
        mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    /**
     * Returns {@code true} if currently in emergency callback mode.
     *
     * <p>
     * This is a period where the phone should be using as little power as possible and be ready to
     * receive an incoming call from the emergency operator.
     */
    public boolean isInEcm() {
        return mIsInEcm;
    }

    /**
     * Sets the emergency callback mode state.
     *
     * @param isInEcm {@code true} if currently in emergency callback mode, {@code false} otherwise.
     */
    private void setIsInEcm(boolean isInEcm) {
        mIsInEcm = isInEcm;
    }

    /**
     * Returns {@code true} if currently in emergency callback mode over PS
     */
    public boolean isInImsEcm() {
        return mEmergencyCallDomain == NetworkRegistrationInfo.DOMAIN_PS && isInEcm();
    }

    /**
     * Returns {@code true} if currently in emergency callback mode over CS
     */
    public boolean isInCdmaEcm() {
        // Phone can be null in the case where we are not actively tracking an emergency call.
        if (mPhone == null) return false;
        // Ensure that this method doesn't return true when we are attached to GSM.
        return mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA
                && mEmergencyCallDomain == NetworkRegistrationInfo.DOMAIN_CS && isInEcm();
    }

    /**
     * Returns {@code true} if currently in emergency callback mode with the given {@link Phone}.
     *
     * @param phone the {@link Phone} for the emergency call.
     */
    public boolean isInEcm(Phone phone) {
        return isInEcm() && isSamePhone(mPhone, phone);
    }

    private void sendEmergencyCallStateChange(Phone phone, boolean isAlive) {
        if ((isAlive && !mSentEmergencyCallState && getBroadcastEmergencyCallStateChanges(phone))
                || (!isAlive && mSentEmergencyCallState)) {
            mSentEmergencyCallState = isAlive;
            Rlog.i(TAG, "sendEmergencyCallStateChange: " + isAlive);
            Intent intent = new Intent(ACTION_EMERGENCY_CALL_STATE_CHANGED);
            intent.putExtra(TelephonyManager.EXTRA_PHONE_IN_EMERGENCY_CALL, isAlive);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, phone.getPhoneId());
            mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /**
     * Starts the process of an emergency SMS.
     *
     * @param phone the {@code Phone} on which to process the emergency SMS.
     * @param smsId the SMS id on which to process the emergency SMS.
     * @param isTestEmergencyNumber whether this is a test emergency number.
     * @return A {@code CompletableFuture} that results in {@code DisconnectCause.NOT_DISCONNECTED}
     *         if the emergency SMS is successfully started.
     */
    public CompletableFuture<Integer> startEmergencySms(@NonNull Phone phone, @NonNull String smsId,
            boolean isTestEmergencyNumber) {
        Rlog.i(TAG, "startEmergencySms: phoneId=" + phone.getPhoneId() + ", smsId=" + smsId
                + ", scbm=" + isInScbm());

        // When an emergency call is in progress, it checks whether an emergency call is already in
        // progress on the different phone.
        if (mPhone != null && !isSamePhone(mPhone, phone)) {
            Rlog.e(TAG, "Emergency call is in progress on the other slot.");
            return CompletableFuture.completedFuture(DisconnectCause.ERROR_UNSPECIFIED);
        }

        boolean exitScbmInOtherPhone = false;
        boolean smsStartedInScbm = isInScbm();

        // When an emergency SMS is in progress, it checks whether an emergency SMS is already
        // in progress on the different phone.
        if (mSmsPhone != null && !isSamePhone(mSmsPhone, phone)) {
            if (smsStartedInScbm) {
                // When other phone is in the emergency SMS callback mode, we need to stop the
                // emergency SMS callback mode first.
                exitScbmInOtherPhone = true;
                mIsEmergencySmsStartedDuringScbm = true;
                exitEmergencySmsCallbackModeAndEmergencyMode();
            } else {
                Rlog.e(TAG, "Emergency SMS is in progress on the other slot.");
                return CompletableFuture.completedFuture(DisconnectCause.ERROR_UNSPECIFIED);
            }
        }

        // When the previous emergency SMS is not completed yet and the device is not in SCBM,
        // this new request will not be allowed.
        if (mSmsPhone != null && isInEmergencyMode() && isEmergencyModeInProgress()
                && !smsStartedInScbm) {
            Rlog.e(TAG, "Existing emergency SMS is in progress and not in SCBM.");
            return CompletableFuture.completedFuture(DisconnectCause.ERROR_UNSPECIFIED);
        }

        mSmsPhone = phone;
        mIsTestEmergencyNumberForSms = isTestEmergencyNumber;
        mOngoingEmergencySmsIds.add(smsId);

        if (smsStartedInScbm) {
            // When the device is in SCBM and emergency SMS is being sent,
            // completes the future immediately.
            if (!exitScbmInOtherPhone) {
                // The emergency SMS is allowed and returns the success result.
                return CompletableFuture.completedFuture(DisconnectCause.NOT_DISCONNECTED);
            }

            mSmsEmergencyModeFuture = new CompletableFuture<>();
        } else {
            // When the emergency mode is already set by the previous emergency call or SMS,
            // completes the future immediately.
            if (isInEmergencyMode() && !isEmergencyModeInProgress()) {
                // The emergency SMS is allowed and returns the success result.
                return CompletableFuture.completedFuture(DisconnectCause.NOT_DISCONNECTED);
            }

            mSmsEmergencyModeFuture = new CompletableFuture<>();

            if (!isInEmergencyMode()) {
                setEmergencyMode(mSmsPhone, EMERGENCY_TYPE_SMS, MODE_EMERGENCY_WWAN,
                        MSG_SET_EMERGENCY_MODE_DONE);
            }
        }

        return mSmsEmergencyModeFuture;
    }

    /**
     * Ends an emergency SMS.
     * This should be called once an emergency SMS is sent.
     *
     * @param smsId the SMS id on which to end the emergency SMS.
     * @param success the flag specifying whether an emergency SMS is successfully sent or not.
     *                {@code true} if SMS is successfully sent, {@code false} otherwise.
     * @param domain the domain that MO SMS was sent.
     * @param isLastSmsPart the flag specifying whether this result is for the last SMS part or not.
     */
    public void endSms(@NonNull String smsId, boolean success,
            @NetworkRegistrationInfo.Domain int domain, boolean isLastSmsPart) {
        if (success && !isLastSmsPart) {
            // Waits until all SMS parts are sent successfully.
            // Ensures that all SMS parts are sent while in the emergency mode.
            Rlog.i(TAG, "endSms: wait for additional SMS parts to be sent.");
        } else {
            mOngoingEmergencySmsIds.remove(smsId);
        }

        // If the outgoing emergency SMSs are empty, we can try to exit the emergency mode.
        if (mOngoingEmergencySmsIds.isEmpty()) {
            mSmsEmergencyModeFuture = null;
            mIsEmergencySmsStartedDuringScbm = false;

            if (isInEcm()) {
                // When the emergency mode is not in MODE_EMERGENCY_CALLBACK,
                // it needs to notify the emergency callback mode to modem.
                if (mActiveEmergencyCalls.isEmpty() && mOngoingConnection == null) {
                    setEmergencyCallbackMode(mPhone, EMERGENCY_TYPE_CALL);
                }
            }

            // If SCBM supports, SCBM will be entered here regardless of ECBM state.
            if (success && domain == NetworkRegistrationInfo.DOMAIN_PS
                    && (isInScbm() || isEmergencyCallbackModeSupported(mSmsPhone))) {
                enterEmergencySmsCallbackMode();
            } else if (isInScbm()) {
                // Sets the emergency mode to CALLBACK without re-initiating SCBM timer.
                setEmergencyCallbackMode(mSmsPhone, EMERGENCY_TYPE_SMS);
            } else {
                if (mSmsPhone != null) {
                    exitEmergencyMode(mSmsPhone, EMERGENCY_TYPE_SMS);
                }
                clearEmergencySmsInfo();
            }
        }
    }

    /**
     * Called when emergency SMS is received from the network.
     */
    public void onEmergencySmsReceived() {
        if (isInScbm()) {
            Rlog.d(TAG, "Emergency SMS received, re-initiate SCBM timer");
            // Reinitiate the SCBM timer when receiving emergency SMS while in SCBM.
            enterEmergencySmsCallbackMode();
        }
    }

    private void clearEmergencySmsInfo() {
        mOngoingEmergencySmsIds.clear();
        mIsEmergencySmsStartedDuringScbm = false;
        mIsTestEmergencyNumberForSms = false;
        mSmsEmergencyModeFuture = null;
        mSmsPhone = null;
    }

    /**
     * Returns {@code true} if currently in emergency SMS callback mode.
     */
    public boolean isInScbm() {
        return mIsInScbm;
    }

    /**
     * Sets the emergency SMS callback mode state.
     *
     * @param isInScbm {@code true} if currently in emergency SMS callback mode,
     *                 {@code false} otherwise.
     */
    private void setIsInScbm(boolean isInScbm) {
        mIsInScbm = isInScbm;
    }

    /**
     * Enters the emergency SMS callback mode.
     */
    private void enterEmergencySmsCallbackMode() {
        Rlog.d(TAG, "enter SCBM while " + (isInScbm() ? "in" : "not in") + " SCBM");
        // Remove pending message if present.
        mHandler.removeMessages(MSG_EXIT_SCBM);

        if (!isInScbm()) {
            setIsInScbm(true);
        }

        setEmergencyCallbackMode(mSmsPhone, EMERGENCY_TYPE_SMS);

        // At the moment, the default SCBM timer value will be used with the same value
        // that is configured for emergency callback mode.
        int delayInMillis = Long.valueOf(mEcmExitTimeoutMs).intValue();
        int subId = mSmsPhone.getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            delayInMillis = getConfig(subId,
                    CarrierConfigManager.KEY_EMERGENCY_SMS_MODE_TIMER_MS_INT,
                    delayInMillis);
            if (delayInMillis == 0) {
                delayInMillis = Long.valueOf(mEcmExitTimeoutMs).intValue();
            }
        }
        // Post the message so we will automatically exit if no one invokes
        // exitEmergencySmsCallbackModeAndEmergencyMode() directly.
        mHandler.sendEmptyMessageDelayed(MSG_EXIT_SCBM, delayInMillis);
    }

    /**
     * Exits emergency SMS callback mode and emergency mode if the device is in SCBM and
     * the emergency mode is in CALLBACK.
     */
    private void exitEmergencySmsCallbackModeAndEmergencyMode() {
        Rlog.d(TAG, "exit SCBM and emergency mode");
        final Phone smsPhone = mSmsPhone;
        boolean wasInScbm = isInScbm();
        exitEmergencySmsCallbackMode();

        // The emergency mode needs to be checked to ensure that there is no ongoing emergency SMS.
        if (wasInScbm && mOngoingEmergencySmsIds.isEmpty()) {
            // Exit emergency mode on modem.
            exitEmergencyMode(smsPhone, EMERGENCY_TYPE_SMS);
        }
    }

    /**
     * Exits emergency SMS callback mode.
     */
    private void exitEmergencySmsCallbackMode() {
        // Remove pending message if present.
        mHandler.removeMessages(MSG_EXIT_SCBM);

        if (isInScbm()) {
            Rlog.i(TAG, "exit SCBM");
            setIsInScbm(false);
        }

        if (mOngoingEmergencySmsIds.isEmpty()) {
            mIsTestEmergencyNumberForSms = false;
            mSmsPhone = null;
        }
    }

    /**
     * Returns {@code true} if any phones from PhoneFactory have radio on.
     */
    private boolean isRadioOn() {
        boolean result = false;
        for (Phone phone : mPhoneFactoryProxy.getPhones()) {
            result |= phone.isRadioOn();
        }
        return result;
    }

    /**
     * Returns {@code true} if service states of all phones from PhoneFactory are radio off.
     */
    private boolean isPowerOff() {
        for (Phone phone : mPhoneFactoryProxy.getPhones()) {
            ServiceState ss = phone.getServiceStateTracker().getServiceState();
            if (ss.getState() != ServiceState.STATE_POWER_OFF) return false;
        }
        return true;
    }

    private void registerForVoiceRegStateOrRatChanged() {
        if (mIsWaitingForRadioOff) return;
        for (Phone phone : mPhoneFactoryProxy.getPhones()) {
            phone.getServiceStateTracker().registerForVoiceRegStateOrRatChanged(mHandler,
                    MSG_VOICE_REG_STATE_CHANGED, null);
        }
        mIsWaitingForRadioOff = true;
    }

    private void unregisterForVoiceRegStateOrRatChanged() {
        if (!mIsWaitingForRadioOff) return;
        for (Phone phone : mPhoneFactoryProxy.getPhones()) {
            phone.getServiceStateTracker().unregisterForVoiceRegStateOrRatChanged(mHandler);
        }
        mIsWaitingForRadioOff = false;
    }

    /**
     * Returns {@code true} if airplane mode is on.
     */
    private boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) > 0;
    }

    /**
     * Ensures that the radio is switched on and that DDS is switched for emergency call/SMS.
     *
     * <p>
     * Once radio is on and DDS switched, must call setEmergencyMode() before completing the future
     * and selecting emergency domain. EmergencyRegistrationResult is required to determine domain
     * and setEmergencyMode() is the only API that can receive it before starting domain selection.
     * Once domain selection is finished, the actual emergency mode will be set when
     * onEmergencyTransportChanged() is called.
     *
     * @param phone the {@code Phone} for the emergency call/SMS.
     * @param emergencyType the emergency type to identify an emergency call or SMS.
     * @param isTestEmergencyNumber a flag to inidicate whether the emergency call/SMS uses the test
     *                              emergency number.
     */
    private void turnOnRadioAndSwitchDds(Phone phone, @EmergencyType int emergencyType,
            boolean isTestEmergencyNumber) {
        final boolean isAirplaneModeOn = isAirplaneModeOn(mContext);
        boolean needToTurnOnRadio = !isRadioOn() || isAirplaneModeOn;
        final SatelliteController satelliteController = SatelliteController.getInstance();
        boolean needToTurnOffSatellite = satelliteController.isSatelliteEnabled();

        if (isAirplaneModeOn && !isPowerOff()
                && !phone.getServiceStateTracker().getDesiredPowerState()) {
            // power off is delayed to disconnect data connections
            Rlog.i(TAG, "turnOnRadioAndSwitchDds: wait for the delayed power off");
            registerForVoiceRegStateOrRatChanged();
            return;
        }

        if (needToTurnOnRadio || needToTurnOffSatellite) {
            Rlog.i(TAG, "turnOnRadioAndSwitchDds: phoneId=" + phone.getPhoneId() + " for "
                    + emergencyTypeToString(emergencyType));
            if (mRadioOnHelper == null) {
                mRadioOnHelper = new RadioOnHelper(mContext);
            }

            final Phone phoneForEmergency = phone;
            final android.telecom.Connection expectedConnection = mOngoingConnection;
            final int waitForInServiceTimeout =
                    needToTurnOnRadio ? DEFAULT_WAIT_FOR_IN_SERVICE_TIMEOUT_MS : 0;
            Rlog.i(TAG, "turnOnRadioAndSwitchDds: timeout=" + waitForInServiceTimeout);
            mRadioOnHelper.triggerRadioOnAndListen(new RadioOnStateListener.Callback() {
                @Override
                public void onComplete(RadioOnStateListener listener, boolean isRadioReady) {
                    if (!isRadioReady) {
                        if (satelliteController.isSatelliteEnabled()) {
                            // Could not turn satellite off
                            Rlog.e(TAG, "Failed to turn off satellite modem.");
                            completeEmergencyMode(emergencyType, DisconnectCause.SATELLITE_ENABLED);
                        } else {
                            // Could not turn radio on
                            Rlog.e(TAG, "Failed to turn on radio.");
                            completeEmergencyMode(emergencyType, DisconnectCause.POWER_OFF);
                        }
                    } else {
                        if (!Objects.equals(mOngoingConnection, expectedConnection)) {
                            Rlog.i(TAG, "onComplete "
                                    + expectedConnection.getTelecomCallId() + " canceled.");
                            return;
                        }
                        switchDdsAndSetEmergencyMode(phone, emergencyType);
                    }
                }

                @Override
                public boolean isOkToCall(Phone phone, int serviceState, boolean imsVoiceCapable) {
                    if (!Objects.equals(mOngoingConnection, expectedConnection)) {
                        Rlog.i(TAG, "isOkToCall "
                                + expectedConnection.getTelecomCallId() + " canceled.");
                        return true;
                    }
                    // Wait for normal service state or timeout if required.
                    if (phone == phoneForEmergency
                            && waitForInServiceTimeout > 0
                            && !isNetworkRegistered(phone)) {
                        return false;
                    }
                    return phone.getServiceStateTracker().isRadioOn()
                            && !satelliteController.isSatelliteEnabled();
                }

                @Override
                public boolean onTimeout(Phone phone, int serviceState, boolean imsVoiceCapable) {
                    if (!Objects.equals(mOngoingConnection, expectedConnection)) {
                        Rlog.i(TAG, "onTimeout "
                                + expectedConnection.getTelecomCallId() + " canceled.");
                        return true;
                    }
                    // onTimeout shall be called only with the Phone for emergency
                    return phone.getServiceStateTracker().isRadioOn()
                            && !satelliteController.isSatelliteEnabled();
                }
            }, !isTestEmergencyNumber, phone, isTestEmergencyNumber, waitForInServiceTimeout);
        } else {
            switchDdsAndSetEmergencyMode(phone, emergencyType);
        }
    }

    /**
     * If needed, block until the default data is switched for outgoing emergency call, or
     * timeout expires.
     *
     * @param phone            The Phone to switch the DDS on.
     * @param completeConsumer The consumer to call once the default data subscription has been
     *                         switched, provides {@code true} result if the switch happened
     *                         successfully or {@code false} if the operation timed out/failed.
     */
    @VisibleForTesting
    public void switchDdsDelayed(Phone phone, Consumer<Boolean> completeConsumer) {
        if (phone == null) {
            // Do not block indefinitely.
            completeConsumer.accept(false);
        }
        try {
            // Waiting for PhoneSwitcher to complete the operation.
            CompletableFuture<Boolean> future = possiblyOverrideDefaultDataForEmergencyCall(phone);
            // In the case that there is an issue or bug in PhoneSwitcher logic, do not wait
            // indefinitely for the future to complete. Instead, set a timeout that will complete
            // the future as to not block the outgoing call indefinitely.
            CompletableFuture<Boolean> timeout = new CompletableFuture<>();
            mHandler.postDelayed(() -> timeout.complete(false), DEFAULT_DATA_SWITCH_TIMEOUT_MS);
            // Also ensure that the Consumer is completed on the main thread.
            CompletableFuture<Void> unused = future.acceptEitherAsync(timeout, completeConsumer,
                    mHandler::post);
        } catch (Exception e) {
            Rlog.w(TAG, "switchDdsDelayed - exception= " + e.getMessage());
        }
    }

    /**
     * If needed, block until Default Data subscription is switched for outgoing emergency call.
     *
     * <p>
     * In some cases, we need to try to switch the Default Data subscription before placing the
     * emergency call on DSDS devices. This includes the following situation: - The modem does not
     * support processing GNSS SUPL requests on the non-default data subscription. For some carriers
     * that do not provide a control plane fallback mechanism, the SUPL request will be dropped and
     * we will not be able to get the user's location for the emergency call. In this case, we need
     * to swap default data temporarily.
     *
     * @param phone Evaluates whether or not the default data should be moved to the phone
     *              specified. Should not be null.
     */
    private CompletableFuture<Boolean> possiblyOverrideDefaultDataForEmergencyCall(
            @NonNull Phone phone) {
        int phoneCount = mTelephonyManagerProxy.getPhoneCount();
        // Do not override DDS if this is a single SIM device.
        if (phoneCount <= PhoneConstants.MAX_PHONE_COUNT_SINGLE_SIM) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        // Do not switch Default data if this device supports emergency SUPL on non-DDS.
        if (!mIsSuplDdsSwitchRequiredForEmergencyCall) {
            Rlog.d(TAG, "possiblyOverrideDefaultDataForEmergencyCall: not switching DDS, does not "
                    + "require DDS switch.");
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        // Only override default data if we are IN_SERVICE already.
        if (!isAvailableForEmergencyCalls(phone)) {
            Rlog.d(TAG, "possiblyOverrideDefaultDataForEmergencyCall: not switching DDS");
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        // Only override default data if we are not roaming, we do not want to switch onto a network
        // that only supports data plane only (if we do not know).
        boolean isRoaming = phone.getServiceState().getVoiceRoaming();
        // In some roaming conditions, we know the roaming network doesn't support control plane
        // fallback even though the home operator does. For these operators we will need to do a DDS
        // switch anyway to make sure the SUPL request doesn't fail.
        boolean roamingNetworkSupportsControlPlaneFallback = true;
        String[] dataPlaneRoamPlmns = getConfig(phone.getSubId(),
                CarrierConfigManager.Gps.KEY_ES_SUPL_DATA_PLANE_ONLY_ROAMING_PLMN_STRING_ARRAY);
        if (dataPlaneRoamPlmns != null && Arrays.asList(dataPlaneRoamPlmns)
                .contains(phone.getServiceState().getOperatorNumeric())) {
            roamingNetworkSupportsControlPlaneFallback = false;
        }
        if (isRoaming && roamingNetworkSupportsControlPlaneFallback) {
            Rlog.d(TAG, "possiblyOverrideDefaultDataForEmergencyCall: roaming network is assumed "
                    + "to support CP fallback, not switching DDS.");
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }
        // Do not try to swap default data if we support CS fallback or it is assumed that the
        // roaming network supports control plane fallback, we do not want to introduce a lag in
        // emergency call setup time if possible.
        final boolean supportsCpFallback = getConfig(phone.getSubId(),
                CarrierConfigManager.Gps.KEY_ES_SUPL_CONTROL_PLANE_SUPPORT_INT,
                CarrierConfigManager.Gps.SUPL_EMERGENCY_MODE_TYPE_CP_ONLY)
                != CarrierConfigManager.Gps.SUPL_EMERGENCY_MODE_TYPE_DP_ONLY;
        if (supportsCpFallback && roamingNetworkSupportsControlPlaneFallback) {
            Rlog.d(TAG, "possiblyOverrideDefaultDataForEmergencyCall: not switching DDS, carrier "
                    + "supports CP fallback.");
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        // Get extension time, may be 0 for some carriers that support ECBM as well. Use
        // CarrierConfig default if format fails.
        int extensionTime = 0;
        try {
            extensionTime = Integer.parseInt(getConfig(phone.getSubId(),
                    CarrierConfigManager.Gps.KEY_ES_EXTENSION_SEC_STRING, "0"));
        } catch (NumberFormatException e) {
            // Just use default.
        }
        CompletableFuture<Boolean> modemResultFuture = new CompletableFuture<>();
        try {
            Rlog.d(TAG, "possiblyOverrideDefaultDataForEmergencyCall: overriding DDS for "
                    + extensionTime + "seconds");
            mPhoneSwitcherProxy.getPhoneSwitcher().overrideDefaultDataForEmergency(
                    phone.getPhoneId(), extensionTime, modemResultFuture);
            // Catch all exceptions, we want to continue with emergency call if possible.
        } catch (Exception e) {
            Rlog.w(TAG,
                    "possiblyOverrideDefaultDataForEmergencyCall: exception = " + e.getMessage());
            modemResultFuture = CompletableFuture.completedFuture(Boolean.FALSE);
        }
        return modemResultFuture;
    }

    // Helper functions for easy CarrierConfigManager access
    private String getConfig(int subId, String key, String defVal) {
        return getConfigBundle(subId, key).getString(key, defVal);
    }
    private int getConfig(int subId, String key, int defVal) {
        return getConfigBundle(subId, key).getInt(key, defVal);
    }
    private String[] getConfig(int subId, String key) {
        return getConfigBundle(subId, key).getStringArray(key);
    }
    private boolean getConfig(int subId, String key, boolean defVal) {
        return getConfigBundle(subId, key).getBoolean(key, defVal);
    }
    private PersistableBundle getConfigBundle(int subId, String... keys) {
        if (mConfigManager == null) return new PersistableBundle();
        return mConfigManager.getConfigForSubId(subId, keys);
    }

    /**
     * Returns true if the state of the Phone is IN_SERVICE or available for emergency calling only.
     */
    private boolean isAvailableForEmergencyCalls(Phone phone) {
        return ServiceState.STATE_IN_SERVICE == phone.getServiceState().getState()
                || phone.getServiceState().isEmergencyOnly();
    }

    private static boolean isNetworkRegistered(Phone phone) {
        ServiceState ss = phone.getServiceStateTracker().getServiceState();
        if (ss != null) {
            NetworkRegistrationInfo nri = ss.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS,
                    AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (nri != null && nri.isNetworkRegistered()) {
                // PS is IN_SERVICE state.
                return true;
            }
            nri = ss.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_CS,
                    AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (nri != null && nri.isNetworkRegistered()) {
                // CS is IN_SERVICE state.
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether both {@code Phone}s are same or not.
     */
    private static boolean isSamePhone(Phone p1, Phone p2) {
        return p1 != null && p2 != null && (p1.getPhoneId() == p2.getPhoneId());
    }

    private static String emergencyTypeToString(@EmergencyType int emergencyType) {
        switch (emergencyType) {
            case EMERGENCY_TYPE_CALL: return "CALL";
            case EMERGENCY_TYPE_SMS: return "SMS";
            default: return "UNKNOWN";
        }
    }

    private void onCarrierConfigurationChanged(int slotIndex, int subId) {
        Rlog.i(TAG, "onCarrierConfigChanged slotIndex=" + slotIndex + ", subId=" + subId);

        if (slotIndex < 0) {
            return;
        }

        SharedPreferences sp = null;
        Boolean savedConfig = mNoSimEcbmSupported.get(Integer.valueOf(slotIndex));
        if (savedConfig == null) {
            sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            savedConfig = Boolean.valueOf(
                    sp.getBoolean(KEY_NO_SIM_ECBM_SUPPORT + slotIndex, false));
            mNoSimEcbmSupported.put(Integer.valueOf(slotIndex), savedConfig);
            Rlog.i(TAG, "onCarrierConfigChanged load from preference slotIndex=" + slotIndex
                    + ", ecbmSupported=" + savedConfig);
        }

        if (!isSimReady(slotIndex, subId)) {
            Rlog.i(TAG, "onCarrierConfigChanged SIM not ready");
            return;
        }

        PersistableBundle b = getConfigBundle(subId,
                KEY_EMERGENCY_CALLBACK_MODE_SUPPORTED_BOOL,
                KEY_BROADCAST_EMERGENCY_CALL_STATE_CHANGES_BOOL);
        if (b.isEmpty()) {
            Rlog.e(TAG, "onCarrierConfigChanged empty result");
            return;
        }

        if (!CarrierConfigManager.isConfigForIdentifiedCarrier(b)) {
            Rlog.i(TAG, "onCarrierConfigChanged not carrier specific configuration");
            return;
        }

        // KEY_BROADCAST_EMERGENCY_CALL_STATE_CHANGES_BOOL
        boolean broadcast = b.getBoolean(KEY_BROADCAST_EMERGENCY_CALL_STATE_CHANGES_BOOL);
        mBroadcastEmergencyCallStateChanges.put(
                Integer.valueOf(slotIndex), Boolean.valueOf(broadcast));

        Rlog.i(TAG, "onCarrierConfigChanged slotIndex=" + slotIndex
                + ", broadcastEmergencyCallStateChanges=" + broadcast);

        // KEY_EMERGENCY_CALLBACK_MODE_SUPPORTED_BOOL
        boolean carrierConfig = b.getBoolean(KEY_EMERGENCY_CALLBACK_MODE_SUPPORTED_BOOL);
        if (carrierConfig == savedConfig) {
            return;
        }

        mNoSimEcbmSupported.put(Integer.valueOf(slotIndex), Boolean.valueOf(carrierConfig));

        if (sp == null) {
            sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_NO_SIM_ECBM_SUPPORT + slotIndex, carrierConfig);
        editor.apply();

        Rlog.i(TAG, "onCarrierConfigChanged preference updated slotIndex=" + slotIndex
                + ", ecbmSupported=" + carrierConfig);
    }

    private boolean isSimReady(int slotIndex, int subId) {
        return SubscriptionManager.isValidSubscriptionId(subId)
                && mTelephonyManagerProxy.getSimState(slotIndex)
                        == TelephonyManager.SIM_STATE_READY;
    }

    private boolean getBroadcastEmergencyCallStateChanges(Phone phone) {
        Boolean broadcast = mBroadcastEmergencyCallStateChanges.get(
                Integer.valueOf(phone.getPhoneId()));
        return (broadcast == null) ? false : broadcast;
    }

    /**
     * Resets the emergency call state if it's in alive state.
     */
    @VisibleForTesting
    public void maybeResetEmergencyCallStateChangedIntent() {
        Intent intent = mContext.registerReceiver(null,
            new IntentFilter(ACTION_EMERGENCY_CALL_STATE_CHANGED), Context.RECEIVER_NOT_EXPORTED);
        if (intent != null
                && ACTION_EMERGENCY_CALL_STATE_CHANGED.equals(intent.getAction())) {
            boolean isAlive = intent.getBooleanExtra(
                    TelephonyManager.EXTRA_PHONE_IN_EMERGENCY_CALL, false);
            Rlog.i(TAG, "maybeResetEmergencyCallStateChangedIntent isAlive=" + isAlive);
            if (isAlive) {
                intent = new Intent(ACTION_EMERGENCY_CALL_STATE_CHANGED);
                intent.putExtra(TelephonyManager.EXTRA_PHONE_IN_EMERGENCY_CALL, false);
                mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            }
        }
    }

    private Call getRingingCall(Phone phone) {
        if (phone == null) return null;
        Call ringingCall = phone.getRingingCall();
        if (ringingCall != null
                && ringingCall.getState() != Call.State.IDLE
                && ringingCall.getState() != Call.State.DISCONNECTED) {
            return ringingCall;
        }
        // Check the ImsPhoneCall in DISCONNECTING state.
        Phone imsPhone = phone.getImsPhone();
        if (imsPhone != null) {
            ringingCall = imsPhone.getRingingCall();
        }
        if (imsPhone != null && ringingCall != null
                && ringingCall.getState() != Call.State.IDLE
                && ringingCall.getState() != Call.State.DISCONNECTED) {
            return ringingCall;
        }
        return null;
    }

    /**
     * Ensures that there is no incoming call.
     *
     * @param completeConsumer The consumer to call once rejecting incoming call completes,
     *                         provides {@code true} result if operation completes successfully
     *                         or {@code false} if the operation timed out/failed.
     */
    private void maybeRejectIncomingCall(Consumer<Boolean> completeConsumer) {
        Phone[] phones = mPhoneFactoryProxy.getPhones();
        if (phones == null) {
            if (completeConsumer != null) {
                completeConsumer.accept(true);
            }
            return;
        }

        Call ringingCall = null;
        for (Phone phone : phones) {
            ringingCall = getRingingCall(phone);
            if (ringingCall != null) {
                Rlog.i(TAG, "maybeRejectIncomingCall found a ringing call");
                break;
            }
        }

        if (ringingCall == null) {
            if (completeConsumer != null) {
                completeConsumer.accept(true);
            }
            return;
        }

        try {
            ringingCall.hangup();
            if (completeConsumer == null) return;

            CompletableFuture<Boolean> future = new CompletableFuture<>();
            com.android.internal.telephony.Connection cn = ringingCall.getLatestConnection();
            cn.addListener(new OnDisconnectListener(future));
            // A timeout that will complete the future to not block the outgoing call indefinitely.
            CompletableFuture<Boolean> timeout = new CompletableFuture<>();
            mHandler.postDelayed(
                    () -> timeout.complete(false), DEFAULT_REJECT_INCOMING_CALL_TIMEOUT_MS);
            // Ensure that the Consumer is completed on the main thread.
            CompletableFuture<Void> unused = future.acceptEitherAsync(timeout, completeConsumer,
                    mHandler::post).exceptionally((ex) -> {
                        Rlog.w(TAG, "maybeRejectIncomingCall - exceptionally= " + ex);
                        return null;
                    });
        } catch (Exception e) {
            Rlog.w(TAG, "maybeRejectIncomingCall - exception= " + e.getMessage());
            if (completeConsumer != null) {
                completeConsumer.accept(false);
            }
        }
    }

    private void registerForNewRingingConnection() {
        Phone[] phones = mPhoneFactoryProxy.getPhones();
        if (phones == null) {
            // unit testing
            return;
        }
        for (Phone phone : phones) {
            phone.registerForNewRingingConnection(mHandler, MSG_NEW_RINGING_CONNECTION,
                    mRegistrantidentifier);
        }
    }

    /**
     * Hangup the new ringing call if there is an ongoing emergency call not connected.
     */
    private void handleNewRingingConnection(Message msg) {
        Connection c = (Connection) ((AsyncResult) msg.obj).result;
        if (c == null) return;
        if ((mNormalRoutingEmergencyConnection == null
                || mNormalRoutingEmergencyConnection.getState() == STATE_ACTIVE
                || mNormalRoutingEmergencyConnection.getState() == STATE_DISCONNECTED)
                && (mOngoingConnection == null
                || mOngoingConnection.getState() == STATE_ACTIVE
                || mOngoingConnection.getState() == STATE_DISCONNECTED)) {
            return;
        }
        if ((c.getPhoneType() == PhoneConstants.PHONE_TYPE_IMS)
                && ((ImsPhoneConnection) c).isIncomingCallAutoRejected()) {
            Rlog.i(TAG, "handleNewRingingConnection auto rejected call");
        } else {
            try {
                Rlog.i(TAG, "handleNewRingingConnection silently drop incoming call");
                c.getCall().hangup();
            } catch (CallStateException e) {
                Rlog.w(TAG, "handleNewRingingConnection", e);
            }
        }
    }

    /**
     * Indicates the start of a normal routing emergency call.
     *
     * <p>
     * Handles turning on radio and switching DDS.
     *
     * @param phone the {@code Phone} on which to process the emergency call.
     * @param c the {@code Connection} on which to process the emergency call.
     * @param completeConsumer The consumer to call once rejecting incoming call completes,
     *        provides {@code true} result if operation completes successfully
     *        or {@code false} if the operation timed out/failed.
     */
    public void startNormalRoutingEmergencyCall(@NonNull Phone phone,
            @NonNull android.telecom.Connection c, @NonNull Consumer<Boolean> completeConsumer) {
        Rlog.i(TAG, "startNormalRoutingEmergencyCall: phoneId=" + phone.getPhoneId()
                + ", callId=" + c.getTelecomCallId());

        mNormalRoutingEmergencyConnection = c;
        maybeRejectIncomingCall(completeConsumer);
    }

    /**
     * Indicates the termination of a normal routing emergency call.
     *
     * @param c the normal routing emergency call disconnected.
     */
    public void endNormalRoutingEmergencyCall(@NonNull android.telecom.Connection c) {
        if (c != mNormalRoutingEmergencyConnection) return;
        Rlog.i(TAG, "endNormalRoutingEmergencyCall: callId=" + c.getTelecomCallId());
        mNormalRoutingEmergencyConnection = null;
    }

    /**
     * Handles the normal routing emergency call state change.
     *
     * @param c the call whose state has changed
     * @param state the new call state
     */
    public void onNormalRoutingEmergencyCallStateChanged(android.telecom.Connection c,
            @android.telecom.Connection.ConnectionState int state) {
        if (c != mNormalRoutingEmergencyConnection) return;

        // If the call is connected, we don't need to monitor incoming call any more.
        if (state == android.telecom.Connection.STATE_ACTIVE
                || state == android.telecom.Connection.STATE_DISCONNECTED) {
            endNormalRoutingEmergencyCall(c);
        }
    }
}

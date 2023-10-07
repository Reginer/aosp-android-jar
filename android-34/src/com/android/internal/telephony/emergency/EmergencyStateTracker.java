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

import static com.android.internal.telephony.emergency.EmergencyConstants.MODE_EMERGENCY_CALLBACK;
import static com.android.internal.telephony.emergency.EmergencyConstants.MODE_EMERGENCY_NONE;
import static com.android.internal.telephony.emergency.EmergencyConstants.MODE_EMERGENCY_WWAN;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.sysprop.TelephonyProperties;
import android.telephony.Annotation.DisconnectCauses;
import android.telephony.CarrierConfigManager;
import android.telephony.DisconnectCause;
import android.telephony.EmergencyRegResult;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.emergency.EmergencyNumber;
import android.util.ArraySet;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.data.PhoneSwitcher;
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

    /**
     * Timeout before we continue with the emergency call without waiting for DDS switch response
     * from the modem.
     */
    private static final int DEFAULT_DATA_SWITCH_TIMEOUT_MS = 1000;
    /** Default value for if Emergency Callback Mode is supported. */
    private static final boolean DEFAULT_EMERGENCY_CALLBACK_MODE_SUPPORTED = true;
    /** Default Emergency Callback Mode exit timeout value. */
    private static final long DEFAULT_ECM_EXIT_TIMEOUT_MS = 300000;

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
    private EmergencyRegResult mLastEmergencyRegResult;
    private boolean mIsEmergencyModeInProgress;
    private boolean mIsEmergencyCallStartedDuringEmergencySms;

    /** For emergency calls */
    private final long mEcmExitTimeoutMs;
    // A runnable which is used to automatically exit from Ecm after a period of time.
    private final Runnable mExitEcmRunnable = this::exitEmergencyCallbackMode;
    // Tracks emergency calls by callId that have reached {@link Call.State#ACTIVE}.
    private final Set<String> mActiveEmergencyCalls = new ArraySet<>();
    private Phone mPhone;
    // Tracks ongoing emergency callId to handle a second emergency call
    private String mOngoingCallId;
    // Domain of the active emergency call. Assuming here that there will only be one domain active.
    private int mEmergencyCallDomain = NetworkRegistrationInfo.DOMAIN_UNKNOWN;
    private CompletableFuture<Integer> mCallEmergencyModeFuture;
    private boolean mIsInEmergencyCall;
    private boolean mIsInEcm;
    private boolean mIsTestEmergencyNumber;
    private Runnable mOnEcmExitCompleteRunnable;

    /** For emergency SMS */
    private final Set<String> mOngoingEmergencySmsIds = new ArraySet<>();
    private Phone mSmsPhone;
    private CompletableFuture<Integer> mSmsEmergencyModeFuture;
    private boolean mIsTestEmergencyNumberForSms;

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
                        mLastEmergencyRegResult = (EmergencyRegResult) ar.result;
                    } else {
                        mLastEmergencyRegResult = null;
                        Rlog.w(TAG, "LastEmergencyRegResult not set. AsyncResult.exception: "
                                + ar.exception);
                    }
                    setEmergencyModeInProgress(false);

                    if (emergencyType == EMERGENCY_TYPE_CALL) {
                        setIsInEmergencyCall(true);
                        completeEmergencyMode(emergencyType);

                        // Case 1) When the emergency call is setting the emergency mode and
                        // the emergency SMS is being sent, completes the SMS future also.
                        // Case 2) When the emergency SMS is setting the emergency mode and
                        // the emergency call is beint started, the SMS request is cancelled and
                        // the call request will be handled.
                        if (mSmsPhone != null) {
                            completeEmergencyMode(EMERGENCY_TYPE_SMS);
                        }
                    } else if (emergencyType == EMERGENCY_TYPE_SMS) {
                        if (mPhone != null && mSmsPhone != null) {
                            // Clear call phone temporarily to exit the emergency mode
                            // if the emergency call is started.
                            if (mIsEmergencyCallStartedDuringEmergencySms) {
                                Phone phone = mPhone;
                                mPhone = null;
                                exitEmergencyMode(mSmsPhone, emergencyType);
                                // Restore call phone for further use.
                                mPhone = phone;

                                if (!isSamePhone(mPhone, mSmsPhone)) {
                                    completeEmergencyMode(emergencyType,
                                            DisconnectCause.OUTGOING_EMERGENCY_CALL_PLACED);
                                }
                            } else {
                                completeEmergencyMode(emergencyType);
                            }
                            break;
                        } else {
                            completeEmergencyMode(emergencyType);
                        }

                        if (mIsEmergencyCallStartedDuringEmergencySms) {
                            mIsEmergencyCallStartedDuringEmergencySms = false;
                            turnOnRadioAndSwitchDds(mPhone, EMERGENCY_TYPE_CALL,
                                    mIsTestEmergencyNumber);
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
                    } else if (emergencyType == EMERGENCY_TYPE_SMS) {
                        if (mIsEmergencyCallStartedDuringEmergencySms) {
                            mIsEmergencyCallStartedDuringEmergencySms = false;
                            turnOnRadioAndSwitchDds(mPhone, EMERGENCY_TYPE_CALL,
                                    mIsTestEmergencyNumber);
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
                    if (mSmsPhone != null) {
                        completeEmergencyMode(EMERGENCY_TYPE_SMS);
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

        // Register receiver for ECM exit.
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        context.registerReceiver(mEcmExitReceiver, filter, null, mHandler);
        mTelephonyManagerProxy = new TelephonyManagerProxyImpl(context);
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
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        context.registerReceiver(mEcmExitReceiver, filter, null, mHandler);
    }

    /**
     * Starts the process of an emergency call.
     *
     * <p>
     * Handles turning on radio and switching DDS.
     *
     * @param phone                 the {@code Phone} on which to process the emergency call.
     * @param callId                the call id on which to process the emergency call.
     * @param isTestEmergencyNumber whether this is a test emergency number.
     * @return a {@code CompletableFuture} that results in {@code DisconnectCause.NOT_DISCONNECTED}
     *         if emergency call successfully started.
     */
    public CompletableFuture<Integer> startEmergencyCall(@NonNull Phone phone,
            @NonNull String callId, boolean isTestEmergencyNumber) {
        Rlog.i(TAG, "startEmergencyCall: phoneId=" + phone.getPhoneId() + ", callId=" + callId);

        if (mPhone != null) {
            // Create new future to return as to not interfere with any uncompleted futures.
            // Case1) When 2nd emergency call is initiated during an active call on the same phone.
            // Case2) While the device is in ECBM, an emergency call is initiated on the same phone.
            if (isSamePhone(mPhone, phone) && (!mActiveEmergencyCalls.isEmpty() || isInEcm())) {
                mOngoingCallId = callId;
                mIsTestEmergencyNumber = isTestEmergencyNumber;
                return CompletableFuture.completedFuture(DisconnectCause.NOT_DISCONNECTED);
            }

            Rlog.e(TAG, "startEmergencyCall failed. Existing emergency call in progress.");
            return CompletableFuture.completedFuture(DisconnectCause.ERROR_UNSPECIFIED);
        }

        mCallEmergencyModeFuture = new CompletableFuture<>();

        if (mSmsPhone != null) {
            mIsEmergencyCallStartedDuringEmergencySms = true;
            // Case1) While exiting the emergency mode on the other phone,
            // the emergency mode for this call will be restarted after the exit complete.
            // Case2) While entering the emergency mode on the other phone,
            // exit the emergency mode when receiving the result of setting the emergency mode and
            // the emergency mode for this call will be restarted after the exit complete.
            if (isInEmergencyMode() && !isEmergencyModeInProgress()) {
                exitEmergencyMode(mSmsPhone, EMERGENCY_TYPE_SMS);
            }

            mPhone = phone;
            mOngoingCallId = callId;
            mIsTestEmergencyNumber = isTestEmergencyNumber;
            return mCallEmergencyModeFuture;
        }

        mPhone = phone;
        mOngoingCallId = callId;
        mIsTestEmergencyNumber = isTestEmergencyNumber;
        turnOnRadioAndSwitchDds(mPhone, EMERGENCY_TYPE_CALL, mIsTestEmergencyNumber);
        return mCallEmergencyModeFuture;
    }

    /**
     * Ends emergency call.
     *
     * <p>
     * Enter ECM only once all active emergency calls have ended. If a call never reached
     * {@link Call.State#ACTIVE}, then no need to enter ECM.
     *
     * @param callId the call id on which to end the emergency call.
     */
    public void endCall(@NonNull String callId) {
        boolean wasActive = mActiveEmergencyCalls.remove(callId);

        if (Objects.equals(mOngoingCallId, callId)) {
            mOngoingCallId = null;
        }

        if (wasActive && mActiveEmergencyCalls.isEmpty()
                && isEmergencyCallbackModeSupported()) {
            enterEmergencyCallbackMode();

            if (mOngoingCallId == null) {
                mIsEmergencyCallStartedDuringEmergencySms = false;
                mCallEmergencyModeFuture = null;
            }
        } else if (mOngoingCallId == null) {
            if (isInEcm()) {
                mIsEmergencyCallStartedDuringEmergencySms = false;
                mCallEmergencyModeFuture = null;
                // If the emergency call was initiated during the emergency callback mode,
                // the emergency callback mode should be restored when the emergency call is ended.
                if (mActiveEmergencyCalls.isEmpty()) {
                    setEmergencyMode(mPhone, EMERGENCY_TYPE_CALL, MODE_EMERGENCY_CALLBACK,
                            MSG_SET_EMERGENCY_CALLBACK_MODE_DONE);
                }
            } else {
                exitEmergencyMode(mPhone, EMERGENCY_TYPE_CALL);
                clearEmergencyCallInfo();
            }
        }
    }

    private void clearEmergencyCallInfo() {
        mEmergencyCallDomain = NetworkRegistrationInfo.DOMAIN_UNKNOWN;
        mIsTestEmergencyNumber = false;
        mIsEmergencyCallStartedDuringEmergencySms = false;
        mCallEmergencyModeFuture = null;
        mOngoingCallId = null;
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
            // emergency domain. EmergencyRegResult is required to determine domain and this is the
            // only API that can receive it before starting domain selection. Once domain selection
            // is finished, the actual emergency mode will be set when onEmergencyTransportChanged()
            // is called.
            setEmergencyMode(phone, emergencyType, MODE_EMERGENCY_WWAN,
                    MSG_SET_EMERGENCY_MODE_DONE);
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
            return;
        }
        mEmergencyMode = mode;
        setEmergencyModeInProgress(true);

        Message m = mHandler.obtainMessage(msg, Integer.valueOf(emergencyType));
        if ((mIsTestEmergencyNumber && emergencyType == EMERGENCY_TYPE_CALL)
                || (mIsTestEmergencyNumberForSms && emergencyType == EMERGENCY_TYPE_SMS)) {
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

    /** Returns last {@link EmergencyRegResult} as set by {@code setEmergencyMode()}. */
    public EmergencyRegResult getEmergencyRegResult() {
        return mLastEmergencyRegResult;
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
     * @param callId The ID of the call
     */
    public void onEmergencyCallDomainUpdated(int phoneType, String callId) {
        Rlog.d(TAG, "domain update for callId: " + callId);
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
     * @param callId the callId whose state has changed
     */
    public void onEmergencyCallStateChanged(Call.State state, String callId) {
        if (state == Call.State.ACTIVE) {
            mActiveEmergencyCalls.add(callId);
        }
    }

    /**
     * Returns {@code true} if device and carrier support emergency callback mode.
     */
    private boolean isEmergencyCallbackModeSupported() {
        return getConfig(mPhone.getSubId(),
                CarrierConfigManager.ImsEmergency.KEY_EMERGENCY_CALLBACK_MODE_SUPPORTED_BOOL,
                DEFAULT_EMERGENCY_CALLBACK_MODE_SUPPORTED);
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

            // Set emergency mode on modem.
            setEmergencyMode(mPhone, EMERGENCY_TYPE_CALL, MODE_EMERGENCY_CALLBACK,
                    MSG_SET_EMERGENCY_CALLBACK_MODE_DONE);

            // Post this runnable so we will automatically exit if no one invokes
            // exitEmergencyCallbackMode() directly.
            long delayInMillis = TelephonyProperties.ecm_exit_timer()
                    .orElse(mEcmExitTimeoutMs);
            mHandler.postDelayed(mExitEcmRunnable, delayInMillis);

            // We don't want to go to sleep while in ECM.
            if (mWakeLock != null) mWakeLock.acquire(delayInMillis);
        }
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
            if (mWakeLock != null && mWakeLock.isHeld()) {
                try {
                    mWakeLock.release();
                } catch (Exception e) {
                    // Ignore the exception if the system has already released this WakeLock.
                    Rlog.d(TAG, "WakeLock already released: " + e.toString());
                }
            }

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
        Rlog.i(TAG, "startEmergencySms: phoneId=" + phone.getPhoneId() + ", smsId=" + smsId);

        // When an emergency call is in progress, it checks whether an emergency call is already in
        // progress on the different phone.
        if (mPhone != null && !isSamePhone(mPhone, phone)) {
            Rlog.e(TAG, "Emergency call is in progress on the other slot.");
            return CompletableFuture.completedFuture(DisconnectCause.ERROR_UNSPECIFIED);
        }

        // When an emergency SMS is in progress, it checks whether an emergency SMS is already in
        // progress on the different phone.
        if (mSmsPhone != null && !isSamePhone(mSmsPhone, phone)) {
            Rlog.e(TAG, "Emergency SMS is in progress on the other slot.");
            return CompletableFuture.completedFuture(DisconnectCause.ERROR_UNSPECIFIED);
        }

        // When the previous emergency SMS is not completed yet,
        // this new request will not be allowed.
        if (mSmsPhone != null && isInEmergencyMode() && isEmergencyModeInProgress()) {
            Rlog.e(TAG, "Existing emergency SMS is in progress.");
            return CompletableFuture.completedFuture(DisconnectCause.ERROR_UNSPECIFIED);
        }

        mSmsPhone = phone;
        mIsTestEmergencyNumberForSms = isTestEmergencyNumber;
        mOngoingEmergencySmsIds.add(smsId);

        // When the emergency mode is already set by the previous emergency call or SMS,
        // completes the future immediately.
        if (isInEmergencyMode() && !isEmergencyModeInProgress()) {
            return CompletableFuture.completedFuture(DisconnectCause.NOT_DISCONNECTED);
        }

        mSmsEmergencyModeFuture = new CompletableFuture<>();
        if (!isInEmergencyMode()) {
            setEmergencyMode(mSmsPhone, EMERGENCY_TYPE_SMS, MODE_EMERGENCY_WWAN,
                    MSG_SET_EMERGENCY_MODE_DONE);
        }
        return mSmsEmergencyModeFuture;
    }

    /**
     * Ends an emergency SMS.
     * This should be called once an emergency SMS is sent.
     *
     * @param smsId the SMS id on which to end the emergency SMS.
     * @param emergencyNumber the emergency number which was used for the emergency SMS.
     */
    public void endSms(@NonNull String smsId, EmergencyNumber emergencyNumber) {
        mOngoingEmergencySmsIds.remove(smsId);

        // If the outgoing emergency SMSs are empty, we can try to exit the emergency mode.
        if (mOngoingEmergencySmsIds.isEmpty()) {
            if (isInEcm()) {
                // When the emergency mode is not in MODE_EMERGENCY_CALLBACK,
                // it needs to notify the emergency callback mode to modem.
                if (mActiveEmergencyCalls.isEmpty() && mOngoingCallId == null) {
                    setEmergencyMode(mPhone, EMERGENCY_TYPE_CALL, MODE_EMERGENCY_CALLBACK,
                            MSG_SET_EMERGENCY_CALLBACK_MODE_DONE);
                }
            } else {
                exitEmergencyMode(mSmsPhone, EMERGENCY_TYPE_SMS);
            }

            clearEmergencySmsInfo();
        }
    }

    private void clearEmergencySmsInfo() {
        mOngoingEmergencySmsIds.clear();
        mIsTestEmergencyNumberForSms = false;
        mSmsEmergencyModeFuture = null;
        mSmsPhone = null;
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
     * and selecting emergency domain. EmergencyRegResult is required to determine domain and
     * setEmergencyMode() is the only API that can receive it before starting domain selection.
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

        if (needToTurnOnRadio) {
            Rlog.i(TAG, "turnOnRadioAndSwitchDds: phoneId=" + phone.getPhoneId() + " for "
                    + emergencyTypeToString(emergencyType));
            if (mRadioOnHelper == null) {
                mRadioOnHelper = new RadioOnHelper(mContext);
            }

            mRadioOnHelper.triggerRadioOnAndListen(new RadioOnStateListener.Callback() {
                @Override
                public void onComplete(RadioOnStateListener listener, boolean isRadioReady) {
                    if (!isRadioReady) {
                        // Could not turn radio on
                        Rlog.e(TAG, "Failed to turn on radio.");
                        completeEmergencyMode(emergencyType, DisconnectCause.POWER_OFF);
                    } else {
                        switchDdsAndSetEmergencyMode(phone, emergencyType);
                    }
                }

                @Override
                public boolean isOkToCall(Phone phone, int serviceState, boolean imsVoiceCapable) {
                    // We currently only look to make sure that the radio is on before dialing. We
                    // should be able to make emergency calls at any time after the radio has been
                    // powered on and isn't in the UNAVAILABLE state, even if it is reporting the
                    // OUT_OF_SERVICE state.
                    return phone.getServiceStateTracker().isRadioOn();
                }

                @Override
                public boolean onTimeout(Phone phone, int serviceState, boolean imsVoiceCapable) {
                    return true;
                }
            }, !isTestEmergencyNumber, phone, isTestEmergencyNumber, 0);
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
    private PersistableBundle getConfigBundle(int subId, String key) {
        if (mConfigManager == null) return new PersistableBundle();
        return mConfigManager.getConfigForSubId(subId, key);
    }

    /**
     * Returns true if the state of the Phone is IN_SERVICE or available for emergency calling only.
     */
    private boolean isAvailableForEmergencyCalls(Phone phone) {
        return ServiceState.STATE_IN_SERVICE == phone.getServiceState().getState()
                || phone.getServiceState().isEmergencyOnly();
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
}

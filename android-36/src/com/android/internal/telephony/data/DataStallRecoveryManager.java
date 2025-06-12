/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.internal.telephony.data;

import static android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE;

import android.annotation.CallbackExecutor;
import android.annotation.ElapsedRealtimeLong;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.Annotation.RadioPowerState;
import android.telephony.Annotation.ValidationStatus;
import android.telephony.CellSignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.data.DataConfigManager.DataConfigManagerCallback;
import com.android.internal.telephony.data.DataNetworkController.DataNetworkControllerCallback;
import com.android.internal.telephony.data.DataSettingsManager.DataSettingsManagerCallback;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.metrics.DataStallRecoveryStats;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * DataStallRecoveryManager monitors the network validation result from connectivity service and
 * takes actions to recovery data network.
 */
public class DataStallRecoveryManager extends Handler {
    private static final boolean VDBG = false;

    /** Recovery actions taken in case of data stall */
    @IntDef(
            value = {
                RECOVERY_ACTION_GET_DATA_CALL_LIST,
                RECOVERY_ACTION_CLEANUP,
                RECOVERY_ACTION_RADIO_RESTART,
                RECOVERY_ACTION_RESET_MODEM
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecoveryAction {}

    /* DataStallRecoveryManager queries RIL for link properties (IP addresses, DNS server addresses
     * etc) using RIL_REQUEST_GET_DATA_CALL_LIST.  This will help in cases where the data stall
     * occurred because of a link property changed but not notified to connectivity service.
     */
    public static final int RECOVERY_ACTION_GET_DATA_CALL_LIST = 0;

    /* DataStallRecoveryManager will request DataNetworkController to reestablish internet using
     * RIL_REQUEST_DEACTIVATE_DATA_CALL and sets up the data call back using SETUP_DATA_CALL.
     * It will help to reestablish the channel between RIL and modem.
     */
    public static final int RECOVERY_ACTION_CLEANUP = 1;

    /* DataStallRecoveryManager will request ServiceStateTracker to send RIL_REQUEST_RADIO_POWER
     * to restart radio. It will restart the radio and re-attch to the network.
     */
    public static final int RECOVERY_ACTION_RADIO_RESTART = 3;

    /* DataStallRecoveryManager will request to reboot modem using NV_RESET_CONFIG. It will recover
     * if there is a problem in modem side.
     */
    public static final int RECOVERY_ACTION_RESET_MODEM = 4;

    /** Recovered reason taken in case of data stall recovered */
    @IntDef(
            value = {
                RECOVERED_REASON_NONE,
                RECOVERED_REASON_DSRM,
                RECOVERED_REASON_MODEM,
                RECOVERED_REASON_USER
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecoveredReason {}

    // The reason when data stall recovered.
    /** The data stall not recovered yet. */
    private static final int RECOVERED_REASON_NONE = 0;
    /** The data stall recovered by our DataStallRecoveryManager. */
    private static final int RECOVERED_REASON_DSRM = 1;
    /** The data stall recovered by modem(Radio Power off/on). */
    private static final int RECOVERED_REASON_MODEM = 2;
    /** The data stall recovered by user (Mobile Data Power off/on). */
    private static final int RECOVERED_REASON_USER = 3;

    /** The number of milliseconds to wait for the DSRM prediction to complete. */
    private static final int DSRM_PREDICT_WAITING_MILLIS = 1000;

    /** Event for send data stall broadcast. */
    private static final int EVENT_SEND_DATA_STALL_BROADCAST = 1;

    /** Event for triggering recovery action. */
    private static final int EVENT_DO_RECOVERY = 2;

    /** Event for radio state changed. */
    private static final int EVENT_RADIO_STATE_CHANGED = 3;

    /** Event for recovery actions changed. */
    private static final int EVENT_CONTENT_DSRM_ENABLED_ACTIONS_CHANGED = 4;

    /** Event for duration milliseconds changed. */
    private static final int EVENT_CONTENT_DSRM_DURATION_MILLIS_CHANGED = 5;

    @NonNull
    private final Phone mPhone;
    @NonNull
    private final String mLogTag;
    @NonNull
    private final LocalLog mLocalLog = new LocalLog(128);
    @NonNull
    private final FeatureFlags mFeatureFlags;

    /** Data network controller */
    @NonNull
    private final DataNetworkController mDataNetworkController;

    /** Data config manager */
    @NonNull
    private final DataConfigManager mDataConfigManager;

    /** Cellular data service */
    @NonNull
    private final DataServiceManager mWwanDataServiceManager;

    /** The data stall recovery action. */
    @RecoveryAction
    private int mRecoveryAction;
    /** The elapsed real time of last recovery attempted */
    @ElapsedRealtimeLong
    private long mTimeLastRecoveryStartMs;
    /** Whether current network is good or not */
    private boolean mIsValidNetwork;
    /** Whether data stall recovery is triggered or not */
    private boolean mRecoveryTriggered = false;
    /** Whether data stall happened or not. */
    private boolean mDataStalled;
    /** Whether the result of last action(RADIO_RESTART) reported. */
    private boolean mLastActionReported;
    /** The real time for data stall start. */
    @VisibleForTesting
    @ElapsedRealtimeLong
    public long mDataStallStartMs;
    /** Last data stall recovery action. */
    @RecoveryAction
    private int mLastAction;
    /** Last radio power state. */
    @RadioPowerState
    private int mRadioPowerState;
    /** Whether the NetworkCheckTimer start. */
    private boolean mNetworkCheckTimerStarted = false;
    /** Whether radio state changed during data stall. */
    private boolean mRadioStateChangedDuringDataStall;
    /** Whether airplane mode enabled during data stall. */
    private boolean mIsAirPlaneModeEnableDuringDataStall;
    /** Whether mobile data change to Enabled during data stall. */
    private boolean mMobileDataChangedToEnabledDuringDataStall;
    /** Whether attempted all recovery steps. */
    private boolean mIsAttemptedAllSteps;
    /** Whether internet network that require validation is connected. */
    private boolean mIsInternetNetworkConnected;
    /** The durations for current recovery action */
    @ElapsedRealtimeLong
    private long mTimeElapsedOfCurrentAction;
    /** Tracks the total number of validation duration a data stall */
    private int mValidationCount;
    /** Tracks the number of validation for current action during a data stall */
    private int mActionValidationCount;
    /** The array for the timers between recovery actions. */
    @NonNull
    private long[] mDataStallRecoveryDelayMillisArray;
    /** The boolean array for the flags. They are used to skip the recovery actions if needed. */
    @NonNull
    private boolean[] mSkipRecoveryActionArray;

    /**
     * The content URI for the DSRM recovery actions.
     *
     * @see Settings.Global#DSRM_ENABLED_ACTIONS
     */
    private static final Uri CONTENT_URL_DSRM_ENABLED_ACTIONS = Settings.Global.getUriFor(
            Settings.Global.DSRM_ENABLED_ACTIONS);

    /**
     * The content URI for the DSRM duration milliseconds.
     *
     * @see Settings.Global#DSRM_DURATION_MILLIS
     */
    private static final Uri CONTENT_URL_DSRM_DURATION_MILLIS = Settings.Global.getUriFor(
            Settings.Global.DSRM_DURATION_MILLIS);


    private final DataStallRecoveryManagerCallback mDataStallRecoveryManagerCallback;

    private final DataStallRecoveryStats mStats;

    /** The number of milliseconds to wait for the DSRM prediction to complete. */
    @ElapsedRealtimeLong
    private long mPredictWaitingMillis = 0L;

    /**
     * The data stall recovery manager callback. Note this is only used for passing information
     * internally in the data stack, should not be used externally.
     */
    public abstract static class DataStallRecoveryManagerCallback extends DataCallback {
        /**
         * Constructor
         *
         * @param executor The executor of the callback.
         */
        public DataStallRecoveryManagerCallback(@NonNull @CallbackExecutor Executor executor) {
            super(executor);
        }

        /**
         * Called when data stall occurs and needed to tear down / setup a new data network for
         * internet.
         */
        public abstract void onDataStallReestablishInternet();
    }

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param dataNetworkController Data network controller
     * @param dataServiceManager The WWAN data service manager.
     * @param featureFlags The feature flag.
     * @param looper The looper to be used by the handler. Currently the handler thread is the phone
     *     process's main thread.
     * @param callback Callback to notify data network controller for data stall events.
     */
    public DataStallRecoveryManager(
            @NonNull Phone phone,
            @NonNull DataNetworkController dataNetworkController,
            @NonNull DataServiceManager dataServiceManager,
            @NonNull FeatureFlags featureFlags,
            @NonNull Looper looper,
            @NonNull DataStallRecoveryManagerCallback callback) {
        super(looper);
        mPhone = phone;
        mLogTag = "DSRM-" + mPhone.getPhoneId();
        log("DataStallRecoveryManager created.");
        mDataNetworkController = dataNetworkController;
        mWwanDataServiceManager = dataServiceManager;
        mFeatureFlags = featureFlags;
        mDataConfigManager = mDataNetworkController.getDataConfigManager();
        mDataNetworkController
                .getDataSettingsManager()
                .registerCallback(
                        new DataSettingsManagerCallback(this::post) {
                            @Override
                            public void onDataEnabledChanged(
                                    boolean enabled,
                                    @TelephonyManager.DataEnabledChangedReason int reason,
                                    @NonNull String callingPackage) {
                                onMobileDataEnabledChanged(enabled);
                            }
                        });
        mDataStallRecoveryManagerCallback = callback;
        mRadioPowerState = mPhone.getRadioPowerState();
        updateDataStallRecoveryConfigs();

        registerAllEvents();

        mStats = new DataStallRecoveryStats(mPhone, mFeatureFlags, dataNetworkController);
    }

    /** Register for all events that data stall monitor is interested. */
    private void registerAllEvents() {
        mDataConfigManager.registerCallback(new DataConfigManagerCallback(this::post) {
            @Override
            public void onCarrierConfigChanged() {
                DataStallRecoveryManager.this.onCarrierConfigUpdated();
            }
        });
        mDataNetworkController.registerDataNetworkControllerCallback(
                new DataNetworkControllerCallback(this::post) {
                    @Override
                    public void onInternetDataNetworkValidationStatusChanged(
                            @ValidationStatus int validationStatus) {
                        onInternetValidationStatusChanged(validationStatus);
                    }

                    @Override
                    public void onConnectedInternetDataNetworksChanged(
                            @NonNull Set<DataNetwork> internetNetworks) {
                        boolean anyInternetRequireValidatedConnected = internetNetworks.stream()
                                .anyMatch(nw -> {
                                    NetworkCapabilities capabilities = nw.getNetworkCapabilities();
                                    // Only track the networks that require validation.
                                    // The criteria is base on NetworkMonitorUtils.java.
                                    return capabilities.hasCapability(
                                            NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                                            && capabilities.hasCapability(
                                            NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                                            && capabilities.hasCapability(
                                            NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
                                });
                        if (mIsInternetNetworkConnected != anyInternetRequireValidatedConnected) {
                            mIsInternetNetworkConnected = anyInternetRequireValidatedConnected;
                            logl(mIsInternetNetworkConnected
                                    ? "At Least One InternetDataNetwork Connected"
                                    : "All InternetDataNetwork Disconnected");
                        }
                    }
                });
        mPhone.mCi.registerForRadioStateChanged(this, EVENT_RADIO_STATE_CHANGED, null);

        // Register for DSRM global setting changes.
        mPhone.getContext().getContentResolver().registerContentObserver(
                CONTENT_URL_DSRM_ENABLED_ACTIONS, false, mContentObserver);
        mPhone.getContext().getContentResolver().registerContentObserver(
                CONTENT_URL_DSRM_DURATION_MILLIS, false, mContentObserver);

    }

    @Override
    public void handleMessage(Message msg) {
        logv("handleMessage = " + msg);
        switch (msg.what) {
            case EVENT_SEND_DATA_STALL_BROADCAST:
                mRecoveryTriggered = true;
                // Verify that DSRM needs to process the recovery action
                if (!isRecoveryNeeded(false)) {
                    cancelNetworkCheckTimer();
                    startNetworkCheckTimer(getRecoveryAction());
                    return;
                }
                // Broadcast intent that data stall has been detected.
                broadcastDataStallDetected(getRecoveryAction());
                // Schedule the message to to wait for the predicted value.
                sendMessageDelayed(
                        obtainMessage(EVENT_DO_RECOVERY), mPredictWaitingMillis);
                break;
            case EVENT_DO_RECOVERY:
                doRecovery();
                break;
            case EVENT_RADIO_STATE_CHANGED:
                mRadioPowerState = mPhone.getRadioPowerState();
                if (mDataStalled) {
                    // Store the radio state changed flag only when data stall occurred.
                    mRadioStateChangedDuringDataStall = true;
                    if (Settings.Global.getInt(
                                    mPhone.getContext().getContentResolver(),
                                    Settings.Global.AIRPLANE_MODE_ON,
                                    0) != 0) {
                        mIsAirPlaneModeEnableDuringDataStall = true;
                    }
                    setRecoveryAction(mLastAction);
                }
                break;
            case EVENT_CONTENT_DSRM_ENABLED_ACTIONS_CHANGED:
                updateGlobalConfigActions();
                break;
            case EVENT_CONTENT_DSRM_DURATION_MILLIS_CHANGED:
                updateGlobalConfigDurations();
                break;
            default:
                loge("Unexpected message = " + msg);
                break;
        }
    }

    private final ContentObserver mContentObserver = new ContentObserver(this) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (CONTENT_URL_DSRM_ENABLED_ACTIONS.equals(uri)) {
                log("onChange URI: " + uri);
                sendMessage(obtainMessage(EVENT_CONTENT_DSRM_ENABLED_ACTIONS_CHANGED));
            } else if (CONTENT_URL_DSRM_DURATION_MILLIS.equals(uri)) {
                log("onChange URI: " + uri);
                sendMessage(obtainMessage(EVENT_CONTENT_DSRM_DURATION_MILLIS_CHANGED));
            }
        }
    };


    @VisibleForTesting
    public ContentObserver getContentObserver() {
        return mContentObserver;
    }

    /**
     * Updates the skip recovery action array based on DSRM global configuration actions.
     *
     * @see Settings.Global#DSRM_ENABLED_ACTIONS
     */
    private void updateGlobalConfigActions() {
        String enabledActions = Settings.Global.getString(
                mPhone.getContext().getContentResolver(),
                Settings.Global.DSRM_ENABLED_ACTIONS);

        if (!TextUtils.isEmpty(enabledActions)) {
            String[] splitEnabledActions = enabledActions.split(",");
            boolean[] enabledActionsArray = new boolean[splitEnabledActions.length];
            for (int i = 0; i < enabledActionsArray.length; i++) {
                enabledActionsArray[i] = Boolean.parseBoolean(splitEnabledActions[i].trim());
            }

            int minLength = Math.min(enabledActionsArray.length,
                    mSkipRecoveryActionArray.length);

            // Update the skip recovery action array.
            for (int i = 0; i < minLength; i++) {
                mSkipRecoveryActionArray[i] = !enabledActionsArray[i];
            }
            log("SkipRecoveryAction: "
                    + Arrays.toString(mSkipRecoveryActionArray));
            mPredictWaitingMillis = DSRM_PREDICT_WAITING_MILLIS;
        } else {
            mPredictWaitingMillis = 0;
            log("Enabled actions is null");
        }
    }

    /**
     * Updates the duration millis array based on DSRM global configuration durations.
     *
     * @see Settings.Global#DSRM_DURATION_MILLIS
     */
    private void updateGlobalConfigDurations() {
        String durationMillis = Settings.Global.getString(
                mPhone.getContext().getContentResolver(),
                Settings.Global.DSRM_DURATION_MILLIS);

        if (!TextUtils.isEmpty(durationMillis)) {
            String[] splitDurationMillis = durationMillis.split(",");
            long[] durationMillisArray = new long[splitDurationMillis.length];
            for (int i = 0; i < durationMillisArray.length; i++) {
                try {
                    durationMillisArray[i] = Long.parseLong(splitDurationMillis[i].trim());
                } catch (NumberFormatException e) {
                    mPredictWaitingMillis = 0;
                    loge("Parsing duration millis error");
                    return;
                }
            }

            int minLength = Math.min(durationMillisArray.length,
                    mDataStallRecoveryDelayMillisArray.length);

            // Copy the values from the durationMillisArray array to the
            // mDataStallRecoveryDelayMillisArray array.
            System.arraycopy(durationMillisArray, 0, mDataStallRecoveryDelayMillisArray,
                    0, minLength);
            log("DataStallRecoveryDelayMillis: "
                    + Arrays.toString(mDataStallRecoveryDelayMillisArray));
            mPredictWaitingMillis = DSRM_PREDICT_WAITING_MILLIS;
        } else {
            mPredictWaitingMillis = 0;
            log("Duration millis is null");
        }
    }

    /** Update the data stall recovery configs from DataConfigManager. */
    private void updateDataStallRecoveryConfigs() {
        mDataStallRecoveryDelayMillisArray = mDataConfigManager.getDataStallRecoveryDelayMillis();
        mSkipRecoveryActionArray = mDataConfigManager.getDataStallRecoveryShouldSkipArray();

        //Update Global settings
        updateGlobalConfigActions();
        updateGlobalConfigDurations();
    }

    /**
     * Get the duration for specific data stall recovery action.
     *
     * @param recoveryAction The recovery action to query.
     * @return the delay in milliseconds for the specific recovery action.
     */
    @VisibleForTesting
    public long getDataStallRecoveryDelayMillis(@RecoveryAction int recoveryAction) {
        return mDataStallRecoveryDelayMillisArray[recoveryAction];
    }

    /**
     * Check if the recovery action needs to be skipped.
     *
     * @param recoveryAction The recovery action.
     * @return {@code true} if the action needs to be skipped.
     */
    @VisibleForTesting
    public boolean shouldSkipRecoveryAction(@RecoveryAction int recoveryAction) {
        return mSkipRecoveryActionArray[recoveryAction];
    }

    /** Called when carrier config was updated. */
    private void onCarrierConfigUpdated() {
        updateDataStallRecoveryConfigs();
    }

    /**
     * Called when mobile data setting changed.
     *
     * @param enabled true for mobile data settings enabled & false for disabled.
     */
    private void onMobileDataEnabledChanged(boolean enabled) {
        logl("onMobileDataEnabledChanged: DataEnabled:" + enabled + ",DataStalled:" + mDataStalled);
        // Store the mobile data changed flag (from disabled to enabled) as TRUE
        // during data stalled.
        if (mDataStalled && enabled) {
            mMobileDataChangedToEnabledDuringDataStall = true;
            setRecoveryAction(mLastAction);
        }
    }

    /**
     * Called when internet validation status passed. We will initialize all parameters.
     */
    private void reset() {
        mIsValidNetwork = true;
        mRecoveryTriggered = false;
        mIsAttemptedAllSteps = false;
        mRadioStateChangedDuringDataStall = false;
        mIsAirPlaneModeEnableDuringDataStall = false;
        mMobileDataChangedToEnabledDuringDataStall = false;
        cancelNetworkCheckTimer();
        mTimeLastRecoveryStartMs = 0;
        mLastAction = RECOVERY_ACTION_GET_DATA_CALL_LIST;
        mRecoveryAction = RECOVERY_ACTION_GET_DATA_CALL_LIST;
        mValidationCount = 0;
        mActionValidationCount = 0;
    }

    /**
     * Called when internet validation status changed.
     *
     * @param status Validation status.
     */
    private void onInternetValidationStatusChanged(@ValidationStatus int status) {
        logl("onInternetValidationStatusChanged: " + DataUtils.validationStatusToString(status));
        final boolean isValid = status == NetworkAgent.VALIDATION_STATUS_VALID;
        mValidationCount += 1;
        mActionValidationCount += 1;
        setNetworkValidationState(isValid);
        if (isValid) {
            // Broadcast intent that data stall recovered.
            broadcastDataStallDetected(mLastAction);
            reset();
        } else if (isRecoveryNeeded(true)) {
            // Set the network as invalid, because recovery is needed
            mIsValidNetwork = false;
            log("trigger data stall recovery");
            mTimeLastRecoveryStartMs = SystemClock.elapsedRealtime();
            sendMessage(obtainMessage(EVENT_SEND_DATA_STALL_BROADCAST));
        }
    }

    /** Reset the action to initial step. */
    private void resetAction() {
        mTimeLastRecoveryStartMs = 0;
        mMobileDataChangedToEnabledDuringDataStall = false;
        mRadioStateChangedDuringDataStall = false;
        mIsAirPlaneModeEnableDuringDataStall = false;
        setRecoveryAction(RECOVERY_ACTION_GET_DATA_CALL_LIST);
    }

    /**
     * Get recovery action from settings.
     *
     * @return recovery action
     */
    @VisibleForTesting
    @RecoveryAction
    public int getRecoveryAction() {
        log("getRecoveryAction: " + recoveryActionToString(mRecoveryAction));
        return mRecoveryAction;
    }

    /**
     * Put recovery action into settings.
     *
     * @param action The next recovery action.
     */
    @VisibleForTesting
    public void setRecoveryAction(@RecoveryAction int action) {
        // Reset the validation count for action change
        if (mRecoveryAction != action) {
            mActionValidationCount = 0;
        }
        mRecoveryAction = action;

        // Check if the mobile data enabled is TRUE, it means that the mobile data setting changed
        // from DISABLED to ENABLED, we will set the next recovery action to
        // RECOVERY_ACTION_RADIO_RESTART due to already did the RECOVERY_ACTION_CLEANUP.
        if (mMobileDataChangedToEnabledDuringDataStall
                && mRecoveryAction < RECOVERY_ACTION_RADIO_RESTART) {
            mRecoveryAction = RECOVERY_ACTION_RADIO_RESTART;
        }
        // Check if the radio state changed from off to on, it means that the modem already
        // did the radio restart, we will set the next action to RECOVERY_ACTION_RESET_MODEM.
        if (mRadioStateChangedDuringDataStall
                && mRadioPowerState == TelephonyManager.RADIO_POWER_ON) {
            mRecoveryAction = RECOVERY_ACTION_RESET_MODEM;
        }
        // To check the flag from DataConfigManager if we need to skip the step.
        if (shouldSkipRecoveryAction(mRecoveryAction)) {
            switch (mRecoveryAction) {
                case RECOVERY_ACTION_GET_DATA_CALL_LIST:
                    setRecoveryAction(RECOVERY_ACTION_CLEANUP);
                    break;
                case RECOVERY_ACTION_CLEANUP:
                    setRecoveryAction(RECOVERY_ACTION_RADIO_RESTART);
                    break;
                case RECOVERY_ACTION_RADIO_RESTART:
                    setRecoveryAction(RECOVERY_ACTION_RESET_MODEM);
                    break;
                case RECOVERY_ACTION_RESET_MODEM:
                    resetAction();
                    break;
            }
        }

        log("setRecoveryAction: " + recoveryActionToString(mRecoveryAction));
    }

    /**
     * Check if recovery already started.
     *
     * @return {@code true} if recovery already started, {@code false} recovery not started.
     */
    private boolean isRecoveryAlreadyStarted() {
        return getRecoveryAction() != RECOVERY_ACTION_GET_DATA_CALL_LIST || mRecoveryTriggered;
    }

    /**
     * Get elapsed time since last recovery.
     *
     * @return the time since last recovery started.
     */
    private long getElapsedTimeSinceRecoveryMs() {
        return (SystemClock.elapsedRealtime() - mTimeLastRecoveryStartMs);
    }

    /**
     * Get duration time for current recovery action.
     *
     * @return the time duration for current recovery action.
     */
    private long getDurationOfCurrentRecoveryMs() {
        return (SystemClock.elapsedRealtime() - mTimeElapsedOfCurrentAction);
    }

    /**
     * Broadcast intent when data stall occurred.
     *
     * @param recoveryAction Send the data stall detected intent with RecoveryAction info.
     */
    private void broadcastDataStallDetected(@RecoveryAction int recoveryAction) {
        log("broadcastDataStallDetected recoveryAction: " + recoveryAction);
        Intent intent = new Intent(TelephonyManager.ACTION_DATA_STALL_DETECTED);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
        intent.putExtra(TelephonyManager.EXTRA_RECOVERY_ACTION, recoveryAction);

        // Get the information for DSRS state
        final boolean isRecovered = !mDataStalled;
        final int duration = (int) (SystemClock.elapsedRealtime() - mDataStallStartMs);
        @RecoveredReason final int reason = getRecoveredReason(mIsValidNetwork);
        final int durationOfAction = (int) getDurationOfCurrentRecoveryMs();
        log("mValidationCount=" + mValidationCount
                + ", mActionValidationCount=" + mActionValidationCount);

        // Get the bundled DSRS stats.
        Bundle bundle = mStats.getDataStallRecoveryMetricsData(
                recoveryAction, isRecovered, duration, reason, mValidationCount,
                mActionValidationCount, durationOfAction);

        // Put the bundled stats extras on the intent.
        intent.putExtra("EXTRA_DSRS_STATS_BUNDLE", bundle);

        if (mFeatureFlags.hsumBroadcast()) {
            mPhone.getContext().sendBroadcastAsUser(intent, UserHandle.ALL,
                    READ_PRIVILEGED_PHONE_STATE);
        } else {
            mPhone.getContext().sendBroadcast(intent, READ_PRIVILEGED_PHONE_STATE);
        }
    }

    /** Recovery Action: RECOVERY_ACTION_GET_DATA_CALL_LIST */
    private void getDataCallList() {
        log("getDataCallList: request data call list");
        mWwanDataServiceManager.requestDataCallList(null);
    }

    /** Recovery Action: RECOVERY_ACTION_CLEANUP */
    private void cleanUpDataNetwork() {
        log("cleanUpDataNetwork: notify clean up data network");
        mDataStallRecoveryManagerCallback.invokeFromExecutor(
                mDataStallRecoveryManagerCallback::onDataStallReestablishInternet);
    }

    /** Recovery Action: RECOVERY_ACTION_RADIO_RESTART */
    private void powerOffRadio() {
        log("powerOffRadio: Restart radio");
        mPhone.getServiceStateTracker().powerOffRadioSafely();
    }

    /** Recovery Action: RECOVERY_ACTION_RESET_MODEM */
    private void rebootModem() {
        log("rebootModem: reboot modem");
        mPhone.rebootModem(null);
    }

    /**
     * Initialize the network check timer.
     *
     * @param action The recovery action to start the network check timer.
     */
    private void startNetworkCheckTimer(@RecoveryAction int action) {
        // Ignore send message delayed due to reached the last action.
        if (action == RECOVERY_ACTION_RESET_MODEM) return;
        log("startNetworkCheckTimer(): " + getDataStallRecoveryDelayMillis(action) + "ms");
        if (!mNetworkCheckTimerStarted) {
            mNetworkCheckTimerStarted = true;
            mTimeLastRecoveryStartMs = SystemClock.elapsedRealtime();
            sendMessageDelayed(
                    obtainMessage(EVENT_SEND_DATA_STALL_BROADCAST),
                    getDataStallRecoveryDelayMillis(action));
        }
    }

    /** Cancel the network check timer. */
    private void cancelNetworkCheckTimer() {
        log("cancelNetworkCheckTimer()");
        if (mNetworkCheckTimerStarted) {
            mNetworkCheckTimerStarted = false;
            removeMessages(EVENT_SEND_DATA_STALL_BROADCAST);
        }
    }

    /**
     * Check the conditions if we need to do recovery action.
     *
     * @param isNeedToCheckTimer {@code true} indicating we need the check timer when
     * we receive the internet validation status changed.
     * @return {@code true} if need to do recovery action, {@code false} no need to do recovery
     *     action.
     */
    private boolean isRecoveryNeeded(boolean isNeedToCheckTimer) {
        logv("enter: isRecoveryNeeded()");

        // Skip if network is invalid and recovery was not started yet
        if (!mIsValidNetwork && !isRecoveryAlreadyStarted()) {
            logl("skip when network still remains invalid and recovery was not started yet");
            return false;
        }

        // Skip recovery if we have already attempted all steps.
        if (mIsAttemptedAllSteps) {
            logl("skip retrying continue recovery action");
            return false;
        }

        // To avoid back to back recovery, wait for a grace period
        if (getElapsedTimeSinceRecoveryMs() < getDataStallRecoveryDelayMillis(mLastAction)
                && isNeedToCheckTimer) {
            logl("skip back to back data stall recovery");
            return false;
        }

        // Skip recovery if it can cause a call to drop
        if (mPhone.getState() != PhoneConstants.State.IDLE
                && getRecoveryAction() > RECOVERY_ACTION_CLEANUP) {
            logl("skip data stall recovery as there is an active call");
            return false;
        }

        // Skip when poor signal strength
        if (mPhone.getSignalStrength().getLevel() <= CellSignalStrength.SIGNAL_STRENGTH_POOR) {
            logl("skip data stall recovery as in poor signal condition");
            return false;
        }

        if (!mDataNetworkController.isInternetDataAllowed(true/* ignoreExistingNetworks */)) {
            logl("skip data stall recovery as data not allowed.");
            return false;
        }

        if (!mIsInternetNetworkConnected) {
            logl("skip data stall recovery as data not connected");
            return false;
        }
        return true;
    }

    /**
     * Set the validation status into metrics.
     *
     * @param isValid true for validation passed & false for validation failed
     */
    private void setNetworkValidationState(boolean isValid) {
        boolean isLogNeeded = false;
        int timeDuration = 0;
        int timeDurationOfCurrentAction;
        boolean isFirstDataStall = false;
        boolean isFirstValidationAfterDoRecovery = false;
        @RecoveredReason int reason = getRecoveredReason(isValid);
        // Validation status is true and was not data stall.
        if (isValid && !mDataStalled) {
            return;
        }

        if (!mDataStalled) {
            // First data stall
            isLogNeeded = true;
            mDataStalled = true;
            isFirstDataStall = true;
            mDataStallStartMs = SystemClock.elapsedRealtime();
            logl("data stall: start time = " + DataUtils.elapsedTimeToString(mDataStallStartMs));
        } else if (!mLastActionReported) {
            // When the first validation status appears, enter this block.
            isLogNeeded = true;
            timeDuration = (int) (SystemClock.elapsedRealtime() - mDataStallStartMs);
            mLastActionReported = true;
            isFirstValidationAfterDoRecovery = true;
        }

        if (isValid) {
            // When the validation passed(mobile data resume), enter this block.
            isLogNeeded = true;
            timeDuration = (int) (SystemClock.elapsedRealtime() - mDataStallStartMs);
            mLastActionReported = false;
            mDataStalled = false;
        }

        if (isLogNeeded) {
            timeDurationOfCurrentAction =
                ((getRecoveryAction() > RECOVERY_ACTION_GET_DATA_CALL_LIST
                   && !mIsAttemptedAllSteps)
                 || mLastAction == RECOVERY_ACTION_RESET_MODEM)
                 ? (int) getDurationOfCurrentRecoveryMs() : 0;

            mStats.uploadMetrics(
                    mLastAction, isValid, timeDuration, reason,
                    isFirstValidationAfterDoRecovery, timeDurationOfCurrentAction);
            logl(
                    "data stall: "
                    + (isFirstDataStall ? "start" : !isValid ? "in process" : "end")
                    + ", lastaction="
                    + recoveryActionToString(mLastAction)
                    + ", isRecovered="
                    + isValid
                    + ", reason="
                    + recoveredReasonToString(reason)
                    + ", isFirstValidationAfterDoRecovery="
                    + isFirstValidationAfterDoRecovery
                    + ", TimeDuration="
                    + timeDuration
                    + ", TimeDurationForCurrentRecoveryAction="
                    + timeDurationOfCurrentAction);
        }
    }

    /**
     * Get the data stall recovered reason.
     *
     * @param isValid true for validation passed & false for validation failed
     */
    @RecoveredReason
    private int getRecoveredReason(boolean isValid) {
        if (!isValid) return RECOVERED_REASON_NONE;

        int ret = RECOVERED_REASON_DSRM;
        if (mRadioStateChangedDuringDataStall) {
            if (mLastAction <= RECOVERY_ACTION_CLEANUP) {
                ret = RECOVERED_REASON_MODEM;
            }
            if (mIsAirPlaneModeEnableDuringDataStall) {
                ret = RECOVERED_REASON_USER;
            }
        } else if (mMobileDataChangedToEnabledDuringDataStall) {
            ret = RECOVERED_REASON_USER;
        }
        return ret;
    }

    /** Perform a series of data stall recovery actions. */
    private void doRecovery() {
        @RecoveryAction final int recoveryAction = getRecoveryAction();
        final int signalStrength = mPhone.getSignalStrength().getLevel();

        TelephonyMetrics.getInstance()
                .writeSignalStrengthEvent(mPhone.getPhoneId(), signalStrength);
        TelephonyMetrics.getInstance().writeDataStallEvent(mPhone.getPhoneId(), recoveryAction);
        mLastAction = recoveryAction;
        mLastActionReported = false;
        mNetworkCheckTimerStarted = false;
        mTimeElapsedOfCurrentAction = SystemClock.elapsedRealtime();

        switch (recoveryAction) {
            case RECOVERY_ACTION_GET_DATA_CALL_LIST:
                logl("doRecovery(): get data call list");
                getDataCallList();
                setRecoveryAction(RECOVERY_ACTION_CLEANUP);
                break;
            case RECOVERY_ACTION_CLEANUP:
                logl("doRecovery(): cleanup all connections");
                cleanUpDataNetwork();
                setRecoveryAction(RECOVERY_ACTION_RADIO_RESTART);
                break;
            case RECOVERY_ACTION_RADIO_RESTART:
                logl("doRecovery(): restarting radio");
                setRecoveryAction(RECOVERY_ACTION_RESET_MODEM);
                powerOffRadio();
                break;
            case RECOVERY_ACTION_RESET_MODEM:
                logl("doRecovery(): modem reset");
                rebootModem();
                resetAction();
                mIsAttemptedAllSteps = true;
                break;
            default:
                throw new RuntimeException(
                        "doRecovery: Invalid recoveryAction = "
                                + recoveryActionToString(recoveryAction));
        }

        startNetworkCheckTimer(mLastAction);
    }

    /**
     * Convert @RecoveredReason to string
     *
     * @param reason The recovered reason.
     * @return The recovered reason in string format.
     */
    @NonNull
    private static String recoveredReasonToString(@RecoveredReason int reason) {
        return switch (reason) {
            case RECOVERED_REASON_NONE -> "RECOVERED_REASON_NONE";
            case RECOVERED_REASON_DSRM -> "RECOVERED_REASON_DSRM";
            case RECOVERED_REASON_MODEM -> "RECOVERED_REASON_MODEM";
            case RECOVERED_REASON_USER -> "RECOVERED_REASON_USER";
            default -> "Unknown(" + reason + ")";
        };
    }

    /**
     * Convert RadioPowerState to string
     *
     * @param state The radio power state
     * @return The radio power state in string format.
     */
    @NonNull
    private static String radioPowerStateToString(@RadioPowerState int state) {
        return switch (state) {
            case TelephonyManager.RADIO_POWER_OFF -> "RADIO_POWER_OFF";
            case TelephonyManager.RADIO_POWER_ON -> "RADIO_POWER_ON";
            case TelephonyManager.RADIO_POWER_UNAVAILABLE -> "RADIO_POWER_UNAVAILABLE";
            default -> "Unknown(" + state + ")";
        };
    }

    /**
     * Convert RecoveryAction to string
     *
     * @param action The recovery action
     * @return The recovery action in string format.
     */
    @NonNull
    private static String recoveryActionToString(@RecoveryAction int action) {
        return switch (action) {
            case RECOVERY_ACTION_GET_DATA_CALL_LIST -> "RECOVERY_ACTION_GET_DATA_CALL_LIST";
            case RECOVERY_ACTION_CLEANUP -> "RECOVERY_ACTION_CLEANUP";
            case RECOVERY_ACTION_RADIO_RESTART -> "RECOVERY_ACTION_RADIO_RESTART";
            case RECOVERY_ACTION_RESET_MODEM -> "RECOVERY_ACTION_RESET_MODEM";
            default -> "Unknown(" + action + ")";
        };
    }

    /**
     * Log debug messages.
     *
     * @param s debug messages
     */
    private void log(@NonNull String s) {
        Rlog.d(mLogTag, s);
    }

    /**
     * Log verbose messages.
     *
     * @param s debug messages.
     */
    private void logv(@NonNull String s) {
        if (VDBG) Rlog.v(mLogTag, s);
    }

    /**
     * Log error messages.
     *
     * @param s error messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(mLogTag, s);
    }

    /**
     * Log debug messages and also log into the local log.
     *
     * @param s debug messages
     */
    private void logl(@NonNull String s) {
        log(s);
        mLocalLog.log(s);
    }

    /**
     * Dump the state of DataStallRecoveryManager
     *
     * @param fd File descriptor
     * @param printWriter Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(
                DataStallRecoveryManager.class.getSimpleName() + "-" + mPhone.getPhoneId() + ":");
        pw.increaseIndent();

        pw.println("mIsValidNetwork=" + mIsValidNetwork);
        pw.println("mIsInternetNetworkConnected=" + mIsInternetNetworkConnected);
        pw.println("mIsAirPlaneModeEnableDuringDataStall=" + mIsAirPlaneModeEnableDuringDataStall);
        pw.println("mDataStalled=" + mDataStalled);
        pw.println("mLastAction=" + recoveryActionToString(mLastAction));
        pw.println("mIsAttemptedAllSteps=" + mIsAttemptedAllSteps);
        pw.println("mDataStallStartMs=" + DataUtils.elapsedTimeToString(mDataStallStartMs));
        pw.println("mRadioPowerState=" + radioPowerStateToString(mRadioPowerState));
        pw.println("mLastActionReported=" + mLastActionReported);
        pw.println("mTimeLastRecoveryStartMs="
                        + DataUtils.elapsedTimeToString(mTimeLastRecoveryStartMs));
        pw.println("getRecoveryAction()=" + recoveryActionToString(getRecoveryAction()));
        pw.println("mRadioStateChangedDuringDataStall=" + mRadioStateChangedDuringDataStall);
        pw.println(
                "mMobileDataChangedToEnabledDuringDataStall="
                        + mMobileDataChangedToEnabledDuringDataStall);
        pw.println("mPredictWaitingMillis=" + mPredictWaitingMillis);
        pw.println(
                "DataStallRecoveryDelayMillisArray="
                        + Arrays.toString(mDataStallRecoveryDelayMillisArray));
        pw.println("SkipRecoveryActionArray=" + Arrays.toString(mSkipRecoveryActionArray));
        pw.decreaseIndent();
        pw.println("");

        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
    }
}

/*
* Copyright (C) 2015 The Android Open Source Project
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

import static android.telephony.CarrierConfigManager.KEY_DATA_SWITCH_VALIDATION_TIMEOUT_LONG;
import static android.telephony.SubscriptionManager.DEFAULT_PHONE_INDEX;
import static android.telephony.SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
import static android.telephony.SubscriptionManager.INVALID_PHONE_INDEX;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;
import static android.telephony.TelephonyManager.SET_OPPORTUNISTIC_SUB_INACTIVE_SUBSCRIPTION;
import static android.telephony.TelephonyManager.SET_OPPORTUNISTIC_SUB_SUCCESS;
import static android.telephony.TelephonyManager.SET_OPPORTUNISTIC_SUB_VALIDATION_FAILED;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_CROSS_SIM;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_NONE;

import static java.util.Arrays.copyOf;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.TelephonyNetworkSpecifier;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyRegistryManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsRegistrationAttributes;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseIntArray;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.ISetOpportunisticDataCallback;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConfigurationManager;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RadioConfig;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.data.DataNetworkController.NetworkRequestList;
import com.android.internal.telephony.data.DataSettingsManager.DataSettingsManagerCallback;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.DataSwitch;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.internal.telephony.subscription.SubscriptionManagerService.SubscriptionManagerServiceCallback;
import com.android.internal.telephony.subscription.SubscriptionManagerService.WatchedInt;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Utility singleton to monitor subscription changes and incoming NetworkRequests
 * and determine which phone/phones are active.
 * <p>
 * Manages the ALLOW_DATA calls to modems and notifies phones about changes to
 * the active phones.  Note we don't wait for data attach (which may not happen anyway).
 */
public class PhoneSwitcher extends Handler {
    private static final String LOG_TAG = "PhoneSwitcher";
    protected static final boolean VDBG = Rlog.isLoggable(LOG_TAG, Log.VERBOSE);

    private static final int MODEM_COMMAND_RETRY_PERIOD_MS     = 5000;
    // After the emergency call ends, wait for a few seconds to see if we enter ECBM before starting
    // the countdown to remove the emergency DDS override.
    @VisibleForTesting
    // not final for testing.
    public static int ECBM_DEFAULT_DATA_SWITCH_BASE_TIME_MS = 5000;
    // Wait for a few seconds after the override request comes in to receive the outgoing call
    // event. If it does not happen before the timeout specified, cancel the override.
    @VisibleForTesting
    public static int DEFAULT_DATA_OVERRIDE_TIMEOUT_MS = 5000;

    // If there are no subscriptions in a device, then the phone to be used for emergency should
    // always be the "first" phone.
    private static final int DEFAULT_EMERGENCY_PHONE_ID = 0;

    /**
     * Container for an ongoing request to override the DDS in the context of an ongoing emergency
     * call to allow for carrier specific operations, such as provide SUPL updates during or after
     * the emergency call, since some modems do not support these operations on the non DDS.
     */
    private static final class EmergencyOverrideRequest {
        /* The Phone ID that the DDS should be set to. */
        int mPhoneId = INVALID_PHONE_INDEX;
        /* The time after the emergency call ends that the DDS should be overridden for. */
        int mGnssOverrideTimeMs = -1;
        /* A callback to the requester notifying them if the initial call to the modem to override
         * the DDS was successful.
         */
        CompletableFuture<Boolean> mOverrideCompleteFuture;
        /* In the special case that the device goes into emergency callback mode after the emergency
         * call ends, keep the override until ECM finishes and then start the mGnssOverrideTimeMs
         * timer to leave DDS override.
         */
        boolean mRequiresEcmFinish = false;

        /*
         * Keeps track of whether or not this request has already serviced the outgoing emergency
         * call. Once finished, do not delay for any other calls.
         */
        boolean mPendingOriginatingCall = true;

        /**
         * @return true if there is a pending override complete callback.
         */
        boolean isCallbackAvailable() {
            return mOverrideCompleteFuture != null;
        }

        /**
         * Send the override complete callback the result of setting the DDS to the new value.
         */
        void sendOverrideCompleteCallbackResultAndClear(boolean result) {
            if (isCallbackAvailable()) {
                mOverrideCompleteFuture.complete(result);
                mOverrideCompleteFuture = null;
            }
        }


        @Override
        public String toString() {
            return String.format("EmergencyOverrideRequest: [phoneId= %d, overrideMs= %d,"
                    + " hasCallback= %b, ecmFinishStatus= %b]", mPhoneId, mGnssOverrideTimeMs,
                    isCallbackAvailable(), mRequiresEcmFinish);
        }
    }

    /**
     * Callback from PhoneSwitcher
     */
    public static class PhoneSwitcherCallback extends DataCallback {
        /**
         * Constructor
         *
         * @param executor The executor of the callback.
         */
        public PhoneSwitcherCallback(@NonNull @CallbackExecutor Executor executor) {
            super(executor);
        }

        /**
         * Called when preferred data phone id changed.
         *
         * @param phoneId The phone id of the preferred data.
         */
        public void onPreferredDataPhoneIdChanged(int phoneId) {}
    }

    @NonNull
    private final NetworkRequestList mNetworkRequestList = new NetworkRequestList();
    protected final RegistrantList mActivePhoneRegistrants;
    private final SubscriptionManagerService mSubscriptionManagerService;
    @NonNull
    private final FeatureFlags mFlags;
    protected final Context mContext;
    private final LocalLog mLocalLog;
    protected PhoneState[] mPhoneStates;
    protected int[] mPhoneSubscriptions;
    private boolean mIsRegisteredForImsRadioTechChange;
    @VisibleForTesting
    protected final CellularNetworkValidator mValidator;
    private int mPendingSwitchSubId = INVALID_SUBSCRIPTION_ID;
    /** The reason for the last time changing preferred data sub **/
    private int mLastSwitchPreferredDataReason = -1;
    private boolean mPendingSwitchNeedValidation;
    @VisibleForTesting
    public final CellularNetworkValidator.ValidationCallback mValidationCallback =
            new CellularNetworkValidator.ValidationCallback() {
                @Override
                public void onValidationDone(boolean validated, int subId) {
                    Message.obtain(PhoneSwitcher.this,
                            EVENT_NETWORK_VALIDATION_DONE, subId, validated ? 1 : 0).sendToTarget();
                }

                @Override
                public void onNetworkAvailable(Network network, int subId) {
                    Message.obtain(PhoneSwitcher.this,
                            EVENT_NETWORK_AVAILABLE, subId, 0, network).sendToTarget();

                }
            };

    // How many phones (correspondingly logical modems) are allowed for PS attach. This is used
    // when we specifically use setDataAllowed to initiate on-demand PS(data) attach for each phone.
    protected int mMaxDataAttachModemCount;
    // Local cache of TelephonyManager#getActiveModemCount(). 1 if in single SIM mode, 2 if in dual
    // SIM mode.
    protected int mActiveModemCount;
    protected static PhoneSwitcher sPhoneSwitcher = null;

    // Which primary (non-opportunistic) subscription is set as data subscription among all primary
    // subscriptions. This value usually comes from user setting, and it's the subscription used for
    // Internet data if mOpptDataSubId is not set.
    protected int mPrimaryDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    // The automatically suggested preferred data subId (by e.g. CBRS or auto data switch), a
    // candidate for preferred data subId, which is eventually presided by
    // updatePreferredDataPhoneId().
    // If CBRS/auto switch feature selects the primary data subId as the preferred data subId,
    // its value will be DEFAULT_SUBSCRIPTION_ID.
    private int mAutoSelectedDataSubId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;

    // The phone ID that has an active voice call. If set, and its mobile data setting is on,
    // it will become the mPreferredDataPhoneId.
    protected int mPhoneIdInVoiceCall = SubscriptionManager.INVALID_PHONE_INDEX;

    @VisibleForTesting
    // It decides:
    // 1. In modem layer, which modem is DDS (preferred to have data traffic on)
    // 2. In TelephonyNetworkProvider, which subscription will apply default network requests, which
    //    are requests without specifying a subId.
    // Corresponding phoneId after considering mOpptDataSubId, mPrimaryDataSubId and
    // mPhoneIdInVoiceCall above.
    protected int mPreferredDataPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;

    // Subscription ID corresponds to mPreferredDataPhoneId.
    protected WatchedInt mPreferredDataSubId = new WatchedInt(
            SubscriptionManager.INVALID_SUBSCRIPTION_ID);

    // If non-null, An emergency call is about to be started, is ongoing, or has just ended and we
    // are overriding the DDS.
    // Internal state, should ONLY be accessed/modified inside of the handler.
    private EmergencyOverrideRequest mEmergencyOverride;

    private ISetOpportunisticDataCallback mSetOpptSubCallback;

    /** Phone switcher callbacks. */
    @NonNull
    private final Set<PhoneSwitcherCallback> mPhoneSwitcherCallbacks = new ArraySet<>();

    private static final int EVENT_PRIMARY_DATA_SUB_CHANGED       = 101;
    protected static final int EVENT_SUBSCRIPTION_CHANGED         = 102;
    // ECBM has started/ended. If we just ended an emergency call and mEmergencyOverride is not
    // null, we will wait for EVENT_EMERGENCY_TOGGLE again with ECBM ending to send the message
    // EVENT_REMOVE_DDS_EMERGENCY_OVERRIDE to remove the override after the mEmergencyOverride
    // override timer ends.
    private static final int EVENT_EMERGENCY_TOGGLE               = 105;
    private static final int EVENT_RADIO_CAPABILITY_CHANGED       = 106;
    private static final int EVENT_OPPT_DATA_SUB_CHANGED          = 107;
    private static final int EVENT_RADIO_ON                       = 108;
    // A call has either started or ended. If an emergency ended and DDS is overridden using
    // mEmergencyOverride, start the countdown to remove the override using the message
    // EVENT_REMOVE_DDS_EMERGENCY_OVERRIDE. The only exception to this is if the device moves to
    // ECBM, which is detected by EVENT_EMERGENCY_TOGGLE.
    private static final int EVENT_PRECISE_CALL_STATE_CHANGED     = 109;
    private static final int EVENT_NETWORK_VALIDATION_DONE        = 110;

    private static final int EVENT_MODEM_COMMAND_DONE             = 112;
    private static final int EVENT_MODEM_COMMAND_RETRY            = 113;

    // An emergency call is about to be originated and requires the DDS to be overridden.
    // Uses EVENT_PRECISE_CALL_STATE_CHANGED message to start countdown to finish override defined
    // in mEmergencyOverride. If EVENT_PRECISE_CALL_STATE_CHANGED does not come in
    // DEFAULT_DATA_OVERRIDE_TIMEOUT_MS milliseconds, then the override will be removed.
    private static final int EVENT_OVERRIDE_DDS_FOR_EMERGENCY     = 115;
    // If it exists, remove the current mEmergencyOverride DDS override.
    private static final int EVENT_REMOVE_DDS_EMERGENCY_OVERRIDE  = 116;
    // If it exists, remove the current mEmergencyOverride DDS override.
    private static final int EVENT_MULTI_SIM_CONFIG_CHANGED       = 117;
    private static final int EVENT_NETWORK_AVAILABLE              = 118;
    private static final int EVENT_PROCESS_SIM_STATE_CHANGE       = 119;
    private static final int EVENT_IMS_RADIO_TECH_CHANGED         = 120;

    // List of events triggers re-evaluations
    private static final String EVALUATION_REASON_RADIO_ON = "EVENT_RADIO_ON";

    // Depending on version of IRadioConfig, we need to send either RIL_REQUEST_ALLOW_DATA if it's
    // 1.0, or RIL_REQUEST_SET_PREFERRED_DATA if it's 1.1 or later. So internally mHalCommandToUse
    // will be either HAL_COMMAND_ALLOW_DATA or HAL_COMMAND_ALLOW_DATA or HAL_COMMAND_UNKNOWN.
    protected static final int HAL_COMMAND_UNKNOWN        = 0;
    protected static final int HAL_COMMAND_ALLOW_DATA     = 1;
    protected static final int HAL_COMMAND_PREFERRED_DATA = 2;
    protected int mHalCommandToUse = HAL_COMMAND_UNKNOWN;

    protected RadioConfig mRadioConfig;

    private static final int MAX_LOCAL_LOG_LINES = 256;

    // Default timeout value of network validation in millisecond.
    private static final int DEFAULT_VALIDATION_EXPIRATION_TIME = 2000;

    /** Controller that tracks {@link TelephonyManager#MOBILE_DATA_POLICY_AUTO_DATA_SWITCH} */
    @NonNull private final AutoDataSwitchController mAutoDataSwitchController;
    /** Callback to deal with requests made by the auto data switch controller. */
    @NonNull private final AutoDataSwitchController.AutoDataSwitchControllerCallback
            mAutoDataSwitchCallback;

    private final ConnectivityManager mConnectivityManager;
    private int mImsRegistrationTech = REGISTRATION_TECH_NONE;
    @VisibleForTesting
    public final SparseIntArray mImsRegistrationRadioTechMap = new SparseIntArray();
    @NonNull
    private final List<Set<CommandException.Error>> mCurrentDdsSwitchFailure;

    /** Data settings manager callback. Key is the phone id. */
    @NonNull
    private final Map<Integer, DataSettingsManagerCallback> mDataSettingsManagerCallbacks =
            new ArrayMap<>();

    private class DefaultNetworkCallback extends ConnectivityManager.NetworkCallback {
        public int mExpectedSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        public int mSwitchReason = TelephonyEvent.DataSwitch.Reason.DATA_SWITCH_REASON_UNKNOWN;
        @Override
        public void onCapabilitiesChanged(@NonNull Network network,
                NetworkCapabilities networkCapabilities) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                if (SubscriptionManager.isValidSubscriptionId(mExpectedSubId)
                        && mExpectedSubId == getSubIdFromNetworkSpecifier(
                        networkCapabilities.getNetworkSpecifier())) {
                    logDataSwitchEvent(
                            mExpectedSubId,
                            TelephonyEvent.EventState.EVENT_STATE_END,
                            mSwitchReason);
                    mExpectedSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                    mSwitchReason = TelephonyEvent.DataSwitch.Reason.DATA_SWITCH_REASON_UNKNOWN;
                }
            }
            mAutoDataSwitchController.updateDefaultNetworkCapabilities(networkCapabilities);
        }

        @Override
        public void onLost(@NonNull Network network) {
            mAutoDataSwitchController.updateDefaultNetworkCapabilities(null);
        }
    }

    private final RegistrationManager.RegistrationCallback mRegistrationCallback =
            new RegistrationManager.RegistrationCallback() {
        @Override
        public void onRegistered(@NonNull ImsRegistrationAttributes attributes) {
            int imsRegistrationTech = attributes.getRegistrationTechnology();
            if (imsRegistrationTech != mImsRegistrationTech) {
                mImsRegistrationTech = imsRegistrationTech;
                sendMessage(obtainMessage(EVENT_IMS_RADIO_TECH_CHANGED));
            }
        }

        @Override
        public void onUnregistered(@NonNull ImsReasonInfo info) {
            if (mImsRegistrationTech != REGISTRATION_TECH_NONE) {
                mImsRegistrationTech = REGISTRATION_TECH_NONE;
                sendMessage(obtainMessage(EVENT_IMS_RADIO_TECH_CHANGED));
            }
        }
    };

    private final DefaultNetworkCallback mDefaultNetworkCallback = new DefaultNetworkCallback();

    /**
     * Interface to get ImsRegistrationTech. It's a wrapper of ImsManager#getRegistrationTech,
     * to make it mock-able in unittests.
     */
    public interface ImsRegTechProvider {
        /** Get IMS registration tech. */
        @ImsRegistrationImplBase.ImsRegistrationTech int get(Context context, int phoneId);
    }

    @VisibleForTesting
    public ImsRegTechProvider mImsRegTechProvider =
            (context, phoneId) -> ImsManager.getInstance(context, phoneId).getRegistrationTech();

    /**
     * Interface to register RegistrationCallback. It's a wrapper of
     * ImsManager#addRegistrationCallback, to make it mock-able in unittests.
     */
    public interface ImsRegisterCallback {
        /** Set RegistrationCallback. */
        void setCallback(Context context, int phoneId, RegistrationManager.RegistrationCallback cb,
                Executor executor) throws ImsException;
    }

    @VisibleForTesting
    public ImsRegisterCallback mImsRegisterCallback =
            (context, phoneId, cb, executor)-> ImsManager.getInstance(context, phoneId)
                    .addRegistrationCallback(cb, executor);

    /**
     * Method to get singleton instance.
     */
    public static PhoneSwitcher getInstance() {
        return sPhoneSwitcher;
    }

    /**
     * Method to create singleton instance.
     */
    public static PhoneSwitcher make(int maxDataAttachModemCount, Context context, Looper looper,
            @NonNull FeatureFlags flags) {
        if (sPhoneSwitcher == null) {
            sPhoneSwitcher = new PhoneSwitcher(maxDataAttachModemCount, context, looper, flags);
        }

        return sPhoneSwitcher;
    }

    private boolean updatesIfPhoneInVoiceCallChanged() {
        int oldPhoneIdInVoiceCall = mPhoneIdInVoiceCall;
        // If there's no active call, the value will become INVALID_PHONE_INDEX
        // and internet data will be switched back to system selected or user selected
        // subscription.
        mPhoneIdInVoiceCall = SubscriptionManager.INVALID_PHONE_INDEX;
        for (Phone phone : PhoneFactory.getPhones()) {
            if (isPhoneInVoiceCall(phone) || isPhoneInVoiceCall(phone.getImsPhone())) {
                mPhoneIdInVoiceCall = phone.getPhoneId();
                break;
            }
        }

        if (mPhoneIdInVoiceCall != oldPhoneIdInVoiceCall) {
            logl("isPhoneInVoiceCallChanged from phoneId " + oldPhoneIdInVoiceCall
                    + " to phoneId " + mPhoneIdInVoiceCall);
            return true;
        } else {
            return false;
        }
    }

    private void registerForImsRadioTechChange(Context context, int phoneId) {
        try {
            mImsRegisterCallback.setCallback(context, phoneId, mRegistrationCallback, this::post);
            mIsRegisteredForImsRadioTechChange = true;
        } catch (ImsException imsException) {
            mIsRegisteredForImsRadioTechChange = false;
        }
    }

    private void registerForImsRadioTechChange() {
        // register for radio tech change to listen to radio tech handover.
        if (!mIsRegisteredForImsRadioTechChange) {
            for (int i = 0; i < mActiveModemCount; i++) {
                registerForImsRadioTechChange(mContext, i);
            }
        }
    }

    /**
     * Register the callback for receiving information from {@link PhoneSwitcher}.
     *
     * @param callback The callback.
     */
    public void registerCallback(@NonNull PhoneSwitcherCallback callback) {
        mPhoneSwitcherCallbacks.add(callback);
    }

    /**
     * Unregister the callback for receiving information from {@link PhoneSwitcher}.
     *
     * @param callback The callback.
     */
    public void unregisterCallback(@NonNull PhoneSwitcherCallback callback) {
        mPhoneSwitcherCallbacks.remove(callback);
    }

    private void evaluateIfImmediateDataSwitchIsNeeded(String evaluationReason, int switchReason) {
        if (onEvaluate(REQUESTS_UNCHANGED, evaluationReason)) {
            logDataSwitchEvent(mPreferredDataSubId.get(),
                    TelephonyEvent.EventState.EVENT_STATE_START,
                    switchReason);
            registerDefaultNetworkChangeCallback(mPreferredDataSubId.get(),
                    switchReason);
        }
    }

    @VisibleForTesting
    public PhoneSwitcher(int maxActivePhones, Context context, Looper looper,
            @NonNull FeatureFlags featureFlags) {
        super(looper);
        mContext = context;
        mFlags = featureFlags;
        mActiveModemCount = getTm().getActiveModemCount();
        mPhoneSubscriptions = new int[mActiveModemCount];
        mPhoneStates = new PhoneState[mActiveModemCount];
        mMaxDataAttachModemCount = maxActivePhones;
        mLocalLog = new LocalLog(MAX_LOCAL_LOG_LINES);

        mSubscriptionManagerService = SubscriptionManagerService.getInstance();

        mRadioConfig = RadioConfig.getInstance();
        mValidator = CellularNetworkValidator.getInstance();

        mCurrentDdsSwitchFailure = new ArrayList<>();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        mContext.registerReceiver(mSimStateIntentReceiver, filter);

        mActivePhoneRegistrants = new RegistrantList();
        for (int phoneId = 0; phoneId < mActiveModemCount; phoneId++) {
            mPhoneStates[phoneId] = new PhoneState();
            Phone phone = PhoneFactory.getPhone(phoneId);
            if (phone != null) {
                phone.registerForEmergencyCallToggle(
                        this, EVENT_EMERGENCY_TOGGLE, null);
                // TODO (b/135566422): combine register for both GsmCdmaPhone and ImsPhone.
                phone.registerForPreciseCallStateChanged(
                        this, EVENT_PRECISE_CALL_STATE_CHANGED, null);
                if (phone.getImsPhone() != null) {
                    phone.getImsPhone().registerForPreciseCallStateChanged(
                            this, EVENT_PRECISE_CALL_STATE_CHANGED, null);
                    if (mFlags.changeMethodOfObtainingImsRegistrationRadioTech()) {
                        // Initialize IMS registration tech
                        mImsRegistrationRadioTechMap.put(phoneId, REGISTRATION_TECH_NONE);
                        ((ImsPhone) phone.getImsPhone()).registerForImsRegistrationChanges(
                                this, EVENT_IMS_RADIO_TECH_CHANGED, null);

                        log("register handler to receive IMS registration : " + phoneId);
                    }
                }
                mDataSettingsManagerCallbacks.computeIfAbsent(phoneId,
                        v -> new DataSettingsManagerCallback(this::post) {
                            @Override
                            public void onDataEnabledChanged(boolean enabled,
                                    @TelephonyManager.DataEnabledChangedReason int reason,
                                    @NonNull String callingPackage) {
                                PhoneSwitcher.this.onDataEnabledChanged();
                            }
                            @Override
                            public void onDataRoamingEnabledChanged(boolean enabled) {
                                PhoneSwitcher.this.mAutoDataSwitchController.evaluateAutoDataSwitch(
                                        AutoDataSwitchController
                                                .EVALUATION_REASON_DATA_SETTINGS_CHANGED);
                            }});
                phone.getDataSettingsManager().registerCallback(
                        mDataSettingsManagerCallbacks.get(phoneId));

                if (!mFlags.changeMethodOfObtainingImsRegistrationRadioTech()) {
                    registerForImsRadioTechChange(context, phoneId);
                }
            }
            Set<CommandException.Error> ddsFailure = new HashSet<>();
            mCurrentDdsSwitchFailure.add(ddsFailure);
        }

        if (mActiveModemCount > 0) {
            PhoneFactory.getPhone(0).mCi.registerForOn(this, EVENT_RADIO_ON, null);
        }

        TelephonyRegistryManager telephonyRegistryManager = (TelephonyRegistryManager)
                context.getSystemService(Context.TELEPHONY_REGISTRY_SERVICE);
        telephonyRegistryManager.addOnSubscriptionsChangedListener(
                mSubscriptionsChangedListener, this::post);

        mConnectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        mAutoDataSwitchCallback = new AutoDataSwitchController.AutoDataSwitchControllerCallback() {
            @Override
            public void onRequireValidation(int targetPhoneId, boolean needValidation) {
                int targetSubId = targetPhoneId == DEFAULT_PHONE_INDEX
                        ? DEFAULT_SUBSCRIPTION_ID
                        : mSubscriptionManagerService.getSubId(targetPhoneId);
                PhoneSwitcher.this.validate(targetSubId, needValidation,
                        DataSwitch.Reason.DATA_SWITCH_REASON_AUTO, null);
            }

            @Override
            public void onRequireImmediatelySwitchToPhone(int targetPhoneId,
                    @AutoDataSwitchController.AutoDataSwitchEvaluationReason int reason) {
                PhoneSwitcher.this.mAutoSelectedDataSubId =
                        targetPhoneId == DEFAULT_PHONE_INDEX
                                ? DEFAULT_SUBSCRIPTION_ID
                                : mSubscriptionManagerService.getSubId(targetPhoneId);
                PhoneSwitcher.this.evaluateIfImmediateDataSwitchIsNeeded(
                        AutoDataSwitchController.evaluationReasonToString(reason),
                        DataSwitch.Reason.DATA_SWITCH_REASON_MANUAL);
            }

            @Override
            public void onRequireCancelAnyPendingAutoSwitchValidation() {
                PhoneSwitcher.this.cancelPendingAutoDataSwitchValidation();
            }
        };
        mAutoDataSwitchController = new AutoDataSwitchController(context, looper, this,
                mFlags, mAutoDataSwitchCallback);
        if (!mFlags.ddsCallback()) {
            mContext.registerReceiver(mDefaultDataChangedReceiver,
                    new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED));
        } else {
            mSubscriptionManagerService.registerCallback(new SubscriptionManagerServiceCallback(
                    this::post) {
                @Override
                public void onDefaultDataSubscriptionChanged(int subId) {
                    evaluateIfImmediateDataSwitchIsNeeded("default data sub changed to " + subId,
                            DataSwitch.Reason.DATA_SWITCH_REASON_MANUAL);
                }
            });
        }

        PhoneConfigurationManager.registerForMultiSimConfigChange(
                this, EVENT_MULTI_SIM_CONFIG_CHANGED, null);

        mConnectivityManager.registerDefaultNetworkCallback(mDefaultNetworkCallback, this);
        updateHalCommandToUse();

        logl("PhoneSwitcher started");
    }

    private final BroadcastReceiver mDefaultDataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message msg = PhoneSwitcher.this.obtainMessage(EVENT_PRIMARY_DATA_SUB_CHANGED);
            msg.sendToTarget();
        }
    };

    private final BroadcastReceiver mSimStateIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED)) {
                int state = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                        TelephonyManager.SIM_STATE_UNKNOWN);
                int slotIndex = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                logl("mSimStateIntentReceiver: slotIndex = " + slotIndex + " state = " + state);
                obtainMessage(EVENT_PROCESS_SIM_STATE_CHANGE, slotIndex, state).sendToTarget();
            }
        }
    };

    private boolean isSimApplicationReady(int slotIndex) {
        if (!SubscriptionManager.isValidSlotIndex(slotIndex)) {
            return false;
        }

        SubscriptionInfo info = mSubscriptionManagerService
                .getActiveSubscriptionInfoForSimSlotIndex(slotIndex,
                        mContext.getOpPackageName(), mContext.getAttributionTag());
        boolean uiccAppsEnabled = info != null && info.areUiccApplicationsEnabled();

        IccCard iccCard = PhoneFactory.getPhone(slotIndex).getIccCard();
        if (!iccCard.isEmptyProfile() && uiccAppsEnabled) {
            logl("isSimApplicationReady: SIM is ready for slotIndex: " + slotIndex);
            return true;
        } else {
            return false;
        }
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionsChangedListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            Message msg = PhoneSwitcher.this.obtainMessage(EVENT_SUBSCRIPTION_CHANGED);
            msg.sendToTarget();
        }
    };

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_SUBSCRIPTION_CHANGED: {
                onEvaluate(REQUESTS_UNCHANGED, "subscription changed");
                break;
            }
            case EVENT_PRIMARY_DATA_SUB_CHANGED: {
                evaluateIfImmediateDataSwitchIsNeeded("primary data sub changed",
                        DataSwitch.Reason.DATA_SWITCH_REASON_MANUAL);
                break;
            }
            case EVENT_EMERGENCY_TOGGLE: {
                boolean isInEcm = isInEmergencyCallbackMode();
                if (mEmergencyOverride != null) {
                    logl("Emergency override - ecbm status = " + isInEcm);
                    if (isInEcm) {
                        // The device has gone into ECBM. Wait until it's out.
                        removeMessages(EVENT_REMOVE_DDS_EMERGENCY_OVERRIDE);
                        mEmergencyOverride.mRequiresEcmFinish = true;
                    } else if (mEmergencyOverride.mRequiresEcmFinish) {
                        // we have exited ECM! Start the timer to exit DDS override.
                        Message msg2 = obtainMessage(EVENT_REMOVE_DDS_EMERGENCY_OVERRIDE);
                        sendMessageDelayed(msg2, mEmergencyOverride.mGnssOverrideTimeMs);
                    }
                }
                onEvaluate(REQUESTS_CHANGED, "emergencyToggle");
                break;
            }
            case EVENT_RADIO_CAPABILITY_CHANGED: {
                final int phoneId = msg.arg1;
                sendRilCommands(phoneId);
                break;
            }
            case EVENT_OPPT_DATA_SUB_CHANGED: {
                int subId = msg.arg1;
                boolean needValidation = (msg.arg2 == 1);
                ISetOpportunisticDataCallback callback =
                        (ISetOpportunisticDataCallback) msg.obj;
                setOpportunisticDataSubscription(subId, needValidation, callback);
                break;
            }
            case EVENT_RADIO_ON: {
                updateHalCommandToUse();
                onEvaluate(REQUESTS_UNCHANGED, EVALUATION_REASON_RADIO_ON);
                break;
            }
            case EVENT_IMS_RADIO_TECH_CHANGED: {
                // register for radio tech change to listen to radio tech handover in case previous
                // attempt was not successful
                if (!mFlags.changeMethodOfObtainingImsRegistrationRadioTech()) {
                    registerForImsRadioTechChange();
                } else {
                    if (msg.obj == null) {
                        log("EVENT_IMS_RADIO_TECH_CHANGED but parameter is not available");
                        break;
                    }
                    if (!onImsRadioTechChanged((AsyncResult) (msg.obj))) {
                        break;
                    }
                }

                // if voice call state changes or in voice call didn't change
                // but RAT changes(e.g. Iwlan -> cross sim), reevaluate for data switch.
                if (updatesIfPhoneInVoiceCallChanged() || isAnyVoiceCallActiveOnDevice()) {
                    evaluateIfImmediateDataSwitchIsNeeded("Ims radio tech changed",
                            DataSwitch.Reason.DATA_SWITCH_REASON_IN_CALL);
                }
                break;
            }
            case EVENT_PRECISE_CALL_STATE_CHANGED: {
                // register for radio tech change to listen to radio tech handover in case previous
                // attempt was not successful
                if (!mFlags.changeMethodOfObtainingImsRegistrationRadioTech()) {
                    registerForImsRadioTechChange();
                }

                // If the phoneId in voice call didn't change, do nothing.
                if (!updatesIfPhoneInVoiceCallChanged()) {
                    break;
                }

                if (!isAnyVoiceCallActiveOnDevice()) {
                    for (int i = 0; i < mActiveModemCount; i++) {
                        if (mCurrentDdsSwitchFailure.get(i).contains(
                                CommandException.Error.OP_NOT_ALLOWED_DURING_VOICE_CALL)
                                 && isPhoneIdValidForRetry(i)) {
                            sendRilCommands(i);
                        }
                    }
                }

                // Only handle this event if we are currently waiting for the emergency call
                // associated with the override request to start or end.
                if (mEmergencyOverride != null && mEmergencyOverride.mPendingOriginatingCall) {
                    removeMessages(EVENT_REMOVE_DDS_EMERGENCY_OVERRIDE);
                    if (mPhoneIdInVoiceCall == SubscriptionManager.INVALID_PHONE_INDEX) {
                        // not in a call anymore.
                        Message msg2 = obtainMessage(EVENT_REMOVE_DDS_EMERGENCY_OVERRIDE);
                        sendMessageDelayed(msg2, mEmergencyOverride.mGnssOverrideTimeMs
                                + ECBM_DEFAULT_DATA_SWITCH_BASE_TIME_MS);
                        // Do not extend the emergency override by waiting for other calls to end.
                        // If it needs to be extended, a new request will come in and replace the
                        // current override.
                        mEmergencyOverride.mPendingOriginatingCall = false;
                    }
                }
                // Always update data modem via data during call code path, because
                // mAutoSelectedDataSubId doesn't know about any data switch due to voice call
                evaluateIfImmediateDataSwitchIsNeeded("precise call state changed",
                        DataSwitch.Reason.DATA_SWITCH_REASON_IN_CALL);
                if (!isAnyVoiceCallActiveOnDevice()) {
                    // consider auto switch on hang up all voice call
                    mAutoDataSwitchController.evaluateAutoDataSwitch(
                            AutoDataSwitchController.EVALUATION_REASON_VOICE_CALL_END);
                }
                break;
            }
            case EVENT_NETWORK_VALIDATION_DONE: {
                int subId = msg.arg1;
                boolean passed = (msg.arg2 == 1);
                onValidationDone(subId, passed);
                break;
            }
            case EVENT_NETWORK_AVAILABLE: {
                int subId = msg.arg1;
                Network network = (Network) msg.obj;
                onNetworkAvailable(subId, network);
                break;
            }
            case EVENT_MODEM_COMMAND_DONE: {
                AsyncResult ar = (AsyncResult) msg.obj;
                onDdsSwitchResponse(ar);
                break;
            }
            case EVENT_MODEM_COMMAND_RETRY: {
                int phoneId = (int) msg.obj;
                if (mActiveModemCount <= phoneId) {
                    break;
                }
                if (isPhoneIdValidForRetry(phoneId)) {
                    logl("EVENT_MODEM_COMMAND_RETRY: resend modem command on phone " + phoneId);
                    sendRilCommands(phoneId);
                } else {
                    logl("EVENT_MODEM_COMMAND_RETRY: skip retry as DDS sub changed");
                    mCurrentDdsSwitchFailure.get(phoneId).clear();
                }
                break;
            }
            case EVENT_OVERRIDE_DDS_FOR_EMERGENCY: {
                EmergencyOverrideRequest req = (EmergencyOverrideRequest) msg.obj;
                if (mEmergencyOverride != null) {
                    // If an override request comes in for a different phone ID than what is already
                    // being overridden, ignore. We should not try to switch DDS while already
                    // waiting for SUPL.
                    if (mEmergencyOverride.mPhoneId != req.mPhoneId) {
                        logl("emergency override requested for phone id " + req.mPhoneId + " when "
                                + "there is already an override in place for phone id "
                                + mEmergencyOverride.mPhoneId + ". Ignoring.");
                        if (req.isCallbackAvailable()) {
                            // Send failed result
                            req.mOverrideCompleteFuture.complete(false);
                        }
                        break;
                    } else {
                        if (mEmergencyOverride.isCallbackAvailable()) {
                            // Unblock any waiting overrides if a new request comes in before the
                            // previous one is processed.
                            mEmergencyOverride.mOverrideCompleteFuture.complete(false);
                        }
                    }
                }
                mEmergencyOverride = req;

                logl("new emergency override - " + mEmergencyOverride);
                // a new request has been created, remove any previous override complete scheduled.
                removeMessages(EVENT_REMOVE_DDS_EMERGENCY_OVERRIDE);
                Message msg2 = obtainMessage(EVENT_REMOVE_DDS_EMERGENCY_OVERRIDE);
                // Make sure that if we never get an incall indication that we remove the override.
                sendMessageDelayed(msg2, DEFAULT_DATA_OVERRIDE_TIMEOUT_MS);
                // Wait for call to end and EVENT_PRECISE_CALL_STATE_CHANGED to be called, then
                // start timer to remove DDS emergency override.
                if (!onEvaluate(REQUESTS_UNCHANGED, "emer_override_dds")) {
                    // Nothing changed as a result of override, so no modem command was sent. Treat
                    // as success.
                    mEmergencyOverride.sendOverrideCompleteCallbackResultAndClear(true);
                    // Do not clear mEmergencyOverride here, as we still want to keep the override
                    // active for the time specified in case the user tries to switch default data.
                }
                break;
            }
            case EVENT_REMOVE_DDS_EMERGENCY_OVERRIDE: {
                logl("Emergency override removed - " + mEmergencyOverride);
                mEmergencyOverride = null;
                onEvaluate(REQUESTS_UNCHANGED, "emer_rm_override_dds");
                break;
            }
            case EVENT_MULTI_SIM_CONFIG_CHANGED: {
                int activeModemCount = (int) ((AsyncResult) msg.obj).result;
                onMultiSimConfigChanged(activeModemCount);
                break;
            }
            case EVENT_PROCESS_SIM_STATE_CHANGE: {
                int slotIndex = msg.arg1;
                int simState = msg.arg2;

                if (!SubscriptionManager.isValidSlotIndex(slotIndex)) {
                    logl("EVENT_PROCESS_SIM_STATE_CHANGE: skip processing due to invalid slotId: "
                            + slotIndex);
                } else if (TelephonyManager.SIM_STATE_LOADED == simState) {
                    if (mCurrentDdsSwitchFailure.get(slotIndex).contains(
                        CommandException.Error.INVALID_SIM_STATE)
                        && isSimApplicationReady(slotIndex)) {
                        sendRilCommands(slotIndex);
                    }
                    // SIM loaded after subscriptions slot mapping are done. Evaluate for auto
                    // data switch.
                    mAutoDataSwitchController.evaluateAutoDataSwitch(
                            AutoDataSwitchController.EVALUATION_REASON_SIM_LOADED);
                }
                break;
            }
        }
    }

    /**
     * Only provide service for the handler of PhoneSwitcher.
     * @return true if the radio tech changed, otherwise false
     */
    private boolean onImsRadioTechChanged(@NonNull AsyncResult asyncResult) {
        ImsPhone.ImsRegistrationRadioTechInfo imsRegistrationRadioTechInfo =
                (ImsPhone.ImsRegistrationRadioTechInfo) asyncResult.result;
        if (imsRegistrationRadioTechInfo == null
                || imsRegistrationRadioTechInfo.phoneId() == INVALID_PHONE_INDEX
                || imsRegistrationRadioTechInfo.imsRegistrationState()
                == RegistrationManager.REGISTRATION_STATE_REGISTERING) {
            // Ignore REGISTERING state, handle only REGISTERED and NOT_REGISTERED
            log("onImsRadioTechChanged : result is not available");
            return false;
        }

        int phoneId = imsRegistrationRadioTechInfo.phoneId();
        int subId = SubscriptionManager.getSubscriptionId(phoneId);
        int tech = imsRegistrationRadioTechInfo.imsRegistrationTech();
        log("onImsRadioTechChanged phoneId : " + phoneId + " subId : " + subId + " old tech : "
                + mImsRegistrationRadioTechMap.get(phoneId, REGISTRATION_TECH_NONE)
                + " new tech : " + tech);

        if (mImsRegistrationRadioTechMap.get(phoneId, REGISTRATION_TECH_NONE) == tech) {
            // Registration tech not changed
            return false;
        }

        mImsRegistrationRadioTechMap.put(phoneId, tech);

        // Need to update the cached IMS registration tech but no need to do any of the
        // following. When the SIM removed, REGISTRATION_STATE_NOT_REGISTERED is notified.
        return subId != INVALID_SUBSCRIPTION_ID;
    }

    private synchronized void onMultiSimConfigChanged(int activeModemCount) {
        // No change.
        if (mActiveModemCount == activeModemCount) return;
        int oldActiveModemCount = mActiveModemCount;
        mActiveModemCount = activeModemCount;

        mPhoneSubscriptions = copyOf(mPhoneSubscriptions, mActiveModemCount);
        mPhoneStates = copyOf(mPhoneStates, mActiveModemCount);

        // Dual SIM -> Single SIM switch.
        for (int phoneId = oldActiveModemCount - 1; phoneId >= mActiveModemCount; phoneId--) {
            mCurrentDdsSwitchFailure.remove(phoneId);
        }

        // Single SIM -> Dual SIM switch.
        for (int phoneId = oldActiveModemCount; phoneId < mActiveModemCount; phoneId++) {
            mPhoneStates[phoneId] = new PhoneState();
            Phone phone = PhoneFactory.getPhone(phoneId);
            if (phone == null) continue;

            phone.registerForEmergencyCallToggle(this, EVENT_EMERGENCY_TOGGLE, null);
            // TODO (b/135566422): combine register for both GsmCdmaPhone and ImsPhone.
            phone.registerForPreciseCallStateChanged(this, EVENT_PRECISE_CALL_STATE_CHANGED, null);
            if (phone.getImsPhone() != null) {
                phone.getImsPhone().registerForPreciseCallStateChanged(
                        this, EVENT_PRECISE_CALL_STATE_CHANGED, null);
                if (mFlags.changeMethodOfObtainingImsRegistrationRadioTech()) {
                    // Initialize IMS registration tech for new phoneId
                    mImsRegistrationRadioTechMap.put(phoneId, REGISTRATION_TECH_NONE);
                    ((ImsPhone) phone.getImsPhone()).registerForImsRegistrationChanges(
                            this, EVENT_IMS_RADIO_TECH_CHANGED, null);

                    log("register handler to receive IMS registration : " + phoneId);
                }
            }

            mDataSettingsManagerCallbacks.computeIfAbsent(phone.getPhoneId(),
                    v -> new DataSettingsManagerCallback(this::post) {
                        @Override
                        public void onDataEnabledChanged(boolean enabled,
                                @TelephonyManager.DataEnabledChangedReason int reason,
                                @NonNull String callingPackage) {
                            PhoneSwitcher.this.onDataEnabledChanged();
                        }
                        @Override
                        public void onDataRoamingEnabledChanged(boolean enabled) {
                            PhoneSwitcher.this.mAutoDataSwitchController.evaluateAutoDataSwitch(
                                    AutoDataSwitchController
                                            .EVALUATION_REASON_DATA_SETTINGS_CHANGED);
                        }
                    });
            phone.getDataSettingsManager().registerCallback(
                    mDataSettingsManagerCallbacks.get(phone.getPhoneId()));

            Set<CommandException.Error> ddsFailure = new HashSet<>();
            mCurrentDdsSwitchFailure.add(ddsFailure);

            if (!mFlags.changeMethodOfObtainingImsRegistrationRadioTech()) {
                registerForImsRadioTechChange(mContext, phoneId);
            }
        }

        mAutoDataSwitchController.onMultiSimConfigChanged(activeModemCount);
    }

    /**
     * Called when
     * 1. user changed mobile data settings
     * 2. OR user changed auto data switch feature
     */
    private void onDataEnabledChanged() {
        if (isAnyVoiceCallActiveOnDevice()) {
            // user changed data related settings during call, switch or turn off immediately
            evaluateIfImmediateDataSwitchIsNeeded(
                    "user changed data settings during call",
                    DataSwitch.Reason.DATA_SWITCH_REASON_IN_CALL);
        } else {
            mAutoDataSwitchController.evaluateAutoDataSwitch(AutoDataSwitchController
                    .EVALUATION_REASON_DATA_SETTINGS_CHANGED);
        }
    }

    private boolean isInEmergencyCallbackMode() {
        for (Phone p : PhoneFactory.getPhones()) {
            if (p == null) continue;
            if (p.isInEcm()) return true;
            Phone imsPhone = p.getImsPhone();
            if (imsPhone != null && imsPhone.isInEcm()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called when receiving a network request.
     *
     * @param networkRequest The network request.
     */
    // TODO: Transform to TelephonyNetworkRequest after removing TelephonyNetworkFactory
    public void onRequestNetwork(@NonNull TelephonyNetworkRequest networkRequest) {
        if (!mNetworkRequestList.contains(networkRequest)) {
            mNetworkRequestList.add(networkRequest);
            onEvaluate(REQUESTS_CHANGED, "netRequest");
        }
    }

    /**
     * Called when releasing a network request.
     *
     * @param networkRequest The network request to release.
     */
    public void onReleaseNetwork(@NonNull TelephonyNetworkRequest networkRequest) {
        if (mNetworkRequestList.remove(networkRequest)) {
            onEvaluate(REQUESTS_CHANGED, "netReleased");
        }
    }

    private void registerDefaultNetworkChangeCallback(int expectedSubId, int reason) {
        mDefaultNetworkCallback.mExpectedSubId = expectedSubId;
        mDefaultNetworkCallback.mSwitchReason = reason;
    }

    /**
     * Cancel any auto switch attempts when the current environment is not suitable for auto switch.
     */
    private void cancelPendingAutoDataSwitchValidation() {
        if (mValidator.isValidating()) {
            mValidator.stopValidation();

            removeMessages(EVENT_NETWORK_VALIDATION_DONE);
            removeMessages(EVENT_NETWORK_AVAILABLE);
            mPendingSwitchSubId = INVALID_SUBSCRIPTION_ID;
            mPendingSwitchNeedValidation = false;
        }
    }

    private TelephonyManager getTm() {
        return (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    protected static final boolean REQUESTS_CHANGED   = true;
    protected static final boolean REQUESTS_UNCHANGED = false;
    /**
     * Re-evaluate things. Do nothing if nothing's changed.
     * <p>
     * Otherwise, go through the requests in priority order adding their phone until we've added up
     * to the max allowed.  Then go through shutting down phones that aren't in the active phone
     * list. Finally, activate all phones in the active phone list.
     *
     * @return {@code True} if the default data subscription need to be changed.
     */
    protected boolean onEvaluate(boolean requestsChanged, String reason) {
        StringBuilder sb = new StringBuilder(reason);

        // If we use HAL_COMMAND_PREFERRED_DATA,
        boolean diffDetected = mHalCommandToUse != HAL_COMMAND_PREFERRED_DATA && requestsChanged;

        // Check if user setting of default non-opportunistic data sub is changed.
        int primaryDataSubId = mSubscriptionManagerService.getDefaultDataSubId();
        if (primaryDataSubId != mPrimaryDataSubId) {
            sb.append(" mPrimaryDataSubId ").append(mPrimaryDataSubId).append("->")
                .append(primaryDataSubId);
            mPrimaryDataSubId = primaryDataSubId;
            mLastSwitchPreferredDataReason = DataSwitch.Reason.DATA_SWITCH_REASON_MANUAL;
        }

        // Check to see if there is any active subscription on any phone
        boolean hasAnyActiveSubscription = false;

        // Check if phoneId to subId mapping is changed.
        for (int i = 0; i < mActiveModemCount; i++) {
            int sub = SubscriptionManager.getSubscriptionId(i);

            if (SubscriptionManager.isValidSubscriptionId(sub)) hasAnyActiveSubscription = true;

            if (sub != mPhoneSubscriptions[i]) {
                sb.append(" phone[").append(i).append("] ").append(mPhoneSubscriptions[i]);
                sb.append("->").append(sub);
                if (mAutoSelectedDataSubId == mPhoneSubscriptions[i]) {
                    mAutoSelectedDataSubId = DEFAULT_SUBSCRIPTION_ID;
                }
                mPhoneSubscriptions[i] = sub;

                if (!mFlags.changeMethodOfObtainingImsRegistrationRadioTech()) {
                    // Listen to IMS radio tech change for new sub
                    if (SubscriptionManager.isValidSubscriptionId(sub)) {
                        registerForImsRadioTechChange(mContext, i);
                    }
                }

                diffDetected = true;
                mAutoDataSwitchController.notifySubscriptionsMappingChanged();
            }
        }

        if (!hasAnyActiveSubscription) {
            transitionToEmergencyPhone();
        } else {
            if (VDBG) log("Found an active subscription");
        }

        // Check if phoneId for preferred data is changed.
        int oldPreferredDataPhoneId = mPreferredDataPhoneId;

        // Check if subId for preferred data is changed.
        int oldPreferredDataSubId = mPreferredDataSubId.get();

        // When there are no subscriptions, the preferred data phone ID is invalid, but we want
        // to keep a valid phoneId for Emergency, so skip logic that updates for preferred data
        // phone ID. Ideally there should be a single set of checks that evaluate the correct
        // phoneId on a service-by-service basis (EIMS being one), but for now... just bypass
        // this logic in the no-SIM case.
        if (hasAnyActiveSubscription) updatePreferredDataPhoneId();

        if (oldPreferredDataPhoneId != mPreferredDataPhoneId) {
            sb.append(" preferred data phoneId ").append(oldPreferredDataPhoneId)
                    .append("->").append(mPreferredDataPhoneId);
            diffDetected = true;
        } else if (oldPreferredDataSubId != mPreferredDataSubId.get()) {
            logl("SIM refresh, notify dds change");
            // Inform connectivity about the active data phone
            notifyPreferredDataSubIdChanged();
        }

        // Always force DDS when radio on. This is to handle the corner cases that modem and android
        // DDS are out of sync after APM, AP should force DDS when radio on. long term solution
        // should be having API to query preferred data modem to detect the out-of-sync scenarios.
        if (diffDetected || EVALUATION_REASON_RADIO_ON.equals(reason)) {
            logl("evaluating due to " + sb);
            if (mHalCommandToUse == HAL_COMMAND_PREFERRED_DATA) {
                // With HAL_COMMAND_PREFERRED_DATA, all phones are assumed to allow PS attach.
                // So marking all phone as active, and the phone with mPreferredDataPhoneId
                // will send radioConfig command.
                for (int phoneId = 0; phoneId < mActiveModemCount; phoneId++) {
                    mPhoneStates[phoneId].active = true;
                }
                sendRilCommands(mPreferredDataPhoneId);
            } else {
                List<Integer> newActivePhones = new ArrayList<>();

                // If all phones can have PS attached, activate all.
                // Otherwise, choose to activate phones according to requests. And
                // if list is not full, add mPreferredDataPhoneId.
                if (mMaxDataAttachModemCount == mActiveModemCount) {
                    for (int i = 0; i < mMaxDataAttachModemCount; i++) {
                        newActivePhones.add(i);
                    }
                } else {
                    // First try to activate phone in voice call.
                    if (mPhoneIdInVoiceCall != SubscriptionManager.INVALID_PHONE_INDEX) {
                        newActivePhones.add(mPhoneIdInVoiceCall);
                    }

                    if (newActivePhones.size() < mMaxDataAttachModemCount) {
                        for (TelephonyNetworkRequest networkRequest : mNetworkRequestList) {
                            int phoneIdForRequest = phoneIdForRequest(networkRequest);
                            if (phoneIdForRequest == INVALID_PHONE_INDEX) continue;
                            if (newActivePhones.contains(phoneIdForRequest)) continue;
                            newActivePhones.add(phoneIdForRequest);
                            if (newActivePhones.size() >= mMaxDataAttachModemCount) break;
                        }
                    }

                    if (newActivePhones.size() < mMaxDataAttachModemCount
                            && !newActivePhones.contains(mPreferredDataPhoneId)
                            && SubscriptionManager.isUsableSubIdValue(mPreferredDataPhoneId)) {
                        newActivePhones.add(mPreferredDataPhoneId);
                    }
                }

                if (VDBG) {
                    log("mPrimaryDataSubId = " + mPrimaryDataSubId);
                    log("mAutoSelectedDataSubId = " + mAutoSelectedDataSubId);
                    for (int i = 0; i < mActiveModemCount; i++) {
                        log(" phone[" + i + "] using sub[" + mPhoneSubscriptions[i] + "]");
                    }
                    log(" newActivePhones:");
                    for (Integer i : newActivePhones) log("  " + i);
                }

                for (int phoneId = 0; phoneId < mActiveModemCount; phoneId++) {
                    if (!newActivePhones.contains(phoneId)) {
                        deactivate(phoneId);
                    }
                }

                // only activate phones up to the limit
                for (int phoneId : newActivePhones) {
                    activate(phoneId);
                }
            }
        }
        return diffDetected;
    }

    protected static class PhoneState {
        public volatile boolean active = false;
        public long lastRequested = 0;
    }

    protected void activate(int phoneId) {
        switchPhone(phoneId, true);
    }

    protected void deactivate(int phoneId) {
        switchPhone(phoneId, false);
    }

    private void switchPhone(int phoneId, boolean active) {
        PhoneState state = mPhoneStates[phoneId];
        if (state.active == active) return;
        state.active = active;
        logl((active ? "activate " : "deactivate ") + phoneId);
        state.lastRequested = System.currentTimeMillis();
        sendRilCommands(phoneId);
    }

    /**
     * Used when the modem may have been rebooted and we
     * want to resend setDataAllowed or setPreferredDataSubscriptionId
     */
    public void onRadioCapChanged(int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) return;
        Message msg = obtainMessage(EVENT_RADIO_CAPABILITY_CHANGED);
        msg.arg1 = phoneId;
        msg.sendToTarget();
    }

    /**
     * Switch the Default data for the context of an outgoing emergency call.
     * <p>
     * In some cases, we need to try to switch the Default Data subscription before placing the
     * emergency call on DSDS devices. This includes the following situation:
     * - The modem does not support processing GNSS SUPL requests on the non-default data
     * subscription. For some carriers that do not provide a control plane fallback mechanism, the
     * SUPL request will be dropped and we will not be able to get the user's location for the
     * emergency call. In this case, we need to swap default data temporarily.
     * @param phoneId The phone to use to evaluate whether or not the default data should be moved
     *                to this subscription.
     * @param overrideTimeSec The amount of time to override the default data setting for after the
     *                       emergency call ends.
     * @param dataSwitchResult A {@link CompletableFuture} to be called with a {@link Boolean}
     *                         result when the default data switch has either completed (true) or
     *                         failed (false).
     */
    public void overrideDefaultDataForEmergency(int phoneId, int overrideTimeSec,
            CompletableFuture<Boolean> dataSwitchResult) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) return;
        Message msg = obtainMessage(EVENT_OVERRIDE_DDS_FOR_EMERGENCY);
        EmergencyOverrideRequest request  = new EmergencyOverrideRequest();
        request.mPhoneId = phoneId;
        request.mGnssOverrideTimeMs = overrideTimeSec * 1000;
        request.mOverrideCompleteFuture = dataSwitchResult;
        msg.obj = request;
        msg.sendToTarget();
    }

    protected void sendRilCommands(int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            logl("sendRilCommands: skip dds switch due to invalid phoneId=" + phoneId);
            return;
        }

        Message message = Message.obtain(this, EVENT_MODEM_COMMAND_DONE, phoneId);
        if (mHalCommandToUse == HAL_COMMAND_ALLOW_DATA || mHalCommandToUse == HAL_COMMAND_UNKNOWN) {
            // Skip ALLOW_DATA for single SIM device
            if (mActiveModemCount > 1) {
                PhoneFactory.getPhone(phoneId).mCi.setDataAllowed(isPhoneActive(phoneId), message);
            }
        } else if (phoneId == mPreferredDataPhoneId) {
            // Only setPreferredDataModem if the phoneId equals to current mPreferredDataPhoneId
            logl("sendRilCommands: setPreferredDataModem - phoneId: " + phoneId);
            mRadioConfig.setPreferredDataModem(mPreferredDataPhoneId, message);
        }
    }

    private int phoneIdForRequest(TelephonyNetworkRequest networkRequest) {
        NetworkRequest netRequest = networkRequest.getNativeNetworkRequest();
        int subId = getSubIdFromNetworkSpecifier(netRequest.getNetworkSpecifier());

        if (subId == DEFAULT_SUBSCRIPTION_ID) return mPreferredDataPhoneId;
        if (subId == INVALID_SUBSCRIPTION_ID) return INVALID_PHONE_INDEX;

        int preferredDataSubId = (mPreferredDataPhoneId >= 0
                && mPreferredDataPhoneId < mActiveModemCount)
                ? mPhoneSubscriptions[mPreferredDataPhoneId] : INVALID_SUBSCRIPTION_ID;

        // Currently we assume multi-SIM devices will only support one Internet PDN connection. So
        // if Internet PDN is established on the non-preferred phone, it will interrupt
        // Internet connection on the preferred phone. So we only accept Internet request with
        // preferred data subscription or no specified subscription.
        // One exception is, if it's restricted request (doesn't have NET_CAPABILITY_NOT_RESTRICTED)
        // it will be accepted, which is used temporary data usage from system.
        if (netRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && netRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                && subId != preferredDataSubId && subId != mValidator.getSubIdInValidation()) {
            // Returning INVALID_PHONE_INDEX will result in netRequest not being handled.
            return INVALID_PHONE_INDEX;
        }

        // Try to find matching phone ID. If it doesn't exist, we'll end up returning INVALID.
        int phoneId = INVALID_PHONE_INDEX;
        for (int i = 0; i < mActiveModemCount; i++) {
            if (mPhoneSubscriptions[i] == subId) {
                phoneId = i;
                break;
            }
        }
        return phoneId;
    }

    protected int getSubIdFromNetworkSpecifier(NetworkSpecifier specifier) {
        if (specifier == null) {
            return DEFAULT_SUBSCRIPTION_ID;
        }
        if (specifier instanceof TelephonyNetworkSpecifier) {
            return ((TelephonyNetworkSpecifier) specifier).getSubscriptionId();
        }
        return INVALID_SUBSCRIPTION_ID;
    }

    private boolean isActiveSubId(int subId) {
        SubscriptionInfoInternal subInfo = mSubscriptionManagerService
                .getSubscriptionInfoInternal(subId);
        return subInfo != null && subInfo.isActive();
    }

    // This updates mPreferredDataPhoneId which decides which phone should handle default network
    // requests.
    protected void updatePreferredDataPhoneId() {
        if (mEmergencyOverride != null && findPhoneById(mEmergencyOverride.mPhoneId) != null) {
            // Override DDS for emergency even if user data is not enabled, since it is an
            // emergency.
            // TODO: Provide a notification to the user that metered data is currently being
            // used during this period.
            logl("updatePreferredDataPhoneId: preferred data overridden for emergency."
                    + " phoneId = " + mEmergencyOverride.mPhoneId);
            mPreferredDataPhoneId = mEmergencyOverride.mPhoneId;
            mLastSwitchPreferredDataReason = DataSwitch.Reason.DATA_SWITCH_REASON_UNKNOWN;
        } else {
            if (isAnyVoiceCallActiveOnDevice()) {
                int imsRegTech = mImsRegTechProvider.get(mContext, mPhoneIdInVoiceCall);
                if (imsRegTech != REGISTRATION_TECH_IWLAN) {
                    if (imsRegTech != REGISTRATION_TECH_CROSS_SIM) {
                        mPreferredDataPhoneId = shouldSwitchDataDueToInCall()
                                ? mPhoneIdInVoiceCall : getFallbackDataPhoneIdForInternetRequests();
                    } else {
                        logl("IMS call on cross-SIM, skip switching data to phone "
                                + mPhoneIdInVoiceCall);
                    }
                } else {
                    mPreferredDataPhoneId = getFallbackDataPhoneIdForInternetRequests();
                }
            } else {
                mPreferredDataPhoneId = getFallbackDataPhoneIdForInternetRequests();
            }
        }

        mPreferredDataSubId.set(SubscriptionManager.getSubscriptionId(mPreferredDataPhoneId));
    }

    /**
     * @return the default data phone Id (or auto selected phone Id in auto data switch/CBRS case)
     */
    private int getFallbackDataPhoneIdForInternetRequests() {
        int fallbackSubId = isActiveSubId(mAutoSelectedDataSubId)
                ? mAutoSelectedDataSubId : mPrimaryDataSubId;

        if (SubscriptionManager.isUsableSubIdValue(fallbackSubId)) {
            for (int phoneId = 0; phoneId < mActiveModemCount; phoneId++) {
                if (mPhoneSubscriptions[phoneId] == fallbackSubId) {
                    return phoneId;
                }
            }
        }
        return SubscriptionManager.INVALID_PHONE_INDEX;
    }

    /**
     * If a phone is in call and user enabled its mobile data and auto data switch feature, we
     * should switch internet connection to it because the other modem will lose data connection
     * anyway.
     * @return {@code true} if should switch data to the phone in voice call
     */
    private boolean shouldSwitchDataDueToInCall() {
        Phone voicePhone = findPhoneById(mPhoneIdInVoiceCall);
        Phone defaultDataPhone = getPhoneBySubId(mPrimaryDataSubId);
        return defaultDataPhone != null // check user enabled data
                && defaultDataPhone.isUserDataEnabled()
                && voicePhone != null // check user enabled voice during call feature
                && voicePhone.getDataSettingsManager().isDataEnabled();
    }

    protected void transitionToEmergencyPhone() {
        if (mActiveModemCount <= 0) {
            logl("No phones: unable to reset preferred phone for emergency");
            return;
        }

        if (mPreferredDataPhoneId != DEFAULT_EMERGENCY_PHONE_ID) {
            logl("No active subscriptions: resetting preferred phone to 0 for emergency");
            mPreferredDataPhoneId = DEFAULT_EMERGENCY_PHONE_ID;
        }

        if (mPreferredDataSubId.get() != INVALID_SUBSCRIPTION_ID) {
            mPreferredDataSubId.set(INVALID_SUBSCRIPTION_ID);
            notifyPreferredDataSubIdChanged();
        }
    }

    private Phone getPhoneBySubId(int subId) {
        return findPhoneById(mSubscriptionManagerService.getPhoneId(subId));
    }

    private Phone findPhoneById(final int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return null;
        }
        return PhoneFactory.getPhone(phoneId);
    }

    public synchronized boolean shouldApplyNetworkRequest(
            TelephonyNetworkRequest networkRequest, int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) return false;

        int subId = SubscriptionManager.getSubscriptionId(phoneId);

        // In any case, if phone state is inactive, don't apply the network request.
        if (!isPhoneActive(phoneId) || (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                && !isEmergencyNetworkRequest(networkRequest))) {
            return false;
        }

        NetworkRequest netRequest = networkRequest.getNativeNetworkRequest();
        subId = getSubIdFromNetworkSpecifier(netRequest.getNetworkSpecifier());

        //if this phone is an emergency networkRequest
        //and subId is not specified that is invalid or default
        if (isAnyVoiceCallActiveOnDevice() && isEmergencyNetworkRequest(networkRequest)
                && (subId == DEFAULT_SUBSCRIPTION_ID || subId == INVALID_SUBSCRIPTION_ID)) {
            return phoneId == mPhoneIdInVoiceCall;
        }

        int phoneIdToHandle = phoneIdForRequest(networkRequest);
        return phoneId == phoneIdToHandle;
    }

    boolean isEmergencyNetworkRequest(TelephonyNetworkRequest networkRequest) {
        return networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_EIMS);
    }

    @VisibleForTesting
    protected boolean isPhoneActive(int phoneId) {
        if (phoneId >= mActiveModemCount)
            return false;
        return mPhoneStates[phoneId].active;
    }

    /**
     * Set opportunistic data subscription. It's an indication to switch Internet data to this
     * subscription. It has to be an active subscription, and PhoneSwitcher will try to validate
     * it first if needed. If subId is DEFAULT_SUBSCRIPTION_ID, it means we are un-setting
     * opportunistic data sub and switch data back to primary sub.
     *
     * @param subId the opportunistic data subscription to switch to. pass DEFAULT_SUBSCRIPTION_ID
     *              if un-setting it.
     * @param needValidation whether Telephony will wait until the network is validated by
     *              connectivity service before switching data to it. More details see
     *              {@link NetworkCapabilities#NET_CAPABILITY_VALIDATED}.
     * @param callback Callback will be triggered once it succeeds or failed.
     *                 Pass null if don't care about the result.
     */
    private void setOpportunisticDataSubscription(int subId, boolean needValidation,
            ISetOpportunisticDataCallback callback) {
        validate(subId, needValidation,
                DataSwitch.Reason.DATA_SWITCH_REASON_CBRS, callback);
    }

    /**
     * Try setup a new internet connection on the subId that's pending validation. If the validation
     * succeeds, this subId will be evaluated for being the preferred data subId; If fails, nothing
     * happens.
     * Callback will be updated with the validation result.
     *
     * @param subId Sub Id that's pending switch, awaiting validation.
     * @param needValidation {@code false} if switch to the subId even if validation fails.
     * @param switchReason The switch reason for this validation
     * @param callback Optional - specific for external opportunistic sub validation request.
     */
    private void validate(int subId, boolean needValidation, int switchReason,
            @Nullable ISetOpportunisticDataCallback callback) {
        int subIdToValidate = (subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID)
                ? mPrimaryDataSubId : subId;
        logl("Validate subId " + subId + " due to " + switchReasonToString(switchReason)
                + " needValidation=" + needValidation + " subIdToValidate=" + subIdToValidate
                + " mAutoSelectedDataSubId=" + mAutoSelectedDataSubId
                + " mPreferredDataSubId=" + mPreferredDataSubId.get());
        if (!isActiveSubId(subIdToValidate)) {
            logl("Can't switch data to inactive subId " + subIdToValidate);
            if (subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
                // the default data sub is not selected yet, store the intent of switching to
                // default subId once it becomes available.
                mAutoSelectedDataSubId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
                sendSetOpptCallbackHelper(callback, SET_OPPORTUNISTIC_SUB_SUCCESS);
            } else {
                sendSetOpptCallbackHelper(callback, SET_OPPORTUNISTIC_SUB_INACTIVE_SUBSCRIPTION);
            }
            return;
        }

        if (mValidator.isValidating()) {
            mValidator.stopValidation();
            sendSetOpptCallbackHelper(mSetOpptSubCallback, SET_OPPORTUNISTIC_SUB_VALIDATION_FAILED);
            mSetOpptSubCallback = null;
        }

        // Remove EVENT_NETWORK_VALIDATION_DONE. Don't handle validation result of previous subId
        // if queued.
        removeMessages(EVENT_NETWORK_VALIDATION_DONE);
        removeMessages(EVENT_NETWORK_AVAILABLE);

        mPendingSwitchSubId = INVALID_SUBSCRIPTION_ID;

        if (subIdToValidate == mPreferredDataSubId.get()) {
            if (subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
                mAutoSelectedDataSubId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
            }
            sendSetOpptCallbackHelper(callback, SET_OPPORTUNISTIC_SUB_SUCCESS);
            return;
        }

        mLastSwitchPreferredDataReason = switchReason;
        logDataSwitchEvent(subIdToValidate,
                TelephonyEvent.EventState.EVENT_STATE_START,
                switchReason);
        registerDefaultNetworkChangeCallback(subIdToValidate,
                switchReason);

        // If validation feature is not supported, set it directly. Otherwise,
        // start validation on the subscription first.
        if (!mValidator.isValidationFeatureSupported()) {
            setAutoSelectedDataSubIdInternal(subId);
            sendSetOpptCallbackHelper(callback, SET_OPPORTUNISTIC_SUB_SUCCESS);
            return;
        }

        // Even if needValidation is false, we still send request to validator. The reason is we
        // want to delay data switch until network is available on the target sub, to have a
        // smoothest transition possible.
        // In this case, even if data connection eventually failed in 2 seconds, we still
        // confirm the switch, to maximally respect the request.
        mPendingSwitchSubId = subIdToValidate;
        mPendingSwitchNeedValidation = needValidation;
        mSetOpptSubCallback = callback;
        long validationTimeout = getValidationTimeout(subIdToValidate, needValidation);
        mValidator.validate(subIdToValidate, validationTimeout,
                mFlags.keepPingRequest() && mPendingSwitchNeedValidation, mValidationCallback);
    }

    private long getValidationTimeout(int subId, boolean needValidation) {
        if (!needValidation) return DEFAULT_VALIDATION_EXPIRATION_TIME;

        long validationTimeout = DEFAULT_VALIDATION_EXPIRATION_TIME;
        CarrierConfigManager configManager = (CarrierConfigManager)
                mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            PersistableBundle b = configManager.getConfigForSubId(subId);
            if (b != null) {
                validationTimeout = b.getLong(KEY_DATA_SWITCH_VALIDATION_TIMEOUT_LONG);
            }
        }
        return validationTimeout;
    }

    private void sendSetOpptCallbackHelper(ISetOpportunisticDataCallback callback, int result) {
        if (callback == null) return;
        try {
            callback.onComplete(result);
        } catch (RemoteException exception) {
            logl("RemoteException " + exception);
        }
    }

    /**
     * Evaluate whether the specified sub Id can be set to be the preferred data sub Id.
     *
     * @param subId The subId that we tried to validate: could possibly be unvalidated if validation
     * feature is not supported.
     */
    private void setAutoSelectedDataSubIdInternal(int subId) {
        if (mAutoSelectedDataSubId != subId) {
            mAutoSelectedDataSubId = subId;
            onEvaluate(REQUESTS_UNCHANGED, switchReasonToString(mLastSwitchPreferredDataReason));
        }
    }

    private void confirmSwitch(int subId, boolean confirm) {
        logl("confirmSwitch: subId " + subId + (confirm ? " confirmed." : " cancelled."));
        int resultForCallBack;
        if (!isActiveSubId(subId)) {
            logl("confirmSwitch: subId " + subId + " is no longer active");
            resultForCallBack = SET_OPPORTUNISTIC_SUB_INACTIVE_SUBSCRIPTION;
        } else if (!confirm) {
            resultForCallBack = SET_OPPORTUNISTIC_SUB_VALIDATION_FAILED;

            // retry for auto data switch validation failure
            if (mLastSwitchPreferredDataReason == DataSwitch.Reason.DATA_SWITCH_REASON_AUTO) {
                mAutoDataSwitchController.evaluateRetryOnValidationFailed();
            }
        } else {
            if (subId == mPrimaryDataSubId) {
                setAutoSelectedDataSubIdInternal(DEFAULT_SUBSCRIPTION_ID);
            } else {
                setAutoSelectedDataSubIdInternal(subId);
            }
            resultForCallBack = SET_OPPORTUNISTIC_SUB_SUCCESS;
            mAutoDataSwitchController.resetFailedCount();
        }

        // Trigger callback if needed
        sendSetOpptCallbackHelper(mSetOpptSubCallback, resultForCallBack);
        mSetOpptSubCallback = null;
        mPendingSwitchSubId = INVALID_SUBSCRIPTION_ID;
    }

    private void onNetworkAvailable(int subId, Network network) {
        log("onNetworkAvailable: on subId " + subId);
        // Do nothing unless pending switch matches target subId and it doesn't require
        // validation pass.
        if (mPendingSwitchSubId == INVALID_SUBSCRIPTION_ID || mPendingSwitchSubId != subId
                || mPendingSwitchNeedValidation) {
            return;
        }
        confirmSwitch(subId, true);
    }

    private void onValidationDone(int subId, boolean passed) {
        logl("onValidationDone: " + (passed ? "passed" : "failed") + " on subId " + subId);
        if (mPendingSwitchSubId == INVALID_SUBSCRIPTION_ID || mPendingSwitchSubId != subId) return;

        // If validation failed and mPendingSwitch.mNeedValidation is false, we still confirm
        // the switch.
        confirmSwitch(subId, passed || !mPendingSwitchNeedValidation);
    }

    /**
     * Notify PhoneSwitcher to try to switch data to an opportunistic subscription.
     * <p>
     * Set opportunistic data subscription. It's an indication to switch Internet data to this
     * subscription. It has to be an active subscription, and PhoneSwitcher will try to validate
     * it first if needed. If subId is DEFAULT_SUBSCRIPTION_ID, it means we are un-setting
     * opportunistic data sub and switch data back to primary sub.
     *
     * @param subId the opportunistic data subscription to switch to. pass DEFAULT_SUBSCRIPTION_ID
     *              if un-setting it.
     * @param needValidation whether Telephony will wait until the network is validated by
     *              connectivity service before switching data to it. More details see
     *              {@link NetworkCapabilities#NET_CAPABILITY_VALIDATED}.
     * @param callback Callback will be triggered once it succeeds or failed.
     *                 Pass null if don't care about the result.
     */
    public void trySetOpportunisticDataSubscription(int subId, boolean needValidation,
            ISetOpportunisticDataCallback callback) {
        logl("Try set opportunistic data subscription to subId " + subId
                + (needValidation ? " with " : " without ") + "validation");
        PhoneSwitcher.this.obtainMessage(EVENT_OPPT_DATA_SUB_CHANGED,
                subId, needValidation ? 1 : 0, callback).sendToTarget();
    }

    protected boolean isPhoneInVoiceCall(Phone phone) {
        if (phone == null) {
            return false;
        }
        Call bgCall = phone.getBackgroundCall();
        Call fgCall = phone.getForegroundCall();
        if (bgCall == null || fgCall == null) {
            return false;
        }
        // A phone in voice call might trigger data being switched to it.
        // Exclude dialing to give modem time to process an EMC first before dealing with DDS switch
        // Include alerting because modem RLF leads to delay in switch, so carrier required to
        // switch in alerting phase.
        // TODO: check ringing call for vDADA
        return (!bgCall.isIdle() && bgCall.getState() != Call.State.DIALING)
                || (!fgCall.isIdle() && fgCall.getState() != Call.State.DIALING);
    }

    private void updateHalCommandToUse() {
        mHalCommandToUse = mRadioConfig.isSetPreferredDataCommandSupported()
                ? HAL_COMMAND_PREFERRED_DATA : HAL_COMMAND_ALLOW_DATA;
    }

    public int getPreferredDataPhoneId() {
        return mPreferredDataPhoneId;
    }

    /**
     * Log debug messages and also log into the local log.
     * @param l debug messages
     */
    protected void logl(String l) {
        log(l);
        mLocalLog.log(l);
    }

    /**
     * Log debug messages.
     * @param s debug messages
     */
    private void log(@NonNull String s) {
        Rlog.d(LOG_TAG, s);
    }

    /**
     * Log debug error messages.
     * @param s debug messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(LOG_TAG, s);
    }


    /**
     * Convert data switch reason into string.
     *
     * @param reason The switch reason.
     * @return The switch reason in string format.
     */
    @NonNull
    private static String switchReasonToString(int reason) {
        return switch (reason) {
            case DataSwitch.Reason.DATA_SWITCH_REASON_UNKNOWN -> "UNKNOWN";
            case DataSwitch.Reason.DATA_SWITCH_REASON_MANUAL -> "MANUAL";
            case DataSwitch.Reason.DATA_SWITCH_REASON_IN_CALL -> "IN_CALL";
            case DataSwitch.Reason.DATA_SWITCH_REASON_CBRS -> "CBRS";
            case DataSwitch.Reason.DATA_SWITCH_REASON_AUTO -> "AUTO";
            default -> "UNKNOWN(" + reason + ")";
        };
    }

    /**
     * Concert switching state to string
     *
     * @param state The switching state.
     * @return The switching state in string format.
     */
    @NonNull
    private static String switchStateToString(int state) {
        return switch (state) {
            case TelephonyEvent.EventState.EVENT_STATE_UNKNOWN -> "UNKNOWN";
            case TelephonyEvent.EventState.EVENT_STATE_START -> "START";
            case TelephonyEvent.EventState.EVENT_STATE_END -> "END";
            default -> "UNKNOWN(" + state + ")";
        };
    }

    /**
     * Log data switch event
     *
     * @param subId Subscription index.
     * @param state The switching state.
     * @param reason The switching reason.
     */
    private void logDataSwitchEvent(int subId, int state, int reason) {
        logl("Data switch state=" + switchStateToString(state) + " due to reason="
                + switchReasonToString(reason) + " on subId " + subId);
        DataSwitch dataSwitch = new DataSwitch();
        dataSwitch.state = state;
        dataSwitch.reason = reason;
        TelephonyMetrics.getInstance().writeDataSwitch(subId, dataSwitch);
    }

    /**
     * See {@link PhoneStateListener#LISTEN_ACTIVE_DATA_SUBSCRIPTION_ID_CHANGE}.
     */
    protected void notifyPreferredDataSubIdChanged() {
        TelephonyRegistryManager telephonyRegistryManager = (TelephonyRegistryManager) mContext
                .getSystemService(Context.TELEPHONY_REGISTRY_SERVICE);
        logl("notifyPreferredDataSubIdChanged to " + mPreferredDataSubId.get());
        telephonyRegistryManager.notifyActiveDataSubIdChanged(mPreferredDataSubId.get());
    }

    /**
     * @return The active data subscription id
     */
    public int getActiveDataSubId() {
        return mPreferredDataSubId.get();
    }

    /**
     * @return The auto selected data subscription id.
     */
    public int getAutoSelectedDataSubId() {
        return mAutoSelectedDataSubId;
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        pw.println("PhoneSwitcher:");
        pw.increaseIndent();
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < mActiveModemCount; i++) {
            PhoneState ps = mPhoneStates[i];
            c.setTimeInMillis(ps.lastRequested);
            pw.println("PhoneId(" + i + ") active=" + ps.active
                    + ", lastRequest="
                    + (ps.lastRequested == 0 ? "never" :
                     String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c)));
        }
        pw.println("mPreferredDataPhoneId=" + mPreferredDataPhoneId);
        pw.println("mPreferredDataSubId=" + mPreferredDataSubId.get());
        pw.println("DefaultDataSubId=" + mSubscriptionManagerService.getDefaultDataSubId());
        pw.println("DefaultDataPhoneId=" + mSubscriptionManagerService.getPhoneId(
                mSubscriptionManagerService.getDefaultDataSubId()));
        pw.println("mPrimaryDataSubId=" + mPrimaryDataSubId);
        pw.println("mAutoSelectedDataSubId=" + mAutoSelectedDataSubId);
        pw.println("mIsRegisteredForImsRadioTechChange=" + mIsRegisteredForImsRadioTechChange);
        pw.println("mPendingSwitchNeedValidation=" + mPendingSwitchNeedValidation);
        pw.println("mMaxDataAttachModemCount=" + mMaxDataAttachModemCount);
        pw.println("mActiveModemCount=" + mActiveModemCount);
        pw.println("mPhoneIdInVoiceCall=" + mPhoneIdInVoiceCall);
        pw.println("mCurrentDdsSwitchFailure=" + mCurrentDdsSwitchFailure);
        pw.println("mLastSwitchPreferredDataReason="
                + switchReasonToString(mLastSwitchPreferredDataReason));
        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        mAutoDataSwitchController.dump(fd, pw, args);
        pw.decreaseIndent();
    }

    private boolean isAnyVoiceCallActiveOnDevice() {
        boolean ret = mPhoneIdInVoiceCall != SubscriptionManager.INVALID_PHONE_INDEX;
        if (VDBG) log("isAnyVoiceCallActiveOnDevice: " + ret);
        return ret;
    }

    private void onDdsSwitchResponse(AsyncResult ar) {
        boolean commandSuccess = ar != null && ar.exception == null;
        int phoneId = (int) ar.userObj;
        if (mEmergencyOverride != null) {
            logl("Emergency override result sent = " + commandSuccess);
            mEmergencyOverride.sendOverrideCompleteCallbackResultAndClear(commandSuccess);
            // Do not retry , as we do not allow changes in onEvaluate during an emergency
            // call. When the call ends, we will start the countdown to remove the override.
        } else if (!commandSuccess) {
            logl("onDdsSwitchResponse: DDS switch failed. with exception " + ar.exception);
            if (ar.exception instanceof CommandException) {
                CommandException.Error error = ((CommandException)
                        (ar.exception)).getCommandError();
                mCurrentDdsSwitchFailure.get(phoneId).add(error);
                if (error == CommandException.Error.OP_NOT_ALLOWED_DURING_VOICE_CALL) {
                    logl("onDdsSwitchResponse: Wait for call end indication");
                    return;
                } else if (error == CommandException.Error.INVALID_SIM_STATE) {
                    /* If there is a attach failure due to sim not ready then
                    hold the retry until sim gets ready */
                    logl("onDdsSwitchResponse: Wait for SIM to get READY");
                    return;
                }
            }
            logl("onDdsSwitchResponse: Scheduling DDS switch retry");
            sendMessageDelayed(Message.obtain(this, EVENT_MODEM_COMMAND_RETRY,
                        phoneId), MODEM_COMMAND_RETRY_PERIOD_MS);
            return;
        }
        if (commandSuccess) {
            logl("onDdsSwitchResponse: DDS switch success on phoneId = " + phoneId);
            mAutoDataSwitchController.displayAutoDataSwitchNotification(phoneId,
                    mLastSwitchPreferredDataReason == DataSwitch.Reason.DATA_SWITCH_REASON_AUTO);
        }
        mCurrentDdsSwitchFailure.get(phoneId).clear();
        // Notify all registrants
        mActivePhoneRegistrants.notifyRegistrants();
        notifyPreferredDataSubIdChanged();
        mPhoneSwitcherCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onPreferredDataPhoneIdChanged(phoneId)));
    }

    private boolean isPhoneIdValidForRetry(int phoneId) {
        int ddsPhoneId = mSubscriptionManagerService.getPhoneId(
                mSubscriptionManagerService.getDefaultDataSubId());
        if (ddsPhoneId != INVALID_PHONE_INDEX && ddsPhoneId == phoneId) {
            return true;
        } else {
            if (mNetworkRequestList.isEmpty()) return false;
            for (TelephonyNetworkRequest networkRequest : mNetworkRequestList) {
                if (phoneIdForRequest(networkRequest) == phoneId) {
                    return true;
                }
            }
        }
        return false;
    }
}

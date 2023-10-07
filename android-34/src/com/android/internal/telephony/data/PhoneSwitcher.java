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

import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.telephony.CarrierConfigManager.KEY_DATA_SWITCH_VALIDATION_TIMEOUT_LONG;
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.MatchAllNetworkSpecifier;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.TelephonyNetworkSpecifier;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.AccessNetworkConstants;
import android.telephony.CarrierConfigManager;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PhoneCapability;
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
import android.util.LocalLog;
import android.util.Log;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
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
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.DataSwitch;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.OnDemandDataSwitch;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.internal.telephony.subscription.SubscriptionManagerService.WatchedInt;
import com.android.internal.telephony.util.NotificationChannelController;
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

/**
 * Utility singleton to monitor subscription changes and incoming NetworkRequests
 * and determine which phone/phones are active.
 *
 * Manages the ALLOW_DATA calls to modems and notifies phones about changes to
 * the active phones.  Note we don't wait for data attach (which may not happen anyway).
 */
public class PhoneSwitcher extends Handler {
    private static final String LOG_TAG = "PhoneSwitcher";
    protected static final boolean VDBG = Rlog.isLoggable(LOG_TAG, Log.VERBOSE);

    /** Fragment "key" argument passed thru {@link #SETTINGS_EXTRA_SHOW_FRAGMENT_ARGUMENTS} */
    private static final String SETTINGS_EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    /**
     * When starting this activity, this extra can also be specified to supply a Bundle of arguments
     * to pass to that fragment when it is instantiated during the initial creation of the activity.
     */
    private static final String SETTINGS_EXTRA_SHOW_FRAGMENT_ARGUMENTS =
            ":settings:show_fragment_args";
    /** The res Id of the auto data switch fragment in settings. **/
    private static final String AUTO_DATA_SWITCH_SETTING_R_ID = "auto_data_switch";
    /** Notification tag **/
    private static final String AUTO_DATA_SWITCH_NOTIFICATION_TAG = "auto_data_switch";
    /** Notification ID **/
    private static final int AUTO_DATA_SWITCH_NOTIFICATION_ID = 1;

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

    private final @NonNull NetworkRequestList mNetworkRequestList = new NetworkRequestList();
    protected final RegistrantList mActivePhoneRegistrants;
    private final SubscriptionManagerService mSubscriptionManagerService;
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
    /** {@code true} if we've displayed the notification the first time auto switch occurs **/
    private boolean mDisplayedAutoSwitchNotification = false;
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

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
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

    /** The count of consecutive auto switch validation failure **/
    private int mAutoSwitchRetryFailedCount = 0;

    // The phone ID that has an active voice call. If set, and its mobile data setting is on,
    // it will become the mPreferredDataPhoneId.
    protected int mPhoneIdInVoiceCall = SubscriptionManager.INVALID_PHONE_INDEX;

    @VisibleForTesting
    // It decides:
    // 1. In modem layer, which modem is DDS (preferred to have data traffic on)
    // 2. In TelephonyNetworkFactory, which subscription will apply default network requests, which
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

    private static final int EVENT_PRIMARY_DATA_SUB_CHANGED       = 101;
    protected static final int EVENT_SUBSCRIPTION_CHANGED         = 102;
    private static final int EVENT_REQUEST_NETWORK                = 103;
    private static final int EVENT_RELEASE_NETWORK                = 104;
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
    private static final int EVENT_EVALUATE_AUTO_SWITCH           = 111;
    private static final int EVENT_MODEM_COMMAND_DONE             = 112;
    private static final int EVENT_MODEM_COMMAND_RETRY            = 113;
    private static final int EVENT_SERVICE_STATE_CHANGED          = 114;
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
    private static final int EVENT_MEETS_AUTO_DATA_SWITCH_STATE   = 121;

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
    private final static int DEFAULT_VALIDATION_EXPIRATION_TIME = 2000;

    private ConnectivityManager mConnectivityManager;
    private int mImsRegistrationTech = REGISTRATION_TECH_NONE;

    private List<Set<CommandException.Error>> mCurrentDdsSwitchFailure;

    /**
     * {@code true} if requires ping test before switching preferred data modem; otherwise, switch
     * even if ping test fails.
     */
    private boolean mRequirePingTestBeforeDataSwitch = true;

    /**
     * Time threshold in ms to define a internet connection status to be stable(e.g. out of service,
     * in service, wifi is the default active network.etc), while -1 indicates auto switch
     * feature disabled.
     */
    private long mAutoDataSwitchAvailabilityStabilityTimeThreshold = -1;

    /**
     * The maximum number of retries when a validation for switching failed.
     */
    private int mAutoDataSwitchValidationMaxRetry;

    /** Data settings manager callback. Key is the phone id. */
    private final @NonNull Map<Integer, DataSettingsManagerCallback> mDataSettingsManagerCallbacks =
            new ArrayMap<>();

    private class DefaultNetworkCallback extends ConnectivityManager.NetworkCallback {
        public int mExpectedSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        public int mSwitchReason = TelephonyEvent.DataSwitch.Reason.DATA_SWITCH_REASON_UNKNOWN;
        public boolean isDefaultNetworkOnCellular = false;
        @Override
        public void onCapabilitiesChanged(Network network,
                NetworkCapabilities networkCapabilities) {
            if (networkCapabilities.hasTransport(TRANSPORT_CELLULAR)) {
                isDefaultNetworkOnCellular = true;
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
            } else {
                if (isDefaultNetworkOnCellular) {
                    // non-cellular transport is active
                    isDefaultNetworkOnCellular = false;
                    log("default network is active on non cellular");
                    evaluateIfAutoSwitchIsNeeded();
                }
            }
        }

        @Override
        public void onLost(Network network) {
            // try find an active sub to switch to
            if (!hasMessages(EVENT_EVALUATE_AUTO_SWITCH)) {
                sendEmptyMessage(EVENT_EVALUATE_AUTO_SWITCH);
            }
        }
    }

    private RegistrationManager.RegistrationCallback mRegistrationCallback =
            new RegistrationManager.RegistrationCallback() {
        @Override
        public void onRegistered(ImsRegistrationAttributes attributes) {
            int imsRegistrationTech = attributes.getRegistrationTechnology();
            if (imsRegistrationTech != mImsRegistrationTech) {
                mImsRegistrationTech = imsRegistrationTech;
                sendMessage(obtainMessage(EVENT_IMS_RADIO_TECH_CHANGED));
            }
        }

        @Override
        public void onUnregistered(ImsReasonInfo info) {
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
     * Method to get singleton instance.
     */
    public static PhoneSwitcher getInstance() {
        return sPhoneSwitcher;
    }

    /**
     * Method to create singleton instance.
     */
    public static PhoneSwitcher make(int maxDataAttachModemCount, Context context, Looper looper) {
        if (sPhoneSwitcher == null) {
            sPhoneSwitcher = new PhoneSwitcher(maxDataAttachModemCount, context, looper);
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
            ImsManager.getInstance(context, phoneId).addRegistrationCallback(
                    mRegistrationCallback, this::post);
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
    public PhoneSwitcher(int maxActivePhones, Context context, Looper looper) {
        super(looper);
        mContext = context;
        mActiveModemCount = getTm().getActiveModemCount();
        mPhoneSubscriptions = new int[mActiveModemCount];
        mPhoneStates = new PhoneState[mActiveModemCount];
        mMaxDataAttachModemCount = maxActivePhones;
        mLocalLog = new LocalLog(MAX_LOCAL_LOG_LINES);

        mSubscriptionManagerService = SubscriptionManagerService.getInstance();

        mRadioConfig = RadioConfig.getInstance();
        mValidator = CellularNetworkValidator.getInstance();

        mCurrentDdsSwitchFailure = new ArrayList<Set<CommandException.Error>>();
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
                }
                mDataSettingsManagerCallbacks.computeIfAbsent(phoneId,
                        v -> new DataSettingsManagerCallback(this::post) {
                            @Override
                            public void onDataEnabledChanged(boolean enabled,
                                    @TelephonyManager.DataEnabledChangedReason int reason,
                                    @NonNull String callingPackage) {
                                PhoneSwitcher.this.onDataEnabledChanged();
                            }});
                phone.getDataSettingsManager().registerCallback(
                        mDataSettingsManagerCallbacks.get(phoneId));
                phone.getServiceStateTracker().registerForServiceStateChanged(this,
                        EVENT_SERVICE_STATE_CHANGED, phoneId);
                registerForImsRadioTechChange(context, phoneId);
            }
            Set<CommandException.Error> ddsFailure = new HashSet<CommandException.Error>();
            mCurrentDdsSwitchFailure.add(ddsFailure);
        }

        if (mActiveModemCount > 0) {
            PhoneFactory.getPhone(0).mCi.registerForOn(this, EVENT_RADIO_ON, null);
        }

        readDeviceResourceConfig();

        TelephonyRegistryManager telephonyRegistryManager = (TelephonyRegistryManager)
                context.getSystemService(Context.TELEPHONY_REGISTRY_SERVICE);
        telephonyRegistryManager.addOnSubscriptionsChangedListener(
                mSubscriptionsChangedListener, mSubscriptionsChangedListener.getHandlerExecutor());

        mConnectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        mContext.registerReceiver(mDefaultDataChangedReceiver,
                new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED));

        PhoneConfigurationManager.registerForMultiSimConfigChange(
                this, EVENT_MULTI_SIM_CONFIG_CHANGED, null);

        mConnectivityManager.registerDefaultNetworkCallback(mDefaultNetworkCallback, this);

        final NetworkCapabilities.Builder builder = new NetworkCapabilities.Builder()
                .addTransportType(TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_SUPL)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_DUN)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_FOTA)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_IMS)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_CBS)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_IA)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_RCS)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_MMTEL)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_XCAP)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_EIMS)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_MCX)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_LATENCY)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_BANDWIDTH)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_1)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_2)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_3)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_4)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_5)
                .setNetworkSpecifier(new MatchAllNetworkSpecifier());

        NetworkFactory networkFactory = new PhoneSwitcherNetworkRequestListener(looper, context,
                builder.build(), this);
        // we want to see all requests
        networkFactory.registerIgnoringScore();

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

    private BroadcastReceiver mSimStateIntentReceiver = new BroadcastReceiver() {
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
            case EVENT_SERVICE_STATE_CHANGED: {
                AsyncResult ar = (AsyncResult) msg.obj;
                final int phoneId = (int) ar.userObj;
                onServiceStateChanged(phoneId);
                break;
            }
            case EVENT_MEETS_AUTO_DATA_SWITCH_STATE: {
                final int targetSubId = msg.arg1;
                final boolean needValidation = (boolean) msg.obj;
                validate(targetSubId, needValidation,
                        DataSwitch.Reason.DATA_SWITCH_REASON_AUTO, null);
                break;
            }
            case EVENT_PRIMARY_DATA_SUB_CHANGED: {
                evaluateIfImmediateDataSwitchIsNeeded("primary data sub changed",
                        DataSwitch.Reason.DATA_SWITCH_REASON_MANUAL);
                break;
            }
            case EVENT_REQUEST_NETWORK: {
                onRequestNetwork((NetworkRequest)msg.obj);
                break;
            }
            case EVENT_RELEASE_NETWORK: {
                onReleaseNetwork((NetworkRequest)msg.obj);
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
            case EVENT_EVALUATE_AUTO_SWITCH:
                evaluateIfAutoSwitchIsNeeded();
                break;
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
                registerForImsRadioTechChange();
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
                registerForImsRadioTechChange();

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
                    evaluateIfAutoSwitchIsNeeded();
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
                    mEmergencyOverride = req;
                } else {
                    mEmergencyOverride = req;
                }

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
                        && (TelephonyManager.SIM_STATE_LOADED == simState)
                        && isSimApplicationReady(slotIndex)) {
                        sendRilCommands(slotIndex);
                    }
                    // SIM loaded after subscriptions slot mapping are done. Evaluate for auto
                    // data switch.
                    sendEmptyMessage(EVENT_EVALUATE_AUTO_SWITCH);
                }
                break;
            }
        }
    }

    /**
     * Read the default device config from any default phone because the resource config are per
     * device. No need to register callback for the same reason.
     */
    private void readDeviceResourceConfig() {
        Phone phone = PhoneFactory.getDefaultPhone();
        DataConfigManager dataConfig = phone.getDataNetworkController().getDataConfigManager();
        mRequirePingTestBeforeDataSwitch = dataConfig.isPingTestBeforeAutoDataSwitchRequired();
        mAutoDataSwitchAvailabilityStabilityTimeThreshold =
                dataConfig.getAutoDataSwitchAvailabilityStabilityTimeThreshold();
        mAutoDataSwitchValidationMaxRetry =
                dataConfig.getAutoDataSwitchValidationMaxRetry();
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
            }

            mDataSettingsManagerCallbacks.computeIfAbsent(phone.getPhoneId(),
                    v -> new DataSettingsManagerCallback(this::post) {
                        @Override
                        public void onDataEnabledChanged(boolean enabled,
                                @TelephonyManager.DataEnabledChangedReason int reason,
                                @NonNull String callingPackage) {
                            PhoneSwitcher.this.onDataEnabledChanged();
                        }
                    });
            phone.getDataSettingsManager().registerCallback(
                    mDataSettingsManagerCallbacks.get(phone.getPhoneId()));
            phone.getServiceStateTracker().registerForServiceStateChanged(this,
                    EVENT_SERVICE_STATE_CHANGED, phoneId);

            Set<CommandException.Error> ddsFailure = new HashSet<CommandException.Error>();
            mCurrentDdsSwitchFailure.add(ddsFailure);
            registerForImsRadioTechChange(mContext, phoneId);
        }
    }

    /**
     * Called when
     * 1. user changed mobile data settings
     * 2. OR user changed auto data switch feature
     */
    private void onDataEnabledChanged() {
        logl("user changed data related settings");
        if (isAnyVoiceCallActiveOnDevice()) {
            // user changed data related settings during call, switch or turn off immediately
            evaluateIfImmediateDataSwitchIsNeeded(
                    "user changed data settings during call",
                    DataSwitch.Reason.DATA_SWITCH_REASON_IN_CALL);
        } else {
            evaluateIfAutoSwitchIsNeeded();
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

    private static class PhoneSwitcherNetworkRequestListener extends NetworkFactory {
        private final PhoneSwitcher mPhoneSwitcher;
        public PhoneSwitcherNetworkRequestListener (Looper l, Context c,
                NetworkCapabilities nc, PhoneSwitcher ps) {
            super(l, c, "PhoneSwitcherNetworkRequstListener", nc);
            mPhoneSwitcher = ps;
        }

        @Override
        protected void needNetworkFor(NetworkRequest networkRequest) {
            if (VDBG) log("needNetworkFor " + networkRequest);
            Message msg = mPhoneSwitcher.obtainMessage(EVENT_REQUEST_NETWORK);
            msg.obj = networkRequest;
            msg.sendToTarget();
        }

        @Override
        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            if (VDBG) log("releaseNetworkFor " + networkRequest);
            Message msg = mPhoneSwitcher.obtainMessage(EVENT_RELEASE_NETWORK);
            msg.obj = networkRequest;
            msg.sendToTarget();
        }
    }

    private void onRequestNetwork(NetworkRequest networkRequest) {
        TelephonyNetworkRequest telephonyNetworkRequest = new TelephonyNetworkRequest(
                networkRequest, PhoneFactory.getDefaultPhone());
        if (!mNetworkRequestList.contains(telephonyNetworkRequest)) {
            mNetworkRequestList.add(telephonyNetworkRequest);
            onEvaluate(REQUESTS_CHANGED, "netRequest");
        }
    }

    private void onReleaseNetwork(NetworkRequest networkRequest) {
        TelephonyNetworkRequest telephonyNetworkRequest = new TelephonyNetworkRequest(
                networkRequest, PhoneFactory.getDefaultPhone());
        if (mNetworkRequestList.remove(telephonyNetworkRequest)) {
            onEvaluate(REQUESTS_CHANGED, "netReleased");
            collectReleaseNetworkMetrics(networkRequest);
        }
    }

    private void registerDefaultNetworkChangeCallback(int expectedSubId, int reason) {
        mDefaultNetworkCallback.mExpectedSubId = expectedSubId;
        mDefaultNetworkCallback.mSwitchReason = reason;
    }

    private void collectRequestNetworkMetrics(NetworkRequest networkRequest) {
        // Request network for MMS will temporary disable the network on default data subscription,
        // this only happen on multi-sim device.
        if (mActiveModemCount > 1 && networkRequest.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_MMS)) {
            OnDemandDataSwitch onDemandDataSwitch = new OnDemandDataSwitch();
            onDemandDataSwitch.apn = TelephonyEvent.ApnType.APN_TYPE_MMS;
            onDemandDataSwitch.state = TelephonyEvent.EventState.EVENT_STATE_START;
            TelephonyMetrics.getInstance().writeOnDemandDataSwitch(onDemandDataSwitch);
        }
    }

    private void collectReleaseNetworkMetrics(NetworkRequest networkRequest) {
        // Release network for MMS will recover the network on default data subscription, this only
        // happen on multi-sim device.
        if (mActiveModemCount > 1 && networkRequest.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_MMS)) {
            OnDemandDataSwitch onDemandDataSwitch = new OnDemandDataSwitch();
            onDemandDataSwitch.apn = TelephonyEvent.ApnType.APN_TYPE_MMS;
            onDemandDataSwitch.state = TelephonyEvent.EventState.EVENT_STATE_END;
            TelephonyMetrics.getInstance().writeOnDemandDataSwitch(onDemandDataSwitch);
        }
    }

    /**
     * Called when service state changed.
     */
    private void onServiceStateChanged(int phoneId) {
        Phone phone = findPhoneById(phoneId);
        if (phone != null) {
            int newRegState = phone.getServiceState()
                    .getNetworkRegistrationInfo(
                            NetworkRegistrationInfo.DOMAIN_PS,
                            AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                    .getRegistrationState();
            if (newRegState != mPhoneStates[phoneId].dataRegState) {
                mPhoneStates[phoneId].dataRegState = newRegState;
                logl("onServiceStateChanged: phoneId:" + phoneId + " dataReg-> "
                        + NetworkRegistrationInfo.registrationStateToString(newRegState));
                if (!hasMessages(EVENT_EVALUATE_AUTO_SWITCH)) {
                    sendEmptyMessage(EVENT_EVALUATE_AUTO_SWITCH);
                }
            }
        }
    }

    /**
     * Evaluate if auto switch is suitable at the moment.
     */
    private void evaluateIfAutoSwitchIsNeeded() {
        // auto data switch feature is disabled from server
        if (mAutoDataSwitchAvailabilityStabilityTimeThreshold < 0) return;
        // check is valid DSDS
        if (!isActiveSubId(mPrimaryDataSubId) || mSubscriptionManagerService
                .getActiveSubIdList(true).length <= 1) {
            return;
        }

        Phone primaryDataPhone = getPhoneBySubId(mPrimaryDataSubId);
        if (primaryDataPhone == null) {
            loge("evaluateIfAutoSwitchIsNeeded: cannot find primary data phone. subId="
                    + mPrimaryDataSubId);
            return;
        }

        int primaryPhoneId = primaryDataPhone.getPhoneId();
        log("evaluateIfAutoSwitchIsNeeded: primaryPhoneId: " + primaryPhoneId
                + " preferredPhoneId: " + mPreferredDataPhoneId);
        Phone secondaryDataPhone;

        if (mPreferredDataPhoneId == primaryPhoneId) {
            // on primary data sub

            int candidateSubId = getAutoSwitchTargetSubIdIfExists();
            if (candidateSubId != INVALID_SUBSCRIPTION_ID) {
                startAutoDataSwitchStabilityCheck(candidateSubId, mRequirePingTestBeforeDataSwitch);
            } else {
                cancelPendingAutoDataSwitch();
            }
        } else if ((secondaryDataPhone = findPhoneById(mPreferredDataPhoneId)) != null) {
            // on secondary data sub

            if (!primaryDataPhone.isUserDataEnabled()
                    || !secondaryDataPhone.isDataAllowed()) {
                // immediately switch back if user setting changes
                mAutoSelectedDataSubId = DEFAULT_SUBSCRIPTION_ID;
                evaluateIfImmediateDataSwitchIsNeeded("User disabled data settings",
                        DataSwitch.Reason.DATA_SWITCH_REASON_MANUAL);
                return;
            }

            NetworkCapabilities defaultNetworkCapabilities = mConnectivityManager
                    .getNetworkCapabilities(mConnectivityManager.getActiveNetwork());
            if (defaultNetworkCapabilities != null && !defaultNetworkCapabilities
                    .hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                log("evaluateIfAutoSwitchIsNeeded: "
                        + "Default network is active on non-cellular transport");
                startAutoDataSwitchStabilityCheck(DEFAULT_SUBSCRIPTION_ID, false);
                return;
            }

            if (mPhoneStates[secondaryDataPhone.getPhoneId()].dataRegState
                    != NetworkRegistrationInfo.REGISTRATION_STATE_HOME) {
                // secondary phone lost its HOME availability
                startAutoDataSwitchStabilityCheck(DEFAULT_SUBSCRIPTION_ID, false);
                return;
            }

            if (isInService(mPhoneStates[primaryPhoneId])) {
                // primary becomes available
                startAutoDataSwitchStabilityCheck(DEFAULT_SUBSCRIPTION_ID,
                        mRequirePingTestBeforeDataSwitch);
                return;
            }

            // cancel any previous attempts of switching back to primary
            cancelPendingAutoDataSwitch();
        }
    }

    /**
     * @param phoneState The phone state to check
     * @return {@code true} if the phone state is considered in service.
     */
    private boolean isInService(@NonNull PhoneState phoneState) {
        return phoneState.dataRegState == NetworkRegistrationInfo.REGISTRATION_STATE_HOME
                || phoneState.dataRegState == NetworkRegistrationInfo.REGISTRATION_STATE_ROAMING;
    }

    /**
     * Called when the current environment suits auto data switch.
     * Start pre-switch validation if the current environment suits auto data switch for
     * {@link #mAutoDataSwitchAvailabilityStabilityTimeThreshold} MS.
     * @param targetSubId the target sub Id.
     * @param needValidation {@code true} if validation is needed.
     */
    private void startAutoDataSwitchStabilityCheck(int targetSubId, boolean needValidation) {
        log("startAutoDataSwitchStabilityCheck: targetSubId=" + targetSubId
                + " needValidation=" + needValidation);
        if (!hasMessages(EVENT_MEETS_AUTO_DATA_SWITCH_STATE, needValidation)) {
            sendMessageDelayed(obtainMessage(EVENT_MEETS_AUTO_DATA_SWITCH_STATE, targetSubId,
                            0/*placeholder*/,
                            needValidation),
                    mAutoDataSwitchAvailabilityStabilityTimeThreshold);
        }
    }

    /**
     * Cancel any auto switch attempts when the current environment is not suitable for auto switch.
     */
    private void cancelPendingAutoDataSwitch() {
        mAutoSwitchRetryFailedCount = 0;
        removeMessages(EVENT_MEETS_AUTO_DATA_SWITCH_STATE);
        if (mValidator.isValidating()) {
            mValidator.stopValidation();

            removeMessages(EVENT_NETWORK_VALIDATION_DONE);
            removeMessages(EVENT_NETWORK_AVAILABLE);
            mPendingSwitchSubId = INVALID_SUBSCRIPTION_ID;
            mPendingSwitchNeedValidation = false;
        }
    }

    /**
     * Called when consider switching from primary default data sub to another data sub.
     * @return the target subId if a suitable candidate is found, otherwise return
     * {@link SubscriptionManager#INVALID_SUBSCRIPTION_ID}
     */
    private int getAutoSwitchTargetSubIdIfExists() {
        Phone primaryDataPhone = getPhoneBySubId(mPrimaryDataSubId);
        if (primaryDataPhone == null) {
            log("getAutoSwitchTargetSubId: no sim loaded");
            return INVALID_SUBSCRIPTION_ID;
        }

        int primaryPhoneId = primaryDataPhone.getPhoneId();

        if (!primaryDataPhone.isUserDataEnabled()) {
            log("getAutoSwitchTargetSubId: user disabled data");
            return INVALID_SUBSCRIPTION_ID;
        }

        NetworkCapabilities defaultNetworkCapabilities = mConnectivityManager
                .getNetworkCapabilities(mConnectivityManager.getActiveNetwork());
        if (defaultNetworkCapabilities != null && !defaultNetworkCapabilities
                .hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            // Exists other active default transport
            log("getAutoSwitchTargetSubId: Default network is active on non-cellular transport");
            return INVALID_SUBSCRIPTION_ID;
        }

        // check whether primary and secondary signal status worth switching
        if (isInService(mPhoneStates[primaryPhoneId])) {
            log("getAutoSwitchTargetSubId: primary is in service");
            return INVALID_SUBSCRIPTION_ID;
        }
        for (int phoneId = 0; phoneId < mPhoneStates.length; phoneId++) {
            if (phoneId != primaryPhoneId) {
                // the alternative phone must have HOME availability
                if (mPhoneStates[phoneId].dataRegState
                        == NetworkRegistrationInfo.REGISTRATION_STATE_HOME) {
                    log("getAutoSwitchTargetSubId: found phone " + phoneId + " in HOME service");
                    Phone secondaryDataPhone = findPhoneById(phoneId);
                    if (secondaryDataPhone != null && // check auto switch feature enabled
                            secondaryDataPhone.isDataAllowed()) {
                        return secondaryDataPhone.getSubId();
                    }
                }
            }
        }
        return INVALID_SUBSCRIPTION_ID;
    }

    private TelephonyManager getTm() {
        return (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    protected static final boolean REQUESTS_CHANGED   = true;
    protected static final boolean REQUESTS_UNCHANGED = false;
    /**
     * Re-evaluate things. Do nothing if nothing's changed.
     *
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
                // Listen to IMS radio tech change for new sub
                if (SubscriptionManager.isValidSubscriptionId(sub)) {
                    registerForImsRadioTechChange(mContext, i);
                }
                diffDetected = true;
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
                List<Integer> newActivePhones = new ArrayList<Integer>();

                /**
                 * If all phones can have PS attached, activate all.
                 * Otherwise, choose to activate phones according to requests. And
                 * if list is not full, add mPreferredDataPhoneId.
                 */
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
        public @NetworkRegistrationInfo.RegistrationState int dataRegState =
                NetworkRegistrationInfo.REGISTRATION_STATE_NOT_REGISTERED_OR_SEARCHING;
        public long lastRequested = 0;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void activate(int phoneId) {
        switchPhone(phoneId, true);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
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
     *
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

    private void onPhoneCapabilityChangedInternal(PhoneCapability capability) {
        int newMaxDataAttachModemCount = TelephonyManager.getDefault()
                .getNumberOfModemsWithSimultaneousDataConnections();
        if (mMaxDataAttachModemCount != newMaxDataAttachModemCount) {
            mMaxDataAttachModemCount = newMaxDataAttachModemCount;
            logl("Max active phones changed to " + mMaxDataAttachModemCount);
            onEvaluate(REQUESTS_UNCHANGED, "phoneCfgChanged");
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
            int imsRegTech = mImsRegTechProvider.get(mContext, mPhoneIdInVoiceCall);
            if (isAnyVoiceCallActiveOnDevice() && imsRegTech != REGISTRATION_TECH_IWLAN) {
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
                && voicePhone.isDataAllowed();
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
     * If preferred phone changes, or phone activation status changes, registrants
     * will be notified.
     */
    public void registerForActivePhoneSwitch(Handler h, int what, Object o) {
        Registrant r = new Registrant(h, what, o);
        mActivePhoneRegistrants.add(r);
        r.notifyRegistrant();
    }

    public void unregisterForActivePhoneSwitch(Handler h) {
        mActivePhoneRegistrants.remove(h);
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
        logl("Validate subId " + subId + " due to " + switchReasonToString(switchReason)
                + " needValidation=" + needValidation);
        int subIdToValidate = (subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID)
                ? mPrimaryDataSubId : subId;
        if (!isActiveSubId(subIdToValidate)) {
            logl("Can't switch data to inactive subId " + subIdToValidate);
            if (subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
                // the default data sub is not selected yet, store the intent of switching to
                // default subId once it becomes available.
                mAutoSelectedDataSubId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
            }
            sendSetOpptCallbackHelper(callback, SET_OPPORTUNISTIC_SUB_INACTIVE_SUBSCRIPTION);
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
            setAutoSelectedDataSubIdInternal(subIdToValidate);
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
        mValidator.validate(subIdToValidate, validationTimeout, false, mValidationCallback);
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
            mAutoSwitchRetryFailedCount = 0;
        } else if (!confirm) {
            resultForCallBack = SET_OPPORTUNISTIC_SUB_VALIDATION_FAILED;

            // retry for auto data switch validation failure
            if (mLastSwitchPreferredDataReason == DataSwitch.Reason.DATA_SWITCH_REASON_AUTO) {
                scheduleAutoSwitchRetryEvaluation();
                mAutoSwitchRetryFailedCount++;
            }
        } else {
            if (subId == mPrimaryDataSubId) {
                setAutoSelectedDataSubIdInternal(DEFAULT_SUBSCRIPTION_ID);
            } else {
                setAutoSelectedDataSubIdInternal(subId);
            }
            resultForCallBack = SET_OPPORTUNISTIC_SUB_SUCCESS;
            mAutoSwitchRetryFailedCount = 0;
        }

        // Trigger callback if needed
        sendSetOpptCallbackHelper(mSetOpptSubCallback, resultForCallBack);
        mSetOpptSubCallback = null;
        mPendingSwitchSubId = INVALID_SUBSCRIPTION_ID;
    }

    /**
     * Schedule auto data switch evaluation retry if haven't reached the max retry count.
     */
    private void scheduleAutoSwitchRetryEvaluation() {
        if (mAutoSwitchRetryFailedCount < mAutoDataSwitchValidationMaxRetry) {
            if (!hasMessages(EVENT_EVALUATE_AUTO_SWITCH)) {
                sendMessageDelayed(obtainMessage(EVENT_EVALUATE_AUTO_SWITCH),
                        mAutoDataSwitchAvailabilityStabilityTimeThreshold
                                << mAutoSwitchRetryFailedCount);
            }
        } else {
            logl("scheduleAutoSwitchEvaluation: reached max auto switch retry count "
                    + mAutoDataSwitchValidationMaxRetry);
            mAutoSwitchRetryFailedCount = 0;
        }
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
     *
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

        // A phone in voice call might trigger data being switched to it.
        return (!phone.getBackgroundCall().isIdle()
                || !phone.getForegroundCall().isIdle());
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
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
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
    private static @NonNull String switchReasonToString(int reason) {
        switch(reason) {
            case TelephonyEvent.DataSwitch.Reason.DATA_SWITCH_REASON_UNKNOWN:
                return "UNKNOWN";
            case TelephonyEvent.DataSwitch.Reason.DATA_SWITCH_REASON_MANUAL:
                return "MANUAL";
            case TelephonyEvent.DataSwitch.Reason.DATA_SWITCH_REASON_IN_CALL:
                return "IN_CALL";
            case TelephonyEvent.DataSwitch.Reason.DATA_SWITCH_REASON_CBRS:
                return "CBRS";
            case TelephonyEvent.DataSwitch.Reason.DATA_SWITCH_REASON_AUTO:
                return "AUTO";
            default: return "UNKNOWN(" + reason + ")";
        }
    }

    /**
     * Concert switching state to string
     *
     * @param state The switching state.
     * @return The switching state in string format.
     */
    private static @NonNull String switchStateToString(int state) {
        switch(state) {
            case TelephonyEvent.EventState.EVENT_STATE_UNKNOWN:
                return "UNKNOWN";
            case TelephonyEvent.EventState.EVENT_STATE_START:
                return "START";
            case TelephonyEvent.EventState.EVENT_STATE_END:
                return "END";
            default: return "UNKNOWN(" + state + ")";
        }
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

    // TODO (b/148396668): add an internal callback method to monitor phone capability change,
    // and hook this call to that callback.
    private void onPhoneCapabilityChanged(PhoneCapability capability) {
        onPhoneCapabilityChangedInternal(capability);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        pw.println("PhoneSwitcher:");
        pw.increaseIndent();
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < mActiveModemCount; i++) {
            PhoneState ps = mPhoneStates[i];
            c.setTimeInMillis(ps.lastRequested);
            pw.println("PhoneId(" + i + ") active=" + ps.active + ", dataRegState="
                    + NetworkRegistrationInfo.registrationStateToString(ps.dataRegState)
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
        pw.println("mAutoDataSwitchAvailabilityStabilityTimeThreshold="
                + mAutoDataSwitchAvailabilityStabilityTimeThreshold);
        pw.println("mAutoDataSwitchValidationMaxRetry=" + mAutoDataSwitchValidationMaxRetry);
        pw.println("mRequirePingTestBeforeDataSwitch=" + mRequirePingTestBeforeDataSwitch);
        pw.println("mLastSwitchPreferredDataReason="
                + switchReasonToString(mLastSwitchPreferredDataReason));
        pw.println("mDisplayedAutoSwitchNotification=" + mDisplayedAutoSwitchNotification);
        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
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
        if (commandSuccess) logl("onDdsSwitchResponse: DDS switch success on phoneId = " + phoneId);
        mCurrentDdsSwitchFailure.get(phoneId).clear();
        // Notify all registrants
        mActivePhoneRegistrants.notifyRegistrants();
        notifyPreferredDataSubIdChanged();
        displayAutoDataSwitchNotification();
    }

    /**
     * Display a notification the first time auto data switch occurs.
     */
    private void displayAutoDataSwitchNotification() {
        NotificationManager notificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mDisplayedAutoSwitchNotification) {
            // cancel posted notification if any exist
            log("displayAutoDataSwitchNotification: canceling any notifications for subId "
                    + mAutoSelectedDataSubId);
            notificationManager.cancel(AUTO_DATA_SWITCH_NOTIFICATION_TAG,
                    AUTO_DATA_SWITCH_NOTIFICATION_ID);
            return;
        }
        // proceed only the first time auto data switch occurs, which includes data during call
        if (mLastSwitchPreferredDataReason != DataSwitch.Reason.DATA_SWITCH_REASON_AUTO) {
            log("displayAutoDataSwitchNotification: Ignore DDS switch due to "
                    + switchReasonToString(mLastSwitchPreferredDataReason));
            return;
        }
        SubscriptionInfo subInfo = mSubscriptionManagerService
                .getSubscriptionInfo(mAutoSelectedDataSubId);
        if (subInfo == null || subInfo.isOpportunistic()) {
            loge("displayAutoDataSwitchNotification: mAutoSelectedDataSubId="
                    + mAutoSelectedDataSubId + " unexpected subInfo " + subInfo);
            return;
        }
        logl("displayAutoDataSwitchNotification: display for subId=" + mAutoSelectedDataSubId);
        // "Mobile network settings" screen / dialog
        Intent intent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        final Bundle fragmentArgs = new Bundle();
        // Special contract for Settings to highlight permission row
        fragmentArgs.putString(SETTINGS_EXTRA_FRAGMENT_ARG_KEY, AUTO_DATA_SWITCH_SETTING_R_ID);
        intent.putExtra(Settings.EXTRA_SUB_ID, mAutoSelectedDataSubId);
        intent.putExtra(SETTINGS_EXTRA_SHOW_FRAGMENT_ARGUMENTS, fragmentArgs);
        PendingIntent contentIntent = PendingIntent.getActivity(
                mContext, mAutoSelectedDataSubId, intent, PendingIntent.FLAG_IMMUTABLE);

        CharSequence activeCarrierName = subInfo.getDisplayName();
        CharSequence contentTitle = mContext.getString(
                com.android.internal.R.string.auto_data_switch_title, activeCarrierName);
        CharSequence contentText = mContext.getText(
                com.android.internal.R.string.auto_data_switch_content);

        final Notification notif = new Notification.Builder(mContext)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setColor(mContext.getResources().getColor(
                        com.android.internal.R.color.system_notification_accent_color))
                .setChannelId(NotificationChannelController.CHANNEL_ID_MOBILE_DATA_STATUS)
                .setContentIntent(contentIntent)
                .setStyle(new Notification.BigTextStyle().bigText(contentText))
                .build();
        notificationManager.notify(AUTO_DATA_SWITCH_NOTIFICATION_TAG,
                AUTO_DATA_SWITCH_NOTIFICATION_ID, notif);
        mDisplayedAutoSwitchNotification = true;
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

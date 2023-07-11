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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneCapability;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyRegistryManager;
import android.telephony.data.ApnSetting;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsRegistrationAttributes;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.ArrayMap;
import android.util.LocalLog;

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
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.SubscriptionController.WatchedInt;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.data.DataNetworkController.NetworkRequestList;
import com.android.internal.telephony.data.DataSettingsManager.DataSettingsManagerCallback;
import com.android.internal.telephony.dataconnection.ApnConfigTypeRepository;
import com.android.internal.telephony.dataconnection.DcRequest;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.DataSwitch;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.OnDemandDataSwitch;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
    protected static final boolean VDBG = false;

    private static final int DEFAULT_NETWORK_CHANGE_TIMEOUT_MS = 5000;
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

    protected final List<DcRequest> mPrioritizedDcRequests = new ArrayList<>();
    private final @NonNull NetworkRequestList mNetworkRequestList = new NetworkRequestList();
    protected final RegistrantList mActivePhoneRegistrants;
    protected final SubscriptionController mSubscriptionController;
    protected final Context mContext;
    private final LocalLog mLocalLog;
    protected PhoneState[] mPhoneStates;
    protected int[] mPhoneSubscriptions;
    private boolean mIsRegisteredForImsRadioTechChange;
    @VisibleForTesting
    protected final CellularNetworkValidator mValidator;
    private int mPendingSwitchSubId = INVALID_SUBSCRIPTION_ID;
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

    // mOpptDataSubId must be an active subscription. If it's set, it overrides mPrimaryDataSubId
    // to be used for Internet data.
    private int mOpptDataSubId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;

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
    protected WatchedInt mPreferredDataSubId =
            new WatchedInt(SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
        @Override
        public void set(int newValue) {
            super.set(newValue);
            SubscriptionController.invalidateActiveDataSubIdCaches();
        }
    };

    // If non-null, An emergency call is about to be started, is ongoing, or has just ended and we
    // are overriding the DDS.
    // Internal state, should ONLY be accessed/modified inside of the handler.
    private EmergencyOverrideRequest mEmergencyOverride;

    private ISetOpportunisticDataCallback mSetOpptSubCallback;

    private static final int EVENT_PRIMARY_DATA_SUB_CHANGED       = 101;
    protected static final int EVENT_SUBSCRIPTION_CHANGED           = 102;
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
    @VisibleForTesting
    public static final int EVENT_PRECISE_CALL_STATE_CHANGED      = 109;
    private static final int EVENT_NETWORK_VALIDATION_DONE        = 110;
    private static final int EVENT_REMOVE_DEFAULT_NETWORK_CHANGE_CALLBACK = 111;
    private static final int EVENT_MODEM_COMMAND_DONE             = 112;
    private static final int EVENT_MODEM_COMMAND_RETRY            = 113;
    @VisibleForTesting
    public static final int EVENT_DATA_ENABLED_CHANGED            = 114;
    // An emergency call is about to be originated and requires the DDS to be overridden.
    // Uses EVENT_PRECISE_CALL_STATE_CHANGED message to start countdown to finish override defined
    // in mEmergencyOverride. If EVENT_PRECISE_CALL_STATE_CHANGED does not come in
    // DEFAULT_DATA_OVERRIDE_TIMEOUT_MS milliseconds, then the override will be removed.
    private static final int EVENT_OVERRIDE_DDS_FOR_EMERGENCY     = 115;
    // If it exists, remove the current mEmergencyOverride DDS override.
    private static final int EVENT_REMOVE_DDS_EMERGENCY_OVERRIDE  = 116;
    // If it exists, remove the current mEmergencyOverride DDS override.
    @VisibleForTesting
    public static final int EVENT_MULTI_SIM_CONFIG_CHANGED        = 117;
    private static final int EVENT_NETWORK_AVAILABLE              = 118;
    private static final int EVENT_PROCESS_SIM_STATE_CHANGE       = 119;
    @VisibleForTesting
    public static final int EVENT_IMS_RADIO_TECH_CHANGED          = 120;

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

    private Boolean mHasRegisteredDefaultNetworkChangeCallback = false;

    private ConnectivityManager mConnectivityManager;
    private int mImsRegistrationTech = REGISTRATION_TECH_NONE;

    private List<Set<CommandException.Error>> mCurrentDdsSwitchFailure;

    /** Data settings manager callback. Key is the phone id. */
    private final @NonNull Map<Integer, DataSettingsManagerCallback> mDataSettingsManagerCallbacks =
            new ArrayMap<>();

    private class DefaultNetworkCallback extends ConnectivityManager.NetworkCallback {
        public int mExpectedSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        public int mSwitchReason = TelephonyEvent.DataSwitch.Reason.DATA_SWITCH_REASON_UNKNOWN;
        @Override
        public void onCapabilitiesChanged(Network network,
                NetworkCapabilities networkCapabilities) {
            if (networkCapabilities.hasTransport(TRANSPORT_CELLULAR)
                    && SubscriptionManager.isValidSubscriptionId(mExpectedSubId)
                    && mExpectedSubId == getSubIdFromNetworkSpecifier(
                            networkCapabilities.getNetworkSpecifier())) {
                logDataSwitchEvent(
                        mExpectedSubId,
                        TelephonyEvent.EventState.EVENT_STATE_END,
                        mSwitchReason);
                removeDefaultNetworkChangeCallback();
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
            SubscriptionController.invalidateActiveDataSubIdCaches();
        }

        return sPhoneSwitcher;
    }

    /**
     * Whether this phone IMS registration is on its original network. This result impacts
     * whether we want to do DDS switch to the phone having voice call.
     * If it's registered on IWLAN or cross SIM in multi-SIM case, return false. Otherwise,
     * return true.
     */
    private boolean isImsOnOriginalNetwork(Phone phone) {
        if (phone == null) return false;
        int phoneId = phone.getPhoneId();
        if (!SubscriptionManager.isValidPhoneId(phoneId)) return false;

        int imsRegTech = mImsRegTechProvider.get(mContext, phoneId);
        // If IMS is registered on IWLAN or cross SIM, return false.
        boolean isOnOriginalNetwork = (imsRegTech != REGISTRATION_TECH_IWLAN)
                && (imsRegTech != REGISTRATION_TECH_CROSS_SIM);
        if (!isOnOriginalNetwork) {
            log("IMS call on IWLAN or cross SIM. Call will be ignored for DDS switch");
        }
        return isOnOriginalNetwork;
    }

    private boolean isPhoneInVoiceCallChanged() {
        int oldPhoneIdInVoiceCall = mPhoneIdInVoiceCall;
        // If there's no active call, the value will become INVALID_PHONE_INDEX
        // and internet data will be switched back to system selected or user selected
        // subscription.
        mPhoneIdInVoiceCall = SubscriptionManager.INVALID_PHONE_INDEX;
        for (Phone phone : PhoneFactory.getPhones()) {
            if (isPhoneInVoiceCall(phone) || (isPhoneInVoiceCall(phone.getImsPhone())
                    && isImsOnOriginalNetwork(phone))) {
                mPhoneIdInVoiceCall = phone.getPhoneId();
                break;
            }
        }

        if (mPhoneIdInVoiceCall != oldPhoneIdInVoiceCall) {
            log("isPhoneInVoiceCallChanged from phoneId " + oldPhoneIdInVoiceCall
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

    private void evaluateIfDataSwitchIsNeeded(String reason) {
        if (onEvaluate(REQUESTS_UNCHANGED, reason)) {
            logDataSwitchEvent(mPreferredDataSubId.get(),
                    TelephonyEvent.EventState.EVENT_STATE_START,
                    DataSwitch.Reason.DATA_SWITCH_REASON_IN_CALL);
            registerDefaultNetworkChangeCallback(mPreferredDataSubId.get(),
                    DataSwitch.Reason.DATA_SWITCH_REASON_IN_CALL);
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

        mSubscriptionController = SubscriptionController.getInstance();
        mRadioConfig = RadioConfig.getInstance();
        mValidator = CellularNetworkValidator.getInstance();

        mCurrentDdsSwitchFailure = new ArrayList<Set<CommandException.Error>>();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        mContext.registerReceiver(mSimStateIntentReceiver, filter);

        mActivePhoneRegistrants = new RegistrantList();
        for (int i = 0; i < mActiveModemCount; i++) {
            mPhoneStates[i] = new PhoneState();
            Phone phone = PhoneFactory.getPhone(i);
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
                if (phone.isUsingNewDataStack()) {
                    mDataSettingsManagerCallbacks.computeIfAbsent(phone.getPhoneId(),
                            v -> new DataSettingsManagerCallback(this::post) {
                                @Override
                                public void onDataEnabledChanged(boolean enabled,
                                        @TelephonyManager.DataEnabledChangedReason int reason,
                                        @NonNull String callingPackage) {
                                    evaluateIfDataSwitchIsNeeded("EVENT_DATA_ENABLED_CHANGED");
                                }});
                    phone.getDataSettingsManager().registerCallback(
                            mDataSettingsManagerCallbacks.get(phone.getPhoneId()));
                } else {
                    phone.getDataEnabledSettings().registerForDataEnabledChanged(
                            this, EVENT_DATA_ENABLED_CHANGED, null);
                }

                registerForImsRadioTechChange(context, i);
            }
            Set<CommandException.Error> ddsFailure = new HashSet<CommandException.Error>();
            mCurrentDdsSwitchFailure.add(ddsFailure);
        }

        if (mActiveModemCount > 0) {
            PhoneFactory.getPhone(0).mCi.registerForOn(this, EVENT_RADIO_ON, null);
        }

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

        log("PhoneSwitcher started");
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
                log("mSimStateIntentReceiver: slotIndex = " + slotIndex + " state = " + state);
                obtainMessage(EVENT_PROCESS_SIM_STATE_CHANGE, slotIndex, state).sendToTarget();
            }
        }
    };

    private boolean isSimApplicationReady(int slotIndex) {
        if (!SubscriptionManager.isValidSlotIndex(slotIndex)) {
            return false;
        }

        SubscriptionInfo info = SubscriptionController.getInstance()
                .getActiveSubscriptionInfoForSimSlotIndex(slotIndex,
                mContext.getOpPackageName(), null);
        boolean uiccAppsEnabled = info != null && info.areUiccApplicationsEnabled();

        IccCard iccCard = PhoneFactory.getPhone(slotIndex).getIccCard();
        if (!iccCard.isEmptyProfile() && uiccAppsEnabled) {
            log("isSimApplicationReady: SIM is ready for slotIndex: " + slotIndex);
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
                onEvaluate(REQUESTS_UNCHANGED, "subChanged");
                break;
            }
            case EVENT_PRIMARY_DATA_SUB_CHANGED: {
                if (onEvaluate(REQUESTS_UNCHANGED, "primary data subId changed")) {
                    logDataSwitchEvent(mPreferredDataSubId.get(),
                            TelephonyEvent.EventState.EVENT_STATE_START,
                            DataSwitch.Reason.DATA_SWITCH_REASON_MANUAL);
                    registerDefaultNetworkChangeCallback(mPreferredDataSubId.get(),
                            DataSwitch.Reason.DATA_SWITCH_REASON_MANUAL);
                }
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
                    log("Emergency override - ecbm status = " + isInEcm);
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
            case EVENT_IMS_RADIO_TECH_CHANGED:
                // register for radio tech change to listen to radio tech handover in case previous
                // attempt was not successful
                registerForImsRadioTechChange();
                // If the phoneId in voice call didn't change, do nothing.
                if (!isPhoneInVoiceCallChanged()) {
                    break;
                }
                evaluateIfDataSwitchIsNeeded("EVENT_IMS_RADIO_TECH_CHANGED");
                break;

            case EVENT_PRECISE_CALL_STATE_CHANGED: {
                // register for radio tech change to listen to radio tech handover in case previous
                // attempt was not successful
                registerForImsRadioTechChange();

                // If the phoneId in voice call didn't change, do nothing.
                if (!isPhoneInVoiceCallChanged()) {
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
                evaluateIfDataSwitchIsNeeded("EVENT_PRECISE_CALL_STATE_CHANGED");
                break;
            }

            case EVENT_DATA_ENABLED_CHANGED:
                evaluateIfDataSwitchIsNeeded("EVENT_DATA_ENABLED_CHANGED");
                break;
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
            case EVENT_REMOVE_DEFAULT_NETWORK_CHANGE_CALLBACK: {
                removeDefaultNetworkChangeCallback();
                break;
            }
            case EVENT_MODEM_COMMAND_DONE: {
                AsyncResult ar = (AsyncResult) msg.obj;
                onDdsSwitchResponse(ar);
                break;
            }
            case EVENT_MODEM_COMMAND_RETRY: {
                int phoneId = (int) msg.obj;
                if (isPhoneIdValidForRetry(phoneId)) {
                    log("EVENT_MODEM_COMMAND_RETRY: resend modem command on phone " + phoneId);
                    sendRilCommands(phoneId);
                } else {
                    log("EVENT_MODEM_COMMAND_RETRY: skip retry as DDS sub changed");
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
                        log("emergency override requested for phone id " + req.mPhoneId + " when "
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

                log("new emergency override - " + mEmergencyOverride);
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
                log("Emergency override removed - " + mEmergencyOverride);
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
                int slotIndex = (int) msg.arg1;
                int simState = (int) msg.arg2;

                if (!SubscriptionManager.isValidSlotIndex(slotIndex)) {
                    log("EVENT_PROCESS_SIM_STATE_CHANGE: skip processing due to invalid slotId: "
                            + slotIndex);
                } else if (mCurrentDdsSwitchFailure.get(slotIndex).contains(
                        CommandException.Error.INVALID_SIM_STATE)
                        && (TelephonyManager.SIM_STATE_LOADED == simState)
                        && isSimApplicationReady(slotIndex)) {
                    sendRilCommands(slotIndex);
                }
                break;
            }
        }
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

            if (phone.isUsingNewDataStack()) {
                mDataSettingsManagerCallbacks.computeIfAbsent(phone.getPhoneId(),
                        v -> new DataSettingsManagerCallback(this::post) {
                            @Override
                            public void onDataEnabledChanged(boolean enabled,
                                    @TelephonyManager.DataEnabledChangedReason int reason,
                                    @NonNull String callingPackage) {
                                evaluateIfDataSwitchIsNeeded("EVENT_DATA_ENABLED_CHANGED");
                            }
                        });
                phone.getDataSettingsManager().registerCallback(
                        mDataSettingsManagerCallbacks.get(phone.getPhoneId()));
            } else {
                phone.getDataEnabledSettings().registerForDataEnabledChanged(
                        this, EVENT_DATA_ENABLED_CHANGED, null);
            }

            Set<CommandException.Error> ddsFailure = new HashSet<CommandException.Error>();
            mCurrentDdsSwitchFailure.add(ddsFailure);
            registerForImsRadioTechChange(mContext, phoneId);
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
        if (PhoneFactory.getDefaultPhone().isUsingNewDataStack()) {
            TelephonyNetworkRequest telephonyNetworkRequest = new TelephonyNetworkRequest(
                    networkRequest, PhoneFactory.getDefaultPhone());
            if (!mNetworkRequestList.contains(telephonyNetworkRequest)) {
                mNetworkRequestList.add(telephonyNetworkRequest);
                onEvaluate(REQUESTS_CHANGED, "netRequest");
            }
            return;
        }
        final DcRequest dcRequest =
                DcRequest.create(networkRequest, createApnRepository(networkRequest));
        if (dcRequest != null) {
            if (!mPrioritizedDcRequests.contains(dcRequest)) {
                collectRequestNetworkMetrics(networkRequest);
                mPrioritizedDcRequests.add(dcRequest);
                Collections.sort(mPrioritizedDcRequests);
                onEvaluate(REQUESTS_CHANGED, "netRequest");
                if (VDBG) log("Added DcRequest, size: " + mPrioritizedDcRequests.size());
            }
        }
    }

    private void onReleaseNetwork(NetworkRequest networkRequest) {
        if (PhoneFactory.getDefaultPhone().isUsingNewDataStack()) {
            TelephonyNetworkRequest telephonyNetworkRequest = new TelephonyNetworkRequest(
                    networkRequest, PhoneFactory.getDefaultPhone());
            if (mNetworkRequestList.remove(telephonyNetworkRequest)) {
                onEvaluate(REQUESTS_CHANGED, "netReleased");
                collectReleaseNetworkMetrics(networkRequest);
            }
            return;
        }
        final DcRequest dcRequest =
                DcRequest.create(networkRequest, createApnRepository(networkRequest));
        if (dcRequest != null) {
            if (mPrioritizedDcRequests.remove(dcRequest)) {
                onEvaluate(REQUESTS_CHANGED, "netReleased");
                collectReleaseNetworkMetrics(networkRequest);
                if (VDBG) log("Removed DcRequest, size: " + mPrioritizedDcRequests.size());
            }
        }
    }

    private ApnConfigTypeRepository createApnRepository(NetworkRequest networkRequest) {
        int phoneIdForRequest = phoneIdForRequest(networkRequest);
        int subId = mSubscriptionController.getSubIdUsingPhoneId(phoneIdForRequest);
        CarrierConfigManager configManager = (CarrierConfigManager) mContext
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);

        PersistableBundle carrierConfig;
        if (configManager != null) {
            carrierConfig = configManager.getConfigForSubId(subId);
        } else {
            carrierConfig = null;
        }
        return new ApnConfigTypeRepository(carrierConfig);
    }

    private void removeDefaultNetworkChangeCallback() {
        removeMessages(EVENT_REMOVE_DEFAULT_NETWORK_CHANGE_CALLBACK);
        mDefaultNetworkCallback.mExpectedSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mDefaultNetworkCallback.mSwitchReason =
                TelephonyEvent.DataSwitch.Reason.DATA_SWITCH_REASON_UNKNOWN;
        mConnectivityManager.unregisterNetworkCallback(mDefaultNetworkCallback);
    }

    private void registerDefaultNetworkChangeCallback(int expectedSubId, int reason) {
        mDefaultNetworkCallback.mExpectedSubId = expectedSubId;
        mDefaultNetworkCallback.mSwitchReason = reason;
        mConnectivityManager.registerDefaultNetworkCallback(mDefaultNetworkCallback, this);
        sendMessageDelayed(
                obtainMessage(EVENT_REMOVE_DEFAULT_NETWORK_CHANGE_CALLBACK),
                DEFAULT_NETWORK_CHANGE_TIMEOUT_MS);
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
        final int primaryDataSubId = mSubscriptionController.getDefaultDataSubId();
        if (primaryDataSubId != mPrimaryDataSubId) {
            sb.append(" mPrimaryDataSubId ").append(mPrimaryDataSubId).append("->")
                .append(primaryDataSubId);
            mPrimaryDataSubId = primaryDataSubId;
        }

        // Check to see if there is any active subscription on any phone
        boolean hasAnyActiveSubscription = false;

        // Check if phoneId to subId mapping is changed.
        for (int i = 0; i < mActiveModemCount; i++) {
            int sub = mSubscriptionController.getSubIdUsingPhoneId(i);

            if (SubscriptionManager.isValidSubscriptionId(sub)) hasAnyActiveSubscription = true;

            if (sub != mPhoneSubscriptions[i]) {
                sb.append(" phone[").append(i).append("] ").append(mPhoneSubscriptions[i]);
                sb.append("->").append(sub);
                mPhoneSubscriptions[i] = sub;
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
            sb.append(" preferred phoneId ").append(oldPreferredDataPhoneId)
                    .append("->").append(mPreferredDataPhoneId);
            diffDetected = true;
        } else if (oldPreferredDataSubId != mPreferredDataSubId.get()) {
            log("SIM refresh, notify dds change");
            // Inform connectivity about the active data phone
            notifyPreferredDataSubIdChanged();
        }

        // Always force DDS when radio on. This is to handle the corner cases that modem and android
        // DDS are out of sync after APM, AP should force DDS when radio on. long term solution
        // should be having API to query preferred data modem to detect the out-of-sync scenarios.
        if (diffDetected || EVALUATION_REASON_RADIO_ON.equals(reason)) {
            log("evaluating due to " + sb.toString());
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
                        if (PhoneFactory.getDefaultPhone().isUsingNewDataStack()) {
                            for (TelephonyNetworkRequest networkRequest : mNetworkRequestList) {
                                int phoneIdForRequest = phoneIdForRequest(networkRequest);
                                if (phoneIdForRequest == INVALID_PHONE_INDEX) continue;
                                if (newActivePhones.contains(phoneIdForRequest)) continue;
                                newActivePhones.add(phoneIdForRequest);
                                if (newActivePhones.size() >= mMaxDataAttachModemCount) break;
                            }
                        } else {
                            for (DcRequest dcRequest : mPrioritizedDcRequests) {
                                int phoneIdForRequest = phoneIdForRequest(dcRequest.networkRequest);
                                if (phoneIdForRequest == INVALID_PHONE_INDEX) continue;
                                if (newActivePhones.contains(phoneIdForRequest)) continue;
                                newActivePhones.add(phoneIdForRequest);
                                if (newActivePhones.size() >= mMaxDataAttachModemCount) break;
                            }
                        }
                    }

                    if (newActivePhones.size() < mMaxDataAttachModemCount
                            && newActivePhones.contains(mPreferredDataPhoneId)
                            && SubscriptionManager.isUsableSubIdValue(mPreferredDataPhoneId)) {
                        newActivePhones.add(mPreferredDataPhoneId);
                    }
                }

                if (VDBG) {
                    log("mPrimaryDataSubId = " + mPrimaryDataSubId);
                    log("mOpptDataSubId = " + mOpptDataSubId);
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
        log((active ? "activate " : "deactivate ") + phoneId);
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
            log("sendRilCommands: skip dds switch due to invalid phoneId=" + phoneId);
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
            log("sendRilCommands: setPreferredDataModem - phoneId: " + phoneId);
            mRadioConfig.setPreferredDataModem(mPreferredDataPhoneId, message);
        }
    }

    private void onPhoneCapabilityChangedInternal(PhoneCapability capability) {
        int newMaxDataAttachModemCount = TelephonyManager.getDefault()
                .getNumberOfModemsWithSimultaneousDataConnections();
        if (mMaxDataAttachModemCount != newMaxDataAttachModemCount) {
            mMaxDataAttachModemCount = newMaxDataAttachModemCount;
            log("Max active phones changed to " + mMaxDataAttachModemCount);
            onEvaluate(REQUESTS_UNCHANGED, "phoneCfgChanged");
        }
    }

    // Merge phoneIdForRequest(NetworkRequest netRequest) after Phone.isUsingNewDataStack() is
    // cleaned up.
    private int phoneIdForRequest(TelephonyNetworkRequest networkRequest) {
        return phoneIdForRequest(networkRequest.getNativeNetworkRequest());
    }

    private int phoneIdForRequest(NetworkRequest netRequest) {
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

    private int getSubIdForDefaultNetworkRequests() {
        if (mSubscriptionController.isActiveSubId(mOpptDataSubId)) {
            return mOpptDataSubId;
        } else {
            return mPrimaryDataSubId;
        }
    }

    // This updates mPreferredDataPhoneId which decides which phone should handle default network
    // requests.
    protected void updatePreferredDataPhoneId() {
        Phone voicePhone = findPhoneById(mPhoneIdInVoiceCall);
        boolean isDataEnabled = false;
        if (voicePhone != null) {
            if (voicePhone.isUsingNewDataStack()) {
                isDataEnabled = voicePhone.getDataSettingsManager()
                        .isDataEnabled(ApnSetting.TYPE_DEFAULT);
            } else {
                isDataEnabled = voicePhone.getDataEnabledSettings()
                        .isDataEnabled(ApnSetting.TYPE_DEFAULT);
            }
        }

        if (mEmergencyOverride != null && findPhoneById(mEmergencyOverride.mPhoneId) != null) {
            // Override DDS for emergency even if user data is not enabled, since it is an
            // emergency.
            // TODO: Provide a notification to the user that metered data is currently being
            // used during this period.
            log("updatePreferredDataPhoneId: preferred data overridden for emergency."
                    + " phoneId = " + mEmergencyOverride.mPhoneId);
            mPreferredDataPhoneId = mEmergencyOverride.mPhoneId;
        } else if (isDataEnabled) {
            // If a phone is in call and user enabled its mobile data, we
            // should switch internet connection to it. Because the other modem
            // will lose data connection anyway.
            // TODO: validate network first.
            mPreferredDataPhoneId = mPhoneIdInVoiceCall;
        } else {
            int subId = getSubIdForDefaultNetworkRequests();
            int phoneId = SubscriptionManager.INVALID_PHONE_INDEX;

            if (SubscriptionManager.isUsableSubIdValue(subId)) {
                for (int i = 0; i < mActiveModemCount; i++) {
                    if (mPhoneSubscriptions[i] == subId) {
                        phoneId = i;
                        break;
                    }
                }
            }

            mPreferredDataPhoneId = phoneId;
        }

        mPreferredDataSubId.set(
                mSubscriptionController.getSubIdUsingPhoneId(mPreferredDataPhoneId));
    }

    protected void transitionToEmergencyPhone() {
        if (mActiveModemCount <= 0) {
            log("No phones: unable to reset preferred phone for emergency");
            return;
        }

        if (mPreferredDataPhoneId != DEFAULT_EMERGENCY_PHONE_ID) {
            log("No active subscriptions: resetting preferred phone to 0 for emergency");
            mPreferredDataPhoneId = DEFAULT_EMERGENCY_PHONE_ID;
        }

        if (mPreferredDataSubId.get() != INVALID_SUBSCRIPTION_ID) {
            mPreferredDataSubId.set(INVALID_SUBSCRIPTION_ID);
            notifyPreferredDataSubIdChanged();
        }
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

        // In any case, if phone state is inactive, don't apply the network request.
        if (!isPhoneActive(phoneId) || (
                mSubscriptionController.getSubIdUsingPhoneId(phoneId) == INVALID_SUBSCRIPTION_ID
                && !isEmergencyNetworkRequest(networkRequest))) {
            return false;
        }

        NetworkRequest netRequest = networkRequest.getNativeNetworkRequest();
        int subId = getSubIdFromNetworkSpecifier(netRequest.getNetworkSpecifier());

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
        if (!mSubscriptionController.isActiveSubId(subId)
                && subId != SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
            log("Can't switch data to inactive subId " + subId);
            sendSetOpptCallbackHelper(callback, SET_OPPORTUNISTIC_SUB_INACTIVE_SUBSCRIPTION);
            return;
        }

        // Remove EVENT_NETWORK_VALIDATION_DONE. Don't handle validation result of previously subId
        // if queued.
        removeMessages(EVENT_NETWORK_VALIDATION_DONE);
        removeMessages(EVENT_NETWORK_AVAILABLE);

        int subIdToValidate = (subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID)
                ? mPrimaryDataSubId : subId;

        mPendingSwitchSubId = INVALID_SUBSCRIPTION_ID;

        if (mValidator.isValidating()) {
            mValidator.stopValidation();
            sendSetOpptCallbackHelper(mSetOpptSubCallback, SET_OPPORTUNISTIC_SUB_VALIDATION_FAILED);
            mSetOpptSubCallback = null;
        }

        if (subId == mOpptDataSubId) {
            sendSetOpptCallbackHelper(callback, SET_OPPORTUNISTIC_SUB_SUCCESS);
            return;
        }

        logDataSwitchEvent(subId == DEFAULT_SUBSCRIPTION_ID ? mPrimaryDataSubId : subId,
                TelephonyEvent.EventState.EVENT_STATE_START,
                DataSwitch.Reason.DATA_SWITCH_REASON_CBRS);
        registerDefaultNetworkChangeCallback(
                subId == DEFAULT_SUBSCRIPTION_ID ? mPrimaryDataSubId : subId,
                DataSwitch.Reason.DATA_SWITCH_REASON_CBRS);

        // If validation feature is not supported, set it directly. Otherwise,
        // start validation on the subscription first.
        if (!mValidator.isValidationFeatureSupported()) {
            setOpportunisticSubscriptionInternal(subId);
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
            log("RemoteException " + exception);
        }
    }

    /**
     * Set opportunistic data subscription.
     */
    private void setOpportunisticSubscriptionInternal(int subId) {
        if (mOpptDataSubId != subId) {
            mOpptDataSubId = subId;
            onEvaluate(REQUESTS_UNCHANGED, "oppt data subId changed");
        }
    }

    private void confirmSwitch(int subId, boolean confirm) {
        log("confirmSwitch: subId " + subId + (confirm ? " confirmed." : " cancelled."));
        int resultForCallBack;
        if (!mSubscriptionController.isActiveSubId(subId)) {
            log("confirmSwitch: subId " + subId + " is no longer active");
            resultForCallBack = SET_OPPORTUNISTIC_SUB_INACTIVE_SUBSCRIPTION;
        } else if (!confirm) {
            resultForCallBack = SET_OPPORTUNISTIC_SUB_VALIDATION_FAILED;
        } else {
            if (mSubscriptionController.isOpportunistic(subId)) {
                setOpportunisticSubscriptionInternal(subId);
            } else {
                // Switching data back to primary subscription.
                setOpportunisticSubscriptionInternal(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
            }
            resultForCallBack = SET_OPPORTUNISTIC_SUB_SUCCESS;
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
        log("onValidationDone: " + (passed ? "passed" : "failed") + " on subId " + subId);
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
        log("Try set opportunistic data subscription to subId " + subId
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

    public int getOpportunisticDataSubscriptionId() {
        return mOpptDataSubId;
    }

    public int getPreferredDataPhoneId() {
        return mPreferredDataPhoneId;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void log(String l) {
        Rlog.d(LOG_TAG, l);
        mLocalLog.log(l);
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
        log("Data switch event. subId=" + subId + ", state=" + switchStateToString(state)
                + ", reason=" + switchReasonToString(reason));
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
        log("notifyPreferredDataSubIdChanged to " + mPreferredDataSubId.get());
        telephonyRegistryManager.notifyActiveDataSubIdChanged(mPreferredDataSubId.get());
    }

    /**
     * @return The active data subscription id
     */
    public int getActiveDataSubId() {
        return mPreferredDataSubId.get();
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
            pw.println("PhoneId(" + i + ") active=" + ps.active + ", lastRequest=" +
                    (ps.lastRequested == 0 ? "never" :
                     String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c)));
        }
        pw.println("mPreferredDataPhoneId=" + mPreferredDataPhoneId);
        pw.println("mPreferredDataSubId=" + mPreferredDataSubId.get());
        pw.println("DefaultDataSubId=" + mSubscriptionController.getDefaultDataSubId());
        pw.println("DefaultDataPhoneId=" + mSubscriptionController.getPhoneId(
                mSubscriptionController.getDefaultDataSubId()));
        pw.println("mPrimaryDataSubId=" + mPrimaryDataSubId);
        pw.println("mOpptDataSubId=" + mOpptDataSubId);
        pw.println("mIsRegisteredForImsRadioTechChange=" + mIsRegisteredForImsRadioTechChange);
        pw.println("mPendingSwitchNeedValidation=" + mPendingSwitchNeedValidation);
        pw.println("mMaxDataAttachModemCount=" + mMaxDataAttachModemCount);
        pw.println("mActiveModemCount=" + mActiveModemCount);
        pw.println("mPhoneIdInVoiceCall=" + mPhoneIdInVoiceCall);
        pw.println("mCurrentDdsSwitchFailure=" + mCurrentDdsSwitchFailure);
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
            log("Emergency override result sent = " + commandSuccess);
            mEmergencyOverride.sendOverrideCompleteCallbackResultAndClear(commandSuccess);
            // Do not retry , as we do not allow changes in onEvaluate during an emergency
            // call. When the call ends, we will start the countdown to remove the override.
        } else if (!commandSuccess) {
            log("onDdsSwitchResponse: DDS switch failed. with exception " + ar.exception);
            if (ar.exception instanceof CommandException) {
                CommandException.Error error = ((CommandException)
                        (ar.exception)).getCommandError();
                mCurrentDdsSwitchFailure.get(phoneId).add(error);
                if (error == CommandException.Error.OP_NOT_ALLOWED_DURING_VOICE_CALL) {
                    log("onDdsSwitchResponse: Wait for call end indication");
                    return;
                } else if (error == CommandException.Error.INVALID_SIM_STATE) {
                    /* If there is a attach failure due to sim not ready then
                    hold the retry until sim gets ready */
                    log("onDdsSwitchResponse: Wait for SIM to get READY");
                    return;
                }
            }
            log("onDdsSwitchResponse: Scheduling DDS switch retry");
            sendMessageDelayed(Message.obtain(this, EVENT_MODEM_COMMAND_RETRY,
                        phoneId), MODEM_COMMAND_RETRY_PERIOD_MS);
            return;
        }
        if (commandSuccess) log("onDdsSwitchResponse: DDS switch success on phoneId = " + phoneId);
        mCurrentDdsSwitchFailure.get(phoneId).clear();
        // Notify all registrants
        mActivePhoneRegistrants.notifyRegistrants();
        notifyPreferredDataSubIdChanged();
    }

    private boolean isPhoneIdValidForRetry(int phoneId) {
        int phoneIdForRequest = INVALID_PHONE_INDEX;
        int ddsPhoneId = mSubscriptionController.getPhoneId(
                mSubscriptionController.getDefaultDataSubId());
        if (ddsPhoneId != INVALID_PHONE_INDEX && ddsPhoneId == phoneId) {
            return true;
        } else {
            if (PhoneFactory.getDefaultPhone().isUsingNewDataStack()) {
                if (mNetworkRequestList.isEmpty()) return false;
                for (TelephonyNetworkRequest networkRequest : mNetworkRequestList) {
                    phoneIdForRequest = phoneIdForRequest(networkRequest);
                    if (phoneIdForRequest == phoneId) {
                        return true;
                    }
                }
            } else {
                if (mPrioritizedDcRequests.size() == 0) {
                    return false;
                }
                for (DcRequest dcRequest : mPrioritizedDcRequests) {
                    if (dcRequest != null) {
                        phoneIdForRequest = phoneIdForRequest(dcRequest.networkRequest);
                        if (phoneIdForRequest == phoneId) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}

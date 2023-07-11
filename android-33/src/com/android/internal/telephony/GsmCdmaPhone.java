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

package com.android.internal.telephony;

import static com.android.internal.telephony.CommandException.Error.GENERIC_FAILURE;
import static com.android.internal.telephony.CommandException.Error.SIM_BUSY;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_DISABLE;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_ENABLE;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_ERASURE;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_REGISTRATION;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_ALL;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_ALL_CONDITIONAL;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_BUSY;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_NOT_REACHABLE;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_NO_REPLY;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_UNCONDITIONAL;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_VOICE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.sysprop.TelephonyProperties;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.AccessNetworkConstants;
import android.telephony.Annotation.DataActivityType;
import android.telephony.Annotation.RadioPowerState;
import android.telephony.BarringInfo;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.LinkCapacityEstimate;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneNumberUtils;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.ServiceState.RilRadioTechnology;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.telephony.UssdResponse;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.cdma.CdmaMmiCode;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.data.AccessNetworksManager;
import com.android.internal.telephony.data.DataNetworkController;
import com.android.internal.telephony.data.LinkBandwidthEstimator;
import com.android.internal.telephony.dataconnection.DataEnabledSettings;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.TransportManager;
import com.android.internal.telephony.emergency.EmergencyNumberTracker;
import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.imsphone.ImsPhoneMmiCode;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.metrics.VoiceCallSessionStats;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccException;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.IccVmNotSupportedException;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.IsimUiccRecords;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccPort;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.internal.telephony.uicc.UiccSlot;
import com.android.internal.telephony.util.ArrayUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@hide}
 */
public class GsmCdmaPhone extends Phone {
    // NOTE that LOG_TAG here is "GsmCdma", which means that log messages
    // from this file will go into the radio log rather than the main
    // log.  (Use "adb logcat -b radio" to see them.)
    public static final String LOG_TAG = "GsmCdmaPhone";
    private static final boolean DBG = true;
    private static final boolean VDBG = false; /* STOPSHIP if true */

    /** Required throughput change between unsolicited LinkCapacityEstimate reports. */
    private static final int REPORTING_HYSTERESIS_KBPS = 50;
    /** Minimum time between unsolicited LinkCapacityEstimate reports. */
    private static final int REPORTING_HYSTERESIS_MILLIS = 3000;

    //GSM
    // Key used to read/write voice mail number
    private static final String VM_NUMBER = "vm_number_key";
    // Key used to read/write the SIM IMSI used for storing the voice mail
    private static final String VM_SIM_IMSI = "vm_sim_imsi_key";
    /** List of Registrants to receive Supplementary Service Notifications. */
    private RegistrantList mSsnRegistrants = new RegistrantList();

    //CDMA
    // Default Emergency Callback Mode exit timer
    private static final long DEFAULT_ECM_EXIT_TIMER_VALUE = 300000;
    private static final String VM_NUMBER_CDMA = "vm_number_key_cdma";
    public static final int RESTART_ECM_TIMER = 0; // restart Ecm timer
    public static final int CANCEL_ECM_TIMER = 1; // cancel Ecm timer
    private static final String PREFIX_WPS = "*272";
    // WPS prefix when CLIR is being deactivated for the call.
    private static final String PREFIX_WPS_CLIR_DEACTIVATE = "#31#*272";
    // WPS prefix when CLIS is being activated for the call.
    private static final String PREFIX_WPS_CLIR_ACTIVATE = "*31#*272";
    private CdmaSubscriptionSourceManager mCdmaSSM;
    public int mCdmaSubscriptionSource = CdmaSubscriptionSourceManager.SUBSCRIPTION_SOURCE_UNKNOWN;
    private PowerManager.WakeLock mWakeLock;
    // mEcmExitRespRegistrant is informed after the phone has been exited
    @UnsupportedAppUsage
    private Registrant mEcmExitRespRegistrant;
    private String mEsn;
    private String mMeid;
    // string to define how the carrier specifies its own ota sp number
    private String mCarrierOtaSpNumSchema;
    private Boolean mUiccApplicationsEnabled = null;
    // keeps track of when we have triggered an emergency call due to the ril.test.emergencynumber
    // param being set and we should generate a simulated exit from the modem upon exit of ECbM.
    private boolean mIsTestingEmergencyCallbackMode = false;
    @VisibleForTesting
    public static int ENABLE_UICC_APPS_MAX_RETRIES = 3;
    private static final int REAPPLY_UICC_APPS_SETTING_RETRY_TIME_GAP_IN_MS = 5000;

    // A runnable which is used to automatically exit from Ecm after a period of time.
    private Runnable mExitEcmRunnable = new Runnable() {
        @Override
        public void run() {
            exitEmergencyCallbackMode();
        }
    };
    public static final String PROPERTY_CDMA_HOME_OPERATOR_NUMERIC =
            "ro.cdma.home.operator.numeric";

    //CDMALTE
    /** PHONE_TYPE_CDMA_LTE in addition to RuimRecords needs access to SIMRecords and
     * IsimUiccRecords
     */
    private SIMRecords mSimRecords;

    // For non-persisted manual network selection
    private String mManualNetworkSelectionPlmn;

    //Common
    // Instance Variables
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private IsimUiccRecords mIsimUiccRecords;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public GsmCdmaCallTracker mCT;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public ServiceStateTracker mSST;
    public EmergencyNumberTracker mEmergencyNumberTracker;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private ArrayList <MmiCode> mPendingMMIs = new ArrayList<MmiCode>();
    private IccPhoneBookInterfaceManager mIccPhoneBookIntManager;

    private int mPrecisePhoneType;

    // mEcmTimerResetRegistrants are informed after Ecm timer is canceled or re-started
    private final RegistrantList mEcmTimerResetRegistrants = new RegistrantList();

    private final RegistrantList mVolteSilentRedialRegistrants = new RegistrantList();
    private DialArgs mDialArgs = null;

    private String mImei;
    private String mImeiSv;
    private String mVmNumber;

    // Create Cfu (Call forward unconditional) so that dialing number &
    // mOnComplete (Message object passed by client) can be packed &
    // given as a single Cfu object as user data to RIL.
    private static class Cfu {
        final String mSetCfNumber;
        final Message mOnComplete;

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        Cfu(String cfNumber, Message onComplete) {
            mSetCfNumber = cfNumber;
            mOnComplete = onComplete;
        }
    }

    /**
     * Used to create ImsManager instances, which may be injected during testing.
     */
    @VisibleForTesting
    public interface ImsManagerFactory {
        /**
         * Create a new instance of ImsManager for the specified phoneId.
         */
        ImsManager create(Context context, int phoneId);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private IccSmsInterfaceManager mIccSmsInterfaceManager;

    private boolean mResetModemOnRadioTechnologyChange = false;
    private boolean mSsOverCdmaSupported = false;

    private int mRilVersion;
    private boolean mBroadcastEmergencyCallStateChanges = false;
    private @ServiceState.RegState int mTelecomVoiceServiceStateOverride =
            ServiceState.STATE_OUT_OF_SERVICE;

    private CarrierKeyDownloadManager mCDM;
    private CarrierInfoManager mCIM;

    private final SettingsObserver mSettingsObserver;

    private final ImsManagerFactory mImsManagerFactory;
    private final CarrierPrivilegesTracker mCarrierPrivilegesTracker;

    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionsChangedListener;

    // Constructors

    public GsmCdmaPhone(Context context, CommandsInterface ci, PhoneNotifier notifier, int phoneId,
                        int precisePhoneType, TelephonyComponentFactory telephonyComponentFactory) {
        this(context, ci, notifier, false, phoneId, precisePhoneType, telephonyComponentFactory);
    }

    public GsmCdmaPhone(Context context, CommandsInterface ci, PhoneNotifier notifier,
                        boolean unitTestMode, int phoneId, int precisePhoneType,
                        TelephonyComponentFactory telephonyComponentFactory) {
        this(context, ci, notifier,
                unitTestMode, phoneId, precisePhoneType,
                telephonyComponentFactory,
                ImsManager::getInstance);
    }

    public GsmCdmaPhone(Context context, CommandsInterface ci, PhoneNotifier notifier,
            boolean unitTestMode, int phoneId, int precisePhoneType,
            TelephonyComponentFactory telephonyComponentFactory,
            ImsManagerFactory imsManagerFactory) {
        super(precisePhoneType == PhoneConstants.PHONE_TYPE_GSM ? "GSM" : "CDMA",
                notifier, context, ci, unitTestMode, phoneId, telephonyComponentFactory);

        // phone type needs to be set before other initialization as other objects rely on it
        mPrecisePhoneType = precisePhoneType;
        mVoiceCallSessionStats = new VoiceCallSessionStats(mPhoneId, this);
        mImsManagerFactory = imsManagerFactory;
        initOnce(ci);
        initRatSpecific(precisePhoneType);
        // CarrierSignalAgent uses CarrierActionAgent in construction so it needs to be created
        // after CarrierActionAgent.
        mCarrierActionAgent = mTelephonyComponentFactory.inject(CarrierActionAgent.class.getName())
                .makeCarrierActionAgent(this);
        mCarrierSignalAgent = mTelephonyComponentFactory.inject(CarrierSignalAgent.class.getName())
                .makeCarrierSignalAgent(this);
        mAccessNetworksManager = mTelephonyComponentFactory
                .inject(AccessNetworksManager.class.getName())
                .makeAccessNetworksManager(this, getLooper());
        if (!isUsingNewDataStack()) {
            mTransportManager = mTelephonyComponentFactory.inject(TransportManager.class.getName())
                    .makeTransportManager(this);
        }
        // SST/DSM depends on SSC, so SSC is instanced before SST/DSM
        mSignalStrengthController = mTelephonyComponentFactory.inject(
                SignalStrengthController.class.getName()).makeSignalStrengthController(this);
        mSST = mTelephonyComponentFactory.inject(ServiceStateTracker.class.getName())
                .makeServiceStateTracker(this, this.mCi);
        mEmergencyNumberTracker = mTelephonyComponentFactory
                .inject(EmergencyNumberTracker.class.getName()).makeEmergencyNumberTracker(
                        this, this.mCi);
        if (!isUsingNewDataStack()) {
            mDataEnabledSettings = mTelephonyComponentFactory
                    .inject(DataEnabledSettings.class.getName()).makeDataEnabledSettings(this);
        }
        mDeviceStateMonitor = mTelephonyComponentFactory.inject(DeviceStateMonitor.class.getName())
                .makeDeviceStateMonitor(this);

        // DisplayInfoController creates an OverrideNetworkTypeController, which uses
        // DeviceStateMonitor so needs to be crated after it is instantiated.
        mDisplayInfoController = mTelephonyComponentFactory.inject(
                DisplayInfoController.class.getName()).makeDisplayInfoController(this);

        if (isUsingNewDataStack()) {
            mDataNetworkController = mTelephonyComponentFactory.inject(
                    DataNetworkController.class.getName())
                    .makeDataNetworkController(this, getLooper());
        } else {
            // DcTracker uses ServiceStateTracker and DisplayInfoController so needs to be created
            // after they are instantiated
            for (int transport : mAccessNetworksManager.getAvailableTransports()) {
                DcTracker dcTracker = mTelephonyComponentFactory.inject(DcTracker.class.getName())
                        .makeDcTracker(this, transport);
                mDcTrackers.put(transport, dcTracker);
                mAccessNetworksManager.registerDataThrottler(dcTracker.getDataThrottler());
            }
        }

        mCarrierResolver = mTelephonyComponentFactory.inject(CarrierResolver.class.getName())
                .makeCarrierResolver(this);
        mCarrierPrivilegesTracker = new CarrierPrivilegesTracker(Looper.myLooper(), this, context);

        getCarrierActionAgent().registerForCarrierAction(
                CarrierActionAgent.CARRIER_ACTION_SET_METERED_APNS_ENABLED, this,
                EVENT_SET_CARRIER_DATA_ENABLED, null, false);

        mSST.registerForNetworkAttached(this, EVENT_REGISTERED_TO_NETWORK, null);
        mSST.registerForVoiceRegStateOrRatChanged(this, EVENT_VRS_OR_RAT_CHANGED, null);

        // TODO: Remove SettingsObserver and provisioning events when DataEnabledSettings is removed
        mSettingsObserver = new SettingsObserver(context, this);
        mSettingsObserver.observe(
                Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONED),
                EVENT_DEVICE_PROVISIONED_CHANGE);
        mSettingsObserver.observe(
                Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONING_MOBILE_DATA_ENABLED),
                EVENT_DEVICE_PROVISIONING_DATA_SETTING_CHANGE);

        SubscriptionController.getInstance().registerForUiccAppsEnabled(this,
                EVENT_UICC_APPS_ENABLEMENT_SETTING_CHANGED, null, false);

        mLinkBandwidthEstimator = mTelephonyComponentFactory
                .inject(LinkBandwidthEstimator.class.getName())
                .makeLinkBandwidthEstimator(this);

        loadTtyMode();

        CallManager.getInstance().registerPhone(this);

        mSubscriptionsChangedListener =
                new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                sendEmptyMessage(EVENT_SUBSCRIPTIONS_CHANGED);
            }
        };

        SubscriptionManager subMan = context.getSystemService(SubscriptionManager.class);
        subMan.addOnSubscriptionsChangedListener(
                new HandlerExecutor(this), mSubscriptionsChangedListener);

        logd("GsmCdmaPhone: constructor: sub = " + mPhoneId);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Rlog.d(LOG_TAG, "mBroadcastReceiver: action " + intent.getAction());
            String action = intent.getAction();
            if (CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED.equals(action)) {
                // Only handle carrier config changes for this phone id.
                if (mPhoneId == intent.getIntExtra(CarrierConfigManager.EXTRA_SLOT_INDEX, -1)) {
                    sendMessage(obtainMessage(EVENT_CARRIER_CONFIG_CHANGED));
                }
            } else if (TelecomManager.ACTION_CURRENT_TTY_MODE_CHANGED.equals(action)) {
                int ttyMode = intent.getIntExtra(
                        TelecomManager.EXTRA_CURRENT_TTY_MODE, TelecomManager.TTY_MODE_OFF);
                updateTtyMode(ttyMode);
            } else if (TelecomManager.ACTION_TTY_PREFERRED_MODE_CHANGED.equals(action)) {
                int newPreferredTtyMode = intent.getIntExtra(
                        TelecomManager.EXTRA_TTY_PREFERRED_MODE, TelecomManager.TTY_MODE_OFF);
                updateUiTtyMode(newPreferredTtyMode);
            }
        }
    };

    private void initOnce(CommandsInterface ci) {
        if (ci instanceof SimulatedRadioControl) {
            mSimulatedRadioControl = (SimulatedRadioControl) ci;
        }

        mCT = mTelephonyComponentFactory.inject(GsmCdmaCallTracker.class.getName())
                .makeGsmCdmaCallTracker(this);
        mIccPhoneBookIntManager = mTelephonyComponentFactory
                .inject(IccPhoneBookInterfaceManager.class.getName())
                .makeIccPhoneBookInterfaceManager(this);
        PowerManager pm
                = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
        mIccSmsInterfaceManager = mTelephonyComponentFactory
                .inject(IccSmsInterfaceManager.class.getName())
                .makeIccSmsInterfaceManager(this);

        mCi.registerForAvailable(this, EVENT_RADIO_AVAILABLE, null);
        mCi.registerForOffOrNotAvailable(this, EVENT_RADIO_OFF_OR_NOT_AVAILABLE, null);
        mCi.registerForOn(this, EVENT_RADIO_ON, null);
        mCi.registerForRadioStateChanged(this, EVENT_RADIO_STATE_CHANGED, null);
        mCi.registerUiccApplicationEnablementChanged(this,
                EVENT_UICC_APPS_ENABLEMENT_STATUS_CHANGED,
                null);
        mCi.setOnSuppServiceNotification(this, EVENT_SSN, null);
        mCi.setOnRegistrationFailed(this, EVENT_REGISTRATION_FAILED, null);
        mCi.registerForBarringInfoChanged(this, EVENT_BARRING_INFO_CHANGED, null);

        //GSM
        mCi.setOnUSSD(this, EVENT_USSD, null);
        mCi.setOnSs(this, EVENT_SS, null);

        //CDMA
        mCdmaSSM = mTelephonyComponentFactory.inject(CdmaSubscriptionSourceManager.class.getName())
                .getCdmaSubscriptionSourceManagerInstance(mContext,
                mCi, this, EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED, null);
        mCi.setEmergencyCallbackMode(this, EVENT_EMERGENCY_CALLBACK_MODE_ENTER, null);
        mCi.registerForExitEmergencyCallbackMode(this, EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE,
                null);
        mCi.registerForModemReset(this, EVENT_MODEM_RESET, null);
        // get the string that specifies the carrier OTA Sp number
        mCarrierOtaSpNumSchema = TelephonyManager.from(mContext).getOtaSpNumberSchemaForPhone(
                getPhoneId(), "");

        mResetModemOnRadioTechnologyChange = TelephonyProperties.reset_on_radio_tech_change()
                .orElse(false);

        mCi.registerForRilConnected(this, EVENT_RIL_CONNECTED, null);
        mCi.registerForVoiceRadioTechChanged(this, EVENT_VOICE_RADIO_TECH_CHANGED, null);
        mCi.registerForLceInfo(this, EVENT_LINK_CAPACITY_CHANGED, null);
        mCi.registerForCarrierInfoForImsiEncryption(this,
                EVENT_RESET_CARRIER_KEY_IMSI_ENCRYPTION, null);
        IntentFilter filter = new IntentFilter(
                CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        filter.addAction(TelecomManager.ACTION_CURRENT_TTY_MODE_CHANGED);
        filter.addAction(TelecomManager.ACTION_TTY_PREFERRED_MODE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter,
                android.Manifest.permission.MODIFY_PHONE_STATE, null, Context.RECEIVER_EXPORTED);

        mCDM = new CarrierKeyDownloadManager(this);
        mCIM = new CarrierInfoManager();
    }

    private void initRatSpecific(int precisePhoneType) {
        mPendingMMIs.clear();
        mIccPhoneBookIntManager.updateIccRecords(null);

        mPrecisePhoneType = precisePhoneType;
        logd("Precise phone type " + mPrecisePhoneType);

        TelephonyManager tm = TelephonyManager.from(mContext);
        UiccProfile uiccProfile = getUiccProfile();
        if (isPhoneTypeGsm()) {
            mCi.setPhoneType(PhoneConstants.PHONE_TYPE_GSM);
            tm.setPhoneType(getPhoneId(), PhoneConstants.PHONE_TYPE_GSM);
            if (uiccProfile != null) {
                uiccProfile.setVoiceRadioTech(ServiceState.RIL_RADIO_TECHNOLOGY_UMTS);
            }
        } else {
            mCdmaSubscriptionSource = mCdmaSSM.getCdmaSubscriptionSource();
            // This is needed to handle phone process crashes
            mIsPhoneInEcmState = getInEcmMode();
            if (mIsPhoneInEcmState) {
                // Send a message which will invoke handleExitEmergencyCallbackMode
                mCi.exitEmergencyCallbackMode(null);
            }

            mCi.setPhoneType(PhoneConstants.PHONE_TYPE_CDMA);
            tm.setPhoneType(getPhoneId(), PhoneConstants.PHONE_TYPE_CDMA);
            if (uiccProfile != null) {
                uiccProfile.setVoiceRadioTech(ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT);
            }
            // Sets operator properties by retrieving from build-time system property
            String operatorAlpha = SystemProperties.get("ro.cdma.home.operator.alpha");
            String operatorNumeric = SystemProperties.get(PROPERTY_CDMA_HOME_OPERATOR_NUMERIC);
            logd("init: operatorAlpha='" + operatorAlpha
                    + "' operatorNumeric='" + operatorNumeric + "'");
            if (!TextUtils.isEmpty(operatorAlpha)) {
                logd("init: set 'gsm.sim.operator.alpha' to operator='" + operatorAlpha + "'");
                tm.setSimOperatorNameForPhone(mPhoneId, operatorAlpha);
            }
            if (!TextUtils.isEmpty(operatorNumeric)) {
                logd("init: set 'gsm.sim.operator.numeric' to operator='" + operatorNumeric +
                        "'");
                logd("update icc_operator_numeric=" + operatorNumeric);
                tm.setSimOperatorNumericForPhone(mPhoneId, operatorNumeric);

                SubscriptionController.getInstance().setMccMnc(operatorNumeric, getSubId());

                // Sets iso country property by retrieving from build-time system property
                String iso = "";
                try {
                    iso = MccTable.countryCodeForMcc(operatorNumeric.substring(0, 3));
                } catch (StringIndexOutOfBoundsException ex) {
                    Rlog.e(LOG_TAG, "init: countryCodeForMcc error", ex);
                }

                logd("init: set 'gsm.sim.operator.iso-country' to iso=" + iso);
                tm.setSimCountryIsoForPhone(mPhoneId, iso);
                SubscriptionController.getInstance().setCountryIso(iso, getSubId());

                // Updates MCC MNC device configuration information
                logd("update mccmnc=" + operatorNumeric);
                MccTable.updateMccMncConfiguration(mContext, operatorNumeric);
            }

            // Sets current entry in the telephony carrier table
            updateCurrentCarrierInProvider(operatorNumeric);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isPhoneTypeGsm() {
        return mPrecisePhoneType == PhoneConstants.PHONE_TYPE_GSM;
    }

    public boolean isPhoneTypeCdma() {
        return mPrecisePhoneType == PhoneConstants.PHONE_TYPE_CDMA;
    }

    public boolean isPhoneTypeCdmaLte() {
        return mPrecisePhoneType == PhoneConstants.PHONE_TYPE_CDMA_LTE;
    }

    private void switchPhoneType(int precisePhoneType) {
        removeCallbacks(mExitEcmRunnable);

        initRatSpecific(precisePhoneType);

        mSST.updatePhoneType();
        setPhoneName(precisePhoneType == PhoneConstants.PHONE_TYPE_GSM ? "GSM" : "CDMA");
        onUpdateIccAvailability();
        // if is possible that onUpdateIccAvailability() does not unregister and re-register for
        // ICC events, for example if mUiccApplication does not change which can happen if phone
        // type is transitioning from CDMA to GSM but 3gpp2 application was not available.
        // To handle such cases, unregister and re-register here. They still need to be called in
        // onUpdateIccAvailability(), since in normal cases register/unregister calls can be on
        // different IccRecords objects. Here they are on the same IccRecords object.
        unregisterForIccRecordEvents();
        registerForIccRecordEvents();

        mCT.updatePhoneType();

        int radioState = mCi.getRadioState();
        if (radioState != TelephonyManager.RADIO_POWER_UNAVAILABLE) {
            handleRadioAvailable();
            if (radioState == TelephonyManager.RADIO_POWER_ON) {
                handleRadioOn();
            }
        }
        if (radioState != TelephonyManager.RADIO_POWER_ON) {
            handleRadioOffOrNotAvailable();
        }
    }

    private void updateLinkCapacityEstimate(List<LinkCapacityEstimate> linkCapacityEstimateList) {
        if (DBG) logd("updateLinkCapacityEstimate: lce list=" + linkCapacityEstimateList);
        if (linkCapacityEstimateList == null) {
            return;
        }
        notifyLinkCapacityEstimateChanged(linkCapacityEstimateList);
    }

    @Override
    protected void finalize() {
        if(DBG) logd("GsmCdmaPhone finalized");
        if (mWakeLock != null && mWakeLock.isHeld()) {
            Rlog.e(LOG_TAG, "UNEXPECTED; mWakeLock is held when finalizing.");
            mWakeLock.release();
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    @NonNull
    public ServiceState getServiceState() {
        ServiceState baseSs = mSST != null ? mSST.getServiceState() : new ServiceState();
        ServiceState imsSs = mImsPhone != null ? mImsPhone.getServiceState() : new ServiceState();
        return mergeVoiceServiceStates(baseSs, imsSs, mTelecomVoiceServiceStateOverride);
    }

    @Override
    public void setVoiceServiceStateOverride(boolean hasService) {
        int newOverride =
                hasService ? ServiceState.STATE_IN_SERVICE : ServiceState.STATE_OUT_OF_SERVICE;
        boolean changed = newOverride != mTelecomVoiceServiceStateOverride;
        mTelecomVoiceServiceStateOverride = newOverride;
        if (changed && mSST != null) {
            mSST.onTelecomVoiceServiceStateOverrideChanged();
        }
    }

    @Override
    public void getCellIdentity(WorkSource workSource, Message rspMsg) {
        mSST.requestCellIdentity(workSource, rspMsg);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public PhoneConstants.State getState() {
        if (mImsPhone != null) {
            PhoneConstants.State imsState = mImsPhone.getState();
            if (imsState != PhoneConstants.State.IDLE) {
                return imsState;
            }
        }

        return mCT.mState;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public int getPhoneType() {
        if (mPrecisePhoneType == PhoneConstants.PHONE_TYPE_GSM) {
            return PhoneConstants.PHONE_TYPE_GSM;
        } else {
            return PhoneConstants.PHONE_TYPE_CDMA;
        }
    }

    @Override
    public ServiceStateTracker getServiceStateTracker() {
        return mSST;
    }

    @Override
    public EmergencyNumberTracker getEmergencyNumberTracker() {
        return mEmergencyNumberTracker;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public CallTracker getCallTracker() {
        return mCT;
    }

    @Override
    public TransportManager getTransportManager() {
        return mTransportManager;
    }

    @Override
    public AccessNetworksManager getAccessNetworksManager() {
        return mAccessNetworksManager;
    }

    @Override
    public DeviceStateMonitor getDeviceStateMonitor() {
        return mDeviceStateMonitor;
    }

    @Override
    public DisplayInfoController getDisplayInfoController() {
        return mDisplayInfoController;
    }

    @Override
    public SignalStrengthController getSignalStrengthController() {
        return mSignalStrengthController;
    }

    @Override
    public void updateVoiceMail() {
        if (isPhoneTypeGsm()) {
            int countVoiceMessages = 0;
            IccRecords r = mIccRecords.get();
            if (r != null) {
                // get voice mail count from SIM
                countVoiceMessages = r.getVoiceMessageCount();
            }
            if (countVoiceMessages == IccRecords.DEFAULT_VOICE_MESSAGE_COUNT) {
                countVoiceMessages = getStoredVoiceMessageCount();
            }
            logd("updateVoiceMail countVoiceMessages = " + countVoiceMessages
                    + " subId " + getSubId());
            setVoiceMessageCount(countVoiceMessages);
        } else {
            setVoiceMessageCount(getStoredVoiceMessageCount());
        }
    }

    @Override
    public List<? extends MmiCode>
    getPendingMmiCodes() {
        return mPendingMMIs;
    }

    @Override
    public boolean isDataSuspended() {
        return mCT.mState != PhoneConstants.State.IDLE && !mSST.isConcurrentVoiceAndDataAllowed();
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState(String apnType) {
        PhoneConstants.DataState ret = PhoneConstants.DataState.DISCONNECTED;

        if (mSST == null) {
            // Radio Technology Change is ongoing, dispose() and removeReferences() have
            // already been called

            ret = PhoneConstants.DataState.DISCONNECTED;
        } else if (mSST.getCurrentDataConnectionState() != ServiceState.STATE_IN_SERVICE
                && (isPhoneTypeCdma() || isPhoneTypeCdmaLte() ||
                (isPhoneTypeGsm() && !apnType.equals(ApnSetting.TYPE_EMERGENCY_STRING)))) {
            // If we're out of service, open TCP sockets may still work
            // but no data will flow

            // Emergency APN is available even in Out Of Service
            // Pass the actual State of EPDN

            ret = PhoneConstants.DataState.DISCONNECTED;
        } else { /* mSST.gprsState == ServiceState.STATE_IN_SERVICE */
            int currentTransport = mAccessNetworksManager.getCurrentTransport(
                    ApnSetting.getApnTypesBitmaskFromString(apnType));
            if (getDcTracker(currentTransport) != null) {
                switch (getDcTracker(currentTransport).getState(apnType)) {
                    case CONNECTED:
                    case DISCONNECTING:
                        if (isDataSuspended()) {
                            ret = PhoneConstants.DataState.SUSPENDED;
                        } else {
                            ret = PhoneConstants.DataState.CONNECTED;
                        }
                        break;
                    case CONNECTING:
                        ret = PhoneConstants.DataState.CONNECTING;
                        break;
                    default:
                        ret = PhoneConstants.DataState.DISCONNECTED;
                }
            }
        }

        logd("getDataConnectionState apnType=" + apnType + " ret=" + ret);
        return ret;
    }

    @Override
    public @DataActivityType int getDataActivityState() {
        if (isUsingNewDataStack()) {
            return getDataNetworkController().getDataActivity();
        }
        int ret = TelephonyManager.DATA_ACTIVITY_NONE;

        if (mSST.getCurrentDataConnectionState() == ServiceState.STATE_IN_SERVICE
                && getDcTracker(AccessNetworkConstants.TRANSPORT_TYPE_WWAN) != null) {
            switch (getDcTracker(AccessNetworkConstants.TRANSPORT_TYPE_WWAN).getActivity()) {
                case DATAIN:
                    ret = TelephonyManager.DATA_ACTIVITY_IN;
                break;

                case DATAOUT:
                    ret = TelephonyManager.DATA_ACTIVITY_OUT;
                break;

                case DATAINANDOUT:
                    ret = TelephonyManager.DATA_ACTIVITY_INOUT;
                break;

                case DORMANT:
                    ret = TelephonyManager.DATA_ACTIVITY_DORMANT;
                break;

                default:
                    ret = TelephonyManager.DATA_ACTIVITY_NONE;
                break;
            }
        }

        return ret;
    }

    /**
     * Notify any interested party of a Phone state change
     * {@link com.android.internal.telephony.PhoneConstants.State}
     */
    public void notifyPhoneStateChanged() {
        mNotifier.notifyPhoneState(this);
    }

    /**
     * Notify registrants of a change in the call state. This notifies changes in
     * {@link com.android.internal.telephony.Call.State}. Use this when changes
     * in the precise call state are needed, else use notifyPhoneStateChanged.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void notifyPreciseCallStateChanged() {
        /* we'd love it if this was package-scoped*/
        super.notifyPreciseCallStateChangedP();
    }

    public void notifyNewRingingConnection(Connection c) {
        super.notifyNewRingingConnectionP(c);
    }

    public void notifyDisconnect(Connection cn) {
        mDisconnectRegistrants.notifyResult(cn);

        mNotifier.notifyDisconnectCause(this, cn.getDisconnectCause(),
                cn.getPreciseDisconnectCause());
    }

    public void notifyUnknownConnection(Connection cn) {
        super.notifyUnknownConnectionP(cn);
    }

    @Override
    public boolean isInEmergencyCall() {
        if (isPhoneTypeGsm()) {
            return false;
        } else {
            return mCT.isInEmergencyCall();
        }
    }

    @Override
    protected void setIsInEmergencyCall() {
        if (!isPhoneTypeGsm()) {
            mCT.setIsInEmergencyCall();
        }
    }

    @Override
    public boolean isInEmergencySmsMode() {
        return super.isInEmergencySmsMode()
                || (mImsPhone != null && mImsPhone.isInEmergencySmsMode());
    }

    //CDMA
    private void sendEmergencyCallbackModeChange(){
        //Send an Intent
        Intent intent = new Intent(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        intent.putExtra(TelephonyManager.EXTRA_PHONE_IN_ECM_STATE, isInEcm());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
        mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        logi("sendEmergencyCallbackModeChange");
    }

    @Override
    public void sendEmergencyCallStateChange(boolean callActive) {
        if (!isPhoneTypeCdma()) {
            // It possible that this method got called from ImsPhoneCallTracker#
            logi("sendEmergencyCallStateChange - skip for non-cdma");
            return;
        }
        if (mBroadcastEmergencyCallStateChanges) {
            Intent intent = new Intent(TelephonyIntents.ACTION_EMERGENCY_CALL_STATE_CHANGED);
            intent.putExtra(TelephonyManager.EXTRA_PHONE_IN_EMERGENCY_CALL, callActive);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
            mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            if (DBG) Rlog.d(LOG_TAG, "sendEmergencyCallStateChange: callActive " + callActive);
        }
    }

    @Override
    public void setBroadcastEmergencyCallStateChanges(boolean broadcast) {
        mBroadcastEmergencyCallStateChanges = broadcast;
    }

    public void notifySuppServiceFailed(SuppService code) {
        mSuppServiceFailedRegistrants.notifyResult(code);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void notifyServiceStateChanged(ServiceState ss) {
        super.notifyServiceStateChangedP(ss);
    }

    void notifyServiceStateChangedForSubId(ServiceState ss, int subId) {
        super.notifyServiceStateChangedPForSubId(ss, subId);
    }

    /**
     * Notify that the cell location has changed.
     *
     * @param cellIdentity the new CellIdentity
     */
    public void notifyLocationChanged(CellIdentity cellIdentity) {
        mNotifier.notifyCellLocation(this, cellIdentity);
    }

    @Override
    public void notifyCallForwardingIndicator() {
        mNotifier.notifyCallForwardingChanged(this);
    }

    @Override
    public void registerForSuppServiceNotification(
            Handler h, int what, Object obj) {
        mSsnRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler h) {
        mSsnRegistrants.remove(h);
    }

    @Override
    public void registerForSimRecordsLoaded(Handler h, int what, Object obj) {
        mSimRecordsLoadedRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForSimRecordsLoaded(Handler h) {
        mSimRecordsLoadedRegistrants.remove(h);
    }

    @Override
    public void acceptCall(int videoState) throws CallStateException {
        Phone imsPhone = mImsPhone;
        if ( imsPhone != null && imsPhone.getRingingCall().isRinging() ) {
            imsPhone.acceptCall(videoState);
        } else {
            mCT.acceptCall();
        }
    }

    @Override
    public void rejectCall() throws CallStateException {
        mCT.rejectCall();
    }

    @Override
    public void switchHoldingAndActive() throws CallStateException {
        mCT.switchWaitingOrHoldingAndActive();
    }

    @Override
    public String getIccSerialNumber() {
        IccRecords r = mIccRecords.get();
        if (!isPhoneTypeGsm() && r == null) {
            // to get ICCID form SIMRecords because it is on MF.
            r = mUiccController.getIccRecords(mPhoneId, UiccController.APP_FAM_3GPP);
        }
        return (r != null) ? r.getIccId() : null;
    }

    @Override
    public String getFullIccSerialNumber() {
        IccRecords r = mIccRecords.get();
        if (!isPhoneTypeGsm() && r == null) {
            // to get ICCID form SIMRecords because it is on MF.
            r = mUiccController.getIccRecords(mPhoneId, UiccController.APP_FAM_3GPP);
        }
        return (r != null) ? r.getFullIccId() : null;
    }

    @Override
    public boolean canConference() {
        if (mImsPhone != null && mImsPhone.canConference()) {
            return true;
        }
        if (isPhoneTypeGsm()) {
            return mCT.canConference();
        } else {
            loge("canConference: not possible in CDMA");
            return false;
        }
    }

    @Override
    public void conference() {
        if (mImsPhone != null && mImsPhone.canConference()) {
            logd("conference() - delegated to IMS phone");
            try {
                mImsPhone.conference();
            } catch (CallStateException e) {
                loge(e.toString());
            }
            return;
        }
        if (isPhoneTypeGsm()) {
            mCT.conference();
        } else {
            // three way calls in CDMA will be handled by feature codes
            loge("conference: not possible in CDMA");
        }
    }

    @Override
    public void dispose() {
        // Note: this API is currently never called. We are defining actions here in case
        // we need to dispose GsmCdmaPhone/Phone object.
        super.dispose();
        SubscriptionController.getInstance().unregisterForUiccAppsEnabled(this);
    }

    @Override
    public void enableEnhancedVoicePrivacy(boolean enable, Message onComplete) {
        if (isPhoneTypeGsm()) {
            loge("enableEnhancedVoicePrivacy: not expected on GSM");
        } else {
            mCi.setPreferredVoicePrivacy(enable, onComplete);
        }
    }

    @Override
    public void getEnhancedVoicePrivacy(Message onComplete) {
        if (isPhoneTypeGsm()) {
            loge("getEnhancedVoicePrivacy: not expected on GSM");
        } else {
            mCi.getPreferredVoicePrivacy(onComplete);
        }
    }

    @Override
    public void clearDisconnected() {
        mCT.clearDisconnected();
    }

    @Override
    public boolean canTransfer() {
        if (isPhoneTypeGsm()) {
            return mCT.canTransfer();
        } else {
            loge("canTransfer: not possible in CDMA");
            return false;
        }
    }

    @Override
    public void explicitCallTransfer() {
        if (isPhoneTypeGsm()) {
            mCT.explicitCallTransfer();
        } else {
            loge("explicitCallTransfer: not possible in CDMA");
        }
    }

    @Override
    public GsmCdmaCall getForegroundCall() {
        return mCT.mForegroundCall;
    }

    @Override
    public GsmCdmaCall getBackgroundCall() {
        return mCT.mBackgroundCall;
    }

    @Override
    public Call getRingingCall() {
        Phone imsPhone = mImsPhone;
        // It returns the ringing call of ImsPhone if the ringing call of GSMPhone isn't ringing.
        // In CallManager.registerPhone(), it always registers ringing call of ImsPhone, because
        // the ringing call of GSMPhone isn't ringing. Consequently, it can't answer GSM call
        // successfully by invoking TelephonyManager.answerRingingCall() since the implementation
        // in PhoneInterfaceManager.answerRingingCallInternal() could not get the correct ringing
        // call from CallManager. So we check the ringing call state of imsPhone first as
        // accpetCall() does.
        if ( imsPhone != null && imsPhone.getRingingCall().isRinging()) {
            return imsPhone.getRingingCall();
        }
        //It returns the ringing connections which during SRVCC handover
        if (!mCT.mRingingCall.isRinging()
                && mCT.getRingingHandoverConnection() != null
                && mCT.getRingingHandoverConnection().getCall() != null
                && mCT.getRingingHandoverConnection().getCall().isRinging()) {
            return mCT.getRingingHandoverConnection().getCall();
        }
        return mCT.mRingingCall;
    }

    @Override
    @NonNull
    public CarrierPrivilegesTracker getCarrierPrivilegesTracker() {
        return mCarrierPrivilegesTracker;
    }

    /**
     * Amends {@code baseSs} if its voice registration state is {@code OUT_OF_SERVICE}.
     *
     * <p>Even if the device has lost the CS link to the tower, there are two potential additional
     * sources of voice capability not directly saved inside ServiceStateTracker:
     *
     * <ul>
     *   <li>IMS voice registration state ({@code imsSs}) - if this is {@code IN_SERVICE} for voice,
     *       we substite {@code baseSs#getDataRegState} as the final voice service state (ImsService
     *       reports {@code IN_SERVICE} for its voice registration state even if the device has lost
     *       the physical link to the tower)
     *   <li>OTT voice capability provided through telecom ({@code telecomSs}) - if this is {@code
     *       IN_SERVICE}, we directly substitute it as the final voice service state
     * </ul>
     */
    private static ServiceState mergeVoiceServiceStates(
            ServiceState baseSs, ServiceState imsSs, @ServiceState.RegState int telecomSs) {
        if (baseSs.getState() == ServiceState.STATE_IN_SERVICE) {
            // No need to merge states if the baseSs is IN_SERVICE.
            return baseSs;
        }
        // If any of the following additional sources are IN_SERVICE, we use that since voice calls
        // can be routed through something other than the CS link.
        @ServiceState.RegState int finalVoiceSs = ServiceState.STATE_OUT_OF_SERVICE;
        if (telecomSs == ServiceState.STATE_IN_SERVICE) {
            // If telecom reports there's a PhoneAccount that can provide voice service
            // (CAPABILITY_VOICE_CALLING_AVAILABLE), then we trust that info as it may account for
            // external possibilities like wi-fi calling provided by the SIM call manager app. Note
            // that CAPABILITY_PLACE_EMERGENCY_CALLS is handled separately.
            finalVoiceSs = telecomSs;
        } else if (imsSs.getState() == ServiceState.STATE_IN_SERVICE) {
            // Voice override for IMS case. In this case, voice registration is OUT_OF_SERVICE, but
            // IMS is available, so use data registration state as a basis for determining
            // whether or not the physical link is available.
            finalVoiceSs = baseSs.getDataRegistrationState();
        }
        if (finalVoiceSs != ServiceState.STATE_IN_SERVICE) {
            // None of the additional sources provide a usable route, and they only use IN/OUT.
            return baseSs;
        }
        ServiceState newSs = new ServiceState(baseSs);
        newSs.setVoiceRegState(finalVoiceSs);
        newSs.setEmergencyOnly(false); // Must be IN_SERVICE if we're here
        return newSs;
    }

    private boolean handleCallDeflectionIncallSupplementaryService(
            String dialString) {
        if (dialString.length() > 1) {
            return false;
        }

        if (getRingingCall().getState() != GsmCdmaCall.State.IDLE) {
            if (DBG) logd("MmiCode 0: rejectCall");
            try {
                mCT.rejectCall();
            } catch (CallStateException e) {
                if (DBG) Rlog.d(LOG_TAG,
                        "reject failed", e);
                notifySuppServiceFailed(Phone.SuppService.REJECT);
            }
        } else if (getBackgroundCall().getState() != GsmCdmaCall.State.IDLE) {
            if (DBG) logd("MmiCode 0: hangupWaitingOrBackground");
            mCT.hangupWaitingOrBackground();
        }

        return true;
    }

    //GSM
    private boolean handleCallWaitingIncallSupplementaryService(String dialString) {
        int len = dialString.length();

        if (len > 2) {
            return false;
        }

        GsmCdmaCall call = getForegroundCall();

        try {
            if (len > 1) {
                char ch = dialString.charAt(1);
                int callIndex = ch - '0';

                if (callIndex >= 1 && callIndex <= GsmCdmaCallTracker.MAX_CONNECTIONS_GSM) {
                    if (DBG) logd("MmiCode 1: hangupConnectionByIndex " + callIndex);
                    mCT.hangupConnectionByIndex(call, callIndex);
                }
            } else {
                if (call.getState() != GsmCdmaCall.State.IDLE) {
                    if (DBG) logd("MmiCode 1: hangup foreground");
                    //mCT.hangupForegroundResumeBackground();
                    mCT.hangup(call);
                } else {
                    if (DBG) logd("MmiCode 1: switchWaitingOrHoldingAndActive");
                    mCT.switchWaitingOrHoldingAndActive();
                }
            }
        } catch (CallStateException e) {
            if (DBG) Rlog.d(LOG_TAG,
                    "hangup failed", e);
            notifySuppServiceFailed(Phone.SuppService.HANGUP);
        }

        return true;
    }

    private boolean handleCallHoldIncallSupplementaryService(String dialString) {
        int len = dialString.length();

        if (len > 2) {
            return false;
        }

        GsmCdmaCall call = getForegroundCall();

        if (len > 1) {
            try {
                char ch = dialString.charAt(1);
                int callIndex = ch - '0';
                GsmCdmaConnection conn = mCT.getConnectionByIndex(call, callIndex);

                // GsmCdma index starts at 1, up to 5 connections in a call,
                if (conn != null && callIndex >= 1 && callIndex <= GsmCdmaCallTracker.MAX_CONNECTIONS_GSM) {
                    if (DBG) logd("MmiCode 2: separate call " + callIndex);
                    mCT.separate(conn);
                } else {
                    if (DBG) logd("separate: invalid call index " + callIndex);
                    notifySuppServiceFailed(Phone.SuppService.SEPARATE);
                }
            } catch (CallStateException e) {
                if (DBG) Rlog.d(LOG_TAG, "separate failed", e);
                notifySuppServiceFailed(Phone.SuppService.SEPARATE);
            }
        } else {
            try {
                if (getRingingCall().getState() != GsmCdmaCall.State.IDLE) {
                    if (DBG) logd("MmiCode 2: accept ringing call");
                    mCT.acceptCall();
                } else {
                    if (DBG) logd("MmiCode 2: switchWaitingOrHoldingAndActive");
                    mCT.switchWaitingOrHoldingAndActive();
                }
            } catch (CallStateException e) {
                if (DBG) Rlog.d(LOG_TAG, "switch failed", e);
                notifySuppServiceFailed(Phone.SuppService.SWITCH);
            }
        }

        return true;
    }

    private boolean handleMultipartyIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return false;
        }

        if (DBG) logd("MmiCode 3: merge calls");
        conference();
        return true;
    }

    private boolean handleEctIncallSupplementaryService(String dialString) {

        int len = dialString.length();

        if (len != 1) {
            return false;
        }

        if (DBG) logd("MmiCode 4: explicit call transfer");
        explicitCallTransfer();
        return true;
    }

    private boolean handleCcbsIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return false;
        }

        Rlog.i(LOG_TAG, "MmiCode 5: CCBS not supported!");
        // Treat it as an "unknown" service.
        notifySuppServiceFailed(Phone.SuppService.UNKNOWN);
        return true;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public boolean handleInCallMmiCommands(String dialString) throws CallStateException {
        if (!isPhoneTypeGsm()) {
            loge("method handleInCallMmiCommands is NOT supported in CDMA!");
            return false;
        }

        Phone imsPhone = mImsPhone;
        if (imsPhone != null
                && imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE) {
            return imsPhone.handleInCallMmiCommands(dialString);
        }

        if (!isInCall()) {
            return false;
        }

        if (TextUtils.isEmpty(dialString)) {
            return false;
        }

        boolean result = false;
        char ch = dialString.charAt(0);
        switch (ch) {
            case '0':
                result = handleCallDeflectionIncallSupplementaryService(dialString);
                break;
            case '1':
                result = handleCallWaitingIncallSupplementaryService(dialString);
                break;
            case '2':
                result = handleCallHoldIncallSupplementaryService(dialString);
                break;
            case '3':
                result = handleMultipartyIncallSupplementaryService(dialString);
                break;
            case '4':
                result = handleEctIncallSupplementaryService(dialString);
                break;
            case '5':
                result = handleCcbsIncallSupplementaryService(dialString);
                break;
            default:
                break;
        }

        return result;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isInCall() {
        GsmCdmaCall.State foregroundCallState = getForegroundCall().getState();
        GsmCdmaCall.State backgroundCallState = getBackgroundCall().getState();
        GsmCdmaCall.State ringingCallState = getRingingCall().getState();

       return (foregroundCallState.isAlive() ||
                backgroundCallState.isAlive() ||
                ringingCallState.isAlive());
    }

    private boolean useImsForCall(DialArgs dialArgs) {
        return isImsUseEnabled()
                && mImsPhone != null
                && (mImsPhone.isVoiceOverCellularImsEnabled() || mImsPhone.isWifiCallingEnabled()
                || (mImsPhone.isVideoEnabled() && VideoProfile.isVideo(dialArgs.videoState)))
                && (mImsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE);
    }

    public boolean useImsForEmergency() {
        CarrierConfigManager configManager =
                (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        boolean alwaysTryImsForEmergencyCarrierConfig = configManager.getConfigForSubId(getSubId())
                .getBoolean(CarrierConfigManager.KEY_CARRIER_USE_IMS_FIRST_FOR_EMERGENCY_BOOL);
        return mImsPhone != null
                && alwaysTryImsForEmergencyCarrierConfig
                && ImsManager.getInstance(mContext, mPhoneId).isNonTtyOrTtyOnVolteEnabled()
                && mImsPhone.isImsAvailable();
    }

    @Override
    public Connection startConference(String[] participantsToDial, DialArgs dialArgs)
            throws CallStateException {
        Phone imsPhone = mImsPhone;
        boolean useImsForCall = useImsForCall(dialArgs);
        logd("useImsForCall=" + useImsForCall);
        if (useImsForCall) {
            try {
                if (DBG) logd("Trying IMS PS Conference call");
                return imsPhone.startConference(participantsToDial, dialArgs);
            } catch (CallStateException e) {
                if (DBG) logd("IMS PS conference call exception " + e +
                        "useImsForCall =" + useImsForCall + ", imsPhone =" + imsPhone);
                 CallStateException ce = new CallStateException(e.getError(), e.getMessage());
                 ce.setStackTrace(e.getStackTrace());
                 throw ce;
            }
        } else {
            throw new CallStateException(
                CallStateException.ERROR_OUT_OF_SERVICE,
                "cannot dial conference call in out of service");
        }
    }

    @Override
    public Connection dial(String dialString, @NonNull DialArgs dialArgs,
            Consumer<Phone> chosenPhoneConsumer) throws CallStateException {
        if (!isPhoneTypeGsm() && dialArgs.uusInfo != null) {
            throw new CallStateException("Sending UUS information NOT supported in CDMA!");
        }
        String possibleEmergencyNumber = checkForTestEmergencyNumber(dialString);
        // Record if the dialed number was swapped for a test emergency number.
        boolean isDialedNumberSwapped = !TextUtils.equals(dialString, possibleEmergencyNumber);
        if (isDialedNumberSwapped) {
            logi("dialString replaced for possible emergency number: " + dialString + " -> "
                    + possibleEmergencyNumber);
            dialString = possibleEmergencyNumber;
        }

        CarrierConfigManager configManager =
                (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle carrierConfig = configManager.getConfigForSubId(getSubId());
        boolean allowWpsOverIms = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_SUPPORT_WPS_OVER_IMS_BOOL);
        boolean useOnlyDialedSimEccList = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_USE_ONLY_DIALED_SIM_ECC_LIST_BOOL);


        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        boolean isEmergency;
        // Check if the carrier wants to treat a call as an emergency call based on its own list of
        // known emergency numbers.
        // useOnlyDialedSimEccList is false for the vast majority of carriers.  There are, however,
        // some carriers which do not want to handle dial requests for numbers which are in the
        // emergency number list on another SIM, but is not on theirs.  In this case we will use the
        // emergency number list for this carrier's SIM only.
        if (useOnlyDialedSimEccList) {
            isEmergency = getEmergencyNumberTracker().isEmergencyNumber(dialString,
                    true /* exactMatch */);
            logi("dial; isEmergency=" + isEmergency
                    + " (based on this phone only); globalIsEmergency="
                    + tm.isEmergencyNumber(dialString));
        } else {
            isEmergency = tm.isEmergencyNumber(dialString);
            logi("dial; isEmergency=" + isEmergency + " (based on all phones)");
        }

        /** Check if the call is Wireless Priority Service call */
        boolean isWpsCall = dialString != null ? (dialString.startsWith(PREFIX_WPS)
                || dialString.startsWith(PREFIX_WPS_CLIR_ACTIVATE)
                || dialString.startsWith(PREFIX_WPS_CLIR_DEACTIVATE)) : false;

        ImsPhone.ImsDialArgs.Builder imsDialArgsBuilder;
        imsDialArgsBuilder = ImsPhone.ImsDialArgs.Builder.from(dialArgs)
                                                 .setIsEmergency(isEmergency)
                                                 .setIsWpsCall(isWpsCall);
        mDialArgs = dialArgs = imsDialArgsBuilder.build();

        Phone imsPhone = mImsPhone;

        boolean useImsForEmergency = isEmergency && useImsForEmergency();

        String dialPart = PhoneNumberUtils.extractNetworkPortionAlt(PhoneNumberUtils.
                stripSeparators(dialString));
        boolean isMmiCode = (dialPart.startsWith("*") || dialPart.startsWith("#"))
                && dialPart.endsWith("#");
        boolean isSuppServiceCode = ImsPhoneMmiCode.isSuppServiceCodes(dialPart, this);
        boolean isPotentialUssdCode = isMmiCode && !isSuppServiceCode;
        boolean useImsForUt = imsPhone != null && imsPhone.isUtEnabled();
        boolean useImsForCall = useImsForCall(dialArgs)
                && (isWpsCall ? allowWpsOverIms : true);

        if (DBG) {
            logi("useImsForCall=" + useImsForCall
                    + ", useOnlyDialedSimEccList=" + useOnlyDialedSimEccList
                    + ", isEmergency=" + isEmergency
                    + ", useImsForEmergency=" + useImsForEmergency
                    + ", useImsForUt=" + useImsForUt
                    + ", isUt=" + isMmiCode
                    + ", isSuppServiceCode=" + isSuppServiceCode
                    + ", isPotentialUssdCode=" + isPotentialUssdCode
                    + ", isWpsCall=" + isWpsCall
                    + ", allowWpsOverIms=" + allowWpsOverIms
                    + ", imsPhone=" + imsPhone
                    + ", imsPhone.isVoiceOverCellularImsEnabled()="
                    + ((imsPhone != null) ? imsPhone.isVoiceOverCellularImsEnabled() : "N/A")
                    + ", imsPhone.isVowifiEnabled()="
                    + ((imsPhone != null) ? imsPhone.isWifiCallingEnabled() : "N/A")
                    + ", imsPhone.isVideoEnabled()="
                    + ((imsPhone != null) ? imsPhone.isVideoEnabled() : "N/A")
                    + ", imsPhone.getServiceState().getState()="
                    + ((imsPhone != null) ? imsPhone.getServiceState().getState() : "N/A"));
        }

        // Bypass WiFi Only WFC check if this is an emergency call - we should still try to
        // place over cellular if possible.
        if (!isEmergency) {
            Phone.checkWfcWifiOnlyModeBeforeDial(mImsPhone, mPhoneId, mContext);
        }
        if (imsPhone != null && !allowWpsOverIms && !useImsForCall && isWpsCall
                && imsPhone.getCallTracker() instanceof ImsPhoneCallTracker) {
            logi("WPS call placed over CS; disconnecting all IMS calls..");
            ImsPhoneCallTracker tracker = (ImsPhoneCallTracker) imsPhone.getCallTracker();
            tracker.hangupAllConnections();
        }

        if ((useImsForCall && (!isMmiCode || isPotentialUssdCode))
                || (isMmiCode && useImsForUt)
                || useImsForEmergency) {
            try {
                if (DBG) logd("Trying IMS PS call");
                chosenPhoneConsumer.accept(imsPhone);
                return imsPhone.dial(dialString, dialArgs);
            } catch (CallStateException e) {
                if (DBG) logd("IMS PS call exception " + e +
                        "useImsForCall =" + useImsForCall + ", imsPhone =" + imsPhone);
                // Do not throw a CallStateException and instead fall back to Circuit switch
                // for emergency calls and MMI codes.
                if (Phone.CS_FALLBACK.equals(e.getMessage()) || isEmergency) {
                    logi("IMS call failed with Exception: " + e.getMessage() + ". Falling back "
                            + "to CS.");
                } else {
                    CallStateException ce = new CallStateException(e.getError(), e.getMessage());
                    ce.setStackTrace(e.getStackTrace());
                    throw ce;
                }
            }
        }

        if (mSST != null && mSST.mSS.getState() == ServiceState.STATE_OUT_OF_SERVICE
                && mSST.mSS.getDataRegistrationState() != ServiceState.STATE_IN_SERVICE
                && !isEmergency) {
            throw new CallStateException("cannot dial in current state");
        }
        // Check non-emergency voice CS call - shouldn't dial when POWER_OFF
        if (mSST != null && mSST.mSS.getState() == ServiceState.STATE_POWER_OFF /* CS POWER_OFF */
                && !VideoProfile.isVideo(dialArgs.videoState) /* voice call */
                && !isEmergency /* non-emergency call */
                && !(isMmiCode && useImsForUt) /* not UT */
                /* If config_allow_ussd_over_ims is false, USSD is sent over the CS pipe instead */
                && !isPotentialUssdCode) {
            throw new CallStateException(
                CallStateException.ERROR_POWER_OFF,
                "cannot dial voice call in airplane mode");
        }
        // Check for service before placing non emergency CS voice call.
        // Allow dial only if either CS is camped on any RAT (or) PS is in LTE/NR service.
        if (mSST != null
                && mSST.mSS.getState() == ServiceState.STATE_OUT_OF_SERVICE /* CS out of service */
                && !(mSST.mSS.getDataRegistrationState() == ServiceState.STATE_IN_SERVICE
                && ServiceState.isPsOnlyTech(
                        mSST.mSS.getRilDataRadioTechnology())) /* PS not in LTE/NR */
                && !VideoProfile.isVideo(dialArgs.videoState) /* voice call */
                && !isEmergency /* non-emergency call */
                /* If config_allow_ussd_over_ims is false, USSD is sent over the CS pipe instead */
                && !isPotentialUssdCode) {
            throw new CallStateException(
                CallStateException.ERROR_OUT_OF_SERVICE,
                "cannot dial voice call in out of service");
        }
        if (DBG) logd("Trying (non-IMS) CS call");
        if (isDialedNumberSwapped && isEmergency) {
            // Triggers ECM when CS call ends only for test emergency calls using
            // ril.test.emergencynumber.
            mIsTestingEmergencyCallbackMode = true;
            mCi.testingEmergencyCall();
        }

        chosenPhoneConsumer.accept(this);
        return dialInternal(dialString, dialArgs);
    }

    /**
     * @return {@code true} if the user should be informed of an attempt to dial an international
     * number while on WFC only, {@code false} otherwise.
     */
    public boolean isNotificationOfWfcCallRequired(String dialString) {
        CarrierConfigManager configManager =
                (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle config = configManager.getConfigForSubId(getSubId());

        // Determine if carrier config indicates that international calls over WFC should trigger a
        // notification to the user. This is controlled by carrier configuration and is off by
        // default.
        boolean shouldNotifyInternationalCallOnWfc = config != null
                && config.getBoolean(
                        CarrierConfigManager.KEY_NOTIFY_INTERNATIONAL_CALL_ON_WFC_BOOL);

        if (!shouldNotifyInternationalCallOnWfc) {
            return false;
        }

        Phone imsPhone = mImsPhone;
        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        boolean isEmergency = tm.isEmergencyNumber(dialString);
        boolean shouldConfirmCall =
                        // Using IMS
                        isImsUseEnabled()
                        && imsPhone != null
                        // VoLTE not available
                        && !imsPhone.isVoiceOverCellularImsEnabled()
                        // WFC is available
                        && imsPhone.isWifiCallingEnabled()
                        && !isEmergency
                        // Dialing international number
                        && PhoneNumberUtils.isInternationalNumber(dialString, getCountryIso());
        return shouldConfirmCall;
    }

    @Override
    protected Connection dialInternal(String dialString, DialArgs dialArgs)
            throws CallStateException {
        return dialInternal(dialString, dialArgs, null);
    }

    protected Connection dialInternal(String dialString, DialArgs dialArgs,
            ResultReceiver wrappedCallback)
            throws CallStateException {

        // Need to make sure dialString gets parsed properly
        String newDialString = PhoneNumberUtils.stripSeparators(dialString);

        if (isPhoneTypeGsm()) {
            // handle in-call MMI first if applicable
            if (handleInCallMmiCommands(newDialString)) {
                return null;
            }

            // Only look at the Network portion for mmi
            String networkPortion = PhoneNumberUtils.extractNetworkPortionAlt(newDialString);
            GsmMmiCode mmi = GsmMmiCode.newFromDialString(networkPortion, this,
                    mUiccApplication.get(), wrappedCallback);
            if (DBG) logd("dialInternal: dialing w/ mmi '" + mmi + "'...");

            if (mmi == null) {
                return mCT.dialGsm(newDialString, dialArgs);
            } else if (mmi.isTemporaryModeCLIR()) {
                return mCT.dialGsm(mmi.mDialingNumber, mmi.getCLIRMode(), dialArgs.uusInfo,
                        dialArgs.intentExtras);
            } else {
                mPendingMMIs.add(mmi);
                mMmiRegistrants.notifyRegistrants(new AsyncResult(null, mmi, null));
                mmi.processCode();
                return null;
            }
        } else {
            return mCT.dial(newDialString, dialArgs);
        }
    }

   @Override
    public boolean handlePinMmi(String dialString) {
        MmiCode mmi;
        if (isPhoneTypeGsm()) {
            mmi = GsmMmiCode.newFromDialString(dialString, this, mUiccApplication.get());
        } else {
            mmi = CdmaMmiCode.newFromDialString(dialString, this, mUiccApplication.get());
        }

        if (mmi != null && mmi.isPinPukCommand()) {
            mPendingMMIs.add(mmi);
            mMmiRegistrants.notifyRegistrants(new AsyncResult(null, mmi, null));
            try {
                mmi.processCode();
            } catch (CallStateException e) {
                //do nothing
            }
            return true;
        }

        loge("Mmi is null or unrecognized!");
        return false;
    }

    private void sendUssdResponse(String ussdRequest, CharSequence message, int returnCode,
                                   ResultReceiver wrappedCallback) {
        UssdResponse response = new UssdResponse(ussdRequest, message);
        Bundle returnData = new Bundle();
        returnData.putParcelable(TelephonyManager.USSD_RESPONSE, response);
        wrappedCallback.send(returnCode, returnData);
    }

    @Override
    public boolean handleUssdRequest(String ussdRequest, ResultReceiver wrappedCallback) {
        if (!isPhoneTypeGsm() || mPendingMMIs.size() > 0) {
            //todo: replace the generic failure with specific error code.
            sendUssdResponse(ussdRequest, null, TelephonyManager.USSD_RETURN_FAILURE,
                    wrappedCallback );
            return true;
        }

        // Try over IMS if possible.
        Phone imsPhone = mImsPhone;
        if ((imsPhone != null)
                && ((imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)
                || imsPhone.isUtEnabled())) {
            try {
                logd("handleUssdRequest: attempting over IMS");
                return imsPhone.handleUssdRequest(ussdRequest, wrappedCallback);
            } catch (CallStateException cse) {
                if (!CS_FALLBACK.equals(cse.getMessage())) {
                    return false;
                }
                // At this point we've tried over IMS but have been informed we need to handover
                // back to GSM.
                logd("handleUssdRequest: fallback to CS required");
            }
        }

        // Try USSD over GSM.
        try {
            dialInternal(ussdRequest, new DialArgs.Builder<>().build(), wrappedCallback);
        } catch (Exception e) {
            logd("handleUssdRequest: exception" + e);
            return false;
        }
        return true;
    }

    @Override
    public void sendUssdResponse(String ussdMessge) {
        if (isPhoneTypeGsm()) {
            GsmMmiCode mmi = GsmMmiCode.newFromUssdUserInput(ussdMessge, this, mUiccApplication.get());
            mPendingMMIs.add(mmi);
            mMmiRegistrants.notifyRegistrants(new AsyncResult(null, mmi, null));
            mmi.sendUssd(ussdMessge);
        } else {
            loge("sendUssdResponse: not possible in CDMA");
        }
    }

    @Override
    public void sendDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("sendDtmf called with invalid character '" + c + "'");
        } else {
            if (mCT.mState ==  PhoneConstants.State.OFFHOOK) {
                mCi.sendDtmf(c, null);
            }
        }
    }

    @Override
    public void startDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("startDtmf called with invalid character '" + c + "'");
        } else {
            mCi.startDtmf(c, null);
        }
    }

    @Override
    public void stopDtmf() {
        mCi.stopDtmf(null);
    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message onComplete) {
        if (isPhoneTypeGsm()) {
            loge("[GsmCdmaPhone] sendBurstDtmf() is a CDMA method");
        } else {
            boolean check = true;
            for (int itr = 0;itr < dtmfString.length(); itr++) {
                if (!PhoneNumberUtils.is12Key(dtmfString.charAt(itr))) {
                    Rlog.e(LOG_TAG,
                            "sendDtmf called with invalid character '" + dtmfString.charAt(itr)+ "'");
                    check = false;
                    break;
                }
            }
            if (mCT.mState == PhoneConstants.State.OFFHOOK && check) {
                mCi.sendBurstDtmf(dtmfString, on, off, onComplete);
            }
        }
    }

    @Override
    public void setRadioPowerOnForTestEmergencyCall(boolean isSelectedPhoneForEmergencyCall) {
        mSST.clearAllRadioOffReasons();

        // We don't want to have forEmergency call be true to prevent radio emergencyDial command
        // from being called for a test emergency number because the network may not be able to
        // find emergency routing for it and dial it do the default emergency services line.
        setRadioPower(true, false, isSelectedPhoneForEmergencyCall, false);
    }

    @Override
    public void setRadioPower(boolean power, boolean forEmergencyCall,
            boolean isSelectedPhoneForEmergencyCall, boolean forceApply) {
        setRadioPowerForReason(power, forEmergencyCall, isSelectedPhoneForEmergencyCall, forceApply,
                Phone.RADIO_POWER_REASON_USER);
    }

    @Override
    public void setRadioPowerForReason(boolean power, boolean forEmergencyCall,
            boolean isSelectedPhoneForEmergencyCall, boolean forceApply, int reason) {
        mSST.setRadioPowerForReason(power, forEmergencyCall, isSelectedPhoneForEmergencyCall,
                forceApply, reason);
    }

    private void storeVoiceMailNumber(String number) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        setVmSimImsi(getSubscriberId());
        logd("storeVoiceMailNumber: mPrecisePhoneType=" + mPrecisePhoneType + " vmNumber="
                + Rlog.pii(LOG_TAG, number));
        if (isPhoneTypeGsm()) {
            editor.putString(VM_NUMBER + getPhoneId(), number);
            editor.apply();
        } else {
            editor.putString(VM_NUMBER_CDMA + getPhoneId(), number);
            editor.apply();
        }
    }

    @Override
    public String getVoiceMailNumber() {
        String number = null;
        if (isPhoneTypeGsm() || mSimRecords != null) {
            // Read from the SIM. If its null, try reading from the shared preference area.
            IccRecords r = isPhoneTypeGsm() ? mIccRecords.get() : mSimRecords;
            number = (r != null) ? r.getVoiceMailNumber() : "";
            if (TextUtils.isEmpty(number)) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
                String spName = isPhoneTypeGsm() ? VM_NUMBER : VM_NUMBER_CDMA;
                number = sp.getString(spName + getPhoneId(), null);
                logd("getVoiceMailNumber: from " + spName + " number="
                        + Rlog.pii(LOG_TAG, number));
            } else {
                logd("getVoiceMailNumber: from IccRecords number=" + Rlog.pii(LOG_TAG, number));
            }
        }
        if (!isPhoneTypeGsm() && TextUtils.isEmpty(number)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            number = sp.getString(VM_NUMBER_CDMA + getPhoneId(), null);
            logd("getVoiceMailNumber: from VM_NUMBER_CDMA number=" + number);
        }

        if (TextUtils.isEmpty(number)) {
            CarrierConfigManager configManager = (CarrierConfigManager)
                    getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle b = configManager.getConfigForSubId(getSubId());
            if (b != null) {
                String defaultVmNumber =
                        b.getString(CarrierConfigManager.KEY_DEFAULT_VM_NUMBER_STRING);
                String defaultVmNumberRoaming =
                        b.getString(CarrierConfigManager.KEY_DEFAULT_VM_NUMBER_ROAMING_STRING);
                String defaultVmNumberRoamingAndImsUnregistered = b.getString(
                        CarrierConfigManager
                                .KEY_DEFAULT_VM_NUMBER_ROAMING_AND_IMS_UNREGISTERED_STRING);

                if (!TextUtils.isEmpty(defaultVmNumber)) number = defaultVmNumber;
                if (mSST.mSS.getRoaming()) {
                    if (!TextUtils.isEmpty(defaultVmNumberRoamingAndImsUnregistered)
                            && !mSST.isImsRegistered()) {
                        // roaming and IMS unregistered case if CC configured
                        number = defaultVmNumberRoamingAndImsUnregistered;
                    } else if (!TextUtils.isEmpty(defaultVmNumberRoaming)) {
                        // roaming default case if CC configured
                        number = defaultVmNumberRoaming;
                    }
                }
            }
        }

        if (TextUtils.isEmpty(number)) {
            // Read platform settings for dynamic voicemail number
            CarrierConfigManager configManager = (CarrierConfigManager)
                    getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle b = configManager.getConfigForSubId(getSubId());
            if (b != null && b.getBoolean(
                    CarrierConfigManager.KEY_CONFIG_TELEPHONY_USE_OWN_NUMBER_FOR_VOICEMAIL_BOOL)) {
                number = getLine1Number();
            }
        }

        return number;
    }

    private String getVmSimImsi() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sp.getString(VM_SIM_IMSI + getPhoneId(), null);
    }

    private void setVmSimImsi(String imsi) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VM_SIM_IMSI + getPhoneId(), imsi);
        editor.apply();
    }

    @Override
    public String getVoiceMailAlphaTag() {
        String ret = "";

        if (isPhoneTypeGsm() || mSimRecords != null) {
            IccRecords r = isPhoneTypeGsm() ? mIccRecords.get() : mSimRecords;

            ret = (r != null) ? r.getVoiceMailAlphaTag() : "";
        }

        if (ret == null || ret.length() == 0) {
            return mContext.getText(
                com.android.internal.R.string.defaultVoiceMailAlphaTag).toString();
        }

        return ret;
    }

    @Override
    public String getDeviceId() {
        if (isPhoneTypeGsm()) {
            return mImei;
        } else {
            CarrierConfigManager configManager = (CarrierConfigManager)
                    mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            boolean force_imei = configManager.getConfigForSubId(getSubId())
                    .getBoolean(CarrierConfigManager.KEY_FORCE_IMEI_BOOL);
            if (force_imei) return mImei;

            String id = getMeid();
            if ((id == null) || id.matches("^0*$")) {
                loge("getDeviceId(): MEID is not initialized use ESN");
                id = getEsn();
            }
            return id;
        }
    }

    @Override
    public String getDeviceSvn() {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            return mImeiSv;
        } else {
            loge("getDeviceSvn(): return 0");
            return "0";
        }
    }

    @Override
    public IsimRecords getIsimRecords() {
        return mIsimUiccRecords;
    }

    @Override
    public String getImei() {
        return mImei;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public String getEsn() {
        if (isPhoneTypeGsm()) {
            loge("[GsmCdmaPhone] getEsn() is a CDMA method");
            return "0";
        } else {
            return mEsn;
        }
    }

    @Override
    public String getMeid() {
        return mMeid;
    }

    @Override
    public String getNai() {
        IccRecords r = mUiccController.getIccRecords(mPhoneId, UiccController.APP_FAM_3GPP2);
        if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
            Rlog.v(LOG_TAG, "IccRecords is " + r);
        }
        return (r != null) ? r.getNAI() : null;
    }

    @Override
    @Nullable
    public String getSubscriberId() {
        String subscriberId = null;
        if (isPhoneTypeCdma()) {
            subscriberId = mSST.getImsi();
        } else {
            // Both Gsm and CdmaLte get the IMSI from Usim.
            IccRecords iccRecords = mUiccController.getIccRecords(
                    mPhoneId, UiccController.APP_FAM_3GPP);
            if (iccRecords != null) {
                subscriberId = iccRecords.getIMSI();
            }
        }
        return subscriberId;
    }

    @Override
    public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int keyType, boolean fallback) {
        final TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(getSubId());
        String operatorNumeric = telephonyManager.getSimOperator();
        int carrierId = telephonyManager.getSimCarrierId();
        return CarrierInfoManager.getCarrierInfoForImsiEncryption(keyType,
                mContext, operatorNumeric, carrierId, fallback, getSubId());
    }

    @Override
    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo) {
        CarrierInfoManager.setCarrierInfoForImsiEncryption(imsiEncryptionInfo, mContext, mPhoneId);
        mCi.setCarrierInfoForImsiEncryption(imsiEncryptionInfo, null);
    }

    @Override
    public void deleteCarrierInfoForImsiEncryption(int carrierId) {
        CarrierInfoManager.deleteCarrierInfoForImsiEncryption(mContext, getSubId(),
                carrierId);
    }

    @Override
    public int getCarrierId() {
        return mCarrierResolver != null
                ? mCarrierResolver.getCarrierId() : super.getCarrierId();
    }

    @Override
    public String getCarrierName() {
        return mCarrierResolver != null
                ? mCarrierResolver.getCarrierName() : super.getCarrierName();
    }

    @Override
    public int getMNOCarrierId() {
        return mCarrierResolver != null
                ? mCarrierResolver.getMnoCarrierId() : super.getMNOCarrierId();
    }

    @Override
    public int getSpecificCarrierId() {
        return mCarrierResolver != null
                ? mCarrierResolver.getSpecificCarrierId() : super.getSpecificCarrierId();
    }

    @Override
    public String getSpecificCarrierName() {
        return mCarrierResolver != null
                ? mCarrierResolver.getSpecificCarrierName() : super.getSpecificCarrierName();
    }

    @Override
    public void resolveSubscriptionCarrierId(String simState) {
        if (mCarrierResolver != null) {
            mCarrierResolver.resolveSubscriptionCarrierId(simState);
        }
    }

    @Override
    public int getCarrierIdListVersion() {
        return mCarrierResolver != null
                ? mCarrierResolver.getCarrierListVersion() : super.getCarrierIdListVersion();
    }

    @Override
    public int getEmergencyNumberDbVersion() {
        return getEmergencyNumberTracker().getEmergencyNumberDbVersion();
    }

    @Override
    public void resetCarrierKeysForImsiEncryption() {
        mCIM.resetCarrierKeysForImsiEncryption(mContext, mPhoneId);
    }

    @Override
    public void setCarrierTestOverride(String mccmnc, String imsi, String iccid, String gid1,
            String gid2, String pnn, String spn, String carrierPrivilegeRules, String apn) {
        mCarrierResolver.setTestOverrideApn(apn);
        UiccProfile uiccProfile = mUiccController.getUiccProfileForPhone(getPhoneId());
        if (uiccProfile != null) {
            List<UiccAccessRule> testRules;
            if (carrierPrivilegeRules == null) {
                testRules = null;
            } else if (carrierPrivilegeRules.isEmpty()) {
                testRules = Collections.emptyList();
            } else {
                UiccAccessRule accessRule = new UiccAccessRule(
                        IccUtils.hexStringToBytes(carrierPrivilegeRules), null, 0);
                testRules = Collections.singletonList(accessRule);
            }
            uiccProfile.setTestOverrideCarrierPrivilegeRules(testRules);
        } else {
            // TODO: Fix "privilege" typo throughout telephony.
            mCarrierResolver.setTestOverrideCarrierPriviledgeRule(carrierPrivilegeRules); // NOTYPO
        }
        IccRecords r = null;
        if (isPhoneTypeGsm()) {
            r = mIccRecords.get();
        } else if (isPhoneTypeCdmaLte()) {
            r = mSimRecords;
        } else {
            loge("setCarrierTestOverride fails in CDMA only");
        }
        if (r != null) {
            r.setCarrierTestOverride(mccmnc, imsi, iccid, gid1, gid2, pnn, spn);
        }
    }

    @Override
    public String getGroupIdLevel1() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getGid1() : null;
        } else if (isPhoneTypeCdma()) {
            loge("GID1 is not available in CDMA");
            return null;
        } else { //isPhoneTypeCdmaLte()
            return (mSimRecords != null) ? mSimRecords.getGid1() : "";
        }
    }

    @Override
    public String getGroupIdLevel2() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getGid2() : null;
        } else if (isPhoneTypeCdma()) {
            loge("GID2 is not available in CDMA");
            return null;
        } else { //isPhoneTypeCdmaLte()
            return (mSimRecords != null) ? mSimRecords.getGid2() : "";
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public String getLine1Number() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getMsisdnNumber() : null;
        } else {
            CarrierConfigManager configManager = (CarrierConfigManager)
                    mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            boolean use_usim = configManager.getConfigForSubId(getSubId()).getBoolean(
                    CarrierConfigManager.KEY_USE_USIM_BOOL);
            if (use_usim) {
                return (mSimRecords != null) ? mSimRecords.getMsisdnNumber() : null;
            }
            return mSST.getMdnNumber();
        }
    }

    @Override
    public String getPlmn() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getPnnHomeName() : null;
        } else if (isPhoneTypeCdma()) {
            loge("Plmn is not available in CDMA");
            return null;
        } else { //isPhoneTypeCdmaLte()
            return (mSimRecords != null) ? mSimRecords.getPnnHomeName() : null;
        }
    }

    /**
     * Update non-persisited manual network selection.
     *
     * @param nsm contains Plmn info
     */
    @Override
    protected void updateManualNetworkSelection(NetworkSelectMessage nsm) {
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            mManualNetworkSelectionPlmn = nsm.operatorNumeric;
        } else {
        //on Phone0 in emergency mode (no SIM), or in some races then clear the cache
            mManualNetworkSelectionPlmn = null;
            Rlog.e(LOG_TAG, "Cannot update network selection due to invalid subId "
                    + subId);
        }
    }

    @Override
    public String getManualNetworkSelectionPlmn() {
        return (mManualNetworkSelectionPlmn == null) ? "" : mManualNetworkSelectionPlmn;
    }

    @Override
    public String getCdmaPrlVersion() {
        return mSST.getPrlVersion();
    }

    @Override
    public String getCdmaMin() {
        return mSST.getCdmaMin();
    }

    @Override
    public boolean isMinInfoReady() {
        return mSST.isMinInfoReady();
    }

    @Override
    public String getMsisdn() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getMsisdnNumber() : null;
        } else if (isPhoneTypeCdmaLte()) {
            return (mSimRecords != null) ? mSimRecords.getMsisdnNumber() : null;
        } else {
            loge("getMsisdn: not expected on CDMA");
            return null;
        }
    }

    @Override
    public String getLine1AlphaTag() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getMsisdnAlphaTag() : null;
        } else {
            loge("getLine1AlphaTag: not possible in CDMA");
            return null;
        }
    }

    @Override
    public boolean setLine1Number(String alphaTag, String number, Message onComplete) {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            if (r != null) {
                r.setMsisdnNumber(alphaTag, number, onComplete);
                return true;
            } else {
                return false;
            }
        } else {
            loge("setLine1Number: not possible in CDMA");
            return false;
        }
    }

    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceMailNumber, Message onComplete) {
        Message resp;
        mVmNumber = voiceMailNumber;
        resp = obtainMessage(EVENT_SET_VM_NUMBER_DONE, 0, 0, onComplete);

        IccRecords r = mIccRecords.get();

        if (!isPhoneTypeGsm() && mSimRecords != null) {
            r = mSimRecords;
        }

        if (r != null) {
            r.setVoiceMailNumber(alphaTag, mVmNumber, resp);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isValidCommandInterfaceCFReason (int commandInterfaceCFReason) {
        switch (commandInterfaceCFReason) {
            case CF_REASON_UNCONDITIONAL:
            case CF_REASON_BUSY:
            case CF_REASON_NO_REPLY:
            case CF_REASON_NOT_REACHABLE:
            case CF_REASON_ALL:
            case CF_REASON_ALL_CONDITIONAL:
                return true;
            default:
                return false;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public String getSystemProperty(String property, String defValue) {
        if (getUnitTestMode()) {
            return null;
        }
        return TelephonyManager.getTelephonyProperty(mPhoneId, property, defValue);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isValidCommandInterfaceCFAction (int commandInterfaceCFAction) {
        switch (commandInterfaceCFAction) {
            case CF_ACTION_DISABLE:
            case CF_ACTION_ENABLE:
            case CF_ACTION_REGISTRATION:
            case CF_ACTION_ERASURE:
                return true;
            default:
                return false;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isCfEnable(int action) {
        return (action == CF_ACTION_ENABLE) || (action == CF_ACTION_REGISTRATION);
    }

    private boolean isImsUtEnabledOverCdma() {
        return isPhoneTypeCdmaLte()
            && mImsPhone != null
            && mImsPhone.isUtEnabled();
    }

    private boolean isCsRetry(Message onComplete) {
        if (onComplete != null) {
            return onComplete.getData().getBoolean(CS_FALLBACK_SS, false);
        }
        return false;
    }

    private void updateSsOverCdmaSupported(PersistableBundle b) {
        if (b == null) return;
        mSsOverCdmaSupported = b.getBoolean(CarrierConfigManager.KEY_SUPPORT_SS_OVER_CDMA_BOOL);
    }

    @Override
    public boolean useSsOverIms(Message onComplete) {
        boolean isUtEnabled = isUtEnabled();

        Rlog.d(LOG_TAG, "useSsOverIms: isUtEnabled()= " + isUtEnabled +
                " isCsRetry(onComplete))= " + isCsRetry(onComplete));

        if (isUtEnabled && !isCsRetry(onComplete)) {
            return true;
        }
        return false;
    }

    @Override
    public void getCallForwardingOption(int commandInterfaceCFReason, Message onComplete) {
        getCallForwardingOption(commandInterfaceCFReason,
                CommandsInterface.SERVICE_CLASS_VOICE, onComplete);
    }

    @Override
    public void getCallForwardingOption(int commandInterfaceCFReason, int serviceClass,
            Message onComplete) {
        Phone imsPhone = mImsPhone;
        if (useSsOverIms(onComplete)) {
            imsPhone.getCallForwardingOption(commandInterfaceCFReason, serviceClass, onComplete);
            return;
        }

        if (isPhoneTypeGsm()) {
            if (isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
                if (DBG) logd("requesting call forwarding query.");
                Message resp;
                if (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL) {
                    resp = obtainMessage(EVENT_GET_CALL_FORWARD_DONE, onComplete);
                } else {
                    resp = onComplete;
                }
                mCi.queryCallForwardStatus(commandInterfaceCFReason, serviceClass, null, resp);
            }
        } else {
            if (!mSsOverCdmaSupported) {
                // If SS over CDMA is not supported and UT is not at the time, notify the user of
                // the error and disable the option.
                AsyncResult.forMessage(onComplete, null,
                        new CommandException(CommandException.Error.INVALID_STATE,
                                "Call Forwarding over CDMA unavailable"));
            } else {
                loge("getCallForwardingOption: not possible in CDMA, just return empty result");
                AsyncResult.forMessage(onComplete, makeEmptyCallForward(), null);
            }
            onComplete.sendToTarget();
        }
    }

    @Override
    public void setCallForwardingOption(int commandInterfaceCFAction,
            int commandInterfaceCFReason,
            String dialingNumber,
            int timerSeconds,
            Message onComplete) {
        setCallForwardingOption(commandInterfaceCFAction, commandInterfaceCFReason,
                dialingNumber, CommandsInterface.SERVICE_CLASS_VOICE, timerSeconds, onComplete);
    }

    @Override
    public void setCallForwardingOption(int commandInterfaceCFAction,
            int commandInterfaceCFReason,
            String dialingNumber,
            int serviceClass,
            int timerSeconds,
            Message onComplete) {
        Phone imsPhone = mImsPhone;
        if (useSsOverIms(onComplete)) {
            imsPhone.setCallForwardingOption(commandInterfaceCFAction, commandInterfaceCFReason,
                    dialingNumber, serviceClass, timerSeconds, onComplete);
            return;
        }

        if (isPhoneTypeGsm()) {
            if ((isValidCommandInterfaceCFAction(commandInterfaceCFAction)) &&
                    (isValidCommandInterfaceCFReason(commandInterfaceCFReason))) {

                Message resp;
                if (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL) {
                    Cfu cfu = new Cfu(dialingNumber, onComplete);
                    resp = obtainMessage(EVENT_SET_CALL_FORWARD_DONE,
                            isCfEnable(commandInterfaceCFAction) ? 1 : 0, 0, cfu);
                } else {
                    resp = onComplete;
                }
                mCi.setCallForward(commandInterfaceCFAction,
                        commandInterfaceCFReason,
                        serviceClass,
                        dialingNumber,
                        timerSeconds,
                        resp);
            }
        } else if (mSsOverCdmaSupported) {
            String formatNumber = GsmCdmaConnection.formatDialString(dialingNumber);
            String cfNumber = CdmaMmiCode.getCallForwardingPrefixAndNumber(
                    commandInterfaceCFAction, commandInterfaceCFReason, formatNumber);
            loge("setCallForwardingOption: dial for set call forwarding"
                    + " prefixWithNumber= " + cfNumber + " number= " + dialingNumber);

            PhoneAccountHandle phoneAccountHandle = subscriptionIdToPhoneAccountHandle(getSubId());
            Bundle extras = new Bundle();
            extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);

            final TelecomManager telecomManager = TelecomManager.from(mContext);
            telecomManager.placeCall(
                    Uri.fromParts(PhoneAccount.SCHEME_TEL, cfNumber, null), extras);

            AsyncResult.forMessage(onComplete, CommandsInterface.SS_STATUS_UNKNOWN, null);
            onComplete.sendToTarget();
        } else {
            loge("setCallForwardingOption: SS over CDMA not supported, can not complete");
            AsyncResult.forMessage(onComplete, CommandsInterface.SS_STATUS_UNKNOWN, null);
            onComplete.sendToTarget();
        }
    }

    @Override
    public void getCallBarring(String facility, String password, Message onComplete,
            int serviceClass) {
        Phone imsPhone = mImsPhone;
        if (useSsOverIms(onComplete)) {
            imsPhone.getCallBarring(facility, password, onComplete, serviceClass);
            return;
        }

        if (isPhoneTypeGsm()) {
            mCi.queryFacilityLock(facility, password, serviceClass, onComplete);
        } else {
            loge("getCallBarringOption: not possible in CDMA");
        }
    }

    @Override
    public void setCallBarring(String facility, boolean lockState, String password,
            Message onComplete, int serviceClass) {
        Phone imsPhone = mImsPhone;
        if (useSsOverIms(onComplete)) {
            imsPhone.setCallBarring(facility, lockState, password, onComplete, serviceClass);
            return;
        }

        if (isPhoneTypeGsm()) {
            mCi.setFacilityLock(facility, lockState, password, serviceClass, onComplete);
        } else {
            loge("setCallBarringOption: not possible in CDMA");
        }
    }

    /**
     * Changes access code used for call barring
     *
     * @param facility is one of CB_FACILTY_*
     * @param oldPwd is old password
     * @param newPwd is new password
     * @param onComplete is callback message when the action is completed.
     */
    public void changeCallBarringPassword(String facility, String oldPwd, String newPwd,
            Message onComplete) {
        if (isPhoneTypeGsm()) {
            mCi.changeBarringPassword(facility, oldPwd, newPwd, onComplete);
        } else {
            loge("changeCallBarringPassword: not possible in CDMA");
        }
    }

    @Override
    public void getOutgoingCallerIdDisplay(Message onComplete) {
        Phone imsPhone = mImsPhone;
        if (useSsOverIms(onComplete)) {
            imsPhone.getOutgoingCallerIdDisplay(onComplete);
            return;
        }

        if (isPhoneTypeGsm()) {
            mCi.getCLIR(onComplete);
        } else {
            loge("getOutgoingCallerIdDisplay: not possible in CDMA");
            AsyncResult.forMessage(onComplete, null,
                    new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED));
            onComplete.sendToTarget();
        }
    }

    @Override
    public void setOutgoingCallerIdDisplay(int commandInterfaceCLIRMode, Message onComplete) {
        Phone imsPhone = mImsPhone;
        if (useSsOverIms(onComplete)) {
            imsPhone.setOutgoingCallerIdDisplay(commandInterfaceCLIRMode, onComplete);
            return;
        }

        if (isPhoneTypeGsm()) {
            // Packing CLIR value in the message. This will be required for
            // SharedPreference caching, if the message comes back as part of
            // a success response.
            mCi.setCLIR(commandInterfaceCLIRMode,
                    obtainMessage(EVENT_SET_CLIR_COMPLETE, commandInterfaceCLIRMode, 0, onComplete));
        } else {
            loge("setOutgoingCallerIdDisplay: not possible in CDMA");
            AsyncResult.forMessage(onComplete, null,
                    new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED));
            onComplete.sendToTarget();
        }
    }

    @Override
    public void queryCLIP(Message onComplete) {
        Phone imsPhone = mImsPhone;
        if (useSsOverIms(onComplete)) {
            imsPhone.queryCLIP(onComplete);
            return;
        }

        if (isPhoneTypeGsm()) {
            mCi.queryCLIP(onComplete);
        } else {
            loge("queryCLIP: not possible in CDMA");
            AsyncResult.forMessage(onComplete, null,
                    new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED));
            onComplete.sendToTarget();
        }
    }

    @Override
    public void getCallWaiting(Message onComplete) {
        Phone imsPhone = mImsPhone;
        if (useSsOverIms(onComplete)) {
            imsPhone.getCallWaiting(onComplete);
            return;
        }

        if (isPhoneTypeGsm()) {
            //As per 3GPP TS 24.083, section 1.6 UE doesn't need to send service
            //class parameter in call waiting interrogation  to network
            mCi.queryCallWaiting(CommandsInterface.SERVICE_CLASS_NONE, onComplete);
        } else {
            if (!mSsOverCdmaSupported) {
                // If SS over CDMA is not supported and UT is not at the time, notify the user of
                // the error and disable the option.
                AsyncResult.forMessage(onComplete, null,
                        new CommandException(CommandException.Error.INVALID_STATE,
                                "Call Waiting over CDMA unavailable"));
            } else {
                int[] arr =
                        {CommandsInterface.SS_STATUS_UNKNOWN, CommandsInterface.SERVICE_CLASS_NONE};
                AsyncResult.forMessage(onComplete, arr, null);
            }
            onComplete.sendToTarget();
        }
    }

    @Override
    public void setCallWaiting(boolean enable, Message onComplete) {
        int serviceClass = CommandsInterface.SERVICE_CLASS_VOICE;
        CarrierConfigManager configManager = (CarrierConfigManager)
            getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configManager.getConfigForSubId(getSubId());
        if (b != null) {
            serviceClass = b.getInt(CarrierConfigManager.KEY_CALL_WAITING_SERVICE_CLASS_INT,
                    CommandsInterface.SERVICE_CLASS_VOICE);
        }
        setCallWaiting(enable, serviceClass, onComplete);
    }

    @Override
    public void setCallWaiting(boolean enable, int serviceClass, Message onComplete) {
        Phone imsPhone = mImsPhone;
        if (useSsOverIms(onComplete)) {
            imsPhone.setCallWaiting(enable, onComplete);
            return;
        }

        if (isPhoneTypeGsm()) {
            mCi.setCallWaiting(enable, serviceClass, onComplete);
        } else if (mSsOverCdmaSupported) {
            String cwPrefix = CdmaMmiCode.getCallWaitingPrefix(enable);
            Rlog.i(LOG_TAG, "setCallWaiting in CDMA : dial for set call waiting" + " prefix= " + cwPrefix);

            PhoneAccountHandle phoneAccountHandle = subscriptionIdToPhoneAccountHandle(getSubId());
            Bundle extras = new Bundle();
            extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);

            final TelecomManager telecomManager = TelecomManager.from(mContext);
            telecomManager.placeCall(
                    Uri.fromParts(PhoneAccount.SCHEME_TEL, cwPrefix, null), extras);

            AsyncResult.forMessage(onComplete, CommandsInterface.SS_STATUS_UNKNOWN, null);
            onComplete.sendToTarget();
        } else {
            loge("setCallWaiting: SS over CDMA not supported, can not complete");
            AsyncResult.forMessage(onComplete, CommandsInterface.SS_STATUS_UNKNOWN, null);
            onComplete.sendToTarget();
        }
    }

    @Override
    public void getAvailableNetworks(Message response) {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            Message msg = obtainMessage(EVENT_GET_AVAILABLE_NETWORKS_DONE, response);
            mCi.getAvailableNetworks(msg);
        } else {
            loge("getAvailableNetworks: not possible in CDMA");
        }
    }

    @Override
    public void startNetworkScan(NetworkScanRequest nsr, Message response) {
        mCi.startNetworkScan(nsr, response);
    }

    @Override
    public void stopNetworkScan(Message response) {
        mCi.stopNetworkScan(response);
    }

    @Override
    public void setTTYMode(int ttyMode, Message onComplete) {
        // Send out the TTY Mode change over RIL as well
        super.setTTYMode(ttyMode, onComplete);
        if (mImsPhone != null) {
            mImsPhone.setTTYMode(ttyMode, onComplete);
        }
    }

    @Override
    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
       if (mImsPhone != null) {
           mImsPhone.setUiTTYMode(uiTtyMode, onComplete);
       }
    }

    @Override
    public void setMute(boolean muted) {
        mCT.setMute(muted);
    }

    @Override
    public boolean getMute() {
        return mCT.getMute();
    }

    @Override
    public void updateServiceLocation(WorkSource workSource) {
        mSST.enableSingleLocationUpdate(workSource);
    }

    @Override
    public void enableLocationUpdates() {
        mSST.enableLocationUpdates();
    }

    @Override
    public void disableLocationUpdates() {
        mSST.disableLocationUpdates();
    }

    @Override
    public boolean getDataRoamingEnabled() {
        if (isUsingNewDataStack()) {
            return getDataSettingsManager().isDataRoamingEnabled();
        }
        if (getDcTracker(AccessNetworkConstants.TRANSPORT_TYPE_WWAN) != null) {
            return getDcTracker(AccessNetworkConstants.TRANSPORT_TYPE_WWAN).getDataRoamingEnabled();
        }
        return false;
    }

    @Override
    public void setDataRoamingEnabled(boolean enable) {
        if (isUsingNewDataStack()) {
            getDataSettingsManager().setDataRoamingEnabled(enable);
            return;
        }
        if (getDcTracker(AccessNetworkConstants.TRANSPORT_TYPE_WWAN) != null) {
            getDcTracker(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                    .setDataRoamingEnabledByUser(enable);
        }
    }

    @Override
    public void registerForCdmaOtaStatusChange(Handler h, int what, Object obj) {
        mCi.registerForCdmaOtaProvision(h, what, obj);
    }

    @Override
    public void unregisterForCdmaOtaStatusChange(Handler h) {
        mCi.unregisterForCdmaOtaProvision(h);
    }

    @Override
    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
        mSST.registerForSubscriptionInfoReady(h, what, obj);
    }

    @Override
    public void unregisterForSubscriptionInfoReady(Handler h) {
        mSST.unregisterForSubscriptionInfoReady(h);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void setOnEcbModeExitResponse(Handler h, int what, Object obj) {
        mEcmExitRespRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unsetOnEcbModeExitResponse(Handler h) {
        mEcmExitRespRegistrant.clear();
    }

    @Override
    public void registerForCallWaiting(Handler h, int what, Object obj) {
        mCT.registerForCallWaiting(h, what, obj);
    }

    @Override
    public void unregisterForCallWaiting(Handler h) {
        mCT.unregisterForCallWaiting(h);
    }

    /**
     * Whether data is enabled by user. Unlike isDataEnabled, this only
     * checks user setting stored in {@link android.provider.Settings.Global#MOBILE_DATA}
     * if not provisioning, or isProvisioningDataEnabled if provisioning.
     */
    @Override
    public boolean isUserDataEnabled() {
        if (isUsingNewDataStack()) {
            return getDataSettingsManager().isDataEnabledForReason(
                    TelephonyManager.DATA_ENABLED_REASON_USER);
        }
        if (mDataEnabledSettings.isProvisioning()) {
            return mDataEnabledSettings.isProvisioningDataEnabled();
        } else {
            return mDataEnabledSettings.isUserDataEnabled();
        }
    }

    /**
     * Removes the given MMI from the pending list and notifies
     * registrants that it is complete.
     * @param mmi MMI that is done
     */
    public void onMMIDone(MmiCode mmi) {
        /* Only notify complete if it's on the pending list.
         * Otherwise, it's already been handled (eg, previously canceled).
         * The exception is cancellation of an incoming USSD-REQUEST, which is
         * not on the list.
         */
        if (mPendingMMIs.remove(mmi) || (isPhoneTypeGsm() && (mmi.isUssdRequest() ||
                ((GsmMmiCode)mmi).isSsInfo()))) {
            ResultReceiver receiverCallback = mmi.getUssdCallbackReceiver();
            if (receiverCallback != null) {
                Rlog.i(LOG_TAG, "onMMIDone: invoking callback: " + mmi);
                int returnCode = (mmi.getState() ==  MmiCode.State.COMPLETE) ?
                    TelephonyManager.USSD_RETURN_SUCCESS : TelephonyManager.USSD_RETURN_FAILURE;
                sendUssdResponse(mmi.getDialString(), mmi.getMessage(), returnCode,
                        receiverCallback );
            } else {
                Rlog.i(LOG_TAG, "onMMIDone: notifying registrants: " + mmi);
                mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult(null, mmi, null));
            }
        } else {
            Rlog.i(LOG_TAG, "onMMIDone: invalid response or already handled; ignoring: " + mmi);
        }
    }

    public boolean supports3gppCallForwardingWhileRoaming() {
        CarrierConfigManager configManager = (CarrierConfigManager)
                getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configManager.getConfigForSubId(getSubId());
        if (b != null) {
            return b.getBoolean(
                    CarrierConfigManager.KEY_SUPPORT_3GPP_CALL_FORWARDING_WHILE_ROAMING_BOOL, true);
        } else {
            // Default value set in CarrierConfigManager
            return true;
        }
    }

    private void onNetworkInitiatedUssd(MmiCode mmi) {
        Rlog.v(LOG_TAG, "onNetworkInitiatedUssd: mmi=" + mmi);
        mMmiCompleteRegistrants.notifyRegistrants(
            new AsyncResult(null, mmi, null));
    }

    /** ussdMode is one of CommandsInterface.USSD_MODE_* */
    private void onIncomingUSSD (int ussdMode, String ussdMessage) {
        if (!isPhoneTypeGsm()) {
            loge("onIncomingUSSD: not expected on GSM");
        }

        boolean isUssdError;
        boolean isUssdRequest;
        boolean isUssdRelease;

        isUssdRequest
            = (ussdMode == CommandsInterface.USSD_MODE_REQUEST);

        isUssdError
            = (ussdMode != CommandsInterface.USSD_MODE_NOTIFY
                && ussdMode != CommandsInterface.USSD_MODE_REQUEST);

        isUssdRelease = (ussdMode == CommandsInterface.USSD_MODE_NW_RELEASE);


        // See comments in GsmMmiCode.java
        // USSD requests aren't finished until one
        // of these two events happen
        GsmMmiCode found = null;
        for (int i = 0, s = mPendingMMIs.size() ; i < s; i++) {
            if(((GsmMmiCode)mPendingMMIs.get(i)).isPendingUSSD()) {
                found = (GsmMmiCode)mPendingMMIs.get(i);
                break;
            }
        }

        if (found != null) {
            // Complete pending USSD
            if (isUssdRelease) {
                found.onUssdRelease();
            } else if (isUssdError) {
                found.onUssdFinishedError();
            } else {
                found.onUssdFinished(ussdMessage, isUssdRequest);
            }
        } else if (!isUssdError && !TextUtils.isEmpty(ussdMessage)) {
            // pending USSD not found
            // The network may initiate its own USSD request

            // ignore everything that isnt a Notify or a Request
            // also, discard if there is no message to present
            GsmMmiCode mmi;
            mmi = GsmMmiCode.newNetworkInitiatedUssd(ussdMessage,
                                                   isUssdRequest,
                                                   GsmCdmaPhone.this,
                                                   mUiccApplication.get());
            onNetworkInitiatedUssd(mmi);
        } else if (isUssdError && !isUssdRelease) {
            GsmMmiCode mmi;
            mmi = GsmMmiCode.newNetworkInitiatedUssd(ussdMessage,
                    true,
                    GsmCdmaPhone.this,
                    mUiccApplication.get());
            mmi.onUssdFinishedError();
        }
    }

    /**
     * Make sure the network knows our preferred setting.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void syncClirSetting() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        migrateClirSettingIfNeeded(sp);

        int clirSetting = sp.getInt(CLIR_KEY + getSubId(), -1);
        Rlog.i(LOG_TAG, "syncClirSetting: " + CLIR_KEY + getSubId() + "=" + clirSetting);
        if (clirSetting >= 0) {
            mCi.setCLIR(clirSetting, null);
        } else {
            // if there is no preference set, ensure the CLIR is updated to the default value in
            // order to ensure that CLIR values in the RIL are not carried over during SIM swap.
            mCi.setCLIR(CommandsInterface.CLIR_DEFAULT, null);
        }
    }

    /**
     * Migrate CLIR setting with sudId mapping once if there's CLIR setting mapped with phoneId.
     */
    private void migrateClirSettingIfNeeded(SharedPreferences sp) {
        // Get old CLIR setting mapped with phoneId
        int clirSetting = sp.getInt("clir_key" + getPhoneId(), -1);
        if (clirSetting >= 0) {
            // Migrate CLIR setting to new shared preference key with subId
            Rlog.i(LOG_TAG, "Migrate CLIR setting: value=" + clirSetting + ", clir_key"
                    + getPhoneId() + " -> " + CLIR_KEY + getSubId());
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(CLIR_KEY + getSubId(), clirSetting);

            // Remove old CLIR setting key
            editor.remove("clir_key" + getPhoneId()).commit();
        }
    }

    private void handleRadioAvailable() {
        mCi.getBasebandVersion(obtainMessage(EVENT_GET_BASEBAND_VERSION_DONE));

        mCi.getDeviceIdentity(obtainMessage(EVENT_GET_DEVICE_IDENTITY_DONE));
        mCi.getRadioCapability(obtainMessage(EVENT_GET_RADIO_CAPABILITY));
        mCi.areUiccApplicationsEnabled(obtainMessage(EVENT_GET_UICC_APPS_ENABLEMENT_DONE));

        startLceAfterRadioIsAvailable();
    }

    private void handleRadioOn() {
        /* Proactively query voice radio technologies */
        mCi.getVoiceRadioTechnology(obtainMessage(EVENT_REQUEST_VOICE_RADIO_TECH_DONE));

        if (!isPhoneTypeGsm()) {
            mCdmaSubscriptionSource = mCdmaSSM.getCdmaSubscriptionSource();
        }
    }

    private void handleRadioOffOrNotAvailable() {
        if (isPhoneTypeGsm()) {
            // Some MMI requests (eg USSD) are not completed
            // within the course of a CommandsInterface request
            // If the radio shuts off or resets while one of these
            // is pending, we need to clean up.

            for (int i = mPendingMMIs.size() - 1; i >= 0; i--) {
                if (((GsmMmiCode) mPendingMMIs.get(i)).isPendingUSSD()) {
                    ((GsmMmiCode) mPendingMMIs.get(i)).onUssdFinishedError();
                }
            }
        }
        mRadioOffOrNotAvailableRegistrants.notifyRegistrants();
    }

    private void handleRadioPowerStateChange() {
        @RadioPowerState int newState = mCi.getRadioState();
        Rlog.d(LOG_TAG, "handleRadioPowerStateChange, state= " + newState);
        mNotifier.notifyRadioPowerStateChanged(this, newState);
        TelephonyMetrics.getInstance().writeRadioState(mPhoneId, newState);
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        Message onComplete;

        switch (msg.what) {
            case EVENT_RADIO_AVAILABLE: {
                handleRadioAvailable();
            }
            break;

            case EVENT_GET_DEVICE_IDENTITY_DONE:{
                ar = (AsyncResult)msg.obj;

                if (ar.exception != null) {
                    break;
                }
                String[] respId = (String[])ar.result;
                mImei = respId[0];
                mImeiSv = respId[1];
                mEsn  =  respId[2];
                mMeid =  respId[3];
                // some modems return all 0's instead of null/empty string when MEID is unavailable
                if (!TextUtils.isEmpty(mMeid) && mMeid.matches("^0*$")) {
                    logd("EVENT_GET_DEVICE_IDENTITY_DONE: set mMeid to null");
                    mMeid = null;
                }
            }
            break;

            case EVENT_EMERGENCY_CALLBACK_MODE_ENTER:{
                handleEnterEmergencyCallbackMode(msg);
            }
            break;

            case  EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE:{
                handleExitEmergencyCallbackMode(msg);
            }
            break;

            case EVENT_MODEM_RESET: {
                logd("Event EVENT_MODEM_RESET Received" + " isInEcm = " + isInEcm()
                        + " isPhoneTypeGsm = " + isPhoneTypeGsm() + " mImsPhone = " + mImsPhone);
                if (isInEcm()) {
                    if (isPhoneTypeGsm()) {
                        if (mImsPhone != null) {
                            mImsPhone.handleExitEmergencyCallbackMode();
                        }
                    } else {
                        handleExitEmergencyCallbackMode(msg);
                    }
                }
            }
            break;

            case EVENT_RUIM_RECORDS_LOADED:
                logd("Event EVENT_RUIM_RECORDS_LOADED Received");
                updateCurrentCarrierInProvider();
                break;

            case EVENT_RADIO_ON:
                logd("Event EVENT_RADIO_ON Received");
                handleRadioOn();
                break;

            case EVENT_RIL_CONNECTED:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null && ar.result != null) {
                    mRilVersion = (Integer) ar.result;
                } else {
                    logd("Unexpected exception on EVENT_RIL_CONNECTED");
                    mRilVersion = -1;
                }
                break;

            case EVENT_VOICE_RADIO_TECH_CHANGED:
            case EVENT_REQUEST_VOICE_RADIO_TECH_DONE:
                String what = (msg.what == EVENT_VOICE_RADIO_TECH_CHANGED) ?
                        "EVENT_VOICE_RADIO_TECH_CHANGED" : "EVENT_REQUEST_VOICE_RADIO_TECH_DONE";
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    if ((ar.result != null) && (((int[]) ar.result).length != 0)) {
                        int newVoiceTech = ((int[]) ar.result)[0];
                        logd(what + ": newVoiceTech=" + newVoiceTech);
                        phoneObjectUpdater(newVoiceTech);
                    } else {
                        loge(what + ": has no tech!");
                    }
                } else {
                    loge(what + ": exception=" + ar.exception);
                }
                break;

            case EVENT_LINK_CAPACITY_CHANGED:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null && ar.result != null) {
                    updateLinkCapacityEstimate((List<LinkCapacityEstimate>) ar.result);
                } else {
                    logd("Unexpected exception on EVENT_LINK_CAPACITY_CHANGED");
                }
                break;

            case EVENT_UPDATE_PHONE_OBJECT:
                phoneObjectUpdater(msg.arg1);
                break;

            case EVENT_CARRIER_CONFIG_CHANGED:
                // Only check for the voice radio tech if it not going to be updated by the voice
                // registration changes.
                if (!mContext.getResources().getBoolean(
                        com.android.internal.R.bool
                                .config_switch_phone_on_voice_reg_state_change)) {
                    mCi.getVoiceRadioTechnology(obtainMessage(EVENT_REQUEST_VOICE_RADIO_TECH_DONE));
                }

                CarrierConfigManager configMgr = (CarrierConfigManager)
                        getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
                PersistableBundle b = configMgr.getConfigForSubId(getSubId());

                updateBroadcastEmergencyCallStateChangesAfterCarrierConfigChanged(b);

                updateCdmaRoamingSettingsAfterCarrierConfigChanged(b);

                updateNrSettingsAfterCarrierConfigChanged(b);
                updateVoNrSettings(b);
                updateSsOverCdmaSupported(b);
                loadAllowedNetworksFromSubscriptionDatabase();
                // Obtain new radio capabilities from the modem, since some are SIM-dependent
                mCi.getRadioCapability(obtainMessage(EVENT_GET_RADIO_CAPABILITY));
                break;

            case EVENT_SET_ROAMING_PREFERENCE_DONE:
                logd("cdma_roaming_mode change is done");
                break;

            case EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED:
                logd("EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED");
                mCdmaSubscriptionSource = mCdmaSSM.getCdmaSubscriptionSource();
                break;

            case EVENT_REGISTERED_TO_NETWORK:
                logd("Event EVENT_REGISTERED_TO_NETWORK Received");
                if (isPhoneTypeGsm()) {
                    syncClirSetting();
                }
                break;

            case EVENT_SIM_RECORDS_LOADED:
                updateCurrentCarrierInProvider();

                // Check if this is a different SIM than the previous one. If so unset the
                // voice mail number.
                String imsi = getVmSimImsi();
                String imsiFromSIM = getSubscriberId();
                if ((!isPhoneTypeGsm() || imsi != null) && imsiFromSIM != null
                        && !imsiFromSIM.equals(imsi)) {
                    storeVoiceMailNumber(null);
                    setVmSimImsi(null);
                }

                updateVoiceMail();

                mSimRecordsLoadedRegistrants.notifyRegistrants();
                break;

            case EVENT_GET_BASEBAND_VERSION_DONE:
                ar = (AsyncResult)msg.obj;

                if (ar.exception != null) {
                    break;
                }

                if (DBG) logd("Baseband version: " + ar.result);
                /* Android property value is limited to 91 characters, but low layer
                 could pass a larger version string. To avoid runtime exception,
                 truncate the string baseband version string to 45 characters at most
                 for this per sub property. Since the latter part of the version string
                 is meaningful, truncated the version string from the beginning and
                 keep the end of the version.
                */
                String version = (String)ar.result;
                if (version != null) {
                    int length = version.length();
                    final int MAX_VERSION_LEN = SystemProperties.PROP_VALUE_MAX/2;
                    TelephonyManager.from(mContext).setBasebandVersionForPhone(getPhoneId(),
                            length <= MAX_VERSION_LEN ? version
                                : version.substring(length - MAX_VERSION_LEN, length));
                }
            break;

            case EVENT_USSD:
                ar = (AsyncResult)msg.obj;

                String[] ussdResult = (String[]) ar.result;

                if (ussdResult.length > 1) {
                    try {
                        onIncomingUSSD(Integer.parseInt(ussdResult[0]), ussdResult[1]);
                    } catch (NumberFormatException e) {
                        Rlog.w(LOG_TAG, "error parsing USSD");
                    }
                }
            break;

            case EVENT_RADIO_OFF_OR_NOT_AVAILABLE: {
                logd("Event EVENT_RADIO_OFF_OR_NOT_AVAILABLE Received");
                handleRadioOffOrNotAvailable();
                break;
            }

            case EVENT_RADIO_STATE_CHANGED: {
                logd("EVENT EVENT_RADIO_STATE_CHANGED");
                handleRadioPowerStateChange();
                break;
            }

            case EVENT_SSN:
                logd("Event EVENT_SSN Received");
                if (isPhoneTypeGsm()) {
                    ar = (AsyncResult) msg.obj;
                    SuppServiceNotification not = (SuppServiceNotification) ar.result;
                    mSsnRegistrants.notifyRegistrants(ar);
                }
                break;

            case EVENT_REGISTRATION_FAILED:
                logd("Event RegistrationFailed Received");
                ar = (AsyncResult) msg.obj;
                RegistrationFailedEvent rfe = (RegistrationFailedEvent) ar.result;
                mNotifier.notifyRegistrationFailed(this, rfe.cellIdentity, rfe.chosenPlmn,
                        rfe.domain, rfe.causeCode, rfe.additionalCauseCode);
                break;

            case EVENT_BARRING_INFO_CHANGED:
                logd("Event BarringInfoChanged Received");
                ar = (AsyncResult) msg.obj;
                BarringInfo barringInfo = (BarringInfo) ar.result;
                mNotifier.notifyBarringInfoChanged(this, barringInfo);
                break;

            case EVENT_SET_CALL_FORWARD_DONE:
                ar = (AsyncResult)msg.obj;
                Cfu cfu = (Cfu) ar.userObj;
                if (ar.exception == null) {
                    setVoiceCallForwardingFlag(1, msg.arg1 == 1, cfu.mSetCfNumber);
                }
                if (cfu.mOnComplete != null) {
                    AsyncResult.forMessage(cfu.mOnComplete, ar.result, ar.exception);
                    cfu.mOnComplete.sendToTarget();
                }
                break;

            case EVENT_SET_VM_NUMBER_DONE:
                ar = (AsyncResult)msg.obj;
                if (((isPhoneTypeGsm() || mSimRecords != null)
                        && IccVmNotSupportedException.class.isInstance(ar.exception))
                        || (!isPhoneTypeGsm() && mSimRecords == null
                        && IccException.class.isInstance(ar.exception))) {
                    storeVoiceMailNumber(mVmNumber);
                    ar.exception = null;
                }
                onComplete = (Message) ar.userObj;
                if (onComplete != null) {
                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                    onComplete.sendToTarget();
                }
                break;


            case EVENT_GET_CALL_FORWARD_DONE:
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    handleCfuQueryResult((CallForwardInfo[])ar.result);
                }
                onComplete = (Message) ar.userObj;
                if (onComplete != null) {
                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                    onComplete.sendToTarget();
                }
                break;

            case EVENT_SET_NETWORK_AUTOMATIC:
                // Automatic network selection from EF_CSP SIM record
                ar = (AsyncResult) msg.obj;
                if (mSST.mSS.getIsManualSelection()) {
                    setNetworkSelectionModeAutomatic((Message) ar.result);
                    logd("SET_NETWORK_SELECTION_AUTOMATIC: set to automatic");
                } else {
                    // prevent duplicate request which will push current PLMN to low priority
                    logd("SET_NETWORK_SELECTION_AUTOMATIC: already automatic, ignore");
                }
                break;

            case EVENT_ICC_RECORD_EVENTS:
                ar = (AsyncResult)msg.obj;
                processIccRecordEvents((Integer)ar.result);
                break;

            case EVENT_SET_CLIR_COMPLETE:
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    saveClirSetting(msg.arg1);
                }
                onComplete = (Message) ar.userObj;
                if (onComplete != null) {
                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                    onComplete.sendToTarget();
                }
                break;

            case EVENT_SS:
                ar = (AsyncResult)msg.obj;
                logd("Event EVENT_SS received");
                if (isPhoneTypeGsm()) {
                    // SS data is already being handled through MMI codes.
                    // So, this result if processed as MMI response would help
                    // in re-using the existing functionality.
                    GsmMmiCode mmi = new GsmMmiCode(this, mUiccApplication.get());
                    mmi.processSsData(ar);
                }
                break;

            case EVENT_GET_RADIO_CAPABILITY:
                ar = (AsyncResult) msg.obj;
                RadioCapability rc = (RadioCapability) ar.result;
                if (ar.exception != null) {
                    Rlog.d(LOG_TAG, "get phone radio capability fail, no need to change " +
                            "mRadioCapability");
                } else {
                    radioCapabilityUpdated(rc, false);
                }
                Rlog.d(LOG_TAG, "EVENT_GET_RADIO_CAPABILITY: phone rc: " + rc);
                break;
            case EVENT_VRS_OR_RAT_CHANGED:
                ar = (AsyncResult) msg.obj;
                Pair<Integer, Integer> vrsRatPair = (Pair<Integer, Integer>) ar.result;
                onVoiceRegStateOrRatChanged(vrsRatPair.first, vrsRatPair.second);
                break;

            case EVENT_SET_CARRIER_DATA_ENABLED:
                ar = (AsyncResult) msg.obj;
                boolean enabled = (boolean) ar.result;
                if (isUsingNewDataStack()) {
                    getDataSettingsManager().setDataEnabled(
                            TelephonyManager.DATA_ENABLED_REASON_CARRIER, enabled,
                            mContext.getOpPackageName());
                    return;
                }
                mDataEnabledSettings.setDataEnabled(TelephonyManager.DATA_ENABLED_REASON_CARRIER,
                        enabled);
                break;
            case EVENT_DEVICE_PROVISIONED_CHANGE:
                if (!isUsingNewDataStack()) {
                    mDataEnabledSettings.updateProvisionedChanged();
                }
                break;
            case EVENT_DEVICE_PROVISIONING_DATA_SETTING_CHANGE:
                if (!isUsingNewDataStack()) {
                    mDataEnabledSettings.updateProvisioningDataEnabled();
                }
                break;
            case EVENT_GET_AVAILABLE_NETWORKS_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null && ar.result != null && mSST != null) {
                    List<OperatorInfo> operatorInfoList = (List<OperatorInfo>) ar.result;
                    List<OperatorInfo> filteredInfoList = new ArrayList<>();
                    for (OperatorInfo operatorInfo : operatorInfoList) {
                        if (OperatorInfo.State.CURRENT == operatorInfo.getState()) {
                            filteredInfoList.add(new OperatorInfo(
                                    mSST.filterOperatorNameByPattern(
                                            operatorInfo.getOperatorAlphaLong()),
                                    mSST.filterOperatorNameByPattern(
                                            operatorInfo.getOperatorAlphaShort()),
                                    operatorInfo.getOperatorNumeric(),
                                    operatorInfo.getState()
                            ));
                        } else {
                            filteredInfoList.add(operatorInfo);
                        }
                    }
                    ar.result = filteredInfoList;
                }

                onComplete = (Message) ar.userObj;
                if (onComplete != null) {
                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                    onComplete.sendToTarget();
                }
                break;
            case EVENT_GET_UICC_APPS_ENABLEMENT_DONE:
            case EVENT_UICC_APPS_ENABLEMENT_STATUS_CHANGED:
                ar = (AsyncResult) msg.obj;
                if (ar == null) return;
                if (ar.exception != null) {
                    logd("Received exception on event" + msg.what + " : " + ar.exception);
                    return;
                }

                mUiccApplicationsEnabled = (Boolean) ar.result;
            // Intentional falling through.
            case EVENT_UICC_APPS_ENABLEMENT_SETTING_CHANGED:
                reapplyUiccAppsEnablementIfNeeded(ENABLE_UICC_APPS_MAX_RETRIES);
                break;

            case EVENT_REAPPLY_UICC_APPS_ENABLEMENT_DONE: {
                ar = (AsyncResult) msg.obj;
                if (ar == null || ar.exception == null) return;
                Pair<Boolean, Integer> userObject = (Pair) ar.userObj;
                if (userObject == null) return;
                boolean expectedValue = userObject.first;
                int retries = userObject.second;
                CommandException.Error error = ((CommandException) ar.exception).getCommandError();
                loge("Error received when re-applying uicc application"
                        + " setting to " +  expectedValue + " on phone " + mPhoneId
                        + " Error code: " + error + " retry count left: " + retries);
                if (retries > 0 && (error == GENERIC_FAILURE || error == SIM_BUSY)) {
                    // Retry for certain errors, but not for others like RADIO_NOT_AVAILABLE or
                    // SIM_ABSENT, as they will trigger it whey they become available.
                    postDelayed(()->reapplyUiccAppsEnablementIfNeeded(retries - 1),
                            REAPPLY_UICC_APPS_SETTING_RETRY_TIME_GAP_IN_MS);
                }
                break;
            }
            case EVENT_RESET_CARRIER_KEY_IMSI_ENCRYPTION: {
                resetCarrierKeysForImsiEncryption();
                break;
            }
            case EVENT_SET_VONR_ENABLED_DONE:
                logd("EVENT_SET_VONR_ENABLED_DONE is done");
                break;
            case EVENT_SUBSCRIPTIONS_CHANGED:
                logd("EVENT_SUBSCRIPTIONS_CHANGED");
                updateUsageSetting();
                break;

            default:
                super.handleMessage(msg);
        }
    }

    public UiccCardApplication getUiccCardApplication() {
        if (isPhoneTypeGsm()) {
            return mUiccController.getUiccCardApplication(mPhoneId, UiccController.APP_FAM_3GPP);
        } else {
            return mUiccController.getUiccCardApplication(mPhoneId, UiccController.APP_FAM_3GPP2);
        }
    }

    // todo: check if ICC availability needs to be handled here. mSimRecords should not be needed
    // now because APIs can be called directly on UiccProfile, and that should handle the requests
    // correctly based on supported apps, voice RAT, etc.
    @Override
    protected void onUpdateIccAvailability() {
        if (mUiccController == null ) {
            return;
        }

        UiccCardApplication newUiccApplication = null;

        // Update mIsimUiccRecords
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            newUiccApplication =
                    mUiccController.getUiccCardApplication(mPhoneId, UiccController.APP_FAM_IMS);
            IsimUiccRecords newIsimUiccRecords = null;

            if (newUiccApplication != null) {
                newIsimUiccRecords = (IsimUiccRecords) newUiccApplication.getIccRecords();
                if (DBG) logd("New ISIM application found");
            }
            mIsimUiccRecords = newIsimUiccRecords;
        }

        // Update mSimRecords
        if (mSimRecords != null) {
            mSimRecords.unregisterForRecordsLoaded(this);
        }
        if (isPhoneTypeCdmaLte() || isPhoneTypeCdma()) {
            newUiccApplication = mUiccController.getUiccCardApplication(mPhoneId,
                    UiccController.APP_FAM_3GPP);
            SIMRecords newSimRecords = null;
            if (newUiccApplication != null) {
                newSimRecords = (SIMRecords) newUiccApplication.getIccRecords();
            }
            mSimRecords = newSimRecords;
            if (mSimRecords != null) {
                mSimRecords.registerForRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
            }
        } else {
            mSimRecords = null;
        }

        // Update mIccRecords, mUiccApplication, mIccPhoneBookIntManager
        newUiccApplication = getUiccCardApplication();
        if (!isPhoneTypeGsm() && newUiccApplication == null) {
            logd("can't find 3GPP2 application; trying APP_FAM_3GPP");
            newUiccApplication = mUiccController.getUiccCardApplication(mPhoneId,
                    UiccController.APP_FAM_3GPP);
        }

        UiccCardApplication app = mUiccApplication.get();
        if (app != newUiccApplication) {
            if (app != null) {
                if (DBG) logd("Removing stale icc objects.");
                if (mIccRecords.get() != null) {
                    unregisterForIccRecordEvents();
                    mIccPhoneBookIntManager.updateIccRecords(null);
                }
                mIccRecords.set(null);
                mUiccApplication.set(null);
            }
            if (newUiccApplication != null) {
                if (DBG) {
                    logd("New Uicc application found. type = " + newUiccApplication.getType());
                }
                final IccRecords iccRecords = newUiccApplication.getIccRecords();
                mUiccApplication.set(newUiccApplication);
                mIccRecords.set(iccRecords);
                registerForIccRecordEvents();
                mIccPhoneBookIntManager.updateIccRecords(iccRecords);
                if (iccRecords != null) {
                    final String simOperatorNumeric = iccRecords.getOperatorNumeric();
                    if (DBG) {
                        logd("New simOperatorNumeric = " + simOperatorNumeric);
                    }
                    if (!TextUtils.isEmpty(simOperatorNumeric)) {
                        TelephonyManager.from(mContext).setSimOperatorNumericForPhone(mPhoneId,
                                simOperatorNumeric);
                    }
                }
                updateCurrentCarrierInProvider();
            }
        }

        reapplyUiccAppsEnablementIfNeeded(ENABLE_UICC_APPS_MAX_RETRIES);
    }

    private void processIccRecordEvents(int eventCode) {
        switch (eventCode) {
            case IccRecords.EVENT_CFI:
                logi("processIccRecordEvents: EVENT_CFI");
                notifyCallForwardingIndicator();
                break;
        }
    }

    /**
     * Sets the "current" field in the telephony provider according to the SIM's operator
     *
     * @return true for success; false otherwise.
     */
    @Override
    public boolean updateCurrentCarrierInProvider() {
        long currentDds = SubscriptionManager.getDefaultDataSubscriptionId();
        String operatorNumeric = getOperatorNumeric();

        logd("updateCurrentCarrierInProvider: mSubId = " + getSubId()
                + " currentDds = " + currentDds + " operatorNumeric = " + operatorNumeric);

        if (!TextUtils.isEmpty(operatorNumeric) && (getSubId() == currentDds)) {
            try {
                Uri uri = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current");
                ContentValues map = new ContentValues();
                map.put(Telephony.Carriers.NUMERIC, operatorNumeric);
                mContext.getContentResolver().insert(uri, map);
                return true;
            } catch (SQLException e) {
                Rlog.e(LOG_TAG, "Can't store current operator", e);
            }
        }
        return false;
    }

    //CDMA
    /**
     * Sets the "current" field in the telephony provider according to the
     * build-time operator numeric property
     *
     * @return true for success; false otherwise.
     */
    private boolean updateCurrentCarrierInProvider(String operatorNumeric) {
        if (isPhoneTypeCdma()
                || (isPhoneTypeCdmaLte() && mUiccController.getUiccCardApplication(mPhoneId,
                        UiccController.APP_FAM_3GPP) == null)) {
            logd("CDMAPhone: updateCurrentCarrierInProvider called");
            if (!TextUtils.isEmpty(operatorNumeric)) {
                try {
                    Uri uri = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current");
                    ContentValues map = new ContentValues();
                    map.put(Telephony.Carriers.NUMERIC, operatorNumeric);
                    logd("updateCurrentCarrierInProvider from system: numeric=" + operatorNumeric);
                    getContext().getContentResolver().insert(uri, map);

                    // Updates MCC MNC device configuration information
                    logd("update mccmnc=" + operatorNumeric);
                    MccTable.updateMccMncConfiguration(mContext, operatorNumeric);

                    return true;
                } catch (SQLException e) {
                    Rlog.e(LOG_TAG, "Can't store current operator", e);
                }
            }
            return false;
        } else { // isPhoneTypeCdmaLte()
            if (DBG) logd("updateCurrentCarrierInProvider not updated X retVal=" + true);
            return true;
        }
    }

    private void handleCfuQueryResult(CallForwardInfo[] infos) {
        if (infos == null || infos.length == 0) {
            // Assume the default is not active
            // Set unconditional CFF in SIM to false
            setVoiceCallForwardingFlag(1, false, null);
        } else {
            for (int i = 0, s = infos.length; i < s; i++) {
                if ((infos[i].serviceClass & SERVICE_CLASS_VOICE) != 0) {
                    setVoiceCallForwardingFlag(1, (infos[i].status == 1),
                        infos[i].number);
                    // should only have the one
                    break;
                }
            }
        }
    }

    /**
     * Retrieves the IccPhoneBookInterfaceManager of the GsmCdmaPhone
     */
    @Override
    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager(){
        return mIccPhoneBookIntManager;
    }

    /**
     * Activate or deactivate cell broadcast SMS.
     *
     * @param activate 0 = activate, 1 = deactivate
     * @param response Callback message is empty on completion
     */
    @Override
    public void activateCellBroadcastSms(int activate, Message response) {
        loge("[GsmCdmaPhone] activateCellBroadcastSms() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    /**
     * Query the current configuration of cdma cell broadcast SMS.
     *
     * @param response Callback message is empty on completion
     */
    @Override
    public void getCellBroadcastSmsConfig(Message response) {
        loge("[GsmCdmaPhone] getCellBroadcastSmsConfig() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    /**
     * Configure cdma cell broadcast SMS.
     *
     * @param response Callback message is empty on completion
     */
    @Override
    public void setCellBroadcastSmsConfig(int[] configValuesArray, Message response) {
        loge("[GsmCdmaPhone] setCellBroadcastSmsConfig() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    /**
     * Returns true if OTA Service Provisioning needs to be performed.
     */
    @Override
    public boolean needsOtaServiceProvisioning() {
        if (isPhoneTypeGsm()) {
            return false;
        } else {
            return mSST.getOtasp() != TelephonyManager.OTASP_NOT_NEEDED;
        }
    }

    @Override
    public boolean isCspPlmnEnabled() {
        IccRecords r = mIccRecords.get();
        return (r != null) ? r.isCspPlmnEnabled() : false;
    }

    /**
     * Whether manual select is now allowed and we should set
     * to auto network select mode.
     */
    public boolean shouldForceAutoNetworkSelect() {

        int networkTypeBitmask = RadioAccessFamily.getRafFromNetworkType(
                RILConstants.PREFERRED_NETWORK_MODE);
        int subId = getSubId();

        // If it's invalid subId, we shouldn't force to auto network select mode.
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }

        networkTypeBitmask = (int) getAllowedNetworkTypes(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER);

        logd("shouldForceAutoNetworkSelect in mode = " + networkTypeBitmask);
        /*
         *  For multimode targets in global mode manual network
         *  selection is disallowed. So we should force auto select mode.
         */
        if (isManualSelProhibitedInGlobalMode()
                && ((networkTypeBitmask == RadioAccessFamily.getRafFromNetworkType(
                TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA))
                || (networkTypeBitmask == RadioAccessFamily.getRafFromNetworkType(
                TelephonyManager.NETWORK_MODE_GLOBAL)))) {
            logd("Should force auto network select mode = " + networkTypeBitmask);
            return true;
        } else {
            logd("Should not force auto network select mode = " + networkTypeBitmask);
        }

        /*
         *  Single mode phone with - GSM network modes/global mode
         *  LTE only for 3GPP
         *  LTE centric + 3GPP Legacy
         *  Note: the actual enabling/disabling manual selection for these
         *  cases will be controlled by csp
         */
        return false;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isManualSelProhibitedInGlobalMode() {
        boolean isProhibited = false;
        final String configString = getContext().getResources().getString(com.android.internal
                .R.string.prohibit_manual_network_selection_in_gobal_mode);

        if (!TextUtils.isEmpty(configString)) {
            String[] configArray = configString.split(";");

            if (configArray != null &&
                    ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true")) ||
                        (configArray.length == 2 && !TextUtils.isEmpty(configArray[1]) &&
                            configArray[0].equalsIgnoreCase("true") &&
                            isMatchGid(configArray[1])))) {
                            isProhibited = true;
            }
        }
        logd("isManualNetSelAllowedInGlobal in current carrier is " + isProhibited);
        return isProhibited;
    }

    private void registerForIccRecordEvents() {
        IccRecords r = mIccRecords.get();
        if (r == null) {
            return;
        }
        if (isPhoneTypeGsm()) {
            r.registerForNetworkSelectionModeAutomatic(
                    this, EVENT_SET_NETWORK_AUTOMATIC, null);
            r.registerForRecordsEvents(this, EVENT_ICC_RECORD_EVENTS, null);
            r.registerForRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
        } else {
            r.registerForRecordsLoaded(this, EVENT_RUIM_RECORDS_LOADED, null);
            if (isPhoneTypeCdmaLte()) {
                // notify simRecordsLoaded registrants for cdmaLte phone
                r.registerForRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
            }
        }
    }

    private void unregisterForIccRecordEvents() {
        IccRecords r = mIccRecords.get();
        if (r == null) {
            return;
        }
        r.unregisterForNetworkSelectionModeAutomatic(this);
        r.unregisterForRecordsEvents(this);
        r.unregisterForRecordsLoaded(this);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void exitEmergencyCallbackMode() {
        if (DBG) {
            Rlog.d(LOG_TAG, "exitEmergencyCallbackMode: mImsPhone=" + mImsPhone
                    + " isPhoneTypeGsm=" + isPhoneTypeGsm());
        }
        if (mImsPhone != null && mImsPhone.isInImsEcm()) {
            mImsPhone.exitEmergencyCallbackMode();
        } else {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            Message msg = null;
            if (mIsTestingEmergencyCallbackMode) {
                // prevent duplicate exit messages from happening due to this message being handled
                // as well as an UNSOL when the modem exits ECbM. Instead, only register for this
                // message callback when this is a test and we will not be receiving the UNSOL from
                // the modem.
                msg = obtainMessage(EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE);
            }
            mCi.exitEmergencyCallbackMode(msg);
        }
    }

    //CDMA
    private void handleEnterEmergencyCallbackMode(Message msg) {
        if (DBG) {
            Rlog.d(LOG_TAG, "handleEnterEmergencyCallbackMode, isInEcm()="
                    + isInEcm());
        }
        // if phone is not in Ecm mode, and it's changed to Ecm mode
        if (!isInEcm()) {
            setIsInEcm(true);

            // notify change
            sendEmergencyCallbackModeChange();

            // Post this runnable so we will automatically exit
            // if no one invokes exitEmergencyCallbackMode() directly.
            long delayInMillis = TelephonyProperties.ecm_exit_timer()
                    .orElse(DEFAULT_ECM_EXIT_TIMER_VALUE);
            postDelayed(mExitEcmRunnable, delayInMillis);
            // We don't want to go to sleep while in Ecm
            mWakeLock.acquire();
        }
    }

    //CDMA
    private void handleExitEmergencyCallbackMode(Message msg) {
        AsyncResult ar = (AsyncResult)msg.obj;
        if (DBG) {
            Rlog.d(LOG_TAG, "handleExitEmergencyCallbackMode,ar.exception , isInEcm="
                    + ar.exception + isInEcm());
        }
        // Remove pending exit Ecm runnable, if any
        removeCallbacks(mExitEcmRunnable);

        if (mEcmExitRespRegistrant != null) {
            mEcmExitRespRegistrant.notifyRegistrant(ar);
        }
        // if exiting is successful or we are testing and the modem responded with an error upon
        // exit, which may occur in some IRadio implementations.
        if (ar.exception == null || mIsTestingEmergencyCallbackMode) {
            if (isInEcm()) {
                setIsInEcm(false);
            }

            // release wakeLock
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }

            // send an Intent
            sendEmergencyCallbackModeChange();
            // Re-initiate data connection
            if (!isUsingNewDataStack()) {
                mDataEnabledSettings.setInternalDataEnabled(true);
            }
            notifyEmergencyCallRegistrants(false);
        }
        mIsTestingEmergencyCallbackMode = false;
    }

    //CDMA
    public void notifyEmergencyCallRegistrants(boolean started) {
        mEmergencyCallToggledRegistrants.notifyResult(started ? 1 : 0);
    }

    //CDMA
    /**
     * Handle to cancel or restart Ecm timer in emergency call back mode
     * if action is CANCEL_ECM_TIMER, cancel Ecm timer and notify apps the timer is canceled;
     * otherwise, restart Ecm timer and notify apps the timer is restarted.
     */
    public void handleTimerInEmergencyCallbackMode(int action) {
        switch(action) {
            case CANCEL_ECM_TIMER:
                removeCallbacks(mExitEcmRunnable);
                mEcmTimerResetRegistrants.notifyResult(Boolean.TRUE);
                setEcmCanceledForEmergency(true /*isCanceled*/);
                break;
            case RESTART_ECM_TIMER:
                long delayInMillis = TelephonyProperties.ecm_exit_timer()
                        .orElse(DEFAULT_ECM_EXIT_TIMER_VALUE);
                postDelayed(mExitEcmRunnable, delayInMillis);
                mEcmTimerResetRegistrants.notifyResult(Boolean.FALSE);
                setEcmCanceledForEmergency(false /*isCanceled*/);
                break;
            default:
                Rlog.e(LOG_TAG, "handleTimerInEmergencyCallbackMode, unsupported action " + action);
        }
    }

    //CDMA
    private static final String IS683A_FEATURE_CODE = "*228";
    private static final int IS683A_FEATURE_CODE_NUM_DIGITS = 4;
    private static final int IS683A_SYS_SEL_CODE_NUM_DIGITS = 2;
    private static final int IS683A_SYS_SEL_CODE_OFFSET = 4;

    private static final int IS683_CONST_800MHZ_A_BAND = 0;
    private static final int IS683_CONST_800MHZ_B_BAND = 1;
    private static final int IS683_CONST_1900MHZ_A_BLOCK = 2;
    private static final int IS683_CONST_1900MHZ_B_BLOCK = 3;
    private static final int IS683_CONST_1900MHZ_C_BLOCK = 4;
    private static final int IS683_CONST_1900MHZ_D_BLOCK = 5;
    private static final int IS683_CONST_1900MHZ_E_BLOCK = 6;
    private static final int IS683_CONST_1900MHZ_F_BLOCK = 7;
    private static final int INVALID_SYSTEM_SELECTION_CODE = -1;

    // Define the pattern/format for carrier specified OTASP number schema.
    // It separates by comma and/or whitespace.
    private static Pattern pOtaSpNumSchema = Pattern.compile("[,\\s]+");

    //CDMA
    private static boolean isIs683OtaSpDialStr(String dialStr) {
        int sysSelCodeInt;
        boolean isOtaspDialString = false;
        int dialStrLen = dialStr.length();

        if (dialStrLen == IS683A_FEATURE_CODE_NUM_DIGITS) {
            if (dialStr.equals(IS683A_FEATURE_CODE)) {
                isOtaspDialString = true;
            }
        } else {
            sysSelCodeInt = extractSelCodeFromOtaSpNum(dialStr);
            switch (sysSelCodeInt) {
                case IS683_CONST_800MHZ_A_BAND:
                case IS683_CONST_800MHZ_B_BAND:
                case IS683_CONST_1900MHZ_A_BLOCK:
                case IS683_CONST_1900MHZ_B_BLOCK:
                case IS683_CONST_1900MHZ_C_BLOCK:
                case IS683_CONST_1900MHZ_D_BLOCK:
                case IS683_CONST_1900MHZ_E_BLOCK:
                case IS683_CONST_1900MHZ_F_BLOCK:
                    isOtaspDialString = true;
                    break;
                default:
                    break;
            }
        }
        return isOtaspDialString;
    }

    //CDMA
    /**
     * This function extracts the system selection code from the dial string.
     */
    private static int extractSelCodeFromOtaSpNum(String dialStr) {
        int dialStrLen = dialStr.length();
        int sysSelCodeInt = INVALID_SYSTEM_SELECTION_CODE;

        if ((dialStr.regionMatches(0, IS683A_FEATURE_CODE,
                0, IS683A_FEATURE_CODE_NUM_DIGITS)) &&
                (dialStrLen >= (IS683A_FEATURE_CODE_NUM_DIGITS +
                        IS683A_SYS_SEL_CODE_NUM_DIGITS))) {
            // Since we checked the condition above, the system selection code
            // extracted from dialStr will not cause any exception
            sysSelCodeInt = Integer.parseInt (
                    dialStr.substring (IS683A_FEATURE_CODE_NUM_DIGITS,
                            IS683A_FEATURE_CODE_NUM_DIGITS + IS683A_SYS_SEL_CODE_NUM_DIGITS));
        }
        if (DBG) Rlog.d(LOG_TAG, "extractSelCodeFromOtaSpNum " + sysSelCodeInt);
        return sysSelCodeInt;
    }

    //CDMA
    /**
     * This function checks if the system selection code extracted from
     * the dial string "sysSelCodeInt' is the system selection code specified
     * in the carrier ota sp number schema "sch".
     */
    private static boolean checkOtaSpNumBasedOnSysSelCode(int sysSelCodeInt, String sch[]) {
        boolean isOtaSpNum = false;
        try {
            // Get how many number of system selection code ranges
            int selRc = Integer.parseInt(sch[1]);
            for (int i = 0; i < selRc; i++) {
                if (!TextUtils.isEmpty(sch[i*2+2]) && !TextUtils.isEmpty(sch[i*2+3])) {
                    int selMin = Integer.parseInt(sch[i*2+2]);
                    int selMax = Integer.parseInt(sch[i*2+3]);
                    // Check if the selection code extracted from the dial string falls
                    // within any of the range pairs specified in the schema.
                    if ((sysSelCodeInt >= selMin) && (sysSelCodeInt <= selMax)) {
                        isOtaSpNum = true;
                        break;
                    }
                }
            }
        } catch (NumberFormatException ex) {
            // If the carrier ota sp number schema is not correct, we still allow dial
            // and only log the error:
            Rlog.e(LOG_TAG, "checkOtaSpNumBasedOnSysSelCode, error", ex);
        }
        return isOtaSpNum;
    }

    //CDMA
    /**
     * The following function checks if a dial string is a carrier specified
     * OTASP number or not by checking against the OTASP number schema stored
     * in PROPERTY_OTASP_NUM_SCHEMA.
     *
     * Currently, there are 2 schemas for carriers to specify the OTASP number:
     * 1) Use system selection code:
     *    The schema is:
     *    SELC,the # of code pairs,min1,max1,min2,max2,...
     *    e.g "SELC,3,10,20,30,40,60,70" indicates that there are 3 pairs of
     *    selection codes, and they are {10,20}, {30,40} and {60,70} respectively.
     *
     * 2) Use feature code:
     *    The schema is:
     *    "FC,length of feature code,feature code".
     *     e.g "FC,2,*2" indicates that the length of the feature code is 2,
     *     and the code itself is "*2".
     */
    private boolean isCarrierOtaSpNum(String dialStr) {
        boolean isOtaSpNum = false;
        int sysSelCodeInt = extractSelCodeFromOtaSpNum(dialStr);
        if (sysSelCodeInt == INVALID_SYSTEM_SELECTION_CODE) {
            return isOtaSpNum;
        }
        // mCarrierOtaSpNumSchema is retrieved from PROPERTY_OTASP_NUM_SCHEMA:
        if (!TextUtils.isEmpty(mCarrierOtaSpNumSchema)) {
            Matcher m = pOtaSpNumSchema.matcher(mCarrierOtaSpNumSchema);
            if (DBG) {
                Rlog.d(LOG_TAG, "isCarrierOtaSpNum,schema" + mCarrierOtaSpNumSchema);
            }

            if (m.find()) {
                String sch[] = pOtaSpNumSchema.split(mCarrierOtaSpNumSchema);
                // If carrier uses system selection code mechanism
                if (!TextUtils.isEmpty(sch[0]) && sch[0].equals("SELC")) {
                    if (sysSelCodeInt!=INVALID_SYSTEM_SELECTION_CODE) {
                        isOtaSpNum=checkOtaSpNumBasedOnSysSelCode(sysSelCodeInt,sch);
                    } else {
                        if (DBG) {
                            Rlog.d(LOG_TAG, "isCarrierOtaSpNum,sysSelCodeInt is invalid");
                        }
                    }
                } else if (!TextUtils.isEmpty(sch[0]) && sch[0].equals("FC")) {
                    int fcLen =  Integer.parseInt(sch[1]);
                    String fc = sch[2];
                    if (dialStr.regionMatches(0,fc,0,fcLen)) {
                        isOtaSpNum = true;
                    } else {
                        if (DBG) Rlog.d(LOG_TAG, "isCarrierOtaSpNum,not otasp number");
                    }
                } else {
                    if (DBG) {
                        Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema not supported" + sch[0]);
                    }
                }
            } else {
                if (DBG) {
                    Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema pattern not right" +
                            mCarrierOtaSpNumSchema);
                }
            }
        } else {
            if (DBG) Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema pattern empty");
        }
        return isOtaSpNum;
    }

    /**
     * isOTASPNumber: checks a given number against the IS-683A OTASP dial string and carrier
     * OTASP dial string.
     *
     * @param dialStr the number to look up.
     * @return true if the number is in IS-683A OTASP dial string or carrier OTASP dial string
     */
    @Override
    public  boolean isOtaSpNumber(String dialStr) {
        if (isPhoneTypeGsm()) {
            return super.isOtaSpNumber(dialStr);
        } else {
            boolean isOtaSpNum = false;
            String dialableStr = PhoneNumberUtils.extractNetworkPortionAlt(dialStr);
            if (dialableStr != null) {
                isOtaSpNum = isIs683OtaSpDialStr(dialableStr);
                if (isOtaSpNum == false) {
                    isOtaSpNum = isCarrierOtaSpNum(dialableStr);
                }
            }
            if (DBG) Rlog.d(LOG_TAG, "isOtaSpNumber " + isOtaSpNum);
            return isOtaSpNum;
        }
    }

    @Override
    public int getOtasp() {
        return mSST.getOtasp();
    }

    @Override
    public int getCdmaEriIconIndex() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriIconIndex();
        } else {
            return getServiceState().getCdmaEriIconIndex();
        }
    }

    /**
     * Returns the CDMA ERI icon mode,
     * 0 - ON
     * 1 - FLASHING
     */
    @Override
    public int getCdmaEriIconMode() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriIconMode();
        } else {
            return getServiceState().getCdmaEriIconMode();
        }
    }

    /**
     * Returns the CDMA ERI text,
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public String getCdmaEriText() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriText();
        } else {
            int roamInd = getServiceState().getCdmaRoamingIndicator();
            int defRoamInd = getServiceState().getCdmaDefaultRoamingIndicator();
            return mSST.getCdmaEriText(roamInd, defRoamInd);
        }
    }

    // Return true if either CSIM or RUIM app is present
    @Override
    public boolean isCdmaSubscriptionAppPresent() {
        UiccCardApplication cdmaApplication =
                mUiccController.getUiccCardApplication(mPhoneId, UiccController.APP_FAM_3GPP2);
        return cdmaApplication != null && (cdmaApplication.getType() == AppType.APPTYPE_CSIM ||
                cdmaApplication.getType() == AppType.APPTYPE_RUIM);
    }

    protected void phoneObjectUpdater(int newVoiceRadioTech) {
        logd("phoneObjectUpdater: newVoiceRadioTech=" + newVoiceRadioTech);

        // Check for a voice over LTE/NR replacement
        if (ServiceState.isPsOnlyTech(newVoiceRadioTech)
                || (newVoiceRadioTech == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)) {
            CarrierConfigManager configMgr = (CarrierConfigManager)
                    getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle b = configMgr.getConfigForSubId(getSubId());
            if (b != null) {
                int volteReplacementRat =
                        b.getInt(CarrierConfigManager.KEY_VOLTE_REPLACEMENT_RAT_INT);
                logd("phoneObjectUpdater: volteReplacementRat=" + volteReplacementRat);
                if (volteReplacementRat != ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN &&
                           //In cdma case, replace rat only if csim or ruim app present
                           (ServiceState.isGsm(volteReplacementRat) ||
                           isCdmaSubscriptionAppPresent())) {
                    newVoiceRadioTech = volteReplacementRat;
                }
            } else {
                loge("phoneObjectUpdater: didn't get volteReplacementRat from carrier config");
            }
        }

        if(mRilVersion == 6 && getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
            /*
             * On v6 RIL, when LTE_ON_CDMA is TRUE, always create CDMALTEPhone
             * irrespective of the voice radio tech reported.
             */
            if (getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                logd("phoneObjectUpdater: LTE ON CDMA property is set. Use CDMA Phone" +
                        " newVoiceRadioTech=" + newVoiceRadioTech +
                        " mActivePhone=" + getPhoneName());
                return;
            } else {
                logd("phoneObjectUpdater: LTE ON CDMA property is set. Switch to CDMALTEPhone" +
                        " newVoiceRadioTech=" + newVoiceRadioTech +
                        " mActivePhone=" + getPhoneName());
                newVoiceRadioTech = ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT;
            }
        } else {

            // If the device is shutting down, then there is no need to switch to the new phone
            // which might send unnecessary attach request to the modem.
            if (isShuttingDown()) {
                logd("Device is shutting down. No need to switch phone now.");
                return;
            }

            boolean matchCdma = ServiceState.isCdma(newVoiceRadioTech);
            boolean matchGsm = ServiceState.isGsm(newVoiceRadioTech);
            if ((matchCdma && getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) ||
                    (matchGsm && getPhoneType() == PhoneConstants.PHONE_TYPE_GSM)) {
                // Nothing changed. Keep phone as it is.
                logd("phoneObjectUpdater: No change ignore," +
                        " newVoiceRadioTech=" + newVoiceRadioTech +
                        " mActivePhone=" + getPhoneName());
                return;
            }
            if (!matchCdma && !matchGsm) {
                loge("phoneObjectUpdater: newVoiceRadioTech=" + newVoiceRadioTech +
                        " doesn't match either CDMA or GSM - error! No phone change");
                return;
            }
        }

        if (newVoiceRadioTech == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) {
            // We need some voice phone object to be active always, so never
            // delete the phone without anything to replace it with!
            logd("phoneObjectUpdater: Unknown rat ignore, "
                    + " newVoiceRadioTech=Unknown. mActivePhone=" + getPhoneName());
            return;
        }

        boolean oldPowerState = false; // old power state to off
        if (mResetModemOnRadioTechnologyChange) {
            if (mCi.getRadioState() == TelephonyManager.RADIO_POWER_ON) {
                oldPowerState = true;
                logd("phoneObjectUpdater: Setting Radio Power to Off");
                mCi.setRadioPower(false, null);
            }
        }

        switchVoiceRadioTech(newVoiceRadioTech);

        if (mResetModemOnRadioTechnologyChange && oldPowerState) { // restore power state
            logd("phoneObjectUpdater: Resetting Radio");
            mCi.setRadioPower(oldPowerState, null);
        }

        // update voice radio tech in UiccProfile
        UiccProfile uiccProfile = getUiccProfile();
        if (uiccProfile != null) {
            uiccProfile.setVoiceRadioTech(newVoiceRadioTech);
        }

        // Send an Intent to the PhoneApp that we had a radio technology change
        Intent intent = new Intent(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        intent.putExtra(PhoneConstants.PHONE_NAME_KEY, getPhoneName());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhoneId);
        mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void switchVoiceRadioTech(int newVoiceRadioTech) {

        String outgoingPhoneName = getPhoneName();

        logd("Switching Voice Phone : " + outgoingPhoneName + " >>> "
                + (ServiceState.isGsm(newVoiceRadioTech) ? "GSM" : "CDMA"));

        if (ServiceState.isCdma(newVoiceRadioTech)) {
            UiccCardApplication cdmaApplication =
                    mUiccController.getUiccCardApplication(mPhoneId, UiccController.APP_FAM_3GPP2);
            if (cdmaApplication != null && cdmaApplication.getType() == AppType.APPTYPE_RUIM) {
                switchPhoneType(PhoneConstants.PHONE_TYPE_CDMA);
            } else {
                switchPhoneType(PhoneConstants.PHONE_TYPE_CDMA_LTE);
            }
        } else if (ServiceState.isGsm(newVoiceRadioTech)) {
            switchPhoneType(PhoneConstants.PHONE_TYPE_GSM);
        } else {
            loge("deleteAndCreatePhone: newVoiceRadioTech=" + newVoiceRadioTech +
                    " is not CDMA or GSM (error) - aborting!");
            return;
        }
    }

    @Override
    public void setLinkCapacityReportingCriteria(int[] dlThresholds, int[] ulThresholds, int ran) {
        mCi.setLinkCapacityReportingCriteria(REPORTING_HYSTERESIS_MILLIS, REPORTING_HYSTERESIS_KBPS,
                REPORTING_HYSTERESIS_KBPS, dlThresholds, ulThresholds, ran, null);
    }

    @Override
    public IccSmsInterfaceManager getIccSmsInterfaceManager(){
        return mIccSmsInterfaceManager;
    }

    @Override
    public void updatePhoneObject(int voiceRadioTech) {
        logd("updatePhoneObject: radioTechnology=" + voiceRadioTech);
        sendMessage(obtainMessage(EVENT_UPDATE_PHONE_OBJECT, voiceRadioTech, 0, null));
    }

    @Override
    public void setImsRegistrationState(boolean registered) {
        mSST.setImsRegistrationState(registered);
    }

    @Override
    public boolean getIccRecordsLoaded() {
        UiccProfile uiccProfile = getUiccProfile();
        return uiccProfile != null && uiccProfile.getIccRecordsLoaded();
    }

    @Override
    public IccCard getIccCard() {
        // This function doesn't return null for backwards compatability purposes.
        // To differentiate between cases where SIM is absent vs. unknown we return a placeholder
        // IccCard with the sim state set.
        IccCard card = getUiccProfile();
        if (card != null) {
            return card;
        } else {
            UiccSlot slot = mUiccController.getUiccSlotForPhone(mPhoneId);
            if (slot == null || slot.isStateUnknown()) {
                return new IccCard(IccCardConstants.State.UNKNOWN);
            } else {
                return new IccCard(IccCardConstants.State.ABSENT);
            }
        }
    }

    private UiccProfile getUiccProfile() {
        return UiccController.getInstance().getUiccProfileForPhone(mPhoneId);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GsmCdmaPhone extends:");
        super.dump(fd, pw, args);
        pw.println(" mPrecisePhoneType=" + mPrecisePhoneType);
        pw.println(" mCT=" + mCT);
        pw.println(" mSST=" + mSST);
        pw.println(" mPendingMMIs=" + mPendingMMIs);
        pw.println(" mIccPhoneBookIntManager=" + mIccPhoneBookIntManager);
        pw.println(" mImei=" + pii(mImei));
        pw.println(" mImeiSv=" + pii(mImeiSv));
        pw.println(" mVmNumber=" + pii(mVmNumber));
        pw.println(" mCdmaSSM=" + mCdmaSSM);
        pw.println(" mCdmaSubscriptionSource=" + mCdmaSubscriptionSource);
        pw.println(" mWakeLock=" + mWakeLock);
        pw.println(" isInEcm()=" + isInEcm());
        pw.println(" mEsn=" + pii(mEsn));
        pw.println(" mMeid=" + pii(mMeid));
        pw.println(" mCarrierOtaSpNumSchema=" + mCarrierOtaSpNumSchema);
        if (!isPhoneTypeGsm()) {
            pw.println(" getCdmaEriIconIndex()=" + getCdmaEriIconIndex());
            pw.println(" getCdmaEriIconMode()=" + getCdmaEriIconMode());
            pw.println(" getCdmaEriText()=" + getCdmaEriText());
            pw.println(" isMinInfoReady()=" + isMinInfoReady());
        }
        pw.println(" isCspPlmnEnabled()=" + isCspPlmnEnabled());
        pw.println(" mManualNetworkSelectionPlmn=" + mManualNetworkSelectionPlmn);
        pw.println(
                " mTelecomVoiceServiceStateOverride=" + mTelecomVoiceServiceStateOverride + "("
                        + ServiceState.rilServiceStateToString(mTelecomVoiceServiceStateOverride)
                        + ")");
        pw.flush();
    }

    @Override
    public boolean setOperatorBrandOverride(String brand) {
        if (mUiccController == null) {
            return false;
        }

        UiccPort port = mUiccController.getUiccPort(getPhoneId());
        if (port == null) {
            return false;
        }

        boolean status = port.setOperatorBrandOverride(brand);

        // Refresh.
        if (status) {
            TelephonyManager.from(mContext).setSimOperatorNameForPhone(
                    getPhoneId(), mSST.getServiceProviderName());
            // TODO: check if pollState is need when set operator brand override.
            mSST.pollState();
        }
        return status;
    }

    /**
     * This allows a short number to be remapped to a test emergency number for testing how the
     * frameworks handles Emergency Callback Mode without actually calling an emergency number.
     *
     * This is not a full test and is not a substitute for testing real emergency
     * numbers but can be useful.
     *
     * To use this feature, first set a test emergency number using
     * adb shell cmd phone emergency-number-test-mode -a 1-555-555-1212
     *
     * and then set the system property ril.test.emergencynumber to a pair of
     * numbers separated by a colon. If the first number matches the number parameter
     * this routine returns the second number. Example:
     *
     * ril.test.emergencynumber=411:1-555-555-1212
     *
     * To test Dial 411 take call then hang up on MO device to enter ECM.
     *
     * @param dialString to test if it should be remapped
     * @return the same number or the remapped number.
     */
    private String checkForTestEmergencyNumber(String dialString) {
        String testEn = SystemProperties.get("ril.test.emergencynumber");
        if (!TextUtils.isEmpty(testEn)) {
            String[] values = testEn.split(":");
            logd("checkForTestEmergencyNumber: values.length=" + values.length);
            if (values.length == 2) {
                if (values[0].equals(PhoneNumberUtils.stripSeparators(dialString))) {
                    logd("checkForTestEmergencyNumber: remap " + dialString + " to " + values[1]);
                    dialString = values[1];
                }
            }
        }
        return dialString;
    }

    @Override
    @NonNull
    public String getOperatorNumeric() {
        String operatorNumeric = null;
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            if (r != null) {
                operatorNumeric = r.getOperatorNumeric();
            }
        } else { //isPhoneTypeCdmaLte()
            IccRecords curIccRecords = null;
            if (mCdmaSubscriptionSource == CDMA_SUBSCRIPTION_NV) {
                operatorNumeric = SystemProperties.get("ro.cdma.home.operator.numeric");
            } else if (mCdmaSubscriptionSource == CDMA_SUBSCRIPTION_RUIM_SIM) {
                UiccCardApplication uiccCardApplication = mUiccApplication.get();
                if (uiccCardApplication != null
                        && uiccCardApplication.getType() == AppType.APPTYPE_RUIM) {
                    logd("Legacy RUIM app present");
                    curIccRecords = mIccRecords.get();
                } else {
                    // Use sim-records for SimApp, USimApp, CSimApp and ISimApp.
                    curIccRecords = mSimRecords;
                }
                if (curIccRecords != null && curIccRecords == mSimRecords) {
                    operatorNumeric = curIccRecords.getOperatorNumeric();
                } else {
                    curIccRecords = mIccRecords.get();
                    if (curIccRecords != null && (curIccRecords instanceof RuimRecords)) {
                        RuimRecords csim = (RuimRecords) curIccRecords;
                        operatorNumeric = csim.getRUIMOperatorNumeric();
                    }
                }
            }
            if (operatorNumeric == null) {
                loge("getOperatorNumeric: Cannot retrieve operatorNumeric:"
                        + " mCdmaSubscriptionSource = " + mCdmaSubscriptionSource +
                        " mIccRecords = " + ((curIccRecords != null) ?
                        curIccRecords.getRecordsLoaded() : null));
            }

            logd("getOperatorNumeric: mCdmaSubscriptionSource = " + mCdmaSubscriptionSource
                    + " operatorNumeric = " + operatorNumeric);

        }
        return TextUtils.emptyIfNull(operatorNumeric);
    }

    /**
     * @return The country ISO for the subscription associated with this phone.
     */
    public String getCountryIso() {
        int subId = getSubId();
        SubscriptionInfo subInfo = SubscriptionManager.from(getContext())
                .getActiveSubscriptionInfo(subId);
        if (subInfo == null || TextUtils.isEmpty(subInfo.getCountryIso())) {
            return null;
        }
        return subInfo.getCountryIso().toUpperCase();
    }

    public void notifyEcbmTimerReset(Boolean flag) {
        mEcmTimerResetRegistrants.notifyResult(flag);
    }

    private static final int[] VOICE_PS_CALL_RADIO_TECHNOLOGY = {
            ServiceState.RIL_RADIO_TECHNOLOGY_LTE,
            ServiceState.RIL_RADIO_TECHNOLOGY_LTE_CA,
            ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN,
            ServiceState.RIL_RADIO_TECHNOLOGY_NR
    };

    /**
     * Calculates current RIL voice radio technology for CS calls.
     *
     * This function should only be used in {@link com.android.internal.telephony.GsmCdmaConnection}
     * to indicate current CS call radio technology.
     *
     * @return the RIL voice radio technology used for CS calls,
     *         see {@code RIL_RADIO_TECHNOLOGY_*} in {@link android.telephony.ServiceState}.
     */
    public @RilRadioTechnology int getCsCallRadioTech() {
        int calcVrat = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
        if (mSST != null) {
            calcVrat = getCsCallRadioTech(mSST.mSS.getState(),
                    mSST.mSS.getRilVoiceRadioTechnology());
        }

        return calcVrat;
    }

    /**
     * Calculates current RIL voice radio technology for CS calls based on current voice
     * registration state and technology.
     *
     * Mark current RIL voice radio technology as unknow when any of below condtion is met:
     *  1) Current RIL voice registration state is not in-service.
     *  2) Current RIL voice radio technology is PS call technology, which means CSFB will
     *     happen later after call connection is established.
     *     It is inappropriate to notify upper layer the PS call technology while current call
     *     is CS call, so before CSFB happens, mark voice radio technology as unknow.
     *     After CSFB happens, {@link #onVoiceRegStateOrRatChanged} will update voice call radio
     *     technology with correct value.
     *
     * @param vrs the voice registration state
     * @param vrat the RIL voice radio technology
     *
     * @return the RIL voice radio technology used for CS calls,
     *         see {@code RIL_RADIO_TECHNOLOGY_*} in {@link android.telephony.ServiceState}.
     */
    private @RilRadioTechnology int getCsCallRadioTech(int vrs, int vrat) {
        logd("getCsCallRadioTech, current vrs=" + vrs + ", vrat=" + vrat);
        int calcVrat = vrat;
        if (vrs != ServiceState.STATE_IN_SERVICE
                || ArrayUtils.contains(VOICE_PS_CALL_RADIO_TECHNOLOGY, vrat)) {
            calcVrat = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
        }

        logd("getCsCallRadioTech, result calcVrat=" + calcVrat);
        return calcVrat;
    }

    /**
     * Handler of RIL Voice Radio Technology changed event.
     */
    private void onVoiceRegStateOrRatChanged(int vrs, int vrat) {
        logd("onVoiceRegStateOrRatChanged");
        mCT.dispatchCsCallRadioTech(getCsCallRadioTech(vrs, vrat));
    }

    /**
     * Registration point for Ecm timer reset
     *
     * @param h handler to notify
     * @param what User-defined message code
     * @param obj placed in Message.obj
     */
    @Override
    public void registerForEcmTimerReset(Handler h, int what, Object obj) {
        mEcmTimerResetRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForEcmTimerReset(Handler h) {
        mEcmTimerResetRegistrants.remove(h);
    }

    @Override
    public void registerForVolteSilentRedial(Handler h, int what, Object obj) {
        mVolteSilentRedialRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForVolteSilentRedial(Handler h) {
        mVolteSilentRedialRegistrants.remove(h);
    }

    public void notifyVolteSilentRedial(String dialString, int causeCode) {
        logd("notifyVolteSilentRedial: dialString=" + dialString + " causeCode=" + causeCode);
        AsyncResult ar = new AsyncResult(null,
                new SilentRedialParam(dialString, causeCode, mDialArgs), null);
        mVolteSilentRedialRegistrants.notifyRegistrants(ar);
    }

    /**
     * Sets the SIM voice message waiting indicator records.
     * @param line GSM Subscriber Profile Number, one-based. Only '1' is supported
     * @param countWaiting The number of messages waiting, if known. Use
     *                     -1 to indicate that an unknown number of
     *                      messages are waiting
     */
    @Override
    public void setVoiceMessageWaiting(int line, int countWaiting) {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            if (r != null) {
                r.setVoiceMessageWaiting(line, countWaiting);
            } else {
                logd("SIM Records not found, MWI not updated");
            }
        } else {
            setVoiceMessageCount(countWaiting);
        }
    }

    private CallForwardInfo[] makeEmptyCallForward() {
        CallForwardInfo infos[] = new CallForwardInfo[1];

        infos[0] = new CallForwardInfo();
        infos[0].status = CommandsInterface.SS_STATUS_UNKNOWN;
        infos[0].reason = 0;
        infos[0].serviceClass = CommandsInterface.SERVICE_CLASS_VOICE;
        infos[0].toa = PhoneNumberUtils.TOA_Unknown;
        infos[0].number = "";
        infos[0].timeSeconds = 0;

        return infos;
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(final int subId) {
        final TelecomManager telecomManager = TelecomManager.from(mContext);
        final TelephonyManager telephonyManager = TelephonyManager.from(mContext);
        final Iterator<PhoneAccountHandle> phoneAccounts =
            telecomManager.getCallCapablePhoneAccounts(true).listIterator();

        while (phoneAccounts.hasNext()) {
            final PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            final PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
            if (subId == telephonyManager.getSubIdForPhoneAccount(phoneAccount)) {
                return phoneAccountHandle;
            }
        }

        return null;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void logd(String s) {
        Rlog.d(LOG_TAG, "[" + mPhoneId + "] " + s);
    }

    private void logi(String s) {
        Rlog.i(LOG_TAG, "[" + mPhoneId + "] " + s);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void loge(String s) {
        Rlog.e(LOG_TAG, "[" + mPhoneId + "] " + s);
    }

    private static String pii(String s) {
        return Rlog.pii(LOG_TAG, s);
    }

    @Override
    public boolean isUtEnabled() {
        Phone imsPhone = mImsPhone;
        if (imsPhone != null) {
            return imsPhone.isUtEnabled();
        } else {
            logd("isUtEnabled: called for GsmCdma");
            return false;
        }
    }

    public String getDtmfToneDelayKey() {
        return isPhoneTypeGsm() ?
                CarrierConfigManager.KEY_GSM_DTMF_TONE_DELAY_INT :
                CarrierConfigManager.KEY_CDMA_DTMF_TONE_DELAY_INT;
    }

    @VisibleForTesting
    public PowerManager.WakeLock getWakeLock() {
        return mWakeLock;
    }

    public int getLteOnCdmaMode() {
        int currentConfig = TelephonyProperties.lte_on_cdma_device()
                .orElse(PhoneConstants.LTE_ON_CDMA_FALSE);
        int lteOnCdmaModeDynamicValue = currentConfig;

        UiccCardApplication cdmaApplication =
                    mUiccController.getUiccCardApplication(mPhoneId, UiccController.APP_FAM_3GPP2);
        if (cdmaApplication != null && cdmaApplication.getType() == AppType.APPTYPE_RUIM) {
            //Legacy RUIM cards don't support LTE.
            lteOnCdmaModeDynamicValue = RILConstants.LTE_ON_CDMA_FALSE;

            //Override only if static configuration is TRUE.
            if (currentConfig == RILConstants.LTE_ON_CDMA_TRUE) {
                return lteOnCdmaModeDynamicValue;
            }
        }
        return currentConfig;
    }

    private void updateTtyMode(int ttyMode) {
        logi(String.format("updateTtyMode ttyMode=%d", ttyMode));
        setTTYMode(telecomModeToPhoneMode(ttyMode), null);
    }
    private void updateUiTtyMode(int ttyMode) {
        logi(String.format("updateUiTtyMode ttyMode=%d", ttyMode));
        setUiTTYMode(telecomModeToPhoneMode(ttyMode), null);
    }

    /**
     * Given a telecom TTY mode, convert to a Telephony mode equivalent.
     * @param telecomMode Telecom TTY mode.
     * @return Telephony phone TTY mode.
     */
    private static int telecomModeToPhoneMode(int telecomMode) {
        switch (telecomMode) {
            // AT command only has 0 and 1, so mapping VCO
            // and HCO to FULL
            case TelecomManager.TTY_MODE_FULL:
            case TelecomManager.TTY_MODE_VCO:
            case TelecomManager.TTY_MODE_HCO:
                return Phone.TTY_MODE_FULL;
            default:
                return Phone.TTY_MODE_OFF;
        }
    }

    /**
     * Load the current TTY mode in GsmCdmaPhone based on Telecom and UI settings.
     */
    private void loadTtyMode() {
        int ttyMode = TelecomManager.TTY_MODE_OFF;
        TelecomManager telecomManager = mContext.getSystemService(TelecomManager.class);
        if (telecomManager != null) {
            ttyMode = telecomManager.getCurrentTtyMode();
        }
        updateTtyMode(ttyMode);
        //Get preferred TTY mode from settings as UI Tty mode is always user preferred Tty mode.
        ttyMode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.PREFERRED_TTY_MODE, TelecomManager.TTY_MODE_OFF);
        updateUiTtyMode(ttyMode);
    }

    private void reapplyUiccAppsEnablementIfNeeded(int retries) {
        UiccSlot slot = mUiccController.getUiccSlotForPhone(mPhoneId);

        // If no card is present or we don't have mUiccApplicationsEnabled yet, do nothing.
        if (slot == null || slot.getCardState() != IccCardStatus.CardState.CARDSTATE_PRESENT
                || mUiccApplicationsEnabled == null) {
            return;
        }

        // Due to timing issue, sometimes UiccPort is coming null, so don't use UiccPort object
        // to retrieve the iccId here. Instead, depend on the UiccSlot API.
        String iccId = slot.getIccId(slot.getPortIndexFromPhoneId(mPhoneId));
        if (iccId == null) {
            loge("reapplyUiccAppsEnablementIfNeeded iccId is null, phoneId: " + mPhoneId
                    + " portIndex: " + slot.getPortIndexFromPhoneId(mPhoneId));
            return;
        }

        SubscriptionInfo info = SubscriptionController.getInstance().getSubInfoForIccId(
                IccUtils.stripTrailingFs(iccId));

        // If info is null, it could be a new subscription. By default we enable it.
        boolean expectedValue = info == null ? true : info.areUiccApplicationsEnabled();

        // If for any reason current state is different from configured state, re-apply the
        // configured state.
        if (expectedValue != mUiccApplicationsEnabled) {
            mCi.enableUiccApplications(expectedValue, Message.obtain(
                    this, EVENT_REAPPLY_UICC_APPS_ENABLEMENT_DONE,
                    new Pair<Boolean, Integer>(expectedValue, retries)));
        }
    }

    // Enable or disable uicc applications.
    @Override
    public void enableUiccApplications(boolean enable, Message onCompleteMessage) {
        // First check if card is present. Otherwise mUiccApplicationsDisabled doesn't make
        // any sense.
        UiccSlot slot = mUiccController.getUiccSlotForPhone(mPhoneId);
        if (slot == null || slot.getCardState() != IccCardStatus.CardState.CARDSTATE_PRESENT) {
            if (onCompleteMessage != null) {
                AsyncResult.forMessage(onCompleteMessage, null,
                        new IllegalStateException("No SIM card is present"));
                onCompleteMessage.sendToTarget();
            }
            return;
        }

        mCi.enableUiccApplications(enable, onCompleteMessage);
    }

    /**
     * Whether disabling a physical subscription is supported or not.
     */
    @Override
    public boolean canDisablePhysicalSubscription() {
        return mCi.canToggleUiccApplicationsEnablement();
    }

    @Override
    public @NonNull List<String> getEquivalentHomePlmns() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            if (r != null && r.getEhplmns() != null) {
                return Arrays.asList(r.getEhplmns());
            }
        } else if (isPhoneTypeCdma()) {
            loge("EHPLMN is not available in CDMA");
        }
        return Collections.emptyList();
    }

    /**
     * @return Currently bound data service package names.
     */
    public @NonNull List<String> getDataServicePackages() {
        if (isUsingNewDataStack()) {
            return getDataNetworkController().getDataServicePackages();
        }
        List<String> packages = new ArrayList<>();
        int[] transports = new int[]{AccessNetworkConstants.TRANSPORT_TYPE_WWAN,
                AccessNetworkConstants.TRANSPORT_TYPE_WLAN};

        for (int transport : transports) {
            DcTracker dct = getDcTracker(transport);
            if (dct != null) {
                String pkg = dct.getDataServiceManager().getDataServicePackageName();
                if (!TextUtils.isEmpty(pkg)) {
                    packages.add(pkg);
                }
            }
        }

        return packages;
    }

    private void updateBroadcastEmergencyCallStateChangesAfterCarrierConfigChanged(
            PersistableBundle config) {
        if (config == null) {
            loge("didn't get broadcastEmergencyCallStateChanges from carrier config");
            return;
        }

        // get broadcastEmergencyCallStateChanges
        boolean broadcastEmergencyCallStateChanges = config.getBoolean(
                CarrierConfigManager.KEY_BROADCAST_EMERGENCY_CALL_STATE_CHANGES_BOOL);
        logd("broadcastEmergencyCallStateChanges = " + broadcastEmergencyCallStateChanges);
        setBroadcastEmergencyCallStateChanges(broadcastEmergencyCallStateChanges);
    }

    private void updateNrSettingsAfterCarrierConfigChanged(PersistableBundle config) {
        if (config == null) {
            loge("didn't get the carrier_nr_availability_int from the carrier config.");
            return;
        }
        int[] nrAvailabilities = config.getIntArray(
                CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
        mIsCarrierNrSupported = !ArrayUtils.isEmpty(nrAvailabilities);
    }

    private void updateVoNrSettings(PersistableBundle config) {
        UiccSlot slot = mUiccController.getUiccSlotForPhone(mPhoneId);

        // If no card is present, do nothing.
        if (slot == null || slot.getCardState() != IccCardStatus.CardState.CARDSTATE_PRESENT) {
            return;
        }

        if (config == null) {
            loge("didn't get the vonr_enabled_bool from the carrier config.");
            return;
        }

        boolean mIsVonrEnabledByCarrier =
                config.getBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL);

        String result = SubscriptionController.getInstance().getSubscriptionProperty(
                getSubId(),
                SubscriptionManager.NR_ADVANCED_CALLING_ENABLED);

        int setting = -1;
        if (result != null) {
            setting = Integer.parseInt(result);
        }

        logd("VoNR setting from telephony.db:"
                + setting
                + " ,vonr_enabled_bool:"
                + mIsVonrEnabledByCarrier);

        if (!mIsVonrEnabledByCarrier) {
            mCi.setVoNrEnabled(false, obtainMessage(EVENT_SET_VONR_ENABLED_DONE), null);
        } else if (setting == 1 || setting == -1) {
            mCi.setVoNrEnabled(true, obtainMessage(EVENT_SET_VONR_ENABLED_DONE), null);
        } else if (setting == 0) {
            mCi.setVoNrEnabled(false, obtainMessage(EVENT_SET_VONR_ENABLED_DONE), null);
        }
    }

    private void updateCdmaRoamingSettingsAfterCarrierConfigChanged(PersistableBundle config) {
        if (config == null) {
            loge("didn't get the cdma_roaming_mode changes from the carrier config.");
            return;
        }

        // Changing the cdma roaming settings based carrier config.
        int config_cdma_roaming_mode = config.getInt(
                CarrierConfigManager.KEY_CDMA_ROAMING_MODE_INT);
        int current_cdma_roaming_mode =
                Settings.Global.getInt(getContext().getContentResolver(),
                        Settings.Global.CDMA_ROAMING_MODE,
                        TelephonyManager.CDMA_ROAMING_MODE_RADIO_DEFAULT);
        switch (config_cdma_roaming_mode) {
            // Carrier's cdma_roaming_mode will overwrite the user's previous settings
            // Keep the user's previous setting in global variable which will be used
            // when carrier's setting is turn off.
            case TelephonyManager.CDMA_ROAMING_MODE_HOME:
            case TelephonyManager.CDMA_ROAMING_MODE_AFFILIATED:
            case TelephonyManager.CDMA_ROAMING_MODE_ANY:
                logd("cdma_roaming_mode is going to changed to "
                        + config_cdma_roaming_mode);
                setCdmaRoamingPreference(config_cdma_roaming_mode,
                        obtainMessage(EVENT_SET_ROAMING_PREFERENCE_DONE));
                break;

            // When carrier's setting is turn off, change the cdma_roaming_mode to the
            // previous user's setting
            case TelephonyManager.CDMA_ROAMING_MODE_RADIO_DEFAULT:
                if (current_cdma_roaming_mode != config_cdma_roaming_mode) {
                    logd("cdma_roaming_mode is going to changed to "
                            + current_cdma_roaming_mode);
                    setCdmaRoamingPreference(current_cdma_roaming_mode,
                            obtainMessage(EVENT_SET_ROAMING_PREFERENCE_DONE));
                }
                break;
            default:
                loge("Invalid cdma_roaming_mode settings: " + config_cdma_roaming_mode);
        }
    }

    /**
     * Determines if IMS is enabled for call.
     *
     * @return {@code true} if IMS calling is enabled.
     */
    public boolean isImsUseEnabled() {
        ImsManager imsManager = mImsManagerFactory.create(mContext, mPhoneId);
        boolean imsUseEnabled = ((imsManager.isVolteEnabledByPlatform()
                && imsManager.isEnhanced4gLteModeSettingEnabledByUser())
                || (imsManager.isWfcEnabledByPlatform() && imsManager.isWfcEnabledByUser())
                && imsManager.isNonTtyOrTtyOnVolteEnabled());
        return imsUseEnabled;
    }

    @Override
    public InboundSmsHandler getInboundSmsHandler(boolean is3gpp2) {
        return mIccSmsInterfaceManager.getInboundSmsHandler(is3gpp2);
    }
}

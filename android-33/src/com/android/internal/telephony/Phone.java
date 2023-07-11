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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.BroadcastOptions;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.sysprop.TelephonyProperties;
import android.telecom.VideoProfile;
import android.telephony.AccessNetworkConstants;
import android.telephony.Annotation.ApnType;
import android.telephony.CarrierConfigManager;
import android.telephony.CarrierRestrictionRules;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.ClientRequestStats;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.LinkCapacityEstimate;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PhoneStateListener;
import android.telephony.PhysicalChannelConfig;
import android.telephony.PreciseDataConnectionState;
import android.telephony.RadioAccessFamily;
import android.telephony.RadioAccessSpecifier;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;

import com.android.ims.ImsCall;
import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.data.AccessNetworksManager;
import com.android.internal.telephony.data.DataNetworkController;
import com.android.internal.telephony.data.DataSettingsManager;
import com.android.internal.telephony.data.LinkBandwidthEstimator;
import com.android.internal.telephony.dataconnection.DataConnectionReasons;
import com.android.internal.telephony.dataconnection.DataEnabledSettings;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.TransportManager;
import com.android.internal.telephony.emergency.EmergencyNumberTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.metrics.SmsStats;
import com.android.internal.telephony.metrics.VoiceCallSessionStats;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccPort;
import com.android.internal.telephony.uicc.UsimServiceTable;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.XmlUtils;
import com.android.telephony.Rlog;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * (<em>Not for SDK use</em>)
 * A base implementation for the com.android.internal.telephony.Phone interface.
 *
 * Note that implementations of Phone.java are expected to be used
 * from a single application thread. This should be the same thread that
 * originally called PhoneFactory to obtain the interface.
 *
 *  {@hide}
 *
 */

public abstract class Phone extends Handler implements PhoneInternalInterface {
    private static final String LOG_TAG = "Phone";

    protected final static Object lockForRadioTechnologyChange = new Object();

    protected final int USSD_MAX_QUEUE = 10;

    // Key used to read and write the saved network selection numeric value
    public static final String NETWORK_SELECTION_KEY = "network_selection_key";
    // Key used to read and write the saved network selection operator name
    public static final String NETWORK_SELECTION_NAME_KEY = "network_selection_name_key";
    // Key used to read and write the saved network selection operator short name
    public static final String NETWORK_SELECTION_SHORT_KEY = "network_selection_short_key";


    // Key used to read/write "disable data connection on boot" pref (used for testing)
    public static final String DATA_DISABLED_ON_BOOT_KEY = "disabled_on_boot_key";

    // Key used to read/write data_roaming_is_user_setting pref
    public static final String DATA_ROAMING_IS_USER_SETTING_KEY =
            "data_roaming_is_user_setting_key";

    // Default value when there has been no last emergency SMS time recorded yet.
    private static final int EMERGENCY_SMS_NO_TIME_RECORDED = -1;
    // The max timer value that the platform can be in emergency SMS mode (5 minutes).
    private static final int EMERGENCY_SMS_TIMER_MAX_MS = 300000;

    /* Event Constants */
    protected static final int EVENT_RADIO_AVAILABLE             = 1;
    /** Supplementary Service Notification received. */
    protected static final int EVENT_SSN                         = 2;
    protected static final int EVENT_SIM_RECORDS_LOADED          = 3;
    private static final int EVENT_MMI_DONE                      = 4;
    protected static final int EVENT_RADIO_ON                    = 5;
    protected static final int EVENT_GET_BASEBAND_VERSION_DONE   = 6;
    protected static final int EVENT_USSD                        = 7;
    protected static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE  = 8;
    private static final int EVENT_GET_SIM_STATUS_DONE           = 11;
    protected static final int EVENT_SET_CALL_FORWARD_DONE       = 12;
    protected static final int EVENT_GET_CALL_FORWARD_DONE       = 13;
    protected static final int EVENT_CALL_RING                   = 14;
    private static final int EVENT_CALL_RING_CONTINUE            = 15;

    // Used to intercept the carrier selection calls so that
    // we can save the values.
    private static final int EVENT_SET_NETWORK_MANUAL_COMPLETE      = 16;
    private static final int EVENT_SET_NETWORK_AUTOMATIC_COMPLETE   = 17;
    protected static final int EVENT_SET_CLIR_COMPLETE              = 18;
    protected static final int EVENT_REGISTERED_TO_NETWORK          = 19;
    protected static final int EVENT_SET_VM_NUMBER_DONE             = 20;
    // Events for CDMA support
    protected static final int EVENT_GET_DEVICE_IDENTITY_DONE       = 21;
    protected static final int EVENT_RUIM_RECORDS_LOADED            = 22;
    protected static final int EVENT_NV_READY                       = 23;
    private static final int EVENT_SET_ENHANCED_VP                  = 24;
    @VisibleForTesting
    public static final int EVENT_EMERGENCY_CALLBACK_MODE_ENTER  = 25;
    protected static final int EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE = 26;
    protected static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 27;
    // other
    protected static final int EVENT_SET_NETWORK_AUTOMATIC          = 28;
    protected static final int EVENT_ICC_RECORD_EVENTS              = 29;
    @VisibleForTesting
    protected static final int EVENT_ICC_CHANGED                    = 30;
    // Single Radio Voice Call Continuity
    @VisibleForTesting
    protected static final int EVENT_SRVCC_STATE_CHANGED             = 31;
    private static final int EVENT_INITIATE_SILENT_REDIAL           = 32;
    private static final int EVENT_RADIO_NOT_AVAILABLE              = 33;
    private static final int EVENT_UNSOL_OEM_HOOK_RAW               = 34;
    protected static final int EVENT_GET_RADIO_CAPABILITY           = 35;
    protected static final int EVENT_SS                             = 36;
    private static final int EVENT_CONFIG_LCE                       = 37;
    private static final int EVENT_CHECK_FOR_NETWORK_AUTOMATIC      = 38;
    protected static final int EVENT_VOICE_RADIO_TECH_CHANGED       = 39;
    protected static final int EVENT_REQUEST_VOICE_RADIO_TECH_DONE  = 40;
    protected static final int EVENT_RIL_CONNECTED                  = 41;
    protected static final int EVENT_UPDATE_PHONE_OBJECT            = 42;
    protected static final int EVENT_CARRIER_CONFIG_CHANGED         = 43;
    // Carrier's CDMA prefer mode setting
    protected static final int EVENT_SET_ROAMING_PREFERENCE_DONE    = 44;
    protected static final int EVENT_MODEM_RESET                    = 45;
    protected static final int EVENT_VRS_OR_RAT_CHANGED             = 46;
    // Radio state change
    protected static final int EVENT_RADIO_STATE_CHANGED            = 47;
    protected static final int EVENT_SET_CARRIER_DATA_ENABLED       = 48;
    protected static final int EVENT_DEVICE_PROVISIONED_CHANGE      = 49;
    protected static final int EVENT_DEVICE_PROVISIONING_DATA_SETTING_CHANGE = 50;
    protected static final int EVENT_GET_AVAILABLE_NETWORKS_DONE    = 51;

    private static final int EVENT_ALL_DATA_DISCONNECTED                  = 52;
    protected static final int EVENT_UICC_APPS_ENABLEMENT_STATUS_CHANGED  = 53;
    protected static final int EVENT_UICC_APPS_ENABLEMENT_SETTING_CHANGED = 54;
    protected static final int EVENT_GET_UICC_APPS_ENABLEMENT_DONE        = 55;
    protected static final int EVENT_REAPPLY_UICC_APPS_ENABLEMENT_DONE    = 56;
    protected static final int EVENT_REGISTRATION_FAILED = 57;
    protected static final int EVENT_BARRING_INFO_CHANGED = 58;
    protected static final int EVENT_LINK_CAPACITY_CHANGED = 59;
    protected static final int EVENT_RESET_CARRIER_KEY_IMSI_ENCRYPTION = 60;
    protected static final int EVENT_SET_VONR_ENABLED_DONE = 61;
    protected static final int EVENT_SUBSCRIPTIONS_CHANGED = 62;
    protected static final int EVENT_GET_USAGE_SETTING_DONE = 63;
    protected static final int EVENT_SET_USAGE_SETTING_DONE = 64;

    protected static final int EVENT_LAST = EVENT_SET_USAGE_SETTING_DONE;

    // For shared prefs.
    private static final String GSM_ROAMING_LIST_OVERRIDE_PREFIX = "gsm_roaming_list_";
    private static final String GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX = "gsm_non_roaming_list_";
    private static final String CDMA_ROAMING_LIST_OVERRIDE_PREFIX = "cdma_roaming_list_";
    private static final String CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX = "cdma_non_roaming_list_";

    // Key used to read/write current CLIR setting
    public static final String CLIR_KEY = "clir_sub_key";

    // Key used for storing voice mail count
    private static final String VM_COUNT = "vm_count_key";
    // Key used to read/write the ID for storing the voice mail
    private static final String VM_ID = "vm_id_key";

    // Key used for storing call forwarding status
    public static final String CF_STATUS = "cf_status_key";
    // Key used to read/write the ID for storing the call forwarding status
    public static final String CF_ID = "cf_id_key";

    // Key used to read/write "disable DNS server check" pref (used for testing)
    private static final String DNS_SERVER_CHECK_DISABLED_KEY = "dns_server_check_disabled_key";

    // Integer used to let the calling application know that the we are ignoring auto mode switch.
    private static final int ALREADY_IN_AUTO_SELECTION = 1;

    /**
     * This method is invoked when the Phone exits Emergency Callback Mode.
     */
    protected void handleExitEmergencyCallbackMode() {
    }

    /**
     * Small container class used to hold information relevant to
     * the carrier selection process. operatorNumeric can be ""
     * if we are looking for automatic selection. operatorAlphaLong is the
     * corresponding operator name.
     */
    protected static class NetworkSelectMessage {
        public Message message;
        public String operatorNumeric;
        public String operatorAlphaLong;
        public String operatorAlphaShort;
    }

    public static class SilentRedialParam {
        public String dialString;
        public int causeCode;
        public DialArgs dialArgs;

        public SilentRedialParam(String dialString, int causeCode, DialArgs dialArgs) {
            this.dialString = dialString;
            this.causeCode = causeCode;
            this.dialArgs = dialArgs;
        }
    }

    /* Instance Variables */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public CommandsInterface mCi;
    protected int mVmCount = 0;
    private boolean mDnsCheckDisabled;
    // Data connection trackers. For each transport type (e.g. WWAN, WLAN), there will be a
    // corresponding DcTracker. The WWAN DcTracker is for cellular data connections while
    // WLAN DcTracker is for IWLAN data connection. For IWLAN legacy mode, only one (WWAN) DcTracker
    // will be created.
    protected final SparseArray<DcTracker> mDcTrackers = new SparseArray<>();
    protected DataNetworkController mDataNetworkController;
    /* Used for dispatching signals to configured carrier apps */
    protected CarrierSignalAgent mCarrierSignalAgent;
    /* Used for dispatching carrier action from carrier apps */
    protected CarrierActionAgent mCarrierActionAgent;
    private boolean mDoesRilSendMultipleCallRing;
    private int mCallRingContinueToken;
    private int mCallRingDelay;
    private boolean mIsVoiceCapable = true;
    private final AppSmsManager mAppSmsManager;
    private SimActivationTracker mSimActivationTracker;
    // Keep track of whether or not the phone is in Emergency Callback Mode for Phone and
    // subclasses
    protected boolean mIsPhoneInEcmState = false;
    // Keep track of the case where ECM was cancelled to place another outgoing emergency call.
    // We will need to restart it after the emergency call ends.
    protected boolean mEcmCanceledForEmergency = false;
    private volatile long mTimeLastEmergencySmsSentMs = EMERGENCY_SMS_NO_TIME_RECORDED;

    // Variable to cache the video capability. When RAT changes, we lose this info and are unable
    // to recover from the state. We cache it and notify listeners when they register.
    protected boolean mIsVideoCapable = false;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected UiccController mUiccController = null;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected final AtomicReference<IccRecords> mIccRecords = new AtomicReference<IccRecords>();
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public SmsStorageMonitor mSmsStorageMonitor;
    public SmsUsageMonitor mSmsUsageMonitor;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected AtomicReference<UiccCardApplication> mUiccApplication =
            new AtomicReference<UiccCardApplication>();
    TelephonyTester mTelephonyTester;
    private String mName;
    private final String mActionDetached;
    private final String mActionAttached;
    protected DeviceStateMonitor mDeviceStateMonitor;
    protected DisplayInfoController mDisplayInfoController;
    protected TransportManager mTransportManager;
    protected AccessNetworksManager mAccessNetworksManager;
    protected DataEnabledSettings mDataEnabledSettings;
    // Used for identify the carrier of current subscription
    protected CarrierResolver mCarrierResolver;
    protected SignalStrengthController mSignalStrengthController;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected int mPhoneId;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected Phone mImsPhone = null;

    private final AtomicReference<RadioCapability> mRadioCapability =
            new AtomicReference<RadioCapability>();

    private static final int DEFAULT_REPORT_INTERVAL_MS = 200;
    private static final boolean LCE_PULL_MODE = true;
    private int mLceStatus = RILConstants.LCE_NOT_AVAILABLE;
    protected TelephonyComponentFactory mTelephonyComponentFactory;

    private int mPreferredUsageSetting = SubscriptionManager.USAGE_SETTING_UNKNOWN;
    private int mUsageSettingFromModem = SubscriptionManager.USAGE_SETTING_UNKNOWN;
    private boolean mIsUsageSettingSupported = true;

    //IMS
    /**
     * {@link CallStateException} message text used to indicate that an IMS call has failed because
     * it needs to be retried using GSM or CDMA (e.g. CS fallback).
     * TODO: Replace this with a proper exception; {@link CallStateException} doesn't make sense.
     */
    public static final String CS_FALLBACK = "cs_fallback";

    // Used for retry over cs for supplementary services
    public static final String CS_FALLBACK_SS = "cs_fallback_ss";

    /**
     * @deprecated Use {@link android.telephony.ims.ImsManager#EXTRA_WFC_REGISTRATION_FAILURE_TITLE}
     * instead.
     */
    @Deprecated
    public static final String EXTRA_KEY_ALERT_TITLE =
            android.telephony.ims.ImsManager.EXTRA_WFC_REGISTRATION_FAILURE_TITLE;
    /**
     * @deprecated Use
     * {@link android.telephony.ims.ImsManager#EXTRA_WFC_REGISTRATION_FAILURE_MESSAGE} instead.
     */
    @Deprecated
    public static final String EXTRA_KEY_ALERT_MESSAGE =
            android.telephony.ims.ImsManager.EXTRA_WFC_REGISTRATION_FAILURE_MESSAGE;
    public static final String EXTRA_KEY_ALERT_SHOW = "alertShow";
    public static final String EXTRA_KEY_NOTIFICATION_MESSAGE = "notificationMessage";

    private final RegistrantList mPreciseCallStateRegistrants = new RegistrantList();

    private final RegistrantList mHandoverRegistrants = new RegistrantList();

    private final RegistrantList mNewRingingConnectionRegistrants = new RegistrantList();

    private final RegistrantList mIncomingRingRegistrants = new RegistrantList();

    protected final RegistrantList mDisconnectRegistrants = new RegistrantList();

    private final RegistrantList mServiceStateRegistrants = new RegistrantList();

    protected final RegistrantList mMmiCompleteRegistrants = new RegistrantList();

    @UnsupportedAppUsage
    protected final RegistrantList mMmiRegistrants = new RegistrantList();

    protected final RegistrantList mUnknownConnectionRegistrants = new RegistrantList();

    protected final RegistrantList mSuppServiceFailedRegistrants = new RegistrantList();

    protected final RegistrantList mRadioOffOrNotAvailableRegistrants = new RegistrantList();

    protected final RegistrantList mSimRecordsLoadedRegistrants = new RegistrantList();

    private final RegistrantList mVideoCapabilityChangedRegistrants = new RegistrantList();

    protected final RegistrantList mEmergencyCallToggledRegistrants = new RegistrantList();

    private final RegistrantList mAllDataDisconnectedRegistrants = new RegistrantList();

    private final RegistrantList mCellInfoRegistrants = new RegistrantList();

    private final RegistrantList mRedialRegistrants = new RegistrantList();

    private final RegistrantList mPhysicalChannelConfigRegistrants = new RegistrantList();

    private final RegistrantList mOtaspRegistrants = new RegistrantList();

    private final RegistrantList mPreferredNetworkTypeRegistrants = new RegistrantList();

    protected Registrant mPostDialHandler;

    protected final LocalLog mLocalLog;

    private Looper mLooper; /* to insure registrants are in correct thread*/

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected final Context mContext;

    /**
     * PhoneNotifier is an abstraction for all system-wide
     * state change notification. DefaultPhoneNotifier is
     * used here unless running we're inside a unit test.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected PhoneNotifier mNotifier;

    protected SimulatedRadioControl mSimulatedRadioControl;

    private Map<Integer, Long> mAllowedNetworkTypesForReasons = new HashMap<>();
    private static final String ALLOWED_NETWORK_TYPES_TEXT_USER = "user";
    private static final String ALLOWED_NETWORK_TYPES_TEXT_POWER = "power";
    private static final String ALLOWED_NETWORK_TYPES_TEXT_CARRIER = "carrier";
    private static final String ALLOWED_NETWORK_TYPES_TEXT_ENABLE_2G = "enable_2g";
    private static final int INVALID_ALLOWED_NETWORK_TYPES = -1;
    protected boolean mIsCarrierNrSupported = false;
    protected boolean mIsAllowedNetworkTypesLoadedFromDb = false;
    private boolean mUnitTestMode;

    protected VoiceCallSessionStats mVoiceCallSessionStats;
    protected SmsStats mSmsStats;

    protected LinkBandwidthEstimator mLinkBandwidthEstimator;

    /** The flag indicating using the new data stack or not. */
    // This flag and the old data stack code will be deleted in Android 14.
    private final boolean mNewDataStackEnabled;

    public IccRecords getIccRecords() {
        return mIccRecords.get();
    }

    /**
     * Returns a string identifier for this phone interface for parties
     *  outside the phone app process.
     *  @return The string name.
     */
    @UnsupportedAppUsage
    public String getPhoneName() {
        return mName;
    }

    protected void setPhoneName(String name) {
        mName = name;
    }

    /**
     * Retrieves Nai for phones. Returns null if Nai is not set.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public String getNai(){
         return null;
    }

    /**
     * Return the ActionDetached string. When this action is received by components
     * they are to simulate detaching from the network.
     *
     * @return com.android.internal.telephony.{mName}.action_detached
     *          {mName} is GSM, CDMA ...
     */
    public String getActionDetached() {
        return mActionDetached;
    }

    /**
     * Return the ActionAttached string. When this action is received by components
     * they are to simulate attaching to the network.
     *
     * @return com.android.internal.telephony.{mName}.action_detached
     *          {mName} is GSM, CDMA ...
     */
    public String getActionAttached() {
        return mActionAttached;
    }

    /**
     * Set a system property, unless we're in unit test mode
     */
    // CAF_MSIM TODO this need to be replated with TelephonyManager API ?
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public String getSystemProperty(String property, String defValue) {
        if(getUnitTestMode()) {
            return null;
        }
        return SystemProperties.get(property, defValue);
    }

    /**
     * Constructs a Phone in normal (non-unit test) mode.
     *
     * @param notifier An instance of DefaultPhoneNotifier,
     * @param context Context object from hosting application
     * unless unit testing.
     * @param ci is CommandsInterface
     * @param unitTestMode when true, prevents notifications
     * of state change events
     */
    protected Phone(String name, PhoneNotifier notifier, Context context, CommandsInterface ci,
                    boolean unitTestMode) {
        this(name, notifier, context, ci, unitTestMode, SubscriptionManager.DEFAULT_PHONE_INDEX,
                TelephonyComponentFactory.getInstance());
    }

    /**
     * Constructs a Phone in normal (non-unit test) mode.
     *
     * @param notifier An instance of DefaultPhoneNotifier,
     * @param context Context object from hosting application
     * unless unit testing.
     * @param ci is CommandsInterface
     * @param unitTestMode when true, prevents notifications
     * of state change events
     * @param phoneId the phone-id of this phone.
     */
    protected Phone(String name, PhoneNotifier notifier, Context context, CommandsInterface ci,
                    boolean unitTestMode, int phoneId,
                    TelephonyComponentFactory telephonyComponentFactory) {
        mPhoneId = phoneId;
        mName = name;
        mNotifier = notifier;
        mContext = context;
        mLooper = Looper.myLooper();
        mCi = ci;
        mActionDetached = this.getClass().getPackage().getName() + ".action_detached";
        mActionAttached = this.getClass().getPackage().getName() + ".action_attached";
        mAppSmsManager = telephonyComponentFactory.inject(AppSmsManager.class.getName())
                .makeAppSmsManager(context);
        mLocalLog = new LocalLog(64);

        if (TelephonyUtils.IS_DEBUGGABLE) {
            mTelephonyTester = new TelephonyTester(this);
        }

        setUnitTestMode(unitTestMode);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        mDnsCheckDisabled = sp.getBoolean(DNS_SERVER_CHECK_DISABLED_KEY, false);
        mCi.setOnCallRing(this, EVENT_CALL_RING, null);

        /* "Voice capable" means that this device supports circuit-switched
        * (i.e. voice) phone calls over the telephony network, and is allowed
        * to display the in-call UI while a cellular voice call is active.
        * This will be false on "data only" devices which can't make voice
        * calls and don't support any in-call UI.
        */
        mIsVoiceCapable = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
                .isVoiceCapable();

        /**
         *  Some RIL's don't always send RIL_UNSOL_CALL_RING so it needs
         *  to be generated locally. Ideally all ring tones should be loops
         * and this wouldn't be necessary. But to minimize changes to upper
         * layers it is requested that it be generated by lower layers.
         *
         * By default old phones won't have the property set but do generate
         * the RIL_UNSOL_CALL_RING so the default if there is no property is
         * true.
         */
        mDoesRilSendMultipleCallRing = TelephonyProperties.ril_sends_multiple_call_ring()
                .orElse(true);
        Rlog.d(LOG_TAG, "mDoesRilSendMultipleCallRing=" + mDoesRilSendMultipleCallRing);

        mCallRingDelay = TelephonyProperties.call_ring_delay().orElse(3000);
        Rlog.d(LOG_TAG, "mCallRingDelay=" + mCallRingDelay);

        // Initialize SMS stats
        mSmsStats = new SmsStats(this);

        mNewDataStackEnabled = !mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_force_disable_telephony_new_data_stack);

        if (getPhoneType() == PhoneConstants.PHONE_TYPE_IMS) {
            return;
        }

        // Initialize device storage and outgoing SMS usage monitors for SMSDispatchers.
        mTelephonyComponentFactory = telephonyComponentFactory;
        mSmsStorageMonitor = mTelephonyComponentFactory.inject(SmsStorageMonitor.class.getName())
                .makeSmsStorageMonitor(this);
        mSmsUsageMonitor = mTelephonyComponentFactory.inject(SmsUsageMonitor.class.getName())
                .makeSmsUsageMonitor(context);
        mUiccController = UiccController.getInstance();
        mUiccController.registerForIccChanged(this, EVENT_ICC_CHANGED, null);
        mSimActivationTracker = mTelephonyComponentFactory
                .inject(SimActivationTracker.class.getName())
                .makeSimActivationTracker(this);
        if (getPhoneType() != PhoneConstants.PHONE_TYPE_SIP) {
            mCi.registerForSrvccStateChanged(this, EVENT_SRVCC_STATE_CHANGED, null);
        }
        mCi.startLceService(DEFAULT_REPORT_INTERVAL_MS, LCE_PULL_MODE,
                obtainMessage(EVENT_CONFIG_LCE));
    }

    /**
     * Start setup of ImsPhone, which will start trying to connect to the ImsResolver. Will not be
     * called if this device does not support FEATURE_IMS_TELEPHONY.
     */
    public void createImsPhone() {
        if (getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
            return;
        }

        synchronized(Phone.lockForRadioTechnologyChange) {
            if (mImsPhone == null) {
                mImsPhone = PhoneFactory.makeImsPhone(mNotifier, this);
                CallManager.getInstance().registerPhone(mImsPhone);
                mImsPhone.registerForSilentRedial(
                        this, EVENT_INITIATE_SILENT_REDIAL, null);
            }
        }
    }

    /**
     * Checks if device should convert CDMA Caller ID restriction related MMI codes to
     * equivalent 3GPP MMI Codes that provide same functionality when device is roaming.
     * This method should only return true on multi-mode devices when carrier requires this
     * conversion to be done on the device.
     *
     * @return true when carrier config
     * "KEY_CONVERT_CDMA_CALLER_ID_MMI_CODES_WHILE_ROAMING_ON_3GPP_BOOL" is set to true
     */
    public boolean supportsConversionOfCdmaCallerIdMmiCodesWhileRoaming() {
        CarrierConfigManager configManager = (CarrierConfigManager)
                getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configManager.getConfigForSubId(getSubId());
        if (b != null) {
            return b.getBoolean(
                    CarrierConfigManager
                            .KEY_CONVERT_CDMA_CALLER_ID_MMI_CODES_WHILE_ROAMING_ON_3GPP_BOOL,
                    false);
        } else {
            // Default value set in CarrierConfigManager
            return false;
        }
    }

    /**
     * Check if sending CLIR activation("*31#") and deactivation("#31#") code only without dialing
     * number is prevented.
     *
     * @return {@code true} when carrier config
     * "KEY_PREVENT_CLIR_ACTIVATION_AND_DEACTIVATION_CODE_BOOL" is set to {@code true}
     */
    public boolean isClirActivationAndDeactivationPrevented() {
        CarrierConfigManager configManager = (CarrierConfigManager)
                getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configManager.getConfigForSubId(getSubId());
        if (b == null) {
            b = CarrierConfigManager.getDefaultConfig();
        }
        return b.getBoolean(
                CarrierConfigManager.KEY_PREVENT_CLIR_ACTIVATION_AND_DEACTIVATION_CODE_BOOL);
    }

    /**
     * When overridden the derived class needs to call
     * super.handleMessage(msg) so this method has a
     * a chance to process the message.
     *
     * @param msg
     */
    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;

        // messages to be handled whether or not the phone is being destroyed
        // should only include messages which are being re-directed and do not use
        // resources of the phone being destroyed
        switch (msg.what) {
            // handle the select network completion callbacks.
            case EVENT_SET_NETWORK_MANUAL_COMPLETE:
            case EVENT_SET_NETWORK_AUTOMATIC_COMPLETE:
                handleSetSelectNetwork((AsyncResult) msg.obj);
                return;
        }

        switch(msg.what) {
            case EVENT_CALL_RING:
                Rlog.d(LOG_TAG, "Event EVENT_CALL_RING Received state=" + getState());
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    PhoneConstants.State state = getState();
                    if ((!mDoesRilSendMultipleCallRing)
                            && ((state == PhoneConstants.State.RINGING) ||
                                    (state == PhoneConstants.State.IDLE))) {
                        mCallRingContinueToken += 1;
                        sendIncomingCallRingNotification(mCallRingContinueToken);
                    } else {
                        notifyIncomingRing();
                    }
                }
                break;

            case EVENT_CALL_RING_CONTINUE:
                Rlog.d(LOG_TAG, "Event EVENT_CALL_RING_CONTINUE Received state=" + getState());
                if (getState() == PhoneConstants.State.RINGING) {
                    sendIncomingCallRingNotification(msg.arg1);
                }
                break;

            case EVENT_ICC_CHANGED:
                onUpdateIccAvailability();
                break;

            case EVENT_INITIATE_SILENT_REDIAL:
                // This is an ImsPhone -> GsmCdmaPhone redial
                // See ImsPhone#initiateSilentRedial
                Rlog.d(LOG_TAG, "Event EVENT_INITIATE_SILENT_REDIAL Received");
                ar = (AsyncResult) msg.obj;
                if ((ar.exception == null) && (ar.result != null)) {
                    SilentRedialParam result = (SilentRedialParam) ar.result;
                    String dialString = result.dialString;
                    int causeCode = result.causeCode;
                    DialArgs dialArgs = result.dialArgs;
                    if (TextUtils.isEmpty(dialString)) return;
                    try {
                        Connection cn = dialInternal(dialString, dialArgs);
                        // The ImsPhoneConnection that is owned by the ImsPhone is currently the
                        // one with a callback registered to TelephonyConnection. Notify the
                        // redial happened over that Phone so that it can be replaced with the
                        // new GSM/CDMA Connection.
                        Rlog.d(LOG_TAG, "Notify redial connection changed cn: " + cn);
                        if (mImsPhone != null) {
                            // Don't care it is null or not.
                            mImsPhone.notifyRedialConnectionChanged(cn);
                        }
                    } catch (CallStateException e) {
                        Rlog.e(LOG_TAG, "silent redial failed: " + e);
                        if (mImsPhone != null) {
                            mImsPhone.notifyRedialConnectionChanged(null);
                        }
                    }
                }
                break;

            case EVENT_SRVCC_STATE_CHANGED:
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    handleSrvccStateChanged((int[]) ar.result);
                } else {
                    Rlog.e(LOG_TAG, "Srvcc exception: " + ar.exception);
                }
                break;

            case EVENT_UNSOL_OEM_HOOK_RAW:
                // deprecated, ignore
                break;

            case EVENT_CONFIG_LCE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Rlog.d(LOG_TAG, "config LCE service failed: " + ar.exception);
                } else {
                    final ArrayList<Integer> statusInfo = (ArrayList<Integer>)ar.result;
                    mLceStatus = statusInfo.get(0);
                }
                break;

            case EVENT_CHECK_FOR_NETWORK_AUTOMATIC: {
                onCheckForNetworkSelectionModeAutomatic(msg);
                break;
            }

            case EVENT_ALL_DATA_DISCONNECTED:
                if (areAllDataDisconnected()) {
                    mAllDataDisconnectedRegistrants.notifyRegistrants();
                }
                break;
            case EVENT_GET_USAGE_SETTING_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    try {
                        mUsageSettingFromModem = ((int[]) ar.result)[0];
                    } catch (NullPointerException | ClassCastException e) {
                        Rlog.e(LOG_TAG, "Invalid response for usage setting " + ar.result);
                        break;
                    }

                    logd("Received mUsageSettingFromModem=" + mUsageSettingFromModem);
                    if (mUsageSettingFromModem != mPreferredUsageSetting) {
                        mCi.setUsageSetting(obtainMessage(EVENT_SET_USAGE_SETTING_DONE),
                                mPreferredUsageSetting);
                    }
                } else {
                    try {
                        CommandException ce = (CommandException) ar.exception;
                        if (ce.getCommandError() == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                            mIsUsageSettingSupported = false;
                        }
                        Rlog.w(LOG_TAG, "Unexpected failure to retrieve usage setting " + ce);
                    } catch (ClassCastException unused) {
                        Rlog.e(LOG_TAG, "Invalid Exception for usage setting " + ar.exception);
                        break; // technically extraneous, but good hygiene
                    }
                }
                break;
            case EVENT_SET_USAGE_SETTING_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    try {
                        CommandException ce = (CommandException) ar.exception;
                        if (ce.getCommandError() == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                            mIsUsageSettingSupported = false;
                        }
                        Rlog.w(LOG_TAG, "Unexpected failure to set usage setting " + ce);
                    } catch (ClassCastException unused) {
                        Rlog.e(LOG_TAG, "Invalid Exception for usage setting " + ar.exception);
                        break; // technically extraneous, but good hygiene
                    }
                }
                break;
            default:
                throw new RuntimeException("unexpected event not handled");
        }
    }

    public ArrayList<Connection> getHandoverConnection() {
        return null;
    }

    public void notifySrvccState(Call.SrvccState state) {
    }

    public void registerForSilentRedial(Handler h, int what, Object obj) {
    }

    public void unregisterForSilentRedial(Handler h) {
    }

    public void registerForVolteSilentRedial(Handler h, int what, Object obj) {
    }

    public void unregisterForVolteSilentRedial(Handler h) {
    }

    private void handleSrvccStateChanged(int[] ret) {
        Rlog.d(LOG_TAG, "handleSrvccStateChanged");

        ArrayList<Connection> conn = null;
        Phone imsPhone = mImsPhone;
        Call.SrvccState srvccState = Call.SrvccState.NONE;
        if (ret != null && ret.length != 0) {
            int state = ret[0];
            switch(state) {
                case TelephonyManager.SRVCC_STATE_HANDOVER_STARTED:
                    srvccState = Call.SrvccState.STARTED;
                    if (imsPhone != null) {
                        conn = imsPhone.getHandoverConnection();
                        migrateFrom(imsPhone);
                    } else {
                        Rlog.d(LOG_TAG, "HANDOVER_STARTED: mImsPhone null");
                    }
                    break;
                case TelephonyManager.SRVCC_STATE_HANDOVER_COMPLETED:
                    srvccState = Call.SrvccState.COMPLETED;
                    if (imsPhone != null) {
                        imsPhone.notifySrvccState(srvccState);
                    } else {
                        Rlog.d(LOG_TAG, "HANDOVER_COMPLETED: mImsPhone null");
                    }
                    break;
                case TelephonyManager.SRVCC_STATE_HANDOVER_FAILED:
                case TelephonyManager.SRVCC_STATE_HANDOVER_CANCELED:
                    srvccState = Call.SrvccState.FAILED;
                    break;

                default:
                    //ignore invalid state
                    return;
            }

            getCallTracker().notifySrvccState(srvccState, conn);

            notifySrvccStateChanged(state);
        }
    }

    /**
     * Gets the context for the phone, as set at initialization time.
     */
    @UnsupportedAppUsage
    public Context getContext() {
        return mContext;
    }

    // Will be called when icc changed
    protected abstract void onUpdateIccAvailability();

    /**
     * Disables the DNS check (i.e., allows "0.0.0.0").
     * Useful for lab testing environment.
     * @param b true disables the check, false enables.
     */
    public void disableDnsCheck(boolean b) {
        mDnsCheckDisabled = b;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(DNS_SERVER_CHECK_DISABLED_KEY, b);
        editor.apply();
    }

    /**
     * Returns true if the DNS check is currently disabled.
     */
    public boolean isDnsCheckDisabled() {
        return mDnsCheckDisabled;
    }

    /**
     * Register for getting notifications for change in the Call State {@link Call.State}
     * This is called PreciseCallState because the call state is more precise than the
     * {@link PhoneConstants.State} which can be obtained using the {@link PhoneStateListener}
     *
     * Resulting events will have an AsyncResult in <code>Message.obj</code>.
     * AsyncResult.userData will be set to the obj argument here.
     * The <em>h</em> parameter is held only by a weak reference.
     */
    @UnsupportedAppUsage
    public void registerForPreciseCallStateChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);

        mPreciseCallStateRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for voice call state change notifications.
     * Extraneous calls are tolerated silently.
     */
    @UnsupportedAppUsage
    public void unregisterForPreciseCallStateChanged(Handler h) {
        mPreciseCallStateRegistrants.remove(h);
    }

    /**
     * Subclasses of Phone probably want to replace this with a
     * version scoped to their packages
     */
    protected void notifyPreciseCallStateChangedP() {
        AsyncResult ar = new AsyncResult(null, this, null);
        mPreciseCallStateRegistrants.notifyRegistrants(ar);

        mNotifier.notifyPreciseCallState(this);
    }

    /**
     * Notifies when a Handover happens due to SRVCC or Silent Redial
     */
    public void registerForHandoverStateChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        mHandoverRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for handover state notifications
     */
    public void unregisterForHandoverStateChanged(Handler h) {
        mHandoverRegistrants.remove(h);
    }

    /**
     * Subclasses of Phone probably want to replace this with a
     * version scoped to their packages
     */
    public void notifyHandoverStateChanged(Connection cn) {
       AsyncResult ar = new AsyncResult(null, cn, null);
       mHandoverRegistrants.notifyRegistrants(ar);
    }

    /**
     * Notifies when a Handover happens due to Silent Redial
     */
    public void registerForRedialConnectionChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        mRedialRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for redial connection notifications
     */
    public void unregisterForRedialConnectionChanged(Handler h) {
        mRedialRegistrants.remove(h);
    }

    /**
     * Subclasses of Phone probably want to replace this with a
     * version scoped to their packages
     */
    public void notifyRedialConnectionChanged(Connection cn) {
        AsyncResult ar = new AsyncResult(null, cn, null);
        mRedialRegistrants.notifyRegistrants(ar);
    }

    protected void setIsInEmergencyCall() {
    }

    /**
     * Notify the phone that an SMS has been sent. This will be used determine if the SMS was sent
     * to an emergency address.
     * @param destinationAddress the address that the SMS was sent to.
     */
    public void notifySmsSent(String destinationAddress) {
        TelephonyManager m = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        if (m != null && m.isEmergencyNumber(destinationAddress)) {
            mLocalLog.log("Emergency SMS detected, recording time.");
            mTimeLastEmergencySmsSentMs = SystemClock.elapsedRealtime();
        }
    }

    /**
     * Determine if the Phone has recently sent an emergency SMS and is still in the interval of
     * time defined by a carrier that we may need to do perform special actions, for example
     * override user setting for location so the carrier can find the user's location for emergency
     * services.
     *
     * @return true if the device is in emergency SMS mode, false otherwise.
     */
    public boolean isInEmergencySmsMode() {
        long lastSmsTimeMs = mTimeLastEmergencySmsSentMs;
        if (lastSmsTimeMs == EMERGENCY_SMS_NO_TIME_RECORDED) {
            // an emergency SMS hasn't been sent since the last check.
            return false;
        }
        CarrierConfigManager configManager = (CarrierConfigManager)
                getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configManager.getConfigForSubId(getSubId());
        if (b == null) {
            // default for KEY_EMERGENCY_SMS_MODE_TIMER_MS_INT is 0 and CarrierConfig isn't
            // available, so return false.
            return false;
        }
        int eSmsTimerMs = b.getInt(CarrierConfigManager.KEY_EMERGENCY_SMS_MODE_TIMER_MS_INT, 0);
        if (eSmsTimerMs == 0) {
            // We do not support this feature for this carrier.
            return false;
        }
        if (eSmsTimerMs > EMERGENCY_SMS_TIMER_MAX_MS) {
            eSmsTimerMs = EMERGENCY_SMS_TIMER_MAX_MS;
        }
        boolean isInEmergencySmsMode = SystemClock.elapsedRealtime()
                <= (lastSmsTimeMs + eSmsTimerMs);
        if (!isInEmergencySmsMode) {
            // Shortcut this next time so we do not have to waste time if another emergency SMS
            // hasn't been sent since the last query.
            mTimeLastEmergencySmsSentMs = EMERGENCY_SMS_NO_TIME_RECORDED;
        } else {
            mLocalLog.log("isInEmergencySmsMode: queried while eSMS mode is active.");
        }
        return isInEmergencySmsMode;
    }

    protected void migrateFrom(Phone from) {
        migrate(mHandoverRegistrants, from.mHandoverRegistrants);
        migrate(mPreciseCallStateRegistrants, from.mPreciseCallStateRegistrants);
        migrate(mNewRingingConnectionRegistrants, from.mNewRingingConnectionRegistrants);
        migrate(mIncomingRingRegistrants, from.mIncomingRingRegistrants);
        migrate(mDisconnectRegistrants, from.mDisconnectRegistrants);
        migrate(mServiceStateRegistrants, from.mServiceStateRegistrants);
        migrate(mMmiCompleteRegistrants, from.mMmiCompleteRegistrants);
        migrate(mMmiRegistrants, from.mMmiRegistrants);
        migrate(mUnknownConnectionRegistrants, from.mUnknownConnectionRegistrants);
        migrate(mSuppServiceFailedRegistrants, from.mSuppServiceFailedRegistrants);
        migrate(mCellInfoRegistrants, from.mCellInfoRegistrants);
        migrate(mRedialRegistrants, from.mRedialRegistrants);
        // The emergency state of IMS phone will be cleared in ImsPhone#notifySrvccState after
        // receive SRVCC completed
        if (from.isInEmergencyCall()) {
            setIsInEmergencyCall();
        }
        setEcmCanceledForEmergency(from.isEcmCanceledForEmergency());
    }

    protected void migrate(RegistrantList to, RegistrantList from) {
        if (from == null) {
            // May be null in some cases, such as testing.
            return;
        }
        from.removeCleared();
        for (int i = 0, n = from.size(); i < n; i++) {
            Registrant r = (Registrant) from.get(i);
            Message msg = r.messageForRegistrant();
            // Since CallManager has already registered with both CS and IMS phones,
            // the migrate should happen only for those registrants which are not
            // registered with CallManager.Hence the below check is needed to add
            // only those registrants to the registrant list which are not
            // coming from the CallManager.
            if (msg != null) {
                if (msg.obj == CallManager.getInstance().getRegistrantIdentifier()) {
                    continue;
                } else {
                    to.add((Registrant) from.get(i));
                }
            } else {
                Rlog.d(LOG_TAG, "msg is null");
            }
        }
    }

    /**
     * Notifies when a previously untracked non-ringing/waiting connection has appeared.
     * This is likely due to some other entity (eg, SIM card application) initiating a call.
     */
    @UnsupportedAppUsage
    public void registerForUnknownConnection(Handler h, int what, Object obj) {
        checkCorrectThread(h);

        mUnknownConnectionRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for unknown connection notifications.
     */
    @UnsupportedAppUsage
    public void unregisterForUnknownConnection(Handler h) {
        mUnknownConnectionRegistrants.remove(h);
    }

    /**
     * Notifies when a new ringing or waiting connection has appeared.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = a Connection. <p>
     *  Please check Connection.isRinging() to make sure the Connection
     *  has not dropped since this message was posted.
     *  If Connection.isRinging() is true, then
     *   Connection.getCall() == Phone.getRingingCall()
     */
    @UnsupportedAppUsage
    public void registerForNewRingingConnection(
            Handler h, int what, Object obj) {
        checkCorrectThread(h);

        mNewRingingConnectionRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for new ringing connection notification.
     * Extraneous calls are tolerated silently
     */
    @UnsupportedAppUsage
    public void unregisterForNewRingingConnection(Handler h) {
        mNewRingingConnectionRegistrants.remove(h);
    }

    /**
     * Notifies when phone's video capabilities changes <p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = true if phone supports video calling <p>
     */
    public void registerForVideoCapabilityChanged(
            Handler h, int what, Object obj) {
        checkCorrectThread(h);

        mVideoCapabilityChangedRegistrants.addUnique(h, what, obj);

        // Notify any registrants of the cached video capability as soon as they register.
        notifyForVideoCapabilityChanged(mIsVideoCapable);
    }

    /**
     * Unregisters for video capability changed notification.
     * Extraneous calls are tolerated silently
     */
    public void unregisterForVideoCapabilityChanged(Handler h) {
        mVideoCapabilityChangedRegistrants.remove(h);
    }

    /**
     * Register for notifications when a sInCall VoicePrivacy is enabled
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj){
        mCi.registerForInCallVoicePrivacyOn(h, what, obj);
    }

    /**
     * Unegister for notifications when a sInCall VoicePrivacy is enabled
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForInCallVoicePrivacyOn(Handler h){
        mCi.unregisterForInCallVoicePrivacyOn(h);
    }

    /**
     * Register for notifications when a sInCall VoicePrivacy is disabled
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj){
        mCi.registerForInCallVoicePrivacyOff(h, what, obj);
    }

    /**
     * Unregister for notifications when a sInCall VoicePrivacy is disabled
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForInCallVoicePrivacyOff(Handler h){
        mCi.unregisterForInCallVoicePrivacyOff(h);
    }

    /**
     * Notifies when an incoming call rings.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = a Connection. <p>
     */
    @UnsupportedAppUsage
    public void registerForIncomingRing(
            Handler h, int what, Object obj) {
        checkCorrectThread(h);

        mIncomingRingRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for ring notification.
     * Extraneous calls are tolerated silently
     */
    @UnsupportedAppUsage
    public void unregisterForIncomingRing(Handler h) {
        mIncomingRingRegistrants.remove(h);
    }

    /**
     * Notifies when a voice connection has disconnected, either due to local
     * or remote hangup or error.
     *
     *  Messages received from this will have the following members:<p>
     *  <ul><li>Message.obj will be an AsyncResult</li>
     *  <li>AsyncResult.userObj = obj</li>
     *  <li>AsyncResult.result = a Connection object that is
     *  no longer connected.</li></ul>
     */
    @UnsupportedAppUsage
    public void registerForDisconnect(Handler h, int what, Object obj) {
        checkCorrectThread(h);

        mDisconnectRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for voice disconnection notification.
     * Extraneous calls are tolerated silently
     */
    @UnsupportedAppUsage
    public void unregisterForDisconnect(Handler h) {
        mDisconnectRegistrants.remove(h);
    }

    /**
     * Register for notifications when a supplementary service attempt fails.
     * Message.obj will contain an AsyncResult.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSuppServiceFailed(Handler h, int what, Object obj) {
        checkCorrectThread(h);

        mSuppServiceFailedRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications when a supplementary service attempt fails.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSuppServiceFailed(Handler h) {
        mSuppServiceFailedRegistrants.remove(h);
    }

    /**
     * Register for notifications of initiation of a new MMI code request.
     * MMI codes for GSM are discussed in 3GPP TS 22.030.<p>
     *
     * Example: If Phone.dial is called with "*#31#", then the app will
     * be notified here.<p>
     *
     * The returned <code>Message.obj</code> will contain an AsyncResult.
     *
     * <code>obj.result</code> will be an "MmiCode" object.
     */
    @UnsupportedAppUsage
    public void registerForMmiInitiate(Handler h, int what, Object obj) {
        checkCorrectThread(h);

        mMmiRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for new MMI initiate notification.
     * Extraneous calls are tolerated silently
     */
    @UnsupportedAppUsage
    public void unregisterForMmiInitiate(Handler h) {
        mMmiRegistrants.remove(h);
    }

    /**
     * Register for notifications that an MMI request has completed
     * its network activity and is in its final state. This may mean a state
     * of COMPLETE, FAILED, or CANCELLED.
     *
     * <code>Message.obj</code> will contain an AsyncResult.
     * <code>obj.result</code> will be an "MmiCode" object
     */
    @UnsupportedAppUsage
    public void registerForMmiComplete(Handler h, int what, Object obj) {
        checkCorrectThread(h);

        mMmiCompleteRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for MMI complete notification.
     * Extraneous calls are tolerated silently
     */
    @UnsupportedAppUsage
    public void unregisterForMmiComplete(Handler h) {
        checkCorrectThread(h);

        mMmiCompleteRegistrants.remove(h);
    }

    /**
     * Registration point for Sim records loaded
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    @UnsupportedAppUsage
    public void registerForSimRecordsLoaded(Handler h, int what, Object obj) {
    }

    /**
     * Unregister for notifications for Sim records loaded
     * @param h Handler to be removed from the registrant list.
     */
    @UnsupportedAppUsage
    public void unregisterForSimRecordsLoaded(Handler h) {
    }

    /**
     * Register for TTY mode change notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be an Integer containing new mode.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForTtyModeReceived(Handler h, int what, Object obj) {
    }

    /**
     * Unregisters for TTY mode change notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForTtyModeReceived(Handler h) {
    }

    /**
     * Switches network selection mode to "automatic", re-scanning and
     * re-selecting a network if appropriate.
     *
     * @param response The message to dispatch when the network selection
     * is complete.
     *
     * @see #selectNetworkManually(OperatorInfo, boolean, android.os.Message)
     */
    @UnsupportedAppUsage
    public void setNetworkSelectionModeAutomatic(Message response) {
        Rlog.d(LOG_TAG, "setNetworkSelectionModeAutomatic, querying current mode");
        // we don't want to do this unecesarily - it acutally causes
        // the radio to repeate network selection and is costly
        // first check if we're already in automatic mode
        Message msg = obtainMessage(EVENT_CHECK_FOR_NETWORK_AUTOMATIC);
        msg.obj = response;
        mCi.getNetworkSelectionMode(msg);
    }

    private void onCheckForNetworkSelectionModeAutomatic(Message fromRil) {
        AsyncResult ar = (AsyncResult)fromRil.obj;
        Message response = (Message)ar.userObj;
        boolean doAutomatic = true;
        if (ar.exception == null && ar.result != null) {
            try {
                int[] modes = (int[])ar.result;
                if (modes[0] == 0) {
                    // already confirmed to be in automatic mode - don't resend
                    doAutomatic = false;
                }
            } catch (Exception e) {
                // send the setting on error
            }
        }

        // wrap the response message in our own message along with
        // an empty string (to indicate automatic selection) for the
        // operator's id.
        NetworkSelectMessage nsm = new NetworkSelectMessage();
        nsm.message = response;
        nsm.operatorNumeric = "";
        nsm.operatorAlphaLong = "";
        nsm.operatorAlphaShort = "";

        if (doAutomatic) {
            Message msg = obtainMessage(EVENT_SET_NETWORK_AUTOMATIC_COMPLETE, nsm);
            mCi.setNetworkSelectionModeAutomatic(msg);
        } else {
            Rlog.d(LOG_TAG, "setNetworkSelectionModeAutomatic - already auto, ignoring");
            // let the calling application know that the we are ignoring automatic mode switch.
            if (nsm.message != null) {
                nsm.message.arg1 = ALREADY_IN_AUTO_SELECTION;
            }

            ar.userObj = nsm;
            handleSetSelectNetwork(ar);
        }

        updateSavedNetworkOperator(nsm);
    }

    /**
     * Query the radio for the current network selection mode.
     *
     * Return values:
     *     0 - automatic.
     *     1 - manual.
     */
    public void getNetworkSelectionMode(Message message) {
        mCi.getNetworkSelectionMode(message);
    }

    public List<ClientRequestStats> getClientRequestStats() {
        return mCi.getClientRequestStats();
    }

    /**
     * Manually selects a network. <code>response</code> is
     * dispatched when this is complete.  <code>response.obj</code> will be
     * an AsyncResult, and <code>response.obj.exception</code> will be non-null
     * on failure.
     *
     * @see #setNetworkSelectionModeAutomatic(Message)
     */
    @UnsupportedAppUsage
    public void selectNetworkManually(OperatorInfo network, boolean persistSelection,
            Message response) {
        // wrap the response message in our own message along with
        // the operator's id.
        NetworkSelectMessage nsm = new NetworkSelectMessage();
        nsm.message = response;
        nsm.operatorNumeric = network.getOperatorNumeric();
        nsm.operatorAlphaLong = network.getOperatorAlphaLong();
        nsm.operatorAlphaShort = network.getOperatorAlphaShort();

        Message msg = obtainMessage(EVENT_SET_NETWORK_MANUAL_COMPLETE, nsm);
        mCi.setNetworkSelectionModeManual(network.getOperatorNumeric(), network.getRan(), msg);

        if (persistSelection) {
            updateSavedNetworkOperator(nsm);
        } else {
            clearSavedNetworkSelection();
            updateManualNetworkSelection(nsm);
        }
    }

    /**
     * Registration point for emergency call/callback mode start. Message.obj is AsyncResult and
     * Message.obj.result will be Integer indicating start of call by value 1 or end of call by
     * value 0
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj.userObj
     */
    public void registerForEmergencyCallToggle(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mEmergencyCallToggledRegistrants.add(r);
    }

    public void unregisterForEmergencyCallToggle(Handler h) {
        mEmergencyCallToggledRegistrants.remove(h);
    }

    private void updateSavedNetworkOperator(NetworkSelectMessage nsm) {
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            // open the shared preferences editor, and write the value.
            // nsm.operatorNumeric is "" if we're in automatic.selection.
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(NETWORK_SELECTION_KEY + subId, nsm.operatorNumeric);
            editor.putString(NETWORK_SELECTION_NAME_KEY + subId, nsm.operatorAlphaLong);
            editor.putString(NETWORK_SELECTION_SHORT_KEY + subId, nsm.operatorAlphaShort);

            // commit and log the result.
            if (!editor.commit()) {
                Rlog.e(LOG_TAG, "failed to commit network selection preference");
            }
        } else {
            Rlog.e(LOG_TAG, "Cannot update network selection preference due to invalid subId " +
                    subId);
        }
    }

    /**
     * Update non-perisited manual network selection.
     *
     * @param nsm PLMN info of the selected network
     */
    protected void updateManualNetworkSelection(NetworkSelectMessage nsm)  {
        Rlog.e(LOG_TAG, "updateManualNetworkSelection() should be overridden");
    }

    /**
     * Used to track the settings upon completion of the network change.
     */
    private void handleSetSelectNetwork(AsyncResult ar) {
        // look for our wrapper within the asyncresult, skip the rest if it
        // is null.
        if (!(ar.userObj instanceof NetworkSelectMessage)) {
            Rlog.e(LOG_TAG, "unexpected result from user object.");
            return;
        }

        NetworkSelectMessage nsm = (NetworkSelectMessage) ar.userObj;

        // found the object, now we send off the message we had originally
        // attached to the request.
        if (nsm.message != null) {
            AsyncResult.forMessage(nsm.message, ar.result, ar.exception);
            nsm.message.sendToTarget();
        }
    }

    /**
     * Method to retrieve the saved operator from the Shared Preferences
     */
    @NonNull
    public OperatorInfo getSavedNetworkSelection() {
        // open the shared preferences and search with our key.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        String numeric = sp.getString(NETWORK_SELECTION_KEY + getSubId(), "");
        String name = sp.getString(NETWORK_SELECTION_NAME_KEY + getSubId(), "");
        String shrt = sp.getString(NETWORK_SELECTION_SHORT_KEY + getSubId(), "");
        return new OperatorInfo(name, shrt, numeric);
    }

    /**
     * Clears the saved network selection.
     */
    private void clearSavedNetworkSelection() {
        // open the shared preferences and search with our key.
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().
                remove(NETWORK_SELECTION_KEY + getSubId()).
                remove(NETWORK_SELECTION_NAME_KEY + getSubId()).
                remove(NETWORK_SELECTION_SHORT_KEY + getSubId()).commit();
    }

    /**
     * Method to restore the previously saved operator id, or reset to
     * automatic selection, all depending upon the value in the shared
     * preferences.
     */
    private void restoreSavedNetworkSelection(Message response) {
        // retrieve the operator
        OperatorInfo networkSelection = getSavedNetworkSelection();

        // set to auto if the id is empty, otherwise select the network.
        if (networkSelection == null || TextUtils.isEmpty(networkSelection.getOperatorNumeric())) {
            setNetworkSelectionModeAutomatic(response);
        } else {
            selectNetworkManually(networkSelection, true, response);
        }
    }

    /**
     * Saves CLIR setting so that we can re-apply it as necessary
     * (in case the RIL resets it across reboots).
     */
    public void saveClirSetting(int commandInterfaceCLIRMode) {
        // Open the shared preferences editor, and write the value.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(CLIR_KEY + getSubId(), commandInterfaceCLIRMode);
        Rlog.i(LOG_TAG, "saveClirSetting: " + CLIR_KEY + getSubId() + "="
                + commandInterfaceCLIRMode);

        // Commit and log the result.
        if (!editor.commit()) {
            Rlog.e(LOG_TAG, "Failed to commit CLIR preference");
        }
    }

    /**
     * For unit tests; don't send notifications to "Phone"
     * mailbox registrants if true.
     */
    private void setUnitTestMode(boolean f) {
        mUnitTestMode = f;
    }

    /**
     * @return true If unit test mode is enabled
     */
    public boolean getUnitTestMode() {
        return mUnitTestMode;
    }

    /**
     * To be invoked when a voice call Connection disconnects.
     *
     * Subclasses of Phone probably want to replace this with a
     * version scoped to their packages
     */
    protected void notifyDisconnectP(Connection cn) {
        AsyncResult ar = new AsyncResult(null, cn, null);
        mDisconnectRegistrants.notifyRegistrants(ar);
    }

    /**
     * Register for ServiceState changed.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a ServiceState instance
     */
    @UnsupportedAppUsage
    public void registerForServiceStateChanged(
            Handler h, int what, Object obj) {
        mServiceStateRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for ServiceStateChange notification.
     * Extraneous calls are tolerated silently
     */
    @UnsupportedAppUsage
    public void unregisterForServiceStateChanged(Handler h) {
        mServiceStateRegistrants.remove(h);
    }

    /**
     * Notifies when out-band ringback tone is needed.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = boolean, true to start play ringback tone
     *                       and false to stop. <p>
     */
    @UnsupportedAppUsage
    public void registerForRingbackTone(Handler h, int what, Object obj) {
        mCi.registerForRingbackTone(h, what, obj);
    }

    /**
     * Unregisters for ringback tone notification.
     */
    @UnsupportedAppUsage
    public void unregisterForRingbackTone(Handler h) {
        mCi.unregisterForRingbackTone(h);
    }

    /**
     * Notifies when out-band on-hold tone is needed.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = boolean, true to start play on-hold tone
     *                       and false to stop. <p>
     */
    public void registerForOnHoldTone(Handler h, int what, Object obj) {
    }

    /**
     * Unregisters for on-hold tone notification.
     */
    public void unregisterForOnHoldTone(Handler h) {
    }

    /**
     * Registers the handler to reset the uplink mute state to get
     * uplink audio.
     */
    public void registerForResendIncallMute(Handler h, int what, Object obj) {
        mCi.registerForResendIncallMute(h, what, obj);
    }

    /**
     * Unregisters for resend incall mute notifications.
     */
    public void unregisterForResendIncallMute(Handler h) {
        mCi.unregisterForResendIncallMute(h);
    }

    /**
     * Registers for CellInfo changed.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a List<CellInfo> instance
     */
    public void registerForCellInfo(
            Handler h, int what, Object obj) {
        mCellInfoRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for CellInfo notification.
     * Extraneous calls are tolerated silently
     */
    public void unregisterForCellInfo(Handler h) {
        mCellInfoRegistrants.remove(h);
    }

    /**
     * Enables or disables echo suppression.
     */
    public void setEchoSuppressionEnabled() {
        // no need for regular phone
    }

    /**
     * Subclasses of Phone probably want to replace this with a
     * version scoped to their packages
     */
    protected void notifyServiceStateChangedP(ServiceState ss) {
        AsyncResult ar = new AsyncResult(null, new ServiceState(ss), null);
        mServiceStateRegistrants.notifyRegistrants(ar);

        mNotifier.notifyServiceState(this);
    }

    /**
     * Version of notifyServiceStateChangedP which allows us to specify the subId. This is used when
     * we send out a final ServiceState update when a phone's subId becomes invalid.
     */
    protected void notifyServiceStateChangedPForSubId(ServiceState ss, int subId) {
        AsyncResult ar = new AsyncResult(null, ss, null);
        mServiceStateRegistrants.notifyRegistrants(ar);

        mNotifier.notifyServiceStateForSubId(this, ss, subId);
    }

    /**
     * If this is a simulated phone interface, returns a SimulatedRadioControl.
     * @return SimulatedRadioControl if this is a simulated interface;
     * otherwise, null.
     */
    public SimulatedRadioControl getSimulatedRadioControl() {
        return mSimulatedRadioControl;
    }

    /**
     * Verifies the current thread is the same as the thread originally
     * used in the initialization of this instance. Throws RuntimeException
     * if not.
     *
     * @exception RuntimeException if the current thread is not
     * the thread that originally obtained this Phone instance.
     */
    private void checkCorrectThread(Handler h) {
        if (h.getLooper() != mLooper) {
            throw new RuntimeException(
                    "com.android.internal.telephony.Phone must be used from within one thread");
        }
    }

    /**
     * Set the properties by matching the carrier string in
     * a string-array resource
     */
    @Nullable Locale getLocaleFromCarrierProperties() {
        String carrier = SystemProperties.get("ro.carrier");

        if (null == carrier || 0 == carrier.length() || "unknown".equals(carrier)) {
            return null;
        }

        CharSequence[] carrierLocales = mContext.getResources().getTextArray(
                R.array.carrier_properties);

        for (int i = 0; i < carrierLocales.length; i+=3) {
            String c = carrierLocales[i].toString();
            if (carrier.equals(c)) {
                return Locale.forLanguageTag(carrierLocales[i + 1].toString().replace('_', '-'));
            }
        }

        return null;
    }

    /**
     * Get current coarse-grained voice call state.
     * Use {@link #registerForPreciseCallStateChanged(Handler, int, Object)
     * registerForPreciseCallStateChanged()} for change notification. <p>
     * If the phone has an active call and call waiting occurs,
     * then the phone state is RINGING not OFFHOOK
     * <strong>Note:</strong>
     * This registration point provides notification of finer-grained
     * changes.<p>
     */
    @UnsupportedAppUsage
    public abstract PhoneConstants.State getState();

    /**
     * Retrieves the IccFileHandler of the Phone instance
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public IccFileHandler getIccFileHandler(){
        UiccCardApplication uiccApplication = mUiccApplication.get();
        IccFileHandler fh;

        if (uiccApplication == null) {
            Rlog.d(LOG_TAG, "getIccFileHandler: uiccApplication == null, return null");
            fh = null;
        } else {
            fh = uiccApplication.getIccFileHandler();
        }

        Rlog.d(LOG_TAG, "getIccFileHandler: fh=" + fh);
        return fh;
    }

    /*
     * Retrieves the Handler of the Phone instance
     */
    public Handler getHandler() {
        return this;
    }

    /**
     * Update the phone object if the voice radio technology has changed
     *
     * @param voiceRadioTech The new voice radio technology
     */
    public void updatePhoneObject(int voiceRadioTech) {
    }

    /**
    * Retrieves the ServiceStateTracker of the phone instance.
    */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public ServiceStateTracker getServiceStateTracker() {
        return null;
    }

    /**
     * Override to merge into {@link #getServiceState} when telecom has registered a SIM call
     * manager that supports over-the-top SIM-based calling (e.g. carrier-provided wi-fi calling
     * implementation).
     *
     * @param hasService Whether or not the SIM call manager currently provides over-the-top voice
     */
    public void setVoiceServiceStateOverride(boolean hasService) {}

    /**
     * Check whether the radio is off for thermal reason.
     *
     * @return {@code true} only if thermal mitigation is one of the reason for which radio is off.
     */
    public boolean isRadioOffForThermalMitigation() {
        ServiceStateTracker sst = getServiceStateTracker();
        return sst != null && sst.getRadioPowerOffReasons()
                .contains(Phone.RADIO_POWER_REASON_THERMAL);
    }

    /**
     * Retrieves the EmergencyNumberTracker of the phone instance.
     */
    public EmergencyNumberTracker getEmergencyNumberTracker() {
        return null;
    }

    /**
    * Get call tracker
    */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public CallTracker getCallTracker() {
        return null;
    }

    /**
     * @return The instance of transport manager.
     */
    public TransportManager getTransportManager() {
        return null;
    }

    /**
     * @return The instance of access networks manager.
     */
    public AccessNetworksManager getAccessNetworksManager() {
        return null;
    }

    /**
     * Retrieves the DeviceStateMonitor of the phone instance.
     */
    public DeviceStateMonitor getDeviceStateMonitor() {
        return null;
    }

    /**
     * Retrieves the DisplayInfoController of the phone instance.
     */
    public DisplayInfoController getDisplayInfoController() {
        return null;
    }

    /**
     * Retrieves the SignalStrengthController of the phone instance.
     */
    public SignalStrengthController getSignalStrengthController() {
        Log.wtf(LOG_TAG, "getSignalStrengthController return null.");
        return null;
    }

    /**
     * Update voice activation state
     */
    public void setVoiceActivationState(int state) {
        mSimActivationTracker.setVoiceActivationState(state);
    }
    /**
     * Update data activation state
     */
    public void setDataActivationState(int state) {
        mSimActivationTracker.setDataActivationState(state);
    }

    /**
     * Returns voice activation state
     */
    public int getVoiceActivationState() {
        return mSimActivationTracker.getVoiceActivationState();
    }
    /**
     * Returns data activation state
     */
    public int getDataActivationState() {
        return mSimActivationTracker.getDataActivationState();
    }

    /**
     * Update voice mail count related fields and notify listeners
     */
    public void updateVoiceMail() {
        Rlog.e(LOG_TAG, "updateVoiceMail() should be overridden");
    }

    public AppType getCurrentUiccAppType() {
        UiccCardApplication currentApp = mUiccApplication.get();
        if (currentApp != null) {
            return currentApp.getType();
        }
        return AppType.APPTYPE_UNKNOWN;
    }

    /**
     * Returns the ICC card interface for this phone, or null
     * if not applicable to underlying technology.
     */
    @UnsupportedAppUsage
    public IccCard getIccCard() {
        return null;
        //throw new Exception("getIccCard Shouldn't be called from Phone");
    }

    /**
     * Retrieves the serial number of the ICC, if applicable. Returns only the decimal digits before
     * the first hex digit in the ICC ID.
     */
    @UnsupportedAppUsage
    public String getIccSerialNumber() {
        IccRecords r = mIccRecords.get();
        return (r != null) ? r.getIccId() : null;
    }

    /**
     * Retrieves the full serial number of the ICC (including hex digits), if applicable.
     */
    public String getFullIccSerialNumber() {
        IccRecords r = mIccRecords.get();
        return (r != null) ? r.getFullIccId() : null;
    }

    /**
     * Returns SIM record load state. Use
     * <code>getSimCard().registerForReady()</code> for change notification.
     *
     * @return true if records from the SIM have been loaded and are
     * available (if applicable). If not applicable to the underlying
     * technology, returns true as well.
     */
    public boolean getIccRecordsLoaded() {
        IccRecords r = mIccRecords.get();
        return (r != null) ? r.getRecordsLoaded() : false;
    }

    /** Set the minimum interval for CellInfo requests to the modem */
    public void setCellInfoMinInterval(int interval) {
        getServiceStateTracker().setCellInfoMinInterval(interval);
    }

    /**
     * @return the last known CellInfo
     */
    public List<CellInfo> getAllCellInfo() {
        return getServiceStateTracker().getAllCellInfo();
    }

    /**
     * @param workSource calling WorkSource
     * @param rspMsg the response message containing the cell info
     */
    public void requestCellInfoUpdate(WorkSource workSource, Message rspMsg) {
        getServiceStateTracker().requestAllCellInfo(workSource, rspMsg);
    }

    /**
     * Returns the current CellIdentity if known
     */
    public CellIdentity getCurrentCellIdentity() {
        return getServiceStateTracker().getCellIdentity();
    }

    /**
     * @param workSource calling WorkSource
     * @param rspMsg the response message containing the cell location
     */
    public void getCellIdentity(WorkSource workSource, Message rspMsg) {
        getServiceStateTracker().requestCellIdentity(workSource, rspMsg);
    }

    /**
     * Sets the minimum time in milli-seconds between {@link PhoneStateListener#onCellInfoChanged
     * PhoneStateListener.onCellInfoChanged} will be invoked.
     *
     * The default, 0, means invoke onCellInfoChanged when any of the reported
     * information changes. Setting the value to INT_MAX(0x7fffffff) means never issue
     * A onCellInfoChanged.
     *
     * @param rateInMillis the rate
     * @param workSource calling WorkSource
     */
    public void setCellInfoListRate(int rateInMillis, WorkSource workSource) {
        mCi.setCellInfoListRate(rateInMillis, null, workSource);
    }

    /**
     * Get voice message waiting indicator status. No change notification
     * available on this interface. Use PhoneStateNotifier or similar instead.
     *
     * @return true if there is a voice message waiting
     */
    public boolean getMessageWaitingIndicator() {
        return mVmCount != 0;
    }

    /**
     *  Retrieves manually selected network info.
     */
    public String getManualNetworkSelectionPlmn() {
        return null;
    }


    private int getCallForwardingIndicatorFromSharedPref() {
        int status = IccRecords.CALL_FORWARDING_STATUS_DISABLED;
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            status = sp.getInt(CF_STATUS + subId, IccRecords.CALL_FORWARDING_STATUS_UNKNOWN);
            Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: for subId " + subId + "= " +
                    status);
            // Check for old preference if status is UNKNOWN for current subId. This part of the
            // code is needed only when upgrading from M to N.
            if (status == IccRecords.CALL_FORWARDING_STATUS_UNKNOWN) {
                String subscriberId = sp.getString(CF_ID, null);
                if (subscriberId != null) {
                    String currentSubscriberId = getSubscriberId();

                    if (subscriberId.equals(currentSubscriberId)) {
                        // get call forwarding status from preferences
                        status = sp.getInt(CF_STATUS, IccRecords.CALL_FORWARDING_STATUS_DISABLED);
                        setCallForwardingIndicatorInSharedPref(
                                status == IccRecords.CALL_FORWARDING_STATUS_ENABLED ? true : false);
                        Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: " + status);
                    } else {
                        Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: returning " +
                                "DISABLED as status for matching subscriberId not found");
                    }

                    // get rid of old preferences.
                    SharedPreferences.Editor editor = sp.edit();
                    editor.remove(CF_ID);
                    editor.remove(CF_STATUS);
                    editor.apply();
                }
            }
        } else {
            Rlog.e(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: invalid subId " + subId);
        }
        return status;
    }

    private void setCallForwardingIndicatorInSharedPref(boolean enable) {
        int status = enable ? IccRecords.CALL_FORWARDING_STATUS_ENABLED :
                IccRecords.CALL_FORWARDING_STATUS_DISABLED;
        int subId = getSubId();
        Rlog.i(LOG_TAG, "setCallForwardingIndicatorInSharedPref: Storing status = " + status +
                " in pref " + CF_STATUS + subId);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(CF_STATUS + subId, status);
        editor.apply();
    }

    public void setVoiceCallForwardingFlag(int line, boolean enable, String number) {
        setCallForwardingIndicatorInSharedPref(enable);
        IccRecords r = getIccRecords();
        if (r != null) {
            r.setVoiceCallForwardingFlag(line, enable, number);
        }
        notifyCallForwardingIndicator();
    }

    /**
     * Set the voice call forwarding flag for GSM/UMTS and the like SIMs
     *
     * @param r to enable/disable
     * @param line to enable/disable
     * @param enable
     * @param number to which CFU is enabled
     */
    public void setVoiceCallForwardingFlag(IccRecords r, int line, boolean enable,
                                              String number) {
        setCallForwardingIndicatorInSharedPref(enable);
        if (r != null) {
            r.setVoiceCallForwardingFlag(line, enable, number);
        }
        notifyCallForwardingIndicator();
    }

    /**
     * Get voice call forwarding indicator status. No change notification
     * available on this interface. Use PhoneStateNotifier or similar instead.
     *
     * @return true if there is a voice call forwarding
     */
    public boolean getCallForwardingIndicator() {
        if (getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            Rlog.e(LOG_TAG, "getCallForwardingIndicator: not possible in CDMA");
            return false;
        }
        IccRecords r = getIccRecords();
        int callForwardingIndicator = IccRecords.CALL_FORWARDING_STATUS_UNKNOWN;
        if (r != null) {
            callForwardingIndicator = r.getVoiceCallForwardingFlag();
        }
        if (callForwardingIndicator == IccRecords.CALL_FORWARDING_STATUS_UNKNOWN) {
            callForwardingIndicator = getCallForwardingIndicatorFromSharedPref();
        }
        Rlog.v(LOG_TAG, "getCallForwardingIndicator: iccForwardingFlag=" + (r != null
                    ? r.getVoiceCallForwardingFlag() : "null") + ", sharedPrefFlag="
                    + getCallForwardingIndicatorFromSharedPref());
        return (callForwardingIndicator == IccRecords.CALL_FORWARDING_STATUS_ENABLED);
    }

    public CarrierSignalAgent getCarrierSignalAgent() {
        return mCarrierSignalAgent;
    }

    public CarrierActionAgent getCarrierActionAgent() {
        return mCarrierActionAgent;
    }

    /**
     * Query the CDMA roaming preference setting.
     *
     * @param response is callback message to report one of TelephonyManager#CDMA_ROAMING_MODE_*
     */
    public void queryCdmaRoamingPreference(Message response) {
        mCi.queryCdmaRoamingPreference(response);
    }

    /**
     * Get the CDMA subscription mode setting.
     *
     * @param response is callback message to report one of TelephonyManager#CDMA_SUBSCRIPTION_*
     */
    public void queryCdmaSubscriptionMode(Message response) {
        mCi.getCdmaSubscriptionSource(response);
    }

    /**
     * Get current signal strength. No change notification available on this
     * interface. Use <code>PhoneStateNotifier</code> or an equivalent.
     * An ASU is 0-31 or -1 if unknown (for GSM, dBm = -113 - 2 * asu).
     * The following special values are defined:</p>
     * <ul><li>0 means "-113 dBm or less".</li>
     * <li>31 means "-51 dBm or greater".</li></ul>
     *
     * @return Current signal strength as SignalStrength
     */
    public SignalStrength getSignalStrength() {
        SignalStrengthController ssc = getSignalStrengthController();
        if (ssc == null) {
            return new SignalStrength();
        } else {
            return ssc.getSignalStrength();
        }
    }

    /**
     * @return true, if the device is in a state where both voice and data
     * are supported simultaneously. This can change based on location or network condition.
     */
    public boolean isConcurrentVoiceAndDataAllowed() {
        ServiceStateTracker sst = getServiceStateTracker();
        return sst == null ? false : sst.isConcurrentVoiceAndDataAllowed();
    }

    /**
     * Requests to set the CDMA roaming preference
     * @param cdmaRoamingType one of TelephonyManager#CDMA_ROAMING_MODE_*
     * @param response is callback message
     */
    public void setCdmaRoamingPreference(int cdmaRoamingType, Message response) {
        mCi.setCdmaRoamingPreference(cdmaRoamingType, response);
    }

    /**
     * Requests to set the CDMA subscription mode
     * @param cdmaSubscriptionType one of TelephonyManager#CDMA_SUBSCRIPTION_*
     * @param response is callback message
     */
    public void setCdmaSubscriptionMode(int cdmaSubscriptionType, Message response) {
        mCi.setCdmaSubscriptionSource(cdmaSubscriptionType, response);
    }

    /**
     * Get the effective allowed network types on the device.
     *
     * @return effective network type
     */
    private @TelephonyManager.NetworkTypeBitMask long getEffectiveAllowedNetworkTypes() {
        long allowedNetworkTypes = TelephonyManager.getAllNetworkTypesBitmask();
        synchronized (mAllowedNetworkTypesForReasons) {
            for (long networkTypes : mAllowedNetworkTypesForReasons.values()) {
                allowedNetworkTypes = allowedNetworkTypes & networkTypes;
            }
        }
        if (!mIsCarrierNrSupported) {
            allowedNetworkTypes &= ~TelephonyManager.NETWORK_TYPE_BITMASK_NR;
        }
        logd("SubId" + getSubId() + ",getEffectiveAllowedNetworkTypes: "
                + TelephonyManager.convertNetworkTypeBitmaskToString(allowedNetworkTypes));
        return allowedNetworkTypes;
    }

    /**
     * Notify the latest allowed network types changed.
     */
    public void notifyAllowedNetworkTypesChanged(
            @TelephonyManager.AllowedNetworkTypesReason int reason) {
        logd("SubId" + getSubId() + ",notifyAllowedNetworkTypesChanged: reason: " + reason
                + " value:" + TelephonyManager.convertNetworkTypeBitmaskToString(
                getAllowedNetworkTypes(reason)));
        mNotifier.notifyAllowedNetworkTypesChanged(this, reason, getAllowedNetworkTypes(reason));
    }

    /**
     * Is E-UTRA-NR Dual Connectivity enabled
     */
    public void isNrDualConnectivityEnabled(Message message, WorkSource workSource) {
        mCi.isNrDualConnectivityEnabled(message, workSource);
    }

    /**
     * Enable/Disable E-UTRA-NR Dual Connectivity
     * @param nrDualConnectivityState expected NR dual connectivity state
     * This can be passed following states
     * <ol>
     * <li>Enable NR dual connectivity {@link TelephonyManager#NR_DUAL_CONNECTIVITY_ENABLE}
     * <li>Disable NR dual connectivity {@link TelephonyManager#NR_DUAL_CONNECTIVITY_DISABLE}
     * <li>Disable NR dual connectivity and force secondary cell to be released
     * {@link TelephonyManager#NR_DUAL_CONNECTIVITY_DISABLE_IMMEDIATE}
     * </ol>
     */
    public void setNrDualConnectivityState(
            @TelephonyManager.NrDualConnectivityState int nrDualConnectivityState,
            Message message, WorkSource workSource) {
        mCi.setNrDualConnectivityState(nrDualConnectivityState, message, workSource);
    }

    /**
     * Get the allowed network types for a certain reason.
     * @param reason reason to configure allowed network types
     * @return the allowed network types.
     */
    public @TelephonyManager.NetworkTypeBitMask long getAllowedNetworkTypes(
            @TelephonyManager.AllowedNetworkTypesReason int reason) {
        long allowedNetworkTypes;
        long defaultAllowedNetworkTypes = RadioAccessFamily.getRafFromNetworkType(
                RILConstants.PREFERRED_NETWORK_MODE);

        if (!TelephonyManager.isValidAllowedNetworkTypesReason(reason)) {
            throw new IllegalArgumentException("AllowedNetworkTypes NumberFormat exception");
        }

        synchronized (mAllowedNetworkTypesForReasons) {
            allowedNetworkTypes = mAllowedNetworkTypesForReasons.getOrDefault(
                    reason,
                    defaultAllowedNetworkTypes);
        }
        if (!mIsCarrierNrSupported
                && reason == TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER) {
            allowedNetworkTypes = updateAllowedNetworkTypeForCarrierWithCarrierConfig();
        }
        logd("SubId" + getSubId() + ",get allowed network types "
                + convertAllowedNetworkTypeMapIndexToDbName(reason)
                + ": value = " + TelephonyManager.convertNetworkTypeBitmaskToString(
                allowedNetworkTypes));
        return allowedNetworkTypes;
    }

    /**
     * Loads the allowed network type from subscription database.
     */
    public void loadAllowedNetworksFromSubscriptionDatabase() {
        // Try to load ALLOWED_NETWORK_TYPES from SIMINFO.
        if (SubscriptionController.getInstance() == null) {
            return;
        }

        String result = SubscriptionController.getInstance().getSubscriptionProperty(
                getSubId(),
                SubscriptionManager.ALLOWED_NETWORK_TYPES);
        // After fw load network type from DB, do unlock if subId is valid.
        mIsAllowedNetworkTypesLoadedFromDb = SubscriptionManager.isValidSubscriptionId(getSubId());
        if (result == null) {
            return;
        }

        logd("SubId" + getSubId() + ",load allowed network types : value = " + result);
        Map<Integer, Long> oldAllowedNetworkTypes = new HashMap<>(mAllowedNetworkTypesForReasons);
        mAllowedNetworkTypesForReasons.clear();
        try {
            // Format: "REASON=VALUE,REASON2=VALUE2"
            for (String pair : result.trim().split(",")) {
                String[] networkTypesValues = (pair.trim().toLowerCase()).split("=");
                if (networkTypesValues.length != 2) {
                    Rlog.e(LOG_TAG, "Invalid ALLOWED_NETWORK_TYPES from DB, value = " + pair);
                    continue;
                }
                int key = convertAllowedNetworkTypeDbNameToMapIndex(networkTypesValues[0]);
                long value = Long.parseLong(networkTypesValues[1]);
                if (key != INVALID_ALLOWED_NETWORK_TYPES
                        && value != INVALID_ALLOWED_NETWORK_TYPES) {
                    synchronized (mAllowedNetworkTypesForReasons) {
                        mAllowedNetworkTypesForReasons.put(key, value);
                    }
                    if (!oldAllowedNetworkTypes.containsKey(key)
                            || oldAllowedNetworkTypes.get(key) != value) {
                        if (oldAllowedNetworkTypes.containsKey(key)) {
                            oldAllowedNetworkTypes.remove(key);
                        }
                        notifyAllowedNetworkTypesChanged(key);
                    }
                }
            }
        } catch (NumberFormatException e) {
            Rlog.e(LOG_TAG, "allowedNetworkTypes NumberFormat exception" + e);
        }

        for (int key : oldAllowedNetworkTypes.keySet()) {
            notifyAllowedNetworkTypesChanged(key);
        }
    }

    private int convertAllowedNetworkTypeDbNameToMapIndex(String name) {
        switch (name) {
            case ALLOWED_NETWORK_TYPES_TEXT_USER:
                return TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER;
            case ALLOWED_NETWORK_TYPES_TEXT_POWER:
                return TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_POWER;
            case ALLOWED_NETWORK_TYPES_TEXT_CARRIER:
                return TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER;
            case ALLOWED_NETWORK_TYPES_TEXT_ENABLE_2G:
                return TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G;
            default:
                return INVALID_ALLOWED_NETWORK_TYPES;
        }
    }

    private String convertAllowedNetworkTypeMapIndexToDbName(int reason) {
        switch (reason) {
            case TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER:
                return ALLOWED_NETWORK_TYPES_TEXT_USER;
            case TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_POWER:
                return ALLOWED_NETWORK_TYPES_TEXT_POWER;
            case TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER:
                return ALLOWED_NETWORK_TYPES_TEXT_CARRIER;
            case TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G:
                return ALLOWED_NETWORK_TYPES_TEXT_ENABLE_2G;
            default:
                return Integer.toString(INVALID_ALLOWED_NETWORK_TYPES);
        }
    }

    private @TelephonyManager.NetworkTypeBitMask long
            updateAllowedNetworkTypeForCarrierWithCarrierConfig() {
        long defaultAllowedNetworkTypes = RadioAccessFamily.getRafFromNetworkType(
                RILConstants.PREFERRED_NETWORK_MODE);
        long allowedNetworkTypes;
        synchronized (mAllowedNetworkTypesForReasons) {
            allowedNetworkTypes = mAllowedNetworkTypesForReasons.getOrDefault(
                    TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER,
                    defaultAllowedNetworkTypes);
        }
        if (mIsCarrierNrSupported) {
            return allowedNetworkTypes;
        }
        allowedNetworkTypes = allowedNetworkTypes & ~TelephonyManager.NETWORK_TYPE_BITMASK_NR;

        logd("Allowed network types for 'carrier' reason is changed by carrier config = "
                + TelephonyManager.convertNetworkTypeBitmaskToString(allowedNetworkTypes));
        return allowedNetworkTypes;
    }

    /**
     * Requests to set the allowed network types for a specific reason
     *
     * @param reason reason to configure allowed network type
     * @param networkTypes one of the network types
     * @param response callback Message
     */
    public void setAllowedNetworkTypes(@TelephonyManager.AllowedNetworkTypesReason int reason,
            @TelephonyManager.NetworkTypeBitMask long networkTypes, @Nullable Message response) {
        int subId = getSubId();
        if (!TelephonyManager.isValidAllowedNetworkTypesReason(reason)) {
            loge("setAllowedNetworkTypes: Invalid allowed network type reason: " + reason);
            if (response != null) {
                AsyncResult.forMessage(response, null,
                        new CommandException(CommandException.Error.INVALID_ARGUMENTS));
                response.sendToTarget();
            }
            return;
        }
        if (!SubscriptionManager.isUsableSubscriptionId(subId)
                || !mIsAllowedNetworkTypesLoadedFromDb) {
            loge("setAllowedNetworkTypes: no sim or network type is not loaded. SubscriptionId: "
                    + subId + ", isNetworkTypeLoaded" + mIsAllowedNetworkTypesLoadedFromDb);
            if (response != null) {
                AsyncResult.forMessage(response, null,
                        new CommandException(CommandException.Error.MISSING_RESOURCE));
                response.sendToTarget();
            }
            return;
        }
        String mapAsString = "";
        synchronized (mAllowedNetworkTypesForReasons) {
            mAllowedNetworkTypesForReasons.put(reason, networkTypes);
            mapAsString = mAllowedNetworkTypesForReasons.keySet().stream()
                    .map(key -> convertAllowedNetworkTypeMapIndexToDbName(key) + "="
                            + mAllowedNetworkTypesForReasons.get(key))
                    .collect(Collectors.joining(","));
        }
        SubscriptionManager.setSubscriptionProperty(subId,
                SubscriptionManager.ALLOWED_NETWORK_TYPES,
                mapAsString);
        logd("setAllowedNetworkTypes: SubId" + subId + ",setAllowedNetworkTypes " + mapAsString);

        updateAllowedNetworkTypes(response);
        notifyAllowedNetworkTypesChanged(reason);
    }

    protected void updateAllowedNetworkTypes(Message response) {
        int modemRaf = getRadioAccessFamily();
        if (modemRaf == RadioAccessFamily.RAF_UNKNOWN) {
            Rlog.d(LOG_TAG, "setPreferredNetworkType: Abort, unknown RAF: "
                    + modemRaf);
            if (response != null) {
                CommandException ex;
                ex = new CommandException(CommandException.Error.GENERIC_FAILURE);
                AsyncResult.forMessage(response, null, ex);
                response.sendToTarget();
            }
            return;
        }

        int filteredRaf = (int) (modemRaf & getEffectiveAllowedNetworkTypes());

        logd("setAllowedNetworkTypes: modemRafBitMask = " + modemRaf
                + " ,modemRaf = " + TelephonyManager.convertNetworkTypeBitmaskToString(modemRaf)
                + " ,filteredRafBitMask = " + filteredRaf
                + " ,filteredRaf = " + TelephonyManager.convertNetworkTypeBitmaskToString(
                filteredRaf));
        mCi.setAllowedNetworkTypesBitmap(filteredRaf, response);
        mPreferredNetworkTypeRegistrants.notifyRegistrants();
    }

    /**
     * Query the allowed network types bitmask setting
     *
     * @param response is callback message to report network types bitmask
     */
    public void getAllowedNetworkTypesBitmask(Message response) {
        mCi.getAllowedNetworkTypesBitmap(response);
    }

    /**
     * Register for preferred network type changes
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForPreferredNetworkTypeChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        mPreferredNetworkTypeRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregister for preferred network type changes.
     *
     * @param h Handler that should be unregistered.
     */
    public void unregisterForPreferredNetworkTypeChanged(Handler h) {
        mPreferredNetworkTypeRegistrants.remove(h);
    }

    /**
     * Get the cached value of the preferred network type setting
     */
    public int getCachedAllowedNetworkTypesBitmask() {
        if (mCi != null && mCi instanceof BaseCommands) {
            return ((BaseCommands) mCi).mAllowedNetworkTypesBitmask;
        } else {
            return RadioAccessFamily.getRafFromNetworkType(RILConstants.PREFERRED_NETWORK_MODE);
        }
    }

    /**
     * Gets the default SMSC address.
     *
     * @param result Callback message contains the SMSC address.
     */
    @UnsupportedAppUsage
    public void getSmscAddress(Message result) {
        mCi.getSmscAddress(result);
    }

    /**
     * Sets the default SMSC address.
     *
     * @param address new SMSC address
     * @param result Callback message is empty on completion
     */
    @UnsupportedAppUsage
    public void setSmscAddress(String address, Message result) {
        mCi.setSmscAddress(address, result);
    }

    /**
     * setTTYMode
     * sets a TTY mode option.
     * @param ttyMode is a one of the following:
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_OFF}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_FULL}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_HCO}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_VCO}
     * @param onComplete a callback message when the action is completed
     */
    public void setTTYMode(int ttyMode, Message onComplete) {
        mCi.setTTYMode(ttyMode, onComplete);
    }

    /**
     * setUiTTYMode
     * sets a TTY mode option.
     * @param ttyMode is a one of the following:
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_OFF}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_FULL}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_HCO}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_VCO}
     * @param onComplete a callback message when the action is completed
     */
    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
        Rlog.d(LOG_TAG, "unexpected setUiTTYMode method call");
    }

    /**
     * queryTTYMode
     * query the status of the TTY mode
     *
     * @param onComplete a callback message when the action is completed.
     */
    public void queryTTYMode(Message onComplete) {
        mCi.queryTTYMode(onComplete);
    }

    /**
     * Enable or disable enhanced Voice Privacy (VP). If enhanced VP is
     * disabled, normal VP is enabled.
     *
     * @param enable whether true or false to enable or disable.
     * @param onComplete a callback message when the action is completed.
     */
    public void enableEnhancedVoicePrivacy(boolean enable, Message onComplete) {
    }

    /**
     * Get the currently set Voice Privacy (VP) mode.
     *
     * @param onComplete a callback message when the action is completed.
     */
    public void getEnhancedVoicePrivacy(Message onComplete) {
    }

    /**
     * Assign a specified band for RF configuration.
     *
     * @param bandMode one of BM_*_BAND
     * @param response is callback message
     */
    public void setBandMode(int bandMode, Message response) {
        mCi.setBandMode(bandMode, response);
    }

    /**
     * Query the list of band mode supported by RF.
     *
     * @param response is callback message
     *        ((AsyncResult)response.obj).result  is an int[] where int[0] is
     *        the size of the array and the rest of each element representing
     *        one available BM_*_BAND
     */
    public void queryAvailableBandMode(Message response) {
        mCi.queryAvailableBandMode(response);
    }

    /**
     * Invokes RIL_REQUEST_OEM_HOOK_RAW on RIL implementation.
     *
     * @param data The data for the request.
     * @param response <strong>On success</strong>,
     * (byte[])(((AsyncResult)response.obj).result)
     * <strong>On failure</strong>,
     * (((AsyncResult)response.obj).result) == null and
     * (((AsyncResult)response.obj).exception) being an instance of
     * com.android.internal.telephony.gsm.CommandException
     *
     * @see #invokeOemRilRequestRaw(byte[], android.os.Message)
     * @deprecated OEM needs a vendor-extension hal and their apps should use that instead
     */
    @UnsupportedAppUsage
    @Deprecated
    public void invokeOemRilRequestRaw(byte[] data, Message response) {
        mCi.invokeOemRilRequestRaw(data, response);
    }

    /**
     * Invokes RIL_REQUEST_OEM_HOOK_Strings on RIL implementation.
     *
     * @param strings The strings to make available as the request data.
     * @param response <strong>On success</strong>, "response" bytes is
     * made available as:
     * (String[])(((AsyncResult)response.obj).result).
     * <strong>On failure</strong>,
     * (((AsyncResult)response.obj).result) == null and
     * (((AsyncResult)response.obj).exception) being an instance of
     * com.android.internal.telephony.gsm.CommandException
     *
     * @see #invokeOemRilRequestStrings(java.lang.String[], android.os.Message)
     * @deprecated OEM needs a vendor-extension hal and their apps should use that instead
     */
    @UnsupportedAppUsage
    @Deprecated
    public void invokeOemRilRequestStrings(String[] strings, Message response) {
        mCi.invokeOemRilRequestStrings(strings, response);
    }

    /**
     * Read one of the NV items defined in {@link RadioNVItems} / {@code ril_nv_items.h}.
     * Used for device configuration by some CDMA operators.
     *
     * @param itemID the ID of the item to read
     * @param response callback message with the String response in the obj field
     * @param workSource calling WorkSource
     */
    public void nvReadItem(int itemID, Message response, WorkSource workSource) {
        mCi.nvReadItem(itemID, response, workSource);
    }

    /**
     * Write one of the NV items defined in {@link RadioNVItems} / {@code ril_nv_items.h}.
     * Used for device configuration by some CDMA operators.
     *
     * @param itemID the ID of the item to read
     * @param itemValue the value to write, as a String
     * @param response Callback message.
     * @param workSource calling WorkSource
     */
    public void nvWriteItem(int itemID, String itemValue, Message response,
            WorkSource workSource) {
        mCi.nvWriteItem(itemID, itemValue, response, workSource);
    }

    /**
     * Update the CDMA Preferred Roaming List (PRL) in the radio NV storage.
     * Used for device configuration by some CDMA operators.
     *
     * @param preferredRoamingList byte array containing the new PRL
     * @param response Callback message.
     */
    public void nvWriteCdmaPrl(byte[] preferredRoamingList, Message response) {
        mCi.nvWriteCdmaPrl(preferredRoamingList, response);
    }

    /**
     * Perform the radio modem reboot. The radio will be taken offline. Used for device
     * configuration by some CDMA operators.
     * TODO: reuse nvResetConfig for now, should move to separate HAL API.
     *
     * @param response Callback message.
     */
    public void rebootModem(Message response) {
        mCi.nvResetConfig(1 /* 1: reload NV reset, trigger a modem reboot */, response);
    }

    /**
     * Perform the modem configuration reset. Used for device configuration by some CDMA operators.
     * TODO: reuse nvResetConfig for now, should move to separate HAL API.
     *
     * @param response Callback message.
     */
    public void resetModemConfig(Message response) {
        mCi.nvResetConfig(3 /* factory NV reset */, response);
    }

    /**
     * Perform modem configuration erase. Used for network reset
     *
     * @param response Callback message.
     */
    public void eraseModemConfig(Message response) {
        mCi.nvResetConfig(2 /* erase NV */, response);
    }

    /**
     * Erase data saved in the SharedPreference. Used for network reset
     *
     */
    public boolean eraseDataInSharedPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        Rlog.d(LOG_TAG, "Erase all data saved in SharedPreferences");
        editor.clear();
        return editor.commit();
    }

    public void setSystemSelectionChannels(List<RadioAccessSpecifier> specifiers,
            Message response) {
        mCi.setSystemSelectionChannels(specifiers, response);
    }

    /**
     * Get which bands the modem's background scan is acting on.
     *
     * @param response Callback message.
     */
    public void getSystemSelectionChannels(Message response) {
        mCi.getSystemSelectionChannels(response);
    }

    public void notifyDataActivity() {
        mNotifier.notifyDataActivity(this);
    }

    private void notifyMessageWaitingIndicator() {
        // Do not notify voice mail waiting if device doesn't support voice
        if (!mIsVoiceCapable)
            return;

        // This function is added to send the notification to DefaultPhoneNotifier.
        mNotifier.notifyMessageWaitingChanged(this);
    }

    /** Send notification with an updated PreciseDataConnectionState to a single data connection */
    public void notifyDataConnection(PreciseDataConnectionState state) {
        mNotifier.notifyDataConnection(this, state);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void notifyOtaspChanged(int otaspMode) {
        mOtaspRegistrants.notifyRegistrants(new AsyncResult(null, otaspMode, null));
    }

    public void notifyVoiceActivationStateChanged(int state) {
        mNotifier.notifyVoiceActivationStateChanged(this, state);
    }

    public void notifyDataActivationStateChanged(int state) {
        mNotifier.notifyDataActivationStateChanged(this, state);
    }

    public void notifyUserMobileDataStateChanged(boolean state) {
        mNotifier.notifyUserMobileDataStateChanged(this, state);
    }

    /** Send notification that display info has changed. */
    public void notifyDisplayInfoChanged(TelephonyDisplayInfo telephonyDisplayInfo) {
        mNotifier.notifyDisplayInfoChanged(this, telephonyDisplayInfo);
    }

    public void notifySignalStrength() {
        mNotifier.notifySignalStrength(this);
    }

    public PhoneConstants.DataState getDataConnectionState(String apnType) {
        return PhoneConstants.DataState.DISCONNECTED;
    }

    /** Default implementation to get the PreciseDataConnectionState */
    public @Nullable PreciseDataConnectionState getPreciseDataConnectionState(String apnType) {
        return null;
    }

    public void notifyCellInfo(List<CellInfo> cellInfo) {
        AsyncResult ar = new AsyncResult(null, cellInfo, null);
        mCellInfoRegistrants.notifyRegistrants(ar);

        mNotifier.notifyCellInfo(this, cellInfo);
    }

    /**
     * Registration point for PhysicalChannelConfig change.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj.userObj
     */
    public void registerForPhysicalChannelConfig(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        Registrant registrant = new Registrant(h, what, obj);
        mPhysicalChannelConfigRegistrants.add(registrant);
        // notify first
        List<PhysicalChannelConfig> physicalChannelConfigs = getPhysicalChannelConfigList();
        if (physicalChannelConfigs != null) {
            registrant.notifyRegistrant(new AsyncResult(null, physicalChannelConfigs, null));
        }
    }

    public void unregisterForPhysicalChannelConfig(Handler h) {
        mPhysicalChannelConfigRegistrants.remove(h);
    }

    /** Notify {@link PhysicalChannelConfig} changes. */
    public void notifyPhysicalChannelConfig(List<PhysicalChannelConfig> configs) {
        mPhysicalChannelConfigRegistrants.notifyRegistrants(new AsyncResult(null, configs, null));
        mNotifier.notifyPhysicalChannelConfig(this, configs);
    }

    public List<PhysicalChannelConfig> getPhysicalChannelConfigList() {
        return null;
    }

    /**
     * Notify listeners that SRVCC state has changed.
     */
    public void notifySrvccStateChanged(int state) {
        mNotifier.notifySrvccStateChanged(this, state);
    }

    /** Notify the {@link EmergencyNumber} changes. */
    public void notifyEmergencyNumberList() {
        mNotifier.notifyEmergencyNumberList(this);
    }

    /** Notify the outgoing Sms {@link EmergencyNumber} changes. */
    public void notifyOutgoingEmergencySms(EmergencyNumber emergencyNumber) {
        mNotifier.notifyOutgoingEmergencySms(this, emergencyNumber);
    }

    /** Notify the data enabled changes. */
    public void notifyDataEnabled(boolean enabled,
            @TelephonyManager.DataEnabledReason int reason) {
        mNotifier.notifyDataEnabled(this, enabled, reason);
    }

    /** Notify link capacity estimate has changed. */
    public void notifyLinkCapacityEstimateChanged(
            List<LinkCapacityEstimate> linkCapacityEstimateList) {
        mNotifier.notifyLinkCapacityEstimateChanged(this, linkCapacityEstimateList);
    }

    /**
     * @return true if a mobile originating emergency call is active
     */
    public boolean isInEmergencyCall() {
        return false;
    }

    // This property is used to handle phone process crashes, and is the same for CDMA and IMS
    // phones
    protected static boolean getInEcmMode() {
        return TelephonyProperties.in_ecm_mode().orElse(false);
    }

    /**
     * @return {@code true} if we are in emergency call back mode. This is a period where the phone
     * should be using as little power as possible and be ready to receive an incoming call from the
     * emergency operator.
     */
    public boolean isInEcm() {
        return mIsPhoneInEcmState;
    }

    public boolean isInImsEcm() {
        return false;
    }

    public boolean isInCdmaEcm() {
        return getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA && isInEcm()
                && (mImsPhone == null || !mImsPhone.isInImsEcm());
    }

    public void setIsInEcm(boolean isInEcm) {
        if (!getUnitTestMode()) {
            TelephonyProperties.in_ecm_mode(isInEcm);
        }
        mIsPhoneInEcmState = isInEcm;
    }

    /**
     * @return true if this Phone is in an emergency call that caused emergency callback mode to be
     * canceled, false if not.
     */
    public boolean isEcmCanceledForEmergency() {
        return mEcmCanceledForEmergency;
    }

    /**
     * Set whether or not this Phone has an active emergency call that was placed during emergency
     * callback mode and caused it to be temporarily canceled.
     * @param isCanceled true if an emergency call was placed that caused ECM to be canceled, false
     *                   if it is not in this state.
     */
    public void setEcmCanceledForEmergency(boolean isCanceled) {
        mEcmCanceledForEmergency = isCanceled;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private static int getVideoState(Call call) {
        int videoState = VideoProfile.STATE_AUDIO_ONLY;
        Connection conn = call.getEarliestConnection();
        if (conn != null) {
            videoState = conn.getVideoState();
        }
        return videoState;
    }

    /**
     * Determines if the specified call currently is or was at some point a video call, or if it is
     * a conference call.
     * @param call The call.
     * @return {@code true} if the call is or was a video call or is a conference call,
     *      {@code false} otherwise.
     */
    private boolean isVideoCallOrConference(Call call) {
        if (call.isMultiparty()) {
            return true;
        }

        boolean isDowngradedVideoCall = false;
        if (call instanceof ImsPhoneCall) {
            ImsPhoneCall imsPhoneCall = (ImsPhoneCall) call;
            ImsCall imsCall = imsPhoneCall.getImsCall();
            return imsCall != null && (imsCall.isVideoCall() ||
                    imsCall.wasVideoCall());
        }
        return isDowngradedVideoCall;
    }

    /**
     * @return {@code true} if an IMS video call or IMS conference is present, false otherwise.
     */
    public boolean isImsVideoCallOrConferencePresent() {
        boolean isPresent = false;
        if (mImsPhone != null) {
            isPresent = isVideoCallOrConference(mImsPhone.getForegroundCall()) ||
                    isVideoCallOrConference(mImsPhone.getBackgroundCall()) ||
                    isVideoCallOrConference(mImsPhone.getRingingCall());
        }
        Rlog.d(LOG_TAG, "isImsVideoCallOrConferencePresent: " + isPresent);
        return isPresent;
    }

    /**
     * Return a numerical identifier for the phone radio interface.
     * @return PHONE_TYPE_XXX as defined above.
     */
    @UnsupportedAppUsage
    public abstract int getPhoneType();

    /**
     * Returns unread voicemail count. This count is shown when the  voicemail
     * notification is expanded.<p>
     */
    public int getVoiceMessageCount(){
        return mVmCount;
    }

    /** sets the voice mail count of the phone and notifies listeners. */
    public void setVoiceMessageCount(int countWaiting) {
        mVmCount = countWaiting;
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {

            Rlog.d(LOG_TAG, "setVoiceMessageCount: Storing Voice Mail Count = " + countWaiting +
                    " for mVmCountKey = " + VM_COUNT + subId + " in preferences.");

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(VM_COUNT + subId, countWaiting);
            editor.apply();
        } else {
            Rlog.e(LOG_TAG, "setVoiceMessageCount in sharedPreference: invalid subId " + subId);
        }
        // store voice mail count in SIM
        IccRecords records = UiccController.getInstance().getIccRecords(
                mPhoneId, UiccController.APP_FAM_3GPP);
        if (records != null) {
            Rlog.d(LOG_TAG, "setVoiceMessageCount: updating SIM Records");
            records.setVoiceMessageWaiting(1, countWaiting);
        } else {
            Rlog.d(LOG_TAG, "setVoiceMessageCount: SIM Records not found");
        }
        // notify listeners of voice mail
        notifyMessageWaitingIndicator();
    }

    /** gets the voice mail count from preferences */
    protected int getStoredVoiceMessageCount() {
        int countVoiceMessages = 0;
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            int invalidCount = -2;  //-1 is not really invalid. It is used for unknown number of vm
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            int countFromSP = sp.getInt(VM_COUNT + subId, invalidCount);
            if (countFromSP != invalidCount) {
                countVoiceMessages = countFromSP;
                Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: from preference for subId " + subId +
                        "= " + countVoiceMessages);
            } else {
                // Check for old preference if count not found for current subId. This part of the
                // code is needed only when upgrading from M to N.
                String subscriberId = sp.getString(VM_ID, null);
                if (subscriberId != null) {
                    String currentSubscriberId = getSubscriberId();

                    if (currentSubscriberId != null && currentSubscriberId.equals(subscriberId)) {
                        // get voice mail count from preferences
                        countVoiceMessages = sp.getInt(VM_COUNT, 0);
                        setVoiceMessageCount(countVoiceMessages);
                        Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: from preference = " +
                                countVoiceMessages);
                    } else {
                        Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: returning 0 as count for " +
                                "matching subscriberId not found");

                    }
                    // get rid of old preferences.
                    SharedPreferences.Editor editor = sp.edit();
                    editor.remove(VM_ID);
                    editor.remove(VM_COUNT);
                    editor.apply();
                }
            }
        } else {
            Rlog.e(LOG_TAG, "getStoredVoiceMessageCount: invalid subId " + subId);
        }
        return countVoiceMessages;
    }

    /**
     * send secret dialer codes to launch arbitrary activities.
     * an Intent is started with the android_secret_code://<code> URI.
     *
     * @param code stripped version of secret code without *#*# prefix and #*#* suffix
     */
    public void sendDialerSpecialCode(String code) {
        if (!TextUtils.isEmpty(code)) {
            final BroadcastOptions options = BroadcastOptions.makeBasic();
            options.setBackgroundActivityStartsAllowed(true);
            Intent intent = new Intent(TelephonyIntents.SECRET_CODE_ACTION,
                    Uri.parse("android_secret_code://" + code));
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            mContext.sendBroadcast(intent, null, options.toBundle());

            // {@link TelephonyManager.ACTION_SECRET_CODE} will replace {@link
            // TelephonyIntents#SECRET_CODE_ACTION} in the next Android version. Before
            // that both of these two actions will be broadcast.
            Intent secrectCodeIntent = new Intent(TelephonyManager.ACTION_SECRET_CODE,
                    Uri.parse("android_secret_code://" + code));
            secrectCodeIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            mContext.sendBroadcast(secrectCodeIntent, null, options.toBundle());
        }
    }

    /**
     * Returns the CDMA ERI icon index to display
     */
    public int getCdmaEriIconIndex() {
        return -1;
    }

    /**
     * Returns the CDMA ERI icon mode,
     * 0 - ON
     * 1 - FLASHING
     */
    public int getCdmaEriIconMode() {
        return -1;
    }

    /**
     * Returns the CDMA ERI text,
     */
    public String getCdmaEriText() {
        return "GSM nw, no ERI";
    }

    /**
     * Retrieves the MIN for CDMA phones.
     */
    public String getCdmaMin() {
        return null;
    }

    /**
     * Check if subscription data has been assigned to mMin
     *
     * return true if MIN info is ready; false otherwise.
     */
    public boolean isMinInfoReady() {
        return false;
    }

    /**
     *  Retrieves PRL Version for CDMA phones
     */
    public String getCdmaPrlVersion(){
        return null;
    }

    /**
     * @return {@code true} if data is suspended.
     */
    public boolean isDataSuspended() {
        return false;
    }

    /**
     * send burst DTMF tone, it can send the string as single character or multiple character
     * ignore if there is no active call or not valid digits string.
     * Valid digit means only includes characters ISO-LATIN characters 0-9, *, #
     * The difference between sendDtmf and sendBurstDtmf is sendDtmf only sends one character,
     * this api can send single character and multiple character, also, this api has response
     * back to caller.
     *
     * @param dtmfString is string representing the dialing digit(s) in the active call
     * @param on the DTMF ON length in milliseconds, or 0 for default
     * @param off the DTMF OFF length in milliseconds, or 0 for default
     * @param onComplete is the callback message when the action is processed by BP
     *
     */
    public void sendBurstDtmf(String dtmfString, int on, int off, Message onComplete) {
    }

    /**
     * Sets an event to be fired when the telephony system processes
     * a post-dial character on an outgoing call.<p>
     *
     * Messages of type <code>what</code> will be sent to <code>h</code>.
     * The <code>obj</code> field of these Message's will be instances of
     * <code>AsyncResult</code>. <code>Message.obj.result</code> will be
     * a Connection object.<p>
     *
     * Message.arg1 will be the post dial character being processed,
     * or 0 ('\0') if end of string.<p>
     *
     * If Connection.getPostDialState() == WAIT,
     * the application must call
     * {@link com.android.internal.telephony.Connection#proceedAfterWaitChar()
     * Connection.proceedAfterWaitChar()} or
     * {@link com.android.internal.telephony.Connection#cancelPostDial()
     * Connection.cancelPostDial()}
     * for the telephony system to continue playing the post-dial
     * DTMF sequence.<p>
     *
     * If Connection.getPostDialState() == WILD,
     * the application must call
     * {@link com.android.internal.telephony.Connection#proceedAfterWildChar
     * Connection.proceedAfterWildChar()}
     * or
     * {@link com.android.internal.telephony.Connection#cancelPostDial()
     * Connection.cancelPostDial()}
     * for the telephony system to continue playing the
     * post-dial DTMF sequence.<p>
     *
     * Only one post dial character handler may be set. <p>
     * Calling this method with "h" equal to null unsets this handler.<p>
     */
    @UnsupportedAppUsage
    public void setOnPostDialCharacter(Handler h, int what, Object obj) {
        mPostDialHandler = new Registrant(h, what, obj);
    }

    public Registrant getPostDialHandler() {
        return mPostDialHandler;
    }

    /**
     * request to exit emergency call back mode
     * the caller should use setOnECMModeExitResponse
     * to receive the emergency callback mode exit response
     */
    @UnsupportedAppUsage
    public void exitEmergencyCallbackMode() {
    }

    /**
     * Register for notifications when CDMA OTA Provision status change
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForCdmaOtaStatusChange(Handler h, int what, Object obj) {
    }

    /**
     * Unregister for notifications when CDMA OTA Provision status change
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForCdmaOtaStatusChange(Handler h) {
    }

    /**
     * Registration point for subscription info ready
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
    }

    /**
     * Unregister for notifications for subscription info
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSubscriptionInfoReady(Handler h) {
    }

    /**
     * Returns true if OTA Service Provisioning needs to be performed.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean needsOtaServiceProvisioning() {
        return false;
    }

    /**
     * this decides if the dial number is OTA(Over the air provision) number or not
     * @param dialStr is string representing the dialing digit(s)
     * @return  true means the dialStr is OTA number, and false means the dialStr is not OTA number
     */
    public  boolean isOtaSpNumber(String dialStr) {
        return false;
    }

    /**
     * Register for notifications when OTA Service Provisioning mode has changed.
     *
     * <p>The mode is integer. {@link TelephonyManager#OTASP_UNKNOWN}
     * means the value is currently unknown and the system should wait until
     * {@link TelephonyManager#OTASP_NEEDED} or {@link TelephonyManager#OTASP_NOT_NEEDED} is
     * received before making the decision to perform OTASP or not.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForOtaspChange(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        mOtaspRegistrants.addUnique(h, what, obj);
        // notify first
        new Registrant(h, what, obj).notifyRegistrant(new AsyncResult(null, getOtasp(), null));
    }

    /**
     * Unegister for notifications when OTA Service Provisioning mode has changed.
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForOtaspChange(Handler h) {
        mOtaspRegistrants.remove(h);
    }

    /**
     * Returns the current OTA Service Provisioning mode.
     *
     * @see registerForOtaspChange
     */
    public int getOtasp() {
        return TelephonyManager.OTASP_UNKNOWN;
    }

    /**
     * Register for notifications when CDMA call waiting comes
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForCallWaiting(Handler h, int what, Object obj){
    }

    /**
     * Unegister for notifications when CDMA Call waiting comes
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForCallWaiting(Handler h){
    }

    /**
     * Registration point for Ecm timer reset
     * @param h handler to notify
     * @param what user-defined message code
     * @param obj placed in Message.obj
     */
    @UnsupportedAppUsage
    public void registerForEcmTimerReset(Handler h, int what, Object obj) {
    }

    /**
     * Unregister for notification for Ecm timer reset
     * @param h Handler to be removed from the registrant list.
     */
    @UnsupportedAppUsage
    public void unregisterForEcmTimerReset(Handler h) {
    }

    /**
     * Register for signal information notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSignalInfo(Handler h, int what, Object obj) {
        mCi.registerForSignalInfo(h, what, obj);
    }

    /**
     * Unregisters for signal information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSignalInfo(Handler h) {
        mCi.unregisterForSignalInfo(h);
    }

    /**
     * Register for display information notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForDisplayInfo(Handler h, int what, Object obj) {
        mCi.registerForDisplayInfo(h, what, obj);
    }

    /**
     * Unregisters for display information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForDisplayInfo(Handler h) {
         mCi.unregisterForDisplayInfo(h);
    }

    /**
     * Register for CDMA number information record notification from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a CdmaInformationRecords.CdmaNumberInfoRec
     * instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForNumberInfo(Handler h, int what, Object obj) {
        mCi.registerForNumberInfo(h, what, obj);
    }

    /**
     * Unregisters for number information record notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForNumberInfo(Handler h) {
        mCi.unregisterForNumberInfo(h);
    }

    /**
     * Register for CDMA redirected number information record notification
     * from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a CdmaInformationRecords.CdmaRedirectingNumberInfoRec
     * instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForRedirectedNumberInfo(Handler h, int what, Object obj) {
        mCi.registerForRedirectedNumberInfo(h, what, obj);
    }

    /**
     * Unregisters for redirected number information record notification.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForRedirectedNumberInfo(Handler h) {
        mCi.unregisterForRedirectedNumberInfo(h);
    }

    /**
     * Register for CDMA line control information record notification
     * from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a CdmaInformationRecords.CdmaLineControlInfoRec
     * instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForLineControlInfo(Handler h, int what, Object obj) {
        mCi.registerForLineControlInfo(h, what, obj);
    }

    /**
     * Unregisters for line control information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForLineControlInfo(Handler h) {
        mCi.unregisterForLineControlInfo(h);
    }

    /**
     * Register for CDMA T53 CLIR information record notifications
     * from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a CdmaInformationRecords.CdmaT53ClirInfoRec
     * instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerFoT53ClirlInfo(Handler h, int what, Object obj) {
        mCi.registerFoT53ClirlInfo(h, what, obj);
    }

    /**
     * Unregisters for T53 CLIR information record notification
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForT53ClirInfo(Handler h) {
        mCi.unregisterForT53ClirInfo(h);
    }

    /**
     * Register for CDMA T53 audio control information record notifications
     * from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a CdmaInformationRecords.CdmaT53AudioControlInfoRec
     * instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForT53AudioControlInfo(Handler h, int what, Object obj) {
        mCi.registerForT53AudioControlInfo(h, what, obj);
    }

    /**
     * Unregisters for T53 audio control information record notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForT53AudioControlInfo(Handler h) {
        mCi.unregisterForT53AudioControlInfo(h);
    }

    /**
     * registers for exit emergency call back mode request response
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    @UnsupportedAppUsage
    public void setOnEcbModeExitResponse(Handler h, int what, Object obj){
    }

    /**
     * Unregisters for exit emergency call back mode request response
     *
     * @param h Handler to be removed from the registrant list.
     */
    @UnsupportedAppUsage
    public void unsetOnEcbModeExitResponse(Handler h){
    }

    /**
     * Register for radio off or not available
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForRadioOffOrNotAvailable(Handler h, int what, Object obj) {
        mRadioOffOrNotAvailableRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for radio off or not available
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForRadioOffOrNotAvailable(Handler h) {
        mRadioOffOrNotAvailableRegistrants.remove(h);
    }

    /**
     *  Location to an updatable file listing carrier provisioning urls.
     *  An example:
     *
     * <?xml version="1.0" encoding="utf-8"?>
     *  <provisioningUrls>
     *   <provisioningUrl mcc="310" mnc="4">http://myserver.com/foo?mdn=%3$s&amp;iccid=%1$s&amp;imei=%2$s</provisioningUrl>
     *  </provisioningUrls>
     */
    private static final String PROVISIONING_URL_PATH =
            "/data/misc/radio/provisioning_urls.xml";
    private final File mProvisioningUrlFile = new File(PROVISIONING_URL_PATH);

    /** XML tag for root element. */
    private static final String TAG_PROVISIONING_URLS = "provisioningUrls";
    /** XML tag for individual url */
    private static final String TAG_PROVISIONING_URL = "provisioningUrl";
    /** XML attribute for mcc */
    private static final String ATTR_MCC = "mcc";
    /** XML attribute for mnc */
    private static final String ATTR_MNC = "mnc";

    private String getProvisioningUrlBaseFromFile() {
        XmlPullParser parser;
        final Configuration config = mContext.getResources().getConfiguration();

        try (FileReader fileReader = new FileReader(mProvisioningUrlFile)) {
            parser = Xml.newPullParser();
            parser.setInput(fileReader);
            XmlUtils.beginDocument(parser, TAG_PROVISIONING_URLS);

            while (true) {
                XmlUtils.nextElement(parser);

                final String element = parser.getName();
                if (element == null) break;

                if (element.equals(TAG_PROVISIONING_URL)) {
                    String mcc = parser.getAttributeValue(null, ATTR_MCC);
                    try {
                        if (mcc != null && Integer.parseInt(mcc) == config.mcc) {
                            String mnc = parser.getAttributeValue(null, ATTR_MNC);
                            if (mnc != null && Integer.parseInt(mnc) == config.mnc) {
                                parser.next();
                                if (parser.getEventType() == XmlPullParser.TEXT) {
                                    return parser.getText();
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        Rlog.e(LOG_TAG, "Exception in getProvisioningUrlBaseFromFile: " + e);
                    }
                }
            }
            return null;
        } catch (FileNotFoundException e) {
            Rlog.e(LOG_TAG, "Carrier Provisioning Urls file not found");
        } catch (XmlPullParserException e) {
            Rlog.e(LOG_TAG, "Xml parser exception reading Carrier Provisioning Urls file: " + e);
        } catch (IOException e) {
            Rlog.e(LOG_TAG, "I/O exception reading Carrier Provisioning Urls file: " + e);
        }
        return null;
    }

    /**
     * Get the mobile provisioning url.
     */
    public String getMobileProvisioningUrl() {
        String url = getProvisioningUrlBaseFromFile();
        if (TextUtils.isEmpty(url)) {
            url = mContext.getResources().getString(R.string.mobile_provisioning_url);
            Rlog.d(LOG_TAG, "getMobileProvisioningUrl: url from resource =" + url);
        } else {
            Rlog.d(LOG_TAG, "getMobileProvisioningUrl: url from File =" + url);
        }
        // Populate the iccid, imei and phone number in the provisioning url.
        if (!TextUtils.isEmpty(url)) {
            String phoneNumber = getLine1Number();
            if (TextUtils.isEmpty(phoneNumber)) {
                phoneNumber = "0000000000";
            }
            url = String.format(url,
                    getIccSerialNumber() /* ICCID */,
                    getDeviceId() /* IMEI */,
                    phoneNumber /* Phone number */);
        }

        return url;
    }

    /**
     * Check if there are matching tethering (i.e DUN) for the carrier.
     * @return true if there is a matching DUN APN.
     */
    public boolean hasMatchedTetherApnSetting() {
        if (isUsingNewDataStack()) {
            NetworkRegistrationInfo nrs = getServiceState().getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (nrs != null) {
                return getDataNetworkController().getDataProfileManager()
                        .isTetheringDataProfileExisting(nrs.getAccessNetworkTechnology());
            }
            return false;
        }
        if (getDcTracker(AccessNetworkConstants.TRANSPORT_TYPE_WWAN) != null) {
            return getDcTracker(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                    .hasMatchedTetherApnSetting();
        }
        return false;
    }

    /**
     * Report on whether data connectivity is allowed for internet.
     *
     * @return {@code true} if internet data is allowed to be established.
     */
    public boolean isDataAllowed() {
        if (isUsingNewDataStack()) {
            return getDataNetworkController().isInternetDataAllowed();
        }
        return isDataAllowed(ApnSetting.TYPE_DEFAULT, null);
    }

    /**
     * Report on whether data connectivity is allowed.
     *
     * @param apnType APN type
     * @param reasons The reasons that data can/can't be established. This is an output param.
     * @return True if data is allowed to be established
     */
    public boolean isDataAllowed(@ApnType int apnType, DataConnectionReasons reasons) {
        if (mAccessNetworksManager != null) {
            int transport = mAccessNetworksManager.getCurrentTransport(apnType);
            if (getDcTracker(transport) != null) {
                return getDcTracker(transport).isDataAllowed(reasons);
            }
        }
        return false;
    }


    /**
     * Action set from carrier signalling broadcast receivers to enable/disable metered apns.
     */
    public void carrierActionSetMeteredApnsEnabled(boolean enabled) {
        mCarrierActionAgent.carrierActionSetMeteredApnsEnabled(enabled);
    }

    /**
     * Action set from carrier signalling broadcast receivers to enable/disable radio
     */
    public void carrierActionSetRadioEnabled(boolean enabled) {
        mCarrierActionAgent.carrierActionSetRadioEnabled(enabled);
    }

    /**
     * Action set from carrier app to start/stop reporting default network condition.
     */
    public void carrierActionReportDefaultNetworkStatus(boolean report) {
        mCarrierActionAgent.carrierActionReportDefaultNetworkStatus(report);
    }

    /**
     * Action set from carrier signalling broadcast receivers to reset all carrier actions
     */
    public void carrierActionResetAll() {
        mCarrierActionAgent.carrierActionReset();
    }

    /**
     * Notify registrants of a new ringing Connection.
     * Subclasses of Phone probably want to replace this with a
     * version scoped to their packages
     */
    public void notifyNewRingingConnectionP(Connection cn) {
        Rlog.i(LOG_TAG, String.format(
                "notifyNewRingingConnection: phoneId=[%d], connection=[%s], registrants=[%s]",
                getPhoneId(), cn, getNewRingingConnectionRegistrantsAsString()));
        if (!mIsVoiceCapable)
            return;
        AsyncResult ar = new AsyncResult(null, cn, null);
        mNewRingingConnectionRegistrants.notifyRegistrants(ar);
    }

    /**
     * helper for notifyNewRingingConnectionP(Connection) to create a string for a log message.
     *
     * @return a list of objects in mNewRingingConnectionRegistrants as a String
     */
    private String getNewRingingConnectionRegistrantsAsString() {
        List<String> registrants = new ArrayList<>();
        for (int i = 0; i < mNewRingingConnectionRegistrants.size(); i++) {
            registrants.add(mNewRingingConnectionRegistrants.get(i).toString());
        }
        return String.join(", ", registrants);
    }

    /**
     * Notify registrants of a new unknown connection.
     */
    public void notifyUnknownConnectionP(Connection cn) {
        mUnknownConnectionRegistrants.notifyResult(cn);
    }

    /**
     * Notify registrants if phone is video capable.
     */
    public void notifyForVideoCapabilityChanged(boolean isVideoCallCapable) {
        // Cache the current video capability so that we don't lose the information.
        mIsVideoCapable = isVideoCallCapable;

        AsyncResult ar = new AsyncResult(null, isVideoCallCapable, null);
        mVideoCapabilityChangedRegistrants.notifyRegistrants(ar);
    }

    /**
     * Notify registrants of a RING event.
     */
    private void notifyIncomingRing() {
        if (!mIsVoiceCapable)
            return;
        AsyncResult ar = new AsyncResult(null, this, null);
        mIncomingRingRegistrants.notifyRegistrants(ar);
    }

    /**
     * Send the incoming call Ring notification if conditions are right.
     */
    private void sendIncomingCallRingNotification(int token) {
        if (mIsVoiceCapable && !mDoesRilSendMultipleCallRing &&
                (token == mCallRingContinueToken)) {
            Rlog.d(LOG_TAG, "Sending notifyIncomingRing");
            notifyIncomingRing();
            sendMessageDelayed(
                    obtainMessage(EVENT_CALL_RING_CONTINUE, token, 0), mCallRingDelay);
        } else {
            Rlog.d(LOG_TAG, "Ignoring ring notification request,"
                    + " mDoesRilSendMultipleCallRing=" + mDoesRilSendMultipleCallRing
                    + " token=" + token
                    + " mCallRingContinueToken=" + mCallRingContinueToken
                    + " mIsVoiceCapable=" + mIsVoiceCapable);
        }
    }

    /**
     * Enable or disable always reporting signal strength changes from radio.
     *
     * @param isEnable {@code true} for enabling; {@code false} for disabling.
     */
    public void setAlwaysReportSignalStrength(boolean isEnable) {
        if (mDeviceStateMonitor != null) {
            mDeviceStateMonitor.setAlwaysReportSignalStrength(isEnable);
        }
    }

    /**
     * TODO: Adding a function for each property is not good.
     * A fucntion of type getPhoneProp(propType) where propType is an
     * enum of GSM+CDMA+LTE props would be a better approach.
     *
     * Get "Restriction of menu options for manual PLMN selection" bit
     * status from EF_CSP data, this belongs to "Value Added Services Group".
     * @return true if this bit is set or EF_CSP data is unavailable,
     * false otherwise
     */
    @UnsupportedAppUsage
    public boolean isCspPlmnEnabled() {
        return false;
    }

    /**
     * Return an interface to retrieve the ISIM records for IMS, if available.
     * @return the interface to retrieve the ISIM records, or null if not supported
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public IsimRecords getIsimRecords() {
        Rlog.e(LOG_TAG, "getIsimRecords() is only supported on LTE devices");
        return null;
    }

    /**
     * Retrieves the MSISDN from the UICC. For GSM/UMTS phones, this is equivalent to
     * {@link #getLine1Number()}. For CDMA phones, {@link #getLine1Number()} returns
     * the MDN, so this method is provided to return the MSISDN on CDMA/LTE phones.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public String getMsisdn() {
        return null;
    }

    /**
     * Retrieves the EF_PNN from the UICC For GSM/UMTS phones.
     */
    public String getPlmn() {
        return null;
    }

    /**
     * Get the current for the default apn DataState. No change notification
     * exists at this interface -- use
     * {@link android.telephony.PhoneStateListener} instead.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public PhoneConstants.DataState getDataConnectionState() {
        return getDataConnectionState(ApnSetting.TYPE_DEFAULT_STRING);
    }

    public void notifyCallForwardingIndicator() {
    }

    /**
     * Sets the SIM voice message waiting indicator records.
     * @param line GSM Subscriber Profile Number, one-based. Only '1' is supported
     * @param countWaiting The number of messages waiting, if known. Use
     *                     -1 to indicate that an unknown number of
     *                      messages are waiting
     */
    public void setVoiceMessageWaiting(int line, int countWaiting) {
        // This function should be overridden by class GsmCdmaPhone.
        Rlog.e(LOG_TAG, "Error! This function should never be executed, inactive Phone.");
    }

    /**
     * Gets the USIM service table from the UICC, if present and available.
     * @return an interface to the UsimServiceTable record, or null if not available
     */
    public UsimServiceTable getUsimServiceTable() {
        IccRecords r = mIccRecords.get();
        return (r != null) ? r.getUsimServiceTable() : null;
    }

    /**
     * Gets the Uicc card corresponding to this phone.
     * @return the UiccCard object corresponding to the phone ID.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public UiccCard getUiccCard() {
        return mUiccController.getUiccCard(mPhoneId);
    }

    /**
     * Gets the Uicc port corresponding to this phone.
     * @return the UiccPort object corresponding to the phone ID.
     */
    public UiccPort getUiccPort() {
        return mUiccController.getUiccPort(mPhoneId);
    }

    /**
     * Set IMS registration state
     */
    public void setImsRegistrationState(boolean registered) {
    }

    /**
     * Return an instance of a IMS phone
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public Phone getImsPhone() {
        return mImsPhone;
    }

    @VisibleForTesting
    public void setImsPhone(ImsPhone imsPhone) {
        mImsPhone = imsPhone;
    }

    /**
     * Returns Carrier specific information that will be used to encrypt the IMSI and IMPI.
     * @param keyType whether the key is being used for WLAN or ePDG.
     * @param fallback whether or not to fall back to the encryption key info stored in carrier
     *                 config
     * @return ImsiEncryptionInfo which includes the Key Type, the Public Key
     *        {@link java.security.PublicKey} and the Key Identifier.
     *        The keyIdentifier This is used by the server to help it locate the private key to
     *        decrypt the permanent identity.
     */
    public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int keyType, boolean fallback) {
        return null;
    }

    /**
     * Sets the carrier information needed to encrypt the IMSI and IMPI.
     * @param imsiEncryptionInfo Carrier specific information that will be used to encrypt the
     *        IMSI and IMPI. This includes the Key type, the Public key
     *        {@link java.security.PublicKey} and the Key identifier.
     */
    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo) {
        return;
    }

    /**
     * Deletes all the keys for a given Carrier from the device keystore.
     * @param carrierId : the carrier ID which needs to be matched in the delete query
     */
    public void deleteCarrierInfoForImsiEncryption(int carrierId) {
        return;
    }

    public int getCarrierId() {
        return TelephonyManager.UNKNOWN_CARRIER_ID;
    }

    public String getCarrierName() {
        return null;
    }

    public int getMNOCarrierId() {
        return TelephonyManager.UNKNOWN_CARRIER_ID;
    }

    public int getSpecificCarrierId() {
        return TelephonyManager.UNKNOWN_CARRIER_ID;
    }

    public String getSpecificCarrierName() {
        return null;
    }

    public int getCarrierIdListVersion() {
        return TelephonyManager.UNKNOWN_CARRIER_ID_LIST_VERSION;
    }

    public int getEmergencyNumberDbVersion() {
        return TelephonyManager.INVALID_EMERGENCY_NUMBER_DB_VERSION;
    }

    public void resolveSubscriptionCarrierId(String simState) {
    }

    /**
     *  Resets the Carrier Keys in the database. This involves 2 steps:
     *  1. Delete the keys from the database.
     *  2. Send an intent to download new Certificates.
     */
    public void resetCarrierKeysForImsiEncryption() {
        return;
    }

    /**
     * Return if UT capability of ImsPhone is enabled or not
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isUtEnabled() {
        if (mImsPhone != null) {
            return mImsPhone.isUtEnabled();
        }
        return false;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void dispose() {
    }

    /**
     * Dials a number.
     *
     * @param dialString The number to dial.
     * @param dialArgs Parameters to dial with.
     * @return The Connection.
     * @throws CallStateException
     */
    protected Connection dialInternal(String dialString, DialArgs dialArgs)
            throws CallStateException {
        // dialInternal shall be overriden by GsmCdmaPhone
        return null;
    }

    /*
     * This function is for CSFB SS. GsmCdmaPhone overrides this function.
     */
    public void setCallWaiting(boolean enable, int serviceClass, Message onComplete) {
    }

    public void queryCLIP(Message onComplete) {
    }

    /*
     * Returns the subscription id.
     */
    @UnsupportedAppUsage
    public int getSubId() {
        if (SubscriptionController.getInstance() == null) {
            // TODO b/78359408 getInstance sometimes returns null in Treehugger tests, which causes
            // flakiness. Even though we haven't seen this crash in the wild we should keep this
            // check in until we've figured out the root cause.
            Rlog.e(LOG_TAG, "SubscriptionController.getInstance = null! Returning default subId");
            return SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
        }
        return SubscriptionController.getInstance().getSubIdUsingPhoneId(mPhoneId);
    }

    /**
     * Returns the phone id.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int getPhoneId() {
        return mPhoneId;
    }

    /**
     * Override the service provider name and the operator name for the current ICCID.
     */
    public boolean setOperatorBrandOverride(String brand) {
        return false;
    }

    /**
     * Override the roaming indicator for the current ICCID.
     */
    public boolean setRoamingOverride(List<String> gsmRoamingList,
            List<String> gsmNonRoamingList, List<String> cdmaRoamingList,
            List<String> cdmaNonRoamingList) {
        String iccId = getIccSerialNumber();
        if (TextUtils.isEmpty(iccId)) {
            return false;
        }

        setRoamingOverrideHelper(gsmRoamingList, GSM_ROAMING_LIST_OVERRIDE_PREFIX, iccId);
        setRoamingOverrideHelper(gsmNonRoamingList, GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX, iccId);
        setRoamingOverrideHelper(cdmaRoamingList, CDMA_ROAMING_LIST_OVERRIDE_PREFIX, iccId);
        setRoamingOverrideHelper(cdmaNonRoamingList, CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX, iccId);

        // Refresh.
        ServiceStateTracker tracker = getServiceStateTracker();
        if (tracker != null) {
            tracker.pollState();
        }
        return true;
    }

    private void setRoamingOverrideHelper(List<String> list, String prefix, String iccId) {
        SharedPreferences.Editor spEditor =
                PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        String key = prefix + iccId;
        if (list == null || list.isEmpty()) {
            spEditor.remove(key).commit();
        } else {
            spEditor.putStringSet(key, new HashSet<String>(list)).commit();
        }
    }

    public boolean isMccMncMarkedAsRoaming(String mccMnc) {
        return getRoamingOverrideHelper(GSM_ROAMING_LIST_OVERRIDE_PREFIX, mccMnc);
    }

    public boolean isMccMncMarkedAsNonRoaming(String mccMnc) {
        return getRoamingOverrideHelper(GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX, mccMnc);
    }

    public boolean isSidMarkedAsRoaming(int SID) {
        return getRoamingOverrideHelper(CDMA_ROAMING_LIST_OVERRIDE_PREFIX,
                Integer.toString(SID));
    }

    public boolean isSidMarkedAsNonRoaming(int SID) {
        return getRoamingOverrideHelper(CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX,
                Integer.toString(SID));
    }

    /**
     * Query the IMS Registration Status.
     *
     * @return true if IMS is Registered
     */
    public boolean isImsRegistered() {
        Phone imsPhone = mImsPhone;
        boolean isImsRegistered = false;
        if (imsPhone != null) {
            isImsRegistered = imsPhone.isImsRegistered();
        } else {
            ServiceStateTracker sst = getServiceStateTracker();
            if (sst != null) {
                isImsRegistered = sst.isImsRegistered();
            }
        }
        Rlog.d(LOG_TAG, "isImsRegistered =" + isImsRegistered);
        return isImsRegistered;
    }

    /**
     * Get Wifi Calling Feature Availability
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isWifiCallingEnabled() {
        Phone imsPhone = mImsPhone;
        boolean isWifiCallingEnabled = false;
        if (imsPhone != null) {
            isWifiCallingEnabled = imsPhone.isWifiCallingEnabled();
        }
        Rlog.d(LOG_TAG, "isWifiCallingEnabled =" + isWifiCallingEnabled);
        return isWifiCallingEnabled;
    }

    /**
     * @return true if the IMS capability for the registration technology specified is available,
     * false otherwise.
     */
    public boolean isImsCapabilityAvailable(int capability, int regTech) throws ImsException {
        Phone imsPhone = mImsPhone;
        boolean isAvailable = false;
        if (imsPhone != null) {
            isAvailable = imsPhone.isImsCapabilityAvailable(capability, regTech);
        }
        Rlog.d(LOG_TAG, "isImsCapabilityAvailable, capability=" + capability + ", regTech="
                + regTech + ", isAvailable=" + isAvailable);
        return isAvailable;
    }

    /**
     * Get Volte Feature Availability
     * @deprecated Use {@link #isVoiceOverCellularImsEnabled} instead.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Deprecated
    public boolean isVolteEnabled() {
        return isVoiceOverCellularImsEnabled();
    }

    /**
     * @return {@code true} if voice over IMS on cellular is enabled, {@code false} otherwise.
     */
    public boolean isVoiceOverCellularImsEnabled() {
        Phone imsPhone = mImsPhone;
        boolean isVolteEnabled = false;
        if (imsPhone != null) {
            isVolteEnabled = imsPhone.isVoiceOverCellularImsEnabled();
        }
        Rlog.d(LOG_TAG, "isVoiceOverCellularImsEnabled=" + isVolteEnabled);
        return isVolteEnabled;
    }

    /**
     * @return the IMS MmTel Registration technology for this Phone, defined in
     * {@link ImsRegistrationImplBase}.
     */
    public int getImsRegistrationTech() {
        Phone imsPhone = mImsPhone;
        int regTech = ImsRegistrationImplBase.REGISTRATION_TECH_NONE;
        if (imsPhone != null) {
            regTech = imsPhone.getImsRegistrationTech();
        }
        Rlog.d(LOG_TAG, "getImsRegistrationTechnology =" + regTech);
        return regTech;
    }

    /**
     * Get the IMS MmTel Registration technology for this Phone, defined in
     * {@link ImsRegistrationImplBase}.
     */
    public void getImsRegistrationTech(Consumer<Integer> callback) {
        Phone imsPhone = mImsPhone;
        if (imsPhone != null) {
            imsPhone.getImsRegistrationTech(callback);
        } else {
            callback.accept(ImsRegistrationImplBase.REGISTRATION_TECH_NONE);
        }
    }

    /**
     * Asynchronously get the IMS MmTel Registration state for this Phone.
     */
    public void getImsRegistrationState(Consumer<Integer> callback) {
        Phone imsPhone = mImsPhone;
        if (imsPhone != null) {
            imsPhone.getImsRegistrationState(callback);
        } else {
            callback.accept(RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED);
        }
    }


    private boolean getRoamingOverrideHelper(String prefix, String key) {
        String iccId = getIccSerialNumber();
        if (TextUtils.isEmpty(iccId) || TextUtils.isEmpty(key)) {
            return false;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        Set<String> value = sp.getStringSet(prefix + iccId, null);
        if (value == null) {
            return false;
        }
        return value.contains(key);
    }

    /**
     * @return returns the latest radio state from the modem
     */
    public int getRadioPowerState() {
        return mCi.getRadioState();
    }

    /**
     * Is Radio Present on the device and is it accessible
     */
    public boolean isRadioAvailable() {
        return mCi.getRadioState() != TelephonyManager.RADIO_POWER_UNAVAILABLE;
    }

    /**
     * Is Radio turned on
     */
    public boolean isRadioOn() {
        return mCi.getRadioState() == TelephonyManager.RADIO_POWER_ON;
    }

    /**
     * shutdown Radio gracefully
     */
    public void shutdownRadio() {
        getServiceStateTracker().requestShutdown();
    }

    /**
     * Return true if the device is shutting down.
     */
    public boolean isShuttingDown() {
        return getServiceStateTracker().isDeviceShuttingDown();
    }

    /**
     *  Set phone radio capability
     *
     *  @param rc the phone radio capability defined in
     *         RadioCapability. It's a input object used to transfer parameter to logic modem
     *  @param response Callback message.
     */
    public void setRadioCapability(RadioCapability rc, Message response) {
        mCi.setRadioCapability(rc, response);
    }

    /**
     *  Get phone radio access family
     *
     *  @return a bit mask to identify the radio access family.
     */
    public int getRadioAccessFamily() {
        final RadioCapability rc = getRadioCapability();
        return (rc == null ? RadioAccessFamily.RAF_UNKNOWN : rc.getRadioAccessFamily());
    }

    /**
     *  Get the associated data modems Id.
     *
     *  @return a String containing the id of the data modem
     */
    public String getModemUuId() {
        final RadioCapability rc = getRadioCapability();
        return (rc == null ? "" : rc.getLogicalModemUuid());
    }

    /**
     *  Get phone radio capability
     *
     *  @return the capability of the radio defined in RadioCapability
     */
    public RadioCapability getRadioCapability() {
        return mRadioCapability.get();
    }

    /**
     *  The RadioCapability has changed. This comes up from the RIL and is called when radios first
     *  become available or after a capability switch.  The flow is we use setRadioCapability to
     *  request a change with the RIL and get an UNSOL response with the new data which gets set
     *  here.
     *
     *  @param rc the phone radio capability currently in effect for this phone.
     *  @param capabilitySwitched whether this method called after a radio capability switch
     *      completion or called when radios first become available.
     */
    public void radioCapabilityUpdated(RadioCapability rc, boolean capabilitySwitched) {
        // Called when radios first become available or after a capability switch
        // Update the cached value
        mRadioCapability.set(rc);

        if (SubscriptionManager.isValidSubscriptionId(getSubId())) {
            boolean restoreSelection = !mContext.getResources().getBoolean(
                    com.android.internal.R.bool.skip_restoring_network_selection);
            sendSubscriptionSettings(restoreSelection);
        }

        // When radio capability switch is done, query IMEI value and update it in Phone objects
        // to make it in sync with the IMEI value currently used by Logical-Modem.
        if (capabilitySwitched) {
            mCi.getDeviceIdentity(obtainMessage(EVENT_GET_DEVICE_IDENTITY_DONE));
        }
    }

    public void sendSubscriptionSettings(boolean restoreNetworkSelection) {
        // Send settings down
        if (mIsAllowedNetworkTypesLoadedFromDb) {
            updateAllowedNetworkTypes(null);
        }

        if (restoreNetworkSelection) {
            restoreSavedNetworkSelection(null);
        }

        updateUsageSetting();
    }

    private int getResolvedUsageSetting(int subId) {
        SubscriptionInfo subInfo = SubscriptionController.getInstance().getSubscriptionInfo(subId);

        if (subInfo == null
                || subInfo.getUsageSetting() == SubscriptionManager.USAGE_SETTING_UNKNOWN) {
            loge("Failed to get SubscriptionInfo for subId=" + subId);
            return SubscriptionManager.USAGE_SETTING_UNKNOWN;
        }

        if (subInfo.getUsageSetting() != SubscriptionManager.USAGE_SETTING_DEFAULT) {
            return subInfo.getUsageSetting();
        }

        if (subInfo.isOpportunistic()) {
            return SubscriptionManager.USAGE_SETTING_DATA_CENTRIC;
        } else {
            return mContext.getResources().getInteger(
                    com.android.internal.R.integer.config_default_cellular_usage_setting);
        }
    }

    /**
     * Attempt to update the usage setting.
     *
     * @return whether the usage setting will be updated (used for test)
     */
    public boolean updateUsageSetting() {
        if (!mIsUsageSettingSupported) return false;

        final int subId = getSubId();
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return false;

        final int lastPreferredUsageSetting = mPreferredUsageSetting;

        mPreferredUsageSetting = getResolvedUsageSetting(subId);
        if (mPreferredUsageSetting == SubscriptionManager.USAGE_SETTING_UNKNOWN) {
            loge("Usage Setting is Supported but Preferred Setting Unknown!");
            return false;
        }

        // We might get a lot of requests to update, so definitely we don't want to hammer
        // the modem with multiple duplicate requests for usage setting updates
        if (mPreferredUsageSetting == lastPreferredUsageSetting) return false;

        String logStr = "mPreferredUsageSetting=" + mPreferredUsageSetting
                + ", lastPreferredUsageSetting=" + lastPreferredUsageSetting
                + ", mUsageSettingFromModem=" + mUsageSettingFromModem;
        logd(logStr);
        mLocalLog.log(logStr);

        // If the modem value hasn't been updated, request it.
        if (mUsageSettingFromModem == SubscriptionManager.USAGE_SETTING_UNKNOWN) {
            mCi.getUsageSetting(obtainMessage(EVENT_GET_USAGE_SETTING_DONE));
            // If the modem value is already known, and the value has changed, proceed to update.
        } else if (mPreferredUsageSetting != mUsageSettingFromModem) {
            mCi.setUsageSetting(obtainMessage(EVENT_SET_USAGE_SETTING_DONE),
                    mPreferredUsageSetting);
        }
        return true;
    }



    /**
     * Registers the handler when phone radio  capability is changed.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForRadioCapabilityChanged(Handler h, int what, Object obj) {
        mCi.registerForRadioCapabilityChanged(h, what, obj);
    }

    /**
     * Unregister for notifications when phone radio type and access technology is changed.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForRadioCapabilityChanged(Handler h) {
        mCi.unregisterForRadioCapabilityChanged(this);
    }

    /**
     * Determines if the connection to IMS services are available yet.
     * @return {@code true} if the connection to IMS services are available.
     */
    public boolean isImsAvailable() {
        if (mImsPhone == null) {
            return false;
        }

        return mImsPhone.isImsAvailable();
    }

    /**
     * Determines if video calling is enabled for the phone.
     *
     * @return {@code true} if video calling is enabled, {@code false} otherwise.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isVideoEnabled() {
        Phone imsPhone = mImsPhone;
        if (imsPhone != null) {
            return imsPhone.isVideoEnabled();
        }
        return false;
    }

    /**
     * Returns the status of Link Capacity Estimation (LCE) service.
     */
    public int getLceStatus() {
        return mLceStatus;
    }

    /**
     * Returns the modem activity information
     */
    public void getModemActivityInfo(Message response, WorkSource workSource)  {
        mCi.getModemActivityInfo(response, workSource);
    }

    /**
     * Starts LCE service after radio becomes available.
     * LCE service state may get destroyed on the modem when radio becomes unavailable.
     */
    public void startLceAfterRadioIsAvailable() {
        mCi.startLceService(DEFAULT_REPORT_INTERVAL_MS, LCE_PULL_MODE,
                obtainMessage(EVENT_CONFIG_LCE));
    }

    /**
     * Control the data throttling at modem.
     *
     * @param result Message that will be sent back to the requester
     * @param workSource calling Worksource
     * @param dataThrottlingAction the DataThrottlingAction that is being requested. Defined in
     *      android.telephony.TelephonyManger.
     * @param completionWindowMillis milliseconds in which data throttling action has to be
     *      achieved.
     */
    public void setDataThrottling(Message result, WorkSource workSource,
            int dataThrottlingAction, long completionWindowMillis) {
        mCi.setDataThrottling(result, workSource, dataThrottlingAction, completionWindowMillis);
    }

    /**
     * Set allowed carriers
     */
    public void setAllowedCarriers(CarrierRestrictionRules carrierRestrictionRules,
            Message response, WorkSource workSource) {
        mCi.setAllowedCarriers(carrierRestrictionRules, response, workSource);
    }

    /** Sets the SignalStrength reporting criteria. */
    public void setLinkCapacityReportingCriteria(int[] dlThresholds, int[] ulThresholds, int ran) {
        // no-op default implementation
    }

    /**
     * Get allowed carriers
     */
    public void getAllowedCarriers(Message response, WorkSource workSource) {
        mCi.getAllowedCarriers(response, workSource);
    }

    /**
     * Returns the locale based on the carrier properties (such as {@code ro.carrier}) and
     * SIM preferences.
     */
    public Locale getLocaleFromSimAndCarrierPrefs() {
        final IccRecords records = mIccRecords.get();
        if (records != null && records.getSimLanguage() != null) {
            return new Locale(records.getSimLanguage());
        }

        return getLocaleFromCarrierProperties();
    }

    public boolean updateCurrentCarrierInProvider() {
        return false;
    }

    /**
     * @return True if all data connections are disconnected.
     */
    public boolean areAllDataDisconnected() {
        if (mAccessNetworksManager != null) {
            for (int transport : mAccessNetworksManager.getAvailableTransports()) {
                if (getDcTracker(transport) != null
                        && !getDcTracker(transport).areAllDataDisconnected()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void registerForAllDataDisconnected(Handler h, int what) {
        mAllDataDisconnectedRegistrants.addUnique(h, what, null);
        if (mAccessNetworksManager != null) {
            for (int transport : mAccessNetworksManager.getAvailableTransports()) {
                if (getDcTracker(transport) != null
                        && !getDcTracker(transport).areAllDataDisconnected()) {
                    getDcTracker(transport).registerForAllDataDisconnected(
                            this, EVENT_ALL_DATA_DISCONNECTED);
                }
            }
        }
    }

    public void unregisterForAllDataDisconnected(Handler h) {
        mAllDataDisconnectedRegistrants.remove(h);
    }

    public DataEnabledSettings getDataEnabledSettings() {
        return mDataEnabledSettings;
    }

    @UnsupportedAppUsage
    public IccSmsInterfaceManager getIccSmsInterfaceManager(){
        return null;
    }

    protected boolean isMatchGid(String gid) {
        String gid1 = getGroupIdLevel1();
        int gidLength = gid.length();
        if (!TextUtils.isEmpty(gid1) && (gid1.length() >= gidLength)
                && gid1.substring(0, gidLength).equalsIgnoreCase(gid)) {
            return true;
        }
        return false;
    }

    public static void checkWfcWifiOnlyModeBeforeDial(Phone imsPhone, int phoneId, Context context)
            throws CallStateException {
        if (imsPhone == null || !imsPhone.isWifiCallingEnabled()) {
            ImsManager imsManager = ImsManager.getInstance(context, phoneId);
            boolean wfcWiFiOnly = (imsManager.isWfcEnabledByPlatform()
                    && imsManager.isWfcEnabledByUser() && (imsManager.getWfcMode()
                    == ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY));
            if (wfcWiFiOnly) {
                throw new CallStateException(
                        CallStateException.ERROR_OUT_OF_SERVICE,
                        "WFC Wi-Fi Only Mode: IMS not registered");
            }
        }
    }

    public void startRingbackTone() {
    }

    public void stopRingbackTone() {
    }

    public void callEndCleanupHandOverCallIfAny() {
    }

    /**
     * Cancel USSD session.
     *
     * @param msg The message to dispatch when the USSD session terminated.
     */
    public void cancelUSSD(Message msg) {
    }

    /**
     * Set boolean broadcastEmergencyCallStateChanges
     */
    public abstract void setBroadcastEmergencyCallStateChanges(boolean broadcast);

    public abstract void sendEmergencyCallStateChange(boolean callActive);

    /**
     * This function returns the parent phone of the current phone. It is applicable
     * only for IMS phone (function is overridden by ImsPhone). For others the phone
     * object itself is returned.
     * @return
     */
    public Phone getDefaultPhone() {
        return this;
    }

    /**
     * SIP URIs aliased to the current subscriber given by the IMS implementation.
     * Applicable only on IMS; used in absence of line1number.
     * @return array of SIP URIs aliased to the current subscriber
     */
    public Uri[] getCurrentSubscriberUris() {
        return null;
    }

    public AppSmsManager getAppSmsManager() {
        return mAppSmsManager;
    }

    /**
     * Set SIM card power state.
     * @param state State of SIM (power down, power up, pass through)
     * - {@link android.telephony.TelephonyManager#CARD_POWER_DOWN}
     * - {@link android.telephony.TelephonyManager#CARD_POWER_UP}
     * - {@link android.telephony.TelephonyManager#CARD_POWER_UP_PASS_THROUGH}
     **/
    public void setSimPowerState(int state, Message result, WorkSource workSource) {
        mCi.setSimCardPower(state, result, workSource);
    }

    /**
     * Enable or disable Voice over NR (VoNR)
     * @param enabled enable or disable VoNR.
     **/
    public void setVoNrEnabled(boolean enabled, Message result, WorkSource workSource) {
        mCi.setVoNrEnabled(enabled, result, workSource);
    }

    /**
     * Is voice over NR enabled
     */
    public void isVoNrEnabled(Message message, WorkSource workSource) {
        mCi.isVoNrEnabled(message, workSource);
    }

    public void setCarrierTestOverride(String mccmnc, String imsi, String iccid, String gid1,
            String gid2, String pnn, String spn, String carrierPrivilegeRules, String apn) {
    }

    /**
     * Check if the device can only make the emergency call. The device is emergency call only if
     * none of the phone is in service, and one of them has the capability to make the emergency
     * call.
     *
     * @return {@code True} if the device is emergency call only, otherwise return {@code False}.
     */
    public static boolean isEmergencyCallOnly() {
        boolean isEmergencyCallOnly = false;
        for (Phone phone : PhoneFactory.getPhones()) {
            if (phone != null) {
                ServiceStateTracker sst = phone.getServiceStateTracker();
                ServiceState ss = sst.getServiceState();
                // Combined reg state is in service, hence the device is not emergency call only.
                if (sst.getCombinedRegState(ss) == ServiceState.STATE_IN_SERVICE) {
                    return false;
                }
                isEmergencyCallOnly |= ss.isEmergencyOnly();
            }
        }
        return isEmergencyCallOnly;
    }

    /**
     * Get data connection tracker based on the transport type
     *
     * @param transportType Transport type defined in AccessNetworkConstants.TransportType
     * @return The data connection tracker. Null if not found.
     */
    public @Nullable DcTracker getDcTracker(int transportType) {
        return mDcTrackers.get(transportType);
    }

    // Return true if either CSIM or RUIM app is present. By default it returns false.
    public boolean isCdmaSubscriptionAppPresent() {
        return false;
    }

    /**
     * Enable or disable uicc applications.
     * @param enable whether to enable or disable uicc applications.
     * @param onCompleteMessage callback for async operation. Ignored if blockingCall is true.
     */
    public void enableUiccApplications(boolean enable, Message onCompleteMessage) {}

    /**
     * Whether disabling a physical subscription is supported or not.
     */
    public boolean canDisablePhysicalSubscription() {
        return false;
    }

    /**
     * Get the HAL version.
     *
     * @return the current HalVersion
     */
    public HalVersion getHalVersion() {
        if (mCi != null && mCi instanceof RIL) {
            return ((RIL) mCi).getHalVersion();
        }
        return RIL.RADIO_HAL_VERSION_UNKNOWN;
    }

    /**
     * Get the SIM's MCC/MNC
     *
     * @return MCC/MNC in string format, empty string if not available.
     */
    @NonNull
    public String getOperatorNumeric() {
        return "";
    }

    /** Returns the {@link VoiceCallSessionStats} for this phone ID. */
    public VoiceCallSessionStats getVoiceCallSessionStats() {
        return mVoiceCallSessionStats;
    }

    /** Sets the {@link VoiceCallSessionStats} mock for this phone ID during unit testing. */
    @VisibleForTesting
    public void setVoiceCallSessionStats(VoiceCallSessionStats voiceCallSessionStats) {
        mVoiceCallSessionStats = voiceCallSessionStats;
    }

    /** Returns the {@link SmsStats} for this phone ID. */
    public SmsStats getSmsStats() {
        return mSmsStats;
    }

    /** Sets the {@link SmsStats} mock for this phone ID during unit testing. */
    @VisibleForTesting
    public void setSmsStats(SmsStats smsStats) {
        mSmsStats = smsStats;
    }

    /** @hide */
    public CarrierPrivilegesTracker getCarrierPrivilegesTracker() {
        return null;
    }

    public boolean useSsOverIms(Message onComplete) {
        return false;
    }

    /**
     * Check if device is idle. Device is idle when it is not in high power consumption mode.
     *
     * @see DeviceStateMonitor#shouldEnableHighPowerConsumptionIndications()
     *
     * @return true if device is idle
     */
    public boolean isDeviceIdle() {
        DeviceStateMonitor dsm = getDeviceStateMonitor();
        if (dsm == null) {
            Rlog.e(LOG_TAG, "isDeviceIdle: DeviceStateMonitor is null");
            return false;
        }
        return !dsm.shouldEnableHighPowerConsumptionIndications();
    }

    /**
     * Get notified when device idleness state has changed
     *
     * @param isIdle true if the new state is idle
     */
    public void notifyDeviceIdleStateChanged(boolean isIdle) {
        SignalStrengthController ssc = getSignalStrengthController();
        if (ssc == null) {
            Rlog.e(LOG_TAG, "notifyDeviceIdleStateChanged: SignalStrengthController is null");
            return;
        }
        ssc.onDeviceIdleStateChanged(isIdle);
    }

    /**
     * Returns a list of the equivalent home PLMNs (EF_EHPLMN) from the USIM app.
     *
     * @return A list of equivalent home PLMNs. Returns an empty list if EF_EHPLMN is empty or
     * does not exist on the SIM card.
     */
    public @NonNull List<String> getEquivalentHomePlmns() {
        return Collections.emptyList();
    }

    /**
     *
     * @return
     */
    public @NonNull List<String> getDataServicePackages() {
        return Collections.emptyList();
    }

    /**
     * Return link bandwidth estimator
     */
    public LinkBandwidthEstimator getLinkBandwidthEstimator() {
        return mLinkBandwidthEstimator;
    }

    /**
     * Request to get the current slicing configuration including URSP rules and
     * NSSAIs (configured, allowed and rejected).
     */
    public void getSlicingConfig(Message response) {
        mCi.getSlicingConfig(response);
    }

    /**
     * Returns the InboundSmsHandler object for this phone
     */
    public InboundSmsHandler getInboundSmsHandler(boolean is3gpp2) {
        return null;
    }

    /**
     * @return The data network controller
     */
    public @Nullable DataNetworkController getDataNetworkController() {
        return mDataNetworkController;
    }

    /**
     * @return The data settings manager
     */
    public @Nullable DataSettingsManager getDataSettingsManager() {
        if (mDataNetworkController == null) return null;
        return mDataNetworkController.getDataSettingsManager();
    }

    /**
     * Used in unit tests to set whether the AllowedNetworkTypes is loaded from Db.  Should not
     * be used otherwise.
     *
     * @return {@code true} if the AllowedNetworkTypes is loaded from Db,
     * {@code false} otherwise.
     */
    @VisibleForTesting
    public boolean isAllowedNetworkTypesLoadedFromDb() {
        return mIsAllowedNetworkTypesLoadedFromDb;
    }

    /**
     * @return {@code true} if using the new telephony data stack.
     */
    // This flag and the old data stack code will be deleted in Android 14.
    public boolean isUsingNewDataStack() {
        return mNewDataStackEnabled;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Phone: subId=" + getSubId());
        pw.println(" mPhoneId=" + mPhoneId);
        pw.println(" mCi=" + mCi);
        pw.println(" mDnsCheckDisabled=" + mDnsCheckDisabled);
        pw.println(" mDoesRilSendMultipleCallRing=" + mDoesRilSendMultipleCallRing);
        pw.println(" mCallRingContinueToken=" + mCallRingContinueToken);
        pw.println(" mCallRingDelay=" + mCallRingDelay);
        pw.println(" mIsVoiceCapable=" + mIsVoiceCapable);
        pw.println(" mIccRecords=" + mIccRecords.get());
        pw.println(" mUiccApplication=" + mUiccApplication.get());
        pw.println(" mSmsStorageMonitor=" + mSmsStorageMonitor);
        pw.println(" mSmsUsageMonitor=" + mSmsUsageMonitor);
        pw.flush();
        pw.println(" mLooper=" + mLooper);
        pw.println(" mContext=" + mContext);
        pw.println(" mNotifier=" + mNotifier);
        pw.println(" mSimulatedRadioControl=" + mSimulatedRadioControl);
        pw.println(" mUnitTestMode=" + mUnitTestMode);
        pw.println(" isDnsCheckDisabled()=" + isDnsCheckDisabled());
        pw.println(" getUnitTestMode()=" + getUnitTestMode());
        pw.println(" getState()=" + getState());
        pw.println(" getIccSerialNumber()=" + getIccSerialNumber());
        pw.println(" getIccRecordsLoaded()=" + getIccRecordsLoaded());
        pw.println(" getMessageWaitingIndicator()=" + getMessageWaitingIndicator());
        pw.println(" getCallForwardingIndicator()=" + getCallForwardingIndicator());
        pw.println(" isInEmergencyCall()=" + isInEmergencyCall());
        pw.flush();
        pw.println(" isInEcm()=" + isInEcm());
        pw.println(" getPhoneName()=" + getPhoneName());
        pw.println(" getPhoneType()=" + getPhoneType());
        pw.println(" getVoiceMessageCount()=" + getVoiceMessageCount());
        pw.println(" needsOtaServiceProvisioning=" + needsOtaServiceProvisioning());
        pw.println(" isInEmergencySmsMode=" + isInEmergencySmsMode());
        pw.println(" isEcmCanceledForEmergency=" + isEcmCanceledForEmergency());
        pw.println(" isUsingNewDataStack=" + isUsingNewDataStack());
        pw.println(" service state=" + getServiceState());
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");

        if (mImsPhone != null) {
            try {
                mImsPhone.dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (mAccessNetworksManager != null) {
            for (int transport : mAccessNetworksManager.getAvailableTransports()) {
                if (getDcTracker(transport) != null) {
                    getDcTracker(transport).dump(fd, pw, args);
                    pw.flush();
                    pw.println("++++++++++++++++++++++++++++++++");
                }
            }
        }

        if (mDataNetworkController != null) {
            try {
                mDataNetworkController.dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (getServiceStateTracker() != null) {
            try {
                getServiceStateTracker().dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (getEmergencyNumberTracker() != null) {
            try {
                getEmergencyNumberTracker().dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (getDisplayInfoController() != null) {
            try {
                getDisplayInfoController().dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (mCarrierResolver != null) {
            try {
                mCarrierResolver.dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (mCarrierActionAgent != null) {
            try {
                mCarrierActionAgent.dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (mCarrierSignalAgent != null) {
            try {
                mCarrierSignalAgent.dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (getCallTracker() != null) {
            try {
                getCallTracker().dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (mSimActivationTracker != null) {
            try {
                mSimActivationTracker.dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (mDeviceStateMonitor != null) {
            pw.println("DeviceStateMonitor:");
            mDeviceStateMonitor.dump(fd, pw, args);
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (mSignalStrengthController != null) {
            pw.println("SignalStrengthController:");
            mSignalStrengthController.dump(fd, pw, args);
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (mAccessNetworksManager != null) {
            mAccessNetworksManager.dump(fd, pw, args);
        }

        if (mCi != null && mCi instanceof RIL) {
            try {
                ((RIL)mCi).dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (getCarrierPrivilegesTracker() != null) {
            pw.println("CarrierPrivilegesTracker:");
            getCarrierPrivilegesTracker().dump(fd, pw, args);
            pw.println("++++++++++++++++++++++++++++++++");
        }

        if (getLinkBandwidthEstimator() != null) {
            pw.println("LinkBandwidthEstimator:");
            getLinkBandwidthEstimator().dump(fd, pw, args);
            pw.println("++++++++++++++++++++++++++++++++");
        }

        pw.println("Phone Local Log: ");
        if (mLocalLog != null) {
            try {
                mLocalLog.dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
    }

    private void logd(String s) {
        Rlog.d(LOG_TAG, "[" + mPhoneId + "] " + s);
    }

    private void logi(String s) {
        Rlog.i(LOG_TAG, "[" + mPhoneId + "] " + s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, "[" + mPhoneId + "] " + s);
    }

    private static String pii(String s) {
        return Rlog.pii(LOG_TAG, s);
    }
}

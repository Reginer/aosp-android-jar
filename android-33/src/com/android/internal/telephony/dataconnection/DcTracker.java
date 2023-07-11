/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony.dataconnection;

import static android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE;
import static android.net.NetworkPolicyManager.SUBSCRIPTION_OVERRIDE_CONGESTED;
import static android.net.NetworkPolicyManager.SUBSCRIPTION_OVERRIDE_UNMETERED;
import static android.telephony.TelephonyManager.NETWORK_TYPE_LTE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_NR;
import static android.telephony.data.DataCallResponse.HANDOVER_FAILURE_MODE_DO_FALLBACK;
import static android.telephony.data.DataCallResponse.HANDOVER_FAILURE_MODE_LEGACY;
import static android.telephony.data.DataCallResponse.HANDOVER_FAILURE_MODE_NO_FALLBACK_RETRY_SETUP_NORMAL;

import static com.android.internal.telephony.RILConstants.DATA_PROFILE_DEFAULT;
import static com.android.internal.telephony.RILConstants.DATA_PROFILE_INVALID;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkPolicyManager;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Telephony;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.ApnType;
import android.telephony.Annotation.DataFailureCause;
import android.telephony.Annotation.NetworkType;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.DataFailCause;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PcoData;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.ServiceState.RilRadioTechnology;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionPlan;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyFrameworkInitializer;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.SimState;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataCallResponse.HandoverFailureMode;
import android.telephony.data.DataProfile;
import android.telephony.data.ThrottleStatus;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.WindowManager;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RetryManager;
import com.android.internal.telephony.SettingsObserver;
import com.android.internal.telephony.SubscriptionInfoUpdater;
import com.android.internal.telephony.data.DataConfigManager;
import com.android.internal.telephony.data.PhoneSwitcher;
import com.android.internal.telephony.dataconnection.DataConnectionReasons.DataAllowedReasonType;
import com.android.internal.telephony.dataconnection.DataConnectionReasons.DataDisallowedReasonType;
import com.android.internal.telephony.dataconnection.DataEnabledSettings.DataEnabledChangedReason;
import com.android.internal.telephony.metrics.DataStallRecoveryStats;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.util.ArrayUtils;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.AsyncChannel;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * {@hide}
 */
public class DcTracker extends Handler {
    protected static final boolean DBG = true;
    private static final boolean VDBG = false; // STOPSHIP if true
    private static final boolean VDBG_STALL = false; // STOPSHIP if true
    private static final boolean RADIO_TESTS = false;
    private static final String NOTIFICATION_TAG = DcTracker.class.getSimpleName();

    @IntDef(value = {
            REQUEST_TYPE_NORMAL,
            REQUEST_TYPE_HANDOVER,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RequestNetworkType {}

    /**
     * Normal request for {@link #requestNetwork(NetworkRequest, int, Message)}. For request
     * network, this adds the request to the {@link ApnContext}. If there were no network request
     * attached to the {@link ApnContext} earlier, this request setups a data connection.
     */
    public static final int REQUEST_TYPE_NORMAL = 1;

    /**
     * Handover request for {@link #requestNetwork(NetworkRequest, int, Message)} or
     * {@link #releaseNetwork(NetworkRequest, int)}. For request network, this
     * initiates the handover data setup process. The existing data connection will be seamlessly
     * handover to the new network. For release network, this performs a data connection softly
     * clean up at the underlying layer (versus normal data release).
     */
    public static final int REQUEST_TYPE_HANDOVER = 2;

    @IntDef(value = {
            RELEASE_TYPE_NORMAL,
            RELEASE_TYPE_DETACH,
            RELEASE_TYPE_HANDOVER,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ReleaseNetworkType {}

    /**
     * For release network, this is just removing the network request from the {@link ApnContext}.
     * Note this does not tear down the physical data connection. Normally the data connection is
     * torn down by connectivity service directly calling {@link NetworkAgent#unwanted()}.
     */
    public static final int RELEASE_TYPE_NORMAL = 1;

    /**
     * Detach request for {@link #releaseNetwork(NetworkRequest, int)} only. This
     * forces the APN context detach from the data connection. If this {@link ApnContext} is the
     * last one attached to the data connection, the data connection will be torn down, otherwise
     * the data connection remains active.
     */
    public static final int RELEASE_TYPE_DETACH = 2;

    /**
     * Handover request for {@link #releaseNetwork(NetworkRequest, int)}. For release
     * network, this performs a data connection softly clean up at the underlying layer (versus
     * normal data release).
     */
    public static final int RELEASE_TYPE_HANDOVER = 3;

    /** The extras for handover completion message */
    public static final String DATA_COMPLETE_MSG_EXTRA_NETWORK_REQUEST = "extra_network_request";
    public static final String DATA_COMPLETE_MSG_EXTRA_TRANSPORT_TYPE = "extra_transport_type";
    public static final String DATA_COMPLETE_MSG_EXTRA_SUCCESS = "extra_success";
    /**
     * The flag indicates whether after handover failure, the data connection should remain on the
     * original transport.
     */
    public static final String DATA_COMPLETE_MSG_EXTRA_HANDOVER_FAILURE_FALLBACK =
            "extra_handover_failure_fallback";

    private final String mLogTag;

    public AtomicBoolean isCleanupRequired = new AtomicBoolean(false);

    private final TelephonyManager mTelephonyManager;

    private final AlarmManager mAlarmManager;

    /* Currently requested APN type (TODO: This should probably be a parameter not a member) */
    private int mRequestedApnType = ApnSetting.TYPE_DEFAULT;

    // All data enabling/disabling related settings
    private final DataEnabledSettings mDataEnabledSettings;

    /**
     * After detecting a potential connection problem, this is the max number
     * of subsequent polls before attempting recovery.
     */
    // 1 sec. default polling interval when screen is on.
    private static final int POLL_NETSTAT_MILLIS = 1000;
    // 10 min. default polling interval when screen is off.
    private static final int POLL_NETSTAT_SCREEN_OFF_MILLIS = 1000*60*10;
    // Default sent packets without ack which triggers initial recovery steps
    private static final int NUMBER_SENT_PACKETS_OF_HANG = 10;

    // Default for the data stall alarm while non-aggressive stall detection
    private static final int DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 1000 * 60 * 6;
    // Default for the data stall alarm for aggressive stall detection
    private static final int DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 1000 * 60;

    private static final boolean DATA_STALL_SUSPECTED = true;
    protected static final boolean DATA_STALL_NOT_SUSPECTED = false;

    private static final String INTENT_DATA_STALL_ALARM =
            "com.android.internal.telephony.data-stall";
    // Tag for tracking stale alarms
    private static final String INTENT_DATA_STALL_ALARM_EXTRA_TAG = "data_stall_alarm_extra_tag";
    private static final String INTENT_DATA_STALL_ALARM_EXTRA_TRANSPORT_TYPE =
            "data_stall_alarm_extra_transport_type";

    // Unique id for no data notification on setup data permanently failed.
    private static final int NO_DATA_NOTIFICATION = 1001;

    /** The higher index has higher priority. */
    private static final DctConstants.State[] DATA_CONNECTION_STATE_PRIORITIES = {
            DctConstants.State.IDLE,
            DctConstants.State.DISCONNECTING,
            DctConstants.State.CONNECTING,
            DctConstants.State.CONNECTED,
    };

    private DcTesterFailBringUpAll mDcTesterFailBringUpAll;
    private DcController mDcc;

    /** kept in sync with mApnContexts
     * Higher numbers are higher priority and sorted so highest priority is first */
    private ArrayList<ApnContext> mPrioritySortedApnContexts = new ArrayList<>();

    /** all APN settings applicable to the current carrier */
    private ArrayList<ApnSetting> mAllApnSettings = new ArrayList<>();

    /** preferred apn */
    private ApnSetting mPreferredApn = null;

    /** Is packet service restricted by network */
    private boolean mIsPsRestricted = false;

    /** emergency apn Setting*/
    private ApnSetting mEmergencyApn = null;

    /* Once disposed dont handle any messages */
    private boolean mIsDisposed = false;

    private ContentResolver mResolver;

    /* Set to true with CMD_ENABLE_MOBILE_PROVISIONING */
    private boolean mIsProvisioning = false;

    /* The Url passed as object parameter in CMD_ENABLE_MOBILE_PROVISIONING */
    private String mProvisioningUrl = null;

    /* Indicating data service is bound or not */
    private boolean mDataServiceBound = false;

    /* Intent to hide/show the sign-in error notification for provisioning */
    private static final String INTENT_PROVISION = "com.android.internal.telephony.PROVISION";

    /**
     * Extra containing the phone ID for INTENT_PROVISION
     * Must be kept consistent with NetworkNotificationManager#setProvNotificationVisible.
     * TODO: refactor the deprecated API to prevent hardcoding values.
     */
    private static final String EXTRA_PROVISION_PHONE_ID = "provision.phone.id";

    /* Intent for the provisioning apn alarm */
    private static final String INTENT_PROVISIONING_APN_ALARM =
            "com.android.internal.telephony.provisioning_apn_alarm";

    /* Tag for tracking stale alarms */
    private static final String PROVISIONING_APN_ALARM_TAG_EXTRA = "provisioning.apn.alarm.tag";

    /* Debug property for overriding the PROVISIONING_APN_ALARM_DELAY_IN_MS */
    private static final String DEBUG_PROV_APN_ALARM = "persist.debug.prov_apn_alarm";

    /* Default for the provisioning apn alarm timeout */
    private static final int PROVISIONING_APN_ALARM_DELAY_IN_MS_DEFAULT = 1000 * 60 * 15;

    /* The provision apn alarm intent used to disable the provisioning apn */
    private PendingIntent mProvisioningApnAlarmIntent = null;

    /* Used to track stale provisioning apn alarms */
    private int mProvisioningApnAlarmTag = (int) SystemClock.elapsedRealtime();

    private AsyncChannel mReplyAc = new AsyncChannel();

    private final LocalLog mDataRoamingLeakageLog = new LocalLog(32);
    private final LocalLog mApnSettingsInitializationLog = new LocalLog(32);

    /* 5G connection reevaluation watchdog alarm constants */
    private long mWatchdogTimeMs = 1000 * 60 * 60;
    private boolean mWatchdog = false;

    /* Default for whether 5G frequencies are considered unmetered */
    private boolean mNrNsaAllUnmetered = false;
    private boolean mNrNsaMmwaveUnmetered = false;
    private boolean mNrNsaSub6Unmetered = false;
    private boolean mNrSaAllUnmetered = false;
    private boolean mNrSaMmwaveUnmetered = false;
    private boolean mNrSaSub6Unmetered = false;
    private boolean mNrNsaRoamingUnmetered = false;

    // it effect the PhysicalLinkStatusChanged
    private boolean mLteEndcUsingUserDataForRrcDetection = false;

    /* List of SubscriptionPlans, updated when initialized and when plans are changed. */
    private List<SubscriptionPlan> mSubscriptionPlans = new ArrayList<>();
    /* List of network types an unmetered override applies to, set by onSubscriptionOverride
     * and cleared when the device is rebooted or the override expires. */
    private List<Integer> mUnmeteredNetworkTypes = null;
    /* List of network types a congested override applies to, set by onSubscriptionOverride
     * and cleared when the device is rebooted or the override expires. */
    private List<Integer> mCongestedNetworkTypes = null;
    /* Whether an unmetered override is currently active. */
    private boolean mUnmeteredOverride = false;
    /* Whether a congested override is currently active. */
    private boolean mCongestedOverride = false;

    @SimState
    private int mSimState = TelephonyManager.SIM_STATE_UNKNOWN;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver () {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                // TODO: Evaluate hooking this up with DeviceStateMonitor
                if (DBG) log("screen on");
                mIsScreenOn = true;
                stopNetStatPoll();
                startNetStatPoll();
                restartDataStallAlarm();
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (DBG) log("screen off");
                mIsScreenOn = false;
                stopNetStatPoll();
                startNetStatPoll();
                restartDataStallAlarm();
            } else if (action.equals(INTENT_DATA_STALL_ALARM)) {
                onActionIntentDataStallAlarm(intent);
            } else if (action.equals(INTENT_PROVISIONING_APN_ALARM)) {
                if (DBG) log("Provisioning apn alarm");
                onActionIntentProvisioningApnAlarm(intent);
            } else if (action.equals(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED)
                    || action.equals(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED)) {
                if (mPhone.getPhoneId() == intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX)) {
                    int simState = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                            TelephonyManager.SIM_STATE_UNKNOWN);
                    sendMessage(obtainMessage(DctConstants.EVENT_SIM_STATE_UPDATED, simState, 0));
                }
            } else if (action.equals(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)) {
                if (mPhone.getPhoneId() == intent.getIntExtra(CarrierConfigManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX)) {
                    if (intent.getBooleanExtra(
                            CarrierConfigManager.EXTRA_REBROADCAST_ON_UNLOCK, false)) {
                        // Ignore the rebroadcast one to prevent multiple carrier config changed
                        // event during boot up.
                        return;
                    }
                    int subId = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
                            SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                    if (SubscriptionManager.isValidSubscriptionId(subId)) {
                        sendEmptyMessage(DctConstants.EVENT_CARRIER_CONFIG_CHANGED);
                    }
                }
            } else {
                if (DBG) log("onReceive: Unknown action=" + action);
            }
        }
    };

    private final Runnable mPollNetStat = new Runnable() {
        @Override
        public void run() {
            updateDataActivity();

            if (mIsScreenOn) {
                mNetStatPollPeriod = Settings.Global.getInt(mResolver,
                        Settings.Global.PDP_WATCHDOG_POLL_INTERVAL_MS, POLL_NETSTAT_MILLIS);
            } else {
                mNetStatPollPeriod = Settings.Global.getInt(mResolver,
                        Settings.Global.PDP_WATCHDOG_LONG_POLL_INTERVAL_MS,
                        POLL_NETSTAT_SCREEN_OFF_MILLIS);
            }

            if (mNetStatPollEnabled) {
                mDataConnectionTracker.postDelayed(this, mNetStatPollPeriod);
            }
        }
    };

    private class ThrottleStatusChangedCallback implements DataThrottler.Callback {
        @Override
        public void onThrottleStatusChanged(List<ThrottleStatus> throttleStatuses) {
            for (ThrottleStatus status : throttleStatuses) {
                if (status.getThrottleType() == ThrottleStatus.THROTTLE_TYPE_NONE) {
                    setupDataOnConnectableApn(mApnContextsByType.get(status.getApnType()),
                            Phone.REASON_DATA_UNTHROTTLED,
                            RetryFailures.ALWAYS);
                }
            }
        }
    }

    private NetworkPolicyManager mNetworkPolicyManager;
    private final NetworkPolicyManager.SubscriptionCallback mSubscriptionCallback =
            new NetworkPolicyManager.SubscriptionCallback() {
        @Override
        public void onSubscriptionOverride(int subId, int overrideMask, int overrideValue,
                int[] networkTypes) {
            if (mPhone == null || mPhone.getSubId() != subId) return;

            List<Integer> tempList = new ArrayList<>();
            for (int networkType : networkTypes) {
                tempList.add(networkType);
            }

            log("Subscription override: overrideMask=" + overrideMask
                    + ", overrideValue=" + overrideValue + ", networkTypes=" + tempList);

            if (overrideMask == SUBSCRIPTION_OVERRIDE_UNMETERED) {
                mUnmeteredNetworkTypes = tempList;
                mUnmeteredOverride = overrideValue != 0;
                reevaluateUnmeteredConnections();
            } else if (overrideMask == SUBSCRIPTION_OVERRIDE_CONGESTED) {
                mCongestedNetworkTypes = tempList;
                mCongestedOverride = overrideValue != 0;
                reevaluateCongestedConnections();
            }
        }

        @Override
        public void onSubscriptionPlansChanged(int subId, SubscriptionPlan[] plans) {
            if (mPhone == null || mPhone.getSubId() != subId) return;

            mSubscriptionPlans = Arrays.asList(plans);
            if (DBG) log("SubscriptionPlans changed: " + mSubscriptionPlans);
            reevaluateUnmeteredConnections();
        }
    };

    private final SettingsObserver mSettingsObserver;

    private void registerSettingsObserver() {
        mSettingsObserver.unobserve();
        String simSuffix = "";
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            simSuffix = Integer.toString(mPhone.getSubId());
        }

        mSettingsObserver.observe(
                Settings.Global.getUriFor(Settings.Global.DATA_ROAMING + simSuffix),
                DctConstants.EVENT_ROAMING_SETTING_CHANGE);
        mSettingsObserver.observe(
                Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONED),
                DctConstants.EVENT_DEVICE_PROVISIONED_CHANGE);
    }

    /**
     * Maintain the sum of transmit and receive packets.
     *
     * The packet counts are initialized and reset to -1 and
     * remain -1 until they can be updated.
     */
    public static class TxRxSum {
        public long txPkts;
        public long rxPkts;

        public TxRxSum() {
            reset();
        }

        public TxRxSum(long txPkts, long rxPkts) {
            this.txPkts = txPkts;
            this.rxPkts = rxPkts;
        }

        public TxRxSum(TxRxSum sum) {
            txPkts = sum.txPkts;
            rxPkts = sum.rxPkts;
        }

        public void reset() {
            txPkts = -1;
            rxPkts = -1;
        }

        @Override
        public String toString() {
            return "{txSum=" + txPkts + " rxSum=" + rxPkts + "}";
        }

        /**
         * Get total Tx/Rx packet count from TrafficStats
         */
        public void updateTotalTxRxSum() {
            this.txPkts = TrafficStats.getMobileTxPackets();
            this.rxPkts = TrafficStats.getMobileRxPackets();
        }
    }

    private void onDataReconnect(ApnContext apnContextforRetry, int subId,
            @RequestNetworkType int requestType) {
        int phoneSubId = mPhone.getSubId();
        String apnType = apnContextforRetry.getApnType();
        String reason =  apnContextforRetry.getReason();

        if (!SubscriptionManager.isValidSubscriptionId(subId) || (subId != phoneSubId)) {
            log("onDataReconnect: invalid subId");
            return;
        }

        ApnContext apnContext = mApnContexts.get(apnType);

        if (DBG) {
            log("onDataReconnect: mState=" + mState + " reason=" + reason + " apnType=" + apnType
                    + " apnContext=" + apnContext);
        }

        if ((apnContext != null) && (apnContext.isEnabled())) {
            apnContext.setReason(reason);
            DctConstants.State apnContextState = apnContext.getState();
            if (DBG) {
                log("onDataReconnect: apnContext state=" + apnContextState);
            }
            if ((apnContextState == DctConstants.State.FAILED)
                    || (apnContextState == DctConstants.State.IDLE)) {
                if (DBG) {
                    log("onDataReconnect: state is FAILED|IDLE, disassociate");
                }
                apnContext.releaseDataConnection("");
            } else {
                if (DBG) log("onDataReconnect: keep associated");
            }
            // TODO: IF already associated should we send the EVENT_TRY_SETUP_DATA???
            sendMessage(obtainMessage(DctConstants.EVENT_TRY_SETUP_DATA, requestType,
                    0, apnContext));
        }
    }

    private void onActionIntentDataStallAlarm(Intent intent) {
        if (VDBG_STALL) log("onActionIntentDataStallAlarm: action=" + intent.getAction());

        int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        if (!SubscriptionManager.isValidSubscriptionId(subId) || (subId != mPhone.getSubId())) {
            return;
        }

        int transportType = intent.getIntExtra(INTENT_DATA_STALL_ALARM_EXTRA_TRANSPORT_TYPE, 0);
        if (transportType != mTransportType) {
            return;
        }

        Message msg = obtainMessage(DctConstants.EVENT_DATA_STALL_ALARM,
                intent.getAction());
        msg.arg1 = intent.getIntExtra(INTENT_DATA_STALL_ALARM_EXTRA_TAG, 0);
        sendMessage(msg);
    }

    private RegistrantList mAllDataDisconnectedRegistrants = new RegistrantList();

    // member variables
    protected final Phone mPhone;
    private DctConstants.Activity mActivity = DctConstants.Activity.NONE;
    private DctConstants.State mState = DctConstants.State.IDLE;
    private final Handler mDataConnectionTracker;

    private long mTxPkts;
    private long mRxPkts;
    private int mNetStatPollPeriod;
    private boolean mNetStatPollEnabled = false;

    private TxRxSum mDataStallTxRxSum = new TxRxSum(0, 0);
    // Used to track stale data stall alarms.
    private int mDataStallAlarmTag = (int) SystemClock.elapsedRealtime();
    // The current data stall alarm intent
    private PendingIntent mDataStallAlarmIntent = null;
    // Number of packets sent since the last received packet
    private long mSentSinceLastRecv;
    // Controls when a simple recovery attempt it to be tried
    private int mNoRecvPollCount = 0;
    // Reference counter for enabling fail fast
    private static int sEnableFailFastRefCounter = 0;
    // True if data stall detection is enabled
    private volatile boolean mDataStallNoRxEnabled = true;

    protected volatile boolean mFailFast = false;

    // True when in voice call
    protected boolean mInVoiceCall = false;

    /** Intent sent when the reconnect alarm fires. */
    private PendingIntent mReconnectIntent = null;

    // When false we will not auto attach and manually attaching is required.
    protected boolean mAutoAttachOnCreationConfig = false;
    private AtomicBoolean mAutoAttachEnabled = new AtomicBoolean(false);

    // State of screen
    // (TODO: Reconsider tying directly to screen, maybe this is
    //        really a lower power mode")
    private boolean mIsScreenOn = true;

    /** Allows the generation of unique Id's for DataConnection objects */
    private AtomicInteger mUniqueIdGenerator = new AtomicInteger(0);

    /** The data connections. */
    private HashMap<Integer, DataConnection> mDataConnections =
            new HashMap<Integer, DataConnection>();

    /** Convert an ApnType string to Id (TODO: Use "enumeration" instead of String for ApnType) */
    private HashMap<String, Integer> mApnToDataConnectionId = new HashMap<String, Integer>();

    /** Phone.APN_TYPE_* ===> ApnContext */
    protected ConcurrentHashMap<String, ApnContext> mApnContexts =
            new ConcurrentHashMap<String, ApnContext>();

    private SparseArray<ApnContext> mApnContextsByType = new SparseArray<ApnContext>();

    private ArrayList<DataProfile> mLastDataProfileList = new ArrayList<>();

    /** RAT name ===> (downstream, upstream) bandwidth values from carrier config. */
    private ConcurrentHashMap<String, Pair<Integer, Integer>> mBandwidths =
            new ConcurrentHashMap<>();

    private boolean mConfigReady = false;

    /**
     * Handles changes to the APN db.
     */
    private class ApnChangeObserver extends ContentObserver {
        public ApnChangeObserver () {
            super(mDataConnectionTracker);
        }

        @Override
        public void onChange(boolean selfChange) {
            sendMessage(obtainMessage(DctConstants.EVENT_APN_CHANGED));
        }
    }

    //***** Instance Variables

    private boolean mReregisterOnReconnectFailure = false;


    //***** Constants

    private static final int PROVISIONING_SPINNER_TIMEOUT_MILLIS = 120 * 1000;

    static final Uri PREFERAPN_NO_UPDATE_URI_USING_SUBID =
                        Uri.parse("content://telephony/carriers/preferapn_no_update/subId/");
    static final String APN_ID = "apn_id";

    private boolean mCanSetPreferApn = false;

    private AtomicBoolean mAttached = new AtomicBoolean(false);

    /** Watches for changes to the APN db. */
    private ApnChangeObserver mApnObserver;

    private BroadcastReceiver mProvisionBroadcastReceiver;
    private ProgressDialog mProvisioningSpinner;

    private final DataServiceManager mDataServiceManager;

    @AccessNetworkConstants.TransportType
    private final int mTransportType;

    private DataStallRecoveryHandler mDsRecoveryHandler;
    private HandlerThread mHandlerThread;

    private final DataThrottler mDataThrottler;

    private final ThrottleStatusChangedCallback mThrottleStatusCallback;

    /**
     * Request network completion message map. Key is the APN type, value is the list of completion
     * messages to be sent. Using a list because there might be multiple network requests for
     * the same APN type.
     */
    private final Map<Integer, List<Message>> mHandoverCompletionMsgs = new HashMap<>();

    //***** Constructor
    public DcTracker(Phone phone, @TransportType int transportType) {
        super();
        mPhone = phone;
        if (DBG) log("DCT.constructor");
        mTelephonyManager = TelephonyManager.from(phone.getContext())
                .createForSubscriptionId(phone.getSubId());
        // The 'C' in tag indicates cellular, and 'I' indicates IWLAN. This is to distinguish
        // between two DcTrackers, one for each.
        String tagSuffix = "-" + ((transportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                ? "C" : "I");
        tagSuffix += "-" + mPhone.getPhoneId();
        mLogTag = "DCT" + tagSuffix;

        mTransportType = transportType;
        mDataServiceManager = new DataServiceManager(phone, transportType, tagSuffix);
        mDataThrottler = new DataThrottler(mPhone, transportType);

        mResolver = mPhone.getContext().getContentResolver();
        mAlarmManager =
                (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);

        mDsRecoveryHandler = new DataStallRecoveryHandler();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(INTENT_DATA_STALL_ALARM);
        filter.addAction(INTENT_PROVISIONING_APN_ALARM);
        filter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        filter.addAction(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);
        filter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);

        mDataEnabledSettings = mPhone.getDataEnabledSettings();

        mDataEnabledSettings.registerForDataEnabledChanged(this,
                DctConstants.EVENT_DATA_ENABLED_CHANGED, null);
        mDataEnabledSettings.registerForDataEnabledOverrideChanged(this,
                DctConstants.EVENT_DATA_ENABLED_OVERRIDE_RULES_CHANGED);

        mPhone.getContext().registerReceiver(mIntentReceiver, filter, null, mPhone);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mPhone.getContext());
        mAutoAttachEnabled.set(sp.getBoolean(Phone.DATA_DISABLED_ON_BOOT_KEY, false));

        mNetworkPolicyManager = (NetworkPolicyManager) mPhone.getContext()
                .getSystemService(Context.NETWORK_POLICY_SERVICE);
        mNetworkPolicyManager.registerSubscriptionCallback(mSubscriptionCallback);

        mHandlerThread = new HandlerThread("DcHandlerThread");
        mHandlerThread.start();
        Handler dcHandler = new Handler(mHandlerThread.getLooper());
        mDcc = DcController.makeDcc(mPhone, this, mDataServiceManager, dcHandler.getLooper(),
                tagSuffix);
        mDcTesterFailBringUpAll = new DcTesterFailBringUpAll(mPhone, dcHandler);

        mDataConnectionTracker = this;
        registerForAllEvents();
        mApnObserver = new ApnChangeObserver();
        phone.getContext().getContentResolver().registerContentObserver(
                Telephony.Carriers.CONTENT_URI, true, mApnObserver);

        initApnContexts();

        addDefaultApnSettingsAsNeeded();

        mSettingsObserver = new SettingsObserver(mPhone.getContext(), this);
        registerSettingsObserver();

        mThrottleStatusCallback = new ThrottleStatusChangedCallback();
        mDataThrottler.registerForThrottleStatusChanges(mThrottleStatusCallback);
    }

    @VisibleForTesting
    public DcTracker() {
        mLogTag = "DCT";
        mTelephonyManager = null;
        mAlarmManager = null;
        mPhone = null;
        mDataConnectionTracker = null;
        mSettingsObserver = new SettingsObserver(null, this);
        mDataEnabledSettings = null;
        mTransportType = 0;
        mDataServiceManager = null;
        mDataThrottler = null;
        mThrottleStatusCallback = null;
    }

    public void registerServiceStateTrackerEvents() {
        mPhone.getServiceStateTracker().registerForDataConnectionAttached(mTransportType, this,
                DctConstants.EVENT_DATA_CONNECTION_ATTACHED, null);
        mPhone.getServiceStateTracker().registerForDataConnectionDetached(mTransportType, this,
                DctConstants.EVENT_DATA_CONNECTION_DETACHED, null);
        mPhone.getServiceStateTracker().registerForDataRoamingOn(this,
                DctConstants.EVENT_ROAMING_ON, null);
        mPhone.getServiceStateTracker().registerForDataRoamingOff(this,
                DctConstants.EVENT_ROAMING_OFF, null, true);
        mPhone.getServiceStateTracker().registerForPsRestrictedEnabled(this,
                DctConstants.EVENT_PS_RESTRICT_ENABLED, null);
        mPhone.getServiceStateTracker().registerForPsRestrictedDisabled(this,
                DctConstants.EVENT_PS_RESTRICT_DISABLED, null);
        mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(mTransportType, this,
                DctConstants.EVENT_DATA_RAT_CHANGED, null);
    }

    public void unregisterServiceStateTrackerEvents() {
        mPhone.getServiceStateTracker().unregisterForDataConnectionAttached(mTransportType, this);
        mPhone.getServiceStateTracker().unregisterForDataConnectionDetached(mTransportType, this);
        mPhone.getServiceStateTracker().unregisterForDataRoamingOn(this);
        mPhone.getServiceStateTracker().unregisterForDataRoamingOff(this);
        mPhone.getServiceStateTracker().unregisterForPsRestrictedEnabled(this);
        mPhone.getServiceStateTracker().unregisterForPsRestrictedDisabled(this);
        mPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(mTransportType, this);
        mPhone.getServiceStateTracker().unregisterForAirplaneModeChanged(this);
    }

    private void registerForAllEvents() {
        if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
            mPhone.mCi.registerForAvailable(this, DctConstants.EVENT_RADIO_AVAILABLE, null);
            mPhone.mCi.registerForOffOrNotAvailable(this,
                    DctConstants.EVENT_RADIO_OFF_OR_NOT_AVAILABLE, null);
            mPhone.mCi.registerForPcoData(this, DctConstants.EVENT_PCO_DATA_RECEIVED, null);
        }

        // Note, this is fragile - the Phone is now presenting a merged picture
        // of PS (volte) & CS and by diving into its internals you're just seeing
        // the CS data.  This works well for the purposes this is currently used for
        // but that may not always be the case.  Should probably be redesigned to
        // accurately reflect what we're really interested in (registerForCSVoiceCallEnded).
        mPhone.getCallTracker().registerForVoiceCallEnded(this,
                DctConstants.EVENT_VOICE_CALL_ENDED, null);
        mPhone.getCallTracker().registerForVoiceCallStarted(this,
                DctConstants.EVENT_VOICE_CALL_STARTED, null);
        mPhone.getDisplayInfoController().registerForTelephonyDisplayInfoChanged(this,
                DctConstants.EVENT_TELEPHONY_DISPLAY_INFO_CHANGED, null);
        registerServiceStateTrackerEvents();
        mDataServiceManager.registerForServiceBindingChanged(this,
                DctConstants.EVENT_DATA_SERVICE_BINDING_CHANGED, null);
        mDataServiceManager.registerForApnUnthrottled(this, DctConstants.EVENT_APN_UNTHROTTLED);
    }

    public void dispose() {
        if (DBG) log("DCT.dispose");

        if (mProvisionBroadcastReceiver != null) {
            mPhone.getContext().unregisterReceiver(mProvisionBroadcastReceiver);
            mProvisionBroadcastReceiver = null;
        }
        if (mProvisioningSpinner != null) {
            mProvisioningSpinner.dismiss();
            mProvisioningSpinner = null;
        }

        cleanUpAllConnectionsInternal(true, null);

        mIsDisposed = true;
        mPhone.getContext().unregisterReceiver(mIntentReceiver);
        mSettingsObserver.unobserve();

        mNetworkPolicyManager.unregisterSubscriptionCallback(mSubscriptionCallback);
        mDcTesterFailBringUpAll.dispose();

        mPhone.getContext().getContentResolver().unregisterContentObserver(mApnObserver);
        mApnContexts.clear();
        mApnContextsByType.clear();
        mPrioritySortedApnContexts.clear();
        unregisterForAllEvents();

        destroyDataConnections();
    }

    /**
     * Stop the internal handler thread
     *
     * TESTING ONLY
     */
    @VisibleForTesting
    public void stopHandlerThread() {
        mHandlerThread.quit();
    }

    private void unregisterForAllEvents() {
         //Unregister for all events
        if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
            mPhone.mCi.unregisterForAvailable(this);
            mPhone.mCi.unregisterForOffOrNotAvailable(this);
            mPhone.mCi.unregisterForPcoData(this);
        }

        mPhone.getCallTracker().unregisterForVoiceCallEnded(this);
        mPhone.getCallTracker().unregisterForVoiceCallStarted(this);
        mPhone.getDisplayInfoController().unregisterForTelephonyDisplayInfoChanged(this);
        unregisterServiceStateTrackerEvents();
        mDataServiceManager.unregisterForServiceBindingChanged(this);
        mDataEnabledSettings.unregisterForDataEnabledChanged(this);
        mDataEnabledSettings.unregisterForDataEnabledOverrideChanged(this);
        mDataServiceManager.unregisterForApnUnthrottled(this);
    }

    /**
     * Reevaluate existing data connections when conditions change.
     *
     * For example, handle reverting restricted networks back to unrestricted. If we're changing
     * user data to enabled and this makes data truly enabled (not disabled by other factors) we
     * need to reevaluate and possibly add NET_CAPABILITY_NOT_RESTRICTED capability to the data
     * connection. This allows non-privilege apps to use the network.
     *
     * Or when we brought up a unmetered data connection while data is off, we only limit this
     * data connection for unmetered use only. When data is turned back on, we need to tear that
     * down so a full capable data connection can be re-established.
     */
    private void reevaluateDataConnections() {
        for (DataConnection dataConnection : mDataConnections.values()) {
            dataConnection.reevaluateRestrictedState();
        }
    }

    public long getSubId() {
        return mPhone.getSubId();
    }

    public DctConstants.Activity getActivity() {
        return mActivity;
    }

    private void setActivity(DctConstants.Activity activity) {
        log("setActivity = " + activity);
        mActivity = activity;
        mPhone.notifyDataActivity();
    }

    /**
     * Request a network
     *
     * @param networkRequest Network request from clients
     * @param type The request type
     * @param onHandoverCompleteMsg When request type is handover, this message will be sent when
     * handover is completed. For normal request, this should be null.
     */
    public void requestNetwork(NetworkRequest networkRequest, @RequestNetworkType int type,
            Message onHandoverCompleteMsg) {
        if (type != REQUEST_TYPE_HANDOVER && onHandoverCompleteMsg != null) {
            throw new RuntimeException("request network with normal type request type but passing "
                    + "handover complete message.");
        }
        final int apnType = ApnContext.getApnTypeFromNetworkRequest(networkRequest);
        final ApnContext apnContext = mApnContextsByType.get(apnType);
        if (apnContext != null) {
            apnContext.requestNetwork(networkRequest, type, onHandoverCompleteMsg);
        }
    }

    public void releaseNetwork(NetworkRequest networkRequest, @ReleaseNetworkType int type) {
        final int apnType = ApnContext.getApnTypeFromNetworkRequest(networkRequest);
        final ApnContext apnContext = mApnContextsByType.get(apnType);
        if (apnContext != null) {
            apnContext.releaseNetwork(networkRequest, type);
        }
    }

    // Turn telephony radio on or off.
    private void setRadio(boolean on) {
        final ITelephony phone = ITelephony.Stub.asInterface(
                TelephonyFrameworkInitializer
                        .getTelephonyServiceManager()
                        .getTelephonyServiceRegisterer()
                        .get());
        try {
            phone.setRadio(on);
        } catch (Exception e) {
            // Ignore.
        }
    }

    // Class to handle Intent dispatched with user selects the "Sign-in to network"
    // notification.
    private class ProvisionNotificationBroadcastReceiver extends BroadcastReceiver {
        private final String mNetworkOperator;
        // Mobile provisioning URL.  Valid while provisioning notification is up.
        // Set prior to notification being posted as URL contains ICCID which
        // disappears when radio is off (which is the case when notification is up).
        private final String mProvisionUrl;

        public ProvisionNotificationBroadcastReceiver(String provisionUrl, String networkOperator) {
            mNetworkOperator = networkOperator;
            mProvisionUrl = provisionUrl;
        }

        private void setEnableFailFastMobileData(int enabled) {
            sendMessage(obtainMessage(DctConstants.CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA, enabled, 0));
        }

        private void enableMobileProvisioning() {
            final Message msg = obtainMessage(DctConstants.CMD_ENABLE_MOBILE_PROVISIONING);
            Bundle bundle = new Bundle(1);
            bundle.putString(DctConstants.PROVISIONING_URL_KEY, mProvisionUrl);
            msg.setData(bundle);
            sendMessage(msg);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPhone.getPhoneId() != intent.getIntExtra(EXTRA_PROVISION_PHONE_ID,
                    SubscriptionManager.INVALID_PHONE_INDEX)) {
                return;
            }
            // Turning back on the radio can take time on the order of a minute, so show user a
            // spinner so they know something is going on.
            log("onReceive : ProvisionNotificationBroadcastReceiver");
            mProvisioningSpinner = new ProgressDialog(context);
            mProvisioningSpinner.setTitle(mNetworkOperator);
            mProvisioningSpinner.setMessage(
                    // TODO: Don't borrow "Connecting..." i18n string; give Telephony a version.
                    context.getText(com.android.internal.R.string.media_route_status_connecting));
            mProvisioningSpinner.setIndeterminate(true);
            mProvisioningSpinner.setCancelable(true);
            // Allow non-Activity Service Context to create a View.
            mProvisioningSpinner.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
            mProvisioningSpinner.show();
            // After timeout, hide spinner so user can at least use their device.
            // TODO: Indicate to user that it is taking an unusually long time to connect?
            sendMessageDelayed(obtainMessage(DctConstants.CMD_CLEAR_PROVISIONING_SPINNER,
                    mProvisioningSpinner), PROVISIONING_SPINNER_TIMEOUT_MILLIS);
            // This code is almost identical to the old
            // ConnectivityService.handleMobileProvisioningAction code.
            setRadio(true);
            setEnableFailFastMobileData(DctConstants.ENABLED);
            enableMobileProvisioning();
        }
    }

    @Override
    protected void finalize() {
        if(DBG && mPhone != null) log("finalize");
    }

    private void initApnContexts() {
        PersistableBundle carrierConfig;
        CarrierConfigManager configManager = (CarrierConfigManager) mPhone.getContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            carrierConfig = configManager.getConfigForSubId(mPhone.getSubId());
        } else {
            carrierConfig = null;
        }
        initApnContexts(carrierConfig);
    }

    //Blows away any existing apncontexts that may exist, only use in ctor.
    private void initApnContexts(PersistableBundle carrierConfig) {
        if (!mTelephonyManager.isDataCapable()) {
            log("initApnContexts: isDataCapable == false.  No Apn Contexts loaded");
            return;
        }

        log("initApnContexts: E");
        // Load device network attributes from resources
        final Collection<ApnConfigType> types =
                new ApnConfigTypeRepository(carrierConfig).getTypes();

        for (ApnConfigType apnConfigType : types) {
            ApnContext apnContext = new ApnContext(mPhone, apnConfigType.getType(), mLogTag, this,
                    apnConfigType.getPriority());
            int bitmask = ApnSetting.getApnTypesBitmaskFromString(apnContext.getApnType());
            mPrioritySortedApnContexts.add(apnContext);
            mApnContexts.put(apnContext.getApnType(), apnContext);
            mApnContextsByType.put(bitmask, apnContext);
            // Notify listeners that all data is disconnected when DCT is initialized.
            // Once connections are established, DC will then notify that data is connected.
            // This is to prevent the case where the phone process crashed but we don't notify
            // listeners that data was disconnected, so they may be stuck in a connected state.
            mPhone.notifyDataConnection(new PreciseDataConnectionState.Builder()
                    .setTransportType(mTransportType)
                    .setState(TelephonyManager.DATA_DISCONNECTED)
                    .setApnSetting(new ApnSetting.Builder()
                            .setApnTypeBitmask(bitmask).buildWithoutCheck())
                    .setNetworkType(getDataRat())
                    .build());
            log("initApnContexts: apnContext=" + ApnSetting.getApnTypeString(
                    apnConfigType.getType()));
        }
        mPrioritySortedApnContexts.sort((c1, c2) -> c2.getPriority() - c1.getPriority());
        logSortedApnContexts();
    }

    private void sortApnContextByPriority() {
        if (!mTelephonyManager.isDataCapable()) {
            log("sortApnContextByPriority: isDataCapable == false.  No Apn Contexts loaded");
            return;
        }

        PersistableBundle carrierConfig;
        CarrierConfigManager configManager = (CarrierConfigManager) mPhone.getContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            carrierConfig = configManager.getConfigForSubId(mPhone.getSubId());
        } else {
            carrierConfig = null;
        }

        log("sortApnContextByPriority: E");
        // Load device network attributes from resources
        final Collection<ApnConfigType> types =
                new ApnConfigTypeRepository(carrierConfig).getTypes();
        for (ApnConfigType apnConfigType : types) {
            if (mApnContextsByType.contains(apnConfigType.getType())) {
                ApnContext apnContext = mApnContextsByType.get(apnConfigType.getType());
                apnContext.setPriority(apnConfigType.getPriority());
            }
        }

        //Doing sorted in a different list to keep thread safety
        ArrayList<ApnContext> prioritySortedApnContexts =
                new ArrayList<>(mPrioritySortedApnContexts);
        prioritySortedApnContexts.sort((c1, c2) -> c2.getPriority() - c1.getPriority());
        mPrioritySortedApnContexts = prioritySortedApnContexts;
        logSortedApnContexts();
    }

    public LinkProperties getLinkProperties(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext != null) {
            DataConnection dataConnection = apnContext.getDataConnection();
            if (dataConnection != null) {
                if (DBG) log("return link properties for " + apnType);
                return dataConnection.getLinkProperties();
            }
        }
        if (DBG) log("return new LinkProperties");
        return new LinkProperties();
    }

    public NetworkCapabilities getNetworkCapabilities(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext!=null) {
            DataConnection dataConnection = apnContext.getDataConnection();
            if (dataConnection != null) {
                if (DBG) {
                    log("get active pdp is not null, return NetworkCapabilities for " + apnType);
                }
                return dataConnection.getNetworkCapabilities();
            }
        }
        if (DBG) log("return new NetworkCapabilities");
        return new NetworkCapabilities();
    }

    // Return all active apn types
    public String[] getActiveApnTypes() {
        if (DBG) log("get all active apn types");
        ArrayList<String> result = new ArrayList<String>();

        for (ApnContext apnContext : mApnContexts.values()) {
            if (mAttached.get() && apnContext.isReady()) {
                result.add(apnContext.getApnType());
            }
        }

        return result.toArray(new String[0]);
    }

    /**
     * Get ApnTypes with connected data connections.  This is different than getActiveApnTypes()
     * which returns apn types that with active apn contexts.
     * @return apn types
     */
    public String[] getConnectedApnTypes() {
        return mApnContexts.values().stream()
                .filter(ac -> ac.getState() == DctConstants.State.CONNECTED)
                .map(ApnContext::getApnType)
                .toArray(String[]::new);
    }

    @VisibleForTesting
    public Collection<ApnContext> getApnContexts() {
        return mPrioritySortedApnContexts;
    }

    /** Return active ApnSetting of a specific apnType */
    public ApnSetting getActiveApnSetting(String apnType) {
        if (VDBG) log("get active ApnSetting for type:" + apnType);
        ApnContext apnContext = mApnContexts.get(apnType);
        return (apnContext != null) ? apnContext.getApnSetting() : null;
    }

    // Return active apn of specific apn type
    public String getActiveApnString(String apnType) {
        if (VDBG) log( "get active apn string for type:" + apnType);
        ApnSetting setting = getActiveApnSetting(apnType);
        return (setting != null) ? setting.getApnName() : null;
    }

    /**
     * Returns {@link DctConstants.State} based on the state of the {@link DataConnection} that
     * contains a {@link ApnSetting} that supported the given apn type {@code anpType}.
     *
     * <p>
     * Assumes there is less than one {@link ApnSetting} can support the given apn type.
     */
    // TODO: for enterprise this always returns IDLE, which is ok for now since it is never called
    // for enterprise
    public DctConstants.State getState(String apnType) {
        DctConstants.State state = DctConstants.State.IDLE;
        final int apnTypeBitmask = ApnSetting.getApnTypesBitmaskFromString(apnType);
        for (DataConnection dc : mDataConnections.values()) {
            ApnSetting apnSetting = dc.getApnSetting();
            if (apnSetting != null && apnSetting.canHandleType(apnTypeBitmask)) {
                if (dc.isActive()) {
                    state = getBetterConnectionState(state, DctConstants.State.CONNECTED);
                } else if (dc.isActivating()) {
                    state = getBetterConnectionState(state, DctConstants.State.CONNECTING);
                } else if (dc.isInactive()) {
                    state = getBetterConnectionState(state, DctConstants.State.IDLE);
                } else if (dc.isDisconnecting()) {
                    state = getBetterConnectionState(state, DctConstants.State.DISCONNECTING);
                }
            }
        }
        return state;
    }

    /**
     * Return a better connection state between {@code stateA} and {@code stateB}. Check
     * {@link #DATA_CONNECTION_STATE_PRIORITIES} for the details.
     * @return the better connection state between {@code stateA} and {@code stateB}.
     */
    private static DctConstants.State getBetterConnectionState(
            DctConstants.State stateA, DctConstants.State stateB) {
        int idxA = ArrayUtils.indexOf(DATA_CONNECTION_STATE_PRIORITIES, stateA);
        int idxB = ArrayUtils.indexOf(DATA_CONNECTION_STATE_PRIORITIES, stateB);
        return idxA >= idxB ? stateA : stateB;
    }

    // Return if apn type is a provisioning apn.
    private boolean isProvisioningApn(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext != null) {
            return apnContext.isProvisioningApn();
        }
        return false;
    }

    //****** Called from ServiceStateTracker
    /**
     * Invoked when ServiceStateTracker observes a transition from GPRS
     * attach to detach.
     */
    private void onDataConnectionDetached() {
        /*
         * We presently believe it is unnecessary to tear down the PDP context
         * when GPRS detaches, but we should stop the network polling.
         */
        if (DBG) log ("onDataConnectionDetached: stop polling and notify detached");
        stopNetStatPoll();
        stopDataStallAlarm();
        mAttached.set(false);
    }

    private void onDataConnectionAttached() {
        if (DBG) log("onDataConnectionAttached");
        mAttached.set(true);
        if (isAnyDataConnected()) {
            if (DBG) log("onDataConnectionAttached: start polling notify attached");
            startNetStatPoll();
            startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
        }
        if (mAutoAttachOnCreationConfig) {
            mAutoAttachEnabled.set(true);
        }
        setupDataOnAllConnectableApns(Phone.REASON_DATA_ATTACHED, RetryFailures.ALWAYS);
    }

    /**
     * Check if it is allowed to make a data connection (without checking APN context specific
     * conditions).
     *
     * @param dataConnectionReasons Data connection allowed or disallowed reasons as the output
     *                              param. It's okay to pass null here and no reasons will be
     *                              provided.
     * @return True if data connection is allowed, otherwise false.
     */
    public boolean isDataAllowed(DataConnectionReasons dataConnectionReasons) {
        return isDataAllowed(null, REQUEST_TYPE_NORMAL, dataConnectionReasons);
    }

    /**
     * Check if it is allowed to make a data connection for a given APN type.
     *
     * @param apnContext APN context. If passing null, then will only check general but not APN
     *                   specific conditions (e.g. APN state, metered/unmetered APN).
     * @param requestType Setup data request type.
     * @param dataConnectionReasons Data connection allowed or disallowed reasons as the output
     *                              param. It's okay to pass null here and no reasons will be
     *                              provided.
     * @return True if data connection is allowed, otherwise false.
     */
    public boolean isDataAllowed(ApnContext apnContext, @RequestNetworkType int requestType,
                                 DataConnectionReasons dataConnectionReasons) {
        // Step 1: Get all environment conditions.
        // Step 2: Special handling for emergency APN.
        // Step 3. Build disallowed reasons.
        // Step 4: Determine if data should be allowed in some special conditions.

        DataConnectionReasons reasons = new DataConnectionReasons();

        int requestApnType = 0;
        if (apnContext != null) {
            requestApnType = apnContext.getApnTypeBitmask();
        }

        // Step 1: Get all environment conditions.
        final boolean internalDataEnabled = mDataEnabledSettings.isInternalDataEnabled();
        boolean attachedState = mAttached.get();
        boolean desiredPowerState = mPhone.getServiceStateTracker().getDesiredPowerState();
        boolean radioStateFromCarrier = mPhone.getServiceStateTracker().getPowerStateFromCarrier();
        // TODO: Remove this hack added by ag/641832.
        int dataRat = getDataRat();
        if (dataRat == ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN) {
            desiredPowerState = true;
            radioStateFromCarrier = true;
        }

        boolean defaultDataSelected = SubscriptionManager.isValidSubscriptionId(
                SubscriptionManager.getDefaultDataSubscriptionId());

        boolean isMeteredApnType = apnContext == null
                || ApnSettingUtils.isMeteredApnType(requestApnType, mPhone);

        PhoneConstants.State phoneState = PhoneConstants.State.IDLE;
        // Note this is explicitly not using mPhone.getState.  See b/19090488.
        // mPhone.getState reports the merge of CS and PS (volte) voice call state
        // but we only care about CS calls here for data/voice concurrency issues.
        // Calling getCallTracker currently gives you just the CS side where the
        // ImsCallTracker is held internally where applicable.
        // This should be redesigned to ask explicitly what we want:
        // voiceCallStateAllowDataCall, or dataCallAllowed or something similar.
        if (mPhone.getCallTracker() != null) {
            phoneState = mPhone.getCallTracker().getState();
        }

        // Step 2: Special handling for emergency APN.
        if (apnContext != null
                && requestApnType == ApnSetting.TYPE_EMERGENCY
                && apnContext.isConnectable()) {
            // If this is an emergency APN, as long as the APN is connectable, we
            // should allow it.
            if (dataConnectionReasons != null) {
                dataConnectionReasons.add(DataAllowedReasonType.EMERGENCY_APN);
            }
            // Bail out without further checks.
            return true;
        }

        // Step 3. Build disallowed reasons.
        if (apnContext != null && !apnContext.isConnectable()) {
            DctConstants.State state = apnContext.getState();
            if (state == DctConstants.State.CONNECTED) {
                reasons.add(DataDisallowedReasonType.DATA_ALREADY_CONNECTED);
            } else if (state == DctConstants.State.DISCONNECTING) {
                reasons.add(DataDisallowedReasonType.DATA_IS_DISCONNECTING);
            } else if (state == DctConstants.State.CONNECTING) {
                reasons.add(DataDisallowedReasonType.DATA_IS_CONNECTING);
            } else {
                reasons.add(DataDisallowedReasonType.APN_NOT_CONNECTABLE);
            }
        }

        // In legacy mode, if RAT is IWLAN then don't allow default/IA PDP at all.
        // Rest of APN types can be evaluated for remaining conditions.
        if ((apnContext != null && requestApnType == ApnSetting.TYPE_DEFAULT
                || requestApnType == ApnSetting.TYPE_ENTERPRISE
                || requestApnType == ApnSetting.TYPE_IA)
                && mPhone.getAccessNetworksManager().isInLegacyMode()
                && dataRat == ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN) {
            reasons.add(DataDisallowedReasonType.ON_IWLAN);
        }

        // If device is not on NR, don't allow enterprise
        if (apnContext != null && requestApnType == ApnSetting.TYPE_ENTERPRISE
                && dataRat != ServiceState.RIL_RADIO_TECHNOLOGY_NR) {
            reasons.add(DataDisallowedReasonType.NOT_ON_NR);
        }

        if (shouldRestrictDataForEcbm() || mPhone.isInEmergencyCall()) {
            reasons.add(DataDisallowedReasonType.IN_ECBM);
        }

        if (!attachedState && !shouldAutoAttach() && requestType != REQUEST_TYPE_HANDOVER) {
            reasons.add(DataDisallowedReasonType.NOT_ATTACHED);
        }
        if (mPhone.getSubId() == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            reasons.add(DataDisallowedReasonType.SIM_NOT_READY);
        }
        if (phoneState != PhoneConstants.State.IDLE
                && !mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
            reasons.add(DataDisallowedReasonType.INVALID_PHONE_STATE);
            reasons.add(DataDisallowedReasonType.CONCURRENT_VOICE_DATA_NOT_ALLOWED);
        }
        if (!internalDataEnabled) {
            reasons.add(DataDisallowedReasonType.INTERNAL_DATA_DISABLED);
        }
        if (!defaultDataSelected) {
            reasons.add(DataDisallowedReasonType.DEFAULT_DATA_UNSELECTED);
        }
        if (mPhone.getServiceState().getDataRoaming() && !getDataRoamingEnabled()) {
            reasons.add(DataDisallowedReasonType.ROAMING_DISABLED);
        }
        if (mIsPsRestricted) {
            reasons.add(DataDisallowedReasonType.PS_RESTRICTED);
        }
        if (!desiredPowerState) {
            reasons.add(DataDisallowedReasonType.UNDESIRED_POWER_STATE);
        }
        if (!radioStateFromCarrier) {
            reasons.add(DataDisallowedReasonType.RADIO_DISABLED_BY_CARRIER);
        }
        if (!mDataServiceBound) {
            reasons.add(DataDisallowedReasonType.DATA_SERVICE_NOT_READY);
        }

        if (apnContext != null) {
            if (mPhone.getAccessNetworksManager().getPreferredTransport(
                    apnContext.getApnTypeBitmask())
                    == AccessNetworkConstants.TRANSPORT_TYPE_INVALID) {
                // If QNS explicitly specified this APN type is not allowed on either cellular or
                // IWLAN, we should not allow data setup.
                reasons.add(DataDisallowedReasonType.DISABLED_BY_QNS);
            } else if (mTransportType != mPhone.getAccessNetworksManager().getPreferredTransport(
                    apnContext.getApnTypeBitmask())) {
                // If the latest preference has already switched to other transport, we should not
                // allow data setup.
                reasons.add(DataDisallowedReasonType.ON_OTHER_TRANSPORT);
            }

            // If the transport has been already switched to the other transport, we should not
            // allow the data setup. The only exception is the handover case, where we setup
            // handover data connection before switching the transport.
            if (mTransportType != mPhone.getAccessNetworksManager().getCurrentTransport(
                    apnContext.getApnTypeBitmask()) && requestType != REQUEST_TYPE_HANDOVER) {
                reasons.add(DataDisallowedReasonType.ON_OTHER_TRANSPORT);
            }

            // Check if the device is under data throttling.
            long retryTime = mDataThrottler.getRetryTime(apnContext.getApnTypeBitmask());
            if (retryTime > SystemClock.elapsedRealtime()) {
                reasons.add(DataDisallowedReasonType.DATA_THROTTLED);
            }
        }

        boolean isDataEnabled = apnContext == null ? mDataEnabledSettings.isDataEnabled()
                : mDataEnabledSettings.isDataEnabled(requestApnType);

        if (!isDataEnabled) {
            reasons.add(DataDisallowedReasonType.DATA_DISABLED);
        }

        // If there are hard disallowed reasons, we should not allow data connection no matter what.
        if (reasons.containsHardDisallowedReasons()) {
            if (dataConnectionReasons != null) {
                dataConnectionReasons.copyFrom(reasons);
            }
            return false;
        }

        // Step 4: Determine if data should be allowed in some special conditions.

        // At this point, if data is not allowed, it must be because of the soft reasons. We
        // should start to check some special conditions that data will be allowed.
        if (!reasons.allowed()) {
            // Check if the transport is WLAN ie wifi (for AP-assisted mode devices)
            if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                reasons.add(DataAllowedReasonType.UNMETERED_APN);
            // Or if the data is on cellular, and the APN type is determined unmetered by the
            // configuration.
            } else if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                    && !isMeteredApnType && requestApnType != ApnSetting.TYPE_DEFAULT
                    && requestApnType != ApnSetting.TYPE_ENTERPRISE) {
                reasons.add(DataAllowedReasonType.UNMETERED_APN);
            }

            // If the request is restricted and there are only soft disallowed reasons (e.g. data
            // disabled, data roaming disabled) existing, we should allow the data. ENTERPRISE is
            // an exception and should not be treated as restricted for this purpose; it should be
            // treated same as DEFAULT.
            if (apnContext != null
                    && apnContext.hasRestrictedRequests(true)
                    && !apnContext.getApnType().equals(ApnSetting.TYPE_ENTERPRISE_STRING)
                    && !reasons.allowed()) {
                reasons.add(DataAllowedReasonType.RESTRICTED_REQUEST);
            }
        } else {
            // If there is no disallowed reasons, then we should allow the data request with
            // normal reason.
            reasons.add(DataAllowedReasonType.NORMAL);
        }

        if (dataConnectionReasons != null) {
            dataConnectionReasons.copyFrom(reasons);
        }

        return reasons.allowed();
    }

    // arg for setupDataOnAllConnectableApns
    protected enum RetryFailures {
        // retry failed networks always (the old default)
        ALWAYS,
        // retry only when a substantial change has occurred.  Either:
        // 1) we were restricted by voice/data concurrency and aren't anymore
        // 2) our apn list has change
        ONLY_ON_CHANGE
    };

    protected void setupDataOnAllConnectableApns(String reason, RetryFailures retryFailures) {
        if (VDBG) log("setupDataOnAllConnectableApns: " + reason);

        if (DBG && !VDBG) {
            StringBuilder sb = new StringBuilder(120);
            for (ApnContext apnContext : mPrioritySortedApnContexts) {
                sb.append(apnContext.getApnType());
                sb.append(":[state=");
                sb.append(apnContext.getState());
                sb.append(",enabled=");
                sb.append(apnContext.isEnabled());
                sb.append("] ");
            }
            log("setupDataOnAllConnectableApns: " + reason + " " + sb);
        }

        for (ApnContext apnContext : mPrioritySortedApnContexts) {
            setupDataOnConnectableApn(apnContext, reason, retryFailures);
        }
    }

    protected void setupDataOnConnectableApn(ApnContext apnContext, String reason,
            RetryFailures retryFailures) {
        if (VDBG) log("setupDataOnAllConnectableApns: apnContext " + apnContext);

        if (apnContext.getState() == DctConstants.State.FAILED
                || apnContext.getState() == DctConstants.State.RETRYING) {
            if (retryFailures == RetryFailures.ALWAYS) {
                apnContext.releaseDataConnection(reason);
            } else if (!apnContext.isConcurrentVoiceAndDataAllowed()
                    && mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                // RetryFailures.ONLY_ON_CHANGE - check if voice concurrency has changed
                apnContext.releaseDataConnection(reason);
            }
        }
        if (apnContext.isConnectable()) {
            log("isConnectable() call trySetupData");
            apnContext.setReason(reason);
            trySetupData(apnContext, REQUEST_TYPE_NORMAL, null);
        }
    }

    private boolean shouldRestrictDataForEcbm() {
        boolean isInEcm = mPhone.isInEcm();
        boolean isInImsEcm = mPhone.getImsPhone() != null && mPhone.getImsPhone().isInImsEcm();
        log("shouldRestrictDataForEcbm: isInEcm=" + isInEcm + " isInImsEcm=" + isInImsEcm);
        return isInEcm && !isInImsEcm;
    }

    private boolean isHandoverPending(@ApnType int apnType) {
        List<Message> messageList = mHandoverCompletionMsgs.get(apnType);
        return messageList != null && messageList.size() > 0;
    }

    private void trySetupData(ApnContext apnContext, @RequestNetworkType int requestType,
            @Nullable Message onHandoverCompleteMsg) {
        if (onHandoverCompleteMsg != null) {
            addHandoverCompleteMsg(onHandoverCompleteMsg, apnContext.getApnTypeBitmask());
        }

        if (mPhone.getSimulatedRadioControl() != null) {
            // Assume data is connected on the simulator
            log("trySetupData: X We're on the simulator; assuming connected retValue=true");
            return;
        }

        DataConnectionReasons dataConnectionReasons = new DataConnectionReasons();
        boolean isDataAllowed = isDataAllowed(apnContext, requestType, dataConnectionReasons);
        String logStr = "trySetupData for APN type " + apnContext.getApnType() + ", reason: "
                + apnContext.getReason() + ", requestType=" + requestTypeToString(requestType)
                + ". " + dataConnectionReasons.toString();
        if (dataConnectionReasons.contains(DataDisallowedReasonType.DISABLED_BY_QNS)
                || dataConnectionReasons.contains(DataDisallowedReasonType.ON_OTHER_TRANSPORT)) {
            logStr += ", current transport=" + AccessNetworkConstants.transportTypeToString(
                    mPhone.getAccessNetworksManager().getCurrentTransport(
                            apnContext.getApnTypeBitmask()));
            logStr += ", preferred transport=" + AccessNetworkConstants.transportTypeToString(
                    mPhone.getAccessNetworksManager().getPreferredTransport(
                            apnContext.getApnTypeBitmask()));
        }
        if (DBG) log(logStr);
        ApnContext.requestLog(apnContext, logStr);
        if (!isDataAllowed) {
            StringBuilder str = new StringBuilder();

            str.append("trySetupData failed. apnContext = [type=" + apnContext.getApnType()
                    + ", mState=" + apnContext.getState() + ", apnEnabled="
                    + apnContext.isEnabled() + ", mDependencyMet="
                    + apnContext.isDependencyMet() + "] ");

            if (!mDataEnabledSettings.isDataEnabled()) {
                str.append("isDataEnabled() = false. " + mDataEnabledSettings);
            }

            // Check if it fails because of the existing data is still disconnecting.
            if (dataConnectionReasons.contains(DataDisallowedReasonType.DATA_IS_DISCONNECTING)
                    && isHandoverPending(apnContext.getApnTypeBitmask())) {
                // Normally we don't retry when isDataAllow() returns false, because that's consider
                // pre-condition not met, for example, data not enabled by the user, or airplane
                // mode is on. If we retry in those cases, there will be significant power impact.
                // DATA_IS_DISCONNECTING is a special case we want to retry, and for the handover
                // case only.
                log("Data is disconnecting. Will retry handover later.");
                return;
            }

            // If this is a data retry, we should set the APN state to FAILED so it won't stay
            // in RETRYING forever.
            if (apnContext.getState() == DctConstants.State.RETRYING) {
                apnContext.setState(DctConstants.State.FAILED);
                str.append(" Stop retrying.");
            }

            if (DBG) log(str.toString());
            ApnContext.requestLog(apnContext, str.toString());
            if (requestType == REQUEST_TYPE_HANDOVER) {
                // If fails due to latest preference already changed back to source transport, then
                // just fallback (will not attempt handover anymore, and will not tear down the
                // data connection on source transport.
                boolean fallback = dataConnectionReasons.contains(
                        DataDisallowedReasonType.ON_OTHER_TRANSPORT);
                sendHandoverCompleteMessages(apnContext.getApnTypeBitmask(), false, fallback);
            }
            return;
        }

        if (apnContext.getState() == DctConstants.State.FAILED) {
            String str = "trySetupData: make a FAILED ApnContext IDLE so its reusable";
            if (DBG) log(str);
            ApnContext.requestLog(apnContext, str);
            apnContext.setState(DctConstants.State.IDLE);
        }
        int radioTech = getDataRat();
        if (radioTech == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN && mPhone.getServiceState()
                .getState() == ServiceState.STATE_IN_SERVICE) {
            radioTech = getVoiceRat();
        }
        log("service state=" + mPhone.getServiceState());
        apnContext.setConcurrentVoiceAndDataAllowed(mPhone.getServiceStateTracker()
                .isConcurrentVoiceAndDataAllowed());
        if (apnContext.getState() == DctConstants.State.IDLE) {
            ArrayList<ApnSetting> waitingApns =
                    buildWaitingApns(apnContext.getApnType(), radioTech);
            if (waitingApns.isEmpty()) {
                String str = "trySetupData: X No APN found retValue=false";
                if (DBG) log(str);
                ApnContext.requestLog(apnContext, str);
                if (requestType == REQUEST_TYPE_HANDOVER) {
                    sendHandoverCompleteMessages(apnContext.getApnTypeBitmask(), false,
                            false);
                }
                return;
            } else {
                apnContext.setWaitingApns(waitingApns);
                if (DBG) {
                    log("trySetupData: Create from mAllApnSettings : "
                                + apnListToString(mAllApnSettings));
                }
            }
        }

        if (!setupData(apnContext, radioTech, requestType)
                && requestType == REQUEST_TYPE_HANDOVER) {
            sendHandoverCompleteMessages(apnContext.getApnTypeBitmask(), false, false);
        }
    }

    /**
     * Clean up all data connections. Note this is just detach the APN context from the data
     * connection. After all APN contexts are detached from the data connection, the data
     * connection will be torn down.
     *
     * @param reason Reason for the clean up.
     */
    public void cleanUpAllConnections(String reason) {
        log("cleanUpAllConnections");
        Message msg = obtainMessage(DctConstants.EVENT_CLEAN_UP_ALL_CONNECTIONS);
        msg.obj = reason;
        sendMessage(msg);
    }

    /**
     * Clean up all data connections by detaching the APN contexts from the data connections, which
     * eventually tearing down all data connections after all APN contexts are detached from the
     * data connections.
     *
     * @param detach {@code true} if detaching APN context from the underlying data connection (when
     * no other APN context is attached to the data connection, the data connection will be torn
     * down.) {@code false} to only reset the data connection's state machine.
     *
     * @param reason reason for the clean up.
     * @return boolean - true if we did cleanup any connections, false if they
     *                   were already all disconnected.
     */
    private boolean cleanUpAllConnectionsInternal(boolean detach, String reason) {
        if (DBG) log("cleanUpAllConnectionsInternal: detach=" + detach + " reason=" + reason);
        boolean didDisconnect = false;
        boolean disableMeteredOnly = false;

        // reasons that only metered apn will be torn down
        if (!TextUtils.isEmpty(reason)) {
            disableMeteredOnly = reason.equals(Phone.REASON_DATA_SPECIFIC_DISABLED) ||
                    reason.equals(Phone.REASON_ROAMING_ON) ||
                    reason.equals(Phone.REASON_CARRIER_ACTION_DISABLE_METERED_APN);
        }

        for (ApnContext apnContext : mApnContexts.values()) {
            // Exclude the IMS APN from single data connection case.
            if (reason.equals(Phone.REASON_SINGLE_PDN_ARBITRATION)
                    && apnContext.getApnType().equals(ApnSetting.TYPE_IMS_STRING)) {
                continue;
            }

            if (shouldCleanUpConnection(apnContext, disableMeteredOnly,
                    reason.equals(Phone.REASON_SINGLE_PDN_ARBITRATION))) {
                // TODO - only do cleanup if not disconnected
                if (apnContext.isDisconnected() == false) didDisconnect = true;
                apnContext.setReason(reason);
                cleanUpConnectionInternal(detach, RELEASE_TYPE_DETACH, apnContext);
            } else if (DBG) {
                log("cleanUpAllConnectionsInternal: APN type " + apnContext.getApnType()
                        + " shouldn't be cleaned up.");
            }
        }

        stopNetStatPoll();
        stopDataStallAlarm();

        // TODO: Do we need mRequestedApnType?
        mRequestedApnType = ApnSetting.TYPE_DEFAULT;

        if (areAllDataDisconnected()) {
            notifyAllDataDisconnected();
        }

        return didDisconnect;
    }

    boolean shouldCleanUpConnection(ApnContext apnContext, boolean disableMeteredOnly,
            boolean singlePdn) {
        if (apnContext == null) return false;

        // If APN setting is not null and the reason is single PDN arbitration, clean up connection.
        ApnSetting apnSetting = apnContext.getApnSetting();
        if (apnSetting != null && singlePdn) return true;

        // If meteredOnly is false, clean up all connections.
        if (!disableMeteredOnly) return true;

        // If meteredOnly is true, and apnSetting is null or it's un-metered, no need to clean up.
        if (apnSetting == null || !ApnSettingUtils.isMetered(apnSetting, mPhone)) return false;

        boolean isRoaming = mPhone.getServiceState().getDataRoaming();
        boolean isDataRoamingDisabled = !getDataRoamingEnabled();
        boolean isDataDisabled = !mDataEnabledSettings.isDataEnabled(
                apnSetting.getApnTypeBitmask());

        // Should clean up if its data is disabled, or data roaming is disabled while roaming.
        return isDataDisabled || (isRoaming && isDataRoamingDisabled);
    }

    /**
     * Detach the APN context from the associated data connection. This data connection might be
     * torn down if no other APN context is attached to it.
     *
     * @param apnContext The APN context to be detached
     */
    void cleanUpConnection(ApnContext apnContext) {
        if (DBG) log("cleanUpConnection: apnContext=" + apnContext);
        Message msg = obtainMessage(DctConstants.EVENT_CLEAN_UP_CONNECTION);
        msg.arg2 = 0;
        msg.obj = apnContext;
        sendMessage(msg);
    }

    /**
     * Detach the APN context from the associated data connection. This data connection will be
     * torn down if no other APN context is attached to it.
     *
     * @param detach {@code true} if detaching APN context from the underlying data connection (when
     * no other APN context is attached to the data connection, the data connection will be torn
     * down.) {@code false} to only reset the data connection's state machine.
     * @param releaseType Data release type.
     * @param apnContext The APN context to be detached.
     */
    private void cleanUpConnectionInternal(boolean detach, @ReleaseNetworkType int releaseType,
                                           ApnContext apnContext) {
        if (apnContext == null) {
            if (DBG) log("cleanUpConnectionInternal: apn context is null");
            return;
        }

        DataConnection dataConnection = apnContext.getDataConnection();
        String str = "cleanUpConnectionInternal: detach=" + detach + " reason="
                + apnContext.getReason();
        if (VDBG) log(str + " apnContext=" + apnContext);
        ApnContext.requestLog(apnContext, str);
        if (detach) {
            if (apnContext.isDisconnected()) {
                // The request is detach and but ApnContext is not connected.
                // If apnContext is not enabled anymore, break the linkage to the data connection.
                apnContext.releaseDataConnection("");
            } else {
                // Connection is still there. Try to clean up.
                if (dataConnection != null) {
                    if (apnContext.getState() != DctConstants.State.DISCONNECTING) {
                        boolean disconnectAll = false;
                        if (ApnSetting.TYPE_DUN_STRING.equals(apnContext.getApnType())
                                && ServiceState.isCdma(getDataRat())) {
                            if (DBG) {
                                log("cleanUpConnectionInternal: disconnectAll DUN connection");
                            }
                            // For CDMA DUN, we need to tear it down immediately. A new data
                            // connection will be reestablished with correct profile id.
                            disconnectAll = true;
                        }
                        final int generation = apnContext.getConnectionGeneration();
                        str = "cleanUpConnectionInternal: tearing down"
                                + (disconnectAll ? " all" : "") + " using gen#" + generation;
                        if (DBG) log(str + "apnContext=" + apnContext);
                        ApnContext.requestLog(apnContext, str);
                        Pair<ApnContext, Integer> pair = new Pair<>(apnContext, generation);
                        Message msg = obtainMessage(DctConstants.EVENT_DISCONNECT_DONE, pair);

                        if (disconnectAll || releaseType == RELEASE_TYPE_HANDOVER) {
                            dataConnection.tearDownAll(apnContext.getReason(), releaseType, msg);
                        } else {
                            dataConnection.tearDown(apnContext, apnContext.getReason(), msg);
                        }

                        apnContext.setState(DctConstants.State.DISCONNECTING);
                    }
                } else {
                    // apn is connected but no reference to the data connection.
                    // Should not be happen, but reset the state in case.
                    apnContext.setState(DctConstants.State.IDLE);
                    ApnContext.requestLog(
                            apnContext, "cleanUpConnectionInternal: connected, bug no dc");
                }
            }
        } else {
            // force clean up the data connection.
            if (dataConnection != null) dataConnection.reset();
            apnContext.setState(DctConstants.State.IDLE);
            apnContext.setDataConnection(null);
        }

        // If there is any outstanding handover request, we need to respond it.
        sendHandoverCompleteMessages(apnContext.getApnTypeBitmask(), false, false);

        // Make sure reconnection alarm is cleaned up if there is no ApnContext
        // associated to the connection.
        if (dataConnection != null) {
            cancelReconnect(apnContext);
        }
        str = "cleanUpConnectionInternal: X detach=" + detach + " reason="
                + apnContext.getReason();
        if (DBG) log(str + " apnContext=" + apnContext + " dc=" + apnContext.getDataConnection());
    }

    private Cursor getPreferredApnCursor(int subId) {
        Cursor cursor = null;
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            cursor = mPhone.getContext().getContentResolver().query(
                    Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID,
                            String.valueOf(subId)), null, null, null,
                    Telephony.Carriers.DEFAULT_SORT_ORDER);
        }
        return cursor;
    }

    private ApnSetting getPreferredApnFromDB() {
        ApnSetting preferredApn = null;
        Cursor cursor = getPreferredApnCursor(mPhone.getSubId());
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                preferredApn = ApnSetting.makeApnSetting(cursor);
            }
            cursor.close();
        }
        if (VDBG) log("getPreferredApnFromDB: preferredApn=" + preferredApn);
        return preferredApn;
    }

    private void setDefaultPreferredApnIfNeeded() {
        ApnSetting defaultPreferredApn = null;
        PersistableBundle bundle = getCarrierConfig();
        String defaultPreferredApnName = bundle.getString(CarrierConfigManager
                .KEY_DEFAULT_PREFERRED_APN_NAME_STRING);

        if (TextUtils.isEmpty(defaultPreferredApnName) || getPreferredApnFromDB() != null) {
            return;
        }

        String selection = Telephony.Carriers.APN + " = \"" + defaultPreferredApnName + "\" AND "
                + Telephony.Carriers.EDITED_STATUS + " = " + Telephony.Carriers.UNEDITED;
        Cursor cursor = mPhone.getContext().getContentResolver().query(
                Uri.withAppendedPath(Telephony.Carriers.SIM_APN_URI,
                        "filtered/subId/" + mPhone.getSubId()),
                null, selection, null, Telephony.Carriers._ID);

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    defaultPreferredApn = ApnSetting.makeApnSetting(cursor);
                }
            }
            cursor.close();
        }

        if (defaultPreferredApn != null
                && defaultPreferredApn.canHandleType(mRequestedApnType)) {
            log("setDefaultPreferredApnIfNeeded: For APN type "
                    + ApnSetting.getApnTypeString(mRequestedApnType)
                    + " found default apnSetting "
                    + defaultPreferredApn);

            setPreferredApn(defaultPreferredApn.getId(), true);
        }

        return;
    }

    /**
     * Check if preferred apn is allowed to edit by user.
     * @return {@code true} if it is allowed to edit.
     */
    @VisibleForTesting
    public boolean isPreferredApnUserEdited() {
        boolean isUserEdited = false;
        Cursor cursor = getPreferredApnCursor(mPhone.getSubId());
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    isUserEdited = cursor.getInt(
                            cursor.getColumnIndexOrThrow(Telephony.Carriers.EDITED_STATUS))
                            == Telephony.Carriers.USER_EDITED;
                }
            }
            cursor.close();
        }
        if (VDBG) log("isPreferredApnUserEdited: isUserEdited=" + isUserEdited);
        return isUserEdited;
    }

    /**
     * Fetch the DUN apns
     * @return a list of DUN ApnSetting objects
     */
    @VisibleForTesting
    public @NonNull ArrayList<ApnSetting> fetchDunApns() {
        if (mPhone.getServiceState().getRoaming() && !isPreferredApnUserEdited()
                && getCarrierConfig().getBoolean(CarrierConfigManager
                .KEY_DISABLE_DUN_APN_WHILE_ROAMING_WITH_PRESET_APN_BOOL)) {
            if (VDBG) log("fetchDunApns: Dun apn is not used in roaming network");
            return new ArrayList<ApnSetting>(0);
        }

        int bearer = getDataRat();
        ArrayList<ApnSetting> dunCandidates = new ArrayList<ApnSetting>();
        ArrayList<ApnSetting> retDunSettings = new ArrayList<ApnSetting>();

        if (dunCandidates.isEmpty()) {
            if (!ArrayUtils.isEmpty(mAllApnSettings)) {
                for (ApnSetting apn : mAllApnSettings) {
                    if (apn.canHandleType(ApnSetting.TYPE_DUN)) {
                        dunCandidates.add(apn);
                    }
                }
                if (VDBG) log("fetchDunApns: dunCandidates from database: " + dunCandidates);
            }
        }

        int preferredApnSetId = getPreferredApnSetId();
        ApnSetting preferredApn = getPreferredApnFromDB();
        for (ApnSetting dunSetting : dunCandidates) {
            if (dunSetting.canSupportNetworkType(
                    ServiceState.rilRadioTechnologyToNetworkType(bearer))) {
                if (preferredApnSetId == dunSetting.getApnSetId()) {
                    if (preferredApn != null && preferredApn.equals(dunSetting)) {
                        // If there is a preferred APN can handled DUN type, prepend it to list to
                        // use it preferred.
                        retDunSettings.add(0, dunSetting);
                    } else {
                        retDunSettings.add(dunSetting);
                    }
                }
            }
        }

        if (VDBG) log("fetchDunApns: dunSettings=" + retDunSettings);
        return retDunSettings;
    }

    private int getPreferredApnSetId() {
        // preferapnset uri returns all APNs for the current carrier which have an apn_set_id
        // equal to the preferred APN (if no preferred APN, or if the preferred APN has no set id,
        // the query will return null)
        Cursor c = mPhone.getContext().getContentResolver()
                .query(Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI,
                    "preferapnset/subId/" + mPhone.getSubId()),
                        new String[] {Telephony.Carriers.APN_SET_ID}, null, null, null);
        if (c == null) {
            loge("getPreferredApnSetId: cursor is null");
            return Telephony.Carriers.NO_APN_SET_ID;
        }

        int setId;
        if (c.getCount() < 1) {
            loge("getPreferredApnSetId: no APNs found");
            setId = Telephony.Carriers.NO_APN_SET_ID;
        } else {
            c.moveToFirst();
            setId = c.getInt(0 /* index of Telephony.Carriers.APN_SET_ID */);
        }

        if (!c.isClosed()) {
            c.close();
        }
        return setId;
    }

    public boolean hasMatchedTetherApnSetting() {
        ArrayList<ApnSetting> matches = fetchDunApns();
        log("hasMatchedTetherApnSetting: APNs=" + matches);
        return matches.size() > 0;
    }

    /**
     * @return the {@link DataConnection} with the given context id {@code cid}.
     */
    public DataConnection getDataConnectionByContextId(int cid) {
        return mDcc.getActiveDcByCid(cid);
    }

    /**
     * @return the {@link DataConnection} with the given APN context. Null if no data connection
     * is found.
     */
    public @Nullable DataConnection getDataConnectionByApnType(String apnType) {
        // TODO: Clean up all APN type in string usage
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext != null) {
            return apnContext.getDataConnection();
        }
        return null;
    }

    /**
     * Check if the data fail cause is a permanent failure (i.e. Frameworks will not retry data
     * setup).
     *
     * @param dcFailCause The data fail cause
     * @return {@code true} if the data fail cause is a permanent failure.
     */
    @VisibleForTesting
    public boolean isPermanentFailure(@DataFailureCause int dcFailCause) {
        return (DataFailCause.isPermanentFailure(mPhone.getContext(), dcFailCause,
                mPhone.getSubId())
                && (mAttached.get() == false || dcFailCause != DataFailCause.SIGNAL_LOST));
    }

    private DataConnection findFreeDataConnection() {
        for (DataConnection dataConnection : mDataConnections.values()) {
            boolean inUse = false;
            for (ApnContext apnContext : mApnContexts.values()) {
                if (apnContext.getDataConnection() == dataConnection) {
                    inUse = true;
                    break;
                }
            }
            if (!inUse) {
                if (DBG) {
                    log("findFreeDataConnection: found free DataConnection=" + dataConnection);
                }
                return dataConnection;
            }
        }
        log("findFreeDataConnection: NO free DataConnection");
        return null;
    }

    /**
     * Setup a data connection based on given APN type.
     *
     * @param apnContext APN context
     * @param radioTech RAT of the data connection
     * @param requestType Data request type
     * @return True if successful, otherwise false.
     */
    private boolean setupData(ApnContext apnContext, int radioTech,
                              @RequestNetworkType int requestType) {
        if (DBG) {
            log("setupData: apnContext=" + apnContext + ", requestType="
                    + requestTypeToString(requestType));
        }
        ApnContext.requestLog(
                apnContext, "setupData. requestType=" + requestTypeToString(requestType));
        ApnSetting apnSetting;
        DataConnection dataConnection = null;

        apnSetting = apnContext.getNextApnSetting();

        if (apnSetting == null) {
            if (DBG) log("setupData: return for no apn found!");
            return false;
        }

        // profile id is only meaningful when the profile is persistent on the modem.
        int profileId = DATA_PROFILE_INVALID;
        if (apnSetting.isPersistent()) {
            profileId = apnSetting.getProfileId();
            if (profileId == DATA_PROFILE_DEFAULT) {
                profileId = getApnProfileID(apnContext.getApnType());
            }
        }

        // On CDMA, if we're explicitly asking for DUN, we need have
        // a dun-profiled connection so we can't share an existing one
        // On GSM/LTE we can share existing apn connections provided they support
        // this type.
        // If asking for ENTERPRISE, there are no compatible data connections, so skip this check
        if ((apnContext.getApnTypeBitmask() != ApnSetting.TYPE_DUN
                || ServiceState.isGsm(getDataRat()))
                && apnContext.getApnTypeBitmask() != ApnSetting.TYPE_ENTERPRISE) {
            dataConnection = checkForCompatibleDataConnection(apnContext, apnSetting);
            if (dataConnection != null) {
                // Get the apn setting used by the data connection
                ApnSetting dataConnectionApnSetting = dataConnection.getApnSetting();
                if (dataConnectionApnSetting != null) {
                    // Setting is good, so use it.
                    apnSetting = dataConnectionApnSetting;
                }
            }
        }
        if (dataConnection == null) {
            if (isOnlySingleDcAllowed(radioTech)) {
                if (isHigherPriorityApnContextActive(apnContext)) {
                    if (DBG) {
                        log("setupData: Higher priority ApnContext active.  Ignoring call");
                    }
                    return false;
                }

                // Should not start cleanUp if the setupData is for IMS APN
                // or retry of same APN(State==RETRYING).
                if (!apnContext.getApnType().equals(ApnSetting.TYPE_IMS_STRING)
                        && (apnContext.getState() != DctConstants.State.RETRYING)) {
                    // Only lower priority calls left.  Disconnect them all in this single PDP case
                    // so that we can bring up the requested higher priority call (once we receive
                    // response for deactivate request for the calls we are about to disconnect
                    if (cleanUpAllConnectionsInternal(true, Phone.REASON_SINGLE_PDN_ARBITRATION)) {
                        // If any call actually requested to be disconnected, means we can't
                        // bring up this connection yet as we need to wait for those data calls
                        // to be disconnected.
                        if (DBG) log("setupData: Some calls are disconnecting first."
                                + " Wait and retry");
                        return false;
                    }
                }

                // No other calls are active, so proceed
                if (DBG) log("setupData: Single pdp. Continue setting up data call.");
            }

            dataConnection = findFreeDataConnection();

            if (dataConnection == null) {
                dataConnection = createDataConnection();
            }

            if (dataConnection == null) {
                if (DBG) log("setupData: No free DataConnection and couldn't create one, WEIRD");
                return false;
            }
        }
        final int generation = apnContext.incAndGetConnectionGeneration();
        if (DBG) {
            log("setupData: dc=" + dataConnection + " apnSetting=" + apnSetting + " gen#="
                    + generation);
        }

        apnContext.setDataConnection(dataConnection);
        apnContext.setApnSetting(apnSetting);
        apnContext.setState(DctConstants.State.CONNECTING);

        Message msg = obtainMessage();
        msg.what = DctConstants.EVENT_DATA_SETUP_COMPLETE;
        msg.obj = new Pair<ApnContext, Integer>(apnContext, generation);

        ApnSetting preferredApn = getPreferredApn();
        boolean isPreferredApn = apnSetting.equals(preferredApn);
        dataConnection.bringUp(apnContext, profileId, radioTech, msg, generation, requestType,
                mPhone.getSubId(), isPreferredApn);

        if (DBG) {
            if (isPreferredApn) {
                log("setupData: initing! isPreferredApn=" + isPreferredApn
                        + ", apnSetting={" + apnSetting.toString() + "}");
            } else {
                String preferredApnStr = preferredApn == null ? "null" : preferredApn.toString();
                log("setupData: initing! isPreferredApn=" + isPreferredApn
                        + ", apnSetting={" + apnSetting + "}"
                        + ", preferredApn={" + preferredApnStr + "}");
            }
        }
        return true;
    }

    // Get the allowed APN types for initial attach. The order in the returned list represent
    // the order of APN types that should be used for initial attach.
    private @NonNull @ApnType List<Integer> getAllowedInitialAttachApnTypes() {
        PersistableBundle bundle = getCarrierConfig();
        if (bundle != null) {
            String[] apnTypesArray = bundle.getStringArray(
                    CarrierConfigManager.KEY_ALLOWED_INITIAL_ATTACH_APN_TYPES_STRING_ARRAY);
            if (apnTypesArray != null) {
                return Arrays.stream(apnTypesArray)
                        .map(ApnSetting::getApnTypesBitmaskFromString)
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    protected void setInitialAttachApn() {
        ApnSetting apnSetting = null;
        int preferredApnSetId = getPreferredApnSetId();
        ArrayList<ApnSetting> allApnSettings = new ArrayList<>();
        if (mPreferredApn != null) {
            // Put the preferred apn at the beginning of the list. It's okay to have a duplicate
            // when later on mAllApnSettings get added. That would not change the selection result.
            allApnSettings.add(mPreferredApn);
        }
        allApnSettings.addAll(mAllApnSettings);

        // Get the allowed APN types for initial attach. Note that if none of the APNs has the
        // allowed APN types, then the initial attach will not be performed.
        List<Integer> allowedApnTypes = getAllowedInitialAttachApnTypes();
        for (int allowedApnType : allowedApnTypes) {
            apnSetting = allApnSettings.stream()
                    .filter(apn -> apn.canHandleType(allowedApnType))
                    .filter(apn -> (apn.getApnSetId() == preferredApnSetId
                            || apn.getApnSetId() == Telephony.Carriers.MATCH_ALL_APN_SET_ID))
                    .findFirst()
                    .orElse(null);
            if (apnSetting != null) break;
        }

        if (DBG) {
            log("setInitialAttachApn: Allowed APN types=" + allowedApnTypes.stream()
                    .map(ApnSetting::getApnTypeString)
                    .collect(Collectors.joining(",")));
        }

        if (apnSetting == null) {
            if (DBG) log("setInitialAttachApn: X There in no available apn.");
        } else {
            if (DBG) log("setInitialAttachApn: X selected APN=" + apnSetting);
            mDataServiceManager.setInitialAttachApn(new DataProfile.Builder()
                    .setApnSetting(apnSetting)
                    .setPreferred(apnSetting.equals(getPreferredApn()))
                    .build(),
                    mPhone.getServiceState().getDataRoamingFromRegistration(), null);
        }
    }

    /**
     * Handles changes to the APN database.
     */
    private void onApnChanged() {
        if (mPhone instanceof GsmCdmaPhone) {
            // The "current" may no longer be valid.  MMS depends on this to send properly. TBD
            ((GsmCdmaPhone)mPhone).updateCurrentCarrierInProvider();
        }

        // TODO: It'd be nice to only do this if the changed entrie(s)
        // match the current operator.
        if (DBG) log("onApnChanged: createAllApnList and cleanUpAllConnections");
        mDataThrottler.reset();
        setDefaultPreferredApnIfNeeded();
        createAllApnList();
        setDataProfilesAsNeeded();
        setInitialAttachApn();
        cleanUpConnectionsOnUpdatedApns(isAnyDataConnected(), Phone.REASON_APN_CHANGED);

        // FIXME: See bug 17426028 maybe no conditional is needed.
        if (mPhone.getSubId() == SubscriptionManager.getDefaultDataSubscriptionId()) {
            setupDataOnAllConnectableApns(Phone.REASON_APN_CHANGED, RetryFailures.ALWAYS);
        }
    }

    /**
     * "Active" here means ApnContext isEnabled() and not in FAILED state
     * @param apnContext to compare with
     * @return true if higher priority active apn found
     */
    private boolean isHigherPriorityApnContextActive(ApnContext apnContext) {
        if (apnContext.getApnType().equals(ApnSetting.TYPE_IMS_STRING)) {
            return false;
        }

        for (ApnContext otherContext : mPrioritySortedApnContexts) {
            if (otherContext.getApnType().equals(ApnSetting.TYPE_IMS_STRING)) {
                continue;
            }
            if (apnContext.getApnType().equalsIgnoreCase(otherContext.getApnType())) return false;
            if (otherContext.isEnabled() && otherContext.getState() != DctConstants.State.FAILED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reports if we support multiple connections or not.
     * This is a combination of factors, based on carrier and RAT.
     * @param rilRadioTech the RIL Radio Tech currently in use
     * @return true if only single DataConnection is allowed
     */
    private boolean isOnlySingleDcAllowed(int rilRadioTech) {
        int networkType = ServiceState.rilRadioTechnologyToNetworkType(rilRadioTech);
        // Default single dc rats with no knowledge of carrier
        int[] singleDcRats = null;
        // get the carrier specific value, if it exists, from CarrierConfigManager.
        // generally configManager and bundle should not be null, but if they are it should be okay
        // to leave singleDcRats null as well
        CarrierConfigManager configManager = (CarrierConfigManager)
                mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            PersistableBundle bundle = configManager.getConfigForSubId(mPhone.getSubId());
            if (bundle != null) {
                singleDcRats = bundle.getIntArray(
                        CarrierConfigManager.KEY_ONLY_SINGLE_DC_ALLOWED_INT_ARRAY);
            }
        }
        boolean onlySingleDcAllowed = false;
        if (TelephonyUtils.IS_DEBUGGABLE
                && SystemProperties.getBoolean("persist.telephony.test.singleDc", false)) {
            onlySingleDcAllowed = true;
        }
        if (singleDcRats != null) {
            for (int i = 0; i < singleDcRats.length && !onlySingleDcAllowed; i++) {
                if (networkType == singleDcRats[i]) {
                    onlySingleDcAllowed = true;
                }
            }
        }

        if (DBG) {
            log("isOnlySingleDcAllowed(" + TelephonyManager.getNetworkTypeName(networkType) + "): "
                    + onlySingleDcAllowed);
        }
        return onlySingleDcAllowed;
    }

    void sendRestartRadio() {
        if (DBG)log("sendRestartRadio:");
        Message msg = obtainMessage(DctConstants.EVENT_RESTART_RADIO);
        sendMessage(msg);
    }

    private void restartRadio() {
        if (DBG) log("restartRadio: ************TURN OFF RADIO**************");
        cleanUpAllConnectionsInternal(true, Phone.REASON_RADIO_TURNED_OFF);
        mPhone.getServiceStateTracker().powerOffRadioSafely();
        /* Note: no need to call setRadioPower(true).  Assuming the desired
         * radio power state is still ON (as tracked by ServiceStateTracker),
         * ServiceStateTracker will call setRadioPower when it receives the
         * RADIO_STATE_CHANGED notification for the power off.  And if the
         * desired power state has changed in the interim, we don't want to
         * override it with an unconditional power on.
         */
    }

    /**
     * Return true if data connection need to be setup after disconnected due to
     * reason.
     *
     * @param apnContext APN context
     * @return true if try setup data connection is need for this reason
     */
    private boolean retryAfterDisconnected(ApnContext apnContext) {
        boolean retry = true;
        String reason = apnContext.getReason();

        if (Phone.REASON_RADIO_TURNED_OFF.equals(reason) || (isOnlySingleDcAllowed(getDataRat())
                && isHigherPriorityApnContextActive(apnContext))) {
            retry = false;
        }
        return retry;
    }

    protected void startReconnect(long delay, ApnContext apnContext,
            @RequestNetworkType int requestType) {
        apnContext.setState(DctConstants.State.RETRYING);
        Message msg = obtainMessage(DctConstants.EVENT_DATA_RECONNECT,
                       mPhone.getSubId(), requestType, apnContext);
        cancelReconnect(apnContext);

        // Wait a bit before trying the next APN, so that
        // we're not tying up the RIL command channel
        sendMessageDelayed(msg, delay);

        if (DBG) {
            log("startReconnect: delay=" + delay + ", apn="
                    + apnContext + ", reason=" + apnContext.getReason()
                    + ", subId=" + mPhone.getSubId() + ", request type="
                    + requestTypeToString(requestType));
        }
    }

    /**
     * Cancels the alarm associated with apnContext.
     *
     * @param apnContext on which the alarm should be stopped.
     */
    protected void cancelReconnect(ApnContext apnContext) {
        if (apnContext == null) return;

        if (DBG) {
            log("cancelReconnect: apn=" + apnContext);
        }
        removeMessages(DctConstants.EVENT_DATA_RECONNECT, apnContext);
    }

    /**
     * Read configuration. Note this must be called after carrier config is ready.
     */
    private void readConfiguration() {
        log("readConfiguration");
        if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
            // Auto attach is for cellular only.
            mAutoAttachOnCreationConfig = mPhone.getContext().getResources()
                    .getBoolean(com.android.internal.R.bool.config_auto_attach_data_on_creation);
        }

        mAutoAttachEnabled.set(false);
        setDefaultPreferredApnIfNeeded();
        read5GConfiguration();
        registerSettingsObserver();
        SubscriptionPlan[] plans = mNetworkPolicyManager.getSubscriptionPlans(
                mPhone.getSubId(), mPhone.getContext().getOpPackageName());
        mSubscriptionPlans = plans == null ? Collections.emptyList() : Arrays.asList(plans);
        if (DBG) log("SubscriptionPlans initialized: " + mSubscriptionPlans);
        reevaluateUnmeteredConnections();
        mConfigReady = true;
    }

    /**
     * @return {@code true} if carrier config has been applied.
     */
    private boolean isCarrierConfigApplied() {
        CarrierConfigManager configManager = (CarrierConfigManager) mPhone.getContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            PersistableBundle b = configManager.getConfigForSubId(mPhone.getSubId());
            if (b != null) {
                return CarrierConfigManager.isConfigForIdentifiedCarrier(b);
            }
        }
        return false;
    }

    private void onCarrierConfigChanged() {
        if (DBG) log("onCarrierConfigChanged");

        if (!isCarrierConfigApplied()) {
            log("onCarrierConfigChanged: Carrier config is not ready yet.");
            return;
        }

        readConfiguration();

        if (mSimState == TelephonyManager.SIM_STATE_LOADED) {
            setDefaultDataRoamingEnabled();
            createAllApnList();
            setDataProfilesAsNeeded();
            setInitialAttachApn();
            sortApnContextByPriority();
            cleanUpConnectionsOnUpdatedApns(true, Phone.REASON_CARRIER_CHANGE);
            setupDataOnAllConnectableApns(Phone.REASON_CARRIER_CHANGE, RetryFailures.ALWAYS);
        } else {
            log("onCarrierConfigChanged: SIM is not loaded yet.");
        }
    }

    private void onSimAbsent() {
        if (DBG) log("onSimAbsent");

        mConfigReady = false;
        cleanUpAllConnectionsInternal(true, Phone.REASON_SIM_NOT_READY);
        mAllApnSettings.clear();
        mAutoAttachOnCreationConfig = false;
        // Clear auto attach as modem is expected to do a new attach once SIM is ready
        mAutoAttachEnabled.set(false);
        // In no-sim case, we should still send the emergency APN to the modem, if there is any.
        createAllApnList();
        setDataProfilesAsNeeded();
    }

    private void onSimStateUpdated(@SimState int simState) {
        mSimState = simState;

        if (DBG) {
            log("onSimStateUpdated: state=" + SubscriptionInfoUpdater.simStateString(mSimState));
        }

        if (mSimState == TelephonyManager.SIM_STATE_ABSENT) {
            onSimAbsent();
        } else if (mSimState == TelephonyManager.SIM_STATE_LOADED) {
            mDataThrottler.reset();
            if (mConfigReady) {
                createAllApnList();
                setDataProfilesAsNeeded();
                setInitialAttachApn();
                setupDataOnAllConnectableApns(Phone.REASON_SIM_LOADED, RetryFailures.ALWAYS);
            } else {
                log("onSimStateUpdated: config not ready yet.");
            }
        }
    }

    private void onApnUnthrottled(String apn) {
        if (apn != null) {
            ApnSetting apnSetting = mAllApnSettings.stream()
                    .filter(as -> apn.equals(as.getApnName()))
                    .findFirst()
                    .orElse(null);
            if (apnSetting != null) {
                @ApnType int apnTypes = apnSetting.getApnTypeBitmask();
                mDataThrottler.setRetryTime(apnTypes, RetryManager.NO_SUGGESTED_RETRY_DELAY,
                        REQUEST_TYPE_NORMAL);
            } else {
                loge("EVENT_APN_UNTHROTTLED: Invalid APN passed: " + apn);
            }
        } else {
            loge("EVENT_APN_UNTHROTTLED: apn is null");
        }
    }

    private void onTrafficDescriptorsUpdated() {
        for (ApnContext apnContext : mPrioritySortedApnContexts) {
            if (apnContext.getApnTypeBitmask() == ApnSetting.TYPE_ENTERPRISE
                    && apnContext.getApnSetting().getPermanentFailed()) {
                setupDataOnConnectableApn(
                        apnContext, Phone.REASON_TRAFFIC_DESCRIPTORS_UPDATED, RetryFailures.ALWAYS);
            }
        }
    }

    private DataConnection checkForCompatibleDataConnection(ApnContext apnContext,
            ApnSetting nextApn) {
        int apnType = apnContext.getApnTypeBitmask();
        ArrayList<ApnSetting> dunSettings = null;

        if (ApnSetting.TYPE_DUN == apnType) {
            dunSettings = fetchDunApns();
        }
        if (DBG) {
            log("checkForCompatibleDataConnection: apnContext=" + apnContext);
        }

        DataConnection potentialDc = null;
        for (DataConnection curDc : mDataConnections.values()) {
            if (curDc != null) {
                ApnSetting apnSetting = curDc.getApnSetting();
                log("apnSetting: " + apnSetting);
                if (dunSettings != null && dunSettings.size() > 0) {
                    for (ApnSetting dunSetting : dunSettings) {
                        //This ignore network type as a check which is ok because that's checked
                        //when calculating dun candidates.
                        if (areCompatible(dunSetting, apnSetting)) {
                            if (curDc.isActive()) {
                                if (DBG) {
                                    log("checkForCompatibleDataConnection:"
                                            + " found dun conn=" + curDc);
                                }
                                return curDc;
                            } else if (curDc.isActivating()) {
                                potentialDc = curDc;
                            }
                        }
                    }
                } else if (isApnSettingCompatible(curDc, apnType)) {
                    if (curDc.isActive()) {
                        if (DBG) {
                            log("checkForCompatibleDataConnection:"
                                    + " found canHandle conn=" + curDc);
                        }
                        return curDc;
                    } else if (curDc.isActivating()
                            || (apnSetting !=  null && apnSetting.equals(nextApn))) {
                        potentialDc = curDc;
                    }
                }
            }
        }

        if (DBG) {
            log("checkForCompatibleDataConnection: potential dc=" + potentialDc);
        }
        return potentialDc;
    }

    private boolean isApnSettingCompatible(DataConnection dc, int apnType) {
        ApnSetting apnSetting = dc.getApnSetting();
        if (apnSetting == null) return false;

        // Nothing can be compatible with type ENTERPRISE
        for (ApnContext apnContext : dc.getApnContexts()) {
            if (apnContext.getApnTypeBitmask() == ApnSetting.TYPE_ENTERPRISE) {
                return false;
            }
        }

        return apnSetting.canHandleType(apnType);
    }

    private void addHandoverCompleteMsg(Message onCompleteMsg,
            @ApnType int apnType) {
        if (onCompleteMsg != null) {
            List<Message> messageList = mHandoverCompletionMsgs.get(apnType);
            if (messageList == null) messageList = new ArrayList<>();
            messageList.add(onCompleteMsg);
            mHandoverCompletionMsgs.put(apnType, messageList);
        }
    }

    private void sendHandoverCompleteMessages(@ApnType int apnType, boolean success,
            boolean fallbackOnFailedHandover) {
        List<Message> messageList = mHandoverCompletionMsgs.get(apnType);
        if (messageList != null) {
            for (Message msg : messageList) {
                sendHandoverCompleteMsg(msg, success, mTransportType, fallbackOnFailedHandover);
            }
            messageList.clear();
        }
    }

    private void sendHandoverCompleteMsg(Message message, boolean success,
            @TransportType int transport, boolean doFallbackOnFailedHandover) {
        if (message == null) return;

        Bundle b = message.getData();
        b.putBoolean(DATA_COMPLETE_MSG_EXTRA_SUCCESS, success);
        b.putInt(DATA_COMPLETE_MSG_EXTRA_TRANSPORT_TYPE, transport);
        b.putBoolean(DATA_COMPLETE_MSG_EXTRA_HANDOVER_FAILURE_FALLBACK, doFallbackOnFailedHandover);
        message.sendToTarget();
    }

    private static boolean shouldFallbackOnFailedHandover(
                               @HandoverFailureMode int handoverFailureMode,
                               @RequestNetworkType int requestType,
                               @DataFailureCause int cause) {
        if (requestType != REQUEST_TYPE_HANDOVER) {
            //The fallback is only relevant if the request is a handover
            return false;
        } else if (handoverFailureMode == HANDOVER_FAILURE_MODE_DO_FALLBACK) {
            return true;
        } else if (handoverFailureMode == HANDOVER_FAILURE_MODE_LEGACY) {
            return cause == DataFailCause.HANDOFF_PREFERENCE_CHANGED;
        } else {
            return false;
        }
    }

    /**
     * Calculates the new request type that will be used the next time a data connection retries
     * after a failed data call attempt.
     */
    @RequestNetworkType
    public static int calculateNewRetryRequestType(@HandoverFailureMode int handoverFailureMode,
            @RequestNetworkType int requestType,
            @DataFailureCause int cause) {
        boolean fallbackOnFailedHandover =
                shouldFallbackOnFailedHandover(handoverFailureMode, requestType, cause);
        if (requestType != REQUEST_TYPE_HANDOVER) {
            //The fallback is only relevant if the request is a handover
            return requestType;
        }

        if (fallbackOnFailedHandover) {
            // Since fallback is happening, the request type is really "NONE".
            return REQUEST_TYPE_NORMAL;
        }

        if (handoverFailureMode == HANDOVER_FAILURE_MODE_NO_FALLBACK_RETRY_SETUP_NORMAL) {
            return REQUEST_TYPE_NORMAL;
        }

        return REQUEST_TYPE_HANDOVER;
    }

    public void enableApn(@ApnType int apnType, @RequestNetworkType int requestType,
            Message onHandoverCompleteMsg) {
        sendMessage(obtainMessage(DctConstants.EVENT_ENABLE_APN, apnType, requestType,
                onHandoverCompleteMsg));
    }

    private void onEnableApn(@ApnType int apnType, @RequestNetworkType int requestType,
            Message onHandoverCompleteMsg) {
        ApnContext apnContext = mApnContextsByType.get(apnType);
        if (apnContext == null) {
            loge("onEnableApn(" + apnType + "): NO ApnContext");
            if (onHandoverCompleteMsg != null) {
                sendHandoverCompleteMsg(onHandoverCompleteMsg, false, mTransportType, false);
            }
            return;
        }

        String str = "onEnableApn: apnType=" + ApnSetting.getApnTypeString(apnType)
                + ", request type=" + requestTypeToString(requestType);
        if (DBG) log(str);
        ApnContext.requestLog(apnContext, str);

        if (!apnContext.isDependencyMet()) {
            apnContext.setReason(Phone.REASON_DATA_DEPENDENCY_UNMET);
            apnContext.setEnabled(true);
            str = "onEnableApn: dependency is not met.";
            if (DBG) log(str);
            ApnContext.requestLog(apnContext, str);
            if (onHandoverCompleteMsg != null) {
                sendHandoverCompleteMsg(onHandoverCompleteMsg, false, mTransportType, false);
            }
            return;
        }

        if (apnContext.isReady()) {
            DctConstants.State state = apnContext.getState();
            switch(state) {
                case CONNECTING:
                    if (onHandoverCompleteMsg != null) {
                        if (DBG) {
                            log("onEnableApn: already in CONNECTING state. Handover request "
                                    + "will be responded after connected.");
                        }
                        addHandoverCompleteMsg(onHandoverCompleteMsg, apnType);
                    } else {
                        if (DBG) log("onEnableApn: in CONNECTING state. Exit now.");
                    }
                    return;
                case CONNECTED:
                    if (onHandoverCompleteMsg != null) {
                        sendHandoverCompleteMsg(onHandoverCompleteMsg, true, mTransportType,
                                false);
                        if (DBG) {
                            log("onEnableApn: already in CONNECTED state. Consider as handover "
                                    + "succeeded");
                        }
                    } else {
                        if (DBG) log("onEnableApn: APN in CONNECTED state. Exit now.");
                    }
                    return;
                case IDLE:
                case FAILED:
                case RETRYING:
                    // We're "READY" but not active so disconnect (cleanup = true) and
                    // connect (trySetup = true) to be sure we retry the connection.
                    apnContext.setReason(Phone.REASON_DATA_ENABLED);
                    break;
            }
        } else {
            if (apnContext.isEnabled()) {
                apnContext.setReason(Phone.REASON_DATA_DEPENDENCY_MET);
            } else {
                apnContext.setReason(Phone.REASON_DATA_ENABLED);
            }
            if (apnContext.getState() == DctConstants.State.FAILED) {
                apnContext.setState(DctConstants.State.IDLE);
            }
        }
        apnContext.setEnabled(true);
        apnContext.resetErrorCodeRetries();

        if (mConfigReady || apnContext.getApnTypeBitmask() == ApnSetting.TYPE_EMERGENCY) {
            trySetupData(apnContext, requestType, onHandoverCompleteMsg);
        } else {
            log("onEnableApn: config not ready yet.");
        }
    }

    public void disableApn(@ApnType int apnType, @ReleaseNetworkType int releaseType) {
        sendMessage(obtainMessage(DctConstants.EVENT_DISABLE_APN, apnType, releaseType));
    }

    private void onDisableApn(@ApnType int apnType,
                              @ReleaseNetworkType int releaseType) {
        ApnContext apnContext = mApnContextsByType.get(apnType);
        if (apnContext == null) {
            loge("disableApn(" + apnType + "): NO ApnContext");
            return;
        }

        boolean cleanup = false;
        String str = "onDisableApn: apnType=" + ApnSetting.getApnTypeString(apnType)
                + ", release type=" + releaseTypeToString(releaseType);
        if (DBG) log(str);
        ApnContext.requestLog(apnContext, str);

        if (apnContext.isReady()) {
            cleanup = (releaseType == RELEASE_TYPE_DETACH
                    || releaseType == RELEASE_TYPE_HANDOVER);
            if (apnContext.isDependencyMet()) {
                apnContext.setReason(Phone.REASON_DATA_DISABLED_INTERNAL);
                // If ConnectivityService has disabled this network, stop trying to bring
                // it up, but do not tear it down - ConnectivityService will do that
                // directly by talking with the DataConnection.
                //
                // This doesn't apply to DUN. When the user disable tethering, we would like to
                // detach the APN context from the data connection so the data connection can be
                // torn down if no other APN context attached to it.
                if (ApnSetting.TYPE_DUN_STRING.equals(apnContext.getApnType())
                        || apnContext.getState() != DctConstants.State.CONNECTED) {
                    str = "Clean up the connection. Apn type = " + apnContext.getApnType()
                            + ", state = " + apnContext.getState();
                    if (DBG) log(str);
                    ApnContext.requestLog(apnContext, str);
                    cleanup = true;
                }
            } else {
                apnContext.setReason(Phone.REASON_DATA_DEPENDENCY_UNMET);
            }
        }

        apnContext.setEnabled(false);
        if (cleanup) {
            cleanUpConnectionInternal(true, releaseType, apnContext);
        }

        if (isOnlySingleDcAllowed(getDataRat()) && !isHigherPriorityApnContextActive(apnContext)) {
            if (DBG) log("disableApn:isOnlySingleDcAllowed true & higher priority APN disabled");
            // If the highest priority APN is disabled and only single
            // data call is allowed, try to setup data call on other connectable APN.
            setupDataOnAllConnectableApns(Phone.REASON_SINGLE_PDN_ARBITRATION,
                    RetryFailures.ALWAYS);
        }
    }

    /**
     * Modify {@link android.provider.Settings.Global#DATA_ROAMING} value for user modification only
     */
    public void setDataRoamingEnabledByUser(boolean enabled) {
        mDataEnabledSettings.setDataRoamingEnabled(enabled);
        setDataRoamingFromUserAction(true);
        if (DBG) {
            log("setDataRoamingEnabledByUser: set phoneSubId=" + mPhone.getSubId()
                    + " isRoaming=" + enabled);
        }
    }

    /**
     * Return current {@link android.provider.Settings.Global#DATA_ROAMING} value.
     */
    public boolean getDataRoamingEnabled() {
        boolean isDataRoamingEnabled = mDataEnabledSettings.getDataRoamingEnabled();

        if (VDBG) {
            log("getDataRoamingEnabled: phoneSubId=" + mPhone.getSubId()
                    + " isDataRoamingEnabled=" + isDataRoamingEnabled);
        }
        return isDataRoamingEnabled;
    }

    /**
     * Set default value for {@link android.provider.Settings.Global#DATA_ROAMING}
     * if the setting is not from user actions. default value is based on carrier config and system
     * properties.
     */
    private void setDefaultDataRoamingEnabled() {
        // For single SIM phones, this is a per phone property.
        String setting = Settings.Global.DATA_ROAMING;
        boolean useCarrierSpecificDefault = false;
        if (mTelephonyManager.getSimCount() != 1) {
            setting = setting + mPhone.getSubId();
            try {
                Settings.Global.getInt(mResolver, setting);
            } catch (SettingNotFoundException ex) {
                // For msim, update to carrier default if uninitialized.
                useCarrierSpecificDefault = true;
            }
        } else if (!isDataRoamingFromUserAction()) {
            // for single sim device, update to carrier default if user action is not set
            useCarrierSpecificDefault = true;
        }
        log("setDefaultDataRoamingEnabled: useCarrierSpecificDefault "
                + useCarrierSpecificDefault);
        if (useCarrierSpecificDefault) {
            boolean defaultVal = mDataEnabledSettings.getDefaultDataRoamingEnabled();
            mDataEnabledSettings.setDataRoamingEnabled(defaultVal);
        }
    }

    private boolean isDataRoamingFromUserAction() {
        final SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(mPhone.getContext());
        // since we don't want to unset user preference from system update, pass true as the default
        // value if shared pref does not exist and set shared pref to false explicitly from factory
        // reset.
        if (!sp.contains(Phone.DATA_ROAMING_IS_USER_SETTING_KEY)) {
            sp.edit().putBoolean(Phone.DATA_ROAMING_IS_USER_SETTING_KEY, false).commit();
        }
        return sp.getBoolean(Phone.DATA_ROAMING_IS_USER_SETTING_KEY, true);
    }

    private void setDataRoamingFromUserAction(boolean isUserAction) {
        final SharedPreferences.Editor sp = PreferenceManager
                .getDefaultSharedPreferences(mPhone.getContext()).edit();
        sp.putBoolean(Phone.DATA_ROAMING_IS_USER_SETTING_KEY, isUserAction).commit();
    }

    // When the data roaming status changes from roaming to non-roaming.
    private void onDataRoamingOff() {
        if (DBG) log("onDataRoamingOff");

        reevaluateDataConnections();

        if (!getDataRoamingEnabled()) {
            // TODO: Remove this once all old vendor RILs are gone. We don't need to set initial apn
            // attach and send the data profile again as the modem should have both roaming and
            // non-roaming protocol in place. Modem should choose the right protocol based on the
            // roaming condition.
            setDataProfilesAsNeeded();
            setInitialAttachApn();

            // If the user did not enable data roaming, now when we transit from roaming to
            // non-roaming, we should try to reestablish the data connection.

            setupDataOnAllConnectableApns(Phone.REASON_ROAMING_OFF, RetryFailures.ALWAYS);
        }
    }

    // This method is called
    // 1. When the data roaming status changes from non-roaming to roaming.
    // 2. When allowed data roaming settings is changed by the user.
    private void onDataRoamingOnOrSettingsChanged(int messageType) {
        if (DBG) log("onDataRoamingOnOrSettingsChanged");
        // Used to differentiate data roaming turned on vs settings changed.
        boolean settingChanged = (messageType == DctConstants.EVENT_ROAMING_SETTING_CHANGE);

        // Check if the device is actually data roaming
        if (!mPhone.getServiceState().getDataRoaming()) {
            if (DBG) log("device is not roaming. ignored the request.");
            return;
        }

        checkDataRoamingStatus(settingChanged);

        if (getDataRoamingEnabled()) {
            // If the restricted data was brought up when data roaming is disabled, and now users
            // enable data roaming, we need to re-evaluate the conditions and possibly change the
            // network's capability.
            if (settingChanged) {
                reevaluateDataConnections();
            }

            if (DBG) log("onDataRoamingOnOrSettingsChanged: setup data on roaming");

            setupDataOnAllConnectableApns(Phone.REASON_ROAMING_ON, RetryFailures.ALWAYS);
        } else {
            // If the user does not turn on data roaming, when we transit from non-roaming to
            // roaming, we need to tear down the data connection otherwise the user might be
            // charged for data roaming usage.
            if (DBG) log("onDataRoamingOnOrSettingsChanged: Tear down data connection on roaming.");
            cleanUpAllConnectionsInternal(true, Phone.REASON_ROAMING_ON);
        }
    }

    // We want to track possible roaming data leakage. Which is, if roaming setting
    // is disabled, yet we still setup a roaming data connection or have a connected ApnContext
    // switched to roaming. When this happens, we log it in a local log.
    private void checkDataRoamingStatus(boolean settingChanged) {
        if (!settingChanged && !getDataRoamingEnabled()
                && mPhone.getServiceState().getDataRoaming()) {
            for (ApnContext apnContext : mApnContexts.values()) {
                if (apnContext.getState() == DctConstants.State.CONNECTED) {
                    mDataRoamingLeakageLog.log("PossibleRoamingLeakage "
                            + " connection params: " + (apnContext.getDataConnection() != null
                            ? apnContext.getDataConnection().getConnectionParams() : ""));
                }
            }
        }
    }

    private void onRadioAvailable() {
        if (DBG) log("onRadioAvailable");
        if (!areAllDataDisconnected()) {
            cleanUpConnectionInternal(true, RELEASE_TYPE_DETACH, null);
        }
    }

    private void onRadioOffOrNotAvailable() {
        // Make sure our reconnect delay starts at the initial value
        // next time the radio comes on

        mReregisterOnReconnectFailure = false;

        // Clear auto attach as modem is expected to do a new attach
        mAutoAttachEnabled.set(false);

        if (mPhone.getSimulatedRadioControl() != null) {
            // Assume data is connected on the simulator
            // FIXME  this can be improved
            log("We're on the simulator; assuming radio off is meaningless");
        } else {
            if (DBG) log("onRadioOffOrNotAvailable: is off and clean up all connections");
            cleanUpAllConnectionsInternal(false, Phone.REASON_RADIO_TURNED_OFF);
        }
    }

    private void completeConnection(ApnContext apnContext, @RequestNetworkType int type) {

        if (DBG) log("completeConnection: successful, notify the world apnContext=" + apnContext);

        if (mIsProvisioning && !TextUtils.isEmpty(mProvisioningUrl)) {
            if (DBG) {
                log("completeConnection: MOBILE_PROVISIONING_ACTION url="
                        + mProvisioningUrl);
            }
            Intent newIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_BROWSER);
            newIntent.setData(Uri.parse(mProvisioningUrl));
            newIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mPhone.getContext().startActivity(newIntent);
            } catch (ActivityNotFoundException e) {
                loge("completeConnection: startActivityAsUser failed" + e);
            }
        }
        mIsProvisioning = false;
        mProvisioningUrl = null;
        if (mProvisioningSpinner != null) {
            sendMessage(obtainMessage(DctConstants.CMD_CLEAR_PROVISIONING_SPINNER,
                    mProvisioningSpinner));
        }

        startNetStatPoll();
        startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);

        PersistableBundle b = getCarrierConfig();
        if (apnContext.getApnTypeBitmask() == ApnSetting.TYPE_DEFAULT
                && b.getBoolean(CarrierConfigManager
                .KEY_DISPLAY_NO_DATA_NOTIFICATION_ON_PERMANENT_FAILURE_BOOL)) {
            NotificationManager notificationManager = (NotificationManager)
                    mPhone.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(Integer.toString(mPhone.getSubId()),
                    NO_DATA_NOTIFICATION);
        }
    }

    /**
     * A SETUP (aka bringUp) has completed, possibly with an error. If
     * there is an error this method will call {@link #onDataSetupCompleteError}.
     */
    protected void onDataSetupComplete(ApnContext apnContext, boolean success,
            @DataFailureCause int cause, @RequestNetworkType int requestType,
            @HandoverFailureMode int handoverFailureMode) {
        boolean fallbackOnFailedHandover = shouldFallbackOnFailedHandover(
                handoverFailureMode, requestType, cause);

        if (success && (handoverFailureMode != DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN
                && handoverFailureMode != DataCallResponse.HANDOVER_FAILURE_MODE_LEGACY)) {
            Log.wtf(mLogTag, "bad failure mode: "
                    + DataCallResponse.failureModeToString(handoverFailureMode));
        } else if (handoverFailureMode
                != DataCallResponse.HANDOVER_FAILURE_MODE_NO_FALLBACK_RETRY_HANDOVER
                && cause != DataFailCause.SERVICE_TEMPORARILY_UNAVAILABLE) {
            sendHandoverCompleteMessages(apnContext.getApnTypeBitmask(), success,
                    fallbackOnFailedHandover);
        }

        if (success) {
            DataConnection dataConnection = apnContext.getDataConnection();

            if (RADIO_TESTS) {
                // Note: To change radio.test.onDSC.null.dcac from command line you need to
                // adb root and adb remount and from the command line you can only change the
                // value to 1 once. To change it a second time you can reboot or execute
                // adb shell stop and then adb shell start. The command line to set the value is:
                // adb shell sqlite3 /data/data/com.android.providers.settings/databases/settings.db "insert into system (name,value) values ('radio.test.onDSC.null.dcac', '1');"
                ContentResolver cr = mPhone.getContext().getContentResolver();
                String radioTestProperty = "radio.test.onDSC.null.dcac";
                if (Settings.System.getInt(cr, radioTestProperty, 0) == 1) {
                    log("onDataSetupComplete: " + radioTestProperty +
                            " is true, set dcac to null and reset property to false");
                    dataConnection = null;
                    Settings.System.putInt(cr, radioTestProperty, 0);
                    log("onDataSetupComplete: " + radioTestProperty + "=" +
                            Settings.System.getInt(mPhone.getContext().getContentResolver(),
                                    radioTestProperty, -1));
                }
            }
            if (dataConnection == null) {
                log("onDataSetupComplete: no connection to DC, handle as error");
                onDataSetupCompleteError(apnContext, requestType, false);
            } else {
                ApnSetting apn = apnContext.getApnSetting();
                if (DBG) {
                    log("onDataSetupComplete: success apn=" + (apn == null ? "unknown"
                            : apn.getApnName()));
                }

                // everything is setup
                if (TextUtils.equals(apnContext.getApnType(), ApnSetting.TYPE_DEFAULT_STRING)
                        && mCanSetPreferApn && mPreferredApn == null) {
                    if (DBG) log("onDataSetupComplete: PREFERRED APN is null");
                    mPreferredApn = apn;
                    if (mPreferredApn != null) {
                        setPreferredApn(mPreferredApn.getId());
                    }
                }

                // A connection is setup
                apnContext.setState(DctConstants.State.CONNECTED);

                checkDataRoamingStatus(false);

                boolean isProvApn = apnContext.isProvisioningApn();
                final ConnectivityManager cm = (ConnectivityManager) mPhone.getContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                if (mProvisionBroadcastReceiver != null) {
                    mPhone.getContext().unregisterReceiver(mProvisionBroadcastReceiver);
                    mProvisionBroadcastReceiver = null;
                }

                if ((!isProvApn) || mIsProvisioning) {
                    if (mIsProvisioning) {
                        // Hide any notification that was showing previously
                        hideProvisioningNotification();
                    }

                    // Complete the connection normally notifying the world we're connected.
                    // We do this if this isn't a special provisioning apn or if we've been
                    // told its time to provision.
                    completeConnection(apnContext, requestType);
                } else {
                    // This is a provisioning APN that we're reporting as connected. Later
                    // when the user desires to upgrade this to a "default" connection,
                    // mIsProvisioning == true, we'll go through the code path above.
                    // mIsProvisioning becomes true when CMD_ENABLE_MOBILE_PROVISIONING
                    // is sent to the DCT.
                    if (DBG) {
                        log("onDataSetupComplete: successful, BUT send connected to prov apn as"
                                + " mIsProvisioning:" + mIsProvisioning + " == false"
                                + " && (isProvisioningApn:" + isProvApn + " == true");
                    }

                    // While radio is up, grab provisioning URL.  The URL contains ICCID which
                    // disappears when radio is off.
                    mProvisionBroadcastReceiver = new ProvisionNotificationBroadcastReceiver(
                            mPhone.getMobileProvisioningUrl(),
                            mTelephonyManager.getNetworkOperatorName());
                    mPhone.getContext().registerReceiver(mProvisionBroadcastReceiver,
                            new IntentFilter(INTENT_PROVISION));

                    // Put up user notification that sign-in is required.
                    showProvisioningNotification();

                    // Turn off radio to save battery and avoid wasting carrier resources.
                    // The network isn't usable and network validation will just fail anyhow.
                    setRadio(false);
                }
                if (DBG) {
                    log("onDataSetupComplete: SETUP complete type=" + apnContext.getApnType());
                }
                if (TelephonyUtils.IS_DEBUGGABLE) {
                    // adb shell setprop persist.radio.test.pco [pco_val]
                    String radioTestProperty = "persist.radio.test.pco";
                    int pcoVal = SystemProperties.getInt(radioTestProperty, -1);
                    if (pcoVal != -1) {
                        log("PCO testing: read pco value from persist.radio.test.pco " + pcoVal);
                        final byte[] value = new byte[1];
                        value[0] = (byte) pcoVal;
                        final Intent intent =
                                new Intent(TelephonyManager.ACTION_CARRIER_SIGNAL_PCO_VALUE);
                        intent.putExtra(TelephonyManager.EXTRA_APN_TYPE, ApnSetting.TYPE_DEFAULT);
                        intent.putExtra(TelephonyManager.EXTRA_APN_PROTOCOL,
                                ApnSetting.PROTOCOL_IPV4V6);
                        intent.putExtra(TelephonyManager.EXTRA_PCO_ID, 0xFF00);
                        intent.putExtra(TelephonyManager.EXTRA_PCO_VALUE, value);
                        mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);
                    }
                }
            }
        } else {
            if (DBG) {
                ApnSetting apn = apnContext.getApnSetting();
                log("onDataSetupComplete: error apn=" + apn.getApnName() + ", cause="
                        + DataFailCause.toString(cause) + ", requestType="
                        + requestTypeToString(requestType));
            }
            if (DataFailCause.isEventLoggable(cause)) {
                // Log this failure to the Event Logs.
                int cid = getCellLocationId();
                EventLog.writeEvent(EventLogTags.PDP_SETUP_FAIL,
                        cause, cid, mTelephonyManager.getNetworkType());
            }
            ApnSetting apn = apnContext.getApnSetting();

            // Compose broadcast intent send to the specific carrier signaling receivers
            Intent intent = new Intent(TelephonyManager
                    .ACTION_CARRIER_SIGNAL_REQUEST_NETWORK_FAILED);
            intent.putExtra(TelephonyManager.EXTRA_DATA_FAIL_CAUSE, cause);
            intent.putExtra(TelephonyManager.EXTRA_APN_TYPE,
                    ApnSetting.getApnTypesBitmaskFromString(apnContext.getApnType()));
            mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);

            if (DataFailCause.isRadioRestartFailure(mPhone.getContext(), cause, mPhone.getSubId())
                    || apnContext.restartOnError(cause)) {
                if (DBG) log("Modem restarted.");
                sendRestartRadio();
            }

            // If the data call failure cause is a permanent failure, we mark the APN as permanent
            // failed.
            if (isPermanentFailure(cause)) {
                log("cause=" + DataFailCause.toString(cause)
                        + ", mark apn as permanent failed. apn = " + apn);
                apnContext.markApnPermanentFailed(apn);

                PersistableBundle b = getCarrierConfig();
                if (apnContext.getApnTypeBitmask() == ApnSetting.TYPE_DEFAULT
                        && b.getBoolean(CarrierConfigManager
                        .KEY_DISPLAY_NO_DATA_NOTIFICATION_ON_PERMANENT_FAILURE_BOOL)) {
                    NotificationManager notificationManager = (NotificationManager)
                            mPhone.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

                    CharSequence title = mPhone.getContext().getText(
                            com.android.internal.R.string.RestrictedOnDataTitle);
                    CharSequence details = mPhone.getContext().getText(
                            com.android.internal.R.string.RestrictedStateContent);

                    Notification notification = new Notification.Builder(mPhone.getContext(),
                            NotificationChannelController.CHANNEL_ID_MOBILE_DATA_STATUS)
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            .setSmallIcon(com.android.internal.R.drawable.stat_sys_warning)
                            .setTicker(title)
                            .setColor(mPhone.getContext().getResources().getColor(
                                    com.android.internal.R.color.system_notification_accent_color))
                            .setContentTitle(title)
                            .setStyle(new Notification.BigTextStyle().bigText(details))
                            .setContentText(details)
                            .build();
                    notificationManager.notify(Integer.toString(mPhone.getSubId()),
                            NO_DATA_NOTIFICATION, notification);
                }
            }

            int newRequestType = calculateNewRetryRequestType(handoverFailureMode, requestType,
                    cause);
            onDataSetupCompleteError(apnContext, newRequestType, fallbackOnFailedHandover);
        }
    }



    /**
     * Error has occurred during the SETUP {aka bringUP} request and the DCT
     * should either try the next waiting APN or start over from the
     * beginning if the list is empty. Between each SETUP request there will
     * be a delay defined by {@link ApnContext#getDelayForNextApn(boolean)}.
     */
    protected void onDataSetupCompleteError(ApnContext apnContext,
            @RequestNetworkType int requestType, boolean fallbackOnFailedHandover) {
        long delay = apnContext.getDelayForNextApn(mFailFast);
        // Check if we need to retry or not.
        if (delay >= 0 && delay != RetryManager.NO_RETRY && !fallbackOnFailedHandover) {
            if (DBG) {
                log("onDataSetupCompleteError: APN type=" + apnContext.getApnType()
                        + ". Request type=" + requestTypeToString(requestType) + ", Retry in "
                        + delay + "ms.");
            }
            startReconnect(delay, apnContext, requestType);
        } else {
            // If we are not going to retry any APN, set this APN context to failed state.
            // This would be the final state of a data connection.
            apnContext.setState(DctConstants.State.FAILED);
            apnContext.setDataConnection(null);
            log("onDataSetupCompleteError: Stop retrying APNs. delay=" + delay
                    + ", requestType=" + requestTypeToString(requestType));
            //send request network complete messages as needed
            sendHandoverCompleteMessages(apnContext.getApnTypeBitmask(), false,
                    fallbackOnFailedHandover);
        }
    }

    /**
     * Called when EVENT_NETWORK_STATUS_CHANGED is received.
     *
     * @param status One of {@code NetworkAgent.VALID_NETWORK} or
     * {@code NetworkAgent.INVALID_NETWORK}.
     * @param cid context id {@code cid}
     * @param redirectUrl If the Internet probe was redirected, this
     * is the destination it was redirected to, otherwise {@code null}
     */
    private void onNetworkStatusChanged(int status, int cid, String redirectUrl) {
        if (!TextUtils.isEmpty(redirectUrl)) {
            Intent intent = new Intent(TelephonyManager.ACTION_CARRIER_SIGNAL_REDIRECTED);
            intent.putExtra(TelephonyManager.EXTRA_REDIRECTION_URL, redirectUrl);
            mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);
            log("Notify carrier signal receivers with redirectUrl: " + redirectUrl);
        } else {
            final boolean isValid = status == NetworkAgent.VALIDATION_STATUS_VALID;
            final DataConnection dc = getDataConnectionByContextId(cid);
            if (!mDsRecoveryHandler.isRecoveryOnBadNetworkEnabled()) {
                if (DBG) log("Skip data stall recovery on network status change with in threshold");
                return;
            }
            if (mTransportType != AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                if (DBG) log("Skip data stall recovery on non WWAN");
                return;
            }
            if (dc != null && dc.isValidationRequired()) {
                mDsRecoveryHandler.processNetworkStatusChanged(isValid);
            }
        }
    }

    /**
     * Called when EVENT_DISCONNECT_DONE is received.
     */
    private void onDisconnectDone(ApnContext apnContext) {
        if(DBG) log("onDisconnectDone: EVENT_DISCONNECT_DONE apnContext=" + apnContext);
        apnContext.setState(DctConstants.State.IDLE);
        // If all data connection are gone, check whether Airplane mode request was pending.
        if (areAllDataDisconnected()
                && mPhone.getServiceStateTracker().processPendingRadioPowerOffAfterDataOff()) {
            if (DBG) log("onDisconnectDone: radio will be turned off, no retries");
            // Radio will be turned off. No need to retry data setup
            apnContext.setApnSetting(null);
            apnContext.setDataConnection(null);

            // Need to notify disconnect as well, in the case of switching Airplane mode.
            // Otherwise, it would cause 30s delayed to turn on Airplane mode.
            notifyAllDataDisconnected();
            return;
        }
        // If APN is still enabled, try to bring it back up automatically
        if (mAttached.get() && apnContext.isReady() && retryAfterDisconnected(apnContext)) {
            // Wait a bit before trying the next APN, so that
            // we're not tying up the RIL command channel.
            // This also helps in any external dependency to turn off the context.
            if (DBG) log("onDisconnectDone: attached, ready and retry after disconnect");

            // See if there are still handover request pending that we need to retry handover
            // after previous data gets disconnected.
            if (isHandoverPending(apnContext.getApnTypeBitmask())) {
                if (DBG) log("Handover request pending. Retry handover immediately.");
                startReconnect(0, apnContext, REQUEST_TYPE_HANDOVER);
            } else {
                long delay = apnContext.getRetryAfterDisconnectDelay();
                if (delay > 0) {
                    // Data connection is in IDLE state, so when we reconnect later, we'll rebuild
                    // the waiting APN list, which will also reset/reconfigure the retry manager.
                    startReconnect(delay, apnContext, REQUEST_TYPE_NORMAL);
                }
            }
        } else {
            boolean restartRadioAfterProvisioning = mPhone.getContext().getResources().getBoolean(
                    com.android.internal.R.bool.config_restartRadioAfterProvisioning);

            if (apnContext.isProvisioningApn() && restartRadioAfterProvisioning) {
                log("onDisconnectDone: restartRadio after provisioning");
                restartRadio();
            }
            apnContext.setApnSetting(null);
            apnContext.setDataConnection(null);
            if (isOnlySingleDcAllowed(getDataRat())) {
                if(DBG) log("onDisconnectDone: isOnlySigneDcAllowed true so setup single apn");
                setupDataOnAllConnectableApns(Phone.REASON_SINGLE_PDN_ARBITRATION,
                        RetryFailures.ALWAYS);
            } else {
                if(DBG) log("onDisconnectDone: not retrying");
            }
        }

        if (areAllDataDisconnected()) {
            apnContext.setConcurrentVoiceAndDataAllowed(
                    mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed());
            notifyAllDataDisconnected();
        }

    }

    private void onVoiceCallStarted() {
        if (DBG) log("onVoiceCallStarted");
        mInVoiceCall = true;
        if (isAnyDataConnected()
                && !mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
            if (DBG) log("onVoiceCallStarted stop polling");
            stopNetStatPoll();
            stopDataStallAlarm();
        }
    }

    protected void onVoiceCallEnded() {
        if (DBG) log("onVoiceCallEnded");
        mInVoiceCall = false;
        if (isAnyDataConnected()) {
            if (!mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                startNetStatPoll();
                startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
            } else {
                // clean slate after call end.
                resetPollStats();
            }
        }
        // reset reconnect timer
        setupDataOnAllConnectableApns(Phone.REASON_VOICE_CALL_ENDED, RetryFailures.ALWAYS);
    }
    /**
     * @return {@code true} if there is any data in connected state.
     */
    @VisibleForTesting
    public boolean isAnyDataConnected() {
        for (DataConnection dc : mDataConnections.values()) {
            if (dc.isActive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} if all data connections are in disconnected state.
     */
    public boolean areAllDataDisconnected() {
        for (DataConnection dc : mDataConnections.values()) {
            if (!dc.isInactive()) {
                if (DBG) log("areAllDataDisconnected false due to DC: " + dc.getName());
                return false;
            }
        }
        return true;
    }

    protected void setDataProfilesAsNeeded() {
        if (DBG) log("setDataProfilesAsNeeded");

        ArrayList<DataProfile> dataProfileList = new ArrayList<>();

        int preferredApnSetId = getPreferredApnSetId();
        for (ApnSetting apn : mAllApnSettings) {
            if (apn.getApnSetId() == Telephony.Carriers.MATCH_ALL_APN_SET_ID
                    || preferredApnSetId == apn.getApnSetId()) {
                DataProfile dp = new DataProfile.Builder()
                        .setApnSetting(apn)
                        .setPreferred(apn.equals(getPreferredApn()))
                        .build();
                if (!dataProfileList.contains(dp)) {
                    dataProfileList.add(dp);
                }
            } else {
                if (VDBG) {
                    log("setDataProfilesAsNeeded: APN set id " + apn.getApnSetId()
                            + " does not match the preferred set id " + preferredApnSetId);
                }
            }
        }

        // Check if the data profiles we are sending are same as we did last time. We don't want to
        // send the redundant profiles to the modem. Also if there the list is empty, we don't
        // send it to the modem.
        if (!dataProfileList.isEmpty()
                && (dataProfileList.size() != mLastDataProfileList.size()
                || !mLastDataProfileList.containsAll(dataProfileList))) {
            mDataServiceManager.setDataProfile(dataProfileList,
                    mPhone.getServiceState().getDataRoamingFromRegistration(), null);
        }
    }

    /**
     * Based on the sim operator numeric, create a list for all possible
     * Data Connections and setup the preferredApn.
     */
    protected void createAllApnList() {
        mAllApnSettings.clear();
        String operator = mPhone.getOperatorNumeric();

        // ORDER BY Telephony.Carriers._ID ("_id")
        Cursor cursor = mPhone.getContext().getContentResolver().query(
                Uri.withAppendedPath(Telephony.Carriers.SIM_APN_URI, "filtered/subId/"
                        + mPhone.getSubId()), null, null, null, Telephony.Carriers._ID);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                ApnSetting apn = ApnSetting.makeApnSetting(cursor);
                if (apn == null) {
                    continue;
                }
                mAllApnSettings.add(apn);
            }
            cursor.close();
        } else {
            if (DBG) log("createAllApnList: cursor is null");
            mApnSettingsInitializationLog.log("cursor is null for carrier, operator: "
                    + operator);
        }

        dedupeApnSettings();

        if (mAllApnSettings.isEmpty()) {
            log("createAllApnList: No APN found for carrier, operator: " + operator);
            mApnSettingsInitializationLog.log("no APN found for carrier, operator: "
                    + operator);
            mPreferredApn = null;
        } else {
            mPreferredApn = getPreferredApn();
            if (DBG) log("createAllApnList: mPreferredApn=" + mPreferredApn);
        }

        addDefaultApnSettingsAsNeeded();
        if (DBG) log("createAllApnList: X mAllApnSettings=" + mAllApnSettings);
    }

    private void dedupeApnSettings() {
        ArrayList<ApnSetting> resultApns = new ArrayList<ApnSetting>();

        // coalesce APNs if they are similar enough to prevent
        // us from bringing up two data calls with the same interface
        int i = 0;
        while (i < mAllApnSettings.size() - 1) {
            ApnSetting first = mAllApnSettings.get(i);
            ApnSetting second = null;
            int j = i + 1;
            while (j < mAllApnSettings.size()) {
                second = mAllApnSettings.get(j);
                if (first.similar(second)) {
                    ApnSetting newApn = mergeApns(first, second);
                    mAllApnSettings.set(i, newApn);
                    first = newApn;
                    mAllApnSettings.remove(j);
                } else {
                    j++;
                }
            }
            i++;
        }
    }

    private ApnSetting mergeApns(ApnSetting dest, ApnSetting src) {
        int id = dest.getId();
        if ((src.getApnTypeBitmask() & ApnSetting.TYPE_DEFAULT) == ApnSetting.TYPE_DEFAULT) {
            id = src.getId();
        }
        final int resultApnType = src.getApnTypeBitmask() | dest.getApnTypeBitmask();
        Uri mmsc = (dest.getMmsc() == null ? src.getMmsc() : dest.getMmsc());
        String mmsProxy = TextUtils.isEmpty(dest.getMmsProxyAddressAsString())
                ? src.getMmsProxyAddressAsString() : dest.getMmsProxyAddressAsString();
        int mmsPort = dest.getMmsProxyPort() == -1 ? src.getMmsProxyPort() : dest.getMmsProxyPort();
        String proxy = TextUtils.isEmpty(dest.getProxyAddressAsString())
                ? src.getProxyAddressAsString() : dest.getProxyAddressAsString();
        int port = dest.getProxyPort() == -1 ? src.getProxyPort() : dest.getProxyPort();
        int protocol = src.getProtocol() == ApnSetting.PROTOCOL_IPV4V6 ? src.getProtocol()
                : dest.getProtocol();
        int roamingProtocol = src.getRoamingProtocol() == ApnSetting.PROTOCOL_IPV4V6
                ? src.getRoamingProtocol() : dest.getRoamingProtocol();
        int networkTypeBitmask = (dest.getNetworkTypeBitmask() == 0
                || src.getNetworkTypeBitmask() == 0)
                ? 0 : (dest.getNetworkTypeBitmask() | src.getNetworkTypeBitmask());
        return new ApnSetting.Builder()
                .setId(id)
                .setOperatorNumeric(dest.getOperatorNumeric())
                .setEntryName(dest.getEntryName())
                .setApnName(dest.getApnName())
                .setProxyAddress(proxy)
                .setProxyPort(port)
                .setMmsc(mmsc)
                .setMmsProxyAddress(mmsProxy)
                .setMmsProxyPort(mmsPort)
                .setUser(dest.getUser())
                .setPassword(dest.getPassword())
                .setAuthType(dest.getAuthType())
                .setApnTypeBitmask(resultApnType)
                .setProtocol(protocol)
                .setRoamingProtocol(roamingProtocol)
                .setCarrierEnabled(dest.isEnabled())
                .setNetworkTypeBitmask(networkTypeBitmask)
                .setProfileId(dest.getProfileId())
                .setModemCognitive(dest.isPersistent() || src.isPersistent())
                .setMaxConns(dest.getMaxConns())
                .setWaitTime(dest.getWaitTime())
                .setMaxConnsTime(dest.getMaxConnsTime())
                .setMtuV4(dest.getMtuV4())
                .setMtuV6(dest.getMtuV6())
                .setMvnoType(dest.getMvnoType())
                .setMvnoMatchData(dest.getMvnoMatchData())
                .setApnSetId(dest.getApnSetId())
                .setCarrierId(dest.getCarrierId())
                .setSkip464Xlat(dest.getSkip464Xlat())
                .build();
    }

    private DataConnection createDataConnection() {
        if (DBG) log("createDataConnection E");

        int id = mUniqueIdGenerator.getAndIncrement();
        DataConnection dataConnection = DataConnection.makeDataConnection(mPhone, id, this,
                mDataServiceManager, mDcTesterFailBringUpAll, mDcc);
        mDataConnections.put(id, dataConnection);
        if (DBG) log("createDataConnection() X id=" + id + " dc=" + dataConnection);
        return dataConnection;
    }

    private void destroyDataConnections() {
        if(mDataConnections != null) {
            if (DBG) log("destroyDataConnections: clear mDataConnectionList");
            mDataConnections.clear();
        } else {
            if (DBG) log("destroyDataConnections: mDataConnecitonList is empty, ignore");
        }
    }

    /**
     * Build a list of APNs to be used to create PDP's.
     *
     * @param requestedApnType
     * @return waitingApns list to be used to create PDP
     *          error when waitingApns.isEmpty()
     */
    private @NonNull ArrayList<ApnSetting> buildWaitingApns(String requestedApnType,
            int radioTech) {
        if (DBG) log("buildWaitingApns: E requestedApnType=" + requestedApnType);
        ArrayList<ApnSetting> apnList = new ArrayList<ApnSetting>();

        int requestedApnTypeBitmask = ApnSetting.getApnTypesBitmaskFromString(requestedApnType);
        if (requestedApnTypeBitmask == ApnSetting.TYPE_ENTERPRISE) {
            requestedApnTypeBitmask = ApnSetting.TYPE_DEFAULT;
        }
        if (requestedApnTypeBitmask == ApnSetting.TYPE_DUN) {
            ArrayList<ApnSetting> dunApns = fetchDunApns();
            if (dunApns.size() > 0) {
                for (ApnSetting dun : dunApns) {
                    apnList.add(dun);
                    if (DBG) log("buildWaitingApns: X added APN_TYPE_DUN apnList=" + apnList);
                }
                return apnList;
            }
        }

        String operator = mPhone.getOperatorNumeric();

        // This is a workaround for a bug (7305641) where we don't failover to other
        // suitable APNs if our preferred APN fails.  On prepaid ATT sims we need to
        // failover to a provisioning APN, but once we've used their default data
        // connection we are locked to it for life.  This change allows ATT devices
        // to say they don't want to use preferred at all.
        boolean usePreferred = true;
        try {
            usePreferred = !mPhone.getContext().getResources().getBoolean(com.android
                    .internal.R.bool.config_dontPreferApn);
        } catch (Resources.NotFoundException e) {
            if (DBG) log("buildWaitingApns: usePreferred NotFoundException set to true");
            usePreferred = true;
        }
        if (usePreferred) {
            mPreferredApn = getPreferredApn();
        }
        if (DBG) {
            log("buildWaitingApns: usePreferred=" + usePreferred
                    + " canSetPreferApn=" + mCanSetPreferApn
                    + " mPreferredApn=" + mPreferredApn
                    + " operator=" + operator + " radioTech=" + radioTech);
        }

        if (usePreferred && mCanSetPreferApn && mPreferredApn != null &&
                mPreferredApn.canHandleType(requestedApnTypeBitmask)) {
            if (DBG) {
                log("buildWaitingApns: Preferred APN:" + operator + ":"
                        + mPreferredApn.getOperatorNumeric() + ":" + mPreferredApn);
            }

            if (TextUtils.equals(mPreferredApn.getOperatorNumeric(), operator)
                    || mPreferredApn.getCarrierId() == mPhone.getCarrierId()) {
                if (mPreferredApn.canSupportNetworkType(
                        ServiceState.rilRadioTechnologyToNetworkType(radioTech))) {
                    // Create a new instance of ApnSetting for ENTERPRISE because each
                    // DataConnection should have its own ApnSetting. ENTERPRISE uses the same
                    // APN as DEFAULT but is a separate DataConnection
                    if (ApnSetting.getApnTypesBitmaskFromString(requestedApnType)
                            == ApnSetting.TYPE_ENTERPRISE) {
                        apnList.add(ApnSetting.makeApnSetting(mPreferredApn));
                    } else {
                        apnList.add(mPreferredApn);
                    }
                    if (DBG) log("buildWaitingApns: X added preferred apnList=" + apnList);
                    return apnList;
                }
            }
            if (DBG) log("buildWaitingApns: no preferred APN");
            setPreferredApn(-1);
            mPreferredApn = null;
        }

        if (DBG) log("buildWaitingApns: mAllApnSettings=" + mAllApnSettings);
        int preferredApnSetId = getPreferredApnSetId();
        for (ApnSetting apn : mAllApnSettings) {
            if (apn.canHandleType(requestedApnTypeBitmask)) {
                if (apn.canSupportNetworkType(
                        ServiceState.rilRadioTechnologyToNetworkType(radioTech))) {
                    if (apn.getApnSetId() == Telephony.Carriers.MATCH_ALL_APN_SET_ID
                            || preferredApnSetId == apn.getApnSetId()) {
                        if (VDBG) log("buildWaitingApns: adding apn=" + apn);
                        // Create a new instance of ApnSetting for ENTERPRISE because each
                        // DataConnection should have its own ApnSetting. ENTERPRISE uses the same
                        // APN as DEFAULT but is a separate DataConnection
                        if (ApnSetting.getApnTypesBitmaskFromString(requestedApnType)
                                == ApnSetting.TYPE_ENTERPRISE) {
                            apnList.add(ApnSetting.makeApnSetting(apn));
                        } else {
                            apnList.add(apn);
                        }
                    } else {
                        log("buildWaitingApns: APN set id " + apn.getApnSetId()
                                + " does not match the preferred set id " + preferredApnSetId);
                    }
                } else {
                    if (DBG) {
                        log("buildWaitingApns: networkTypeBitmask:"
                                + apn.getNetworkTypeBitmask()
                                + " does not include radioTech:"
                                + ServiceState.rilRadioTechnologyToString(radioTech));
                    }
                }
            } else if (VDBG) {
                log("buildWaitingApns: couldn't handle requested ApnType="
                        + requestedApnType);
            }
        }

        if (DBG) log("buildWaitingApns: " + apnList.size() + " APNs in the list: " + apnList);
        return apnList;
    }

    private String apnListToString (ArrayList<ApnSetting> apns) {
        StringBuilder result = new StringBuilder();
        for (int i = 0, size = apns.size(); i < size; i++) {
            result.append('[')
                  .append(apns.get(i).toString())
                  .append(']');
        }
        return result.toString();
    }

    private void setPreferredApn(int pos) {
        setPreferredApn(pos, false);
    }

    private void setPreferredApn(int pos, boolean force) {
        if (!force && !mCanSetPreferApn) {
            log("setPreferredApn: X !canSEtPreferApn");
            return;
        }

        String subId = Long.toString(mPhone.getSubId());
        Uri uri = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, subId);
        log("setPreferredApn: delete");
        ContentResolver resolver = mPhone.getContext().getContentResolver();
        resolver.delete(uri, null, null);

        if (pos >= 0) {
            log("setPreferredApn: insert");
            ContentValues values = new ContentValues();
            values.put(APN_ID, pos);
            resolver.insert(uri, values);
        }
    }

    @Nullable
    ApnSetting getPreferredApn() {
        //Only call this method from main thread
        if (mAllApnSettings == null || mAllApnSettings.isEmpty()) {
            log("getPreferredApn: mAllApnSettings is empty");
            return null;
        }

        String subId = Long.toString(mPhone.getSubId());
        Uri uri = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, subId);
        Cursor cursor = mPhone.getContext().getContentResolver().query(
                uri, new String[] { "_id", "name", "apn" },
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            mCanSetPreferApn = true;
        } else {
            mCanSetPreferApn = false;
        }

        if (VDBG) {
            log("getPreferredApn: mRequestedApnType=" + mRequestedApnType + " cursor=" + cursor
                    + " cursor.count=" + ((cursor != null) ? cursor.getCount() : 0));
        }

        if (mCanSetPreferApn && cursor.getCount() > 0) {
            int pos;
            cursor.moveToFirst();
            pos = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID));
            for(ApnSetting p : mAllApnSettings) {
                if (p.getId() == pos && p.canHandleType(mRequestedApnType)) {
                    log("getPreferredApn: For APN type "
                            + ApnSetting.getApnTypeString(mRequestedApnType)
                            + " found apnSetting " + p);
                    cursor.close();
                    return p;
                }
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        log("getPreferredApn: X not found");
        return null;
    }

    @Override
    public void handleMessage (Message msg) {
        if (VDBG) log("handleMessage msg=" + msg);

        AsyncResult ar;
        Pair<ApnContext, Integer> pair;
        ApnContext apnContext;
        int generation;
        int requestType;
        int handoverFailureMode;
        switch (msg.what) {
            case DctConstants.EVENT_DATA_CONNECTION_DETACHED:
                onDataConnectionDetached();
                break;

            case DctConstants.EVENT_DATA_CONNECTION_ATTACHED:
                onDataConnectionAttached();
                break;

            case DctConstants.EVENT_DO_RECOVERY:
                mDsRecoveryHandler.doRecovery();
                break;

            case DctConstants.EVENT_APN_CHANGED:
                onApnChanged();
                break;

            case DctConstants.EVENT_PS_RESTRICT_ENABLED:
                /**
                 * We don't need to explicitly to tear down the PDP context
                 * when PS restricted is enabled. The base band will deactive
                 * PDP context and notify us with PDP_CONTEXT_CHANGED.
                 * But we should stop the network polling and prevent reset PDP.
                 */
                if (DBG) log("EVENT_PS_RESTRICT_ENABLED " + mIsPsRestricted);
                stopNetStatPoll();
                stopDataStallAlarm();
                mIsPsRestricted = true;
                break;

            case DctConstants.EVENT_PS_RESTRICT_DISABLED:
                /**
                 * When PS restrict is removed, we need setup PDP connection if
                 * PDP connection is down.
                 */
                if (DBG) log("EVENT_PS_RESTRICT_DISABLED " + mIsPsRestricted);
                mIsPsRestricted  = false;
                if (isAnyDataConnected()) {
                    startNetStatPoll();
                    startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
                } else {
                    // TODO: Should all PDN states be checked to fail?
                    if (mState == DctConstants.State.FAILED) {
                        cleanUpAllConnectionsInternal(false, Phone.REASON_PS_RESTRICT_ENABLED);
                        mReregisterOnReconnectFailure = false;
                    }
                    apnContext = mApnContextsByType.get(ApnSetting.TYPE_DEFAULT);
                    if (apnContext != null) {
                        apnContext.setReason(Phone.REASON_PS_RESTRICT_ENABLED);
                        trySetupData(apnContext, REQUEST_TYPE_NORMAL, null);
                    } else {
                        loge("**** Default ApnContext not found ****");
                        if (TelephonyUtils.IS_DEBUGGABLE) {
                            throw new RuntimeException("Default ApnContext not found");
                        }
                    }
                }
                break;

            case DctConstants.EVENT_TRY_SETUP_DATA:
                apnContext = (ApnContext) msg.obj;
                requestType = msg.arg1;
                trySetupData(apnContext, requestType, null);
                break;
            case DctConstants.EVENT_CLEAN_UP_CONNECTION:
                if (DBG) log("EVENT_CLEAN_UP_CONNECTION");
                cleanUpConnectionInternal(true, RELEASE_TYPE_DETACH, (ApnContext) msg.obj);
                break;
            case DctConstants.EVENT_CLEAN_UP_ALL_CONNECTIONS:
                if ((msg.obj != null) && (msg.obj instanceof String == false)) {
                    msg.obj = null;
                }
                cleanUpAllConnectionsInternal(true, (String) msg.obj);
                break;

            case DctConstants.EVENT_DATA_RAT_CHANGED:
                if (getDataRat() == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) {
                    // unknown rat is an exception for data rat change. It's only received when out
                    // of service and is not applicable for apn bearer bitmask. We should bypass the
                    // check of waiting apn list and keep the data connection on, and no need to
                    // setup a new one.
                    break;
                }
                cleanUpConnectionsOnUpdatedApns(false, Phone.REASON_NW_TYPE_CHANGED);
                //May new Network allow setupData, so try it here
                setupDataOnAllConnectableApns(Phone.REASON_NW_TYPE_CHANGED,
                        RetryFailures.ONLY_ON_CHANGE);
                break;

            case DctConstants.CMD_CLEAR_PROVISIONING_SPINNER:
                // Check message sender intended to clear the current spinner.
                if (mProvisioningSpinner == msg.obj) {
                    mProvisioningSpinner.dismiss();
                    mProvisioningSpinner = null;
                }
                break;

            case DctConstants.EVENT_ENABLE_APN:
                onEnableApn(msg.arg1, msg.arg2, (Message) msg.obj);
                break;

            case DctConstants.EVENT_DISABLE_APN:
                onDisableApn(msg.arg1, msg.arg2);
                break;

            case DctConstants.EVENT_DATA_STALL_ALARM:
                onDataStallAlarm(msg.arg1);
                break;

            case DctConstants.EVENT_ROAMING_OFF:
                onDataRoamingOff();
                break;

            case DctConstants.EVENT_ROAMING_ON:
            case DctConstants.EVENT_ROAMING_SETTING_CHANGE:
                onDataRoamingOnOrSettingsChanged(msg.what);
                break;

            case DctConstants.EVENT_DEVICE_PROVISIONED_CHANGE:
                // Update sharedPreference to false when exits new device provisioning, indicating
                // no users modifications on the settings for new devices. Thus carrier specific
                // default roaming settings can be applied for new devices till user modification.
                final SharedPreferences sp = PreferenceManager
                        .getDefaultSharedPreferences(mPhone.getContext());
                if (!sp.contains(Phone.DATA_ROAMING_IS_USER_SETTING_KEY)) {
                    sp.edit().putBoolean(Phone.DATA_ROAMING_IS_USER_SETTING_KEY, false).commit();
                }
                break;

            case DctConstants.EVENT_NETWORK_STATUS_CHANGED:
                int status = msg.arg1;
                int cid = msg.arg2;
                String url = (String) msg.obj;
                onNetworkStatusChanged(status, cid, url);
                break;

            case DctConstants.EVENT_RADIO_AVAILABLE:
                onRadioAvailable();
                break;

            case DctConstants.EVENT_RADIO_OFF_OR_NOT_AVAILABLE:
                onRadioOffOrNotAvailable();
                break;

            case DctConstants.EVENT_DATA_SETUP_COMPLETE:
                ar = (AsyncResult) msg.obj;
                pair = (Pair<ApnContext, Integer>) ar.userObj;
                apnContext = pair.first;
                generation = pair.second;
                requestType = msg.arg1;
                handoverFailureMode = msg.arg2;
                if (apnContext.getConnectionGeneration() == generation) {
                    boolean success = true;
                    int cause = DataFailCause.UNKNOWN;
                    if (ar.exception != null) {
                        success = false;
                        cause = (int) ar.result;
                    }
                    onDataSetupComplete(apnContext, success, cause, requestType,
                            handoverFailureMode);
                } else {
                    loge("EVENT_DATA_SETUP_COMPLETE: Dropped the event because generation "
                            + "did not match.");
                }
                break;

            case DctConstants.EVENT_DATA_SETUP_COMPLETE_ERROR:
                ar = (AsyncResult) msg.obj;
                pair = (Pair<ApnContext, Integer>) ar.userObj;
                apnContext = pair.first;
                generation = pair.second;
                handoverFailureMode = msg.arg2;
                if (apnContext.getConnectionGeneration() == generation) {
                    onDataSetupCompleteError(apnContext, handoverFailureMode, false);
                } else {
                    loge("EVENT_DATA_SETUP_COMPLETE_ERROR: Dropped the event because generation "
                            + "did not match.");
                }
                break;

            case DctConstants.EVENT_DISCONNECT_DONE:
                log("EVENT_DISCONNECT_DONE msg=" + msg);
                ar = (AsyncResult) msg.obj;
                pair = (Pair<ApnContext, Integer>) ar.userObj;
                apnContext = pair.first;
                generation = pair.second;
                if (apnContext.getConnectionGeneration() == generation) {
                    onDisconnectDone(apnContext);
                } else {
                    loge("EVENT_DISCONNECT_DONE: Dropped the event because generation "
                            + "did not match.");
                }
                break;

            case DctConstants.EVENT_VOICE_CALL_STARTED:
                onVoiceCallStarted();
                break;

            case DctConstants.EVENT_VOICE_CALL_ENDED:
                onVoiceCallEnded();
                break;
            case DctConstants.CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: {
                sEnableFailFastRefCounter += (msg.arg1 == DctConstants.ENABLED) ? 1 : -1;
                if (DBG) {
                    log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: "
                            + " sEnableFailFastRefCounter=" + sEnableFailFastRefCounter);
                }
                if (sEnableFailFastRefCounter < 0) {
                    final String s = "CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: "
                            + "sEnableFailFastRefCounter:" + sEnableFailFastRefCounter + " < 0";
                    loge(s);
                    sEnableFailFastRefCounter = 0;
                }
                final boolean enabled = sEnableFailFastRefCounter > 0;
                if (DBG) {
                    log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: enabled=" + enabled
                            + " sEnableFailFastRefCounter=" + sEnableFailFastRefCounter);
                }
                if (mFailFast != enabled) {
                    mFailFast = enabled;

                    mDataStallNoRxEnabled = !enabled;
                    if (mDsRecoveryHandler.isNoRxDataStallDetectionEnabled()
                            && isAnyDataConnected()
                            && (!mInVoiceCall ||
                                    mPhone.getServiceStateTracker()
                                        .isConcurrentVoiceAndDataAllowed())) {
                        if (DBG) log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: start data stall");
                        stopDataStallAlarm();
                        startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
                    } else {
                        if (DBG) log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: stop data stall");
                        stopDataStallAlarm();
                    }
                }

                break;
            }
            case DctConstants.CMD_ENABLE_MOBILE_PROVISIONING: {
                Bundle bundle = msg.getData();
                if (bundle != null) {
                    try {
                        mProvisioningUrl = (String)bundle.get(DctConstants.PROVISIONING_URL_KEY);
                    } catch(ClassCastException e) {
                        loge("CMD_ENABLE_MOBILE_PROVISIONING: provisioning url not a string" + e);
                        mProvisioningUrl = null;
                    }
                }
                if (TextUtils.isEmpty(mProvisioningUrl)) {
                    loge("CMD_ENABLE_MOBILE_PROVISIONING: provisioning url is empty, ignoring");
                    mIsProvisioning = false;
                    mProvisioningUrl = null;
                } else {
                    loge("CMD_ENABLE_MOBILE_PROVISIONING: provisioningUrl=" + mProvisioningUrl);
                    mIsProvisioning = true;
                    startProvisioningApnAlarm();
                }
                break;
            }
            case DctConstants.EVENT_PROVISIONING_APN_ALARM: {
                if (DBG) log("EVENT_PROVISIONING_APN_ALARM");
                ApnContext apnCtx = mApnContextsByType.get(ApnSetting.TYPE_DEFAULT);
                if (apnCtx.isProvisioningApn() && apnCtx.isConnectedOrConnecting()) {
                    if (mProvisioningApnAlarmTag == msg.arg1) {
                        if (DBG) log("EVENT_PROVISIONING_APN_ALARM: Disconnecting");
                        mIsProvisioning = false;
                        mProvisioningUrl = null;
                        stopProvisioningApnAlarm();
                        cleanUpConnectionInternal(true, RELEASE_TYPE_DETACH, apnCtx);
                    } else {
                        if (DBG) {
                            log("EVENT_PROVISIONING_APN_ALARM: ignore stale tag,"
                                    + " mProvisioningApnAlarmTag:" + mProvisioningApnAlarmTag
                                    + " != arg1:" + msg.arg1);
                        }
                    }
                } else {
                    if (DBG) log("EVENT_PROVISIONING_APN_ALARM: Not connected ignore");
                }
                break;
            }
            case DctConstants.CMD_IS_PROVISIONING_APN: {
                if (DBG) log("CMD_IS_PROVISIONING_APN");
                boolean isProvApn;
                try {
                    String apnType = null;
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        apnType = (String)bundle.get(DctConstants.APN_TYPE_KEY);
                    }
                    if (TextUtils.isEmpty(apnType)) {
                        loge("CMD_IS_PROVISIONING_APN: apnType is empty");
                        isProvApn = false;
                    } else {
                        isProvApn = isProvisioningApn(apnType);
                    }
                } catch (ClassCastException e) {
                    loge("CMD_IS_PROVISIONING_APN: NO provisioning url ignoring");
                    isProvApn = false;
                }
                if (DBG) log("CMD_IS_PROVISIONING_APN: ret=" + isProvApn);
                mReplyAc.replyToMessage(msg, DctConstants.CMD_IS_PROVISIONING_APN,
                        isProvApn ? DctConstants.ENABLED : DctConstants.DISABLED);
                break;
            }
            case DctConstants.EVENT_RESTART_RADIO: {
                restartRadio();
                break;
            }
            case DctConstants.CMD_NET_STAT_POLL: {
                if (msg.arg1 == DctConstants.ENABLED) {
                    handleStartNetStatPoll((DctConstants.Activity)msg.obj);
                } else if (msg.arg1 == DctConstants.DISABLED) {
                    handleStopNetStatPoll((DctConstants.Activity)msg.obj);
                }
                break;
            }
            case DctConstants.EVENT_PCO_DATA_RECEIVED: {
                handlePcoData((AsyncResult)msg.obj);
                break;
            }
            case DctConstants.EVENT_DATA_RECONNECT:
                if (DBG) {
                    log("EVENT_DATA_RECONNECT: subId=" + msg.arg1 + ", type="
                            + requestTypeToString(msg.arg2));
                }
                onDataReconnect((ApnContext) msg.obj, msg.arg1, msg.arg2);
                break;
            case DctConstants.EVENT_DATA_SERVICE_BINDING_CHANGED:
                onDataServiceBindingChanged((Boolean) ((AsyncResult) msg.obj).result);
                break;
            case DctConstants.EVENT_DATA_ENABLED_CHANGED:
                ar = (AsyncResult) msg.obj;
                if (ar.result instanceof Pair) {
                    Pair<Boolean, Integer> p = (Pair<Boolean, Integer>) ar.result;
                    boolean enabled = p.first;
                    int reason = p.second;
                    onDataEnabledChanged(enabled, reason);
                }
                break;
            case DctConstants.EVENT_DATA_ENABLED_OVERRIDE_RULES_CHANGED:
                onDataEnabledOverrideRulesChanged();
                break;
            case DctConstants.EVENT_NR_TIMER_WATCHDOG:
                mWatchdog = false;
                reevaluateUnmeteredConnections();
                break;
            case DctConstants.EVENT_TELEPHONY_DISPLAY_INFO_CHANGED:
                reevaluateCongestedConnections();
                reevaluateUnmeteredConnections();
                break;
            case DctConstants.EVENT_CARRIER_CONFIG_CHANGED:
                onCarrierConfigChanged();
                break;
            case DctConstants.EVENT_SIM_STATE_UPDATED:
                int simState = msg.arg1;
                onSimStateUpdated(simState);
                break;
            case DctConstants.EVENT_APN_UNTHROTTLED:
                ar = (AsyncResult) msg.obj;
                String apn = (String) ar.result;
                onApnUnthrottled(apn);
                break;
            case DctConstants.EVENT_TRAFFIC_DESCRIPTORS_UPDATED:
                onTrafficDescriptorsUpdated();
                break;
            default:
                Rlog.e("DcTracker", "Unhandled event=" + msg);
                break;

        }
    }

    private int getApnProfileID(String apnType) {
        if (TextUtils.equals(apnType, ApnSetting.TYPE_IMS_STRING)) {
            return RILConstants.DATA_PROFILE_IMS;
        } else if (TextUtils.equals(apnType, ApnSetting.TYPE_FOTA_STRING)) {
            return RILConstants.DATA_PROFILE_FOTA;
        } else if (TextUtils.equals(apnType, ApnSetting.TYPE_CBS_STRING)) {
            return RILConstants.DATA_PROFILE_CBS;
        } else if (TextUtils.equals(apnType, ApnSetting.TYPE_IA_STRING)) {
            return RILConstants.DATA_PROFILE_DEFAULT; // DEFAULT for now
        } else if (TextUtils.equals(apnType, ApnSetting.TYPE_DUN_STRING)) {
            return RILConstants.DATA_PROFILE_TETHERED;
        } else {
            return RILConstants.DATA_PROFILE_DEFAULT;
        }
    }

    private int getCellLocationId() {
        int cid = -1;
        CellLocation loc = mPhone.getCurrentCellIdentity().asCellLocation();

        if (loc != null) {
            if (loc instanceof GsmCellLocation) {
                cid = ((GsmCellLocation)loc).getCid();
            } else if (loc instanceof CdmaCellLocation) {
                cid = ((CdmaCellLocation)loc).getBaseStationId();
            }
        }
        return cid;
    }

    /**
     * Update link bandwidth estimate default values from carrier config.
     * @param bandwidths String array of "RAT:upstream,downstream" for each RAT
     * @param useLte For NR NSA, whether to use LTE value for upstream or not
     */
    private void updateLinkBandwidths(String[] bandwidths, boolean useLte) {
        ConcurrentHashMap<String, Pair<Integer, Integer>> temp = new ConcurrentHashMap<>();
        for (String config : bandwidths) {
            int downstream = 14;
            int upstream = 14;
            String[] kv = config.split(":");
            if (kv.length == 2) {
                String[] split = kv[1].split(",");
                if (split.length == 2) {
                    try {
                        downstream = Integer.parseInt(split[0]);
                        upstream = Integer.parseInt(split[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                temp.put(kv[0], new Pair<>(downstream, upstream));
            }
        }
        if (useLte) {
            Pair<Integer, Integer> ltePair =
                    temp.get(DataConfigManager.DATA_CONFIG_NETWORK_TYPE_LTE);
            if (ltePair != null) {
                if (temp.containsKey(DataConfigManager.DATA_CONFIG_NETWORK_TYPE_NR_NSA)) {
                    temp.put(DataConfigManager.DATA_CONFIG_NETWORK_TYPE_NR_NSA, new Pair<>(
                            temp.get(DataConfigManager.DATA_CONFIG_NETWORK_TYPE_NR_NSA).first,
                            ltePair.second));
                }
                if (temp.containsKey(DataConfigManager.DATA_CONFIG_NETWORK_TYPE_NR_NSA_MMWAVE)) {
                    temp.put(DataConfigManager.DATA_CONFIG_NETWORK_TYPE_NR_NSA_MMWAVE, new Pair<>(
                            temp.get(DataConfigManager.DATA_CONFIG_NETWORK_TYPE_NR_NSA_MMWAVE)
                                    .first, ltePair.second));
                }
            }
        }
        mBandwidths = temp;
        for (DataConnection dc : mDataConnections.values()) {
            dc.sendMessage(DataConnection.EVENT_CARRIER_CONFIG_LINK_BANDWIDTHS_CHANGED);
        }
    }

    /**
     * Return the link upstream/downstream values from CarrierConfig for the given RAT name.
     * @param ratName RAT name from ServiceState#rilRadioTechnologyToString.
     * @return pair of downstream/upstream values (kbps), or null if the config is not defined.
     */
    public Pair<Integer, Integer> getLinkBandwidthsFromCarrierConfig(String ratName) {
        return mBandwidths.get(ratName);
    }

    @VisibleForTesting
    public boolean shouldAutoAttach() {
        if (mAutoAttachEnabled.get()) return true;

        PhoneSwitcher phoneSwitcher = PhoneSwitcher.getInstance();
        ServiceState serviceState = mPhone.getServiceState();

        if (phoneSwitcher == null || serviceState == null) return false;

        // If voice is also not in service, don't auto attach.
        if (serviceState.getState() != ServiceState.STATE_IN_SERVICE) return false;

        // If voice is on LTE or NR, don't auto attach as for LTE / NR data would be attached.
        if (serviceState.getVoiceNetworkType() == NETWORK_TYPE_LTE
                || serviceState.getVoiceNetworkType() == NETWORK_TYPE_NR) return false;

        // If phone is non default phone, modem may have detached from data for optimization.
        // If phone is in voice call, for DSDS case DDS switch may be limited so we do try our
        // best to setup data connection and allow auto-attach.
        return (mPhone.getPhoneId() != phoneSwitcher.getPreferredDataPhoneId()
                || mPhone.getState() != PhoneConstants.State.IDLE);
    }

    private void notifyAllDataDisconnected() {
        sEnableFailFastRefCounter = 0;
        mFailFast = false;
        log("notify all data disconnected");
        mAllDataDisconnectedRegistrants.notifyRegistrants();
    }

    public void registerForAllDataDisconnected(Handler h, int what) {
        mAllDataDisconnectedRegistrants.addUnique(h, what, null);

        if (areAllDataDisconnected()) {
            notifyAllDataDisconnected();
        }
    }

    public void unregisterForAllDataDisconnected(Handler h) {
        mAllDataDisconnectedRegistrants.remove(h);
    }

    private void onDataEnabledChanged(boolean enable,
                                      @DataEnabledChangedReason int enabledChangedReason) {
        if (DBG) {
            log("onDataEnabledChanged: enable=" + enable + ", enabledChangedReason="
                    + enabledChangedReason);
        }

        if (enable) {
            reevaluateDataConnections();
            setupDataOnAllConnectableApns(Phone.REASON_DATA_ENABLED, RetryFailures.ALWAYS);
        } else {
            String cleanupReason;
            switch (enabledChangedReason) {
                case DataEnabledSettings.REASON_INTERNAL_DATA_ENABLED:
                    cleanupReason = Phone.REASON_DATA_DISABLED_INTERNAL;
                    break;
                case DataEnabledSettings.REASON_DATA_ENABLED_BY_CARRIER:
                    cleanupReason = Phone.REASON_CARRIER_ACTION_DISABLE_METERED_APN;
                    break;
                case DataEnabledSettings.REASON_USER_DATA_ENABLED:
                case DataEnabledSettings.REASON_POLICY_DATA_ENABLED:
                case DataEnabledSettings.REASON_PROVISIONED_CHANGED:
                case DataEnabledSettings.REASON_PROVISIONING_DATA_ENABLED_CHANGED:
                default:
                    cleanupReason = Phone.REASON_DATA_SPECIFIC_DISABLED;
                    break;

            }
            cleanUpAllConnectionsInternal(true, cleanupReason);
        }
    }

    private void reevaluateCongestedConnections() {
        log("reevaluateCongestedConnections");
        int rat = mPhone.getDisplayInfoController().getTelephonyDisplayInfo().getNetworkType();
        // congested override and either network is specified or unknown and all networks specified
        boolean isCongested = mCongestedOverride && (mCongestedNetworkTypes.contains(rat)
                || mCongestedNetworkTypes.containsAll(Arrays.stream(
                TelephonyManager.getAllNetworkTypes()).boxed().collect(Collectors.toSet())));
        for (DataConnection dataConnection : mDataConnections.values()) {
            dataConnection.onCongestednessChanged(isCongested);
        }
    }

    private void reevaluateUnmeteredConnections() {
        log("reevaluateUnmeteredConnections");
        int rat = mPhone.getDisplayInfoController().getTelephonyDisplayInfo().getNetworkType();
        if (isNrUnmetered() && (!mPhone.getServiceState().getRoaming() || mNrNsaRoamingUnmetered)) {
            setDataConnectionUnmetered(true);
            if (!mWatchdog) {
                startWatchdogAlarm();
            }
        } else {
            stopWatchdogAlarm();
            setDataConnectionUnmetered(isNetworkTypeUnmetered(rat));
        }
    }

    private void setDataConnectionUnmetered(boolean isUnmetered) {
        if (!isUnmetered || isTempNotMeteredSupportedByCarrier()) {
            for (DataConnection dataConnection : mDataConnections.values()) {
                dataConnection.onMeterednessChanged(isUnmetered);
            }
        }
    }

    private boolean isNetworkTypeUnmetered(@NetworkType int networkType) {
        boolean isUnmetered;
        if (mUnmeteredNetworkTypes == null || !mUnmeteredOverride) {
            // check SubscriptionPlans if override is not defined
            isUnmetered = isNetworkTypeUnmeteredViaSubscriptionPlan(networkType);
            log("isNetworkTypeUnmeteredViaSubscriptionPlan: networkType=" + networkType
                    + ", isUnmetered=" + isUnmetered);
            return isUnmetered;
        }
        // unmetered override and either network is specified or unknown and all networks specified
        isUnmetered = mUnmeteredNetworkTypes.contains(networkType)
                || mUnmeteredNetworkTypes.containsAll(Arrays.stream(
                TelephonyManager.getAllNetworkTypes()).boxed().collect(Collectors.toSet()));
        if (DBG) {
            log("isNetworkTypeUnmetered: networkType=" + networkType
                    + ", isUnmetered=" + isUnmetered);
        }
        return isUnmetered;
    }

    private boolean isNetworkTypeUnmeteredViaSubscriptionPlan(@NetworkType int networkType) {
        if (mSubscriptionPlans.isEmpty()) {
            // safe return false if unable to get subscription plans or plans don't exist
            return false;
        }

        boolean isGeneralUnmetered = true;
        Set<Integer> allNetworkTypes = Arrays.stream(TelephonyManager.getAllNetworkTypes())
                .boxed().collect(Collectors.toSet());
        for (SubscriptionPlan plan : mSubscriptionPlans) {
            // check plan is general (applies to all network types) or specific
            if (Arrays.stream(plan.getNetworkTypes()).boxed().collect(Collectors.toSet())
                    .containsAll(allNetworkTypes)) {
                if (!isPlanUnmetered(plan)) {
                    // metered takes precedence over unmetered for safety
                    isGeneralUnmetered = false;
                }
            } else {
                // check plan applies to given network type
                if (networkType != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                    for (int planNetworkType : plan.getNetworkTypes()) {
                        if (planNetworkType == networkType) {
                            return isPlanUnmetered(plan);
                        }
                    }
                }
            }
        }
        return isGeneralUnmetered;
    }

    private boolean isPlanUnmetered(SubscriptionPlan plan) {
        return plan.getDataLimitBytes() == SubscriptionPlan.BYTES_UNLIMITED;
    }

    private boolean isNrUnmetered() {
        int rat = mPhone.getDisplayInfoController().getTelephonyDisplayInfo().getNetworkType();
        int override = mPhone.getDisplayInfoController().getTelephonyDisplayInfo()
                .getOverrideNetworkType();

        if (isNetworkTypeUnmetered(NETWORK_TYPE_NR)) {
            if (mNrNsaMmwaveUnmetered) {
                if (override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED) {
                    if (DBG) log("NR unmetered for mmwave only");
                    return true;
                }
                return false;
            } else if (mNrNsaSub6Unmetered) {
                if (override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA) {
                    if (DBG) log("NR unmetered for sub6 only");
                    return true;
                }
                return false;
            }
            if (override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED
                    || override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA
                    || rat == NETWORK_TYPE_NR) {
                if (DBG) log("NR unmetered for all frequencies");
                return true;
            }
            return false;
        }

        if (mNrNsaAllUnmetered) {
            if (mNrNsaMmwaveUnmetered) {
                if (override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED) {
                    if (DBG) log("NR NSA unmetered for mmwave only via carrier configs");
                    return true;
                }
                return false;
            } else if (mNrNsaSub6Unmetered) {
                if (override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA) {
                    if (DBG) log("NR NSA unmetered for sub6 only via carrier configs");
                    return true;
                }
                return false;
            }
            if (override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED
                    || override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA) {
                if (DBG) log("NR NSA unmetered for all frequencies via carrier configs");
                return true;
            }
            return false;
        }

        if (mNrSaAllUnmetered) {
            // TODO: add logic for mNrSaMmwaveUnmetered and mNrSaSub6Unmetered once it's defined
            // in TelephonyDisplayInfo
            if (rat == NETWORK_TYPE_NR) {
                if (DBG) log("NR SA unmetered for all frequencies via carrier configs");
                return true;
            }
            return false;
        }

        return false;
    }

    private boolean isTempNotMeteredSupportedByCarrier() {
        CarrierConfigManager configManager =
                mPhone.getContext().getSystemService(CarrierConfigManager.class);
        if (configManager != null) {
            PersistableBundle bundle = configManager.getConfigForSubId(mPhone.getSubId());
            if (bundle != null) {
                return bundle.getBoolean(
                        CarrierConfigManager.KEY_NETWORK_TEMP_NOT_METERED_SUPPORTED_BOOL);
            }
        }

        return false;
    }

    protected void log(String s) {
        Rlog.d(mLogTag, s);
    }

    private void loge(String s) {
        Rlog.e(mLogTag, s);
    }

    private void logSortedApnContexts() {
        if (VDBG) {
            log("initApnContexts: X mApnContexts=" + mApnContexts);

            StringBuilder sb = new StringBuilder();
            sb.append("sorted apncontexts -> [");
            for (ApnContext apnContext : mPrioritySortedApnContexts) {
                sb.append(apnContext);
                sb.append(", ");

                log("sorted list");
            }
            sb.append("]");
            log(sb.toString());
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("DcTracker:");
        pw.println(" RADIO_TESTS=" + RADIO_TESTS);
        pw.println(" mDataEnabledSettings=" + mDataEnabledSettings);
        pw.println(" isDataAllowed=" + isDataAllowed(null));
        pw.flush();
        pw.println(" mRequestedApnType=" + mRequestedApnType);
        pw.println(" mPhone=" + mPhone.getPhoneName());
        pw.println(" mConfigReady=" + mConfigReady);
        pw.println(" mSimState=" + SubscriptionInfoUpdater.simStateString(mSimState));
        pw.println(" mActivity=" + mActivity);
        pw.println(" mState=" + mState);
        pw.println(" mTxPkts=" + mTxPkts);
        pw.println(" mRxPkts=" + mRxPkts);
        pw.println(" mNetStatPollPeriod=" + mNetStatPollPeriod);
        pw.println(" mNetStatPollEnabled=" + mNetStatPollEnabled);
        pw.println(" mDataStallTxRxSum=" + mDataStallTxRxSum);
        pw.println(" mDataStallAlarmTag=" + mDataStallAlarmTag);
        pw.println(" mDataStallNoRxEnabled=" + mDataStallNoRxEnabled);
        pw.println(" mEmergencyApn=" + mEmergencyApn);
        pw.println(" mSentSinceLastRecv=" + mSentSinceLastRecv);
        pw.println(" mNoRecvPollCount=" + mNoRecvPollCount);
        pw.println(" mResolver=" + mResolver);
        pw.println(" mReconnectIntent=" + mReconnectIntent);
        pw.println(" mAutoAttachEnabled=" + mAutoAttachEnabled.get());
        pw.println(" mIsScreenOn=" + mIsScreenOn);
        pw.println(" mUniqueIdGenerator=" + mUniqueIdGenerator);
        pw.println(" mDataServiceBound=" + mDataServiceBound);
        pw.println(" mDataRoamingLeakageLog= ");
        mDataRoamingLeakageLog.dump(fd, pw, args);
        pw.println(" mApnSettingsInitializationLog= ");
        mApnSettingsInitializationLog.dump(fd, pw, args);
        pw.flush();
        pw.println(" ***************************************");
        DcController dcc = mDcc;
        if (dcc != null) {
            if (mDataServiceBound) {
                dcc.dump(fd, pw, args);
            } else {
                pw.println(" Can't dump mDcc because data service is not bound.");
            }
        } else {
            pw.println(" mDcc=null");
        }
        pw.println(" ***************************************");
        HashMap<Integer, DataConnection> dcs = mDataConnections;
        if (dcs != null) {
            Set<Entry<Integer, DataConnection> > mDcSet = mDataConnections.entrySet();
            pw.println(" mDataConnections: count=" + mDcSet.size());
            for (Entry<Integer, DataConnection> entry : mDcSet) {
                pw.printf(" *** mDataConnection[%d] \n", entry.getKey());
                entry.getValue().dump(fd, pw, args);
            }
        } else {
            pw.println("mDataConnections=null");
        }
        pw.println(" ***************************************");
        pw.flush();
        HashMap<String, Integer> apnToDcId = mApnToDataConnectionId;
        if (apnToDcId != null) {
            Set<Entry<String, Integer>> apnToDcIdSet = apnToDcId.entrySet();
            pw.println(" mApnToDataConnectonId size=" + apnToDcIdSet.size());
            for (Entry<String, Integer> entry : apnToDcIdSet) {
                pw.printf(" mApnToDataConnectonId[%s]=%d\n", entry.getKey(), entry.getValue());
            }
        } else {
            pw.println("mApnToDataConnectionId=null");
        }
        pw.println(" ***************************************");
        pw.flush();
        ConcurrentHashMap<String, ApnContext> apnCtxs = mApnContexts;
        if (apnCtxs != null) {
            Set<Entry<String, ApnContext>> apnCtxsSet = apnCtxs.entrySet();
            pw.println(" mApnContexts size=" + apnCtxsSet.size());
            for (Entry<String, ApnContext> entry : apnCtxsSet) {
                entry.getValue().dump(fd, pw, args);
            }
            ApnContext.dumpLocalLog(fd, pw, args);
            pw.println(" ***************************************");
        } else {
            pw.println(" mApnContexts=null");
        }
        pw.flush();

        pw.println(" mAllApnSettings size=" + mAllApnSettings.size());
        for (int i = 0; i < mAllApnSettings.size(); i++) {
            pw.printf(" mAllApnSettings[%d]: %s\n", i, mAllApnSettings.get(i));
        }
        pw.flush();

        pw.println(" mPreferredApn=" + mPreferredApn);
        pw.println(" mIsPsRestricted=" + mIsPsRestricted);
        pw.println(" mIsDisposed=" + mIsDisposed);
        pw.println(" mIntentReceiver=" + mIntentReceiver);
        pw.println(" mReregisterOnReconnectFailure=" + mReregisterOnReconnectFailure);
        pw.println(" canSetPreferApn=" + mCanSetPreferApn);
        pw.println(" mApnObserver=" + mApnObserver);
        pw.println(" isAnyDataConnected=" + isAnyDataConnected());
        pw.println(" mAttached=" + mAttached.get());
        mDataEnabledSettings.dump(fd, pw, args);
        pw.flush();
    }

    public String[] getPcscfAddress(String apnType) {
        log("getPcscfAddress()");
        ApnContext apnContext = null;

        if(apnType == null){
            log("apnType is null, return null");
            return null;
        }

        if (TextUtils.equals(apnType, ApnSetting.TYPE_EMERGENCY_STRING)) {
            apnContext = mApnContextsByType.get(ApnSetting.TYPE_EMERGENCY);
        } else if (TextUtils.equals(apnType, ApnSetting.TYPE_IMS_STRING)) {
            apnContext = mApnContextsByType.get(ApnSetting.TYPE_IMS);
        } else {
            log("apnType is invalid, return null");
            return null;
        }

        if (apnContext == null) {
            log("apnContext is null, return null");
            return null;
        }

        DataConnection dataConnection = apnContext.getDataConnection();
        String[] result = null;

        if (dataConnection != null) {
            result = dataConnection.getPcscfAddresses();

            if (result != null) {
                for (int i = 0; i < result.length; i++) {
                    log("Pcscf[" + i + "]: " + result[i]);
                }
            }
            return result;
        }
        return null;
    }

    /**
     * Create default apn settings for the apn type like emergency, and ims
     */
    private ApnSetting buildDefaultApnSetting(@NonNull String entry,
            @NonNull String apn, @ApnType int apnTypeBitmask) {
        return new ApnSetting.Builder()
                .setEntryName(entry)
                .setProtocol(ApnSetting.PROTOCOL_IPV4V6)
                .setRoamingProtocol(ApnSetting.PROTOCOL_IPV4V6)
                .setApnName(apn)
                .setApnTypeBitmask(apnTypeBitmask)
                .setCarrierEnabled(true)
                .setApnSetId(Telephony.Carriers.MATCH_ALL_APN_SET_ID)
                .build();
    }

    /**
     * Add default APN settings to APN settings list as needed
     */
    private void addDefaultApnSettingsAsNeeded() {
        boolean isEmergencyApnConfigured = false;
        boolean isImsApnConfigured = false;

        for (ApnSetting apn : mAllApnSettings) {
            if (apn.canHandleType(ApnSetting.TYPE_EMERGENCY)) {
                isEmergencyApnConfigured = true;
            }
            if (apn.canHandleType(ApnSetting.TYPE_IMS)) {
                isImsApnConfigured = true;
            }
            if (isEmergencyApnConfigured && isImsApnConfigured) {
                log("Both emergency and ims apn setting are already present");
                return;
            }
        }

        // Add default apn setting for emergency service if it is not present
        if (!isEmergencyApnConfigured) {
            mAllApnSettings.add(buildDefaultApnSetting(
                    "DEFAULT EIMS", "sos", ApnSetting.TYPE_EMERGENCY));
            log("default emergency apn is created");
        }

        // Only add default apn setting for ims when it is not present and sim is loaded
        if (!isImsApnConfigured && mSimState == TelephonyManager.SIM_STATE_LOADED) {
            mAllApnSettings.add(buildDefaultApnSetting(
                    "DEFAULT IMS", "ims", ApnSetting.TYPE_IMS));
            log("default ims apn is created");
        }
    }

    private void cleanUpConnectionsOnUpdatedApns(boolean detach, String reason) {
        if (DBG) log("cleanUpConnectionsOnUpdatedApns: detach=" + detach);
        if (mAllApnSettings.isEmpty()) {
            cleanUpAllConnectionsInternal(detach, Phone.REASON_APN_CHANGED);
        } else {
            if (getDataRat() == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) {
                // unknown rat is an exception for data rat change. Its only received when out of
                // service and is not applicable for apn bearer bitmask. We should bypass the check
                // of waiting apn list and keep the data connection on.
                return;
            }
            for (ApnContext apnContext : mApnContexts.values()) {
                boolean cleanupRequired = true;
                if (!apnContext.isDisconnected()) {
                    ArrayList<ApnSetting> waitingApns = buildWaitingApns(
                            apnContext.getApnType(), getDataRat());
                    if (apnContext.getWaitingApns().size() != waitingApns.size()
                            || !apnContext.getWaitingApns().containsAll(waitingApns)) {
                        apnContext.setWaitingApns(waitingApns);
                    }
                    for (ApnSetting apnSetting : waitingApns) {
                        if (areCompatible(apnSetting, apnContext.getApnSetting())) {
                            cleanupRequired = false;
                            break;
                        }
                    }

                    if (cleanupRequired) {
                        if (DBG) {
                            log("cleanUpConnectionsOnUpdatedApns: APN type "
                                    + apnContext.getApnType() + " clean up is required. The new "
                                    + "waiting APN list " + waitingApns + " does not cover "
                                    + apnContext.getApnSetting());
                        }
                        apnContext.setReason(reason);
                        cleanUpConnectionInternal(true, RELEASE_TYPE_DETACH, apnContext);
                    }
                }
            }
        }

        if (!isAnyDataConnected()) {
            stopNetStatPoll();
            stopDataStallAlarm();
        }

        mRequestedApnType = ApnSetting.TYPE_DEFAULT;

        if (areAllDataDisconnected()) {
            notifyAllDataDisconnected();
        }
    }

    /**
     * Polling stuff
     */
    protected void resetPollStats() {
        mTxPkts = -1;
        mRxPkts = -1;
        mNetStatPollPeriod = POLL_NETSTAT_MILLIS;
    }

    protected void startNetStatPoll() {
        if (isAnyDataConnected() && !mNetStatPollEnabled) {
            if (DBG) {
                log("startNetStatPoll");
            }
            resetPollStats();
            mNetStatPollEnabled = true;
            mPollNetStat.run();
        }
        if (mPhone != null) {
            mPhone.notifyDataActivity();
        }
    }

    protected void stopNetStatPoll() {
        mNetStatPollEnabled = false;
        removeCallbacks(mPollNetStat);
        if (DBG) {
            log("stopNetStatPoll");
        }

        // To sync data activity icon in the case of switching data connection to send MMS.
        if (mPhone != null) {
            mPhone.notifyDataActivity();
        }
    }

    public void sendStartNetStatPoll(DctConstants.Activity activity) {
        Message msg = obtainMessage(DctConstants.CMD_NET_STAT_POLL);
        msg.arg1 = DctConstants.ENABLED;
        msg.obj = activity;
        sendMessage(msg);
    }

    private void handleStartNetStatPoll(DctConstants.Activity activity) {
        startNetStatPoll();
        startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
        setActivity(activity);
    }

    public void sendStopNetStatPoll(DctConstants.Activity activity) {
        Message msg = obtainMessage(DctConstants.CMD_NET_STAT_POLL);
        msg.arg1 = DctConstants.DISABLED;
        msg.obj = activity;
        sendMessage(msg);
    }

    private void handleStopNetStatPoll(DctConstants.Activity activity) {
        stopNetStatPoll();
        stopDataStallAlarm();
        setActivity(activity);
    }

    private void onDataEnabledOverrideRulesChanged() {
        if (DBG) {
            log("onDataEnabledOverrideRulesChanged");
        }

        for (ApnContext apnContext : mPrioritySortedApnContexts) {
            if (isDataAllowed(apnContext, REQUEST_TYPE_NORMAL, null)) {
                if (apnContext.getDataConnection() != null) {
                    apnContext.getDataConnection().reevaluateRestrictedState();
                }
                setupDataOnConnectableApn(apnContext, Phone.REASON_DATA_ENABLED_OVERRIDE,
                        RetryFailures.ALWAYS);
            } else if (shouldCleanUpConnection(apnContext, true, false)) {
                apnContext.setReason(Phone.REASON_DATA_ENABLED_OVERRIDE);
                cleanUpConnectionInternal(true, RELEASE_TYPE_DETACH, apnContext);
            }
        }
    }

    private void updateDataActivity() {
        long sent, received;

        DctConstants.Activity newActivity;

        TxRxSum preTxRxSum = new TxRxSum(mTxPkts, mRxPkts);
        TxRxSum curTxRxSum = new TxRxSum();
        curTxRxSum.updateTotalTxRxSum();
        mTxPkts = curTxRxSum.txPkts;
        mRxPkts = curTxRxSum.rxPkts;

        if (VDBG) {
            log("updateDataActivity: curTxRxSum=" + curTxRxSum + " preTxRxSum=" + preTxRxSum);
        }

        if (mNetStatPollEnabled && (preTxRxSum.txPkts > 0 || preTxRxSum.rxPkts > 0)) {
            sent = mTxPkts - preTxRxSum.txPkts;
            received = mRxPkts - preTxRxSum.rxPkts;

            if (VDBG)
                log("updateDataActivity: sent=" + sent + " received=" + received);
            if (sent > 0 && received > 0) {
                newActivity = DctConstants.Activity.DATAINANDOUT;
            } else if (sent > 0 && received == 0) {
                newActivity = DctConstants.Activity.DATAOUT;
            } else if (sent == 0 && received > 0) {
                newActivity = DctConstants.Activity.DATAIN;
            } else {
                newActivity = (mActivity == DctConstants.Activity.DORMANT) ?
                        mActivity : DctConstants.Activity.NONE;
            }

            if (mActivity != newActivity && mIsScreenOn) {
                if (VDBG)
                    log("updateDataActivity: newActivity=" + newActivity);
                mActivity = newActivity;
                mPhone.notifyDataActivity();
            }
        }
    }

    private void handlePcoData(AsyncResult ar) {
        if (ar.exception != null) {
            loge("PCO_DATA exception: " + ar.exception);
            return;
        }
        PcoData pcoData = (PcoData)(ar.result);
        ArrayList<DataConnection> dcList = new ArrayList<>();
        DataConnection temp = mDcc.getActiveDcByCid(pcoData.cid);
        if (temp != null) {
            dcList.add(temp);
        }
        if (dcList.size() == 0) {
            loge("PCO_DATA for unknown cid: " + pcoData.cid + ", inferring");
            for (DataConnection dc : mDataConnections.values()) {
                final int cid = dc.getCid();
                if (cid == pcoData.cid) {
                    if (VDBG) log("  found " + dc);
                    dcList.clear();
                    dcList.add(dc);
                    break;
                }
                // check if this dc is still connecting
                if (cid == -1) {
                    for (ApnContext apnContext : dc.getApnContexts()) {
                        if (apnContext.getState() == DctConstants.State.CONNECTING) {
                            if (VDBG) log("  found potential " + dc);
                            dcList.add(dc);
                            break;
                        }
                    }
                }
            }
        }
        if (dcList.size() == 0) {
            loge("PCO_DATA - couldn't infer cid");
            return;
        }
        for (DataConnection dc : dcList) {
            List<ApnContext> apnContextList = dc.getApnContexts();
            if (apnContextList.size() == 0) {
                break;
            }
            // send one out for each apn type in play
            for (ApnContext apnContext : apnContextList) {
                String apnType = apnContext.getApnType();

                final Intent intent = new Intent(TelephonyManager.ACTION_CARRIER_SIGNAL_PCO_VALUE);
                intent.putExtra(TelephonyManager.EXTRA_APN_TYPE,
                        ApnSetting.getApnTypesBitmaskFromString(apnType));
                intent.putExtra(TelephonyManager.EXTRA_APN_PROTOCOL,
                        ApnSetting.getProtocolIntFromString(pcoData.bearerProto));
                intent.putExtra(TelephonyManager.EXTRA_PCO_ID, pcoData.pcoId);
                intent.putExtra(TelephonyManager.EXTRA_PCO_VALUE, pcoData.contents);
                mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);
            }
        }
    }

    /**
     * Data-Stall
     */

    // Recovery action taken in case of data stall
    @IntDef(
        value = {
            RECOVERY_ACTION_GET_DATA_CALL_LIST,
            RECOVERY_ACTION_CLEANUP,
            RECOVERY_ACTION_REREGISTER,
            RECOVERY_ACTION_RADIO_RESTART
        })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecoveryAction {};
    private static final int RECOVERY_ACTION_GET_DATA_CALL_LIST      = 0;
    private static final int RECOVERY_ACTION_CLEANUP                 = 1;
    private static final int RECOVERY_ACTION_REREGISTER              = 2;
    private static final int RECOVERY_ACTION_RADIO_RESTART           = 3;

    // Recovery handler class for cellular data stall
    private class DataStallRecoveryHandler {
        // Default minimum duration between each recovery steps
        private static final int
                DEFAULT_MIN_DURATION_BETWEEN_RECOVERY_STEPS_IN_MS = (3 * 60 * 1000); // 3 mins

        // The elapsed real time of last recovery attempted
        private long mTimeLastRecoveryStartMs;
        // Whether current network good or not
        private boolean mIsValidNetwork;
        // Whether data stall happened or not.
        private boolean mWasDataStall;
        // Whether the result of last action(RADIO_RESTART) reported.
        private boolean mLastActionReported;
        // The real time for data stall start.
        private long mDataStallStartMs;
        // Last data stall action.
        private @RecoveryAction int mLastAction;

        public DataStallRecoveryHandler() {
            reset();
        }

        public void reset() {
            mTimeLastRecoveryStartMs = 0;
            putRecoveryAction(RECOVERY_ACTION_GET_DATA_CALL_LIST);
        }

        private void setNetworkValidationState(boolean isValid) {
            // Validation status is true and was not data stall.
            if (isValid && !mWasDataStall) {
                return;
            }

            if (!mWasDataStall) {
                mWasDataStall = true;
                mDataStallStartMs = SystemClock.elapsedRealtime();
                if (DBG) log("data stall: start time = " + mDataStallStartMs);
                return;
            }

            if (!mLastActionReported) {
                int timeDuration = (int) (SystemClock.elapsedRealtime() - mDataStallStartMs);
                if (DBG) {
                    log("data stall: lastaction = " + mLastAction + ", isRecovered = "
                            + isValid + ", mTimeDuration = " + timeDuration);
                }
                DataStallRecoveryStats.onDataStallEvent(mLastAction, mPhone, isValid,
                                                        timeDuration);
                mLastActionReported = true;
            }

            if (isValid) {
                mLastActionReported = false;
                mWasDataStall = false;
            }
        }

        public boolean isAggressiveRecovery() {
            @RecoveryAction int action = getRecoveryAction();

            return ((action == RECOVERY_ACTION_CLEANUP)
                    || (action == RECOVERY_ACTION_REREGISTER)
                    || (action == RECOVERY_ACTION_RADIO_RESTART));
        }

        private long getMinDurationBetweenRecovery() {
            return Settings.Global.getLong(mResolver,
                Settings.Global.MIN_DURATION_BETWEEN_RECOVERY_STEPS_IN_MS,
                DEFAULT_MIN_DURATION_BETWEEN_RECOVERY_STEPS_IN_MS);
        }

        private long getElapsedTimeSinceRecoveryMs() {
            return (SystemClock.elapsedRealtime() - mTimeLastRecoveryStartMs);
        }

        @RecoveryAction
        private int getRecoveryAction() {
            @RecoveryAction int action = Settings.System.getInt(mResolver,
                    "radio.data.stall.recovery.action", RECOVERY_ACTION_GET_DATA_CALL_LIST);
            if (VDBG_STALL) log("getRecoveryAction: " + action);
            return action;
        }

        private void putRecoveryAction(@RecoveryAction int action) {
            Settings.System.putInt(mResolver, "radio.data.stall.recovery.action", action);
            if (VDBG_STALL) log("putRecoveryAction: " + action);
        }

        private void broadcastDataStallDetected(@RecoveryAction int recoveryAction) {
            Intent intent = new Intent(TelephonyManager.ACTION_DATA_STALL_DETECTED);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
            intent.putExtra(TelephonyManager.EXTRA_RECOVERY_ACTION, recoveryAction);
            mPhone.getContext().sendBroadcast(intent, READ_PRIVILEGED_PHONE_STATE);
        }

        private boolean isRecoveryAlreadyStarted() {
            return getRecoveryAction() != RECOVERY_ACTION_GET_DATA_CALL_LIST;
        }

        private boolean checkRecovery() {
            // To avoid back to back recovery wait for a grace period
            if (getElapsedTimeSinceRecoveryMs() < getMinDurationBetweenRecovery()) {
                if (VDBG_STALL) log("skip back to back data stall recovery");
                return false;
            }

            // Skip recovery if it can cause a call to drop
            if (mPhone.getState() != PhoneConstants.State.IDLE
                    && getRecoveryAction() > RECOVERY_ACTION_CLEANUP) {
                if (VDBG_STALL) log("skip data stall recovery as there is an active call");
                return false;
            }

            // Allow recovery if data is expected to work
            return mAttached.get() && isDataAllowed(null);
        }

        private void triggerRecovery() {
            // Updating the recovery start time early to avoid race when
            // the message is being processed in the Queue
            mTimeLastRecoveryStartMs = SystemClock.elapsedRealtime();
            sendMessage(obtainMessage(DctConstants.EVENT_DO_RECOVERY));
        }

        public void doRecovery() {
            if (isAnyDataConnected()) {
                // Go through a series of recovery steps, each action transitions to the next action
                @RecoveryAction final int recoveryAction = getRecoveryAction();
                final int signalStrength = mPhone.getSignalStrength().getLevel();
                TelephonyMetrics.getInstance().writeSignalStrengthEvent(
                        mPhone.getPhoneId(), signalStrength);
                TelephonyMetrics.getInstance().writeDataStallEvent(
                        mPhone.getPhoneId(), recoveryAction);
                mLastAction = recoveryAction;
                mLastActionReported = false;
                broadcastDataStallDetected(recoveryAction);

                switch (recoveryAction) {
                    case RECOVERY_ACTION_GET_DATA_CALL_LIST:
                        EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_GET_DATA_CALL_LIST,
                            mSentSinceLastRecv);
                        if (DBG) log("doRecovery() get data call list");
                        mDataServiceManager.requestDataCallList(obtainMessage());
                        putRecoveryAction(RECOVERY_ACTION_CLEANUP);
                        break;
                    case RECOVERY_ACTION_CLEANUP:
                        EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_CLEANUP,
                            mSentSinceLastRecv);
                        if (DBG) log("doRecovery() cleanup all connections");
                        cleanUpConnection(mApnContexts.get(ApnSetting.getApnTypeString(
                                ApnSetting.TYPE_DEFAULT)));
                        cleanUpConnection(mApnContexts.get(ApnSetting.getApnTypeString(
                                ApnSetting.TYPE_ENTERPRISE)));
                        putRecoveryAction(RECOVERY_ACTION_REREGISTER);
                        break;
                    case RECOVERY_ACTION_REREGISTER:
                        EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_REREGISTER,
                            mSentSinceLastRecv);
                        if (DBG) log("doRecovery() re-register");
                        mPhone.getServiceStateTracker().reRegisterNetwork(null);
                        putRecoveryAction(RECOVERY_ACTION_RADIO_RESTART);
                        break;
                    case RECOVERY_ACTION_RADIO_RESTART:
                        EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_RADIO_RESTART,
                            mSentSinceLastRecv);
                        if (DBG) log("restarting radio");
                        restartRadio();
                        reset();
                        break;
                    default:
                        throw new RuntimeException("doRecovery: Invalid recoveryAction="
                            + recoveryAction);
                }
                mSentSinceLastRecv = 0;
            }
        }

        public void processNetworkStatusChanged(boolean isValid) {
            setNetworkValidationState(isValid);
            if (isValid) {
                mIsValidNetwork = true;
                reset();
            } else {
                if (mIsValidNetwork || isRecoveryAlreadyStarted()) {
                    mIsValidNetwork = false;
                    // Check and trigger a recovery if network switched from good
                    // to bad or recovery is already started before.
                    if (checkRecovery()) {
                        if (DBG) log("trigger data stall recovery");
                        triggerRecovery();
                    }
                }
            }
        }

        public boolean isRecoveryOnBadNetworkEnabled() {
            return Settings.Global.getInt(mResolver,
                    Settings.Global.DATA_STALL_RECOVERY_ON_BAD_NETWORK, 1) == 1;
        }

        public boolean isNoRxDataStallDetectionEnabled() {
            return mDataStallNoRxEnabled && !isRecoveryOnBadNetworkEnabled();
        }
    }

    private void updateDataStallInfo() {
        long sent, received;

        TxRxSum preTxRxSum = new TxRxSum(mDataStallTxRxSum);
        mDataStallTxRxSum.updateTotalTxRxSum();

        if (VDBG_STALL) {
            log("updateDataStallInfo: mDataStallTxRxSum=" + mDataStallTxRxSum +
                    " preTxRxSum=" + preTxRxSum);
        }

        sent = mDataStallTxRxSum.txPkts - preTxRxSum.txPkts;
        received = mDataStallTxRxSum.rxPkts - preTxRxSum.rxPkts;

        if (RADIO_TESTS) {
            if (SystemProperties.getBoolean("radio.test.data.stall", false)) {
                log("updateDataStallInfo: radio.test.data.stall true received = 0;");
                received = 0;
            }
        }
        if ( sent > 0 && received > 0 ) {
            if (VDBG_STALL) log("updateDataStallInfo: IN/OUT");
            mSentSinceLastRecv = 0;
            mDsRecoveryHandler.reset();
        } else if (sent > 0 && received == 0) {
            if (isPhoneStateIdle()) {
                mSentSinceLastRecv += sent;
            } else {
                mSentSinceLastRecv = 0;
            }
            if (DBG) {
                log("updateDataStallInfo: OUT sent=" + sent +
                        " mSentSinceLastRecv=" + mSentSinceLastRecv);
            }
        } else if (sent == 0 && received > 0) {
            if (VDBG_STALL) log("updateDataStallInfo: IN");
            mSentSinceLastRecv = 0;
            mDsRecoveryHandler.reset();
        } else {
            if (VDBG_STALL) log("updateDataStallInfo: NONE");
        }
    }

    private boolean isPhoneStateIdle() {
        for (int i = 0; i < mTelephonyManager.getPhoneCount(); i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone != null && phone.getState() != PhoneConstants.State.IDLE) {
                log("isPhoneStateIdle false: Voice call active on phone " + i);
                return false;
            }
        }
        return true;
    }

    private void onDataStallAlarm(int tag) {
        if (mDataStallAlarmTag != tag) {
            if (DBG) {
                log("onDataStallAlarm: ignore, tag=" + tag + " expecting " + mDataStallAlarmTag);
            }
            return;
        }

        if (DBG) log("Data stall alarm");
        updateDataStallInfo();

        int hangWatchdogTrigger = Settings.Global.getInt(mResolver,
                Settings.Global.PDP_WATCHDOG_TRIGGER_PACKET_COUNT,
                NUMBER_SENT_PACKETS_OF_HANG);

        boolean suspectedStall = DATA_STALL_NOT_SUSPECTED;
        if (mSentSinceLastRecv >= hangWatchdogTrigger) {
            if (DBG) {
                log("onDataStallAlarm: tag=" + tag + " do recovery action="
                        + mDsRecoveryHandler.getRecoveryAction());
            }
            suspectedStall = DATA_STALL_SUSPECTED;
            sendMessage(obtainMessage(DctConstants.EVENT_DO_RECOVERY));
        } else {
            if (VDBG_STALL) {
                log("onDataStallAlarm: tag=" + tag + " Sent " + String.valueOf(mSentSinceLastRecv) +
                    " pkts since last received, < watchdogTrigger=" + hangWatchdogTrigger);
            }
        }
        startDataStallAlarm(suspectedStall);
    }

    protected void startDataStallAlarm(boolean suspectedStall) {
        int delayInMs;

        if (mDsRecoveryHandler.isNoRxDataStallDetectionEnabled() && isAnyDataConnected()) {
            // If screen is on or data stall is currently suspected, set the alarm
            // with an aggressive timeout.
            if (mIsScreenOn || suspectedStall || mDsRecoveryHandler.isAggressiveRecovery()) {
                delayInMs = Settings.Global.getInt(mResolver,
                        Settings.Global.DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS,
                        DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS_DEFAULT);
            } else {
                delayInMs = Settings.Global.getInt(mResolver,
                        Settings.Global.DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS,
                        DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT);
            }

            mDataStallAlarmTag += 1;
            if (VDBG_STALL) {
                log("startDataStallAlarm: tag=" + mDataStallAlarmTag +
                        " delay=" + (delayInMs / 1000) + "s");
            }
            Intent intent = new Intent(INTENT_DATA_STALL_ALARM);
            intent.putExtra(INTENT_DATA_STALL_ALARM_EXTRA_TAG, mDataStallAlarmTag);
            intent.putExtra(INTENT_DATA_STALL_ALARM_EXTRA_TRANSPORT_TYPE, mTransportType);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
            mDataStallAlarmIntent = PendingIntent.getBroadcast(mPhone.getContext(), 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            mAlarmManager.set(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + delayInMs, mDataStallAlarmIntent);
        } else {
            if (VDBG_STALL) {
                log("startDataStallAlarm: NOT started, no connection tag=" + mDataStallAlarmTag);
            }
        }
    }

    private void stopDataStallAlarm() {
        if (VDBG_STALL) {
            log("stopDataStallAlarm: current tag=" + mDataStallAlarmTag +
                    " mDataStallAlarmIntent=" + mDataStallAlarmIntent);
        }
        mDataStallAlarmTag += 1;
        if (mDataStallAlarmIntent != null) {
            mAlarmManager.cancel(mDataStallAlarmIntent);
            mDataStallAlarmIntent = null;
        }
    }

    private void restartDataStallAlarm() {
        if (!isAnyDataConnected()) return;
        // To be called on screen status change.
        // Do not cancel the alarm if it is set with aggressive timeout.
        if (mDsRecoveryHandler.isAggressiveRecovery()) {
            if (DBG) log("restartDataStallAlarm: action is pending. not resetting the alarm.");
            return;
        }
        if (VDBG_STALL) log("restartDataStallAlarm: stop then start.");
        stopDataStallAlarm();
        startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
    }

    /**
     * Provisioning APN
     */
    private void onActionIntentProvisioningApnAlarm(Intent intent) {
        if (DBG) log("onActionIntentProvisioningApnAlarm: action=" + intent.getAction());
        Message msg = obtainMessage(DctConstants.EVENT_PROVISIONING_APN_ALARM,
                intent.getAction());
        msg.arg1 = intent.getIntExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, 0);
        sendMessage(msg);
    }

    private void startProvisioningApnAlarm() {
        int delayInMs = Settings.Global.getInt(mResolver,
                                Settings.Global.PROVISIONING_APN_ALARM_DELAY_IN_MS,
                                PROVISIONING_APN_ALARM_DELAY_IN_MS_DEFAULT);
        if (TelephonyUtils.IS_DEBUGGABLE) {
            // Allow debug code to use a system property to provide another value
            String delayInMsStrg = Integer.toString(delayInMs);
            delayInMsStrg = System.getProperty(DEBUG_PROV_APN_ALARM, delayInMsStrg);
            try {
                delayInMs = Integer.parseInt(delayInMsStrg);
            } catch (NumberFormatException e) {
                loge("startProvisioningApnAlarm: e=" + e);
            }
        }
        mProvisioningApnAlarmTag += 1;
        if (DBG) {
            log("startProvisioningApnAlarm: tag=" + mProvisioningApnAlarmTag +
                    " delay=" + (delayInMs / 1000) + "s");
        }
        Intent intent = new Intent(INTENT_PROVISIONING_APN_ALARM);
        intent.putExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, mProvisioningApnAlarmTag);
        mProvisioningApnAlarmIntent = PendingIntent.getBroadcast(mPhone.getContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayInMs, mProvisioningApnAlarmIntent);
    }

    private void stopProvisioningApnAlarm() {
        if (DBG) {
            log("stopProvisioningApnAlarm: current tag=" + mProvisioningApnAlarmTag +
                    " mProvsioningApnAlarmIntent=" + mProvisioningApnAlarmIntent);
        }
        mProvisioningApnAlarmTag += 1;
        if (mProvisioningApnAlarmIntent != null) {
            mAlarmManager.cancel(mProvisioningApnAlarmIntent);
            mProvisioningApnAlarmIntent = null;
        }
    }

    /**
     * 5G connection reevaluation alarm
     */
    private void startWatchdogAlarm() {
        sendMessageDelayed(obtainMessage(DctConstants.EVENT_NR_TIMER_WATCHDOG), mWatchdogTimeMs);
        mWatchdog = true;
    }

    private void stopWatchdogAlarm() {
        removeMessages(DctConstants.EVENT_NR_TIMER_WATCHDOG);
        mWatchdog = false;
    }

    private void onDataServiceBindingChanged(boolean bound) {
        if (!bound) {
            if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                boolean connPersistenceOnRestart = mPhone.getContext().getResources()
                   .getBoolean(com.android
                       .internal.R.bool.config_wlan_data_service_conn_persistence_on_restart);
                if (!connPersistenceOnRestart) {
                    cleanUpAllConnectionsInternal(false, Phone.REASON_IWLAN_DATA_SERVICE_DIED);
                }
            }
        } else {
            //reset throttling after binding to data service
            mDataThrottler.reset();
        }
        mDataServiceBound = bound;
    }

    public static String requestTypeToString(@RequestNetworkType int type) {
        switch (type) {
            case REQUEST_TYPE_NORMAL: return "NORMAL";
            case REQUEST_TYPE_HANDOVER: return "HANDOVER";
        }
        return "UNKNOWN";
    }

    public static String releaseTypeToString(@ReleaseNetworkType int type) {
        switch (type) {
            case RELEASE_TYPE_NORMAL: return "NORMAL";
            case RELEASE_TYPE_DETACH: return "DETACH";
            case RELEASE_TYPE_HANDOVER: return "HANDOVER";
        }
        return "UNKNOWN";
    }

    @RilRadioTechnology
    protected int getDataRat() {
        ServiceState ss = mPhone.getServiceState();
        NetworkRegistrationInfo nrs = ss.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, mTransportType);
        if (nrs != null) {
            return ServiceState.networkTypeToRilRadioTechnology(nrs.getAccessNetworkTechnology());
        }
        return ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
    }

    @RilRadioTechnology
    private int getVoiceRat() {
        ServiceState ss = mPhone.getServiceState();
        NetworkRegistrationInfo nrs = ss.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_CS, mTransportType);
        if (nrs != null) {
            return ServiceState.networkTypeToRilRadioTechnology(nrs.getAccessNetworkTechnology());
        }
        return ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
    }

    private void read5GConfiguration() {
        if (DBG) log("read5GConfiguration");
        String[] bandwidths = CarrierConfigManager.getDefaultConfig().getStringArray(
                CarrierConfigManager.KEY_BANDWIDTH_STRING_ARRAY);
        boolean useLte = false;
        CarrierConfigManager configManager = (CarrierConfigManager) mPhone.getContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            PersistableBundle b = configManager.getConfigForSubId(mPhone.getSubId());
            if (b != null) {
                if (b.getStringArray(CarrierConfigManager.KEY_BANDWIDTH_STRING_ARRAY) != null) {
                    bandwidths = b.getStringArray(CarrierConfigManager.KEY_BANDWIDTH_STRING_ARRAY);
                }
                useLte = b.getBoolean(CarrierConfigManager
                        .KEY_BANDWIDTH_NR_NSA_USE_LTE_VALUE_FOR_UPLINK_BOOL);
                mWatchdogTimeMs = b.getLong(CarrierConfigManager.KEY_5G_WATCHDOG_TIME_MS_LONG);
                mNrNsaAllUnmetered = b.getBoolean(CarrierConfigManager.KEY_UNMETERED_NR_NSA_BOOL);
                mNrNsaMmwaveUnmetered = b.getBoolean(
                        CarrierConfigManager.KEY_UNMETERED_NR_NSA_MMWAVE_BOOL);
                mNrNsaSub6Unmetered = b.getBoolean(
                        CarrierConfigManager.KEY_UNMETERED_NR_NSA_SUB6_BOOL);
                mNrSaAllUnmetered = b.getBoolean(CarrierConfigManager.KEY_UNMETERED_NR_SA_BOOL);
                mNrSaMmwaveUnmetered = b.getBoolean(
                        CarrierConfigManager.KEY_UNMETERED_NR_SA_MMWAVE_BOOL);
                mNrSaSub6Unmetered = b.getBoolean(
                        CarrierConfigManager.KEY_UNMETERED_NR_SA_SUB6_BOOL);
                mNrNsaRoamingUnmetered = b.getBoolean(
                        CarrierConfigManager.KEY_UNMETERED_NR_NSA_WHEN_ROAMING_BOOL);
                mLteEndcUsingUserDataForRrcDetection = b.getBoolean(
                        CarrierConfigManager.KEY_LTE_ENDC_USING_USER_DATA_FOR_RRC_DETECTION_BOOL);
            }
        }
        updateLinkBandwidths(bandwidths, useLte);
    }

    public boolean getLteEndcUsingUserDataForIdleDetection() {
        return mLteEndcUsingUserDataForRrcDetection;
    }

    /**
     * Register for physical link status (i.e. RRC state) changed event.
     * if {@link CarrierConfigManager.KEY_LTE_ENDC_USING_USER_DATA_FOR_RRC_DETECTION_BOOL} is true,
     * then physical link state is focusing on "internet data connection" instead of RRC state.
     *
     * @param h The handler
     * @param what The event
     */
    public void registerForPhysicalLinkStatusChanged(Handler h, int what) {
        mDcc.registerForPhysicalLinkStatusChanged(h, what);
    }

    /**
     * Unregister from physical link status (i.e. RRC state) changed event.
     *
     * @param h The previously registered handler
     */
    public void unregisterForPhysicalLinkStatusChanged(Handler h) {
        mDcc.unregisterForPhysicalLinkStatusChanged(h);
    }

    // We use a specialized equals function in Apn setting when checking if an active
    // data connection is still legitimate to use against a different apn setting.
    // This method is extracted to a function to ensure that any future changes to this check will
    // be applied to both cleanUpConnectionsOnUpdatedApns and checkForCompatibleDataConnection.
    // Fix for b/158908392.
    private boolean areCompatible(ApnSetting apnSetting1, ApnSetting apnSetting2) {
        return apnSetting1.equals(apnSetting2,
                mPhone.getServiceState().getDataRoamingFromRegistration());
    }

    @NonNull
    private PersistableBundle getCarrierConfig() {
        CarrierConfigManager configManager = (CarrierConfigManager) mPhone.getContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            // If an invalid subId is used, this bundle will contain default values.
            PersistableBundle config = configManager.getConfigForSubId(mPhone.getSubId());
            if (config != null) {
                return config;
            }
        }
        // Return static default defined in CarrierConfigManager.
        return CarrierConfigManager.getDefaultConfig();
    }

    /**
     * @return The data service manager.
     */
    public @NonNull DataServiceManager getDataServiceManager() {
        return mDataServiceManager;
    }

    /**
     * @return The data throttler
     */
    public @NonNull DataThrottler getDataThrottler() {
        return mDataThrottler;
    }

    private void showProvisioningNotification() {
        final Intent intent = new Intent(DcTracker.INTENT_PROVISION);
        intent.putExtra(DcTracker.EXTRA_PROVISION_PHONE_ID, mPhone.getPhoneId());
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                mPhone.getContext(), 0 /* requestCode */, intent, PendingIntent.FLAG_IMMUTABLE);

        final Resources r = mPhone.getContext().getResources();
        final String title = r.getString(R.string.network_available_sign_in, 0);
        final String details = mTelephonyManager.getNetworkOperator(mPhone.getSubId());
        final Notification.Builder builder = new Notification.Builder(mPhone.getContext())
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.stat_notify_rssi_in_range)
                .setChannelId(NotificationChannelController.CHANNEL_ID_MOBILE_DATA_STATUS)
                .setAutoCancel(true)
                .setTicker(title)
                .setColor(mPhone.getContext().getColor(
                        com.android.internal.R.color.system_notification_accent_color))
                .setContentTitle(title)
                .setContentText(details)
                .setContentIntent(pendingIntent)
                .setLocalOnly(true)
                .setOnlyAlertOnce(true);

        final Notification notification = builder.build();
        try {
            getNotificationManager().notify(NOTIFICATION_TAG, mPhone.getPhoneId(), notification);
        } catch (final NullPointerException npe) {
            Log.e(mLogTag, "showProvisioningNotification: error showing notification", npe);
        }
    }

    private void hideProvisioningNotification() {
        try {
            getNotificationManager().cancel(NOTIFICATION_TAG, mPhone.getPhoneId());
        } catch (final NullPointerException npe) {
            Log.e(mLogTag, "hideProvisioningNotification: error hiding notification", npe);
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) mPhone.getContext()
                .createContextAsUser(UserHandle.ALL, 0 /* flags */)
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }
}

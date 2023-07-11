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

package com.android.internal.telephony;

import static android.provider.Telephony.ServiceStateTable.getUriForSubscriptionId;

import static com.android.internal.telephony.CarrierActionAgent.CARRIER_ACTION_SET_RADIO_ENABLED;
import static com.android.internal.telephony.uicc.IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_PLMN;
import static com.android.internal.telephony.uicc.IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Notification;
import android.app.NotificationManager;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.radio.V1_0.CellInfoType;
import android.net.NetworkCapabilities;
import android.os.AsyncResult;
import android.os.BaseBundle;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.sysprop.TelephonyProperties;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.DataSpecificRegistrationInfo;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PhysicalChannelConfig;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.ServiceState.RilRadioTechnology;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.telephony.VoiceSpecificRegistrationInfo;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.LocalLog;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.internal.telephony.cdma.EriManager;
import com.android.internal.telephony.cdnr.CarrierDisplayNameData;
import com.android.internal.telephony.cdnr.CarrierDisplayNameResolver;
import com.android.internal.telephony.data.AccessNetworksManager;
import com.android.internal.telephony.data.DataNetwork;
import com.android.internal.telephony.data.DataNetworkController.DataNetworkControllerCallback;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.telephony.metrics.ServiceStateStats;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccPort;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.internal.telephony.util.ArrayUtils;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@hide}
 */
public class ServiceStateTracker extends Handler {
    static final String LOG_TAG = "SST";
    static final boolean DBG = true;
    private static final boolean VDBG = false;  // STOPSHIP if true

    private static final String PROP_FORCE_ROAMING = "telephony.test.forceRoaming";

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private CommandsInterface mCi;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private UiccController mUiccController = null;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private UiccCardApplication mUiccApplcation = null;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private IccRecords mIccRecords = null;

    private boolean mVoiceCapable;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public ServiceState mSS;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private ServiceState mNewSS;
    // A placeholder service state which will always be out of service. This is broadcast to
    // listeners when the subscription ID for a phone becomes invalid so that they get a final
    // state update.
    private final ServiceState mOutOfServiceSS;

    // This is the minimum interval at which CellInfo requests will be serviced by the modem.
    // Any requests that arrive within MinInterval of the previous reuqest will simply receive the
    // cached result. This is a power-saving feature, because requests to the modem may require
    // wakeup of a separate chip and bus communication. Because the cost of wakeups is
    // architecture dependent, it would be preferable if this sort of optimization could be
    // handled in SoC-specific code, but for now, keep it here to ensure that in case further
    // optimizations are not present elsewhere, there is a power-management scheme of last resort.
    private int mCellInfoMinIntervalMs =  2000;

    // Maximum time to wait for a CellInfo request before assuming it won't arrive and returning
    // null to callers. Note, that if a CellInfo response does arrive later, then it will be
    // treated as an UNSOL, which means it will be cached as well as sent to registrants; thus,
    // this only impacts the behavior of one-shot requests (be they blocking or non-blocking).
    private static final long CELL_INFO_LIST_QUERY_TIMEOUT = 2000;

    private long mLastCellInfoReqTime;
    private List<CellInfo> mLastCellInfoList = null;
    private List<PhysicalChannelConfig> mLastPhysicalChannelConfigList = null;

    private final Set<Integer> mRadioPowerOffReasons = new HashSet();

    // TODO - this should not be public, right now used externally GsmConnetion.
    public RestrictedState mRestrictedState;

    /**
     * A unique identifier to track requests associated with a poll
     * and ignore stale responses.  The value is a count-down of
     * expected responses in this pollingContext.
     */
    @VisibleForTesting
    public int[] mPollingContext;
    @UnsupportedAppUsage
    private boolean mDesiredPowerState;

    @UnsupportedAppUsage
    private RegistrantList mVoiceRoamingOnRegistrants = new RegistrantList();
    @UnsupportedAppUsage
    private RegistrantList mVoiceRoamingOffRegistrants = new RegistrantList();
    @UnsupportedAppUsage
    private RegistrantList mDataRoamingOnRegistrants = new RegistrantList();
    @UnsupportedAppUsage
    private RegistrantList mDataRoamingOffRegistrants = new RegistrantList();
    protected SparseArray<RegistrantList> mAttachedRegistrants = new SparseArray<>();
    protected SparseArray<RegistrantList> mDetachedRegistrants = new SparseArray();
    private RegistrantList mVoiceRegStateOrRatChangedRegistrants = new RegistrantList();
    private SparseArray<RegistrantList> mDataRegStateOrRatChangedRegistrants = new SparseArray<>();
    @UnsupportedAppUsage
    private RegistrantList mNetworkAttachedRegistrants = new RegistrantList();
    private RegistrantList mNetworkDetachedRegistrants = new RegistrantList();
    private RegistrantList mServiceStateChangedRegistrants = new RegistrantList();
    private RegistrantList mPsRestrictEnabledRegistrants = new RegistrantList();
    private RegistrantList mPsRestrictDisabledRegistrants = new RegistrantList();
    private RegistrantList mImsCapabilityChangedRegistrants = new RegistrantList();
    private RegistrantList mNrStateChangedRegistrants = new RegistrantList();
    private RegistrantList mNrFrequencyChangedRegistrants = new RegistrantList();
    private RegistrantList mCssIndicatorChangedRegistrants = new RegistrantList();
    private final RegistrantList mBandwidthChangedRegistrants = new RegistrantList();
    private final RegistrantList mAirplaneModeChangedRegistrants = new RegistrantList();
    private final RegistrantList mAreaCodeChangedRegistrants = new RegistrantList();

    /* Radio power off pending flag and tag counter */
    private boolean mPendingRadioPowerOffAfterDataOff = false;
    private int mPendingRadioPowerOffAfterDataOffTag = 0;

    /** Waiting period before recheck gprs and voice registration. */
    public static final int DEFAULT_GPRS_CHECK_PERIOD_MILLIS = 60 * 1000;

    /**
     * The timer value to wait for all data networks to be torn down.
     */
    private static final long POWER_OFF_ALL_DATA_NETWORKS_DISCONNECTED_TIMEOUT =
            TimeUnit.SECONDS.toMillis(10);

    /** GSM events */
    protected static final int EVENT_RADIO_STATE_CHANGED                    = 1;
    protected static final int EVENT_NETWORK_STATE_CHANGED                  = 2;
    protected static final int EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION    = 4;
    protected static final int EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION    = 5;
    protected static final int EVENT_POLL_STATE_PS_IWLAN_REGISTRATION       = 6;
    protected static final int EVENT_POLL_STATE_OPERATOR                    = 7;
    protected static final int EVENT_NITZ_TIME                              = 11;
    protected static final int EVENT_POLL_STATE_NETWORK_SELECTION_MODE      = 14;
    protected static final int EVENT_GET_LOC_DONE                           = 15;
    protected static final int EVENT_SIM_RECORDS_LOADED                     = 16;
    protected static final int EVENT_SIM_READY                              = 17;
    protected static final int EVENT_LOCATION_UPDATES_ENABLED               = 18;
    protected static final int EVENT_GET_ALLOWED_NETWORK_TYPES              = 19;
    protected static final int EVENT_SET_ALLOWED_NETWORK_TYPES              = 20;
    protected static final int EVENT_RESET_ALLOWED_NETWORK_TYPES            = 21;
    protected static final int EVENT_CHECK_REPORT_GPRS                      = 22;
    protected static final int EVENT_RESTRICTED_STATE_CHANGED               = 23;

    /** CDMA events */
    protected static final int EVENT_RUIM_READY                        = 26;
    protected static final int EVENT_RUIM_RECORDS_LOADED               = 27;
    protected static final int EVENT_POLL_STATE_CDMA_SUBSCRIPTION      = 34;
    protected static final int EVENT_NV_READY                          = 35;
    protected static final int EVENT_OTA_PROVISION_STATUS_CHANGE       = 37;
    protected static final int EVENT_SET_RADIO_POWER_OFF               = 38;
    protected static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED  = 39;
    protected static final int EVENT_CDMA_PRL_VERSION_CHANGED          = 40;

    protected static final int EVENT_RADIO_ON                          = 41;
    public    static final int EVENT_ICC_CHANGED                       = 42;
    protected static final int EVENT_GET_CELL_INFO_LIST                = 43;
    protected static final int EVENT_UNSOL_CELL_INFO_LIST              = 44;
    // Only sent if the IMS state is moving from true -> false and power off delay for IMS
    // registration feature is enabled.
    protected static final int EVENT_CHANGE_IMS_STATE                  = 45;
    protected static final int EVENT_IMS_STATE_CHANGED                 = 46;
    protected static final int EVENT_IMS_STATE_DONE                    = 47;
    protected static final int EVENT_IMS_CAPABILITY_CHANGED            = 48;
    protected static final int EVENT_ALL_DATA_DISCONNECTED             = 49;
    protected static final int EVENT_PHONE_TYPE_SWITCHED               = 50;
    protected static final int EVENT_RADIO_POWER_FROM_CARRIER          = 51;
    protected static final int EVENT_IMS_SERVICE_STATE_CHANGED         = 53;
    protected static final int EVENT_RADIO_POWER_OFF_DONE              = 54;
    protected static final int EVENT_PHYSICAL_CHANNEL_CONFIG           = 55;
    protected static final int EVENT_CELL_LOCATION_RESPONSE            = 56;
    protected static final int EVENT_CARRIER_CONFIG_CHANGED            = 57;
    private static final int EVENT_POLL_STATE_REQUEST                  = 58;
    // Timeout event used when delaying radio power off to wait for IMS deregistration to happen.
    private static final int EVENT_POWER_OFF_RADIO_IMS_DEREG_TIMEOUT   = 62;
    protected static final int EVENT_RESET_LAST_KNOWN_CELL_IDENTITY    = 63;
    private static final int EVENT_REGISTER_DATA_NETWORK_EXISTING_CHANGED = 64;
    // Telecom has un/registered a PhoneAccount that provides OTT voice calling capability, e.g.
    // wi-fi calling.
    protected static final int EVENT_TELECOM_VOICE_SERVICE_STATE_OVERRIDE_CHANGED = 65;

    /**
     * The current service state.
     *
     * This is a column name in {@link android.provider.Telephony.ServiceStateTable}.
     *
     * Copied from packages/services/Telephony/src/com/android/phone/ServiceStateProvider.java
     */
    private static final String SERVICE_STATE = "service_state";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"CARRIER_NAME_DISPLAY_BITMASK"},
            value = {CARRIER_NAME_DISPLAY_BITMASK_SHOW_PLMN,
                    CARRIER_NAME_DISPLAY_BITMASK_SHOW_SPN},
            flag = true)
    public @interface CarrierNameDisplayBitmask {}

    // Show SPN only and only if this bit is set.
    public static final int CARRIER_NAME_DISPLAY_BITMASK_SHOW_SPN = 1 << 0;

    // Show PLMN only and only if this bit is set.
    public static final int CARRIER_NAME_DISPLAY_BITMASK_SHOW_PLMN = 1 << 1;

    private List<Message> mPendingCellInfoRequests = new LinkedList<Message>();
    // @GuardedBy("mPendingCellInfoRequests")
    private boolean mIsPendingCellInfoRequest = false;

    /** Reason for registration denial. */
    protected static final String REGISTRATION_DENIED_GEN  = "General";
    protected static final String REGISTRATION_DENIED_AUTH = "Authentication Failure";

    private CarrierDisplayNameResolver mCdnr;

    private boolean mImsRegistrationOnOff = false;
    /** Radio is disabled by carrier. Radio power will not be override if this field is set */
    private boolean mRadioDisabledByCarrier = false;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean mDeviceShuttingDown = false;
    /** Keep track of SPN display rules, so we only broadcast intent if something changes. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean mSpnUpdatePending = false;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String mCurSpn = null;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String mCurDataSpn = null;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String mCurPlmn = null;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean mCurShowPlmn = false;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean mCurShowSpn = false;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @VisibleForTesting
    public int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private int mPrevSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private boolean mImsRegistered = false;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private SubscriptionManager mSubscriptionManager;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private SubscriptionController mSubscriptionController;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final SstSubscriptionsChangedListener mOnSubscriptionsChangedListener =
        new SstSubscriptionsChangedListener();


    private final RatRatcheter mRatRatcheter;

    private final LocaleTracker mLocaleTracker;

    private final LocalLog mRoamingLog = new LocalLog(8);
    private final LocalLog mAttachLog = new LocalLog(8);
    private final LocalLog mPhoneTypeLog = new LocalLog(8);
    private final LocalLog mRatLog = new LocalLog(16);
    private final LocalLog mRadioPowerLog = new LocalLog(16);
    private final LocalLog mCdnrLogs = new LocalLog(64);

    private Pattern mOperatorNameStringPattern;

    private class SstSubscriptionsChangedListener extends OnSubscriptionsChangedListener {

        /**
         * Callback invoked when there is any change to any SubscriptionInfo. Typically
         * this method would invoke {@link SubscriptionManager#getActiveSubscriptionInfoList}
         */
        @Override
        public void onSubscriptionsChanged() {
            if (DBG) log("SubscriptionListener.onSubscriptionInfoChanged");

            final int curSubId = mPhone.getSubId();

            // If the sub info changed, but the subId is the same, then we're done.
            if (mSubId == curSubId) return;

            // If not, then the subId has changed, so we need to remember the old subId,
            // even if the new subId is invalid (likely).
            mPrevSubId = mSubId;
            mSubId = curSubId;

            // Update voicemail count and notify message waiting changed regardless of
            // whether the new subId is valid. This is an exception to the general logic
            // of only updating things if the new subscription is valid. The result is that
            // VoiceMail counts (and UI indicators) are cleared when the SIM is removed,
            // which seems desirable.
            mPhone.updateVoiceMail();

            if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
                if (SubscriptionManager.isValidSubscriptionId(mPrevSubId)) {
                    // just went from valid to invalid subId, so notify phone state listeners
                    // with final broadcast
                    mPhone.notifyServiceStateChangedForSubId(mOutOfServiceSS,
                            ServiceStateTracker.this.mPrevSubId);
                }
                // If the new subscription ID isn't valid, then we don't need to do all the
                // UI updating, so we're done.
                return;
            }

            Context context = mPhone.getContext();

            mPhone.notifyPhoneStateChanged();

            if (!SubscriptionManager.isValidSubscriptionId(mPrevSubId)) {
                // just went from invalid to valid subId, so notify with current service
                // state in case our service state was never broadcasted (we don't notify
                // service states when the subId is invalid)
                mPhone.notifyServiceStateChanged(mPhone.getServiceState());
            }

            boolean restoreSelection = !context.getResources().getBoolean(
                    com.android.internal.R.bool.skip_restoring_network_selection);
            mPhone.sendSubscriptionSettings(restoreSelection);

            setDataNetworkTypeForPhone(mSS.getRilDataRadioTechnology());

            if (mSpnUpdatePending) {
                mSubscriptionController.setPlmnSpn(mPhone.getPhoneId(), mCurShowPlmn,
                        mCurPlmn, mCurShowSpn, mCurSpn);
                mSpnUpdatePending = false;
            }

            // Remove old network selection sharedPreferences since SP key names are now
            // changed to include subId. This will be done only once when upgrading from an
            // older build that did not include subId in the names.
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                    context);
            String oldNetworkSelection = sp.getString(
                    Phone.NETWORK_SELECTION_KEY, "");
            String oldNetworkSelectionName = sp.getString(
                    Phone.NETWORK_SELECTION_NAME_KEY, "");
            String oldNetworkSelectionShort = sp.getString(
                    Phone.NETWORK_SELECTION_SHORT_KEY, "");
            if (!TextUtils.isEmpty(oldNetworkSelection)
                    || !TextUtils.isEmpty(oldNetworkSelectionName)
                    || !TextUtils.isEmpty(oldNetworkSelectionShort)) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(Phone.NETWORK_SELECTION_KEY + mSubId,
                        oldNetworkSelection);
                editor.putString(Phone.NETWORK_SELECTION_NAME_KEY + mSubId,
                        oldNetworkSelectionName);
                editor.putString(Phone.NETWORK_SELECTION_SHORT_KEY + mSubId,
                        oldNetworkSelectionShort);
                editor.remove(Phone.NETWORK_SELECTION_KEY);
                editor.remove(Phone.NETWORK_SELECTION_NAME_KEY);
                editor.remove(Phone.NETWORK_SELECTION_SHORT_KEY);
                editor.commit();
            }

            // Once sub id becomes valid, we need to update the service provider name
            // displayed on the UI again. The old SPN update intents sent to
            // MobileSignalController earlier were actually ignored due to invalid sub id.
            updateSpnDisplay();
        }
    };

    //Common
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected final GsmCdmaPhone mPhone;

    private CellIdentity mCellIdentity;
    @Nullable private CellIdentity mLastKnownCellIdentity;
    private static final int MS_PER_HOUR = 60 * 60 * 1000;
    private final NitzStateMachine mNitzState;

    private ServiceStateStats mServiceStateStats;

    /**
     * Holds the last NITZ signal received. Used only for trying to determine an MCC from a CDMA
     * SID.
     */
    @Nullable
    private NitzData mLastNitzData;

    private final EriManager mEriManager;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final ContentResolver mCr;

    //GSM
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mAllowedNetworkTypes;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mMaxDataCalls = 1;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mNewMaxDataCalls = 1;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mReasonDataDenied = -1;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mNewReasonDataDenied = -1;

    /**
     * The code of the rejection cause that is sent by network when the CS
     * registration is rejected. It should be shown to the user as a notification.
     */
    private int mRejectCode;
    private int mNewRejectCode;

    /**
     * GSM voice roaming status solely based on TS 27.007 7.2 CREG. Only used by
     * handlePollStateResult to store CREG roaming result.
     */
    private boolean mGsmVoiceRoaming = false;
    /**
     * Gsm data roaming status solely based on TS 27.007 10.1.19 CGREG. Only used by
     * handlePollStateResult to store CGREG roaming result.
     */
    private boolean mGsmDataRoaming = false;
    /**
     * Mark when service state is in emergency call only mode
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean mEmergencyOnly = false;
    private boolean mCSEmergencyOnly = false;
    private boolean mPSEmergencyOnly = false;
    /** Started the recheck process after finding gprs should registered but not. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean mStartedGprsRegCheck;
    /** Already sent the event-log for no gprs register. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean mReportedGprsNoReg;

    private CarrierServiceStateTracker mCSST;
    /**
     * The Notification object given to the NotificationManager.
     */
    private Notification mNotification;
    /** Notification type. */
    public static final int PS_ENABLED = 1001;            // Access Control blocks data service
    public static final int PS_DISABLED = 1002;           // Access Control enables data service
    public static final int CS_ENABLED = 1003;            // Access Control blocks all voice/sms service
    public static final int CS_DISABLED = 1004;           // Access Control enables all voice/sms service
    public static final int CS_NORMAL_ENABLED = 1005;     // Access Control blocks normal voice/sms service
    public static final int CS_EMERGENCY_ENABLED = 1006;  // Access Control blocks emergency call service
    public static final int CS_REJECT_CAUSE_ENABLED = 2001;     // Notify MM rejection cause
    public static final int CS_REJECT_CAUSE_DISABLED = 2002;    // Cancel MM rejection cause
    /** Notification id. */
    public static final int PS_NOTIFICATION = 888;  // Id to update and cancel PS restricted
    public static final int CS_NOTIFICATION = 999;  // Id to update and cancel CS restricted
    public static final int CS_REJECT_CAUSE_NOTIFICATION = 111; // Id to update and cancel MM
                                                                // rejection cause

    /** To identify whether EVENT_SIM_READY is received or not */
    private boolean mIsSimReady = false;

    private String mLastKnownNetworkCountry = "";

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)) {
                int phoneId = intent.getExtras().getInt(CarrierConfigManager.EXTRA_SLOT_INDEX);
                // Ignore the carrier config changed if the phoneId is not matched.
                if (phoneId == mPhone.getPhoneId()) {
                    sendEmptyMessage(EVENT_CARRIER_CONFIG_CHANGED);
                }
            } else if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
                // Update emergency string or operator name, polling service state.
                pollState();
            } else if (action.equals(TelephonyManager.ACTION_NETWORK_COUNTRY_CHANGED)) {
                String lastKnownNetworkCountry = intent.getStringExtra(
                        TelephonyManager.EXTRA_LAST_KNOWN_NETWORK_COUNTRY);
                if (!mLastKnownNetworkCountry.equals(lastKnownNetworkCountry)) {
                    updateSpnDisplay();
                }
            }
        }
    };

    //CDMA
    // Min values used to by getOtasp()
    public static final String UNACTIVATED_MIN2_VALUE = "000000";
    public static final String UNACTIVATED_MIN_VALUE = "1111110111";
    // Current Otasp value
    private int mCurrentOtaspMode = TelephonyManager.OTASP_UNINITIALIZED;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mRoamingIndicator;
    private boolean mIsInPrl;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mDefaultRoamingIndicator;
    /**
     * Initially assume no data connection.
     */
    private int mRegistrationState = -1;
    private RegistrantList mCdmaForSubscriptionInfoReadyRegistrants = new RegistrantList();
    private String mMdn;
    private int mHomeSystemId[] = null;
    private int mHomeNetworkId[] = null;
    private String mMin;
    private String mPrlVersion;
    private boolean mIsMinInfoReady = false;
    private boolean mIsEriTextLoaded = false;
    private String mEriText;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean mIsSubscriptionFromRuim = false;
    private CdmaSubscriptionSourceManager mCdmaSSM;
    public static final String INVALID_MCC = "000";
    public static final String DEFAULT_MNC = "00";
    private HbpcdUtils mHbpcdUtils = null;
    /* Used only for debugging purposes. */
    private String mRegistrationDeniedReason;
    private String mCurrentCarrier = null;

    private final AccessNetworksManager mAccessNetworksManager;
    private final SparseArray<NetworkRegistrationManager> mRegStateManagers = new SparseArray<>();

    /* Last known TAC/LAC */
    private int mLastKnownAreaCode = CellInfo.UNAVAILABLE;

    /**
     * Indicating if there is any data network existing. This is used in airplane mode turning on
     * scenario, where service state tracker should wait all data disconnected before powering
     * down the modem.
     */
    private boolean mAnyDataExisting = false;

    public ServiceStateTracker(GsmCdmaPhone phone, CommandsInterface ci) {
        mNitzState = TelephonyComponentFactory.getInstance()
                .inject(NitzStateMachine.class.getName())
                .makeNitzStateMachine(phone);
        mPhone = phone;
        mCi = ci;

        mServiceStateStats = new ServiceStateStats(mPhone);

        mCdnr = new CarrierDisplayNameResolver(mPhone);

        // Create EriManager only if phone supports CDMA
        if (UiccController.isCdmaSupported(mPhone.getContext())) {
            mEriManager = TelephonyComponentFactory.getInstance().inject(EriManager.class.getName())
                    .makeEriManager(mPhone, EriManager.ERI_FROM_XML);
        } else {
            mEriManager = null;
        }

        mRatRatcheter = new RatRatcheter(mPhone);
        mVoiceCapable = ((TelephonyManager) mPhone.getContext()
                .getSystemService(Context.TELEPHONY_SERVICE))
                .isVoiceCapable();
        mUiccController = UiccController.getInstance();

        mUiccController.registerForIccChanged(this, EVENT_ICC_CHANGED, null);
        mCi.registerForCellInfoList(this, EVENT_UNSOL_CELL_INFO_LIST, null);
        mCi.registerForPhysicalChannelConfiguration(this, EVENT_PHYSICAL_CHANNEL_CONFIG, null);

        mSubscriptionController = SubscriptionController.getInstance();
        mSubscriptionManager = SubscriptionManager.from(phone.getContext());
        mSubscriptionManager.addOnSubscriptionsChangedListener(
                new android.os.HandlerExecutor(this), mOnSubscriptionsChangedListener);
        mRestrictedState = new RestrictedState();

        mAccessNetworksManager = mPhone.getAccessNetworksManager();
        mOutOfServiceSS = new ServiceState();
        mOutOfServiceSS.setOutOfService(mAccessNetworksManager.isInLegacyMode(), false);

        for (int transportType : mAccessNetworksManager.getAvailableTransports()) {
            mRegStateManagers.append(transportType, new NetworkRegistrationManager(
                    transportType, phone));
            mRegStateManagers.get(transportType).registerForNetworkRegistrationInfoChanged(
                    this, EVENT_NETWORK_STATE_CHANGED, null);
        }
        mLocaleTracker = TelephonyComponentFactory.getInstance()
                .inject(LocaleTracker.class.getName())
                .makeLocaleTracker(mPhone, mNitzState, getLooper());

        mCi.registerForImsNetworkStateChanged(this, EVENT_IMS_STATE_CHANGED, null);
        mCi.registerForRadioStateChanged(this, EVENT_RADIO_STATE_CHANGED, null);
        mCi.setOnNITZTime(this, EVENT_NITZ_TIME, null);

        mCr = phone.getContext().getContentResolver();
        // system setting property AIRPLANE_MODE_ON is set in Settings.
        int airplaneMode = Settings.Global.getInt(mCr, Settings.Global.AIRPLANE_MODE_ON, 0);
        int enableCellularOnBoot = Settings.Global.getInt(mCr,
                Settings.Global.ENABLE_CELLULAR_ON_BOOT, 1);
        mDesiredPowerState = (enableCellularOnBoot > 0) && ! (airplaneMode > 0);
        if (!mDesiredPowerState) {
            mRadioPowerOffReasons.add(Phone.RADIO_POWER_REASON_USER);
        }
        mRadioPowerLog.log("init : airplane mode = " + airplaneMode + " enableCellularOnBoot = " +
                enableCellularOnBoot);


        mPhone.getCarrierActionAgent().registerForCarrierAction(CARRIER_ACTION_SET_RADIO_ENABLED,
                this, EVENT_RADIO_POWER_FROM_CARRIER, null, false);

        // Monitor locale change
        Context context = mPhone.getContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        filter.addAction(TelephonyManager.ACTION_NETWORK_COUNTRY_CHANGED);
        context.registerReceiver(mIntentReceiver, filter);

        mPhone.notifyOtaspChanged(TelephonyManager.OTASP_UNINITIALIZED);

        mCi.setOnRestrictedStateChanged(this, EVENT_RESTRICTED_STATE_CHANGED, null);
        updatePhoneType();

        mCSST = new CarrierServiceStateTracker(phone, this);

        registerForNetworkAttached(mCSST,
                CarrierServiceStateTracker.CARRIER_EVENT_VOICE_REGISTRATION, null);
        registerForNetworkDetached(mCSST,
                CarrierServiceStateTracker.CARRIER_EVENT_VOICE_DEREGISTRATION, null);
        registerForDataConnectionAttached(AccessNetworkConstants.TRANSPORT_TYPE_WWAN, mCSST,
                CarrierServiceStateTracker.CARRIER_EVENT_DATA_REGISTRATION, null);
        registerForDataConnectionDetached(AccessNetworkConstants.TRANSPORT_TYPE_WWAN, mCSST,
                CarrierServiceStateTracker.CARRIER_EVENT_DATA_DEREGISTRATION, null);
        registerForImsCapabilityChanged(mCSST,
                CarrierServiceStateTracker.CARRIER_EVENT_IMS_CAPABILITIES_CHANGED, null);

        if (mPhone.isUsingNewDataStack()) {
            sendEmptyMessage(EVENT_REGISTER_DATA_NETWORK_EXISTING_CHANGED);
        }
    }

    @VisibleForTesting
    public void updatePhoneType() {

        // If we are previously voice roaming, we need to notify that roaming status changed before
        // we change back to non-roaming.
        if (mSS != null && mSS.getVoiceRoaming()) {
            mVoiceRoamingOffRegistrants.notifyRegistrants();
        }

        // If we are previously data roaming, we need to notify that roaming status changed before
        // we change back to non-roaming.
        if (mSS != null && mSS.getDataRoaming()) {
            mDataRoamingOffRegistrants.notifyRegistrants();
        }

        // If we are previously in service, we need to notify that we are out of service now.
        if (mSS != null && mSS.getState() == ServiceState.STATE_IN_SERVICE) {
            mNetworkDetachedRegistrants.notifyRegistrants();
        }

        // If we are previously in service, we need to notify that we are out of service now.
        for (int transport : mAccessNetworksManager.getAvailableTransports()) {
            if (mSS != null) {
                NetworkRegistrationInfo nrs = mSS.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_PS, transport);
                if (nrs != null && nrs.isInService()
                        && mDetachedRegistrants.get(transport) != null) {
                    mDetachedRegistrants.get(transport).notifyRegistrants();
                }
            }
        }

        mSS = new ServiceState();
        mSS.setOutOfService(mAccessNetworksManager.isInLegacyMode(), false);
        mNewSS = new ServiceState();
        mNewSS.setOutOfService(mAccessNetworksManager.isInLegacyMode(), false);
        mLastCellInfoReqTime = 0;
        mLastCellInfoList = null;
        mStartedGprsRegCheck = false;
        mReportedGprsNoReg = false;
        mMdn = null;
        mMin = null;
        mPrlVersion = null;
        mIsMinInfoReady = false;
        mLastNitzData = null;
        mNitzState.handleNetworkUnavailable();
        mCellIdentity = null;
        mPhone.getSignalStrengthController().setSignalStrengthDefaultValues();
        mLastKnownCellIdentity = null;

        //cancel any pending pollstate request on voice tech switching
        cancelPollState();

        if (mPhone.isPhoneTypeGsm()) {
            //clear CDMA registrations first
            if (mCdmaSSM != null) {
                mCdmaSSM.dispose(this);
            }

            mCi.unregisterForCdmaPrlChanged(this);
            mCi.unregisterForCdmaOtaProvision(this);
            mPhone.unregisterForSimRecordsLoaded(this);

        } else {
            mPhone.registerForSimRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
            mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(mPhone.getContext(), mCi, this,
                    EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED, null);
            mIsSubscriptionFromRuim = (mCdmaSSM.getCdmaSubscriptionSource() ==
                    CdmaSubscriptionSourceManager.SUBSCRIPTION_FROM_RUIM);

            mCi.registerForCdmaPrlChanged(this, EVENT_CDMA_PRL_VERSION_CHANGED, null);
            mCi.registerForCdmaOtaProvision(this, EVENT_OTA_PROVISION_STATUS_CHANGE, null);

            mHbpcdUtils = new HbpcdUtils(mPhone.getContext());
            // update OTASP state in case previously set by another service
            updateOtaspState();
        }

        // This should be done after the technology specific initializations above since it relies
        // on fields like mIsSubscriptionFromRuim (which is updated above)
        onUpdateIccAvailability();

        setDataNetworkTypeForPhone(ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN);
        // Query signal strength from the modem after service tracker is created (i.e. boot up,
        // switching between GSM and CDMA phone), because the unsolicited signal strength
        // information might come late or even never come. This will get the accurate signal
        // strength information displayed on the UI.
        mPhone.getSignalStrengthController().getSignalStrengthFromCi();
        sendMessage(obtainMessage(EVENT_PHONE_TYPE_SWITCHED));

        logPhoneTypeChange();

        // Tell everybody that the registration state and RAT have changed.
        notifyVoiceRegStateRilRadioTechnologyChanged();
        for (int transport : mAccessNetworksManager.getAvailableTransports()) {
            notifyDataRegStateRilRadioTechnologyChanged(transport);
        }
    }

    @VisibleForTesting
    public void requestShutdown() {
        if (mDeviceShuttingDown == true) return;
        mDeviceShuttingDown = true;
        mDesiredPowerState = false;
        setPowerStateToDesired();
    }

    /**
     * @return the timeout value in milliseconds that the framework will delay a pending radio power
     * off command while waiting for an IMS deregistered indication.
     */
    @VisibleForTesting
    public int getRadioPowerOffDelayTimeoutForImsRegistration() {
        return mPhone.getContext().getResources().getInteger(
                R.integer.config_delay_for_ims_dereg_millis);
    }

    public void dispose() {
        mPhone.getSignalStrengthController().dispose();
        mUiccController.unregisterForIccChanged(this);
        mCi.unregisterForCellInfoList(this);
        mCi.unregisterForPhysicalChannelConfiguration(this);
        mSubscriptionManager
            .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangedListener);
        mCi.unregisterForImsNetworkStateChanged(this);
        mPhone.getCarrierActionAgent().unregisterForCarrierAction(this,
                CARRIER_ACTION_SET_RADIO_ENABLED);
        mPhone.getContext().unregisterReceiver(mIntentReceiver);
        if (mCSST != null) {
            mCSST.dispose();
            mCSST = null;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean getDesiredPowerState() {
        return mDesiredPowerState;
    }
    public boolean getPowerStateFromCarrier() { return !mRadioDisabledByCarrier; }

    public List<PhysicalChannelConfig> getPhysicalChannelConfigList() {
        return mLastPhysicalChannelConfigList;
    }

    /**
     * Notify all mVoiceRegStateOrRatChangedRegistrants using an
     * AsyncResult in msg.obj where AsyncResult#result contains the
     * new RAT as an Integer Object.
     */
    protected void notifyVoiceRegStateRilRadioTechnologyChanged() {
        int rat = mSS.getRilVoiceRadioTechnology();
        int vrs = mSS.getState();
        if (DBG) log("notifyVoiceRegStateRilRadioTechnologyChanged: vrs=" + vrs + " rat=" + rat);

        mVoiceRegStateOrRatChangedRegistrants.notifyResult(new Pair<Integer, Integer>(vrs, rat));
    }

    /**
     * Get registration info
     *
     * @param transport The transport type
     * @return Pair of registration info including {@link ServiceState.RegState} and
     * {@link RilRadioTechnology}.
     *
     */
    @Nullable
    private Pair<Integer, Integer> getRegistrationInfo(@TransportType int transport) {
        NetworkRegistrationInfo nrs = mSS.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, transport);
        if (nrs != null) {
            int rat = ServiceState.networkTypeToRilRadioTechnology(
                    nrs.getAccessNetworkTechnology());
            int drs = regCodeToServiceState(nrs.getRegistrationState());
            return new Pair<>(drs, rat);
        }
        return null;
    }

    /**
     * Notify all mDataConnectionRatChangeRegistrants using an
     * AsyncResult in msg.obj where AsyncResult#result contains the
     * new RAT as an Integer Object.
     */
    protected void notifyDataRegStateRilRadioTechnologyChanged(@TransportType int transport) {
        RegistrantList registrantList = mDataRegStateOrRatChangedRegistrants.get(transport);
        if (registrantList != null) {
            Pair<Integer, Integer> registrationInfo = getRegistrationInfo(transport);
            if (registrationInfo != null) {
                registrantList.notifyResult(registrationInfo);
            }
        }
    }

    /**
     * Some operators have been known to report registration failure
     * data only devices, to fix that use DataRegState.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void useDataRegStateForDataOnlyDevices() {
        if (mVoiceCapable == false) {
            if (DBG) {
                log("useDataRegStateForDataOnlyDevice: VoiceRegState=" + mNewSS.getState()
                        + " DataRegState=" + mNewSS.getDataRegistrationState());
            }
            // TODO: Consider not lying and instead have callers know the difference.
            mNewSS.setVoiceRegState(mNewSS.getDataRegistrationState());
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void updatePhoneObject() {
        if (mPhone.getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_switch_phone_on_voice_reg_state_change)) {
            // If the phone is not registered on a network, no need to update.
            boolean isRegistered = mSS.getState() == ServiceState.STATE_IN_SERVICE
                    || mSS.getState() == ServiceState.STATE_EMERGENCY_ONLY;
            if (!isRegistered) {
                log("updatePhoneObject: Ignore update");
                return;
            }
            mPhone.updatePhoneObject(mSS.getRilVoiceRadioTechnology());
        }
    }

    /**
     * Registration point for combined roaming on of mobile voice
     * combined roaming is true when roaming is true and ONS differs SPN
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForVoiceRoamingOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mVoiceRoamingOnRegistrants.add(r);

        if (mSS.getVoiceRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForVoiceRoamingOn(Handler h) {
        mVoiceRoamingOnRegistrants.remove(h);
    }

    /**
     * Registration point for roaming off of mobile voice
     * combined roaming is true when roaming is true and ONS differs SPN
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForVoiceRoamingOff(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mVoiceRoamingOffRegistrants.add(r);

        if (!mSS.getVoiceRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForVoiceRoamingOff(Handler h) {
        mVoiceRoamingOffRegistrants.remove(h);
    }

    /**
     * Registration point for combined roaming on of mobile data
     * combined roaming is true when roaming is true and ONS differs SPN
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForDataRoamingOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDataRoamingOnRegistrants.add(r);

        if (mSS.getDataRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForDataRoamingOn(Handler h) {
        mDataRoamingOnRegistrants.remove(h);
    }

    /**
     * Registration point for roaming off of mobile data
     * combined roaming is true when roaming is true and ONS differs SPN
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     * @param notifyNow notify upon registration if data roaming is off
     */
    public void registerForDataRoamingOff(Handler h, int what, Object obj, boolean notifyNow) {
        Registrant r = new Registrant(h, what, obj);
        mDataRoamingOffRegistrants.add(r);

        if (notifyNow && !mSS.getDataRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForDataRoamingOff(Handler h) {
        mDataRoamingOffRegistrants.remove(h);
    }

    /**
     * Re-register network by toggling preferred network type.
     * This is a work-around to deregister and register network since there is
     * no ril api to set COPS=2 (deregister) only.
     *
     * @param onComplete is dispatched when this is complete.  it will be
     * an AsyncResult, and onComplete.obj.exception will be non-null
     * on failure.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void reRegisterNetwork(Message onComplete) {
        mCi.getAllowedNetworkTypesBitmap(
                obtainMessage(EVENT_GET_ALLOWED_NETWORK_TYPES, onComplete));
    }

    /**
     * @return the current reasons for which the radio is off.
     */
    public Set<Integer> getRadioPowerOffReasons() {
        return mRadioPowerOffReasons;
    }

    /**
     * Clear all the radio off reasons. This should be done when turning radio off for genuine or
     * test emergency calls.
     */
    public void clearAllRadioOffReasons() {
        mRadioPowerOffReasons.clear();
    }

    /**
     * Turn on or off radio power.
     */
    public final void setRadioPower(boolean power) {
        setRadioPower(power, false, false, false);
    }

    /**
     * Turn on or off radio power with option to specify whether it's for emergency call.
     * More details check {@link PhoneInternalInterface#setRadioPower(
     * boolean, boolean, boolean, boolean)}.
     */
    public void setRadioPower(boolean power, boolean forEmergencyCall,
            boolean isSelectedPhoneForEmergencyCall, boolean forceApply) {
        setRadioPowerForReason(power, forEmergencyCall, isSelectedPhoneForEmergencyCall, forceApply,
                Phone.RADIO_POWER_REASON_USER);
    }

    /**
     * Turn on or off radio power with option to specify whether it's for emergency call and specify
     * a reason for setting the power state.
     * More details check {@link PhoneInternalInterface#setRadioPowerForReason(
     * boolean, boolean, boolean, boolean, int)}.
     */
    public void setRadioPowerForReason(boolean power, boolean forEmergencyCall,
            boolean isSelectedPhoneForEmergencyCall, boolean forceApply, int reason) {
        log("setRadioPower power " + power + " forEmergencyCall " + forEmergencyCall
                + " forceApply " + forceApply + " reason " + reason);

        if (power) {
            if (forEmergencyCall) {
                clearAllRadioOffReasons();
            } else {
                mRadioPowerOffReasons.remove(reason);
            }
        } else {
            mRadioPowerOffReasons.add(reason);
        }
        if (power == mDesiredPowerState && !forceApply) {
            log("setRadioPower mDesiredPowerState is already " + power + " Do nothing.");
            return;
        }
        if (power && !mRadioPowerOffReasons.isEmpty()) {
            log("setRadioPowerForReason " + "power: " + power + " forEmergencyCall= "
                    + forEmergencyCall + " isSelectedPhoneForEmergencyCall: "
                    + isSelectedPhoneForEmergencyCall + " forceApply " + forceApply + "reason:"
                    + reason + " will not power on the radio as it is powered off for the "
                    + "following reasons: " + mRadioPowerOffReasons + ".");
            return;
        }

        mDesiredPowerState = power;
        setPowerStateToDesired(forEmergencyCall, isSelectedPhoneForEmergencyCall, forceApply);
    }

    /**
     * Radio power set from carrier action. if set to false means carrier desire to turn radio off
     * and radio wont be re-enabled unless carrier explicitly turn it back on.
     * @param enable indicate if radio power is enabled or disabled from carrier action.
     */
    public void setRadioPowerFromCarrier(boolean enable) {
        boolean disableByCarrier = !enable;
        if (mRadioDisabledByCarrier == disableByCarrier) {
            log("setRadioPowerFromCarrier mRadioDisabledByCarrier is already "
                    + disableByCarrier + " Do nothing.");
            return;
        }

        mRadioDisabledByCarrier = disableByCarrier;
        setPowerStateToDesired();
    }

    /**
     * These two flags manage the behavior of the cell lock -- the
     * lock should be held if either flag is true.  The intention is
     * to allow temporary acquisition of the lock to get a single
     * update.  Such a lock grab and release can thus be made to not
     * interfere with more permanent lock holds -- in other words, the
     * lock will only be released if both flags are false, and so
     * releases by temporary users will only affect the lock state if
     * there is no continuous user.
     */
    private boolean mWantContinuousLocationUpdates;
    private boolean mWantSingleLocationUpdate;

    /**
     * Request a single update of the device's current registered cell.
     */
    public void enableSingleLocationUpdate(WorkSource workSource) {
        if (mWantSingleLocationUpdate || mWantContinuousLocationUpdates) return;
        mWantSingleLocationUpdate = true;
        mCi.setLocationUpdates(true, workSource, obtainMessage(EVENT_LOCATION_UPDATES_ENABLED));
    }

    public void enableLocationUpdates() {
        if (mWantSingleLocationUpdate || mWantContinuousLocationUpdates) return;
        mWantContinuousLocationUpdates = true;
        mCi.setLocationUpdates(true, null, obtainMessage(EVENT_LOCATION_UPDATES_ENABLED));
    }

    protected void disableSingleLocationUpdate() {
        mWantSingleLocationUpdate = false;
        if (!mWantSingleLocationUpdate && !mWantContinuousLocationUpdates) {
            mCi.setLocationUpdates(false, null, null);
        }
    }

    public void disableLocationUpdates() {
        mWantContinuousLocationUpdates = false;
        if (!mWantSingleLocationUpdate && !mWantContinuousLocationUpdates) {
            mCi.setLocationUpdates(false, null, null);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        int[] ints;
        Message message;

        if (VDBG) log("received event " + msg.what);
        switch (msg.what) {
            case EVENT_SET_RADIO_POWER_OFF:
                synchronized (this) {
                    if (mPhone.isUsingNewDataStack()) {
                        log("Wait for all data networks torn down timed out. Power off now.");
                        cancelPendingRadioPowerOff();
                        hangupAndPowerOff();
                        return;
                    }
                    if (mPendingRadioPowerOffAfterDataOff &&
                            (msg.arg1 == mPendingRadioPowerOffAfterDataOffTag)) {
                        if (DBG) log("EVENT_SET_RADIO_OFF, turn radio off now.");
                        hangupAndPowerOff();
                        mPendingRadioPowerOffAfterDataOffTag += 1;
                        mPendingRadioPowerOffAfterDataOff = false;
                    } else {
                        log("EVENT_SET_RADIO_OFF is stale arg1=" + msg.arg1 +
                                "!= tag=" + mPendingRadioPowerOffAfterDataOffTag);
                    }
                }
                break;

            case EVENT_ICC_CHANGED:
                if (isSimAbsent()) {
                    if (DBG) log("EVENT_ICC_CHANGED: SIM absent");
                    // cancel notifications if SIM is removed/absent
                    cancelAllNotifications();
                    // clear cached values on SIM removal
                    mMdn = null;
                    mMin = null;
                    mIsMinInfoReady = false;

                    // Remove the EF records that come from UICC.
                    mCdnr.updateEfFromRuim(null /* ruim */);
                    mCdnr.updateEfFromUsim(null /* Usim */);
                }
                onUpdateIccAvailability();
                if (mUiccApplcation == null
                        || mUiccApplcation.getState() != AppState.APPSTATE_READY) {
                    mIsSimReady = false;
                    updateSpnDisplay();
                }
                break;

            case EVENT_GET_CELL_INFO_LIST: // fallthrough
            case EVENT_UNSOL_CELL_INFO_LIST: {
                List<CellInfo> cellInfo = null;
                Throwable ex = null;
                if (msg.obj != null) {
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        log("EVENT_GET_CELL_INFO_LIST: error ret null, e=" + ar.exception);
                        ex = ar.exception;
                    } else if (ar.result == null) {
                        loge("Invalid CellInfo result");
                    } else {
                        cellInfo = (List<CellInfo>) ar.result;
                        updateOperatorNameForCellInfo(cellInfo);
                        mLastCellInfoList = cellInfo;
                        mPhone.notifyCellInfo(cellInfo);
                        if (VDBG) {
                            log("CELL_INFO_LIST: size=" + cellInfo.size() + " list=" + cellInfo);
                        }
                    }
                } else {
                    synchronized (mPendingCellInfoRequests) {
                        // If we receive an empty message, it's probably a timeout; if there is no
                        // pending request, drop it.
                        if (!mIsPendingCellInfoRequest) break;
                        // If there is a request pending, we still need to check whether it's a
                        // timeout for the current request of whether it's leftover from a
                        // previous request.
                        final long curTime = SystemClock.elapsedRealtime();
                        if ((curTime - mLastCellInfoReqTime) <  CELL_INFO_LIST_QUERY_TIMEOUT) {
                            break;
                        }
                        // We've received a legitimate timeout, so something has gone terribly
                        // wrong.
                        loge("Timeout waiting for CellInfo; (everybody panic)!");
                        mLastCellInfoList = null;
                        // Since the timeout is applicable, fall through and update all synchronous
                        // callers with the failure.
                    }
                }
                synchronized (mPendingCellInfoRequests) {
                    // If we have pending requests, then service them. Note that in case of a
                    // timeout, we send null responses back to the callers.
                    if (mIsPendingCellInfoRequest) {
                        // regardless of timeout or valid response, when something arrives,
                        mIsPendingCellInfoRequest = false;
                        for (Message m : mPendingCellInfoRequests) {
                            AsyncResult.forMessage(m, cellInfo, ex);
                            m.sendToTarget();
                        }
                        mPendingCellInfoRequests.clear();
                    }
                }
                break;
            }

            case  EVENT_IMS_STATE_CHANGED: // received unsol
                mCi.getImsRegistrationState(this.obtainMessage(EVENT_IMS_STATE_DONE));
                break;

            case EVENT_IMS_STATE_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    final int[] responseArray = (int[]) ar.result;
                    final boolean imsRegistered = responseArray[0] == 1;
                    mPhone.setImsRegistrationState(imsRegistered);
                    mImsRegistered = imsRegistered;
                }
                break;

            case EVENT_RADIO_POWER_OFF_DONE:
                if (DBG) log("EVENT_RADIO_POWER_OFF_DONE");
                if (mDeviceShuttingDown && mCi.getRadioState()
                        != TelephonyManager.RADIO_POWER_UNAVAILABLE) {
                    // during shutdown the modem may not send radio state changed event
                    // as a result of radio power request
                    // Hence, issuing shut down regardless of radio power response
                    mCi.requestShutdown(null);
                }
                break;

            // GSM
            case EVENT_SIM_READY:
                // Reset the mPrevSubId so we treat a SIM power bounce
                // as a first boot.  See b/19194287
                mPrevSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                mIsSimReady = true;
                pollStateInternal(false);
                break;

            case EVENT_RADIO_STATE_CHANGED:
            case EVENT_PHONE_TYPE_SWITCHED:
                if(!mPhone.isPhoneTypeGsm() &&
                        mCi.getRadioState() == TelephonyManager.RADIO_POWER_ON) {
                    handleCdmaSubscriptionSource(mCdmaSSM.getCdmaSubscriptionSource());
                }
                // This will do nothing in the 'radio not available' case
                setPowerStateToDesired();
                // These events are modem triggered, so pollState() needs to be forced
                pollStateInternal(true);
                break;

            case EVENT_NETWORK_STATE_CHANGED:
                pollStateInternal(true);
                break;

            case EVENT_GET_LOC_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    CellIdentity cellIdentity = ((NetworkRegistrationInfo) ar.result)
                            .getCellIdentity();
                    updateOperatorNameForCellIdentity(cellIdentity);
                    mCellIdentity = cellIdentity;
                    mPhone.notifyLocationChanged(getCellIdentity());
                }

                // Release any temporary cell lock, which could have been
                // acquired to allow a single-shot location update.
                disableSingleLocationUpdate();
                break;

            case EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION:
            case EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION:
            case EVENT_POLL_STATE_PS_IWLAN_REGISTRATION:
            case EVENT_POLL_STATE_OPERATOR:
                ar = (AsyncResult) msg.obj;
                handlePollStateResult(msg.what, ar);
                break;

            case EVENT_POLL_STATE_NETWORK_SELECTION_MODE:
                if (DBG) log("EVENT_POLL_STATE_NETWORK_SELECTION_MODE");
                ar = (AsyncResult) msg.obj;
                if (mPhone.isPhoneTypeGsm()) {
                    handlePollStateResult(msg.what, ar);
                } else {
                    if (ar.exception == null && ar.result != null) {
                        ints = (int[])ar.result;
                        if (ints[0] == 1) {  // Manual selection.
                            mPhone.setNetworkSelectionModeAutomatic(null);
                        }
                    } else {
                        log("Unable to getNetworkSelectionMode");
                    }
                }
                break;

            case EVENT_NITZ_TIME: {
                ar = (AsyncResult) msg.obj;

                Object[] nitzArgs = (Object[])ar.result;
                String nitzString = (String)nitzArgs[0];
                long nitzReceiveTimeMs = ((Long)nitzArgs[1]).longValue();
                long ageMs = 0;
                if (nitzArgs.length >= 3) {
                    ageMs = ((Long)nitzArgs[2]).longValue();
                }

                setTimeFromNITZString(nitzString, nitzReceiveTimeMs, ageMs);
                break;
            }

            case EVENT_SIM_RECORDS_LOADED:
                log("EVENT_SIM_RECORDS_LOADED: what=" + msg.what);
                updatePhoneObject();
                updateOtaspState();
                if (mPhone.isPhoneTypeGsm()) {
                    mCdnr.updateEfFromUsim((SIMRecords) mIccRecords);
                    updateSpnDisplay();
                }
                break;

            case EVENT_LOCATION_UPDATES_ENABLED:
                ar = (AsyncResult) msg.obj;

                if (ar.exception == null) {
                    mRegStateManagers.get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                            .requestNetworkRegistrationInfo(NetworkRegistrationInfo.DOMAIN_CS,
                            obtainMessage(EVENT_GET_LOC_DONE, null));
                }
                break;

            case EVENT_SET_ALLOWED_NETWORK_TYPES:
                ar = (AsyncResult) msg.obj;
                // Don't care the result, only use for dereg network (COPS=2)
                message = obtainMessage(EVENT_RESET_ALLOWED_NETWORK_TYPES, ar.userObj);
                mCi.setAllowedNetworkTypesBitmap(mAllowedNetworkTypes, message);
                break;

            case EVENT_RESET_ALLOWED_NETWORK_TYPES:
                ar = (AsyncResult) msg.obj;
                if (ar.userObj != null) {
                    AsyncResult.forMessage(((Message) ar.userObj)).exception
                            = ar.exception;
                    ((Message) ar.userObj).sendToTarget();
                }
                break;

            case EVENT_GET_ALLOWED_NETWORK_TYPES:
                ar = (AsyncResult) msg.obj;

                if (ar.exception == null) {
                    mAllowedNetworkTypes = ((int[]) ar.result)[0];
                } else {
                    mAllowedNetworkTypes = RadioAccessFamily.getRafFromNetworkType(
                            RILConstants.NETWORK_MODE_GLOBAL);
                }

                message = obtainMessage(EVENT_SET_ALLOWED_NETWORK_TYPES, ar.userObj);
                int toggledNetworkType = RadioAccessFamily.getRafFromNetworkType(
                        RILConstants.NETWORK_MODE_GLOBAL);

                mCi.setAllowedNetworkTypesBitmap(toggledNetworkType, message);
                break;

            case EVENT_CHECK_REPORT_GPRS:
                if (mPhone.isPhoneTypeGsm() && mSS != null &&
                        !isGprsConsistent(mSS.getDataRegistrationState(), mSS.getState())) {

                    // Can't register data service while voice service is ok
                    // i.e. CREG is ok while CGREG is not
                    // possible a network or baseband side error
                    EventLog.writeEvent(EventLogTags.DATA_NETWORK_REGISTRATION_FAIL,
                            mSS.getOperatorNumeric(), getCidFromCellIdentity(mCellIdentity));
                    mReportedGprsNoReg = true;
                }
                mStartedGprsRegCheck = false;
                break;

            case EVENT_RESTRICTED_STATE_CHANGED:
                if (mPhone.isPhoneTypeGsm()) {
                    // This is a notification from
                    // CommandsInterface.setOnRestrictedStateChanged

                    if (DBG) log("EVENT_RESTRICTED_STATE_CHANGED");

                    ar = (AsyncResult) msg.obj;

                    onRestrictedStateChanged(ar);
                }
                break;

            case EVENT_REGISTER_DATA_NETWORK_EXISTING_CHANGED: {
                mPhone.getDataNetworkController().registerDataNetworkControllerCallback(
                        new DataNetworkControllerCallback(this::post) {
                        @Override
                        public void onAnyDataNetworkExistingChanged(boolean anyDataExisting) {
                            if (mAnyDataExisting != anyDataExisting) {
                                mAnyDataExisting = anyDataExisting;
                                log("onAnyDataNetworkExistingChanged: anyDataExisting="
                                        + anyDataExisting);
                                if (!mAnyDataExisting) {
                                    sendEmptyMessage(EVENT_ALL_DATA_DISCONNECTED);
                                }
                            }
                        }
                        });
                break;
            }
            case EVENT_ALL_DATA_DISCONNECTED:
                if (mPhone.isUsingNewDataStack()) {
                    log("EVENT_ALL_DATA_DISCONNECTED");
                    synchronized (this) {
                        if (mPendingRadioPowerOffAfterDataOff) {
                            if (DBG) log("EVENT_ALL_DATA_DISCONNECTED, turn radio off now.");
                            cancelPendingRadioPowerOff();
                            hangupAndPowerOff();
                        }
                    }
                    return;
                }
                int dds = SubscriptionManager.getDefaultDataSubscriptionId();
                ProxyController.getInstance().unregisterForAllDataDisconnected(dds, this);
                synchronized (this) {
                    if (mPendingRadioPowerOffAfterDataOff) {
                        if (DBG) log("EVENT_ALL_DATA_DISCONNECTED, turn radio off now.");
                        hangupAndPowerOff();
                        mPendingRadioPowerOffAfterDataOffTag += 1;
                        mPendingRadioPowerOffAfterDataOff = false;
                    } else {
                        log("EVENT_ALL_DATA_DISCONNECTED is stale");
                    }
                }
                break;

            case EVENT_CHANGE_IMS_STATE:
                if (DBG) log("EVENT_CHANGE_IMS_STATE:");

                setPowerStateToDesired();
                break;

            case EVENT_IMS_CAPABILITY_CHANGED:
                if (DBG) log("EVENT_IMS_CAPABILITY_CHANGED");
                updateSpnDisplay();
                mImsCapabilityChangedRegistrants.notifyRegistrants();
                break;

            case EVENT_IMS_SERVICE_STATE_CHANGED:
                if (DBG) log("EVENT_IMS_SERVICE_STATE_CHANGED");
                // IMS state will only affect the merged service state if the service state of
                // GsmCdma phone is not STATE_IN_SERVICE.
                if (mSS.getState() != ServiceState.STATE_IN_SERVICE) {
                    mPhone.notifyServiceStateChanged(mPhone.getServiceState());
                }
                break;

            //CDMA
            case EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED:
                handleCdmaSubscriptionSource(mCdmaSSM.getCdmaSubscriptionSource());
                break;

            case EVENT_RUIM_READY:
                if (mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    // Subscription will be read from SIM I/O
                    if (DBG) log("Receive EVENT_RUIM_READY");
                    pollStateInternal(false);
                } else {
                    if (DBG) log("Receive EVENT_RUIM_READY and Send Request getCDMASubscription.");
                    getSubscriptionInfoAndStartPollingThreads();
                }

                // Only support automatic selection mode in CDMA.
                mCi.getNetworkSelectionMode(obtainMessage(EVENT_POLL_STATE_NETWORK_SELECTION_MODE));

                break;

            case EVENT_NV_READY:
                updatePhoneObject();

                // Only support automatic selection mode in CDMA.
                mCi.getNetworkSelectionMode(obtainMessage(EVENT_POLL_STATE_NETWORK_SELECTION_MODE));

                // For Non-RUIM phones, the subscription information is stored in
                // Non Volatile. Here when Non-Volatile is ready, we can poll the CDMA
                // subscription info.
                getSubscriptionInfoAndStartPollingThreads();
                break;

            case EVENT_POLL_STATE_CDMA_SUBSCRIPTION: // Handle RIL_CDMA_SUBSCRIPTION
                if (!mPhone.isPhoneTypeGsm()) {
                    ar = (AsyncResult) msg.obj;

                    if (ar.exception == null) {
                        String cdmaSubscription[] = (String[]) ar.result;
                        if (cdmaSubscription != null && cdmaSubscription.length >= 5) {
                            mMdn = cdmaSubscription[0];
                            parseSidNid(cdmaSubscription[1], cdmaSubscription[2]);

                            mMin = cdmaSubscription[3];
                            mPrlVersion = cdmaSubscription[4];
                            if (DBG) log("GET_CDMA_SUBSCRIPTION: MDN=" + mMdn);

                            mIsMinInfoReady = true;

                            updateOtaspState();
                            // Notify apps subscription info is ready
                            notifyCdmaSubscriptionInfoReady();

                            if (!mIsSubscriptionFromRuim && mIccRecords != null) {
                                if (DBG) {
                                    log("GET_CDMA_SUBSCRIPTION set imsi in mIccRecords");
                                }
                                mIccRecords.setImsi(getImsi());
                            } else {
                                if (DBG) {
                                    log("GET_CDMA_SUBSCRIPTION either mIccRecords is null or NV " +
                                            "type device - not setting Imsi in mIccRecords");
                                }
                            }
                        } else {
                            if (DBG) {
                                log("GET_CDMA_SUBSCRIPTION: error parsing cdmaSubscription " +
                                        "params num=" + cdmaSubscription.length);
                            }
                        }
                    }
                }
                break;

            case EVENT_RUIM_RECORDS_LOADED:
                if (!mPhone.isPhoneTypeGsm()) {
                    log("EVENT_RUIM_RECORDS_LOADED: what=" + msg.what);
                    mCdnr.updateEfFromRuim((RuimRecords) mIccRecords);
                    updatePhoneObject();
                    if (mPhone.isPhoneTypeCdma()) {
                        updateSpnDisplay();
                    } else {
                        RuimRecords ruim = (RuimRecords) mIccRecords;
                        if (ruim != null) {
                            // Do not wait for RUIM to be provisioned before using mdn. Line1Number
                            // can be queried before that and mdn may still be available.
                            // Also note that any special casing is not done in getMdnNumber() as it
                            // may be called on another thread, so simply doing a read operation
                            // there.
                            mMdn = ruim.getMdn();
                            if (ruim.isProvisioned()) {
                                mMin = ruim.getMin();
                                parseSidNid(ruim.getSid(), ruim.getNid());
                                mPrlVersion = ruim.getPrlVersion();
                                mIsMinInfoReady = true;
                            }
                            updateOtaspState();
                            // Notify apps subscription info is ready
                            notifyCdmaSubscriptionInfoReady();
                        }
                        // SID/NID/PRL is loaded. Poll service state
                        // again to update to the roaming state with
                        // the latest variables.
                        pollStateInternal(false);
                    }
                }
                break;
            case EVENT_OTA_PROVISION_STATUS_CHANGE:
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    ints = (int[]) ar.result;
                    int otaStatus = ints[0];
                    if (otaStatus == Phone.CDMA_OTA_PROVISION_STATUS_COMMITTED
                            || otaStatus == Phone.CDMA_OTA_PROVISION_STATUS_OTAPA_STOPPED) {
                        if (DBG) log("EVENT_OTA_PROVISION_STATUS_CHANGE: Complete, Reload MDN");
                        mCi.getCDMASubscription( obtainMessage(EVENT_POLL_STATE_CDMA_SUBSCRIPTION));
                    }
                }
                break;

            case EVENT_CDMA_PRL_VERSION_CHANGED:
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    ints = (int[]) ar.result;
                    mPrlVersion = Integer.toString(ints[0]);
                }
                break;

            case EVENT_RADIO_POWER_FROM_CARRIER:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    boolean enable = (boolean) ar.result;
                    if (DBG) log("EVENT_RADIO_POWER_FROM_CARRIER: " + enable);
                    setRadioPowerFromCarrier(enable);
                }
                break;

            case EVENT_PHYSICAL_CHANNEL_CONFIG:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    List<PhysicalChannelConfig> list = (List<PhysicalChannelConfig>) ar.result;
                    if (VDBG) {
                        log("EVENT_PHYSICAL_CHANNEL_CONFIG: size=" + list.size() + " list="
                                + list);
                    }
                    mLastPhysicalChannelConfigList = list;
                    boolean hasChanged = false;
                    if (updateNrStateFromPhysicalChannelConfigs(list, mSS)) {
                        mNrStateChangedRegistrants.notifyRegistrants();
                        hasChanged = true;
                    }
                    if (updateNrFrequencyRangeFromPhysicalChannelConfigs(list, mSS)) {
                        mNrFrequencyChangedRegistrants.notifyRegistrants();
                        hasChanged = true;
                    }
                    hasChanged |= RatRatcheter
                            .updateBandwidths(getBandwidthsFromConfigs(list), mSS);

                    mPhone.notifyPhysicalChannelConfig(list);
                    // Notify NR frequency, NR connection status or bandwidths changed.
                    if (hasChanged) {
                        mPhone.notifyServiceStateChanged(mPhone.getServiceState());
                        TelephonyMetrics.getInstance().writeServiceStateChanged(
                                mPhone.getPhoneId(), mSS);
                        mPhone.getVoiceCallSessionStats().onServiceStateChanged(mSS);
                        mServiceStateStats.onServiceStateChanged(mSS);
                    }
                }
                break;

            case EVENT_CELL_LOCATION_RESPONSE:
                ar = (AsyncResult) msg.obj;
                if (ar == null) {
                    loge("Invalid null response to getCellIdentity!");
                    break;
                }
                // This response means that the correct CellInfo is already cached; thus we
                // can rely on the last cell info to already contain any cell info that is
                // available, which means that we can return the result of the existing
                // getCellIdentity() function without any additional processing here.
                Message rspRspMsg = (Message) ar.userObj;
                AsyncResult.forMessage(rspRspMsg, getCellIdentity(), ar.exception);
                rspRspMsg.sendToTarget();
                break;

            case EVENT_CARRIER_CONFIG_CHANGED:
                onCarrierConfigChanged();
                break;

            case EVENT_POLL_STATE_REQUEST:
                pollStateInternal(false);
                break;

            case EVENT_POWER_OFF_RADIO_IMS_DEREG_TIMEOUT: {
                if (DBG) log("EVENT_POWER_OFF_RADIO_IMS_DEREG_TIMEOUT triggered");
                powerOffRadioSafely();
                break;
            }

            case EVENT_RESET_LAST_KNOWN_CELL_IDENTITY: {
                if (DBG) log("EVENT_RESET_LAST_KNOWN_CELL_IDENTITY triggered");
                mLastKnownCellIdentity = null;
                break;
            }

            case EVENT_TELECOM_VOICE_SERVICE_STATE_OVERRIDE_CHANGED:
                if (DBG) log("EVENT_TELECOM_VOICE_SERVICE_STATE_OVERRIDE_CHANGED");
                // Similar to IMS, OTT voice state will only affect the merged service state if the
                // CS voice service state of GsmCdma phone is not STATE_IN_SERVICE.
                if (mSS.getState() != ServiceState.STATE_IN_SERVICE) {
                    mPhone.notifyServiceStateChanged(mPhone.getServiceState());
                }
                break;

            default:
                log("Unhandled message with number: " + msg.what);
                break;
        }
    }

    private boolean isSimAbsent() {
        boolean simAbsent;
        if (mUiccController == null) {
            simAbsent = true;
        } else {
            UiccCard uiccCard = mUiccController.getUiccCard(mPhone.getPhoneId());
            if (uiccCard == null) {
                simAbsent = true;
            } else {
                simAbsent = (uiccCard.getCardState() == CardState.CARDSTATE_ABSENT);
            }
        }
        return simAbsent;
    }

    private static int[] getBandwidthsFromConfigs(List<PhysicalChannelConfig> list) {
        return list.stream()
                .map(PhysicalChannelConfig::getCellBandwidthDownlinkKhz)
                .mapToInt(Integer::intValue)
                .toArray();
    }

    protected boolean isSidsAllZeros() {
        if (mHomeSystemId != null) {
            for (int i=0; i < mHomeSystemId.length; i++) {
                if (mHomeSystemId[i] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return a copy of the current service state.
     */
    public ServiceState getServiceState() {
        return new ServiceState(mSS);
    }

    /**
     * Check whether a specified system ID that matches one of the home system IDs.
     */
    private boolean isHomeSid(int sid) {
        if (mHomeSystemId != null) {
            for (int i=0; i < mHomeSystemId.length; i++) {
                if (sid == mHomeSystemId[i]) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getMdnNumber() {
        return mMdn;
    }

    public String getCdmaMin() {
        return mMin;
    }

    /** Returns null if NV is not yet ready */
    public String getPrlVersion() {
        return mPrlVersion;
    }

    /**
     * Returns IMSI as MCC + MNC + MIN
     */
    public String getImsi() {
        // TODO: When RUIM is enabled, IMSI will come from RUIM not build-time props.
        String operatorNumeric = ((TelephonyManager) mPhone.getContext()
                .getSystemService(Context.TELEPHONY_SERVICE))
                .getSimOperatorNumericForPhone(mPhone.getPhoneId());

        if (!TextUtils.isEmpty(operatorNumeric) && getCdmaMin() != null) {
            return (operatorNumeric + getCdmaMin());
        } else {
            return null;
        }
    }

    /**
     * Check if subscription data has been assigned to mMin
     *
     * return true if MIN info is ready; false otherwise.
     */
    public boolean isMinInfoReady() {
        return mIsMinInfoReady;
    }

    /**
     * Returns OTASP_UNKNOWN, OTASP_UNINITIALIZED, OTASP_NEEDED or OTASP_NOT_NEEDED
     */
    public int getOtasp() {
        int provisioningState;
        // if sim is not loaded, return otasp uninitialized
        if(!mPhone.getIccRecordsLoaded()) {
            if(DBG) log("getOtasp: otasp uninitialized due to sim not loaded");
            return TelephonyManager.OTASP_UNINITIALIZED;
        }
        // if voice tech is Gsm, return otasp not needed
        if(mPhone.isPhoneTypeGsm()) {
            if(DBG) log("getOtasp: otasp not needed for GSM");
            return TelephonyManager.OTASP_NOT_NEEDED;
        }
        // for ruim, min is null means require otasp.
        if (mIsSubscriptionFromRuim && mMin == null) {
            return TelephonyManager.OTASP_NEEDED;
        }
        if (mMin == null || (mMin.length() < 6)) {
            if (DBG) log("getOtasp: bad mMin='" + mMin + "'");
            provisioningState = TelephonyManager.OTASP_UNKNOWN;
        } else {
            if ((mMin.equals(UNACTIVATED_MIN_VALUE)
                    || mMin.substring(0,6).equals(UNACTIVATED_MIN2_VALUE))
                    || SystemProperties.getBoolean("test_cdma_setup", false)) {
                provisioningState = TelephonyManager.OTASP_NEEDED;
            } else {
                provisioningState = TelephonyManager.OTASP_NOT_NEEDED;
            }
        }
        if (DBG) log("getOtasp: state=" + provisioningState);
        return provisioningState;
    }

    protected void parseSidNid (String sidStr, String nidStr) {
        if (sidStr != null) {
            String[] sid = sidStr.split(",");
            mHomeSystemId = new int[sid.length];
            for (int i = 0; i < sid.length; i++) {
                try {
                    mHomeSystemId[i] = Integer.parseInt(sid[i]);
                } catch (NumberFormatException ex) {
                    loge("error parsing system id: " + ex);
                }
            }
        }
        if (DBG) log("CDMA_SUBSCRIPTION: SID=" + sidStr);

        if (nidStr != null) {
            String[] nid = nidStr.split(",");
            mHomeNetworkId = new int[nid.length];
            for (int i = 0; i < nid.length; i++) {
                try {
                    mHomeNetworkId[i] = Integer.parseInt(nid[i]);
                } catch (NumberFormatException ex) {
                    loge("CDMA_SUBSCRIPTION: error parsing network id: " + ex);
                }
            }
        }
        if (DBG) log("CDMA_SUBSCRIPTION: NID=" + nidStr);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void updateOtaspState() {
        int otaspMode = getOtasp();
        int oldOtaspMode = mCurrentOtaspMode;
        mCurrentOtaspMode = otaspMode;

        if (oldOtaspMode != mCurrentOtaspMode) {
            if (DBG) {
                log("updateOtaspState: call notifyOtaspChanged old otaspMode=" +
                        oldOtaspMode + " new otaspMode=" + mCurrentOtaspMode);
            }
            mPhone.notifyOtaspChanged(mCurrentOtaspMode);
        }
    }

    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        mLastNitzData = null;
        mNitzState.handleAirplaneModeChanged(isAirplaneModeOn);
        mAirplaneModeChangedRegistrants.notifyResult(isAirplaneModeOn);
    }

    protected Phone getPhone() {
        return mPhone;
    }

    protected void handlePollStateResult(int what, AsyncResult ar) {
        // Ignore stale requests from last poll
        if (ar.userObj != mPollingContext) return;

        if (ar.exception != null) {
            CommandException.Error err = null;

            if (ar.exception instanceof IllegalStateException) {
                log("handlePollStateResult exception " + ar.exception);
            }

            if (ar.exception instanceof CommandException) {
                err = ((CommandException)(ar.exception)).getCommandError();
            }

            if (mCi.getRadioState() != TelephonyManager.RADIO_POWER_ON) {
                log("handlePollStateResult: Invalid response due to radio off or unavailable. "
                        + "Set ServiceState to out of service.");
                pollStateInternal(false);
                return;
            }

            if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                loge("handlePollStateResult: RIL returned RADIO_NOT_AVAILABLE when radio is on.");
                cancelPollState();
                return;
            }

            if (err != CommandException.Error.OP_NOT_ALLOWED_BEFORE_REG_NW) {
                loge("handlePollStateResult: RIL returned an error where it must succeed: "
                        + ar.exception);
            }
        } else try {
            handlePollStateResultMessage(what, ar);
        } catch (RuntimeException ex) {
            loge("Exception while polling service state. Probably malformed RIL response." + ex);
        }

        mPollingContext[0]--;

        if (mPollingContext[0] == 0) {
            mNewSS.setEmergencyOnly(mEmergencyOnly);
            combinePsRegistrationStates(mNewSS);
            updateOperatorNameForServiceState(mNewSS);
            if (mPhone.isPhoneTypeGsm()) {
                updateRoamingState();
            } else {
                boolean namMatch = false;
                if (!isSidsAllZeros() && isHomeSid(mNewSS.getCdmaSystemId())) {
                    namMatch = true;
                }

                // Setting SS Roaming (general)
                if (mIsSubscriptionFromRuim) {
                    boolean isRoamingBetweenOperators = isRoamingBetweenOperators(
                            mNewSS.getVoiceRoaming(), mNewSS);
                    if (isRoamingBetweenOperators != mNewSS.getVoiceRoaming()) {
                        log("isRoamingBetweenOperators=" + isRoamingBetweenOperators
                                + ". Override CDMA voice roaming to " + isRoamingBetweenOperators);
                        mNewSS.setVoiceRoaming(isRoamingBetweenOperators);
                    }
                }
                /**
                 * For CDMA, voice and data should have the same roaming status.
                 * If voice is not in service, use TSB58 roaming indicator to set
                 * data roaming status. If TSB58 roaming indicator is not in the
                 * carrier-specified list of ERIs for home system then set roaming.
                 */
                final int dataRat = getRilDataRadioTechnologyForWwan(mNewSS);
                if (ServiceState.isCdma(dataRat)) {
                    final boolean isVoiceInService =
                            (mNewSS.getState() == ServiceState.STATE_IN_SERVICE);
                    if (isVoiceInService) {
                        boolean isVoiceRoaming = mNewSS.getVoiceRoaming();
                        if (mNewSS.getDataRoaming() != isVoiceRoaming) {
                            log("Data roaming != Voice roaming. Override data roaming to "
                                    + isVoiceRoaming);
                            mNewSS.setDataRoaming(isVoiceRoaming);
                        }
                    } else {
                        /**
                         * As per VoiceRegStateResult from radio types.hal the TSB58
                         * Roaming Indicator shall be sent if device is registered
                         * on a CDMA or EVDO system.
                         */
                        boolean isRoamIndForHomeSystem = isRoamIndForHomeSystem(mRoamingIndicator);
                        if (mNewSS.getDataRoaming() == isRoamIndForHomeSystem) {
                            log("isRoamIndForHomeSystem=" + isRoamIndForHomeSystem
                                    + ", override data roaming to " + !isRoamIndForHomeSystem);
                            mNewSS.setDataRoaming(!isRoamIndForHomeSystem);
                        }
                    }
                }

                // Setting SS CdmaRoamingIndicator and CdmaDefaultRoamingIndicator
                mNewSS.setCdmaDefaultRoamingIndicator(mDefaultRoamingIndicator);
                mNewSS.setCdmaRoamingIndicator(mRoamingIndicator);
                boolean isPrlLoaded = true;
                if (TextUtils.isEmpty(mPrlVersion)) {
                    isPrlLoaded = false;
                }
                if (!isPrlLoaded || (mNewSS.getRilVoiceRadioTechnology()
                        == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)) {
                    log("Turn off roaming indicator if !isPrlLoaded or voice RAT is unknown");
                    mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_OFF);
                } else if (!isSidsAllZeros()) {
                    if (!namMatch && !mIsInPrl) {
                        // Use default
                        mNewSS.setCdmaRoamingIndicator(mDefaultRoamingIndicator);
                    } else if (namMatch && !mIsInPrl) {
                        // TODO: remove when we handle roaming on LTE/NR on CDMA+LTE phones
                        if (ServiceState.isPsOnlyTech(mNewSS.getRilVoiceRadioTechnology())) {
                            log("Turn off roaming indicator as voice is LTE or NR");
                            mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_OFF);
                        } else {
                            mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_FLASH);
                        }
                    } else if (!namMatch && mIsInPrl) {
                        // Use the one from PRL/ERI
                        mNewSS.setCdmaRoamingIndicator(mRoamingIndicator);
                    } else {
                        // It means namMatch && mIsInPrl
                        if ((mRoamingIndicator <= 2)) {
                            mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_OFF);
                        } else {
                            // Use the one from PRL/ERI
                            mNewSS.setCdmaRoamingIndicator(mRoamingIndicator);
                        }
                    }
                }

                if (mEriManager != null) {
                    int roamingIndicator = mNewSS.getCdmaRoamingIndicator();
                    mNewSS.setCdmaEriIconIndex(mEriManager.getCdmaEriIconIndex(roamingIndicator,
                            mDefaultRoamingIndicator));
                    mNewSS.setCdmaEriIconMode(mEriManager.getCdmaEriIconMode(roamingIndicator,
                            mDefaultRoamingIndicator));
                }

                // NOTE: Some operator may require overriding mCdmaRoaming
                // (set by the modem), depending on the mRoamingIndicator.

                if (DBG) {
                    log("Set CDMA Roaming Indicator to: " + mNewSS.getCdmaRoamingIndicator()
                            + ". voiceRoaming = " + mNewSS.getVoiceRoaming()
                            + ". dataRoaming = " + mNewSS.getDataRoaming()
                            + ", isPrlLoaded = " + isPrlLoaded
                            + ". namMatch = " + namMatch + " , mIsInPrl = " + mIsInPrl
                            + ", mRoamingIndicator = " + mRoamingIndicator
                            + ", mDefaultRoamingIndicator= " + mDefaultRoamingIndicator);
                }
            }
            pollStateDone();
        }

    }

    /**
     * Set roaming state when cdmaRoaming is true and ons is different from spn
     * @param cdmaRoaming TS 27.007 7.2 CREG registered roaming
     * @param s ServiceState hold current ons
     * @return true for roaming state set
     */
    private boolean isRoamingBetweenOperators(boolean cdmaRoaming, ServiceState s) {
        return cdmaRoaming && !isSameOperatorNameFromSimAndSS(s);
    }

    private boolean updateNrFrequencyRangeFromPhysicalChannelConfigs(
            List<PhysicalChannelConfig> physicalChannelConfigs, ServiceState ss) {
        int newFrequencyRange = ServiceState.FREQUENCY_RANGE_UNKNOWN;
        if (physicalChannelConfigs != null) {
            for (PhysicalChannelConfig config : physicalChannelConfigs) {
                if (isNrPhysicalChannelConfig(config) && isInternetPhysicalChannelConfig(config)) {
                    // Update the NR frequency range if there is an internet data connection
                    // associated with this NR physical channel channel config.
                    newFrequencyRange = ServiceState.getBetterNRFrequencyRange(
                            newFrequencyRange, config.getFrequencyRange());
                    break;
                }
            }
        }

        boolean hasChanged = newFrequencyRange != ss.getNrFrequencyRange();
        ss.setNrFrequencyRange(newFrequencyRange);
        return hasChanged;
    }

    private boolean updateNrStateFromPhysicalChannelConfigs(
            List<PhysicalChannelConfig> configs, ServiceState ss) {
        NetworkRegistrationInfo regInfo = ss.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        if (regInfo == null || configs == null) return false;

        boolean hasNrSecondaryServingCell = false;
        for (PhysicalChannelConfig config : configs) {
            if (isNrPhysicalChannelConfig(config) && isInternetPhysicalChannelConfig(config)
                    && config.getConnectionStatus()
                    == PhysicalChannelConfig.CONNECTION_SECONDARY_SERVING) {
                hasNrSecondaryServingCell = true;
                break;
            }
        }

        int oldNrState = regInfo.getNrState();
        int newNrState;
        if (hasNrSecondaryServingCell) {
            newNrState = NetworkRegistrationInfo.NR_STATE_CONNECTED;
        } else {
            regInfo.updateNrState();
            newNrState = regInfo.getNrState();
        }

        boolean hasChanged = newNrState != oldNrState;
        regInfo.setNrState(newNrState);
        ss.addNetworkRegistrationInfo(regInfo);
        return hasChanged;
    }

    private boolean isNrPhysicalChannelConfig(PhysicalChannelConfig config) {
        return config.getNetworkType() == TelephonyManager.NETWORK_TYPE_NR;
    }

    private boolean isInternetPhysicalChannelConfig(PhysicalChannelConfig config) {
        for (int cid : config.getContextIds()) {
            if (mPhone.isUsingNewDataStack()) {
                if (mPhone.getDataNetworkController().isInternetNetwork(cid)) {
                    return true;
                }
            } else {
                DataConnection dc = mPhone.getDcTracker(
                        AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                        .getDataConnectionByContextId(cid);
                if (dc != null && dc.getNetworkCapabilities().hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This combine PS registration states from cellular and IWLAN and generates the final data
     * reg state and rat for backward compatibility purpose. In reality there should be two separate
     * registration states for cellular and IWLAN, but in legacy mode, if the device camps on IWLAN,
     * the IWLAN registration states overwrites the service states. This method is to simulate that
     * behavior.
     *
     * @param serviceState The service state having combined registration states.
     */
    private void combinePsRegistrationStates(ServiceState serviceState) {
        NetworkRegistrationInfo wlanPsRegState = serviceState.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WLAN);
        NetworkRegistrationInfo wwanPsRegState = serviceState.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);

        // Check if any APN is preferred on IWLAN.
        boolean isIwlanPreferred = mAccessNetworksManager.isAnyApnOnIwlan();
        serviceState.setIwlanPreferred(isIwlanPreferred);
        if (wlanPsRegState != null
                && wlanPsRegState.getAccessNetworkTechnology()
                == TelephonyManager.NETWORK_TYPE_IWLAN
                && wlanPsRegState.getRegistrationState()
                == NetworkRegistrationInfo.REGISTRATION_STATE_HOME
                && isIwlanPreferred) {
            serviceState.setDataRegState(ServiceState.STATE_IN_SERVICE);
        } else if (wwanPsRegState != null) {
            // If the device is not camped on IWLAN, then we use cellular PS registration state
            // to compute reg state and rat.
            int regState = wwanPsRegState.getRegistrationState();
            serviceState.setDataRegState(regCodeToServiceState(regState));
        }
        if (DBG) {
            log("combinePsRegistrationStates: " + serviceState);
        }
    }

    protected void handlePollStateResultMessage(int what, AsyncResult ar) {
        int ints[];
        switch (what) {
            case EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION: {
                NetworkRegistrationInfo networkRegState = (NetworkRegistrationInfo) ar.result;
                VoiceSpecificRegistrationInfo voiceSpecificStates =
                        networkRegState.getVoiceSpecificInfo();

                int registrationState = networkRegState.getRegistrationState();
                int cssIndicator = voiceSpecificStates.cssSupported ? 1 : 0;
                int newVoiceRat = ServiceState.networkTypeToRilRadioTechnology(
                        networkRegState.getAccessNetworkTechnology());
                mNewSS.setVoiceRegState(regCodeToServiceState(registrationState));
                mNewSS.setCssIndicator(cssIndicator);
                mNewSS.addNetworkRegistrationInfo(networkRegState);

                setPhyCellInfoFromCellIdentity(mNewSS, networkRegState.getCellIdentity());

                //Denial reason if registrationState = 3
                int reasonForDenial = networkRegState.getRejectCause();
                mCSEmergencyOnly = networkRegState.isEmergencyEnabled();
                mEmergencyOnly = (mCSEmergencyOnly || mPSEmergencyOnly);
                if (mPhone.isPhoneTypeGsm()) {

                    mGsmVoiceRoaming = regCodeIsRoaming(registrationState);
                    mNewRejectCode = reasonForDenial;
                } else {
                    int roamingIndicator = voiceSpecificStates.roamingIndicator;

                    //Indicates if current system is in PR
                    int systemIsInPrl = voiceSpecificStates.systemIsInPrl;

                    //Is default roaming indicator from PRL
                    int defaultRoamingIndicator = voiceSpecificStates.defaultRoamingIndicator;

                    mRegistrationState = registrationState;
                    // When registration state is roaming and TSB58
                    // roaming indicator is not in the carrier-specified
                    // list of ERIs for home system, mCdmaRoaming is true.
                    boolean cdmaRoaming =
                            regCodeIsRoaming(registrationState)
                                    && !isRoamIndForHomeSystem(roamingIndicator);
                    mNewSS.setVoiceRoaming(cdmaRoaming);
                    mRoamingIndicator = roamingIndicator;
                    mIsInPrl = (systemIsInPrl == 0) ? false : true;
                    mDefaultRoamingIndicator = defaultRoamingIndicator;

                    int systemId = 0;
                    int networkId = 0;
                    CellIdentity cellIdentity = networkRegState.getCellIdentity();
                    if (cellIdentity != null && cellIdentity.getType() == CellInfoType.CDMA) {
                        systemId = ((CellIdentityCdma) cellIdentity).getSystemId();
                        networkId = ((CellIdentityCdma) cellIdentity).getNetworkId();
                    }
                    mNewSS.setCdmaSystemAndNetworkId(systemId, networkId);

                    if (reasonForDenial == 0) {
                        mRegistrationDeniedReason = ServiceStateTracker.REGISTRATION_DENIED_GEN;
                    } else if (reasonForDenial == 1) {
                        mRegistrationDeniedReason = ServiceStateTracker.REGISTRATION_DENIED_AUTH;
                    } else {
                        mRegistrationDeniedReason = "";
                    }

                    if (mRegistrationState == 3) {
                        if (DBG) log("Registration denied, " + mRegistrationDeniedReason);
                    }
                }

                if (DBG) {
                    log("handlePollStateResultMessage: CS cellular. " + networkRegState);
                }
                break;
            }

            case EVENT_POLL_STATE_PS_IWLAN_REGISTRATION: {
                NetworkRegistrationInfo networkRegState = (NetworkRegistrationInfo) ar.result;
                mNewSS.addNetworkRegistrationInfo(networkRegState);

                if (DBG) {
                    log("handlePollStateResultMessage: PS IWLAN. " + networkRegState);
                }
                break;
            }

            case EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION: {
                NetworkRegistrationInfo networkRegState = (NetworkRegistrationInfo) ar.result;
                mNewSS.addNetworkRegistrationInfo(networkRegState);
                DataSpecificRegistrationInfo dataSpecificStates =
                        networkRegState.getDataSpecificInfo();
                int registrationState = networkRegState.getRegistrationState();
                int serviceState = regCodeToServiceState(registrationState);
                int newDataRat = ServiceState.networkTypeToRilRadioTechnology(
                        networkRegState.getAccessNetworkTechnology());

                if (DBG) {
                    log("handlePollStateResultMessage: PS cellular. " + networkRegState);
                }

                // When we receive OOS reset the PhyChanConfig list so that non-return-to-idle
                // implementers of PhyChanConfig unsol will not carry forward a CA report
                // (2 or more cells) to a new cell if they camp for emergency service only.
                if (serviceState == ServiceState.STATE_OUT_OF_SERVICE) {
                    mLastPhysicalChannelConfigList = null;
                }

                mPSEmergencyOnly = networkRegState.isEmergencyEnabled();
                mEmergencyOnly = (mCSEmergencyOnly || mPSEmergencyOnly);
                if (mPhone.isPhoneTypeGsm()) {
                    mNewReasonDataDenied = networkRegState.getRejectCause();
                    mNewMaxDataCalls = dataSpecificStates.maxDataCalls;
                    mGsmDataRoaming = regCodeIsRoaming(registrationState);
                    // Save the data roaming state reported by modem registration before resource
                    // overlay or carrier config possibly overrides it.
                    mNewSS.setDataRoamingFromRegistration(mGsmDataRoaming);
                } else if (mPhone.isPhoneTypeCdma()) {
                    boolean isDataRoaming = regCodeIsRoaming(registrationState);
                    mNewSS.setDataRoaming(isDataRoaming);
                    // Save the data roaming state reported by modem registration before resource
                    // overlay or carrier config possibly overrides it.
                    mNewSS.setDataRoamingFromRegistration(isDataRoaming);
                } else {

                    // If the unsolicited signal strength comes just before data RAT family changes
                    // (i.e. from UNKNOWN to LTE/NR, CDMA to LTE/NR, LTE/NR to CDMA), the signal bar
                    // might display the wrong information until the next unsolicited signal
                    // strength information coming from the modem, which might take a long time to
                    // come or even not come at all.  In order to provide the best user experience,
                    // we query the latest signal information so it will show up on the UI on time.
                    int oldDataRAT = getRilDataRadioTechnologyForWwan(mSS);
                    if (((oldDataRAT == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)
                            && (newDataRat != ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN))
                            || (ServiceState.isCdma(oldDataRAT)
                            && ServiceState.isPsOnlyTech(newDataRat))
                            || (ServiceState.isPsOnlyTech(oldDataRAT)
                            && ServiceState.isCdma(newDataRat))) {
                        mPhone.getSignalStrengthController().getSignalStrengthFromCi();
                    }

                    // voice roaming state in done while handling EVENT_POLL_STATE_REGISTRATION_CDMA
                    boolean isDataRoaming = regCodeIsRoaming(registrationState);
                    mNewSS.setDataRoaming(isDataRoaming);
                    // Save the data roaming state reported by modem registration before resource
                    // overlay or carrier config possibly overrides it.
                    mNewSS.setDataRoamingFromRegistration(isDataRoaming);
                }

                mPhone.getSignalStrengthController().updateServiceStateArfcnRsrpBoost(mNewSS,
                        networkRegState.getCellIdentity());
                break;
            }

            case EVENT_POLL_STATE_OPERATOR: {
                if (mPhone.isPhoneTypeGsm()) {
                    String opNames[] = (String[]) ar.result;

                    if (opNames != null && opNames.length >= 3) {
                        mNewSS.setOperatorAlphaLongRaw(opNames[0]);
                        mNewSS.setOperatorAlphaShortRaw(opNames[1]);
                        // FIXME: Giving brandOverride higher precedence, is this desired?
                        String brandOverride = getOperatorBrandOverride();
                        mCdnr.updateEfForBrandOverride(brandOverride);
                        if (brandOverride != null) {
                            log("EVENT_POLL_STATE_OPERATOR: use brandOverride=" + brandOverride);
                            mNewSS.setOperatorName(brandOverride, brandOverride, opNames[2]);
                        } else {
                            mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                        }
                    }
                } else {
                    String opNames[] = (String[])ar.result;

                    if (opNames != null && opNames.length >= 3) {
                        // TODO: Do we care about overriding in this case.
                        // If the NUMERIC field isn't valid use PROPERTY_CDMA_HOME_OPERATOR_NUMERIC
                        if ((opNames[2] == null) || (opNames[2].length() < 5)
                                || ("00000".equals(opNames[2]))) {
                            opNames[2] = SystemProperties.get(
                                    GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, "00000");
                            if (DBG) {
                                log("RIL_REQUEST_OPERATOR.response[2], the numeric, " +
                                        " is bad. Using SystemProperties '" +
                                        GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC +
                                        "'= " + opNames[2]);
                            }
                        }

                        if (!mIsSubscriptionFromRuim) {
                            // NV device (as opposed to CSIM)
                            mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                        } else {
                            String brandOverride = getOperatorBrandOverride();
                            mCdnr.updateEfForBrandOverride(brandOverride);
                            if (brandOverride != null) {
                                mNewSS.setOperatorName(brandOverride, brandOverride, opNames[2]);
                            } else {
                                mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                            }
                        }
                    } else {
                        if (DBG) log("EVENT_POLL_STATE_OPERATOR_CDMA: error parsing opNames");
                    }
                }
                break;
            }

            case EVENT_POLL_STATE_NETWORK_SELECTION_MODE: {
                ints = (int[])ar.result;
                mNewSS.setIsManualSelection(ints[0] == 1);
                if ((ints[0] == 1) && (mPhone.shouldForceAutoNetworkSelect())) {
                        /*
                         * modem is currently in manual selection but manual
                         * selection is not allowed in the current mode so
                         * switch to automatic registration
                         */
                    mPhone.setNetworkSelectionModeAutomatic (null);
                    log(" Forcing Automatic Network Selection, " +
                            "manual selection is not allowed");
                }
                break;
            }

            default:
                loge("handlePollStateResultMessage: Unexpected RIL response received: " + what);
        }
    }

    private static boolean isValidLteBandwidthKhz(int bandwidth) {
        // Valid bandwidths, see 3gpp 36.101 sec. 5.6
        switch (bandwidth) {
            case 1400:
            case 3000:
            case 5000:
            case 10000:
            case 15000:
            case 20000:
                return true;
            default:
                return false;
        }
    }

    private static boolean isValidNrBandwidthKhz(int bandwidth) {
        // Valid bandwidths, see 3gpp 38.101 sec 5.3
        switch (bandwidth) {
            case 5000:
            case 10000:
            case 15000:
            case 20000:
            case 25000:
            case 30000:
            case 40000:
            case 50000:
            case 60000:
            case 70000:
            case 80000:
            case 90000:
            case 100000:
                return true;
            default:
                return false;
        }
    }

    /**
     * Extract the CID/CI for GSM/UTRA/EUTRA
     *
     * @returns the cell ID (unique within a PLMN for a given tech) or -1 if invalid
     */
    private static long getCidFromCellIdentity(CellIdentity id) {
        if (id == null) return -1;
        long cid = -1;
        switch(id.getType()) {
            case CellInfo.TYPE_GSM: cid = ((CellIdentityGsm) id).getCid(); break;
            case CellInfo.TYPE_WCDMA: cid = ((CellIdentityWcdma) id).getCid(); break;
            case CellInfo.TYPE_TDSCDMA: cid = ((CellIdentityTdscdma) id).getCid(); break;
            case CellInfo.TYPE_LTE: cid = ((CellIdentityLte) id).getCi(); break;
            case CellInfo.TYPE_NR: cid = ((CellIdentityNr) id).getNci(); break;
            default: break;
        }
        // If the CID is unreported
        if (cid == (id.getType() == CellInfo.TYPE_NR
                ? CellInfo.UNAVAILABLE_LONG : CellInfo.UNAVAILABLE)) {
            cid = -1;
        }

        return cid;
    }

    //TODO: Move this and getCidFromCellIdentity to CellIdentityUtils.
    private static int getAreaCodeFromCellIdentity(CellIdentity id) {
        if (id == null) return CellInfo.UNAVAILABLE;
        switch(id.getType()) {
            case CellInfo.TYPE_GSM: return ((CellIdentityGsm) id).getLac();
            case CellInfo.TYPE_WCDMA: return ((CellIdentityWcdma) id).getLac();
            case CellInfo.TYPE_TDSCDMA: return ((CellIdentityTdscdma) id).getLac();
            case CellInfo.TYPE_LTE: return ((CellIdentityLte) id).getTac();
            case CellInfo.TYPE_NR: return ((CellIdentityNr) id).getTac();
            default: return CellInfo.UNAVAILABLE;
        }
    }

    private void setPhyCellInfoFromCellIdentity(ServiceState ss, CellIdentity cellIdentity) {
        if (cellIdentity == null) {
            if (DBG) {
                log("Could not set ServiceState channel number. CellIdentity null");
            }
            return;
        }

        ss.setChannelNumber(cellIdentity.getChannelNumber());
        if (VDBG) {
            log("Setting channel number: " + cellIdentity.getChannelNumber());
        }
        int[] bandwidths = null;
        PhysicalChannelConfig primaryPcc = getPrimaryPhysicalChannelConfigForCell(
                mLastPhysicalChannelConfigList, cellIdentity);
        if (cellIdentity instanceof CellIdentityLte) {
            CellIdentityLte ci = (CellIdentityLte) cellIdentity;
            // Prioritize the PhysicalChannelConfig list because we might already be in carrier
            // aggregation by the time poll state is performed.
            if (primaryPcc != null) {
                bandwidths = getBandwidthsFromConfigs(mLastPhysicalChannelConfigList);
                for (int bw : bandwidths) {
                    if (!isValidLteBandwidthKhz(bw)) {
                        loge("Invalid LTE Bandwidth in RegistrationState, " + bw);
                        bandwidths = null;
                        break;
                    }
                }
            } else {
                if (VDBG) log("No primary LTE PhysicalChannelConfig");
            }
            // If we don't have a PhysicalChannelConfig[] list, then pull from CellIdentityLte.
            // This is normal if we're in idle mode and the PhysicalChannelConfig[] has already
            // been updated. This is also a fallback in case the PhysicalChannelConfig info
            // is invalid (ie, broken).
            // Also, for vendor implementations that do not report return-to-idle, we should
            // prioritize the bandwidth report in the CellIdentity, because the physical channel
            // config report may be stale in the case where a single carrier was used previously
            // and we transition to camped-for-emergency (since we never have a physical
            // channel active). In the normal case of single-carrier non-return-to-idle, the
            // values *must* be the same, so it doesn't matter which is chosen.
            if (bandwidths == null || bandwidths.length == 1) {
                final int cbw = ci.getBandwidth();
                if (isValidLteBandwidthKhz(cbw)) {
                    bandwidths = new int[] {cbw};
                } else if (cbw == Integer.MAX_VALUE) {
                    // Bandwidth is unreported; c'est la vie. This is not an error because
                    // pre-1.2 HAL implementations do not support bandwidth reporting.
                } else {
                    loge("Invalid LTE Bandwidth in RegistrationState, " + cbw);
                }
            }
        } else if (cellIdentity instanceof CellIdentityNr) {
            // Prioritize the PhysicalChannelConfig list because we might already be in carrier
            // aggregation by the time poll state is performed.
            if (primaryPcc != null) {
                bandwidths = getBandwidthsFromConfigs(mLastPhysicalChannelConfigList);
                for (int bw : bandwidths) {
                    if (!isValidNrBandwidthKhz(bw)) {
                        loge("Invalid NR Bandwidth in RegistrationState, " + bw);
                        bandwidths = null;
                        break;
                    }
                }
            } else {
                if (VDBG) log("No primary NR PhysicalChannelConfig");
            }
            // TODO: update bandwidths from CellIdentityNr if the field is added
        } else {
            if (VDBG) log("Skipping bandwidth update for Non-LTE and Non-NR cell.");
        }

        if (bandwidths == null && primaryPcc != null && primaryPcc.getCellBandwidthDownlinkKhz()
                != PhysicalChannelConfig.CELL_BANDWIDTH_UNKNOWN) {
            bandwidths = new int[] {primaryPcc.getCellBandwidthDownlinkKhz()};
        } else if (VDBG) {
            log("Skipping bandwidth update because no primary PhysicalChannelConfig exists.");
        }

        if (bandwidths != null) {
            ss.setCellBandwidths(bandwidths);
        }
    }

    private static PhysicalChannelConfig getPrimaryPhysicalChannelConfigForCell(
            List<PhysicalChannelConfig> pccs, CellIdentity cellIdentity) {
        if (ArrayUtils.isEmpty(pccs) || !(cellIdentity instanceof CellIdentityLte
                || cellIdentity instanceof CellIdentityNr)) {
            return null;
        }

        int networkType, pci;
        if (cellIdentity instanceof CellIdentityLte) {
            networkType = TelephonyManager.NETWORK_TYPE_LTE;
            pci = ((CellIdentityLte) cellIdentity).getPci();
        } else {
            networkType = TelephonyManager.NETWORK_TYPE_NR;
            pci = ((CellIdentityNr) cellIdentity).getPci();
        }

        for (PhysicalChannelConfig pcc : pccs) {
            if (pcc.getConnectionStatus() == PhysicalChannelConfig.CONNECTION_PRIMARY_SERVING
                    && pcc.getNetworkType() == networkType && pcc.getPhysicalCellId() == pci) {
                return pcc;
            }
        }

        return null;
    }

    /**
     * Determine whether a roaming indicator is in the carrier-specified list of ERIs for
     * home system
     *
     * @param roamInd roaming indicator
     * @return true if the roamInd is in the carrier-specified list of ERIs for home network
     */
    private boolean isRoamIndForHomeSystem(int roamInd) {
        // retrieve the carrier-specified list of ERIs for home system
        final PersistableBundle config = getCarrierConfig();
        int[] homeRoamIndicators = config.getIntArray(CarrierConfigManager
                    .KEY_CDMA_ENHANCED_ROAMING_INDICATOR_FOR_HOME_NETWORK_INT_ARRAY);

        log("isRoamIndForHomeSystem: homeRoamIndicators=" + Arrays.toString(homeRoamIndicators));

        if (homeRoamIndicators != null) {
            // searches through the comma-separated list for a match,
            // return true if one is found.
            for (int homeRoamInd : homeRoamIndicators) {
                if (homeRoamInd == roamInd) {
                    return true;
                }
            }
            // no matches found against the list!
            log("isRoamIndForHomeSystem: No match found against list for roamInd=" + roamInd);
            return false;
        }

        // no system property found for the roaming indicators for home system
        log("isRoamIndForHomeSystem: No list found");
        return false;
    }

    /**
     * Query the carrier configuration to determine if there any network overrides
     * for roaming or not roaming for the current service state.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void updateRoamingState() {
        PersistableBundle bundle = getCarrierConfig();

        if (mPhone.isPhoneTypeGsm()) {
            /**
             * Since the roaming state of gsm service (from +CREG) and
             * data service (from +CGREG) could be different, the new SS
             * is set to roaming when either is true.
             *
             * There are exceptions for the above rule.
             * The new SS is not set as roaming while gsm service or
             * data service reports roaming but indeed it is same
             * operator. And the operator is considered non roaming.
             *
             * The test for the operators is to handle special roaming
             * agreements and MVNO's.
             */
            boolean roaming = (mGsmVoiceRoaming || mGsmDataRoaming);

            if (roaming && !isOperatorConsideredRoaming(mNewSS)
                    && (isSameNamedOperators(mNewSS) || isOperatorConsideredNonRoaming(mNewSS))) {
                log("updateRoamingState: resource override set non roaming.isSameNamedOperators="
                        + isSameNamedOperators(mNewSS) + ",isOperatorConsideredNonRoaming="
                        + isOperatorConsideredNonRoaming(mNewSS));
                roaming = false;
            }

            if (alwaysOnHomeNetwork(bundle)) {
                log("updateRoamingState: carrier config override always on home network");
                roaming = false;
            } else if (isNonRoamingInGsmNetwork(bundle, mNewSS.getOperatorNumeric())) {
                log("updateRoamingState: carrier config override set non roaming:"
                        + mNewSS.getOperatorNumeric());
                roaming = false;
            } else if (isRoamingInGsmNetwork(bundle, mNewSS.getOperatorNumeric())) {
                log("updateRoamingState: carrier config override set roaming:"
                        + mNewSS.getOperatorNumeric());
                roaming = true;
            }

            mNewSS.setRoaming(roaming);
        } else {
            String systemId = Integer.toString(mNewSS.getCdmaSystemId());

            if (alwaysOnHomeNetwork(bundle)) {
                log("updateRoamingState: carrier config override always on home network");
                setRoamingOff();
            } else if (isNonRoamingInGsmNetwork(bundle, mNewSS.getOperatorNumeric())
                    || isNonRoamingInCdmaNetwork(bundle, systemId)) {
                log("updateRoamingState: carrier config override set non-roaming:"
                        + mNewSS.getOperatorNumeric() + ", " + systemId);
                setRoamingOff();
            } else if (isRoamingInGsmNetwork(bundle, mNewSS.getOperatorNumeric())
                    || isRoamingInCdmaNetwork(bundle, systemId)) {
                log("updateRoamingState: carrier config override set roaming:"
                        + mNewSS.getOperatorNumeric() + ", " + systemId);
                setRoamingOn();
            }

            if (TelephonyUtils.IS_DEBUGGABLE
                    && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
                mNewSS.setRoaming(true);
            }
        }
    }

    private void setRoamingOn() {
        mNewSS.setRoaming(true);
        mNewSS.setCdmaEriIconIndex(EriInfo.ROAMING_INDICATOR_ON);
        mNewSS.setCdmaEriIconMode(EriInfo.ROAMING_ICON_MODE_NORMAL);
    }

    private void setRoamingOff() {
        mNewSS.setRoaming(false);
        mNewSS.setCdmaEriIconIndex(EriInfo.ROAMING_INDICATOR_OFF);
    }

    private void updateOperatorNameFromCarrierConfig() {
        // Brand override gets a priority over carrier config. If brand override is not available,
        // override the operator name in home network. Also do this only for CDMA. This is temporary
        // and should be fixed in a proper way in a later release.
        if (!mPhone.isPhoneTypeGsm() && !mSS.getRoaming()) {
            boolean hasBrandOverride = mUiccController.getUiccPort(getPhoneId()) != null
                    && mUiccController.getUiccPort(getPhoneId()).getOperatorBrandOverride() != null;
            if (!hasBrandOverride) {
                PersistableBundle config = getCarrierConfig();
                if (config.getBoolean(
                        CarrierConfigManager.KEY_CDMA_HOME_REGISTERED_PLMN_NAME_OVERRIDE_BOOL)) {
                    String operator = config.getString(
                            CarrierConfigManager.KEY_CDMA_HOME_REGISTERED_PLMN_NAME_STRING);
                    log("updateOperatorNameFromCarrierConfig: changing from "
                            + mSS.getOperatorAlpha() + " to " + operator);
                    // override long and short operator name, keeping numeric the same
                    mSS.setOperatorName(operator, operator, mSS.getOperatorNumeric());
                }
            }
        }
    }

    private void notifySpnDisplayUpdate(CarrierDisplayNameData data) {
        int subId = mPhone.getSubId();
        // Update ACTION_SERVICE_PROVIDERS_UPDATED IFF any value changes
        if (mSubId != subId
                || data.shouldShowPlmn() != mCurShowPlmn
                || data.shouldShowSpn() != mCurShowSpn
                || !TextUtils.equals(data.getSpn(), mCurSpn)
                || !TextUtils.equals(data.getDataSpn(), mCurDataSpn)
                || !TextUtils.equals(data.getPlmn(), mCurPlmn)) {

            final String log = String.format("updateSpnDisplay: changed sending intent, "
                            + "rule=%d, showPlmn='%b', plmn='%s', showSpn='%b', spn='%s', "
                            + "dataSpn='%s', subId='%d'",
                    getCarrierNameDisplayBitmask(mSS),
                    data.shouldShowPlmn(),
                    data.getPlmn(),
                    data.shouldShowSpn(),
                    data.getSpn(),
                    data.getDataSpn(),
                    subId);
            mCdnrLogs.log(log);
            if (DBG) log("updateSpnDisplay: " + log);

            Intent intent = new Intent(TelephonyManager.ACTION_SERVICE_PROVIDERS_UPDATED);
            intent.putExtra(TelephonyManager.EXTRA_SHOW_SPN, data.shouldShowSpn());
            intent.putExtra(TelephonyManager.EXTRA_SPN, data.getSpn());
            intent.putExtra(TelephonyManager.EXTRA_DATA_SPN, data.getDataSpn());
            intent.putExtra(TelephonyManager.EXTRA_SHOW_PLMN, data.shouldShowPlmn());
            intent.putExtra(TelephonyManager.EXTRA_PLMN, data.getPlmn());
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
            mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);

            if (!mSubscriptionController.setPlmnSpn(mPhone.getPhoneId(),
                    data.shouldShowPlmn(), data.getPlmn(), data.shouldShowSpn(), data.getSpn())) {
                mSpnUpdatePending = true;
            }
        }

        mSubId = subId;
        mCurShowSpn = data.shouldShowSpn();
        mCurShowPlmn = data.shouldShowPlmn();
        mCurSpn = data.getSpn();
        mCurDataSpn = data.getDataSpn();
        mCurPlmn = data.getPlmn();
    }

    private void updateSpnDisplayCdnr() {
        log("updateSpnDisplayCdnr+");
        CarrierDisplayNameData data = mCdnr.getCarrierDisplayNameData();
        notifySpnDisplayUpdate(data);
        log("updateSpnDisplayCdnr-");
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @VisibleForTesting
    public void updateSpnDisplay() {
        PersistableBundle config = getCarrierConfig();
        if (config.getBoolean(CarrierConfigManager.KEY_ENABLE_CARRIER_DISPLAY_NAME_RESOLVER_BOOL)) {
            updateSpnDisplayCdnr();
        } else {
            updateSpnDisplayLegacy();
        }
    }

    private void updateSpnDisplayLegacy() {
        log("updateSpnDisplayLegacy+");

        String spn = null;
        String dataSpn = null;
        boolean showSpn = false;
        String plmn = null;
        boolean showPlmn = false;

        String wfcVoiceSpnFormat = null;
        String wfcDataSpnFormat = null;
        String wfcFlightSpnFormat = null;
        int combinedRegState = getCombinedRegState(mSS);
        if (mPhone.getImsPhone() != null && mPhone.getImsPhone().isWifiCallingEnabled()
                && (combinedRegState == ServiceState.STATE_IN_SERVICE
                && mSS.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_IWLAN)) {
            // In Wi-Fi Calling mode (connected to WiFi and WFC enabled),
            // show SPN or PLMN + WiFi Calling
            //
            // 1) Show SPN + Wi-Fi Calling If SIM has SPN and SPN display condition
            //    is satisfied or SPN override is enabled for this carrier
            //
            // 2) Show PLMN + Wi-Fi Calling if there is no valid SPN in case 1

            int voiceIdx = 0;
            int dataIdx = 0;
            int flightModeIdx = -1;
            boolean useRootLocale = false;

            PersistableBundle bundle = getCarrierConfig();

            voiceIdx = bundle.getInt(CarrierConfigManager.KEY_WFC_SPN_FORMAT_IDX_INT);
            dataIdx = bundle.getInt(
                    CarrierConfigManager.KEY_WFC_DATA_SPN_FORMAT_IDX_INT);
            flightModeIdx = bundle.getInt(
                    CarrierConfigManager.KEY_WFC_FLIGHT_MODE_SPN_FORMAT_IDX_INT);
            useRootLocale =
                    bundle.getBoolean(CarrierConfigManager.KEY_WFC_SPN_USE_ROOT_LOCALE);

            String[] wfcSpnFormats = SubscriptionManager.getResourcesForSubId(mPhone.getContext(),
                    mPhone.getSubId(), useRootLocale)
                    .getStringArray(com.android.internal.R.array.wfcSpnFormats);

            if (voiceIdx < 0 || voiceIdx >= wfcSpnFormats.length) {
                loge("updateSpnDisplay: KEY_WFC_SPN_FORMAT_IDX_INT out of bounds: " + voiceIdx);
                voiceIdx = 0;
            }
            if (dataIdx < 0 || dataIdx >= wfcSpnFormats.length) {
                loge("updateSpnDisplay: KEY_WFC_DATA_SPN_FORMAT_IDX_INT out of bounds: "
                        + dataIdx);
                dataIdx = 0;
            }
            if (flightModeIdx < 0 || flightModeIdx >= wfcSpnFormats.length) {
                // KEY_WFC_FLIGHT_MODE_SPN_FORMAT_IDX_INT out of bounds. Use the value from
                // voiceIdx.
                flightModeIdx = voiceIdx;
            }

            wfcVoiceSpnFormat = wfcSpnFormats[voiceIdx];
            wfcDataSpnFormat = wfcSpnFormats[dataIdx];
            wfcFlightSpnFormat = wfcSpnFormats[flightModeIdx];
        }

        String crossSimSpnFormat = null;
        if (mPhone.getImsPhone() != null
                && (mPhone.getImsPhone() != null)
                && (mPhone.getImsPhone().getImsRegistrationTech()
                == ImsRegistrationImplBase.REGISTRATION_TECH_CROSS_SIM)) {
            // In Cros SIM Calling mode show SPN or PLMN + Cross SIM Calling
            //
            // 1) Show SPN + Cross SIM Calling If SIM has SPN and SPN display condition
            //    is satisfied or SPN override is enabled for this carrier
            //
            // 2) Show PLMN + Cross SIM Calling if there is no valid SPN in case 1
            PersistableBundle bundle = getCarrierConfig();
            int crossSimSpnFormatIdx =
                    bundle.getInt(CarrierConfigManager.KEY_CROSS_SIM_SPN_FORMAT_INT);
            boolean useRootLocale =
                    bundle.getBoolean(CarrierConfigManager.KEY_WFC_SPN_USE_ROOT_LOCALE);

            String[] crossSimSpnFormats = SubscriptionManager.getResourcesForSubId(
                    mPhone.getContext(),
                    mPhone.getSubId(), useRootLocale)
                    .getStringArray(R.array.crossSimSpnFormats);

            if (crossSimSpnFormatIdx < 0 || crossSimSpnFormatIdx >= crossSimSpnFormats.length) {
                loge("updateSpnDisplay: KEY_CROSS_SIM_SPN_FORMAT_INT out of bounds: "
                        + crossSimSpnFormatIdx);
                crossSimSpnFormatIdx = 0;
            }
            crossSimSpnFormat = crossSimSpnFormats[crossSimSpnFormatIdx];
        }

        if (mPhone.isPhoneTypeGsm()) {
            // The values of plmn/showPlmn change in different scenarios.
            // 1) No service but emergency call allowed -> expected
            //    to show "Emergency call only"
            //    EXTRA_SHOW_PLMN = true
            //    EXTRA_PLMN = "Emergency call only"

            // 2) No service at all --> expected to show "No service"
            //    EXTRA_SHOW_PLMN = true
            //    EXTRA_PLMN = "No service"

            // 3) Normal operation in either home or roaming service
            //    EXTRA_SHOW_PLMN = depending on IccRecords rule
            //    EXTRA_PLMN = plmn

            // 4) No service due to power off, aka airplane mode
            //    EXTRA_SHOW_PLMN = true
            //    EXTRA_PLMN = null

            IccRecords iccRecords = mIccRecords;
            int rule = getCarrierNameDisplayBitmask(mSS);
            boolean noService = false;
            if (combinedRegState == ServiceState.STATE_OUT_OF_SERVICE
                    || combinedRegState == ServiceState.STATE_EMERGENCY_ONLY) {
                showPlmn = true;

                // Force display no service
                final boolean forceDisplayNoService = shouldForceDisplayNoService() && !mIsSimReady;
                if (!forceDisplayNoService && Phone.isEmergencyCallOnly()) {
                    // No service but emergency call allowed
                    plmn = Resources.getSystem()
                            .getText(com.android.internal.R.string.emergency_calls_only).toString();
                } else {
                    // No service at all
                    plmn = Resources.getSystem()
                            .getText(
                                com.android.internal.R.string.lockscreen_carrier_default)
                            .toString();
                    noService = true;
                }
                if (DBG) log("updateSpnDisplay: radio is on but out " +
                        "of service, set plmn='" + plmn + "'");
            } else if (combinedRegState == ServiceState.STATE_IN_SERVICE) {
                // In either home or roaming service
                plmn = mSS.getOperatorAlpha();
                showPlmn = !TextUtils.isEmpty(plmn) &&
                        ((rule & CARRIER_NAME_DISPLAY_BITMASK_SHOW_PLMN)
                                == CARRIER_NAME_DISPLAY_BITMASK_SHOW_PLMN);
                if (DBG) log("updateSpnDisplay: rawPlmn = " + plmn);
            } else {
                // Power off state, such as airplane mode, show plmn as null
                showPlmn = true;
                plmn = null;
                if (DBG) log("updateSpnDisplay: radio is off w/ showPlmn="
                        + showPlmn + " plmn=" + plmn);
            }

            // The value of spn/showSpn are same in different scenarios.
            //    EXTRA_SHOW_SPN = depending on IccRecords rule and radio/IMS state
            //    EXTRA_SPN = spn
            //    EXTRA_DATA_SPN = dataSpn
            spn = getServiceProviderName();
            dataSpn = spn;
            showSpn = !noService && !TextUtils.isEmpty(spn)
                    && ((rule & CARRIER_NAME_DISPLAY_BITMASK_SHOW_SPN)
                    == CARRIER_NAME_DISPLAY_BITMASK_SHOW_SPN);
            if (DBG) log("updateSpnDisplay: rawSpn = " + spn);
            if (!TextUtils.isEmpty(crossSimSpnFormat)) {
                if (!TextUtils.isEmpty(spn)) {
                    // Show SPN + Cross-SIM Calling If SIM has SPN and SPN display condition
                    // is satisfied or SPN override is enabled for this carrier.
                    String originalSpn = spn.trim();
                    spn = String.format(crossSimSpnFormat, originalSpn);
                    dataSpn = spn;
                    showSpn = true;
                    showPlmn = false;
                } else if (!TextUtils.isEmpty(plmn)) {
                    // Show PLMN + Cross-SIM Calling if there is no valid SPN in the above case
                    String originalPlmn = plmn.trim();
                    PersistableBundle config = getCarrierConfig();
                    if (mIccRecords != null && config.getBoolean(
                            CarrierConfigManager.KEY_WFC_CARRIER_NAME_OVERRIDE_BY_PNN_BOOL)) {
                        originalPlmn = mIccRecords.getPnnHomeName();
                    }
                    plmn = String.format(crossSimSpnFormat, originalPlmn);
                }
            } else if (!TextUtils.isEmpty(spn) && !TextUtils.isEmpty(wfcVoiceSpnFormat)
                    && !TextUtils.isEmpty(wfcDataSpnFormat)) {
                // Show SPN + Wi-Fi Calling If SIM has SPN and SPN display condition
                // is satisfied or SPN override is enabled for this carrier.

                // Handle Flight Mode
                if (mSS.getState() == ServiceState.STATE_POWER_OFF) {
                    wfcVoiceSpnFormat = wfcFlightSpnFormat;
                }

                String originalSpn = spn.trim();
                spn = String.format(wfcVoiceSpnFormat, originalSpn);
                dataSpn = String.format(wfcDataSpnFormat, originalSpn);
                showSpn = true;
                showPlmn = false;
            } else if (!TextUtils.isEmpty(plmn) && !TextUtils.isEmpty(wfcVoiceSpnFormat)) {
                // Show PLMN + Wi-Fi Calling if there is no valid SPN in the above case
                String originalPlmn = plmn.trim();

                PersistableBundle config = getCarrierConfig();
                if (mIccRecords != null && config.getBoolean(
                        CarrierConfigManager.KEY_WFC_CARRIER_NAME_OVERRIDE_BY_PNN_BOOL)) {
                    originalPlmn = mIccRecords.getPnnHomeName();
                }

                plmn = String.format(wfcVoiceSpnFormat, originalPlmn);
            } else if (mSS.getState() == ServiceState.STATE_POWER_OFF
                    || (showPlmn && TextUtils.equals(spn, plmn))) {
                // airplane mode or spn equals plmn, do not show spn
                spn = null;
                showSpn = false;
            }
        } else {
            String eriText = getOperatorNameFromEri();
            if (eriText != null) mSS.setOperatorAlphaLong(eriText);

            // carrier config gets a priority over ERI
            updateOperatorNameFromCarrierConfig();

            // mOperatorAlpha contains the ERI text
            plmn = mSS.getOperatorAlpha();
            if (DBG) log("updateSpnDisplay: cdma rawPlmn = " + plmn);

            showPlmn = plmn != null;

            if (!TextUtils.isEmpty(plmn) && !TextUtils.isEmpty(wfcVoiceSpnFormat)) {
                // In Wi-Fi Calling mode show SPN+WiFi
                String originalPlmn = plmn.trim();
                plmn = String.format(wfcVoiceSpnFormat, originalPlmn);
            } else if (mCi.getRadioState() == TelephonyManager.RADIO_POWER_OFF) {
                // todo: temporary hack; should have a better fix. This is to avoid using operator
                // name from ServiceState (populated in processIwlanRegistrationInfo()) until
                // wifi calling is actually enabled
                log("updateSpnDisplay: overwriting plmn from " + plmn + " to null as radio " +
                        "state is off");
                plmn = null;
            }

            if (combinedRegState == ServiceState.STATE_OUT_OF_SERVICE) {
                plmn = Resources.getSystem().getText(com.android.internal.R.string
                        .lockscreen_carrier_default).toString();
                if (DBG) {
                    log("updateSpnDisplay: radio is on but out of svc, set plmn='" + plmn + "'");
                }
            }

        }

        notifySpnDisplayUpdate(new CarrierDisplayNameData.Builder()
                .setSpn(spn)
                .setDataSpn(dataSpn)
                .setShowSpn(showSpn)
                .setPlmn(plmn)
                .setShowPlmn(showPlmn)
                .build());
        log("updateSpnDisplayLegacy-");
    }

    /**
     * Returns whether out-of-service will be displayed as "no service" to the user.
     */
    public boolean shouldForceDisplayNoService() {
        String[] countriesWithNoService = mPhone.getContext().getResources().getStringArray(
                com.android.internal.R.array.config_display_no_service_when_sim_unready);
        if (ArrayUtils.isEmpty(countriesWithNoService)) {
            return false;
        }
        mLastKnownNetworkCountry = mLocaleTracker.getLastKnownCountryIso();
        for (String country : countriesWithNoService) {
            if (country.equalsIgnoreCase(mLastKnownNetworkCountry)) {
                return true;
            }
        }
        return false;
    }

    protected void setPowerStateToDesired() {
        setPowerStateToDesired(false, false, false);
    }

    protected void setPowerStateToDesired(boolean forEmergencyCall,
            boolean isSelectedPhoneForEmergencyCall, boolean forceApply) {
        if (DBG) {
            String tmpLog = "setPowerStateToDesired: mDeviceShuttingDown=" + mDeviceShuttingDown +
                    ", mDesiredPowerState=" + mDesiredPowerState +
                    ", getRadioState=" + mCi.getRadioState() +
                    ", mRadioDisabledByCarrier=" + mRadioDisabledByCarrier +
                    ", IMS reg state=" + mImsRegistrationOnOff +
                    ", pending radio off=" + hasMessages(EVENT_POWER_OFF_RADIO_IMS_DEREG_TIMEOUT);
            log(tmpLog);
            mRadioPowerLog.log(tmpLog);
        }

        if (mDesiredPowerState && mDeviceShuttingDown) {
            log("setPowerStateToDesired powering on of radio failed because the device is " +
                    "powering off");
            return;
        }

        // If we want it on and it's off, turn it on
        if (mDesiredPowerState && !mRadioDisabledByCarrier
                && (forceApply || mCi.getRadioState() == TelephonyManager.RADIO_POWER_OFF)) {
            mCi.setRadioPower(true, forEmergencyCall, isSelectedPhoneForEmergencyCall, null);
        } else if ((!mDesiredPowerState || mRadioDisabledByCarrier) && mCi.getRadioState()
                == TelephonyManager.RADIO_POWER_ON) {
            // If it's on and available and we want it off gracefully
            if (!mPhone.isUsingNewDataStack() && mImsRegistrationOnOff
                    && getRadioPowerOffDelayTimeoutForImsRegistration() > 0) {
                if (DBG) log("setPowerStateToDesired: delaying power off until IMS dereg.");
                startDelayRadioOffWaitingForImsDeregTimeout();
            } else {
                if (DBG) log("setPowerStateToDesired: powerOffRadioSafely()");
                powerOffRadioSafely();
            }
            // Return early here as we do not want to hit the cancel timeout code below.
            return;
        } else if (mDeviceShuttingDown
                && (mCi.getRadioState() != TelephonyManager.RADIO_POWER_UNAVAILABLE)) {
            // !mDesiredPowerState condition above will happen first if the radio is on, so we will
            // see the following: (delay for IMS dereg) -> RADIO_POWER_OFF ->
            // RADIO_POWER_UNAVAILABLE
            mCi.requestShutdown(null);
        }
        // Cancel any pending timeouts because the state has been re-evaluated.
        cancelDelayRadioOffWaitingForImsDeregTimeout();
        cancelPendingRadioPowerOff();
    }

    /**
     * Cancel the pending radio power off request that was sent to force the radio to power off
     * if waiting for all data to disconnect times out.
     */
    private synchronized void cancelPendingRadioPowerOff() {
        if (mPhone.isUsingNewDataStack() && mPendingRadioPowerOffAfterDataOff) {
            if (DBG) log("cancelPendingRadioPowerOff: cancelling.");
            mPendingRadioPowerOffAfterDataOff = false;
            removeMessages(EVENT_SET_RADIO_POWER_OFF);
        }
    }

    /**
     * Cancel the EVENT_POWER_OFF_RADIO_DELAYED event if it is currently pending to be completed.
     * @return true if there was a pending timeout message in the queue, false otherwise.
     */
    private void cancelDelayRadioOffWaitingForImsDeregTimeout() {
        if (hasMessages(EVENT_POWER_OFF_RADIO_IMS_DEREG_TIMEOUT)) {
            if (DBG) log("cancelDelayRadioOffWaitingForImsDeregTimeout: cancelling.");
            removeMessages(EVENT_POWER_OFF_RADIO_IMS_DEREG_TIMEOUT);
        }
    }

    /**
     * Start a timer to turn off the radio if IMS does not move to deregistered after the
     * radio power off event occurred. If this event already exists in the message queue, then
     * ignore the new request and use the existing one.
     */
    private void startDelayRadioOffWaitingForImsDeregTimeout() {
        if (hasMessages(EVENT_POWER_OFF_RADIO_IMS_DEREG_TIMEOUT)) {
            if (DBG) log("startDelayRadioOffWaitingForImsDeregTimeout: timer exists, ignoring");
            return;
        }
        if (DBG) log("startDelayRadioOffWaitingForImsDeregTimeout: starting timer");
        sendEmptyMessageDelayed(EVENT_POWER_OFF_RADIO_IMS_DEREG_TIMEOUT,
                getRadioPowerOffDelayTimeoutForImsRegistration());
    }

    protected void onUpdateIccAvailability() {
        if (mUiccController == null ) {
            return;
        }

        UiccCardApplication newUiccApplication = getUiccCardApplication();

        if (mUiccApplcation != newUiccApplication) {

            // Remove the EF records that come from UICC
            if (mIccRecords instanceof SIMRecords) {
                mCdnr.updateEfFromUsim(null /* usim */);
            } else if (mIccRecords instanceof RuimRecords) {
                mCdnr.updateEfFromRuim(null /* ruim */);
            }

            if (mUiccApplcation != null) {
                log("Removing stale icc objects.");
                mUiccApplcation.unregisterForReady(this);
                if (mIccRecords != null) {
                    mIccRecords.unregisterForRecordsLoaded(this);
                }
                mIccRecords = null;
                mUiccApplcation = null;
            }
            if (newUiccApplication != null) {
                log("New card found");
                mUiccApplcation = newUiccApplication;
                mIccRecords = mUiccApplcation.getIccRecords();
                if (mPhone.isPhoneTypeGsm()) {
                    mUiccApplcation.registerForReady(this, EVENT_SIM_READY, null);
                    if (mIccRecords != null) {
                        mIccRecords.registerForRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
                    }
                } else if (mIsSubscriptionFromRuim) {
                    mUiccApplcation.registerForReady(this, EVENT_RUIM_READY, null);
                    if (mIccRecords != null) {
                        mIccRecords.registerForRecordsLoaded(this, EVENT_RUIM_RECORDS_LOADED, null);
                    }
                }
            }
        }
    }

    private void logRoamingChange() {
        mRoamingLog.log(mSS.toString());
    }

    private void logAttachChange() {
        mAttachLog.log(mSS.toString());
    }

    private void logPhoneTypeChange() {
        mPhoneTypeLog.log(Integer.toString(mPhone.getPhoneType()));
    }

    private void logRatChange() {
        mRatLog.log(mSS.toString());
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected final void log(String s) {
        Rlog.d(LOG_TAG, "[" + mPhone.getPhoneId() + "] " + s);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected final void loge(String s) {
        Rlog.e(LOG_TAG, "[" + mPhone.getPhoneId() + "] " + s);
    }

    /**
     * @return The current GPRS state. IN_SERVICE is the same as "attached"
     * and OUT_OF_SERVICE is the same as detached.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int getCurrentDataConnectionState() {
        return mSS.getDataRegistrationState();
    }

    /**
     * @return true if phone is camping on a technology (eg UMTS)
     * that could support voice and data simultaneously.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isConcurrentVoiceAndDataAllowed() {
        if (mSS.getCssIndicator() == 1) {
            // Checking the Concurrent Service Supported flag first for all phone types.
            return true;
        } else if (mPhone.isPhoneTypeGsm()) {
            int radioTechnology = mSS.getRilDataRadioTechnology();
            // There are cases where we we would setup data connection even data is not yet
            // attached. In these cases we check voice rat.
            if (radioTechnology == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN
                    && mSS.getDataRegistrationState() != ServiceState.STATE_IN_SERVICE) {
                radioTechnology = mSS.getRilVoiceRadioTechnology();
            }
            // Concurrent voice and data is not allowed for 2G technologies. It's allowed in other
            // rats e.g. UMTS, LTE, etc.
            return radioTechnology != ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN
                    && ServiceState.rilRadioTechnologyToAccessNetworkType(radioTechnology)
                        != AccessNetworkType.GERAN;
        } else {
            return false;
        }
    }

    /** Called when the service state of ImsPhone is changed. */
    public void onImsServiceStateChanged() {
        sendMessage(obtainMessage(EVENT_IMS_SERVICE_STATE_CHANGED));
    }

    /**
     * Sets the Ims registration state.  If the 3 second shut down timer has begun and the state
     * is set to unregistered, the timer is cancelled and the radio is shutdown immediately.
     *
     * @param registered whether ims is registered
     */
    public void setImsRegistrationState(final boolean registered) {
        log("setImsRegistrationState: {registered=" + registered
                + " mImsRegistrationOnOff=" + mImsRegistrationOnOff
                + "}");


        if (mImsRegistrationOnOff && !registered) {
            // moving to deregistered, only send this event if we need to re-evaluate
            if (getRadioPowerOffDelayTimeoutForImsRegistration() > 0) {
                // only send this event if the power off delay for IMS deregistration feature is
                // enabled.
                sendMessage(obtainMessage(EVENT_CHANGE_IMS_STATE));
            } else {
                log("setImsRegistrationState: EVENT_CHANGE_IMS_STATE not sent because power off "
                        + "delay for IMS deregistration is not enabled.");
            }
        }
        mImsRegistrationOnOff = registered;
    }

    public void onImsCapabilityChanged() {
        sendMessage(obtainMessage(EVENT_IMS_CAPABILITY_CHANGED));
    }

    public boolean isRadioOn() {
        return mCi.getRadioState() == TelephonyManager.RADIO_POWER_ON;
    }

    /**
     * A complete "service state" from our perspective is
     * composed of a handful of separate requests to the radio.
     *
     * We make all of these requests at once, but then abandon them
     * and start over again if the radio notifies us that some
     * event has changed
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void pollState() {
        sendEmptyMessage(EVENT_POLL_STATE_REQUEST);
    }

    private void pollStateInternal(boolean modemTriggered) {
        mPollingContext = new int[1];
        NetworkRegistrationInfo nri;

        log("pollState: modemTriggered=" + modemTriggered + ", radioState=" + mCi.getRadioState());

        switch (mCi.getRadioState()) {
            case TelephonyManager.RADIO_POWER_UNAVAILABLE:
                // Preserve the IWLAN registration state, because that should not be affected by
                // radio availability.
                nri = mNewSS.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_PS,
                        AccessNetworkConstants.TRANSPORT_TYPE_WLAN);
                mNewSS.setOutOfService(mAccessNetworksManager.isInLegacyMode(), false);
                // Add the IWLAN registration info back to service state.
                if (nri != null) {
                    mNewSS.addNetworkRegistrationInfo(nri);
                }
                mPhone.getSignalStrengthController().setSignalStrengthDefaultValues();
                mLastNitzData = null;
                mNitzState.handleNetworkUnavailable();
                pollStateDone();
                break;

            case TelephonyManager.RADIO_POWER_OFF:
                // Preserve the IWLAN registration state, because that should not be affected by
                // radio availability.
                nri = mNewSS.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_PS,
                        AccessNetworkConstants.TRANSPORT_TYPE_WLAN);
                mNewSS.setOutOfService(mAccessNetworksManager.isInLegacyMode(), true);
                // Add the IWLAN registration info back to service state.
                if (nri != null) {
                    mNewSS.addNetworkRegistrationInfo(nri);
                }
                mPhone.getSignalStrengthController().setSignalStrengthDefaultValues();
                mLastNitzData = null;
                mNitzState.handleNetworkUnavailable();
                // Don't poll when device is shutting down or the poll was not modemTriggered
                // (they sent us new radio data) and the current network is not IWLAN
                if (mDeviceShuttingDown ||
                        (!modemTriggered && ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                        != mSS.getRilDataRadioTechnology())) {
                    pollStateDone();
                    break;
                }

            default:
                // Issue all poll-related commands at once then count down the responses, which
                // are allowed to arrive out-of-order
                mPollingContext[0]++;
                mCi.getOperator(obtainMessage(EVENT_POLL_STATE_OPERATOR, mPollingContext));

                mPollingContext[0]++;
                mRegStateManagers.get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                        .requestNetworkRegistrationInfo(NetworkRegistrationInfo.DOMAIN_PS,
                                obtainMessage(EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION,
                                        mPollingContext));

                mPollingContext[0]++;
                mRegStateManagers.get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                        .requestNetworkRegistrationInfo(NetworkRegistrationInfo.DOMAIN_CS,
                        obtainMessage(EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION, mPollingContext));

                if (mRegStateManagers.get(AccessNetworkConstants.TRANSPORT_TYPE_WLAN) != null) {
                    mPollingContext[0]++;
                    mRegStateManagers.get(AccessNetworkConstants.TRANSPORT_TYPE_WLAN)
                            .requestNetworkRegistrationInfo(NetworkRegistrationInfo.DOMAIN_PS,
                                    obtainMessage(EVENT_POLL_STATE_PS_IWLAN_REGISTRATION,
                                            mPollingContext));
                }

                if (mPhone.isPhoneTypeGsm()) {
                    mPollingContext[0]++;
                    mCi.getNetworkSelectionMode(obtainMessage(
                            EVENT_POLL_STATE_NETWORK_SELECTION_MODE, mPollingContext));
                }
                break;
        }
    }

    /**
     * Get the highest-priority CellIdentity for a provided ServiceState.
     *
     * Choose a CellIdentity for ServiceState using the following rules:
     * 1) WWAN only (WLAN is excluded)
     * 2) Registered > Camped
     * 3) CS > PS
     *
     * @param ss a Non-Null ServiceState object
     *
     * @return a list of CellIdentity objects in *decreasing* order of preference.
     */
    @VisibleForTesting public static @NonNull List<CellIdentity> getPrioritizedCellIdentities(
            @NonNull final ServiceState ss) {
        final List<NetworkRegistrationInfo> regInfos = ss.getNetworkRegistrationInfoList();
        if (regInfos.isEmpty()) return Collections.emptyList();

        return regInfos.stream()
            .filter(nri -> nri.getCellIdentity() != null)
            .filter(nri -> nri.getTransportType() == AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
            .sorted(Comparator
                    .comparing(NetworkRegistrationInfo::isRegistered)
                    .thenComparing((nri) -> nri.getDomain() & NetworkRegistrationInfo.DOMAIN_CS)
                    .reversed())
            .map(nri -> nri.getCellIdentity())
            .distinct()
            .collect(Collectors.toList());
    }

    private void pollStateDone() {
        if (!mPhone.isPhoneTypeGsm()) {
            updateRoamingState();
        }

        if (TelephonyUtils.IS_DEBUGGABLE
                && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
            mNewSS.setRoaming(true);
        }
        useDataRegStateForDataOnlyDevices();
        processIwlanRegistrationInfo();

        if (TelephonyUtils.IS_DEBUGGABLE && mPhone.mTelephonyTester != null) {
            mPhone.mTelephonyTester.overrideServiceState(mNewSS);
        }

        NetworkRegistrationInfo networkRegState = mNewSS.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        updateNrFrequencyRangeFromPhysicalChannelConfigs(mLastPhysicalChannelConfigList, mNewSS);
        updateNrStateFromPhysicalChannelConfigs(mLastPhysicalChannelConfigList, mNewSS);
        setPhyCellInfoFromCellIdentity(mNewSS, networkRegState.getCellIdentity());

        if (DBG) {
            log("Poll ServiceState done: "
                    + " oldSS=[" + mSS + "] newSS=[" + mNewSS + "]"
                    + " oldMaxDataCalls=" + mMaxDataCalls
                    + " mNewMaxDataCalls=" + mNewMaxDataCalls
                    + " oldReasonDataDenied=" + mReasonDataDenied
                    + " mNewReasonDataDenied=" + mNewReasonDataDenied);
        }

        boolean hasRegistered =
                mSS.getState() != ServiceState.STATE_IN_SERVICE
                        && mNewSS.getState() == ServiceState.STATE_IN_SERVICE;

        boolean hasDeregistered =
                mSS.getState() == ServiceState.STATE_IN_SERVICE
                        && mNewSS.getState() != ServiceState.STATE_IN_SERVICE;

        boolean hasAirplaneModeOnChanged =
                mSS.getState() != ServiceState.STATE_POWER_OFF
                        && mNewSS.getState() == ServiceState.STATE_POWER_OFF;
        boolean hasAirplaneModeOffChanged =
                mSS.getState() == ServiceState.STATE_POWER_OFF
                        && mNewSS.getState() != ServiceState.STATE_POWER_OFF;

        SparseBooleanArray hasDataAttached = new SparseBooleanArray(
                mAccessNetworksManager.getAvailableTransports().length);
        SparseBooleanArray hasDataDetached = new SparseBooleanArray(
                mAccessNetworksManager.getAvailableTransports().length);
        SparseBooleanArray hasRilDataRadioTechnologyChanged = new SparseBooleanArray(
                mAccessNetworksManager.getAvailableTransports().length);
        SparseBooleanArray hasDataRegStateChanged = new SparseBooleanArray(
                mAccessNetworksManager.getAvailableTransports().length);
        boolean anyDataRegChanged = false;
        boolean anyDataRatChanged = false;
        boolean hasAlphaRawChanged =
                !TextUtils.equals(mSS.getOperatorAlphaLongRaw(), mNewSS.getOperatorAlphaLongRaw())
                        || !TextUtils.equals(mSS.getOperatorAlphaShortRaw(),
                        mNewSS.getOperatorAlphaShortRaw());

        for (int transport : mAccessNetworksManager.getAvailableTransports()) {
            NetworkRegistrationInfo oldNrs = mSS.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, transport);
            NetworkRegistrationInfo newNrs = mNewSS.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, transport);

            // If the previously it was not in service, and now it's in service, trigger the
            // attached event. Also if airplane mode was just turned on, and data is already in
            // service, we need to trigger the attached event again so that DcTracker can setup
            // data on all connectable APNs again (because we've already torn down all data
            // connections just before airplane mode turned on)
            boolean changed = (oldNrs == null || !oldNrs.isInService() || hasAirplaneModeOnChanged)
                    && (newNrs != null && newNrs.isInService());
            hasDataAttached.put(transport, changed);

            changed = (oldNrs != null && oldNrs.isInService())
                    && (newNrs == null || !newNrs.isInService());
            hasDataDetached.put(transport, changed);

            int oldRAT = oldNrs != null ? oldNrs.getAccessNetworkTechnology()
                    : TelephonyManager.NETWORK_TYPE_UNKNOWN;
            int newRAT = newNrs != null ? newNrs.getAccessNetworkTechnology()
                    : TelephonyManager.NETWORK_TYPE_UNKNOWN;

            boolean isOldCA = oldNrs != null ? oldNrs.isUsingCarrierAggregation() : false;
            boolean isNewCA = newNrs != null ? newNrs.isUsingCarrierAggregation() : false;

            // If the carrier enable KEY_SHOW_CARRIER_DATA_ICON_PATTERN_STRING and the operator name
            // match this pattern, the data rat display LteAdvanced indicator.
            hasRilDataRadioTechnologyChanged.put(transport,
                    oldRAT != newRAT || isOldCA != isNewCA || hasAlphaRawChanged);
            if (oldRAT != newRAT) {
                anyDataRatChanged = true;
            }

            int oldRegState = oldNrs != null ? oldNrs.getRegistrationState()
                    : NetworkRegistrationInfo.REGISTRATION_STATE_UNKNOWN;
            int newRegState = newNrs != null ? newNrs.getRegistrationState()
                    : NetworkRegistrationInfo.REGISTRATION_STATE_UNKNOWN;
            hasDataRegStateChanged.put(transport, oldRegState != newRegState);
            if (oldRegState != newRegState) {
                anyDataRegChanged = true;
            }
        }

        // Filter out per transport data RAT changes, only want to track changes based on
        // transport preference changes (WWAN to WLAN, for example).
        boolean hasDataTransportPreferenceChanged = !anyDataRatChanged
                && (mSS.getRilDataRadioTechnology() != mNewSS.getRilDataRadioTechnology());

        boolean hasVoiceRegStateChanged =
                mSS.getState() != mNewSS.getState();

        boolean hasNrFrequencyRangeChanged =
                mSS.getNrFrequencyRange() != mNewSS.getNrFrequencyRange();

        boolean hasNrStateChanged = mSS.getNrState() != mNewSS.getNrState();

        final List<CellIdentity> prioritizedCids = getPrioritizedCellIdentities(mNewSS);

        final CellIdentity primaryCellIdentity = prioritizedCids.isEmpty()
                ? null : prioritizedCids.get(0);

        boolean hasLocationChanged = mCellIdentity == null
                ? primaryCellIdentity != null : !mCellIdentity.isSameCell(primaryCellIdentity);

        boolean isRegisteredOnWwan = false;
        for (NetworkRegistrationInfo nri : mNewSS.getNetworkRegistrationInfoListForTransportType(
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN)) {
            isRegisteredOnWwan |= nri.isRegistered();
        }

        // Ratchet if the device is in service on the same cell
        if (isRegisteredOnWwan && !hasLocationChanged) {
            mRatRatcheter.ratchet(mSS, mNewSS);
        }

        boolean hasRilVoiceRadioTechnologyChanged =
                mSS.getRilVoiceRadioTechnology() != mNewSS.getRilVoiceRadioTechnology();

        boolean hasChanged = !mNewSS.equals(mSS);

        boolean hasVoiceRoamingOn = !mSS.getVoiceRoaming() && mNewSS.getVoiceRoaming();

        boolean hasVoiceRoamingOff = mSS.getVoiceRoaming() && !mNewSS.getVoiceRoaming();

        boolean hasDataRoamingOn = !mSS.getDataRoaming() && mNewSS.getDataRoaming();

        boolean hasDataRoamingOff = mSS.getDataRoaming() && !mNewSS.getDataRoaming();

        boolean hasRejectCauseChanged = mRejectCode != mNewRejectCode;

        boolean hasCssIndicatorChanged = (mSS.getCssIndicator() != mNewSS.getCssIndicator());

        boolean hasBandwidthChanged = mSS.getCellBandwidths() != mNewSS.getCellBandwidths();

        boolean has4gHandoff = false;
        boolean hasMultiApnSupport = false;
        boolean hasLostMultiApnSupport = false;
        if (mPhone.isPhoneTypeCdmaLte()) {
            final int wwanDataRat = getRilDataRadioTechnologyForWwan(mSS);
            final int newWwanDataRat = getRilDataRadioTechnologyForWwan(mNewSS);
            has4gHandoff = mNewSS.getDataRegistrationState() == ServiceState.STATE_IN_SERVICE
                    && ((ServiceState.isPsOnlyTech(wwanDataRat)
                    && (newWwanDataRat == ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD))
                    || ((wwanDataRat == ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD)
                    && ServiceState.isPsOnlyTech(newWwanDataRat)));

            hasMultiApnSupport = ((ServiceState.isPsOnlyTech(newWwanDataRat)
                    || (newWwanDataRat == ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD))
                    && (!ServiceState.isPsOnlyTech(wwanDataRat)
                    && (wwanDataRat != ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD)));

            hasLostMultiApnSupport = ((newWwanDataRat >= ServiceState.RIL_RADIO_TECHNOLOGY_IS95A)
                    && (newWwanDataRat <= ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_A));
        }

        if (DBG) {
            log("pollStateDone:"
                    + " hasRegistered = " + hasRegistered
                    + " hasDeregistered = " + hasDeregistered
                    + " hasDataAttached = " + hasDataAttached
                    + " hasDataDetached = " + hasDataDetached
                    + " hasDataRegStateChanged = " + hasDataRegStateChanged
                    + " hasRilVoiceRadioTechnologyChanged = " + hasRilVoiceRadioTechnologyChanged
                    + " hasRilDataRadioTechnologyChanged = " + hasRilDataRadioTechnologyChanged
                    + " hasDataTransportPreferenceChanged = " + hasDataTransportPreferenceChanged
                    + " hasChanged = " + hasChanged
                    + " hasVoiceRoamingOn = " + hasVoiceRoamingOn
                    + " hasVoiceRoamingOff = " + hasVoiceRoamingOff
                    + " hasDataRoamingOn =" + hasDataRoamingOn
                    + " hasDataRoamingOff = " + hasDataRoamingOff
                    + " hasLocationChanged = " + hasLocationChanged
                    + " has4gHandoff = " + has4gHandoff
                    + " hasMultiApnSupport = " + hasMultiApnSupport
                    + " hasLostMultiApnSupport = " + hasLostMultiApnSupport
                    + " hasCssIndicatorChanged = " + hasCssIndicatorChanged
                    + " hasNrFrequencyRangeChanged = " + hasNrFrequencyRangeChanged
                    + " hasNrStateChanged = " + hasNrStateChanged
                    + " hasBandwidthChanged = " + hasBandwidthChanged
                    + " hasAirplaneModeOnlChanged = " + hasAirplaneModeOnChanged);
        }

        // Add an event log when connection state changes
        if (hasVoiceRegStateChanged || anyDataRegChanged) {
            EventLog.writeEvent(mPhone.isPhoneTypeGsm() ? EventLogTags.GSM_SERVICE_STATE_CHANGE :
                            EventLogTags.CDMA_SERVICE_STATE_CHANGE,
                    mSS.getState(), mSS.getDataRegistrationState(),
                    mNewSS.getState(), mNewSS.getDataRegistrationState());
        }

        if (mPhone.isPhoneTypeGsm()) {
            // Add an event log when network type switched
            // TODO: we may add filtering to reduce the event logged,
            // i.e. check preferred network setting, only switch to 2G, etc
            if (hasRilVoiceRadioTechnologyChanged) {
                long cid = getCidFromCellIdentity(primaryCellIdentity);
                // NOTE: this code was previously located after mSS and mNewSS are swapped, so
                // existing logs were incorrectly using the new state for "network_from"
                // and STATE_OUT_OF_SERVICE for "network_to". To avoid confusion, use a new log tag
                // to record the correct states.
                EventLog.writeEvent(EventLogTags.GSM_RAT_SWITCHED_NEW, cid,
                        mSS.getRilVoiceRadioTechnology(),
                        mNewSS.getRilVoiceRadioTechnology());
                if (DBG) {
                    log("RAT switched "
                            + ServiceState.rilRadioTechnologyToString(
                            mSS.getRilVoiceRadioTechnology())
                            + " -> "
                            + ServiceState.rilRadioTechnologyToString(
                            mNewSS.getRilVoiceRadioTechnology()) + " at cell " + cid);
                }
            }

            mReasonDataDenied = mNewReasonDataDenied;
            mMaxDataCalls = mNewMaxDataCalls;
            mRejectCode = mNewRejectCode;
        }

        if (!Objects.equals(mSS, mNewSS)) {
            mServiceStateChangedRegistrants.notifyRegistrants();
        }

        ServiceState oldMergedSS = new ServiceState(mPhone.getServiceState());
        mSS = new ServiceState(mNewSS);

        mNewSS.setOutOfService(mAccessNetworksManager.isInLegacyMode(), false);

        mCellIdentity = primaryCellIdentity;
        if (mSS.getState() == ServiceState.STATE_IN_SERVICE && primaryCellIdentity != null) {
            mLastKnownCellIdentity = mCellIdentity;
            removeMessages(EVENT_RESET_LAST_KNOWN_CELL_IDENTITY);
        }

        if (hasDeregistered && !hasMessages(EVENT_RESET_LAST_KNOWN_CELL_IDENTITY)) {
            sendEmptyMessageDelayed(EVENT_RESET_LAST_KNOWN_CELL_IDENTITY,
                    TimeUnit.DAYS.toMillis(1));
        }

        int areaCode = getAreaCodeFromCellIdentity(mCellIdentity);
        if (areaCode != mLastKnownAreaCode && areaCode != CellInfo.UNAVAILABLE) {
            mLastKnownAreaCode = areaCode;
            mAreaCodeChangedRegistrants.notifyRegistrants();
        }

        if (hasRilVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }

        TelephonyManager tm = (TelephonyManager) mPhone.getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        if (anyDataRatChanged) {
            tm.setDataNetworkTypeForPhone(mPhone.getPhoneId(), mSS.getRilDataRadioTechnology());
            TelephonyStatsLog.write(TelephonyStatsLog.MOBILE_RADIO_TECHNOLOGY_CHANGED,
                    ServiceState.rilRadioTechnologyToNetworkType(
                            mSS.getRilDataRadioTechnology()), mPhone.getPhoneId());
        }

        if (hasRegistered) {
            mNetworkAttachedRegistrants.notifyRegistrants();
            mNitzState.handleNetworkAvailable();
        }

        if (hasDeregistered) {
            mNetworkDetachedRegistrants.notifyRegistrants();
            mNitzState.handleNetworkUnavailable();
        }

        if (hasCssIndicatorChanged) {
            mCssIndicatorChangedRegistrants.notifyRegistrants();
        }

        if (hasBandwidthChanged) {
            mBandwidthChangedRegistrants.notifyRegistrants();
        }

        if (hasRejectCauseChanged) {
            setNotification(CS_REJECT_CAUSE_ENABLED);
        }

        String eriText = mPhone.getCdmaEriText();
        boolean hasEriChanged = !TextUtils.equals(mEriText, eriText);
        mEriText = eriText;
        // Trigger updateSpnDisplay when
        // 1. Service state is changed.
        // 2. phone type is Cdma or CdmaLte and ERI text has changed.
        if (hasChanged || (!mPhone.isPhoneTypeGsm() && hasEriChanged)) {
            updateSpnDisplay();
        }

        if (hasChanged) {
            tm.setNetworkOperatorNameForPhone(mPhone.getPhoneId(), mSS.getOperatorAlpha());
            String operatorNumeric = mSS.getOperatorNumeric();

            if (!mPhone.isPhoneTypeGsm()) {
                // try to fix the invalid Operator Numeric
                if (isInvalidOperatorNumeric(operatorNumeric)) {
                    int sid = mSS.getCdmaSystemId();
                    operatorNumeric = fixUnknownMcc(operatorNumeric, sid);
                }
            }

            tm.setNetworkOperatorNumericForPhone(mPhone.getPhoneId(), operatorNumeric);

            // If the OPERATOR command hasn't returned a valid operator or the device is on IWLAN (
            // because operatorNumeric would be SIM's mcc/mnc when device is on IWLAN), but if the
            // device has camped on a cell either to attempt registration or for emergency services,
            // then for purposes of setting the locale, we don't care if registration fails or is
            // incomplete.
            // CellIdentity can return a null MCC and MNC in CDMA
            String localeOperator = operatorNumeric;
            if (isInvalidOperatorNumeric(operatorNumeric)
                    || mSS.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_IWLAN) {
                for (CellIdentity cid : prioritizedCids) {
                    if (!TextUtils.isEmpty(cid.getPlmn())) {
                        localeOperator = cid.getPlmn();
                        break;
                    }
                }
            }

            if (isInvalidOperatorNumeric(localeOperator)) {
                if (DBG) log("localeOperator " + localeOperator + " is invalid");
                // Passing empty string is important for the first update. The initial value of
                // operator numeric in locale tracker is null. The async update will allow getting
                // cell info from the modem instead of using the cached one.
                mLocaleTracker.updateOperatorNumeric("");
            } else {
                if (!mPhone.isPhoneTypeGsm()) {
                    setOperatorIdd(localeOperator);
                }
                mLocaleTracker.updateOperatorNumeric(localeOperator);
            }

            tm.setNetworkRoamingForPhone(mPhone.getPhoneId(),
                    mPhone.isPhoneTypeGsm() ? mSS.getVoiceRoaming() :
                            (mSS.getVoiceRoaming() || mSS.getDataRoaming()));

            setRoamingType(mSS);
            log("Broadcasting ServiceState : " + mSS);
            // notify using PhoneStateListener and the legacy intent ACTION_SERVICE_STATE_CHANGED
            // notify service state changed only if the merged service state is changed.
            if (!oldMergedSS.equals(mPhone.getServiceState())) {
                mPhone.notifyServiceStateChanged(mPhone.getServiceState());
            }

            // insert into ServiceStateProvider. This will trigger apps to wake through JobScheduler
            mPhone.getContext().getContentResolver()
                    .insert(getUriForSubscriptionId(mPhone.getSubId()),
                            getContentValuesForServiceState(mSS));

            TelephonyMetrics.getInstance().writeServiceStateChanged(mPhone.getPhoneId(), mSS);
            mPhone.getVoiceCallSessionStats().onServiceStateChanged(mSS);
            mServiceStateStats.onServiceStateChanged(mSS);
        }

        boolean shouldLogAttachedChange = false;
        boolean shouldLogRatChange = false;

        if (hasRegistered || hasDeregistered) {
            shouldLogAttachedChange = true;
        }

        if (has4gHandoff) {
            mAttachedRegistrants.get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                    .notifyRegistrants();
            shouldLogAttachedChange = true;
        }

        if (hasRilVoiceRadioTechnologyChanged) {
            shouldLogRatChange = true;
            // TODO(b/178429976): Remove the dependency on SSC. Double check if the SS broadcast
            // is really needed when CS/PS RAT change.
            mPhone.getSignalStrengthController().notifySignalStrength();
        }

        for (int transport : mAccessNetworksManager.getAvailableTransports()) {
            if (hasRilDataRadioTechnologyChanged.get(transport)) {
                shouldLogRatChange = true;
                mPhone.getSignalStrengthController().notifySignalStrength();
            }

            if (hasDataRegStateChanged.get(transport)
                    || hasRilDataRadioTechnologyChanged.get(transport)
                    // Update all transports if preference changed so that consumers can be notified
                    // that ServiceState#getRilDataRadioTechnology has changed.
                    || hasDataTransportPreferenceChanged) {
                setDataNetworkTypeForPhone(mSS.getRilDataRadioTechnology());
                notifyDataRegStateRilRadioTechnologyChanged(transport);
            }

            if (hasDataAttached.get(transport)) {
                shouldLogAttachedChange = true;
                if (mAttachedRegistrants.get(transport) != null) {
                    mAttachedRegistrants.get(transport).notifyRegistrants();
                }
            }
            if (hasDataDetached.get(transport)) {
                shouldLogAttachedChange = true;
                if (mDetachedRegistrants.get(transport) != null) {
                    mDetachedRegistrants.get(transport).notifyRegistrants();
                }
            }
        }

        // Before starting to poll network state, the signal strength will be
        // reset under radio power off, so here expects to query it again
        // because the signal strength might come earlier RAT and radio state
        // changed.
        if (hasAirplaneModeOffChanged) {
            // TODO(b/178429976): Remove the dependency on SSC. This should be done in SSC.
            mPhone.getSignalStrengthController().getSignalStrengthFromCi();
        }

        if (shouldLogAttachedChange) {
            logAttachChange();
        }
        if (shouldLogRatChange) {
            logRatChange();
        }

        if (hasVoiceRegStateChanged || hasRilVoiceRadioTechnologyChanged) {
            notifyVoiceRegStateRilRadioTechnologyChanged();
        }

        if (hasVoiceRoamingOn || hasVoiceRoamingOff || hasDataRoamingOn || hasDataRoamingOff) {
            logRoamingChange();
        }

        if (hasVoiceRoamingOn) {
            mVoiceRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasVoiceRoamingOff) {
            mVoiceRoamingOffRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOn) {
            mDataRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOff) {
            mDataRoamingOffRegistrants.notifyRegistrants();
        }

        if (hasLocationChanged) {
            mPhone.notifyLocationChanged(getCellIdentity());
        }

        if (hasNrStateChanged) {
            mNrStateChangedRegistrants.notifyRegistrants();
        }

        if (hasNrFrequencyRangeChanged) {
            mNrFrequencyChangedRegistrants.notifyRegistrants();
        }

        if (mPhone.isPhoneTypeGsm()) {
            if (!isGprsConsistent(mSS.getDataRegistrationState(), mSS.getState())) {
                if (!mStartedGprsRegCheck && !mReportedGprsNoReg) {
                    mStartedGprsRegCheck = true;

                    int check_period = Settings.Global.getInt(
                            mPhone.getContext().getContentResolver(),
                            Settings.Global.GPRS_REGISTER_CHECK_PERIOD_MS,
                            DEFAULT_GPRS_CHECK_PERIOD_MILLIS);
                    sendMessageDelayed(obtainMessage(EVENT_CHECK_REPORT_GPRS),
                            check_period);
                }
            } else {
                mReportedGprsNoReg = false;
            }
        }
    }

    private String getOperatorNameFromEri() {
        String eriText = null;
        if (mPhone.isPhoneTypeCdma()) {
            if ((mCi.getRadioState() == TelephonyManager.RADIO_POWER_ON)
                    && (!mIsSubscriptionFromRuim)) {
                // Now the Phone sees the new ServiceState so it can get the new ERI text
                if (mSS.getState() == ServiceState.STATE_IN_SERVICE) {
                    eriText = mPhone.getCdmaEriText();
                } else {
                    // Note that ServiceState.STATE_OUT_OF_SERVICE is valid used for
                    // mRegistrationState 0,2,3 and 4
                    eriText = mPhone.getContext().getText(
                            com.android.internal.R.string.roamingTextSearching).toString();
                }
            }
        } else if (mPhone.isPhoneTypeCdmaLte()) {
            boolean hasBrandOverride = mUiccController.getUiccPort(getPhoneId()) != null
                    && mUiccController.getUiccPort(getPhoneId()).getOperatorBrandOverride() != null;
            if (!hasBrandOverride && (mCi.getRadioState() == TelephonyManager.RADIO_POWER_ON)
                    && (mEriManager != null && mEriManager.isEriFileLoaded())
                    && (!ServiceState.isPsOnlyTech(mSS.getRilVoiceRadioTechnology())
                    || mPhone.getContext().getResources().getBoolean(com.android.internal.R
                    .bool.config_LTE_eri_for_network_name))) {
                // Only when CDMA is in service, ERI will take effect
                eriText = mSS.getOperatorAlpha();
                // Now the Phone sees the new ServiceState so it can get the new ERI text
                if (mSS.getState() == ServiceState.STATE_IN_SERVICE) {
                    eriText = mPhone.getCdmaEriText();
                } else if (mSS.getState() == ServiceState.STATE_POWER_OFF) {
                    eriText = getServiceProviderName();
                    if (TextUtils.isEmpty(eriText)) {
                        // Sets operator alpha property by retrieving from
                        // build-time system property
                        eriText = SystemProperties.get("ro.cdma.home.operator.alpha");
                    }
                } else if (mSS.getDataRegistrationState() != ServiceState.STATE_IN_SERVICE) {
                    // Note that ServiceState.STATE_OUT_OF_SERVICE is valid used
                    // for mRegistrationState 0,2,3 and 4
                    eriText = mPhone.getContext()
                            .getText(com.android.internal.R.string.roamingTextSearching).toString();
                }
            }

            if (mUiccApplcation != null && mUiccApplcation.getState() == AppState.APPSTATE_READY &&
                    mIccRecords != null && getCombinedRegState(mSS) == ServiceState.STATE_IN_SERVICE
                    && !ServiceState.isPsOnlyTech(mSS.getRilVoiceRadioTechnology())) {
                // SIM is found on the device. If ERI roaming is OFF, and SID/NID matches
                // one configured in SIM, use operator name from CSIM record. Note that ERI, SID,
                // and NID are CDMA only, not applicable to LTE.
                boolean showSpn =
                        ((RuimRecords) mIccRecords).getCsimSpnDisplayCondition();
                int iconIndex = mSS.getCdmaEriIconIndex();

                if (showSpn && (iconIndex == EriInfo.ROAMING_INDICATOR_OFF)
                        && isInHomeSidNid(mSS.getCdmaSystemId(), mSS.getCdmaNetworkId())
                        && mIccRecords != null) {
                    eriText = getServiceProviderName();
                }
            }
        }
        return eriText;
    }

    /**
     * Get the service provider name with highest priority among various source.
     * @return service provider name.
     */
    public String getServiceProviderName() {
        // BrandOverride has higher priority than the carrier config
        String operatorBrandOverride = getOperatorBrandOverride();
        if (!TextUtils.isEmpty(operatorBrandOverride)) {
            return operatorBrandOverride;
        }

        String carrierName = mIccRecords != null ? mIccRecords.getServiceProviderName() : "";
        PersistableBundle config = getCarrierConfig();
        if (config.getBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL)
                || TextUtils.isEmpty(carrierName)) {
            return config.getString(CarrierConfigManager.KEY_CARRIER_NAME_STRING);
        }

        return carrierName;
    }

    /**
     * Get the resolved carrier name display condition bitmask.
     *
     * <p> Show service provider name if only if {@link #CARRIER_NAME_DISPLAY_BITMASK_SHOW_SPN}
     * is set.
     *
     * <p> Show PLMN network name if only if {@link #CARRIER_NAME_DISPLAY_BITMASK_SHOW_PLMN} is set.
     *
     * @param ss service state
     * @return carrier name display bitmask.
     */
    @CarrierNameDisplayBitmask
    public int getCarrierNameDisplayBitmask(ServiceState ss) {
        PersistableBundle config = getCarrierConfig();
        if (!TextUtils.isEmpty(getOperatorBrandOverride())) {
            // If the operator has been overridden, all PLMNs will be considered HOME PLMNs, only
            // show SPN.
            return CARRIER_NAME_DISPLAY_BITMASK_SHOW_SPN;
        } else if (TextUtils.isEmpty(getServiceProviderName())) {
            // If SPN is null or empty, we should show plmn.
            // This is a hack from IccRecords#getServiceProviderName().
            return CARRIER_NAME_DISPLAY_BITMASK_SHOW_PLMN;
        } else {
            boolean useRoamingFromServiceState = config.getBoolean(
                    CarrierConfigManager.KEY_SPN_DISPLAY_RULE_USE_ROAMING_FROM_SERVICE_STATE_BOOL);
            int carrierDisplayNameConditionFromSim =
                    mIccRecords == null ? 0 : mIccRecords.getCarrierNameDisplayCondition();

            boolean isRoaming;
            if (useRoamingFromServiceState) {
                isRoaming = ss.getRoaming();
            } else {
                String[] hplmns = mIccRecords != null ? mIccRecords.getHomePlmns() : null;
                isRoaming = !ArrayUtils.contains(hplmns, ss.getOperatorNumeric());
            }
            int rule;
            if (isRoaming) {
                // Show PLMN when roaming.
                rule = CARRIER_NAME_DISPLAY_BITMASK_SHOW_PLMN;

                // Check if show SPN is required when roaming.
                if ((carrierDisplayNameConditionFromSim
                        & CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN)
                        == CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN) {
                    rule |= CARRIER_NAME_DISPLAY_BITMASK_SHOW_SPN;
                }
            } else {
                // Show SPN when not roaming.
                rule = CARRIER_NAME_DISPLAY_BITMASK_SHOW_SPN;

                // Check if show PLMN is required when not roaming.
                if ((carrierDisplayNameConditionFromSim
                        & CARRIER_NAME_DISPLAY_CONDITION_BITMASK_PLMN)
                        == CARRIER_NAME_DISPLAY_CONDITION_BITMASK_PLMN) {
                    rule |= CARRIER_NAME_DISPLAY_BITMASK_SHOW_PLMN;
                }
            }
            return rule;
        }
    }

    private String getOperatorBrandOverride() {
        UiccPort uiccPort = mPhone.getUiccPort();
        if (uiccPort == null) return null;
        UiccProfile profile = uiccPort.getUiccProfile();
        if (profile == null) return null;
        return profile.getOperatorBrandOverride();
    }

    /**
     * Check whether the specified SID and NID pair appears in the HOME SID/NID list
     * read from NV or SIM.
     *
     * @return true if provided sid/nid pair belongs to operator's home network.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isInHomeSidNid(int sid, int nid) {
        // if SID/NID is not available, assume this is home network.
        if (isSidsAllZeros()) return true;

        // length of SID/NID shold be same
        if (mHomeSystemId.length != mHomeNetworkId.length) return true;

        if (sid == 0) return true;

        for (int i = 0; i < mHomeSystemId.length; i++) {
            // Use SID only if NID is a reserved value.
            // SID 0 and NID 0 and 65535 are reserved. (C.0005 2.6.5.2)
            if ((mHomeSystemId[i] == sid) &&
                    ((mHomeNetworkId[i] == 0) || (mHomeNetworkId[i] == 65535) ||
                            (nid == 0) || (nid == 65535) || (mHomeNetworkId[i] == nid))) {
                return true;
            }
        }
        // SID/NID are not in the list. So device is not in home network
        return false;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void setOperatorIdd(String operatorNumeric) {
        if (mPhone.getUnitTestMode()) {
            return;
        }

        // Retrieve the current country information
        // with the MCC got from operatorNumeric.
        String idd = mHbpcdUtils.getIddByMcc(
                Integer.parseInt(operatorNumeric.substring(0,3)));
        if (idd != null && !idd.isEmpty()) {
            TelephonyProperties.operator_idp_string(idd);
        } else {
            // use default "+", since we don't know the current IDP
            TelephonyProperties.operator_idp_string("+");
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isInvalidOperatorNumeric(String operatorNumeric) {
        return operatorNumeric == null || operatorNumeric.length() < 5 ||
                operatorNumeric.startsWith(INVALID_MCC);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String fixUnknownMcc(String operatorNumeric, int sid) {
        if (sid <= 0) {
            // no cdma information is available, do nothing
            return operatorNumeric;
        }

        // resolve the mcc from sid, using time zone information from the latest NITZ signal when
        // available.
        int utcOffsetHours = 0;
        boolean isDst = false;
        boolean isNitzTimeZone = false;
        NitzData lastNitzData = mLastNitzData;
        if (lastNitzData != null) {
            utcOffsetHours = lastNitzData.getLocalOffsetMillis() / MS_PER_HOUR;
            Integer dstAdjustmentMillis = lastNitzData.getDstAdjustmentMillis();
            isDst = (dstAdjustmentMillis != null) && (dstAdjustmentMillis != 0);
            isNitzTimeZone = true;
        }
        int mcc = mHbpcdUtils.getMcc(sid, utcOffsetHours, (isDst ? 1 : 0), isNitzTimeZone);
        if (mcc > 0) {
            operatorNumeric = mcc + DEFAULT_MNC;
        }
        return operatorNumeric;
    }

    /**
     * Check if GPRS got registered while voice is registered.
     *
     * @param dataRegState i.e. CGREG in GSM
     * @param voiceRegState i.e. CREG in GSM
     * @return false if device only register to voice but not gprs
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isGprsConsistent(int dataRegState, int voiceRegState) {
        return !((voiceRegState == ServiceState.STATE_IN_SERVICE) &&
                (dataRegState != ServiceState.STATE_IN_SERVICE));
    }

    /** convert ServiceState registration code
     * to service state */
    private int regCodeToServiceState(int code) {
        switch (code) {
            case NetworkRegistrationInfo.REGISTRATION_STATE_HOME:
            case NetworkRegistrationInfo.REGISTRATION_STATE_ROAMING:
                return ServiceState.STATE_IN_SERVICE;
            default:
                return ServiceState.STATE_OUT_OF_SERVICE;
        }
    }

    /**
     * code is registration state 0-5 from TS 27.007 7.2
     * returns true if registered roam, false otherwise
     */
    private boolean regCodeIsRoaming (int code) {
        return NetworkRegistrationInfo.REGISTRATION_STATE_ROAMING == code;
    }

    private boolean isSameOperatorNameFromSimAndSS(ServiceState s) {
        String spn = ((TelephonyManager) mPhone.getContext().
                getSystemService(Context.TELEPHONY_SERVICE)).
                getSimOperatorNameForPhone(getPhoneId());

        // NOTE: in case of RUIM we should completely ignore the ERI data file and
        // mOperatorAlphaLong is set from RIL_REQUEST_OPERATOR response 0 (alpha ONS)
        String onsl = s.getOperatorAlphaLong();
        String onss = s.getOperatorAlphaShort();

        boolean equalsOnsl = !TextUtils.isEmpty(spn) && spn.equalsIgnoreCase(onsl);
        boolean equalsOnss = !TextUtils.isEmpty(spn) && spn.equalsIgnoreCase(onss);

        return (equalsOnsl || equalsOnss);
    }

    /**
     * Set roaming state if operator mcc is the same as sim mcc
     * and ons is not different from spn
     *
     * @param s ServiceState hold current ons
     * @return true if same operator
     */
    private boolean isSameNamedOperators(ServiceState s) {
        return currentMccEqualsSimMcc(s) && isSameOperatorNameFromSimAndSS(s);
    }

    /**
     * Compare SIM MCC with Operator MCC
     *
     * @param s ServiceState hold current ons
     * @return true if both are same
     */
    private boolean currentMccEqualsSimMcc(ServiceState s) {
        String simNumeric = ((TelephonyManager) mPhone.getContext().
                getSystemService(Context.TELEPHONY_SERVICE)).
                getSimOperatorNumericForPhone(getPhoneId());
        String operatorNumeric = s.getOperatorNumeric();
        boolean equalsMcc = true;

        try {
            equalsMcc = simNumeric.substring(0, 3).
                    equals(operatorNumeric.substring(0, 3));
        } catch (Exception e){
        }
        return equalsMcc;
    }

    /**
     * Do not set roaming state in case of oprators considered non-roaming.
     *
     * Can use mcc or mcc+mnc as item of
     * {@link CarrierConfigManager#KEY_NON_ROAMING_OPERATOR_STRING_ARRAY}.
     * For example, 302 or 21407. If mcc or mcc+mnc match with operator,
     * don't set roaming state.
     *
     * @param s ServiceState hold current ons
     * @return false for roaming state set
     */
    private boolean isOperatorConsideredNonRoaming(ServiceState s) {
        String operatorNumeric = s.getOperatorNumeric();

        PersistableBundle config = getCarrierConfig();
        String[] numericArray = config.getStringArray(
                CarrierConfigManager.KEY_NON_ROAMING_OPERATOR_STRING_ARRAY);

        if (ArrayUtils.isEmpty(numericArray) || operatorNumeric == null) {
            return false;
        }

        for (String numeric : numericArray) {
            if (!TextUtils.isEmpty(numeric) && operatorNumeric.startsWith(numeric)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOperatorConsideredRoaming(ServiceState s) {
        String operatorNumeric = s.getOperatorNumeric();
        PersistableBundle config = getCarrierConfig();
        String[] numericArray = config.getStringArray(
                CarrierConfigManager.KEY_ROAMING_OPERATOR_STRING_ARRAY);
        if (ArrayUtils.isEmpty(numericArray) || operatorNumeric == null) {
            return false;
        }

        for (String numeric : numericArray) {
            if (!TextUtils.isEmpty(numeric) && operatorNumeric.startsWith(numeric)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set restricted state based on the OnRestrictedStateChanged notification
     * If any voice or packet restricted state changes, trigger a UI
     * notification and notify registrants when sim is ready.
     *
     * @param ar an int value of RIL_RESTRICTED_STATE_*
     */
    private void onRestrictedStateChanged(AsyncResult ar) {
        RestrictedState newRs = new RestrictedState();

        if (DBG) log("onRestrictedStateChanged: E rs "+ mRestrictedState);

        if (ar.exception == null && ar.result != null) {
            int state = (int)ar.result;

            newRs.setCsEmergencyRestricted(
                    ((state & RILConstants.RIL_RESTRICTED_STATE_CS_EMERGENCY) != 0) ||
                            ((state & RILConstants.RIL_RESTRICTED_STATE_CS_ALL) != 0) );
            //ignore the normal call and data restricted state before SIM READY
            if (mUiccApplcation != null && mUiccApplcation.getState() == AppState.APPSTATE_READY) {
                newRs.setCsNormalRestricted(
                        ((state & RILConstants.RIL_RESTRICTED_STATE_CS_NORMAL) != 0) ||
                                ((state & RILConstants.RIL_RESTRICTED_STATE_CS_ALL) != 0) );
                newRs.setPsRestricted(
                        (state & RILConstants.RIL_RESTRICTED_STATE_PS_ALL)!= 0);
            }

            if (DBG) log("onRestrictedStateChanged: new rs "+ newRs);

            if (!mRestrictedState.isPsRestricted() && newRs.isPsRestricted()) {
                mPsRestrictEnabledRegistrants.notifyRegistrants();
                setNotification(PS_ENABLED);
            } else if (mRestrictedState.isPsRestricted() && !newRs.isPsRestricted()) {
                mPsRestrictDisabledRegistrants.notifyRegistrants();
                setNotification(PS_DISABLED);
            }

            /**
             * There are two kind of cs restriction, normal and emergency. So
             * there are 4 x 4 combinations in current and new restricted states
             * and we only need to notify when state is changed.
             */
            if (mRestrictedState.isCsRestricted()) {
                if (!newRs.isAnyCsRestricted()) {
                    // remove all restriction
                    setNotification(CS_DISABLED);
                } else if (!newRs.isCsNormalRestricted()) {
                    // remove normal restriction
                    setNotification(CS_EMERGENCY_ENABLED);
                } else if (!newRs.isCsEmergencyRestricted()) {
                    // remove emergency restriction
                    setNotification(CS_NORMAL_ENABLED);
                }
            } else if (mRestrictedState.isCsEmergencyRestricted() &&
                    !mRestrictedState.isCsNormalRestricted()) {
                if (!newRs.isAnyCsRestricted()) {
                    // remove all restriction
                    setNotification(CS_DISABLED);
                } else if (newRs.isCsRestricted()) {
                    // enable all restriction
                    setNotification(CS_ENABLED);
                } else if (newRs.isCsNormalRestricted()) {
                    // remove emergency restriction and enable normal restriction
                    setNotification(CS_NORMAL_ENABLED);
                }
            } else if (!mRestrictedState.isCsEmergencyRestricted() &&
                    mRestrictedState.isCsNormalRestricted()) {
                if (!newRs.isAnyCsRestricted()) {
                    // remove all restriction
                    setNotification(CS_DISABLED);
                } else if (newRs.isCsRestricted()) {
                    // enable all restriction
                    setNotification(CS_ENABLED);
                } else if (newRs.isCsEmergencyRestricted()) {
                    // remove normal restriction and enable emergency restriction
                    setNotification(CS_EMERGENCY_ENABLED);
                }
            } else {
                if (newRs.isCsRestricted()) {
                    // enable all restriction
                    setNotification(CS_ENABLED);
                } else if (newRs.isCsEmergencyRestricted()) {
                    // enable emergency restriction
                    setNotification(CS_EMERGENCY_ENABLED);
                } else if (newRs.isCsNormalRestricted()) {
                    // enable normal restriction
                    setNotification(CS_NORMAL_ENABLED);
                }
            }

            mRestrictedState = newRs;
        }
        log("onRestrictedStateChanged: X rs "+ mRestrictedState);
    }

    /**
     * Get CellIdentity from the ServiceState if available or guess from cached
     *
     * Get the CellIdentity by first checking if ServiceState has a current CID. If so
     * then return that info. Otherwise, check the latest List<CellInfo> and return the first GSM or
     * WCDMA result that appears. If no GSM or WCDMA results, then return an LTE result. The
     * behavior is kept consistent for backwards compatibility; (do not apply logic to determine
     * why the behavior is this way).
     *
     * @return the current cell location if known or a non-null "empty" cell location
     */
    @NonNull
    public CellIdentity getCellIdentity() {
        if (mCellIdentity != null) return mCellIdentity;

        CellIdentity ci = getCellIdentityFromCellInfo(getAllCellInfo());
        if (ci != null) return ci;

        return mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA
                ? new CellIdentityCdma() : new CellIdentityGsm();
    }

    /**
     * Get CellIdentity from the ServiceState if available or guess from CellInfo
     *
     * Get the CellLocation by first checking if ServiceState has a current CID. If so
     * then return that info. Otherwise, query AllCellInfo and return the first GSM or
     * WCDMA result that appears. If no GSM or WCDMA results, then return an LTE result.
     * The behavior is kept consistent for backwards compatibility; (do not apply logic
     * to determine why the behavior is this way).
     *
     * @param workSource calling WorkSource
     * @param rspMsg the response message which must be non-null
     */
    public void requestCellIdentity(WorkSource workSource, Message rspMsg) {
        if (mCellIdentity != null) {
            AsyncResult.forMessage(rspMsg, mCellIdentity, null);
            rspMsg.sendToTarget();
            return;
        }

        Message cellLocRsp = obtainMessage(EVENT_CELL_LOCATION_RESPONSE, rspMsg);
        requestAllCellInfo(workSource, cellLocRsp);
    }

    /* Find and return a CellIdentity from CellInfo
     *
     * This method returns the first GSM or WCDMA result that appears in List<CellInfo>. If no GSM
     * or  WCDMA results are found, then it returns an LTE result. The behavior is kept consistent
     * for backwards compatibility; (do not apply logic to determine why the behavior is this way).
     *
     * @return the current CellIdentity from CellInfo or null
     */
    private static CellIdentity getCellIdentityFromCellInfo(List<CellInfo> info) {
        CellIdentity cl = null;
        if (info != null && info.size() > 0) {
            CellIdentity fallbackLteCid = null; // We prefer not to use LTE
            for (CellInfo ci : info) {
                CellIdentity c = ci.getCellIdentity();
                if (c instanceof CellIdentityLte && fallbackLteCid == null) {
                    if (getCidFromCellIdentity(c) != -1) fallbackLteCid = c;
                    continue;
                }
                if (getCidFromCellIdentity(c) != -1) {
                    cl = c;
                    break;
                }
            }
            if (cl == null && fallbackLteCid != null) {
                cl = fallbackLteCid;
            }
        }
        return cl;
    }

    /**
     * Handle the NITZ string from the modem
     *
     * @param nitzString NITZ time string in the form "yy/mm/dd,hh:mm:ss(+/-)tz,dt"
     * @param nitzReceiveTimeMs time according to {@link android.os.SystemClock#elapsedRealtime()}
     *        when the RIL sent the NITZ time to the framework
     * @param ageMs time in milliseconds indicating how long NITZ was cached in RIL and modem
     */
    private void setTimeFromNITZString(String nitzString, long nitzReceiveTimeMs, long ageMs) {
        long start = SystemClock.elapsedRealtime();
        if (DBG) {
            Rlog.d(LOG_TAG, "NITZ: " + nitzString + "," + nitzReceiveTimeMs + ", ageMs=" + ageMs
                    + " start=" + start + " delay=" + (start - nitzReceiveTimeMs));
        }
        NitzData newNitzData = NitzData.parse(nitzString);
        mLastNitzData = newNitzData;
        if (newNitzData != null) {
            try {
                NitzSignal nitzSignal = new NitzSignal(nitzReceiveTimeMs, newNitzData, ageMs);
                mNitzState.handleNitzReceived(nitzSignal);
            } finally {
                if (DBG) {
                    long end = SystemClock.elapsedRealtime();
                    Rlog.d(LOG_TAG, "NITZ: end=" + end + " dur=" + (end - start));
                }
            }
        }
    }

    /**
     * Cancels all notifications posted to NotificationManager for this subId. These notifications
     * for restricted state and rejection cause for cs registration are no longer valid after the
     * SIM has been removed.
     */
    private void cancelAllNotifications() {
        if (DBG) log("cancelAllNotifications: mPrevSubId=" + mPrevSubId);
        NotificationManager notificationManager = (NotificationManager)
                mPhone.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (SubscriptionManager.isValidSubscriptionId(mPrevSubId)) {
            notificationManager.cancel(Integer.toString(mPrevSubId), PS_NOTIFICATION);
            notificationManager.cancel(Integer.toString(mPrevSubId), CS_NOTIFICATION);
            notificationManager.cancel(Integer.toString(mPrevSubId), CS_REJECT_CAUSE_NOTIFICATION);

            // Cancel Emergency call warning and network preference notifications
            notificationManager.cancel(
                    CarrierServiceStateTracker.EMERGENCY_NOTIFICATION_TAG, mPrevSubId);
            notificationManager.cancel(
                    CarrierServiceStateTracker.PREF_NETWORK_NOTIFICATION_TAG, mPrevSubId);
        }
    }

    /**
     * Post a notification to NotificationManager for restricted state and
     * rejection cause for cs registration
     *
     * @param notifyType is one state of PS/CS_*_ENABLE/DISABLE
     */
    @VisibleForTesting
    public void setNotification(int notifyType) {
        if (DBG) log("setNotification: create notification " + notifyType);

        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            // notifications are posted per-sub-id, so return if current sub-id is invalid
            loge("cannot setNotification on invalid subid mSubId=" + mSubId);
            return;
        }
        Context context = mPhone.getContext();

        SubscriptionInfo info = mSubscriptionController
                .getActiveSubscriptionInfo(mPhone.getSubId(), context.getOpPackageName(),
                        context.getAttributionTag());

        //if subscription is part of a group and non-primary, suppress all notifications
        if (info == null || (info.isOpportunistic() && info.getGroupUuid() != null)) {
            log("cannot setNotification on invisible subid mSubId=" + mSubId);
            return;
        }

        // Needed because sprout RIL sends these when they shouldn't?
        boolean isSetNotification = context.getResources().getBoolean(
                com.android.internal.R.bool.config_user_notification_of_restrictied_mobile_access);
        if (!isSetNotification) {
            if (DBG) log("Ignore all the notifications");
            return;
        }

        boolean autoCancelCsRejectNotification = false;

        PersistableBundle bundle = getCarrierConfig();
        boolean disableVoiceBarringNotification = bundle.getBoolean(
                CarrierConfigManager.KEY_DISABLE_VOICE_BARRING_NOTIFICATION_BOOL, false);
        if (disableVoiceBarringNotification && (notifyType == CS_ENABLED
                || notifyType == CS_NORMAL_ENABLED
                || notifyType == CS_EMERGENCY_ENABLED)) {
            if (DBG) log("Voice/emergency call barred notification disabled");
            return;
        }
        autoCancelCsRejectNotification = bundle.getBoolean(
                CarrierConfigManager.KEY_AUTO_CANCEL_CS_REJECT_NOTIFICATION, false);

        CharSequence details = "";
        CharSequence title = "";
        int notificationId = CS_NOTIFICATION;
        int icon = com.android.internal.R.drawable.stat_sys_warning;

        final boolean multipleSubscriptions = (((TelephonyManager) mPhone.getContext()
                  .getSystemService(Context.TELEPHONY_SERVICE)).getPhoneCount() > 1);
        final int simNumber = mSubscriptionController.getSlotIndex(mSubId) + 1;

        switch (notifyType) {
            case PS_ENABLED:
                long dataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
                if (dataSubId != mPhone.getSubId()) {
                    return;
                }
                notificationId = PS_NOTIFICATION;
                title = context.getText(com.android.internal.R.string.RestrictedOnDataTitle);
                details = multipleSubscriptions
                        ? context.getString(
                                com.android.internal.R.string.RestrictedStateContentMsimTemplate,
                                simNumber) :
                        context.getText(com.android.internal.R.string.RestrictedStateContent);
                break;
            case PS_DISABLED:
                notificationId = PS_NOTIFICATION;
                break;
            case CS_ENABLED:
                title = context.getText(com.android.internal.R.string.RestrictedOnAllVoiceTitle);
                details = multipleSubscriptions
                        ? context.getString(
                                com.android.internal.R.string.RestrictedStateContentMsimTemplate,
                                simNumber) :
                        context.getText(com.android.internal.R.string.RestrictedStateContent);
                break;
            case CS_NORMAL_ENABLED:
                title = context.getText(com.android.internal.R.string.RestrictedOnNormalTitle);
                details = multipleSubscriptions
                        ? context.getString(
                                com.android.internal.R.string.RestrictedStateContentMsimTemplate,
                                simNumber) :
                        context.getText(com.android.internal.R.string.RestrictedStateContent);
                break;
            case CS_EMERGENCY_ENABLED:
                title = context.getText(com.android.internal.R.string.RestrictedOnEmergencyTitle);
                details = multipleSubscriptions
                        ? context.getString(
                                com.android.internal.R.string.RestrictedStateContentMsimTemplate,
                                simNumber) :
                        context.getText(com.android.internal.R.string.RestrictedStateContent);
                break;
            case CS_DISABLED:
                // do nothing and cancel the notification later
                break;
            case CS_REJECT_CAUSE_ENABLED:
                notificationId = CS_REJECT_CAUSE_NOTIFICATION;
                int resId = selectResourceForRejectCode(mRejectCode, multipleSubscriptions);
                if (0 == resId) {
                    if (autoCancelCsRejectNotification) {
                        notifyType = CS_REJECT_CAUSE_DISABLED;
                    } else {
                        loge("setNotification: mRejectCode=" + mRejectCode + " is not handled.");
                        return;
                    }
                } else {
                    icon = com.android.internal.R.drawable.stat_notify_mmcc_indication_icn;
                    // if using the single SIM resource, simNumber will be ignored
                    title = context.getString(resId, simNumber);
                    details = null;
                }
                break;
        }

        if (DBG) {
            log("setNotification, create notification, notifyType: " + notifyType
                    + ", title: " + title + ", details: " + details + ", subId: " + mSubId);
        }

        mNotification = new Notification.Builder(context)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setSmallIcon(icon)
                .setTicker(title)
                .setColor(context.getResources().getColor(
                        com.android.internal.R.color.system_notification_accent_color))
                .setContentTitle(title)
                .setStyle(new Notification.BigTextStyle().bigText(details))
                .setContentText(details)
                .setChannelId(NotificationChannelController.CHANNEL_ID_ALERT)
                .build();

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notifyType == PS_DISABLED || notifyType == CS_DISABLED
                || notifyType == CS_REJECT_CAUSE_DISABLED) {
            // cancel previous post notification
            notificationManager.cancel(Integer.toString(mSubId), notificationId);
        } else {
            boolean show = false;
            if (mSS.isEmergencyOnly() && notifyType == CS_EMERGENCY_ENABLED) {
                // if reg state is emergency only, always show restricted emergency notification.
                show = true;
            } else if (notifyType == CS_REJECT_CAUSE_ENABLED) {
                // always show notification due to CS reject irrespective of service state.
                show = true;
            } else if (mSS.getState() == ServiceState.STATE_IN_SERVICE) {
                // for non in service states, we have system UI and signal bar to indicate limited
                // service. No need to show notification again. This also helps to mitigate the
                // issue if phone go to OOS and camp to other networks and received restricted ind.
                show = true;
            }
            // update restricted state notification for this subId
            if (show) {
                notificationManager.notify(Integer.toString(mSubId), notificationId, mNotification);
            }
        }
    }

    /**
     * Selects the resource ID, which depends on rejection cause that is sent by the network when CS
     * registration is rejected.
     *
     * @param rejCode should be compatible with TS 24.008.
     */
    private int selectResourceForRejectCode(int rejCode, boolean multipleSubscriptions) {
        int rejResourceId = 0;
        switch (rejCode) {
            case 1:// Authentication reject
                rejResourceId = multipleSubscriptions
                        ? com.android.internal.R.string.mmcc_authentication_reject_msim_template :
                        com.android.internal.R.string.mmcc_authentication_reject;
                break;
            case 2:// IMSI unknown in HLR
                rejResourceId = multipleSubscriptions
                        ? com.android.internal.R.string.mmcc_imsi_unknown_in_hlr_msim_template :
                        com.android.internal.R.string.mmcc_imsi_unknown_in_hlr;
                break;
            case 3:// Illegal MS
                rejResourceId = multipleSubscriptions
                        ? com.android.internal.R.string.mmcc_illegal_ms_msim_template :
                        com.android.internal.R.string.mmcc_illegal_ms;
                break;
            case 6:// Illegal ME
                rejResourceId = multipleSubscriptions
                        ? com.android.internal.R.string.mmcc_illegal_me_msim_template :
                        com.android.internal.R.string.mmcc_illegal_me;
                break;
            default:
                // The other codes are not defined or not required by operators till now.
                break;
        }
        return rejResourceId;
    }

    private UiccCardApplication getUiccCardApplication() {
        if (mPhone.isPhoneTypeGsm()) {
            return mUiccController.getUiccCardApplication(mPhone.getPhoneId(),
                    UiccController.APP_FAM_3GPP);
        } else {
            return mUiccController.getUiccCardApplication(mPhone.getPhoneId(),
                    UiccController.APP_FAM_3GPP2);
        }
    }

    private void notifyCdmaSubscriptionInfoReady() {
        if (mCdmaForSubscriptionInfoReadyRegistrants != null) {
            if (DBG) log("CDMA_SUBSCRIPTION: call notifyRegistrants()");
            mCdmaForSubscriptionInfoReadyRegistrants.notifyRegistrants();
        }
    }

    /**
     * Registration point for transition into DataConnection attached.
     * @param transport Transport type
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForDataConnectionAttached(@TransportType int transport, Handler h, int what,
                                                  Object obj) {
        Registrant r = new Registrant(h, what, obj);
        if (mAttachedRegistrants.get(transport) == null) {
            mAttachedRegistrants.put(transport, new RegistrantList());
        }
        mAttachedRegistrants.get(transport).add(r);

        if (mSS != null) {
            NetworkRegistrationInfo netRegState = mSS.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, transport);
            if (netRegState == null || netRegState.isInService()) {
                r.notifyRegistrant();
            }
        }
    }

    /**
     * Unregister for data attached event
     *
     * @param transport Transport type
     * @param h Handler to notify
     */
    public void unregisterForDataConnectionAttached(@TransportType int transport, Handler h) {
        if (mAttachedRegistrants.get(transport) != null) {
            mAttachedRegistrants.get(transport).remove(h);
        }
    }

    /**
     * Registration point for transition into DataConnection detached.
     * @param transport Transport type
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForDataConnectionDetached(@TransportType int transport, Handler h, int what,
                                                  Object obj) {
        Registrant r = new Registrant(h, what, obj);
        if (mDetachedRegistrants.get(transport) == null) {
            mDetachedRegistrants.put(transport, new RegistrantList());
        }
        mDetachedRegistrants.get(transport).add(r);

        if (mSS != null) {
            NetworkRegistrationInfo netRegState = mSS.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, transport);
            if (netRegState != null && !netRegState.isInService()) {
                r.notifyRegistrant();
            }
        }
    }

    /**
     * Unregister for data detatched event
     *
     * @param transport Transport type
     * @param h Handler to notify
     */
    public void unregisterForDataConnectionDetached(@TransportType int transport, Handler h) {
        if (mDetachedRegistrants.get(transport) != null) {
            mDetachedRegistrants.get(transport).remove(h);
        }
    }

    /**
     * Registration for RIL Voice Radio Technology changing. The
     * new radio technology will be returned AsyncResult#result as an Integer Object.
     * The AsyncResult will be in the notification Message#obj.
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForVoiceRegStateOrRatChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mVoiceRegStateOrRatChangedRegistrants.add(r);
        notifyVoiceRegStateRilRadioTechnologyChanged();
    }

    public void unregisterForVoiceRegStateOrRatChanged(Handler h) {
        mVoiceRegStateOrRatChangedRegistrants.remove(h);
    }

    /**
     * Registration for DataConnection RIL Data Radio Technology changing. The
     * new radio technology will be returned AsyncResult#result as an Integer Object.
     * The AsyncResult will be in the notification Message#obj.
     *
     * @param transport Transport
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForDataRegStateOrRatChanged(@TransportType int transport, Handler h,
                                                    int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        if (mDataRegStateOrRatChangedRegistrants.get(transport) == null) {
            mDataRegStateOrRatChangedRegistrants.put(transport, new RegistrantList());
        }
        mDataRegStateOrRatChangedRegistrants.get(transport).add(r);
        Pair<Integer, Integer> registrationInfo = getRegistrationInfo(transport);
        if (registrationInfo != null) {
            r.notifyResult(registrationInfo);
        }
    }

    /**
     * Unregister for data registration state changed or RAT changed event
     *
     * @param transport Transport
     * @param h The handler
     */
    public void unregisterForDataRegStateOrRatChanged(@TransportType int transport, Handler h) {
        if (mDataRegStateOrRatChangedRegistrants.get(transport) != null) {
            mDataRegStateOrRatChangedRegistrants.get(transport).remove(h);
        }
    }

    /**
     * Registration for Airplane Mode changing.  The state of Airplane Mode will be returned
     * {@link AsyncResult#result} as a {@link Boolean} Object.
     * The {@link AsyncResult} will be in the notification {@link Message#obj}.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in {@link AsyncResult#userObj}
     */
    public void registerForAirplaneModeChanged(Handler h, int what, Object obj) {
        mAirplaneModeChangedRegistrants.add(h, what, obj);
    }

    /**
     * Unregister for Airplane Mode changed event.
     *
     * @param h The handler
     */
    public void unregisterForAirplaneModeChanged(Handler h) {
        mAirplaneModeChangedRegistrants.remove(h);
    }

    /**
     * Registration point for transition into network attached.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj in Message.obj
     */
    public void registerForNetworkAttached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);

        mNetworkAttachedRegistrants.add(r);
        if (mSS.getState() == ServiceState.STATE_IN_SERVICE) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForNetworkAttached(Handler h) {
        mNetworkAttachedRegistrants.remove(h);
    }

    /**
     * Registration point for transition into network detached.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj in Message.obj
     */
    public void registerForNetworkDetached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);

        mNetworkDetachedRegistrants.add(r);
        if (mSS.getState() != ServiceState.STATE_IN_SERVICE) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForNetworkDetached(Handler h) {
        mNetworkDetachedRegistrants.remove(h);
    }

    /**
     * Registration point for transition into packet service restricted zone.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForPsRestrictedEnabled(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mPsRestrictEnabledRegistrants.add(r);

        if (mRestrictedState.isPsRestricted()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForPsRestrictedEnabled(Handler h) {
        mPsRestrictEnabledRegistrants.remove(h);
    }

    /**
     * Registration point for transition out of packet service restricted zone.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForPsRestrictedDisabled(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mPsRestrictDisabledRegistrants.add(r);

        if (mRestrictedState.isPsRestricted()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForPsRestrictedDisabled(Handler h) {
        mPsRestrictDisabledRegistrants.remove(h);
    }

    /**
     * Registers for IMS capability changed.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForImsCapabilityChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mImsCapabilityChangedRegistrants.add(r);
    }

    /**
     * Unregisters for IMS capability changed.
     * @param h handler to notify
     */
    public void unregisterForImsCapabilityChanged(Handler h) {
        mImsCapabilityChangedRegistrants.remove(h);
    }

    /**
     * Register for service state changed event.
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     */
    public void registerForServiceStateChanged(Handler h, int what) {
        mServiceStateChangedRegistrants.addUnique(h, what, null);
    }

    /**
     * Unregister for service state changed event.
     *
     * @param h The handler.
     */
    public void unregisterForServiceStateChanged(Handler h) {
        mServiceStateChangedRegistrants.remove(h);
    }

    /**
     * Clean up existing voice and data connection then turn off radio power.
     *
     * Hang up the existing voice calls to decrease call drop rate.
     */
    public void powerOffRadioSafely() {
        synchronized (this) {
            if (!mPendingRadioPowerOffAfterDataOff) {
                if (mPhone.isUsingNewDataStack()) {
                    if (mAnyDataExisting) {
                        log("powerOffRadioSafely: Tear down all data networks.");
                        mPhone.getDataNetworkController().tearDownAllDataNetworks(
                                DataNetwork.TEAR_DOWN_REASON_AIRPLANE_MODE_ON);
                        sendEmptyMessageDelayed(EVENT_SET_RADIO_POWER_OFF,
                                POWER_OFF_ALL_DATA_NETWORKS_DISCONNECTED_TIMEOUT);
                    } else {
                        log("powerOffRadioSafely: No data is connected.");
                        sendEmptyMessage(EVENT_ALL_DATA_DISCONNECTED);
                    }
                    mPendingRadioPowerOffAfterDataOff = true;
                    return;
                }
                int dds = SubscriptionManager.getDefaultDataSubscriptionId();
                // To minimize race conditions we call cleanUpAllConnections on
                // both if else paths instead of before this isDisconnected test.
                if (mPhone.areAllDataDisconnected()
                        && (dds == mPhone.getSubId()
                        || (dds != mPhone.getSubId()
                        && ProxyController.getInstance().areAllDataDisconnected(dds)))) {
                    // To minimize race conditions we do this after isDisconnected
                    for (int transport : mAccessNetworksManager.getAvailableTransports()) {
                        if (mPhone.getDcTracker(transport) != null) {
                            mPhone.getDcTracker(transport).cleanUpAllConnections(
                                    Phone.REASON_RADIO_TURNED_OFF);
                        }
                    }
                    if (DBG) {
                        log("powerOffRadioSafely: Data disconnected, turn off radio now.");
                    }
                    hangupAndPowerOff();
                } else {
                    // hang up all active voice calls first
                    if (mPhone.isPhoneTypeGsm() && mPhone.isInCall()) {
                        mPhone.mCT.mRingingCall.hangupIfAlive();
                        mPhone.mCT.mBackgroundCall.hangupIfAlive();
                        mPhone.mCT.mForegroundCall.hangupIfAlive();
                    }
                    for (int transport : mAccessNetworksManager.getAvailableTransports()) {
                        if (mPhone.getDcTracker(transport) != null) {
                            mPhone.getDcTracker(transport).cleanUpAllConnections(
                                    Phone.REASON_RADIO_TURNED_OFF);
                        }
                    }

                    if (dds != mPhone.getSubId()
                            && !ProxyController.getInstance().areAllDataDisconnected(dds)) {
                        if (DBG) {
                            log(String.format("powerOffRadioSafely: Data is active on DDS (%d)."
                                    + " Wait for all data disconnect", dds));
                        }
                        // Data is not disconnected on DDS. Wait for the data disconnect complete
                        // before sending the RADIO_POWER off.
                        ProxyController.getInstance().registerForAllDataDisconnected(dds, this,
                                EVENT_ALL_DATA_DISCONNECTED);
                        mPendingRadioPowerOffAfterDataOff = true;
                    }
                    Message msg = Message.obtain(this);
                    msg.what = EVENT_SET_RADIO_POWER_OFF;
                    msg.arg1 = ++mPendingRadioPowerOffAfterDataOffTag;
                    if (sendMessageDelayed(msg, 30000)) {
                        if (DBG) {
                            log("powerOffRadioSafely: Wait up to 30s for data to isconnect, then"
                                    + " turn off radio.");
                        }
                        mPendingRadioPowerOffAfterDataOff = true;
                    } else {
                        log("powerOffRadioSafely: Cannot send delayed Msg, turn off radio right"
                                + " away.");
                        hangupAndPowerOff();
                        mPendingRadioPowerOffAfterDataOff = false;
                    }
                }
            }
        }
    }

    /**
     * process the pending request to turn radio off after data is disconnected
     *
     * return true if there is pending request to process; false otherwise.
     */
    public boolean processPendingRadioPowerOffAfterDataOff() {
        synchronized(this) {
            if (mPendingRadioPowerOffAfterDataOff) {
                if (DBG) log("Process pending request to turn radio off.");
                hangupAndPowerOff();
                mPendingRadioPowerOffAfterDataOffTag += 1;
                mPendingRadioPowerOffAfterDataOff = false;
                return true;
            }
            return false;
        }
    }

    private void onCarrierConfigChanged() {
        PersistableBundle config = getCarrierConfig();
        log("CarrierConfigChange " + config);

        // Load the ERI based on carrier config. Carrier might have their specific ERI.
        if (mEriManager != null) {
            mEriManager.loadEriFile();
            mCdnr.updateEfForEri(getOperatorNameFromEri());
        }

        updateOperatorNamePattern(config);
        mCdnr.updateEfFromCarrierConfig(config);
        mPhone.notifyCallForwardingIndicator();

        // Sometimes the network registration information comes before carrier config is ready.
        // For some cases like roaming/non-roaming overriding, we need carrier config. So it's
        // important to poll state again when carrier config is ready.
        pollStateInternal(false);
    }

    /**
     * Hang up all voice call and turn off radio. Implemented by derived class.
     */
    protected void hangupAndPowerOff() {
        if (mCi.getRadioState() == TelephonyManager.RADIO_POWER_OFF) return;
        // hang up all active voice calls
        if (!mPhone.isPhoneTypeGsm() || mPhone.isInCall()) {
            mPhone.mCT.mRingingCall.hangupIfAlive();
            mPhone.mCT.mBackgroundCall.hangupIfAlive();
            mPhone.mCT.mForegroundCall.hangupIfAlive();
        }

        mCi.setRadioPower(false, obtainMessage(EVENT_RADIO_POWER_OFF_DONE));

    }

    /** Cancel a pending (if any) pollState() operation */
    protected void cancelPollState() {
        // This will effectively cancel the rest of the poll requests.
        mPollingContext = new int[1];
    }

    /**
     * Return true if the network operator's country code changed.
     */
    private boolean networkCountryIsoChanged(String newCountryIsoCode, String prevCountryIsoCode) {
        // Return false if the new ISO code isn't valid as we don't know where we are.
        // Return true if the previous ISO code wasn't valid, or if it was and the new one differs.

        // If newCountryIsoCode is invalid then we'll return false
        if (TextUtils.isEmpty(newCountryIsoCode)) {
            if (DBG) {
                log("countryIsoChanged: no new country ISO code");
            }
            return false;
        }

        if (TextUtils.isEmpty(prevCountryIsoCode)) {
            if (DBG) {
                log("countryIsoChanged: no previous country ISO code");
            }
            return true;
        }
        return !newCountryIsoCode.equals(prevCountryIsoCode);
    }

    // Determine if the Icc card exists
    private boolean iccCardExists() {
        boolean iccCardExist = false;
        if (mUiccApplcation != null) {
            iccCardExist = mUiccApplcation.getState() != AppState.APPSTATE_UNKNOWN;
        }
        return iccCardExist;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public String getSystemProperty(String property, String defValue) {
        return TelephonyManager.getTelephonyProperty(mPhone.getPhoneId(), property, defValue);
    }

    public List<CellInfo> getAllCellInfo() {
        return mLastCellInfoList;
    }

    /** Set the minimum time between CellInfo requests to the modem, in milliseconds */
    public void setCellInfoMinInterval(int interval) {
        mCellInfoMinIntervalMs = interval;
    }

    /**
     * Request the latest CellInfo from the modem.
     *
     * If sufficient time has elapsed, then this request will be sent to the modem. Otherwise
     * the latest cached List<CellInfo> will be returned.
     *
     * @param workSource of the caller for power accounting
     * @param rspMsg an optional response message to get the response to the CellInfo request. If
     *     the rspMsg is not provided, then CellInfo will still be requested from the modem and
     *     cached locally for future lookup.
     */
    public void requestAllCellInfo(WorkSource workSource, Message rspMsg) {
        if (VDBG) log("SST.requestAllCellInfo(): E");
        if (mCi.getRilVersion() < 8) {
            AsyncResult.forMessage(rspMsg);
            rspMsg.sendToTarget();
            if (DBG) log("SST.requestAllCellInfo(): not implemented");
            return;
        }
        synchronized (mPendingCellInfoRequests) {
            // If there are pending requests, then we already have a request active, so add this
            // request to the response queue without initiating a new request.
            if (mIsPendingCellInfoRequest) {
                if (rspMsg != null) mPendingCellInfoRequests.add(rspMsg);
                return;
            }
            // Check to see whether the elapsed time is sufficient for a new request; if not, then
            // return the result of the last request (if expected).
            final long curTime = SystemClock.elapsedRealtime();
            if ((curTime - mLastCellInfoReqTime) < mCellInfoMinIntervalMs) {
                if (rspMsg != null) {
                    if (DBG) log("SST.requestAllCellInfo(): return last, back to back calls");
                    AsyncResult.forMessage(rspMsg, mLastCellInfoList, null);
                    rspMsg.sendToTarget();
                }
                return;
            }
            // If this request needs an explicit response (it's a synchronous request), then queue
            // the response message.
            if (rspMsg != null) mPendingCellInfoRequests.add(rspMsg);
            // Update the timeout window so that we don't delay based on slow responses
            mLastCellInfoReqTime = curTime;
            // Set a flag to remember that we have a pending cell info request
            mIsPendingCellInfoRequest = true;
            // Send a cell info request and also chase it with a timeout message
            Message msg = obtainMessage(EVENT_GET_CELL_INFO_LIST);
            mCi.getCellInfoList(msg, workSource);
            // This message will arrive TIMEOUT ms later and ensure that we don't wait forever for
            // a CELL_INFO response.
            sendMessageDelayed(
                    obtainMessage(EVENT_GET_CELL_INFO_LIST), CELL_INFO_LIST_QUERY_TIMEOUT);
        }
    }

    /**
     * Registration point for subscription info ready
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCdmaForSubscriptionInfoReadyRegistrants.add(r);

        if (isMinInfoReady()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForSubscriptionInfoReady(Handler h) {
        mCdmaForSubscriptionInfoReadyRegistrants.remove(h);
    }

    /**
     * Save current source of cdma subscription
     * @param source - 1 for NV, 0 for RUIM
     */
    private void saveCdmaSubscriptionSource(int source) {
        log("Storing cdma subscription source: " + source);
        Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                Settings.Global.CDMA_SUBSCRIPTION_MODE,
                source);
        log("Read from settings: " + Settings.Global.getInt(mPhone.getContext().getContentResolver(),
                Settings.Global.CDMA_SUBSCRIPTION_MODE, -1));
    }

    private void getSubscriptionInfoAndStartPollingThreads() {
        mCi.getCDMASubscription(obtainMessage(EVENT_POLL_STATE_CDMA_SUBSCRIPTION));

        // Get Registration Information
        pollStateInternal(false);
    }

    private void handleCdmaSubscriptionSource(int newSubscriptionSource) {
        log("Subscription Source : " + newSubscriptionSource);
        mIsSubscriptionFromRuim =
                (newSubscriptionSource == CdmaSubscriptionSourceManager.SUBSCRIPTION_FROM_RUIM);
        log("isFromRuim: " + mIsSubscriptionFromRuim);
        saveCdmaSubscriptionSource(newSubscriptionSource);
        if (!mIsSubscriptionFromRuim) {
            // NV is ready when subscription source is NV
            sendMessage(obtainMessage(EVENT_NV_READY));
        }
    }

    /** Called when telecom has reported a voice service state change. */
    public void onTelecomVoiceServiceStateOverrideChanged() {
        sendMessage(obtainMessage(EVENT_TELECOM_VOICE_SERVICE_STATE_OVERRIDE_CHANGED));
    }

    private void dumpCellInfoList(PrintWriter pw) {
        pw.print(" mLastCellInfoList={");
        if(mLastCellInfoList != null) {
            boolean first = true;
            for(CellInfo info : mLastCellInfoList) {
               if(first == false) {
                   pw.print(",");
               }
               first = false;
               pw.print(info.toString());
            }
        }
        pw.println("}");
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ServiceStateTracker:");
        pw.println(" mSubId=" + mSubId);
        pw.println(" mSS=" + mSS);
        pw.println(" mNewSS=" + mNewSS);
        pw.println(" mVoiceCapable=" + mVoiceCapable);
        pw.println(" mRestrictedState=" + mRestrictedState);
        pw.println(" mPollingContext=" + mPollingContext + " - " +
                (mPollingContext != null ? mPollingContext[0] : ""));
        pw.println(" mDesiredPowerState=" + mDesiredPowerState);
        pw.println(" mRestrictedState=" + mRestrictedState);
        pw.println(" mPendingRadioPowerOffAfterDataOff=" + mPendingRadioPowerOffAfterDataOff);
        pw.println(" mPendingRadioPowerOffAfterDataOffTag=" + mPendingRadioPowerOffAfterDataOffTag);
        pw.println(" mCellIdentity=" + Rlog.pii(VDBG, mCellIdentity));
        pw.println(" mLastCellInfoReqTime=" + mLastCellInfoReqTime);
        dumpCellInfoList(pw);
        pw.flush();
        pw.println(" mAllowedNetworkTypes=" + mAllowedNetworkTypes);
        pw.println(" mAnyDataExisting=" + mAnyDataExisting);
        pw.println(" mMaxDataCalls=" + mMaxDataCalls);
        pw.println(" mNewMaxDataCalls=" + mNewMaxDataCalls);
        pw.println(" mReasonDataDenied=" + mReasonDataDenied);
        pw.println(" mNewReasonDataDenied=" + mNewReasonDataDenied);
        pw.println(" mGsmVoiceRoaming=" + mGsmVoiceRoaming);
        pw.println(" mGsmDataRoaming=" + mGsmDataRoaming);
        pw.println(" mEmergencyOnly=" + mEmergencyOnly);
        pw.println(" mCSEmergencyOnly=" + mCSEmergencyOnly);
        pw.println(" mPSEmergencyOnly=" + mPSEmergencyOnly);
        pw.flush();
        mNitzState.dumpState(pw);
        pw.println(" mLastNitzData=" + mLastNitzData);
        pw.flush();
        pw.println(" mStartedGprsRegCheck=" + mStartedGprsRegCheck);
        pw.println(" mReportedGprsNoReg=" + mReportedGprsNoReg);
        pw.println(" mNotification=" + mNotification);
        pw.println(" mCurSpn=" + mCurSpn);
        pw.println(" mCurDataSpn=" + mCurDataSpn);
        pw.println(" mCurShowSpn=" + mCurShowSpn);
        pw.println(" mCurPlmn=" + mCurPlmn);
        pw.println(" mCurShowPlmn=" + mCurShowPlmn);
        pw.flush();
        pw.println(" mCurrentOtaspMode=" + mCurrentOtaspMode);
        pw.println(" mRoamingIndicator=" + mRoamingIndicator);
        pw.println(" mIsInPrl=" + mIsInPrl);
        pw.println(" mDefaultRoamingIndicator=" + mDefaultRoamingIndicator);
        pw.println(" mRegistrationState=" + mRegistrationState);
        pw.println(" mMdn=" + mMdn);
        pw.println(" mHomeSystemId=" + mHomeSystemId);
        pw.println(" mHomeNetworkId=" + mHomeNetworkId);
        pw.println(" mMin=" + mMin);
        pw.println(" mPrlVersion=" + mPrlVersion);
        pw.println(" mIsMinInfoReady=" + mIsMinInfoReady);
        pw.println(" mIsEriTextLoaded=" + mIsEriTextLoaded);
        pw.println(" mIsSubscriptionFromRuim=" + mIsSubscriptionFromRuim);
        pw.println(" mCdmaSSM=" + mCdmaSSM);
        pw.println(" mRegistrationDeniedReason=" + mRegistrationDeniedReason);
        pw.println(" mCurrentCarrier=" + mCurrentCarrier);
        pw.flush();
        pw.println(" mImsRegistered=" + mImsRegistered);
        pw.println(" mImsRegistrationOnOff=" + mImsRegistrationOnOff);
        pw.println(" pending radio off event="
                + hasMessages(EVENT_POWER_OFF_RADIO_IMS_DEREG_TIMEOUT));
        pw.println(" mRadioDisabledByCarrier" + mRadioDisabledByCarrier);
        pw.println(" mDeviceShuttingDown=" + mDeviceShuttingDown);
        pw.println(" mSpnUpdatePending=" + mSpnUpdatePending);
        pw.println(" mCellInfoMinIntervalMs=" + mCellInfoMinIntervalMs);
        pw.println(" mEriManager=" + mEriManager);

        mLocaleTracker.dump(fd, pw, args);
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "    ");

        mCdnr.dump(ipw);

        ipw.println(" Carrier Display Name update records:");
        ipw.increaseIndent();
        mCdnrLogs.dump(fd, ipw, args);
        ipw.decreaseIndent();

        ipw.println(" Roaming Log:");
        ipw.increaseIndent();
        mRoamingLog.dump(fd, ipw, args);
        ipw.decreaseIndent();

        ipw.println(" Attach Log:");
        ipw.increaseIndent();
        mAttachLog.dump(fd, ipw, args);
        ipw.decreaseIndent();

        ipw.println(" Phone Change Log:");
        ipw.increaseIndent();
        mPhoneTypeLog.dump(fd, ipw, args);
        ipw.decreaseIndent();

        ipw.println(" Rat Change Log:");
        ipw.increaseIndent();
        mRatLog.dump(fd, ipw, args);
        ipw.decreaseIndent();

        ipw.println(" Radio power Log:");
        ipw.increaseIndent();
        mRadioPowerLog.dump(fd, ipw, args);
        ipw.decreaseIndent();

        mNitzState.dumpLogs(fd, ipw, args);

        ipw.flush();
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isImsRegistered() {
        return mImsRegistered;
    }
    /**
     * Verifies the current thread is the same as the thread originally
     * used in the initialization of this instance. Throws RuntimeException
     * if not.
     *
     * @exception RuntimeException if the current thread is not
     * the thread that originally obtained this Phone instance.
     */
    protected void checkCorrectThread() {
        if (Thread.currentThread() != getLooper().getThread()) {
            throw new RuntimeException(
                    "ServiceStateTracker must be used from within one thread");
        }
    }

    protected boolean isCallerOnDifferentThread() {
        boolean value = Thread.currentThread() != getLooper().getThread();
        if (VDBG) log("isCallerOnDifferentThread: " + value);
        return value;
    }

    /**
     * Check ISO country by MCC to see if phone is roaming in same registered country
     */
    protected boolean inSameCountry(String operatorNumeric) {
        if (TextUtils.isEmpty(operatorNumeric) || (operatorNumeric.length() < 5)) {
            // Not a valid network
            return false;
        }
        final String homeNumeric = getHomeOperatorNumeric();
        if (TextUtils.isEmpty(homeNumeric) || (homeNumeric.length() < 5)) {
            // Not a valid SIM MCC
            return false;
        }
        boolean inSameCountry = true;
        final String networkMCC = operatorNumeric.substring(0, 3);
        final String homeMCC = homeNumeric.substring(0, 3);
        final String networkCountry = MccTable.countryCodeForMcc(networkMCC);
        final String homeCountry = MccTable.countryCodeForMcc(homeMCC);

        if (mLocaleTracker != null && !TextUtils.isEmpty(mLocaleTracker.getCountryOverride())) {
            log("inSameCountry:  countryOverride var set.  This should only be set for testing "
                    + "purposes to override the device location.");
            return mLocaleTracker.getCountryOverride().equals(homeCountry);
        }

        if (networkCountry.isEmpty() || homeCountry.isEmpty()) {
            // Not a valid country
            return false;
        }
        inSameCountry = homeCountry.equals(networkCountry);
        if (inSameCountry) {
            return inSameCountry;
        }
        // special same country cases
        if ("us".equals(homeCountry) && "vi".equals(networkCountry)) {
            inSameCountry = true;
        } else if ("vi".equals(homeCountry) && "us".equals(networkCountry)) {
            inSameCountry = true;
        }
        return inSameCountry;
    }

    /**
     * Set both voice and data roaming type,
     * judging from the ISO country of SIM VS network.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void setRoamingType(ServiceState currentServiceState) {
        final boolean isVoiceInService =
                (currentServiceState.getState() == ServiceState.STATE_IN_SERVICE);
        if (isVoiceInService) {
            if (currentServiceState.getVoiceRoaming()) {
                if (mPhone.isPhoneTypeGsm()) {
                    // check roaming type by MCC
                    if (inSameCountry(currentServiceState.getOperatorNumeric())) {
                        currentServiceState.setVoiceRoamingType(
                                ServiceState.ROAMING_TYPE_DOMESTIC);
                    } else {
                        currentServiceState.setVoiceRoamingType(
                                ServiceState.ROAMING_TYPE_INTERNATIONAL);
                    }
                } else {
                    // some carrier defines international roaming by indicator
                    int[] intRoamingIndicators = mPhone.getContext().getResources().getIntArray(
                            com.android.internal.R.array
                                    .config_cdma_international_roaming_indicators);
                    if ((intRoamingIndicators != null) && (intRoamingIndicators.length > 0)) {
                        // It's domestic roaming at least now
                        currentServiceState.setVoiceRoamingType(ServiceState.ROAMING_TYPE_DOMESTIC);
                        int curRoamingIndicator = currentServiceState.getCdmaRoamingIndicator();
                        for (int i = 0; i < intRoamingIndicators.length; i++) {
                            if (curRoamingIndicator == intRoamingIndicators[i]) {
                                currentServiceState.setVoiceRoamingType(
                                        ServiceState.ROAMING_TYPE_INTERNATIONAL);
                                break;
                            }
                        }
                    } else {
                        // check roaming type by MCC
                        if (inSameCountry(currentServiceState.getOperatorNumeric())) {
                            currentServiceState.setVoiceRoamingType(
                                    ServiceState.ROAMING_TYPE_DOMESTIC);
                        } else {
                            currentServiceState.setVoiceRoamingType(
                                    ServiceState.ROAMING_TYPE_INTERNATIONAL);
                        }
                    }
                }
            } else {
                currentServiceState.setVoiceRoamingType(ServiceState.ROAMING_TYPE_NOT_ROAMING);
            }
        }
        final boolean isDataInService =
                (currentServiceState.getDataRegistrationState() == ServiceState.STATE_IN_SERVICE);
        final int dataRegType = getRilDataRadioTechnologyForWwan(currentServiceState);
        if (isDataInService) {
            if (!currentServiceState.getDataRoaming()) {
                currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_NOT_ROAMING);
            } else {
                if (mPhone.isPhoneTypeGsm()) {
                    if (ServiceState.isGsm(dataRegType)) {
                        if (isVoiceInService) {
                            // GSM data should have the same state as voice
                            currentServiceState.setDataRoamingType(currentServiceState
                                    .getVoiceRoamingType());
                        } else {
                            // we can not decide GSM data roaming type without voice
                            currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_UNKNOWN);
                        }
                    } else {
                        // we can not decide 3gpp2 roaming state here
                        currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_UNKNOWN);
                    }
                } else {
                    if (ServiceState.isCdma(dataRegType)) {
                        if (isVoiceInService) {
                            // CDMA data should have the same state as voice
                            currentServiceState.setDataRoamingType(currentServiceState
                                    .getVoiceRoamingType());
                        } else {
                            // we can not decide CDMA data roaming type without voice
                            // set it as same as last time
                            currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_UNKNOWN);
                        }
                    } else {
                        // take it as 3GPP roaming
                        if (inSameCountry(currentServiceState.getOperatorNumeric())) {
                            currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_DOMESTIC);
                        } else {
                            currentServiceState.setDataRoamingType(
                                    ServiceState.ROAMING_TYPE_INTERNATIONAL);
                        }
                    }
                }
            }
        }
    }

    protected String getHomeOperatorNumeric() {
        String numeric = ((TelephonyManager) mPhone.getContext().
                getSystemService(Context.TELEPHONY_SERVICE)).
                getSimOperatorNumericForPhone(mPhone.getPhoneId());
        if (!mPhone.isPhoneTypeGsm() && TextUtils.isEmpty(numeric)) {
            numeric = SystemProperties.get(GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, "");
        }
        return numeric;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected int getPhoneId() {
        return mPhone.getPhoneId();
    }

    /* Reset Service state when IWLAN is enabled as polling in airplane mode
     * causes state to go to OUT_OF_SERVICE state instead of STATE_OFF
     */


    /**
     * This method adds IWLAN registration info for legacy mode devices camped on IWLAN. It also
     * makes some adjustments when the device camps on IWLAN in airplane mode.
     */
    private void processIwlanRegistrationInfo() {
        if (mCi.getRadioState() == TelephonyManager.RADIO_POWER_OFF) {
            boolean resetIwlanRatVal = false;
            log("set service state as POWER_OFF");
            if (ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                    == mNewSS.getRilDataRadioTechnology()) {
                log("pollStateDone: mNewSS = " + mNewSS);
                log("pollStateDone: reset iwlan RAT value");
                resetIwlanRatVal = true;
            }
            // operator info should be kept in SS
            String operator = mNewSS.getOperatorAlphaLong();
            mNewSS.setOutOfService(mAccessNetworksManager.isInLegacyMode(), true);
            if (resetIwlanRatVal) {
                mNewSS.setDataRegState(ServiceState.STATE_IN_SERVICE);
                NetworkRegistrationInfo nri = new NetworkRegistrationInfo.Builder()
                        .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WLAN)
                        .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                        .setAccessNetworkTechnology(TelephonyManager.NETWORK_TYPE_IWLAN)
                        .setRegistrationState(NetworkRegistrationInfo.REGISTRATION_STATE_HOME)
                        .build();
                mNewSS.addNetworkRegistrationInfo(nri);
                if (mAccessNetworksManager.isInLegacyMode()) {
                    // If in legacy mode, simulate the behavior that IWLAN registration info
                    // is reported through WWAN transport.
                    nri = new NetworkRegistrationInfo.Builder()
                            .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                            .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                            .setAccessNetworkTechnology(TelephonyManager.NETWORK_TYPE_IWLAN)
                            .setRegistrationState(NetworkRegistrationInfo.REGISTRATION_STATE_HOME)
                            .build();
                    mNewSS.addNetworkRegistrationInfo(nri);
                }
                mNewSS.setOperatorAlphaLong(operator);
                // Since it's in airplane mode, cellular must be out of service. The only possible
                // transport for data to go through is the IWLAN transport. Setting this to true
                // so that ServiceState.getDataNetworkType can report the right RAT.
                mNewSS.setIwlanPreferred(true);
                log("pollStateDone: mNewSS = " + mNewSS);
            }
            return;
        }

        // If the device operates in legacy mode and camps on IWLAN, modem reports IWLAN as a RAT
        // through WWAN registration info. To be consistent with the behavior with AP-assisted mode,
        // we manually make a WLAN registration info for clients to consume. In this scenario,
        // both WWAN and WLAN registration info are the IWLAN registration info and that's the
        // unfortunate limitation we have when the device operates in legacy mode. In AP-assisted
        // mode, the WWAN registration will correctly report the actual cellular registration info
        // when the device camps on IWLAN.
        if (mAccessNetworksManager.isInLegacyMode()) {
            NetworkRegistrationInfo wwanNri = mNewSS.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (wwanNri != null && wwanNri.getAccessNetworkTechnology()
                    == TelephonyManager.NETWORK_TYPE_IWLAN) {
                NetworkRegistrationInfo wlanNri = new NetworkRegistrationInfo.Builder()
                        .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WLAN)
                        .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                        .setRegistrationState(wwanNri.getRegistrationState())
                        .setAccessNetworkTechnology(TelephonyManager.NETWORK_TYPE_IWLAN)
                        .setRejectCause(wwanNri.getRejectCause())
                        .setEmergencyOnly(wwanNri.isEmergencyEnabled())
                        .setAvailableServices(wwanNri.getAvailableServices())
                        .build();
                mNewSS.addNetworkRegistrationInfo(wlanNri);
            }
        }
    }

    /**
     * Check if device is non-roaming and always on home network.
     *
     * @param b carrier config bundle obtained from CarrierConfigManager
     * @return true if network is always on home network, false otherwise
     * @see CarrierConfigManager
     */
    protected final boolean alwaysOnHomeNetwork(BaseBundle b) {
        return b.getBoolean(CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL);
    }

    /**
     * Check if the network identifier has membership in the set of
     * network identifiers stored in the carrier config bundle.
     *
     * @param b carrier config bundle obtained from CarrierConfigManager
     * @param network The network identifier to check network existence in bundle
     * @param key The key to index into the bundle presenting a string array of
     *            networks to check membership
     * @return true if network has membership in bundle networks, false otherwise
     * @see CarrierConfigManager
     */
    private boolean isInNetwork(BaseBundle b, String network, String key) {
        String[] networks = b.getStringArray(key);

        if (networks != null && Arrays.asList(networks).contains(network)) {
            return true;
        }
        return false;
    }

    protected final boolean isRoamingInGsmNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, CarrierConfigManager.KEY_GSM_ROAMING_NETWORKS_STRING_ARRAY);
    }

    protected final boolean isNonRoamingInGsmNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, CarrierConfigManager.KEY_GSM_NONROAMING_NETWORKS_STRING_ARRAY);
    }

    protected final boolean isRoamingInCdmaNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, CarrierConfigManager.KEY_CDMA_ROAMING_NETWORKS_STRING_ARRAY);
    }

    protected final boolean isNonRoamingInCdmaNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, CarrierConfigManager.KEY_CDMA_NONROAMING_NETWORKS_STRING_ARRAY);
    }

    /** Check if the device is shutting down. */
    public boolean isDeviceShuttingDown() {
        return mDeviceShuttingDown;
    }

    /**
     * Consider dataRegState if voiceRegState is OOS to determine SPN to be displayed.
     * If dataRegState is in service on IWLAN, also check for wifi calling enabled.
     * @param ss service state.
     */
    public int getCombinedRegState(ServiceState ss) {
        int regState = ss.getState();
        int dataRegState = ss.getDataRegistrationState();
        if ((regState == ServiceState.STATE_OUT_OF_SERVICE
                || regState == ServiceState.STATE_POWER_OFF)
                && (dataRegState == ServiceState.STATE_IN_SERVICE)) {
            if (ss.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_IWLAN) {
                if (mPhone.getImsPhone() != null && mPhone.getImsPhone().isWifiCallingEnabled()) {
                    log("getCombinedRegState: return STATE_IN_SERVICE for IWLAN as "
                            + "Data is in service and WFC is enabled");
                    regState = dataRegState;
                }
            } else {
                log("getCombinedRegState: return STATE_IN_SERVICE as Data is in service");
                regState = dataRegState;
            }
        }
        return regState;
    }

    /**
     * Gets the carrier configuration values for a particular subscription.
     *
     * @return A {@link PersistableBundle} containing the config for the given subId,
     *         or default values for an invalid subId.
     */
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

    public LocaleTracker getLocaleTracker() {
        return mLocaleTracker;
    }

    String getCdmaEriText(int roamInd, int defRoamInd) {
        return mEriManager != null ? mEriManager.getCdmaEriText(roamInd, defRoamInd) : "no ERI";
    }

    private void updateOperatorNamePattern(PersistableBundle config) {
        String operatorNamePattern = config.getString(
                CarrierConfigManager.KEY_OPERATOR_NAME_FILTER_PATTERN_STRING);
        if (!TextUtils.isEmpty(operatorNamePattern)) {
            mOperatorNameStringPattern = Pattern.compile(operatorNamePattern);
            if (DBG) {
                log("mOperatorNameStringPattern: " + mOperatorNameStringPattern.toString());
            }
        }
    }

    private void updateOperatorNameForServiceState(ServiceState servicestate) {
        if (servicestate == null) {
            return;
        }

        servicestate.setOperatorName(
                filterOperatorNameByPattern(servicestate.getOperatorAlphaLong()),
                filterOperatorNameByPattern(servicestate.getOperatorAlphaShort()),
                servicestate.getOperatorNumeric());

        List<NetworkRegistrationInfo> networkRegistrationInfos =
                servicestate.getNetworkRegistrationInfoList();

        for (int i = 0; i < networkRegistrationInfos.size(); i++) {
            if (networkRegistrationInfos.get(i) != null) {
                updateOperatorNameForCellIdentity(
                        networkRegistrationInfos.get(i).getCellIdentity());
                servicestate.addNetworkRegistrationInfo(networkRegistrationInfos.get(i));
            }
        }
    }

    private void updateOperatorNameForCellIdentity(CellIdentity cellIdentity) {
        if (cellIdentity == null) {
            return;
        }
        cellIdentity.setOperatorAlphaLong(
                filterOperatorNameByPattern((String) cellIdentity.getOperatorAlphaLong()));
        cellIdentity.setOperatorAlphaShort(
                filterOperatorNameByPattern((String) cellIdentity.getOperatorAlphaShort()));
    }

    /**
     * To modify the operator name of CellInfo by pattern.
     *
     * @param cellInfos List of CellInfo{@link CellInfo}.
     */
    public void updateOperatorNameForCellInfo(List<CellInfo> cellInfos) {
        if (cellInfos == null || cellInfos.isEmpty()) {
            return;
        }
        for (CellInfo cellInfo : cellInfos) {
            if (cellInfo.isRegistered()) {
                updateOperatorNameForCellIdentity(cellInfo.getCellIdentity());
            }
        }
    }

    /**
     * To modify the operator name by pattern.
     *
     * @param operatorName Registered operator name
     * @return An operator name.
     */
    public String filterOperatorNameByPattern(String operatorName) {
        if (mOperatorNameStringPattern == null || TextUtils.isEmpty(operatorName)) {
            return operatorName;
        }
        Matcher matcher = mOperatorNameStringPattern.matcher(operatorName);
        if (matcher.find()) {
            if (matcher.groupCount() > 0) {
                operatorName = matcher.group(1);
            } else {
                log("filterOperatorNameByPattern: pattern no group");
            }
        }
        return operatorName;
    }

    @RilRadioTechnology
    private static int getRilDataRadioTechnologyForWwan(ServiceState ss) {
        NetworkRegistrationInfo regInfo = ss.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (regInfo != null) {
            networkType = regInfo.getAccessNetworkTechnology();
        }
        return ServiceState.networkTypeToRilRadioTechnology(networkType);
    }

    /**
     * Registers for 5G NR state changed.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForNrStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mNrStateChangedRegistrants.add(r);
    }

    /**
     * Unregisters for 5G NR state changed.
     * @param h handler to notify
     */
    public void unregisterForNrStateChanged(Handler h) {
        mNrStateChangedRegistrants.remove(h);
    }

    /**
     * Registers for 5G NR frequency changed.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForNrFrequencyChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mNrFrequencyChangedRegistrants.add(r);
    }

    /**
     * Unregisters for 5G NR frequency changed.
     * @param h handler to notify
     */
    public void unregisterForNrFrequencyChanged(Handler h) {
        mNrFrequencyChangedRegistrants.remove(h);
    }

    /**
     * Registers for CSS indicator changed.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForCssIndicatorChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCssIndicatorChangedRegistrants.add(r);
    }

    /**
     * Unregisters for CSS indicator changed.
     * @param h handler to notify
     */
    public void unregisterForCssIndicatorChanged(Handler h) {
        mCssIndicatorChangedRegistrants.remove(h);
    }

    /**
     * Registers for cell bandwidth changed.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForBandwidthChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mBandwidthChangedRegistrants.add(r);
    }

    /**
     * Unregisters for cell bandwidth changed.
     * @param h handler to notify
     */
    public void unregisterForBandwidthChanged(Handler h) {
        mBandwidthChangedRegistrants.remove(h);
    }

    /**
     * Get the NR data connection context ids.
     *
     * @return data connection context ids.
     */
    @NonNull
    public Set<Integer> getNrContextIds() {
        Set<Integer> idSet = new HashSet<>();

        if (!ArrayUtils.isEmpty(mLastPhysicalChannelConfigList)) {
            for (PhysicalChannelConfig config : mLastPhysicalChannelConfigList) {
                if (isNrPhysicalChannelConfig(config)) {
                    for (int id : config.getContextIds()) {
                        idSet.add(id);
                    }
                }
            }
        }

        return idSet;
    }

    private void setDataNetworkTypeForPhone(int type) {
        if (mPhone.getUnitTestMode()) {
            return;
        }
        TelephonyManager tm = (TelephonyManager) mPhone.getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        tm.setDataNetworkTypeForPhone(mPhone.getPhoneId(), type);
    }

    /** Returns the {@link ServiceStateStats} for the phone tracked. */
    public ServiceStateStats getServiceStateStats() {
        return mServiceStateStats;
    }

    /** Replaces the {@link ServiceStateStats} for testing purposes. */
    @VisibleForTesting
    public void setServiceStateStats(ServiceStateStats serviceStateStats) {
        mServiceStateStats = serviceStateStats;
    }

    /**
     * Used to insert a ServiceState into the ServiceStateProvider as a ContentValues instance.
     *
     * Copied from packages/services/Telephony/src/com/android/phone/ServiceStateProvider.java
     *
     * @param state the ServiceState to convert into ContentValues
     * @return the convertedContentValues instance
     */
    private ContentValues getContentValuesForServiceState(ServiceState state) {
        ContentValues values = new ContentValues();
        final Parcel p = Parcel.obtain();
        state.writeToParcel(p, 0);
        // Turn the parcel to byte array. Safe to do this because the content values were never
        // written into a persistent storage. ServiceStateProvider keeps values in the memory.
        values.put(SERVICE_STATE, p.marshall());
        return values;
    }

    /**
     * Registers for TAC/LAC changed event.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForAreaCodeChanged(Handler h, int what, Object obj) {
        mAreaCodeChangedRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for TAC/LAC changed event.
     * @param h handler to notify
     */
    public void unregisterForAreaCodeChanged(Handler h) {
        mAreaCodeChangedRegistrants.remove(h);
    }

    /**
     * get last known cell identity
     * If there is current registered network this value will be same as the registered cell
     * identity. If the device goes out of service the previous cell identity is cached and
     * will be returned. If the cache age of the cell identity is more than 24 hours
     * it will be cleared and null will be returned.
     * @return last known cell identity.
     */
    public @Nullable CellIdentity getLastKnownCellIdentity() {
        return mLastKnownCellIdentity;
    }
}

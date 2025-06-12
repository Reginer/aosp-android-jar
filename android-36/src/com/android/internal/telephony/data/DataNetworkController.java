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

import android.annotation.CallbackExecutor;
import android.annotation.ElapsedRealtimeLong;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkPolicyManager;
import android.net.NetworkPolicyManager.SubscriptionCallback;
import android.net.NetworkRequest;
import android.net.NetworkTemplate;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.telecom.TelecomManager;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.AccessNetworkConstants.RadioAccessNetworkType;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.DataActivityType;
import android.telephony.Annotation.DataFailureCause;
import android.telephony.Annotation.NetCapability;
import android.telephony.Annotation.NetworkType;
import android.telephony.Annotation.ValidationStatus;
import android.telephony.AnomalyReporter;
import android.telephony.CarrierConfigManager;
import android.telephony.CellSignalStrength;
import android.telephony.DataFailCause;
import android.telephony.DataSpecificRegistrationInfo;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.NetworkRegistrationInfo.RegistrationState;
import android.telephony.PcoData;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.SubscriptionPlan;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.DataState;
import android.telephony.TelephonyManager.SimState;
import android.telephony.TelephonyRegistryManager;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataCallResponse.HandoverFailureMode;
import android.telephony.data.DataCallResponse.LinkStatus;
import android.telephony.data.DataProfile;
import android.telephony.data.DataServiceCallback;
import android.telephony.data.QosBearerSession;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsRegistrationAttributes;
import android.telephony.ims.ImsStateCallback;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.feature.ImsFeature;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SlidingWindowEventCounter;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.data.AccessNetworksManager.AccessNetworksManagerCallback;
import com.android.internal.telephony.data.DataConfigManager.DataConfigManagerCallback;
import com.android.internal.telephony.data.DataEvaluation.DataAllowedReason;
import com.android.internal.telephony.data.DataEvaluation.DataDisallowedReason;
import com.android.internal.telephony.data.DataEvaluation.DataEvaluationReason;
import com.android.internal.telephony.data.DataNetwork.DataNetworkCallback;
import com.android.internal.telephony.data.DataNetwork.TearDownReason;
import com.android.internal.telephony.data.DataProfileManager.DataProfileManagerCallback;
import com.android.internal.telephony.data.DataRetryManager.DataHandoverRetryEntry;
import com.android.internal.telephony.data.DataRetryManager.DataRetryEntry;
import com.android.internal.telephony.data.DataRetryManager.DataRetryManagerCallback;
import com.android.internal.telephony.data.DataRetryManager.DataSetupRetryEntry;
import com.android.internal.telephony.data.DataSettingsManager.DataSettingsManagerCallback;
import com.android.internal.telephony.data.DataStallRecoveryManager.DataStallRecoveryManagerCallback;
import com.android.internal.telephony.data.LinkBandwidthEstimator.LinkBandwidthEstimatorCallback;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.ims.ImsResolver;
import com.android.internal.telephony.satellite.SatelliteController;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DataNetworkController in the central module of the telephony data stack. It is responsible to
 * create and manage all the mobile data networks. It is per-SIM basis which means for DSDS devices,
 * there will be two DataNetworkController instances. Unlike the Android 12 DcTracker, which is
 * designed to be per-transport (i.e. cellular, IWLAN), DataNetworkController is designed to handle
 * data networks on both cellular and IWLAN.
 */
public class DataNetworkController extends Handler {
    private static final boolean VDBG = false;

    /** Event for adding a network request. */
    private static final int EVENT_ADD_NETWORK_REQUEST = 2;

    /** Event for removing a network request. */
    private static final int EVENT_REMOVE_NETWORK_REQUEST = 3;

    /** Event for SRVCC state changed. */
    private static final int EVENT_SRVCC_STATE_CHANGED = 4;

    /** Re-evaluate all unsatisfied network requests. */
    private static final int EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS = 5;

    /** Event for packet switch restricted enabled by network. */
    private static final int EVENT_PS_RESTRICT_ENABLED = 6;

    /** Event for packet switch restricted disabled by network. */
    private static final int EVENT_PS_RESTRICT_DISABLED = 7;

    /** Event for data service binding changed. */
    private static final int EVENT_DATA_SERVICE_BINDING_CHANGED = 8;

    /** Event for SIM state changed. */
    private static final int EVENT_SIM_STATE_CHANGED = 9;

    /** Event for tearing down all data networks. */
    private static final int EVENT_TEAR_DOWN_ALL_DATA_NETWORKS = 12;

    /** Event for registering data network controller callback. */
    private static final int EVENT_REGISTER_DATA_NETWORK_CONTROLLER_CALLBACK = 13;

    /** Event for unregistering data network controller callback. */
    private static final int EVENT_UNREGISTER_DATA_NETWORK_CONTROLLER_CALLBACK = 14;

    /** Event for subscription info changed. */
    private static final int EVENT_SUBSCRIPTION_CHANGED = 15;

    /** Event for re-evaluating existing data networks. */
    private static final int EVENT_REEVALUATE_EXISTING_DATA_NETWORKS = 16;

    /** Event for data RAT or registration state changed. */
    private static final int EVENT_SERVICE_STATE_CHANGED = 17;

    /** Event for voice call ended. */
    private static final int EVENT_VOICE_CALL_ENDED = 18;

    /** Event for registering all events. */
    private static final int EVENT_REGISTER_ALL_EVENTS = 19;

    /** Event for emergency call started or ended. */
    private static final int EVENT_EMERGENCY_CALL_CHANGED = 20;

    /** Event for evaluating preferred transport. */
    private static final int EVENT_EVALUATE_PREFERRED_TRANSPORT = 21;

    /** Event for subscription plans changed. */
    private static final int EVENT_SUBSCRIPTION_PLANS_CHANGED = 22;

    /** Event for unmetered or congested subscription override. */
    private static final int EVENT_SUBSCRIPTION_OVERRIDE = 23;

    /** Event for slice config changed. */
    private static final int EVENT_SLICE_CONFIG_CHANGED = 24;

    /** Event for tracking area code changed. */
    private static final int EVENT_TAC_CHANGED = 25;

    /** The supported IMS features. This is for IMS graceful tear down support. */
    private static final Collection<Integer> SUPPORTED_IMS_FEATURES =
            List.of(ImsFeature.FEATURE_MMTEL, ImsFeature.FEATURE_RCS);

    /** The maximum number of previously connected data networks for debugging purposes. */
    private static final int MAX_HISTORICAL_CONNECTED_DATA_NETWORKS = 10;

    /**
     * The delay in milliseconds to re-evaluate preferred transport when handover failed and
     * fallback to source.
     */
    private static final long REEVALUATE_PREFERRED_TRANSPORT_DELAY_MILLIS =
            TimeUnit.SECONDS.toMillis(3);

    /** The delay in milliseconds to re-evaluate unsatisfied network requests after call end. */
    private static final long REEVALUATE_UNSATISFIED_NETWORK_REQUESTS_AFTER_CALL_END_DELAY_MILLIS =
            TimeUnit.MILLISECONDS.toMillis(500);

    /** The delay in milliseconds to re-evaluate unsatisfied network requests after TAC changes. */
    private static final long REEVALUATE_UNSATISFIED_NETWORK_REQUESTS_TAC_CHANGED_DELAY_MILLIS =
            TimeUnit.MILLISECONDS.toMillis(100);

    /**
     * The delay in milliseconds to re-evaluate unsatisfied network requests after network request
     * detached.
     */
    private static final long REEVALUATE_UNSATISFIED_NETWORK_REQUESTS_AFTER_DETACHED_DELAY_MILLIS =
            TimeUnit.SECONDS.toMillis(1);

    /**
     * The guard timer in milliseconds to limit querying the data usage api stats frequently
     */
    private static final long GUARD_TIMER_INTERVAL_TO_QUERY_DATA_USAGE_API_STATS_MILLIS =
            TimeUnit.SECONDS.toMillis(1);

    /**
     * bootstrap sim total data usage bytes
     */
    private long mBootStrapSimTotalDataUsageBytes = 0L;

    /**
     * bootstrap sim last data usage query time
     */
    @ElapsedRealtimeLong
    private long mBootstrapSimLastDataUsageQueryTime = 0L;

    private final Phone mPhone;
    private final String mLogTag;
    private final LocalLog mLocalLog = new LocalLog(128);

    @NonNull
    private final DataConfigManager mDataConfigManager;
    @NonNull
    private final DataSettingsManager mDataSettingsManager;
    @NonNull
    private final DataProfileManager mDataProfileManager;
    @NonNull
    private final DataStallRecoveryManager mDataStallRecoveryManager;
    @NonNull
    private final AccessNetworksManager mAccessNetworksManager;
    @NonNull
    private final DataRetryManager mDataRetryManager;
    @NonNull
    private final ImsManager mImsManager;
    @NonNull
    private final TelecomManager mTelecomManager;
    @NonNull
    private final NetworkPolicyManager mNetworkPolicyManager;
    @NonNull
    private final SparseArray<DataServiceManager> mDataServiceManagers = new SparseArray<>();

    /** The subscription index associated with this data network controller. */
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    /** The current service state of the device. */
    // Note that keeping a copy here instead of directly using ServiceStateTracker.getServiceState()
    // is intended for detecting the delta.
    @NonNull
    private ServiceState mServiceState;

    /** The list of SubscriptionPlans, updated when initialized and when plans are changed. */
    @NonNull
    private final List<SubscriptionPlan> mSubscriptionPlans = new ArrayList<>();

    /**
     * The set of network types an unmetered override applies to, set by onSubscriptionOverride
     * and cleared when the device is rebooted or the override expires.
     */
    @NonNull
    @NetworkType
    private final Set<Integer> mUnmeteredOverrideNetworkTypes = new ArraySet<>();

    /**
     * The set of network types a congested override applies to, set by onSubscriptionOverride
     * and cleared when the device is rebooted or the override expires.
     */
    @NonNull
    @NetworkType
    private final Set<Integer> mCongestedOverrideNetworkTypes = new ArraySet<>();

    /**
     * The list of all network requests.
     */
    @NonNull
    private final NetworkRequestList mAllNetworkRequestList = new NetworkRequestList();

    /**
     * The current data network list, including the ones that are connected, connecting, or
     * disconnecting.
     */
    @NonNull
    private final List<DataNetwork> mDataNetworkList = new ArrayList<>();

    /** {@code true} indicating at least one data network exists. */
    private boolean mAnyDataNetworkExisting;

    /**
     * Contain the last 10 data networks that were connected. This is for debugging purposes only.
     */
    @NonNull
    private final List<DataNetwork> mPreviousConnectedDataNetworkList = new ArrayList<>();

    /**
     * The internet data network state. Note that this is the best effort if more than one
     * data network supports internet.
     */
    @DataState
    private int mInternetDataNetworkState = TelephonyManager.DATA_DISCONNECTED;

    /** All the current connected/handover internet networks.  */
    @NonNull
    private Set<DataNetwork> mConnectedInternetNetworks = new HashSet<>();

    /**
     * The IMS data network state. For now this is just for debugging purposes.
     */
    @DataState
    private int mImsDataNetworkState = TelephonyManager.DATA_DISCONNECTED;

    /** Overall aggregated link status from internet data networks. */
    @LinkStatus
    private int mInternetLinkStatus = DataCallResponse.LINK_STATUS_UNKNOWN;

    /** Data network controller callbacks. */
    @NonNull
    private final Set<DataNetworkControllerCallback> mDataNetworkControllerCallbacks =
            new ArraySet<>();

    /** Indicates if packet switch data is restricted by the cellular network. */
    private boolean mPsRestricted = false;

    /** Indicates if NR advanced is allowed by PCO. */
    private boolean mNrAdvancedCapableByPco = false;

    /** Indicates if srvcc is going on. */
    private boolean mIsSrvccHandoverInProcess = false;

    /**
     * Indicates if the data services are bound. Key if the transport type, and value is the boolean
     * indicating service is bound or not.
     */
    @NonNull
    private final SparseBooleanArray mDataServiceBound = new SparseBooleanArray();

    /** SIM state. */
    @SimState
    private int mSimState = TelephonyManager.SIM_STATE_UNKNOWN;

    /** Data activity. */
    @DataActivityType
    private int mDataActivity = TelephonyManager.DATA_ACTIVITY_NONE;

    /**
     * IMS state callbacks. Key is the IMS feature, value is the callback.
     */
    @NonNull
    private final SparseArray<ImsStateCallback> mImsStateCallbacks = new SparseArray<>();

    /** Registered IMS features. Unregistered IMS features are removed from the set. */
    @NonNull
    private final Set<Integer> mRegisteredImsFeatures = new ArraySet<>();

    /** IMS feature package names. Key is the IMS feature, value is the package name. */
    @NonNull
    private final SparseArray<String> mImsFeaturePackageName = new SparseArray<>();

    /**
     * Networks that are pending IMS de-registration. Key is the data network, value is the function
     * to tear down the network.
     */
    @NonNull
    private final Map<DataNetwork, Runnable> mPendingImsDeregDataNetworks = new ArrayMap<>();

    /**
     * IMS feature registration callback. The key is the IMS feature, the value is the registration
     * callback. When new SIM inserted, the old callbacks associated with the old subscription index
     * will be unregistered.
     */
    @NonNull
    private final SparseArray<RegistrationManager.RegistrationCallback>
            mImsFeatureRegistrationCallbacks = new SparseArray<>();

    /** The counter to detect back to back release/request IMS network. */
    @NonNull
    private SlidingWindowEventCounter mImsThrottleCounter;
    /** Event counter for unwanted network within time window, is used to trigger anomaly report. */
    @NonNull
    private SlidingWindowEventCounter mNetworkUnwantedCounter;
    /** Event counter for WLAN setup data failure within time window to trigger anomaly report. */
    @NonNull
    private SlidingWindowEventCounter mSetupDataCallWlanFailureCounter;
    /** Event counter for WWAN setup data failure within time window to trigger anomaly report. */
    @NonNull
    private SlidingWindowEventCounter mSetupDataCallWwanFailureCounter;

    /**
     * The capabilities of the latest released IMS request. To detect back to back release/request
     * IMS network.
     */
    private int[] mLastReleasedImsRequestCapabilities;

    /** True after try to release an IMS network; False after try to request an IMS network. */
    private boolean mLastImsOperationIsRelease;

    @NonNull
    private final FeatureFlags mFeatureFlags;

    /** The broadcast receiver. */
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED:
                case TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED:
                    if (mPhone.getPhoneId() == intent.getIntExtra(
                            SubscriptionManager.EXTRA_SLOT_INDEX,
                            SubscriptionManager.INVALID_SIM_SLOT_INDEX)) {
                        int simState = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                                TelephonyManager.SIM_STATE_UNKNOWN);
                        sendMessage(obtainMessage(EVENT_SIM_STATE_CHANGED, simState, 0));
                    }
            }
        }
    };

    private boolean hasCalling() {
        if (!TelephonyCapabilities.minimalTelephonyCdmCheck(mFeatureFlags)) return true;
        return mPhone.getContext().getPackageManager().hasSystemFeature(
            PackageManager.FEATURE_TELEPHONY_CALLING);
    }

    /**
     * The sorted network request list by priority. The highest priority network request stays at
     * the head of the list. The highest priority is 100, the lowest is 0.
     * <p>
     * Note this list is not thread-safe. Do not access the list from different threads.
     */
    @VisibleForTesting
    public static class NetworkRequestList extends LinkedList<TelephonyNetworkRequest> {
        /**
         * Constructor
         */
        public NetworkRequestList() {
        }

        /**
         * Copy constructor
         *
         * @param requestList The network request list.
         */
        public NetworkRequestList(@NonNull NetworkRequestList requestList) {
            addAll(requestList);
        }

        /**
         * Constructor
         *
         * @param requestList The network request list.
         */
        public NetworkRequestList(@NonNull List<TelephonyNetworkRequest> requestList) {
            addAll(requestList);
        }

        /**
         * Constructor
         *
         * @param newRequest The initial request of the list.
         */
        public NetworkRequestList(@NonNull TelephonyNetworkRequest newRequest) {
            this();
            add(newRequest);
        }

        /**
         * Add the network request to the list. Note that the item will be inserted to the position
         * based on the priority.
         *
         * @param newRequest The network request to be added.
         * @return {@code true} if added successfully. {@code false} if the request already exists.
         */
        @Override
        public boolean add(@NonNull TelephonyNetworkRequest newRequest) {
            int index = 0;
            while (index < size()) {
                TelephonyNetworkRequest networkRequest = get(index);
                if (networkRequest.equals(newRequest)) {
                    return false;   // Do not allow duplicate
                }
                if (newRequest.getPriority() > networkRequest.getPriority()) {
                    break;
                }
                index++;
            }
            super.add(index, newRequest);
            return true;
        }

        @Override
        public void add(int index, @NonNull TelephonyNetworkRequest newRequest) {
            throw new UnsupportedOperationException("Insertion to certain position is illegal.");
        }

        @Override
        public boolean addAll(Collection<? extends TelephonyNetworkRequest> requests) {
            for (TelephonyNetworkRequest networkRequest : requests) {
                add(networkRequest);
            }
            return true;
        }

        /**
         * Get the first network request that contains all the provided network capabilities.
         *
         * @param netCaps The network capabilities.
         * @return The first network request in the list that contains all the provided
         * capabilities.
         */
        @Nullable
        public TelephonyNetworkRequest get(@NonNull @NetCapability int[] netCaps) {
            int index = 0;
            while (index < size()) {
                TelephonyNetworkRequest networkRequest = get(index);
                // Check if any network requests contains all the provided capabilities.
                if (Arrays.stream(networkRequest.getCapabilities())
                        .boxed()
                        .collect(Collectors.toSet())
                        .containsAll(Arrays.stream(netCaps).boxed()
                                .toList())) {
                    return networkRequest;
                }
                index++;
            }
            return null;
        }

        /**
         * Check if any network request is requested by the specified package.
         *
         * @param packageName The package name.
         * @return {@code true} if any request is originated from the specified package.
         */
        public boolean hasNetworkRequestsFromPackage(@NonNull String packageName) {
            for (TelephonyNetworkRequest networkRequest : this) {
                if (packageName.equals(
                        networkRequest.getNativeNetworkRequest().getRequestorPackageName())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "[NetworkRequestList: size=" + size() + (size() > 0 ? ", leading by "
                    + get(0) : "") + "]";
        }

        /**
         * Print "capabilities - connectivity transport". e.g. INTERNET|NOT_RESTRICTED-SATELLITE
         */
        @NonNull
        public String toStringSimplified() {
            return size() > 0 ? DataUtils.networkCapabilitiesToString(get(0).getCapabilities())
                    + "-" + DataUtils.connectivityTransportsToString(get(0).getTransportTypes())
                    : "";
        }

        /**
         * Dump the network request list.
         *
         * @param pw print writer.
         */
        public void dump(IndentingPrintWriter pw) {
            pw.increaseIndent();
            for (TelephonyNetworkRequest networkRequest : this) {
                pw.println(networkRequest);
            }
            pw.decreaseIndent();
        }
    }

    /**
     * The data network controller callback. Note this is only used for passing information
     * internally in the data stack, should not be used externally.
     */
    public static class DataNetworkControllerCallback extends DataCallback {
        /**
         * Constructor
         *
         * @param executor The executor of the callback.
         */
        public DataNetworkControllerCallback(@NonNull @CallbackExecutor Executor executor) {
            super(executor);
        }

        /**
         * Called when internet data network validation status changed.
         *
         * @param validationStatus The validation status.
         */
        public void onInternetDataNetworkValidationStatusChanged(
                @ValidationStatus int validationStatus) {}

        /**
         * Called when a network that's capable of internet is newly connected or disconnected.
         *
         * @param internetNetworks The connected internet data network. It should be only one in
         *                         most of the cases.
         */
        public void onConnectedInternetDataNetworksChanged(@NonNull Set<DataNetwork>
                internetNetworks) {}

        /**
         * Called when data network is connected.
         *
         * @param transport Transport for the connected network.
         * @param dataProfile The data profile of the connected data network.
         */
        public void onDataNetworkConnected(@TransportType int transport,
                @NonNull DataProfile dataProfile) {}

        /**
         * Called when any data network existing status changed.
         *
         * @param anyDataExisting {@code true} indicating there is at least one data network
         * existing regardless of its state. {@code false} indicating all data networks are
         * disconnected.
         */
        public void onAnyDataNetworkExistingChanged(boolean anyDataExisting) {}

        /**
         * Called when {@link SubscriptionPlan}s change or an unmetered or congested subscription
         * override is set.
         */
        public void onSubscriptionPlanOverride() {}

        /**
         * Called when the physical link status changed.
         *
         * @param status The latest link status.
         */
        public void onPhysicalLinkStatusChanged(@LinkStatus int status) {}

        /**
         * Called when NR advanced capable by PCO changed.
         *
         * @param nrAdvancedCapable {@code true} if at least one of the data network is NR advanced
         * capable.
         */
        public void onNrAdvancedCapableByPcoChanged(boolean nrAdvancedCapable) {}

        /**
         * Called when data service is bound.
         *
         * @param transport The transport of the data service.
         */
        public void onDataServiceBound(@TransportType int transport) {}

        /**
         * Called when SIM load state changed.
         *
         * @param simState The current SIM state
         */
        public void onSimStateChanged(@SimState int simState) {}

        /**
         * Called when QosBearerSessions changed.
         *
         * @param qosBearerSessions The latest QOS bearer sessions.
         */
        public void onQosSessionsChanged(@NonNull List<QosBearerSession> qosBearerSessions) {}
    }

    /**
     * This class represent a rule allowing or disallowing handover between IWLAN and cellular
     * networks.
     *
     * @see CarrierConfigManager#KEY_IWLAN_HANDOVER_POLICY_STRING_ARRAY
     */
    public static class HandoverRule {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(prefix = {"RULE_TYPE_"},
                value = {
                        RULE_TYPE_ALLOWED,
                        RULE_TYPE_DISALLOWED,
                })
        public @interface HandoverRuleType {}

        /** Indicating this rule is for allowing handover. */
        public static final int RULE_TYPE_ALLOWED = 1;

        /** Indicating this rule is for disallowing handover. */
        public static final int RULE_TYPE_DISALLOWED = 2;

        private static final String RULE_TAG_SOURCE_ACCESS_NETWORKS = "source";

        private static final String RULE_TAG_TARGET_ACCESS_NETWORKS = "target";

        private static final String RULE_TAG_TYPE = "type";

        private static final String RULE_TAG_CAPABILITIES = "capabilities";

        private static final String RULE_TAG_ROAMING = "roaming";

        private static final String RULE_TAG_INCALL = "incall";

        /** Handover rule type. */
        @HandoverRuleType
        public final int type;

        /** The applicable source access networks for handover. */
        @NonNull
        @RadioAccessNetworkType
        public final Set<Integer> sourceAccessNetworks;

        /** The applicable target access networks for handover. */
        @NonNull
        @RadioAccessNetworkType
        public final Set<Integer> targetAccessNetworks;

        /**
         * The network capabilities to any of which this handover rule applies.
         * If is empty, then capability is ignored as a rule matcher.
         */
        @NonNull
        @NetCapability
        public final Set<Integer> networkCapabilities;

        /** {@code true} indicates this policy is only applicable when the device is roaming. */
        public final boolean isOnlyForRoaming;

        /** {@code true} indicates this policy is only applicable when the device is incall. */
        public final boolean isOnlyForIncall;

        /**
         * Constructor
         *
         * @param ruleString The rule in string format.
         *
         * @see CarrierConfigManager#KEY_IWLAN_HANDOVER_POLICY_STRING_ARRAY
         */
        public HandoverRule(@NonNull String ruleString, @NonNull FeatureFlags featureFlags) {
            if (TextUtils.isEmpty(ruleString)) {
                throw new IllegalArgumentException("illegal rule " + ruleString);
            }

            Set<Integer> source = null, target = null, capabilities = Collections.emptySet();
            int type = 0;
            boolean roaming = false;
            boolean incall = false;

            ruleString = ruleString.trim().toLowerCase(Locale.ROOT);
            String[] expressions = ruleString.split("\\s*,\\s*");
            for (String expression : expressions) {
                String[] tokens = expression.trim().split("\\s*=\\s*");
                if (tokens.length != 2) {
                    throw new IllegalArgumentException("illegal rule " + ruleString + ", tokens="
                            + Arrays.toString(tokens));
                }
                String key = tokens[0].trim();
                String value = tokens[1].trim();
                try {
                    switch (key) {
                        case RULE_TAG_SOURCE_ACCESS_NETWORKS:
                            source = Arrays.stream(value.split("\\s*\\|\\s*"))
                                    .map(String::trim)
                                    .map(AccessNetworkType::fromString)
                                    .collect(Collectors.toSet());
                            break;
                        case RULE_TAG_TARGET_ACCESS_NETWORKS:
                            target = Arrays.stream(value.split("\\s*\\|\\s*"))
                                    .map(String::trim)
                                    .map(AccessNetworkType::fromString)
                                    .collect(Collectors.toSet());
                            break;
                        case RULE_TAG_TYPE:
                            if (value.toLowerCase(Locale.ROOT).equals("allowed")) {
                                type = RULE_TYPE_ALLOWED;
                            } else if (value.toLowerCase(Locale.ROOT).equals("disallowed")) {
                                type = RULE_TYPE_DISALLOWED;
                            } else {
                                throw new IllegalArgumentException("unexpected rule type " + value);
                            }
                            break;
                        case RULE_TAG_CAPABILITIES:
                            capabilities = DataUtils.getNetworkCapabilitiesFromString(value);
                            break;
                        case RULE_TAG_ROAMING:
                            roaming = Boolean.parseBoolean(value);
                            break;
                        case RULE_TAG_INCALL:
                            if (featureFlags.incallHandoverPolicy()) {
                                incall = Boolean.parseBoolean(value);
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("unexpected key " + key);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("illegal rule \"" + ruleString + "\", e="
                            + e);
                }
            }

            if (source == null || target == null || source.isEmpty() || target.isEmpty()) {
                throw new IllegalArgumentException("Need to specify both source and target. "
                        + "\"" + ruleString + "\"");
            }

            if (source.contains(AccessNetworkType.UNKNOWN) && type != RULE_TYPE_DISALLOWED) {
                throw new IllegalArgumentException("Unknown access network can be only specified in"
                        + " the disallowed rule. \"" + ruleString + "\"");
            }

            if (target.contains(AccessNetworkType.UNKNOWN)) {
                throw new IllegalArgumentException("Target access networks contains unknown. "
                        + "\"" + ruleString + "\"");
            }

            if (type == 0) {
                throw new IllegalArgumentException("Rule type is not specified correctly. "
                        + "\"" + ruleString + "\"");
            }

            if (capabilities != null && capabilities.contains(-1)) {
                throw new IllegalArgumentException("Network capabilities contains unknown. "
                            + "\"" + ruleString + "\"");
            }

            if (!source.contains(AccessNetworkType.IWLAN)
                    && !target.contains(AccessNetworkType.IWLAN)) {
                throw new IllegalArgumentException("IWLAN must be specified in either source or "
                        + "target access networks.\"" + ruleString + "\"");
            }

            sourceAccessNetworks = source;
            targetAccessNetworks = target;
            this.type = type;
            networkCapabilities = capabilities;
            isOnlyForRoaming = roaming;
            isOnlyForIncall = incall;
        }

        @Override
        public String toString() {
            return "[HandoverRule: type=" + (type == RULE_TYPE_ALLOWED ? "allowed"
                    : "disallowed") + ", source=" + sourceAccessNetworks.stream()
                    .map(AccessNetworkType::toString).collect(Collectors.joining("|"))
                    + ", target=" + targetAccessNetworks.stream().map(AccessNetworkType::toString)
                    .collect(Collectors.joining("|")) + ", isRoaming=" + isOnlyForRoaming
                    + ", isIncall=" + isOnlyForIncall + ", capabilities="
                    + DataUtils.networkCapabilitiesToString(networkCapabilities) + "]";
        }
    }

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param looper The looper to be used by the handler. Currently the handler thread is the
     * phone process's main thread.
     * @param featureFlags The feature flag.
     */
    public DataNetworkController(@NonNull Phone phone, @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags) {
        super(looper);
        mPhone = phone;
        mFeatureFlags = featureFlags;
        mLogTag = "DNC-" + mPhone.getPhoneId();
        log("DataNetworkController created.");

        mAccessNetworksManager = phone.getAccessNetworksManager();
        for (int transport : mAccessNetworksManager.getAvailableTransports()) {
            mDataServiceManagers.put(transport, new DataServiceManager(mPhone, looper, transport));
        }

        mDataConfigManager = new DataConfigManager(mPhone, looper, featureFlags);

        // ========== Anomaly counters ==========
        mImsThrottleCounter = new SlidingWindowEventCounter(
                mDataConfigManager.getAnomalyImsReleaseRequestThreshold().timeWindow,
                mDataConfigManager.getAnomalyImsReleaseRequestThreshold().eventNumOccurrence);
        mNetworkUnwantedCounter = new SlidingWindowEventCounter(
                mDataConfigManager.getAnomalyNetworkUnwantedThreshold().timeWindow,
                mDataConfigManager.getAnomalyNetworkUnwantedThreshold().eventNumOccurrence);
        mSetupDataCallWlanFailureCounter = new SlidingWindowEventCounter(
                mDataConfigManager.getAnomalySetupDataCallThreshold().timeWindow,
                mDataConfigManager.getAnomalySetupDataCallThreshold().eventNumOccurrence);
        mSetupDataCallWwanFailureCounter = new SlidingWindowEventCounter(
                mDataConfigManager.getAnomalySetupDataCallThreshold().timeWindow,
                mDataConfigManager.getAnomalySetupDataCallThreshold().eventNumOccurrence);
        // ========================================

        mDataSettingsManager = TelephonyComponentFactory.getInstance().inject(
                DataSettingsManager.class.getName())
                .makeDataSettingsManager(mPhone, this, mFeatureFlags, looper,
                        new DataSettingsManagerCallback(this::post) {
                            @Override
                            public void onDataEnabledChanged(boolean enabled,
                                    @TelephonyManager.DataEnabledChangedReason int reason,
                                    @NonNull String callingPackage) {
                                // If mobile data is enabled by the user, evaluate the unsatisfied
                                // network requests and then attempt to setup data networks to
                                // satisfy them. If mobile data is disabled, evaluate the existing
                                // data networks and see if they need to be torn down.
                                logl("onDataEnabledChanged: enabled=" + enabled);
                                sendMessage(obtainMessage(enabled
                                                ? EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS
                                                : EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                                        DataEvaluationReason.DATA_ENABLED_CHANGED));
                            }
                            @Override
                            public void onDataEnabledOverrideChanged(boolean enabled,
                                    @TelephonyManager.MobileDataPolicy int policy) {
                                // If data enabled override is enabled by the user, evaluate the
                                // unsatisfied network requests and then attempt to setup data
                                // networks to satisfy them. If data enabled override is disabled,
                                // evaluate the existing data networks and see if they need to be
                                // torn down.
                                logl("onDataEnabledOverrideChanged: enabled=" + enabled);
                                sendMessage(obtainMessage(enabled
                                                ? EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS
                                                : EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                                        DataEvaluationReason.DATA_ENABLED_OVERRIDE_CHANGED));
                            }
                            @Override
                            public void onDataRoamingEnabledChanged(boolean enabled) {
                                // If data roaming is enabled by the user, evaluate the unsatisfied
                                // network requests and then attempt to setup data networks to
                                // satisfy them. If data roaming is disabled, evaluate the existing
                                // data networks and see if they need to be torn down.
                                logl("onDataRoamingEnabledChanged: enabled=" + enabled);
                                sendMessage(obtainMessage(enabled
                                                ? EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS
                                                : EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                                        DataEvaluationReason.ROAMING_ENABLED_CHANGED));
                            }
                        });
        mDataProfileManager = TelephonyComponentFactory.getInstance().inject(
                DataProfileManager.class.getName())
                .makeDataProfileManager(mPhone, this, mDataServiceManagers
                                .get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN), looper,
                        mFeatureFlags,
                        new DataProfileManagerCallback(this::post) {
                            @Override
                            public void onDataProfilesChanged() {
                                sendMessage(
                                        obtainMessage(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                                        DataEvaluationReason.DATA_PROFILES_CHANGED));
                                sendMessage(
                                        obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                                        DataEvaluationReason.DATA_PROFILES_CHANGED));
                            }
                        });
        mDataStallRecoveryManager = new DataStallRecoveryManager(mPhone, this, mDataServiceManagers
                .get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN), mFeatureFlags, looper,
                new DataStallRecoveryManagerCallback(this::post) {
                    @Override
                    public void onDataStallReestablishInternet() {
                        DataNetworkController.this.onDataStallReestablishInternet();
                    }
                });
        mDataRetryManager = new DataRetryManager(mPhone, this,
                mDataServiceManagers, looper, mFeatureFlags,
                new DataRetryManagerCallback(this::post) {
                    @Override
                    public void onDataNetworkSetupRetry(
                            @NonNull DataSetupRetryEntry dataSetupRetryEntry) {
                        Objects.requireNonNull(dataSetupRetryEntry);
                        DataNetworkController.this.onDataNetworkSetupRetry(dataSetupRetryEntry);
                    }
                    @Override
                    public void onDataNetworkHandoverRetry(
                            @NonNull DataHandoverRetryEntry dataHandoverRetryEntry) {
                        Objects.requireNonNull(dataHandoverRetryEntry);
                        DataNetworkController.this
                                .onDataNetworkHandoverRetry(dataHandoverRetryEntry);
                    }
                    @Override
                    public void onDataNetworkHandoverRetryStopped(
                            @NonNull DataNetwork dataNetwork) {
                        Objects.requireNonNull(dataNetwork);
                        DataNetworkController.this.onDataNetworkHandoverRetryStopped(dataNetwork);
                    }
                });
        mImsManager = mPhone.getContext().getSystemService(ImsManager.class);
        mNetworkPolicyManager = mPhone.getContext().getSystemService(NetworkPolicyManager.class);
        mTelecomManager = mPhone.getContext().getSystemService(TelecomManager.class);

        // Use the raw one from ServiceStateTracker instead of the combined one from
        // mPhone.getServiceState().
        mServiceState = mPhone.getServiceStateTracker().getServiceState();

        // Instead of calling onRegisterAllEvents directly from the constructor, send the event.
        // The reason is that getImsPhone is null when we are still in the constructor here.
        sendEmptyMessage(EVENT_REGISTER_ALL_EVENTS);
    }

    /**
     * Called when needed to register for all events that data network controller is interested.
     */
    private void onRegisterAllEvents() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);
        filter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        mPhone.getContext().registerReceiver(mIntentReceiver, filter, null, mPhone);

        mAccessNetworksManager.registerCallback(new AccessNetworksManagerCallback(this::post) {
            @Override
            public void onPreferredTransportChanged(
                    @NetCapability int capability, boolean forceReconnect) {
                int preferredTransport = mAccessNetworksManager
                        .getPreferredTransportByNetworkCapability(capability);
                logl("onPreferredTransportChanged: "
                        + DataUtils.networkCapabilityToString(capability) + " preferred on "
                        + AccessNetworkConstants.transportTypeToString(preferredTransport)
                        + (forceReconnect ? "forceReconnect:true" : ""));

                DataNetworkController.this.onEvaluatePreferredTransport(capability, forceReconnect);
                if (!hasMessages(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS)) {
                    sendMessage(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                            DataEvaluationReason.PREFERRED_TRANSPORT_CHANGED));
                } else {
                    log("onPreferredTransportChanged: Skipped evaluating unsatisfied network "
                            + "requests because another evaluation was already scheduled.");
                }
            }
        });

        mNetworkPolicyManager.registerSubscriptionCallback(new SubscriptionCallback() {
            @Override
            public void onSubscriptionPlansChanged(int subId, SubscriptionPlan[] plans) {
                if (mSubId != subId) return;
                obtainMessage(EVENT_SUBSCRIPTION_PLANS_CHANGED, plans).sendToTarget();
            }

            @Override
            public void onSubscriptionOverride(int subId, int overrideMask, int overrideValue,
                    int[] networkTypes) {
                if (mSubId != subId) return;
                obtainMessage(EVENT_SUBSCRIPTION_OVERRIDE, overrideMask, overrideValue,
                        networkTypes).sendToTarget();
            }
        });

        mPhone.getServiceStateTracker().registerForServiceStateChanged(this,
                EVENT_SERVICE_STATE_CHANGED, null);
        mDataConfigManager.registerCallback(new DataConfigManagerCallback(this::post) {
            @Override
            public void onCarrierConfigChanged() {
                DataNetworkController.this.onCarrierConfigUpdated();
            }
            @Override
            public void onDeviceConfigChanged() {
                DataNetworkController.this.onDeviceConfigUpdated();
            }
        });
        mPhone.getServiceStateTracker().registerForPsRestrictedEnabled(this,
                EVENT_PS_RESTRICT_ENABLED, null);
        mPhone.getServiceStateTracker().registerForPsRestrictedDisabled(this,
                EVENT_PS_RESTRICT_DISABLED, null);
        mPhone.getServiceStateTracker().registerForAreaCodeChanged(this, EVENT_TAC_CHANGED, null);
        mPhone.registerForEmergencyCallToggle(this, EVENT_EMERGENCY_CALL_CHANGED, null);
        mDataServiceManagers.get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                .registerForServiceBindingChanged(this, EVENT_DATA_SERVICE_BINDING_CHANGED);

        mPhone.getServiceStateTracker().registerForServiceStateChanged(this,
                EVENT_SERVICE_STATE_CHANGED, null);
        mDataServiceManagers.get(AccessNetworkConstants.TRANSPORT_TYPE_WLAN)
                .registerForServiceBindingChanged(this, EVENT_DATA_SERVICE_BINDING_CHANGED);

        mPhone.getContext().getSystemService(TelephonyRegistryManager.class)
                .addOnSubscriptionsChangedListener(new OnSubscriptionsChangedListener() {
                    @Override
                    public void onSubscriptionsChanged() {
                        sendEmptyMessage(EVENT_SUBSCRIPTION_CHANGED);
                    }
                }, this::post);

        if (hasCalling()) {
            // Register for call ended event for voice/data concurrent not supported case. It is
            // intended to only listen for events from the same phone as most of the telephony
            // modules are designed as per-SIM basis. For DSDS call ended on non-DDS sub, the
            // frameworks relies on service state on DDS sub change from out-of-service to
            // in-service to trigger data retry.
            mPhone.getCallTracker().registerForVoiceCallEnded(this, EVENT_VOICE_CALL_ENDED, null);
            // Check null for devices not supporting FEATURE_TELEPHONY_IMS.
            if (mPhone.getImsPhone() != null) {
                mPhone.getImsPhone().getCallTracker().registerForVoiceCallEnded(
                        this, EVENT_VOICE_CALL_ENDED, null);
            }
        }
        mPhone.mCi.registerForSlicingConfigChanged(this, EVENT_SLICE_CONFIG_CHANGED, null);
        mPhone.mCi.registerForSrvccStateChanged(this, EVENT_SRVCC_STATE_CHANGED, null);

        mPhone.getLinkBandwidthEstimator().registerCallback(
                new LinkBandwidthEstimatorCallback(this::post) {
                    @Override
                    public void onDataActivityChanged(@DataActivityType int dataActivity) {
                        DataNetworkController.this.updateDataActivity();
                    }
                }
        );
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        AsyncResult ar;
        switch (msg.what) {
            case EVENT_REGISTER_ALL_EVENTS:
                onRegisterAllEvents();
                break;
            case EVENT_ADD_NETWORK_REQUEST:
                onAddNetworkRequest((TelephonyNetworkRequest) msg.obj);
                break;
            case EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS:
                DataEvaluationReason reason = (DataEvaluationReason) msg.obj;
                onReevaluateUnsatisfiedNetworkRequests(reason);
                break;
            case EVENT_REEVALUATE_EXISTING_DATA_NETWORKS:
                reason = (DataEvaluationReason) msg.obj;
                onReevaluateExistingDataNetworks(reason);
                break;
            case EVENT_REMOVE_NETWORK_REQUEST:
                onRemoveNetworkRequest((TelephonyNetworkRequest) msg.obj);
                break;
            case EVENT_VOICE_CALL_ENDED:
                // In some cases we need to tear down network after call ends. For example, when
                // delay IMS tear down until call ends is turned on.
                sendMessage(obtainMessage(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                        DataEvaluationReason.VOICE_CALL_ENDED));
                // Delay evaluating unsatisfied network requests. In temporary DDS switch case, it
                // takes some time to switch DDS after call end. We do not want to bring up network
                // before switch completes.
                sendMessageDelayed(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                        DataEvaluationReason.VOICE_CALL_ENDED),
                        REEVALUATE_UNSATISFIED_NETWORK_REQUESTS_AFTER_CALL_END_DELAY_MILLIS);
                break;
            case EVENT_SLICE_CONFIG_CHANGED:
                sendMessage(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                        DataEvaluationReason.SLICE_CONFIG_CHANGED));
                break;
            case EVENT_SRVCC_STATE_CHANGED:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    onSrvccStateChanged((int[]) ar.result);
                }
                break;
            case EVENT_PS_RESTRICT_ENABLED:
                mPsRestricted = true;
                break;
            case EVENT_PS_RESTRICT_DISABLED:
                mPsRestricted = false;
                sendMessage(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                        DataEvaluationReason.DATA_RESTRICTED_CHANGED));
                break;
            case EVENT_TAC_CHANGED:
                // Re-evaluate unsatisfied network requests with some delays to let DataRetryManager
                // clears the throttling record.
                sendMessageDelayed(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                        DataEvaluationReason.TAC_CHANGED),
                        REEVALUATE_UNSATISFIED_NETWORK_REQUESTS_TAC_CHANGED_DELAY_MILLIS);
                break;
            case EVENT_DATA_SERVICE_BINDING_CHANGED:
                ar = (AsyncResult) msg.obj;
                int transport = (int) ar.userObj;
                boolean bound = (boolean) ar.result;
                onDataServiceBindingChanged(transport, bound);
                break;
            case EVENT_SIM_STATE_CHANGED:
                int simState = msg.arg1;
                onSimStateChanged(simState);
                break;
            case EVENT_TEAR_DOWN_ALL_DATA_NETWORKS:
                onTearDownAllDataNetworks(msg.arg1);
                break;
            case EVENT_REGISTER_DATA_NETWORK_CONTROLLER_CALLBACK:
                DataNetworkControllerCallback callback = (DataNetworkControllerCallback) msg.obj;
                mDataNetworkControllerCallbacks.add(callback);
                // Notify upon registering if no data networks currently exist.
                if (mDataNetworkList.isEmpty()) {
                    callback.invokeFromExecutor(
                            () -> callback.onAnyDataNetworkExistingChanged(false));
                }
                break;
            case EVENT_UNREGISTER_DATA_NETWORK_CONTROLLER_CALLBACK:
                mDataNetworkControllerCallbacks.remove((DataNetworkControllerCallback) msg.obj);
                break;
            case EVENT_SUBSCRIPTION_CHANGED:
                onSubscriptionChanged();
                break;
            case EVENT_SERVICE_STATE_CHANGED:
                onServiceStateChanged();
                break;
            case EVENT_EMERGENCY_CALL_CHANGED:
                if (mPhone.isInEcm()) {
                    sendMessage(obtainMessage(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                            DataEvaluationReason.EMERGENCY_CALL_CHANGED));
                } else {
                    sendMessage(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                            DataEvaluationReason.EMERGENCY_CALL_CHANGED));
                }
                break;
            case EVENT_EVALUATE_PREFERRED_TRANSPORT:
                onEvaluatePreferredTransport(msg.arg1, msg.arg2 != 0 /* forceReconnect */);
                break;
            case EVENT_SUBSCRIPTION_PLANS_CHANGED:
                SubscriptionPlan[] plans = (SubscriptionPlan[]) msg.obj;
                log("Subscription plans changed: " + Arrays.toString(plans));
                mSubscriptionPlans.clear();
                mSubscriptionPlans.addAll(Arrays.asList(plans));
                mDataNetworkControllerCallbacks.forEach(cb -> cb.invokeFromExecutor(
                        cb::onSubscriptionPlanOverride));
                break;
            case EVENT_SUBSCRIPTION_OVERRIDE:
                int overrideMask = msg.arg1;
                boolean override = msg.arg2 != 0;
                int[] networkTypes = (int[]) msg.obj;

                if (overrideMask == NetworkPolicyManager.SUBSCRIPTION_OVERRIDE_UNMETERED) {
                    log("Unmetered subscription override: override=" + override
                            + ", networkTypes=" + Arrays.stream(networkTypes)
                            .mapToObj(TelephonyManager::getNetworkTypeName)
                            .collect(Collectors.joining(",")));
                    for (int networkType : networkTypes) {
                        if (override) {
                            mUnmeteredOverrideNetworkTypes.add(networkType);
                        } else {
                            mUnmeteredOverrideNetworkTypes.remove(networkType);
                        }
                    }
                    mDataNetworkControllerCallbacks.forEach(cb -> cb.invokeFromExecutor(
                            cb::onSubscriptionPlanOverride));
                } else if (overrideMask == NetworkPolicyManager.SUBSCRIPTION_OVERRIDE_CONGESTED) {
                    log("Congested subscription override: override=" + override
                            + ", networkTypes=" + Arrays.stream(networkTypes)
                            .mapToObj(TelephonyManager::getNetworkTypeName)
                            .collect(Collectors.joining(",")));
                    for (int networkType : networkTypes) {
                        if (override) {
                            mCongestedOverrideNetworkTypes.add(networkType);
                        } else {
                            mCongestedOverrideNetworkTypes.remove(networkType);
                        }
                    }
                    mDataNetworkControllerCallbacks.forEach(cb -> cb.invokeFromExecutor(
                            cb::onSubscriptionPlanOverride));
                } else {
                    loge("Unknown override mask: " + overrideMask);
                }
                break;
            default:
                loge("Unexpected event " + msg.what);
        }
    }

    /**
     * Add a network request, which is originated from the apps. Note that add a network request
     * is not necessarily setting up a {@link DataNetwork}.
     *
     * @param networkRequest Network request
     *
     */
    public void addNetworkRequest(@NonNull TelephonyNetworkRequest networkRequest) {
        sendMessage(obtainMessage(EVENT_ADD_NETWORK_REQUEST, networkRequest));
    }

    /**
     * Called when a network request arrives data network controller.
     *
     * @param networkRequest The network request.
     */
    private void onAddNetworkRequest(@NonNull TelephonyNetworkRequest networkRequest) {
        // TelephonyNetworkRequest at TelephonyNetworkProvider layer does not have config assigned
        // (Because TelephonyNetworkProvider is a singleton across all SIMs. We are not able to
        // retrieve the right carrier config for it.). So as soon as the request arrives
        // DataNetworkController, we need to update the config in the request so it can update
        // some of its config-dependent properties like request priority.
        networkRequest.updateDataConfig(mDataConfigManager);

        // To detect IMS back-to-back release-request anomaly event
        if (mLastImsOperationIsRelease) {
            mLastImsOperationIsRelease = false;
            if (Arrays.equals(
                    mLastReleasedImsRequestCapabilities, networkRequest.getCapabilities())
                    && mImsThrottleCounter.addOccurrence()) {
                reportAnomaly(networkRequest.getNativeNetworkRequest().getRequestorPackageName()
                                + " requested with same capabilities "
                                + mImsThrottleCounter.getFrequencyString(),
                        "ead6f8db-d2f2-4ed3-8da5-1d8560fe7daf");
            }
        }
        if (!mAllNetworkRequestList.add(networkRequest)) {
            loge("onAddNetworkRequest: Duplicate network request. " + networkRequest);
            return;
        }
        log("onAddNetworkRequest: added " + networkRequest);
        onSatisfyNetworkRequest(networkRequest);
    }

    /**
     * Called when attempting to satisfy a network request. If after evaluation, the network
     * request is determined that can be satisfied, the data network controller will establish
     * the data network. If the network request can't be satisfied, it will remain in the
     * unsatisfied pool until the environment changes.
     *
     * @param networkRequest The network request to be satisfied.
     */
    private void onSatisfyNetworkRequest(@NonNull TelephonyNetworkRequest networkRequest) {
        if (networkRequest.getState() == TelephonyNetworkRequest.REQUEST_STATE_SATISFIED) {
            logv("Already satisfied. " + networkRequest);
            return;
        }

        // Check if there is any existing data network that can satisfy the network request, and
        // attempt to attach if possible.
        if (findCompatibleDataNetworkAndAttach(networkRequest)) {
            return;
        }

        // If no data network can satisfy the requests, then start the evaluation process. Since
        // all the requests in the list have the same capabilities, we can only evaluate one
        // of them.
        DataEvaluation evaluation = evaluateNetworkRequest(networkRequest,
                DataEvaluationReason.NEW_REQUEST);
        if (!evaluation.containsDisallowedReasons()) {
            DataProfile dataProfile = evaluation.getCandidateDataProfile();
            if (dataProfile != null) {
                setupDataNetwork(dataProfile, null,
                        evaluation.getDataAllowedReason());
            }
        } else if (evaluation.contains(DataDisallowedReason.ONLY_ALLOWED_SINGLE_NETWORK)) {
            // Re-evaluate the existing data networks. If this request's priority is higher than
            // the existing data network, the data network will be torn down so this request will
            // get a chance to be satisfied.
            sendMessage(obtainMessage(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                    DataEvaluationReason.SINGLE_DATA_NETWORK_ARBITRATION));
        }
    }

    /**
     * Attempt to attach a network request to an existing data network that can satisfy the
     * network request.
     *
     * @param networkRequest The network request to attach.
     *
     * @return {@code false} if can't find the data network to to satisfy the network request.
     * {@code true} if the network request has been scheduled to attach to the data network.
     * If attach succeeds, the network request's state will be set to
     * {@link TelephonyNetworkRequest#REQUEST_STATE_SATISFIED}. If failed,
     * {@link #onAttachNetworkRequestsFailed(DataNetwork, NetworkRequestList)} will be invoked.
     */
    private boolean findCompatibleDataNetworkAndAttach(
            @NonNull TelephonyNetworkRequest networkRequest) {
        return findCompatibleDataNetworkAndAttach(new NetworkRequestList(networkRequest));
    }

    /**
     * Attempt to attach a network request list to an existing data network that can satisfy all the
     * network requests. Note this method does not support partial attach (i.e. Only attach some
     * of the satisfiable requests to the network). All requests must be satisfied so they can be
     * attached.
     *
     * @param requestList The network request list to attach. It is expected that every network
     * request in this list has the same network capabilities.
     *
     * @return {@code false} if can't find the data network to to satisfy the network requests, even
     * if only one of network request can't be satisfied. {@code true} if the network request
     * has been scheduled to attach to the data network. If attach succeeds, the network request's
     * state will be set to
     * {@link TelephonyNetworkRequest#REQUEST_STATE_SATISFIED}. If failed,
     * {@link #onAttachNetworkRequestsFailed(DataNetwork, NetworkRequestList)} will be invoked.
     */
    private boolean findCompatibleDataNetworkAndAttach(@NonNull NetworkRequestList requestList) {
        if (requestList.isEmpty()) return false;
        // Try to find a data network that can satisfy all the network requests.
        for (DataNetwork dataNetwork : mDataNetworkList) {
            TelephonyNetworkRequest networkRequest = requestList.stream()
                    .filter(request -> !request.canBeSatisfiedBy(
                            dataNetwork.getNetworkCapabilities()))
                    .findAny()
                    .orElse(null);
            // If found any request that can't be satisfied by this data network, continue to try
            // next data network. We must find a data network that can satisfy all the provided
            // network requests.
            if (networkRequest != null) {
                continue;
            }

            // When reaching here, it means this data network can satisfy all the network requests.
            logv("Found a compatible data network " + dataNetwork + ". Attaching "
                    + requestList);
            return dataNetwork.attachNetworkRequests(requestList);
        }
        return false;
    }

    /**
     * @param ss The service state to be checked
     * @param transport The transport is used to determine the data registration state
     *
     * @return {@code true} if data is in service or if voice is in service on legacy CS
     * connections (2G/3G) on the non-DDS. In those cases we attempt to attach PS. We don't try for
     * newer RAT because for those PS attach already occurred.
     */
    private boolean serviceStateAllowsPSAttach(@NonNull ServiceState ss,
            @TransportType int transport) {
        // Use the data registration state from the modem instead of the current data registration
        // state, which can be overridden.
        int nriRegState = getDataRegistrationState(ss, transport);
        if (nriRegState == NetworkRegistrationInfo.REGISTRATION_STATE_HOME
                || nriRegState == NetworkRegistrationInfo.REGISTRATION_STATE_ROAMING) return true;

        // If data is OOS as this device slot is not modem preferred(i.e. not active for internet),
        // attempt to attach PS on 2G/3G if CS connection is available.
        return ss.getVoiceRegState() == ServiceState.STATE_IN_SERVICE
                && mPhone.getPhoneId() != PhoneSwitcher.getInstance().getPreferredDataPhoneId()
                && isLegacyCs(ss.getVoiceNetworkType());
    }

    /**
     * @param voiceNetworkType The voice network type to be checked.
     * @return {@code true} if the network type is on legacy CS connection.
     */
    private boolean isLegacyCs(@NetworkType int voiceNetworkType) {
        int voiceAccessNetworkType = DataUtils.networkTypeToAccessNetworkType(voiceNetworkType);
        return voiceAccessNetworkType == AccessNetworkType.GERAN
                || voiceAccessNetworkType == AccessNetworkType.UTRAN
                || voiceAccessNetworkType == AccessNetworkType.CDMA2000;
    }

    /**
     * @return {@code true} if the network only allows single data network at one time.
     */
    private boolean isOnlySingleDataNetworkAllowed(@TransportType int transport) {
        if (transport == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) return false;

        return mDataConfigManager.getNetworkTypesOnlySupportSingleDataNetwork()
                .contains(getDataNetworkType(transport));
    }

    /**
     * @param capabilities The Network Capabilities to be checked.
     * @return {@code true} if the capabilities contain any capability that's exempt from the single
     * PDN rule.
     */
    private boolean hasCapabilityExemptsFromSinglePdnRule(@NetCapability int[] capabilities) {
        Set<Integer> exemptCapabilities =
                mDataConfigManager.getCapabilitiesExemptFromSingleDataNetwork();
        return Arrays.stream(capabilities).anyMatch(exemptCapabilities::contains);
    }

    /**
     * Evaluate if telephony frameworks would allow data setup for internet in current environment.
     *
     * @param ignoreExistingNetworks {@code true} to skip the existing network check.
     * @return {@code true} if the environment is allowed for internet data. {@code false} if not
     * allowed. For example, if SIM is absent, or airplane mode is on, then data is NOT allowed.
     * This API does not reflect the currently internet data network status. It's possible there is
     * no internet data due to weak cellular signal or network side issue, but internet data is
     * still allowed in this case.
     */
    public boolean isInternetDataAllowed(boolean ignoreExistingNetworks) {
        return !getInternetEvaluation(ignoreExistingNetworks).containsDisallowedReasons();
    }

    /**
     * @param ignoreExistingNetworks {@code true} to skip the existing network check.
     * @return The internet evaluation result.
     * For example, if SIM is absent, or airplane mode is on, then data is NOT allowed.
     * This API does not reflect the currently internet data network status. It's possible there is
     * no internet data due to weak cellular signal or network side issue, but internet data is
     * still allowed in this case.
     */
    @NonNull
    public DataEvaluation getInternetEvaluation(boolean ignoreExistingNetworks) {
        TelephonyNetworkRequest internetRequest = new TelephonyNetworkRequest(
                new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(), mPhone, mFeatureFlags);
        // If we don't skip checking existing network, then we should check If one of the
        // existing networks can satisfy the internet request, then internet is allowed.
        if (!ignoreExistingNetworks && mDataNetworkList.stream().anyMatch(
                dataNetwork -> internetRequest.canBeSatisfiedBy(
                        dataNetwork.getNetworkCapabilities()))) {
            return new DataEvaluation(DataEvaluationReason.EXTERNAL_QUERY);
        }

        // If no existing network can satisfy the request, then check if we can possibly setup
        // the internet network.

        DataEvaluation evaluation = evaluateNetworkRequest(internetRequest,
                DataEvaluationReason.EXTERNAL_QUERY);
        if (evaluation.containsOnly(DataDisallowedReason.ONLY_ALLOWED_SINGLE_NETWORK)
                && internetRequest.getPriority() > mDataNetworkList.stream()
                .map(DataNetwork::getPriority)
                .max(Comparator.comparing(Integer::valueOf))
                .orElse(0)) {
            // If the only failed reason is only single network allowed, then check if the request
            // can trump the current network.
            evaluation.addDataAllowedReason(DataAllowedReason.NORMAL);
        }
        return evaluation;
    }


    /**
     * @return {@code true} if internet is unmetered.
     */
    public boolean isInternetUnmetered() {
        return mDataNetworkList.stream()
                .filter(dataNetwork -> !dataNetwork.isConnecting() && !dataNetwork.isDisconnected())
                .filter(DataNetwork::isInternetSupported)
                .allMatch(dataNetwork -> dataNetwork.getNetworkCapabilities()
                        .hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        || dataNetwork.getNetworkCapabilities()
                        .hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED));
    }

    /**
     * @return {@code true} if all data networks are disconnected.
     */
    public boolean areAllDataDisconnected() {
        if (!mDataNetworkList.isEmpty()) {
            log("areAllDataDisconnected false due to: " + mDataNetworkList.stream()
                    .map(DataNetwork::name).collect(Collectors.joining(", ")));
        }
        return mDataNetworkList.isEmpty();
    }

    /**
     * @return List of the reasons why internet data is not allowed. An empty list if internet
     * is allowed.
     */
    @NonNull
    public List<DataDisallowedReason> getInternetDataDisallowedReasons() {
        TelephonyNetworkRequest internetRequest = new TelephonyNetworkRequest(
                new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(), mPhone, mFeatureFlags);
        DataEvaluation evaluation = evaluateNetworkRequest(internetRequest,
                DataEvaluationReason.EXTERNAL_QUERY);
        return evaluation.getDataDisallowedReasons();
    }

    /**
     * Evaluate a network request. The goal is to find a suitable {@link DataProfile} that can be
     * used to setup the data network.
     *
     * @param networkRequest The network request to evaluate.
     * @param reason The reason for evaluation.
     * @return The data evaluation result.
     */
    @NonNull
    @VisibleForTesting
    public DataEvaluation evaluateNetworkRequest(
            @NonNull TelephonyNetworkRequest networkRequest, DataEvaluationReason reason) {
        DataEvaluation evaluation = new DataEvaluation(reason);
        int transport = mAccessNetworksManager.getPreferredTransportByNetworkCapability(
                networkRequest.getHighestPriorityApnTypeNetworkCapability());

        // Check if the request can be satisfied by cellular network or satellite network.
        if (!canConnectivityTransportSatisfyNetworkRequest(networkRequest, transport)) {
            evaluation.addDataDisallowedReason(
                    DataDisallowedReason.DATA_NETWORK_TRANSPORT_NOT_ALLOWED);
        }

        // Bypass all checks for emergency network request.
        if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_EIMS)) {
            DataProfile emergencyProfile = mDataProfileManager.getDataProfileForNetworkRequest(
                    networkRequest, getDataNetworkType(transport),
                    mServiceState.isUsingNonTerrestrialNetwork(),
                    false /*isEsimBootStrapProvisioning*/, true);

            // Check if the profile is being throttled.
            if (mDataConfigManager.shouldHonorRetryTimerForEmergencyNetworkRequest()
                    && emergencyProfile != null
                    && mDataRetryManager.isDataProfileThrottled(emergencyProfile, transport)) {
                evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_THROTTLED);
                log("Emergency network request is throttled by the previous setup data "
                            + "call response.");
            }

            if (!evaluation.containsDisallowedReasons()) {
                evaluation.addDataAllowedReason(DataAllowedReason.EMERGENCY_REQUEST);
                if (emergencyProfile != null) {
                    evaluation.setCandidateDataProfile(emergencyProfile);
                }
            }
            networkRequest.setEvaluation(evaluation);
            log(evaluation.toString());
            return evaluation;
        }

        if (!serviceStateAllowsPSAttach(mServiceState, transport)) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.NOT_IN_SERVICE);
        }

        // Check SIM state
        if (mSimState != TelephonyManager.SIM_STATE_LOADED) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.SIM_NOT_READY);
        }

        // Check if carrier specific config is loaded or not.
        if (!mDataConfigManager.isConfigCarrierSpecific()) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_CONFIG_NOT_READY);
        }

        if (mFeatureFlags.dataServiceCheck() && !isDataServiceSupported(transport)) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.SERVICE_OPTION_NOT_SUPPORTED);
        }

        // Check CS call state and see if concurrent voice/data is allowed.
        if (hasCalling() && mPhone.getCallTracker().getState() != PhoneConstants.State.IDLE
                && !mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
            evaluation.addDataDisallowedReason(
                    DataDisallowedReason.CONCURRENT_VOICE_DATA_NOT_ALLOWED);
        }

        // Check VoPS support
        if (transport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                && networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMTEL)) {
            NetworkRegistrationInfo nri = mServiceState.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (nri != null) {
                DataSpecificRegistrationInfo dsri = nri.getDataSpecificInfo();
                if (dsri != null && dsri.getVopsSupportInfo() != null
                        && !dsri.getVopsSupportInfo().isVopsSupported()
                        && !mDataConfigManager.allowBringUpNetworkInNonVops(
                                nri.getNetworkRegistrationState())) {
                    evaluation.addDataDisallowedReason(DataDisallowedReason.VOPS_NOT_SUPPORTED);
                }
            }
        }

        // Check if default data is selected.
        if (!SubscriptionManager.isValidSubscriptionId(
                SubscriptionManager.getDefaultDataSubscriptionId())) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.DEFAULT_DATA_UNSELECTED);
        }

        // Check if data roaming is disabled.
        // But if the data roaming setting for satellite connection is ignored as the satellite
        // data plan is included in the user mobile plan, then we should not disallow data due to
        // roaming disabled.
        if (mServiceState.getDataRoaming() && !mDataSettingsManager.isDataRoamingEnabled()
                    && !shouldIgnoreDataRoamingSettingForSatellite()) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.ROAMING_DISABLED);
        }

        // Check if data is restricted by the cellular network.
        if (mPsRestricted && transport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_RESTRICTED_BY_NETWORK);
        }

        // Check if there are pending tear down all networks request.
        if (mPhone.getServiceStateTracker().isPendingRadioPowerOffAfterDataOff()) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.PENDING_TEAR_DOWN_ALL);
        }

        // Check if the request is preferred on cellular and radio is/will be turned off.
        // We are using getDesiredPowerState() instead of isRadioOn() because we also don't want
        // to setup data network when radio power is about to be turned off.
        if (transport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                && (!mPhone.getServiceStateTracker().getDesiredPowerState()
                || mPhone.mCi.getRadioState() != TelephonyManager.RADIO_POWER_ON)) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.RADIO_POWER_OFF);
        }

        // Check if radio is/will be turned off by carrier.
        if (!mPhone.getServiceStateTracker().getPowerStateFromCarrier()) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.RADIO_DISABLED_BY_CARRIER);
        }

        // Check if the underlying data service is bound.
        if (!mDataServiceBound.get(transport)) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_SERVICE_NOT_READY);
        }

        // Check if device is in CDMA ECBM
        if (mPhone.isInCdmaEcm()) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.CDMA_EMERGENCY_CALLBACK_MODE);
        }

        // Check if only one data network is allowed.
        if (isOnlySingleDataNetworkAllowed(transport)
                && !hasCapabilityExemptsFromSinglePdnRule(networkRequest.getCapabilities())) {
            // if exists not-exempt network.
            if (mDataNetworkList.stream()
                    .anyMatch(dataNetwork -> !hasCapabilityExemptsFromSinglePdnRule(
                            dataNetwork.getNetworkCapabilities().getCapabilities()))) {
                evaluation.addDataDisallowedReason(
                        DataDisallowedReason.ONLY_ALLOWED_SINGLE_NETWORK);
            }
        }

        if (mDataSettingsManager.isDataInitialized()) {
            if (!mDataSettingsManager.isDataEnabled(DataUtils.networkCapabilityToApnType(
                    networkRequest.getHighestPriorityApnTypeNetworkCapability()))) {
                evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_DISABLED);
            }
        } else {
            evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_SETTINGS_NOT_READY);
        }

        // Check whether to allow data in certain situations if data is disallowed for soft reasons
        if (!evaluation.containsDisallowedReasons()) {
            evaluation.addDataAllowedReason(DataAllowedReason.NORMAL);

            if (!mDataSettingsManager.isDataEnabled()
                    && networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
                    && mDataSettingsManager.isMobileDataPolicyEnabled(TelephonyManager
                    .MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED)) {
                // We reach here when data is disabled, but MMS always-allowed is enabled.
                // (Note that isDataEnabled(ApnSetting.TYPE_MMS) returns true in this case, so it
                // would not generate any soft disallowed reason. We need to explicitly handle it.)
                evaluation.addDataAllowedReason(DataAllowedReason.MMS_REQUEST);
            }
        } else if (!evaluation.containsHardDisallowedReasons()) {
            if ((mTelecomManager.isInEmergencyCall() || mPhone.isInEcm())
                    && networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_SUPL)) {
                // Check if it's SUPL during emergency call.
                evaluation.addDataAllowedReason(DataAllowedReason.EMERGENCY_SUPL);
            } else if (!networkRequest.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    && isValidRestrictedRequest(networkRequest)) {
                // Check if request is restricted and not for tethering, which always comes with
                // a restricted network request.
                evaluation.addDataAllowedReason(DataAllowedReason.RESTRICTED_REQUEST);
            } else if (transport == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                // Check if request is unmetered (WiFi or unmetered APN).
                evaluation.addDataAllowedReason(DataAllowedReason.UNMETERED_USAGE);
            } else if (transport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                boolean isMeteredRequest = mDataConfigManager.isAnyMeteredCapability(
                        networkRequest.getCapabilities(), mServiceState.getDataRoaming());
                if (!isMeteredRequest) {
                    evaluation.addDataAllowedReason(DataAllowedReason.UNMETERED_USAGE);
                }
            }
        }

        // Check if there is any compatible data profile
        int networkType = getDataNetworkType(transport);
        if (networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN
                && transport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
            // reach here when data is OOS but serviceStateAllowsPSAttach == true, so we adopt the
            // voice RAT to select data profile
            networkType = mServiceState.getVoiceNetworkType();
        }
        DataProfile dataProfile = mDataProfileManager
                .getDataProfileForNetworkRequest(networkRequest, networkType,
                        mServiceState.isUsingNonTerrestrialNetwork(),
                        isEsimBootStrapProvisioningActivated(),
                        // If the evaluation is due to environmental changes, then we should ignore
                        // the permanent failure reached earlier.
                        reason.isConditionBased());
        if (dataProfile == null) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.NO_SUITABLE_DATA_PROFILE);
        } else if (// Check for new requests if we already self-scheduled(as opposed to modem
            // demanded) retry for similar requests.
                reason == DataEvaluationReason.NEW_REQUEST
                        &&  mDataRetryManager.isSimilarNetworkRequestRetryScheduled(
                        networkRequest, transport)) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.RETRY_SCHEDULED);
        } else if (mDataRetryManager.isDataProfileThrottled(dataProfile, transport)) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_THROTTLED);
        }

        if (!evaluation.containsDisallowedReasons()) {
            if (transport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                    && isEsimBootStrapProvisioningActivated()
                    && isEsimBootStrapMaxDataLimitReached()) {
                log("BootStrap Sim Data Usage limit reached");
                evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_LIMIT_REACHED);
            } else {
                evaluation.setCandidateDataProfile(dataProfile);
            }
        }

        networkRequest.setEvaluation(evaluation);
        // EXTERNAL_QUERY generates too many log spam.
        if (reason != DataEvaluationReason.EXTERNAL_QUERY) {
            log(evaluation + ", network type="
                    + TelephonyManager.getNetworkTypeName(getDataNetworkType(transport))
                    + ", reg state="
                    + NetworkRegistrationInfo.registrationStateToString(
                    getDataRegistrationState(mServiceState, transport))
                    + ", " + networkRequest);
        }
        return evaluation;
    }

    /**
     * Returns whether the data roaming setting should be ignored for satellite connection,
     * as the satellite data plan is included in the user mobile plan.
     *
     * @return {@code true} if the data roaming setting should be ignored for satellite connection.
     * {@code false} otherwise.
     */
    private boolean shouldIgnoreDataRoamingSettingForSatellite() {
        return mServiceState.isUsingNonTerrestrialNetwork()
                && mDataConfigManager.isIgnoringDataRoamingSettingForSatellite();
    }

    /**
     * This method
     *  - At evaluation network request and evaluation data network determines, if
     *    bootstrap sim current data usage reached bootstrap sim max data limit allowed set
     *    at {@link DataConfigManager#getEsimBootStrapMaxDataLimitBytes()}
     *  - Query the current data usage at {@link #getDataUsage()}, if last data usage query guarding
     *    interval as expired.
     *
     * @return true, if bootstrap sim data limit is reached
     *         else false, if bootstrap sim max data limit allowed set is -1(Unlimited) or current
     *         bootstrap sim total data usage is less than bootstrap sim max data limit allowed.
     *
     */
    private boolean isEsimBootStrapMaxDataLimitReached() {
        long esimBootStrapMaxDataLimitBytes =
                mDataConfigManager.getEsimBootStrapMaxDataLimitBytes();

        if (esimBootStrapMaxDataLimitBytes < 0L) {
            return false;
        }

        if (mBootStrapSimTotalDataUsageBytes < esimBootStrapMaxDataLimitBytes
                && (mBootstrapSimLastDataUsageQueryTime == 0
                || SystemClock.elapsedRealtime() - mBootstrapSimLastDataUsageQueryTime
                > GUARD_TIMER_INTERVAL_TO_QUERY_DATA_USAGE_API_STATS_MILLIS)) {
            mBootStrapSimTotalDataUsageBytes = getDataUsage();
            log("current bootstrap sim data usage: " + mBootStrapSimTotalDataUsageBytes);
            mBootstrapSimLastDataUsageQueryTime =  SystemClock.elapsedRealtime();
        }
        return mBootStrapSimTotalDataUsageBytes >= esimBootStrapMaxDataLimitBytes;
    }

    /**
     * Query network usage statistics summaries based on {@link
     * NetworkStatsManager#querySummaryForDevice(NetworkTemplate, long, long)}
     *
     * @return Data usage in bytes for the connected networks related to the current subscription
     */
    private long getDataUsage() {
        NetworkStatsManager networkStatsManager =
                        mPhone.getContext().getSystemService(NetworkStatsManager.class);

        if (networkStatsManager != null) {
            final NetworkTemplate.Builder builder =
                    new NetworkTemplate.Builder(NetworkTemplate.MATCH_MOBILE);
            final String subscriberId = mPhone.getSubscriberId();

            if (!TextUtils.isEmpty(subscriberId)) {
                builder.setSubscriberIds(Set.of(subscriberId));
                // Consider data usage calculation of only metered capabilities / data network
                builder.setMeteredness(android.net.NetworkStats.METERED_YES);
                NetworkTemplate template = builder.build();
                final NetworkStats.Bucket ret = networkStatsManager
                        .querySummaryForDevice(template, 0L, System.currentTimeMillis());
                return ret.getRxBytes() + ret.getTxBytes();
            }
        }
        return 0L;
    }

    /**
     * @return The grouped unsatisfied network requests. The network requests that have the same
     * network capabilities is grouped into one {@link NetworkRequestList}.
     */
    @NonNull
    private List<NetworkRequestList> getGroupedUnsatisfiedNetworkRequests() {
        NetworkRequestList networkRequestList = new NetworkRequestList();
        for (TelephonyNetworkRequest networkRequest : mAllNetworkRequestList) {
            if (networkRequest.getState() == TelephonyNetworkRequest.REQUEST_STATE_UNSATISFIED) {
                networkRequestList.add(networkRequest);
            }
        }
        return DataUtils.getGroupedNetworkRequestList(networkRequestList, mFeatureFlags);
    }

    /**
     * Called when it's needed to evaluate all unsatisfied network requests.
     *
     * @param reason The reason for evaluation.
     */
    private void onReevaluateUnsatisfiedNetworkRequests(@NonNull DataEvaluationReason reason) {
        // First, try to group similar network request together.
        List<NetworkRequestList> networkRequestLists = getGroupedUnsatisfiedNetworkRequests();
        log("Re-evaluating " + networkRequestLists.stream().mapToInt(List::size).sum()
                + " unsatisfied network requests in " + networkRequestLists.size()
                + " groups, " + networkRequestLists.stream().map(
                        NetworkRequestList::toStringSimplified)
                .collect(Collectors.joining(", ")) + " due to " + reason);

        // Second, see if any existing network can satisfy those network requests.
        for (NetworkRequestList requestList : networkRequestLists) {
            if (findCompatibleDataNetworkAndAttach(requestList)) {
                continue;
            }

            // If no data network can satisfy the requests, then start the evaluation process. Since
            // all the requests in the list have the same capabilities, we can only evaluate one
            // of them.
            DataEvaluation evaluation = evaluateNetworkRequest(requestList.get(0), reason);
            if (!evaluation.containsDisallowedReasons()) {
                DataProfile dataProfile = evaluation.getCandidateDataProfile();
                if (dataProfile != null) {
                    setupDataNetwork(dataProfile, null,
                            evaluation.getDataAllowedReason());
                }
            }
        }
    }

    /**
     * Evaluate an existing data network to see if it is still allowed to exist. For example, if
     * RAT changes from LTE to UMTS, an IMS data network is not allowed anymore. Or when SIM is
     * removal, all data networks (except emergency) should be torn down.
     *
     * @param dataNetwork The data network to evaluate.
     * @param reason The reason for evaluation.
     *
     * @return The data evaluation result.
     */
    @NonNull
    private DataEvaluation evaluateDataNetwork(@NonNull DataNetwork dataNetwork,
            @NonNull DataEvaluationReason reason) {
        DataEvaluation evaluation = new DataEvaluation(reason);
        // Bypass all checks for emergency data network.
        if (dataNetwork.getNetworkCapabilities().hasCapability(
                NetworkCapabilities.NET_CAPABILITY_EIMS)) {
            evaluation.addDataAllowedReason(DataAllowedReason.EMERGENCY_REQUEST);
            log(evaluation.toString());
            return evaluation;
        }

        // Check SIM state
        if (mSimState != TelephonyManager.SIM_STATE_LOADED) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.SIM_NOT_READY);
        }

        // Check if device is in CDMA ECBM
        if (mPhone.isInCdmaEcm()) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.CDMA_EMERGENCY_CALLBACK_MODE);
        }

        if (mFeatureFlags.dataServiceCheck()
                && !isDataServiceSupported(dataNetwork.getTransport())) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.SERVICE_OPTION_NOT_SUPPORTED);
        }

        // If the network is satellite, then the network must be restricted.
        // The IWLAN data network should remain intact even when satellite is connected.
        if (dataNetwork.getTransport() != AccessNetworkConstants.TRANSPORT_TYPE_WLAN
                && mServiceState.isUsingNonTerrestrialNetwork() != dataNetwork.isSatellite()) {
            // Since we don't support satellite/cellular network handover, we should always
            // tear down the network when transport changes.
            evaluation.addDataDisallowedReason(
                    DataDisallowedReason.DATA_NETWORK_TRANSPORT_NOT_ALLOWED);
        }

        // Check whether data limit reached for bootstrap sim, else re-evaluate based on the timer
        // set.
        if (isEsimBootStrapProvisioningActivated()
                && dataNetwork.getTransport() == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
            if (isEsimBootStrapMaxDataLimitReached()) {
                log("BootStrap Sim Data Usage limit reached");
                evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_LIMIT_REACHED);
            } else {
                if (!hasMessages(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS)) {
                    sendMessageDelayed(obtainMessage(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                            DataEvaluationReason.CHECK_DATA_USAGE),
                            mDataConfigManager.getReevaluateBootstrapSimDataUsageMillis());
                } else {
                    log("skip scheduling evaluating existing data networks since already"
                            + "scheduled");
                }
            }
        }

        // Check if there are other network that has higher priority, and only single data network
        // is allowed.
        if (isOnlySingleDataNetworkAllowed(dataNetwork.getTransport())
                && !hasCapabilityExemptsFromSinglePdnRule(
                        dataNetwork.getNetworkCapabilities().getCapabilities())) {
            // If there is network request that has higher priority than this data network, then
            // tear down the network, regardless that network request is satisfied or not.
            if (mAllNetworkRequestList.stream()
                    .filter(request -> dataNetwork.getTransport()
                            == mAccessNetworksManager.getPreferredTransportByNetworkCapability(
                                    request.getHighestPriorityApnTypeNetworkCapability()))
                    .filter(request
                            -> !hasCapabilityExemptsFromSinglePdnRule(request.getCapabilities()))
                    .anyMatch(request -> request.getPriority() > dataNetwork.getPriority())) {
                evaluation.addDataDisallowedReason(
                        DataDisallowedReason.ONLY_ALLOWED_SINGLE_NETWORK);
            } else {
                log("evaluateDataNetwork: " + dataNetwork + " has the highest priority. "
                        + "No need to tear down");
            }
        }

        // It's recommended for IMS service not requesting MMTEL capability, so that MMTEL
        // capability is dynamically added when moving between vops and nonvops area.
        boolean vopsIsRequired = dataNetwork.hasNetworkCapabilityInNetworkRequests(
                NetworkCapabilities.NET_CAPABILITY_MMTEL);

        // Check an active call relying on this network and config for "delay tear down due to vops
        // call" is enabled.
        if (dataNetwork.shouldDelayImsTearDownDueToInCall()) {
            if (vopsIsRequired) {
                log("Ignored VoPS check due to delay IMS tear down until call ends.");
            }
        } else {
            // Reach here means we should ignore active calls even if there are any.

            // Check if VoPS requirement is met.
            if (vopsIsRequired) {
                if (dataNetwork.getTransport() == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                    NetworkRegistrationInfo nri = mServiceState.getNetworkRegistrationInfo(
                            NetworkRegistrationInfo.DOMAIN_PS,
                            AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
                    if (nri != null) {
                        DataSpecificRegistrationInfo dsri = nri.getDataSpecificInfo();
                        if (dsri != null && dsri.getVopsSupportInfo() != null
                                && !dsri.getVopsSupportInfo().isVopsSupported()
                                && !mDataConfigManager.shouldKeepNetworkUpInNonVops(
                                        nri.getNetworkRegistrationState())) {
                            evaluation.addDataDisallowedReason(
                                    DataDisallowedReason.VOPS_NOT_SUPPORTED);
                        }
                    }
                }
            }

            // Check if handover retry stopped and preferred transport still not matched.
            int preferredTransport = mAccessNetworksManager
                    .getPreferredTransportByNetworkCapability(
                            dataNetwork.getApnTypeNetworkCapability());
            if (preferredTransport != dataNetwork.getTransport()
                    && mDataRetryManager.isDataNetworkHandoverRetryStopped(dataNetwork)) {
                evaluation.addDataDisallowedReason(DataDisallowedReason.HANDOVER_RETRY_STOPPED);
            }
        }

        // Check if data is disabled
        boolean dataDisabled = !mDataSettingsManager.isDataEnabled();

        // Check if data roaming is disabled
        if (mServiceState.getDataRoaming() && !mDataSettingsManager.isDataRoamingEnabled()) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.ROAMING_DISABLED);
        }

        // Check if current data network type is allowed by the data profile. Use the lingering
        // network type. Some data network is allowed to create on certain RATs, but can linger
        // to extended RATs. For example, IMS is allowed to be created on LTE only, but can
        // extend its life cycle to 3G.
        int networkType = getDataNetworkType(dataNetwork.getTransport());
        DataProfile dataProfile = dataNetwork.getDataProfile();
        if (dataProfile.getApnSetting() != null) {
            // Check if data is disabled for the APN type
            dataDisabled = !mDataSettingsManager.isDataEnabled(
                    DataUtils.networkCapabilityToApnType(
                            dataNetwork.getApnTypeNetworkCapability()));

            // Sometimes network temporarily OOS and network type becomes UNKNOWN. We don't
            // tear down network in that case.
            if (networkType != TelephonyManager.NETWORK_TYPE_UNKNOWN
                    && !dataProfile.getApnSetting().canSupportLingeringNetworkType(networkType)) {
                log("networkType=" + TelephonyManager.getNetworkTypeName(networkType)
                        + ", networkTypeBitmask="
                        + TelephonyManager.convertNetworkTypeBitmaskToString(
                                dataProfile.getApnSetting().getNetworkTypeBitmask())
                        + ", lingeringNetworkTypeBitmask="
                        + TelephonyManager.convertNetworkTypeBitmaskToString(
                                dataProfile.getApnSetting().getLingeringNetworkTypeBitmask()));
                evaluation.addDataDisallowedReason(
                        DataDisallowedReason.DATA_NETWORK_TYPE_NOT_ALLOWED);
            }
        }

        if (dataDisabled) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_DISABLED);
        }

        // Check if the data profile is still compatible, sometimes the users can remove it from the
        // APN editor. If some of the important fields are changed in APN settings, we need to
        // tear down the network. Note traffic descriptor from the data profile will not be checked.
        if (!mDataProfileManager.isDataProfileCompatible(dataProfile)) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_PROFILE_INVALID);
        }

        // If users switch preferred profile in APN editor, we need to tear down network.
        if (dataNetwork.isInternetSupported()
                && !mDataProfileManager.isDataProfilePreferred(dataProfile)
                && mDataProfileManager.canPreferredDataProfileSatisfy(
                        dataNetwork.getAttachedNetworkRequestList())) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_PROFILE_NOT_PREFERRED);
        }

        // Check whether if there are any reason we should tear down the network.
        if (!evaluation.containsDisallowedReasons()) {
            // The data is allowed in the current condition.
            evaluation.addDataAllowedReason(DataAllowedReason.NORMAL);
        } else if (!evaluation.containsHardDisallowedReasons()) {
            // If there are reasons we should tear down the network, check if those are hard reasons
            // or soft reasons. In some scenarios, we can make exceptions if they are soft
            // disallowed reasons.
            if ((mTelecomManager.isInEmergencyCall() || mPhone.isInEcm())
                    && dataNetwork.isEmergencySupl()) {
                // Check if it's SUPL during emergency call.
                evaluation.addDataAllowedReason(DataAllowedReason.EMERGENCY_SUPL);
            } else if (!dataNetwork.getNetworkCapabilities().hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    && dataNetwork.getAttachedNetworkRequestList().stream()
                            .allMatch(this::isValidRestrictedRequest)) {
                // Check if request is restricted and there are no exceptional requests attached to
                // the network.
                evaluation.addDataAllowedReason(DataAllowedReason.RESTRICTED_REQUEST);
            } else if (dataNetwork.getTransport() == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                // Check if request is unmetered (WiFi or unmetered APN)
                evaluation.addDataAllowedReason(DataAllowedReason.UNMETERED_USAGE);
            } else {
                boolean unmeteredNetwork = !mDataConfigManager.isAnyMeteredCapability(
                        dataNetwork.getNetworkCapabilities()
                                .getCapabilities(), mServiceState.getDataRoaming());
                if (unmeteredNetwork) {
                    evaluation.addDataAllowedReason(DataAllowedReason.UNMETERED_USAGE);
                }
            }
        }

        // Check if we allow additional lingering for active VoPS call network if
        // a. this network is SRVCC handover in progress
        // or b. "delay tear down due to active VoPS call" is enabled
        boolean isInSrvcc = vopsIsRequired && mIsSrvccHandoverInProcess;
        if (evaluation.containsOnly(DataDisallowedReason.DATA_NETWORK_TYPE_NOT_ALLOWED)
                && (dataNetwork.shouldDelayImsTearDownDueToInCall() || isInSrvcc)) {
            evaluation.addDataAllowedReason(DataAllowedReason.IN_VOICE_CALL);
        }

        log("Evaluated " + dataNetwork + ", " + evaluation);
        return evaluation;
    }

    /**
     * Check if the available services support data service.
     * {@link NetworkRegistrationInfo#SERVICE_TYPE_DATA} service or not.
     *
     * @param transport The preferred transport type for the request. The transport here is
     * WWAN/WLAN.
     * @return {@code true} if data services is supported, otherwise {@code false}.
     */
    private boolean isDataServiceSupported(@TransportType int transport) {
        NetworkRegistrationInfo nri = mServiceState.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, transport);
        return nri != null && nri.getAvailableServices().contains(
                    NetworkRegistrationInfo.SERVICE_TYPE_DATA);
    }

    /**
     * Check if the transport from connectivity service can satisfy the network request. Note the
     * transport here is connectivity service's transport (Wifi, cellular, satellite, etc..), not
     * the widely used {@link AccessNetworkConstants#TRANSPORT_TYPE_WLAN WLAN},
     * {@link AccessNetworkConstants#TRANSPORT_TYPE_WWAN WWAN} transport in telephony.
     *
     * @param networkRequest Network request
     * @param transport The preferred transport type for the request. The transport here is
     * WWAN/WLAN.
     * @return {@code true} if the connectivity transport can satisfy the network request, otherwise
     * {@code false}.
     */
    private boolean canConnectivityTransportSatisfyNetworkRequest(
            @NonNull TelephonyNetworkRequest networkRequest, @TransportType int transport) {
        // Check if this is a IWLAN network request.
        if (transport == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
            // If the request would result in bringing up network on IWLAN, then no
            // need to check if the device is using satellite network.
            return true;
        }

        // When the device is on satellite, allow network request without bandwidth not constrained
        // to enable data connection with constrained network.
        if (mServiceState.isUsingNonTerrestrialNetwork() && networkRequest.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)) {

            int dataPolicy;
            if (mFeatureFlags.dataServiceCheck()) {
                final SatelliteController satelliteController = SatelliteController.getInstance();
                dataPolicy = satelliteController.getSatelliteDataServicePolicyForPlmn(mSubId,
                        mPhone.getServiceState().getOperatorNumeric());
            } else {
                dataPolicy = mDataConfigManager.getSatelliteDataSupportMode();
            }
            switch (dataPolicy) {
                case CarrierConfigManager.SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED -> {
                    return false;
                }
                case CarrierConfigManager.SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED -> {
                    try {
                        if (networkRequest.hasCapability(DataUtils
                                .NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED)) {
                            return false;
                        }
                    } catch (Exception ignored) { }
                }
                // default case CarrierConfigManager.SATELLITE_DATA_SUPPORT_ALL
            }
        }

        // If the network request does not specify cellular or satellite, then it can be
        // satisfied when the device is either on cellular ot satellite.
        if (!networkRequest.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                && !networkRequest.hasTransport(NetworkCapabilities.TRANSPORT_SATELLITE)) {
            return true;
        }

        // As a short term solution, allowing some networks to be always marked as cellular
        // transport if certain capabilities are in the network request.
        if (networkRequest.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && Arrays.stream(
                        networkRequest.getCapabilities())
                .anyMatch(mDataConfigManager.getForcedCellularTransportCapabilities()::contains)) {
            return true;
        }

        // If the network is cellular, then the request must specify cellular transport. Or if the
        // the network is satellite, then the request must specify satellite transport and
        // restricted.
        return (mServiceState.isUsingNonTerrestrialNetwork()
                && networkRequest.hasTransport(NetworkCapabilities.TRANSPORT_SATELLITE))
                || (!mServiceState.isUsingNonTerrestrialNetwork()
                        && networkRequest.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    /**
     * Check if a network request should be treated as a valid restricted network request that
     * can bypass soft disallowed reasons, for example, mobile data off.
     *
     * @param networkRequest The network request to evaluate.
     * @return {@code true} if the request can be considered as a valid restricted network request
     * that can bypass any soft disallowed reasons, otherwise {@code false}.
     */
    private boolean isValidRestrictedRequest(@NonNull TelephonyNetworkRequest networkRequest) {
        // tethering, enterprise and mms with restricted capabilities always honor soft
        // disallowed reasons and not respected as restricted request
        if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_DUN)
                || networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE)
                || networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS)) {
            return false;
        }
        // When the device is on satellite, internet with restricted capabilities always honor
        // soft disallowed reasons and not respected as restricted request
        // Note - ping test are performed with restricted request on satellite assuming they cannot
        // bypass any checks. If below is removed, reevaluate the ping request in
        // CellularNetworkValidator and the getInternetEvaluation in AutoDataSwitchController
        return !(mServiceState.isUsingNonTerrestrialNetwork()
                && networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
    }

    /**
     * Called when needed to re-evaluate existing data networks and tear down networks if needed.
     *
     * @param reason The reason for this data evaluation.
     */
    private void onReevaluateExistingDataNetworks(@NonNull DataEvaluationReason reason) {
        if (mDataNetworkList.isEmpty()) {
            log("onReevaluateExistingDataNetworks: No existing data networks to re-evaluate.");
            return;
        }
        log("Re-evaluating " + mDataNetworkList.size() + " existing data networks due to "
                + reason);
        for (DataNetwork dataNetwork : mDataNetworkList) {
            if (dataNetwork.isConnecting() || dataNetwork.isConnected()) {
                DataEvaluation dataEvaluation = evaluateDataNetwork(dataNetwork, reason);
                if (dataEvaluation.containsDisallowedReasons()) {
                    tearDownGracefully(dataNetwork, getTearDownReason(dataEvaluation));
                }
            }
        }
    }

    /**
     * Evaluate if it is allowed to handover the data network between IWLAN and cellular. Some
     * carriers do not allow handover in certain conditions.
     *
     * @param dataNetwork The data network to be handover.
     * @return The evaluation result.
     *
     * @see CarrierConfigManager#KEY_IWLAN_HANDOVER_POLICY_STRING_ARRAY
     */
    @NonNull
    private DataEvaluation evaluateDataNetworkHandover(@NonNull DataNetwork dataNetwork) {
        DataEvaluation dataEvaluation = new DataEvaluation(DataEvaluationReason.DATA_HANDOVER);
        if (!dataNetwork.isConnecting() && !dataNetwork.isConnected()) {
            dataEvaluation.addDataDisallowedReason(DataDisallowedReason.ILLEGAL_STATE);
            return dataEvaluation;
        }

        // If enhanced handover check is enabled, perform extra checks.
        if (mDataConfigManager.isEnhancedIwlanHandoverCheckEnabled()) {
            int targetTransport = DataUtils.getTargetTransport(dataNetwork.getTransport());
            NetworkRegistrationInfo nri = mServiceState.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, targetTransport);
            if (nri != null) {
                // Check if OOS on target transport.
                if (!nri.isInService()) {
                    dataEvaluation.addDataDisallowedReason(DataDisallowedReason.NOT_IN_SERVICE);
                }

                // Check if VoPS is required, but the target transport is non-VoPS.
                // It's recommended for IMS service not requesting MMTEL capability, so that MMTEL
                // capability is dynamically added when moving between vops and nonvops area.
                NetworkRequestList networkRequestList =
                        dataNetwork.getAttachedNetworkRequestList();
                if (networkRequestList.stream().anyMatch(request
                        -> request.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMTEL))) {
                    DataSpecificRegistrationInfo dsri = nri.getDataSpecificInfo();
                    // Check if the network is non-VoPS.
                    if (dsri != null && dsri.getVopsSupportInfo() != null
                            && !dsri.getVopsSupportInfo().isVopsSupported()
                            && !mDataConfigManager.shouldKeepNetworkUpInNonVops(
                                    nri.getNetworkRegistrationState())) {
                        dataEvaluation.addDataDisallowedReason(
                                DataDisallowedReason.VOPS_NOT_SUPPORTED);
                    }
                }

                if (dataEvaluation.containsDisallowedReasons()) {
                    return dataEvaluation;
                }
            }
        }

        if (mDataConfigManager.isIwlanHandoverPolicyEnabled()) {
            List<HandoverRule> handoverRules = mDataConfigManager.getHandoverRules();

            int sourceNetworkType = getDataNetworkType(dataNetwork.getTransport());
            if (sourceNetworkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                // Using the data network type stored in the data network. We
                // cache the last known network type in data network controller
                // because data network has much shorter life cycle. It can prevent
                // the obsolete last known network type cached in data network
                // type controller.
                sourceNetworkType = dataNetwork.getLastKnownDataNetworkType();
            }
            int sourceAccessNetwork = DataUtils.networkTypeToAccessNetworkType(
                    sourceNetworkType);
            NetworkRegistrationInfo nri = mServiceState.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            boolean isWwanInService = nri != null && nri.isInService();
            // If WWAN is inService, use the real roaming state reported by modem instead of
            // using the overridden roaming state, otherwise get last known roaming state stored
            // in data network.
            boolean isRoaming = isWwanInService ? mServiceState.getDataRoamingFromRegistration()
                    : dataNetwork.getLastKnownRoamingState();
            Phone imsPhone = mPhone.getImsPhone();
            boolean isIncall = mFeatureFlags.incallHandoverPolicy() && imsPhone != null
                    && (imsPhone.getCallTracker().getState() != PhoneConstants.State.IDLE);
            int targetAccessNetwork = DataUtils.networkTypeToAccessNetworkType(
                    getDataNetworkType(DataUtils.getTargetTransport(dataNetwork.getTransport())));
            NetworkCapabilities capabilities = dataNetwork.getNetworkCapabilities();
            log("evaluateDataNetworkHandover: "
                    + "source=" + AccessNetworkType.toString(sourceAccessNetwork)
                    + ", target=" + AccessNetworkType.toString(targetAccessNetwork)
                    + ", roaming=" + isRoaming
                    + ", incall=" + isIncall
                    + ", ServiceState=" + mServiceState
                    + ", capabilities=" + capabilities);

            // Matching the rules by the configured order. Bail out if find first matching rule.
            for (HandoverRule rule : handoverRules) {
                // Check if the rule is only for roaming and we are not  aroaming.
                if (rule.isOnlyForRoaming && !isRoaming) {
                    // If the rule is for roaming only, and the device is not roaming, then bypass
                    // this rule.
                    continue;
                }
                if (rule.isOnlyForIncall && (!mFeatureFlags.incallHandoverPolicy() || !isIncall)) {
                    // If the rule is for incall only, and the device is not incall, then bypass
                    // this rule.
                    continue;
                }

                if (rule.sourceAccessNetworks.contains(sourceAccessNetwork)
                        && rule.targetAccessNetworks.contains(targetAccessNetwork)) {
                    // if no capability rule specified,
                    // data network capability is considered matched.
                    // otherwise, any capabilities overlap is also considered matched.
                    if (rule.networkCapabilities.isEmpty()
                            || rule.networkCapabilities.stream()
                            .anyMatch(capabilities::hasCapability)) {
                        log("evaluateDataNetworkHandover: Matched " + rule);
                        if (rule.type == HandoverRule.RULE_TYPE_DISALLOWED) {
                            dataEvaluation.addDataDisallowedReason(
                                    DataDisallowedReason.NOT_ALLOWED_BY_POLICY);
                        } else {
                            dataEvaluation.addDataAllowedReason(DataAllowedReason.NORMAL);
                        }
                        log("evaluateDataNetworkHandover: " + dataEvaluation);
                        return dataEvaluation;
                    }
                }
            }
            log("evaluateDataNetworkHandover: Did not find matching rule.");
        } else {
            log("evaluateDataNetworkHandover: IWLAN handover policy not enabled.");
        }

        // Allow handover by default if no rule is found/not enabled by config.
        dataEvaluation.addDataAllowedReason(DataAllowedReason.NORMAL);
        return dataEvaluation;
    }

    /**
     * Get tear down reason from the evaluation result.
     *
     * @param dataEvaluation The evaluation result from
     * {@link #evaluateDataNetwork(DataNetwork, DataEvaluationReason)}.
     * @return The tear down reason.
     */
    @TearDownReason
    private static int getTearDownReason(@NonNull DataEvaluation dataEvaluation) {
        if (dataEvaluation.containsDisallowedReasons()) {
            switch (dataEvaluation.getDataDisallowedReasons().get(0)) {
                case DATA_DISABLED:
                    return DataNetwork.TEAR_DOWN_REASON_DATA_DISABLED;
                case ROAMING_DISABLED:
                    return DataNetwork.TEAR_DOWN_REASON_ROAMING_DISABLED;
                case DEFAULT_DATA_UNSELECTED:
                    return DataNetwork.TEAR_DOWN_REASON_DEFAULT_DATA_UNSELECTED;
                case NOT_IN_SERVICE:
                    return DataNetwork.TEAR_DOWN_REASON_NOT_IN_SERVICE;
                case DATA_CONFIG_NOT_READY:
                    return DataNetwork.TEAR_DOWN_REASON_DATA_CONFIG_NOT_READY;
                case SIM_NOT_READY:
                    return DataNetwork.TEAR_DOWN_REASON_SIM_REMOVAL;
                case CONCURRENT_VOICE_DATA_NOT_ALLOWED:
                    return DataNetwork.TEAR_DOWN_REASON_CONCURRENT_VOICE_DATA_NOT_ALLOWED;
                case SERVICE_OPTION_NOT_SUPPORTED:
                    return DataNetwork.TEAR_DOWN_REASON_SERVICE_OPTION_NOT_SUPPORTED;
                case RADIO_POWER_OFF:
                    return DataNetwork.TEAR_DOWN_REASON_AIRPLANE_MODE_ON;
                case PENDING_TEAR_DOWN_ALL:
                    return DataNetwork.TEAR_DOWN_REASON_PENDING_TEAR_DOWN_ALL;
                case RADIO_DISABLED_BY_CARRIER:
                    return DataNetwork.TEAR_DOWN_REASON_POWER_OFF_BY_CARRIER;
                case DATA_SERVICE_NOT_READY:
                    return DataNetwork.TEAR_DOWN_REASON_DATA_SERVICE_NOT_READY;
                case NO_SUITABLE_DATA_PROFILE:
                    return DataNetwork.TEAR_DOWN_REASON_NO_SUITABLE_DATA_PROFILE;
                case DATA_NETWORK_TYPE_NOT_ALLOWED:
                    return DataNetwork.TEAR_DOWN_REASON_RAT_NOT_ALLOWED;
                case CDMA_EMERGENCY_CALLBACK_MODE:
                    return DataNetwork.TEAR_DOWN_REASON_CDMA_EMERGENCY_CALLBACK_MODE;
                case RETRY_SCHEDULED:
                    return DataNetwork.TEAR_DOWN_REASON_RETRY_SCHEDULED;
                case DATA_THROTTLED:
                    return DataNetwork.TEAR_DOWN_REASON_DATA_THROTTLED;
                case DATA_PROFILE_INVALID:
                    return DataNetwork.TEAR_DOWN_REASON_DATA_PROFILE_INVALID;
                case DATA_PROFILE_NOT_PREFERRED:
                    return DataNetwork.TEAR_DOWN_REASON_DATA_PROFILE_NOT_PREFERRED;
                case NOT_ALLOWED_BY_POLICY:
                    return DataNetwork.TEAR_DOWN_REASON_NOT_ALLOWED_BY_POLICY;
                case ILLEGAL_STATE:
                    return DataNetwork.TEAR_DOWN_REASON_ILLEGAL_STATE;
                case VOPS_NOT_SUPPORTED:
                    return DataNetwork.TEAR_DOWN_REASON_VOPS_NOT_SUPPORTED;
                case ONLY_ALLOWED_SINGLE_NETWORK:
                    return DataNetwork.TEAR_DOWN_REASON_ONLY_ALLOWED_SINGLE_NETWORK;
                case HANDOVER_RETRY_STOPPED:
                    return DataNetwork.TEAR_DOWN_REASON_HANDOVER_FAILED;
                case DATA_LIMIT_REACHED:
                    return DataNetwork.TEAR_DOWN_REASON_DATA_LIMIT_REACHED;
                case DATA_NETWORK_TRANSPORT_NOT_ALLOWED:
                    return DataNetwork.TEAR_DOWN_REASON_DATA_NETWORK_TRANSPORT_NOT_ALLOWED;
            }
        }
        return DataNetwork.TEAR_DOWN_REASON_NONE;
    }

    /**
     * Check whether a dataNetwork is actively capable of internet connection
     * @param cid dataNetwork unique identifier
     * @return true if the dataNetwork is connected and capable of internet connection
     */
    public boolean isInternetNetwork(int cid) {
        for (DataNetwork dataNetwork : mDataNetworkList) {
            if (dataNetwork.getId() == cid
                    && dataNetwork.isConnected()
                    && dataNetwork.isInternetSupported()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} if data is dormant.
     */
    private boolean isDataDormant() {
        return mDataNetworkList.stream().anyMatch(
                dataNetwork -> dataNetwork.getLinkStatus()
                        == DataCallResponse.LINK_STATUS_DORMANT)
                && mDataNetworkList.stream().noneMatch(
                        dataNetwork -> dataNetwork.getLinkStatus()
                                == DataCallResponse.LINK_STATUS_ACTIVE);
    }

    /**
     * Update data activity.
     */
    private void updateDataActivity() {
        int dataActivity = TelephonyManager.DATA_ACTIVITY_NONE;
        if (isDataDormant()) {
            dataActivity = TelephonyManager.DATA_ACTIVITY_DORMANT;
        } else if (mPhone.getLinkBandwidthEstimator() != null) {
            dataActivity = mPhone.getLinkBandwidthEstimator().getDataActivity();
        }

        if (mDataActivity != dataActivity) {
            logv("updateDataActivity: dataActivity="
                    + DataUtils.dataActivityToString(dataActivity));
            mDataActivity = dataActivity;
            mPhone.notifyDataActivity();
        }
    }

    /**
     * Remove a network request, which is originated from the apps. Note that remove a network
     * will not result in tearing down the network. The tear down request directly comes from
     * {@link com.android.server.ConnectivityService} through
     * {@link NetworkAgent#onNetworkUnwanted()}.
     *
     * @param networkRequest Network request
     */
    public void removeNetworkRequest(@NonNull TelephonyNetworkRequest networkRequest) {
        sendMessage(obtainMessage(EVENT_REMOVE_NETWORK_REQUEST, networkRequest));
    }

    private void onRemoveNetworkRequest(@NonNull TelephonyNetworkRequest request) {
        if (!mAllNetworkRequestList.remove(request)) {
            loge("onRemoveNetworkRequest: Network request does not exist. " + request);
            return;
        }

        if (request.hasCapability(NetworkCapabilities.NET_CAPABILITY_IMS)) {
            mImsThrottleCounter.addOccurrence();
            mLastReleasedImsRequestCapabilities = request.getCapabilities();
            mLastImsOperationIsRelease = true;
        }

        if (request.getAttachedNetwork() != null) {
            request.getAttachedNetwork().detachNetworkRequest(
                    request, false /* shouldRetry */);
        }

        request.setState(TelephonyNetworkRequest.REQUEST_STATE_UNSATISFIED);
        request.setEvaluation(null);

        log("onRemoveNetworkRequest: Removed " + request);
        return;
    }

    /**
     * Check if the network request is existing. Note this method is not thread safe so can be only
     * called within the modules in {@link com.android.internal.telephony.data}.
     *
     * @param networkRequest Telephony network request to check.
     * @return {@code true} if the network request exists.
     */
    public boolean isNetworkRequestExisting(@NonNull TelephonyNetworkRequest networkRequest) {
        return mAllNetworkRequestList.contains(networkRequest);
    }

    /**
     * Get data network by interface name.
     *
     * @param interfaceName The network interface name.
     * @return The data network if found.
     */
    @Nullable
    public DataNetwork getDataNetworkByInterface(@NonNull String interfaceName) {
        return mDataNetworkList.stream()
                .filter(dataNetwork -> !(dataNetwork.isDisconnecting()
                        || dataNetwork.isDisconnected()))
                .filter(dataNetwork -> interfaceName.equals(
                        dataNetwork.getLinkProperties().getInterfaceName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if the device is in eSIM bootstrap provisioning state.
     *
     * @return {@code true} if the device is under eSIM bootstrap provisioning.
     */
    public boolean isEsimBootStrapProvisioningActivated() {
        if (!mFeatureFlags.esimBootstrapProvisioningFlag()) {
            return false;
        }

        SubscriptionInfoInternal subInfo = SubscriptionManagerService.getInstance()
                .getSubscriptionInfoInternal(mPhone.getSubId());
        return subInfo != null
                && subInfo.getProfileClass() == SubscriptionManager.PROFILE_CLASS_PROVISIONING;
    }

    /**
     * Register for IMS feature registration state.
     *
     * @param subId The subscription index.
     * @param imsFeature The IMS feature. Only {@link ImsFeature#FEATURE_MMTEL} and
     * {@link ImsFeature#FEATURE_RCS} are supported at this point.
     */
    private void registerImsFeatureRegistrationState(int subId,
            @ImsFeature.FeatureType int imsFeature) {
        RegistrationManager.RegistrationCallback callback =
                new RegistrationManager.RegistrationCallback() {
                    @Override
                    public void onRegistered(@NonNull ImsRegistrationAttributes attributes) {
                        log("IMS " + DataUtils.imsFeatureToString(imsFeature)
                                + " registered. Attributes=" + attributes);
                        mRegisteredImsFeatures.add(imsFeature);
                    }

                    @Override
                    public void onUnregistered(@NonNull ImsReasonInfo info) {
                        log("IMS " + DataUtils.imsFeatureToString(imsFeature)
                                + " deregistered. Info=" + info);
                        mRegisteredImsFeatures.remove(imsFeature);
                        evaluatePendingImsDeregDataNetworks();
                    }
                };

        try {
            // Use switch here as we can't make a generic callback registration logic because
            // RcsManager does not implement RegistrationManager.
            switch (imsFeature) {
                case ImsFeature.FEATURE_MMTEL:
                    mImsManager.getImsMmTelManager(subId).registerImsRegistrationCallback(
                            DataNetworkController.this::post, callback);
                    break;
                case ImsFeature.FEATURE_RCS:
                    mImsManager.getImsRcsManager(subId).registerImsRegistrationCallback(
                            DataNetworkController.this::post, callback);
                    break;
            }

            // Store the callback so that we can unregister in the future.
            mImsFeatureRegistrationCallbacks.put(imsFeature, callback);
            log("Successfully register " + DataUtils.imsFeatureToString(imsFeature)
                    + " registration state. subId=" + subId);
        } catch (ImsException e) {
            loge("updateImsFeatureRegistrationStateListening: subId=" + subId
                    + ", imsFeature=" + DataUtils.imsFeatureToString(imsFeature) + ", " + e);
        }
    }

    /**
     * Unregister IMS feature callback.
     *
     * @param subId The subscription index.
     * @param imsFeature The IMS feature. Only {@link ImsFeature#FEATURE_MMTEL} and
     * {@link ImsFeature#FEATURE_RCS} are supported at this point.
     */
    private void unregisterImsFeatureRegistrationState(int subId,
            @ImsFeature.FeatureType int imsFeature) {
        RegistrationManager.RegistrationCallback oldCallback =
                mImsFeatureRegistrationCallbacks.get(imsFeature);
        if (oldCallback != null) {
            if (imsFeature == ImsFeature.FEATURE_MMTEL) {
                mImsManager.getImsMmTelManager(subId)
                        .unregisterImsRegistrationCallback(oldCallback);
            } else if (imsFeature == ImsFeature.FEATURE_RCS) {
                mImsManager.getImsRcsManager(subId)
                        .unregisterImsRegistrationCallback(oldCallback);
            }
            log("Successfully unregistered " + DataUtils.imsFeatureToString(imsFeature)
                    + " registration state. subId=" + subId);
            mImsFeatureRegistrationCallbacks.remove(imsFeature);
        }
    }

    /**
     * Register IMS state callback.
     *
     * @param subId Subscription index.
     */
    private void registerImsStateCallback(int subId) {
        if (isImsSupportedOnDevice()) {
            Function<Integer, ImsStateCallback> imsFeatureStateCallbackFactory =
                    imsFeature -> new ImsStateCallback() {
                        @Override
                        public void onUnavailable(int reason) {
                            // Unregister registration state update when IMS service is unbound.
                            unregisterImsFeatureRegistrationState(subId, imsFeature);
                        }

                        @Override
                        public void onAvailable() {
                            mImsFeaturePackageName.put(imsFeature, ImsResolver.getInstance()
                                    .getConfiguredImsServicePackageName(mPhone.getPhoneId(),
                                            imsFeature));
                            // Once IMS service is bound, register for registration state update.
                            registerImsFeatureRegistrationState(subId, imsFeature);
                        }

                        @Override
                        public void onError() {
                        }
                    };

            try {
                ImsStateCallback callback = imsFeatureStateCallbackFactory
                        .apply(ImsFeature.FEATURE_MMTEL);
                mImsManager.getImsMmTelManager(subId).registerImsStateCallback(this::post,
                        callback);
                mImsStateCallbacks.put(ImsFeature.FEATURE_MMTEL, callback);
                log("Successfully register MMTEL state on sub " + subId);

                callback = imsFeatureStateCallbackFactory.apply(ImsFeature.FEATURE_RCS);
                mImsManager.getImsRcsManager(subId).registerImsStateCallback(this::post, callback);
                mImsStateCallbacks.put(ImsFeature.FEATURE_RCS, callback);
                log("Successfully register RCS state on sub " + subId);
            } catch (ImsException e) {
                loge("Exception when registering IMS state callback. " + e);
            }
        } else {
            log("IMS is not supported on this device, skipping registerImsStateCallback");
        }
    }

    /**
     * Unregister IMS feature state callbacks.
     *
     * @param subId Subscription index.
     */
    private void unregisterImsStateCallbacks(int subId) {
        if (isImsSupportedOnDevice()) {
            ImsStateCallback callback = mImsStateCallbacks.get(ImsFeature.FEATURE_MMTEL);
            if (callback != null) {
                mImsManager.getImsMmTelManager(subId).unregisterImsStateCallback(callback);
                mImsStateCallbacks.remove(ImsFeature.FEATURE_MMTEL);
                log("Unregister MMTEL state on sub " + subId);
            }

            callback = mImsStateCallbacks.get(ImsFeature.FEATURE_RCS);
            if (callback != null) {
                mImsManager.getImsRcsManager(subId).unregisterImsStateCallback(callback);
                mImsStateCallbacks.remove(ImsFeature.FEATURE_RCS);
                log("Unregister RCS state on sub " + subId);
            }
        } else {
            log("IMS is not supported on this device, skipping unregisterImsStateCallbacks");
        }
    }

    /**
     * called to check if FEATURE_TELEPHONY_IMS feature is available
     */
    private boolean isImsSupportedOnDevice() {
        return mPhone.getContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TELEPHONY_IMS);
    }

    /** Called when subscription info changed. */
    private void onSubscriptionChanged() {
        if (mSubId != mPhone.getSubId()) {
            log("onDataConfigUpdated: mSubId changed from " + mSubId + " to "
                    + mPhone.getSubId());
            if (isImsGracefulTearDownSupported()) {
                if (SubscriptionManager.isValidSubscriptionId(mPhone.getSubId())) {
                    registerImsStateCallback(mPhone.getSubId());
                } else {
                    unregisterImsStateCallbacks(mSubId);
                }
            }
            mSubId = mPhone.getSubId();
            updateSubscriptionPlans();
        }
    }

    /**
     * Called when carrier config was updated.
     */
    private void onCarrierConfigUpdated() {
        log("onCarrierConfigUpdated: config is "
                + (mDataConfigManager.isConfigCarrierSpecific() ? "" : "not ")
                + "carrier specific. mSimState="
                + TelephonyManager.simStateToString(mSimState));
        updateNetworkRequestsPriority();
        onReevaluateUnsatisfiedNetworkRequests(DataEvaluationReason.DATA_CONFIG_CHANGED);
    }

    /**
     * Called when device config was updated.
     */
    private void onDeviceConfigUpdated() {
        log("onDeviceConfigUpdated: DeviceConfig updated.");
        updateAnomalySlidingWindowCounters();
    }

    /**
     * Update each network request's priority.
     */
    private void updateNetworkRequestsPriority() {
        for (TelephonyNetworkRequest networkRequest : mAllNetworkRequestList) {
            networkRequest.updatePriority();
        }
    }

    /**
     * Update the threshold of anomaly report counters
     */
    private void updateAnomalySlidingWindowCounters() {
        mImsThrottleCounter = new SlidingWindowEventCounter(
                mDataConfigManager.getAnomalyImsReleaseRequestThreshold().timeWindow,
                mDataConfigManager.getAnomalyImsReleaseRequestThreshold().eventNumOccurrence);
        mNetworkUnwantedCounter = new SlidingWindowEventCounter(
                mDataConfigManager.getAnomalyNetworkUnwantedThreshold().timeWindow,
                mDataConfigManager.getAnomalyNetworkUnwantedThreshold().eventNumOccurrence);
        mSetupDataCallWwanFailureCounter = new SlidingWindowEventCounter(
                mDataConfigManager.getAnomalySetupDataCallThreshold().timeWindow,
                mDataConfigManager.getAnomalySetupDataCallThreshold().eventNumOccurrence);
        mSetupDataCallWlanFailureCounter = new SlidingWindowEventCounter(
                mDataConfigManager.getAnomalySetupDataCallThreshold().timeWindow,
                mDataConfigManager.getAnomalySetupDataCallThreshold().eventNumOccurrence);
    }

    /**
     * There have been several bugs where a RECONNECT loop kicks off where a data network
     * is brought up, but connectivity service indicates that the network is unwanted so telephony
     * tears down the network. But later telephony bring up the data network again and becomes an
     * infinite loop. By the time we get the bug report it's too late because there have already
     * been hundreds of bring up/tear down. This is meant to capture the issue when it first starts.
     */
    private void onTrackNetworkUnwanted() {
        if (mNetworkUnwantedCounter.addOccurrence()) {
            reportAnomaly("Network Unwanted called "
                            + mNetworkUnwantedCounter.getFrequencyString(),
                    "9f3bc55b-bfa6-4e26-afaa-5031426a66d3");
        }
    }

    /**
     * Find unsatisfied network requests that can be satisfied by the given data profile.
     *
     * @param dataProfile The data profile.
     * @return The network requests list.
     */
    @NonNull
    private NetworkRequestList findSatisfiableNetworkRequests(@NonNull DataProfile dataProfile) {
        return new NetworkRequestList(mAllNetworkRequestList.stream()
                .filter(request -> request.getState()
                        == TelephonyNetworkRequest.REQUEST_STATE_UNSATISFIED)
                .filter(request -> request.canBeSatisfiedBy(dataProfile))
                .collect(Collectors.toList()));
    }

    /**
     * Setup data network.
     *
     * @param dataProfile The data profile to setup the data network.
     * @param dataSetupRetryEntry Data retry entry. {@code null} if this data network setup is not
     * initiated by a data retry.
     * @param allowedReason The reason that why setting up this data network is allowed.
     */
    private void setupDataNetwork(@NonNull DataProfile dataProfile,
            @Nullable DataSetupRetryEntry dataSetupRetryEntry,
            @NonNull DataAllowedReason allowedReason) {
        log("onSetupDataNetwork: dataProfile=" + dataProfile + ", retryEntry="
                + dataSetupRetryEntry + ", allowed reason=" + allowedReason + ", service state="
                + mServiceState);
        for (DataNetwork dataNetwork : mDataNetworkList) {
            DataProfile currentDataProfile = dataNetwork.getDataProfile();
            if (dataProfile.equals(currentDataProfile)
                    || mDataProfileManager.areDataProfilesSharingApn(
                            dataProfile, currentDataProfile)) {
                log("onSetupDataNetwork: Found existing data network " + dataNetwork
                        + " using the same or a similar data profile.");
                if (dataSetupRetryEntry != null) {
                    dataSetupRetryEntry.setState(DataRetryEntry.RETRY_STATE_CANCELLED);
                }
                return;
            }
        }

        NetworkRequestList networkRequestList = findSatisfiableNetworkRequests(dataProfile);

        if (networkRequestList.isEmpty()) {
            log("Can't find any unsatisfied network requests that can be satisfied by this data "
                    + "profile.");
            if (dataSetupRetryEntry != null) {
                dataSetupRetryEntry.setState(DataRetryEntry.RETRY_STATE_CANCELLED);
            }

            return;
        }

        int transport = mAccessNetworksManager.getPreferredTransportByNetworkCapability(
                networkRequestList.get(0).getHighestPriorityApnTypeNetworkCapability());
        logl("Creating data network on "
                + AccessNetworkConstants.transportTypeToString(transport) + " with " + dataProfile
                + ", and attaching " + networkRequestList.size() + " network requests to it.");

        mDataNetworkList.add(new DataNetwork(mPhone, mFeatureFlags, getLooper(),
                mDataServiceManagers, dataProfile, networkRequestList, transport, allowedReason,
                new DataNetworkCallback(this::post) {
                    @Override
                    public void onSetupDataFailed(@NonNull DataNetwork dataNetwork,
                            @NonNull NetworkRequestList requestList, @DataFailureCause int cause,
                            long retryDelayMillis) {
                        if (dataSetupRetryEntry != null) {
                            dataSetupRetryEntry.setState(DataRetryEntry.RETRY_STATE_FAILED);
                        }
                        DataNetworkController.this.onDataNetworkSetupFailed(
                                dataNetwork, requestList, cause, retryDelayMillis);
                    }

                    @Override
                    public void onConnected(@NonNull DataNetwork dataNetwork) {
                        if (dataSetupRetryEntry != null) {
                            dataSetupRetryEntry.setState(DataRetryEntry.RETRY_STATE_SUCCEEDED);
                        }
                        DataNetworkController.this.onDataNetworkConnected(dataNetwork);
                    }

                    @Override
                    public void onAttachFailed(@NonNull DataNetwork dataNetwork,
                            @NonNull NetworkRequestList requestList) {
                        DataNetworkController.this.onAttachNetworkRequestsFailed(
                                dataNetwork, requestList);
                    }

                    @Override
                    public void onValidationStatusChanged(@NonNull DataNetwork dataNetwork,
                            @ValidationStatus int status, @Nullable Uri redirectUri) {
                        DataNetworkController.this.onDataNetworkValidationStatusChanged(
                                dataNetwork, status, redirectUri);
                    }

                    @Override
                    public void onSuspendedStateChanged(@NonNull DataNetwork dataNetwork,
                            boolean suspended) {
                        DataNetworkController.this.onDataNetworkSuspendedStateChanged(
                                dataNetwork, suspended);
                    }

                    @Override
                    public void onDisconnected(@NonNull DataNetwork dataNetwork,
                            @DataFailureCause int cause, @TearDownReason int tearDownReason) {
                        DataNetworkController.this.onDataNetworkDisconnected(
                                dataNetwork, cause, tearDownReason);
                    }

                    @Override
                    public void onHandoverSucceeded(@NonNull DataNetwork dataNetwork) {
                        DataNetworkController.this.onDataNetworkHandoverSucceeded(dataNetwork);
                    }

                    @Override
                    public void onHandoverFailed(@NonNull DataNetwork dataNetwork,
                            @DataFailureCause int cause, long retryDelayMillis,
                            @HandoverFailureMode int handoverFailureMode) {
                        DataNetworkController.this.onDataNetworkHandoverFailed(
                                dataNetwork, cause, retryDelayMillis, handoverFailureMode);
                    }

                    @Override
                    public void onLinkStatusChanged(@NonNull DataNetwork dataNetwork,
                            @LinkStatus int linkStatus) {
                        DataNetworkController.this.onLinkStatusChanged(dataNetwork, linkStatus);
                    }

                    @Override
                    public void onPcoDataChanged(@NonNull DataNetwork dataNetwork) {
                        DataNetworkController.this.onPcoDataChanged(dataNetwork);
                    }

                    @Override
                    public void onNetworkCapabilitiesChanged(@NonNull DataNetwork dataNetwork) {
                        DataNetworkController.this.onNetworkCapabilitiesChanged(dataNetwork);
                    }

                    @Override
                    public void onTrackNetworkUnwanted(@NonNull DataNetwork dataNetwork) {
                        DataNetworkController.this.onTrackNetworkUnwanted();
                    }

                    @Override
                    public void onRetryUnsatisfiedNetworkRequest(
                            @NonNull TelephonyNetworkRequest networkRequest) {
                        DataNetworkController.this.onRetryUnsatisfiedNetworkRequest(
                                networkRequest);
                    }

                    @Override
                    public void onQosSessionsChanged(
                            @NonNull List<QosBearerSession> qosBearerSessions) {
                        mDataNetworkControllerCallbacks.forEach(
                                callback -> callback.invokeFromExecutor(() ->
                                        callback.onQosSessionsChanged(qosBearerSessions)));
                    }
                }
        ));
        if (!mAnyDataNetworkExisting) {
            mAnyDataNetworkExisting = true;
            mDataNetworkControllerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                    () -> callback.onAnyDataNetworkExistingChanged(mAnyDataNetworkExisting)));
        }
    }

    /**
     * Called when setup data network failed.
     *
     * @param dataNetwork The data network.
     * @param requestList The network requests attached to the data network.
     * @param cause The fail cause
     * @param retryDelayMillis The retry timer suggested by the network/data service.
     */
    private void onDataNetworkSetupFailed(@NonNull DataNetwork dataNetwork,
            @NonNull NetworkRequestList requestList, @DataFailureCause int cause,
            long retryDelayMillis) {
        logl("onDataNetworkSetupDataFailed: " + dataNetwork + ", cause="
                + DataFailCause.toString(cause) + ", retryDelayMillis=" + retryDelayMillis + "ms.");
        mDataNetworkList.remove(dataNetwork);
        trackSetupDataCallFailure(dataNetwork.getTransport(), cause);
        if (mAnyDataNetworkExisting && mDataNetworkList.isEmpty()) {
            mAnyDataNetworkExisting = false;
            mDataNetworkControllerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                    () -> callback.onAnyDataNetworkExistingChanged(mAnyDataNetworkExisting)));
        }

        requestList.removeIf(request -> !mAllNetworkRequestList.contains(request));
        if (requestList.isEmpty()) {
            log("onDataNetworkSetupFailed: All requests have been released. "
                    + "Will not evaluate retry.");
            return;
        }

        // Data retry manager will determine if retry is needed. If needed, retry will be scheduled.
        mDataRetryManager.evaluateDataSetupRetry(dataNetwork.getDataProfile(),
                dataNetwork.getTransport(), requestList, cause, retryDelayMillis);
    }

    /**
     * Track the frequency of setup data failure on each
     * {@link AccessNetworkConstants.TransportType} data service.
     *
     * @param transport The transport of the data service.
     * @param cause The fail cause
     */
    private void trackSetupDataCallFailure(@TransportType int transport,
            @DataFailureCause int cause) {
        switch (transport) {
            case AccessNetworkConstants.TRANSPORT_TYPE_WWAN:
                // Skip when poor signal strength
                if (mPhone.getSignalStrength().getLevel()
                        <= CellSignalStrength.SIGNAL_STRENGTH_POOR) {
                    return;
                }
                if (cause == DataFailCause.ERROR_UNSPECIFIED || cause == DataFailCause.UNKNOWN) {
                    reportAnomaly("RIL set up data call fails: unknown/unspecified error",
                            "ce7d1465-d8e4-404a-b76f-de2c60bee843");
                }
                if (mSetupDataCallWwanFailureCounter.addOccurrence()) {
                    reportAnomaly("RIL fails setup data call request "
                                    + mSetupDataCallWwanFailureCounter.getFrequencyString(),
                            "e6a98b97-9e34-4977-9a92-01d52a6691f6");
                }
                break;
            case AccessNetworkConstants.TRANSPORT_TYPE_WLAN:
                if (cause == DataFailCause.ERROR_UNSPECIFIED || cause == DataFailCause.UNKNOWN) {
                    reportAnomaly("IWLAN set up data call fails: unknown/unspecified error",
                            "a16fc15c-815b-4908-b8e6-5f3bc7cbc20b");
                }
                if (mSetupDataCallWlanFailureCounter.addOccurrence()) {
                    reportAnomaly("IWLAN data service fails setup data call request "
                                    + mSetupDataCallWlanFailureCounter.getFrequencyString(),
                            "e2248d8b-d55f-42bd-871c-0cfd80c3ddd1");
                }
                break;
            default:
                loge("trackSetupDataCallFailure: INVALID transport.");
        }
    }

    /**
     * Trigger the anomaly report with the specified UUID.
     *
     * @param anomalyMsg Description of the event
     * @param uuid UUID associated with that event
     */
    private void reportAnomaly(@NonNull String anomalyMsg, @NonNull String uuid) {
        logl(anomalyMsg);
        AnomalyReporter.reportAnomaly(UUID.fromString(uuid), anomalyMsg, mPhone.getCarrierId());
    }

    /**
     * Called when data network is connected.
     *
     * @param dataNetwork The data network.
     */
    private void onDataNetworkConnected(@NonNull DataNetwork dataNetwork) {
        logl("onDataNetworkConnected: " + dataNetwork);

        mDataNetworkControllerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onDataNetworkConnected(dataNetwork.getTransport(),
                        dataNetwork.getDataProfile())));

        mPreviousConnectedDataNetworkList.add(0, dataNetwork);
        // Preserve the connected data networks for debugging purposes.
        if (mPreviousConnectedDataNetworkList.size() > MAX_HISTORICAL_CONNECTED_DATA_NETWORKS) {
            mPreviousConnectedDataNetworkList.remove(MAX_HISTORICAL_CONNECTED_DATA_NETWORKS);
        }

        if (dataNetwork.isInternetSupported()) updateOverallInternetDataState();

        if (dataNetwork.getNetworkCapabilities().hasCapability(
                NetworkCapabilities.NET_CAPABILITY_IMS)) {
            logl("IMS data state changed from "
                    + TelephonyUtils.dataStateToString(mImsDataNetworkState) + " to CONNECTED.");
            mImsDataNetworkState = TelephonyManager.DATA_CONNECTED;
        }

        if (isEsimBootStrapProvisioningActivated()) {
            sendMessageDelayed(obtainMessage(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                    DataEvaluationReason.CHECK_DATA_USAGE),
                    mDataConfigManager.getReevaluateBootstrapSimDataUsageMillis());
        }
    }

    /**
     * Called when needed to retry data setup.
     *
     * @param dataSetupRetryEntry The data setup retry entry scheduled by {@link DataRetryManager}.
     */
    private void onDataNetworkSetupRetry(@NonNull DataSetupRetryEntry dataSetupRetryEntry) {
        // The request might be already removed before retry happens. Remove them from the list
        // if that's the case. Copy the list first. We don't want to remove the requests from
        // the retry entry. They can be later used to determine what kind of retry it is.
        NetworkRequestList requestList = new NetworkRequestList(
                dataSetupRetryEntry.networkRequestList);
        requestList.removeIf(request -> !mAllNetworkRequestList.contains(request));
        // Retrieves the newly added unsatisfied NetworkRequest if all NetworkRequests in the
        // DataSetupRetryEntry have already been removed.
        if (requestList.isEmpty()) {
            List<NetworkRequestList> groupRequestLists = getGroupedUnsatisfiedNetworkRequests();
            dataSetupRetryEntry.networkRequestList.stream()
                    .filter(request -> groupRequestLists.stream()
                            .anyMatch(groupRequestList -> {
                                // The unsatisfied request has all the requested capabilities.
                                if (groupRequestList.get(request.getCapabilities()) == null) {
                                    return false;
                                }
                                TelephonyNetworkRequest leading = groupRequestList.getFirst();
                                // The unsatisfied request covers all the requested transports.
                                return leading.getTransportTypes().length == 0
                                        || request.getTransportTypes().length == 0
                                        || Arrays.stream(request.getTransportTypes())
                                        .allMatch(leading::hasTransport);
                            }))
                    .forEach(requestList::add);
        }
        if (requestList.isEmpty()) {
            loge("onDataNetworkSetupRetry: Request list is empty. Abort retry.");
            dataSetupRetryEntry.setState(DataRetryEntry.RETRY_STATE_CANCELLED);
            return;
        }
        log("onDataNetworkSetupRetry: Request list:" + requestList);
        TelephonyNetworkRequest telephonyNetworkRequest = requestList.get(0);

        int networkCapability = telephonyNetworkRequest
                .getHighestPriorityApnTypeNetworkCapability();
        int preferredTransport = mAccessNetworksManager.getPreferredTransportByNetworkCapability(
                networkCapability);
        if (preferredTransport != dataSetupRetryEntry.transport) {
            log("Cannot re-satisfy " + telephonyNetworkRequest + " on "
                    + AccessNetworkConstants.transportTypeToString(dataSetupRetryEntry.transport)
                    + ". The preferred transport has switched to "
                    + AccessNetworkConstants.transportTypeToString(preferredTransport)
                    + ". " + dataSetupRetryEntry);
            // Cancel the retry since the preferred transport has already changed, but then
            // re-evaluate the unsatisfied network requests again so the new network can be brought
            // up on the new target transport later.
            dataSetupRetryEntry.setState(DataRetryEntry.RETRY_STATE_CANCELLED);
            sendMessage(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                    DataEvaluationReason.PREFERRED_TRANSPORT_CHANGED));
            return;
        }

        DataEvaluation evaluation = evaluateNetworkRequest(
                telephonyNetworkRequest, DataEvaluationReason.DATA_RETRY);
        if (!evaluation.containsDisallowedReasons()) {
            DataProfile dataProfile = dataSetupRetryEntry.dataProfile;
            if (dataProfile == null || !mDataProfileManager.isDataProfileCompatible(dataProfile)) {
                dataProfile = evaluation.getCandidateDataProfile();
            }
            if (dataProfile != null) {
                setupDataNetwork(dataProfile, dataSetupRetryEntry,
                        evaluation.getDataAllowedReason());
            } else {
                loge("onDataNetworkSetupRetry: Not able to find a suitable data profile to retry.");
                dataSetupRetryEntry.setState(DataRetryEntry.RETRY_STATE_FAILED);
            }
        } else {
            dataSetupRetryEntry.setState(DataRetryEntry.RETRY_STATE_FAILED);
        }
    }

    /**
     * Called when needed to retry data network handover.
     *
     * @param dataHandoverRetryEntry The handover entry.
     */
    private void onDataNetworkHandoverRetry(
            @NonNull DataHandoverRetryEntry dataHandoverRetryEntry) {
        DataNetwork dataNetwork = dataHandoverRetryEntry.dataNetwork;
        if (!mDataNetworkList.contains(dataNetwork)) {
            log("onDataNetworkHandoverRetry: " + dataNetwork + " no longer exists.");
            dataHandoverRetryEntry.setState(DataRetryEntry.RETRY_STATE_CANCELLED);
            return;
        }

        if (!dataNetwork.isConnected()) {
            log("onDataNetworkHandoverRetry: " + dataNetwork + " is not in the right state.");
            dataHandoverRetryEntry.setState(DataRetryEntry.RETRY_STATE_CANCELLED);
            return;
        }

        int preferredTransport = mAccessNetworksManager.getPreferredTransportByNetworkCapability(
                dataNetwork.getApnTypeNetworkCapability());
        if (dataNetwork.getTransport() == preferredTransport) {
            log("onDataNetworkHandoverRetry: " + dataNetwork + " is already on the preferred "
                    + "transport " + AccessNetworkConstants.transportTypeToString(
                            preferredTransport) + ".");
            dataHandoverRetryEntry.setState(DataRetryEntry.RETRY_STATE_CANCELLED);
            return;
        }

        logl("onDataNetworkHandoverRetry: Start handover " + dataNetwork + " to "
                + AccessNetworkConstants.transportTypeToString(preferredTransport)
                + ", " + dataHandoverRetryEntry);
        tryHandoverDataNetwork(dataNetwork, preferredTransport, dataHandoverRetryEntry);
    }

    /**
     * Called when data network reached max handover retry count.
     *
     * @param dataNetwork The data network.
     */
    private void onDataNetworkHandoverRetryStopped(@NonNull DataNetwork dataNetwork) {
        int preferredTransport = mAccessNetworksManager
                .getPreferredTransportByNetworkCapability(
                        dataNetwork.getApnTypeNetworkCapability());
        if (dataNetwork.getTransport() == preferredTransport) {
            log("onDataNetworkHandoverRetryStopped: " + dataNetwork + " is already "
                    + "on the preferred transport "
                    + AccessNetworkConstants.transportTypeToString(
                    preferredTransport));
            return;
        }
        if (dataNetwork.shouldDelayImsTearDownDueToInCall()) {
            log("onDataNetworkHandoverRetryStopped: Delay IMS tear down until call "
                    + "ends. " + dataNetwork);
            return;
        }

        tearDownGracefully(dataNetwork,
                DataNetwork.TEAR_DOWN_REASON_HANDOVER_FAILED);
    }

    /**
     * Called when data network validation status changed.
     *
     * @param status one of {@link NetworkAgent#VALIDATION_STATUS_VALID} or
     * {@link NetworkAgent#VALIDATION_STATUS_NOT_VALID}.
     * @param redirectUri If internet connectivity is being redirected (e.g., on a captive portal),
     * this is the destination the probes are being redirected to, otherwise {@code null}.
     *
     * @param dataNetwork The data network.
     */
    private void onDataNetworkValidationStatusChanged(@NonNull DataNetwork dataNetwork,
            @ValidationStatus int status, @Nullable Uri redirectUri) {
        log("onDataNetworkValidationStatusChanged: " + dataNetwork + ", validation status="
                + DataUtils.validationStatusToString(status)
                + (redirectUri != null ? ", " + redirectUri : ""));
        if (!TextUtils.isEmpty(redirectUri.toString())) {
            Intent intent = new Intent(TelephonyManager.ACTION_CARRIER_SIGNAL_REDIRECTED);
            intent.putExtra(TelephonyManager.EXTRA_REDIRECTION_URL, redirectUri);
            mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);
            log("Notify carrier signal receivers with redirectUri: " + redirectUri);
        }

        if (status != NetworkAgent.VALIDATION_STATUS_VALID
                && status != NetworkAgent.VALIDATION_STATUS_NOT_VALID) {
            loge("Invalid validation status " + status + " received.");
            return;
        }

        if (!mDataSettingsManager.isRecoveryOnBadNetworkEnabled()) {
            log("Ignore data network validation status changed because "
                    + "data stall recovery is disabled.");
            return;
        }

        // Only track the networks that require validation.
        // The criteria is base on NetworkMonitorUtils.java.
        NetworkCapabilities capabilities = dataNetwork.getNetworkCapabilities();
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
            if (status == NetworkAgent.VALIDATION_STATUS_NOT_VALID
                    && (dataNetwork.getCurrentState() == null || dataNetwork.isDisconnected())) {
                log("Ignoring invalid validation status for disconnected DataNetwork");
                return;
            }
            mDataNetworkControllerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                    () -> callback.onInternetDataNetworkValidationStatusChanged(status)));
        }
    }

    /**
     * Called when data network suspended state changed.
     *
     * @param dataNetwork The data network.
     * @param suspended {@code true} if data is suspended.
     */
    private void onDataNetworkSuspendedStateChanged(@NonNull DataNetwork dataNetwork,
            boolean suspended) {
        if (dataNetwork.isInternetSupported()) updateOverallInternetDataState();

        if (dataNetwork.getNetworkCapabilities().hasCapability(
                NetworkCapabilities.NET_CAPABILITY_IMS)) {
            logl("IMS data state changed from "
                    + TelephonyUtils.dataStateToString(mImsDataNetworkState) + " to "
                    + (suspended ? "SUSPENDED" : "CONNECTED"));
            mImsDataNetworkState = suspended
                    ? TelephonyManager.DATA_SUSPENDED : TelephonyManager.DATA_CONNECTED;
        }
    }

    /**
     * Called when data network disconnected.
     *
     * @param dataNetwork The data network.
     * @param cause The disconnect cause.
     * @param tearDownReason The reason the network was torn down
     */
    private void onDataNetworkDisconnected(@NonNull DataNetwork dataNetwork,
            @DataFailureCause int cause, @TearDownReason int tearDownReason) {
        logl("onDataNetworkDisconnected: " + dataNetwork + ", cause="
                + DataFailCause.toString(cause) + "(" + cause + "), tearDownReason="
                + DataNetwork.tearDownReasonToString(tearDownReason));
        mDataNetworkList.remove(dataNetwork);
        mPendingImsDeregDataNetworks.remove(dataNetwork);
        mDataRetryManager.cancelPendingHandoverRetry(dataNetwork);
        if (dataNetwork.isInternetSupported()) updateOverallInternetDataState();

        if (dataNetwork.getNetworkCapabilities().hasCapability(
                NetworkCapabilities.NET_CAPABILITY_IMS)) {
            logl("IMS data state changed from "
                    + TelephonyUtils.dataStateToString(mImsDataNetworkState) + " to DISCONNECTED.");
            mImsDataNetworkState = TelephonyManager.DATA_DISCONNECTED;
        }

        if (mAnyDataNetworkExisting && mDataNetworkList.isEmpty()) {
            log("All data networks disconnected now.");
            mAnyDataNetworkExisting = false;
            mDataNetworkControllerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                    () -> callback.onAnyDataNetworkExistingChanged(mAnyDataNetworkExisting)));
        }

        // Immediately reestablish on target transport if network was torn down due to policy
        long delayMillis = tearDownReason == DataNetwork.TEAR_DOWN_REASON_HANDOVER_NOT_ALLOWED
                ? 0 : mDataConfigManager.getRetrySetupAfterDisconnectMillis();
        // Sometimes network was unsolicitedly reported lost for reasons. We should re-evaluate
        // and see if data network can be re-established again.
        sendMessageDelayed(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                        DataEvaluationReason.RETRY_AFTER_DISCONNECTED), delayMillis);
    }

    /**
     * Called when handover between IWLAN and cellular network succeeded.
     *
     * @param dataNetwork The data network.
     */
    private void onDataNetworkHandoverSucceeded(@NonNull DataNetwork dataNetwork) {
        logl("Handover successfully. " + dataNetwork + " to " + AccessNetworkConstants
                .transportTypeToString(dataNetwork.getTransport()));
        // The preferred transport might be changed when handover was in progress. We need to
        // evaluate again to make sure we are not out-of-sync with the input from access network
        // manager.
        sendMessage(obtainMessage(EVENT_EVALUATE_PREFERRED_TRANSPORT,
                dataNetwork.getApnTypeNetworkCapability(), 0));

        // There might be network we didn't tear down in the last evaluation due to handover in
        // progress. We should evaluate again.
        sendMessage(obtainMessage(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                DataEvaluationReason.DATA_HANDOVER));
    }

    /**
     * Called when data network handover between IWLAN and cellular network failed.
     *
     * @param dataNetwork The data network.
     * @param cause The fail cause.
     * @param retryDelayMillis Network suggested retry time in milliseconds.
     * {@link Long#MAX_VALUE} indicates data retry should not occur.
     * {@link DataCallResponse#RETRY_DURATION_UNDEFINED} indicates network did not suggest any
     * retry duration.
     * @param handoverFailureMode The handover failure mode that determine the behavior of
     * how frameworks should handle the handover failure.
     */
    private void onDataNetworkHandoverFailed(@NonNull DataNetwork dataNetwork,
            @DataFailureCause int cause, long retryDelayMillis,
            @HandoverFailureMode int handoverFailureMode) {
        logl("Handover failed. " + dataNetwork + ", cause=" + DataFailCause.toString(cause)
                + ", retryDelayMillis=" + retryDelayMillis + "ms, handoverFailureMode="
                + DataCallResponse.failureModeToString(handoverFailureMode));
        // There might be network we didn't tear down in the last evaluation due to handover in
        // progress. We should evaluate again.
        sendMessage(obtainMessage(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                DataEvaluationReason.DATA_HANDOVER));

        if (dataNetwork.getAttachedNetworkRequestList().isEmpty()) {
            log("onDataNetworkHandoverFailed: No network requests attached to " + dataNetwork
                    + ". No need to retry since the network will be torn down soon.");
            return;
        }

        if (handoverFailureMode == DataCallResponse.HANDOVER_FAILURE_MODE_DO_FALLBACK
                || (handoverFailureMode == DataCallResponse.HANDOVER_FAILURE_MODE_LEGACY
                && cause == DataFailCause.HANDOFF_PREFERENCE_CHANGED)) {
            // Don't retry handover anymore. Give QNS some time to switch the preferred transport
            // to the original one, but we should re-evaluate the preferred transport again to
            // make sure QNS does change it back, if not, we still need to perform handover at that
            // time.
            sendMessageDelayed(obtainMessage(EVENT_EVALUATE_PREFERRED_TRANSPORT,
                    dataNetwork.getApnTypeNetworkCapability(), 0),
                    REEVALUATE_PREFERRED_TRANSPORT_DELAY_MILLIS);
        } else if (handoverFailureMode == DataCallResponse
                .HANDOVER_FAILURE_MODE_NO_FALLBACK_RETRY_SETUP_NORMAL || handoverFailureMode
                == DataCallResponse.HANDOVER_FAILURE_MODE_LEGACY) {
            int preferredTransport = mAccessNetworksManager
                    .getPreferredTransportByNetworkCapability(
                            dataNetwork.getApnTypeNetworkCapability());
            if (dataNetwork.getTransport() == preferredTransport) {
                log("onDataNetworkHandoverFailed: Already on preferred transport "
                        + AccessNetworkConstants.transportTypeToString(preferredTransport)
                        + ". No further actions needed.");
                return;
            }

            int targetTransport = DataUtils.getTargetTransport(dataNetwork.getTransport());
            mDataRetryManager.evaluateDataSetupRetry(dataNetwork.getDataProfile(), targetTransport,
                    dataNetwork.getAttachedNetworkRequestList(), cause, retryDelayMillis);
            // Tear down the data network on source transport. Retry manager will schedule
            // setup a new data network on the target transport.
            tearDownGracefully(dataNetwork, DataNetwork.TEAR_DOWN_REASON_HANDOVER_FAILED);
        } else {
            mDataRetryManager.evaluateDataHandoverRetry(dataNetwork, cause, retryDelayMillis);
        }
    }

    /**
     * Called when network requests failed to attach to the data network.
     *
     * @param dataNetwork The data network that can't be attached.
     * @param requestList The requests failed to attach to the network.
     */
    private void onAttachNetworkRequestsFailed(@NonNull DataNetwork dataNetwork,
            @NonNull NetworkRequestList requestList) {
        log("Failed to attach " + requestList + " to " + dataNetwork);
    }

    /**
     * Called when a network request is detached from the data network and should be retried.
     *
     * @param networkRequest The detached network request.
     */
    private void onRetryUnsatisfiedNetworkRequest(
            @NonNull TelephonyNetworkRequest networkRequest) {
        if (!mAllNetworkRequestList.contains(networkRequest)) return;

        sendMessageDelayed(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                        DataEvaluationReason.UNSATISFIED_REQUEST_DETACHED),
                REEVALUATE_UNSATISFIED_NETWORK_REQUESTS_AFTER_DETACHED_DELAY_MILLIS);
    }

    /**
     * Called when data stall occurs and needed to tear down / setup a new data network for
     * internet. This event is from {@link DataStallRecoveryManager}.
     */
    private void onDataStallReestablishInternet() {
        log("onDataStallReestablishInternet: Tear down data networks that support internet.");
        // Tear down all data networks that support internet. After data disconnected, unsatisfied
        // network requests will be re-evaluate again and data network controller will attempt to
        // setup data networks to satisfy them.
        mDataNetworkList.stream()
                .filter(DataNetwork::isInternetSupported)
                .forEach(dataNetwork -> dataNetwork.tearDown(
                        DataNetwork.TEAR_DOWN_REASON_DATA_STALL));
    }

    /**
     * Called when SRVCC handover state changes. To preserve the voice call, we don't tear down the
     * IMS network while handover in process. We reevaluate the network when handover ends.
     *
     * @param state The handover state of SRVCC
     */
    private void onSrvccStateChanged(@NonNull int[] state) {
        if (state != null && state.length != 0) {
            log("onSrvccStateChanged: " + TelephonyManager.srvccStateToString(state[0]));
            mIsSrvccHandoverInProcess = state[0] == TelephonyManager.SRVCC_STATE_HANDOVER_STARTED;
            // Reevaluate networks if SRVCC ends.
            if (!mIsSrvccHandoverInProcess
                    && !hasMessages(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS)) {
                sendMessage(obtainMessage(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                        DataEvaluationReason.SRVCC_STATE_CHANGED));
            }
        }
    }

    /**
     * Called when data service binding changed.
     *
     * @param transport The transport of the changed data service.
     * @param bound {@code true} if data service is bound.
     */
    private void onDataServiceBindingChanged(@TransportType int transport, boolean bound) {
        log("onDataServiceBindingChanged: " + AccessNetworkConstants
                .transportTypeToString(transport) + " data service is "
                + (bound ? "bound." : "unbound."));
        if (bound) {
            mDataNetworkControllerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                    () -> callback.onDataServiceBound(transport)));
        }
        mDataServiceBound.put(transport, bound);
    }

    /**
     * Called when SIM is absent.
     */
    private void onSimAbsent() {
        log("onSimAbsent");
        sendMessage(obtainMessage(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                DataEvaluationReason.SIM_REMOVAL));
    }

    /**
     * Called when SIM state changes.
     *
     * @param simState SIM state. (Note this is mixed with card state and application state.)
     */
    private void onSimStateChanged(@SimState int simState) {
        log("onSimStateChanged: state=" + TelephonyManager.simStateToString(simState));
        if (mSimState != simState) {
            mSimState = simState;
            if (simState == TelephonyManager.SIM_STATE_ABSENT) {
                onSimAbsent();
            } else if (simState == TelephonyManager.SIM_STATE_LOADED) {
                sendMessage(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                        DataEvaluationReason.SIM_LOADED));
            }
            mDataNetworkControllerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                    () -> callback.onSimStateChanged(mSimState)));
        }
    }

    /**
     * Called when needed to evaluate the preferred transport for certain capability.
     *
     * @param capability The network capability to evaluate.
     * @param forceReconnect indicates whether enforce reconnection to move to the preferred
     *                       transport type.
     *
     */
    private void onEvaluatePreferredTransport(
            @NetCapability int capability, boolean forceReconnect) {
        int preferredTransport = mAccessNetworksManager
                .getPreferredTransportByNetworkCapability(capability);
        log("onEvaluatePreferredTransport: " + DataUtils.networkCapabilityToString(capability)
                + " preferred on "
                + AccessNetworkConstants.transportTypeToString(preferredTransport));
        for (DataNetwork dataNetwork : mDataNetworkList) {
            if (dataNetwork.getApnTypeNetworkCapability() == capability) {
                // Check if the data network's current transport is different than from the
                // preferred transport. If it's different, then handover is needed.
                if (dataNetwork.getTransport() == preferredTransport) {
                    log("onEvaluatePreferredTransport:" + dataNetwork + " already on "
                            + AccessNetworkConstants.transportTypeToString(preferredTransport));
                    continue;
                }

                // If handover is ongoing, ignore the preference change for now. After handover
                // succeeds or fails, preferred transport will be re-evaluate again. Handover will
                // be performed at that time if needed.
                if (dataNetwork.isHandoverInProgress()) {
                    log("onEvaluatePreferredTransport: " + dataNetwork + " handover in progress.");
                    continue;
                }

                if (forceReconnect) {
                    tearDownGracefully(
                            dataNetwork, DataNetwork.TEAR_DOWN_REASON_HANDOVER_NOT_ALLOWED);
                } else {
                    tryHandoverDataNetwork(
                            dataNetwork, preferredTransport, null/*handoverRetryEntry*/);
                }
            }
        }
    }

    /**
     * Perform data network handover if condition allows, otherwise tear down the network to allow
     * new network setup on the target transport.
     *
     * @param dataNetwork The network on which the handover occurs
     * @param targetTransport The target transport of the handover
     * @param dataHandoverRetryEntry {@code null} if the handover attempt is not due to scheduled
     *                                           retry
     */
    private void tryHandoverDataNetwork(@NonNull DataNetwork dataNetwork,
            @TransportType int targetTransport,
            @Nullable DataHandoverRetryEntry dataHandoverRetryEntry) {
        if (dataHandoverRetryEntry == null // This handover is a new request
                && mDataRetryManager.isAnyHandoverRetryScheduled(dataNetwork)) {
            log("tryHandoverDataNetwork: retry scheduled for" + dataNetwork
                    + ", ignore this attempt");
            return;
        }
        DataEvaluation dataEvaluation = evaluateDataNetworkHandover(dataNetwork);
        log("tryHandoverDataNetwork: " + dataEvaluation + ", " + dataNetwork);
        if (!dataEvaluation.containsDisallowedReasons()) {
            logl("Start handover " + dataNetwork + " to "
                    + AccessNetworkConstants.transportTypeToString(targetTransport));
            dataNetwork.startHandover(targetTransport, dataHandoverRetryEntry);
        } else if (dataNetwork.shouldDelayImsTearDownDueToInCall()
                && (dataEvaluation.containsOnly(DataDisallowedReason.NOT_IN_SERVICE)
                || dataEvaluation.isSubsetOf(DataDisallowedReason.NOT_IN_SERVICE,
                        DataDisallowedReason.NOT_ALLOWED_BY_POLICY))) {
            // We try our best to preserve the voice call by retrying later
            if (dataHandoverRetryEntry != null) {
                dataHandoverRetryEntry.setState(DataRetryEntry.RETRY_STATE_FAILED);
            }
            mDataRetryManager.evaluateDataHandoverRetry(dataNetwork,
                    DataFailCause.HANDOVER_FAILED,
                    DataCallResponse.RETRY_DURATION_UNDEFINED /* retry mills */);
            logl("tryHandoverDataNetwork: Scheduled retry due to in voice call and target OOS");
        } else if (dataEvaluation.containsAny(DataDisallowedReason.NOT_ALLOWED_BY_POLICY,
                DataDisallowedReason.NOT_IN_SERVICE,
                DataDisallowedReason.VOPS_NOT_SUPPORTED)) {
            logl("tryHandoverDataNetwork: Handover not allowed. Tear down"
                    + dataNetwork + " so a new network can be setup on "
                    + AccessNetworkConstants.transportTypeToString(targetTransport));
            tearDownGracefully(dataNetwork,
                    DataNetwork.TEAR_DOWN_REASON_HANDOVER_NOT_ALLOWED);
        } else if (dataEvaluation.containsAny(DataDisallowedReason.ILLEGAL_STATE,
                DataDisallowedReason.RETRY_SCHEDULED)) {
            logl("tryHandoverDataNetwork: Handover not allowed. " + dataNetwork
                    + " will remain on " + AccessNetworkConstants.transportTypeToString(
                    dataNetwork.getTransport()));
        } else {
            loge("tryHandoverDataNetwork: Unexpected handover evaluation result.");
        }
    }

    /**
     * Update {@link SubscriptionPlan}s from {@link NetworkPolicyManager}.
     */
    private void updateSubscriptionPlans() {
        SubscriptionPlan[] plans = mNetworkPolicyManager.getSubscriptionPlans(
                mSubId, mPhone.getContext().getOpPackageName());
        mSubscriptionPlans.clear();
        mSubscriptionPlans.addAll(plans != null ? Arrays.asList(plans) : Collections.emptyList());
        mCongestedOverrideNetworkTypes.clear();
        mUnmeteredOverrideNetworkTypes.clear();
        log("Subscription plans initialized: " + mSubscriptionPlans);
    }

    /**
     * Called when data network's link status changed.
     *
     * @param dataNetwork The data network that has link status changed.
     * @param linkStatus The link status (i.e. RRC state).
     */
    private void onLinkStatusChanged(@NonNull DataNetwork dataNetwork, @LinkStatus int linkStatus) {
        // TODO: Since this is only used for 5G icon display logic, so we only use internet data
        //   data network's link status. Consider expanding to all data networks if needed, and
        //   should use CarrierConfigManager.KEY_LTE_ENDC_USING_USER_DATA_FOR_RRC_DETECTION_BOOL
        //   to determine if using all data networks or only internet data networks.
        int status = DataCallResponse.LINK_STATUS_INACTIVE;
        boolean anyInternet = mDataNetworkList.stream()
                .anyMatch(network -> network.isInternetSupported() && network.isConnected());
        if (anyInternet) {
            status = mDataNetworkList.stream()
                    .anyMatch(network -> network.isInternetSupported()
                            && network.isConnected() && network.getLinkStatus()
                            == DataCallResponse.LINK_STATUS_ACTIVE)
                    ? DataCallResponse.LINK_STATUS_ACTIVE
                    : DataCallResponse.LINK_STATUS_DORMANT;
        }

        if (mInternetLinkStatus != status) {
            log("Internet link status changed to " + DataUtils.linkStatusToString(status));
            mInternetLinkStatus = status;
            mDataNetworkControllerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                    () -> callback.onPhysicalLinkStatusChanged(mInternetLinkStatus)));
        }

        updateDataActivity();
    }

    /**
     * Called when PCO data changed.
     *
     * @param dataNetwork The data network.
     */
    private void onPcoDataChanged(@NonNull DataNetwork dataNetwork) {
        // Check if any data network is using NR advanced bands.
        int nrAdvancedPcoId = mDataConfigManager.getNrAdvancedCapablePcoId();
        if (nrAdvancedPcoId != 0) {
            boolean nrAdvancedCapableByPco = false;
            for (DataNetwork network : mDataNetworkList) {
                PcoData pcoData = network.getPcoData().get(nrAdvancedPcoId);
                if (pcoData != null && pcoData.contents.length > 0
                        && pcoData.contents[pcoData.contents.length - 1] == 1) {
                    nrAdvancedCapableByPco = true;
                    break;
                }
            }

            if (nrAdvancedCapableByPco != mNrAdvancedCapableByPco) {
                log("onPcoDataChanged: mNrAdvancedCapableByPco = " + nrAdvancedCapableByPco);
                mNrAdvancedCapableByPco = nrAdvancedCapableByPco;
                mDataNetworkControllerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                        () -> callback.onNrAdvancedCapableByPcoChanged(mNrAdvancedCapableByPco)));
            }
        }
    }

    /**
     * Called when network capabilities changed.
     *
     * @param dataNetwork The data network.
     */
    private void onNetworkCapabilitiesChanged(@NonNull DataNetwork dataNetwork) {
        // The network capabilities changed. See if there are unsatisfied network requests that
        // become satisfiable.
        NetworkRequestList networkRequestList = new NetworkRequestList();
        for (TelephonyNetworkRequest networkRequest : mAllNetworkRequestList) {
            if (networkRequest.getState() == TelephonyNetworkRequest.REQUEST_STATE_UNSATISFIED) {
                if (networkRequest.canBeSatisfiedBy(dataNetwork.getNetworkCapabilities())) {
                    networkRequestList.add(networkRequest);
                }
            }
        }

        if (!networkRequestList.isEmpty()) {
            log("Found more network requests that can be satisfied. " + networkRequestList);
            dataNetwork.attachNetworkRequests(networkRequestList);
        }

        if (dataNetwork.isInternetSupported()) {
            // Update because DataNetwork#isInternetSupported might have changed with capabilities.
            updateOverallInternetDataState();
        }
    }

    /**
     * Check if needed to re-evaluate the existing data networks.
     *
     * @param oldNri Previous network registration info.
     * @param newNri Current network registration info.
     * @return {@code true} if needed to re-evaluate the existing data networks.
     */
    private boolean shouldReevaluateDataNetworks(@Nullable NetworkRegistrationInfo oldNri,
            @Nullable NetworkRegistrationInfo newNri) {
        if (oldNri == null || newNri == null) return false;
        if (newNri.getAccessNetworkTechnology() == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            // Sometimes devices temporarily lose signal and RAT becomes unknown. We don't tear
            // down data network in this case.
            return false;
        }

        if (oldNri.getAccessNetworkTechnology() != newNri.getAccessNetworkTechnology()
                // Some CarrierConfig disallows vops in nonVops area for specified home/roaming.
                || (oldNri.isRoaming() != newNri.isRoaming())) {
            return true;
        }

        if (oldNri.isNonTerrestrialNetwork() != newNri.isNonTerrestrialNetwork()) {
            return true;
        }

        DataSpecificRegistrationInfo oldDsri = oldNri.getDataSpecificInfo();
        DataSpecificRegistrationInfo newDsri = newNri.getDataSpecificInfo();

        if (newDsri == null) return false;
        // If previously VoPS was supported (or does not exist), and now the network reports
        // VoPS not supported, we should evaluate existing data networks to see if they need
        // to be torn down.
        return (oldDsri == null || oldDsri.getVopsSupportInfo() == null
                || oldDsri.getVopsSupportInfo().isVopsSupported())
                && (newDsri.getVopsSupportInfo() != null && !newDsri.getVopsSupportInfo()
                .isVopsSupported());
    }

    /**
     * Check if needed to re-evaluate the unsatisfied network requests.
     *
     * @param oldSS Previous raw service state.
     * @param newSS Current raw service state.
     * @param transport The network transport to be checked.
     * @return {@code true} if needed to re-evaluate the unsatisfied network requests.
     */
    private boolean shouldReevaluateNetworkRequests(@NonNull ServiceState oldSS,
            @NonNull ServiceState newSS, @TransportType int transport)  {
        NetworkRegistrationInfo oldPsNri = oldSS.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, transport);
        NetworkRegistrationInfo newPsNri = newSS.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, transport);

        if (newPsNri == null) return false;
        if (newPsNri.getAccessNetworkTechnology() == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            // Sometimes devices temporarily lose signal and RAT becomes unknown. We don't setup
            // data in this case.
            return false;
        }

        if (oldPsNri == null
                || oldPsNri.getAccessNetworkTechnology() != newPsNri.getAccessNetworkTechnology()
                || (!oldPsNri.isInService() && newPsNri.isInService())
                // Some CarrierConfig allows vops in nonVops area for specified home/roaming.
                || (oldPsNri.isRoaming() != newPsNri.isRoaming())) {
            return true;
        }

        // If CS connection is back to service on non-DDS, reevaluate for potential PS
        if (!serviceStateAllowsPSAttach(oldSS, transport)
                && serviceStateAllowsPSAttach(newSS, transport)) {
            return true;
        }

        if (oldSS.isUsingNonTerrestrialNetwork() != newSS.isUsingNonTerrestrialNetwork()) {
            return true;
        }

        DataSpecificRegistrationInfo oldDsri = oldPsNri.getDataSpecificInfo();
        DataSpecificRegistrationInfo newDsri = newPsNri.getDataSpecificInfo();

        if (oldDsri == null) return false;
        // If previously VoPS was not supported, and now the network reports
        // VoPS supported (or does not report), we should evaluate the unsatisfied network
        // request to see if the can be satisfied again.
        return (newDsri == null || newDsri.getVopsSupportInfo() == null
                || newDsri.getVopsSupportInfo().isVopsSupported())
                && (oldDsri.getVopsSupportInfo() != null && !oldDsri.getVopsSupportInfo()
                .isVopsSupported());
    }

    /**
     * Called when service state changed.
     */
    // Note that this is only called when data RAT or data registration changed. If we need to know
    // more "changed" events other than data RAT and data registration state, we should add
    // a new listening ServiceStateTracker.registerForServiceStateChanged().
    private void onServiceStateChanged() {
        // Use the raw service state instead of the mPhone.getServiceState().
        ServiceState newServiceState = mPhone.getServiceStateTracker().getServiceState();
        StringBuilder debugMessage = new StringBuilder("onServiceStateChanged: ");
        boolean evaluateNetworkRequests = false, evaluateDataNetworks = false;

        if (!mServiceState.equals(newServiceState)) {
            log("onServiceStateChanged: changed to " + newServiceState);
            for (int transport : mAccessNetworksManager.getAvailableTransports()) {
                NetworkRegistrationInfo oldNri = mServiceState.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_PS, transport);
                NetworkRegistrationInfo newNri = newServiceState.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_PS, transport);
                debugMessage.append("[").append(
                        AccessNetworkConstants.transportTypeToString(transport)).append(": ");
                debugMessage.append(oldNri != null ? TelephonyManager.getNetworkTypeName(
                        oldNri.getAccessNetworkTechnology()) : null);
                debugMessage.append("->").append(
                        newNri != null ? TelephonyManager.getNetworkTypeName(
                                newNri.getAccessNetworkTechnology()) : null).append(", ");
                debugMessage.append(
                        oldNri != null ? NetworkRegistrationInfo.registrationStateToString(
                                oldNri.getRegistrationState()) : null);
                debugMessage.append("->").append(newNri != null
                        ? NetworkRegistrationInfo.registrationStateToString(
                        newNri.getRegistrationState()) : null).append(", ");
                debugMessage.append(oldNri != null ? NetworkRegistrationInfo
                        .isNonTerrestrialNetworkToString(oldNri.isNonTerrestrialNetwork()) : null);
                debugMessage.append("->").append(newNri != null ? NetworkRegistrationInfo
                        .isNonTerrestrialNetworkToString(newNri.isNonTerrestrialNetwork()) : null)
                        .append("] ");
                if (shouldReevaluateDataNetworks(oldNri, newNri)) {
                    if (!hasMessages(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS)) {
                        sendMessage(obtainMessage(EVENT_REEVALUATE_EXISTING_DATA_NETWORKS,
                                DataEvaluationReason.DATA_SERVICE_STATE_CHANGED));
                        evaluateDataNetworks = true;
                    }
                }
                if (shouldReevaluateNetworkRequests(mServiceState, newServiceState, transport)) {
                    if (!hasMessages(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS)) {
                        sendMessage(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                                DataEvaluationReason.DATA_SERVICE_STATE_CHANGED));
                        evaluateNetworkRequests = true;
                    }
                }
            }
            mServiceState = newServiceState;
        } else {
            debugMessage.append("not changed");
        }
        debugMessage.append(". Evaluating network requests is ").append(
                evaluateNetworkRequests ? "" : "not ").append(
                "needed, evaluating existing data networks is ").append(
                evaluateDataNetworks ? "" : "not ").append("needed.");
        log(debugMessage.toString());
    }

    /**
     * Update the internet data network state. For now only {@link TelephonyManager#DATA_CONNECTED},
     * {@link TelephonyManager#DATA_SUSPENDED}, and {@link TelephonyManager#DATA_DISCONNECTED}
     * are supported.
     */
    private void updateOverallInternetDataState() {
        boolean anyInternetConnected = mDataNetworkList.stream()
                .anyMatch(dataNetwork -> dataNetwork.isInternetSupported()
                        && (dataNetwork.isConnected() || dataNetwork.isHandoverInProgress()));
        // If any one is not suspended, then the overall is not suspended.
        Set<DataNetwork> allConnectedInternetDataNetworks = mDataNetworkList.stream()
                .filter(DataNetwork::isInternetSupported)
                .filter(dataNetwork -> dataNetwork.isConnected()
                        || dataNetwork.isHandoverInProgress())
                .collect(Collectors.toSet());
        boolean isSuspended = !allConnectedInternetDataNetworks.isEmpty()
                && allConnectedInternetDataNetworks.stream().allMatch(DataNetwork::isSuspended);
        logv("isSuspended=" + isSuspended + ", anyInternetConnected=" + anyInternetConnected
                + ", mDataNetworkList=" + mDataNetworkList);

        int dataNetworkState = TelephonyManager.DATA_DISCONNECTED;
        if (isSuspended) {
            dataNetworkState = TelephonyManager.DATA_SUSPENDED;
        } else if (anyInternetConnected) {
            dataNetworkState = TelephonyManager.DATA_CONNECTED;
        }

        if (mInternetDataNetworkState != dataNetworkState) {
            logl("Internet data state changed from "
                    + TelephonyUtils.dataStateToString(mInternetDataNetworkState) + " to "
                    + TelephonyUtils.dataStateToString(dataNetworkState) + ".");
            mInternetDataNetworkState = dataNetworkState;
        }
        // Check data network reference equality to update current connected internet networks.
        if (!mConnectedInternetNetworks.equals(allConnectedInternetDataNetworks)) {
            mConnectedInternetNetworks = allConnectedInternetDataNetworks;
            mDataNetworkControllerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                    () -> callback.onConnectedInternetDataNetworksChanged(
                            allConnectedInternetDataNetworks)));
        }
    }

    /**
     * @return Data config manager instance.
     */
    @NonNull
    public DataConfigManager getDataConfigManager() {
        return mDataConfigManager;
    }

    /**
     * @return Data profile manager instance.
     */
    @NonNull
    public DataProfileManager getDataProfileManager() {
        return mDataProfileManager;
    }

    /**
     * @return Data settings manager instance.
     */
    @NonNull
    public DataSettingsManager getDataSettingsManager() {
        return mDataSettingsManager;
    }

    /**
     * @return Data retry manager instance.
     */
    @NonNull
    public DataRetryManager getDataRetryManager() {
        return mDataRetryManager;
    }

    /**
     * @return The list of SubscriptionPlans
     */
    @VisibleForTesting
    @NonNull
    public List<SubscriptionPlan> getSubscriptionPlans() {
        return mSubscriptionPlans;
    }

    /**
     * @return The set of network types an unmetered override applies to
     */
    @VisibleForTesting
    @NonNull
    @NetworkType
    public Set<Integer> getUnmeteredOverrideNetworkTypes() {
        return mUnmeteredOverrideNetworkTypes;
    }

    /**
     * @return The set of network types a congested override applies to
     */
    @VisibleForTesting
    @NonNull
    @NetworkType
    public Set<Integer> getCongestedOverrideNetworkTypes() {
        return mCongestedOverrideNetworkTypes;
    }

    /**
     * Get data network type based on transport.
     *
     * @param transport The transport.
     * @return The current network type.
     */
    @NetworkType
    private int getDataNetworkType(@TransportType int transport) {
        NetworkRegistrationInfo nri = mServiceState.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, transport);
        if (nri != null) {
            return nri.getAccessNetworkTechnology();
        }
        return TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }

    /**
     * Get data registration state based on transport.
     *
     * @param ss The service state from which to extract the data registration state.
     * @param transport The transport.
     * @return The registration state.
     */
    @RegistrationState
    private int getDataRegistrationState(@NonNull ServiceState ss, @TransportType int transport) {
        NetworkRegistrationInfo nri = ss.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, transport);
        if (nri != null) {
            return nri.getRegistrationState();
        }
        return NetworkRegistrationInfo.REGISTRATION_STATE_UNKNOWN;
    }

    /**
     * @return The data activity. Note this is only updated when screen is on.
     */
    @DataActivityType
    public int getDataActivity() {
        return mDataActivity;
    }

    /**
     * Register data network controller callback.
     *
     * @param callback The callback.
     */
    public void registerDataNetworkControllerCallback(
            @NonNull DataNetworkControllerCallback callback) {
        sendMessage(obtainMessage(EVENT_REGISTER_DATA_NETWORK_CONTROLLER_CALLBACK, callback));
    }

    /**
     * Unregister data network controller callback.
     *
     * @param callback The callback.
     */
    public void unregisterDataNetworkControllerCallback(
            @NonNull DataNetworkControllerCallback callback) {
        sendMessage(obtainMessage(EVENT_UNREGISTER_DATA_NETWORK_CONTROLLER_CALLBACK, callback));
    }

    /**
     * Tear down all data networks.
     *
     * @param reason The reason to tear down.
     */
    public void tearDownAllDataNetworks(@TearDownReason int reason) {
        sendMessage(obtainMessage(EVENT_TEAR_DOWN_ALL_DATA_NETWORKS, reason, 0));
    }

    /**
     * Called when needed to tear down all data networks.
     *
     * @param reason The reason to tear down.
     */
    private void onTearDownAllDataNetworks(@TearDownReason int reason) {
        log("onTearDownAllDataNetworks: reason=" + DataNetwork.tearDownReasonToString(reason));
        if (mDataNetworkList.isEmpty()) {
            log("tearDownAllDataNetworks: No pending networks. All disconnected now.");
            return;
        }

        for (DataNetwork dataNetwork : mDataNetworkList) {
            if (!dataNetwork.isDisconnecting()) {
                tearDownGracefully(dataNetwork, reason);
            }
        }
    }

    /**
     * Evaluate the pending IMS de-registration networks and tear it down if it is safe to do that.
     */
    private void evaluatePendingImsDeregDataNetworks() {
        Iterator<Map.Entry<DataNetwork, Runnable>> it =
                mPendingImsDeregDataNetworks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataNetwork, Runnable> entry = it.next();
            if (isSafeToTearDown(entry.getKey())) {
                // Now tear down the network.
                log("evaluatePendingImsDeregDataNetworks: Safe to tear down data network "
                        + entry.getKey() + " now.");
                entry.getValue().run();
                it.remove();
            } else {
                log("Still not safe to tear down " + entry.getKey() + ".");
            }
        }
    }

    /**
     * Check if the data network is safe to tear down at this moment.
     *
     * @param dataNetwork The data network.
     * @return {@code true} if the data network is safe to tear down. {@code false} indicates this
     * data network has requests originated from the IMS/RCS service and IMS/RCS is not
     * de-registered yet.
     */
    private boolean isSafeToTearDown(@NonNull DataNetwork dataNetwork) {
        if (dataNetwork.hasNetworkCapabilityInNetworkRequests(
                NetworkCapabilities.NET_CAPABILITY_EIMS)) {
            // FWK currently doesn't track emergency registration state for graceful tear down.
            return true;
        }
        for (int imsFeature : SUPPORTED_IMS_FEATURES) {
            String imsFeaturePackage = mImsFeaturePackageName.get(imsFeature);
            if (imsFeaturePackage != null) {
                if (dataNetwork.getAttachedNetworkRequestList()
                        .hasNetworkRequestsFromPackage(imsFeaturePackage)) {
                    if (mRegisteredImsFeatures.contains(imsFeature)) {
                        return false;
                    }
                }
            }
        }
        // All IMS features are de-registered (or this data network has no requests from IMS feature
        // packages.
        return true;
    }

    /**
     * @return {@code true} if IMS graceful tear down is supported by frameworks.
     */
    private boolean isImsGracefulTearDownSupported() {
        return mDataConfigManager.getImsDeregistrationDelay() > 0;
    }

    /**
     * Tear down the data network gracefully.
     *
     * @param dataNetwork The data network.
     */
    private void tearDownGracefully(@NonNull DataNetwork dataNetwork, @TearDownReason int reason) {
        long deregDelay = mDataConfigManager.getImsDeregistrationDelay();
        if (isImsGracefulTearDownSupported() && !isSafeToTearDown(dataNetwork)) {
            log("tearDownGracefully: Not safe to tear down " + dataNetwork
                    + " at this point. Wait for IMS de-registration or timeout. MMTEL="
                    + (mRegisteredImsFeatures.contains(ImsFeature.FEATURE_MMTEL)
                    ? "registered" : "not registered")
                    + ", RCS="
                    + (mRegisteredImsFeatures.contains(ImsFeature.FEATURE_RCS)
                    ? "registered" : "not registered")
            );
            Runnable runnable = dataNetwork.tearDownWhenConditionMet(reason, deregDelay);
            if (runnable != null) {
                mPendingImsDeregDataNetworks.put(dataNetwork, runnable);
            } else {
                log(dataNetwork + " is being torn down already.");
            }
        } else {
            // Graceful tear down is not turned on. Tear down the network immediately.
            log("tearDownGracefully: Safe to tear down " + dataNetwork);
            dataNetwork.tearDown(reason);
        }
    }

    /**
     * Get the internet data network state. Note that this is the best effort if more than one
     * data network supports internet. For now only {@link TelephonyManager#DATA_CONNECTED},
     * {@link TelephonyManager#DATA_SUSPENDED}, and {@link TelephonyManager#DATA_DISCONNECTED}
     * are supported.
     *
     * @return The data network state.
     */
    @DataState
    public int getInternetDataNetworkState() {
        return mInternetDataNetworkState;
    }

    /**
     * @return List of bound data service packages name on WWAN and WLAN.
     */
    @NonNull
    public List<String> getDataServicePackages() {
        List<String> packages = new ArrayList<>();
        for (int i = 0; i < mDataServiceManagers.size(); i++) {
            packages.add(mDataServiceManagers.valueAt(i).getDataServicePackageName());
        }
        return packages;
    }

    /**
     * Request network validation.
     * <p>
     * Network validation request is sent to the DataNetwork that matches the network capability
     * in the list of DataNetwork owned by the DNC.
     *
     * @param capability network capability {@link NetCapability}
     */
    public void requestNetworkValidation(@NetCapability int capability,
            @NonNull Consumer<Integer> resultCodeCallback) {

        if (DataUtils.networkCapabilityToApnType(capability) == ApnSetting.TYPE_NONE) {
            // If the capability is not an apn type based capability, sent an invalid argument.
            loge("requestNetworkValidation: the capability is not an apn type based. capability:"
                    + capability);
            FunctionalUtils.ignoreRemoteException(resultCodeCallback::accept)
                    .accept(DataServiceCallback.RESULT_ERROR_INVALID_ARG);
            return;
        }

        // Find DataNetwork that matches the capability.
        List<DataNetwork> list = mDataNetworkList.stream()
                .filter(dataNetwork ->
                        dataNetwork.getNetworkCapabilities().hasCapability(capability))
                .toList();

        if (!list.isEmpty()) {
            // request network validation.
            list.forEach(dataNetwork -> dataNetwork.requestNetworkValidation(resultCodeCallback));
        } else {
            // If not found, sent an invalid argument.
            loge("requestNetworkValidation: No matching DataNetwork was found");
            FunctionalUtils.ignoreRemoteException(resultCodeCallback::accept)
                    .accept(DataServiceCallback.RESULT_ERROR_INVALID_ARG);
        }
    }

    /**
     * Log debug messages.
     * @param s debug messages
     */
    private void log(@NonNull String s) {
        Rlog.d(mLogTag, s);
    }

    /**
     * Log error messages.
     * @param s error messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(mLogTag, s);
    }

    /**
     * Log verbose messages.
     * @param s debug messages.
     */
    private void logv(@NonNull String s) {
        if (VDBG) Rlog.v(mLogTag, s);
    }

    /**
     * Log debug messages and also log into the local log.
     * @param s debug messages
     */
    private void logl(@NonNull String s) {
        log(s);
        mLocalLog.log(s);
    }

    /**
     * Dump the state of DataNetworkController
     *
     * @param fd File descriptor
     * @param printWriter Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(DataNetworkController.class.getSimpleName() + "-" + mPhone.getPhoneId() + ":");
        pw.increaseIndent();
        pw.println("Current data networks:");
        pw.increaseIndent();
        for (DataNetwork dn : mDataNetworkList) {
            dn.dump(fd, pw, args);
        }
        pw.decreaseIndent();

        pw.println("Pending tear down data networks:");
        pw.increaseIndent();
        for (DataNetwork dn : mPendingImsDeregDataNetworks.keySet()) {
            dn.dump(fd, pw, args);
        }
        pw.decreaseIndent();

        pw.println("Previously connected data networks: (up to "
                + MAX_HISTORICAL_CONNECTED_DATA_NETWORKS + ")");
        pw.increaseIndent();
        for (DataNetwork dn: mPreviousConnectedDataNetworkList) {
            // Do not print networks which is already in current network list.
            if (!mDataNetworkList.contains(dn)) {
                dn.dump(fd, pw, args);
            }
        }
        pw.decreaseIndent();

        pw.println("All telephony network requests:");
        pw.increaseIndent();
        for (TelephonyNetworkRequest networkRequest : mAllNetworkRequestList) {
            pw.println(networkRequest);
        }
        pw.decreaseIndent();

        pw.println("IMS features registration state: MMTEL="
                + (mRegisteredImsFeatures.contains(ImsFeature.FEATURE_MMTEL)
                ? "registered" : "not registered")
                + ", RCS="
                + (mRegisteredImsFeatures.contains(ImsFeature.FEATURE_RCS)
                ? "registered" : "not registered"));
        pw.println("mServiceState=" + mServiceState);
        pw.println("mPsRestricted=" + mPsRestricted);
        pw.println("mAnyDataNetworkExisting=" + mAnyDataNetworkExisting);
        pw.println("mInternetDataNetworkState="
                + TelephonyUtils.dataStateToString(mInternetDataNetworkState));
        pw.println("mImsDataNetworkState="
                + TelephonyUtils.dataStateToString(mImsDataNetworkState));
        pw.println("mDataServiceBound=" + mDataServiceBound);
        pw.println("mIsSrvccHandoverInProcess=" + mIsSrvccHandoverInProcess);
        pw.println("mSimState=" + TelephonyManager.simStateToString(mSimState));
        pw.println("mDataNetworkControllerCallbacks=" + mDataNetworkControllerCallbacks);
        pw.println("Subscription plans:");
        pw.increaseIndent();
        mSubscriptionPlans.forEach(pw::println);
        pw.decreaseIndent();
        pw.println("Unmetered override network types=" + mUnmeteredOverrideNetworkTypes.stream()
                .map(TelephonyManager::getNetworkTypeName).collect(Collectors.joining(",")));
        pw.println("Congested override network types=" + mCongestedOverrideNetworkTypes.stream()
                .map(TelephonyManager::getNetworkTypeName).collect(Collectors.joining(",")));
        pw.println("mImsThrottleCounter=" + mImsThrottleCounter);
        pw.println("mNetworkUnwantedCounter=" + mNetworkUnwantedCounter);
        pw.println("mBootStrapSimTotalDataUsageBytes=" + mBootStrapSimTotalDataUsageBytes);
        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();

        pw.println("-------------------------------------");
        mDataProfileManager.dump(fd, pw, args);
        pw.println("-------------------------------------");
        mDataRetryManager.dump(fd, pw, args);
        pw.println("-------------------------------------");
        mDataSettingsManager.dump(fd, pw, args);
        pw.println("-------------------------------------");
        mDataStallRecoveryManager.dump(fd, pw, args);
        pw.println("-------------------------------------");
        mDataConfigManager.dump(fd, pw, args);

        pw.decreaseIndent();
    }
}

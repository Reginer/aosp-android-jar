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

import static android.telephony.TelephonyManager.HAL_SERVICE_DATA;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.net.NetworkProvider;
import android.net.NetworkRequest;
import android.net.NetworkScore;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.TelephonyNetworkSpecifier;
import android.net.Uri;
import android.net.vcn.VcnManager;
import android.net.vcn.VcnManager.VcnNetworkPolicyChangeListener;
import android.net.vcn.VcnNetworkPolicyResult;
import android.os.AsyncResult;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Telephony;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.DataFailureCause;
import android.telephony.Annotation.DataState;
import android.telephony.Annotation.NetCapability;
import android.telephony.Annotation.NetworkType;
import android.telephony.Annotation.ValidationStatus;
import android.telephony.AnomalyReporter;
import android.telephony.CarrierConfigManager;
import android.telephony.DataFailCause;
import android.telephony.DataSpecificRegistrationInfo;
import android.telephony.LinkCapacityEstimate;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PcoData;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionPlan;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataCallResponse.HandoverFailureMode;
import android.telephony.data.DataCallResponse.LinkStatus;
import android.telephony.data.DataProfile;
import android.telephony.data.DataService;
import android.telephony.data.DataServiceCallback;
import android.telephony.data.NetworkSliceInfo;
import android.telephony.data.Qos;
import android.telephony.data.QosBearerSession;
import android.telephony.data.TrafficDescriptor;
import android.telephony.data.TrafficDescriptor.OsAppId;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.android.internal.telephony.CarrierSignalAgent;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.data.AccessNetworksManager.AccessNetworksManagerCallback;
import com.android.internal.telephony.data.DataConfigManager.DataConfigManagerCallback;
import com.android.internal.telephony.data.DataEvaluation.DataAllowedReason;
import com.android.internal.telephony.data.DataNetworkController.NetworkRequestList;
import com.android.internal.telephony.data.DataRetryManager.DataHandoverRetryEntry;
import com.android.internal.telephony.data.DataRetryManager.DataRetryEntry;
import com.android.internal.telephony.data.DataSettingsManager.DataSettingsManagerCallback;
import com.android.internal.telephony.data.LinkBandwidthEstimator.LinkBandwidthEstimatorCallback;
import com.android.internal.telephony.data.PhoneSwitcher.PhoneSwitcherCallback;
import com.android.internal.telephony.data.TelephonyNetworkAgent.TelephonyNetworkAgentCallback;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.metrics.DataCallSessionStats;
import com.android.internal.telephony.metrics.DataNetworkValidationStats;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.satellite.SatelliteController;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.net.module.util.LinkPropertiesUtils;
import com.android.net.module.util.NetUtils;
import com.android.net.module.util.NetworkCapabilitiesUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * DataNetwork class represents a single PDN (Packet Data Network).
 * <p>
 * The life cycle of a data network starts from {@link ConnectingState}. If setup data request
 * succeeds, then it enters {@link ConnectedState}, otherwise it enters
 * {@link DisconnectedState}.
 * <p>
 * When data network is in {@link ConnectingState}, it can enter {@link HandoverState} if handover
 * between IWLAN and cellular occurs. After handover completes or fails, it return back to
 * {@link ConnectedState}. When the data network is about to be disconnected, it first enters
 * {@link DisconnectingState} when performing graceful tear down or when sending the data
 * deactivation request. At the end, it enters {@link DisconnectedState} when {@link DataService}
 * notifies data disconnected. Note that an unsolicited disconnected event from {@link DataService}
 * or any vendor HAL failure response can immediately move data network from {@link ConnectedState}
 * to {@link DisconnectedState}. {@link DisconnectedState} is the final state of a data network.
 * <p>
 * State machine diagram:
 * <p>
 *
 *                                  ┌─────────┐
 *                                  │Handover │
 *                                  └─▲────┬──┘
 *                                    │    │
 *             ┌───────────┐        ┌─┴────▼──┐        ┌──────────────┐
 *             │Connecting ├────────►Connected├────────►Disconnecting │
 *             └─────┬─────┘        └────┬────┘        └───────┬──────┘
 *                   │                   │                     │
 *                   │             ┌─────▼──────┐              │
 *                   └─────────────►Disconnected◄──────────────┘
 *                                 └────────────┘
 *
 */
public class DataNetwork extends StateMachine {
    private static final boolean VDBG = false;
    /** Event for data config updated. */
    private static final int EVENT_DATA_CONFIG_UPDATED = 1;

    /** Event for attaching a network request. */
    private static final int EVENT_ATTACH_NETWORK_REQUEST = 2;

    /** Event for detaching a network request. */
    private static final int EVENT_DETACH_NETWORK_REQUEST = 3;

    /** Event when detect radio not available. */
    private static final int  EVENT_RADIO_NOT_AVAILABLE = 4;

    /** Event for allocating PDU session id response. */
    private static final int EVENT_ALLOCATE_PDU_SESSION_ID_RESPONSE = 5;

    /** Event for setup data network response. */
    private static final int EVENT_SETUP_DATA_NETWORK_RESPONSE = 6;

    /** Event for tearing down data network. */
    private static final int EVENT_TEAR_DOWN_NETWORK = 7;

    /** Event triggered by {@link DataServiceCallback#onDataCallListChanged(List)}. */
    private static final int EVENT_DATA_STATE_CHANGED = 8;

    /** Data network service state changed event. */
    private static final int EVENT_SERVICE_STATE_CHANGED = 9;

    /** Event for detaching all network requests. */
    private static final int EVENT_DETACH_ALL_NETWORK_REQUESTS = 10;

    /** Event for bandwidth estimation from the modem changed. */
    private static final int EVENT_BANDWIDTH_ESTIMATE_FROM_MODEM_CHANGED = 11;

    /** Event to report anomaly {@link #EVENT_NOTIFY_HANDOVER_CANCELLED_RESPONSE} not received. */
    private static final int EVENT_CANCEL_HANDOVER_NO_RESPONSE = 12;

    /** Event for display info changed. This is for getting 5G NSA or mmwave information. */
    private static final int EVENT_DISPLAY_INFO_CHANGED = 13;

    /** Event for setup data call (for handover) response from the data service. */
    private static final int EVENT_HANDOVER_RESPONSE = 15;

    /** Event for subscription plan changed or unmetered/congested override set. */
    private static final int EVENT_SUBSCRIPTION_PLAN_OVERRIDE = 16;

    /** Event for PCO data received from network. */
    private static final int EVENT_PCO_DATA_RECEIVED = 17;

    /** Event for carrier privileged UIDs changed. */
    private static final int EVENT_CARRIER_PRIVILEGED_UIDS_CHANGED = 18;

    /** Event for deactivate data network response. */
    private static final int EVENT_DEACTIVATE_DATA_NETWORK_RESPONSE = 19;

    /**
     * Event for data network stuck in transient (i.e. connecting/disconnecting/handover) state for
     * too long time. Timeout value specified in
     * {@link DataConfigManager#getAnomalyNetworkConnectingTimeoutMs()},
     * {@link DataConfigManager#getAnomalyNetworkDisconnectingTimeoutMs()},
     * {@link DataConfigManager#getNetworkHandoverTimeoutMs()}.
     */
    private static final int EVENT_STUCK_IN_TRANSIENT_STATE = 20;

    /**
     * Event for waiting for tearing down condition met. This will cause data network entering
     * disconnecting state.
     */
    private static final int EVENT_WAITING_FOR_TEARING_DOWN_CONDITION_MET = 21;

    /** Event for call started. */
    private static final int EVENT_VOICE_CALL_STARTED = 22;

    /** Event for call ended. */
    private static final int EVENT_VOICE_CALL_ENDED = 23;

    /** Event for CSS indicator changed. */
    private static final int EVENT_CSS_INDICATOR_CHANGED = 24;

    /**
     * Event for notifying source transport that handover is about to be initiated on target
     * transport.
     */
    private static final int EVENT_NOTIFY_HANDOVER_STARTED = 25;

    /**
     * Event for the response of notifying source transport that handover is about to be initiated
     * on target transport.
     */
    private static final int EVENT_NOTIFY_HANDOVER_STARTED_RESPONSE = 26;

    /**
     * Event for the response of notifying source transport that handover is cancelled/failed on the
     * target transport.
     */
    private static final int EVENT_NOTIFY_HANDOVER_CANCELLED_RESPONSE = 27;

    /** Event for data network validation request from the AccessNetworksManager. */
    private static final int EVENT_DATA_NETWORK_VALIDATION_REQUESTED = 28;

    /** Event for response to data network validation request. */
    private static final int EVENT_DATA_NETWORK_VALIDATION_RESPONSE = 29;

    /** Event for preferred data subscription changed. */
    private static final int EVENT_PREFERRED_DATA_SUBSCRIPTION_CHANGED = 30;

    /** Invalid context id. */
    private static final int INVALID_CID = -1;

    @IntDef(prefix = {"TEAR_DOWN_REASON_"},
            value = {
                    TEAR_DOWN_REASON_NONE,
                    TEAR_DOWN_REASON_CONNECTIVITY_SERVICE_UNWANTED,
                    TEAR_DOWN_REASON_SIM_REMOVAL,
                    TEAR_DOWN_REASON_AIRPLANE_MODE_ON,
                    TEAR_DOWN_REASON_DATA_DISABLED,
                    TEAR_DOWN_REASON_NO_LIVE_REQUEST,
                    TEAR_DOWN_REASON_RAT_NOT_ALLOWED,
                    TEAR_DOWN_REASON_ROAMING_DISABLED,
                    TEAR_DOWN_REASON_CONCURRENT_VOICE_DATA_NOT_ALLOWED,
                    TEAR_DOWN_REASON_SERVICE_OPTION_NOT_SUPPORTED,
                    TEAR_DOWN_REASON_DATA_SERVICE_NOT_READY,
                    TEAR_DOWN_REASON_POWER_OFF_BY_CARRIER,
                    TEAR_DOWN_REASON_DATA_STALL,
                    TEAR_DOWN_REASON_HANDOVER_FAILED,
                    TEAR_DOWN_REASON_HANDOVER_NOT_ALLOWED,
                    TEAR_DOWN_REASON_VCN_REQUESTED,
                    TEAR_DOWN_REASON_VOPS_NOT_SUPPORTED,
                    TEAR_DOWN_REASON_DEFAULT_DATA_UNSELECTED,
                    TEAR_DOWN_REASON_NOT_IN_SERVICE,
                    TEAR_DOWN_REASON_DATA_CONFIG_NOT_READY,
                    TEAR_DOWN_REASON_PENDING_TEAR_DOWN_ALL,
                    TEAR_DOWN_REASON_NO_SUITABLE_DATA_PROFILE,
                    TEAR_DOWN_REASON_CDMA_EMERGENCY_CALLBACK_MODE,
                    TEAR_DOWN_REASON_RETRY_SCHEDULED,
                    TEAR_DOWN_REASON_DATA_THROTTLED,
                    TEAR_DOWN_REASON_DATA_PROFILE_INVALID,
                    TEAR_DOWN_REASON_DATA_PROFILE_NOT_PREFERRED,
                    TEAR_DOWN_REASON_NOT_ALLOWED_BY_POLICY,
                    TEAR_DOWN_REASON_ILLEGAL_STATE,
                    TEAR_DOWN_REASON_ONLY_ALLOWED_SINGLE_NETWORK,
                    TEAR_DOWN_REASON_PREFERRED_DATA_SWITCHED,
                    TEAR_DOWN_REASON_DATA_LIMIT_REACHED,
                    TEAR_DOWN_REASON_DATA_NETWORK_TRANSPORT_NOT_ALLOWED,
            })
    public @interface TearDownReason {}

    /** Data network was not torn down. */
    public static final int TEAR_DOWN_REASON_NONE = 0;

    /** Data network tear down requested by connectivity service. */
    public static final int TEAR_DOWN_REASON_CONNECTIVITY_SERVICE_UNWANTED = 1;

    /** Data network tear down due to SIM removal. */
    public static final int TEAR_DOWN_REASON_SIM_REMOVAL = 2;

    /** Data network tear down due to airplane mode turned on. */
    public static final int TEAR_DOWN_REASON_AIRPLANE_MODE_ON = 3;

    /** Data network tear down due to data disabled (by user, policy, carrier, etc...). */
    public static final int TEAR_DOWN_REASON_DATA_DISABLED = 4;

    /** Data network tear down due to no live network request. */
    public static final int TEAR_DOWN_REASON_NO_LIVE_REQUEST = 5;

    /** Data network tear down due to current RAT is not allowed by the data profile. */
    public static final int TEAR_DOWN_REASON_RAT_NOT_ALLOWED = 6;

    /** Data network tear down due to data roaming not enabled. */
    public static final int TEAR_DOWN_REASON_ROAMING_DISABLED = 7;

    /** Data network tear down due to concurrent voice/data not allowed. */
    public static final int TEAR_DOWN_REASON_CONCURRENT_VOICE_DATA_NOT_ALLOWED = 8;

    /** Data network tear down due to service option is not supported. */
    public static final int TEAR_DOWN_REASON_SERVICE_OPTION_NOT_SUPPORTED = 9;

    /** Data network tear down due to data service unbound. */
    public static final int TEAR_DOWN_REASON_DATA_SERVICE_NOT_READY = 10;

    /** Data network tear down due to radio turned off by the carrier. */
    public static final int TEAR_DOWN_REASON_POWER_OFF_BY_CARRIER = 11;

    /** Data network tear down due to data stall. */
    public static final int TEAR_DOWN_REASON_DATA_STALL = 12;

    /** Data network tear down due to handover failed. */
    public static final int TEAR_DOWN_REASON_HANDOVER_FAILED = 13;

    /** Data network tear down due to handover not allowed. */
    public static final int TEAR_DOWN_REASON_HANDOVER_NOT_ALLOWED = 14;

    /** Data network tear down due to VCN service requested. */
    public static final int TEAR_DOWN_REASON_VCN_REQUESTED = 15;

    /** Data network tear down due to VOPS no longer supported. */
    public static final int TEAR_DOWN_REASON_VOPS_NOT_SUPPORTED = 16;

    /** Data network tear down due to default data unselected. */
    public static final int TEAR_DOWN_REASON_DEFAULT_DATA_UNSELECTED = 17;

    /** Data network tear down due to device not in service. */
    public static final int TEAR_DOWN_REASON_NOT_IN_SERVICE = 18;

    /** Data network tear down due to data config not ready. */
    public static final int TEAR_DOWN_REASON_DATA_CONFIG_NOT_READY = 19;

    /** Data network tear down due to tear down all pending. */
    public static final int TEAR_DOWN_REASON_PENDING_TEAR_DOWN_ALL = 20;

    /** Data network tear down due to no suitable data profile. */
    public static final int TEAR_DOWN_REASON_NO_SUITABLE_DATA_PROFILE = 21;

    /** Data network tear down due to CDMA ECBM. */
    public static final int TEAR_DOWN_REASON_CDMA_EMERGENCY_CALLBACK_MODE = 22;

    /** Data network tear down due to retry scheduled. */
    public static final int TEAR_DOWN_REASON_RETRY_SCHEDULED = 23;

    /** Data network tear down due to data throttled. */
    public static final int TEAR_DOWN_REASON_DATA_THROTTLED = 24;

    /** Data network tear down due to data profile invalid. */
    public static final int TEAR_DOWN_REASON_DATA_PROFILE_INVALID = 25;

    /** Data network tear down due to data profile not preferred. */
    public static final int TEAR_DOWN_REASON_DATA_PROFILE_NOT_PREFERRED = 26;

    /** Data network tear down due to handover not allowed by policy. */
    public static final int TEAR_DOWN_REASON_NOT_ALLOWED_BY_POLICY = 27;

    /** Data network tear down due to illegal state. */
    public static final int TEAR_DOWN_REASON_ILLEGAL_STATE = 28;

    /** Data network tear down due to only allowed single network. */
    public static final int TEAR_DOWN_REASON_ONLY_ALLOWED_SINGLE_NETWORK = 29;

    /** Data network tear down due to preferred data switched to another phone. */
    public static final int TEAR_DOWN_REASON_PREFERRED_DATA_SWITCHED = 30;

    /** Data network tear down due to bootstrap sim data limit reached. */
    public static final int TEAR_DOWN_REASON_DATA_LIMIT_REACHED = 31;

    /** Data network tear down due to current data network transport mismatch. */
    public static final int TEAR_DOWN_REASON_DATA_NETWORK_TRANSPORT_NOT_ALLOWED = 32;

    //********************************************************************************************//
    // WHENEVER ADD A NEW TEAR DOWN REASON, PLEASE UPDATE DataDeactivateReasonEnum in enums.proto //
    //********************************************************************************************//

    @IntDef(prefix = {"BANDWIDTH_SOURCE_"},
            value = {
                    BANDWIDTH_SOURCE_UNKNOWN,
                    BANDWIDTH_SOURCE_MODEM,
                    BANDWIDTH_SOURCE_CARRIER_CONFIG,
                    BANDWIDTH_SOURCE_BANDWIDTH_ESTIMATOR,
            })
    public @interface BandwidthEstimationSource {}

    /** Indicates the bandwidth estimation source is unknown. This must be a configuration error. */
    public static final int BANDWIDTH_SOURCE_UNKNOWN = 0;

    /** Indicates the bandwidth estimation source is from the modem. */
    public static final int BANDWIDTH_SOURCE_MODEM = 1;

    /** Indicates the bandwidth estimation source is from the static carrier config. */
    public static final int BANDWIDTH_SOURCE_CARRIER_CONFIG = 2;

    /** Indicates the bandwidth estimation source is from {@link LinkBandwidthEstimator}. */
    public static final int BANDWIDTH_SOURCE_BANDWIDTH_ESTIMATOR = 3;

    /**
     * The capabilities that are allowed to changed dynamically during the life cycle of network.
     * This is copied from {@code NetworkCapabilities#MUTABLE_CAPABILITIES}. There is no plan to
     * make this a connectivity manager API since in the future, immutable network capabilities
     * would be allowed to changed dynamically. (i.e. not immutable anymore.)
     */
    private static final List<Integer> MUTABLE_CAPABILITIES = List.of(
            NetworkCapabilities.NET_CAPABILITY_TRUSTED,
            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
            NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL,
            NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING,
            NetworkCapabilities.NET_CAPABILITY_FOREGROUND,
            NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED,
            NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED,
            NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY,
            NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED,
            NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED,
            NetworkCapabilities.NET_CAPABILITY_HEAD_UNIT,
            // Connectivity service will support NOT_METERED as a mutable and requestable
            // capability.
            NetworkCapabilities.NET_CAPABILITY_NOT_METERED,
            // Dynamically add and remove MMTEL capability when network transition between VoPS
            // and non-VoPS network if the request is not MMTEL. For MMTEL, we retain the capability
            // to prevent immediate tear down.
            NetworkCapabilities.NET_CAPABILITY_MMTEL,
            // Dynamically add and remove MMS capability depending on QNS's preference if there is
            // a transport specific APN alternative.
            NetworkCapabilities.NET_CAPABILITY_MMS
    );

    /** The parent state. Any messages not handled by the child state fallback to this. */
    private final DefaultState mDefaultState = new DefaultState();

    /**
     * The connecting state. This is the initial state of a data network.
     *
     * @see DataNetwork for the state machine diagram.
     */
    private final ConnectingState mConnectingState = new ConnectingState();

    /**
     * The connected state. This is the state when data network becomes usable.
     *
     * @see DataNetwork for the state machine diagram.
     */
    private final ConnectedState mConnectedState = new ConnectedState();

    /**
     * The handover state. This is the state when data network handover between IWLAN and cellular.
     *
     * @see DataNetwork for the state machine diagram.
     */
    private final HandoverState mHandoverState = new HandoverState();

    /**
     * The disconnecting state. This is the state when data network is about to be disconnected.
     * The network is still usable in this state, but the clients should be prepared to lose the
     * network in any moment. This state is particular useful for IMS graceful tear down, where
     * the network enters disconnecting state while waiting for IMS de-registration signal.
     *
     * @see DataNetwork for the state machine diagram.
     */
    private final DisconnectingState mDisconnectingState = new DisconnectingState();

    /**
     * The disconnected state. This is the final state of a data network.
     *
     * @see DataNetwork for the state machine diagram.
     */
    private final DisconnectedState mDisconnectedState = new DisconnectedState();

    /** The phone instance. */
    @NonNull
    private final Phone mPhone;

    /** Feature flags */
    @NonNull
    private final FeatureFlags mFlags;

    /**
     * The subscription id. This is assigned when the network is created, and not supposed to
     * change afterwards.
     */
    private final int mSubId;

    /** The network score of this network. */
    @NonNull
    private NetworkScore mNetworkScore;

    /**
     * Indicates that
     * {@link DataService.DataServiceProvider#deactivateDataCall(int, int, DataServiceCallback)}
     * has been called. This flag can be only changed from {@code false} to {@code true}.
     */
    private boolean mInvokedDataDeactivation = false;

    /**
     * Indicates that if the data network has ever entered {@link ConnectedState}.
     */
    private boolean mEverConnected = false;

    /** RIL interface. */
    @NonNull
    private final CommandsInterface mRil;

    /** Local log. */
    private final LocalLog mLocalLog = new LocalLog(128);

    /** The callback to receives data network state update. */
    @NonNull
    private final DataNetworkCallback mDataNetworkCallback;

    /** The log tag. */
    private String mLogTag;

    /** Metrics of per data network connection. */
    private final DataCallSessionStats mDataCallSessionStats;

    /** Metrics of per data network validation. */
    @NonNull
    private final DataNetworkValidationStats mDataNetworkValidationStats;

    /**
     * The unique context id assigned by the data service in {@link DataCallResponse#getId()}. One
     * for {@link AccessNetworkConstants#TRANSPORT_TYPE_WWAN} and one for
     * {@link AccessNetworkConstants#TRANSPORT_TYPE_WLAN}. The reason for storing both is that
     * during handover, both cid will be used.
     */
    private final SparseIntArray mCid = new SparseIntArray(2);

    /**
     * The initial network agent id. The network agent can be re-created due to immutable capability
     * changed. This is to preserve the initial network agent id so the id in the logging tag won't
     * change for the entire life cycle of data network.
     */
    private int mInitialNetworkAgentId;

    /** PDU session id. */
    private int mPduSessionId = DataCallResponse.PDU_SESSION_ID_NOT_SET;

    /**
     * Data service managers for accessing {@link AccessNetworkConstants#TRANSPORT_TYPE_WWAN} and
     * {@link AccessNetworkConstants#TRANSPORT_TYPE_WLAN} data services.
     */
    @NonNull
    private final SparseArray<DataServiceManager> mDataServiceManagers;

    /** Access networks manager. */
    @NonNull
    private final AccessNetworksManager mAccessNetworksManager;

    /** Data network controller. */
    @NonNull
    private final DataNetworkController mDataNetworkController;

    /** Phone switcher which is responsible to determine which phone to route network request. */
    @NonNull
    private final PhoneSwitcher mPhoneSwitcher;

    /** Data network controller callback. */
    @NonNull
    private final DataNetworkController.DataNetworkControllerCallback
            mDataNetworkControllerCallback;

    /** Data settings manager callback. */
    @NonNull
    private DataSettingsManagerCallback mDataSettingsManagerCallback;

    /** Data config manager. */
    @NonNull
    private final DataConfigManager mDataConfigManager;

    /** VCN manager. */
    @Nullable
    private final VcnManager mVcnManager;

    /** VCN policy changed listener. */
    @Nullable
    private VcnNetworkPolicyChangeListener mVcnPolicyChangeListener;

    /** The network agent associated with this data network. */
    @NonNull
    private TelephonyNetworkAgent mNetworkAgent;

    /** QOS callback tracker. This is only created after network connected on WWAN. */
    @Nullable
    private QosCallbackTracker mQosCallbackTracker;

    /** NAT keepalive tracker. */
    @Nullable
    private KeepaliveTracker mKeepaliveTracker;

    /** The data profile used to establish this data network. */
    @NonNull
    private DataProfile mDataProfile;

    /**
     * The data profile used for data handover. Some carriers might use different data profile
     * between IWLAN and cellular. Only set before handover started.
     */
    @Nullable
    private DataProfile mHandoverDataProfile;

    /** The network capabilities of this data network. */
    @NonNull
    private NetworkCapabilities mNetworkCapabilities;

    /** The matched traffic descriptor returned from setup data call request. */
    @NonNull
    private final List<TrafficDescriptor> mTrafficDescriptors = new ArrayList<>();

    /** The link properties of this data network. */
    @NonNull
    private LinkProperties mLinkProperties;

    /** The network slice info. */
    @Nullable
    private NetworkSliceInfo mNetworkSliceInfo;

    /** The link status (i.e. RRC state). */
    @LinkStatus
    private int mLinkStatus = DataCallResponse.LINK_STATUS_UNKNOWN;

    /** The network bandwidth. */
    @NonNull
    private NetworkBandwidth mNetworkBandwidth = new NetworkBandwidth(14, 14);

    /** The TCP buffer sizes config. */
    @NonNull
    private String mTcpBufferSizes;

    /** The telephony display info. */
    @NonNull
    private TelephonyDisplayInfo mTelephonyDisplayInfo;

    /** Whether {@link NetworkCapabilities#NET_CAPABILITY_TEMPORARILY_NOT_METERED} is supported. */
    private boolean mTempNotMeteredSupported = false;

    /** Whether the current data network is temporarily not metered. */
    private boolean mTempNotMetered = false;

    /** Whether the current data network is congested. */
    private boolean mCongested = false;

    /**
     * Whether the current data network is on preferred data modem.
     *
     * @see PhoneSwitcher#getPreferredDataPhoneId()
     */
    private boolean mOnPreferredDataPhone;

    /** The network requests associated with this data network */
    @NonNull
    private final NetworkRequestList mAttachedNetworkRequestList =
            new NetworkRequestList();

    /**
     * The latest data call response received from either
     * {@link DataServiceCallback#onSetupDataCallComplete(int, DataCallResponse)} or
     * {@link DataServiceCallback#onDataCallListChanged(List)}. The very first update must be
     * from {@link DataServiceCallback#onSetupDataCallComplete(int, DataCallResponse)}.
     */
    @Nullable
    private DataCallResponse mDataCallResponse = null;

    /**
     * The fail cause from either setup data failure or unsolicited disconnect reported by data
     * service.
     */
    @DataFailureCause
    private int mFailCause = DataFailCause.NONE;

    /**
     * The tear down reason if the data call is voluntarily deactivated, not due to failure.
     */
    @TearDownReason
    private int mTearDownReason = TEAR_DOWN_REASON_NONE;

    /**
     * The retry delay in milliseconds from setup data failure.
     */
    private long mRetryDelayMillis = DataCallResponse.RETRY_DURATION_UNDEFINED;

    /**
     * Indicates if data network is suspended. Note this is slightly different from the
     * {@link TelephonyManager#DATA_SUSPENDED}, which is only possible when data network is in
     * connected state. This flag reflects to the
     * {@link NetworkCapabilities#NET_CAPABILITY_NOT_SUSPENDED} which can happen when data network
     * is in connected or disconnecting state.
     */
    private boolean mSuspended = false;

    /**
     * The current transport of the data network. For handover, the current transport will be set
     * after handover completes.
     */
    @TransportType
    private int mTransport;

    /**
     * The last known data network type.
     */
    @NetworkType
    private int mLastKnownDataNetworkType;

    /**
     * The last known roaming state of this data network.
     */
    private boolean mLastKnownRoamingState;

    /**
     * The non-terrestrial status
     */
    private final boolean mIsSatellite;

    /** The reason that why setting up this data network is allowed. */
    @NonNull
    private final DataAllowedReason mDataAllowedReason;

    /**
     * PCO (Protocol Configuration Options) data received from the network. The first key is the
     * cid of the PCO data, the second key is the PCO id, the value is the PCO data.
     */
    @NonNull
    private final Map<Integer, Map<Integer, PcoData>> mPcoData = new ArrayMap<>();

    /** The QOS bearer sessions. */
    @NonNull
    private final List<QosBearerSession> mQosBearerSessions = new ArrayList<>();

    /** The QOS for the Default Bearer, should be non-null on LTE and NR */
    @Nullable
    private Qos mDefaultQos;

    /**
     * The UIDs of packages that have carrier privilege.
     */
    @NonNull
    private int[] mAdministratorUids = new int[0];

    /** Carrier privileges callback to monitor administrator UID change. */
    @Nullable
    private TelephonyManager.CarrierPrivilegesCallback mCarrierPrivilegesCallback;

    /**
     * Carrier service package uid. This UID will not change through the life cycle of data network.
     */
    private int mCarrierServicePackageUid = Process.INVALID_UID;

    /**
     * Link bandwidth estimator callback for receiving latest link bandwidth information.
     */
    @Nullable
    private LinkBandwidthEstimatorCallback mLinkBandwidthEstimatorCallback;

    /**
     * Data config callback for carrier config update.
     */
    @Nullable
    private DataConfigManagerCallback mDataConfigManagerCallback;

    /**
     * Network validation status for this data network. If the data service provider does not
     * support the network validation feature, should be UNSUPPORTED.
     */
    @PreciseDataConnectionState.NetworkValidationStatus
    private int mNetworkValidationStatus =
            PreciseDataConnectionState.NETWORK_VALIDATION_UNSUPPORTED;

    /**
     * Callback used to respond to a network validation request to determine whether the request is
     * successfully submitted. If the request has been submitted, change it to null.
     */
    @Nullable
    private Consumer<Integer> mNetworkValidationResultCodeCallback;

    /**
     * Callback used to listen QNS preference changes.
     */
    @Nullable
    private AccessNetworksManagerCallback mAccessNetworksManagerCallback;

    /**
     * PreciseDataConnectionState, the most recently notified. If it has never been notified, it is
     * null.
     */
    @Nullable
    private PreciseDataConnectionState mPreciseDataConnectionState;

    /**
     * Callback to listen event from {@link PhoneSwitcher}.
     */
    @NonNull
    private PhoneSwitcherCallback mPhoneSwitcherCallback;

    /**
     * The network bandwidth.
     */
    public static class NetworkBandwidth {
        /** The downlink bandwidth in Kbps. */
        public final int downlinkBandwidthKbps;

        /** The uplink Bandwidth in Kbps. */
        public final int uplinkBandwidthKbps;

        /**
         * Constructor.
         *
         * @param downlinkBandwidthKbps The downlink bandwidth in Kbps.
         * @param uplinkBandwidthKbps The uplink Bandwidth in Kbps.
         */
        public NetworkBandwidth(int downlinkBandwidthKbps, int uplinkBandwidthKbps) {
            this.downlinkBandwidthKbps = downlinkBandwidthKbps;
            this.uplinkBandwidthKbps = uplinkBandwidthKbps;
        }

        @Override
        public String toString() {
            return String.format("NetworkBandwidth=[downlink=%d, uplink=%d]",
                    downlinkBandwidthKbps, uplinkBandwidthKbps);
        }
    }

    /**
     * Data network callback. Should only be used by {@link DataNetworkController}.
     */
    public abstract static class DataNetworkCallback extends DataCallback {
        /**
         * Constructor
         *
         * @param executor The executor of the callback.
         */
        public DataNetworkCallback(@NonNull @CallbackExecutor Executor executor) {
            super(executor);
        }

        /**
         * Called when data setup failed.
         *
         * @param dataNetwork The data network.
         * @param requestList The network requests attached to this data network.
         * @param cause The fail cause of setup data network.
         * @param retryDurationMillis The network suggested data retry duration in milliseconds as
         * specified in 3GPP TS 24.302 section 8.2.9.1. The {@link DataProfile} associated to this
         * data network will be throttled for the specified duration unless
         * {@link DataServiceCallback#onApnUnthrottled} is called. {@link Long#MAX_VALUE} indicates
         * data retry should not occur. {@link DataCallResponse#RETRY_DURATION_UNDEFINED} indicates
         * network did not suggest any retry duration.
         */
        public abstract void onSetupDataFailed(@NonNull DataNetwork dataNetwork,
                @NonNull NetworkRequestList requestList, @DataFailureCause int cause,
                long retryDurationMillis);

        /**
         * Called when data network enters {@link ConnectedState}.
         *
         * @param dataNetwork The data network.
         */
        public abstract void onConnected(@NonNull DataNetwork dataNetwork);

        /**
         * Called when data network validation status changed.
         *
         * @param dataNetwork The data network.
         * @param status one of {@link NetworkAgent#VALIDATION_STATUS_VALID} or
         * {@link NetworkAgent#VALIDATION_STATUS_NOT_VALID}.
         * @param redirectUri If internet connectivity is being redirected (e.g., on a captive
         * portal), this is the destination the probes are being redirected to, otherwise
         * {@code null}.
         */
        public abstract void onValidationStatusChanged(@NonNull DataNetwork dataNetwork,
                @ValidationStatus int status, @Nullable Uri redirectUri);

        /**
         * Called when data network suspended state changed.
         *
         * @param dataNetwork The data network.
         * @param suspended {@code true} if data is suspended.
         */
        public abstract void onSuspendedStateChanged(@NonNull DataNetwork dataNetwork,
                boolean suspended);

        /**
         * Called when network requests were failed to attach to the data network.
         *
         * @param dataNetwork The data network.
         * @param requestList The requests failed to attach.
         */
        public abstract void onAttachFailed(@NonNull DataNetwork dataNetwork,
                @NonNull NetworkRequestList requestList);

        /**
         * Called when data network enters {@link DisconnectedState}. Note this is only called
         * when the data network was previously connected. For setup data network failed,
         * {@link #onSetupDataFailed(DataNetwork, NetworkRequestList, int, long)} is called.
         *
         * @param dataNetwork The data network.
         * @param cause The disconnect cause.
         * @param tearDownReason The reason the network was torn down
         */
        public abstract void onDisconnected(@NonNull DataNetwork dataNetwork,
                @DataFailureCause int cause, @TearDownReason int tearDownReason);

        /**
         * Called when handover between IWLAN and cellular network succeeded.
         *
         * @param dataNetwork The data network.
         */
        public abstract void onHandoverSucceeded(@NonNull DataNetwork dataNetwork);

        /**
         * Called when data network handover between IWLAN and cellular network failed.
         *
         * @param dataNetwork The data network.
         * @param cause The fail cause.
         * @param retryDurationMillis Network suggested retry time in milliseconds.
         * {@link Long#MAX_VALUE} indicates data retry should not occur.
         * {@link DataCallResponse#RETRY_DURATION_UNDEFINED} indicates network did not suggest any
         * retry duration.
         * @param handoverFailureMode The handover failure mode that determine the behavior of
         * how frameworks should handle the handover failure.
         */
        public abstract void onHandoverFailed(@NonNull DataNetwork dataNetwork,
                @DataFailureCause int cause, long retryDurationMillis,
                @HandoverFailureMode int handoverFailureMode);

        /**
         * Called when data network link status (i.e. RRC state) changed.
         *
         * @param dataNetwork The data network.
         * @param linkStatus The link status (i.e. RRC state).
         */
        public abstract void onLinkStatusChanged(@NonNull DataNetwork dataNetwork,
                @LinkStatus int linkStatus);

        /**
         * Called when PCO data changed.
         *
         * @param dataNetwork The data network.
         */
        public abstract void onPcoDataChanged(@NonNull DataNetwork dataNetwork);

        /**
         * Called when network capabilities changed.
         *
         * @param dataNetwork The data network.
         */
        public abstract void onNetworkCapabilitiesChanged(@NonNull DataNetwork dataNetwork);

        /**
         * Called when attempt to tear down a data network
         *
         * @param dataNetwork The data network.
         */
        public abstract void onTrackNetworkUnwanted(@NonNull DataNetwork dataNetwork);

        /**
         * Called when a network request is detached after no longer satisfied.
         *
         * @param networkRequest The detached network request.
         */
        public abstract void onRetryUnsatisfiedNetworkRequest(
                @NonNull TelephonyNetworkRequest networkRequest);

        /**
         * Called when QosBearerSessions bearer changed, which indicates VoNr or VoLte calls.
         *
         * @param qosBearerSessions The current qosBearerSessions.
         */
        public abstract void onQosSessionsChanged(
                @NonNull List<QosBearerSession> qosBearerSessions);
    }

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param looper The looper to be used by the state machine. Currently the handler thread is the
     * phone process's main thread.
     * @param dataServiceManagers Data service managers.
     * @param dataProfile The data profile for establishing the data network.
     * @param networkRequestList The initial network requests attached to this data network.
     * @param transport The initial transport of the data network.
     * @param dataAllowedReason The reason that why setting up this data network is allowed.
     * @param callback The callback to receives data network state update.
     */
    public DataNetwork(@NonNull Phone phone, FeatureFlags featureFlags, @NonNull Looper looper,
            @NonNull SparseArray<DataServiceManager> dataServiceManagers,
            @NonNull DataProfile dataProfile,
            @NonNull NetworkRequestList networkRequestList,
            @TransportType int transport,
            @NonNull DataAllowedReason dataAllowedReason,
            @NonNull DataNetworkCallback callback) {
        super("DataNetwork", looper);
        // State machine should be initialized at the top of constructor. log() can be only used
        // after state machine initialized (because getCurrentState() crashes if state machine has
        // not started.)
        initializeStateMachine();

        mPhone = phone;
        mFlags = featureFlags;
        mSubId = phone.getSubId();
        mRil = mPhone.mCi;
        mLinkProperties = new LinkProperties();
        mDataServiceManagers = dataServiceManagers;
        mAccessNetworksManager = phone.getAccessNetworksManager();
        mVcnManager = mPhone.getContext().getSystemService(VcnManager.class);
        mDataNetworkController = phone.getDataNetworkController();
        mPhoneSwitcher = PhoneSwitcher.getInstance();
        mOnPreferredDataPhone = phone.getPhoneId() == mPhoneSwitcher.getPreferredDataPhoneId();
        mDataNetworkControllerCallback = new DataNetworkController.DataNetworkControllerCallback(
                getHandler()::post) {
            @Override
            public void onSubscriptionPlanOverride() {
                sendMessage(EVENT_SUBSCRIPTION_PLAN_OVERRIDE);
            }};
        mDataNetworkController.registerDataNetworkControllerCallback(
                mDataNetworkControllerCallback);
        mDataConfigManager = mDataNetworkController.getDataConfigManager();
        mDataCallSessionStats = new DataCallSessionStats(mPhone);
        mDataNetworkValidationStats = new DataNetworkValidationStats(mPhone);
        mDataNetworkCallback = callback;
        mDataProfile = dataProfile;
        if (dataProfile.getTrafficDescriptor() != null) {
            // The initial traffic descriptor is from the data profile. After that traffic
            // descriptors will be updated by modem through setup data call response and data call
            // list changed event.
            mTrafficDescriptors.add(dataProfile.getTrafficDescriptor());
        }
        mTransport = transport;
        mLastKnownDataNetworkType = getDataNetworkType();
        mLastKnownRoamingState = mPhone.getServiceState().getDataRoamingFromRegistration();
        mIsSatellite = mPhone.getServiceState().isUsingNonTerrestrialNetwork()
                && transport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
        mDataAllowedReason = dataAllowedReason;
        dataProfile.setLastSetupTimestamp(SystemClock.elapsedRealtime());
        for (int transportType : mAccessNetworksManager.getAvailableTransports()) {
            mCid.put(transportType, INVALID_CID);
        }
        mTelephonyDisplayInfo = mPhone.getDisplayInfoController().getTelephonyDisplayInfo();
        mTcpBufferSizes = mDataConfigManager.getTcpConfigString(mTelephonyDisplayInfo);

        // network capabilities infer connectivity transport and MMTEL from the requested
        // capabilities.
        // TODO: Ideally we shouldn't infer network capabilities base on the requested capabilities,
        // but currently there are 2 hacks associated with getForcedCellularTransportCapabilities
        // and IMS service requesting IMS|MMTEL that need to support. When we stop supporting these
        // cases, we shouldn't consider the requests when determining the network capabilities.
        mAttachedNetworkRequestList.addAll(networkRequestList);
        // Update the capabilities in the constructor is to make sure the data network has initial
        // capability immediately after created. Doing this connecting state creates the window that
        // DataNetworkController might check if existing data network's capability can satisfy the
        // next network request within this window.
        updateNetworkCapabilities();

        // Remove the requests that can't use the initial capabilities
        ListIterator<TelephonyNetworkRequest> iter = mAttachedNetworkRequestList.listIterator();
        while (iter.hasNext()) {
            TelephonyNetworkRequest request = iter.next();
            if (request.canBeSatisfiedBy(mNetworkCapabilities)) {
                request.setAttachedNetwork(DataNetwork.this);
                request.setState(TelephonyNetworkRequest.REQUEST_STATE_SATISFIED);
            } else {
                iter.remove();
            }
        }
    }

    /**
     * Initialize and start the state machine.
     */
    private void initializeStateMachine() {
        addState(mDefaultState);
        addState(mConnectingState, mDefaultState);
        addState(mConnectedState, mDefaultState);
        addState(mHandoverState, mDefaultState);
        addState(mDisconnectingState, mDefaultState);
        addState(mDisconnectedState, mDefaultState);
        setInitialState(mConnectingState);
        start();
    }

    /**
     * @return {@code true} if 464xlat should be skipped.
     */
    private boolean shouldSkip464Xlat() {
        if (mDataProfile.getApnSetting() != null) {
            switch (mDataProfile.getApnSetting().getSkip464Xlat()) {
                case Telephony.Carriers.SKIP_464XLAT_ENABLE:
                    return true;
                case Telephony.Carriers.SKIP_464XLAT_DISABLE:
                    return false;
                case Telephony.Carriers.SKIP_464XLAT_DEFAULT:
                default:
                    break;
            }
        }

        // As default, return true if ims and no internet
        final NetworkCapabilities nc = getNetworkCapabilities();
        return nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_IMS)
                && !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * Create the telephony network agent.
     *
     * @return The telephony network agent.
     */
    @NonNull
    private TelephonyNetworkAgent createNetworkAgent() {
        final NetworkAgentConfig.Builder configBuilder = new NetworkAgentConfig.Builder();
        configBuilder.setLegacyType(ConnectivityManager.TYPE_MOBILE);
        configBuilder.setLegacyTypeName("MOBILE");
        int networkType = getDataNetworkType();
        configBuilder.setLegacySubType(networkType);
        configBuilder.setLegacySubTypeName(TelephonyManager.getNetworkTypeName(networkType));
        if (mDataProfile.getApnSetting() != null) {
            configBuilder.setLegacyExtraInfo(mDataProfile.getApnSetting().getApnName());
        }

        final CarrierSignalAgent carrierSignalAgent = mPhone.getCarrierSignalAgent();
        if (carrierSignalAgent.hasRegisteredReceivers(TelephonyManager
                .ACTION_CARRIER_SIGNAL_REDIRECTED)) {
            // carrierSignal Receivers will place the carrier-specific provisioning notification
            configBuilder.setProvisioningNotificationEnabled(false);
        }

        // Fill the IMSI
        final String subscriberId = mPhone.getSubscriberId();
        if (!TextUtils.isEmpty(subscriberId)) {
            configBuilder.setSubscriberId(subscriberId);
        }

        // set skip464xlat if it is not default otherwise
        if (shouldSkip464Xlat()) {
            configBuilder.setNat64DetectionEnabled(false);
        }

        NetworkProvider provider;
        provider = PhoneFactory.getNetworkProvider();

        NetworkScore.Builder builder = new NetworkScore.Builder()
                .setKeepConnectedReason(isHandoverInProgress()
                        ? NetworkScore.KEEP_CONNECTED_FOR_HANDOVER
                        : NetworkScore.KEEP_CONNECTED_NONE);
        builder.setTransportPrimary(mOnPreferredDataPhone);
        mNetworkScore = builder.build();
        logl("mNetworkScore: isPrimary=" + mNetworkScore.isTransportPrimary()
                + ", keepConnectedReason=" + mNetworkScore.getKeepConnectedReason());

        return new TelephonyNetworkAgent(mPhone, getHandler().getLooper(), this,
                mNetworkScore, configBuilder.build(), provider,
                new TelephonyNetworkAgentCallback(getHandler()::post) {
                    @Override
                    public void onValidationStatus(@ValidationStatus int status,
                            @Nullable Uri redirectUri) {
                        mDataNetworkCallback.invokeFromExecutor(
                                () -> mDataNetworkCallback.onValidationStatusChanged(
                                        DataNetwork.this, status, redirectUri));
                    }
                });
    }

    /**
     * The default state. Any events that were not handled by the child states fallback to this
     * state.
     *
     * @see DataNetwork for the state machine diagram.
     */
    private final class DefaultState extends State {
        @Override
        public void enter() {
            logv("Registering all events.");
            mDataConfigManagerCallback = new DataConfigManagerCallback(getHandler()::post) {
                @Override
                public void onCarrierConfigChanged() {
                    sendMessage(EVENT_DATA_CONFIG_UPDATED);
                }
            };
            mRil.registerForPcoData(getHandler(), EVENT_PCO_DATA_RECEIVED, null);

            mDataConfigManager.registerCallback(mDataConfigManagerCallback);

            mDataSettingsManagerCallback = new DataSettingsManagerCallback(getHandler()::post) {
                @Override
                public void onDataEnabledChanged(boolean enabled,
                        @TelephonyManager.DataEnabledChangedReason int reason,
                        @NonNull String callingPackage) {
                    if (enabled) {
                        // The NOT_RESTRICTED capability might be changed after data enabled. We
                        // need to update the capabilities again.
                        log("Data enabled. update network capabilities.");
                        updateNetworkCapabilities();
                    }
                }

                @Override
                public void onDataRoamingEnabledChanged(boolean enabled) {
                    if (enabled) {
                        // The NOT_RESTRICTED capability might be changed after data roaming
                        // enabled. We need to update the capabilities again.
                        log("Data roaming enabled. update network capabilities.");
                        updateNetworkCapabilities();
                    }
                }
            };

            mDataNetworkController.getDataSettingsManager()
                    .registerCallback(mDataSettingsManagerCallback);

            mPhoneSwitcherCallback = new PhoneSwitcherCallback(Runnable::run) {
                @Override
                public void onPreferredDataPhoneIdChanged(int phoneId) {
                    sendMessage(EVENT_PREFERRED_DATA_SUBSCRIPTION_CHANGED, phoneId, 0);
                }
            };
            mPhoneSwitcher.registerCallback(mPhoneSwitcherCallback);

            mPhone.getDisplayInfoController().registerForTelephonyDisplayInfoChanged(
                    getHandler(), EVENT_DISPLAY_INFO_CHANGED, null);
            mPhone.getServiceStateTracker().registerForServiceStateChanged(getHandler(),
                    EVENT_SERVICE_STATE_CHANGED, null);
            for (int transport : mAccessNetworksManager.getAvailableTransports()) {
                mDataServiceManagers.get(transport)
                        .registerForDataCallListChanged(getHandler(), EVENT_DATA_STATE_CHANGED);
            }

            mCarrierPrivilegesCallback =
                    (Set<String> privilegedPackageNames, Set<Integer> privilegedUids) -> {
                        log("onCarrierPrivilegesChanged, Uids=" + privilegedUids);
                        Message message = obtainMessage(EVENT_CARRIER_PRIVILEGED_UIDS_CHANGED);
                        AsyncResult.forMessage(
                                message,
                                privilegedUids.stream().mapToInt(i -> i).toArray(),
                                null /* ex */);
                        sendMessage(message);
                    };
            TelephonyManager tm = mPhone.getContext().getSystemService(TelephonyManager.class);
            if (tm != null) {
                tm.registerCarrierPrivilegesCallback(
                        mPhone.getPhoneId(), getHandler()::post, mCarrierPrivilegesCallback);
            }

            mPhone.getServiceStateTracker().registerForCssIndicatorChanged(
                    getHandler(), EVENT_CSS_INDICATOR_CHANGED, null);
            if (mPhone.getCallTracker() != null) {
                mPhone.getCallTracker().registerForVoiceCallStarted(
                        getHandler(), EVENT_VOICE_CALL_STARTED, null);
                mPhone.getCallTracker().registerForVoiceCallEnded(
                        getHandler(), EVENT_VOICE_CALL_ENDED, null);
            }
            // Check null for devices not supporting FEATURE_TELEPHONY_IMS.
            if (mPhone.getImsPhone() != null && mPhone.getImsPhone().getCallTracker() != null) {
                mPhone.getImsPhone().getCallTracker().registerForVoiceCallStarted(
                        getHandler(), EVENT_VOICE_CALL_STARTED, null);
                mPhone.getImsPhone().getCallTracker().registerForVoiceCallEnded(
                        getHandler(), EVENT_VOICE_CALL_ENDED, null);
            }

            if (mDataProfile.canSatisfy(NetworkCapabilities.NET_CAPABILITY_MMS)) {
                mAccessNetworksManagerCallback = new AccessNetworksManagerCallback(
                        getHandler()::post) {
                    @Override
                    public void onPreferredTransportChanged(
                            @NetCapability int networkCapability, boolean forceReconnect) {
                        if (networkCapability == NetworkCapabilities.NET_CAPABILITY_MMS) {
                            log("MMS preference changed.");
                            updateNetworkCapabilities();
                        }
                    }
                };
                mAccessNetworksManager.registerCallback(mAccessNetworksManagerCallback);
            }

            // Only add symmetric code here, for example, registering and unregistering.
            // DefaultState.enter() is the starting point in the life cycle of the DataNetwork,
            // and DefaultState.exit() is the end. For non-symmetric initializing works, put them
            // in ConnectingState.enter().
        }

        @Override
        public void exit() {
            logv("Unregistering all events.");
            if (mAccessNetworksManagerCallback != null) {
                mAccessNetworksManager.unregisterCallback(mAccessNetworksManagerCallback);
            }

            // Check null for devices not supporting FEATURE_TELEPHONY_IMS.
            if (mPhone.getImsPhone() != null && mPhone.getImsPhone().getCallTracker() != null) {
                mPhone.getImsPhone().getCallTracker().unregisterForVoiceCallStarted(getHandler());
                mPhone.getImsPhone().getCallTracker().unregisterForVoiceCallEnded(getHandler());
            }
            if (mPhone.getCallTracker() != null) {
                mPhone.getCallTracker().unregisterForVoiceCallStarted(getHandler());
                mPhone.getCallTracker().unregisterForVoiceCallEnded(getHandler());
            }

            mPhone.getServiceStateTracker().unregisterForCssIndicatorChanged(getHandler());
            TelephonyManager tm = mPhone.getContext().getSystemService(TelephonyManager.class);
            if (tm != null && mCarrierPrivilegesCallback != null) {
                tm.unregisterCarrierPrivilegesCallback(mCarrierPrivilegesCallback);
            }
            for (int transport : mAccessNetworksManager.getAvailableTransports()) {
                mDataServiceManagers.get(transport)
                        .unregisterForDataCallListChanged(getHandler());
            }
            mPhone.getServiceStateTracker().unregisterForServiceStateChanged(getHandler());
            mPhone.getDisplayInfoController().unregisterForTelephonyDisplayInfoChanged(
                    getHandler());
            mPhoneSwitcher.unregisterCallback(mPhoneSwitcherCallback);
            mDataNetworkController.getDataSettingsManager()
                    .unregisterCallback(mDataSettingsManagerCallback);
            mRil.unregisterForPcoData(getHandler());
            mDataConfigManager.unregisterCallback(mDataConfigManagerCallback);
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case EVENT_DATA_CONFIG_UPDATED:
                    onCarrierConfigUpdated();
                    break;
                case EVENT_SERVICE_STATE_CHANGED: {
                    int networkType = getDataNetworkType();
                    mDataCallSessionStats.onDrsOrRatChanged(networkType);
                    if (networkType != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                        mLastKnownDataNetworkType = networkType;
                    }
                    NetworkRegistrationInfo nri =
                            mPhone.getServiceState()
                                    .getNetworkRegistrationInfo(
                                            NetworkRegistrationInfo.DOMAIN_PS,
                                            AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
                    if (nri != null && nri.isInService()) {
                        mLastKnownRoamingState = nri.getNetworkRegistrationState()
                                == NetworkRegistrationInfo.REGISTRATION_STATE_ROAMING;
                    }
                    updateSuspendState();
                    updateNetworkCapabilities();
                    break;
                }
                case EVENT_ATTACH_NETWORK_REQUEST: {
                    onAttachNetworkRequests((NetworkRequestList) msg.obj);
                    updateNetworkScore();
                    break;
                }
                case EVENT_DETACH_NETWORK_REQUEST: {
                    onDetachNetworkRequest((TelephonyNetworkRequest) msg.obj,
                            msg.arg1 != 0 /* shouldRetry */);
                    updateNetworkScore();
                    break;
                }
                case EVENT_DETACH_ALL_NETWORK_REQUESTS: {
                    for (TelephonyNetworkRequest networkRequest : mAttachedNetworkRequestList) {
                        // Check if the network request still belongs to this network, because
                        // during data switch, the network request can be attached to other network
                        // on a different SIM.
                        if (networkRequest.getAttachedNetwork() == DataNetwork.this) {
                            networkRequest.setState(
                                    TelephonyNetworkRequest.REQUEST_STATE_UNSATISFIED);
                            networkRequest.setAttachedNetwork(null);
                        }
                    }
                    log("All network requests detached.");
                    mAttachedNetworkRequestList.clear();
                    break;
                }
                case EVENT_DATA_STATE_CHANGED: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    int transport = (int) ar.userObj;
                    onDataStateChanged(transport, (List<DataCallResponse>) ar.result);
                    break;
                }
                case EVENT_CARRIER_PRIVILEGED_UIDS_CHANGED: {
                    AsyncResult asyncResult = (AsyncResult) msg.obj;
                    int[] administratorUids = (int[]) asyncResult.result;
                    mAdministratorUids = Arrays.copyOf(administratorUids, administratorUids.length);
                    updateNetworkCapabilities();
                    break;
                }
                case EVENT_PCO_DATA_RECEIVED: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    onPcoDataReceived((PcoData) ar.result);
                    break;
                }
                case EVENT_NOTIFY_HANDOVER_CANCELLED_RESPONSE:
                    removeMessages(EVENT_CANCEL_HANDOVER_NO_RESPONSE);
                    log("Notified handover cancelled.");
                    break;
                case EVENT_BANDWIDTH_ESTIMATE_FROM_MODEM_CHANGED:
                case EVENT_TEAR_DOWN_NETWORK:
                case EVENT_STUCK_IN_TRANSIENT_STATE:
                case EVENT_CANCEL_HANDOVER_NO_RESPONSE:
                case EVENT_DISPLAY_INFO_CHANGED:
                case EVENT_WAITING_FOR_TEARING_DOWN_CONDITION_MET:
                case EVENT_CSS_INDICATOR_CHANGED:
                case EVENT_VOICE_CALL_STARTED:
                case EVENT_VOICE_CALL_ENDED:
                    // Ignore the events when not in the correct state.
                    log("Ignored " + eventToString(msg.what));
                    break;
                case EVENT_NOTIFY_HANDOVER_STARTED_RESPONSE:
                case EVENT_NOTIFY_HANDOVER_STARTED:
                    // We reach here if network is not in the right state.
                    if (msg.obj != null) {
                        // Cancel it because it's either HO in progress or will soon disconnect.
                        // Either case we want to clean up obsolete retry attempts.
                        DataHandoverRetryEntry retryEntry = (DataHandoverRetryEntry) msg.obj;
                        retryEntry.setState(DataRetryEntry.RETRY_STATE_CANCELLED);
                    }
                    log("Ignore handover to " + AccessNetworkConstants
                            .transportTypeToString(msg.arg1) + " request.");
                    break;
                case EVENT_RADIO_NOT_AVAILABLE:
                    mFailCause = DataFailCause.RADIO_NOT_AVAILABLE;
                    loge(eventToString(msg.what) + ": transition to disconnected state");
                    transitionTo(mDisconnectedState);
                    break;
                case EVENT_DATA_NETWORK_VALIDATION_REQUESTED:
                    // If the data network is not connected, the request should be ignored.
                    handleErrorDataNetworkValidationRequest((Consumer<Integer>) msg.obj);
                    break;
                case EVENT_DATA_NETWORK_VALIDATION_RESPONSE:
                    // handle the resultCode in response for the request.
                    handleDataNetworkValidationRequestResultCode(msg.arg1 /* resultCode */);
                    break;
                case EVENT_PREFERRED_DATA_SUBSCRIPTION_CHANGED:
                    mOnPreferredDataPhone = mPhone.getPhoneId() == msg.arg1;
                    logl("Preferred data phone id changed to " + msg.arg1
                            + ", mOnPreferredDataPhone=" + mOnPreferredDataPhone);
                    updateNetworkScore();
                    break;
                default:
                    loge("Unhandled event " + eventToString(msg.what));
                    break;
            }
            return HANDLED;
        }
    }

    /**
     * The connecting state. This is the initial state of a data network.
     *
     * @see DataNetwork for the state machine diagram.
     */
    private final class ConnectingState extends State {
        /** Used for checking setup response IP mismatch. */
        @NetworkRegistrationInfo.RegistrationState private int mRegStateWhenSetup;
        @Override
        public void enter() {
            sendMessageDelayed(EVENT_STUCK_IN_TRANSIENT_STATE,
                    mDataConfigManager.getAnomalyNetworkConnectingTimeoutMs());
            mNetworkAgent = createNetworkAgent();
            mInitialNetworkAgentId = mNetworkAgent.getId();
            mLogTag = "DN-" + mInitialNetworkAgentId + "-"
                    + ((mTransport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) ? "C" : "I");

            // Get carrier config package uid. Note that this uid will not change through the life
            // cycle of this data network. So there is no need to listen to the change event.
            mCarrierServicePackageUid = mPhone.getCarrierPrivilegesTracker()
                    .getCarrierServicePackageUid();

            notifyPreciseDataConnectionState();
            if (mTransport == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                // Defer setupData until we get the PDU session ID response
                allocatePduSessionId();
                return;
            }

            setupData();
        }

        @Override
        public void exit() {
            removeMessages(EVENT_STUCK_IN_TRANSIENT_STATE);
        }

        @Override
        public boolean processMessage(Message msg) {
            logv("event=" + eventToString(msg.what));
            switch (msg.what) {
                case EVENT_ALLOCATE_PDU_SESSION_ID_RESPONSE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        mPduSessionId = (int) ar.result;
                        log("Set PDU session id to " + mPduSessionId);
                    } else {
                        loge("Failed to allocate PDU session id. e=" + ar.exception);
                    }
                    //Check whether all network requests were removed before setupData.
                    if (!mAttachedNetworkRequestList.isEmpty()) {
                        setupData();
                    } else {
                        mRetryDelayMillis = DataCallResponse.RETRY_DURATION_UNDEFINED;
                        transitionTo(mDisconnectedState);
                    }
                    break;
                case EVENT_SETUP_DATA_NETWORK_RESPONSE:
                    int resultCode = msg.arg1;
                    DataCallResponse dataCallResponse =
                            msg.getData().getParcelable(DataServiceManager.DATA_CALL_RESPONSE);
                    onSetupResponse(resultCode, dataCallResponse);
                    break;
                case EVENT_NOTIFY_HANDOVER_STARTED:
                case EVENT_TEAR_DOWN_NETWORK:
                case EVENT_WAITING_FOR_TEARING_DOWN_CONDITION_MET:
                    // Defer the request until connected or disconnected.
                    log("Defer message " + eventToString(msg.what));
                    deferMessage(msg);
                    break;
                case EVENT_STUCK_IN_TRANSIENT_STATE:
                    reportAnomaly("Data network stuck in connecting state for "
                            + TimeUnit.MILLISECONDS.toSeconds(
                            mDataConfigManager.getAnomalyNetworkConnectingTimeoutMs())
                            + " seconds.", "58c56403-7ea7-4e56-a0c7-e467114d09b8");
                    // Setup data failed. Use the retry logic defined in
                    // CarrierConfigManager.KEY_TELEPHONY_DATA_SETUP_RETRY_RULES_STRING_ARRAY.
                    mRetryDelayMillis = DataCallResponse.RETRY_DURATION_UNDEFINED;
                    mFailCause = DataFailCause.NO_RETRY_FAILURE;
                    transitionTo(mDisconnectedState);
                    break;
                case EVENT_DEACTIVATE_DATA_NETWORK_RESPONSE:
                    int responseCode = msg.arg1;
                    onDeactivateResponse(responseCode);
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        /**
         * Setup a data network.
         */
        private void setupData() {
            int dataNetworkType = getDataNetworkType();

            NetworkRegistrationInfo nri = getNetworkRegistrationInfo();
            mRegStateWhenSetup = nri != null
                    ? nri.getNetworkRegistrationState()
                    : NetworkRegistrationInfo.REGISTRATION_STATE_UNKNOWN;
            // We need to use the actual modem roaming state instead of the framework roaming state
            // here. This flag is only passed down to ril_service for picking the correct protocol
            // (for old modem backward compatibility).
            boolean isModemRoaming = mPhone.getServiceState().getDataRoamingFromRegistration();

            // Set this flag to true if the user turns on data roaming. Or if we override the
            // roaming state in framework, we should set this flag to true as well so the modem will
            // not reject the data call setup (because the modem thinks the device is roaming).
            boolean allowRoaming = mPhone.getDataRoamingEnabled()
                    || (isModemRoaming && (!mPhone.getServiceState().getDataRoaming()
                    /*|| isUnmeteredUseOnly()*/));

            TrafficDescriptor trafficDescriptor = mDataProfile.getTrafficDescriptor();
            final boolean matchAllRuleAllowed = trafficDescriptor == null
                    || !TextUtils.isEmpty(trafficDescriptor.getDataNetworkName())
                    // Both OsAppId and APN name are null. This helps for modem to handle when we
                    // are on 5G or LTE with URSP support in falling back to default network.
                    || (TextUtils.isEmpty(trafficDescriptor.getDataNetworkName())
                    && trafficDescriptor.getOsAppId() == null);

            int accessNetwork = DataUtils.networkTypeToAccessNetworkType(dataNetworkType);

            mDataServiceManagers.get(mTransport)
                    .setupDataCall(accessNetwork, mDataProfile, isModemRoaming, allowRoaming,
                            DataService.REQUEST_REASON_NORMAL, null, mPduSessionId, null,
                            trafficDescriptor, matchAllRuleAllowed,
                            obtainMessage(EVENT_SETUP_DATA_NETWORK_RESPONSE));

            int apnTypeBitmask = mDataProfile.getApnSetting() != null
                    ? mDataProfile.getApnSetting().getApnTypeBitmask() : ApnSetting.TYPE_NONE;
            mDataCallSessionStats.onSetupDataCall(apnTypeBitmask, isSatellite());

            logl("setupData: accessNetwork="
                    + AccessNetworkType.toString(accessNetwork) + ", " + mDataProfile
                    + ", isModemRoaming=" + isModemRoaming + ", allowRoaming=" + allowRoaming
                    + ", PDU session id=" + mPduSessionId + ", matchAllRuleAllowed="
                    + matchAllRuleAllowed);
            TelephonyMetrics.getInstance().writeSetupDataCall(mPhone.getPhoneId(),
                    ServiceState.networkTypeToRilRadioTechnology(dataNetworkType),
                    mDataProfile.getProfileId(), mDataProfile.getApn(),
                    mDataProfile.getProtocolType());
        }

        /**
         * Called when receiving setup data network response from the data service.
         *
         * @param resultCode The result code.
         * @param response The response.
         */
        private void onSetupResponse(@DataServiceCallback.ResultCode int resultCode,
                @Nullable DataCallResponse response) {
            logl("onSetupResponse: resultCode=" + DataServiceCallback.resultCodeToString(resultCode)
                    + ", response=" + response);
            mFailCause = getFailCauseFromDataCallResponse(resultCode, response);
            validateDataCallResponse(response, mRegStateWhenSetup);
            if (mFailCause == DataFailCause.NONE) {
                DataNetwork dataNetwork = mDataNetworkController.getDataNetworkByInterface(
                        response.getInterfaceName());
                if (dataNetwork != null) {
                    logl("Interface " + response.getInterfaceName() + " has been already used by "
                            + dataNetwork + ". Silently tear down now.");
                    // If this is a pre-5G data setup, that means APN database has some problems.
                    // For example, different APN settings have the same APN name.
                    if (response.getTrafficDescriptors().isEmpty() && dataNetwork.isConnected()) {
                        reportAnomaly("Duplicate network interface " + response.getInterfaceName()
                                + " detected.", "62f66e7e-8d71-45de-a57b-dc5c78223fd5");
                    }

                    // Do not actually invoke onTearDown, otherwise the existing data network will
                    // be torn down.
                    mRetryDelayMillis = DataCallResponse.RETRY_DURATION_UNDEFINED;
                    mFailCause = DataFailCause.NO_RETRY_FAILURE;
                    transitionTo(mDisconnectedState);
                    return;
                }

                updateDataNetwork(response);

                if (mVcnManager != null && mVcnManager.applyVcnNetworkPolicy(mNetworkCapabilities,
                        mLinkProperties).isTeardownRequested()) {
                    log("VCN service requested to tear down the network.");
                    // Directly call onTearDown here. Calling tearDown will cause deadlock because
                    // EVENT_TEAR_DOWN_NETWORK is deferred until state machine enters connected
                    // state, which will never happen in this case.
                    onTearDown(TEAR_DOWN_REASON_VCN_REQUESTED);
                    return;
                }

                transitionTo(mConnectedState);
            } else {
                // Setup data failed.
                mRetryDelayMillis = response != null ? response.getRetryDurationMillis()
                        : DataCallResponse.RETRY_DURATION_UNDEFINED;
                transitionTo(mDisconnectedState);
            }

            int apnTypeBitmask = ApnSetting.TYPE_NONE;
            int protocol = ApnSetting.PROTOCOL_UNKNOWN;
            if (mDataProfile.getApnSetting() != null) {
                apnTypeBitmask = mDataProfile.getApnSetting().getApnTypeBitmask();
                protocol = mDataProfile.getApnSetting().getProtocol();
            }
            mDataCallSessionStats.onSetupDataCallResponse(response,
                    getDataNetworkType(),
                    apnTypeBitmask,
                    protocol,
                    // Log the raw fail cause to avoid large amount of UNKNOWN showing on metrics.
                    response != null ? response.getCause() : mFailCause);
        }
    }

    /**
     * The connected state. This is the state when data network becomes usable.
     *
     * @see DataNetwork for the state machine diagram.
     */
    private final class ConnectedState extends State {
        @Override
        public void enter() {
            // Note that reaching here could mean from connecting -> connected, or from
            // handover -> connected.
            if (!mEverConnected) {
                // Transited from ConnectingState
                log("network connected.");
                mEverConnected = true;
                mNetworkAgent.markConnected();
                mDataNetworkCallback.invokeFromExecutor(
                        () -> mDataNetworkCallback.onConnected(DataNetwork.this));

                mQosCallbackTracker = new QosCallbackTracker(mNetworkAgent, mPhone);
                mQosCallbackTracker.updateSessions(mQosBearerSessions);
                mKeepaliveTracker = new KeepaliveTracker(mPhone,
                        getHandler().getLooper(), DataNetwork.this, mNetworkAgent);
                if (mTransport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                    registerForWwanEvents();
                }

                // Create the VCN policy changed listener. When the policy changed, we might need
                // to tear down the VCN-managed network.
                if (mVcnManager != null) {
                    mVcnPolicyChangeListener = () -> {
                        log("VCN policy changed.");
                        if (mVcnManager.applyVcnNetworkPolicy(mNetworkCapabilities, mLinkProperties)
                                .isTeardownRequested()) {
                            tearDown(TEAR_DOWN_REASON_VCN_REQUESTED);
                        } else {
                            updateNetworkCapabilities();
                        }
                    };
                    mVcnManager.addVcnNetworkPolicyChangeListener(
                            getHandler()::post, mVcnPolicyChangeListener);
                }
            }

            // If we've ever received PCO data before connected, now it's the time to process it.
            mPcoData.getOrDefault(mCid.get(mTransport), Collections.emptyMap())
                    .forEach((pcoId, pcoData) -> onPcoDataChanged(pcoData));

            mDataNetworkCallback.invokeFromExecutor(
                    () -> mDataNetworkCallback.onLinkStatusChanged(DataNetwork.this, mLinkStatus));
            notifyPreciseDataConnectionState();
            updateSuspendState();
        }

        @Override
        public boolean processMessage(Message msg) {
            logv("event=" + eventToString(msg.what));
            switch (msg.what) {
                case EVENT_TEAR_DOWN_NETWORK:
                    if (mInvokedDataDeactivation) {
                        log("Ignore tear down request because network is being torn down.");
                        break;
                    }

                    int tearDownReason = msg.arg1;

                    removeMessages(EVENT_TEAR_DOWN_NETWORK);
                    removeDeferredMessages(EVENT_TEAR_DOWN_NETWORK);
                    transitionTo(mDisconnectingState);
                    onTearDown(tearDownReason);
                    break;
                case EVENT_BANDWIDTH_ESTIMATE_FROM_MODEM_CHANGED:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        log("EVENT_BANDWIDTH_ESTIMATE_FROM_MODEM_CHANGED: error ignoring, e="
                                + ar.exception);
                        break;
                    }
                    onBandwidthUpdatedFromModem((List<LinkCapacityEstimate>) ar.result);
                    break;
                case EVENT_DISPLAY_INFO_CHANGED:
                    onDisplayInfoChanged();
                    break;
                case EVENT_NOTIFY_HANDOVER_STARTED:
                    // Notify source transport that handover is about to start. Note this will not
                    // initiate the handover process on target transport, but more for notifying
                    // the source transport so that PDU session id can be preserved if network
                    // notifies PDN lost during handover. The real handover process will kick off
                    // after receiving EVENT_NOTIFY_HANDOVER_STARTED_RESPONSE.
                    log("Notifying source transport "
                            + AccessNetworkConstants.transportTypeToString(mTransport)
                            + " that handover is about to start.");
                    mDataServiceManagers.get(mTransport).startHandover(mCid.get(mTransport),
                            obtainMessage(EVENT_NOTIFY_HANDOVER_STARTED_RESPONSE, 0, msg.arg2,
                                    msg.obj));
                    // We enter handover state here because this is the first action we do for
                    // handover.
                    transitionTo(mHandoverState);
                    break;
                case EVENT_SUBSCRIPTION_PLAN_OVERRIDE:
                    updateMeteredAndCongested();
                    break;
                case EVENT_DEACTIVATE_DATA_NETWORK_RESPONSE:
                    int resultCode = msg.arg1;
                    onDeactivateResponse(resultCode);
                    break;
                case EVENT_WAITING_FOR_TEARING_DOWN_CONDITION_MET:
                    transitionTo(mDisconnectingState);
                    sendMessageDelayed(EVENT_TEAR_DOWN_NETWORK, msg.arg1, msg.arg2);
                    break;
                case EVENT_VOICE_CALL_STARTED:
                case EVENT_VOICE_CALL_ENDED:
                case EVENT_CSS_INDICATOR_CHANGED:
                    updateSuspendState();
                    updateNetworkCapabilities();
                    break;
                case EVENT_DATA_NETWORK_VALIDATION_REQUESTED:
                    // Network validation request can be accepted if the data is in connected state
                    handleDataNetworkValidationRequest((Consumer<Integer>) msg.obj);
                    break;
                case EVENT_CANCEL_HANDOVER_NO_RESPONSE:
                    reportAnomaly("Cancel handover no response within "
                            + TimeUnit.MILLISECONDS.toSeconds(
                            mDataConfigManager.getNetworkHandoverTimeoutMs())
                            + " seconds.", "ad320988-0601-4955-836a-e6b67289c294");
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    /**
     * The handover state. This is the state when data network handover between IWLAN and cellular.
     *
     * @see DataNetwork for the state machine diagram.
     */
    private final class HandoverState extends State {
        @Override
        public void enter() {
            removeMessages(EVENT_CANCEL_HANDOVER_NO_RESPONSE);
            sendMessageDelayed(EVENT_STUCK_IN_TRANSIENT_STATE,
                    mDataConfigManager.getNetworkHandoverTimeoutMs());
            notifyPreciseDataConnectionState();
            updateNetworkScore(true /* keepConnectedForHandover */);
        }

        @Override
        public void exit() {
            removeMessages(EVENT_STUCK_IN_TRANSIENT_STATE);
            updateNetworkScore(false /* keepConnectedForHandover */);
        }

        @Override
        public boolean processMessage(Message msg) {
            logv("event=" + eventToString(msg.what));
            switch (msg.what) {
                case EVENT_DATA_STATE_CHANGED:
                    // The data call list changed event should be conditionally deferred.
                    // Otherwise the deferred message might be incorrectly treated as "disconnected"
                    // signal. So we only defer the related data call list changed event, and drop
                    // the unrelated.
                    AsyncResult ar = (AsyncResult) msg.obj;
                    int transport = (int) ar.userObj;
                    List<DataCallResponse> responseList = (List<DataCallResponse>) ar.result;
                    if (transport != mTransport) {
                        log("Dropped unrelated "
                                + AccessNetworkConstants.transportTypeToString(transport)
                                + " data call list changed event. " + responseList);
                    } else {
                        log("Defer message " + eventToString(msg.what) + ":" + responseList);
                        deferMessage(msg);
                    }
                    break;
                case EVENT_WAITING_FOR_TEARING_DOWN_CONDITION_MET:
                case EVENT_DISPLAY_INFO_CHANGED:
                case EVENT_TEAR_DOWN_NETWORK:
                case EVENT_CSS_INDICATOR_CHANGED:
                case EVENT_VOICE_CALL_ENDED:
                case EVENT_VOICE_CALL_STARTED:
                case EVENT_DATA_NETWORK_VALIDATION_REQUESTED:
                    // Defer the request until handover succeeds or fails.
                    log("Defer message " + eventToString(msg.what));
                    deferMessage(msg);
                    break;
                case EVENT_NOTIFY_HANDOVER_STARTED_RESPONSE:
                    onStartHandover(msg.arg2, (DataHandoverRetryEntry) msg.obj);
                    break;
                case EVENT_HANDOVER_RESPONSE:
                    int resultCode = msg.arg1;
                    DataCallResponse dataCallResponse =
                            msg.getData().getParcelable(DataServiceManager.DATA_CALL_RESPONSE);
                    onHandoverResponse(resultCode, dataCallResponse,
                            (DataHandoverRetryEntry) msg.obj);
                    break;
                case EVENT_STUCK_IN_TRANSIENT_STATE:
                    // enable detection only for valid timeout range
                    reportAnomaly("Data service did not respond the handover request within "
                                    + TimeUnit.MILLISECONDS.toSeconds(
                            mDataConfigManager.getNetworkHandoverTimeoutMs()) + " seconds.",
                            "1afe68cb-8b41-4964-a737-4f34372429ea");
                    // Handover failed. Use the retry logic defined in
                    // CarrierConfigManager.KEY_TELEPHONY_DATA_HANDOVER_RETRY_RULES_STRING_ARRAY.
                    long retry = DataCallResponse.RETRY_DURATION_UNDEFINED;
                    int handoverFailureMode =
                            DataCallResponse.HANDOVER_FAILURE_MODE_NO_FALLBACK_RETRY_SETUP_NORMAL;
                    mFailCause = DataFailCause.ERROR_UNSPECIFIED;
                    mDataNetworkCallback.invokeFromExecutor(
                            () -> mDataNetworkCallback.onHandoverFailed(DataNetwork.this,
                                    mFailCause, retry, handoverFailureMode));
                    // No matter handover succeeded or not, transit back to connected state.
                    transitionTo(mConnectedState);
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    /**
     * The disconnecting state. This is the state when data network is about to be disconnected.
     * The network is still usable in this state, but the clients should be prepared to lose the
     * network in any moment. This state is particular useful for IMS graceful tear down, where
     * the network enters disconnecting state while waiting for IMS de-registration signal.
     *
     * @see DataNetwork for the state machine diagram.
     */
    private final class DisconnectingState extends State {
        @Override
        public void enter() {
            sendMessageDelayed(EVENT_STUCK_IN_TRANSIENT_STATE,
                    mDataConfigManager.getAnomalyNetworkDisconnectingTimeoutMs());
            notifyPreciseDataConnectionState();
        }

        @Override
        public void exit() {
            removeMessages(EVENT_STUCK_IN_TRANSIENT_STATE);
        }

        @Override
        public boolean processMessage(Message msg) {
            logv("event=" + eventToString(msg.what));
            switch (msg.what) {
                case EVENT_TEAR_DOWN_NETWORK:
                    if (mInvokedDataDeactivation) {
                        log("Ignore tear down request because network is being torn down.");
                        break;
                    }
                    removeMessages(EVENT_TEAR_DOWN_NETWORK);
                    removeDeferredMessages(EVENT_TEAR_DOWN_NETWORK);
                    onTearDown(msg.arg1);
                    break;
                case EVENT_DEACTIVATE_DATA_NETWORK_RESPONSE:
                    int resultCode = msg.arg1;
                    onDeactivateResponse(resultCode);
                    break;
                case EVENT_STUCK_IN_TRANSIENT_STATE:
                    // After frameworks issues deactivate data call request, RIL should report
                    // data disconnected through data call list changed event subsequently.

                    reportAnomaly("RIL did not send data call list changed event after "
                            + "deactivate data call request within "
                            + TimeUnit.MILLISECONDS.toSeconds(
                            mDataConfigManager.getAnomalyNetworkDisconnectingTimeoutMs())
                            + " seconds.", "d0e4fa1c-c57b-4ba5-b4b6-8955487012cc");
                    mFailCause = DataFailCause.LOST_CONNECTION;
                    transitionTo(mDisconnectedState);
                    break;
                case EVENT_DISPLAY_INFO_CHANGED:
                    onDisplayInfoChanged();
                    break;
                case EVENT_CSS_INDICATOR_CHANGED:
                case EVENT_VOICE_CALL_STARTED:
                case EVENT_VOICE_CALL_ENDED:
                    updateSuspendState();
                    updateNetworkCapabilities();
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    /**
     * The disconnected state. This is the final state of a data network.
     *
     * @see DataNetwork for the state machine diagram.
     */
    private final class DisconnectedState extends State {
        @Override
        public void enter() {
            logl("Data network disconnected. mEverConnected=" + mEverConnected);
            // Preserve the list for onSetupDataFailed callback, because we need to pass that list
            // back to DataNetworkController, but after EVENT_DETACH_ALL_NETWORK_REQUESTS gets
            // processed, the network request list would become empty.
            NetworkRequestList requestList = new NetworkRequestList(mAttachedNetworkRequestList);

            // The detach all network requests must be the last message to handle.
            sendMessage(EVENT_DETACH_ALL_NETWORK_REQUESTS);
            // Gracefully handle all the un-processed events then quit the state machine.
            // quit() throws a QUIT event to the end of message queue. All the events before quit()
            // will be processed. Events after quit() will not be processed.
            quit();

            //************************************************************//
            // DO NOT POST ANY EVENTS AFTER HERE.                         //
            // THE STATE MACHINE WONT PROCESS EVENTS AFTER QUIT.          //
            // ONLY CLEANUP SHOULD BE PERFORMED AFTER THIS.               //
            //************************************************************//

            if (mEverConnected) {
                mLinkStatus = DataCallResponse.LINK_STATUS_INACTIVE;
                mNetworkValidationStatus =
                        PreciseDataConnectionState.NETWORK_VALIDATION_UNSUPPORTED;
                mDataNetworkCallback.invokeFromExecutor(() -> mDataNetworkCallback
                        .onLinkStatusChanged(DataNetwork.this, mLinkStatus));
                mDataNetworkCallback.invokeFromExecutor(() -> mDataNetworkCallback
                        .onDisconnected(DataNetwork.this, mFailCause, mTearDownReason));
                if (mTransport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                    unregisterForWwanEvents();
                }
                // Since NetworkValidation is able to request only in the Connected state,
                // if ever connected, log for onDataNetworkDisconnected.
                mDataNetworkValidationStats.onDataNetworkDisconnected(getDataNetworkType());
            } else {
                mDataNetworkCallback.invokeFromExecutor(() -> mDataNetworkCallback
                        .onSetupDataFailed(DataNetwork.this,
                                requestList, mFailCause, mRetryDelayMillis));
            }
            notifyPreciseDataConnectionState();
            mNetworkAgent.unregister();
            mDataNetworkController.unregisterDataNetworkControllerCallback(
                    mDataNetworkControllerCallback);
            mDataCallSessionStats.onDataCallDisconnected(mFailCause);

            if (mTransport == AccessNetworkConstants.TRANSPORT_TYPE_WLAN
                    && mPduSessionId != DataCallResponse.PDU_SESSION_ID_NOT_SET) {
                mRil.releasePduSessionId(null, mPduSessionId);
            }

            if (mVcnManager != null && mVcnPolicyChangeListener != null) {
                mVcnManager.removeVcnNetworkPolicyChangeListener(mVcnPolicyChangeListener);
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            logv("event=" + eventToString(msg.what));
            return NOT_HANDLED;
        }
    }

    /**
     * Register for events that can only happen on cellular networks.
     */
    private void registerForWwanEvents() {
        registerForBandwidthUpdate();
        mKeepaliveTracker.registerForKeepaliveStatus();
        mRil.registerForNotAvailable(this.getHandler(), EVENT_RADIO_NOT_AVAILABLE, null);
    }

    /**
     * Unregister for events that can only happen on cellular networks.
     */
    private void unregisterForWwanEvents() {
        unregisterForBandwidthUpdate();
        mKeepaliveTracker.unregisterForKeepaliveStatus();
        mRil.unregisterForNotAvailable(this.getHandler());
    }

    @Override
    protected void unhandledMessage(Message msg) {
        IState state = getCurrentState();
        loge("Unhandled message " + msg.what + " in state "
                + (state == null ? "null" : state.getName()));
    }

    /**
     * Attempt to attach the network request list to this data network. Whether the network can
     * satisfy the request or not will be checked when EVENT_ATTACH_NETWORK_REQUEST is processed.
     * If the request can't be attached, {@link DataNetworkCallback#onAttachFailed(
     * DataNetwork, NetworkRequestList)}.
     *
     * @param requestList Network request list to attach.
     * @return {@code false} if the network is already disconnected. {@code true} means the request
     * has been scheduled to attach to the network. If attach succeeds, the network request's state
     * will be set to {@link TelephonyNetworkRequest#REQUEST_STATE_SATISFIED}. If failed, the
     * callback {@link DataNetworkCallback#onAttachFailed(DataNetwork, NetworkRequestList)} will
     * be called.
     */
    public boolean attachNetworkRequests(@NonNull NetworkRequestList requestList) {
        // If the network is already ended, we still attach the network request to the data network,
        // so it can be retried later by data network controller.
        if (getCurrentState() == null || isDisconnected()) {
            // The state machine has already stopped. This is due to data network is disconnected.
            return false;
        }
        sendMessage(obtainMessage(EVENT_ATTACH_NETWORK_REQUEST, requestList));
        return true;
    }

    /**
     * Called when attaching network request list to this data network.
     *
     * @param requestList Network request list to attach.
     */
    public void onAttachNetworkRequests(@NonNull NetworkRequestList requestList) {
        NetworkRequestList failedList = new NetworkRequestList();
        for (TelephonyNetworkRequest networkRequest : requestList) {
            if (!mDataNetworkController.isNetworkRequestExisting(networkRequest)) {
                failedList.add(networkRequest);
                log("Attached failed. Network request was already removed. " + networkRequest);
            } else if (!networkRequest.canBeSatisfiedBy(getNetworkCapabilities())) {
                failedList.add(networkRequest);
                log("Attached failed. Cannot satisfy the network request "
                        + networkRequest);
            } else {
                mAttachedNetworkRequestList.add(networkRequest);
                networkRequest.setAttachedNetwork(DataNetwork.this);
                networkRequest.setState(
                        TelephonyNetworkRequest.REQUEST_STATE_SATISFIED);
                log("Successfully attached network request " + networkRequest);
            }
        }
        if (!failedList.isEmpty()) {
            mDataNetworkCallback.invokeFromExecutor(() -> mDataNetworkCallback
                    .onAttachFailed(DataNetwork.this, failedList));
        }
    }

    /**
     * Called when detaching the network request from this data network.
     *
     * @param networkRequest Network request to detach.
     * @param shouldRetry {@code true} if the detached network request should be retried.
     */
    private void onDetachNetworkRequest(@NonNull TelephonyNetworkRequest networkRequest,
            boolean shouldRetry) {
        mAttachedNetworkRequestList.remove(networkRequest);
        // Check if the network request still belongs to this network, because
        // during data switch, the network request can be attached to other network
        // on a different SIM.
        if (networkRequest.getAttachedNetwork() == DataNetwork.this) {
            networkRequest.setState(TelephonyNetworkRequest.REQUEST_STATE_UNSATISFIED);
            networkRequest.setAttachedNetwork(null);

            if (shouldRetry) {
                // Inform DataNetworkController that a network request was detached and should be
                // scheduled to retry.
                mDataNetworkCallback.invokeFromExecutor(() ->
                        mDataNetworkCallback.onRetryUnsatisfiedNetworkRequest(networkRequest));
            }
        }

        if (mAttachedNetworkRequestList.isEmpty()) {
            log("All network requests are detached.");

            // If there is no network request attached, and we are not preferred data phone, then
            // this detach is likely due to temp DDS switch. We should tear down the network when
            // all requests are detached so the other network on preferred data sub can be
            // established properly.
            int preferredDataPhoneId = PhoneSwitcher.getInstance().getPreferredDataPhoneId();
            if (preferredDataPhoneId != SubscriptionManager.INVALID_PHONE_INDEX
                    && preferredDataPhoneId != mPhone.getPhoneId()) {
                tearDown(TEAR_DOWN_REASON_PREFERRED_DATA_SWITCHED);
            }
        }
    }

    /**
     * Detach the network request from this data network. Note that this will not tear down the
     * network.
     *
     * @param networkRequest Network request to detach.
     * @param shouldRetry {@code true} if the detached network request should be retried.
     */
    public void detachNetworkRequest(@NonNull TelephonyNetworkRequest networkRequest,
            boolean shouldRetry) {
        if (getCurrentState() == null || isDisconnected()) {
            return;
        }
        sendMessage(obtainMessage(EVENT_DETACH_NETWORK_REQUEST, shouldRetry ? 1 : 0, 0,
                networkRequest));
    }

    /**
     * Register for bandwidth update.
     */
    private void registerForBandwidthUpdate() {
        int bandwidthEstimateSource = mDataConfigManager.getBandwidthEstimateSource();
        if (bandwidthEstimateSource == BANDWIDTH_SOURCE_MODEM) {
            mPhone.mCi.registerForLceInfo(
                    getHandler(), EVENT_BANDWIDTH_ESTIMATE_FROM_MODEM_CHANGED, null);
        } else if (bandwidthEstimateSource == BANDWIDTH_SOURCE_BANDWIDTH_ESTIMATOR) {
            if (mLinkBandwidthEstimatorCallback == null) {
                mLinkBandwidthEstimatorCallback =
                        new LinkBandwidthEstimatorCallback(getHandler()::post) {
                            @Override
                            public void onBandwidthChanged(int uplinkBandwidthKbps,
                                    int downlinkBandwidthKbps) {
                                if (isConnected()) {
                                    onBandwidthUpdated(uplinkBandwidthKbps, downlinkBandwidthKbps);
                                }
                            }
                        };
                mPhone.getLinkBandwidthEstimator().registerCallback(
                        mLinkBandwidthEstimatorCallback);
            }
        } else {
            loge("Invalid bandwidth source configuration: " + bandwidthEstimateSource);
        }
    }

    /**
     * Unregister bandwidth update.
     */
    private void unregisterForBandwidthUpdate() {
        int bandwidthEstimateSource = mDataConfigManager.getBandwidthEstimateSource();
        if (bandwidthEstimateSource == BANDWIDTH_SOURCE_MODEM) {
            mPhone.mCi.unregisterForLceInfo(getHandler());
        } else if (bandwidthEstimateSource == BANDWIDTH_SOURCE_BANDWIDTH_ESTIMATOR) {
            if (mLinkBandwidthEstimatorCallback != null) {
                mPhone.getLinkBandwidthEstimator()
                        .unregisterCallback(mLinkBandwidthEstimatorCallback);
                mLinkBandwidthEstimatorCallback = null;
            }
        } else {
            loge("Invalid bandwidth source configuration: " + bandwidthEstimateSource);
        }
    }

    /**
     * Remove network requests that can't be satisfied anymore.
     *
     * @param shouldRetry {@code true} if the detached network requests should be retried.
     */
    private void removeUnsatisfiedNetworkRequests(boolean shouldRetry) {
        for (TelephonyNetworkRequest networkRequest : mAttachedNetworkRequestList) {
            if (!networkRequest.canBeSatisfiedBy(mNetworkCapabilities)) {
                log("removeUnsatisfiedNetworkRequests: " + networkRequest
                        + " can't be satisfied anymore. Will be detached.");
                detachNetworkRequest(networkRequest, shouldRetry);
            }
        }
    }

    /**
     * Check if the new link properties are compatible with the old link properties. For example,
     * if IP changes, that's considered incompatible.
     *
     * @param oldLinkProperties Old link properties.
     * @param newLinkProperties New Link properties.
     *
     * @return {@code true} if the new link properties is compatible with the old link properties.
     */
    private boolean isLinkPropertiesCompatible(@NonNull LinkProperties oldLinkProperties,
            @NonNull LinkProperties newLinkProperties) {
        if (Objects.equals(oldLinkProperties, newLinkProperties)) return true;

        if (!LinkPropertiesUtils.isIdenticalAddresses(oldLinkProperties, newLinkProperties)) {
            // If the same address type was removed and added we need to cleanup.
            LinkPropertiesUtils.CompareOrUpdateResult<Integer, LinkAddress> result =
                    new LinkPropertiesUtils.CompareOrUpdateResult<>(
                            oldLinkProperties.getLinkAddresses(),
                            newLinkProperties.getLinkAddresses(),
                            linkAddress -> Objects.hash(linkAddress.getAddress(),
                                    linkAddress.getPrefixLength(), linkAddress.getScope()));
            log("isLinkPropertiesCompatible: old=" + oldLinkProperties
                    + " new=" + newLinkProperties + " result=" + result);
            for (LinkAddress added : result.added) {
                for (LinkAddress removed : result.removed) {
                    if (NetUtils.addressTypeMatches(removed.getAddress(), added.getAddress())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Check if there are immutable capabilities changed. The connectivity service is not able
     * to handle immutable capabilities changed, but in very rare scenarios, immutable capabilities
     * need to be changed dynamically, such as in setup data call response, modem responded with the
     * same cid. In that case, we need to merge the new capabilities into the existing data network.
     *
     * @param oldCapabilities The old network capabilities.
     * @param newCapabilities The new network capabilities.
     * @return {@code true} if there are immutable network capabilities changed.
     */
    private static boolean areImmutableCapabilitiesChanged(
            @NonNull NetworkCapabilities oldCapabilities,
            @NonNull NetworkCapabilities newCapabilities) {
        if (ArrayUtils.isEmpty(oldCapabilities.getCapabilities())) return false;

        // Remove mutable capabilities from both old and new capabilities, the remaining
        // capabilities would be immutable capabilities.
        List<Integer> oldImmutableCapabilities = Arrays.stream(oldCapabilities.getCapabilities())
                .boxed().collect(Collectors.toList());
        oldImmutableCapabilities.removeAll(MUTABLE_CAPABILITIES);
        List<Integer> newImmutableCapabilities = Arrays.stream(newCapabilities.getCapabilities())
                .boxed().collect(Collectors.toList());
        newImmutableCapabilities.removeAll(MUTABLE_CAPABILITIES);
        return oldImmutableCapabilities.size() != newImmutableCapabilities.size()
                || !oldImmutableCapabilities.containsAll(newImmutableCapabilities);
    }

    /**
     * In some rare cases we need to re-create the network agent, for example, underlying network
     * IP changed, or when we unfortunately need to remove/add a immutable network capability.
     */
    private void recreateNetworkAgent() {
        if (isConnecting() || isDisconnected() || isDisconnecting()) {
            loge("Incorrect state for re-creating the network agent.");
            return;
        }

        // Abandon the network agent because we are going to create a new one.
        mNetworkAgent.abandon();
        // Create a new network agent and register with connectivity service. Note that the agent
        // will always be registered with NOT_SUSPENDED capability.
        mNetworkAgent = createNetworkAgent();
        mNetworkAgent.markConnected();
        notifyPreciseDataConnectionState();
        // Because network agent is always created with NOT_SUSPENDED, we need to update
        // the suspended if it's was in suspended state.
        if (mSuspended) {
            log("recreateNetworkAgent: The network is in suspended state. Update the network"
                    + " capability again. nc=" + mNetworkCapabilities);
            mNetworkAgent.sendNetworkCapabilities(mNetworkCapabilities);
        }
    }

    /**
     * @return {@code true} if this is a satellite data network.
     */
    public boolean isSatellite() {
        return mIsSatellite;
    }

    /**
     * Update the network capabilities.
     */
    private void updateNetworkCapabilities() {
        final NetworkCapabilities.Builder builder = new NetworkCapabilities.Builder();

        if (mIsSatellite && mDataConfigManager.getForcedCellularTransportCapabilities().stream()
                .noneMatch(this::hasNetworkCapabilityInNetworkRequests)) {
            builder.addTransportType(NetworkCapabilities.TRANSPORT_SATELLITE);
        } else {
            builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        }

        boolean roaming = mPhone.getServiceState().getDataRoaming();

        builder.setNetworkSpecifier(new TelephonyNetworkSpecifier.Builder()
                .setSubscriptionId(mSubId).build());
        builder.setSubscriptionIds(Collections.singleton(mSubId));

        ApnSetting apnSetting = mDataProfile.getApnSetting();

        if (apnSetting != null) {
            apnSetting.getApnTypes().stream()
                    .map(DataUtils::apnTypeToNetworkCapability)
                    .filter(cap -> cap >= 0)
                    .forEach(builder::addCapability);
            if (apnSetting.getApnTypes().contains(ApnSetting.TYPE_ENTERPRISE)) {
                builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
        }

        // If MMTEL capability is requested, we should not remove it because it's an immutable
        // capability defined by connectivity service. When the device enters from VoPS to non-VoPS,
        // we should perform grace tear down from data network controller if needed.
        if (hasNetworkCapabilityInNetworkRequests(NetworkCapabilities.NET_CAPABILITY_MMTEL)) {
            // Request has MMTEL, add it again so the network won't be unwanted by connectivity.
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_MMTEL);
        } else if (mDataProfile.canSatisfy(NetworkCapabilities.NET_CAPABILITY_IMS)) {
            // Request has IMS capability only.
            // Always add MMTEL capability on IMS network unless network explicitly indicates VoPS
            // not supported.
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_MMTEL);
            if (mTransport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                NetworkRegistrationInfo nri = getNetworkRegistrationInfo();
                if (nri != null) {
                    DataSpecificRegistrationInfo dsri = nri.getDataSpecificInfo();
                    // Check if the network is non-VoPS.
                    if (dsri != null && dsri.getVopsSupportInfo() != null
                            && !dsri.getVopsSupportInfo().isVopsSupported()) {
                        builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_MMTEL);
                    }
                    log("updateNetworkCapabilities: dsri=" + dsri);
                }
            }
        }

        // Extract network capabilities from the traffic descriptor.
        for (TrafficDescriptor trafficDescriptor : mTrafficDescriptors) {
            try {
                if (trafficDescriptor.getOsAppId() == null) continue;
                OsAppId osAppId = new OsAppId(trafficDescriptor.getOsAppId());
                if (!osAppId.getOsId().equals(OsAppId.ANDROID_OS_ID)) {
                    loge("Received non-Android OS id " + osAppId.getOsId());
                    continue;
                }
                int networkCapability = DataUtils.getNetworkCapabilityFromString(
                        osAppId.getAppId());
                switch (networkCapability) {
                    case NetworkCapabilities.NET_CAPABILITY_ENTERPRISE:
                        builder.addCapability(networkCapability);
                        // Always add internet if TD contains enterprise.
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                        builder.addEnterpriseId(osAppId.getDifferentiator());
                        break;
                    case NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_LATENCY:
                    case NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_BANDWIDTH:
                    case NetworkCapabilities.NET_CAPABILITY_CBS:
                        builder.addCapability(networkCapability);
                        break;
                    default:
                        loge("Invalid app id " + osAppId.getAppId());
                }
            } catch (Exception e) {
                loge("Exception: " + e + ". Failed to create osAppId from "
                        + new BigInteger(1, trafficDescriptor.getOsAppId()).toString(16));
            }
        }

        if (!mCongested) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED);
        }

        if (mTempNotMeteredSupported && mTempNotMetered) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED);
        }

        // Always start with NOT_VCN_MANAGED, then remove if VcnManager indicates this is part of a
        // VCN.
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED);
        final VcnNetworkPolicyResult vcnPolicy = getVcnPolicy(builder.build());
        if (vcnPolicy != null && !vcnPolicy.getNetworkCapabilities()
                .hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED)) {
            builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED);
        }

        if (!roaming) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);
        }

        if (!mSuspended) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);
        }

        if (mCarrierServicePackageUid != Process.INVALID_UID
                && ArrayUtils.contains(mAdministratorUids, mCarrierServicePackageUid)) {
            builder.setOwnerUid(mCarrierServicePackageUid);
            builder.setAllowedUids(Collections.singleton(mCarrierServicePackageUid));
        }
        builder.setAdministratorUids(mAdministratorUids);

        Set<Integer> meteredCapabilities = mDataConfigManager
                .getMeteredNetworkCapabilities(roaming).stream()
                .filter(cap -> mAccessNetworksManager.getPreferredTransportByNetworkCapability(cap)
                        == AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                .collect(Collectors.toSet());
        boolean unmeteredNetwork = meteredCapabilities.stream().noneMatch(
                Arrays.stream(builder.build().getCapabilities()).boxed()
                        .collect(Collectors.toSet())::contains);

        if (unmeteredNetwork) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        }

        // Always start with not-restricted, and then remove if needed.
        // By default, NET_CAPABILITY_NOT_RESTRICTED and NET_CAPABILITY_NOT_CONSTRAINED are included
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);

        // When data is disabled, or data roaming is disabled and the device is roaming, we need
        // to remove certain capabilities depending on scenarios.
        if (!mDataNetworkController.getDataSettingsManager().isDataEnabled()
                || (mPhone.getServiceState().getDataRoaming()
                && !mDataNetworkController.getDataSettingsManager().isDataRoamingEnabled())) {
            // If data is allowed because the request is a restricted network request, we need
            // to mark the network as restricted when data is disabled or data roaming is disabled
            // and the device is roaming. If we don't do that, non-privileged apps will be able
            // to use this network when data is disabled.
            if (mDataAllowedReason == DataAllowedReason.RESTRICTED_REQUEST) {
                builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
            } else if (mDataAllowedReason == DataAllowedReason.UNMETERED_USAGE
                    || mDataAllowedReason == DataAllowedReason.MMS_REQUEST
                    || mDataAllowedReason == DataAllowedReason.EMERGENCY_SUPL) {
                // If data is allowed due to unmetered usage, or MMS always-allowed, we need to
                // remove unrelated-but-metered capabilities.
                for (int capability : meteredCapabilities) {
                    // 1. If it's unmetered usage, remove all metered capabilities.
                    // 2. If it's MMS always-allowed, then remove all metered capabilities but MMS.
                    // 3/ If it's for emergency SUPL, then remove all metered capabilities but SUPL.
                    if ((capability == NetworkCapabilities.NET_CAPABILITY_MMS
                            && mDataAllowedReason == DataAllowedReason.MMS_REQUEST)
                            || (capability == NetworkCapabilities.NET_CAPABILITY_SUPL
                            && mDataAllowedReason == DataAllowedReason.EMERGENCY_SUPL)) {
                        // Not removing the capability for special uses.
                        continue;
                    }
                    builder.removeCapability(capability);
                }
            }
        }

        if (mDataNetworkController.isEsimBootStrapProvisioningActivated()) {
            builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        }

        // Check if the feature force MMS on IWLAN is enabled. When the feature is enabled, MMS
        // will be attempted on IWLAN if possible, even if existing cellular networks already
        // supports IWLAN.
        if (builder.build().hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS)) {
            // If QNS sets MMS preferred on IWLAN, and it is possible to setup an MMS network on
            // IWLAN, then we need to remove the MMS capability on the cellular network. This will
            // allow the new MMS network to be brought up on IWLAN when MMS network request arrives.
            if (mAccessNetworksManager.getPreferredTransportByNetworkCapability(
                    NetworkCapabilities.NET_CAPABILITY_MMS)
                    == AccessNetworkConstants.TRANSPORT_TYPE_WLAN && mTransport
                    == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {

                DataProfile dataProfile = mDataNetworkController.getDataProfileManager()
                        .getDataProfileForNetworkRequest(new TelephonyNetworkRequest(
                                new NetworkRequest.Builder().addCapability(
                                NetworkCapabilities.NET_CAPABILITY_MMS).build(), mPhone, mFlags),
                        TelephonyManager.NETWORK_TYPE_IWLAN, false, false, false);
                // If we find another data data profile that can support MMS on IWLAN, then remove
                // the MMS capability from this cellular network. This will allow IWLAN to be
                // brought up for MMS later.
                if (dataProfile != null && !dataProfile.getApn().equals(mDataProfile.getApn())) {
                    log("Found a different apn name " + mDataProfile.getApn()
                            + " that can serve MMS on IWLAN.");
                    builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
                }
            }
        }

        // If one of the capabilities are for special use, for example, IMS, CBS, then this
        // network should be restricted, regardless data is enabled or not.
        if (NetworkCapabilitiesUtils.inferRestrictedCapability(builder.build())
                || (vcnPolicy != null && !vcnPolicy.getNetworkCapabilities()
                        .hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED))) {
            builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        }

        // Set the bandwidth information.
        builder.setLinkDownstreamBandwidthKbps(mNetworkBandwidth.downlinkBandwidthKbps);
        builder.setLinkUpstreamBandwidthKbps(mNetworkBandwidth.uplinkBandwidthKbps);

        // Configure the network as restricted/constrained for unrestricted satellite network.
        if (mIsSatellite && builder.build().hasCapability(
                NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)) {

            int dataPolicy;
            if (mFlags.dataServiceCheck()) {
                final SatelliteController satelliteController = SatelliteController.getInstance();
                dataPolicy = satelliteController.getSatelliteDataServicePolicyForPlmn(mSubId,
                        mPhone.getServiceState().getOperatorNumeric());
            } else {
                dataPolicy = mDataConfigManager.getSatelliteDataSupportMode();
            }
            switch (dataPolicy) {
                case CarrierConfigManager.SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED
                        -> builder.removeCapability(
                                NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
                case CarrierConfigManager.SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED -> {
                    try {
                        builder.removeCapability(DataUtils
                                .NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED);
                    } catch (Exception ignored) { }
                }
                // default case CarrierConfigManager.SATELLITE_DATA_SUPPORT_ALL
            }
        }

        NetworkCapabilities nc = builder.build();
        if (mNetworkCapabilities == null || mNetworkAgent == null) {
            // This is the first time when network capabilities is created. The agent is not created
            // at this time. Just return here. The network capabilities will be used when network
            // agent is created.
            mNetworkCapabilities = nc;
            logl("Initial capabilities " + mNetworkCapabilities);
            return;
        }

        if (!nc.equals(mNetworkCapabilities)) {
            // Check if we are changing the immutable capabilities. Note that we should be very
            // careful and limit the use cases of changing immutable capabilities. Connectivity
            // service would not close sockets for clients if a network request becomes
            // unsatisfiable.
            if (mEverConnected && areImmutableCapabilitiesChanged(mNetworkCapabilities, nc)
                    && (isConnected() || isHandoverInProgress())) {
                // Before connectivity service supports making all capabilities mutable, it is
                // suggested to de-register and re-register the network agent if it is needed to
                // add/remove immutable capabilities.
                logl("updateNetworkCapabilities: Immutable capabilities changed. Re-create the "
                        + "network agent. Attempted to change from " + mNetworkCapabilities + " to "
                        + nc);
                mNetworkCapabilities = nc;
                recreateNetworkAgent();
            } else {
                // Now we need to inform connectivity service and data network controller
                // about the capabilities changed.
                mNetworkCapabilities = nc;
                log("Capabilities changed to " + mNetworkCapabilities);
                mNetworkAgent.sendNetworkCapabilities(mNetworkCapabilities);
            }

            // Only retry the request when the network is in connected or handover state. This is to
            // prevent request is detached during connecting state, and then become a setup/detach
            // infinite loop.
            boolean shouldRetry = isConnected() || isHandoverInProgress();
            removeUnsatisfiedNetworkRequests(shouldRetry);
            mDataNetworkCallback.invokeFromExecutor(() -> mDataNetworkCallback
                    .onNetworkCapabilitiesChanged(DataNetwork.this));
        } else {
            log("updateNetworkCapabilities: Capabilities not changed.");
        }
    }

    /**
     * @return The network capabilities of this data network.
     */
    @NonNull
    public NetworkCapabilities getNetworkCapabilities() {
        return mNetworkCapabilities;
    }

    /**
     * @return The link properties of this data network.
     */
    @NonNull
    public LinkProperties getLinkProperties() {
        return mLinkProperties;
    }

    /**
     * @return The data profile of this data network.
     */
    @NonNull
    public DataProfile getDataProfile() {
        return mDataProfile;
    }

    /**
     * Update data suspended state.
     */
    private void updateSuspendState() {
        if (isConnecting() || isDisconnected()) {
            // Return if not in the right state.
            return;
        }

        boolean newSuspendedState = false;
        // Get the uncombined service state directly.
        NetworkRegistrationInfo nri = getNetworkRegistrationInfo();
        if (nri == null) return;

        // Never set suspended for emergency apn. Emergency data connection
        // can work while device is not in service.
        if (mNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_EIMS)) {
            // If we are not in service, change to suspended.
        } else if (nri.getRegistrationState()
                != NetworkRegistrationInfo.REGISTRATION_STATE_HOME
                && nri.getRegistrationState()
                != NetworkRegistrationInfo.REGISTRATION_STATE_ROAMING) {
            newSuspendedState = true;
            // Check voice/data concurrency.
        } else if (!mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()
                && mTransport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                && mPhone.getCallTracker() != null) {
            newSuspendedState = mPhone.getCallTracker().getState() != PhoneConstants.State.IDLE;
        }

        // Only notify when there is a change.
        if (mSuspended != newSuspendedState) {
            mSuspended = newSuspendedState;
            logl("Network becomes " + (mSuspended ? "suspended" : "unsuspended"));
            // To update NOT_SUSPENDED capability.
            updateNetworkCapabilities();
            notifyPreciseDataConnectionState();
            mDataNetworkCallback.invokeFromExecutor(() ->
                    mDataNetworkCallback.onSuspendedStateChanged(DataNetwork.this, mSuspended));
        }
    }

    /**
     * Allocate PDU session ID from the modem. This is only needed when the data network is
     * initiated on IWLAN.
     */
    private void allocatePduSessionId() {
        mRil.allocatePduSessionId(obtainMessage(EVENT_ALLOCATE_PDU_SESSION_ID_RESPONSE));
    }

    /**
     * Get fail cause from {@link DataCallResponse} and the result code.
     *
     * @param resultCode The result code returned from
     * {@link DataServiceCallback#onSetupDataCallComplete(int, DataCallResponse)}.
     * @param response The data call response returned from
     * {@link DataServiceCallback#onSetupDataCallComplete(int, DataCallResponse)}.
     *
     * @return The fail cause. {@link DataFailCause#NONE} if succeeds.
     */
    @DataFailureCause
    private int getFailCauseFromDataCallResponse(
            @DataServiceCallback.ResultCode int resultCode, @Nullable DataCallResponse response) {
        int failCause = DataFailCause.NONE;
        switch (resultCode) {
            case DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE:
                failCause = DataFailCause.RADIO_NOT_AVAILABLE;
                break;
            case DataServiceCallback.RESULT_ERROR_BUSY:
            case DataServiceCallback.RESULT_ERROR_TEMPORARILY_UNAVAILABLE:
                failCause = DataFailCause.SERVICE_TEMPORARILY_UNAVAILABLE;
                break;
            case DataServiceCallback.RESULT_ERROR_INVALID_ARG:
                failCause = DataFailCause.UNACCEPTABLE_NETWORK_PARAMETER;
                break;
            case DataServiceCallback.RESULT_ERROR_UNSUPPORTED:
                failCause = DataFailCause.REQUEST_NOT_SUPPORTED;
                break;
            case DataServiceCallback.RESULT_SUCCESS:
                if (response != null) {
                    failCause = DataFailCause.getFailCause(response.getCause());
                }
                break;
        }
        return failCause;
    }

    /**
     * Update data network based on the latest {@link DataCallResponse}.
     *
     * @param response The data call response from data service.
     */
    private void updateDataNetwork(@Nullable DataCallResponse response) {
        if (response == null) return;
        mCid.put(mTransport, response.getId());
        LinkProperties linkProperties = new LinkProperties();

        // Set interface name
        linkProperties.setInterfaceName(response.getInterfaceName());

        // Set PDU session id
        if (mPduSessionId != response.getPduSessionId()) {
            mPduSessionId = response.getPduSessionId();
            log("PDU session id updated to " + mPduSessionId);
        }

        // Set the link status
        if (mLinkStatus != response.getLinkStatus()) {
            mLinkStatus = response.getLinkStatus();
            log("Link status updated to " + DataUtils.linkStatusToString(mLinkStatus));
            if (isConnected()) {
                // If the data network is in a transition state, the link status will be notified
                // upon entering connected or disconnected state. If the data network is already
                // connected, send the updated link status from the updated data call response.
                mDataNetworkCallback.invokeFromExecutor(() -> mDataNetworkCallback
                        .onLinkStatusChanged(DataNetwork.this, mLinkStatus));
            }
        }

        // Set link addresses
        if (!response.getAddresses().isEmpty()) {
            for (LinkAddress la : response.getAddresses()) {
                if (!la.getAddress().isAnyLocalAddress()) {
                    logv("addr/pl=" + la.getAddress() + "/" + la.getPrefixLength());
                    linkProperties.addLinkAddress(la);
                }
            }
        } else {
            loge("no address for ifname=" + response.getInterfaceName());
        }

        // Set DNS servers
        if (!response.getDnsAddresses().isEmpty()) {
            for (InetAddress dns : response.getDnsAddresses()) {
                if (!dns.isAnyLocalAddress()) {
                    linkProperties.addDnsServer(dns);
                }
            }
        } else {
            loge("Empty dns response");
        }

        // Set PCSCF
        if (!response.getPcscfAddresses().isEmpty()) {
            for (InetAddress pcscf : response.getPcscfAddresses()) {
                linkProperties.addPcscfServer(pcscf);
            }
        }

        // For backwards compatibility, use getMtu() if getMtuV4() is not available.
        int mtuV4 = response.getMtuV4() > 0 ? response.getMtuV4() : response.getMtu();

        if (mtuV4 <= 0) {
            // Use back up value from data profile.
            if (mDataProfile.getApnSetting() != null) {
                mtuV4 = mDataProfile.getApnSetting().getMtuV4();
            }
            if (mtuV4 <= 0) {
                mtuV4 = mDataConfigManager.getDefaultMtu();
            }
        }

        // For backwards compatibility, use getMtu() if getMtuV6() is not available.
        int mtuV6 = response.getMtuV6() > 0 ? response.getMtuV6() : response.getMtu();
        if (mtuV6 <= 0) {
            // Use back up value from data profile.
            if (mDataProfile.getApnSetting() != null) {
                mtuV6 = mDataProfile.getApnSetting().getMtuV6();
            }
            if (mtuV6 <= 0) {
                mtuV6 = mDataConfigManager.getDefaultMtu();
            }
        }

        // Set MTU for each route.
        for (InetAddress gateway : response.getGatewayAddresses()) {
            int mtu = gateway instanceof java.net.Inet6Address ? mtuV6 : mtuV4;
            linkProperties.addRoute(new RouteInfo(null, gateway, null,
                    RouteInfo.RTN_UNICAST, mtu));
        }

        // LinkProperties.setMtu should be deprecated. The mtu for each route has been already
        // provided in addRoute() above. For backwards compatibility, we still need to provide
        // a value for the legacy MTU. Use the higher value of v4 and v6 value here.
        linkProperties.setMtu(Math.max(mtuV4, mtuV6));

        if (mDataProfile.getApnSetting() != null
                && !TextUtils.isEmpty(mDataProfile.getApnSetting().getProxyAddressAsString())) {
            int port = mDataProfile.getApnSetting().getProxyPort();
            if (port == -1) {
                port = 8080;
            }
            ProxyInfo proxy = ProxyInfo.buildDirectProxy(
                    mDataProfile.getApnSetting().getProxyAddressAsString(), port);
            linkProperties.setHttpProxy(proxy);
        }

        linkProperties.setTcpBufferSizes(mTcpBufferSizes);

        mNetworkSliceInfo = response.getSliceInfo();

        mTrafficDescriptors.clear();
        mTrafficDescriptors.addAll(response.getTrafficDescriptors());

        mDefaultQos = response.getDefaultQos();


        Set<QosBearerSession> newSessions = new HashSet<>(response.getQosBearerSessions());
        if (newSessions.size() != mQosBearerSessions.size()
                || !newSessions.containsAll(mQosBearerSessions)) {
            mDataNetworkCallback.onQosSessionsChanged(response.getQosBearerSessions());
        }
        mQosBearerSessions.clear();
        mQosBearerSessions.addAll(response.getQosBearerSessions());
        if (mQosCallbackTracker != null) {
            mQosCallbackTracker.updateSessions(mQosBearerSessions);
        }

        if (!linkProperties.equals(mLinkProperties)) {
            // If the new link properties is not compatible (e.g. IP changes, interface changes),
            // then we should de-register the network agent and re-create a new one.
            if ((isConnected() || isHandoverInProgress())
                    && !isLinkPropertiesCompatible(mLinkProperties, linkProperties)) {
                logl("updateDataNetwork: Incompatible link properties detected. Re-create the "
                        + "network agent. Changed from " + mLinkProperties + " to "
                        + linkProperties);

                mLinkProperties = linkProperties;
                recreateNetworkAgent();
            } else {
                mLinkProperties = linkProperties;
                log("sendLinkProperties " + mLinkProperties);
                mNetworkAgent.sendLinkProperties(mLinkProperties);
            }
        }

        updateNetworkCapabilities();
        updateValidationStatus(response.getNetworkValidationStatus());
    }

    /**
     * If the {@link DataCallResponse} contains invalid info, triggers an anomaly report.
     *
     * @param response The response to be validated
     * @param setupRegState Registration state if the response is for initial data call setup.
     */
    private void validateDataCallResponse(@Nullable DataCallResponse response,
            @NetworkRegistrationInfo.RegistrationState int setupRegState) {
        if (response == null
                || response.getLinkStatus() == DataCallResponse.LINK_STATUS_INACTIVE) return;
        int failCause = response.getCause();
        if (failCause == DataFailCause.NONE) {
            if (TextUtils.isEmpty(response.getInterfaceName())
                    || response.getAddresses().isEmpty()
                    // if out of range
                    || response.getLinkStatus() < DataCallResponse.LINK_STATUS_UNKNOWN
                    || response.getLinkStatus() > DataCallResponse.LINK_STATUS_ACTIVE
                    || response.getProtocolType() < ApnSetting.PROTOCOL_UNKNOWN
                    || response.getProtocolType() > ApnSetting.PROTOCOL_UNSTRUCTURED
                    || response.getHandoverFailureMode()
                    < DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN
                    || response.getHandoverFailureMode()
                    > DataCallResponse.HANDOVER_FAILURE_MODE_NO_FALLBACK_RETRY_SETUP_NORMAL) {
                loge("Invalid DataCallResponse:" + response);
                reportAnomaly("Invalid DataCallResponse detected",
                        "1f273e9d-b09c-46eb-ad1c-421d01f61164");
            }
            // Check IP for initial setup response
            NetworkRegistrationInfo nri = getNetworkRegistrationInfo();
            if (setupRegState != -1 // Is setup response
                    && mDataProfile.getApnSetting() != null && nri != null && nri.isInService()) {
                boolean wasRoaming = setupRegState
                        == NetworkRegistrationInfo.REGISTRATION_STATE_ROAMING;
                boolean isRoaming = nri.getNetworkRegistrationState()
                        == NetworkRegistrationInfo.REGISTRATION_STATE_ROAMING;
                if (wasRoaming == isRoaming) { // Ignore check if in race condition.
                    int protocol = isRoaming ? mDataProfile.getApnSetting().getRoamingProtocol()
                            : mDataProfile.getApnSetting().getProtocol();
                    String underlyingDataService = mTransport
                            == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                            ? "RIL" : "IWLAN data service";
                    if (protocol == ApnSetting.PROTOCOL_IP) {
                        if (response.getAddresses().stream().noneMatch(
                                la -> la.getAddress() instanceof java.net.Inet4Address)) {
                            loge("Invalid DataCallResponse. Requested IPv4 but didn't get an "
                                    + "IPv4 address." + response);
                            reportAnomaly(underlyingDataService + " reported mismatched IP "
                                    + "type. Requested IPv4 but didn't get an IPv4 "
                                    + "address.", "7744f920-fb64-4db0-ba47-de0eae485a82");
                        }
                    } else if (protocol == ApnSetting.PROTOCOL_IPV6) {
                        if (response.getAddresses().stream().noneMatch(
                                la -> la.getAddress() instanceof java.net.Inet6Address)) {
                            loge("Invalid DataCallResponse. Requested IPv6 but didn't get an "
                                    + "IPv6 address." + response);
                            reportAnomaly(underlyingDataService + " reported mismatched IP "
                                    + "type. Requested IPv6 but didn't get an IPv6 "
                                    + "address.", "7744f920-fb64-4db0-ba47-de0eae485a82");
                        }
                    }
                }
            }
        } else if (!DataFailCause.isFailCauseExisting(failCause)) { // Setup data failed.
            loge("Invalid DataFailCause in " + response);
            reportAnomaly("Invalid DataFailCause: (0x" + Integer.toHexString(failCause)
                            + ")",
                    "6b264f28-9f58-4cbd-9e0e-d7624ba30879");
        }
    }

    /**
     * Called when receiving deactivate data network response from the data service.
     *
     * @param resultCode The result code.
     */
    private void onDeactivateResponse(@DataServiceCallback.ResultCode int resultCode) {
        logl("onDeactivateResponse: resultCode="
                + DataServiceCallback.resultCodeToString(resultCode));
        if (resultCode == DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE) {
            log("Remove network since deactivate request returned an error.");
            mFailCause = DataFailCause.RADIO_NOT_AVAILABLE;
            transitionTo(mDisconnectedState);
        } else if (mPhone.getHalVersion(HAL_SERVICE_DATA).less(RIL.RADIO_HAL_VERSION_2_0)) {
            log("Remove network on deactivate data response on old HAL "
                    + mPhone.getHalVersion(HAL_SERVICE_DATA));
            mFailCause = DataFailCause.LOST_CONNECTION;
            transitionTo(mDisconnectedState);
        }
    }

    /**
     * Tear down the data network immediately.
     *
     * @param reason The reason of tearing down the network.
     */
    public void tearDown(@TearDownReason int reason) {
        if (getCurrentState() == null || isDisconnected()) {
            return;
        }
        mTearDownReason = reason;
        sendMessage(obtainMessage(EVENT_TEAR_DOWN_NETWORK, reason));
    }

    private void onTearDown(@TearDownReason int reason) {
        logl("onTearDown: reason=" + tearDownReasonToString(reason));

        // track frequent NetworkAgent.onNetworkUnwanted() call of IMS and INTERNET
        if (reason == TEAR_DOWN_REASON_CONNECTIVITY_SERVICE_UNWANTED
                && isConnected()
                && (mNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_IMS)
                || mNetworkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET))) {
            mDataNetworkCallback.onTrackNetworkUnwanted(this);
        }

        mDataServiceManagers.get(mTransport).deactivateDataCall(mCid.get(mTransport),
                reason == TEAR_DOWN_REASON_AIRPLANE_MODE_ON ? DataService.REQUEST_REASON_SHUTDOWN
                        : DataService.REQUEST_REASON_NORMAL,
                obtainMessage(EVENT_DEACTIVATE_DATA_NETWORK_RESPONSE));
        mDataCallSessionStats.setDeactivateDataCallReason(reason);
        mInvokedDataDeactivation = true;
    }

    /**
     * @return {@code true} if we shall delay tear down this network because an active voice call is
     * relying on it and
     * {@link CarrierConfigManager#KEY_DELAY_IMS_TEAR_DOWN_UNTIL_CALL_END_BOOL} is enabled.
     */
    public boolean shouldDelayImsTearDownDueToInCall() {
        return mDataConfigManager.isImsDelayTearDownUntilVoiceCallEndEnabled()
                && mNetworkCapabilities != null
                && mNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_IMS)
                && mPhone.getImsPhone() != null
                && mPhone.getImsPhone().getCallTracker().getState()
                != PhoneConstants.State.IDLE;
    }

    /**
     * Tear down the data network when condition is met or timed out. Data network will enter
     * {@link DisconnectingState} immediately and waiting for condition met. When condition is met,
     * {@link DataNetworkController} should invoke {@link Consumer#accept(Object)} so the actual
     * tear down work can be performed.
     *
     * This is primarily used for IMS graceful tear down. {@link DataNetworkController} inform
     * {@link DataNetwork} to enter {@link DisconnectingState}. IMS service can observe this
     * through {@link PreciseDataConnectionState#getState()} and then perform IMS de-registration
     * work. After IMS de-registered, {@link DataNetworkController} informs {@link DataNetwork}
     * that it's okay to tear down the network.
     *
     * @param reason The tear down reason.
     *
     * @param timeoutMillis Timeout in milliseconds. Within the time window, clients will have to
     * call {@link Consumer#accept(Object)}, otherwise, data network will be torn down when
     * timed out.
     *
     * @return The runnable for client to execute when condition is met. When executed, tear down
     * will be performed. {@code null} if the data network is already disconnected or being
     * disconnected.
     */
    @Nullable
    public Runnable tearDownWhenConditionMet(@TearDownReason int reason, long timeoutMillis) {
        if (getCurrentState() == null || isDisconnected() || isDisconnecting()) {
            loge("tearDownWhenConditionMet: Not in the right state. State=" + getCurrentState());
            return null;
        }
        logl("tearDownWhenConditionMet: reason=" + tearDownReasonToString(reason) + ", timeout="
                + timeoutMillis + "ms.");
        sendMessage(EVENT_WAITING_FOR_TEARING_DOWN_CONDITION_MET, reason, (int) timeoutMillis);
        return () -> this.tearDown(reason);
    }

    /**
     * Called when receiving {@link DataServiceCallback#onDataCallListChanged(List)} from the data
     * service.
     *
     * @param transport The transport where this event from.
     * @param responseList The data call response list.
     */
    private void onDataStateChanged(@TransportType int transport,
            @NonNull List<DataCallResponse> responseList) {
        // Ignore the update if it's not from the data service on the right transport.
        // Also if never received data call response from setup call response, which updates the
        // cid, ignore the update here.
        logv("onDataStateChanged: " + responseList);
        if (transport != mTransport || mCid.get(mTransport) == INVALID_CID || isDisconnected()) {
            return;
        }

        DataCallResponse response = responseList.stream()
                .filter(r -> mCid.get(mTransport) == r.getId())
                .findFirst()
                .orElse(null);
        if (response != null) {
            if (!response.equals(mDataCallResponse)) {
                log("onDataStateChanged: " + response);
                validateDataCallResponse(response, -1 /*setupRegState setup only*/);
                mDataCallResponse = response;
                if (response.getLinkStatus() != DataCallResponse.LINK_STATUS_INACTIVE) {
                    updateDataNetwork(response);
                    notifyPreciseDataConnectionState();
                } else {
                    log("onDataStateChanged: PDN inactive reported by "
                            + AccessNetworkConstants.transportTypeToString(mTransport)
                            + " data service.");
                    mFailCause = mEverConnected ? response.getCause()
                            : DataFailCause.NO_RETRY_FAILURE;
                    mRetryDelayMillis = DataCallResponse.RETRY_DURATION_UNDEFINED;
                    transitionTo(mDisconnectedState);
                }
            }
        } else {
            // The data call response is missing from the list. This means the PDN is gone. This
            // is the PDN lost reported by the modem. We don't send another DEACTIVATE_DATA request
            // for that
            log("onDataStateChanged: PDN disconnected reported by "
                    + AccessNetworkConstants.transportTypeToString(mTransport) + " data service.");
            mFailCause = mEverConnected ? DataFailCause.LOST_CONNECTION
                    : DataFailCause.NO_RETRY_FAILURE;
            mRetryDelayMillis = DataCallResponse.RETRY_DURATION_UNDEFINED;
            transitionTo(mDisconnectedState);
        }
    }

    /**
     * Called when carrier config updated.
     */
    private void onCarrierConfigUpdated() {
        log("onCarrierConfigUpdated");

        updateBandwidthFromDataConfig();
        updateTcpBufferSizes();
        updateMeteredAndCongested();
    }

    /**
     * Called when receiving bandwidth update from the modem.
     *
     * @param linkCapacityEstimates The link capacity estimate list from the modem.
     */
    private void onBandwidthUpdatedFromModem(
            @NonNull List<LinkCapacityEstimate> linkCapacityEstimates) {
        Objects.requireNonNull(linkCapacityEstimates);
        if (linkCapacityEstimates.isEmpty()) return;

        int uplinkBandwidthKbps = 0, downlinkBandwidthKbps = 0;
        for (LinkCapacityEstimate linkCapacityEstimate : linkCapacityEstimates) {
            if (linkCapacityEstimate.getType() == LinkCapacityEstimate.LCE_TYPE_COMBINED) {
                uplinkBandwidthKbps = linkCapacityEstimate.getUplinkCapacityKbps();
                downlinkBandwidthKbps = linkCapacityEstimate.getDownlinkCapacityKbps();
                break;
            } else if (linkCapacityEstimate.getType() == LinkCapacityEstimate.LCE_TYPE_PRIMARY
                    || linkCapacityEstimate.getType() == LinkCapacityEstimate.LCE_TYPE_SECONDARY) {
                uplinkBandwidthKbps += linkCapacityEstimate.getUplinkCapacityKbps();
                downlinkBandwidthKbps += linkCapacityEstimate.getDownlinkCapacityKbps();
            } else {
                loge("Invalid LinkCapacityEstimate type " + linkCapacityEstimate.getType());
            }
        }
        onBandwidthUpdated(uplinkBandwidthKbps, downlinkBandwidthKbps);
    }

    /**
     * Called when bandwidth estimation updated from either modem or the bandwidth estimator.
     *
     * @param uplinkBandwidthKbps Uplink bandwidth estimate in Kbps.
     * @param downlinkBandwidthKbps Downlink bandwidth estimate in Kbps.
     */
    private void onBandwidthUpdated(int uplinkBandwidthKbps, int downlinkBandwidthKbps) {
        log("onBandwidthUpdated: downlinkBandwidthKbps=" + downlinkBandwidthKbps
                + ", uplinkBandwidthKbps=" + uplinkBandwidthKbps);
        NetworkBandwidth bandwidthFromConfig = mDataConfigManager.getBandwidthForNetworkType(
                mTelephonyDisplayInfo);

        if (downlinkBandwidthKbps == LinkCapacityEstimate.INVALID) {
            // Fallback to carrier config.
            downlinkBandwidthKbps = bandwidthFromConfig.downlinkBandwidthKbps;
        }

        if (uplinkBandwidthKbps == LinkCapacityEstimate.INVALID) {
            // Fallback to carrier config.
            uplinkBandwidthKbps = bandwidthFromConfig.uplinkBandwidthKbps;
        }

        // Make sure uplink is not greater than downlink.
        uplinkBandwidthKbps = Math.min(uplinkBandwidthKbps, downlinkBandwidthKbps);
        mNetworkBandwidth = new NetworkBandwidth(downlinkBandwidthKbps, uplinkBandwidthKbps);

        updateNetworkCapabilities();
    }

    /**
     * Called when {@link TelephonyDisplayInfo} changed. This can happen when network types or
     * override network types (5G NSA, 5G MMWAVE) change.
     */
    private void onDisplayInfoChanged() {
        mTelephonyDisplayInfo = mPhone.getDisplayInfoController().getTelephonyDisplayInfo();
        updateBandwidthFromDataConfig();
        updateTcpBufferSizes();
        updateMeteredAndCongested();
    }

    /**
     * Update the bandwidth from carrier config. Note this is no-op if the bandwidth source is not
     * carrier config.
     */
    private void updateBandwidthFromDataConfig() {
        if (mDataConfigManager.getBandwidthEstimateSource() != BANDWIDTH_SOURCE_CARRIER_CONFIG) {
            return;
        }
        log("updateBandwidthFromDataConfig");
        mNetworkBandwidth = mDataConfigManager.getBandwidthForNetworkType(mTelephonyDisplayInfo);
        updateNetworkCapabilities();
    }

    /**
     * Update the TCP buffer sizes from resource overlays.
     */
    private void updateTcpBufferSizes() {
        log("updateTcpBufferSizes");
        mTcpBufferSizes = mDataConfigManager.getTcpConfigString(mTelephonyDisplayInfo);
        LinkProperties linkProperties = new LinkProperties(mLinkProperties);
        linkProperties.setTcpBufferSizes(mTcpBufferSizes);
        if (!linkProperties.equals(mLinkProperties)) {
            mLinkProperties = linkProperties;
            log("sendLinkProperties " + mLinkProperties);
            mNetworkAgent.sendLinkProperties(mLinkProperties);
        }
    }

    /**
     * Update the metered and congested values from carrier configs and subscription overrides
     */
    private void updateMeteredAndCongested() {
        int networkType = mTelephonyDisplayInfo.getNetworkType();
        switch (mTelephonyDisplayInfo.getOverrideNetworkType()) {
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED:
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA:
                networkType = TelephonyManager.NETWORK_TYPE_NR;
                break;
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO:
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA:
                networkType = TelephonyManager.NETWORK_TYPE_LTE_CA;
                break;
        }
        log("updateMeteredAndCongested: networkType="
                + TelephonyManager.getNetworkTypeName(networkType));
        boolean changed = false;
        if (mDataConfigManager.isTempNotMeteredSupportedByCarrier() != mTempNotMeteredSupported) {
            mTempNotMeteredSupported = !mTempNotMeteredSupported;
            changed = true;
            log("updateMeteredAndCongested: mTempNotMeteredSupported changed to "
                    + mTempNotMeteredSupported);
        }
        boolean isTempNotMetered = mDataConfigManager.isNetworkTypeUnmetered(
                mTelephonyDisplayInfo, mPhone.getServiceState())
                && (mDataNetworkController.getUnmeteredOverrideNetworkTypes().contains(networkType)
                || isNetworkTypeUnmetered(networkType));
        if (isTempNotMetered != mTempNotMetered) {
            mTempNotMetered = isTempNotMetered;
            changed = true;
            log("updateMeteredAndCongested: mTempNotMetered changed to " + mTempNotMetered);
        }
        if (mDataNetworkController.getCongestedOverrideNetworkTypes().contains(networkType)
                != mCongested) {
            mCongested = !mCongested;
            changed = true;
            log("updateMeteredAndCongested: mCongested changed to " + mCongested);
        }
        if (changed) {
            updateNetworkCapabilities();
        }
        if (mTempNotMetered && isInternetSupported()) {
            // NR NSA and NR have the same network type: NR
            mDataCallSessionStats.onUnmeteredUpdate(networkType);
        }
    }

    /**
     * Get whether the network type is unmetered from SubscriptionPlans, from either an unmetered
     * general plan or specific plan for the given network type.
     *
     * @param networkType The network type to check meteredness for
     * @return Whether the given network type is unmetered based on SubscriptionPlans
     */
    private boolean isNetworkTypeUnmetered(@NetworkType int networkType) {
        List<SubscriptionPlan> plans = mDataNetworkController.getSubscriptionPlans();
        if (plans.isEmpty()) return false;
        boolean isGeneralUnmetered = true;
        Set<Integer> allNetworkTypes = Arrays.stream(TelephonyManager.getAllNetworkTypes())
                .boxed().collect(Collectors.toSet());
        for (SubscriptionPlan plan : plans) {
            // Check if plan is general (applies to all network types) or specific
            if (Arrays.stream(plan.getNetworkTypes()).boxed().collect(Collectors.toSet())
                    .containsAll(allNetworkTypes)) {
                if (plan.getDataLimitBytes() != SubscriptionPlan.BYTES_UNLIMITED) {
                    // Metered takes precedence over unmetered for safety
                    isGeneralUnmetered = false;
                }
            } else {
                // Check if plan applies to given network type
                if (networkType != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                    for (int planNetworkType : plan.getNetworkTypes()) {
                        if (planNetworkType == networkType) {
                            return plan.getDataLimitBytes() == SubscriptionPlan.BYTES_UNLIMITED;
                        }
                    }
                }
            }
        }
        return isGeneralUnmetered;
    }

    /**
     * @return The unique context id assigned by the data service in
     * {@link DataCallResponse#getId()}.
     */
    public int getId() {
        return mCid.get(mTransport);
    }

    /**
     * @return The current network type reported by the network service.
     */
    @NetworkType
    private int getDataNetworkType() {
        return getDataNetworkType(mTransport);
    }

    /**
     * Get the data network type on the specified transport.
     *
     * @param transport The transport.
     * @return The data network type.
     */
    @NetworkType
    private int getDataNetworkType(@TransportType int transport) {
        // WLAN transport can't have network type other than IWLAN. Ideally service state tracker
        // should report the correct RAT, but sometimes race condition could happen that service
        // state is reset to out of service and RAT not updated to IWLAN yet.
        if (transport == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
            return TelephonyManager.NETWORK_TYPE_IWLAN;
        }

        ServiceState ss = mPhone.getServiceState();
        NetworkRegistrationInfo nrs = ss.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, transport);
        if (nrs != null) {
            return nrs.getAccessNetworkTechnology();
        }
        return TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }

    /**
     * @return The physical link status (i.e. RRC state).
     */
    @LinkStatus
    public int getLinkStatus() {
        return mLinkStatus;
    }

    /**
     * Update the network score and report to connectivity service if necessary.
     */
    private void updateNetworkScore() {
        updateNetworkScore(isHandoverInProgress());
    }

    /**
     * Update the network score and report to connectivity service if necessary.
     *
     * @param keepConnectedForHandover indicate handover is in progress or not.
     */
    private void updateNetworkScore(boolean keepConnectedForHandover) {
        int connectedReason = keepConnectedForHandover
                ? NetworkScore.KEEP_CONNECTED_FOR_HANDOVER : NetworkScore.KEEP_CONNECTED_NONE;
        if (mNetworkScore.getKeepConnectedReason() != connectedReason
                || mNetworkScore.isTransportPrimary() != mOnPreferredDataPhone) {
            NetworkScore.Builder builder = new NetworkScore.Builder()
                    .setKeepConnectedReason(connectedReason);
            builder.setTransportPrimary(mOnPreferredDataPhone);
            mNetworkScore = builder.build();
            mNetworkAgent.sendNetworkScore(mNetworkScore);
            logl("updateNetworkScore: isPrimary=" + mNetworkScore.isTransportPrimary()
                    + ", keepConnectedForHandover=" + keepConnectedForHandover);
        }
    }

    /**
     * @return Network registration info on the current transport.
     */
    @Nullable
    private NetworkRegistrationInfo getNetworkRegistrationInfo() {
        NetworkRegistrationInfo nri = mPhone.getServiceStateTracker().getServiceState()
                .getNetworkRegistrationInfo(NetworkRegistrationInfo.DOMAIN_PS, mTransport);
        if (nri == null) {
            loge("Can't get network registration info for "
                    + AccessNetworkConstants.transportTypeToString(mTransport));
            return null;
        }
        return nri;
    }

    /**
     * Get the APN type network capability. If there are more than one capabilities that are
     * APN types, then return the highest priority one which also has associated network request.
     * For example, if the network supports both MMS and internet, but only internet request
     * attached at this time, then the capability would be internet. Later on if MMS network request
     * attached to this network, then the APN type capability would be MMS.
     *
     * @return The APN type network capability from this network.
     *
     * @see #getPriority()
     */
    @NetCapability
    public int getApnTypeNetworkCapability() {
        if (!mAttachedNetworkRequestList.isEmpty()) {
            // The highest priority network request is always at the top of list.
            return mAttachedNetworkRequestList.get(0).getHighestPriorityApnTypeNetworkCapability();
        } else {
            return Arrays.stream(getNetworkCapabilities().getCapabilities()).boxed()
                    .filter(cap -> DataUtils.networkCapabilityToApnType(cap)
                            != ApnSetting.TYPE_NONE)
                    .max(Comparator.comparingInt(mDataConfigManager::getNetworkCapabilityPriority))
                    .orElse(-1);
        }
    }

    /**
     * Get the priority of the network. The priority is derived from the highest priority capability
     * which also has such associated network request. For example, if the network supports both
     * MMS and internet, but only has internet request attached, then this network has internet's
     * priority. Later on when the MMS request attached to this network, the network's priority will
     * be updated to MMS's priority.
     *
     * @return The priority of the network.
     *
     * @see #getApnTypeNetworkCapability()
     */
    public int getPriority() {
        if (!mAttachedNetworkRequestList.isEmpty()) {
            // The highest priority network request is always at the top of list.
            return mAttachedNetworkRequestList.get(0).getPriority();
        } else {
            // If all network requests are already detached, then just pick the highest priority
            // capability's priority.
            return Arrays.stream(getNetworkCapabilities().getCapabilities()).boxed()
                    .map(mDataConfigManager::getNetworkCapabilityPriority)
                    .max(Integer::compare)
                    .orElse(0);
        }
    }

    /**
     * @return The attached network request list.
     */
    @NonNull
    public NetworkRequestList getAttachedNetworkRequestList() {
        return mAttachedNetworkRequestList;
    }

    /**
     * @return {@code true} if in connecting state.
     */
    public boolean isConnecting() {
        return getCurrentState() == mConnectingState;
    }

    /**
     * @return {@code true} if in connected state.
     */
    public boolean isConnected() {
        return getCurrentState() == mConnectedState;
    }

    /**
     * @return {@code true} if in disconnecting state.
     */
    public boolean isDisconnecting() {
        return getCurrentState() == mDisconnectingState;
    }

    /**
     * @return {@code true} if in disconnected state.
     */
    public boolean isDisconnected() {
        return getCurrentState() == mDisconnectedState;
    }

    /**
     * @return {@code true} if in handover state.
     */
    public boolean isHandoverInProgress() {
        return getCurrentState() == mHandoverState;
    }

    /**
     * @return {@code true} if the data network is suspended.
     */
    public boolean isSuspended() {
        return getState() == TelephonyManager.DATA_SUSPENDED;
    }

    /**
     * @return The current transport of the data network.
     */
    @TransportType
    public int getTransport() {
        return mTransport;
    }

    @DataState
    private int getState() {
        IState state = getCurrentState();
        if (state == null || isDisconnected()) {
            return TelephonyManager.DATA_DISCONNECTED;
        } else if (isConnecting()) {
            return TelephonyManager.DATA_CONNECTING;
        } else if (isConnected()) {
            // The data connection can only be suspended when it's in active state.
            if (mSuspended) {
                return TelephonyManager.DATA_SUSPENDED;
            }
            return TelephonyManager.DATA_CONNECTED;
        } else if (isDisconnecting()) {
            return TelephonyManager.DATA_DISCONNECTING;
        } else if (isHandoverInProgress()) {
            return TelephonyManager.DATA_HANDOVER_IN_PROGRESS;
        }

        return TelephonyManager.DATA_UNKNOWN;
    }

    /**
     * @return {@code true} if this data network supports internet.
     */
    public boolean isInternetSupported() {
        return mNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * @return {@code true} if this network was setup for SUPL during emergency call. {@code false}
     * otherwise.
     */
    public boolean isEmergencySupl() {
        return mDataAllowedReason == DataAllowedReason.EMERGENCY_SUPL;
    }

    /**
     * Get precise data connection state
     *
     * @return The {@link PreciseDataConnectionState}
     */
    private PreciseDataConnectionState getPreciseDataConnectionState() {
        return new PreciseDataConnectionState.Builder()
                .setTransportType(mTransport)
                .setId(mCid.get(mTransport))
                .setNetworkAgentId(mNetworkAgent.getId())
                .setState(getState())
                .setApnSetting(mDataProfile.getApnSetting())
                .setLinkProperties(mLinkProperties)
                .setNetworkType(getDataNetworkType())
                .setFailCause(mFailCause)
                .setDefaultQos(mDefaultQos)
                .setNetworkValidationStatus(mNetworkValidationStatus)
                .build();
    }

    /**
     * Send the precise data connection state to the listener of
     * {@link android.telephony.TelephonyCallback.PreciseDataConnectionStateListener}.
     * <p>
     * Note that notify only when {@link DataState} or {@link
     * PreciseDataConnectionState.NetworkValidationStatus} or {@link TelephonyNetworkAgent#getId}
     * changes.
     */
    private void notifyPreciseDataConnectionState() {
        PreciseDataConnectionState pdcs = getPreciseDataConnectionState();
        if (mPreciseDataConnectionState == null
                || mPreciseDataConnectionState.getState() != pdcs.getState()
                || mPreciseDataConnectionState.getNetworkValidationStatus()
                        != pdcs.getNetworkValidationStatus()
                || mPreciseDataConnectionState.getNetId() != pdcs.getNetId()) {
            mPreciseDataConnectionState = pdcs;
            logv("notifyPreciseDataConnectionState=" + pdcs);
            mPhone.notifyDataConnection(pdcs);
        }
    }

    /**
     * Request the data network to handover to the target transport.
     * <p>
     * This is the starting point of initiating IWLAN/cellular handover. It will first call
     * {@link DataServiceManager#startHandover(int, Message)} to notify source transport that
     * handover is about to start, and then call {@link DataServiceManager#setupDataCall(int,
     * DataProfile, boolean, boolean, int, LinkProperties, int, NetworkSliceInfo, TrafficDescriptor,
     * boolean, Message)} on target transport to initiate the handover process.
     *
     * @param targetTransport The target transport.
     * @param retryEntry Data handover retry entry. This would be {@code null} for first time
     * handover attempt.
     * @return {@code true} if the request has been accepted.
     */
    public boolean startHandover(@TransportType int targetTransport,
            @Nullable DataHandoverRetryEntry retryEntry) {
        if (getCurrentState() == null || isDisconnected() || isDisconnecting()) {
            // Fail the request if not in the appropriate state.
            if (retryEntry != null) retryEntry.setState(DataRetryEntry.RETRY_STATE_CANCELLED);
            return false;
        }

        // Before we really initiate the handover process on target transport, we need to notify
        // source transport that handover is about to start. Handover will be eventually initiated
        // in onStartHandover().
        sendMessage(obtainMessage(EVENT_NOTIFY_HANDOVER_STARTED, 0, targetTransport, retryEntry));
        return true;
    }

    /**
     * Called when starting IWLAN/cellular handover process on the target transport.
     *
     * @param targetTransport The target transport.
     * @param retryEntry Data handover retry entry. This would be {@code null} for first time
     * handover attempt.
     */
    private void onStartHandover(@TransportType int targetTransport,
            @Nullable DataHandoverRetryEntry retryEntry) {
        if (mTransport == targetTransport) {
            log("onStartHandover: The network is already on "
                    + AccessNetworkConstants.transportTypeToString(mTransport)
                    + ", handover is not needed.");
            if (retryEntry != null) retryEntry.setState(DataRetryEntry.RETRY_STATE_CANCELLED);
            return;
        }

        // We need to use the actual modem roaming state instead of the framework roaming state
        // here. This flag is only passed down to ril_service for picking the correct protocol (for
        // old modem backward compatibility).
        boolean isModemRoaming = mPhone.getServiceState().getDataRoamingFromRegistration();

        // Set this flag to true if the user turns on data roaming. Or if we override the roaming
        // state in framework, we should set this flag to true as well so the modem will not reject
        // the data call setup (because the modem actually thinks the device is roaming).
        boolean allowRoaming = mPhone.getDataRoamingEnabled()
                || (isModemRoaming && (!mPhone.getServiceState().getDataRoaming()));

        mHandoverDataProfile = mDataProfile;
        int targetNetworkType = getDataNetworkType(targetTransport);
        if (targetNetworkType != TelephonyManager.NETWORK_TYPE_UNKNOWN
                && !mAttachedNetworkRequestList.isEmpty()) {
            TelephonyNetworkRequest networkRequest = mAttachedNetworkRequestList.get(0);
            DataProfile dataProfile = mDataNetworkController.getDataProfileManager()
                    .getDataProfileForNetworkRequest(networkRequest, targetNetworkType,
                            mPhone.getServiceState().isUsingNonTerrestrialNetwork(),
                            mDataNetworkController.isEsimBootStrapProvisioningActivated(), false);
            // Some carriers have different profiles between cellular and IWLAN. We need to
            // dynamically switch profile, but only when those profiles have same APN name.
            if (dataProfile != null && dataProfile.getApnSetting() != null
                    && mDataProfile.getApnSetting() != null
                    && TextUtils.equals(dataProfile.getApnSetting().getApnName(),
                    mDataProfile.getApnSetting().getApnName())
                    && !dataProfile.equals(mDataProfile)) {
                mHandoverDataProfile = dataProfile;
                log("Used different data profile for handover. " + mDataProfile);
            }
        }

        logl("Start handover from " + AccessNetworkConstants.transportTypeToString(mTransport)
                + " to " + AccessNetworkConstants.transportTypeToString(targetTransport));
        // Send the handover request to the target transport data service.
        mDataServiceManagers.get(targetTransport).setupDataCall(
                DataUtils.networkTypeToAccessNetworkType(getDataNetworkType(targetTransport)),
                mHandoverDataProfile, isModemRoaming, allowRoaming,
                DataService.REQUEST_REASON_HANDOVER, mLinkProperties, mPduSessionId,
                mNetworkSliceInfo, mHandoverDataProfile.getTrafficDescriptor(), true,
                obtainMessage(EVENT_HANDOVER_RESPONSE, retryEntry));

        mDataNetworkValidationStats.onHandoverAttempted();
    }

    /**
     * Called when receiving handover response from the data service.
     *
     * @param resultCode The result code.
     * @param response The response.
     * @param retryEntry Data handover retry entry. This would be {@code null} for first time
     * handover attempt.
     */
    private void onHandoverResponse(@DataServiceCallback.ResultCode int resultCode,
            @Nullable DataCallResponse response, @Nullable DataHandoverRetryEntry retryEntry) {
        logl("onHandoverResponse: resultCode=" + DataServiceCallback.resultCodeToString(resultCode)
                + ", response=" + response);
        mFailCause = getFailCauseFromDataCallResponse(resultCode, response);
        validateDataCallResponse(response, -1 /*setupRegState setup only*/);
        if (mFailCause == DataFailCause.NONE) {
            // Handover succeeded.

            // Clean up on the source transport.
            mDataServiceManagers.get(mTransport).deactivateDataCall(mCid.get(mTransport),
                    DataService.REQUEST_REASON_HANDOVER, null);
            // Switch the transport to the target.
            mTransport = DataUtils.getTargetTransport(mTransport);
            // Update the logging tag
            mLogTag = "DN-" + mInitialNetworkAgentId + "-"
                    + ((mTransport == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) ? "C" : "I");
            // Switch the data profile. This is no-op in most of the case since almost all carriers
            // use same data profile between IWLAN and cellular.
            mDataProfile = mHandoverDataProfile;
            updateDataNetwork(response);
            if (mTransport != AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                unregisterForWwanEvents();
            } else {
                // Handover from WLAN to WWAN
                registerForWwanEvents();
            }
            if (retryEntry != null) retryEntry.setState(DataRetryEntry.RETRY_STATE_SUCCEEDED);
            mDataNetworkCallback.invokeFromExecutor(
                    () -> mDataNetworkCallback.onHandoverSucceeded(DataNetwork.this));
        } else {
            // Handover failed.

            // Notify source transport that handover failed on target transport so that PDU session
            // id can be released if it is preserved for handover.
            mDataServiceManagers.get(mTransport).cancelHandover(mCid.get(mTransport),
                    obtainMessage(EVENT_NOTIFY_HANDOVER_CANCELLED_RESPONSE));
            sendMessageDelayed(EVENT_CANCEL_HANDOVER_NO_RESPONSE,
                    mDataConfigManager.getNetworkHandoverTimeoutMs());

            long retry = response != null ? response.getRetryDurationMillis()
                    : DataCallResponse.RETRY_DURATION_UNDEFINED;
            // If the handover mode is unspecified, default to HANDOVER_FAILURE_MODE_UNKNOWN,
            // which will retry handover if retry rules are defined.
            int handoverFailureMode = response != null ? response.getHandoverFailureMode()
                    : DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN;
            if (retryEntry != null) retryEntry.setState(DataRetryEntry.RETRY_STATE_FAILED);
            mDataNetworkCallback.invokeFromExecutor(
                    () -> mDataNetworkCallback.onHandoverFailed(DataNetwork.this,
                            mFailCause, retry, handoverFailureMode));
            trackHandoverFailure(response != null ? response.getCause() : mFailCause);
        }

        // No matter handover succeeded or not, transit back to connected state.
        transitionTo(mConnectedState);
    }

    /**
     * Called when handover failed. Record the source and target RAT{@link NetworkType} and the
     * failure cause {@link android.telephony.DataFailCause}.
     *
     * @param cause The fail cause.
     */
    private void trackHandoverFailure(int cause) {
        int sourceRat = getDataNetworkType();
        int targetTransport = DataUtils.getTargetTransport(mTransport);
        int targetRat = getDataNetworkType(targetTransport);

        mDataCallSessionStats.onHandoverFailure(cause, sourceRat, targetRat);
    }

    /**
     * Called when receiving PCO (Protocol Configuration Options) data from the cellular network.
     *
     * @param pcoData The PCO data.
     */
    private void onPcoDataChanged(@NonNull PcoData pcoData) {
        log("onPcoDataChanged: " + pcoData);
        mDataNetworkCallback.invokeFromExecutor(
                () -> mDataNetworkCallback.onPcoDataChanged(DataNetwork.this));
        if (mDataProfile.getApnSetting() != null) {
            for (int apnType : mDataProfile.getApnSetting().getApnTypes()) {
                Intent intent = new Intent(TelephonyManager.ACTION_CARRIER_SIGNAL_PCO_VALUE);
                intent.putExtra(TelephonyManager.EXTRA_APN_TYPE, apnType);
                intent.putExtra(TelephonyManager.EXTRA_APN_PROTOCOL,
                        ApnSetting.getProtocolIntFromString(pcoData.bearerProto));
                intent.putExtra(TelephonyManager.EXTRA_PCO_ID, pcoData.pcoId);
                intent.putExtra(TelephonyManager.EXTRA_PCO_VALUE, pcoData.contents);
                mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);
            }
        }
    }

    /**
     * Called when receiving PCO (Protocol Configuration Options) data from the cellular network.
     *
     * @param pcoData PCO data.
     */
    private void onPcoDataReceived(@NonNull PcoData pcoData) {
        // Save all the PCO data received, even though it might be unrelated to this data network.
        // The network might be still in connecting state. Save all now and use it when entering
        // connected state.
        log("onPcoDataReceived: " + pcoData);
        PcoData oldData = mPcoData.computeIfAbsent(pcoData.cid, m -> new ArrayMap<>())
                .put(pcoData.pcoId, pcoData);
        if (getId() == INVALID_CID || pcoData.cid != getId()) return;
        if (!Objects.equals(oldData, pcoData)) {
            onPcoDataChanged(pcoData);
        }
    }

    /**
     * @return The last known data network type of the data network.
     */
    @NetworkType
    public int getLastKnownDataNetworkType() {
        return mLastKnownDataNetworkType;
    }

    /**
     * @return The last known roaming state of this data network.
     */
    public boolean getLastKnownRoamingState() {
        return mLastKnownRoamingState;
    }

    /**
     * @return The PCO data received from the network.
     */
    @NonNull
    public Map<Integer, PcoData> getPcoData() {
        if (mTransport == AccessNetworkConstants.TRANSPORT_TYPE_WLAN
                || mCid.get(mTransport) == INVALID_CID) {
            return Collections.emptyMap();
        }
        return mPcoData.getOrDefault(mCid.get(mTransport), Collections.emptyMap());
    }

    /**
     * Check if the this data network is VCN-managed.
     *
     * @param networkCapabilities The network capabilities of this data network.
     * @return The VCN's policy for this DataNetwork.
     */
    private VcnNetworkPolicyResult getVcnPolicy(NetworkCapabilities networkCapabilities) {
        if (mVcnManager == null) {
            return null;
        }

        return mVcnManager.applyVcnNetworkPolicy(networkCapabilities, getLinkProperties());
    }

    /**
     * Check if any of the attached request has the specified network capability.
     *
     * @param netCapability The network capability to check.
     * @return {@code true} if at least one network request has specified network capability.
     */
    public boolean hasNetworkCapabilityInNetworkRequests(@NetCapability int netCapability) {
        return mAttachedNetworkRequestList.stream().anyMatch(
                request -> request.hasCapability(netCapability));
    }

    /**
     * The network validation requests moves to process on the state machine handler. A request is
     * processed according to state of the data network.
     */
    public void requestNetworkValidation(@NonNull Consumer<Integer> resultCodeCallback) {
        // request a network validation by DataNetwork state
        sendMessage(EVENT_DATA_NETWORK_VALIDATION_REQUESTED, resultCodeCallback);
    }

    /**
     * Request network validation to data service provider.
     */
    private void handleDataNetworkValidationRequest(@NonNull Consumer<Integer> resultCodeCallback) {
        if (mNetworkValidationResultCodeCallback != null) {
            loge("requestNetworkValidation: previous networkValidationRequest is in progress.");
            FunctionalUtils.ignoreRemoteException(resultCodeCallback::accept)
                    .accept(DataServiceCallback.RESULT_ERROR_BUSY);
            return;
        }

        mNetworkValidationResultCodeCallback = resultCodeCallback;

        // Request validation directly from the data service.
        mDataServiceManagers.get(mTransport).requestNetworkValidation(
                mCid.get(mTransport), obtainMessage(EVENT_DATA_NETWORK_VALIDATION_RESPONSE));

        int apnTypeBitmask = mDataProfile.getApnSetting() != null
                ? mDataProfile.getApnSetting().getApnTypeBitmask() : ApnSetting.TYPE_NONE;
        mDataNetworkValidationStats.onRequestNetworkValidation(apnTypeBitmask);

        log("handleDataNetworkValidationRequest, network validation requested");
    }

    private void handleErrorDataNetworkValidationRequest(
            @NonNull Consumer<Integer> resultCodeCallback) {
        loge("handleErrorDataNetworkValidationRequest: DataNetwork is not in Connected state");
        FunctionalUtils.ignoreRemoteException(resultCodeCallback::accept)
                .accept(DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
    }

    /**
     * handle the resultCode in response for the request.
     *
     * @param resultCode {@link DataServiceCallback.ResultCode}
     */
    private void handleDataNetworkValidationRequestResultCode(
            @DataServiceCallback.ResultCode int resultCode) {
        if (mNetworkValidationResultCodeCallback != null) {
            log("handleDataNetworkValidationRequestResultCode, resultCode:"
                    + DataServiceCallback.resultCodeToString(resultCode));
            FunctionalUtils.ignoreRemoteException(mNetworkValidationResultCodeCallback::accept)
                    .accept(resultCode);
            mNetworkValidationResultCodeCallback = null;
        }
    }

    /**
     * Update the validation status from {@link DataCallResponse}, convert to network validation
     * status {@link PreciseDataConnectionState.NetworkValidationStatus}.
     *
     * @param networkValidationStatus {@link PreciseDataConnectionState.NetworkValidationStatus}
     */
    private void updateValidationStatus(
            @PreciseDataConnectionState.NetworkValidationStatus int networkValidationStatus) {
        if (!mFlags.networkValidation()) {
            // Do nothing, if network validation feature is disabled
            return;
        }

        // if network validation status is changed, notify preciseDataConnectionState.
        if (mNetworkValidationStatus != networkValidationStatus) {
            log("updateValidationStatus:"
                    + PreciseDataConnectionState.networkValidationStatusToString(
                    networkValidationStatus));
            mNetworkValidationStatus = networkValidationStatus;
        }

        mDataNetworkValidationStats.onUpdateNetworkValidationState(
                mNetworkValidationStatus, getDataNetworkType());
    }

    /**
     * Convert the data tear down reason to string.
     *
     * @param reason Data deactivation reason.
     * @return The deactivation reason in string format.
     */
    @NonNull
    public static String tearDownReasonToString(@TearDownReason int reason) {
        return switch (reason) {
            case TEAR_DOWN_REASON_NONE -> "NONE";
            case TEAR_DOWN_REASON_CONNECTIVITY_SERVICE_UNWANTED -> "CONNECTIVITY_SERVICE_UNWANTED";
            case TEAR_DOWN_REASON_SIM_REMOVAL -> "SIM_REMOVAL";
            case TEAR_DOWN_REASON_AIRPLANE_MODE_ON -> "AIRPLANE_MODE_ON";
            case TEAR_DOWN_REASON_DATA_DISABLED -> "DATA_DISABLED";
            case TEAR_DOWN_REASON_NO_LIVE_REQUEST -> "TEAR_DOWN_REASON_NO_LIVE_REQUEST";
            case TEAR_DOWN_REASON_RAT_NOT_ALLOWED -> "TEAR_DOWN_REASON_RAT_NOT_ALLOWED";
            case TEAR_DOWN_REASON_ROAMING_DISABLED -> "TEAR_DOWN_REASON_ROAMING_DISABLED";
            case TEAR_DOWN_REASON_CONCURRENT_VOICE_DATA_NOT_ALLOWED ->
                    "TEAR_DOWN_REASON_CONCURRENT_VOICE_DATA_NOT_ALLOWED";
            case TEAR_DOWN_REASON_SERVICE_OPTION_NOT_SUPPORTED ->
                    "TEAR_DOWN_REASON_SERVICE_OPTION_NOT_SUPPORTED";
            case TEAR_DOWN_REASON_DATA_SERVICE_NOT_READY ->
                    "TEAR_DOWN_REASON_DATA_SERVICE_NOT_READY";
            case TEAR_DOWN_REASON_POWER_OFF_BY_CARRIER -> "TEAR_DOWN_REASON_POWER_OFF_BY_CARRIER";
            case TEAR_DOWN_REASON_DATA_STALL -> "TEAR_DOWN_REASON_DATA_STALL";
            case TEAR_DOWN_REASON_HANDOVER_FAILED -> "TEAR_DOWN_REASON_HANDOVER_FAILED";
            case TEAR_DOWN_REASON_HANDOVER_NOT_ALLOWED -> "TEAR_DOWN_REASON_HANDOVER_NOT_ALLOWED";
            case TEAR_DOWN_REASON_VCN_REQUESTED -> "TEAR_DOWN_REASON_VCN_REQUESTED";
            case TEAR_DOWN_REASON_VOPS_NOT_SUPPORTED -> "TEAR_DOWN_REASON_VOPS_NOT_SUPPORTED";
            case TEAR_DOWN_REASON_DEFAULT_DATA_UNSELECTED ->
                    "TEAR_DOWN_REASON_DEFAULT_DATA_UNSELECTED";
            case TEAR_DOWN_REASON_NOT_IN_SERVICE -> "TEAR_DOWN_REASON_NOT_IN_SERVICE";
            case TEAR_DOWN_REASON_DATA_CONFIG_NOT_READY -> "TEAR_DOWN_REASON_DATA_CONFIG_NOT_READY";
            case TEAR_DOWN_REASON_PENDING_TEAR_DOWN_ALL -> "TEAR_DOWN_REASON_PENDING_TEAR_DOWN_ALL";
            case TEAR_DOWN_REASON_NO_SUITABLE_DATA_PROFILE ->
                    "TEAR_DOWN_REASON_NO_SUITABLE_DATA_PROFILE";
            case TEAR_DOWN_REASON_CDMA_EMERGENCY_CALLBACK_MODE ->
                    "TEAR_DOWN_REASON_CDMA_EMERGENCY_CALLBACK_MODE";
            case TEAR_DOWN_REASON_RETRY_SCHEDULED -> "TEAR_DOWN_REASON_RETRY_SCHEDULED";
            case TEAR_DOWN_REASON_DATA_THROTTLED -> "TEAR_DOWN_REASON_DATA_THROTTLED";
            case TEAR_DOWN_REASON_DATA_PROFILE_INVALID -> "TEAR_DOWN_REASON_DATA_PROFILE_INVALID";
            case TEAR_DOWN_REASON_DATA_PROFILE_NOT_PREFERRED ->
                    "TEAR_DOWN_REASON_DATA_PROFILE_NOT_PREFERRED";
            case TEAR_DOWN_REASON_NOT_ALLOWED_BY_POLICY -> "TEAR_DOWN_REASON_NOT_ALLOWED_BY_POLICY";
            case TEAR_DOWN_REASON_ILLEGAL_STATE -> "TEAR_DOWN_REASON_ILLEGAL_STATE";
            case TEAR_DOWN_REASON_ONLY_ALLOWED_SINGLE_NETWORK ->
                    "TEAR_DOWN_REASON_ONLY_ALLOWED_SINGLE_NETWORK";
            case TEAR_DOWN_REASON_PREFERRED_DATA_SWITCHED ->
                    "TEAR_DOWN_REASON_PREFERRED_DATA_SWITCHED";
            case TEAR_DOWN_REASON_DATA_LIMIT_REACHED -> "TEAR_DOWN_REASON_DATA_LIMIT_REACHED";
            case TEAR_DOWN_REASON_DATA_NETWORK_TRANSPORT_NOT_ALLOWED ->
                    "TEAR_DOWN_REASON_DATA_NETWORK_TRANSPORT_NOT_ALLOWED";
            default -> "UNKNOWN(" + reason + ")";
        };
    }

    /**
     * Convert event to string
     *
     * @param event The event
     * @return The event in string format.
     */
    @NonNull
    private static String eventToString(int event) {
        return switch (event) {
            case EVENT_DATA_CONFIG_UPDATED -> "EVENT_DATA_CONFIG_UPDATED";
            case EVENT_ATTACH_NETWORK_REQUEST -> "EVENT_ATTACH_NETWORK_REQUEST";
            case EVENT_DETACH_NETWORK_REQUEST -> "EVENT_DETACH_NETWORK_REQUEST";
            case EVENT_RADIO_NOT_AVAILABLE -> "EVENT_RADIO_NOT_AVAILABLE";
            case EVENT_ALLOCATE_PDU_SESSION_ID_RESPONSE -> "EVENT_ALLOCATE_PDU_SESSION_ID_RESPONSE";
            case EVENT_SETUP_DATA_NETWORK_RESPONSE -> "EVENT_SETUP_DATA_NETWORK_RESPONSE";
            case EVENT_TEAR_DOWN_NETWORK -> "EVENT_TEAR_DOWN_NETWORK";
            case EVENT_DATA_STATE_CHANGED -> "EVENT_DATA_STATE_CHANGED";
            case EVENT_SERVICE_STATE_CHANGED -> "EVENT_DATA_NETWORK_TYPE_REG_STATE_CHANGED";
            case EVENT_DETACH_ALL_NETWORK_REQUESTS -> "EVENT_DETACH_ALL_NETWORK_REQUESTS";
            case EVENT_BANDWIDTH_ESTIMATE_FROM_MODEM_CHANGED ->
                    "EVENT_BANDWIDTH_ESTIMATE_FROM_MODEM_CHANGED";
            case EVENT_CANCEL_HANDOVER_NO_RESPONSE -> "EVENT_CANCEL_HANDOVER_NO_RESPONSE";
            case EVENT_DISPLAY_INFO_CHANGED -> "EVENT_DISPLAY_INFO_CHANGED";
            case EVENT_HANDOVER_RESPONSE -> "EVENT_HANDOVER_RESPONSE";
            case EVENT_SUBSCRIPTION_PLAN_OVERRIDE -> "EVENT_SUBSCRIPTION_PLAN_OVERRIDE";
            case EVENT_PCO_DATA_RECEIVED -> "EVENT_PCO_DATA_RECEIVED";
            case EVENT_CARRIER_PRIVILEGED_UIDS_CHANGED -> "EVENT_CARRIER_PRIVILEGED_UIDS_CHANGED";
            case EVENT_DEACTIVATE_DATA_NETWORK_RESPONSE -> "EVENT_DEACTIVATE_DATA_NETWORK_RESPONSE";
            case EVENT_STUCK_IN_TRANSIENT_STATE -> "EVENT_STUCK_IN_TRANSIENT_STATE";
            case EVENT_WAITING_FOR_TEARING_DOWN_CONDITION_MET ->
                    "EVENT_WAITING_FOR_TEARING_DOWN_CONDITION_MET";
            case EVENT_VOICE_CALL_STARTED -> "EVENT_VOICE_CALL_STARTED";
            case EVENT_VOICE_CALL_ENDED -> "EVENT_VOICE_CALL_ENDED";
            case EVENT_CSS_INDICATOR_CHANGED -> "EVENT_CSS_INDICATOR_CHANGED";
            case EVENT_NOTIFY_HANDOVER_STARTED -> "EVENT_NOTIFY_HANDOVER_STARTED";
            case EVENT_NOTIFY_HANDOVER_STARTED_RESPONSE -> "EVENT_NOTIFY_HANDOVER_STARTED_RESPONSE";
            case EVENT_NOTIFY_HANDOVER_CANCELLED_RESPONSE ->
                    "EVENT_NOTIFY_HANDOVER_CANCELLED_RESPONSE";
            case EVENT_DATA_NETWORK_VALIDATION_REQUESTED ->
                    "EVENT_DATA_NETWORK_VALIDATION_REQUESTED";
            case EVENT_DATA_NETWORK_VALIDATION_RESPONSE -> "EVENT_DATA_NETWORK_VALIDATION_RESPONSE";
            default -> "Unknown(" + event + ")";
        };
    }

    @Override
    public String toString() {
        return "[DataNetwork: " + mLogTag + ", " + (mDataProfile.getApnSetting() != null
                ? mDataProfile.getApnSetting().getApnName() : null) + ", state="
                + (getCurrentState() != null ? getCurrentState().getName() : null) + "]";
    }

    /**
     * @return The short name of the data network (e.g. DN-C-1)
     */
    @NonNull
    public String name() {
        return mLogTag;
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
     * Log debug messages.
     * @param s debug messages
     */
    @Override
    protected void log(@NonNull String s) {
        Rlog.d(mLogTag, (getCurrentState() != null
                ? (getCurrentState().getName() + ": ") : "") + s);
    }

    /**
     * Log error messages.
     * @param s error messages
     */
    @Override
    protected void loge(@NonNull String s) {
        Rlog.e(mLogTag, (getCurrentState() != null
                ? (getCurrentState().getName() + ": ") : "") + s);
    }

    /**
     * Log verbose messages.
     * @param s error messages
     */
    @Override
    protected void logv(@NonNull String s) {
        if (VDBG) {
            Rlog.v(mLogTag, (getCurrentState() != null
                    ? (getCurrentState().getName() + ": ") : "") + s);
        }
    }

    /**
     * Log debug messages and also log into the local log.
     * @param s debug messages
     */
    private void logl(@NonNull String s) {
        log(s);
        mLocalLog.log((getCurrentState() != null ? (getCurrentState().getName() + ": ") : "") + s);
    }

    /**
     * Dump the state of DataNetwork
     *
     * @param fd File descriptor
     * @param printWriter Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        super.dump(fd, pw, args);
        pw.println("Tag: " + name());
        pw.increaseIndent();
        pw.println("mSubId=" + mSubId);
        pw.println("mOnPreferredDataPhone=" + mOnPreferredDataPhone);
        pw.println("mTransport=" + AccessNetworkConstants.transportTypeToString(mTransport));
        pw.println("mLastKnownDataNetworkType=" + TelephonyManager
                .getNetworkTypeName(mLastKnownDataNetworkType));
        pw.println("WWAN cid=" + mCid.get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN));
        pw.println("WLAN cid=" + mCid.get(AccessNetworkConstants.TRANSPORT_TYPE_WLAN));
        pw.println("mNetworkScore=" + mNetworkScore);
        pw.println("keepConnectedReason=" + mNetworkScore.getKeepConnectedReason());
        pw.println("mDataAllowedReason=" + mDataAllowedReason);
        pw.println("mPduSessionId=" + mPduSessionId);
        pw.println("mDataProfile=" + mDataProfile);
        pw.println("mNetworkCapabilities=" + mNetworkCapabilities);
        pw.println("mLinkProperties=" + mLinkProperties);
        pw.println("mNetworkSliceInfo=" + mNetworkSliceInfo);
        pw.println("mNetworkBandwidth=" + mNetworkBandwidth);
        pw.println("mTcpBufferSizes=" + mTcpBufferSizes);
        pw.println("mTelephonyDisplayInfo=" + mTelephonyDisplayInfo);
        pw.println("mTempNotMeteredSupported=" + mTempNotMeteredSupported);
        pw.println("mTempNotMetered=" + mTempNotMetered);
        pw.println("mCongested=" + mCongested);
        pw.println("mSuspended=" + mSuspended);
        pw.println("mDataCallResponse=" + mDataCallResponse);
        pw.println("mFailCause=" + DataFailCause.toString(mFailCause));
        pw.println("mAdministratorUids=" + Arrays.toString(mAdministratorUids));
        pw.println("mCarrierServicePackageUid=" + mCarrierServicePackageUid);
        pw.println("mEverConnected=" + mEverConnected);
        pw.println("mInvokedDataDeactivation=" + mInvokedDataDeactivation);

        pw.println("Attached network requests:");
        pw.increaseIndent();
        for (TelephonyNetworkRequest request : mAttachedNetworkRequestList) {
            pw.println(request);
        }
        pw.decreaseIndent();
        pw.println("mQosBearerSessions=" + mQosBearerSessions);

        mNetworkAgent.dump(fd, pw, args);
        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
        pw.println("---------------");
    }
}

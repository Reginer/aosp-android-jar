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

import static android.telephony.data.DataCallResponse.PDU_SESSION_ID_NOT_SET;

import static com.android.internal.telephony.dataconnection.DcTracker.REQUEST_TYPE_HANDOVER;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.InetAddresses;
import android.net.KeepalivePacketData;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkProvider;
import android.net.NetworkRequest;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.SocketKeepalive;
import android.net.TelephonyNetworkSpecifier;
import android.net.vcn.VcnManager;
import android.net.vcn.VcnManager.VcnNetworkPolicyChangeListener;
import android.net.vcn.VcnNetworkPolicyResult;
import android.os.AsyncResult;
import android.os.HandlerExecutor;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.Telephony;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.ApnType;
import android.telephony.Annotation.DataFailureCause;
import android.telephony.Annotation.DataState;
import android.telephony.Annotation.NetworkType;
import android.telephony.CarrierConfigManager;
import android.telephony.DataFailCause;
import android.telephony.LinkCapacityEstimate;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataCallResponse.HandoverFailureMode;
import android.telephony.data.DataProfile;
import android.telephony.data.DataService;
import android.telephony.data.DataServiceCallback;
import android.telephony.data.NetworkSliceInfo;
import android.telephony.data.Qos;
import android.telephony.data.QosBearerSession;
import android.telephony.data.TrafficDescriptor;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Pair;
import android.util.TimeUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CarrierPrivilegesTracker;
import com.android.internal.telephony.CarrierSignalAgent;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RetryManager;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.internal.telephony.data.DataConfigManager;
import com.android.internal.telephony.data.KeepaliveStatus;
import com.android.internal.telephony.dataconnection.DcTracker.ReleaseNetworkType;
import com.android.internal.telephony.dataconnection.DcTracker.RequestNetworkType;
import com.android.internal.telephony.metrics.DataCallSessionStats;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.nano.TelephonyProto.RilDataCall;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.util.ArrayUtils;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Protocol;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.net.module.util.NetworkCapabilitiesUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * {@hide}
 *
 * DataConnection StateMachine.
 *
 * This a class for representing a single data connection, with instances of this
 * class representing a connection via the cellular network. There may be multiple
 * data connections and all of them are managed by the <code>DataConnectionTracker</code>.
 *
 * NOTE: All DataConnection objects must be running on the same looper, which is the default
 * as the coordinator has members which are used without synchronization.
 */
public class DataConnection extends StateMachine {
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private static final String NETWORK_TYPE = "MOBILE";

    private static final String RAT_NAME_5G = "nr";
    private static final String RAT_NAME_EVDO = "evdo";

    /**
     * OSId for "Android", using UUID version 5 with namespace ISO OSI.
     * Prepended to the OsAppId in TrafficDescriptor to use for URSP matching.
     */
    private static final UUID OS_ID = UUID.fromString("97a498e3-fc92-5c94-8986-0333d06e4e47");

    /**
     * The data connection is not being or been handovered. Note this is the state for the source
     * data connection, not destination data connection
     */
    private static final int HANDOVER_STATE_IDLE = 1;

    /**
     * The data connection is being handovered. Note this is the state for the source
     * data connection, not destination data connection.
     */
    private static final int HANDOVER_STATE_BEING_TRANSFERRED = 2;

    /**
     * The data connection is already handovered. Note this is the state for the source
     * data connection, not destination data connection.
     */
    private static final int HANDOVER_STATE_COMPLETED = 3;


    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"HANDOVER_STATE_"}, value = {
            HANDOVER_STATE_IDLE,
            HANDOVER_STATE_BEING_TRANSFERRED,
            HANDOVER_STATE_COMPLETED})
    public @interface HandoverState {}

    // The data connection providing default Internet connection will have a higher score of 50.
    // Other connections will have a slightly lower score of 45. The intention is other connections
    // will not cause ConnectivityService to tear down default internet connection. For example,
    // to validate Internet connection on non-default data SIM, we'll set up a temporary Internet
    // connection on that data SIM. In this case, score of 45 is assigned so ConnectivityService
    // will not replace the default Internet connection with it.
    private static final int DEFAULT_INTERNET_CONNECTION_SCORE = 50;
    private static final int OTHER_CONNECTION_SCORE = 45;

    // The score we report to connectivity service
    private int mScore;

    // The subscription id associated with this data connection.
    private int mSubId;

    // The data connection controller
    private DcController mDcController;

    // The Tester for failing all bringup's
    private DcTesterFailBringUpAll mDcTesterFailBringUpAll;

    // Whether or not the data connection should allocate its own pdu session id
    private boolean mDoAllocatePduSessionId;

    private static AtomicInteger mInstanceNumber = new AtomicInteger(0);
    private AsyncChannel mAc;

    // The DCT that's talking to us, we only support one!
    private DcTracker mDct = null;

    private String[] mPcscfAddr;

    private final String mTagSuffix;

    private final LocalLog mHandoverLocalLog = new LocalLog(64);

    private int[] mAdministratorUids = new int[0];

    // stats per data call
    private DataCallSessionStats mDataCallSessionStats;

    /**
     * Used internally for saving connecting parameters.
     */
    public static class ConnectionParams {
        int mTag;
        ApnContext mApnContext;
        int mProfileId;
        int mRilRat;
        Message mOnCompletedMsg;
        final int mConnectionGeneration;
        @RequestNetworkType
        final int mRequestType;
        final int mSubId;
        final boolean mIsPreferredApn;

        ConnectionParams(ApnContext apnContext, int profileId, int rilRadioTechnology,
                         Message onCompletedMsg, int connectionGeneration,
                         @RequestNetworkType int requestType, int subId,
                         boolean isPreferredApn) {
            mApnContext = apnContext;
            mProfileId = profileId;
            mRilRat = rilRadioTechnology;
            mOnCompletedMsg = onCompletedMsg;
            mConnectionGeneration = connectionGeneration;
            mRequestType = requestType;
            mSubId = subId;
            mIsPreferredApn = isPreferredApn;
        }

        @Override
        public String toString() {
            return "{mTag=" + mTag + " mApnContext=" + mApnContext
                    + " mProfileId=" + mProfileId
                    + " mRat=" + mRilRat
                    + " mOnCompletedMsg=" + msgToString(mOnCompletedMsg)
                    + " mRequestType=" + DcTracker.requestTypeToString(mRequestType)
                    + " mSubId=" + mSubId
                    + " mIsPreferredApn=" + mIsPreferredApn
                    + "}";
        }
    }

    /**
     * Used internally for saving disconnecting parameters.
     */
    public static class DisconnectParams {
        int mTag;
        public ApnContext mApnContext;
        String mReason;
        @ReleaseNetworkType
        final int mReleaseType;
        Message mOnCompletedMsg;

        DisconnectParams(ApnContext apnContext, String reason, @ReleaseNetworkType int releaseType,
                         Message onCompletedMsg) {
            mApnContext = apnContext;
            mReason = reason;
            mReleaseType = releaseType;
            mOnCompletedMsg = onCompletedMsg;
        }

        @Override
        public String toString() {
            return "{mTag=" + mTag + " mApnContext=" + mApnContext
                    + " mReason=" + mReason
                    + " mReleaseType=" + DcTracker.releaseTypeToString(mReleaseType)
                    + " mOnCompletedMsg=" + msgToString(mOnCompletedMsg) + "}";
        }
    }

    private volatile ApnSetting mApnSetting;
    private ConnectionParams mConnectionParams;
    private DisconnectParams mDisconnectParams;
    @DataFailureCause
    private int mDcFailCause;

    @HandoverFailureMode
    private int mHandoverFailureMode;

    private Phone mPhone;
    private DataServiceManager mDataServiceManager;
    private VcnManager mVcnManager;
    private final int mTransportType;
    private LinkProperties mLinkProperties = new LinkProperties();
    private int mPduSessionId;
    private long mCreateTime;
    private long mLastFailTime;
    @DataFailureCause
    private int mLastFailCause;
    private static final String NULL_IP = "0.0.0.0";
    private Object mUserData;
    private boolean mCongestedOverride;
    private boolean mUnmeteredOverride;
    private int mRilRat = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
    private int mDataRegState = Integer.MAX_VALUE;
    // Indicating data connection is suspended due to temporary reasons, for example, out of
    // service, concurrency voice/data not supported, etc.. Note this flag is only meaningful when
    // data is in active state. When data is in inactive, connecting, or disconnecting, this flag
    // is unmeaningful.
    private boolean mIsSuspended;
    private int mDownlinkBandwidth = 14;
    private int mUplinkBandwidth = 14;
    private Qos mDefaultQos = null;
    private List<QosBearerSession> mQosBearerSessions = new ArrayList<>();
    private NetworkSliceInfo mSliceInfo;
    private List<TrafficDescriptor> mTrafficDescriptors = new ArrayList<>();

    /** The corresponding network agent for this data connection. */
    private DcNetworkAgent mNetworkAgent;

    /**
     * The network agent from handover source data connection. This is the potential network agent
     * that will be transferred here after handover completed.
     */
    private DcNetworkAgent mHandoverSourceNetworkAgent;

    private int mDisabledApnTypeBitMask = 0;

    int mTag;

    /** Data connection id assigned by the modem. This is unique across transports */
    public int mCid;

    @HandoverState
    private int mHandoverState = HANDOVER_STATE_IDLE;
    private final Map<ApnContext, ConnectionParams> mApnContexts = new ConcurrentHashMap<>();
    PendingIntent mReconnectIntent = null;

    /** Class used to track VCN-defined Network policies for this DcNetworkAgent. */
    private final VcnNetworkPolicyChangeListener mVcnPolicyChangeListener =
            new DataConnectionVcnNetworkPolicyChangeListener();

    // ***** Event codes for driving the state machine, package visible for Dcc
    static final int BASE = Protocol.BASE_DATA_CONNECTION;
    static final int EVENT_CONNECT = BASE + 0;
    static final int EVENT_SETUP_DATA_CONNECTION_DONE = BASE + 1;
    static final int EVENT_DEACTIVATE_DONE = BASE + 3;
    static final int EVENT_DISCONNECT = BASE + 4;
    static final int EVENT_DISCONNECT_ALL = BASE + 6;
    static final int EVENT_DATA_STATE_CHANGED = BASE + 7;
    static final int EVENT_TEAR_DOWN_NOW = BASE + 8;
    static final int EVENT_LOST_CONNECTION = BASE + 9;
    static final int EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED = BASE + 11;
    static final int EVENT_DATA_CONNECTION_ROAM_ON = BASE + 12;
    static final int EVENT_DATA_CONNECTION_ROAM_OFF = BASE + 13;
    static final int EVENT_BW_REFRESH_RESPONSE = BASE + 14;
    static final int EVENT_DATA_CONNECTION_VOICE_CALL_STARTED = BASE + 15;
    static final int EVENT_DATA_CONNECTION_VOICE_CALL_ENDED = BASE + 16;
    static final int EVENT_DATA_CONNECTION_CONGESTEDNESS_CHANGED = BASE + 17;
    static final int EVENT_KEEPALIVE_STATUS = BASE + 18;
    static final int EVENT_KEEPALIVE_STARTED = BASE + 19;
    static final int EVENT_KEEPALIVE_STOPPED = BASE + 20;
    static final int EVENT_KEEPALIVE_START_REQUEST = BASE + 21;
    static final int EVENT_KEEPALIVE_STOP_REQUEST = BASE + 22;
    static final int EVENT_LINK_CAPACITY_CHANGED = BASE + 23;
    static final int EVENT_RESET = BASE + 24;
    static final int EVENT_REEVALUATE_RESTRICTED_STATE = BASE + 25;
    static final int EVENT_REEVALUATE_DATA_CONNECTION_PROPERTIES = BASE + 26;
    static final int EVENT_NR_STATE_CHANGED = BASE + 27;
    static final int EVENT_DATA_CONNECTION_METEREDNESS_CHANGED = BASE + 28;
    static final int EVENT_NR_FREQUENCY_CHANGED = BASE + 29;
    static final int EVENT_CARRIER_CONFIG_LINK_BANDWIDTHS_CHANGED = BASE + 30;
    static final int EVENT_CARRIER_PRIVILEGED_UIDS_CHANGED = BASE + 31;
    static final int EVENT_CSS_INDICATOR_CHANGED = BASE + 32;
    static final int EVENT_UPDATE_SUSPENDED_STATE = BASE + 33;
    static final int EVENT_START_HANDOVER = BASE + 34;
    static final int EVENT_CANCEL_HANDOVER = BASE + 35;
    static final int EVENT_START_HANDOVER_ON_TARGET = BASE + 36;
    static final int EVENT_ALLOCATE_PDU_SESSION_ID = BASE + 37;
    static final int EVENT_RELEASE_PDU_SESSION_ID = BASE + 38;
    static final int EVENT_LINK_BANDWIDTH_ESTIMATOR_UPDATE = BASE + 39;
    private static final int CMD_TO_STRING_COUNT = EVENT_LINK_BANDWIDTH_ESTIMATOR_UPDATE - BASE + 1;

    private static String[] sCmdToString = new String[CMD_TO_STRING_COUNT];
    static {
        sCmdToString[EVENT_CONNECT - BASE] = "EVENT_CONNECT";
        sCmdToString[EVENT_SETUP_DATA_CONNECTION_DONE - BASE] =
                "EVENT_SETUP_DATA_CONNECTION_DONE";
        sCmdToString[EVENT_DEACTIVATE_DONE - BASE] = "EVENT_DEACTIVATE_DONE";
        sCmdToString[EVENT_DISCONNECT - BASE] = "EVENT_DISCONNECT";
        sCmdToString[EVENT_DISCONNECT_ALL - BASE] = "EVENT_DISCONNECT_ALL";
        sCmdToString[EVENT_DATA_STATE_CHANGED - BASE] = "EVENT_DATA_STATE_CHANGED";
        sCmdToString[EVENT_TEAR_DOWN_NOW - BASE] = "EVENT_TEAR_DOWN_NOW";
        sCmdToString[EVENT_LOST_CONNECTION - BASE] = "EVENT_LOST_CONNECTION";
        sCmdToString[EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED - BASE] =
                "EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED";
        sCmdToString[EVENT_DATA_CONNECTION_ROAM_ON - BASE] = "EVENT_DATA_CONNECTION_ROAM_ON";
        sCmdToString[EVENT_DATA_CONNECTION_ROAM_OFF - BASE] = "EVENT_DATA_CONNECTION_ROAM_OFF";
        sCmdToString[EVENT_BW_REFRESH_RESPONSE - BASE] = "EVENT_BW_REFRESH_RESPONSE";
        sCmdToString[EVENT_DATA_CONNECTION_VOICE_CALL_STARTED - BASE] =
                "EVENT_DATA_CONNECTION_VOICE_CALL_STARTED";
        sCmdToString[EVENT_DATA_CONNECTION_VOICE_CALL_ENDED - BASE] =
                "EVENT_DATA_CONNECTION_VOICE_CALL_ENDED";
        sCmdToString[EVENT_DATA_CONNECTION_CONGESTEDNESS_CHANGED - BASE] =
                "EVENT_DATA_CONNECTION_CONGESTEDNESS_CHANGED";
        sCmdToString[EVENT_KEEPALIVE_STATUS - BASE] = "EVENT_KEEPALIVE_STATUS";
        sCmdToString[EVENT_KEEPALIVE_STARTED - BASE] = "EVENT_KEEPALIVE_STARTED";
        sCmdToString[EVENT_KEEPALIVE_STOPPED - BASE] = "EVENT_KEEPALIVE_STOPPED";
        sCmdToString[EVENT_KEEPALIVE_START_REQUEST - BASE] = "EVENT_KEEPALIVE_START_REQUEST";
        sCmdToString[EVENT_KEEPALIVE_STOP_REQUEST - BASE] = "EVENT_KEEPALIVE_STOP_REQUEST";
        sCmdToString[EVENT_LINK_CAPACITY_CHANGED - BASE] = "EVENT_LINK_CAPACITY_CHANGED";
        sCmdToString[EVENT_RESET - BASE] = "EVENT_RESET";
        sCmdToString[EVENT_REEVALUATE_RESTRICTED_STATE - BASE] =
                "EVENT_REEVALUATE_RESTRICTED_STATE";
        sCmdToString[EVENT_REEVALUATE_DATA_CONNECTION_PROPERTIES - BASE] =
                "EVENT_REEVALUATE_DATA_CONNECTION_PROPERTIES";
        sCmdToString[EVENT_NR_STATE_CHANGED - BASE] = "EVENT_NR_STATE_CHANGED";
        sCmdToString[EVENT_DATA_CONNECTION_METEREDNESS_CHANGED - BASE] =
                "EVENT_DATA_CONNECTION_METEREDNESS_CHANGED";
        sCmdToString[EVENT_NR_FREQUENCY_CHANGED - BASE] = "EVENT_NR_FREQUENCY_CHANGED";
        sCmdToString[EVENT_CARRIER_CONFIG_LINK_BANDWIDTHS_CHANGED - BASE] =
                "EVENT_CARRIER_CONFIG_LINK_BANDWIDTHS_CHANGED";
        sCmdToString[EVENT_CARRIER_PRIVILEGED_UIDS_CHANGED - BASE] =
                "EVENT_CARRIER_PRIVILEGED_UIDS_CHANGED";
        sCmdToString[EVENT_CSS_INDICATOR_CHANGED - BASE] = "EVENT_CSS_INDICATOR_CHANGED";
        sCmdToString[EVENT_UPDATE_SUSPENDED_STATE - BASE] = "EVENT_UPDATE_SUSPENDED_STATE";
        sCmdToString[EVENT_START_HANDOVER - BASE] = "EVENT_START_HANDOVER";
        sCmdToString[EVENT_CANCEL_HANDOVER - BASE] = "EVENT_CANCEL_HANDOVER";
        sCmdToString[EVENT_START_HANDOVER_ON_TARGET - BASE] = "EVENT_START_HANDOVER_ON_TARGET";
        sCmdToString[EVENT_ALLOCATE_PDU_SESSION_ID - BASE] = "EVENT_ALLOCATE_PDU_SESSION_ID";
        sCmdToString[EVENT_RELEASE_PDU_SESSION_ID - BASE] = "EVENT_RELEASE_PDU_SESSION_ID";
        sCmdToString[EVENT_LINK_BANDWIDTH_ESTIMATOR_UPDATE - BASE] =
                "EVENT_LINK_BANDWIDTH_ESTIMATOR_UPDATE";
    }
    // Convert cmd to string or null if unknown
    static String cmdToString(int cmd) {
        String value = null;
        cmd -= BASE;
        if ((cmd >= 0) && (cmd < sCmdToString.length)) {
            value = sCmdToString[cmd];
        }
        if (value == null) {
            value = "0x" + Integer.toHexString(cmd + BASE);
        }
        return value;
    }

    /**
     * Create the connection object
     *
     * @param phone the Phone
     * @param id the connection id
     * @return DataConnection that was created.
     */
    public static DataConnection makeDataConnection(Phone phone, int id, DcTracker dct,
                                                    DataServiceManager dataServiceManager,
                                                    DcTesterFailBringUpAll failBringUpAll,
                                                    DcController dcc) {
        String transportType = (dataServiceManager.getTransportType()
                == AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                ? "C"   // Cellular
                : "I";  // IWLAN
        DataConnection dc = new DataConnection(phone, transportType + "-"
                + mInstanceNumber.incrementAndGet(), id, dct, dataServiceManager, failBringUpAll,
                dcc);
        dc.start();
        if (DBG) dc.log("Made " + dc.getName());
        return dc;
    }

    void dispose() {
        log("dispose: call quiteNow()");
        quitNow();
    }

    /* Getter functions */

    LinkProperties getLinkProperties() {
        return new LinkProperties(mLinkProperties);
    }

    boolean isDisconnecting() {
        return getCurrentState() == mDisconnectingState
                || getCurrentState() == mDisconnectingErrorCreatingConnection;
    }

    @VisibleForTesting
    public boolean isActive() {
        return getCurrentState() == mActiveState;
    }

    @VisibleForTesting
    public boolean isInactive() {
        return getCurrentState() == mInactiveState;
    }

    boolean isActivating() {
        return getCurrentState() == mActivatingState;
    }

    boolean hasBeenTransferred() {
        return mHandoverState == HANDOVER_STATE_COMPLETED;
    }

    int getCid() {
        return mCid;
    }

    /**
     * @return DataConnection's ApnSetting.
     */
    public ApnSetting getApnSetting() {
        return mApnSetting;
    }

    /**
     * Update http proxy of link properties based on current apn setting
     */
    private void updateLinkPropertiesHttpProxy() {
        if (mApnSetting == null
                || TextUtils.isEmpty(mApnSetting.getProxyAddressAsString())) {
            return;
        }
        try {
            int port = mApnSetting.getProxyPort();
            if (port == -1) {
                port = 8080;
            }
            ProxyInfo proxy = ProxyInfo.buildDirectProxy(
                    mApnSetting.getProxyAddressAsString(), port);
            mLinkProperties.setHttpProxy(proxy);
        } catch (NumberFormatException e) {
            loge("onDataSetupComplete: NumberFormatException making ProxyProperties ("
                    + mApnSetting.getProxyPort() + "): " + e);
        }
    }

    public static class UpdateLinkPropertyResult {
        public SetupResult setupResult = SetupResult.SUCCESS;
        public LinkProperties oldLp;
        public LinkProperties newLp;
        public UpdateLinkPropertyResult(LinkProperties curLp) {
            oldLp = curLp;
            newLp = curLp;
        }
    }

    /**
     * Class returned by onSetupConnectionCompleted.
     */
    public enum SetupResult {
        SUCCESS,
        ERROR_RADIO_NOT_AVAILABLE,
        ERROR_INVALID_ARG,
        ERROR_STALE,
        ERROR_DATA_SERVICE_SPECIFIC_ERROR,
        ERROR_DUPLICATE_CID,
        ERROR_NO_DEFAULT_CONNECTION;

        public int mFailCause;

        SetupResult() {
            mFailCause = DataFailCause.getFailCause(0);
        }

        @Override
        public String toString() {
            return name() + "  SetupResult.mFailCause=" + DataFailCause.toString(mFailCause);
        }
    }

    public boolean isIpv4Connected() {
        boolean ret = false;
        Collection <InetAddress> addresses = mLinkProperties.getAddresses();

        for (InetAddress addr: addresses) {
            if (addr instanceof java.net.Inet4Address) {
                java.net.Inet4Address i4addr = (java.net.Inet4Address) addr;
                if (!i4addr.isAnyLocalAddress() && !i4addr.isLinkLocalAddress() &&
                        !i4addr.isLoopbackAddress() && !i4addr.isMulticastAddress()) {
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    public boolean isIpv6Connected() {
        boolean ret = false;
        Collection <InetAddress> addresses = mLinkProperties.getAddresses();

        for (InetAddress addr: addresses) {
            if (addr instanceof java.net.Inet6Address) {
                java.net.Inet6Address i6addr = (java.net.Inet6Address) addr;
                if (!i6addr.isAnyLocalAddress() && !i6addr.isLinkLocalAddress() &&
                        !i6addr.isLoopbackAddress() && !i6addr.isMulticastAddress()) {
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    public int getPduSessionId() {
        return mPduSessionId;
    }

    public NetworkSliceInfo getSliceInfo() {
        return mSliceInfo;
    }

    public List<TrafficDescriptor> getTrafficDescriptors() {
        return mTrafficDescriptors;
    }

    /**
     * Update DC fields based on a new DataCallResponse
     * @param response the response to use to update DC fields
     */
    public void updateResponseFields(DataCallResponse response) {
        updateQosParameters(response);
        updateSliceInfo(response);
        updateTrafficDescriptors(response);
    }

    public void updateQosParameters(final @Nullable DataCallResponse response) {
        if (response == null) {
            mDefaultQos = null;
            mQosBearerSessions.clear();
            return;
        }

        mDefaultQos = response.getDefaultQos();
        mQosBearerSessions = response.getQosBearerSessions();

        if (mNetworkAgent != null) {
            syncQosToNetworkAgent();
        }
    }

    private void syncQosToNetworkAgent() {
        final DcNetworkAgent networkAgent = mNetworkAgent;
        final List<QosBearerSession> qosBearerSessions = mQosBearerSessions;
        if (qosBearerSessions == null) {
            networkAgent.updateQosBearerSessions(new ArrayList<>());
            return;
        }
        networkAgent.updateQosBearerSessions(qosBearerSessions);
    }

    /**
     * Update the latest slice info on this data connection with
     * {@link DataCallResponse#getSliceInfo}.
     */
    public void updateSliceInfo(DataCallResponse response) {
        mSliceInfo = response.getSliceInfo();
    }

    /**
     * Update the latest traffic descriptor on this data connection with
     * {@link DataCallResponse#getTrafficDescriptors}.
     */
    public void updateTrafficDescriptors(DataCallResponse response) {
        mTrafficDescriptors = response.getTrafficDescriptors();
        mDcController.updateTrafficDescriptorsForCid(response.getId(),
                response.getTrafficDescriptors());
    }

    @VisibleForTesting
    public UpdateLinkPropertyResult updateLinkProperty(DataCallResponse newState) {
        UpdateLinkPropertyResult result = new UpdateLinkPropertyResult(mLinkProperties);

        if (newState == null) return result;

        result.newLp = new LinkProperties();

        // set link properties based on data call response
        result.setupResult = setLinkProperties(newState, result.newLp);
        if (result.setupResult != SetupResult.SUCCESS) {
            if (DBG) log("updateLinkProperty failed : " + result.setupResult);
            return result;
        }
        // copy HTTP proxy as it is not part DataCallResponse.
        result.newLp.setHttpProxy(mLinkProperties.getHttpProxy());

        checkSetMtu(mApnSetting, result.newLp);

        mLinkProperties = result.newLp;

        updateTcpBufferSizes(mRilRat);

        if (DBG && (! result.oldLp.equals(result.newLp))) {
            log("updateLinkProperty old LP=" + result.oldLp);
            log("updateLinkProperty new LP=" + result.newLp);
        }

        if (result.newLp.equals(result.oldLp) == false &&
                mNetworkAgent != null) {
            mNetworkAgent.sendLinkProperties(mLinkProperties, DataConnection.this);
        }

        return result;
    }

    /**
     * Sets the pdu session id of the data connection
     * @param pduSessionId pdu session id to set
     */
    @VisibleForTesting
    public void setPduSessionId(int pduSessionId) {
        if (mPduSessionId != pduSessionId) {
            logd("Changing pdu session id from: " + mPduSessionId + " to: " + pduSessionId + ", "
                    + "Handover state: " + handoverStateToString(this.mHandoverState));
            mPduSessionId = pduSessionId;
        }
    }

    /**
     * Read the MTU value from link properties where it can be set from network. In case
     * not set by the network, set it again using the mtu szie value defined in the APN
     * database for the connected APN
     */
    private void checkSetMtu(ApnSetting apn, LinkProperties lp) {
        if (lp == null) return;

        if (apn == null || lp == null) return;

        if (lp.getMtu() != PhoneConstants.UNSET_MTU) {
            if (DBG) log("MTU set by call response to: " + lp.getMtu());
            return;
        }

        if (apn != null && apn.getMtuV4() != PhoneConstants.UNSET_MTU) {
            lp.setMtu(apn.getMtuV4());
            if (DBG) log("MTU set by APN to: " + apn.getMtuV4());
            return;
        }

        int mtu = mPhone.getContext().getResources().getInteger(
                com.android.internal.R.integer.config_mobile_mtu);
        if (mtu != PhoneConstants.UNSET_MTU) {
            lp.setMtu(mtu);
            if (DBG) log("MTU set by config resource to: " + mtu);
        }
    }

    //***** Constructor (NOTE: uses dcc.getHandler() as its Handler)
    private DataConnection(Phone phone, String tagSuffix, int id,
                           DcTracker dct, DataServiceManager dataServiceManager,
                           DcTesterFailBringUpAll failBringUpAll, DcController dcc) {
        super("DC-" + tagSuffix, dcc);
        mTagSuffix = tagSuffix;
        setLogRecSize(300);
        setLogOnlyTransitions(true);
        if (DBG) log("DataConnection created");

        mPhone = phone;
        mDct = dct;
        mDataServiceManager = dataServiceManager;
        mVcnManager = mPhone.getContext().getSystemService(VcnManager.class);
        mTransportType = dataServiceManager.getTransportType();
        mDcTesterFailBringUpAll = failBringUpAll;
        mDcController = dcc;
        mId = id;
        mCid = -1;
        mDataRegState = mPhone.getServiceState().getDataRegistrationState();
        mIsSuspended = false;
        mDataCallSessionStats = new DataCallSessionStats(mPhone);
        mDoAllocatePduSessionId = false;

        int networkType = getNetworkType();
        mRilRat = ServiceState.networkTypeToRilRadioTechnology(networkType);
        updateLinkBandwidthsFromCarrierConfig(mRilRat);

        addState(mDefaultState);
            addState(mInactiveState, mDefaultState);
            addState(mActivatingState, mDefaultState);
            addState(mActiveState, mDefaultState);
            addState(mDisconnectingState, mDefaultState);
            addState(mDisconnectingErrorCreatingConnection, mDefaultState);
        setInitialState(mInactiveState);
    }

    private @NetworkType int getNetworkType() {
        ServiceState ss = mPhone.getServiceState();
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;

        NetworkRegistrationInfo nri = ss.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, mTransportType);
        if (nri != null) {
            networkType = nri.getAccessNetworkTechnology();
        }

        return networkType;
    }

    /**
     * Get the source transport for handover. For example, handover from WWAN to WLAN, WWAN is the
     * source transport, and vice versa.
     */
    private @TransportType int getHandoverSourceTransport() {
        return mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                ? AccessNetworkConstants.TRANSPORT_TYPE_WLAN
                : AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
    }

    /**
     * API to generate the OsAppId for enterprise traffic category.
     * @return byte[] representing OsId + length of OsAppId + OsAppId
     */
    @VisibleForTesting
    public static byte[] getEnterpriseOsAppId() {
        byte[] osAppId = NetworkCapabilities.getCapabilityCarrierName(
                NetworkCapabilities.NET_CAPABILITY_ENTERPRISE).getBytes();
        // 16 bytes for UUID, 1 byte for length of osAppId, and up to 255 bytes for osAppId
        ByteBuffer bb = ByteBuffer.allocate(16 + 1 + osAppId.length);
        bb.putLong(OS_ID.getMostSignificantBits());
        bb.putLong(OS_ID.getLeastSignificantBits());
        bb.put((byte) osAppId.length);
        bb.put(osAppId);
        if (VDBG) {
            Rlog.d("DataConnection", "getEnterpriseOsAppId: "
                    + IccUtils.bytesToHexString(bb.array()));
        }
        return bb.array();
    }

    /**
     * Begin setting up a data connection, calls setupDataCall
     * and the ConnectionParams will be returned with the
     * EVENT_SETUP_DATA_CONNECTION_DONE
     *
     * @param cp is the connection parameters
     *
     * @return Fail cause if failed to setup data connection. {@link DataFailCause#NONE} if success.
     */
    private @DataFailureCause int connect(ConnectionParams cp) {
        log("connect: carrier='" + mApnSetting.getEntryName()
                + "' APN='" + mApnSetting.getApnName()
                + "' proxy='" + mApnSetting.getProxyAddressAsString()
                + "' port='" + mApnSetting.getProxyPort() + "'");
        ApnContext.requestLog(cp.mApnContext, "DataConnection.connect");

        // Check if we should fake an error.
        if (mDcTesterFailBringUpAll.getDcFailBringUp().mCounter  > 0) {
            DataCallResponse response = new DataCallResponse.Builder()
                    .setCause(mDcTesterFailBringUpAll.getDcFailBringUp().mFailCause)
                    .setRetryDurationMillis(
                            mDcTesterFailBringUpAll.getDcFailBringUp().mSuggestedRetryTime)
                    .setMtuV4(PhoneConstants.UNSET_MTU)
                    .setMtuV6(PhoneConstants.UNSET_MTU)
                    .build();

            Message msg = obtainMessage(EVENT_SETUP_DATA_CONNECTION_DONE, cp);
            AsyncResult.forMessage(msg, response, null);
            sendMessage(msg);
            if (DBG) {
                log("connect: FailBringUpAll=" + mDcTesterFailBringUpAll.getDcFailBringUp()
                        + " send error response=" + response);
            }
            mDcTesterFailBringUpAll.getDcFailBringUp().mCounter -= 1;
            return DataFailCause.NONE;
        }

        mCreateTime = -1;
        mLastFailTime = -1;
        mLastFailCause = DataFailCause.NONE;

        Message msg = obtainMessage(EVENT_SETUP_DATA_CONNECTION_DONE, cp);
        msg.obj = cp;

        DataProfile dp = new DataProfile.Builder()
                .setApnSetting(mApnSetting)
                .setPreferred(cp.mIsPreferredApn)
                .build();

        // We need to use the actual modem roaming state instead of the framework roaming state
        // here. This flag is only passed down to ril_service for picking the correct protocol (for
        // old modem backward compatibility).
        boolean isModemRoaming = mPhone.getServiceState().getDataRoamingFromRegistration();

        // If the apn is NOT metered, we will allow data roaming regardless of the setting.
        boolean isUnmeteredApnType = !ApnSettingUtils.isMeteredApnType(
                cp.mApnContext.getApnTypeBitmask(), mPhone);

        // Set this flag to true if the user turns on data roaming. Or if we override the roaming
        // state in framework, we should set this flag to true as well so the modem will not reject
        // the data call setup (because the modem actually thinks the device is roaming).
        boolean allowRoaming = mPhone.getDataRoamingEnabled()
                || (isModemRoaming && (!mPhone.getServiceState().getDataRoaming()
                || isUnmeteredApnType));

        String dnn = null;
        byte[] osAppId = null;
        if (cp.mApnContext.getApnTypeBitmask() == ApnSetting.TYPE_ENTERPRISE) {
            osAppId = getEnterpriseOsAppId();
        } else {
            dnn = mApnSetting.getApnName();
        }
        final TrafficDescriptor td = osAppId == null && dnn == null ? null
                : new TrafficDescriptor(dnn, osAppId);
        final boolean matchAllRuleAllowed = td == null || td.getOsAppId() == null;

        if (DBG) {
            log("allowRoaming=" + allowRoaming
                    + ", mPhone.getDataRoamingEnabled()=" + mPhone.getDataRoamingEnabled()
                    + ", isModemRoaming=" + isModemRoaming
                    + ", mPhone.getServiceState().getDataRoaming()="
                    + mPhone.getServiceState().getDataRoaming()
                    + ", isUnmeteredApnType=" + isUnmeteredApnType
                    + ", trafficDescriptor=" + td
                    + ", matchAllRuleAllowed=" + matchAllRuleAllowed
            );
        }

        // Check if this data setup is a handover.
        LinkProperties linkProperties = null;
        int reason = DataService.REQUEST_REASON_NORMAL;
        if (cp.mRequestType == REQUEST_TYPE_HANDOVER) {
            // If this is a data setup for handover, we need to pass the link properties
            // of the existing data connection to the modem.
            DcTracker srcDcTracker = mPhone.getDcTracker(getHandoverSourceTransport());
            if (srcDcTracker == null || cp.mApnContext == null) {
                loge("connect: Handover failed. dcTracker=" + srcDcTracker + ", apnContext="
                        + cp.mApnContext);
                return DataFailCause.HANDOVER_FAILED;
            }


            // srcDc is the source data connection while the current instance is the target
            DataConnection srcDc =
                    srcDcTracker.getDataConnectionByApnType(cp.mApnContext.getApnType());
            if (srcDc == null) {
                loge("connect: Can't find data connection for handover.");
                return DataFailCause.HANDOVER_FAILED;
            }

            // Helpful for logging purposes
            DataServiceManager srcDsm = srcDc.mDataServiceManager;
            String srcDsmTag = (srcDsm == null ? "(null)" : srcDsm.getTag());
            logd("connect: REQUEST_TYPE_HANDOVER - Request handover from " + srcDc.getName()
                    + ", targetDsm=" + mDataServiceManager.getTag()
                    + ", sourceDsm=" + srcDsmTag);


            /* startHandover is called on the source data connection, and if successful,
               we ask the target data connection (which is the current instance) to call
               #setupDataCall with request type handover.
            */
            Consumer<Integer> onCompleted = (dataServiceCallbackResultCode) ->
                    /* startHandover is called on the srcDc handler, but the callback needs to
                       be called on the current (which is the targetDc) handler which is why we
                       call sendRunnableMessage. */
                    sendRunnableMessage(EVENT_START_HANDOVER_ON_TARGET,
                        (inCorrectState) -> requestHandover(inCorrectState, srcDc,
                            dataServiceCallbackResultCode,
                            cp, msg, dp, isModemRoaming, allowRoaming));
            srcDc.startHandover(onCompleted);
            return DataFailCause.NONE;
        }

        // setup data call for REQUEST_TYPE_NORMAL
        mDoAllocatePduSessionId = mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WLAN;
        allocatePduSessionId(psi -> {
            this.setPduSessionId(psi);
            mDataServiceManager.setupDataCall(
                    ServiceState.rilRadioTechnologyToAccessNetworkType(cp.mRilRat),
                    dp,
                    isModemRoaming,
                    allowRoaming,
                    reason,
                    linkProperties,
                    psi,
                    null, //slice info is null since this is not a handover
                    td,
                    matchAllRuleAllowed,
                    msg);
            TelephonyMetrics.getInstance().writeSetupDataCall(mPhone.getPhoneId(), cp.mRilRat,
                    dp.getProfileId(), dp.getApn(), dp.getProtocolType());
        });
        return DataFailCause.NONE;
    }

    private void allocatePduSessionId(Consumer<Integer> allocateCallback) {
        if (mDoAllocatePduSessionId) {
            Message msg = this.obtainMessage(EVENT_ALLOCATE_PDU_SESSION_ID);
            msg.obj = allocateCallback;
            mPhone.mCi.allocatePduSessionId(msg);
        } else {
            allocateCallback.accept(PDU_SESSION_ID_NOT_SET);
        }
    }

    private void onRquestHandoverFailed(ConnectionParams cp) {
        sendMessage(obtainMessage(EVENT_CANCEL_HANDOVER));
        notifyConnectCompleted(cp, DataFailCause.UNKNOWN,
                DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN, false);
    }

    private void requestHandover(boolean inCorrectState, DataConnection srcDc,
            @DataServiceCallback.ResultCode int resultCode,
            ConnectionParams cp, Message msg, DataProfile dp, boolean isModemRoaming,
            boolean allowRoaming) {

        if (!inCorrectState) {
            logd("requestHandover: Not in correct state");
            if (isResultCodeSuccess(resultCode)) {
                if (srcDc != null) {
                    logd("requestHandover: Not in correct state - Success result code");
                    // We need to cancel the handover on source if we ended up in the wrong state.
                    srcDc.cancelHandover();
                } else {
                    logd("requestHandover: Not in correct state - Success result code - "
                            + "srcdc = null");
                }
            }
            onRquestHandoverFailed(cp);
            return;
        } else if (!isResultCodeSuccess(resultCode)) {
            if (DBG) {
                logd("requestHandover: Non success result code from DataService, "
                        + "setupDataCall will not be called, result code = "
                        + DataServiceCallback.resultCodeToString(resultCode));
            }
            onRquestHandoverFailed(cp);
            return;
        }

        if (srcDc == null) {
            loge("requestHandover: Cannot find source data connection.");
            onRquestHandoverFailed(cp);
            return;
        }

        LinkProperties linkProperties;
        int reason;

        // Preserve the potential network agent from the source data connection. The ownership
        // is not transferred at this moment.
        mHandoverSourceNetworkAgent = srcDc.getNetworkAgent();
        if (mHandoverSourceNetworkAgent == null) {
            loge("requestHandover: Cannot get network agent from the source dc " + srcDc.getName());
            onRquestHandoverFailed(cp);
            return;
        }

        linkProperties = srcDc.getLinkProperties();
        if (linkProperties == null || linkProperties.getLinkAddresses().isEmpty()) {
            loge("requestHandover: Can't find link properties of handover data connection. dc="
                    + srcDc);
            onRquestHandoverFailed(cp);
            return;
        }

        mHandoverLocalLog.log("Handover started. Preserved the agent.");
        log("Get the handover source network agent: " + mHandoverSourceNetworkAgent);

        reason = DataService.REQUEST_REASON_HANDOVER;

        TrafficDescriptor td = dp.getApn() == null ? null
                : new TrafficDescriptor(dp.getApn(), null);
        boolean matchAllRuleAllowed = true;

        mDataServiceManager.setupDataCall(
                ServiceState.rilRadioTechnologyToAccessNetworkType(cp.mRilRat),
                dp,
                isModemRoaming,
                allowRoaming,
                reason,
                linkProperties,
                srcDc.getPduSessionId(),
                srcDc.getSliceInfo(),
                td,
                matchAllRuleAllowed,
                msg);
        TelephonyMetrics.getInstance().writeSetupDataCall(mPhone.getPhoneId(), cp.mRilRat,
                dp.getProfileId(), dp.getApn(), dp.getProtocolType());
    }

    /**
     * Called on the source data connection from the target data connection.
     */
    @VisibleForTesting
    public void startHandover(Consumer<Integer> onTargetDcComplete) {
        logd("startHandover: " + toStringSimple());
        // Set the handover state to being transferred on "this" data connection which is the src.
        setHandoverState(HANDOVER_STATE_BEING_TRANSFERRED);

        Consumer<Integer> onSrcDcComplete =
                resultCode -> onHandoverStarted(resultCode, onTargetDcComplete);
        /*
            The flow here is:
            srcDc#startHandover -> dataService#startHandover -> (onHandoverStarted) ->
                onSrcDcComplete -> onTargetDcComplete
         */
        mDataServiceManager.startHandover(mCid,
                this.obtainMessage(EVENT_START_HANDOVER,
                        onSrcDcComplete));
    }

    /**
     * Called on the source data connection when the async call to start handover is complete
     */
    private void onHandoverStarted(@DataServiceCallback.ResultCode int resultCode,
            Consumer<Integer> onTargetDcComplete) {
        logd("onHandoverStarted: " + toStringSimple());
        if (!isResultCodeSuccess(resultCode)) {
            setHandoverState(HANDOVER_STATE_IDLE);
        }
        onTargetDcComplete.accept(resultCode);
    }

    private void cancelHandover() {
        if (mHandoverState != HANDOVER_STATE_BEING_TRANSFERRED) {
            logd("cancelHandover: handover state is " + handoverStateToString(mHandoverState)
                    + ", expecting HANDOVER_STATE_BEING_TRANSFERRED");
        }
        mDataServiceManager.cancelHandover(mCid, this.obtainMessage(EVENT_CANCEL_HANDOVER));
        setHandoverState(HANDOVER_STATE_IDLE);
    }

    /**
     * Update NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED based on congested override
     * @param isCongested whether this DC should be set to congested or not
     */
    public void onCongestednessChanged(boolean isCongested) {
        sendMessage(obtainMessage(EVENT_DATA_CONNECTION_CONGESTEDNESS_CHANGED, isCongested));
    }

    /**
     * Update NetworkCapabilities.NET_CAPABILITY_NOT_METERED based on metered override
     * @param isUnmetered whether this DC should be set to unmetered or not
     */
    public void onMeterednessChanged(boolean isUnmetered) {
        sendMessage(obtainMessage(EVENT_DATA_CONNECTION_METEREDNESS_CHANGED, isUnmetered));
    }

    /**
     * TearDown the data connection when the deactivation is complete a Message with
     * msg.what == EVENT_DEACTIVATE_DONE
     *
     * @param o is the object returned in the AsyncResult.obj.
     */
    private void tearDownData(Object o) {
        int discReason = DataService.REQUEST_REASON_NORMAL;
        ApnContext apnContext = null;
        if ((o != null) && (o instanceof DisconnectParams)) {
            DisconnectParams dp = (DisconnectParams) o;
            apnContext = dp.mApnContext;
            if (TextUtils.equals(dp.mReason, Phone.REASON_RADIO_TURNED_OFF)
                    || TextUtils.equals(dp.mReason, Phone.REASON_PDP_RESET)) {
                discReason = DataService.REQUEST_REASON_SHUTDOWN;
            } else if (dp.mReleaseType == DcTracker.RELEASE_TYPE_HANDOVER) {
                discReason = DataService.REQUEST_REASON_HANDOVER;
            }
        }

        String str = "tearDownData. mCid=" + mCid + ", reason=" + discReason;
        if (DBG) log(str);
        ApnContext.requestLog(apnContext, str);


        //Needed to be final to work in a closure
        final int fDiscReason = discReason;
        releasePduSessionId(() -> {
            // This is run after release pdu session id is complete
            this.setPduSessionId(PDU_SESSION_ID_NOT_SET);
            mDataServiceManager.deactivateDataCall(mCid, fDiscReason,
                    obtainMessage(EVENT_DEACTIVATE_DONE, mTag, 0, o));
            mDataCallSessionStats.setDeactivateDataCallReason(fDiscReason);
        });
    }

    private void releasePduSessionId(Runnable releaseCallback) {
        // If the transport is IWLAN, and there is a valid PDU session id, also the data connection
        // is not being handovered, we should release the pdu session id.
        if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WLAN
                && mHandoverState == HANDOVER_STATE_IDLE
                && this.getPduSessionId() != PDU_SESSION_ID_NOT_SET) {
            Message msg = this.obtainMessage(EVENT_RELEASE_PDU_SESSION_ID);
            msg.obj = releaseCallback;
            mPhone.mCi.releasePduSessionId(msg, this.getPduSessionId());
        } else {
            // Just go and run the callback since we either have no pdu session id to release
            // or we are in the middle of a handover
            releaseCallback.run();
        }
    }

    private void notifyAllWithEvent(ApnContext alreadySent, int event, String reason) {
        for (ConnectionParams cp : mApnContexts.values()) {
            ApnContext apnContext = cp.mApnContext;
            if (apnContext == alreadySent) continue;
            if (reason != null) apnContext.setReason(reason);
            Pair<ApnContext, Integer> pair = new Pair<>(apnContext, cp.mConnectionGeneration);
            Message msg = mDct.obtainMessage(event, cp.mRequestType,
                    DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN, pair);
            AsyncResult.forMessage(msg);
            msg.sendToTarget();
        }
    }

    /**
     * Send the connectionCompletedMsg.
     *
     * @param cp is the ConnectionParams
     * @param cause and if no error the cause is DataFailCause.NONE
     * @param handoverFailureMode The action on handover failure
     * @param sendAll is true if all contexts are to be notified
     */
    private void notifyConnectCompleted(ConnectionParams cp, @DataFailureCause int cause,
            @HandoverFailureMode int handoverFailureMode, boolean sendAll) {
        ApnContext alreadySent = null;

        if (cp != null && cp.mOnCompletedMsg != null) {
            // Get the completed message but only use it once
            Message connectionCompletedMsg = cp.mOnCompletedMsg;
            cp.mOnCompletedMsg = null;
            alreadySent = cp.mApnContext;

            long timeStamp = System.currentTimeMillis();
            connectionCompletedMsg.arg1 = cp.mRequestType;
            connectionCompletedMsg.arg2 = handoverFailureMode;

            if (cause == DataFailCause.NONE) {
                mCreateTime = timeStamp;
                AsyncResult.forMessage(connectionCompletedMsg);
            } else {
                mLastFailCause = cause;
                mLastFailTime = timeStamp;

                // Return message with a Throwable exception to signify an error.
                if (cause == DataFailCause.NONE) cause = DataFailCause.UNKNOWN;
                AsyncResult.forMessage(connectionCompletedMsg, cause,
                        new Throwable(DataFailCause.toString(cause)));
            }
            if (DBG) {
                log("notifyConnectCompleted at " + timeStamp + " cause="
                        + DataFailCause.toString(cause) + " connectionCompletedMsg="
                        + msgToString(connectionCompletedMsg));
            }

            connectionCompletedMsg.sendToTarget();
        }
        if (sendAll) {
            log("Send to all. " + alreadySent + " " + DataFailCause.toString(cause));
            notifyAllWithEvent(alreadySent, DctConstants.EVENT_DATA_SETUP_COMPLETE_ERROR,
                    DataFailCause.toString(cause));
        }
    }

    /**
     * Send ar.userObj if its a message, which is should be back to originator.
     *
     * @param dp is the DisconnectParams.
     */
    private void notifyDisconnectCompleted(DisconnectParams dp, boolean sendAll) {
        if (VDBG) log("NotifyDisconnectCompleted");

        ApnContext alreadySent = null;
        String reason = null;

        if (dp != null && dp.mOnCompletedMsg != null) {
            // Get the completed message but only use it once
            Message msg = dp.mOnCompletedMsg;
            dp.mOnCompletedMsg = null;
            if (msg.obj instanceof ApnContext) {
                alreadySent = (ApnContext)msg.obj;
            }
            reason = dp.mReason;
            if (VDBG) {
                log(String.format("msg=%s msg.obj=%s", msg.toString(),
                    ((msg.obj instanceof String) ? (String) msg.obj : "<no-reason>")));
            }
            AsyncResult.forMessage(msg);
            msg.sendToTarget();
        }
        if (sendAll) {
            if (reason == null) {
                reason = DataFailCause.toString(DataFailCause.UNKNOWN);
            }
            notifyAllWithEvent(alreadySent, DctConstants.EVENT_DISCONNECT_DONE, reason);
        }
        if (DBG) log("NotifyDisconnectCompleted DisconnectParams=" + dp);
    }

    private void sendRunnableMessage(int eventCode, @NonNull final Consumer<Boolean> r) {
        sendMessage(eventCode, r);
    }

    /*
     * **************************************************************************
     * Begin Members and methods owned by DataConnectionTracker but stored
     * in a DataConnection because there is one per connection.
     * **************************************************************************
     */

    /*
     * The id is owned by DataConnectionTracker.
     */
    private int mId;

    /**
     * Get the DataConnection ID
     */
    public int getDataConnectionId() {
        return mId;
    }

    /*
     * **************************************************************************
     * End members owned by DataConnectionTracker
     * **************************************************************************
     */

    /**
     * Clear all settings called when entering mInactiveState.
     */
    private synchronized void clearSettings() {
        if (DBG) log("clearSettings");

        mCreateTime = -1;
        mLastFailTime = -1;
        mLastFailCause = DataFailCause.NONE;
        mCid = -1;

        mPcscfAddr = new String[5];

        mLinkProperties = new LinkProperties();
        mApnContexts.clear();
        mApnSetting = null;
        mUnmeteredUseOnly = false;
        mMmsUseOnly = false;
        mEnterpriseUse = false;
        mRestrictedNetworkOverride = false;
        mDcFailCause = DataFailCause.NONE;
        mDisabledApnTypeBitMask = 0;
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mCongestedOverride = false;
        mUnmeteredOverride = false;
        mDownlinkBandwidth = 14;
        mUplinkBandwidth = 14;
        mIsSuspended = false;
        mHandoverState = HANDOVER_STATE_IDLE;
        mHandoverFailureMode = DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN;
        mSliceInfo = null;
        mDefaultQos = null;
        mDoAllocatePduSessionId = false;
        mQosBearerSessions.clear();
        mTrafficDescriptors.clear();
    }

    /**
     * Process setup data completion result from data service
     *
     * @param resultCode The result code returned by data service
     * @param response Data call setup response from data service
     * @param cp The original connection params used for data call setup
     * @return Setup result
     */
    private SetupResult onSetupConnectionCompleted(@DataServiceCallback.ResultCode int resultCode,
                                                   DataCallResponse response,
                                                   ConnectionParams cp) {
        SetupResult result;

        log("onSetupConnectionCompleted: resultCode=" + resultCode + ", response=" + response);
        if (cp.mTag != mTag) {
            if (DBG) {
                log("onSetupConnectionCompleted stale cp.tag=" + cp.mTag + ", mtag=" + mTag);
            }
            result = SetupResult.ERROR_STALE;
        } else if (resultCode == DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE) {
            result = SetupResult.ERROR_RADIO_NOT_AVAILABLE;
            result.mFailCause = DataFailCause.RADIO_NOT_AVAILABLE;
        } else if (resultCode == DataServiceCallback.RESULT_ERROR_TEMPORARILY_UNAVAILABLE) {
            result = SetupResult.ERROR_DATA_SERVICE_SPECIFIC_ERROR;
            result.mFailCause = DataFailCause.SERVICE_TEMPORARILY_UNAVAILABLE;
        } else if (resultCode == DataServiceCallback.RESULT_ERROR_INVALID_ARG) {
            result = SetupResult.ERROR_INVALID_ARG;
            result.mFailCause = DataFailCause.UNACCEPTABLE_NETWORK_PARAMETER;
        } else if (response.getCause() != 0) {
            if (response.getCause() == DataFailCause.RADIO_NOT_AVAILABLE) {
                result = SetupResult.ERROR_RADIO_NOT_AVAILABLE;
                result.mFailCause = DataFailCause.RADIO_NOT_AVAILABLE;
            } else {
                result = SetupResult.ERROR_DATA_SERVICE_SPECIFIC_ERROR;
                result.mFailCause = DataFailCause.getFailCause(response.getCause());
            }
        } else if (cp.mApnContext.getApnTypeBitmask() == ApnSetting.TYPE_ENTERPRISE
                && mDcController.getActiveDcByCid(response.getId()) != null) {
            if (!mDcController.getTrafficDescriptorsForCid(response.getId())
                    .equals(response.getTrafficDescriptors())) {
                if (DBG) log("Updating traffic descriptors: " + response.getTrafficDescriptors());
                mDcController.getActiveDcByCid(response.getId()).updateTrafficDescriptors(response);
                mDct.obtainMessage(DctConstants.EVENT_TRAFFIC_DESCRIPTORS_UPDATED).sendToTarget();
            }
            if (DBG) log("DataConnection already exists for cid: " + response.getId());
            result = SetupResult.ERROR_DUPLICATE_CID;
            result.mFailCause = DataFailCause.DUPLICATE_CID;
        } else if (cp.mApnContext.getApnTypeBitmask() == ApnSetting.TYPE_ENTERPRISE
                && !mDcController.isDefaultDataActive()) {
            if (DBG) log("No default data connection currently active");
            mCid = response.getId();
            result = SetupResult.ERROR_NO_DEFAULT_CONNECTION;
            result.mFailCause = DataFailCause.NO_DEFAULT_DATA;
        } else {
            if (DBG) log("onSetupConnectionCompleted received successful DataCallResponse");
            mCid = response.getId();
            setPduSessionId(response.getPduSessionId());
            updatePcscfAddr(response);
            updateResponseFields(response);
            result = updateLinkProperty(response).setupResult;
        }

        return result;
    }

    private static boolean isResultCodeSuccess(int resultCode) {
        return resultCode == DataServiceCallback.RESULT_SUCCESS
                || resultCode == DataServiceCallback.RESULT_ERROR_UNSUPPORTED;
    }

    private boolean isDnsOk(String[] domainNameServers) {
        if (NULL_IP.equals(domainNameServers[0]) && NULL_IP.equals(domainNameServers[1])
                && !mPhone.isDnsCheckDisabled()) {
            // Work around a race condition where QMI does not fill in DNS:
            // Deactivate PDP and let DataConnectionTracker retry.
            // Do not apply the race condition workaround for MMS APN
            // if Proxy is an IP-address.
            // Otherwise, the default APN will not be restored anymore.
            if (!isIpAddress(mApnSetting.getMmsProxyAddressAsString())) {
                log(String.format(
                        "isDnsOk: return false apn.types=%d APN_TYPE_MMS=%s isIpAddress(%s)=%s",
                        mApnSetting.getApnTypeBitmask(), ApnSetting.TYPE_MMS_STRING,
                        mApnSetting.getMmsProxyAddressAsString(),
                        isIpAddress(mApnSetting.getMmsProxyAddressAsString())));
                return false;
            }
        }
        return true;
    }

    /**
     * TCP buffer size config based on the ril technology. There are 6 parameters
     * read_min, read_default, read_max, write_min, write_default, write_max in the TCP buffer
     * config string and they are separated by a comma. The unit of these parameters is byte.
     */
    private static final String TCP_BUFFER_SIZES_GPRS = "4092,8760,48000,4096,8760,48000";
    private static final String TCP_BUFFER_SIZES_EDGE = "4093,26280,70800,4096,16384,70800";
    private static final String TCP_BUFFER_SIZES_UMTS = "58254,349525,1048576,58254,349525,1048576";
    private static final String TCP_BUFFER_SIZES_1XRTT = "16384,32768,131072,4096,16384,102400";
    private static final String TCP_BUFFER_SIZES_EVDO = "4094,87380,262144,4096,16384,262144";
    private static final String TCP_BUFFER_SIZES_EHRPD = "131072,262144,1048576,4096,16384,524288";
    private static final String TCP_BUFFER_SIZES_HSDPA = "61167,367002,1101005,8738,52429,262114";
    private static final String TCP_BUFFER_SIZES_HSPA = "40778,244668,734003,16777,100663,301990";
    private static final String TCP_BUFFER_SIZES_LTE =
            "524288,1048576,2097152,262144,524288,1048576";
    private static final String TCP_BUFFER_SIZES_HSPAP =
            "122334,734003,2202010,32040,192239,576717";
    private static final String TCP_BUFFER_SIZES_NR =
            "2097152,6291456,16777216,512000,2097152,8388608";
    private static final String TCP_BUFFER_SIZES_LTE_CA =
            "4096,6291456,12582912,4096,1048576,2097152";

    private void updateTcpBufferSizes(int rilRat) {
        String sizes = null;
        ServiceState ss = mPhone.getServiceState();
        if (rilRat == ServiceState.RIL_RADIO_TECHNOLOGY_LTE &&
                ss.isUsingCarrierAggregation()) {
            rilRat = ServiceState.RIL_RADIO_TECHNOLOGY_LTE_CA;
        }
        String ratName = ServiceState.rilRadioTechnologyToString(rilRat).toLowerCase(Locale.ROOT);
        // ServiceState gives slightly different names for EVDO tech ("evdo-rev.0" for ex)
        // - patch it up:
        if (rilRat == ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_0 ||
                rilRat == ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_A ||
                rilRat == ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_B) {
            ratName = RAT_NAME_EVDO;
        }

        // NR 5G Non-Standalone use LTE cell as the primary cell, the ril technology is LTE in this
        // case. We use NR 5G TCP buffer size when connected to NR 5G Non-Standalone network.
        if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                && ((rilRat == ServiceState.RIL_RADIO_TECHNOLOGY_LTE ||
                rilRat == ServiceState.RIL_RADIO_TECHNOLOGY_LTE_CA) && isNRConnected())
                && mPhone.getServiceStateTracker().getNrContextIds().contains(mCid)) {
            ratName = RAT_NAME_5G;
        }

        log("updateTcpBufferSizes: " + ratName);

        // in the form: "ratname:rmem_min,rmem_def,rmem_max,wmem_min,wmem_def,wmem_max"
        String[] configOverride = mPhone.getContext().getResources().getStringArray(
                com.android.internal.R.array.config_mobile_tcp_buffers);
        for (int i = 0; i < configOverride.length; i++) {
            String[] split = configOverride[i].split(":");
            if (ratName.equals(split[0]) && split.length == 2) {
                sizes = split[1];
                break;
            }
        }

        if (sizes == null) {
            // no override - use telephony defaults
            // doing it this way allows device or carrier to just override the types they
            // care about and inherit the defaults for the others.
            switch (rilRat) {
                case ServiceState.RIL_RADIO_TECHNOLOGY_GPRS:
                    sizes = TCP_BUFFER_SIZES_GPRS;
                    break;
                case ServiceState.RIL_RADIO_TECHNOLOGY_EDGE:
                    sizes = TCP_BUFFER_SIZES_EDGE;
                    break;
                case ServiceState.RIL_RADIO_TECHNOLOGY_UMTS:
                    sizes = TCP_BUFFER_SIZES_UMTS;
                    break;
                case ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT:
                    sizes = TCP_BUFFER_SIZES_1XRTT;
                    break;
                case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_0:
                case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_A:
                case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_B:
                    sizes = TCP_BUFFER_SIZES_EVDO;
                    break;
                case ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD:
                    sizes = TCP_BUFFER_SIZES_EHRPD;
                    break;
                case ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA:
                    sizes = TCP_BUFFER_SIZES_HSDPA;
                    break;
                case ServiceState.RIL_RADIO_TECHNOLOGY_HSPA:
                case ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA:
                    sizes = TCP_BUFFER_SIZES_HSPA;
                    break;
                case ServiceState.RIL_RADIO_TECHNOLOGY_LTE:
                    // Use NR 5G TCP buffer size when connected to NR 5G Non-Standalone network.
                    if (RAT_NAME_5G.equals(ratName)) {
                        sizes = TCP_BUFFER_SIZES_NR;
                    } else {
                        sizes = TCP_BUFFER_SIZES_LTE;
                    }
                    break;
                case ServiceState.RIL_RADIO_TECHNOLOGY_LTE_CA:
                    // Use NR 5G TCP buffer size when connected to NR 5G Non-Standalone network.
                    if (RAT_NAME_5G.equals(ratName)) {
                        sizes = TCP_BUFFER_SIZES_NR;
                    } else {
                        sizes = TCP_BUFFER_SIZES_LTE_CA;
                    }
                    break;
                case ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP:
                    sizes = TCP_BUFFER_SIZES_HSPAP;
                    break;
                case ServiceState.RIL_RADIO_TECHNOLOGY_NR:
                    sizes = TCP_BUFFER_SIZES_NR;
                    break;
                default:
                    // Leave empty - this will let ConnectivityService use the system default.
                    break;
            }
        }
        mLinkProperties.setTcpBufferSizes(sizes);
    }

    private void updateLinkBandwidthsFromCarrierConfig(int rilRat) {
        String ratName = DataConfigManager.getDataConfigNetworkType(
                mPhone.getDisplayInfoController().getTelephonyDisplayInfo());

        if (DBG) log("updateLinkBandwidthsFromCarrierConfig: " + ratName);

        Pair<Integer, Integer> values = mDct.getLinkBandwidthsFromCarrierConfig(ratName);
        if (values == null) {
            values = new Pair<>(14, 14);
        }
        mDownlinkBandwidth = values.first;
        mUplinkBandwidth = values.second;
    }


    private void updateLinkBandwidthsFromModem(List<LinkCapacityEstimate> lceList) {
        if (DBG) log("updateLinkBandwidthsFromModem: lceList=" + lceList);
        boolean downlinkUpdated = false;
        boolean uplinkUpdated = false;
        LinkCapacityEstimate lce = lceList.get(0);
        // LCE status deprecated in IRadio 1.2, so only check for IRadio < 1.2
        if (mPhone.getHalVersion().greaterOrEqual(RIL.RADIO_HAL_VERSION_1_2)
                || mPhone.getLceStatus() == RILConstants.LCE_ACTIVE) {
            if (lce.getDownlinkCapacityKbps() != LinkCapacityEstimate.INVALID) {
                mDownlinkBandwidth = lce.getDownlinkCapacityKbps();
                downlinkUpdated = true;
            }
            if (lce.getUplinkCapacityKbps() != LinkCapacityEstimate.INVALID) {
                mUplinkBandwidth = lce.getUplinkCapacityKbps();
                uplinkUpdated = true;
            }
        }

        if (!downlinkUpdated || !uplinkUpdated) {
            fallBackToCarrierConfigValues(downlinkUpdated, uplinkUpdated);
        }

        if (mNetworkAgent != null) {
            mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(), DataConnection.this);
        }
    }

    private void updateLinkBandwidthsFromBandwidthEstimator(int uplinkBandwidthKbps,
            int downlinkBandwidthKbps) {
        if (DBG) {
            log("updateLinkBandwidthsFromBandwidthEstimator, UL= "
                    + uplinkBandwidthKbps + " DL= " + downlinkBandwidthKbps);
        }
        boolean downlinkUpdated = false;
        boolean uplinkUpdated = false;
        if (downlinkBandwidthKbps > 0) {
            mDownlinkBandwidth = downlinkBandwidthKbps;
            downlinkUpdated = true;
        }
        if (uplinkBandwidthKbps > 0) {
            mUplinkBandwidth = uplinkBandwidthKbps;
            uplinkUpdated = true;
        }

        if (!downlinkUpdated || !uplinkUpdated) {
            fallBackToCarrierConfigValues(downlinkUpdated, uplinkUpdated);
        }
        if (mNetworkAgent != null) {
            mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(), DataConnection.this);
        }
    }

    private void fallBackToCarrierConfigValues(boolean downlinkUpdated, boolean uplinkUpdated) {
        String ratName = ServiceState.rilRadioTechnologyToString(mRilRat);
        if (mRilRat == ServiceState.RIL_RADIO_TECHNOLOGY_LTE && isNRConnected()) {
            ratName = mPhone.getServiceState().getNrFrequencyRange()
                    == ServiceState.FREQUENCY_RANGE_MMWAVE
                    ? DctConstants.RAT_NAME_NR_NSA_MMWAVE : DctConstants.RAT_NAME_NR_NSA;
        }
        Pair<Integer, Integer> values = mDct.getLinkBandwidthsFromCarrierConfig(ratName);
        if (values != null) {
            if (!downlinkUpdated) {
                mDownlinkBandwidth = values.first;
            }
            if (!uplinkUpdated) {
                mUplinkBandwidth = values.second;
            }
            mUplinkBandwidth = Math.min(mUplinkBandwidth, mDownlinkBandwidth);
        }
    }

    private boolean isBandwidthSourceKey(String source) {
        return source.equals(mPhone.getContext().getResources().getString(
                com.android.internal.R.string.config_bandwidthEstimateSource));
    }

    /**
     * Indicates if this data connection was established for unmetered use only. Note that this
     * flag should be populated when data becomes active. And if it is set to true, it can be set to
     * false later when we are reevaluating the data connection. But if it is set to false, it
     * can never become true later because setting it to true will cause this data connection
     * losing some immutable network capabilities, which can cause issues in connectivity service.
     */
    private boolean mUnmeteredUseOnly = false;

    /**
     * Indicates if this data connection was established for MMS use only. This is true only when
     * mobile data is disabled but the user allows sending and receiving MMS messages. If the data
     * enabled settings indicate that MMS data is allowed unconditionally, MMS can be sent when data
     * is disabled even if it is a metered APN type.
     */
    private boolean mMmsUseOnly = false;

    /**
     * Indicates if when this connection was established we had a restricted/privileged
     * NetworkRequest and needed it to overcome data-enabled limitations.
     *
     * This flag overrides the APN-based restriction capability, restricting the network
     * based on both having a NetworkRequest with restricted AND needing a restricted
     * bit to overcome user-disabled status.  This allows us to handle the common case
     * of having both restricted requests and unrestricted requests for the same apn:
     * if conditions require a restricted network to overcome user-disabled then it must
     * be restricted, otherwise it is unrestricted (or restricted based on APN type).
     *
     * This supports a privileged app bringing up a network without general apps having access
     * to it when the network is otherwise unavailable (hipri).  The first use case is
     * pre-paid SIM reprovisioning over internet, where the carrier insists on no traffic
     * other than from the privileged carrier-app.
     *
     * Note that the data connection cannot go from unrestricted to restricted because the
     * connectivity service does not support dynamically closing TCP connections at this point.
     */
    private boolean mRestrictedNetworkOverride = false;

    /**
     * Indicates if this data connection supports enterprise use. Note that this flag should be
     * populated when data becomes active. Once it is set, the value cannot be changed because
     * setting it will cause this data connection to lose immutable network capabilities, which can
     * cause issues in connectivity service.
     */
    private boolean mEnterpriseUse = false;

    /**
     * Check if this data connection should be restricted. We should call this when data connection
     * becomes active, or when we want to re-evaluate the conditions to decide if we need to
     * unstrict the data connection.
     *
     * @return True if this data connection needs to be restricted.
     */
    private boolean shouldRestrictNetwork() {
        // first, check if there is any network request that containing restricted capability
        // (i.e. Do not have NET_CAPABILITY_NOT_RESTRICTED in the request)
        boolean isAnyRestrictedRequest = false;
        for (ApnContext apnContext : mApnContexts.keySet()) {
            if (apnContext.hasRestrictedRequests(true /* exclude DUN */)) {
                isAnyRestrictedRequest = true;
                break;
            }
        }

        // If all of the network requests are non-restricted, then we don't need to restrict
        // the network.
        if (!isAnyRestrictedRequest) {
            return false;
        }

        // If the network is unmetered, then we don't need to restrict the network because users
        // won't be charged anyway.
        if (!ApnSettingUtils.isMetered(mApnSetting, mPhone)) {
            return false;
        }

        // If the data is disabled, then we need to restrict the network so only privileged apps can
        // use the restricted network while data is disabled.
        if (!mPhone.getDataEnabledSettings().isDataEnabled()) {
            return true;
        }

        // If the device is roaming, and the user does not turn on data roaming, then we need to
        // restrict the network so only privileged apps can use it.
        if (!mDct.getDataRoamingEnabled() && mPhone.getServiceState().getDataRoaming()) {
            return true;
        }

        // Otherwise we should not restrict the network so anyone who requests can use it.
        return false;
    }

    /**
     * @return True if this data connection should only be used for unmetered purposes.
     */
    private boolean isUnmeteredUseOnly() {
        // If this data connection is on IWLAN, then it's unmetered and can be used by everyone.
        // Should not be for unmetered used only.
        if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
            return false;
        }

        // If data is enabled, this data connection can't be for unmetered used only because
        // everyone should be able to use it if:
        // 1. Device is not roaming, or
        // 2. Device is roaming and data roaming is turned on
        if (mPhone.getDataEnabledSettings().isDataEnabled()) {
            if (!mPhone.getServiceState().getDataRoaming() || mDct.getDataRoamingEnabled()) {
                return false;
            }
        }

        // The data connection can only be unmetered used only if all attached APN contexts
        // attached to this data connection are unmetered.
        for (ApnContext apnContext : mApnContexts.keySet()) {
            if (ApnSettingUtils.isMeteredApnType(apnContext.getApnTypeBitmask(), mPhone)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return True if this data connection should only be used for MMS purposes.
     */
    private boolean isMmsUseOnly() {
        // MMS use only if data is disabled, MMS is allowed unconditionally, and MMS is the only
        // APN type for this data connection.
        DataEnabledSettings des = mPhone.getDataEnabledSettings();
        boolean mmsAllowedUnconditionally = !des.isDataEnabled() && des.isMmsAlwaysAllowed();
        boolean mmsApnOnly = isApnContextAttached(ApnSetting.TYPE_MMS, true);
        return mmsAllowedUnconditionally && mmsApnOnly;
    }

    /**
     * Check if this data connection supports enterprise use. We call this when the data connection
     * becomes active or when we want to reevaluate the conditions to decide if we need to update
     * the network agent capabilities.
     *
     * @return True if this data connection supports enterprise use.
     */
    private boolean isEnterpriseUse() {
        return  mApnContexts.keySet().stream().anyMatch(
                ac -> ac.getApnTypeBitmask() == ApnSetting.TYPE_ENTERPRISE);
    }

    /**
     * Get the network capabilities for this data connection.
     *
     * @return the {@link NetworkCapabilities} of this data connection.
     */
    public NetworkCapabilities getNetworkCapabilities() {
        final NetworkCapabilities.Builder builder = new NetworkCapabilities.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        boolean unmeteredApns = false;

        if (mApnSetting != null && !mEnterpriseUse && !mMmsUseOnly) {
            final int[] types = ApnSetting.getApnTypesFromBitmask(
                    mApnSetting.getApnTypeBitmask() & ~mDisabledApnTypeBitMask);
            for (int type : types) {
                if ((!mRestrictedNetworkOverride && mUnmeteredUseOnly)
                        && ApnSettingUtils.isMeteredApnType(type, mPhone)) {
                    log("Dropped the metered " + ApnSetting.getApnTypeString(type)
                            + " type for the unmetered data call.");
                    continue;
                }
                switch (type) {
                    case ApnSetting.TYPE_ALL: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_SUPL);
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_FOTA);
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_CBS);
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_IA);
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_DUN);
                        break;
                    }
                    case ApnSetting.TYPE_DEFAULT: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                        break;
                    }
                    case ApnSetting.TYPE_MMS: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
                        break;
                    }
                    case ApnSetting.TYPE_SUPL: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_SUPL);
                        break;
                    }
                    case ApnSetting.TYPE_DUN: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_DUN);
                        break;
                    }
                    case ApnSetting.TYPE_FOTA: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_FOTA);
                        break;
                    }
                    case ApnSetting.TYPE_IMS: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);
                        break;
                    }
                    case ApnSetting.TYPE_CBS: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_CBS);
                        break;
                    }
                    case ApnSetting.TYPE_IA: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_IA);
                        break;
                    }
                    case ApnSetting.TYPE_EMERGENCY: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_EIMS);
                        break;
                    }
                    case ApnSetting.TYPE_MCX: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_MCX);
                        break;
                    }
                    case ApnSetting.TYPE_XCAP: {
                        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_XCAP);
                        break;
                    }
                    default:
                }
            }

            if (!ApnSettingUtils.isMetered(mApnSetting, mPhone)) {
                unmeteredApns = true;
            }
        }

        // Mark NOT_METERED in the following cases:
        // 1. All APNs in the APN settings are unmetered.
        // 2. The non-restricted data is intended for unmetered use only.
        if (unmeteredApns || (mUnmeteredUseOnly && !mRestrictedNetworkOverride)) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        } else {
            builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        }

        if (mEnterpriseUse) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE);
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }

        if (NetworkCapabilitiesUtils.inferRestrictedCapability(builder.build())) {
            builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        }

        if (mMmsUseOnly) {
            if (ApnSettingUtils.isMeteredApnType(ApnSetting.TYPE_MMS, mPhone)) {
                log("Adding unmetered capability for the unmetered MMS-only data connection");
                builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
            }
            log("Adding MMS capability for the MMS-only data connection");
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
        }

        if (mRestrictedNetworkOverride) {
            builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
            // don't use dun on restriction-overriden networks.
            builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_DUN);
        }

        builder.setLinkDownstreamBandwidthKbps(mDownlinkBandwidth);
        builder.setLinkUpstreamBandwidthKbps(mUplinkBandwidth);

        builder.setNetworkSpecifier(new TelephonyNetworkSpecifier.Builder()
                .setSubscriptionId(mSubId).build());
        builder.setSubscriptionIds(Collections.singleton(mSubId));

        if (!mPhone.getServiceState().getDataRoaming()) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);
        }

        if (!mCongestedOverride) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED);
        }

        if (mUnmeteredOverride) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED);
        }

        if (!mIsSuspended) {
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);
        }

        final int carrierServicePackageUid = getCarrierServicePackageUid();

        // TODO(b/205736323): Owner and Admin UIDs currently come from separate data sources. Unify
        //                    them, and remove ArrayUtils.contains() check.
        if (carrierServicePackageUid != Process.INVALID_UID
                && ArrayUtils.contains(mAdministratorUids, carrierServicePackageUid)) {
            builder.setOwnerUid(carrierServicePackageUid);
            builder.setAllowedUids(Collections.singleton(carrierServicePackageUid));
        }
        builder.setAdministratorUids(mAdministratorUids);

        // Always start with NOT_VCN_MANAGED, then remove if VcnManager indicates this is part of a
        // VCN.
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED);
        final VcnNetworkPolicyResult vcnPolicy = getVcnPolicy(builder.build());
        if (!vcnPolicy.getNetworkCapabilities()
                .hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED)) {
            builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED);
        }
        if (!vcnPolicy.getNetworkCapabilities()
                .hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)) {
            builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        }

        return builder.build();
    }

    // TODO(b/205736323): Once TelephonyManager#getCarrierServicePackageNameForLogicalSlot() is
    //                    plumbed to CarrierPrivilegesTracker's cache, query the cached UIDs.
    private int getFirstUidForPackage(String pkgName) {
        if (pkgName == null) {
            return Process.INVALID_UID;
        }

        List<UserInfo> users = mPhone.getContext().getSystemService(UserManager.class).getUsers();
        for (UserInfo user : users) {
            int userId = user.getUserHandle().getIdentifier();
            try {
                PackageManager pm = mPhone.getContext().getPackageManager();

                if (pm != null) {
                    return pm.getPackageUidAsUser(pkgName, userId);
                }
            } catch (NameNotFoundException exception) {
                // Didn't find package. Try other users
                Rlog.i(
                        "DataConnection",
                        "Unable to find uid for package " + pkgName + " and user " + userId);
            }
        }
        return Process.INVALID_UID;
    }

    private int getCarrierServicePackageUid() {
        String pkgName =
                mPhone.getContext()
                        .getSystemService(TelephonyManager.class)
                        .getCarrierServicePackageNameForLogicalSlot(mPhone.getPhoneId());

        return getFirstUidForPackage(pkgName);
    }

    /**
     * Check if the this data network is VCN-managed.
     *
     * @param networkCapabilities The network capabilities of this data network.
     * @return The VCN's policy for this DataNetwork.
     */
    private VcnNetworkPolicyResult getVcnPolicy(NetworkCapabilities networkCapabilities) {
        return mVcnManager.applyVcnNetworkPolicy(networkCapabilities, getLinkProperties());
    }

    /** @return {@code true} if validation is required, {@code false} otherwise. */
    public boolean isValidationRequired() {
        final NetworkCapabilities nc = getNetworkCapabilities();
        return nc != null
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
    }

    /**
     * @return {@code True} if 464xlat should be skipped.
     */
    @VisibleForTesting
    public boolean shouldSkip464Xlat() {
        switch (mApnSetting.getSkip464Xlat()) {
            case Telephony.Carriers.SKIP_464XLAT_ENABLE:
                return true;
            case Telephony.Carriers.SKIP_464XLAT_DISABLE:
                return false;
            case Telephony.Carriers.SKIP_464XLAT_DEFAULT:
            default:
                break;
        }

        // As default, return true if ims and no internet
        final NetworkCapabilities nc = getNetworkCapabilities();
        return nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_IMS)
                && !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * @return {@code} true iff. {@code address} is a literal IPv4 or IPv6 address.
     */
    @VisibleForTesting
    public static boolean isIpAddress(String address) {
        if (address == null) return false;

        // Accept IPv6 addresses (only) in square brackets for compatibility.
        if (address.startsWith("[") && address.endsWith("]") && address.indexOf(':') != -1) {
            address = address.substring(1, address.length() - 1);
        }
        return InetAddresses.isNumericAddress(address);
    }

    private SetupResult setLinkProperties(DataCallResponse response,
            LinkProperties linkProperties) {
        // Check if system property dns usable
        String propertyPrefix = "net." + response.getInterfaceName() + ".";
        String dnsServers[] = new String[2];
        dnsServers[0] = SystemProperties.get(propertyPrefix + "dns1");
        dnsServers[1] = SystemProperties.get(propertyPrefix + "dns2");
        boolean okToUseSystemPropertyDns = isDnsOk(dnsServers);

        SetupResult result;

        // Start with clean network properties and if we have
        // a failure we'll clear again at the bottom of this code.
        linkProperties.clear();

        if (response.getCause() == DataFailCause.NONE) {
            try {
                // set interface name
                linkProperties.setInterfaceName(response.getInterfaceName());

                // set link addresses
                if (response.getAddresses().size() > 0) {
                    for (LinkAddress la : response.getAddresses()) {
                        if (!la.getAddress().isAnyLocalAddress()) {
                            if (DBG) {
                                log("addr/pl=" + la.getAddress() + "/"
                                        + la.getPrefixLength());
                            }
                            linkProperties.addLinkAddress(la);
                        }
                    }
                } else {
                    throw new UnknownHostException("no address for ifname="
                            + response.getInterfaceName());
                }

                // set dns servers
                if (response.getDnsAddresses().size() > 0) {
                    for (InetAddress dns : response.getDnsAddresses()) {
                        if (!dns.isAnyLocalAddress()) {
                            linkProperties.addDnsServer(dns);
                        }
                    }
                } else if (okToUseSystemPropertyDns) {
                    for (String dnsAddr : dnsServers) {
                        dnsAddr = dnsAddr.trim();
                        if (dnsAddr.isEmpty()) continue;
                        InetAddress ia;
                        try {
                            ia = InetAddresses.parseNumericAddress(dnsAddr);
                        } catch (IllegalArgumentException e) {
                            throw new UnknownHostException("Non-numeric dns addr=" + dnsAddr);
                        }
                        if (!ia.isAnyLocalAddress()) {
                            linkProperties.addDnsServer(ia);
                        }
                    }
                } else {
                    throw new UnknownHostException("Empty dns response and no system default dns");
                }

                // set pcscf
                if (response.getPcscfAddresses().size() > 0) {
                    for (InetAddress pcscf : response.getPcscfAddresses()) {
                        linkProperties.addPcscfServer(pcscf);
                    }
                }

                for (InetAddress gateway : response.getGatewayAddresses()) {
                    int mtu = gateway instanceof java.net.Inet6Address ? response.getMtuV6()
                            : response.getMtuV4();
                    // Allow 0.0.0.0 or :: as a gateway;
                    // this indicates a point-to-point interface.
                    linkProperties.addRoute(new RouteInfo(null, gateway, null,
                            RouteInfo.RTN_UNICAST, mtu));
                }

                // set interface MTU
                // this may clobber the setting read from the APN db, but that's ok
                // TODO: remove once LinkProperties#setMtu is deprecated
                linkProperties.setMtu(response.getMtu());

                result = SetupResult.SUCCESS;
            } catch (UnknownHostException e) {
                log("setLinkProperties: UnknownHostException " + e);
                result = SetupResult.ERROR_INVALID_ARG;
            }
        } else {
            result = SetupResult.ERROR_DATA_SERVICE_SPECIFIC_ERROR;
        }

        // An error occurred so clear properties
        if (result != SetupResult.SUCCESS) {
            if (DBG) {
                log("setLinkProperties: error clearing LinkProperties status="
                        + response.getCause() + " result=" + result);
            }
            linkProperties.clear();
        }

        return result;
    }

    /**
     * Initialize connection, this will fail if the
     * apnSettings are not compatible.
     *
     * @param cp the Connection parameters
     * @return true if initialization was successful.
     */
    private boolean initConnection(ConnectionParams cp) {
        ApnContext apnContext = cp.mApnContext;
        if (mApnSetting == null) {
            // Only change apn setting if it isn't set, it will
            // only NOT be set only if we're in DcInactiveState.
            mApnSetting = apnContext.getApnSetting();
        }
        if (mApnSetting == null || (!mApnSetting.canHandleType(apnContext.getApnTypeBitmask())
                && apnContext.getApnTypeBitmask() != ApnSetting.TYPE_ENTERPRISE)) {
            if (DBG) {
                log("initConnection: incompatible apnSetting in ConnectionParams cp=" + cp
                        + " dc=" + DataConnection.this);
            }
            return false;
        }
        mTag += 1;
        mConnectionParams = cp;
        mConnectionParams.mTag = mTag;

        // always update the ConnectionParams with the latest or the
        // connectionGeneration gets stale
        mApnContexts.put(apnContext, cp);

        if (DBG) {
            log("initConnection: "
                    + " RefCount=" + mApnContexts.size()
                    + " mApnList=" + mApnContexts
                    + " mConnectionParams=" + mConnectionParams);
        }
        return true;
    }

    /**
     * The parent state for all other states.
     */
    private class DcDefaultState extends State {
        @Override
        public void enter() {
            if (DBG) log("DcDefaultState: enter");

            // Register for DRS or RAT change
            mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(
                    mTransportType, getHandler(),
                    DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED, null);

            mPhone.getServiceStateTracker().registerForDataRoamingOn(getHandler(),
                    DataConnection.EVENT_DATA_CONNECTION_ROAM_ON, null);
            mPhone.getServiceStateTracker().registerForDataRoamingOff(getHandler(),
                    DataConnection.EVENT_DATA_CONNECTION_ROAM_OFF, null, true);
            mPhone.getServiceStateTracker().registerForNrStateChanged(getHandler(),
                    DataConnection.EVENT_NR_STATE_CHANGED, null);
            mPhone.getServiceStateTracker().registerForNrFrequencyChanged(getHandler(),
                    DataConnection.EVENT_NR_FREQUENCY_CHANGED, null);
            mPhone.getServiceStateTracker().registerForCssIndicatorChanged(getHandler(),
                    DataConnection.EVENT_CSS_INDICATOR_CHANGED, null);
            if (isBandwidthSourceKey(DctConstants.BANDWIDTH_SOURCE_BANDWIDTH_ESTIMATOR_KEY)) {
                mPhone.getLinkBandwidthEstimator().registerForBandwidthChanged(getHandler(),
                        DataConnection.EVENT_LINK_BANDWIDTH_ESTIMATOR_UPDATE, null);
            }

            // Add ourselves to the list of data connections
            mDcController.addDc(DataConnection.this);
        }
        @Override
        public void exit() {
            if (DBG) log("DcDefaultState: exit");

            // Unregister for DRS or RAT change.
            mPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(
                    mTransportType, getHandler());

            mPhone.getServiceStateTracker().unregisterForDataRoamingOn(getHandler());
            mPhone.getServiceStateTracker().unregisterForDataRoamingOff(getHandler());
            mPhone.getServiceStateTracker().unregisterForNrStateChanged(getHandler());
            mPhone.getServiceStateTracker().unregisterForNrFrequencyChanged(getHandler());
            mPhone.getServiceStateTracker().unregisterForCssIndicatorChanged(getHandler());
            if (isBandwidthSourceKey(DctConstants.BANDWIDTH_SOURCE_BANDWIDTH_ESTIMATOR_KEY)) {
                mPhone.getLinkBandwidthEstimator().unregisterForBandwidthChanged(getHandler());
            }

            // Remove ourselves from the DC lists
            mDcController.removeDc(DataConnection.this);

            if (mAc != null) {
                mAc.disconnected();
                mAc = null;
            }
            mApnContexts.clear();
            mReconnectIntent = null;
            mDct = null;
            mApnSetting = null;
            mPhone = null;
            mDataServiceManager = null;
            mLinkProperties = null;
            mLastFailCause = DataFailCause.NONE;
            mUserData = null;
            mDcController = null;
            mDcTesterFailBringUpAll = null;
        }

        @Override
        public boolean processMessage(Message msg) {
            boolean retVal = HANDLED;

            if (VDBG) {
                log("DcDefault msg=" + getWhatToString(msg.what)
                        + " RefCount=" + mApnContexts.size());
            }
            switch (msg.what) {
                case EVENT_RESET:
                    if (VDBG) log("DcDefaultState: msg.what=REQ_RESET");
                    transitionTo(mInactiveState);
                    break;
                case EVENT_CONNECT:
                    if (DBG) log("DcDefaultState: msg.what=EVENT_CONNECT, fail not expected");
                    ConnectionParams cp = (ConnectionParams) msg.obj;
                    notifyConnectCompleted(cp, DataFailCause.UNKNOWN,
                            DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN, false);
                    break;

                case EVENT_DISCONNECT:
                case EVENT_DISCONNECT_ALL:
                case EVENT_REEVALUATE_RESTRICTED_STATE:
                    if (DBG) {
                        log("DcDefaultState deferring msg.what=" + getWhatToString(msg.what)
                                + " RefCount=" + mApnContexts.size());
                    }
                    deferMessage(msg);
                    break;
                case EVENT_TEAR_DOWN_NOW:
                    if (DBG) log("DcDefaultState EVENT_TEAR_DOWN_NOW");
                    mDataServiceManager.deactivateDataCall(mCid, DataService.REQUEST_REASON_NORMAL,
                            null);
                    mDataCallSessionStats.setDeactivateDataCallReason(
                            DataService.REQUEST_REASON_NORMAL);
                    break;
                case EVENT_LOST_CONNECTION:
                    if (DBG) {
                        String s = "DcDefaultState ignore EVENT_LOST_CONNECTION"
                                + " tag=" + msg.arg1 + ":mTag=" + mTag;
                        logAndAddLogRec(s);
                    }
                    break;
                case EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED:
                    AsyncResult ar = (AsyncResult)msg.obj;
                    Pair<Integer, Integer> drsRatPair = (Pair<Integer, Integer>)ar.result;
                    mDataRegState = drsRatPair.first;
                    updateTcpBufferSizes(drsRatPair.second);
                    if (isBandwidthSourceKey(DctConstants.BANDWIDTH_SOURCE_CARRIER_CONFIG_KEY)) {
                        updateLinkBandwidthsFromCarrierConfig(drsRatPair.second);
                    }
                    mRilRat = drsRatPair.second;
                    if (DBG) {
                        log("DcDefaultState: EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED"
                                + " regState=" + ServiceState.rilServiceStateToString(mDataRegState)
                                + " RAT=" + ServiceState.rilRadioTechnologyToString(mRilRat));
                    }
                    mDataCallSessionStats.onDrsOrRatChanged(
                            ServiceState.rilRadioTechnologyToNetworkType(mRilRat));
                    break;

                case EVENT_START_HANDOVER:  //calls startHandover()
                    if (DBG) {
                        log("DcDefaultState: EVENT_START_HANDOVER not expected.");
                    }
                    Consumer<Integer> r = (Consumer<Integer>) msg.obj;
                    r.accept(DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
                    break;
                case EVENT_START_HANDOVER_ON_TARGET:
                    if (DBG) {
                        log("DcDefaultState: EVENT_START_HANDOVER not expected, but will "
                                + "clean up, result code: "
                                + DataServiceCallback.resultCodeToString(msg.arg1));
                    }
                    ((Consumer<Boolean>) msg.obj).accept(false /* is in correct state*/);
                    break;
                case EVENT_CANCEL_HANDOVER:
                    // We don't need to do anything in this case
                    if (DBG) {
                        log("DcDefaultState: EVENT_CANCEL_HANDOVER resultCode="
                                + DataServiceCallback.resultCodeToString(msg.arg1));
                    }
                    break;
                case EVENT_RELEASE_PDU_SESSION_ID: {
                    // We do the same thing in all state in order to preserve the existing workflow
                    final AsyncResult asyncResult = (AsyncResult) msg.obj;
                    if (asyncResult == null) {
                        loge("EVENT_RELEASE_PDU_SESSION_ID: asyncResult is null!");
                    } else {
                        if (msg.obj != null) {
                            if (DBG) logd("EVENT_RELEASE_PDU_SESSION_ID: id released");
                            Runnable runnable = (Runnable) asyncResult.userObj;
                            runnable.run();
                        } else {
                            loge("EVENT_RELEASE_PDU_SESSION_ID: no runnable set");
                        }
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_ALLOCATE_PDU_SESSION_ID: {
                    // We do the same thing in all state in order to preserve the existing workflow
                    final AsyncResult asyncResult = (AsyncResult) msg.obj;
                    if (asyncResult == null) {
                        loge("EVENT_ALLOCATE_PDU_SESSION_ID: asyncResult is null!");
                    } else {
                        Consumer<Integer> onAllocated = (Consumer<Integer>) asyncResult.userObj;
                        if (asyncResult.exception != null) {
                            loge("EVENT_ALLOCATE_PDU_SESSION_ID: exception",
                                    asyncResult.exception);
                            onAllocated.accept(PDU_SESSION_ID_NOT_SET);
                        } else if (asyncResult.result == null) {
                            loge("EVENT_ALLOCATE_PDU_SESSION_ID: result null, no id");
                            onAllocated.accept(PDU_SESSION_ID_NOT_SET);
                        } else {
                            int psi = (int) asyncResult.result;
                            if (DBG) logd("EVENT_ALLOCATE_PDU_SESSION_ID: psi=" + psi);
                            onAllocated.accept(psi);
                        }
                    }
                    retVal = HANDLED;
                    break;
                }
                default:
                    if (DBG) {
                        log("DcDefaultState: ignore msg.what=" + getWhatToString(msg.what));
                    }
                    break;
            }

            return retVal;
        }
    }

    private void updateSuspendState() {
        if (mNetworkAgent == null) {
            Rlog.d(getName(), "Setting suspend state without a NetworkAgent");
        }

        boolean newSuspendedState = false;
        // Data can only be (temporarily) suspended while data is in active state
        if (getCurrentState() == mActiveState) {
            // Never set suspended for emergency apn. Emergency data connection
            // can work while device is not in service.
            if (mApnSetting != null && mApnSetting.isEmergencyApn()) {
                newSuspendedState = false;
            // If we are not in service, change to suspended.
            } else if (mDataRegState != ServiceState.STATE_IN_SERVICE) {
                newSuspendedState = true;
            // Check voice/data concurrency.
            } else if (!mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                newSuspendedState = mPhone.getCallTracker().getState() != PhoneConstants.State.IDLE;
            }
        }

        // Only notify when there is a change.
        if (mIsSuspended != newSuspendedState) {
            mIsSuspended = newSuspendedState;

            // If data connection is active, we need to notify the new data connection state
            // changed event reflecting the latest suspended state.
            if (isActive()) {
                notifyDataConnectionState();
            }
        }
    }

    private void notifyDataConnectionState() {
        // The receivers of this have no way to differentiate between default and enterprise
        // connections. Do not notify for enterprise.
        if (!isEnterpriseUse()) {
            mPhone.notifyDataConnection(getPreciseDataConnectionState());
        } else {
            log("notifyDataConnectionState: Skipping for enterprise; state=" + getState());
        }
    }

    private DcDefaultState mDefaultState = new DcDefaultState();

    private int getApnTypeBitmask() {
        return isEnterpriseUse() ? ApnSetting.TYPE_ENTERPRISE :
                mApnSetting != null ? mApnSetting.getApnTypeBitmask() : 0;
    }

    private boolean canHandleDefault() {
        return !isEnterpriseUse() && mApnSetting != null
                ? mApnSetting.canHandleType(ApnSetting.TYPE_DEFAULT) : false;
    }

    /**
     * The state machine is inactive and expects a EVENT_CONNECT.
     */
    private class DcInactiveState extends State {
        // Inform all contexts we've failed connecting
        public void setEnterNotificationParams(ConnectionParams cp, @DataFailureCause int cause,
                @HandoverFailureMode int handoverFailureMode) {
            if (VDBG) log("DcInactiveState: setEnterNotificationParams cp,cause");
            mConnectionParams = cp;
            mDisconnectParams = null;
            mDcFailCause = cause;
            mHandoverFailureMode = handoverFailureMode;
        }

        // Inform all contexts we've failed disconnected
        public void setEnterNotificationParams(DisconnectParams dp) {
            if (VDBG) log("DcInactiveState: setEnterNotificationParams dp");
            mConnectionParams = null;
            mDisconnectParams = dp;
            mDcFailCause = DataFailCause.NONE;
        }

        // Inform all contexts of the failure cause
        public void setEnterNotificationParams(@DataFailureCause int cause) {
            mConnectionParams = null;
            mDisconnectParams = null;
            mDcFailCause = cause;
        }

        @Override
        public void enter() {
            mTag += 1;
            if (DBG) log("DcInactiveState: enter() mTag=" + mTag);
            TelephonyStatsLog.write(TelephonyStatsLog.MOBILE_CONNECTION_STATE_CHANGED,
                    TelephonyStatsLog.MOBILE_CONNECTION_STATE_CHANGED__STATE__INACTIVE,
                    mPhone.getPhoneId(), mId, getApnTypeBitmask(), canHandleDefault());
            mDataCallSessionStats.onDataCallDisconnected(mDcFailCause);
            if (mHandoverState == HANDOVER_STATE_BEING_TRANSFERRED) {
                // This is from source data connection to set itself's state
                setHandoverState(HANDOVER_STATE_COMPLETED);
            }

            // Check for dangling agent. Ideally the handover source agent should be null if
            // handover process is smooth. When it's not null, that means handover failed. The
            // agent was not successfully transferred to the new data connection. We should
            // gracefully notify connectivity service the network was disconnected.
            if (mHandoverSourceNetworkAgent != null) {
                DataConnection sourceDc = mHandoverSourceNetworkAgent.getDataConnection();
                if (sourceDc != null) {
                    // If the source data connection still owns this agent, then just reset the
                    // handover state back to idle because handover is already failed.
                    mHandoverLocalLog.log(
                            "Handover failed. Reset the source dc " + sourceDc.getName()
                                    + " state to idle");
                    sourceDc.cancelHandover();
                } else {
                    // The agent is now a dangling agent. No data connection owns this agent.
                    // Gracefully notify connectivity service disconnected.
                    mHandoverLocalLog.log(
                            "Handover failed and dangling agent found.");
                    mHandoverSourceNetworkAgent.acquireOwnership(
                            DataConnection.this, mTransportType);
                    log("Cleared dangling network agent. " + mHandoverSourceNetworkAgent);
                    mHandoverSourceNetworkAgent.unregister(DataConnection.this);
                    mHandoverSourceNetworkAgent.releaseOwnership(DataConnection.this);
                }
                mHandoverSourceNetworkAgent = null;
            }

            if (mConnectionParams != null) {
                if (DBG) {
                    log("DcInactiveState: enter notifyConnectCompleted +ALL failCause="
                            + DataFailCause.toString(mDcFailCause));
                }
                notifyConnectCompleted(mConnectionParams, mDcFailCause, mHandoverFailureMode,
                        true);
            }
            if (mDisconnectParams != null) {
                if (DBG) {
                    log("DcInactiveState: enter notifyDisconnectCompleted +ALL failCause="
                            + DataFailCause.toString(mDcFailCause));
                }
                notifyDisconnectCompleted(mDisconnectParams, true);
            }
            if (mDisconnectParams == null && mConnectionParams == null
                    && mDcFailCause != DataFailCause.NONE) {
                if (DBG) {
                    log("DcInactiveState: enter notifyAllDisconnectCompleted failCause="
                            + DataFailCause.toString(mDcFailCause));
                }
                notifyAllWithEvent(null, DctConstants.EVENT_DISCONNECT_DONE,
                        DataFailCause.toString(mDcFailCause));
            }

            // Remove ourselves from cid mapping, before clearSettings
            mDcController.removeActiveDcByCid(DataConnection.this);

            // For the first time entering here (idle state before setup), do not notify
            // disconnected state. Only notify data connection disconnected for data that is
            // actually moving from disconnecting to disconnected, or setup failed. In both cases,
            // APN setting will not be null.
            if (mApnSetting != null) {
                notifyDataConnectionState();
            }
            clearSettings();
        }

        @Override
        public void exit() {
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESET:
                case EVENT_REEVALUATE_RESTRICTED_STATE:
                    if (DBG) {
                        log("DcInactiveState: msg.what=" + getWhatToString(msg.what)
                                + ", ignore we're already done");
                    }
                    return HANDLED;
                case EVENT_CONNECT:
                    if (DBG) log("DcInactiveState: mag.what=EVENT_CONNECT");
                    ConnectionParams cp = (ConnectionParams) msg.obj;

                    if (!initConnection(cp)) {
                        log("DcInactiveState: msg.what=EVENT_CONNECT initConnection failed");
                        notifyConnectCompleted(cp, DataFailCause.UNACCEPTABLE_NETWORK_PARAMETER,
                                DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN, false);
                        transitionTo(mInactiveState);
                        return HANDLED;
                    }

                    int cause = connect(cp);
                    if (cause != DataFailCause.NONE) {
                        log("DcInactiveState: msg.what=EVENT_CONNECT connect failed");
                        notifyConnectCompleted(cp, cause,
                                DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN, false);
                        transitionTo(mInactiveState);
                        return HANDLED;
                    }

                    if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        mSubId = cp.mSubId;
                    }

                    transitionTo(mActivatingState);
                    return HANDLED;
                case EVENT_DISCONNECT:
                    if (DBG) log("DcInactiveState: msg.what=EVENT_DISCONNECT");
                    notifyDisconnectCompleted((DisconnectParams)msg.obj, false);
                    return HANDLED;
                case EVENT_DISCONNECT_ALL:
                    if (DBG) log("DcInactiveState: msg.what=EVENT_DISCONNECT_ALL");
                    notifyDisconnectCompleted((DisconnectParams)msg.obj, false);
                    return HANDLED;
                default:
                    if (VDBG) {
                        log("DcInactiveState not handled msg.what=" + getWhatToString(msg.what));
                    }
                    return NOT_HANDLED;
            }
        }
    }
    private DcInactiveState mInactiveState = new DcInactiveState();

    /**
     * The state machine is activating a connection.
     */
    private class DcActivatingState extends State {
        @Override
        public void enter() {
            int apnTypeBitmask = getApnTypeBitmask();
            TelephonyStatsLog.write(TelephonyStatsLog.MOBILE_CONNECTION_STATE_CHANGED,
                    TelephonyStatsLog.MOBILE_CONNECTION_STATE_CHANGED__STATE__ACTIVATING,
                    mPhone.getPhoneId(), mId, apnTypeBitmask, canHandleDefault());
            setHandoverState(HANDOVER_STATE_IDLE);
            // restricted evaluation depends on network requests from apnContext. The evaluation
            // should happen once entering connecting state rather than active state because it's
            // possible that restricted network request can be released during the connecting window
            // and if we wait for connection established, then we might mistakenly
            // consider it as un-restricted. ConnectivityService then will immediately
            // tear down the connection through networkAgent unwanted callback if all requests for
            // this connection are going away.
            mRestrictedNetworkOverride = shouldRestrictNetwork();

            CarrierPrivilegesTracker carrierPrivTracker = mPhone.getCarrierPrivilegesTracker();
            if (carrierPrivTracker != null) {
                carrierPrivTracker.registerCarrierPrivilegesListener(
                            getHandler(), EVENT_CARRIER_PRIVILEGED_UIDS_CHANGED, null);
            }
            notifyDataConnectionState();
            mDataCallSessionStats.onSetupDataCall(apnTypeBitmask);
        }
        @Override
        public boolean processMessage(Message msg) {
            boolean retVal;
            AsyncResult ar;
            ConnectionParams cp;

            if (DBG) log("DcActivatingState: msg=" + msgToString(msg));
            switch (msg.what) {
                case EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED:
                case EVENT_CONNECT:
                    // Activating can't process until we're done.
                    deferMessage(msg);
                    retVal = HANDLED;
                    break;

                case EVENT_SETUP_DATA_CONNECTION_DONE:
                    cp = (ConnectionParams) msg.obj;

                    DataCallResponse dataCallResponse =
                            msg.getData().getParcelable(DataServiceManager.DATA_CALL_RESPONSE);
                    SetupResult result = onSetupConnectionCompleted(msg.arg1, dataCallResponse, cp);
                    if (result != SetupResult.ERROR_STALE) {
                        if (mConnectionParams != cp) {
                            loge("DcActivatingState: WEIRD mConnectionsParams:"+ mConnectionParams
                                    + " != cp:" + cp);
                        }
                    }
                    if (DBG) {
                        log("DcActivatingState onSetupConnectionCompleted result=" + result
                                + " dc=" + DataConnection.this);
                    }
                    ApnContext.requestLog(
                            cp.mApnContext, "onSetupConnectionCompleted result=" + result);
                    switch (result) {
                        case SUCCESS:
                            // All is well
                            mDcFailCause = DataFailCause.NONE;
                            transitionTo(mActiveState);
                            break;
                        case ERROR_RADIO_NOT_AVAILABLE:
                            // Vendor ril rejected the command and didn't connect.
                            // Transition to inactive but send notifications after
                            // we've entered the mInactive state.
                            mInactiveState.setEnterNotificationParams(cp, result.mFailCause,
                                    DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN);
                            transitionTo(mInactiveState);
                            break;
                        case ERROR_DUPLICATE_CID:
                            // TODO (b/180988471): Properly handle the case when an existing cid is
                            // returned by tearing down the network agent if enterprise changed.
                            long retry = RetryManager.NO_SUGGESTED_RETRY_DELAY;
                            if (cp.mApnContext != null) {
                                retry = RetryManager.NO_RETRY;
                                mDct.getDataThrottler().setRetryTime(
                                        cp.mApnContext.getApnTypeBitmask(),
                                        retry, DcTracker.REQUEST_TYPE_NORMAL);
                            }
                            String logStr = "DcActivatingState: "
                                    + DataFailCause.toString(result.mFailCause)
                                    + " retry=" + retry;
                            if (DBG) log(logStr);
                            ApnContext.requestLog(cp.mApnContext, logStr);
                            mInactiveState.setEnterNotificationParams(cp, result.mFailCause,
                                    DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN);
                            transitionTo(mInactiveState);
                            break;
                        case ERROR_NO_DEFAULT_CONNECTION:
                            // TODO (b/180988471): Properly handle the case when a default data
                            // connection doesn't exist (tear down connection and retry).
                            // Currently, this just tears down the connection without retry.
                            if (DBG) log("DcActivatingState: NO_DEFAULT_DATA");
                        case ERROR_INVALID_ARG:
                            // The addresses given from the RIL are bad
                            tearDownData(cp);
                            transitionTo(mDisconnectingErrorCreatingConnection);
                            break;
                        case ERROR_DATA_SERVICE_SPECIFIC_ERROR:

                            // Retrieve the suggested retry delay from the modem and save it.
                            // If the modem want us to retry the current APN again, it will
                            // suggest a positive delay value (in milliseconds). Otherwise we'll get
                            // NO_SUGGESTED_RETRY_DELAY here.

                            long delay = getSuggestedRetryDelay(dataCallResponse);
                            long retryTime = RetryManager.NO_SUGGESTED_RETRY_DELAY;
                            if (delay == RetryManager.NO_RETRY) {
                                retryTime = RetryManager.NO_RETRY;
                            } else if (delay >= 0) {
                                retryTime = SystemClock.elapsedRealtime() + delay;
                            }
                            int newRequestType = DcTracker.calculateNewRetryRequestType(
                                    mHandoverFailureMode, cp.mRequestType, mDcFailCause);
                            mDct.getDataThrottler().setRetryTime(getApnTypeBitmask(),
                                    retryTime, newRequestType);

                            String str = "DcActivatingState: ERROR_DATA_SERVICE_SPECIFIC_ERROR "
                                    + " delay=" + delay
                                    + " result=" + result
                                    + " result.isRadioRestartFailure="
                                    + DataFailCause.isRadioRestartFailure(mPhone.getContext(),
                                    result.mFailCause, mPhone.getSubId())
                                    + " isPermanentFailure=" +
                                    mDct.isPermanentFailure(result.mFailCause);
                            if (DBG) log(str);
                            ApnContext.requestLog(cp.mApnContext, str);

                            // Save the cause. DcTracker.onDataSetupComplete will check this
                            // failure cause and determine if we need to retry this APN later
                            // or not.
                            mInactiveState.setEnterNotificationParams(cp, result.mFailCause,
                                    dataCallResponse != null
                                            ? dataCallResponse.getHandoverFailureMode()
                                            : DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN);
                            transitionTo(mInactiveState);
                            break;
                        case ERROR_STALE:
                            loge("DcActivatingState: stale EVENT_SETUP_DATA_CONNECTION_DONE"
                                    + " tag:" + cp.mTag + " != mTag:" + mTag);
                            break;
                        default:
                            throw new RuntimeException("Unknown SetupResult, should not happen");
                    }
                    retVal = HANDLED;
                    mDataCallSessionStats
                            .onSetupDataCallResponse(dataCallResponse,
                                    ServiceState.rilRadioTechnologyToNetworkType(cp.mRilRat),
                                    getApnTypeBitmask(), mApnSetting.getProtocol(),
                                    result.mFailCause);
                    break;
                case EVENT_CARRIER_PRIVILEGED_UIDS_CHANGED:
                    AsyncResult asyncResult = (AsyncResult) msg.obj;
                    int[] administratorUids = (int[]) asyncResult.result;
                    mAdministratorUids = Arrays.copyOf(administratorUids, administratorUids.length);
                    retVal = HANDLED;
                    break;
                case EVENT_START_HANDOVER_ON_TARGET:
                    //called after startHandover on target transport
                    ((Consumer<Boolean>) msg.obj).accept(true /* is in correct state*/);
                    retVal = HANDLED;
                    break;
                case EVENT_CANCEL_HANDOVER:
                    transitionTo(mInactiveState);
                    retVal = HANDLED;
                    break;
                default:
                    if (VDBG) {
                        log("DcActivatingState not handled msg.what=" +
                                getWhatToString(msg.what) + " RefCount=" + mApnContexts.size());
                    }
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }
    }
    private DcActivatingState mActivatingState = new DcActivatingState();

    /**
     * The state machine is connected, expecting an EVENT_DISCONNECT.
     */
    private class DcActiveState extends State {

        @Override public void enter() {
            if (DBG) log("DcActiveState: enter dc=" + DataConnection.this);
            TelephonyStatsLog.write(TelephonyStatsLog.MOBILE_CONNECTION_STATE_CHANGED,
                    TelephonyStatsLog.MOBILE_CONNECTION_STATE_CHANGED__STATE__ACTIVE,
                    mPhone.getPhoneId(), mId, getApnTypeBitmask(), canHandleDefault());
            // If we were retrying there maybe more than one, otherwise they'll only be one.
            notifyAllWithEvent(null, DctConstants.EVENT_DATA_SETUP_COMPLETE,
                    Phone.REASON_CONNECTED);

            mPhone.getCallTracker().registerForVoiceCallStarted(getHandler(),
                    DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_STARTED, null);
            mPhone.getCallTracker().registerForVoiceCallEnded(getHandler(),
                    DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_ENDED, null);

            // If the EVENT_CONNECT set the current max retry restore it here
            // if it didn't then this is effectively a NOP.
            mDcController.addActiveDcByCid(DataConnection.this);

            updateTcpBufferSizes(mRilRat);
            updateLinkBandwidthsFromCarrierConfig(mRilRat);

            final NetworkAgentConfig.Builder configBuilder = new NetworkAgentConfig.Builder();
            configBuilder.setLegacyType(ConnectivityManager.TYPE_MOBILE);
            configBuilder.setLegacyTypeName(NETWORK_TYPE);
            int networkType = getNetworkType();
            configBuilder.setLegacySubType(networkType);
            configBuilder.setLegacySubTypeName(TelephonyManager.getNetworkTypeName(networkType));
            configBuilder.setLegacyExtraInfo(mApnSetting.getApnName());
            final CarrierSignalAgent carrierSignalAgent = mPhone.getCarrierSignalAgent();
            if (carrierSignalAgent.hasRegisteredReceivers(TelephonyManager
                    .ACTION_CARRIER_SIGNAL_REDIRECTED)) {
                // carrierSignal Receivers will place the carrier-specific provisioning notification
                configBuilder.setProvisioningNotificationEnabled(false);
            }

            final String subscriberId = mPhone.getSubscriberId();
            if (!TextUtils.isEmpty(subscriberId)) {
                configBuilder.setSubscriberId(subscriberId);
            }

            // set skip464xlat if it is not default otherwise
            if (shouldSkip464Xlat()) {
                configBuilder.setNat64DetectionEnabled(false);
            }

            mUnmeteredUseOnly = isUnmeteredUseOnly();
            mMmsUseOnly = isMmsUseOnly();
            mEnterpriseUse = isEnterpriseUse();

            if (DBG) {
                log("mRestrictedNetworkOverride = " + mRestrictedNetworkOverride
                        + ", mUnmeteredUseOnly = " + mUnmeteredUseOnly
                        + ", mMmsUseOnly = " + mMmsUseOnly
                        + ", mEnterpriseUse = " + mEnterpriseUse);
            }

            // Always register a VcnNetworkPolicyChangeListener, regardless of whether this is a
            // handover
            // or new Network.
            mVcnManager.addVcnNetworkPolicyChangeListener(
                    new HandlerExecutor(getHandler()), mVcnPolicyChangeListener);

            if (mConnectionParams != null
                    && mConnectionParams.mRequestType == REQUEST_TYPE_HANDOVER) {
                // If this is a data setup for handover, we need to reuse the existing network agent
                // instead of creating a new one. This should be transparent to connectivity
                // service.
                DcTracker dcTracker = mPhone.getDcTracker(getHandoverSourceTransport());
                DataConnection dc = dcTracker.getDataConnectionByApnType(
                        mConnectionParams.mApnContext.getApnType());
                // It's possible that the source data connection has been disconnected by the modem
                // already. If not, set its handover state to completed.
                if (dc != null) {
                    // Transfer network agent from the original data connection as soon as the
                    // new handover data connection is connected.
                    dc.setHandoverState(HANDOVER_STATE_COMPLETED);
                }

                if (mHandoverSourceNetworkAgent != null) {
                    String logStr = "Transfer network agent " + mHandoverSourceNetworkAgent.getTag()
                            + " successfully.";
                    log(logStr);
                    mHandoverLocalLog.log(logStr);
                    mNetworkAgent = mHandoverSourceNetworkAgent;
                    mNetworkAgent.acquireOwnership(DataConnection.this, mTransportType);

                    // TODO: Should evaluate mDisabledApnTypeBitMask again after handover. We don't
                    // do it now because connectivity service does not support dynamically removing
                    // immutable capabilities.

                    mNetworkAgent.updateLegacySubtype(DataConnection.this);
                    // Update the capability after handover
                    mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                            DataConnection.this);
                    mNetworkAgent.sendLinkProperties(mLinkProperties, DataConnection.this);
                    mHandoverSourceNetworkAgent = null;
                } else {
                    String logStr = "Failed to get network agent from original data connection";
                    loge(logStr);
                    mHandoverLocalLog.log(logStr);
                    return;
                }
            } else {
                mScore = calculateScore();
                final NetworkFactory factory = PhoneFactory.getNetworkFactory(
                        mPhone.getPhoneId());
                final NetworkProvider provider = (null == factory) ? null : factory.getProvider();

                mDisabledApnTypeBitMask |= getDisallowedApnTypes();
                updateLinkPropertiesHttpProxy();
                mNetworkAgent = new DcNetworkAgent(DataConnection.this, mPhone, mScore,
                        configBuilder.build(), provider, mTransportType);

                VcnNetworkPolicyResult policyResult =
                        mVcnManager.applyVcnNetworkPolicy(
                                getNetworkCapabilities(), getLinkProperties());
                if (policyResult.isTeardownRequested()) {
                    tearDownAll(
                            Phone.REASON_VCN_REQUESTED_TEARDOWN,
                            DcTracker.RELEASE_TYPE_DETACH,
                            null /* onCompletedMsg */);
                } else {
                    // All network agents start out in CONNECTING mode, but DcNetworkAgents are
                    // created when the network is already connected. Hence, send the connected
                    // notification immediately.
                    mNetworkAgent.markConnected();
                }

                // The network agent is always created with NOT_SUSPENDED capability, but the
                // network might be already out of service (or voice call is ongoing) just right
                // before data connection is created. Connectivity service would not allow a network
                // created with suspended state, so we create a non-suspended network first, and
                // then immediately evaluate the suspended state.
                sendMessage(obtainMessage(EVENT_UPDATE_SUSPENDED_STATE));
            }

            // The qos parameters are set when the call is connected
            syncQosToNetworkAgent();

            if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                mPhone.mCi.registerForNattKeepaliveStatus(
                        getHandler(), DataConnection.EVENT_KEEPALIVE_STATUS, null);
                mPhone.mCi.registerForLceInfo(
                        getHandler(), DataConnection.EVENT_LINK_CAPACITY_CHANGED, null);
            }
            notifyDataConnectionState();
            TelephonyMetrics.getInstance().writeRilDataCallEvent(mPhone.getPhoneId(),
                    mCid, getApnTypeBitmask(), RilDataCall.State.CONNECTED);
        }

        @Override
        public void exit() {
            if (DBG) log("DcActiveState: exit dc=" + this);
            mPhone.getCallTracker().unregisterForVoiceCallStarted(getHandler());
            mPhone.getCallTracker().unregisterForVoiceCallEnded(getHandler());

            if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                mPhone.mCi.unregisterForNattKeepaliveStatus(getHandler());
                mPhone.mCi.unregisterForLceInfo(getHandler());
            }

            // If we are still owning this agent, then we should inform connectivity service the
            // data connection is disconnected. There is one exception that we shouldn't unregister,
            // which is when IWLAN handover is ongoing. Instead of unregistering, the agent will
            // be transferred to the new data connection on the other transport.
            if (mNetworkAgent != null) {
                syncQosToNetworkAgent();
                if (mHandoverState == HANDOVER_STATE_IDLE) {
                    mNetworkAgent.unregister(DataConnection.this);
                }
                mNetworkAgent.releaseOwnership(DataConnection.this);
            }
            mNetworkAgent = null;

            TelephonyMetrics.getInstance().writeRilDataCallEvent(mPhone.getPhoneId(),
                    mCid, getApnTypeBitmask(), RilDataCall.State.DISCONNECTED);

            mVcnManager.removeVcnNetworkPolicyChangeListener(mVcnPolicyChangeListener);

            CarrierPrivilegesTracker carrierPrivTracker = mPhone.getCarrierPrivilegesTracker();
            if (carrierPrivTracker != null) {
                carrierPrivTracker.unregisterCarrierPrivilegesListener(getHandler());
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            boolean retVal;

            switch (msg.what) {
                case EVENT_CONNECT: {
                    ConnectionParams cp = (ConnectionParams) msg.obj;
                    // either add this new apn context to our set or
                    // update the existing cp with the latest connection generation number
                    mApnContexts.put(cp.mApnContext, cp);
                    // TODO (b/118347948): evaluate if it's still needed after assigning
                    // different scores to different Cellular network.
                    mDisabledApnTypeBitMask &= ~cp.mApnContext.getApnTypeBitmask();
                    mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                            DataConnection.this);
                    if (DBG) {
                        log("DcActiveState: EVENT_CONNECT cp=" + cp + " dc=" + DataConnection.this);
                    }
                    notifyConnectCompleted(cp, DataFailCause.NONE,
                            DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN, false);
                    retVal = HANDLED;
                    break;
                }
                case EVENT_DISCONNECT: {
                    DisconnectParams dp = (DisconnectParams) msg.obj;
                    if (DBG) {
                        log("DcActiveState: EVENT_DISCONNECT dp=" + dp
                                + " dc=" + DataConnection.this);
                    }
                    if (mApnContexts.containsKey(dp.mApnContext)) {
                        if (DBG) {
                            log("DcActiveState msg.what=EVENT_DISCONNECT RefCount="
                                    + mApnContexts.size());
                        }

                        if (mApnContexts.size() == 1) {
                            mApnContexts.clear();
                            mDisconnectParams = dp;
                            mConnectionParams = null;
                            dp.mTag = mTag;
                            tearDownData(dp);
                            transitionTo(mDisconnectingState);
                        } else {
                            mApnContexts.remove(dp.mApnContext);
                            // TODO (b/118347948): evaluate if it's still needed after assigning
                            // different scores to different Cellular network.
                            mDisabledApnTypeBitMask |= dp.mApnContext.getApnTypeBitmask();
                            mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                                    DataConnection.this);
                            notifyDisconnectCompleted(dp, false);
                        }
                    } else {
                        log("DcActiveState ERROR no such apnContext=" + dp.mApnContext
                                + " in this dc=" + DataConnection.this);
                        notifyDisconnectCompleted(dp, false);
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_DISCONNECT_ALL: {
                    if (DBG) {
                        log("DcActiveState EVENT_DISCONNECT clearing apn contexts,"
                                + " dc=" + DataConnection.this);
                    }
                    DisconnectParams dp = (DisconnectParams) msg.obj;
                    mDisconnectParams = dp;
                    mConnectionParams = null;
                    dp.mTag = mTag;
                    tearDownData(dp);
                    transitionTo(mDisconnectingState);
                    retVal = HANDLED;
                    break;
                }
                case EVENT_LOST_CONNECTION: {
                    if (DBG) {
                        log("DcActiveState EVENT_LOST_CONNECTION dc=" + DataConnection.this);
                    }

                    mInactiveState.setEnterNotificationParams(DataFailCause.LOST_CONNECTION);
                    transitionTo(mInactiveState);
                    retVal = HANDLED;
                    break;
                }
                case EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Pair<Integer, Integer> drsRatPair = (Pair<Integer, Integer>) ar.result;
                    mDataRegState = drsRatPair.first;
                    updateTcpBufferSizes(drsRatPair.second);
                    if (isBandwidthSourceKey(DctConstants.BANDWIDTH_SOURCE_CARRIER_CONFIG_KEY)) {
                        updateLinkBandwidthsFromCarrierConfig(drsRatPair.second);
                    }
                    mRilRat = drsRatPair.second;
                    if (DBG) {
                        log("DcActiveState: EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED"
                                + " drs=" + mDataRegState
                                + " mRilRat=" + mRilRat);
                    }
                    updateSuspendState();
                    if (mNetworkAgent != null) {
                        mNetworkAgent.updateLegacySubtype(DataConnection.this);
                        // The new suspended state will be passed through connectivity service
                        // through NET_CAPABILITY_NOT_SUSPENDED.
                        mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                                DataConnection.this);
                        mNetworkAgent.sendLinkProperties(mLinkProperties, DataConnection.this);
                    }
                    retVal = HANDLED;
                    mDataCallSessionStats.onDrsOrRatChanged(
                            ServiceState.rilRadioTechnologyToNetworkType(mRilRat));
                    break;
                }
                case EVENT_NR_FREQUENCY_CHANGED:
                    // fallthrough
                case EVENT_CARRIER_CONFIG_LINK_BANDWIDTHS_CHANGED:
                    if (isBandwidthSourceKey(DctConstants.BANDWIDTH_SOURCE_CARRIER_CONFIG_KEY)) {
                        updateLinkBandwidthsFromCarrierConfig(mRilRat);
                    }
                    if (mNetworkAgent != null) {
                        mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                                DataConnection.this);
                    }
                    retVal = HANDLED;
                    break;
                case EVENT_DATA_CONNECTION_METEREDNESS_CHANGED:
                    boolean isUnmetered = (boolean) msg.obj;
                    if (isUnmetered == mUnmeteredOverride) {
                        retVal = HANDLED;
                        break;
                    }
                    mUnmeteredOverride = isUnmetered;
                    if (mNetworkAgent != null) {
                        mNetworkAgent.updateLegacySubtype(DataConnection.this);
                        mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                                DataConnection.this);
                    }
                    retVal = HANDLED;
                    break;
                case EVENT_DATA_CONNECTION_CONGESTEDNESS_CHANGED:
                    boolean isCongested = (boolean) msg.obj;
                    if (isCongested == mCongestedOverride) {
                        retVal = HANDLED;
                        break;
                    }
                    mCongestedOverride = isCongested;
                    if (mNetworkAgent != null) {
                        mNetworkAgent.updateLegacySubtype(DataConnection.this);
                        mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                                DataConnection.this);
                    }
                    retVal = HANDLED;
                    break;
                case EVENT_DATA_CONNECTION_ROAM_ON:
                case EVENT_DATA_CONNECTION_ROAM_OFF: {
                    if (mNetworkAgent != null) {
                        mNetworkAgent.updateLegacySubtype(DataConnection.this);
                        mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                                DataConnection.this);
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_DATA_CONNECTION_VOICE_CALL_STARTED:
                case EVENT_DATA_CONNECTION_VOICE_CALL_ENDED:
                case EVENT_CSS_INDICATOR_CHANGED:
                case EVENT_UPDATE_SUSPENDED_STATE: {
                    updateSuspendState();
                    if (mNetworkAgent != null) {
                        mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                                DataConnection.this);
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_BW_REFRESH_RESPONSE: {
                    AsyncResult ar = (AsyncResult)msg.obj;
                    if (ar.exception != null) {
                        log("EVENT_BW_REFRESH_RESPONSE: error ignoring, e=" + ar.exception);
                    } else {
                        if (isBandwidthSourceKey(DctConstants.BANDWIDTH_SOURCE_MODEM_KEY)) {
                            updateLinkBandwidthsFromModem((List<LinkCapacityEstimate>) ar.result);
                        }
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_KEEPALIVE_START_REQUEST: {
                    KeepalivePacketData pkt = (KeepalivePacketData) msg.obj;
                    int slotId = msg.arg1;
                    int intervalMillis = msg.arg2 * 1000;
                    if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                        mPhone.mCi.startNattKeepalive(
                                DataConnection.this.mCid, pkt, intervalMillis,
                                DataConnection.this.obtainMessage(
                                        EVENT_KEEPALIVE_STARTED, slotId, 0, null));
                    } else {
                        // We currently do not support NATT Keepalive requests using the
                        // DataService API, so unless the request is WWAN (always bound via
                        // the CommandsInterface), the request cannot be honored.
                        //
                        // TODO: b/72331356 to add support for Keepalive to the DataService
                        // so that keepalive requests can be handled (if supported) by the
                        // underlying transport.
                        if (mNetworkAgent != null) {
                            mNetworkAgent.sendSocketKeepaliveEvent(
                                    msg.arg1, SocketKeepalive.ERROR_INVALID_NETWORK);
                        }
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_KEEPALIVE_STOP_REQUEST: {
                    int slotId = msg.arg1;
                    int handle = mNetworkAgent.keepaliveTracker.getHandleForSlot(slotId);
                    if (handle < 0) {
                        loge("No slot found for stopSocketKeepalive! " + slotId);
                        mNetworkAgent.sendSocketKeepaliveEvent(
                                slotId, SocketKeepalive.ERROR_NO_SUCH_SLOT);
                        retVal = HANDLED;
                        break;
                    } else {
                        logd("Stopping keepalive with handle: " + handle);
                    }

                    mPhone.mCi.stopNattKeepalive(
                            handle, DataConnection.this.obtainMessage(
                                    EVENT_KEEPALIVE_STOPPED, handle, slotId, null));
                    retVal = HANDLED;
                    break;
                }
                case EVENT_KEEPALIVE_STARTED: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    final int slot = msg.arg1;
                    if (ar.exception != null || ar.result == null) {
                        loge("EVENT_KEEPALIVE_STARTED: error starting keepalive, e="
                                + ar.exception);
                        mNetworkAgent.sendSocketKeepaliveEvent(
                                slot, SocketKeepalive.ERROR_HARDWARE_ERROR);
                    } else {
                        KeepaliveStatus ks = (KeepaliveStatus) ar.result;
                        if (ks == null) {
                            loge("Null KeepaliveStatus received!");
                        } else {
                            mNetworkAgent.keepaliveTracker.handleKeepaliveStarted(slot, ks);
                        }
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_KEEPALIVE_STATUS: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        loge("EVENT_KEEPALIVE_STATUS: error in keepalive, e=" + ar.exception);
                        // We have no way to notify connectivity in this case.
                    }
                    if (ar.result != null) {
                        KeepaliveStatus ks = (KeepaliveStatus) ar.result;
                        mNetworkAgent.keepaliveTracker.handleKeepaliveStatus(ks);
                    }

                    retVal = HANDLED;
                    break;
                }
                case EVENT_KEEPALIVE_STOPPED: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    final int handle = msg.arg1;
                    final int slotId = msg.arg2;

                    if (ar.exception != null) {
                        loge("EVENT_KEEPALIVE_STOPPED: error stopping keepalive for handle="
                                + handle + " e=" + ar.exception);
                        mNetworkAgent.keepaliveTracker.handleKeepaliveStatus(
                                new KeepaliveStatus(KeepaliveStatus.ERROR_UNKNOWN));
                    } else {
                        log("Keepalive Stop Requested for handle=" + handle);
                        mNetworkAgent.keepaliveTracker.handleKeepaliveStatus(
                                new KeepaliveStatus(
                                        handle, KeepaliveStatus.STATUS_INACTIVE));
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_LINK_CAPACITY_CHANGED: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        loge("EVENT_LINK_CAPACITY_CHANGED e=" + ar.exception);
                    } else {
                        if (isBandwidthSourceKey(DctConstants.BANDWIDTH_SOURCE_MODEM_KEY)) {
                            updateLinkBandwidthsFromModem((List<LinkCapacityEstimate>) ar.result);
                        }
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_LINK_BANDWIDTH_ESTIMATOR_UPDATE: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        loge("EVENT_LINK_BANDWIDTH_ESTIMATOR_UPDATE e=" + ar.exception);
                    } else {
                        Pair<Integer, Integer> pair = (Pair<Integer, Integer>) ar.result;
                        if (isBandwidthSourceKey(
                                DctConstants.BANDWIDTH_SOURCE_BANDWIDTH_ESTIMATOR_KEY)) {
                            updateLinkBandwidthsFromBandwidthEstimator(pair.first, pair.second);
                        }
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_REEVALUATE_RESTRICTED_STATE: {
                    // If the network was restricted, and now it does not need to be restricted
                    // anymore, we should add the NET_CAPABILITY_NOT_RESTRICTED capability.
                    if (mRestrictedNetworkOverride && !shouldRestrictNetwork()) {
                        if (DBG) {
                            log("Data connection becomes not-restricted. dc=" + this);
                        }
                        // Note we only do this when network becomes non-restricted. When a
                        // non-restricted becomes restricted (e.g. users disable data, or turn off
                        // data roaming), DCT will explicitly tear down the networks (because
                        // connectivity service does not support force-close TCP connections today).
                        // Also note that NET_CAPABILITY_NOT_RESTRICTED is an immutable capability
                        // (see {@link NetworkCapabilities}) once we add it to the network, we can't
                        // remove it through the entire life cycle of the connection.
                        mRestrictedNetworkOverride = false;
                        mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                                DataConnection.this);
                    }

                    // If the data does need to be unmetered use only (e.g. users turn on data, or
                    // device is not roaming anymore assuming data roaming is off), then we can
                    // dynamically add those metered APN type capabilities back. (But not the
                    // other way around because most of the APN-type capabilities are immutable
                    // capabilities.)
                    if (mUnmeteredUseOnly && !isUnmeteredUseOnly()) {
                        mUnmeteredUseOnly = false;
                        mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                                DataConnection.this);
                    }

                    mMmsUseOnly = isMmsUseOnly();

                    retVal = HANDLED;
                    break;
                }
                case EVENT_REEVALUATE_DATA_CONNECTION_PROPERTIES: {
                    // Update other properties like link properties if needed in future.
                    updateScore();
                    retVal = HANDLED;
                    break;
                }
                case EVENT_NR_STATE_CHANGED: {
                    updateTcpBufferSizes(mRilRat);
                    if (isBandwidthSourceKey(DctConstants.BANDWIDTH_SOURCE_CARRIER_CONFIG_KEY)) {
                        updateLinkBandwidthsFromCarrierConfig(mRilRat);
                    }
                    if (mNetworkAgent != null) {
                        mNetworkAgent.sendLinkProperties(mLinkProperties, DataConnection.this);
                        mNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities(),
                                DataConnection.this);
                    }
                    retVal = HANDLED;
                    break;
                }
                case EVENT_CARRIER_PRIVILEGED_UIDS_CHANGED:
                    AsyncResult asyncResult = (AsyncResult) msg.obj;
                    int[] administratorUids = (int[]) asyncResult.result;
                    mAdministratorUids = Arrays.copyOf(administratorUids, administratorUids.length);

                    // Administrator UIDs changed, so update NetworkAgent with new
                    // NetworkCapabilities
                    if (mNetworkAgent != null) {
                        mNetworkAgent.sendNetworkCapabilities(
                                getNetworkCapabilities(), DataConnection.this);
                    }
                    retVal = HANDLED;
                    break;
                case EVENT_START_HANDOVER:  //calls startHandover()
                    Consumer<Integer> r = (Consumer<Integer>) msg.obj;
                    r.accept(msg.arg1);
                    retVal = HANDLED;
                    break;

                default:
                    if (VDBG) {
                        log("DcActiveState not handled msg.what=" + getWhatToString(msg.what));
                    }
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }
    }
    private DcActiveState mActiveState = new DcActiveState();

    /**
     * The state machine is disconnecting.
     */
    private class DcDisconnectingState extends State {
        @Override
        public void enter() {
            TelephonyStatsLog.write(TelephonyStatsLog.MOBILE_CONNECTION_STATE_CHANGED,
                    TelephonyStatsLog.MOBILE_CONNECTION_STATE_CHANGED__STATE__DISCONNECTING,
                    mPhone.getPhoneId(), mId, getApnTypeBitmask(), canHandleDefault());
            notifyDataConnectionState();
        }
        @Override
        public boolean processMessage(Message msg) {
            boolean retVal;

            switch (msg.what) {
                case EVENT_CONNECT:
                    if (DBG) log("DcDisconnectingState msg.what=EVENT_CONNECT. Defer. RefCount = "
                            + mApnContexts.size());
                    deferMessage(msg);
                    retVal = HANDLED;
                    break;

                case EVENT_DEACTIVATE_DONE:
                    DisconnectParams dp = (DisconnectParams) msg.obj;

                    String str = "DcDisconnectingState msg.what=EVENT_DEACTIVATE_DONE RefCount="
                            + mApnContexts.size();

                    if (DBG) log(str);
                    ApnContext.requestLog(dp.mApnContext, str);

                    // Clear out existing qos sessions
                    updateQosParameters(null);

                    if (dp.mTag == mTag) {
                        // Transition to inactive but send notifications after
                        // we've entered the mInactive state.
                        mInactiveState.setEnterNotificationParams(dp);
                        transitionTo(mInactiveState);
                    } else {
                        if (DBG) log("DcDisconnectState stale EVENT_DEACTIVATE_DONE"
                                + " dp.tag=" + dp.mTag + " mTag=" + mTag);
                    }
                    retVal = HANDLED;
                    break;

                default:
                    if (VDBG) {
                        log("DcDisconnectingState not handled msg.what="
                                + getWhatToString(msg.what));
                    }
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }
    }
    private DcDisconnectingState mDisconnectingState = new DcDisconnectingState();

    /**
     * The state machine is disconnecting after an creating a connection.
     */
    private class DcDisconnectionErrorCreatingConnection extends State {
        @Override
        public void enter() {
            TelephonyStatsLog.write(TelephonyStatsLog.MOBILE_CONNECTION_STATE_CHANGED,
                    TelephonyStatsLog
                            .MOBILE_CONNECTION_STATE_CHANGED__STATE__DISCONNECTION_ERROR_CREATING_CONNECTION,
                    mPhone.getPhoneId(), mId, getApnTypeBitmask(), canHandleDefault());
            notifyDataConnectionState();
        }
        @Override
        public boolean processMessage(Message msg) {
            boolean retVal;

            switch (msg.what) {
                case EVENT_DEACTIVATE_DONE:
                    ConnectionParams cp = (ConnectionParams) msg.obj;
                    if (cp.mTag == mTag) {
                        String str = "DcDisconnectionErrorCreatingConnection" +
                                " msg.what=EVENT_DEACTIVATE_DONE";
                        if (DBG) log(str);
                        ApnContext.requestLog(cp.mApnContext, str);

                        // Transition to inactive but send notifications after
                        // we've entered the mInactive state.
                        mInactiveState.setEnterNotificationParams(cp,
                                DataFailCause.UNACCEPTABLE_NETWORK_PARAMETER,
                                DataCallResponse.HANDOVER_FAILURE_MODE_UNKNOWN);
                        transitionTo(mInactiveState);
                    } else {
                        if (DBG) {
                            log("DcDisconnectionErrorCreatingConnection stale EVENT_DEACTIVATE_DONE"
                                    + " dp.tag=" + cp.mTag + ", mTag=" + mTag);
                        }
                    }
                    retVal = HANDLED;
                    break;

                default:
                    if (VDBG) {
                        log("DcDisconnectionErrorCreatingConnection not handled msg.what="
                                + getWhatToString(msg.what));
                    }
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }
    }
    private DcDisconnectionErrorCreatingConnection mDisconnectingErrorCreatingConnection =
                new DcDisconnectionErrorCreatingConnection();

    /**
     * Bring up a connection to the apn and return an AsyncResult in onCompletedMsg.
     * Used for cellular networks that use Access Point Names (APN) such
     * as GSM networks.
     *
     * @param apnContext is the Access Point Name to bring up a connection to
     * @param profileId for the connection
     * @param rilRadioTechnology Radio technology for the data connection
     * @param onCompletedMsg is sent with its msg.obj as an AsyncResult object.
     *                       With AsyncResult.userObj set to the original msg.obj,
     *                       AsyncResult.result = FailCause and AsyncResult.exception = Exception().
     * @param connectionGeneration used to track a single connection request so disconnects can get
     *                             ignored if obsolete.
     * @param requestType Data request type
     * @param subId the subscription id associated with this data connection.
     * @param isApnPreferred whether or not the apn is preferred.
     */
    public void bringUp(ApnContext apnContext, int profileId, int rilRadioTechnology,
                        Message onCompletedMsg, int connectionGeneration,
                        @RequestNetworkType int requestType, int subId, boolean isApnPreferred) {
        if (DBG) {
            log("bringUp: apnContext=" + apnContext + " onCompletedMsg=" + onCompletedMsg);
        }

        if (mApnSetting == null) {
            mApnSetting = apnContext.getApnSetting();
        }

        sendMessage(DataConnection.EVENT_CONNECT,
                new ConnectionParams(apnContext, profileId, rilRadioTechnology, onCompletedMsg,
                        connectionGeneration, requestType, subId, isApnPreferred));
    }

    /**
     * Tear down the connection through the apn on the network.
     *
     * @param apnContext APN context
     * @param reason reason to tear down
     * @param onCompletedMsg is sent with its msg.obj as an AsyncResult object.
     *        With AsyncResult.userObj set to the original msg.obj.
     */
    public void tearDown(ApnContext apnContext, String reason, Message onCompletedMsg) {
        if (DBG) {
            log("tearDown: apnContext=" + apnContext + " reason=" + reason + " onCompletedMsg="
                    + onCompletedMsg);
        }
        sendMessage(DataConnection.EVENT_DISCONNECT,
                new DisconnectParams(apnContext, reason, DcTracker.RELEASE_TYPE_DETACH,
                        onCompletedMsg));
    }

    // ******* "public" interface

    /**
     * Used for testing purposes.
     */
    void tearDownNow() {
        if (DBG) log("tearDownNow()");
        sendMessage(obtainMessage(EVENT_TEAR_DOWN_NOW));
    }

    /**
     * Tear down the connection through the apn on the network.  Ignores reference count and
     * and always tears down.
     *
     * @param releaseType Data release type
     * @param onCompletedMsg is sent with its msg.obj as an AsyncResult object.
     *        With AsyncResult.userObj set to the original msg.obj.
     */
    public void tearDownAll(String reason, @ReleaseNetworkType int releaseType,
                            Message onCompletedMsg) {
        if (DBG) {
            log("tearDownAll: reason=" + reason + ", releaseType="
                    + DcTracker.releaseTypeToString(releaseType));
        }
        sendMessage(DataConnection.EVENT_DISCONNECT_ALL,
                new DisconnectParams(null, reason, releaseType, onCompletedMsg));
    }

    /**
     * Reset the data connection to inactive state.
     */
    public void reset() {
        sendMessage(EVENT_RESET);
        if (DBG) log("reset");
    }

    /**
     * Re-evaluate the restricted state. If the restricted data connection does not need to be
     * restricted anymore, we need to dynamically change the network's capability.
     */
    void reevaluateRestrictedState() {
        sendMessage(EVENT_REEVALUATE_RESTRICTED_STATE);
        if (DBG) log("reevaluate restricted state");
    }

    /**
     * Re-evaluate the data connection properties. For example, it will recalculate data connection
     * score and update through network agent it if changed.
     */
    void reevaluateDataConnectionProperties() {
        sendMessage(EVENT_REEVALUATE_DATA_CONNECTION_PROPERTIES);
        if (DBG) log("reevaluate data connection properties");
    }

    /**
     * @return The parameters used for initiating a data connection.
     */
    public ConnectionParams getConnectionParams() {
        return mConnectionParams;
    }

    /**
     * Update PCSCF addresses
     *
     * @param response
     */
    public void updatePcscfAddr(DataCallResponse response) {
        mPcscfAddr = response.getPcscfAddresses().stream()
                .map(InetAddress::getHostAddress).toArray(String[]::new);
    }

    /**
     * @return The list of PCSCF addresses
     */
    public String[] getPcscfAddresses() {
        return mPcscfAddr;
    }

    /**
     * Using the result of the SETUP_DATA_CALL determine the retry delay.
     *
     * @param response The response from setup data call
     * @return {@link RetryManager#NO_SUGGESTED_RETRY_DELAY} if not suggested.
     * {@link RetryManager#NO_RETRY} if retry should not happen. Otherwise the delay in milliseconds
     * to the next SETUP_DATA_CALL.
     */
    private long getSuggestedRetryDelay(DataCallResponse response) {
        /** According to ril.h
         * The value < 0 means no value is suggested
         * The value 0 means retry should be done ASAP.
         * The value of Long.MAX_VALUE(0x7fffffffffffffff) means no retry.
         */
        if (response == null) {
            return RetryManager.NO_SUGGESTED_RETRY_DELAY;
        }

        long suggestedRetryTime = response.getRetryDurationMillis();

        // The value < 0 means no value is suggested
        if (suggestedRetryTime < 0) {
            if (DBG) log("No suggested retry delay.");
            return RetryManager.NO_SUGGESTED_RETRY_DELAY;
        } else if (mPhone.getHalVersion().greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)
                && suggestedRetryTime == Long.MAX_VALUE) {
            if (DBG) log("Network suggested not retrying.");
            return RetryManager.NO_RETRY;
        } else if (mPhone.getHalVersion().less(RIL.RADIO_HAL_VERSION_1_6)
                && suggestedRetryTime == Integer.MAX_VALUE) {
            if (DBG) log("Network suggested not retrying.");
            return RetryManager.NO_RETRY;
        }

        return suggestedRetryTime;
    }

    public List<ApnContext> getApnContexts() {
        return new ArrayList<>(mApnContexts.keySet());
    }

    /**
     * Return whether there is an ApnContext for the given type in this DataConnection.
     * @param type APN type to check
     * @param exclusive true if the given APN type should be the only APN type that exists
     * @return True if there is an ApnContext for the given type
     */
    private boolean isApnContextAttached(@ApnType int type, boolean exclusive) {
        boolean attached = mApnContexts.keySet().stream()
                .map(ApnContext::getApnTypeBitmask)
                .anyMatch(bitmask -> bitmask == type);
        if (exclusive) {
            attached &= mApnContexts.size() == 1;
        }
        return attached;
    }

    /** Get the network agent of the data connection */
    @Nullable
    DcNetworkAgent getNetworkAgent() {
        return mNetworkAgent;
    }

    void setHandoverState(@HandoverState int state) {
        if (mHandoverState != state) {
            String logStr = "State changed from " + handoverStateToString(mHandoverState)
                    + " to " + handoverStateToString(state);
            mHandoverLocalLog.log(logStr);
            logd(logStr);
            mHandoverState = state;
        }
    }

    /** Sets the {@link DataCallSessionStats} mock for this data connection during unit testing. */
    @VisibleForTesting
    public void setDataCallSessionStats(DataCallSessionStats dataCallSessionStats) {
        mDataCallSessionStats = dataCallSessionStats;
    }

    /**
     * @return the string for msg.what as our info.
     */
    @Override
    protected String getWhatToString(int what) {
        return cmdToString(what);
    }

    private static String msgToString(Message msg) {
        String retVal;
        if (msg == null) {
            retVal = "null";
        } else {
            StringBuilder   b = new StringBuilder();

            b.append("{what=");
            b.append(cmdToString(msg.what));

            b.append(" when=");
            TimeUtils.formatDuration(msg.getWhen() - SystemClock.uptimeMillis(), b);

            if (msg.arg1 != 0) {
                b.append(" arg1=");
                b.append(msg.arg1);
            }

            if (msg.arg2 != 0) {
                b.append(" arg2=");
                b.append(msg.arg2);
            }

            if (msg.obj != null) {
                b.append(" obj=");
                b.append(msg.obj);
            }

            b.append(" target=");
            b.append(msg.getTarget());

            b.append(" replyTo=");
            b.append(msg.replyTo);

            b.append("}");

            retVal = b.toString();
        }
        return retVal;
    }

    static void slog(String s) {
        Rlog.d("DC", s);
    }

    /**
     * Log with debug
     *
     * @param s is string log
     */
    @Override
    protected void log(String s) {
        Rlog.d(getName(), s);
    }

    /**
     * Log with debug attribute
     *
     * @param s is string log
     */
    @Override
    protected void logd(String s) {
        Rlog.d(getName(), s);
    }

    /**
     * Log with verbose attribute
     *
     * @param s is string log
     */
    @Override
    protected void logv(String s) {
        Rlog.v(getName(), s);
    }

    /**
     * Log with info attribute
     *
     * @param s is string log
     */
    @Override
    protected void logi(String s) {
        Rlog.i(getName(), s);
    }

    /**
     * Log with warning attribute
     *
     * @param s is string log
     */
    @Override
    protected void logw(String s) {
        Rlog.w(getName(), s);
    }

    /**
     * Log with error attribute
     *
     * @param s is string log
     */
    @Override
    protected void loge(String s) {
        Rlog.e(getName(), s);
    }

    /**
     * Log with error attribute
     *
     * @param s is string log
     * @param e is a Throwable which logs additional information.
     */
    @Override
    protected void loge(String s, Throwable e) {
        Rlog.e(getName(), s, e);
    }

    /** Doesn't print mApnList of ApnContext's which would be recursive */
    public synchronized String toStringSimple() {
        return getName() + ": State=" + getCurrentState().getName()
                + " mApnSetting=" + mApnSetting + " RefCount=" + mApnContexts.size()
                + " mCid=" + mCid + " mCreateTime=" + mCreateTime
                + " mLastastFailTime=" + mLastFailTime
                + " mLastFailCause=" + DataFailCause.toString(mLastFailCause)
                + " mTag=" + mTag
                + " mLinkProperties=" + mLinkProperties
                + " linkCapabilities=" + getNetworkCapabilities()
                + " mRestrictedNetworkOverride=" + mRestrictedNetworkOverride;
    }

    @Override
    public String toString() {
        return "{" + toStringSimple() + " mApnContexts=" + mApnContexts + "}";
    }

    /** Check if the device is connected to NR 5G Non-Standalone network. */
    private boolean isNRConnected() {
        return mPhone.getServiceState().getNrState()
                == NetworkRegistrationInfo.NR_STATE_CONNECTED;
    }

    /**
     * @return The disallowed APN types bitmask
     */
    private @ApnType int getDisallowedApnTypes() {
        CarrierConfigManager configManager = (CarrierConfigManager)
                mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        int apnTypesBitmask = 0;
        if (configManager != null) {
            PersistableBundle bundle = configManager.getConfigForSubId(mSubId);
            if (bundle != null) {
                String key = (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                        ? CarrierConfigManager.KEY_CARRIER_WWAN_DISALLOWED_APN_TYPES_STRING_ARRAY
                        : CarrierConfigManager.KEY_CARRIER_WLAN_DISALLOWED_APN_TYPES_STRING_ARRAY;
                if (bundle.getStringArray(key) != null) {
                    String disallowedApnTypesString =
                            TextUtils.join(",", bundle.getStringArray(key));
                    if (!TextUtils.isEmpty(disallowedApnTypesString)) {
                        apnTypesBitmask = ApnSetting.getApnTypesBitmaskFromString(
                                disallowedApnTypesString);
                    }
                }
            }
        }

        return apnTypesBitmask;
    }

    private void dumpToLog() {
        dump(null, new PrintWriter(new StringWriter(0)) {
            @Override
            public void println(String s) {
                DataConnection.this.logd(s);
            }

            @Override
            public void flush() {
            }
        }, null);
    }

    /**
     *  Re-calculate score and update through network agent if it changes.
     */
    private void updateScore() {
        int oldScore = mScore;
        mScore = calculateScore();
        if (oldScore != mScore && mNetworkAgent != null) {
            log("Updating score from " + oldScore + " to " + mScore);
            mNetworkAgent.sendNetworkScore(mScore, this);
        }
    }

    private int calculateScore() {
        int score = OTHER_CONNECTION_SCORE;

        // If it's serving a network request that asks NET_CAPABILITY_INTERNET and doesn't have
        // specify a subId, this dataConnection is considered to be default Internet data
        // connection. In this case we assign a slightly higher score of 50. The intention is
        // it will not be replaced by other data connections accidentally in DSDS usecase.
        for (ApnContext apnContext : mApnContexts.keySet()) {
            for (NetworkRequest networkRequest : apnContext.getNetworkRequests()) {
                if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && networkRequest.getNetworkSpecifier() == null) {
                    score = DEFAULT_INTERNET_CONNECTION_SCORE;
                    break;
                }
            }
        }

        return score;
    }

    private String handoverStateToString(@HandoverState int state) {
        switch (state) {
            case HANDOVER_STATE_IDLE: return "IDLE";
            case HANDOVER_STATE_BEING_TRANSFERRED: return "BEING_TRANSFERRED";
            case HANDOVER_STATE_COMPLETED: return "COMPLETED";
            default: return "UNKNOWN";
        }
    }

    private @DataState int getState() {
        if (isInactive()) {
            return TelephonyManager.DATA_DISCONNECTED;
        } else if (isActivating()) {
            return TelephonyManager.DATA_CONNECTING;
        } else if (isActive()) {
            // The data connection can only be suspended when it's in active state.
            if (mIsSuspended) {
                return TelephonyManager.DATA_SUSPENDED;
            }
            return TelephonyManager.DATA_CONNECTED;
        } else if (isDisconnecting()) {
            return TelephonyManager.DATA_DISCONNECTING;
        }

        return TelephonyManager.DATA_UNKNOWN;
    }

    /**
     * Get precise data connection state
     *
     * @return The {@link PreciseDataConnectionState}
     */
    public PreciseDataConnectionState getPreciseDataConnectionState() {
        return new PreciseDataConnectionState.Builder()
                .setTransportType(mTransportType)
                .setId(mCid)
                .setState(getState())
                .setApnSetting(mApnSetting)
                .setLinkProperties(mLinkProperties)
                .setNetworkType(getNetworkType())
                .setFailCause(mDcFailCause)
                .build();
    }

    /**
     * Dump the current state.
     *
     * @param fd
     * @param printWriter
     * @param args
     */
    @Override
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, " ");
        pw.print("DataConnection ");
        super.dump(fd, pw, args);
        pw.flush();
        pw.increaseIndent();
        pw.println("transport type="
                + AccessNetworkConstants.transportTypeToString(mTransportType));
        pw.println("mApnContexts.size=" + mApnContexts.size());
        pw.println("mApnContexts=" + mApnContexts);
        pw.println("mApnSetting=" + mApnSetting);
        pw.println("mTag=" + mTag);
        pw.println("mCid=" + mCid);
        pw.println("mConnectionParams=" + mConnectionParams);
        pw.println("mDisconnectParams=" + mDisconnectParams);
        pw.println("mDcFailCause=" + DataFailCause.toString(mDcFailCause));
        pw.println("mPhone=" + mPhone);
        pw.println("mSubId=" + mSubId);
        pw.println("mLinkProperties=" + mLinkProperties);
        pw.flush();
        pw.println("mDataRegState=" + mDataRegState);
        pw.println("mHandoverState=" + handoverStateToString(mHandoverState));
        pw.println("mRilRat=" + mRilRat);
        pw.println("mNetworkCapabilities=" + getNetworkCapabilities());
        pw.println("mCreateTime=" + TimeUtils.logTimeOfDay(mCreateTime));
        pw.println("mLastFailTime=" + TimeUtils.logTimeOfDay(mLastFailTime));
        pw.println("mLastFailCause=" + DataFailCause.toString(mLastFailCause));
        pw.println("mUserData=" + mUserData);
        pw.println("mRestrictedNetworkOverride=" + mRestrictedNetworkOverride);
        pw.println("mUnmeteredUseOnly=" + mUnmeteredUseOnly);
        pw.println("mMmsUseOnly=" + mMmsUseOnly);
        pw.println("mEnterpriseUse=" + mEnterpriseUse);
        pw.println("mUnmeteredOverride=" + mUnmeteredOverride);
        pw.println("mCongestedOverride=" + mCongestedOverride);
        pw.println("mDownlinkBandwidth" + mDownlinkBandwidth);
        pw.println("mUplinkBandwidth=" + mUplinkBandwidth);
        pw.println("mDefaultQos=" + mDefaultQos);
        pw.println("mQosBearerSessions=" + mQosBearerSessions);
        pw.println("disallowedApnTypes="
                + ApnSetting.getApnTypesStringFromBitmask(getDisallowedApnTypes()));
        pw.println("mInstanceNumber=" + mInstanceNumber);
        pw.println("mAc=" + mAc);
        pw.println("mScore=" + mScore);
        if (mNetworkAgent != null) {
            mNetworkAgent.dump(fd, pw, args);
        }
        pw.println("handover local log:");
        pw.increaseIndent();
        mHandoverLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
        pw.println();
        pw.flush();
    }

    /**
     * Class used to track VCN-defined Network policies for this DataConnection.
     *
     * <p>MUST be registered with the associated DataConnection's Handler.
     */
    private class DataConnectionVcnNetworkPolicyChangeListener
            implements VcnNetworkPolicyChangeListener {
        @Override
        public void onPolicyChanged() {
            // Poll the current underlying Network policy from VcnManager and send to NetworkAgent.
            final NetworkCapabilities networkCapabilities = getNetworkCapabilities();
            VcnNetworkPolicyResult policyResult =
                    mVcnManager.applyVcnNetworkPolicy(
                            networkCapabilities, getLinkProperties());
            if (policyResult.isTeardownRequested()) {
                tearDownAll(
                        Phone.REASON_VCN_REQUESTED_TEARDOWN,
                        DcTracker.RELEASE_TYPE_DETACH,
                        null /* onCompletedMsg */);
            }

            if (mNetworkAgent != null) {
                mNetworkAgent.sendNetworkCapabilities(networkCapabilities, DataConnection.this);
            }
        }
    }
}


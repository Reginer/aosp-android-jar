/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.telephony.metrics;

import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_CS;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_IMS;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_UNKNOWN;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__DIRECTION__CALL_DIRECTION_MO;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__DIRECTION__CALL_DIRECTION_MT;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_FULLBAND;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_NARROWBAND;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_SUPER_WIDEBAND;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_UNKNOWN;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_WIDEBAND;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_EXTREMELY_FAST;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_EXTREMELY_SLOW;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_FAST;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_NORMAL;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_SLOW;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_ULTRA_FAST;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_ULTRA_SLOW;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_UNKNOWN;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_VERY_FAST;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_VERY_SLOW;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SIGNAL_STRENGTH_AT_END__SIGNAL_STRENGTH_GREAT;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__SIGNAL_STRENGTH_AT_END__SIGNAL_STRENGTH_NONE_OR_UNKNOWN;

import android.annotation.Nullable;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.telecom.VideoProfile;
import android.telecom.VideoProfile.VideoState;
import android.telephony.Annotation.NetworkType;
import android.telephony.DisconnectCause;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsStreamMediaProfile;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.GsmCdmaConnection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.internal.telephony.nano.PersistAtomsProto.VoiceCallSession;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.AudioCodec;
import com.android.internal.telephony.uicc.UiccController;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Collects voice call events per phone ID for the pulled atom. */
public class VoiceCallSessionStats {
    private static final String TAG = VoiceCallSessionStats.class.getSimpleName();

    /** Upper bounds of each call setup duration category in milliseconds. */
    private static final int CALL_SETUP_DURATION_UNKNOWN = 0;
    private static final int CALL_SETUP_DURATION_EXTREMELY_FAST = 400;
    private static final int CALL_SETUP_DURATION_ULTRA_FAST = 700;
    private static final int CALL_SETUP_DURATION_VERY_FAST = 1000;
    private static final int CALL_SETUP_DURATION_FAST = 1500;
    private static final int CALL_SETUP_DURATION_NORMAL = 2500;
    private static final int CALL_SETUP_DURATION_SLOW = 4000;
    private static final int CALL_SETUP_DURATION_VERY_SLOW = 6000;
    private static final int CALL_SETUP_DURATION_ULTRA_SLOW = 10000;
    // CALL_SETUP_DURATION_EXTREMELY_SLOW has no upper bound (it includes everything above 10000)

    /** Number of buckets for codec quality, from UNKNOWN to FULLBAND. */
    private static final int CODEC_QUALITY_COUNT = 5;

    /**
     * Threshold to calculate the main audio codec quality of the call.
     *
     * The audio codec quality was equal to or greater than the main audio codec quality for
     * at least 70% of the call.
     */
    private static final int MAIN_CODEC_QUALITY_THRESHOLD = 70;

    /** Holds the audio codec value for CS calls. */
    private static final SparseIntArray CS_CODEC_MAP = buildGsmCdmaCodecMap();

    /** Holds the audio codec value for IMS calls. */
    private static final SparseIntArray IMS_CODEC_MAP = buildImsCodecMap();

    /** Holds setup duration buckets with values as their upper bounds in milliseconds. */
    private static final SparseIntArray CALL_SETUP_DURATION_MAP = buildCallSetupDurationMap();

    /**
     * Tracks statistics for each call connection, indexed with ID returned by {@link
     * #getConnectionId}.
     */
    private final SparseArray<VoiceCallSession> mCallProtos = new SparseArray<>();

    /**
     * Tracks usage of codecs for each call. The outer array is used to map each connection id to
     * the corresponding codec usage. The inner array is used to map timestamp (key) with the
     * codec in use (value).
     */
    private final SparseArray<LongSparseArray<Integer>> mCodecUsage = new SparseArray<>();

    /**
     * Tracks call RAT usage.
     *
     * <p>RAT usage is mainly tied to phones rather than calls, since each phone can have multiple
     * concurrent calls, and we do not want to count the RAT duration multiple times.
     */
    private final VoiceCallRatTracker mRatUsage = new VoiceCallRatTracker();

    private final int mPhoneId;
    private final Phone mPhone;

    private final PersistAtomsStorage mAtomsStorage =
            PhoneFactory.getMetricsCollector().getAtomsStorage();
    private final UiccController mUiccController = UiccController.getInstance();

    public VoiceCallSessionStats(int phoneId, Phone phone) {
        mPhoneId = phoneId;
        mPhone = phone;
    }

    /* CS calls */

    /** Updates internal states when previous CS calls are accepted to track MT call setup time. */
    public synchronized void onRilAcceptCall(List<Connection> connections) {
        for (Connection conn : connections) {
            addCall(conn);
        }
    }

    /** Updates internal states when a CS MO call is created. */
    public synchronized void onRilDial(Connection conn) {
        addCall(conn);
    }

    /**
     * Updates internal states when CS calls are created or terminated, or CS call state is changed.
     */
    public synchronized void onRilCallListChanged(List<GsmCdmaConnection> connections) {
        for (Connection conn : connections) {
            int id = getConnectionId(conn);
            if (!mCallProtos.contains(id)) {
                // handle new connections
                if (conn.getDisconnectCause() == DisconnectCause.NOT_DISCONNECTED) {
                    addCall(conn);
                    checkCallSetup(conn, mCallProtos.get(id));
                } else {
                    logd("onRilCallListChanged: skip adding disconnected connection");
                }
            } else {
                VoiceCallSession proto = mCallProtos.get(id);
                // handle call state change
                checkCallSetup(conn, proto);
                // handle terminated connections
                if (conn.getDisconnectCause() != DisconnectCause.NOT_DISCONNECTED) {
                    proto.bearerAtEnd = getBearer(conn); // should be CS
                    proto.disconnectReasonCode = conn.getDisconnectCause();
                    proto.disconnectExtraCode = conn.getPreciseDisconnectCause();
                    proto.disconnectExtraMessage = conn.getVendorDisconnectCause();
                    finishCall(id);
                }
            }
        }
        // NOTE: we cannot check stray connections (CS call in our list but not in RIL), as
        // GsmCdmaCallTracker can call this with a partial list
    }

    /* IMS calls */

    /** Updates internal states when an IMS MO call is created. */
    public synchronized void onImsDial(ImsPhoneConnection conn) {
        addCall(conn);
        if (conn.hasRttTextStream()) {
            setRttStarted(conn);
        }
    }

    /** Updates internal states when an IMS MT call is created. */
    public synchronized void onImsCallReceived(ImsPhoneConnection conn) {
        addCall(conn);
        if (conn.hasRttTextStream()) {
            setRttStarted(conn);
        }
    }

    /** Updates internal states when previous IMS calls are accepted to track MT call setup time. */
    public synchronized void onImsAcceptCall(List<Connection> connections) {
        for (Connection conn : connections) {
            addCall(conn);
        }
    }

    /** Updates internal states when an IMS call is terminated. */
    public synchronized void onImsCallTerminated(
            @Nullable ImsPhoneConnection conn, ImsReasonInfo reasonInfo) {
        if (conn == null) {
            List<Integer> imsConnIds = getImsConnectionIds();
            if (imsConnIds.size() == 1) {
                loge("onImsCallTerminated: ending IMS call w/ conn=null");
                finishImsCall(imsConnIds.get(0), reasonInfo);
            } else {
                loge("onImsCallTerminated: %d IMS calls w/ conn=null", imsConnIds.size());
            }
        } else {
            int id = getConnectionId(conn);
            if (mCallProtos.contains(id)) {
                finishImsCall(id, reasonInfo);
            } else {
                loge("onImsCallTerminated: untracked connection");
                // fake a call so at least some info can be tracked
                addCall(conn);
                finishImsCall(id, reasonInfo);
            }
        }
    }

    /** Updates internal states when RTT is started on an IMS call. */
    public synchronized void onRttStarted(ImsPhoneConnection conn) {
        setRttStarted(conn);
    }

    /* general & misc. */

    /** Updates internal states when audio codec for a call is changed. */
    public synchronized void onAudioCodecChanged(Connection conn, int audioQuality) {
        int id = getConnectionId(conn);
        VoiceCallSession proto = mCallProtos.get(id);
        if (proto == null) {
            loge("onAudioCodecChanged: untracked connection");
            return;
        }
        int codec = audioQualityToCodec(proto.bearerAtEnd, audioQuality);
        proto.codecBitmask |= (1L << codec);

        if (mCodecUsage.contains(id)) {
            mCodecUsage.get(id).append(getTimeMillis(), codec);
        } else {
            LongSparseArray<Integer> arr = new LongSparseArray<>();
            arr.append(getTimeMillis(), codec);
            mCodecUsage.put(id, arr);
        }
    }

    /** Updates internal states when video state changes. */
    public synchronized void onVideoStateChange(
            ImsPhoneConnection conn, @VideoState int videoState) {
        int id = getConnectionId(conn);
        VoiceCallSession proto = mCallProtos.get(id);
        if (proto == null) {
            loge("onVideoStateChange: untracked connection");
            return;
        }
        logd("Video state = " + videoState);
        if (videoState != VideoProfile.STATE_AUDIO_ONLY) {
            proto.videoEnabled = true;
        }
    }

    /** Updates internal states when multiparty state changes. */
    public synchronized void onMultipartyChange(ImsPhoneConnection conn, boolean isMultiParty) {
        int id = getConnectionId(conn);
        VoiceCallSession proto = mCallProtos.get(id);
        if (proto == null) {
            loge("onMultipartyChange: untracked connection");
            return;
        }
        logd("Multiparty = " + isMultiParty);
        if (isMultiParty) {
            proto.isMultiparty = true;
        }
    }

    /**
     * Updates internal states when a call changes state to track setup time and status.
     *
     * <p>This is currently mainly used by IMS since CS call states are updated through {@link
     * #onRilCallListChanged}.
     */
    public synchronized void onCallStateChanged(Call call) {
        for (Connection conn : call.getConnections()) {
            VoiceCallSession proto = mCallProtos.get(getConnectionId(conn));
            if (proto != null) {
                checkCallSetup(conn, proto);
            } else {
                loge("onCallStateChanged: untracked connection");
            }
        }
    }

    /** Updates internal states when an IMS call is handover to a CS call. */
    public synchronized void onRilSrvccStateChanged(int state) {
        List<Connection> handoverConnections = null;
        if (mPhone.getImsPhone() != null) {
            loge("onRilSrvccStateChanged: ImsPhone is null");
        } else {
            handoverConnections = mPhone.getImsPhone().getHandoverConnection();
        }
        List<Integer> imsConnIds;
        if (handoverConnections == null) {
            imsConnIds = getImsConnectionIds();
            loge("onRilSrvccStateChanged: ImsPhone has no handover, we have %d", imsConnIds.size());
        } else {
            imsConnIds =
                    handoverConnections.stream()
                            .map(VoiceCallSessionStats::getConnectionId)
                            .collect(Collectors.toList());
        }
        switch (state) {
            case TelephonyManager.SRVCC_STATE_HANDOVER_COMPLETED:
                // connection will now be CS
                for (int id : imsConnIds) {
                    VoiceCallSession proto = mCallProtos.get(id);
                    proto.srvccCompleted = true;
                    proto.bearerAtEnd = VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_CS;
                }
                break;
            case TelephonyManager.SRVCC_STATE_HANDOVER_FAILED:
                for (int id : imsConnIds) {
                    mCallProtos.get(id).srvccFailureCount++;
                }
                break;
            case TelephonyManager.SRVCC_STATE_HANDOVER_CANCELED:
                for (int id : imsConnIds) {
                    mCallProtos.get(id).srvccCancellationCount++;
                }
                break;
            default: // including STARTED and NONE, do nothing
        }
    }

    /** Updates internal states when RAT changes. */
    public synchronized void onServiceStateChanged(ServiceState state) {
        if (hasCalls()) {
            updateRatTracker(state);
        }
    }

    /* internal */

    /**
     * Adds a call connection.
     *
     * <p>Should be called when the call is created, and when setup begins (upon {@code
     * RilRequest.RIL_REQUEST_ANSWER} or {@code ImsCommand.IMS_CMD_ACCEPT}).
     */
    private void addCall(Connection conn) {
        int id = getConnectionId(conn);
        if (mCallProtos.contains(id)) {
            // mostly handles ringing MT call getting accepted (MT call setup begins)
            logd("addCall: resetting setup info");
            VoiceCallSession proto = mCallProtos.get(id);
            proto.setupBeginMillis = getTimeMillis();
            proto.setupDuration = VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_UNKNOWN;
        } else {
            int bearer = getBearer(conn);
            ServiceState serviceState = getServiceState();
            @NetworkType int rat = getRat(serviceState);

            VoiceCallSession proto = new VoiceCallSession();

            proto.bearerAtStart = bearer;
            proto.bearerAtEnd = bearer;
            proto.direction = getDirection(conn);
            proto.setupDuration = VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_UNKNOWN;
            proto.setupFailed = true;
            proto.disconnectReasonCode = conn.getDisconnectCause();
            proto.disconnectExtraCode = conn.getPreciseDisconnectCause();
            proto.disconnectExtraMessage = conn.getVendorDisconnectCause();
            proto.ratAtStart = rat;
            proto.ratAtConnected = TelephonyManager.NETWORK_TYPE_UNKNOWN;
            proto.ratAtEnd = rat;
            proto.ratSwitchCount = 0L;
            proto.codecBitmask = 0L;
            proto.simSlotIndex = mPhoneId;
            proto.isMultiSim = SimSlotState.isMultiSim();
            proto.isEsim = SimSlotState.isEsim(mPhoneId);
            proto.carrierId = mPhone.getCarrierId();
            proto.srvccCompleted = false;
            proto.srvccFailureCount = 0L;
            proto.srvccCancellationCount = 0L;
            proto.rttEnabled = false;
            proto.isEmergency = conn.isEmergencyCall();
            proto.isRoaming = serviceState != null ? serviceState.getVoiceRoaming() : false;
            proto.isMultiparty = conn.isMultiparty();

            // internal fields for tracking
            proto.setupBeginMillis = getTimeMillis();

            proto.concurrentCallCountAtStart = mCallProtos.size();
            mCallProtos.put(id, proto);

            // RAT call count needs to be updated
            updateRatTracker(serviceState);
        }
    }

    /** Sends the call metrics to persist storage when it is finished. */
    private void finishCall(int connectionId) {
        VoiceCallSession proto = mCallProtos.get(connectionId);
        if (proto == null) {
            loge("finishCall: could not find call to be removed");
            return;
        }
        mCallProtos.delete(connectionId);
        proto.concurrentCallCountAtEnd = mCallProtos.size();

        // Calculate signal strength at the end of the call
        proto.signalStrengthAtEnd = getSignalStrength(proto.ratAtEnd);

        // Calculate main codec quality
        proto.mainCodecQuality = finalizeMainCodecQuality(connectionId);

        // ensure internal fields are cleared
        proto.setupBeginMillis = 0L;

        // sanitize for javanano & StatsEvent
        if (proto.disconnectExtraMessage == null) {
            proto.disconnectExtraMessage = "";
        }

        // Retry populating carrier ID if it was invalid
        if (proto.carrierId <= 0) {
            proto.carrierId = mPhone.getCarrierId();
        }

        mAtomsStorage.addVoiceCallSession(proto);

        // merge RAT usages to PersistPullers when the call session ends (i.e. no more active calls)
        if (!hasCalls()) {
            mRatUsage.conclude(getTimeMillis());
            mAtomsStorage.addVoiceCallRatUsage(mRatUsage);
            mRatUsage.clear();
        }
    }

    private void setRttStarted(ImsPhoneConnection conn) {
        VoiceCallSession proto = mCallProtos.get(getConnectionId(conn));
        if (proto == null) {
            loge("onRttStarted: untracked connection");
            return;
        }
        // should be IMS w/o SRVCC
        if (proto.bearerAtStart != getBearer(conn) || proto.bearerAtEnd != getBearer(conn)) {
            loge("onRttStarted: connection bearer mismatch but proceeding");
        }
        proto.rttEnabled = true;
    }

    /** Returns a {@link Set} of Connection IDs so RAT usage can be correctly tracked. */
    private Set<Integer> getConnectionIds() {
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < mCallProtos.size(); i++) {
            ids.add(mCallProtos.keyAt(i));
        }
        return ids;
    }

    private List<Integer> getImsConnectionIds() {
        List<Integer> imsConnIds = new ArrayList<>(mCallProtos.size());
        for (int i = 0; i < mCallProtos.size(); i++) {
            if (mCallProtos.valueAt(i).bearerAtEnd
                    == VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_IMS) {
                imsConnIds.add(mCallProtos.keyAt(i));
            }
        }
        return imsConnIds;
    }

    private boolean hasCalls() {
        return mCallProtos.size() > 0;
    }

    private void checkCallSetup(Connection conn, VoiceCallSession proto) {
        if (proto.setupBeginMillis != 0L && isSetupFinished(conn.getCall())) {
            proto.setupDurationMillis = (int) (getTimeMillis() - proto.setupBeginMillis);
            proto.setupDuration = classifySetupDuration(proto.setupDurationMillis);
            proto.setupBeginMillis = 0L;
        }
        // Clear setupFailed if call now active, but otherwise leave it unchanged
        // This block is executed only once, when call becomes active for the first time.
        if (proto.setupFailed && conn.getState() == Call.State.ACTIVE) {
            proto.setupFailed = false;
            // Track RAT when voice call is connected.
            ServiceState serviceState = getServiceState();
            proto.ratAtConnected = getRat(serviceState);
            // Reset list of codecs with the last codec at the present time. In this way, we
            // track codec quality only after call is connected and not while ringing.
            resetCodecList(conn);
        }
    }

    private void updateRatTracker(ServiceState state) {
        @NetworkType int rat = getRat(state);
        int band = ServiceStateStats.getBand(state, rat);

        mRatUsage.add(mPhone.getCarrierId(), rat, getTimeMillis(), getConnectionIds());
        for (int i = 0; i < mCallProtos.size(); i++) {
            VoiceCallSession proto = mCallProtos.valueAt(i);
            if (proto.ratAtEnd != rat) {
                proto.ratSwitchCount++;
                proto.ratAtEnd = rat;
            }
            proto.bandAtEnd = band;
            // assuming that SIM carrier ID does not change during the call
        }
    }

    private void finishImsCall(int id, ImsReasonInfo reasonInfo) {
        VoiceCallSession proto = mCallProtos.get(id);
        proto.bearerAtEnd = VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_IMS;
        proto.disconnectReasonCode = reasonInfo.mCode;
        proto.disconnectExtraCode = reasonInfo.mExtraCode;
        proto.disconnectExtraMessage = ImsStats.filterExtraMessage(reasonInfo.mExtraMessage);
        finishCall(id);
    }

    private @Nullable ServiceState getServiceState() {
        ServiceStateTracker tracker = mPhone.getServiceStateTracker();
        return tracker != null ? tracker.getServiceState() : null;
    }

    private static int getDirection(Connection conn) {
        return conn.isIncoming()
                ? VOICE_CALL_SESSION__DIRECTION__CALL_DIRECTION_MT
                : VOICE_CALL_SESSION__DIRECTION__CALL_DIRECTION_MO;
    }

    private static int getBearer(Connection conn) {
        int phoneType = conn.getPhoneType();
        switch (phoneType) {
            case PhoneConstants.PHONE_TYPE_GSM:
            case PhoneConstants.PHONE_TYPE_CDMA:
                return VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_CS;
            case PhoneConstants.PHONE_TYPE_IMS:
                return VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_IMS;
            default:
                loge("getBearer: unknown phoneType=%d", phoneType);
                return VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_UNKNOWN;
        }
    }

    private @NetworkType int getRat(@Nullable ServiceState state) {
        if (state == null) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
        boolean isWifiCall =
                mPhone.getImsPhone() != null
                        && mPhone.getImsPhone().isWifiCallingEnabled()
                        && state.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_IWLAN;
        return isWifiCall ? TelephonyManager.NETWORK_TYPE_IWLAN : state.getVoiceNetworkType();
    }

    /** Returns the signal strength. */
    private int getSignalStrength(@NetworkType int rat) {
        if (rat == TelephonyManager.NETWORK_TYPE_IWLAN) {
            return getSignalStrengthWifi();
        } else {
            return getSignalStrengthCellular();
        }
    }

    /** Returns the signal strength of WiFi. */
    private int getSignalStrengthWifi() {
        WifiManager wifiManager =
                (WifiManager) mPhone.getContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int result = VOICE_CALL_SESSION__SIGNAL_STRENGTH_AT_END__SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        if (wifiInfo != null) {
            int level = wifiManager.calculateSignalLevel(wifiInfo.getRssi());
            int max = wifiManager.getMaxSignalLevel();
            // Scale result into 0 to 4 range.
            result = VOICE_CALL_SESSION__SIGNAL_STRENGTH_AT_END__SIGNAL_STRENGTH_GREAT
                    * level / max;
            logd("WiFi level: " + result + " (" + level + "/" + max + ")");
        }
        return result;
    }

    /** Returns the signal strength of cellular RAT. */
    private int getSignalStrengthCellular() {
        return mPhone.getSignalStrength().getLevel();
    }

    /** Resets the list of codecs used for the connection with only the codec currently in use. */
    private void resetCodecList(Connection conn) {
        int id = getConnectionId(conn);
        LongSparseArray<Integer> codecUsage = mCodecUsage.get(id);
        if (codecUsage != null) {
            int lastCodec = codecUsage.valueAt(codecUsage.size() - 1);
            LongSparseArray<Integer> arr = new LongSparseArray<>();
            arr.append(getTimeMillis(), lastCodec);
            mCodecUsage.put(id, arr);
        }
    }

    /** Returns the main codec quality used during the call. */
    private int finalizeMainCodecQuality(int connectionId) {
        // Retrieve information about codec usage for this call and remove it from main array.
        if (!mCodecUsage.contains(connectionId)) {
            return VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_UNKNOWN;
        }
        LongSparseArray<Integer> codecUsage = mCodecUsage.get(connectionId);
        mCodecUsage.delete(connectionId);

        // Append fake entry at the end, to facilitate the calculation of time for each codec.
        codecUsage.put(getTimeMillis(), AudioCodec.AUDIO_CODEC_UNKNOWN);

        // Calculate array with time for each quality
        int totalTime = 0;
        long[] timePerQuality = new long[CODEC_QUALITY_COUNT];
        for (int i = 0; i < codecUsage.size() - 1; i++) {
            long time = codecUsage.keyAt(i + 1) - codecUsage.keyAt(i);
            int quality = getCodecQuality(codecUsage.valueAt(i));
            timePerQuality[quality] += time;
            totalTime += time;
        }
        logd("Time per codec quality = " + Arrays.toString(timePerQuality));

        // We calculate 70% duration of the call as the threshold for the main audio codec quality
        // and iterate on all codec qualities. As soon as the sum of codec duration is greater than
        // the threshold, we have identified the main codec quality.
        long timeAtMinimumQuality = 0;
        long timeThreshold = totalTime * MAIN_CODEC_QUALITY_THRESHOLD / 100;
        for (int i = CODEC_QUALITY_COUNT - 1; i >= 0; i--) {
            timeAtMinimumQuality += timePerQuality[i];
            if (timeAtMinimumQuality >= timeThreshold) {
                return i;
            }
        }
        return VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_UNKNOWN;
    }

    private int getCodecQuality(int codec) {
        switch(codec) {
            case AudioCodec.AUDIO_CODEC_AMR:
            case AudioCodec.AUDIO_CODEC_QCELP13K:
            case AudioCodec.AUDIO_CODEC_EVRC:
            case AudioCodec.AUDIO_CODEC_EVRC_B:
            case AudioCodec.AUDIO_CODEC_EVRC_NW:
            case AudioCodec.AUDIO_CODEC_GSM_EFR:
            case AudioCodec.AUDIO_CODEC_GSM_FR:
            case AudioCodec.AUDIO_CODEC_GSM_HR:
            case AudioCodec.AUDIO_CODEC_G711U:
            case AudioCodec.AUDIO_CODEC_G723:
            case AudioCodec.AUDIO_CODEC_G711A:
            case AudioCodec.AUDIO_CODEC_G722:
            case AudioCodec.AUDIO_CODEC_G711AB:
            case AudioCodec.AUDIO_CODEC_G729:
            case AudioCodec.AUDIO_CODEC_EVS_NB:
                return VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_NARROWBAND;
            case AudioCodec.AUDIO_CODEC_AMR_WB:
            case AudioCodec.AUDIO_CODEC_EVS_WB:
            case AudioCodec.AUDIO_CODEC_EVRC_WB:
                return VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_WIDEBAND;
            case AudioCodec.AUDIO_CODEC_EVS_SWB:
                return VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_SUPER_WIDEBAND;
            case AudioCodec.AUDIO_CODEC_EVS_FB:
                return VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_FULLBAND;
            default:
                return VOICE_CALL_SESSION__MAIN_CODEC_QUALITY__CODEC_QUALITY_UNKNOWN;
        }
    }

    private static boolean isSetupFinished(@Nullable Call call) {
        // NOTE: when setup is finished for MO calls, it is not successful yet.
        if (call != null) {
            switch (call.getState()) {
                case ACTIVE: // MT setup: accepted to ACTIVE
                case ALERTING: // MO setup: dial to ALERTING
                    return true;
                default: // do nothing
            }
        }
        return false;
    }

    private static int audioQualityToCodec(int bearer, int audioQuality) {
        switch (bearer) {
            case VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_CS:
                return CS_CODEC_MAP.get(audioQuality, AudioCodec.AUDIO_CODEC_UNKNOWN);
            case VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_IMS:
                return IMS_CODEC_MAP.get(audioQuality, AudioCodec.AUDIO_CODEC_UNKNOWN);
            default:
                loge("audioQualityToCodec: unknown bearer %d", bearer);
                return AudioCodec.AUDIO_CODEC_UNKNOWN;
        }
    }

    private static int classifySetupDuration(int durationMillis) {
        // keys in CALL_SETUP_DURATION_MAP are upper bounds in ascending order
        for (int i = 0; i < CALL_SETUP_DURATION_MAP.size(); i++) {
            if (durationMillis < CALL_SETUP_DURATION_MAP.keyAt(i)) {
                return CALL_SETUP_DURATION_MAP.valueAt(i);
            }
        }
        return VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_EXTREMELY_SLOW;
    }

    /**
     * Generates an ID for each connection, which should be the same for IMS and CS connections
     * involved in the same SRVCC.
     *
     * <p>Among the fields copied from ImsPhoneConnection to GsmCdmaConnection during SRVCC, the
     * Connection's create time seems to be the best choice for ID (assuming no multiple calls in a
     * millisecond). The 64-bit time is truncated to 32-bit so it can be used as an index in various
     * data structures, which is good for calls shorter than 49 days.
     */
    private static int getConnectionId(Connection conn) {
        return conn == null ? 0 : (int) conn.getCreateTime();
    }

    @VisibleForTesting
    protected long getTimeMillis() {
        return SystemClock.elapsedRealtime();
    }

    private static void logd(String format, Object... args) {
        Rlog.d(TAG, String.format(format, args));
    }

    private static void loge(String format, Object... args) {
        Rlog.e(TAG, String.format(format, args));
    }

    private static SparseIntArray buildGsmCdmaCodecMap() {
        SparseIntArray map = new SparseIntArray();
        map.put(DriverCall.AUDIO_QUALITY_AMR, AudioCodec.AUDIO_CODEC_AMR);
        map.put(DriverCall.AUDIO_QUALITY_AMR_WB, AudioCodec.AUDIO_CODEC_AMR_WB);
        map.put(DriverCall.AUDIO_QUALITY_GSM_EFR, AudioCodec.AUDIO_CODEC_GSM_EFR);
        map.put(DriverCall.AUDIO_QUALITY_GSM_FR, AudioCodec.AUDIO_CODEC_GSM_FR);
        map.put(DriverCall.AUDIO_QUALITY_GSM_HR, AudioCodec.AUDIO_CODEC_GSM_HR);
        map.put(DriverCall.AUDIO_QUALITY_EVRC, AudioCodec.AUDIO_CODEC_EVRC);
        map.put(DriverCall.AUDIO_QUALITY_EVRC_B, AudioCodec.AUDIO_CODEC_EVRC_B);
        map.put(DriverCall.AUDIO_QUALITY_EVRC_WB, AudioCodec.AUDIO_CODEC_EVRC_WB);
        map.put(DriverCall.AUDIO_QUALITY_EVRC_NW, AudioCodec.AUDIO_CODEC_EVRC_NW);
        return map;
    }

    private static SparseIntArray buildImsCodecMap() {
        SparseIntArray map = new SparseIntArray();
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_AMR, AudioCodec.AUDIO_CODEC_AMR);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_AMR_WB, AudioCodec.AUDIO_CODEC_AMR_WB);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_QCELP13K, AudioCodec.AUDIO_CODEC_QCELP13K);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_EVRC, AudioCodec.AUDIO_CODEC_EVRC);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_B, AudioCodec.AUDIO_CODEC_EVRC_B);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_WB, AudioCodec.AUDIO_CODEC_EVRC_WB);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_NW, AudioCodec.AUDIO_CODEC_EVRC_NW);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_GSM_EFR, AudioCodec.AUDIO_CODEC_GSM_EFR);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_GSM_FR, AudioCodec.AUDIO_CODEC_GSM_FR);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_GSM_HR, AudioCodec.AUDIO_CODEC_GSM_HR);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_G711U, AudioCodec.AUDIO_CODEC_G711U);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_G723, AudioCodec.AUDIO_CODEC_G723);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_G711A, AudioCodec.AUDIO_CODEC_G711A);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_G722, AudioCodec.AUDIO_CODEC_G722);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_G711AB, AudioCodec.AUDIO_CODEC_G711AB);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_G729, AudioCodec.AUDIO_CODEC_G729);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_EVS_NB, AudioCodec.AUDIO_CODEC_EVS_NB);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_EVS_WB, AudioCodec.AUDIO_CODEC_EVS_WB);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_EVS_SWB, AudioCodec.AUDIO_CODEC_EVS_SWB);
        map.put(ImsStreamMediaProfile.AUDIO_QUALITY_EVS_FB, AudioCodec.AUDIO_CODEC_EVS_FB);
        return map;
    }

    private static SparseIntArray buildCallSetupDurationMap() {
        SparseIntArray map = new SparseIntArray();

        map.put(
                CALL_SETUP_DURATION_UNKNOWN,
                VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_UNKNOWN);
        map.put(
                CALL_SETUP_DURATION_EXTREMELY_FAST,
                VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_EXTREMELY_FAST);
        map.put(
                CALL_SETUP_DURATION_ULTRA_FAST,
                VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_ULTRA_FAST);
        map.put(
                CALL_SETUP_DURATION_VERY_FAST,
                VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_VERY_FAST);
        map.put(
                CALL_SETUP_DURATION_FAST,
                VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_FAST);
        map.put(
                CALL_SETUP_DURATION_NORMAL,
                VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_NORMAL);
        map.put(
                CALL_SETUP_DURATION_SLOW,
                VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_SLOW);
        map.put(
                CALL_SETUP_DURATION_VERY_SLOW,
                VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_VERY_SLOW);
        map.put(
                CALL_SETUP_DURATION_ULTRA_SLOW,
                VOICE_CALL_SESSION__SETUP_DURATION__CALL_SETUP_DURATION_ULTRA_SLOW);
        // anything above would be CALL_SETUP_DURATION_EXTREMELY_SLOW

        return map;
    }
}

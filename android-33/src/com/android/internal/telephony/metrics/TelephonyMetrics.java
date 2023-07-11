/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import static com.android.internal.telephony.InboundSmsHandler.SOURCE_INJECTED_FROM_IMS;
import static com.android.internal.telephony.InboundSmsHandler.SOURCE_NOT_INJECTED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ANSWER;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_SEND_SMS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DEACTIVATE_DATA_CALL;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DIAL;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_HANGUP;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_IMS_SEND_SMS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SEND_SMS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SEND_SMS_EXPECT_MORE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SETUP_DATA_CALL;
import static com.android.internal.telephony.data.LinkBandwidthEstimator.NUM_SIGNAL_LEVEL;
import static com.android.internal.telephony.nano.TelephonyProto.PdpType.PDP_TYPE_IP;
import static com.android.internal.telephony.nano.TelephonyProto.PdpType.PDP_TYPE_IPV4V6;
import static com.android.internal.telephony.nano.TelephonyProto.PdpType.PDP_TYPE_IPV6;
import static com.android.internal.telephony.nano.TelephonyProto.PdpType.PDP_TYPE_NON_IP;
import static com.android.internal.telephony.nano.TelephonyProto.PdpType.PDP_TYPE_PPP;
import static com.android.internal.telephony.nano.TelephonyProto.PdpType.PDP_TYPE_UNSTRUCTURED;
import static com.android.internal.telephony.nano.TelephonyProto.PdpType.PDP_UNKNOWN;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.os.BatteryStatsManager;
import android.os.Build;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Telephony.Sms.Intents;
import android.telephony.AccessNetworkConstants;
import android.telephony.Annotation.RadioPowerState;
import android.telephony.CallQuality;
import android.telephony.DisconnectCause;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyHistogram;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.PrefNetworkMode;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataService;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsStreamMediaProfile;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.SparseArray;

import com.android.internal.telephony.CarrierResolver;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.GsmCdmaConnection;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.SmsController;
import com.android.internal.telephony.SmsResponse;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.data.LinkBandwidthEstimator;
import com.android.internal.telephony.emergency.EmergencyNumberTracker;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.nano.TelephonyProto;
import com.android.internal.telephony.nano.TelephonyProto.ActiveSubscriptionInfo;
import com.android.internal.telephony.nano.TelephonyProto.BandwidthEstimatorStats;
import com.android.internal.telephony.nano.TelephonyProto.EmergencyNumberInfo;
import com.android.internal.telephony.nano.TelephonyProto.ImsCapabilities;
import com.android.internal.telephony.nano.TelephonyProto.ImsConnectionState;
import com.android.internal.telephony.nano.TelephonyProto.ModemPowerStats;
import com.android.internal.telephony.nano.TelephonyProto.RilDataCall;
import com.android.internal.telephony.nano.TelephonyProto.SimState;
import com.android.internal.telephony.nano.TelephonyProto.SmsSession;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.CallState;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall.Type;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.CarrierIdMatching;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.CarrierIdMatchingResult;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.CarrierKeyChange;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.DataSwitch;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.ModemRestart;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.NetworkCapabilitiesInfo;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.OnDemandDataSwitch;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.RadioState;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.RilDeactivateDataCall;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.RilDeactivateDataCall.DeactivateReason;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.RilSetupDataCall;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.RilSetupDataCallResponse;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.RilSetupDataCallResponse.RilDataCallFailCause;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyLog;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyServiceState;
import com.android.internal.telephony.nano.TelephonyProto.TelephonySettings;
import com.android.internal.telephony.nano.TelephonyProto.TimeInterval;
import com.android.internal.telephony.protobuf.nano.MessageNano;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Telephony metrics holds all metrics events and convert it into telephony proto buf.
 * @hide
 */
public class TelephonyMetrics {

    private static final String TAG = TelephonyMetrics.class.getSimpleName();

    private static final boolean DBG = true;
    private static final boolean VDBG = false; // STOPSHIP if true

    /** Maximum telephony events stored */
    private static final int MAX_TELEPHONY_EVENTS = 1000;

    /** Maximum call sessions stored */
    private static final int MAX_COMPLETED_CALL_SESSIONS = 50;

    /** Maximum sms sessions stored */
    private static final int MAX_COMPLETED_SMS_SESSIONS = 500;

    /** For reducing the timing precision for privacy purposes */
    private static final int SESSION_START_PRECISION_MINUTES = 5;

    /** The TelephonyMetrics singleton instance */
    private static TelephonyMetrics sInstance;

    /** Telephony events */
    private final Deque<TelephonyEvent> mTelephonyEvents = new ArrayDeque<>();

    /**
     * In progress call sessions. Note that each phone can only have up to 1 in progress call
     * session (might contains multiple calls). Having a sparse array in case we need to support
     * DSDA in the future.
     */
    private final SparseArray<InProgressCallSession> mInProgressCallSessions = new SparseArray<>();

    /** The completed call sessions */
    private final Deque<TelephonyCallSession> mCompletedCallSessions = new ArrayDeque<>();

    /** The in-progress SMS sessions. When finished, it will be moved into the completed sessions */
    private final SparseArray<InProgressSmsSession> mInProgressSmsSessions = new SparseArray<>();

    /** The completed SMS sessions */
    private final Deque<SmsSession> mCompletedSmsSessions = new ArrayDeque<>();

    /** Last service state. This is for injecting the base of a new log or a new call/sms session */
    private final SparseArray<TelephonyServiceState> mLastServiceState = new SparseArray<>();

    /**
     * Last ims capabilities. This is for injecting the base of a new log or a new call/sms session
     */
    private final SparseArray<ImsCapabilities> mLastImsCapabilities = new SparseArray<>();

    /**
     * Last IMS connection state. This is for injecting the base of a new log or a new call/sms
     * session
     */
    private final SparseArray<ImsConnectionState> mLastImsConnectionState = new SparseArray<>();

    /** Last settings state. This is for deduping same settings event logged. */
    private final SparseArray<TelephonySettings> mLastSettings = new SparseArray<>();

    /** Last sim state, indexed by phone id. */
    private final SparseArray<Integer> mLastSimState = new SparseArray<>();

    /** Last radio state, indexed by phone id. */
    private final SparseArray<Integer> mLastRadioState = new SparseArray<>();

    /** Last active subscription information, indexed by phone id. */
    private final SparseArray<ActiveSubscriptionInfo> mLastActiveSubscriptionInfos =
            new SparseArray<>();

    /**
     * The last modem state represent by a bitmap, the i-th bit(LSB) indicates the i-th modem
     * state(0 - disabled, 1 - enabled).
     *
     * TODO: initialize the enabled modem bitmap when it's possible to get the modem state.
     */
    private int mLastEnabledModemBitmap = (1 << TelephonyManager.getDefault().getPhoneCount()) - 1;

    /** Last carrier id matching. */
    private final SparseArray<CarrierIdMatching> mLastCarrierId = new SparseArray<>();

    /** Last NetworkCapabilitiesInfo, indexed by phone id. */
    private final SparseArray<NetworkCapabilitiesInfo> mLastNetworkCapabilitiesInfos =
            new SparseArray<>();

    /** Last RilDataCall Events (indexed by cid), indexed by phone id */
    private final SparseArray<SparseArray<RilDataCall>> mLastRilDataCallEvents =
            new SparseArray<>();

    /** List of Tx and Rx Bandwidth estimation stats maps */
    private final List<Map<String, BwEstimationStats>> mBwEstStatsMapList = new ArrayList<>(
            Arrays.asList(new ArrayMap<>(), new ArrayMap<>()));

    /** The start system time of the TelephonyLog in milliseconds*/
    private long mStartSystemTimeMs;

    /** The start elapsed time of the TelephonyLog in milliseconds*/
    private long mStartElapsedTimeMs;

    /** Indicating if some of the telephony events are dropped in this log */
    private boolean mTelephonyEventsDropped = false;

    private Context mContext;

    public TelephonyMetrics() {
        mStartSystemTimeMs = System.currentTimeMillis();
        mStartElapsedTimeMs = SystemClock.elapsedRealtime();
    }

    /**
     * Get the singleton instance of telephony metrics.
     *
     * @return The instance
     */
    public synchronized static TelephonyMetrics getInstance() {
        if (sInstance == null) {
            sInstance = new TelephonyMetrics();
        }

        return sInstance;
    }

    /**
     * Set the context for telephony metrics.
     *
     * @param context Context
     * @hide
     */
    public void setContext(Context context) {
        mContext = context;
    }

    /**
     * Dump the state of various objects, add calls to other objects as desired.
     *
     * @param fd File descriptor
     * @param pw Print writer
     * @param args Arguments
     */
    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args != null && args.length > 0) {
            boolean reset = true;
            if (args.length > 1 && "--keep".equals(args[1])) {
                reset = false;
            }

            switch (args[0]) {
                case "--metrics":
                    printAllMetrics(pw);
                    break;
                case "--metricsproto":
                    pw.println(convertProtoToBase64String(buildProto()));
                    pw.println(RcsStats.getInstance().buildLog());
                    if (reset) {
                        reset();
                    }
                    break;
                case "--metricsprototext":
                    pw.println(buildProto().toString());
                    pw.println(RcsStats.getInstance().buildProto().toString());
                    break;
            }
        }
    }

    private void logv(String log) {
        if (VDBG) {
            Rlog.v(TAG, log);
        }
    }

    /**
     * Convert the telephony event to string
     *
     * @param event The event in integer
     * @return The event in string
     */
    private static String telephonyEventToString(int event) {
        switch (event) {
            case TelephonyEvent.Type.UNKNOWN:
                return "UNKNOWN";
            case TelephonyEvent.Type.SETTINGS_CHANGED:
                return "SETTINGS_CHANGED";
            case TelephonyEvent.Type.RIL_SERVICE_STATE_CHANGED:
                return "RIL_SERVICE_STATE_CHANGED";
            case TelephonyEvent.Type.IMS_CONNECTION_STATE_CHANGED:
                return "IMS_CONNECTION_STATE_CHANGED";
            case TelephonyEvent.Type.IMS_CAPABILITIES_CHANGED:
                return "IMS_CAPABILITIES_CHANGED";
            case TelephonyEvent.Type.DATA_CALL_SETUP:
                return "DATA_CALL_SETUP";
            case TelephonyEvent.Type.DATA_CALL_SETUP_RESPONSE:
                return "DATA_CALL_SETUP_RESPONSE";
            case TelephonyEvent.Type.DATA_CALL_LIST_CHANGED:
                return "DATA_CALL_LIST_CHANGED";
            case TelephonyEvent.Type.DATA_CALL_DEACTIVATE:
                return "DATA_CALL_DEACTIVATE";
            case TelephonyEvent.Type.DATA_CALL_DEACTIVATE_RESPONSE:
                return "DATA_CALL_DEACTIVATE_RESPONSE";
            case TelephonyEvent.Type.DATA_STALL_ACTION:
                return "DATA_STALL_ACTION";
            case TelephonyEvent.Type.MODEM_RESTART:
                return "MODEM_RESTART";
            case TelephonyEvent.Type.CARRIER_ID_MATCHING:
                return "CARRIER_ID_MATCHING";
            case TelephonyEvent.Type.NITZ_TIME:
                return "NITZ_TIME";
            case TelephonyEvent.Type.EMERGENCY_NUMBER_REPORT:
                return "EMERGENCY_NUMBER_REPORT";
            case TelephonyEvent.Type.NETWORK_CAPABILITIES_CHANGED:
                return "NETWORK_CAPABILITIES_CHANGED";
            default:
                return Integer.toString(event);
        }
    }

    /**
     * Convert the call session event into string
     *
     * @param event The event in integer
     * @return The event in String
     */
    private static String callSessionEventToString(int event) {
        switch (event) {
            case TelephonyCallSession.Event.Type.EVENT_UNKNOWN:
                return "EVENT_UNKNOWN";
            case TelephonyCallSession.Event.Type.SETTINGS_CHANGED:
                return "SETTINGS_CHANGED";
            case TelephonyCallSession.Event.Type.RIL_SERVICE_STATE_CHANGED:
                return "RIL_SERVICE_STATE_CHANGED";
            case TelephonyCallSession.Event.Type.IMS_CONNECTION_STATE_CHANGED:
                return "IMS_CONNECTION_STATE_CHANGED";
            case TelephonyCallSession.Event.Type.IMS_CAPABILITIES_CHANGED:
                return "IMS_CAPABILITIES_CHANGED";
            case TelephonyCallSession.Event.Type.DATA_CALL_LIST_CHANGED:
                return "DATA_CALL_LIST_CHANGED";
            case TelephonyCallSession.Event.Type.RIL_REQUEST:
                return "RIL_REQUEST";
            case TelephonyCallSession.Event.Type.RIL_RESPONSE:
                return "RIL_RESPONSE";
            case TelephonyCallSession.Event.Type.RIL_CALL_RING:
                return "RIL_CALL_RING";
            case TelephonyCallSession.Event.Type.RIL_CALL_SRVCC:
                return "RIL_CALL_SRVCC";
            case TelephonyCallSession.Event.Type.RIL_CALL_LIST_CHANGED:
                return "RIL_CALL_LIST_CHANGED";
            case TelephonyCallSession.Event.Type.IMS_COMMAND:
                return "IMS_COMMAND";
            case TelephonyCallSession.Event.Type.IMS_COMMAND_RECEIVED:
                return "IMS_COMMAND_RECEIVED";
            case TelephonyCallSession.Event.Type.IMS_COMMAND_FAILED:
                return "IMS_COMMAND_FAILED";
            case TelephonyCallSession.Event.Type.IMS_COMMAND_COMPLETE:
                return "IMS_COMMAND_COMPLETE";
            case TelephonyCallSession.Event.Type.IMS_CALL_RECEIVE:
                return "IMS_CALL_RECEIVE";
            case TelephonyCallSession.Event.Type.IMS_CALL_STATE_CHANGED:
                return "IMS_CALL_STATE_CHANGED";
            case TelephonyCallSession.Event.Type.IMS_CALL_TERMINATED:
                return "IMS_CALL_TERMINATED";
            case TelephonyCallSession.Event.Type.IMS_CALL_HANDOVER:
                return "IMS_CALL_HANDOVER";
            case TelephonyCallSession.Event.Type.IMS_CALL_HANDOVER_FAILED:
                return "IMS_CALL_HANDOVER_FAILED";
            case TelephonyCallSession.Event.Type.PHONE_STATE_CHANGED:
                return "PHONE_STATE_CHANGED";
            case TelephonyCallSession.Event.Type.NITZ_TIME:
                return "NITZ_TIME";
            case TelephonyCallSession.Event.Type.AUDIO_CODEC:
                return "AUDIO_CODEC";
            default:
                return Integer.toString(event);
        }
    }

    /**
     * Convert the SMS session event into string
     * @param event The event in integer
     * @return The event in String
     */
    private static String smsSessionEventToString(int event) {
        switch (event) {
            case SmsSession.Event.Type.EVENT_UNKNOWN:
                return "EVENT_UNKNOWN";
            case SmsSession.Event.Type.SETTINGS_CHANGED:
                return "SETTINGS_CHANGED";
            case SmsSession.Event.Type.RIL_SERVICE_STATE_CHANGED:
                return "RIL_SERVICE_STATE_CHANGED";
            case SmsSession.Event.Type.IMS_CONNECTION_STATE_CHANGED:
                return "IMS_CONNECTION_STATE_CHANGED";
            case SmsSession.Event.Type.IMS_CAPABILITIES_CHANGED:
                return "IMS_CAPABILITIES_CHANGED";
            case SmsSession.Event.Type.DATA_CALL_LIST_CHANGED:
                return "DATA_CALL_LIST_CHANGED";
            case SmsSession.Event.Type.SMS_SEND:
                return "SMS_SEND";
            case SmsSession.Event.Type.SMS_SEND_RESULT:
                return "SMS_SEND_RESULT";
            case SmsSession.Event.Type.SMS_RECEIVED:
                return "SMS_RECEIVED";
            case SmsSession.Event.Type.INCOMPLETE_SMS_RECEIVED:
                return "INCOMPLETE_SMS_RECEIVED";
            default:
                return Integer.toString(event);
        }
    }

    /**
     * Print all metrics data for debugging purposes
     *
     * @param rawWriter Print writer
     */
    private synchronized void printAllMetrics(PrintWriter rawWriter) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(rawWriter, "  ");

        pw.println("Telephony metrics proto:");
        pw.println("------------------------------------------");
        pw.println("Telephony events:");
        pw.increaseIndent();
        for (TelephonyEvent event : mTelephonyEvents) {
            pw.print(event.timestampMillis);
            pw.print(" [");
            pw.print(event.phoneId);
            pw.print("] ");

            pw.print("T=");
            if (event.type == TelephonyEvent.Type.RIL_SERVICE_STATE_CHANGED) {
                pw.print(telephonyEventToString(event.type)
                        + "(" + "Data RAT " + event.serviceState.dataRat
                        + " Voice RAT " + event.serviceState.voiceRat
                        + " Channel Number " + event.serviceState.channelNumber
                        + " NR Frequency Range " + event.serviceState.nrFrequencyRange
                        + " NR State " + event.serviceState.nrState
                        + ")");
                for (int i = 0; i < event.serviceState.networkRegistrationInfo.length; i++) {
                    pw.print("reg info: domain="
                            + event.serviceState.networkRegistrationInfo[i].domain
                            + ", rat=" + event.serviceState.networkRegistrationInfo[i].rat);
                }
            } else {
                pw.print(telephonyEventToString(event.type));
            }

            pw.println("");
        }

        pw.decreaseIndent();
        pw.println("Call sessions:");
        pw.increaseIndent();

        for (TelephonyCallSession callSession : mCompletedCallSessions) {
            pw.print("Start time in minutes: " + callSession.startTimeMinutes);
            pw.print(", phone: " + callSession.phoneId);
            if (callSession.eventsDropped) {
                pw.println(", events dropped: " + callSession.eventsDropped);
            } else {
                pw.println("");
            }

            pw.println("Events: ");
            pw.increaseIndent();
            for (TelephonyCallSession.Event event : callSession.events) {
                pw.print(event.delay);
                pw.print(" T=");
                if (event.type == TelephonyCallSession.Event.Type.RIL_SERVICE_STATE_CHANGED) {
                    pw.println(callSessionEventToString(event.type)
                            + "(" + "Data RAT " + event.serviceState.dataRat
                            + " Voice RAT " + event.serviceState.voiceRat
                            + " Channel Number " + event.serviceState.channelNumber
                            + " NR Frequency Range " + event.serviceState.nrFrequencyRange
                            + " NR State " + event.serviceState.nrState
                            + ")");
                } else if (event.type == TelephonyCallSession.Event.Type.RIL_CALL_LIST_CHANGED) {
                    pw.println(callSessionEventToString(event.type));
                    pw.increaseIndent();
                    for (RilCall call : event.calls) {
                        pw.println(call.index + ". Type = " + call.type + " State = "
                                + call.state + " End Reason " + call.callEndReason
                                + " Precise Disconnect Cause " + call.preciseDisconnectCause
                                + " isMultiparty = " + call.isMultiparty);
                    }
                    pw.decreaseIndent();
                } else if (event.type == TelephonyCallSession.Event.Type.AUDIO_CODEC) {
                    pw.println(callSessionEventToString(event.type)
                            + "(" + event.audioCodec + ")");
                } else {
                    pw.println(callSessionEventToString(event.type));
                }
            }
            pw.decreaseIndent();
        }

        pw.decreaseIndent();
        pw.println("Sms sessions:");
        pw.increaseIndent();

        int count = 0;
        for (SmsSession smsSession : mCompletedSmsSessions) {
            count++;
            pw.print("[" + count + "] Start time in minutes: "
                    + smsSession.startTimeMinutes);
            pw.print(", phone: " + smsSession.phoneId);
            if (smsSession.eventsDropped) {
                pw.println(", events dropped: " + smsSession.eventsDropped);
            } else {
                pw.println("");
            }
            pw.println("Events: ");
            pw.increaseIndent();
            for (SmsSession.Event event : smsSession.events) {
                pw.print(event.delay);
                pw.print(" T=");
                if (event.type == SmsSession.Event.Type.RIL_SERVICE_STATE_CHANGED) {
                    pw.println(smsSessionEventToString(event.type)
                            + "(" + "Data RAT " + event.serviceState.dataRat
                            + " Voice RAT " + event.serviceState.voiceRat
                            + " Channel Number " + event.serviceState.channelNumber
                            + " NR Frequency Range " + event.serviceState.nrFrequencyRange
                            + " NR State " + event.serviceState.nrState
                            + ")");
                } else if (event.type == SmsSession.Event.Type.SMS_RECEIVED) {
                    pw.println(smsSessionEventToString(event.type));
                    pw.increaseIndent();
                    switch (event.smsType) {
                        case SmsSession.Event.SmsType.SMS_TYPE_SMS_PP:
                            pw.println("Type: SMS-PP");
                            break;
                        case SmsSession.Event.SmsType.SMS_TYPE_VOICEMAIL_INDICATION:
                            pw.println("Type: Voicemail indication");
                            break;
                        case SmsSession.Event.SmsType.SMS_TYPE_ZERO:
                            pw.println("Type: zero");
                            break;
                        case SmsSession.Event.SmsType.SMS_TYPE_WAP_PUSH:
                            pw.println("Type: WAP PUSH");
                            break;
                        default:
                            break;
                    }
                    if (event.errorCode != SmsManager.RESULT_ERROR_NONE) {
                        pw.println("E=" + event.errorCode);
                    }
                    pw.decreaseIndent();
                } else if (event.type == SmsSession.Event.Type.SMS_SEND
                        || event.type == SmsSession.Event.Type.SMS_SEND_RESULT) {
                    pw.println(smsSessionEventToString(event.type));
                    pw.increaseIndent();
                    pw.println("ReqId=" + event.rilRequestId);
                    pw.println("E=" + event.errorCode);
                    pw.println("RilE=" + event.error);
                    pw.println("ImsE=" + event.imsError);
                    pw.decreaseIndent();
                } else if (event.type == SmsSession.Event.Type.INCOMPLETE_SMS_RECEIVED) {
                    pw.println(smsSessionEventToString(event.type));
                    pw.increaseIndent();
                    pw.println("Received: " + event.incompleteSms.receivedParts + "/"
                            + event.incompleteSms.totalParts);
                    pw.decreaseIndent();
                }
            }
            pw.decreaseIndent();
        }

        pw.decreaseIndent();
        pw.println("Modem power stats:");
        pw.increaseIndent();

        BatteryStatsManager batteryStatsManager = mContext == null ? null :
                (BatteryStatsManager) mContext.getSystemService(Context.BATTERY_STATS_SERVICE);
        ModemPowerStats s = new ModemPowerMetrics(batteryStatsManager).buildProto();

        pw.println("Power log duration (battery time) (ms): " + s.loggingDurationMs);
        pw.println("Energy consumed by modem (mAh): " + s.energyConsumedMah);
        pw.println("Number of packets sent (tx): " + s.numPacketsTx);
        pw.println("Number of bytes sent (tx): " + s.numBytesTx);
        pw.println("Number of packets received (rx): " + s.numPacketsRx);
        pw.println("Number of bytes received (rx): " + s.numBytesRx);
        pw.println("Amount of time kernel is active because of cellular data (ms): "
                + s.cellularKernelActiveTimeMs);
        pw.println("Amount of time spent in very poor rx signal level (ms): "
                + s.timeInVeryPoorRxSignalLevelMs);
        pw.println("Amount of time modem is in sleep (ms): " + s.sleepTimeMs);
        pw.println("Amount of time modem is in idle (ms): " + s.idleTimeMs);
        pw.println("Amount of time modem is in rx (ms): " + s.rxTimeMs);
        pw.println("Amount of time modem is in tx (ms): " + Arrays.toString(s.txTimeMs));
        pw.println("Amount of time phone spent in various Radio Access Technologies (ms): "
                + Arrays.toString(s.timeInRatMs));
        pw.println("Amount of time phone spent in various cellular "
                + "rx signal strength levels (ms): "
                + Arrays.toString(s.timeInRxSignalStrengthLevelMs));
        pw.println("Energy consumed across measured modem rails (mAh): "
                + new DecimalFormat("#.##").format(s.monitoredRailEnergyConsumedMah));
        pw.decreaseIndent();
        pw.println("Hardware Version: " + SystemProperties.get("ro.boot.revision", ""));

        pw.decreaseIndent();
        pw.println("LinkBandwidthEstimator stats:");
        pw.increaseIndent();

        pw.println("Tx");
        for (BwEstimationStats stats : mBwEstStatsMapList.get(0).values()) {
            pw.println(stats.toString());
        }

        pw.println("Rx");
        for (BwEstimationStats stats : mBwEstStatsMapList.get(1).values()) {
            pw.println(stats.toString());
        }

        RcsStats.getInstance().printAllMetrics(rawWriter);
    }

    /**
     * Convert the telephony proto into Base-64 encoded string
     *
     * @param proto Telephony proto
     * @return Encoded string
     */
    private static String convertProtoToBase64String(TelephonyLog proto) {
        return Base64.encodeToString(
                TelephonyProto.TelephonyLog.toByteArray(proto), Base64.DEFAULT);
    }

    /**
     * Reset all events and sessions
     */
    private synchronized void reset() {
        mTelephonyEvents.clear();
        mCompletedCallSessions.clear();
        mCompletedSmsSessions.clear();
        mBwEstStatsMapList.get(0).clear();
        mBwEstStatsMapList.get(1).clear();

        mTelephonyEventsDropped = false;

        mStartSystemTimeMs = System.currentTimeMillis();
        mStartElapsedTimeMs = SystemClock.elapsedRealtime();

        // Insert the last known sim state, enabled modem bitmap, active subscription info,
        // service state, ims capabilities, ims connection states, carrier id and Data call
        // events as the base.
        // Sim state, modem bitmap and active subscription info events are logged before
        // other events.
        addTelephonyEvent(new TelephonyEventBuilder(mStartElapsedTimeMs, -1 /* phoneId */)
                .setSimStateChange(mLastSimState).build());

        addTelephonyEvent(new TelephonyEventBuilder(mStartElapsedTimeMs, -1 /* phoneId */)
                .setEnabledModemBitmap(mLastEnabledModemBitmap).build());

        for (int i = 0; i < mLastActiveSubscriptionInfos.size(); i++) {
          final int key = mLastActiveSubscriptionInfos.keyAt(i);
          TelephonyEvent event = new TelephonyEventBuilder(mStartElapsedTimeMs, key)
                  .setActiveSubscriptionInfoChange(mLastActiveSubscriptionInfos.get(key)).build();
          addTelephonyEvent(event);
        }

        for (int i = 0; i < mLastServiceState.size(); i++) {
            final int key = mLastServiceState.keyAt(i);

            TelephonyEvent event = new TelephonyEventBuilder(mStartElapsedTimeMs, key)
                    .setServiceState(mLastServiceState.get(key)).build();
            addTelephonyEvent(event);
        }

        for (int i = 0; i < mLastImsCapabilities.size(); i++) {
            final int key = mLastImsCapabilities.keyAt(i);

            TelephonyEvent event = new TelephonyEventBuilder(mStartElapsedTimeMs, key)
                    .setImsCapabilities(mLastImsCapabilities.get(key)).build();
            addTelephonyEvent(event);
        }

        for (int i = 0; i < mLastImsConnectionState.size(); i++) {
            final int key = mLastImsConnectionState.keyAt(i);

            TelephonyEvent event = new TelephonyEventBuilder(mStartElapsedTimeMs, key)
                    .setImsConnectionState(mLastImsConnectionState.get(key)).build();
            addTelephonyEvent(event);
        }

        for (int i = 0; i < mLastCarrierId.size(); i++) {
            final int key = mLastCarrierId.keyAt(i);
            TelephonyEvent event = new TelephonyEventBuilder(mStartElapsedTimeMs, key)
                    .setCarrierIdMatching(mLastCarrierId.get(key)).build();
            addTelephonyEvent(event);
        }

        for (int i = 0; i < mLastNetworkCapabilitiesInfos.size(); i++) {
            final int key = mLastNetworkCapabilitiesInfos.keyAt(i);
            TelephonyEvent event = new TelephonyEventBuilder(mStartElapsedTimeMs, key)
                    .setNetworkCapabilities(mLastNetworkCapabilitiesInfos.get(key)).build();
            addTelephonyEvent(event);
        }

        for (int i = 0; i < mLastRilDataCallEvents.size(); i++) {
            final int key = mLastRilDataCallEvents.keyAt(i);
            for (int j = 0; j < mLastRilDataCallEvents.get(key).size(); j++) {
                final int cidKey = mLastRilDataCallEvents.get(key).keyAt(j);
                RilDataCall[] dataCalls = new RilDataCall[1];
                dataCalls[0] = mLastRilDataCallEvents.get(key).get(cidKey);
                addTelephonyEvent(new TelephonyEventBuilder(mStartElapsedTimeMs, key)
                        .setDataCalls(dataCalls).build());
            }
        }

        for (int i = 0; i < mLastRadioState.size(); i++) {
            final int key = mLastRadioState.keyAt(i);
            TelephonyEvent event = new TelephonyEventBuilder(mStartElapsedTimeMs, key)
                    .setRadioState(mLastRadioState.get(key)).build();
            addTelephonyEvent(event);
        }

        RcsStats.getInstance().reset();
    }

    /**
     * Build the telephony proto
     *
     * @return Telephony proto
     */
    private synchronized TelephonyLog buildProto() {

        TelephonyLog log = new TelephonyLog();
        // Build telephony events
        log.events = new TelephonyEvent[mTelephonyEvents.size()];
        mTelephonyEvents.toArray(log.events);
        log.eventsDropped = mTelephonyEventsDropped;

        // Build call sessions
        log.callSessions = new TelephonyCallSession[mCompletedCallSessions.size()];
        mCompletedCallSessions.toArray(log.callSessions);

        // Build SMS sessions
        log.smsSessions = new SmsSession[mCompletedSmsSessions.size()];
        mCompletedSmsSessions.toArray(log.smsSessions);

        // Build histogram. Currently we only support RIL histograms.
        List<TelephonyHistogram> rilHistograms = RIL.getTelephonyRILTimingHistograms();
        log.histograms = new TelephonyProto.TelephonyHistogram[rilHistograms.size()];
        for (int i = 0; i < rilHistograms.size(); i++) {
            log.histograms[i] = new TelephonyProto.TelephonyHistogram();
            TelephonyHistogram rilHistogram = rilHistograms.get(i);
            TelephonyProto.TelephonyHistogram histogramProto = log.histograms[i];

            histogramProto.category = rilHistogram.getCategory();
            histogramProto.id = rilHistogram.getId();
            histogramProto.minTimeMillis = rilHistogram.getMinTime();
            histogramProto.maxTimeMillis = rilHistogram.getMaxTime();
            histogramProto.avgTimeMillis = rilHistogram.getAverageTime();
            histogramProto.count = rilHistogram.getSampleCount();
            histogramProto.bucketCount = rilHistogram.getBucketCount();
            histogramProto.bucketEndPoints = rilHistogram.getBucketEndPoints();
            histogramProto.bucketCounters = rilHistogram.getBucketCounters();
        }

        // Build modem power metrics
        BatteryStatsManager batteryStatsManager = mContext == null ? null :
                (BatteryStatsManager) mContext.getSystemService(Context.BATTERY_STATS_SERVICE);
        log.modemPowerStats = new ModemPowerMetrics(batteryStatsManager).buildProto();

        // Log the hardware revision
        log.hardwareRevision = SystemProperties.get("ro.boot.revision", "");

        // Log the starting system time
        log.startTime = new TelephonyProto.Time();
        log.startTime.systemTimestampMillis = mStartSystemTimeMs;
        log.startTime.elapsedTimestampMillis = mStartElapsedTimeMs;

        log.endTime = new TelephonyProto.Time();
        log.endTime.systemTimestampMillis = System.currentTimeMillis();
        log.endTime.elapsedTimestampMillis = SystemClock.elapsedRealtime();

        // Log the last active subscription information.
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        ActiveSubscriptionInfo[] activeSubscriptionInfo =
                new ActiveSubscriptionInfo[phoneCount];
        for (int i = 0; i < mLastActiveSubscriptionInfos.size(); i++) {
            int key = mLastActiveSubscriptionInfos.keyAt(i);
            activeSubscriptionInfo[key] = mLastActiveSubscriptionInfos.get(key);
        }
        for (int i = 0; i < phoneCount; i++) {
            if (activeSubscriptionInfo[i] == null) {
                activeSubscriptionInfo[i] = makeInvalidSubscriptionInfo(i);
            }
        }
        log.lastActiveSubscriptionInfo = activeSubscriptionInfo;
        log.bandwidthEstimatorStats = buildBandwidthEstimatorStats();
        return log;
    }

    /** Update the sim state. */
    public void updateSimState(int phoneId, int simState) {
        int state = mapSimStateToProto(simState);
        Integer lastSimState = mLastSimState.get(phoneId);
        if (lastSimState == null || !lastSimState.equals(state)) {
            mLastSimState.put(phoneId, state);
            addTelephonyEvent(
                    new TelephonyEventBuilder(phoneId).setSimStateChange(mLastSimState).build());
        }
    }

    /** Update active subscription info list. */
    public synchronized void updateActiveSubscriptionInfoList(List<SubscriptionInfo> subInfos) {
        List<Integer> inActivePhoneList = new ArrayList<>();
        for (int i = 0; i < mLastActiveSubscriptionInfos.size(); i++) {
            inActivePhoneList.add(mLastActiveSubscriptionInfos.keyAt(i));
        }

        for (SubscriptionInfo info : subInfos) {
            int phoneId = info.getSimSlotIndex();
            inActivePhoneList.removeIf(value -> value.equals(phoneId));
            ActiveSubscriptionInfo activeSubscriptionInfo = new ActiveSubscriptionInfo();
            activeSubscriptionInfo.slotIndex = phoneId;
            activeSubscriptionInfo.isOpportunistic = info.isOpportunistic() ? 1 : 0;
            activeSubscriptionInfo.carrierId = info.getCarrierId();
            if (info.getMccString() != null && info.getMncString() != null) {
                activeSubscriptionInfo.simMccmnc = info.getMccString() + info.getMncString();
            }
            if (!MessageNano.messageNanoEquals(
                    mLastActiveSubscriptionInfos.get(phoneId), activeSubscriptionInfo)) {
                addTelephonyEvent(new TelephonyEventBuilder(phoneId)
                        .setActiveSubscriptionInfoChange(activeSubscriptionInfo).build());

                mLastActiveSubscriptionInfos.put(phoneId, activeSubscriptionInfo);
            }
        }

        for (int phoneId : inActivePhoneList) {
            mLastActiveSubscriptionInfos.remove(phoneId);
            addTelephonyEvent(new TelephonyEventBuilder(phoneId)
                    .setActiveSubscriptionInfoChange(makeInvalidSubscriptionInfo(phoneId)).build());
        }
    }

    /** Update the enabled modem bitmap. */
    public void updateEnabledModemBitmap(int enabledModemBitmap) {
        if (mLastEnabledModemBitmap == enabledModemBitmap) return;
        mLastEnabledModemBitmap = enabledModemBitmap;
        addTelephonyEvent(new TelephonyEventBuilder()
                .setEnabledModemBitmap(mLastEnabledModemBitmap).build());
    }

    private static ActiveSubscriptionInfo makeInvalidSubscriptionInfo(int phoneId) {
        ActiveSubscriptionInfo invalidSubscriptionInfo = new ActiveSubscriptionInfo();
        invalidSubscriptionInfo.slotIndex = phoneId;
        invalidSubscriptionInfo.carrierId = -1;
        invalidSubscriptionInfo.isOpportunistic = -1;
        return invalidSubscriptionInfo;
    }

    /**
     * Reduce precision to meet privacy requirements.
     *
     * @param timestamp timestamp in milliseconds
     * @return Precision reduced timestamp in minutes
     */
    static int roundSessionStart(long timestamp) {
        return (int) ((timestamp) / (MINUTE_IN_MILLIS * SESSION_START_PRECISION_MINUTES)
                * (SESSION_START_PRECISION_MINUTES));
    }

    /**
     * Write the Carrier Key change event
     *
     * @param phoneId Phone id
     * @param keyType type of key
     * @param isDownloadSuccessful true if the key was successfully downloaded
     */
    public void writeCarrierKeyEvent(int phoneId, int keyType,  boolean isDownloadSuccessful) {
        final CarrierKeyChange carrierKeyChange = new CarrierKeyChange();
        carrierKeyChange.keyType = keyType;
        carrierKeyChange.isDownloadSuccessful = isDownloadSuccessful;
        TelephonyEvent event = new TelephonyEventBuilder(phoneId).setCarrierKeyChange(
                carrierKeyChange).build();
        addTelephonyEvent(event);
    }


    /**
     * Get the time interval with reduced prevision
     *
     * @param previousTimestamp Previous timestamp in milliseconds
     * @param currentTimestamp Current timestamp in milliseconds
     * @return The time interval
     */
    static int toPrivacyFuzzedTimeInterval(long previousTimestamp, long currentTimestamp) {
        long diff = currentTimestamp - previousTimestamp;
        if (diff < 0) {
            return TimeInterval.TI_UNKNOWN;
        } else if (diff <= 10) {
            return TimeInterval.TI_10_MILLIS;
        } else if (diff <= 20) {
            return TimeInterval.TI_20_MILLIS;
        } else if (diff <= 50) {
            return TimeInterval.TI_50_MILLIS;
        } else if (diff <= 100) {
            return TimeInterval.TI_100_MILLIS;
        } else if (diff <= 200) {
            return TimeInterval.TI_200_MILLIS;
        } else if (diff <= 500) {
            return TimeInterval.TI_500_MILLIS;
        } else if (diff <= 1000) {
            return TimeInterval.TI_1_SEC;
        } else if (diff <= 2000) {
            return TimeInterval.TI_2_SEC;
        } else if (diff <= 5000) {
            return TimeInterval.TI_5_SEC;
        } else if (diff <= 10000) {
            return TimeInterval.TI_10_SEC;
        } else if (diff <= 30000) {
            return TimeInterval.TI_30_SEC;
        } else if (diff <= 60000) {
            return TimeInterval.TI_1_MINUTE;
        } else if (diff <= 180000) {
            return TimeInterval.TI_3_MINUTES;
        } else if (diff <= 600000) {
            return TimeInterval.TI_10_MINUTES;
        } else if (diff <= 1800000) {
            return TimeInterval.TI_30_MINUTES;
        } else if (diff <= 3600000) {
            return TimeInterval.TI_1_HOUR;
        } else if (diff <= 7200000) {
            return TimeInterval.TI_2_HOURS;
        } else if (diff <= 14400000) {
            return TimeInterval.TI_4_HOURS;
        } else {
            return TimeInterval.TI_MANY_HOURS;
        }
    }

    /**
     * Convert the service state into service state proto
     *
     * @param serviceState Service state
     * @return Service state proto
     */
    private TelephonyServiceState toServiceStateProto(ServiceState serviceState) {
        TelephonyServiceState ssProto = new TelephonyServiceState();

        ssProto.voiceRoamingType = serviceState.getVoiceRoamingType();
        ssProto.dataRoamingType = serviceState.getDataRoamingType();

        ssProto.voiceOperator = new TelephonyServiceState.TelephonyOperator();
        ssProto.dataOperator = new TelephonyServiceState.TelephonyOperator();
        if (serviceState.getOperatorAlphaLong() != null) {
            ssProto.voiceOperator.alphaLong = serviceState.getOperatorAlphaLong();
            ssProto.dataOperator.alphaLong = serviceState.getOperatorAlphaLong();
        }

        if (serviceState.getOperatorAlphaShort() != null) {
            ssProto.voiceOperator.alphaShort = serviceState.getOperatorAlphaShort();
            ssProto.dataOperator.alphaShort = serviceState.getOperatorAlphaShort();
        }

        if (serviceState.getOperatorNumeric() != null) {
            ssProto.voiceOperator.numeric = serviceState.getOperatorNumeric();
            ssProto.dataOperator.numeric = serviceState.getOperatorNumeric();
        }

        // Log PS WWAN only because CS WWAN would be exactly the same as voiceRat, and PS WLAN
        // would be always IWLAN in the rat field.
        // Note that we intentionally do not log reg state because it changes too frequently that
        // will grow the proto size too much.
        List<TelephonyServiceState.NetworkRegistrationInfo> nriList = new ArrayList<>();
        NetworkRegistrationInfo nri = serviceState.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        if (nri != null) {
            TelephonyServiceState.NetworkRegistrationInfo nriProto =
                    new TelephonyServiceState.NetworkRegistrationInfo();
            nriProto.domain = TelephonyServiceState.Domain.DOMAIN_PS;
            nriProto.transport = TelephonyServiceState.Transport.TRANSPORT_WWAN;
            nriProto.rat = ServiceState.networkTypeToRilRadioTechnology(
                    nri.getAccessNetworkTechnology());
            nriList.add(nriProto);
            ssProto.networkRegistrationInfo =
                    new TelephonyServiceState.NetworkRegistrationInfo[nriList.size()];
            nriList.toArray(ssProto.networkRegistrationInfo);
        }

        ssProto.voiceRat = serviceState.getRilVoiceRadioTechnology();
        ssProto.dataRat = serviceState.getRilDataRadioTechnology();
        ssProto.channelNumber = serviceState.getChannelNumber();
        ssProto.nrFrequencyRange = serviceState.getNrFrequencyRange();
        ssProto.nrState = serviceState.getNrState();
        return ssProto;
    }

    /**
     * Annotate the call session with events
     *
     * @param timestamp Event timestamp
     * @param phoneId Phone id
     * @param eventBuilder Call session event builder
     */
    private synchronized void annotateInProgressCallSession(long timestamp, int phoneId,
                                                            CallSessionEventBuilder eventBuilder) {
        InProgressCallSession callSession = mInProgressCallSessions.get(phoneId);
        if (callSession != null) {
            callSession.addEvent(timestamp, eventBuilder);
        }
    }

    /**
     * Annotate the SMS session with events
     *
     * @param timestamp Event timestamp
     * @param phoneId Phone id
     * @param eventBuilder SMS session event builder
     */
    private synchronized void annotateInProgressSmsSession(long timestamp, int phoneId,
                                                           SmsSessionEventBuilder eventBuilder) {
        InProgressSmsSession smsSession = mInProgressSmsSessions.get(phoneId);
        if (smsSession != null) {
            smsSession.addEvent(timestamp, eventBuilder);
        }
    }

    /**
     * Create the call session if there isn't any existing one
     *
     * @param phoneId Phone id
     * @return The call session
     */
    private synchronized InProgressCallSession startNewCallSessionIfNeeded(int phoneId) {
        InProgressCallSession callSession = mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            logv("Starting a new call session on phone " + phoneId);
            callSession = new InProgressCallSession(phoneId);
            mInProgressCallSessions.append(phoneId, callSession);

            // Insert the latest service state, ims capabilities, and ims connection states as the
            // base.
            TelephonyServiceState serviceState = mLastServiceState.get(phoneId);
            if (serviceState != null) {
                callSession.addEvent(callSession.startElapsedTimeMs, new CallSessionEventBuilder(
                        TelephonyCallSession.Event.Type.RIL_SERVICE_STATE_CHANGED)
                        .setServiceState(serviceState));
            }

            ImsCapabilities imsCapabilities = mLastImsCapabilities.get(phoneId);
            if (imsCapabilities != null) {
                callSession.addEvent(callSession.startElapsedTimeMs, new CallSessionEventBuilder(
                        TelephonyCallSession.Event.Type.IMS_CAPABILITIES_CHANGED)
                        .setImsCapabilities(imsCapabilities));
            }

            ImsConnectionState imsConnectionState = mLastImsConnectionState.get(phoneId);
            if (imsConnectionState != null) {
                callSession.addEvent(callSession.startElapsedTimeMs, new CallSessionEventBuilder(
                        TelephonyCallSession.Event.Type.IMS_CONNECTION_STATE_CHANGED)
                        .setImsConnectionState(imsConnectionState));
            }
        }
        return callSession;
    }

    /**
     * Create the SMS session if there isn't any existing one
     *
     * @param phoneId Phone id
     * @return The SMS session
     */
    private synchronized InProgressSmsSession startNewSmsSessionIfNeeded(int phoneId) {
        InProgressSmsSession smsSession = mInProgressSmsSessions.get(phoneId);
        if (smsSession == null) {
            logv("Starting a new sms session on phone " + phoneId);
            smsSession = startNewSmsSession(phoneId);
            mInProgressSmsSessions.append(phoneId, smsSession);
        }
        return smsSession;
    }

    /**
     * Create a new SMS session
     *
     * @param phoneId Phone id
     * @return The SMS session
     */
    private InProgressSmsSession startNewSmsSession(int phoneId) {
        InProgressSmsSession smsSession = new InProgressSmsSession(phoneId);

        // Insert the latest service state, ims capabilities, and ims connection state as the
        // base.
        TelephonyServiceState serviceState = mLastServiceState.get(phoneId);
        if (serviceState != null) {
            smsSession.addEvent(smsSession.startElapsedTimeMs, new SmsSessionEventBuilder(
                    SmsSession.Event.Type.RIL_SERVICE_STATE_CHANGED)
                    .setServiceState(serviceState));
        }

        ImsCapabilities imsCapabilities = mLastImsCapabilities.get(phoneId);
        if (imsCapabilities != null) {
            smsSession.addEvent(smsSession.startElapsedTimeMs, new SmsSessionEventBuilder(
                    SmsSession.Event.Type.IMS_CAPABILITIES_CHANGED)
                    .setImsCapabilities(imsCapabilities));
        }

        ImsConnectionState imsConnectionState = mLastImsConnectionState.get(phoneId);
        if (imsConnectionState != null) {
            smsSession.addEvent(smsSession.startElapsedTimeMs, new SmsSessionEventBuilder(
                    SmsSession.Event.Type.IMS_CONNECTION_STATE_CHANGED)
                    .setImsConnectionState(imsConnectionState));
        }
        return smsSession;
    }

    /**
     * Finish the call session and move it into the completed session
     *
     * @param inProgressCallSession The in progress call session
     */
    private synchronized void finishCallSession(InProgressCallSession inProgressCallSession) {
        TelephonyCallSession callSession = new TelephonyCallSession();
        callSession.events = new TelephonyCallSession.Event[inProgressCallSession.events.size()];
        inProgressCallSession.events.toArray(callSession.events);
        callSession.startTimeMinutes = inProgressCallSession.startSystemTimeMin;
        callSession.phoneId = inProgressCallSession.phoneId;
        callSession.eventsDropped = inProgressCallSession.isEventsDropped();
        if (mCompletedCallSessions.size() >= MAX_COMPLETED_CALL_SESSIONS) {
            mCompletedCallSessions.removeFirst();
        }
        mCompletedCallSessions.add(callSession);
        mInProgressCallSessions.remove(inProgressCallSession.phoneId);
        logv("Call session finished");
    }

    /**
     * Finish the SMS session and move it into the completed session
     *
     * @param inProgressSmsSession The in progress SMS session
     */
    private synchronized void finishSmsSessionIfNeeded(InProgressSmsSession inProgressSmsSession) {
        if (inProgressSmsSession.getNumExpectedResponses() == 0) {
            SmsSession smsSession = finishSmsSession(inProgressSmsSession);

            mInProgressSmsSessions.remove(inProgressSmsSession.phoneId);
            logv("SMS session finished");
        }
    }

    private synchronized SmsSession finishSmsSession(InProgressSmsSession inProgressSmsSession) {
        SmsSession smsSession = new SmsSession();
        smsSession.events = new SmsSession.Event[inProgressSmsSession.events.size()];
        inProgressSmsSession.events.toArray(smsSession.events);
        smsSession.startTimeMinutes = inProgressSmsSession.startSystemTimeMin;
        smsSession.phoneId = inProgressSmsSession.phoneId;
        smsSession.eventsDropped = inProgressSmsSession.isEventsDropped();

        if (mCompletedSmsSessions.size() >= MAX_COMPLETED_SMS_SESSIONS) {
            mCompletedSmsSessions.removeFirst();
        }
        mCompletedSmsSessions.add(smsSession);
        return smsSession;
    }

    /**
     * Add telephony event into the queue
     *
     * @param event Telephony event
     */
    private synchronized void addTelephonyEvent(TelephonyEvent event) {
        if (mTelephonyEvents.size() >= MAX_TELEPHONY_EVENTS) {
            mTelephonyEvents.removeFirst();
            mTelephonyEventsDropped = true;
        }
        mTelephonyEvents.add(event);
    }

    /**
     * Write service changed event
     *
     * @param phoneId Phone id
     * @param serviceState Service state
     */
    public synchronized void writeServiceStateChanged(int phoneId, ServiceState serviceState) {

        TelephonyEvent event = new TelephonyEventBuilder(phoneId)
                .setServiceState(toServiceStateProto(serviceState)).build();

        // If service state doesn't change, we don't log the event.
        if (mLastServiceState.get(phoneId) != null &&
                Arrays.equals(TelephonyServiceState.toByteArray(mLastServiceState.get(phoneId)),
                        TelephonyServiceState.toByteArray(event.serviceState))) {
            return;
        }

        mLastServiceState.put(phoneId, event.serviceState);
        addTelephonyEvent(event);

        annotateInProgressCallSession(event.timestampMillis, phoneId,
                new CallSessionEventBuilder(
                        TelephonyCallSession.Event.Type.RIL_SERVICE_STATE_CHANGED)
                        .setServiceState(event.serviceState));
        annotateInProgressSmsSession(event.timestampMillis, phoneId,
                new SmsSessionEventBuilder(
                        SmsSession.Event.Type.RIL_SERVICE_STATE_CHANGED)
                        .setServiceState(event.serviceState));
    }

    /**
     * Write data stall event
     *
     * @param phoneId Phone id
     * @param recoveryAction Data stall recovery action
     */
    public void writeDataStallEvent(int phoneId, int recoveryAction) {
        addTelephonyEvent(new TelephonyEventBuilder(phoneId)
                .setDataStallRecoveryAction(recoveryAction).build());
    }

    /**
     * Write SignalStrength event
     *
     * @param phoneId Phone id
     * @param signalStrength Signal strength at the time of data stall recovery
     */
    public void writeSignalStrengthEvent(int phoneId, int signalStrength) {
        addTelephonyEvent(new TelephonyEventBuilder(phoneId)
                .setSignalStrength(signalStrength).build());
    }

    private TelephonySettings cloneCurrentTelephonySettings(int phoneId) {
        TelephonySettings newSettings = new TelephonySettings();
        TelephonySettings lastSettings = mLastSettings.get(phoneId);
        if (lastSettings != null) {
            // No clone method available, so each relevant field is copied individually.
            newSettings.preferredNetworkMode = lastSettings.preferredNetworkMode;
            newSettings.isEnhanced4GLteModeEnabled = lastSettings.isEnhanced4GLteModeEnabled;
            newSettings.isVtOverLteEnabled = lastSettings.isVtOverLteEnabled;
            newSettings.isWifiCallingEnabled = lastSettings.isWifiCallingEnabled;
            newSettings.isVtOverWifiEnabled = lastSettings.isVtOverWifiEnabled;
        }
        return newSettings;
    }

    /**
     * Write IMS feature settings changed event
     *
     * @param phoneId Phone id
     * @param feature IMS feature
     * @param network The IMS network type
     * @param value The settings. 0 indicates disabled, otherwise enabled.
     */
    public synchronized void writeImsSetFeatureValue(int phoneId, int feature, int network,
            int value) {
        TelephonySettings s = cloneCurrentTelephonySettings(phoneId);
        if (network == ImsRegistrationImplBase.REGISTRATION_TECH_LTE) {
            switch (feature) {
                case MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE:
                    s.isEnhanced4GLteModeEnabled = (value != 0);
                    break;
                case MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO:
                    s.isVtOverLteEnabled = (value != 0);
                    break;
            }
        } else if (network == ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN) {
            switch (feature) {
                case MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE:
                    s.isWifiCallingEnabled = (value != 0);
                    break;
                case MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO:
                    s.isVtOverWifiEnabled = (value != 0);
                    break;
            }
        }

        // If the settings don't change, we don't log the event.
        if (mLastSettings.get(phoneId) != null &&
                Arrays.equals(TelephonySettings.toByteArray(mLastSettings.get(phoneId)),
                        TelephonySettings.toByteArray(s))) {
            return;
        }

        mLastSettings.put(phoneId, s);

        TelephonyEvent event = new TelephonyEventBuilder(phoneId).setSettings(s).build();
        addTelephonyEvent(event);

        annotateInProgressCallSession(event.timestampMillis, phoneId,
                new CallSessionEventBuilder(TelephonyCallSession.Event.Type.SETTINGS_CHANGED)
                        .setSettings(s));
        annotateInProgressSmsSession(event.timestampMillis, phoneId,
                new SmsSessionEventBuilder(SmsSession.Event.Type.SETTINGS_CHANGED)
                        .setSettings(s));
    }

    /**
     * Write the preferred network settings changed event
     *
     * @param phoneId Phone id
     * @param networkType The preferred network
     */
    public synchronized void writeSetPreferredNetworkType(int phoneId,
            @PrefNetworkMode int networkType) {
        TelephonySettings s = cloneCurrentTelephonySettings(phoneId);
        s.preferredNetworkMode = networkType + 1;

        // If the settings don't change, we don't log the event.
        if (mLastSettings.get(phoneId) != null &&
                Arrays.equals(TelephonySettings.toByteArray(mLastSettings.get(phoneId)),
                        TelephonySettings.toByteArray(s))) {
            return;
        }

        mLastSettings.put(phoneId, s);

        addTelephonyEvent(new TelephonyEventBuilder(phoneId).setSettings(s).build());
    }

    /**
     * Write the IMS connection state changed event
     *
     * @param phoneId Phone id
     * @param state IMS connection state
     * @param reasonInfo The reason info. Only used for disconnected state.
     */
    public synchronized void writeOnImsConnectionState(int phoneId, int state,
                                                       ImsReasonInfo reasonInfo) {
        ImsConnectionState imsState = new ImsConnectionState();
        imsState.state = state;

        if (reasonInfo != null) {
            TelephonyProto.ImsReasonInfo ri = new TelephonyProto.ImsReasonInfo();

            ri.reasonCode = reasonInfo.getCode();
            ri.extraCode = reasonInfo.getExtraCode();
            String extraMessage = reasonInfo.getExtraMessage();
            if (extraMessage != null) {
                ri.extraMessage = extraMessage;
            }

            imsState.reasonInfo = ri;
        }

        // If the connection state does not change, do not log it.
        if (mLastImsConnectionState.get(phoneId) != null &&
                Arrays.equals(ImsConnectionState.toByteArray(mLastImsConnectionState.get(phoneId)),
                        ImsConnectionState.toByteArray(imsState))) {
            return;
        }

        mLastImsConnectionState.put(phoneId, imsState);

        TelephonyEvent event = new TelephonyEventBuilder(phoneId)
                .setImsConnectionState(imsState).build();
        addTelephonyEvent(event);

        annotateInProgressCallSession(event.timestampMillis, phoneId,
                new CallSessionEventBuilder(
                        TelephonyCallSession.Event.Type.IMS_CONNECTION_STATE_CHANGED)
                        .setImsConnectionState(event.imsConnectionState));
        annotateInProgressSmsSession(event.timestampMillis, phoneId,
                new SmsSessionEventBuilder(
                        SmsSession.Event.Type.IMS_CONNECTION_STATE_CHANGED)
                        .setImsConnectionState(event.imsConnectionState));
    }

    /**
     * Write the IMS capabilities changed event
     *
     * @param phoneId Phone id
     * @param capabilities IMS capabilities array
     */
    public synchronized void writeOnImsCapabilities(int phoneId,
            @ImsRegistrationImplBase.ImsRegistrationTech int radioTech,
            MmTelFeature.MmTelCapabilities capabilities) {
        ImsCapabilities cap = new ImsCapabilities();

        if (radioTech == ImsRegistrationImplBase.REGISTRATION_TECH_LTE) {
            cap.voiceOverLte = capabilities.isCapable(
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE);
            cap.videoOverLte = capabilities.isCapable(
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO);
            cap.utOverLte = capabilities.isCapable(
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_UT);

        } else if (radioTech == ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN) {
            cap.voiceOverWifi = capabilities.isCapable(
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE);
            cap.videoOverWifi = capabilities.isCapable(
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO);
            cap.utOverWifi = capabilities.isCapable(
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_UT);
        }

        TelephonyEvent event = new TelephonyEventBuilder(phoneId).setImsCapabilities(cap).build();

        // If the capabilities don't change, we don't log the event.
        if (mLastImsCapabilities.get(phoneId) != null &&
                Arrays.equals(ImsCapabilities.toByteArray(mLastImsCapabilities.get(phoneId)),
                ImsCapabilities.toByteArray(cap))) {
            return;
        }

        mLastImsCapabilities.put(phoneId, cap);
        addTelephonyEvent(event);

        annotateInProgressCallSession(event.timestampMillis, phoneId,
                new CallSessionEventBuilder(
                        TelephonyCallSession.Event.Type.IMS_CAPABILITIES_CHANGED)
                        .setImsCapabilities(event.imsCapabilities));
        annotateInProgressSmsSession(event.timestampMillis, phoneId,
                new SmsSessionEventBuilder(
                        SmsSession.Event.Type.IMS_CAPABILITIES_CHANGED)
                        .setImsCapabilities(event.imsCapabilities));
    }

    /**
     * Convert PDP type into the enumeration
     *
     * @param type PDP type
     * @return The proto defined enumeration
     */
    private int toPdpType(String type) {
        switch (type) {
            case "IP":
                return PDP_TYPE_IP;
            case "IPV6":
                return PDP_TYPE_IPV6;
            case "IPV4V6":
                return PDP_TYPE_IPV4V6;
            case "PPP":
                return PDP_TYPE_PPP;
            case "NON-IP":
                return PDP_TYPE_NON_IP;
            case "UNSTRUCTURED":
                return PDP_TYPE_UNSTRUCTURED;
        }
        Rlog.e(TAG, "Unknown type: " + type);
        return PDP_UNKNOWN;
    }

    /**
     * Write setup data call event
     *
     * @param phoneId Phone id
     * @param radioTechnology The data call RAT
     * @param profileId Data profile id
     * @param apn APN in string
     * @param protocol Data connection protocol
     */
    public void writeSetupDataCall(int phoneId, int radioTechnology, int profileId, String apn,
                                   int protocol) {

        RilSetupDataCall setupDataCall = new RilSetupDataCall();
        setupDataCall.rat = radioTechnology;
        setupDataCall.dataProfile = profileId + 1;  // off by 1 between proto and RIL constants.
        if (apn != null) {
            setupDataCall.apn = apn;
        }

        setupDataCall.type = protocol + 1;

        addTelephonyEvent(new TelephonyEventBuilder(phoneId).setSetupDataCall(
                setupDataCall).build());
    }

    /**
     * Write data call deactivate event
     *
     * @param phoneId Phone id
     * @param rilSerial RIL request serial number
     * @param cid call id
     * @param reason Deactivate reason
     */
    public void writeRilDeactivateDataCall(int phoneId, int rilSerial, int cid, int reason) {

        RilDeactivateDataCall deactivateDataCall = new RilDeactivateDataCall();
        deactivateDataCall.cid = cid;
        switch (reason) {
            case DataService.REQUEST_REASON_NORMAL:
                deactivateDataCall.reason = DeactivateReason.DEACTIVATE_REASON_NONE;
                break;
            case DataService.REQUEST_REASON_SHUTDOWN:
                deactivateDataCall.reason = DeactivateReason.DEACTIVATE_REASON_RADIO_OFF;
                break;
            case DataService.REQUEST_REASON_HANDOVER:
                deactivateDataCall.reason = DeactivateReason.DEACTIVATE_REASON_HANDOVER;
                break;
            default:
                deactivateDataCall.reason = DeactivateReason.DEACTIVATE_REASON_UNKNOWN;
        }

        addTelephonyEvent(new TelephonyEventBuilder(phoneId).setDeactivateDataCall(
                deactivateDataCall).build());
    }

    /**
     * Write data call list event when connected
     * @param phoneId          Phone id
     * @param cid              Context Id, uniquely identifies the call
     * @param apnTypeBitmask   Bitmask of supported APN types
     * @param state            State of the data call event
     */
    public void writeRilDataCallEvent(int phoneId, int cid,
            int apnTypeBitmask, int state) {
        RilDataCall[] dataCalls = new RilDataCall[1];
        dataCalls[0] = new RilDataCall();
        dataCalls[0].cid = cid;
        dataCalls[0].apnTypeBitmask = apnTypeBitmask;
        dataCalls[0].state = state;

        SparseArray<RilDataCall> dataCallList;
        if (mLastRilDataCallEvents.get(phoneId) != null) {
            // If the Data call event does not change, do not log it.
            if (mLastRilDataCallEvents.get(phoneId).get(cid) != null
                    && Arrays.equals(
                        RilDataCall.toByteArray(mLastRilDataCallEvents.get(phoneId).get(cid)),
                        RilDataCall.toByteArray(dataCalls[0]))) {
                return;
            }
            dataCallList =  mLastRilDataCallEvents.get(phoneId);
        } else {
            dataCallList = new SparseArray<>();
        }

        dataCallList.put(cid, dataCalls[0]);
        mLastRilDataCallEvents.put(phoneId, dataCallList);
        addTelephonyEvent(new TelephonyEventBuilder(phoneId).setDataCalls(dataCalls).build());
    }

    /**
     * Write CS call list event
     *
     * @param phoneId    Phone id
     * @param connections Array of GsmCdmaConnection objects
     */
    public void writeRilCallList(int phoneId, ArrayList<GsmCdmaConnection> connections,
                                 String countryIso) {
        logv("Logging CallList Changed Connections Size = " + connections.size());
        InProgressCallSession callSession = startNewCallSessionIfNeeded(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "writeRilCallList: Call session is missing");
        } else {
            RilCall[] calls = convertConnectionsToRilCalls(connections, countryIso);
            callSession.addEvent(
                    new CallSessionEventBuilder(
                            TelephonyCallSession.Event.Type.RIL_CALL_LIST_CHANGED)
                            .setRilCalls(calls)
            );
            logv("Logged Call list changed");
            if (callSession.isPhoneIdle() && disconnectReasonsKnown(calls)) {
                finishCallSession(callSession);
            }
        }
    }

    private boolean disconnectReasonsKnown(RilCall[] calls) {
        for (RilCall call : calls) {
            if (call.callEndReason == 0) return false;
        }
        return true;
    }

    private RilCall[] convertConnectionsToRilCalls(ArrayList<GsmCdmaConnection> mConnections,
                                                   String countryIso) {
        RilCall[] calls = new RilCall[mConnections.size()];
        for (int i = 0; i < mConnections.size(); i++) {
            calls[i] = new RilCall();
            calls[i].index = i;
            convertConnectionToRilCall(mConnections.get(i), calls[i], countryIso);
        }
        return calls;
    }

    private EmergencyNumberInfo convertEmergencyNumberToEmergencyNumberInfo(EmergencyNumber num) {
        EmergencyNumberInfo emergencyNumberInfo = new EmergencyNumberInfo();
        emergencyNumberInfo.address = num.getNumber();
        emergencyNumberInfo.countryIso = num.getCountryIso();
        emergencyNumberInfo.mnc = num.getMnc();
        emergencyNumberInfo.serviceCategoriesBitmask = num.getEmergencyServiceCategoryBitmask();
        emergencyNumberInfo.urns = num.getEmergencyUrns().stream().toArray(String[]::new);
        emergencyNumberInfo.numberSourcesBitmask = num.getEmergencyNumberSourceBitmask();
        emergencyNumberInfo.routing = num.getEmergencyCallRouting();
        return emergencyNumberInfo;
    }

    private void convertConnectionToRilCall(GsmCdmaConnection conn, RilCall call,
                                            String countryIso) {
        if (conn.isIncoming()) {
            call.type = Type.MT;
        } else {
            call.type = Type.MO;
        }
        switch (conn.getState()) {
            case IDLE:
                call.state = CallState.CALL_IDLE;
                break;
            case ACTIVE:
                call.state = CallState.CALL_ACTIVE;
                break;
            case HOLDING:
                call.state = CallState.CALL_HOLDING;
                break;
            case DIALING:
                call.state = CallState.CALL_DIALING;
                break;
            case ALERTING:
                call.state = CallState.CALL_ALERTING;
                break;
            case INCOMING:
                call.state = CallState.CALL_INCOMING;
                break;
            case WAITING:
                call.state = CallState.CALL_WAITING;
                break;
            case DISCONNECTED:
                call.state = CallState.CALL_DISCONNECTED;
                break;
            case DISCONNECTING:
                call.state = CallState.CALL_DISCONNECTING;
                break;
            default:
                call.state = CallState.CALL_UNKNOWN;
                break;
        }
        call.callEndReason = conn.getDisconnectCause();
        call.isMultiparty = conn.isMultiparty();
        call.preciseDisconnectCause = conn.getPreciseDisconnectCause();

        // Emergency call metrics when call ends
        if (conn.getDisconnectCause() != DisconnectCause.NOT_DISCONNECTED
                && conn.isEmergencyCall() && conn.getEmergencyNumberInfo() != null) {
            /** Only collect this emergency number information per sample percentage */
            if (ThreadLocalRandom.current().nextDouble(0, 100)
                    < getSamplePercentageForEmergencyCall(countryIso)) {
                call.isEmergencyCall = conn.isEmergencyCall();
                call.emergencyNumberInfo = convertEmergencyNumberToEmergencyNumberInfo(
                        conn.getEmergencyNumberInfo());
                EmergencyNumberTracker emergencyNumberTracker = conn.getEmergencyNumberTracker();
                call.emergencyNumberDatabaseVersion = emergencyNumberTracker != null
                        ? emergencyNumberTracker.getEmergencyNumberDbVersion()
                        : TelephonyManager.INVALID_EMERGENCY_NUMBER_DB_VERSION;
            }
        }
    }

    /**
     * Write dial event
     *
     * @param phoneId Phone id
     * @param conn Connection object created to track this call
     * @param clirMode CLIR (Calling Line Identification Restriction) mode
     * @param uusInfo User-to-User signaling Info
     */
    public void writeRilDial(int phoneId, GsmCdmaConnection conn, int clirMode, UUSInfo uusInfo) {

        InProgressCallSession callSession = startNewCallSessionIfNeeded(phoneId);
        logv("Logging Dial Connection = " + conn);
        if (callSession == null) {
            Rlog.e(TAG, "writeRilDial: Call session is missing");
        } else {
            RilCall[] calls = new RilCall[1];
            calls[0] = new RilCall();
            calls[0].index = -1;
            convertConnectionToRilCall(conn, calls[0], "");
            callSession.addEvent(callSession.startElapsedTimeMs,
                    new CallSessionEventBuilder(TelephonyCallSession.Event.Type.RIL_REQUEST)
                            .setRilRequest(TelephonyCallSession.Event.RilRequest.RIL_REQUEST_DIAL)
                            .setRilCalls(calls));
            logv("Logged Dial event");
        }
    }

    /**
     * Write incoming call event
     *
     * @param phoneId Phone id
     * @param response Unused today
     */
    public void writeRilCallRing(int phoneId, char[] response) {
        InProgressCallSession callSession = startNewCallSessionIfNeeded(phoneId);

        callSession.addEvent(callSession.startElapsedTimeMs,
                new CallSessionEventBuilder(TelephonyCallSession.Event.Type.RIL_CALL_RING));
    }

    /**
     * Write call hangup event
     *
     * @param phoneId Phone id
     * @param conn Connection object associated with the call that is being hung-up
     * @param callId Call id
     */
    public void writeRilHangup(int phoneId, GsmCdmaConnection conn, int callId,
                               String countryIso) {
        InProgressCallSession callSession = mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "writeRilHangup: Call session is missing");
        } else {
            RilCall[] calls = new RilCall[1];
            calls[0] = new RilCall();
            calls[0].index = callId;
            convertConnectionToRilCall(conn, calls[0], countryIso);
            callSession.addEvent(
                    new CallSessionEventBuilder(TelephonyCallSession.Event.Type.RIL_REQUEST)
                            .setRilRequest(TelephonyCallSession.Event.RilRequest.RIL_REQUEST_HANGUP)
                            .setRilCalls(calls));
            logv("Logged Hangup event");
        }
    }

    /**
     * Write call answer event
     *
     * @param phoneId Phone id
     * @param rilSerial RIL request serial number
     */
    public void writeRilAnswer(int phoneId, int rilSerial) {
        InProgressCallSession callSession = mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "writeRilAnswer: Call session is missing");
        } else {
            callSession.addEvent(
                    new CallSessionEventBuilder(TelephonyCallSession.Event.Type.RIL_REQUEST)
                            .setRilRequest(TelephonyCallSession.Event.RilRequest.RIL_REQUEST_ANSWER)
                            .setRilRequestId(rilSerial));
        }
    }

    /**
     * Write IMS call SRVCC event
     *
     * @param phoneId Phone id
     * @param rilSrvccState SRVCC state
     */
    public void writeRilSrvcc(int phoneId, int rilSrvccState) {
        InProgressCallSession callSession =  mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "writeRilSrvcc: Call session is missing");
        } else {
            callSession.addEvent(
                    new CallSessionEventBuilder(TelephonyCallSession.Event.Type.RIL_CALL_SRVCC)
                            .setSrvccState(rilSrvccState + 1));
        }
    }

    /**
     * Convert RIL request into proto defined RIL request
     *
     * @param r RIL request
     * @return RIL request defined in call session proto
     */
    private int toCallSessionRilRequest(int r) {
        switch (r) {
            case RILConstants.RIL_REQUEST_DIAL:
                return TelephonyCallSession.Event.RilRequest.RIL_REQUEST_DIAL;

            case RILConstants.RIL_REQUEST_ANSWER:
                return TelephonyCallSession.Event.RilRequest.RIL_REQUEST_ANSWER;

            case RILConstants.RIL_REQUEST_HANGUP:
            case RILConstants.RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND:
            case RILConstants.RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND:
                return TelephonyCallSession.Event.RilRequest.RIL_REQUEST_HANGUP;

            case RILConstants.RIL_REQUEST_SET_CALL_WAITING:
                return TelephonyCallSession.Event.RilRequest.RIL_REQUEST_SET_CALL_WAITING;

            case RILConstants.RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE:
                return TelephonyCallSession.Event.RilRequest.RIL_REQUEST_SWITCH_HOLDING_AND_ACTIVE;

            case RILConstants.RIL_REQUEST_CDMA_FLASH:
                return TelephonyCallSession.Event.RilRequest.RIL_REQUEST_CDMA_FLASH;

            case RILConstants.RIL_REQUEST_CONFERENCE:
                return TelephonyCallSession.Event.RilRequest.RIL_REQUEST_CONFERENCE;
        }
        Rlog.e(TAG, "Unknown RIL request: " + r);
        return TelephonyCallSession.Event.RilRequest.RIL_REQUEST_UNKNOWN;
    }

    /**
     * Write setup data call response event
     *
     * @param phoneId Phone id
     * @param rilSerial RIL request serial number
     * @param rilError RIL error
     * @param rilRequest RIL request
     * @param result Data call result
     */
    private void writeOnSetupDataCallResponse(int phoneId, int rilSerial, int rilError,
                                              int rilRequest, DataCallResponse response) {

        RilSetupDataCallResponse setupDataCallResponse = new RilSetupDataCallResponse();
        RilDataCall dataCall = new RilDataCall();

        if (response != null) {
            setupDataCallResponse.status = (response.getCause() == 0
                    ? RilDataCallFailCause.PDP_FAIL_NONE : response.getCause());
            setupDataCallResponse.suggestedRetryTimeMillis = response.getSuggestedRetryTime();

            dataCall.cid = response.getId();
            dataCall.type = response.getProtocolType() + 1;

            if (!TextUtils.isEmpty(response.getInterfaceName())) {
                dataCall.ifname = response.getInterfaceName();
            }
        }
        setupDataCallResponse.call = dataCall;

        addTelephonyEvent(new TelephonyEventBuilder(phoneId)
                .setSetupDataCallResponse(setupDataCallResponse).build());
    }

    /**
     * Write call related solicited response event
     *
     * @param phoneId Phone id
     * @param rilSerial RIL request serial number
     * @param rilError RIL error
     * @param rilRequest RIL request
     */
    private void writeOnCallSolicitedResponse(int phoneId, int rilSerial, int rilError,
                                              int rilRequest) {
        InProgressCallSession callSession = mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "writeOnCallSolicitedResponse: Call session is missing");
        } else {
            callSession.addEvent(new CallSessionEventBuilder(
                    TelephonyCallSession.Event.Type.RIL_RESPONSE)
                    .setRilRequest(toCallSessionRilRequest(rilRequest))
                    .setRilRequestId(rilSerial)
                    .setRilError(rilError + 1));
        }
    }

    /**
     * Write SMS related solicited response event
     *
     * @param phoneId Phone id
     * @param rilSerial RIL request serial number
     * @param rilError RIL error
     * @param response SMS response
     */
    private synchronized void writeOnSmsSolicitedResponse(int phoneId, int rilSerial, int rilError,
                                                          SmsResponse response) {
        InProgressSmsSession smsSession = mInProgressSmsSessions.get(phoneId);
        if (smsSession == null) {
            Rlog.e(TAG, "SMS session is missing");
        } else {
            int errorCode = SmsResponse.NO_ERROR_CODE;
            long messageId = 0L;
            if (response != null) {
                errorCode = response.mErrorCode;
                messageId = response.mMessageId;
            }

            smsSession.addEvent(new SmsSessionEventBuilder(
                    SmsSession.Event.Type.SMS_SEND_RESULT)
                    .setErrorCode(errorCode)
                    .setRilErrno(rilError + 1)
                    .setRilRequestId(rilSerial)
                    .setMessageId(messageId)
            );

            smsSession.decreaseExpectedResponse();
            finishSmsSessionIfNeeded(smsSession);
        }
    }

    /**
     * Write SMS related solicited response event
     *
     * @param phoneId Phone id
     * @param errorReason Defined in {@link SmsManager} RESULT_XXX.
     * @param messageId Unique id for this message.
     */
    public synchronized void writeOnImsServiceSmsSolicitedResponse(int phoneId,
            @ImsSmsImplBase.SendStatusResult int resultCode, int errorReason,
            long messageId) {

        InProgressSmsSession smsSession = mInProgressSmsSessions.get(phoneId);
        if (smsSession == null) {
            Rlog.e(TAG, "SMS session is missing");
        } else {

            smsSession.addEvent(new SmsSessionEventBuilder(
                    SmsSession.Event.Type.SMS_SEND_RESULT)
                    .setImsServiceErrno(resultCode)
                    .setErrorCode(errorReason)
                    .setMessageId(messageId)
            );

            smsSession.decreaseExpectedResponse();
            finishSmsSessionIfNeeded(smsSession);
        }
    }

    /**
     * Write deactivate data call response event
     *
     * @param phoneId Phone id
     * @param rilError RIL error
     */
    private void writeOnDeactivateDataCallResponse(int phoneId, int rilError) {
        addTelephonyEvent(new TelephonyEventBuilder(phoneId)
                .setDeactivateDataCallResponse(rilError + 1).build());
    }

    /**
     * Write RIL solicited response event
     *
     * @param phoneId Phone id
     * @param rilSerial RIL request serial number
     * @param rilError RIL error
     * @param rilRequest RIL request
     * @param ret The returned RIL response
     */
    public void writeOnRilSolicitedResponse(int phoneId, int rilSerial, int rilError,
                                            int rilRequest, Object ret) {
        switch (rilRequest) {
            case RIL_REQUEST_SETUP_DATA_CALL:
                DataCallResponse response = (DataCallResponse) ret;
                writeOnSetupDataCallResponse(phoneId, rilSerial, rilError, rilRequest, response);
                break;
            case RIL_REQUEST_DEACTIVATE_DATA_CALL:
                writeOnDeactivateDataCallResponse(phoneId, rilError);
                break;
            case RIL_REQUEST_HANGUP:
            case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND:
            case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND:
            case RIL_REQUEST_DIAL:
            case RIL_REQUEST_ANSWER:
                writeOnCallSolicitedResponse(phoneId, rilSerial, rilError, rilRequest);
                break;
            case RIL_REQUEST_SEND_SMS:
            case RIL_REQUEST_SEND_SMS_EXPECT_MORE:
            case RIL_REQUEST_CDMA_SEND_SMS:
            case RIL_REQUEST_IMS_SEND_SMS:
                SmsResponse smsResponse = (SmsResponse) ret;
                writeOnSmsSolicitedResponse(phoneId, rilSerial, rilError, smsResponse);
                break;
        }
    }

    /**
     * Write network validation event.
     * @param networkValidationState the network validation state.
     */
    public void writeNetworkValidate(int networkValidationState) {
        addTelephonyEvent(
                new TelephonyEventBuilder().setNetworkValidate(networkValidationState).build());
    }

    /**
     * Write data switch event.
     * @param subId data switch to the subscription with this id.
     * @param dataSwitch the reason and state of data switch.
     */
    public void writeDataSwitch(int subId, DataSwitch dataSwitch) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        addTelephonyEvent(new TelephonyEventBuilder(phoneId).setDataSwitch(dataSwitch).build());
    }

    /**
     * Write on demand data switch event.
     * @param onDemandDataSwitch the apn and state of on demand data switch.
     */
    public void writeOnDemandDataSwitch(OnDemandDataSwitch onDemandDataSwitch) {
        addTelephonyEvent(
                new TelephonyEventBuilder().setOnDemandDataSwitch(onDemandDataSwitch).build());
    }

    /**
     * Write phone state changed event
     *
     * @param phoneId Phone id
     * @param phoneState Phone state. See PhoneConstants.State for the details.
     */
    public void writePhoneState(int phoneId, PhoneConstants.State phoneState) {
        int state;
        switch (phoneState) {
            case IDLE:
                state = TelephonyCallSession.Event.PhoneState.STATE_IDLE;
                break;
            case RINGING:
                state = TelephonyCallSession.Event.PhoneState.STATE_RINGING;
                break;
            case OFFHOOK:
                state = TelephonyCallSession.Event.PhoneState.STATE_OFFHOOK;
                break;
            default:
                state = TelephonyCallSession.Event.PhoneState.STATE_UNKNOWN;
                break;
        }

        InProgressCallSession callSession = mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "writePhoneState: Call session is missing");
        } else {
            // For CS Calls Finish the Call Session after Receiving the Last Call Fail Cause
            // For IMS calls we receive the Disconnect Cause along with Call End event.
            // So we can finish the call session here.
            callSession.setLastKnownPhoneState(state);
            if ((state == TelephonyCallSession.Event.PhoneState.STATE_IDLE)
                    && (!callSession.containsCsCalls())) {
                finishCallSession(callSession);
            }
            callSession.addEvent(new CallSessionEventBuilder(
                    TelephonyCallSession.Event.Type.PHONE_STATE_CHANGED)
                    .setPhoneState(state));
        }
    }

    /**
     * Extracts the call ID from an ImsSession.
     *
     * @param session The session.
     * @return The call ID for the session, or -1 if none was found.
     */
    private int getCallId(ImsCallSession session) {
        if (session == null) {
            return -1;
        }

        try {
            return Integer.parseInt(session.getCallId());
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    /**
     * Write IMS call state changed event
     *
     * @param phoneId Phone id
     * @param session IMS call session
     * @param callState IMS call state
     */
    public void writeImsCallState(int phoneId, ImsCallSession session,
                                  ImsPhoneCall.State callState) {
        int state;
        switch (callState) {
            case IDLE:
                state = TelephonyCallSession.Event.CallState.CALL_IDLE; break;
            case ACTIVE:
                state = TelephonyCallSession.Event.CallState.CALL_ACTIVE; break;
            case HOLDING:
                state = TelephonyCallSession.Event.CallState.CALL_HOLDING; break;
            case DIALING:
                state = TelephonyCallSession.Event.CallState.CALL_DIALING; break;
            case ALERTING:
                state = TelephonyCallSession.Event.CallState.CALL_ALERTING; break;
            case INCOMING:
                state = TelephonyCallSession.Event.CallState.CALL_INCOMING; break;
            case WAITING:
                state = TelephonyCallSession.Event.CallState.CALL_WAITING; break;
            case DISCONNECTED:
                state = TelephonyCallSession.Event.CallState.CALL_DISCONNECTED; break;
            case DISCONNECTING:
                state = TelephonyCallSession.Event.CallState.CALL_DISCONNECTING; break;
            default:
                state = TelephonyCallSession.Event.CallState.CALL_UNKNOWN; break;
        }

        InProgressCallSession callSession = mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "Call session is missing");
        } else {
            callSession.addEvent(new CallSessionEventBuilder(
                    TelephonyCallSession.Event.Type.IMS_CALL_STATE_CHANGED)
                    .setCallIndex(getCallId(session))
                    .setCallState(state));
        }
    }

    /**
     * Write IMS call start event
     *
     * @param phoneId Phone id
     * @param session IMS call session
     */
    public void writeOnImsCallStart(int phoneId, ImsCallSession session) {
        InProgressCallSession callSession = startNewCallSessionIfNeeded(phoneId);

        callSession.addEvent(
                new CallSessionEventBuilder(TelephonyCallSession.Event.Type.IMS_COMMAND)
                        .setCallIndex(getCallId(session))
                        .setImsCommand(TelephonyCallSession.Event.ImsCommand.IMS_CMD_START));
    }

    /**
     * Write IMS incoming call event
     *
     * @param phoneId Phone id
     * @param session IMS call session
     */
    public void writeOnImsCallReceive(int phoneId, ImsCallSession session) {
        InProgressCallSession callSession = startNewCallSessionIfNeeded(phoneId);

        callSession.addEvent(
                new CallSessionEventBuilder(TelephonyCallSession.Event.Type.IMS_CALL_RECEIVE)
                        .setCallIndex(getCallId(session)));
    }

    /**
     * Write IMS command event
     *
     * @param phoneId Phone id
     * @param session IMS call session
     * @param command IMS command
     */
    public void writeOnImsCommand(int phoneId, ImsCallSession session, int command) {

        InProgressCallSession callSession =  mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "Call session is missing");
        } else {
            callSession.addEvent(
                    new CallSessionEventBuilder(TelephonyCallSession.Event.Type.IMS_COMMAND)
                            .setCallIndex(getCallId(session))
                            .setImsCommand(command));
        }
    }

    /**
     * Convert IMS reason info into proto
     *
     * @param reasonInfo IMS reason info
     * @return Converted proto
     */
    private TelephonyProto.ImsReasonInfo toImsReasonInfoProto(ImsReasonInfo reasonInfo) {
        TelephonyProto.ImsReasonInfo ri = new TelephonyProto.ImsReasonInfo();
        if (reasonInfo != null) {
            ri.reasonCode = reasonInfo.getCode();
            ri.extraCode = reasonInfo.getExtraCode();
            String extraMessage = reasonInfo.getExtraMessage();
            if (extraMessage != null) {
                ri.extraMessage = extraMessage;
            }
        }
        return ri;
    }

    /**
     * Convert CallQuality to proto.
     *
     * @param callQuality call quality to convert
     * @return Coverted proto
     */
    public static TelephonyCallSession.Event.CallQuality toCallQualityProto(
            CallQuality callQuality) {
        TelephonyCallSession.Event.CallQuality cq = new TelephonyCallSession.Event.CallQuality();
        if (callQuality != null) {
            cq.downlinkLevel = callQualityLevelToProtoEnum(callQuality
                    .getDownlinkCallQualityLevel());
            cq.uplinkLevel = callQualityLevelToProtoEnum(callQuality.getUplinkCallQualityLevel());
            // callDuration is reported in millis, so convert to seconds
            cq.durationInSeconds = callQuality.getCallDuration() / 1000;
            cq.rtpPacketsTransmitted = callQuality.getNumRtpPacketsTransmitted();
            cq.rtpPacketsReceived = callQuality.getNumRtpPacketsReceived();
            cq.rtpPacketsTransmittedLost = callQuality.getNumRtpPacketsTransmittedLost();
            cq.rtpPacketsNotReceived = callQuality.getNumRtpPacketsNotReceived();
            cq.averageRelativeJitterMillis = callQuality.getAverageRelativeJitter();
            cq.maxRelativeJitterMillis = callQuality.getMaxRelativeJitter();
            cq.codecType = convertImsCodec(callQuality.getCodecType());
            cq.rtpInactivityDetected = callQuality.isRtpInactivityDetected();
            cq.rxSilenceDetected = callQuality.isIncomingSilenceDetectedAtCallSetup();
            cq.txSilenceDetected = callQuality.isOutgoingSilenceDetectedAtCallSetup();
            cq.voiceFrames = callQuality.getNumVoiceFrames();
            cq.noDataFrames = callQuality.getNumNoDataFrames();
            cq.rtpDroppedPackets = callQuality.getNumDroppedRtpPackets();
            cq.minPlayoutDelayMillis = callQuality.getMinPlayoutDelayMillis();
            cq.maxPlayoutDelayMillis = callQuality.getMaxPlayoutDelayMillis();
            cq.rxRtpSidPackets = callQuality.getNumRtpSidPacketsReceived();
            cq.rtpDuplicatePackets = callQuality.getNumRtpDuplicatePackets();
        }
        return cq;
    }

    /**
     * Convert Call quality level into proto defined value.
     */
    private static int callQualityLevelToProtoEnum(int level) {
        if (level == CallQuality.CALL_QUALITY_EXCELLENT) {
            return TelephonyCallSession.Event.CallQuality.CallQualityLevel.EXCELLENT;
        } else if (level == CallQuality.CALL_QUALITY_GOOD) {
            return TelephonyCallSession.Event.CallQuality.CallQualityLevel.GOOD;
        } else if (level == CallQuality.CALL_QUALITY_FAIR) {
            return TelephonyCallSession.Event.CallQuality.CallQualityLevel.FAIR;
        } else if (level == CallQuality.CALL_QUALITY_POOR) {
            return TelephonyCallSession.Event.CallQuality.CallQualityLevel.POOR;
        } else if (level == CallQuality.CALL_QUALITY_BAD) {
            return TelephonyCallSession.Event.CallQuality.CallQualityLevel.BAD;
        } else if (level == CallQuality.CALL_QUALITY_NOT_AVAILABLE) {
            return TelephonyCallSession.Event.CallQuality.CallQualityLevel.NOT_AVAILABLE;
        } else {
            return TelephonyCallSession.Event.CallQuality.CallQualityLevel.UNDEFINED;
        }
    }

    /**
     * Write IMS call end event
     *
     * @param phoneId Phone id
     * @param session IMS call session
     * @param reasonInfo Call end reason
     * @param cqm Call Quality Metrics
     * @param emergencyNumber Emergency Number Info
     * @param countryIso Network country iso
     * @param emergencyNumberDatabaseVersion Emergency Number Database Version
     */
    public void writeOnImsCallTerminated(int phoneId, ImsCallSession session,
                                         ImsReasonInfo reasonInfo, CallQualityMetrics cqm,
                                         EmergencyNumber emergencyNumber, String countryIso,
                                         int emergencyNumberDatabaseVersion) {
        InProgressCallSession callSession = mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "Call session is missing");
        } else {
            CallSessionEventBuilder callSessionEvent = new CallSessionEventBuilder(
                    TelephonyCallSession.Event.Type.IMS_CALL_TERMINATED);
            callSessionEvent.setCallIndex(getCallId(session));
            callSessionEvent.setImsReasonInfo(toImsReasonInfoProto(reasonInfo));

            if (cqm != null) {
                callSessionEvent.setCallQualitySummaryDl(cqm.getCallQualitySummaryDl())
                        .setCallQualitySummaryUl(cqm.getCallQualitySummaryUl());
            }

            if (emergencyNumber != null) {
                /** Only collect this emergency number information per sample percentage */
                if (ThreadLocalRandom.current().nextDouble(0, 100)
                        < getSamplePercentageForEmergencyCall(countryIso)) {
                    callSessionEvent.setIsImsEmergencyCall(true);
                    callSessionEvent.setImsEmergencyNumberInfo(
                            convertEmergencyNumberToEmergencyNumberInfo(emergencyNumber));
                    callSessionEvent.setEmergencyNumberDatabaseVersion(
                            emergencyNumberDatabaseVersion);
                }
            }
            callSession.addEvent(callSessionEvent);
        }
    }

    /**
     * Write IMS call hangover event
     *
     * @param phoneId Phone id
     * @param eventType hangover type
     * @param session IMS call session
     * @param srcAccessTech Hangover starting RAT
     * @param targetAccessTech Hangover destination RAT
     * @param reasonInfo Hangover reason
     */
    public void writeOnImsCallHandoverEvent(int phoneId, int eventType, ImsCallSession session,
                                            int srcAccessTech, int targetAccessTech,
                                            ImsReasonInfo reasonInfo) {
        InProgressCallSession callSession = mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "Call session is missing");
        } else {
            callSession.addEvent(
                    new CallSessionEventBuilder(eventType)
                            .setCallIndex(getCallId(session))
                            .setSrcAccessTech(srcAccessTech)
                            .setTargetAccessTech(targetAccessTech)
                            .setImsReasonInfo(toImsReasonInfoProto(reasonInfo)));
        }
    }

    /**
     * Write Send SMS event
     *
     * @param phoneId Phone id
     * @param rilSerial RIL request serial number
     * @param tech SMS RAT
     * @param format SMS format. Either {@link SmsMessage#FORMAT_3GPP} or
     *         {@link SmsMessage#FORMAT_3GPP2}.
     * @param messageId Unique id for this message.
     */
    public synchronized void writeRilSendSms(int phoneId, int rilSerial, int tech, int format,
            long messageId) {
        InProgressSmsSession smsSession = startNewSmsSessionIfNeeded(phoneId);

        smsSession.addEvent(new SmsSessionEventBuilder(SmsSession.Event.Type.SMS_SEND)
                .setTech(tech)
                .setRilRequestId(rilSerial)
                .setFormat(format)
                .setMessageId(messageId)
        );

        smsSession.increaseExpectedResponse();
    }

    /**
     * Write Send SMS event using ImsService. Expecting response from
     * {@link #writeOnSmsSolicitedResponse}.
     *
     * @param phoneId Phone id
     * @param format SMS format. Either {@link SmsMessage#FORMAT_3GPP} or
     *         {@link SmsMessage#FORMAT_3GPP2}.
     * @param resultCode The result of sending the new SMS to the vendor layer to be sent to the
     *         carrier network.
     * @param messageId Unique id for this message.
     */
    public synchronized void writeImsServiceSendSms(int phoneId, String format,
            @ImsSmsImplBase.SendStatusResult int resultCode, long messageId) {
        InProgressSmsSession smsSession = startNewSmsSessionIfNeeded(phoneId);
        smsSession.addEvent(new SmsSessionEventBuilder(SmsSession.Event.Type.SMS_SEND)
                .setTech(SmsSession.Event.Tech.SMS_IMS)
                .setImsServiceErrno(resultCode)
                .setFormat(convertSmsFormat(format))
                .setMessageId(messageId)
        );

        smsSession.increaseExpectedResponse();
    }

    /**
     * Write incoming Broadcast SMS event
     *
     * @param phoneId Phone id
     * @param format CB msg format
     * @param priority CB msg priority
     * @param isCMAS true if msg is CMAS
     * @param isETWS true if msg is ETWS
     * @param serviceCategory Service category of CB msg
     * @param serialNumber Serial number of the message
     * @param deliveredTimestamp Message's delivered timestamp
     */
    public synchronized void writeNewCBSms(int phoneId, int format, int priority, boolean isCMAS,
                                           boolean isETWS, int serviceCategory, int serialNumber,
                                           long deliveredTimestamp) {
        InProgressSmsSession smsSession = startNewSmsSessionIfNeeded(phoneId);

        int type;
        if (isCMAS) {
            type = SmsSession.Event.CBMessageType.CMAS;
        } else if (isETWS) {
            type = SmsSession.Event.CBMessageType.ETWS;
        } else {
            type = SmsSession.Event.CBMessageType.OTHER;
        }

        SmsSession.Event.CBMessage cbm = new SmsSession.Event.CBMessage();
        cbm.msgFormat = format;
        cbm.msgPriority = priority + 1;
        cbm.msgType = type;
        cbm.serviceCategory = serviceCategory;
        cbm.serialNumber = serialNumber;
        cbm.deliveredTimestampMillis = deliveredTimestamp;

        smsSession.addEvent(new SmsSessionEventBuilder(SmsSession.Event.Type.CB_SMS_RECEIVED)
                .setCellBroadcastMessage(cbm)
        );

        finishSmsSessionIfNeeded(smsSession);
    }

    /**
     * Write an incoming multi-part SMS that was discarded because some parts were missing
     *
     * @param phoneId Phone id
     * @param format SMS format. Either {@link SmsMessage#FORMAT_3GPP} or
     *         {@link SmsMessage#FORMAT_3GPP2}.
     * @param receivedCount Number of received parts.
     * @param totalCount Total number of parts of the SMS.
     */
    public void writeDroppedIncomingMultipartSms(int phoneId, String format,
            int receivedCount, int totalCount) {
        logv("Logged dropped multipart SMS: received " + receivedCount
                + " out of " + totalCount);

        SmsSession.Event.IncompleteSms details = new SmsSession.Event.IncompleteSms();
        details.receivedParts = receivedCount;
        details.totalParts = totalCount;

        InProgressSmsSession smsSession = startNewSmsSession(phoneId);
        smsSession.addEvent(
                new SmsSessionEventBuilder(SmsSession.Event.Type.INCOMPLETE_SMS_RECEIVED)
                    .setFormat(convertSmsFormat(format))
                    .setIncompleteSms(details));

        finishSmsSession(smsSession);
    }

    /**
     * Write a generic SMS of any type
     *
     * @param phoneId Phone id
     * @param type Type of the SMS.
     * @param format SMS format. Either {@link SmsMessage#FORMAT_3GPP} or
     *         {@link SmsMessage#FORMAT_3GPP2}.
     * @param success Indicates if the SMS-PP was successfully delivered to the USIM.
     */
    private void writeIncomingSmsWithType(int phoneId, int type, String format, boolean success) {
        InProgressSmsSession smsSession = startNewSmsSession(phoneId);
        smsSession.addEvent(new SmsSessionEventBuilder(SmsSession.Event.Type.SMS_RECEIVED)
                .setFormat(convertSmsFormat(format))
                .setSmsType(type)
                .setErrorCode(success ? SmsManager.RESULT_ERROR_NONE :
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE));
        finishSmsSession(smsSession);
    }

    /**
     * Write an incoming SMS-PP for the USIM
     *
     * @param phoneId Phone id
     * @param format SMS format. Either {@link SmsMessage#FORMAT_3GPP} or
     *         {@link SmsMessage#FORMAT_3GPP2}.
     * @param success Indicates if the SMS-PP was successfully delivered to the USIM.
     */
    public void writeIncomingSMSPP(int phoneId, String format, boolean success) {
        logv("Logged SMS-PP session. Result = " + success);
        writeIncomingSmsWithType(phoneId,
                SmsSession.Event.SmsType.SMS_TYPE_SMS_PP, format, success);
    }

    /**
     * Write an incoming SMS to update voicemail indicator
     *
     * @param phoneId Phone id
     * @param format SMS format. Either {@link SmsMessage#FORMAT_3GPP} or
     *         {@link SmsMessage#FORMAT_3GPP2}.
     */
    public void writeIncomingVoiceMailSms(int phoneId, String format) {
        logv("Logged VoiceMail message.");
        writeIncomingSmsWithType(phoneId,
                SmsSession.Event.SmsType.SMS_TYPE_VOICEMAIL_INDICATION, format, true);
    }

    /**
     * Write an incoming SMS of type 0
     *
     * @param phoneId Phone id
     * @param format SMS format. Either {@link SmsMessage#FORMAT_3GPP} or
     *         {@link SmsMessage#FORMAT_3GPP2}.
     */
    public void writeIncomingSmsTypeZero(int phoneId, String format) {
        logv("Logged Type-0 SMS message.");
        writeIncomingSmsWithType(phoneId,
                SmsSession.Event.SmsType.SMS_TYPE_ZERO, format, true);
    }

    /**
     * Write a successful incoming SMS session
     *
     * @param phoneId Phone id
     * @param type Type of the SMS.
     * @param smsSource the source of the SMS message
     * @param format SMS format. Either {@link SmsMessage#FORMAT_3GPP} or
     *         {@link SmsMessage#FORMAT_3GPP2}.
     * @param timestamps array with timestamps of each incoming SMS part. It contains a single
     * @param blocked indicates if the message was blocked or not.
     * @param success Indicates if the SMS-PP was successfully delivered to the USIM.
     * @param messageId Unique id for this message.
     */
    private void writeIncomingSmsSessionWithType(int phoneId, int type,
            @InboundSmsHandler.SmsSource int smsSource, String format, long[] timestamps,
            boolean blocked, boolean success, long messageId) {
        logv("Logged SMS session consisting of " + timestamps.length
                + " parts, source = " + smsSource
                + " blocked = " + blocked
                + " type = " + type
                + " " + SmsController.formatCrossStackMessageId(messageId));

        int smsFormat = convertSmsFormat(format);
        int smsError =
                success ? SmsManager.RESULT_ERROR_NONE : SmsManager.RESULT_ERROR_GENERIC_FAILURE;
        int smsTech = getSmsTech(smsSource, smsFormat == SmsSession.Event.Format.SMS_FORMAT_3GPP2);

        InProgressSmsSession smsSession = startNewSmsSession(phoneId);

        long startElapsedTimeMillis = SystemClock.elapsedRealtime();
        for (int i = 0; i < timestamps.length; i++) {
            SmsSessionEventBuilder eventBuilder =
                    new SmsSessionEventBuilder(SmsSession.Event.Type.SMS_RECEIVED)
                        .setFormat(smsFormat)
                        .setTech(smsTech)
                        .setErrorCode(smsError)
                        .setSmsType(type)
                        .setBlocked(blocked)
                        .setMessageId(messageId);
            long interval = (i > 0) ? timestamps[i] - timestamps[i - 1] : 0;
            smsSession.addEvent(startElapsedTimeMillis + interval, eventBuilder);
        }
        finishSmsSession(smsSession);
    }

    /**
     * Write an incoming WAP-PUSH message.
     *
     * @param phoneId Phone id
     * @param smsSource the source of the SMS message
     * @param format SMS format. Either {@link SmsMessage#FORMAT_3GPP} or
     *         {@link SmsMessage#FORMAT_3GPP2}.
     * @param timestamps array with timestamps of each incoming SMS part. It contains a single
     * @param success Indicates if the SMS-PP was successfully delivered to the USIM.
     * @param messageId Unique id for this message.
     */
    public void writeIncomingWapPush(int phoneId, @InboundSmsHandler.SmsSource int smsSource,
            String format, long[] timestamps, boolean success, long messageId) {
        writeIncomingSmsSessionWithType(phoneId, SmsSession.Event.SmsType.SMS_TYPE_WAP_PUSH,
                smsSource, format, timestamps, false, success, messageId);
    }

    /**
     * Write a successful incoming SMS session
     *
     * @param phoneId Phone id
     * @param smsSource the source of the SMS message
     * @param format SMS format. Either {@link SmsMessage#FORMAT_3GPP} or
     *         {@link SmsMessage#FORMAT_3GPP2}.
     * @param timestamps array with timestamps of each incoming SMS part. It contains a single
     * @param blocked indicates if the message was blocked or not.
     * @param messageId Unique id for this message.
     */
    public void writeIncomingSmsSession(int phoneId, @InboundSmsHandler.SmsSource int smsSource,
            String format, long[] timestamps, boolean blocked, long messageId) {
        writeIncomingSmsSessionWithType(phoneId, SmsSession.Event.SmsType.SMS_TYPE_NORMAL,
                smsSource, format, timestamps, blocked, true, messageId);
    }

    /**
     * Write an error incoming SMS
     *
     * @param phoneId Phone id
     * @param is3gpp2 true for 3GPP2 format, false for 3GPP format.
     * @param smsSource the source of the SMS message
     * @param result Indicates the reason of the failure.
     */
    public void writeIncomingSmsError(int phoneId, boolean is3gpp2,
            @InboundSmsHandler.SmsSource int smsSource, int result) {
        logv("Incoming SMS error = " + result);

        int smsError = SmsManager.RESULT_ERROR_GENERIC_FAILURE;
        switch (result) {
            case Intents.RESULT_SMS_HANDLED:
                // This should not happen.
                return;
            case Intents.RESULT_SMS_OUT_OF_MEMORY:
                smsError = SmsManager.RESULT_NO_MEMORY;
                break;
            case Intents.RESULT_SMS_UNSUPPORTED:
                smsError = SmsManager.RESULT_REQUEST_NOT_SUPPORTED;
                break;
            case Intents.RESULT_SMS_GENERIC_ERROR:
            default:
                smsError = SmsManager.RESULT_ERROR_GENERIC_FAILURE;
                break;
        }

        InProgressSmsSession smsSession = startNewSmsSession(phoneId);

        SmsSessionEventBuilder eventBuilder =
                new SmsSessionEventBuilder(SmsSession.Event.Type.SMS_RECEIVED)
                    .setFormat(is3gpp2
                                ? SmsSession.Event.Format.SMS_FORMAT_3GPP2
                                : SmsSession.Event.Format.SMS_FORMAT_3GPP)
                    .setTech(getSmsTech(smsSource, is3gpp2))
                    .setErrorCode(smsError);
        smsSession.addEvent(eventBuilder);
        finishSmsSession(smsSession);
    }

    /**
     * Write NITZ event
     *
     * @param phoneId Phone id
     * @param timestamp NITZ time in milliseconds
     */
    public void writeNITZEvent(int phoneId, long timestamp) {
        TelephonyEvent event = new TelephonyEventBuilder(phoneId).setNITZ(timestamp).build();
        addTelephonyEvent(event);

        annotateInProgressCallSession(event.timestampMillis, phoneId,
                new CallSessionEventBuilder(
                        TelephonyCallSession.Event.Type.NITZ_TIME)
                        .setNITZ(timestamp));
    }

    /**
     * Write Modem Restart event
     *
     * @param phoneId Phone id
     * @param reason Reason for the modem reset.
     */
    public void writeModemRestartEvent(int phoneId, String reason) {
        final ModemRestart modemRestart = new ModemRestart();
        String basebandVersion = Build.getRadioVersion();
        if (basebandVersion != null) modemRestart.basebandVersion = basebandVersion;
        if (reason != null) modemRestart.reason = reason;
        TelephonyEvent event = new TelephonyEventBuilder(phoneId).setModemRestart(
                modemRestart).build();
        addTelephonyEvent(event);
    }

    /**
     * Write carrier identification matching event
     *
     * @param phoneId Phone id
     * @param version Carrier table version
     * @param cid Unique Carrier Id
     * @param unknownMcmnc MCC and MNC that map to this carrier
     * @param unknownGid1 Group id level 1
     * @param simInfo Subscription info
     */
    public void writeCarrierIdMatchingEvent(int phoneId, int version, int cid,
                                            String unknownMcmnc, String unknownGid1,
                                            CarrierResolver.CarrierMatchingRule simInfo) {
        final CarrierIdMatching carrierIdMatching = new CarrierIdMatching();
        final CarrierIdMatchingResult carrierIdMatchingResult = new CarrierIdMatchingResult();

        // fill in information for unknown mccmnc and gid1 for unidentified carriers.
        if (cid != TelephonyManager.UNKNOWN_CARRIER_ID) {
            // Successful matching event if result only has carrierId
            carrierIdMatchingResult.carrierId = cid;
            // Unknown Gid1 event if result only has carrierId, gid1 and mccmnc
            if (unknownGid1 != null) {
                carrierIdMatchingResult.unknownMccmnc = unknownMcmnc;
                carrierIdMatchingResult.unknownGid1 = unknownGid1;
            }
        } else {
            // Unknown mccmnc event if result only has mccmnc
            if (unknownMcmnc != null) {
                carrierIdMatchingResult.unknownMccmnc = unknownMcmnc;
            }
        }

        // fill in complete matching information from the SIM.
        carrierIdMatchingResult.mccmnc = TelephonyUtils.emptyIfNull(simInfo.mccMnc);
        carrierIdMatchingResult.spn = TelephonyUtils.emptyIfNull(simInfo.spn);
        carrierIdMatchingResult.pnn = TelephonyUtils.emptyIfNull(simInfo.plmn);
        carrierIdMatchingResult.gid1 = TelephonyUtils.emptyIfNull(simInfo.gid1);
        carrierIdMatchingResult.gid2 = TelephonyUtils.emptyIfNull(simInfo.gid2);
        carrierIdMatchingResult.imsiPrefix = TelephonyUtils.emptyIfNull(simInfo.imsiPrefixPattern);
        carrierIdMatchingResult.iccidPrefix = TelephonyUtils.emptyIfNull(simInfo.iccidPrefix);
        carrierIdMatchingResult.preferApn = TelephonyUtils.emptyIfNull(simInfo.apn);
        if (simInfo.privilegeAccessRule != null) {
            carrierIdMatchingResult.privilegeAccessRule =
                    simInfo.privilegeAccessRule.stream().toArray(String[]::new);
        }

        carrierIdMatching.cidTableVersion = version;
        carrierIdMatching.result = carrierIdMatchingResult;

        TelephonyEvent event = new TelephonyEventBuilder(phoneId).setCarrierIdMatching(
                carrierIdMatching).build();
        mLastCarrierId.put(phoneId, carrierIdMatching);
        addTelephonyEvent(event);
    }

    /**
     * Write emergency number update event
     *
     * @param emergencyNumber Updated emergency number
     */
    public void writeEmergencyNumberUpdateEvent(int phoneId, EmergencyNumber emergencyNumber,
            int emergencyNumberDatabaseVersion) {
        if (emergencyNumber == null) {
            return;
        }
        final EmergencyNumberInfo emergencyNumberInfo =
                convertEmergencyNumberToEmergencyNumberInfo(emergencyNumber);

        TelephonyEvent event = new TelephonyEventBuilder(phoneId).setUpdatedEmergencyNumber(
                emergencyNumberInfo, emergencyNumberDatabaseVersion).build();
        addTelephonyEvent(event);
    }

    /**
     * Write network capabilities changed event
     *
     * @param phoneId Phone id
     * @param networkCapabilities Network capabilities
     */
    public void writeNetworkCapabilitiesChangedEvent(int phoneId,
            NetworkCapabilities networkCapabilities) {
        final NetworkCapabilitiesInfo caps = new NetworkCapabilitiesInfo();
        caps.isNetworkUnmetered = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED);

        TelephonyEvent event = new TelephonyEventBuilder(phoneId)
                .setNetworkCapabilities(caps).build();
        mLastNetworkCapabilitiesInfos.put(phoneId, caps);
        addTelephonyEvent(event);
    }

    /** Write radio state changed event */
    public void writeRadioState(int phoneId, @RadioPowerState int state) {
        int radioState = convertRadioState(state);
        TelephonyEvent event = new TelephonyEventBuilder(phoneId).setRadioState(radioState).build();
        mLastRadioState.put(phoneId, radioState);
        addTelephonyEvent(event);
    }

    private static int convertRadioState(@RadioPowerState int state) {
        switch (state) {
            case TelephonyManager.RADIO_POWER_OFF:
                return RadioState.RADIO_STATE_OFF;
            case TelephonyManager.RADIO_POWER_ON:
                return RadioState.RADIO_STATE_ON;
            case TelephonyManager.RADIO_POWER_UNAVAILABLE:
                return RadioState.RADIO_STATE_UNAVAILABLE;
            default:
                return RadioState.RADIO_STATE_UNKNOWN;
        }
    }

    /**
     * Convert SMS format
     */
    private int convertSmsFormat(String format) {
        int formatCode = SmsSession.Event.Format.SMS_FORMAT_UNKNOWN;
        switch (format) {
            case SmsMessage.FORMAT_3GPP : {
                formatCode = SmsSession.Event.Format.SMS_FORMAT_3GPP;
                break;
            }
            case SmsMessage.FORMAT_3GPP2: {
                formatCode = SmsSession.Event.Format.SMS_FORMAT_3GPP2;
                break;
            }
        }
        return formatCode;
    }

    /**
     * Get SMS technology
     */
    private int getSmsTech(@InboundSmsHandler.SmsSource int smsSource, boolean is3gpp2) {
        if (smsSource == SOURCE_INJECTED_FROM_IMS) {
            return SmsSession.Event.Tech.SMS_IMS;
        } else if (smsSource == SOURCE_NOT_INJECTED) {
            return is3gpp2 ? SmsSession.Event.Tech.SMS_CDMA : SmsSession.Event.Tech.SMS_GSM;
        } else { // SOURCE_INJECTED_FROM_UNKNOWN
            return SmsSession.Event.Tech.SMS_UNKNOWN;
        }
    }

    /**
     * Convert IMS audio codec into proto defined value
     *
     * @param c IMS codec value
     * @return Codec value defined in call session proto
     */
    private static int convertImsCodec(int c) {
        switch (c) {
            case ImsStreamMediaProfile.AUDIO_QUALITY_AMR:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_AMR;
            case ImsStreamMediaProfile.AUDIO_QUALITY_AMR_WB:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_AMR_WB;
            case ImsStreamMediaProfile.AUDIO_QUALITY_QCELP13K:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_QCELP13K;
            case ImsStreamMediaProfile.AUDIO_QUALITY_EVRC:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVRC;
            case ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_B:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVRC_B;
            case ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_WB:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVRC_WB;
            case ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_NW:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVRC_NW;
            case ImsStreamMediaProfile.AUDIO_QUALITY_GSM_EFR:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_GSM_EFR;
            case ImsStreamMediaProfile.AUDIO_QUALITY_GSM_FR:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_GSM_FR;
            case ImsStreamMediaProfile.AUDIO_QUALITY_GSM_HR:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_GSM_HR;
            case ImsStreamMediaProfile.AUDIO_QUALITY_G711U:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_G711U;
            case ImsStreamMediaProfile.AUDIO_QUALITY_G723:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_G723;
            case ImsStreamMediaProfile.AUDIO_QUALITY_G711A:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_G711A;
            case ImsStreamMediaProfile.AUDIO_QUALITY_G722:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_G722;
            case ImsStreamMediaProfile.AUDIO_QUALITY_G711AB:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_G711AB;
            case ImsStreamMediaProfile.AUDIO_QUALITY_G729:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_G729;
            case ImsStreamMediaProfile.AUDIO_QUALITY_EVS_NB:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVS_NB;
            case ImsStreamMediaProfile.AUDIO_QUALITY_EVS_WB:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVS_WB;
            case ImsStreamMediaProfile.AUDIO_QUALITY_EVS_SWB:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVS_SWB;
            case ImsStreamMediaProfile.AUDIO_QUALITY_EVS_FB:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVS_FB;
            default:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_UNKNOWN;
        }
    }

    /**
     * Convert GSM/CDMA audio codec into proto defined value
     *
     * @param c GSM/CDMA codec value
     * @return Codec value defined in call session proto
     */
    private int convertGsmCdmaCodec(int c) {
        switch (c) {
            case DriverCall.AUDIO_QUALITY_AMR:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_AMR;
            case DriverCall.AUDIO_QUALITY_AMR_WB:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_AMR_WB;
            case DriverCall.AUDIO_QUALITY_GSM_EFR:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_GSM_EFR;
            case DriverCall.AUDIO_QUALITY_GSM_FR:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_GSM_FR;
            case DriverCall.AUDIO_QUALITY_GSM_HR:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_GSM_HR;
            case DriverCall.AUDIO_QUALITY_EVRC:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVRC;
            case DriverCall.AUDIO_QUALITY_EVRC_B:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVRC_B;
            case DriverCall.AUDIO_QUALITY_EVRC_WB:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVRC_WB;
            case DriverCall.AUDIO_QUALITY_EVRC_NW:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_EVRC_NW;
            default:
                return TelephonyCallSession.Event.AudioCodec.AUDIO_CODEC_UNKNOWN;
        }
    }

    /**
     * Write audio codec event
     *
     * @param phoneId Phone id
     * @param session IMS call session
     */
    public void writeAudioCodecIms(int phoneId, ImsCallSession session) {
        InProgressCallSession callSession = mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "Call session is missing");
            return;
        }

        ImsCallProfile localCallProfile = session.getLocalCallProfile();
        if (localCallProfile != null) {
            int codec = convertImsCodec(localCallProfile.mMediaProfile.mAudioQuality);
            callSession.addEvent(new CallSessionEventBuilder(
                    TelephonyCallSession.Event.Type.AUDIO_CODEC)
                    .setCallIndex(getCallId(session))
                    .setAudioCodec(codec));

            logv("Logged Audio Codec event. Value: " + codec);
        }
    }

    /**
     * Write audio codec event
     *
     * @param phoneId Phone id
     * @param audioQuality Audio quality value
     */
    public void writeAudioCodecGsmCdma(int phoneId, int audioQuality) {
        InProgressCallSession callSession = mInProgressCallSessions.get(phoneId);
        if (callSession == null) {
            Rlog.e(TAG, "Call session is missing");
            return;
        }

        int codec = convertGsmCdmaCodec(audioQuality);
        callSession.addEvent(new CallSessionEventBuilder(
                TelephonyCallSession.Event.Type.AUDIO_CODEC)
                .setAudioCodec(codec));

        logv("Logged Audio Codec event. Value: " + codec);
    }

    //TODO: Expand the proto in the future
    public void writeOnImsCallInitiating(int phoneId, ImsCallSession session) {}
    public void writeOnImsCallProgressing(int phoneId, ImsCallSession session) {}
    public void writeOnImsCallStarted(int phoneId, ImsCallSession session) {}
    public void writeOnImsCallStartFailed(int phoneId, ImsCallSession session,
                                          ImsReasonInfo reasonInfo) {}
    public void writeOnImsCallHeld(int phoneId, ImsCallSession session) {}
    public void writeOnImsCallHoldReceived(int phoneId, ImsCallSession session) {}
    public void writeOnImsCallHoldFailed(int phoneId, ImsCallSession session,
                                         ImsReasonInfo reasonInfo) {}
    public void writeOnImsCallResumed(int phoneId, ImsCallSession session) {}
    public void writeOnImsCallResumeReceived(int phoneId, ImsCallSession session) {}
    public void writeOnImsCallResumeFailed(int phoneId, ImsCallSession session,
                                           ImsReasonInfo reasonInfo) {}
    public void writeOnRilTimeoutResponse(int phoneId, int rilSerial, int rilRequest) {}

    /**
     * Get the sample percentage of collecting metrics based on countries' population.
     *
     * The larger population the country has, the lower percentage we use to collect this
     * metrics. Since the exact population changes frequently, buckets of the population are used
     * instead of its exact number. Seven different levels of sampling percentage are assigned
     * based on the scale of population for countries.
     */
    private double getSamplePercentageForEmergencyCall(String countryIso) {
        String countriesFor1Percentage = "cn,in";
        String countriesFor5Percentage = "us,id,br,pk,ng,bd,ru,mx,jp,et,ph,eg,vn,cd,tr,ir,de";
        String countriesFor15Percentage = "th,gb,fr,tz,it,za,mm,ke,kr,co,es,ug,ar,ua,dz,sd,iq";
        String countriesFor25Percentage = "pl,ca,af,ma,sa,pe,uz,ve,my,ao,mz,gh,np,ye,mg,kp,cm";
        String countriesFor35Percentage = "au,tw,ne,lk,bf,mw,ml,ro,kz,sy,cl,zm,gt,zw,nl,ec,sn";
        String countriesFor45Percentage = "kh,td,so,gn,ss,rw,bj,tn,bi,be,cu,bo,ht,gr,do,cz,pt";
        if (countriesFor1Percentage.contains(countryIso)) {
            return 1;
        } else if (countriesFor5Percentage.contains(countryIso)) {
            return 5;
        } else if (countriesFor15Percentage.contains(countryIso)) {
            return 15;
        } else if (countriesFor25Percentage.contains(countryIso)) {
            return 25;
        } else if (countriesFor35Percentage.contains(countryIso)) {
            return 35;
        } else if (countriesFor45Percentage.contains(countryIso)) {
            return 45;
        } else {
            return 50;
        }
    }

    private static int mapSimStateToProto(int simState) {
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                return SimState.SIM_STATE_ABSENT;
            case TelephonyManager.SIM_STATE_LOADED:
                return SimState.SIM_STATE_LOADED;
            default:
                return SimState.SIM_STATE_UNKNOWN;
        }
    }

    /**
     * Write bandwidth estimator stats
     */
    public synchronized void writeBandwidthStats(int link, int rat, int nrMode,
            int signalLevel, int bwEstExtErrPercent, int coldStartErrPercent, int bwKbps) {
        BwEstimationStats stats = lookupEstimationStats(link, rat, nrMode);
        stats.mBwEstErrorAcc[signalLevel] += Math.abs(bwEstExtErrPercent);
        stats.mStaticBwErrorAcc[signalLevel] += Math.abs(coldStartErrPercent);
        stats.mBwAccKbps[signalLevel] += bwKbps;
        stats.mCount[signalLevel]++;
    }

    private BwEstimationStats lookupEstimationStats(int linkIndex, int dataRat, int nrMode) {
        String dataRatName = LinkBandwidthEstimator.getDataRatName(dataRat, nrMode);
        BwEstimationStats ans = mBwEstStatsMapList.get(linkIndex).get(dataRatName);
        if (ans == null) {
            ans = new BwEstimationStats(dataRat, nrMode);
            mBwEstStatsMapList.get(linkIndex).put(dataRatName, ans);
        }
        return ans;
    }

    private BandwidthEstimatorStats buildBandwidthEstimatorStats() {
        BandwidthEstimatorStats stats = new BandwidthEstimatorStats();
        List<BandwidthEstimatorStats.PerRat> ratList;
        ratList = writeBandwidthEstimatorStatsRatList(mBwEstStatsMapList.get(0));
        stats.perRatTx = ratList.toArray(new BandwidthEstimatorStats.PerRat[0]);
        ratList = writeBandwidthEstimatorStatsRatList(mBwEstStatsMapList.get(1));
        stats.perRatRx = ratList.toArray(new BandwidthEstimatorStats.PerRat[0]);
        return stats;
    }

    private List<BandwidthEstimatorStats.PerRat> writeBandwidthEstimatorStatsRatList(
            Map<String, BwEstimationStats> bwEstStatsMap) {
        List<BandwidthEstimatorStats.PerRat> ratList = new ArrayList<>();
        for (BwEstimationStats perRat : bwEstStatsMap.values()) {
            ratList.add(perRat.writeBandwidthStats());
        }
        return ratList;
    }

    private static class BwEstimationStats {
        final int mRadioTechnology;
        final int mNrMode;
        final long[] mBwEstErrorAcc = new long[NUM_SIGNAL_LEVEL];
        final long[] mStaticBwErrorAcc = new long[NUM_SIGNAL_LEVEL];
        final long[] mBwAccKbps = new long[NUM_SIGNAL_LEVEL];
        final int[] mCount = new int[NUM_SIGNAL_LEVEL];

        BwEstimationStats(int radioTechnology, int nrMode) {
            mRadioTechnology = radioTechnology;
            mNrMode = nrMode;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            return sb.append(LinkBandwidthEstimator.getDataRatName(mRadioTechnology, mNrMode))
                    .append("\n Count\n").append(printValues(mCount))
                    .append("\n AvgKbps\n").append(printAvgValues(mBwAccKbps, mCount))
                    .append("\n BwEst Error\n").append(printAvgValues(mBwEstErrorAcc, mCount))
                    .append("\n StaticBw Error\n").append(printAvgValues(mStaticBwErrorAcc, mCount))
                    .toString();
        }

        private String printValues(int[] values) {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < NUM_SIGNAL_LEVEL; k++) {
                sb.append(" " + values[k]);
            }
            return sb.toString();
        }

        private String printAvgValues(long[] stats, int[] count) {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < NUM_SIGNAL_LEVEL; k++) {
                int avgStat = calculateAvg(stats[k], count[k]);
                sb.append(" " + avgStat);
            }
            return sb.toString();
        }

        private BandwidthEstimatorStats.PerRat writeBandwidthStats() {
            BandwidthEstimatorStats.PerRat stats = new BandwidthEstimatorStats.PerRat();
            List<BandwidthEstimatorStats.PerLevel> levelList = new ArrayList<>();
            for (int level = 0; level < NUM_SIGNAL_LEVEL; level++) {
                BandwidthEstimatorStats.PerLevel currStats = writeBandwidthStatsPerLevel(level);
                if (currStats != null) {
                    levelList.add(currStats);
                }
            }
            stats.rat = mRadioTechnology;
            stats.perLevel = levelList.toArray(new BandwidthEstimatorStats.PerLevel[0]);
            stats.nrMode = mNrMode;
            return stats;
        }

        private BandwidthEstimatorStats.PerLevel writeBandwidthStatsPerLevel(int level) {
            int count = mCount[level];
            if (count > 0) {
                BandwidthEstimatorStats.PerLevel stats = new BandwidthEstimatorStats.PerLevel();
                stats.signalLevel = level;
                stats.count = count;
                stats.avgBwKbps = calculateAvg(mBwAccKbps[level], count);
                stats.staticBwErrorPercent = calculateAvg(mStaticBwErrorAcc[level], count);
                stats.bwEstErrorPercent = calculateAvg(mBwEstErrorAcc[level], count);
                return stats;
            }
            return null;
        }

        private int calculateAvg(long acc, int count) {
            return (count > 0) ? (int) (acc / count) : 0;
        }
    }

}

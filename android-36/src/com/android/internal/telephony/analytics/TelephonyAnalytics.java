/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.internal.telephony.analytics;

import static com.android.internal.telephony.InboundSmsHandler.SOURCE_INJECTED_FROM_IMS;
import static com.android.internal.telephony.InboundSmsHandler.SOURCE_INJECTED_FROM_UNKNOWN;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__ERROR__SMS_ERROR_GENERIC;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__ERROR__SMS_ERROR_NOT_SUPPORTED;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__ERROR__SMS_ERROR_NO_MEMORY;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__ERROR__SMS_SUCCESS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.Telephony.Sms.Intents;
import android.telephony.Annotation;
import android.telephony.DisconnectCause;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.stub.ImsRegistrationImplBase;

import com.android.internal.os.BackgroundThread;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.flags.Flags;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class to handle all telephony analytics related operations Initializes all the Analytics,
 * Provider and Util classes. Registers the required Callbacks for supporting the
 * ServiceStateAnalytics , SMS Analytics and Call Analytics
 */
public class TelephonyAnalytics {
    private static final String TAG = TelephonyAnalytics.class.getSimpleName();
    protected static final int INVALID_SUB_ID = -1;
    private final int mSlotIndex;
    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private ExecutorService mExecutorService;
    protected TelephonyAnalyticsUtil mTelephonyAnalyticsUtil;
    protected int mSubId;
    protected ServiceStateAnalytics mServiceStateAnalytics;
    protected Context mContext;
    protected Executor mExecutor;
    protected SubscriptionManager mSubscriptionManager;
    protected final SubscriptionManager.OnSubscriptionsChangedListener
            mSubscriptionsChangeListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                    int newSubId = getSubId();
                    if ((mSubId != newSubId)
                            && (newSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID)) {
                        stopAnalytics(mSubId);
                        mSubId = newSubId;
                        startAnalytics(newSubId);
                        Rlog.d(
                                TAG,
                                "Started Listener, mSubId = "
                                        + mSubId
                                        + "SlotId = "
                                        + mSlotIndex);
                    }
                }
            };
    protected CallAnalyticsProvider mCallAnalyticsProvider;
    protected SmsMmsAnalyticsProvider mSmsMmsAnalyticsProvider;
    protected ServiceStateAnalyticsProvider mServiceStateAnalyticsProvider;
    protected SmsMmsAnalytics mSmsMmsAnalytics;
    protected CallAnalytics mCallAnalytics;
    protected Phone mPhone;

    public TelephonyAnalytics(Phone phone) {
        mPhone = phone;
        mContext = mPhone.getContext();
        mExecutor = Runnable::run;
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mSlotIndex = mPhone.getPhoneId();

        if (Flags.threadShred()) {
            mHandlerThread = null; // TODO: maybe this doesn't need to be a member variable
            mHandler = new Handler(BackgroundThread.get().getLooper());
        } else {
            mHandlerThread = new HandlerThread(TelephonyAnalytics.class.getSimpleName());
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
        }
        mExecutorService = Executors.newSingleThreadExecutor();
        mTelephonyAnalyticsUtil = TelephonyAnalyticsUtil.getInstance(mContext);
        initializeAnalyticsClasses();
        mCallAnalyticsProvider = new CallAnalyticsProvider(mTelephonyAnalyticsUtil, mSlotIndex);
        mSmsMmsAnalyticsProvider = new SmsMmsAnalyticsProvider(mTelephonyAnalyticsUtil, mSlotIndex);
        mServiceStateAnalyticsProvider =
                new ServiceStateAnalyticsProvider(mTelephonyAnalyticsUtil, mSlotIndex);

        startAnalytics(mSubId);

        if (mSubscriptionManager != null) {
            mSubscriptionManager.addOnSubscriptionsChangedListener(
                    mExecutor, mSubscriptionsChangeListener);
            Rlog.d(TAG, "stopped listener");
        }
    }

    @SuppressLint("MissingPermission")
    private int getSubId() {
        int subId = INVALID_SUB_ID;
        try {
            SubscriptionInfo info =
                    mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(mSlotIndex);
            subId = info.getSubscriptionId();
            Rlog.d("TelephonyAnalyticsSubId", "SubId = " + subId
                    + "SlotIndex = " + mSlotIndex);
        } catch (NullPointerException e) {
            Rlog.e("TelephonyAnalyticsSubId", "Null Pointer Exception Caught");
        }
        return subId;
    }

    private void initializeAnalyticsClasses() {
        mServiceStateAnalytics = new ServiceStateAnalytics(mExecutor);
        mSmsMmsAnalytics = new SmsMmsAnalytics();
        mCallAnalytics = new CallAnalytics();
    }

    protected void startAnalytics(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Rlog.d(
                    "StartAnalytics",
                    "Invalid SubId = " + SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            return;
        }
        mServiceStateAnalytics.registerMyListener(mContext, subId);
    }

    protected void stopAnalytics(int subId) {
        if (mServiceStateAnalytics != null) {
            mServiceStateAnalytics.unregisterMyListener(subId);
        }
    }

    public SmsMmsAnalytics getSmsMmsAnalytics() {
        return mSmsMmsAnalytics;
    }

    public CallAnalytics getCallAnalytics() {
        return mCallAnalytics;
    }

    /**
     * Uses the provider class objects,collects the aggregated report from the respective provider
     * classes. Dumps the collected stats in the bugreport.
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        pw.println("+    Telephony Analytics Report [2 months] [Slot ID = " + mSlotIndex + "]  +");
        pw.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        pw.println("Call Analytics Summary");
        ArrayList<String> aggregatedCallInfo = mCallAnalyticsProvider.aggregate();
        for (String info : aggregatedCallInfo) {
            pw.println("\t" + info);
        }
        pw.println("-----------------------------------------------");
        pw.println("SMS/MMS Analytics Summary");
        ArrayList<String> aggregatedSmsMmsInfo = mSmsMmsAnalyticsProvider.aggregate();
        for (String info : aggregatedSmsMmsInfo) {
            pw.println("\t\t" + info);
        }
        pw.println("-----------------------------------------------");
        mServiceStateAnalytics.recordCurrentStateBeforeDump();
        pw.println("Service State Analytics Summary ");
        ArrayList<String> aggregatedServiceStateInfo = mServiceStateAnalyticsProvider.aggregate();
        for (String info : aggregatedServiceStateInfo) {
            pw.println("\t\t" + info);
        }
        pw.println("-----------------------------------------------");
    }

    /**
     * Provides implementation for processing received Call related data. It implements functions to
     * handle various scenarios pertaining to Calls. Passes the data to its provider class
     * for further processing.
     */
    public class CallAnalytics {
        private static final String TAG = CallAnalytics.class.getSimpleName();

        private enum Status {
            SUCCESS("Success"),
            FAILURE("Failure");
            public String value;

            Status(String value) {
                this.value = value;
            }
        }

        private enum CallType {
            NORMAL_CALL("Normal Call"),
            SOS_CALL("SOS Call");
            public String value;

            CallType(String value) {
                this.value = value;
            }
        }

        public CallAnalytics() {}

        /**
         * Collects and processes data related to calls once the call is terminated.
         *
         * @param isEmergency : Stores whether the call is an SOS call or not
         * @param isOverIms : Stores whether the call is over IMS.
         * @param rat : Stores the Radio Access Technology being used when the call ended
         * @param simSlotIndex : Sim Slot from which call was operating.
         * @param disconnectCause : Reason for call disconnect.
         */
        public void onCallTerminated(
                boolean isEmergency,
                boolean isOverIms,
                int rat,
                int simSlotIndex,
                int disconnectCause) {
            String disconnectCauseString;
            String status;
            String callType;
            if (isEmergency) {
                callType = CallType.SOS_CALL.value;
            } else {
                callType = CallType.NORMAL_CALL.value;
            }
            if (isOverIms) {
                disconnectCauseString =
                        sImsCodeMap.getOrDefault(disconnectCause, "UNKNOWN_REJECT_CAUSE");
                if (disconnectCauseString.equals("UNKNOWN_REJECT_CAUSE")) {
                    Rlog.d(TAG, "UNKNOWN_REJECT_CAUSE: " + disconnectCause);
                }
                status =
                        disconnectCause == ImsReasonInfo.CODE_USER_TERMINATED
                                || disconnectCause
                                == ImsReasonInfo.CODE_USER_TERMINATED_BY_REMOTE
                                ? Status.SUCCESS.value
                                : Status.FAILURE.value;
            } else {
                disconnectCauseString = DisconnectCause.toString(disconnectCause);
                status =
                        disconnectCause == DisconnectCause.LOCAL
                                || disconnectCause == DisconnectCause.NORMAL
                                ? Status.SUCCESS.value
                                : Status.FAILURE.value;
            }
            String ratString = TelephonyManager.getNetworkTypeName(rat);
            sendDataToProvider(callType, status, simSlotIndex, rat, ratString,
                    disconnectCause, disconnectCauseString);
        }

        private void sendDataToProvider(String callType, String status, int simSlotIndex,
                int rat, String ratString, int disconnectCause, String disconnectCauseString) {
            mExecutorService.execute(() -> {
                mCallAnalyticsProvider.insertDataToDb(
                        callType, status, simSlotIndex, ratString, disconnectCauseString);
                ArrayList<String> data;
                data =
                        new ArrayList<>(
                                List.of(
                                        callType,
                                        status,
                                        disconnectCauseString,
                                        "(" + disconnectCause + ")",
                                        ratString,
                                        "(" + rat + ")",
                                        Integer.toString(simSlotIndex)));
                Rlog.d(TAG, data.toString());
            });
        }

        private static final Map<Integer, String> sImsCodeMap;

        static {
            sImsCodeMap = new HashMap<>();
            sImsCodeMap.put(ImsReasonInfo.CODE_UNSPECIFIED, "CODE_UNSPECIFIED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_LOCAL_ILLEGAL_ARGUMENT, "CODE_LOCAL_ILLEGAL_ARGUMENT");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_ILLEGAL_STATE, "CODE_LOCAL_ILLEGAL_STATE");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_INTERNAL_ERROR, "CODE_LOCAL_INTERNAL_ERROR");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_LOCAL_IMS_SERVICE_DOWN, "CODE_LOCAL_IMS_SERVICE_DOWN");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_NO_PENDING_CALL, "CODE_LOCAL_NO_PENDING_CALL");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_LOCAL_ENDED_BY_CONFERENCE_MERGE,
                    "CODE_LOCAL_ENDED_BY_CONFERENCE_MERGE");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_POWER_OFF, "CODE_LOCAL_POWER_OFF");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_LOW_BATTERY, "CODE_LOCAL_LOW_BATTERY");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_LOCAL_NETWORK_NO_SERVICE, "CODE_LOCAL_NETWORK_NO_SERVICE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_LOCAL_NETWORK_NO_LTE_COVERAGE,
                    "CODE_LOCAL_NETWORK_NO_LTE_COVERAGE");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_NETWORK_ROAMING, "CODE_LOCAL_NETWORK_ROAMING");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_LOCAL_NETWORK_IP_CHANGED, "CODE_LOCAL_NETWORK_IP_CHANGED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_LOCAL_SERVICE_UNAVAILABLE, "CODE_LOCAL_SERVICE_UNAVAILABLE");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_NOT_REGISTERED, "CODE_LOCAL_NOT_REGISTERED");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_CALL_EXCEEDED, "CODE_LOCAL_CALL_EXCEEDED");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_CALL_BUSY, "CODE_LOCAL_CALL_BUSY");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_CALL_DECLINE, "CODE_LOCAL_CALL_DECLINE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_LOCAL_CALL_VCC_ON_PROGRESSING,
                    "CODE_LOCAL_CALL_VCC_ON_PROGRESSING");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_LOCAL_CALL_RESOURCE_RESERVATION_FAILED,
                    "CODE_LOCAL_CALL_RESOURCE_RESERVATION_FAILED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_LOCAL_CALL_CS_RETRY_REQUIRED,
                    "CODE_LOCAL_CALL_CS_RETRY_REQUIRED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_LOCAL_CALL_VOLTE_RETRY_REQUIRED,
                    "CODE_LOCAL_CALL_VOLTE_RETRY_REQUIRED");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_CALL_TERMINATED, "CODE_LOCAL_CALL_TERMINATED");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOCAL_HO_NOT_FEASIBLE, "CODE_LOCAL_HO_NOT_FEASIBLE");
            sImsCodeMap.put(ImsReasonInfo.CODE_TIMEOUT_1XX_WAITING, "CODE_TIMEOUT_1XX_WAITING");
            sImsCodeMap.put(ImsReasonInfo.CODE_TIMEOUT_NO_ANSWER, "CODE_TIMEOUT_NO_ANSWER");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_TIMEOUT_NO_ANSWER_CALL_UPDATE,
                    "CODE_TIMEOUT_NO_ANSWER_CALL_UPDATE");
            sImsCodeMap.put(ImsReasonInfo.CODE_CALL_BARRED, "CODE_CALL_BARRED");
            sImsCodeMap.put(ImsReasonInfo.CODE_FDN_BLOCKED, "CODE_FDN_BLOCKED");
            sImsCodeMap.put(ImsReasonInfo.CODE_IMEI_NOT_ACCEPTED, "CODE_IMEI_NOT_ACCEPTED");
            sImsCodeMap.put(ImsReasonInfo.CODE_DIAL_MODIFIED_TO_USSD, "CODE_DIAL_MODIFIED_TO_USSD");
            sImsCodeMap.put(ImsReasonInfo.CODE_DIAL_MODIFIED_TO_SS, "CODE_DIAL_MODIFIED_TO_SS");
            sImsCodeMap.put(ImsReasonInfo.CODE_DIAL_MODIFIED_TO_DIAL, "CODE_DIAL_MODIFIED_TO_DIAL");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_DIAL_MODIFIED_TO_DIAL_VIDEO,
                    "CODE_DIAL_MODIFIED_TO_DIAL_VIDEO");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_DIAL_VIDEO_MODIFIED_TO_DIAL,
                    "CODE_DIAL_VIDEO_MODIFIED_TO_DIAL");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_DIAL_VIDEO_MODIFIED_TO_DIAL_VIDEO,
                    "CODE_DIAL_VIDEO_MODIFIED_TO_DIAL_VIDEO");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_DIAL_VIDEO_MODIFIED_TO_SS, "CODE_DIAL_VIDEO_MODIFIED_TO_SS");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_DIAL_VIDEO_MODIFIED_TO_USSD,
                    "CODE_DIAL_VIDEO_MODIFIED_TO_USSD");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_REDIRECTED, "CODE_SIP_REDIRECTED");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_BAD_REQUEST, "CODE_SIP_BAD_REQUEST");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_FORBIDDEN, "CODE_SIP_FORBIDDEN");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_NOT_FOUND, "CODE_SIP_NOT_FOUND");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_NOT_SUPPORTED, "CODE_SIP_NOT_SUPPORTED");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_REQUEST_TIMEOUT, "CODE_SIP_REQUEST_TIMEOUT");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_TEMPRARILY_UNAVAILABLE,
                    "CODE_SIP_TEMPRARILY_UNAVAILABLE");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_BAD_ADDRESS, "CODE_SIP_BAD_ADDRESS");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_BUSY, "CODE_SIP_BUSY");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_REQUEST_CANCELLED, "CODE_SIP_REQUEST_CANCELLED");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_NOT_ACCEPTABLE, "CODE_SIP_NOT_ACCEPTABLE");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_NOT_REACHABLE, "CODE_SIP_NOT_REACHABLE");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_CLIENT_ERROR, "CODE_SIP_CLIENT_ERROR");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_TRANSACTION_DOES_NOT_EXIST,
                    "CODE_SIP_TRANSACTION_DOES_NOT_EXIST");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_SERVER_INTERNAL_ERROR, "CODE_SIP_SERVER_INTERNAL_ERROR");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_SERVICE_UNAVAILABLE, "CODE_SIP_SERVICE_UNAVAILABLE");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_SERVER_TIMEOUT, "CODE_SIP_SERVER_TIMEOUT");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_SERVER_ERROR, "CODE_SIP_SERVER_ERROR");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_USER_REJECTED, "CODE_SIP_USER_REJECTED");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_GLOBAL_ERROR, "CODE_SIP_GLOBAL_ERROR");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_EMERGENCY_TEMP_FAILURE, "CODE_EMERGENCY_TEMP_FAILURE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_EMERGENCY_PERM_FAILURE, "CODE_EMERGENCY_PERM_FAILURE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_USER_MARKED_UNWANTED, "CODE_SIP_USER_MARKED_UNWANTED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_METHOD_NOT_ALLOWED, "CODE_SIP_METHOD_NOT_ALLOWED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_PROXY_AUTHENTICATION_REQUIRED,
                    "CODE_SIP_PROXY_AUTHENTICATION_REQUIRED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_REQUEST_ENTITY_TOO_LARGE,
                    "CODE_SIP_REQUEST_ENTITY_TOO_LARGE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_REQUEST_URI_TOO_LARGE, "CODE_SIP_REQUEST_URI_TOO_LARGE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_EXTENSION_REQUIRED, "CODE_SIP_EXTENSION_REQUIRED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_INTERVAL_TOO_BRIEF, "CODE_SIP_INTERVAL_TOO_BRIEF");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_CALL_OR_TRANS_DOES_NOT_EXIST,
                    "CODE_SIP_CALL_OR_TRANS_DOES_NOT_EXIST");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_LOOP_DETECTED, "CODE_SIP_LOOP_DETECTED");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_TOO_MANY_HOPS, "CODE_SIP_TOO_MANY_HOPS");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_AMBIGUOUS, "CODE_SIP_AMBIGUOUS");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_REQUEST_PENDING, "CODE_SIP_REQUEST_PENDING");
            sImsCodeMap.put(ImsReasonInfo.CODE_SIP_UNDECIPHERABLE, "CODE_SIP_UNDECIPHERABLE");
            sImsCodeMap.put(ImsReasonInfo.CODE_MEDIA_INIT_FAILED, "CODE_MEDIA_INIT_FAILED");
            sImsCodeMap.put(ImsReasonInfo.CODE_MEDIA_NO_DATA, "CODE_MEDIA_NO_DATA");
            sImsCodeMap.put(ImsReasonInfo.CODE_MEDIA_NOT_ACCEPTABLE, "CODE_MEDIA_NOT_ACCEPTABLE");
            sImsCodeMap.put(ImsReasonInfo.CODE_MEDIA_UNSPECIFIED, "CODE_MEDIA_UNSPECIFIED");
            sImsCodeMap.put(ImsReasonInfo.CODE_USER_TERMINATED, "CODE_USER_TERMINATED");
            sImsCodeMap.put(ImsReasonInfo.CODE_USER_NOANSWER, "CODE_USER_NOANSWER");
            sImsCodeMap.put(ImsReasonInfo.CODE_USER_IGNORE, "CODE_USER_IGNORE");
            sImsCodeMap.put(ImsReasonInfo.CODE_USER_DECLINE, "CODE_USER_DECLINE");
            sImsCodeMap.put(ImsReasonInfo.CODE_LOW_BATTERY, "CODE_LOW_BATTERY");
            sImsCodeMap.put(ImsReasonInfo.CODE_BLACKLISTED_CALL_ID, "CODE_BLACKLISTED_CALL_ID");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_USER_TERMINATED_BY_REMOTE, "CODE_USER_TERMINATED_BY_REMOTE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_USER_REJECTED_SESSION_MODIFICATION,
                    "CODE_USER_REJECTED_SESSION_MODIFICATION");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_USER_CANCELLED_SESSION_MODIFICATION,
                    "CODE_USER_CANCELLED_SESSION_MODIFICATION");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SESSION_MODIFICATION_FAILED,
                    "CODE_SESSION_MODIFICATION_FAILED");
            sImsCodeMap.put(ImsReasonInfo.CODE_UT_NOT_SUPPORTED, "CODE_UT_NOT_SUPPORTED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, "CODE_UT_SERVICE_UNAVAILABLE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_UT_OPERATION_NOT_ALLOWED, "CODE_UT_OPERATION_NOT_ALLOWED");
            sImsCodeMap.put(ImsReasonInfo.CODE_UT_NETWORK_ERROR, "CODE_UT_NETWORK_ERROR");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_UT_CB_PASSWORD_MISMATCH, "CODE_UT_CB_PASSWORD_MISMATCH");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_DIAL, "CODE_UT_SS_MODIFIED_TO_DIAL");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_USSD, "CODE_UT_SS_MODIFIED_TO_USSD");
            sImsCodeMap.put(ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_SS, "CODE_UT_SS_MODIFIED_TO_SS");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_DIAL_VIDEO,
                    "CODE_UT_SS_MODIFIED_TO_DIAL_VIDEO");
            sImsCodeMap.put(ImsReasonInfo.CODE_ECBM_NOT_SUPPORTED, "CODE_ECBM_NOT_SUPPORTED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_MULTIENDPOINT_NOT_SUPPORTED,
                    "CODE_MULTIENDPOINT_NOT_SUPPORTED");
            sImsCodeMap.put(ImsReasonInfo.CODE_REGISTRATION_ERROR, "CODE_REGISTRATION_ERROR");
            sImsCodeMap.put(ImsReasonInfo.CODE_ANSWERED_ELSEWHERE, "CODE_ANSWERED_ELSEWHERE");
            sImsCodeMap.put(ImsReasonInfo.CODE_CALL_PULL_OUT_OF_SYNC, "CODE_CALL_PULL_OUT_OF_SYNC");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_CALL_END_CAUSE_CALL_PULL, "CODE_CALL_END_CAUSE_CALL_PULL");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_CALL_DROP_IWLAN_TO_LTE_UNAVAILABLE,
                    "CODE_CALL_DROP_IWLAN_TO_LTE_UNAVAILABLE");
            sImsCodeMap.put(ImsReasonInfo.CODE_REJECTED_ELSEWHERE, "CODE_REJECTED_ELSEWHERE");
            sImsCodeMap.put(ImsReasonInfo.CODE_SUPP_SVC_FAILED, "CODE_SUPP_SVC_FAILED");
            sImsCodeMap.put(ImsReasonInfo.CODE_SUPP_SVC_CANCELLED, "CODE_SUPP_SVC_CANCELLED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SUPP_SVC_REINVITE_COLLISION,
                    "CODE_SUPP_SVC_REINVITE_COLLISION");
            sImsCodeMap.put(ImsReasonInfo.CODE_IWLAN_DPD_FAILURE, "CODE_IWLAN_DPD_FAILURE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_EPDG_TUNNEL_ESTABLISH_FAILURE,
                    "CODE_EPDG_TUNNEL_ESTABLISH_FAILURE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_EPDG_TUNNEL_REKEY_FAILURE, "CODE_EPDG_TUNNEL_REKEY_FAILURE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_EPDG_TUNNEL_LOST_CONNECTION,
                    "CODE_EPDG_TUNNEL_LOST_CONNECTION");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_MAXIMUM_NUMBER_OF_CALLS_REACHED,
                    "CODE_MAXIMUM_NUMBER_OF_CALLS_REACHED");
            sImsCodeMap.put(ImsReasonInfo.CODE_REMOTE_CALL_DECLINE, "CODE_REMOTE_CALL_DECLINE");
            sImsCodeMap.put(ImsReasonInfo.CODE_DATA_LIMIT_REACHED, "CODE_DATA_LIMIT_REACHED");
            sImsCodeMap.put(ImsReasonInfo.CODE_DATA_DISABLED, "CODE_DATA_DISABLED");
            sImsCodeMap.put(ImsReasonInfo.CODE_WIFI_LOST, "CODE_WIFI_LOST");
            sImsCodeMap.put(ImsReasonInfo.CODE_IKEV2_AUTH_FAILURE, "CODE_IKEV2_AUTH_FAILURE");
            sImsCodeMap.put(ImsReasonInfo.CODE_RADIO_OFF, "CODE_RADIO_OFF");
            sImsCodeMap.put(ImsReasonInfo.CODE_NO_VALID_SIM, "CODE_NO_VALID_SIM");
            sImsCodeMap.put(ImsReasonInfo.CODE_RADIO_INTERNAL_ERROR, "CODE_RADIO_INTERNAL_ERROR");
            sImsCodeMap.put(ImsReasonInfo.CODE_NETWORK_RESP_TIMEOUT, "CODE_NETWORK_RESP_TIMEOUT");
            sImsCodeMap.put(ImsReasonInfo.CODE_NETWORK_REJECT, "CODE_NETWORK_REJECT");
            sImsCodeMap.put(ImsReasonInfo.CODE_RADIO_ACCESS_FAILURE, "CODE_RADIO_ACCESS_FAILURE");
            sImsCodeMap.put(ImsReasonInfo.CODE_RADIO_LINK_FAILURE, "CODE_RADIO_LINK_FAILURE");
            sImsCodeMap.put(ImsReasonInfo.CODE_RADIO_LINK_LOST, "CODE_RADIO_LINK_LOST");
            sImsCodeMap.put(ImsReasonInfo.CODE_RADIO_UPLINK_FAILURE, "CODE_RADIO_UPLINK_FAILURE");
            sImsCodeMap.put(ImsReasonInfo.CODE_RADIO_SETUP_FAILURE, "CODE_RADIO_SETUP_FAILURE");
            sImsCodeMap.put(ImsReasonInfo.CODE_RADIO_RELEASE_NORMAL, "CODE_RADIO_RELEASE_NORMAL");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_RADIO_RELEASE_ABNORMAL, "CODE_RADIO_RELEASE_ABNORMAL");
            sImsCodeMap.put(ImsReasonInfo.CODE_ACCESS_CLASS_BLOCKED, "CODE_ACCESS_CLASS_BLOCKED");
            sImsCodeMap.put(ImsReasonInfo.CODE_NETWORK_DETACH, "CODE_NETWORK_DETACH");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_SIP_ALTERNATE_EMERGENCY_CALL,
                    "CODE_SIP_ALTERNATE_EMERGENCY_CALL");
            sImsCodeMap.put(ImsReasonInfo.CODE_UNOBTAINABLE_NUMBER, "CODE_UNOBTAINABLE_NUMBER");
            sImsCodeMap.put(ImsReasonInfo.CODE_NO_CSFB_IN_CS_ROAM, "CODE_NO_CSFB_IN_CS_ROAM");
            sImsCodeMap.put(ImsReasonInfo.CODE_REJECT_UNKNOWN, "CODE_REJECT_UNKNOWN");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_ONGOING_CALL_WAITING_DISABLED,
                    "CODE_REJECT_ONGOING_CALL_WAITING_DISABLED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_CALL_ON_OTHER_SUB, "CODE_REJECT_CALL_ON_OTHER_SUB");
            sImsCodeMap.put(ImsReasonInfo.CODE_REJECT_1X_COLLISION, "CODE_REJECT_1X_COLLISION");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_SERVICE_NOT_REGISTERED,
                    "CODE_REJECT_SERVICE_NOT_REGISTERED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_CALL_TYPE_NOT_ALLOWED,
                    "CODE_REJECT_CALL_TYPE_NOT_ALLOWED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_ONGOING_E911_CALL, "CODE_REJECT_ONGOING_E911_CALL");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_ONGOING_CALL_SETUP, "CODE_REJECT_ONGOING_CALL_SETUP");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_MAX_CALL_LIMIT_REACHED,
                    "CODE_REJECT_MAX_CALL_LIMIT_REACHED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_UNSUPPORTED_SIP_HEADERS,
                    "CODE_REJECT_UNSUPPORTED_SIP_HEADERS");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_UNSUPPORTED_SDP_HEADERS,
                    "CODE_REJECT_UNSUPPORTED_SDP_HEADERS");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_ONGOING_CALL_TRANSFER,
                    "CODE_REJECT_ONGOING_CALL_TRANSFER");
            sImsCodeMap.put(ImsReasonInfo.CODE_REJECT_INTERNAL_ERROR, "CODE_REJECT_INTERNAL_ERROR");
            sImsCodeMap.put(ImsReasonInfo.CODE_REJECT_QOS_FAILURE, "CODE_REJECT_QOS_FAILURE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_ONGOING_HANDOVER, "CODE_REJECT_ONGOING_HANDOVER");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_VT_TTY_NOT_ALLOWED, "CODE_REJECT_VT_TTY_NOT_ALLOWED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_ONGOING_CALL_UPGRADE,
                    "CODE_REJECT_ONGOING_CALL_UPGRADE");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_CONFERENCE_TTY_NOT_ALLOWED,
                    "CODE_REJECT_CONFERENCE_TTY_NOT_ALLOWED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_ONGOING_CONFERENCE_CALL,
                    "CODE_REJECT_ONGOING_CONFERENCE_CALL");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_VT_AVPF_NOT_ALLOWED,
                    "CODE_REJECT_VT_AVPF_NOT_ALLOWED");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_ONGOING_ENCRYPTED_CALL,
                    "CODE_REJECT_ONGOING_ENCRYPTED_CALL");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_REJECT_ONGOING_CS_CALL, "CODE_REJECT_ONGOING_CS_CALL");
            sImsCodeMap.put(ImsReasonInfo.CODE_NETWORK_CONGESTION, "CODE_NETWORK_CONGESTION");
            sImsCodeMap.put(
                    ImsReasonInfo.CODE_RETRY_ON_IMS_WITHOUT_RTT, "CODE_RETRY_ON_IMS_WITHOUT_RTT");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_1, "CODE_OEM_CAUSE_1");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_2, "CODE_OEM_CAUSE_2");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_3, "CODE_OEM_CAUSE_3");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_4, "CODE_OEM_CAUSE_4");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_5, "CODE_OEM_CAUSE_5");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_6, "CODE_OEM_CAUSE_6");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_7, "CODE_OEM_CAUSE_7");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_8, "CODE_OEM_CAUSE_8");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_9, "CODE_OEM_CAUSE_9");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_10, "CODE_OEM_CAUSE_10");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_11, "CODE_OEM_CAUSE_11");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_12, "CODE_OEM_CAUSE_12");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_13, "CODE_OEM_CAUSE_13");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_14, "CODE_OEM_CAUSE_14");
            sImsCodeMap.put(ImsReasonInfo.CODE_OEM_CAUSE_15, "CODE_OEM_CAUSE_15");
        }
    }

    /**
     * Implements and Registers the required Listeners and BroadcastReceivers for receiving
     * ServiceState related information. Performs required logic on received data and then Passes
     * the information to its provider class for further processing.
     */
    public class ServiceStateAnalytics extends TelephonyCallback
            implements TelephonyCallback.ServiceStateListener {
        private final Executor mExecutor;
        private static final String TAG = ServiceStateAnalytics.class.getSimpleName();
        private static final int BUFFER_TIME = 10000;

        private TelephonyManager mTelephonyManager;

        private enum DeviceStatus {
            APM,
            CELLULAR_OOS_WITH_IWLAN,
            NO_NETWORK_COVERAGE,
            SIM_ABSENT,
            IN_SERVICE;
        }

        private final AtomicReference<TimeStampedServiceState> mLastState =
                new AtomicReference<>(null);
        private static final String NA = "NA";
        private final BroadcastReceiver mBroadcastReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final long now = getTimeMillis();
                        if (intent.getAction()
                                .equals(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED)) {
                            int simState =
                                    intent.getIntExtra(
                                            TelephonyManager.EXTRA_SIM_STATE,
                                            TelephonyManager.SIM_STATE_UNKNOWN);
                            if (simState == TelephonyManager.SIM_STATE_ABSENT) {
                                Rlog.d("AnkitSimAbsent", "Sim is Absent");
                                logSimAbsentState();
                            }
                        }
                    }
                };

        protected ServiceStateAnalytics(Executor executor) {
            super();
            mExecutor = executor;
            IntentFilter mIntentFilter =
                    new IntentFilter(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver, mIntentFilter);
        }

        @Override
        public void onServiceStateChanged(@NonNull ServiceState serviceState) {
            int dataRegState = serviceState.getDataRegState();
            int voiceRegState = serviceState.getVoiceRegState();
            int voiceRadioTechnology = serviceState.getRilVoiceRadioTechnology();
            int dataRadioTechnology = serviceState.getRilDataRadioTechnology();

            mExecutorService.execute(() -> {
                logServiceState(dataRegState, voiceRegState, voiceRadioTechnology,
                        dataRadioTechnology);
            });
        }

        private void logServiceState(
                int dataRegState,
                int voiceRegState,
                int voiceRadioTechnology,
                int dataRadioTechnology) {
            long now = getTimeMillis();
            String voiceRadioTechnologyName =
                    ServiceState.rilRadioTechnologyToString(voiceRadioTechnology);
            String dataRadioTechnologyName =
                    ServiceState.rilRadioTechnologyToString(dataRadioTechnology);

            if (isAirplaneModeOn()) {
                if (dataRadioTechnology == ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                        && dataRegState == ServiceState.STATE_IN_SERVICE) {
                    logOosWithIwlan(now);
                } else {
                    logAirplaneModeServiceState(now);
                }
            } else {
                if (voiceRadioTechnology == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN
                        && dataRadioTechnology == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) {
                    logNoNetworkCoverage(now);

                } else if (voiceRadioTechnology != ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN
                        && dataRadioTechnology == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) {
                    if (voiceRegState == ServiceState.STATE_IN_SERVICE) {
                        logInServiceData(voiceRadioTechnologyName, now);
                    } else {
                        logNoNetworkCoverage(now);
                    }
                } else if (voiceRadioTechnology == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) {
                    if (dataRegState == ServiceState.STATE_IN_SERVICE) {
                        if (dataRadioTechnology == ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN) {
                            logOosWithIwlan(now);
                        } else {
                            logInServiceData(dataRadioTechnologyName, now);
                        }
                    } else {
                        logNoNetworkCoverage(now);
                    }
                } else {
                    if (dataRegState == ServiceState.STATE_IN_SERVICE
                            || voiceRegState == ServiceState.STATE_IN_SERVICE) {
                        logInServiceData(voiceRadioTechnologyName, now);
                    } else {
                        logNoNetworkCoverage(now);
                    }
                }
            }
        }

        private void logSimAbsentState() {
            long now = getTimeMillis();
            TimeStampedServiceState currentState =
                    new TimeStampedServiceState(
                            mSlotIndex, NA, DeviceStatus.SIM_ABSENT.name(), now);
            setCurrentStateAndAddLastState(currentState, now);
        }
        private void logOosWithIwlan(long now) {
            TimeStampedServiceState currentState =
                    new TimeStampedServiceState(mSlotIndex, NA,
                            DeviceStatus.CELLULAR_OOS_WITH_IWLAN.name(), now);
            setCurrentStateAndAddLastState(currentState, now);
        }

        private void logAirplaneModeServiceState(long now) {
            TimeStampedServiceState currentState =
                    new TimeStampedServiceState(mSlotIndex, NA, DeviceStatus.APM.name(), now);
            setCurrentStateAndAddLastState(currentState, now);
        }

        private void logNoNetworkCoverage(long now) {
            TimeStampedServiceState currentState =
                    new TimeStampedServiceState(
                            mSlotIndex, NA, DeviceStatus.NO_NETWORK_COVERAGE.name(), now);
            setCurrentStateAndAddLastState(currentState, now);
        }

        private void logInServiceData(String rat, long now) {
            TimeStampedServiceState currentState =
                    new TimeStampedServiceState(
                            mSlotIndex, rat, DeviceStatus.IN_SERVICE.name(), now);
            setCurrentStateAndAddLastState(currentState, now);
        }

        private void setCurrentStateAndAddLastState(
                TimeStampedServiceState currentState, long now) {
            TimeStampedServiceState lastState = mLastState.getAndSet(currentState);
            addData(lastState, now);
        }

        private void addData(TimeStampedServiceState lastState, long now) {
            if (lastState == null) {
                return;
            }
            if (now - lastState.mTimestampStart < BUFFER_TIME) {
                return;
            }
            Rlog.d(TAG, "Last State = " + lastState.toString() + "End = " + now);
            mServiceStateAnalyticsProvider.insertDataToDb(lastState, now);
        }

        private void recordCurrentStateBeforeDump() {
            long now = getTimeMillis();
            Rlog.d("RecordingStateBDump", "Recording " + now);
            TimeStampedServiceState currentState = mLastState.get();
            mLastState.set(createCopyWithUpdatedTimestamp(currentState));
            addData(currentState, now);
        }

        private TimeStampedServiceState createCopyWithUpdatedTimestamp(
                TimeStampedServiceState currentState) {
            if (currentState == null) {
                return null;
            }
            long now = getTimeMillis();
            TimeStampedServiceState state =
                    new TimeStampedServiceState(
                            currentState.mSlotIndex,
                            currentState.mRAT,
                            currentState.mDeviceStatus,
                            now);
            return state;
        }

        private boolean isAirplaneModeOn() {
            return Settings.Global.getInt(
                    mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0)
                    != 0;
        }

        protected long getTimeMillis() {
            return SystemClock.elapsedRealtime();
        }

        void registerMyListener(Context context, int subId) {
            try {
                mTelephonyManager =
                        context.getSystemService(TelephonyManager.class)
                                .createForSubscriptionId(subId);
                mTelephonyManager.registerTelephonyCallback(mExecutor, this);

            } catch (NullPointerException e) {
                log("Null pointer exception caught " + e);
            }
        }

        void unregisterMyListener(int subId) {
            mTelephonyManager.unregisterTelephonyCallback(this);
        }

        private void log(String s) {
            Rlog.d(ServiceStateAnalytics.class.getSimpleName(), s);
        }

        /**
         * Serves the functionality of storing service state related information,
         * Along with the timestamp at which the state was detected.
         */
        public static class TimeStampedServiceState {
            protected final int mSlotIndex;
            protected final String mRAT;
            protected final String mDeviceStatus;
            protected final long mTimestampStart;

            public TimeStampedServiceState(
                    int slotIndex, String rat, String deviceStatus, long timestampStart) {
                mSlotIndex = slotIndex;
                mRAT = rat;
                mDeviceStatus = deviceStatus;
                mTimestampStart = timestampStart;
            }

            @Override
            public String toString() {
                return "SlotIndex = "
                        + mSlotIndex
                        + " RAT = "
                        + mRAT
                        + " DeviceStatus = "
                        + mDeviceStatus
                        + "TimeStampStart = "
                        + mTimestampStart;
            }
            /** Getter function for slotIndex */
            public int getSlotIndex() {
                return mSlotIndex;
            }

            /** Getter function for state start Timestamp */
            public long getTimestampStart() {
                return mTimestampStart;
            }

            /** Getter function for device Status */
            public String getDeviceStatus() {
                return mDeviceStatus;
            }

            /** Getter function for radio access technology  */
            public String getRAT() {
                return mRAT;
            }
        }
    }

    /**
     * Provides implementation for processing received Sms related data. Implements functions to
     * handle various scenarios pertaining to Sms. Passes the data to its provider for further
     * processing.
     */
    public class SmsMmsAnalytics {
        private static final String TAG = SmsMmsAnalytics.class.getSimpleName();
        public SmsMmsAnalytics() {

        }

        /** Collects Outgoing Sms related information. */
        public void onOutgoingSms(boolean isOverIms, @SmsManager.Result int sendErrorCode) {
            Rlog.d(
                    TAG,
                    "Is Over Ims = "
                            + isOverIms
                            + " sendErrorCode = "
                            + sendErrorCode
                            + "SlotInfo ="
                            + mSlotIndex);
            logOutgoingSms(isOverIms, sendErrorCode);
        }

        /** Collects Successful Incoming Sms related information. */
        public void onIncomingSmsSuccess(@InboundSmsHandler.SmsSource int smsSource) {
            Rlog.d(TAG, " smsSource = " + smsSource);
            String status = "Success";
            String failureReason = "NA";
            logIncomingSms(smsSource, status, failureReason);
        }

        /** Collects Failed Incoming Multipart Sms related information. */
        public void onDroppedIncomingMultipartSms() {
            String status = "Failure";
            String type = "SMS Incoming";
            // Mark the RAT as unknown since it might have changed over time.
            int rat = TelephonyManager.NETWORK_TYPE_UNKNOWN;
            String ratString = ServiceState.rilRadioTechnologyToString(rat);
            String failureReason = "INCOMING_SMS__ERROR__SMS_ERROR_GENERIC";
            sendDataToProvider(status, type, ratString, failureReason);
        }

        /** Collects Failed Incoming Sms related information. */
        public void onIncomingSmsError(@InboundSmsHandler.SmsSource int smsSource, int result) {
            String status = "Failure";
            String failureReason = getIncomingSmsErrorString(result);
            logIncomingSms(smsSource, status, failureReason);
            Rlog.d(
                    TAG,
                    " smsSource = "
                            + smsSource
                            + "Result = "
                            + result
                            + "IncomingError = "
                            + failureReason
                            + "("
                            + getIncomingError(result)
                            + ")");
        }

        private void logOutgoingSms(boolean isOverIms, @SmsManager.Result int sendErrorCode) {
            try {
                String type = "SMS Outgoing";
                String status = sendErrorCode == 0 ? "Success" : "Failure";
                int rat = getRat(isOverIms);
                String ratString = TelephonyManager.getNetworkTypeName(rat);
                String failureReason =
                        status.equals("Success") ? "NA" : getSmsFailureReasonString(sendErrorCode);
                Rlog.d(
                        TAG,
                        "SlotInfo = "
                                + mSlotIndex
                                + " Type = "
                                + type
                                + " Status = "
                                + status
                                + "RAT "
                                + ratString
                                + " "
                                + rat
                                + "Failure Reason = "
                                + failureReason);
                sendDataToProvider(status, type, ratString, failureReason);

            } catch (Exception e) {
                Rlog.d(TAG, "Error in SmsLogs" + e);
            }
        }

        private void logIncomingSms(
                @InboundSmsHandler.SmsSource int smsSource, String status, String failureReason) {
            String type = "SMS Incoming";
            try {
                int rat = getRat(smsSource);
                String ratString = TelephonyManager.getNetworkTypeName(rat);
                sendDataToProvider(status, type, ratString, failureReason);
                Rlog.d(
                        TAG,
                        "SlotInfo ="
                                + mSlotIndex
                                + " Type = "
                                + type
                                + " Status = "
                                + status
                                + " RAT "
                                + ratString
                                + " ("
                                + rat
                                + " ) Failure Reason = "
                                + failureReason);
            } catch (Exception e) {
                Rlog.e(TAG, "Exception = " + e);
            }
        }

        private void sendDataToProvider(
                String status, String type, String rat, String failureReason) {
            mExecutorService.execute(() -> {
                mSmsMmsAnalyticsProvider.insertDataToDb(status, type, rat, failureReason);
            });
        }

        private static int getIncomingError(int result) {
            switch (result) {
                case Activity.RESULT_OK:
                case Intents.RESULT_SMS_HANDLED:
                    return INCOMING_SMS__ERROR__SMS_SUCCESS;
                case Intents.RESULT_SMS_OUT_OF_MEMORY:
                    return INCOMING_SMS__ERROR__SMS_ERROR_NO_MEMORY;
                case Intents.RESULT_SMS_UNSUPPORTED:
                    return INCOMING_SMS__ERROR__SMS_ERROR_NOT_SUPPORTED;
                case Intents.RESULT_SMS_GENERIC_ERROR:
                default:
                    return INCOMING_SMS__ERROR__SMS_ERROR_GENERIC;
            }
        }

        private static String getIncomingSmsErrorString(int result) {
            switch (result) {
                case Activity.RESULT_OK:
                case Intents.RESULT_SMS_HANDLED:
                    return "INCOMING_SMS__ERROR__SMS_SUCCESS";
                case Intents.RESULT_SMS_OUT_OF_MEMORY:
                    return "INCOMING_SMS__ERROR__SMS_ERROR_NO_MEMORY";
                case Intents.RESULT_SMS_UNSUPPORTED:
                    return "INCOMING_SMS__ERROR__SMS_ERROR_NOT_SUPPORTED";
                case Intents.RESULT_SMS_GENERIC_ERROR:
                default:
                    return "INCOMING_SMS__ERROR__SMS_ERROR_GENERIC";
            }
        }

        @Nullable
        private ServiceState getServiceState() {
            Phone phone = mPhone;
            if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_IMS) {
                phone = mPhone.getDefaultPhone();
            }
            ServiceStateTracker serviceStateTracker = phone.getServiceStateTracker();
            return serviceStateTracker != null ? serviceStateTracker.getServiceState() : null;
        }

        @Annotation.NetworkType
        private int getRat(@InboundSmsHandler.SmsSource int smsSource) {
            if (smsSource == SOURCE_INJECTED_FROM_UNKNOWN) {
                return TelephonyManager.NETWORK_TYPE_UNKNOWN;
            }
            return getRat(smsSource == SOURCE_INJECTED_FROM_IMS);
        }

        @Annotation.NetworkType
        private int getRat(boolean isOverIms) {
            if (isOverIms) {
                if (mPhone.getImsRegistrationTech()
                        == ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN) {
                    return TelephonyManager.NETWORK_TYPE_IWLAN;
                }
            }
            ServiceState serviceState = getServiceState();
            return serviceState != null
                    ? serviceState.getVoiceNetworkType()
                    : TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }

        private String getSmsFailureReasonString(int sendErrorCode) {
            switch (sendErrorCode) {
                case SmsManager.RESULT_ERROR_NONE:
                    return "RESULT_ERROR_NONE";
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    return "RESULT_ERROR_GENERIC_FAILURE";
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    return "RESULT_ERROR_RADIO_OFF";
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    return "RESULT_ERROR_NULL_PDU";
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    return "RESULT_ERROR_NO_SERVICE";
                case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:
                    return "RESULT_ERROR_LIMIT_EXCEEDED";
                case SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE:
                    return "RESULT_ERROR_FDN_CHECK_FAILURE";
                case SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED:
                    return "RESULT_ERROR_SHORT_CODE_NOT_ALLOWED";
                case SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED:
                    return "RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED";
                case SmsManager.RESULT_RADIO_NOT_AVAILABLE:
                    return "RESULT_RADIO_NOT_AVAILABLE";
                case SmsManager.RESULT_NETWORK_REJECT:
                    return "RESULT_NETWORK_REJECT";
                case SmsManager.RESULT_INVALID_ARGUMENTS:
                    return "RESULT_INVALID_ARGUMENTS";
                case SmsManager.RESULT_INVALID_STATE:
                    return "RESULT_INVALID_STATE";
                case SmsManager.RESULT_NO_MEMORY:
                    return "RESULT_NO_MEMORY";
                case SmsManager.RESULT_INVALID_SMS_FORMAT:
                    return "RESULT_INVALID_SMS_FORMAT";
                case SmsManager.RESULT_SYSTEM_ERROR:
                    return "RESULT_SYSTEM_ERROR";
                case SmsManager.RESULT_MODEM_ERROR:
                    return "RESULT_MODEM_ERROR";
                case SmsManager.RESULT_NETWORK_ERROR:
                    return "RESULT_NETWORK_ERROR";
                case SmsManager.RESULT_INVALID_SMSC_ADDRESS:
                    return "RESULT_INVALID_SMSC_ADDRESS";
                case SmsManager.RESULT_OPERATION_NOT_ALLOWED:
                    return "RESULT_OPERATION_NOT_ALLOWED";
                case SmsManager.RESULT_INTERNAL_ERROR:
                    return "RESULT_INTERNAL_ERROR";
                case SmsManager.RESULT_NO_RESOURCES:
                    return "RESULT_NO_RESOURCES";
                case SmsManager.RESULT_CANCELLED:
                    return "RESULT_CANCELLED";
                case SmsManager.RESULT_REQUEST_NOT_SUPPORTED:
                    return "RESULT_REQUEST_NOT_SUPPORTED";
                case SmsManager.RESULT_NO_BLUETOOTH_SERVICE:
                    return "RESULT_NO_BLUETOOTH_SERVICE";
                case SmsManager.RESULT_INVALID_BLUETOOTH_ADDRESS:
                    return "RESULT_INVALID_BLUETOOTH_ADDRESS";
                case SmsManager.RESULT_BLUETOOTH_DISCONNECTED:
                    return "RESULT_BLUETOOTH_DISCONNECTED";
                case SmsManager.RESULT_UNEXPECTED_EVENT_STOP_SENDING:
                    return "RESULT_UNEXPECTED_EVENT_STOP_SENDING";
                case SmsManager.RESULT_SMS_BLOCKED_DURING_EMERGENCY:
                    return "RESULT_SMS_BLOCKED_DURING_EMERGENCY";
                case SmsManager.RESULT_SMS_SEND_RETRY_FAILED:
                    return "RESULT_SMS_SEND_RETRY_FAILED";
                case SmsManager.RESULT_REMOTE_EXCEPTION:
                    return "RESULT_REMOTE_EXCEPTION";
                case SmsManager.RESULT_NO_DEFAULT_SMS_APP:
                    return "RESULT_NO_DEFAULT_SMS_APP";
                case SmsManager.RESULT_USER_NOT_ALLOWED:
                    return "RESULT_USER_NOT_ALLOWED";
                case SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE:
                    return "RESULT_RIL_RADIO_NOT_AVAILABLE";
                case SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY:
                    return "RESULT_RIL_SMS_SEND_FAIL_RETRY";
                case SmsManager.RESULT_RIL_NETWORK_REJECT:
                    return "RESULT_RIL_NETWORK_REJECT";
                case SmsManager.RESULT_RIL_INVALID_STATE:
                    return "RESULT_RIL_INVALID_STATE";
                case SmsManager.RESULT_RIL_INVALID_ARGUMENTS:
                    return "RESULT_RIL_INVALID_ARGUMENTS";
                case SmsManager.RESULT_RIL_NO_MEMORY:
                    return "RESULT_RIL_NO_MEMORY";
                case SmsManager.RESULT_RIL_REQUEST_RATE_LIMITED:
                    return "RESULT_RIL_REQUEST_RATE_LIMITED";
                case SmsManager.RESULT_RIL_INVALID_SMS_FORMAT:
                    return "RESULT_RIL_INVALID_SMS_FORMAT";
                case SmsManager.RESULT_RIL_SYSTEM_ERR:
                    return "RESULT_RIL_SYSTEM_ERR";
                case SmsManager.RESULT_RIL_ENCODING_ERR:
                    return "RESULT_RIL_ENCODING_ERR";
                case SmsManager.RESULT_RIL_INVALID_SMSC_ADDRESS:
                    return "RESULT_RIL_INVALID_SMSC_ADDRESS";
                case SmsManager.RESULT_RIL_MODEM_ERR:
                    return "RESULT_RIL_MODEM_ERR";
                case SmsManager.RESULT_RIL_NETWORK_ERR:
                    return "RESULT_RIL_NETWORK_ERR";
                case SmsManager.RESULT_RIL_INTERNAL_ERR:
                    return "RESULT_RIL_INTERNAL_ERR";
                case SmsManager.RESULT_RIL_REQUEST_NOT_SUPPORTED:
                    return "RESULT_RIL_REQUEST_NOT_SUPPORTED";
                case SmsManager.RESULT_RIL_INVALID_MODEM_STATE:
                    return "RESULT_RIL_INVALID_MODEM_STATE";
                case SmsManager.RESULT_RIL_NETWORK_NOT_READY:
                    return "RESULT_RIL_NETWORK_NOT_READY";
                case SmsManager.RESULT_RIL_OPERATION_NOT_ALLOWED:
                    return "RESULT_RIL_OPERATION_NOT_ALLOWED";
                case SmsManager.RESULT_RIL_NO_RESOURCES:
                    return "RESULT_RIL_NO_RESOURCES";
                case SmsManager.RESULT_RIL_CANCELLED:
                    return "RESULT_RIL_CANCELLED";
                case SmsManager.RESULT_RIL_SIM_ABSENT:
                    return "RESULT_RIL_SIM_ABSENT";
                case SmsManager.RESULT_RIL_SIMULTANEOUS_SMS_AND_CALL_NOT_ALLOWED:
                    return "RESULT_RIL_SIMULTANEOUS_SMS_AND_CALL_NOT_ALLOWED";
                case SmsManager.RESULT_RIL_ACCESS_BARRED:
                    return "RESULT_RIL_ACCESS_BARRED";
                case SmsManager.RESULT_RIL_BLOCKED_DUE_TO_CALL:
                    return "RESULT_RIL_BLOCKED_DUE_TO_CALL";
                case SmsManager.RESULT_RIL_GENERIC_ERROR:
                    return "RESULT_RIL_GENERIC_ERROR";
                case SmsManager.RESULT_RIL_INVALID_RESPONSE:
                    return "RESULT_RIL_INVALID_RESPONSE";
                case SmsManager.RESULT_RIL_SIM_PIN2:
                    return "RESULT_RIL_SIM_PIN2";
                case SmsManager.RESULT_RIL_SIM_PUK2:
                    return "RESULT_RIL_SIM_PUK2";
                case SmsManager.RESULT_RIL_SUBSCRIPTION_NOT_AVAILABLE:
                    return "RESULT_RIL_SUBSCRIPTION_NOT_AVAILABLE";
                case SmsManager.RESULT_RIL_SIM_ERROR:
                    return "RESULT_RIL_SIM_ERROR";
                case SmsManager.RESULT_RIL_INVALID_SIM_STATE:
                    return "RESULT_RIL_INVALID_SIM_STATE";
                case SmsManager.RESULT_RIL_NO_SMS_TO_ACK:
                    return "RESULT_RIL_NO_SMS_TO_ACK";
                case SmsManager.RESULT_RIL_SIM_BUSY:
                    return "RESULT_RIL_SIM_BUSY";
                case SmsManager.RESULT_RIL_SIM_FULL:
                    return "RESULT_RIL_SIM_FULL";
                case SmsManager.RESULT_RIL_NO_SUBSCRIPTION:
                    return "RESULT_RIL_NO_SUBSCRIPTION";
                case SmsManager.RESULT_RIL_NO_NETWORK_FOUND:
                    return "RESULT_RIL_NO_NETWORK_FOUND";
                case SmsManager.RESULT_RIL_DEVICE_IN_USE:
                    return "RESULT_RIL_DEVICE_IN_USE";
                case SmsManager.RESULT_RIL_ABORTED:
                    return "RESULT_RIL_ABORTED";
                default:
                    return "NA";
            }
        }
    }
}

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

import static com.android.internal.telephony.TelephonyStatsLog.DATA_CALL_SESSION__DEACTIVATE_REASON__DEACTIVATE_REASON_HANDOVER;
import static com.android.internal.telephony.TelephonyStatsLog.DATA_CALL_SESSION__DEACTIVATE_REASON__DEACTIVATE_REASON_NORMAL;
import static com.android.internal.telephony.TelephonyStatsLog.DATA_CALL_SESSION__DEACTIVATE_REASON__DEACTIVATE_REASON_RADIO_OFF;
import static com.android.internal.telephony.TelephonyStatsLog.DATA_CALL_SESSION__DEACTIVATE_REASON__DEACTIVATE_REASON_UNKNOWN;
import static com.android.internal.telephony.TelephonyStatsLog.DATA_CALL_SESSION__IP_TYPE__APN_PROTOCOL_IPV4;

import android.annotation.Nullable;
import android.os.SystemClock;
import android.telephony.Annotation.ApnType;
import android.telephony.Annotation.DataFailureCause;
import android.telephony.Annotation.NetworkType;
import android.telephony.DataFailCause;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting.ProtocolType;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataService;
import android.telephony.data.DataService.DeactivateDataReason;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.nano.PersistAtomsProto.DataCallSession;
import com.android.telephony.Rlog;

import java.util.Arrays;
import java.util.Random;

/** Collects data call change events per DataConnection for the pulled atom. */
public class DataCallSessionStats {
    private static final String TAG = DataCallSessionStats.class.getSimpleName();
    private static final int SIZE_LIMIT_HANDOVER_FAILURE_CAUSES = 15;

    private final Phone mPhone;
    private long mStartTime;
    @Nullable private DataCallSession mDataCallSession;

    private final PersistAtomsStorage mAtomsStorage =
            PhoneFactory.getMetricsCollector().getAtomsStorage();

    private static final Random RANDOM = new Random();

    public DataCallSessionStats(Phone phone) {
        mPhone = phone;
    }

    /** Creates a new ongoing atom when data call is set up. */
    public synchronized void onSetupDataCall(@ApnType int apnTypeBitMask) {
        mDataCallSession = getDefaultProto(apnTypeBitMask);
        mStartTime = getTimeMillis();
        PhoneFactory.getMetricsCollector().registerOngoingDataCallStat(this);
    }

    /**
     * Updates the ongoing dataCall's atom for data call response event.
     *
     * @param response setup Data call response
     * @param currentRat The data call current Network Type
     * @param apnTypeBitmask APN type bitmask
     * @param protocol Data connection protocol
     * @param failureCause failure cause as per android.telephony.DataFailCause
     */
    public synchronized void onSetupDataCallResponse(
            @Nullable DataCallResponse response,
            @NetworkType int currentRat,
            @ApnType int apnTypeBitmask,
            @ProtocolType int protocol,
            @DataFailureCause int failureCause) {
        // there should've been a call to onSetupDataCall to initiate the atom,
        // so this method is being called out of order -> no metric will be logged
        if (mDataCallSession == null) {
            loge("onSetupDataCallResponse: no DataCallSession atom has been initiated.");
            return;
        }

        if (currentRat != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            mDataCallSession.ratAtEnd = currentRat;
            mDataCallSession.bandAtEnd =
                    (currentRat == TelephonyManager.NETWORK_TYPE_IWLAN)
                            ? 0
                            : ServiceStateStats.getBand(mPhone);
        }

        // only set if apn hasn't been set during setup
        if (mDataCallSession.apnTypeBitmask == 0) {
            mDataCallSession.apnTypeBitmask = apnTypeBitmask;
        }

        mDataCallSession.ipType = protocol;
        mDataCallSession.failureCause = failureCause;
        if (response != null) {
            mDataCallSession.suggestedRetryMillis =
                    (int) Math.min(response.getRetryDurationMillis(), Integer.MAX_VALUE);
            // If setup has failed, then store the atom
            if (failureCause != DataFailCause.NONE) {
                mDataCallSession.setupFailed = true;
                endDataCallSession();
            }
        }
    }

    /**
     * Updates the dataCall atom when data call is deactivated.
     *
     * @param reason Deactivate reason
     */
    public synchronized void setDeactivateDataCallReason(@DeactivateDataReason int reason) {
        // there should've been another call to initiate the atom,
        // so this method is being called out of order -> no metric will be logged
        if (mDataCallSession == null) {
            loge("setDeactivateDataCallReason: no DataCallSession atom has been initiated.");
            return;
        }
        switch (reason) {
            case DataService.REQUEST_REASON_NORMAL:
                mDataCallSession.deactivateReason =
                        DATA_CALL_SESSION__DEACTIVATE_REASON__DEACTIVATE_REASON_NORMAL;
                break;
            case DataService.REQUEST_REASON_SHUTDOWN:
                mDataCallSession.deactivateReason =
                        DATA_CALL_SESSION__DEACTIVATE_REASON__DEACTIVATE_REASON_RADIO_OFF;
                break;
            case DataService.REQUEST_REASON_HANDOVER:
                mDataCallSession.deactivateReason =
                        DATA_CALL_SESSION__DEACTIVATE_REASON__DEACTIVATE_REASON_HANDOVER;
                break;
            default:
                mDataCallSession.deactivateReason =
                        DATA_CALL_SESSION__DEACTIVATE_REASON__DEACTIVATE_REASON_UNKNOWN;
                break;
        }
    }

    /**
     * Stores the atom when DataConnection reaches DISCONNECTED state.
     *
     * @param failureCause failure cause as per android.telephony.DataFailCause
     */
    public synchronized void onDataCallDisconnected(@DataFailureCause int failureCause) {
        // there should've been another call to initiate the atom,
        // so this method is being called out of order -> no atom will be saved
        // this also happens when DataConnection is created, which is expected
        if (mDataCallSession == null) {
            logi("onDataCallDisconnected: no DataCallSession atom has been initiated.");
            return;
        }
        mDataCallSession.failureCause = failureCause;
        mDataCallSession.durationMinutes = convertMillisToMinutes(getTimeMillis() - mStartTime);
        endDataCallSession();
    }

    /**
     * Updates the atom when a handover fails. Note we only record distinct failure causes, as in
     * most cases retry failures are due to the same cause.
     *
     * @param failureCause failure cause as per android.telephony.DataFailCause
     */
    public synchronized void onHandoverFailure(@DataFailureCause int failureCause) {
        if (mDataCallSession != null
                && mDataCallSession.handoverFailureCauses.length
                < SIZE_LIMIT_HANDOVER_FAILURE_CAUSES) {
            int[] failureCauses = mDataCallSession.handoverFailureCauses;
            for (int cause : failureCauses) {
                if (failureCause == cause) return;
            }
            mDataCallSession.handoverFailureCauses = Arrays.copyOf(
                    failureCauses, failureCauses.length + 1);
            mDataCallSession.handoverFailureCauses[failureCauses.length] = failureCause;
        }
    }

    /**
     * Updates the atom when data registration state or RAT changes.
     *
     * <p>NOTE: in {@link ServiceStateTracker}, change of channel number will trigger data
     * registration state change.
     */
    public synchronized void onDrsOrRatChanged(@NetworkType int currentRat) {
        if (mDataCallSession != null && currentRat != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            if (mDataCallSession.ratAtEnd != currentRat) {
                mDataCallSession.ratSwitchCount++;
                mDataCallSession.ratAtEnd = currentRat;
            }
            // band may have changed even if RAT was the same
            mDataCallSession.bandAtEnd =
                    (currentRat == TelephonyManager.NETWORK_TYPE_IWLAN)
                            ? 0
                            : ServiceStateStats.getBand(mPhone);
        }
    }

    /**
     * Take a snapshot of the on-going data call segment to add to the atom storage.
     *
     * Note the following fields are reset after the snapshot:
     * - rat switch count
     * - handover failure causes
     */
    public synchronized void conclude() {
        if (mDataCallSession != null) {
            DataCallSession call = copyOf(mDataCallSession);
            long nowMillis = getTimeMillis();
            call.durationMinutes = convertMillisToMinutes(nowMillis - mStartTime);
            mStartTime = nowMillis;
            mDataCallSession.ratSwitchCount = 0L;
            mDataCallSession.handoverFailureCauses = new int[0];
            mAtomsStorage.addDataCallSession(call);
        }
    }

    /** Put the current data call to an end after being uploaded to AtomStorage. */
    private void endDataCallSession() {
        mDataCallSession.oosAtEnd = getIsOos();
        mDataCallSession.ongoing = false;
        // store for the data call list event, after DataCall is disconnected and entered into
        // inactive mode
        PhoneFactory.getMetricsCollector().unregisterOngoingDataCallStat(this);
        mAtomsStorage.addDataCallSession(mDataCallSession);
        mDataCallSession = null;
    }

    private static long convertMillisToMinutes(long millis) {
        return Math.round(millis / 60000.0);
    }

    private static DataCallSession copyOf(DataCallSession call) {
        DataCallSession copy = new DataCallSession();
        copy.dimension = call.dimension;
        copy.isMultiSim = call.isMultiSim;
        copy.isEsim = call.isEsim;
        copy.apnTypeBitmask = call.apnTypeBitmask;
        copy.carrierId = call.carrierId;
        copy.isRoaming = call.isRoaming;
        copy.ratAtEnd = call.ratAtEnd;
        copy.oosAtEnd = call.oosAtEnd;
        copy.ratSwitchCount = call.ratSwitchCount;
        copy.isOpportunistic = call.isOpportunistic;
        copy.ipType = call.ipType;
        copy.setupFailed = call.setupFailed;
        copy.failureCause = call.failureCause;
        copy.suggestedRetryMillis = call.suggestedRetryMillis;
        copy.deactivateReason = call.deactivateReason;
        copy.durationMinutes = call.durationMinutes;
        copy.ongoing = call.ongoing;
        copy.bandAtEnd = call.bandAtEnd;
        copy.handoverFailureCauses = Arrays.copyOf(call.handoverFailureCauses,
                call.handoverFailureCauses.length);
        return copy;
    }

    /** Creates a proto for a normal {@code DataCallSession} with default values. */
    private DataCallSession getDefaultProto(@ApnType int apnTypeBitmask) {
        DataCallSession proto = new DataCallSession();
        proto.dimension = RANDOM.nextInt();
        proto.isMultiSim = SimSlotState.isMultiSim();
        proto.isEsim = SimSlotState.isEsim(mPhone.getPhoneId());
        proto.apnTypeBitmask = apnTypeBitmask;
        proto.carrierId = mPhone.getCarrierId();
        proto.isRoaming = getIsRoaming();
        proto.oosAtEnd = false;
        proto.ratSwitchCount = 0L;
        proto.isOpportunistic = getIsOpportunistic();
        proto.ipType = DATA_CALL_SESSION__IP_TYPE__APN_PROTOCOL_IPV4;
        proto.setupFailed = false;
        proto.failureCause = DataFailCause.NONE;
        proto.suggestedRetryMillis = 0;
        proto.deactivateReason = DATA_CALL_SESSION__DEACTIVATE_REASON__DEACTIVATE_REASON_UNKNOWN;
        proto.durationMinutes = 0;
        proto.ongoing = true;
        proto.handoverFailureCauses = new int[0];
        return proto;
    }

    private boolean getIsRoaming() {
        ServiceStateTracker serviceStateTracker = mPhone.getServiceStateTracker();
        ServiceState serviceState =
                serviceStateTracker != null ? serviceStateTracker.getServiceState() : null;
        return serviceState != null ? serviceState.getRoaming() : false;
    }

    private boolean getIsOpportunistic() {
        SubscriptionController subController = SubscriptionController.getInstance();
        return subController != null ? subController.isOpportunistic(mPhone.getSubId()) : false;
    }

    private boolean getIsOos() {
        ServiceStateTracker serviceStateTracker = mPhone.getServiceStateTracker();
        ServiceState serviceState =
                serviceStateTracker != null ? serviceStateTracker.getServiceState() : null;
        return serviceState != null
                ? serviceState.getDataRegistrationState() == ServiceState.STATE_OUT_OF_SERVICE
                : false;
    }

    private void logi(String format, Object... args) {
        Rlog.i(TAG, "[" + mPhone.getPhoneId() + "]" + String.format(format, args));
    }

    private void loge(String format, Object... args) {
        Rlog.e(TAG, "[" + mPhone.getPhoneId() + "]" + String.format(format, args));
    }

    @VisibleForTesting
    protected long getTimeMillis() {
        return SystemClock.elapsedRealtime();
    }
}

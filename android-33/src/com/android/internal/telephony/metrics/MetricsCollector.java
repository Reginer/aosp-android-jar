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

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import static com.android.internal.telephony.TelephonyStatsLog.CARRIER_ID_TABLE_VERSION;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_DATA_SERVICE_SWITCH;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_SERVICE_STATE;
import static com.android.internal.telephony.TelephonyStatsLog.DATA_CALL_SESSION;
import static com.android.internal.telephony.TelephonyStatsLog.DEVICE_TELEPHONY_PROPERTIES;
import static com.android.internal.telephony.TelephonyStatsLog.GBA_EVENT;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_DEDICATED_BEARER_EVENT;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_DEDICATED_BEARER_LISTENER_EVENT;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_FEATURE_TAG_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_TERMINATION;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS;
import static com.android.internal.telephony.TelephonyStatsLog.OUTGOING_SMS;
import static com.android.internal.telephony.TelephonyStatsLog.PER_SIM_STATUS;
import static com.android.internal.telephony.TelephonyStatsLog.PRESENCE_NOTIFY_EVENT;
import static com.android.internal.telephony.TelephonyStatsLog.RCS_ACS_PROVISIONING_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.RCS_CLIENT_PROVISIONING_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.SIM_SLOT_STATE;
import static com.android.internal.telephony.TelephonyStatsLog.SIP_DELEGATE_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.SIP_MESSAGE_RESPONSE;
import static com.android.internal.telephony.TelephonyStatsLog.SIP_TRANSPORT_FEATURE_TAG_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.SIP_TRANSPORT_SESSION;
import static com.android.internal.telephony.TelephonyStatsLog.SUPPORTED_RADIO_ACCESS_FAMILY;
import static com.android.internal.telephony.TelephonyStatsLog.TELEPHONY_NETWORK_REQUESTS_V2;
import static com.android.internal.telephony.TelephonyStatsLog.UCE_EVENT_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_RAT_USAGE;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION;

import android.annotation.Nullable;
import android.app.StatsManager;
import android.content.Context;
import android.util.StatsEvent;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.nano.PersistAtomsProto.CellularDataServiceSwitch;
import com.android.internal.telephony.nano.PersistAtomsProto.CellularServiceState;
import com.android.internal.telephony.nano.PersistAtomsProto.DataCallSession;
import com.android.internal.telephony.nano.PersistAtomsProto.GbaEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsDedicatedBearerEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsDedicatedBearerListenerEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationFeatureTagStats;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationServiceDescStats;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationStats;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationTermination;
import com.android.internal.telephony.nano.PersistAtomsProto.IncomingSms;
import com.android.internal.telephony.nano.PersistAtomsProto.NetworkRequestsV2;
import com.android.internal.telephony.nano.PersistAtomsProto.OutgoingSms;
import com.android.internal.telephony.nano.PersistAtomsProto.PresenceNotifyEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.RcsAcsProvisioningStats;
import com.android.internal.telephony.nano.PersistAtomsProto.RcsClientProvisioningStats;
import com.android.internal.telephony.nano.PersistAtomsProto.SipDelegateStats;
import com.android.internal.telephony.nano.PersistAtomsProto.SipMessageResponse;
import com.android.internal.telephony.nano.PersistAtomsProto.SipTransportFeatureTagStats;
import com.android.internal.telephony.nano.PersistAtomsProto.SipTransportSession;
import com.android.internal.telephony.nano.PersistAtomsProto.UceEventStats;
import com.android.internal.telephony.nano.PersistAtomsProto.VoiceCallRatUsage;
import com.android.internal.telephony.nano.PersistAtomsProto.VoiceCallSession;
import com.android.internal.util.ConcurrentUtils;
import com.android.telephony.Rlog;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements statsd pullers for Telephony.
 *
 * <p>This class registers pullers to statsd, which will be called once a day to obtain telephony
 * statistics that cannot be sent to statsd in real time.
 */
public class MetricsCollector implements StatsManager.StatsPullAtomCallback {
    private static final String TAG = MetricsCollector.class.getSimpleName();

    /** Disables various restrictions to ease debugging during development. */
    private static final boolean DBG = false; // STOPSHIP if true

    /**
     * Sets atom pull cool down to 23 hours to help enforcing privacy requirement.
     *
     * <p>Applies to certain atoms. The interval of 23 hours leaves some margin for pull operations
     * that occur once a day.
     */
    private static final long MIN_COOLDOWN_MILLIS =
            DBG ? 10L * SECOND_IN_MILLIS : 23L * HOUR_IN_MILLIS;

    /**
     * Buckets with less than these many calls will be dropped.
     *
     * <p>Applies to metrics with duration fields. Currently used by voice call RAT usages.
     */
    private static final long MIN_CALLS_PER_BUCKET = DBG ? 0L : 5L;

    /** Bucket size in milliseconds to round call durations into. */
    private static final long DURATION_BUCKET_MILLIS =
            DBG ? 2L * SECOND_IN_MILLIS : 5L * MINUTE_IN_MILLIS;

    private static final StatsManager.PullAtomMetadata POLICY_PULL_DAILY =
            new StatsManager.PullAtomMetadata.Builder()
                    .setCoolDownMillis(MIN_COOLDOWN_MILLIS)
                    .build();

    private final PersistAtomsStorage mStorage;
    private final StatsManager mStatsManager;
    private final AirplaneModeStats mAirplaneModeStats;
    private final Set<DataCallSessionStats> mOngoingDataCallStats = ConcurrentHashMap.newKeySet();
    private static final Random sRandom = new Random();

    public MetricsCollector(Context context) {
        this(context, new PersistAtomsStorage(context));
    }

    /** Allows dependency injection. Used during unit tests. */
    @VisibleForTesting
    public MetricsCollector(Context context,
                            PersistAtomsStorage storage) {
        mStorage = storage;
        mStatsManager = (StatsManager) context.getSystemService(Context.STATS_MANAGER);
        if (mStatsManager != null) {
            registerAtom(CELLULAR_DATA_SERVICE_SWITCH, POLICY_PULL_DAILY);
            registerAtom(CELLULAR_SERVICE_STATE, POLICY_PULL_DAILY);
            registerAtom(SIM_SLOT_STATE, null);
            registerAtom(SUPPORTED_RADIO_ACCESS_FAMILY, null);
            registerAtom(VOICE_CALL_RAT_USAGE, POLICY_PULL_DAILY);
            registerAtom(VOICE_CALL_SESSION, POLICY_PULL_DAILY);
            registerAtom(INCOMING_SMS, POLICY_PULL_DAILY);
            registerAtom(OUTGOING_SMS, POLICY_PULL_DAILY);
            registerAtom(CARRIER_ID_TABLE_VERSION, null);
            registerAtom(DATA_CALL_SESSION, POLICY_PULL_DAILY);
            registerAtom(IMS_REGISTRATION_STATS, POLICY_PULL_DAILY);
            registerAtom(IMS_REGISTRATION_TERMINATION, POLICY_PULL_DAILY);
            registerAtom(TELEPHONY_NETWORK_REQUESTS_V2, POLICY_PULL_DAILY);
            registerAtom(IMS_REGISTRATION_FEATURE_TAG_STATS, POLICY_PULL_DAILY);
            registerAtom(RCS_CLIENT_PROVISIONING_STATS, POLICY_PULL_DAILY);
            registerAtom(RCS_ACS_PROVISIONING_STATS, POLICY_PULL_DAILY);
            registerAtom(SIP_DELEGATE_STATS, POLICY_PULL_DAILY);
            registerAtom(SIP_TRANSPORT_FEATURE_TAG_STATS, POLICY_PULL_DAILY);
            registerAtom(SIP_MESSAGE_RESPONSE, POLICY_PULL_DAILY);
            registerAtom(SIP_TRANSPORT_SESSION, POLICY_PULL_DAILY);
            registerAtom(DEVICE_TELEPHONY_PROPERTIES, null);
            registerAtom(IMS_DEDICATED_BEARER_LISTENER_EVENT, POLICY_PULL_DAILY);
            registerAtom(IMS_DEDICATED_BEARER_EVENT, POLICY_PULL_DAILY);
            registerAtom(IMS_REGISTRATION_SERVICE_DESC_STATS, POLICY_PULL_DAILY);
            registerAtom(UCE_EVENT_STATS, POLICY_PULL_DAILY);
            registerAtom(PRESENCE_NOTIFY_EVENT, POLICY_PULL_DAILY);
            registerAtom(GBA_EVENT, POLICY_PULL_DAILY);
            registerAtom(PER_SIM_STATUS, null);

            Rlog.d(TAG, "registered");
        } else {
            Rlog.e(TAG, "could not get StatsManager, atoms not registered");
        }

        mAirplaneModeStats = new AirplaneModeStats(context);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link StatsManager#PULL_SUCCESS} with list of atoms (potentially empty) if pull
     *     succeeded, {@link StatsManager#PULL_SKIP} if pull was too frequent or atom ID is
     *     unexpected.
     */
    @Override
    public int onPullAtom(int atomTag, List<StatsEvent> data) {
        switch (atomTag) {
            case CELLULAR_DATA_SERVICE_SWITCH:
                return pullCellularDataServiceSwitch(data);
            case CELLULAR_SERVICE_STATE:
                return pullCellularServiceState(data);
            case SIM_SLOT_STATE:
                return pullSimSlotState(data);
            case SUPPORTED_RADIO_ACCESS_FAMILY:
                return pullSupportedRadioAccessFamily(data);
            case VOICE_CALL_RAT_USAGE:
                return pullVoiceCallRatUsages(data);
            case VOICE_CALL_SESSION:
                return pullVoiceCallSessions(data);
            case INCOMING_SMS:
                return pullIncomingSms(data);
            case OUTGOING_SMS:
                return pullOutgoingSms(data);
            case CARRIER_ID_TABLE_VERSION:
                return pullCarrierIdTableVersion(data);
            case DATA_CALL_SESSION:
                return pullDataCallSession(data);
            case IMS_REGISTRATION_STATS:
                return pullImsRegistrationStats(data);
            case IMS_REGISTRATION_TERMINATION:
                return pullImsRegistrationTermination(data);
            case TELEPHONY_NETWORK_REQUESTS_V2:
                return pullTelephonyNetworkRequestsV2(data);
            case DEVICE_TELEPHONY_PROPERTIES:
                return pullDeviceTelephonyProperties(data);
            case IMS_REGISTRATION_FEATURE_TAG_STATS:
                return pullImsRegistrationFeatureTagStats(data);
            case RCS_CLIENT_PROVISIONING_STATS:
                return pullRcsClientProvisioningStats(data);
            case RCS_ACS_PROVISIONING_STATS:
                return pullRcsAcsProvisioningStats(data);
            case SIP_DELEGATE_STATS:
                return pullSipDelegateStats(data);
            case SIP_TRANSPORT_FEATURE_TAG_STATS:
                return pullSipTransportFeatureTagStats(data);
            case SIP_MESSAGE_RESPONSE:
                return pullSipMessageResponse(data);
            case SIP_TRANSPORT_SESSION:
                return pullSipTransportSession(data);
            case IMS_DEDICATED_BEARER_LISTENER_EVENT:
                return pullImsDedicatedBearerListenerEvent(data);
            case IMS_DEDICATED_BEARER_EVENT:
                return pullImsDedicatedBearerEvent(data);
            case IMS_REGISTRATION_SERVICE_DESC_STATS:
                return pullImsRegistrationServiceDescStats(data);
            case UCE_EVENT_STATS:
                return pullUceEventStats(data);
            case PRESENCE_NOTIFY_EVENT:
                return pullPresenceNotifyEvent(data);
            case GBA_EVENT:
                return pullGbaEvent(data);
            case PER_SIM_STATUS:
                return pullPerSimStatus(data);
            default:
                Rlog.e(TAG, String.format("unexpected atom ID %d", atomTag));
                return StatsManager.PULL_SKIP;
        }
    }

    /** Returns the {@link PersistAtomsStorage} backing the puller. */
    public PersistAtomsStorage getAtomsStorage() {
        return mStorage;
    }

    /**
     * Registers a {@link DataCallSessionStats} which will be pinged for on-going data calls when
     * data call atoms are pulled.
     */
    public void registerOngoingDataCallStat(DataCallSessionStats call) {
        mOngoingDataCallStats.add(call);
    }

    /** Unregisters a {@link DataCallSessionStats} when it no longer handles an active data call. */
    public void unregisterOngoingDataCallStat(DataCallSessionStats call) {
        mOngoingDataCallStats.remove(call);
    }

    private static int pullSimSlotState(List<StatsEvent> data) {
        SimSlotState state;
        try {
            state = SimSlotState.getCurrentState();
        } catch (RuntimeException e) {
            // UiccController has not been made yet
            return StatsManager.PULL_SKIP;
        }

        data.add(
                TelephonyStatsLog.buildStatsEvent(
                        SIM_SLOT_STATE,
                        state.numActiveSlots,
                        state.numActiveSims,
                        state.numActiveEsims));
        return StatsManager.PULL_SUCCESS;
    }

    private static int pullSupportedRadioAccessFamily(List<StatsEvent> data) {
        Phone[] phones = getPhonesIfAny();
        if (phones.length == 0) {
            return StatsManager.PULL_SKIP;
        }

        // The bitmask is defined in android.telephony.TelephonyManager.NetworkTypeBitMask
        long rafSupported = 0L;
        for (Phone phone : PhoneFactory.getPhones()) {
            rafSupported |= phone.getRadioAccessFamily();
        }

        data.add(TelephonyStatsLog.buildStatsEvent(SUPPORTED_RADIO_ACCESS_FAMILY, rafSupported));
        return StatsManager.PULL_SUCCESS;
    }

    private static int pullCarrierIdTableVersion(List<StatsEvent> data) {
        Phone[] phones = getPhonesIfAny();
        if (phones.length == 0) {
            return StatsManager.PULL_SKIP;
        } else {
            // All phones should have the same version of the carrier ID table, so only query the
            // first one.
            int version = phones[0].getCarrierIdListVersion();
            data.add(TelephonyStatsLog.buildStatsEvent(CARRIER_ID_TABLE_VERSION, version));
            return StatsManager.PULL_SUCCESS;
        }
    }

    private int pullVoiceCallRatUsages(List<StatsEvent> data) {
        VoiceCallRatUsage[] usages = mStorage.getVoiceCallRatUsages(MIN_COOLDOWN_MILLIS);
        if (usages != null) {
            // sort by carrier/RAT and remove buckets with insufficient number of calls
            Arrays.stream(usages)
                    .sorted(
                            Comparator.comparingLong(
                                    usage -> ((long) usage.carrierId << 32) | usage.rat))
                    .filter(usage -> usage.callCount >= MIN_CALLS_PER_BUCKET)
                    .forEach(usage -> data.add(buildStatsEvent(usage)));
            Rlog.d(
                    TAG,
                    String.format(
                            "%d out of %d VOICE_CALL_RAT_USAGE pulled",
                            data.size(), usages.length));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "VOICE_CALL_RAT_USAGE pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullVoiceCallSessions(List<StatsEvent> data) {
        VoiceCallSession[] calls = mStorage.getVoiceCallSessions(MIN_COOLDOWN_MILLIS);
        if (calls != null) {
            // call session list is already shuffled when calls were inserted
            Arrays.stream(calls).forEach(call -> data.add(buildStatsEvent(call)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "VOICE_CALL_SESSION pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullIncomingSms(List<StatsEvent> data) {
        IncomingSms[] smsList = mStorage.getIncomingSms(MIN_COOLDOWN_MILLIS);
        if (smsList != null) {
            // SMS list is already shuffled when SMS were inserted
            Arrays.stream(smsList).forEach(sms -> data.add(buildStatsEvent(sms)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "INCOMING_SMS pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullOutgoingSms(List<StatsEvent> data) {
        OutgoingSms[] smsList = mStorage.getOutgoingSms(MIN_COOLDOWN_MILLIS);
        if (smsList != null) {
            // SMS list is already shuffled when SMS were inserted
            Arrays.stream(smsList).forEach(sms -> data.add(buildStatsEvent(sms)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "OUTGOING_SMS pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullDataCallSession(List<StatsEvent> data) {
        // Include ongoing data call segments
        for (DataCallSessionStats stats : mOngoingDataCallStats) {
            stats.conclude();
        }

        DataCallSession[] dataCallSessions = mStorage.getDataCallSessions(MIN_COOLDOWN_MILLIS);
        if (dataCallSessions != null) {
            Arrays.stream(dataCallSessions)
                    .forEach(dataCall -> data.add(buildStatsEvent(dataCall)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "DATA_CALL_SESSION pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullCellularDataServiceSwitch(List<StatsEvent> data) {
        CellularDataServiceSwitch[] persistAtoms =
                mStorage.getCellularDataServiceSwitches(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            // list is already shuffled when instances were inserted
            Arrays.stream(persistAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "CELLULAR_DATA_SERVICE_SWITCH pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullCellularServiceState(List<StatsEvent> data) {
        // Include the latest durations
        for (Phone phone : getPhonesIfAny()) {
            phone.getServiceStateTracker().getServiceStateStats().conclude();
        }

        CellularServiceState[] persistAtoms =
                mStorage.getCellularServiceStates(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            // list is already shuffled when instances were inserted
            Arrays.stream(persistAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "CELLULAR_SERVICE_STATE pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullImsRegistrationStats(List<StatsEvent> data) {
        // Include the latest durations
        for (Phone phone : getPhonesIfAny()) {
            ImsPhone imsPhone = (ImsPhone) phone.getImsPhone();
            if (imsPhone != null) {
                imsPhone.getImsStats().conclude();
            }
        }

        ImsRegistrationStats[] persistAtoms = mStorage.getImsRegistrationStats(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            // list is already shuffled when instances were inserted
            Arrays.stream(persistAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "IMS_REGISTRATION_STATS pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullImsRegistrationTermination(List<StatsEvent> data) {
        ImsRegistrationTermination[] persistAtoms =
                mStorage.getImsRegistrationTerminations(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            // list is already shuffled when instances were inserted
            Arrays.stream(persistAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "IMS_REGISTRATION_TERMINATION pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullTelephonyNetworkRequestsV2(List<StatsEvent> data) {
        NetworkRequestsV2[] persistAtoms = mStorage.getNetworkRequestsV2(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "TELEPHONY_NETWORK_REQUESTS_V2 pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private static int pullDeviceTelephonyProperties(List<StatsEvent> data) {
        Phone[] phones = getPhonesIfAny();
        if (phones.length == 0) {
            return StatsManager.PULL_SKIP;
        }

        data.add(TelephonyStatsLog.buildStatsEvent(DEVICE_TELEPHONY_PROPERTIES,
                phones[0].isUsingNewDataStack()));
        return StatsManager.PULL_SUCCESS;
    }

    private int pullImsRegistrationFeatureTagStats(List<StatsEvent> data) {
        RcsStats.getInstance().onFlushIncompleteImsRegistrationFeatureTagStats();

        ImsRegistrationFeatureTagStats[] persistAtoms =
                mStorage.getImsRegistrationFeatureTagStats(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "IMS_REGISTRATION_FEATURE_TAG_STATS pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullRcsClientProvisioningStats(List<StatsEvent> data) {
        RcsClientProvisioningStats[] persistAtoms =
                mStorage.getRcsClientProvisioningStats(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "RCS_CLIENT_PROVISIONING_STATS pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullRcsAcsProvisioningStats(List<StatsEvent> data) {
        RcsStats.getInstance().onFlushIncompleteRcsAcsProvisioningStats();

        RcsAcsProvisioningStats[] persistAtoms =
                mStorage.getRcsAcsProvisioningStats(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "RCS_ACS_PROVISIONING_STATS pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullSipDelegateStats(List<StatsEvent> data) {
        SipDelegateStats[] persisAtoms =
                mStorage.getSipDelegateStats(MIN_COOLDOWN_MILLIS);
        if (persisAtoms != null) {
            Arrays.stream(persisAtoms)
                    .forEach(persisAtom -> data.add(buildStatsEvent(persisAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "SIP_DELEGATE_STATS pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullSipTransportFeatureTagStats(List<StatsEvent> data) {
        RcsStats.getInstance().concludeSipTransportFeatureTagsStat();

        SipTransportFeatureTagStats[] persisAtoms =
                mStorage.getSipTransportFeatureTagStats(MIN_COOLDOWN_MILLIS);
        if (persisAtoms != null) {
            Arrays.stream(persisAtoms)
                    .forEach(persisAtom -> data.add(buildStatsEvent(persisAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "SIP_DELEGATE_STATS pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullSipMessageResponse(List<StatsEvent> data) {
        SipMessageResponse[] persistAtoms =
                mStorage.getSipMessageResponse(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "RCS_SIP_MESSAGE_RESPONSE pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullSipTransportSession(List<StatsEvent> data) {
        SipTransportSession[] persistAtoms =
                mStorage.getSipTransportSession(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "RCS_SIP_TRANSPORT_SESSION pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullImsDedicatedBearerListenerEvent(List<StatsEvent> data) {
        ImsDedicatedBearerListenerEvent[] persistAtoms =
            mStorage.getImsDedicatedBearerListenerEvent(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "IMS_DEDICATED_BEARER_LISTENER_EVENT pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullImsDedicatedBearerEvent(List<StatsEvent> data) {
        ImsDedicatedBearerEvent[] persistAtoms =
            mStorage.getImsDedicatedBearerEvent(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "IMS_DEDICATED_BEARER_EVENT pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullImsRegistrationServiceDescStats(List<StatsEvent> data) {
        RcsStats.getInstance().onFlushIncompleteImsRegistrationServiceDescStats();
        ImsRegistrationServiceDescStats[] persistAtoms =
            mStorage.getImsRegistrationServiceDescStats(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "IMS_REGISTRATION_SERVICE_DESC_STATS pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullUceEventStats(List<StatsEvent> data) {
        UceEventStats[] persistAtoms = mStorage.getUceEventStats(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "UCE_EVENT_STATS pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullPresenceNotifyEvent(List<StatsEvent> data) {
        PresenceNotifyEvent[] persistAtoms = mStorage.getPresenceNotifyEvent(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "PRESENCE_NOTIFY_EVENT pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullGbaEvent(List<StatsEvent> data) {
        GbaEvent[] persistAtoms = mStorage.getGbaEvent(MIN_COOLDOWN_MILLIS);
        if (persistAtoms != null) {
            Arrays.stream(persistAtoms)
                .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "GBA_EVENT pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullPerSimStatus(List<StatsEvent> data) {
        int result = StatsManager.PULL_SKIP;
        for (Phone phone : getPhonesIfAny()) {
            PerSimStatus perSimStatus = PerSimStatus.getCurrentState(phone);
            if (perSimStatus == null) {
                continue;
            }
            StatsEvent statsEvent = TelephonyStatsLog.buildStatsEvent(
                    PER_SIM_STATUS,
                    phone.getPhoneId(), // simSlotIndex
                    perSimStatus.carrierId, // carrierId
                    perSimStatus.phoneNumberSourceUicc, // phoneNumberSourceUicc
                    perSimStatus.phoneNumberSourceCarrier, // phoneNumberSourceCarrier
                    perSimStatus.phoneNumberSourceIms, // phoneNumberSourceIms
                    perSimStatus.advancedCallingSettingEnabled, // volteEnabled
                    perSimStatus.voWiFiSettingEnabled, // wfcEnabled
                    perSimStatus.voWiFiModeSetting, // wfcMode
                    perSimStatus.voWiFiRoamingModeSetting, // wfcRoamingMode
                    perSimStatus.vtSettingEnabled, // videoCallingEnabled
                    perSimStatus.dataRoamingEnabled, // dataRoamingEnabled
                    perSimStatus.preferredNetworkType, // allowedNetworksByUser
                    perSimStatus.disabled2g, // is2gDisabled
                    perSimStatus.pin1Enabled, // isPin1Enabled
                    perSimStatus.minimumVoltageClass, // simVoltageClass
                    perSimStatus.userModifiedApnTypes); // userModifiedApnTypeBitmask
            data.add(statsEvent);
            result = StatsManager.PULL_SUCCESS;
        }
        return result;
    }

    /** Registers a pulled atom ID {@code atomId} with optional {@code policy} for pulling. */
    private void registerAtom(int atomId, @Nullable StatsManager.PullAtomMetadata policy) {
        mStatsManager.setPullAtomCallback(atomId, policy, ConcurrentUtils.DIRECT_EXECUTOR, this);
    }

    private static StatsEvent buildStatsEvent(CellularDataServiceSwitch serviceSwitch) {
        return TelephonyStatsLog.buildStatsEvent(
                CELLULAR_DATA_SERVICE_SWITCH,
                serviceSwitch.ratFrom,
                serviceSwitch.ratTo,
                serviceSwitch.simSlotIndex,
                serviceSwitch.isMultiSim,
                serviceSwitch.carrierId,
                serviceSwitch.switchCount);
    }

    private static StatsEvent buildStatsEvent(CellularServiceState state) {
        return TelephonyStatsLog.buildStatsEvent(
                CELLULAR_SERVICE_STATE,
                state.voiceRat,
                state.dataRat,
                state.voiceRoamingType,
                state.dataRoamingType,
                state.isEndc,
                state.simSlotIndex,
                state.isMultiSim,
                state.carrierId,
                (int) (round(state.totalTimeMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS),
                state.isEmergencyOnly);
    }

    private static StatsEvent buildStatsEvent(VoiceCallRatUsage usage) {
        return TelephonyStatsLog.buildStatsEvent(
                VOICE_CALL_RAT_USAGE,
                usage.carrierId,
                usage.rat,
                round(usage.totalDurationMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS,
                usage.callCount);
    }

    private static StatsEvent buildStatsEvent(VoiceCallSession session) {
        return TelephonyStatsLog.buildStatsEvent(
                VOICE_CALL_SESSION,
                session.bearerAtStart,
                session.bearerAtEnd,
                session.direction,
                session.setupDuration,
                session.setupFailed,
                session.disconnectReasonCode,
                session.disconnectExtraCode,
                session.disconnectExtraMessage,
                session.ratAtStart,
                session.ratAtEnd,
                session.ratSwitchCount,
                session.codecBitmask,
                session.concurrentCallCountAtStart,
                session.concurrentCallCountAtEnd,
                session.simSlotIndex,
                session.isMultiSim,
                session.isEsim,
                session.carrierId,
                session.srvccCompleted,
                session.srvccFailureCount,
                session.srvccCancellationCount,
                session.rttEnabled,
                session.isEmergency,
                session.isRoaming,
                // workaround: dimension required for keeping multiple pulled atoms
                sRandom.nextInt(),
                // New fields introduced in Android S
                session.signalStrengthAtEnd,
                session.bandAtEnd,
                session.setupDurationMillis,
                session.mainCodecQuality,
                session.videoEnabled,
                session.ratAtConnected,
                session.isMultiparty,
                session.callDuration);
    }

    private static StatsEvent buildStatsEvent(IncomingSms sms) {
        return TelephonyStatsLog.buildStatsEvent(
                INCOMING_SMS,
                sms.smsFormat,
                sms.smsTech,
                sms.rat,
                sms.smsType,
                sms.totalParts,
                sms.receivedParts,
                sms.blocked,
                sms.error,
                sms.isRoaming,
                sms.simSlotIndex,
                sms.isMultiSim,
                sms.isEsim,
                sms.carrierId,
                sms.messageId);
    }

    private static StatsEvent buildStatsEvent(OutgoingSms sms) {
        return TelephonyStatsLog.buildStatsEvent(
                OUTGOING_SMS,
                sms.smsFormat,
                sms.smsTech,
                sms.rat,
                sms.sendResult,
                sms.errorCode,
                sms.isRoaming,
                sms.isFromDefaultApp,
                sms.simSlotIndex,
                sms.isMultiSim,
                sms.isEsim,
                sms.carrierId,
                sms.messageId,
                sms.retryId,
                sms.intervalMillis);
    }

    private static StatsEvent buildStatsEvent(DataCallSession dataCallSession) {
        return TelephonyStatsLog.buildStatsEvent(
                DATA_CALL_SESSION,
                dataCallSession.dimension,
                dataCallSession.isMultiSim,
                dataCallSession.isEsim,
                0, // profile is deprecated, so we default to 0
                dataCallSession.apnTypeBitmask,
                dataCallSession.carrierId,
                dataCallSession.isRoaming,
                dataCallSession.ratAtEnd,
                dataCallSession.oosAtEnd,
                dataCallSession.ratSwitchCount,
                dataCallSession.isOpportunistic,
                dataCallSession.ipType,
                dataCallSession.setupFailed,
                dataCallSession.failureCause,
                dataCallSession.suggestedRetryMillis,
                dataCallSession.deactivateReason,
                round(dataCallSession.durationMinutes, DURATION_BUCKET_MILLIS / MINUTE_IN_MILLIS),
                dataCallSession.ongoing,
                dataCallSession.bandAtEnd,
                dataCallSession.handoverFailureCauses);
    }

    private static StatsEvent buildStatsEvent(ImsRegistrationStats stats) {
        return TelephonyStatsLog.buildStatsEvent(
                IMS_REGISTRATION_STATS,
                stats.carrierId,
                stats.simSlotIndex,
                stats.rat,
                (int) (round(stats.registeredMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS),
                (int) (round(stats.voiceCapableMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS),
                (int)
                        (round(stats.voiceAvailableMillis, DURATION_BUCKET_MILLIS)
                                / SECOND_IN_MILLIS),
                (int) (round(stats.smsCapableMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS),
                (int) (round(stats.smsAvailableMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS),
                (int) (round(stats.videoCapableMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS),
                (int)
                        (round(stats.videoAvailableMillis, DURATION_BUCKET_MILLIS)
                                / SECOND_IN_MILLIS),
                (int) (round(stats.utCapableMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS),
                (int) (round(stats.utAvailableMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS));
    }

    private static StatsEvent buildStatsEvent(ImsRegistrationTermination termination) {
        return TelephonyStatsLog.buildStatsEvent(
                IMS_REGISTRATION_TERMINATION,
                termination.carrierId,
                termination.isMultiSim,
                termination.ratAtEnd,
                termination.setupFailed,
                termination.reasonCode,
                termination.extraCode,
                termination.extraMessage,
                termination.count);
    }

    private static StatsEvent buildStatsEvent(NetworkRequestsV2 networkRequests) {
        return TelephonyStatsLog.buildStatsEvent(
                TELEPHONY_NETWORK_REQUESTS_V2,
                networkRequests.carrierId,
                networkRequests.capability,
                networkRequests.requestCount);
    }

    private static StatsEvent buildStatsEvent(ImsRegistrationFeatureTagStats stats) {
        return TelephonyStatsLog.buildStatsEvent(
                IMS_REGISTRATION_FEATURE_TAG_STATS,
                stats.carrierId,
                stats.slotId,
                stats.featureTagName,
                stats.registrationTech,
                (int) (round(stats.registeredMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS));
    }

    private static StatsEvent buildStatsEvent(RcsClientProvisioningStats stats) {
        return TelephonyStatsLog.buildStatsEvent(
                RCS_CLIENT_PROVISIONING_STATS,
                stats.carrierId,
                stats.slotId,
                stats.event,
                stats.count);
    }

    private static StatsEvent buildStatsEvent(RcsAcsProvisioningStats stats) {
        return TelephonyStatsLog.buildStatsEvent(
                RCS_ACS_PROVISIONING_STATS,
                stats.carrierId,
                stats.slotId,
                stats.responseCode,
                stats.responseType,
                stats.isSingleRegistrationEnabled,
                stats.count,
                (int) (round(stats.stateTimerMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS));
    }

    private static StatsEvent buildStatsEvent(SipDelegateStats stats) {
        return TelephonyStatsLog.buildStatsEvent(
                SIP_DELEGATE_STATS,
                stats.dimension,
                stats.carrierId,
                stats.slotId,
                (int) (round(stats.uptimeMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS),
                stats.destroyReason);
    }

    private static StatsEvent buildStatsEvent(SipTransportFeatureTagStats stats) {
        return TelephonyStatsLog.buildStatsEvent(
                SIP_TRANSPORT_FEATURE_TAG_STATS,
                stats.carrierId,
                stats.slotId,
                stats.featureTagName,
                stats.sipTransportDeniedReason,
                stats.sipTransportDeregisteredReason,
                (int) (round(stats.associatedMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS));
    }

    private static StatsEvent buildStatsEvent(SipMessageResponse stats) {
        return TelephonyStatsLog.buildStatsEvent(
                SIP_MESSAGE_RESPONSE,
                stats.carrierId,
                stats.slotId,
                stats.sipMessageMethod,
                stats.sipMessageResponse,
                stats.sipMessageDirection,
                stats.messageError,
                stats.count);
    }

    private static StatsEvent buildStatsEvent(SipTransportSession stats) {
        return TelephonyStatsLog.buildStatsEvent(
                SIP_TRANSPORT_SESSION,
                stats.carrierId,
                stats.slotId,
                stats.sessionMethod,
                stats.sipMessageDirection,
                stats.sipResponse,
                stats.sessionCount,
                stats.endedGracefullyCount);
    }

    private static StatsEvent buildStatsEvent(ImsDedicatedBearerListenerEvent stats) {
        return TelephonyStatsLog.buildStatsEvent(
                IMS_DEDICATED_BEARER_LISTENER_EVENT,
                stats.carrierId,
                stats.slotId,
                stats.ratAtEnd,
                stats.qci,
                stats.dedicatedBearerEstablished,
                stats.eventCount);
    }

    private static StatsEvent buildStatsEvent(ImsDedicatedBearerEvent stats) {
        return TelephonyStatsLog.buildStatsEvent(
                IMS_DEDICATED_BEARER_EVENT,
                stats.carrierId,
                stats.slotId,
                stats.ratAtEnd,
                stats.qci,
                stats.bearerState,
                stats.localConnectionInfoReceived,
                stats.remoteConnectionInfoReceived,
                stats.hasListeners,
                stats.count);
    }

    private static StatsEvent buildStatsEvent(ImsRegistrationServiceDescStats stats) {
        return TelephonyStatsLog.buildStatsEvent(
                IMS_REGISTRATION_SERVICE_DESC_STATS,
                stats.carrierId,
                stats.slotId,
                stats.serviceIdName,
                stats.serviceIdVersion,
                stats.registrationTech,
                (int) (round(stats.publishedMillis, DURATION_BUCKET_MILLIS) / SECOND_IN_MILLIS));
    }

    private static StatsEvent buildStatsEvent(UceEventStats stats) {
        return TelephonyStatsLog.buildStatsEvent(
                UCE_EVENT_STATS,
                stats.carrierId,
                stats.slotId,
                stats.type,
                stats.successful,
                stats.commandCode,
                stats.networkResponse,
                stats.count);
    }

    private static StatsEvent buildStatsEvent(PresenceNotifyEvent stats) {
        return TelephonyStatsLog.buildStatsEvent(
                PRESENCE_NOTIFY_EVENT,
                stats.carrierId,
                stats.slotId,
                stats.reason,
                stats.contentBodyReceived,
                stats.rcsCapsCount,
                stats.mmtelCapsCount,
                stats.noCapsCount,
                stats.count);
    }

    private static StatsEvent buildStatsEvent(GbaEvent stats) {
        return TelephonyStatsLog.buildStatsEvent(
                GBA_EVENT,
                stats.carrierId,
                stats.slotId,
                stats.successful,
                stats.failedReason,
                stats.count);
    }

    /** Returns all phones in {@link PhoneFactory}, or an empty array if phones not made yet. */
    private static Phone[] getPhonesIfAny() {
        try {
            return PhoneFactory.getPhones();
        } catch (IllegalStateException e) {
            // Phones have not been made yet
            return new Phone[0];
        }
    }

    /** Returns the value rounded to the bucket. */
    private static long round(long value, long bucket) {
        return bucket == 0 ? value : ((value + bucket / 2) / bucket) * bucket;
    }
}

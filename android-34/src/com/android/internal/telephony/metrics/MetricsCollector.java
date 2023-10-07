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

import static com.android.internal.telephony.TelephonyStatsLog.CARRIER_ID_TABLE_VERSION;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_DATA_SERVICE_SWITCH;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_SERVICE_STATE;
import static com.android.internal.telephony.TelephonyStatsLog.DATA_CALL_SESSION;
import static com.android.internal.telephony.TelephonyStatsLog.DEVICE_TELEPHONY_PROPERTIES;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO;
import static com.android.internal.telephony.TelephonyStatsLog.GBA_EVENT;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_DEDICATED_BEARER_EVENT;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_DEDICATED_BEARER_LISTENER_EVENT;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_FEATURE_TAG_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_TERMINATION;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS;
import static com.android.internal.telephony.TelephonyStatsLog.OUTGOING_SHORT_CODE_SMS;
import static com.android.internal.telephony.TelephonyStatsLog.OUTGOING_SMS;
import static com.android.internal.telephony.TelephonyStatsLog.PER_SIM_STATUS;
import static com.android.internal.telephony.TelephonyStatsLog.PRESENCE_NOTIFY_EVENT;
import static com.android.internal.telephony.TelephonyStatsLog.RCS_ACS_PROVISIONING_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.RCS_CLIENT_PROVISIONING_STATS;
import static com.android.internal.telephony.TelephonyStatsLog.SATELLITE_CONTROLLER;
import static com.android.internal.telephony.TelephonyStatsLog.SATELLITE_INCOMING_DATAGRAM;
import static com.android.internal.telephony.TelephonyStatsLog.SATELLITE_OUTGOING_DATAGRAM;
import static com.android.internal.telephony.TelephonyStatsLog.SATELLITE_PROVISION;
import static com.android.internal.telephony.TelephonyStatsLog.SATELLITE_SESSION;
import static com.android.internal.telephony.TelephonyStatsLog.SATELLITE_SOS_MESSAGE_RECOMMENDER;
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
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__CALL_DURATION__CALL_DURATION_UNKNOWN;

import android.app.StatsManager;
import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.StatsEvent;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.internal.telephony.emergency.EmergencyNumberTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.nano.PersistAtomsProto.CellularDataServiceSwitch;
import com.android.internal.telephony.nano.PersistAtomsProto.CellularServiceState;
import com.android.internal.telephony.nano.PersistAtomsProto.DataCallSession;
import com.android.internal.telephony.nano.PersistAtomsProto.EmergencyNumbersInfo;
import com.android.internal.telephony.nano.PersistAtomsProto.GbaEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsDedicatedBearerEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsDedicatedBearerListenerEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationFeatureTagStats;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationServiceDescStats;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationStats;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationTermination;
import com.android.internal.telephony.nano.PersistAtomsProto.IncomingSms;
import com.android.internal.telephony.nano.PersistAtomsProto.NetworkRequestsV2;
import com.android.internal.telephony.nano.PersistAtomsProto.OutgoingShortCodeSms;
import com.android.internal.telephony.nano.PersistAtomsProto.OutgoingSms;
import com.android.internal.telephony.nano.PersistAtomsProto.PresenceNotifyEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.RcsAcsProvisioningStats;
import com.android.internal.telephony.nano.PersistAtomsProto.RcsClientProvisioningStats;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteController;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteIncomingDatagram;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteOutgoingDatagram;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteProvision;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteSession;
import com.android.internal.telephony.nano.PersistAtomsProto.SatelliteSosMessageRecommender;
import com.android.internal.telephony.nano.PersistAtomsProto.SipDelegateStats;
import com.android.internal.telephony.nano.PersistAtomsProto.SipMessageResponse;
import com.android.internal.telephony.nano.PersistAtomsProto.SipTransportFeatureTagStats;
import com.android.internal.telephony.nano.PersistAtomsProto.SipTransportSession;
import com.android.internal.telephony.nano.PersistAtomsProto.UceEventStats;
import com.android.internal.telephony.nano.PersistAtomsProto.VoiceCallRatUsage;
import com.android.internal.telephony.nano.PersistAtomsProto.VoiceCallSession;
import com.android.internal.util.ConcurrentUtils;
import com.android.telephony.Rlog;

import java.time.Duration;
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

    private static final long MILLIS_PER_HOUR = Duration.ofHours(1).toMillis();
    private static final long MILLIS_PER_MINUTE = Duration.ofMinutes(1).toMillis();
    private static final long MILLIS_PER_SECOND = Duration.ofSeconds(1).toMillis();

    /**
     * Sets atom pull cool down to 23 hours to help enforcing privacy requirement.
     *
     * <p>Applies to certain atoms. The interval of 23 hours leaves some margin for pull operations
     * that occur once a day.
     */
    private static final long MIN_COOLDOWN_MILLIS =
            DBG ? 10L * MILLIS_PER_SECOND : 23L * MILLIS_PER_HOUR;

    /**
     * Buckets with less than these many calls will be dropped.
     *
     * <p>Applies to metrics with duration fields. Currently used by voice call RAT usages.
     */
    private static final long MIN_CALLS_PER_BUCKET = DBG ? 0L : 5L;

    /** Bucket size in milliseconds to round call durations into. */
    private static final long DURATION_BUCKET_MILLIS =
            DBG ? 2L * MILLIS_PER_SECOND : 5L * MILLIS_PER_MINUTE;

    private final PersistAtomsStorage mStorage;
    private final DeviceStateHelper mDeviceStateHelper;
    private final StatsManager mStatsManager;
    private final AirplaneModeStats mAirplaneModeStats;
    private final Set<DataCallSessionStats> mOngoingDataCallStats = ConcurrentHashMap.newKeySet();
    private static final Random sRandom = new Random();

    public MetricsCollector(Context context) {
        this(context, new PersistAtomsStorage(context), new DeviceStateHelper(context));
    }

    /** Allows dependency injection. Used during unit tests. */
    @VisibleForTesting
    public MetricsCollector(
            Context context, PersistAtomsStorage storage, DeviceStateHelper deviceStateHelper) {
        mStorage = storage;
        mDeviceStateHelper = deviceStateHelper;
        mStatsManager = (StatsManager) context.getSystemService(Context.STATS_MANAGER);
        if (mStatsManager != null) {
            // Most (but not all) of these are subject to cooldown specified by MIN_COOLDOWN_MILLIS.
            registerAtom(CELLULAR_DATA_SERVICE_SWITCH);
            registerAtom(CELLULAR_SERVICE_STATE);
            registerAtom(SIM_SLOT_STATE);
            registerAtom(SUPPORTED_RADIO_ACCESS_FAMILY);
            registerAtom(VOICE_CALL_RAT_USAGE);
            registerAtom(VOICE_CALL_SESSION);
            registerAtom(INCOMING_SMS);
            registerAtom(OUTGOING_SMS);
            registerAtom(CARRIER_ID_TABLE_VERSION);
            registerAtom(DATA_CALL_SESSION);
            registerAtom(IMS_REGISTRATION_STATS);
            registerAtom(IMS_REGISTRATION_TERMINATION);
            registerAtom(TELEPHONY_NETWORK_REQUESTS_V2);
            registerAtom(IMS_REGISTRATION_FEATURE_TAG_STATS);
            registerAtom(RCS_CLIENT_PROVISIONING_STATS);
            registerAtom(RCS_ACS_PROVISIONING_STATS);
            registerAtom(SIP_DELEGATE_STATS);
            registerAtom(SIP_TRANSPORT_FEATURE_TAG_STATS);
            registerAtom(SIP_MESSAGE_RESPONSE);
            registerAtom(SIP_TRANSPORT_SESSION);
            registerAtom(DEVICE_TELEPHONY_PROPERTIES);
            registerAtom(IMS_DEDICATED_BEARER_LISTENER_EVENT);
            registerAtom(IMS_DEDICATED_BEARER_EVENT);
            registerAtom(IMS_REGISTRATION_SERVICE_DESC_STATS);
            registerAtom(UCE_EVENT_STATS);
            registerAtom(PRESENCE_NOTIFY_EVENT);
            registerAtom(GBA_EVENT);
            registerAtom(PER_SIM_STATUS);
            registerAtom(OUTGOING_SHORT_CODE_SMS);
            registerAtom(SATELLITE_CONTROLLER);
            registerAtom(SATELLITE_SESSION);
            registerAtom(SATELLITE_INCOMING_DATAGRAM);
            registerAtom(SATELLITE_OUTGOING_DATAGRAM);
            registerAtom(SATELLITE_PROVISION);
            registerAtom(SATELLITE_SOS_MESSAGE_RECOMMENDER);
            registerAtom(EMERGENCY_NUMBERS_INFO);
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
            case OUTGOING_SHORT_CODE_SMS:
                return pullOutgoingShortCodeSms(data);
            case SATELLITE_CONTROLLER:
                return pullSatelliteController(data);
            case SATELLITE_SESSION:
                return pullSatelliteSession(data);
            case SATELLITE_INCOMING_DATAGRAM:
                return pullSatelliteIncomingDatagram(data);
            case SATELLITE_OUTGOING_DATAGRAM:
                return pullSatelliteOutgoingDatagram(data);
            case SATELLITE_PROVISION:
                return pullSatelliteProvision(data);
            case SATELLITE_SOS_MESSAGE_RECOMMENDER:
                return pullSatelliteSosMessageRecommender(data);
            case EMERGENCY_NUMBERS_INFO:
                return pullEmergencyNumbersInfo(data);
            default:
                Rlog.e(TAG, String.format("unexpected atom ID %d", atomTag));
                return StatsManager.PULL_SKIP;
        }
    }

    /** Returns the {@link PersistAtomsStorage} backing the puller. */
    public PersistAtomsStorage getAtomsStorage() {
        return mStorage;
    }

    /** Returns the {@link DeviceStateHelper}. */
    public DeviceStateHelper getDeviceStateHelper() {
        return mDeviceStateHelper;
    }

    /** Updates duration segments and calls {@link PersistAtomsStorage#flushAtoms()}. */
    public void flushAtomsStorage() {
        concludeAll();
        mStorage.flushAtoms();
    }

    /** Updates duration segments and calls {@link PersistAtomsStorage#clearAtoms()}. */
    public void clearAtomsStorage() {
        concludeAll();
        mStorage.clearAtoms();
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

    private void concludeDataCallSessionStats() {
        for (DataCallSessionStats stats : mOngoingDataCallStats) {
            stats.conclude();
        }
    }

    private void concludeImsStats() {
        for (Phone phone : getPhonesIfAny()) {
            ImsPhone imsPhone = (ImsPhone) phone.getImsPhone();
            if (imsPhone != null) {
                imsPhone.getImsStats().conclude();
            }
        }
    }

    private void concludeServiceStateStats() {
        for (Phone phone : getPhonesIfAny()) {
            phone.getServiceStateTracker().getServiceStateStats().conclude();
        }
    }

    private void concludeRcsStats() {
        RcsStats rcsStats = RcsStats.getInstance();
        if (rcsStats != null) {
            rcsStats.concludeSipTransportFeatureTagsStat();
            rcsStats.onFlushIncompleteRcsAcsProvisioningStats();
            rcsStats.onFlushIncompleteImsRegistrationServiceDescStats();
            rcsStats.onFlushIncompleteImsRegistrationFeatureTagStats();
        }
    }

    private void concludeAll() {
        concludeDataCallSessionStats();
        concludeImsStats();
        concludeServiceStateStats();
        concludeRcsStats();
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
        concludeDataCallSessionStats();
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
        concludeServiceStateStats();
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
        concludeImsStats();
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

    private int pullDeviceTelephonyProperties(List<StatsEvent> data) {
        Phone[] phones = getPhonesIfAny();
        if (phones.length == 0) {
            return StatsManager.PULL_SKIP;
        }
        boolean isAutoDataSwitchOn = Arrays.stream(phones)
                .anyMatch(phone ->
                        phone.getSubId() != SubscriptionManager.getDefaultDataSubscriptionId()
                                && phone.getDataSettingsManager().isMobileDataPolicyEnabled(
                        TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH));
        boolean hasDedicatedManagedProfileSub = Arrays.stream(phones)
                .anyMatch(Phone::isManagedProfile);

        data.add(TelephonyStatsLog.buildStatsEvent(DEVICE_TELEPHONY_PROPERTIES, true,
                isAutoDataSwitchOn, mStorage.getAutoDataSwitchToggleCount(),
                hasDedicatedManagedProfileSub));
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
                    perSimStatus.userModifiedApnTypes, // userModifiedApnTypeBitmask
                    perSimStatus.unmeteredNetworks, // unmeteredNetworks
                    perSimStatus.vonrEnabled); // vonrEnabled
            data.add(statsEvent);
            result = StatsManager.PULL_SUCCESS;
        }
        return result;
    }

    private int pullOutgoingShortCodeSms(List<StatsEvent> data) {
        OutgoingShortCodeSms[] outgoingShortCodeSmsList = mStorage
                .getOutgoingShortCodeSms(MIN_COOLDOWN_MILLIS);
        if (outgoingShortCodeSmsList != null) {
            // Outgoing short code SMS list is already shuffled when SMS were inserted
            Arrays.stream(outgoingShortCodeSmsList).forEach(sms -> data.add(buildStatsEvent(sms)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "OUTGOING_SHORT_CODE_SMS pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullSatelliteController(List<StatsEvent> data) {
        SatelliteController[] controllerAtoms =
                mStorage.getSatelliteControllerStats(MIN_COOLDOWN_MILLIS);
        if (controllerAtoms != null) {
            Arrays.stream(controllerAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "SATELLITE_CONTROLLER pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullSatelliteSession(List<StatsEvent> data) {
        SatelliteSession[] sessionAtoms =
                mStorage.getSatelliteSessionStats(MIN_COOLDOWN_MILLIS);
        if (sessionAtoms != null) {
            Arrays.stream(sessionAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "SATELLITE_SESSION pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullSatelliteIncomingDatagram(List<StatsEvent> data) {
        SatelliteIncomingDatagram[] incomingDatagramAtoms =
                mStorage.getSatelliteIncomingDatagramStats(MIN_COOLDOWN_MILLIS);
        if (incomingDatagramAtoms != null) {
            Arrays.stream(incomingDatagramAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "SATELLITE_INCOMING_DATAGRAM pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }


    private int pullSatelliteOutgoingDatagram(List<StatsEvent> data) {
        SatelliteOutgoingDatagram[] outgoingDatagramAtoms =
                mStorage.getSatelliteOutgoingDatagramStats(MIN_COOLDOWN_MILLIS);
        if (outgoingDatagramAtoms != null) {
            Arrays.stream(outgoingDatagramAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "SATELLITE_OUTGOING_DATAGRAM pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }


    private int pullSatelliteProvision(List<StatsEvent> data) {
        SatelliteProvision[] provisionAtoms =
                mStorage.getSatelliteProvisionStats(MIN_COOLDOWN_MILLIS);
        if (provisionAtoms != null) {
            Arrays.stream(provisionAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "SATELLITE_PROVISION pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullSatelliteSosMessageRecommender(List<StatsEvent> data) {
        SatelliteSosMessageRecommender[] sosMessageRecommenderAtoms =
                mStorage.getSatelliteSosMessageRecommenderStats(MIN_COOLDOWN_MILLIS);
        if (sosMessageRecommenderAtoms != null) {
            Arrays.stream(sosMessageRecommenderAtoms)
                    .forEach(persistAtom -> data.add(buildStatsEvent(persistAtom)));
            return StatsManager.PULL_SUCCESS;
        } else {
            Rlog.w(TAG, "SATELLITE_SOS_MESSAGE_RECOMMENDER pull too frequent, skipping");
            return StatsManager.PULL_SKIP;
        }
    }

    private int pullEmergencyNumbersInfo(List<StatsEvent> data) {
        boolean isDataLogged = false;
        for (Phone phone : getPhonesIfAny()) {
            if (phone != null) {
                EmergencyNumberTracker tracker = phone.getEmergencyNumberTracker();
                if (tracker != null) {
                    EmergencyNumbersInfo[] numList = tracker.getEmergencyNumbersProtoArray();
                    Arrays.stream(numList).forEach(number -> data.add(buildStatsEvent(number)));
                    isDataLogged = true;
                }
            }
        }
        return isDataLogged ? StatsManager.PULL_SUCCESS : StatsManager.PULL_SKIP;
    }

    /** Registers a pulled atom ID {@code atomId}. */
    private void registerAtom(int atomId) {
        mStatsManager.setPullAtomCallback(atomId, /* metadata= */ null,
                ConcurrentUtils.DIRECT_EXECUTOR, this);
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
                roundAndConvertMillisToSeconds(state.totalTimeMillis),
                state.isEmergencyOnly,
                state.isInternetPdnUp,
                state.foldState);
    }

    private static StatsEvent buildStatsEvent(VoiceCallRatUsage usage) {
        return TelephonyStatsLog.buildStatsEvent(
                VOICE_CALL_RAT_USAGE,
                usage.carrierId,
                usage.rat,
                roundAndConvertMillisToSeconds(usage.totalDurationMillis),
                usage.callCount);
    }

    private static StatsEvent buildStatsEvent(VoiceCallSession session) {
        return TelephonyStatsLog.buildStatsEvent(
                VOICE_CALL_SESSION,
                session.bearerAtStart,
                session.bearerAtEnd,
                session.direction,
                // deprecated and replaced by setupDurationMillis
                VOICE_CALL_SESSION__CALL_DURATION__CALL_DURATION_UNKNOWN,
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
                session.callDuration,
                session.lastKnownRat,
                session.foldState);
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
                sms.messageId,
                sms.count,
                sms.isManagedProfile);
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
                sms.intervalMillis,
                sms.count,
                sms.sendErrorCode,
                sms.networkErrorCode,
                sms.isManagedProfile);
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
                roundAndConvertMillisToMinutes(
                        dataCallSession.durationMinutes * MILLIS_PER_MINUTE),
                dataCallSession.ongoing,
                dataCallSession.bandAtEnd,
                dataCallSession.handoverFailureCauses,
                dataCallSession.handoverFailureRat,
                dataCallSession.isNonDds);
    }

    private static StatsEvent buildStatsEvent(ImsRegistrationStats stats) {
        return TelephonyStatsLog.buildStatsEvent(
                IMS_REGISTRATION_STATS,
                stats.carrierId,
                stats.simSlotIndex,
                stats.rat,
                roundAndConvertMillisToSeconds(stats.registeredMillis),
                roundAndConvertMillisToSeconds(stats.voiceCapableMillis),
                roundAndConvertMillisToSeconds(stats.voiceAvailableMillis),
                roundAndConvertMillisToSeconds(stats.smsCapableMillis),
                roundAndConvertMillisToSeconds(stats.smsAvailableMillis),
                roundAndConvertMillisToSeconds(stats.videoCapableMillis),
                roundAndConvertMillisToSeconds(stats.videoAvailableMillis),
                roundAndConvertMillisToSeconds(stats.utCapableMillis),
                roundAndConvertMillisToSeconds(stats.utAvailableMillis));
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
                roundAndConvertMillisToSeconds(stats.registeredMillis));
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
                roundAndConvertMillisToSeconds(stats.stateTimerMillis));
    }

    private static StatsEvent buildStatsEvent(SipDelegateStats stats) {
        return TelephonyStatsLog.buildStatsEvent(
                SIP_DELEGATE_STATS,
                stats.dimension,
                stats.carrierId,
                stats.slotId,
                roundAndConvertMillisToSeconds(stats.uptimeMillis),
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
                roundAndConvertMillisToSeconds(stats.associatedMillis));
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
                roundAndConvertMillisToSeconds(stats.publishedMillis));
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

    private static StatsEvent buildStatsEvent(OutgoingShortCodeSms shortCodeSms) {
        return TelephonyStatsLog.buildStatsEvent(
                OUTGOING_SHORT_CODE_SMS,
                shortCodeSms.category,
                shortCodeSms.xmlVersion,
                shortCodeSms.shortCodeSmsCount);
    }

    private static StatsEvent buildStatsEvent(SatelliteController satelliteController) {
        return TelephonyStatsLog.buildStatsEvent(
                SATELLITE_CONTROLLER,
                satelliteController.countOfSatelliteServiceEnablementsSuccess,
                satelliteController.countOfSatelliteServiceEnablementsFail,
                satelliteController.countOfOutgoingDatagramSuccess,
                satelliteController.countOfOutgoingDatagramFail,
                satelliteController.countOfIncomingDatagramSuccess,
                satelliteController.countOfIncomingDatagramFail,
                satelliteController.countOfDatagramTypeSosSmsSuccess,
                satelliteController.countOfDatagramTypeSosSmsFail,
                satelliteController.countOfDatagramTypeLocationSharingSuccess,
                satelliteController.countOfDatagramTypeLocationSharingFail,
                satelliteController.countOfProvisionSuccess,
                satelliteController.countOfProvisionFail,
                satelliteController.countOfDeprovisionSuccess,
                satelliteController.countOfDeprovisionFail,
                satelliteController.totalServiceUptimeSec,
                satelliteController.totalBatteryConsumptionPercent,
                satelliteController.totalBatteryChargedTimeSec);
    }

    private static StatsEvent buildStatsEvent(SatelliteSession satelliteSession) {
        return TelephonyStatsLog.buildStatsEvent(
                SATELLITE_SESSION,
                satelliteSession.satelliteServiceInitializationResult,
                satelliteSession.satelliteTechnology,
                satelliteSession.count);
    }

    private static StatsEvent buildStatsEvent(SatelliteIncomingDatagram stats) {
        return TelephonyStatsLog.buildStatsEvent(
                SATELLITE_INCOMING_DATAGRAM,
                stats.resultCode,
                stats.datagramSizeBytes,
                stats.datagramTransferTimeMillis);
    }

    private static StatsEvent buildStatsEvent(SatelliteOutgoingDatagram stats) {
        return TelephonyStatsLog.buildStatsEvent(
                SATELLITE_OUTGOING_DATAGRAM,
                stats.datagramType,
                stats.resultCode,
                stats.datagramSizeBytes,
                stats.datagramTransferTimeMillis);
    }

    private static StatsEvent buildStatsEvent(SatelliteProvision stats) {
        return TelephonyStatsLog.buildStatsEvent(
                SATELLITE_PROVISION,
                stats.resultCode,
                stats.provisioningTimeSec,
                stats.isProvisionRequest,
                stats.isCanceled);
    }

    private static StatsEvent buildStatsEvent(SatelliteSosMessageRecommender stats) {
        return TelephonyStatsLog.buildStatsEvent(
                SATELLITE_SOS_MESSAGE_RECOMMENDER,
                stats.isDisplaySosMessageSent,
                stats.countOfTimerStarted,
                stats.isImsRegistered,
                stats.cellularServiceState,
                stats.count);
    }

    private static StatsEvent buildStatsEvent(EmergencyNumbersInfo emergencyNumber) {
        return TelephonyStatsLog.buildStatsEvent(
                EMERGENCY_NUMBERS_INFO,
                emergencyNumber.isDbVersionIgnored,
                emergencyNumber.assetVersion,
                emergencyNumber.otaVersion,
                emergencyNumber.number,
                emergencyNumber.countryIso,
                emergencyNumber.mnc,
                emergencyNumber.route,
                emergencyNumber.urns,
                emergencyNumber.serviceCategories,
                emergencyNumber.sources);
    }

    /** Returns all phones in {@link PhoneFactory}, or an empty array if phones not made yet. */
    static Phone[] getPhonesIfAny() {
        try {
            return PhoneFactory.getPhones();
        } catch (IllegalStateException e) {
            // Phones have not been made yet
            return new Phone[0];
        }
    }

    /**
     * Rounds the duration and converts it from milliseconds to seconds.
     */
    private static int roundAndConvertMillisToSeconds(long valueMillis) {
        long roundedValueMillis = Math.round((double) valueMillis / DURATION_BUCKET_MILLIS)
                * DURATION_BUCKET_MILLIS;
        return (int) (roundedValueMillis / MILLIS_PER_SECOND);
    }

    /**
     * Rounds the duration and converts it from milliseconds to minutes.
     */
    private static int roundAndConvertMillisToMinutes(long valueMillis) {
        long roundedValueMillis = Math.round((double) valueMillis / DURATION_BUCKET_MILLIS)
                * DURATION_BUCKET_MILLIS;
        return (int) (roundedValueMillis / MILLIS_PER_MINUTE);
    }
}

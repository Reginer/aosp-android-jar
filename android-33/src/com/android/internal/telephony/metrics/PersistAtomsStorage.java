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

import static android.text.format.DateUtils.DAY_IN_MILLIS;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.telephony.TelephonyManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.nano.PersistAtomsProto.CarrierIdMismatch;
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
import com.android.internal.telephony.nano.PersistAtomsProto.PersistAtoms;
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
import com.android.internal.util.ArrayUtils;
import com.android.telephony.Rlog;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Stores and aggregates metrics that should not be pulled at arbitrary frequency.
 *
 * <p>NOTE: while this class checks timestamp against {@code minIntervalMillis}, it is {@link
 * MetricsCollector}'s responsibility to ensure {@code minIntervalMillis} is set correctly.
 */
public class PersistAtomsStorage {
    private static final String TAG = PersistAtomsStorage.class.getSimpleName();

    /** Name of the file where cached statistics are saved to. */
    private static final String FILENAME = "persist_atoms.pb";

    /** Delay to store atoms to persistent storage to bundle multiple operations together. */
    private static final int SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS = 30000;

    /**
     * Delay to store atoms to persistent storage during pulls to avoid unnecessary operations.
     *
     * <p>This delay should be short to avoid duplicating atoms or losing pull timestamp in case of
     * crash or power loss.
     */
    private static final int SAVE_TO_FILE_DELAY_FOR_GET_MILLIS = 500;

    /** Maximum number of call sessions to store between pulls. */
    private final int mMaxNumVoiceCallSessions;

    /**
     * Maximum number of SMS to store between pulls. Incoming messages and outgoing messages are
     * counted separately.
     */
    private final int mMaxNumSms;

    /**
     * Maximum number of carrier ID mismatch events stored on the device to avoid sending duplicated
     * metrics.
     */
    private final int mMaxNumCarrierIdMismatches;

    /** Maximum number of data call sessions to store during pulls. */
    private final int mMaxNumDataCallSessions;

    /** Maximum number of service states to store between pulls. */
    private final int mMaxNumCellularServiceStates;

    /** Maximum number of data service switches to store between pulls. */
    private final int mMaxNumCellularDataSwitches;

    /** Maximum number of IMS registration stats to store between pulls. */
    private final int mMaxNumImsRegistrationStats;

    /** Maximum number of IMS registration terminations to store between pulls. */
    private final int mMaxNumImsRegistrationTerminations;

    /** Maximum number of IMS Registration Feature Tags to store between pulls. */
    private final int mMaxNumImsRegistrationFeatureStats;

    /** Maximum number of RCS Client Provisioning to store between pulls. */
    private final int mMaxNumRcsClientProvisioningStats;

    /** Maximum number of RCS Acs Provisioning to store between pulls. */
    private final int mMaxNumRcsAcsProvisioningStats;

    /** Maximum number of Sip Message Response to store between pulls. */
    private final int mMaxNumSipMessageResponseStats;

    /** Maximum number of Sip Transport Session to store between pulls. */
    private final int mMaxNumSipTransportSessionStats;

    /** Maximum number of Sip Delegate to store between pulls. */
    private final int mMaxNumSipDelegateStats;

    /** Maximum number of Sip Transport Feature Tag to store between pulls. */
    private final int mMaxNumSipTransportFeatureTagStats;

    /** Maximum number of Dedicated Bearer Listener Event to store between pulls. */
    private final int mMaxNumDedicatedBearerListenerEventStats;

    /** Maximum number of Dedicated Bearer Event to store between pulls. */
    private final int mMaxNumDedicatedBearerEventStats;

    /** Maximum number of IMS Registration Service Desc to store between pulls. */
    private final int mMaxNumImsRegistrationServiceDescStats;

    /** Maximum number of UCE Event to store between pulls. */
    private final int mMaxNumUceEventStats;

    /** Maximum number of Presence Notify Event to store between pulls. */
    private final int mMaxNumPresenceNotifyEventStats;

    /** Maximum number of GBA Event to store between pulls. */
    private final int mMaxNumGbaEventStats;

    /** Stores persist atoms and persist states of the puller. */
    @VisibleForTesting protected final PersistAtoms mAtoms;

    /** Aggregates RAT duration and call count. */
    private final VoiceCallRatTracker mVoiceCallRatTracker;

    /** Whether atoms should be saved immediately, skipping the delay. */
    @VisibleForTesting protected boolean mSaveImmediately;

    private final Context mContext;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private static final SecureRandom sRandom = new SecureRandom();

    private Runnable mSaveRunnable =
            new Runnable() {
                @Override
                public void run() {
                    saveAtomsToFileNow();
                }
            };

    public PersistAtomsStorage(Context context) {
        mContext = context;

        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_RAM_LOW)) {
            Rlog.i(TAG, "Low RAM device");
            mMaxNumVoiceCallSessions = 10;
            mMaxNumSms = 5;
            mMaxNumCarrierIdMismatches = 8;
            mMaxNumDataCallSessions = 5;
            mMaxNumCellularServiceStates = 10;
            mMaxNumCellularDataSwitches = 5;
            mMaxNumImsRegistrationStats = 5;
            mMaxNumImsRegistrationTerminations = 5;
            mMaxNumImsRegistrationFeatureStats = 15;
            mMaxNumRcsClientProvisioningStats = 5;
            mMaxNumRcsAcsProvisioningStats = 5;
            mMaxNumSipMessageResponseStats = 10;
            mMaxNumSipTransportSessionStats = 10;
            mMaxNumSipDelegateStats = 5;
            mMaxNumSipTransportFeatureTagStats = 15;
            mMaxNumDedicatedBearerListenerEventStats = 5;
            mMaxNumDedicatedBearerEventStats = 5;
            mMaxNumImsRegistrationServiceDescStats = 15;
            mMaxNumUceEventStats = 5;
            mMaxNumPresenceNotifyEventStats = 10;
            mMaxNumGbaEventStats = 5;
        } else {
            mMaxNumVoiceCallSessions = 50;
            mMaxNumSms = 25;
            mMaxNumCarrierIdMismatches = 40;
            mMaxNumDataCallSessions = 15;
            mMaxNumCellularServiceStates = 50;
            mMaxNumCellularDataSwitches = 50;
            mMaxNumImsRegistrationStats = 10;
            mMaxNumImsRegistrationTerminations = 10;
            mMaxNumImsRegistrationFeatureStats = 25;
            mMaxNumRcsClientProvisioningStats = 10;
            mMaxNumRcsAcsProvisioningStats = 10;
            mMaxNumSipMessageResponseStats = 25;
            mMaxNumSipTransportSessionStats = 25;
            mMaxNumSipDelegateStats = 10;
            mMaxNumSipTransportFeatureTagStats = 25;
            mMaxNumDedicatedBearerListenerEventStats = 10;
            mMaxNumDedicatedBearerEventStats = 10;
            mMaxNumImsRegistrationServiceDescStats = 25;
            mMaxNumUceEventStats = 25;
            mMaxNumPresenceNotifyEventStats = 50;
            mMaxNumGbaEventStats = 10;
        }

        mAtoms = loadAtomsFromFile();
        mVoiceCallRatTracker = VoiceCallRatTracker.fromProto(mAtoms.voiceCallRatUsage);

        mHandlerThread = new HandlerThread("PersistAtomsThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mSaveImmediately = false;
    }

    /** Adds a call to the storage. */
    public synchronized void addVoiceCallSession(VoiceCallSession call) {
        mAtoms.voiceCallSession =
                insertAtRandomPlace(mAtoms.voiceCallSession, call, mMaxNumVoiceCallSessions);
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);

        Rlog.d(TAG, "Add new voice call session: " + call.toString());
    }

    /** Adds RAT usages to the storage when a call session ends. */
    public synchronized void addVoiceCallRatUsage(VoiceCallRatTracker ratUsages) {
        mVoiceCallRatTracker.mergeWith(ratUsages);
        mAtoms.voiceCallRatUsage = mVoiceCallRatTracker.toProto();
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds an incoming SMS to the storage. */
    public synchronized void addIncomingSms(IncomingSms sms) {
        mAtoms.incomingSms = insertAtRandomPlace(mAtoms.incomingSms, sms, mMaxNumSms);
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);

        // To be removed
        Rlog.d(TAG, "Add new incoming SMS atom: " + sms.toString());
    }

    /** Adds an outgoing SMS to the storage. */
    public synchronized void addOutgoingSms(OutgoingSms sms) {
        // Update the retry id, if needed, so that it's unique and larger than all
        // previous ones. (this algorithm ignores the fact that some SMS atoms might
        // be dropped due to limit in size of the array).
        for (OutgoingSms storedSms : mAtoms.outgoingSms) {
            if (storedSms.messageId == sms.messageId && storedSms.retryId >= sms.retryId) {
                sms.retryId = storedSms.retryId + 1;
            }
        }

        mAtoms.outgoingSms = insertAtRandomPlace(mAtoms.outgoingSms, sms, mMaxNumSms);
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);

        // To be removed
        Rlog.d(TAG, "Add new outgoing SMS atom: " + sms.toString());
    }

    /** Adds a service state to the storage, together with data service switch if any. */
    public synchronized void addCellularServiceStateAndCellularDataServiceSwitch(
            CellularServiceState state, @Nullable CellularDataServiceSwitch serviceSwitch) {
        CellularServiceState existingState = find(state);
        if (existingState != null) {
            existingState.totalTimeMillis += state.totalTimeMillis;
            existingState.lastUsedMillis = getWallTimeMillis();
        } else {
            state.lastUsedMillis = getWallTimeMillis();
            mAtoms.cellularServiceState =
                    insertAtRandomPlace(
                            mAtoms.cellularServiceState, state, mMaxNumCellularServiceStates);
        }

        if (serviceSwitch != null) {
            CellularDataServiceSwitch existingSwitch = find(serviceSwitch);
            if (existingSwitch != null) {
                existingSwitch.switchCount += serviceSwitch.switchCount;
                existingSwitch.lastUsedMillis = getWallTimeMillis();
            } else {
                serviceSwitch.lastUsedMillis = getWallTimeMillis();
                mAtoms.cellularDataServiceSwitch =
                        insertAtRandomPlace(
                                mAtoms.cellularDataServiceSwitch,
                                serviceSwitch,
                                mMaxNumCellularDataSwitches);
            }
        }

        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a data call session to the storage. */
    public synchronized void addDataCallSession(DataCallSession dataCall) {
        int index = findIndex(dataCall);
        if (index >= 0) {
            DataCallSession existingCall = mAtoms.dataCallSession[index];
            dataCall.ratSwitchCount += existingCall.ratSwitchCount;
            dataCall.durationMinutes += existingCall.durationMinutes;
            mAtoms.dataCallSession[index] = dataCall;
        } else {
            mAtoms.dataCallSession =
                    insertAtRandomPlace(mAtoms.dataCallSession, dataCall, mMaxNumDataCallSessions);
        }

        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /**
     * Adds a new carrier ID mismatch event to the storage.
     *
     * @return true if the item was not present and was added to the persistent storage, false
     *     otherwise.
     */
    public synchronized boolean addCarrierIdMismatch(CarrierIdMismatch carrierIdMismatch) {
        // Check if the details of the SIM cards are already present and in case return.
        if (find(carrierIdMismatch) != null) {
            return false;
        }
        // Add the new CarrierIdMismatch at the end of the array, so that the same atom will not be
        // sent again in future.
        if (mAtoms.carrierIdMismatch.length == mMaxNumCarrierIdMismatches) {
            System.arraycopy(
                    mAtoms.carrierIdMismatch,
                    1,
                    mAtoms.carrierIdMismatch,
                    0,
                    mMaxNumCarrierIdMismatches - 1);
            mAtoms.carrierIdMismatch[mMaxNumCarrierIdMismatches - 1] = carrierIdMismatch;
        } else {
            int newLength = mAtoms.carrierIdMismatch.length + 1;
            mAtoms.carrierIdMismatch = Arrays.copyOf(mAtoms.carrierIdMismatch, newLength);
            mAtoms.carrierIdMismatch[newLength - 1] = carrierIdMismatch;
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
        return true;
    }

    /** Adds IMS registration stats to the storage. */
    public synchronized void addImsRegistrationStats(ImsRegistrationStats stats) {
        ImsRegistrationStats existingStats = find(stats);
        if (existingStats != null) {
            existingStats.registeredMillis += stats.registeredMillis;
            existingStats.voiceCapableMillis += stats.voiceCapableMillis;
            existingStats.voiceAvailableMillis += stats.voiceAvailableMillis;
            existingStats.smsCapableMillis += stats.smsCapableMillis;
            existingStats.smsAvailableMillis += stats.smsAvailableMillis;
            existingStats.videoCapableMillis += stats.videoCapableMillis;
            existingStats.videoAvailableMillis += stats.videoAvailableMillis;
            existingStats.utCapableMillis += stats.utCapableMillis;
            existingStats.utAvailableMillis += stats.utAvailableMillis;
            existingStats.lastUsedMillis = getWallTimeMillis();
        } else {
            stats.lastUsedMillis = getWallTimeMillis();
            mAtoms.imsRegistrationStats =
                    insertAtRandomPlace(
                            mAtoms.imsRegistrationStats, stats, mMaxNumImsRegistrationStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds IMS registration termination to the storage. */
    public synchronized void addImsRegistrationTermination(ImsRegistrationTermination termination) {
        ImsRegistrationTermination existingTermination = find(termination);
        if (existingTermination != null) {
            existingTermination.count += termination.count;
            existingTermination.lastUsedMillis = getWallTimeMillis();
        } else {
            termination.lastUsedMillis = getWallTimeMillis();
            mAtoms.imsRegistrationTermination =
                    insertAtRandomPlace(
                            mAtoms.imsRegistrationTermination,
                            termination,
                            mMaxNumImsRegistrationTerminations);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /**
     * Stores the version of the carrier ID matching table.
     *
     * @return true if the version is newer than last available version, false otherwise.
     */
    public synchronized boolean setCarrierIdTableVersion(int carrierIdTableVersion) {
        if (mAtoms.carrierIdTableVersion < carrierIdTableVersion) {
            mAtoms.carrierIdTableVersion = carrierIdTableVersion;
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
            return true;
        } else {
            return false;
        }
    }

    /** Adds a new {@link NetworkRequestsV2} to the storage. */
    public synchronized void addNetworkRequestsV2(NetworkRequestsV2 networkRequests) {
        NetworkRequestsV2 existingMetrics = find(networkRequests);
        if (existingMetrics != null) {
            existingMetrics.requestCount += networkRequests.requestCount;
        } else {
            NetworkRequestsV2 newMetrics = new NetworkRequestsV2();
            newMetrics.capability = networkRequests.capability;
            newMetrics.carrierId = networkRequests.carrierId;
            newMetrics.requestCount = networkRequests.requestCount;
            int newLength = mAtoms.networkRequestsV2.length + 1;
            mAtoms.networkRequestsV2 = Arrays.copyOf(mAtoms.networkRequestsV2, newLength);
            mAtoms.networkRequestsV2[newLength - 1] = newMetrics;
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link ImsRegistrationFeatureTagStats} to the storage. */
    public synchronized void addImsRegistrationFeatureTagStats(
                ImsRegistrationFeatureTagStats stats) {
        ImsRegistrationFeatureTagStats existingStats = find(stats);
        if (existingStats != null) {
            existingStats.registeredMillis += stats.registeredMillis;
        } else {
            mAtoms.imsRegistrationFeatureTagStats =
                insertAtRandomPlace(mAtoms.imsRegistrationFeatureTagStats,
                    stats, mMaxNumImsRegistrationFeatureStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link RcsClientProvisioningStats} to the storage. */
    public synchronized void addRcsClientProvisioningStats(RcsClientProvisioningStats stats) {
        RcsClientProvisioningStats existingStats = find(stats);
        if (existingStats != null) {
            existingStats.count += 1;
        } else {
            mAtoms.rcsClientProvisioningStats =
                insertAtRandomPlace(mAtoms.rcsClientProvisioningStats, stats,
                        mMaxNumRcsClientProvisioningStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link RcsAcsProvisioningStats} to the storage. */
    public synchronized void addRcsAcsProvisioningStats(RcsAcsProvisioningStats stats) {
        RcsAcsProvisioningStats existingStats = find(stats);
        if (existingStats != null) {
            existingStats.count += 1;
            existingStats.stateTimerMillis += stats.stateTimerMillis;
        } else {
            // prevent that wrong count from caller effects total count
            stats.count = 1;
            mAtoms.rcsAcsProvisioningStats =
                insertAtRandomPlace(mAtoms.rcsAcsProvisioningStats, stats,
                        mMaxNumRcsAcsProvisioningStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link SipDelegateStats} to the storage. */
    public synchronized void addSipDelegateStats(SipDelegateStats stats) {
        mAtoms.sipDelegateStats = insertAtRandomPlace(mAtoms.sipDelegateStats, stats,
                mMaxNumSipDelegateStats);
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link SipTransportFeatureTagStats} to the storage. */
    public synchronized void addSipTransportFeatureTagStats(SipTransportFeatureTagStats stats) {
        SipTransportFeatureTagStats lastStat = find(stats);
        if (lastStat != null) {
            lastStat.associatedMillis += stats.associatedMillis;
        } else {
            mAtoms.sipTransportFeatureTagStats =
                    insertAtRandomPlace(mAtoms.sipTransportFeatureTagStats, stats,
                            mMaxNumSipTransportFeatureTagStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link SipMessageResponse} to the storage. */
    public synchronized void addSipMessageResponse(SipMessageResponse stats) {
        SipMessageResponse existingStats = find(stats);
        if (existingStats != null) {
            existingStats.count += 1;
        } else {
            mAtoms.sipMessageResponse = insertAtRandomPlace(mAtoms.sipMessageResponse, stats,
                    mMaxNumSipMessageResponseStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link SipTransportSession} to the storage. */
    public synchronized void addCompleteSipTransportSession(SipTransportSession stats) {
        SipTransportSession existingStats = find(stats);
        if (existingStats != null) {
            existingStats.sessionCount += 1;
            if (stats.isEndedGracefully) {
                existingStats.endedGracefullyCount += 1;
            }
        } else {
            mAtoms.sipTransportSession =
                    insertAtRandomPlace(mAtoms.sipTransportSession, stats,
                            mMaxNumSipTransportSessionStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link ImsDedicatedBearerListenerEvent} to the storage. */
    public synchronized void addImsDedicatedBearerListenerEvent(
                ImsDedicatedBearerListenerEvent stats) {
        ImsDedicatedBearerListenerEvent existingStats = find(stats);
        if (existingStats != null) {
            existingStats.eventCount += 1;
        } else {
            mAtoms.imsDedicatedBearerListenerEvent =
                insertAtRandomPlace(mAtoms.imsDedicatedBearerListenerEvent,
                    stats, mMaxNumDedicatedBearerListenerEventStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link ImsDedicatedBearerEvent} to the storage. */
    public synchronized void addImsDedicatedBearerEvent(ImsDedicatedBearerEvent stats) {
        ImsDedicatedBearerEvent existingStats = find(stats);
        if (existingStats != null) {
            existingStats.count += 1;
        } else {
            mAtoms.imsDedicatedBearerEvent =
                insertAtRandomPlace(mAtoms.imsDedicatedBearerEvent, stats,
                        mMaxNumDedicatedBearerEventStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link ImsRegistrationServiceDescStats} to the storage. */
    public synchronized void addImsRegistrationServiceDescStats(
            ImsRegistrationServiceDescStats stats) {
        ImsRegistrationServiceDescStats existingStats = find(stats);
        if (existingStats != null) {
            existingStats.publishedMillis += stats.publishedMillis;
        } else {
            mAtoms.imsRegistrationServiceDescStats =
                insertAtRandomPlace(mAtoms.imsRegistrationServiceDescStats,
                    stats, mMaxNumImsRegistrationServiceDescStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link UceEventStats} to the storage. */
    public synchronized void addUceEventStats(UceEventStats stats) {
        UceEventStats existingStats = find(stats);
        if (existingStats != null) {
            existingStats.count += 1;
        } else {
            mAtoms.uceEventStats =
                insertAtRandomPlace(mAtoms.uceEventStats, stats, mMaxNumUceEventStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link PresenceNotifyEvent} to the storage. */
    public synchronized void addPresenceNotifyEvent(PresenceNotifyEvent stats) {
        PresenceNotifyEvent existingStats = find(stats);
        if (existingStats != null) {
            existingStats.rcsCapsCount += stats.rcsCapsCount;
            existingStats.mmtelCapsCount += stats.mmtelCapsCount;
            existingStats.noCapsCount += stats.noCapsCount;
            existingStats.count += stats.count;
        } else {
            mAtoms.presenceNotifyEvent =
                insertAtRandomPlace(mAtoms.presenceNotifyEvent, stats,
                        mMaxNumPresenceNotifyEventStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a new {@link GbaEvent} to the storage. */
    public synchronized void addGbaEvent(GbaEvent stats) {
        GbaEvent existingStats = find(stats);
        if (existingStats != null) {
            existingStats.count += 1;
        } else {
            mAtoms.gbaEvent =
                insertAtRandomPlace(mAtoms.gbaEvent, stats, mMaxNumGbaEventStats);
        }
        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /**
     * Returns and clears the voice call sessions if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized VoiceCallSession[] getVoiceCallSessions(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.voiceCallSessionPullTimestampMillis > minIntervalMillis) {
            mAtoms.voiceCallSessionPullTimestampMillis = getWallTimeMillis();
            VoiceCallSession[] previousCalls = mAtoms.voiceCallSession;
            mAtoms.voiceCallSession = new VoiceCallSession[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousCalls;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the voice call RAT usages if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized VoiceCallRatUsage[] getVoiceCallRatUsages(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.voiceCallRatUsagePullTimestampMillis > minIntervalMillis) {
            mAtoms.voiceCallRatUsagePullTimestampMillis = getWallTimeMillis();
            VoiceCallRatUsage[] previousUsages = mAtoms.voiceCallRatUsage;
            mVoiceCallRatTracker.clear();
            mAtoms.voiceCallRatUsage = new VoiceCallRatUsage[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousUsages;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the incoming SMS if last pulled longer than {@code minIntervalMillis} ago,
     * otherwise returns {@code null}.
     */
    @Nullable
    public synchronized IncomingSms[] getIncomingSms(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.incomingSmsPullTimestampMillis > minIntervalMillis) {
            mAtoms.incomingSmsPullTimestampMillis = getWallTimeMillis();
            IncomingSms[] previousIncomingSms = mAtoms.incomingSms;
            mAtoms.incomingSms = new IncomingSms[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousIncomingSms;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the outgoing SMS if last pulled longer than {@code minIntervalMillis} ago,
     * otherwise returns {@code null}.
     */
    @Nullable
    public synchronized OutgoingSms[] getOutgoingSms(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.outgoingSmsPullTimestampMillis > minIntervalMillis) {
            mAtoms.outgoingSmsPullTimestampMillis = getWallTimeMillis();
            OutgoingSms[] previousOutgoingSms = mAtoms.outgoingSms;
            mAtoms.outgoingSms = new OutgoingSms[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousOutgoingSms;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the data call session if last pulled longer than {@code minIntervalMillis}
     * ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized DataCallSession[] getDataCallSessions(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.dataCallSessionPullTimestampMillis > minIntervalMillis) {
            mAtoms.dataCallSessionPullTimestampMillis = getWallTimeMillis();
            DataCallSession[] previousDataCallSession = mAtoms.dataCallSession;
            mAtoms.dataCallSession = new DataCallSession[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            for (DataCallSession dataCallSession : previousDataCallSession) {
                // sort to de-correlate any potential pattern for UII concern
                Arrays.sort(dataCallSession.handoverFailureCauses);
            }
            return previousDataCallSession;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the service state durations if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized CellularServiceState[] getCellularServiceStates(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.cellularServiceStatePullTimestampMillis
                > minIntervalMillis) {
            mAtoms.cellularServiceStatePullTimestampMillis = getWallTimeMillis();
            CellularServiceState[] previousStates = mAtoms.cellularServiceState;
            Arrays.stream(previousStates).forEach(state -> state.lastUsedMillis = 0L);
            mAtoms.cellularServiceState = new CellularServiceState[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStates;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the service state durations if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized CellularDataServiceSwitch[] getCellularDataServiceSwitches(
            long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.cellularDataServiceSwitchPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.cellularDataServiceSwitchPullTimestampMillis = getWallTimeMillis();
            CellularDataServiceSwitch[] previousSwitches = mAtoms.cellularDataServiceSwitch;
            Arrays.stream(previousSwitches)
                    .forEach(serviceSwitch -> serviceSwitch.lastUsedMillis = 0L);
            mAtoms.cellularDataServiceSwitch = new CellularDataServiceSwitch[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousSwitches;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the IMS registration statistics normalized to 24h cycle if last
     * pulled longer than {@code minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized ImsRegistrationStats[] getImsRegistrationStats(long minIntervalMillis) {
        long intervalMillis =
                getWallTimeMillis() - mAtoms.imsRegistrationStatsPullTimestampMillis;
        if (intervalMillis > minIntervalMillis) {
            mAtoms.imsRegistrationStatsPullTimestampMillis = getWallTimeMillis();
            ImsRegistrationStats[] previousStats = mAtoms.imsRegistrationStats;
            Arrays.stream(previousStats).forEach(stats -> stats.lastUsedMillis = 0L);
            mAtoms.imsRegistrationStats = new ImsRegistrationStats[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return normalizeData(previousStats, intervalMillis);
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the IMS registration terminations if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized ImsRegistrationTermination[] getImsRegistrationTerminations(
            long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.imsRegistrationTerminationPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.imsRegistrationTerminationPullTimestampMillis = getWallTimeMillis();
            ImsRegistrationTermination[] previousTerminations = mAtoms.imsRegistrationTermination;
            Arrays.stream(previousTerminations)
                    .forEach(termination -> termination.lastUsedMillis = 0L);
            mAtoms.imsRegistrationTermination = new ImsRegistrationTermination[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousTerminations;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the network requests if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized NetworkRequestsV2[] getNetworkRequestsV2(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.networkRequestsV2PullTimestampMillis > minIntervalMillis) {
            mAtoms.networkRequestsV2PullTimestampMillis = getWallTimeMillis();
            NetworkRequestsV2[] previousNetworkRequests = mAtoms.networkRequestsV2;
            mAtoms.networkRequestsV2 = new NetworkRequestsV2[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousNetworkRequests;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the ImsRegistrationFeatureTagStats if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized ImsRegistrationFeatureTagStats[] getImsRegistrationFeatureTagStats(
            long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.imsRegistrationFeatureTagStatsPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.imsRegistrationFeatureTagStatsPullTimestampMillis = getWallTimeMillis();
            ImsRegistrationFeatureTagStats[] previousStats =
                    mAtoms.imsRegistrationFeatureTagStats;
            mAtoms.imsRegistrationFeatureTagStats = new ImsRegistrationFeatureTagStats[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the RcsClientProvisioningStats if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized RcsClientProvisioningStats[] getRcsClientProvisioningStats(
            long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.rcsClientProvisioningStatsPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.rcsClientProvisioningStatsPullTimestampMillis = getWallTimeMillis();
            RcsClientProvisioningStats[] previousStats = mAtoms.rcsClientProvisioningStats;
            mAtoms.rcsClientProvisioningStats = new RcsClientProvisioningStats[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the RcsAcsProvisioningStats if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized RcsAcsProvisioningStats[] getRcsAcsProvisioningStats(
            long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.rcsAcsProvisioningStatsPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.rcsAcsProvisioningStatsPullTimestampMillis = getWallTimeMillis();
            RcsAcsProvisioningStats[] previousStats = mAtoms.rcsAcsProvisioningStats;
            mAtoms.rcsAcsProvisioningStats = new RcsAcsProvisioningStats[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the SipDelegateStats if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized SipDelegateStats[] getSipDelegateStats(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.sipDelegateStatsPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.sipDelegateStatsPullTimestampMillis = getWallTimeMillis();
            SipDelegateStats[] previousStats = mAtoms.sipDelegateStats;
            mAtoms.sipDelegateStats = new SipDelegateStats[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the SipTransportFeatureTagStats if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized SipTransportFeatureTagStats[] getSipTransportFeatureTagStats(
            long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.sipTransportFeatureTagStatsPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.sipTransportFeatureTagStatsPullTimestampMillis = getWallTimeMillis();
            SipTransportFeatureTagStats[] previousStats = mAtoms.sipTransportFeatureTagStats;
            mAtoms.sipTransportFeatureTagStats = new SipTransportFeatureTagStats[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the SipMessageResponse if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized SipMessageResponse[] getSipMessageResponse(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.sipMessageResponsePullTimestampMillis
                > minIntervalMillis) {
            mAtoms.sipMessageResponsePullTimestampMillis = getWallTimeMillis();
            SipMessageResponse[] previousStats =
                    mAtoms.sipMessageResponse;
            mAtoms.sipMessageResponse = new SipMessageResponse[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the SipTransportSession if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized SipTransportSession[] getSipTransportSession(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.sipTransportSessionPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.sipTransportSessionPullTimestampMillis = getWallTimeMillis();
            SipTransportSession[] previousStats =
                    mAtoms.sipTransportSession;
            mAtoms.sipTransportSession = new SipTransportSession[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the ImsDedicatedBearerListenerEvent if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized ImsDedicatedBearerListenerEvent[] getImsDedicatedBearerListenerEvent(
            long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.imsDedicatedBearerListenerEventPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.imsDedicatedBearerListenerEventPullTimestampMillis = getWallTimeMillis();
            ImsDedicatedBearerListenerEvent[] previousStats =
                mAtoms.imsDedicatedBearerListenerEvent;
            mAtoms.imsDedicatedBearerListenerEvent = new ImsDedicatedBearerListenerEvent[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the ImsDedicatedBearerEvent if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized ImsDedicatedBearerEvent[] getImsDedicatedBearerEvent(
            long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.imsDedicatedBearerEventPullTimestampMillis
                  > minIntervalMillis) {
            mAtoms.imsDedicatedBearerEventPullTimestampMillis = getWallTimeMillis();
            ImsDedicatedBearerEvent[] previousStats =
                mAtoms.imsDedicatedBearerEvent;
            mAtoms.imsDedicatedBearerEvent = new ImsDedicatedBearerEvent[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the ImsRegistrationServiceDescStats if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized ImsRegistrationServiceDescStats[] getImsRegistrationServiceDescStats(long
            minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.imsRegistrationServiceDescStatsPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.imsRegistrationServiceDescStatsPullTimestampMillis = getWallTimeMillis();
            ImsRegistrationServiceDescStats[] previousStats =
                mAtoms.imsRegistrationServiceDescStats;
            mAtoms.imsRegistrationServiceDescStats = new ImsRegistrationServiceDescStats[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the UceEventStats if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized UceEventStats[] getUceEventStats(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.uceEventStatsPullTimestampMillis > minIntervalMillis) {
            mAtoms.uceEventStatsPullTimestampMillis = getWallTimeMillis();
            UceEventStats[] previousStats = mAtoms.uceEventStats;
            mAtoms.uceEventStats = new UceEventStats[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the PresenceNotifyEvent if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized PresenceNotifyEvent[] getPresenceNotifyEvent(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.presenceNotifyEventPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.presenceNotifyEventPullTimestampMillis = getWallTimeMillis();
            PresenceNotifyEvent[] previousStats = mAtoms.presenceNotifyEvent;
            mAtoms.presenceNotifyEvent = new PresenceNotifyEvent[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /**
     * Returns and clears the GbaEvent if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized GbaEvent[] getGbaEvent(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.gbaEventPullTimestampMillis > minIntervalMillis) {
            mAtoms.gbaEventPullTimestampMillis = getWallTimeMillis();
            GbaEvent[] previousStats = mAtoms.gbaEvent;
            mAtoms.gbaEvent = new GbaEvent[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
        } else {
            return null;
        }
    }

    /** Saves a pending {@link PersistAtoms} to a file in private storage immediately. */
    public void flushAtoms() {
        if (mHandler.hasCallbacks(mSaveRunnable)) {
            mHandler.removeCallbacks(mSaveRunnable);
            saveAtomsToFileNow();
        }
    }

    /** Loads {@link PersistAtoms} from a file in private storage. */
    private PersistAtoms loadAtomsFromFile() {
        try {
            PersistAtoms atoms =
                    PersistAtoms.parseFrom(
                            Files.readAllBytes(mContext.getFileStreamPath(FILENAME).toPath()));
            // Start from scratch if build changes, since mixing atoms from different builds could
            // produce strange results
            if (!Build.FINGERPRINT.equals(atoms.buildFingerprint)) {
                Rlog.d(TAG, "Build changed");
                return makeNewPersistAtoms();
            }
            // check all the fields in case of situations such as OTA or crash during saving
            atoms.voiceCallRatUsage =
                    sanitizeAtoms(atoms.voiceCallRatUsage, VoiceCallRatUsage.class);
            atoms.voiceCallSession =
                    sanitizeAtoms(
                            atoms.voiceCallSession,
                            VoiceCallSession.class,
                            mMaxNumVoiceCallSessions);
            atoms.incomingSms = sanitizeAtoms(atoms.incomingSms, IncomingSms.class, mMaxNumSms);
            atoms.outgoingSms = sanitizeAtoms(atoms.outgoingSms, OutgoingSms.class, mMaxNumSms);
            atoms.carrierIdMismatch =
                    sanitizeAtoms(
                            atoms.carrierIdMismatch,
                            CarrierIdMismatch.class,
                            mMaxNumCarrierIdMismatches);
            atoms.dataCallSession =
                    sanitizeAtoms(
                            atoms.dataCallSession,
                            DataCallSession.class,
                            mMaxNumDataCallSessions);
            atoms.cellularServiceState =
                    sanitizeAtoms(
                            atoms.cellularServiceState,
                            CellularServiceState.class,
                            mMaxNumCellularServiceStates);
            atoms.cellularDataServiceSwitch =
                    sanitizeAtoms(
                            atoms.cellularDataServiceSwitch,
                            CellularDataServiceSwitch.class,
                            mMaxNumCellularDataSwitches);
            atoms.imsRegistrationStats =
                    sanitizeAtoms(
                            atoms.imsRegistrationStats,
                            ImsRegistrationStats.class,
                            mMaxNumImsRegistrationStats);
            atoms.imsRegistrationTermination =
                    sanitizeAtoms(
                            atoms.imsRegistrationTermination,
                            ImsRegistrationTermination.class,
                            mMaxNumImsRegistrationTerminations);
            atoms.networkRequestsV2 =
                    sanitizeAtoms(atoms.networkRequestsV2, NetworkRequestsV2.class);
            atoms.imsRegistrationFeatureTagStats =
                    sanitizeAtoms(
                            atoms.imsRegistrationFeatureTagStats,
                            ImsRegistrationFeatureTagStats.class,
                            mMaxNumImsRegistrationFeatureStats);
            atoms.rcsClientProvisioningStats =
                    sanitizeAtoms(
                            atoms.rcsClientProvisioningStats,
                            RcsClientProvisioningStats.class,
                            mMaxNumRcsClientProvisioningStats);
            atoms.rcsAcsProvisioningStats =
                    sanitizeAtoms(
                            atoms.rcsAcsProvisioningStats,
                            RcsAcsProvisioningStats.class,
                            mMaxNumRcsAcsProvisioningStats);
            atoms.sipDelegateStats =
                    sanitizeAtoms(
                            atoms.sipDelegateStats,
                            SipDelegateStats.class,
                            mMaxNumSipDelegateStats);
            atoms.sipTransportFeatureTagStats =
                    sanitizeAtoms(
                            atoms.sipTransportFeatureTagStats,
                            SipTransportFeatureTagStats.class,
                            mMaxNumSipTransportFeatureTagStats);
            atoms.sipMessageResponse =
                    sanitizeAtoms(
                            atoms.sipMessageResponse,
                            SipMessageResponse.class,
                            mMaxNumSipMessageResponseStats);
            atoms.sipTransportSession =
                    sanitizeAtoms(
                            atoms.sipTransportSession,
                            SipTransportSession.class,
                            mMaxNumSipTransportSessionStats);
            atoms.imsDedicatedBearerListenerEvent =
                    sanitizeAtoms(
                            atoms.imsDedicatedBearerListenerEvent,
                            ImsDedicatedBearerListenerEvent.class,
                            mMaxNumDedicatedBearerListenerEventStats);
            atoms.imsDedicatedBearerEvent =
                    sanitizeAtoms(
                            atoms.imsDedicatedBearerEvent,
                            ImsDedicatedBearerEvent.class,
                            mMaxNumDedicatedBearerEventStats);
            atoms.imsRegistrationServiceDescStats =
                    sanitizeAtoms(
                            atoms.imsRegistrationServiceDescStats,
                            ImsRegistrationServiceDescStats.class,
                            mMaxNumImsRegistrationServiceDescStats);
            atoms.uceEventStats =
                    sanitizeAtoms(
                            atoms.uceEventStats,
                            UceEventStats.class,
                            mMaxNumUceEventStats);
            atoms.presenceNotifyEvent =
                    sanitizeAtoms(
                            atoms.presenceNotifyEvent,
                            PresenceNotifyEvent.class,
                            mMaxNumPresenceNotifyEventStats);
            atoms.gbaEvent =
                    sanitizeAtoms(
                            atoms.gbaEvent,
                            GbaEvent.class,
                            mMaxNumGbaEventStats);

            // out of caution, sanitize also the timestamps
            atoms.voiceCallRatUsagePullTimestampMillis =
                    sanitizeTimestamp(atoms.voiceCallRatUsagePullTimestampMillis);
            atoms.voiceCallSessionPullTimestampMillis =
                    sanitizeTimestamp(atoms.voiceCallSessionPullTimestampMillis);
            atoms.incomingSmsPullTimestampMillis =
                    sanitizeTimestamp(atoms.incomingSmsPullTimestampMillis);
            atoms.outgoingSmsPullTimestampMillis =
                    sanitizeTimestamp(atoms.outgoingSmsPullTimestampMillis);
            atoms.dataCallSessionPullTimestampMillis =
                    sanitizeTimestamp(atoms.dataCallSessionPullTimestampMillis);
            atoms.cellularServiceStatePullTimestampMillis =
                    sanitizeTimestamp(atoms.cellularServiceStatePullTimestampMillis);
            atoms.cellularDataServiceSwitchPullTimestampMillis =
                    sanitizeTimestamp(atoms.cellularDataServiceSwitchPullTimestampMillis);
            atoms.imsRegistrationStatsPullTimestampMillis =
                    sanitizeTimestamp(atoms.imsRegistrationStatsPullTimestampMillis);
            atoms.imsRegistrationTerminationPullTimestampMillis =
                    sanitizeTimestamp(atoms.imsRegistrationTerminationPullTimestampMillis);
            atoms.networkRequestsV2PullTimestampMillis =
                    sanitizeTimestamp(atoms.networkRequestsV2PullTimestampMillis);
            atoms.imsRegistrationFeatureTagStatsPullTimestampMillis =
                    sanitizeTimestamp(atoms.imsRegistrationFeatureTagStatsPullTimestampMillis);
            atoms.rcsClientProvisioningStatsPullTimestampMillis =
                    sanitizeTimestamp(atoms.rcsClientProvisioningStatsPullTimestampMillis);
            atoms.rcsAcsProvisioningStatsPullTimestampMillis =
                    sanitizeTimestamp(atoms.rcsAcsProvisioningStatsPullTimestampMillis);
            atoms.sipDelegateStatsPullTimestampMillis =
                    sanitizeTimestamp(atoms.sipDelegateStatsPullTimestampMillis);
            atoms.sipTransportFeatureTagStatsPullTimestampMillis =
                    sanitizeTimestamp(atoms.sipTransportFeatureTagStatsPullTimestampMillis);
            atoms.sipMessageResponsePullTimestampMillis =
                    sanitizeTimestamp(atoms.sipMessageResponsePullTimestampMillis);
            atoms.sipTransportSessionPullTimestampMillis =
                    sanitizeTimestamp(atoms.sipTransportSessionPullTimestampMillis);
            atoms.imsDedicatedBearerListenerEventPullTimestampMillis =
                    sanitizeTimestamp(atoms.imsDedicatedBearerListenerEventPullTimestampMillis);
            atoms.imsDedicatedBearerEventPullTimestampMillis =
                    sanitizeTimestamp(atoms.imsDedicatedBearerEventPullTimestampMillis);
            atoms.imsRegistrationServiceDescStatsPullTimestampMillis =
                    sanitizeTimestamp(atoms.imsRegistrationServiceDescStatsPullTimestampMillis);
            atoms.uceEventStatsPullTimestampMillis =
                    sanitizeTimestamp(atoms.uceEventStatsPullTimestampMillis);
            atoms.presenceNotifyEventPullTimestampMillis =
                    sanitizeTimestamp(atoms.presenceNotifyEventPullTimestampMillis);
            atoms.gbaEventPullTimestampMillis =
                    sanitizeTimestamp(atoms.gbaEventPullTimestampMillis);

            return atoms;
        } catch (NoSuchFileException e) {
            Rlog.d(TAG, "PersistAtoms file not found");
        } catch (IOException | NullPointerException e) {
            Rlog.e(TAG, "cannot load/parse PersistAtoms", e);
        }
        return makeNewPersistAtoms();
    }

    /**
     * Posts message to save a copy of {@link PersistAtoms} to a file after a delay.
     *
     * <p>The delay is introduced to avoid too frequent operations to disk, which would negatively
     * impact the power consumption.
     */
    private void saveAtomsToFile(int delayMillis) {
        if (delayMillis > 0 && !mSaveImmediately) {
            mHandler.removeCallbacks(mSaveRunnable);
            if (mHandler.postDelayed(mSaveRunnable, delayMillis)) {
                return;
            }
        }
        // In case of error posting the event or if delay is 0, save immediately
        saveAtomsToFileNow();
    }

    /** Saves a copy of {@link PersistAtoms} to a file in private storage. */
    private synchronized void saveAtomsToFileNow() {
        try (FileOutputStream stream = mContext.openFileOutput(FILENAME, Context.MODE_PRIVATE)) {
            stream.write(PersistAtoms.toByteArray(mAtoms));
        } catch (IOException e) {
            Rlog.e(TAG, "cannot save PersistAtoms", e);
        }
    }

    /**
     * Returns the service state that has the same dimension values with the given one, or {@code
     * null} if it does not exist.
     */
    private @Nullable CellularServiceState find(CellularServiceState key) {
        for (CellularServiceState state : mAtoms.cellularServiceState) {
            if (state.voiceRat == key.voiceRat
                    && state.dataRat == key.dataRat
                    && state.voiceRoamingType == key.voiceRoamingType
                    && state.dataRoamingType == key.dataRoamingType
                    && state.isEndc == key.isEndc
                    && state.simSlotIndex == key.simSlotIndex
                    && state.isMultiSim == key.isMultiSim
                    && state.carrierId == key.carrierId
                    && state.isEmergencyOnly == key.isEmergencyOnly) {
                return state;
            }
        }
        return null;
    }

    /**
     * Returns the data service switch that has the same dimension values with the given one, or
     * {@code null} if it does not exist.
     */
    private @Nullable CellularDataServiceSwitch find(CellularDataServiceSwitch key) {
        for (CellularDataServiceSwitch serviceSwitch : mAtoms.cellularDataServiceSwitch) {
            if (serviceSwitch.ratFrom == key.ratFrom
                    && serviceSwitch.ratTo == key.ratTo
                    && serviceSwitch.simSlotIndex == key.simSlotIndex
                    && serviceSwitch.isMultiSim == key.isMultiSim
                    && serviceSwitch.carrierId == key.carrierId) {
                return serviceSwitch;
            }
        }
        return null;
    }

    /**
     * Returns the carrier ID mismatch event that has the same dimension values with the given one,
     * or {@code null} if it does not exist.
     */
    private @Nullable CarrierIdMismatch find(CarrierIdMismatch key) {
        for (CarrierIdMismatch mismatch : mAtoms.carrierIdMismatch) {
            if (mismatch.mccMnc.equals(key.mccMnc)
                    && mismatch.gid1.equals(key.gid1)
                    && mismatch.spn.equals(key.spn)
                    && mismatch.pnn.equals(key.pnn)) {
                return mismatch;
            }
        }
        return null;
    }

    /**
     * Returns the IMS registration stats that has the same dimension values with the given one, or
     * {@code null} if it does not exist.
     */
    private @Nullable ImsRegistrationStats find(ImsRegistrationStats key) {
        for (ImsRegistrationStats stats : mAtoms.imsRegistrationStats) {
            if (stats.carrierId == key.carrierId
                    && stats.simSlotIndex == key.simSlotIndex
                    && stats.rat == key.rat) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns the IMS registration termination that has the same dimension values with the given
     * one, or {@code null} if it does not exist.
     */
    private @Nullable ImsRegistrationTermination find(ImsRegistrationTermination key) {
        for (ImsRegistrationTermination termination : mAtoms.imsRegistrationTermination) {
            if (termination.carrierId == key.carrierId
                    && termination.isMultiSim == key.isMultiSim
                    && termination.ratAtEnd == key.ratAtEnd
                    && termination.setupFailed == key.setupFailed
                    && termination.reasonCode == key.reasonCode
                    && termination.extraCode == key.extraCode
                    && termination.extraMessage.equals(key.extraMessage)) {
                return termination;
            }
        }
        return null;
    }

    /**
     * Returns the network requests event that has the same carrier id and capability as the given
     * one, or {@code null} if it does not exist.
     */
    private @Nullable NetworkRequestsV2 find(NetworkRequestsV2 key) {
        for (NetworkRequestsV2 item : mAtoms.networkRequestsV2) {
            if (item.carrierId == key.carrierId && item.capability == key.capability) {
                return item;
            }
        }
        return null;
    }

    /**
     * Returns the index of data call session that has the same random dimension as the given one,
     * or -1 if it does not exist.
     */
    private int findIndex(DataCallSession key) {
        for (int i = 0; i < mAtoms.dataCallSession.length; i++) {
            if (mAtoms.dataCallSession[i].dimension == key.dimension) {
                return i;
            }
        }
        return -1;
    }
    /**
     * Returns the Dedicated Bearer Listener event that has the same carrier id, slot id, rat, qci
     * and established state as the given one, or {@code null} if it does not exist.
     */
    private @Nullable ImsDedicatedBearerListenerEvent find(ImsDedicatedBearerListenerEvent key) {
        for (ImsDedicatedBearerListenerEvent stats : mAtoms.imsDedicatedBearerListenerEvent) {
            if (stats.carrierId == key.carrierId
                    && stats.slotId == key.slotId
                    && stats.ratAtEnd == key.ratAtEnd
                    && stats.qci == key.qci
                    && stats.dedicatedBearerEstablished == key.dedicatedBearerEstablished) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns the Dedicated Bearer event that has the same carrier id, slot id, rat,
     * qci, bearer state, local/remote connection and exsting listener as the given one,
     * or {@code null} if it does not exist.
     */
    private @Nullable ImsDedicatedBearerEvent find(ImsDedicatedBearerEvent key) {
        for (ImsDedicatedBearerEvent stats : mAtoms.imsDedicatedBearerEvent) {
            if (stats.carrierId == key.carrierId
                    && stats.slotId == key.slotId
                    && stats.ratAtEnd == key.ratAtEnd
                    && stats.qci == key.qci
                    && stats.bearerState == key.bearerState
                    && stats.localConnectionInfoReceived == key.localConnectionInfoReceived
                    && stats.remoteConnectionInfoReceived == key.remoteConnectionInfoReceived
                    && stats.hasListeners == key.hasListeners) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns the Registration Feature Tag that has the same carrier id, slot id,
     * feature tag name or custom feature tag name and registration tech as the given one,
     * or {@code null} if it does not exist.
     */
    private @Nullable ImsRegistrationFeatureTagStats find(ImsRegistrationFeatureTagStats key) {
        for (ImsRegistrationFeatureTagStats stats : mAtoms.imsRegistrationFeatureTagStats) {
            if (stats.carrierId == key.carrierId
                    && stats.slotId == key.slotId
                    && stats.featureTagName == key.featureTagName
                    && stats.registrationTech == key.registrationTech) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns Client Provisioning that has the same carrier id, slot id and event as the given
     * one, or {@code null} if it does not exist.
     */
    private @Nullable RcsClientProvisioningStats find(RcsClientProvisioningStats key) {
        for (RcsClientProvisioningStats stats : mAtoms.rcsClientProvisioningStats) {
            if (stats.carrierId == key.carrierId
                    && stats.slotId == key.slotId
                    && stats.event == key.event) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns ACS Provisioning that has the same carrier id, slot id, response code, response type
     * and SR supported as the given one, or {@code null} if it does not exist.
     */
    private @Nullable RcsAcsProvisioningStats find(RcsAcsProvisioningStats key) {
        for (RcsAcsProvisioningStats stats : mAtoms.rcsAcsProvisioningStats) {
            if (stats.carrierId == key.carrierId
                    && stats.slotId == key.slotId
                    && stats.responseCode == key.responseCode
                    && stats.responseType == key.responseType
                    && stats.isSingleRegistrationEnabled == key.isSingleRegistrationEnabled) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns Sip Message Response that has the same carrier id, slot id, method, response,
     * direction and error as the given one, or {@code null} if it does not exist.
     */
    private @Nullable SipMessageResponse find(SipMessageResponse key) {
        for (SipMessageResponse stats : mAtoms.sipMessageResponse) {
            if (stats.carrierId == key.carrierId
                    && stats.slotId == key.slotId
                    && stats.sipMessageMethod == key.sipMessageMethod
                    && stats.sipMessageResponse == key.sipMessageResponse
                    && stats.sipMessageDirection == key.sipMessageDirection
                    && stats.messageError == key.messageError) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns Sip Transport Session that has the same carrier id, slot id, method, direction and
     * response as the given one, or {@code null} if it does not exist.
     */
    private @Nullable SipTransportSession find(SipTransportSession key) {
        for (SipTransportSession stats : mAtoms.sipTransportSession) {
            if (stats.carrierId == key.carrierId
                    && stats.slotId == key.slotId
                    && stats.sessionMethod == key.sessionMethod
                    && stats.sipMessageDirection == key.sipMessageDirection
                    && stats.sipResponse == key.sipResponse) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns Registration Service Desc Stats that has the same carrier id, slot id, service id or
     * custom service id, service id version and registration tech as the given one,
     * or {@code null} if it does not exist.
     */
    private @Nullable ImsRegistrationServiceDescStats find(ImsRegistrationServiceDescStats key) {
        for (ImsRegistrationServiceDescStats stats : mAtoms.imsRegistrationServiceDescStats) {
            if (stats.carrierId == key.carrierId
                    && stats.slotId == key.slotId
                    && stats.serviceIdName == key.serviceIdName
                    && stats.serviceIdVersion == key.serviceIdVersion
                    && stats.registrationTech == key.registrationTech) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns UCE Event Stats that has the same carrier id, slot id, event result, command code and
     * network response as the given one, or {@code null} if it does not exist.
     */
    private @Nullable UceEventStats find(UceEventStats key) {
        for (UceEventStats stats : mAtoms.uceEventStats) {
            if (stats.carrierId == key.carrierId
                    && stats.slotId == key.slotId
                    && stats.type == key.type
                    && stats.successful == key.successful
                    && stats.commandCode == key.commandCode
                    && stats.networkResponse == key.networkResponse) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns Presence Notify Event that has the same carrier id, slot id, reason and body in
     * response as the given one, or {@code null} if it does not exist.
     */
    private @Nullable PresenceNotifyEvent find(PresenceNotifyEvent key) {
        for (PresenceNotifyEvent stats : mAtoms.presenceNotifyEvent) {
            if (stats.carrierId == key.carrierId
                    && stats.slotId == key.slotId
                    && stats.reason == key.reason
                    && stats.contentBodyReceived == key.contentBodyReceived) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns GBA Event that has the same carrier id, slot id, result of operation and fail reason
     * as the given one, or {@code null} if it does not exist.
     */
    private @Nullable GbaEvent find(GbaEvent key) {
        for (GbaEvent stats : mAtoms.gbaEvent) {
            if (stats.carrierId == key.carrierId
                    && stats.slotId == key.slotId
                    && stats.successful == key.successful
                    && stats.failedReason == key.failedReason) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Returns Sip Transport Feature Tag Stats that has the same carrier id, slot id, feature tag
     * name, deregister reason, denied reason and feature tag name or custom feature tag name as
     * the given one, or {@code null} if it does not exist.
     */
    private @Nullable SipTransportFeatureTagStats find(SipTransportFeatureTagStats key) {
        for (SipTransportFeatureTagStats stat: mAtoms.sipTransportFeatureTagStats) {
            if (stat.carrierId == key.carrierId
                    && stat.slotId == key.slotId
                    && stat.featureTagName == key.featureTagName
                    && stat.sipTransportDeregisteredReason == key.sipTransportDeregisteredReason
                    && stat.sipTransportDeniedReason == key.sipTransportDeniedReason) {
                return stat;
            }
        }
        return null;
    }

    /**
     * Inserts a new element in a random position in an array with a maximum size, replacing the
     * least recent item if possible.
     */
    private static <T> T[] insertAtRandomPlace(T[] storage, T instance, int maxLength) {
        final int newLength = storage.length + 1;
        final boolean arrayFull = (newLength > maxLength);
        T[] result = Arrays.copyOf(storage, arrayFull ? maxLength : newLength);
        if (newLength == 1) {
            result[0] = instance;
        } else if (arrayFull) {
            result[findItemToEvict(storage)] = instance;
        } else {
            // insert at random place (by moving the item at the random place to the end)
            int insertAt = sRandom.nextInt(newLength);
            result[newLength - 1] = result[insertAt];
            result[insertAt] = instance;
        }
        return result;
    }

    /** Returns index of the item suitable for eviction when the array is full. */
    private static <T> int findItemToEvict(T[] array) {
        if (array instanceof CellularServiceState[]) {
            // Evict the item that was used least recently
            CellularServiceState[] arr = (CellularServiceState[]) array;
            return IntStream.range(0, arr.length)
                    .reduce((i, j) -> arr[i].lastUsedMillis < arr[j].lastUsedMillis ? i : j)
                    .getAsInt();
        }

        if (array instanceof CellularDataServiceSwitch[]) {
            // Evict the item that was used least recently
            CellularDataServiceSwitch[] arr = (CellularDataServiceSwitch[]) array;
            return IntStream.range(0, arr.length)
                    .reduce((i, j) -> arr[i].lastUsedMillis < arr[j].lastUsedMillis ? i : j)
                    .getAsInt();
        }

        if (array instanceof ImsRegistrationStats[]) {
            // Evict the item that was used least recently
            ImsRegistrationStats[] arr = (ImsRegistrationStats[]) array;
            return IntStream.range(0, arr.length)
                    .reduce((i, j) -> arr[i].lastUsedMillis < arr[j].lastUsedMillis ? i : j)
                    .getAsInt();
        }

        if (array instanceof ImsRegistrationTermination[]) {
            // Evict the item that was used least recently
            ImsRegistrationTermination[] arr = (ImsRegistrationTermination[]) array;
            return IntStream.range(0, arr.length)
                    .reduce((i, j) -> arr[i].lastUsedMillis < arr[j].lastUsedMillis ? i : j)
                    .getAsInt();
        }

        if (array instanceof VoiceCallSession[]) {
            // For voice calls, try to keep emergency calls over regular calls.
            VoiceCallSession[] arr = (VoiceCallSession[]) array;
            int[] nonEmergencyCallIndexes = IntStream.range(0, arr.length)
                    .filter(i -> !arr[i].isEmergency)
                    .toArray();
            if (nonEmergencyCallIndexes.length > 0) {
                return nonEmergencyCallIndexes[sRandom.nextInt(nonEmergencyCallIndexes.length)];
            }
            // If all calls in the storage are emergency calls, proceed with default case
            // even if the new call is not an emergency call.
        }

        return sRandom.nextInt(array.length);
    }

    /** Sanitizes the loaded array of atoms to avoid null values. */
    private <T> T[] sanitizeAtoms(T[] array, Class<T> cl) {
        return ArrayUtils.emptyIfNull(array, cl);
    }

    /** Sanitizes the loaded array of atoms loaded to avoid null values and enforce max length. */
    private <T> T[] sanitizeAtoms(T[] array, Class<T> cl, int maxLength) {
        array = sanitizeAtoms(array, cl);
        if (array.length > maxLength) {
            return Arrays.copyOf(array, maxLength);
        }
        return array;
    }

    /** Sanitizes the timestamp of the last pull loaded from persistent storage. */
    private long sanitizeTimestamp(long timestamp) {
        return timestamp <= 0L ? getWallTimeMillis() : timestamp;
    }

    /**
     * Returns {@link ImsRegistrationStats} array with durations normalized to 24 hours
     * depending on the interval.
     */
    private ImsRegistrationStats[] normalizeData(ImsRegistrationStats[] stats,
            long intervalMillis) {
        for (int i = 0; i < stats.length; i++) {
            stats[i].registeredMillis =
                    normalizeDurationTo24H(stats[i].registeredMillis, intervalMillis);
            stats[i].voiceCapableMillis =
                    normalizeDurationTo24H(stats[i].voiceCapableMillis, intervalMillis);
            stats[i].voiceAvailableMillis =
                    normalizeDurationTo24H(stats[i].voiceAvailableMillis, intervalMillis);
            stats[i].smsCapableMillis =
                    normalizeDurationTo24H(stats[i].smsCapableMillis, intervalMillis);
            stats[i].smsAvailableMillis =
                    normalizeDurationTo24H(stats[i].smsAvailableMillis, intervalMillis);
            stats[i].videoCapableMillis =
                    normalizeDurationTo24H(stats[i].videoCapableMillis, intervalMillis);
            stats[i].videoAvailableMillis =
                    normalizeDurationTo24H(stats[i].videoAvailableMillis, intervalMillis);
            stats[i].utCapableMillis =
                    normalizeDurationTo24H(stats[i].utCapableMillis, intervalMillis);
            stats[i].utAvailableMillis =
                    normalizeDurationTo24H(stats[i].utAvailableMillis, intervalMillis);
        }
        return stats;
    }

    /** Returns a duration normalized to 24 hours. */
    private long normalizeDurationTo24H(long timeInMillis, long intervalMillis) {
        long interval = intervalMillis < 1000 ? 1 : intervalMillis / 1000;
        return ((timeInMillis / 1000) * (DAY_IN_MILLIS / 1000) / interval) * 1000;
    }

    /** Returns an empty PersistAtoms with pull timestamp set to current time. */
    private PersistAtoms makeNewPersistAtoms() {
        PersistAtoms atoms = new PersistAtoms();
        // allow pulling only after some time so data are sufficiently aggregated
        long currentTime = getWallTimeMillis();
        atoms.buildFingerprint = Build.FINGERPRINT;
        atoms.voiceCallRatUsagePullTimestampMillis = currentTime;
        atoms.voiceCallSessionPullTimestampMillis = currentTime;
        atoms.incomingSmsPullTimestampMillis = currentTime;
        atoms.outgoingSmsPullTimestampMillis = currentTime;
        atoms.carrierIdTableVersion = TelephonyManager.UNKNOWN_CARRIER_ID_LIST_VERSION;
        atoms.dataCallSessionPullTimestampMillis = currentTime;
        atoms.cellularServiceStatePullTimestampMillis = currentTime;
        atoms.cellularDataServiceSwitchPullTimestampMillis = currentTime;
        atoms.imsRegistrationStatsPullTimestampMillis = currentTime;
        atoms.imsRegistrationTerminationPullTimestampMillis = currentTime;
        atoms.networkRequestsPullTimestampMillis = currentTime;
        atoms.networkRequestsV2PullTimestampMillis = currentTime;
        atoms.imsRegistrationFeatureTagStatsPullTimestampMillis = currentTime;
        atoms.rcsClientProvisioningStatsPullTimestampMillis = currentTime;
        atoms.rcsAcsProvisioningStatsPullTimestampMillis = currentTime;
        atoms.sipDelegateStatsPullTimestampMillis = currentTime;
        atoms.sipTransportFeatureTagStatsPullTimestampMillis = currentTime;
        atoms.sipMessageResponsePullTimestampMillis = currentTime;
        atoms.sipTransportSessionPullTimestampMillis = currentTime;
        atoms.imsDedicatedBearerListenerEventPullTimestampMillis = currentTime;
        atoms.imsDedicatedBearerEventPullTimestampMillis = currentTime;
        atoms.imsRegistrationServiceDescStatsPullTimestampMillis = currentTime;
        atoms.uceEventStatsPullTimestampMillis = currentTime;
        atoms.presenceNotifyEventPullTimestampMillis = currentTime;
        atoms.gbaEventPullTimestampMillis = currentTime;

        Rlog.d(TAG, "created new PersistAtoms");
        return atoms;
    }

    @VisibleForTesting
    protected long getWallTimeMillis() {
        // Epoch time in UTC, preserved across reboots, but can be adjusted e.g. by the user or NTP
        return System.currentTimeMillis();
    }
}

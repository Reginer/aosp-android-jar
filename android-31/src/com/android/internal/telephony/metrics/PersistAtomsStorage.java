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

import android.annotation.Nullable;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.telephony.TelephonyManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.nano.PersistAtomsProto.CarrierIdMismatch;
import com.android.internal.telephony.nano.PersistAtomsProto.CellularDataServiceSwitch;
import com.android.internal.telephony.nano.PersistAtomsProto.CellularServiceState;
import com.android.internal.telephony.nano.PersistAtomsProto.DataCallSession;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationStats;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationTermination;
import com.android.internal.telephony.nano.PersistAtomsProto.IncomingSms;
import com.android.internal.telephony.nano.PersistAtomsProto.NetworkRequests;
import com.android.internal.telephony.nano.PersistAtomsProto.OutgoingSms;
import com.android.internal.telephony.nano.PersistAtomsProto.PersistAtoms;
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
    private static final int MAX_NUM_CALL_SESSIONS = 50;

    /**
     * Maximum number of SMS to store between pulls. Incoming messages and outgoing messages are
     * counted separately.
     */
    private static final int MAX_NUM_SMS = 25;

    /**
     * Maximum number of carrier ID mismatch events stored on the device to avoid sending duplicated
     * metrics.
     */
    private static final int MAX_CARRIER_ID_MISMATCH = 40;

    /** Maximum number of data call sessions to store during pulls. */
    private static final int MAX_NUM_DATA_CALL_SESSIONS = 15;

    /** Maximum number of service states to store between pulls. */
    private static final int MAX_NUM_CELLULAR_SERVICE_STATES = 50;

    /** Maximum number of data service switches to store between pulls. */
    private static final int MAX_NUM_CELLULAR_DATA_SERVICE_SWITCHES = 50;

    /** Maximum number of IMS registration stats to store between pulls. */
    private static final int MAX_NUM_IMS_REGISTRATION_STATS = 10;

    /** Maximum number of IMS registration terminations to store between pulls. */
    private static final int MAX_NUM_IMS_REGISTRATION_TERMINATIONS = 10;

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
                insertAtRandomPlace(mAtoms.voiceCallSession, call, MAX_NUM_CALL_SESSIONS);
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
        mAtoms.incomingSms = insertAtRandomPlace(mAtoms.incomingSms, sms, MAX_NUM_SMS);
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

        mAtoms.outgoingSms = insertAtRandomPlace(mAtoms.outgoingSms, sms, MAX_NUM_SMS);
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
                            mAtoms.cellularServiceState, state, MAX_NUM_CELLULAR_SERVICE_STATES);
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
                                MAX_NUM_CELLULAR_DATA_SERVICE_SWITCHES);
            }
        }

        saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_UPDATE_MILLIS);
    }

    /** Adds a data call session to the storage. */
    public synchronized void addDataCallSession(DataCallSession dataCall) {
        mAtoms.dataCallSession =
                insertAtRandomPlace(mAtoms.dataCallSession, dataCall, MAX_NUM_DATA_CALL_SESSIONS);
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
        if (mAtoms.carrierIdMismatch.length == MAX_CARRIER_ID_MISMATCH) {
            System.arraycopy(
                    mAtoms.carrierIdMismatch,
                    1,
                    mAtoms.carrierIdMismatch,
                    0,
                    MAX_CARRIER_ID_MISMATCH - 1);
            mAtoms.carrierIdMismatch[MAX_CARRIER_ID_MISMATCH - 1] = carrierIdMismatch;
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
                            mAtoms.imsRegistrationStats, stats, MAX_NUM_IMS_REGISTRATION_STATS);
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
                            MAX_NUM_IMS_REGISTRATION_TERMINATIONS);
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

    /** Adds a new {@link NetworkRequests} to the storage. */
    public synchronized void addNetworkRequests(NetworkRequests networkRequests) {
        NetworkRequests existingMetrics = find(networkRequests);
        if (existingMetrics != null) {
            existingMetrics.enterpriseRequestCount += networkRequests.enterpriseRequestCount;
            existingMetrics.enterpriseReleaseCount += networkRequests.enterpriseReleaseCount;
        } else {
            int newLength = mAtoms.networkRequests.length + 1;
            mAtoms.networkRequests = Arrays.copyOf(mAtoms.networkRequests, newLength);
            mAtoms.networkRequests[newLength - 1] = networkRequests;
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
     * Returns and clears the IMS registration statistics if last pulled longer than {@code
     * minIntervalMillis} ago, otherwise returns {@code null}.
     */
    @Nullable
    public synchronized ImsRegistrationStats[] getImsRegistrationStats(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.imsRegistrationStatsPullTimestampMillis
                > minIntervalMillis) {
            mAtoms.imsRegistrationStatsPullTimestampMillis = getWallTimeMillis();
            ImsRegistrationStats[] previousStats = mAtoms.imsRegistrationStats;
            Arrays.stream(previousStats).forEach(stats -> stats.lastUsedMillis = 0L);
            mAtoms.imsRegistrationStats = new ImsRegistrationStats[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousStats;
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
    public synchronized NetworkRequests[] getNetworkRequests(long minIntervalMillis) {
        if (getWallTimeMillis() - mAtoms.networkRequestsPullTimestampMillis > minIntervalMillis) {
            mAtoms.networkRequestsPullTimestampMillis = getWallTimeMillis();
            NetworkRequests[] previousNetworkRequests = mAtoms.networkRequests;
            mAtoms.networkRequests = new NetworkRequests[0];
            saveAtomsToFile(SAVE_TO_FILE_DELAY_FOR_GET_MILLIS);
            return previousNetworkRequests;
        } else {
            return null;
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
                            atoms.voiceCallSession, VoiceCallSession.class, MAX_NUM_CALL_SESSIONS);
            atoms.incomingSms = sanitizeAtoms(atoms.incomingSms, IncomingSms.class, MAX_NUM_SMS);
            atoms.outgoingSms = sanitizeAtoms(atoms.outgoingSms, OutgoingSms.class, MAX_NUM_SMS);
            atoms.carrierIdMismatch =
                    sanitizeAtoms(
                            atoms.carrierIdMismatch,
                            CarrierIdMismatch.class,
                            MAX_CARRIER_ID_MISMATCH);
            atoms.dataCallSession =
                    sanitizeAtoms(
                            atoms.dataCallSession,
                            DataCallSession.class,
                            MAX_NUM_DATA_CALL_SESSIONS);
            atoms.cellularServiceState =
                    sanitizeAtoms(
                            atoms.cellularServiceState,
                            CellularServiceState.class,
                            MAX_NUM_CELLULAR_SERVICE_STATES);
            atoms.cellularDataServiceSwitch =
                    sanitizeAtoms(
                            atoms.cellularDataServiceSwitch,
                            CellularDataServiceSwitch.class,
                            MAX_NUM_CELLULAR_DATA_SERVICE_SWITCHES);
            atoms.imsRegistrationStats =
                    sanitizeAtoms(
                            atoms.imsRegistrationStats,
                            ImsRegistrationStats.class,
                            MAX_NUM_IMS_REGISTRATION_STATS);
            atoms.imsRegistrationTermination =
                    sanitizeAtoms(
                            atoms.imsRegistrationTermination,
                            ImsRegistrationTermination.class,
                            MAX_NUM_IMS_REGISTRATION_TERMINATIONS);
            atoms.networkRequests = sanitizeAtoms(atoms.networkRequests, NetworkRequests.class);
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
            atoms.networkRequestsPullTimestampMillis =
                    sanitizeTimestamp(atoms.networkRequestsPullTimestampMillis);
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
                    && state.carrierId == key.carrierId) {
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
     * Returns the network requests event that has the same carrier id as the given one,
     * or {@code null} if it does not exist.
     */
    private @Nullable NetworkRequests find(NetworkRequests key) {
        for (NetworkRequests item : mAtoms.networkRequests) {
            if (item.carrierId == key.carrierId) {
                return item;
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
            CellularServiceState[] arr = (CellularServiceState[]) array;
            return IntStream.range(0, arr.length)
                    .reduce((i, j) -> arr[i].lastUsedMillis < arr[j].lastUsedMillis ? i : j)
                    .getAsInt();
        }

        if (array instanceof CellularDataServiceSwitch[]) {
            CellularDataServiceSwitch[] arr = (CellularDataServiceSwitch[]) array;
            return IntStream.range(0, arr.length)
                    .reduce((i, j) -> arr[i].lastUsedMillis < arr[j].lastUsedMillis ? i : j)
                    .getAsInt();
        }

        if (array instanceof ImsRegistrationStats[]) {
            ImsRegistrationStats[] arr = (ImsRegistrationStats[]) array;
            return IntStream.range(0, arr.length)
                    .reduce((i, j) -> arr[i].lastUsedMillis < arr[j].lastUsedMillis ? i : j)
                    .getAsInt();
        }

        if (array instanceof ImsRegistrationTermination[]) {
            ImsRegistrationTermination[] arr = (ImsRegistrationTermination[]) array;
            return IntStream.range(0, arr.length)
                    .reduce((i, j) -> arr[i].lastUsedMillis < arr[j].lastUsedMillis ? i : j)
                    .getAsInt();
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
        Rlog.d(TAG, "created new PersistAtoms");
        return atoms;
    }

    @VisibleForTesting
    protected long getWallTimeMillis() {
        // Epoch time in UTC, preserved across reboots, but can be adjusted e.g. by the user or NTP
        return System.currentTimeMillis();
    }
}

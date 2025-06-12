/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.internal.telephony.euicc;

import android.annotation.Nullable;
import android.util.ArraySet;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.flags.Flags;
import com.android.internal.telephony.uicc.euicc.apdu.ApduSender;
import com.android.telephony.Rlog;

import java.util.Set;

/**
 * A eUICC transaction session aims to optimize multiple back-to-back EuiccPort API calls by only
 * open and close a logical channel once.
 *
 * <p>This class is thread-safe.
 */
public class EuiccSession {
    private static final String TAG = "EuiccSession";

    // **** Well known session IDs, see #startSession() ****
    public static final String DOWNLOAD = "DOWNLOAD";

    @GuardedBy("EuiccSession.class")
    private static EuiccSession sInstance;

    public static synchronized EuiccSession get() {
        if (sInstance == null) {
            sInstance = new EuiccSession();
        }
        return sInstance;
    }

    @GuardedBy("this")
    private final Set<String> mSessions = new ArraySet<>();

    @GuardedBy("this")
    private final Set<ApduSender> mApduSenders = new ArraySet<>();

    /**
     * Marks the start of a eUICC transaction session.
     *
     * <p>A session means a long-open logical channel (see {@link ApduSender}) used to
     * send multiple APDUs for one action e.g. {@link EuiccController#downloadSubscription()}.
     * Those APDUs can be send by one or multiple {@link EuiccCardController} methods.
     *
     * <p>Ideally a session should correespond to one phoneId and hence just one logical channel.
     * But many {@link EuiccCardController} methods uses first available port and is not specific
     * to a phoneId. So EuiccController cannot choose one phoneId to use. Hence a session has to
     * be not specific to phoneId, i.e. for DSDS device both phoneId's will be in a session.
     *
     * <p>If called multiple times with different {@code sessionId}'s, the session is truly closed
     * when the all sessions are ended. See {@link #endSession()}.
     *
     * @param sessionId The session ID.
     */
    public void startSession(String sessionId) {
        if (!Flags.optimizationApduSender()) {
            // Other methods in this class is no-op if no session started.
            // Do not add flag to other methods, so if the flag gets turned off,
            // the session can be ended properly.
            return;
        }
        Rlog.i(TAG, "startSession: " + sessionId);
        synchronized(this) {
            mSessions.add(sessionId);
        }
    }

    /** Returns {@code true} if there is at least one session ongoing. */
    public boolean hasSession() {
        boolean hasSession = hasSessionInternal();
        Rlog.i(TAG, "hasSession: " + hasSession);
        return hasSession;
    }

    // The bare metal implementation of hasSession() without logging.
    private boolean hasSessionInternal() {
        synchronized(this) {
            return !mSessions.isEmpty();
        }
    }

    /**
     * Notes that a logical channel may be opened by the {@code apduSender}, which will
     * be used to close the channel when session ends (see {@link #endSession()}).
     *
     * <p>No-op if no session ongoing (see {@link #hasSession()}).
     *
     * @param apduSender The ApduSender that will open the channel.
     */
    public void noteChannelOpen(ApduSender apduSender) {
        Rlog.i(TAG, "noteChannelOpen: " + apduSender);
        synchronized(this) {
            if (hasSessionInternal()) {
                mApduSenders.add(apduSender);
            }
        }
    }

    /**
     * Marks the end of a eUICC transaction session. If this ends the last ongoing session,
     * try to close the logical channel using the noted {@code apduSender}s
     * (see {@link #noteChannelOpen()}).
     *
     * @param sessionId The session ID.
     */
    public void endSession(String sessionId) {
        Rlog.i(TAG, "endSession: " + sessionId);
        endSessionInternal(sessionId);
    }

    /**
     * Marks the end of all eUICC transaction sessions and close the logical
     * channels using the noted {@code apduSender}s
     * (see {@link #noteChannelOpen()}).
     *
     * <p>This is useful in global cleanup e.g. when EuiccService
     * implementation app crashes and indivial {@link #endSession()} calls
     * won't happen in {@link EuiccConnector}.
     */
    public void endAllSessions() {
        Rlog.i(TAG, "endAllSessions");
        endSessionInternal(null);
    }

    // The implementation of endSession(sessionId) or endAllSessions() when the sessionId is null,
    // without logging.
    private void endSessionInternal(@Nullable String sessionId) {
        ApduSender[] apduSenders = new ApduSender[0];
        synchronized(this) {
            boolean sessionRemoved = removeOrClear(mSessions, sessionId);
            // 1. sessionRemoved is false if the `sessionId` was never started or there was
            // no session. Don't bother invoke `apduSender`.
            // 2. If some session is removed, and as a result there is no more session, we
            // can clsoe channels.
            if (sessionRemoved && !hasSessionInternal()) {
                // copy mApduSenders to a local variable so we don't call closeAnyOpenChannel()
                // which can take time in synchronized block.
                apduSenders = mApduSenders.toArray(apduSenders);
                mApduSenders.clear();
            }
        }
        for (ApduSender apduSender : apduSenders) {
            apduSender.closeAnyOpenChannel();
        }
    }

    /**
     * Removes the given element from the set. If the element is null, clears the set.
     *
     * @return true if the set changed as a result of the call
     */
    private static boolean removeOrClear(Set<String> collection, @Nullable String element) {
        if (element == null) {
            boolean collectionChanged = !collection.isEmpty();
            collection.clear();
            return collectionChanged;
        } else {
            return collection.remove(element);
        }
    }

    @VisibleForTesting
    public EuiccSession() {}
}

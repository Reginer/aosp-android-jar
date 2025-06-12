/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.internal.net.eap;

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.statemachine.EapSimAkaMethodStateMachine.KEY_LEN;
import static com.android.internal.net.eap.statemachine.EapSimAkaMethodStateMachine.MASTER_KEY_LENGTH;

import android.annotation.Nullable;
import android.os.SystemClock;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** EapSimAkaIdentityTracker will manage information to support Re-authentication & Pseudonym. */
public class EapSimAkaIdentityTracker {
    private static final String TAG = EapSimAkaIdentityTracker.class.getSimpleName();
    /** Lifetime of Reauth Info */
    private static final long REAUTH_INFO_LIFETIME_MILLIS = TimeUnit.HOURS.toMillis(12); // 12Hour
    /** Quota of Reauth Info */
    @VisibleForTesting
    static final int MAX_NUMBER_OF_REAUTH_INFO = 20;

    /**
     * The LinkedHashMap to preserve credentials for re-authentication, with concatenation of reauth
     * ID and permanent ID as the key and ReauthInfo as the value.
     *
     * <p>A map entry will be deleted either if the map size reaches the max quota and the
     * entry is the oldest, or if the ReauthInfo of this entry is found expired when a new
     * ReauthInfo is registered
     */
    private static Map<String, ReauthInfo> sReauthInfoMap =
            Collections.synchronizedMap(
                    new LinkedHashMap<String, ReauthInfo>() {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<String, ReauthInfo> eldest) {
                            LOG.d(
                                    TAG,
                                    "Reached MAX_NUMBER_OF_REAUTH_INFO("
                                            + MAX_NUMBER_OF_REAUTH_INFO
                                            + ") remove EldestEntry");
                            return size() > MAX_NUMBER_OF_REAUTH_INFO;
                        }
                    });

    /** ReauthInfo stores information used for re-authentication */
    public static class ReauthInfo {
        private final int mReauthCount;
        private final byte[] mMk;
        private final byte[] mKeyEncr;
        private final byte[] mKeyAut;

        /**
         * If system elapsed time is larger than mExpiryTimestampElapsedRealtime, ReauthInfo will be
         * deleted.
         */
        private final long mExpiryTimestampElapsedRealtime;

        /**
         * Constructor of ReauthInfo
         *
         * @param mk : Master key derived at Full auth
         * @param kEncr : Encryption key derived at Full auth
         * @param kAut : Authentication key derived at Full auth
         */
        private ReauthInfo(int reauthCount, byte[] mk, byte[] kEncr, byte[] kAut) {
            mReauthCount = reauthCount;
            mMk = mk.clone();
            mKeyEncr = kEncr.clone();
            mKeyAut = kAut.clone();
            mExpiryTimestampElapsedRealtime =
                    SystemClock.elapsedRealtime() + REAUTH_INFO_LIFETIME_MILLIS;
        }

        @VisibleForTesting
        ReauthInfo(
                int reauthCount,
                byte[] mk,
                byte[] kEncr,
                byte[] kAut,
                long elapsedExpiryTimeMillis) {
            mReauthCount = reauthCount;
            mMk = mk;
            mKeyEncr = kEncr;
            mKeyAut = kAut;
            mExpiryTimestampElapsedRealtime =
                    SystemClock.elapsedRealtime() + elapsedExpiryTimeMillis;
        }

        /**
         * Retrieves reauth count for re-authentication
         *
         * @return : reauth count
         */
        public int getReauthCount() {
            return mReauthCount;
        }

        /**
         * Retrieves Master key for re-authentication
         *
         * @return : Master Key
         */
        public byte[] getMk() {
            return mMk;
        }

        /**
         * Retrieves encryption key for re-authentication
         *
         * @return : encryption key
         */
        public byte[] getKeyEncr() {
            return mKeyEncr;
        }

        /**
         * Retrieves authentication key for re-authentication
         *
         * @return : authentication key
         */
        public byte[] getKeyAut() {
            return mKeyAut;
        }

        /**
         * Check expiration of this ReauthInfo
         *
         * @return : true(not expired)/false(expired)
         */
        public boolean isValid() {
            return SystemClock.elapsedRealtime() < mExpiryTimestampElapsedRealtime;
        }
    }

    private static class EapSimAkaIdentityTrackerHolder {
        static final EapSimAkaIdentityTracker INSTANCE = new EapSimAkaIdentityTracker();
    }

    /**
     * Retrieves static EapSimAkaIdentityTracker instance
     *
     * @return static EapSimAkaIdentityTracker
     */
    public static EapSimAkaIdentityTracker getInstance() {
        return EapSimAkaIdentityTrackerHolder.INSTANCE;
    }

    /**
     * Create ReauthInfo & add it to ReauthInfo Map
     *
     * @param reauthId : re-authentication ID
     * @param permanentId : permanent ID
     * @param count : re-authentication Count
     * @param mk : master key derived from full-auth
     * @param kEncr : encryption key derived from full-auth
     * @param kAut : authentication derived from full-auth
     */
    public void registerReauthCredentials(
            String reauthId, String permanentId, int count, byte[] mk, byte[] kEncr, byte[] kAut) {
        if (mk.length != MASTER_KEY_LENGTH || kEncr.length != KEY_LEN || kAut.length != KEY_LEN) {
            throw new IllegalArgumentException("Invalid Full auth key len");
        }
        ReauthInfo reauthInfo = new ReauthInfo(count, mk, kEncr, kAut);
        String key = reauthId + permanentId;
        LOG.d(TAG, "registerReauthCredentials: key" + key + " reauth count:" + count);
        LOG.d(TAG, "    MK=" + LOG.pii(mk));
        LOG.d(TAG, "    K_encr=" + LOG.pii(kEncr));
        LOG.d(TAG, "    K_aut=" + LOG.pii(kAut));
        sReauthInfoMap.put(key, reauthInfo);
        garbageCollect();
    }

    /**
     * Add ReauthInfo to ReauthInfo Map. Only test purpose.
     *
     * @param reauthInfo : reauth Info
     */
    @VisibleForTesting
    void addReauthInfo(String key, ReauthInfo reauthInfo) {
        sReauthInfoMap.put(key, reauthInfo);
    }

    /**
     * Retrieves ReauthInfo with reauthId & permanentId as a key
     *
     * @param reauthId : re-authId set by application
     * @param permanentId : permanentId set by application
     * @return ReauthInfo : mapped ReauthInfo, return null when there is no matched info.
     */
    @Nullable
    public ReauthInfo getReauthInfo(String reauthId, String permanentId) {
        String key = reauthId + permanentId;
        ReauthInfo reauthInfo = sReauthInfoMap.get(key);
        if (reauthInfo == null) {
            LOG.d(TAG, "getReauthInfo no reauthInfo for key:" + key);
        }
        return reauthInfo;
    }

    /**
     * Delete ReauthInfo with reauthId & permanentId as a key
     *
     * @param reauthId : re-authId set by application
     * @param permanentId : permanentId set by application
     */
    public void deleteReauthInfo(String reauthId, String permanentId) {
        String key = reauthId + permanentId;
        LOG.d(TAG, "deleteReauthInfo for key:" + key);
        sReauthInfoMap.remove(key);
    }

    /** Clean up the reauthInfoMap */
    @VisibleForTesting
    void garbageCollect() {
        long elapsedTimeMillis = SystemClock.elapsedRealtime();
        ArrayList<String> expiredKeys = new ArrayList<>();

        Iterator<Map.Entry<String, ReauthInfo>> iter = sReauthInfoMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ReauthInfo> entry = iter.next();
            if (!entry.getValue().isValid()) {
                iter.remove();
            }
        }
    }

    /**
     * Retrieves the number of ReauthInfos stored in ReauthInfoMap. Only test purpose.
     *
     * @return int : Number of ReauthInfos
     */
    @VisibleForTesting
    int getNumberOfReauthInfo() {
        return sReauthInfoMap.size();
    }

    /** Clear this ReauthInfoMap. Only test purpose. */
    @VisibleForTesting
    void clearReauthInfoMap() {
        sReauthInfoMap.clear();
    }
}

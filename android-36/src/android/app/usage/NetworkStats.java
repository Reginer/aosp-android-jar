/**
 * Copyright (C) 2015 The Android Open Source Project
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

package android.app.usage;

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.content.Context;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.RemoteException;
import android.util.Log;

import com.android.net.module.util.CollectionUtils;

import dalvik.system.CloseGuard;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Class providing enumeration over buckets of network usage statistics. {@link NetworkStats}
 * objects are returned as results to various queries in {@link NetworkStatsManager}.
 */
public final class NetworkStats implements AutoCloseable {
    private static final String TAG = "NetworkStats";

    private final CloseGuard mCloseGuard = CloseGuard.get();

    /**
     * Start timestamp of stats collected
     */
    private final long mStartTimeStamp;

    /**
     * End timestamp of stats collected
     */
    private final long mEndTimeStamp;

    /**
     * Non-null array indicates the query enumerates over uids.
     */
    private int[] mUids;

    /**
     * Index of the current uid in mUids when doing uid enumeration or a single uid value,
     * depending on query type.
     */
    private int mUidOrUidIndex;

    /**
     * Tag id in case if was specified in the query.
     */
    private int mTag = android.net.NetworkStats.TAG_NONE;

    /**
     * State in case it was not specified in the query.
     */
    private int mState = Bucket.STATE_ALL;

    /**
     * The session while the query requires it, null if all the stats have been collected or close()
     * has been called.
     */
    private INetworkStatsSession mSession;
    private NetworkTemplate mTemplate;

    /**
     * Results of a summary query.
     */
    private android.net.NetworkStats mSummary = null;

    /**
     * Results of detail queries.
     */
    private NetworkStatsHistory mHistory = null;

    /**
     * Where we are in enumerating over the current result.
     */
    private int mEnumerationIndex = 0;

    /**
     * Recycling entry objects to prevent heap fragmentation.
     */
    private android.net.NetworkStats.Entry mRecycledSummaryEntry = null;
    private NetworkStatsHistory.Entry mRecycledHistoryEntry = null;

    /** @hide */
    NetworkStats(Context context, NetworkTemplate template, int flags, long startTimestamp,
            long endTimestamp, INetworkStatsService statsService)
            throws RemoteException, SecurityException {
        // Open network stats session
        mSession = statsService.openSessionForUsageStats(flags, context.getOpPackageName());
        mCloseGuard.open("close");
        mTemplate = template;
        mStartTimeStamp = startTimestamp;
        mEndTimeStamp = endTimestamp;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mCloseGuard != null) {
                mCloseGuard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    // -------------------------BEGINNING OF PUBLIC API-----------------------------------

    /**
     * Buckets are the smallest elements of a query result. As some dimensions of a result may be
     * aggregated (e.g. time or state) some values may be equal across all buckets.
     */
    public static class Bucket {
        /** @hide */
        @IntDef(prefix = { "STATE_" }, value = {
                STATE_ALL,
                STATE_DEFAULT,
                STATE_FOREGROUND
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface State {}

        /**
         * Combined usage across all states.
         */
        public static final int STATE_ALL = -1;

        /**
         * Usage not accounted for in any other state.
         */
        public static final int STATE_DEFAULT = 0x1;

        /**
         * Foreground usage.
         */
        public static final int STATE_FOREGROUND = 0x2;

        /**
         * Special UID value for aggregate/unspecified.
         */
        public static final int UID_ALL = android.net.NetworkStats.UID_ALL;

        /**
         * Special UID value for removed apps.
         */
        public static final int UID_REMOVED = TrafficStats.UID_REMOVED;

        /**
         * Special UID value for data usage by tethering.
         */
        public static final int UID_TETHERING = TrafficStats.UID_TETHERING;

        /** @hide */
        @IntDef(prefix = { "METERED_" }, value = {
                METERED_ALL,
                METERED_NO,
                METERED_YES
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface Metered {}

        /**
         * Combined usage across all metered states. Covers metered and unmetered usage.
         */
        public static final int METERED_ALL = -1;

        /**
         * Usage that occurs on an unmetered network.
         */
        public static final int METERED_NO = 0x1;

        /**
         * Usage that occurs on a metered network.
         *
         * <p>A network is classified as metered when the user is sensitive to heavy data usage on
         * that connection.
         */
        public static final int METERED_YES = 0x2;

        /** @hide */
        @IntDef(prefix = { "ROAMING_" }, value = {
                ROAMING_ALL,
                ROAMING_NO,
                ROAMING_YES
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface Roaming {}

        /**
         * Combined usage across all roaming states. Covers both roaming and non-roaming usage.
         */
        public static final int ROAMING_ALL = -1;

        /**
         * Usage that occurs on a home, non-roaming network.
         *
         * <p>Any cellular usage in this bucket was incurred while the device was connected to a
         * tower owned or operated by the user's wireless carrier, or a tower that the user's
         * wireless carrier has indicated should be treated as a home network regardless.
         *
         * <p>This is also the default value for network types that do not support roaming.
         */
        public static final int ROAMING_NO = 0x1;

        /**
         * Usage that occurs on a roaming network.
         *
         * <p>Any cellular usage in this bucket as incurred while the device was roaming on another
         * carrier's network, for which additional charges may apply.
         */
        public static final int ROAMING_YES = 0x2;

        /** @hide */
        @IntDef(prefix = { "DEFAULT_NETWORK_" }, value = {
                DEFAULT_NETWORK_ALL,
                DEFAULT_NETWORK_NO,
                DEFAULT_NETWORK_YES
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface DefaultNetworkStatus {}

        /**
         * Combined usage for this network regardless of default network status.
         */
        public static final int DEFAULT_NETWORK_ALL = -1;

        /**
         * Usage that occurs while this network is not a default network.
         *
         * <p>This implies that the app responsible for this usage requested that it occur on a
         * specific network different from the one(s) the system would have selected for it.
         */
        public static final int DEFAULT_NETWORK_NO = 0x1;

        /**
         * Usage that occurs while this network is a default network.
         *
         * <p>This implies that the app either did not select a specific network for this usage,
         * or it selected a network that the system could have selected for app traffic.
         */
        public static final int DEFAULT_NETWORK_YES = 0x2;

        /**
         * Special TAG value for total data across all tags
         */
        public static final int TAG_NONE = android.net.NetworkStats.TAG_NONE;

        private int mUid;
        private int mTag;
        private int mState;
        private int mDefaultNetworkStatus;
        private int mMetered;
        private int mRoaming;
        private long mBeginTimeStamp;
        private long mEndTimeStamp;
        private long mRxBytes;
        private long mRxPackets;
        private long mTxBytes;
        private long mTxPackets;

        private static int convertSet(@State int state) {
            switch (state) {
                case STATE_ALL: return android.net.NetworkStats.SET_ALL;
                case STATE_DEFAULT: return android.net.NetworkStats.SET_DEFAULT;
                case STATE_FOREGROUND: return android.net.NetworkStats.SET_FOREGROUND;
            }
            return 0;
        }

        private static @State int convertState(int networkStatsSet) {
            switch (networkStatsSet) {
                case android.net.NetworkStats.SET_ALL : return STATE_ALL;
                case android.net.NetworkStats.SET_DEFAULT : return STATE_DEFAULT;
                case android.net.NetworkStats.SET_FOREGROUND : return STATE_FOREGROUND;
            }
            return 0;
        }

        private static int convertUid(int uid) {
            switch (uid) {
                case TrafficStats.UID_REMOVED: return UID_REMOVED;
                case TrafficStats.UID_TETHERING: return UID_TETHERING;
            }
            return uid;
        }

        private static int convertTag(int tag) {
            switch (tag) {
                case android.net.NetworkStats.TAG_NONE: return TAG_NONE;
            }
            return tag;
        }

        private static @Metered int convertMetered(int metered) {
            switch (metered) {
                case android.net.NetworkStats.METERED_ALL : return METERED_ALL;
                case android.net.NetworkStats.METERED_NO: return METERED_NO;
                case android.net.NetworkStats.METERED_YES: return METERED_YES;
            }
            return 0;
        }

        private static @Roaming int convertRoaming(int roaming) {
            switch (roaming) {
                case android.net.NetworkStats.ROAMING_ALL : return ROAMING_ALL;
                case android.net.NetworkStats.ROAMING_NO: return ROAMING_NO;
                case android.net.NetworkStats.ROAMING_YES: return ROAMING_YES;
            }
            return 0;
        }

        private static @DefaultNetworkStatus int convertDefaultNetworkStatus(
                int defaultNetworkStatus) {
            switch (defaultNetworkStatus) {
                case android.net.NetworkStats.DEFAULT_NETWORK_ALL : return DEFAULT_NETWORK_ALL;
                case android.net.NetworkStats.DEFAULT_NETWORK_NO: return DEFAULT_NETWORK_NO;
                case android.net.NetworkStats.DEFAULT_NETWORK_YES: return DEFAULT_NETWORK_YES;
            }
            return 0;
        }

        public Bucket() {
        }

        /**
         * Key of the bucket. Usually an app uid or one of the following special values:<p />
         * <ul>
         * <li>{@link #UID_REMOVED}</li>
         * <li>{@link #UID_TETHERING}</li>
         * <li>{@link android.os.Process#SYSTEM_UID}</li>
         * </ul>
         * @return Bucket key.
         */
        public int getUid() {
            return mUid;
        }

        /**
         * Tag of the bucket.<p />
         * @return Bucket tag.
         */
        public int getTag() {
            return mTag;
        }

        /**
         * Usage state. One of the following values:<p/>
         * <ul>
         * <li>{@link #STATE_ALL}</li>
         * <li>{@link #STATE_DEFAULT}</li>
         * <li>{@link #STATE_FOREGROUND}</li>
         * </ul>
         * @return Usage state.
         */
        public @State int getState() {
            return mState;
        }

        /**
         * Metered state. One of the following values:<p/>
         * <ul>
         * <li>{@link #METERED_ALL}</li>
         * <li>{@link #METERED_NO}</li>
         * <li>{@link #METERED_YES}</li>
         * </ul>
         * <p>A network is classified as metered when the user is sensitive to heavy data usage on
         * that connection. Apps may warn before using these networks for large downloads. The
         * metered state can be set by the user within data usage network restrictions.
         */
        public @Metered int getMetered() {
            return mMetered;
        }

        /**
         * Roaming state. One of the following values:<p/>
         * <ul>
         * <li>{@link #ROAMING_ALL}</li>
         * <li>{@link #ROAMING_NO}</li>
         * <li>{@link #ROAMING_YES}</li>
         * </ul>
         */
        public @Roaming int getRoaming() {
            return mRoaming;
        }

        /**
         * Default network status. One of the following values:<p/>
         * <ul>
         * <li>{@link #DEFAULT_NETWORK_ALL}</li>
         * <li>{@link #DEFAULT_NETWORK_NO}</li>
         * <li>{@link #DEFAULT_NETWORK_YES}</li>
         * </ul>
         */
        public @DefaultNetworkStatus int getDefaultNetworkStatus() {
            return mDefaultNetworkStatus;
        }

        /**
         * Start timestamp of the bucket's time interval. Defined in terms of "Unix time", see
         * {@link java.lang.System#currentTimeMillis}.
         * @return Start of interval.
         */
        public long getStartTimeStamp() {
            return mBeginTimeStamp;
        }

        /**
         * End timestamp of the bucket's time interval. Defined in terms of "Unix time", see
         * {@link java.lang.System#currentTimeMillis}.
         * @return End of interval.
         */
        public long getEndTimeStamp() {
            return mEndTimeStamp;
        }

        /**
         * Number of bytes received during the bucket's time interval. Statistics are measured at
         * the network layer, so they include both TCP and UDP usage.
         * @return Number of bytes.
         */
        public long getRxBytes() {
            return mRxBytes;
        }

        /**
         * Number of bytes transmitted during the bucket's time interval. Statistics are measured at
         * the network layer, so they include both TCP and UDP usage.
         * @return Number of bytes.
         */
        public long getTxBytes() {
            return mTxBytes;
        }

        /**
         * Number of packets received during the bucket's time interval. Statistics are measured at
         * the network layer, so they include both TCP and UDP usage.
         * @return Number of packets.
         */
        public long getRxPackets() {
            return mRxPackets;
        }

        /**
         * Number of packets transmitted during the bucket's time interval. Statistics are measured
         * at the network layer, so they include both TCP and UDP usage.
         * @return Number of packets.
         */
        public long getTxPackets() {
            return mTxPackets;
        }
    }

    /**
     * Fills the recycled bucket with data of the next bin in the enumeration.
     * @param bucketOut Bucket to be filled with data. If null, the method does
     *                  nothing and returning false.
     * @return true if successfully filled the bucket, false otherwise.
     */
    public boolean getNextBucket(@Nullable Bucket bucketOut) {
        if (mSummary != null) {
            return getNextSummaryBucket(bucketOut);
        } else {
            return getNextHistoryBucket(bucketOut);
        }
    }

    /**
     * Check if it is possible to ask for a next bucket in the enumeration.
     * @return true if there is at least one more bucket.
     */
    public boolean hasNextBucket() {
        if (mSummary != null) {
            return mEnumerationIndex < mSummary.size();
        } else if (mHistory != null) {
            return mEnumerationIndex < mHistory.size()
                    || hasNextUid();
        }
        return false;
    }

    /**
     * Closes the enumeration. Call this method before this object gets out of scope.
     */
    @Override
    public void close() {
        if (mSession != null) {
            try {
                mSession.close();
            } catch (RemoteException e) {
                Log.w(TAG, e);
                // Otherwise, meh
            }
        }
        mSession = null;
        if (mCloseGuard != null) {
            mCloseGuard.close();
        }
    }

    // -------------------------END OF PUBLIC API-----------------------------------

    /**
     * Collects device summary results into a Bucket.
     * @throws RemoteException
     */
    Bucket getDeviceSummaryForNetwork() throws RemoteException {
        mSummary = mSession.getDeviceSummaryForNetwork(mTemplate, mStartTimeStamp, mEndTimeStamp);

        // Setting enumeration index beyond end to avoid accidental enumeration over data that does
        // not belong to the calling user.
        mEnumerationIndex = mSummary.size();

        return getSummaryAggregate();
    }

    /**
     * Collects summary results and sets summary enumeration mode.
     * @throws RemoteException
     */
    void startSummaryEnumeration() throws RemoteException {
        mSummary = mSession.getSummaryForAllUid(mTemplate, mStartTimeStamp, mEndTimeStamp,
                false /* includeTags */);
        mEnumerationIndex = 0;
    }

    /**
     * Collects tagged summary results and sets summary enumeration mode.
     * @throws RemoteException
     */
    void startTaggedSummaryEnumeration() throws RemoteException {
        mSummary = mSession.getTaggedSummaryForAllUid(mTemplate, mStartTimeStamp, mEndTimeStamp);
        mEnumerationIndex = 0;
    }

    /**
     * Collects history results for uid and resets history enumeration index.
     */
    void startHistoryUidEnumeration(int uid, int tag, int state) {
        mHistory = null;
        try {
            mHistory = mSession.getHistoryIntervalForUid(mTemplate, uid,
                    Bucket.convertSet(state), tag, NetworkStatsHistory.FIELD_ALL,
                    mStartTimeStamp, mEndTimeStamp);
            setSingleUidTagState(uid, tag, state);
        } catch (RemoteException e) {
            Log.w(TAG, e);
            // Leaving mHistory null
        }
        mEnumerationIndex = 0;
    }

    /**
     * Collects history results for network and resets history enumeration index.
     */
    void startHistoryDeviceEnumeration() {
        try {
            mHistory = mSession.getHistoryIntervalForNetwork(
                    mTemplate, NetworkStatsHistory.FIELD_ALL, mStartTimeStamp, mEndTimeStamp);
        } catch (RemoteException e) {
            Log.w(TAG, e);
            mHistory = null;
        }
        mEnumerationIndex = 0;
    }

    /**
     * Starts uid enumeration for current user.
     * @throws RemoteException
     */
    void startUserUidEnumeration() throws RemoteException {
        // TODO: getRelevantUids should be sensitive to time interval. When that's done,
        //       the filtering logic below can be removed.
        int[] uids = mSession.getRelevantUids();
        // Filtering of uids with empty history.
        final ArrayList<Integer> filteredUids = new ArrayList<>();
        for (int uid : uids) {
            try {
                NetworkStatsHistory history = mSession.getHistoryIntervalForUid(mTemplate, uid,
                        android.net.NetworkStats.SET_ALL, android.net.NetworkStats.TAG_NONE,
                        NetworkStatsHistory.FIELD_ALL, mStartTimeStamp, mEndTimeStamp);
                if (history != null && history.size() > 0) {
                    filteredUids.add(uid);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Error while getting history of uid " + uid, e);
            }
        }
        mUids = CollectionUtils.toIntArray(filteredUids);
        mUidOrUidIndex = -1;
        stepHistory();
    }

    /**
     * Steps to next uid in enumeration and collects history for that.
     */
    private void stepHistory() {
        if (hasNextUid()) {
            stepUid();
            mHistory = null;
            try {
                mHistory = mSession.getHistoryIntervalForUid(mTemplate, getUid(),
                        android.net.NetworkStats.SET_ALL, android.net.NetworkStats.TAG_NONE,
                        NetworkStatsHistory.FIELD_ALL, mStartTimeStamp, mEndTimeStamp);
            } catch (RemoteException e) {
                Log.w(TAG, e);
                // Leaving mHistory null
            }
            mEnumerationIndex = 0;
        }
    }

    private void fillBucketFromSummaryEntry(Bucket bucketOut) {
        bucketOut.mUid = Bucket.convertUid(mRecycledSummaryEntry.uid);
        bucketOut.mTag = Bucket.convertTag(mRecycledSummaryEntry.tag);
        bucketOut.mState = Bucket.convertState(mRecycledSummaryEntry.set);
        bucketOut.mDefaultNetworkStatus = Bucket.convertDefaultNetworkStatus(
                mRecycledSummaryEntry.defaultNetwork);
        bucketOut.mMetered = Bucket.convertMetered(mRecycledSummaryEntry.metered);
        bucketOut.mRoaming = Bucket.convertRoaming(mRecycledSummaryEntry.roaming);
        bucketOut.mBeginTimeStamp = mStartTimeStamp;
        bucketOut.mEndTimeStamp = mEndTimeStamp;
        bucketOut.mRxBytes = mRecycledSummaryEntry.rxBytes;
        bucketOut.mRxPackets = mRecycledSummaryEntry.rxPackets;
        bucketOut.mTxBytes = mRecycledSummaryEntry.txBytes;
        bucketOut.mTxPackets = mRecycledSummaryEntry.txPackets;
    }

    /**
     * Getting the next item in summary enumeration.
     * @param bucketOut Next item will be set here.
     * @return true if a next item could be set.
     */
    private boolean getNextSummaryBucket(@Nullable Bucket bucketOut) {
        if (bucketOut != null && mEnumerationIndex < mSummary.size()) {
            mRecycledSummaryEntry = mSummary.getValues(mEnumerationIndex++, mRecycledSummaryEntry);
            fillBucketFromSummaryEntry(bucketOut);
            return true;
        }
        return false;
    }

    Bucket getSummaryAggregate() {
        if (mSummary == null) {
            return null;
        }
        Bucket bucket = new Bucket();
        if (mRecycledSummaryEntry == null) {
            mRecycledSummaryEntry = new android.net.NetworkStats.Entry();
        }
        mSummary.getTotal(mRecycledSummaryEntry);
        fillBucketFromSummaryEntry(bucket);
        return bucket;
    }

    /**
     * Getting the next item in a history enumeration.
     * @param bucketOut Next item will be set here.
     * @return true if a next item could be set.
     */
    private boolean getNextHistoryBucket(@Nullable Bucket bucketOut) {
        if (bucketOut != null && mHistory != null) {
            if (mEnumerationIndex < mHistory.size()) {
                mRecycledHistoryEntry = mHistory.getValues(mEnumerationIndex++,
                        mRecycledHistoryEntry);
                bucketOut.mUid = Bucket.convertUid(getUid());
                bucketOut.mTag = Bucket.convertTag(mTag);
                bucketOut.mState = mState;
                bucketOut.mDefaultNetworkStatus = Bucket.DEFAULT_NETWORK_ALL;
                bucketOut.mMetered = Bucket.METERED_ALL;
                bucketOut.mRoaming = Bucket.ROAMING_ALL;
                bucketOut.mBeginTimeStamp = mRecycledHistoryEntry.bucketStart;
                bucketOut.mEndTimeStamp = mRecycledHistoryEntry.bucketStart
                        + mRecycledHistoryEntry.bucketDuration;
                bucketOut.mRxBytes = mRecycledHistoryEntry.rxBytes;
                bucketOut.mRxPackets = mRecycledHistoryEntry.rxPackets;
                bucketOut.mTxBytes = mRecycledHistoryEntry.txBytes;
                bucketOut.mTxPackets = mRecycledHistoryEntry.txPackets;
                return true;
            } else if (hasNextUid()) {
                stepHistory();
                return getNextHistoryBucket(bucketOut);
            }
        }
        return false;
    }

    // ------------------ UID LOGIC------------------------

    private boolean isUidEnumeration() {
        return mUids != null;
    }

    private boolean hasNextUid() {
        return isUidEnumeration() && (mUidOrUidIndex + 1) < mUids.length;
    }

    private int getUid() {
        // Check if uid enumeration.
        if (isUidEnumeration()) {
            if (mUidOrUidIndex < 0 || mUidOrUidIndex >= mUids.length) {
                throw new IndexOutOfBoundsException(
                        "Index=" + mUidOrUidIndex + " mUids.length=" + mUids.length);
            }
            return mUids[mUidOrUidIndex];
        }
        // Single uid mode.
        return mUidOrUidIndex;
    }

    private void setSingleUidTagState(int uid, int tag, int state) {
        mUidOrUidIndex = uid;
        mTag = tag;
        mState = state;
    }

    private void stepUid() {
        if (mUids != null) {
            ++mUidOrUidIndex;
        }
    }
}

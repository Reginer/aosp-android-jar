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

import com.android.internal.telephony.nano.PersistAtomsProto.VoiceCallRatUsage;
import com.android.telephony.Rlog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Uses a HashMap to track carrier and RAT usage, in terms of duration and number of calls.
 *
 * <p>This tracker is first used by each call, and then used for aggregating usages across calls.
 *
 * <p>This class is not thread-safe. Callers (from {@link PersistAtomsStorage} and {@link
 * VoiceCallSessionStats}) should ensure each instance of this class is accessed by only one thread
 * at a time.
 */
public class VoiceCallRatTracker {
    private static final String TAG = VoiceCallRatTracker.class.getSimpleName();

    /** A Map holding all carrier-RAT combinations and corresponding durations and call counts. */
    private final Map<Key, Value> mRatUsageMap = new HashMap<>();

    /** Tracks last carrier/RAT combination during a call. */
    private Key mLastKey;

    /** Tracks the time when last carrier/RAT combination was updated. */
    private long mLastKeyTimestampMillis;

    /** Creates an empty RAT tracker for each call. */
    VoiceCallRatTracker() {
        clear();
    }

    /** Creates an RAT tracker from saved atoms at startup. */
    public static VoiceCallRatTracker fromProto(VoiceCallRatUsage[] usages) {
        VoiceCallRatTracker tracker = new VoiceCallRatTracker();
        if (usages == null) {
            Rlog.e(TAG, "fromProto: usages=null");
        } else {
            Arrays.stream(usages).forEach(e -> tracker.addProto(e));
        }
        return tracker;
    }

    /** Append the map to javanano persist atoms. */
    public VoiceCallRatUsage[] toProto() {
        return mRatUsageMap.entrySet().stream()
                .map(VoiceCallRatTracker::entryToProto)
                .toArray(VoiceCallRatUsage[]::new);
    }

    /** Resets the tracker. */
    public void clear() {
        mRatUsageMap.clear();
        mLastKey = null;
        mLastKeyTimestampMillis = 0L;
    }

    /** Adds one RAT usage to the tracker during a call session. */
    public void add(int carrierId, int rat, long timestampMillis, Set<Integer> connectionIds) {
        // both new RAT (different key) and new connections (same key) needs to be added
        // set duration and connections (may have changed) for last RAT/carrier combination
        if (mLastKey != null) {
            long durationMillis = timestampMillis - mLastKeyTimestampMillis;
            if (durationMillis < 0L) {
                Rlog.e(TAG, "add: durationMillis<0");
                durationMillis = 0L;
            }
            addToKey(mLastKey, durationMillis, connectionIds);
        }

        // set connections for new RAT/carrier combination
        Key key = new Key(carrierId, rat);
        addToKey(key, 0L, connectionIds);

        mLastKey = key;
        mLastKeyTimestampMillis = timestampMillis;
    }

    /** Finalize the duration of the last RAT at the end of a call session. */
    public void conclude(long timestampMillis) {
        if (mLastKey != null) {
            long durationMillis = timestampMillis - mLastKeyTimestampMillis;
            if (durationMillis < 0L) {
                Rlog.e(TAG, "conclude: durationMillis<0");
                durationMillis = 0L;
            }
            Value value = mRatUsageMap.get(mLastKey);
            if (value == null) {
                Rlog.e(TAG, "conclude: value=null && mLastKey!=null");
            } else {
                value.durationMillis += durationMillis;
            }
            mRatUsageMap.values().stream().forEach(Value::endSession);
        } else {
            Rlog.e(TAG, "conclude: mLastKey=null");
        }
    }

    /** Merges this tracker with another instance created during a call session. */
    public VoiceCallRatTracker mergeWith(VoiceCallRatTracker that) {
        if (that == null) {
            Rlog.e(TAG, "mergeWith: attempting to merge with null", new Throwable());
        } else {
            that.mRatUsageMap.entrySet().stream()
                    .forEach(
                            e ->
                                    this.mRatUsageMap.merge(
                                            e.getKey(), e.getValue(), Value::mergeInPlace));
        }
        return this;
    }

    private void addToKey(Key key, long durationMillis, Set<Integer> connectionIds) {
        Value value = mRatUsageMap.get(key);
        if (value == null) {
            mRatUsageMap.put(key, new Value(durationMillis, connectionIds));
        } else {
            value.add(durationMillis, connectionIds);
        }
    }

    private void addProto(VoiceCallRatUsage usage) {
        mRatUsageMap.put(Key.fromProto(usage), Value.fromProto(usage));
    }

    private static VoiceCallRatUsage entryToProto(Map.Entry<Key, Value> entry) {
        Key key = entry.getKey();
        Value value = entry.getValue();
        VoiceCallRatUsage usage = new VoiceCallRatUsage();
        usage.carrierId = key.carrierId;
        usage.rat = key.rat;
        if (value.mConnectionIds != null) {
            Rlog.e(TAG, "call not concluded when converting to proto");
        }
        usage.totalDurationMillis = value.durationMillis;
        usage.callCount = value.callCount;
        return usage;
    }

    /*
     * NOTE: proto does not support message as map keys, and javanano generates mutable objects for
     * keys anyways. So it is better to implement the key/value class in java.
     */

    private static class Key {
        public final int carrierId;
        public final int rat;

        Key(int carrierId, int rat) {
            this.carrierId = carrierId;
            this.rat = rat;
        }

        static Key fromProto(VoiceCallRatUsage usage) {
            return new Key(usage.carrierId, usage.rat);
        }

        public int hashCode() {
            return Objects.hash(carrierId, rat);
        }

        public boolean equals(Object that) {
            if (that == null || that.getClass() != this.getClass()) {
                return false;
            }
            Key thatKey = (Key) that;
            return thatKey.carrierId == this.carrierId && thatKey.rat == this.rat;
        }
    }

    private static class Value {
        public long durationMillis;
        public long callCount;

        private Set<Integer> mConnectionIds;

        Value(long durationMillis, Set<Integer> connectionIds) {
            this.durationMillis = durationMillis;
            mConnectionIds = connectionIds;
            callCount = 0L;
        }

        private Value(long durationMillis, long callCount) {
            this.durationMillis = durationMillis;
            mConnectionIds = null;
            this.callCount = callCount;
        }

        void add(long durationMillis, Set<Integer> connectionIds) {
            this.durationMillis += durationMillis;
            if (mConnectionIds != null) {
                mConnectionIds.addAll(connectionIds);
            } else {
                Rlog.e(TAG, "Value: trying to add to concluded call");
            }
        }

        void endSession() {
            if (mConnectionIds != null) {
                if (callCount != 0L) {
                    Rlog.e(TAG, "Value: mConnectionIds!=null && callCount!=0");
                }
                callCount = mConnectionIds.size();
                mConnectionIds = null; // allow GC
            }
        }

        static Value fromProto(VoiceCallRatUsage usage) {
            Value value = new Value(usage.totalDurationMillis, usage.callCount);
            return value;
        }

        static Value mergeInPlace(Value dest, Value src) {
            if (src.mConnectionIds != null || dest.mConnectionIds != null) {
                Rlog.e(TAG, "Value: call not concluded yet when merging");
            }
            // NOTE: does not handle overflow since it is practically impossible
            dest.durationMillis += src.durationMillis;
            dest.callCount += src.callCount;
            return dest;
        }
    }
}

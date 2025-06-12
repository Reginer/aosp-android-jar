/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.am;

import static com.android.internal.util.FrameworkStatsLog.BROADCAST_PROCESSED;

import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.internal.util.FrameworkStatsLog;

final class BroadcastProcessedEventRecord {

    /**
     * Minimum threshold for logging the broadcast processed event.
     */
    private static final int MIN_THRESHOLD_FOR_LOGGING_TIME_MILLIS = 10;

    @Nullable
    private String mIntentAction;

    private int mSenderUid;

    private int mReceiverUid;

    private int mNumberOfReceivers;

    @NonNull
    private String mReceiverProcessName;

    private long mTotalBroadcastFinishTimeMillis;

    private long mMaxReceiverFinishTimeMillis = Long.MIN_VALUE;

    @NonNull
    private int[] mBroadcastTypes;

    @NonNull
    public BroadcastProcessedEventRecord setBroadcastTypes(@NonNull int[] broadcastTypes) {
        this.mBroadcastTypes = broadcastTypes;
        return this;
    }

    @NonNull
    public BroadcastProcessedEventRecord setReceiverProcessName(
            @NonNull String receiverProcessName) {
        mReceiverProcessName = receiverProcessName;
        return this;
    }

    @NonNull
    public BroadcastProcessedEventRecord setIntentAction(@Nullable String intentAction) {
        mIntentAction = intentAction;
        return this;
    }

    @NonNull
    public BroadcastProcessedEventRecord setSenderUid(int uid) {
        mSenderUid = uid;
        return this;
    }

    @NonNull
    public BroadcastProcessedEventRecord setReceiverUid(int uid) {
        mReceiverUid = uid;
        return this;
    }

    public void addReceiverFinishTime(long timeMillis) {
        mTotalBroadcastFinishTimeMillis += timeMillis;
        mMaxReceiverFinishTimeMillis = Math.max(mMaxReceiverFinishTimeMillis, timeMillis);
        mNumberOfReceivers++;
    }

    @Nullable
    String getIntentActionForTest() {
        return mIntentAction;
    }

    int getSenderUidForTest() {
        return mSenderUid;
    }

    int getReceiverUidForTest() {
        return mReceiverUid;
    }

    int getNumberOfReceiversForTest() {
        return mNumberOfReceivers;
    }

    @NonNull
    String getReceiverProcessNameForTest() {
        return mReceiverProcessName;
    }

    long getTotalBroadcastFinishTimeMillisForTest() {
        return mTotalBroadcastFinishTimeMillis;
    }

    long getMaxReceiverFinishTimeMillisForTest() {
        return mMaxReceiverFinishTimeMillis;
    }

    @NonNull
    int[] getBroadcastTypesForTest() {
        return mBroadcastTypes;
    }

    public void logToStatsD() {
        // We do not care about the processes where total time to process the
        // broadcast is less than 10ms/ are quick to process the broadcast.
        if (mTotalBroadcastFinishTimeMillis <= MIN_THRESHOLD_FOR_LOGGING_TIME_MILLIS) {
            return;
        }

        FrameworkStatsLog.write(
                BROADCAST_PROCESSED,
                mIntentAction,
                mSenderUid,
                mReceiverUid,
                mNumberOfReceivers,
                mReceiverProcessName,
                mTotalBroadcastFinishTimeMillis,
                mMaxReceiverFinishTimeMillis,
                mBroadcastTypes);
    }
}

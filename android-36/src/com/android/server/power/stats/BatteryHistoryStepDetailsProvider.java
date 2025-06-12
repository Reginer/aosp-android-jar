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

package com.android.server.power.stats;

import android.hardware.power.stats.PowerEntity;
import android.hardware.power.stats.State;
import android.hardware.power.stats.StateResidency;
import android.hardware.power.stats.StateResidencyResult;
import android.os.BatteryStats;
import android.power.PowerStatsInternal;
import android.util.Slog;
import android.util.SparseArray;

import com.android.server.LocalServices;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class BatteryHistoryStepDetailsProvider {
    public static final String TAG = "BatteryHistoryStepDetails";
    private static final boolean DEBUG = false;

    private static final int POWER_STATS_QUERY_TIMEOUT_MILLIS = 2000;
    private static final int MAX_LOW_POWER_STATS_SIZE = 32768;

    private final BatteryStatsImpl mBatteryStats;

    private final BatteryStats.HistoryStepDetails mDetails = new BatteryStats.HistoryStepDetails();

    private boolean mHasHistoryStepDetails;

    /**
     * Total time (in milliseconds) spent executing in user code.
     */
    private long mLastStepCpuUserTimeMs;
    private long mCurStepCpuUserTimeMs;
    /**
     * Total time (in milliseconds) spent executing in kernel code.
     */
    private long mLastStepCpuSystemTimeMs;
    private long mCurStepCpuSystemTimeMs;
    /**
     * Times from /proc/stat (but measured in milliseconds).
     */
    private long mLastStepStatUserTimeMs;
    private long mLastStepStatSystemTimeMs;
    private long mLastStepStatIOWaitTimeMs;
    private long mLastStepStatIrqTimeMs;
    private long mLastStepStatSoftIrqTimeMs;
    private long mLastStepStatIdleTimeMs;
    private long mCurStepStatUserTimeMs;
    private long mCurStepStatSystemTimeMs;
    private long mCurStepStatIOWaitTimeMs;
    private long mCurStepStatIrqTimeMs;
    private long mCurStepStatSoftIrqTimeMs;
    private long mCurStepStatIdleTimeMs;

    private PowerStatsInternal mPowerStatsInternal;
    private final Map<Integer, String> mEntityNames = new HashMap<>();
    private final Map<Integer, Map<Integer, String>> mStateNames = new HashMap<>();

    BatteryHistoryStepDetailsProvider(BatteryStatsImpl batteryStats) {
        mBatteryStats = batteryStats;
    }

    void onSystemReady() {
        mPowerStatsInternal = LocalServices.getService(PowerStatsInternal.class);
        if (mPowerStatsInternal != null) {
            populatePowerEntityMaps();
        }
    }

    void requestUpdate() {
        mBatteryStats.mHandler.post(this::update);
    }

    void update() {
        mHasHistoryStepDetails = false;
        mBatteryStats.updateCpuDetails();
        calculateHistoryStepDetails();
        updateStateResidency();
        mBatteryStats.getHistory().recordHistoryStepDetails(mDetails,
                mBatteryStats.mClock.elapsedRealtime(),
                mBatteryStats.mClock.uptimeMillis());
    }

    private void calculateHistoryStepDetails() {
        if (!mHasHistoryStepDetails) {
            return;
        }

        if (DEBUG) {
            Slog.d(TAG, "Step stats last: user=" + mLastStepCpuUserTimeMs + " sys="
                    + mLastStepStatSystemTimeMs + " io=" + mLastStepStatIOWaitTimeMs
                    + " irq=" + mLastStepStatIrqTimeMs + " sirq="
                    + mLastStepStatSoftIrqTimeMs + " idle=" + mLastStepStatIdleTimeMs);
            Slog.d(TAG, "Step stats cur: user=" + mCurStepCpuUserTimeMs + " sys="
                    + mCurStepStatSystemTimeMs + " io=" + mCurStepStatIOWaitTimeMs
                    + " irq=" + mCurStepStatIrqTimeMs + " sirq="
                    + mCurStepStatSoftIrqTimeMs + " idle=" + mCurStepStatIdleTimeMs);
        }
        mDetails.userTime = (int) (mCurStepCpuUserTimeMs - mLastStepCpuUserTimeMs);
        mDetails.systemTime = (int) (mCurStepCpuSystemTimeMs - mLastStepCpuSystemTimeMs);
        mDetails.statUserTime = (int) (mCurStepStatUserTimeMs - mLastStepStatUserTimeMs);
        mDetails.statSystemTime =
                (int) (mCurStepStatSystemTimeMs - mLastStepStatSystemTimeMs);
        mDetails.statIOWaitTime =
                (int) (mCurStepStatIOWaitTimeMs - mLastStepStatIOWaitTimeMs);
        mDetails.statIrqTime = (int) (mCurStepStatIrqTimeMs - mLastStepStatIrqTimeMs);
        mDetails.statSoftIrqTime =
                (int) (mCurStepStatSoftIrqTimeMs - mLastStepStatSoftIrqTimeMs);
        mDetails.statIdlTime = (int) (mCurStepStatIdleTimeMs - mLastStepStatIdleTimeMs);
        mDetails.appCpuUid1 = mDetails.appCpuUid2 = mDetails.appCpuUid3 = -1;
        mDetails.appCpuUTime1 = mDetails.appCpuUTime2 = mDetails.appCpuUTime3 = 0;
        mDetails.appCpuSTime1 = mDetails.appCpuSTime2 = mDetails.appCpuSTime3 = 0;
        SparseArray<? extends BatteryStats.Uid> uidStats = mBatteryStats.getUidStats();
        final int uidCount = uidStats.size();
        for (int i = 0; i < uidCount; i++) {
            final BatteryStatsImpl.Uid uid = (BatteryStatsImpl.Uid) uidStats.valueAt(i);
            final int totalUTimeMs =
                    (int) (uid.mCurStepUserTimeMs - uid.mLastStepUserTimeMs);
            final int totalSTimeMs =
                    (int) (uid.mCurStepSystemTimeMs - uid.mLastStepSystemTimeMs);
            final int totalTimeMs = totalUTimeMs + totalSTimeMs;
            uid.mLastStepUserTimeMs = uid.mCurStepUserTimeMs;
            uid.mLastStepSystemTimeMs = uid.mCurStepSystemTimeMs;
            if (totalTimeMs <= (mDetails.appCpuUTime3 + mDetails.appCpuSTime3)) {
                continue;
            }
            if (totalTimeMs <= (mDetails.appCpuUTime2 + mDetails.appCpuSTime2)) {
                mDetails.appCpuUid3 = uid.mUid;
                mDetails.appCpuUTime3 = totalUTimeMs;
                mDetails.appCpuSTime3 = totalSTimeMs;
            } else {
                mDetails.appCpuUid3 = mDetails.appCpuUid2;
                mDetails.appCpuUTime3 = mDetails.appCpuUTime2;
                mDetails.appCpuSTime3 = mDetails.appCpuSTime2;
                if (totalTimeMs <= (mDetails.appCpuUTime1 + mDetails.appCpuSTime1)) {
                    mDetails.appCpuUid2 = uid.mUid;
                    mDetails.appCpuUTime2 = totalUTimeMs;
                    mDetails.appCpuSTime2 = totalSTimeMs;
                } else {
                    mDetails.appCpuUid2 = mDetails.appCpuUid1;
                    mDetails.appCpuUTime2 = mDetails.appCpuUTime1;
                    mDetails.appCpuSTime2 = mDetails.appCpuSTime1;
                    mDetails.appCpuUid1 = uid.mUid;
                    mDetails.appCpuUTime1 = totalUTimeMs;
                    mDetails.appCpuSTime1 = totalSTimeMs;
                }
            }
        }
        mLastStepCpuUserTimeMs = mCurStepCpuUserTimeMs;
        mLastStepCpuSystemTimeMs = mCurStepCpuSystemTimeMs;
        mLastStepStatUserTimeMs = mCurStepStatUserTimeMs;
        mLastStepStatSystemTimeMs = mCurStepStatSystemTimeMs;
        mLastStepStatIOWaitTimeMs = mCurStepStatIOWaitTimeMs;
        mLastStepStatIrqTimeMs = mCurStepStatIrqTimeMs;
        mLastStepStatSoftIrqTimeMs = mCurStepStatSoftIrqTimeMs;
        mLastStepStatIdleTimeMs = mCurStepStatIdleTimeMs;
    }

    public void addCpuStats(int totalUTimeMs, int totalSTimeMs, int statUserTimeMs,
            int statSystemTimeMs, int statIOWaitTimeMs, int statIrqTimeMs,
            int statSoftIrqTimeMs, int statIdleTimeMs) {
        if (DEBUG) {
            Slog.d(TAG, "Adding cpu: tuser=" + totalUTimeMs + " tsys=" + totalSTimeMs
                    + " user=" + statUserTimeMs + " sys=" + statSystemTimeMs
                    + " io=" + statIOWaitTimeMs + " irq=" + statIrqTimeMs
                    + " sirq=" + statSoftIrqTimeMs + " idle=" + statIdleTimeMs);
        }
        mCurStepCpuUserTimeMs += totalUTimeMs;
        mCurStepCpuSystemTimeMs += totalSTimeMs;
        mCurStepStatUserTimeMs += statUserTimeMs;
        mCurStepStatSystemTimeMs += statSystemTimeMs;
        mCurStepStatIOWaitTimeMs += statIOWaitTimeMs;
        mCurStepStatIrqTimeMs += statIrqTimeMs;
        mCurStepStatSoftIrqTimeMs += statSoftIrqTimeMs;
        mCurStepStatIdleTimeMs += statIdleTimeMs;
    }

    public void finishAddingCpuLocked() {
        mHasHistoryStepDetails = true;
    }

    public void reset() {
        mHasHistoryStepDetails = false;
        mLastStepCpuUserTimeMs = mCurStepCpuUserTimeMs = 0;
        mLastStepCpuSystemTimeMs = mCurStepCpuSystemTimeMs = 0;
        mLastStepStatUserTimeMs = mCurStepStatUserTimeMs = 0;
        mLastStepStatSystemTimeMs = mCurStepStatSystemTimeMs = 0;
        mLastStepStatIOWaitTimeMs = mCurStepStatIOWaitTimeMs = 0;
        mLastStepStatIrqTimeMs = mCurStepStatIrqTimeMs = 0;
        mLastStepStatSoftIrqTimeMs = mCurStepStatSoftIrqTimeMs = 0;
        mLastStepStatIdleTimeMs = mCurStepStatIdleTimeMs = 0;
    }

    private void updateStateResidency() {
        mDetails.statSubsystemPowerState = null;

        if (mPowerStatsInternal == null || mEntityNames.isEmpty() || mStateNames.isEmpty()) {
            return;
        }

        final StateResidencyResult[] results;
        try {
            results = mPowerStatsInternal.getStateResidencyAsync(new int[0])
                    .get(POWER_STATS_QUERY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to getStateResidencyAsync", e);
            return;
        }

        if (results == null || results.length == 0) {
            return;
        }

        StringBuilder builder = new StringBuilder("SubsystemPowerState");
        for (int i = 0; i < results.length; i++) {
            final StateResidencyResult result = results[i];
            int length = builder.length();
            builder.append(" subsystem_").append(i);
            builder.append(" name=").append(mEntityNames.get(result.id));

            for (int j = 0; j < result.stateResidencyData.length; j++) {
                final StateResidency stateResidency = result.stateResidencyData[j];
                builder.append(" state_").append(j);
                builder.append(" name=").append(mStateNames.get(result.id).get(
                        stateResidency.id));
                builder.append(" time=").append(stateResidency.totalTimeInStateMs);
                builder.append(" count=").append(stateResidency.totalStateEntryCount);
                builder.append(" last entry=").append(stateResidency.lastEntryTimestampMs);
            }

            if (builder.length() > MAX_LOW_POWER_STATS_SIZE) {
                Slog.e(TAG, "updateStateResidency: buffer not enough");
                builder.setLength(length);
                break;
            }
        }

        mDetails.statSubsystemPowerState = builder.toString();
    }

    private void populatePowerEntityMaps() {
        PowerEntity[] entities = mPowerStatsInternal.getPowerEntityInfo();
        if (entities == null) {
            return;
        }

        for (final PowerEntity entity : entities) {
            Map<Integer, String> states = new HashMap<>();
            for (int j = 0; j < entity.states.length; j++) {
                final State state = entity.states[j];
                states.put(state.id, state.name);
            }

            mEntityNames.put(entity.id, entity.name);
            mStateNames.put(entity.id, states);
        }
    }
}

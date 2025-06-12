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

package com.android.server.power.stats;

import android.os.BatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.util.LongSparseArray;

import com.android.internal.os.PowerProfile;

public class MemoryPowerCalculator extends PowerCalculator {
    public static final String TAG = "MemoryPowerCalculator";
    private final UsageBasedPowerEstimator[] mPowerEstimators;

    public MemoryPowerCalculator(PowerProfile profile) {
        int numBuckets = profile.getNumElements(PowerProfile.POWER_MEMORY);
        mPowerEstimators = new UsageBasedPowerEstimator[numBuckets];
        for (int i = 0; i < numBuckets; i++) {
            mPowerEstimators[i] = new UsageBasedPowerEstimator(
                    profile.getAveragePower(PowerProfile.POWER_MEMORY, i));
        }
    }

    @Override
    public boolean isPowerComponentSupported(@BatteryConsumer.PowerComponent int powerComponent) {
        return powerComponent == BatteryConsumer.POWER_COMPONENT_MEMORY;
    }

    @Override
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats,
            long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        final long durationMs = calculateDuration(batteryStats, rawRealtimeUs,
                BatteryStats.STATS_SINCE_CHARGED);
        final double powerMah = calculatePower(batteryStats, rawRealtimeUs,
                BatteryStats.STATS_SINCE_CHARGED);
        builder.getAggregateBatteryConsumerBuilder(
                BatteryUsageStats.AGGREGATE_BATTERY_CONSUMER_SCOPE_DEVICE)
                .setUsageDurationMillis(BatteryConsumer.POWER_COMPONENT_MEMORY, durationMs)
                .setConsumedPower(BatteryConsumer.POWER_COMPONENT_MEMORY, powerMah);
    }

    private long calculateDuration(BatteryStats batteryStats, long rawRealtimeUs, int statsType) {
        long usageDurationMs = 0;
        LongSparseArray<? extends BatteryStats.Timer> timers = batteryStats.getKernelMemoryStats();
        for (int i = 0; i < timers.size() && i < mPowerEstimators.length; i++) {
            usageDurationMs += mPowerEstimators[i].calculateDuration(timers.valueAt(i),
                    rawRealtimeUs, statsType);
        }
        return usageDurationMs;
    }

    private double calculatePower(BatteryStats batteryStats, long rawRealtimeUs, int statsType) {
        double powerMah = 0;
        LongSparseArray<? extends BatteryStats.Timer> timers = batteryStats.getKernelMemoryStats();
        for (int i = 0; i < timers.size() && i < mPowerEstimators.length; i++) {
            UsageBasedPowerEstimator estimator = mPowerEstimators[(int) timers.keyAt(i)];
            final long usageDurationMs =
                    estimator.calculateDuration(timers.valueAt(i), rawRealtimeUs, statsType);
            powerMah += estimator.calculatePower(usageDurationMs);
        }
        return powerMah;
    }
}

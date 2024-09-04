/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.os.BatteryStatsManager;
import android.os.connectivity.CellularBatteryStats;
import android.telephony.CellSignalStrength;
import android.telephony.ModemActivityInfo;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;

import com.android.internal.telephony.nano.TelephonyProto.ModemPowerStats;

import java.util.ArrayList;
import java.util.List;

/**
 * ModemPowerMetrics holds the modem power metrics and converts them to ModemPowerStats proto buf.
 * This proto buf is included in the Telephony proto buf.
 */
public class ModemPowerMetrics {


    private static final int DATA_CONNECTION_EMERGENCY_SERVICE =
            TelephonyManager.getAllNetworkTypes().length + 1;
    private static final int DATA_CONNECTION_OTHER = DATA_CONNECTION_EMERGENCY_SERVICE + 1;
    private static final int NUM_DATA_CONNECTION_TYPES = DATA_CONNECTION_OTHER + 1;

    /* BatteryStatsManager API */
    private BatteryStatsManager mBatteryStatsManager;

    public ModemPowerMetrics(BatteryStatsManager batteryStatsManager) {
        mBatteryStatsManager = batteryStatsManager;
    }

    /**
     * Build ModemPowerStats proto
     * @return ModemPowerStats
     */
    public ModemPowerStats buildProto() {
        ModemPowerStats m = new ModemPowerStats();
        CellularBatteryStats stats = getStats();
        if (stats != null) {
            m.loggingDurationMs = stats.getLoggingDurationMillis();
            m.energyConsumedMah = stats.getEnergyConsumedMaMillis()
                / ((double) DateUtils.HOUR_IN_MILLIS);
            m.numPacketsTx = stats.getNumPacketsTx();
            m.cellularKernelActiveTimeMs = stats.getKernelActiveTimeMillis();

            long timeInVeryPoorRxSignalLevelMs = stats.getTimeInRxSignalStrengthLevelMicros(
                    CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN);
            if (timeInVeryPoorRxSignalLevelMs >= 0) {
                m.timeInVeryPoorRxSignalLevelMs = timeInVeryPoorRxSignalLevelMs;
            }

            m.sleepTimeMs = stats.getSleepTimeMillis();
            m.idleTimeMs = stats.getIdleTimeMillis();
            m.rxTimeMs = stats.getRxTimeMillis();

            List<Long> txTimeMillis = new ArrayList<>();
            for (int i = 0; i < ModemActivityInfo.getNumTxPowerLevels(); i++) {
                long t = stats.getTxTimeMillis(i);
                if (t >= 0) {
                    txTimeMillis.add(t);
                }
            }
            m.txTimeMs = txTimeMillis.stream().mapToLong(Long::longValue).toArray();

            m.numBytesTx = stats.getNumBytesTx();
            m.numPacketsRx = stats.getNumPacketsRx();
            m.numBytesRx = stats.getNumBytesRx();
            List<Long> timeInRatMicros = new ArrayList<>();
            for (int i = 0; i < NUM_DATA_CONNECTION_TYPES; i++) {
                long tr = stats.getTimeInRatMicros(i);
                if (tr >= 0) {
                    timeInRatMicros.add(tr);
                }
            }
            m.timeInRatMs = timeInRatMicros.stream().mapToLong(Long::longValue).toArray();

            List<Long> rxSignalStrengthLevelMicros = new ArrayList<>();
            for (int i = 0; i < CellSignalStrength.getNumSignalStrengthLevels(); i++) {
                long rx = stats.getTimeInRxSignalStrengthLevelMicros(i);
                if (rx >= 0) {
                    rxSignalStrengthLevelMicros.add(rx);
                }
            }
            m.timeInRxSignalStrengthLevelMs = rxSignalStrengthLevelMicros.stream().mapToLong(
                    Long::longValue).toArray();

            m.monitoredRailEnergyConsumedMah = stats.getMonitoredRailChargeConsumedMaMillis()
                / ((double) DateUtils.HOUR_IN_MILLIS);
        }
        return m;
    }

    /**
     * Get cellular stats from BatteryStatsManager
     * @return CellularBatteryStats
     */
    private CellularBatteryStats getStats() {
        if (mBatteryStatsManager == null) {
            return null;
        }
        return mBatteryStatsManager.getCellularBatteryStats();
    }
}

/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.modules.expresslog;

import android.annotation.NonNull;

/** Counter encapsulates StatsD write API calls */
public final class Counter {

    // Not instantiable.
    private Counter() {}

    /**
     * Increments Telemetry Express Counter metric by 1
     * @param metricId to log, no-op if metricId is not defined in the TeX catalog
     */
    public static void logIncrement(@NonNull String metricId) {
        logIncrement(metricId, 1);
    }

    /**
     * Increments Telemetry Express Counter metric by 1
     * @param metricId to log, no-op if metricId is not defined in the TeX catalog
     * @param uid used as a dimension for the count metric
     */
    public static void logIncrementWithUid(@NonNull String metricId, int uid) {
        logIncrementWithUid(metricId, uid, 1);
    }

    /**
     * Increments Telemetry Express Counter metric by arbitrary value
     * @param metricId to log, no-op if metricId is not defined in the TeX catalog
     * @param amount to increment counter
     */
    public static void logIncrement(@NonNull String metricId, long amount) {
        final long metricIdHash =
                MetricIds.getMetricIdHash(metricId, MetricIds.METRIC_TYPE_COUNTER);
        if (metricIdHash != MetricIds.INVALID_METRIC_ID) {
          StatsExpressLog.write(StatsExpressLog.EXPRESS_EVENT_REPORTED, metricIdHash, amount);
        }
    }

    /**
     * Increments Telemetry Express Counter metric by arbitrary value
     * @param metricId to log, no-op if metricId is not defined in the TeX catalog
     * @param uid used as a dimension for the count metric
     * @param amount to increment counter
     */
    public static void logIncrementWithUid(@NonNull String metricId, int uid, long amount) {
        final long metricIdHash =
                MetricIds.getMetricIdHash(metricId, MetricIds.METRIC_TYPE_COUNTER_WITH_UID);
        if (metricIdHash != MetricIds.INVALID_METRIC_ID) {
          StatsExpressLog.write(
              StatsExpressLog.EXPRESS_UID_EVENT_REPORTED, metricIdHash, amount, uid);
        }
    }
}

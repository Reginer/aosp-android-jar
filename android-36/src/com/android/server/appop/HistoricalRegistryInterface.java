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

package com.android.server.appop;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.AppOpsManager;
import android.content.ContentResolver;
import android.os.RemoteCallback;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A registry to record app operation access events, which are generated upon an application's
 * access to private data or system resources. These events are stored in both aggregated
 * and individual/discrete formats.
 */
public interface HistoricalRegistryInterface {
    /**
     * A callback to inform system components are ready.
     */
    void systemReady(@NonNull ContentResolver resolver);

    /**
     * Callback for system shutdown.
     */
    void shutdown();

    /**
     * Dumps aggregated/historical events to the console based on the filters.
     */
    void dump(String prefix, PrintWriter pw, int filterUid,
            @Nullable String filterPackage, @Nullable String filterAttributionTag, int filterOp,
            @AppOpsManager.HistoricalOpsRequestFilter int filter);

    /**
     * Dumps discrete/individual events to the console based on filters.
     */
    void dumpDiscreteData(@NonNull PrintWriter pw, int uidFilter,
            @Nullable String packageNameFilter, @Nullable String attributionTagFilter,
            @AppOpsManager.HistoricalOpsRequestFilter int filter, int dumpOp,
            @NonNull SimpleDateFormat sdf, @NonNull Date date, @NonNull String prefix,
            int nDiscreteOps);

    /**
     * Record duration for given op.
     */
    void increaseOpAccessDuration(int op, int uid, @NonNull String packageName,
            @NonNull String deviceId, @Nullable String attributionTag,
            @AppOpsManager.UidState int uidState,
            @AppOpsManager.OpFlags int flags, long eventStartTime, long increment,
            @AppOpsManager.AttributionFlags int attributionFlags, int attributionChainId);

    /**
     * Record access counts for given op.
     */
    void incrementOpAccessedCount(int op, int uid, @NonNull String packageName,
            @NonNull String deviceId, @Nullable String attributionTag,
            @AppOpsManager.UidState int uidState,
            @AppOpsManager.OpFlags int flags, long accessTime,
            @AppOpsManager.AttributionFlags int attributionFlags, int attributionChainId,
            int accessCount);

    /**
     * Record rejected counts for given op.
     */
    void incrementOpRejectedCount(int op, int uid, @NonNull String packageName,
            @Nullable String attributionTag, @AppOpsManager.UidState int uidState,
            @AppOpsManager.OpFlags int flags);

    /**
     * Read historical ops from both aggregated and discrete events based on input filter.
     */
    void getHistoricalOps(int uid, @Nullable String packageName, @Nullable String attributionTag,
            @Nullable String[] opNames, @AppOpsManager.OpHistoryFlags int historyFlags,
            @AppOpsManager.HistoricalOpsRequestFilter int filter, long beginTimeMillis,
            long endTimeMillis,
            @AppOpsManager.OpFlags int flags, @Nullable String[] attributionExemptPkgs,
            @NonNull RemoteCallback callback);

    /**
     * Remove app op events for a given UID and package.
     */
    void clearHistory(int uid, String packageName);

    /**
     * A periodic callback from {@link AppOpsService} to flush the in memory discrete
     * app op events to disk/database.
     */
    void writeAndClearDiscreteHistory();

    /**
     * A callback flush the in memory app op events to disk/database.
     */
    void persistPendingHistory();

    /**
     * Set history parameters.
     *
     * @param mode - Whether historical registry is Active, Passive or Disabled.
     * @param baseSnapshotInterval - Interval between 2 snapshots, default 15 minutes.
     * @param intervalCompressionMultiplier - Interval compression multiplier, default is 10.
     */
    void setHistoryParameters(@AppOpsManager.HistoricalMode int mode,
            long baseSnapshotInterval, long intervalCompressionMultiplier);

    /**
     * Reset history parameters to defaults.
     */
    void resetHistoryParameters();

    /**
     * Remove all app op accesses from both aggregated and individual event's storage.
     */
    void clearAllHistory();

    /**
     * Offsets the history by the given duration.
     */
    void offsetHistory(long offsetMillis);

    /**
     * Retrieve historical app op stats for a period form disk.
     */
    void getHistoricalOpsFromDiskRaw(int uid, @Nullable String packageName,
            @Nullable String attributionTag, @Nullable String[] opNames,
            @AppOpsManager.OpHistoryFlags int historyFlags,
            @AppOpsManager.HistoricalOpsRequestFilter int filter,
            long beginTimeMillis, long endTimeMillis, @AppOpsManager.OpFlags int flags,
            String[] attributionExemptedPackages, @NonNull RemoteCallback callback);

    /**
     * Adds ops to the history directly. This could be useful for testing especially
     * when the historical registry operates in passive mode.
     */
    void addHistoricalOps(AppOpsManager.HistoricalOps ops);
}

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
import android.content.Context;
import android.os.RemoteCallback;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

// TODO add more documentation later

/**
 * This historical registry implementation store app events in sqlite. The data is stored in 2
 * tables 1) discrete events 2) aggregated events.
 */
public class HistoricalRegistrySql implements HistoricalRegistryInterface {
    // TODO impl will be added in a separate CL
    HistoricalRegistrySql(Context context) {
    }

    HistoricalRegistrySql(HistoricalRegistrySql other) {
    }

    @Override
    public void systemReady(@NonNull ContentResolver resolver) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void dump(String prefix, PrintWriter pw, int filterUid,
            @Nullable String filterPackage, @Nullable String filterAttributionTag, int filterOp,
            int filter) {

    }

    @Override
    public void dumpDiscreteData(@NonNull PrintWriter pw, int uidFilter,
            @Nullable String packageNameFilter, @Nullable String attributionTagFilter, int filter,
            int dumpOp, @NonNull SimpleDateFormat sdf, @NonNull Date date, @NonNull String prefix,
            int nDiscreteOps) {

    }

    @Override
    public void increaseOpAccessDuration(int op, int uid, @NonNull String packageName,
            @NonNull String deviceId, @Nullable String attributionTag, int uidState, int flags,
            long eventStartTime, long increment, int attributionFlags, int attributionChainId) {

    }

    @Override
    public void incrementOpAccessedCount(int op, int uid, @NonNull String packageName,
            @NonNull String deviceId, @Nullable String attributionTag, int uidState, int flags,
            long accessTime, int attributionFlags, int attributionChainId, int accessCount) {

    }

    @Override
    public void incrementOpRejectedCount(int op, int uid, @NonNull String packageName,
            @Nullable String attributionTag, int uidState, int flags) {

    }

    @Override
    public void getHistoricalOps(int uid, @Nullable String packageName,
            @Nullable String attributionTag, @Nullable String[] opNames, int historyFlags,
            int filter, long beginTimeMillis, long endTimeMillis, int flags,
            @Nullable String[] attributionExemptPkgs, @NonNull RemoteCallback callback) {

    }

    @Override
    public void clearHistory(int uid, String packageName) {

    }

    @Override
    public void writeAndClearDiscreteHistory() {

    }

    @Override
    public void persistPendingHistory() {

    }

    @Override
    public void setHistoryParameters(int mode, long baseSnapshotInterval,
            long intervalCompressionMultiplier) {

    }

    @Override
    public void resetHistoryParameters() {

    }

    @Override
    public void clearAllHistory() {

    }

    @Override
    public void offsetHistory(long offsetMillis) {

    }

    @Override
    public void getHistoricalOpsFromDiskRaw(int uid, @Nullable String packageName,
            @Nullable String attributionTag, @Nullable String[] opNames, int historyFlags,
            int filter, long beginTimeMillis, long endTimeMillis, int flags,
            String[] attributionExemptedPackages, @NonNull RemoteCallback callback) {

    }

    @Override
    public void addHistoricalOps(AppOpsManager.HistoricalOps ops) {

    }
}

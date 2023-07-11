/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.wifitrackerlib;

import android.net.wifi.ScanResult;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to keep a running list of scan results merged by SSID+BSSID pair.
 *
 * Thread-safe.
 */
public class ScanResultUpdater {
    private Map<Pair<String, String>, ScanResult> mScanResultsBySsidAndBssid = new HashMap<>();
    private final long mMaxScanAgeMillis;
    private final Object mLock = new Object();
    private final Clock mClock;

    /**
     * Creates a ScanResultUpdater with no max scan age.
     *
     * @param clock Elapsed real time Clock to compare with ScanResult timestamps.
     */
    public ScanResultUpdater(Clock clock) {
        this(clock, Long.MAX_VALUE);
    }

    /**
     * Creates a ScanResultUpdater with a max scan age in milliseconds. Scans older than this limit
     * will be pruned upon update/retrieval to keep the size of the scan list down.
     */
    public ScanResultUpdater(Clock clock, long maxScanAgeMillis) {
        mMaxScanAgeMillis = maxScanAgeMillis;
        mClock = clock;
    }

    /**
     * Updates scan result list and replaces older scans of the same SSID+BSSID pair.
     */
    public void update(@NonNull List<ScanResult> newResults) {
        synchronized (mLock) {
            evictOldScans();

            for (ScanResult result : newResults) {
                final Pair<String, String> key = new Pair(result.SSID, result.BSSID);
                ScanResult prevResult = mScanResultsBySsidAndBssid.get(key);
                if (prevResult == null || (prevResult.timestamp < result.timestamp)) {
                    mScanResultsBySsidAndBssid.put(key, result);
                }
            }
        }
    }

    /**
     * Returns all seen scan results merged by SSID+BSSID pair.
     */
    @NonNull
    public List<ScanResult> getScanResults() {
        return getScanResults(mMaxScanAgeMillis);
    }

    /**
     * Returns all seen scan results merged by SSID+BSSID pair and newer than maxScanAgeMillis.
     * maxScanAgeMillis must be less than or equal to the mMaxScanAgeMillis field if it was set.
     */
    @NonNull
    public List<ScanResult> getScanResults(long maxScanAgeMillis) throws IllegalArgumentException {
        if (maxScanAgeMillis > mMaxScanAgeMillis) {
            throw new IllegalArgumentException(
                    "maxScanAgeMillis argument cannot be greater than mMaxScanAgeMillis!");
        }
        synchronized (mLock) {
            List<ScanResult> ageFilteredResults = new ArrayList<>();
            for (ScanResult result : mScanResultsBySsidAndBssid.values()) {
                if (mClock.millis() - result.timestamp / 1000 <= maxScanAgeMillis) {
                    ageFilteredResults.add(result);
                }
            }
            return ageFilteredResults;
        }
    }

    private void evictOldScans() {
        synchronized (mLock) {
            mScanResultsBySsidAndBssid.entrySet().removeIf((entry) ->
                    mClock.millis() - entry.getValue().timestamp / 1000 > mMaxScanAgeMillis);
        }
    }
}

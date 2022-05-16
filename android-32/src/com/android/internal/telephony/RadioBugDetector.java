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

package com.android.internal.telephony;

import android.content.Context;
import android.hardware.radio.V1_0.RadioError;
import android.provider.Settings;
import android.telephony.AnomalyReporter;

import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class aims to detect radio bug based on wakelock timeout and system error.
 *
 * {@hide}
 */
public class RadioBugDetector {
    private static final String TAG = "RadioBugDetector";

    /** Radio error constants */
    private static final int RADIO_BUG_NONE = 0x00;
    private static final int RADIO_BUG_REPETITIVE_WAKELOCK_TIMEOUT_ERROR = 0x01;
    @VisibleForTesting
    protected static final int RADIO_BUG_REPETITIVE_SYSTEM_ERROR = 0x02;

    /**
     * Default configuration values for radio bug detection.
     * The value should be large enough to avoid false alarm. From past log analysis, 10 wakelock
     * timeout took around 1 hour. 100 accumulated system_err is an estimation for abnormal radio.
     */
    private static final int DEFAULT_WAKELOCK_TIMEOUT_COUNT_THRESHOLD = 10;
    private static final int DEFAULT_SYSTEM_ERROR_COUNT_THRESHOLD = 100;

    private Context mContext;
    private int mContinuousWakelockTimoutCount = 0;
    private int mRadioBugStatus = RADIO_BUG_NONE;
    private int mSlotId;
    private int mWakelockTimeoutThreshold = 0;
    private int mSystemErrorThreshold = 0;

    private HashMap<Integer, Integer> mSysErrRecord = new HashMap<Integer, Integer>();

    /** Constructor */
    public RadioBugDetector(Context context, int slotId) {
        mContext = context;
        mSlotId = slotId;
        init();
    }

    private void init() {
        mWakelockTimeoutThreshold = Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.RADIO_BUG_WAKELOCK_TIMEOUT_COUNT_THRESHOLD,
                DEFAULT_WAKELOCK_TIMEOUT_COUNT_THRESHOLD);
        mSystemErrorThreshold = Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.RADIO_BUG_SYSTEM_ERROR_COUNT_THRESHOLD,
                DEFAULT_SYSTEM_ERROR_COUNT_THRESHOLD);
    }

    /**
     * Detect radio bug and notify this issue once the threshold is reached.
     *
     * @param requestType The command type information we retrieved
     * @param error       The error we received
     */
    public synchronized void detectRadioBug(int requestType, int error) {
        /**
         * When this function is executed, it means RIL is alive. So, reset WakelockTimeoutCount.
         * Regarding SYSTEM_ERR, although RIL is alive, the connection with modem may be broken.
         * For this error, accumulate the count and check if broadcast should be sent or not.
         * For normal response or other error, reset the count of SYSTEM_ERR of the specific
         * request type and mRadioBugStatus if necessary
         */
        mContinuousWakelockTimoutCount = 0;
        if (error == RadioError.SYSTEM_ERR) {
            int errorCount = mSysErrRecord.getOrDefault(requestType, 0);
            errorCount++;
            mSysErrRecord.put(requestType, errorCount);
            broadcastBug(true);
        } else {
            // Reset system error count if we get non-system error response.
            mSysErrRecord.remove(requestType);
            if (!isFrequentSystemError()) {
                mRadioBugStatus = RADIO_BUG_NONE;
            }
        }
    }

    /**
     * When wakelock timeout is detected, accumulate its count and check if broadcast should be
     * sent or not.
     */
    public void processWakelockTimeout() {
        mContinuousWakelockTimoutCount++;
        broadcastBug(false);
    }

    private synchronized void broadcastBug(boolean isSystemError) {
        if (isSystemError) {
            if (!isFrequentSystemError()) {
                return;
            }
        } else {
            if (mContinuousWakelockTimoutCount < mWakelockTimeoutThreshold) {
                return;
            }
        }

        // Notify that the RIL is stuck if SYSTEM_ERR or WAKE_LOCK_TIMEOUT returned by vendor
        // RIL is more than the threshold times.
        if (mRadioBugStatus == RADIO_BUG_NONE) {
            mRadioBugStatus = isSystemError ? RADIO_BUG_REPETITIVE_SYSTEM_ERROR :
                    RADIO_BUG_REPETITIVE_WAKELOCK_TIMEOUT_ERROR;
            String message = "Repeated radio error " + mRadioBugStatus + " on slot " + mSlotId;
            Rlog.d(TAG, message);
            // Using fixed UUID to avoid duplicate bugreport notification
            AnomalyReporter.reportAnomaly(
                    UUID.fromString("d264ead0-3f05-11ea-b77f-2e728ce88125"),
                    message);
        }
    }

    private boolean isFrequentSystemError() {
        int countForError = 0;
        boolean error = false;
        for (int count : mSysErrRecord.values()) {
            countForError += count;
            if (countForError >= mSystemErrorThreshold) {
                error = true;
                break;
            }
        }
        return error;
    }

    @VisibleForTesting
    public int getRadioBugStatus() {
        return mRadioBugStatus;
    }

    @VisibleForTesting
    public int getWakelockTimeoutThreshold() {
        return mWakelockTimeoutThreshold;
    }

    @VisibleForTesting
    public int getSystemErrorThreshold() {
        return mSystemErrorThreshold;
    }

    @VisibleForTesting
    public int getWakelockTimoutCount() {
        return mContinuousWakelockTimoutCount;
    }

}


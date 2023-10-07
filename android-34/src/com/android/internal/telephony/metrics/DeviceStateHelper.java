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

package com.android.internal.telephony.metrics;

import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_SERVICE_STATE__FOLD_STATE__STATE_CLOSED;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_SERVICE_STATE__FOLD_STATE__STATE_FLIPPED;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_SERVICE_STATE__FOLD_STATE__STATE_HALF_OPENED;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_SERVICE_STATE__FOLD_STATE__STATE_OPENED;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_SERVICE_STATE__FOLD_STATE__STATE_UNKNOWN;

import android.content.Context;
import android.hardware.devicestate.DeviceStateManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;

import com.android.internal.telephony.Phone;

/** Device state information like the fold state. */
public class DeviceStateHelper {
    private int mFoldState = CELLULAR_SERVICE_STATE__FOLD_STATE__STATE_UNKNOWN;

    public DeviceStateHelper(Context context) {
        HandlerThread mHandlerThread = new HandlerThread("DeviceStateHelperThread");
        mHandlerThread.start();
        context.getSystemService(DeviceStateManager.class)
                .registerCallback(
                        new HandlerExecutor(new Handler(mHandlerThread.getLooper())),
                        state -> {
                            updateFoldState(state);
                        });
    }

    private void updateFoldState(int posture) {
        switch (posture) {
            case 0:
                mFoldState = CELLULAR_SERVICE_STATE__FOLD_STATE__STATE_CLOSED;
                break;
            case 1:
                mFoldState = CELLULAR_SERVICE_STATE__FOLD_STATE__STATE_HALF_OPENED;
                break;
            case 2:
                mFoldState = CELLULAR_SERVICE_STATE__FOLD_STATE__STATE_OPENED;
                break;
            case 4:
                mFoldState = CELLULAR_SERVICE_STATE__FOLD_STATE__STATE_FLIPPED;
                break;
            default:
                mFoldState = CELLULAR_SERVICE_STATE__FOLD_STATE__STATE_UNKNOWN;
        }
        updateServiceStateStats();
    }

    private void updateServiceStateStats() {
        for (Phone phone : MetricsCollector.getPhonesIfAny()) {
            phone.getServiceStateTracker().getServiceStateStats().onFoldStateChanged(mFoldState);
        }
    }

    public int getFoldState() {
        return mFoldState;
    }
}

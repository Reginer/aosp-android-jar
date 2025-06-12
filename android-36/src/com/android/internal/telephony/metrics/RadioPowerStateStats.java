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

import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_RADIO_POWER_STATE_CHANGED;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_RADIO_POWER_STATE_CHANGED__STATE__RADIO_POWER_STATE_OFF;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_RADIO_POWER_STATE_CHANGED__STATE__RADIO_POWER_STATE_ON;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_RADIO_POWER_STATE_CHANGED__STATE__RADIO_POWER_STATE_UNAVAILABLE;
import static com.android.internal.telephony.TelephonyStatsLog.CELLULAR_RADIO_POWER_STATE_CHANGED__STATE__RADIO_POWER_STATE_UNKNOWN;

import android.telephony.Annotation;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.TelephonyStatsLog;

/** Logs cellular radio power state changes to statsd */
public class RadioPowerStateStats {

    /** Logs cellular radio power state changes to statsd */
    public static void onRadioStateChanged(@Annotation.RadioPowerState int state) {
        TelephonyStatsLog.write(CELLULAR_RADIO_POWER_STATE_CHANGED,
                radioPowerStateToProtoEnum(state));
    }

    private static int radioPowerStateToProtoEnum(@Annotation.RadioPowerState int state) {
        switch (state) {
            case TelephonyManager.RADIO_POWER_OFF:
                return CELLULAR_RADIO_POWER_STATE_CHANGED__STATE__RADIO_POWER_STATE_OFF;
            case TelephonyManager.RADIO_POWER_ON:
                return CELLULAR_RADIO_POWER_STATE_CHANGED__STATE__RADIO_POWER_STATE_ON;
            case TelephonyManager.RADIO_POWER_UNAVAILABLE:
                return CELLULAR_RADIO_POWER_STATE_CHANGED__STATE__RADIO_POWER_STATE_UNAVAILABLE;
            default:
                return CELLULAR_RADIO_POWER_STATE_CHANGED__STATE__RADIO_POWER_STATE_UNKNOWN;
        }
    }

}

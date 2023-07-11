/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.internal.telephony.d2d;

import android.telecom.Connection;
import android.telecom.CallDiagnostics;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.BiMap;

/**
 * Helper class to map between the message types and values used in {@link Communicator} and those
 * defined in the public API in {@link CallDiagnostics}.
 */
public class MessageTypeAndValueHelper {
    // Maps between the local message and value types defined here and those defined in the
    // DiagnosticCall class as part of the actual API.

    /**
     * Convert between the local message type (e.g.
     * {@link Communicator#MESSAGE_CALL_RADIO_ACCESS_TYPE})
     * and
     * the ones referred to in {@link CallDiagnostics}.
     */
    public static final BiMap<Integer, Integer> MSG_TYPE_TO_DC_MSG_TYPE = new BiMap<>();

    /**
     * Convert between the local RAT type (e.g. {@link Communicator#RADIO_ACCESS_TYPE_IWLAN}) and
     * the ones
     * referred to by {@link CallDiagnostics#MESSAGE_CALL_NETWORK_TYPE}.
     */
    public static final BiMap<Integer, Integer> RAT_TYPE_TO_DC_NETWORK_TYPE = new BiMap<>();

    /**
     * Convert between the local codec (e.g. {@link Communicator#AUDIO_CODEC_AMR_WB}) and the ones
     * referred to by {@link CallDiagnostics#MESSAGE_CALL_AUDIO_CODEC}.
     */
    public static final BiMap<Integer, Integer> CODEC_TO_DC_CODEC = new BiMap<>();

    /**
     * Convert between the local battery state (e.g. {@link Communicator#BATTERY_STATE_GOOD}) and
     * the ones referred to by {@link CallDiagnostics#MESSAGE_DEVICE_BATTERY_STATE}.
     */
    public static final BiMap<Integer, Integer> BATTERY_STATE_TO_DC_BATTERY_STATE = new BiMap();

    /**
     * Convert between the local battery state (e.g. {@link Communicator#COVERAGE_GOOD}) and the
     * ones referred to by {@link CallDiagnostics#MESSAGE_DEVICE_NETWORK_COVERAGE}.
     */
    public static final BiMap<Integer, Integer> COVERAGE_TO_DC_COVERAGE = new BiMap();

    static {
        MSG_TYPE_TO_DC_MSG_TYPE.put(Communicator.MESSAGE_CALL_RADIO_ACCESS_TYPE,
                CallDiagnostics.MESSAGE_CALL_NETWORK_TYPE);
        MSG_TYPE_TO_DC_MSG_TYPE.put(Communicator.MESSAGE_CALL_AUDIO_CODEC,
                CallDiagnostics.MESSAGE_CALL_AUDIO_CODEC);
        MSG_TYPE_TO_DC_MSG_TYPE.put(Communicator.MESSAGE_DEVICE_BATTERY_STATE,
                CallDiagnostics.MESSAGE_DEVICE_BATTERY_STATE);
        MSG_TYPE_TO_DC_MSG_TYPE.put(Communicator.MESSAGE_DEVICE_NETWORK_COVERAGE,
                CallDiagnostics.MESSAGE_DEVICE_NETWORK_COVERAGE);

        RAT_TYPE_TO_DC_NETWORK_TYPE.put(Communicator.RADIO_ACCESS_TYPE_LTE,
                TelephonyManager.NETWORK_TYPE_LTE);
        RAT_TYPE_TO_DC_NETWORK_TYPE.put(Communicator.RADIO_ACCESS_TYPE_IWLAN,
                TelephonyManager.NETWORK_TYPE_IWLAN);
        RAT_TYPE_TO_DC_NETWORK_TYPE.put(Communicator.RADIO_ACCESS_TYPE_NR,
                TelephonyManager.NETWORK_TYPE_NR);

        CODEC_TO_DC_CODEC.put(Communicator.AUDIO_CODEC_EVS, Connection.AUDIO_CODEC_EVS_WB);
        CODEC_TO_DC_CODEC.put(Communicator.AUDIO_CODEC_AMR_WB, Connection.AUDIO_CODEC_AMR_WB);
        CODEC_TO_DC_CODEC.put(Communicator.AUDIO_CODEC_AMR_NB, Connection.AUDIO_CODEC_AMR);

        BATTERY_STATE_TO_DC_BATTERY_STATE.put(Communicator.BATTERY_STATE_LOW,
                CallDiagnostics.BATTERY_STATE_LOW);
        BATTERY_STATE_TO_DC_BATTERY_STATE.put(Communicator.BATTERY_STATE_GOOD,
                CallDiagnostics.BATTERY_STATE_GOOD);
        BATTERY_STATE_TO_DC_BATTERY_STATE.put(Communicator.BATTERY_STATE_CHARGING,
                CallDiagnostics.BATTERY_STATE_CHARGING);

        COVERAGE_TO_DC_COVERAGE.put(Communicator.COVERAGE_POOR, CallDiagnostics.COVERAGE_POOR);
        COVERAGE_TO_DC_COVERAGE.put(Communicator.COVERAGE_GOOD, CallDiagnostics.COVERAGE_GOOD);
    }
}

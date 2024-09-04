/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.telephony.emergency;

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Define the constants for emergency call domain selection.
 */
public class EmergencyConstants {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"MODE_EMERGENCY_"},
            value = {
                    MODE_EMERGENCY_NONE,
                    MODE_EMERGENCY_WWAN,
                    MODE_EMERGENCY_WLAN,
                    MODE_EMERGENCY_CALLBACK,
            })
    public @interface EmergencyMode {}

    /**
     * Default value.
     */
    public static final int MODE_EMERGENCY_NONE = 0;
    /**
     * Mode Type Emergency WWAN, indicates that the current domain selected for the Emergency call
     * is cellular.
     */
    public static final int MODE_EMERGENCY_WWAN = 1;
    /**
     * Mode Type Emergency WLAN, indicates that the current domain selected for the Emergency call
     * is WLAN/WIFI.
     */
    public static final int MODE_EMERGENCY_WLAN = 2;
    /**
     * Mode Type Emergency Callback, indicates that the current mode set request is for Emergency
     * callback.
     */
    public static final int MODE_EMERGENCY_CALLBACK = 3;

    /** Converts the {@link EmergencyMode} to String */
    public static String emergencyModeToString(int emcMode) {
        switch (emcMode) {
            case MODE_EMERGENCY_NONE: return "NONE";
            case MODE_EMERGENCY_WWAN: return "WWAN";
            case MODE_EMERGENCY_WLAN: return "WLAN";
            case MODE_EMERGENCY_CALLBACK: return "CALLBACK";
            default: return "UNKNOWN(" + emcMode + ")";
        }
    }
}

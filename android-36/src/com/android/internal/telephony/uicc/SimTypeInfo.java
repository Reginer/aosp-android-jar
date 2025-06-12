/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This class contains the sim type information of active physical slot ids.
 */
public class SimTypeInfo {

    /**
     * SimType (bit mask)
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            SimType.SIM_TYPE_UNKNOWN,
            SimType.SIM_TYPE_PHYSICAL,
            SimType.SIM_TYPE_ESIM,
    })
    public @interface SimType {
        /** Unknown SIM */
        int SIM_TYPE_UNKNOWN = 0;
        /** Physical SIM (Can have eUICC capabilities) */
        int SIM_TYPE_PHYSICAL = 1 << 0;
        /** Embedded SIM*/
        int SIM_TYPE_ESIM = 1 << 1;
    }

    public @SimType int mCurrentSimType = SimType.SIM_TYPE_UNKNOWN;
    // Bitmask of the supported {@code SimType}s
    public int mSupportedSimTypes;

    /**
     * Set the current SimType according to the input type.
     */
    public void setCurrentSimType(int simType) {
        switch(simType) {
            case android.hardware.radio.config.SimType.UNKNOWN:
                mCurrentSimType = SimType.SIM_TYPE_UNKNOWN;
                break;
            case android.hardware.radio.config.SimType.PHYSICAL:
                mCurrentSimType = SimType.SIM_TYPE_PHYSICAL;
                break;
            case android.hardware.radio.config.SimType.ESIM:
                mCurrentSimType = SimType.SIM_TYPE_ESIM;
                break;
            default:
                throw new RuntimeException("Unrecognized RIL_SimType: " + simType);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SimTypeInfo {activeSimType=").append(mCurrentSimType).append(",")
                .append("supportedSimType=").append(mSupportedSimTypes);
        sb.append("}");
        return sb.toString();
    }
}

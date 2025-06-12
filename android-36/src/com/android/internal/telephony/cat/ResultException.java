/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony.cat;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;


/**
 * Class for errors in the Result object.
 *
 * {@hide}
 */
public class ResultException extends CatException {
    private ResultCode mResult;
    private int mAdditionalInfo;
    private String mExplanation;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public ResultException(ResultCode result) {
        super();

        // ETSI TS 102 223, 8.12 -- For the general results '20', '21', '26',
        // '38', '39', '3A', '3C', and '3D', it is mandatory for the terminal
        // to provide a specific cause value as additional information.
        switch (result) {
            case TERMINAL_CRNTLY_UNABLE_TO_PROCESS:    // 0x20
            case NETWORK_CRNTLY_UNABLE_TO_PROCESS:     // 0x21
            case LAUNCH_BROWSER_ERROR:                 // 0x26
            case MULTI_CARDS_CMD_ERROR:                // 0x38
            case USIM_CALL_CONTROL_PERMANENT:          // 0x39
            case BIP_ERROR:                            // 0x3a
            case FRAMES_ERROR:                         // 0x3c
            case MMS_ERROR:                            // 0x3d
                throw new AssertionError(
                        "For result code, " + result +
                        ", additional information must be given!");
            default:
                break;
        }

        mResult = result;
        mAdditionalInfo = -1;
        mExplanation = "";
    }

    public ResultException(ResultCode result, String explanation) {
        this(result);
        mExplanation = explanation;
    }

    public ResultException(ResultCode result, int additionalInfo) {
        this(result);

        if (additionalInfo < 0) {
            throw new AssertionError(
                    "Additional info must be greater than zero!");
        }

        mAdditionalInfo = additionalInfo;
    }

    public ResultException(ResultCode result, int additionalInfo, String explanation) {
        this(result, additionalInfo);
        mExplanation = explanation;
    }

    public ResultCode result() {
        return mResult;
    }

    public boolean hasAdditionalInfo() {
        return mAdditionalInfo >= 0;
    }

    public int additionalInfo() {
        return mAdditionalInfo;
    }

    public String explanation() {
        return mExplanation;
    }

    @Override
    public String toString() {
        return "result=" + mResult + " additionalInfo=" + mAdditionalInfo +
                " explantion=" + mExplanation;
    }
}

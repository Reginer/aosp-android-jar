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

package com.android.internal.telephony;

import android.compat.annotation.UnsupportedAppUsage;

/**
 * {@hide}
 */
public class CallStateException extends Exception
{
    private int mError = ERROR_INVALID;

    /** The error code is not valid (Not received a disconnect cause) */
    public static final int ERROR_INVALID = -1;

    public static final int ERROR_OUT_OF_SERVICE = 1;
    public static final int ERROR_POWER_OFF = 2;
    public static final int ERROR_ALREADY_DIALING = 3;
    public static final int ERROR_CALL_RINGING = 4;
    public static final int ERROR_CALLING_DISABLED = 5;
    public static final int ERROR_TOO_MANY_CALLS = 6;
    public static final int ERROR_OTASP_PROVISIONING_IN_PROCESS = 7;

    public
    CallStateException()
    {
    }

    @UnsupportedAppUsage
    public
    CallStateException(String string)
    {
        super(string);
    }

    public
    CallStateException(int error, String string)
    {
        super(string);
        mError = error;
    }

    public int getError() {
        return mError;
    }
}

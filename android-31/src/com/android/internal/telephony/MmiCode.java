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
import android.os.ResultReceiver;

import java.util.regex.Pattern;

/**
 * {@hide}
 */
public interface MmiCode
{
    /**
     * {@hide}
     */
    public enum State {
        @UnsupportedAppUsage
        PENDING,
        @UnsupportedAppUsage
        CANCELLED,
        @UnsupportedAppUsage
        COMPLETE,
        @UnsupportedAppUsage
        FAILED
    }

    /**
     * @return Current state of MmiCode request
     */
    public State getState();

    /**
     * @return Localized message for UI display, valid only in COMPLETE
     * or FAILED states. null otherwise
     */

    public CharSequence getMessage();

    /**
     * @return Phone associated with the MMI/USSD message
     */
    @UnsupportedAppUsage
    public Phone getPhone();

    /**
     * Cancels pending MMI request.
     * State becomes CANCELLED unless already COMPLETE or FAILED
     */
    public void cancel();

    /**
     * @return true if the network response is a REQUEST for more user input.
     */
    public boolean isUssdRequest();

    /**
     * @return true if the request was initiated USSD by the network.
     */
    boolean isNetworkInitiatedUssd();

    /**
     * @return true if an outstanding request can be canceled.
     */
    public boolean isCancelable();

    /**
     * @return true if the Service Code is PIN/PIN2/PUK/PUK2-related
     */
    public boolean isPinPukCommand();

    /**
     * Process a MMI code or short code...anything that isn't a dialing number
     */
    void processCode() throws CallStateException;

    /**
     * @return the Receiver for the Ussd Callback.
     */
    public ResultReceiver getUssdCallbackReceiver();

    /**
     * @return the dialString.
     */
    public String getDialString();

    Pattern sPatternCdmaMmiCodeWhileRoaming = Pattern.compile(
            "\\*(\\d{2})(\\+{0,1})(\\d{0,})");
    /*           1        2         3
           1 = service code
           2 = prefix
           3 = number
    */
    int MATCH_GROUP_CDMA_MMI_CODE_SERVICE_CODE = 1;
    int MATCH_GROUP_CDMA_MMI_CODE_NUMBER_PREFIX = 2;
    int MATCH_GROUP_CDMA_MMI_CODE_NUMBER = 3;
}

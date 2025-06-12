/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.telephony.uicc.euicc.apdu;

import android.telephony.IccOpenLogicalChannelResponse;

/**
 * The exception of failing to execute an APDU command. It can be caused by an error happening on
 * opening the basic or logical channel, or the response of the APDU command is not success
 * ({@link ApduSender#STATUS_NO_ERROR}).
 *
 * @hide
 */
public class ApduException extends Exception {
    private final int mApduStatus;

    /** Creates an exception with the apduStatus code of the response of an APDU command. */
    public ApduException(int apduStatus) {
        super();
        mApduStatus = apduStatus;
    }

    public ApduException(String message) {
        super(message);
        mApduStatus = 0;
    }

    /**
     * @return The error status of the response of an APDU command. An error status can be any
     *         positive 16-bit integer (i.e., SW1 & SW2) other than
     *         {@link ApduSender#STATUS_NO_ERROR} which means no error. For an error encountered
     *         when opening a logical channel before the APDU command gets sent, this is not the
     *         status defined in {@link IccOpenLogicalChannelResponse}. In this caes, 0 will be
     *         returned and the message of this exception will have the detailed error information.
     */
    public int getApduStatus() {
        return mApduStatus;
    }

    /** @return The hex string of the error status. */
    public String getStatusHex() {
        return Integer.toHexString(mApduStatus);
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " (apduStatus=" + getStatusHex() + ")";
    }
}

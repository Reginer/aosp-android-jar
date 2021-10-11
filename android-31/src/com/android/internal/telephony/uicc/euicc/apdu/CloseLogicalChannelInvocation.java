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

import android.os.AsyncResult;
import android.os.Message;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.euicc.async.AsyncMessageInvocation;
import com.android.telephony.Rlog;

/**
 * Invokes {@link CommandsInterface#iccCloseLogicalChannel(int, Message)}. This takes a channel id
 * (Integer) as the input and return a boolean to indicate if closing the logical channel is
 * succeeded. No exception will be returned to the result callback.
 *
 * @hide
 */
class CloseLogicalChannelInvocation extends AsyncMessageInvocation<Integer, Boolean> {
    private static final String LOG_TAG = "CloseChan";

    private final CommandsInterface mCi;

    CloseLogicalChannelInvocation(CommandsInterface ci) {
        mCi = ci;
    }

    @Override
    protected void sendRequestMessage(Integer channel, Message msg) {
        Rlog.v(LOG_TAG, "Channel: " + channel);
        mCi.iccCloseLogicalChannel(channel, msg);
    }

    @Override
    protected Boolean parseResult(AsyncResult ar) {
        if (ar.exception == null) {
            return true;
        }
        if (ar.exception instanceof CommandException) {
            Rlog.e(LOG_TAG, "CommandException", ar.exception);
        } else {
            Rlog.e(LOG_TAG, "Unknown exception", ar.exception);
        }
        return false;
    }
}

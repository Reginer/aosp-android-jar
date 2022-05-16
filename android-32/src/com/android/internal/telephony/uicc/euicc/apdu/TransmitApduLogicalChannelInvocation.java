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
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.euicc.async.AsyncMessageInvocation;
import com.android.telephony.Rlog;

/**
 * Invokes {@link CommandsInterface#iccTransmitApduLogicalChannel(int, int, int, int, int, int,
 * String, Message)}. This takes an APDU command as the input and return the response. The status of
 * returned response will be 0x6F00 if any error happens. No exception will be returned to the
 * result callback.
 *
 * @hide
 */
public class TransmitApduLogicalChannelInvocation
        extends AsyncMessageInvocation<ApduCommand, IccIoResult> {
    private static final String LOG_TAG = "TransApdu";
    private static final int SW1_ERROR = 0x6F;

    private final CommandsInterface mCi;

    TransmitApduLogicalChannelInvocation(CommandsInterface ci) {
        mCi = ci;
    }

    @Override
    protected void sendRequestMessage(ApduCommand command, Message msg) {
        Rlog.v(LOG_TAG, "Send: " + command);
        mCi.iccTransmitApduLogicalChannel(command.channel, command.cla | command.channel,
                command.ins, command.p1, command.p2, command.p3, command.cmdHex, msg);
    }

    @Override
    protected IccIoResult parseResult(AsyncResult ar) {
        IccIoResult response;
        if (ar.exception == null && ar.result != null) {
            response  = (IccIoResult) ar.result;
        } else {
            if (ar.result == null) {
                Rlog.e(LOG_TAG, "Empty response");
            } else if (ar.exception instanceof CommandException) {
                Rlog.e(LOG_TAG,  "CommandException", ar.exception);
            } else {
                Rlog.e(LOG_TAG,  "CommandException", ar.exception);
            }
            response = new IccIoResult(SW1_ERROR, 0 /* sw2 */, (byte[]) null /* payload */);
        }

        Rlog.v(LOG_TAG, "Response: " + response);
        return response;
    }
}

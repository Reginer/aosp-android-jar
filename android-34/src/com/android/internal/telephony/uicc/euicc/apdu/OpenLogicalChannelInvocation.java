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
import android.telephony.IccOpenLogicalChannelResponse;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.euicc.async.AsyncMessageInvocation;
import com.android.telephony.Rlog;

/**
 * Invokes {@link CommandsInterface#iccOpenLogicalChannel(String, int, Message)}. This takes AID
 * (String) as the input and return the response of opening a logical channel. Error will be
 * included in the {@link IccOpenLogicalChannelResponse} result and no exception will be returned to
 * the result callback.
 *
 * @hide
 */
class OpenLogicalChannelInvocation
        extends AsyncMessageInvocation<String, IccOpenLogicalChannelResponse> {
    private static final String LOG_TAG = "OpenChan";

    private final CommandsInterface mCi;

    OpenLogicalChannelInvocation(CommandsInterface ci) {
        mCi = ci;
    }

    @Override
    protected void sendRequestMessage(String aid, Message msg) {
        mCi.iccOpenLogicalChannel(aid, 0, msg);
    }

    @Override
    protected IccOpenLogicalChannelResponse parseResult(AsyncResult ar) {
        IccOpenLogicalChannelResponse openChannelResp;
        // The code below is copied from PhoneInterfaceManager.java.
        // TODO: move this code into IccOpenLogicalChannelResponse so that it can be shared.
        if (ar.exception == null && ar.result != null) {
            int[] result = (int[]) ar.result;
            int channel = result[0];
            byte[] selectResponse = null;
            if (result.length > 1) {
                selectResponse = new byte[result.length - 1];
                for (int i = 1; i < result.length; ++i) {
                    selectResponse[i - 1] = (byte) result[i];
                }
            }
            openChannelResp = new IccOpenLogicalChannelResponse(
                    channel, IccOpenLogicalChannelResponse.STATUS_NO_ERROR, selectResponse);
        } else {
            if (ar.result == null) {
                Rlog.e(LOG_TAG, "Empty response");
            }
            if (ar.exception != null) {
                Rlog.e(LOG_TAG, "Exception", ar.exception);
            }

            int errorCode = IccOpenLogicalChannelResponse.STATUS_UNKNOWN_ERROR;
            if (ar.exception instanceof CommandException) {
                CommandException.Error error =
                        ((CommandException) (ar.exception)).getCommandError();
                if (error == CommandException.Error.MISSING_RESOURCE) {
                    errorCode = IccOpenLogicalChannelResponse.STATUS_MISSING_RESOURCE;
                } else if (error == CommandException.Error.NO_SUCH_ELEMENT) {
                    errorCode = IccOpenLogicalChannelResponse.STATUS_NO_SUCH_ELEMENT;
                }
            }
            openChannelResp = new IccOpenLogicalChannelResponse(
                    IccOpenLogicalChannelResponse.INVALID_CHANNEL, errorCode, null);
        }

        Rlog.v(LOG_TAG, "Response: " + openChannelResp);
        return openChannelResp;
    }
}

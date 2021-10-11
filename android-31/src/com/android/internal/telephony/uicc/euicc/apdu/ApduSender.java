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

import android.annotation.Nullable;
import android.os.Handler;
import android.os.Looper;
import android.telephony.IccOpenLogicalChannelResponse;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultCallback;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultHelper;
import com.android.telephony.Rlog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * This class sends a list of APDU commands to an AID on a UICC. A logical channel will be opened
 * before sending and closed after all APDU commands are sent. The complete response of the last
 * APDU command will be returned. If any APDU command returns an error status (other than
 * {@link #STATUS_NO_ERROR}) or causing an exception, an {@link ApduException} will be returned
 * immediately without sending the rest of commands. This class is thread-safe.
 *
 * @hide
 */
public class ApduSender {
    private static final String LOG_TAG = "ApduSender";

    // Parameter and response used by the command to get extra responses of an APDU command.
    private static final int INS_GET_MORE_RESPONSE = 0xC0;
    private static final int SW1_MORE_RESPONSE = 0x61;

    // Status code of APDU response
    private static final int STATUS_NO_ERROR = 0x9000;
    private static final int SW1_NO_ERROR = 0x91;

    private static final int WAIT_TIME_MS = 2000;

    private static void logv(String msg) {
        Rlog.v(LOG_TAG, msg);
    }

    private static void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private final String mAid;
    private final boolean mSupportExtendedApdu;
    private final OpenLogicalChannelInvocation mOpenChannel;
    private final CloseLogicalChannelInvocation mCloseChannel;
    private final TransmitApduLogicalChannelInvocation mTransmitApdu;

    // Lock for accessing mChannelOpened. We only allow to open a single logical channel at any
    // time for an AID.
    private final Object mChannelLock = new Object();
    private boolean mChannelOpened;

    /**
     * @param aid The AID that will be used to open a logical channel to.
     */
    public ApduSender(CommandsInterface ci, String aid, boolean supportExtendedApdu) {
        mAid = aid;
        mSupportExtendedApdu = supportExtendedApdu;
        mOpenChannel = new OpenLogicalChannelInvocation(ci);
        mCloseChannel = new CloseLogicalChannelInvocation(ci);
        mTransmitApdu = new TransmitApduLogicalChannelInvocation(ci);
    }

    /**
     * Sends APDU commands.
     *
     * @param requestProvider Will be called after a logical channel is opened successfully. This is
     *     in charge of building a request with all APDU commands to be sent. This won't be called
     *     if any error happens when opening a logical channel.
     * @param resultCallback Will be called after an error or the last APDU command has been
     *     executed. The result will be the full response of the last APDU command. Error will be
     *     returned as an {@link ApduException} exception.
     * @param handler The handler that {@code requestProvider} and {@code resultCallback} will be
     *     executed on.
     */
    public void send(
            RequestProvider requestProvider,
            ApduSenderResultCallback resultCallback,
            Handler handler) {
        synchronized (mChannelLock) {
            if (mChannelOpened) {
                if (!Looper.getMainLooper().equals(Looper.myLooper())) {
                    logd("Logical channel has already been opened. Wait.");
                    try {
                        mChannelLock.wait(WAIT_TIME_MS);
                    } catch (InterruptedException e) {
                        // nothing to do
                    }
                    if (mChannelOpened) {
                        AsyncResultHelper.throwException(
                                new ApduException("The logical channel is still in use."),
                                resultCallback, handler);
                        return;
                    }
                } else {
                    AsyncResultHelper.throwException(
                            new ApduException("The logical channel is in use."),
                            resultCallback, handler);
                    return;
                }
            }
            mChannelOpened = true;
        }

        mOpenChannel.invoke(mAid, new AsyncResultCallback<IccOpenLogicalChannelResponse>() {
            @Override
            public void onResult(IccOpenLogicalChannelResponse openChannelResponse) {
                int channel = openChannelResponse.getChannel();
                int status = openChannelResponse.getStatus();
                if (channel == IccOpenLogicalChannelResponse.INVALID_CHANNEL
                        || status != IccOpenLogicalChannelResponse.STATUS_NO_ERROR) {
                    synchronized (mChannelLock) {
                        mChannelOpened = false;
                        mChannelLock.notify();
                    }
                    resultCallback.onException(
                            new ApduException("Failed to open logical channel opened for AID: "
                                    + mAid + ", with status: " + status));
                    return;
                }

                RequestBuilder builder = new RequestBuilder(channel, mSupportExtendedApdu);
                Throwable requestException = null;
                try {
                    requestProvider.buildRequest(openChannelResponse.getSelectResponse(), builder);
                } catch (Throwable e) {
                    requestException = e;
                }
                if (builder.getCommands().isEmpty() || requestException != null) {
                    // Just close the channel if we don't have commands to send or an error
                    // was encountered.
                    closeAndReturn(channel, null /* response */, requestException, resultCallback,
                            handler);
                    return;
                }
                sendCommand(builder.getCommands(), 0 /* index */, resultCallback, handler);
            }
        }, handler);
    }

    /**
     * Sends the current command and then continue to send the next one. If this is the last
     * command or any error happens, {@code resultCallback} will be called.
     *
     * @param commands All commands to be sent.
     * @param index The current command index.
     */
    private void sendCommand(
            List<ApduCommand> commands,
            int index,
            ApduSenderResultCallback resultCallback,
            Handler handler) {
        ApduCommand command = commands.get(index);
        mTransmitApdu.invoke(command, new AsyncResultCallback<IccIoResult>() {
            @Override
            public void onResult(IccIoResult response) {
                // A long response may need to be fetched by multiple following-up APDU
                // commands. Makes sure that we get the complete response.
                getCompleteResponse(command.channel, response, null /* responseBuilder */,
                        new AsyncResultCallback<IccIoResult>() {
                            @Override
                            public void onResult(IccIoResult fullResponse) {
                                logv("Full APDU response: " + fullResponse);
                                int status = (fullResponse.sw1 << 8) | fullResponse.sw2;
                                if (status != STATUS_NO_ERROR && fullResponse.sw1 != SW1_NO_ERROR) {
                                    closeAndReturn(command.channel, null /* response */,
                                            new ApduException(status), resultCallback, handler);
                                    return;
                                }

                                boolean continueSendCommand = index < commands.size() - 1
                                        // Checks intermediate APDU result except the last one
                                        && resultCallback.shouldContinueOnIntermediateResult(
                                                fullResponse);
                                if (continueSendCommand) {
                                    // Sends the next command
                                    sendCommand(commands, index + 1, resultCallback, handler);
                                } else {
                                    // Returns the result of the last command
                                    closeAndReturn(command.channel, fullResponse.payload,
                                            null /* exception */, resultCallback, handler);
                                }
                            }
                        }, handler);
            }
        }, handler);
    }

    /**
     * Gets the full response.
     *
     * @param lastResponse Will be checked to see if we need to fetch more.
     * @param responseBuilder For continuously building the full response. It should not contain the
     *     last response. If it's null, a new builder will be created.
     * @param resultCallback Error will be included in the result and no exception will be returned.
     */
    private void getCompleteResponse(
            int channel,
            IccIoResult lastResponse,
            @Nullable ByteArrayOutputStream responseBuilder,
            AsyncResultCallback<IccIoResult> resultCallback,
            Handler handler) {
        ByteArrayOutputStream resultBuilder =
                responseBuilder == null ? new ByteArrayOutputStream() : responseBuilder;
        if (lastResponse.payload != null) {
            try {
                resultBuilder.write(lastResponse.payload);
            } catch (IOException e) {
                // Should never reach here.
            }
        }
        if (lastResponse.sw1 != SW1_MORE_RESPONSE) {
            lastResponse.payload = resultBuilder.toByteArray();
            resultCallback.onResult(lastResponse);
            return;
        }

        mTransmitApdu.invoke(
                new ApduCommand(channel, 0 /* cls  */, INS_GET_MORE_RESPONSE, 0 /* p1 */,
                        0 /* p2 */, lastResponse.sw2, "" /* cmdHex */),
                new AsyncResultCallback<IccIoResult>() {
                    @Override
                    public void onResult(IccIoResult response) {
                        getCompleteResponse(
                                channel, response, resultBuilder, resultCallback, handler);
                    }
                }, handler);
    }

    /**
     * Closes the opened logical channel.
     *
     * @param response If {@code exception} is null, this will be returned to {@code resultCallback}
     *     after the channel has been closed.
     * @param exception If not null, this will be returned to {@code resultCallback} after the
     *     channel has been closed.
     */
    private void closeAndReturn(
            int channel,
            @Nullable byte[] response,
            @Nullable Throwable exception,
            ApduSenderResultCallback resultCallback,
            Handler handler) {
        mCloseChannel.invoke(channel, new AsyncResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean aBoolean) {
                synchronized (mChannelLock) {
                    mChannelOpened = false;
                    mChannelLock.notify();
                }

                if (exception == null) {
                    resultCallback.onResult(response);
                } else {
                    resultCallback.onException(exception);
                }
            }
        }, handler);
    }
}

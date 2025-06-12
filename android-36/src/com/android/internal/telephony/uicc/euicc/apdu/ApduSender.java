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
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.IccOpenLogicalChannelResponse;
import android.util.Base64;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.euicc.EuiccSession;
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
 * immediately without sending the rest of commands.
 *
 * <p>If {@link EuiccSession} indicates ongoing session(s), the behavior changes: 1) before
 * sending, check if a channel is opened already. If yes, reuse the channel and send APDU commands
 * directly. If no, open a channel before sending. 2) The channel is closed when EuiccSession
 * class ends all sessions, independent of APDU sending.
 *
 * <p>This class is thread-safe.
 *
 * @hide
 */
public class ApduSender {
    // Parameter and response used by the command to get extra responses of an APDU command.
    private static final int INS_GET_MORE_RESPONSE = 0xC0;
    private static final int SW1_MORE_RESPONSE = 0x61;

    // Status code of APDU response
    private static final int STATUS_NO_ERROR = 0x9000;
    private static final int SW1_NO_ERROR = 0x91;
    private static final int STATUS_CHANNEL_CLOSED = 0x6881; // b/359336875

    private static final int WAIT_TIME_MS = 2000;
    private static final String CHANNEL_ID_PRE = "esim-channel";
    static final String ISD_R_AID = "A0000005591010FFFFFFFF8900000100";
    private static final String CHANNEL_RESPONSE_ID_PRE = "esim-res-id";

    private final String mAid;
    private final boolean mSupportExtendedApdu;
    private final OpenLogicalChannelInvocation mOpenChannel;
    private final CloseLogicalChannelInvocation mCloseChannel;
    private final TransmitApduLogicalChannelInvocation mTransmitApdu;
    private final Context mContext;
    private final String mChannelKey;
    private final String mChannelResponseKey;
    // closeAnyOpenChannel() needs a handler for its async callbacks.
    private final Handler mHandler;
    private final String mLogTag;

    // Lock for accessing mChannelInUse. We only allow to open a single logical
    // channel at any time for an AID and to invoke one command at any time.
    // Only the thread (and its async callbacks) that sets mChannelInUse
    // can open/close/send, and update mChannelOpened.
    private final Object mChannelInUseLock = new Object();
    @GuardedBy("mChannelInUseLock")
    private boolean mChannelInUse;
    private boolean mChannelOpened;

    /**
     * @param aid The AID that will be used to open a logical channel to.
     */
    public ApduSender(Context context, int phoneId, CommandsInterface ci, String aid,
            boolean supportExtendedApdu) {
        if (!aid.equals(ISD_R_AID) && !"user".equals(Build.TYPE)) {
            throw new IllegalArgumentException("Only ISD-R AID is supported.");
        }
        mLogTag = "ApduSender-" + phoneId;
        mAid = aid;
        mContext = context;
        mSupportExtendedApdu = supportExtendedApdu;
        mOpenChannel = new OpenLogicalChannelInvocation(ci);
        mCloseChannel = new CloseLogicalChannelInvocation(ci);
        mTransmitApdu = new TransmitApduLogicalChannelInvocation(ci);
        mChannelKey = CHANNEL_ID_PRE + "_" + phoneId;
        mChannelResponseKey = CHANNEL_RESPONSE_ID_PRE + "_" + phoneId;
        mHandler = new Handler();
        mChannelInUse = false;
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
        if (!acquireChannelLock()) {
            AsyncResultHelper.throwException(
                    new ApduException("The logical channel is still in use."),
                    resultCallback,
                    handler);
            return;
        }

        boolean euiccSession = EuiccSession.get().hasSession();
        // Case 1, channel was already opened AND EuiccSession is ongoing.
        // sendCommand directly. Do not immediately close channel after sendCommand.
        // Case 2, channel was already opened AND EuiccSession is not ongoing. This means
        // EuiccSession#endSession is already called but closeAnyOpenChannel() is not
        // yet executed because of waiting to acquire lock hold by this thread.
        // sendCommand directly. Close channel immediately anyways after sendCommand.
        // Case 3, channel is not open AND EuiccSession is ongoing. Open channel
        // before sendCommand. Do not immediately close channel after sendCommand.
        // Case 4, channel is not open AND EuiccSession is not ongoing. Open channel
        // before sendCommand. Close channel immediately after sendCommand.
        if (mChannelOpened) {  // Case 1 or 2
            if (euiccSession) {
                EuiccSession.get().noteChannelOpen(this);
            }
            RequestBuilder builder = getRequestBuilderWithOpenedChannel(requestProvider,
                    !euiccSession /* closeChannelImmediately */, resultCallback, handler);
            if (builder == null) {
                return;
            }
            sendCommand(builder.getCommands(), 0 /* index */,
                    !euiccSession /* closeChannelImmediately */, resultCallback, handler);
        } else {  // Case 3 or 4
            if (euiccSession) {
                EuiccSession.get().noteChannelOpen(this);
            }
            openChannel(requestProvider,
                    !euiccSession /* closeChannelImmediately */, resultCallback, handler);
        }
    }

    private RequestBuilder getRequestBuilderWithOpenedChannel(
            RequestProvider requestProvider,
            boolean closeChannelImmediately,
            ApduSenderResultCallback resultCallback,
            Handler handler) {
        Throwable requestException = null;
        int channel =
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getInt(mChannelKey, IccOpenLogicalChannelResponse.INVALID_CHANNEL);
        String storedResponse =
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getString(mChannelResponseKey, "");
        byte[] selectResponse = Base64.decode(storedResponse, Base64.DEFAULT);
        RequestBuilder builder = new RequestBuilder(channel, mSupportExtendedApdu);
        try {
            requestProvider.buildRequest(selectResponse, builder);
        } catch (Throwable e) {
            requestException = e;
        }
        if (builder.getCommands().isEmpty() || requestException != null) {
            logd("Release as commands are empty or exception occurred");
            returnRespnseOrException(channel, closeChannelImmediately,
                    null /* response */, requestException, resultCallback, handler);
            return null;
        }
        return builder;
    }

    private void openChannel(
            RequestProvider requestProvider,
            boolean closeChannelImmediately,
            ApduSenderResultCallback resultCallback,
            Handler handler) {
        mOpenChannel.invoke(mAid, new AsyncResultCallback<IccOpenLogicalChannelResponse>() {
                    @Override
                    public void onResult(IccOpenLogicalChannelResponse openChannelResponse) {
                        int channel = openChannelResponse.getChannel();
                        int status = openChannelResponse.getStatus();
                        byte[] selectResponse = openChannelResponse.getSelectResponse();
                        if (status == IccOpenLogicalChannelResponse.STATUS_NO_SUCH_ELEMENT) {
                            channel = PreferenceManager.getDefaultSharedPreferences(mContext)
                                            .getInt(mChannelKey,
                                                    IccOpenLogicalChannelResponse.INVALID_CHANNEL);
                            if (channel != IccOpenLogicalChannelResponse.INVALID_CHANNEL) {
                                logv("Try to use already opened channel: " + channel);
                                status = IccOpenLogicalChannelResponse.STATUS_NO_ERROR;
                                String storedResponse = PreferenceManager
                                        .getDefaultSharedPreferences(mContext)
                                              .getString(mChannelResponseKey, "");
                                selectResponse = Base64.decode(storedResponse, Base64.DEFAULT);
                            }
                        }

                        if (channel == IccOpenLogicalChannelResponse.INVALID_CHANNEL
                                || status != IccOpenLogicalChannelResponse.STATUS_NO_ERROR) {
                            mChannelOpened = false;
                            returnRespnseOrException(
                                    channel,
                                    /* closeChannelImmediately= */ false,
                                    /* response= */ null,
                                    new ApduException(
                                            "Failed to open logical channel for AID: "
                                                    + mAid
                                                    + ", with status: "
                                                    + status),
                                    resultCallback,
                                    handler);
                            return;
                        }
                        PreferenceManager.getDefaultSharedPreferences(mContext)
                                .edit()
                                .putInt(mChannelKey, channel)
                                .putString(mChannelResponseKey,
                                    Base64.encodeToString(selectResponse, Base64.DEFAULT)).apply();
                        mChannelOpened = true;

                        RequestBuilder builder =
                                getRequestBuilderWithOpenedChannel(requestProvider,
                                        closeChannelImmediately, resultCallback, handler);
                        if (builder == null) {
                            return;
                        }

                        sendCommand(builder.getCommands(), 0 /* index */,
                                closeChannelImmediately, resultCallback, handler);
                    }
                },
                handler);
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
            boolean closeChannelImmediately,
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
                                if (status != STATUS_NO_ERROR
                                        && fullResponse.sw1 != SW1_NO_ERROR) {
                                    if (status == STATUS_CHANNEL_CLOSED) {
                                        // Channel is closed by EUICC e.g. REFRESH.
                                        tearDownPreferences();
                                        mChannelOpened = false;
                                        // TODO: add retry
                                    }
                                    returnRespnseOrException(
                                            command.channel,
                                            closeChannelImmediately,
                                            null /* response */,
                                            new ApduException(status),
                                            resultCallback,
                                            handler);
                                    return;
                                }

                                boolean continueSendCommand = index < commands.size() - 1
                                        // Checks intermediate APDU result except the last one
                                        && resultCallback.shouldContinueOnIntermediateResult(
                                                fullResponse);
                                if (continueSendCommand) {
                                    // Sends the next command
                                    sendCommand(commands, index + 1,
                                            closeChannelImmediately, resultCallback, handler);
                                } else {
                                    // Returns the result of the last command
                                    returnRespnseOrException(
                                            command.channel,
                                            closeChannelImmediately,
                                            fullResponse.payload,
                                            null /* exception */,
                                            resultCallback,
                                            handler);
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

    private void tearDownPreferences() {
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .edit()
                .remove(mChannelKey)
                .remove(mChannelResponseKey)
                .apply();
    }

    /**
     * Fires the {@code resultCallback} to return a response or exception. Also
     * closes the open logical channel if {@code closeChannelImmediately} is {@code true}.
     */
    private void returnRespnseOrException(
            int channel,
            boolean closeChannelImmediately,
            @Nullable byte[] response,
            @Nullable Throwable exception,
            ApduSenderResultCallback resultCallback,
            Handler handler) {
        if (closeChannelImmediately) {
            closeAndReturn(
                    channel,
                    response,
                    exception,
                    resultCallback,
                    handler);
        } else {
            releaseChannelLockAndReturn(
                    response,
                    exception,
                    resultCallback,
                    handler);
        }
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
                tearDownPreferences();
                mChannelOpened = false;
                releaseChannelLock();

                if (exception == null) {
                    resultCallback.onResult(response);
                } else {
                    resultCallback.onException(exception);
                }
            }
        }, handler);
    }

    /**
     * Cleanup the existing opened channel which remained opened earlier due
     * to:
     *
     * <p> 1) onging EuiccSession. This will be called by {@link EuiccSession#endSession()}
     * from non-main-thread. Or,
     *
     * <p> 2) telephony crash. This will be called by constructor from main-thread.
     */
    public void closeAnyOpenChannel() {
        if (!acquireChannelLock()) {
            // This cannot happen for case 2) when called by constructor
            loge("[closeAnyOpenChannel] failed to acquire channel lock");
            return;
        }
        int channelId = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getInt(mChannelKey, IccOpenLogicalChannelResponse.INVALID_CHANNEL);
        if (channelId == IccOpenLogicalChannelResponse.INVALID_CHANNEL) {
            releaseChannelLock();
            return;
        }
        logv("[closeAnyOpenChannel] closing the open channel : " +  channelId);
        mCloseChannel.invoke(channelId, new AsyncResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean isSuccess) {
                if (isSuccess) {
                    logv("[closeAnyOpenChannel] Channel closed successfully: " + channelId);
                    tearDownPreferences();
                }
                // Even if CloseChannel failed, pretend that the channel is closed.
                // So next send() will try open the channel again. If the channel is
                // indeed still open, we use the channelId saved in sharedPref.
                mChannelOpened = false;
                releaseChannelLock();
            }
        }, mHandler);
    }

    // releases channel and callback
    private void releaseChannelLockAndReturn(
            @Nullable byte[] response,
            @Nullable Throwable exception,
            ApduSenderResultCallback resultCallback,
            Handler handler) {
        handler.post(
                () -> {
                    releaseChannelLock();
                    if (exception == null) {
                        resultCallback.onResult(response);
                    } else {
                        resultCallback.onException(exception);
                    }
                });
    }

    private void releaseChannelLock() {
        synchronized (mChannelInUseLock) {
            logd("Channel lock released.");
            mChannelInUse = false;
            mChannelInUseLock.notify();
        }
    }

    /**
     * Acquires channel lock and returns {@code true} if successful.
     *
     * <p>It fails and returns {@code false} when:
     * <ul>
     *   <li>Called from main thread, and mChannelInUse=true, fails immediately.
     *   <li>Called from non main thread, and mChannelInUse=true after 2 seconds waiting, fails.
     * </ul>
     */
    private boolean acquireChannelLock() {
        synchronized (mChannelInUseLock) {
            if (mChannelInUse) {
                if (!Looper.getMainLooper().equals(Looper.myLooper())) {
                    logd("Logical channel is in use. Wait.");
                    try {
                        mChannelInUseLock.wait(WAIT_TIME_MS);
                    } catch (InterruptedException e) {
                        // nothing to do
                    }
                    if (mChannelInUse) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            mChannelInUse = true;
            logd("Channel lock acquired.");
            return true;
        }
    }

    private void logv(String msg) {
        Rlog.v(mLogTag, msg);
    }

    private void logd(String msg) {
        Rlog.d(mLogTag, msg);
    }

    private void loge(String msg) {
        Rlog.e(mLogTag, msg);
    }
}

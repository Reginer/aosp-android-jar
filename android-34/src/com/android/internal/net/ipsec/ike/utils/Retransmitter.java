/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.internal.net.ipsec.ike.utils;

import static com.android.internal.net.ipsec.ike.IkeSessionStateMachine.CMD_RETRANSMIT;

import android.os.Handler;

import com.android.internal.net.ipsec.ike.message.IkeMessage;

/**
 * Retransmitter represents a class that will send a message and trigger delayed retransmissions
 *
 * <p>The Retransmitter class will queue retransmission signals on the provided handler. The owner
 * of this retransmitter instance is expected to wait for the signal, and call retransmit() on the
 * instance of this class.
 */
public abstract class Retransmitter {
    private final Handler mHandler;
    private final IkeMessage mRetransmitMsg;
    private int mRetransmitCount = 0;
    private int[] mRetransmissionTimeouts;

    public Retransmitter(Handler handler, IkeMessage msg, int[] retransmissionTimeouts) {
        mHandler = handler;
        mRetransmitMsg = msg;
        mRetransmissionTimeouts = retransmissionTimeouts;
    }

    /**
     * Triggers a (re)transmission. Will enqueue a future retransmission signal on the given handler
     */
    public void retransmit() {
        if (mRetransmitMsg == null) {
            return;
        }

        // If the failed iteration is beyond the max attempts, clean up and shut down.
        if (mRetransmitCount >= mRetransmissionTimeouts.length) {
            handleRetransmissionFailure();
            return;
        }

        send();

        long timeout = mRetransmissionTimeouts[mRetransmitCount++];
        mHandler.sendMessageDelayed(mHandler.obtainMessage(CMD_RETRANSMIT, this), timeout);
    }

    /** Cancels any future retransmissions */
    public void stopRetransmitting() {
        mHandler.removeMessages(CMD_RETRANSMIT, this);
    }

    /** Retrieves the message this retransmitter is tracking */
    public IkeMessage getMessage() {
        return mRetransmitMsg;
    }

    /**
     * Implementation-provided sender
     *
     * <p>For Retransmitter-internal use only.
     */
    protected abstract void send();

    /**
     * Callback for implementations to be informed that we have reached the max retransmissions.
     *
     * <p>For Retransmitter-internal use only.
     */
    protected abstract void handleRetransmissionFailure();
}

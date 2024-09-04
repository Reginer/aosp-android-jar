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

    /**
     * Retransmission allowed state. Initial state when created.
     *
     * <p>This state allows packet transmission.
     */
    static final int STATE_RETRANSMISSION_ALLOWED = 1;
    /**
     * Retransmission suspended state.
     *
     * <p>This state does not allow packet transmission.
     */
    static final int STATE_RETRANSMISSION_SUSPENDED = 2;
    /**
     * Retransmission finished state.
     *
     * <p>This state does not allow packet transmission.
     */
    static final int STATE_RETRANSMISSION_FINISHED = 3;

    private final Handler mHandler;
    private final IkeMessage mRetransmitMsg;
    private int mRetransmitCount = 0;
    private int[] mRetransmissionTimeouts;

    /** State of retransmitting. */
    private int mRetransmitterState;

    public Retransmitter(Handler handler, IkeMessage msg, int[] retransmissionTimeouts) {
        mHandler = handler;
        mRetransmitMsg = msg;
        mRetransmissionTimeouts = retransmissionTimeouts;
        mRetransmitterState = STATE_RETRANSMISSION_ALLOWED;
    }

    /**
     * Triggers a (re)transmission. Will enqueue a future retransmission signal on the given handler
     *
     * <p>This method will only transmit packets when the Retransmitter is in the
     * STATE_RETRANSMISSION_ALLOWED state
     */
    public void retransmit() {
        if (mRetransmitMsg == null) {
            return;
        }
        // Packets can be transferred only when retransmission is in allowed state. Any other state
        // does not allow packet transfer.
        if (mRetransmitterState != STATE_RETRANSMISSION_ALLOWED) {
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

    /**
     * Finish retransmitting and cancels any future retransmissions.
     *
     * <p>This function is called when all packet transmissions have been completed and any future
     * next retransmission is no longer needed.
     */
    public void stopRetransmitting() {
        mHandler.removeMessages(CMD_RETRANSMIT, this);

        // Set as stopped
        mRetransmitterState = STATE_RETRANSMISSION_FINISHED;
    }

    /**
     * Suspends retransmitting.
     *
     * <p>Cancels any future retransmissions and sets the state to the Suspended. Retransmission can
     * only be resumed using the {@link #restartRetransmitting()}.
     */
    public void suspendRetransmitting() {
        // When retransmission is finished, it does not transition to the suspend state.
        if (mRetransmitterState != STATE_RETRANSMISSION_ALLOWED) {
            return;
        }

        // Remove future retransmissions.
        mHandler.removeMessages(CMD_RETRANSMIT, this);

        // Set as suspended
        mRetransmitterState = STATE_RETRANSMISSION_SUSPENDED;
    }

    /**
     * Restarts retransmitting.
     *
     * <p>Resumes a retransmission suspended by {@link #suspendRetransmitting} and retransmission
     * starts again from the beginning by resetting retransmit count.
     *
     * <p>The state finished by {@link #stopRetransmitting()} will not be restarted again.
     */
    public void restartRetransmitting() {
        // When retransmission is finished, it does not transition to the allowed state.
        if (mRetransmitterState != STATE_RETRANSMISSION_SUSPENDED) {
            return;
        }

        // Set as resumed.
        mRetransmitterState = STATE_RETRANSMISSION_ALLOWED;

        // Reset retransmission count to 0.
        mRetransmitCount = 0;

        // Resume retransmitting.
        retransmit();
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

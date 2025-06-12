/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *    * Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *    * Neither the name of The Linux Foundation nor the names of its
 *      contributors may be used to endorse or promote products derived
 *      from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.ims;

import java.util.concurrent.Executor;

/**
 * Listener for receiving notifications about changes to the IMS connection.
 * It provides a state of IMS registration between UE and IMS network, the service
 * availability of the local device during IMS registered.
 *
 * @hide
 */
public abstract class ImsEcbmStateListener {
    protected Executor mListenerExecutor = Runnable::run;
    /**
     * constructor.
     *
     * @param executor the executor that will execute callbacks.
     */
    public ImsEcbmStateListener(Executor executor) {
        if (executor != null)
            mListenerExecutor = executor;
    }
    /**
     * Called when the device enters Emergency Callback Mode
     */
    public final void onECBMEntered() {
        onECBMEntered(mListenerExecutor);
    }

    /**
     * Called when the device enters Emergency Callback Mode
     *
     * @param executor the executor that will execute callbacks.
     */
    public abstract void onECBMEntered(Executor executor);

    /**
     * Called when the device exits Emergency Callback Mode
     */
    public final void onECBMExited() {
        onECBMExited(mListenerExecutor);
    }

    /**
     * Called when the device exits Emergency Callback Mode
     *
     * @param executor the executor that will execute callbacks.
     */
    public abstract void onECBMExited(Executor executor);
}

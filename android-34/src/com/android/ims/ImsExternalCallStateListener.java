/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.ims;

import android.telephony.ims.ImsExternalCallState;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Listener for receiving notifications about {@link ImsExternalCallState} information received
 * from the network via a dialog event package.
 *
 * @hide
 */
public abstract class ImsExternalCallStateListener {
    protected Executor mListenerExecutor = Runnable::run;
    /**
     * constructor.
     *
     * @param executor the executor that will execute callbacks.
     */
    public ImsExternalCallStateListener(Executor executor) {
        if (executor != null)
            mListenerExecutor = executor;
    }
    /**
     * Notifies client when Dialog Event Package update is received
     *
     * @param externalCallState the external call state.
     */
    public final void onImsExternalCallStateUpdate(List<ImsExternalCallState> externalCallState) {
        onImsExternalCallStateUpdate(externalCallState, mListenerExecutor);
    }
    /**
     * Notifies client when Dialog Event Package update is received
     *
     * @param externalCallState the external call state.
     *
     * @param executor the executor that will execute callbacks.
     */
    public abstract void onImsExternalCallStateUpdate(
        List<ImsExternalCallState> externalCallState, Executor executor);
}

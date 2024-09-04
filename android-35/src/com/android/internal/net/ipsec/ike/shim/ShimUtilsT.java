/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.net.ipsec.ike.shim;

import static android.net.ipsec.ike.IkeManager.getIkeLog;
import static android.net.ipsec.ike.exceptions.IkeException.wrapAsIkeException;

import android.net.Network;
import android.net.ipsec.ike.exceptions.IkeException;
import android.net.ipsec.ike.exceptions.IkeIOException;
import android.net.ipsec.ike.exceptions.IkeInternalException;
import android.net.ipsec.ike.exceptions.IkeNetworkLostException;
import android.net.ipsec.ike.exceptions.IkeTimeoutException;

import com.android.internal.net.ipsec.ike.net.IkeConnectionController;

import java.io.IOException;
import java.net.UnknownHostException;

/** Shim utilities for SDK T */
public class ShimUtilsT extends ShimUtilsRAndS {
    // Package protected constructor for ShimUtils to access
    ShimUtilsT() {
        super();
    }

    @Override
    public IkeException getWrappedIkeException(Exception exception) {
        if (exception instanceof IkeException) {
            return (IkeException) exception;
        }

        if (exception instanceof IOException) {
            return new IkeIOException((IOException) exception);
        }

        return new IkeInternalException(exception);
    }

    @Override
    public Exception getRetransmissionFailedException(String errMsg) {
        return new IkeTimeoutException(errMsg);
    }

    @Override
    public IOException getDnsFailedException(String errMsg) {
        return new UnknownHostException(errMsg);
    }

    @Override
    public void onUnderlyingNetworkDiedWithoutMobility(
            IIkeSessionStateMachineShim ikeSession, Network network) {
        ikeSession.onFatalError(new IkeNetworkLostException(network));
    }

    @Override
    public void executeOrSendFatalError(Runnable r, IkeConnectionController.Callback cb) {
        try {
            r.run();
        } catch (Exception e) {
            getIkeLog().wtf("IkeConnectionController", "Unexpected exception");

            // An unexpected exception is an unrecoverable error to IKE Session. Thus fire onError
            // to notify the IkeSessionStateMachine of this fatal issue.
            cb.onError(wrapAsIkeException(e));
        }
    }
}

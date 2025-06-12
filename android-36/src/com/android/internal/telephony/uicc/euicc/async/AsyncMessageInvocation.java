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

package com.android.internal.telephony.uicc.euicc.async;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;

/**
 * This class wraps an invocation to an asynchronous method using {@link Message} to be working with
 * {@link AsyncResultCallback}. With this class, you can use callbacks instead of managing a state
 * machine to complete a task relying on multiple asynchronous method calls.
 *
 * <p>Subclasses should override the abstract methods to invoke the actual asynchronous method and
 * parse the returned result.
 *
 * @param <Request> Class of the request data.
 * @param <Response> Class of the response data.
 *
 * @hide
 */
public abstract class AsyncMessageInvocation<Request, Response> implements Handler.Callback {
    /**
     * Executes an invocation.
     *
     * @param request The request to be sent with the invocation.
     * @param resultCallback Will be called after result is returned.
     * @param handler The handler that {@code resultCallback} will be executed on.
     */
    public final void invoke(
            Request request, AsyncResultCallback<Response> resultCallback, Handler handler) {
        Handler h = new Handler(handler.getLooper(), this);
        sendRequestMessage(request, h.obtainMessage(0, resultCallback));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean handleMessage(Message msg) {
        AsyncResult result = (AsyncResult) msg.obj;
        AsyncResultCallback<Response> resultCallback =
                (AsyncResultCallback<Response>) result.userObj;
        try {
            resultCallback.onResult(parseResult(result));
        } catch (Throwable t) {
            resultCallback.onException(t);
        }
        return true;
    }

    /**
     * Calls the asynchronous method with the given {@code msg}. The implementation should convert
     * the given {@code request} to the parameters of the asynchronous method.
     */
    protected abstract void sendRequestMessage(Request request, Message msg);

    /** Parses the asynchronous result returned by the method to a {@link Response}. */
    protected abstract Response parseResult(AsyncResult result) throws Throwable;
}

/*
 * Copyright (C) 2017 The Android Open Source Project
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

import com.android.telephony.Rlog;

/**
 * Class to deliver the returned value from an asynchronous call. Either {@link #onResult(Result)}
 * or {@link #onException(Throwable)} will be called. You can create an anonymous subclass and
 * override these methods to handle the result or the throwable from an asynchronous call, for
 * example:
 *
 * <pre>
 *     doSomethingAsync(
 *             new AsyncResultCallback&lt;Result&gt;() {
 *                 void onResult(Result r) {
 *                     Log.i("Got the result: %s", r.toString());
 *                 }
 *
 *                 void onException(Throwable e) {...}
 *             });
 * <pre>
 *
 * @param <Result> The returned value of the asynchronous call.
 * @hide
 */
public abstract class AsyncResultCallback<Result> {

    private static final String LOG_TAG = "AsyncResultCallback";

    /** This will be called when the result is returned. */
    public abstract void onResult(Result result);

    /** This will be called when any exception is thrown. */
    public void onException(Throwable e) {
        Rlog.e(LOG_TAG, "Error in onException", e);
    }
}

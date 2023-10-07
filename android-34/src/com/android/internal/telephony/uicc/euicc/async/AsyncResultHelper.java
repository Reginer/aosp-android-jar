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

import android.annotation.Nullable;
import android.os.Handler;

/**
 * Helper on {@link AsyncResultCallback}.
 *
 * @hide
 */
public final class AsyncResultHelper {
    /**
     * Calls the {@code callback} to return the {@code result} object. The {@code callback} will be
     * run in the {@code handler}. If the {@code handler} is null, the callback will be called
     * immediately.
     *
     * @param <T> Result type.
     */
    public static <T> void returnResult(
            final T result, final AsyncResultCallback<T> callback, @Nullable Handler handler) {
        if (handler == null) {
            callback.onResult(result);
        } else {
            handler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(result);
                        }
                    });
        }
    }

    /**
     * Calls the {@code callback} to return the thrown {@code e} exception. The {@code callback}
     * will be run in the {@code handler}. If the {@code handler} is null, the callback will be
     * called immediately.
     */
    public static void throwException(
            final Throwable e, final AsyncResultCallback<?> callback, @Nullable Handler handler) {
        if (handler == null) {
            callback.onException(e);
        } else {
            handler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            callback.onException(e);
                        }
                    });
        }
    }

    private AsyncResultHelper() {}
}

/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.internal.telephony.data;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;

import com.android.internal.annotations.VisibleForTesting;

import java.util.concurrent.Executor;

/**
 * This is the generic callback used for passing information between telephony data modules.
 */
public class DataCallback {
    /** The executor of the callback. */
    private final @NonNull Executor mExecutor;

    /**
     * Constructor
     *
     * @param executor The executor of the callback.
     */
    public DataCallback(@NonNull @CallbackExecutor Executor executor) {
        mExecutor = executor;
    }

    /**
     * @return The executor of the callback.
     */
    @VisibleForTesting
    public @NonNull Executor getExecutor() {
        return mExecutor;
    }

    /**
     * Invoke the callback from executor.
     *
     * @param runnable The callback method to invoke.
     */
    public void invokeFromExecutor(@NonNull Runnable runnable) {
        mExecutor.execute(runnable);
    }
}

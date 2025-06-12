/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.modules.utils;

import android.annotation.NonNull;
import android.os.Handler;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * An adapter {@link Executor} that posts all executed tasks onto the given
 * {@link Handler}.
 */
public class HandlerExecutor implements Executor {
    private final Handler mHandler;

    public HandlerExecutor(@NonNull Handler handler) {
        mHandler = Objects.requireNonNull(handler);
    }

    @Override
    public void execute(Runnable command) {
        if (!mHandler.post(command)) {
            throw new RejectedExecutionException(mHandler + " is shutting down");
        }
    }
}

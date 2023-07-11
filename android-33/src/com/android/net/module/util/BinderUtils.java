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

package com.android.net.module.util;

import android.annotation.NonNull;
import android.os.Binder;

/**
 * Collection of utilities for {@link Binder} and related classes.
 * @hide
 */
public class BinderUtils {
    /**
     * Convenience method for running the provided action enclosed in
     * {@link Binder#clearCallingIdentity}/{@link Binder#restoreCallingIdentity}
     *
     * Any exception thrown by the given action will be caught and rethrown after the call to
     * {@link Binder#restoreCallingIdentity}
     *
     * Note that this is copied from Binder#withCleanCallingIdentity with minor changes
     * since it is not public.
     *
     * @hide
     */
    public static final <T extends Exception> void withCleanCallingIdentity(
            @NonNull ThrowingRunnable<T> action) throws T {
        final long callingIdentity = Binder.clearCallingIdentity();
        try {
            action.run();
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    /**
     * Like a Runnable, but declared to throw an exception.
     *
     * @param <T> The exception class which is declared to be thrown.
     */
    @FunctionalInterface
    public interface ThrowingRunnable<T extends Exception> {
        /** @see java.lang.Runnable */
        void run() throws T;
    }
}

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

package com.android.testutils;

import java.util.function.Supplier;

/**
 * A class grouping some utilities to deal with exceptions.
 */
public class ExceptionUtils {
    /**
     * Like a Consumer, but declared to throw an exception.
     * @param <T>
     */
    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        /** @see java.util.function.Consumer */
        void accept(T t) throws Exception;
    }

    /**
     * Like a Supplier, but declared to throw an exception.
     * @param <T>
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        /** @see java.util.function.Supplier */
        T get() throws Exception;
    }

    /**
     * Like a Runnable, but declared to throw an exception.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        /** @see java.lang.Runnable */
        void run() throws Exception;
    }

    /**
     * Convert a supplier that throws into one that doesn't.
     *
     * The returned supplier returns null in cases where the source throws.
     */
    public static <T> Supplier<T> ignoreExceptions(ThrowingSupplier<T> func) {
        return () -> {
            try {
                return func.get();
            } catch (Exception e) {
                return null;
            }
        };
    }

    /**
     * Convert a runnable that throws into one that doesn't.
     *
     * All exceptions are ignored by the returned Runnable.
     */
    public static Runnable ignoreExceptions(ThrowingRunnable r) {
        return () -> {
            try {
                r.run();
            } catch (Exception e) {
            }
        };
    }
}

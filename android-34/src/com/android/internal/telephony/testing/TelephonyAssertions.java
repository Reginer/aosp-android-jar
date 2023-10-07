/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.telephony.testing;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Assert;

/** Assertions used by telephony tests. */
public class TelephonyAssertions {

    /**
     * Asserts that the provided action throws the specified exception.
     *
     * <p>TODO: Replace with org.junit.Assert.assertThrows when Android upgrades to JUnit 4.12
     *
     * @return the exception that was thrown when the assertion passes.
     */
    public static <T extends Throwable> T assertThrows(
            Class<T> throwableClass, ThrowingRunnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            assertThat(t).isInstanceOf(throwableClass);
            return throwableClass.cast(t);
        }
        Assert.fail("Expected " + throwableClass.getSimpleName() + " but no exception was thrown");
        // This is unreachable but needed to compile.
        return null;
    }

    /** Runnable that can throw a checked exception. */
    public interface ThrowingRunnable {
        /** Method with code that may throw a checked exception. */
        void run() throws Exception;
    }
}

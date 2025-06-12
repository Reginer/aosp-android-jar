/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.annotation.Nullable;

/**
 * Utilities to deal with multiple SDKs in a single mainline module.
 * @hide
 */
public class SdkUtil {
    /**
     * Holder class taking advantage of erasure to avoid reflection running into class not found
     * exceptions.
     *
     * This is useful to store a reference to a class that might not be present at runtime when
     * fields are examined through reflection. An example is the MessageUtils class, which tries
     * to get all fields in a class and therefore will try to load any class for which there
     * is a member. Another example would be arguments or return values of methods in tests,
     * when the testing framework uses reflection to list methods and their arguments.
     *
     * In these cases, LateSdk<T> can be used to hide type T from reflection, since it's erased
     * and it becomes a vanilla LateSdk in Java bytecode. The T still can't be instantiated at
     * runtime of course, but runtime tests will avoid that.
     *
     * @param <T> The held type
     * @hide
     */
    public static class LateSdk<T> {
        @Nullable public final T value;
        public LateSdk(@Nullable final T value) {
            this.value = value;
        }
    }
}

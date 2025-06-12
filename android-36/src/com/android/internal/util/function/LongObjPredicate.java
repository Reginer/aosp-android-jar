/*
 * Copyright 2020 The Android Open Source Project
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
package com.android.internal.util.function;

/**
 * Represents a predicate (boolean-valued function) of a {@code long}-valued argument
 * and an object-valued argument.
 *
 * @param <T> the type of the object-valued argument to the predicate
 */
@FunctionalInterface
public interface LongObjPredicate<T> {
    /**
     * Evaluates this predicate on the given arguments.
     *
     * @param value the first input argument
     * @param t the second input argument
     * @return {@code true} if the input arguments match the predicate,
     *         otherwise {@code false}
     */
    boolean test(long value, T t);
}

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

package com.android.internal.net.utils;

/**
 * SimpleStateMachine provides a minimal, synchronous state machine framework.
 *
 * <p>This state machine is of limited functionality, but sufficient for implementation of simple
 * protocols. Due to the limited functionality, it is also easy to read and maintain.
 *
 * SimpleStateMachine defaults to the null state. Implementers should immediately transition
 * to their default state when instantiated.
 *
 * @param <T> The input message type.
 * @param <R> The result type. For SimpleStateMachines without a return value, use {@link
 *     java.lang.Void}
 */
public abstract class SimpleStateMachine<T, R> {
    protected final SimpleState mNullState =
            new SimpleState() {
                public R process(T msg) {
                    throw new IllegalStateException("Process called on null state");
                }
            };

    protected SimpleState mState = mNullState;

    // Non-static to allow for compiler verification of T, R from SimpleStateMachine
    protected abstract class SimpleState {
        public abstract R process(T msg);
    }

    /**
     * Processes the given message based on the current {@link SimpleState}
     *
     * @param msg The message to be processed by the current state
     * @return The result of the processing by the current state
     */
    public R process(T msg) {
        return mState.process(msg);
    }

    /**
     * Transitions to a new state
     *
     * @param newState The {@link SimpleState} that the {@link SimpleStateMachine} should
     *     transition to
     * @throws IllegalArgumentException if newState is null
     */
    protected void transitionTo(SimpleState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("SimpleState value must be non-null.");
        }

        mState = newState;
    }

    /**
     * Transitions to a new state, and lets the new state process the given message
     *
     * @param newState The {@link SimpleState} that the {@link SimpleStateMachine} should transition
     *     to. This state will immediately be requested to process the given message.
     * @param msg The message that should be processed by the new state
     * @return The result of the processing by the new state
     * @throws IllegalArgumentException if newState is null
     */
    protected R transitionAndProcess(SimpleState newState, T msg) {
        transitionTo(newState);
        return mState.process(msg);
    }
}

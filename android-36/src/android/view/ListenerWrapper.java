/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.view;

import android.annotation.NonNull;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * A utilty class to bundle a {@link Consumer} and an {@link Executor}
 * @param <T> the type of value to be reported.
 * @hide
 */
public class ListenerWrapper<T> {

    @NonNull
    private final Consumer<T> mConsumer;
    @NonNull
    private final Executor mExecutor;

    public ListenerWrapper(@NonNull Executor executor, @NonNull Consumer<T> consumer) {
        mExecutor = Objects.requireNonNull(executor);
        mConsumer = Objects.requireNonNull(consumer);
    }

    /**
     * Relays the new value to the {@link Consumer} using the {@link  Executor}
     */
    public void accept(@NonNull T value) {
        mExecutor.execute(() -> mConsumer.accept(value));
    }

    /**
     * Returns {@code true} if the consumer matches the one provided in the constructor,
     * {@code false} otherwise.
     */
    public boolean isConsumerSame(@NonNull Consumer<T> consumer) {
        return mConsumer.equals(consumer);
    }
}

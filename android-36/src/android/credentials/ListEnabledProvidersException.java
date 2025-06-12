/*
 * Copyright 2022 The Android Open Source Project
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

package android.credentials;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;

import com.android.internal.util.Preconditions;

/**
 * Represents an error encountered during the {@link
 * CredentialManager#listEnabledProviders(CancellationSignal Executor, OutcomeReceiver)} operation.
 *
 * @hide
 */
public class ListEnabledProvidersException extends Exception {

    @NonNull private final String mType;

    /** Returns the specific exception type. */
    @NonNull
    public String getType() {
        return mType;
    }

    /**
     * Constructs a {@link ListEnabledProvidersException}.
     *
     * @throws IllegalArgumentException If type is empty.
     */
    public ListEnabledProvidersException(@NonNull String type, @Nullable String message) {
        this(type, message, null);
    }

    /**
     * Constructs a {@link ListEnabledProvidersException}.
     *
     * @throws IllegalArgumentException If type is empty.
     */
    public ListEnabledProvidersException(
            @NonNull String type, @Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        this.mType =
                Preconditions.checkStringNotEmpty(type, "type must not be empty");
    }

    /**
     * Constructs a {@link ListEnabledProvidersException}.
     *
     * @throws IllegalArgumentException If type is empty.
     */
    public ListEnabledProvidersException(@NonNull String type, @Nullable Throwable cause) {
        this(type, null, cause);
    }

    /**
     * Constructs a {@link ListEnabledProvidersException}.
     *
     * @throws IllegalArgumentException If type is empty.
     */
    public ListEnabledProvidersException(@NonNull String type) {
        this(type, null, null);
    }
}

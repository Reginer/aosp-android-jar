/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.net.ipsec.ike.exceptions;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.IkeSessionCallback;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Objects;

/** Wrapper for I/O exceptions encountered during IKE operations. */
// Does not make sense to name it IkeIoException since "I" and "O" represent two words. The current
// name follows the convention set by Java.
@SuppressLint("AcronymName")
public final class IkeIOException extends IkeNonProtocolException {
    /**
     * Constructs a new exception with the specified cause.
     *
     * <p>Callers are not generally expected to instantiate this object themselves, except for
     * testing. A reference is passed via {@link IkeSessionCallback} or {@link
     * ChildSessionCallback}.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()}
     *     method).
     */
    public IkeIOException(@NonNull IOException cause) {
        super(Objects.requireNonNull(cause, "cause MUST NOT be null"));
    }

    /**
     * Returns the cause of this IkeIOException.
     *
     * @return the cause of this IkeIOException. It might be a subclass of IOException that
     *     represents a specific type of I/O issue. For example, {@link UnknownHostException} and
     *     {@link IkeTimeoutException}.
     */
    @Override
    @NonNull
    public IOException getCause() {
        return (IOException) super.getCause();
    }

    /** @hide */
    @Override
    public synchronized Throwable initCause(Throwable cause) {
        throw new UnsupportedOperationException("It is not allowed to set cause with this method");
    }
}

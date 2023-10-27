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
package android.net.ipsec.ike.exceptions;

import android.annotation.NonNull;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.IkeSessionCallback;

/**
 * IkeInternalException encapsulates all local implementation or resource related exceptions.
 *
 * <p>Causes may include exceptions such as {@link android.net.IpSecManager.SpiUnavailableException}
 * when the requested SPI resources failed to be allocated.
 */
public final class IkeInternalException extends IkeNonProtocolException {
    /**
     * Constructs a new exception with the specified cause.
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()}
     *     method).
     */
    public IkeInternalException(@NonNull Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with a descriptive message.
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     *
     * @param message the descriptive message (which is saved for later retrieval by the {@link
     *     #getMessage()} method).
     * @hide
     */
    public IkeInternalException(@NonNull String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     *
     * @param message the descriptive message (which is saved for later retrieval by the {@link
     *     #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()}
     *     method).
     */
    public IkeInternalException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }
}

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

package com.android.internal.net.eap.exceptions;

/**
 * EapSilentException represents a category of EAP errors that should be silently discarded by
 * authenticators and peers.
 *
 * <p>EapSilentException is so-named due to its role in the EAP standard. RFC 3748 requires that
 * several error cases be "silently discarded". In these cases, no EAP-Response message will be sent
 * to the Authenticator. However, when thrown SilentExceptions will still signal an EAP failure to
 * the IKE library calling it, resulting in an Authentication failure.
 *
 * Each type of silent error should implement its own subclass.
 */
public abstract class EapSilentException extends Exception {
    /**
     * Construct an instance of EapSilentException with the specified detail message.
     *
     * @param message the detail message.
     */
    public EapSilentException(String message) {
        super(message);
    }

    /**
     * Construct an instance of EapSilentException with the specified message and cause.
     *
     * @param message the detail message.
     * @param cause the cause.
     */
    public EapSilentException(String message, Throwable cause) {
        super(message, cause);
    }
}

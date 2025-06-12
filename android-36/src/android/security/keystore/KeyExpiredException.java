/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.security.keystore;

import java.security.InvalidKeyException;

/**
 * Indicates that a cryptographic operation failed because the employed key's validity end date
 * is in the past.
 */
public class KeyExpiredException extends InvalidKeyException {

    /**
     * Constructs a new {@code KeyExpiredException} without detail message and cause.
     */
    public KeyExpiredException() {
        super("Key expired");
    }

    /**
     * Constructs a new {@code KeyExpiredException} with the provided detail message and no cause.
     */
    public KeyExpiredException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code KeyExpiredException} with the provided detail message and cause.
     */
    public KeyExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}

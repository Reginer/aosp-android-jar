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

package com.android.internal.net.eap.exceptions.simaka;

import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtPadding;

/**
 * EapSimAkaInvalidAtPaddingException is thrown when an {@link AtPadding} with invalid padding is
 * parsed. Per RFC 4186#10.12 and RFC 4187#10.12, all padding bytes must be 0x00.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4186#section-10.12">RFC 4186, EAP-SIM, Section
 * 10.12</a>
 * @see <a href="https://tools.ietf.org/html/rfc4187#section-10.12">RFC 4187, EAP-AKA, Section
 * 10.12</a>
 */
public class EapSimAkaInvalidAtPaddingException extends EapSimAkaInvalidAttributeException {
    /**
     * Construct an instance of EapSimAkaInvalidAtPaddingException with the specified detail
     * message.
     *
     * @param message the detail message.
     */
    public EapSimAkaInvalidAtPaddingException(String message) {
        super(message);
    }

    /**
     * Construct an instance of EapSimAkaInvalidAtPaddingException with the specified message and
     * cause.
     *
     * @param message the detail message.
     * @param cause the cause.
     */
    public EapSimAkaInvalidAtPaddingException(String message, Throwable cause) {
        super(message, cause);
    }
}

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

import com.android.internal.net.eap.message.EapMessage;

/**
 * This exception is thrown when the EAP Code for an {@link EapMessage} is invalid.
 *
 * @see <a href="https://tools.ietf.org/html/rfc3748#section-4">RFC 3748, Extensible Authentication
 *     Protocol (EAP)</a>
 */
public class InvalidEapCodeException extends EapSilentException {
    /**
     * Construct an instance of InvalidEapCodeException with the specified code.
     *
     * @param code The invalid code type
     */
    public InvalidEapCodeException(int code) {
        super("Invalid Code included in EapMessage: " + code);
    }
}

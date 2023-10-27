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

import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtRandSim;

/**
 * EapSimInvalidAtRandException is thrown when an {@link AtRandSim} with an invalid number of RAND
 * values is parsed. When this error is encountered, an EAP-Response/SIM/Client-Error response must
 * be used. Note that there MUST BE 2 or 3 RAND values for the AtRandSim to be considered valid.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4186#section-10.9">RFC 4186, EAP-SIM, Section
 * 10.9</a>
 */
public class EapSimInvalidAtRandException extends EapSimAkaInvalidAttributeException {
    /**
     * Construct an instance of EapSimInvalidAtRandException with the specified detail message.
     *
     * @param message the detail message.
     */
    public EapSimInvalidAtRandException(String message) {
        super(message);
    }

    /**
     * Construct an instance of EapSimInvalidAtRandException with the specified message and
     * cause.
     *
     * @param message the detail message.
     * @param cause the cause.
     */
    public EapSimInvalidAtRandException(String message, Throwable cause) {
        super(message, cause);
    }
}

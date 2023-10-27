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

/**
 * EapAkaInvalidAuthenticationResponse is thrown when a UICC Challenge is processed during an
 * EAP-AKA session and an invalid response format is returned.
 */
public class EapAkaInvalidAuthenticationResponse extends EapSimAkaAuthenticationFailureException {
    /**
     * Construct an instance of EapAkaInvalidAuthenticationResponse with the specified detail
     * message.
     *
     * @param message the detail message.
     */
    public EapAkaInvalidAuthenticationResponse(String message) {
        super(message);
    }

    /**
     * Construct an instance of EapAkaInvalidAuthenticationResponse with the specified message and
     * cause.
     *
     * @param message the detail message.
     * @param cause the cause.
     */
    public EapAkaInvalidAuthenticationResponse(String message, Throwable cause) {
        super(message, cause);
    }
}

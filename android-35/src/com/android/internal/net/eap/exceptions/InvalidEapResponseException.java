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
 * InvalidEapResponseException is thrown when an invalid EapResponse is attempted to be constructed.
 *
 * <p>EapResponses can only be constructed from EapMessages with the EAP Code for Responses (2).
 */
public class InvalidEapResponseException extends Exception {

    /**
     * Construct an instance of InvalidEapResponseException with the specified detail message.
     *
     * @param message the detail message.
     */
    public InvalidEapResponseException(String message) {
        super(message);
    }
}

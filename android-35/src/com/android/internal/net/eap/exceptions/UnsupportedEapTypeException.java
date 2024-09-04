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

import com.android.internal.net.eap.message.EapData.EapType;
import com.android.internal.net.eap.message.EapMessage;

/**
 * UnsupportedEapTypeException is thrown when an {@link EapMessage} is constructed with an
 * unsupported {@link EapType} value.
 */
public class UnsupportedEapTypeException extends EapSilentException {
    public final int eapIdentifier;

    /**
     * Construct an instance of UnsupportedEapTypeException with the specified detail message.
     *
     * @param eapIdentifier the EAP Identifier for the message that contained the unsupported
     *                      EapType
     * @param message the detail message.
     */
    public UnsupportedEapTypeException(int eapIdentifier, String message) {
        super(message);
        this.eapIdentifier = eapIdentifier;
    }

    /**
     * Construct an instance of UnsupportedEapTypeException with the specified message and cause.
     *
     * @param eapIdentifier the EAP Identifier for the message that contained the unsupported
     *                      EapType
     * @param message the detail message.
     * @param cause the cause.
     */
    public UnsupportedEapTypeException(int eapIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.eapIdentifier = eapIdentifier;
    }
}

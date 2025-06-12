/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.internal.net.ipsec.ike.ike3gpp;

import android.net.ipsec.ike.exceptions.InvalidSyntaxException;

import java.nio.ByteBuffer;

/**
 * Ike3gppBackoffTimerUtils contains functions needed to support 3GPP-specific Backoff Timer
 * functionality.
 *
 * <p>This class is package-private.
 */
class Ike3gppBackoffTimerUtils {
    private static final int BACKOFF_TIMER_DATA_LEN = 2;
    private static final byte BACKOFF_TIMER_LEN = (byte) 1;

    /**
     * Get the backoff timer byte from the specified Notify Data.
     *
     * @see TS 124 302 Section 8.2.9.1 for specification of BACKOFF_TIMER Notify payload.
     * @param notifyData The Notify-Data payload from which the backoff timer value will be parsed.
     * @return the parsed backoff timer value
     * @throws InvalidSyntaxException if the BACKOFF_TIMER's notifyData is encoded incorrectly
     */
    static byte getBackoffTimerfromNotifyData(byte[] notifyData) throws InvalidSyntaxException {
        ByteBuffer buffer = ByteBuffer.wrap(notifyData);

        if (buffer.remaining() != BACKOFF_TIMER_DATA_LEN || buffer.get() != BACKOFF_TIMER_LEN) {
            throw new InvalidSyntaxException("BACKOFF_TIMER payload with an invalid encoding");
        }

        return buffer.get();
    }
}

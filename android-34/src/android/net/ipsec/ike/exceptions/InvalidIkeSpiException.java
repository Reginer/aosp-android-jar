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
package android.net.ipsec.ike.exceptions;

import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.IkeSessionCallback;

/**
 * This exception is thrown if an IKE message was received with an unrecognized destination SPI.
 *
 * <p>This usually indicates that the message recipient has rebooted and forgotten the existence of
 * an IKE SA.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-2.21">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @hide
 */
public final class InvalidIkeSpiException extends IkeProtocolException {
    private static final int EXPECTED_ERROR_DATA_LEN = 0;

    /**
     * Construct an instance of InvalidIkeSpiException.
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     */
    public InvalidIkeSpiException() {
        super(ERROR_TYPE_INVALID_IKE_SPI);
    }

    /**
     * Construct a instance of InvalidIkeSpiException from a notify payload.
     *
     * @param notifyData the notify data included in the payload.
     * @hide
     */
    public InvalidIkeSpiException(byte[] notifyData) {
        super(ERROR_TYPE_INVALID_IKE_SPI, notifyData);
    }

    /** @hide */
    @Override
    protected boolean isValidDataLength(int dataLen) {
        return EXPECTED_ERROR_DATA_LEN == dataLen;
    }
}

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
 * This exception is thrown if the remote server is unwilling to accept any more Child SAs.
 *
 * <p>Some minimal implementations may only accept a single Child SA setup in the context of an
 * initial IKE exchange and reject any subsequent attempts to add more.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-1.3">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @hide
 */
public final class NoAdditionalSasException extends IkeProtocolException {
    private static final int EXPECTED_ERROR_DATA_LEN = 0;

    /**
     * Construct an instance of NoAdditionalSasException.
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     */
    public NoAdditionalSasException() {
        super(ERROR_TYPE_NO_ADDITIONAL_SAS);
    }

    /**
     * Construct a instance of NoAdditionalSasException from a notify payload.
     *
     * @param notifyData the notify data included in the payload.
     * @hide
     */
    public NoAdditionalSasException(byte[] notifyData) {
        super(ERROR_TYPE_NO_ADDITIONAL_SAS, notifyData);
    }

    /** @hide */
    @Override
    protected boolean isValidDataLength(int dataLen) {
        return EXPECTED_ERROR_DATA_LEN == dataLen;
    }
}

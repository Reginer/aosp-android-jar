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
package android.net.ipsec.ike.exceptions;

import android.annotation.NonNull;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.IkeSessionCallback;

import java.util.Objects;

/**
 * This exception represents an unrecognized error notification in a received response.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.10.1">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @hide
 */
// When receiving an unrecognized error notification in a response, IKE Session MUST assume that
// the corresponding request has failed entirely. If it is in a request, IKE Session MUST ignore it.
public final class UnrecognizedIkeProtocolException extends IkeProtocolException {
    private final byte[] mUnrecognizedErrorData;
    /**
     * Constructs an instance of UnrecognizedIkeProtocolException
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     *
     * @param errorType the error type
     * @param errorData the error data in bytes
     */
    public UnrecognizedIkeProtocolException(int errorType, @NonNull byte[] errorData) {
        super(errorType, errorData);
        Objects.requireNonNull(errorData, "errorData is null");
        mUnrecognizedErrorData = errorData.clone();
    }

    /** Returns the included error data of this UnrecognizedIkeProtocolException */
    @NonNull
    public byte[] getUnrecognizedErrorData() {
        return mUnrecognizedErrorData;
    }

    /** @hide */
    @Override
    protected boolean isValidDataLength(int dataLen) {
        // Unrecognized error does not have an expected error data length. Any non-negative length
        // is valid
        return dataLen >= 0;
    }
}

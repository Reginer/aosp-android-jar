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
 * This exception is thrown if the remote server requires a single pair of addresses as selectors.
 *
 * <p>This exception indicates that the remote server is only willing to accept Traffic Selectors
 * specifying a single pair of addresses. Callers may retry Child creation with only the specific
 * traffic it is trying to forward.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-2.9">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @hide
 */
public class SinglePairRequiredException extends IkeProtocolException {
    private static final int EXPECTED_ERROR_DATA_LEN = 0;

    /**
     * Construct an instance of SinglePairRequiredException.
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     */
    public SinglePairRequiredException() {
        super(ERROR_TYPE_SINGLE_PAIR_REQUIRED);
    }

    /**
     * Construct a instance of SinglePairRequiredException from a notify payload.
     *
     * @param notifyData the notify data included in the payload.
     * @hide
     */
    public SinglePairRequiredException(byte[] notifyData) {
        super(ERROR_TYPE_SINGLE_PAIR_REQUIRED, notifyData);
    }

    /** @hide */
    @Override
    protected boolean isValidDataLength(int dataLen) {
        return EXPECTED_ERROR_DATA_LEN == dataLen;
    }
}

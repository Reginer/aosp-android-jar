/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.IkeSessionCallback;

/**
 * This exception is thrown when major version of an inbound message is higher than 2.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-2.5">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
// Include INVALID_MAJOR_VERSION Notify payload in an unencrypted response message containing
// version number 2.
public final class InvalidMajorVersionException extends IkeProtocolException {
    private static final int EXPECTED_ERROR_DATA_LEN = 1;

    private final byte mVersion;

    /**
     * Construct a instance of InvalidMajorVersionException
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     *
     * @param version the major version in received packet
     */
    // NoByteOrShort: using byte to be consistent with the Major Version specification
    public InvalidMajorVersionException(@SuppressLint("NoByteOrShort") byte version) {
        super(ERROR_TYPE_INVALID_MAJOR_VERSION, new byte[] {version});
        mVersion = version;
    }

    /**
     * Construct a instance of InvalidMajorVersionException from a notify payload.
     *
     * @param notifyData the notify data included in the payload.
     * @hide
     */
    public InvalidMajorVersionException(byte[] notifyData) {
        super(ERROR_TYPE_INVALID_MAJOR_VERSION, notifyData);
        mVersion = notifyData[0];
    }

    /**
     * Return the major version included in this exception.
     *
     * @return the major version
     */
    // NoByteOrShort: using byte to be consistent with the Major Version specification
    @SuppressLint("NoByteOrShort")
    public byte getMajorVersion() {
        return mVersion;
    }

    /** @hide */
    @Override
    protected boolean isValidDataLength(int dataLen) {
        return EXPECTED_ERROR_DATA_LEN == dataLen;
    }
}

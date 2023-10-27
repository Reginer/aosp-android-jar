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
 * This exception is thrown if the remote server received a request for a nonexistent Child SA.
 *
 * <p>This exception is usually caused by a request collision. IKE library will handle it internally
 * by deleting the Child SA (if it still exists).
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-2.25">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @hide
 */
public final class ChildSaNotFoundException extends IkeProtocolException {
    private static final int EXPECTED_ERROR_DATA_LEN = 0;

    private final int mIpSecSpi;

    /**
     * Construct an instance of ChildSaNotFoundException.
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     *
     * @param spi the SPI of the Child SA (IPsec SA) that does not exist.
     */
    public ChildSaNotFoundException(int spi) {
        super(ERROR_TYPE_CHILD_SA_NOT_FOUND);
        mIpSecSpi = spi;
    }

    /**
     * Construct a instance of ChildSaNotFoundException from a notify payload.
     *
     * @param spi the SPI of the Child SA (IPsec SA) that does not exist.
     * @param notifyData the notify data included in the payload.
     * @hide
     */
    public ChildSaNotFoundException(int spi, byte[] notifyData) {
        super(ERROR_TYPE_CHILD_SA_NOT_FOUND, notifyData);
        mIpSecSpi = spi;
    }

    /** @hide */
    @Override
    protected boolean isValidDataLength(int dataLen) {
        return EXPECTED_ERROR_DATA_LEN == dataLen;
    }

    /** Returns the SPI of the Child SA (IPsec SA) that does not exist. */
    public int getIpSecSpi() {
        return mIpSecSpi;
    }
}

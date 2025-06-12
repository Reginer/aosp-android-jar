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

package com.android.internal.net.ipsec.ike.message;

import java.nio.ByteBuffer;

/**
 * IkeUnsupportedPayload represents anunsupported payload.
 *
 * <p>This special payload is only created in decoding process.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-2.5">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
final class IkeUnsupportedPayload extends IkePayload {
    /**
     * Construct an instance of IkeSaPayload in the context of IkePayloadFactory.
     *
     * @param payload indicates the current payload type
     * @param critical indicates if it is a critical payload.
     */
    IkeUnsupportedPayload(int payload, boolean critical) {
        super(payload, critical);
    }

    /**
     * Throw an Exception when trying to encode this payload.
     *
     * @throws UnsupportedOperationException for this payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        throw new UnsupportedOperationException(
                "It is not supported to encode a " + getTypeString());
    }

    /**
     * Throw an Exception when trying to get payload length
     *
     * @throws UnsupportedOperationException for this payload.
     */
    @Override
    protected int getPayloadLength() {
        throw new UnsupportedOperationException(
                "It is not supported to get payload length of " + getTypeString());
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        return String.valueOf(payloadType);
    }
}

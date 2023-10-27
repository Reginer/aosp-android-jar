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
 * IkeVendorPayload represents a Vendor ID payload
 *
 * <p>Vendor ID allows a vendor to experiment with new features. This implementation doesn't have
 * support for any specific Vendor IDs.
 *
 * <p>Critical bit must be ignored when doing decoding.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#page-105">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeVendorPayload extends IkePayload {
    public final byte[] vendorId;

    /**
     * Construct an instance of IkeVendorPayload in the context of {@link IkePayloadFactory}.
     *
     * @param critical indicates if it is a critical payload.
     * @param payloadBody the vendor ID.
     */
    IkeVendorPayload(boolean critical, byte[] payloadBody) {
        super(PAYLOAD_TYPE_VENDOR, critical);
        vendorId = payloadBody;
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
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        return GENERIC_HEADER_LENGTH + vendorId.length;
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        return "Vendor";
    }
}

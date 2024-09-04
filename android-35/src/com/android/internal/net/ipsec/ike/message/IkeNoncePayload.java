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

import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;

import com.android.internal.net.ipsec.ike.utils.RandomnessFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * IkeNoncePayload represents a Nonce payload.
 *
 * <p>Length of nonce data must be at least half the key size of negotiated PRF. It must be between
 * 16 and 256 octets. IKE library always generates nonce of GENERATED_NONCE_LEN octets which is long
 * enough for all currently known PRFs.
 *
 * <p>Critical bit must be ignored when doing decoding and must not be set when doing encoding for
 * this payload.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296">RFC 7296, Internet Key Exchange Protocol
 *     Version 2 (IKEv2).
 */
public final class IkeNoncePayload extends IkePayload {
    // The longest key size of all currently known PRFs is 512 bits (64 bytes). Since we are
    // required to generate nonce that is long enough for all proposed PRFs, it is simple that we
    // always generate 32 bytes nonce, which is enough for all known PRFs.
    private static final int GENERATED_NONCE_LEN = 32;

    private static final int MIN_NONCE_LEN = 16;
    private static final int MAX_NONCE_LEN = 256;

    public final byte[] nonceData;

    /**
     * Construct an instance of IkeNoncePayload in the context of {@link IkePayloadFactory}.
     *
     * @param critical indicates if it is a critical payload.
     * @param payloadBody the nonce data
     */
    IkeNoncePayload(boolean critical, byte[] payloadBody) throws IkeProtocolException {
        super(PAYLOAD_TYPE_NONCE, critical);
        if (payloadBody.length < MIN_NONCE_LEN || payloadBody.length > MAX_NONCE_LEN) {
            throw new InvalidSyntaxException(
                    "Invalid nonce data with length of: " + payloadBody.length);
        }
        // Check that the length of payloadBody satisfies the "half the key size of negotiated PRF"
        // condition when processing IKE Message in upper layer. Cannot do this check here for
        // lacking PRF information.
        nonceData = payloadBody;
    }

    /** Generate Nonce data and construct an instance of IkeNoncePayload. */
    public IkeNoncePayload(RandomnessFactory randomnessFactory) {
        super(PAYLOAD_TYPE_NONCE, false);
        nonceData = new byte[GENERATED_NONCE_LEN];

        SecureRandom random = randomnessFactory.getRandom();
        random = random == null ? new SecureRandom() : random;
        random.nextBytes(nonceData);
    }

    /**
     * Encode Nonce payload to ByteBuffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param byteBuffer destination ByteBuffer that stores encoded payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        int payloadLength = GENERIC_HEADER_LENGTH + nonceData.length;

        encodePayloadHeaderToByteBuffer(nextPayload, payloadLength, byteBuffer);
        byteBuffer.put(nonceData);
    }

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        return GENERIC_HEADER_LENGTH + nonceData.length;
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        return "Nonce";
    }
}

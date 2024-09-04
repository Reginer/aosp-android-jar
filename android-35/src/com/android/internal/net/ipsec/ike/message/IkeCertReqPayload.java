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

package com.android.internal.net.ipsec.ike.message;

import static com.android.internal.net.ipsec.ike.message.IkeCertPayload.CERT_ENCODING_LEN;
import static com.android.internal.net.ipsec.ike.message.IkeCertPayload.CertificateEncoding;

import android.net.ipsec.ike.exceptions.IkeProtocolException;

import java.nio.ByteBuffer;

/**
 * This class represents a Certificate Request Payload
 *
 * <p>A Certificate Request Payload provides suggestion for an end certificate to select. Receiver
 * of this payload is allowed to send an alternate. It is possible that there is a preferred CA sent
 * in the IkeCertReqPayload, but an alternate is still acceptable.
 *
 * <p>IKE library will always ignore this payload since only one end certificate can be configured
 * by users.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.8">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public class IkeCertReqPayload extends IkePayload {
    /** Certificate encoding type */
    @CertificateEncoding public final int certEncodingType;
    /** Concatenated list of SHA-1 hashes of CAs' Subject Public Key Info */
    public final byte[] caSubjectPublicKeyInforHashes;

    /**
     * Construct an instance of IkeCertReqPayload from decoding an inbound IKE packet.
     *
     * <p>NegativeArraySizeException and BufferUnderflowException will be caught in {@link
     * IkeMessage}
     *
     * @param critical indicates if this payload is critical. Ignored in supported payload as
     *     instructed by the RFC 7296.
     * @param payloadBody payload body in byte array
     * @throws IkeProtocolException if there is any error
     */
    public IkeCertReqPayload(boolean critical, byte[] payloadBody) throws IkeProtocolException {
        super(PAYLOAD_TYPE_CERT_REQUEST, critical);

        ByteBuffer inputBuffer = ByteBuffer.wrap(payloadBody);
        certEncodingType = Byte.toUnsignedInt(inputBuffer.get());
        caSubjectPublicKeyInforHashes = new byte[inputBuffer.remaining()];
        inputBuffer.get(caSubjectPublicKeyInforHashes);
    }

    /**
     * Encode Certificate Request Payload to ByteBuffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param byteBuffer destination ByteBuffer that stores encoded payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        encodePayloadHeaderToByteBuffer(nextPayload, getPayloadLength(), byteBuffer);

        byteBuffer.put((byte) certEncodingType).put(caSubjectPublicKeyInforHashes);
    }

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        return GENERIC_HEADER_LENGTH + CERT_ENCODING_LEN + caSubjectPublicKeyInforHashes.length;
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        return "CertReq";
    }
}

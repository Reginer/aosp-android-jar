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

import android.net.ipsec.ike.IkeDerAsn1DnIdentification;
import android.net.ipsec.ike.IkeFqdnIdentification;
import android.net.ipsec.ike.IkeIdentification;
import android.net.ipsec.ike.IkeIpv4AddrIdentification;
import android.net.ipsec.ike.IkeIpv6AddrIdentification;
import android.net.ipsec.ike.IkeKeyIdIdentification;
import android.net.ipsec.ike.IkeRfc822AddrIdentification;
import android.net.ipsec.ike.exceptions.AuthenticationFailedException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;

import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;

/**
 * IkeIdPayload represents an Identification Initiator Payload or an Identification Responder
 * Payload.
 *
 * <p>Identification Initiator Payload and Identification Responder Payload have same format but
 * different payload type.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.5">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeIdPayload extends IkePayload {
    // Length of ID Payload header in octets.
    private static final int ID_HEADER_LEN = 4;
    // Length of reserved field in octets.
    private static final int ID_HEADER_RESERVED_LEN = 3;

    public final IkeIdentification ikeId;

    /**
     * Construct IkeIdPayload for received IKE packet in the context of {@link IkePayloadFactory}.
     *
     * @param critical indicates if it is a critical payload.
     * @param payloadBody payload body in byte array.
     * @param isInitiator indicates whether this payload contains the ID of IKE initiator or IKE
     *     responder.
     * @throws IkeProtocolException for decoding error.
     */
    IkeIdPayload(boolean critical, byte[] payloadBody, boolean isInitiator)
            throws IkeProtocolException {
        super((isInitiator ? PAYLOAD_TYPE_ID_INITIATOR : PAYLOAD_TYPE_ID_RESPONDER), critical);
        // TODO: b/119791832 Add helper method for checking payload body length in superclass.
        if (payloadBody.length <= ID_HEADER_LEN) {
            throw new InvalidSyntaxException(getTypeString() + " is too short.");
        }

        ByteBuffer inputBuffer = ByteBuffer.wrap(payloadBody);
        int idType = Byte.toUnsignedInt(inputBuffer.get());

        // Skip reserved field
        inputBuffer.get(new byte[ID_HEADER_RESERVED_LEN]);

        byte[] idData = new byte[payloadBody.length - ID_HEADER_LEN];
        inputBuffer.get(idData);

        switch (idType) {
            case IkeIdentification.ID_TYPE_IPV4_ADDR:
                ikeId = new IkeIpv4AddrIdentification(idData);
                return;
            case IkeIdentification.ID_TYPE_FQDN:
                ikeId = new IkeFqdnIdentification(idData);
                return;
            case IkeIdentification.ID_TYPE_RFC822_ADDR:
                ikeId = new IkeRfc822AddrIdentification(idData);
                return;
            case IkeIdentification.ID_TYPE_IPV6_ADDR:
                ikeId = new IkeIpv6AddrIdentification(idData);
                return;
            case IkeIdentification.ID_TYPE_DER_ASN1_DN:
                ikeId = new IkeDerAsn1DnIdentification(idData);
                return;
            case IkeIdentification.ID_TYPE_KEY_ID:
                ikeId = new IkeKeyIdIdentification(idData);
                return;
            default:
                throw new AuthenticationFailedException("Unsupported ID type: " + idType);
        }
    }

    /**
     * Construct IkeIdPayload for an outbound IKE packet.
     *
     * @param isInitiator indicates whether this payload contains the ID of IKE initiator or IKE
     *     responder.
     * @param ikeId the IkeIdentification.
     */
    public IkeIdPayload(boolean isInitiator, IkeIdentification ikeId) {
        super((isInitiator ? PAYLOAD_TYPE_ID_INITIATOR : PAYLOAD_TYPE_ID_RESPONDER), false);
        this.ikeId = ikeId;
    }

    /**
     * Get encoded ID payload body for building or validating an Auth Payload.
     *
     * @return the byte array of encoded ID payload body.
     */
    public byte[] getEncodedPayloadBody() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(getPayloadLength() - GENERIC_HEADER_LENGTH);

        byteBuffer
                .put((byte) ikeId.idType)
                .put(new byte[ID_HEADER_RESERVED_LEN])
                .put(ikeId.getEncodedIdData());
        return byteBuffer.array();
    }

    /** Validate if the end certificate matches the ID */
    public void validateEndCertIdOrThrow(X509Certificate endCert)
            throws AuthenticationFailedException {
        ikeId.validateEndCertIdOrThrow(endCert);
    }

    /**
     * Encode Identification Payload to ByteBuffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param byteBuffer destination ByteBuffer that stores encoded payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        encodePayloadHeaderToByteBuffer(nextPayload, getPayloadLength(), byteBuffer);
        byteBuffer.put(getEncodedPayloadBody());
    }

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        return GENERIC_HEADER_LENGTH + ID_HEADER_LEN + ikeId.getEncodedIdData().length;
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        switch (payloadType) {
            case PAYLOAD_TYPE_ID_INITIATOR:
                return "IDi";
            case PAYLOAD_TYPE_ID_RESPONDER:
                return "IDr";
            default:
                // Won't reach here.
                throw new IllegalArgumentException(
                        "Invalid Payload Type for Identification Payload.");
        }
    }
}

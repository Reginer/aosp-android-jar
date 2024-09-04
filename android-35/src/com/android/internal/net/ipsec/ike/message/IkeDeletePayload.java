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

package com.android.internal.net.ipsec.ike.message;

import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;

import java.nio.ByteBuffer;

/**
 * IkeDeletePayload represents a Delete Payload.
 *
 * <p>As instructed in RFC 7296, deletion of the IKE SA is indicated by a protocol ID of 1 (IKE) but
 * no SPIs. Deletion of a Child SA will contain the IPsec protocol ID and SPIs of inbound IPsec
 * packets. Since IKE library only supports negotiating Child SA using ESP, only the protocol ID of
 * 3 (ESP) is used for deleting Child SA.
 *
 * The possible request/response pairs for deletion are as follows:
 * - IKE SA deletion:
 *     Incoming: INFORMATIONAL(DELETE(PROTO_IKE))
 *     Outgoing: INFORMATIONAL()
 *
 * - ESP SA deletion:
 *     Incoming: INFORMATIONAL(DELETE(PROTO_ESP, SPI_A_OUT))
 *     Outgoing: INFORMATIONAL(DELETE(PROTO_ESP, SPI_A_IN))
 *
 * - ESP SA simultaneous deletion:
 *     Outgoing: INFORMATIONAL(DELETE(PROTO_ESP, SPI_A_IN))
 *     Incoming: INFORMATIONAL(DELETE(PROTO_ESP, SPI_A_OUT))
 *     Outgoing: INFORMATIONAL() // Notice DELETE payload omitted
 *
 * - ESP SA simultaneous multi-deletion:
 *     Outgoing: INFORMATIONAL(DELETE(PROTO_ESP, SPI_A_IN))
 *     Incoming: INFORMATIONAL(DELETE(PROTO_ESP, SPI_A_OUT, SPI_B_OUT))
 *     Outgoing: INFORMATIONAL(DELETE(PROTO_ESP, SPI_B_IN)) // Notice SPI_A_OUT omitted
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.11">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeDeletePayload extends IkeInformationalPayload {
    private static final int DELETE_HEADER_LEN = 4;

    @ProtocolId public final int protocolId;
    public final byte spiSize;
    public final int numSpi;
    public final int[] spisToDelete;

    /**
     * Construct an instance of IkeDeletePayload from decoding inbound IKE packet.
     *
     * <p>NegativeArraySizeException and BufferUnderflowException will be caught in {@link
     * IkeMessage}
     *
     * @param critical indicates if this payload is critical. Ignored in supported payload as
     *     instructed by the RFC 7296.
     * @param payloadBody payload body in byte array
     * @throws IkeProtocolException if there is any error
     */
    IkeDeletePayload(boolean critical, byte[] payloadBody) throws IkeProtocolException {
        super(PAYLOAD_TYPE_DELETE, critical);

        ByteBuffer inputBuffer = ByteBuffer.wrap(payloadBody);

        protocolId = Byte.toUnsignedInt(inputBuffer.get());
        spiSize = inputBuffer.get();
        numSpi = Short.toUnsignedInt(inputBuffer.getShort());
        spisToDelete = new int[numSpi];

        switch (protocolId) {
            case PROTOCOL_ID_IKE:
                // Delete payload for IKE SA must not include SPI.
                if (spiSize != SPI_LEN_NOT_INCLUDED
                        || numSpi != 0
                        || inputBuffer.remaining() != 0) {
                    throw new InvalidSyntaxException("Invalid Delete IKE Payload.");
                }
                break;
            case PROTOCOL_ID_ESP:
                // Delete payload for Child SA must include SPI
                if (spiSize != SPI_LEN_IPSEC
                        || numSpi == 0
                        || inputBuffer.remaining() != SPI_LEN_IPSEC * numSpi) {
                    throw new InvalidSyntaxException("Invalid Delete Child Payload.");
                }

                for (int i = 0; i < numSpi; i++) {
                    spisToDelete[i] = inputBuffer.getInt();
                }
                break;
            default:
                throw new InvalidSyntaxException("Unrecognized protocol in Delete Payload.");
        }
    }

    /**
     * Constructor for an outbound IKE SA deletion payload.
     *
     * <p>This constructor takes no SPI, as IKE SAs are deleted by sending a delete payload within
     * the negotiated session. As such, the SPIs are shared state that does not need to be sent.
     */
    public IkeDeletePayload() {
        super(PAYLOAD_TYPE_DELETE, false);
        protocolId = PROTOCOL_ID_IKE;
        spiSize = SPI_LEN_NOT_INCLUDED;
        numSpi = 0;
        spisToDelete = new int[0];
    }

    /**
     * Constructor for an outbound Child SA deletion payload.
     *
     * @param spis array of SPIs of Child SAs to delete. Must contain at least one SPI.
     */
    public IkeDeletePayload(int[] spis) {
        super(PAYLOAD_TYPE_DELETE, false);

        if (spis == null || spis.length < 1) {
            throw new IllegalArgumentException("No SPIs provided");
        }

        protocolId = PROTOCOL_ID_ESP;
        spiSize = SPI_LEN_IPSEC;
        numSpi = spis.length;
        spisToDelete = spis;
    }

    /**
     * Encode Delete Payload to ByteBuffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param byteBuffer destination ByteBuffer that stores encoded payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        encodePayloadHeaderToByteBuffer(nextPayload, getPayloadLength(), byteBuffer);
        byteBuffer.put((byte) protocolId).put(spiSize).putShort((short) numSpi);

        for (int toDelete : spisToDelete) {
            byteBuffer.putInt(toDelete);
        }
    }

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        return GENERIC_HEADER_LENGTH + DELETE_HEADER_LEN + spisToDelete.length * spiSize;
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        return "Del";
    }
}

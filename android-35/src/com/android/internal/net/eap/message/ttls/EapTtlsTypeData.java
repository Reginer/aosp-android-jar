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

package com.android.internal.net.eap.message.ttls;

import static com.android.internal.net.eap.EapAuthenticator.LOG;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.exceptions.ttls.EapTtlsParsingException;
import com.android.internal.net.eap.message.EapMessage;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * EapTtlsTypeData represents the type data for an {@link EapMessage} during an EAP-TTLS session.
 * The structure of the flag byte is as follows:
 *
 * <pre>
 * |---+---+---+---+---+-------+
 * | 0 | 1 | 2 | 3 | 4 | 5 6 7 |
 * | L | M | S | R | R |   V   |
 * |---+---+---+---+---+-------+
 * L = Message length is included
 * M = More fragments incoming
 * S = Start
 * R = Reserved
 * V = Version
 * </pre>
 *
 * @see <a href="https://tools.ietf.org/html/rfc5281">RFC 5281, Extensible Authentication Protocol
 *     Tunneled Transport Layer Security Authenticated Protocol Version 0 (EAP-TTLSv0)</a>
 */
public class EapTtlsTypeData {
    private static final String TAG = EapTtlsTypeData.class.getSimpleName();

    /*
     * Used to extract bits from the flag byte as well as set them. Flag defined via:
     * https://tools.ietf.org/html/rfc5281#section-9.1
     * Note that unlike the flag diagram, the length included field is treated as the
     * most significant bit
     */
    private static final int FLAG_LENGTH_INCLUDED = 1 << 7;
    private static final int FLAG_PACKET_FRAGMENTED = 1 << 6;
    private static final int FLAG_START = 1 << 5;
    // used to extract the lower 3 bits from the flag byte
    private static final int FLAG_VERSION_MASK = 0x07;

    private static final int FLAGS_LEN_BYTES = 1;
    private static final int MESSAGE_LENGTH_LEN_BYTES = 4;

    private static final int SUPPORTED_EAP_TTLS_VERSION = 0;
    private static final int LEN_NOT_INCLUDED = 0;

    public final boolean isLengthIncluded;
    public final boolean isStart;
    public final boolean isDataFragmented;
    public final int version;
    public final int messageLength;
    public byte[] data;

    // Package-private
    EapTtlsTypeData(ByteBuffer buffer) throws EapTtlsParsingException {
        byte flags = buffer.get();
        isLengthIncluded = (flags & FLAG_LENGTH_INCLUDED) != 0;
        isDataFragmented = (flags & FLAG_PACKET_FRAGMENTED) != 0;
        isStart = (flags & FLAG_START) != 0;
        version = (flags & FLAG_VERSION_MASK);

        messageLength = isLengthIncluded ? buffer.getInt() : 0;
        data = new byte[buffer.remaining()];
        buffer.get(data);

        if (!isDataFragmented && isLengthIncluded && data.length != messageLength) {
            throw new EapTtlsParsingException(
                    "Received an unfragmented packet with message length not equal to payload");
        }
    }

    private EapTtlsTypeData(
            boolean isDataFragmented, boolean isStart, int version, int messageLength, byte[] data)
            throws EapTtlsParsingException {
        this.isLengthIncluded = messageLength != LEN_NOT_INCLUDED;
        this.isDataFragmented = isDataFragmented;
        this.isStart = isStart;
        if (version != SUPPORTED_EAP_TTLS_VERSION) {
            throw new EapTtlsParsingException("Unsupported version number: " + version);
        }
        this.version = version;
        this.messageLength = messageLength;
        this.data = data;

        if (!isDataFragmented && isLengthIncluded && data.length != messageLength) {
            throw new EapTtlsParsingException(
                    "Received an unfragmented packet with message length not equal to payload");
        }
    }
    /**
     * Assembles each bit from the flag byte into a byte
     *
     * @return a byte that compromises the EAP-TTLS flags
     */
    private byte getFlagByte() {
        return (byte)
                ((isLengthIncluded ? FLAG_LENGTH_INCLUDED : 0)
                        | (isDataFragmented ? FLAG_PACKET_FRAGMENTED : 0)
                        | (isStart ? FLAG_START : 0)
                        | (version));
    }

    /**
     * Determines if the type data represents an acknowledgment packet (RFC5281#9.2.3)
     *
     * @return true if it is an ack
     */
    public boolean isAcknowledgmentPacket() {
        return data.length == 0 && !isStart && !isLengthIncluded && !isDataFragmented;
    }

    /**
     * Constructs and returns new EAP-TTLS response type data.
     *
     * @param packetFragmented a boolean that indicates whether this is a fragmented message
     * @param start indicates if the start bit should be set
     * @param version the EAP-TTLS version number
     * @param messageLength an optional field to indicate the raw length of the data field prior to
     *     fragmentation
     * @param data the raw tls message sequence
     * @return an EapTtlsTypeData or null if the packet configuration is invalid
     */
    public static EapTtlsTypeData getEapTtlsTypeData(
            boolean packetFragmented, boolean start, int version, int messageLength, byte[] data) {
        try {
            return new EapTtlsTypeData(packetFragmented, start, version, messageLength, data);
        } catch (EapTtlsParsingException e) {
            LOG.e(TAG, "Parsing exception thrown while attempting to create an EapTtlsTypeData");
            return null;
        }
    }

    /**
     * Encodes this EapTtlsTypeData instance as a byte[].
     *
     * @return byte[] representing the encoded value of this EapTtlsTypeData instance
     */
    public byte[] encode() {
        int msgLen = isLengthIncluded ? MESSAGE_LENGTH_LEN_BYTES : 0;
        int bufferSize = data.length + FLAGS_LEN_BYTES + msgLen;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.put(getFlagByte());
        if (isLengthIncluded) {
            buffer.putInt(messageLength);
        }
        buffer.put(data);
        return buffer.array();
    }

    /** EapTtlsAcknowledgement represents an EapTtls ack response (EAP-TTLS#9.2.3) */
    public static class EapTtlsAcknowledgement extends EapTtlsTypeData {
        private static final String TAG = EapTtlsAcknowledgement.class.getSimpleName();

        @VisibleForTesting
        public EapTtlsAcknowledgement() throws EapTtlsParsingException {
            super(
                    false /* no fragmentation */,
                    false /* not start */,
                    0 /* version */,
                    0 /* length */,
                    new byte[0] /* no data */);
        }

        /**
         * Constructs and returns a new EAP-TTLS acknowledgement type data.
         *
         * @return a new EapTtlsAcknowledgement instance
         */
        public static EapTtlsAcknowledgement getEapTtlsAcknowledgement() {
            try {
                return new EapTtlsAcknowledgement();
            } catch (EapTtlsParsingException e) {
                // This should never happen
                LOG.e(
                        TAG,
                        "Parsing exception thrown while attempting"
                                + "to create an acknowledgement packet");
                return null;
            }
        }
    }

    /** EapTtlsTypeDataDecoder will be used for decoding {@link EapTtlsTypeData} objects. */
    public static class EapTtlsTypeDataDecoder {

        /**
         * Decodes and returns an EapTtlsTypeData for the specified eapTypeData.
         *
         * @param eapTypeData byte[] to be decoded as an EapTtlsTypeData instance
         * @return DecodeResult wrapping an EapTtlsTypeData instance for the given eapTypeData iff
         *     the eapTypeData is formatted correctly. Otherwise, the DecodeResult wraps the
         *     appropriate EapError.
         */
        public DecodeResult decodeEapTtlsRequestPacket(byte[] eapTypeData) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(eapTypeData);
                return new DecodeResult(new EapTtlsTypeData(buffer));
            } catch (BufferUnderflowException | EapTtlsParsingException e) {
                return new DecodeResult(new EapError(e));
            }
        }

        /**
         * DecodeResult represents the result from calling a decode method within
         * EapTtlsTypeDataDecoder. It will contain either an EapTtlsTypeData or an EapError.
         */
        public static class DecodeResult {
            public final EapTtlsTypeData eapTypeData;
            public final EapError eapError;

            public DecodeResult(EapTtlsTypeData eapTypeData) {
                this.eapTypeData = eapTypeData;
                this.eapError = null;
            }

            public DecodeResult(EapError eapError) {
                this.eapTypeData = null;
                this.eapError = eapError;
            }

            /**
             * Checks whether this instance represents a successful decode operation.
             *
             * @return true iff this DecodeResult represents a successfully decoded Type Data
             */
            public boolean isSuccessfulDecode() {
                return eapTypeData != null;
            }
        }
    }
}

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

import android.net.ipsec.ike.IkeTrafficSelector;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;

import java.nio.ByteBuffer;

/**
 * IkeTsPayload represents an Traffic Selector Initiator Payload or an Traffic Selector Responder
 * Payload.
 *
 * <p>Traffic Selector Initiator Payload and Traffic Selector Responder Payload have same format but
 * different payload types. They describe the address ranges and port ranges of Child SA initiator
 * and Child SA responder.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.13">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeTsPayload extends IkePayload {
    // Length of Traffic Selector Payload header.
    private static final int TS_HEADER_LEN = 4;
    // Length of reserved field in octets.
    private static final int TS_HEADER_RESERVED_LEN = 3;

    /** Number of Traffic Selectors */
    public final int numTs;
    /** Array of Traffic Selectors */
    public final IkeTrafficSelector[] trafficSelectors;

    IkeTsPayload(boolean critical, byte[] payloadBody, boolean isInitiator)
            throws IkeProtocolException {
        super((isInitiator ? PAYLOAD_TYPE_TS_INITIATOR : PAYLOAD_TYPE_TS_RESPONDER), critical);

        ByteBuffer inputBuffer = ByteBuffer.wrap(payloadBody);
        numTs = Byte.toUnsignedInt(inputBuffer.get());
        if (numTs == 0) {
            throw new InvalidSyntaxException("Cannot find Traffic Selector in TS payload.");
        }

        // Skip RESERVED byte
        inputBuffer.get(new byte[TS_HEADER_RESERVED_LEN]);

        // Decode Traffic Selectors
        byte[] tsBytes = new byte[inputBuffer.remaining()];
        inputBuffer.get(tsBytes);
        trafficSelectors = IkeTrafficSelector.decodeIkeTrafficSelectors(numTs, tsBytes);
    }

    /**
     * Construct an instance of IkeTsPayload for building an outbound IKE message.
     *
     * @param isInitiator indicates if this payload is for a Child SA initiator or responder.
     * @param ikeTrafficSelectors the array of included traffic selectors.
     */
    public IkeTsPayload(boolean isInitiator, IkeTrafficSelector[] ikeTrafficSelectors) {
        super((isInitiator ? PAYLOAD_TYPE_TS_INITIATOR : PAYLOAD_TYPE_TS_RESPONDER), false);

        if (ikeTrafficSelectors == null || ikeTrafficSelectors.length == 0) {
            throw new IllegalArgumentException(
                    "TS Payload requires at least one Traffic Selector.");
        }

        numTs = ikeTrafficSelectors.length;
        trafficSelectors = ikeTrafficSelectors;
    }

    /**
     * Check if this TS payload contains the all TS in the provided TS payload.
     *
     * <p>A TS response cannot be narrower than a TS request. When doing rekey, the newly negotiated
     * TS cannot be narrower than old negotiated TS.
     *
     * <p>This method will be used to (1) validate that an inbound response is subset of a locally
     * generated request; and (2) validate that an inbound rekey request/response is superset of
     * current negotiated TS.
     *
     * @param tsPayload the other TS payload to validate
     * @return true if current TS Payload contains all TS in the input tsPayload
     */
    public boolean contains(IkeTsPayload tsPayload) {
        subTsLoop:
        for (IkeTrafficSelector subTs : tsPayload.trafficSelectors) {
            for (IkeTrafficSelector superTs : this.trafficSelectors) {
                if (superTs.contains(subTs)) {
                    continue subTsLoop;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Encode Traffic Selector Payload to ByteBuffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param byteBuffer destination ByteBuffer that stores encoded payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        encodePayloadHeaderToByteBuffer(nextPayload, getPayloadLength(), byteBuffer);

        byteBuffer.put((byte) numTs).put(new byte[TS_HEADER_RESERVED_LEN]);
        for (IkeTrafficSelector ts : trafficSelectors) {
            ts.encodeToByteBuffer(byteBuffer);
        }
    }

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        int len = GENERIC_HEADER_LENGTH + TS_HEADER_LEN;
        for (IkeTrafficSelector ts : trafficSelectors) {
            len += ts.selectorLength;
        }

        return len;
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        switch (payloadType) {
            case PAYLOAD_TYPE_TS_INITIATOR:
                return "TSi";
            case PAYLOAD_TYPE_TS_RESPONDER:
                return "TSr";
            default:
                // Won't reach here.
                throw new IllegalArgumentException(
                        "Invalid Payload Type for Traffic Selector Payload.");
        }
    }
}

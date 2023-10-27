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

import com.android.internal.net.eap.message.EapMessage;

import java.nio.ByteBuffer;

/**
 * IkeEapPayload represents an EAP payload.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.8">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @see <a href="https://tools.ietf.org/html/rfc3748#section-4">RFC 3748, Extensible Authentication
 *     Protocol (EAP)</a>
 */
public final class IkeEapPayload extends IkePayload {
    public final byte[] eapMessage;

    /**
     * Construct an instance of IkeEapPayload from a decoded inbound IKE packet.
     *
     * <p>Any syntax errors contained in the eapMessage will be handled in {@link EapMessage}.
     *
     * @param isCritical indicates if this payload is critical. Ignored in supported payload as
     *     instructed by the RFC 7296.
     * @param eapMessage byte-array encoded EapMessage
     */
    IkeEapPayload(boolean isCritical, byte[] eapMessage) {
        super(PAYLOAD_TYPE_EAP, isCritical);

        this.eapMessage = eapMessage;
    }

    /**
     * Construct an instance of IkeEapPayload for an outbound IKE EAP message.
     *
     * <p>This eapMessage is constructed in the IKE session and is guaranteed to have valid syntax.
     *
     * @param eapMessage byte-array encoded EapMessage
     */
    public IkeEapPayload(byte[] eapMessage) {
        super(PAYLOAD_TYPE_EAP, false);

        this.eapMessage = eapMessage;
    }

    /**
     * Encode EAP Payload to ByteBuffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param byteBuffer destination ByteBuffer that stores encoded payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        encodePayloadHeaderToByteBuffer(nextPayload, getPayloadLength(), byteBuffer);
        byteBuffer.put(eapMessage);
    }

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        return GENERIC_HEADER_LENGTH + eapMessage.length;
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        return "EAP";
    }
}

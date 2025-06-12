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

import android.annotation.IntDef;
import android.util.SparseArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * IkePayload is an abstract class that represents the common information for all IKE payload types.
 *
 * <p>Each types of IKE payload should implement its own subclass with its own decoding and encoding
 * logic.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.2">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public abstract class IkePayload {
    // Critical bit and following reserved 7 bits in payload generic header must all be zero
    private static final byte PAYLOAD_HEADER_CRITICAL_BIT_UNSET = 0;
    /** Length of a generic IKE payload header */
    public static final int GENERIC_HEADER_LENGTH = 4;

    /**
     * Payload types as defined by IANA:
     *
     * @see <a href="https://www.iana.org/assignments/ikev2-parameters/ikev2-parameters.xhtml">
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        PAYLOAD_TYPE_NO_NEXT,
        PAYLOAD_TYPE_SA,
        PAYLOAD_TYPE_KE,
        PAYLOAD_TYPE_CERT,
        PAYLOAD_TYPE_CERT_REQUEST,
        PAYLOAD_TYPE_AUTH,
        PAYLOAD_TYPE_ID_INITIATOR,
        PAYLOAD_TYPE_ID_RESPONDER,
        PAYLOAD_TYPE_NONCE,
        PAYLOAD_TYPE_NOTIFY,
        PAYLOAD_TYPE_DELETE,
        PAYLOAD_TYPE_VENDOR,
        PAYLOAD_TYPE_TS_INITIATOR,
        PAYLOAD_TYPE_TS_RESPONDER,
        PAYLOAD_TYPE_SK,
        PAYLOAD_TYPE_CP,
        PAYLOAD_TYPE_EAP,
        PAYLOAD_TYPE_SKF
    })
    public @interface PayloadType {}

    /** No Next Payload */
    public static final int PAYLOAD_TYPE_NO_NEXT = 0;
    /** Security Association Payload */
    public static final int PAYLOAD_TYPE_SA = 33;
    /** Key Exchange Payload */
    public static final int PAYLOAD_TYPE_KE = 34;
    /** Identification Payload for IKE SA Initiator */
    public static final int PAYLOAD_TYPE_ID_INITIATOR = 35;
    /** Identification Payload for IKE SA Responder */
    public static final int PAYLOAD_TYPE_ID_RESPONDER = 36;
    /** Certificate Payload */
    public static final int PAYLOAD_TYPE_CERT = 37;
    /** Certificate Request Payload */
    public static final int PAYLOAD_TYPE_CERT_REQUEST = 38;
    /** Authentication Payload */
    public static final int PAYLOAD_TYPE_AUTH = 39;
    /** Nonce Payload */
    public static final int PAYLOAD_TYPE_NONCE = 40;
    /** Notify Payload */
    public static final int PAYLOAD_TYPE_NOTIFY = 41;
    /** Delete Payload */
    public static final int PAYLOAD_TYPE_DELETE = 42;
    /** Vendor Payload */
    public static final int PAYLOAD_TYPE_VENDOR = 43;
    /** Traffic Selector Payload of Child SA Initiator */
    public static final int PAYLOAD_TYPE_TS_INITIATOR = 44;
    /** Traffic Selector Payload of Child SA Responder */
    public static final int PAYLOAD_TYPE_TS_RESPONDER = 45;
    /** Encrypted and Authenticated Payload */
    public static final int PAYLOAD_TYPE_SK = 46;
    /** Configuration Payload */
    public static final int PAYLOAD_TYPE_CP = 47;
    /** EAP Payload */
    public static final int PAYLOAD_TYPE_EAP = 48;
    /** Encrypted and Authenticated Fragment */
    public static final int PAYLOAD_TYPE_SKF = 53;

    // TODO: List all payload types.

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        PROTOCOL_ID_UNSET,
        PROTOCOL_ID_IKE,
        PROTOCOL_ID_AH,
        PROTOCOL_ID_ESP,
    })
    public @interface ProtocolId {}

    public static final int PROTOCOL_ID_UNSET = 0;
    public static final int PROTOCOL_ID_IKE = 1;
    public static final int PROTOCOL_ID_AH = 2;
    public static final int PROTOCOL_ID_ESP = 3;

    private static final SparseArray<String> PROTOCOL_TO_STR;

    static {
        PROTOCOL_TO_STR = new SparseArray<>();
        PROTOCOL_TO_STR.put(PROTOCOL_ID_UNSET, "Protocol Unset");
        PROTOCOL_TO_STR.put(PROTOCOL_ID_IKE, "IKE");
        PROTOCOL_TO_STR.put(PROTOCOL_ID_AH, "AH");
        PROTOCOL_TO_STR.put(PROTOCOL_ID_ESP, "ESP");
    }

    public static final byte SPI_LEN_NOT_INCLUDED = 0;
    public static final byte SPI_LEN_IPSEC = 4;
    public static final byte SPI_LEN_IKE = 8;

    public static final int SPI_NOT_INCLUDED = 0;

    /** Length of port number in bytes */
    public static final int IP_PORT_LEN = 2;

    public final int payloadType;
    public final boolean isCritical;

    /**
     * Construct a instance of IkePayload in the context of a IkePayloadFactory.
     *
     * <p>It should be overrided by subclass of IkePayload
     *
     * @param payload the payload type. All supported types will fall in {@link
     *     IkePayload.PayloadType}
     * @param critical indicates if this payload is critical. Ignore it when payload type is
     *     supported.
     */
    IkePayload(int payload, boolean critical) {
        payloadType = payload;
        isCritical = critical;
    }

    /**
     * A helper method to quickly obtain payloads with the input payload type in the provided
     * payload list.
     *
     * <p>This method will not check if this payload type can be repeatable in an IKE message
     * because it does not know the context of the provided payload list. Caller should call this
     * method if they are expecting more than one payloads in the list.
     *
     * @param payloadType the payloadType to look for.
     * @param payloadClass the class of the desired payload.
     * @param searchList the payload list to do the search.
     * @return a list of IkePayloads with the payloadType.
     */
    public static <T extends IkePayload> List<T> getPayloadListForTypeInProvidedList(
            @IkePayload.PayloadType int payloadType,
            Class<T> payloadClass,
            List<IkePayload> searchList) {
        List<T> payloadList = new LinkedList<>();

        for (IkePayload payload : searchList) {
            if (payloadType == payload.payloadType) {
                payloadList.add(payloadClass.cast(payload));
            }
        }

        return payloadList;
    }

    /**
     * A helper method to quickly obtain the payload with the input payload type in the provided
     * payload list.
     *
     * <p>This method will not check if this payload type can be repeatable in an IKE message
     * because it does not know the context of the provided payload list. Caller should call this
     * method if they are expecting no more than one payloads in the list.
     *
     * @param payloadType the payloadType to look for.
     * @param payloadClass the class of the desired payload.
     * @param searchList the payload list to do the search.
     * @return the IkePayload with the payloadType.
     */
    public static <T extends IkePayload> T getPayloadForTypeInProvidedList(
            @IkePayload.PayloadType int payloadType,
            Class<T> payloadClass,
            List<IkePayload> searchList) {
        List<T> payloadList =
                getPayloadListForTypeInProvidedList(payloadType, payloadClass, searchList);
        return payloadList.isEmpty() ? null : payloadList.get(0);
    }

    /**
     * Encode generic payload header to ByteBuffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param payloadLength length of the entire payload
     * @param byteBuffer destination ByteBuffer that stores encoded payload header
     */
    protected static void encodePayloadHeaderToByteBuffer(
            @PayloadType int nextPayload, int payloadLength, ByteBuffer byteBuffer) {
        byteBuffer
                .put((byte) nextPayload)
                .put(PAYLOAD_HEADER_CRITICAL_BIT_UNSET)
                .putShort((short) payloadLength);
    }

    /** Retuns protocol type as String. */
    public static String getProtocolTypeString(@ProtocolId int protocol) {
        return PROTOCOL_TO_STR.get(protocol);
    }

    /**
     * Encode payload to ByteBuffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param byteBuffer destination ByteBuffer that stores encoded payload.
     */
    protected abstract void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer);

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    protected abstract int getPayloadLength();

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    public abstract String getTypeString();
}

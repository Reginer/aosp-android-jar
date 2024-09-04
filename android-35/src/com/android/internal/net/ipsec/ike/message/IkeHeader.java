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

import static com.android.internal.net.ipsec.ike.message.IkePayload.PayloadType;

import android.annotation.IntDef;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.exceptions.InvalidMajorVersionException;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * IkeHeader represents an IKE message header. It contains all header attributes and provide methods
 * for encoding and decoding it.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.1">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeHeader {
    // TODO: b/122838549 Change IkeHeader to static inner class of IkeMessage.
    private static final byte IKE_HEADER_VERSION_INFO = (byte) 0x20;

    // Indicate whether this message is a response message
    private static final byte IKE_HEADER_FLAG_IS_RESP_MSG = (byte) 0x20;
    // Indicate whether this message is sent from the original IKE initiator
    private static final byte IKE_HEADER_FLAG_FROM_IKE_INITIATOR = (byte) 0x08;

    private static final SparseArray<String> EXCHANGE_TYPE_TO_STRING;

    public static final int IKE_HEADER_LENGTH = 28;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        EXCHANGE_TYPE_IKE_SA_INIT,
        EXCHANGE_TYPE_IKE_AUTH,
        EXCHANGE_TYPE_CREATE_CHILD_SA,
        EXCHANGE_TYPE_INFORMATIONAL
    })
    public @interface ExchangeType {}

    public static final int EXCHANGE_TYPE_IKE_SA_INIT = 34;
    public static final int EXCHANGE_TYPE_IKE_AUTH = 35;
    public static final int EXCHANGE_TYPE_CREATE_CHILD_SA = 36;
    public static final int EXCHANGE_TYPE_INFORMATIONAL = 37;

    static {
        EXCHANGE_TYPE_TO_STRING = new SparseArray<>();
        EXCHANGE_TYPE_TO_STRING.put(EXCHANGE_TYPE_IKE_SA_INIT, "IKE INIT");
        EXCHANGE_TYPE_TO_STRING.put(EXCHANGE_TYPE_IKE_AUTH, "IKE AUTH");
        EXCHANGE_TYPE_TO_STRING.put(EXCHANGE_TYPE_CREATE_CHILD_SA, "Create Child");
        EXCHANGE_TYPE_TO_STRING.put(EXCHANGE_TYPE_INFORMATIONAL, "Informational");
    }

    public final long ikeInitiatorSpi;
    public final long ikeResponderSpi;
    @PayloadType public final int nextPayloadType;
    public final byte majorVersion;
    public final byte minorVersion;
    @ExchangeType public final int exchangeType;
    public final boolean isResponseMsg;
    public final boolean fromIkeInitiator;
    public final int messageId;

    // Cannot assign encoded message length value for an outbound IKE message before it's encoded.
    private static final int ENCODED_MESSAGE_LEN_UNAVAILABLE = -1;

    // mEncodedMessageLength is only set for an inbound IkeMessage. When building an outbound
    // IkeMessage, message length is not set because message body length is unknown until it gets
    // encrypted and encoded.
    private final int mEncodedMessageLength;

    /**
     * Construct an instance of IkeHeader. It is only called in the process of building outbound
     * message.
     *
     * @param iSpi the SPI of IKE initiator
     * @param rSpi the SPI of IKE responder
     * @param nextPType the first payload's type
     * @param eType the type of IKE exchange being used
     * @param isResp indicates if this message is a response or a request
     * @param fromInit indictaes if this message is sent from the IKE initiator or the IKE responder
     * @param msgId the message identifier
     */
    public IkeHeader(
            long iSpi,
            long rSpi,
            @PayloadType int nextPType,
            @ExchangeType int eType,
            boolean isResp,
            boolean fromInit,
            int msgId) {
        ikeInitiatorSpi = iSpi;
        ikeResponderSpi = rSpi;
        nextPayloadType = nextPType;
        exchangeType = eType;
        isResponseMsg = isResp;
        fromIkeInitiator = fromInit;
        messageId = msgId;

        mEncodedMessageLength = ENCODED_MESSAGE_LEN_UNAVAILABLE;

        // Major version of IKE protocol in use; it must be set to 2 when building an IKEv2 message.
        majorVersion = 2;
        // Minor version of IKE protocol in use; it must be set to 0 when building an IKEv2 message.
        minorVersion = 0;
    }

    /**
     * Decode IKE header from a byte array and construct an IkeHeader instance.
     *
     * @param packet the raw byte array of the whole IKE message
     */
    public IkeHeader(byte[] packet) throws IkeProtocolException {
        if (packet.length <= IKE_HEADER_LENGTH) {
            throw new InvalidSyntaxException("IKE message is too short to contain a header");
        }

        ByteBuffer buffer = ByteBuffer.wrap(packet);

        ikeInitiatorSpi = buffer.getLong();
        ikeResponderSpi = buffer.getLong();
        nextPayloadType = Byte.toUnsignedInt(buffer.get());

        byte versionByte = buffer.get();
        majorVersion = (byte) ((versionByte >> 4) & 0x0F);
        minorVersion = (byte) (versionByte & 0x0F);

        exchangeType = Byte.toUnsignedInt(buffer.get());

        byte flagsByte = buffer.get();
        isResponseMsg = ((flagsByte & 0x20) != 0);
        fromIkeInitiator = ((flagsByte & 0x08) != 0);

        messageId = buffer.getInt();
        mEncodedMessageLength = buffer.getInt();
    }

    /** Packet private method to build header of an IKE fragemnt from current IKE header. */
    IkeHeader makeSkfHeaderFromSkHeader() {
        if (nextPayloadType != IkePayload.PAYLOAD_TYPE_SK) {
            throw new IllegalArgumentException("Next payload type is not SK.");
        }
        return new IkeHeader(
                ikeInitiatorSpi,
                ikeResponderSpi,
                IkePayload.PAYLOAD_TYPE_SKF,
                exchangeType,
                isResponseMsg,
                fromIkeInitiator,
                messageId);
    }

    /*Package private*/
    @VisibleForTesting
    int getInboundMessageLength() {
        if (mEncodedMessageLength == ENCODED_MESSAGE_LEN_UNAVAILABLE) {
            throw new UnsupportedOperationException(
                    "It is not supported to get encoded message length from an outbound message.");
        }
        return mEncodedMessageLength;
    }

    /** Validate major version of inbound IKE header. */
    public void validateMajorVersion() throws IkeProtocolException {
        if (majorVersion > 2) {
            // Receive higher version of protocol. Stop parsing.
            throw new InvalidMajorVersionException(majorVersion);
        }
        if (majorVersion < 2) {
            // There is no specific instruction for dealing with this error case.
            // Since IKE library only supports IKEv2 and not allowed to check if message
            // sender supports higher version, it is proper to treat this error as an invalid syntax
            // error.
            throw new InvalidSyntaxException("Major version is smaller than 2.");
        }
    }

    /**
     * Validate syntax of inbound IKE header.
     *
     * <p>It MUST only be used for an inbound IKE header because we don't know the outbound message
     * length before we encode it.
     */
    public void validateInboundHeader(int packetLength) throws IkeProtocolException {
        if (exchangeType < EXCHANGE_TYPE_IKE_SA_INIT
                || exchangeType > EXCHANGE_TYPE_INFORMATIONAL) {
            throw new InvalidSyntaxException("Invalid IKE Exchange Type.");
        }
        if (mEncodedMessageLength != packetLength) {
            throw new InvalidSyntaxException("Invalid IKE Message Length.");
        }
    }

    /** Encode IKE header to ByteBuffer */
    public void encodeToByteBuffer(ByteBuffer byteBuffer, int encodedMessageBodyLen) {
        byteBuffer
                .putLong(ikeInitiatorSpi)
                .putLong(ikeResponderSpi)
                .put((byte) nextPayloadType)
                .put(IKE_HEADER_VERSION_INFO)
                .put((byte) exchangeType);

        byte flag = 0;
        if (isResponseMsg) {
            flag |= IKE_HEADER_FLAG_IS_RESP_MSG;
        }
        if (fromIkeInitiator) {
            flag |= IKE_HEADER_FLAG_FROM_IKE_INITIATOR;
        }

        byteBuffer.put(flag).putInt(messageId).putInt(IKE_HEADER_LENGTH + encodedMessageBodyLen);
    }

    /** Returns basic information for logging. */
    public String getBasicInfoString() {
        String exchangeStr = EXCHANGE_TYPE_TO_STRING.get(exchangeType);
        if (exchangeStr == null) exchangeStr = "Unknown exchange (" + exchangeType + ")";

        String reqOrResp = isResponseMsg ? "response" : "request";

        return exchangeStr + " " + reqOrResp + " " + messageId;
    }
}

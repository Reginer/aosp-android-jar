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

import static android.net.ipsec.ike.IkeManager.getIkeLog;
import static android.net.ipsec.ike.exceptions.IkeException.wrapAsIkeException;

import static com.android.internal.net.ipsec.ike.message.IkePayload.PAYLOAD_TYPE_NOTIFY;
import static com.android.internal.net.ipsec.ike.message.IkePayload.PayloadType;

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.net.ipsec.ike.exceptions.IkeException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.exceptions.InvalidMessageIdException;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;
import android.net.ipsec.ike.exceptions.UnsupportedCriticalPayloadException;
import android.util.Pair;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.ipsec.ike.SaRecord.IkeSaRecord;
import com.android.internal.net.ipsec.ike.crypto.IkeCipher;
import com.android.internal.net.ipsec.ike.crypto.IkeMacIntegrity;
import com.android.internal.net.ipsec.ike.message.IkeNotifyPayload.NotifyType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * IkeMessage represents an IKE message.
 *
 * <p>It contains all attributes and provides methods for encoding, decoding, encrypting and
 * decrypting.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeMessage {
    private static final String TAG = "IkeMessage";

    private static IIkeMessageHelper sIkeMessageHelper = new IkeMessageHelper();

    // Currently use HarmonyJSSE as TrustManager provider
    static final Provider TRUST_MANAGER_PROVIDER = Security.getProvider("HarmonyJSSE");

    // Payload types in this set may be included multiple times within an IKE message. All other
    // payload types can be included at most once.
    private static final Set<Integer> REPEATABLE_PAYLOAD_TYPES = new HashSet<>();

    static {
        REPEATABLE_PAYLOAD_TYPES.add(IkePayload.PAYLOAD_TYPE_CERT);
        REPEATABLE_PAYLOAD_TYPES.add(IkePayload.PAYLOAD_TYPE_CERT_REQUEST);
        REPEATABLE_PAYLOAD_TYPES.add(IkePayload.PAYLOAD_TYPE_NOTIFY);
        REPEATABLE_PAYLOAD_TYPES.add(IkePayload.PAYLOAD_TYPE_DELETE);
        REPEATABLE_PAYLOAD_TYPES.add(IkePayload.PAYLOAD_TYPE_VENDOR);
    }

    // IKE exchange subtypes describe the specific function of a IKE request/response exchange. It
    // helps IKE and Child Session to process message according to the subtype specific rules.
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        IKE_EXCHANGE_SUBTYPE_INVALID,
        IKE_EXCHANGE_SUBTYPE_IKE_INIT,
        IKE_EXCHANGE_SUBTYPE_IKE_AUTH,
        IKE_EXCHANGE_SUBTYPE_DELETE_IKE,
        IKE_EXCHANGE_SUBTYPE_DELETE_CHILD,
        IKE_EXCHANGE_SUBTYPE_REKEY_IKE,
        IKE_EXCHANGE_SUBTYPE_REKEY_CHILD,
        IKE_EXCHANGE_SUBTYPE_GENERIC_INFO
    })
    public @interface IkeExchangeSubType {}

    public static final int IKE_EXCHANGE_SUBTYPE_INVALID = 0;
    public static final int IKE_EXCHANGE_SUBTYPE_IKE_INIT = 1;
    public static final int IKE_EXCHANGE_SUBTYPE_IKE_AUTH = 2;
    public static final int IKE_EXCHANGE_SUBTYPE_CREATE_CHILD = 3;
    public static final int IKE_EXCHANGE_SUBTYPE_DELETE_IKE = 4;
    public static final int IKE_EXCHANGE_SUBTYPE_DELETE_CHILD = 5;
    public static final int IKE_EXCHANGE_SUBTYPE_REKEY_IKE = 6;
    public static final int IKE_EXCHANGE_SUBTYPE_REKEY_CHILD = 7;
    public static final int IKE_EXCHANGE_SUBTYPE_GENERIC_INFO = 8;

    private static final SparseArray<String> EXCHANGE_SUBTYPE_TO_STRING;

    static {
        EXCHANGE_SUBTYPE_TO_STRING = new SparseArray<>();
        EXCHANGE_SUBTYPE_TO_STRING.put(IKE_EXCHANGE_SUBTYPE_INVALID, "Invalid");
        EXCHANGE_SUBTYPE_TO_STRING.put(IKE_EXCHANGE_SUBTYPE_IKE_INIT, "IKE INIT");
        EXCHANGE_SUBTYPE_TO_STRING.put(IKE_EXCHANGE_SUBTYPE_IKE_AUTH, "IKE AUTH");
        EXCHANGE_SUBTYPE_TO_STRING.put(IKE_EXCHANGE_SUBTYPE_CREATE_CHILD, "Create Child");
        EXCHANGE_SUBTYPE_TO_STRING.put(IKE_EXCHANGE_SUBTYPE_DELETE_IKE, "Delete IKE");
        EXCHANGE_SUBTYPE_TO_STRING.put(IKE_EXCHANGE_SUBTYPE_DELETE_CHILD, "Delete Child");
        EXCHANGE_SUBTYPE_TO_STRING.put(IKE_EXCHANGE_SUBTYPE_REKEY_IKE, "Rekey IKE");
        EXCHANGE_SUBTYPE_TO_STRING.put(IKE_EXCHANGE_SUBTYPE_REKEY_CHILD, "Rekey Child");
        EXCHANGE_SUBTYPE_TO_STRING.put(IKE_EXCHANGE_SUBTYPE_GENERIC_INFO, "Generic Info");
    }

    public final IkeHeader ikeHeader;
    public final List<IkePayload> ikePayloadList = new ArrayList<>();
    /**
     * Construct an instance of IkeMessage. It is called by decode or for building outbound message.
     *
     * @param header the header of this IKE message
     * @param payloadList the list of decoded IKE payloads in this IKE message
     */
    public IkeMessage(IkeHeader header, List<IkePayload> payloadList) {
        ikeHeader = header;
        ikePayloadList.addAll(payloadList);
    }

    /**
     * Get security provider for X509TrustManager to do certificate validation.
     *
     * <p>Use JSSEProvdier as the default security provider.
     *
     * @return the provider for X509TrustManager
     */
    public static Provider getTrustManagerProvider() {
        return TRUST_MANAGER_PROVIDER;
    }

    /**
     * Decode unencrypted IKE message body and create an instance of IkeMessage.
     *
     * <p>This method catches all RuntimeException during decoding incoming IKE packet.
     *
     * @param expectedMsgId the expected message ID to validate against.
     * @param header the IKE header that is decoded but not validated.
     * @param inputPacket the byte array contains the whole IKE message.
     * @return the decoding result.
     */
    public static DecodeResult decode(int expectedMsgId, IkeHeader header, byte[] inputPacket) {
        return sIkeMessageHelper.decode(expectedMsgId, header, inputPacket);
    }

    /**
     * Decrypt and decode encrypted IKE message body and create an instance of IkeMessage.
     *
     * @param expectedMsgId the expected message ID to validate against.
     * @param integrityMac the negotiated integrity algorithm.
     * @param decryptCipher the negotiated encryption algorithm.
     * @param ikeSaRecord ikeSaRecord where this packet is sent on.
     * @param ikeHeader header of IKE packet.
     * @param packet IKE packet as a byte array.
     * @param collectedFragments previously received IKE fragments.
     * @return the decoding result.
     */
    public static DecodeResult decode(
            int expectedMsgId,
            @Nullable IkeMacIntegrity integrityMac,
            IkeCipher decryptCipher,
            IkeSaRecord ikeSaRecord,
            IkeHeader ikeHeader,
            byte[] packet,
            DecodeResultPartial collectedFragments) {
        return sIkeMessageHelper.decode(
                expectedMsgId,
                integrityMac,
                decryptCipher,
                ikeSaRecord,
                ikeHeader,
                packet,
                collectedFragments);
    }

    private static List<IkePayload> decodePayloadList(
            @PayloadType int firstPayloadType, boolean isResp, byte[] unencryptedPayloads)
            throws IkeProtocolException {
        ByteBuffer inputBuffer = ByteBuffer.wrap(unencryptedPayloads);
        int currentPayloadType = firstPayloadType;
        // For supported payload
        List<IkePayload> supportedPayloadList = new LinkedList<>();
        // For unsupported critical payload
        List<Integer> unsupportedCriticalPayloadList = new LinkedList<>();

        // For marking the existence of supported payloads in this message.
        HashSet<Integer> supportedTypesFoundSet = new HashSet<>();

        StringBuilder logPayloadsSb = new StringBuilder();
        logPayloadsSb.append("Decoded payloads [ ");

        while (currentPayloadType != IkePayload.PAYLOAD_TYPE_NO_NEXT) {
            Pair<IkePayload, Integer> pair =
                    IkePayloadFactory.getIkePayload(currentPayloadType, isResp, inputBuffer);
            IkePayload payload = pair.first;
            logPayloadsSb.append(payload.getTypeString()).append(" ");

            if (!(payload instanceof IkeUnsupportedPayload)) {
                int type = payload.payloadType;
                if (!supportedTypesFoundSet.add(type) && !REPEATABLE_PAYLOAD_TYPES.contains(type)) {
                    throw new InvalidSyntaxException(
                            "It is not allowed to have multiple payloads with payload type: "
                                    + type);
                }

                supportedPayloadList.add(payload);
            } else if (payload.isCritical) {
                unsupportedCriticalPayloadList.add(payload.payloadType);
            }
            // Simply ignore unsupported uncritical payload.

            currentPayloadType = pair.second;
        }

        logPayloadsSb.append("]");
        getIkeLog().d("IkeMessage", logPayloadsSb.toString());

        if (inputBuffer.remaining() > 0) {
            throw new InvalidSyntaxException(
                    "Malformed IKE Payload: Unexpected bytes at the end of packet.");
        }

        if (unsupportedCriticalPayloadList.size() > 0) {
            throw new UnsupportedCriticalPayloadException(unsupportedCriticalPayloadList);
        }

        // TODO: Verify that for all status notification payloads, only
        // NOTIFY_TYPE_NAT_DETECTION_SOURCE_IP and NOTIFY_TYPE_IPCOMP_SUPPORTED can be included
        // multiple times in a request message. There is not a clear number restriction for
        // error notification payloads.

        return supportedPayloadList;
    }

    /**
     * Encode unencrypted IKE message.
     *
     * @return encoded IKE message in byte array.
     */
    public byte[] encode() {
        return sIkeMessageHelper.encode(this);
    }

    /**
     * Encrypt and encode packet.
     *
     * @param integrityMac the negotiated integrity algorithm.
     * @param encryptCipher the negotiated encryption algortihm.
     * @param ikeSaRecord the ikeSaRecord where this packet is sent on.
     * @param supportFragment if IKE fragmentation is supported
     * @param fragSize the maximum size of IKE fragment
     * @return encoded IKE message in byte array.
     */
    public byte[][] encryptAndEncode(
            @Nullable IkeMacIntegrity integrityMac,
            IkeCipher encryptCipher,
            IkeSaRecord ikeSaRecord,
            boolean supportFragment,
            int fragSize) {
        return sIkeMessageHelper.encryptAndEncode(
                integrityMac, encryptCipher, ikeSaRecord, this, supportFragment, fragSize);
    }

    /**
     * Encode all payloads to a byte array.
     *
     * @return byte array contains all encoded payloads
     */
    private byte[] encodePayloads() {
        StringBuilder logPayloadsSb = new StringBuilder();
        logPayloadsSb.append("Generating payloads [ ");

        int payloadLengthSum = 0;
        for (IkePayload payload : ikePayloadList) {
            payloadLengthSum += payload.getPayloadLength();
            logPayloadsSb.append(payload.getTypeString()).append(" ");
        }
        logPayloadsSb.append("]");
        getIkeLog().d("IkeMessage", logPayloadsSb.toString());

        if (ikePayloadList.isEmpty()) return new byte[0];

        ByteBuffer byteBuffer = ByteBuffer.allocate(payloadLengthSum);
        for (int i = 0; i < ikePayloadList.size() - 1; i++) {
            ikePayloadList
                    .get(i)
                    .encodeToByteBuffer(ikePayloadList.get(i + 1).payloadType, byteBuffer);
        }
        ikePayloadList
                .get(ikePayloadList.size() - 1)
                .encodeToByteBuffer(IkePayload.PAYLOAD_TYPE_NO_NEXT, byteBuffer);

        return byteBuffer.array();
    }

    /** Package */
    @VisibleForTesting
    byte[] attachEncodedHeader(byte[] encodedIkeBody) {
        ByteBuffer outputBuffer =
                ByteBuffer.allocate(IkeHeader.IKE_HEADER_LENGTH + encodedIkeBody.length);
        ikeHeader.encodeToByteBuffer(outputBuffer, encodedIkeBody.length);
        outputBuffer.put(encodedIkeBody);
        return outputBuffer.array();
    }

    /**
     * Obtain all payloads with input payload type.
     *
     * <p>This method can be only applied to the payload types that can be included multiple times
     * within an IKE message.
     *
     * @param payloadType the payloadType to look for.
     * @param payloadClass the class of the desired payloads.
     * @return a list of IkePayloads with the payloadType.
     */
    public <T extends IkePayload> List<T> getPayloadListForType(
            @IkePayload.PayloadType int payloadType, Class<T> payloadClass) {
        // STOPSHIP: b/130190639 Notify user the error and close IKE session.
        if (!REPEATABLE_PAYLOAD_TYPES.contains(payloadType)) {
            throw new IllegalArgumentException(
                    "Received unexpected payloadType: "
                            + payloadType
                            + " that can be included at most once within an IKE message.");
        }

        return IkePayload.getPayloadListForTypeInProvidedList(
                payloadType, payloadClass, ikePayloadList);
    }

    /**
     * Obtain the payload with the input payload type.
     *
     * <p>This method can be only applied to the payload type that can be included at most once
     * within an IKE message.
     *
     * @param payloadType the payloadType to look for.
     * @param payloadClass the class of the desired payload.
     * @return the IkePayload with the payloadType.
     */
    public <T extends IkePayload> T getPayloadForType(
            @IkePayload.PayloadType int payloadType, Class<T> payloadClass) {
        // STOPSHIP: b/130190639 Notify user the error and close IKE session.
        if (REPEATABLE_PAYLOAD_TYPES.contains(payloadType)) {
            throw new IllegalArgumentException(
                    "Received unexpected payloadType: "
                            + payloadType
                            + " that may be included multiple times within an IKE message.");
        }

        return IkePayload.getPayloadForTypeInProvidedList(
                payloadType, payloadClass, ikePayloadList);
    }

    /** Returns if a notification payload with a specified type is included in this message. */
    public boolean hasNotifyPayload(@NotifyType int notifyType) {
        for (IkeNotifyPayload notify :
                this.getPayloadListForType(PAYLOAD_TYPE_NOTIFY, IkeNotifyPayload.class)) {
            if (notify.notifyType == notifyType) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if this Request IkeMessage was a DPD message
     *
     * <p>An IKE message is a DPD request iff the message was encrypted (has a SK payload) and there
     * were no payloads within the SK payload (or outside the SK payload).
     */
    public boolean isDpdRequest() {
        return !ikeHeader.isResponseMsg
                && ikeHeader.exchangeType == IkeHeader.EXCHANGE_TYPE_INFORMATIONAL
                && ikePayloadList.isEmpty()
                && ikeHeader.nextPayloadType == IkePayload.PAYLOAD_TYPE_SK;
    }

    /** Returns the exchange sub type as a String */
    public static String getIkeExchangeSubTypeString(@IkeExchangeSubType int exchangeSubtype) {
        if (!EXCHANGE_SUBTYPE_TO_STRING.contains(exchangeSubtype)) {
            throw new IllegalStateException("Unrecognized exchangeSubtype " + exchangeSubtype);
        }
        return EXCHANGE_SUBTYPE_TO_STRING.get(exchangeSubtype);
    }

    /**
     * Gets IKE exchange subtype of an inbound IKE request message.
     *
     * <p>It is not allowed to obtain exchange subtype from an inbound response message for two
     * reasons. Firstly, the exchange subtype of a response message is the same with its
     * corresponding request message. Secondly, trying to get the exchange subtype from a response
     * message will easily fail when the response message contains only error notification payloads.
     */
    @IkeExchangeSubType
    public int getIkeExchangeSubType() {
        if (ikeHeader.isResponseMsg) {
            throw new IllegalStateException(
                    "IKE Exchange subtype unsupported for response messages.");
        }

        switch (ikeHeader.exchangeType) {
            case IkeHeader.EXCHANGE_TYPE_IKE_SA_INIT:
                return IKE_EXCHANGE_SUBTYPE_IKE_INIT;
            case IkeHeader.EXCHANGE_TYPE_IKE_AUTH:
                return IKE_EXCHANGE_SUBTYPE_IKE_AUTH;
            case IkeHeader.EXCHANGE_TYPE_CREATE_CHILD_SA:
                // It is guaranteed in the decoding process that SA Payload has at least one SA
                // Proposal. Since Rekey IKE and Create Child (both initial creation and rekey
                // creation) will cause a collision, although the RFC 7296 does not prohibit one SA
                // Payload to contain both IKE proposals and Child proposals, containing two types
                // does not make sense. IKE library will reply according to the first SA Proposal
                // type and ignore the other type.
                IkeSaPayload saPayload =
                        getPayloadForType(IkePayload.PAYLOAD_TYPE_SA, IkeSaPayload.class);
                if (saPayload == null) {
                    return IKE_EXCHANGE_SUBTYPE_INVALID;
                }

                // If the received message has both SA(IKE) Payload and Notify-Rekey Payload, IKE
                // library will treat it as a Rekey IKE request and ignore the Notify-Rekey
                // Payload to provide better interoperability.
                if (saPayload.proposalList.get(0).protocolId == IkePayload.PROTOCOL_ID_IKE) {
                    return IKE_EXCHANGE_SUBTYPE_REKEY_IKE;
                }

                // If a Notify-Rekey Payload is found, this message is for rekeying a Child SA.
                List<IkeNotifyPayload> notifyPayloads =
                        getPayloadListForType(
                                IkePayload.PAYLOAD_TYPE_NOTIFY, IkeNotifyPayload.class);

                // It is checked during decoding that there is at most one Rekey notification
                // payload.
                for (IkeNotifyPayload notifyPayload : notifyPayloads) {
                    if (notifyPayload.notifyType == IkeNotifyPayload.NOTIFY_TYPE_REKEY_SA) {
                        return IKE_EXCHANGE_SUBTYPE_REKEY_CHILD;
                    }
                }

                return IKE_EXCHANGE_SUBTYPE_CREATE_CHILD;
            case IkeHeader.EXCHANGE_TYPE_INFORMATIONAL:
                List<IkeDeletePayload> deletePayloads =
                        getPayloadListForType(
                                IkePayload.PAYLOAD_TYPE_DELETE, IkeDeletePayload.class);

                // If no Delete payload was found, this request is a generic informational request.
                if (deletePayloads.isEmpty()) return IKE_EXCHANGE_SUBTYPE_GENERIC_INFO;

                // IKEv2 protocol does not clearly disallow to have both a Delete IKE payload and a
                // Delete Child payload in one IKE message. In this case, IKE library will only
                // respond to the Delete IKE payload.
                for (IkeDeletePayload deletePayload : deletePayloads) {
                    if (deletePayload.protocolId == IkePayload.PROTOCOL_ID_IKE) {
                        return IKE_EXCHANGE_SUBTYPE_DELETE_IKE;
                    }
                }
                return IKE_EXCHANGE_SUBTYPE_DELETE_CHILD;
            default:
                throw new IllegalStateException(
                        "Unrecognized exchange type in the validated IKE header: "
                                + ikeHeader.exchangeType);
        }
    }

    /**
     * IIkeMessageHelper provides interface for decoding, encoding and processing IKE packet.
     *
     * <p>IkeMessageHelper exists so that the interface is injectable for testing.
     */
    @VisibleForTesting
    public interface IIkeMessageHelper {
        /**
         * Encode IKE message.
         *
         * @param ikeMessage message need to be encoded.
         * @return encoded IKE message in byte array.
         */
        byte[] encode(IkeMessage ikeMessage);

        /**
         * Encrypt and encode IKE message.
         *
         * @param integrityMac the negotiated integrity algorithm.
         * @param encryptCipher the negotiated encryption algortihm.
         * @param ikeSaRecord the ikeSaRecord where this packet is sent on.
         * @param ikeMessage message need to be encoded. * @param supportFragment if IKE
         *     fragmentation is supported.
         * @param fragSize the maximum size of IKE fragment.
         * @return encoded IKE message in byte array.
         */
        byte[][] encryptAndEncode(
                @Nullable IkeMacIntegrity integrityMac,
                IkeCipher encryptCipher,
                IkeSaRecord ikeSaRecord,
                IkeMessage ikeMessage,
                boolean supportFragment,
                int fragSize);

        // TODO: Return DecodeResult when decoding unencrypted message
        /**
         * Decode unencrypted packet.
         *
         * @param expectedMsgId the expected message ID to validate against.
         * @param ikeHeader header of IKE packet.
         * @param packet IKE packet as a byte array.
         * @return the decoding result.
         */
        DecodeResult decode(int expectedMsgId, IkeHeader ikeHeader, byte[] packet);

        /**
         * Decrypt and decode packet.
         *
         * @param expectedMsgId the expected message ID to validate against.
         * @param integrityMac the negotiated integrity algorithm.
         * @param decryptCipher the negotiated encryption algorithm.
         * @param ikeSaRecord ikeSaRecord where this packet is sent on.
         * @param ikeHeader header of IKE packet.
         * @param packet IKE packet as a byte array.
         * @param collectedFragments previously received IKE fragments.
         * @return the decoding result.
         */
        DecodeResult decode(
                int expectedMsgId,
                @Nullable IkeMacIntegrity integrityMac,
                IkeCipher decryptCipher,
                IkeSaRecord ikeSaRecord,
                IkeHeader ikeHeader,
                byte[] packet,
                DecodeResultPartial collectedFragments);
    }

    /** IkeMessageHelper provides methods for decoding, encoding and processing IKE packet. */
    public static final class IkeMessageHelper implements IIkeMessageHelper {
        @Override
        public byte[] encode(IkeMessage ikeMessage) {
            getIkeLog().d("IkeMessage", "Generating " + ikeMessage.ikeHeader.getBasicInfoString());

            byte[] encodedIkeBody = ikeMessage.encodePayloads();
            byte[] packet = ikeMessage.attachEncodedHeader(encodedIkeBody);
            getIkeLog().d("IkeMessage", "Build a complete IKE message: " + getIkeLog().pii(packet));
            return packet;
        }

        @Override
        public byte[][] encryptAndEncode(
                @Nullable IkeMacIntegrity integrityMac,
                IkeCipher encryptCipher,
                IkeSaRecord ikeSaRecord,
                IkeMessage ikeMessage,
                boolean supportFragment,
                int fragSize) {
            getIkeLog().d("IkeMessage", "Generating " + ikeMessage.ikeHeader.getBasicInfoString());

            return encryptAndEncode(
                    ikeMessage.ikeHeader,
                    ikeMessage.ikePayloadList.isEmpty()
                            ? IkePayload.PAYLOAD_TYPE_NO_NEXT
                            : ikeMessage.ikePayloadList.get(0).payloadType,
                    ikeMessage.encodePayloads(),
                    integrityMac,
                    encryptCipher,
                    ikeSaRecord.getOutboundIntegrityKey(),
                    ikeSaRecord.getOutboundEncryptionKey(),
                    supportFragment,
                    fragSize);
        }

        @VisibleForTesting
        byte[][] encryptAndEncode(
                IkeHeader ikeHeader,
                @PayloadType int firstInnerPayload,
                byte[] unencryptedPayloads,
                @Nullable IkeMacIntegrity integrityMac,
                IkeCipher encryptCipher,
                byte[] integrityKey,
                byte[] encryptionKey,
                boolean supportFragment,
                int fragSize) {

            IkeSkPayload skPayload =
                    new IkeSkPayload(
                            ikeHeader,
                            firstInnerPayload,
                            unencryptedPayloads,
                            integrityMac,
                            encryptCipher,
                            integrityKey,
                            encryptionKey);
            int msgLen = IkeHeader.IKE_HEADER_LENGTH + skPayload.getPayloadLength();

            // Build complete IKE message
            if (!supportFragment || msgLen <= fragSize) {
                byte[][] packetList = new byte[1][];
                packetList[0] = encodeHeaderAndBody(ikeHeader, skPayload, firstInnerPayload);

                getIkeLog()
                        .d(
                                "IkeMessage",
                                "Build a complete IKE message: " + getIkeLog().pii(packetList[0]));
                return packetList;
            }

            // Build IKE fragments
            int dataLenPerPacket =
                    fragSize
                            - IkeHeader.IKE_HEADER_LENGTH
                            - IkePayload.GENERIC_HEADER_LENGTH
                            - IkeSkfPayload.SKF_HEADER_LEN
                            - encryptCipher.getIvLen()
                            - (integrityMac == null ? 0 : integrityMac.getChecksumLen())
                            - encryptCipher.getBlockSize();

            // Caller of this method MUST validate fragSize is valid.
            if (dataLenPerPacket <= 0) {
                throw new IllegalArgumentException(
                        "Max fragment size is too small for an IKE fragment.");
            }

            int totalFragments =
                    (unencryptedPayloads.length + dataLenPerPacket - 1) / dataLenPerPacket;
            IkeHeader skfHeader = ikeHeader.makeSkfHeaderFromSkHeader();
            byte[][] packetList = new byte[totalFragments][];

            ByteBuffer unencryptedDataBuffer = ByteBuffer.wrap(unencryptedPayloads);
            for (int i = 0; i < totalFragments; i++) {
                byte[] unencryptedData =
                        new byte[Math.min(dataLenPerPacket, unencryptedDataBuffer.remaining())];
                unencryptedDataBuffer.get(unencryptedData);

                int fragNum = i + 1; // 1-based

                int fragFirstInnerPayload =
                        i == 0 ? firstInnerPayload : IkePayload.PAYLOAD_TYPE_NO_NEXT;
                IkeSkfPayload skfPayload =
                        new IkeSkfPayload(
                                skfHeader,
                                fragFirstInnerPayload,
                                unencryptedData,
                                integrityMac,
                                encryptCipher,
                                integrityKey,
                                encryptionKey,
                                fragNum,
                                totalFragments);

                packetList[i] = encodeHeaderAndBody(skfHeader, skfPayload, fragFirstInnerPayload);
                getIkeLog()
                        .d(
                                "IkeMessage",
                                "Build an IKE fragment ("
                                        + (i + 1)
                                        + "/"
                                        + totalFragments
                                        + "): "
                                        + getIkeLog().pii(packetList[i]));
            }

            return packetList;
        }

        private byte[] encodeHeaderAndBody(
                IkeHeader ikeHeader, IkeSkPayload skPayload, @PayloadType int firstInnerPayload) {
            ByteBuffer outputBuffer =
                    ByteBuffer.allocate(IkeHeader.IKE_HEADER_LENGTH + skPayload.getPayloadLength());
            ikeHeader.encodeToByteBuffer(outputBuffer, skPayload.getPayloadLength());
            skPayload.encodeToByteBuffer(firstInnerPayload, outputBuffer);
            return outputBuffer.array();
        }

        @Override
        public DecodeResult decode(int expectedMsgId, IkeHeader header, byte[] inputPacket) {
            try {
                if (header.messageId != expectedMsgId) {
                    throw new InvalidMessageIdException(header.messageId);
                }

                header.validateMajorVersion();
                header.validateInboundHeader(inputPacket.length);

                byte[] unencryptedPayloads =
                        Arrays.copyOfRange(
                                inputPacket, IkeHeader.IKE_HEADER_LENGTH, inputPacket.length);
                List<IkePayload> supportedPayloadList =
                        decodePayloadList(
                                header.nextPayloadType, header.isResponseMsg, unencryptedPayloads);
                return new DecodeResultOk(
                        new IkeMessage(header, supportedPayloadList), inputPacket);
            } catch (NegativeArraySizeException | BufferUnderflowException e) {
                // Invalid length error when parsing payload bodies.
                return new DecodeResultUnprotectedError(
                        new InvalidSyntaxException("Malformed IKE Payload"));
            } catch (IkeProtocolException e) {
                return new DecodeResultUnprotectedError(e);
            }
        }

        @Override
        public DecodeResult decode(
                int expectedMsgId,
                @Nullable IkeMacIntegrity integrityMac,
                IkeCipher decryptCipher,
                IkeSaRecord ikeSaRecord,
                IkeHeader ikeHeader,
                byte[] packet,
                DecodeResultPartial collectedFragments) {
            return decode(
                    expectedMsgId,
                    ikeHeader,
                    packet,
                    integrityMac,
                    decryptCipher,
                    ikeSaRecord.getInboundIntegrityKey(),
                    ikeSaRecord.getInboundDecryptionKey(),
                    collectedFragments);
        }

        private DecodeResult decode(
                int expectedMsgId,
                IkeHeader header,
                byte[] inputPacket,
                @Nullable IkeMacIntegrity integrityMac,
                IkeCipher decryptCipher,
                byte[] integrityKey,
                byte[] decryptionKey,
                DecodeResultPartial collectedFragments) {
            if (header.nextPayloadType != IkePayload.PAYLOAD_TYPE_SK
                    && header.nextPayloadType != IkePayload.PAYLOAD_TYPE_SKF) {
                return new DecodeResultUnprotectedError(
                        new InvalidSyntaxException("Message contains unprotected payloads"));
            }

            // Decrypt message and do authentication
            Pair<IkeSkPayload, Integer> pair;
            try {
                pair =
                        decryptAndAuthenticate(
                                expectedMsgId,
                                header,
                                inputPacket,
                                integrityMac,
                                decryptCipher,
                                integrityKey,
                                decryptionKey);
            } catch (IkeException e) {
                if (collectedFragments == null) {
                    return new DecodeResultUnprotectedError(e);
                } else {
                    getIkeLog()
                            .i(
                                    TAG,
                                    "Message authentication or decryption failed on received"
                                            + " message. Discard it ",
                                    e);
                    return collectedFragments;
                }
            }

            // Handle IKE fragment
            boolean isFragment = (header.nextPayloadType == IkePayload.PAYLOAD_TYPE_SKF);
            boolean fragReassemblyStarted = (collectedFragments != null);

            if (isFragment) {
                getIkeLog()
                        .d(
                                TAG,
                                "Received an IKE fragment ("
                                        + ((IkeSkfPayload) pair.first).fragmentNum
                                        + "/"
                                        + ((IkeSkfPayload) pair.first).totalFragments
                                        + ")");
            }

            // IKE fragment reassembly has started but a complete message was received.
            if (!isFragment && fragReassemblyStarted) {
                getIkeLog()
                        .w(
                                TAG,
                                "Received a complete IKE message while doing IKE fragment"
                                        + " reassembly. Discard the newly received message.");
                return collectedFragments;
            }

            byte[] firstPacket = inputPacket;
            byte[] decryptedBytes = pair.first.getUnencryptedData();
            int firstPayloadType = pair.second;

            // Received an IKE fragment
            if (isFragment) {
                validateFragmentHeader(header, inputPacket.length, collectedFragments);

                // Add the recently received fragment to the reassembly queue.
                DecodeResultPartial DecodeResultPartial =
                        processIkeFragment(
                                header,
                                inputPacket,
                                (IkeSkfPayload) (pair.first),
                                pair.second,
                                collectedFragments);

                if (!DecodeResultPartial.isAllFragmentsReceived()) return DecodeResultPartial;

                firstPayloadType = DecodeResultPartial.firstPayloadType;
                decryptedBytes = DecodeResultPartial.reassembleAllFrags();
                firstPacket = DecodeResultPartial.firstFragBytes;
            }

            // Received or has reassembled a complete IKE message. Check if there is protocol error.
            try {
                // TODO: Log IKE header information and payload types

                List<IkePayload> supportedPayloadList =
                        decodePayloadList(firstPayloadType, header.isResponseMsg, decryptedBytes);

                header.validateInboundHeader(inputPacket.length);
                return new DecodeResultOk(
                        new IkeMessage(header, supportedPayloadList), firstPacket);
            } catch (NegativeArraySizeException | BufferUnderflowException e) {
                // Invalid length error when parsing payload bodies.
                return new DecodeResultProtectedError(
                        new InvalidSyntaxException("Malformed IKE Payload", e), firstPacket);
            } catch (IkeProtocolException e) {
                return new DecodeResultProtectedError(e, firstPacket);
            }
        }

        private Pair<IkeSkPayload, Integer> decryptAndAuthenticate(
                int expectedMsgId,
                IkeHeader header,
                byte[] inputPacket,
                @Nullable IkeMacIntegrity integrityMac,
                IkeCipher decryptCipher,
                byte[] integrityKey,
                byte[] decryptionKey)
                throws IkeException {

            try {
                if (header.messageId != expectedMsgId) {
                    throw new InvalidMessageIdException(header.messageId);
                }

                header.validateMajorVersion();

                boolean isSkf = header.nextPayloadType == IkePayload.PAYLOAD_TYPE_SKF;
                return IkePayloadFactory.getIkeSkPayload(
                        isSkf,
                        inputPacket,
                        integrityMac,
                        decryptCipher,
                        integrityKey,
                        decryptionKey);
            } catch (NegativeArraySizeException | BufferUnderflowException e) {
                throw new InvalidSyntaxException("Malformed IKE Payload", e);
            } catch (GeneralSecurityException e) {
                throw wrapAsIkeException(e);
            }
        }

        private void validateFragmentHeader(
                IkeHeader fragIkeHeader, int packetLen, DecodeResultPartial collectedFragments) {
            try {
                fragIkeHeader.validateInboundHeader(packetLen);
            } catch (IkeProtocolException e) {
                getIkeLog()
                        .e(
                                TAG,
                                "Received an IKE fragment with invalid header. Will be handled when"
                                        + " reassembly is done.",
                                e);
            }

            if (collectedFragments == null) return;
            if (fragIkeHeader.exchangeType != collectedFragments.ikeHeader.exchangeType) {
                getIkeLog()
                        .e(
                                TAG,
                                "Received an IKE fragment with different exchange type from"
                                        + " previously collected fragments. Ignore it.");
            }
        }

        private DecodeResultPartial processIkeFragment(
                IkeHeader header,
                byte[] inputPacket,
                IkeSkfPayload skf,
                int nextPayloadType,
                @Nullable DecodeResultPartial collectedFragments) {
            if (collectedFragments == null) {
                return new DecodeResultPartial(
                        header, inputPacket, skf, nextPayloadType, collectedFragments);
            }

            if (skf.totalFragments > collectedFragments.collectedFragsList.length) {
                getIkeLog()
                        .i(
                                TAG,
                                "Received IKE fragment has larger total fragments number. Discard"
                                        + " all previously collected fragments");
                return new DecodeResultPartial(
                        header, inputPacket, skf, nextPayloadType, null /*collectedFragments*/);
            }

            if (skf.totalFragments < collectedFragments.collectedFragsList.length) {
                getIkeLog()
                        .i(
                                TAG,
                                "Received IKE fragment has smaller total fragments number. Discard"
                                        + " it.");
                return collectedFragments;
            }

            if (collectedFragments.collectedFragsList[skf.fragmentNum - 1] != null) {
                getIkeLog().i(TAG, "Received IKE fragment is a replay.");
                return collectedFragments;
            }

            return new DecodeResultPartial(
                    header, inputPacket, skf, nextPayloadType, collectedFragments);
        }
    }

    /** Status to describe the result of decoding an inbound IKE message. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        DECODE_STATUS_OK,
        DECODE_STATUS_PARTIAL,
        DECODE_STATUS_PROTECTED_ERROR,
        DECODE_STATUS_UNPROTECTED_ERROR,
    })
    public @interface DecodeStatus {}

    /**
     * Represents a message that has been successfully (decrypted and) decoded or reassembled from
     * IKE fragments
     */
    public static final int DECODE_STATUS_OK = 0;
    /** Represents that reassembly process of IKE fragments has started but has not finished */
    public static final int DECODE_STATUS_PARTIAL = 1;
    /** Represents a crypto protected message with correct message ID but has parsing error. */
    public static final int DECODE_STATUS_PROTECTED_ERROR = 2;
    /**
     * Represents an unencrypted message with parsing error, an encrypted message with
     * authentication or decryption error, or any message with wrong message ID.
     */
    public static final int DECODE_STATUS_UNPROTECTED_ERROR = 3;

    /** This class represents common decoding result of an IKE message. */
    public abstract static class DecodeResult {
        public final int status;

        /** Construct an instance of DecodeResult. */
        protected DecodeResult(int status) {
            this.status = status;
        }
    }

    /** This class represents an IKE message has been successfully (decrypted and) decoded. */
    public static class DecodeResultOk extends DecodeResult {
        public final IkeMessage ikeMessage;
        public final byte[] firstPacket;

        public DecodeResultOk(IkeMessage ikeMessage, byte[] firstPacket) {
            super(DECODE_STATUS_OK);
            this.ikeMessage = ikeMessage;
            this.firstPacket = firstPacket;
        }
    }

    /**
     * This class represents IKE fragments are being reassembled to build a complete IKE message.
     *
     * <p>All IKE fragments should have the same IKE headers, except for the message length. This
     * class only stores the IKE header of the first arrived IKE fragment to represent the IKE
     * header of the complete IKE message. In this way we can verify all subsequent fragments'
     * headers against it.
     *
     * <p>The first payload type is only stored in the first fragment, as indicated in RFC 7383. So
     * this class only stores the next payload type field taken from the first fragment.
     */
    public static class DecodeResultPartial extends DecodeResult {
        public final int firstPayloadType;
        public final byte[] firstFragBytes;
        public final IkeHeader ikeHeader;
        public final byte[][] collectedFragsList;

        /**
         * Construct an instance of DecodeResultPartial with collected fragments and the newly
         * received fragment.
         *
         * <p>The newly received fragment has been validated against collected fragments during
         * decoding that all fragments have the same total fragments number and the newly received
         * fragment is not a replay.
         */
        public DecodeResultPartial(
                IkeHeader ikeHeader,
                byte[] inputPacket,
                IkeSkfPayload skfPayload,
                int nextPayloadType,
                @Nullable DecodeResultPartial collectedFragments) {
            super(DECODE_STATUS_PARTIAL);

            boolean isFirstFragment = 1 == skfPayload.fragmentNum;
            if (collectedFragments == null) {
                // First arrived IKE fragment
                this.ikeHeader = ikeHeader;
                this.firstPayloadType =
                        isFirstFragment ? nextPayloadType : IkePayload.PAYLOAD_TYPE_NO_NEXT;
                this.firstFragBytes = isFirstFragment ? inputPacket : null;
                this.collectedFragsList = new byte[skfPayload.totalFragments][];
            } else {
                this.ikeHeader = collectedFragments.ikeHeader;
                this.firstPayloadType =
                        isFirstFragment ? nextPayloadType : collectedFragments.firstPayloadType;
                this.firstFragBytes =
                        isFirstFragment ? inputPacket : collectedFragments.firstFragBytes;
                this.collectedFragsList = collectedFragments.collectedFragsList;
            }

            this.collectedFragsList[skfPayload.fragmentNum - 1] = skfPayload.getUnencryptedData();
        }

        /** Return if all IKE fragments have been collected */
        public boolean isAllFragmentsReceived() {
            for (byte[] frag : collectedFragsList) {
                if (frag == null) return false;
            }
            return true;
        }

        /** Reassemble all IKE fragments and return the unencrypted message body in byte array. */
        public byte[] reassembleAllFrags() {
            if (!isAllFragmentsReceived()) {
                throw new IllegalStateException("Not all fragments have been received");
            }

            int len = 0;
            for (byte[] frag : collectedFragsList) {
                len += frag.length;
            }

            ByteBuffer buffer = ByteBuffer.allocate(len);
            for (byte[] frag : collectedFragsList) {
                buffer.put(frag);
            }

            return buffer.array();
        }
    }

    /**
     * This class represents common information of error cases in decrypting and decoding message.
     */
    public abstract static class DecodeResultError extends DecodeResult {
        public final IkeException ikeException;

        protected DecodeResultError(int status, IkeException ikeException) {
            super(status);
            this.ikeException = ikeException;
        }
    }
    /**
     * This class represents that decoding errors have been found after the IKE message is
     * authenticated and decrypted.
     */
    public static class DecodeResultProtectedError extends DecodeResultError {
        public final byte[] firstPacket;

        public DecodeResultProtectedError(IkeException ikeException, byte[] firstPacket) {
            super(DECODE_STATUS_PROTECTED_ERROR, ikeException);
            this.firstPacket = firstPacket;
        }
    }
    /** This class represents errors have been found during message authentication or decryption. */
    public static class DecodeResultUnprotectedError extends DecodeResultError {
        public DecodeResultUnprotectedError(IkeException ikeException) {
            super(DECODE_STATUS_UNPROTECTED_ERROR, ikeException);
        }
    }

    /**
     * For setting mocked IIkeMessageHelper for testing
     *
     * @param helper the mocked IIkeMessageHelper
     */
    public static void setIkeMessageHelper(IIkeMessageHelper helper) {
        sIkeMessageHelper = helper;
    }
}

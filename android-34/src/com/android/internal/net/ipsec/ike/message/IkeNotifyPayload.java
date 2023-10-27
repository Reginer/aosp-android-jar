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
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_AUTHENTICATION_FAILED;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_CHILD_SA_NOT_FOUND;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_FAILED_CP_REQUIRED;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_INTERNAL_ADDRESS_FAILURE;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_INVALID_IKE_SPI;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_INVALID_KE_PAYLOAD;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_INVALID_MAJOR_VERSION;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_INVALID_MESSAGE_ID;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_INVALID_SELECTORS;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_INVALID_SYNTAX;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_NO_ADDITIONAL_SAS;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_NO_PROPOSAL_CHOSEN;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_SINGLE_PAIR_REQUIRED;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_TEMPORARY_FAILURE;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_TS_UNACCEPTABLE;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_UNSUPPORTED_CRITICAL_PAYLOAD;

import android.annotation.IntDef;
import android.net.ipsec.ike.exceptions.AuthenticationFailedException;
import android.net.ipsec.ike.exceptions.ChildSaNotFoundException;
import android.net.ipsec.ike.exceptions.FailedCpRequiredException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.exceptions.InternalAddressFailureException;
import android.net.ipsec.ike.exceptions.InvalidIkeSpiException;
import android.net.ipsec.ike.exceptions.InvalidKeException;
import android.net.ipsec.ike.exceptions.InvalidMajorVersionException;
import android.net.ipsec.ike.exceptions.InvalidMessageIdException;
import android.net.ipsec.ike.exceptions.InvalidSelectorsException;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;
import android.net.ipsec.ike.exceptions.NoAdditionalSasException;
import android.net.ipsec.ike.exceptions.NoValidProposalChosenException;
import android.net.ipsec.ike.exceptions.SinglePairRequiredException;
import android.net.ipsec.ike.exceptions.TemporaryFailureException;
import android.net.ipsec.ike.exceptions.TsUnacceptableException;
import android.net.ipsec.ike.exceptions.UnrecognizedIkeProtocolException;
import android.net.ipsec.ike.exceptions.UnsupportedCriticalPayloadException;
import android.util.ArraySet;
import android.util.SparseArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.util.Set;

/**
 * IkeNotifyPayload represents a Notify Payload.
 *
 * <p>As instructed by RFC 7296, for IKE SA concerned Notify Payload, Protocol ID and SPI Size must
 * be zero. Unrecognized notify message type must be ignored but should be logged.
 *
 * <p>Notification types that smaller or equal than ERROR_NOTIFY_TYPE_MAX are error types. The rest
 * of them are status types.
 *
 * <p>Critical bit for this payload must be ignored in received packet and must not be set in
 * outbound packet.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296">RFC 7296, Internet Key Exchange Protocol
 *     Version 2 (IKEv2)</a>
 */
public final class IkeNotifyPayload extends IkeInformationalPayload {
    private static final String TAG = IkeNotifyPayload.class.getSimpleName();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        NOTIFY_TYPE_INITIAL_CONTACT,
        NOTIFY_TYPE_ADDITIONAL_TS_POSSIBLE,
        NOTIFY_TYPE_IPCOMP_SUPPORTED,
        NOTIFY_TYPE_NAT_DETECTION_SOURCE_IP,
        NOTIFY_TYPE_NAT_DETECTION_DESTINATION_IP,
        NOTIFY_TYPE_USE_TRANSPORT_MODE,
        NOTIFY_TYPE_REKEY_SA,
        NOTIFY_TYPE_ESP_TFC_PADDING_NOT_SUPPORTED,
        NOTIFY_TYPE_EAP_ONLY_AUTHENTICATION,
        NOTIFY_TYPE_IKEV2_FRAGMENTATION_SUPPORTED,
        NOTIFY_TYPE_SIGNATURE_HASH_ALGORITHMS
    })
    public @interface NotifyType {}

    /**
     * Indicates that the sender supports INITIAL CONTACT functionality for the IKE Session. Only
     * allowed in the request of first IKE_AUTH exchange Note: Currently IKE only supports sending
     * this payload & will ignore the received payload
     */
    public static final int NOTIFY_TYPE_INITIAL_CONTACT = 16384;
    /**
     * Indicates that the responder has narrowed the proposed Traffic Selectors but other Traffic
     * Selectors would also have been acceptable. Only allowed in the response for negotiating a
     * Child SA.
     */
    public static final int NOTIFY_TYPE_ADDITIONAL_TS_POSSIBLE = 16386;
    /**
     * Indicates a willingness by its sender to use IPComp on this Child SA. Only allowed in the
     * request/response for negotiating a Child SA.
     */
    public static final int NOTIFY_TYPE_IPCOMP_SUPPORTED = 16387;
    /**
     * Used for detecting if the IKE initiator is behind a NAT. Only allowed in the request/response
     * of IKE_SA_INIT exchange.
     */
    public static final int NOTIFY_TYPE_NAT_DETECTION_SOURCE_IP = 16388;
    /**
     * Used for detecting if the IKE responder is behind a NAT. Only allowed in the request/response
     * of IKE_SA_INIT exchange.
     */
    public static final int NOTIFY_TYPE_NAT_DETECTION_DESTINATION_IP = 16389;
    /**
     * Might be sent by the IKE responder in an IKE_SA_INIT response, to prevent DoS Attacks. If
     * receiving it, IKE client MUST retry IKE_SA_INIT request with the same associated data.
     */
    public static final int NOTIFY_TYPE_COOKIE = 16390;
    /**
     * Indicates a willingness by its sender to use transport mode rather than tunnel mode on this
     * Child SA. Only allowed in the request/response for negotiating a Child SA.
     */
    public static final int NOTIFY_TYPE_USE_TRANSPORT_MODE = 16391;
    /**
     * Used for rekeying a Child SA or an IKE SA. Only allowed in the request/response of
     * CREATE_CHILD_SA exchange.
     */
    public static final int NOTIFY_TYPE_REKEY_SA = 16393;
    /**
     * Indicates that the sender will not accept packets that contain TFC padding over the Child SA
     * being negotiated. Only allowed in the request/response for negotiating a Child SA.
     */
    public static final int NOTIFY_TYPE_ESP_TFC_PADDING_NOT_SUPPORTED = 16394;
    /**
     * Indicates that the sender supports MOBIKE functionality for the IKE Session. Only allowed in
     * the request/response of IKE_AUTH exchange.
     */
    public static final int NOTIFY_TYPE_MOBIKE_SUPPORTED = 16396;
    /**
     * Used for notifying the Responder that an address change has occurred during a MOBIKE-enabled
     * IKE Session. Only allowed in Informational exchanges sent after the IKE_AUTH exchange has
     * finished.
     */
    public static final int NOTIFY_TYPE_UPDATE_SA_ADDRESSES = 16400;

    /**
     * Used in any INFORMATIONAL request for return routability check purposes when performing
     * MOBIKE.
     */
    public static final int NOTIFY_TYPE_COOKIE2 = 16401;

    /** Indicates that the sender prefers to use only eap based authentication */
    public static final int NOTIFY_TYPE_EAP_ONLY_AUTHENTICATION = 16417;

    /** Indicates that the sender supports IKE fragmentation. */
    public static final int NOTIFY_TYPE_IKEV2_FRAGMENTATION_SUPPORTED = 16430;

    /**
     * Indicates that the sender supports GENERIC_DIGITAL_SIGNATURE authentication payloads.
     *
     * <p>See RFC 7427 - Signature Authentication in the Internet Key Exchange Version 2 (IKEv2) for
     * more details
     */
    public static final int NOTIFY_TYPE_SIGNATURE_HASH_ALGORITHMS = 16431;

    private static final int NOTIFY_HEADER_LEN = 4;
    private static final int ERROR_NOTIFY_TYPE_MAX = 16383;

    private static final String NAT_DETECTION_DIGEST_ALGORITHM = "SHA-1";

    private static final int COOKIE_DATA_LEN_MIN = 1;
    private static final int COOKIE_DATA_LEN_MAX = 64;

    private static final int COOKIE2_DATA_LEN_MIN = 8;
    private static final int COOKIE2_DATA_LEN_MAX = 64;

    private static final Set<Integer> VALID_NOTIFY_TYPES_FOR_EXISTING_CHILD_SA;
    private static final Set<Integer> VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA;

    private static final SparseArray<String> NOTIFY_TYPE_TO_STRING;

    static {
        VALID_NOTIFY_TYPES_FOR_EXISTING_CHILD_SA = new ArraySet<>();
        VALID_NOTIFY_TYPES_FOR_EXISTING_CHILD_SA.add(ERROR_TYPE_INVALID_SELECTORS);
        VALID_NOTIFY_TYPES_FOR_EXISTING_CHILD_SA.add(ERROR_TYPE_CHILD_SA_NOT_FOUND);
        VALID_NOTIFY_TYPES_FOR_EXISTING_CHILD_SA.add(NOTIFY_TYPE_REKEY_SA);
    }

    static {
        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA = new ArraySet<>();
        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.add(IkeProtocolException.ERROR_TYPE_NO_PROPOSAL_CHOSEN);
        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.add(IkeProtocolException.ERROR_TYPE_INVALID_KE_PAYLOAD);
        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.add(
                IkeProtocolException.ERROR_TYPE_SINGLE_PAIR_REQUIRED);
        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.add(IkeProtocolException.ERROR_TYPE_NO_ADDITIONAL_SAS);
        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.add(
                IkeProtocolException.ERROR_TYPE_INTERNAL_ADDRESS_FAILURE);
        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.add(IkeProtocolException.ERROR_TYPE_FAILED_CP_REQUIRED);
        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.add(IkeProtocolException.ERROR_TYPE_TS_UNACCEPTABLE);

        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.add(NOTIFY_TYPE_ADDITIONAL_TS_POSSIBLE);
        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.add(NOTIFY_TYPE_IPCOMP_SUPPORTED);
        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.add(NOTIFY_TYPE_USE_TRANSPORT_MODE);
        VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.add(NOTIFY_TYPE_ESP_TFC_PADDING_NOT_SUPPORTED);
    }

    static {
        NOTIFY_TYPE_TO_STRING = new SparseArray<>();
        NOTIFY_TYPE_TO_STRING.put(
                ERROR_TYPE_UNSUPPORTED_CRITICAL_PAYLOAD, "Unsupported critical payload");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_INVALID_IKE_SPI, "Invalid IKE SPI");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_INVALID_MAJOR_VERSION, "Invalid major version");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_INVALID_SYNTAX, "Invalid syntax");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_INVALID_MESSAGE_ID, "Invalid message ID");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_NO_PROPOSAL_CHOSEN, "No proposal chosen");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_INVALID_KE_PAYLOAD, "Invalid KE payload");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_AUTHENTICATION_FAILED, "Authentication failed");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_SINGLE_PAIR_REQUIRED, "Single pair required");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_NO_ADDITIONAL_SAS, "No additional SAs");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_INTERNAL_ADDRESS_FAILURE, "Internal address failure");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_FAILED_CP_REQUIRED, "Failed CP required");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_TS_UNACCEPTABLE, "TS unacceptable");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_INVALID_SELECTORS, "Invalid selectors");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_TEMPORARY_FAILURE, "Temporary failure");
        NOTIFY_TYPE_TO_STRING.put(ERROR_TYPE_CHILD_SA_NOT_FOUND, "Child SA not found");

        NOTIFY_TYPE_TO_STRING.put(NOTIFY_TYPE_ADDITIONAL_TS_POSSIBLE, "Additional TS possible");
        NOTIFY_TYPE_TO_STRING.put(NOTIFY_TYPE_IPCOMP_SUPPORTED, "IPCOMP supported");
        NOTIFY_TYPE_TO_STRING.put(NOTIFY_TYPE_NAT_DETECTION_SOURCE_IP, "NAT detection source IP");
        NOTIFY_TYPE_TO_STRING.put(
                NOTIFY_TYPE_NAT_DETECTION_DESTINATION_IP, "NAT detection destination IP");
        NOTIFY_TYPE_TO_STRING.put(NOTIFY_TYPE_COOKIE, "COOKIE");
        NOTIFY_TYPE_TO_STRING.put(NOTIFY_TYPE_USE_TRANSPORT_MODE, "Use transport mode");
        NOTIFY_TYPE_TO_STRING.put(NOTIFY_TYPE_REKEY_SA, "Rekey SA");
        NOTIFY_TYPE_TO_STRING.put(
                NOTIFY_TYPE_ESP_TFC_PADDING_NOT_SUPPORTED, "ESP TFC Padding not supported");
        NOTIFY_TYPE_TO_STRING.put(NOTIFY_TYPE_MOBIKE_SUPPORTED, "MOBIKE supported");
        NOTIFY_TYPE_TO_STRING.put(NOTIFY_TYPE_UPDATE_SA_ADDRESSES, "UPDATE_SA_ADDRESSES");
        NOTIFY_TYPE_TO_STRING.put(NOTIFY_TYPE_COOKIE2, "COOKIE2");
        NOTIFY_TYPE_TO_STRING.put(
                NOTIFY_TYPE_IKEV2_FRAGMENTATION_SUPPORTED, "Fragmentation supported");
        NOTIFY_TYPE_TO_STRING.put(
                NOTIFY_TYPE_SIGNATURE_HASH_ALGORITHMS, "Generic Digital Signatures supported");
    }

    public final int protocolId;
    public final byte spiSize;
    public final int notifyType;
    public final int spi;
    public final byte[] notifyData;

    /**
     * Construct an instance of IkeNotifyPayload in the context of IkePayloadFactory
     *
     * @param critical indicates if this payload is critical. Ignored in supported payload as
     *     instructed by the RFC 7296.
     * @param payloadBody payload body in byte array
     * @throws IkeProtocolException if there is any error
     */
    IkeNotifyPayload(boolean isCritical, byte[] payloadBody) throws IkeProtocolException {
        super(PAYLOAD_TYPE_NOTIFY, isCritical);

        ByteBuffer inputBuffer = ByteBuffer.wrap(payloadBody);

        protocolId = Byte.toUnsignedInt(inputBuffer.get());
        spiSize = inputBuffer.get();
        notifyType = Short.toUnsignedInt(inputBuffer.getShort());

        // Validate syntax of spiSize, protocolId and notifyType.
        // Reference: <https://tools.ietf.org/html/rfc7296#page-100>
        if (spiSize == SPI_LEN_IPSEC) {
            // For message concerning existing Child SA
            validateNotifyPayloadForExistingChildSa();
            spi = inputBuffer.getInt();

        } else if (spiSize == SPI_LEN_NOT_INCLUDED) {
            // For message concerning IKE SA or for new Child SA that to be negotiated.
            validateNotifyPayloadForIkeAndNewChild();
            spi = SPI_NOT_INCLUDED;

        } else {
            throw new InvalidSyntaxException("Invalid SPI Size: " + spiSize);
        }

        notifyData = new byte[payloadBody.length - NOTIFY_HEADER_LEN - spiSize];
        inputBuffer.get(notifyData);
    }

    private void validateNotifyPayloadForExistingChildSa() throws InvalidSyntaxException {
        if (protocolId != PROTOCOL_ID_AH && protocolId != PROTOCOL_ID_ESP) {
            throw new InvalidSyntaxException(
                    "Expected Procotol ID AH(2) or ESP(3): Protocol ID is " + protocolId);
        }

        if (!VALID_NOTIFY_TYPES_FOR_EXISTING_CHILD_SA.contains(notifyType)) {
            throw new InvalidSyntaxException(
                    "Expected Notify Type for existing Child SA: Notify Type is " + notifyType);
        }
    }

    private void validateNotifyPayloadForIkeAndNewChild() throws InvalidSyntaxException {
        if (protocolId != PROTOCOL_ID_UNSET) {
            getIkeLog().w(TAG, "Expected Procotol ID unset: Protocol ID is " + protocolId);
        }

        if (notifyType == ERROR_TYPE_INVALID_SELECTORS
                || notifyType == ERROR_TYPE_CHILD_SA_NOT_FOUND) {
            throw new InvalidSyntaxException(
                    "Expected Notify Type concerning IKE SA or new Child SA under negotiation"
                            + ": Notify Type is "
                            + notifyType);
        }
    }

    /**
     * Generate NAT DETECTION notification data.
     *
     * <p>This method calculates NAT DETECTION notification data which is a SHA-1 digest of the IKE
     * initiator's SPI, IKE responder's SPI, IP address and port. Source address and port should be
     * used for generating NAT_DETECTION_SOURCE_IP data. Destination address and port should be used
     * for generating NAT_DETECTION_DESTINATION_IP data. Here "source" and "destination" mean the
     * direction of this IKE message.
     *
     * @param initiatorIkeSpi the SPI of IKE initiator
     * @param responderIkeSpi the SPI of IKE responder
     * @param ipAddress the IP address
     * @param port the port
     * @return the generated NAT DETECTION notification data as a byte array.
     */
    public static byte[] generateNatDetectionData(
            long initiatorIkeSpi, long responderIkeSpi, InetAddress ipAddress, int port) {
        byte[] rawIpAddr = ipAddress.getAddress();

        ByteBuffer byteBuffer =
                ByteBuffer.allocate(2 * SPI_LEN_IKE + rawIpAddr.length + IP_PORT_LEN);
        byteBuffer
                .putLong(initiatorIkeSpi)
                .putLong(responderIkeSpi)
                .put(rawIpAddr)
                .putShort((short) port);

        try {
            MessageDigest natDetectionDataDigest =
                    MessageDigest.getInstance(NAT_DETECTION_DIGEST_ALGORITHM);
            return natDetectionDataDigest.digest(byteBuffer.array());
        } catch (NoSuchAlgorithmException e) {
            throw new ProviderException(
                    "Failed to obtain algorithm :" + NAT_DETECTION_DIGEST_ALGORITHM, e);
        }
    }

    private static IkeNotifyPayload handleCookieAndGenerateCopy(
            IkeNotifyPayload cookie2Notify, int minLen, int maxLen) throws InvalidSyntaxException {
        byte[] notifyData = cookie2Notify.notifyData;
        if (notifyData.length < minLen || notifyData.length > maxLen) {
            String cookieType =
                    cookie2Notify.notifyType == NOTIFY_TYPE_COOKIE2 ? "COOKIE2" : "COOKIE";
            throw new InvalidSyntaxException(
                    "Invalid "
                            + cookieType
                            + " notification data with length "
                            + notifyData.length);
        }

        return new IkeNotifyPayload(cookie2Notify.notifyType, notifyData);
    }

    /** Validate inbound Cookie in IKE_INIT response and build a Cookie notify payload in request */
    public static IkeNotifyPayload handleCookieAndGenerateCopy(IkeNotifyPayload cookieNotify)
            throws InvalidSyntaxException {
        return handleCookieAndGenerateCopy(cookieNotify, COOKIE_DATA_LEN_MIN, COOKIE_DATA_LEN_MAX);
    }

    /** Validate inbound Cookie2 request and build a response Cookie2 notify payload */
    public static IkeNotifyPayload handleCookie2AndGenerateCopy(IkeNotifyPayload cookie2Notify)
            throws InvalidSyntaxException {
        return handleCookieAndGenerateCopy(
                cookie2Notify, COOKIE2_DATA_LEN_MIN, COOKIE2_DATA_LEN_MAX);
    }

    /**
     * Encode Notify payload to ByteBuffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param byteBuffer destination ByteBuffer that stores encoded payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        encodePayloadHeaderToByteBuffer(nextPayload, getPayloadLength(), byteBuffer);
        byteBuffer.put((byte) protocolId).put(spiSize).putShort((short) notifyType);
        if (spiSize == SPI_LEN_IPSEC) {
            byteBuffer.putInt(spi);
        }
        byteBuffer.put(notifyData);
    }

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        return GENERIC_HEADER_LENGTH + NOTIFY_HEADER_LEN + spiSize + notifyData.length;
    }

    protected IkeNotifyPayload(
            @ProtocolId int protocolId, byte spiSize, int spi, int notifyType, byte[] notifyData) {
        super(PAYLOAD_TYPE_NOTIFY, false);
        this.protocolId = protocolId;
        this.spiSize = spiSize;
        this.spi = spi;
        this.notifyType = notifyType;
        this.notifyData = notifyData;
    }

    /**
     * Construct IkeNotifyPayload concerning either an IKE SA, or Child SA that is going to be
     * negotiated with associated notification data.
     *
     * @param notifyType the notify type concerning IKE SA
     * @param notifytData status or error data transmitted. Values for this field are notify type
     *     specific.
     */
    public IkeNotifyPayload(int notifyType, byte[] notifyData) {
        this(PROTOCOL_ID_UNSET, SPI_LEN_NOT_INCLUDED, SPI_NOT_INCLUDED, notifyType, notifyData);
        try {
            validateNotifyPayloadForIkeAndNewChild();
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Construct IkeNotifyPayload concerning either an IKE SA, or Child SA that is going to be
     * negotiated without additional notification data.
     *
     * @param notifyType the notify type concerning IKE SA
     */
    public IkeNotifyPayload(int notifyType) {
        this(notifyType, new byte[0]);
    }

    /**
     * Construct IkeNotifyPayload concerning existing Child SA
     *
     * @param notifyType the notify type concerning Child SA
     * @param notifytData status or error data transmitted. Values for this field are notify type
     *     specific.
     */
    public IkeNotifyPayload(
            @ProtocolId int protocolId, int spi, int notifyType, byte[] notifyData) {
        this(protocolId, SPI_LEN_IPSEC, spi, notifyType, notifyData);
        try {
            validateNotifyPayloadForExistingChildSa();
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Indicates if this is an error notification payload.
     *
     * @return if this is an error notification payload.
     */
    public boolean isErrorNotify() {
        return notifyType <= ERROR_NOTIFY_TYPE_MAX;
    }

    /**
     * Indicates if this is an notification for a new Child SA negotiation.
     *
     * <p>This notification may provide additional configuration information for negotiating a new
     * Child SA or is an error notification of the Child SA negotiation failure.
     *
     * @return if this is an notification for a new Child SA negotiation.
     */
    public boolean isNewChildSaNotify() {
        return VALID_NOTIFY_TYPES_FOR_NEW_CHILD_SA.contains(notifyType);
    }

    /**
     * Validate error data and build IkeProtocolException for this error notification.
     *
     * @return the IkeProtocolException that represents this error.
     * @throws InvalidSyntaxException if error data has invalid size.
     */
    public IkeProtocolException validateAndBuildIkeException() throws InvalidSyntaxException {
        if (!isErrorNotify()) {
            throw new IllegalArgumentException(
                    "Do not support building IkeException for a non-error notificaton. Notify"
                            + " type: "
                            + notifyType);
        }

        try {
            switch (notifyType) {
                case ERROR_TYPE_UNSUPPORTED_CRITICAL_PAYLOAD:
                    return new UnsupportedCriticalPayloadException(notifyData);
                case ERROR_TYPE_INVALID_IKE_SPI:
                    return new InvalidIkeSpiException(notifyData);
                case ERROR_TYPE_INVALID_MAJOR_VERSION:
                    return new InvalidMajorVersionException(notifyData);
                case ERROR_TYPE_INVALID_SYNTAX:
                    return new InvalidSyntaxException(notifyData);
                case ERROR_TYPE_INVALID_MESSAGE_ID:
                    return new InvalidMessageIdException(notifyData);
                case ERROR_TYPE_NO_PROPOSAL_CHOSEN:
                    return new NoValidProposalChosenException(notifyData);
                case ERROR_TYPE_INVALID_KE_PAYLOAD:
                    return new InvalidKeException(notifyData);
                case ERROR_TYPE_AUTHENTICATION_FAILED:
                    return new AuthenticationFailedException(notifyData);
                case ERROR_TYPE_SINGLE_PAIR_REQUIRED:
                    return new SinglePairRequiredException(notifyData);
                case ERROR_TYPE_NO_ADDITIONAL_SAS:
                    return new NoAdditionalSasException(notifyData);
                case ERROR_TYPE_INTERNAL_ADDRESS_FAILURE:
                    return new InternalAddressFailureException(notifyData);
                case ERROR_TYPE_FAILED_CP_REQUIRED:
                    return new FailedCpRequiredException(notifyData);
                case ERROR_TYPE_TS_UNACCEPTABLE:
                    return new TsUnacceptableException(notifyData);
                case ERROR_TYPE_INVALID_SELECTORS:
                    return new InvalidSelectorsException(spi, notifyData);
                case ERROR_TYPE_TEMPORARY_FAILURE:
                    return new TemporaryFailureException(notifyData);
                case ERROR_TYPE_CHILD_SA_NOT_FOUND:
                    return new ChildSaNotFoundException(spi, notifyData);
                default:
                    return new UnrecognizedIkeProtocolException(notifyType, notifyData);
            }
        } catch (IllegalArgumentException e) {
            // Notification data length is invalid.
            throw new InvalidSyntaxException(e);
        }
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        String notifyTypeString = NOTIFY_TYPE_TO_STRING.get(notifyType);

        if (notifyTypeString == null) {
            return "Notify(" + notifyType + ")";
        }
        return "Notify(" + notifyTypeString + ")";
    }
}

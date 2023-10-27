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

package com.android.internal.net.eap.message.mschapv2;

import static com.android.internal.net.eap.EapAuthenticator.LOG;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.exceptions.mschapv2.EapMsChapV2ParsingException;
import com.android.internal.net.eap.message.EapMessage;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * EapMsChapV2TypeData represents the Type Data for an {@link EapMessage} during an EAP MSCHAPv2
 * session.
 */
public class EapMsChapV2TypeData {
    private static final int LABEL_VALUE_LENGTH = 2;
    private static final String ASCII_CHARSET_NAME = "US-ASCII";
    private static final String MESSAGE_PREFIX = "M=";
    private static final String MESSAGE_LABEL = "M";

    // EAP MSCHAPv2 OpCode values (EAP MSCHAPv2#2)
    public static final int EAP_MSCHAP_V2_CHALLENGE = 1;
    public static final int EAP_MSCHAP_V2_RESPONSE = 2;
    public static final int EAP_MSCHAP_V2_SUCCESS = 3;
    public static final int EAP_MSCHAP_V2_FAILURE = 4;
    public static final int EAP_MSCHAP_V2_CHANGE_PASSWORD = 7;

    public static final Map<Integer, String> EAP_OP_CODE_STRING = new HashMap<>();
    static {
        EAP_OP_CODE_STRING.put(EAP_MSCHAP_V2_CHALLENGE, "Challenge");
        EAP_OP_CODE_STRING.put(EAP_MSCHAP_V2_RESPONSE, "Response");
        EAP_OP_CODE_STRING.put(EAP_MSCHAP_V2_SUCCESS, "Success");
        EAP_OP_CODE_STRING.put(EAP_MSCHAP_V2_FAILURE, "Failure");
        EAP_OP_CODE_STRING.put(EAP_MSCHAP_V2_CHANGE_PASSWORD, "Change-Password");
    }

    private static final Set<Integer> SUPPORTED_OP_CODES = new HashSet<>();
    static {
        SUPPORTED_OP_CODES.add(EAP_MSCHAP_V2_CHALLENGE);
        SUPPORTED_OP_CODES.add(EAP_MSCHAP_V2_RESPONSE);
        SUPPORTED_OP_CODES.add(EAP_MSCHAP_V2_SUCCESS);
        SUPPORTED_OP_CODES.add(EAP_MSCHAP_V2_FAILURE);
    }

    public final int opCode;

    EapMsChapV2TypeData(int opCode) throws EapMsChapV2ParsingException {
        this.opCode = opCode;

        if (!SUPPORTED_OP_CODES.contains(opCode)) {
            throw new EapMsChapV2ParsingException("Unsupported opCode provided: " + opCode);
        }
    }

    /**
     * Encodes this EapMsChapV2TypeData instance as a byte[].
     *
     * @return byte[] representing the encoded value of this EapMsChapV2TypeData instance.
     */
    public byte[] encode() {
        throw new UnsupportedOperationException(
                "encode() not supported by " + this.getClass().getSimpleName());
    }

    abstract static class EapMsChapV2VariableTypeData extends EapMsChapV2TypeData {
        public final int msChapV2Id;
        public final int msLength;

        EapMsChapV2VariableTypeData(int opCode, int msChapV2Id, int msLength)
                throws EapMsChapV2ParsingException {
            super(opCode);

            this.msChapV2Id = msChapV2Id;
            this.msLength = msLength;
        }
    }

    /**
     * EapMsChapV2ChallengeRequest represents the EAP MSCHAPv2 Challenge Packet (EAP MSCHAPv2#2.1).
     */
    public static class EapMsChapV2ChallengeRequest extends EapMsChapV2VariableTypeData {
        public static final int VALUE_SIZE = 16;
        public static final int TYPE_DATA_HEADER_SIZE = 5;

        public final byte[] challenge = new byte[VALUE_SIZE];
        public final byte[] name;

        EapMsChapV2ChallengeRequest(ByteBuffer buffer) throws EapMsChapV2ParsingException {
            super(
                    EAP_MSCHAP_V2_CHALLENGE,
                    Byte.toUnsignedInt(buffer.get()),
                    Short.toUnsignedInt(buffer.getShort()));

            int valueSize = Byte.toUnsignedInt(buffer.get());
            if (valueSize != VALUE_SIZE) {
                throw new EapMsChapV2ParsingException("Challenge Value-Size must be 16");
            }
            buffer.get(challenge);

            int nameLenBytes = msLength - VALUE_SIZE - TYPE_DATA_HEADER_SIZE;
            if (nameLenBytes < 0) {
                throw new EapMsChapV2ParsingException("Invalid MS-Length specified");
            }

            name = new byte[nameLenBytes];
            buffer.get(name);
        }

        @VisibleForTesting
        public EapMsChapV2ChallengeRequest(
                int msChapV2Id, int msLength, byte[] challenge, byte[] name)
                throws EapMsChapV2ParsingException {
            super(EAP_MSCHAP_V2_CHALLENGE, msChapV2Id, msLength);

            if (challenge.length != VALUE_SIZE) {
                throw new EapMsChapV2ParsingException("Challenge length must be 16");
            }

            System.arraycopy(challenge, 0, this.challenge, 0, VALUE_SIZE);
            this.name = name;
        }
    }

    /**
     * EapMsChapV2ChallengeResponse represents the EAP MSCHAPv2 Response Packet (EAP MSCHAPv2#2.2).
     */
    public static class EapMsChapV2ChallengeResponse extends EapMsChapV2VariableTypeData {
        public static final int VALUE_SIZE = 49;
        public static final int PEER_CHALLENGE_SIZE = 16;
        public static final int RESERVED_BYTES = 8;
        public static final int NT_RESPONSE_SIZE = 24;
        public static final int TYPE_DATA_HEADER_SIZE = 5;

        public final byte[] peerChallenge = new byte[PEER_CHALLENGE_SIZE];
        public final byte[] ntResponse = new byte[NT_RESPONSE_SIZE];
        public final int flags;
        public final byte[] name;

        public EapMsChapV2ChallengeResponse(
                int msChapV2Id, byte[] peerChallenge, byte[] ntResponse, int flags, byte[] name)
                throws EapMsChapV2ParsingException {
            super(
                    EAP_MSCHAP_V2_RESPONSE,
                    msChapV2Id,
                    TYPE_DATA_HEADER_SIZE + VALUE_SIZE + name.length);

            if (peerChallenge.length != PEER_CHALLENGE_SIZE) {
                throw new EapMsChapV2ParsingException("Peer-Challenge must be 16B");
            } else if (ntResponse.length != NT_RESPONSE_SIZE) {
                throw new EapMsChapV2ParsingException("NT-Response must be 24B");
            } else if (flags != 0) {
                throw new EapMsChapV2ParsingException("Flags must be 0x00");
            }

            System.arraycopy(peerChallenge, 0, this.peerChallenge, 0, PEER_CHALLENGE_SIZE);
            System.arraycopy(ntResponse, 0, this.ntResponse, 0, NT_RESPONSE_SIZE);
            this.flags = flags;
            this.name = name;
        }

        @Override
        public byte[] encode() {
            ByteBuffer buffer = ByteBuffer.allocate(msLength);
            buffer.put((byte) EAP_MSCHAP_V2_RESPONSE);
            buffer.put((byte) msChapV2Id);
            buffer.putShort((short) msLength);
            buffer.put((byte) VALUE_SIZE);
            buffer.put(peerChallenge);
            buffer.put(new byte[RESERVED_BYTES]);
            buffer.put(ntResponse);
            buffer.put((byte) flags);
            buffer.put(name);

            return buffer.array();
        }
    }

    /**
     * EapMsChapV2SuccessRequest represents the EAP MSCHAPv2 Success Request Packet
     * (EAP MSCHAPv2#2.3).
     */
    public static class EapMsChapV2SuccessRequest extends EapMsChapV2VariableTypeData {
        private static final int AUTH_STRING_LEN_HEX = 40;
        private static final int AUTH_STRING_LEN_BYTES = 20;
        private static final int NUM_REQUIRED_ATTRIBUTES = 2;
        private static final String AUTH_STRING_LABEL = "S";

        public final byte[] authBytes = new byte[AUTH_STRING_LEN_BYTES];
        public final String message;

        EapMsChapV2SuccessRequest(ByteBuffer buffer) throws EapMsChapV2ParsingException {
            super(
                    EAP_MSCHAP_V2_SUCCESS,
                    Byte.toUnsignedInt(buffer.get()),
                    Short.toUnsignedInt(buffer.getShort()));

            byte[] message = new byte[buffer.remaining()];
            buffer.get(message);

            // message formatting: "S=<auth_string> M=<message>"
            Map<String, String> mappings =
                    getMessageMappings(new String(message, Charset.forName(ASCII_CHARSET_NAME)));

            if (!mappings.containsKey(AUTH_STRING_LABEL)
                    || mappings.size() != NUM_REQUIRED_ATTRIBUTES) {
                throw new EapMsChapV2ParsingException(
                        "Auth message must be in the format: 'S=<auth_string> M=<message>'");
            }

            String authStringHex = mappings.get(AUTH_STRING_LABEL);
            if (authStringHex.length() != AUTH_STRING_LEN_HEX) {
                throw new EapMsChapV2ParsingException("Auth String must be 40 hex chars (20B)");
            }
            byte[] authBytes = hexStringToByteArray(authStringHex);
            System.arraycopy(authBytes, 0, this.authBytes, 0, AUTH_STRING_LEN_BYTES);

            this.message = mappings.get(MESSAGE_LABEL);
        }

        @VisibleForTesting
        public EapMsChapV2SuccessRequest(
                int msChapV2Id, int msLength, byte[] authBytes, String message)
                throws EapMsChapV2ParsingException {
            super(EAP_MSCHAP_V2_SUCCESS, msChapV2Id, msLength);

            if (authBytes.length != AUTH_STRING_LEN_BYTES) {
                throw new EapMsChapV2ParsingException("Auth String must be 20B");
            }

            System.arraycopy(authBytes, 0, this.authBytes, 0, AUTH_STRING_LEN_BYTES);
            this.message = message;
        }
    }

    /**
     * EapMsChapV2SuccessResponse represents the EAP MSCHAPv2 Success Response Packet
     * (EAP MSCHAPv2#2.4).
     */
    public static class EapMsChapV2SuccessResponse extends EapMsChapV2TypeData {
        private EapMsChapV2SuccessResponse() throws EapMsChapV2ParsingException {
            super(EAP_MSCHAP_V2_SUCCESS);
        }

        /**
         * Constructs and returns a new EAP MSCHAPv2 Success Response type data.
         *
         * @return a new EapMsChapV2SuccessResponse instance
         */
        public static EapMsChapV2SuccessResponse getEapMsChapV2SuccessResponse() {
            try {
                return new EapMsChapV2SuccessResponse();
            } catch (EapMsChapV2ParsingException ex) {
                // This should never happen
                LOG.wtf(
                        EapMsChapV2SuccessResponse.class.getSimpleName(),
                        "ParsingException thrown while creating EapMsChapV2SuccessResponse",
                        ex);
                return null;
            }
        }

        @Override
        public byte[] encode() {
            return new byte[] {(byte) EAP_MSCHAP_V2_SUCCESS};
        }
    }

    /**
     * EapMsChapV2FailureRequest represents the EAP MSCHAPv2 Failure Request Packet
     * (EAP MSCHAPv2#2.5).
     */
    public static class EapMsChapV2FailureRequest extends EapMsChapV2VariableTypeData {
        private static final int NUM_REQUIRED_ATTRIBUTES = 5;
        private static final int CHALLENGE_LENGTH = 16;
        private static final String ERROR_LABEL = "E";
        private static final String RETRY_LABEL = "R";
        private static final String IS_RETRYABLE_FLAG = "1";
        private static final String CHALLENGE_LABEL = "C";
        private static final String PASSWORD_CHANGE_PROTOCOL_LABEL = "V";

        // Error codes defined in EAP MSCHAPv2#2.5
        public static final Map<Integer, String> EAP_ERROR_CODE_STRING = new HashMap<>();
        static {
            EAP_ERROR_CODE_STRING.put(646, "ERROR_RESTRICTED_LOGON_HOURS");
            EAP_ERROR_CODE_STRING.put(647, "ERROR_ACCT_DISABLED");
            EAP_ERROR_CODE_STRING.put(648, "ERROR_PASSWD_EXPIRED");
            EAP_ERROR_CODE_STRING.put(649, "ERROR_NO_DIALIN_PERMISSION");
            EAP_ERROR_CODE_STRING.put(691, "ERROR_AUTHENTICATION_FAILURE");
            EAP_ERROR_CODE_STRING.put(709, "ERROR_CHANGING_PASSWORD");
        }

        public final int errorCode;
        public final boolean isRetryable;
        public final byte[] challenge;
        public final int passwordChangeProtocol;
        public final String message;

        EapMsChapV2FailureRequest(ByteBuffer buffer)
                throws EapMsChapV2ParsingException, NumberFormatException {
            super(
                    EAP_MSCHAP_V2_FAILURE,
                    Byte.toUnsignedInt(buffer.get()),
                    Short.toUnsignedInt(buffer.getShort()));

            byte[] message = new byte[buffer.remaining()];
            buffer.get(message);

            // message formatting:
            // "E=<error_code> R=<retry bit> C=<challenge> V=<password_change_protocol> M=<message>"
            Map<String, String> mappings =
                    getMessageMappings(new String(message, Charset.forName(ASCII_CHARSET_NAME)));
            if (!mappings.containsKey(ERROR_LABEL)
                    || !mappings.containsKey(RETRY_LABEL)
                    || !mappings.containsKey(CHALLENGE_LABEL)
                    || !mappings.containsKey(PASSWORD_CHANGE_PROTOCOL_LABEL)
                    || mappings.size() != NUM_REQUIRED_ATTRIBUTES) {
                throw new EapMsChapV2ParsingException(
                        "Message must be formatted as: E=<error_code> R=<retry bit> C=<challenge>"
                            + " V=<password_change_protocol> M=<message>");
            }

            this.errorCode = Integer.parseInt(mappings.get(ERROR_LABEL));
            this.isRetryable = IS_RETRYABLE_FLAG.equals(mappings.get(RETRY_LABEL));
            this.challenge = hexStringToByteArray(mappings.get(CHALLENGE_LABEL));
            this.passwordChangeProtocol = Integer.parseInt(mappings.get(
                    PASSWORD_CHANGE_PROTOCOL_LABEL));
            this.message = mappings.get("M");

            if (challenge.length != CHALLENGE_LENGTH) {
                throw new EapMsChapV2ParsingException("Challenge must be 16B long");
            }
        }

        @VisibleForTesting
        public EapMsChapV2FailureRequest(
                int msChapV2Id,
                int msLength,
                int errorCode,
                boolean isRetryable,
                byte[] challenge,
                int passwordChangeProtocol,
                String message)
                throws EapMsChapV2ParsingException {
            super(EAP_MSCHAP_V2_FAILURE, msChapV2Id, msLength);

            this.errorCode = errorCode;
            this.isRetryable = isRetryable;
            this.challenge = challenge;
            this.passwordChangeProtocol = passwordChangeProtocol;
            this.message = message;

            if (challenge.length != CHALLENGE_LENGTH) {
                throw new EapMsChapV2ParsingException("Challenge length must be 16B");
            }
        }
    }

    /**
     * EapMsChapV2FailureResponse represents the EAP MSCHAPv2 Failure Response Packet
     * (EAP MSCHAPv2#2.6).
     */
    public static class EapMsChapV2FailureResponse extends EapMsChapV2TypeData {
        private EapMsChapV2FailureResponse() throws EapMsChapV2ParsingException {
            super(EAP_MSCHAP_V2_FAILURE);
        }

        /**
         * Constructs and returns a new EAP MSCHAPv2 Failure Response type data.
         *
         * @return a new EapMsChapV2FailureResponse instance
         */
        public static EapMsChapV2FailureResponse getEapMsChapV2FailureResponse() {
            try {
                return new EapMsChapV2FailureResponse();
            } catch (EapMsChapV2ParsingException ex) {
                // This should never happen
                LOG.wtf(
                        EapMsChapV2SuccessResponse.class.getSimpleName(),
                        "ParsingException thrown while creating EapMsChapV2FailureResponse",
                        ex);
                return null;
            }
        }

        @Override
        public byte[] encode() {
            return new byte[] {(byte) EAP_MSCHAP_V2_FAILURE};
        }
    }

    @VisibleForTesting
    static Map<String, String> getMessageMappings(String message)
            throws EapMsChapV2ParsingException {
        Map<String, String> messageMappings = new HashMap<>();
        int mPos = message.indexOf(MESSAGE_PREFIX);

        String preMString;
        if (mPos == -1) {
            preMString = message;
            messageMappings.put(MESSAGE_LABEL, "<omitted by authenticator>");
        } else {
            preMString = message.substring(0, mPos);
            messageMappings.put(MESSAGE_LABEL, message.substring(mPos + MESSAGE_PREFIX.length()));
        }

        // preMString: "S=<auth string> " or "E=<error> R=r C=<challenge> V=<version> "
        for (String value : preMString.split(" ")) {
            String[] keyValue = value.split("=");
            if (keyValue.length != LABEL_VALUE_LENGTH) {
                throw new EapMsChapV2ParsingException(
                        "Message must be formatted <label character>=<value>");
            } else if (messageMappings.containsKey(keyValue[0])) {
                throw new EapMsChapV2ParsingException(
                        "Duplicated key-value pair in message: " + LOG.pii(message));
            }
            messageMappings.put(keyValue[0], keyValue[1]);
        }

        return messageMappings;
    }

    @VisibleForTesting
    static byte[] hexStringToByteArray(String hexString)
            throws EapMsChapV2ParsingException, NumberFormatException {
        if (hexString.length() % 2 != 0) {
            throw new EapMsChapV2ParsingException(
                    "Hex string must contain an even number of characters");
        }

        byte[] dataBytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            dataBytes[i / 2] = (byte) Integer.parseInt(hexString.substring(i, i + 2), 16);
        }
        return dataBytes;
    }

    /** Class for decoding EAP MSCHAPv2 type data. */
    public static class EapMsChapV2TypeDataDecoder {
        /**
         * Returns the EAP MSCHAPv2 Op Code for the given EAP type data.
         *
         * @param eapTypeData byte[] type data to read the Op Code from
         * @return the EAP MSCHAPv2 Op Code for the current type data
         * @throws BufferUnderflowException iff eapTypeData.length == 0
         */
        public int getOpCode(byte[] eapTypeData) throws BufferUnderflowException {
            return Byte.toUnsignedInt(ByteBuffer.wrap(eapTypeData).get());
        }

        /**
         * Decodes and returns an EapMsChapV2ChallengeRequest for the specified eapTypeData.
         *
         * @param tag String for logging tag
         * @param eapTypeData byte[] to be decoded as an EapMsChapV2ChallengeRequest instance
         * @return DecodeResult wrapping an EapMsChapV2ChallengeRequest instance for the given
         *     eapTypeData iff the eapTypeData is formatted correctly. Otherwise, the DecodeResult
         *     wraps the appropriate EapError.
         */
        public DecodeResult<EapMsChapV2ChallengeRequest> decodeChallengeRequest(
                String tag, byte[] eapTypeData) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(eapTypeData);
                int opCode = Byte.toUnsignedInt(buffer.get());

                if (opCode != EAP_MSCHAP_V2_CHALLENGE) {
                    return new DecodeResult<>(
                            new EapError(
                                    new EapMsChapV2ParsingException(
                                            "Received type data with invalid opCode: "
                                                    + EAP_OP_CODE_STRING.getOrDefault(
                                                            opCode, "Unknown (" + opCode + ")"))));
                }

                return new DecodeResult<>(new EapMsChapV2ChallengeRequest(buffer));
            } catch (BufferUnderflowException | EapMsChapV2ParsingException ex) {
                LOG.e(tag, "Error parsing EAP MSCHAPv2 Challenge Request type data", ex);
                return new DecodeResult<>(new EapError(ex));
            }
        }

        /**
         * Decodes and returns an EapMsChapV2SuccessRequest for the specified eapTypeData.
         *
         * @param tag String for logging tag
         * @param eapTypeData byte[] to be decoded as an EapMsChapV2SuccessRequest instance
         * @return DecodeResult wrapping an EapMsChapV2SuccessRequest instance for the given
         *     eapTypeData iff the eapTypeData is formatted correctly. Otherwise, the DecodeResult
         *     wraps the appropriate EapError.
         */
        public DecodeResult<EapMsChapV2SuccessRequest> decodeSuccessRequest(
                String tag, byte[] eapTypeData) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(eapTypeData);
                int opCode = Byte.toUnsignedInt(buffer.get());

                if (opCode != EAP_MSCHAP_V2_SUCCESS) {
                    return new DecodeResult<>(
                            new EapError(
                                    new EapMsChapV2ParsingException(
                                            "Received type data with invalid opCode: "
                                                    + EAP_OP_CODE_STRING.getOrDefault(
                                                            opCode, "Unknown (" + opCode + ")"))));
                }

                return new DecodeResult<>(new EapMsChapV2SuccessRequest(buffer));
            } catch (BufferUnderflowException
                    | NumberFormatException
                    | EapMsChapV2ParsingException ex) {
                LOG.e(tag, "Error parsing EAP MSCHAPv2 Success Request type data", ex);
                return new DecodeResult<>(new EapError(ex));
            }
        }

        /**
         * Decodes and returns an EapMsChapV2FailureRequest for the specified eapTypeData.
         *
         * @param tag String for logging tag
         * @param eapTypeData byte[] to be decoded as an EapMsChapV2FailureRequest instance
         * @return DecodeResult wrapping an EapMsChapV2FailureRequest instance for the given
         *     eapTypeData iff the eapTypeData is formatted correctly. Otherwise, the DecodeResult
         *     wraps the appropriate EapError.
         */
        public DecodeResult<EapMsChapV2FailureRequest> decodeFailureRequest(
                String tag, byte[] eapTypeData) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(eapTypeData);
                int opCode = Byte.toUnsignedInt(buffer.get());

                if (opCode != EAP_MSCHAP_V2_FAILURE) {
                    return new DecodeResult<>(
                            new EapError(
                                    new EapMsChapV2ParsingException(
                                            "Received type data with invalid opCode: "
                                                    + EAP_OP_CODE_STRING.getOrDefault(
                                                            opCode, "Unknown (" + opCode + ")"))));
                }

                return new DecodeResult<>(new EapMsChapV2FailureRequest(buffer));
            } catch (BufferUnderflowException
                    | NumberFormatException
                    | EapMsChapV2ParsingException ex) {
                LOG.e(tag, "Error parsing EAP MSCHAPv2 Failure Request type data", ex);
                return new DecodeResult<>(new EapError(ex));
            }
        }

        /**
         * DecodeResult represents the result from calling a decode method within
         * EapMsChapV2TypeDataDecoder. It will contain either an EapMsChapV2TypeData or an EapError.
         *
         * @param <T> The EapMsChapV2TypeData type that is wrapped in this DecodeResult
         */
        public static class DecodeResult<T extends EapMsChapV2TypeData> {
            public final T eapTypeData;
            public final EapError eapError;

            public DecodeResult(T eapTypeData) {
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

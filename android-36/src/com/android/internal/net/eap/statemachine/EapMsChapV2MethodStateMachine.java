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

package com.android.internal.net.eap.statemachine;

import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_MSCHAP_V2;

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.message.EapData.EAP_NOTIFICATION;
import static com.android.internal.net.eap.message.EapData.EAP_TYPE_STRING;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_FAILURE;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_RESPONSE;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_SUCCESS;
import static com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EAP_MSCHAP_V2_FAILURE;
import static com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EAP_MSCHAP_V2_SUCCESS;
import static com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EAP_OP_CODE_STRING;
import static com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EapMsChapV2FailureRequest.EAP_ERROR_CODE_STRING;
import static com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EapMsChapV2FailureResponse.getEapMsChapV2FailureResponse;
import static com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EapMsChapV2SuccessResponse.getEapMsChapV2SuccessResponse;

import android.net.eap.EapSessionConfig.EapMethodConfig.EapMethod;
import android.net.eap.EapSessionConfig.EapMsChapV2Config;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.EapResult;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.EapResult.EapFailure;
import com.android.internal.net.eap.EapResult.EapResponse;
import com.android.internal.net.eap.EapResult.EapSuccess;
import com.android.internal.net.eap.crypto.ParityBitUtil;
import com.android.internal.net.eap.exceptions.EapInvalidRequestException;
import com.android.internal.net.eap.exceptions.EapSilentException;
import com.android.internal.net.eap.exceptions.mschapv2.EapMsChapV2ParsingException;
import com.android.internal.net.eap.message.EapData;
import com.android.internal.net.eap.message.EapMessage;
import com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData;
import com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EapMsChapV2ChallengeRequest;
import com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EapMsChapV2ChallengeResponse;
import com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EapMsChapV2FailureRequest;
import com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EapMsChapV2SuccessRequest;
import com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EapMsChapV2TypeDataDecoder;
import com.android.internal.net.eap.message.mschapv2.EapMsChapV2TypeData.EapMsChapV2TypeDataDecoder.DecodeResult;
import com.android.internal.net.utils.Log;

import org.bouncycastle.crypto.digests.MD4Digest;

import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * EapMsChapV2MethodStateMachine represents the valid paths possible for the EAP MSCHAPv2 protocol.
 *
 * <p>EAP MSCHAPv2 sessions will always follow the path:
 *
 * <p>CreatedState
 *      |
 *      +--> ChallengeState
 *             |
 *             +--> ValidateAuthenticatorState --+--> AwaitingEapSuccessState --> FinalState
 *                                               |
 *                                               +--> AwaitingEapFailureState --> FinalState
 *
 * <p>Note: All Failure-Request messages received in the PostChallenge state will be responded to
 * with Failure-Response messages. That is, retryable failures <i>will not</i> be retried.
 *
 * <p>Note: The EAP standard states that EAP methods may disallow EAP Notification messages for the
 * duration of the method (RFC 3748#5.2). EAP MSCHAPv2 does not explicitly ban these packets, so
 * they are allowed at any time (except once a terminal state is reached).
 *
 * @see <a href="https://tools.ietf.org/html/draft-kamath-pppext-eap-mschapv2-02">Microsoft EAP CHAP
 *     Extensions Draft (EAP MSCHAPv2)</a>
 * @see <a href="https://tools.ietf.org/html/rfc2759">RFC 2759, Microsoft PPP CHAP Extensions,
 *     Version 2 (MSCHAPv2)</a>
 * @see <a href="https://tools.ietf.org/html/rfc3079">RFC 3079, Deriving Keys for use with Microsoft
 *     Point-to-Point Encryption (MPPE)</a>
 * @hide
 */
public class EapMsChapV2MethodStateMachine extends EapMethodStateMachine {
    private static final String SHA_ALG = "SHA-1";
    private static final String DES_ALG = "DES/ECB/NoPadding";
    private static final String DES_KEY_FACTORY = "DES";
    private static final int PEER_CHALLENGE_SIZE = 16;
    private static final int CHALLENGE_HASH_LEN = 8;
    private static final int PASSWORD_HASH_LEN = 16;
    private static final int PASSWORD_HASH_HASH_LEN = 16;
    private static final int RESPONSE_LEN = 24;
    private static final int Z_PASSWORD_HASH_LEN = 21;
    private static final int Z_PASSWORD_SECTION_LEN = 7;
    private static final int RESPONSE_SECTION_LEN = 8;
    private static final int SHS_PAD_LEN = 40;
    private static final int MASTER_KEY_LEN = 16;
    private static final int SESSION_KEY_LEN = 16;

    // 32B (2 * SESSION_KEY_LEN) of data zero-padded to 64B
    private static final int MSK_LEN = MIN_MSK_LEN_BYTES;
    private static final int EMSK_LEN = MIN_EMSK_LEN_BYTES;

    // Reserved for future use and must be 0 (EAP MSCHAPv2#2.2)
    private static final int FLAGS = 0;

    // we all need a little magic in our lives
    // Defined in RFC 2759#8.7. Constants used for Success response generation.
    private static final byte[] CHALLENGE_MAGIC_1 = {
        (byte) 0x4D, (byte) 0x61, (byte) 0x67, (byte) 0x69, (byte) 0x63, (byte) 0x20, (byte) 0x73,
        (byte) 0x65, (byte) 0x72, (byte) 0x76, (byte) 0x65, (byte) 0x72, (byte) 0x20, (byte) 0x74,
        (byte) 0x6F, (byte) 0x20, (byte) 0x63, (byte) 0x6C, (byte) 0x69, (byte) 0x65, (byte) 0x6E,
        (byte) 0x74, (byte) 0x20, (byte) 0x73, (byte) 0x69, (byte) 0x67, (byte) 0x6E, (byte) 0x69,
        (byte) 0x6E, (byte) 0x67, (byte) 0x20, (byte) 0x63, (byte) 0x6F, (byte) 0x6E, (byte) 0x73,
        (byte) 0x74, (byte) 0x61, (byte) 0x6E, (byte) 0x74
    };
    private static final byte[] CHALLENGE_MAGIC_2 = {
        (byte) 0x50, (byte) 0x61, (byte) 0x64, (byte) 0x20, (byte) 0x74, (byte) 0x6F, (byte) 0x20,
        (byte) 0x6D, (byte) 0x61, (byte) 0x6B, (byte) 0x65, (byte) 0x20, (byte) 0x69, (byte) 0x74,
        (byte) 0x20, (byte) 0x64, (byte) 0x6F, (byte) 0x20, (byte) 0x6D, (byte) 0x6F, (byte) 0x72,
        (byte) 0x65, (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x61, (byte) 0x6E, (byte) 0x20,
        (byte) 0x6F, (byte) 0x6E, (byte) 0x65, (byte) 0x20, (byte) 0x69, (byte) 0x74, (byte) 0x65,
        (byte) 0x72, (byte) 0x61, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E
    };

    // Defined in RFC 3079#3.4. Constants used for Master Session Key (MSK) generation
    private static final byte[] SHS_PAD_1 = new byte[SHS_PAD_LEN];
    private static final byte[] SHS_PAD_2 = new byte[SHS_PAD_LEN];

    static {
        Arrays.fill(SHS_PAD_2, (byte) 0xF2);
    }

    private static final byte[] MSK_MAGIC_1 = {
        (byte) 0x54, (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x69,
        (byte) 0x73, (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x20,
        (byte) 0x4D, (byte) 0x50, (byte) 0x50, (byte) 0x45, (byte) 0x20, (byte) 0x4D,
        (byte) 0x61, (byte) 0x73, (byte) 0x74, (byte) 0x65, (byte) 0x72, (byte) 0x20,
        (byte) 0x4B, (byte) 0x65, (byte) 0x79
    };
    private static final byte[] MSK_MAGIC_2 = {
        (byte) 0x4F, (byte) 0x6E, (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x65,
        (byte) 0x20, (byte) 0x63, (byte) 0x6C, (byte) 0x69, (byte) 0x65, (byte) 0x6E,
        (byte) 0x74, (byte) 0x20, (byte) 0x73, (byte) 0x69, (byte) 0x64, (byte) 0x65,
        (byte) 0x2C, (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x69, (byte) 0x73,
        (byte) 0x20, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x74, (byte) 0x68,
        (byte) 0x65, (byte) 0x20, (byte) 0x73, (byte) 0x65, (byte) 0x6E, (byte) 0x64,
        (byte) 0x20, (byte) 0x6B, (byte) 0x65, (byte) 0x79, (byte) 0x3B, (byte) 0x20,
        (byte) 0x6F, (byte) 0x6E, (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x65,
        (byte) 0x20, (byte) 0x73, (byte) 0x65, (byte) 0x72, (byte) 0x76, (byte) 0x65,
        (byte) 0x72, (byte) 0x20, (byte) 0x73, (byte) 0x69, (byte) 0x64, (byte) 0x65,
        (byte) 0x2C, (byte) 0x20, (byte) 0x69, (byte) 0x74, (byte) 0x20, (byte) 0x69,
        (byte) 0x73, (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x20,
        (byte) 0x72, (byte) 0x65, (byte) 0x63, (byte) 0x65, (byte) 0x69, (byte) 0x76,
        (byte) 0x65, (byte) 0x20, (byte) 0x6B, (byte) 0x65, (byte) 0x79, (byte) 0x2E
    };
    private static final byte[] MSK_MAGIC_3 = {
        (byte) 0x4F, (byte) 0x6E, (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x65,
        (byte) 0x20, (byte) 0x63, (byte) 0x6C, (byte) 0x69, (byte) 0x65, (byte) 0x6E,
        (byte) 0x74, (byte) 0x20, (byte) 0x73, (byte) 0x69, (byte) 0x64, (byte) 0x65,
        (byte) 0x2C, (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x69, (byte) 0x73,
        (byte) 0x20, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x74, (byte) 0x68,
        (byte) 0x65, (byte) 0x20, (byte) 0x72, (byte) 0x65, (byte) 0x63, (byte) 0x65,
        (byte) 0x69, (byte) 0x76, (byte) 0x65, (byte) 0x20, (byte) 0x6B, (byte) 0x65,
        (byte) 0x79, (byte) 0x3B, (byte) 0x20, (byte) 0x6F, (byte) 0x6E, (byte) 0x20,
        (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x20, (byte) 0x73, (byte) 0x65,
        (byte) 0x72, (byte) 0x76, (byte) 0x65, (byte) 0x72, (byte) 0x20, (byte) 0x73,
        (byte) 0x69, (byte) 0x64, (byte) 0x65, (byte) 0x2C, (byte) 0x20, (byte) 0x69,
        (byte) 0x74, (byte) 0x20, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x74,
        (byte) 0x68, (byte) 0x65, (byte) 0x20, (byte) 0x73, (byte) 0x65, (byte) 0x6E,
        (byte) 0x64, (byte) 0x20, (byte) 0x6B, (byte) 0x65, (byte) 0x79, (byte) 0x2E
    };

    private final EapMsChapV2Config mEapMsChapV2Config;
    private final SecureRandom mSecureRandom;
    private final EapMsChapV2TypeDataDecoder mTypeDataDecoder;

    public EapMsChapV2MethodStateMachine(
            EapMsChapV2Config eapMsChapV2Config, SecureRandom secureRandom) {
        this(eapMsChapV2Config, secureRandom, new EapMsChapV2TypeDataDecoder());
    }

    @VisibleForTesting
    EapMsChapV2MethodStateMachine(
            EapMsChapV2Config eapMsChapV2Config,
            SecureRandom secureRandom,
            EapMsChapV2TypeDataDecoder eapMsChapV2TypeDataDecoder) {
        this.mEapMsChapV2Config = eapMsChapV2Config;
        this.mSecureRandom = secureRandom;
        this.mTypeDataDecoder = eapMsChapV2TypeDataDecoder;

        transitionTo(new CreatedState());
    }

    @Override
    @EapMethod
    int getEapMethod() {
        return EAP_TYPE_MSCHAP_V2;
    }

    @Override
    EapResult handleEapNotification(String tag, EapMessage message) {
        return EapStateMachine.handleNotification(tag, message);
    }

    protected class CreatedState extends EapMethodState {
        private final String mTAG = this.getClass().getSimpleName();

        @Override
        public EapResult process(EapMessage message) {
            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            DecodeResult<EapMsChapV2ChallengeRequest> decodeResult =
                    mTypeDataDecoder.decodeChallengeRequest(mTAG, message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                return decodeResult.eapError;
            }

            return transitionAndProcess(new ChallengeState(), message);
        }
    }

    protected class ChallengeState extends EapMethodState {
        private final String mTAG = this.getClass().getSimpleName();

        @Override
        public EapResult process(EapMessage message) {
            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            DecodeResult<EapMsChapV2ChallengeRequest> decodeResult =
                    mTypeDataDecoder.decodeChallengeRequest(mTAG, message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                return decodeResult.eapError;
            }

            EapMsChapV2ChallengeRequest challengeRequest = decodeResult.eapTypeData;
            LOG.d(
                    mTAG,
                    "Received Challenge Request:"
                            + " Challenge=" + LOG.pii(challengeRequest.challenge)
                            + " Server-Name=" + Log.byteArrayToHexString(challengeRequest.name));

            byte[] peerChallenge = new byte[PEER_CHALLENGE_SIZE];
            mSecureRandom.nextBytes(peerChallenge);

            byte[] ntResponse;
            try {
                ntResponse =
                        generateNtResponse(
                                challengeRequest.challenge,
                                peerChallenge,
                                mEapMsChapV2Config.getUsername(),
                                mEapMsChapV2Config.getPassword());
            } catch (GeneralSecurityException ex) {
                LOG.e(mTAG, "Error generating EAP MSCHAPv2 Challenge response", ex);
                return new EapError(ex);
            }

            LOG.d(
                    mTAG,
                    "Generating Challenge Response:"
                            + " Username=" + LOG.pii(mEapMsChapV2Config.getUsername())
                            + " Peer-Challenge=" + LOG.pii(peerChallenge)
                            + " NT-Response=" + LOG.pii(ntResponse));

            try {
                EapMsChapV2ChallengeResponse challengeResponse =
                        new EapMsChapV2ChallengeResponse(
                                challengeRequest.msChapV2Id,
                                peerChallenge,
                                ntResponse,
                                FLAGS,
                                usernameToBytes(mEapMsChapV2Config.getUsername()));
                transitionTo(
                        new ValidateAuthenticatorState(
                                challengeRequest.challenge, peerChallenge, ntResponse));

                return buildEapMessageResponse(mTAG, message.eapIdentifier, challengeResponse);
            } catch (EapMsChapV2ParsingException ex) {
                LOG.e(mTAG, "Error building response type data", ex);
                return new EapError(ex);
            }
        }
    }

    protected class ValidateAuthenticatorState extends EapMethodState {
        private final String mTAG = this.getClass().getSimpleName();

        private final byte[] mAuthenticatorChallenge;
        private final byte[] mPeerChallenge;
        private final byte[] mNtResponse;

        @VisibleForTesting
        ValidateAuthenticatorState(
                byte[] authenticatorChallenge, byte[] peerChallenge, byte[] ntResponse) {
            this.mAuthenticatorChallenge = authenticatorChallenge;
            this.mPeerChallenge = peerChallenge;
            this.mNtResponse = ntResponse;
        }

        @Override
        public EapResult process(EapMessage message) {
            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            int opCode;
            try {
                opCode = mTypeDataDecoder.getOpCode(message.eapData.eapTypeData);
            } catch (BufferUnderflowException ex) {
                LOG.e(mTAG, "Empty type data received in ValidateAuthenticatorState", ex);
                return new EapError(ex);
            }

            LOG.d(
                    mTAG,
                    "Received Op Code: "
                            + EAP_OP_CODE_STRING.getOrDefault(opCode, "Unknown")
                            + " (" + opCode + ")");

            switch (opCode) {
                case EAP_MSCHAP_V2_SUCCESS:
                    DecodeResult<EapMsChapV2SuccessRequest> successDecodeResult =
                            mTypeDataDecoder.decodeSuccessRequest(
                                    mTAG, message.eapData.eapTypeData);
                    if (!successDecodeResult.isSuccessfulDecode()) {
                        return successDecodeResult.eapError;
                    }

                    EapMsChapV2SuccessRequest successRequest = successDecodeResult.eapTypeData;
                    LOG.d(
                            mTAG,
                            "Received SuccessRequest:"
                                    + " Auth-String=" + LOG.pii(successRequest.authBytes)
                                    + " Message=" + successRequest.message);

                    boolean isSuccessfulAuth;
                    try {
                        isSuccessfulAuth =
                                checkAuthenticatorResponse(
                                        mEapMsChapV2Config.getPassword(),
                                        mNtResponse,
                                        mPeerChallenge,
                                        mAuthenticatorChallenge,
                                        mEapMsChapV2Config.getUsername(),
                                        successRequest.authBytes);
                    } catch (GeneralSecurityException | UnsupportedEncodingException ex) {
                        LOG.e(mTAG, "Error validating MSCHAPv2 Authenticator Response", ex);
                        return new EapError(ex);
                    }

                    if (!isSuccessfulAuth) {
                        LOG.e(
                                mTAG,
                                "Authenticator Response does not match expected response value");
                        transitionTo(new FinalState());
                        return new EapFailure();
                    }

                    transitionTo(new AwaitingEapSuccessState(mNtResponse));
                    return buildEapMessageResponse(
                            mTAG, message.eapIdentifier, getEapMsChapV2SuccessResponse());

                case EAP_MSCHAP_V2_FAILURE:
                    DecodeResult<EapMsChapV2FailureRequest> failureDecodeResult =
                            mTypeDataDecoder.decodeFailureRequest(
                                    mTAG, message.eapData.eapTypeData);
                    if (!failureDecodeResult.isSuccessfulDecode()) {
                        return failureDecodeResult.eapError;
                    }

                    EapMsChapV2FailureRequest failureRequest = failureDecodeResult.eapTypeData;
                    int errorCode = failureRequest.errorCode;
                    LOG.e(
                            mTAG,
                            String.format(
                                    "Received MSCHAPv2 Failure-Request: E=%s (%d) R=%b V=%d M=%s",
                                    EAP_ERROR_CODE_STRING.getOrDefault(errorCode, "UNKNOWN"),
                                    errorCode,
                                    failureRequest.isRetryable,
                                    failureRequest.passwordChangeProtocol,
                                    failureRequest.message));
                    transitionTo(new AwaitingEapFailureState());
                    return buildEapMessageResponse(
                            mTAG, message.eapIdentifier, getEapMsChapV2FailureResponse());

                default:
                    LOG.e(mTAG, "Invalid OpCode: " + opCode);
                    return new EapError(
                            new EapInvalidRequestException(
                                    "Unexpected request received in EAP MSCHAPv2"));
            }
        }
    }

    protected class AwaitingEapSuccessState extends EapMethodState {
        private final String mTAG = this.getClass().getSimpleName();

        private final byte[] mNtResponse;

        AwaitingEapSuccessState(byte[] ntResponse) {
            this.mNtResponse = ntResponse;
        }

        @Override
        public EapResult process(EapMessage message) {
            if (message.eapCode == EAP_CODE_FAILURE) {
                LOG.e(mTAG, "Received EAP-Failure in PreSuccessState");
                transitionTo(new FinalState());
                return new EapFailure();
            } else if (message.eapCode != EAP_CODE_SUCCESS) {
                int eapType = message.eapData.eapType;
                if (eapType == EAP_NOTIFICATION) {
                    return handleEapNotification(mTAG, message);
                } else {
                    LOG.e(
                            mTAG,
                            "Received unexpected EAP message. Type="
                                    + EAP_TYPE_STRING.getOrDefault(
                                            eapType, "UNKNOWN (" + eapType + ")"));
                    return new EapError(
                            new EapInvalidRequestException(
                                    "Expected EAP Type "
                                            + getEapMethod()
                                            + ", received "
                                            + eapType));
                }
            }

            try {
                byte[] msk = generateMsk(mEapMsChapV2Config.getPassword(), mNtResponse);
                transitionTo(new FinalState());
                return new EapSuccess(msk, new byte[EMSK_LEN]);
            } catch (GeneralSecurityException | UnsupportedEncodingException ex) {
                LOG.e(mTAG, "Error generating MSK for EAP MSCHAPv2", ex);
                return new EapError(ex);
            }
        }
    }

    protected class AwaitingEapFailureState extends EapMethodState {
        private final String mTAG = this.getClass().getSimpleName();

        @Override
        public EapResult process(EapMessage message) {
            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }
            int eapType = message.eapData.eapType;
            LOG.e(
                    mTAG,
                    "Received unexpected EAP message. Type="
                            + EAP_TYPE_STRING.getOrDefault(eapType, "UNKNOWN (" + eapType + ")"));
            return new EapError(
                    new EapInvalidRequestException(
                            "Expected EAP Type " + getEapMethod() + ", received " + eapType));
        }
    }

    private EapResult buildEapMessageResponse(
            String tag, int eapIdentifier, EapMsChapV2TypeData typeData) {
        try {
            EapData eapData = new EapData(getEapMethod(), typeData.encode());
            EapMessage eapMessage = new EapMessage(EAP_CODE_RESPONSE, eapIdentifier, eapData);
            return EapResponse.getEapResponse(eapMessage);
        } catch (EapSilentException ex) {
            LOG.e(tag, "Error building response EapMessage", ex);
            return new EapError(ex);
        }
    }

    /** Util for converting String username to "0-to-256 char username", as used in RFC 2759#8. */
    @VisibleForTesting
    static byte[] usernameToBytes(String username) {
        return username.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Util for converting String password to "0-to-256-unicode-char password", as used in
     * RFC 2759#8.
     */
    @VisibleForTesting
    static byte[] passwordToBytes(String password) {
        return password.getBytes(StandardCharsets.UTF_16LE);
    }

    /* Implementation of RFC 2759#8.1: GenerateNTResponse() */
    @VisibleForTesting
    static byte[] generateNtResponse(
            byte[] authenticatorChallenge, byte[] peerChallenge, String username, String password)
            throws GeneralSecurityException {
        byte[] challenge = challengeHash(peerChallenge, authenticatorChallenge, username);
        byte[] passwordHash = ntPasswordHash(password);
        return challengeResponse(challenge, passwordHash);
    }

    /* Implementation of RFC 2759#8.2: ChallengeHash() */
    @VisibleForTesting
    static byte[] challengeHash(
            byte[] peerChallenge, byte[] authenticatorChallenge, String username)
            throws GeneralSecurityException {
        MessageDigest sha1 = MessageDigest.getInstance(SHA_ALG);
        sha1.update(peerChallenge);
        sha1.update(authenticatorChallenge);
        sha1.update(usernameToBytes(username));
        return Arrays.copyOf(sha1.digest(), CHALLENGE_HASH_LEN);
    }

    /* Implementation of RFC 2759#8.3: NtPasswordHash() */
    @VisibleForTesting
    static byte[] ntPasswordHash(String password) {
        MD4Digest md4 = new MD4Digest();
        byte[] passwordBytes = passwordToBytes(password);
        md4.update(passwordBytes, 0, passwordBytes.length);

        byte[] passwordHash = new byte[PASSWORD_HASH_LEN];
        md4.doFinal(passwordHash, 0);
        return passwordHash;
    }

    /* Implementation of RFC 2759#8.4: HashNtPasswordHash() */
    @VisibleForTesting
    static byte[] hashNtPasswordHash(byte[] passwordHash) {
        MD4Digest md4 = new MD4Digest();
        md4.update(passwordHash, 0, passwordHash.length);

        byte[] passwordHashHash = new byte[PASSWORD_HASH_HASH_LEN];
        md4.doFinal(passwordHashHash, 0);
        return passwordHashHash;
    }

    /* Implementation of RFC 2759#8.5: ChallengeResponse() */
    @VisibleForTesting
    static byte[] challengeResponse(byte[] challenge, byte[] passwordHash)
            throws GeneralSecurityException {
        byte[] zPasswordHash = Arrays.copyOf(passwordHash, Z_PASSWORD_HASH_LEN);

        ByteBuffer response = ByteBuffer.allocate(RESPONSE_LEN);
        for (int i = 0; i < 3; i++) {
            int from = i * Z_PASSWORD_SECTION_LEN;
            int to = from + Z_PASSWORD_SECTION_LEN;
            byte[] zPasswordSection = Arrays.copyOfRange(zPasswordHash, from, to);
            response.put(desEncrypt(challenge, zPasswordSection));
        }
        return response.array();
    }

    /* Implementation of RFC 2759#8.6: DesEncrypt() */
    @VisibleForTesting
    static byte[] desEncrypt(byte[] clear, byte[] key) throws GeneralSecurityException {
        if (key.length != Z_PASSWORD_SECTION_LEN) {
            throw new IllegalArgumentException("DES Key must be 7B before parity-bits are added");
        }

        key = ParityBitUtil.addParityBits(key);
        SecretKey secretKey =
                SecretKeyFactory.getInstance(DES_KEY_FACTORY).generateSecret(new DESKeySpec(key));

        Cipher des = Cipher.getInstance(DES_ALG);
        des.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] output = des.doFinal(clear);

        // RFC 2759#8.6 specifies 8B outputs for DesEncrypt()
        return Arrays.copyOf(output, RESPONSE_SECTION_LEN);
    }

    /**
     * Implementation of RFC 2759#8.7: GenerateAuthenticatorResponse()
     *
     * <p>Keep response as byte[] so checkAuthenticatorResponse() can easily compare byte[]'s
     */
    @VisibleForTesting
    static byte[] generateAuthenticatorResponse(
            String password,
            byte[] ntResponse,
            byte[] peerChallenge,
            byte[] authenticatorChallenge,
            String username)
            throws GeneralSecurityException, UnsupportedEncodingException {
        byte[] passwordHash = ntPasswordHash(password);
        byte[] passwordHashHash = hashNtPasswordHash(passwordHash);

        MessageDigest sha1 = MessageDigest.getInstance(SHA_ALG);
        sha1.update(passwordHashHash);
        sha1.update(ntResponse);
        sha1.update(CHALLENGE_MAGIC_1); // add just a dash of magic
        byte[] digest = sha1.digest();

        byte[] challenge = challengeHash(peerChallenge, authenticatorChallenge, username);

        sha1.update(digest);
        sha1.update(challenge);
        sha1.update(CHALLENGE_MAGIC_2);

        return sha1.digest();
    }

    /* Implementation of RFC 2759#8.8: CheckAuthenticatorResponse() */
    @VisibleForTesting
    static boolean checkAuthenticatorResponse(
            String password,
            byte[] ntResponse,
            byte[] peerChallenge,
            byte[] authenticatorChallenge,
            String userName,
            byte[] receivedResponse)
            throws GeneralSecurityException, UnsupportedEncodingException {
        byte[] myResponse =
                generateAuthenticatorResponse(
                        password, ntResponse, peerChallenge, authenticatorChallenge, userName);
        return Arrays.equals(myResponse, receivedResponse);
    }

    /* Implementation of RFC 3079#3.4: GetMasterKey() */
    @VisibleForTesting
    static byte[] getMasterKey(byte[] passwordHashHash, byte[] ntResponse)
            throws GeneralSecurityException {
        MessageDigest sha1 = MessageDigest.getInstance(SHA_ALG);
        sha1.update(passwordHashHash);
        sha1.update(ntResponse);
        sha1.update(MSK_MAGIC_1);
        return Arrays.copyOf(sha1.digest(), MASTER_KEY_LEN);
    }

    /* Implementation of RFC 3079#3.4: GetAsymmetricStartKey() */
    @VisibleForTesting
    static byte[] getAsymmetricStartKey(byte[] masterKey, boolean isSend)
            throws GeneralSecurityException {
        // salt: referred to as 's' in RFC 3079#3.4 GetAsymmetricStartKey()
        byte[] salt = isSend ? MSK_MAGIC_2 : MSK_MAGIC_3;
        MessageDigest sha1 = MessageDigest.getInstance(SHA_ALG);
        sha1.update(masterKey);
        sha1.update(SHS_PAD_1);
        sha1.update(salt);
        sha1.update(SHS_PAD_2);
        return Arrays.copyOf(sha1.digest(), SESSION_KEY_LEN);
    }

    @VisibleForTesting
    static byte[] generateMsk(String password, byte[] ntResponse)
            throws GeneralSecurityException, UnsupportedEncodingException {
        byte[] passwordHash = ntPasswordHash(password);
        byte[] passwordHashHash = hashNtPasswordHash(passwordHash);
        byte[] masterKey = getMasterKey(passwordHashHash, ntResponse);

        // MSK: SendKey + ReceiveKey (zero-padded to 64B)
        ByteBuffer msk = ByteBuffer.allocate(MSK_LEN);
        msk.put(getAsymmetricStartKey(masterKey, true /* isSend */));
        msk.put(getAsymmetricStartKey(masterKey, false /* isSend */));

        return msk.array();
    }
}

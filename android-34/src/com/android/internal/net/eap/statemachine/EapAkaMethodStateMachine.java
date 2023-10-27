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

import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_AKA;

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_SUCCESS;
import static com.android.internal.net.eap.message.simaka.EapAkaTypeData.EAP_AKA_AUTHENTICATION_REJECT;
import static com.android.internal.net.eap.message.simaka.EapAkaTypeData.EAP_AKA_CHALLENGE;
import static com.android.internal.net.eap.message.simaka.EapAkaTypeData.EAP_AKA_CLIENT_ERROR;
import static com.android.internal.net.eap.message.simaka.EapAkaTypeData.EAP_AKA_IDENTITY;
import static com.android.internal.net.eap.message.simaka.EapAkaTypeData.EAP_AKA_NOTIFICATION;
import static com.android.internal.net.eap.message.simaka.EapAkaTypeData.EAP_AKA_REAUTHENTICATION;
import static com.android.internal.net.eap.message.simaka.EapAkaTypeData.EAP_AKA_SYNCHRONIZATION_FAILURE;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_ANY_ID_REQ;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_AUTN;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_BIDDING;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_COUNTER;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_ENCR_DATA;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_FULLAUTH_ID_REQ;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_IV;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_MAC;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_NEXT_REAUTH_ID;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_NONCE_S;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_PERMANENT_ID_REQ;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_RAND;

import android.annotation.Nullable;
import android.content.Context;
import android.net.eap.EapAkaInfo;
import android.net.eap.EapSessionConfig.EapAkaConfig;
import android.net.eap.EapSessionConfig.EapMethodConfig.EapMethod;
import android.telephony.TelephonyManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.EapResult;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.EapResult.EapSuccess;
import com.android.internal.net.eap.EapSimAkaIdentityTracker;
import com.android.internal.net.eap.EapSimAkaIdentityTracker.ReauthInfo;
import com.android.internal.net.eap.crypto.Fips186_2Prf;
import com.android.internal.net.eap.exceptions.EapInvalidRequestException;
import com.android.internal.net.eap.exceptions.EapSilentException;
import com.android.internal.net.eap.exceptions.simaka.EapAkaInvalidAuthenticationResponse;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaAuthenticationFailureException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaIdentityUnavailableException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaInvalidAttributeException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaInvalidLengthException;
import com.android.internal.net.eap.message.EapMessage;
import com.android.internal.net.eap.message.simaka.EapAkaPrimeTypeData;
import com.android.internal.net.eap.message.simaka.EapAkaTypeData;
import com.android.internal.net.eap.message.simaka.EapAkaTypeData.EapAkaTypeDataDecoder;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtAutn;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtAuts;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtBidding;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtClientErrorCode;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtIdentity;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtRandAka;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtRes;
import com.android.internal.net.eap.message.simaka.EapSimAkaTypeData.DecodeResult;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * EapAkaMethodStateMachine represents the valid paths possible for the EAP-AKA protocol.
 *
 * <p>EAP-AKA sessions will always follow the path:
 *
 * Created --+--> Identity ->+--+>----> Challenge ------>+-> Final
 *           |               ^  v           ^            ^
 *           |               |  |           |            |
 *           +---------------+  +--> Re-authentication --+
 *
 * <p>Note: If the EAP-Request/AKA-Challenge message contains an AUTN with an invalid sequence
 * number, the peer will indicate a synchronization failure to the server and a new challenge will
 * be attempted.
 *
 * <p>Note: EAP-Request/Notification messages can be received at any point in the above state
 * machine At most one EAP-AKA/Notification message is allowed per EAP-AKA session.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4187">RFC 4187, Extensible Authentication Protocol
 *     for Authentication and Key Agreement (EAP-AKA)</a>
 */
class EapAkaMethodStateMachine extends EapSimAkaMethodStateMachine {
    private static final String TAG = EapAkaMethodStateMachine.class.getSimpleName();

    // EAP-AKA identity prefix (RFC 4187#4.1.1.6)
    private static final String AKA_IDENTITY_PREFIX = "0";

    private final EapAkaTypeDataDecoder mEapAkaTypeDataDecoder;
    private final EapSimAkaIdentityTracker mEapSimAkaIdentityTracker;
    private final boolean mSupportsEapAkaPrime;

    protected EapAkaMethodStateMachine(
            Context context, byte[] eapIdentity, EapAkaConfig eapAkaConfig) {
        this(context, eapIdentity, eapAkaConfig, false, null);
    }

    EapAkaMethodStateMachine(
            Context context,
            byte[] eapIdentity,
            EapAkaConfig eapAkaConfig,
            boolean supportsEapAkaPrime,
            SecureRandom secureRandom) {
        this(
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE),
                eapIdentity,
                eapAkaConfig,
                EapAkaTypeData.getEapAkaTypeDataDecoder(),
                supportsEapAkaPrime,
                secureRandom);
    }

    @VisibleForTesting
    protected EapAkaMethodStateMachine(
            TelephonyManager telephonyManager,
            byte[] eapIdentity,
            EapAkaConfig eapAkaConfig,
            EapAkaTypeDataDecoder eapAkaTypeDataDecoder,
            boolean supportsEapAkaPrime,
            SecureRandom secureRandom) {
        super(
                telephonyManager.createForSubscriptionId(eapAkaConfig.getSubId()),
                eapIdentity,
                eapAkaConfig);
        mEapAkaTypeDataDecoder = eapAkaTypeDataDecoder;
        mSupportsEapAkaPrime = supportsEapAkaPrime;
        mEapSimAkaIdentityTracker = EapSimAkaIdentityTracker.getInstance();
        mSecureRandom = secureRandom;
        transitionTo(new CreatedState());
    }

    private byte[] getReauthIdentity() {
        EapAkaConfig akaConfig = (EapAkaConfig) mEapUiccConfig;
        if (akaConfig.getEapAkaOption() != null
                && akaConfig.getEapAkaOption().getReauthId() != null) {
            return akaConfig.getEapAkaOption().getReauthId();
        }
        return null;
    }

    private ReauthInfo getAvailableReauthInfo(byte[] reauthId, byte[] eapId) {
        if (reauthId == null || eapId == null) {
            LOG.e(TAG, "getAvailableReauthInfo with NULL ID");
            return null;
        }
        String reauthIdentity = new String(reauthId, StandardCharsets.UTF_8);
        String permanentIdentity = new String(eapId, StandardCharsets.UTF_8);
        ReauthInfo reauthInfo =
                mEapSimAkaIdentityTracker.getReauthInfo(reauthIdentity, permanentIdentity);
        mEapSimAkaIdentityTracker.deleteReauthInfo(reauthIdentity, permanentIdentity);
        if (reauthInfo != null && reauthInfo.isValid()) {
            return reauthInfo;
        }
        return null;
    }

    @Override
    @EapMethod
    int getEapMethod() {
        return EAP_TYPE_AKA;
    }

    protected DecodeResult<EapAkaTypeData> decode(byte[] typeData) {
        return mEapAkaTypeDataDecoder.decode(typeData);
    }

    /**
     * This exists so we can override the identity prefix in the EapAkaPrimeMethodStateMachine.
     *
     * @return the Identity prefix for this EAP method
     */
    protected String getIdentityPrefix() {
        return AKA_IDENTITY_PREFIX;
    }

    protected ChallengeState buildChallengeState() {
        return new ChallengeState();
    }

    protected ChallengeState buildChallengeState(byte[] identity) {
        return new ChallengeState(identity);
    }

    protected ReauthState buildReauthState() {
        return new ReauthState();
    }

    protected ReauthState buildReauthState(byte[] identity, ReauthInfo reauthInfo) {
        return new ReauthState(identity, reauthInfo);
    }

    protected class CreatedState extends EapMethodState {
        private final String mTAG = CreatedState.class.getSimpleName();

        public EapResult process(EapMessage message) {
            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            DecodeResult<? extends EapAkaTypeData> decodeResult =
                    decode(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                return buildClientErrorResponse(
                        message.eapIdentifier, getEapMethod(), decodeResult.atClientErrorCode);
            }

            EapAkaTypeData eapAkaTypeData = decodeResult.eapTypeData;
            switch (eapAkaTypeData.eapSubtype) {
                case EAP_AKA_IDENTITY:
                    return transitionAndProcess(new IdentityState(), message);
                case EAP_AKA_CHALLENGE:
                    return transitionAndProcess(buildChallengeState(), message);
                case EAP_AKA_REAUTHENTICATION:
                    return transitionAndProcess(buildReauthState(), message);
                case EAP_AKA_NOTIFICATION:
                    return handleEapSimAkaNotification(
                            mTAG,
                            true, // isPreChallengeState
                            false, // isReauthState
                            false, // hadSuccessfulAuthentication
                            message.eapIdentifier,
                            0,
                            eapAkaTypeData);
                default:
                    return buildClientErrorResponse(
                            message.eapIdentifier,
                            getEapMethod(),
                            AtClientErrorCode.UNABLE_TO_PROCESS);
            }
        }
    }

    protected class IdentityState extends EapMethodState {
        private final String mTAG = IdentityState.class.getSimpleName();

        private byte[] mIdentity;
        private ReauthInfo mReauthInfo;
        private byte[] mReauthIdentity;

        IdentityState() {
            mReauthIdentity = getReauthIdentity();
            mReauthInfo = getAvailableReauthInfo(mReauthIdentity, mEapIdentity);
        }

        public EapResult process(EapMessage message) {
            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            DecodeResult<? extends EapAkaTypeData> decodeResult =
                    decode(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                return buildClientErrorResponse(
                        message.eapIdentifier, getEapMethod(), decodeResult.atClientErrorCode);
            }

            EapAkaTypeData eapAkaTypeData = decodeResult.eapTypeData;
            switch (eapAkaTypeData.eapSubtype) {
                case EAP_AKA_IDENTITY:
                    break;
                case EAP_AKA_CHALLENGE:
                    return transitionAndProcess(buildChallengeState(mIdentity), message);
                case EAP_AKA_REAUTHENTICATION:
                    return transitionAndProcess(
                            buildReauthState(mReauthIdentity, mReauthInfo), message);
                case EAP_AKA_NOTIFICATION:
                    return handleEapSimAkaNotification(
                            mTAG,
                            true, // isPreChallengeState
                            false, // isReauthState
                            false, // hadSuccessfulAuthentication
                            message.eapIdentifier,
                            0,
                            eapAkaTypeData);
                default:
                    return buildClientErrorResponse(
                            message.eapIdentifier,
                            getEapMethod(),
                            AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            if (!isValidIdentityAttributes(eapAkaTypeData)) {
                LOG.e(mTAG, "Invalid attributes: " + eapAkaTypeData.attributeMap.keySet());
                return buildClientErrorResponse(
                        message.eapIdentifier,
                        EAP_TYPE_AKA,
                        AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            byte[] identityToResponse;
            if (eapAkaTypeData.attributeMap.get(EAP_AT_ANY_ID_REQ) != null
                    && mReauthIdentity != null
                    && mReauthInfo != null) {

                identityToResponse = mReauthIdentity;
                LOG.d(
                        mTAG,
                        "Send EAP-AKA/Identity response with Reauth ID:"
                                + LOG.pii(new String(mIdentity, StandardCharsets.UTF_8)));
            } else {
                String imsi = mTelephonyManager.getSubscriberId();
                if (imsi == null) {
                    int subId = mEapUiccConfig.getSubId();
                    LOG.e(mTAG, "Unable to get IMSI for subId=" + subId);
                    return new EapError(
                            new EapSimAkaIdentityUnavailableException(
                                    "IMSI for subId (" + subId + ") not available"));
                }
                String identityString = getIdentityPrefix() + imsi;
                mIdentity = identityString.getBytes(StandardCharsets.US_ASCII);
                identityToResponse = mIdentity;
                LOG.d(mTAG, "Send EAP-AKA/Identity response with permanent ID:"
                        + LOG.pii(identityString));
            }

            AtIdentity atIdentity;
            try {
                atIdentity = AtIdentity.getAtIdentity(identityToResponse);
            } catch (EapSimAkaInvalidAttributeException ex) {
                LOG.wtf(mTAG, "Exception thrown while making AtIdentity attribute", ex);
                return new EapError(ex);
            }

            return buildResponseMessage(
                    getEapMethod(),
                    EAP_AKA_IDENTITY,
                    message.eapIdentifier,
                    Arrays.asList(atIdentity));
        }

        private boolean isValidIdentityAttributes(EapAkaTypeData eapAkaTypeData) {
            Set<Integer> attrs = eapAkaTypeData.attributeMap.keySet();

            // exactly one ID request type required
            int idRequests = 0;
            idRequests += attrs.contains(EAP_AT_PERMANENT_ID_REQ) ? 1 : 0;
            idRequests += attrs.contains(EAP_AT_ANY_ID_REQ) ? 1 : 0;
            idRequests += attrs.contains(EAP_AT_FULLAUTH_ID_REQ) ? 1 : 0;

            if (idRequests != 1) {
                return false;
            }

            // can't contain mac, iv, encr data
            if (attrs.contains(EAP_AT_MAC)
                    || attrs.contains(EAP_AT_IV)
                    || attrs.contains(EAP_AT_ENCR_DATA)) {
                return false;
            }
            return true;
        }
    }

    protected class ChallengeState extends EapMethodState {
        private final String mTAG = ChallengeState.class.getSimpleName();

        @VisibleForTesting boolean mHadSuccessfulChallenge = false;
        @VisibleForTesting protected final byte[] mIdentity;

        // IK and CK lengths defined as 16B (RFC 4187#1)
        private static final int IK_LEN_BYTES = 16;
        private static final int CK_LEN_BYTES = 16;

        // Tags for Successful and Synchronization responses
        private static final byte RAND_SUCCESS = (byte) 0xDB;
        private static final byte RAND_SYNCHRONIZATION = (byte) 0xDC;

        private byte[] mNextReauthIdentity;

        ChallengeState() {
            // use the EAP-Identity for the default value (RFC 4187#7)
            this(mEapIdentity);
        }

        ChallengeState(byte[] identity) {
            this.mIdentity = identity;
        }

        public EapResult process(EapMessage message) {
            if (message.eapCode == EAP_CODE_SUCCESS) {
                if (!mHadSuccessfulChallenge) {
                    LOG.e(mTAG, "Received unexpected EAP-Success");
                    return new EapError(
                            new EapInvalidRequestException(
                                    "Received an EAP-Success in the ChallengeState"));
                }

                transitionTo(new FinalState());
                return processEapSuccess(mNextReauthIdentity, 0 /* reauthCounter */);
            }

            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            DecodeResult<? extends EapAkaTypeData> decodeResult =
                    decode(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                return buildClientErrorResponse(
                        message.eapIdentifier, getEapMethod(), decodeResult.atClientErrorCode);
            }

            EapAkaTypeData eapAkaTypeData = decodeResult.eapTypeData;
            switch (eapAkaTypeData.eapSubtype) {
                case EAP_AKA_CHALLENGE:
                    break;
                case EAP_AKA_NOTIFICATION:
                    return handleEapSimAkaNotification(
                            mTAG,
                            false, // isPreChallengeState
                            false, // isReauthState
                            mHadSuccessfulChallenge, // hadSuccessfulAuthentication
                            message.eapIdentifier,
                            0,
                            eapAkaTypeData);
                default:
                    return buildClientErrorResponse(
                            message.eapIdentifier,
                            getEapMethod(),
                            AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            if (!isValidChallengeAttributes(eapAkaTypeData)) {
                LOG.e(mTAG, "Invalid attributes: " + eapAkaTypeData.attributeMap.keySet());
                return buildClientErrorResponse(
                        message.eapIdentifier, getEapMethod(), AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            return handleChallengeAuthentication(message, eapAkaTypeData);
        }

        protected EapResult handleChallengeAuthentication(
                EapMessage message, EapAkaTypeData eapAkaTypeData) {
            RandChallengeResult result;
            try {
                result = getRandChallengeResult(eapAkaTypeData);
            } catch (EapAkaInvalidAuthenticationResponse ex) {
                return new EapError(ex);
            } catch (EapSimAkaInvalidLengthException | BufferUnderflowException ex) {
                LOG.e(mTAG, "Invalid response returned from SIM", ex);
                return buildClientErrorResponse(
                        message.eapIdentifier, getEapMethod(), AtClientErrorCode.UNABLE_TO_PROCESS);
            } catch (EapSimAkaAuthenticationFailureException ex) {
                // Return EAP-Response/AKA-Authentication-Reject when the AUTN is rejected
                // (RFC 4187#6.3.1)
                return buildAuthenticationRejectMessage(message.eapIdentifier);
            }

            if (!result.isSuccessfulResult()) {
                try {
                    return buildResponseMessage(
                            getEapMethod(),
                            EAP_AKA_SYNCHRONIZATION_FAILURE,
                            message.eapIdentifier,
                            Arrays.asList(new AtAuts(result.auts)));
                } catch (EapSimAkaInvalidAttributeException ex) {
                    LOG.wtf(mTAG, "Error creating an AtAuts attr", ex);
                    return new EapError(ex);
                }
            }

            EapResult eapResult =
                    generateAndPersistEapAkaKeys(result, message.eapIdentifier, eapAkaTypeData);
            if (eapResult != null) {
                return eapResult;
            }

            try {
                if (!isValidMac(mTAG, message, eapAkaTypeData, new byte[0])) {
                    return buildClientErrorResponse(
                            message.eapIdentifier,
                            getEapMethod(),
                            AtClientErrorCode.UNABLE_TO_PROCESS);
                }
            } catch (GeneralSecurityException
                    | EapSilentException
                    | EapSimAkaInvalidAttributeException ex) {
                // if the MAC can't be generated, we can't continue
                LOG.e(mTAG, "Error computing MAC for EapMessage", ex);
                return new EapError(ex);
            }

            // before sending a response, check for bidding-down attacks (RFC 5448#4)
            if (mSupportsEapAkaPrime) {
                AtBidding atBidding = (AtBidding) eapAkaTypeData.attributeMap.get(EAP_AT_BIDDING);
                if (atBidding != null && atBidding.doesServerSupportEapAkaPrime) {
                    LOG.w(
                            mTAG,
                            "Potential bidding down attack. AT_BIDDING attr included and EAP-AKA'"
                                + " is supported");
                    return buildAuthenticationRejectMessage(message.eapIdentifier);
                }
            }

            // try to retrieve Next Reauth ID
            mNextReauthIdentity = retrieveNextReauthId(mTAG, eapAkaTypeData);

            // server has been authenticated, so we can send a response
            try {
                mHadSuccessfulChallenge = true;
                return buildResponseMessageWithMac(
                        message.eapIdentifier,
                        EAP_AKA_CHALLENGE,
                        new byte[0],
                        Arrays.asList(AtRes.getAtRes(result.res)),
                        (eapAkaTypeData instanceof EapAkaPrimeTypeData)
                                ? null /* no flags set */
                                : new int[] {
                                    EapResult.EapResponse.RESPONSE_FLAG_EAP_AKA_SERVER_AUTHENTICATED
                                });
            } catch (EapSimAkaInvalidAttributeException ex) {
                LOG.wtf(mTAG, "Error creating AtRes value", ex);
                return new EapError(ex);
            }
        }

        @VisibleForTesting
        class RandChallengeResult {
            public final byte[] res;
            public final byte[] ik;
            public final byte[] ck;
            public final byte[] auts;

            RandChallengeResult(byte[] res, byte[] ik, byte[] ck)
                    throws EapSimAkaInvalidLengthException {
                if (!AtRes.isValidResLen(res.length)) {
                    throw new EapSimAkaInvalidLengthException("Invalid RES length");
                } else if (ik.length != IK_LEN_BYTES) {
                    throw new EapSimAkaInvalidLengthException("Invalid IK length");
                } else if (ck.length != CK_LEN_BYTES) {
                    throw new EapSimAkaInvalidLengthException("Invalid CK length");
                }

                this.res = res;
                this.ik = ik;
                this.ck = ck;
                this.auts = null;
            }

            RandChallengeResult(byte[] auts) throws EapSimAkaInvalidLengthException {
                if (auts.length != AtAuts.AUTS_LENGTH) {
                    throw new EapSimAkaInvalidLengthException("Invalid AUTS length");
                }

                this.res = null;
                this.ik = null;
                this.ck = null;
                this.auts = auts;
            }

            private boolean isSuccessfulResult() {
                return res != null && ik != null && ck != null;
            }
        }

        private boolean isValidChallengeAttributes(EapAkaTypeData eapAkaTypeData) {
            Set<Integer> attrs = eapAkaTypeData.attributeMap.keySet();

            // must contain: AT_RAND, AT_AUTN, AT_MAC
            return attrs.contains(EAP_AT_RAND)
                    && attrs.contains(EAP_AT_AUTN)
                    && attrs.contains(EAP_AT_MAC);
        }

        private RandChallengeResult getRandChallengeResult(EapAkaTypeData eapAkaTypeData)
                throws EapSimAkaAuthenticationFailureException, EapSimAkaInvalidLengthException {
            AtRandAka atRandAka = (AtRandAka) eapAkaTypeData.attributeMap.get(EAP_AT_RAND);
            AtAutn atAutn = (AtAutn) eapAkaTypeData.attributeMap.get(EAP_AT_AUTN);

            // pre-Base64 formatting needs to be: [Length][RAND][Length][AUTN]
            int randLen = atRandAka.rand.length;
            int autnLen = atAutn.autn.length;
            ByteBuffer formattedChallenge = ByteBuffer.allocate(1 + randLen + 1 + autnLen);
            formattedChallenge.put((byte) randLen);
            formattedChallenge.put(atRandAka.rand);
            formattedChallenge.put((byte) autnLen);
            formattedChallenge.put(atAutn.autn);

            byte[] challengeResponse =
                    processUiccAuthentication(
                            mTAG,
                            TelephonyManager.AUTHTYPE_EAP_AKA,
                            formattedChallenge.array());
            ByteBuffer buffer = ByteBuffer.wrap(challengeResponse);
            byte tag = buffer.get();

            switch (tag) {
                case RAND_SUCCESS:
                    // response format: [tag][RES length][RES][CK length][CK][IK length][IK]
                    // (TS 131 102#7.1.2.1)
                    break;
                case RAND_SYNCHRONIZATION:
                    // response format: [tag][AUTS length][AUTS]
                    // (TS 131 102#7.1.2.1)
                    byte[] auts = new byte[Byte.toUnsignedInt(buffer.get())];
                    buffer.get(auts);

                    LOG.i(mTAG, "Synchronization Failure");
                    LOG.d(
                            mTAG,
                            "RAND=" + LOG.pii(atRandAka.rand)
                                    + " AUTN=" + LOG.pii(atAutn.autn)
                                    + " AUTS=" + LOG.pii(auts));

                    return new RandChallengeResult(auts);
                default:
                    throw new EapAkaInvalidAuthenticationResponse(
                            "Invalid tag for UICC response: " + String.format("%02X", tag));
            }

            byte[] res = new byte[Byte.toUnsignedInt(buffer.get())];
            buffer.get(res);

            byte[] ck = new byte[Byte.toUnsignedInt(buffer.get())];
            buffer.get(ck);

            byte[] ik = new byte[Byte.toUnsignedInt(buffer.get())];
            buffer.get(ik);

            LOG.d(mTAG, "RAND=" + LOG.pii(atRandAka.rand));
            LOG.d(mTAG, "AUTN=" + LOG.pii(atAutn.autn));
            LOG.d(mTAG, "RES=" + LOG.pii(res));
            LOG.d(mTAG, "IK=" + LOG.pii(ik));
            LOG.d(mTAG, "CK=" + LOG.pii(ck));

            return new RandChallengeResult(res, ik, ck);
        }

        protected EapResult buildAuthenticationRejectMessage(int eapIdentifier) {
            mIsExpectingEapFailure = true;
            return buildResponseMessage(
                    getEapMethod(),
                    EAP_AKA_AUTHENTICATION_REJECT,
                    eapIdentifier,
                    new ArrayList<>());
        }

        @Nullable
        protected EapResult generateAndPersistEapAkaKeys(
                RandChallengeResult result, int eapIdentifier, EapAkaTypeData eapAkaTypeData) {
            try {
                MessageDigest sha1 = MessageDigest.getInstance(MASTER_KEY_GENERATION_ALG);
                byte[] mkInputData = getMkInputData(result);
                generateAndPersistKeys(mTAG, sha1, new Fips186_2Prf(), mkInputData);
                return null;
            } catch (NoSuchAlgorithmException | BufferUnderflowException ex) {
                LOG.e(mTAG, "Error while creating keys", ex);
                return buildClientErrorResponse(
                        eapIdentifier, EAP_TYPE_AKA, AtClientErrorCode.UNABLE_TO_PROCESS);
            }
        }

        private byte[] getMkInputData(RandChallengeResult result) {
            int numInputBytes = mIdentity.length + result.ik.length + result.ck.length;
            ByteBuffer buffer = ByteBuffer.allocate(numInputBytes);
            buffer.put(mIdentity);
            buffer.put(result.ik);
            buffer.put(result.ck);
            return buffer.array();
        }
    }

    protected class ReauthState extends EapMethodState {
        private final String mTAG = ReauthState.class.getSimpleName();

        @VisibleForTesting boolean mHadSuccessfulReauth = false;
        @VisibleForTesting protected final byte[] mReauthIdentity;
        private final ReauthInfo mReauthInfo;
        private byte[] mNextReauthIdentity;
        private int mReauthCounter;

        ReauthState() {
            this.mReauthIdentity = getReauthIdentity();
            this.mReauthInfo = getAvailableReauthInfo(getReauthIdentity(), mEapIdentity);
        }

        ReauthState(byte[] identity, ReauthInfo reauthInfo) {
            this.mReauthIdentity = identity;
            this.mReauthInfo = reauthInfo;
        }

        public EapResult process(EapMessage message) {
            if (message.eapCode == EAP_CODE_SUCCESS) {
                if (!mHadSuccessfulReauth) {
                    LOG.e(mTAG, "Received unexpected EAP-Success");
                    return new EapError(
                            new EapInvalidRequestException(
                                    "Received an EAP-Success in the ReauthState"));
                }
                transitionTo(new FinalState());
                return processEapSuccess(mNextReauthIdentity, mReauthCounter);
            }

            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            DecodeResult<? extends EapAkaTypeData> decodeResult =
                    decode(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                return buildClientErrorResponse(
                        message.eapIdentifier, getEapMethod(), decodeResult.atClientErrorCode);
            }

            EapAkaTypeData eapAkaTypeData = decodeResult.eapTypeData;
            switch (eapAkaTypeData.eapSubtype) {
                case EAP_AKA_REAUTHENTICATION:
                    break;

                case EAP_AKA_CHALLENGE:
                    return transitionAndProcess(buildChallengeState(mEapIdentity), message);

                case EAP_AKA_NOTIFICATION:
                    return handleEapSimAkaNotification(
                            mTAG,
                            false, // isPreChallengeState
                            true, // isReauthState
                            mHadSuccessfulReauth, // hadSuccessfulAuthentication
                            message.eapIdentifier,
                            mReauthCounter,
                            eapAkaTypeData);
                default:
                    return buildClientErrorResponse(
                            message.eapIdentifier,
                            getEapMethod(),
                            AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            if (!isValidReauthAttributes(eapAkaTypeData)) {
                LOG.e(mTAG, "Invalid attributes: " + eapAkaTypeData.attributeMap.keySet());
                return buildClientErrorResponse(
                        message.eapIdentifier, getEapMethod(), AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            return handleReauthentication(message, eapAkaTypeData);
        }

        protected EapResult handleReauthentication(
                EapMessage message, EapAkaTypeData eapAkaTypeData) {
            if (mReauthIdentity == null || mReauthInfo == null) {
                LOG.e(mTAG, "No reauth Info for this re-authentication request");
                return buildClientErrorResponse(
                        message.eapIdentifier, getEapMethod(), AtClientErrorCode.UNABLE_TO_PROCESS);
            }
            System.arraycopy(mReauthInfo.getMk(), 0, mMk, 0, MASTER_KEY_LENGTH);
            System.arraycopy(mReauthInfo.getKeyEncr(), 0, mKEncr, 0, KEY_LEN);
            System.arraycopy(mReauthInfo.getKeyAut(), 0, mKAut, 0, KEY_LEN);

            try {
                if (!isValidMac(mTAG, message, eapAkaTypeData, new byte[0])) {
                    return buildClientErrorResponse(
                            message.eapIdentifier,
                            getEapMethod(),
                            AtClientErrorCode.UNABLE_TO_PROCESS);
                }
            } catch (GeneralSecurityException
                    | EapSilentException
                    | EapSimAkaInvalidAttributeException ex) {
                // if the MAC can't be generated, we can't continue
                LOG.e(mTAG, "Error computing MAC for EapMessage", ex);
                return new EapError(ex);
            }

            LinkedHashMap<Integer, EapSimAkaAttribute> securedAttributes =
                    retrieveSecuredAttributes(mTAG, eapAkaTypeData);

            if (!isValidReauthSecuredAttributes(securedAttributes)) {
                LOG.e(mTAG, "Invalid attributes: " + securedAttributes.keySet());
                return buildClientErrorResponse(
                        message.eapIdentifier, getEapMethod(), AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            EapSimAkaAttribute atNextReauthId = securedAttributes.get(EAP_AT_NEXT_REAUTH_ID);
            if (atNextReauthId != null) {
                mNextReauthIdentity =
                        ((EapSimAkaAttribute.AtNextReauthId) atNextReauthId).reauthId.clone();
            } else {
                mNextReauthIdentity = null;
            }

            int counter =
                    ((EapSimAkaAttribute.AtCounter) securedAttributes.get(EAP_AT_COUNTER)).counter;
            byte[] nonceS =
                    ((EapSimAkaAttribute.AtNonceS) securedAttributes.get(EAP_AT_NONCE_S)).nonceS;

            if (counter <= mReauthInfo.getReauthCount()) {
                LOG.e(mTAG, "COUNTER TOO SMALL");
                try {
                    EapSimAkaAttribute.AtIv atIv = new EapSimAkaAttribute.AtIv(mSecureRandom);
                    List<EapSimAkaAttribute> attributeList =
                            buildReauthResponse(counter, true /* isCounterSmall */, mKEncr, atIv);

                    return buildResponseMessageWithMac(
                            message.eapIdentifier,
                            EAP_AKA_REAUTHENTICATION,
                            nonceS,
                            attributeList,
                            null);
                } catch (EapSimAkaInvalidAttributeException ex) {
                    LOG.wtf(mTAG, "Error build EAP response for reauth", ex);
                    return new EapError(ex);
                }
            } else {
                mReauthCounter = counter;
            }

            EapResult eapResult =
                    generateAndPersistEapAkaKeys(message.eapIdentifier, counter, nonceS);
            if (eapResult != null) {
                return eapResult;
            }

            // before sending a response, check for bidding-down attacks (RFC 5448#4)
            if (mSupportsEapAkaPrime) {
                AtBidding atBidding = (AtBidding) eapAkaTypeData.attributeMap.get(EAP_AT_BIDDING);
                if (atBidding != null && atBidding.doesServerSupportEapAkaPrime) {
                    LOG.w(
                            mTAG,
                            "Potential bidding down attack. AT_BIDDING attr included and EAP-AKA'"
                                    + " is supported");
                    return buildAuthenticationRejectMessage(message.eapIdentifier);
                }
            }

            // server has been authenticated, so we can send a response
            try {
                mHadSuccessfulReauth = true;
                EapSimAkaAttribute.AtIv atIv = new EapSimAkaAttribute.AtIv(mSecureRandom);
                List<EapSimAkaAttribute> attributeList =
                        buildReauthResponse(counter, false /* isCounterSmall */, mKEncr, atIv);

                return buildResponseMessageWithMac(
                        message.eapIdentifier,
                        EAP_AKA_REAUTHENTICATION,
                        nonceS,
                        attributeList,
                        (eapAkaTypeData instanceof EapAkaPrimeTypeData)
                                ? null /* no flags set */
                                : new int[] {
                                    EapResult.EapResponse.RESPONSE_FLAG_EAP_AKA_SERVER_AUTHENTICATED
                                });
            } catch (EapSimAkaInvalidAttributeException ex) {
                LOG.wtf(mTAG, "Error build EAP response for reauth", ex);
                return new EapError(ex);
            }
        }

        private boolean isValidReauthAttributes(EapAkaTypeData eapAkaTypeData) {
            Set<Integer> attrs = eapAkaTypeData.attributeMap.keySet();

            // must contain: AT_ENCR_DATA, AT_IV, AT_MAC
            return attrs.contains(EAP_AT_IV)
                    && attrs.contains(EAP_AT_ENCR_DATA)
                    && attrs.contains(EAP_AT_MAC);
        }

        private boolean isValidReauthSecuredAttributes(
                LinkedHashMap<Integer, EapSimAkaAttribute> secureAttributes) {
            Set<Integer> attrs = secureAttributes.keySet();

            // must contain: EAP_AT_COUNTER, EAP_AT_NONCE_S
            return attrs.contains(EAP_AT_COUNTER) && attrs.contains(EAP_AT_NONCE_S);
        }

        protected EapResult buildAuthenticationRejectMessage(int eapIdentifier) {
            mIsExpectingEapFailure = true;
            return buildResponseMessage(
                    getEapMethod(),
                    EAP_AKA_AUTHENTICATION_REJECT,
                    eapIdentifier,
                    new ArrayList<>());
        }

        @Nullable
        protected EapResult generateAndPersistEapAkaKeys(
                int eapIdentifier, int counter, byte[] nonceS) {
            try {
                MessageDigest sha1 = MessageDigest.getInstance(MASTER_KEY_GENERATION_ALG);
                generateAndPersistReauthKeys(
                        mTAG,
                        sha1,
                        new Fips186_2Prf(),
                        mReauthIdentity,
                        counter,
                        nonceS,
                        mReauthInfo.getMk());
                return null;
            } catch (NoSuchAlgorithmException | BufferUnderflowException ex) {
                LOG.e(mTAG, "Error while creating keys", ex);
                return buildClientErrorResponse(
                        eapIdentifier, EAP_TYPE_AKA, AtClientErrorCode.UNABLE_TO_PROCESS);
            }
        }
    }

    private EapSuccess processEapSuccess(byte[] nextReauthId, int reauthCounter) {
        EapSuccess eapSuccess;
        if (nextReauthId != null) {
            mEapSimAkaIdentityTracker.registerReauthCredentials(
                    new String(nextReauthId, StandardCharsets.UTF_8),
                    new String(mEapIdentity, StandardCharsets.UTF_8),
                    reauthCounter,
                    mMk,
                    mKEncr,
                    mKAut);
            eapSuccess =
                    new EapSuccess(
                            mMsk,
                            mEmsk,
                            new EapAkaInfo.Builder().setReauthId(nextReauthId).build());
        } else {
            eapSuccess = new EapSuccess(mMsk, mEmsk);
        }
        return eapSuccess;
    }

    EapAkaTypeData getEapSimAkaTypeData(AtClientErrorCode clientErrorCode) {
        return new EapAkaTypeData(EAP_AKA_CLIENT_ERROR, Arrays.asList(clientErrorCode));
    }

    EapAkaTypeData getEapSimAkaTypeData(int eapSubtype, List<EapSimAkaAttribute> attributes) {
        return new EapAkaTypeData(eapSubtype, attributes);
    }
}

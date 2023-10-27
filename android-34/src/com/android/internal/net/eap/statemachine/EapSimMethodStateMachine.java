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

import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_SIM;

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_SUCCESS;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_ANY_ID_REQ;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_ENCR_DATA;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_FULLAUTH_ID_REQ;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_IV;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_MAC;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_PERMANENT_ID_REQ;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_RAND;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_VERSION_LIST;
import static com.android.internal.net.eap.message.simaka.EapSimTypeData.EAP_SIM_CHALLENGE;
import static com.android.internal.net.eap.message.simaka.EapSimTypeData.EAP_SIM_CLIENT_ERROR;
import static com.android.internal.net.eap.message.simaka.EapSimTypeData.EAP_SIM_NOTIFICATION;
import static com.android.internal.net.eap.message.simaka.EapSimTypeData.EAP_SIM_START;

import android.annotation.Nullable;
import android.content.Context;
import android.net.eap.EapSessionConfig.EapMethodConfig.EapMethod;
import android.net.eap.EapSessionConfig.EapSimConfig;
import android.telephony.TelephonyManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.EapResult;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.EapResult.EapSuccess;
import com.android.internal.net.eap.crypto.Fips186_2Prf;
import com.android.internal.net.eap.exceptions.EapInvalidRequestException;
import com.android.internal.net.eap.exceptions.EapSilentException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaAuthenticationFailureException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaIdentityUnavailableException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaInvalidAttributeException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaInvalidLengthException;
import com.android.internal.net.eap.message.EapMessage;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtClientErrorCode;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtIdentity;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtNonceMt;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtRandSim;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtSelectedVersion;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtVersionList;
import com.android.internal.net.eap.message.simaka.EapSimAkaTypeData.DecodeResult;
import com.android.internal.net.eap.message.simaka.EapSimTypeData;
import com.android.internal.net.eap.message.simaka.EapSimTypeData.EapSimTypeDataDecoder;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * EapSimMethodStateMachine represents the valid paths possible for the EAP-SIM protocol.
 *
 * <p>EAP-SIM procedures will always follow the path:
 *
 * Created ---> Start --+--> Challenge --+--> null
 *                      |                |
 *                      +-->  failed  >--+
 *
 * Note that EAP-SIM/Notification messages can be received at any point in the above state machine.
 * At most one EAP-SIM/Notification message is allowed per EAP-SIM session.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4186">RFC 4186, Extensible Authentication Protocol
 * Method for Subscriber Identity Modules (EAP-SIM)</a>
 */
class EapSimMethodStateMachine extends EapSimAkaMethodStateMachine {
    private final EapSimTypeDataDecoder mEapSimTypeDataDecoder;

    EapSimMethodStateMachine(
            Context context,
            byte[] eapIdentity,
            EapSimConfig eapSimConfig,
            SecureRandom secureRandom) {
        this(
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE),
                eapIdentity,
                eapSimConfig,
                secureRandom,
                EapSimTypeData.getEapSimTypeDataDecoder());
    }

    @VisibleForTesting
    EapSimMethodStateMachine(
            TelephonyManager telephonyManager,
            byte[] eapIdentity,
            EapSimConfig eapSimConfig,
            SecureRandom secureRandom,
            EapSimTypeDataDecoder eapSimTypeDataDecoder) {
        super(
                telephonyManager.createForSubscriptionId(eapSimConfig.getSubId()),
                eapIdentity,
                eapSimConfig);

        if (eapSimTypeDataDecoder == null) {
            throw new IllegalArgumentException("EapSimTypeDataDecoder must be non-null");
        }

        mSecureRandom = secureRandom;
        this.mEapSimTypeDataDecoder = eapSimTypeDataDecoder;
        transitionTo(new CreatedState());
    }

    @Override
    @EapMethod
    int getEapMethod() {
        return EAP_TYPE_SIM;
    }

    protected class CreatedState extends EapMethodState {
        private final String mTAG = CreatedState.class.getSimpleName();

        public EapResult process(EapMessage message) {
            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            DecodeResult<EapSimTypeData> decodeResult =
                    mEapSimTypeDataDecoder.decode(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                return buildClientErrorResponse(
                        message.eapIdentifier,
                        EAP_TYPE_SIM,
                        decodeResult.atClientErrorCode);
            }

            EapSimTypeData eapSimTypeData = decodeResult.eapTypeData;
            switch (eapSimTypeData.eapSubtype) {
                case EAP_SIM_START:
                    break;
                case EAP_SIM_NOTIFICATION:
                    return handleEapSimAkaNotification(
                            mTAG,
                            true, // isPreChallengeState
                            false, // isReauthState
                            false, // hadSuccessfulAuthentication
                            message.eapIdentifier,
                            0,
                            eapSimTypeData);
                default:
                    return buildClientErrorResponse(
                            message.eapIdentifier,
                            EAP_TYPE_SIM,
                            AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            byte[] nonce = new byte[AtNonceMt.NONCE_MT_LENGTH];
            mSecureRandom.nextBytes(nonce);
            AtNonceMt atNonceMt;
            try {
                atNonceMt = new AtNonceMt(nonce);
            } catch (EapSimAkaInvalidAttributeException ex) {
                LOG.wtf(mTAG, "Exception thrown while creating AtNonceMt", ex);
                return new EapError(ex);
            }
            return transitionAndProcess(new StartState(atNonceMt), message);
        }
    }

    protected class StartState extends EapMethodState {
        private final String mTAG = StartState.class.getSimpleName();
        private final AtNonceMt mAtNonceMt;

        private List<Integer> mVersions;

        // use the EAP-Identity for the default value (RFC 4186#7)
        @VisibleForTesting byte[] mIdentity = mEapIdentity;

        protected StartState(AtNonceMt atNonceMt) {
            this.mAtNonceMt = atNonceMt;
        }

        public EapResult process(EapMessage message) {
            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            DecodeResult<EapSimTypeData> decodeResult =
                    mEapSimTypeDataDecoder.decode(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                return buildClientErrorResponse(
                        message.eapIdentifier,
                        EAP_TYPE_SIM,
                        decodeResult.atClientErrorCode);
            }

            EapSimTypeData eapSimTypeData = decodeResult.eapTypeData;
            switch (eapSimTypeData.eapSubtype) {
                case EAP_SIM_START:
                    break;
                case EAP_SIM_NOTIFICATION:
                    return handleEapSimAkaNotification(
                            mTAG,
                            true, // isPreChallengeState
                            false, // isReauthState
                            false, // hadSuccessfulAuthentication
                            message.eapIdentifier,
                            0,
                            eapSimTypeData);
                case EAP_SIM_CHALLENGE:
                    // By virtue of being in the StartState, we have received (and processed) the
                    // EAP-SIM/Start request. Receipt of an EAP-SIM/Challenge request indicates that
                    // the server has accepted our EAP-SIM/Start response, including our identity
                    // (if any).
                    return transitionAndProcess(
                            new ChallengeState(mVersions, mAtNonceMt, mIdentity), message);
                default:
                    return buildClientErrorResponse(
                            message.eapIdentifier,
                            EAP_TYPE_SIM,
                            AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            if (!isValidStartAttributes(eapSimTypeData)) {
                LOG.e(mTAG, "Invalid attributes: " + eapSimTypeData.attributeMap.keySet());
                return buildClientErrorResponse(
                        message.eapIdentifier,
                        EAP_TYPE_SIM,
                        AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            List<EapSimAkaAttribute> responseAttributes = new ArrayList<>();
            responseAttributes.add(mAtNonceMt);

            // choose EAP-SIM version
            AtVersionList atVersionList = (AtVersionList)
                    eapSimTypeData.attributeMap.get(EAP_AT_VERSION_LIST);
            mVersions = atVersionList.versions;
            if (!mVersions.contains(AtSelectedVersion.SUPPORTED_VERSION)) {
                return buildClientErrorResponse(
                        message.eapIdentifier,
                        EAP_TYPE_SIM,
                        AtClientErrorCode.UNSUPPORTED_VERSION);
            }
            responseAttributes.add(AtSelectedVersion.getSelectedVersion());

            try {
                AtIdentity atIdentity = getIdentityResponse(eapSimTypeData);
                if (atIdentity != null) {
                    responseAttributes.add(atIdentity);
                }
            } catch (EapSimAkaInvalidAttributeException ex) {
                LOG.wtf(mTAG, "Exception thrown while making AtIdentity attribute", ex);
                return new EapError(ex);
            } catch (EapSimAkaIdentityUnavailableException ex) {
                LOG.e(mTAG, "Unable to get IMSI for subId=" + mEapUiccConfig.getSubId());
                return new EapError(ex);
            }

            return buildResponseMessage(
                    EAP_TYPE_SIM,
                    EAP_SIM_START,
                    message.eapIdentifier,
                    responseAttributes);
        }

        /**
         * Returns true iff the given EapSimTypeData meets the following conditions:
         *  - contains an AT_VERSION_LIST attribute
         *  - contains at most one of AT_PERMANENT_ID_REQ, AT_ANY_ID_REQ, or AT_FULLAUTH_D_REQ
         *      attributes
         *  - does not contain AT_MAC, AT_IV, or AT_ENCR_DATA attributes
         */
        @VisibleForTesting
        boolean isValidStartAttributes(EapSimTypeData eapSimTypeData) {
            // must contain: version list
            Set<Integer> attrs = eapSimTypeData.attributeMap.keySet();
            if (!attrs.contains(EAP_AT_VERSION_LIST)) {
                return false;
            }

            // may contain: ID request (but only 1)
            int idRequests = 0;
            if (attrs.contains(EAP_AT_PERMANENT_ID_REQ)) {
                idRequests++;
            }
            if (attrs.contains(EAP_AT_ANY_ID_REQ)) {
                idRequests++;
            }
            if (attrs.contains(EAP_AT_FULLAUTH_ID_REQ)) {
                idRequests++;
            }
            if (idRequests > 1) {
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

        @VisibleForTesting
        @Nullable
        AtIdentity getIdentityResponse(EapSimTypeData eapSimTypeData)
                throws EapSimAkaInvalidAttributeException, EapSimAkaIdentityUnavailableException {
            Set<Integer> attributes = eapSimTypeData.attributeMap.keySet();

            // TODO(b/136180022): process separate ID requests differently (pseudonym vs permanent)
            if (attributes.contains(EAP_AT_PERMANENT_ID_REQ)
                    || attributes.contains(EAP_AT_FULLAUTH_ID_REQ)
                    || attributes.contains(EAP_AT_ANY_ID_REQ)) {
                String imsi = mTelephonyManager.getSubscriberId();
                if (imsi == null) {
                    throw new EapSimAkaIdentityUnavailableException(
                            "IMSI for subId (" + mEapUiccConfig.getSubId() + ") not available");
                }

                // Permanent Identity is "1" + IMSI (RFC 4186 Section 4.1.2.6)
                String identity = "1" + imsi;
                mIdentity = identity.getBytes(StandardCharsets.US_ASCII);
                LOG.d(mTAG, "EAP-SIM/Identity=" + LOG.pii(identity));

                return AtIdentity.getAtIdentity(mIdentity);
            }

            return null;
        }
    }

    protected class ChallengeState extends EapMethodState {
        private final String mTAG = ChallengeState.class.getSimpleName();
        private final int mBytesPerShort = 2;
        private final int mVersionLenBytes = 2;

        // Lengths defined by TS 31.102 Section 7.1.2.1 (case 3)
        // SRES stands for "SIM response"
        // Kc stands for "cipher key"
        private final int mSresLenBytes = 4;
        private final int mKcLenBytes = 8;

        private final List<Integer> mVersions;
        private final byte[] mNonce;
        @VisibleForTesting boolean mHadSuccessfulChallenge = false;
        @VisibleForTesting final byte[] mIdentity;

        protected ChallengeState(List<Integer> versions, AtNonceMt atNonceMt, byte[] identity) {
            mVersions = versions;
            mNonce = atNonceMt.nonceMt;
            mIdentity = identity;
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
                return new EapSuccess(mMsk, mEmsk);
            }

            EapResult eapResult = handleEapSuccessFailureNotification(mTAG, message);
            if (eapResult != null) {
                return eapResult;
            }

            DecodeResult<EapSimTypeData> decodeResult =
                    mEapSimTypeDataDecoder.decode(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                return buildClientErrorResponse(
                        message.eapIdentifier,
                        EAP_TYPE_SIM,
                        decodeResult.atClientErrorCode);
            }

            EapSimTypeData eapSimTypeData = decodeResult.eapTypeData;
            switch (eapSimTypeData.eapSubtype) {
                case EAP_SIM_NOTIFICATION:
                    return handleEapSimAkaNotification(
                            mTAG,
                            false, // isPreChallengeState
                            false, // isReauthState
                            mHadSuccessfulChallenge, // hadSuccessfulAuthentication
                            message.eapIdentifier,
                            0,
                            eapSimTypeData);
                case EAP_SIM_CHALLENGE:
                    break;
                default:
                    return buildClientErrorResponse(
                            message.eapIdentifier,
                            EAP_TYPE_SIM,
                            AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            if (!isValidChallengeAttributes(eapSimTypeData)) {
                LOG.e(mTAG, "Invalid attributes: " + eapSimTypeData.attributeMap.keySet());
                return buildClientErrorResponse(
                        message.eapIdentifier,
                        EAP_TYPE_SIM,
                        AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            List<RandChallengeResult> randChallengeResults;
            try {
                randChallengeResults = getRandChallengeResults(eapSimTypeData);
            } catch (EapSimAkaInvalidLengthException | BufferUnderflowException ex) {
                LOG.e(mTAG, "Invalid SRES/Kc tuple returned from SIM", ex);
                return buildClientErrorResponse(
                        message.eapIdentifier,
                        EAP_TYPE_SIM,
                        AtClientErrorCode.UNABLE_TO_PROCESS);
            }  catch (EapSimAkaAuthenticationFailureException ex) {
                return new EapError(ex);
            }

            try {
                MessageDigest sha1 = MessageDigest.getInstance(MASTER_KEY_GENERATION_ALG);
                byte[] mkInputData = getMkInputData(randChallengeResults);
                generateAndPersistKeys(mTAG, sha1, new Fips186_2Prf(), mkInputData);
            } catch (NoSuchAlgorithmException | BufferUnderflowException ex) {
                LOG.e(mTAG, "Error while creating keys", ex);
                return buildClientErrorResponse(
                        message.eapIdentifier,
                        EAP_TYPE_SIM,
                        AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            try {
                if (!isValidMac(mTAG, message, eapSimTypeData, mNonce)) {
                    return buildClientErrorResponse(
                            message.eapIdentifier,
                            EAP_TYPE_SIM,
                            AtClientErrorCode.UNABLE_TO_PROCESS);
                }
            } catch (GeneralSecurityException | EapSilentException
                    | EapSimAkaInvalidAttributeException ex) {
                // if the MAC can't be generated, we can't continue
                LOG.e(mTAG, "Error computing MAC for EapMessage", ex);
                return new EapError(ex);
            }

            ByteBuffer sresValues =
                    ByteBuffer.allocate(randChallengeResults.size() * mSresLenBytes);
            for (RandChallengeResult result : randChallengeResults) {
                sresValues.put(result.sres);
            }

            // server has been authenticated, so we can send a response
            mHadSuccessfulChallenge = true;
            return buildResponseMessageWithMac(
                    message.eapIdentifier,
                    EAP_SIM_CHALLENGE,
                    sresValues.array());
        }

        /**
         * Returns true iff the given EapSimTypeData contains both AT_RAND and AT_MAC attributes.
         */
        @VisibleForTesting
        boolean isValidChallengeAttributes(EapSimTypeData eapSimTypeData) {
            Set<Integer> attrs = eapSimTypeData.attributeMap.keySet();
            return attrs.contains(EAP_AT_RAND) && attrs.contains(EAP_AT_MAC);
        }

        @VisibleForTesting
        List<RandChallengeResult> getRandChallengeResults(EapSimTypeData eapSimTypeData)
                throws EapSimAkaInvalidLengthException, EapSimAkaAuthenticationFailureException {
            AtRandSim atRand = (AtRandSim) eapSimTypeData.attributeMap.get(EAP_AT_RAND);
            List<byte[]> randList = atRand.rands;
            List<RandChallengeResult> challengeResults = new ArrayList<>();

            for (byte[] rand : randList) {
                // Rand (pre-Base64 formatting) needs to be formatted as [length][rand data]
                ByteBuffer formattedRand = ByteBuffer.allocate(rand.length + 1);
                formattedRand.put((byte) rand.length);
                formattedRand.put(rand);

                byte[] challengeResponseBytes =
                        processUiccAuthentication(
                                mTAG,
                                TelephonyManager.AUTHTYPE_EAP_SIM,
                                formattedRand.array());

                RandChallengeResult randChallengeResult =
                        getRandChallengeResultFromResponse(challengeResponseBytes);
                challengeResults.add(randChallengeResult);

                // Log rand/challenge as PII
                LOG.d(mTAG, "RAND=" + LOG.pii(rand));
                LOG.d(mTAG, "SRES=" + LOG.pii(randChallengeResult.sres));
                LOG.d(mTAG, "Kc=" + LOG.pii(randChallengeResult.kc));
            }

            return challengeResults;
        }

        /**
         * Parses the SRES and Kc values from the given challengeResponse. The values are returned
         * in a Pair<byte[], byte[]>, where SRES and Kc are the first and second values,
         * respectively.
         */
        @VisibleForTesting
        RandChallengeResult getRandChallengeResultFromResponse(byte[] challengeResponse)
                throws EapSimAkaInvalidLengthException {
            ByteBuffer buffer = ByteBuffer.wrap(challengeResponse);
            int lenSres = Byte.toUnsignedInt(buffer.get());
            if (lenSres != mSresLenBytes) {
                throw new EapSimAkaInvalidLengthException("Invalid SRES length specified");
            }
            byte[] sres = new byte[mSresLenBytes];
            buffer.get(sres);

            int lenKc = Byte.toUnsignedInt(buffer.get());
            if (lenKc != mKcLenBytes) {
                throw new EapSimAkaInvalidLengthException("Invalid Kc length specified");
            }
            byte[] kc = new byte[mKcLenBytes];
            buffer.get(kc);

            return new RandChallengeResult(sres, kc);
        }

        @VisibleForTesting
        class RandChallengeResult {
            public final byte[] sres;
            public final byte[] kc;

            RandChallengeResult(byte[] sres, byte[] kc) throws EapSimAkaInvalidLengthException {
                this.sres = sres;
                this.kc = kc;

                if (sres.length != mSresLenBytes) {
                    throw new EapSimAkaInvalidLengthException("Invalid SRES length");
                }
                if (kc.length != mKcLenBytes) {
                    throw new EapSimAkaInvalidLengthException("Invalid Kc length");
                }
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof RandChallengeResult)) return false;
                RandChallengeResult that = (RandChallengeResult) o;
                return Arrays.equals(sres, that.sres)
                        && Arrays.equals(kc, that.kc);
            }

            @Override
            public int hashCode() {
                int result = Arrays.hashCode(sres);
                result = 31 * result + Arrays.hashCode(kc);
                return result;
            }
        }

        private byte[] getMkInputData(List<RandChallengeResult> randChallengeResults) {
            int numInputBytes =
                    mIdentity.length
                            + (randChallengeResults.size() * mKcLenBytes)
                            + mNonce.length
                            + (mVersions.size() * mBytesPerShort) // 2B per version
                            + mVersionLenBytes;

            ByteBuffer mkInputBuffer = ByteBuffer.allocate(numInputBytes);
            mkInputBuffer.put(mIdentity);
            for (RandChallengeResult randChallengeResult : randChallengeResults) {
                mkInputBuffer.put(randChallengeResult.kc);
            }
            mkInputBuffer.put(mNonce);
            for (int i : mVersions) {
                mkInputBuffer.putShort((short) i);
            }
            mkInputBuffer.putShort((short) AtSelectedVersion.SUPPORTED_VERSION);
            return mkInputBuffer.array();
        }
    }

    EapSimTypeData getEapSimAkaTypeData(AtClientErrorCode clientErrorCode) {
        return new EapSimTypeData(EAP_SIM_CLIENT_ERROR, Arrays.asList(clientErrorCode));
    }

    EapSimTypeData getEapSimAkaTypeData(int eapSubtype, List<EapSimAkaAttribute> attributes) {
        return new EapSimTypeData(eapSubtype, attributes);
    }
}

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

import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_AKA_PRIME;

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.message.simaka.EapAkaTypeData.EAP_AKA_CLIENT_ERROR;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_AUTN;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_KDF;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_KDF_INPUT;

import android.annotation.Nullable;
import android.content.Context;
import android.net.eap.EapSessionConfig.EapAkaPrimeConfig;
import android.net.eap.EapSessionConfig.EapMethodConfig.EapMethod;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.crypto.KeyGenerationUtils;
import com.android.internal.net.eap.EapResult;
import com.android.internal.net.eap.crypto.HmacSha256ByteSigner;
import com.android.internal.net.eap.message.EapMessage;
import com.android.internal.net.eap.message.simaka.EapAkaPrimeTypeData;
import com.android.internal.net.eap.message.simaka.EapAkaPrimeTypeData.EapAkaPrimeTypeDataDecoder;
import com.android.internal.net.eap.message.simaka.EapAkaTypeData;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtAutn;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtClientErrorCode;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtKdf;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtKdfInput;
import com.android.internal.net.eap.message.simaka.EapSimAkaTypeData.DecodeResult;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * EapAkaPrimeMethodStateMachine represents the valid paths possible for the EAP-AKA' protocol.
 *
 * <p>EAP-AKA' sessions will always follow the path:
 *
 * Created --+--> Identity --+--> Challenge  --> Final
 *           |               |
 *           +---------------+
 *
 * <p>Note: If the EAP-Request/AKA'-Challenge message contains an AUTN with an invalid sequence
 * number, the peer will indicate a synchronization failure to the server and a new challenge will
 * be attempted.
 *
 * <p>Note: EAP-Request/Notification messages can be received at any point in the above state
 * machine At most one EAP-AKA'/Notification message is allowed per EAP-AKA' session.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4187">RFC 4187, Extensible Authentication Protocol
 *     for Authentication and Key Agreement (EAP-AKA)</a>
 * @see <a href="https://tools.ietf.org/html/rfc5448">RFC 5448, Improved Extensible Authentication
 *     Protocol Method for 3rd Generation Authentication and Key Agreement (EAP-AKA')</a>
 * @hide
 */
public class EapAkaPrimeMethodStateMachine extends EapAkaMethodStateMachine {
    public static final int K_AUT_LEN = 32;
    public static final int K_RE_LEN = 32;

    // EAP-AKA' identity prefix (RFC 5448#3)
    private static final String AKA_PRIME_IDENTITY_PREFIX = "6";
    private static final int SUPPORTED_KDF = 1;
    private static final int FC = 0x20; // Required by TS 133 402 Annex A.2
    private static final int SQN_XOR_AK_LEN = 6;
    private static final int IK_PRIME_LENGTH = 16;
    private static final int CK_PRIME_LENGTH = 16;
    private static final String MAC_ALGORITHM_STRING = "HmacSHA256";
    private static final String MK_DATA_PREFIX = "EAP-AKA'";

    // MK_LEN_BYTES = len(K_encr | K_aut | K_re | MSK | EMSK)
    private static final int MK_LEN_BYTES =
            KEY_LEN + K_AUT_LEN + K_RE_LEN + (2 * SESSION_KEY_LENGTH);

    public final byte[] mKRe = new byte[getKReLen()];

    private final EapAkaPrimeConfig mEapAkaPrimeConfig;
    private final EapAkaPrimeTypeDataDecoder mEapAkaPrimeTypeDataDecoder;

    EapAkaPrimeMethodStateMachine(
            Context context, byte[] eapIdentity, EapAkaPrimeConfig eapAkaPrimeConfig) {
        this(
                context,
                eapIdentity,
                eapAkaPrimeConfig,
                EapAkaPrimeTypeData.getEapAkaPrimeTypeDataDecoder());
    }

    @VisibleForTesting
    protected EapAkaPrimeMethodStateMachine(
            Context context,
            byte[] eapIdentity,
            EapAkaPrimeConfig eapAkaPrimeConfig,
            EapAkaPrimeTypeDataDecoder eapAkaPrimeTypeDataDecoder) {
        super(context, eapIdentity, eapAkaPrimeConfig);
        mEapAkaPrimeConfig = eapAkaPrimeConfig;
        mEapAkaPrimeTypeDataDecoder = eapAkaPrimeTypeDataDecoder;

        transitionTo(new CreatedState());
    }

    @Override
    @EapMethod
    int getEapMethod() {
        return EAP_TYPE_AKA_PRIME;
    }

    @Override
    protected int getKAutLength() {
        return K_AUT_LEN;
    }

    protected int getKReLen() {
        return K_RE_LEN;
    }

    @Override
    protected DecodeResult<EapAkaTypeData> decode(byte[] typeData) {
        return mEapAkaPrimeTypeDataDecoder.decode(typeData);
    }

    @Override
    protected String getIdentityPrefix() {
        return AKA_PRIME_IDENTITY_PREFIX;
    }

    @Override
    protected ChallengeState buildChallengeState() {
        return new ChallengeState();
    }

    @Override
    protected ChallengeState buildChallengeState(byte[] identity) {
        return new ChallengeState(identity);
    }

    @Override
    protected String getMacAlgorithm() {
        return MAC_ALGORITHM_STRING;
    }

    protected class ChallengeState extends EapAkaMethodStateMachine.ChallengeState {
        private final String mTAG = ChallengeState.class.getSimpleName();

        ChallengeState() {
            super();
        }

        ChallengeState(byte[] identity) {
            super(identity);
        }

        @Override
        protected EapResult handleChallengeAuthentication(
                EapMessage message, EapAkaTypeData eapAkaTypeData) {
            EapAkaPrimeTypeData eapAkaPrimeTypeData = (EapAkaPrimeTypeData) eapAkaTypeData;

            if (!isValidChallengeAttributes(eapAkaPrimeTypeData)) {
                return buildAuthenticationRejectMessage(message.eapIdentifier);
            }
            return super.handleChallengeAuthentication(message, eapAkaPrimeTypeData);
        }

        @VisibleForTesting
        boolean isValidChallengeAttributes(EapAkaPrimeTypeData eapAkaPrimeTypeData) {
            Map<Integer, EapSimAkaAttribute> attrs = eapAkaPrimeTypeData.attributeMap;

            if (!attrs.containsKey(EAP_AT_KDF) || !attrs.containsKey(EAP_AT_KDF_INPUT)) {
                return false;
            }

            // TODO(b/143073851): implement KDF resolution specified in RFC 5448#3.2
            // This is safe, as there only exists one defined KDF.
            AtKdf atKdf = (AtKdf) attrs.get(EAP_AT_KDF);
            if (atKdf.kdf != SUPPORTED_KDF) {
                return false;
            }

            AtKdfInput atKdfInput = (AtKdfInput) attrs.get(EAP_AT_KDF_INPUT);
            if (atKdfInput.networkName.length == 0) {
                return false;
            }

            boolean hasMatchingNetworkNames =
                    hasMatchingNetworkNames(
                            mEapAkaPrimeConfig.getNetworkName(),
                            new String(atKdfInput.networkName, StandardCharsets.UTF_8));
            return mEapAkaPrimeConfig.allowsMismatchedNetworkNames() || hasMatchingNetworkNames;
        }

        /**
         * Compares the peer's network name against the server's network name.
         *
         * <p>RFC 5448#3.1 describes how the network names are to be compared: "each name is broken
         * down to the fields separated by colons. If one of the names has more colons and fields
         * than the other one, the additional fields are ignored. The remaining sequences of fields
         * are compared. This algorithm allows a prefix match".
         *
         * @return true iff one network name matches the other, as defined by RC 5448#3.1
         */
        @VisibleForTesting
        boolean hasMatchingNetworkNames(String peerNetworkName, String serverNetworkName) {
            // compare network names according to RFC 5448#3.1
            if (peerNetworkName.isEmpty() || serverNetworkName.isEmpty()) {
                return true;
            }

            String[] peerNetworkNameFields = peerNetworkName.split(":");
            String[] serverNetworkNameFields = serverNetworkName.split(":");
            int numFieldsToCompare =
                    Math.min(peerNetworkNameFields.length, serverNetworkNameFields.length);
            for (int i = 0; i < numFieldsToCompare; i++) {
                if (!peerNetworkNameFields[i].equals(serverNetworkNameFields[i])) {
                    LOG.i(
                            mTAG,
                            "EAP-AKA' network names don't match."
                                    + " Peer: " + LOG.pii(peerNetworkName)
                                    + ", Server: " + LOG.pii(serverNetworkName));
                    return false;
                }
            }

            return true;
        }

        @Nullable
        @Override
        protected EapResult generateAndPersistEapAkaKeys(
                RandChallengeResult result, int eapIdentifier, EapAkaTypeData eapAkaTypeData) {
            try {
                AtKdfInput atKdfInput =
                        (AtKdfInput) eapAkaTypeData.attributeMap.get(EAP_AT_KDF_INPUT);
                AtAutn atAutn = (AtAutn) eapAkaTypeData.attributeMap.get(EAP_AT_AUTN);

                // Values derived as (CK' | IK'), but PRF' needs (IK' | CK')
                byte[] ckIkPrime = deriveCkIkPrime(result, atKdfInput, atAutn);
                ByteBuffer prfKey = ByteBuffer.allocate(IK_PRIME_LENGTH + CK_PRIME_LENGTH);
                prfKey.put(ckIkPrime, CK_PRIME_LENGTH, IK_PRIME_LENGTH);
                prfKey.put(ckIkPrime, 0, CK_PRIME_LENGTH);

                int dataToSignLen = MK_DATA_PREFIX.length() + mIdentity.length;
                ByteBuffer dataToSign = ByteBuffer.allocate(dataToSignLen);
                dataToSign.put(MK_DATA_PREFIX.getBytes(StandardCharsets.US_ASCII));
                dataToSign.put(mIdentity);

                ByteBuffer mk =
                        ByteBuffer.wrap(
                                KeyGenerationUtils.prfPlus(
                                        HmacSha256ByteSigner.getInstance(),
                                        prfKey.array(),
                                        dataToSign.array(),
                                        MK_LEN_BYTES));

                mk.get(mKEncr);
                mk.get(mKAut);
                mk.get(mKRe);
                mk.get(mMsk);
                mk.get(mEmsk);

                // Log as hash unless PII debug mode enabled
                LOG.d(mTAG, "K_encr=" + LOG.pii(mKEncr));
                LOG.d(mTAG, "K_aut=" + LOG.pii(mKAut));
                LOG.d(mTAG, "K_re=" + LOG.pii(mKRe));
                LOG.d(mTAG, "MSK=" + LOG.pii(mMsk));
                LOG.d(mTAG, "EMSK=" + LOG.pii(mEmsk));
                return null;
            } catch (GeneralSecurityException
                    | BufferOverflowException
                    | BufferUnderflowException ex) {
                LOG.e(mTAG, "Error while generating keys", ex);
                return buildClientErrorResponse(
                        eapIdentifier, getEapMethod(), AtClientErrorCode.UNABLE_TO_PROCESS);
            }
        }

        /**
         * Derives CK' and IK' values from CK and IK
         *
         * <p>CK' and IK' generation is specified in TS 133 402 Annex A.2, which relies on the key
         * derivation function KDF specified in TS 133 220 Annex B.2.
         */
        @VisibleForTesting
        byte[] deriveCkIkPrime(
                RandChallengeResult randChallengeResult, AtKdfInput atKdfInput, AtAutn atAutn)
                throws GeneralSecurityException {
            final int fcLen = 1;
            int lengthFieldLen = 2;

            // SQN ^ AK is the first 6B of the AUTN value
            byte[] sqnXorAk = Arrays.copyOf(atAutn.autn, SQN_XOR_AK_LEN);
            int sLength =
                    fcLen
                            + atKdfInput.networkName.length + lengthFieldLen
                            + SQN_XOR_AK_LEN + lengthFieldLen;

            ByteBuffer dataToSign = ByteBuffer.allocate(sLength);
            dataToSign.put((byte) FC);
            dataToSign.put(atKdfInput.networkName);
            dataToSign.putShort((short) atKdfInput.networkName.length);
            dataToSign.put(sqnXorAk);
            dataToSign.putShort((short) SQN_XOR_AK_LEN);

            int keyLen = randChallengeResult.ck.length + randChallengeResult.ik.length;
            ByteBuffer key = ByteBuffer.allocate(keyLen);
            key.put(randChallengeResult.ck);
            key.put(randChallengeResult.ik);

            Mac mac = Mac.getInstance(MAC_ALGORITHM_STRING);
            mac.init(new SecretKeySpec(key.array(), MAC_ALGORITHM_STRING));
            return mac.doFinal(dataToSign.array());
        }
    }

    EapAkaPrimeTypeData getEapSimAkaTypeData(AtClientErrorCode clientErrorCode) {
        return new EapAkaPrimeTypeData(EAP_AKA_CLIENT_ERROR, Arrays.asList(clientErrorCode));
    }

    EapAkaPrimeTypeData getEapSimAkaTypeData(int eapSubtype, List<EapSimAkaAttribute> attributes) {
        return new EapAkaPrimeTypeData(eapSubtype, attributes);
    }
}

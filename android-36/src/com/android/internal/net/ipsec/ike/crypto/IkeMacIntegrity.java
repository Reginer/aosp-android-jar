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

package com.android.internal.net.ipsec.ike.crypto;

import static android.net.ipsec.ike.SaProposal.INTEGRITY_ALGORITHM_AES_XCBC_96;

import android.annotation.Nullable;
import android.net.IpSecAlgorithm;
import android.net.ipsec.ike.SaProposal;
import android.util.SparseArray;

import com.android.internal.net.ipsec.ike.message.IkeSaPayload.IntegrityTransform;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * IkeMacIntegrity represents a negotiated integrity algorithm.
 *
 * <p>For integrity algorithms based on encryption algorithm, all operations will be done by a
 * {@link Cipher}. Otherwise, all operations will be done by a {@link Mac}.
 *
 * <p>@see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.2">RFC 7296, Internet Key
 * Exchange Protocol Version 2 (IKEv2)</a>
 */
public class IkeMacIntegrity extends IkeMac {
    // Map IKE algorithm numbers to IPsec algorithm names
    private static final SparseArray<String> IKE_ALGO_TO_IPSEC_ALGO;

    static {
        IKE_ALGO_TO_IPSEC_ALGO = new SparseArray<>();
        IKE_ALGO_TO_IPSEC_ALGO.put(
                SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96, IpSecAlgorithm.AUTH_HMAC_SHA1);
        IKE_ALGO_TO_IPSEC_ALGO.put(
                SaProposal.INTEGRITY_ALGORITHM_AES_XCBC_96, IpSecAlgorithm.AUTH_AES_XCBC);
        IKE_ALGO_TO_IPSEC_ALGO.put(
                SaProposal.INTEGRITY_ALGORITHM_AES_CMAC_96, IpSecAlgorithm.AUTH_AES_CMAC);
        IKE_ALGO_TO_IPSEC_ALGO.put(
                SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_256_128, IpSecAlgorithm.AUTH_HMAC_SHA256);
        IKE_ALGO_TO_IPSEC_ALGO.put(
                SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_384_192, IpSecAlgorithm.AUTH_HMAC_SHA384);
        IKE_ALGO_TO_IPSEC_ALGO.put(
                SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_512_256, IpSecAlgorithm.AUTH_HMAC_SHA512);
    }

    private final int mChecksumLength;

    private IkeMacIntegrity(
            @SaProposal.IntegrityAlgorithm int algorithmId,
            int keyLength,
            String algorithmName,
            boolean isJceSupported,
            int checksumLength) {
        super(algorithmId, keyLength, algorithmName, isJceSupported);
        mChecksumLength = checksumLength;
    }

    /**
     * Construct an instance of IkeMacIntegrity.
     *
     * @param integrityTransform the valid negotiated IntegrityTransform.
     * @return an instance of IkeMacIntegrity.
     */
    public static IkeMacIntegrity create(IntegrityTransform integrityTransform) {
        int algorithmId = integrityTransform.id;

        int keyLength = 0;
        String algorithmName = "";
        boolean isJceSupported = true;
        int checksumLength = 0;

        switch (algorithmId) {
            case SaProposal.INTEGRITY_ALGORITHM_NONE:
                throw new IllegalArgumentException("Integrity algorithm is not found.");
            case SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96:
                keyLength = 20;
                algorithmName = "HmacSHA1";
                checksumLength = 12;
                break;
            case SaProposal.INTEGRITY_ALGORITHM_AES_XCBC_96:
                keyLength = 16;
                isJceSupported = false;
                algorithmName = ALGO_NAME_JCE_UNSUPPORTED;
                checksumLength = 12;
                break;
            case SaProposal.INTEGRITY_ALGORITHM_AES_CMAC_96:
                keyLength = 16;
                algorithmName = "AESCMAC";
                checksumLength = 12;
                break;
            case SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_256_128:
                keyLength = 32;
                algorithmName = "HmacSHA256";
                checksumLength = 16;
                break;
            case SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_384_192:
                keyLength = 48;
                algorithmName = "HmacSHA384";
                checksumLength = 24;
                break;
            case SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_512_256:
                keyLength = 64;
                algorithmName = "HmacSHA512";
                checksumLength = 32;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unrecognized Integrity Algorithm ID: " + algorithmId);
        }

        return new IkeMacIntegrity(
                algorithmId, keyLength, algorithmName, isJceSupported, checksumLength);
    }

    @Override
    public byte[] signBytes(byte[] keyBytes, byte[] dataToSign) {
        if (getAlgorithmId() == INTEGRITY_ALGORITHM_AES_XCBC_96) {
            try {
                return new AesXCbcImpl().mac(keyBytes, dataToSign, true /*needTruncation*/);
            } catch (GeneralSecurityException | IllegalStateException e) {
                throw new IllegalArgumentException("Failed to generate MAC: ", e);
            }
        } else {
            return super.signBytes(keyBytes, dataToSign);
        }
    }

    /**
     * Gets integrity checksum length (in bytes).
     *
     * <p>IKE defines a fixed truncation length for each integirty algorithm as its checksum length.
     *
     * @return the integrity checksum length (in bytes).
     */
    public int getChecksumLen() {
        return mChecksumLength;
    }

    /**
     * Signs the bytes to generate an integrity checksum.
     *
     * @param keyBytes the negotiated integrity key.
     * @param dataToAuthenticate the data to authenticate.
     * @return the integrity checksum.
     */
    public byte[] generateChecksum(byte[] keyBytes, byte[] dataToAuthenticate) {
        if (getKeyLength() != keyBytes.length) {
            throw new IllegalArgumentException(
                    "Expected key length: "
                            + getKeyLength()
                            + " Received key length: "
                            + keyBytes.length);
        }

        byte[] signedBytes = signBytes(keyBytes, dataToAuthenticate);
        return Arrays.copyOfRange(signedBytes, 0, mChecksumLength);
    }

    /**
     * Returns the IPsec algorithm name defined in {@link IpSecAlgorithm} given the IKE algorithm
     * ID.
     *
     * <p>Returns null if there is no corresponding IPsec algorithm given the IKE algorithm ID.
     */
    @Nullable
    public static String getIpSecAlgorithmName(int ikeAlgoId) {
        return IKE_ALGO_TO_IPSEC_ALGO.get(ikeAlgoId);
    }

    /**
     * Build IpSecAlgorithm from this IkeMacIntegrity.
     *
     * <p>Build IpSecAlgorithm that represents the same integrity algorithm with this
     * IkeMacIntegrity instance with provided integrity key.
     *
     * @param key the integrity key in byte array.
     * @return the IpSecAlgorithm.
     */
    public IpSecAlgorithm buildIpSecAlgorithmWithKey(byte[] key) {
        if (key.length != getKeyLength()) {
            throw new IllegalArgumentException(
                    "Expected key with length of : "
                            + getKeyLength()
                            + " Received key with length of : "
                            + key.length);
        }
        if (getIpSecAlgorithmName(getAlgorithmId()) == null) {
            throw new IllegalStateException(
                    "Unsupported algorithm " + getAlgorithmId() + " in IPsec");
        }
        return new IpSecAlgorithm(
                getIpSecAlgorithmName(getAlgorithmId()), key, mChecksumLength * 8);
    }

    /**
     * Returns algorithm type as a String.
     *
     * @return the algorithm type as a String.
     */
    @Override
    public String getTypeString() {
        return "Integrity Algorithm.";
    }
}

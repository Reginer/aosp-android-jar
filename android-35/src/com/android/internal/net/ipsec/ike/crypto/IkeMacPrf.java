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

import static android.net.ipsec.ike.SaProposal.PSEUDORANDOM_FUNCTION_AES128_CMAC;
import static android.net.ipsec.ike.SaProposal.PSEUDORANDOM_FUNCTION_AES128_XCBC;

import android.net.ipsec.ike.SaProposal;

import com.android.internal.net.crypto.KeyGenerationUtils;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.PrfTransform;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * IkeMacPrf represents a negotiated pseudorandom function.
 *
 * <p>Pseudorandom function is usually used for IKE SA authentication and generating keying
 * materials.
 *
 * <p>For pseudorandom functions based on integrity algorithms, all operations will be done by a
 * {@link Mac}. For pseudorandom functions based on encryption algorithms, all operations will be
 * done by a {@link Cipher}.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.2">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public class IkeMacPrf extends IkeMac {
    private static final int PSEUDORANDOM_FUNCTION_AES128_XCBC_KEY_LEN = 16;

    private IkeMacPrf(
            @SaProposal.PseudorandomFunction int algorithmId,
            int keyLength,
            String algorithmName,
            boolean isJceSupported) {
        super(algorithmId, keyLength, algorithmName, isJceSupported);
    }

    /**
     * Construct an instance of IkeMacPrf.
     *
     * @param prfTransform the valid negotiated PrfTransform.
     * @return an instance of IkeMacPrf.
     */
    public static IkeMacPrf create(PrfTransform prfTransform) {
        int algorithmId = prfTransform.id;

        int keyLength = 0;
        String algorithmName = "";
        boolean isJceSupported = true;

        switch (algorithmId) {
            case SaProposal.PSEUDORANDOM_FUNCTION_HMAC_SHA1:
                keyLength = 20;
                algorithmName = "HmacSHA1";
                break;
            case SaProposal.PSEUDORANDOM_FUNCTION_AES128_XCBC:
                keyLength = 16;
                isJceSupported = false;
                algorithmName = ALGO_NAME_JCE_UNSUPPORTED;
                break;
            case SaProposal.PSEUDORANDOM_FUNCTION_AES128_CMAC:
                keyLength = 16;
                algorithmName = "AESCMAC";
                break;
            case SaProposal.PSEUDORANDOM_FUNCTION_SHA2_256:
                keyLength = 32;
                algorithmName = "HmacSHA256";
                break;
            case SaProposal.PSEUDORANDOM_FUNCTION_SHA2_384:
                keyLength = 48;
                algorithmName = "HmacSHA384";
                break;
            case SaProposal.PSEUDORANDOM_FUNCTION_SHA2_512:
                keyLength = 64;
                algorithmName = "HmacSHA512";
                break;
            default:
                throw new IllegalArgumentException("Unrecognized PRF ID: " + algorithmId);
        }

        return new IkeMacPrf(algorithmId, keyLength, algorithmName, isJceSupported);
    }

    @Override
    public byte[] signBytes(byte[] keyBytes, byte[] dataToSign) {
        if (getAlgorithmId() == PSEUDORANDOM_FUNCTION_AES128_XCBC) {
            try {
                keyBytes = modifyAesXCbcKeyIfNeeded(keyBytes);
                return new AesXCbcImpl().mac(keyBytes, dataToSign, false /*needTruncation*/);
            } catch (GeneralSecurityException | IllegalStateException e) {
                throw new IllegalArgumentException("Failed to generate MAC: ", e);
            }
        } else if (getAlgorithmId() == PSEUDORANDOM_FUNCTION_AES128_CMAC) {
            keyBytes = modifyAesCmacKeyIfNeeded(keyBytes);
        }

        return super.signBytes(keyBytes, dataToSign);
    }

    private byte[] modifyAesXCbcKeyIfNeeded(byte[] keyBytes) throws GeneralSecurityException {
        // As per RFC 4434:
        // The key for AES-XCBC-PRF-128 is created as follows:
        //
        // 1. If the key is exactly 128 bits long, use it as-is.
        //
        // 2. If the key has fewer than 128 bits, lengthen it to exactly 128 bits by padding it on
        // the right with zero bits.
        //
        // 3. If the key is 129 bits or longer, shorten it to exactly 128 bits by performing the
        // steps in AES-XCBC-PRF-128 (that is, the algorithm described in this document). In that
        // re-application of this algorithm, the key is 128 zero bits; the message is the too-long
        // current key.
        if (keyBytes.length < 16) {
            keyBytes = Arrays.copyOf(keyBytes, 16);
        } else if (keyBytes.length > 16) {
            keyBytes = new AesXCbcImpl().mac(new byte[16], keyBytes, false /*needTruncation*/);
        }

        return keyBytes;
    }

    private byte[] modifyAesCmacKeyIfNeeded(byte[] keyBytes) {
        // As per RFC 4615:
        // The key for AES-CMAC-PRF-128 is created as follows:
        //
        // 1. If the key, VK, is exactly 128 bits, then we use it as-is.
        //
        // 2. If it is longer or shorter than 128 bits, then we derive the key, K, by applying the
        // AES-CMAC algorithm using the 128-bit all-zero string as the key and VK as the input
        // message.
        if (keyBytes.length != 16) {
            keyBytes = signBytes(new byte[16], keyBytes);
        }
        return keyBytes;
    }

    /**
     * Generates SKEYSEED based on the nonces and shared DH secret.
     *
     * @param nonceInit the IKE initiator nonce.
     * @param nonceResp the IKE responder nonce.
     * @param sharedDhKey the DH shared key.
     * @return the byte array of SKEYSEED.
     */
    public byte[] generateSKeySeed(byte[] nonceInit, byte[] nonceResp, byte[] sharedDhKey) {
        ByteBuffer keyBuffer = null;
        if (getAlgorithmId() == SaProposal.PSEUDORANDOM_FUNCTION_AES128_XCBC
                || getAlgorithmId() == SaProposal.PSEUDORANDOM_FUNCTION_AES128_CMAC) {
            keyBuffer = ByteBuffer.allocate(getKeyLength());
            // When generating initial keys, use 8 bytes each from initiator and responder nonces as
            // per RFC 7296
            keyBuffer
                    .put(Arrays.copyOfRange(nonceInit, 0, 8))
                    .put(Arrays.copyOfRange(nonceResp, 0, 8));
        } else {
            keyBuffer = ByteBuffer.allocate(nonceInit.length + nonceResp.length);
            keyBuffer.put(nonceInit).put(nonceResp);
        }

        return signBytes(keyBuffer.array(), sharedDhKey);
    }

    /**
     * Generates a rekey SKEYSEED based on the nonces and shared DH secret.
     *
     * @param skD the secret for deriving new keys
     * @param nonceInit the IKE initiator nonce.
     * @param nonceResp the IKE responder nonce.
     * @param sharedDhKey the DH shared key.
     * @return the byte array of SKEYSEED.
     */
    public byte[] generateRekeyedSKeySeed(
            byte[] skD, byte[] nonceInit, byte[] nonceResp, byte[] sharedDhKey) {
        ByteBuffer dataToSign =
                ByteBuffer.allocate(sharedDhKey.length + nonceInit.length + nonceResp.length);
        dataToSign.put(sharedDhKey).put(nonceInit).put(nonceResp);

        return signBytes(skD, dataToSign.array());
    }

    /**
     * Derives keying materials from IKE/Child SA negotiation.
     *
     * <p>prf+(K, S) outputs a pseudorandom stream by using negotiated PRF iteratively. In this way
     * it can generate long enough keying material containing all the keys for this IKE/Child SA.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7296#section-2.13">RFC 7296 Internet Key
     *     Exchange Protocol Version 2 (IKEv2) 2.13. Generating Keying Material </a>
     * @param keyBytes the key to sign data. SKEYSEED is used for generating KEYMAT for IKE SA. SK_d
     *     is used for generating KEYMAT for Child SA.
     * @param dataToSign the data to be signed.
     * @param keyMaterialLen the length of keying materials.
     * @return the byte array of keying materials
     */
    public byte[] generateKeyMat(byte[] keyBytes, byte[] dataToSign, int keyMaterialLen) {
        return KeyGenerationUtils.prfPlus(this, keyBytes, dataToSign, keyMaterialLen);
    }

    /**
     * Returns algorithm type as a String.
     *
     * @return the algorithm type as a String.
     */
    @Override
    public String getTypeString() {
        return "Pseudorandom Function";
    }
}

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

import android.net.IpSecAlgorithm;
import android.net.ipsec.ike.SaProposal;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * IkeCipher represents a negotiated combined-mode cipher(AEAD) encryption algorithm.
 *
 * <p>Checksum mentioned in this class is also known as authentication tag or Integrity Checksum
 * Vector(ICV)
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.2">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @see <a href="https://tools.ietf.org/html/rfc5282">RFC 5282,Using Authenticated Encryption
 *     Algorithms with the Encrypted Payload of the Internet Key Exchange version 2 (IKEv2)
 *     Protocol</a>
 */
public final class IkeCombinedModeCipher extends IkeCipher {
    private final int mChecksumLen;

    /** Package private */
    IkeCombinedModeCipher(
            int algorithmId, int keyLength, int ivLength, String algorithmName, int saltLen) {
        this(algorithmId, keyLength, ivLength, algorithmName, saltLen, BLOCK_SIZE_NOT_SPECIFIED);
    }

    /** Package private */
    IkeCombinedModeCipher(
            int algorithmId,
            int keyLength,
            int ivLength,
            String algorithmName,
            int saltLen,
            int blockSize) {
        super(algorithmId, keyLength, ivLength, algorithmName, true /*isAead*/, saltLen, blockSize);
        switch (algorithmId) {
            case SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8:
                mChecksumLen = 8;
                break;
            case SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_12:
                mChecksumLen = 12;
                break;
            case SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16:
                mChecksumLen = 16;
                break;
            case SaProposal.ENCRYPTION_ALGORITHM_CHACHA20_POLY1305:
                mChecksumLen = 16;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unrecognized Encryption Algorithm ID: " + algorithmId);
        }
    }

    private byte[] doCipherAction(
            byte[] data, byte[] additionalAuthData, byte[] keyBytes, byte[] ivBytes, int opmode)
            throws AEADBadTagException {
        try {
            // Provided key consists of encryption/decryption key plus 4-byte salt. Salt is used
            // with IV to build the nonce.
            ByteBuffer secretKeyAndSaltBuffer = ByteBuffer.wrap(keyBytes);
            byte[] secretKeyBytes = new byte[keyBytes.length - mSaltLen];
            byte[] salt = new byte[mSaltLen];
            secretKeyAndSaltBuffer.get(secretKeyBytes);
            secretKeyAndSaltBuffer.get(salt);

            SecretKeySpec key = new SecretKeySpec(secretKeyBytes, getAlgorithmName());

            ByteBuffer nonceBuffer = ByteBuffer.allocate(mSaltLen + ivBytes.length);
            nonceBuffer.put(salt);
            nonceBuffer.put(ivBytes);

            mCipher.init(opmode, key, getParamSpec(nonceBuffer.array()));
            mCipher.updateAAD(additionalAuthData);

            ByteBuffer inputBuffer = ByteBuffer.wrap(data);

            int outputLen = data.length;
            if (opmode == Cipher.ENCRYPT_MODE) outputLen += mChecksumLen;
            ByteBuffer outputBuffer = ByteBuffer.allocate(outputLen);

            mCipher.doFinal(inputBuffer, outputBuffer);
            return outputBuffer.array();
        } catch (AEADBadTagException e) {
            // Checksum failed in decryption
            throw (AEADBadTagException) e;
        } catch (InvalidKeyException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException
                | BadPaddingException
                | ShortBufferException e) {
            String errorMessage =
                    Cipher.ENCRYPT_MODE == opmode
                            ? "Failed to encrypt data: "
                            : "Failed to decrypt data: ";
            throw new IllegalArgumentException(errorMessage, e);
        }
    }

    private AlgorithmParameterSpec getParamSpec(byte[] nonce) {
        switch (getAlgorithmId()) {
            case SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8:
                // Fall through
            case SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_12:
                // Fall through
            case SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16:
                return new GCMParameterSpec(mChecksumLen * 8, nonce);
            case SaProposal.ENCRYPTION_ALGORITHM_CHACHA20_POLY1305:
                return new IvParameterSpec(nonce);
            default:
                throw new IllegalArgumentException(
                        "Unrecognized Encryption Algorithm ID: " + getAlgorithmId());
        }
    }

    /**
     * Encrypt padded data and calculate checksum for it.
     *
     * @param paddedData the padded data to encrypt.
     * @param additionalAuthData additional data to authenticate (also known as associated data).
     * @param keyBytes the encryption key.
     * @param ivBytes the initialization vector (IV).
     * @return the encrypted and padded data with checksum.
     */
    public byte[] encrypt(
            byte[] paddedData, byte[] additionalAuthData, byte[] keyBytes, byte[] ivBytes) {
        try {
            return doCipherAction(
                    paddedData, additionalAuthData, keyBytes, ivBytes, Cipher.ENCRYPT_MODE);
        } catch (AEADBadTagException e) {
            throw new IllegalArgumentException("Failed to encrypt data: ", e);
        }
    }

    /**
     * Authenticate and decrypt the padded data with checksum.
     *
     * @param paddedDataWithChecksum the padded data with checksum.
     * @param additionalAuthData additional data to authenticate (also known as associated data).
     * @param keyBytes the decryption key.
     * @param ivBytes the initialization vector (IV).
     * @return the decrypted and padded data
     * @throws AEADBadTagException if authentication or decryption fails
     */
    public byte[] decrypt(
            byte[] paddedDataWithChecksum,
            byte[] additionalAuthData,
            byte[] keyBytes,
            byte[] ivBytes)
            throws AEADBadTagException {

        byte[] decryptPaddedDataAndAuthTag =
                doCipherAction(
                        paddedDataWithChecksum,
                        additionalAuthData,
                        keyBytes,
                        ivBytes,
                        Cipher.DECRYPT_MODE);

        int decryptPaddedDataLen = decryptPaddedDataAndAuthTag.length - mChecksumLen;
        return Arrays.copyOf(decryptPaddedDataAndAuthTag, decryptPaddedDataLen);
    }

    /**
     * Returns length of checksum.
     *
     * @return the length of checksum in bytes.
     */
    public int getChecksumLen() {
        return mChecksumLen;
    }

    @Override
    protected IpSecAlgorithm buildIpSecAlgorithmWithKeyImpl(byte[] key) {
        return new IpSecAlgorithm(getIpSecAlgorithmName(getAlgorithmId()), key, mChecksumLen * 8);
    }
}

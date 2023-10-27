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
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * IkeCipher represents a negotiated normal mode encryption algorithm.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.2">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeNormalModeCipher extends IkeCipher {
    // Block counter field should be 32 bits and starts from value one.
    static final byte[] AES_CTR_INITIAL_COUNTER = new byte[] {0x00, 0x00, 0x00, 0x01};

    /** Package private */
    IkeNormalModeCipher(int algorithmId, int keyLength, int ivLength, String algorithmName) {
        this(algorithmId, keyLength, ivLength, algorithmName, SALT_LEN_NOT_INCLUDED);
    }

    /** Package private */
    IkeNormalModeCipher(
            int algorithmId, int keyLength, int ivLength, String algorithmName, int saltLen) {
        super(
                algorithmId,
                keyLength,
                ivLength,
                algorithmName,
                false /*isAead*/,
                saltLen,
                BLOCK_SIZE_NOT_SPECIFIED);
    }

    private byte[] doCipherAction(byte[] data, byte[] keyBytes, byte[] ivBytes, int opmode)
            throws IllegalBlockSizeException {
        if (getKeyLength() != keyBytes.length) {
            throw new IllegalArgumentException(
                    "Expected key length: "
                            + getKeyLength()
                            + " Received key length: "
                            + keyBytes.length);
        }
        try {
            byte[] secretKeyBytes = Arrays.copyOfRange(keyBytes, 0, keyBytes.length - mSaltLen);
            byte[] salt = Arrays.copyOfRange(keyBytes, secretKeyBytes.length, keyBytes.length);

            byte[] nonce = concatenateByteArray(salt, ivBytes);
            if (getAlgorithmId() == SaProposal.ENCRYPTION_ALGORITHM_AES_CTR) {
                nonce = concatenateByteArray(nonce, AES_CTR_INITIAL_COUNTER);
            }

            SecretKeySpec key = new SecretKeySpec(secretKeyBytes, getAlgorithmName());
            IvParameterSpec iv = new IvParameterSpec(nonce);
            mCipher.init(opmode, key, iv);

            ByteBuffer inputBuffer = ByteBuffer.wrap(data);
            ByteBuffer outputBuffer = ByteBuffer.allocate(data.length);

            mCipher.doFinal(inputBuffer, outputBuffer);
            return outputBuffer.array();
        } catch (InvalidKeyException
                | InvalidAlgorithmParameterException
                | BadPaddingException
                | ShortBufferException e) {
            String errorMessage =
                    Cipher.ENCRYPT_MODE == opmode
                            ? "Failed to encrypt data: "
                            : "Failed to decrypt data: ";
            throw new IllegalArgumentException(errorMessage, e);
        }
    }

    /**
     * Encrypt padded data.
     *
     * @param paddedData the padded data to encrypt.
     * @param keyBytes the encryption key.
     * @param ivBytes the initialization vector (IV).
     * @return the encrypted and padded data.
     */
    public byte[] encrypt(byte[] paddedData, byte[] keyBytes, byte[] ivBytes) {
        try {
            return doCipherAction(paddedData, keyBytes, ivBytes, Cipher.ENCRYPT_MODE);
        } catch (IllegalBlockSizeException e) {
            throw new IllegalArgumentException("Failed to encrypt data: ", e);
        }
    }

    /**
     * Decrypt the encrypted and padded data.
     *
     * @param encryptedData the encrypted and padded data.
     * @param keyBytes the decryption key.
     * @param ivBytes the initialization vector (IV).
     * @return the decrypted and padded data.
     * @throws IllegalBlockSizeException if the total encryptedData length is not a multiple of
     *     block size.
     */
    public byte[] decrypt(byte[] encryptedData, byte[] keyBytes, byte[] ivBytes)
            throws IllegalBlockSizeException {
        return doCipherAction(encryptedData, keyBytes, ivBytes, Cipher.DECRYPT_MODE);
    }

    @Override
    protected IpSecAlgorithm buildIpSecAlgorithmWithKeyImpl(byte[] key) {
        return new IpSecAlgorithm(getIpSecAlgorithmName(getAlgorithmId()), key);
    }

    private static byte[] concatenateByteArray(byte[] left, byte[] right) {
        byte[] result = new byte[left.length + right.length];
        System.arraycopy(left, 0, result, 0, left.length);
        System.arraycopy(right, 0, result, left.length, right.length);

        return result;
    }
}

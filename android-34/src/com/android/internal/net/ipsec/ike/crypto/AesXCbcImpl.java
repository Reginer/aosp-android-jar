/*
 * Copyright (C) 2020 The Android Open Source Project
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

import com.android.internal.util.HexDump;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesXCbcImpl {
    // Use AES CBC as per NIST Special Publication 800-38B 6.2
    private static final String AES_CBC = "AES/CBC/NoPadding";

    private static final int AES_CBC_IV_LEN = 16;

    private static final int AES_CBC_BLOCK_LEN = 16;

    private static final int AES_XCBC_96_MAC_LEN = 12;

    private final Cipher mCipher;

    private static final String KEY1_SEED_HEX_STRING = "01010101010101010101010101010101";
    private static final String KEY2_SEED_HEX_STRING = "02020202020202020202020202020202";
    private static final String KEY3_SEED_HEX_STRING = "03030303030303030303030303030303";

    private static final byte[] E_INITIAL = new byte[AES_CBC_BLOCK_LEN];

    public AesXCbcImpl() throws GeneralSecurityException {
        mCipher = Cipher.getInstance(AES_CBC);
    }

    /** Calculate the MAC */
    public byte[] mac(byte[] keyBytes, byte[] dataToSign, boolean needTruncation) {
        // See RFC 3566#section-4 for the algorithm
        int blockSize = mCipher.getBlockSize();
        boolean isPaddingNeeded = dataToSign.length % blockSize != 0;

        byte[] paddedData = dataToSign;
        if (isPaddingNeeded) {
            paddedData = padData(dataToSign, blockSize);
        }

        // (1)  Derive 3 128-bit keys (K1, K2 and K3) from the 128-bit secret key K, as follows:
        // K1 = 0x01010101010101010101010101010101 encrypted with Key K
        // K2 = 0x02020202020202020202020202020202 encrypted with Key K
        // K3 = 0x03030303030303030303030303030303 encrypted with Key K
        byte[] key1 = encryptAesBlock(keyBytes, HexDump.hexStringToByteArray(KEY1_SEED_HEX_STRING));
        byte[] key2 = encryptAesBlock(keyBytes, HexDump.hexStringToByteArray(KEY2_SEED_HEX_STRING));
        byte[] key3 = encryptAesBlock(keyBytes, HexDump.hexStringToByteArray(KEY3_SEED_HEX_STRING));

        // (2)  Define E[0] = 0x00000000000000000000000000000000
        byte[] e = E_INITIAL;

        int numMessageBlocks = paddedData.length / blockSize;

        // (3)  For each block M[i], where i = 1 ... n-1:
        // XOR M[i] with E[i-1], then encrypt the result with Key K1, yielding E[i].
        byte[] message;
        for (int i = 0; i < numMessageBlocks - 1; ++i) {
            message = Arrays.copyOfRange(paddedData, i * blockSize, i * blockSize + blockSize);
            message = xorByteArrays(message, e);
            e = encryptAesBlock(key1, message);
        }

        // (4)  For block M[n]:
        // a)  If the blocksize of M[n] is 128 bits:
        // XOR M[n] with E[n-1] and Key K2, then encrypt the result with Key K1, yielding E[n].
        //
        // b)  If the blocksize of M[n] is less than 128 bits:
        //
        // i)  Pad M[n] with a single "1" bit, followed by the number of "0" bits (possibly none)
        // required to increase M[n]'s blocksize to 128 bits.
        //
        // ii) XOR M[n] with E[n-1] and Key K3, then encrypt the result with Key K1, yielding E[n].
        message = Arrays.copyOfRange(paddedData, paddedData.length - blockSize, paddedData.length);
        message = xorByteArrays(message, e);
        if (isPaddingNeeded) {
            message = xorByteArrays(message, key3);
        } else {
            message = xorByteArrays(message, key2);
        }

        byte[] encryptedMessage = encryptAesBlock(key1, message);

        // AES-XCBC-MAC-96 algorithm requires the output to be truncated to 96-bits
        if (needTruncation) {
            encryptedMessage = Arrays.copyOfRange(encryptedMessage, 0, AES_XCBC_96_MAC_LEN);
        }

        return encryptedMessage;
    }

    private static byte[] xorByteArrays(byte[] message, byte[] e) {
        byte[] output = new byte[message.length];

        for (int i = 0; i < output.length; ++i) {
            output[i] = (byte) (message[i] ^ e[i]);
        }

        return output;
    }

    private static byte[] padData(byte[] dataToSign, int blockSize) {
        int dataLen = dataToSign.length;
        int padLen = (dataLen + blockSize - 1) / blockSize * blockSize - dataLen;
        ByteBuffer paddedData = ByteBuffer.allocate(dataLen + padLen);
        byte[] padding = new byte[padLen];

        // Obligatory 10* Padding as per RFC 3566#section-4
        padding[0] = (byte) 128;
        paddedData.put(dataToSign).put(padding);
        return paddedData.array();
    }

    private byte[] encryptAesBlock(byte[] keyBytes, byte[] dataToEncrypt) {
        // IV is zero as per the recommendation in NIST Special Publication 800-38B
        IvParameterSpec iv = new IvParameterSpec(new byte[AES_CBC_IV_LEN]);

        ByteBuffer inputBuffer = ByteBuffer.wrap(dataToEncrypt);
        ByteBuffer outputBuffer = ByteBuffer.allocate(dataToEncrypt.length);
        try {
            mCipher.init(
                    Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, mCipher.getAlgorithm()), iv);
            mCipher.doFinal(inputBuffer, outputBuffer);
            return outputBuffer.array();
        } catch (InvalidAlgorithmParameterException
                | InvalidKeyException
                | BadPaddingException
                | IllegalBlockSizeException
                | ShortBufferException e) {
            throw new IllegalArgumentException("Failed to sign data: ", e);
        }
    }
}

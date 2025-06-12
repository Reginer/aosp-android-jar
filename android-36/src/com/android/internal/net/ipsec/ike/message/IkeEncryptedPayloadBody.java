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

package com.android.internal.net.ipsec.ike.message;

import android.net.ipsec.ike.exceptions.IkeProtocolException;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.ipsec.ike.crypto.IkeCipher;
import com.android.internal.net.ipsec.ike.crypto.IkeCombinedModeCipher;
import com.android.internal.net.ipsec.ike.crypto.IkeMacIntegrity;
import com.android.internal.net.ipsec.ike.crypto.IkeNormalModeCipher;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.IllegalBlockSizeException;

/**
 * IkeEncryptedPayloadBody is a package private class that represents an IKE payload substructure
 * that contains initialization vector, encrypted content, padding, pad length and integrity
 * checksum.
 *
 * <p>Both an Encrypted Payload (IkeSkPayload) and an EncryptedFragmentPayload (IkeSkfPayload)
 * consists of an IkeEncryptedPayloadBody instance.
 *
 * <p>When using normal cipher with separate integrity algorithm, data to authenticate includes
 * bytes from beginning of IKE header to the pad length, which are concatenation of IKE header,
 * current payload header, iv and encrypted and padded data.
 *
 * <p>When using AEAD, additional authentication data(also known as) associated data is required. It
 * MUST include bytes from beginning of IKE header to the last octet of the Payload Header of the
 * Encrypted Payload. Note fragment number and total fragments are also included if Encrypted
 * Payload is SKF.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#page-105">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @see <a href="https://tools.ietf.org/html/rfc7383#page-6">RFC 7383, Internet Key Exchange
 *     Protocol Version 2 (IKEv2) Message Fragmentation</a>
 */
final class IkeEncryptedPayloadBody {
    // Length of pad length field.
    private static final int PAD_LEN_LEN = 1;

    private final byte[] mUnencryptedData;
    private final byte[] mEncryptedAndPaddedData;
    private final byte[] mIv;
    private final byte[] mIntegrityChecksum;

    /**
     * Package private constructor for constructing an instance of IkeEncryptedPayloadBody from
     * decrypting an incoming packet.
     */
    IkeEncryptedPayloadBody(
            byte[] message,
            int encryptedBodyOffset,
            IkeMacIntegrity integrityMac,
            IkeCipher decryptCipher,
            byte[] integrityKey,
            byte[] decryptionKey)
            throws IkeProtocolException, GeneralSecurityException {
        ByteBuffer inputBuffer = ByteBuffer.wrap(message);

        // Skip IKE header and generic payload header (and SKF header)
        inputBuffer.get(new byte[encryptedBodyOffset]);

        // Extract bytes for authentication and decryption.
        int expectedIvLen = decryptCipher.getIvLen();
        mIv = new byte[expectedIvLen];

        int checksumLen = getChecksum(integrityMac, decryptCipher);
        int encryptedDataLen = message.length - (encryptedBodyOffset + expectedIvLen + checksumLen);
        // IkeMessage will catch exception if encryptedDataLen is negative.
        mEncryptedAndPaddedData = new byte[encryptedDataLen];

        mIntegrityChecksum = new byte[checksumLen];
        inputBuffer.get(mIv).get(mEncryptedAndPaddedData).get(mIntegrityChecksum);

        if (decryptCipher.isAead()) {
            byte[] dataToAuthenticate = Arrays.copyOfRange(message, 0, encryptedBodyOffset);
            mUnencryptedData =
                    combinedModeDecrypt(
                            (IkeCombinedModeCipher) decryptCipher,
                            mEncryptedAndPaddedData,
                            mIntegrityChecksum,
                            dataToAuthenticate,
                            decryptionKey,
                            mIv);
        } else {
            byte[] dataToAuthenticate =
                    Arrays.copyOfRange(message, 0, message.length - checksumLen);

            validateInboundChecksumOrThrow(
                    dataToAuthenticate, integrityMac, integrityKey, mIntegrityChecksum);
            mUnencryptedData =
                    normalModeDecrypt(
                            mEncryptedAndPaddedData,
                            (IkeNormalModeCipher) decryptCipher,
                            decryptionKey,
                            mIv);
        }
    }

    /**
     * Package private constructor for constructing an instance of IkeEncryptedPayloadBody for
     * building an outbound packet.
     */
    IkeEncryptedPayloadBody(
            IkeHeader ikeHeader,
            @IkePayload.PayloadType int firstPayloadType,
            byte[] skfHeaderBytes,
            byte[] unencryptedPayloads,
            IkeMacIntegrity integrityMac,
            IkeCipher encryptCipher,
            byte[] integrityKey,
            byte[] encryptionKey) {
        this(
                ikeHeader,
                firstPayloadType,
                skfHeaderBytes,
                unencryptedPayloads,
                integrityMac,
                encryptCipher,
                integrityKey,
                encryptionKey,
                encryptCipher.generateIv(),
                calculatePadding(unencryptedPayloads.length, encryptCipher.getBlockSize()));
    }

    /** Package private constructor only for testing. */
    @VisibleForTesting
    IkeEncryptedPayloadBody(
            IkeHeader ikeHeader,
            @IkePayload.PayloadType int firstPayloadType,
            byte[] skfHeaderBytes,
            byte[] unencryptedPayloads,
            IkeMacIntegrity integrityMac,
            IkeCipher encryptCipher,
            byte[] integrityKey,
            byte[] encryptionKey,
            byte[] iv,
            byte[] padding) {
        mUnencryptedData = unencryptedPayloads;

        mIv = iv;
        if (encryptCipher.isAead()) {
            byte[] paddedDataWithChecksum =
                    combinedModeEncrypt(
                            (IkeCombinedModeCipher) encryptCipher,
                            ikeHeader,
                            firstPayloadType,
                            skfHeaderBytes,
                            unencryptedPayloads,
                            encryptionKey,
                            iv,
                            padding);

            int checkSumLen = ((IkeCombinedModeCipher) encryptCipher).getChecksumLen();
            mIntegrityChecksum = new byte[checkSumLen];
            mEncryptedAndPaddedData = new byte[paddedDataWithChecksum.length - checkSumLen];

            ByteBuffer buffer = ByteBuffer.wrap(paddedDataWithChecksum);
            buffer.get(mEncryptedAndPaddedData);
            buffer.get(mIntegrityChecksum);
        } else {
            // Encrypt data
            mEncryptedAndPaddedData =
                    normalModeEncrypt(
                            unencryptedPayloads,
                            (IkeNormalModeCipher) encryptCipher,
                            encryptionKey,
                            iv,
                            padding);
            // Calculate checksum
            mIntegrityChecksum =
                    generateOutboundChecksum(
                            ikeHeader,
                            firstPayloadType,
                            skfHeaderBytes,
                            integrityMac,
                            iv,
                            mEncryptedAndPaddedData,
                            integrityKey);
        }
    }

    private int getChecksum(IkeMacIntegrity integrityMac, IkeCipher decryptCipher) {
        if (decryptCipher.isAead()) {
            return ((IkeCombinedModeCipher) decryptCipher).getChecksumLen();
        } else {
            return integrityMac.getChecksumLen();
        }
    }

    /** Package private for testing */
    @VisibleForTesting
    static byte[] generateOutboundChecksum(
            IkeHeader ikeHeader,
            @IkePayload.PayloadType int firstPayloadType,
            byte[] skfHeaderBytes,
            IkeMacIntegrity integrityMac,
            byte[] iv,
            byte[] encryptedAndPaddedData,
            byte[] integrityKey) {
        // Length from encrypted payload header to the Pad Length field
        int encryptedPayloadHeaderToPadLen =
                IkePayload.GENERIC_HEADER_LENGTH
                        + skfHeaderBytes.length
                        + iv.length
                        + encryptedAndPaddedData.length;

        // Calculate length of authentication data and allocate ByteBuffer.
        int dataToAuthenticateLength = IkeHeader.IKE_HEADER_LENGTH + encryptedPayloadHeaderToPadLen;
        ByteBuffer authenticatedSectionBuffer = ByteBuffer.allocate(dataToAuthenticateLength);

        // Build data to authenticate.
        int encryptedPayloadLength = encryptedPayloadHeaderToPadLen + integrityMac.getChecksumLen();
        ikeHeader.encodeToByteBuffer(authenticatedSectionBuffer, encryptedPayloadLength);
        IkePayload.encodePayloadHeaderToByteBuffer(
                firstPayloadType, encryptedPayloadLength, authenticatedSectionBuffer);
        authenticatedSectionBuffer.put(skfHeaderBytes).put(iv).put(encryptedAndPaddedData);

        // Calculate checksum
        return integrityMac.generateChecksum(integrityKey, authenticatedSectionBuffer.array());
    }

    /** Package private for testing */
    @VisibleForTesting
    static void validateInboundChecksumOrThrow(
            byte[] dataToAuthenticate,
            IkeMacIntegrity integrityMac,
            byte[] integrityKey,
            byte[] integrityChecksum)
            throws GeneralSecurityException {
        // TODO: Make it package private and add test.
        int checkSumLen = integrityChecksum.length;
        byte[] calculatedChecksum = integrityMac.generateChecksum(integrityKey, dataToAuthenticate);

        if (!Arrays.equals(integrityChecksum, calculatedChecksum)) {
            throw new GeneralSecurityException("Message authentication failed.");
        }
    }

    /** Package private for testing */
    @VisibleForTesting
    static byte[] normalModeEncrypt(
            byte[] dataToEncrypt,
            IkeNormalModeCipher encryptCipher,
            byte[] encryptionKey,
            byte[] iv,
            byte[] padding) {
        byte[] paddedData = getPaddedData(dataToEncrypt, padding);

        // Encrypt data.
        return encryptCipher.encrypt(paddedData, encryptionKey, iv);
    }

    /** Package private for testing */
    @VisibleForTesting
    static byte[] normalModeDecrypt(
            byte[] encryptedData,
            IkeNormalModeCipher decryptCipher,
            byte[] decryptionKey,
            byte[] iv)
            throws IllegalBlockSizeException {
        byte[] paddedPlaintext = decryptCipher.decrypt(encryptedData, decryptionKey, iv);

        return stripPadding(paddedPlaintext);
    }

    /** Package private for testing */
    @VisibleForTesting
    static byte[] combinedModeEncrypt(
            IkeCombinedModeCipher encryptCipher,
            IkeHeader ikeHeader,
            @IkePayload.PayloadType int firstPayloadType,
            byte[] skfHeaderBytes,
            byte[] dataToEncrypt,
            byte[] encryptionKey,
            byte[] iv,
            byte[] padding) {
        int dataToAuthenticateLength =
                IkeHeader.IKE_HEADER_LENGTH
                        + IkePayload.GENERIC_HEADER_LENGTH
                        + skfHeaderBytes.length;
        ByteBuffer authenticatedSectionBuffer = ByteBuffer.allocate(dataToAuthenticateLength);

        byte[] paddedData = getPaddedData(dataToEncrypt, padding);
        int encryptedPayloadLength =
                IkePayload.GENERIC_HEADER_LENGTH
                        + skfHeaderBytes.length
                        + iv.length
                        + paddedData.length
                        + encryptCipher.getChecksumLen();
        ikeHeader.encodeToByteBuffer(authenticatedSectionBuffer, encryptedPayloadLength);
        IkePayload.encodePayloadHeaderToByteBuffer(
                firstPayloadType, encryptedPayloadLength, authenticatedSectionBuffer);
        authenticatedSectionBuffer.put(skfHeaderBytes);

        return encryptCipher.encrypt(
                paddedData, authenticatedSectionBuffer.array(), encryptionKey, iv);
    }

    /** Package private for testing */
    @VisibleForTesting
    static byte[] combinedModeDecrypt(
            IkeCombinedModeCipher decryptCipher,
            byte[] encryptedData,
            byte[] checksum,
            byte[] dataToAuthenticate,
            byte[] decryptionKey,
            byte[] iv)
            throws AEADBadTagException {
        ByteBuffer dataWithChecksumBuffer =
                ByteBuffer.allocate(encryptedData.length + checksum.length);
        dataWithChecksumBuffer.put(encryptedData);
        dataWithChecksumBuffer.put(checksum);
        dataWithChecksumBuffer.rewind();

        byte[] paddedPlaintext =
                decryptCipher.decrypt(
                        dataWithChecksumBuffer.array(), dataToAuthenticate, decryptionKey, iv);

        return stripPadding(paddedPlaintext);
    }

    /** Package private for testing */
    @VisibleForTesting
    static byte[] calculatePadding(int dataToEncryptLength, int blockSize) {
        // Sum of dataToEncryptLength, PAD_LEN_LEN and padLength should be aligned with block size.
        int unpaddedLen = dataToEncryptLength + PAD_LEN_LEN;
        int padLength = (unpaddedLen + blockSize - 1) / blockSize * blockSize - unpaddedLen;
        byte[] padding = new byte[padLength];

        // According to RFC 7296, "Padding MAY contain any value".
        new SecureRandom().nextBytes(padding);

        return padding;
    }

    private static byte[] getPaddedData(byte[] data, byte[] padding) {
        int padLength = padding.length;
        int paddedDataLength = data.length + padLength + PAD_LEN_LEN;
        ByteBuffer padBuffer = ByteBuffer.allocate(paddedDataLength);
        padBuffer.put(data).put(padding).put((byte) padLength);

        return padBuffer.array();
    }

    private static byte[] stripPadding(byte[] paddedPlaintext) {
        // Remove padding. Pad length value is the last byte of the padded unencrypted data.
        int padLength = Byte.toUnsignedInt(paddedPlaintext[paddedPlaintext.length - 1]);
        int decryptedDataLen = paddedPlaintext.length - padLength - PAD_LEN_LEN;

        return Arrays.copyOfRange(paddedPlaintext, 0, decryptedDataLen);
    }

    /** Package private */
    byte[] getUnencryptedData() {
        return mUnencryptedData;
    }

    /** Package private */
    int getLength() {
        return (mIv.length + mEncryptedAndPaddedData.length + mIntegrityChecksum.length);
    }

    /** Package private */
    byte[] encode() {
        ByteBuffer buffer = ByteBuffer.allocate(getLength());
        buffer.put(mIv).put(mEncryptedAndPaddedData).put(mIntegrityChecksum);
        return buffer.array();
    }
}

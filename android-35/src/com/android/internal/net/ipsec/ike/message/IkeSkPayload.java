/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.annotation.Nullable;
import android.net.ipsec.ike.exceptions.IkeProtocolException;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.ipsec.ike.crypto.IkeCipher;
import com.android.internal.net.ipsec.ike.crypto.IkeMacIntegrity;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

/**
 * IkeSkPayload represents the common information of an Encrypted and Authenticated Payload and an
 * Encrypted and Authenticated Fragment Payload.
 *
 * <p>It contains other payloads in encrypted form. It is must be the last payload in the message.
 * It should be the only payload in this implementation.
 *
 * <p>Critical bit must be ignored when doing decoding.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#page-105">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public class IkeSkPayload extends IkePayload {

    protected final IkeEncryptedPayloadBody mIkeEncryptedPayloadBody;

    /**
     * Construct an instance of IkeSkPayload from decrypting an incoming packet.
     *
     * @param critical indicates if it is a critical payload.
     * @param message the byte array contains the whole IKE message.
     * @param integrityMac the negotiated integrity algorithm.
     * @param decryptCipher the negotiated encryption algorithm.
     * @param integrityKey the negotiated integrity algorithm key.
     * @param decryptionKey the negotiated decryption key.
     */
    @VisibleForTesting
    IkeSkPayload(
            boolean critical,
            byte[] message,
            @Nullable IkeMacIntegrity integrityMac,
            IkeCipher decryptCipher,
            byte[] integrityKey,
            byte[] decryptionKey)
            throws IkeProtocolException, GeneralSecurityException {

        this(
                false /*isSkf*/,
                critical,
                IkeHeader.IKE_HEADER_LENGTH + GENERIC_HEADER_LENGTH,
                message,
                integrityMac,
                decryptCipher,
                integrityKey,
                decryptionKey);
    }

    /** Construct an instance of IkeSkPayload for testing.*/
    @VisibleForTesting
    IkeSkPayload(boolean isSkf, IkeEncryptedPayloadBody encryptedPayloadBody) {
        super(isSkf ? PAYLOAD_TYPE_SKF : PAYLOAD_TYPE_SK, false/*critical*/);
        mIkeEncryptedPayloadBody = encryptedPayloadBody;
    }

    /** Construct an instance of IkeSkPayload from decrypting an incoming packet. */
    protected IkeSkPayload(
            boolean isSkf,
            boolean critical,
            int encryptedBodyOffset,
            byte[] message,
            @Nullable IkeMacIntegrity integrityMac,
            IkeCipher decryptCipher,
            byte[] integrityKey,
            byte[] decryptionKey)
            throws IkeProtocolException, GeneralSecurityException {
        super(isSkf ? PAYLOAD_TYPE_SKF : PAYLOAD_TYPE_SK, critical);

        // TODO: Support constructing IkeEncryptedPayloadBody using AEAD.

        mIkeEncryptedPayloadBody =
                new IkeEncryptedPayloadBody(
                        message,
                        encryptedBodyOffset,
                        integrityMac,
                        decryptCipher,
                        integrityKey,
                        decryptionKey);
    }

    /**
     * Construct an instance of IkeSkPayload for building outbound packet.
     *
     * @param ikeHeader the IKE header.
     * @param firstPayloadType the type of first payload nested in SkPayload.
     * @param unencryptedPayloads the encoded payload list to protect.
     * @param integrityMac the negotiated integrity algorithm.
     * @param encryptCipher the negotiated encryption algorithm.
     * @param integrityKey the negotiated integrity algorithm key.
     * @param encryptionKey the negotiated encryption key.
     */
    @VisibleForTesting
    IkeSkPayload(
            IkeHeader ikeHeader,
            @PayloadType int firstPayloadType,
            byte[] unencryptedPayloads,
            @Nullable IkeMacIntegrity integrityMac,
            IkeCipher encryptCipher,
            byte[] integrityKey,
            byte[] encryptionKey) {

        this(
                ikeHeader,
                firstPayloadType,
                new byte[0] /*skfHeaderBytes*/,
                unencryptedPayloads,
                integrityMac,
                encryptCipher,
                integrityKey,
                encryptionKey);
    }

    /** Construct an instance of IkeSkPayload for building outbound packet. */
    @VisibleForTesting
    protected IkeSkPayload(
            IkeHeader ikeHeader,
            @PayloadType int firstPayloadType,
            byte[] skfHeaderBytes,
            byte[] unencryptedPayloads,
            @Nullable IkeMacIntegrity integrityMac,
            IkeCipher encryptCipher,
            byte[] integrityKey,
            byte[] encryptionKey) {
        super(skfHeaderBytes.length == 0 ? PAYLOAD_TYPE_SK : PAYLOAD_TYPE_SKF, false);

        // TODO: Support constructing IkeEncryptedPayloadBody using AEAD.

        mIkeEncryptedPayloadBody =
                new IkeEncryptedPayloadBody(
                        ikeHeader,
                        firstPayloadType,
                        skfHeaderBytes,
                        unencryptedPayloads,
                        integrityMac,
                        encryptCipher,
                        integrityKey,
                        encryptionKey);
    }

    /**
     * Return unencrypted data.
     *
     * @return unencrypted data in a byte array.
     */
    public byte[] getUnencryptedData() {
        return mIkeEncryptedPayloadBody.getUnencryptedData();
    }

    /**
     * Encode this payload to a ByteBuffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param byteBuffer destination ByteBuffer that stores encoded payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        encodePayloadHeaderToByteBuffer(nextPayload, getPayloadLength(), byteBuffer);
        byteBuffer.put(mIkeEncryptedPayloadBody.encode());
    }

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        return GENERIC_HEADER_LENGTH + mIkeEncryptedPayloadBody.getLength();
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        return "SK";
    }
}

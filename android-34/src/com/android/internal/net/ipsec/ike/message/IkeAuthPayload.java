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

import android.annotation.IntDef;
import android.net.ipsec.ike.exceptions.AuthenticationFailedException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;

import com.android.internal.net.ipsec.ike.crypto.IkeMacPrf;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * IkeAuthPayload is an abstract class that represents the common information for all Authentication
 * Payload with different authentication methods.
 *
 * <p>Authentication Payload using different authentication method should implement its own
 * subclasses with its own logic for signing data, decoding auth data and verifying signature.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.8">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public abstract class IkeAuthPayload extends IkePayload {
    // Length of header of Authentication Payload in octets
    private static final int AUTH_HEADER_LEN = 4;
    // Length of reserved field in octets
    private static final int AUTH_RESERVED_FIELD_LEN = 3;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        AUTH_METHOD_RSA_DIGITAL_SIGN,
        AUTH_METHOD_PRE_SHARED_KEY,
        AUTH_METHOD_GENERIC_DIGITAL_SIGN
    })
    public @interface AuthMethod {}

    // RSA signature-based authentication. Use SHA-1 for hash algorithm.
    public static final int AUTH_METHOD_RSA_DIGITAL_SIGN = 1;
    // PSK-based authentication.
    public static final int AUTH_METHOD_PRE_SHARED_KEY = 2;
    // Generic method for all types of signature-based authentication.
    public static final int AUTH_METHOD_GENERIC_DIGITAL_SIGN = 14;

    @AuthMethod public final int authMethod;

    protected IkeAuthPayload(boolean critical, int authMethod) {
        super(PAYLOAD_TYPE_AUTH, critical);
        this.authMethod = authMethod;
    }

    protected static IkeAuthPayload getIkeAuthPayload(boolean critical, byte[] payloadBody)
            throws IkeProtocolException {
        ByteBuffer inputBuffer = ByteBuffer.wrap(payloadBody);

        int authMethod = Byte.toUnsignedInt(inputBuffer.get());
        // Skip reserved field
        byte[] reservedField = new byte[AUTH_RESERVED_FIELD_LEN];
        inputBuffer.get(reservedField);

        byte[] authData = new byte[payloadBody.length - AUTH_HEADER_LEN];
        inputBuffer.get(authData);
        switch (authMethod) {
            case AUTH_METHOD_PRE_SHARED_KEY:
                return new IkeAuthPskPayload(critical, authData);
            case AUTH_METHOD_RSA_DIGITAL_SIGN:
                return new IkeAuthDigitalSignPayload(
                        critical, AUTH_METHOD_RSA_DIGITAL_SIGN, authData);
            case AUTH_METHOD_GENERIC_DIGITAL_SIGN:
                return new IkeAuthDigitalSignPayload(
                        critical, AUTH_METHOD_GENERIC_DIGITAL_SIGN, authData);
            default:
                throw new AuthenticationFailedException("Unsupported authentication method");
        }
    }

    // When not using EAP, the peers are authenticated by having each sign a block of data named as
    // SignedOctets. IKE initiator's SignedOctets are the concatenation of the IKE_INIT request
    // message, the Nonce of IKE responder and the signed ID-Initiator payload body. Similarly, IKE
    // responder's SignedOctets are the concatenation of the IKE_INIT response message, the Nonce of
    // IKE initiator and the signed ID-Responder payload body.
    protected static byte[] getSignedOctets(
            byte[] ikeInitBytes,
            byte[] nonce,
            byte[] idPayloadBodyBytes,
            IkeMacPrf ikePrf,
            byte[] prfKeyBytes) {
        byte[] signedidPayloadBodyBytes = ikePrf.signBytes(prfKeyBytes, idPayloadBodyBytes);

        ByteBuffer buffer =
                ByteBuffer.allocate(
                        ikeInitBytes.length + nonce.length + signedidPayloadBodyBytes.length);
        buffer.put(ikeInitBytes).put(nonce).put(signedidPayloadBodyBytes);

        return buffer.array();
    }

    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        encodePayloadHeaderToByteBuffer(nextPayload, getPayloadLength(), byteBuffer);
        byteBuffer.put((byte) authMethod).put(new byte[AUTH_RESERVED_FIELD_LEN]);
        encodeAuthDataToByteBuffer(byteBuffer);
    }

    @Override
    protected int getPayloadLength() {
        return GENERIC_HEADER_LENGTH + AUTH_HEADER_LEN + getAuthDataLength();
    }

    protected abstract void encodeAuthDataToByteBuffer(ByteBuffer byteBuffer);

    protected abstract int getAuthDataLength();
}

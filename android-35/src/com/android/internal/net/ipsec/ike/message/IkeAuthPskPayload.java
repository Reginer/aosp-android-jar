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

import android.net.ipsec.ike.exceptions.AuthenticationFailedException;

import com.android.internal.net.ipsec.ike.crypto.IkeMacPrf;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * IkeAuthPskPayload represents an Authentication Payload using Pre-Shared Key to do authentication.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.8">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeAuthPskPayload extends IkeAuthPayload {
    // Hex of ASCII characters "Key Pad for IKEv2" for calculating PSK signature.
    private static final byte[] IKE_KEY_PAD_STRING_ASCII_HEX_BYTES = {
        (byte) 0x4b, (byte) 0x65, (byte) 0x79, (byte) 0x20,
        (byte) 0x50, (byte) 0x61, (byte) 0x64, (byte) 0x20,
        (byte) 0x66, (byte) 0x6f, (byte) 0x72, (byte) 0x20,
        (byte) 0x49, (byte) 0x4b, (byte) 0x45, (byte) 0x76,
        (byte) 0x32
    };

    public final byte[] signature;

    /**
     * Construct IkeAuthPskPayload for received IKE packet in the context of {@link
     * IkePayloadFactory}.
     */
    protected IkeAuthPskPayload(boolean critical, byte[] authData) {
        super(critical, IkeAuthPayload.AUTH_METHOD_PRE_SHARED_KEY);
        signature = authData;
    }

    /**
     * Construct IkeAuthPskPayload for an outbound IKE packet.
     *
     * <p>Since IKE library is always a client, outbound IkeAuthPskPayload always signs IKE
     * initiator's SignedOctets, which is concatenation of the IKE_INIT request message, the Nonce
     * of IKE responder and the signed ID-Initiator payload body.
     *
     * @param psk locally stored pre-shared key
     * @param ikeInitBytes IKE_INIT request for calculating IKE initiator's SignedOctets.
     * @param nonce nonce of IKE responder for calculating IKE initiator's SignedOctets.
     * @param idPayloadBodyBytes ID-Initiator payload body for calculating IKE initiator's
     *     SignedOctets.
     * @param ikePrf the negotiated PRF.
     * @param prfKeyBytes the negotiated PRF key.
     */
    public IkeAuthPskPayload(
            byte[] psk,
            byte[] ikeInitBytes,
            byte[] nonce,
            byte[] idPayloadBodyBytes,
            IkeMacPrf ikePrf,
            byte[] prfKeyBytes) {
        super(false, IkeAuthPayload.AUTH_METHOD_PRE_SHARED_KEY);
        signature =
                calculatePskSignature(
                        psk, ikeInitBytes, nonce, idPayloadBodyBytes, ikePrf, prfKeyBytes);
    }

    private static byte[] calculatePskSignature(
            byte[] psk,
            byte[] ikeInitBytes,
            byte[] nonce,
            byte[] idPayloadBodyBytes,
            IkeMacPrf ikePrf,
            byte[] prfKeyBytes) {
        byte[] signingKeyBytes = ikePrf.signBytes(psk, IKE_KEY_PAD_STRING_ASCII_HEX_BYTES);
        byte[] dataToSignBytes =
                getSignedOctets(ikeInitBytes, nonce, idPayloadBodyBytes, ikePrf, prfKeyBytes);

        return ikePrf.signBytes(signingKeyBytes, dataToSignBytes);
    }

    /**
     * Verify received signature in inbound IKE packet.
     *
     * <p>Since IKE library is always a client, inbound IkeAuthPskPayload always signs IKE
     * responder's SignedOctets, which is concatenation of the IKE_INIT response message, the Nonce
     * of IKE initiator and the signed ID-Responder payload body.
     *
     * @param psk locally stored pre-shared key
     * @param ikeInitBytes IKE_INIT response for calculating IKE responder's SignedOctets.
     * @param nonce nonce of IKE initiator for calculating IKE responder's SignedOctets.
     * @param idPayloadBodyBytes ID-Responder payload body for calculating IKE responder's
     *     SignedOctets.
     * @param ikePrf the negotiated PRF.
     * @param prfKeyBytes the negotiated PRF key.
     * @throws AuthenticationFailedException if received signature is not equal to calculated
     *     signature.
     */
    public void verifyInboundSignature(
            byte[] psk,
            byte[] ikeInitBytes,
            byte[] nonce,
            byte[] idPayloadBodyBytes,
            IkeMacPrf ikePrf,
            byte[] prfKeyBytes)
            throws AuthenticationFailedException {
        byte[] calculatedSignature =
                calculatePskSignature(
                        psk, ikeInitBytes, nonce, idPayloadBodyBytes, ikePrf, prfKeyBytes);
        if (!Arrays.equals(signature, calculatedSignature)) {
            throw new AuthenticationFailedException("Signature verification failed.");
        }
    }

    @Override
    protected void encodeAuthDataToByteBuffer(ByteBuffer byteBuffer) {
        byteBuffer.put(signature);
    }

    @Override
    protected int getAuthDataLength() {
        return signature.length;
    }

    @Override
    public String getTypeString() {
        return "Auth(PSK)";
    }
}

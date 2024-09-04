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

import static android.net.ipsec.ike.IkeManager.getIkeLog;

import android.annotation.NonNull;
import android.annotation.StringDef;
import android.net.ipsec.ike.exceptions.AuthenticationFailedException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;
import android.util.ArraySet;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.ipsec.ike.crypto.IkeMacPrf;
import com.android.internal.net.ipsec.ike.message.IkeAuthPayload.AuthMethod;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IkeAuthDigitalSignPayload represents Authentication Payload using a specific or generic digital
 * signature authentication method.
 *
 * <p>If AUTH_METHOD_RSA_DIGITAL_SIGN is used, then the hash algorithm is SHA1. If
 * AUTH_METHOD_GENERIC_DIGITAL_SIGN is used, the signature algorithm and hash algorithm are
 * extracted from authentication data.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.8">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @see <a href="https://tools.ietf.org/html/rfc7427">RFC 7427, Signature Authentication in the
 *     Internet Key Exchange Version 2 (IKEv2)</a>
 */
public class IkeAuthDigitalSignPayload extends IkeAuthPayload {
    private static final String TAG = IkeAuthDigitalSignPayload.class.getSimpleName();

    private static final String KEY_ALGO_NAME = "RSA";
    private static final byte SIGNATURE_ALGO_ASN1_BYTES_LEN = (byte) 15;
    private static final byte SIGNATURE_ALGO_ASN1_BYTES_LEN_LEN = (byte) 1;

    // Byte arrays of DER encoded identifier ASN.1 objects that indicates the algorithm used to
    // generate the signature, extracted from
    // <a href="https://tools.ietf.org/html/rfc7427#appendix-A"> RFC 7427. There is no need to
    // understand the encoding process. They are just constants to indicate the algorithm type.
    private static final byte[] PKI_ALGO_ID_DER_BYTES_RSA_SHA1 = {
        (byte) 0x30, (byte) 0x0d, (byte) 0x06, (byte) 0x09,
        (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86,
        (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x01,
        (byte) 0x05, (byte) 0x05, (byte) 0x00
    };
    private static final byte[] PKI_ALGO_ID_DER_BYTES_RSA_SHA2_256 = {
        (byte) 0x30, (byte) 0x0d, (byte) 0x06, (byte) 0x09,
        (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86,
        (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x01,
        (byte) 0x0b, (byte) 0x05, (byte) 0x00
    };
    private static final byte[] PKI_ALGO_ID_DER_BYTES_RSA_SHA2_384 = {
        (byte) 0x30, (byte) 0x0d, (byte) 0x06, (byte) 0x09,
        (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86,
        (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x01,
        (byte) 0x0c, (byte) 0x05, (byte) 0x00
    };
    private static final byte[] PKI_ALGO_ID_DER_BYTES_RSA_SHA2_512 = {
        (byte) 0x30, (byte) 0x0d, (byte) 0x06, (byte) 0x09,
        (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86,
        (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x01,
        (byte) 0x0d, (byte) 0x05, (byte) 0x00
    };

    // Length of ASN.1 object length field.
    private static final int SIGNATURE_ALGO_ASN1_LEN_LEN = 1;

    // Currently we only support RSA for signature algorithm.
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
        SIGNATURE_ALGO_RSA_SHA1,
        SIGNATURE_ALGO_RSA_SHA2_256,
        SIGNATURE_ALGO_RSA_SHA2_384,
        SIGNATURE_ALGO_RSA_SHA2_512
    })
    @VisibleForTesting
    @interface SignatureAlgo {}

    public static final String SIGNATURE_ALGO_RSA_SHA1 = "SHA1withRSA";
    public static final String SIGNATURE_ALGO_RSA_SHA2_256 = "SHA256withRSA";
    public static final String SIGNATURE_ALGO_RSA_SHA2_384 = "SHA384withRSA";
    public static final String SIGNATURE_ALGO_RSA_SHA2_512 = "SHA512withRSA";

    // IKEv2 types for hash algorithms.
    public static final short HASH_ALGORITHM_RSA_SHA1 = 1;
    public static final short HASH_ALGORITHM_RSA_SHA2_256 = 2;
    public static final short HASH_ALGORITHM_RSA_SHA2_384 = 3;
    public static final short HASH_ALGORITHM_RSA_SHA2_512 = 4;
    public static final short[] ALL_SIGNATURE_ALGO_TYPES =
            new short[] {
                HASH_ALGORITHM_RSA_SHA1,
                HASH_ALGORITHM_RSA_SHA2_256,
                HASH_ALGORITHM_RSA_SHA2_384,
                HASH_ALGORITHM_RSA_SHA2_512
            };
    private static final Map<Short, String> SIGNATURE_ALGO_TYPE_TO_NAME = new HashMap<>();

    static {
        SIGNATURE_ALGO_TYPE_TO_NAME.put(HASH_ALGORITHM_RSA_SHA1, SIGNATURE_ALGO_RSA_SHA1);
        SIGNATURE_ALGO_TYPE_TO_NAME.put(HASH_ALGORITHM_RSA_SHA2_256, SIGNATURE_ALGO_RSA_SHA2_256);
        SIGNATURE_ALGO_TYPE_TO_NAME.put(HASH_ALGORITHM_RSA_SHA2_384, SIGNATURE_ALGO_RSA_SHA2_384);
        SIGNATURE_ALGO_TYPE_TO_NAME.put(HASH_ALGORITHM_RSA_SHA2_512, SIGNATURE_ALGO_RSA_SHA2_512);
    }

    public final String signatureAndHashAlgos;
    public final byte[] signature;

    protected IkeAuthDigitalSignPayload(
            boolean critical, @AuthMethod int authMethod, byte[] authData)
            throws IkeProtocolException {
        super(critical, authMethod);
        switch (authMethod) {
            case AUTH_METHOD_RSA_DIGITAL_SIGN:
                signatureAndHashAlgos = SIGNATURE_ALGO_RSA_SHA1;
                signature = authData;
                break;
            case AUTH_METHOD_GENERIC_DIGITAL_SIGN:
                ByteBuffer inputBuffer = ByteBuffer.wrap(authData);

                // Get signature algorithm.
                int signAlgoLen = Byte.toUnsignedInt(inputBuffer.get());
                byte[] signAlgoBytes = new byte[signAlgoLen];
                inputBuffer.get(signAlgoBytes);
                signatureAndHashAlgos = bytesToJavaStandardSignAlgoName(signAlgoBytes);

                // Get signature.
                signature = new byte[authData.length - SIGNATURE_ALGO_ASN1_LEN_LEN - signAlgoLen];
                inputBuffer.get(signature);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized authentication method.");
        }
    }

    /**
     * Construct IkeAuthDigitalSignPayload for an outbound IKE packet.
     *
     * <p>Since IKE library is always a client, outbound IkeAuthDigitalSignPayload always signs IKE
     * initiator's SignedOctets, which is concatenation of the IKE_INIT request message, the Nonce
     * of IKE responder and the signed ID-Initiator payload body.
     *
     * <p>Caller MUST validate that the signatureAlgoName is supported by IKE library.
     *
     * @param genericSignAuthAlgos peer supported signature hash algorithms that can be used with
     *     AUTH_METHOD_GENERIC_DIGITAL_SIGN, or an empty set if peer does not support
     *     AUTH_METHOD_GENERIC_DIGITAL_SIGN
     * @param privateKey the private key of the identity whose signature is going to be generated.
     * @param ikeInitBytes IKE_INIT request for calculating IKE initiator's SignedOctets.
     * @param nonce nonce of IKE responder for calculating IKE initiator's SignedOctets.
     * @param idPayloadBodyBytes ID-Initiator payload body for calculating IKE initiator's
     *     SignedOctets.
     * @param ikePrf the negotiated PRF.
     * @param prfKeyBytes the negotiated PRF initiator key.
     */
    public IkeAuthDigitalSignPayload(
            Set<Short> genericSignAuthAlgos,
            PrivateKey privateKey,
            byte[] ikeInitBytes,
            byte[] nonce,
            byte[] idPayloadBodyBytes,
            IkeMacPrf ikePrf,
            byte[] prfKeyBytes) {
        super(false, getAuthMethod(genericSignAuthAlgos));

        byte[] dataToSignBytes =
                getSignedOctets(ikeInitBytes, nonce, idPayloadBodyBytes, ikePrf, prfKeyBytes);

        String signatureAlgoName = null;
        switch (authMethod) {
            case AUTH_METHOD_RSA_DIGITAL_SIGN:
                signatureAlgoName = SIGNATURE_ALGO_RSA_SHA1;
                break;
            case AUTH_METHOD_GENERIC_DIGITAL_SIGN:
                signatureAlgoName = selectGenericSignAuthAlgo(genericSignAuthAlgos);
                break;
            default:
                throw new IllegalStateException("Invalid auth method: " + authMethod);
        }

        try {
            Signature signGen = Signature.getInstance(signatureAlgoName);
            signGen.initSign(privateKey);
            signGen.update(dataToSignBytes);

            signature = signGen.sign();
            signatureAndHashAlgos = signatureAlgoName;
        } catch (SignatureException | InvalidKeyException e) {
            throw new IllegalArgumentException("Signature generation failed", e);
        } catch (NoSuchAlgorithmException e) {
            throw new ProviderException(
                    "Security Provider does not support "
                            + KEY_ALGO_NAME
                            + " or "
                            + signatureAlgoName);
        }
    }

    private static int getAuthMethod(Set<Short> genericSignAuthAlgos) {
        if (genericSignAuthAlgos.isEmpty()) {
            return AUTH_METHOD_RSA_DIGITAL_SIGN;
        }
        return AUTH_METHOD_GENERIC_DIGITAL_SIGN;
    }

    @VisibleForTesting
    static String selectGenericSignAuthAlgo(Set<Short> genericSignAuthAlgos) {
        List<Short> algoList = new ArrayList<>(genericSignAuthAlgos);
        Collections.sort(algoList);

        // NOTE: For all the currently supported algorithms, the larger the algorithm code, the
        // stronger it is. This conclusion might not be true anymore when new algorithms are added
        // and at that time this method will need to be updated.
        short strongestAlgo = algoList.get(algoList.size() - 1);
        return SIGNATURE_ALGO_TYPE_TO_NAME.get(strongestAlgo);
    }

    private byte[] javaStandardSignAlgoNameToAsn1Bytes(String javaSignatureAndHashAlgo) {
        switch (javaSignatureAndHashAlgo) {
            case SIGNATURE_ALGO_RSA_SHA1:
                return PKI_ALGO_ID_DER_BYTES_RSA_SHA1;
            case SIGNATURE_ALGO_RSA_SHA2_256:
                return PKI_ALGO_ID_DER_BYTES_RSA_SHA2_256;
            case SIGNATURE_ALGO_RSA_SHA2_384:
                return PKI_ALGO_ID_DER_BYTES_RSA_SHA2_384;
            case SIGNATURE_ALGO_RSA_SHA2_512:
                return PKI_ALGO_ID_DER_BYTES_RSA_SHA2_512;
            default:
                throw new IllegalArgumentException("Impossible! We used an unsupported algo");
        }
    }

    private String bytesToJavaStandardSignAlgoName(byte[] signAlgoBytes)
            throws AuthenticationFailedException {
        if (Arrays.equals(PKI_ALGO_ID_DER_BYTES_RSA_SHA1, signAlgoBytes)) {
            return SIGNATURE_ALGO_RSA_SHA1;
        } else if (Arrays.equals(PKI_ALGO_ID_DER_BYTES_RSA_SHA2_256, signAlgoBytes)) {
            return SIGNATURE_ALGO_RSA_SHA2_256;
        } else if (Arrays.equals(PKI_ALGO_ID_DER_BYTES_RSA_SHA2_384, signAlgoBytes)) {
            return SIGNATURE_ALGO_RSA_SHA2_384;
        } else if (Arrays.equals(PKI_ALGO_ID_DER_BYTES_RSA_SHA2_512, signAlgoBytes)) {
            return SIGNATURE_ALGO_RSA_SHA2_512;
        } else {
            throw new AuthenticationFailedException(
                    "Unrecognized ASN.1 objects for Signature algorithm and Hash");
        }
    }

    /**
     * Verify received signature in an inbound IKE packet.
     *
     * <p>Since IKE library is always a client, inbound IkeAuthDigitalSignPayload always signs IKE
     * responder's SignedOctets, which is concatenation of the IKE_INIT response message, the Nonce
     * of IKE initiator and the signed ID-Responder payload body.
     *
     * @param certificate received end certificate to verify the signature.
     * @param ikeInitBytes IKE_INIT response for calculating IKE responder's SignedOctets.
     * @param nonce nonce of IKE initiator for calculating IKE responder's SignedOctets.
     * @param idPayloadBodyBytes ID-Responder payload body for calculating IKE responder's
     *     SignedOctets.
     * @param ikePrf the negotiated PRF.
     * @param prfKeyBytes the negotiated PRF responder key.
     * @throws AuthenticationFailedException if received signature verification failed.
     */
    public void verifyInboundSignature(
            X509Certificate certificate,
            byte[] ikeInitBytes,
            byte[] nonce,
            byte[] idPayloadBodyBytes,
            IkeMacPrf ikePrf,
            byte[] prfKeyBytes)
            throws AuthenticationFailedException {
        byte[] dataToSignBytes =
                getSignedOctets(ikeInitBytes, nonce, idPayloadBodyBytes, ikePrf, prfKeyBytes);

        try {
            Signature signValidator = Signature.getInstance(signatureAndHashAlgos);
            signValidator.initVerify(certificate);
            signValidator.update(dataToSignBytes);

            if (!signValidator.verify(signature)) {
                throw new AuthenticationFailedException("Signature verification failed.");
            }
        } catch (SignatureException | InvalidKeyException e) {
            throw new AuthenticationFailedException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new ProviderException(
                    "Security Provider does not support " + signatureAndHashAlgos);
        }
    }

    @Override
    protected void encodeAuthDataToByteBuffer(ByteBuffer byteBuffer) {
        if (authMethod == AUTH_METHOD_GENERIC_DIGITAL_SIGN) {
            byteBuffer.put(SIGNATURE_ALGO_ASN1_BYTES_LEN);
            byteBuffer.put(javaStandardSignAlgoNameToAsn1Bytes(signatureAndHashAlgos));
        }
        byteBuffer.put(signature);
    }

    @Override
    protected int getAuthDataLength() {
        if (authMethod == AUTH_METHOD_GENERIC_DIGITAL_SIGN) {
            return SIGNATURE_ALGO_ASN1_BYTES_LEN_LEN
                    + SIGNATURE_ALGO_ASN1_BYTES_LEN
                    + signature.length;
        }
        return signature.length;
    }

    @Override
    public String getTypeString() {
        switch (authMethod) {
            case AUTH_METHOD_RSA_DIGITAL_SIGN:
                return "Auth(RSA Digital Sign)";
            case AUTH_METHOD_GENERIC_DIGITAL_SIGN:
                return "Auth(Generic Digital Sign)";
            default:
                throw new IllegalArgumentException("Unrecognized authentication method.");
        }
    }

    /**
     * Gets the Signature Hash Algorithsm from the specified IkeNotifyPayload.
     *
     * @param notifyPayload IkeNotifyPayload to read serialized Signature Hash Algorithms from. The
     *     payload type must be SIGNATURE_HASH_ALGORITHMS.
     * @return Set<Short> the Signature Hash Algorithms included in the notifyPayload.
     * @throws InvalidSyntaxException if the included Signature Hash Algorithms are not serialized
     *     correctly
     */
    @NonNull
    public static Set<Short> getSignatureHashAlgorithmsFromIkeNotifyPayload(
            IkeNotifyPayload notifyPayload) throws InvalidSyntaxException {
        if (notifyPayload.notifyType != IkeNotifyPayload.NOTIFY_TYPE_SIGNATURE_HASH_ALGORITHMS) {
            throw new IllegalArgumentException(
                    "Notify payload type must be SIGNATURE_HASH_ALGORITHMS");
        }

        // Hash Algorithm Identifiers are encoded as 16-bit values with no padding (RFC 7427#4)
        int dataLen = notifyPayload.notifyData.length;
        if (dataLen % 2 != 0) {
            throw new InvalidSyntaxException(
                    "Received notify(SIGNATURE_HASH_ALGORITHMS) with invalid notify data");
        }

        Set<Short> hashAlgos = new ArraySet<>();
        ByteBuffer serializedHashAlgos = ByteBuffer.wrap(notifyPayload.notifyData);
        while (serializedHashAlgos.hasRemaining()) {
            short hashAlgo = serializedHashAlgos.getShort();
            if (!SIGNATURE_ALGO_TYPE_TO_NAME.containsKey(hashAlgo) || !hashAlgos.add(hashAlgo)) {
                getIkeLog().w(TAG, "Unexpected or repeated Signature Hash Algorithm: " + hashAlgo);
            }
        }

        return hashAlgos;
    }
}

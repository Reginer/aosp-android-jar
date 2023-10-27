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
import android.annotation.Nullable;
import android.net.ipsec.ike.exceptions.AuthenticationFailedException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * IkeCertPayload is an abstract class that represents the common information for all Certificate
 * Payload carrying different types of certifciate-related data and static methods related to
 * certificate validation.
 *
 * <p>Certificate Payload is only sent in IKE_AUTH exchange.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.6">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public abstract class IkeCertPayload extends IkePayload {
    // Length of certificate encoding type field in octets.
    protected static final int CERT_ENCODING_LEN = 1;

    private static final String KEY_STORE_TYPE_PKCS12 = "PKCS12";
    private static final String CERT_PATH_ALGO_PKIX = "PKIX";
    private static final String CERT_AUTH_TYPE_RSA = "RSA";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        CERTIFICATE_ENCODING_X509_CERT_SIGNATURE,
        CERTIFICATE_ENCODING_CRL,
        CERTIFICATE_ENCODING_X509_CERT_HASH_URL,
    })
    public @interface CertificateEncoding {}

    public static final int CERTIFICATE_ENCODING_X509_CERT_SIGNATURE = 4;
    public static final int CERTIFICATE_ENCODING_CRL = 7;
    public static final int CERTIFICATE_ENCODING_X509_CERT_HASH_URL = 12;

    @CertificateEncoding public final int certEncodingType;

    protected IkeCertPayload(@CertificateEncoding int encodingType) {
        this(false /*critical*/, encodingType);
    }

    protected IkeCertPayload(boolean critical, @CertificateEncoding int encodingType) {
        super(PAYLOAD_TYPE_CERT, critical);
        certEncodingType = encodingType;
    }

    protected static IkeCertPayload getIkeCertPayload(boolean critical, byte[] payloadBody)
            throws IkeProtocolException {
        ByteBuffer inputBuffer = ByteBuffer.wrap(payloadBody);

        int certEncodingType = Byte.toUnsignedInt(inputBuffer.get());
        byte[] certData = new byte[payloadBody.length - CERT_ENCODING_LEN];
        inputBuffer.get(certData);
        switch (certEncodingType) {
            case CERTIFICATE_ENCODING_X509_CERT_SIGNATURE:
                return new IkeCertX509CertPayload(critical, certData);
                // TODO: Support decoding CRL and "Hash and URL".
            case CERTIFICATE_ENCODING_CRL:
                throw new AuthenticationFailedException(
                        "CERTIFICATE_ENCODING_CRL decoding is unsupported.");
            case CERTIFICATE_ENCODING_X509_CERT_HASH_URL:
                throw new AuthenticationFailedException(
                        "CERTIFICATE_ENCODING_X509_CERT_HASH_URL decoding is unsupported");
            default:
                throw new AuthenticationFailedException("Unrecognized certificate encoding type.");
        }
    }

    /**
     * Validate an end certificate against the received chain and trust anchors.
     *
     * <p>Validation is done by checking if there is a valid certificate path from end certificate
     * to provided trust anchors.
     *
     * <p>TrustManager implementation used in this method MUST conforms RFC 4158 and RFC 5280. As
     * indicated in RFC 4158, Key Identifiers(KIDs) are not required to match during certification
     * path validation and cannot be used to eliminate certificates.
     *
     * <p>Validation will fail if any certficate in the certificate chain is using RSA public key
     * whose RSA modulus is smaller than 1024 bits.
     *
     * @param endCert the end certificate that will be used to verify AUTH payload
     * @param certList all the received certificates (include the end certificate)
     * @param crlList the certificate revocation lists
     * @param trustAnchorSet the certificate authority set to validate the end certificate
     * @throws AuthenticationFailedException if there is no valid certificate path
     */
    public static void validateCertificates(
            X509Certificate endCert,
            List<X509Certificate> certList,
            @Nullable List<X509CRL> crlList,
            Set<TrustAnchor> trustAnchorSet)
            throws AuthenticationFailedException {
        try {
            // TODO: b/122676944 Support CRL checking

            // By default, use system-trusted CAs
            KeyStore keyStore = null;

            // But if a specific trust anchor is specified, use that instead
            if (trustAnchorSet != null && !trustAnchorSet.isEmpty()) {
                keyStore = KeyStore.getInstance(KEY_STORE_TYPE_PKCS12);
                keyStore.load(null);
                for (TrustAnchor t : trustAnchorSet) {
                    X509Certificate trustedCert = t.getTrustedCert();
                    String alias =
                            trustedCert.getSubjectX500Principal().getName()
                                    + trustedCert.hashCode();
                    keyStore.setCertificateEntry(alias, trustedCert);
                }
            }

            // Build X509TrustManager with all keystore
            TrustManagerFactory tmFactory =
                    TrustManagerFactory.getInstance(
                            CERT_PATH_ALGO_PKIX, IkeMessage.getTrustManagerProvider());
            tmFactory.init(keyStore);

            X509TrustManager trustManager = null;
            for (TrustManager tm : tmFactory.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    trustManager = (X509TrustManager) tm;
                }
            }
            if (trustManager == null) {
                throw new ProviderException(
                        "X509TrustManager is not supported by "
                                + IkeMessage.getTrustManagerProvider().getName());
            }

            // Build and validate certificate path
            trustManager.checkServerTrusted(
                    certList.toArray(new X509Certificate[certList.size()]), CERT_AUTH_TYPE_RSA);
        } catch (NoSuchAlgorithmException e) {
            throw new ProviderException("Algorithm is not supported by the provider", e);
        } catch (IOException | KeyStoreException e) {
            throw new IllegalStateException(e);
        } catch (CertificateException e) {
            throw new AuthenticationFailedException(e);
        }
    }
}

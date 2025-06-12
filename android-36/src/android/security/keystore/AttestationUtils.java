/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.security.keystore;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.content.Context;
import android.security.keymaster.KeymasterCertificateChain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

/**
 * Utilities for attesting the device's hardware identifiers.
 *
 * @hide
 */
@SystemApi
public abstract class AttestationUtils {
    private AttestationUtils() {
    }

    /**
     * Specifies that the device should attest its serial number. For use with
     * {@link #attestDeviceIds}.
     *
     * @see #attestDeviceIds
     */
    public static final int ID_TYPE_SERIAL = 1;

    /**
     * Specifies that the device should attest its IMEIs. For use with {@link #attestDeviceIds}.
     *
     * @see #attestDeviceIds
     */
    public static final int ID_TYPE_IMEI = 2;

    /**
     * Specifies that the device should attest its MEIDs. For use with {@link #attestDeviceIds}.
     *
     * @see #attestDeviceIds
     */
    public static final int ID_TYPE_MEID = 3;

    /**
     * Specifies that the device should sign the attestation record using its device-unique
     * attestation certificate. For use with {@link #attestDeviceIds}.
     *
     * @see #attestDeviceIds
     */
    public static final int USE_INDIVIDUAL_ATTESTATION = 4;

    /**
     * Creates an array of X509Certificates from the provided KeymasterCertificateChain.
     *
     * @hide Only called by the DevicePolicyManager.
     */
    @NonNull public static X509Certificate[] parseCertificateChain(
            final KeymasterCertificateChain kmChain) throws
            KeyAttestationException {
        // Extract certificate chain.
        final Collection<byte[]> rawChain = kmChain.getCertificates();
        if (rawChain.size() < 2) {
            throw new KeyAttestationException("Attestation certificate chain contained "
                    + rawChain.size() + " entries. At least two are required.");
        }
        final ByteArrayOutputStream concatenatedRawChain = new ByteArrayOutputStream();
        try {
            for (final byte[] cert : rawChain) {
                concatenatedRawChain.write(cert);
            }
            return CertificateFactory.getInstance("X.509").generateCertificates(
                    new ByteArrayInputStream(concatenatedRawChain.toByteArray()))
                            .toArray(new X509Certificate[0]);
        } catch (Exception e) {
            throw new KeyAttestationException("Unable to construct certificate chain", e);
        }
    }

    /**
     * Performs attestation of the device's identifiers. This method returns a certificate chain
     * whose first element contains the requested device identifiers in an extension. The device's
     * manufacturer, model, brand, device and product are always also included in the attestation.
     * If the device supports attestation in secure hardware, the chain will be rooted at a
     * trustworthy CA key. Otherwise, the chain will be rooted at an untrusted certificate. See
     * <a href="https://developer.android.com/training/articles/security-key-attestation.html">
     * Key Attestation</a> for the format of the certificate extension.
     * <p>
     * Attestation will only be successful when all of the following are true:
     * 1) The device has been set up to support device identifier attestation at the factory.
     * 2) The user has not permanently disabled device identifier attestation.
     * 3) You have permission to access the device identifiers you are requesting attestation for.
     * <p>
     * For privacy reasons, you cannot distinguish between (1) and (2). If attestation is
     * unsuccessful, the device may not support it in general or the user may have permanently
     * disabled it.
     *
     * @param context the context to use for retrieving device identifiers.
     * @param idTypes the types of device identifiers to attest.
     * @param attestationChallenge a blob to include in the certificate alongside the device
     * identifiers.
     *
     * @return a certificate chain containing the requested device identifiers in the first element
     *
     * @exception SecurityException if you are not permitted to obtain an attestation of the
     * device's identifiers.
     * @exception DeviceIdAttestationException if the attestation operation fails.
     */
    @RequiresPermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
    @NonNull public static X509Certificate[] attestDeviceIds(Context context,
            @NonNull int[] idTypes, @NonNull byte[] attestationChallenge) throws
            DeviceIdAttestationException {
        if (attestationChallenge == null) {
            throw new NullPointerException("Missing attestation challenge");
        }
        if (idTypes == null) {
            throw new NullPointerException("Missing id types");
        }

        String keystoreAlias = generateRandomAlias();
        KeyGenParameterSpec.Builder builder =
                new KeyGenParameterSpec.Builder(keystoreAlias, KeyProperties.PURPOSE_SIGN)
                        .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setAttestationChallenge(attestationChallenge);

        if (idTypes != null) {
            builder.setAttestationIds(idTypes);
            builder.setDevicePropertiesAttestationIncluded(true);
        }

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            keyPairGenerator.initialize(builder.build());
            keyPairGenerator.generateKeyPair();

            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            Certificate[] certs = keyStore.getCertificateChain(keystoreAlias);
            X509Certificate[] certificateChain =
                Arrays.copyOf(certs, certs.length, X509Certificate[].class);

            keyStore.deleteEntry(keystoreAlias);

            return certificateChain;
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            // If a DeviceIdAttestationException was previously wrapped with some other type,
            // let's throw the original exception instead of wrapping it yet again.
            if (e.getCause() instanceof DeviceIdAttestationException) {
                throw (DeviceIdAttestationException) e.getCause();
            }
            // Illegal argument errors are wrapped up by a ProviderException. Catch those so that
            // we can unwrap them into a more meaningful exception type for the caller.
            if (e instanceof ProviderException
                    && e.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e.getCause();
            }
            throw new DeviceIdAttestationException("Unable to perform attestation", e);
        }
    }

    private static String generateRandomAlias() {
        Random random = new SecureRandom();
        StringBuilder builder = new StringBuilder();
        // Pick random uppercase letters, A-Z.  20 of them gives us ~94 bits of entropy, which
        // should prevent any conflicts with app-selected aliases, even for very unlucky users.
        for (int i = 0; i < 20; ++i) {
            builder.append(random.nextInt(26) + 'A');
        }
        return builder.toString();
    }

    /**
     * Returns true if the attestation chain provided is a valid key attestation chain.
     * @hide
     */
    public static boolean isChainValid(KeymasterCertificateChain chain) {
        return chain != null && chain.getCertificates().size() >= 2;
    }
}

/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.crypto.hpke;

import android.annotation.FlaggedApi;

import libcore.util.NonNull;
import libcore.util.Nullable;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;

/**
 * Provides access to implementations of HPKE hybrid cryptography as per RFC 9180.
 * <p>
 * Provider and HPKE suite selection are done via the {@code getInstance}
 * methods, and then instances of senders and receivers can be created using
 * {@code newSender} or {newReceiver}.  Each sender and receiver is independent, i.e. does
 * not share any encapsulated state with other senders or receivers created via this
 * {@code Hpke}.
 * <p>
 * HPKE suites are composed of a key encapsulation mechanism (KEM), a key derivation
 * function (KDF) and an authenticated cipher algorithm (AEAD) as defined in
 * RFC 9180 section 7. {@link java.security.spec.NamedParameterSpec NamedParameterSpecs} for
 * these can be found in {@link KemParameterSpec}, {@link KdfParameterSpec} and
 * {@link AeadParameterSpec}.  These can be composed into a full HPKE suite name used to
 * request a particular implementation using
 * {@link Hpke#getSuiteName(KemParameterSpec, KdfParameterSpec, AeadParameterSpec)}.
 *
 * @see KemParameterSpec
 * @see KdfParameterSpec
 * @see AeadParameterSpec
 */
@SuppressWarnings("NewApi") // Public HPKE classes are always all present together.
@FlaggedApi(com.android.libcore.Flags.FLAG_HPKE_PUBLIC_API)
public class Hpke {
    private static final String SERVICE_NAME = "ConscryptHpke";
    static final byte[] DEFAULT_PSK = new byte[0];
    static final byte[] DEFAULT_PSK_ID = DEFAULT_PSK;
    private final Provider provider;
    private final Provider.Service service;

    private Hpke(@NonNull String suiteName, @NonNull Provider provider)
            throws NoSuchAlgorithmException {
        this.provider = provider;
        service = getService(provider, suiteName);
        if (service == null) {
            throw new NoSuchAlgorithmException("No such HPKE suite: " + suiteName);
        }
    }

    private static @NonNull Provider findFirstProvider(@NonNull String suiteName)
            throws NoSuchAlgorithmException {
        for (Provider provider : Security.getProviders()) {
            if (getService(provider, suiteName) != null) {
                return provider;
            }
        }
        throw new NoSuchAlgorithmException("No Provider found for HPKE suite: " + suiteName);
    }

    @SuppressWarnings("InlinedApi") // For SERVICE_NAME field which belongs to this class
    private static Provider.Service getService(Provider provider, String suiteName)
            throws NoSuchAlgorithmException {
        if (suiteName == null || suiteName.isEmpty()) {
            throw new NoSuchAlgorithmException();
        }
        return provider.getService(SERVICE_NAME, suiteName);
    }

    @NonNull HpkeSpi findSpi() {
        Object instance;
        try {
            instance = service.newInstance(null);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Initialisation error", e);
        }
        if (instance instanceof HpkeSpi) {
            return (HpkeSpi) instance;
        } else {
            DuckTypedHpkeSpi spi = DuckTypedHpkeSpi.newInstance(instance);
            if (spi != null) {
                return spi;
            }
        }
        throw new IllegalStateException(
                String.format("Provider %s is incorrectly configured", provider.getName()));
    }

    /**
     * Returns the {@link Provider} being used by this Hpke instance.
     * <p>
     *
     * @return the Provider
     */
    public @NonNull Provider getProvider() {
        return provider;
    }

    /**
     * Returns an Hpke instance configured for the requested HPKE suite, using the
     * highest priority {@link Provider} which implements it.
     * <p>
     * Use {@link Hpke#getSuiteName(KemParameterSpec, KdfParameterSpec, AeadParameterSpec)} for
     * generating HPKE suite names from {@link java.security.spec.NamedParameterSpec
     * NamedParameterSpecs}
     *
     * @param suiteName the HPKE suite to use
     * @return an Hpke instance configured for the requested suite
     * @throws NoSuchAlgorithmException if no Providers can be found for the requested suite
     */
    public static @NonNull Hpke getInstance(@NonNull String suiteName)
            throws NoSuchAlgorithmException {
        return new Hpke(suiteName, findFirstProvider(suiteName));
    }

    /**
     * Returns an Hpke instance configured for the requested HPKE suite, using the
     * requested {@link Provider} by name.
     *
     * @param suiteName    the HPKE suite to use
     * @param providerName the name of the provider to use
     * @return an Hpke instance configured for the requested suite and Provider
     * @throws NoSuchAlgorithmException if the named Provider does not implement this suite
     * @throws NoSuchProviderException  if no Provider with the requested name can be found
     * @throws IllegalArgumentException if providerName is null or empty
     */
    public static @NonNull Hpke getInstance(@NonNull String suiteName, @NonNull String providerName)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (providerName == null || providerName.isEmpty()) {
            throw new IllegalArgumentException("Invalid Provider Name");
        }
        Provider provider = Security.getProvider(providerName);
        if (provider == null) {
            throw new NoSuchProviderException();
        }
        return new Hpke(suiteName, provider);
    }

    /**
     * Returns an Hpke instance configured for the requested HPKE suite, using the
     * requested {@link Provider}.
     *
     * @param suiteName the HPKE suite to use
     * @param provider  the provider to use
     * @return an Hpke instance configured for the requested suite and Provider
     * @throws NoSuchAlgorithmException if the Provider does not implement this suite
     * @throws IllegalArgumentException if provider is null
     */
    public static @NonNull Hpke getInstance(@NonNull String suiteName, @NonNull Provider provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null) {
            throw new IllegalArgumentException("Null Provider");
        }
        return new Hpke(suiteName, provider);
    }

    /**
     * Generates a full HPKE suite name from the named parameter specifications of its components,
     * which have names reflecting their usage in RFC 9180.
     * <p>
     * HPKE suites are composed of a key encapsulation mechanism (KEM), a key derivation
     * function (KDF) and an authenticated cipher algorithm (AEAD) as defined in
     * RFC 9180 section 7. {@link java.security.spec.NamedParameterSpec NamedParameterSpecs} for
     * these can be foundu in {@link KemParameterSpec}, {@link KdfParameterSpec} and
     * {@link AeadParameterSpec}.
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9180.html#section-7">RFC 9180 Section 7</a>
     * @see KemParameterSpec
     * @see KdfParameterSpec
     * @see AeadParameterSpec
     *
     * @param kem  the key encapsulation mechanism to use
     * @param kdf  the key derivation function to use
     * @param aead the AEAD cipher to use
     * @return a fully composed HPKE suite name
     */
    public static @NonNull String getSuiteName(@NonNull KemParameterSpec kem,
            @NonNull KdfParameterSpec kdf, @NonNull AeadParameterSpec aead) {
        return kem.getName() + "/" + kdf.getName() + "/" + aead.getName();
    }

    /**
     * One shot API to seal a single message using BASE mode (no authentication).
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9180.html#name-encryption-and-decryption-2">
     *     Opening and sealing</a>
     * @param recipientKey public key of the recipient
     * @param info         additional application-supplied information, may be null or empty
     * @param plaintext    the message to send
     * @param aad          optional additional authenticated data, may be null or empty
     * @return a Message object containing the encapsulated key, ciphertext and aad
     * @throws InvalidKeyException      if recipientKey is null or an unsupported key format
     */
    public @NonNull Message seal(@NonNull PublicKey recipientKey, @Nullable byte[] info,
            @NonNull byte[] plaintext, @Nullable byte[] aad)
            throws InvalidKeyException {
        Sender.Builder senderBuilder = new Sender.Builder(this, recipientKey);
        if (info != null) {
            senderBuilder.setApplicationInfo(info);
        }
        Sender sender = senderBuilder.build();
        byte[] encapsulated = sender.getEncapsulated();
        byte[] ciphertext = sender.seal(plaintext, aad);
        return new Message(encapsulated, ciphertext);
    }

    /**
     * One shot API to open a single ciphertext using BASE mode (no authentication).
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9180.html#name-encryption-and-decryption-2">
     *     Opening and sealing</a>
     * @param recipientKey private key of the recipient
     * @param info         application-supplied information, may be null or empty
     * @param message      the Message to open
     * @param aad          optional additional authenticated data, may be null or empty
     * @return decrypted plaintext
     * @throws InvalidKeyException      if recipientKey is null or an unsupported key format
     * @throws GeneralSecurityException if the decryption fails
     */
    public @NonNull byte[] open(
            @NonNull PrivateKey recipientKey, @Nullable byte[] info, @NonNull Message message,
            @Nullable byte[] aad)
            throws GeneralSecurityException, InvalidKeyException {
        Recipient.Builder recipientBuilder
                = new Recipient.Builder(this, message.getEncapsulated(), recipientKey);
        if (info != null) {
            recipientBuilder.setApplicationInfo(info);
        }
        return recipientBuilder.build().open(message.getCiphertext(), aad);
    }
}

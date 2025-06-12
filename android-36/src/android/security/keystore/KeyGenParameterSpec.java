/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.app.KeyguardManager;
import android.compat.annotation.UnsupportedAppUsage;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.security.GateKeeper;
import android.text.TextUtils;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.security.auth.x500.X500Principal;

/**
 * {@link AlgorithmParameterSpec} for initializing a {@link KeyPairGenerator} or a
 * {@link KeyGenerator} of the <a href="{@docRoot}training/articles/keystore.html">Android Keystore
 * system</a>. The spec determines authorized uses of the key, such as whether user authentication
 * is required for using the key, what operations are authorized (e.g., signing, but not
 * decryption), with what parameters (e.g., only with a particular padding scheme or digest), and
 * the key's validity start and end dates. Key use authorizations expressed in the spec apply
 * only to secret keys and private keys -- public keys can be used for any supported operations.
 *
 * <p>To generate an asymmetric key pair or a symmetric key, create an instance of this class using
 * the {@link Builder}, initialize a {@code KeyPairGenerator} or a {@code KeyGenerator} of the
 * desired key type (e.g., {@code EC} or {@code AES} -- see
 * {@link KeyProperties}.{@code KEY_ALGORITHM} constants) from the {@code AndroidKeyStore} provider
 * with the {@code KeyGenParameterSpec} instance, and then generate a key or key pair using
 * {@link KeyGenerator#generateKey()} or {@link KeyPairGenerator#generateKeyPair()}.
 *
 * <p>The generated key pair or key will be returned by the generator and also stored in the Android
 * Keystore under the alias specified in this spec. To obtain the secret or private key from the
 * Android Keystore use {@link java.security.KeyStore#getKey(String, char[]) KeyStore.getKey(String, null)}
 * or {@link java.security.KeyStore#getEntry(String, java.security.KeyStore.ProtectionParameter) KeyStore.getEntry(String, null)}.
 * To obtain the public key from the Android Keystore use
 * {@link java.security.KeyStore#getCertificate(String)} and then
 * {@link Certificate#getPublicKey()}.
 *
 * <p>To help obtain algorithm-specific public parameters of key pairs stored in the Android
 * Keystore, generated private keys implement {@link java.security.interfaces.ECKey} or
 * {@link java.security.interfaces.RSAKey} interfaces whereas public keys implement
 * {@link java.security.interfaces.ECPublicKey} or {@link java.security.interfaces.RSAPublicKey}
 * interfaces.
 *
 * <p>For asymmetric key pairs, a X.509 certificate will be also generated and stored in the Android
 * Keystore. This is because the {@link java.security.KeyStore} abstraction does not support storing
 * key pairs without a certificate. The subject, serial number, and validity dates of the
 * certificate can be customized in this spec. The certificate may be replaced at a later time by a
 * certificate signed by a Certificate Authority (CA).
 *
 * <p>NOTE: If attestation is not requested using {@link Builder#setAttestationChallenge(byte[])},
 * generated certificate may be self-signed. If a private key is not authorized to sign the
 * certificate, then the certificate will be created with an invalid signature which will not
 * verify. Such a certificate is still useful because it provides access to the public key. To
 * generate a valid signature for the certificate the key needs to be authorized for all of the
 * following:
 * <ul>
 * <li>{@link KeyProperties#PURPOSE_SIGN},</li>
 * <li>operation without requiring the user to be authenticated (see
 * {@link Builder#setUserAuthenticationRequired(boolean)}),</li>
 * <li>signing/origination at this moment in time (see {@link Builder#setKeyValidityStart(Date)}
 * and {@link Builder#setKeyValidityForOriginationEnd(Date)}),</li>
 * <li>suitable digest,</li>
 * <li>(RSA keys only) padding scheme {@link KeyProperties#SIGNATURE_PADDING_RSA_PKCS1}.</li>
 * </ul>
 *
 * <p>NOTE: The key material of the generated symmetric and private keys is not accessible. The key
 * material of the public keys is accessible.
 *
 * <p>Instances of this class are immutable.
 *
 * <p><h3>Known issues</h3>
 * A known bug in Android 6.0 (API Level 23) causes user authentication-related authorizations to be
 * enforced even for public keys. To work around this issue extract the public key material to use
 * outside of Android Keystore. For example:
 * <pre> {@code
 * PublicKey unrestrictedPublicKey =
 *         KeyFactory.getInstance(publicKey.getAlgorithm()).generatePublic(
 *                 new X509EncodedKeySpec(publicKey.getEncoded()));
 * }</pre>
 *
 * <p><h3>Example: NIST P-256 EC key pair for signing/verification using ECDSA</h3>
 * This example illustrates how to generate a NIST P-256 (aka secp256r1 aka prime256v1) EC key pair
 * in the Android KeyStore system under alias {@code key1} where the private key is authorized to be
 * used only for signing using SHA-256, SHA-384, or SHA-512 digest and only if the user has been
 * authenticated within the last five minutes. The use of the public key is unrestricted (See Known
 * Issues).
 * <pre> {@code
 * KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
 *         KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
 * keyPairGenerator.initialize(
 *         new KeyGenParameterSpec.Builder(
 *                 "key1",
 *                 KeyProperties.PURPOSE_SIGN)
 *                 .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
 *                 .setDigests(KeyProperties.DIGEST_SHA256,
 *                         KeyProperties.DIGEST_SHA384,
 *                         KeyProperties.DIGEST_SHA512)
 *                 // Only permit the private key to be used if the user authenticated
 *                 // within the last five minutes.
 *                 .setUserAuthenticationRequired(true)
 *                 .setUserAuthenticationValidityDurationSeconds(5 * 60)
 *                 .build());
 * KeyPair keyPair = keyPairGenerator.generateKeyPair();
 * Signature signature = Signature.getInstance("SHA256withECDSA");
 * signature.initSign(keyPair.getPrivate());
 * ...
 *
 * // The key pair can also be obtained from the Android Keystore any time as follows:
 * KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
 * keyStore.load(null);
 * PrivateKey privateKey = (PrivateKey) keyStore.getKey("key1", null);
 * PublicKey publicKey = keyStore.getCertificate("key1").getPublicKey();
 * }</pre>
 *
 * <p><h3>Example: RSA key pair for signing/verification using RSA-PSS</h3>
 * This example illustrates how to generate an RSA key pair in the Android KeyStore system under
 * alias {@code key1} authorized to be used only for signing using the RSA-PSS signature padding
 * scheme with SHA-256 or SHA-512 digests. The use of the public key is unrestricted.
 * <pre> {@code
 * KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
 *         KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
 * keyPairGenerator.initialize(
 *         new KeyGenParameterSpec.Builder(
 *                 "key1",
 *                 KeyProperties.PURPOSE_SIGN)
 *                 .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
 *                 .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)
 *                 .build());
 * KeyPair keyPair = keyPairGenerator.generateKeyPair();
 * Signature signature = Signature.getInstance("SHA256withRSA/PSS");
 * signature.initSign(keyPair.getPrivate());
 * ...
 *
 * // The key pair can also be obtained from the Android Keystore any time as follows:
 * KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
 * keyStore.load(null);
 * PrivateKey privateKey = (PrivateKey) keyStore.getKey("key1", null);
 * PublicKey publicKey = keyStore.getCertificate("key1").getPublicKey();
 * }</pre>
 *
 * <p><h3>Example: RSA key pair for encryption/decryption using RSA OAEP</h3>
 * This example illustrates how to generate an RSA key pair in the Android KeyStore system under
 * alias {@code key1} where the private key is authorized to be used only for decryption using RSA
 * OAEP encryption padding scheme with SHA-256 or SHA-512 digests. The use of the public key is
 * unrestricted.
 * <pre> {@code
 * KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
 *         KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
 * keyPairGenerator.initialize(
 *         new KeyGenParameterSpec.Builder(
 *                 "key1",
 *                 KeyProperties.PURPOSE_DECRYPT)
 *                 .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
 *                 .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
 *                 .build());
 * KeyPair keyPair = keyPairGenerator.generateKeyPair();
 * Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
 * cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
 * ...
 *
 * // The key pair can also be obtained from the Android Keystore any time as follows:
 * KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
 * keyStore.load(null);
 * PrivateKey privateKey = (PrivateKey) keyStore.getKey("key1", null);
 * PublicKey publicKey = keyStore.getCertificate("key1").getPublicKey();
 * }</pre>
 *
 * <p><h3>Example: AES key for encryption/decryption in GCM mode</h3>
 * The following example illustrates how to generate an AES key in the Android KeyStore system under
 * alias {@code key2} authorized to be used only for encryption/decryption in GCM mode with no
 * padding.
 * <pre> {@code
 * KeyGenerator keyGenerator = KeyGenerator.getInstance(
 *         KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
 * keyGenerator.init(
 *         new KeyGenParameterSpec.Builder("key2",
 *                 KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
 *                 .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
 *                 .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
 *                 .build());
 * SecretKey key = keyGenerator.generateKey();
 *
 * Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
 * cipher.init(Cipher.ENCRYPT_MODE, key);
 * ...
 *
 * // The key can also be obtained from the Android Keystore any time as follows:
 * KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
 * keyStore.load(null);
 * key = (SecretKey) keyStore.getKey("key2", null);
 * }</pre>
 *
 * <p><h3>Example: HMAC key for generating a MAC using SHA-256</h3>
 * This example illustrates how to generate an HMAC key in the Android KeyStore system under alias
 * {@code key2} authorized to be used only for generating an HMAC using SHA-256.
 * <pre> {@code
 * KeyGenerator keyGenerator = KeyGenerator.getInstance(
 *         KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore");
 * keyGenerator.init(
 *         new KeyGenParameterSpec.Builder("key2", KeyProperties.PURPOSE_SIGN).build());
 * SecretKey key = keyGenerator.generateKey();
 * Mac mac = Mac.getInstance("HmacSHA256");
 * mac.init(key);
 * ...
 *
 * // The key can also be obtained from the Android Keystore any time as follows:
 * KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
 * keyStore.load(null);
 * key = (SecretKey) keyStore.getKey("key2", null);
 * }</pre>
 *
 * <p><h3 id="example:ecdh">Example: EC key for ECDH key agreement</h3>
 * This example illustrates how to generate an elliptic curve key pair, used to establish a shared
 * secret with another party using ECDH key agreement.
 * <pre> {@code
 * KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
 *         KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
 * keyPairGenerator.initialize(
 *         new KeyGenParameterSpec.Builder(
 *             "eckeypair",
 *             KeyProperties.PURPOSE_AGREE_KEY)
 *             .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
 *             .build());
 * KeyPair myKeyPair = keyPairGenerator.generateKeyPair();
 *
 * // Exchange public keys with server. A new ephemeral key MUST be used for every message.
 * PublicKey serverEphemeralPublicKey; // Ephemeral key received from server.
 *
 * // Create a shared secret based on our private key and the other party's public key.
 * KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", "AndroidKeyStore");
 * keyAgreement.init(myKeyPair.getPrivate());
 * keyAgreement.doPhase(serverEphemeralPublicKey, true);
 * byte[] sharedSecret = keyAgreement.generateSecret();
 *
 * // sharedSecret cannot safely be used as a key yet. We must run it through a key derivation
 * // function with some other data: "salt" and "info". Salt is an optional random value,
 * // omitted in this example. It's good practice to include both public keys and any other
 * // key negotiation data in info. Here we use the public keys and a label that indicates
 * // messages encrypted with this key are coming from the server.
 * byte[] salt = {};
 * ByteArrayOutputStream info = new ByteArrayOutputStream();
 * info.write("ECDH secp256r1 AES-256-GCM-SIV\0".getBytes(StandardCharsets.UTF_8));
 * info.write(myKeyPair.getPublic().getEncoded());
 * info.write(serverEphemeralPublicKey.getEncoded());
 *
 * // This example uses the Tink library and the HKDF key derivation function.
 * AesGcmSiv key = new AesGcmSiv(Hkdf.computeHkdf(
 *         "HMACSHA256", sharedSecret, salt, info.toByteArray(), 32));
 * byte[] associatedData = {};
 * return key.decrypt(ciphertext, associatedData);
 * }</pre>
 */
public final class KeyGenParameterSpec implements AlgorithmParameterSpec, UserAuthArgs {
    private static final X500Principal DEFAULT_ATTESTATION_CERT_SUBJECT =
            new X500Principal("CN=Android Keystore Key");
    private static final X500Principal DEFAULT_SELF_SIGNED_CERT_SUBJECT =
            new X500Principal("CN=Fake");
    private static final BigInteger DEFAULT_CERT_SERIAL_NUMBER = new BigInteger("1");
    private static final Date DEFAULT_CERT_NOT_BEFORE = new Date(0L); // Jan 1 1970
    private static final Date DEFAULT_CERT_NOT_AFTER = new Date(2461449600000L); // Jan 1 2048

    private final String mKeystoreAlias;
    private final @KeyProperties.Namespace int mNamespace;
    private final int mKeySize;
    private final AlgorithmParameterSpec mSpec;
    private final X500Principal mCertificateSubject;
    private final BigInteger mCertificateSerialNumber;
    private final Date mCertificateNotBefore;
    private final Date mCertificateNotAfter;
    private final Date mKeyValidityStart;
    private final Date mKeyValidityForOriginationEnd;
    private final Date mKeyValidityForConsumptionEnd;
    private final @KeyProperties.PurposeEnum int mPurposes;
    private final @KeyProperties.DigestEnum String[] mDigests;
    private final @NonNull @KeyProperties.DigestEnum Set<String> mMgf1Digests;
    private final @KeyProperties.EncryptionPaddingEnum String[] mEncryptionPaddings;
    private final @KeyProperties.SignaturePaddingEnum String[] mSignaturePaddings;
    private final @KeyProperties.BlockModeEnum String[] mBlockModes;
    private final boolean mRandomizedEncryptionRequired;
    private final boolean mUserAuthenticationRequired;
    private final int mUserAuthenticationValidityDurationSeconds;
    private final @KeyProperties.AuthEnum int mUserAuthenticationType;
    private final boolean mUserPresenceRequired;
    private final byte[] mAttestationChallenge;
    private final boolean mDevicePropertiesAttestationIncluded;
    private final int[] mAttestationIds;
    private final boolean mUniqueIdIncluded;
    private final boolean mUserAuthenticationValidWhileOnBody;
    private final boolean mInvalidatedByBiometricEnrollment;
    private final boolean mIsStrongBoxBacked;
    private final boolean mUserConfirmationRequired;
    private final boolean mUnlockedDeviceRequired;
    private final boolean mCriticalToDeviceEncryption;
    private final int mMaxUsageCount;
    private final String mAttestKeyAlias;
    private final long mBoundToSecureUserId;

    /*
     * ***NOTE***: All new fields MUST also be added to the following:
     * ParcelableKeyGenParameterSpec class.
     * The KeyGenParameterSpec.Builder constructor that takes a KeyGenParameterSpec
     */

    /**
     * @hide should be built with Builder
     */
    public KeyGenParameterSpec(
            String keyStoreAlias,
            @KeyProperties.Namespace int namespace,
            int keySize,
            AlgorithmParameterSpec spec,
            X500Principal certificateSubject,
            BigInteger certificateSerialNumber,
            Date certificateNotBefore,
            Date certificateNotAfter,
            Date keyValidityStart,
            Date keyValidityForOriginationEnd,
            Date keyValidityForConsumptionEnd,
            @KeyProperties.PurposeEnum int purposes,
            @KeyProperties.DigestEnum String[] digests,
            @KeyProperties.DigestEnum Set<String> mgf1Digests,
            @KeyProperties.EncryptionPaddingEnum String[] encryptionPaddings,
            @KeyProperties.SignaturePaddingEnum String[] signaturePaddings,
            @KeyProperties.BlockModeEnum String[] blockModes,
            boolean randomizedEncryptionRequired,
            boolean userAuthenticationRequired,
            int userAuthenticationValidityDurationSeconds,
            @KeyProperties.AuthEnum int userAuthenticationType,
            boolean userPresenceRequired,
            byte[] attestationChallenge,
            boolean devicePropertiesAttestationIncluded,
            @NonNull int[] attestationIds,
            boolean uniqueIdIncluded,
            boolean userAuthenticationValidWhileOnBody,
            boolean invalidatedByBiometricEnrollment,
            boolean isStrongBoxBacked,
            boolean userConfirmationRequired,
            boolean unlockedDeviceRequired,
            boolean criticalToDeviceEncryption,
            int maxUsageCount,
            String attestKeyAlias,
            long boundToSecureUserId) {
        if (TextUtils.isEmpty(keyStoreAlias)) {
            throw new IllegalArgumentException("keyStoreAlias must not be empty");
        }

        if (certificateSubject == null) {
            if (attestationChallenge == null) {
                certificateSubject = DEFAULT_SELF_SIGNED_CERT_SUBJECT;
            } else {
                certificateSubject = DEFAULT_ATTESTATION_CERT_SUBJECT;
            }
        }
        if (certificateNotBefore == null) {
            certificateNotBefore = DEFAULT_CERT_NOT_BEFORE;
        }
        if (certificateNotAfter == null) {
            certificateNotAfter = DEFAULT_CERT_NOT_AFTER;
        }
        if (certificateSerialNumber == null) {
            certificateSerialNumber = DEFAULT_CERT_SERIAL_NUMBER;
        }

        if (certificateNotAfter.before(certificateNotBefore)) {
            throw new IllegalArgumentException("certificateNotAfter < certificateNotBefore");
        }

        mKeystoreAlias = keyStoreAlias;
        mNamespace = namespace;
        mKeySize = keySize;
        mSpec = spec;
        mCertificateSubject = certificateSubject;
        mCertificateSerialNumber = certificateSerialNumber;
        mCertificateNotBefore = Utils.cloneIfNotNull(certificateNotBefore);
        mCertificateNotAfter = Utils.cloneIfNotNull(certificateNotAfter);
        mKeyValidityStart = Utils.cloneIfNotNull(keyValidityStart);
        mKeyValidityForOriginationEnd = Utils.cloneIfNotNull(keyValidityForOriginationEnd);
        mKeyValidityForConsumptionEnd = Utils.cloneIfNotNull(keyValidityForConsumptionEnd);
        mPurposes = purposes;
        mDigests = ArrayUtils.cloneIfNotEmpty(digests);
        // No need to copy the input parameter because the Builder class passes in an immutable
        // collection.
        mMgf1Digests = mgf1Digests != null ? mgf1Digests : Collections.emptySet();
        mEncryptionPaddings =
                ArrayUtils.cloneIfNotEmpty(ArrayUtils.nullToEmpty(encryptionPaddings));
        mSignaturePaddings = ArrayUtils.cloneIfNotEmpty(ArrayUtils.nullToEmpty(signaturePaddings));
        mBlockModes = ArrayUtils.cloneIfNotEmpty(ArrayUtils.nullToEmpty(blockModes));
        mRandomizedEncryptionRequired = randomizedEncryptionRequired;
        mUserAuthenticationRequired = userAuthenticationRequired;
        mUserPresenceRequired = userPresenceRequired;
        mUserAuthenticationValidityDurationSeconds = userAuthenticationValidityDurationSeconds;
        mUserAuthenticationType = userAuthenticationType;
        mAttestationChallenge = Utils.cloneIfNotNull(attestationChallenge);
        mDevicePropertiesAttestationIncluded = devicePropertiesAttestationIncluded;
        mAttestationIds = attestationIds;
        mUniqueIdIncluded = uniqueIdIncluded;
        mUserAuthenticationValidWhileOnBody = userAuthenticationValidWhileOnBody;
        mInvalidatedByBiometricEnrollment = invalidatedByBiometricEnrollment;
        mIsStrongBoxBacked = isStrongBoxBacked;
        mUserConfirmationRequired = userConfirmationRequired;
        mUnlockedDeviceRequired = unlockedDeviceRequired;
        mCriticalToDeviceEncryption = criticalToDeviceEncryption;
        mMaxUsageCount = maxUsageCount;
        mAttestKeyAlias = attestKeyAlias;
        mBoundToSecureUserId = boundToSecureUserId;
    }

    /**
     * Returns the alias that will be used in the {@code java.security.KeyStore}
     * in conjunction with the {@code AndroidKeyStore}.
     */
    @NonNull
    public String getKeystoreAlias() {
        return mKeystoreAlias;
    }

    /**
     * Returns the UID which will own the key. {@code -1} is an alias for the UID of the current
     * process.
     *
     * @deprecated See deprecation message on {@link KeyGenParameterSpec.Builder#setUid(int)}.
     *             Known namespaces will be translated to their legacy UIDs. Unknown
     *             Namespaces will yield {@link IllegalStateException}.
     *
     * @hide
     */
    @UnsupportedAppUsage
    @Deprecated
    public int getUid() {
        try {
            return KeyProperties.namespaceToLegacyUid(mNamespace);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("getUid called on KeyGenParameterSpec with non legacy"
                    + " keystore namespace.", e);
        }
    }

    /**
     * Returns the target namespace for the key.
     * See {@link KeyGenParameterSpec.Builder#setNamespace(int)}.
     *
     * @return The numeric namespace as configured in the keystore2_key_contexts files of Android's
     *         SEPolicy.
     *         See <a href="https://source.android.com/security/keystore#access-control">
     *             Keystore 2.0 access control</a>
     * @hide
     */
    @SystemApi
    public @KeyProperties.Namespace int getNamespace() {
        return mNamespace;
    }

    /**
     * Returns the requested key size. If {@code -1}, the size should be looked up from
     * {@link #getAlgorithmParameterSpec()}, if provided, otherwise an algorithm-specific default
     * size should be used.
     */
    public int getKeySize() {
        return mKeySize;
    }

    /**
     * Returns the key algorithm-specific {@link AlgorithmParameterSpec} that will be used for
     * creation of the key or {@code null} if algorithm-specific defaults should be used.
     */
    @Nullable
    public AlgorithmParameterSpec getAlgorithmParameterSpec() {
        return mSpec;
    }

    /**
     * Returns the subject distinguished name to be used on the X.509 certificate that will be put
     * in the {@link java.security.KeyStore}.
     */
    @NonNull
    public X500Principal getCertificateSubject() {
        return mCertificateSubject;
    }

    /**
     * Returns the serial number to be used on the X.509 certificate that will be put in the
     * {@link java.security.KeyStore}.
     */
    @NonNull
    public BigInteger getCertificateSerialNumber() {
        return mCertificateSerialNumber;
    }

    /**
     * Returns the start date to be used on the X.509 certificate that will be put in the
     * {@link java.security.KeyStore}.
     */
    @NonNull
    public Date getCertificateNotBefore() {
        return Utils.cloneIfNotNull(mCertificateNotBefore);
    }

    /**
     * Returns the end date to be used on the X.509 certificate that will be put in the
     * {@link java.security.KeyStore}.
     */
    @NonNull
    public Date getCertificateNotAfter() {
        return Utils.cloneIfNotNull(mCertificateNotAfter);
    }

    /**
     * Returns the time instant before which the key is not yet valid or {@code null} if not
     * restricted.
     */
    @Nullable
    public Date getKeyValidityStart() {
        return Utils.cloneIfNotNull(mKeyValidityStart);
    }

    /**
     * Returns the time instant after which the key is no longer valid for decryption and
     * verification or {@code null} if not restricted.
     */
    @Nullable
    public Date getKeyValidityForConsumptionEnd() {
        return Utils.cloneIfNotNull(mKeyValidityForConsumptionEnd);
    }

    /**
     * Returns the time instant after which the key is no longer valid for encryption and signing
     * or {@code null} if not restricted.
     */
    @Nullable
    public Date getKeyValidityForOriginationEnd() {
        return Utils.cloneIfNotNull(mKeyValidityForOriginationEnd);
    }

    /**
     * Returns the set of purposes (e.g., encrypt, decrypt, sign) for which the key can be used.
     * Attempts to use the key for any other purpose will be rejected.
     *
     * <p>See {@link KeyProperties}.{@code PURPOSE} flags.
     */
    public @KeyProperties.PurposeEnum int getPurposes() {
        return mPurposes;
    }

    /**
     * Returns the set of digest algorithms (e.g., {@code SHA-256}, {@code SHA-384} with which the
     * key can be used.
     *
     * <p>See {@link KeyProperties}.{@code DIGEST} constants.
     *
     * @throws IllegalStateException if this set has not been specified.
     *
     * @see #isDigestsSpecified()
     */
    @NonNull
    public @KeyProperties.DigestEnum String[] getDigests() {
        if (mDigests == null) {
            throw new IllegalStateException("Digests not specified");
        }
        return ArrayUtils.cloneIfNotEmpty(mDigests);
    }

    /**
     * Returns {@code true} if the set of digest algorithms with which the key can be used has been
     * specified.
     *
     * @see #getDigests()
     */
    @NonNull
    public boolean isDigestsSpecified() {
        return mDigests != null;
    }

    /**
     * Returns the set of digests that can be used by the MGF1 mask generation function
     * (e.g., {@code SHA-256}, {@code SHA-384}) with the key. Useful with the {@code RSA-OAEP}
     * scheme.
     * If not explicitly specified during key generation, the default {@code SHA-1} digest is
     * used and may be specified when using the key.
     *
     * <p>See {@link KeyProperties}.{@code DIGEST} constants.
     *
     * @throws IllegalStateException if this set has not been specified.
     *
     * @see #isMgf1DigestsSpecified()
     */
    @NonNull
    @FlaggedApi(android.security.Flags.FLAG_MGF1_DIGEST_SETTER_V2)
    public @KeyProperties.DigestEnum Set<String> getMgf1Digests() {
        if (mMgf1Digests.isEmpty()) {
            throw new IllegalStateException("Mask generation function (MGF) not specified");
        }
        return new HashSet(mMgf1Digests);
    }

    /**
     * Returns {@code true} if the set of digests for the MGF1 mask generation function,
     * with which the key can be used, has been specified. Useful with the {@code RSA-OAEP} scheme.
     *
     * @see #getMgf1Digests()
     */
    @NonNull
    @FlaggedApi(android.security.Flags.FLAG_MGF1_DIGEST_SETTER_V2)
    public boolean isMgf1DigestsSpecified() {
        return !mMgf1Digests.isEmpty();
    }

    /**
     * Returns the set of padding schemes (e.g., {@code PKCS7Padding}, {@code OEAPPadding},
     * {@code PKCS1Padding}, {@code NoPadding}) with which the key can be used when
     * encrypting/decrypting. Attempts to use the key with any other padding scheme will be
     * rejected.
     *
     * <p>See {@link KeyProperties}.{@code ENCRYPTION_PADDING} constants.
     */
    @NonNull
    public @KeyProperties.EncryptionPaddingEnum String[] getEncryptionPaddings() {
        return ArrayUtils.cloneIfNotEmpty(mEncryptionPaddings);
    }

    /**
     * Gets the set of padding schemes (e.g., {@code PSS}, {@code PKCS#1}) with which the key
     * can be used when signing/verifying. Attempts to use the key with any other padding scheme
     * will be rejected.
     *
     * <p>See {@link KeyProperties}.{@code SIGNATURE_PADDING} constants.
     */
    @NonNull
    public @KeyProperties.SignaturePaddingEnum String[] getSignaturePaddings() {
        return ArrayUtils.cloneIfNotEmpty(mSignaturePaddings);
    }

    /**
     * Gets the set of block modes (e.g., {@code GCM}, {@code CBC}) with which the key can be used
     * when encrypting/decrypting. Attempts to use the key with any other block modes will be
     * rejected.
     *
     * <p>See {@link KeyProperties}.{@code BLOCK_MODE} constants.
     */
    @NonNull
    public @KeyProperties.BlockModeEnum String[] getBlockModes() {
        return ArrayUtils.cloneIfNotEmpty(mBlockModes);
    }

    /**
     * Returns {@code true} if encryption using this key must be sufficiently randomized to produce
     * different ciphertexts for the same plaintext every time. The formal cryptographic property
     * being required is <em>indistinguishability under chosen-plaintext attack ({@code
     * IND-CPA})</em>. This property is important because it mitigates several classes of
     * weaknesses due to which ciphertext may leak information about plaintext.  For example, if a
     * given plaintext always produces the same ciphertext, an attacker may see the repeated
     * ciphertexts and be able to deduce something about the plaintext.
     */
    public boolean isRandomizedEncryptionRequired() {
        return mRandomizedEncryptionRequired;
    }

    /**
     * Returns {@code true} if the key is authorized to be used only if the user has been
     * authenticated.
     *
     * <p>This authorization applies only to secret key and private key operations. Public key
     * operations are not restricted.
     *
     * @see #getUserAuthenticationValidityDurationSeconds()
     * @see Builder#setUserAuthenticationRequired(boolean)
     */
    public boolean isUserAuthenticationRequired() {
        return mUserAuthenticationRequired;
    }

    /**
     * Returns {@code true} if the key is authorized to be used only for messages confirmed by the
     * user.
     *
     * Confirmation is separate from user authentication (see
     * {@link Builder#setUserAuthenticationRequired(boolean)}). Keys can be created that require
     * confirmation but not user authentication, or user authentication but not confirmation, or
     * both. Confirmation verifies that some user with physical possession of the device has
     * approved a displayed message. User authentication verifies that the correct user is present
     * and has authenticated.
     *
     * <p>This authorization applies only to secret key and private key operations. Public key
     * operations are not restricted.
     *
     * @see Builder#setUserConfirmationRequired(boolean)
     */
    public boolean isUserConfirmationRequired() {
        return mUserConfirmationRequired;
    }

    /**
     * Gets the duration of time (seconds) for which this key is authorized to be used after the
     * user is successfully authenticated. This has effect only if user authentication is required
     * (see {@link #isUserAuthenticationRequired()}).
     *
     * <p>This authorization applies only to secret key and private key operations. Public key
     * operations are not restricted.
     *
     * @return duration in seconds or {@code -1} if authentication is required for every use of the
     *         key.
     *
     * @see #isUserAuthenticationRequired()
     * @see Builder#setUserAuthenticationValidityDurationSeconds(int)
     */
    public int getUserAuthenticationValidityDurationSeconds() {
        return mUserAuthenticationValidityDurationSeconds;
    }

    /**
     * Gets the modes of authentication that can authorize use of this key. This has effect only if
     * user authentication is required (see {@link #isUserAuthenticationRequired()}).
     *
     * <p>This authorization applies only to secret key and private key operations. Public key
     * operations are not restricted.
     *
     * @return integer representing the bitwse OR of all acceptable authentication types for the
     *         key.
     *
     * @see #isUserAuthenticationRequired()
     * @see Builder#setUserAuthenticationParameters(int, int)
     */
    public @KeyProperties.AuthEnum int getUserAuthenticationType() {
        return mUserAuthenticationType;
    }
    /**
     * Returns {@code true} if the key is authorized to be used only if a test of user presence has
     * been performed between the {@code Signature.initSign()} and {@code Signature.sign()} calls.
     * It requires that the KeyStore implementation have a direct way to validate the user presence
     * for example a KeyStore hardware backed strongbox can use a button press that is observable
     * in hardware. A test for user presence is tangential to authentication. The test can be part
     * of an authentication step as long as this step can be validated by the hardware protecting
     * the key and cannot be spoofed. For example, a physical button press can be used as a test of
     * user presence if the other pins connected to the button are not able to simulate a button
     * press. There must be no way for the primary processor to fake a button press, or that
     * button must not be used as a test of user presence.
     */
    public boolean isUserPresenceRequired() {
        return mUserPresenceRequired;
    }

    /**
     * Returns the attestation challenge value that will be placed in attestation certificate for
     * this key pair.
     *
     * <p>If this method returns non-{@code null}, the public key certificate for this key pair will
     * contain an extension that describes the details of the key's configuration and
     * authorizations, including the content of the attestation challenge value. If the key is in
     * secure hardware, and if the secure hardware supports attestation, the certificate will be
     * signed by a chain of certificates rooted at a trustworthy CA key. Otherwise the chain will
     * be rooted at an untrusted certificate.
     *
     * <p>If this method returns {@code null}, and the spec is used to generate an asymmetric (RSA
     * or EC) key pair, the public key will have a self-signed certificate if it has purpose {@link
     * KeyProperties#PURPOSE_SIGN}. If does not have purpose {@link KeyProperties#PURPOSE_SIGN}, it
     * will have a fake certificate.
     *
     * <p>Symmetric keys, such as AES and HMAC keys, do not have public key certificates. If a
     * KeyGenParameterSpec with getAttestationChallenge returning non-null is used to generate a
     * symmetric (AES or HMAC) key, {@link javax.crypto.KeyGenerator#generateKey()} will throw
     * {@link java.security.InvalidAlgorithmParameterException}.
     *
     * @see Builder#setAttestationChallenge(byte[])
     */
    public byte[] getAttestationChallenge() {
        return Utils.cloneIfNotNull(mAttestationChallenge);
    }

    /**
     * Returns {@code true} if attestation for the base device properties ({@link Build#BRAND},
     * {@link Build#DEVICE}, {@link Build#MANUFACTURER}, {@link Build#MODEL}, {@link Build#PRODUCT})
     * was requested to be added in the attestation certificate for the generated key.
     *
     * {@link javax.crypto.KeyGenerator#generateKey()} will throw
     * {@link java.security.ProviderException} if device properties attestation fails or is not
     * supported.
     *
     * @see Builder#setDevicePropertiesAttestationIncluded(boolean)
     */
    public boolean isDevicePropertiesAttestationIncluded() {
        return mDevicePropertiesAttestationIncluded;
    }

    /**
     * @hide
     * Allows the caller to specify device IDs to be attested to in the certificate for the
     * generated key pair. These values are the enums specified in
     * {@link android.security.keystore.AttestationUtils}
     *
     * @see android.security.keystore.AttestationUtils#ID_TYPE_SERIAL
     * @see android.security.keystore.AttestationUtils#ID_TYPE_IMEI
     * @see android.security.keystore.AttestationUtils#ID_TYPE_MEID
     * @see android.security.keystore.AttestationUtils#USE_INDIVIDUAL_ATTESTATION
     *
     * @return integer array representing the requested device IDs to attest.
     */
    @SystemApi
    public @NonNull int[] getAttestationIds() {
        return mAttestationIds.clone();
    }

    /**
     * @hide This is a system-only API
     *
     * Returns {@code true} if the attestation certificate will contain a unique ID field.
     */
    @UnsupportedAppUsage
    public boolean isUniqueIdIncluded() {
        return mUniqueIdIncluded;
    }

    /**
     * Returns {@code true} if the key will remain authorized only until the device is removed from
     * the user's body, up to the validity duration.  This option has no effect on keys that don't
     * have an authentication validity duration, and has no effect if the device lacks an on-body
     * sensor.
     *
     * <p>Authorization applies only to secret key and private key operations. Public key operations
     * are not restricted.
     *
     * @see #isUserAuthenticationRequired()
     * @see #getUserAuthenticationValidityDurationSeconds()
     * @see Builder#setUserAuthenticationValidWhileOnBody(boolean)
     */
    public boolean isUserAuthenticationValidWhileOnBody() {
        return mUserAuthenticationValidWhileOnBody;
    }

    /**
     * Returns {@code true} if the key is irreversibly invalidated when a new biometric is
     * enrolled or all enrolled biometrics are removed. This has effect only for keys that
     * require biometric user authentication for every use.
     *
     * @see #isUserAuthenticationRequired()
     * @see #getUserAuthenticationValidityDurationSeconds()
     * @see Builder#setInvalidatedByBiometricEnrollment(boolean)
     */
    public boolean isInvalidatedByBiometricEnrollment() {
        return mInvalidatedByBiometricEnrollment;
    }

    /**
     * Returns {@code true} if the key is protected by a Strongbox security chip.
     */
    public boolean isStrongBoxBacked() {
        return mIsStrongBoxBacked;
    }

    /**
     * Returns {@code true} if the key is authorized to be used only while the device is unlocked.
     *
     * @see Builder#setUnlockedDeviceRequired(boolean)
     */
    public boolean isUnlockedDeviceRequired() {
        return mUnlockedDeviceRequired;
    }

    /**
     * Return the secure user id that this key should be bound to.
     *
     * Normally an authentication-bound key is tied to the secure user id of the current user
     * (either the root SID from GateKeeper for auth-bound keys with a timeout, or the authenticator
     * id of the current biometric set for keys requiring explicit biometric authorization).
     * If this parameter is set (this method returning non-zero value), the key should be tied to
     * the specified secure user id, overriding the logic above.
     *
     * This is only applicable when {@link #isUserAuthenticationRequired} is {@code true}
     *
     * @hide
     */
    public long getBoundToSpecificSecureUserId() {
        return mBoundToSecureUserId;
    }

    /**
     * Returns whether this key is critical to the device encryption flow.
     *
     * @see Builder#setCriticalToDeviceEncryption(boolean)
     * @hide
     */
    public boolean isCriticalToDeviceEncryption() {
        return mCriticalToDeviceEncryption;
    }

    /**
     * Returns the maximum number of times the limited use key is allowed to be used or
     * {@link KeyProperties#UNRESTRICTED_USAGE_COUNT} if there’s no restriction on the number of
     * times the key can be used.
     *
     * @see Builder#setMaxUsageCount(int)
     */
    public int getMaxUsageCount() {
        return mMaxUsageCount;
    }

    /**
     * Returns the alias of the attestation key that will be used to sign the attestation
     * certificate of the generated key.  Note that an attestation certificate will only be
     * generated if an attestation challenge is set.
     *
     * @see Builder#setAttestKeyAlias(String)
     */
    @Nullable
    public String getAttestKeyAlias() {
        return mAttestKeyAlias;
    }

    /**
     * Builder of {@link KeyGenParameterSpec} instances.
     */
    public final static class Builder {
        private final String mKeystoreAlias;
        private @KeyProperties.PurposeEnum int mPurposes;

        private @KeyProperties.Namespace int mNamespace = KeyProperties.NAMESPACE_APPLICATION;
        private int mKeySize = -1;
        private AlgorithmParameterSpec mSpec;
        private X500Principal mCertificateSubject;
        private BigInteger mCertificateSerialNumber;
        private Date mCertificateNotBefore;
        private Date mCertificateNotAfter;
        private Date mKeyValidityStart;
        private Date mKeyValidityForOriginationEnd;
        private Date mKeyValidityForConsumptionEnd;
        private @KeyProperties.DigestEnum String[] mDigests;
        private @NonNull @KeyProperties.DigestEnum Set<String> mMgf1Digests =
                Collections.emptySet();
        private @KeyProperties.EncryptionPaddingEnum String[] mEncryptionPaddings;
        private @KeyProperties.SignaturePaddingEnum String[] mSignaturePaddings;
        private @KeyProperties.BlockModeEnum String[] mBlockModes;
        private boolean mRandomizedEncryptionRequired = true;
        private boolean mUserAuthenticationRequired;
        private int mUserAuthenticationValidityDurationSeconds = 0;
        private @KeyProperties.AuthEnum int mUserAuthenticationType =
                KeyProperties.AUTH_BIOMETRIC_STRONG;
        private boolean mUserPresenceRequired = false;
        private byte[] mAttestationChallenge = null;
        private boolean mDevicePropertiesAttestationIncluded = false;
        private int[] mAttestationIds = new int[0];
        private boolean mUniqueIdIncluded = false;
        private boolean mUserAuthenticationValidWhileOnBody;
        private boolean mInvalidatedByBiometricEnrollment = true;
        private boolean mIsStrongBoxBacked = false;
        private boolean mUserConfirmationRequired;
        private boolean mUnlockedDeviceRequired = false;
        private boolean mCriticalToDeviceEncryption = false;
        private int mMaxUsageCount = KeyProperties.UNRESTRICTED_USAGE_COUNT;
        private String mAttestKeyAlias = null;
        private long mBoundToSecureUserId = GateKeeper.INVALID_SECURE_USER_ID;

        /**
         * Creates a new instance of the {@code Builder}.
         *
         * @param keystoreAlias alias of the entry in which the generated key will appear in
         *        Android KeyStore. Must not be empty.
         * @param purposes set of purposes (e.g., encrypt, decrypt, sign) for which the key can be
         *        used. Attempts to use the key for any other purpose will be rejected.
         *
         *        <p>See {@link KeyProperties}.{@code PURPOSE} flags.
         */
        public Builder(@NonNull String keystoreAlias, @KeyProperties.PurposeEnum int purposes) {
            if (keystoreAlias == null) {
                throw new NullPointerException("keystoreAlias == null");
            } else if (keystoreAlias.isEmpty()) {
                throw new IllegalArgumentException("keystoreAlias must not be empty");
            }
            mKeystoreAlias = keystoreAlias;
            mPurposes = purposes;
        }

        /**
         * A Builder constructor taking in an already-built KeyGenParameterSpec, useful for
         * changing values of the KeyGenParameterSpec quickly.
         * @hide Should be used internally only.
         */
        public Builder(@NonNull KeyGenParameterSpec sourceSpec) {
            this(sourceSpec.getKeystoreAlias(), sourceSpec.getPurposes());
            mNamespace = sourceSpec.getNamespace();
            mKeySize = sourceSpec.getKeySize();
            mSpec = sourceSpec.getAlgorithmParameterSpec();
            mCertificateSubject = sourceSpec.getCertificateSubject();
            mCertificateSerialNumber = sourceSpec.getCertificateSerialNumber();
            mCertificateNotBefore = sourceSpec.getCertificateNotBefore();
            mCertificateNotAfter = sourceSpec.getCertificateNotAfter();
            mKeyValidityStart = sourceSpec.getKeyValidityStart();
            mKeyValidityForOriginationEnd = sourceSpec.getKeyValidityForOriginationEnd();
            mKeyValidityForConsumptionEnd = sourceSpec.getKeyValidityForConsumptionEnd();
            mPurposes = sourceSpec.getPurposes();
            if (sourceSpec.isDigestsSpecified()) {
                mDigests = sourceSpec.getDigests();
            }
            if (sourceSpec.isMgf1DigestsSpecified()) {
                mMgf1Digests = sourceSpec.getMgf1Digests();
            }
            mEncryptionPaddings = sourceSpec.getEncryptionPaddings();
            mSignaturePaddings = sourceSpec.getSignaturePaddings();
            mBlockModes = sourceSpec.getBlockModes();
            mRandomizedEncryptionRequired = sourceSpec.isRandomizedEncryptionRequired();
            mUserAuthenticationRequired = sourceSpec.isUserAuthenticationRequired();
            mUserAuthenticationValidityDurationSeconds =
                sourceSpec.getUserAuthenticationValidityDurationSeconds();
            mUserAuthenticationType = sourceSpec.getUserAuthenticationType();
            mUserPresenceRequired = sourceSpec.isUserPresenceRequired();
            mAttestationChallenge = sourceSpec.getAttestationChallenge();
            mDevicePropertiesAttestationIncluded =
                    sourceSpec.isDevicePropertiesAttestationIncluded();
            mAttestationIds = sourceSpec.getAttestationIds();
            mUniqueIdIncluded = sourceSpec.isUniqueIdIncluded();
            mUserAuthenticationValidWhileOnBody = sourceSpec.isUserAuthenticationValidWhileOnBody();
            mInvalidatedByBiometricEnrollment = sourceSpec.isInvalidatedByBiometricEnrollment();
            mIsStrongBoxBacked = sourceSpec.isStrongBoxBacked();
            mUserConfirmationRequired = sourceSpec.isUserConfirmationRequired();
            mUnlockedDeviceRequired = sourceSpec.isUnlockedDeviceRequired();
            mCriticalToDeviceEncryption = sourceSpec.isCriticalToDeviceEncryption();
            mMaxUsageCount = sourceSpec.getMaxUsageCount();
            mAttestKeyAlias = sourceSpec.getAttestKeyAlias();
            mBoundToSecureUserId = sourceSpec.getBoundToSpecificSecureUserId();
        }

        /**
         * Sets the UID which will own the key.
         *
         * Such cross-UID access is permitted to a few system UIDs and only to a few other UIDs
         * (e.g., Wi-Fi, VPN) all of which are system.
         *
         * @param uid UID or {@code -1} for the UID of the current process.
         *
         * @deprecated Setting the UID of the target namespace is based on a hardcoded
         * hack in the Keystore service. This is no longer supported with Keystore 2.0/Android S.
         * Instead, dedicated non UID based namespaces can be configured in SEPolicy using
         * the keystore2_key_contexts files. The functionality of this method will be supported
         * by mapping knows special UIDs, such as WIFI, to the newly configured SELinux based
         * namespaces. Unknown UIDs will yield {@link IllegalArgumentException}.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        @Deprecated
        public Builder setUid(int uid) {
            mNamespace = KeyProperties.legacyUidToNamespace(uid);
            return this;
        }

        /**
         * Set the designated SELinux namespace that the key shall live in. The caller must
         * have sufficient permissions to install a key in the given namespace. Namespaces
         * can be created using SEPolicy. The keystore2_key_contexts files map numeric
         * namespaces to SELinux labels, and SEPolicy can be used to grant access to these
         * namespaces to the desired target context. This is the preferred way to share
         * keys between system and vendor components, e.g., WIFI settings and WPA supplicant.
         *
         * @param namespace Numeric SELinux namespace as configured in keystore2_key_contexts
         *         of Android's SEPolicy.
         *         See <a href="https://source.android.com/security/keystore#access-control">
         *             Keystore 2.0 access control</a>
         * @return this Builder object.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setNamespace(@KeyProperties.Namespace int namespace) {
            mNamespace = namespace;
            return this;
        }

        /**
         * Sets the size (in bits) of the key to be generated. For instance, for RSA keys this sets
         * the modulus size, for EC keys this selects a curve with a matching field size, and for
         * symmetric keys this sets the size of the bitstring which is their key material.
         *
         * <p>The default key size is specific to each key algorithm. If key size is not set
         * via this method, it should be looked up from the algorithm-specific parameters (if any)
         * provided via
         * {@link #setAlgorithmParameterSpec(AlgorithmParameterSpec) setAlgorithmParameterSpec}.
         */
        @NonNull
        public Builder setKeySize(int keySize) {
            if (keySize < 0) {
                throw new IllegalArgumentException("keySize < 0");
            }
            mKeySize = keySize;
            return this;
        }

        /**
         * Sets the algorithm-specific key generation parameters. For example, for RSA keys this may
         * be an instance of {@link java.security.spec.RSAKeyGenParameterSpec} whereas for EC keys
         * this may be an instance of {@link java.security.spec.ECGenParameterSpec}.
         *
         * <p>These key generation parameters must match other explicitly set parameters (if any),
         * such as key size.
         */
        public Builder setAlgorithmParameterSpec(@NonNull AlgorithmParameterSpec spec) {
            if (spec == null) {
                throw new NullPointerException("spec == null");
            }
            mSpec = spec;
            return this;
        }

        /**
         * Sets the subject used for the certificate of the generated key pair.
         *
         * <p>By default, the subject is {@code CN=fake}.
         */
        @NonNull
        public Builder setCertificateSubject(@NonNull X500Principal subject) {
            if (subject == null) {
                throw new NullPointerException("subject == null");
            }
            mCertificateSubject = subject;
            return this;
        }

        /**
         * Sets the serial number used for the certificate of the generated key pair.
         * To ensure compatibility with devices and certificate parsers, the value
         * should be 20 bytes or shorter (see RFC 5280 section 4.1.2.2).
         *
         * <p>By default, the serial number is {@code 1}.
         */
        @NonNull
        public Builder setCertificateSerialNumber(@NonNull BigInteger serialNumber) {
            if (serialNumber == null) {
                throw new NullPointerException("serialNumber == null");
            }
            mCertificateSerialNumber = serialNumber;
            return this;
        }

        /**
         * Sets the start of the validity period for the certificate of the generated key pair.
         *
         * <p>By default, this date is {@code Jan 1 1970}.
         */
        @NonNull
        public Builder setCertificateNotBefore(@NonNull Date date) {
            if (date == null) {
                throw new NullPointerException("date == null");
            }
            mCertificateNotBefore = Utils.cloneIfNotNull(date);
            return this;
        }

        /**
         * Sets the end of the validity period for the certificate of the generated key pair.
         *
         * <p>By default, this date is {@code Jan 1 2048}.
         */
        @NonNull
        public Builder setCertificateNotAfter(@NonNull Date date) {
            if (date == null) {
                throw new NullPointerException("date == null");
            }
            mCertificateNotAfter = Utils.cloneIfNotNull(date);
            return this;
        }

        /**
         * Sets the time instant before which the key is not yet valid.
         *
         * <p>By default, the key is valid at any instant.
         *
         * @see #setKeyValidityEnd(Date)
         */
        @NonNull
        public Builder setKeyValidityStart(Date startDate) {
            mKeyValidityStart = Utils.cloneIfNotNull(startDate);
            return this;
        }

        /**
         * Sets the time instant after which the key is no longer valid.
         *
         * <p>By default, the key is valid at any instant.
         *
         * @see #setKeyValidityStart(Date)
         * @see #setKeyValidityForConsumptionEnd(Date)
         * @see #setKeyValidityForOriginationEnd(Date)
         */
        @NonNull
        public Builder setKeyValidityEnd(Date endDate) {
            setKeyValidityForOriginationEnd(endDate);
            setKeyValidityForConsumptionEnd(endDate);
            return this;
        }

        /**
         * Sets the time instant after which the key is no longer valid for encryption and signing.
         *
         * <p>By default, the key is valid at any instant.
         *
         * @see #setKeyValidityForConsumptionEnd(Date)
         */
        @NonNull
        public Builder setKeyValidityForOriginationEnd(Date endDate) {
            mKeyValidityForOriginationEnd = Utils.cloneIfNotNull(endDate);
            return this;
        }

        /**
         * Sets the time instant after which the key is no longer valid for decryption and
         * verification.
         *
         * <p>By default, the key is valid at any instant.
         *
         * @see #setKeyValidityForOriginationEnd(Date)
         */
        @NonNull
        public Builder setKeyValidityForConsumptionEnd(Date endDate) {
            mKeyValidityForConsumptionEnd = Utils.cloneIfNotNull(endDate);
            return this;
        }

        /**
         * Sets the set of digests algorithms (e.g., {@code SHA-256}, {@code SHA-384}) with which
         * the key can be used. Attempts to use the key with any other digest algorithm will be
         * rejected.
         *
         * <p>This must be specified for signing/verification keys and RSA encryption/decryption
         * keys used with RSA OAEP padding scheme because these operations involve a digest. For
         * HMAC keys, the default is the digest associated with the key algorithm (e.g.,
         * {@code SHA-256} for key algorithm {@code HmacSHA256}). HMAC keys cannot be authorized
         * for more than one digest.
         *
         * <p>For private keys used for TLS/SSL client or server authentication it is usually
         * necessary to authorize the use of no digest ({@link KeyProperties#DIGEST_NONE}). This is
         * because TLS/SSL stacks typically generate the necessary digest(s) themselves and then use
         * a private key to sign it.
         *
         * <p>See {@link KeyProperties}.{@code DIGEST} constants.
         */
        @NonNull
        public Builder setDigests(@KeyProperties.DigestEnum String... digests) {
            mDigests = ArrayUtils.cloneIfNotEmpty(digests);
            return this;
        }

        /**
         * Sets the set of hash functions (e.g., {@code SHA-256}, {@code SHA-384}) which could be
         * used by the mask generation function MGF1 (which is used for certain operations with
         * the key). Attempts to use the key with any other digest for the mask generation
         * function will be rejected.
         *
         * <p>This can only be specified for signing/verification keys and RSA encryption/decryption
         * keys used with RSA OAEP padding scheme because these operations involve a mask generation
         * function (MGF1) with a digest.
         * The default digest for MGF1 is {@code SHA-1}, which will be specified during key creation
         * time if no digests have been explicitly provided.
         * {@code null} may not be specified as a parameter to this method: It is not possible to
         * disable MGF1 digest, a default must be present for when the caller tries to use it.
         *
         * <p>When using the key, the caller may not specify any digests that were not provided
         * during key creation time. The caller may specify the default digest, {@code SHA-1}, if no
         * digests were explicitly provided during key creation (but it is not necessary to do so).
         *
         * <p>See {@link KeyProperties}.{@code DIGEST} constants.
         */
        @NonNull
        @FlaggedApi(android.security.Flags.FLAG_MGF1_DIGEST_SETTER_V2)
        public Builder setMgf1Digests(@NonNull @KeyProperties.DigestEnum String... mgf1Digests) {
            mMgf1Digests = Set.of(mgf1Digests);
            return this;
        }

        /**
         * Sets the set of padding schemes (e.g., {@code PKCS7Padding}, {@code OAEPPadding},
         * {@code PKCS1Padding}, {@code NoPadding}) with which the key can be used when
         * encrypting/decrypting. Attempts to use the key with any other padding scheme will be
         * rejected.
         *
         * <p>This must be specified for keys which are used for encryption/decryption.
         *
         * <p>For RSA private keys used by TLS/SSL servers to authenticate themselves to clients it
         * is usually necessary to authorize the use of no/any padding
         * ({@link KeyProperties#ENCRYPTION_PADDING_NONE}) and/or PKCS#1 encryption padding
         * ({@link KeyProperties#ENCRYPTION_PADDING_RSA_PKCS1}). This is because RSA decryption is
         * required by some cipher suites, and some stacks request decryption using no padding
         * whereas others request PKCS#1 padding.
         *
         * <p>See {@link KeyProperties}.{@code ENCRYPTION_PADDING} constants.
         */
        @NonNull
        public Builder setEncryptionPaddings(
                @KeyProperties.EncryptionPaddingEnum String... paddings) {
            mEncryptionPaddings = ArrayUtils.cloneIfNotEmpty(paddings);
            return this;
        }

        /**
         * Sets the set of padding schemes (e.g., {@code PSS}, {@code PKCS#1}) with which the key
         * can be used when signing/verifying. Attempts to use the key with any other padding scheme
         * will be rejected.
         *
         * <p>This must be specified for RSA keys which are used for signing/verification.
         *
         * <p>See {@link KeyProperties}.{@code SIGNATURE_PADDING} constants.
         */
        @NonNull
        public Builder setSignaturePaddings(
                @KeyProperties.SignaturePaddingEnum String... paddings) {
            mSignaturePaddings = ArrayUtils.cloneIfNotEmpty(paddings);
            return this;
        }

        /**
         * Sets the set of block modes (e.g., {@code GCM}, {@code CBC}) with which the key can be
         * used when encrypting/decrypting. Attempts to use the key with any other block modes will
         * be rejected.
         *
         * <p>This must be specified for symmetric encryption/decryption keys.
         *
         * <p>See {@link KeyProperties}.{@code BLOCK_MODE} constants.
         */
        @NonNull
        public Builder setBlockModes(@KeyProperties.BlockModeEnum String... blockModes) {
            mBlockModes = ArrayUtils.cloneIfNotEmpty(blockModes);
            return this;
        }

        /**
         * Sets whether encryption using this key must be sufficiently randomized to produce
         * different ciphertexts for the same plaintext every time. The formal cryptographic
         * property being required is <em>indistinguishability under chosen-plaintext attack
         * ({@code IND-CPA})</em>. This property is important because it mitigates several classes
         * of weaknesses due to which ciphertext may leak information about plaintext. For example,
         * if a given plaintext always produces the same ciphertext, an attacker may see the
         * repeated ciphertexts and be able to deduce something about the plaintext.
         *
         * <p>By default, {@code IND-CPA} is required.
         *
         * <p>When {@code IND-CPA} is required:
         * <ul>
         * <li>encryption/decryption transformation which do not offer {@code IND-CPA}, such as
         * {@code ECB} with a symmetric encryption algorithm, or RSA encryption/decryption without
         * padding, are prohibited;</li>
         * <li>in block modes which use an IV, such as {@code GCM}, {@code CBC}, and {@code CTR},
         * caller-provided IVs are rejected when encrypting, to ensure that only random IVs are
         * used.</li>
         * </ul>
         *
         * <p>Before disabling this requirement, consider the following approaches instead:
         * <ul>
         * <li>If you are generating a random IV for encryption and then initializing a {@code}
         * Cipher using the IV, the solution is to let the {@code Cipher} generate a random IV
         * instead. This will occur if the {@code Cipher} is initialized for encryption without an
         * IV. The IV can then be queried via {@link Cipher#getIV()}.</li>
         * <li>If you are generating a non-random IV (e.g., an IV derived from something not fully
         * random, such as the name of the file being encrypted, or transaction ID, or password,
         * or a device identifier), consider changing your design to use a random IV which will then
         * be provided in addition to the ciphertext to the entities which need to decrypt the
         * ciphertext.</li>
         * <li>If you are using RSA encryption without padding, consider switching to encryption
         * padding schemes which offer {@code IND-CPA}, such as PKCS#1 or OAEP.</li>
         * </ul>
         */
        @NonNull
        public Builder setRandomizedEncryptionRequired(boolean required) {
            mRandomizedEncryptionRequired = required;
            return this;
        }

        /**
         * Sets whether this key is authorized to be used only if the user has been authenticated.
         *
         * <p>By default, the key is authorized to be used regardless of whether the user has been
         * authenticated.
         *
         * <p>When user authentication is required:
         * <ul>
         * <li>The key can only be generated if secure lock screen is set up (see
         * {@link KeyguardManager#isDeviceSecure()}). Additionally, if the key requires that user
         * authentication takes place for every use of the key (see
         * {@link #setUserAuthenticationValidityDurationSeconds(int)}), at least one biometric
         * must be enrolled (see {@link BiometricManager#canAuthenticate()}).</li>
         * <li>The use of the key must be authorized by the user by authenticating to this Android
         * device using a subset of their secure lock screen credentials such as
         * password/PIN/pattern or biometric.
         * <a href="{@docRoot}training/articles/keystore.html#UserAuthentication">More
         * information</a>.
         * <li>The key will become <em>irreversibly invalidated</em> once the secure lock screen is
         * disabled (reconfigured to None, Swipe or other mode which does not authenticate the user)
         * or when the secure lock screen is forcibly reset (e.g., by a Device Administrator).
         * Additionally, if the key requires that user authentication takes place for every use of
         * the key, it is also irreversibly invalidated once a new biometric is enrolled or once\
         * no more biometrics are enrolled, unless {@link
         * #setInvalidatedByBiometricEnrollment(boolean)} is used to allow validity after
         * enrollment, or {@code KeyProperties.AUTH_DEVICE_CREDENTIAL} is specified as part of
         * the parameters to {@link #setUserAuthenticationParameters}.
         * Attempts to initialize cryptographic operations using such keys will throw
         * {@link KeyPermanentlyInvalidatedException}.</li>
         * </ul>
         *
         * <p>This authorization applies only to secret key and private key operations. Public key
         * operations are not restricted.
         *
         * @see #setUserAuthenticationValidityDurationSeconds(int)
         * @see KeyguardManager#isDeviceSecure()
         * @see BiometricManager#canAuthenticate()
         */
        @NonNull
        public Builder setUserAuthenticationRequired(boolean required) {
            mUserAuthenticationRequired = required;
            return this;
        }

        /**
         * Sets whether this key is authorized to be used only for messages confirmed by the
         * user.
         *
         * Confirmation is separate from user authentication (see
         * {@link #setUserAuthenticationRequired(boolean)}). Keys can be created that require
         * confirmation but not user authentication, or user authentication but not confirmation,
         * or both. Confirmation verifies that some user with physical possession of the device has
         * approved a displayed message. User authentication verifies that the correct user is
         * present and has authenticated.
         *
         * <p>This authorization applies only to secret key and private key operations. Public key
         * operations are not restricted.
         *
         * See {@link android.security.ConfirmationPrompt} class for
         * more details about user confirmations.
         */
        @NonNull
        public Builder setUserConfirmationRequired(boolean required) {
            mUserConfirmationRequired = required;
            return this;
        }

        /**
         * Sets the duration of time (seconds) for which this key is authorized to be used after the
         * user is successfully authenticated. This has effect if the key requires user
         * authentication for its use (see {@link #setUserAuthenticationRequired(boolean)}).
         *
         * <p>By default, if user authentication is required, it must take place for every use of
         * the key.
         *
         * <p>Cryptographic operations involving keys which require user authentication to take
         * place for every operation can only use biometric authentication. This is achieved by
         * initializing a cryptographic operation ({@link Signature}, {@link Cipher}, {@link Mac})
         * with the key, wrapping it into a {@link BiometricPrompt.CryptoObject}, invoking
         * {@code BiometricPrompt.authenticate} with {@code CryptoObject}, and proceeding with
         * the cryptographic operation only if the authentication flow succeeds.
         *
         * <p>Cryptographic operations involving keys which are authorized to be used for a duration
         * of time after a successful user authentication event can only use secure lock screen
         * authentication. These cryptographic operations will throw
         * {@link UserNotAuthenticatedException} during initialization if the user needs to be
         * authenticated to proceed. This situation can be resolved by the user unlocking the secure
         * lock screen of the Android or by going through the confirm credential flow initiated by
         * {@link KeyguardManager#createConfirmDeviceCredentialIntent(CharSequence, CharSequence)}.
         * Once resolved, initializing a new cryptographic operation using this key (or any other
         * key which is authorized to be used for a fixed duration of time after user
         * authentication) should succeed provided the user authentication flow completed
         * successfully.
         *
         * @param seconds duration in seconds or {@code -1} if user authentication must take place
         *        for every use of the key.
         *
         * @see #setUserAuthenticationRequired(boolean)
         * @see BiometricPrompt
         * @see BiometricPrompt.CryptoObject
         * @see KeyguardManager
         * @deprecated See {@link #setUserAuthenticationParameters(int, int)}
         */
        @Deprecated
        @NonNull
        public Builder setUserAuthenticationValidityDurationSeconds(
                @IntRange(from = -1) int seconds) {
            if (seconds < -1) {
                throw new IllegalArgumentException("seconds must be -1 or larger");
            }
            if (seconds == -1) {
                return setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
            }
            return setUserAuthenticationParameters(seconds, KeyProperties.AUTH_DEVICE_CREDENTIAL
                                                            | KeyProperties.AUTH_BIOMETRIC_STRONG);
        }

        /**
         * Sets the duration of time (seconds) and authorization type for which this key is
         * authorized to be used after the user is successfully authenticated. This has effect if
         * the key requires user authentication for its use (see
         * {@link #setUserAuthenticationRequired(boolean)}).
         *
         * <p>By default, if user authentication is required, it must take place for every use of
         * the key.
         *
         * <p>These cryptographic operations will throw {@link UserNotAuthenticatedException} during
         * initialization if the user needs to be authenticated to proceed. This situation can be
         * resolved by the user authenticating with the appropriate biometric or credential as
         * required by the key. See {@link BiometricPrompt.Builder#setAllowedAuthenticators(int)}
         * and {@link BiometricManager.Authenticators}.
         *
         * <p>Once resolved, initializing a new cryptographic operation using this key (or any other
         * key which is authorized to be used for a fixed duration of time after user
         * authentication) should succeed provided the user authentication flow completed
         * successfully.
         *
         * @param timeout duration in seconds or {@code 0} if user authentication must take place
         *        for every use of the key.
         * @param type set of authentication types which can authorize use of the key. See
         *        {@link KeyProperties}.{@code AUTH} flags.
         *
         * @see #setUserAuthenticationRequired(boolean)
         * @see BiometricPrompt
         * @see BiometricPrompt.CryptoObject
         * @see KeyguardManager
         */
        @NonNull
        public Builder setUserAuthenticationParameters(@IntRange(from = 0) int timeout,
                                                       @KeyProperties.AuthEnum int type) {
            if (timeout < 0) {
                throw new IllegalArgumentException("timeout must be 0 or larger");
            }
            mUserAuthenticationValidityDurationSeconds = timeout;
            mUserAuthenticationType = type;
            return this;
        }

        /**
         * Sets whether a test of user presence is required to be performed between the
         * {@code Signature.initSign()} and {@code Signature.sign()} method calls.
         * It requires that the KeyStore implementation have a direct way to validate the user
         * presence for example a KeyStore hardware backed strongbox can use a button press that
         * is observable in hardware. A test for user presence is tangential to authentication. The
         * test can be part of an authentication step as long as this step can be validated by the
         * hardware protecting the key and cannot be spoofed. For example, a physical button press
         * can be used as a test of user presence if the other pins connected to the button are not
         * able to simulate a button press.There must be no way for the primary processor to fake a
         * button press, or that button must not be used as a test of user presence.
         */
        @NonNull
        public Builder setUserPresenceRequired(boolean required) {
            mUserPresenceRequired = required;
            return this;
        }

        /**
         * Sets whether an attestation certificate will be generated for this key pair, and what
         * challenge value will be placed in the certificate.  The attestation certificate chain
         * can be retrieved with with {@link java.security.KeyStore#getCertificateChain(String)}.
         *
         * <p>If {@code attestationChallenge} is not {@code null}, the public key certificate for
         * this key pair will contain an extension that describes the details of the key's
         * configuration and authorizations, including the {@code attestationChallenge} value. If
         * the key is in secure hardware, and if the secure hardware supports attestation, the
         * certificate will be signed by a chain of certificates rooted at a trustworthy CA key.
         * Otherwise the chain will be rooted at an untrusted certificate.
         *
         * <p>The purpose of the challenge value is to enable relying parties to verify that the key
         * was created in response to a specific request. If attestation is desired but no
         * challenged is needed, any non-{@code null} value may be used, including an empty byte
         * array.
         *
         * <p>If {@code attestationChallenge} is {@code null}, and this spec is used to generate an
         * asymmetric (RSA or EC) key pair, the public key certificate will be self-signed if the
         * key has purpose {@link android.security.keystore.KeyProperties#PURPOSE_SIGN}. If the key
         * does not have purpose {@link android.security.keystore.KeyProperties#PURPOSE_SIGN}, it is
         * not possible to use the key to sign a certificate, so the public key certificate will
         * contain a placeholder signature.
         *
         * <p>Symmetric keys, such as AES and HMAC keys, do not have public key certificates. If a
         * {@link #getAttestationChallenge()} returns non-null and the spec is used to generate a
         * symmetric (AES or HMAC) key, {@link javax.crypto.KeyGenerator#generateKey()} will throw
         * {@link java.security.InvalidAlgorithmParameterException}.
         *
         * <p>The challenge may be up to 128 bytes.
         */
        @NonNull
        public Builder setAttestationChallenge(byte[] attestationChallenge) {
            mAttestationChallenge = attestationChallenge;
            return this;
        }

        /**
         * Sets whether to include the base device properties in the attestation certificate.
         *
         * <p>If {@code attestationChallenge} is not {@code null}, the public key certificate for
         * this key pair will contain an extension that describes the details of the key's
         * configuration and authorizations, including the device properties values (brand, device,
         * manufacturer, model, product). These should be the same as in ({@link Build#BRAND},
         * {@link Build#DEVICE}, {@link Build#MANUFACTURER}, {@link Build#MODEL},
         * {@link Build#PRODUCT}). The attestation certificate chain can
         * be retrieved with {@link java.security.KeyStore#getCertificateChain(String)}.
         *
         * <p> If {@code attestationChallenge} is {@code null}, the public key certificate for
         * this key pair will not contain the extension with the requested attested values.
         *
         * <p> {@link javax.crypto.KeyGenerator#generateKey()} will throw
         * {@link java.security.ProviderException} if device properties attestation fails or is not
         * supported.
         */
        @NonNull
        public Builder setDevicePropertiesAttestationIncluded(
                boolean devicePropertiesAttestationIncluded) {
            mDevicePropertiesAttestationIncluded = devicePropertiesAttestationIncluded;
            return this;
        }

        /**
         * @hide
         * Sets which IDs to attest in the attestation certificate for the key. The acceptable
         * values in this integer array are the enums specified in
         * {@link android.security.keystore.AttestationUtils}
         *
         * @param attestationIds the array of ID types to attest to in the certificate.
         *
         * @see android.security.keystore.AttestationUtils#ID_TYPE_SERIAL
         * @see android.security.keystore.AttestationUtils#ID_TYPE_IMEI
         * @see android.security.keystore.AttestationUtils#ID_TYPE_MEID
         * @see android.security.keystore.AttestationUtils#USE_INDIVIDUAL_ATTESTATION
         */
        @SystemApi
        @NonNull
        public Builder setAttestationIds(@NonNull int[] attestationIds) {
            mAttestationIds = attestationIds;
            return this;
        }

        /**
         * @hide Only system apps can use this method.
         *
         * Sets whether to include a temporary unique ID field in the attestation certificate.
         */
        @UnsupportedAppUsage
        @TestApi
        @NonNull
        public Builder setUniqueIdIncluded(boolean uniqueIdIncluded) {
            mUniqueIdIncluded = uniqueIdIncluded;
            return this;
        }

        /**
         * Sets whether the key will remain authorized only until the device is removed from the
         * user's body up to the limit of the authentication validity period (see
         * {@link #setUserAuthenticationValidityDurationSeconds} and
         * {@link #setUserAuthenticationRequired}). Once the device has been removed from the
         * user's body, the key will be considered unauthorized and the user will need to
         * re-authenticate to use it. If the device does not have an on-body sensor or the key does
         * not have an authentication validity period, this parameter has no effect.
         * <p>
         * Since Android 12 (API level 31), this parameter has no effect even on devices that have
         * an on-body sensor. A future version of Android may restore enforcement of this parameter.
         * Meanwhile, it is recommended to not use it.
         *
         * @param remainsValid if {@code true}, and if the device supports enforcement of this
         * parameter, the key will be invalidated when the device is removed from the user's body or
         * when the authentication validity expires, whichever occurs first.
         */
        @NonNull
        public Builder setUserAuthenticationValidWhileOnBody(boolean remainsValid) {
            mUserAuthenticationValidWhileOnBody = remainsValid;
            return this;
        }

        /**
         * Sets whether this key should be invalidated on biometric enrollment.  This
         * applies only to keys which require user authentication (see {@link
         * #setUserAuthenticationRequired(boolean)}) and if no positive validity duration has been
         * set (see {@link #setUserAuthenticationValidityDurationSeconds(int)}, meaning the key is
         * valid for biometric authentication only.
         *
         * <p>By default, {@code invalidateKey} is {@code true}, so keys that are valid for
         * biometric authentication only are <em>irreversibly invalidated</em> when a new
         * biometric is enrolled, or when all existing biometrics are deleted.  That may be
         * changed by calling this method with {@code invalidateKey} set to {@code false}.
         *
         * <p>Invalidating keys on enrollment of a new biometric or unenrollment of all biometrics
         * improves security by ensuring that an unauthorized person who obtains the password can't
         * gain the use of biometric-authenticated keys by enrolling their own biometric.  However,
         * invalidating keys makes key-dependent operations impossible, requiring some fallback
         * procedure to authenticate the user and set up a new key.
         */
        @NonNull
        public Builder setInvalidatedByBiometricEnrollment(boolean invalidateKey) {
            mInvalidatedByBiometricEnrollment = invalidateKey;
            return this;
        }

        /**
         * Sets whether this key should be protected by a StrongBox security chip.
         */
        @NonNull
        public Builder setIsStrongBoxBacked(boolean isStrongBoxBacked) {
            mIsStrongBoxBacked = isStrongBoxBacked;
            return this;
        }

        /**
         * Sets whether this key is authorized to be used only while the device is unlocked.
         * <p>
         * The device is considered to be locked for a user when the user's apps are currently
         * inaccessible and some form of lock screen authentication is required to regain access to
         * them. For the full definition, see {@link KeyguardManager#isDeviceLocked()}.
         * <p>
         * Public key operations aren't restricted by {@code setUnlockedDeviceRequired(true)} and
         * may be performed even while the device is locked. In Android 11 (API level 30) and lower,
         * encryption and verification operations with symmetric keys weren't restricted either.
         * <p>
         * Keys that use {@code setUnlockedDeviceRequired(true)} can be imported and generated even
         * while the device is locked, as long as the device has been unlocked at least once since
         * the last reboot. However, such keys cannot be used (except for the unrestricted
         * operations mentioned above) until the device is unlocked. Apps that need to encrypt data
         * while the device is locked such that it can only be decrypted while the device is
         * unlocked can generate a key and encrypt the data in software, import the key into
         * Keystore using {@code setUnlockedDeviceRequired(true)}, and zeroize the original key.
         * <p>
         * {@code setUnlockedDeviceRequired(true)} is related to but distinct from
         * {@link #setUserAuthenticationRequired(boolean) setUserAuthenticationRequired(true)}.
         * {@code setUnlockedDeviceRequired(true)} requires that the device be unlocked, whereas
         * {@code setUserAuthenticationRequired(true)} requires that a specific type of strong
         * authentication has happened within a specific time period. They may be used together or
         * separately; there are cases in which one requirement can be satisfied but not the other.
         * <p>
         * <b>Warning:</b> Be careful using {@code setUnlockedDeviceRequired(true)} on Android 14
         * (API level 34) and lower, since the following bugs existed in Android 12 through 14:
         * <ul>
         *   <li>When the user didn't have a secure lock screen, unlocked-device-required keys
         *   couldn't be generated, imported, or used.</li>
         *   <li>When the user's secure lock screen was removed, all of that user's
         *   unlocked-device-required keys were automatically deleted.</li>
         *   <li>Unlocking the device with a non-strong biometric, such as face on many devices,
         *   didn't re-authorize the use of unlocked-device-required keys.</li>
         *   <li>Unlocking the device with a biometric didn't re-authorize the use of
         *   unlocked-device-required keys in profiles that share their parent user's lock.</li>
         * </ul>
         * These issues are fixed in Android 15, so apps can avoid them by using
         * {@code setUnlockedDeviceRequired(true)} only on Android 15 and higher.
         * Apps that use both {@code setUnlockedDeviceRequired(true)} and
         * {@link #setUserAuthenticationRequired(boolean) setUserAuthenticationRequired(true)}
         * are unaffected by the first two issues, since the first two issues describe expected
         * behavior for {@code setUserAuthenticationRequired(true)}.
         */
        @NonNull
        public Builder setUnlockedDeviceRequired(boolean unlockedDeviceRequired) {
            mUnlockedDeviceRequired = unlockedDeviceRequired;
            return this;
        }

        /**
         * Set whether this key is critical to the device encryption flow
         *
         * This is a special flag only available to system servers to indicate the current key
         * is part of the device encryption flow. Setting this flag causes the key to not
         * be cryptographically bound to the LSKF even if the key is otherwise authentication
         * bound.
         *
         * @hide
         */
        public Builder setCriticalToDeviceEncryption(boolean critical) {
            mCriticalToDeviceEncryption = critical;
            return this;
        }

        /**
         * Sets the maximum number of times the key is allowed to be used. After every use of the
         * key, the use counter will decrease. This authorization applies only to secret key and
         * private key operations. Public key operations are not restricted. For example, after
         * successfully encrypting and decrypting data using methods such as
         * {@link Cipher#doFinal()}, the use counter of the secret key will decrease. After
         * successfully signing data using methods such as {@link Signature#sign()}, the use
         * counter of the private key will decrease.
         *
         * When the use counter is depleted, the key will be marked for deletion by Android
         * Keystore and any subsequent attempt to use the key will throw
         * {@link KeyPermanentlyInvalidatedException}. There is no key to be loaded from the
         * Android Keystore once the exhausted key is permanently deleted, as if the key never
         * existed before.
         *
         * <p>By default, there is no restriction on the usage of key.
         *
         * <p>Some secure hardware may not support this feature at all, in which case it will
         * be enforced in software, some secure hardware may support it but only with
         * maxUsageCount = 1, and some secure hardware may support it with larger value
         * of maxUsageCount.
         *
         * <p>The PackageManger feature flags:
         * {@link android.content.pm.PackageManager#FEATURE_KEYSTORE_SINGLE_USE_KEY} and
         * {@link android.content.pm.PackageManager#FEATURE_KEYSTORE_LIMITED_USE_KEY} can be used
         * to check whether the secure hardware cannot enforce this feature, can only enforce it
         * with maxUsageCount = 1, or can enforce it with larger value of maxUsageCount.
         *
         * @param maxUsageCount maximum number of times the key is allowed to be used or
         *        {@link KeyProperties#UNRESTRICTED_USAGE_COUNT} if there is no restriction on the
         *        usage.
         */
        @NonNull
        public Builder setMaxUsageCount(int maxUsageCount) {
            if (maxUsageCount == KeyProperties.UNRESTRICTED_USAGE_COUNT || maxUsageCount > 0) {
                mMaxUsageCount = maxUsageCount;
                return this;
            }
            throw new IllegalArgumentException("maxUsageCount is not valid");
        }

        /**
         * Sets the alias of the attestation key that will be used to sign the attestation
         * certificate for the generated key pair, if an attestation challenge is set with {@link
         * #setAttestationChallenge}.  If an attestKeyAlias is set but no challenge, {@link
         * java.security.KeyPairGenerator#initialize} will throw {@link
         * java.security.InvalidAlgorithmParameterException}.
         *
         * <p>If the attestKeyAlias is set to null (the default), Android Keystore will select an
         * appropriate system-provided attestation signing key.  If not null, the alias must
         * reference an Android Keystore Key that was created with {@link
         * android.security.keystore.KeyProperties#PURPOSE_ATTEST_KEY}, or key generation will throw
         * {@link java.security.InvalidAlgorithmParameterException}.
         *
         * @param attestKeyAlias the alias of the attestation key to be used to sign the
         *        attestation certificate.
         */
        @NonNull
        public Builder setAttestKeyAlias(@Nullable String attestKeyAlias) {
            mAttestKeyAlias = attestKeyAlias;
            return this;
        }

        /**
         * Set the secure user id that this key should be bound to.
         *
         * Normally an authentication-bound key is tied to the secure user id of the current user
         * (either the root SID from GateKeeper for auth-bound keys with a timeout, or the
         * authenticator id of the current biometric set for keys requiring explicit biometric
         * authorization). If this parameter is set (this method returning non-zero value), the key
         * should be tied to the specified secure user id, overriding the logic above.
         *
         * This is only applicable when {@link #setUserAuthenticationRequired} is set to
         * {@code true}
         *
         * @see KeyGenParameterSpec#getBoundToSpecificSecureUserId()
         * @hide
         */
        @NonNull
        public Builder setBoundToSpecificSecureUserId(long secureUserId) {
            mBoundToSecureUserId = secureUserId;
            return this;
        }

        /**
         * Builds an instance of {@code KeyGenParameterSpec}.
         */
        @NonNull
        public KeyGenParameterSpec build() {
            return new KeyGenParameterSpec(
                    mKeystoreAlias,
                    mNamespace,
                    mKeySize,
                    mSpec,
                    mCertificateSubject,
                    mCertificateSerialNumber,
                    mCertificateNotBefore,
                    mCertificateNotAfter,
                    mKeyValidityStart,
                    mKeyValidityForOriginationEnd,
                    mKeyValidityForConsumptionEnd,
                    mPurposes,
                    mDigests,
                    mMgf1Digests,
                    mEncryptionPaddings,
                    mSignaturePaddings,
                    mBlockModes,
                    mRandomizedEncryptionRequired,
                    mUserAuthenticationRequired,
                    mUserAuthenticationValidityDurationSeconds,
                    mUserAuthenticationType,
                    mUserPresenceRequired,
                    mAttestationChallenge,
                    mDevicePropertiesAttestationIncluded,
                    mAttestationIds,
                    mUniqueIdIncluded,
                    mUserAuthenticationValidWhileOnBody,
                    mInvalidatedByBiometricEnrollment,
                    mIsStrongBoxBacked,
                    mUserConfirmationRequired,
                    mUnlockedDeviceRequired,
                    mCriticalToDeviceEncryption,
                    mMaxUsageCount,
                    mAttestKeyAlias,
                    mBoundToSecureUserId);
        }
    }
}

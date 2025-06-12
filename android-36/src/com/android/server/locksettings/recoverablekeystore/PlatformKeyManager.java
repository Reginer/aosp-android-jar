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

package com.android.server.locksettings.recoverablekeystore;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.UserHandle;
import android.security.GateKeeper;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.service.gatekeeper.IGateKeeperService;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverableKeyStoreDb;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Manages creating and checking the validity of the platform key.
 *
 * <p>The platform key is used to wrap the material of recoverable keys before persisting them to
 * disk. It is also used to decrypt the same keys on a screen unlock, before re-wrapping them with
 * a recovery key and syncing them with remote storage.
 *
 * <p>Each platform key has two entries in AndroidKeyStore:
 *
 * <ul>
 *     <li>Encrypt entry - this entry enables the root user to at any time encrypt.
 *     <li>Decrypt entry - this entry enables the root user to decrypt only after recent user
 *       authentication, i.e., within 15 seconds after a screen unlock.
 * </ul>
 *
 * <p>Both entries are enabled only for AES/GCM/NoPadding Cipher algorithm.
 *
 * @hide
 */
public class PlatformKeyManager {
    static final int MIN_GENERATION_ID_FOR_UNLOCKED_DEVICE_REQUIRED = 1001000;

    private static final String TAG = "PlatformKeyManager";
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_SIZE_BITS = 256;
    private static final String KEY_ALIAS_PREFIX =
            "com.android.server.locksettings.recoverablekeystore/platform/";
    private static final String ENCRYPT_KEY_ALIAS_SUFFIX = "encrypt";
    private static final String DECRYPT_KEY_ALIAS_SUFFIX = "decrypt";
    private static final String KEY_WRAP_CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    // Only used for checking if a key is usable
    private static final byte[] GCM_INSECURE_NONCE_BYTES = new byte[12];

    private final Context mContext;
    private final KeyStoreProxy mKeyStore;
    private final RecoverableKeyStoreDb mDatabase;

    /**
     * A new instance operating on behalf of {@code userId}, storing its prefs in the location
     * defined by {@code context}.
     *
     * @param context This should be the context of the RecoverableKeyStoreLoader service.
     * @throws KeyStoreException if failed to initialize AndroidKeyStore.
     * @throws NoSuchAlgorithmException if AES is unavailable - should never happen.
     * @throws SecurityException if the caller does not have permission to write to /data/system.
     *
     * @hide
     */
    public static PlatformKeyManager getInstance(Context context, RecoverableKeyStoreDb database)
            throws KeyStoreException, NoSuchAlgorithmException {
        return new PlatformKeyManager(
                context.getApplicationContext(),
                new KeyStoreProxyImpl(getAndLoadAndroidKeyStore()),
                database);
    }

    @VisibleForTesting
    PlatformKeyManager(
            Context context,
            KeyStoreProxy keyStore,
            RecoverableKeyStoreDb database) {
        mKeyStore = keyStore;
        mContext = context;
        mDatabase = database;
    }

    /**
     * Returns the current generation ID of the platform key. This increments whenever a platform
     * key has to be replaced. (e.g., because the user has removed and then re-added their lock
     * screen). Returns -1 if no key has been generated yet.
     *
     * @param userId The ID of the user to whose lock screen the platform key must be bound.
     *
     * @hide
     */
    public int getGenerationId(int userId) {
        return mDatabase.getPlatformKeyGenerationId(userId);
    }

    /**
     * Returns {@code true} if the platform key is available. A platform key won't be available if
     * device is locked.
     *
     * @param userId The ID of the user to whose lock screen the platform key must be bound.
     *
     * @hide
     */
    public boolean isDeviceLocked(int userId) {
        return mContext.getSystemService(KeyguardManager.class).isDeviceLocked(userId);
    }

    /**
     * Removes the platform key from Android KeyStore.
     * It is triggered when user disables lock screen.
     *
     * @param userId The ID of the user to whose lock screen the platform key must be bound.
     * @param generationId Generation id.
     *
     * @hide
     */
    public void invalidatePlatformKey(int userId, int generationId) {
        if (generationId != -1) {
            try {
                mKeyStore.deleteEntry(getEncryptAlias(userId, generationId));
                mKeyStore.deleteEntry(getDecryptAlias(userId, generationId));
            } catch (KeyStoreException e) {
                // Ignore failed attempt to delete key.
            }
        }
    }

    /**
     * Generates a new key and increments the generation ID. Should be invoked if the platform key
     * is corrupted and needs to be rotated.
     * Updates status of old keys to {@code RecoveryController.RECOVERY_STATUS_PERMANENT_FAILURE}.
     *
     * @param userId The ID of the user to whose lock screen the platform key must be bound.
     * @throws NoSuchAlgorithmException if AES is unavailable - should never happen.
     * @throws KeyStoreException if there is an error in AndroidKeyStore.
     * @throws InsecureUserException if the user does not have a lock screen set.
     * @throws IOException if there was an issue with local database update.
     * @throws RemoteException if there was an issue communicating with {@link IGateKeeperService}.
     *
     * @hide
     */
    @VisibleForTesting
    void regenerate(int userId)
            throws NoSuchAlgorithmException, KeyStoreException, IOException,
                    RemoteException, InsecureUserException {
        int generationId = getGenerationId(userId);
        int nextId;
        if (generationId == -1) {
            nextId = 1;
        } else {
            invalidatePlatformKey(userId, generationId);
            nextId = generationId + 1;
        }
        generateAndLoadKey(userId, nextId);
    }

    /**
     * Returns the platform key used for encryption.
     * Tries to regenerate key one time if it is permanently invalid.
     *
     * @param userId The ID of the user to whose lock screen the platform key must be bound.
     * @throws KeyStoreException if there was an AndroidKeyStore error.
     * @throws UnrecoverableKeyException if the key could not be recovered.
     * @throws NoSuchAlgorithmException if AES is unavailable - should never occur.
     * @throws IOException if there was an issue with local database update.
     * @throws InsecureUserException if the user does not have a lock screen set.
     * @throws RemoteException if there was an issue communicating with {@link IGateKeeperService}.
     *
     * @hide
     */
    public PlatformEncryptionKey getEncryptKey(int userId)
            throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException,
                    IOException, RemoteException, InsecureUserException {
        init(userId);
        try {
            // Try to see if the decryption key is still accessible before using the encryption key.
            // The auth-bound decryption will be unrecoverable if the screen lock is disabled.
            getDecryptKeyInternal(userId);
            return getEncryptKeyInternal(userId);
        } catch (UnrecoverableKeyException e) {
            Log.i(TAG, String.format(Locale.US,
                    "Regenerating permanently invalid Platform key for user %d.",
                    userId));
            regenerate(userId);
            return getEncryptKeyInternal(userId);
        }
    }

    /**
     * Returns the platform key used for encryption.
     *
     * @param userId The ID of the user to whose lock screen the platform key must be bound.
     * @throws KeyStoreException if there was an AndroidKeyStore error.
     * @throws UnrecoverableKeyException if the key could not be recovered.
     * @throws NoSuchAlgorithmException if AES is unavailable - should never occur.
     *
     * @hide
     */
    private PlatformEncryptionKey getEncryptKeyInternal(int userId) throws KeyStoreException,
            UnrecoverableKeyException, NoSuchAlgorithmException {
        int generationId = getGenerationId(userId);
        String alias = getEncryptAlias(userId, generationId);
        if (!isKeyLoaded(userId, generationId)) {
            throw new UnrecoverableKeyException("KeyStore doesn't contain key " + alias);
        }
        SecretKey key = (SecretKey) mKeyStore.getKey(
                alias, /*password=*/ null);
        return new PlatformEncryptionKey(generationId, key);
    }

    /**
     * Returns the platform key used for decryption. Only works after a recent screen unlock.
     * Tries to regenerate key one time if it is permanently invalid.
     *
     * @param userId The ID of the user to whose lock screen the platform key must be bound.
     * @throws KeyStoreException if there was an AndroidKeyStore error.
     * @throws UnrecoverableKeyException if the key could not be recovered.
     * @throws NoSuchAlgorithmException if AES is unavailable - should never occur.
     * @throws IOException if there was an issue with local database update.
     * @throws RemoteException if there was an issue communicating with {@link IGateKeeperService}.
     *
     * @hide
     */
    public PlatformDecryptionKey getDecryptKey(int userId)
            throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException,
                    IOException, InsecureUserException, RemoteException {
        init(userId);
        try {
            PlatformDecryptionKey decryptionKey = getDecryptKeyInternal(userId);
            ensureDecryptionKeyIsValid(userId, decryptionKey);
            return decryptionKey;
        } catch (UnrecoverableKeyException e) {
            Log.i(TAG, String.format(Locale.US,
                    "Regenerating permanently invalid Platform key for user %d.",
                    userId));
            regenerate(userId);
            return getDecryptKeyInternal(userId);
        }
    }

    /**
     * Returns the platform key used for decryption. Only works after a recent screen unlock.
     *
     * @param userId The ID of the user to whose lock screen the platform key must be bound.
     * @throws KeyStoreException if there was an AndroidKeyStore error.
     * @throws UnrecoverableKeyException if the key could not be recovered.
     * @throws NoSuchAlgorithmException if AES is unavailable - should never occur.
     *
     * @hide
     */
    private PlatformDecryptionKey getDecryptKeyInternal(int userId) throws KeyStoreException,
            UnrecoverableKeyException, NoSuchAlgorithmException {
        int generationId = getGenerationId(userId);
        String alias = getDecryptAlias(userId, generationId);
        if (!isKeyLoaded(userId, generationId)) {
            throw new UnrecoverableKeyException("KeyStore doesn't contain key " + alias);
        }
        SecretKey key = (SecretKey) mKeyStore.getKey(
                alias, /*password=*/ null);
        return new PlatformDecryptionKey(generationId, key);
    }

    /**
     * Tries to use the decryption key to make sure it is not permanently invalidated. The exception
     * {@code KeyPermanentlyInvalidatedException} is thrown only when the key is in use.
     *
     * <p>Note that we ignore all other InvalidKeyException exceptions, because such an exception
     * may be thrown for auth-bound keys if there's no recent unlock event.
     */
    private void ensureDecryptionKeyIsValid(int userId, PlatformDecryptionKey decryptionKey)
            throws UnrecoverableKeyException {
        try {
            Cipher.getInstance(KEY_WRAP_CIPHER_ALGORITHM).init(Cipher.UNWRAP_MODE,
                    decryptionKey.getKey(),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, GCM_INSECURE_NONCE_BYTES));
        } catch (KeyPermanentlyInvalidatedException e) {
            Log.e(TAG, String.format(Locale.US, "The platform key for user %d became invalid.",
                    userId));
            throw new UnrecoverableKeyException(e.getMessage());
        } catch (NoSuchAlgorithmException | InvalidKeyException
                | InvalidAlgorithmParameterException | NoSuchPaddingException e) {
            // Ignore all other exceptions
        }
    }

    /**
     * Initializes the class. If there is no current platform key, and the user has a lock screen
     * set, will create the platform key and set the generation ID.
     *
     * @param userId The ID of the user to whose lock screen the platform key must be bound.
     * @throws KeyStoreException if there was an error in AndroidKeyStore.
     * @throws NoSuchAlgorithmException if AES is unavailable - should never happen.
     * @throws IOException if there was an issue with local database update.
     * @throws RemoteException if there was an issue communicating with {@link IGateKeeperService}.
     *
     * @hide
     */
    void init(int userId)
            throws KeyStoreException, NoSuchAlgorithmException, IOException,
                    RemoteException, InsecureUserException {
        int generationId = getGenerationId(userId);
        if (isKeyLoaded(userId, generationId)) {
            Log.i(TAG, String.format(
                    Locale.US, "Platform key generation %d exists already.", generationId));
            return;
        }
        if (generationId == -1) {
            Log.i(TAG, "Generating initial platform key generation ID.");
            generationId = 1;
        } else {
            Log.w(TAG, String.format(Locale.US, "Platform generation ID was %d but no "
                    + "entry was present in AndroidKeyStore. Generating fresh key.", generationId));
            // Have to generate a fresh key, so bump the generation id
            generationId++;
        }

        generationId = Math.max(generationId, MIN_GENERATION_ID_FOR_UNLOCKED_DEVICE_REQUIRED);
        generateAndLoadKey(userId, generationId);
    }

    /**
     * Returns the alias of the encryption key with the specific {@code generationId} in the
     * AndroidKeyStore.
     *
     * <p>These IDs look as follows:
     * {@code com.security.recoverablekeystore/platform/<user id>/<generation id>/encrypt}
     *
     * @param userId The ID of the user to whose lock screen the platform key must be bound.
     * @param generationId The generation ID.
     * @return The alias.
     */
    private String getEncryptAlias(int userId, int generationId) {
        return KEY_ALIAS_PREFIX + userId + "/" + generationId + "/" + ENCRYPT_KEY_ALIAS_SUFFIX;
    }

    /**
     * Returns the alias of the decryption key with the specific {@code generationId} in the
     * AndroidKeyStore.
     *
     * <p>These IDs look as follows:
     * {@code com.security.recoverablekeystore/platform/<user id>/<generation id>/decrypt}
     *
     * @param userId The ID of the user to whose lock screen the platform key must be bound.
     * @param generationId The generation ID.
     * @return The alias.
     */
    private String getDecryptAlias(int userId, int generationId) {
        return KEY_ALIAS_PREFIX + userId + "/" + generationId + "/" + DECRYPT_KEY_ALIAS_SUFFIX;
    }

    /**
     * Sets the current generation ID to {@code generationId}.
     * @throws IOException if there was an issue with local database update.
     */
    private void setGenerationId(int userId, int generationId) throws IOException {
        mDatabase.setPlatformKeyGenerationId(userId, generationId);
    }

    /**
     * Returns {@code true} if a key has been loaded with the given {@code generationId} into
     * AndroidKeyStore.
     *
     * @throws KeyStoreException if there was an error checking AndroidKeyStore.
     */
    private boolean isKeyLoaded(int userId, int generationId) throws KeyStoreException {
        return mKeyStore.containsAlias(getEncryptAlias(userId, generationId))
                && mKeyStore.containsAlias(getDecryptAlias(userId, generationId));
    }

    @VisibleForTesting
    IGateKeeperService getGateKeeperService() {
        return GateKeeper.getService();
    }

    /**
     * Generates a new 256-bit AES key, and loads it into AndroidKeyStore with the given
     * {@code generationId} determining its aliases.
     *
     * @throws NoSuchAlgorithmException if AES is unavailable. This should never happen, as it is
     *     available since API version 1.
     * @throws KeyStoreException if there was an issue loading the keys into AndroidKeyStore.
     * @throws IOException if there was an issue with local database update.
     * @throws RemoteException if there was an issue communicating with {@link IGateKeeperService}.
     */
    private void generateAndLoadKey(int userId, int generationId)
            throws NoSuchAlgorithmException, KeyStoreException, IOException, RemoteException,
                InsecureUserException {
        String encryptAlias = getEncryptAlias(userId, generationId);
        String decryptAlias = getDecryptAlias(userId, generationId);
        // SecretKey implementation doesn't provide reliable way to destroy the secret
        // so it may live in memory for some time.
        SecretKey secretKey = generateAesKey();

        KeyProtection.Builder decryptionKeyProtection =
                new KeyProtection.Builder(KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE);
        // Skip UserAuthenticationRequired for main user
        if (userId ==  UserHandle.USER_SYSTEM) {
            // attempt to store key will fail if screenlock is not set.
            decryptionKeyProtection.setUnlockedDeviceRequired(true);
        } else {
            // Don't set protection params to prevent losing key.
        }
        // Store decryption key first since it is more likely to fail.
        try {
            mKeyStore.setEntry(
                    decryptAlias,
                    new KeyStore.SecretKeyEntry(secretKey),
                    decryptionKeyProtection.build());
            mKeyStore.setEntry(
                    encryptAlias,
                    new KeyStore.SecretKeyEntry(secretKey),
                    new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build());
        } catch (KeyStoreException e) {
            if (!isDeviceSecure(userId)) {
                throw new InsecureUserException("Screenlock is not set");
            } else {
                throw e;
            }
        }
        setGenerationId(userId, generationId);
    }

    /**
     * Generates a new 256-bit AES key, in software.
     *
     * @return The software-generated AES key.
     * @throws NoSuchAlgorithmException if AES key generation is not available. This should never
     *     happen, as AES has been supported since API level 1.
     */
    private static SecretKey generateAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.init(KEY_SIZE_BITS);
        return keyGenerator.generateKey();
    }

    /**
     * Returns AndroidKeyStore-provided {@link KeyStore}, having already invoked
     * {@link KeyStore#load(KeyStore.LoadStoreParameter)}.
     *
     * @throws KeyStoreException if there was a problem getting or initializing the key store.
     */
    private static KeyStore getAndLoadAndroidKeyStore() throws KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(KeyStoreProxyImpl.ANDROID_KEY_STORE_PROVIDER);
        try {
            keyStore.load(/*param=*/ null);
        } catch (CertificateException | IOException | NoSuchAlgorithmException e) {
            // Should never happen.
            throw new KeyStoreException("Unable to load keystore.", e);
        }
        return keyStore;
    }

    private boolean isDeviceSecure(int userId) {
        return mContext.getSystemService(KeyguardManager.class).isDeviceSecure(userId);
    }

}

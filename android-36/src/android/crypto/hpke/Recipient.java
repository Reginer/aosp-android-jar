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
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.util.Objects;

/**
 * A class for receiving HPKE messages.
 */
@SuppressWarnings("NewApi") // Public HPKE classes are always all present together.
@FlaggedApi(com.android.libcore.Flags.FLAG_HPKE_PUBLIC_API)
public class Recipient {
    private final Hpke hpke;
    private final HpkeSpi spi;

    Recipient(@NonNull Hpke hpke, @NonNull HpkeSpi spi) {
        this.hpke = hpke;
        this.spi = spi;
    }

    /**
     * Opens a message, using the internal key schedule maintained by this Recipient.
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9180.html#name-encryption-and-decryption-2">
     *     Opening and sealing</a>
     * @param ciphertext the ciphertext
     * @param aad        optional additional authenticated data, may be null or empty
     * @return the plaintext
     * @throws GeneralSecurityException on decryption failures
     */
    public @NonNull byte[] open(@NonNull byte[] ciphertext, @Nullable byte[] aad)
            throws GeneralSecurityException {
        return spi.engineOpen(ciphertext, aad);
    }

    /**
     * Exports secret key material from this Recipient as described in RFC 9180.
     *
     * @param length  expected output length
     * @param context optional exporter context string, may be null or empty
     * @return exported value
     * @throws IllegalArgumentException if the length is not valid for the KDF in use
     */
    public @NonNull byte[] export(int length, @Nullable byte[] context) {
        return spi.engineExport(length, context);
    }

    /**
     * Returns the {@link HpkeSpi} being used by this Recipient.
     *
     * @return the SPI
     */
    public @NonNull HpkeSpi getSpi() {
        return spi;
    }

    /**
     * Returns the {@link Provider} being used by this Recipient.
     *
     * @return the Provider
     */
    public @NonNull Provider getProvider() {
        return hpke.getProvider();
    }

    /**
     * A builder for HPKE Recipient objects.
     */
    @FlaggedApi(com.android.libcore.Flags.FLAG_HPKE_PUBLIC_API)
    public static class Builder {
        private final Hpke hpke;
        private final byte[] encapsulated ;
        private final PrivateKey recipientKey;
        private byte[] applicationInfo = null;
        private PublicKey senderKey = null;
        private byte[] psk = Hpke.DEFAULT_PSK;
        private byte[] pskId = Hpke.DEFAULT_PSK_ID;

        /**
         * Creates the builder.
         *
         * @param encapsulated encapsulated ephemeral key from an {@link Sender}
         * @param recipientKey private key of the recipient
         */
        public Builder(@NonNull Hpke hpke,
                @NonNull byte[] encapsulated, @NonNull PrivateKey recipientKey) {
            Objects.requireNonNull(hpke);
            Objects.requireNonNull(encapsulated);
            Objects.requireNonNull(recipientKey);
            this.hpke = hpke;
            this.encapsulated = encapsulated;
            this.recipientKey = recipientKey;
        }

        /**
         * Adds optional application-related data which will be used during the key generation
         * process.
         *
         * @param applicationInfo application-specific information
         *
         * @return the Builder
         */
        public @NonNull Builder setApplicationInfo(@NonNull byte[] applicationInfo) {
            Objects.requireNonNull(applicationInfo);
            this.applicationInfo = applicationInfo;
            return this;
        }

        /**
         * Sets the sender key to be used by the recipient for message authentication.
         *
         * @param senderKey the sender's public key
         * @return the Builder
         */
        public @NonNull Builder setSenderKey(@NonNull PublicKey senderKey) {
            Objects.requireNonNull(senderKey);
            this.senderKey = senderKey;
            return this;
        }

        /**
         * Sets pre-shared key information to be used for message authentication.
         *
         * @param psk          the pre-shared secret key
         * @param pskId       the id of the pre-shared key
         * @return the Builder
         */
        public @NonNull Builder setPsk(@NonNull byte[] psk, @NonNull byte[] pskId) {
            Objects.requireNonNull(psk);
            Objects.requireNonNull(pskId);
            this.psk = psk;
            this.pskId = pskId;
            return this;
        }

        /**
         * Builds the {@link Recipient}.
         *
         * @return the Recipient
         * @throws InvalidKeyException           if the sender or recipient key are unsupported
         * @throws UnsupportedOperationException if this Provider does not support the expected mode
         */
        public @NonNull Recipient build() throws InvalidKeyException {
            HpkeSpi spi = hpke.findSpi();
            spi.engineInitRecipient(encapsulated, recipientKey, applicationInfo, senderKey, psk,
                    pskId);
            return new Recipient(hpke, spi);
        }
    }
}

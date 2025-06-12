/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.adservices.topics;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;

import com.android.adservices.flags.Flags;

import java.util.Arrays;
import java.util.Objects;

/**
 * Encrypted form of {@link android.adservices.topics.Topic}. This object will be used to return
 * encrypted topic cipher text along with necessary fields required to decrypt it.
 *
 * <p>Decryption of {@link EncryptedTopic#getEncryptedTopic()} should give json string for {@link
 * Topic}. Example of decrypted json string: {@code { "taxonomy_version": 5, "model_version": 2,
 * "topic_id": 10010 }}
 *
 * <p>Decryption of cipher text is expected to happen on the server with the corresponding algorithm
 * and private key for the public key {@link EncryptedTopic#getKeyIdentifier()}}.
 *
 * <p>Detailed steps on decryption can be found on <a
 * href="https://developer.android.com/design-for-safety/privacy-sandbox/guides/topics">Developer
 * Guide</a>.
 */
@FlaggedApi(Flags.FLAG_TOPICS_ENCRYPTION_ENABLED)
public final class EncryptedTopic {
    @NonNull private final byte[] mEncryptedTopic;
    @NonNull private final String mKeyIdentifier;
    @NonNull private final byte[] mEncapsulatedKey;

    /**
     * Creates encrypted version of the {@link Topic} object.
     *
     * @param encryptedTopic byte array cipher text containing encrypted {@link Topic} json string.
     * @param keyIdentifier key used to identify the public key used for encryption.
     * @param encapsulatedKey encapsulated key generated during HPKE setup.
     */
    public EncryptedTopic(
            @NonNull byte[] encryptedTopic,
            @NonNull String keyIdentifier,
            @NonNull byte[] encapsulatedKey) {
        mEncryptedTopic = Objects.requireNonNull(encryptedTopic);
        mKeyIdentifier = Objects.requireNonNull(keyIdentifier);
        mEncapsulatedKey = Objects.requireNonNull(encapsulatedKey);
    }

    /** Returns encrypted bytes for the JSON version of the {@link Topic} object as cipher text. */
    @NonNull
    public byte[] getEncryptedTopic() {
        return mEncryptedTopic;
    }

    /** Returns key identifier for the used encryption key. */
    @NonNull
    public String getKeyIdentifier() {
        return mKeyIdentifier;
    }

    /** Returns the encapsulated key generated during HPKE setup. */
    @NonNull
    public byte[] getEncapsulatedKey() {
        return mEncapsulatedKey;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof EncryptedTopic)) return false;
        EncryptedTopic encryptedTopic = (EncryptedTopic) object;
        return Arrays.equals(getEncryptedTopic(), encryptedTopic.getEncryptedTopic())
                && getKeyIdentifier().equals(encryptedTopic.getKeyIdentifier())
                && Arrays.equals(getEncapsulatedKey(), encryptedTopic.getEncapsulatedKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                Arrays.hashCode(getEncryptedTopic()),
                getKeyIdentifier(),
                Arrays.hashCode(getEncapsulatedKey()));
    }

    @Override
    public java.lang.String toString() {
        return "EncryptedTopic{"
                + "mEncryptedTopic="
                + Arrays.toString(mEncryptedTopic)
                + ", mKeyIdentifier="
                + mKeyIdentifier
                + ", mEncapsulatedKey="
                + Arrays.toString(mEncapsulatedKey)
                + '}';
    }
}

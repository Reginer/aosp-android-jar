/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.nearby;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents a public credential.
 *
 * @hide
 */
@SystemApi
public final class PublicCredential extends PresenceCredential implements Parcelable {
    @NonNull
    public static final Creator<PublicCredential> CREATOR =
            new Creator<PublicCredential>() {
                @Override
                public PublicCredential createFromParcel(Parcel in) {
                    in.readInt(); // Skip the type as it's used by parent class only.
                    return createFromParcelBody(in);
                }

                @Override
                public PublicCredential[] newArray(int size) {
                    return new PublicCredential[size];
                }
            };

    private final byte[] mPublicKey;
    private final byte[] mEncryptedMetadata;
    private final byte[] mEncryptedMetadataKeyTag;

    private PublicCredential(
            int identityType,
            byte[] secretId,
            byte[] authenticityKey,
            List<CredentialElement> credentialElements,
            byte[] publicKey,
            byte[] encryptedMetadata,
            byte[] metadataEncryptionKeyTag) {
        super(CREDENTIAL_TYPE_PUBLIC, identityType, secretId, authenticityKey, credentialElements);
        mPublicKey = publicKey;
        mEncryptedMetadata = encryptedMetadata;
        mEncryptedMetadataKeyTag = metadataEncryptionKeyTag;
    }

    private PublicCredential(Parcel in) {
        super(CREDENTIAL_TYPE_PUBLIC, in);
        mPublicKey = new byte[in.readInt()];
        in.readByteArray(mPublicKey);
        mEncryptedMetadata = new byte[in.readInt()];
        in.readByteArray(mEncryptedMetadata);
        mEncryptedMetadataKeyTag = new byte[in.readInt()];
        in.readByteArray(mEncryptedMetadataKeyTag);
    }

    static PublicCredential createFromParcelBody(Parcel in) {
        return new PublicCredential(in);
    }

    /** Returns the public key associated with this credential. */
    @NonNull
    public byte[] getPublicKey() {
        return mPublicKey;
    }

    /** Returns the encrypted metadata associated with this credential. */
    @NonNull
    public byte[] getEncryptedMetadata() {
        return mEncryptedMetadata;
    }

    /** Returns the metadata encryption key tag associated with this credential. */
    @NonNull
    public byte[] getEncryptedMetadataKeyTag() {
        return mEncryptedMetadataKeyTag;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof PublicCredential) {
            PublicCredential that = (PublicCredential) obj;
            return super.equals(obj)
                    && Arrays.equals(mPublicKey, that.mPublicKey)
                    && Arrays.equals(mEncryptedMetadata, that.mEncryptedMetadata)
                    && Arrays.equals(mEncryptedMetadataKeyTag, that.mEncryptedMetadataKeyTag);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                Arrays.hashCode(mPublicKey),
                Arrays.hashCode(mEncryptedMetadata),
                Arrays.hashCode(mEncryptedMetadataKeyTag));
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mPublicKey.length);
        dest.writeByteArray(mPublicKey);
        dest.writeInt(mEncryptedMetadata.length);
        dest.writeByteArray(mEncryptedMetadata);
        dest.writeInt(mEncryptedMetadataKeyTag.length);
        dest.writeByteArray(mEncryptedMetadataKeyTag);
    }

    /** Builder class for {@link PresenceCredential}. */
    public static final class Builder {
        private final List<CredentialElement> mCredentialElements;

        private @IdentityType int mIdentityType;
        private final byte[] mSecretId;
        private final byte[] mAuthenticityKey;
        private final byte[] mPublicKey;
        private final byte[] mEncryptedMetadata;
        private final byte[] mEncryptedMetadataKeyTag;

        public Builder(
                @NonNull byte[] secretId,
                @NonNull byte[] authenticityKey,
                @NonNull byte[] publicKey,
                @NonNull byte[] encryptedMetadata,
                @NonNull byte[] encryptedMetadataKeyTag) {
            Preconditions.checkState(
                    secretId != null && secretId.length > 0, "secret id cannot be empty");
            Preconditions.checkState(
                    authenticityKey != null && authenticityKey.length > 0,
                    "authenticity key cannot be empty");
            Preconditions.checkState(
                    publicKey != null && publicKey.length > 0, "publicKey cannot be empty");
            Preconditions.checkState(
                    encryptedMetadata != null && encryptedMetadata.length > 0,
                    "encryptedMetadata cannot be empty");
            Preconditions.checkState(
                    encryptedMetadataKeyTag != null && encryptedMetadataKeyTag.length > 0,
                    "encryptedMetadataKeyTag cannot be empty");

            mSecretId = secretId;
            mAuthenticityKey = authenticityKey;
            mPublicKey = publicKey;
            mEncryptedMetadata = encryptedMetadata;
            mEncryptedMetadataKeyTag = encryptedMetadataKeyTag;
            mCredentialElements = new ArrayList<>();
        }

        /** Sets the identity type for the presence credential. */
        @NonNull
        public Builder setIdentityType(@IdentityType int identityType) {
            mIdentityType = identityType;
            return this;
        }

        /** Adds an element to the credential. */
        @NonNull
        public Builder addCredentialElement(@NonNull CredentialElement credentialElement) {
            Objects.requireNonNull(credentialElement);
            mCredentialElements.add(credentialElement);
            return this;
        }

        /** Builds the {@link PresenceCredential}. */
        @NonNull
        public PublicCredential build() {
            return new PublicCredential(
                    mIdentityType,
                    mSecretId,
                    mAuthenticityKey,
                    mCredentialElements,
                    mPublicKey,
                    mEncryptedMetadata,
                    mEncryptedMetadataKeyTag);
        }
    }
}

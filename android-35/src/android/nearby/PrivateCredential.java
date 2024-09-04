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
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a private credential.
 *
 * @hide
 */
@SystemApi
public final class PrivateCredential extends PresenceCredential implements Parcelable {

    @NonNull
    public static final Creator<PrivateCredential> CREATOR = new Creator<PrivateCredential>() {
        @Override
        public PrivateCredential createFromParcel(Parcel in) {
            in.readInt(); // Skip the type as it's used by parent class only.
            return createFromParcelBody(in);
        }

        @Override
        public PrivateCredential[] newArray(int size) {
            return new PrivateCredential[size];
        }
    };

    private byte[] mMetadataEncryptionKey;
    private String mDeviceName;

    private PrivateCredential(Parcel in) {
        super(CREDENTIAL_TYPE_PRIVATE, in);
        mMetadataEncryptionKey = new byte[in.readInt()];
        in.readByteArray(mMetadataEncryptionKey);
        mDeviceName = in.readString();
    }

    private PrivateCredential(int identityType, byte[] secretId,
            String deviceName, byte[] authenticityKey, List<CredentialElement> credentialElements,
            byte[] metadataEncryptionKey) {
        super(CREDENTIAL_TYPE_PRIVATE, identityType, secretId, authenticityKey,
                credentialElements);
        mDeviceName = deviceName;
        mMetadataEncryptionKey = metadataEncryptionKey;
    }

    static PrivateCredential createFromParcelBody(Parcel in) {
        return new PrivateCredential(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mMetadataEncryptionKey.length);
        dest.writeByteArray(mMetadataEncryptionKey);
        dest.writeString(mDeviceName);
    }

    /**
     * Returns the metadata encryption key associated with this credential.
     */
    @NonNull
    public byte[] getMetadataEncryptionKey() {
        return mMetadataEncryptionKey;
    }

    /**
     * Returns the device name associated with this credential.
     */
    @NonNull
    public String getDeviceName() {
        return mDeviceName;
    }

    /**
     * Builder class for {@link PresenceCredential}.
     */
    public static final class Builder {
        private final List<CredentialElement> mCredentialElements;

        private @IdentityType int mIdentityType;
        private final byte[] mSecretId;
        private final byte[] mAuthenticityKey;
        private final byte[] mMetadataEncryptionKey;
        private final String mDeviceName;

        public Builder(@NonNull byte[] secretId, @NonNull byte[] authenticityKey,
                @NonNull byte[] metadataEncryptionKey, @NonNull String deviceName) {
            Preconditions.checkState(secretId != null && secretId.length > 0,
                    "secret id cannot be empty");
            Preconditions.checkState(authenticityKey != null && authenticityKey.length > 0,
                    "authenticity key cannot be empty");
            Preconditions.checkState(
                    metadataEncryptionKey != null && metadataEncryptionKey.length > 0,
                    "metadataEncryptionKey cannot be empty");
            Preconditions.checkState(deviceName != null && deviceName.length() > 0,
                    "deviceName cannot be empty");
            mSecretId = secretId;
            mAuthenticityKey = authenticityKey;
            mMetadataEncryptionKey = metadataEncryptionKey;
            mDeviceName = deviceName;
            mCredentialElements = new ArrayList<>();
        }

        /**
         * Sets the identity type for the presence credential.
         */
        @NonNull
        public Builder setIdentityType(@IdentityType int identityType) {
            mIdentityType = identityType;
            return this;
        }

        /**
         * Adds an element to the credential.
         */
        @NonNull
        public Builder addCredentialElement(@NonNull CredentialElement credentialElement) {
            mCredentialElements.add(credentialElement);
            return this;
        }

        /**
         * Builds the {@link PresenceCredential}.
         */
        @NonNull
        public PrivateCredential build() {
            return new PrivateCredential(mIdentityType, mSecretId, mDeviceName,
                    mAuthenticityKey, mCredentialElements, mMetadataEncryptionKey);
        }

    }
}

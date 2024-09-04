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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents a credential for Nearby Presence.
 *
 * @hide
 */
@SystemApi
public abstract class PresenceCredential {
    /** Private credential type. */
    public static final int CREDENTIAL_TYPE_PRIVATE = 0;

    /** Public credential type. */
    public static final int CREDENTIAL_TYPE_PUBLIC = 1;

    /**
     * @hide *
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CREDENTIAL_TYPE_PUBLIC, CREDENTIAL_TYPE_PRIVATE})
    public @interface CredentialType {}

    /** Unknown identity type. */
    public static final int IDENTITY_TYPE_UNKNOWN = 0;

    /** Private identity type. */
    public static final int IDENTITY_TYPE_PRIVATE = 1;
    /** Provisioned identity type. */
    public static final int IDENTITY_TYPE_PROVISIONED = 2;
    /** Trusted identity type. */
    public static final int IDENTITY_TYPE_TRUSTED = 3;

    /**
     * @hide *
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        IDENTITY_TYPE_UNKNOWN,
        IDENTITY_TYPE_PRIVATE,
        IDENTITY_TYPE_PROVISIONED,
        IDENTITY_TYPE_TRUSTED
    })
    public @interface IdentityType {}

    private final @CredentialType int mType;
    private final @IdentityType int mIdentityType;
    private final byte[] mSecretId;
    private final byte[] mAuthenticityKey;
    private final List<CredentialElement> mCredentialElements;

    PresenceCredential(
            @CredentialType int type,
            @IdentityType int identityType,
            byte[] secretId,
            byte[] authenticityKey,
            List<CredentialElement> credentialElements) {
        mType = type;
        mIdentityType = identityType;
        mSecretId = secretId;
        mAuthenticityKey = authenticityKey;
        mCredentialElements = credentialElements;
    }

    PresenceCredential(@CredentialType int type, Parcel in) {
        mType = type;
        mIdentityType = in.readInt();
        mSecretId = new byte[in.readInt()];
        in.readByteArray(mSecretId);
        mAuthenticityKey = new byte[in.readInt()];
        in.readByteArray(mAuthenticityKey);
        mCredentialElements = new ArrayList<>();
        in.readList(
                mCredentialElements,
                CredentialElement.class.getClassLoader(),
                CredentialElement.class);
    }

    /** Returns the type of the credential. */
    public @CredentialType int getType() {
        return mType;
    }

    /** Returns the identity type of the credential. */
    public @IdentityType int getIdentityType() {
        return mIdentityType;
    }

    /** Returns the secret id of the credential. */
    @NonNull
    public byte[] getSecretId() {
        return mSecretId;
    }

    /** Returns the authenticity key of the credential. */
    @NonNull
    public byte[] getAuthenticityKey() {
        return mAuthenticityKey;
    }

    /** Returns the elements of the credential. */
    @NonNull
    public List<CredentialElement> getCredentialElements() {
        return mCredentialElements;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PresenceCredential) {
            PresenceCredential that = (PresenceCredential) obj;
            return mType == that.mType
                    && mIdentityType == that.mIdentityType
                    && Arrays.equals(mSecretId, that.mSecretId)
                    && Arrays.equals(mAuthenticityKey, that.mAuthenticityKey)
                    && mCredentialElements.equals(that.mCredentialElements);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mType,
                mIdentityType,
                Arrays.hashCode(mSecretId),
                Arrays.hashCode(mAuthenticityKey),
                mCredentialElements.hashCode());
    }

    /**
     * Writes the presence credential to the parcel.
     *
     * @hide
     */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeInt(mIdentityType);
        dest.writeInt(mSecretId.length);
        dest.writeByteArray(mSecretId);
        dest.writeInt(mAuthenticityKey.length);
        dest.writeByteArray(mAuthenticityKey);
        dest.writeList(mCredentialElements);
    }
}

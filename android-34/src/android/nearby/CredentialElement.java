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

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents an element in {@link PresenceCredential}.
 *
 * @hide
 */
@SystemApi
public final class CredentialElement implements Parcelable {
    private final String mKey;
    private final byte[] mValue;

    /** Constructs a {@link CredentialElement}. */
    public CredentialElement(@NonNull String key, @NonNull byte[] value) {
        Preconditions.checkState(key != null && value != null, "neither key or value can be null");
        mKey = key;
        mValue = value;
    }

    @NonNull
    public static final Parcelable.Creator<CredentialElement> CREATOR =
            new Parcelable.Creator<CredentialElement>() {
                @Override
                public CredentialElement createFromParcel(Parcel in) {
                    String key = in.readString();
                    byte[] value = new byte[in.readInt()];
                    in.readByteArray(value);
                    return new CredentialElement(key, value);
                }

                @Override
                public CredentialElement[] newArray(int size) {
                    return new CredentialElement[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mKey);
        dest.writeInt(mValue.length);
        dest.writeByteArray(mValue);
    }

    /** Returns the key of the credential element. */
    @NonNull
    public String getKey() {
        return mKey;
    }

    /** Returns the value of the credential element. */
    @NonNull
    public byte[] getValue() {
        return mValue;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof CredentialElement) {
            CredentialElement that = (CredentialElement) obj;
            return mKey.equals(that.mKey) && Arrays.equals(mValue, that.mValue);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mKey.hashCode(), Arrays.hashCode(mValue));
    }
}

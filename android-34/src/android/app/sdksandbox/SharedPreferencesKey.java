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

package android.app.sdksandbox;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Key with its type to be synced using {@link
 * SdkSandboxManager#addSyncedSharedPreferencesKeys(java.util.Set)}
 *
 * @hide
 */
public final class SharedPreferencesKey implements Parcelable {
    /** @hide */
    @IntDef(
            prefix = "KEY_TYPE_",
            value = {
                KEY_TYPE_BOOLEAN,
                KEY_TYPE_FLOAT,
                KEY_TYPE_INTEGER,
                KEY_TYPE_LONG,
                KEY_TYPE_STRING,
                KEY_TYPE_STRING_SET,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface KeyType {}

    /**
     * Key type {@code Boolean}.
     */
    public static final int KEY_TYPE_BOOLEAN = 1;

    /**
     * Key type {@code Float}.
     */
    public static final int KEY_TYPE_FLOAT = 2;

    /**
     * Key type {@code Integer}.
     */
    public static final int KEY_TYPE_INTEGER = 3;

    /**
     * Key type {@code Long}.
     */
    public static final int KEY_TYPE_LONG = 4;

    /**
     * Key type {@code String}.
     */
    public static final int KEY_TYPE_STRING = 5;

    /**
     * Key type {@code Set<String>}.
     */
    public static final int KEY_TYPE_STRING_SET = 6;

    private final String mKeyName;
    private final @KeyType int mKeyType;

    public static final @NonNull Parcelable.Creator<SharedPreferencesKey> CREATOR =
            new Parcelable.Creator<SharedPreferencesKey>() {
                public SharedPreferencesKey createFromParcel(Parcel in) {
                    return new SharedPreferencesKey(in);
                }

                public SharedPreferencesKey[] newArray(int size) {
                    return new SharedPreferencesKey[size];
                }
            };

    public SharedPreferencesKey(@NonNull String keyName, @KeyType int keyType) {
        mKeyName = keyName;
        mKeyType = keyType;
    }

    private SharedPreferencesKey(Parcel in) {
        mKeyName = in.readString();
        mKeyType = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeString(mKeyName);
        out.writeInt(mKeyType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "SharedPreferencesKey{" + "mKeyName=" + mKeyName + ", mKeyType='" + mKeyType + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SharedPreferencesKey)) return false;
        final SharedPreferencesKey that = (SharedPreferencesKey) o;
        return mKeyName.equals(that.getName()) && mKeyType == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(mKeyName, mKeyType);
    }

    /** Get name of the key. */
    @NonNull
    public String getName() {
        return mKeyName;
    }

    /** Get type of the key */
    public @KeyType int getType() {
        return mKeyType;
    }
}

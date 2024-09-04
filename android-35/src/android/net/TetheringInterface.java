/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.net;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.TetheringManager.TetheringType;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * The mapping of tethering interface and type.
 * @hide
 */
@SystemApi
public final class TetheringInterface implements Parcelable {
    private final int mType;
    private final String mInterface;

    public TetheringInterface(@TetheringType int type, @NonNull String iface) {
        Objects.requireNonNull(iface);
        mType = type;
        mInterface = iface;
    }

    private TetheringInterface(@NonNull Parcel in) {
        this(in.readInt(), in.readString());
    }

    /** Get tethering type. */
    public int getType() {
        return mType;
    }

    /** Get tethering interface. */
    @NonNull
    public String getInterface() {
        return mInterface;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeString(mInterface);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mInterface);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof TetheringInterface)) return false;
        final TetheringInterface other = (TetheringInterface) obj;
        return mType == other.mType && mInterface.equals(other.mInterface);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<TetheringInterface> CREATOR = new Creator<TetheringInterface>() {
        @NonNull
        @Override
        public TetheringInterface createFromParcel(@NonNull Parcel in) {
            return new TetheringInterface(in);
        }

        @NonNull
        @Override
        public TetheringInterface[] newArray(int size) {
            return new TetheringInterface[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "TetheringInterface {mType=" + mType
                + ", mInterface=" + mInterface + "}";
    }
}

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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.net.TetheringManager.TetheringType;
import android.net.wifi.SoftApConfiguration;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.net.flags.Flags;

import java.util.Objects;

/**
 * The mapping of tethering interface and type.
 */
@SuppressLint("UnflaggedApi")
public final class TetheringInterface implements Parcelable {
    private final int mType;
    private final String mInterface;
    @Nullable
    private final SoftApConfiguration mSoftApConfig;

    @SuppressLint("UnflaggedApi")
    public TetheringInterface(@TetheringType int type, @NonNull String iface) {
        this(type, iface, null);
    }

    @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
    public TetheringInterface(@TetheringType int type, @NonNull String iface,
            @Nullable SoftApConfiguration softApConfig) {
        Objects.requireNonNull(iface);
        mType = type;
        mInterface = iface;
        mSoftApConfig = softApConfig;
    }

    /** Get tethering type. */
    @SuppressLint("UnflaggedApi")
    public int getType() {
        return mType;
    }

    /** Get tethering interface. */
    @NonNull
    @SuppressLint("UnflaggedApi")
    public String getInterface() {
        return mInterface;
    }

    /**
     * Get the SoftApConfiguration provided for this interface, if any. This will only be populated
     * for apps with the same uid that specified the configuration, or apps with permission
     * {@link android.Manifest.permission.NETWORK_SETTINGS}.
     */
    @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
    @RequiresPermission(value = android.Manifest.permission.NETWORK_SETTINGS, conditional = true)
    @Nullable
    @SuppressLint("UnflaggedApi")
    public SoftApConfiguration getSoftApConfiguration() {
        return mSoftApConfig;
    }

    @Override
    @SuppressLint("UnflaggedApi")
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeString(mInterface);
        dest.writeParcelable(mSoftApConfig, flags);
    }

    @Override
    @SuppressLint("UnflaggedApi")
    public int hashCode() {
        return Objects.hash(mType, mInterface, mSoftApConfig);
    }

    @Override
    @SuppressLint("UnflaggedApi")
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof TetheringInterface)) return false;
        final TetheringInterface other = (TetheringInterface) obj;
        return mType == other.mType && mInterface.equals(other.mInterface)
                && Objects.equals(mSoftApConfig, other.mSoftApConfig);
    }

    @Override
    @SuppressLint("UnflaggedApi")
    public int describeContents() {
        return 0;
    }

    @NonNull
    @SuppressLint("UnflaggedApi")
    public static final Creator<TetheringInterface> CREATOR = new Creator<TetheringInterface>() {
        @NonNull
        @Override
        @SuppressLint("UnflaggedApi")
        public TetheringInterface createFromParcel(@NonNull Parcel in) {
            return new TetheringInterface(in.readInt(), in.readString(),
                    in.readParcelable(SoftApConfiguration.class.getClassLoader()));
        }

        @NonNull
        @Override
        @SuppressLint("UnflaggedApi")
        public TetheringInterface[] newArray(int size) {
            return new TetheringInterface[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "TetheringInterface {mType=" + mType
                + ", mInterface=" + mInterface
                + ((mSoftApConfig == null) ? "" : ", mSoftApConfig=" + mSoftApConfig)
                + "}";
    }
}

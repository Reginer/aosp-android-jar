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

package android.net.wifi;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.wifi.util.PersistableBundleUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import com.android.wifi.flags.Flags;

import java.util.Objects;

/**
 * Vendor-provided data for HAL configuration.
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
@SystemApi
public final class OuiKeyedData implements Parcelable {
    private static final String TAG = "OuiKeyedData";

    /** 24-bit OUI identifier to identify the vendor/OEM. */
    private final int mOui;
    /** PersistableBundle containing the vendor-defined data. */
    private final PersistableBundle mData;

    private OuiKeyedData(int oui, @NonNull PersistableBundle data) {
        mOui = oui;
        mData = (data != null) ? data.deepCopy() : null;
    }

    /**
     * Get the OUI for this object.
     *
     * <p>See {@link Builder#Builder(int, PersistableBundle)}}
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int getOui() {
        return mOui;
    }

    /**
     * Get the data for this object.
     *
     * <p>See {@link Builder#Builder(int, PersistableBundle)}}
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public @NonNull PersistableBundle getData() {
        return mData;
    }

    private static boolean validateOui(int oui) {
        // OUI must be a non-zero 24-bit value.
        return oui != 0 && (oui & 0xFF000000) == 0;
    }

    /**
     * Validate the parameters in this instance.
     *
     * @return true if all parameters are valid, false otherwise
     * @hide
     */
    public boolean validate() {
        return validateOui(mOui) && (getData() != null);
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OuiKeyedData that = (OuiKeyedData) o;
        return mOui == that.mOui && PersistableBundleUtils.isEqual(mData, that.mData);
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public int hashCode() {
        return Objects.hash(mOui, PersistableBundleUtils.getHashCode(mData));
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public String toString() {
        return "{oui=" + Integer.toHexString(mOui) + ", data=" + getData() + "}";
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public int describeContents() {
        return 0;
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mOui);
        dest.writePersistableBundle(mData);
    }

    /** @hide */
    OuiKeyedData(@NonNull Parcel in) {
        this.mOui = in.readInt();
        this.mData = in.readPersistableBundle();
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @NonNull
    public static final Parcelable.Creator<OuiKeyedData> CREATOR =
            new Parcelable.Creator<OuiKeyedData>() {
                @Override
                public OuiKeyedData createFromParcel(Parcel in) {
                    return new OuiKeyedData(in);
                }

                @Override
                public OuiKeyedData[] newArray(int size) {
                    return new OuiKeyedData[size];
                }
            };

    /** Builder for {@link OuiKeyedData}. */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final class Builder {
        private final int mOui;
        private final @NonNull PersistableBundle mData;

        /**
         * Constructor for {@link Builder}.
         *
         * @param oui 24-bit OUI identifier to identify the vendor/OEM. See
         *     https://standards-oui.ieee.org/ for more information.
         * @param data PersistableBundle containing additional configuration data. The definition
         *     should be provided by the vendor, and should be known to both the caller and to the
         *     vendor's implementation of the Wi-Fi HALs.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public Builder(int oui, @NonNull PersistableBundle data) {
            mOui = oui;
            mData = data;
        }

        /** Construct an OuiKeyedData object with the specified parameters. */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public OuiKeyedData build() {
            OuiKeyedData ouiKeyedData = new OuiKeyedData(mOui, mData);
            if (!ouiKeyedData.validate()) {
                throw new IllegalArgumentException("Provided parameters are invalid");
            }
            return ouiKeyedData;
        }
    }
}

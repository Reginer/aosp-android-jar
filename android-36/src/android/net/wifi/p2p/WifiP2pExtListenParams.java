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

package android.net.wifi.p2p;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.annotation.SystemApi;
import android.net.wifi.OuiKeyedData;
import android.net.wifi.ParcelUtil;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class representing Wi-Fi P2P extended listen parameters.
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
public final class WifiP2pExtListenParams implements Parcelable {
    /** List of {@link OuiKeyedData} providing vendor-specific configuration data. */
    private @NonNull List<OuiKeyedData> mVendorData;

    private WifiP2pExtListenParams(@NonNull List<OuiKeyedData> vendorData) {
        mVendorData = new ArrayList<>(vendorData);
    }

    /**
     * Get the vendor-provided configuration data, if it exists. See {@link
     * Builder#setVendorData(List)}
     *
     * @return Vendor configuration data, or empty list if it does not exist.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @NonNull
    public List<OuiKeyedData> getVendorData() {
        if (!SdkLevel.isAtLeastV()) {
            throw new UnsupportedOperationException();
        }
        return mVendorData;
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("\n vendorData: ").append(mVendorData);
        return sbuf.toString();
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public int describeContents() {
        return 0;
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeList(mVendorData);
    }

    /** @hide */
    WifiP2pExtListenParams(@NonNull Parcel in) {
        this.mVendorData = ParcelUtil.readOuiKeyedDataList(in);
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @NonNull
    public static final Creator<WifiP2pExtListenParams> CREATOR =
            new Creator<WifiP2pExtListenParams>() {
                public WifiP2pExtListenParams createFromParcel(Parcel in) {
                    return new WifiP2pExtListenParams(in);
                }

                public WifiP2pExtListenParams[] newArray(int size) {
                    return new WifiP2pExtListenParams[size];
                }
            };

    /**
     * Builder for {@link WifiP2pExtListenParams}.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final class Builder {
        private @NonNull List<OuiKeyedData> mVendorData = Collections.emptyList();

        /**
         * Constructor for {@link Builder}.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public Builder() {
        }

        /**
         * Set additional vendor-provided configuration data.
         *
         * @param vendorData List of {@link android.net.wifi.OuiKeyedData} containing the
         *                   vendor-provided configuration data. Note that multiple elements with
         *                   the same OUI are allowed.
         * @return Builder for chaining.
         */
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setVendorData(@NonNull List<OuiKeyedData> vendorData) {
            if (!SdkLevel.isAtLeastV()) {
                throw new UnsupportedOperationException();
            }
            if (vendorData == null) {
                throw new IllegalArgumentException("setVendorData received a null value");
            }
            mVendorData = vendorData;
            return this;
        }

        /**
         * Construct a WifiP2pExtListenParams object with the specified parameters.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public WifiP2pExtListenParams build() {
            return new WifiP2pExtListenParams(mVendorData);
        }
    }
}

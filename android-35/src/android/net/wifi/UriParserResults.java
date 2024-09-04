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

package android.net.wifi;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.wifi.flags.Flags;

import java.util.Objects;

/**
 * Contains information extracted from URI
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
@SystemApi
public final class UriParserResults implements Parcelable {

    /**
     * Return value for {@link #getUriScheme()} indicating that the URI contains
     * a ZXing WiFi configuration.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int URI_SCHEME_ZXING_WIFI_NETWORK_CONFIG = 1;

    /**
     * Return value for {@link #getUriScheme()} indicating that the URI contains
     * a DPP (Easy Connect) configuration.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int URI_SCHEME_DPP = 2;

    /**
     * URI_SCHEME_DPP for standard Wi-Fi device provision protocol;
     * URI_SCHEME_ZXING_WIFI_NETWORK_CONFIG for ZXing reader library's Wi-Fi Network config format.
     */
    @WifiAnnotations.UriScheme private int mScheme;

    /** Public key from parsed Wi-Fi DPP URI, it is valid when mScheme is URI_SCHEME_DPP. */
    @Nullable private String mPublicKey;

    /**
     * Optional device specific information from the Wi-Fi DPP URI,
     * it is valid when mScheme is URI_SCHEME_DPP
     */
    @Nullable private String mInformation;

    /**
     * WifiConfiguration from parsed ZXing reader library's Wi-Fi Network config format. Valid or
     * Not null when mScheme is URI_SCHEME_ZXING_WIFI_NETWORK_CONFIG
     */
    @Nullable private WifiConfiguration mWifiConfig;

    /** @hide */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public UriParserResults(
            int scheme,
            @Nullable String publicKey,
            @Nullable String information,
            @Nullable WifiConfiguration config) {
        mScheme = scheme;
        mPublicKey = publicKey;
        mInformation = information;
        if (config != null) {
            mWifiConfig = new WifiConfiguration(config);
        }
    }

    /**
     * The scheme described by the URI.
     *
     * <p>URI_SCHEME_DPP for standard Wi-Fi device provision protocol.
     * URI_SCHEME_ZXING_WIFI_NETWORK_CONFIG for ZXing reader library's Wi-Fi Network config format.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @WifiAnnotations.UriScheme
    public int getUriScheme() {
        return mScheme;
    }

    /**
     * The public key of the DPP (Wi-Fi Easy Connect).
     *
     * If {@code getUriScheme()} returns URI_SCHEME_DPP, this field contains the public key
     * of the DPP (Wi-Fi Easy Connect). Otherwise, it is null.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Nullable
    public String getPublicKey() {
        return mPublicKey;
    }

    /**
     * The information of the DPP (Wi-Fi Easy Connect).
     *
     * If {@code getUriScheme()} returns URI_SCHEME_DPP, this field contains the information
     * of the DPP (Wi-Fi Easy Connect). Otherwise, it is null.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Nullable
    public String getInformation() {
        return mInformation;
    }

    /**
     * The WifiConfiguration of the zxing wifi network.
     *
     * If {@code getUriScheme()} returns URI_SCHEME_ZXING_WIFI_NETWORK_CONFIG, this field contains
     * the WifiConfiguration of the zxing wifi network. Otherwise, it is null.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Nullable
    public WifiConfiguration getWifiConfiguration() {
        return mWifiConfig;
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    /**
     * Implement the Parcelable interface.
     */
    public int describeContents() {
        return 0;
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    /**
     * Implement the Parcelable interface.
     */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mScheme);
        dest.writeString(mPublicKey);
        dest.writeString(mInformation);
        dest.writeParcelable(mWifiConfig, flags);
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @NonNull
    /**
     * Implement the Parcelable interface.
     */
    public static final Creator<UriParserResults> CREATOR =
            new Creator<UriParserResults>() {
                public UriParserResults createFromParcel(Parcel in) {
                    return new UriParserResults(
                            in.readInt(),
                            in.readString(),
                            in.readString(),
                            in.readParcelable(WifiConfiguration.class.getClassLoader()));
                }

                public UriParserResults[] newArray(int size) {
                    return new UriParserResults[size];
                }
            };

    @NonNull
    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("UriParserResults{");
        sbuf.append(", mScheme= ").append(mScheme);
        sbuf.append(", mPublicKey= ").append(mPublicKey);
        sbuf.append(", mInformation= ").append(mInformation);
        if (mWifiConfig != null) sbuf.append(", mWifiConfig=").append(mWifiConfig.toString());
        sbuf.append("}");
        return sbuf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UriParserResults)) return false;
        UriParserResults results = (UriParserResults) o;
        return mScheme == results.mScheme
                && Objects.equals(mPublicKey, results.mPublicKey)
                && Objects.equals(mInformation, results.mInformation)
                && Objects.equals(
                        mWifiConfig != null ? mWifiConfig.toString() : null,
                        results.mWifiConfig != null ? results.mWifiConfig.toString() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mScheme,
                mPublicKey,
                mInformation,
                mWifiConfig != null ? mWifiConfig.toString() : null);
    }
}

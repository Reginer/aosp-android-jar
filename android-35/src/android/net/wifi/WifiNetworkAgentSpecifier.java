/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.internal.util.Preconditions.checkNotNull;
import static com.android.internal.util.Preconditions.checkState;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.MacAddress;
import android.net.MatchAllNetworkSpecifier;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.ScanResult.WifiBand;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Network specifier object used by wifi's {@link android.net.NetworkAgent}.
 * @hide
 */
public final class WifiNetworkAgentSpecifier extends NetworkSpecifier implements Parcelable {
    /**
     * Security credentials for the currently connected network.
     */
    private final WifiConfiguration mWifiConfiguration;

    /**
     * Band, as one of the ScanResult.WIFI_BAND_* constants.
     */
    @WifiBand private final int mBand;

    /**
     * Whether to match P2P requests.
     *
     * When matching against an instance of WifiNetworkSpecifier, simply matching SSID or
     * BSSID patterns would let apps know if a given WiFi network's (B)SSID matches a pattern
     * by simply filing a NetworkCallback with that pattern. Also, apps asking for a match on
     * (B)SSID are apps requesting a P2P network, which involves protection with UI shown by
     * the system, and the WifiNetworkSpecifiers for these P2P networks should never match
     * an Internet-providing network to avoid calling back these apps on a network that happens
     * to match their requested pattern but has not been brought up for them.
     */
    private final boolean mMatchLocalOnlySpecifiers;

    public WifiNetworkAgentSpecifier(@NonNull WifiConfiguration wifiConfiguration,
            @WifiBand int band, boolean matchLocalOnlySpecifiers) {
        checkNotNull(wifiConfiguration);

        mWifiConfiguration = wifiConfiguration;
        mBand = band;
        mMatchLocalOnlySpecifiers = matchLocalOnlySpecifiers;
    }

    /**
     * @hide
     */
    public static final @android.annotation.NonNull Creator<WifiNetworkAgentSpecifier> CREATOR =
            new Creator<WifiNetworkAgentSpecifier>() {
                @Override
                public WifiNetworkAgentSpecifier createFromParcel(@NonNull Parcel in) {
                    WifiConfiguration wifiConfiguration = in.readParcelable(null);
                    int band = in.readInt();
                    boolean matchLocalOnlySpecifiers = in.readBoolean();
                    return new WifiNetworkAgentSpecifier(wifiConfiguration, band,
                            matchLocalOnlySpecifiers);
                }

                @Override
                public WifiNetworkAgentSpecifier[] newArray(int size) {
                    return new WifiNetworkAgentSpecifier[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mWifiConfiguration, flags);
        dest.writeInt(mBand);
        dest.writeBoolean(mMatchLocalOnlySpecifiers);
    }

    @Override
    public boolean canBeSatisfiedBy(@Nullable NetworkSpecifier other) {
        if (this == other) {
            return true;
        }
        // Any generic requests should be satisifed by a specific wifi network.
        if (other == null || other instanceof MatchAllNetworkSpecifier) {
            return true;
        }
        if (other instanceof WifiNetworkSpecifier) {
            return satisfiesNetworkSpecifier((WifiNetworkSpecifier) other);
        }
        return equals(other);
    }

    /**
     * Match {@link WifiNetworkSpecifier} in app's {@link NetworkRequest} with the
     * {@link WifiNetworkAgentSpecifier} in wifi platform's {@link android.net.NetworkAgent}.
     */
    public boolean satisfiesNetworkSpecifier(@NonNull WifiNetworkSpecifier ns) {
        // None of these should be null by construction.
        // {@link WifiNetworkSpecifier.Builder} enforces non-null in {@link WifiNetworkSpecifier}.
        // {@link WifiNetworkFactory} ensures non-null in {@link WifiNetworkAgentSpecifier}.
        checkNotNull(ns);
        checkNotNull(ns.ssidPatternMatcher);
        checkNotNull(ns.bssidPatternMatcher);
        checkNotNull(ns.wifiConfiguration.allowedKeyManagement);
        checkNotNull(this.mWifiConfiguration.SSID);
        checkNotNull(this.mWifiConfiguration.BSSID);
        checkNotNull(this.mWifiConfiguration.allowedKeyManagement);

        if (!mMatchLocalOnlySpecifiers) {
            // Specifiers that match non-local-only networks only match against the band.
            if (mBand == ScanResult.WIFI_BAND_5_GHZ_LOW) {
                return ns.getBand() == ScanResult.WIFI_BAND_5_GHZ_LOW
                        || ns.getBand() == ScanResult.WIFI_BAND_5_GHZ;
            } else if (mBand == ScanResult.WIFI_BAND_5_GHZ_HIGH) {
                return ns.getBand() == ScanResult.WIFI_BAND_5_GHZ_HIGH
                        || ns.getBand() == ScanResult.WIFI_BAND_5_GHZ;
            } else {
                return ns.getBand() == mBand;
            }
        }
        if (ns.getBand() != ScanResult.UNSPECIFIED && ns.getBand() != mBand) {
            return false;
        }
        final String ssidWithQuotes = this.mWifiConfiguration.SSID;
        checkState(ssidWithQuotes.startsWith("\"") && ssidWithQuotes.endsWith("\""));
        final String ssidWithoutQuotes = ssidWithQuotes.substring(1, ssidWithQuotes.length() - 1);
        if (!ns.ssidPatternMatcher.match(ssidWithoutQuotes)) {
            return false;
        }
        final MacAddress bssid = MacAddress.fromString(this.mWifiConfiguration.BSSID);
        final MacAddress matchBaseAddress = ns.bssidPatternMatcher.first;
        final MacAddress matchMask = ns.bssidPatternMatcher.second;
        if (!bssid.matches(matchBaseAddress, matchMask))  {
            return false;
        }
        if (!ns.wifiConfiguration.allowedKeyManagement.equals(
                this.mWifiConfiguration.allowedKeyManagement)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mWifiConfiguration.SSID,
                mWifiConfiguration.BSSID,
                mWifiConfiguration.allowedKeyManagement,
                mBand,
                mMatchLocalOnlySpecifiers);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiNetworkAgentSpecifier)) {
            return false;
        }
        WifiNetworkAgentSpecifier lhs = (WifiNetworkAgentSpecifier) obj;
        return Objects.equals(this.mWifiConfiguration.SSID, lhs.mWifiConfiguration.SSID)
                && Objects.equals(this.mWifiConfiguration.BSSID, lhs.mWifiConfiguration.BSSID)
                && Objects.equals(this.mWifiConfiguration.allowedKeyManagement,
                    lhs.mWifiConfiguration.allowedKeyManagement)
                && mBand == lhs.mBand
                && mMatchLocalOnlySpecifiers == lhs.mMatchLocalOnlySpecifiers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WifiNetworkAgentSpecifier [");
        sb.append("WifiConfiguration=")
                .append(", SSID=").append(mWifiConfiguration.SSID)
                .append(", BSSID=").append(mWifiConfiguration.BSSID)
                .append(", band=").append(mBand)
                .append(", mMatchLocalOnlySpecifiers=").append(mMatchLocalOnlySpecifiers)
                .append("]");
        return sb.toString();
    }

    @Override
    public NetworkSpecifier redact() {
        return null;
    }
}

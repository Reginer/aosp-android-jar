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
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.wifi.flags.Flags;

import java.util.Arrays;
import java.util.Objects;

/**
 * A class representing a Wi-Fi P2P USD based service discovery configuration for
 * discovering the services.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
public final class WifiP2pUsdBasedServiceDiscoveryConfig implements Parcelable {
    /**
     * Default channel frequency for USD based service discovery.
     */
    private static final int USD_DEFAULT_DISCOVERY_CHANNEL_MHZ = 2437;

    /** One of the WIFI_BAND */
    private @ScanResult.WifiBand int mBand;

    /**
     * Frequencies on which the service needs to be scanned for.
     * Used when band is set to ScanResult.UNSPECIFIED.
     */
    private int[] mFrequenciesMhz;

    private WifiP2pUsdBasedServiceDiscoveryConfig(int band, @NonNull int[] frequencies) {
        mBand = band;
        mFrequenciesMhz = frequencies;
    }

    /**
     * Get the band to scan for services. See {@link Builder#setBand(int)}
     */
    @ScanResult.WifiBand
    public int getBand() {
        return mBand;
    }

    /**
     * Get the frequencies to scan for services. See {@link Builder#setFrequenciesMhz(int[])}
     */
    @Nullable
    public int[] getFrequenciesMhz() {
        return mFrequenciesMhz;
    }

    /**
     * Generates a string of all the defined elements.
     *
     * @return a compiled string representing all elements
     */
    public String toString() {
        StringBuilder sbuf = new StringBuilder("WifiP2pUsdBasedServiceDiscoveryConfig:");
        sbuf.append("\n Band: ").append(mBand);
        sbuf.append("\n Frequencies: ").append((mFrequenciesMhz == null)
                ? "<null>" : Arrays.toString(mFrequenciesMhz));
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mBand);
        dest.writeIntArray(mFrequenciesMhz);
    }

    private WifiP2pUsdBasedServiceDiscoveryConfig(@NonNull Parcel in) {
        this.mBand = in.readInt();
        this.mFrequenciesMhz = in.createIntArray();
    }

    private static boolean isBandValid(@ScanResult.WifiBand int band) {
        int bandAny = ScanResult.WIFI_BAND_24_GHZ | ScanResult.WIFI_BAND_5_GHZ
                | ScanResult.WIFI_BAND_6_GHZ;
        return ((band != 0) && ((band & ~bandAny) == 0));
    }

    /** Implement the Parcelable interface */
    @NonNull
    public static final Creator<WifiP2pUsdBasedServiceDiscoveryConfig> CREATOR =
            new Creator<WifiP2pUsdBasedServiceDiscoveryConfig>() {
                public WifiP2pUsdBasedServiceDiscoveryConfig createFromParcel(Parcel in) {
                    return new WifiP2pUsdBasedServiceDiscoveryConfig(in);
                }

                public WifiP2pUsdBasedServiceDiscoveryConfig[] newArray(int size) {
                    return new WifiP2pUsdBasedServiceDiscoveryConfig[size];
                }
            };

    /**
     * Builder for {@link WifiP2pUsdBasedServiceDiscoveryConfig}.
     */
    public static final class Builder {
        /** Maximum allowed number of channel frequencies */
        private static final int MAXIMUM_CHANNEL_FREQUENCIES = 48;
        private int mBand;
        private int[] mFrequenciesMhz;

        /**
         * Constructs a Builder with default values.
         */
        public Builder() {
            mBand = ScanResult.UNSPECIFIED;
            mFrequenciesMhz = new int[] {USD_DEFAULT_DISCOVERY_CHANNEL_MHZ};
        }

        /**
         * Specifies the band requested for service discovery. The band should
         * be one of the following band constants defined in {@code ScanResult#WIFI_BAND_24_GHZ},
         * {@code ScanResult#WIFI_BAND_5_GHZ} or {@code ScanResult#WIFI_BAND_6_GHZ}
         *
         * <p>
         *     {@link #setBand(int)} and {@link #setFrequenciesMhz(int[])} are
         *     mutually exclusive. Setting operating band and frequency both is invalid.
         * <p>
         *     Optional. {@code ScanResult#UNSPECIFIED} by default.
         *
         * @param band The requested band.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         *
         * @throws IllegalArgumentException - if the band specified is not one among the list
         *         of bands mentioned above.
         */
        public @NonNull Builder setBand(int band) {
            if (!isBandValid(band)) {
                throw new IllegalArgumentException("Invalid band: " + band);
            }
            mBand = band;
            return this;
        }

        /**
         * Set the frequencies requested for service discovery.
         *
         * <p>
         *     {@link #setBand(int)} and {@link #setFrequenciesMhz(int[])} are
         *     mutually exclusive. Setting band and frequencies both is invalid.
         * <p>
         *     Optional. 2437 by default.
         * @param frequenciesMhz Frequencies in MHz to scan for services. This value cannot be an
         *                       empty array of frequencies.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        @NonNull
        public Builder setFrequenciesMhz(@NonNull int[] frequenciesMhz) {
            Objects.requireNonNull(frequenciesMhz, "Frequencies cannot be null");
            if (frequenciesMhz.length < 1 || frequenciesMhz.length > MAXIMUM_CHANNEL_FREQUENCIES) {
                throw new IllegalArgumentException("Number of frequencies: "
                        + frequenciesMhz.length
                        + " must be between 1 and " + MAXIMUM_CHANNEL_FREQUENCIES);
            }
            mFrequenciesMhz = frequenciesMhz;
            return this;
        }

        /**
         * Build {@link WifiP2pUsdBasedServiceDiscoveryConfig} given the current requests made
         * on the builder.
         * @return {@link WifiP2pUsdBasedServiceDiscoveryConfig} constructed based on builder
         * method calls.
         */
        @NonNull
        public WifiP2pUsdBasedServiceDiscoveryConfig build() {
            if (mBand != ScanResult.UNSPECIFIED && mFrequenciesMhz != null) {
                throw new IllegalStateException(
                        "Frequencies and band are mutually exclusive.");
            }
            return new WifiP2pUsdBasedServiceDiscoveryConfig(mBand, mFrequenciesMhz);
        }
    }
}

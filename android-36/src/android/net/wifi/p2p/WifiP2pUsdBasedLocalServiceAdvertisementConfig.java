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
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.wifi.flags.Flags;

/**
 * A class representing a Wi-Fi P2P USD based service advertisement configuration for advertising
 * the services.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
public final class WifiP2pUsdBasedLocalServiceAdvertisementConfig implements Parcelable {
    /**
     * Default channel frequency for USD based service discovery.
     */
    private static final int USD_DEFAULT_DISCOVERY_CHANNEL_MHZ = 2437;

    /**
     * Frequency on which the service needs to be advertised.
     */
    private int mFrequencyMhz;

    private WifiP2pUsdBasedLocalServiceAdvertisementConfig(int frequencyMhz) {
        mFrequencyMhz = frequencyMhz;
    }

    /**
     * Get the frequency on which the service is advertised.
     */
    @IntRange(from = 0)
    public int getFrequencyMhz() {
        return mFrequencyMhz;
    }

    /**
     * Generates a string of all the defined elements.
     *
     * @return a compiled string representing all elements
     */
    public String toString() {
        StringBuilder sbuf = new StringBuilder("WifiP2pUsdBasedLocalServiceAdvertisementConfig:");
        sbuf.append("\n Frequency: ").append(mFrequencyMhz);
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mFrequencyMhz);
    }

    /** Implement the Parcelable interface */
    private WifiP2pUsdBasedLocalServiceAdvertisementConfig(@NonNull Parcel in) {
        this.mFrequencyMhz = in.readInt();
    }

    /** Implement the Parcelable interface */
    @NonNull
    public static final Creator<WifiP2pUsdBasedLocalServiceAdvertisementConfig> CREATOR =
            new Creator<WifiP2pUsdBasedLocalServiceAdvertisementConfig>() {
                public WifiP2pUsdBasedLocalServiceAdvertisementConfig createFromParcel(Parcel in) {
                    return new WifiP2pUsdBasedLocalServiceAdvertisementConfig(in);
                }

                public WifiP2pUsdBasedLocalServiceAdvertisementConfig[] newArray(int size) {
                    return new WifiP2pUsdBasedLocalServiceAdvertisementConfig[size];
                }
            };

    /**
     * Builder for {@link WifiP2pUsdBasedLocalServiceAdvertisementConfig}.
     */
    public static final class Builder {
        private int mFrequencyMhz;

        /**
         * Constructs a Builder with default values.
         */
        public Builder() {
            mFrequencyMhz = USD_DEFAULT_DISCOVERY_CHANNEL_MHZ;
        }

        /**
         * Specifies the frequency requested for advertising the service.
         *
         * @param frequencyMhz The requested frequency on which the service needs to be advertised.
         *                     If not set, the default frequency is
         *                     {@link #USD_DEFAULT_DISCOVERY_CHANNEL_MHZ} MHz.
         *
         * @return The builder to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         *
         */
        @NonNull
        public Builder setFrequencyMhz(@IntRange(from = 1) int frequencyMhz) {
            if (frequencyMhz <= 0) {
                throw new IllegalArgumentException("Frequency must be greater than 0");
            }
            mFrequencyMhz = frequencyMhz;
            return this;
        }

        /**
         * Build {@link WifiP2pUsdBasedLocalServiceAdvertisementConfig} given the
         * current requests made on the builder.
         * @return {@link WifiP2pUsdBasedLocalServiceAdvertisementConfig} constructed based on
         * builder method calls.
         */
        @NonNull
        public WifiP2pUsdBasedLocalServiceAdvertisementConfig build() {
            return new WifiP2pUsdBasedLocalServiceAdvertisementConfig(mFrequencyMhz);
        }
    }
}

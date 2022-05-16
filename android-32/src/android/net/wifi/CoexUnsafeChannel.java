/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.net.wifi.WifiScanner.WIFI_BAND_24_GHZ;
import static android.net.wifi.WifiScanner.WIFI_BAND_5_GHZ;
import static android.net.wifi.WifiScanner.WIFI_BAND_6_GHZ;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.util.Objects;

/**
 * Data structure class representing a Wi-Fi channel that would cause interference to/receive
 * interference from the active cellular channels and should be avoided.
 *
 * @hide
 */
@SystemApi
@RequiresApi(Build.VERSION_CODES.S)
public final class CoexUnsafeChannel implements Parcelable {
    public static final int POWER_CAP_NONE = Integer.MAX_VALUE;

    private @WifiAnnotations.WifiBandBasic int mBand;
    private int mChannel;
    private int mPowerCapDbm;

    /**
     * Constructor for a CoexUnsafeChannel with no power cap specified.
     * @param band One of {@link WifiAnnotations.WifiBandBasic}
     * @param channel Channel number
     */
    public CoexUnsafeChannel(@WifiAnnotations.WifiBandBasic int band, int channel) {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        mBand = band;
        mChannel = channel;
        mPowerCapDbm = POWER_CAP_NONE;
    }

    /**
     * Constructor for a CoexUnsafeChannel with power cap specified.
     * @param band One of {@link WifiAnnotations.WifiBandBasic}
     * @param channel Channel number
     * @param powerCapDbm Power cap in dBm
     */
    public CoexUnsafeChannel(@WifiAnnotations.WifiBandBasic int band, int channel,
            int powerCapDbm) {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        mBand = band;
        mChannel = channel;
        mPowerCapDbm = powerCapDbm;
    }

    /** Returns the Wi-Fi band of this channel as one of {@link WifiAnnotations.WifiBandBasic} */
    public @WifiAnnotations.WifiBandBasic int getBand() {
        return mBand;
    }

    /** Returns the channel number of this channel. */
    public int getChannel() {
        return mChannel;
    }

    /**
     * Returns the power cap of this channel in dBm or {@link CoexUnsafeChannel#POWER_CAP_NONE}
     * if the power cap is not specified.
     */
    public int getPowerCapDbm() {
        return mPowerCapDbm;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoexUnsafeChannel that = (CoexUnsafeChannel) o;
        return mBand == that.mBand
                && mChannel == that.mChannel
                && mPowerCapDbm == that.mPowerCapDbm;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBand, mChannel, mPowerCapDbm);
    }

    @Override
    public String toString() {
        StringBuilder sj = new StringBuilder("CoexUnsafeChannel{");
        if (mBand == WIFI_BAND_24_GHZ) {
            sj.append("2.4GHz");
        } else if (mBand == WIFI_BAND_5_GHZ) {
            sj.append("5GHz");
        } else if (mBand == WIFI_BAND_6_GHZ) {
            sj.append("6GHz");
        } else {
            sj.append("UNKNOWN BAND");
        }
        sj.append(", ").append(mChannel);
        if (mPowerCapDbm != POWER_CAP_NONE) {
            sj.append(", ").append(mPowerCapDbm).append("dBm");
        }
        sj.append('}');
        return sj.toString();
    }

    /** Implement the Parcelable interface {@hide} */
    @Override
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface {@hide} */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mBand);
        dest.writeInt(mChannel);
        dest.writeInt(mPowerCapDbm);
    }

    /** Implement the Parcelable interface */
    public static final @NonNull Creator<CoexUnsafeChannel> CREATOR =
            new Creator<CoexUnsafeChannel>() {
                public CoexUnsafeChannel createFromParcel(Parcel in) {
                    final int band = in.readInt();
                    final int channel = in.readInt();
                    final int powerCapDbm = in.readInt();
                    return new CoexUnsafeChannel(band, channel, powerCapDbm);
                }

                public CoexUnsafeChannel[] newArray(int size) {
                    return new CoexUnsafeChannel[size];
                }
            };
}

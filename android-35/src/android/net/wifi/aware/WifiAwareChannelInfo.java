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

package android.net.wifi.aware;

import android.annotation.IntRange;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiAnnotations;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Wi-Fi Aware data-path channel information. The information can be extracted from the
 * {@link WifiAwareNetworkInfo} using {@link WifiAwareNetworkInfo#getChannelInfoList()} ()}.
 * Wi-Fi Aware data-path channel information includes the channel frequency, bandwidth and num of
 * the spatial streams.
 */
public final class WifiAwareChannelInfo implements Parcelable {
    private final int mChannelFreq;
    private final int mChannelBandwidth;
    private final int mNumSpatialStreams;

    /** @hide **/
    public WifiAwareChannelInfo(int channelFreq, int channelBandwidth, int numSpatialStreams) {
        mChannelFreq = channelFreq;
        mChannelBandwidth = channelBandwidth;
        mNumSpatialStreams = numSpatialStreams;
    }

    /**
     * Get the channel frequency in MHZ used by the Aware data path
     * @return An integer represent the frequency of the channel in MHZ.
     */
    @IntRange(from = 0)
    public int getChannelFrequencyMhz() {
        return mChannelFreq;
    }

    /**
     * Get the channel bandwidth used by the Aware data path
     * @return one of {@link ScanResult#CHANNEL_WIDTH_20MHZ},
     * {@link ScanResult#CHANNEL_WIDTH_40MHZ},
     * {@link ScanResult#CHANNEL_WIDTH_80MHZ}, {@link ScanResult#CHANNEL_WIDTH_160MHZ}
     * or {@link ScanResult #CHANNEL_WIDTH_80MHZ_PLUS_MHZ}.
     */
    @IntRange(from = 0)
    public @WifiAnnotations.ChannelWidth int getChannelBandwidth() {
        return mChannelBandwidth;
    }

    /**
     * Get the number of the spatial streams used by the Aware data path
     * @return An integer represent number of the spatial streams are using in this channel.
     */
    @IntRange(from = 0)
    public int getSpatialStreamCount() {
        return mNumSpatialStreams;
    }

    private WifiAwareChannelInfo(Parcel in) {
        mChannelFreq = in.readInt();
        mChannelBandwidth = in.readInt();
        mNumSpatialStreams = in.readInt();
    }

    public static final @NonNull Creator<WifiAwareChannelInfo> CREATOR =
            new Creator<WifiAwareChannelInfo>() {
                @Override
                public WifiAwareChannelInfo createFromParcel(Parcel in) {
                    return new WifiAwareChannelInfo(in);
                }

                @Override
                public WifiAwareChannelInfo[] newArray(int size) {
                    return new WifiAwareChannelInfo[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mChannelFreq);
        dest.writeInt(mChannelBandwidth);
        dest.writeInt(mNumSpatialStreams);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("{")
                .append(".channelFreq = ").append(mChannelFreq)
                .append(", .channelBandwidth = ").append(mChannelBandwidth)
                .append(", .numSpatialStreams = ").append(mNumSpatialStreams)
                .append("}");
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof WifiAwareChannelInfo)) {
            return false;
        }

        WifiAwareChannelInfo lhs = (WifiAwareChannelInfo) obj;
        return  mChannelFreq == lhs.mChannelFreq
                && mChannelBandwidth == lhs.mChannelBandwidth
                && mNumSpatialStreams == lhs.mNumSpatialStreams;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mChannelFreq, mChannelBandwidth, mNumSpatialStreams);
    }
}

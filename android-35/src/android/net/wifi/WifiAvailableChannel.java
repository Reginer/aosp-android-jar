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
package android.net.wifi;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Contains information about a Wifi channel and bitmask of Wifi operational modes allowed on that
 * channel. Use {@link WifiManager#getAllowedChannels(int, int)} to retrieve the list of channels
 * filtered by regulatory constraints. Use {@link WifiManager#getUsableChannels(int, int)} to
 * retrieve the list of channels filtered by regulatory and dynamic constraints like concurrency and
 * interference due to other radios.
 */
public final class WifiAvailableChannel implements Parcelable {

    /**
     * Wifi Infrastructure client (STA) operational mode.
     */
    public static final int OP_MODE_STA = 1 << 0;

    /**
     * Wifi SoftAp (Mobile Hotspot) operational mode.
     */
    public static final int OP_MODE_SAP = 1 << 1;

    /**
     * Wifi Direct client (CLI) operational mode.
     */
    public static final int OP_MODE_WIFI_DIRECT_CLI = 1 << 2;

    /**
     * Wifi Direct Group Owner (GO) operational mode.
     */
    public static final int OP_MODE_WIFI_DIRECT_GO = 1 << 3;

    /**
     * Wifi Aware (NAN) operational mode.
     */
    public static final int OP_MODE_WIFI_AWARE = 1 << 4;

    /**
     * Wifi Tunneled Direct Link Setup (TDLS) operational mode.
     */
    public static final int OP_MODE_TDLS = 1 << 5;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, prefix = {"OP_MODE_"}, value = {
            OP_MODE_STA,
            OP_MODE_SAP,
            OP_MODE_WIFI_DIRECT_CLI,
            OP_MODE_WIFI_DIRECT_GO,
            OP_MODE_WIFI_AWARE,
            OP_MODE_TDLS,
    })
    public @interface OpMode {}

    /**
     * Filter channel based on regulatory constraints.
     * @hide
     */
    public static final int FILTER_REGULATORY = 0;

    /**
     * Filter channel based on interference from cellular radio.
     * @hide
     */
    public static final int FILTER_CELLULAR_COEXISTENCE = 1 << 0;

    /**
     * Filter channel based on current concurrency state.
     * @hide
     */
    public static final int FILTER_CONCURRENCY = 1 << 1;

    /**
     * Filter channel for the Wi-Fi Aware instant communication mode.
     * @hide
     */
    public static final int FILTER_NAN_INSTANT_MODE = 1 << 2;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, prefix = {"FILTER_"}, value = {
            FILTER_REGULATORY,
            FILTER_CELLULAR_COEXISTENCE,
            FILTER_CONCURRENCY,
            FILTER_NAN_INSTANT_MODE,
    })
    public @interface Filter {}

    /**
     * Wifi channel frequency in MHz.
     */
    private int mFrequency;

    /**
     * Bitwise OR of modes (OP_MODE_*) allowed on this channel.
     */
    private @OpMode int mOpModes;

    public WifiAvailableChannel(int freq, @OpMode int opModes) {
        mFrequency = freq;
        mOpModes = opModes;
    }

    private WifiAvailableChannel(@NonNull Parcel in) {
        readFromParcel(in);
    }

    private void readFromParcel(@NonNull Parcel in) {
        mFrequency = in.readInt();
        mOpModes = in.readInt();
    }

    /**
     * Get the channel frequency in MHz.
     */
    public int getFrequencyMhz() {
        return mFrequency;
    }

    /**
     * Get the operational modes allowed on a channel.
     */
    public @OpMode int getOperationalModes() {
        return mOpModes;
    }

    /**
     * Usable filter implies filter channels by regulatory constraints and
     * other dynamic constraints like concurrency state and interference due
     * to other radios like cellular.
     * @hide
     */
    public static @Filter int getUsableFilter() {
        return FILTER_REGULATORY
                | FILTER_CONCURRENCY
                | FILTER_CELLULAR_COEXISTENCE;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WifiAvailableChannel that = (WifiAvailableChannel) o;
        return mFrequency == that.mFrequency
                && mOpModes == that.mOpModes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mFrequency, mOpModes);
    }

    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("mFrequency = ")
            .append(mFrequency)
            .append(", mOpModes = ")
            .append(String.format("%x", mOpModes));
        return sbuf.toString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mFrequency);
        dest.writeInt(mOpModes);
    }

    public static final @android.annotation.NonNull Creator<WifiAvailableChannel> CREATOR =
            new Creator<WifiAvailableChannel>() {
                @Override
                public WifiAvailableChannel createFromParcel(@NonNull Parcel in) {
                    return new WifiAvailableChannel(in);
                }

                @Override
                public WifiAvailableChannel[] newArray(int size) {
                    return new WifiAvailableChannel[size];
                }
            };
}

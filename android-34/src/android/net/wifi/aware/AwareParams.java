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
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * An object to use with {@link WifiAwareManager#setAwareParams(AwareParams)} specifying
 * configuration of the Wi-Fi Aware protocol implementation.
 * @hide
 */
@SystemApi
public final class AwareParams implements Parcelable {

    /**
     *  An integer representing the parameter is never set.
     */
    public static final int UNSET_PARAMETER = -1;

    private int mDw24Ghz = UNSET_PARAMETER;
    private int mDw5Ghz = UNSET_PARAMETER;
    private int mDw6Ghz = UNSET_PARAMETER;
    private int mDiscoveryBeaconIntervalMs = UNSET_PARAMETER;
    private int mNumSpatialStreamsInDiscovery = UNSET_PARAMETER;
    private boolean mIsDwEarlyTerminationEnabled = false;
    private int mMacRandomIntervalSec = UNSET_PARAMETER;

    /**
     * Construct an empty {@link AwareParams}.
     */
    public AwareParams() {
    }

    private AwareParams(Parcel in) {
        mDw24Ghz = in.readInt();
        mDw5Ghz = in.readInt();
        mDw6Ghz = in.readInt();
        mDiscoveryBeaconIntervalMs = in.readInt();
        mNumSpatialStreamsInDiscovery = in.readInt();
        mIsDwEarlyTerminationEnabled = in.readBoolean();
        mMacRandomIntervalSec = in.readInt();
    }

    public static final @NonNull Creator<AwareParams> CREATOR = new Creator<AwareParams>() {
        @Override
        public AwareParams createFromParcel(Parcel in) {
            return new AwareParams(in);
        }

        @Override
        public AwareParams[] newArray(int size) {
            return new AwareParams[size];
        }
    };

    /**
     * Specifies the discovery window (DW) interval for Sync beacons and SDF frames for 2.4Ghz.
     * Defined as per Wi-Fi Alliance (WFA) Wi-Fi Aware specifications version 3.1 Section 4.1.1.1
     * Valid values of DW Interval are: 1, 2, 3, 4 and 5 corresponding to waking up every  1, 2, 4,
     * 8, and 16 DWs.
     * @param dw A positive number specifying the discovery window (DW) interval
     */
    public void setDiscoveryWindowWakeInterval24Ghz(@IntRange(from = 1, to = 5) int dw) {
        if (dw > 5 || dw < 1) {
            throw new IllegalArgumentException("DW value for 2.4Ghz must be 1 to 5");
        }
        mDw24Ghz = dw;
    }

    /**
     * Specifies the discovery window (DW) interval for Sync beacons and SDF frames for 5Ghz.
     * Defined as per Wi-Fi Alliance (WFA) Wi-Fi Aware specifications version 3.1 Section 4.1.1.1
     * <ul>
     *     <li>0: indicating no discovery in the 5GHz band</li>
     * <li>1, 2, 3, 4, or 5: corresponding to waking up every 1, 2, 4, 8, and 16 DWs.</li>
     * </ul>
     * @param dw An integer specifying the discovery window (DW) interval
     */
    public void setDiscoveryWindowWakeInterval5Ghz(@IntRange(from = 0, to = 5) int dw) {
        if (dw > 5 || dw < 0) {
            throw new IllegalArgumentException("DW value for 5Ghz must be 0 to 5");
        }
        mDw5Ghz = dw;
    }

    /**
     * Set the discovery windows (DW) for 6Ghz reserved.
     * @param dw An integer specifying the discovery window (DW) interval
     * @hide
     */
    public void setDiscoveryWindow6Ghz(@IntRange(from = 0) int dw) {
        mDw6Ghz = dw;
    }

    /**
     * Specify the Discovery Beacon interval in ms. Specification only applicable if the device
     * transmits Discovery Beacons (based on the Wi-Fi Aware protocol selection criteria). The value
     * can be increased to reduce power consumption (on devices which would transmit Discovery
     * Beacons), however - cluster synchronization time will likely increase.
     * @param intervalInMs An integer specifying the interval in millisecond
     */
    public void setDiscoveryBeaconIntervalMillis(@IntRange(from = 1) int intervalInMs) {
        if (intervalInMs < 1) {
            throw new IllegalArgumentException("Discovery Beacon interval must >= 1");
        }
        mDiscoveryBeaconIntervalMs = intervalInMs;
    }

    /**
     * The number of spatial streams to be used for transmitting Wi-Fi Aware management frames (does
     * NOT apply to data-path packets). A small value may reduce power consumption for small
     * discovery packets.
     * @param spatialStreamsNum A positive number specifying the number of spatial streams
     */
    public void setNumSpatialStreamsInDiscovery(@IntRange(from = 1) int spatialStreamsNum) {
        if (spatialStreamsNum < 1) {
            throw new IllegalArgumentException("Number Spatial streams must >= 1");
        }
        mNumSpatialStreamsInDiscovery = spatialStreamsNum;
    }

    /**
     * Specifies the interval in seconds that the Wi-Fi Aware management interface MAC address is
     * re-randomized.
     * @param intervalSec A positive number indicating the interval for the MAC address to
     *                    re-randomize, must not exceed 1800 second (30 mins).
     */
    public void setMacRandomizationIntervalSeconds(@IntRange(from = 1, to = 1800) int intervalSec) {
        if (intervalSec > 1800 || intervalSec < 1) {
            throw new IllegalArgumentException("Mac Randomization Interval must be between 1 to "
                    + "1800 seconds");
        }
        mMacRandomIntervalSec = intervalSec;
    }

    /**
     * Controls whether the device may terminate listening on a Discovery Window (DW) earlier than
     * the DW termination (16ms) if no information is received. Enabling the feature will result in
     * lower power consumption, but may result in some missed messages and hence increased latency.
     *
     * @param enable true to enable, false otherwise
     */
    public void setDwEarlyTerminationEnabled(boolean enable) {
        mIsDwEarlyTerminationEnabled = enable;
    }

    /**
     * Get the discovery window (DW) interval for 2.4Ghz.
     * @see #setDiscoveryWindowWakeInterval24Ghz(int)
     * @return an integer represents discovery window interval, {@link #UNSET_PARAMETER} represent
     * this parameter is not set
     */
    public int getDiscoveryWindowWakeInterval24Ghz() {
        return mDw24Ghz;
    }

    /**
     * Get the discovery window (DW) interval for 5Ghz.
     * @see #setDiscoveryWindowWakeInterval5Ghz(int)
     * @return an integer represents discovery window interval, {@link #UNSET_PARAMETER} represent
     * this parameter is not set
     */
    public int getDiscoveryWindowWakeInterval5Ghz() {
        return mDw5Ghz;
    }

    /**
     * Get the discovery window (DW) interval for 6ghz.
     * @see #setDiscoveryWindowWakeInterval5Ghz(int)
     * @return an integer represents discovery window interval, {@link #UNSET_PARAMETER} represent
     * this parameter is not
     * set
     * @hide
     */
    public int getDiscoveryWindowWakeInterval6Ghz() {
        return mDw24Ghz;
    }

    /**
     * Get the discovery beacon interval in milliseconds
     * @see #setDiscoveryBeaconIntervalMillis(int)
     * @return an integer represents discovery beacon interval in milliseconds,
     * {@link #UNSET_PARAMETER} represent this parameter is not set
     */
    public int getDiscoveryBeaconIntervalMillis() {
        return mDiscoveryBeaconIntervalMs;
    }

    /**
     * Get the number of the spatial streams used in discovery
     * @see #setNumSpatialStreamsInDiscovery(int)
     * @return an integer represents number of the spatial streams, {@link #UNSET_PARAMETER}
     * represent this parameter is not set
     */
    public int getNumSpatialStreamsInDiscovery() {
        return mNumSpatialStreamsInDiscovery;
    }

    /**
     * Check if discovery window early termination is enabled.
     * @see #setDwEarlyTerminationEnabled(boolean)
     * @return true if enabled, false otherwise.
     */
    public boolean isDwEarlyTerminationEnabled() {
        return mIsDwEarlyTerminationEnabled;
    }

    /**
     * Get the interval of the MAC address randomization.
     * @return An integer represents the interval in seconds, {@link #UNSET_PARAMETER} represent
     * this parameter is not set
     */
    public int getMacRandomizationIntervalSeconds() {
        return mMacRandomIntervalSec;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mDw24Ghz);
        dest.writeInt(mDw5Ghz);
        dest.writeInt(mDw6Ghz);
        dest.writeInt(mDiscoveryBeaconIntervalMs);
        dest.writeInt(mNumSpatialStreamsInDiscovery);
        dest.writeBoolean((mIsDwEarlyTerminationEnabled));
        dest.writeInt(mMacRandomIntervalSec);
    }

    /** @hide */
    @Override
    public String toString() {
        StringBuffer sbf = new StringBuffer();
        sbf.append("AwareParams [")
                .append("mDw24Ghz=").append(mDw24Ghz)
                .append(", mDw5Ghz=").append(mDw5Ghz)
                .append(", mDw6Ghz=").append(mDw6Ghz)
                .append(", mDiscoveryBeaconIntervalMs=").append(mDiscoveryBeaconIntervalMs)
                .append(", mNumSpatialStreamsInDiscovery=").append(mNumSpatialStreamsInDiscovery)
                .append(", mIsDwEarlyTerminationEnabled=").append(mIsDwEarlyTerminationEnabled)
                .append(", mMacRandomIntervalSec=").append(mMacRandomIntervalSec)
                .append("]");
        return sbf.toString();
    }
}

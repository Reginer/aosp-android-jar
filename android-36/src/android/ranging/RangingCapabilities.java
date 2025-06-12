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

package android.ranging;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.ranging.RangingManager.RangingTechnology;
import android.ranging.ble.cs.BleCsRangingCapabilities;
import android.ranging.ble.rssi.BleRssiRangingCapabilities;
import android.ranging.uwb.UwbRangingCapabilities;
import android.ranging.wifi.rtt.RttRangingCapabilities;

import com.android.ranging.flags.Flags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the capabilities and availability of various ranging technologies.
 *
 * <p>The {@code RangingCapabilities} class encapsulates the status of different ranging
 * technologies. It also allows querying the availability of other ranging technologies through a
 * mapping of technology identifiers to availability statuses.</p>
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class RangingCapabilities implements Parcelable {

    /**
     * Capabilities object for an individual ranging technology.
     *
     * @hide
     */
    public interface TechnologyCapabilities {
        /** @return the technology that these capabilities are associated with. */
        @RangingManager.RangingTechnology
        int getTechnology();
    }

    @Nullable
    private final UwbRangingCapabilities mUwbCapabilities;

    @Nullable
    private final RttRangingCapabilities mRttRangingCapabilities;

    @Nullable
    private final BleCsRangingCapabilities mCsCapabilities;

    @Nullable
    private final BleRssiRangingCapabilities mBleRssiCapabilities;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    @IntDef({
            /* Ranging technology is not supported on this device. */
            NOT_SUPPORTED,
            /* Ranging technology is disabled. */
            DISABLED_USER,
            /* Ranging technology disabled due to regulation. */
            DISABLED_REGULATORY,
            /* Ranging technology is enabled. */
            ENABLED,
            /* Ranging technology disabled due to admin restrictions. */
            DISABLED_USER_RESTRICTIONS
    })
    public @interface RangingTechnologyAvailability {
    }

    /**
     * Indicates that the ranging technology is not supported on the current device.
     */
    public static final int NOT_SUPPORTED = 0;

    /**
     * Indicates that the ranging technology is disabled by the user.
     */
    public static final int DISABLED_USER = 1;

    /**
     * Indicates that the ranging technology is disabled due to regulatory restrictions.
     */
    public static final int DISABLED_REGULATORY = 2;

    /**
     * Indicates that the ranging technology is enabled and available for use.
     */
    public static final int ENABLED = 3;

    /**
     * Indicates that the ranging technology is disabled due to device usage restrictions.
     */
    public static final int DISABLED_USER_RESTRICTIONS = 4;

    private final Map<@RangingManager.RangingTechnology Integer,
            @RangingTechnologyAvailability Integer> mAvailabilities;

    private RangingCapabilities(Builder builder) {
        mUwbCapabilities =
                (UwbRangingCapabilities) builder.mCapabilities.get(RangingManager.UWB);
        mRttRangingCapabilities = (RttRangingCapabilities) builder.mCapabilities.get(
                RangingManager.WIFI_NAN_RTT);
        mCsCapabilities = (BleCsRangingCapabilities) builder.mCapabilities.get(
                RangingManager.BLE_CS);
        mBleRssiCapabilities = (BleRssiRangingCapabilities) builder.mCapabilities.get(
                RangingManager.BLE_RSSI);
        mAvailabilities = builder.mAvailabilities;
    }

    private RangingCapabilities(Parcel in) {
        mUwbCapabilities = in.readParcelable(
                UwbRangingCapabilities.class.getClassLoader(),
                UwbRangingCapabilities.class);
        mRttRangingCapabilities = in.readParcelable(RttRangingCapabilities.class.getClassLoader(),
                RttRangingCapabilities.class);
        mCsCapabilities = in.readParcelable(
                BleCsRangingCapabilities.class.getClassLoader(), BleCsRangingCapabilities.class);
        mBleRssiCapabilities = in.readParcelable(
                BleRssiRangingCapabilities.class.getClassLoader(),
                BleRssiRangingCapabilities.class);
        int size = in.readInt();
        mAvailabilities = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            int key = in.readInt();
            int value = in.readInt();
            mAvailabilities.put(key, value);
        }
    }

    @NonNull
    public static final Creator<RangingCapabilities> CREATOR =
            new Creator<RangingCapabilities>() {
                @Override
                public RangingCapabilities createFromParcel(Parcel in) {
                    return new RangingCapabilities(in);
                }

                @Override
                public RangingCapabilities[] newArray(int size) {
                    return new RangingCapabilities[size];
                }
            };

    /**
     * Gets a map containing the availability of various ranging technologies.
     *
     * <p>The map uses technology identifiers as keys and their respective availability
     * statuses as values.</p>
     *
     * @return a {@link Map} with key {@link RangingTechnology} and value
     * {@link RangingTechnologyAvailability}.
     */
    @NonNull
    public Map<@RangingTechnology Integer, @RangingTechnologyAvailability Integer>
            getTechnologyAvailability() {
        return mAvailabilities;
    }

    /**
     * Gets the UWB ranging capabilities.
     *
     * @return a {@link UwbRangingCapabilities} object or {@code null} if not available.
     */
    @Nullable
    public UwbRangingCapabilities getUwbCapabilities() {
        return mUwbCapabilities;
    }

    /**
     * Gets the WiFi NAN-RTT ranging capabilities.
     *
     * @return a {@link RttRangingCapabilities} object or {@code null} if not available.
     */
    @Nullable
    public RttRangingCapabilities getRttRangingCapabilities() {
        return mRttRangingCapabilities;
    }

    /**
     * Gets the BLE channel sounding ranging capabilities.
     *
     * @return a {@link BleCsRangingCapabilities} object or {@code null} if not available.
     */
    @Nullable
    public BleCsRangingCapabilities getCsCapabilities() {
        return mCsCapabilities;
    }

    /**
     * Gets the BLE RSSI ranging capabilities.
     * This method is for internal use only-- BLE RSSI has no non-trivial capabilities.
     *
     * @hide
     */
    @Nullable
    public BleRssiRangingCapabilities getBleRssiCapabilities() {
        return mBleRssiCapabilities;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@androidx.annotation.NonNull Parcel dest, int flags) {
        dest.writeParcelable(mUwbCapabilities, flags);
        dest.writeParcelable(mRttRangingCapabilities, flags);
        dest.writeParcelable(mCsCapabilities, flags);
        dest.writeParcelable(mBleRssiCapabilities, flags);
        dest.writeInt(mAvailabilities.size()); // Write map size
        for (Map.Entry<Integer, Integer> entry : mAvailabilities.entrySet()) {
            dest.writeInt(entry.getKey()); // Write the key
            dest.writeInt(entry.getValue()); // Write the value
        }
    }

    /**
     * Builder for {@link RangingCapabilities}
     *
     * @hide
     */
    public static class Builder {
        private final HashMap<Integer, TechnologyCapabilities> mCapabilities = new HashMap<>();
        private final HashMap<Integer, Integer> mAvailabilities = new HashMap<>();

        public Builder addCapabilities(@NonNull TechnologyCapabilities capabilities) {
            mCapabilities.put(capabilities.getTechnology(), capabilities);
            return this;
        }

        public Builder addAvailability(
                @RangingTechnology int technology, @RangingTechnologyAvailability int availability
        ) {
            mAvailabilities.put(technology, availability);
            return this;
        }

        public RangingCapabilities build() {
            return new RangingCapabilities(this);
        }
    }

    @Override
    public String toString() {
        return "RangingCapabilities{ "
                + "mUwbCapabilities="
                + mUwbCapabilities
                + ", mRttRangingCapabilities="
                + mRttRangingCapabilities
                + ", mCsCapabilities="
                + mCsCapabilities
                + ", mBleRssiCapabilities="
                + mBleRssiCapabilities
                + ", mAvailabilities="
                + mAvailabilities
                + " }";
    }
}

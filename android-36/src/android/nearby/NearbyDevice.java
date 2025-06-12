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

package android.nearby;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.util.ArraySet;

import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A class represents a device that can be discovered by multiple mediums.
 *
 * @hide
 */
@SystemApi
public abstract class NearbyDevice {

    @Nullable
    private final String mName;

    @Medium
    private final List<Integer> mMediums;

    private final int mRssi;

    /**
     * Creates a new NearbyDevice.
     *
     * @param name Local device name. Can be {@code null} if there is no name.
     * @param mediums The {@link Medium}s over which the device is discovered.
     * @param rssi The received signal strength in dBm.
     * @hide
     */
    public NearbyDevice(@Nullable String name, List<Integer> mediums, int rssi) {
        for (int medium : mediums) {
            Preconditions.checkState(isValidMedium(medium),
                    "Not supported medium: " + medium
                            + ", scan medium must be one of NearbyDevice#Medium.");
        }
        mName = name;
        mMediums = mediums;
        mRssi = rssi;
    }

    static String mediumToString(@Medium int medium) {
        switch (medium) {
            case Medium.BLE:
                return "BLE";
            case Medium.BLUETOOTH:
                return "Bluetooth Classic";
            default:
                return "Unknown";
        }
    }

    /**
     * True if the medium is defined in {@link Medium}.
     *
     * @param medium Integer that may represent a medium type.
     */
    public static boolean isValidMedium(@Medium int medium) {
        return medium == Medium.BLE
                || medium == Medium.BLUETOOTH;
    }

    /**
     * The name of the device, or null if not available.
     */
    @Nullable
    public String getName() {
        return mName;
    }

    /** The medium over which this device was discovered. */
    @NonNull
    @Medium public List<Integer> getMediums() {
        return mMediums;
    }

    /**
     * Returns the received signal strength in dBm.
     */
    @IntRange(from = -127, to = 126)
    public int getRssi() {
        return mRssi;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NearbyDevice [");
        if (mName != null && !mName.isEmpty()) {
            stringBuilder.append("name=").append(mName).append(", ");
        }
        stringBuilder.append("medium={");
        for (int medium : mMediums) {
            stringBuilder.append(mediumToString(medium));
        }
        stringBuilder.append("} rssi=").append(mRssi);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NearbyDevice)) {
            return false;
        }
        NearbyDevice otherDevice = (NearbyDevice) other;
        Set<Integer> mediumSet = new ArraySet<>(mMediums);
        Set<Integer> otherMediumSet = new ArraySet<>(otherDevice.mMediums);
        if (!mediumSet.equals(otherMediumSet)) {
            return false;
        }

        return Objects.equals(mName, otherDevice.mName) && mRssi == otherDevice.mRssi;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mName, mMediums, mRssi);
    }

    /**
     * The medium where a NearbyDevice was discovered on.
     *
     * @hide
     */
    @IntDef({Medium.BLE, Medium.BLUETOOTH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Medium {
        int BLE = 1;
        int BLUETOOTH = 2;
    }
}


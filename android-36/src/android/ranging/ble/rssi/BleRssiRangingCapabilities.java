/*
 * Copyright 2025 The Android Open Source Project
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

package android.ranging.ble.rssi;

import static android.ranging.RangingManager.BLE_RSSI;

import android.os.Parcel;
import android.os.Parcelable;
import android.ranging.RangingCapabilities.TechnologyCapabilities;
import android.ranging.RangingManager;

import androidx.annotation.NonNull;

/**
 * BLE RSSI has no non-trivial capabilities. This class is for internal use only.
 *
 * @hide
 */
public class BleRssiRangingCapabilities implements Parcelable, TechnologyCapabilities {
    private final String mBluetoothAddress;

    public BleRssiRangingCapabilities(String address) {
        mBluetoothAddress = address;
    }

    protected BleRssiRangingCapabilities(Parcel in) {
        mBluetoothAddress = in.readString();
    }

    public static final Creator<BleRssiRangingCapabilities> CREATOR = new Creator<>() {
        @Override
        public BleRssiRangingCapabilities createFromParcel(Parcel in) {
            return new BleRssiRangingCapabilities(in);
        }

        @Override
        public BleRssiRangingCapabilities[] newArray(int size) {
            return new BleRssiRangingCapabilities[size];
        }
    };

    public String getBluetoothAddress() {
        return mBluetoothAddress;
    }

    @Override
    public @RangingManager.RangingTechnology int getTechnology() {
        return BLE_RSSI;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mBluetoothAddress);
    }

    @Override public String toString() {
        return "BleRssiCapabilities{" +
                "mBluetoothAddress='" + mBluetoothAddress + '\'' +
                '}';
    }
}

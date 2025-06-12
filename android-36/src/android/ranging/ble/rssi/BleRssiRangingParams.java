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

package android.ranging.ble.rssi;


import static android.ranging.raw.RawRangingDevice.UPDATE_RATE_NORMAL;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import android.ranging.raw.RawRangingDevice.RangingUpdateRate;

import com.android.ranging.flags.Flags;

/**
 * BleRssiRangingParams encapsulates the parameters required for a bluetooth rssi based ranging
 * session.
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class BleRssiRangingParams implements Parcelable {

    private final String mPeerBluetoothAddress;

    @RangingUpdateRate
    private final int mRangingUpdateRate;

    private BleRssiRangingParams(Builder builder) {
        mPeerBluetoothAddress = builder.mPeerBluetoothAddress;
        mRangingUpdateRate = builder.mRangingUpdateRate;
    }

    private BleRssiRangingParams(Parcel in) {
        mPeerBluetoothAddress = in.readString();
        mRangingUpdateRate = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mPeerBluetoothAddress);
        dest.writeInt(mRangingUpdateRate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<BleRssiRangingParams> CREATOR =
            new Creator<BleRssiRangingParams>() {
                @Override
                public BleRssiRangingParams createFromParcel(Parcel in) {
                    return new BleRssiRangingParams(in);
                }

                @Override
                public BleRssiRangingParams[] newArray(int size) {
                    return new BleRssiRangingParams[size];
                }
            };

    /**
     * Returns the Bluetooth address of the peer device.
     *
     * @return String representing the Bluetooth address.
     */
    @NonNull
    public String getPeerBluetoothAddress() {
        return mPeerBluetoothAddress;
    }

    /**
     * Returns the ranging update rate.
     *
     * @return ranging update rate.
     */
    @RangingUpdateRate
    public int getRangingUpdateRate() {
        return mRangingUpdateRate;
    }

    /**
     * Builder class to create {@link BleRssiRangingParams} instances.
     */
    public static final class Builder {
        private String mPeerBluetoothAddress;

        @RangingUpdateRate
        private int mRangingUpdateRate = UPDATE_RATE_NORMAL;

        /**
         * Constructs a new {@link Builder} for creating a bluetooth rssi ranging session.
         *
         * <p>Valid Bluetooth hardware addresses must be upper case, in big endian byte order, and
         * in a format such as "00:11:22:33:AA:BB". The helper
         * {@see android.bluetooth.BluetoothAdapter#checkBluetoothAddress} is available to validate
         * a Bluetooth address.
         *
         * @param peerBluetoothAddress The address of the peer device must be non-null
         *                             Bluetooth address.
         *  {@see android.bluetooth.BluetoothDevice#getAddress()}
         * @throws IllegalArgumentException if {@code peerBluetoothAddress} is null or does not
         * conform to "00:11:22:33:AA:BB" format.
         */
        public Builder(@NonNull String peerBluetoothAddress) {
            if (!android.bluetooth.BluetoothAdapter.checkBluetoothAddress(peerBluetoothAddress)) {
                throw new IllegalArgumentException(
                        "Bluetooth address is not in 00:11:22:33:AA:BB format");
            }
            mPeerBluetoothAddress = peerBluetoothAddress;
        }

        /**
         * Sets the update rate for the BLE rssi ranging session.
         * <p>Defaults to {@link RangingUpdateRate#UPDATE_RATE_NORMAL}
         *
         * @param updateRate the reporting frequency.
         * @return this {@link Builder} instance.
         */
        @NonNull
        public Builder setRangingUpdateRate(@RangingUpdateRate int updateRate) {
            mRangingUpdateRate = updateRate;
            return this;
        }

        /**
         * Builds and returns a {@link BleRssiRangingParams} instance.
         *
         * @return a new {@link BleRssiRangingParams}.
         */
        @NonNull
        public BleRssiRangingParams build() {
            return new BleRssiRangingParams(this);
        }
    }
}

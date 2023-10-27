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

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A class represents a Fast Pair device that can be discovered by multiple mediums.
 *
 * @hide
 */
public class FastPairDevice extends NearbyDevice implements Parcelable {
    /**
     * Used to read a FastPairDevice from a Parcel.
     */
    public static final Creator<FastPairDevice> CREATOR = new Creator<FastPairDevice>() {
        @Override
        public FastPairDevice createFromParcel(Parcel in) {
            FastPairDevice.Builder builder = new FastPairDevice.Builder();
            if (in.readInt() == 1) {
                builder.setName(in.readString());
            }
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                builder.addMedium(in.readInt());
            }
            builder.setRssi(in.readInt());
            builder.setTxPower(in.readInt());
            if (in.readInt() == 1) {
                builder.setModelId(in.readString());
            }
            builder.setBluetoothAddress(in.readString());
            if (in.readInt() == 1) {
                int dataLength = in.readInt();
                byte[] data = new byte[dataLength];
                in.readByteArray(data);
                builder.setData(data);
            }
            return builder.build();
        }

        @Override
        public FastPairDevice[] newArray(int size) {
            return new FastPairDevice[size];
        }
    };

    // The transmit power in dBm. Valid range is [-127, 126]. a
    // See android.bluetooth.le.ScanResult#getTxPower
    private int mTxPower;

    // Some OEM devices devices don't have model Id.
    @Nullable private final String mModelId;

    // Bluetooth hardware address as string. Can be read from BLE ScanResult.
    private final String mBluetoothAddress;

    @Nullable
    private final byte[] mData;

    /**
     * Creates a new FastPairDevice.
     *
     * @param name Name of the FastPairDevice. Can be {@code null} if there is no name.
     * @param mediums The {@link Medium}s over which the device is discovered.
     * @param rssi The received signal strength in dBm.
     * @param txPower The transmit power in dBm. Valid range is [-127, 126].
     * @param modelId The identifier of the Fast Pair device.
     *                Can be {@code null} if there is no Model ID.
     * @param bluetoothAddress The hardware address of this BluetoothDevice.
     * @param data Extra data for a Fast Pair device.
     */
    public FastPairDevice(@Nullable String name,
            List<Integer> mediums,
            int rssi,
            int txPower,
            @Nullable String modelId,
            @NonNull String bluetoothAddress,
            @Nullable byte[] data) {
        super(name, mediums, rssi);
        this.mTxPower = txPower;
        this.mModelId = modelId;
        this.mBluetoothAddress = bluetoothAddress;
        this.mData = data;
    }

    /**
     * Gets the transmit power in dBm. A value of
     * android.bluetooth.le.ScanResult#TX_POWER_NOT_PRESENT
     * indicates that the TX power is not present.
     */
    @IntRange(from = -127, to = 126)
    public int getTxPower() {
        return mTxPower;
    }

    /**
     * Gets the identifier of the Fast Pair device. Can be {@code null} if there is no Model ID.
     */
    @Nullable
    public String getModelId() {
        return this.mModelId;
    }

    /**
     * Gets the hardware address of this BluetoothDevice.
     */
    @NonNull
    public String getBluetoothAddress() {
        return mBluetoothAddress;
    }

    /**
     * Gets the extra data for a Fast Pair device. Can be {@code null} if there is extra data.
     *
     * @hide
     */
    @Nullable
    public byte[] getData() {
        return mData;
    }

    /**
     * No special parcel contents.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Returns a string representation of this FastPairDevice.
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("FastPairDevice [");
        String name = getName();
        if (getName() != null && !name.isEmpty()) {
            stringBuilder.append("name=").append(name).append(", ");
        }
        stringBuilder.append("medium={");
        for (int medium: getMediums()) {
            stringBuilder.append(mediumToString(medium));
        }
        stringBuilder.append("} rssi=").append(getRssi());
        stringBuilder.append(" txPower=").append(mTxPower);
        stringBuilder.append(" modelId=").append(mModelId);
        stringBuilder.append(" bluetoothAddress=").append(mBluetoothAddress);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FastPairDevice) {
            FastPairDevice otherDevice = (FastPairDevice) other;
            if (!super.equals(other)) {
                return false;
            }
            return  mTxPower == otherDevice.mTxPower
                    && Objects.equals(mModelId, otherDevice.mModelId)
                    && Objects.equals(mBluetoothAddress, otherDevice.mBluetoothAddress)
                    && Arrays.equals(mData, otherDevice.mData);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getName(), getMediums(), getRssi(), mTxPower, mModelId, mBluetoothAddress,
                Arrays.hashCode(mData));
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        String name = getName();
        dest.writeInt(name == null ? 0 : 1);
        if (name != null) {
            dest.writeString(name);
        }
        List<Integer> mediums = getMediums();
        dest.writeInt(mediums.size());
        for (int medium : mediums) {
            dest.writeInt(medium);
        }
        dest.writeInt(getRssi());
        dest.writeInt(mTxPower);
        dest.writeInt(mModelId == null ? 0 : 1);
        if (mModelId != null) {
            dest.writeString(mModelId);
        }
        dest.writeString(mBluetoothAddress);
        dest.writeInt(mData == null ? 0 : 1);
        if (mData != null) {
            dest.writeInt(mData.length);
            dest.writeByteArray(mData);
        }
    }

    /**
     * A builder class for {@link FastPairDevice}
     *
     * @hide
     */
    public static final class Builder {
        private final List<Integer> mMediums;

        @Nullable private String mName;
        private int mRssi;
        private int mTxPower;
        @Nullable private String mModelId;
        private String mBluetoothAddress;
        @Nullable private byte[] mData;

        public Builder() {
            mMediums = new ArrayList<>();
        }

        /**
         * Sets the name of the Fast Pair device.
         *
         * @param name Name of the FastPairDevice. Can be {@code null} if there is no name.
         */
        @NonNull
        public Builder setName(@Nullable String name) {
            mName = name;
            return this;
        }

        /**
         * Sets the medium over which the Fast Pair device is discovered.
         *
         * @param medium The {@link Medium} over which the device is discovered.
         */
        @NonNull
        public Builder addMedium(@Medium int medium) {
            mMediums.add(medium);
            return this;
        }

        /**
         * Sets the RSSI between the scan device and the discovered Fast Pair device.
         *
         * @param rssi The received signal strength in dBm.
         */
        @NonNull
        public Builder setRssi(@IntRange(from = -127, to = 126) int rssi) {
            mRssi = rssi;
            return this;
        }

        /**
         * Sets the txPower.
         *
         * @param txPower The transmit power in dBm
         */
        @NonNull
        public Builder setTxPower(@IntRange(from = -127, to = 126) int txPower) {
            mTxPower = txPower;
            return this;
        }

        /**
         * Sets the model Id of this Fast Pair device.
         *
         * @param modelId The identifier of the Fast Pair device. Can be {@code null}
         *                if there is no Model ID.
         */
        @NonNull
        public Builder setModelId(@Nullable String modelId) {
            mModelId = modelId;
            return this;
        }

        /**
         * Sets the hardware address of this BluetoothDevice.
         *
         * @param bluetoothAddress The hardware address of this BluetoothDevice.
         */
        @NonNull
        public Builder setBluetoothAddress(@NonNull String bluetoothAddress) {
            Objects.requireNonNull(bluetoothAddress);
            mBluetoothAddress = bluetoothAddress;
            return this;
        }

        /**
         * Sets the raw data for a FastPairDevice. Can be {@code null} if there is no extra data.
         *
         * @hide
         */
        @NonNull
        public Builder setData(@Nullable byte[] data) {
            mData = data;
            return this;
        }

        /**
         * Builds a FastPairDevice and return it.
         */
        @NonNull
        public FastPairDevice build() {
            return new FastPairDevice(mName, mMediums, mRssi, mTxPower, mModelId,
                    mBluetoothAddress, mData);
        }
    }
}

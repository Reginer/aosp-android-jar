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
import android.bluetooth.le.ScanRecord;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Objects;

/**
 * A data class representing scan result from Nearby Service. Scan result can come from multiple
 * mediums like BLE, Wi-Fi Aware, and etc. A scan result consists of An encapsulation of various
 * parameters for requesting nearby scans.
 *
 * <p>All scan results generated through {@link NearbyManager} are guaranteed to have a valid
 * medium, identifier, timestamp (both UTC time and elapsed real-time since boot), and accuracy. All
 * other parameters are optional.
 *
 * @hide
 */
public final class NearbyDeviceParcelable implements Parcelable {

    /** Used to read a NearbyDeviceParcelable from a Parcel. */
    @NonNull
    public static final Creator<NearbyDeviceParcelable> CREATOR =
            new Creator<NearbyDeviceParcelable>() {
                @Override
                public NearbyDeviceParcelable createFromParcel(Parcel in) {
                    Builder builder = new Builder();
                    builder.setDeviceId(in.readLong());
                    builder.setScanType(in.readInt());
                    if (in.readInt() == 1) {
                        builder.setName(in.readString());
                    }
                    builder.setMedium(in.readInt());
                    builder.setTxPower(in.readInt());
                    builder.setRssi(in.readInt());
                    builder.setAction(in.readInt());
                    builder.setPublicCredential(
                            in.readParcelable(
                                    PublicCredential.class.getClassLoader(),
                                    PublicCredential.class));
                    if (in.readInt() == 1) {
                        builder.setFastPairModelId(in.readString());
                    }
                    if (in.readInt() == 1) {
                        builder.setBluetoothAddress(in.readString());
                    }
                    if (in.readInt() == 1) {
                        int dataLength = in.readInt();
                        byte[] data = new byte[dataLength];
                        in.readByteArray(data);
                        builder.setData(data);
                    }
                    if (in.readInt() == 1) {
                        int saltLength = in.readInt();
                        byte[] salt = new byte[saltLength];
                        in.readByteArray(salt);
                        builder.setData(salt);
                    }
                    if (in.readInt() == 1) {
                        builder.setPresenceDevice(in.readParcelable(
                                PresenceDevice.class.getClassLoader(),
                                PresenceDevice.class));
                    }
                    if (in.readInt() == 1) {
                        int encryptionKeyTagLength = in.readInt();
                        byte[] keyTag = new byte[encryptionKeyTagLength];
                        in.readByteArray(keyTag);
                        builder.setData(keyTag);
                    }
                    return builder.build();
                }

                @Override
                public NearbyDeviceParcelable[] newArray(int size) {
                    return new NearbyDeviceParcelable[size];
                }
            };

    private final long mDeviceId;
    @ScanRequest.ScanType int mScanType;
    @Nullable private final String mName;
    @NearbyDevice.Medium private final int mMedium;
    private final int mTxPower;
    private final int mRssi;
    private final int mAction;
    private final PublicCredential mPublicCredential;
    @Nullable private final String mBluetoothAddress;
    @Nullable private final String mFastPairModelId;
    @Nullable private final byte[] mData;
    @Nullable private final byte[] mSalt;
    @Nullable private final PresenceDevice mPresenceDevice;
    @Nullable private final byte[] mEncryptionKeyTag;

    private NearbyDeviceParcelable(
            long deviceId,
            @ScanRequest.ScanType int scanType,
            @Nullable String name,
            int medium,
            int TxPower,
            int rssi,
            int action,
            PublicCredential publicCredential,
            @Nullable String fastPairModelId,
            @Nullable String bluetoothAddress,
            @Nullable byte[] data,
            @Nullable byte[] salt,
            @Nullable PresenceDevice presenceDevice,
            @Nullable byte[] encryptionKeyTag) {
        mDeviceId = deviceId;
        mScanType = scanType;
        mName = name;
        mMedium = medium;
        mTxPower = TxPower;
        mRssi = rssi;
        mAction = action;
        mPublicCredential = publicCredential;
        mFastPairModelId = fastPairModelId;
        mBluetoothAddress = bluetoothAddress;
        mData = data;
        mSalt = salt;
        mPresenceDevice = presenceDevice;
        mEncryptionKeyTag = encryptionKeyTag;
    }

    /** No special parcel contents. */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this NearbyDeviceParcelable in to a Parcel.
     *
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(mDeviceId);
        dest.writeInt(mScanType);
        dest.writeInt(mName == null ? 0 : 1);
        if (mName != null) {
            dest.writeString(mName);
        }
        dest.writeInt(mMedium);
        dest.writeInt(mTxPower);
        dest.writeInt(mRssi);
        dest.writeInt(mAction);
        dest.writeParcelable(mPublicCredential, flags);
        dest.writeInt(mFastPairModelId == null ? 0 : 1);
        if (mFastPairModelId != null) {
            dest.writeString(mFastPairModelId);
        }
        dest.writeInt(mBluetoothAddress == null ? 0 : 1);
        if (mBluetoothAddress != null) {
            dest.writeString(mBluetoothAddress);
        }
        dest.writeInt(mData == null ? 0 : 1);
        if (mData != null) {
            dest.writeInt(mData.length);
            dest.writeByteArray(mData);
        }
        dest.writeInt(mSalt == null ? 0 : 1);
        if (mSalt != null) {
            dest.writeInt(mSalt.length);
            dest.writeByteArray(mSalt);
        }
        dest.writeInt(mPresenceDevice == null ? 0 : 1);
        if (mPresenceDevice != null) {
            dest.writeParcelable(mPresenceDevice, /* parcelableFlags= */ 0);
        }
        dest.writeInt(mEncryptionKeyTag == null ? 0 : 1);
        if (mEncryptionKeyTag != null) {
            dest.writeInt(mEncryptionKeyTag.length);
            dest.writeByteArray(mEncryptionKeyTag);
        }
    }

    /** Returns a string representation of this ScanRequest. */
    @Override
    public String toString() {
        return "NearbyDeviceParcelable["
                + "deviceId="
                + mDeviceId
                + ", scanType="
                + mScanType
                + ", name="
                + mName
                + ", medium="
                + NearbyDevice.mediumToString(mMedium)
                + ", txPower="
                + mTxPower
                + ", rssi="
                + mRssi
                + ", action="
                + mAction
                + ", bluetoothAddress="
                + mBluetoothAddress
                + ", fastPairModelId="
                + mFastPairModelId
                + ", data="
                + Arrays.toString(mData)
                + ", salt="
                + Arrays.toString(mSalt)
                + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof NearbyDeviceParcelable) {
            NearbyDeviceParcelable otherNearbyDeviceParcelable = (NearbyDeviceParcelable) other;
            return  mDeviceId == otherNearbyDeviceParcelable.mDeviceId
                    && mScanType == otherNearbyDeviceParcelable.mScanType
                    && (Objects.equals(mName, otherNearbyDeviceParcelable.mName))
                    && (mMedium == otherNearbyDeviceParcelable.mMedium)
                    && (mTxPower == otherNearbyDeviceParcelable.mTxPower)
                    && (mRssi == otherNearbyDeviceParcelable.mRssi)
                    && (mAction == otherNearbyDeviceParcelable.mAction)
                    && (Objects.equals(
                    mPublicCredential, otherNearbyDeviceParcelable.mPublicCredential))
                    && (Objects.equals(
                    mBluetoothAddress, otherNearbyDeviceParcelable.mBluetoothAddress))
                    && (Objects.equals(
                    mFastPairModelId, otherNearbyDeviceParcelable.mFastPairModelId))
                    && (Arrays.equals(mData, otherNearbyDeviceParcelable.mData))
                    && (Arrays.equals(mSalt, otherNearbyDeviceParcelable.mSalt))
                    && (Objects.equals(
                    mPresenceDevice, otherNearbyDeviceParcelable.mPresenceDevice))
                    && (Arrays.equals(
                    mEncryptionKeyTag, otherNearbyDeviceParcelable.mEncryptionKeyTag));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mDeviceId,
                mScanType,
                mName,
                mMedium,
                mRssi,
                mAction,
                mPublicCredential.hashCode(),
                mBluetoothAddress,
                mFastPairModelId,
                Arrays.hashCode(mData),
                Arrays.hashCode(mSalt),
                mPresenceDevice,
                Arrays.hashCode(mEncryptionKeyTag));
    }

    /**
     * The id of the device.
     * <p>This id is not a hardware id. It may rotate based on the remote device's broadcasts.
     *
     * @hide
     */
    public long getDeviceId() {
        return mDeviceId;
    }

    /**
     * Returns the type of the scan.
     *
     * @hide
     */
    @ScanRequest.ScanType
    public int getScanType() {
        return mScanType;
    }

    /**
     * Gets the name of the NearbyDeviceParcelable. Returns {@code null} If there is no name.
     *
     * Used in Fast Pair.
     */
    @Nullable
    public String getName() {
        return mName;
    }

    /**
     * Gets the {@link android.nearby.NearbyDevice.Medium} of the NearbyDeviceParcelable over which
     * it is discovered.
     *
     * Used in Fast Pair and Nearby Presence.
     */
    @NearbyDevice.Medium
    public int getMedium() {
        return mMedium;
    }

    /**
     * Gets the transmission power in dBm.
     *
     * Used in Fast Pair.
     *
     * @hide
     */
    @IntRange(from = -127, to = 126)
    public int getTxPower() {
        return mTxPower;
    }

    /**
     * Gets the received signal strength in dBm.
     *
     * Used in Fast Pair and Nearby Presence.
     */
    @IntRange(from = -127, to = 126)
    public int getRssi() {
        return mRssi;
    }

    /**
     * Gets the Action.
     *
     * Used in Nearby Presence.
     *
     * @hide
     */
    @IntRange(from = -127, to = 126)
    public int getAction() {
        return mAction;
    }

    /**
     * Gets the public credential.
     *
     * Used in Nearby Presence.
     *
     * @hide
     */
    @NonNull
    public PublicCredential getPublicCredential() {
        return mPublicCredential;
    }

    /**
     * Gets the Fast Pair identifier. Returns {@code null} if there is no Model ID or this is not a
     * Fast Pair device.
     *
     * Used in Fast Pair.
     */
    @Nullable
    public String getFastPairModelId() {
        return mFastPairModelId;
    }

    /**
     * Gets the Bluetooth device hardware address. Returns {@code null} if the device is not
     * discovered by Bluetooth.
     *
     * Used in Fast Pair.
     */
    @Nullable
    public String getBluetoothAddress() {
        return mBluetoothAddress;
    }

    /**
     * Gets the raw data from the scanning.
     * Returns {@code null} if there is no extra data or this is not a Fast Pair device.
     *
     * Used in Fast Pair.
     */
    @Nullable
    public byte[] getData() {
        return mData;
    }

    /**
     * Gets the salt in the advertisement from the Nearby Presence device.
     * Returns {@code null} if this is not a Nearby Presence device.
     *
     * Used in Nearby Presence.
     */
    @Nullable
    public byte[] getSalt() {
        return mSalt;
    }

    /**
     * Gets the {@link PresenceDevice} Nearby Presence device. This field is
     * for Fast Pair client only.
     */
    @Nullable
    public PresenceDevice getPresenceDevice() {
        return mPresenceDevice;
    }

    /**
     * Gets the encryption key tag calculated from advertisement
     * Returns {@code null} if the data is not encrypted or this is not a Presence device.
     *
     * Used in Presence.
     */
    @Nullable
    public byte[] getEncryptionKeyTag() {
        return mEncryptionKeyTag;
    }

    /** Builder class for {@link NearbyDeviceParcelable}. */
    public static final class Builder {
        private long mDeviceId = -1;
        @Nullable private String mName;
        @NearbyDevice.Medium private int mMedium;
        private int mTxPower;
        private int mRssi;
        private int mAction;
        private PublicCredential mPublicCredential;
        @ScanRequest.ScanType int mScanType;
        @Nullable private String mFastPairModelId;
        @Nullable private String mBluetoothAddress;
        @Nullable private byte[] mData;
        @Nullable private byte[] mSalt;
        @Nullable private PresenceDevice mPresenceDevice;
        @Nullable private byte[] mEncryptionKeyTag;

        /** Sets the id of the device. */
        public Builder setDeviceId(long deviceId) {
            this.mDeviceId = deviceId;
            return this;
        }

        /**
         * Sets the scan type of the NearbyDeviceParcelable.
         *
         * @hide
         */
        public Builder setScanType(@ScanRequest.ScanType int scanType) {
            mScanType = scanType;
            return this;
        }

        /**
         * Sets the name of the scanned device.
         *
         * @param name The local name of the scanned device.
         */
        @NonNull
        public Builder setName(@Nullable String name) {
            mName = name;
            return this;
        }

        /**
         * Sets the medium over which the device is discovered.
         *
         * @param medium The {@link NearbyDevice.Medium} over which the device is discovered.
         */
        @NonNull
        public Builder setMedium(@NearbyDevice.Medium int medium) {
            mMedium = medium;
            return this;
        }

        /**
         * Sets the transmission power of the discovered device.
         *
         * @param txPower The transmission power in dBm.
         * @hide
         */
        @NonNull
        public Builder setTxPower(int txPower) {
            mTxPower = txPower;
            return this;
        }

        /**
         * Sets the RSSI between scanned device and the discovered device.
         *
         * @param rssi The received signal strength in dBm.
         */
        @NonNull
        public Builder setRssi(@IntRange(from = -127, to = 126) int rssi) {
            mRssi = rssi;
            return this;
        }

        /**
         * Sets the action from the discovered device.
         *
         * @param action The action of the discovered device.
         * @hide
         */
        @NonNull
        public Builder setAction(int action) {
            mAction = action;
            return this;
        }

        /**
         * Sets the public credential of the discovered device.
         *
         * @param publicCredential The public credential.
         * @hide
         */
        @NonNull
        public Builder setPublicCredential(@NonNull PublicCredential publicCredential) {
            mPublicCredential = publicCredential;
            return this;
        }

        /**
         * Sets the Fast Pair model Id.
         *
         * @param fastPairModelId Fast Pair device identifier.
         */
        @NonNull
        public Builder setFastPairModelId(@Nullable String fastPairModelId) {
            mFastPairModelId = fastPairModelId;
            return this;
        }

        /**
         * Sets the bluetooth address.
         *
         * @param bluetoothAddress The hardware address of the bluetooth device.
         */
        @NonNull
        public Builder setBluetoothAddress(@Nullable String bluetoothAddress) {
            mBluetoothAddress = bluetoothAddress;
            return this;
        }

        /**
         * Sets the scanned raw data.
         *
         * @param data raw data scanned, like {@link ScanRecord#getServiceData()} if scanned by
         *             Bluetooth.
         */
        @NonNull
        public Builder setData(@Nullable byte[] data) {
            mData = data;
            return this;
        }

        /**
         * Sets the encryption key tag calculated from the advertisement.
         *
         * @param encryptionKeyTag calculated from identity scanned from the advertisement
         */
        @NonNull
        public Builder setEncryptionKeyTag(@Nullable byte[] encryptionKeyTag) {
            mEncryptionKeyTag = encryptionKeyTag;
            return this;
        }

        /**
         * Sets the slat in the advertisement from the Nearby Presence device.
         *
         * @param salt in the advertisement from the Nearby Presence device.
         */
        @NonNull
        public Builder setSalt(@Nullable byte[] salt) {
            mSalt = salt;
            return this;
        }

        /**
         * Sets the {@link PresenceDevice} if there is any.
         * The current {@link NearbyDeviceParcelable} can be seen as the wrapper of the
         * {@link PresenceDevice}.
         */
        @Nullable
        public Builder setPresenceDevice(@Nullable PresenceDevice presenceDevice) {
            mPresenceDevice = presenceDevice;
            return this;
        }

        /** Builds a ScanResult. */
        @NonNull
        public NearbyDeviceParcelable build() {
            return new NearbyDeviceParcelable(
                    mDeviceId,
                    mScanType,
                    mName,
                    mMedium,
                    mTxPower,
                    mRssi,
                    mAction,
                    mPublicCredential,
                    mFastPairModelId,
                    mBluetoothAddress,
                    mData,
                    mSalt,
                    mPresenceDevice,
                    mEncryptionKeyTag);
        }
    }
}

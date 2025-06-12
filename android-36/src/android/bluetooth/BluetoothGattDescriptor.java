/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.bluetooth;

import android.annotation.NonNull;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import java.util.UUID;

/**
 * Represents a Bluetooth GATT Descriptor
 *
 * <p>GATT Descriptors contain additional information and attributes of a GATT characteristic,
 * {@link BluetoothGattCharacteristic}. They can be used to describe the characteristic's features
 * or to control certain behaviours of the characteristic.
 */
public class BluetoothGattDescriptor implements Parcelable {

    /** Value used to enable notification for a client configuration descriptor */
    public static final byte[] ENABLE_NOTIFICATION_VALUE = {0x01, 0x00};

    /** Value used to enable indication for a client configuration descriptor */
    public static final byte[] ENABLE_INDICATION_VALUE = {0x02, 0x00};

    /** Value used to disable notifications or indicatinos */
    public static final byte[] DISABLE_NOTIFICATION_VALUE = {0x00, 0x00};

    /** Descriptor read permission */
    public static final int PERMISSION_READ = 0x01;

    /** Descriptor permission: Allow encrypted read operations */
    public static final int PERMISSION_READ_ENCRYPTED = 0x02;

    /** Descriptor permission: Allow reading with person-in-the-middle protection */
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 0x04;

    /** Descriptor write permission */
    public static final int PERMISSION_WRITE = 0x10;

    /** Descriptor permission: Allow encrypted writes */
    public static final int PERMISSION_WRITE_ENCRYPTED = 0x20;

    /** Descriptor permission: Allow encrypted writes with person-in-the-middle protection */
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 0x40;

    /** Descriptor permission: Allow signed write operations */
    public static final int PERMISSION_WRITE_SIGNED = 0x80;

    /** Descriptor permission: Allow signed write operations with person-in-the-middle protection */
    public static final int PERMISSION_WRITE_SIGNED_MITM = 0x100;

    /**
     * The UUID of this descriptor.
     *
     * @hide
     */
    protected UUID mUuid;

    /**
     * Instance ID for this descriptor.
     *
     * @hide
     */
    @UnsupportedAppUsage protected int mInstance;

    /**
     * Permissions for this descriptor
     *
     * @hide
     */
    protected int mPermissions;

    /**
     * Back-reference to the characteristic this descriptor belongs to.
     *
     * @hide
     */
    @UnsupportedAppUsage protected BluetoothGattCharacteristic mCharacteristic;

    /**
     * The value for this descriptor.
     *
     * @hide
     */
    protected byte[] mValue;

    /**
     * Create a new BluetoothGattDescriptor.
     *
     * @param uuid The UUID for this descriptor
     * @param permissions Permissions for this descriptor
     */
    public BluetoothGattDescriptor(UUID uuid, int permissions) {
        initDescriptor(null, uuid, 0, permissions);
    }

    /**
     * Create a new BluetoothGattDescriptor.
     *
     * @param characteristic The characteristic this descriptor belongs to
     * @param uuid The UUID for this descriptor
     * @param permissions Permissions for this descriptor
     */
    /*package*/ BluetoothGattDescriptor(
            BluetoothGattCharacteristic characteristic, UUID uuid, int instance, int permissions) {
        initDescriptor(characteristic, uuid, instance, permissions);
    }

    /** @hide */
    public BluetoothGattDescriptor(UUID uuid, int instance, int permissions) {
        initDescriptor(null, uuid, instance, permissions);
    }

    private void initDescriptor(
            BluetoothGattCharacteristic characteristic, UUID uuid, int instance, int permissions) {
        mCharacteristic = characteristic;
        mUuid = uuid;
        mInstance = instance;
        mPermissions = permissions;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        (new ParcelUuid(mUuid)).writeToParcel(out, flags);
        out.writeInt(mInstance);
        out.writeInt(mPermissions);
    }

    public static final @NonNull Creator<BluetoothGattDescriptor> CREATOR =
            new Creator<>() {
                public BluetoothGattDescriptor createFromParcel(Parcel in) {
                    return new BluetoothGattDescriptor(in);
                }

                public BluetoothGattDescriptor[] newArray(int size) {
                    return new BluetoothGattDescriptor[size];
                }
            };

    private BluetoothGattDescriptor(Parcel in) {
        mUuid = ParcelUuid.CREATOR.createFromParcel(in).getUuid();
        mInstance = in.readInt();
        mPermissions = in.readInt();
    }

    /**
     * Returns the characteristic this descriptor belongs to.
     *
     * @return The characteristic.
     */
    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
    }

    /**
     * Set the back-reference to the associated characteristic
     *
     * @hide
     */
    @UnsupportedAppUsage
    /*package*/ void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        mCharacteristic = characteristic;
    }

    /**
     * Returns the UUID of this descriptor.
     *
     * @return UUID of this descriptor
     */
    public UUID getUuid() {
        return mUuid;
    }

    /**
     * Returns the instance ID for this descriptor.
     *
     * <p>If a remote device offers multiple descriptors with the same UUID, the instance ID is used
     * to distuinguish between descriptors.
     *
     * @return Instance ID of this descriptor
     * @hide
     */
    public int getInstanceId() {
        return mInstance;
    }

    /**
     * Force the instance ID.
     *
     * @hide
     */
    public void setInstanceId(int instanceId) {
        mInstance = instanceId;
    }

    /**
     * Returns the permissions for this descriptor.
     *
     * @return Permissions of this descriptor
     */
    public int getPermissions() {
        return mPermissions;
    }

    /**
     * Returns the stored value for this descriptor
     *
     * <p>This function returns the stored value for this descriptor as retrieved by calling {@link
     * BluetoothGatt#readDescriptor}. The cached value of the descriptor is updated as a result of a
     * descriptor read operation.
     *
     * @return Cached value of the descriptor
     * @deprecated Use {@link BluetoothGatt#readDescriptor(BluetoothGattDescriptor)} instead
     */
    @Deprecated
    public byte[] getValue() {
        return mValue;
    }

    /**
     * Updates the locally stored value of this descriptor.
     *
     * <p>This function modifies the locally stored cached value of this descriptor. To send the
     * value to the remote device, call {@link BluetoothGatt#writeDescriptor} to send the value to
     * the remote device.
     *
     * @param value New value for this descriptor
     * @return true if the locally stored value has been set, false if the requested value could not
     *     be stored locally.
     * @deprecated Pass the descriptor value directly into {@link
     *     BluetoothGatt#writeDescriptor(BluetoothGattDescriptor, byte[])}
     */
    @Deprecated
    public boolean setValue(byte[] value) {
        mValue = value;
        return true;
    }
}

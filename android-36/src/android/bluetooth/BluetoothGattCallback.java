/*
 * Copyright (C) 2017 The Android Open Source Project
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

/** This abstract class is used to implement {@link BluetoothGatt} callbacks. */
public abstract class BluetoothGattCallback {

    /**
     * Callback triggered as result of {@link BluetoothGatt#setPreferredPhy}, or as a result of
     * remote device changing the PHY.
     *
     * @param gatt GATT client
     * @param txPhy the transmitter PHY in use. One of {@link BluetoothDevice#PHY_LE_1M}, {@link
     *     BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
     * @param rxPhy the receiver PHY in use. One of {@link BluetoothDevice#PHY_LE_1M}, {@link
     *     BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
     * @param status Status of the PHY update operation. {@link BluetoothGatt#GATT_SUCCESS} if the
     *     operation succeeds.
     */
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {}

    /**
     * Callback triggered as result of {@link BluetoothGatt#readPhy}
     *
     * @param gatt GATT client
     * @param txPhy the transmitter PHY in use. One of {@link BluetoothDevice#PHY_LE_1M}, {@link
     *     BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
     * @param rxPhy the receiver PHY in use. One of {@link BluetoothDevice#PHY_LE_1M}, {@link
     *     BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
     * @param status Status of the PHY read operation. {@link BluetoothGatt#GATT_SUCCESS} if the
     *     operation succeeds.
     */
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {}

    /**
     * Callback indicating when GATT client has connected/disconnected to/from a remote GATT server.
     *
     * @param gatt GATT client
     * @param status Status of the connect or disconnect operation. {@link
     *     BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     * @param newState Returns the new connection state. Can be one of {@link
     *     BluetoothProfile#STATE_DISCONNECTED} or {@link BluetoothProfile#STATE_CONNECTED}
     */
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {}

    /**
     * Callback invoked when the list of remote services, characteristics and descriptors for the
     * remote device have been updated, ie new services have been discovered.
     *
     * @param gatt GATT client invoked {@link BluetoothGatt#discoverServices}
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device has been explored
     *     successfully.
     */
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {}

    /**
     * Callback reporting the result of a characteristic read operation.
     *
     * @param gatt GATT client invoked {@link
     *     BluetoothGatt#readCharacteristic(BluetoothGattCharacteristic)}
     * @param characteristic Characteristic that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation was completed
     *     successfully.
     * @deprecated Use {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt,
     *     BluetoothGattCharacteristic, byte[], int)} as it is memory safe
     */
    @Deprecated
    public void onCharacteristicRead(
            BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {}

    /**
     * Callback reporting the result of a characteristic read operation.
     *
     * @param gatt GATT client invoked {@link
     *     BluetoothGatt#readCharacteristic(BluetoothGattCharacteristic)}
     * @param characteristic Characteristic that was read from the associated remote device.
     * @param value the value of the characteristic
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation was completed
     *     successfully.
     */
    public void onCharacteristicRead(
            @NonNull BluetoothGatt gatt,
            @NonNull BluetoothGattCharacteristic characteristic,
            @NonNull byte[] value,
            int status) {
        onCharacteristicRead(gatt, characteristic, status);
    }

    /**
     * Callback indicating the result of a characteristic write operation.
     *
     * <p>If this callback is invoked while a reliable write transaction is in progress, the value
     * of the characteristic represents the value reported by the remote device. An application
     * should compare this value to the desired value to be written. If the values don't match, the
     * application must abort the reliable write transaction.
     *
     * @param gatt GATT client that invoked {@link
     *     BluetoothGatt#writeCharacteristic(BluetoothGattCharacteristic, byte[], int)}
     * @param characteristic Characteristic that was written to the associated remote device.
     * @param status The result of the write operation {@link BluetoothGatt#GATT_SUCCESS} if the
     *     operation succeeds.
     */
    public void onCharacteristicWrite(
            BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {}

    /**
     * Callback triggered as a result of a remote characteristic notification.
     *
     * @param gatt GATT client the characteristic is associated with
     * @param characteristic Characteristic that has been updated as a result of a remote
     *     notification event.
     * @deprecated Use {@link BluetoothGattCallback#onCharacteristicChanged(BluetoothGatt,
     *     BluetoothGattCharacteristic, byte[])} as it is memory safe by providing the
     *     characteristic value at the time of notification.
     */
    @Deprecated
    public void onCharacteristicChanged(
            BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {}

    /**
     * Callback triggered as a result of a remote characteristic notification. Note that the value
     * within the characteristic object may have changed since receiving the remote characteristic
     * notification, so check the parameter value for the value at the time of notification.
     *
     * @param gatt GATT client the characteristic is associated with
     * @param characteristic Characteristic that has been updated as a result of a remote
     *     notification event.
     * @param value notified characteristic value
     */
    public void onCharacteristicChanged(
            @NonNull BluetoothGatt gatt,
            @NonNull BluetoothGattCharacteristic characteristic,
            @NonNull byte[] value) {
        onCharacteristicChanged(gatt, characteristic);
    }

    /**
     * Callback reporting the result of a descriptor read operation.
     *
     * @param gatt GATT client invoked {@link BluetoothGatt#readDescriptor}
     * @param descriptor Descriptor that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation was completed
     *     successfully
     * @deprecated Use {@link BluetoothGattCallback#onDescriptorRead(BluetoothGatt,
     *     BluetoothGattDescriptor, int, byte[])} as it is memory safe by providing the descriptor
     *     value at the time it was read.
     */
    @Deprecated
    public void onDescriptorRead(
            BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {}

    /**
     * Callback reporting the result of a descriptor read operation.
     *
     * @param gatt GATT client invoked {@link BluetoothGatt#readDescriptor}
     * @param descriptor Descriptor that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation was completed
     *     successfully
     * @param value the descriptor value at the time of the read operation
     */
    public void onDescriptorRead(
            @NonNull BluetoothGatt gatt,
            @NonNull BluetoothGattDescriptor descriptor,
            int status,
            @NonNull byte[] value) {
        onDescriptorRead(gatt, descriptor, status);
    }

    /**
     * Callback indicating the result of a descriptor write operation.
     *
     * @param gatt GATT client invoked {@link BluetoothGatt#writeDescriptor}
     * @param descriptor Descriptor that was written to the associated remote device.
     * @param status The result of the write operation {@link BluetoothGatt#GATT_SUCCESS} if the
     *     operation succeeds.
     */
    public void onDescriptorWrite(
            BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {}

    /**
     * Callback invoked when a reliable write transaction has been completed.
     *
     * @param gatt GATT client invoked {@link BluetoothGatt#executeReliableWrite}
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the reliable write transaction was
     *     executed successfully
     */
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {}

    /**
     * Callback reporting the RSSI for a remote device connection.
     *
     * <p>This callback is triggered in response to the {@link BluetoothGatt#readRemoteRssi}
     * function.
     *
     * @param gatt GATT client invoked {@link BluetoothGatt#readRemoteRssi}
     * @param rssi The RSSI value for the remote device
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the RSSI was read successfully
     */
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {}

    /**
     * Callback indicating the MTU for a given device connection has changed.
     *
     * <p>This callback is triggered in response to the {@link BluetoothGatt#requestMtu} function,
     * or in response to a connection event.
     *
     * @param gatt GATT client invoked {@link BluetoothGatt#requestMtu}
     * @param mtu The new MTU size
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the MTU has been changed successfully
     */
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {}

    /**
     * Callback indicating the connection parameters were updated.
     *
     * @param gatt GATT client involved
     * @param interval Connection interval used on this connection, 1.25ms unit. Valid range is from
     *     6 (7.5ms) to 3200 (4000ms).
     * @param latency Worker latency for the connection in number of connection events. Valid range
     *     is from 0 to 499
     * @param timeout Supervision timeout for this connection, in 10ms unit. Valid range is from 10
     *     (0.1s) to 3200 (32s)
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the connection has been updated
     *     successfully
     * @hide
     */
    public void onConnectionUpdated(
            BluetoothGatt gatt, int interval, int latency, int timeout, int status) {}

    /**
     * Callback indicating service changed event is received
     *
     * <p>Receiving this event means that the GATT database is out of sync with the remote device.
     * {@link BluetoothGatt#discoverServices} should be called to re-discover the services.
     *
     * @param gatt GATT client involved
     */
    public void onServiceChanged(@NonNull BluetoothGatt gatt) {}

    /**
     * Callback indicating LE connection's subrate parameters have changed.
     *
     * @param gatt GATT client involved
     * @param subrateFactor for the LE connection.
     * @param latency Worker latency for the connection in number of connection events. Valid range
     *     is from 0 to 499
     * @param contNum Valid range is from 0 to 499.
     * @param timeout Supervision timeout for this connection, in 10ms unit. Valid range is from 10
     *     (0.1s) to 3200 (32s)
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if LE connection subrating has been changed
     *     successfully.
     * @hide
     */
    public void onSubrateChange(
            BluetoothGatt gatt,
            int subrateFactor,
            int latency,
            int contNum,
            int timeout,
            int status) {}
}

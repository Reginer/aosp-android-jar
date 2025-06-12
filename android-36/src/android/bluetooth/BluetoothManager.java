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

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import static com.android.modules.utils.build.SdkLevel.isAtLeastU;

import android.annotation.RequiresFeature;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SystemService;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * High level manager used to obtain an instance of an {@link BluetoothAdapter} and to conduct
 * overall Bluetooth Management.
 *
 * <p>Use {@link android.content.Context#getSystemService(java.lang.String)} with {@link
 * Context#BLUETOOTH_SERVICE} to create an {@link BluetoothManager}, then call {@link #getAdapter}
 * to obtain the {@link BluetoothAdapter}. <div class="special reference">
 *
 * <h3>Developer Guides</h3>
 *
 * <p>For more information about using BLUETOOTH, read the <a href=
 * "{@docRoot}guide/topics/connectivity/bluetooth.html">Bluetooth</a> developer guide. </div>
 *
 * @see Context#getSystemService
 * @see BluetoothAdapter#getDefaultAdapter()
 */
@SystemService(Context.BLUETOOTH_SERVICE)
@RequiresFeature(PackageManager.FEATURE_BLUETOOTH)
public final class BluetoothManager {
    private static final String TAG = BluetoothManager.class.getSimpleName();

    private final BluetoothAdapter mAdapter;
    private final Context mContext;

    /** @hide */
    public BluetoothManager(Context context) {
        if (isAtLeastU()) {
            // Pin the context DeviceId prevent the associated attribution source to be obsolete
            // TODO: b/343739429 -- pass the context to BluetoothAdapter constructor instead
            mContext = context.createDeviceContext(Context.DEVICE_ID_DEFAULT);
        } else {
            mContext = context;
        }
        mAdapter = BluetoothAdapter.createAdapter(mContext.getAttributionSource());
    }

    /**
     * Get the BluetoothAdapter for this device.
     *
     * @return the BluetoothAdapter
     */
    @RequiresNoPermission
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Get the current connection state of the profile to the remote device.
     *
     * <p>This is not specific to any application configuration but represents the connection state
     * of the local Bluetooth adapter for certain profile. This can be used by applications like
     * status bar which would just like to know the state of Bluetooth.
     *
     * @param device Remote bluetooth device.
     * @param profile GATT or GATT_SERVER
     * @return State of the profile connection. One of {@link BluetoothProfile#STATE_CONNECTED},
     *     {@link BluetoothProfile#STATE_CONNECTING}, {@link BluetoothProfile#STATE_DISCONNECTED},
     *     {@link BluetoothProfile#STATE_DISCONNECTING}
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public int getConnectionState(BluetoothDevice device, int profile) {
        List<BluetoothDevice> connectedDevices = getConnectedDevices(profile);
        for (BluetoothDevice connectedDevice : connectedDevices) {
            if (device.equals(connectedDevice)) {
                return BluetoothProfile.STATE_CONNECTED;
            }
        }

        return BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Get connected devices for the specified profile.
     *
     * <p>Return the set of devices which are in state {@link BluetoothProfile#STATE_CONNECTED}
     *
     * <p>This is not specific to any application configuration but represents the connection state
     * of Bluetooth for this profile. This can be used by applications like status bar which would
     * just like to know the state of Bluetooth.
     *
     * @param profile GATT or GATT_SERVER
     * @return List of devices. The list will be empty on error.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public List<BluetoothDevice> getConnectedDevices(int profile) {
        return getDevicesMatchingConnectionStates(
                profile, new int[] {BluetoothProfile.STATE_CONNECTED});
    }

    /**
     * Get a list of devices that match any of the given connection states.
     *
     * <p>If none of the devices match any of the given states, an empty list will be returned.
     *
     * <p>This is not specific to any application configuration but represents the connection state
     * of the local Bluetooth adapter for this profile. This can be used by applications like status
     * bar which would just like to know the state of the local adapter.
     *
     * @param profile GATT or GATT_SERVER
     * @param states Array of states. States can be one of {@link BluetoothProfile#STATE_CONNECTED},
     *     {@link BluetoothProfile#STATE_CONNECTING}, {@link BluetoothProfile#STATE_DISCONNECTED},
     *     {@link BluetoothProfile#STATE_DISCONNECTING},
     * @return List of devices. The list will be empty on error.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SuppressWarnings("AndroidFrameworkRethrowFromSystem") // iGatt is not system server
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int profile, int[] states) {
        if (profile != BluetoothProfile.GATT && profile != BluetoothProfile.GATT_SERVER) {
            throw new IllegalArgumentException("Profile not supported: " + profile);
        }

        List<BluetoothDevice> devices = new ArrayList<>();

        IBluetoothGatt iGatt = mAdapter.getBluetoothGatt();
        if (iGatt == null) {
            return devices;
        }
        try {
            devices =
                    Attributable.setAttributionSource(
                            iGatt.getDevicesMatchingConnectionStates(
                                    states, mContext.getAttributionSource()),
                            mContext.getAttributionSource());
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }

        return devices;
    }

    /**
     * Open a GATT Server The callback is used to deliver results to Caller, such as connection
     * status as well as the results of any other GATT server operations. The method returns a
     * BluetoothGattServer instance. You can use BluetoothGattServer to conduct GATT server
     * operations.
     *
     * @param context App context
     * @param callback GATT server callback handler that will receive asynchronous callbacks.
     * @return BluetoothGattServer instance
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public BluetoothGattServer openGattServer(
            Context context, BluetoothGattServerCallback callback) {

        return (openGattServer(context, callback, BluetoothDevice.TRANSPORT_AUTO));
    }

    /**
     * Open a GATT Server The callback is used to deliver results to Caller, such as connection
     * status as well as the results of any other GATT server operations. The method returns a
     * BluetoothGattServer instance. You can use BluetoothGattServer to conduct GATT server
     * operations.
     *
     * @param context App context
     * @param callback GATT server callback handler that will receive asynchronous callbacks.
     * @param eattSupport indicates if server should use eatt channel for notifications.
     * @return BluetoothGattServer instance
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public BluetoothGattServer openGattServer(
            Context context, BluetoothGattServerCallback callback, boolean eattSupport) {
        return (openGattServer(context, callback, BluetoothDevice.TRANSPORT_AUTO, eattSupport));
    }

    /**
     * Open a GATT Server The callback is used to deliver results to Caller, such as connection
     * status as well as the results of any other GATT server operations. The method returns a
     * BluetoothGattServer instance. You can use BluetoothGattServer to conduct GATT server
     * operations.
     *
     * @param context App context
     * @param callback GATT server callback handler that will receive asynchronous callbacks.
     * @param transport preferred transport for GATT connections to remote dual-mode devices {@link
     *     BluetoothDevice#TRANSPORT_AUTO} or {@link BluetoothDevice#TRANSPORT_BREDR} or {@link
     *     BluetoothDevice#TRANSPORT_LE}
     * @return BluetoothGattServer instance
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public BluetoothGattServer openGattServer(
            Context context, BluetoothGattServerCallback callback, int transport) {
        return (openGattServer(context, callback, transport, false));
    }

    /**
     * Open a GATT Server The callback is used to deliver results to Caller, such as connection
     * status as well as the results of any other GATT server operations. The method returns a
     * BluetoothGattServer instance. You can use BluetoothGattServer to conduct GATT server
     * operations.
     *
     * @param context App context
     * @param callback GATT server callback handler that will receive asynchronous callbacks.
     * @param transport preferred transport for GATT connections to remote dual-mode devices {@link
     *     BluetoothDevice#TRANSPORT_AUTO} or {@link BluetoothDevice#TRANSPORT_BREDR} or {@link
     *     BluetoothDevice#TRANSPORT_LE}
     * @param eattSupport indicates if server should use eatt channel for notifications.
     * @return BluetoothGattServer instance
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public BluetoothGattServer openGattServer(
            Context context,
            BluetoothGattServerCallback callback,
            int transport,
            boolean eattSupport) {
        if (context == null || callback == null) {
            throw new IllegalArgumentException("null parameter: " + context + " " + callback);
        }

        // TODO(Bluetooth) check whether platform support BLE
        //     Do the check here or in GattServer?

        IBluetoothGatt iGatt = mAdapter.getBluetoothGatt();
        if (iGatt == null) {
            Log.e(TAG, "Fail to get GATT Server connection");
            return null;
        }
        BluetoothGattServer mGattServer = new BluetoothGattServer(iGatt, transport, mAdapter);
        Boolean regStatus = mGattServer.registerCallback(callback, eattSupport);
        return regStatus ? mGattServer : null;
    }
}

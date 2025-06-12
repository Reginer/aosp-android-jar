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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.content.AttributionSource;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Public API for the Bluetooth GATT Profile server role.
 *
 * <p>This class provides Bluetooth GATT server role functionality, allowing applications to create
 * Bluetooth Smart services and characteristics.
 *
 * <p>BluetoothGattServer is a proxy object for controlling the Bluetooth Service via IPC. Use
 * {@link BluetoothManager#openGattServer} to get an instance of this class.
 */
public final class BluetoothGattServer implements BluetoothProfile {
    private static final String TAG = "BluetoothGattServer";

    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    private final IBluetoothGatt mService;
    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;

    private BluetoothGattServerCallback mCallback;

    private final Object mServerIfLock = new Object();
    private int mServerIf;
    private int mTransport;
    private BluetoothGattService mPendingService;
    private List<BluetoothGattService> mServices;

    private static final int CALLBACK_REG_TIMEOUT = 10000;
    // Max length of an attribute value, defined in gatt_api.h
    private static final int GATT_MAX_ATTR_LEN = 512;

    /** Bluetooth GATT interface callbacks */
    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IBluetoothGattServerCallback mBluetoothGattServerCallback =
            new IBluetoothGattServerCallback.Stub() {
                /**
                 * Application interface registered - app is ready to go
                 *
                 * @hide
                 */
                @Override
                public void onServerRegistered(int status, int serverIf) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onServerRegistered() - status="
                                        + status
                                        + " serverIf="
                                        + serverIf);
                    }
                    synchronized (mServerIfLock) {
                        if (mCallback != null) {
                            mServerIf = serverIf;
                            mServerIfLock.notify();
                        } else {
                            // registration timeout
                            Log.e(TAG, "onServerRegistered: mCallback is null");
                        }
                    }
                }

                /**
                 * Server connection state changed
                 *
                 * @hide
                 */
                @Override
                public void onServerConnectionState(
                        int status, int serverIf, boolean connected, String address) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onServerConnectionState() - status="
                                        + status
                                        + " serverIf="
                                        + serverIf
                                        + " connected="
                                        + connected
                                        + " device="
                                        + BluetoothUtils.toAnonymizedAddress(address));
                    }
                    try {
                        mCallback.onConnectionStateChange(
                                mAdapter.getRemoteDevice(address),
                                status,
                                connected
                                        ? BluetoothProfile.STATE_CONNECTED
                                        : BluetoothProfile.STATE_DISCONNECTED);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception in callback", ex);
                    }
                }

                /**
                 * Service has been added
                 *
                 * @hide
                 */
                @Override
                public void onServiceAdded(int status, BluetoothGattService service) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onServiceAdded() - handle="
                                        + service.getInstanceId()
                                        + " uuid="
                                        + service.getUuid()
                                        + " status="
                                        + status);
                    }

                    if (mPendingService == null) {
                        return;
                    }

                    BluetoothGattService tmp = mPendingService;
                    mPendingService = null;

                    // Rewrite newly assigned handles to existing service.
                    tmp.setInstanceId(service.getInstanceId());
                    List<BluetoothGattCharacteristic> temp_chars = tmp.getCharacteristics();
                    List<BluetoothGattCharacteristic> svc_chars = service.getCharacteristics();
                    for (int i = 0; i < svc_chars.size(); i++) {
                        BluetoothGattCharacteristic temp_char = temp_chars.get(i);
                        BluetoothGattCharacteristic svc_char = svc_chars.get(i);

                        temp_char.setInstanceId(svc_char.getInstanceId());

                        List<BluetoothGattDescriptor> temp_descs = temp_char.getDescriptors();
                        List<BluetoothGattDescriptor> svc_descs = svc_char.getDescriptors();
                        for (int j = 0; j < svc_descs.size(); j++) {
                            temp_descs.get(j).setInstanceId(svc_descs.get(j).getInstanceId());
                        }
                    }

                    mServices.add(tmp);

                    try {
                        mCallback.onServiceAdded((int) status, tmp);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception in callback", ex);
                    }
                }

                /**
                 * Remote client characteristic read request.
                 *
                 * @hide
                 */
                @Override
                public void onCharacteristicReadRequest(
                        String address, int transId, int offset, boolean isLong, int handle) {
                    if (VDBG) Log.d(TAG, "onCharacteristicReadRequest() - handle=" + handle);

                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    BluetoothGattCharacteristic characteristic = getCharacteristicByHandle(handle);
                    if (characteristic == null) {
                        Log.w(TAG, "onCharacteristicReadRequest() no char for handle " + handle);
                        return;
                    }

                    try {
                        mCallback.onCharacteristicReadRequest(
                                device, transId, offset, characteristic);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception in callback", ex);
                    }
                }

                /**
                 * Remote client descriptor read request.
                 *
                 * @hide
                 */
                @Override
                public void onDescriptorReadRequest(
                        String address, int transId, int offset, boolean isLong, int handle) {
                    if (VDBG) Log.d(TAG, "onCharacteristicReadRequest() - handle=" + handle);

                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    BluetoothGattDescriptor descriptor = getDescriptorByHandle(handle);
                    if (descriptor == null) {
                        Log.w(TAG, "onDescriptorReadRequest() no desc for handle " + handle);
                        return;
                    }

                    try {
                        mCallback.onDescriptorReadRequest(device, transId, offset, descriptor);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception in callback", ex);
                    }
                }

                /**
                 * Remote client characteristic write request.
                 *
                 * @hide
                 */
                @Override
                public void onCharacteristicWriteRequest(
                        String address,
                        int transId,
                        int offset,
                        int length,
                        boolean isPrep,
                        boolean needRsp,
                        int handle,
                        byte[] value) {
                    if (VDBG) Log.d(TAG, "onCharacteristicWriteRequest() - handle=" + handle);

                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    BluetoothGattCharacteristic characteristic = getCharacteristicByHandle(handle);
                    if (characteristic == null) {
                        Log.w(TAG, "onCharacteristicWriteRequest() no char for handle " + handle);
                        return;
                    }

                    try {
                        mCallback.onCharacteristicWriteRequest(
                                device, transId, characteristic, isPrep, needRsp, offset, value);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception in callback", ex);
                    }
                }

                /**
                 * Remote client descriptor write request.
                 *
                 * @hide
                 */
                @Override
                public void onDescriptorWriteRequest(
                        String address,
                        int transId,
                        int offset,
                        int length,
                        boolean isPrep,
                        boolean needRsp,
                        int handle,
                        byte[] value) {
                    if (VDBG) Log.d(TAG, "onDescriptorWriteRequest() - handle=" + handle);

                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    BluetoothGattDescriptor descriptor = getDescriptorByHandle(handle);
                    if (descriptor == null) {
                        Log.w(TAG, "onDescriptorWriteRequest() no desc for handle " + handle);
                        return;
                    }

                    try {
                        mCallback.onDescriptorWriteRequest(
                                device, transId, descriptor, isPrep, needRsp, offset, value);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception in callback", ex);
                    }
                }

                /**
                 * Execute pending writes.
                 *
                 * @hide
                 */
                @Override
                public void onExecuteWrite(String address, int transId, boolean execWrite) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onExecuteWrite() - "
                                        + "device="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + ", transId="
                                        + transId
                                        + "execWrite="
                                        + execWrite);
                    }

                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    if (device == null) return;

                    try {
                        mCallback.onExecuteWrite(device, transId, execWrite);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception in callback", ex);
                    }
                }

                /**
                 * A notification/indication has been sent.
                 *
                 * @hide
                 */
                @Override
                public void onNotificationSent(String address, int status) {
                    if (VDBG) {
                        Log.d(
                                TAG,
                                "onNotificationSent() - "
                                        + "device="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + ", status="
                                        + status);
                    }

                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    if (device == null) return;

                    try {
                        mCallback.onNotificationSent(device, status);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception: " + ex);
                    }
                }

                /**
                 * The MTU for a connection has changed
                 *
                 * @hide
                 */
                @Override
                public void onMtuChanged(String address, int mtu) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onMtuChanged() - "
                                        + "device="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + ", mtu="
                                        + mtu);
                    }

                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    if (device == null) return;

                    try {
                        mCallback.onMtuChanged(device, mtu);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception: " + ex);
                    }
                }

                /**
                 * The PHY for a connection was updated
                 *
                 * @hide
                 */
                @Override
                public void onPhyUpdate(String address, int txPhy, int rxPhy, int status) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onPhyUpdate() - "
                                        + "device="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + ", txPHy="
                                        + txPhy
                                        + ", rxPHy="
                                        + rxPhy);
                    }

                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    if (device == null) return;

                    try {
                        mCallback.onPhyUpdate(device, txPhy, rxPhy, status);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception: " + ex);
                    }
                }

                /**
                 * The PHY for a connection was read
                 *
                 * @hide
                 */
                @Override
                public void onPhyRead(String address, int txPhy, int rxPhy, int status) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onPhyUpdate() - "
                                        + "device="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + ", txPHy="
                                        + txPhy
                                        + ", rxPHy="
                                        + rxPhy);
                    }

                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    if (device == null) return;

                    try {
                        mCallback.onPhyRead(device, txPhy, rxPhy, status);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception: " + ex);
                    }
                }

                /**
                 * Callback invoked when the given connection is updated
                 *
                 * @hide
                 */
                @Override
                public void onConnectionUpdated(
                        String address, int interval, int latency, int timeout, int status) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onConnectionUpdated() - Device="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + " interval="
                                        + interval
                                        + " latency="
                                        + latency
                                        + " timeout="
                                        + timeout
                                        + " status="
                                        + status);
                    }
                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    if (device == null) return;

                    try {
                        mCallback.onConnectionUpdated(device, interval, latency, timeout, status);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception: " + ex);
                    }
                }

                /**
                 * Callback invoked when the given connection's subrate parameters are changed
                 *
                 * @hide
                 */
                @Override
                public void onSubrateChange(
                        String address,
                        int subrateFactor,
                        int latency,
                        int contNum,
                        int timeout,
                        int status) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onSubrateChange() - "
                                        + "Device="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + ", subrateFactor="
                                        + subrateFactor
                                        + ", latency="
                                        + latency
                                        + ", contNum="
                                        + contNum
                                        + ", timeout="
                                        + timeout
                                        + ", status="
                                        + status);
                    }
                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    if (device == null) {
                        return;
                    }

                    try {
                        mCallback.onSubrateChange(
                                device, subrateFactor, latency, contNum, timeout, status);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception: " + ex);
                    }
                }
            };

    /** Create a BluetoothGattServer proxy object. */
    /* package */ BluetoothGattServer(
            IBluetoothGatt iGatt, int transport, BluetoothAdapter adapter) {
        mService = iGatt;
        mAdapter = adapter;
        mAttributionSource = adapter.getAttributionSource();
        mCallback = null;
        mServerIf = 0;
        mTransport = transport;
        mServices = new ArrayList<BluetoothGattService>();
    }

    /**
     * Get the identifier of the BluetoothGattServer, or 0 if it is closed
     *
     * @hide
     */
    @RequiresNoPermission
    public int getServerIf() {
        return mServerIf;
    }

    /**
     * Returns a characteristic with given handle.
     *
     * @hide
     */
    /*package*/ BluetoothGattCharacteristic getCharacteristicByHandle(int handle) {
        for (BluetoothGattService svc : mServices) {
            for (BluetoothGattCharacteristic charac : svc.getCharacteristics()) {
                if (charac.getInstanceId() == handle) {
                    return charac;
                }
            }
        }
        return null;
    }

    /**
     * Returns a descriptor with given handle.
     *
     * @hide
     */
    /*package*/ BluetoothGattDescriptor getDescriptorByHandle(int handle) {
        for (BluetoothGattService svc : mServices) {
            for (BluetoothGattCharacteristic charac : svc.getCharacteristics()) {
                for (BluetoothGattDescriptor desc : charac.getDescriptors()) {
                    if (desc.getInstanceId() == handle) {
                        return desc;
                    }
                }
            }
        }
        return null;
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceConnected(IBinder service) {}

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceDisconnected() {}

    /** @hide */
    @Override
    @RequiresNoPermission
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Close this GATT server instance.
     *
     * <p>Application should call this method as early as possible after it is done with this GATT
     * server.
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public void close() {
        if (DBG) Log.d(TAG, "close()");
        unregisterCallback();
    }

    /**
     * Register an application callback to start using GattServer.
     *
     * <p>This is an asynchronous call. The callback is used to notify success or failure if the
     * function returns true.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @return true, the callback will be called to notify success or failure, false on immediate
     *     error
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    /*package*/ boolean registerCallback(BluetoothGattServerCallback callback) {
        return registerCallback(callback, false);
    }

    /**
     * Register an application callback to start using GattServer.
     *
     * <p>This is an asynchronous call. The callback is used to notify success or failure if the
     * function returns true.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @param eattSupport indicates if server can use eatt
     * @return true, the callback will be called to notify success or failure, false on immediate
     *     error
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SuppressWarnings("WaitNotInLoop") // TODO(b/314811467)
    /*package*/ boolean registerCallback(
            BluetoothGattServerCallback callback, boolean eattSupport) {
        if (DBG) Log.d(TAG, "registerCallback()");
        if (mService == null) {
            Log.e(TAG, "GATT service not available");
            return false;
        }
        UUID uuid = UUID.randomUUID();
        if (DBG) Log.d(TAG, "registerCallback() - UUID=" + uuid);

        synchronized (mServerIfLock) {
            if (mCallback != null) {
                Log.e(TAG, "App can register callback only once");
                return false;
            }

            mCallback = callback;
            try {
                mService.registerServer(
                        new ParcelUuid(uuid),
                        mBluetoothGattServerCallback,
                        eattSupport,
                        mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                mCallback = null;
                return false;
            }

            try {
                mServerIfLock.wait(CALLBACK_REG_TIMEOUT);
            } catch (InterruptedException e) {
                Log.e(TAG, "" + e);
                mCallback = null;
            }

            if (mServerIf == 0) {
                mCallback = null;
                return false;
            } else {
                return true;
            }
        }
    }

    /** Unregister the current application and callbacks. */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    private void unregisterCallback() {
        if (DBG) Log.d(TAG, "unregisterCallback() - mServerIf=" + mServerIf);
        if (mService == null || mServerIf == 0) return;

        try {
            mCallback = null;
            mService.unregisterServer(mServerIf, mAttributionSource);
            mServerIf = 0;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Returns a service by UUID, instance and type.
     *
     * @hide
     */
    /*package*/ BluetoothGattService getService(UUID uuid, int instanceId, int type) {
        for (BluetoothGattService svc : mServices) {
            if (svc.getType() == type
                    && svc.getInstanceId() == instanceId
                    && svc.getUuid().equals(uuid)) {
                return svc;
            }
        }
        return null;
    }

    /**
     * Initiate a connection to a Bluetooth GATT capable device.
     *
     * <p>The connection may not be established right away, but will be completed when the remote
     * device is available. A {@link BluetoothGattServerCallback#onConnectionStateChange} callback
     * will be invoked when the connection state changes as a result of this function.
     *
     * <p>The autoConnect parameter determines whether to actively connect to the remote device, or
     * rather passively scan and finalize the connection when the remote device is in
     * range/available. Generally, the first ever connection to a device should be direct
     * (autoConnect set to false) and subsequent connections to known devices should be invoked with
     * the autoConnect parameter set to true.
     *
     * @param autoConnect Whether to directly connect to the remote device (false) or to
     *     automatically connect as soon as the remote device becomes available (true).
     * @return true, if the connection attempt was initiated successfully
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean connect(BluetoothDevice device, boolean autoConnect) {
        if (DBG) {
            Log.d(TAG, "connect() - device: " + device + ", auto: " + autoConnect);
        }
        if (mService == null || mServerIf == 0) return false;

        try {
            // autoConnect is inverse of "isDirect"
            mService.serverConnect(
                    mServerIf,
                    device.getAddress(),
                    device.getAddressType(),
                    !autoConnect,
                    mTransport,
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * Disconnects an established connection, or cancels a connection attempt currently in progress.
     *
     * @param device Remote device
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public void cancelConnection(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "cancelConnection() - device: " + device);
        if (mService == null || mServerIf == 0) return;

        try {
            mService.serverDisconnect(mServerIf, device.getAddress(), mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Set the preferred connection PHY for this app. Please note that this is just a
     * recommendation, whether the PHY change will happen depends on other applications preferences,
     * local and remote controller capabilities. Controller can override these settings.
     *
     * <p>{@link BluetoothGattServerCallback#onPhyUpdate} will be triggered as a result of this
     * call, even if no PHY change happens. It is also triggered when remote device updates the PHY.
     *
     * @param device The remote device to send this response to
     * @param txPhy preferred transmitter PHY. Bitwise OR of any of {@link
     *     BluetoothDevice#PHY_LE_1M_MASK}, {@link BluetoothDevice#PHY_LE_2M_MASK}, and {@link
     *     BluetoothDevice#PHY_LE_CODED_MASK}.
     * @param rxPhy preferred receiver PHY. Bitwise OR of any of {@link
     *     BluetoothDevice#PHY_LE_1M_MASK}, {@link BluetoothDevice#PHY_LE_2M_MASK}, and {@link
     *     BluetoothDevice#PHY_LE_CODED_MASK}.
     * @param phyOptions preferred coding to use when transmitting on the LE Coded PHY. Can be one
     *     of {@link BluetoothDevice#PHY_OPTION_NO_PREFERRED}, {@link BluetoothDevice#PHY_OPTION_S2}
     *     or {@link BluetoothDevice#PHY_OPTION_S8}
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public void setPreferredPhy(BluetoothDevice device, int txPhy, int rxPhy, int phyOptions) {
        try {
            mService.serverSetPreferredPhy(
                    mServerIf, device.getAddress(), txPhy, rxPhy, phyOptions, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Read the current transmitter PHY and receiver PHY of the connection. The values are returned
     * in {@link BluetoothGattServerCallback#onPhyRead}
     *
     * @param device The remote device to send this response to
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public void readPhy(BluetoothDevice device) {
        try {
            mService.serverReadPhy(mServerIf, device.getAddress(), mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Send a response to a read or write request to a remote device.
     *
     * <p>This function must be invoked in when a remote read/write request is received by one of
     * these callback methods:
     *
     * <ul>
     *   <li>{@link BluetoothGattServerCallback#onCharacteristicReadRequest}
     *   <li>{@link BluetoothGattServerCallback#onCharacteristicWriteRequest}
     *   <li>{@link BluetoothGattServerCallback#onDescriptorReadRequest}
     *   <li>{@link BluetoothGattServerCallback#onDescriptorWriteRequest}
     * </ul>
     *
     * @param device The remote device to send this response to
     * @param requestId The ID of the request that was received with the callback
     * @param status The status of the request to be sent to the remote devices
     * @param offset Value offset for partial read/write response
     * @param value The value of the attribute that was read/written (optional)
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean sendResponse(
            BluetoothDevice device, int requestId, int status, int offset, byte[] value) {
        if (VDBG) Log.d(TAG, "sendResponse() - device: " + device);
        if (mService == null || mServerIf == 0) return false;

        try {
            mService.sendResponse(
                    mServerIf,
                    device.getAddress(),
                    requestId,
                    status,
                    offset,
                    value,
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
        return true;
    }

    /**
     * Send a notification or indication that a local characteristic has been updated.
     *
     * <p>A notification or indication is sent to the remote device to signal that the
     * characteristic has been updated. This function should be invoked for every client that
     * requests notifications/indications by writing to the "Client Configuration" descriptor for
     * the given characteristic.
     *
     * @param device The remote device to receive the notification/indication
     * @param characteristic The local characteristic that has been updated
     * @param confirm true to request confirmation from the client (indication), false to send a
     *     notification
     * @return true, if the notification has been triggered successfully
     * @throws IllegalArgumentException if the characteristic value or service is null
     * @deprecated Use {@link BluetoothGattServer#notifyCharacteristicChanged(BluetoothDevice,
     *     BluetoothGattCharacteristic, boolean, byte[])} as this is not memory safe.
     */
    @Deprecated
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean notifyCharacteristicChanged(
            BluetoothDevice device, BluetoothGattCharacteristic characteristic, boolean confirm) {
        return notifyCharacteristicChanged(
                        device, characteristic, confirm, characteristic.getValue())
                == BluetoothStatusCodes.SUCCESS;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                BluetoothStatusCodes.SUCCESS,
                BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
                BluetoothStatusCodes.ERROR_DEVICE_NOT_CONNECTED,
                BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND,
                BluetoothStatusCodes.ERROR_UNKNOWN
            })
    public @interface NotifyCharacteristicReturnValues {}

    /**
     * Send a notification or indication that a local characteristic has been updated.
     *
     * <p>A notification or indication is sent to the remote device to signal that the
     * characteristic has been updated. This function should be invoked for every client that
     * requests notifications/indications by writing to the "Client Configuration" descriptor for
     * the given characteristic.
     *
     * @param device the remote device to receive the notification/indication
     * @param characteristic the local characteristic that has been updated
     * @param confirm {@code true} to request confirmation from the client (indication) or {@code
     *     false} to send a notification
     * @param value the characteristic value
     * @return whether the notification has been triggered successfully
     * @throws IllegalArgumentException if the characteristic value or service is null
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @NotifyCharacteristicReturnValues
    public int notifyCharacteristicChanged(
            @NonNull BluetoothDevice device,
            @NonNull BluetoothGattCharacteristic characteristic,
            boolean confirm,
            @NonNull byte[] value) {
        if (VDBG) Log.d(TAG, "notifyCharacteristicChanged() - device: " + device);
        if (mService == null || mServerIf == 0) {
            return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
        }

        if (characteristic == null) {
            throw new IllegalArgumentException("characteristic must not be null");
        }
        if (device == null) {
            throw new IllegalArgumentException("device must not be null");
        }
        if (value.length > GATT_MAX_ATTR_LEN) {
            throw new IllegalArgumentException(
                    "notification should not be longer than max length of an attribute value");
        }
        BluetoothGattService service = characteristic.getService();
        if (service == null) {
            throw new IllegalArgumentException("Characteristic must have a non-null service");
        }
        if (value == null) {
            throw new IllegalArgumentException("Characteristic value must not be null");
        }

        try {
            return mService.sendNotification(
                    mServerIf,
                    device.getAddress(),
                    characteristic.getInstanceId(),
                    confirm,
                    value,
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Add a service to the list of services to be hosted.
     *
     * <p>Once a service has been added to the list, the service and its included characteristics
     * will be provided by the local device.
     *
     * <p>If the local device has already exposed services when this function is called, a service
     * update notification will be sent to all clients.
     *
     * <p>The {@link BluetoothGattServerCallback#onServiceAdded} callback will indicate whether this
     * service has been added successfully. Do not add another service before this callback.
     *
     * @param service Service to be added to the list of services provided by this device.
     * @return true, if the request to add service has been initiated
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean addService(BluetoothGattService service) {
        if (DBG) Log.d(TAG, "addService() - service: " + service.getUuid());
        if (mService == null || mServerIf == 0) return false;

        mPendingService = service;

        try {
            mService.addService(mServerIf, service, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * Removes a service from the list of services to be provided.
     *
     * @param service Service to be removed.
     * @return true, if the service has been removed
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean removeService(BluetoothGattService service) {
        if (DBG) Log.d(TAG, "removeService() - service: " + service.getUuid());
        if (mService == null || mServerIf == 0) return false;

        BluetoothGattService intService =
                getService(service.getUuid(), service.getInstanceId(), service.getType());
        if (intService == null) return false;

        try {
            mService.removeService(mServerIf, service.getInstanceId(), mAttributionSource);
            mServices.remove(intService);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /** Remove all services from the list of provided services. */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public void clearServices() {
        if (DBG) Log.d(TAG, "clearServices()");
        if (mService == null || mServerIf == 0) return;

        try {
            mService.clearServices(mServerIf, mAttributionSource);
            mServices.clear();
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Returns a list of GATT services offered by this device.
     *
     * <p>An application must call {@link #addService} to add a service to the list of services
     * offered by this device.
     *
     * @return List of services. Returns an empty list if no services have been added yet.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public List<BluetoothGattService> getServices() {
        return mServices;
    }

    /**
     * Returns a {@link BluetoothGattService} from the list of services offered by this device.
     *
     * <p>If multiple instances of the same service (as identified by UUID) exist, the first
     * instance of the service is returned.
     *
     * @param uuid UUID of the requested service
     * @return BluetoothGattService if supported, or null if the requested service is not offered by
     *     this device.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public BluetoothGattService getService(UUID uuid) {
        for (BluetoothGattService service : mServices) {
            if (service.getUuid().equals(uuid)) {
                return service;
            }
        }

        return null;
    }

    /**
     * Not supported - please use {@link BluetoothManager#getConnectedDevices(int)} with {@link
     * BluetoothProfile#GATT} as argument
     *
     * @throws UnsupportedOperationException on every call
     */
    @Override
    @RequiresNoPermission
    public int getConnectionState(BluetoothDevice device) {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectionState instead.");
    }

    /**
     * Not supported - please use {@link BluetoothManager#getConnectedDevices(int)} with {@link
     * BluetoothProfile#GATT} as argument
     *
     * @throws UnsupportedOperationException on every call
     */
    @Override
    @RequiresNoPermission
    public List<BluetoothDevice> getConnectedDevices() {
        throw new UnsupportedOperationException(
                "Use BluetoothManager#getConnectedDevices instead.");
    }

    /**
     * Not supported - please use {@link BluetoothManager#getDevicesMatchingConnectionStates(int,
     * int[])} with {@link BluetoothProfile#GATT} as first argument
     *
     * @throws UnsupportedOperationException on every call
     */
    @Override
    @RequiresNoPermission
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        throw new UnsupportedOperationException(
                "Use BluetoothManager#getDevicesMatchingConnectionStates instead.");
    }
}

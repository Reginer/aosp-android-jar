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
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;
import static android.bluetooth.BluetoothUtils.logRemoteException;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic.WriteType;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AttributionSource;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.android.bluetooth.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public API for the Bluetooth GATT Profile.
 *
 * <p>This class provides Bluetooth GATT functionality to enable communication with Bluetooth Smart
 * or Smart Ready devices.
 *
 * <p>To connect to a remote peripheral device, create a {@link BluetoothGattCallback} and call
 * {@link BluetoothDevice#connectGatt} to get a instance of this class. GATT capable devices can be
 * discovered using the Bluetooth device discovery or BLE scan process.
 */
public final class BluetoothGatt implements BluetoothProfile {
    private static final String TAG = "BluetoothGatt";

    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    @UnsupportedAppUsage private IBluetoothGatt mService;
    @UnsupportedAppUsage private volatile BluetoothGattCallback mCallback;
    private Handler mHandler;
    @UnsupportedAppUsage private int mClientIf;
    private BluetoothDevice mDevice;
    @UnsupportedAppUsage private boolean mAutoConnect;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private int mAuthRetryState;

    private int mConnState;
    private final Object mStateLock = new Object();
    private final Object mDeviceBusyLock = new Object();

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private Boolean mDeviceBusy = false;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mTransport;

    private int mPhy;
    private boolean mOpportunistic;
    private final AttributionSource mAttributionSource;

    private static final int AUTH_RETRY_STATE_IDLE = 0;
    private static final int AUTH_RETRY_STATE_MITM = 2;

    private static final int CONN_STATE_IDLE = 0;
    private static final int CONN_STATE_CONNECTING = 1;
    private static final int CONN_STATE_CONNECTED = 2;
    private static final int CONN_STATE_CLOSED = 4;

    private static final int WRITE_CHARACTERISTIC_MAX_RETRIES = 5;
    private static final int WRITE_CHARACTERISTIC_TIME_TO_WAIT = 10; // milliseconds
    // Max length of an attribute value, defined in gatt_api.h
    private static final int GATT_MAX_ATTR_LEN = 512;

    private CopyOnWriteArrayList<BluetoothGattService> mServices;

    /** A GATT operation completed successfully */
    public static final int GATT_SUCCESS = 0;

    /** GATT read operation is not permitted */
    public static final int GATT_READ_NOT_PERMITTED = 0x2;

    /** GATT write operation is not permitted */
    public static final int GATT_WRITE_NOT_PERMITTED = 0x3;

    /** Insufficient authentication for a given operation */
    public static final int GATT_INSUFFICIENT_AUTHENTICATION = 0x5;

    /** The given request is not supported */
    public static final int GATT_REQUEST_NOT_SUPPORTED = 0x6;

    /** Insufficient encryption for a given operation */
    public static final int GATT_INSUFFICIENT_ENCRYPTION = 0xf;

    /** A read or write operation was requested with an invalid offset */
    public static final int GATT_INVALID_OFFSET = 0x7;

    /** Insufficient authorization for a given operation */
    public static final int GATT_INSUFFICIENT_AUTHORIZATION = 0x8;

    /** A write operation exceeds the maximum length of the attribute */
    public static final int GATT_INVALID_ATTRIBUTE_LENGTH = 0xd;

    /** A remote device connection is congested. */
    public static final int GATT_CONNECTION_CONGESTED = 0x8f;

    /**
     * GATT connection timed out, likely due to the remote device being out of range or not
     * advertising as connectable.
     */
    public static final int GATT_CONNECTION_TIMEOUT = 0x93;

    /** A GATT operation failed, errors other than the above */
    public static final int GATT_FAILURE = 0x101;

    /**
     * Connection parameter update - Use the connection parameters recommended by the Bluetooth SIG.
     * This is the default value if no connection parameter update is requested.
     */
    public static final int CONNECTION_PRIORITY_BALANCED = 0;

    /**
     * Connection parameter update - Request a high priority, low latency connection. An application
     * should only request high priority connection parameters to transfer large amounts of data
     * over LE quickly. Once the transfer is complete, the application should request {@link
     * BluetoothGatt#CONNECTION_PRIORITY_BALANCED} connection parameters to reduce energy use.
     */
    public static final int CONNECTION_PRIORITY_HIGH = 1;

    /** Connection parameter update - Request low power, reduced data rate connection parameters. */
    public static final int CONNECTION_PRIORITY_LOW_POWER = 2;

    /**
     * Connection parameter update - Request the priority preferred for Digital Car Key for a lower
     * latency connection. This connection parameter will consume more power than {@link
     * BluetoothGatt#CONNECTION_PRIORITY_BALANCED}, so it is recommended that apps do not use this
     * unless it specifically fits their use case.
     */
    public static final int CONNECTION_PRIORITY_DCK = 3;

    /**
     * Connection subrate request - Balanced.
     *
     * @hide
     */
    public static final int SUBRATE_REQUEST_MODE_BALANCED = 0;

    /**
     * Connection subrate request - High.
     *
     * @hide
     */
    public static final int SUBRATE_REQUEST_MODE_HIGH = 1;

    /**
     * Connection Subrate Request - Low Power.
     *
     * @hide
     */
    public static final int SUBRATE_REQUEST_MODE_LOW_POWER = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"SUBRATE_REQUEST_MODE"},
            value = {
                SUBRATE_REQUEST_MODE_BALANCED,
                SUBRATE_REQUEST_MODE_HIGH,
                SUBRATE_REQUEST_MODE_LOW_POWER,
            })
    public @interface SubrateRequestMode {}

    /**
     * No authentication required.
     *
     * @hide
     */
    /*package*/ static final int AUTHENTICATION_NONE = 0;

    /**
     * Authentication requested; no person-in-the-middle protection required.
     *
     * @hide
     */
    /*package*/ static final int AUTHENTICATION_NO_MITM = 1;

    /**
     * Authentication with person-in-the-middle protection requested.
     *
     * @hide
     */
    /*package*/ static final int AUTHENTICATION_MITM = 2;

    /** Bluetooth GATT callbacks. Overrides the default BluetoothGattCallback implementation. */
    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IBluetoothGattCallback mBluetoothGattCallback =
            new IBluetoothGattCallback.Stub() {
                /**
                 * Application interface registered - app is ready to go
                 *
                 * @hide
                 */
                @Override
                @SuppressLint("AndroidFrameworkRequiresPermission")
                public void onClientRegistered(int status, int clientIf) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onClientRegistered() -"
                                        + (" status=" + status)
                                        + (" clientIf=" + clientIf));
                    }
                    mClientIf = clientIf;
                    synchronized (mStateLock) {
                        if (mConnState == CONN_STATE_CLOSED) {
                            if (DBG) {
                                Log.d(
                                        TAG,
                                        "Client registration completed after closed,"
                                                + " unregistering");
                            }
                            unregisterApp();
                            if (Flags.unregisterGattClientDisconnected()) {
                                mCallback = null;
                            }
                            return;
                        }
                        if (VDBG) {
                            if (mConnState != CONN_STATE_CONNECTING) {
                                Log.e(TAG, "Bad connection state: " + mConnState);
                            }
                        }
                    }
                    if (status != GATT_SUCCESS) {
                        runOrQueueCallback(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        final BluetoothGattCallback callback = mCallback;
                                        if (callback != null) {
                                            callback.onConnectionStateChange(
                                                    BluetoothGatt.this,
                                                    GATT_FAILURE,
                                                    BluetoothProfile.STATE_DISCONNECTED);
                                        }
                                    }
                                });

                        synchronized (mStateLock) {
                            mConnState = CONN_STATE_IDLE;
                        }
                        return;
                    }
                    try {
                        // autoConnect is inverse of "isDirect"
                        mService.clientConnect(
                                clientIf,
                                mDevice.getAddress(),
                                mDevice.getAddressType(),
                                !mAutoConnect,
                                mTransport,
                                mOpportunistic,
                                mPhy,
                                mAttributionSource);
                    } catch (RemoteException e) {
                        Log.e(TAG, "", e);
                    }
                }

                /**
                 * Phy update callback
                 *
                 * @hide
                 */
                @Override
                public void onPhyUpdate(String address, int txPhy, int rxPhy, int status) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onPhyUpdate() -"
                                        + (" status=" + status)
                                        + (" address="
                                                + BluetoothUtils.toAnonymizedAddress(address))
                                        + (" txPhy=" + txPhy)
                                        + (" rxPhy=" + rxPhy));
                    }
                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onPhyUpdate(
                                                BluetoothGatt.this, txPhy, rxPhy, status);
                                    }
                                }
                            });
                }

                /**
                 * Phy read callback
                 *
                 * @hide
                 */
                @Override
                public void onPhyRead(String address, int txPhy, int rxPhy, int status) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onPhyRead() -"
                                        + (" status=" + status)
                                        + (" address="
                                                + BluetoothUtils.toAnonymizedAddress(address))
                                        + (" txPhy=" + txPhy)
                                        + (" rxPhy=" + rxPhy));
                    }
                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onPhyRead(
                                                BluetoothGatt.this, txPhy, rxPhy, status);
                                    }
                                }
                            });
                }

                /**
                 * Client connection state changed
                 *
                 * @hide
                 */
                @Override
                @RequiresBluetoothConnectPermission
                @RequiresPermission(BLUETOOTH_CONNECT)
                public void onClientConnectionState(
                        int status, int clientIf, boolean connected, String address) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onClientConnectionState() -"
                                        + (" status=" + status)
                                        + (" clientIf=" + clientIf)
                                        + (" connected=" + connected)
                                        + (" device="
                                                + BluetoothUtils.toAnonymizedAddress(address)));
                    }
                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }
                    int profileState =
                            connected
                                    ? BluetoothProfile.STATE_CONNECTED
                                    : BluetoothProfile.STATE_DISCONNECTED;

                    if (Flags.unregisterGattClientDisconnected() && !connected && !mAutoConnect) {
                        unregisterApp();
                    }

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onConnectionStateChange(
                                                BluetoothGatt.this, status, profileState);
                                    }
                                }
                            });

                    synchronized (mStateLock) {
                        if (connected) {
                            mConnState = CONN_STATE_CONNECTED;
                        } else {
                            mConnState = CONN_STATE_IDLE;
                        }
                    }

                    synchronized (mDeviceBusyLock) {
                        mDeviceBusy = false;
                    }
                }

                /**
                 * Remote search has been completed. The internal object structure should now
                 * reflect the state of the remote device database. Let the application know that we
                 * are done at this point.
                 *
                 * @hide
                 */
                @Override
                public void onSearchComplete(
                        String address, List<BluetoothGattService> services, int status) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onSearchComplete() = address="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + " status="
                                        + status);
                    }
                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    for (BluetoothGattService s : services) {
                        // services we receive don't have device set properly.
                        s.setDevice(mDevice);
                    }

                    if (Flags.fixBluetoothGattGettingDuplicateServices()) {
                        mServices.clear();
                    }

                    mServices.addAll(services);

                    // Fix references to included services, as they doesn't point to right objects.
                    for (BluetoothGattService fixedService : mServices) {
                        ArrayList<BluetoothGattService> includedServices =
                                new ArrayList(fixedService.getIncludedServices());
                        fixedService.getIncludedServices().clear();

                        for (BluetoothGattService brokenRef : includedServices) {
                            BluetoothGattService includedService =
                                    getService(
                                            mDevice,
                                            brokenRef.getUuid(),
                                            brokenRef.getInstanceId());
                            if (includedService != null) {
                                fixedService.addIncludedService(includedService);
                            } else {
                                Log.e(TAG, "Broken GATT database: can't find included service.");
                            }
                        }
                    }

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onServicesDiscovered(BluetoothGatt.this, status);
                                    }
                                }
                            });
                }

                /**
                 * Remote characteristic has been read. Updates the internal value.
                 *
                 * @hide
                 */
                @Override
                @SuppressLint("AndroidFrameworkRequiresPermission")
                public void onCharacteristicRead(
                        String address, int status, int handle, byte[] value) {
                    if (VDBG) {
                        Log.d(
                                TAG,
                                "onCharacteristicRead() -"
                                        + (" Device=" + BluetoothUtils.toAnonymizedAddress(address))
                                        + (" handle=" + handle)
                                        + (" Status=" + status));
                    }

                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    synchronized (mDeviceBusyLock) {
                        mDeviceBusy = false;
                    }

                    int clientIf = mClientIf;
                    if ((status == GATT_INSUFFICIENT_AUTHENTICATION
                                    || status == GATT_INSUFFICIENT_ENCRYPTION)
                            && (mAuthRetryState != AUTH_RETRY_STATE_MITM)
                            && (clientIf > 0)) {
                        try {
                            final int authReq =
                                    (mAuthRetryState == AUTH_RETRY_STATE_IDLE)
                                            ? AUTHENTICATION_NO_MITM
                                            : AUTHENTICATION_MITM;
                            mService.readCharacteristic(
                                    clientIf, address, handle, authReq, mAttributionSource);
                            mAuthRetryState++;
                            return;
                        } catch (RemoteException e) {
                            Log.e(TAG, "", e);
                        }
                    }

                    mAuthRetryState = AUTH_RETRY_STATE_IDLE;

                    BluetoothGattCharacteristic characteristic =
                            getCharacteristicById(mDevice, handle);
                    if (characteristic == null) {
                        Log.w(TAG, "onCharacteristicRead() failed to find characteristic!");
                        return;
                    }

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        if (status == 0) characteristic.setValue(value);
                                        callback.onCharacteristicRead(
                                                BluetoothGatt.this, characteristic, value, status);
                                    }
                                }
                            });
                }

                /**
                 * Characteristic has been written to the remote device. Let the app know how we
                 * did...
                 *
                 * @hide
                 */
                @Override
                @SuppressLint("AndroidFrameworkRequiresPermission")
                public void onCharacteristicWrite(
                        String address, int status, int handle, byte[] value) {
                    if (VDBG) {
                        Log.d(
                                TAG,
                                "onCharacteristicWrite() -"
                                        + (" Device=" + BluetoothUtils.toAnonymizedAddress(address))
                                        + (" handle=" + handle)
                                        + (" Status=" + status));
                    }

                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    synchronized (mDeviceBusyLock) {
                        mDeviceBusy = false;
                    }

                    BluetoothGattCharacteristic characteristic =
                            getCharacteristicById(mDevice, handle);
                    if (characteristic == null) return;

                    if ((status == GATT_INSUFFICIENT_AUTHENTICATION
                                    || status == GATT_INSUFFICIENT_ENCRYPTION)
                            && (mAuthRetryState != AUTH_RETRY_STATE_MITM)) {
                        try {
                            final int authReq =
                                    (mAuthRetryState == AUTH_RETRY_STATE_IDLE)
                                            ? AUTHENTICATION_NO_MITM
                                            : AUTHENTICATION_MITM;
                            int requestStatus = BluetoothStatusCodes.ERROR_UNKNOWN;
                            int clientIf = mClientIf;
                            for (int i = 0;
                                    (i < WRITE_CHARACTERISTIC_MAX_RETRIES) && (clientIf > 0);
                                    i++) {
                                requestStatus =
                                        mService.writeCharacteristic(
                                                clientIf,
                                                address,
                                                handle,
                                                characteristic.getWriteType(),
                                                authReq,
                                                value,
                                                mAttributionSource);
                                if (requestStatus
                                        != BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY) {
                                    break;
                                }
                                try {
                                    Thread.sleep(WRITE_CHARACTERISTIC_TIME_TO_WAIT);
                                } catch (InterruptedException e) {
                                    Log.e(TAG, "", e);
                                }
                            }
                            mAuthRetryState++;
                            return;
                        } catch (RemoteException e) {
                            Log.e(TAG, "", e);
                        }
                    }

                    mAuthRetryState = AUTH_RETRY_STATE_IDLE;
                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onCharacteristicWrite(
                                                BluetoothGatt.this, characteristic, status);
                                    }
                                }
                            });
                }

                /**
                 * Remote characteristic has been updated. Updates the internal value.
                 *
                 * @hide
                 */
                @Override
                public void onNotify(String address, int handle, byte[] value) {
                    if (VDBG) {
                        Log.d(
                                TAG,
                                "onNotify() - address="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + " handle="
                                        + handle);
                    }
                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    BluetoothGattCharacteristic characteristic =
                            getCharacteristicById(mDevice, handle);
                    if (characteristic == null) return;

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        characteristic.setValue(value);
                                        callback.onCharacteristicChanged(
                                                BluetoothGatt.this, characteristic, value);
                                    }
                                }
                            });
                }

                /**
                 * Descriptor has been read.
                 *
                 * @hide
                 */
                @Override
                @SuppressLint("AndroidFrameworkRequiresPermission")
                public void onDescriptorRead(String address, int status, int handle, byte[] value) {
                    if (VDBG) {
                        Log.d(
                                TAG,
                                "onDescriptorRead() - address="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + " handle="
                                        + handle);
                    }

                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    synchronized (mDeviceBusyLock) {
                        mDeviceBusy = false;
                    }

                    BluetoothGattDescriptor descriptor = getDescriptorById(mDevice, handle);
                    if (descriptor == null) return;

                    int clientIf = mClientIf;
                    if ((status == GATT_INSUFFICIENT_AUTHENTICATION
                                    || status == GATT_INSUFFICIENT_ENCRYPTION)
                            && (mAuthRetryState != AUTH_RETRY_STATE_MITM)
                            && (clientIf > 0)) {
                        try {
                            final int authReq =
                                    (mAuthRetryState == AUTH_RETRY_STATE_IDLE)
                                            ? AUTHENTICATION_NO_MITM
                                            : AUTHENTICATION_MITM;
                            mService.readDescriptor(
                                    clientIf, address, handle, authReq, mAttributionSource);
                            mAuthRetryState++;
                            return;
                        } catch (RemoteException e) {
                            Log.e(TAG, "", e);
                        }
                    }

                    mAuthRetryState = AUTH_RETRY_STATE_IDLE;

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        if (status == 0) descriptor.setValue(value);
                                        callback.onDescriptorRead(
                                                BluetoothGatt.this, descriptor, status, value);
                                    }
                                }
                            });
                }

                /**
                 * Descriptor write operation complete.
                 *
                 * @hide
                 */
                @Override
                @SuppressLint("AndroidFrameworkRequiresPermission")
                public void onDescriptorWrite(
                        String address, int status, int handle, byte[] value) {
                    if (VDBG) {
                        Log.d(
                                TAG,
                                "onDescriptorWrite() - address="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + " handle="
                                        + handle);
                    }

                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    synchronized (mDeviceBusyLock) {
                        mDeviceBusy = false;
                    }

                    BluetoothGattDescriptor descriptor = getDescriptorById(mDevice, handle);
                    if (descriptor == null) return;

                    int clientIf = mClientIf;
                    if ((status == GATT_INSUFFICIENT_AUTHENTICATION
                                    || status == GATT_INSUFFICIENT_ENCRYPTION)
                            && (mAuthRetryState != AUTH_RETRY_STATE_MITM)
                            && (clientIf > 0)) {
                        try {
                            final int authReq =
                                    (mAuthRetryState == AUTH_RETRY_STATE_IDLE)
                                            ? AUTHENTICATION_NO_MITM
                                            : AUTHENTICATION_MITM;
                            mService.writeDescriptor(
                                    clientIf, address, handle, authReq, value, mAttributionSource);
                            mAuthRetryState++;
                            return;
                        } catch (RemoteException e) {
                            Log.e(TAG, "", e);
                        }
                    }

                    mAuthRetryState = AUTH_RETRY_STATE_IDLE;

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onDescriptorWrite(
                                                BluetoothGatt.this, descriptor, status);
                                    }
                                }
                            });
                }

                /**
                 * Prepared write transaction completed (or aborted)
                 *
                 * @hide
                 */
                @Override
                public void onExecuteWrite(String address, int status) {
                    if (VDBG) {
                        Log.d(
                                TAG,
                                "onExecuteWrite() - address="
                                        + BluetoothUtils.toAnonymizedAddress(address)
                                        + " status="
                                        + status);
                    }
                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    synchronized (mDeviceBusyLock) {
                        mDeviceBusy = false;
                    }

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onReliableWriteCompleted(
                                                BluetoothGatt.this, status);
                                    }
                                }
                            });
                }

                /**
                 * Remote device RSSI has been read
                 *
                 * @hide
                 */
                @Override
                public void onReadRemoteRssi(String address, int rssi, int status) {
                    if (VDBG) {
                        Log.d(
                                TAG,
                                "onReadRemoteRssi() -"
                                        + (" Device=" + BluetoothUtils.toAnonymizedAddress(address))
                                        + (" rssi=" + rssi)
                                        + (" status=" + status));
                    }
                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }
                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onReadRemoteRssi(BluetoothGatt.this, rssi, status);
                                    }
                                }
                            });
                }

                /**
                 * Callback invoked when the MTU for a given connection changes
                 *
                 * @hide
                 */
                @Override
                public void onConfigureMTU(String address, int mtu, int status) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onConfigureMTU() -"
                                        + (" Device=" + BluetoothUtils.toAnonymizedAddress(address))
                                        + (" mtu=" + mtu)
                                        + (" status=" + status));
                    }
                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onMtuChanged(BluetoothGatt.this, mtu, status);
                                    }
                                }
                            });
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
                                "onConnectionUpdated() -"
                                        + (" Device=" + BluetoothUtils.toAnonymizedAddress(address))
                                        + (" interval=" + interval)
                                        + (" latency=" + latency)
                                        + (" timeout=" + timeout)
                                        + (" status=" + status));
                    }
                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onConnectionUpdated(
                                                BluetoothGatt.this,
                                                interval,
                                                latency,
                                                timeout,
                                                status);
                                    }
                                }
                            });
                }

                /**
                 * Callback invoked when service changed event is received
                 *
                 * @hide
                 */
                @Override
                public void onServiceChanged(String address) {
                    if (DBG) {
                        Log.d(
                                TAG,
                                "onServiceChanged() - Device="
                                        + BluetoothUtils.toAnonymizedAddress(address));
                    }

                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onServiceChanged(BluetoothGatt.this);
                                    }
                                }
                            });
                }

                /**
                 * Callback invoked when the given connection's subrate is changed
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
                    Log.d(
                            TAG,
                            "onSubrateChange() - "
                                    + (" Device=" + BluetoothUtils.toAnonymizedAddress(address))
                                    + (" subrateFactor=" + subrateFactor)
                                    + (" latency=" + latency)
                                    + (" contNum=" + contNum)
                                    + (" timeout=" + timeout)
                                    + (" status=" + status));

                    if (!address.equals(mDevice.getAddress())) {
                        return;
                    }

                    runOrQueueCallback(
                            new Runnable() {
                                @Override
                                public void run() {
                                    final BluetoothGattCallback callback = mCallback;
                                    if (callback != null) {
                                        callback.onSubrateChange(
                                                BluetoothGatt.this,
                                                subrateFactor,
                                                latency,
                                                contNum,
                                                timeout,
                                                status);
                                    }
                                }
                            });
                }
            };

    /* package */ BluetoothGatt(
            IBluetoothGatt iGatt,
            BluetoothDevice device,
            int transport,
            boolean opportunistic,
            int phy,
            AttributionSource attributionSource) {
        mService = iGatt;
        mDevice = device;
        mTransport = transport;
        mPhy = phy;
        mOpportunistic = opportunistic;
        mAttributionSource = attributionSource;
        mServices = new CopyOnWriteArrayList<>();

        mConnState = CONN_STATE_IDLE;
        mAuthRetryState = AUTH_RETRY_STATE_IDLE;
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
        return null;
    }

    /**
     * Close this Bluetooth GATT client.
     *
     * <p>Application should call this method as early as possible after it is done with this GATT
     * client.
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public void close() {
        if (DBG) Log.d(TAG, "close()");

        unregisterApp();
        if (Flags.unregisterGattClientDisconnected()) {
            mCallback = null;
        }

        mConnState = CONN_STATE_CLOSED;
        mAuthRetryState = AUTH_RETRY_STATE_IDLE;
    }

    /**
     * Returns a service by UUID, instance and type.
     *
     * @hide
     */
    /*package*/ BluetoothGattService getService(BluetoothDevice device, UUID uuid, int instanceId) {
        for (BluetoothGattService svc : mServices) {
            if (svc.getDevice().equals(device)
                    && svc.getInstanceId() == instanceId
                    && svc.getUuid().equals(uuid)) {
                return svc;
            }
        }
        return null;
    }

    /**
     * Returns a characteristic with id equal to instanceId.
     *
     * @hide
     */
    /*package*/ BluetoothGattCharacteristic getCharacteristicById(
            BluetoothDevice device, int instanceId) {
        for (BluetoothGattService svc : mServices) {
            for (BluetoothGattCharacteristic charac : svc.getCharacteristics()) {
                if (charac.getInstanceId() == instanceId) {
                    return charac;
                }
            }
        }
        return null;
    }

    /**
     * Returns a descriptor with id equal to instanceId.
     *
     * @hide
     */
    /*package*/ BluetoothGattDescriptor getDescriptorById(BluetoothDevice device, int instanceId) {
        for (BluetoothGattService svc : mServices) {
            for (BluetoothGattCharacteristic charac : svc.getCharacteristics()) {
                for (BluetoothGattDescriptor desc : charac.getDescriptors()) {
                    if (desc.getInstanceId() == instanceId) {
                        return desc;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Queue the runnable on a {@link Handler} provided by the user, or execute the runnable
     * immediately if no Handler was provided.
     */
    private void runOrQueueCallback(final Runnable cb) {
        if (mHandler == null) {
            try {
                cb.run();
            } catch (Exception ex) {
                Log.w(TAG, "Unhandled exception in callback", ex);
            }
        } else {
            mHandler.post(cb);
        }
    }

    /**
     * Register an application callback to start using GATT.
     *
     * <p>This is an asynchronous call. If registration is successful, client connection will be
     * initiated.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @return If true, the callback will be called to notify success or failure, false on immediate
     *     error
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    private boolean registerApp(BluetoothGattCallback callback, Handler handler) {
        return registerApp(callback, handler, false);
    }

    /**
     * Register an application callback to start using GATT.
     *
     * <p>This is an asynchronous call. If registration is successful, client connection will be
     * initiated.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @param eattSupport indicate to allow for eatt support
     * @return If true, the callback will be called to notify success or failure, false on immediate
     *     error
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    private boolean registerApp(
            BluetoothGattCallback callback, Handler handler, boolean eattSupport) {
        if (DBG) Log.d(TAG, "registerApp()");
        if (mService == null) return false;

        mCallback = callback;
        mHandler = handler;
        UUID uuid = UUID.randomUUID();
        if (DBG) Log.d(TAG, "registerApp() - UUID=" + uuid);

        try {
            mService.registerClient(
                    new ParcelUuid(uuid), mBluetoothGattCallback, eattSupport, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /** Unregister the current application and callbacks. */
    @UnsupportedAppUsage
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    private void unregisterApp() {
        if (mService == null || mClientIf == 0) return;
        if (DBG) Log.d(TAG, "unregisterApp() - mClientIf=" + mClientIf);

        try {
            if (!Flags.unregisterGattClientDisconnected()) {
                mCallback = null;
            }
            mService.unregisterClient(mClientIf, mAttributionSource);
            mClientIf = 0;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Initiate a connection to a Bluetooth GATT capable device.
     *
     * <p>The connection may not be established right away, but will be completed when the remote
     * device is available. A {@link BluetoothGattCallback#onConnectionStateChange} callback will be
     * invoked when the connection state changes as a result of this function.
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
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    /*package*/ boolean connect(
            Boolean autoConnect, BluetoothGattCallback callback, Handler handler) {
        if (DBG) {
            Log.d(TAG, "connect() - device: " + mDevice + ", auto: " + autoConnect);
        }
        synchronized (mStateLock) {
            if (mConnState != CONN_STATE_IDLE) {
                throw new IllegalStateException("Not idle");
            }
            mConnState = CONN_STATE_CONNECTING;
        }

        mAutoConnect = autoConnect;

        if (!registerApp(callback, handler)) {
            synchronized (mStateLock) {
                mConnState = CONN_STATE_IDLE;
            }
            Log.e(TAG, "Failed to register callback");
            return false;
        }

        // The connection will continue in the onClientRegistered callback
        return true;
    }

    /**
     * Disconnects an established connection, or cancels a connection attempt currently in progress.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public void disconnect() {
        if (DBG) Log.d(TAG, "cancelOpen() - device: " + mDevice);
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return;

        try {
            mService.clientDisconnect(clientIf, mDevice.getAddress(), mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Connect back to remote device.
     *
     * <p>This method is used to re-connect to a remote device after the connection has been
     * dropped. If the device is not in range, the re-connection will be triggered once the device
     * is back in range.
     *
     * @return true, if the connection attempt was initiated successfully
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean connect() {
        int clientIf = mClientIf;
        if (mService == null) return false;
        if (clientIf == 0) {
            if (!Flags.unregisterGattClientDisconnected()) {
                return false;
            }
            synchronized (mStateLock) {
                if (mConnState != CONN_STATE_IDLE) {
                    return false;
                }
                mConnState = CONN_STATE_CONNECTING;
            }

            UUID uuid = UUID.randomUUID();
            if (DBG) Log.d(TAG, "reconnect from connect(), UUID=" + uuid);

            try {
                mService.registerClient(
                        new ParcelUuid(uuid),
                        mBluetoothGattCallback,
                        /* eatt_support= */ false,
                        mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                synchronized (mStateLock) {
                    mConnState = CONN_STATE_IDLE;
                }
                Log.e(TAG, "Failed to register callback");
                return false;
            }

            return true;
        }

        try {
            if (DBG) {
                Log.d(TAG, "connect(void) - device: " + mDevice + ", auto=" + mAutoConnect);
            }

            // autoConnect is inverse of "isDirect"
            mService.clientConnect(
                    clientIf,
                    mDevice.getAddress(),
                    mDevice.getAddressType(),
                    !mAutoConnect,
                    mTransport,
                    mOpportunistic,
                    mPhy,
                    mAttributionSource);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    /**
     * Set the preferred connection PHY for this app. Please note that this is just a
     * recommendation, whether the PHY change will happen depends on other applications preferences,
     * local and remote controller capabilities. Controller can override these settings.
     *
     * <p>{@link BluetoothGattCallback#onPhyUpdate} will be triggered as a result of this call, even
     * if no PHY change happens. It is also triggered when remote device updates the PHY.
     *
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
    public void setPreferredPhy(int txPhy, int rxPhy, int phyOptions) {
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return;

        try {
            mService.clientSetPreferredPhy(
                    clientIf, mDevice.getAddress(), txPhy, rxPhy, phyOptions, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Read the current transmitter PHY and receiver PHY of the connection. The values are returned
     * in {@link BluetoothGattCallback#onPhyRead}
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public void readPhy() {
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return;

        try {
            mService.clientReadPhy(clientIf, mDevice.getAddress(), mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Return the remote bluetooth device this GATT client targets to
     *
     * @return remote bluetooth device
     */
    @RequiresNoPermission
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    /**
     * Discovers services offered by a remote device as well as their characteristics and
     * descriptors.
     *
     * <p>This is an asynchronous operation. Once service discovery is completed, the {@link
     * BluetoothGattCallback#onServicesDiscovered} callback is triggered. If the discovery was
     * successful, the remote services can be retrieved using the {@link #getServices} function.
     *
     * @return true, if the remote service discovery has been started
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean discoverServices() {
        if (DBG) Log.d(TAG, "discoverServices() - device: " + mDevice);

        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        if (!Flags.fixBluetoothGattGettingDuplicateServices()) {
            mServices.clear();
        }

        try {
            mService.discoverServices(clientIf, mDevice.getAddress(), mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * Discovers a service by UUID. This is exposed only for passing PTS tests. It should never be
     * used by real applications. The service is not searched for characteristics and descriptors,
     * or returned in any callback.
     *
     * @return true, if the remote service discovery has been started
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean discoverServiceByUuid(UUID uuid) {
        if (DBG) Log.d(TAG, "discoverServiceByUuid() - device: " + mDevice);
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        if (!Flags.fixBluetoothGattGettingDuplicateServices()) {
            mServices.clear();
        }

        try {
            mService.discoverServiceByUuid(
                    clientIf, mDevice.getAddress(), new ParcelUuid(uuid), mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
        return true;
    }

    /**
     * Returns a list of GATT services offered by the remote device.
     *
     * <p>This function requires that service discovery has been completed for the given device.
     *
     * @return List of services on the remote device. Returns an empty list if service discovery has
     *     not yet been performed.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public List<BluetoothGattService> getServices() {
        List<BluetoothGattService> result = new ArrayList<BluetoothGattService>();

        for (BluetoothGattService service : mServices) {
            if (service.getDevice().equals(mDevice)) {
                result.add(service);
            }
        }

        return result;
    }

    /**
     * Returns a {@link BluetoothGattService}, if the requested UUID is supported by the remote
     * device.
     *
     * <p>This function requires that service discovery has been completed for the given device.
     *
     * <p>If multiple instances of the same service (as identified by UUID) exist, the first
     * instance of the service is returned.
     *
     * @param uuid UUID of the requested service
     * @return BluetoothGattService if supported, or null if the requested service is not offered by
     *     the remote device.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresNoPermission
    public BluetoothGattService getService(UUID uuid) {
        for (BluetoothGattService service : mServices) {
            if (service.getDevice().equals(mDevice) && service.getUuid().equals(uuid)) {
                return service;
            }
        }

        return null;
    }

    /**
     * Reads the requested characteristic from the associated remote device.
     *
     * <p>This is an asynchronous operation. The result of the read operation is reported by the
     * {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic,
     * byte[], int)} callback.
     *
     * @param characteristic Characteristic to read from the remote device
     * @return true, if the read operation was initiated successfully
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
            return false;
        }

        if (VDBG) Log.d(TAG, "readCharacteristic() - uuid: " + characteristic.getUuid());
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null) return false;

        BluetoothDevice device = service.getDevice();
        if (device == null) return false;

        synchronized (mDeviceBusyLock) {
            if (mDeviceBusy) return false;
            mDeviceBusy = true;
        }

        try {
            mService.readCharacteristic(
                    clientIf,
                    device.getAddress(),
                    characteristic.getInstanceId(),
                    AUTHENTICATION_NONE,
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            synchronized (mDeviceBusyLock) {
                mDeviceBusy = false;
            }
            return false;
        }

        return true;
    }

    /**
     * Reads the characteristic using its UUID from the associated remote device.
     *
     * <p>This is an asynchronous operation. The result of the read operation is reported by the
     * {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic,
     * byte[], int)} callback.
     *
     * @param uuid UUID of characteristic to read from the remote device
     * @return true, if the read operation was initiated successfully
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean readUsingCharacteristicUuid(UUID uuid, int startHandle, int endHandle) {
        if (VDBG) Log.d(TAG, "readUsingCharacteristicUuid() - uuid: " + uuid);
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        synchronized (mDeviceBusyLock) {
            if (mDeviceBusy) return false;
            mDeviceBusy = true;
        }

        try {
            mService.readUsingCharacteristicUuid(
                    clientIf,
                    mDevice.getAddress(),
                    new ParcelUuid(uuid),
                    startHandle,
                    endHandle,
                    AUTHENTICATION_NONE,
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            synchronized (mDeviceBusyLock) {
                mDeviceBusy = false;
            }
            return false;
        }

        return true;
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * <p>Once the write operation has been completed, the {@link
     * BluetoothGattCallback#onCharacteristicWrite} callback is invoked, reporting the result of the
     * operation.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return true, if the write operation was initiated successfully
     * @throws IllegalArgumentException if characteristic or its value are null
     * @deprecated Use {@link BluetoothGatt#writeCharacteristic(BluetoothGattCharacteristic, byte[],
     *     int)} as this is not memory safe because it relies on a {@link
     *     BluetoothGattCharacteristic} object whose underlying fields are subject to change outside
     *     this method.
     */
    @Deprecated
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        try {
            return writeCharacteristic(
                            characteristic,
                            characteristic.getValue(),
                            characteristic.getWriteType())
                    == BluetoothStatusCodes.SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                BluetoothStatusCodes.SUCCESS,
                BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
                BluetoothStatusCodes.ERROR_DEVICE_NOT_CONNECTED,
                BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND,
                BluetoothStatusCodes.ERROR_GATT_WRITE_NOT_ALLOWED,
                BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY,
                BluetoothStatusCodes.ERROR_UNKNOWN
            })
    public @interface WriteOperationReturnValues {}

    /**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * <p>Once the write operation has been completed, the {@link
     * BluetoothGattCallback#onCharacteristicWrite} callback is invoked, reporting the result of the
     * operation.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return whether the characteristic was successfully written to
     * @throws IllegalArgumentException if characteristic or value are null
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @WriteOperationReturnValues
    public int writeCharacteristic(
            @NonNull BluetoothGattCharacteristic characteristic,
            @NonNull byte[] value,
            @WriteType int writeType) {
        if (characteristic == null) {
            throw new IllegalArgumentException("characteristic must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (value.length > GATT_MAX_ATTR_LEN) {
            throw new IllegalArgumentException(
                    "value should not be longer than max length of an attribute value");
        }
        if (VDBG) Log.d(TAG, "writeCharacteristic() - uuid: " + characteristic.getUuid());
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
                && (characteristic.getProperties()
                                & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
                        == 0) {
            return BluetoothStatusCodes.ERROR_GATT_WRITE_NOT_ALLOWED;
        }
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) {
            return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
        }

        BluetoothGattService service = characteristic.getService();
        if (service == null) {
            throw new IllegalArgumentException("Characteristic must have a non-null service");
        }

        BluetoothDevice device = service.getDevice();
        if (device == null) {
            throw new IllegalArgumentException("Service must have a non-null device");
        }

        synchronized (mDeviceBusyLock) {
            if (mDeviceBusy) {
                return BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY;
            }
            mDeviceBusy = true;
        }

        int requestStatus = BluetoothStatusCodes.ERROR_UNKNOWN;
        try {
            for (int i = 0; i < WRITE_CHARACTERISTIC_MAX_RETRIES; i++) {
                requestStatus =
                        mService.writeCharacteristic(
                                clientIf,
                                device.getAddress(),
                                characteristic.getInstanceId(),
                                writeType,
                                AUTHENTICATION_NONE,
                                value,
                                mAttributionSource);
                if (requestStatus != BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY) {
                    break;
                }
                try {
                    Thread.sleep(WRITE_CHARACTERISTIC_TIME_TO_WAIT);
                } catch (InterruptedException e) {
                    Log.e(TAG, "", e);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            synchronized (mDeviceBusyLock) {
                mDeviceBusy = false;
            }
            throw e.rethrowAsRuntimeException();
        }
        if (requestStatus != BluetoothStatusCodes.SUCCESS) {
            synchronized (mDeviceBusyLock) {
                mDeviceBusy = false;
            }
        }

        return requestStatus;
    }

    /**
     * Reads the value for a given descriptor from the associated remote device.
     *
     * <p>Once the read operation has been completed, the {@link
     * BluetoothGattCallback#onDescriptorRead} callback is triggered, signaling the result of the
     * operation.
     *
     * @param descriptor Descriptor value to read from the remote device
     * @return true, if the read operation was initiated successfully
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        if (VDBG) Log.d(TAG, "readDescriptor() - uuid: " + descriptor.getUuid());
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (characteristic == null) return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null) return false;

        BluetoothDevice device = service.getDevice();
        if (device == null) return false;

        synchronized (mDeviceBusyLock) {
            if (mDeviceBusy) return false;
            mDeviceBusy = true;
        }

        try {
            mService.readDescriptor(
                    clientIf,
                    device.getAddress(),
                    descriptor.getInstanceId(),
                    AUTHENTICATION_NONE,
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            synchronized (mDeviceBusyLock) {
                mDeviceBusy = false;
            }
            return false;
        }

        return true;
    }

    /**
     * Write the value of a given descriptor to the associated remote device.
     *
     * <p>A {@link BluetoothGattCallback#onDescriptorWrite} callback is triggered to report the
     * result of the write operation.
     *
     * @param descriptor Descriptor to write to the associated remote device
     * @return true, if the write operation was initiated successfully
     * @throws IllegalArgumentException if descriptor or its value are null
     * @deprecated Use {@link BluetoothGatt#writeDescriptor(BluetoothGattDescriptor, byte[])} as
     *     this is not memory safe because it relies on a {@link BluetoothGattDescriptor} object
     *     whose underlying fields are subject to change outside this method.
     */
    @Deprecated
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        try {
            return writeDescriptor(descriptor, descriptor.getValue())
                    == BluetoothStatusCodes.SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Write the value of a given descriptor to the associated remote device.
     *
     * <p>A {@link BluetoothGattCallback#onDescriptorWrite} callback is triggered to report the
     * result of the write operation.
     *
     * @param descriptor Descriptor to write to the associated remote device
     * @return true, if the write operation was initiated successfully
     * @throws IllegalArgumentException if descriptor or value are null
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @WriteOperationReturnValues
    public int writeDescriptor(@NonNull BluetoothGattDescriptor descriptor, @NonNull byte[] value) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (VDBG) Log.d(TAG, "writeDescriptor() - uuid: " + descriptor.getUuid());
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) {
            return BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND;
        }

        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (characteristic == null) {
            throw new IllegalArgumentException("Descriptor must have a non-null characteristic");
        }

        BluetoothGattService service = characteristic.getService();
        if (service == null) {
            throw new IllegalArgumentException("Characteristic must have a non-null service");
        }

        BluetoothDevice device = service.getDevice();
        if (device == null) {
            throw new IllegalArgumentException("Service must have a non-null device");
        }

        synchronized (mDeviceBusyLock) {
            if (mDeviceBusy) return BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY;
            mDeviceBusy = true;
        }

        try {
            return mService.writeDescriptor(
                    clientIf,
                    device.getAddress(),
                    descriptor.getInstanceId(),
                    AUTHENTICATION_NONE,
                    value,
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            synchronized (mDeviceBusyLock) {
                mDeviceBusy = false;
            }
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Initiates a reliable write transaction for a given remote device.
     *
     * <p>Once a reliable write transaction has been initiated, all calls to {@link
     * #writeCharacteristic} are sent to the remote device for verification and queued up for atomic
     * execution. The application will receive a {@link BluetoothGattCallback#onCharacteristicWrite}
     * callback in response to every {@link #writeCharacteristic(BluetoothGattCharacteristic,
     * byte[], int)} call and is responsible for verifying if the value has been transmitted
     * accurately.
     *
     * <p>After all characteristics have been queued up and verified, {@link #executeReliableWrite}
     * will execute all writes. If a characteristic was not written correctly, calling {@link
     * #abortReliableWrite} will cancel the current transaction without committing any values on the
     * remote device.
     *
     * @return true, if the reliable write transaction has been initiated
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean beginReliableWrite() {
        if (VDBG) Log.d(TAG, "beginReliableWrite() - device: " + mDevice);
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        try {
            mService.beginReliableWrite(clientIf, mDevice.getAddress(), mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * Executes a reliable write transaction for a given remote device.
     *
     * <p>This function will commit all queued up characteristic write operations for a given remote
     * device.
     *
     * <p>A {@link BluetoothGattCallback#onReliableWriteCompleted} callback is invoked to indicate
     * whether the transaction has been executed correctly.
     *
     * @return true, if the request to execute the transaction has been sent
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean executeReliableWrite() {
        if (VDBG) Log.d(TAG, "executeReliableWrite() - device: " + mDevice);
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        synchronized (mDeviceBusyLock) {
            if (mDeviceBusy) return false;
            mDeviceBusy = true;
        }

        try {
            mService.endReliableWrite(clientIf, mDevice.getAddress(), true, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            synchronized (mDeviceBusyLock) {
                mDeviceBusy = false;
            }
            return false;
        }

        return true;
    }

    /**
     * Cancels a reliable write transaction for a given device.
     *
     * <p>Calling this function will discard all queued characteristic write operations for a given
     * remote device.
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public void abortReliableWrite() {
        if (VDBG) Log.d(TAG, "abortReliableWrite() - device: " + mDevice);
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return;

        try {
            mService.endReliableWrite(clientIf, mDevice.getAddress(), false, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * @deprecated Use {@link #abortReliableWrite()}
     */
    @Deprecated
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public void abortReliableWrite(BluetoothDevice mDevice) {
        abortReliableWrite();
    }

    /**
     * Enable or disable notifications/indications for a given characteristic.
     *
     * <p>Once notifications are enabled for a characteristic, a {@link
     * BluetoothGattCallback#onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic,
     * byte[])} callback will be triggered if the remote device indicates that the given
     * characteristic has changed.
     *
     * @param characteristic The characteristic for which to enable notifications
     * @param enable Set to true to enable notifications/indications
     * @return true, if the requested notification status was set successfully
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enable) {
        if (DBG) {
            Log.d(
                    TAG,
                    "setCharacteristicNotification() - uuid: "
                            + characteristic.getUuid()
                            + " enable: "
                            + enable);
        }
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null) return false;

        BluetoothDevice device = service.getDevice();
        if (device == null) return false;

        try {
            mService.registerForNotification(
                    clientIf,
                    device.getAddress(),
                    characteristic.getInstanceId(),
                    enable,
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * Clears the internal cache and forces a refresh of the services from the remote device.
     *
     * @hide
     */
    @UnsupportedAppUsage
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean refresh() {
        if (DBG) Log.d(TAG, "refresh() - device: " + mDevice);
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        try {
            mService.refreshDevice(clientIf, mDevice.getAddress(), mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * Read the RSSI for a connected remote device.
     *
     * <p>The {@link BluetoothGattCallback#onReadRemoteRssi} callback will be invoked when the RSSI
     * value has been read.
     *
     * @return true, if the RSSI value has been requested successfully
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean readRemoteRssi() {
        if (DBG) Log.d(TAG, "readRssi() - device: " + mDevice);
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        try {
            mService.readRemoteRssi(clientIf, mDevice.getAddress(), mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * Request an MTU size used for a given connection. Please note that starting from Android 14,
     * the Android Bluetooth stack requests the BLE ATT MTU to 517 bytes when the first GATT client
     * requests an MTU, and disregards all subsequent MTU requests. Check out <a
     * href="{@docRoot}about/versions/14/behavior-changes-all#mtu-set-to-517">MTU is set to 517 for
     * the first GATT client requesting an MTU</a> for more information.
     *
     * <p>When performing a write request operation (write without response), the data sent is
     * truncated to the MTU size. This function may be used to request a larger MTU size to be able
     * to send more data at once.
     *
     * <p>A {@link BluetoothGattCallback#onMtuChanged} callback will indicate whether this operation
     * was successful.
     *
     * @return true, if the new MTU value has been requested successfully
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean requestMtu(int mtu) {
        if (DBG) {
            Log.d(TAG, "configureMTU() - device: " + mDevice + " mtu: " + mtu);
        }
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        try {
            mService.configureMTU(clientIf, mDevice.getAddress(), mtu, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * Request a connection parameter update.
     *
     * <p>This function will send a connection parameter update request to the remote device.
     *
     * @param connectionPriority Request a specific connection priority. Must be one of {@link
     *     BluetoothGatt#CONNECTION_PRIORITY_BALANCED}, {@link
     *     BluetoothGatt#CONNECTION_PRIORITY_HIGH} {@link
     *     BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}, or {@link
     *     BluetoothGatt#CONNECTION_PRIORITY_DCK}.
     * @throws IllegalArgumentException If the parameters are outside of their specified range.
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean requestConnectionPriority(int connectionPriority) {
        if (connectionPriority < CONNECTION_PRIORITY_BALANCED
                || connectionPriority > CONNECTION_PRIORITY_DCK) {
            throw new IllegalArgumentException("connectionPriority not within valid range");
        }

        if (DBG) Log.d(TAG, "requestConnectionPriority() - params: " + connectionPriority);
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        try {
            mService.connectionParameterUpdate(
                    clientIf, mDevice.getAddress(), connectionPriority, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * Request an LE connection parameter update.
     *
     * <p>This function will send an LE connection parameters update request to the remote device.
     *
     * @return true, if the request is send to the Bluetooth stack.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean requestLeConnectionUpdate(
            int minConnectionInterval,
            int maxConnectionInterval,
            int slaveLatency,
            int supervisionTimeout,
            int minConnectionEventLen,
            int maxConnectionEventLen) {
        if (DBG) {
            Log.d(
                    TAG,
                    "requestLeConnectionUpdate() - min=("
                            + minConnectionInterval
                            + ")"
                            + (1.25 * minConnectionInterval)
                            + "msec, max=("
                            + maxConnectionInterval
                            + ")"
                            + (1.25 * maxConnectionInterval)
                            + "msec, latency="
                            + slaveLatency
                            + ", timeout="
                            + supervisionTimeout
                            + "msec"
                            + ", min_ce="
                            + minConnectionEventLen
                            + ", max_ce="
                            + maxConnectionEventLen);
        }
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) return false;

        try {
            mService.leConnectionUpdate(
                    clientIf,
                    mDevice.getAddress(),
                    minConnectionInterval,
                    maxConnectionInterval,
                    slaveLatency,
                    supervisionTimeout,
                    minConnectionEventLen,
                    maxConnectionEventLen,
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * Request LE subrate mode.
     *
     * <p>This function will send a LE subrate request to the remote device.
     *
     * <p>This method requires the calling app to have the {@link
     * android.Manifest.permission#BLUETOOTH_CONNECT} permission. Additionally, an app must either
     * have the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} or be associated with the
     * Companion Device manager (see {@link android.companion.CompanionDeviceManager#associate(
     * AssociationRequest, android.companion.CompanionDeviceManager.Callback, Handler)})
     *
     * @param subrateMode Request a specific subrate mode.
     * @throws IllegalArgumentException If the parameters are outside of their specified range.
     * @return true, if the request is send to the Bluetooth stack.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED},
            conditional = true)
    public int requestSubrateMode(@SubrateRequestMode int subrateMode) {
        if (subrateMode < SUBRATE_REQUEST_MODE_BALANCED
                || subrateMode > SUBRATE_REQUEST_MODE_LOW_POWER) {
            throw new IllegalArgumentException("Subrate Mode not within valid range");
        }

        if (DBG) {
            Log.d(TAG, "requestsubrateMode(" + subrateMode + ")");
        }
        int clientIf = mClientIf;
        if (mService == null || clientIf == 0) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        }

        try {
            return mService.subrateModeRequest(clientIf, mDevice, subrateMode, mAttributionSource);
        } catch (RemoteException e) {
            logRemoteException(TAG, e);
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        }
    }

    /**
     * @deprecated Not supported - please use {@link BluetoothManager#getConnectedDevices(int)} with
     *     {@link BluetoothProfile#GATT} as argument
     * @throws UnsupportedOperationException on every call
     */
    @Override
    @RequiresNoPermission
    @Deprecated
    public int getConnectionState(BluetoothDevice device) {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectionState instead.");
    }

    /**
     * @deprecated Not supported - please use {@link BluetoothManager#getConnectedDevices(int)} with
     *     {@link BluetoothProfile#GATT} as argument
     * @throws UnsupportedOperationException on every call
     */
    @Override
    @RequiresNoPermission
    @Deprecated
    public List<BluetoothDevice> getConnectedDevices() {
        throw new UnsupportedOperationException(
                "Use BluetoothManager#getConnectedDevices instead.");
    }

    /**
     * @deprecated Not supported - please use {@link
     *     BluetoothManager#getDevicesMatchingConnectionStates(int, int[])} with {@link
     *     BluetoothProfile#GATT} as first argument
     * @throws UnsupportedOperationException on every call
     */
    @Override
    @RequiresNoPermission
    @Deprecated
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        throw new UnsupportedOperationException(
                "Use BluetoothManager#getDevicesMatchingConnectionStates instead.");
    }
}

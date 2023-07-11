/*
 * Copyright (C) 2008 The Android Open Source Project
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

import static android.bluetooth.BluetoothUtils.getSyncTimeout;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.IpcDataCache;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;
import android.util.Pair;

import com.android.modules.utils.SynchronousResultReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * This class provides the APIs to control the Bluetooth SIM
 * Access Profile (SAP).
 *
 * <p>BluetoothSap is a proxy object for controlling the Bluetooth
 * Service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get
 * the BluetoothSap proxy object.
 *
 * <p>Each method is protected with its appropriate permission.
 *
 * @hide
 */
@SystemApi
public final class BluetoothSap implements BluetoothProfile, AutoCloseable {

    private static final String TAG = "BluetoothSap";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    private CloseGuard mCloseGuard;

    /**
     * Intent used to broadcast the change in connection state of the profile.
     *
     * <p>This intent will have 3 extras:
     * <ul>
     * <li> {@link #EXTRA_STATE} - The current state of the profile. </li>
     * <li> {@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.</li>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of
     * {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING},
     * {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}.
     *
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SuppressLint("ActionValue")
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.sap.profile.action.CONNECTION_STATE_CHANGED";

    /**
     * There was an error trying to obtain the state.
     *
     * @hide
     */
    public static final int STATE_ERROR = -1;

    /**
     * Connection state change succceeded.
     *
     * @hide
     */
    public static final int RESULT_SUCCESS = 1;

    /**
     * Connection canceled before completion.
     *
     * @hide
     */
    public static final int RESULT_CANCELED = 2;

    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;
    private final BluetoothProfileConnector<IBluetoothSap> mProfileConnector =
            new BluetoothProfileConnector(this, BluetoothProfile.SAP,
                    "BluetoothSap", IBluetoothSap.class.getName()) {
                @Override
                public IBluetoothSap getServiceInterface(IBinder service) {
                    return IBluetoothSap.Stub.asInterface(service);
                }
    };

    /**
     * Create a BluetoothSap proxy object.
     */
    /* package */ BluetoothSap(Context context, ServiceListener listener,
            BluetoothAdapter adapter) {
        if (DBG) Log.d(TAG, "Create BluetoothSap proxy object");
        mAdapter = adapter;
        mAttributionSource = adapter.getAttributionSource();
        mProfileConnector.connect(context, listener);
        mCloseGuard = new CloseGuard();
        mCloseGuard.open("close");
    }

    /**
     *
     * @hide
     */
    protected void finalize() {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        close();
    }

    /**
     * Close the connection to the backing service.
     * Other public functions of BluetoothSap will return default error
     * results once close() has been called. Multiple invocations of close()
     * are ok.
     *
     * @hide
     */
    public synchronized void close() {
        mProfileConnector.disconnect();
    }

    private IBluetoothSap getService() {
        return mProfileConnector.getService();
    }

    /**
     * Get the current state of the BluetoothSap service.
     *
     * @return One of the STATE_ return codes, or STATE_ERROR if this proxy object is currently not
     * connected to the Sap service.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public int getState() {
        if (VDBG) log("getState()");
        final IBluetoothSap service = getService();
        final int defaultValue = BluetoothSap.STATE_ERROR;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getState(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the currently connected remote Bluetooth device (PCE).
     *
     * @return The remote Bluetooth device, or null if not in connected or connecting state, or if
     * this proxy object is not connected to the Sap service.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothDevice getClient() {
        if (VDBG) log("getClient()");
        final IBluetoothSap service = getService();
        final BluetoothDevice defaultValue = null;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<BluetoothDevice> recv =
                        SynchronousResultReceiver.get();
                service.getClient(mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Returns true if the specified Bluetooth device is connected.
     * Returns false if not connected, or if this proxy object is not
     * currently connected to the Sap service.
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean isConnected(BluetoothDevice device) {
        if (VDBG) log("isConnected(" + device + ")");
        final IBluetoothSap service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.isConnected(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Initiate connection. Initiation of outgoing connections is not
     * supported for SAP server.
     *
     * @hide
     */
    @RequiresNoPermission
    public boolean connect(BluetoothDevice device) {
        if (DBG) log("connect(" + device + ")" + "not supported for SAPS");
        return false;
    }

    /**
     * Initiate disconnect.
     *
     * @param device Remote Bluetooth Device
     * @return false on error, true otherwise
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean disconnect(BluetoothDevice device) {
        if (DBG) log("disconnect(" + device + ")");
        final IBluetoothSap service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.disconnect(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the list of connected devices. Currently at most one.
     *
     * @return list of connected devices
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public List<BluetoothDevice> getConnectedDevices() {
        if (DBG) log("getConnectedDevices()");
        final IBluetoothSap service = getService();
        final List<BluetoothDevice> defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                        SynchronousResultReceiver.get();
                service.getConnectedDevices(mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the list of devices matching specified states. Currently at most one.
     *
     * @return list of matching devices
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (DBG) log("getDevicesMatchingStates()");
        final IBluetoothSap service = getService();
        final List<BluetoothDevice> defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<List<BluetoothDevice>> recv =
                        SynchronousResultReceiver.get();
                service.getDevicesMatchingConnectionStates(states, mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * There are several instances of IpcDataCache used in this class.
     * BluetoothCache wraps up the common code.  All caches are created with a maximum of
     * eight entries, and the key is in the bluetooth module.  The name is set to the api.
     */
    private static class BluetoothCache<Q, R> extends IpcDataCache<Q, R> {
        BluetoothCache(String api, IpcDataCache.QueryHandler query) {
            super(8, IpcDataCache.MODULE_BLUETOOTH, api, api, query);
        }};

    /** @hide */
    public void disableBluetoothGetConnectionStateCache() {
        mBluetoothConnectionCache.disableForCurrentProcess();
    }

    /** @hide */
    public static void invalidateBluetoothGetConnectionStateCache() {
        invalidateCache(GET_CONNECTION_STATE_API);
    }

    /**
     * Invalidate a bluetooth cache.  This method is just a short-hand wrapper that
     * enforces the bluetooth module.
     */
    private static void invalidateCache(@NonNull String api) {
        IpcDataCache.invalidateCache(IpcDataCache.MODULE_BLUETOOTH, api);
    }

    private final IpcDataCache.QueryHandler<Pair<IBluetoothSap, BluetoothDevice>, Integer>
            mBluetoothConnectionQuery = new IpcDataCache.QueryHandler<>() {
                @RequiresBluetoothConnectPermission
                @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                @Override
                public Integer apply(Pair<IBluetoothSap, BluetoothDevice> pairQuery) {
                    if (DBG) {
                        log("getConnectionState(" + pairQuery.second.getAnonymizedAddress()
                                + ") uncached");
                    }
                    final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                    try {
                        pairQuery.first
                            .getConnectionState(pairQuery.second, mAttributionSource, recv);
                        return recv.awaitResultNoInterrupt(getSyncTimeout())
                            .getValue(BluetoothProfile.STATE_DISCONNECTED);
                    } catch (RemoteException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

    private static final String GET_CONNECTION_STATE_API = "BluetoothSap_getConnectionState";

    private final BluetoothCache<Pair<IBluetoothSap, BluetoothDevice>, Integer>
            mBluetoothConnectionCache = new BluetoothCache<>(GET_CONNECTION_STATE_API,
                mBluetoothConnectionQuery);

    /**
     * Get connection state of device
     *
     * @return device connection state
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public int getConnectionState(BluetoothDevice device) {
        if (DBG) log("getConnectionState(" + device + ")");
        final IBluetoothSap service = getService();
        if (service == null) {
            Log.w(TAG, "BT not enabled. Cannot get connection state");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return mBluetoothConnectionCache.query(new Pair<>(service, device));
            } catch (RuntimeException e) {
                if (!(e.getCause() instanceof TimeoutException)
                        && !(e.getCause() instanceof RemoteException)) {
                    throw e;
                }
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Set priority of the profile
     *
     * <p> The device should already be paired.
     * Priority can be one of {@link #PRIORITY_ON} or {@link #PRIORITY_OFF},
     *
     * @param device Paired bluetooth device
     * @param priority
     * @return true if priority is set, false on error
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setPriority(BluetoothDevice device, int priority) {
        if (DBG) log("setPriority(" + device + ", " + priority + ")");
        return setConnectionPolicy(device, BluetoothAdapter.priorityToConnectionPolicy(priority));
    }

    /**
     * Set connection policy of the profile
     *
     * <p> The device should already be paired.
     * Connection policy can be one of {@link #CONNECTION_POLICY_ALLOWED},
     * {@link #CONNECTION_POLICY_FORBIDDEN}, {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     *
     * @throws NullPointerException if device is null
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setConnectionPolicy(@NonNull BluetoothDevice device,
            @ConnectionPolicy int connectionPolicy) {
        if (DBG) log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        Objects.requireNonNull(device, "BluetoothDevice cannot be null");
        final IBluetoothSap service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)
                && (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                    || connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setConnectionPolicy(device, connectionPolicy, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the priority of the profile.
     *
     * <p> The priority can be any of:
     * {@link #PRIORITY_OFF}, {@link #PRIORITY_ON}, {@link #PRIORITY_UNDEFINED}
     *
     * @param device Bluetooth device
     * @return priority of the device
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public int getPriority(BluetoothDevice device) {
        if (VDBG) log("getPriority(" + device + ")");
        return BluetoothAdapter.connectionPolicyToPriority(getConnectionPolicy(device));
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p> The connection policy can be any of:
     * {@link #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN},
     * {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     *
     * @throws NullPointerException if device is null
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @ConnectionPolicy int getConnectionPolicy(@NonNull BluetoothDevice device) {
        if (VDBG) log("getConnectionPolicy(" + device + ")");
        Objects.requireNonNull(device, "BluetoothDevice cannot be null");
        final IBluetoothSap service = getService();
        final int defaultValue = BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getConnectionPolicy(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private boolean isEnabled() {
        return mAdapter.isEnabled();
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }
}

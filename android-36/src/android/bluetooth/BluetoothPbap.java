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

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import android.annotation.NonNull;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Public API for controlling the Bluetooth Pbap Service. This includes Bluetooth Phone book Access
 * profile. BluetoothPbap is a proxy object for controlling the Bluetooth Pbap Service via IPC.
 *
 * <p>Creating a BluetoothPbap object will create a binding with the BluetoothPbap service. Users of
 * this object should call close() when they are finished with the BluetoothPbap, so that this proxy
 * object can unbind from the service.
 *
 * <p>This BluetoothPbap object is not immediately bound to the BluetoothPbap service. Use the
 * ServiceListener interface to obtain a notification when it is bound, this is especially important
 * if you wish to immediately call methods on BluetoothPbap after construction.
 *
 * <p>To get an instance of the BluetoothPbap class, you can call {@link
 * BluetoothAdapter#getProfileProxy(Context, ServiceListener, int)} with the final param being
 * {@link BluetoothProfile#PBAP}. The ServiceListener should be able to get the instance of
 * BluetoothPbap in {@link android.bluetooth.BluetoothProfile.ServiceListener#onServiceConnected}.
 *
 * <p>Android only supports one connected Bluetooth Pce at a time.
 *
 * @hide
 */
@SystemApi
public class BluetoothPbap implements BluetoothProfile {
    private static final String TAG = "BluetoothPbap";

    private static final boolean DBG = false;

    /**
     * Intent used to broadcast the change in connection state of the PBAP profile.
     *
     * <p>This intent will have 3 extras:
     *
     * <ul>
     *   <li>{@link BluetoothProfile#EXTRA_STATE} - The current state of the profile.
     *   <li>{@link BluetoothProfile#EXTRA_PREVIOUS_STATE}- The previous state of the profile.
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device.
     * </ul>
     *
     * <p>{@link BluetoothProfile#EXTRA_STATE} or {@link BluetoothProfile#EXTRA_PREVIOUS_STATE} can
     * be any of {@link BluetoothProfile#STATE_DISCONNECTED}, {@link
     * BluetoothProfile#STATE_CONNECTING}, {@link BluetoothProfile#STATE_CONNECTED}, {@link
     * BluetoothProfile#STATE_DISCONNECTING}.
     *
     * @hide
     */
    @SuppressLint("ActionValue")
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED";

    private final AttributionSource mAttributionSource;

    /** @hide */
    public static final int RESULT_FAILURE = 0;

    /** @hide */
    public static final int RESULT_SUCCESS = 1;

    /**
     * Connection canceled before completion.
     *
     * @hide
     */
    public static final int RESULT_CANCELED = 2;

    private BluetoothAdapter mAdapter;

    private IBluetoothPbap mService;

    /**
     * Create a BluetoothPbap proxy object.
     *
     * @hide
     */
    public BluetoothPbap(Context context, BluetoothAdapter adapter) {
        mAdapter = adapter;
        mAttributionSource = adapter.getAttributionSource();
        mService = null;
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceConnected(IBinder service) {
        mService = IBluetoothPbap.Stub.asInterface(service);
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceDisconnected() {
        mService = null;
    }

    private IBluetoothPbap getService() {
        return mService;
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public List<BluetoothDevice> getConnectedDevices() {
        log("getConnectedDevices()");
        final IBluetoothPbap service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            return Collections.emptyList();
        }
        try {
            return Attributable.setAttributionSource(
                    service.getConnectedDevices(mAttributionSource), mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @SystemApi
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public @BtProfileState int getConnectionState(@NonNull BluetoothDevice device) {
        log("getConnectionState: device=" + device);
        try {
            final IBluetoothPbap service = getService();
            if (service != null && isEnabled() && isValidDevice(device)) {
                return service.getConnectionState(device, mAttributionSource);
            }
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return BluetoothProfile.STATE_DISCONNECTED;
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        log("getDevicesMatchingConnectionStates: states=" + Arrays.toString(states));
        final IBluetoothPbap service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            return Collections.emptyList();
        }
        try {
            return Attributable.setAttributionSource(
                    service.getDevicesMatchingConnectionStates(states, mAttributionSource),
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        return Collections.emptyList();
    }

    /**
     * Set connection policy of the profile and tries to disconnect it if connectionPolicy is {@link
     * BluetoothProfile#CONNECTION_POLICY_FORBIDDEN}
     *
     * <p>The device should already be paired. Connection policy can be one of: {@link
     * BluetoothProfile#CONNECTION_POLICY_ALLOWED}, {@link
     * BluetoothProfile#CONNECTION_POLICY_FORBIDDEN}, {@link
     * BluetoothProfile#CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public boolean setConnectionPolicy(
            @NonNull BluetoothDevice device, @ConnectionPolicy int connectionPolicy) {
        if (DBG) log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        try {
            final IBluetoothPbap service = getService();
            if (service != null && isEnabled() && isValidDevice(device)) {
                if (connectionPolicy != BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                        && connectionPolicy != BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
                    return false;
                }
                return service.setConnectionPolicy(device, connectionPolicy, mAttributionSource);
            }
            if (service == null) Log.w(TAG, "Proxy not attached to service");
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    /**
     * Disconnects the current Pbap client (PCE). Currently this call blocks, it may soon be made
     * asynchronous. Returns false if this proxy object is not currently connected to the Pbap
     * service.
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean disconnect(BluetoothDevice device) {
        log("disconnect()");
        final IBluetoothPbap service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            return false;
        }
        try {
            service.disconnect(device, mAttributionSource);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        return false;
    }

    private boolean isEnabled() {
        if (mAdapter.getState() == BluetoothAdapter.STATE_ON) return true;
        return false;
    }

    private boolean isValidDevice(BluetoothDevice device) {
        if (device == null) return false;

        if (BluetoothAdapter.checkBluetoothAddress(device.getAddress())) return true;
        return false;
    }

    private static void log(String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }
}

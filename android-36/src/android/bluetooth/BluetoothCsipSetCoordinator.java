/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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
import static android.bluetooth.BluetoothUtils.executeFromBinder;

import static java.util.Objects.requireNonNull;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.content.AttributionSource;
import android.content.Context;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * This class provides the public APIs to control the Bluetooth CSIP set coordinator.
 *
 * <p>BluetoothCsipSetCoordinator is a proxy object for controlling the Bluetooth CSIP set Service
 * via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get the BluetoothCsipSetCoordinator
 * proxy object.
 */
public final class BluetoothCsipSetCoordinator implements BluetoothProfile, AutoCloseable {
    private static final String TAG = "BluetoothCsipSetCoordinator";

    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    private CloseGuard mCloseGuard;

    /** @hide */
    @SystemApi
    public interface ClientLockCallback {
        /** @hide */
        @IntDef(
                value = {
                    BluetoothStatusCodes.SUCCESS,
                    BluetoothStatusCodes.ERROR_DEVICE_NOT_CONNECTED,
                    BluetoothStatusCodes.ERROR_CSIP_INVALID_GROUP_ID,
                    BluetoothStatusCodes.ERROR_CSIP_GROUP_LOCKED_BY_OTHER,
                    BluetoothStatusCodes.ERROR_CSIP_LOCKED_GROUP_MEMBER_LOST,
                    BluetoothStatusCodes.ERROR_UNKNOWN,
                })
        @Retention(RetentionPolicy.SOURCE)
        @interface Status {}

        /**
         * Callback is invoken as a result on {@link #groupLock()}.
         *
         * @param groupId group identifier
         * @param opStatus status of lock operation
         * @param isLocked indicates if group is locked
         * @hide
         */
        @SystemApi
        void onGroupLockSet(int groupId, @Status int opStatus, boolean isLocked);
    }

    private static class BluetoothCsipSetCoordinatorLockCallbackDelegate
            extends IBluetoothCsipSetCoordinatorLockCallback.Stub {
        private final ClientLockCallback mCallback;
        private final Executor mExecutor;

        BluetoothCsipSetCoordinatorLockCallbackDelegate(
                Executor executor, ClientLockCallback callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        @RequiresNoPermission
        public void onGroupLockSet(int groupId, int opStatus, boolean isLocked) {
            executeFromBinder(
                    mExecutor, () -> mCallback.onGroupLockSet(groupId, opStatus, isLocked));
        }
    }
    ;

    /**
     * Intent used to broadcast the change in connection state of the CSIS Client.
     *
     * <p>This intent will have 3 extras:
     *
     * <ul>
     *   <li>{@link #EXTRA_STATE} - The current state of the profile.
     *   <li>{@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device.
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of {@link
     * #STATE_DISCONNECTED}, {@link #STATE_CONNECTING}, {@link #STATE_CONNECTED}, {@link
     * #STATE_DISCONNECTING}.
     */
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CSIS_CONNECTION_STATE_CHANGED =
            "android.bluetooth.action.CSIS_CONNECTION_STATE_CHANGED";

    /**
     * Intent used to expose broadcast receiving device.
     *
     * <p>This intent will have 2 extras:
     *
     * <ul>
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote Broadcast receiver device.
     *   <li>{@link #EXTRA_CSIS_GROUP_ID} - Group identifier.
     *   <li>{@link #EXTRA_CSIS_GROUP_SIZE} - Group size.
     *   <li>{@link #EXTRA_CSIS_GROUP_TYPE_UUID} - Group type UUID.
     * </ul>
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(BLUETOOTH_PRIVILEGED)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CSIS_DEVICE_AVAILABLE =
            "android.bluetooth.action.CSIS_DEVICE_AVAILABLE";

    /**
     * Used as an extra field in {@link #ACTION_CSIS_DEVICE_AVAILABLE} intent. Contains the group
     * id.
     *
     * <p>Possible Values: {@link GROUP_ID_INVALID} Invalid group identifier 0x01 - 0xEF Valid group
     * identifier
     *
     * @hide
     */
    @SystemApi
    public static final String EXTRA_CSIS_GROUP_ID = "android.bluetooth.extra.CSIS_GROUP_ID";

    /**
     * Group size as int extra field in {@link #ACTION_CSIS_DEVICE_AVAILABLE} intent.
     *
     * @hide
     */
    public static final String EXTRA_CSIS_GROUP_SIZE = "android.bluetooth.extra.CSIS_GROUP_SIZE";

    /**
     * Group type uuid extra field in {@link #ACTION_CSIS_DEVICE_AVAILABLE} intent.
     *
     * @hide
     */
    public static final String EXTRA_CSIS_GROUP_TYPE_UUID =
            "android.bluetooth.extra.CSIS_GROUP_TYPE_UUID";

    /**
     * Intent used to broadcast information about identified set member ready to connect.
     *
     * <p>This intent will have one extra:
     *
     * <ul>
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device. It can be null if no device
     *       is active.
     *   <li>{@link #EXTRA_CSIS_GROUP_ID} - Group identifier.
     * </ul>
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(BLUETOOTH_PRIVILEGED)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CSIS_SET_MEMBER_AVAILABLE =
            "android.bluetooth.action.CSIS_SET_MEMBER_AVAILABLE";

    /**
     * This represents an invalid group ID.
     *
     * @hide
     */
    @SystemApi
    public static final int GROUP_ID_INVALID = IBluetoothCsipSetCoordinator.CSIS_GROUP_ID_INVALID;

    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;

    private IBluetoothCsipSetCoordinator mService;

    /**
     * Create a BluetoothCsipSetCoordinator proxy object for interacting with the local Bluetooth
     * CSIS service.
     */
    /*package*/ BluetoothCsipSetCoordinator(Context context, BluetoothAdapter adapter) {
        mAdapter = adapter;
        mAttributionSource = adapter.getAttributionSource();
        mService = null;
        mCloseGuard = new CloseGuard();
        mCloseGuard.open("close");
    }

    /** @hide */
    @SuppressWarnings("Finalize") // TODO(b/314811467)
    protected void finalize() {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        close();
    }

    /** @hide */
    @Override
    public void close() {
        mAdapter.closeProfileProxy(this);
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceConnected(IBinder service) {
        mService = IBluetoothCsipSetCoordinator.Stub.asInterface(service);
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceDisconnected() {
        mService = null;
    }

    private IBluetoothCsipSetCoordinator getService() {
        return mService;
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Lock the set.
     *
     * @param groupId group ID to lock,
     * @param executor callback executor,
     * @param callback callback to report lock and unlock events - stays valid until the app unlocks
     *     using the returned lock identifier or the lock timeouts on the remote side, as per CSIS
     *     specification,
     * @return unique lock identifier used for unlocking or null if lock has failed.
     * @throws IllegalArgumentException when executor or callback is null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @Nullable UUID lockGroup(
            int groupId,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull ClientLockCallback callback) {
        if (VDBG) log("lockGroup()");
        requireNonNull(executor);
        requireNonNull(callback);
        final IBluetoothCsipSetCoordinator service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            IBluetoothCsipSetCoordinatorLockCallback delegate =
                    new BluetoothCsipSetCoordinatorLockCallbackDelegate(executor, callback);
            try {
                final ParcelUuid ret = service.lockGroup(groupId, delegate, mAttributionSource);
                return ret == null ? null : ret.getUuid();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return null;
    }

    /**
     * Unlock the set.
     *
     * @param lockUuid unique lock identifier
     * @return true if unlocked, false on error
     * @throws IllegalArgumentException when lockUuid is null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean unlockGroup(@NonNull UUID lockUuid) {
        if (VDBG) log("unlockGroup()");
        requireNonNull(lockUuid);
        final IBluetoothCsipSetCoordinator service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.unlockGroup(new ParcelUuid(lockUuid), mAttributionSource);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Get device's groups.
     *
     * @param device the active device
     * @return Map of groups ids and related UUIDs
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @NonNull
    public Map<Integer, ParcelUuid> getGroupUuidMapByDevice(@Nullable BluetoothDevice device) {
        if (VDBG) log("getGroupUuidMapByDevice()");
        final IBluetoothCsipSetCoordinator service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.getGroupUuidMapByDevice(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return new HashMap<>();
    }

    /**
     * Get group id for the given UUID
     *
     * @return list of group IDs
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @NonNull List<Integer> getAllGroupIds(@Nullable ParcelUuid uuid) {
        if (VDBG) log("getAllGroupIds()");
        final IBluetoothCsipSetCoordinator service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.getAllGroupIds(uuid, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        if (VDBG) log("getConnectedDevices()");
        final IBluetoothCsipSetCoordinator service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.getConnectedDevices(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @NonNull
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(@NonNull int[] states) {
        if (VDBG) log("getDevicesMatchingStates(states=" + Arrays.toString(states) + ")");
        final IBluetoothCsipSetCoordinator service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.getDevicesMatchingConnectionStates(states, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @BluetoothProfile.BtProfileState
    public int getConnectionState(@Nullable BluetoothDevice device) {
        if (VDBG) log("getState(" + device + ")");
        final IBluetoothCsipSetCoordinator service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.getConnectionState(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Set connection policy of the profile
     *
     * <p>The device should already be paired. Connection policy can be one of {@link
     * #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN}, {@link
     * #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setConnectionPolicy(
            @Nullable BluetoothDevice device, @ConnectionPolicy int connectionPolicy) {
        if (DBG) log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        final IBluetoothCsipSetCoordinator service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()
                && isValidDevice(device)
                && (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                        || connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED)) {
            try {
                return service.setConnectionPolicy(device, connectionPolicy, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p>The connection policy can be any of: {@link #CONNECTION_POLICY_ALLOWED}, {@link
     * #CONNECTION_POLICY_FORBIDDEN}, {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @ConnectionPolicy int getConnectionPolicy(@Nullable BluetoothDevice device) {
        if (VDBG) log("getConnectionPolicy(" + device + ")");
        final IBluetoothCsipSetCoordinator service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionPolicy(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
    }

    private boolean isEnabled() {
        return mAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    private static boolean isValidDevice(@Nullable BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}

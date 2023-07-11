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

import static android.bluetooth.BluetoothUtils.getSyncTimeout;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.content.AttributionSource;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;

import com.android.modules.utils.SynchronousResultReceiver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;


/**
 * This class provides a public APIs to control the Bluetooth Hearing Access Profile client service.
 *
 * <p>BluetoothHapClient is a proxy object for controlling the Bluetooth HAP
 * Service client via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get the
 * BluetoothHapClient proxy object.
 * @hide
 */
@SystemApi
public final class BluetoothHapClient implements BluetoothProfile, AutoCloseable {
    private static final String TAG = "BluetoothHapClient";
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    private final Map<Callback, Executor> mCallbackExecutorMap = new HashMap<>();

    private CloseGuard mCloseGuard;

    /**
     * This class provides callbacks mechanism for the BluetoothHapClient profile.
     *
     * @hide
     */
    @SystemApi
    public interface Callback {
        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                // needed for future release compatibility
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST,
                BluetoothStatusCodes.REASON_LOCAL_STACK_REQUEST,
                BluetoothStatusCodes.REASON_REMOTE_REQUEST,
                BluetoothStatusCodes.REASON_SYSTEM_POLICY,
        })
        @interface PresetSelectionReason {}

        /**
         * Invoked to inform about HA device's currently active preset.
         *
         * @param device remote device,
         * @param presetIndex the currently active preset index.
         * @param reason reason for the selected preset change
         *
         * @hide
         */
        @SystemApi
        void onPresetSelected(@NonNull BluetoothDevice device, int presetIndex,
                @PresetSelectionReason int reason);

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                // needed for future release compatibility
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.REASON_LOCAL_STACK_REQUEST,
                BluetoothStatusCodes.REASON_SYSTEM_POLICY,
                BluetoothStatusCodes.ERROR_REMOTE_OPERATION_REJECTED,
                BluetoothStatusCodes.ERROR_REMOTE_OPERATION_NOT_SUPPORTED,
                BluetoothStatusCodes.ERROR_HAP_INVALID_PRESET_INDEX,
        })
        @interface PresetSelectionFailureReason {}

        /**
         * Invoked inform about the result of a failed preset change attempt.
         *
         * @param device remote device,
         * @param reason failure reason.
         *
         * @hide
         */
        @SystemApi
        void onPresetSelectionFailed(@NonNull BluetoothDevice device,
                @PresetSelectionFailureReason int reason);

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                // needed for future release compatibility
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.REASON_LOCAL_STACK_REQUEST,
                BluetoothStatusCodes.REASON_SYSTEM_POLICY,
                BluetoothStatusCodes.ERROR_REMOTE_OPERATION_REJECTED,
                BluetoothStatusCodes.ERROR_REMOTE_OPERATION_NOT_SUPPORTED,
                BluetoothStatusCodes.ERROR_HAP_INVALID_PRESET_INDEX,
                BluetoothStatusCodes.ERROR_CSIP_INVALID_GROUP_ID,
        })
        @interface GroupPresetSelectionFailureReason {}

        /**
         * Invoked to inform about the result of a failed preset change attempt.
         *
         * The implementation will try to restore the state for every device back to original
         *
         * @param hapGroupId valid HAP group ID,
         * @param reason failure reason.
         *
         * @hide
         */
        @SystemApi
        void onPresetSelectionForGroupFailed(int hapGroupId,
                @GroupPresetSelectionFailureReason int reason);

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                // needed for future release compatibility
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST,
                BluetoothStatusCodes.REASON_LOCAL_STACK_REQUEST,
                BluetoothStatusCodes.REASON_REMOTE_REQUEST,
                BluetoothStatusCodes.REASON_SYSTEM_POLICY,
        })
        @interface PresetInfoChangeReason {}

        /**
         * Invoked to inform about the preset list changes.
         *
         * @param device remote device,
         * @param presetInfoList a list of all preset information on the target device
         * @param reason reason for the preset list change
         *
         * @hide
         */
        @SystemApi
        void onPresetInfoChanged(@NonNull BluetoothDevice device,
                @NonNull List<BluetoothHapPresetInfo> presetInfoList,
                @PresetInfoChangeReason int reason);

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                // needed for future release compatibility
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.REASON_LOCAL_STACK_REQUEST,
                BluetoothStatusCodes.REASON_SYSTEM_POLICY,
                BluetoothStatusCodes.ERROR_REMOTE_OPERATION_REJECTED,
                BluetoothStatusCodes.ERROR_REMOTE_OPERATION_NOT_SUPPORTED,
                BluetoothStatusCodes.ERROR_HAP_PRESET_NAME_TOO_LONG,
                BluetoothStatusCodes.ERROR_HAP_INVALID_PRESET_INDEX,
        })
        @interface PresetNameChangeFailureReason {}

        /**
         * Invoked to inform about the failed preset rename attempt.
         *
         * @param device remote device
         * @param reason Failure reason code.
         * @hide
         */
        @SystemApi
        void onSetPresetNameFailed(@NonNull BluetoothDevice device,
                @PresetNameChangeFailureReason int reason);

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                // needed for future release compatibility
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.REASON_LOCAL_STACK_REQUEST,
                BluetoothStatusCodes.REASON_SYSTEM_POLICY,
                BluetoothStatusCodes.ERROR_REMOTE_OPERATION_REJECTED,
                BluetoothStatusCodes.ERROR_REMOTE_OPERATION_NOT_SUPPORTED,
                BluetoothStatusCodes.ERROR_HAP_PRESET_NAME_TOO_LONG,
                BluetoothStatusCodes.ERROR_HAP_INVALID_PRESET_INDEX,
                BluetoothStatusCodes.ERROR_CSIP_INVALID_GROUP_ID,
        })
        @interface GroupPresetNameChangeFailureReason {}

        /**
         * Invoked to inform about the failed preset rename attempt.
         *
         * The implementation will try to restore the state for every device back to original
         *
         * @param hapGroupId valid HAP group ID,
         * @param reason Failure reason code.
         * @hide
         */
        @SystemApi
        void onSetPresetNameForGroupFailed(int hapGroupId,
                @GroupPresetNameChangeFailureReason int reason);
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IBluetoothHapClientCallback mCallback = new IBluetoothHapClientCallback.Stub() {
        @Override
        public void onPresetSelected(@NonNull BluetoothDevice device, int presetIndex,
                int reasonCode) {
            Attributable.setAttributionSource(device, mAttributionSource);
            for (Map.Entry<BluetoothHapClient.Callback, Executor> callbackExecutorEntry:
                    mCallbackExecutorMap.entrySet()) {
                BluetoothHapClient.Callback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(() -> callback.onPresetSelected(device, presetIndex, reasonCode));
            }
        }

        @Override
        public void onPresetSelectionFailed(@NonNull BluetoothDevice device, int status) {
            Attributable.setAttributionSource(device, mAttributionSource);
            for (Map.Entry<BluetoothHapClient.Callback, Executor> callbackExecutorEntry:
                    mCallbackExecutorMap.entrySet()) {
                BluetoothHapClient.Callback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(() -> callback.onPresetSelectionFailed(device, status));
            }
        }

        @Override
        public void onPresetSelectionForGroupFailed(int hapGroupId, int statusCode) {
            for (Map.Entry<BluetoothHapClient.Callback, Executor> callbackExecutorEntry:
                    mCallbackExecutorMap.entrySet()) {
                BluetoothHapClient.Callback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(
                        () -> callback.onPresetSelectionForGroupFailed(hapGroupId, statusCode));
            }
        }

        @Override
        public void onPresetInfoChanged(@NonNull BluetoothDevice device,
                @NonNull List<BluetoothHapPresetInfo> presetInfoList, int statusCode) {
            Attributable.setAttributionSource(device, mAttributionSource);
            for (Map.Entry<BluetoothHapClient.Callback, Executor> callbackExecutorEntry:
                    mCallbackExecutorMap.entrySet()) {
                BluetoothHapClient.Callback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(
                        () -> callback.onPresetInfoChanged(device, presetInfoList, statusCode));
            }
        }

        @Override
        public void onSetPresetNameFailed(@NonNull BluetoothDevice device, int status) {
            Attributable.setAttributionSource(device, mAttributionSource);
            for (Map.Entry<BluetoothHapClient.Callback, Executor> callbackExecutorEntry:
                    mCallbackExecutorMap.entrySet()) {
                BluetoothHapClient.Callback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(() -> callback.onSetPresetNameFailed(device, status));
            }
        }

        @Override
        public void onSetPresetNameForGroupFailed(int hapGroupId, int status) {
            for (Map.Entry<BluetoothHapClient.Callback, Executor> callbackExecutorEntry:
                    mCallbackExecutorMap.entrySet()) {
                BluetoothHapClient.Callback callback = callbackExecutorEntry.getKey();
                Executor executor = callbackExecutorEntry.getValue();
                executor.execute(() -> callback.onSetPresetNameForGroupFailed(hapGroupId, status));
            }
        }
    };

    /**
     * Intent used to broadcast the change in connection state of the Hearing Access Profile Client
     * service. Please note that in the binaural case, there will be two different LE devices for
     * the left and right side and each device will have their own connection state changes.
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
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_CONNECTION_STATE_CHANGED =
            "android.bluetooth.action.HAP_CONNECTION_STATE_CHANGED";

    /**
     * Intent used to broadcast the device availability change and the availability of its
     * presets. Please note that in the binaural case, there will be two different LE devices for
     * the left and right side and each device will have their own availability event.
     *
     * <p>This intent will have 2 extras:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * <li> {@link #EXTRA_HAP_FEATURES} - Supported features map. </li>
     * </ul>
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_DEVICE_AVAILABLE =
            "android.bluetooth.action.HAP_DEVICE_AVAILABLE";

    /**
     * Contains a list of all available presets
     * @hide
     */
    public static final String EXTRA_HAP_FEATURES = "android.bluetooth.extra.HAP_FEATURES";

    /**
     * Represets an invalid index value. This is usually value returned in a currently
     * active preset request for a device which is not connected. This value shouldn't be used
     * in the API calls.
     * @hide
     */
    public static final int PRESET_INDEX_UNAVAILABLE = IBluetoothHapClient.PRESET_INDEX_UNAVAILABLE;

    /**
     * Feature value.
     * @hide
     */
    public static final int FEATURE_TYPE_MONAURAL =
            1 << IBluetoothHapClient.FEATURE_BIT_NUM_TYPE_MONAURAL;

    /**
     * Feature value.
     * @hide
     */
    public static final int FEATURE_TYPE_BANDED =
            1 << IBluetoothHapClient.FEATURE_BIT_NUM_TYPE_BANDED;

    /**
     * Feature value.
     * @hide
     */
    public static final int FEATURE_SYNCHRONIZATED_PRESETS =
            1 << IBluetoothHapClient.FEATURE_BIT_NUM_SYNCHRONIZATED_PRESETS;

    /**
     * Feature value.
     * @hide
     */
    public static final int FEATURE_INDEPENDENT_PRESETS =
            1 << IBluetoothHapClient.FEATURE_BIT_NUM_INDEPENDENT_PRESETS;

    /**
     * Feature value.
     * @hide
     */
    public static final int FEATURE_DYNAMIC_PRESETS =
            1 << IBluetoothHapClient.FEATURE_BIT_NUM_DYNAMIC_PRESETS;

    /**
     * Feature value.
     * @hide
     */
    public static final int FEATURE_WRITABLE_PRESETS =
            1 << IBluetoothHapClient.FEATURE_BIT_NUM_WRITABLE_PRESETS;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        flag = true,
        value = {
            FEATURE_TYPE_MONAURAL,
            FEATURE_TYPE_BANDED,
            FEATURE_SYNCHRONIZATED_PRESETS,
            FEATURE_DYNAMIC_PRESETS,
            FEATURE_WRITABLE_PRESETS,
    })
    @interface Feature {}

    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;
    private final BluetoothProfileConnector<IBluetoothHapClient> mProfileConnector =
            new BluetoothProfileConnector(this, BluetoothProfile.HAP_CLIENT, "BluetoothHapClient",
                    IBluetoothHapClient.class.getName()) {
                @Override
                public IBluetoothHapClient getServiceInterface(IBinder service) {
                    return IBluetoothHapClient.Stub.asInterface(service);
                }
            };

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback =
            new IBluetoothStateChangeCallback.Stub() {
                public void onBluetoothStateChange(boolean up) {
                    if (DBG) Log.d(TAG, "onBluetoothStateChange: up=" + up);
                    if (up) {
                        // re-register the service-to-app callback
                        synchronized (mCallbackExecutorMap) {
                            if (mCallbackExecutorMap.isEmpty()) return;

                            try {
                                final IBluetoothHapClient service = getService();
                                if (service != null) {
                                    final SynchronousResultReceiver<Integer> recv =
                                            SynchronousResultReceiver.get();
                                    service.registerCallback(mCallback, mAttributionSource, recv);
                                    recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
                                }
                            } catch (TimeoutException e) {
                                Log.e(TAG, e.toString() + "\n"
                                        + Log.getStackTraceString(new Throwable()));
                            } catch (RemoteException e) {
                                throw e.rethrowFromSystemServer();
                            }
                        }
                    }
                }
            };

    /**
     * Create a BluetoothHapClient proxy object for interacting with the local
     * Bluetooth Hearing Access Profile (HAP) client.
     */
    /*package*/ BluetoothHapClient(Context context, ServiceListener listener) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mAttributionSource = mAdapter.getAttributionSource();
        mProfileConnector.connect(context, listener);

        IBluetoothManager mgr = mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        mCloseGuard = new CloseGuard();
        mCloseGuard.open("close");
    }

    /**
     * @hide
     */
    protected void finalize() {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        close();
    }

    /**
     * @hide
     */
    public void close() {
        if (VDBG) log("close()");

        IBluetoothManager mgr = mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }

        mProfileConnector.disconnect();
    }

    private IBluetoothHapClient getService() {
        return mProfileConnector.getService();
    }

    /**
     * Register a {@link Callback} that will be invoked during the
     * operation of this profile.
     *
     * Repeated registration of the same <var>callback</var> object after the first call to this
     * method will result with IllegalArgumentException being thrown, even when the
     * <var>executor</var> is different. API caller would have to call
     * {@link #unregisterCallback(Callback)} with the same callback object before registering it
     * again.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link Callback}
     * @throws NullPointerException if a null executor, or callback is given, or
     *  IllegalArgumentException if the same <var>callback<var> is already registered.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public void registerCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull Callback callback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");

        if (DBG) log("registerCallback");

        synchronized (mCallbackExecutorMap) {
            // If the callback map is empty, we register the service-to-app callback
            if (mCallbackExecutorMap.isEmpty()) {
                if (!isEnabled()) {
                    /* If Bluetooth is off, just store callback and it will be registered
                     * when Bluetooth is on
                     */
                    mCallbackExecutorMap.put(callback, executor);
                    return;
                }
                try {
                    final IBluetoothHapClient service = getService();
                    if (service != null) {
                        final SynchronousResultReceiver<Integer> recv =
                                SynchronousResultReceiver.get();
                        service.registerCallback(mCallback, mAttributionSource, recv);
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
                    }
                } catch (TimeoutException e) {
                    Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }

            // Adds the passed in callback to our map of callbacks to executors
            if (mCallbackExecutorMap.containsKey(callback)) {
                throw new IllegalArgumentException("This callback has already been registered");
            }
            mCallbackExecutorMap.put(callback, executor);
        }
    }

    /**
     * Unregister the specified {@link Callback}.
     * <p>The same {@link Callback} object used when calling
     * {@link #registerCallback(Executor, Callback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when application process goes away
     *
     * @param callback user implementation of the {@link Callback}
     * @throws NullPointerException when callback is null or IllegalArgumentException when no
     *  callback is registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public void unregisterCallback(@NonNull Callback callback) {
        Objects.requireNonNull(callback, "callback cannot be null");

        if (DBG) log("unregisterCallback");

        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.remove(callback) == null) {
                throw new IllegalArgumentException("This callback has not been registered");
            }
        }

        // If the callback map is empty, we unregister the service-to-app callback
        if (mCallbackExecutorMap.isEmpty()) {
            try {
                final IBluetoothHapClient service = getService();
                if (service != null) {
                    final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                    service.unregisterCallback(mCallback, mAttributionSource, recv);
                    recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
                }
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
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
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled() && isValidDevice(device)
                    && (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                        || connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
                service.setConnectionPolicy(device, connectionPolicy, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p> The connection policy can be any of:
     * {@link #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN},
     * {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device or {@link #CONNECTION_POLICY_FORBIDDEN} if device is
     *         null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @ConnectionPolicy int getConnectionPolicy(@Nullable BluetoothDevice device) {
        if (VDBG) log("getConnectionPolicy(" + device + ")");
        final IBluetoothHapClient service = getService();
        final int defaultValue = BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getConnectionPolicy(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @Override
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        if (VDBG) Log.d(TAG, "getConnectedDevices()");
        final IBluetoothHapClient service = getService();
        final List defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<List> recv = SynchronousResultReceiver.get();
                service.getConnectedDevices(mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @Override
    public @NonNull List<BluetoothDevice> getDevicesMatchingConnectionStates(
            @NonNull int[] states) {
        if (VDBG) Log.d(TAG, "getDevicesMatchingConnectionStates()");
        final IBluetoothHapClient service = getService();
        final List defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<List> recv = SynchronousResultReceiver.get();
                service.getDevicesMatchingConnectionStates(states, mAttributionSource, recv);
                return Attributable.setAttributionSource(
                        recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue),
                        mAttributionSource);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @Override
    public @BluetoothProfile.BtProfileState int getConnectionState(
            @NonNull BluetoothDevice device) {
        if (VDBG) Log.d(TAG, "getConnectionState(" + device + ")");
        final IBluetoothHapClient service = getService();
        final int defaultValue = BluetoothProfile.STATE_DISCONNECTED;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getConnectionState(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Gets the group identifier, which can be used in the group related part of
     * the API.
     *
     * <p>Users are expected to get group identifier for each of the connected
     * device to discover the device grouping. This allows them to make an informed
     * decision which devices can be controlled by single group API call and which
     * require individual device calls.
     *
     * <p>Note that some binaural HA devices may not support group operations,
     * therefore are not considered a valid HAP group. In such case -1 is returned
     * even if such device is a valid Le Audio Coordinated Set member.
     *
     * @param device
     * @return valid group identifier or -1
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public int getHapGroup(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        final int defaultValue = BluetoothCsipSetCoordinator.GROUP_ID_INVALID;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getHapGroup(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Gets the currently active preset for a HA device.
     *
     * @param device is the device for which we want to set the active preset
     * @return active preset index
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public int getActivePresetIndex(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        final int defaultValue = PRESET_INDEX_UNAVAILABLE;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getActivePresetIndex(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Get the currently active preset info for a remote device.
     *
     * @param device is the device for which we want to get the preset name
     * @return currently active preset info if selected, null if preset info is not available
     *         for the remote device
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public @Nullable BluetoothHapPresetInfo getActivePresetInfo(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        final BluetoothHapPresetInfo defaultValue = null;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<BluetoothHapPresetInfo> recv =
                        SynchronousResultReceiver.get();
                service.getActivePresetInfo(device, mAttributionSource, recv);
                recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        return defaultValue;
    }

    /**
     * Selects the currently active preset for a HA device
     *
     * On success, {@link Callback#onPresetSelected(BluetoothDevice, int, int)} will be called with
     * reason code {@link BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST}
     * On failure, {@link Callback#onPresetSelectionFailed(BluetoothDevice, int)} will be called.
     *
     * @param device is the device for which we want to set the active preset
     * @param presetIndex is an index of one of the available presets
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public void selectPreset(@NonNull BluetoothDevice device, int presetIndex) {
        final IBluetoothHapClient service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                service.selectPreset(device, presetIndex, mAttributionSource);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Selects the currently active preset for a Hearing Aid device group.
     *
     * <p> This group call may replace multiple device calls if those are part of the
     * valid HAS group. Note that binaural HA devices may or may not support group.
     *
     * On success, {@link Callback#onPresetSelected(BluetoothDevice, int, int)} will be called
     * for each device within the group with reason code
     * {@link BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST}
     * On failure, {@link Callback#onPresetSelectionForGroupFailed(int, int)} will be
     * called for the group.
     *
     * @param groupId is the device group identifier for which want to set the active preset
     * @param presetIndex is an index of one of the available presets
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public void selectPresetForGroup(int groupId, int presetIndex) {
        final IBluetoothHapClient service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.selectPresetForGroup(groupId, presetIndex, mAttributionSource);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Sets the next preset as a currently active preset for a HA device
     *
     * <p> Note that the meaning of 'next' is HA device implementation specific and
     * does not necessarily mean a higher preset index.
     *
     * @param device is the device for which we want to set the active preset
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public void switchToNextPreset(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                service.switchToNextPreset(device, mAttributionSource);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Sets the next preset as a currently active preset for a HA device group
     *
     * <p> Note that the meaning of 'next' is HA device implementation specific and
     * does not necessarily mean a higher preset index.
     * <p> This group call may replace multiple device calls if those are part of the
     * valid HAS group. Note that binaural HA devices may or may not support group.
     *
     * @param groupId is the device group identifier for which want to set the active preset
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public void switchToNextPresetForGroup(int groupId) {
        final IBluetoothHapClient service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.switchToNextPresetForGroup(groupId, mAttributionSource);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Sets the previous preset as a currently active preset for a HA device.
     *
     * <p> Note that the meaning of 'previous' is HA device implementation specific and
     * does not necessarily mean a lower preset index.
     *
     * @param device is the device for which we want to set the active preset
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public void switchToPreviousPreset(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                service.switchToPreviousPreset(device, mAttributionSource);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Sets the previous preset as a currently active preset for a HA device group
     *
     * <p> Note the meaning of 'previous' is HA device implementation specific and
     * does not necessarily mean a lower preset index.
     * <p> This group call may replace multiple device calls if those are part of the
     * valid HAS group. Note that binaural HA devices may or may not support group.
     *
     * @param groupId is the device group identifier for which want to set the active preset
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public void switchToPreviousPresetForGroup(int groupId) {
        final IBluetoothHapClient service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.switchToPreviousPresetForGroup(groupId, mAttributionSource);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Requests the preset info
     *
     * @param device is the device for which we want to get the preset name
     * @param presetIndex is an index of one of the available presets
     * @return preset info
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public @Nullable BluetoothHapPresetInfo getPresetInfo(@NonNull BluetoothDevice device,
            int presetIndex) {
        final IBluetoothHapClient service = getService();
        final BluetoothHapPresetInfo defaultValue = null;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<BluetoothHapPresetInfo> recv =
                        SynchronousResultReceiver.get();
                service.getPresetInfo(device, presetIndex, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Get all preset info for a particular device
     *
     * @param device is the device for which we want to get all presets info
     * @return a list of all known preset info
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public @NonNull List<BluetoothHapPresetInfo> getAllPresetInfo(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        final List<BluetoothHapPresetInfo> defaultValue = new ArrayList<BluetoothHapPresetInfo>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<List<BluetoothHapPresetInfo>> recv =
                        SynchronousResultReceiver.get();
                service.getAllPresetInfo(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Requests HAP features
     *
     * @param device is the device for which we want to get features for
     * @return features value with feature bits set
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public @Feature int getFeatures(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        final int defaultValue = 0x00;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
                service.getFeatures(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return defaultValue;
    }

    /**
     * Sets the preset name for a particular device
     *
     * <p> Note that the name length is restricted to 40 characters.
     *
     * On success, {@link Callback#onPresetInfoChanged(BluetoothDevice, List, int)}
     * with a new name will be called and reason code
     * {@link BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST}
     * On failure, {@link Callback#onSetPresetNameFailed(BluetoothDevice, int)} will be called.
     *
     * @param device is the device for which we want to get the preset name
     * @param presetIndex is an index of one of the available presets
     * @param name is a new name for a preset, maximum length is 40 characters
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public void setPresetName(@NonNull BluetoothDevice device, int presetIndex,
            @NonNull String name) {
        final IBluetoothHapClient service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                service.setPresetName(device, presetIndex, name, mAttributionSource);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Sets the name for a hearing aid preset.
     *
     * <p> Note that the name length is restricted to 40 characters.
     *
     * On success, {@link Callback#onPresetInfoChanged(BluetoothDevice, List, int)}
     * with a new name will be called for each device within the group with reason code
     * {@link BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST}
     * On failure, {@link Callback#onSetPresetNameForGroupFailed(int, int)} will be invoked
     *
     * @param groupId is the device group identifier
     * @param presetIndex is an index of one of the available presets
     * @param name is a new name for a preset, maximum length is 40 characters
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED
    })
    public void setPresetNameForGroup(int groupId, int presetIndex, @NonNull String name) {
        final IBluetoothHapClient service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.setPresetNameForGroup(groupId, presetIndex, name, mAttributionSource);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
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
        Log.d(TAG, msg);
    }
}

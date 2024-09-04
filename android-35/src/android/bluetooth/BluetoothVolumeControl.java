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

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.content.AttributionSource;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;

import com.android.bluetooth.flags.Flags;
import com.android.internal.annotations.GuardedBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * This class provides the public APIs to control the Bluetooth Volume Control service.
 *
 * <p>BluetoothVolumeControl is a proxy object for controlling the Bluetooth VC Service via IPC. Use
 * {@link BluetoothAdapter#getProfileProxy} to get the BluetoothVolumeControl proxy object.
 *
 * @hide
 */
@SystemApi
public final class BluetoothVolumeControl implements BluetoothProfile, AutoCloseable {
    private static final String TAG = "BluetoothVolumeControl";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    private CloseGuard mCloseGuard;

    @GuardedBy("mCallbackExecutorMap")
    private final Map<Callback, Executor> mCallbackExecutorMap = new HashMap<>();

    /**
     * This class provides a callback that is invoked when volume offset value changes on the remote
     * device.
     *
     * <p>In order to balance volume on the group of Le Audio devices, Volume Offset Control Service
     * (VOCS) shall be used. User can verify if the remote device supports VOCS by calling {@link
     * #isVolumeOffsetAvailable(device)}.
     *
     * @hide
     */
    @SystemApi
    public interface Callback {
        /**
         * Callback invoked when callback is registered and when volume offset changes on the remote
         * device. Change can be triggered autonomously by the remote device or after volume offset
         * change on the user request done by calling {@link #setVolumeOffset(device, volumeOffset)}
         *
         * @param device remote device whose volume offset changed
         * @param volumeOffset latest volume offset for this device
         * @deprecated Use new callback which give information about a VOCS instance ID
         * @hide
         */
        @Deprecated
        @SystemApi
        default void onVolumeOffsetChanged(
                @NonNull BluetoothDevice device,
                @IntRange(from = -255, to = 255) int volumeOffset) {}

        /**
         * Callback invoked when callback is registered and when volume offset changes on the remote
         * device. Change can be triggered autonomously by the remote device or after volume offset
         * change on the user request done by calling {@link #setVolumeOffset(device, instanceId,
         * volumeOffset)}
         *
         * @param device remote device whose volume offset changed
         * @param instanceId identifier of VOCS instance on the remote device
         * @param volumeOffset latest volume offset for this VOCS instance
         * @hide
         */
        @FlaggedApi(Flags.FLAG_LEAUDIO_MULTIPLE_VOCS_INSTANCES_API)
        @SystemApi
        default void onVolumeOffsetChanged(
                @NonNull BluetoothDevice device,
                @IntRange(from = 1, to = 255) int instanceId,
                @IntRange(from = -255, to = 255) int volumeOffset) {
            if (instanceId == 1) {
                onVolumeOffsetChanged(device, volumeOffset);
            }
        }

        /**
         * Callback invoked when callback is registered and when audio location changes on the
         * remote device. Change can be triggered autonomously by the remote device.
         *
         * @param device remote device whose audio location changed
         * @param instanceId identifier of VOCS instance on the remote device
         * @param audioLocation latest audio location for this VOCS instance
         * @hide
         */
        @FlaggedApi(Flags.FLAG_LEAUDIO_MULTIPLE_VOCS_INSTANCES_API)
        @SystemApi
        default void onVolumeOffsetAudioLocationChanged(
                @NonNull BluetoothDevice device,
                @IntRange(from = 1, to = 255) int instanceId,
                @IntRange(from = -255, to = 255) int audioLocation) {}

        /**
         * Callback invoked when callback is registered and when audio description changes on the
         * remote device. Change can be triggered autonomously by the remote device.
         *
         * @param device remote device whose audio description changed
         * @param instanceId identifier of VOCS instance on the remote device
         * @param audioDescription latest audio description for this VOCS instance
         * @hide
         */
        @FlaggedApi(Flags.FLAG_LEAUDIO_MULTIPLE_VOCS_INSTANCES_API)
        @SystemApi
        default void onVolumeOffsetAudioDescriptionChanged(
                @NonNull BluetoothDevice device,
                @IntRange(from = 1, to = 255) int instanceId,
                @NonNull String audioDescription) {}

        /**
         * Callback for le audio connected device volume level change
         *
         * <p>The valid volume range is [0, 255], as defined in 2.3.1.1 Volume_Setting field of
         * Volume Control Service, Version 1.0.
         *
         * @param device remote device whose volume changed
         * @param volume level
         * @hide
         */
        @FlaggedApi(Flags.FLAG_LEAUDIO_BROADCAST_VOLUME_CONTROL_FOR_CONNECTED_DEVICES)
        @SystemApi
        default void onDeviceVolumeChanged(
                @NonNull BluetoothDevice device, @IntRange(from = 0, to = 255) int volume) {}
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IBluetoothVolumeControlCallback mCallback =
            new VolumeControlNotifyCallback(mCallbackExecutorMap);

    private class VolumeControlNotifyCallback extends IBluetoothVolumeControlCallback.Stub {
        private final Map<Callback, Executor> mCallbackMap;

        VolumeControlNotifyCallback(Map<Callback, Executor> callbackMap) {
            mCallbackMap = callbackMap;
        }

        private void forEach(Consumer<BluetoothVolumeControl.Callback> consumer) {
            synchronized (mCallbackMap) {
                mCallbackMap.forEach(
                        (callback, executor) -> executor.execute(() -> consumer.accept(callback)));
            }
        }

        @Override
        public void onVolumeOffsetChanged(
                @NonNull BluetoothDevice device, int instanceId, int volumeOffset) {
            Attributable.setAttributionSource(device, mAttributionSource);
            if (Flags.leaudioMultipleVocsInstancesApi()) {
                forEach((cb) -> cb.onVolumeOffsetChanged(device, instanceId, volumeOffset));
            }
        }

        @Override
        public void onVolumeOffsetAudioLocationChanged(
                @NonNull BluetoothDevice device, int instanceId, int audioLocation) {
            Attributable.setAttributionSource(device, mAttributionSource);
            forEach(
                    (cb) ->
                            cb.onVolumeOffsetAudioLocationChanged(
                                    device, instanceId, audioLocation));
        }

        @Override
        public void onVolumeOffsetAudioDescriptionChanged(
                @NonNull BluetoothDevice device, int instanceId, String audioDescription) {
            Attributable.setAttributionSource(device, mAttributionSource);
            forEach(
                    (cb) ->
                            cb.onVolumeOffsetAudioDescriptionChanged(
                                    device, instanceId, audioDescription));
        }

        @Override
        public void onDeviceVolumeChanged(@NonNull BluetoothDevice device, int volume) {
            Attributable.setAttributionSource(device, mAttributionSource);
            forEach((cb) -> cb.onDeviceVolumeChanged(device, volume));
        }
    }

    /**
     * Intent used to broadcast the change in connection state of the Volume Control profile.
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
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    @RequiresBluetoothConnectPermission
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.volume-control.profile.action.CONNECTION_STATE_CHANGED";

    private BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;

    private IBluetoothVolumeControl mService;

    /**
     * Create a BluetoothVolumeControl proxy object for interacting with the local Bluetooth Volume
     * Control service.
     */
    /*package*/ BluetoothVolumeControl(Context context, BluetoothAdapter adapter) {
        mAdapter = adapter;
        mAttributionSource = adapter.getAttributionSource();
        mService = null;

        mCloseGuard = new CloseGuard();
        mCloseGuard.open("close");
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    @SuppressWarnings("Finalize") // TODO(b/314811467)
    protected void finalize() {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        close();
    }

    /**
     * Close this VolumeControl server instance.
     *
     * <p>Application should call this method as early as possible after it is done with this
     * VolumeControl server.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    @Override
    public void close() {
        if (VDBG) log("close()");

        mAdapter.closeProfileProxy(this);
    }

    /** @hide */
    @Override
    public void onServiceConnected(IBinder service) {
        mService = IBluetoothVolumeControl.Stub.asInterface(service);
        // re-register the service-to-app callback
        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.isEmpty()) {
                return;
            }
            try {
                mService.registerCallback(mCallback, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, "onBluetoothServiceUp: Failed to register VolumeControl callback", e);
            }
        }
    }

    /** @hide */
    @Override
    public void onServiceDisconnected() {
        mService = null;
    }

    private IBluetoothVolumeControl getService() {
        return mService;
    }

    /** @hide */
    @Override
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Get the list of connected devices. Currently at most one.
     *
     * @return list of connected devices
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            })
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        if (DBG) log("getConnectedDevices()");
        final IBluetoothVolumeControl service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return Attributable.setAttributionSource(
                        service.getConnectedDevices(mAttributionSource), mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return Collections.emptyList();
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
        final IBluetoothVolumeControl service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return Attributable.setAttributionSource(
                        service.getDevicesMatchingConnectionStates(states, mAttributionSource),
                        mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return Collections.emptyList();
    }

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
        final IBluetoothVolumeControl service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionState(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Register a {@link Callback} that will be invoked during the operation of this profile.
     *
     * <p>Repeated registration of the same <var>callback</var> object will have no effect after the
     * first call to this method, even when the <var>executor</var> is different. API caller would
     * have to call {@link #unregisterCallback(Callback)} with the same callback object before
     * registering it again.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link Callback}
     * @throws IllegalArgumentException if a null executor, sink, or callback is given
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            })
    public void registerCallback(
            @NonNull @CallbackExecutor Executor executor, @NonNull Callback callback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");
        if (DBG) log("registerCallback");
        synchronized (mCallbackExecutorMap) {
            if (!mAdapter.isEnabled()) {
                /* If Bluetooth is off, just store callback and it will be registered
                 * when Bluetooth is on
                 */
                mCallbackExecutorMap.put(callback, executor);
                return;
            }

            // Adds the passed in callback to our map of callbacks to executors
            if (mCallbackExecutorMap.containsKey(callback)) {
                throw new IllegalArgumentException("This callback has already been registered");
            }

            final IBluetoothVolumeControl service = getService();
            if (service == null) {
                return;
            }
            try {
                /* If the callback map is empty, we register the service-to-app callback.
                 *  Otherwise, callback is registered in mCallbackExecutorMap and we just notify
                 *  user over callback with current values.
                 */
                boolean isRegisterCallbackRequired = mCallbackExecutorMap.isEmpty();
                mCallbackExecutorMap.put(callback, executor);

                if (isRegisterCallbackRequired) {
                    service.registerCallback(mCallback, mAttributionSource);
                } else {
                    service.notifyNewRegisteredCallback(
                            new VolumeControlNotifyCallback(Map.of(callback, executor)),
                            mAttributionSource);
                }
            } catch (RemoteException e) {
                mCallbackExecutorMap.remove(callback);
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                throw e.rethrowAsRuntimeException();
            }
        }
    }

    /**
     * Unregister the specified {@link Callback}.
     *
     * <p>The same {@link Callback} object used when calling {@link #registerCallback(Executor,
     * Callback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when application process goes away
     *
     * @param callback user implementation of the {@link Callback}
     * @throws IllegalArgumentException when callback is null or when no callback is registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
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

            if (!mCallbackExecutorMap.isEmpty()) {
                return;
            }
        }

        // If the callback map is empty, we unregister the service-to-app callback
        try {
            final IBluetoothVolumeControl service = getService();
            if (service != null) {
                service.unregisterCallback(mCallback, mAttributionSource);
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            throw e.rethrowAsRuntimeException();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        }
    }

    /**
     * Tells the remote device to set a volume offset to the absolute volume.
     *
     * @param device {@link BluetoothDevice} representing the remote device
     * @param volumeOffset volume offset to be set on the remote device
     * @deprecated Use new method which allows for choosing a VOCS instance. This method will always
     *     use the first instance.
     * @hide
     */
    @Deprecated
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            })
    public void setVolumeOffset(
            @NonNull BluetoothDevice device, @IntRange(from = -255, to = 255) int volumeOffset) {
        final int defaultInstanceId = 1;
        setVolumeOffsetInternal(device, defaultInstanceId, volumeOffset);
    }

    /**
     * Tells the remote device to set a volume offset to the absolute volume. One device might have
     * multiple VOCS instances. This instances could be i.e. different speakers or sound types as
     * media/voice/notification.
     *
     * @param device {@link BluetoothDevice} representing the remote device
     * @param instanceId identifier of VOCS instance on the remote device. Identifiers are numerated
     *     from 1. Number of them was notified by callbacks and it can be read using {@link
     *     #getNumberOfVolumeOffsetInstances(BluetoothDevice)}. Providing non existing instance ID
     *     will be ignored
     * @param volumeOffset volume offset to be set on VOCS instance
     * @hide
     */
    @FlaggedApi(Flags.FLAG_LEAUDIO_MULTIPLE_VOCS_INSTANCES_API)
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            })
    public void setVolumeOffset(
            @NonNull BluetoothDevice device,
            @IntRange(from = 1, to = 255) int instanceId,
            @IntRange(from = -255, to = 255) int volumeOffset) {
        setVolumeOffsetInternal(device, instanceId, volumeOffset);
    }

    /**
     * INTERNAL HELPER METHOD, DO NOT MAKE PUBLIC
     *
     * <p>Tells the remote device to set a volume offset to the absolute volume. One device might
     * have multiple VOCS instances. This instances could be i.e. different speakers or sound types
     * as media/voice/notification.
     *
     * @param device {@link BluetoothDevice} representing the remote device
     * @param instanceId identifier of VOCS instance on the remote device. Identifiers are numerated
     *     from 1. Number of them was notified by callbacks and it can be read using {@link
     *     #getNumberOfVolumeOffsetInstances(BluetoothDevice)}. Providing non existing instance ID
     *     will be ignored
     * @param volumeOffset volume offset to be set on VOCS instance
     * @hide
     */
    private void setVolumeOffsetInternal(
            @NonNull BluetoothDevice device,
            @IntRange(from = 1, to = 255) int instanceId,
            @IntRange(from = -255, to = 255) int volumeOffset) {
        if (DBG) {
            log(
                    "setVolumeOffset("
                            + device
                            + "/"
                            + instanceId
                            + " volumeOffset: "
                            + volumeOffset
                            + ")");
        }
        final IBluetoothVolumeControl service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.setVolumeOffset(device, instanceId, volumeOffset, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Provides information about the possibility to set volume offset on the remote device. If the
     * remote device supports Volume Offset Control Service, it is automatically connected.
     *
     * @param device {@link BluetoothDevice} representing the remote device
     * @return {@code true} if volume offset function is supported and available to use on the
     *     remote device. When Bluetooth is off, the return value should always be {@code false}.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            })
    public boolean isVolumeOffsetAvailable(@NonNull BluetoothDevice device) {
        if (DBG) log("isVolumeOffsetAvailable(" + device + ")");
        final IBluetoothVolumeControl service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
            return false;
        }

        if (!isEnabled()) {
            return false;
        }

        try {
            return service.isVolumeOffsetAvailable(device, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        }

        return false;
    }

    /**
     * Provides information about the number of volume offset instances
     *
     * @param device {@link BluetoothDevice} representing the remote device
     * @return number of VOCS instances. When Bluetooth is off, the return value is 0.
     * @hide
     */
    @FlaggedApi(Flags.FLAG_LEAUDIO_MULTIPLE_VOCS_INSTANCES_API)
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            })
    public int getNumberOfVolumeOffsetInstances(@NonNull BluetoothDevice device) {
        if (DBG) log("getNumberOfVolumeOffsetInstances(" + device + ")");
        final IBluetoothVolumeControl service = getService();
        final int defaultValue = 0;

        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
            return defaultValue;
        }

        if (!isEnabled()) {
            return defaultValue;
        }
        try {
            return service.getNumberOfVolumeOffsetInstances(device, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        }

        return defaultValue;
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
    @RequiresPermission(
            allOf = {
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            })
    public boolean setConnectionPolicy(
            @NonNull BluetoothDevice device, @ConnectionPolicy int connectionPolicy) {
        if (DBG) log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        final IBluetoothVolumeControl service = getService();
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
    @RequiresPermission(
            allOf = {
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            })
    public @ConnectionPolicy int getConnectionPolicy(@NonNull BluetoothDevice device) {
        if (VDBG) log("getConnectionPolicy(" + device + ")");
        final IBluetoothVolumeControl service = getService();
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

    /**
     * Set volume for the le audio device
     *
     * <p>This provides volume control for connected remote device directly by volume control
     * service. The valid volume range is [0, 255], as defined in 2.3.1.1 Volume_Setting field of
     * Volume Control Service, Version 1.0.
     *
     * <p>For le audio unicast devices volume control, application should consider to use {@link
     * BluetoothLeAudio#setVolume} instead to control active device volume.
     *
     * @param device {@link BluetoothDevice} representing the remote device
     * @param volume level to set
     * @param isGroupOperation {@code true} if Application wants to perform this operation for all
     *     coordinated set members throughout this session. Otherwise, caller would have to control
     *     individual device volume.
     * @throws IllegalArgumentException if volume is not in the range [0, 255].
     * @hide
     */
    @FlaggedApi(Flags.FLAG_LEAUDIO_BROADCAST_VOLUME_CONTROL_FOR_CONNECTED_DEVICES)
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_PRIVILEGED,
            })
    public void setDeviceVolume(
            @NonNull BluetoothDevice device,
            @IntRange(from = 0, to = 255) int volume,
            boolean isGroupOperation) {
        if (volume < 0 || volume > 255) {
            throw new IllegalArgumentException("illegal volume " + volume);
        }
        final IBluetoothVolumeControl service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.setDeviceVolume(device, volume, isGroupOperation, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
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

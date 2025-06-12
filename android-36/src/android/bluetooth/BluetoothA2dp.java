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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothAdminPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class provides the public APIs to control the Bluetooth A2DP profile.
 *
 * <p>BluetoothA2dp is a proxy object for controlling the Bluetooth A2DP Service via IPC. Use {@link
 * BluetoothAdapter#getProfileProxy} to get the BluetoothA2dp proxy object.
 *
 * <p>Android only supports one connected Bluetooth A2dp device at a time. Each method is protected
 * with its appropriate permission.
 */
public final class BluetoothA2dp implements BluetoothProfile {
    private static final String TAG = "BluetoothA2dp";

    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    /**
     * Intent used to broadcast the change in connection state of the A2DP profile.
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
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED";

    /**
     * Intent used to broadcast the change in the Playing state of the A2DP profile.
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
     * #STATE_PLAYING}, {@link #STATE_NOT_PLAYING},
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_PLAYING_STATE_CHANGED =
            "android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED";

    /** @hide */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_AVRCP_CONNECTION_STATE_CHANGED =
            "android.bluetooth.a2dp.profile.action.AVRCP_CONNECTION_STATE_CHANGED";

    /**
     * Intent used to broadcast the selection of a connected device as active.
     *
     * <p>This intent will have one extra:
     *
     * <ul>
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device. It can be null if no device
     *       is active.
     * </ul>
     *
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SuppressLint("ActionValue")
    public static final String ACTION_ACTIVE_DEVICE_CHANGED =
            "android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED";

    /**
     * Intent used to broadcast the change in the Audio Codec state of the A2DP Source profile.
     *
     * <p>This intent will have 2 extras:
     *
     * <ul>
     *   <li>{@link BluetoothCodecStatus#EXTRA_CODEC_STATUS} - The codec status.
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device if the device is currently
     *       connected, otherwise it is not included.
     * </ul>
     *
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SuppressLint("ActionValue")
    public static final String ACTION_CODEC_CONFIG_CHANGED =
            "android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED";

    /**
     * A2DP sink device is streaming music. This state can be one of {@link #EXTRA_STATE} or {@link
     * #EXTRA_PREVIOUS_STATE} of {@link #ACTION_PLAYING_STATE_CHANGED} intent.
     */
    public static final int STATE_PLAYING = 10;

    /**
     * A2DP sink device is NOT streaming music. This state can be one of {@link #EXTRA_STATE} or
     * {@link #EXTRA_PREVIOUS_STATE} of {@link #ACTION_PLAYING_STATE_CHANGED} intent.
     */
    public static final int STATE_NOT_PLAYING = 11;

    /** @hide */
    @IntDef(
            prefix = "OPTIONAL_CODECS_",
            value = {
                OPTIONAL_CODECS_SUPPORT_UNKNOWN,
                OPTIONAL_CODECS_NOT_SUPPORTED,
                OPTIONAL_CODECS_SUPPORTED
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface OptionalCodecsSupportStatus {}

    /**
     * We don't have a stored preference for whether or not the given A2DP sink device supports
     * optional codecs.
     *
     * @hide
     */
    @SystemApi public static final int OPTIONAL_CODECS_SUPPORT_UNKNOWN = -1;

    /**
     * The given A2DP sink device does not support optional codecs.
     *
     * @hide
     */
    @SystemApi public static final int OPTIONAL_CODECS_NOT_SUPPORTED = 0;

    /**
     * The given A2DP sink device does support optional codecs.
     *
     * @hide
     */
    @SystemApi public static final int OPTIONAL_CODECS_SUPPORTED = 1;

    /** @hide */
    @IntDef(
            prefix = "OPTIONAL_CODECS_PREF_",
            value = {
                OPTIONAL_CODECS_PREF_UNKNOWN,
                OPTIONAL_CODECS_PREF_DISABLED,
                OPTIONAL_CODECS_PREF_ENABLED
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface OptionalCodecsPreferenceStatus {}

    /**
     * We don't have a stored preference for whether optional codecs should be enabled or disabled
     * for the given A2DP device.
     *
     * @hide
     */
    @SystemApi public static final int OPTIONAL_CODECS_PREF_UNKNOWN = -1;

    /**
     * Optional codecs should be disabled for the given A2DP device.
     *
     * @hide
     */
    @SystemApi public static final int OPTIONAL_CODECS_PREF_DISABLED = 0;

    /**
     * Optional codecs should be enabled for the given A2DP device.
     *
     * @hide
     */
    @SystemApi public static final int OPTIONAL_CODECS_PREF_ENABLED = 1;

    /** @hide */
    @IntDef(
            prefix = "DYNAMIC_BUFFER_SUPPORT_",
            value = {
                DYNAMIC_BUFFER_SUPPORT_NONE,
                DYNAMIC_BUFFER_SUPPORT_A2DP_OFFLOAD,
                DYNAMIC_BUFFER_SUPPORT_A2DP_SOFTWARE_ENCODING
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}

    /**
     * Indicates the supported type of Dynamic Audio Buffer is not supported.
     *
     * @hide
     */
    @SystemApi public static final int DYNAMIC_BUFFER_SUPPORT_NONE = 0;

    /**
     * Indicates the supported type of Dynamic Audio Buffer is A2DP offload.
     *
     * @hide
     */
    @SystemApi public static final int DYNAMIC_BUFFER_SUPPORT_A2DP_OFFLOAD = 1;

    /**
     * Indicates the supported type of Dynamic Audio Buffer is A2DP software encoding.
     *
     * @hide
     */
    @SystemApi public static final int DYNAMIC_BUFFER_SUPPORT_A2DP_SOFTWARE_ENCODING = 2;

    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;

    private IBluetoothA2dp mService;

    /**
     * Create a BluetoothA2dp proxy object for interacting with the local Bluetooth A2DP service.
     */
    /* package */ BluetoothA2dp(Context context, BluetoothAdapter adapter) {
        mAdapter = adapter;
        mAttributionSource = adapter.getAttributionSource();
        mService = null;
    }

    /** @hide */
    @UnsupportedAppUsage
    public void close() {
        mAdapter.closeProfileProxy(this);
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceConnected(IBinder service) {
        mService = IBluetoothA2dp.Stub.asInterface(service);
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceDisconnected() {
        mService = null;
    }

    private IBluetoothA2dp getService() {
        return mService;
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    @SuppressWarnings("Finalize") // empty finalize for api signature
    public void finalize() {
        // The empty finalize needs to be kept or the
        // cts signature tests would fail.
    }

    /**
     * Initiate connection to a profile of the remote Bluetooth device.
     *
     * <p>This API returns false in scenarios like the profile on the device is already connected or
     * Bluetooth is not turned on. When this API returns true, it is guaranteed that connection
     * state intent for the profile will be broadcasted with the state. Users can get the connection
     * state of the profile from this intent.
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error, true otherwise
     * @hide
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @UnsupportedAppUsage
    public boolean connect(BluetoothDevice device) {
        if (DBG) log("connect(" + device + ")");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.connect(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Initiate disconnection from a profile
     *
     * <p>This API will return false in scenarios like the profile on the Bluetooth device is not in
     * connected state etc. When this API returns, true, it is guaranteed that the connection state
     * change intent will be broadcasted with the state. Users can get the disconnection state of
     * the profile from this intent.
     *
     * <p>If the disconnection is initiated by a remote device, the state will transition from
     * {@link #STATE_CONNECTED} to {@link #STATE_DISCONNECTED}. If the disconnect is initiated by
     * the host (local) device the state will transition from {@link #STATE_CONNECTED} to state
     * {@link #STATE_DISCONNECTING} to state {@link #STATE_DISCONNECTED}. The transition to {@link
     * #STATE_DISCONNECTING} can be used to distinguish between the two scenarios.
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error, true otherwise
     * @hide
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @UnsupportedAppUsage
    public boolean disconnect(BluetoothDevice device) {
        if (DBG) log("disconnect(" + device + ")");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.disconnect(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public List<BluetoothDevice> getConnectedDevices() {
        if (VDBG) log("getConnectedDevices()");
        final IBluetoothA2dp service = getService();
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

    /** {@inheritDoc} */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (VDBG) log("getDevicesMatchingStates()");
        final IBluetoothA2dp service = getService();
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

    /** {@inheritDoc} */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public @BtProfileState int getConnectionState(BluetoothDevice device) {
        if (VDBG) log("getState(" + device + ")");
        final IBluetoothA2dp service = getService();
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
     * Select a connected device as active.
     *
     * <p>The active device selection is per profile. An active device's purpose is
     * profile-specific. For example, A2DP audio streaming is to the active A2DP Sink device. If a
     * remote device is not connected, it cannot be selected as active.
     *
     * <p>This API returns false in scenarios like the profile on the device is not connected or
     * Bluetooth is not turned on. When this API returns true, it is guaranteed that the {@link
     * #ACTION_ACTIVE_DEVICE_CHANGED} intent will be broadcasted with the active device.
     *
     * @param device the remote Bluetooth device. Could be null to clear the active device and stop
     *     streaming audio to a Bluetooth device.
     * @return false on immediate error, true otherwise
     * @hide
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    @UnsupportedAppUsage(trackingBug = 171933273)
    public boolean setActiveDevice(@Nullable BluetoothDevice device) {
        if (DBG) log("setActiveDevice(" + device + ")");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && ((device == null) || isValidDevice(device))) {
            try {
                return service.setActiveDevice(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Get the connected device that is active.
     *
     * @return the connected device that is active or null if no device is active
     * @hide
     */
    @UnsupportedAppUsage(trackingBug = 171933273)
    @Nullable
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public BluetoothDevice getActiveDevice() {
        if (VDBG) log("getActiveDevice()");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return Attributable.setAttributionSource(
                        service.getActiveDevice(mAttributionSource), mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return null;
    }

    /**
     * Set priority of the profile
     *
     * <p>The device should already be paired. Priority can be one of {@link #PRIORITY_ON} or {@link
     * #PRIORITY_OFF}
     *
     * @param device Paired bluetooth device
     * @return true if priority is set, false on error
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public boolean setPriority(BluetoothDevice device, int priority) {
        if (DBG) log("setPriority(" + device + ", " + priority + ")");
        return setConnectionPolicy(device, BluetoothAdapter.priorityToConnectionPolicy(priority));
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
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public boolean setConnectionPolicy(
            @NonNull BluetoothDevice device, @ConnectionPolicy int connectionPolicy) {
        if (DBG) log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        final IBluetoothA2dp service = getService();
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
     * Get the priority of the profile.
     *
     * <p>The priority can be any of: {@link #PRIORITY_OFF}, {@link #PRIORITY_ON}, {@link
     * #PRIORITY_UNDEFINED}
     *
     * @param device Bluetooth device
     * @return priority of the device
     * @hide
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    public int getPriority(BluetoothDevice device) {
        if (VDBG) log("getPriority(" + device + ")");
        return BluetoothAdapter.connectionPolicyToPriority(getConnectionPolicy(device));
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
    public @ConnectionPolicy int getConnectionPolicy(@NonNull BluetoothDevice device) {
        if (VDBG) log("getConnectionPolicy(" + device + ")");
        final IBluetoothA2dp service = getService();
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
     * Tells remote device to set an absolute volume. Only if absolute volume is supported
     *
     * @param volume Absolute volume to be set on AVRCP side
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public void setAvrcpAbsoluteVolume(int volume) {
        if (DBG) Log.d(TAG, "setAvrcpAbsoluteVolume");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.setAvrcpAbsoluteVolume(volume, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Check if A2DP profile is streaming music.
     *
     * @param device BluetoothDevice device
     */
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean isA2dpPlaying(BluetoothDevice device) {
        if (DBG) log("isA2dpPlaying(" + device + ")");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.isA2dpPlaying(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * This function checks if the remote device is an AVCRP target and thus whether we should send
     * volume keys changes or not.
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(BLUETOOTH_CONNECT)
    public boolean shouldSendVolumeKeys(BluetoothDevice device) {
        if (isEnabled() && isValidDevice(device)) {
            ParcelUuid[] uuids = device.getUuids();
            if (uuids == null) return false;

            for (ParcelUuid uuid : uuids) {
                if (uuid.equals(BluetoothUuid.AVRCP_TARGET)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the list of source codecs that are supported by the current platform.
     *
     * <p>The list always includes the mandatory SBC codec, and may include optional proprietary
     * codecs.
     *
     * @return list of supported source codec types
     */
    @NonNull
    @RequiresLegacyBluetoothPermission
    @RequiresPermission(BLUETOOTH_PRIVILEGED)
    public Collection<BluetoothCodecType> getSupportedCodecTypes() {
        Log.d(TAG, "getSupportedSourceCodecTypes()");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.getSupportedCodecTypes();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return Collections.emptyList();
    }

    /**
     * Gets the current codec status (configuration and capability).
     *
     * <p>This method requires the calling app to have the {@link
     * android.Manifest.permission#BLUETOOTH_CONNECT} permission. Additionally, an app must either
     * have the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} or be associated with the
     * Companion Device manager (see {@link android.companion.CompanionDeviceManager#associate(
     * AssociationRequest, android.companion.CompanionDeviceManager.Callback, Handler)})
     *
     * @param device the remote Bluetooth device.
     * @return the current codec status
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED},
            conditional = true)
    public @Nullable BluetoothCodecStatus getCodecStatus(@NonNull BluetoothDevice device) {
        if (DBG) Log.d(TAG, "getCodecStatus(" + device + ")");
        verifyDeviceNotNull(device, "getCodecStatus");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.getCodecStatus(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return null;
    }

    /**
     * Sets the codec configuration preference.
     *
     * <p>This method requires the calling app to have the {@link
     * android.Manifest.permission#BLUETOOTH_CONNECT} permission. Additionally, an app must either
     * have the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} or be associated with the
     * Companion Device manager (see {@link android.companion.CompanionDeviceManager#associate(
     * AssociationRequest, android.companion.CompanionDeviceManager.Callback, Handler)})
     *
     * @param device the remote Bluetooth device.
     * @param codecConfig the codec configuration preference
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED},
            conditional = true)
    public void setCodecConfigPreference(
            @NonNull BluetoothDevice device, @NonNull BluetoothCodecConfig codecConfig) {
        if (DBG) Log.d(TAG, "setCodecConfigPreference(" + device + ")");
        verifyDeviceNotNull(device, "setCodecConfigPreference");
        if (codecConfig == null) {
            Log.e(TAG, "setCodecConfigPreference: Codec config can't be null");
            throw new IllegalArgumentException("codecConfig cannot be null");
        }
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                service.setCodecConfigPreference(device, codecConfig, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Enables the optional codecs for the given device for this connection.
     *
     * <p>If the given device supports another codec type than {@link
     * BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC}, this will switch to it. Switching from one codec
     * to another will create a short audio drop. In case of multiple applications calling the
     * method, the last call will be taken into account, overriding any previous call
     *
     * <p>See {@link #setOptionalCodecsEnabled} to enable optional codecs by default when the given
     * device is connected.
     *
     * @param device the remote Bluetooth device
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public void enableOptionalCodecs(@NonNull BluetoothDevice device) {
        if (DBG) Log.d(TAG, "enableOptionalCodecs(" + device + ")");
        verifyDeviceNotNull(device, "enableOptionalCodecs");
        enableDisableOptionalCodecs(device, true);
    }

    /**
     * Disables the optional codecs for the given device for this connection.
     *
     * <p>When optional codecs are disabled, the device will use the default Bluetooth audio codec
     * type. Switching from one codec to another will create a short audio drop. In case of multiple
     * applications calling the method, the last call will be taken into account, overriding any
     * previous call
     *
     * <p>See {@link BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC}. See {@link
     * #setOptionalCodecsEnabled} to disable optional codecs by default when the given device is
     * connected.
     *
     * @param device the remote Bluetooth device
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public void disableOptionalCodecs(@NonNull BluetoothDevice device) {
        if (DBG) Log.d(TAG, "disableOptionalCodecs(" + device + ")");
        verifyDeviceNotNull(device, "disableOptionalCodecs");
        enableDisableOptionalCodecs(device, false);
    }

    /**
     * Enables or disables the optional codecs.
     *
     * @param device the remote Bluetooth device.
     * @param enable if true, enable the optional codecs, otherwise disable them
     */
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    private void enableDisableOptionalCodecs(BluetoothDevice device, boolean enable) {
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                if (enable) {
                    service.enableOptionalCodecs(device, mAttributionSource);
                } else {
                    service.disableOptionalCodecs(device, mAttributionSource);
                }
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Returns whether this device supports optional codecs.
     *
     * @param device the remote Bluetooth device
     * @return whether the optional codecs are supported or not, or {@link
     *     #OPTIONAL_CODECS_SUPPORT_UNKNOWN} if the state can't be retrieved.
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    @OptionalCodecsSupportStatus
    public int isOptionalCodecsSupported(@NonNull BluetoothDevice device) {
        if (DBG) log("isOptionalCodecsSupported(" + device + ")");
        verifyDeviceNotNull(device, "isOptionalCodecsSupported");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.isOptionalCodecsSupported(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return OPTIONAL_CODECS_SUPPORT_UNKNOWN;
    }

    /**
     * Returns whether this device has its optional codecs enabled.
     *
     * @param device the remote Bluetooth device
     * @return whether the optional codecs are enabled or not, or {@link
     *     #OPTIONAL_CODECS_PREF_UNKNOWN} if the state can't be retrieved.
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    @OptionalCodecsPreferenceStatus
    public int isOptionalCodecsEnabled(@NonNull BluetoothDevice device) {
        if (DBG) log("isOptionalCodecsEnabled(" + device + ")");
        verifyDeviceNotNull(device, "isOptionalCodecsEnabled");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                return service.isOptionalCodecsEnabled(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return OPTIONAL_CODECS_PREF_UNKNOWN;
    }

    /**
     * Sets the default state of optional codecs for the given device.
     *
     * <p>Automatically enables or disables the optional codecs for the given device when connected.
     *
     * @param device the remote Bluetooth device
     * @param value whether the optional codecs should be enabled for this device
     * @hide
     */
    @SystemApi
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public void setOptionalCodecsEnabled(
            @NonNull BluetoothDevice device, @OptionalCodecsPreferenceStatus int value) {
        if (DBG) log("setOptionalCodecsEnabled(" + device + ")");
        verifyDeviceNotNull(device, "setOptionalCodecsEnabled");
        if (value != BluetoothA2dp.OPTIONAL_CODECS_PREF_UNKNOWN
                && value != BluetoothA2dp.OPTIONAL_CODECS_PREF_DISABLED
                && value != BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED) {
            Log.e(TAG, "Invalid value passed to setOptionalCodecsEnabled: " + value);
            return;
        }
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                service.setOptionalCodecsEnabled(device, value, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Get the supported type of the Dynamic Audio Buffer.
     *
     * <p>Possible return values are {@link #DYNAMIC_BUFFER_SUPPORT_NONE}, {@link
     * #DYNAMIC_BUFFER_SUPPORT_A2DP_OFFLOAD}, {@link
     * #DYNAMIC_BUFFER_SUPPORT_A2DP_SOFTWARE_ENCODING}.
     *
     * @return supported type of Dynamic Audio Buffer feature
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public @Type int getDynamicBufferSupport() {
        if (VDBG) log("getDynamicBufferSupport()");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.getDynamicBufferSupport(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return DYNAMIC_BUFFER_SUPPORT_NONE;
    }

    /**
     * Return the record of {@link BufferConstraints} object that has the default/maximum/minimum
     * audio buffer. This can be used to inform what the controller has support for the audio
     * buffer.
     *
     * @return a record with {@link BufferConstraints} or null if report is unavailable or
     *     unsupported
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public @Nullable BufferConstraints getBufferConstraints() {
        if (VDBG) log("getBufferConstraints()");
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.getBufferConstraints(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return null;
    }

    /**
     * Set Dynamic Audio Buffer Size.
     *
     * @param codec audio codec
     * @param value buffer millis
     * @return true to indicate success, or false on immediate error
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(
            allOf = {
                BLUETOOTH_CONNECT,
                BLUETOOTH_PRIVILEGED,
            })
    public boolean setBufferLengthMillis(
            @BluetoothCodecConfig.SourceCodecType int codec, int value) {
        if (VDBG) log("setBufferLengthMillis(" + codec + ", " + value + ")");
        if (value < 0) {
            Log.e(TAG, "Trying to set audio buffer length to a negative value: " + value);
            return false;
        }
        final IBluetoothA2dp service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                return service.setBufferLengthMillis(codec, value, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return false;
    }

    /**
     * Helper for converting a state to a string.
     *
     * <p>For debug use only - strings are not internationalized.
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    public static String stateToString(int state) {
        switch (state) {
            case STATE_DISCONNECTED:
                return "disconnected";
            case STATE_CONNECTING:
                return "connecting";
            case STATE_CONNECTED:
                return "connected";
            case STATE_DISCONNECTING:
                return "disconnecting";
            case STATE_PLAYING:
                return "playing";
            case STATE_NOT_PLAYING:
                return "not playing";
            default:
                return "<unknown state " + state + ">";
        }
    }

    private boolean isEnabled() {
        if (mAdapter.getState() == BluetoothAdapter.STATE_ON) return true;
        return false;
    }

    private void verifyDeviceNotNull(BluetoothDevice device, String methodName) {
        if (device == null) {
            Log.e(TAG, methodName + ": device param is null");
            throw new IllegalArgumentException("Device cannot be null");
        }
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

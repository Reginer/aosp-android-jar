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
import static android.bluetooth.BluetoothUtils.callServiceIfEnabled;

import static java.util.Objects.requireNonNull;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * This class provides a public APIs to control the Bluetooth Hearing Access Profile client service.
 *
 * <p>BluetoothHapClient is a proxy object for controlling the Bluetooth HAP Service client via IPC.
 * Use {@link BluetoothAdapter#getProfileProxy} to get the BluetoothHapClient proxy object.
 *
 * @hide
 */
@SystemApi
public final class BluetoothHapClient implements BluetoothProfile, AutoCloseable {
    private static final String TAG = BluetoothHapClient.class.getSimpleName();

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
        @IntDef(
                value = {
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
         * @hide
         */
        @SystemApi
        void onPresetSelected(
                @NonNull BluetoothDevice device,
                int presetIndex,
                @PresetSelectionReason int reason);

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
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
         * @hide
         */
        @SystemApi
        void onPresetSelectionFailed(
                @NonNull BluetoothDevice device, @PresetSelectionFailureReason int reason);

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
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
         * <p>The implementation will try to restore the state for every device back to original
         *
         * @param hapGroupId valid HAP group ID,
         * @param reason failure reason.
         * @hide
         */
        @SystemApi
        void onPresetSelectionForGroupFailed(
                int hapGroupId, @GroupPresetSelectionFailureReason int reason);

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
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
         * @hide
         */
        @SystemApi
        void onPresetInfoChanged(
                @NonNull BluetoothDevice device,
                @NonNull List<BluetoothHapPresetInfo> presetInfoList,
                @PresetInfoChangeReason int reason);

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
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
        void onSetPresetNameFailed(
                @NonNull BluetoothDevice device, @PresetNameChangeFailureReason int reason);

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
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
         * <p>The implementation will try to restore the state for every device back to original
         *
         * @param hapGroupId valid HAP group ID,
         * @param reason Failure reason code.
         * @hide
         */
        @SystemApi
        void onSetPresetNameForGroupFailed(
                int hapGroupId, @GroupPresetNameChangeFailureReason int reason);
    }

    private final CallbackWrapper<Callback, IBluetoothHapClient> mCallbackWrapper;

    private final IBluetoothHapClientCallback mCallback = new HapClientNotifyCallback();

    private class HapClientNotifyCallback extends IBluetoothHapClientCallback.Stub {
        @Override
        public void onPresetSelected(BluetoothDevice device, int presetIndex, int reason) {
            Attributable.setAttributionSource(device, mAttributionSource);
            mCallbackWrapper.forEach((cb) -> cb.onPresetSelected(device, presetIndex, reason));
        }

        @Override
        public void onPresetSelectionFailed(BluetoothDevice device, int status) {
            Attributable.setAttributionSource(device, mAttributionSource);
            mCallbackWrapper.forEach((cb) -> cb.onPresetSelectionFailed(device, status));
        }

        @Override
        public void onPresetSelectionForGroupFailed(int groupId, int status) {
            mCallbackWrapper.forEach((cb) -> cb.onPresetSelectionForGroupFailed(groupId, status));
        }

        @Override
        public void onPresetInfoChanged(
                BluetoothDevice device, List<BluetoothHapPresetInfo> presets, int status) {
            Attributable.setAttributionSource(device, mAttributionSource);
            mCallbackWrapper.forEach((cb) -> cb.onPresetInfoChanged(device, presets, status));
        }

        @Override
        public void onSetPresetNameFailed(BluetoothDevice device, int status) {
            Attributable.setAttributionSource(device, mAttributionSource);
            mCallbackWrapper.forEach((cb) -> cb.onSetPresetNameFailed(device, status));
        }

        @Override
        public void onSetPresetNameForGroupFailed(int hapGroupId, int status) {
            mCallbackWrapper.forEach((cb) -> cb.onSetPresetNameForGroupFailed(hapGroupId, status));
        }
    }

    /**
     * Intent used to broadcast the change in connection state of the Hearing Access Profile Client
     * service. Please note that in the binaural case, there will be two different LE devices for
     * the left and right side and each device will have their own connection state changes.
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
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_CONNECTION_STATE_CHANGED =
            "android.bluetooth.action.HAP_CONNECTION_STATE_CHANGED";

    /**
     * Intent used to broadcast the device availability change and the availability of its presets.
     * Please note that in the binaural case, there will be two different LE devices for the left
     * and right side and each device will have their own availability event.
     *
     * <p>This intent will have 2 extras:
     *
     * <ul>
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device.
     *   <li>{@link #EXTRA_HAP_FEATURES} - Supported features map.
     * </ul>
     *
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_DEVICE_AVAILABLE =
            "android.bluetooth.action.HAP_DEVICE_AVAILABLE";

    /**
     * Contains a list of all available presets
     *
     * @hide
     */
    public static final String EXTRA_HAP_FEATURES = "android.bluetooth.extra.HAP_FEATURES";

    /**
     * Represents an invalid index value. This is usually value returned in a currently active
     * preset request for a device which is not connected. This value shouldn't be used in the API
     * calls.
     *
     * @hide
     */
    @SystemApi
    public static final int PRESET_INDEX_UNAVAILABLE = IBluetoothHapClient.PRESET_INDEX_UNAVAILABLE;

    /**
     * Hearing aid type value. Indicates this Bluetooth device is belongs to a binaural hearing aid
     * set. A binaural hearing aid set is two hearing aids that form a Coordinated Set, one for the
     * right ear and one for the left ear of the user. Typically used by a user with bilateral
     * hearing loss.
     *
     * @hide
     */
    @SystemApi public static final int TYPE_BINAURAL = 0b00;

    /**
     * Hearing aid type value. Indicates this Bluetooth device is a single hearing aid for the left
     * or the right ear. Typically used by a user with unilateral hearing loss.
     *
     * @hide
     */
    @SystemApi
    public static final int TYPE_MONAURAL = 1 << IBluetoothHapClient.FEATURE_BIT_NUM_TYPE_MONAURAL;

    /**
     * Hearing aid type value. Indicates this Bluetooth device is two hearing aids with a connection
     * to one another that expose a single Bluetooth radio interface.
     *
     * @hide
     */
    @SystemApi
    public static final int TYPE_BANDED = 1 << IBluetoothHapClient.FEATURE_BIT_NUM_TYPE_BANDED;

    /**
     * Hearing aid type value. This value is reserved for future use.
     *
     * @hide
     */
    @SystemApi public static final int TYPE_RFU = TYPE_BANDED + 1;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            flag = true,
            value = {
                TYPE_BINAURAL,
                TYPE_MONAURAL,
                TYPE_BANDED,
                TYPE_RFU,
            })
    @interface HearingAidType {}

    /**
     * Feature mask value.
     *
     * @hide
     */
    public static final int FEATURE_HEARING_AID_TYPE_MASK = TYPE_MONAURAL | TYPE_BANDED;

    /**
     * Feature mask value.
     *
     * @hide
     */
    public static final int FEATURE_SYNCHRONIZATED_PRESETS_MASK =
            1 << IBluetoothHapClient.FEATURE_BIT_NUM_SYNCHRONIZATED_PRESETS;

    /**
     * Feature mask value.
     *
     * @hide
     */
    public static final int FEATURE_INDEPENDENT_PRESETS_MASK =
            1 << IBluetoothHapClient.FEATURE_BIT_NUM_INDEPENDENT_PRESETS;

    /**
     * Feature mask value.
     *
     * @hide
     */
    public static final int FEATURE_DYNAMIC_PRESETS_MASK =
            1 << IBluetoothHapClient.FEATURE_BIT_NUM_DYNAMIC_PRESETS;

    /**
     * Feature mask value.
     *
     * @hide
     */
    public static final int FEATURE_WRITABLE_PRESETS_MASK =
            1 << IBluetoothHapClient.FEATURE_BIT_NUM_WRITABLE_PRESETS;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            flag = true,
            value = {
                FEATURE_HEARING_AID_TYPE_MASK,
                FEATURE_SYNCHRONIZATED_PRESETS_MASK,
                FEATURE_INDEPENDENT_PRESETS_MASK,
                FEATURE_DYNAMIC_PRESETS_MASK,
                FEATURE_WRITABLE_PRESETS_MASK,
            })
    @interface FeatureMask {}

    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;

    private IBluetoothHapClient mService;

    /**
     * Create a BluetoothHapClient proxy object for interacting with the local Bluetooth Hearing
     * Access Profile (HAP) client.
     */
    @SuppressLint("AndroidFrameworkRequiresPermission") // Consumer wrongly report permission
    BluetoothHapClient(Context context, BluetoothAdapter adapter) {
        mAdapter = adapter;
        mAttributionSource = mAdapter.getAttributionSource();
        mService = null;

        Consumer<IBluetoothHapClient> registerConsumer =
                (IBluetoothHapClient service) -> {
                    try {
                        service.registerCallback(mCallback, mAttributionSource);
                    } catch (RemoteException e) {
                        Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                    }
                };
        Consumer<IBluetoothHapClient> unregisterConsumer =
                (IBluetoothHapClient service) -> {
                    try {
                        service.unregisterCallback(mCallback, mAttributionSource);
                    } catch (RemoteException e) {
                        Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                    }
                };

        mCallbackWrapper = new CallbackWrapper(registerConsumer, unregisterConsumer);
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
        Log.v(TAG, "close()");
        mAdapter.closeProfileProxy(this);
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceConnected(IBinder service) {
        mService = IBluetoothHapClient.Stub.asInterface(service);
        mCallbackWrapper.registerToNewService(mService);
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceDisconnected() {
        mService = null;
    }

    private IBluetoothHapClient getService() {
        return mService;
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Register a {@link Callback} that will be invoked during the operation of this profile.
     *
     * <p>Repeated registration of the same <var>callback</var> object after the first call to this
     * method will result with IllegalArgumentException being thrown, even when the
     * <var>executor</var> is different. API caller must call {@link #unregisterCallback(Callback)}
     * with the same callback object before registering it again.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link Callback}
     * @throws NullPointerException if a null executor, or callback is given, or
     *     IllegalArgumentException if the same <var>callback</var> is already registered.
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @SuppressLint("AndroidFrameworkRequiresPermission") // Consumer are fakely reporting permission
    public void registerCallback(
            @NonNull @CallbackExecutor Executor executor, @NonNull Callback callback) {
        mCallbackWrapper.registerCallback(mService, callback, executor);
    }

    /**
     * Unregister the specified {@link Callback}.
     *
     * <p>The same {@link Callback} object used when calling {@link #registerCallback(Executor,
     * Callback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when the application process goes away
     *
     * @param callback user implementation of the {@link Callback}
     * @throws NullPointerException when callback is null or IllegalArgumentException when no
     *     callback is registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @SuppressLint("AndroidFrameworkRequiresPermission") // Consumer are fakely reporting permission
    public void unregisterCallback(@NonNull Callback callback) {
        mCallbackWrapper.unregisterCallback(mService, callback);
    }

    /**
     * Set connection policy of the profile
     *
     * <p>The device should already be paired. Connection policy can be one of {@link
     * #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return {@code true} if connectionPolicy is set, {@code false} on error
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setConnectionPolicy(
            @NonNull BluetoothDevice device, @ConnectionPolicy int connectionPolicy) {
        requireNonNull(device);
        boolean defaultValue = false;
        if (!isValidDevice(device)
                || (connectionPolicy != BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                        && connectionPolicy != BluetoothProfile.CONNECTION_POLICY_ALLOWED)) {
            return defaultValue;
        }
        return callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.setConnectionPolicy(device, connectionPolicy, mAttributionSource),
                defaultValue);
    }

    /**
     * Get the connection policy of the profile.
     *
     * @param device Bluetooth device
     * @return connection policy of the device or {@link #CONNECTION_POLICY_FORBIDDEN} if device is
     *     null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @ConnectionPolicy int getConnectionPolicy(@Nullable BluetoothDevice device) {
        int defaultValue = BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
        if (!isValidDevice(device)) {
            return defaultValue;
        }
        return callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.getConnectionPolicy(device, mAttributionSource),
                defaultValue);
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @Override
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        return callServiceIfEnabled(
                mAdapter,
                this::getService,
                s ->
                        Attributable.setAttributionSource(
                                s.getConnectedDevices(mAttributionSource), mAttributionSource),
                Collections.emptyList());
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @Override
    @NonNull
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(@NonNull int[] states) {
        return callServiceIfEnabled(
                mAdapter,
                this::getService,
                s ->
                        Attributable.setAttributionSource(
                                s.getDevicesMatchingConnectionStates(states, mAttributionSource),
                                mAttributionSource),
                Collections.emptyList());
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @Override
    @BluetoothProfile.BtProfileState
    public int getConnectionState(@NonNull BluetoothDevice device) {
        int defaultValue = BluetoothProfile.STATE_DISCONNECTED;
        if (!isValidDevice(device)) {
            return defaultValue;
        }
        return callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.getConnectionState(device, mAttributionSource),
                defaultValue);
    }

    /**
     * Gets the group identifier, which can be used in the group related part of the API.
     *
     * <p>Users are expected to get group identifier for each of the connected device to discover
     * the device grouping. This allows them to make an informed decision which devices can be
     * controlled by single group API call and which require individual device calls.
     *
     * <p>Note that some binaural HA devices may not support group operations, therefore are not
     * considered a valid HAP group. In such case -1 is returned even if such device is a valid Le
     * Audio Coordinated Set member.
     *
     * @return valid group identifier or -1
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public int getHapGroup(@NonNull BluetoothDevice device) {
        requireNonNull(device);
        int defaultValue = BluetoothCsipSetCoordinator.GROUP_ID_INVALID;
        if (!isValidDevice(device)) {
            return defaultValue;
        }
        return callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.getHapGroup(device, mAttributionSource),
                defaultValue);
    }

    /**
     * Gets the currently active preset for a HA device.
     *
     * @param device is the device for which we want to set the active preset
     * @return active preset index
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public int getActivePresetIndex(@NonNull BluetoothDevice device) {
        requireNonNull(device);
        int defaultValue = PRESET_INDEX_UNAVAILABLE;
        if (!isValidDevice(device)) {
            return defaultValue;
        }
        return callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.getActivePresetIndex(device, mAttributionSource),
                defaultValue);
    }

    /**
     * Get the currently active preset info for a remote device.
     *
     * @param device is the device for which we want to get the preset name
     * @return currently active preset info if selected, null if preset info is not available for
     *     the remote device
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @Nullable BluetoothHapPresetInfo getActivePresetInfo(@NonNull BluetoothDevice device) {
        requireNonNull(device);
        BluetoothHapPresetInfo defaultValue = null;
        if (!isValidDevice(device)) {
            return defaultValue;
        }
        return callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.getActivePresetInfo(device, mAttributionSource),
                defaultValue);
    }

    /**
     * Selects the currently active preset for a HA device
     *
     * <p>On success, {@link Callback#onPresetSelected(BluetoothDevice, int, int)} will be called
     * with reason code {@link BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST} On failure, {@link
     * Callback#onPresetSelectionFailed(BluetoothDevice, int)} will be called.
     *
     * @param device is the device for which we want to set the active preset
     * @param presetIndex is an index of one of the available presets
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void selectPreset(@NonNull BluetoothDevice device, int presetIndex) {
        requireNonNull(device);
        if (!isValidDevice(device)) {
            return;
        }
        callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.selectPreset(device, presetIndex, mAttributionSource));
    }

    /**
     * Selects the currently active preset for a Hearing Aid device group.
     *
     * <p>This group call may replace multiple device calls if those are part of the valid HAS
     * group. Note that binaural HA devices may or may not support group.
     *
     * <p>On success, {@link Callback#onPresetSelected(BluetoothDevice, int, int)} will be called
     * for each device within the group with reason code {@link
     * BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST} On failure, {@link
     * Callback#onPresetSelectionForGroupFailed(int, int)} will be called for the group.
     *
     * @param groupId is the device group identifier for which want to set the active preset
     * @param presetIndex is an index of one of the available presets
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void selectPresetForGroup(int groupId, int presetIndex) {
        callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.selectPresetForGroup(groupId, presetIndex, mAttributionSource));
    }

    /**
     * Sets the next preset as a currently active preset for a HA device
     *
     * <p>Note that the meaning of 'next' is HA device implementation specific and does not
     * necessarily mean a higher preset index.
     *
     * @param device is the device for which we want to set the active preset
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void switchToNextPreset(@NonNull BluetoothDevice device) {
        requireNonNull(device);
        if (!isValidDevice(device)) {
            return;
        }
        callServiceIfEnabled(
                mAdapter, this::getService, s -> s.switchToNextPreset(device, mAttributionSource));
    }

    /**
     * Sets the next preset as a currently active preset for a HA device group
     *
     * <p>Note that the meaning of 'next' is HA device implementation specific and does not
     * necessarily mean a higher preset index.
     *
     * <p>This group call may replace multiple device calls if those are part of the valid HAS
     * group. Note that binaural HA devices may or may not support group.
     *
     * @param groupId is the device group identifier for which want to set the active preset
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void switchToNextPresetForGroup(int groupId) {
        callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.switchToNextPresetForGroup(groupId, mAttributionSource));
    }

    /**
     * Sets the previous preset as a currently active preset for a HA device.
     *
     * <p>Note that the meaning of 'previous' is HA device implementation specific and does not
     * necessarily mean a lower preset index.
     *
     * @param device is the device for which we want to set the active preset
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void switchToPreviousPreset(@NonNull BluetoothDevice device) {
        requireNonNull(device);
        if (!isValidDevice(device)) {
            return;
        }
        callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.switchToPreviousPreset(device, mAttributionSource));
    }

    /**
     * Sets the previous preset as a currently active preset for a HA device group
     *
     * <p>Note the meaning of 'previous' is HA device implementation specific and does not
     * necessarily mean a lower preset index.
     *
     * <p>This group call may replace multiple device calls if those are part of the valid HAS
     * group. Note that binaural HA devices may or may not support group.
     *
     * @param groupId is the device group identifier for which want to set the active preset
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void switchToPreviousPresetForGroup(int groupId) {
        callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.switchToPreviousPresetForGroup(groupId, mAttributionSource));
    }

    /**
     * Requests the preset info
     *
     * @param device is the device for which we want to get the preset
     * @param presetIndex is an index of an available presets
     * @return preset info
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @Nullable
    public BluetoothHapPresetInfo getPresetInfo(@NonNull BluetoothDevice device, int presetIndex) {
        requireNonNull(device);
        BluetoothHapPresetInfo defaultValue = null;
        if (!isValidDevice(device)) {
            return defaultValue;
        }
        return callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.getPresetInfo(device, presetIndex, mAttributionSource),
                defaultValue);
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
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @NonNull List<BluetoothHapPresetInfo> getAllPresetInfo(@NonNull BluetoothDevice device) {
        requireNonNull(device);
        List<BluetoothHapPresetInfo> defaultValue = Collections.emptyList();
        if (!isValidDevice(device)) {
            return defaultValue;
        }
        return callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.getAllPresetInfo(device, mAttributionSource),
                defaultValue);
    }

    /**
     * Requests HAP features
     *
     * @param device is the device for which we want to get features for
     * @return features value with feature bits set
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public int getFeatures(@NonNull BluetoothDevice device) {
        requireNonNull(device);
        int defaultValue = 0x00;
        if (!isValidDevice(device)) {
            return defaultValue;
        }
        return callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.getFeatures(device, mAttributionSource),
                defaultValue);
    }

    /**
     * Retrieves hearing aid type from feature value.
     *
     * @param device is the device for which we want to get the hearing aid type
     * @return hearing aid type
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @HearingAidType
    public int getHearingAidType(@NonNull BluetoothDevice device) {
        return getFeatures(device) & FEATURE_HEARING_AID_TYPE_MASK;
    }

    /**
     * Retrieves if this device supports synchronized presets or not from feature value.
     *
     * @param device is the device for which we want to know if it supports synchronized presets
     * @return {@code true} if the device supports synchronized presets, {@code false} otherwise
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean supportsSynchronizedPresets(@NonNull BluetoothDevice device) {
        return (getFeatures(device) & FEATURE_SYNCHRONIZATED_PRESETS_MASK)
                == FEATURE_SYNCHRONIZATED_PRESETS_MASK;
    }

    /**
     * Retrieves if this device supports independent presets or not from feature value.
     *
     * @param device is the device for which we want to know if it supports independent presets
     * @return {@code true} if the device supports independent presets, {@code false} otherwise
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean supportsIndependentPresets(@NonNull BluetoothDevice device) {
        return (getFeatures(device) & FEATURE_INDEPENDENT_PRESETS_MASK)
                == FEATURE_INDEPENDENT_PRESETS_MASK;
    }

    /**
     * Retrieves if this device supports dynamic presets or not from feature value.
     *
     * @param device is the device for which we want to know if it supports dynamic presets
     * @return {@code true} if the device supports dynamic presets, {@code false} otherwise
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean supportsDynamicPresets(@NonNull BluetoothDevice device) {
        return (getFeatures(device) & FEATURE_DYNAMIC_PRESETS_MASK) == FEATURE_DYNAMIC_PRESETS_MASK;
    }

    /**
     * Retrieves if this device supports writable presets or not from feature value.
     *
     * @param device is the device for which we want to know if it supports writable presets
     * @return {@code true} if the device supports writable presets, {@code false} otherwise
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean supportsWritablePresets(@NonNull BluetoothDevice device) {
        return (getFeatures(device) & FEATURE_WRITABLE_PRESETS_MASK)
                == FEATURE_WRITABLE_PRESETS_MASK;
    }

    /**
     * Sets the preset name for a particular device
     *
     * <p>Note that the name length is restricted to 40 characters.
     *
     * <p>On success, {@link Callback#onPresetInfoChanged(BluetoothDevice, List, int)} with a new
     * name will be called and reason code {@link BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST} On
     * failure, {@link Callback#onSetPresetNameFailed(BluetoothDevice, int)} will be called.
     *
     * @param device is the device for which we want to get the preset name
     * @param presetIndex is an index of one of the available presets
     * @param name is a new name for a preset, maximum length is 40 characters
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void setPresetName(
            @NonNull BluetoothDevice device, int presetIndex, @NonNull String name) {
        requireNonNull(device);
        if (!isValidDevice(device)) {
            return;
        }
        callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.setPresetName(device, presetIndex, name, mAttributionSource));
    }

    /**
     * Sets the name for a hearing aid preset.
     *
     * <p>Note that the name length is restricted to 40 characters.
     *
     * <p>On success, {@link Callback#onPresetInfoChanged(BluetoothDevice, List, int)} with a new
     * name will be called for each device within the group with reason code {@link
     * BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST} On failure, {@link
     * Callback#onSetPresetNameForGroupFailed(int, int)} will be invoked
     *
     * @param groupId is the device group identifier
     * @param presetIndex is an index of one of the available presets
     * @param name is a new name for a preset, maximum length is 40 characters
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void setPresetNameForGroup(int groupId, int presetIndex, @NonNull String name) {
        callServiceIfEnabled(
                mAdapter,
                this::getService,
                s -> s.setPresetNameForGroup(groupId, presetIndex, name, mAttributionSource));
    }

    private boolean isValidDevice(BluetoothDevice device) {
        if (device == null) return false;

        if (BluetoothAdapter.checkBluetoothAddress(device.getAddress())) return true;
        return false;
    }
}

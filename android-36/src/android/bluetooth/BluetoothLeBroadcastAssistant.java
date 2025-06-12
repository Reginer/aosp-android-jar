/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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
import static android.Manifest.permission.BLUETOOTH_SCAN;

import static java.util.Objects.requireNonNull;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresBluetoothLocationPermission;
import android.bluetooth.annotations.RequiresBluetoothScanPermission;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.AttributionSource;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;

import com.android.bluetooth.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * This class provides the public APIs for the LE Audio Broadcast Assistant role, which implements
 * client side control points for Broadcast Audio Scan Service (BASS).
 *
 * <p>An LE Audio Broadcast Assistant can help a Broadcast Sink to scan for available Broadcast
 * Sources. The Broadcast Sink achieves this by offloading the scan to a Broadcast Assistant. This
 * is facilitated by the Broadcast Audio Scan Service (BASS). A BASS server is a GATT server that is
 * part of the Scan Delegator on a Broadcast Sink. A BASS client instead runs on the Broadcast
 * Assistant.
 *
 * <p>Once a GATT connection is established between the BASS client and the BASS server, the
 * Broadcast Sink can offload the scans to the Broadcast Assistant. Upon finding new Broadcast
 * Sources, the Broadcast Assistant then notifies the Broadcast Sink about these over the
 * established GATT connection. The Scan Delegator on the Broadcast Sink can also notify the
 * Assistant about changes such as addition and removal of Broadcast Sources.
 *
 * <p>In the context of this class, BASS server will be addressed as Broadcast Sink and BASS client
 * will be addressed as Broadcast Assistant.
 *
 * <p>BluetoothLeBroadcastAssistant is a proxy object for controlling the Broadcast Assistant
 * service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get the
 * BluetoothLeBroadcastAssistant proxy object.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeBroadcastAssistant implements BluetoothProfile, AutoCloseable {
    private static final String TAG = "BluetoothLeBroadcastAssistant";

    private static final boolean DBG = true;
    private final Map<Callback, Executor> mCallbackExecutorMap = new HashMap<>();

    private final IBluetoothLeBroadcastAssistantCallback mCallback =
            new IBluetoothLeBroadcastAssistantCallback.Stub() {
                @Override
                @RequiresNoPermission
                public void onSearchStarted(int reason) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSearchStarted(reason));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onSearchStartFailed(int reason) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSearchStartFailed(reason));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onSearchStopped(int reason) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSearchStopped(reason));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onSearchStopFailed(int reason) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSearchStopFailed(reason));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onSourceFound(BluetoothLeBroadcastMetadata source) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSourceFound(source));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onSourceAdded(BluetoothDevice sink, int sourceId, int reason) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSourceAdded(sink, sourceId, reason));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onSourceAddFailed(
                        BluetoothDevice sink, BluetoothLeBroadcastMetadata source, int reason) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSourceAddFailed(sink, source, reason));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onSourceModified(BluetoothDevice sink, int sourceId, int reason) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSourceModified(sink, sourceId, reason));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onSourceModifyFailed(BluetoothDevice sink, int sourceId, int reason) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(
                                () -> callback.onSourceModifyFailed(sink, sourceId, reason));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onSourceRemoved(BluetoothDevice sink, int sourceId, int reason) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSourceRemoved(sink, sourceId, reason));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onSourceRemoveFailed(BluetoothDevice sink, int sourceId, int reason) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(
                                () -> callback.onSourceRemoveFailed(sink, sourceId, reason));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onReceiveStateChanged(
                        BluetoothDevice sink,
                        int sourceId,
                        BluetoothLeBroadcastReceiveState state) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(
                                () -> callback.onReceiveStateChanged(sink, sourceId, state));
                    }
                }

                @Override
                @RequiresNoPermission
                public void onSourceLost(int broadcastId) {
                    for (Map.Entry<BluetoothLeBroadcastAssistant.Callback, Executor>
                            callbackExecutorEntry : mCallbackExecutorMap.entrySet()) {
                        BluetoothLeBroadcastAssistant.Callback callback =
                                callbackExecutorEntry.getKey();
                        Executor executor = callbackExecutorEntry.getValue();
                        executor.execute(() -> callback.onSourceLost(broadcastId));
                    }
                }
            };

    /**
     * This class provides a set of callbacks that are invoked when scanning for Broadcast Sources
     * is offloaded to a Broadcast Assistant.
     *
     * @hide
     */
    @SystemApi
    public interface Callback {
        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    BluetoothStatusCodes.ERROR_UNKNOWN,
                    BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST,
                    BluetoothStatusCodes.REASON_LOCAL_STACK_REQUEST,
                    BluetoothStatusCodes.REASON_REMOTE_REQUEST,
                    BluetoothStatusCodes.REASON_SYSTEM_POLICY,
                    BluetoothStatusCodes.ERROR_HARDWARE_GENERIC,
                    BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_DUPLICATE_ADDITION,
                    BluetoothStatusCodes.ERROR_BAD_PARAMETERS,
                    BluetoothStatusCodes.ERROR_REMOTE_LINK_ERROR,
                    BluetoothStatusCodes.ERROR_REMOTE_NOT_ENOUGH_RESOURCES,
                    BluetoothStatusCodes.ERROR_LE_BROADCAST_ASSISTANT_INVALID_SOURCE_ID,
                    BluetoothStatusCodes.ERROR_ALREADY_IN_TARGET_STATE,
                    BluetoothStatusCodes.ERROR_REMOTE_OPERATION_REJECTED,
                })
        @interface Reason {}

        /**
         * Callback invoked when the implementation started searching for nearby Broadcast Sources.
         *
         * @param reason reason code on why search has started
         * @hide
         */
        @SystemApi
        void onSearchStarted(@Reason int reason);

        /**
         * Callback invoked when the implementation failed to start searching for nearby broadcast
         * sources.
         *
         * @param reason reason for why search failed to start
         * @hide
         */
        @SystemApi
        void onSearchStartFailed(@Reason int reason);

        /**
         * Callback invoked when the implementation stopped searching for nearby Broadcast Sources.
         *
         * @param reason reason code on why search has stopped
         * @hide
         */
        @SystemApi
        void onSearchStopped(@Reason int reason);

        /**
         * Callback invoked when the implementation failed to stop searching for nearby broadcast
         * sources.
         *
         * @param reason for why search failed to start
         * @hide
         */
        @SystemApi
        void onSearchStopFailed(@Reason int reason);

        /**
         * Callback invoked when a new Broadcast Source is found together with the {@link
         * BluetoothLeBroadcastMetadata}.
         *
         * <p>Broadcast is found when it is available for user to synchronize with it.
         *
         * @param source {@link BluetoothLeBroadcastMetadata} representing a Broadcast Source
         * @hide
         */
        @SystemApi
        void onSourceFound(@NonNull BluetoothLeBroadcastMetadata source);

        /**
         * Callback invoked when a new Broadcast Source has been successfully added to the Broadcast
         * Sink.
         *
         * <p>Broadcast audio stream may not have been started after this callback, the caller need
         * to monitor {@link #onReceiveStateChanged(BluetoothDevice, int,
         * BluetoothLeBroadcastReceiveState)} to see if synchronization with Broadcast Source is
         * successful
         *
         * <p>When <var>isGroupOp</var> is true when {@link #addSource(BluetoothDevice,
         * BluetoothLeBroadcastMetadata, boolean)} is called, each Broadcast Sink device in the
         * coordinated set will trigger and individual update
         *
         * <p>A new source could be added by the Broadcast Sink itself or other Broadcast Assistants
         * connected to the Broadcast Sink and in this case the reason code will be {@link
         * BluetoothStatusCodes#REASON_REMOTE_REQUEST}
         *
         * @param sink Broadcast Sink device on which a new Broadcast Source has been added
         * @param sourceId source ID as defined in the BASS specification
         * @param reason reason of source addition
         * @hide
         */
        @SystemApi
        void onSourceAdded(@NonNull BluetoothDevice sink, @Reason int sourceId, @Reason int reason);

        /**
         * Callback invoked when the new Broadcast Source failed to be added to the Broadcast Sink.
         *
         * @param sink Broadcast Sink device on which a new Broadcast Source has been added
         * @param source metadata representation of the Broadcast Source
         * @param reason reason why the addition has failed
         * @hide
         */
        @SystemApi
        void onSourceAddFailed(
                @NonNull BluetoothDevice sink,
                @NonNull BluetoothLeBroadcastMetadata source,
                @Reason int reason);

        /**
         * Callback invoked when an existing Broadcast Source within a Broadcast Sink has been
         * modified.
         *
         * <p>Actual state after the modification will be delivered via the next {@link
         * Callback#onReceiveStateChanged(BluetoothDevice, int, BluetoothLeBroadcastReceiveState)}
         * callback.
         *
         * <p>A source could be modified by the Broadcast Sink itself or other Broadcast Assistants
         * connected to the Broadcast Sink and in this case the reason code will be {@link
         * BluetoothStatusCodes#REASON_REMOTE_REQUEST}
         *
         * @param sink Broadcast Sink device on which a Broadcast Source has been modified
         * @param sourceId source ID as defined in the BASS specification
         * @param reason reason of source modification
         * @hide
         */
        @SystemApi
        void onSourceModified(@NonNull BluetoothDevice sink, int sourceId, @Reason int reason);

        /**
         * Callback invoked when the Broadcast Assistant failed to modify an existing Broadcast
         * Source on a Broadcast Sink.
         *
         * @param sink Broadcast Sink device on which a Broadcast Source has been modified
         * @param sourceId source ID as defined in the BASS specification
         * @param reason reason why the modification has failed
         * @hide
         */
        @SystemApi
        void onSourceModifyFailed(@NonNull BluetoothDevice sink, int sourceId, @Reason int reason);

        /**
         * Callback invoked when a Broadcast Source has been successfully removed from the Broadcast
         * Sink.
         *
         * <p>No more update for the source ID via {@link
         * Callback#onReceiveStateChanged(BluetoothDevice, int, BluetoothLeBroadcastReceiveState)}
         * after this callback.
         *
         * <p>A source could be removed by the Broadcast Sink itself or other Broadcast Assistants
         * connected to the Broadcast Sink and in this case the reason code will be {@link
         * BluetoothStatusCodes#REASON_REMOTE_REQUEST}
         *
         * @param sink Broadcast Sink device from which a Broadcast Source has been removed
         * @param sourceId source ID as defined in the BASS specification
         * @param reason reason why the Broadcast Source was removed
         * @hide
         */
        @SystemApi
        void onSourceRemoved(@NonNull BluetoothDevice sink, int sourceId, @Reason int reason);

        /**
         * Callback invoked when the Broadcast Assistant failed to remove an existing Broadcast
         * Source on a Broadcast Sink.
         *
         * @param sink Broadcast Sink device on which a Broadcast Source was to be removed
         * @param sourceId source ID as defined in the BASS specification
         * @param reason reason why the modification has failed
         * @hide
         */
        @SystemApi
        void onSourceRemoveFailed(@NonNull BluetoothDevice sink, int sourceId, @Reason int reason);

        /**
         * Callback invoked when the Broadcast Receive State information of a Broadcast Sink device
         * changes.
         *
         * @param sink BASS server device that is also a Broadcast Sink device
         * @param sourceId source ID as defined in the BASS specification
         * @param state latest state information between the Broadcast Sink and a Broadcast Source
         * @hide
         */
        @SystemApi
        void onReceiveStateChanged(
                @NonNull BluetoothDevice sink,
                int sourceId,
                @NonNull BluetoothLeBroadcastReceiveState state);

        /**
         * Callback invoked when the Broadcast Source is lost together with source broadcast id.
         *
         * <p>Callback client can know that the source notified by {@link
         * Callback#onSourceFound(BluetoothLeBroadcastMetadata)} before is not available any more
         * after this callback.
         *
         * @param broadcastId broadcast ID as defined in the BASS specification
         * @hide
         */
        @SystemApi
        default void onSourceLost(int broadcastId) {}
    }

    /**
     * Intent used to broadcast the change in connection state of devices via Broadcast Audio Scan
     * Service (BASS). Please note that in a coordinated set, each set member will connect via BASS
     * individually. Group operations on a single set member will propagate to the entire set.
     *
     * <p>For example, in the binaural case, there will be two different LE devices for the left and
     * right side and each device will have their own connection state changes. If both devices
     * belongs to on Coordinated Set, group operation on one of them will affect both devices.
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
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.action.CONNECTION_STATE_CHANGED";

    private CloseGuard mCloseGuard;
    private final BluetoothAdapter mBluetoothAdapter;
    private final AttributionSource mAttributionSource;

    private IBluetoothLeBroadcastAssistant mService;

    /**
     * Create a new instance of an LE Audio Broadcast Assistant.
     *
     * @hide
     */
    /*package*/ BluetoothLeBroadcastAssistant(
            @NonNull Context context, @NonNull BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = bluetoothAdapter;
        mAttributionSource = bluetoothAdapter.getAttributionSource();
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
        mBluetoothAdapter.closeProfileProxy(this);
    }

    /** @hide */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void onServiceConnected(IBinder service) {
        mService = IBluetoothLeBroadcastAssistant.Stub.asInterface(service);
        // re-register the service-to-app callback
        log("onServiceConnected");
        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.isEmpty()) {
                return;
            }
            try {
                mService.registerCallback(mCallback, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(
                        TAG,
                        "onServiceConnected: Failed to register "
                                + "Le Broadcaster Assistant callback",
                        e);
            }
        }
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public void onServiceDisconnected() {
        mService = null;
    }

    private IBluetoothLeBroadcastAssistant getService() {
        return mService;
    }

    /** @hide */
    @Override
    @RequiresNoPermission
    public BluetoothAdapter getAdapter() {
        return mBluetoothAdapter;
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
    public @BluetoothProfile.BtProfileState int getConnectionState(@NonNull BluetoothDevice sink) {
        log("getConnectionState(" + sink + ")");
        requireNonNull(sink);
        final IBluetoothLeBroadcastAssistant service = getService();
        final int defaultValue = BluetoothProfile.STATE_DISCONNECTED;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled() && isValidDevice(sink)) {
            try {
                return service.getConnectionState(sink, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
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
        log("getDevicesMatchingConnectionStates()");
        requireNonNull(states);
        final IBluetoothLeBroadcastAssistant service = getService();
        final List<BluetoothDevice> defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled()) {
            try {
                return service.getDevicesMatchingConnectionStates(states, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
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
        log("getConnectedDevices()");
        final IBluetoothLeBroadcastAssistant service = getService();
        final List<BluetoothDevice> defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled()) {
            try {
                return service.getConnectedDevices(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Set connection policy of the profile.
     *
     * <p>The device should already be paired. Connection policy can be one of {@link
     * #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN}, {@link
     * #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     * @throws NullPointerException if <var>device</var> is null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setConnectionPolicy(
            @NonNull BluetoothDevice device, @ConnectionPolicy int connectionPolicy) {
        log("setConnectionPolicy()");
        requireNonNull(device);
        final IBluetoothLeBroadcastAssistant service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled()
                && isValidDevice(device)
                && (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                        || connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED)) {
            try {
                return service.setConnectionPolicy(device, connectionPolicy, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p>The connection policy can be any of: {@link #CONNECTION_POLICY_ALLOWED}, {@link
     * #CONNECTION_POLICY_FORBIDDEN}, {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     * @throws NullPointerException if <var>device</var> is null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @ConnectionPolicy int getConnectionPolicy(@NonNull BluetoothDevice device) {
        log("getConnectionPolicy()");
        requireNonNull(device);
        final IBluetoothLeBroadcastAssistant service = getService();
        final int defaultValue = BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionPolicy(device, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
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
     * @throws NullPointerException if a null executor, or callback is given
     * @throws IllegalArgumentException if the same <var>callback<var> is already registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void registerCallback(
            @NonNull @CallbackExecutor Executor executor, @NonNull Callback callback) {
        requireNonNull(executor);
        requireNonNull(callback);
        log("registerCallback");

        synchronized (mCallbackExecutorMap) {
            // If the callback map is empty, we register the service-to-app callback
            if (mCallbackExecutorMap.isEmpty()) {
                if (!mBluetoothAdapter.isEnabled()) {
                    /* If Bluetooth is off, just store callback and it will be registered
                     * when Bluetooth is on
                     */
                    mCallbackExecutorMap.put(callback, executor);
                    return;
                }
                try {
                    final IBluetoothLeBroadcastAssistant service = getService();
                    if (service != null) {
                        service.registerCallback(mCallback, mAttributionSource);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
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
     *
     * <p>The same {@link Callback} object used when calling {@link #registerCallback(Executor,
     * Callback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when the application process goes away.
     *
     * @param callback user implementation of the {@link Callback}
     * @throws NullPointerException when callback is null
     * @throws IllegalArgumentException when the <var>callback</var> was not registered before
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void unregisterCallback(@NonNull Callback callback) {
        requireNonNull(callback);
        log("unregisterCallback");

        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.remove(callback) == null) {
                throw new IllegalArgumentException("This callback has not been registered");
            }

            // If the callback map is empty, we unregister the service-to-app callback
            if (mCallbackExecutorMap.isEmpty()) {
                try {
                    final IBluetoothLeBroadcastAssistant service = getService();
                    if (service != null) {
                        service.unregisterCallback(mCallback, mAttributionSource);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
                }
            }
        }
    }

    /**
     * Search for LE Audio Broadcast Sources on behalf of all devices connected via Broadcast Audio
     * Scan Service, filtered by <var>filters</var>.
     *
     * <p>On success, {@link Callback#onSearchStarted(int)} will be called with reason code {@link
     * BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST}.
     *
     * <p>On failure, {@link Callback#onSearchStartFailed(int)} will be called with reason code
     *
     * <p>The implementation will also synchronize with discovered Broadcast Sources and get their
     * metadata before passing the Broadcast Source metadata back to the application using {@link
     * Callback#onSourceFound(BluetoothLeBroadcastMetadata)}.
     *
     * <p>Please disconnect the Broadcast Sink's BASS server by calling {@link
     * #setConnectionPolicy(BluetoothDevice, int)} with {@link
     * BluetoothProfile#CONNECTION_POLICY_FORBIDDEN} if you do not want the Broadcast Sink to
     * receive notifications about this search before calling this method.
     *
     * <p>App must also have {@link android.Manifest.permission#ACCESS_FINE_LOCATION
     * ACCESS_FINE_LOCATION} permission in order to get results.
     *
     * <p><var>filters</var> will be AND'ed with internal filters in the implementation and {@link
     * ScanSettings} will be managed by the implementation.
     *
     * @param filters {@link ScanFilter}s for finding exact Broadcast Source, if no filter is
     *     needed, please provide an empty list instead
     * @throws NullPointerException when <var>filters</var> argument is null
     * @throws IllegalStateException when no callback is registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresBluetoothLocationPermission
    @RequiresPermission(allOf = {BLUETOOTH_SCAN, BLUETOOTH_PRIVILEGED})
    public void startSearchingForSources(@NonNull List<ScanFilter> filters) {
        log("searchForBroadcastSources");
        requireNonNull(filters);
        if (mCallback == null) {
            throw new IllegalStateException("No callback was ever registered");
        }

        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.isEmpty()) {
                throw new IllegalStateException("All callbacks are unregistered");
            }
        }

        final IBluetoothLeBroadcastAssistant service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled()) {
            try {
                service.startSearchingForSources(filters, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Stops an ongoing search for nearby Broadcast Sources.
     *
     * <p>On success, {@link Callback#onSearchStopped(int)} will be called with reason code {@link
     * BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST}. On failure, {@link
     * Callback#onSearchStopFailed(int)} will be called with reason code
     *
     * @throws IllegalStateException if callback was not registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(allOf = {BLUETOOTH_SCAN, BLUETOOTH_PRIVILEGED})
    public void stopSearchingForSources() {
        log("stopSearchingForSources:");
        if (mCallback == null) {
            throw new IllegalStateException("No callback was ever registered");
        }

        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.isEmpty()) {
                throw new IllegalStateException("All callbacks are unregistered");
            }
        }

        final IBluetoothLeBroadcastAssistant service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled()) {
            try {
                service.stopSearchingForSources(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Return true if a search has been started by this application.
     *
     * @return true if a search has been started by this application
     * @hide
     */
    @SystemApi
    @RequiresBluetoothScanPermission
    @RequiresPermission(allOf = {BLUETOOTH_SCAN, BLUETOOTH_PRIVILEGED})
    public boolean isSearchInProgress() {
        log("isSearchInProgress:");
        final IBluetoothLeBroadcastAssistant service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled()) {
            try {
                return service.isSearchInProgress(mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Add a Broadcast Source to the Broadcast Sink.
     *
     * <p>Caller can modify <var>sourceMetadata</var> before using it in this method to set a
     * Broadcast Code, to select a different Broadcast Channel in a Broadcast Source such as channel
     * with a different language, and so on. What can be modified is listed in the documentation of
     * {@link #modifySource(BluetoothDevice, int, BluetoothLeBroadcastMetadata)} and can also be
     * modified after a source is added.
     *
     * <p>On success, {@link Callback#onSourceAdded(BluetoothDevice, int, int)} will be invoked with
     * a <var>sourceID</var> assigned by the Broadcast Sink with reason code {@link
     * BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST}. However, this callback only indicates that
     * the Broadcast Sink has allocated resource to receive audio from the Broadcast Source, and
     * audio stream may not have started. The caller should then wait for {@link
     * Callback#onReceiveStateChanged(BluetoothDevice, int, BluetoothLeBroadcastReceiveState)}
     * callback to monitor the encryption and audio sync state.
     *
     * <p>Note that wrong broadcast code will not prevent the source from being added to the
     * Broadcast Sink. Caller should modify the current source to correct the broadcast code.
     *
     * <p>On failure, {@link Callback#onSourceAddFailed(BluetoothDevice,
     * BluetoothLeBroadcastMetadata, int)} will be invoked with the same <var>source</var> metadata
     * and reason code
     *
     * <p>When too many sources was added to Broadcast sink, error {@link
     * BluetoothStatusCodes#ERROR_REMOTE_NOT_ENOUGH_RESOURCES} will be delivered. In this case,
     * check the capacity of Broadcast sink via {@link #getMaximumSourceCapacity(BluetoothDevice)}
     * and the current list of sources via {@link #getAllSources(BluetoothDevice)}.
     *
     * <p>Some sources might be added by other Broadcast Assistants and hence was not in {@link
     * Callback#onSourceAdded(BluetoothDevice, int, int)} callback, but will be updated via {@link
     * Callback#onReceiveStateChanged(BluetoothDevice, int, BluetoothLeBroadcastReceiveState)}
     *
     * <p>If there are multiple members in the coordinated set the sink belongs to, and isGroupOp is
     * set to true, the Broadcast Source will be added to each sink in the coordinated set and a
     * separate {@link Callback#onSourceAdded} callback will be invoked for each member of the
     * coordinated set.
     *
     * <p>The <var>isGroupOp</var> option is sticky. This means that subsequent operations using
     * {@link #modifySource(BluetoothDevice, int, BluetoothLeBroadcastMetadata)} and {@link
     * #removeSource(BluetoothDevice, int)} will act on all devices in the same coordinated set for
     * the <var>sink</var> and <var>sourceID</var> pair until the <var>sourceId</var> is removed
     * from the <var>sink</var> by any Broadcast role (could be another remote device).
     *
     * <p>When <var>isGroupOp</var> is true, if one Broadcast Sink in a coordinated set disconnects
     * from this Broadcast Assistant or lost the Broadcast Source, this Broadcast Assistant will try
     * to add it back automatically to make sure the whole coordinated set is in the same state.
     *
     * @param sink Broadcast Sink to which the Broadcast Source should be added
     * @param sourceMetadata Broadcast Source metadata to be added to the Broadcast Sink
     * @param isGroupOp {@code true} if Application wants to perform this operation for all
     *     coordinated set members throughout this session. Otherwise, caller would have to add,
     *     modify, and remove individual set members.
     * @throws NullPointerException if <var>sink</var> or <var>source</var> is null
     * @throws IllegalStateException if callback was not registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void addSource(
            @NonNull BluetoothDevice sink,
            @NonNull BluetoothLeBroadcastMetadata sourceMetadata,
            boolean isGroupOp) {
        log("addBroadcastSource: " + sourceMetadata + " on " + sink);
        requireNonNull(sink);
        requireNonNull(sourceMetadata);
        if (mCallback == null) {
            throw new IllegalStateException("No callback was ever registered");
        }

        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.isEmpty()) {
                throw new IllegalStateException("All callbacks are unregistered");
            }
        }

        final IBluetoothLeBroadcastAssistant service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled() && isValidDevice(sink)) {
            try {
                service.addSource(sink, sourceMetadata, isGroupOp, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Modify the Broadcast Source information on a Broadcast Sink.
     *
     * <p>One can modify {@link BluetoothLeBroadcastMetadata#getBroadcastCode()} if {@link
     * BluetoothLeBroadcastReceiveState#getBigEncryptionState()} returns {@link
     * BluetoothLeBroadcastReceiveState#BIG_ENCRYPTION_STATE_BAD_CODE} or {@link
     * BluetoothLeBroadcastReceiveState#BIG_ENCRYPTION_STATE_CODE_REQUIRED}
     *
     * <p>One can modify {@link BluetoothLeBroadcastMetadata#getPaSyncInterval()} if the Broadcast
     * Assistant received updated information.
     *
     * <p>One can modify {@link BluetoothLeBroadcastChannel#isSelected()} to select different
     * broadcast channel to listen to (one per {@link BluetoothLeBroadcastSubgroup} or set {@link
     * BluetoothLeBroadcastSubgroup#isNoChannelPreference()} to leave the choice to the Broadcast
     * Sink.
     *
     * <p>One can modify {@link BluetoothLeBroadcastSubgroup#getContentMetadata()} if the subgroup
     * metadata changes and the Broadcast Sink need help updating the metadata from Broadcast
     * Assistant.
     *
     * <p>Each of the above modifications can be accepted or rejected by the Broadcast Assistant
     * implement and/or the Broadcast Sink.
     *
     * <p>On success, {@link Callback#onSourceModified(BluetoothDevice, int, int)} will be invoked
     * with reason code {@link BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST}.
     *
     * <p>On failure, {@link Callback#onSourceModifyFailed(BluetoothDevice, int, int)} will be
     * invoked with reason code.
     *
     * <p>If there are multiple members in the coordinated set the sink belongs to, and isGroupOp is
     * set to true during {@link #addSource(BluetoothDevice, BluetoothLeBroadcastMetadata,
     * boolean)}, the source will be modified on each sink in the coordinated set and a separate
     * {@link Callback#onSourceModified(BluetoothDevice, int, int)} callback will be invoked for
     * each member of the coordinated set.
     *
     * @param sink Broadcast Sink to which the Broadcast Source should be updated
     * @param sourceId source ID as delivered in {@link Callback#onSourceAdded(BluetoothDevice, int,
     *     int)}
     * @param updatedMetadata updated Broadcast Source metadata to be updated on the Broadcast Sink
     * @throws IllegalStateException if callback was not registered
     * @throws NullPointerException if <var>sink</var> or <var>updatedMetadata</var> is null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void modifySource(
            @NonNull BluetoothDevice sink,
            int sourceId,
            @NonNull BluetoothLeBroadcastMetadata updatedMetadata) {
        log("updateBroadcastSource: " + updatedMetadata + " on " + sink);
        requireNonNull(sink);
        requireNonNull(updatedMetadata);
        if (mCallback == null) {
            throw new IllegalStateException("No callback was ever registered");
        }

        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.isEmpty()) {
                throw new IllegalStateException("All callbacks are unregistered");
            }
        }

        final IBluetoothLeBroadcastAssistant service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled() && isValidDevice(sink)) {
            try {
                service.modifySource(sink, sourceId, updatedMetadata, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Removes the Broadcast Source from a Broadcast Sink.
     *
     * <p>On success, {@link Callback#onSourceRemoved(BluetoothDevice, int, int)} will be invoked
     * with reason code {@link BluetoothStatusCodes#REASON_LOCAL_APP_REQUEST}.
     *
     * <p>On failure, {@link Callback#onSourceRemoveFailed(BluetoothDevice, int, int)} will be
     * invoked with reason code.
     *
     * <p>If there are multiple members in the coordinated set the sink belongs to, and isGroupOp is
     * set to true during {@link #addSource(BluetoothDevice, BluetoothLeBroadcastMetadata,
     * boolean)}, the source will be removed from each sink in the coordinated set and a separate
     * {@link Callback#onSourceRemoved(BluetoothDevice, int, int)} callback will be invoked for each
     * member of the coordinated set.
     *
     * @param sink Broadcast Sink from which a Broadcast Source should be removed
     * @param sourceId source ID as delivered in {@link Callback#onSourceAdded(BluetoothDevice, int,
     *     int)}
     * @throws NullPointerException when the <var>sink</var> is null
     * @throws IllegalStateException if callback was not registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void removeSource(@NonNull BluetoothDevice sink, int sourceId) {
        log("removeBroadcastSource: " + sourceId + " from " + sink);
        requireNonNull(sink);
        if (mCallback == null) {
            throw new IllegalStateException("No callback was ever registered");
        }

        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.isEmpty()) {
                throw new IllegalStateException("All callbacks are unregistered");
            }
        }

        final IBluetoothLeBroadcastAssistant service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled() && isValidDevice(sink)) {
            try {
                service.removeSource(sink, sourceId, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    /**
     * Get information about all Broadcast Sources that a Broadcast Sink knows about.
     *
     * @param sink Broadcast Sink from which to get all Broadcast Sources
     * @return the list of Broadcast Receive State {@link BluetoothLeBroadcastReceiveState} stored
     *     in the Broadcast Sink
     * @throws NullPointerException when <var>sink</var> is null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @NonNull
    public List<BluetoothLeBroadcastReceiveState> getAllSources(@NonNull BluetoothDevice sink) {
        log("getAllSources()");
        requireNonNull(sink);
        final IBluetoothLeBroadcastAssistant service = getService();
        final List<BluetoothLeBroadcastReceiveState> defaultValue =
                new ArrayList<BluetoothLeBroadcastReceiveState>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled()) {
            try {
                return service.getAllSources(sink, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get maximum number of sources that can be added to this Broadcast Sink.
     *
     * @param sink Broadcast Sink device
     * @return maximum number of sources that can be added to this Broadcast Sink
     * @throws NullPointerException when <var>sink</var> is null
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public int getMaximumSourceCapacity(@NonNull BluetoothDevice sink) {
        requireNonNull(sink);
        final IBluetoothLeBroadcastAssistant service = getService();
        final int defaultValue = 0;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled() && isValidDevice(sink)) {
            try {
                return service.getMaximumSourceCapacity(sink, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Gets the {@link BluetoothLeBroadcastMetadata} of a specified source added to this Broadcast
     * Sink.
     *
     * <p>This method retrieves the {@link BluetoothLeBroadcastMetadata} associated with the
     * specified source. The metadata is obtained from the {@link BluetoothLeBroadcastReceiveState}
     * maintained by this Broadcast Sink. If no matching metadata is found, this method returns
     * {@code null}.
     *
     * <p>The source's {@link BluetoothLeBroadcastMetadata} is initially set by {@link
     * #addSource(BluetoothDevice, BluetoothLeBroadcastMetadata, boolean)} and can be updated with
     * {@link #modifySource(BluetoothDevice, int, BluetoothLeBroadcastMetadata)}.
     *
     * @param sink Broadcast Sink device
     * @param sourceId Broadcast source id. Valid range is [0, 0xFF] as defined in the Broadcast
     *     Audio Scan Service 1.0 specification (section 3.2).
     * @return metadata {@link BluetoothLeBroadcastMetadata} associated with the specified source.
     * @throws IllegalArgumentException if sourceID is not [0, 0xFF].
     * @hide
     */
    @FlaggedApi(Flags.FLAG_LEAUDIO_BROADCAST_API_GET_LOCAL_METADATA)
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @Nullable BluetoothLeBroadcastMetadata getSourceMetadata(
            @NonNull BluetoothDevice sink, @IntRange(from = 0x00, to = 0xFF) int sourceId) {
        log("getSourceMetadata()");
        requireNonNull(sink);
        if (sourceId < 0x00 || sourceId > 0xFF) {
            throw new IllegalArgumentException(
                    "sourceId " + sourceId + " does not fall between 0x00 and 0xFF");
        }
        final IBluetoothLeBroadcastAssistant service = getService();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mBluetoothAdapter.isEnabled()) {
            try {
                return service.getSourceMetadata(sink, sourceId, mAttributionSource);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return null;
    }

    private static void log(@NonNull String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }

    private static boolean isValidDevice(@Nullable BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }
}

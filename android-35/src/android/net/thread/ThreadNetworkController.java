/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.net.thread;

import static java.util.Objects.requireNonNull;

import android.Manifest.permission;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.Size;
import android.annotation.SystemApi;
import android.os.Binder;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.util.SparseIntArray;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Provides the primary APIs for controlling all aspects of a Thread network.
 *
 * <p>For example, join this device to a Thread network with given Thread Operational Dataset, or
 * migrate an existing network.
 *
 * @hide
 */
@FlaggedApi(ThreadNetworkFlags.FLAG_THREAD_ENABLED)
@SystemApi
public final class ThreadNetworkController {
    private static final String TAG = "ThreadNetworkController";

    /** The Thread stack is stopped. */
    public static final int DEVICE_ROLE_STOPPED = 0;

    /** The device is not currently participating in a Thread network/partition. */
    public static final int DEVICE_ROLE_DETACHED = 1;

    /** The device is a Thread Child. */
    public static final int DEVICE_ROLE_CHILD = 2;

    /** The device is a Thread Router. */
    public static final int DEVICE_ROLE_ROUTER = 3;

    /** The device is a Thread Leader. */
    public static final int DEVICE_ROLE_LEADER = 4;

    /** The Thread radio is disabled. */
    public static final int STATE_DISABLED = 0;

    /** The Thread radio is enabled. */
    public static final int STATE_ENABLED = 1;

    /** The Thread radio is being disabled. */
    public static final int STATE_DISABLING = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        DEVICE_ROLE_STOPPED,
        DEVICE_ROLE_DETACHED,
        DEVICE_ROLE_CHILD,
        DEVICE_ROLE_ROUTER,
        DEVICE_ROLE_LEADER
    })
    public @interface DeviceRole {}

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"STATE_"},
            value = {STATE_DISABLED, STATE_ENABLED, STATE_DISABLING})
    public @interface EnabledState {}

    /** Thread standard version 1.3. */
    public static final int THREAD_VERSION_1_3 = 4;

    /** Minimum value of max power in unit of 0.01dBm. @hide */
    private static final int POWER_LIMITATION_MIN = -32768;

    /** Maximum value of max power in unit of 0.01dBm. @hide */
    private static final int POWER_LIMITATION_MAX = 32767;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({THREAD_VERSION_1_3})
    public @interface ThreadVersion {}

    private final IThreadNetworkController mControllerService;

    private final Object mStateCallbackMapLock = new Object();

    @GuardedBy("mStateCallbackMapLock")
    private final Map<StateCallback, StateCallbackProxy> mStateCallbackMap = new HashMap<>();

    private final Object mOpDatasetCallbackMapLock = new Object();

    @GuardedBy("mOpDatasetCallbackMapLock")
    private final Map<OperationalDatasetCallback, OperationalDatasetCallbackProxy>
            mOpDatasetCallbackMap = new HashMap<>();

    /** @hide */
    public ThreadNetworkController(@NonNull IThreadNetworkController controllerService) {
        requireNonNull(controllerService, "controllerService cannot be null");
        mControllerService = controllerService;
    }

    /**
     * Enables/Disables the radio of this ThreadNetworkController. The requested enabled state will
     * be persistent and survives device reboots.
     *
     * <p>When Thread is in {@code STATE_DISABLED}, {@link ThreadNetworkController} APIs which
     * require the Thread radio will fail with error code {@link
     * ThreadNetworkException#ERROR_THREAD_DISABLED}. When Thread is in {@code STATE_DISABLING},
     * {@link ThreadNetworkController} APIs that return a {@link ThreadNetworkException} will fail
     * with error code {@link ThreadNetworkException#ERROR_BUSY}.
     *
     * <p>On success, {@link OutcomeReceiver#onResult} of {@code receiver} is called. It indicates
     * the operation has completed. But there maybe subsequent calls to update the enabled state,
     * callers of this method should use {@link #registerStateCallback} to subscribe to the Thread
     * enabled state changes.
     *
     * <p>On failure, {@link OutcomeReceiver#onError} of {@code receiver} will be invoked with a
     * specific error in {@link ThreadNetworkException#ERROR_}.
     *
     * @param enabled {@code true} for enabling Thread
     * @param executor the executor to execute {@code receiver}
     * @param receiver the receiver to receive result of this operation
     */
    @RequiresPermission("android.permission.THREAD_NETWORK_PRIVILEGED")
    public void setEnabled(
            boolean enabled,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, ThreadNetworkException> receiver) {
        try {
            mControllerService.setEnabled(enabled, new OperationReceiverProxy(executor, receiver));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns the Thread version this device is operating on. */
    @ThreadVersion
    public int getThreadVersion() {
        try {
            return mControllerService.getThreadVersion();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Creates a new Active Operational Dataset with randomized parameters.
     *
     * <p>This method is the recommended way to create a randomized dataset which can be used with
     * {@link #join} to securely join this device to the specified network . It's highly discouraged
     * to change the randomly generated Extended PAN ID, Network Key or PSKc, as it will compromise
     * the security of a Thread network.
     *
     * @throws IllegalArgumentException if length of the UTF-8 representation of {@code networkName}
     *     isn't in range of [{@link #LENGTH_MIN_NETWORK_NAME_BYTES}, {@link
     *     #LENGTH_MAX_NETWORK_NAME_BYTES}]
     */
    public void createRandomizedDataset(
            @NonNull String networkName,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<ActiveOperationalDataset, ThreadNetworkException> receiver) {
        ActiveOperationalDataset.checkNetworkName(networkName);
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(receiver, "receiver cannot be null");
        try {
            mControllerService.createRandomizedDataset(
                    networkName, new ActiveDatasetReceiverProxy(executor, receiver));
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /** Returns {@code true} if {@code deviceRole} indicates an attached state. */
    public static boolean isAttached(@DeviceRole int deviceRole) {
        return deviceRole == DEVICE_ROLE_CHILD
                || deviceRole == DEVICE_ROLE_ROUTER
                || deviceRole == DEVICE_ROLE_LEADER;
    }

    /**
     * Callback to receive notifications when the Thread network states are changed.
     *
     * <p>Applications which are interested in monitoring Thread network states should implement
     * this interface and register the callback with {@link #registerStateCallback}.
     */
    public interface StateCallback {
        /**
         * The Thread device role has changed.
         *
         * @param deviceRole the new Thread device role
         */
        void onDeviceRoleChanged(@DeviceRole int deviceRole);

        /**
         * The Thread network partition ID has changed.
         *
         * @param partitionId the new Thread partition ID
         */
        default void onPartitionIdChanged(long partitionId) {}

        /**
         * The Thread enabled state has changed.
         *
         * <p>The Thread enabled state can be set with {@link setEnabled}, it may also be updated by
         * airplane mode or admin control.
         *
         * @param enabledState the new Thread enabled state
         */
        default void onThreadEnableStateChanged(@EnabledState int enabledState) {}
    }

    private static final class StateCallbackProxy extends IStateCallback.Stub {
        private final Executor mExecutor;
        private final StateCallback mCallback;

        StateCallbackProxy(@CallbackExecutor Executor executor, StateCallback callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onDeviceRoleChanged(@DeviceRole int deviceRole) {
            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(() -> mCallback.onDeviceRoleChanged(deviceRole));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onPartitionIdChanged(long partitionId) {
            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(() -> mCallback.onPartitionIdChanged(partitionId));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onThreadEnableStateChanged(@EnabledState int enabled) {
            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(() -> mCallback.onThreadEnableStateChanged(enabled));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /**
     * Registers a callback to be called when Thread network states are changed.
     *
     * <p>Upon return of this method, methods of {@code callback} will be invoked immediately with
     * existing states.
     *
     * @param executor the executor to execute the {@code callback}
     * @param callback the callback to receive Thread network state changes
     * @throws IllegalArgumentException if {@code callback} has already been registered
     */
    @RequiresPermission(permission.ACCESS_NETWORK_STATE)
    public void registerStateCallback(
            @NonNull @CallbackExecutor Executor executor, @NonNull StateCallback callback) {
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(callback, "callback cannot be null");
        synchronized (mStateCallbackMapLock) {
            if (mStateCallbackMap.containsKey(callback)) {
                throw new IllegalArgumentException("callback has already been registered");
            }
            StateCallbackProxy callbackProxy = new StateCallbackProxy(executor, callback);
            mStateCallbackMap.put(callback, callbackProxy);

            try {
                mControllerService.registerStateCallback(callbackProxy);
            } catch (RemoteException e) {
                mStateCallbackMap.remove(callback);
                e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Unregisters the Thread state changed callback.
     *
     * @param callback the callback which has been registered with {@link #registerStateCallback}
     * @throws IllegalArgumentException if {@code callback} hasn't been registered
     */
    @RequiresPermission(permission.ACCESS_NETWORK_STATE)
    public void unregisterStateCallback(@NonNull StateCallback callback) {
        requireNonNull(callback, "callback cannot be null");
        synchronized (mStateCallbackMapLock) {
            StateCallbackProxy callbackProxy = mStateCallbackMap.get(callback);
            if (callbackProxy == null) {
                throw new IllegalArgumentException("callback hasn't been registered");
            }
            try {
                mControllerService.unregisterStateCallback(callbackProxy);
                mStateCallbackMap.remove(callback);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Callback to receive notifications when the Thread Operational Datasets are changed.
     *
     * <p>Applications which are interested in monitoring Thread network datasets should implement
     * this interface and register the callback with {@link #registerOperationalDatasetCallback}.
     */
    public interface OperationalDatasetCallback {
        /**
         * Called when the Active Operational Dataset is changed.
         *
         * @param activeDataset the new Active Operational Dataset or {@code null} if the dataset is
         *     absent
         */
        void onActiveOperationalDatasetChanged(@Nullable ActiveOperationalDataset activeDataset);

        /**
         * Called when the Pending Operational Dataset is changed.
         *
         * @param pendingDataset the new Pending Operational Dataset or {@code null} if the dataset
         *     has been committed and removed
         */
        default void onPendingOperationalDatasetChanged(
                @Nullable PendingOperationalDataset pendingDataset) {}
    }

    private static final class OperationalDatasetCallbackProxy
            extends IOperationalDatasetCallback.Stub {
        private final Executor mExecutor;
        private final OperationalDatasetCallback mCallback;

        OperationalDatasetCallbackProxy(
                @CallbackExecutor Executor executor, OperationalDatasetCallback callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onActiveOperationalDatasetChanged(
                @Nullable ActiveOperationalDataset activeDataset) {
            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(() -> mCallback.onActiveOperationalDatasetChanged(activeDataset));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onPendingOperationalDatasetChanged(
                @Nullable PendingOperationalDataset pendingDataset) {
            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(
                        () -> mCallback.onPendingOperationalDatasetChanged(pendingDataset));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /**
     * Registers a callback to be called when Thread Operational Datasets are changed.
     *
     * <p>Upon return of this method, methods of {@code callback} will be invoked immediately with
     * existing Operational Datasets.
     *
     * @param executor the executor to execute {@code callback}
     * @param callback the callback to receive Operational Dataset changes
     * @throws IllegalArgumentException if {@code callback} has already been registered
     */
    @RequiresPermission(
            allOf = {
                permission.ACCESS_NETWORK_STATE,
                "android.permission.THREAD_NETWORK_PRIVILEGED"
            })
    public void registerOperationalDatasetCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OperationalDatasetCallback callback) {
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(callback, "callback cannot be null");
        synchronized (mOpDatasetCallbackMapLock) {
            if (mOpDatasetCallbackMap.containsKey(callback)) {
                throw new IllegalArgumentException("callback has already been registered");
            }
            OperationalDatasetCallbackProxy callbackProxy =
                    new OperationalDatasetCallbackProxy(executor, callback);
            mOpDatasetCallbackMap.put(callback, callbackProxy);

            try {
                mControllerService.registerOperationalDatasetCallback(callbackProxy);
            } catch (RemoteException e) {
                mOpDatasetCallbackMap.remove(callback);
                e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Unregisters the Thread Operational Dataset callback.
     *
     * @param callback the callback which has been registered with {@link
     *     #registerOperationalDatasetCallback}
     * @throws IllegalArgumentException if {@code callback} hasn't been registered
     */
    @RequiresPermission(
            allOf = {
                permission.ACCESS_NETWORK_STATE,
                "android.permission.THREAD_NETWORK_PRIVILEGED"
            })
    public void unregisterOperationalDatasetCallback(@NonNull OperationalDatasetCallback callback) {
        requireNonNull(callback, "callback cannot be null");
        synchronized (mOpDatasetCallbackMapLock) {
            OperationalDatasetCallbackProxy callbackProxy = mOpDatasetCallbackMap.get(callback);
            if (callbackProxy == null) {
                throw new IllegalArgumentException("callback hasn't been registered");
            }
            try {
                mControllerService.unregisterOperationalDatasetCallback(callbackProxy);
                mOpDatasetCallbackMap.remove(callback);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Joins to a Thread network with given Active Operational Dataset.
     *
     * <p>This method does nothing if this device has already joined to the same network specified
     * by {@code activeDataset}. If this device has already joined to a different network, this
     * device will first leave from that network and then join the new network. This method changes
     * only this device and all other connected devices will stay in the old network. To change the
     * network for all connected devices together, use {@link #scheduleMigration}.
     *
     * <p>On success, {@link OutcomeReceiver#onResult} of {@code receiver} is called and the Dataset
     * will be persisted on this device; this device will try to attach to the Thread network and
     * the state changes can be observed by {@link #registerStateCallback}. On failure, {@link
     * OutcomeReceiver#onError} of {@code receiver} will be invoked with a specific error:
     *
     * <ul>
     *   <li>{@link ThreadNetworkException#ERROR_UNSUPPORTED_CHANNEL} {@code activeDataset}
     *       specifies a channel which is not supported in the current country or region; the {@code
     *       activeDataset} is rejected and not persisted so this device won't auto re-join the next
     *       time
     *   <li>{@link ThreadNetworkException#ERROR_ABORTED} this operation is aborted by another
     *       {@code join} or {@code leave} operation
     * </ul>
     *
     * @param activeDataset the Active Operational Dataset represents the Thread network to join
     * @param executor the executor to execute {@code receiver}
     * @param receiver the receiver to receive result of this operation
     */
    @RequiresPermission("android.permission.THREAD_NETWORK_PRIVILEGED")
    public void join(
            @NonNull ActiveOperationalDataset activeDataset,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, ThreadNetworkException> receiver) {
        requireNonNull(activeDataset, "activeDataset cannot be null");
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(receiver, "receiver cannot be null");
        try {
            mControllerService.join(activeDataset, new OperationReceiverProxy(executor, receiver));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Schedules a network migration which moves all devices in the current connected network to a
     * new network or updates parameters of the current connected network.
     *
     * <p>The migration doesn't happen immediately but is registered to the Leader device so that
     * all devices in the current Thread network can be scheduled to apply the new dataset together.
     *
     * <p>On success, the Pending Dataset is successfully registered and persisted on the Leader and
     * {@link OutcomeReceiver#onResult} of {@code receiver} will be called; Operational Dataset
     * changes will be asynchronously delivered via {@link OperationalDatasetCallback} if a callback
     * has been registered with {@link #registerOperationalDatasetCallback}. When failed, {@link
     * OutcomeReceiver#onError} will be called with a specific error:
     *
     * <ul>
     *   <li>{@link ThreadNetworkException#ERROR_FAILED_PRECONDITION} the migration is rejected
     *       because this device is not attached
     *   <li>{@link ThreadNetworkException#ERROR_UNSUPPORTED_CHANNEL} {@code pendingDataset}
     *       specifies a channel which is not supported in the current country or region; the {@code
     *       pendingDataset} is rejected and not persisted
     *   <li>{@link ThreadNetworkException#ERROR_REJECTED_BY_PEER} the Pending Dataset is rejected
     *       by the Leader device
     *   <li>{@link ThreadNetworkException#ERROR_BUSY} another {@code scheduleMigration} request is
     *       being processed
     *   <li>{@link ThreadNetworkException#ERROR_TIMEOUT} response from the Leader device hasn't
     *       been received before deadline
     * </ul>
     *
     * <p>The Delay Timer of {@code pendingDataset} can vary from several minutes to a few days.
     * It's important to select a proper value to safely migrate all devices in the network without
     * leaving sleepy end devices orphaned. Apps are not suggested to specify the Delay Timer value
     * if it's unclear how long it can take to propagate the {@code pendingDataset} to the whole
     * network. Instead, use {@link Duration#ZERO} to use the default value suggested by the system.
     *
     * @param pendingDataset the Pending Operational Dataset
     * @param executor the executor to execute {@code receiver}
     * @param receiver the receiver to receive result of this operation
     */
    @RequiresPermission("android.permission.THREAD_NETWORK_PRIVILEGED")
    public void scheduleMigration(
            @NonNull PendingOperationalDataset pendingDataset,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, ThreadNetworkException> receiver) {
        requireNonNull(pendingDataset, "pendingDataset cannot be null");
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(receiver, "receiver cannot be null");
        try {
            mControllerService.scheduleMigration(
                    pendingDataset, new OperationReceiverProxy(executor, receiver));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Leaves from the Thread network.
     *
     * <p>This undoes a {@link join} operation. On success, this device is disconnected from the
     * joined network and will not automatically join a network before {@link #join} is called
     * again. Active and Pending Operational Dataset configured and persisted on this device will be
     * removed too.
     *
     * @param executor the executor to execute {@code receiver}
     * @param receiver the receiver to receive result of this operation
     */
    @RequiresPermission("android.permission.THREAD_NETWORK_PRIVILEGED")
    public void leave(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, ThreadNetworkException> receiver) {
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(receiver, "receiver cannot be null");
        try {
            mControllerService.leave(new OperationReceiverProxy(executor, receiver));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets to use a specified test network as the upstream.
     *
     * @param testNetworkInterfaceName The name of the test network interface. When it's null,
     *     forbids using test network as an upstream.
     * @param executor the executor to execute {@code receiver}
     * @param receiver the receiver to receive result of this operation
     * @hide
     */
    @VisibleForTesting
    @RequiresPermission(
            allOf = {"android.permission.THREAD_NETWORK_PRIVILEGED", permission.NETWORK_SETTINGS})
    public void setTestNetworkAsUpstream(
            @Nullable String testNetworkInterfaceName,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, ThreadNetworkException> receiver) {
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(receiver, "receiver cannot be null");
        try {
            mControllerService.setTestNetworkAsUpstream(
                    testNetworkInterfaceName, new OperationReceiverProxy(executor, receiver));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets max power of each channel.
     *
     * <p>If not set, the default max power is set by the Thread HAL service or the Thread radio
     * chip firmware.
     *
     * <p>On success, the Pending Dataset is successfully registered and persisted on the Leader and
     * {@link OutcomeReceiver#onResult} of {@code receiver} will be called; When failed, {@link
     * OutcomeReceiver#onError} will be called with a specific error:
     *
     * <ul>
     *   <li>{@link ThreadNetworkException#ERROR_UNSUPPORTED_OPERATION} the operation is no
     *       supported by the platform.
     * </ul>
     *
     * @param channelMaxPowers SparseIntArray (key: channel, value: max power) consists of channel
     *     and corresponding max power. Valid channel values should be between {@link
     *     ActiveOperationalDataset#CHANNEL_MIN_24_GHZ} and {@link
     *     ActiveOperationalDataset#CHANNEL_MAX_24_GHZ}. The unit of the max power is 0.01dBm. Max
     *     power values should be between INT16_MIN (-32768) and INT16_MAX (32767). If the max power
     *     is set to INT16_MAX, the corresponding channel is not supported.
     * @param executor the executor to execute {@code receiver}.
     * @param receiver the receiver to receive the result of this operation.
     * @throws IllegalArgumentException if the size of {@code channelMaxPowers} is smaller than 1,
     *     or invalid channel or max power is configured.
     * @hide
     */
    @RequiresPermission("android.permission.THREAD_NETWORK_PRIVILEGED")
    public final void setChannelMaxPowers(
            @NonNull @Size(min = 1) SparseIntArray channelMaxPowers,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, ThreadNetworkException> receiver) {
        requireNonNull(channelMaxPowers, "channelMaxPowers cannot be null");
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(receiver, "receiver cannot be null");

        if (channelMaxPowers.size() < 1) {
            throw new IllegalArgumentException("channelMaxPowers cannot be empty");
        }

        for (int i = 0; i < channelMaxPowers.size(); i++) {
            int channel = channelMaxPowers.keyAt(i);
            int maxPower = channelMaxPowers.get(channel);

            if ((channel < ActiveOperationalDataset.CHANNEL_MIN_24_GHZ)
                    || (channel > ActiveOperationalDataset.CHANNEL_MAX_24_GHZ)) {
                throw new IllegalArgumentException(
                        "Channel "
                                + channel
                                + " exceeds allowed range ["
                                + ActiveOperationalDataset.CHANNEL_MIN_24_GHZ
                                + ", "
                                + ActiveOperationalDataset.CHANNEL_MAX_24_GHZ
                                + "]");
            }

            if ((maxPower < POWER_LIMITATION_MIN) || (maxPower > POWER_LIMITATION_MAX)) {
                throw new IllegalArgumentException(
                        "Channel power ({channel: "
                                + channel
                                + ", maxPower: "
                                + maxPower
                                + "}) exceeds allowed range ["
                                + POWER_LIMITATION_MIN
                                + ", "
                                + POWER_LIMITATION_MAX
                                + "]");
            }
        }

        try {
            mControllerService.setChannelMaxPowers(
                    toChannelMaxPowerArray(channelMaxPowers),
                    new OperationReceiverProxy(executor, receiver));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static ChannelMaxPower[] toChannelMaxPowerArray(
            @NonNull SparseIntArray channelMaxPowers) {
        final ChannelMaxPower[] powerArray = new ChannelMaxPower[channelMaxPowers.size()];

        for (int i = 0; i < channelMaxPowers.size(); i++) {
            powerArray[i] = new ChannelMaxPower();
            powerArray[i].channel = channelMaxPowers.keyAt(i);
            powerArray[i].maxPower = channelMaxPowers.get(powerArray[i].channel);
        }

        return powerArray;
    }

    private static <T> void propagateError(
            Executor executor,
            OutcomeReceiver<T, ThreadNetworkException> receiver,
            int errorCode,
            String errorMsg) {
        final long identity = Binder.clearCallingIdentity();
        try {
            executor.execute(
                    () -> receiver.onError(new ThreadNetworkException(errorCode, errorMsg)));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static final class ActiveDatasetReceiverProxy
            extends IActiveOperationalDatasetReceiver.Stub {
        final Executor mExecutor;
        final OutcomeReceiver<ActiveOperationalDataset, ThreadNetworkException> mResultReceiver;

        ActiveDatasetReceiverProxy(
                @CallbackExecutor Executor executor,
                OutcomeReceiver<ActiveOperationalDataset, ThreadNetworkException> resultReceiver) {
            this.mExecutor = executor;
            this.mResultReceiver = resultReceiver;
        }

        @Override
        public void onSuccess(ActiveOperationalDataset dataset) {
            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(() -> mResultReceiver.onResult(dataset));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onError(int errorCode, String errorMessage) {
            propagateError(mExecutor, mResultReceiver, errorCode, errorMessage);
        }
    }

    private static final class OperationReceiverProxy extends IOperationReceiver.Stub {
        final Executor mExecutor;
        final OutcomeReceiver<Void, ThreadNetworkException> mResultReceiver;

        OperationReceiverProxy(
                @CallbackExecutor Executor executor,
                OutcomeReceiver<Void, ThreadNetworkException> resultReceiver) {
            this.mExecutor = executor;
            this.mResultReceiver = resultReceiver;
        }

        @Override
        public void onSuccess() {
            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(() -> mResultReceiver.onResult(null));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onError(int errorCode, String errorMessage) {
            propagateError(mExecutor, mResultReceiver, errorCode, errorMessage);
        }
    }
}

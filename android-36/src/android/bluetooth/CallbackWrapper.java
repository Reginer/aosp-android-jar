/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * This class provide a common abstraction to deal with callback registration and broadcast from the
 * bluetooth to external app using their executor. It is designed to be thread safe. It register the
 * client the first time a callback is registered, and unregister when the last callback is
 * unregistered. It handles when the service to connect is not yet available and provide a method to
 * register the callback to the newly available service. @see #registerToNewService
 *
 * @param <S> the type of binder used to register to the bluetooth app (e.g: IBluetoothHapClient)
 * @param <T> the type of the callback (e.g: BluetoothHapClient.Callback)
 * @hide
 */
public class CallbackWrapper<T, S> {
    @GuardedBy("mCallbackExecutorMap")
    private final Map<T, Executor> mCallbackExecutorMap;

    private final Consumer<S> mRegisterConsumer;
    private final Consumer<S> mUnregisterConsumer;

    /**
     * @param registerConsumer is called the first time a callback is being registered or manually
     *     via @see #registerToNewService
     * @param unregisterConsumer is called when the last callback is removed
     */
    CallbackWrapper(Consumer<S> registerConsumer, Consumer<S> unregisterConsumer) {
        this(registerConsumer, unregisterConsumer, new HashMap());
    }

    /**
     * @param map internal map injected for test purpose
     * @see #CallbackWrapper(Consumer, Consumer)
     */
    @VisibleForTesting
    public CallbackWrapper(
            Consumer<S> registerConsumer, Consumer<S> unregisterConsumer, Map<T, Executor> map) {
        mRegisterConsumer = requireNonNull(registerConsumer);
        mUnregisterConsumer = requireNonNull(unregisterConsumer);
        mCallbackExecutorMap = requireNonNull(map);
    }

    /** Dispatch the callback from the Bluetooth service to all the currently registered callback */
    public void forEach(Consumer<T> consumer) {
        synchronized (mCallbackExecutorMap) {
            mCallbackExecutorMap.forEach(
                    (callback, executor) ->
                            executeFromBinder(executor, () -> consumer.accept(callback)));
        }
    }

    /** Register the callback and save the wrapper to the service if needed */
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @SuppressLint("AndroidFrameworkRequiresPermission") // Consumer wrongly report permission
    public void registerCallback(
            @Nullable S service,
            @NonNull T callback,
            @NonNull @CallbackExecutor Executor executor) {
        requireNonNull(callback);
        requireNonNull(executor);

        synchronized (mCallbackExecutorMap) {
            if (mCallbackExecutorMap.containsKey(callback)) {
                throw new IllegalArgumentException("Callback already registered");
            }

            if (service != null && mCallbackExecutorMap.isEmpty()) {
                mRegisterConsumer.accept(service);
            }

            mCallbackExecutorMap.put(callback, executor);
        }
    }

    /** Register the callback and remove the wrapper to the service if needed */
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @SuppressLint("AndroidFrameworkRequiresPermission") // Consumer wrongly report permission
    public void unregisterCallback(@Nullable S service, @NonNull T callback) {
        requireNonNull(callback);
        synchronized (mCallbackExecutorMap) {
            if (!mCallbackExecutorMap.containsKey(callback)) {
                throw new IllegalArgumentException("Callback already unregistered");
            }

            mCallbackExecutorMap.remove(callback);

            if (service != null && mCallbackExecutorMap.isEmpty()) {
                mUnregisterConsumer.accept(service);
            }
        }
    }

    /** A new Service is available from Bluetooth and callback need to be registered */
    public void registerToNewService(@NonNull S service) {
        requireNonNull(service);
        synchronized (mCallbackExecutorMap) {
            if (!mCallbackExecutorMap.isEmpty()) {
                mRegisterConsumer.accept(service);
            }
        }
    }
}

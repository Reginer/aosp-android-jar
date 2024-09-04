/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.NonNull;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/** @hide */
public class BluetoothLeBroadcastAssistantCallback
        extends IBluetoothLeBroadcastAssistantCallback.Stub {
    private static final String TAG = BluetoothLeBroadcastAssistantCallback.class.getSimpleName();
    private boolean mIsRegistered = false;
    private final Map<BluetoothLeBroadcastAssistant.Callback, Executor> mCallbackMap =
            new HashMap<>();
    IBluetoothLeBroadcastAssistant mAdapter;

    public BluetoothLeBroadcastAssistantCallback(IBluetoothLeBroadcastAssistant adapter) {
        mAdapter = adapter;
    }

    /**
     * @hide
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link BluetoothLeBroadcastAssistant#Callback}
     * @throws IllegalArgumentException if the same <var>callback<var> is already registered.
     */
    public void register(
            @NonNull Executor executor, @NonNull BluetoothLeBroadcastAssistant.Callback callback) {
        synchronized (this) {
            if (mCallbackMap.containsKey(callback)) {
                throw new IllegalArgumentException("callback is already registered");
            }
            mCallbackMap.put(callback, executor);

            if (!mIsRegistered) {
                try {
                    mAdapter.registerCallback(this);
                    mIsRegistered = true;
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to register broadcast assistant callback");
                    Log.e(TAG, Log.getStackTraceString(new Throwable()));
                }
            }
        }
    }

    /**
     * @hide
     * @param callback user implementation of the {@link BluetoothLeBroadcastAssistant#Callback}
     * @throws IllegalArgumentException if <var>callback</var> was not registered before
     */
    public void unregister(@NonNull BluetoothLeBroadcastAssistant.Callback callback) {
        synchronized (this) {
            if (!mCallbackMap.containsKey(callback)) {
                throw new IllegalArgumentException("callback was not registered before");
            }
            mCallbackMap.remove(callback);
            if (mCallbackMap.isEmpty() && mIsRegistered) {
                try {
                    mAdapter.unregisterCallback(this);
                    mIsRegistered = false;
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to unregister callback with service");
                    Log.e(TAG, Log.getStackTraceString(new Throwable()));
                }
            }
        }
    }

    /**
     * Check if at least one callback is registered from this App
     *
     * @return true if at least one callback is registered
     * @hide
     */
    public boolean isAtLeastOneCallbackRegistered() {
        synchronized (this) {
            return !mCallbackMap.isEmpty();
        }
    }

    @Override
    public void onSearchStarted(int reason) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSearchStarted(reason));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onSearchStartFailed(int reason) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSearchStartFailed(reason));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onSearchStopped(int reason) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSearchStopped(reason));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onSearchStopFailed(int reason) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSearchStopFailed(reason));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onSourceFound(BluetoothLeBroadcastMetadata source) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSourceFound(source));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onSourceAdded(BluetoothDevice sink, int sourceId, int reason) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSourceAdded(sink, sourceId, reason));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onSourceAddFailed(
            BluetoothDevice sink, BluetoothLeBroadcastMetadata source, int reason) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSourceAddFailed(sink, source, reason));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onSourceModified(BluetoothDevice sink, int sourceId, int reason) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSourceModified(sink, sourceId, reason));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onSourceModifyFailed(BluetoothDevice sink, int sourceId, int reason) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSourceModifyFailed(sink, sourceId, reason));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onSourceRemoved(BluetoothDevice sink, int sourceId, int reason) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSourceRemoved(sink, sourceId, reason));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onSourceRemoveFailed(BluetoothDevice sink, int sourceId, int reason) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSourceRemoveFailed(sink, sourceId, reason));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onReceiveStateChanged(
            BluetoothDevice sink, int sourceId, BluetoothLeBroadcastReceiveState state) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onReceiveStateChanged(sink, sourceId, state));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    @Override
    public void onSourceLost(int broadcastId) {
        synchronized (this) {
            for (BluetoothLeBroadcastAssistant.Callback cb : mCallbackMap.keySet()) {
                Executor executor = mCallbackMap.get(cb);
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> cb.onSourceLost(broadcastId));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }
}

/*
 * Copyright 2021 The Android Open Source Project
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

package android.uwb;
import android.annotation.NonNull;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;
import android.uwb.UwbManager.UwbVendorUciCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @hide
 */
public final class UwbVendorUciCallbackListener extends IUwbVendorUciCallback.Stub{
    private static final String TAG = "Uwb.UwbVendorUciCallbacks";
    private final IUwbAdapter mAdapter;
    private boolean mIsRegistered = false;
    private final Map<UwbVendorUciCallback, Executor> mCallbackMap = new HashMap<>();

    public UwbVendorUciCallbackListener(@NonNull IUwbAdapter adapter) {
        mAdapter = adapter;
    }

    public void register(@NonNull Executor executor, @NonNull UwbVendorUciCallback callback) {
        synchronized (this) {
            if (mCallbackMap.containsKey(callback)) {
                return;
            }
            mCallbackMap.put(callback, executor);
            if (!mIsRegistered) {
                try {
                    mAdapter.registerVendorExtensionCallback(this);
                    mIsRegistered = true;
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to register adapter state callback");
                    mCallbackMap.remove(callback);
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }
    public void unregister(@NonNull UwbVendorUciCallback callback) {
        synchronized (this) {
            if (!mCallbackMap.containsKey(callback) || !mIsRegistered) {
                return;
            }
            if (mCallbackMap.size() == 1) {
                try {
                    mAdapter.unregisterVendorExtensionCallback(this);
                    mIsRegistered = false;
                    mCallbackMap.remove(callback);
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to unregister AdapterStateCallback with service");
                    throw e.rethrowFromSystemServer();
                }
            } else {
                mCallbackMap.remove(callback);
            }
        }
    }

    @Override
    public void onVendorResponseReceived(int gid, int oid, @NonNull byte[] payload)
            throws RemoteException {
        synchronized (this) {
            final long identity = Binder.clearCallingIdentity();
            try {
                for (UwbVendorUciCallback callback : mCallbackMap.keySet()) {
                    Executor executor = mCallbackMap.get(callback);
                    executor.execute(() -> callback.onVendorUciResponse(gid, oid, payload));
                }
            } catch (RuntimeException ex) {
                throw ex;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    @Override
    public void onVendorNotificationReceived(int gid, int oid, @NonNull byte[] payload)
            throws RemoteException {
        synchronized (this) {
            final long identity = Binder.clearCallingIdentity();
            try {
                for (UwbVendorUciCallback callback : mCallbackMap.keySet()) {
                    Executor executor = mCallbackMap.get(callback);
                    executor.execute(() -> callback.onVendorUciNotification(gid, oid, payload));
                }
            } catch (RuntimeException ex) {
                throw ex;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }
}

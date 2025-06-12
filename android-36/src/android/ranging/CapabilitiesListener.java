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

package android.ranging;

import android.annotation.NonNull;
import android.os.Binder;
import android.os.RemoteException;
import android.ranging.RangingManager.RangingCapabilitiesCallback;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * @hide
 */
public class CapabilitiesListener extends IRangingCapabilitiesCallback.Stub {

    private static final String TAG = CapabilitiesListener.class.getSimpleName();
    private final IRangingAdapter mRangingAdapter;
    private boolean mIsRegistered = false;

    private final Map<RangingCapabilitiesCallback, Executor> mCallbackMap =
            new ConcurrentHashMap<>();
    private RangingCapabilities mCachedCapabilities;

    public CapabilitiesListener(@NonNull IRangingAdapter rangingAdapter) {
        mRangingAdapter = rangingAdapter;
    }


    public void register(@NonNull Executor executor,
            @NonNull RangingCapabilitiesCallback callback) {
        if (mCallbackMap.containsKey(callback)) {
            return;
        }
        mCallbackMap.put(callback, executor);

        if (!mIsRegistered) {
            try {
                mRangingAdapter.registerCapabilitiesCallback(this);
                mIsRegistered = true;
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to register capabilities callback");
                throw e.rethrowFromSystemServer();
            }
        } else {
            sendCurrentCapabilities(callback);
        }
    }

    public void unregister(@NonNull RangingCapabilitiesCallback callback) {
        synchronized (this) {
            if (!mCallbackMap.containsKey(callback)) {
                return;
            }
            mCallbackMap.remove(callback);

            if (mCallbackMap.isEmpty() && mIsRegistered) {
                try {
                    mRangingAdapter.unregisterCapabilitiesCallback(this);
                    mIsRegistered = false;
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to unregister capabilities callback");
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    private void sendCurrentCapabilities(@NonNull RangingCapabilitiesCallback callback) {
        synchronized (this) {
            if (mCachedCapabilities != null) {
                Executor executor = mCallbackMap.get(callback);
                long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> callback.onRangingCapabilities(mCachedCapabilities));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    private void onCapabilitiesUpdated(RangingCapabilities rangingCapabilities) {
        synchronized (this) {
            mCachedCapabilities = rangingCapabilities;
            for (RangingCapabilitiesCallback cb : mCallbackMap.keySet()) {
                sendCurrentCapabilities(cb);
            }
        }
    }

    @Override
    public void onRangingCapabilities(RangingCapabilities rangingCapabilities)
            throws RemoteException {
        onCapabilitiesUpdated(rangingCapabilities);
    }
}

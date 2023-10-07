/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.clockwork.displayoffload;

import static com.android.clockwork.displayoffload.DebugUtils.DEBUG_CALLBACK;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;

import com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ClientCallbackManager {
    private static final String TAG = "DOCallback";

    private final Map<Integer, ClientCallback> mClientCallbackList = new ArrayMap<>();
    private final ExecutorService mSingleThreadExecutor = Executors.newSingleThreadExecutor();

    synchronized void attachCallback(int uid, IDisplayOffloadCallbacks callbacks) {
        detachCallback(uid);

        ClientCallback clientCallback = new ClientCallback(callbacks, uid);
        try {
            callbacks.asBinder().linkToDeath(clientCallback, /* flags= */ 0);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
        mClientCallbackList.put(uid, clientCallback);
        if (DEBUG_CALLBACK) {
            Log.d(TAG, "attached: uid=" + uid);
        }
    }

    synchronized void detachCallback(int uid) {
        if (!mClientCallbackList.containsKey(uid)) {
            return;
        }
        final ClientCallback clientCallback = mClientCallbackList.get(uid);
        clientCallback.mCallbacks.asBinder().unlinkToDeath(clientCallback, 0);
        mClientCallbackList.remove(uid);
        if (DEBUG_CALLBACK) {
            Log.d(TAG, "detached: uid=" + uid);
        }
    }

    synchronized IDisplayOffloadCallbacks getCallbackForUid(int uid) {
        if (!mClientCallbackList.containsKey(uid)) {
            // Not found
            if (DEBUG_CALLBACK) {
                Log.i(TAG, "callback no longer exists, uid=" + uid);
            }
            return null;
        }

        return mClientCallbackList.get(uid).mCallbacks;
    }

    synchronized void postCallbackRunnable(
            int uid, ThrowingConsumer<IDisplayOffloadCallbacks, RemoteException> callbackRunnable) {
        mSingleThreadExecutor.submit(() -> invokeCallbackRunnable(uid, callbackRunnable));
    }

    synchronized void invokeCallbackRunnable(
            int uid, ThrowingConsumer<IDisplayOffloadCallbacks, RemoteException> callbackRunnable) {
        if (DEBUG_CALLBACK) {
            Log.d(TAG, "invoking callback: uid=" + uid);
        }
        IDisplayOffloadCallbacks callbacks = getCallbackForUid(uid);
        if (callbacks == null) {
            Log.i(TAG, "callback: uid=" + uid + " is null, skipped");
            return;
        }
        try {
            callbackRunnable.accept(callbacks);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

    final class ClientCallback implements IBinder.DeathRecipient {
        final IDisplayOffloadCallbacks mCallbacks;
        final int mUid;

        ClientCallback(IDisplayOffloadCallbacks callbacks, int uid) {
            mCallbacks = callbacks;
            mUid = uid;
        }

        @Override
        public void binderDied() {
            if (DEBUG_CALLBACK) {
                Log.d(TAG, "binderDied: uid=" + mUid);
            }
            mClientCallbackList.remove(mUid);
        }
    }
}

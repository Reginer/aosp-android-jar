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
package android.nfc;

import android.annotation.NonNull;
import android.nfc.NfcAdapter.NfcVendorNciCallback;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/** @hide */
public final class NfcVendorNciCallbackListener extends INfcVendorNciCallback.Stub {
    private static final String TAG = "Nfc.NfcVendorNciCallbacks";
    private boolean mIsRegistered = false;
    private final Map<NfcVendorNciCallback, Executor> mCallbackMap = new HashMap<>();
    private IBinder.DeathRecipient mDeathRecipient;

    public NfcVendorNciCallbackListener() {}

    private void linkToNfcDeath() {
        try {
            mDeathRecipient = new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    synchronized (this) {
                        mDeathRecipient = null;
                    }
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            try {
                                synchronized (this) {
                                    if (mCallbackMap.size() > 0) {
                                        NfcAdapter.callService(() ->
                                                NfcAdapter.getService()
                                                        .registerVendorExtensionCallback(
                                                                NfcVendorNciCallbackListener.this));
                                    }
                                }
                            } catch (Throwable t) {
                                handler.postDelayed(this, 50);
                            }
                        }
                    }, 50);
                }
            };
            NfcAdapter.getService().asBinder().linkToDeath(mDeathRecipient, 0);
        } catch (RemoteException re) {
            Log.e(TAG, "Couldn't link to death");
        }
    }

    public void register(@NonNull Executor executor, @NonNull NfcVendorNciCallback callback) {
        synchronized (this) {
            if (mCallbackMap.containsKey(callback)) {
                return;
            }
            mCallbackMap.put(callback, executor);
            if (!mIsRegistered) {
                final  NfcVendorNciCallbackListener listener = this;
                NfcAdapter.callService(() -> {
                    NfcAdapter.getService().registerVendorExtensionCallback(listener);
                    linkToNfcDeath();
                    mIsRegistered = true;
                });
            }
        }
    }

    public void unregister(@NonNull NfcVendorNciCallback callback) {
        synchronized (this) {
            if (!mCallbackMap.containsKey(callback) || !mIsRegistered) {
                return;
            }
            if (mCallbackMap.size() == 1) {
                final NfcVendorNciCallbackListener listener = this;
                NfcAdapter.callService(() -> {
                    NfcAdapter.getService().unregisterVendorExtensionCallback(listener);
                    NfcAdapter.getService().asBinder().unlinkToDeath(mDeathRecipient, 0);
                    mIsRegistered = false;
                });
            }
            mCallbackMap.remove(callback);
        }
    }

    @Override
    public void onVendorResponseReceived(int gid, int oid, @NonNull byte[] payload)
            throws RemoteException {
        synchronized (this) {
            final long identity = Binder.clearCallingIdentity();
            try {
                for (NfcVendorNciCallback callback : mCallbackMap.keySet()) {
                    Executor executor = mCallbackMap.get(callback);
                    executor.execute(() -> callback.onVendorNciResponse(gid, oid, payload));
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
                for (NfcVendorNciCallback callback : mCallbackMap.keySet()) {
                    Executor executor = mCallbackMap.get(callback);
                    executor.execute(() -> callback.onVendorNciNotification(gid, oid, payload));
                }
            } catch (RuntimeException ex) {
                throw ex;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }
}

/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.util.Log;

import com.android.modules.utils.build.SdkLevel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @hide
 */
public final class UwbOemExtensionCallbackListener extends IUwbOemExtensionCallback.Stub {

    private static final String TAG = "Uwb.UwbOemExtensionCallback";
    private static final int OEM_EXTENSION_RESPONSE_THRESHOLD_MS = 2000;
    private final IUwbAdapter mAdapter;
    private Executor mExecutor = null;
    private UwbManager.UwbOemExtensionCallback mCallback = null;

    public UwbOemExtensionCallbackListener(@NonNull IUwbAdapter adapter) {
        mAdapter = adapter;
    }

    public void register(@android.annotation.NonNull Executor executor,
            @android.annotation.NonNull UwbManager.UwbOemExtensionCallback callback) {
        synchronized (this) {
            if (mCallback != null) {
                Log.e(TAG, "Callback already registered. Unregister existing callback before"
                        + "registering");
                throw new IllegalArgumentException();
            }
            try {
                mAdapter.registerOemExtensionCallback(this);
                mExecutor = executor;
                mCallback = callback;
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to register Oem extension callback");
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void unregister(
            @android.annotation.NonNull UwbManager.UwbOemExtensionCallback callback) {
        synchronized (this) {
            if (mCallback == null || mCallback != callback) {
                Log.e(TAG, "Callback not registered");
                throw new IllegalArgumentException();
            }
            try {
                mAdapter.unregisterOemExtensionCallback(this);
                mCallback = null;
                mExecutor = null;
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to unregister Oem extension callback");
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @Override
    public void onSessionStatusNotificationReceived(PersistableBundle sessionStatusBundle)
            throws RemoteException {
        synchronized (this) {
            final long identity = Binder.clearCallingIdentity();
            try {
                if (SdkLevel.isAtLeastU()) {
                    mExecutor.execute(() ->
                            mCallback.onSessionStatusNotificationReceived(sessionStatusBundle));
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    @Override
    public void onDeviceStatusNotificationReceived(PersistableBundle deviceStateBundle)
            throws RemoteException {
        synchronized (this) {
            final long identity = Binder.clearCallingIdentity();
            try {
                if (SdkLevel.isAtLeastU()) {
                    mExecutor.execute(() ->
                            mCallback.onDeviceStatusNotificationReceived(deviceStateBundle));
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

    }

    @Override
    public int onSessionConfigurationReceived(PersistableBundle openSessionBundle)
            throws RemoteException {
        synchronized (this) {
            int status = 0;
            final long identity = Binder.clearCallingIdentity();
            try {
                if (SdkLevel.isAtLeastU()) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    FutureTask<Integer> p = new FutureTask<>(
                            () -> mCallback.onSessionConfigurationComplete(openSessionBundle));
                    executor.submit(p);
                    try {
                        status = p.get(OEM_EXTENSION_RESPONSE_THRESHOLD_MS, TimeUnit.MILLISECONDS);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        Log.w(TAG,
                                "Failed to get response for session config from vendor - status :"
                                        + " TIMEOUT");
                        e.printStackTrace();
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
            return status;
        }
    }

    @Override
    public RangingReport onRangingReportReceived(RangingReport rangingReport)
            throws RemoteException {
        synchronized (this) {
            final long identity = Binder.clearCallingIdentity();
            RangingReport vendorRangingReport = rangingReport;
            try {
                if (SdkLevel.isAtLeastU()) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    FutureTask<RangingReport> getOemRangingReport = new FutureTask<RangingReport>(
                            () -> mCallback.onRangingReportReceived(rangingReport)
                    );
                    executor.submit(getOemRangingReport);
                    try {
                        vendorRangingReport = getOemRangingReport.get(
                                OEM_EXTENSION_RESPONSE_THRESHOLD_MS, TimeUnit.MILLISECONDS);
                        return vendorRangingReport == null ? rangingReport
                                : vendorRangingReport;
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        Log.w(TAG, "Failed to get ranging report from vendor: TIMEOUT");
                        e.printStackTrace();
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
            return vendorRangingReport;
        }
    }

    @Override
    public boolean onCheckPointedTarget(PersistableBundle pointedTargetBundle)
            throws RemoteException {
        synchronized (this) {
            final long identity = Binder.clearCallingIdentity();
            boolean result = false;
            try {
                if (SdkLevel.isAtLeastU()) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    FutureTask<Boolean> getCheckPointedTarget = new FutureTask<>(
                            () -> mCallback.onCheckPointedTarget(pointedTargetBundle)
                    );
                    executor.submit(getCheckPointedTarget);
                    try {
                        result = getCheckPointedTarget.get(
                                OEM_EXTENSION_RESPONSE_THRESHOLD_MS, TimeUnit.MILLISECONDS);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        Log.w(TAG, "Check pointed target failed: TIMEOUT");
                        e.printStackTrace();
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
            return result;
        }
    }
}

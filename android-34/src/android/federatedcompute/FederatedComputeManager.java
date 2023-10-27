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

package android.federatedcompute;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.federatedcompute.aidl.IFederatedComputeCallback;
import android.federatedcompute.aidl.IFederatedComputeService;
import android.federatedcompute.common.ScheduleFederatedComputeRequest;
import android.ondevicepersonalization.OnDevicePersonalizationException;
import android.os.IBinder;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * FederatedCompute Manager.
 *
 * @hide
 */
final class FederatedComputeManager {
    private static final String TAG = "FederatedComputeManager";
    private static final String FEDERATED_COMPUTATION_SERVICE_INTENT_FILTER_NAME =
            "android.federatedcompute.FederatedComputeService";
    private static final int BINDER_CONNECTION_TIMEOUT_MS = 5000;

    // A CountDownloadLatch which will be opened when the connection is established or any error
    // occurs.
    private CountDownLatch mConnectionCountDownLatch;
    // Concurrency mLock.
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private IFederatedComputeService mFcpService;

    @GuardedBy("mLock")
    private ServiceConnection mServiceConnection;

    private final Context mContext;

    FederatedComputeManager(Context context) {
        this.mContext = context;
    }

    /**
     * Schedule FederatedCompute task.
     *
     * @hide
     */
    public void scheduleFederatedCompute(
            @NonNull ScheduleFederatedComputeRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> callback) {
        Objects.requireNonNull(request);
        final IFederatedComputeService service = getService(executor);
        try {
            IFederatedComputeCallback federatedComputeCallback =
                    new IFederatedComputeCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            executor.execute(
                                    () ->
                                            callback.onError(
                                                    new OnDevicePersonalizationException(
                                                            errorCode)));
                        }
                    };
            service.scheduleFederatedCompute(
                    request.getTrainingOptions(), federatedComputeCallback);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote Exception", e);
            executor.execute(() -> callback.onError(e));
        }
    }

    private IFederatedComputeService getService(@NonNull Executor executor) {
        synchronized (mLock) {
            if (mFcpService != null) {
                return mFcpService;
            }
            if (mServiceConnection == null) {
                Intent intent = new Intent(FEDERATED_COMPUTATION_SERVICE_INTENT_FILTER_NAME);
                ComponentName serviceComponent = resolveService(intent);
                if (serviceComponent == null) {
                    Log.e(TAG, "Invalid component for federatedcompute service");
                    throw new IllegalStateException(
                            "Invalid component for federatedcompute service");
                }
                intent.setComponent(serviceComponent);
                // This latch will open when the connection is established or any error occurs.
                mConnectionCountDownLatch = new CountDownLatch(1);
                mServiceConnection = new FederatedComputeServiceConnection();
                boolean result =
                        mContext.bindService(
                                intent, Context.BIND_AUTO_CREATE, executor, mServiceConnection);
                if (!result) {
                    mServiceConnection = null;
                    throw new IllegalStateException("Unable to bind to the service");
                } else {
                    Log.i(TAG, "bindService() succeeded...");
                }
            } else {
                Log.i(TAG, "bindService() already pending...");
            }
            try {
                mConnectionCountDownLatch.await(BINDER_CONNECTION_TIMEOUT_MS, MILLISECONDS);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Thread interrupted"); // TODO Handle it better.
            }
            synchronized (mLock) {
                if (mFcpService == null) {
                    throw new IllegalStateException("Failed to connect to the service");
                }
                return mFcpService;
            }
        }
    }

    /**
     * Find the ComponentName of the service, given its intent and package manager.
     *
     * @return ComponentName of the service. Null if the service is not found.
     */
    @Nullable
    private ComponentName resolveService(@NonNull Intent intent) {
        List<ResolveInfo> services = mContext.getPackageManager().queryIntentServices(intent, 0);
        if (services == null || services.isEmpty()) {
            Log.e(TAG, "Failed to find federatedcompute service");
            return null;
        }

        for (int i = 0; i < services.size(); i++) {
            ServiceInfo serviceInfo = services.get(i).serviceInfo;
            if (serviceInfo == null) {
                Log.e(TAG, "Failed to find serviceInfo for federatedcompute service.");
                return null;
            }
            // There should only be one matching service inside the given package.
            // If there's more than one, return the first one found.
            return new ComponentName(serviceInfo.packageName, serviceInfo.name);
        }
        Log.e(TAG, "Didn't find any matching federatedcompute service.");
        return null;
    }

    public void unbindFromService() {
        synchronized (mLock) {
            if (mServiceConnection != null) {
                Log.i(TAG, "unbinding...");
                mContext.unbindService(mServiceConnection);
            }
            mServiceConnection = null;
            mFcpService = null;
        }
    }

    private class FederatedComputeServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            synchronized (mLock) {
                mFcpService = IFederatedComputeService.Stub.asInterface(service);
            }
            mConnectionCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            unbindFromService();
            mConnectionCountDownLatch.countDown();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.e(TAG, "onBindingDied");
            unbindFromService();
            mConnectionCountDownLatch.countDown();
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Log.e(TAG, "onNullBinding shouldn't happen.");
            unbindFromService();
            mConnectionCountDownLatch.countDown();
        }
    }
}

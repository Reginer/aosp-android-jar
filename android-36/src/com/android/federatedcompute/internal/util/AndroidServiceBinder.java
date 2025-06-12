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

package com.android.federatedcompute.internal.util;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;

import com.android.internal.annotations.GuardedBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Function;

class AndroidServiceBinder<T> extends AbstractServiceBinder<T> {
    private static final String TAG = AndroidServiceBinder.class.getSimpleName();

    private static final int BINDER_CONNECTION_TIMEOUT_MS = 5000;
    private static final int MAX_GET_SERVICE_RETRIES = 2;
    private static final long GET_SERVICE_RETRY_DELAY_MS = 100L;
    private final String mServiceIntentActionOrName;
    private final List<String> mServicePackages;
    private final Function<IBinder, T> mBinderConverter;
    private final Context mContext;
    private final boolean mEnableLookupByServiceName;
    private final String mIsolatedProcessName;
    private final int mBindFlags;
    // Concurrency mLock.
    private final Object mLock = new Object();
    // A CountDownLatch which will be opened when the connection is established or any error
    // occurs.
    private CountDownLatch mConnectionCountDownLatch;

    @GuardedBy("mLock")
    private T mService;

    @GuardedBy("mLock")
    private ServiceConnection mServiceConnection;

    AndroidServiceBinder(
            @NonNull Context context,
            @NonNull String serviceIntentAction,
            @NonNull String servicePackage,
            @NonNull Function<IBinder, T> converter) {
        this(context, serviceIntentAction,  List.of(servicePackage), converter);
    }

    AndroidServiceBinder(
            @NonNull Context context,
            @NonNull String serviceIntentAction,
            @NonNull List<String> servicePackages,
            @NonNull Function<IBinder, T> converter) {
        this(context, serviceIntentAction, servicePackages, /* bindFlags= */ 0, converter);
    }

    AndroidServiceBinder(
            @NonNull Context context,
            @NonNull String serviceIntentAction,
            @NonNull List<String> servicePackages,
            int bindFlags,
            @NonNull Function<IBinder, T> converter) {
        this(
                context,
                serviceIntentAction,
                servicePackages,
                /* isolatedProcessName= */ null,
                /* enableLookupByName= */ false,
                bindFlags,
                converter);
    }

    AndroidServiceBinder(
            @NonNull Context context,
            @NonNull String serviceIntentActionOrName,
            @NonNull String servicePackage,
            boolean enableLookupByName,
            @NonNull Function<IBinder, T> converter) {
        this(
                context,
                serviceIntentActionOrName,
                servicePackage,
                /* isolatedProcessName= */ null,
                enableLookupByName,
                /* bindFlags= */ 0,
                converter);
    }

    AndroidServiceBinder(
            @NonNull Context context,
            @NonNull String serviceIntentActionOrName,
            @NonNull String servicePackage,
            @NonNull String isolatedProcessName,
            boolean enableLookupByName,
            int bindFlags,
            @NonNull Function<IBinder, T> converter) {
        this(
                context,
                serviceIntentActionOrName,
                List.of(servicePackage),
                isolatedProcessName,
                enableLookupByName,
                bindFlags,
                converter);
    }

    private AndroidServiceBinder(
            @NonNull Context context,
            @NonNull String serviceIntentActionOrName,
            @NonNull List<String> servicePackages,
            @NonNull String isolatedProcessName,
            boolean enableLookupByName,
            int bindFlags,
            @NonNull Function<IBinder, T> converter) {
        this.mServiceIntentActionOrName = serviceIntentActionOrName;
        this.mContext = context;
        this.mBinderConverter = converter;
        this.mServicePackages = servicePackages;
        this.mEnableLookupByServiceName = enableLookupByName;
        this.mBindFlags = bindFlags;
        this.mIsolatedProcessName = isolatedProcessName;
    }

    @Override
    public T getService(@NonNull Executor executor) {
        int retryAttempts = 0;
        T service;
        IllegalStateException exceptionInfo = null;

        while (retryAttempts < MAX_GET_SERVICE_RETRIES) {
            try {
                service = getServiceWithoutRetry(executor);
                if (service != null) {
                    return service;
                }
            } catch (IllegalStateException e) {
                LogUtil.e(TAG, e, "Failed to get service on attempt " + (retryAttempts + 1));
                exceptionInfo = e;
            }
            retryAttempts++;
            try {
                Thread.sleep(GET_SERVICE_RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                LogUtil.w(TAG, "Thread sleep interrupted");
            }
        }

        throw exceptionInfo != null
            ? exceptionInfo
            : new IllegalStateException(
                String.format("Failed to get non-null service %s after %d retries",
                    mServiceIntentActionOrName, retryAttempts));
    }

    private T getServiceWithoutRetry(@NonNull Executor executor) {
        synchronized (mLock) {
            if (mService != null) {
                return mService;
            }
            if (mServiceConnection == null) {
                Intent bindIntent =
                        mEnableLookupByServiceName
                                ? getIntentBasedOnServiceName()
                                : getIntentBasedOnAction();
                // This latch will open when the connection is established or any error occurs.
                mConnectionCountDownLatch = new CountDownLatch(1);
                mServiceConnection = new GenericServiceConnection();

                boolean result =
                        (mIsolatedProcessName != null)
                        ?
                            mContext.bindIsolatedService(
                                bindIntent,
                                Context.BIND_AUTO_CREATE | mBindFlags,
                                mIsolatedProcessName,
                                executor, mServiceConnection)
                        :
                                mContext.bindService(
                                        bindIntent,
                                        Context.BIND_AUTO_CREATE | mBindFlags, executor,
                                        mServiceConnection);

                if (!result) {
                    mServiceConnection = null;
                    throw new IllegalStateException(
                            String.format(
                                    "Unable to bind to the service %s",
                                    mServiceIntentActionOrName));
                } else {
                    LogUtil.i(TAG, "bindService() %s succeeded...", mServiceIntentActionOrName);
                }
            } else {
                LogUtil.i(TAG, "bindService() %s already pending...", mServiceIntentActionOrName);
            }
        }
        // Release the lock to let the ServiceConnection set the mService. If unbind race condition
        // happen here (e.g. onBindingDied called) client should retry
        try {
            mConnectionCountDownLatch.await(BINDER_CONNECTION_TIMEOUT_MS, MILLISECONDS);
        } catch (InterruptedException e) {
            LogUtil.e(TAG, "Failed to connect to the service %s ", mServiceIntentActionOrName);
            throw new IllegalStateException("Thread interrupted"); // TODO Handle it better.
        }
        synchronized (mLock) {
            if (mService == null) {
                throw new IllegalStateException(
                        String.format(
                                "Failed to connect to the service %s", mServiceIntentActionOrName));
            }
            return mService;
        }
    }

    private Intent getIntentBasedOnServiceName() {
        Intent intent = new Intent();
        ComponentName serviceComponent =
                new ComponentName(mServicePackages.get(0), mServiceIntentActionOrName);
        intent.setComponent(serviceComponent);
        return intent;
    }

    private Intent getIntentBasedOnAction() {
        Intent intent = new Intent(mServiceIntentActionOrName);
        ComponentName serviceComponent = resolveComponentName(intent);
        if (serviceComponent == null) {
            LogUtil.e(TAG, "Invalid component for %s intent", mServiceIntentActionOrName);
            throw new IllegalStateException(
                    String.format("Invalid component for %s service", mServiceIntentActionOrName));
        }
        intent.setComponent(serviceComponent);
        return intent;
    }

    /**
     * Find the ComponentName of the service, given its intent and package manager.
     *
     * @return ComponentName of the service. Null if the service is not found.
     */
    @Nullable
    private ComponentName resolveComponentName(@NonNull Intent intent) {
        List<ResolveInfo> services =
                mContext.getPackageManager()
                        .queryIntentServices(intent, PackageManager.MATCH_SYSTEM_ONLY);
        if (services == null || services.isEmpty()) {
            LogUtil.e(TAG, "Failed to find service %s!", intent.getAction());
            return null;
        } else if (services.size() != 1) {
            LogUtil.i(TAG, "Found more than 1 (%d) service by intent %s!", services.size(), intent);
        }

        for (ResolveInfo ri : services) {
            // Check that found service has expected package.
            if (ri != null
                    && ri.serviceInfo != null
                    && ri.serviceInfo.packageName != null
                    && mServicePackages.contains(ri.serviceInfo.packageName)) {
                // There should only be one matching service inside the given package.
                // If there's more than one, return the first one found.
                LogUtil.d(
                        TAG,
                        "Resolved component with pkg %s, class %s",
                        ri.serviceInfo.packageName,
                        ri.serviceInfo.name);
                return new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name);
            } else {
                if (ri != null && ri.serviceInfo != null) {
                    LogUtil.d(
                            TAG,
                            "Resolved component with pkg %s, class %s",
                            ri.serviceInfo.packageName,
                            ri.serviceInfo.name);
                } else {
                    LogUtil.d(TAG, "Resolved component is null or service info is null");
                }
            }
        }
        LogUtil.e(TAG, "Didn't find any matching service %s.", intent.getAction());
        return null;
    }

    public void unbindFromService() {
        synchronized (mLock) {
            if (mServiceConnection != null) {
                LogUtil.d(TAG, "unbinding %s...", mServiceIntentActionOrName);
                try {
                    mContext.unbindService(mServiceConnection);
                } catch (IllegalArgumentException e) {
                    LogUtil.e(TAG, e, "unbinding failed %s", mServiceIntentActionOrName);
                }
            }
            mServiceConnection = null;
            mService = null;
        }
    }

    private class GenericServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.d(TAG, "onServiceConnected " + mServiceIntentActionOrName);
            synchronized (mLock) {
                mService = mBinderConverter.apply(service);
            }
            mConnectionCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtil.d(TAG, "onServiceDisconnected " + mServiceIntentActionOrName);
            unbindFromService();
            mConnectionCountDownLatch.countDown();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            LogUtil.w(TAG, "onBindingDied " + mServiceIntentActionOrName);
            unbindFromService();
            mConnectionCountDownLatch.countDown();
        }

        @Override
        public void onNullBinding(ComponentName name) {
            LogUtil.w(TAG, "onNullBinding shouldn't happen. " + mServiceIntentActionOrName);
            unbindFromService();
            mConnectionCountDownLatch.countDown();
        }
    }
}

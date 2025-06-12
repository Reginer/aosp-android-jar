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
package com.android.adservices;

import static android.adservices.common.AdServicesStatusUtils.SERVICE_UNAVAILABLE_ERROR_MESSAGE;

import static com.android.adservices.AdServicesCommon.ACTION_ADID_PROVIDER_SERVICE;
import static com.android.adservices.AdServicesCommon.ACTION_ADID_SERVICE;
import static com.android.adservices.AdServicesCommon.ACTION_AD_SELECTION_SERVICE;
import static com.android.adservices.AdServicesCommon.ACTION_AD_SERVICES_COBALT_UPLOAD_SERVICE;
import static com.android.adservices.AdServicesCommon.ACTION_AD_SERVICES_COMMON_SERVICE;
import static com.android.adservices.AdServicesCommon.ACTION_APPSETID_PROVIDER_SERVICE;
import static com.android.adservices.AdServicesCommon.ACTION_APPSETID_SERVICE;
import static com.android.adservices.AdServicesCommon.ACTION_CUSTOM_AUDIENCE_SERVICE;
import static com.android.adservices.AdServicesCommon.ACTION_MEASUREMENT_SERVICE;
import static com.android.adservices.AdServicesCommon.ACTION_PROTECTED_SIGNALS_SERVICE;
import static com.android.adservices.AdServicesCommon.ACTION_SHELL_COMMAND_SERVICE;
import static com.android.adservices.AdServicesCommon.ACTION_TOPICS_SERVICE;
import static com.android.adservices.AdServicesCommon.SYSTEM_PROPERTY_FOR_DEBUGGING_BINDER_TIMEOUT;
import static com.android.adservices.AdServicesCommon.SYSTEM_PROPERTY_FOR_DEBUGGING_FEATURE_RAM_LOW;

import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemProperties;
import android.text.TextUtils;

import com.android.adservices.shared.common.exception.ServiceUnavailableException;
import com.android.internal.annotations.GuardedBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Service binder that connects to a service in the APK.
 *
 * <p>TODO: Make it robust. Currently this class ignores edge cases. TODO: Clean up the log
 *
 * @hide
 */
// TODO(b/251429601): Add unit test for this class.
final class AndroidServiceBinder<T> extends ServiceBinder<T> {
    // TODO(b/218519915): Revisit it.
    private static final int BIND_FLAGS = Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT;

    // It likely causes ANR if a binder call blocks the main thread for >5 seconds.
    private static final int DEFAULT_BINDER_CONNECTION_TIMEOUT_MS = 5_000;

    private final String mServiceIntentAction;
    private final Function<IBinder, T> mBinderConverter;
    private final Context mContext;

    // TODO(b/218519915): have a better timeout handling.
    private final int mBinderTimeout;

    // A CountDownloadLatch which will be opened when the connection is established or any error
    // occurs.
    private CountDownLatch mConnectionCountDownLatch;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private T mService;

    @GuardedBy("mLock")
    private ServiceConnection mServiceConnection;

    private final boolean mSimulatingLowRamDevice;

    // TODO(b/330796095): Create infra for below 2 DEBUG flags to properly test them.
    protected AndroidServiceBinder(
            Context context, String serviceIntentAction, Function<IBinder, T> converter) {
        mServiceIntentAction = serviceIntentAction;
        mContext = context;
        mBinderConverter = converter;
        if (!isDebuggable()) {
            mSimulatingLowRamDevice = false;
        } else {
            String propValue = SystemProperties.get(SYSTEM_PROPERTY_FOR_DEBUGGING_FEATURE_RAM_LOW);
            mSimulatingLowRamDevice = !TextUtils.isEmpty(propValue) && Boolean.valueOf(propValue);
        }
        if (mSimulatingLowRamDevice) {
            LogUtil.w(
                    "Service %s disabled because of system property %s",
                    serviceIntentAction, SYSTEM_PROPERTY_FOR_DEBUGGING_FEATURE_RAM_LOW);
        }

        // Allow debugging environment including tests to override the binder timeout.
        mBinderTimeout =
                SystemProperties.getInt(
                        SYSTEM_PROPERTY_FOR_DEBUGGING_BINDER_TIMEOUT,
                        DEFAULT_BINDER_CONNECTION_TIMEOUT_MS);
        LogUtil.d(
                "AndroidServiceBinder(): serviceIntentAction=%s, fromUser=%d, toUser=%s",
                serviceIntentAction, Process.myUserHandle().getIdentifier(), getUserIdForLogging());
    }

    @Override
    public T getService() {
        if (mSimulatingLowRamDevice) {
            throw new ServiceUnavailableException(
                    "Service is not bound (because of SystemProperty "
                            + SYSTEM_PROPERTY_FOR_DEBUGGING_FEATURE_RAM_LOW
                            + ")");
        }

        synchronized (mLock) {
            // If we already have a service, just return it.
            if (mService != null) {
                // Note there's a chance the service dies right after we return here,
                // but we can't avoid that.
                return mService;
            }

            // If there's no pending bindService(), we need to start one.
            if (mServiceConnection == null) {
                // There's no other pending connection, creating one.
                ComponentName componentName = getServiceComponentName();
                if (componentName == null) {
                    // Already logged
                    return null;
                }
                Intent intent = new Intent(mServiceIntentAction).setComponent(componentName);

                LogUtil.d(
                        "getService(): binding (on user %d) to %s (on user %s)",
                        Process.myUserHandle().getIdentifier(),
                        mServiceIntentAction,
                        getUserIdForLogging());

                // This latch will open when the connection is established or any error occurs.
                mConnectionCountDownLatch = new CountDownLatch(1);
                mServiceConnection = new AdServicesServiceConnection();

                // We use Runnable::run so that the callback is called on a binder thread.
                // Otherwise we'd use the main thread, which could cause a deadlock.
                try {
                    boolean success =
                            mContext.bindService(
                                    intent, BIND_FLAGS, Runnable::run, mServiceConnection);
                    if (!success) {
                        LogUtil.e(
                                "getService(): failed to bind to %s on user %s",
                                mServiceIntentAction, getUserIdForLogging());
                        mServiceConnection = null;
                        return null;
                    } else {
                        LogUtil.d("getService(): bound!");
                    }
                } catch (Exception e) {
                    LogUtil.e(
                            "getService(): caught unexpected exception during service binding to %s"
                                    + " on user %s: %s",
                            mServiceIntentAction, getUserIdForLogging(), e);
                    mServiceConnection = null;
                    return null;
                }
            } else {
                LogUtil.d("getService(): There is already a pending connection!");
            }
        }

        // Then wait for connection result.
        // Note: We must not hold the lock while waiting for the connection since the
        // onServiceConnected callback also needs to acquire the lock. This would cause a deadlock.
        try {
            LogUtil.v("getService(): waiting up to %dms for binder connection", mBinderTimeout);
            // TODO(b/218519915): Better timeout handling
            if (!mConnectionCountDownLatch.await(mBinderTimeout, TimeUnit.MILLISECONDS)) {
                LogUtil.e(
                        "getService(): timed out (%dms) waiting for binder connection",
                        mBinderTimeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Thread interrupted", e); // TODO(b/218519915) Handle it better.
        }

        synchronized (mLock) {
            if (mService == null) {
                throw new ServiceUnavailableException(SERVICE_UNAVAILABLE_ERROR_MESSAGE);
            }
            return mService;
        }
    }

    // A class to handle the connection to the AdService Services.
    private final class AdServicesServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            onCallback(
                    "onServiceConnected",
                    name,
                    () -> {
                        synchronized (mLock) {
                            // Connection is established, open the latch.
                            mService = mBinderConverter.apply(service);
                        }
                    });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            onCallback("onServiceDisconnected", name, () -> unbindFromService());
        }

        @Override
        public void onBindingDied(ComponentName name) {
            onCallback("onBindingDied", name, () -> unbindFromService());
        }

        @Override
        public void onNullBinding(ComponentName name) {
            onCallback("onNullBinding", name, () -> unbindFromService());
        }

        private void onCallback(String callback, @Nullable ComponentName name, Runnable runnable) {
            // Shouldn't be null, but it doesn't hurt to check...
            String service = name == null ? "N/A" : name.flattenToShortString();
            LogUtil.d(
                    "%s(userId=%s, action=%s, service=%s)",
                    callback, getUserIdForLogging(), mServiceIntentAction, service);
            try {
                runnable.run();
            } finally {
                mConnectionCountDownLatch.countDown();
            }
        }
    }

    @Nullable
    private ComponentName getServiceComponentName() {
        if (!mServiceIntentAction.equals(ACTION_TOPICS_SERVICE)
                && !mServiceIntentAction.equals(ACTION_MEASUREMENT_SERVICE)
                && !mServiceIntentAction.equals(ACTION_CUSTOM_AUDIENCE_SERVICE)
                && !mServiceIntentAction.equals(ACTION_AD_SELECTION_SERVICE)
                && !mServiceIntentAction.equals(ACTION_ADID_SERVICE)
                && !mServiceIntentAction.equals(ACTION_ADID_PROVIDER_SERVICE)
                && !mServiceIntentAction.equals(ACTION_APPSETID_SERVICE)
                && !mServiceIntentAction.equals(ACTION_APPSETID_PROVIDER_SERVICE)
                && !mServiceIntentAction.equals(ACTION_AD_SERVICES_COBALT_UPLOAD_SERVICE)
                && !mServiceIntentAction.equals(ACTION_AD_SERVICES_COMMON_SERVICE)
                && !mServiceIntentAction.equals(ACTION_SHELL_COMMAND_SERVICE)
                && !mServiceIntentAction.equals(ACTION_PROTECTED_SIGNALS_SERVICE)) {
            LogUtil.e("Bad service intent action: %s", mServiceIntentAction);
            return null;
        }
        Intent intent = new Intent(mServiceIntentAction);

        List<ResolveInfo> resolveInfos =
                mContext.getPackageManager()
                        .queryIntentServices(intent, PackageManager.MATCH_SYSTEM_ONLY);
        ServiceInfo serviceInfo =
                AdServicesCommon.resolveAdServicesService(resolveInfos, mServiceIntentAction);
        if (serviceInfo == null) {
            LogUtil.e(
                    "Failed to find serviceInfo for adServices service on user %s using intent %s",
                    getUserIdForLogging(), mServiceIntentAction);
            return null;
        }
        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
    }

    private String getUserIdForLogging() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.R
                ? String.valueOf(mContext.getUser().getIdentifier())
                : "N/A";
    }

    @Override
    public void unbindFromService() {
        if (mSimulatingLowRamDevice) {
            LogUtil.d(
                    "unbindFromService(): ignored because it's disabled by system property %s",
                    SYSTEM_PROPERTY_FOR_DEBUGGING_FEATURE_RAM_LOW);
            return;
        }
        synchronized (mLock) {
            if (mServiceConnection != null) {
                LogUtil.d("unbinding...");
                mContext.unbindService(mServiceConnection);
            }
            mServiceConnection = null;
            mService = null;
        }
    }

    private static final boolean IS_DEBUGGABLE = computeIsDebuggable();

    private static boolean isDebuggable() {
        return IS_DEBUGGABLE;
    }

    private static boolean computeIsDebuggable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return Build.isDebuggable();
        }

        // Build.isDebuggable was added in S; duplicate that functionality for R.
        return SystemProperties.getInt("ro.debuggable", 0) == 1;
    }
}

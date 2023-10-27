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

package android.ondevicepersonalization;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.ondevicepersonalization.aidl.IPrivacyStatusService;
import android.ondevicepersonalization.aidl.IPrivacyStatusServiceCallback;
import android.os.IBinder;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.util.Slog;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * OnDevicePersonalizationPrivacyStatusManager provides system APIs
 * for GMSCore to control privacy statuses of ODP users.
 * @hide
 */
public class OnDevicePersonalizationPrivacyStatusManager {
    public static final String ON_DEVICE_PERSONALIZATION_PRIVACY_STATUS_SERVICE =
            "on_device_personalization_privacy_status_service";
    private static final String TAG = "OdpPrivacyStatusManager";
    private static final String ODP_PRIVACY_STATUS_SERVICE_INTENT =
            "android.OnDevicePersonalizationPrivacyStatusService";
    private boolean mBound = false;
    private IPrivacyStatusService mService = null;
    private final Context mContext;
    private final CountDownLatch mConnectionLatch = new CountDownLatch(1);

    public OnDevicePersonalizationPrivacyStatusManager(@NonNull Context context) {
        mContext = context;
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = IPrivacyStatusService.Stub.asInterface(binder);
            mBound = true;
            mConnectionLatch.countDown();
        }

        @Override
        public void onNullBinding(ComponentName name) {
            mBound = false;
            mConnectionLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    private static final int BIND_SERVICE_TIMEOUT_SEC = 5;

    /**
     * Modify the user's kid status in ODP from GMSCore.
     *
     * @param isKidStatusEnabled user's kid status available at GMSCore.
     * @param executor the {@link Executor} on which to invoke the callback
     * @param receiver This either returns true on success or {@link Exception} on failure.
     * @hide
     */
    public void setKidStatus(boolean isKidStatusEnabled,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Boolean, Exception> receiver) {
        try {
            bindService(executor);

            mService.setKidStatus(isKidStatusEnabled, new IPrivacyStatusServiceCallback.Stub() {
                @Override
                public void onSuccess() {
                    executor.execute(() -> receiver.onResult(true));
                }

                @Override
                public void onFailure(int errorCode) {
                    executor.execute(() -> receiver.onError(
                            new OnDevicePersonalizationException(errorCode)));
                }
            });
        } catch (InterruptedException | RemoteException e) {
            receiver.onError(e);
        }
    }

    private void bindService(@NonNull Executor executor) throws InterruptedException {
        if (!mBound) {
            Intent intent = new Intent(ODP_PRIVACY_STATUS_SERVICE_INTENT);
            ComponentName serviceComponent = resolveService(intent);
            if (serviceComponent == null) {
                Slog.e(TAG, "Invalid component for ODP privacy status service");
                return;
            }

            intent.setComponent(serviceComponent);
            boolean r = mContext.bindService(
                    intent, Context.BIND_AUTO_CREATE, executor, mConnection);
            if (!r) {
                return;
            }
            mConnectionLatch.await(BIND_SERVICE_TIMEOUT_SEC, TimeUnit.SECONDS);
        }
    }

    /**
     * Find the ComponentName of the service, given its intent.
     *
     * @return ComponentName of the service. Null if the service is not found.
     */
    @Nullable
    private ComponentName resolveService(@NonNull Intent intent) {
        List<ResolveInfo> services = mContext.getPackageManager().queryIntentServices(intent, 0);
        if (services == null || services.isEmpty()) {
            Slog.e(TAG, "Failed to find OdpPrivacyStatus service");
            return null;
        }

        for (int i = 0; i < services.size(); i++) {
            ServiceInfo serviceInfo = services.get(i).serviceInfo;
            if (serviceInfo == null) {
                Slog.e(TAG, "Failed to find serviceInfo for OdpPrivacyStatus service.");
                return null;
            }
            // There should only be one matching service inside the given package.
            // If there's more than one, return the first one found.
            return new ComponentName(serviceInfo.packageName, serviceInfo.name);
        }
        Slog.e(TAG, "Didn't find any matching OdpPrivacyStatus service.");
        return null;
    }
}

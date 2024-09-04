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

package android.adservices.ondevicepersonalization;

import static android.adservices.ondevicepersonalization.OnDevicePersonalizationPermissions.MODIFY_ONDEVICEPERSONALIZATION_STATE;

import android.adservices.ondevicepersonalization.aidl.IOnDevicePersonalizationConfigService;
import android.adservices.ondevicepersonalization.aidl.IOnDevicePersonalizationConfigServiceCallback;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.content.Context;
import android.os.Binder;
import android.os.OutcomeReceiver;

import com.android.adservices.ondevicepersonalization.flags.Flags;
import com.android.federatedcompute.internal.util.AbstractServiceBinder;
import com.android.internal.annotations.VisibleForTesting;
import com.android.ondevicepersonalization.internal.util.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * OnDevicePersonalizationConfigManager provides system APIs
 * for privileged APKs to control OnDevicePersonalization's enablement status.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ON_DEVICE_PERSONALIZATION_APIS_ENABLED)
public class OnDevicePersonalizationConfigManager {
    /** @hide */
    public static final String ON_DEVICE_PERSONALIZATION_CONFIG_SERVICE =
            "on_device_personalization_config_service";
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getLogger();
    private static final String TAG = OnDevicePersonalizationConfigManager.class.getSimpleName();

    private static final String ODP_CONFIG_SERVICE_PACKAGE_SUFFIX =
            "com.android.ondevicepersonalization.services";

    private static final String ALT_ODP_CONFIG_SERVICE_PACKAGE_SUFFIX =
            "com.google.android.ondevicepersonalization.services";
    private static final String ODP_CONFIG_SERVICE_INTENT =
            "android.OnDevicePersonalizationConfigService";

    private final AbstractServiceBinder<IOnDevicePersonalizationConfigService> mServiceBinder;

    /** @hide */
    public OnDevicePersonalizationConfigManager(@NonNull Context context) {
        this(
                AbstractServiceBinder.getServiceBinderByIntent(
                        context,
                        ODP_CONFIG_SERVICE_INTENT,
                        List.of(
                                ODP_CONFIG_SERVICE_PACKAGE_SUFFIX,
                                ALT_ODP_CONFIG_SERVICE_PACKAGE_SUFFIX),
                        IOnDevicePersonalizationConfigService.Stub::asInterface));
    }

    /** @hide */
    @VisibleForTesting
    public OnDevicePersonalizationConfigManager(
            AbstractServiceBinder<IOnDevicePersonalizationConfigService> serviceBinder) {
        this.mServiceBinder = serviceBinder;
    }

    /**
     * API users are expected to call this to modify personalization status for
     * On Device Personalization. The status is persisted both in memory and to the disk.
     * When reboot, the in-memory status will be restored from the disk.
     * Personalization is disabled by default.
     *
     * @param enabled boolean whether On Device Personalization should be enabled.
     * @param executor The {@link Executor} on which to invoke the callback.
     * @param receiver This either returns null on success or {@link Exception} on failure.
     *
     *     In case of an error, the receiver returns one of the following exceptions:
     *     Returns an {@link IllegalStateException} if the callback is unable to send back results.
     *     Returns a {@link SecurityException} if the caller is unauthorized to modify
     *     personalization status.
     */
    @RequiresPermission(MODIFY_ONDEVICEPERSONALIZATION_STATE)
    public void setPersonalizationEnabled(boolean enabled,
                                          @NonNull @CallbackExecutor Executor executor,
                                          @NonNull OutcomeReceiver<Void, Exception> receiver) {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            IOnDevicePersonalizationConfigService service = mServiceBinder.getService(executor);
            service.setPersonalizationStatus(enabled,
                    new IOnDevicePersonalizationConfigServiceCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            final long token = Binder.clearCallingIdentity();
                            try {
                                executor.execute(() -> {
                                    receiver.onResult(null);
                                    latch.countDown();
                                });
                            } finally {
                                Binder.restoreCallingIdentity(token);
                            }
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            final long token = Binder.clearCallingIdentity();
                            try {
                                executor.execute(() -> {
                                    sLogger.w(TAG + ": Unexpected failure from ODP"
                                            + "config service with error code: " + errorCode);
                                    receiver.onError(
                                            new IllegalStateException("Unexpected failure."));
                                    latch.countDown();
                                });
                            } finally {
                                Binder.restoreCallingIdentity(token);
                            }
                        }
                    });
        } catch (IllegalArgumentException | NullPointerException e) {
            latch.countDown();
            throw e;
        } catch (SecurityException e) {
            sLogger.w(TAG + ": Unauthorized call to ODP config service.");
            receiver.onError(e);
            latch.countDown();
        } catch (Exception e) {
            sLogger.w(TAG + ": Unexpected exception during call to ODP config service.");
            receiver.onError(e);
            latch.countDown();
        } finally {
            try {
                latch.await();
            } catch (InterruptedException e) {
                sLogger.e(TAG + ": Failed to set personalization.", e);
                receiver.onError(e);
            }
            unbindFromService();
        }
    }

    /**
     * Unbind from config service.
     */
    private void unbindFromService() {
        mServiceBinder.unbindFromService();
    }
}

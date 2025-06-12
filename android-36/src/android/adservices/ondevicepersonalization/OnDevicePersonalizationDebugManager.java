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

import android.adservices.ondevicepersonalization.aidl.IOnDevicePersonalizationDebugService;
import android.content.Context;
import android.os.RemoteException;

import com.android.federatedcompute.internal.util.AbstractServiceBinder;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * Provides APIs to support testing.
 * @hide
 */
public class OnDevicePersonalizationDebugManager {

    private static final String INTENT_FILTER_ACTION =
            "android.OnDevicePersonalizationDebugService";
    private static final String ODP_MANAGING_SERVICE_PACKAGE_SUFFIX =
            "com.android.ondevicepersonalization.services";

    private static final String ALT_ODP_MANAGING_SERVICE_PACKAGE_SUFFIX =
            "com.google.android.ondevicepersonalization.services";

    private final AbstractServiceBinder<IOnDevicePersonalizationDebugService> mServiceBinder;

    public OnDevicePersonalizationDebugManager(Context context) {
        mServiceBinder =
                AbstractServiceBinder.getServiceBinderByIntent(
                        context,
                        INTENT_FILTER_ACTION,
                        List.of(
                                ODP_MANAGING_SERVICE_PACKAGE_SUFFIX,
                                ALT_ODP_MANAGING_SERVICE_PACKAGE_SUFFIX),
                        IOnDevicePersonalizationDebugService.Stub::asInterface);
    }

    /** Returns whether the service is enabled. */
    public Boolean isEnabled() {
        try {
            IOnDevicePersonalizationDebugService service = Objects.requireNonNull(
                    mServiceBinder.getService(Executors.newSingleThreadExecutor()));
            boolean result = service.isEnabled();
            mServiceBinder.unbindFromService();
            return result;
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }
}

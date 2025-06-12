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

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.content.Context;
import android.os.OutcomeReceiver;

import java.util.concurrent.Executor;

/**
 * OnDevicePersonalizationConfigManager provides system APIs
 * for privileged APKs to control OnDevicePersonalization's enablement status.
 *
 * @hide
 */
@SystemApi
public class OnDevicePersonalizationConfigManager {
    /** @hide */
    public static final String ON_DEVICE_PERSONALIZATION_CONFIG_SERVICE =
            "on_device_personalization_config_service";

    /** @hide */
    public OnDevicePersonalizationConfigManager(@NonNull Context context) {}

    /**
     * Deprecated. This API is a no-op. ODP automatically determines whether personalization
     * should be enabled using
     * {@link android.adservices.common.AdServicesCommonManager}.
     */
    @RequiresPermission(MODIFY_ONDEVICEPERSONALIZATION_STATE)
    public void setPersonalizationEnabled(boolean enabled,
                                          @NonNull @CallbackExecutor Executor executor,
                                          @NonNull OutcomeReceiver<Void, Exception> receiver) {
        executor.execute(() -> receiver.onResult(null));
    }
}

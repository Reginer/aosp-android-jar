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

import static android.app.ondevicepersonalization.OnDevicePersonalizationSystemServiceManager.ON_DEVICE_PERSONALIZATION_SYSTEM_SERVICE;
import static android.ondevicepersonalization.OnDevicePersonalizationManager.ON_DEVICE_PERSONALIZATION_SERVICE;
import static android.ondevicepersonalization.OnDevicePersonalizationPrivacyStatusManager.ON_DEVICE_PERSONALIZATION_PRIVACY_STATUS_SERVICE;

import android.annotation.SystemApi;
import android.app.SystemServiceRegistry;
import android.app.ondevicepersonalization.OnDevicePersonalizationSystemServiceManager;
import android.content.Context;

import com.android.modules.utils.build.SdkLevel;

/**
 * Class holding initialization code for the OnDevicePersonalization module.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public class OnDevicePersonalizationFrameworkInitializer {
    private OnDevicePersonalizationFrameworkInitializer() {
    }

    /**
     * Called by {@link SystemServiceRegistry}'s static initializer and registers all
     * OnDevicePersonalization services to {@link Context}, so that
     * {@link Context#getSystemService} can return them.
     *
     * @throws IllegalStateException if this is called from anywhere besides
     *     {@link SystemServiceRegistry}
     */
    public static void registerServiceWrappers() {
        SystemServiceRegistry.registerContextAwareService(
                ON_DEVICE_PERSONALIZATION_SERVICE, OnDevicePersonalizationManager.class,
                (c) -> new OnDevicePersonalizationManager(c));
        SystemServiceRegistry.registerContextAwareService(
                ON_DEVICE_PERSONALIZATION_PRIVACY_STATUS_SERVICE,
                OnDevicePersonalizationPrivacyStatusManager.class,
                (c) -> new OnDevicePersonalizationPrivacyStatusManager(c));

        if (SdkLevel.isAtLeastU()) {
            SystemServiceRegistry.registerStaticService(
                    ON_DEVICE_PERSONALIZATION_SYSTEM_SERVICE,
                    OnDevicePersonalizationSystemServiceManager.class,
                    (s) -> new OnDevicePersonalizationSystemServiceManager(s));
        }
    }
}

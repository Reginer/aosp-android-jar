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

package android.adservices;

import static android.adservices.adid.AdIdManager.ADID_SERVICE;
import static android.adservices.adselection.AdSelectionManager.AD_SELECTION_SERVICE;
import static android.adservices.appsetid.AppSetIdManager.APPSETID_SERVICE;
import static android.adservices.common.AdServicesCommonManager.AD_SERVICES_COMMON_SERVICE;
import static android.adservices.customaudience.CustomAudienceManager.CUSTOM_AUDIENCE_SERVICE;
import static android.adservices.measurement.MeasurementManager.MEASUREMENT_SERVICE;
import static android.adservices.signals.ProtectedSignalsManager.PROTECTED_SIGNALS_SERVICE;
import static android.adservices.topics.TopicsManager.TOPICS_SERVICE;

import android.adservices.adid.AdIdManager;
import android.adservices.adselection.AdSelectionManager;
import android.adservices.appsetid.AppSetIdManager;
import android.adservices.common.AdServicesCommonManager;
import android.adservices.customaudience.CustomAudienceManager;
import android.adservices.measurement.MeasurementManager;
import android.adservices.signals.ProtectedSignalsManager;
import android.adservices.topics.TopicsManager;
import android.annotation.SystemApi;
import android.app.SystemServiceRegistry;
import android.app.adservices.AdServicesManager;
import android.app.adservices.IAdServicesManager;
import android.app.sdksandbox.SdkSandboxSystemServiceRegistry;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.android.adservices.LogUtil;

/**
 * Class holding initialization code for the AdServices module.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.S)
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public class AdServicesFrameworkInitializer {
    private AdServicesFrameworkInitializer() {
    }

    /**
     * Called by {@link SystemServiceRegistry}'s static initializer and registers all
     * AdServices services to {@link Context}, so that
     * {@link Context#getSystemService} can return them.
     *
     * @throws IllegalStateException if this is called from anywhere besides
     *     {@link SystemServiceRegistry}
     */
    public static void registerServiceWrappers() {
        LogUtil.d("Registering AdServices's TopicsManager.");
        SystemServiceRegistry.registerContextAwareService(
                TOPICS_SERVICE, TopicsManager.class,
                (c) -> new TopicsManager(c));
        // TODO(b/242889021): don't use this workaround on devices that have proper fix
        SdkSandboxSystemServiceRegistry.getInstance()
                .registerServiceMutator(
                        TOPICS_SERVICE,
                        (service, ctx) -> ((TopicsManager) service).initialize(ctx));

        LogUtil.d("Registering AdServices's CustomAudienceManager.");
        SystemServiceRegistry.registerContextAwareService(
                CUSTOM_AUDIENCE_SERVICE, CustomAudienceManager.class, CustomAudienceManager::new);
        // TODO(b/242889021): don't use this workaround on devices that have proper fix
        SdkSandboxSystemServiceRegistry.getInstance()
                .registerServiceMutator(
                        CUSTOM_AUDIENCE_SERVICE,
                        (service, ctx) -> ((CustomAudienceManager) service).initialize(ctx));

        LogUtil.d("Registering AdServices's AdSelectionManager.");
        SystemServiceRegistry.registerContextAwareService(
                AD_SELECTION_SERVICE, AdSelectionManager.class, AdSelectionManager::new);
        // TODO(b/242889021): don't use this workaround on devices that have proper fix
        SdkSandboxSystemServiceRegistry.getInstance()
                .registerServiceMutator(
                        AD_SELECTION_SERVICE,
                        (service, ctx) -> ((AdSelectionManager) service).initialize(ctx));

        LogUtil.d("Registering AdServices's ProtectedSignalsManager.");
        SystemServiceRegistry.registerContextAwareService(
                PROTECTED_SIGNALS_SERVICE,
                ProtectedSignalsManager.class,
                ProtectedSignalsManager::new);
        // TODO(b/242889021): don't use this workaround on devices that have proper fix
        SdkSandboxSystemServiceRegistry.getInstance()
                .registerServiceMutator(
                        PROTECTED_SIGNALS_SERVICE,
                        (service, ctx) -> ((ProtectedSignalsManager) service).initialize(ctx));

        LogUtil.d("Registering AdServices's MeasurementManager.");
        SystemServiceRegistry.registerContextAwareService(
                MEASUREMENT_SERVICE, MeasurementManager.class, MeasurementManager::new);
        // TODO(b/242889021): don't use this workaround on devices that have proper fix
        SdkSandboxSystemServiceRegistry.getInstance()
                .registerServiceMutator(
                        MEASUREMENT_SERVICE,
                        (service, ctx) -> ((MeasurementManager) service).initialize(ctx));

        LogUtil.d("Registering AdServices's AdIdManager.");
        SystemServiceRegistry.registerContextAwareService(
                ADID_SERVICE, AdIdManager.class, (c) -> new AdIdManager(c));
        // TODO(b/242889021): don't use this workaround on devices that have proper fix
        SdkSandboxSystemServiceRegistry.getInstance()
                .registerServiceMutator(
                        ADID_SERVICE, (service, ctx) -> ((AdIdManager) service).initialize(ctx));

        LogUtil.d("Registering AdServices's AppSetIdManager.");
        SystemServiceRegistry.registerContextAwareService(
                APPSETID_SERVICE, AppSetIdManager.class, (c) -> new AppSetIdManager(c));
        // TODO(b/242889021): don't use this workaround on devices that have proper fix
        SdkSandboxSystemServiceRegistry.getInstance()
                .registerServiceMutator(
                        APPSETID_SERVICE,
                        (service, ctx) -> ((AppSetIdManager) service).initialize(ctx));

        LogUtil.d("Registering AdServices's AdServicesCommonManager.");
        SystemServiceRegistry.registerContextAwareService(AD_SERVICES_COMMON_SERVICE,
                AdServicesCommonManager.class,
                (c) -> new AdServicesCommonManager(c));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            LogUtil.d("Registering Adservices's AdServicesManager.");
            SystemServiceRegistry.registerContextAwareService(
                    AdServicesManager.AD_SERVICES_SYSTEM_SERVICE,
                    AdServicesManager.class,
                    (context, service) ->
                            new AdServicesManager(IAdServicesManager.Stub.asInterface(service)));
        }
    }
}

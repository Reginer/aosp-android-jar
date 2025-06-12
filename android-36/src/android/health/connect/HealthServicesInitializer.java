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

package android.health.connect;

import android.annotation.SystemApi;
import android.app.SystemServiceRegistry;
import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.HealthPermissions;
import android.health.connect.aidl.IHealthConnectService;

import com.android.healthfitness.flags.Flags;

/**
 * Class for performing registration for Health services.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public class HealthServicesInitializer {
    private HealthServicesInitializer() {}

    /**
     * Called by {@link SystemServiceRegistry}'s static initializer and registers all Health
     * services to {@link Context}, so that {@link Context#getSystemService} can return them.
     *
     * @throws IllegalStateException if this is called from anywhere besides {@link
     *     SystemServiceRegistry}
     */
    public static void registerServiceWrappers() {
        SystemServiceRegistry.registerContextAwareService(
                Context.HEALTHCONNECT_SERVICE,
                HealthConnectManager.class,
                (context, serviceBinder) -> {
                    if (!shouldReturnHealthConnectManager(context)) {
                        return null;
                    }

                    IHealthConnectService service =
                            IHealthConnectService.Stub.asInterface(serviceBinder);
                    return new HealthConnectManager(context, service);
                });
    }

    private static boolean shouldReturnHealthConnectManager(Context context) {
        PackageManager pm = context.getPackageManager();
        // Not available on embedded/tv/auto.
        if (pm.hasSystemFeature(PackageManager.FEATURE_EMBEDDED)
                || pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                || pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            return false;
        }
        // Only available on Wear for permission management.
        if (pm.hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            return Flags.replaceBodySensorPermissionEnabled()
                    && context.checkSelfPermission(HealthPermissions.MANAGE_HEALTH_PERMISSIONS)
                            == PackageManager.PERMISSION_GRANTED;
        }
        // Supported everywhere else.
        return true;
    }
}
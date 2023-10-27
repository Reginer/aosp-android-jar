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

package android.system.virtualmachine;

import static android.content.pm.PackageManager.FEATURE_VIRTUALIZATION_FRAMEWORK;

import android.annotation.SystemApi;
import android.app.SystemServiceRegistry;
import android.content.Context;

/**
 * Holds initialization code for virtualization module
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public class VirtualizationFrameworkInitializer {

    private VirtualizationFrameworkInitializer() {}

    /**
     * Called by the static initializer in the {@link SystemServiceRegistry}, and registers {@link
     * VirtualMachineManager} to the {@link Context}. so that it's accessible from {@link
     * Context#getSystemService(String)}.
     */
    public static void registerServiceWrappers() {
        // Note: it's important that the getPackageManager().hasSystemFeature() check is executed
        // in the lambda, and not directly in the registerServiceWrappers method. The
        // registerServiceWrappers is called during Zygote static initialization, and at that
        // point the PackageManager is not available yet.
        //
        // On the other hand, the lambda is executed after the app calls Context.getSystemService
        // (VirtualMachineManager.class), at which point the PackageManager is available. The
        // result of the lambda is cached on per-context basis.
        SystemServiceRegistry.registerContextAwareService(
                Context.VIRTUALIZATION_SERVICE,
                VirtualMachineManager.class,
                ctx ->
                        ctx.getPackageManager().hasSystemFeature(FEATURE_VIRTUALIZATION_FRAMEWORK)
                                ? new VirtualMachineManager(ctx)
                                : null);
    }
}

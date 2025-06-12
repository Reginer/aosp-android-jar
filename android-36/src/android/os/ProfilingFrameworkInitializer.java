/*
 * Copyright (C) 2024 The Android Open Source Project
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
package android.os;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.annotation.SystemApi.Client;
import android.app.SystemServiceRegistry;
import android.content.Context;
import android.os.profiling.Flags;

/**
 * Class for performing registration for profiling service.
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_TELEMETRY_APIS)
@SystemApi(client = Client.MODULE_LIBRARIES)
public class ProfilingFrameworkInitializer {

    private ProfilingFrameworkInitializer() {}

    private static volatile ProfilingServiceManager sProfilingServiceManager;

    /**
     * Sets an instance of {@link ProfilingServiceManager} that allows the profiling module to
     * register/obtain profiling binder services. This is called by the platform during the system
     * initialization.
     *
     * @param profilingServiceManager instance of {@link ProfilingServiceManager} that allows the
     * profiling module to register/obtain profiling binder services.
     */
    public static void setProfilingServiceManager(
            @NonNull ProfilingServiceManager profilingServiceManager) {
        if (sProfilingServiceManager != null) {
            throw new IllegalStateException("setProfilingServiceManager called twice!");
        }

        if (profilingServiceManager == null) {
            throw new NullPointerException("profilingServiceManager is null");
        }

        sProfilingServiceManager = profilingServiceManager;
    }

    /** @hide */
    public static ProfilingServiceManager getProfilingServiceManager() {
        return sProfilingServiceManager;
    }

    /**
     * Called by {@link SystemServiceRegistry}'s static initializer and registers profiling
     * services to {@link Context}, so that {@link Context#getSystemService} can return them.
     *
     * @throws IllegalStateException if this is called from anywhere besides
     * {@link SystemServiceRegistry}
     */
    public static void registerServiceWrappers() {
        SystemServiceRegistry.registerContextAwareService(
                Context.PROFILING_SERVICE,
                ProfilingManager.class,
                context -> new ProfilingManager(context)
        );
    }
}

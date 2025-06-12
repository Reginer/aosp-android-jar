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

package android.os.flagging;

import android.annotation.FlaggedApi;
import android.annotation.SystemApi;
import android.app.SystemServiceRegistry;
import android.provider.flags.Flags;

/**
 * Initializes framework services.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
@FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
public final class ConfigInfrastructureFrameworkInitializer {
    /** Prevent instantiation. */
    private ConfigInfrastructureFrameworkInitializer() {}

    /**
     * Called by {@link SystemServiceRegistry}'s static initializer and registers
     * {@link FlagManager} to {@link Context}, so that {@link Context#getSystemService} can return
     * it.
     *
     * <p>If this is called from other places, it throws a {@link IllegalStateException).
     *
     */
    @FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
    public static void registerServiceWrappers() {
        SystemServiceRegistry.registerContextAwareService(
                FlagManager.FLAG_SERVICE_NAME,
                FlagManager.class,
                (context) -> new FlagManager(context));
    }
}

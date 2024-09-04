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

package android.app.ecm;

import android.annotation.FlaggedApi;
import android.annotation.SystemApi;
import android.annotation.TargetApi;
import android.app.SystemServiceRegistry;
import android.content.Context;
import android.os.Build;
import android.permission.flags.Flags;

/**
 * Class holding initialization code for enhanced confirmation code in the permission module.
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_ENHANCED_CONFIRMATION_MODE_APIS_ENABLED)
@TargetApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public class EnhancedConfirmationFrameworkInitializer {
    private EnhancedConfirmationFrameworkInitializer() {}

    /**
     * Called by {@link SystemServiceRegistry}'s static initializer and registers
     * {@link EnhancedConfirmationManager} to {@link Context}, so that
     * {@link Context#getSystemService} can return it.
     *
     * <p>If this is called from other places, it throws a {@link IllegalStateException}.
     */
    public static void registerServiceWrappers() {
        SystemServiceRegistry.registerContextAwareService(Context.ECM_ENHANCED_CONFIRMATION_SERVICE,
                EnhancedConfirmationManager.class,
                (context, serviceBinder) -> new EnhancedConfirmationManager(context,
                        IEnhancedConfirmationManager.Stub.asInterface(serviceBinder)));
    }
}

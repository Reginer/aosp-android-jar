/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.safetycenter;

import static android.os.Build.VERSION_CODES.TIRAMISU;

import android.annotation.SystemApi;
import android.app.SystemServiceRegistry;
import android.content.Context;

import androidx.annotation.RequiresApi;

/**
 * Class holding initialization code for the safety center in the permission module.
 *
 * @hide
 */
@RequiresApi(TIRAMISU)
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class SafetyCenterFrameworkInitializer {

    private SafetyCenterFrameworkInitializer() {}

    /**
     * Called by {@link SystemServiceRegistry}'s static initializer and registers {@link
     * SafetyCenterManager} to {@link Context}, so that {@link Context#getSystemService} can return
     * it.
     *
     * <p>If this is called from other places, it throws a {@link IllegalStateException}.
     */
    public static void registerServiceWrappers() {
        SystemServiceRegistry.registerContextAwareService(
                Context.SAFETY_CENTER_SERVICE,
                SafetyCenterManager.class,
                (context, serviceBinder) ->
                        new SafetyCenterManager(
                                context, ISafetyCenterManager.Stub.asInterface(serviceBinder)));
    }
}

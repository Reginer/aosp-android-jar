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

package android.app.sdksandbox.sandboxactivity;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * Provides information required for building the sandbox activity {@link Context}.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public interface ActivityContextInfo {
    /**
     * Returns the {@link ApplicationInfo} of the SDK which initially requested the {@link Activity}
     * .
     */
    @NonNull
    ApplicationInfo getSdkApplicationInfo();

    /** The flags which should be used to build the sandbox {@link Activity} context. */
    default int getContextFlags() {
        return Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY;
    }
}

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

package dalvik.system;

import libcore.util.NonNull;

/**
 * Provides APIs to retrieve the information about the application loaded in the current runtime.
 */
public final class ApplicationRuntime {
    private ApplicationRuntime() {}

    /**
     * Returns the optimization status of the base APK loaded in this process. If no base APK has
     * been loaded in this process, this methods returns an object, but the contents are undefined.
     */
    public static @NonNull DexFile.OptimizationInfo getBaseApkOptimizationInfo() {
        return VMRuntime.getBaseApkOptimizationInfo();
    }
}

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

package com.android.net.module.util;

/**
 * Utilities for modules to use jni.
 */
public final class JniUtil {
    /**
     * The method to find jni library accroding to the giving package name.
     *
     * The jni library name would be packageName + _jni.so. E.g.
     * com_android_networkstack_tethering_util_jni for tethering,
     * com_android_connectivity_util_jni for connectivity.
     */
    public static String getJniLibraryName(final Package pkg) {
        final String libPrefix = pkg.getName().replaceAll("\\.", "_");

        return libPrefix + "_jni";
    }
}

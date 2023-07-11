/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.modules.utils.build;

import static android.os.Build.VERSION.CODENAME;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.CUR_DEVELOPMENT;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.NonNull;

/**
 * Utility class to check SDK level on a device.
 *
 * @hide
 */
public final class SdkLevel {

    private SdkLevel() {
    }

    /** Checks if the device is running on a release version of Android R or newer. */
    @ChecksSdkIntAtLeast(api = 30 /* Build.VERSION_CODES.R */)
    public static boolean isAtLeastR() {
        return SDK_INT >= 30;
    }

    /** Checks if the device is running on a release version of Android S or newer. */
    @ChecksSdkIntAtLeast(api = 31 /* Build.VERSION_CODES.S */)
    public static boolean isAtLeastS() {
        return SDK_INT >= 31;
    }

    /** Checks if the device is running on a release version of Android S_V2 or newer */
    @ChecksSdkIntAtLeast(api = 32 /* BUILD.VERSION_CODES.Sv2 */)
    public static boolean isAtLeastSv2() {
        return SDK_INT >= 32;
    }

    /** Checks if the device is running on a release version of Android Tiramisu or newer */
    @ChecksSdkIntAtLeast(api = 33 /* BUILD_VERSION_CODES.Tiramisu */)
    public static boolean isAtLeastT() {
        return SDK_INT >= 33;
    }

    private static boolean isAtLeastPreReleaseCodename(@NonNull String codename) {
        // Special case "REL", which means the build is not a pre-release build.
        if ("REL".equals(CODENAME)) {
            return false;
        }

        // Otherwise lexically compare them. Return true if the build codename is equal to or
        // greater than the requested codename.
        return CODENAME.compareTo(codename) >= 0;
    }
}

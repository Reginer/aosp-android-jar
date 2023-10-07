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

package com.android.modules.utils.pm;

import android.annotation.NonNull;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.android.server.pm.pkg.AndroidPackageSplit;
import com.android.server.pm.pkg.PackageState;

import java.util.List;

public class PackageStateModulesUtils {
    private PackageStateModulesUtils() {}

    /**
     * @return True if the package is dexoptable.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static boolean isDexoptable(@NonNull PackageState packageState) {
        if (packageState.isApex() || "android".equals(packageState.getPackageName())
                || packageState.getAppId() <= 0) {
            return false;
        }

        var pkg = packageState.getAndroidPackage();
        if (pkg == null) {
            return false;
        }

        List<AndroidPackageSplit> splits = pkg.getSplits();
        for (int index = 0; index < splits.size(); index++) {
            if (splits.get(index).isHasCode()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether a package is loadable in other processes. This doesn't guarantee that it
     * will be loaded, just that it can be. Note that this relies on the result of
     * {@link PackageState#getAndroidPackage()}, so it will change between mount/unmount of a
     * package.
     *
     * @param codeOnly Whether to filter to only code usages, ignoring resource only usages.
     * @return True if the package is loadable.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static boolean isLoadableInOtherProcesses(
            @NonNull PackageState packageState, boolean codeOnly) {
        var pkg = packageState.getAndroidPackage();
        if (pkg == null) {
            return false;
        }

        if (!pkg.getLibraryNames().isEmpty()) {
            return true;
        }

        if (!TextUtils.isEmpty(pkg.getSdkLibraryName())) {
            return true;
        }

        if (!TextUtils.isEmpty(pkg.getStaticSharedLibraryName())) {
            return true;
        }

        return !codeOnly && pkg.isResourceOverlay();
    }
}

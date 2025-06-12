/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.app;

import android.compat.annotation.UnsupportedAppUsage;
import android.content.pm.IPackageManager;
import android.permission.IPermissionManager;

/**
 * Special private access for certain globals related to a process.
 * @hide
 */
public class AppGlobals {
    /**
     * Return the first Application object made in the process.
     * NOTE: Only works on the main thread.
     */
    @UnsupportedAppUsage
    public static Application getInitialApplication() {
        return ActivityThread.currentApplication();
    }
    
    /**
     * Return the package name of the first .apk loaded into the process.
     * NOTE: Only works on the main thread.
     */
    @UnsupportedAppUsage
    public static String getInitialPackage() {
        return ActivityThread.currentPackageName();
    }

    /**
     * Return the raw interface to the package manager.
     * @return The package manager.
     */
    @UnsupportedAppUsage
    public static IPackageManager getPackageManager() {
        return ActivityThread.getPackageManager();
    }

    /**
     * Return the raw interface to the permission manager.
     * @return The permission manager.
     */
    public static IPermissionManager getPermissionManager() {
        return ActivityThread.getPermissionManager();
    }

    /**
     * Gets the value of an integer core setting.
     *
     * @param key The setting key.
     * @param defaultValue The setting default value.
     * @return The core settings.
     */
    public static int getIntCoreSetting(String key, int defaultValue) {
        ActivityThread currentActivityThread = ActivityThread.currentActivityThread();
        if (currentActivityThread != null) {
            return currentActivityThread.getIntCoreSetting(key, defaultValue);
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the value of a float core setting.
     *
     * @param key The setting key.
     * @param defaultValue The setting default value.
     * @return The core settings.
     */
    public static float getFloatCoreSetting(String key, float defaultValue) {
        ActivityThread currentActivityThread = ActivityThread.currentActivityThread();
        if (currentActivityThread != null) {
            return currentActivityThread.getFloatCoreSetting(key, defaultValue);
        } else {
            return defaultValue;
        }
    }
}

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

package com.android.net.module.util;

import android.content.Context;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.DeviceConfig;
import android.util.Log;

import androidx.annotation.BoolRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Utilities for modules to query {@link DeviceConfig} and flags.
 */
public final class DeviceConfigUtils {
    private DeviceConfigUtils() {}

    private static final String TAG = DeviceConfigUtils.class.getSimpleName();
    /**
     * DO NOT MODIFY: this may be used by multiple modules that will not see the updated value
     * until they are recompiled, so modifying this constant means that different modules may
     * be referencing a different tethering module variant, or having a stale reference.
     */
    public static final String TETHERING_MODULE_NAME = "com.android.tethering";

    @VisibleForTesting
    public static void resetPackageVersionCacheForTest() {
        sPackageVersion = -1;
        sModuleVersion = -1;
    }

    private static volatile long sPackageVersion = -1;
    private static long getPackageVersion(@NonNull final Context context)
            throws PackageManager.NameNotFoundException {
        // sPackageVersion may be set by another thread just after this check, but querying the
        // package version several times on rare occasions is fine.
        if (sPackageVersion >= 0) {
            return sPackageVersion;
        }
        final long version = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0).getLongVersionCode();
        sPackageVersion = version;
        return version;
    }

    /**
     * Look up the value of a property for a particular namespace from {@link DeviceConfig}.
     * @param namespace The namespace containing the property to look up.
     * @param name The name of the property to look up.
     * @param defaultValue The value to return if the property does not exist or has no valid value.
     * @return the corresponding value, or defaultValue if none exists.
     */
    @Nullable
    public static String getDeviceConfigProperty(@NonNull String namespace, @NonNull String name,
            @Nullable String defaultValue) {
        String value = DeviceConfig.getProperty(namespace, name);
        return value != null ? value : defaultValue;
    }

    /**
     * Look up the value of a property for a particular namespace from {@link DeviceConfig}.
     * @param namespace The namespace containing the property to look up.
     * @param name The name of the property to look up.
     * @param defaultValue The value to return if the property does not exist or its value is null.
     * @return the corresponding value, or defaultValue if none exists.
     */
    public static int getDeviceConfigPropertyInt(@NonNull String namespace, @NonNull String name,
            int defaultValue) {
        String value = getDeviceConfigProperty(namespace, name, null /* defaultValue */);
        try {
            return (value != null) ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Look up the value of a property for a particular namespace from {@link DeviceConfig}.
     *
     * Flags like timeouts should use this method and set an appropriate min/max range: if invalid
     * values like "0" or "1" are pushed to devices, everything would timeout. The min/max range
     * protects against this kind of breakage.
     * @param namespace The namespace containing the property to look up.
     * @param name The name of the property to look up.
     * @param minimumValue The minimum value of a property.
     * @param maximumValue The maximum value of a property.
     * @param defaultValue The value to return if the property does not exist or its value is null.
     * @return the corresponding value, or defaultValue if none exists or the fetched value is
     *         not in the provided range.
     */
    public static int getDeviceConfigPropertyInt(@NonNull String namespace, @NonNull String name,
            int minimumValue, int maximumValue, int defaultValue) {
        int value = getDeviceConfigPropertyInt(namespace, name, defaultValue);
        if (value < minimumValue || value > maximumValue) return defaultValue;
        return value;
    }

    /**
     * Look up the value of a property for a particular namespace from {@link DeviceConfig}.
     * @param namespace The namespace containing the property to look up.
     * @param name The name of the property to look up.
     * @param defaultValue The value to return if the property does not exist or its value is null.
     * @return the corresponding value, or defaultValue if none exists.
     */
    public static boolean getDeviceConfigPropertyBoolean(@NonNull String namespace,
            @NonNull String name, boolean defaultValue) {
        String value = getDeviceConfigProperty(namespace, name, null /* defaultValue */);
        return (value != null) ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Check whether or not one specific experimental feature for a particular namespace from
     * {@link DeviceConfig} is enabled by comparing module package version
     * with current version of property. If this property version is valid, the corresponding
     * experimental feature would be enabled, otherwise disabled.
     *
     * This is useful to ensure that if a module install is rolled back, flags are not left fully
     * rolled out on a version where they have not been well tested.
     * @param context The global context information about an app environment.
     * @param namespace The namespace containing the property to look up.
     * @param name The name of the property to look up.
     * @return true if this feature is enabled, or false if disabled.
     */
    public static boolean isFeatureEnabled(@NonNull Context context, @NonNull String namespace,
            @NonNull String name) {
        return isFeatureEnabled(context, namespace, name, false /* defaultEnabled */);
    }

    /**
     * Check whether or not one specific experimental feature for a particular namespace from
     * {@link DeviceConfig} is enabled by comparing module package version
     * with current version of property. If this property version is valid, the corresponding
     * experimental feature would be enabled, otherwise disabled.
     *
     * This is useful to ensure that if a module install is rolled back, flags are not left fully
     * rolled out on a version where they have not been well tested.
     * @param context The global context information about an app environment.
     * @param namespace The namespace containing the property to look up.
     * @param name The name of the property to look up.
     * @param defaultEnabled The value to return if the property does not exist or its value is
     *                       null.
     * @return true if this feature is enabled, or false if disabled.
     */
    public static boolean isFeatureEnabled(@NonNull Context context, @NonNull String namespace,
            @NonNull String name, boolean defaultEnabled) {
        try {
            final long packageVersion = getPackageVersion(context);
            return isFeatureEnabled(context, packageVersion, namespace, name, defaultEnabled);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find the package name", e);
            return false;
        }
    }

    /**
     * Check whether or not one specific experimental feature for a particular namespace from
     * {@link DeviceConfig} is enabled by comparing module package version
     * with current version of property. If this property version is valid, the corresponding
     * experimental feature would be enabled, otherwise disabled.
     *
     * This is useful to ensure that if a module install is rolled back, flags are not left fully
     * rolled out on a version where they have not been well tested.
     * @param context The global context information about an app environment.
     * @param namespace The namespace containing the property to look up.
     * @param name The name of the property to look up.
     * @param moduleName The mainline module name which is released as apex.
     * @param defaultEnabled The value to return if the property does not exist or its value is
     *                       null.
     * @return true if this feature is enabled, or false if disabled.
     */
    public static boolean isFeatureEnabled(@NonNull Context context, @NonNull String namespace,
            @NonNull String name, @NonNull String moduleName, boolean defaultEnabled) {
        try {
            final long packageVersion = getModuleVersion(context, moduleName);
            return isFeatureEnabled(context, packageVersion, namespace, name, defaultEnabled);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find the module name", e);
            return false;
        }
    }

    private static boolean maybeUseFixedPackageVersion(@NonNull Context context) {
        final String packageName = context.getPackageName();
        if (packageName == null) return false;

        return packageName.equals("com.android.networkstack.tethering")
                || packageName.equals("com.android.networkstack.tethering.inprocess");
    }

    private static boolean isFeatureEnabled(@NonNull Context context, long packageVersion,
            @NonNull String namespace, String name, boolean defaultEnabled)
            throws PackageManager.NameNotFoundException {
        final int propertyVersion = getDeviceConfigPropertyInt(namespace, name,
                0 /* default value */);
        return (propertyVersion == 0 && defaultEnabled)
                || (propertyVersion != 0 && packageVersion >= (long) propertyVersion);
    }

    private static volatile long sModuleVersion = -1;
    @VisibleForTesting public static long FIXED_PACKAGE_VERSION = 10;
    private static long getModuleVersion(@NonNull Context context, @NonNull String moduleName)
            throws PackageManager.NameNotFoundException {
        if (sModuleVersion >= 0) return sModuleVersion;

        final PackageManager packageManager = context.getPackageManager();
        ModuleInfo module;
        try {
            module = packageManager.getModuleInfo(
                    moduleName, PackageManager.MODULE_APEX_NAME);
        } catch (PackageManager.NameNotFoundException e) {
            // The error may happen if mainline module meta data is not installed e.g. there are
            // no meta data configuration in AOSP build. To be able to enable a feature in AOSP
            // by setting a flag via ADB for example. set a small non-zero fixed number for
            // comparing.
            if (maybeUseFixedPackageVersion(context)) {
                sModuleVersion = FIXED_PACKAGE_VERSION;
                return FIXED_PACKAGE_VERSION;
            } else {
                throw e;
            }
        }
        String modulePackageName = module.getPackageName();
        if (modulePackageName == null) throw new PackageManager.NameNotFoundException(moduleName);
        final long version = packageManager.getPackageInfo(modulePackageName,
                PackageManager.MATCH_APEX).getLongVersionCode();
        sModuleVersion = version;

        return version;
    }

    /**
     * Gets boolean config from resources.
     */
    public static boolean getResBooleanConfig(@NonNull final Context context,
            @BoolRes int configResource, final boolean defaultValue) {
        final Resources res = context.getResources();
        try {
            return res.getBoolean(configResource);
        } catch (Resources.NotFoundException e) {
            return defaultValue;
        }
    }
}

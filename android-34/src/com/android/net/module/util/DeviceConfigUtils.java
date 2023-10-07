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

import static android.content.pm.PackageManager.MATCH_SYSTEM_ONLY;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.provider.DeviceConfig;
import android.util.Log;

import androidx.annotation.BoolRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

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
    public static final String RESOURCES_APK_INTENT =
            "com.android.server.connectivity.intent.action.SERVICE_CONNECTIVITY_RESOURCES_APK";
    private static final String CONNECTIVITY_RES_PKG_DIR = "/apex/" + TETHERING_MODULE_NAME + "/";

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
        // TODO: migrate callers to a non-generic isTetheringFeatureEnabled method.
        if (!TETHERING_MODULE_NAME.equals(moduleName)) {
            throw new IllegalArgumentException(
                    "This method is only usable by the tethering module");
        }
        try {
            final long packageVersion = getTetheringModuleVersion(context);
            return isFeatureEnabled(context, packageVersion, namespace, name, defaultEnabled);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find the module name", e);
            return false;
        }
    }

    private static boolean isFeatureEnabled(@NonNull Context context, long packageVersion,
            @NonNull String namespace, String name, boolean defaultEnabled)
            throws PackageManager.NameNotFoundException {
        final int propertyVersion = getDeviceConfigPropertyInt(namespace, name,
                0 /* default value */);
        return (propertyVersion == 0 && defaultEnabled)
                || (propertyVersion != 0 && packageVersion >= (long) propertyVersion);
    }

    // Guess the tethering module name based on the package prefix of the connectivity resources
    // Take the resource package name, cut it before "connectivity" and append "tethering".
    // Then resolve that package version number with packageManager.
    // If that fails retry by appending "go.tethering" instead
    private static long resolveTetheringModuleVersion(@NonNull Context context)
            throws PackageManager.NameNotFoundException {
        final String connResourcesPackage = getConnectivityResourcesPackageName(context);
        final int pkgPrefixLen = connResourcesPackage.indexOf("connectivity");
        if (pkgPrefixLen < 0) {
            throw new IllegalStateException(
                    "Invalid connectivity resources package: " + connResourcesPackage);
        }

        final String pkgPrefix = connResourcesPackage.substring(0, pkgPrefixLen);
        final PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getPackageInfo(pkgPrefix + "tethering",
                    PackageManager.MATCH_APEX).getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Device is using go modules");
            // fall through
        }

        return packageManager.getPackageInfo(pkgPrefix + "go.tethering",
                PackageManager.MATCH_APEX).getLongVersionCode();
    }

    private static volatile long sModuleVersion = -1;
    private static long getTetheringModuleVersion(@NonNull Context context)
            throws PackageManager.NameNotFoundException {
        if (sModuleVersion >= 0) return sModuleVersion;

        sModuleVersion = resolveTetheringModuleVersion(context);
        return sModuleVersion;
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

    /**
     * Gets int config from resources.
     */
    public static int getResIntegerConfig(@NonNull final Context context,
            @BoolRes int configResource, final int defaultValue) {
        final Resources res = context.getResources();
        try {
            return res.getInteger(configResource);
        } catch (Resources.NotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Get the package name of the ServiceConnectivityResources package, used to provide resources
     * for service-connectivity.
     */
    @NonNull
    public static String getConnectivityResourcesPackageName(@NonNull Context context) {
        final List<ResolveInfo> pkgs = new ArrayList<>(context.getPackageManager()
                .queryIntentActivities(new Intent(RESOURCES_APK_INTENT), MATCH_SYSTEM_ONLY));
        pkgs.removeIf(pkg -> !pkg.activityInfo.applicationInfo.sourceDir.startsWith(
                CONNECTIVITY_RES_PKG_DIR));
        if (pkgs.size() > 1) {
            Log.wtf(TAG, "More than one connectivity resources package found: " + pkgs);
        }
        if (pkgs.isEmpty()) {
            throw new IllegalStateException("No connectivity resource package found");
        }

        return pkgs.get(0).activityInfo.applicationInfo.packageName;
    }
}

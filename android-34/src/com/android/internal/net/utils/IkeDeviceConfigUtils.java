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
package com.android.internal.net.utils;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.provider.DeviceConfig;

/** Utilities to query {@link DeviceConfig} and flags. */
// TODO: Remove this class and statically include DeviceConfigUtils instead
public final class IkeDeviceConfigUtils {
    /**
     * Look up the value of a property for a particular namespace from {@link DeviceConfig}.
     *
     * @param namespace The namespace containing the property to look up.
     * @param name The name of the property to look up.
     * @param defaultValue The value to return if the property does not exist or its value is null.
     * @return the corresponding value, or defaultValue if none exists.
     */
    public static int getDeviceConfigPropertyInt(
            @NonNull String namespace, @NonNull String name, int defaultValue) {
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
     * <p>Flags like timeouts should use this method and set an appropriate min/max range: if
     * invalid values like "0" or "1" are pushed to devices, everything would timeout. The min/max
     * range protects against this kind of breakage.
     *
     * @param namespace The namespace containing the property to look up.
     * @param name The name of the property to look up.
     * @param minimumValue The minimum value of a property.
     * @param maximumValue The maximum value of a property.
     * @param defaultValue The value to return if the property does not exist or its value is null.
     * @return the corresponding value, or defaultValue if none exists or the fetched value is not
     *     in the provided range.
     */
    public static int getDeviceConfigPropertyInt(
            @NonNull String namespace,
            @NonNull String name,
            int minimumValue,
            int maximumValue,
            int defaultValue) {
        int value = getDeviceConfigPropertyInt(namespace, name, defaultValue);
        if (value < minimumValue || value > maximumValue) return defaultValue;
        return value;
    }
    /**
     * Look up the value of a property for a particular namespace from {@link DeviceConfig}.
     *
     * @param namespace The namespace containing the property to look up.
     * @param name The name of the property to look up.
     * @param defaultValue The value to return if the property does not exist or its value is null.
     * @return the corresponding value, or defaultValue if none exists.
     */
    public static boolean getDeviceConfigPropertyBoolean(
            @NonNull String namespace, @NonNull String name, boolean defaultValue) {
        String value = getDeviceConfigProperty(namespace, name, null /* defaultValue */);
        return (value != null) ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Look up the value of a property for a particular namespace from {@link DeviceConfig}.
     *
     * @param namespace The namespace containing the property to look up.
     * @param name The name of the property to look up.
     * @param defaultValue The value to return if the property does not exist or has no valid value.
     * @return the corresponding value, or defaultValue if none exists.
     */
    @Nullable
    public static String getDeviceConfigProperty(
            @NonNull String namespace, @NonNull String name, @Nullable String defaultValue) {
        String value = DeviceConfig.getProperty(namespace, name);
        return value != null ? value : defaultValue;
    }
}

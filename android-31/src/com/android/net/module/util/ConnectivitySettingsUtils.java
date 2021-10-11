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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * Collection of connectivity settings utilities.
 *
 * @hide
 */
public class ConnectivitySettingsUtils {
    public static final int PRIVATE_DNS_MODE_OFF = 1;
    public static final int PRIVATE_DNS_MODE_OPPORTUNISTIC = 2;
    public static final int PRIVATE_DNS_MODE_PROVIDER_HOSTNAME = 3;

    public static final String PRIVATE_DNS_DEFAULT_MODE = "private_dns_default_mode";
    public static final String PRIVATE_DNS_MODE = "private_dns_mode";
    public static final String PRIVATE_DNS_MODE_OFF_STRING = "off";
    public static final String PRIVATE_DNS_MODE_OPPORTUNISTIC_STRING = "opportunistic";
    public static final String PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING = "hostname";
    public static final String PRIVATE_DNS_SPECIFIER = "private_dns_specifier";

    /**
     * Get private DNS mode as string.
     *
     * @param mode One of the private DNS values.
     * @return A string of private DNS mode.
     */
    public static String getPrivateDnsModeAsString(int mode) {
        switch (mode) {
            case PRIVATE_DNS_MODE_OFF:
                return PRIVATE_DNS_MODE_OFF_STRING;
            case PRIVATE_DNS_MODE_OPPORTUNISTIC:
                return PRIVATE_DNS_MODE_OPPORTUNISTIC_STRING;
            case PRIVATE_DNS_MODE_PROVIDER_HOSTNAME:
                return PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING;
            default:
                throw new IllegalArgumentException("Invalid private dns mode: " + mode);
        }
    }

    private static int getPrivateDnsModeAsInt(String mode) {
        switch (mode) {
            case "off":
                return PRIVATE_DNS_MODE_OFF;
            case "hostname":
                return PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
            case "opportunistic":
                return PRIVATE_DNS_MODE_OPPORTUNISTIC;
            default:
                throw new IllegalArgumentException("Invalid private dns mode: " + mode);
        }
    }

    /**
     * Get private DNS mode from settings.
     *
     * @param context The Context to query the private DNS mode from settings.
     * @return An integer of private DNS mode.
     */
    public static int getPrivateDnsMode(@NonNull Context context) {
        final ContentResolver cr = context.getContentResolver();
        String mode = Settings.Global.getString(cr, PRIVATE_DNS_MODE);
        if (TextUtils.isEmpty(mode)) mode = Settings.Global.getString(cr, PRIVATE_DNS_DEFAULT_MODE);
        // If both PRIVATE_DNS_MODE and PRIVATE_DNS_DEFAULT_MODE are not set, choose
        // PRIVATE_DNS_MODE_OPPORTUNISTIC as default mode.
        if (TextUtils.isEmpty(mode)) return PRIVATE_DNS_MODE_OPPORTUNISTIC;
        return getPrivateDnsModeAsInt(mode);
    }

    /**
     * Set private DNS mode to settings.
     *
     * @param context The {@link Context} to set the private DNS mode.
     * @param mode The private dns mode. This should be one of the PRIVATE_DNS_MODE_* constants.
     */
    public static void setPrivateDnsMode(@NonNull Context context, int mode) {
        if (!(mode == PRIVATE_DNS_MODE_OFF
                || mode == PRIVATE_DNS_MODE_OPPORTUNISTIC
                || mode == PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            throw new IllegalArgumentException("Invalid private dns mode: " + mode);
        }
        Settings.Global.putString(context.getContentResolver(), PRIVATE_DNS_MODE,
                getPrivateDnsModeAsString(mode));
    }

    /**
     * Get specific private dns provider name from {@link Settings}.
     *
     * @param context The {@link Context} to query the setting.
     * @return The specific private dns provider name, or null if no setting value.
     */
    @Nullable
    public static String getPrivateDnsHostname(@NonNull Context context) {
        return Settings.Global.getString(context.getContentResolver(), PRIVATE_DNS_SPECIFIER);
    }

    /**
     * Set specific private dns provider name to {@link Settings}.
     *
     * @param context The {@link Context} to set the setting.
     * @param specifier The specific private dns provider name.
     */
    public static void setPrivateDnsHostname(@NonNull Context context, @Nullable String specifier) {
        Settings.Global.putString(context.getContentResolver(), PRIVATE_DNS_SPECIFIER, specifier);
    }
}

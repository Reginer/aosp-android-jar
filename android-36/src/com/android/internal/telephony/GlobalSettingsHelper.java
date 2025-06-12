/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.internal.telephony;

import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

/**
 * Helper class that reads and writes Global.Setting values for Telephony. It will:
 * For Single SIM case, read from or write to the singleton setting value.
 * For Multi-SIM case, read from or write to the per subscription value.
 */
public class GlobalSettingsHelper {
    /**
     * Helper function to get integer value.
     */
    public static int getInt(Context context, String settingName, int subId,
            int defaultValue) {
        settingName = getSettingName(context, settingName, subId);
        return Settings.Global.getInt(context.getContentResolver(), settingName, defaultValue);
    }

    /**
     * Helper function to get boolean value.
     */
    public static boolean getBoolean(Context context, String settingName, int subId,
            boolean defaultValue) {
        settingName = getSettingName(context, settingName, subId);
        return Settings.Global.getInt(context.getContentResolver(), settingName,
                defaultValue ? 1 : 0) == 1;
    }

    /**
     * Helper function to get boolean value or throws SettingNotFoundException if not set.
     */
    public static boolean getBoolean(Context context, String settingName, int subId)
            throws SettingNotFoundException {
        settingName = getSettingName(context, settingName, subId);
        return Settings.Global.getInt(context.getContentResolver(), settingName) == 1;
    }

    /**
     * Helper function to set integer value.
     * Returns whether the value is changed or initially set.
     */
    public static boolean setInt(Context context, String settingName, int subId, int value) {
        settingName = getSettingName(context, settingName, subId);

        boolean needChange;
        try {
            needChange = Settings.Global.getInt(context.getContentResolver(), settingName) != value;
        } catch (SettingNotFoundException exception) {
            needChange = true;
        }
        if (needChange) Settings.Global.putInt(context.getContentResolver(), settingName, value);

        return needChange;
    }

    /**
     * Helper function to set boolean value.
     * Returns whether the value is changed or initially set.
     */
    public static boolean setBoolean(Context context, String settingName, int subId,
            boolean value) {
        return setInt(context, settingName, subId, value ? 1 : 0);
    }

    private static String getSettingName(Context context, String settingName, int subId) {
        // For single SIM phones, this is a per phone property. Or if it's invalid subId, we
        // read default setting.
        if (TelephonyManager.from(context).getSimCount() > 1
                && SubscriptionManager.isValidSubscriptionId(subId)) {
            return settingName + subId;
        } else {
            return settingName;
        }
    }
}

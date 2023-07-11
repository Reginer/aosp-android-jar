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

package com.android.clockwork.bluetooth;

import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;

/** Bluetooth related constants and helper methods to access settings. */
public class WearBluetoothSettings {
    private WearBluetoothSettings() {}

    public static final String LOG_TAG = "WearBluetooth";

    // The normal score for proxy is 100 to guarantee that it is selected as the default network
    // by ConnectivityService.  However, when the device is on charger, a lower score is used
    // to ensure that WiFi can gain priority and be the default network instead.
    public static final int PROXY_SCORE_CLASSIC = 100;
    // A score of 55 is between the low end of WiFi (~60) and the score of cellular (50).
    // This is the same as the score used for BLE-paired Wear devices.
    public static final int PROXY_SCORE_ON_CHARGER = 55;
    // The score used for BLE-paired Wear devices.
    public static final int PROXY_SCORE_BLE = 55;

    public static final String WEARABLE_SETTINGS_AUTHORITY = "com.google.android.wearable.settings";

    public static final String BLUETOOTH_PATH = "bluetooth";

    public static final Uri BLUETOOTH_URI =
            new Uri.Builder()
                    .scheme("content")
                    .authority(WEARABLE_SETTINGS_AUTHORITY)
                    .path(BLUETOOTH_PATH)
                    .build();

    public static final String SETTINGS_COLUMN_KEY = "key";

    public static final String SETTINGS_COLUMN_VALUE = "value";

    /**
     * Key of the bluetooth settings storing duplicate address of companion device. This setting is
     * stored in the legacy wear provider {@code
     * content://com.google.android.wearable.settings/bluetooth} and is protected by read
     * permission.
     */
    public static final String KEY_COMPANION_ADDRESS = "companion_address";

    /**
     * Key of the bluetooth settings storing the address of companion device. This setting is stored
     * in the legacy wear provider {@code content://com.google.android.wearable.settings/bluetooth}
     * and is protected by read permission.
     */
    public static final String KEY_COMPANION_ADDRESS_DUAL = "companion_bt_address_dual";

    /**
     * Put a string bluetooth setting.
     *
     * @return if the settings is successfully put.
     */
    public static boolean putString(ContentResolver resolver, String key, String value) {
        if (KEY_COMPANION_ADDRESS.equals(key) || KEY_COMPANION_ADDRESS_DUAL.equals(key)) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(key, value);
            return resolver.update(BLUETOOTH_URI, contentValues, null, null) > 0;
        } else {
            return Settings.Global.putString(resolver, key, value);
        }
    }

    /** Get a string bluetooth setting, or null if it doesn't exist. */
    @Nullable
    public static String getString(ContentResolver resolver, String key) {
        if (KEY_COMPANION_ADDRESS.equals(key) || KEY_COMPANION_ADDRESS_DUAL.equals(key)) {
            try (Cursor cursor = resolver.query(BLUETOOTH_URI, null, null, null, null)) {
                if (cursor != null) {
                    int keyColumn = cursor.getColumnIndex(SETTINGS_COLUMN_KEY);
                    int valueColumn = cursor.getColumnIndex(SETTINGS_COLUMN_VALUE);
                    while (cursor.moveToNext()) {
                        if (key.equals(cursor.getString(keyColumn))) {
                            return cursor.getString(valueColumn);
                        }
                    }
                }
            }
            return null;
        } else {
            return Settings.Global.getString(resolver, key);
        }
    }

    /** Put an integer bluetooth setting. */
    public static boolean putInt(ContentResolver resolver, String key, int value) {
        return putString(resolver, key, String.valueOf(value));
    }

    /** Get an integer bluetooth setting, or the default value if it doesn't exist. */
    public static int getInt(ContentResolver resolver, String key, int defaultVal) {
        String strVal = getString(resolver, key);
        return strVal == null ? defaultVal : Integer.valueOf(strVal);
    }
}

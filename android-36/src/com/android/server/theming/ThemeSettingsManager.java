/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.theming;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.content.ContentResolver;
import android.content.theming.ThemeSettings;
import android.content.theming.ThemeSettingsField;
import android.content.theming.ThemeSettingsUpdater;
import android.provider.Settings;
import android.telecom.Log;

import com.android.internal.util.Preconditions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Manages the loading and saving of theme settings. This class handles the persistence of theme
 * settings to and from the system settings. It utilizes a collection of {@link ThemeSettingsField}
 * objects to represent individual theme setting fields.
 *
 * @hide
 */
@FlaggedApi(android.server.Flags.FLAG_ENABLE_THEME_SERVICE)
class ThemeSettingsManager {
    private static final String TAG = ThemeSettingsManager.class.getSimpleName();
    static final String TIMESTAMP_FIELD = "_applied_timestamp";
    private final ThemeSettingsField<?, ?>[] mFields;
    private final ThemeSettings mDefaults;

    /**
     * Constructs a new {@code ThemeSettingsManager} with the specified default settings.
     *
     * @param defaults The default theme settings to use.
     */
    ThemeSettingsManager(ThemeSettings defaults) {
        mDefaults = defaults;
        mFields = ThemeSettingsField.getFields(defaults);
    }

    /**
     * Loads the theme settings for the specified user.
     *
     * @param userId          The ID of the user.
     * @param contentResolver The content resolver to use.
     * @return The loaded {@link ThemeSettings}.
     */
    @NonNull
    ThemeSettings loadSettings(@UserIdInt int userId, ContentResolver contentResolver) {
        String jsonString = Settings.Secure.getStringForUser(contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, userId);

        JSONObject userSettings;

        try {
            userSettings = new JSONObject(jsonString == null ? "" : jsonString);
        } catch (JSONException e) {
            userSettings = new JSONObject();
        }

        ThemeSettingsUpdater updater = ThemeSettings.updater();

        for (ThemeSettingsField<?, ?> field : mFields) {
            field.fromJSON(userSettings, updater);
        }

        return updater.toThemeSettings(mDefaults);
    }

    /**
     * Saves the specified theme settings for the given user.
     *
     * @param userId          The ID of the user.
     * @param contentResolver The content resolver to use.
     * @param newSettings     The {@link ThemeSettings} to save.
     */
    void replaceSettings(@UserIdInt int userId, ContentResolver contentResolver,
            ThemeSettings newSettings) throws RuntimeException {
        Preconditions.checkArgument(newSettings != null, "Impossible to write empty settings");

        JSONObject jsonSettings = new JSONObject();


        for (ThemeSettingsField<?, ?> field : mFields) {
            field.toJSON(newSettings, jsonSettings);
        }

        // user defined timestamp should be ignored. Storing new timestamp.
        try {
            jsonSettings.put(TIMESTAMP_FIELD, System.currentTimeMillis());
        } catch (JSONException e) {
            Log.w(TAG, "Error saving timestamp: " + e.getMessage());
        }

        String jsonString = jsonSettings.toString();

        Settings.Secure.putStringForUser(contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, jsonString, userId);
    }

    /**
     * Saves the specified theme settings for the given user, while preserving unrelated existing
     * properties.
     *
     * @param userId          The ID of the user.
     * @param contentResolver The content resolver to use.
     * @param newSettings     The {@link ThemeSettings} to save.
     */
    void updateSettings(@UserIdInt int userId, ContentResolver contentResolver,
            ThemeSettings newSettings) throws JSONException, RuntimeException {
        Preconditions.checkArgument(newSettings != null, "Impossible to write empty settings");

        String existingJsonString = Settings.Secure.getStringForUser(contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, userId);

        JSONObject existingJson;
        try {
            existingJson = new JSONObject(existingJsonString == null ? "{}" : existingJsonString);
        } catch (JSONException e) {
            existingJson = new JSONObject();
        }

        JSONObject newJson = new JSONObject();
        for (ThemeSettingsField<?, ?> field : mFields) {
            field.toJSON(newSettings, newJson);
        }

        // user defined timestamp should be ignored. Storing new timestamp.
        try {
            newJson.put(TIMESTAMP_FIELD, System.currentTimeMillis());
        } catch (JSONException e) {
            Log.w(TAG, "Error saving timestamp: " + e.getMessage());
        }

        // Merge the new settings with the existing settings
        Iterator<String> keys = newJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            existingJson.put(key, newJson.get(key));
        }

        String mergedJsonString = existingJson.toString();

        Settings.Secure.putStringForUser(contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, mergedJsonString, userId);
    }
}

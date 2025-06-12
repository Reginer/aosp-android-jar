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

package android.content.theming;


import android.annotation.FlaggedApi;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.internal.util.Preconditions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Represents a field within {@link ThemeSettings}, providing methods for parsing, serializing,
 * managing default values, and validating the field's value.
 * <p>
 * This class is designed to be extended by concrete classes that represent specific fields within
 * {@link ThemeSettings}. Each subclass should define the following methods, where T is the type of
 * the field's value and J is the type of the field's value stored in JSON:
 * <ul>
 *   <li>{@link #parse(Object)} to parse a JSON representation into the field's value type.</li>
 *   <li>{@link #serialize(Object)} to serialize the field's value into a JSON representation.</li>
 *   <li>{@link #validate(Object)} to validate the field's value.</li>
 *   <li>{@link #getFieldType()} to return the type of the field's value.</li>
 *   <li>{@link #getJsonType()} to return the type of the field's value stored in JSON.</li>
 * </ul>
 * <p>
 * The {@link #fromJSON(JSONObject, ThemeSettingsUpdater)} and
 * {@link #toJSON(ThemeSettings, JSONObject)}
 * methods handle the extraction and serialization of the field's value to and from JSON objects
 * respectively. The {@link #fallbackParse(Object, Object)} method is used to parse a string
 * representation of the field's value, falling back to a default value if parsing fails.
 *
 * @param <T> The type of the field's value.
 * @param <J> The type of the JSON property.
 * @hide
 */
@FlaggedApi(android.server.Flags.FLAG_ENABLE_THEME_SERVICE)
public abstract class ThemeSettingsField<T, J> {
    private static final String TAG = ThemeSettingsField.class.getSimpleName();

    private static final String KEY_PREFIX = "android.theme.customization.";
    public static final String OVERLAY_CATEGORY_ACCENT_COLOR = KEY_PREFIX + "accent_color";
    public static final String OVERLAY_CATEGORY_SYSTEM_PALETTE = KEY_PREFIX + "system_palette";
    public static final String OVERLAY_CATEGORY_THEME_STYLE = KEY_PREFIX + "theme_style";
    public static final String OVERLAY_COLOR_SOURCE = KEY_PREFIX + "color_source";
    public static final String OVERLAY_COLOR_INDEX = KEY_PREFIX + "color_index";
    public static final String OVERLAY_COLOR_BOTH = KEY_PREFIX + "color_both";


    /**
     * Returns an array of all available {@link ThemeSettingsField} instances.
     *
     * @param defaults The default {@link ThemeSettings} object to use for default values.
     * @return An array of {@link ThemeSettingsField} instances.
     */
    public static ThemeSettingsField<?, ?>[] getFields(ThemeSettings defaults) {
        return new ThemeSettingsField[]{
                new FieldColorIndex(
                        OVERLAY_COLOR_INDEX,
                        ThemeSettingsUpdater::colorIndex,
                        ThemeSettings::colorIndex,
                        defaults),
                new FieldColor(
                        OVERLAY_CATEGORY_SYSTEM_PALETTE,
                        ThemeSettingsUpdater::systemPalette,
                        ThemeSettings::systemPalette,
                        defaults),
                new FieldColor(
                        OVERLAY_CATEGORY_ACCENT_COLOR,
                        ThemeSettingsUpdater::accentColor,
                        ThemeSettings::accentColor,
                        defaults),
                new FieldColorSource(
                        OVERLAY_COLOR_SOURCE,
                        ThemeSettingsUpdater::colorSource,
                        ThemeSettings::colorSource,
                        defaults),
                new FieldThemeStyle(
                        OVERLAY_CATEGORY_THEME_STYLE,
                        ThemeSettingsUpdater::themeStyle,
                        ThemeSettings::themeStyle,
                        defaults),
                new FieldColorBoth(
                        OVERLAY_COLOR_BOTH,
                        ThemeSettingsUpdater::colorBoth,
                        ThemeSettings::colorBoth,
                        defaults)
        };
    }

    public final String key;
    private final BiConsumer<ThemeSettingsUpdater, T> mSetter;
    private final Function<ThemeSettings, T> mGetter;
    private final ThemeSettings mDefaults;

    /**
     * Creates a new {@link ThemeSettingsField}.
     *
     * @param key      The key to identify the field in JSON objects.
     * @param setter   The setter to update the field's value in a {@link ThemeSettingsUpdater}.
     * @param getter   The getter to retrieve the field's value from a {@link ThemeSettings}
     *                 object.
     * @param defaults The default {@link ThemeSettings} object to provide default values.
     */

    public ThemeSettingsField(
            String key,
            BiConsumer<ThemeSettingsUpdater, T> setter,
            Function<ThemeSettings, T> getter,
            ThemeSettings defaults
    ) {
        this.key = key;
        mSetter = setter;
        mGetter = getter;
        mDefaults = defaults;
    }

    /**
     * Attempts to parse a JSON primitive representation of the field's value. If parsing fails, it
     * defaults to the field's default value.
     *
     * @param primitive The string representation to parse.
     */
    private T fallbackParse(Object primitive, T fallbackValue) {
        if (primitive == null) {
            Log.w(TAG, "Error, field `" + key + "` was not found, defaulting to " + fallbackValue);
            return fallbackValue;
        }

        if (!getJsonType().isInstance(primitive)) {
            Log.w(TAG, "Error, field `" + key + "` expected to be of type `"
                    + getJsonType().getSimpleName()
                    + "`, got `" + primitive.getClass().getSimpleName() + "`, defaulting to "
                    + fallbackValue);
            return fallbackValue;
        }

        // skips parsing if destination json type is already the same as field type
        T parsedValue = getFieldType() == getJsonType() ? (T) primitive : parse((J) primitive);

        if (parsedValue == null) {
            Log.w(TAG, "Error parsing JSON field `" + key + "` , defaulting to " + fallbackValue);
            return fallbackValue;
        }

        if (!validate(parsedValue)) {
            Log.w(TAG,
                    "Error validating JSON field `" + key + "` , defaulting to " + fallbackValue);
            return fallbackValue;
        }

        if (parsedValue.getClass() != getFieldType()) {
            Log.w(TAG, "Error: JSON field `" + key + "` expected to be of type `"
                    + getFieldType().getSimpleName()
                    + "`, defaulting to " + fallbackValue);
            return fallbackValue;
        }

        return parsedValue;
    }


    /**
     * Extracts the field's value from a JSON object and sets it in a
     * {@link ThemeSettingsUpdater}.
     *
     * @param source The JSON object containing the field's value.
     */
    public void fromJSON(JSONObject source, ThemeSettingsUpdater updater) {
        Object primitiveStr = source.opt(key);
        T typedValue = fallbackParse(primitiveStr, getDefaultValue());
        mSetter.accept(updater, typedValue);
    }

    /**
     * Serializes the field's value from a {@link ThemeSettings} object into a JSON object.
     *
     * @param source      The {@link ThemeSettings} object from which to retrieve the field's
     *                    value.
     * @param destination The JSON object to which the field's value will be added.
     */
    public void toJSON(ThemeSettings source, JSONObject destination) {
        T value = mGetter.apply(source);
        Preconditions.checkState(value.getClass() == getFieldType());

        J serialized;
        if (validate(value)) {
            serialized = serialize(value);
        } else {
            T fallbackValue = getDefaultValue();
            serialized = serialize(fallbackValue);
            Log.w(TAG, "Invalid value `" + value + "` for key `" + key + "`, defaulting to '"
                    + fallbackValue);
        }

        try {
            destination.put(key, serialized);
        } catch (JSONException e) {
            Log.d(TAG,
                    "Error writing JSON primitive, skipping field " + key + ", " + e.getMessage());
        }
    }


    /**
     * Returns the default value of the field.
     *
     * @return The default value.
     */
    @VisibleForTesting
    @NonNull
    public T getDefaultValue() {
        return mGetter.apply(mDefaults);
    }

    /**
     * Parses a string representation into the field's value type.
     *
     * @param primitive The string representation to parse.
     * @return The parsed value, or null if parsing fails.
     */
    @VisibleForTesting
    @Nullable
    public abstract T parse(J primitive);

    /**
     * Serializes the field's value into a primitive type suitable for JSON.
     *
     * @param value The value to serialize.
     * @return The serialized value.
     */
    @VisibleForTesting
    public abstract J serialize(T value);

    /**
     * Validates the field's value.
     * This method can be overridden to perform custom validation logic and MUST NOT validate for
     * nullity.
     *
     * @param value The value to validate.
     * @return {@code true} if the value is valid, {@code false} otherwise.
     */
    @VisibleForTesting
    public abstract boolean validate(T value);

    /**
     * Returns the type of the field's value.
     *
     * @return The type of the field's value.
     */
    @VisibleForTesting
    public abstract Class<T> getFieldType();

    /**
     * Returns the type of the field's value stored in JSON.
     *
     * <p>This method is used to determine the expected type of the field's value when it is
     * stored in a JSON object.
     *
     * @return The type of the field's value stored in JSON.
     */
    @VisibleForTesting
    public abstract Class<J> getJsonType();
}

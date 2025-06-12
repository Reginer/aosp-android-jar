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

import android.annotation.ColorInt;
import android.annotation.FlaggedApi;
import android.graphics.Color;

import androidx.annotation.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/** @hide */
@FlaggedApi(android.server.Flags.FLAG_ENABLE_THEME_SERVICE)
public class FieldColor extends ThemeSettingsField<Integer, String> {
    private static final Pattern COLOR_PATTERN = Pattern.compile("[0-9a-fA-F]{6,8}");

    public FieldColor(
            String key,
            BiConsumer<ThemeSettingsUpdater, Integer> setter,
            Function<ThemeSettings, Integer> getter,
            ThemeSettings defaults
    ) {
        super(key, setter, getter, defaults);
    }

    @Override
    @ColorInt
    @Nullable
    public Integer parse(String primitive) {
        if (primitive == null) {
            return null;
        }
        if (!COLOR_PATTERN.matcher(primitive).matches()) {
            return null;
        }

        try {
            return Color.valueOf(Color.parseColor("#" + primitive)).toArgb();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String serialize(@ColorInt Integer value) {
        return Integer.toHexString(value);
    }

    @Override
    public boolean validate(Integer value) {
        return !value.equals(Color.TRANSPARENT);
    }

    @Override
    public Class<Integer> getFieldType() {
        return Integer.class;
    }

    @Override
    public Class<String> getJsonType() {
        return String.class;
    }
}

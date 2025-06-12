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
import android.annotation.StringDef;

import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** @hide */
@FlaggedApi(android.server.Flags.FLAG_ENABLE_THEME_SERVICE)
public class FieldColorSource extends ThemeSettingsField<String, String> {
    public FieldColorSource(
            String key,
            BiConsumer<ThemeSettingsUpdater, String> setter,
            Function<ThemeSettings, String> getter,
            ThemeSettings defaults
    ) {
        super(key, setter, getter, defaults);
    }

    @Override
    @Nullable
    @Type
    public String parse(String primitive) {
        return primitive;
    }

    @Override
    public String serialize(@Type String typedValue) {
        return typedValue;
    }

    @Override
    public boolean validate(String value) {
        return switch (value) {
            case "preset", "home_wallpaper", "lock_wallpaper" -> true;
            default -> false;
        };
    }

    @Override
    public Class<String> getFieldType() {
        return String.class;
    }

    @Override
    public Class<String> getJsonType() {
        return String.class;
    }


    @StringDef({"preset", "home_wallpaper", "lock_wallpaper"})
    @Retention(RetentionPolicy.SOURCE)
    @interface Type {
    }
}

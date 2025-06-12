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

import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** @hide */
@FlaggedApi(android.server.Flags.FLAG_ENABLE_THEME_SERVICE)
public class FieldColorBoth extends ThemeSettingsField<Boolean, String> {
    public FieldColorBoth(
            String key,
            BiConsumer<ThemeSettingsUpdater, Boolean> setter,
            Function<ThemeSettings, Boolean> getter,
            ThemeSettings defaults
    ) {
        super(key, setter, getter, defaults);
    }

    @Override
    @Nullable
    public Boolean parse(String primitive) {
        return switch (primitive) {
            case "1" -> true;
            case "0" -> false;
            default -> null;
        };
    }

    @Override
    public String serialize(Boolean typedValue) {
        if (typedValue) return "1";
        return "0";
    }

    @Override
    public boolean validate(Boolean value) {
        Objects.requireNonNull(value);
        return true;
    }

    @Override
    public Class<Boolean> getFieldType() {
        return Boolean.class;
    }

    @Override
    public Class<String> getJsonType() {
        return String.class;
    }
}

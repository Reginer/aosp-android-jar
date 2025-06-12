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
import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.VisibleForTesting;

import java.util.Objects;

/**
 * Updater class for constructing {@link ThemeSettings} objects.
 * This class provides a fluent interface for setting the various properties of the theme
 * settings.
 *
 * @hide
 */
@FlaggedApi(android.server.Flags.FLAG_ENABLE_THEME_SERVICE)
public class ThemeSettingsUpdater implements Parcelable {
    @ColorInt
    private Integer mAccentColor;
    private Boolean mColorBoth;
    private Integer mColorIndex;
    private String mColorSource;
    @ColorInt
    private Integer mSystemPalette;
    private Integer mThemeStyle;

    ThemeSettingsUpdater(Integer colorIndex, @ColorInt Integer systemPalette,
            @ColorInt Integer accentColor, @FieldColorSource.Type String colorSource,
            @ThemeStyle.Type Integer themeStyle, Boolean colorBoth) {
        this.mAccentColor = accentColor;
        this.mColorBoth = colorBoth;
        this.mColorIndex = colorIndex;
        this.mColorSource = colorSource;
        this.mSystemPalette = systemPalette;
        this.mThemeStyle = themeStyle;
    }

    ThemeSettingsUpdater() {
    }

    // only reading basic JVM types for nullability
    @SuppressLint("ParcelClassLoader")
    protected ThemeSettingsUpdater(Parcel in) {
        mAccentColor = (Integer) in.readValue(null);
        mColorBoth = (Boolean) in.readValue(null);
        mColorIndex = (Integer) in.readValue(null);
        mColorSource = (String) in.readValue(null);
        mSystemPalette = (Integer) in.readValue(null);
        mThemeStyle = (Integer) in.readValue(null);
    }

    // using read/writeValue for nullability support
    @SuppressWarnings("AndroidFrameworkEfficientParcelable")
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mAccentColor);
        dest.writeValue(mColorBoth);
        dest.writeValue(mColorIndex);
        dest.writeValue(mColorSource);
        dest.writeValue(mSystemPalette);
        dest.writeValue(mThemeStyle);
    }

    /**
     * Sets the color index.
     *
     * @param colorIndex The color index to set.
     * @return This {@link ThemeSettingsUpdater} instance.
     */
    public ThemeSettingsUpdater colorIndex(int colorIndex) {
        this.mColorIndex = colorIndex;
        return this;
    }

    /**
     * Returns the color index.
     *
     * @return The color index.
     */
    @VisibleForTesting
    public Integer getColorIndex() {
        return mColorIndex;
    }

    /**
     * Sets the system palette color.
     *
     * @param systemPalette The system palette color to set.
     * @return This {@link ThemeSettingsUpdater} instance.
     */
    public ThemeSettingsUpdater systemPalette(@ColorInt int systemPalette) {
        this.mSystemPalette = systemPalette;
        return this;
    }

    /**
     * Returns the system palette color.
     *
     * @return The system palette color.
     */
    @VisibleForTesting
    public Integer getSystemPalette() {
        return mSystemPalette;
    }

    /**
     * Sets the accent color.
     *
     * @param accentColor The accent color to set.
     * @return This {@link ThemeSettingsUpdater} instance.
     */
    public ThemeSettingsUpdater accentColor(@ColorInt int accentColor) {
        this.mAccentColor = accentColor;
        return this;
    }

    /**
     * Returns the accent color.
     *
     * @return The accent color.
     */
    @VisibleForTesting
    public Integer getAccentColor() {
        return mAccentColor;
    }

    /**
     * Sets the color source.
     *
     * @param colorSource The color source to set.
     * @return This {@link ThemeSettingsUpdater} instance.
     */
    public ThemeSettingsUpdater colorSource(@NonNull @FieldColorSource.Type String colorSource) {
        this.mColorSource = colorSource;
        return this;
    }

    /**
     * Returns the theme style.
     *
     * @return The theme style.
     */
    @VisibleForTesting
    public Integer getThemeStyle() {
        return mThemeStyle;
    }

    /**
     * Sets the theme style.
     *
     * @param themeStyle The theme style to set.
     * @return This {@link ThemeSettingsUpdater} instance.
     */
    public ThemeSettingsUpdater themeStyle(@ThemeStyle.Type int themeStyle) {
        this.mThemeStyle = themeStyle;
        return this;
    }

    /**
     * Returns the color source.
     *
     * @return The color source.
     */
    @VisibleForTesting
    public String getColorSource() {
        return mColorSource;
    }

    /**
     * Sets the color combination.
     *
     * @param colorBoth The color combination to set.
     * @return This {@link ThemeSettingsUpdater} instance.
     */
    public ThemeSettingsUpdater colorBoth(boolean colorBoth) {
        this.mColorBoth = colorBoth;
        return this;
    }

    /**
     * Returns the color combination.
     *
     * @return The color combination.
     */
    @VisibleForTesting
    public Boolean getColorBoth() {
        return mColorBoth;
    }

    /**
     * Constructs a new {@link ThemeSettings} object with the current builder settings.
     *
     * @return A new {@link ThemeSettings} object.
     */
    public ThemeSettings toThemeSettings(@NonNull ThemeSettings defaults) {
        return new ThemeSettings(
                Objects.requireNonNullElse(mColorIndex, defaults.colorIndex()),
                Objects.requireNonNullElse(mSystemPalette, defaults.systemPalette()),
                Objects.requireNonNullElse(mAccentColor, defaults.accentColor()),
                Objects.requireNonNullElse(mColorSource, defaults.colorSource()),
                Objects.requireNonNullElse(mThemeStyle, defaults.themeStyle()),
                Objects.requireNonNullElse(mColorBoth, defaults.colorBoth()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ThemeSettingsUpdater> CREATOR =
            new Creator<>() {
                @Override
                public ThemeSettingsUpdater createFromParcel(Parcel in) {
                    return new ThemeSettingsUpdater(in);
                }

                @Override
                public ThemeSettingsUpdater[] newArray(int size) {
                    return new ThemeSettingsUpdater[size];
                }
            };
}

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
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Represents the theme settings for the system.
 * This class holds various properties related to theming, such as color indices, palettes,
 * accent colors, color sources, theme styles, and color combinations.
 *
 * @hide
 */
@FlaggedApi(android.server.Flags.FLAG_ENABLE_THEME_SERVICE)
public final class ThemeSettings implements Parcelable {
    private final int mColorIndex;
    private final int mSystemPalette;
    private final int mAccentColor;
    @NonNull
    private final String mColorSource;
    private final int mThemeStyle;
    private final boolean mColorBoth;

    /**
     * Constructs a new ThemeSettings object.
     *
     * @param colorIndex    The color index.
     * @param systemPalette The system palette color.
     * @param accentColor   The accent color.
     * @param colorSource   The color source.
     * @param themeStyle    The theme style.
     * @param colorBoth     The color combination.
     */

    public ThemeSettings(int colorIndex, @ColorInt int systemPalette,
            @ColorInt int accentColor, @NonNull String colorSource, int themeStyle,
            boolean colorBoth) {

        this.mAccentColor = accentColor;
        this.mColorBoth = colorBoth;
        this.mColorIndex = colorIndex;
        this.mColorSource = colorSource;
        this.mSystemPalette = systemPalette;
        this.mThemeStyle = themeStyle;
    }

    /**
     * Constructs a ThemeSettings object from a Parcel.
     *
     * @param in The Parcel to read from.
     */
    ThemeSettings(Parcel in) {
        this.mAccentColor = in.readInt();
        this.mColorBoth = in.readBoolean();
        this.mColorIndex = in.readInt();
        this.mColorSource = Objects.requireNonNullElse(in.readString8(), "s");
        this.mSystemPalette = in.readInt();
        this.mThemeStyle = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mAccentColor);
        dest.writeBoolean(mColorBoth);
        dest.writeInt(mColorIndex);
        dest.writeString8(mColorSource);
        dest.writeInt(mSystemPalette);
        dest.writeInt(mThemeStyle);
    }

    /**
     * Gets the color index.
     *
     * @return The color index.
     */
    public Integer colorIndex() {
        return mColorIndex;
    }

    /**
     * Gets the system palette color.
     *
     * @return The system palette color.
     */
    @ColorInt
    public Integer systemPalette() {
        return mSystemPalette;
    }

    /**
     * Gets the accent color.
     *
     * @return The accent color.
     */
    @ColorInt
    public Integer accentColor() {
        return mAccentColor;
    }

    /**
     * Gets the color source.
     *
     * @return The color source.
     */
    @FieldColorSource.Type
    public String colorSource() {
        return mColorSource;
    }

    /**
     * Gets the theme style.
     *
     * @return The theme style.
     */
    @ThemeStyle.Type
    public Integer themeStyle() {
        return mThemeStyle;
    }

    /**
     * Gets the color combination.
     *
     * @return The color combination.
     */
    public Boolean colorBoth() {
        return mColorBoth;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof ThemeSettings other
                && mColorIndex == other.mColorIndex
                && mSystemPalette == other.mSystemPalette
                && mAccentColor == other.mAccentColor
                && mColorSource.equals(other.mColorSource)
                && mThemeStyle == other.mThemeStyle
                && mColorBoth == other.mColorBoth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mColorIndex, mSystemPalette, mAccentColor, mColorSource, mThemeStyle,
                mColorBoth);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Creator for Parcelable interface.
     */
    public static final Creator<ThemeSettings> CREATOR = new Creator<>() {
        @Override
        public ThemeSettings createFromParcel(Parcel in) {
            return new ThemeSettings(in);
        }

        @Override
        public ThemeSettings[] newArray(int size) {
            return new ThemeSettings[size];
        }
    };

    /**
     * Creates a new {@link ThemeSettingsUpdater} instance for updating the {@link ThemeSettings}
     * through the API.
     *
     * @return A new {@link ThemeSettingsUpdater} instance.
     */
    public static ThemeSettingsUpdater updater() {
        return new ThemeSettingsUpdater();
    }
}

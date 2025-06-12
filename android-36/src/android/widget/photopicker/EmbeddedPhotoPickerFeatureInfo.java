/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.widget.photopicker;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorLong;
import androidx.annotation.IntRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * An immutable parcel to carry information regarding desired features of caller for
 * a given session.
 *
 * <p> Below features are currently supported in embedded photopicker.
 *
 * <ul>
 * <li> Mime type to filter media
 * <li> Accent color to change color of primary picker element
 * <li> Ordered selection of media items
 * <li> Max selection media count restriction
 * <li> Pre-selected uris
 * <li> Theme night mode
 * </ul>
 *
 * <p> Callers should use {@link Builder} to set the desired features.
 *
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@FlaggedApi("com.android.providers.media.flags.enable_embedded_photopicker")
public final class EmbeddedPhotoPickerFeatureInfo implements Parcelable {
    private final List<String> mMimeTypes;
    private final long mAccentColor;
    private final boolean mOrderedSelection;
    private final int mMaxSelectionLimit;
    private final List<Uri> mPreSelectedUris;
    private final int mThemeNightMode;

    private EmbeddedPhotoPickerFeatureInfo(
            List<String> mimeTypes,
            long accentColor,
            boolean orderedSelection,
            int maxSelectionLimit,
            List<Uri> preSelectedUris,
            int themeNightMode) {
        this.mMimeTypes = mimeTypes;
        this.mAccentColor = accentColor;
        this.mOrderedSelection = orderedSelection;
        this.mMaxSelectionLimit = maxSelectionLimit;
        this.mPreSelectedUris = preSelectedUris;
        this.mThemeNightMode = themeNightMode;
    }
    @NonNull
    public List<Uri> getPreSelectedUris() {
        return this.mPreSelectedUris;
    }
    public int getMaxSelectionLimit() {
        return this.mMaxSelectionLimit;
    }
    public boolean isOrderedSelection() {
        return this.mOrderedSelection;
    }
    @ColorLong
    public long getAccentColor() {
        return this.mAccentColor;
    }
    @NonNull
    public List<String> getMimeTypes() {
        return this.mMimeTypes;
    }
    public int getThemeNightMode() {
        return this.mThemeNightMode;
    }

    public static final class Builder {
        //All mime-types are returned by default.
        @NonNull private static final List<String> DEFAULT_MIME_TYPES =
                Arrays.asList("image/*", "video/*");
        @ColorLong
        private static final long DEFAULT_ACCENT_COLOR = -1;
        private static final boolean DEFAULT_ORDERED_SELECTION = false;
        /**
         * By-default session will open in multiselect mode and below is the maximum
         * selection limit if user doesn't specify anything.
         */
        private static final int DEFAULT_MAX_SELECTION_LIMIT = 100;
        @NonNull
        private static final List<Uri> DEFAULT_PRE_SELECTED_URIS = Arrays.asList();
        private static final int DEFAULT_NIGHT_MODE = Configuration.UI_MODE_NIGHT_UNDEFINED;

        private List<String> mMimeTypes = DEFAULT_MIME_TYPES;
        private long mAccentColor = DEFAULT_ACCENT_COLOR;
        private boolean mOrderedSelection = DEFAULT_ORDERED_SELECTION;
        private int mMaxSelectionLimit = DEFAULT_MAX_SELECTION_LIMIT;
        private List<Uri> mPreSelectedUris = DEFAULT_PRE_SELECTED_URIS;
        private int mThemeNightMode = DEFAULT_NIGHT_MODE;

        public Builder() {}

        /**
         * Sets the mime type to filter media items on.
         *
         * <p> Values may be a combination of concrete MIME types (such as "image/png")
         * and/or partial MIME types (such as "image/*").
         *
         * @param mimeTypes List of mime types to filter. By default, all media items
         *                  will be returned
         */
        @NonNull
        public Builder setMimeTypes(@NonNull List<String> mimeTypes) {
            validateMimeType(mimeTypes);
            mMimeTypes = mimeTypes;
            return this;
        }

        private void validateMimeType(List<String> mimeTypes) {
            requireNonNull(mimeTypes, "Mime type list must not be null.");
            for (String mimeType : mimeTypes) {
                requireNonNull(mimeType, "Mime type must not be null.");
                if (!isMimeTypeMedia(mimeType)) {
                    throw new IllegalArgumentException("Invalid mime type found. "
                            + "Only image/video mime types are supported");
                }
            }
        }

        /**
         * Checks if the given string is an image or video mime type
         */
        private static boolean isMimeTypeMedia(@NonNull String mimeType) {
            return mimeType.toLowerCase(Locale.getDefault()).startsWith("image/")
                    || mimeType.toLowerCase(Locale.getDefault()).startsWith("video/");
        }

        /**
         * Sets accent color which will change color of primary picker elements like Done button,
         * selected media icon colors, tab color etc.
         *
         * <p> The value of this intent-extra must be a string specifying the hex code of the
         * accent color that is to be used within the picker.
         *
         * <p> This param is same as {@link MediaStore#EXTRA_PICK_IMAGES_ACCENT_COLOR}. See {@link
         * MediaStore#EXTRA_PICK_IMAGES_ACCENT_COLOR} for more details on accepted colors.
         *
         * @param accentColor Hex code of desired accent color. By default, the color of elements
         * will reflect based on device theme
         */
        @NonNull
        public Builder setAccentColor(@ColorLong long accentColor) {
            mAccentColor = accentColor;
            return this;
        }

        /**
         * Sets ordered selection of media items i.e. this allows user to view/receive items in
         * their selected order
         *
         * @param orderedSelection Pass true to set ordered selection. Default is false
         */
        @NonNull
        public Builder setOrderedSelection(boolean orderedSelection) {
            mOrderedSelection = orderedSelection;
            return this;
        }

        /**
         * Sets maximum number of items that can be selected by the user
         *
         * <p> The value of this intent-extra should be a positive integer greater than
         * or equal to 1 and less than or equal to {@link MediaStore#getPickImagesMaxLimit}
         *
         * @param maxSelectionLimit Max selection count restriction. Pass limit as 1 to open
         * PhotoPicker in single-select mode. Default is multi select mode with limit as
         * {@link MediaStore#getPickImagesMaxLimit()}
         */
        @NonNull
        public Builder setMaxSelectionLimit(@IntRange(from = 1) int maxSelectionLimit) {
            if (maxSelectionLimit > DEFAULT_MAX_SELECTION_LIMIT) {
                throw new IllegalArgumentException("Max selection limit should be less than "
                        + DEFAULT_MAX_SELECTION_LIMIT);
            }
            mMaxSelectionLimit = maxSelectionLimit;
            return this;
        }

        /**
         * Sets list of uris to be pre-selected when embedded picker is opened.
         *
         * <p> This is same as {@link MediaStore#EXTRA_PICKER_PRE_SELECTION_URIS}.
         * See {@link MediaStore#EXTRA_PICKER_PRE_SELECTION_URIS} for more details
         * on restrictions and filter criteria.
         *
         * @param preSelectedUris list of uris to be pre-selected
         */
        @NonNull
        public Builder setPreSelectedUris(@NonNull List<Uri> preSelectedUris) {
            requireNonNull(preSelectedUris, "Preselected uri list can not be null.");
            mPreSelectedUris = preSelectedUris;
            return this;
        }

        /**
         * Sets the embedded photo picker theme to light or dark irrespective of the device theme.
         *
         * @param themeNightMode hex code of the desired {@link Configuration#UI_MODE_NIGHT_MASK}
         *                       value.
         *
         * <p> The default value is {@link Configuration#UI_MODE_NIGHT_UNDEFINED} to apply the
         * system (device) theme.
         *
         * <p> Supported values are -</p>
         * <li> {@link Configuration#UI_MODE_NIGHT_UNDEFINED} -> system theme
         * <li> {@link Configuration#UI_MODE_NIGHT_YES} -> dark theme
         * <li> {@link Configuration#UI_MODE_NIGHT_NO} -> light theme
         */
        @NonNull
        public Builder setThemeNightMode(int themeNightMode) {
            if (!isSupportedNightModeConstant(themeNightMode)) {
                throw new IllegalArgumentException("Unsupported themeNightMode: " + themeNightMode);
            }
            mThemeNightMode = themeNightMode;
            return this;
        }

        private static boolean isSupportedNightModeConstant(int value) {
            return value == Configuration.UI_MODE_NIGHT_UNDEFINED
                    || value == Configuration.UI_MODE_NIGHT_NO
                    || value == Configuration.UI_MODE_NIGHT_YES;
        }

        /**
         * Build the class for desired feature info arguments
         */
        @NonNull
        public EmbeddedPhotoPickerFeatureInfo build() {
            return new EmbeddedPhotoPickerFeatureInfo(
                    mMimeTypes,
                    mAccentColor,
                    mOrderedSelection,
                    mMaxSelectionLimit,
                    mPreSelectedUris,
                    mThemeNightMode);
        }
    }
    private EmbeddedPhotoPickerFeatureInfo(Parcel in) {
        List<String> mimeTypes = new java.util.ArrayList<>();
        in.readStringList(mimeTypes);
        this.mMimeTypes = mimeTypes;
        this.mAccentColor = in.readLong();
        this.mOrderedSelection = in.readBoolean();
        this.mMaxSelectionLimit = in.readInt();
        final ArrayList<Uri> preSelectedUris = new ArrayList<>();
        in.readTypedList(preSelectedUris, Uri.CREATOR);
        this.mPreSelectedUris = preSelectedUris;
        this.mThemeNightMode = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(mMimeTypes);
        dest.writeLong(mAccentColor);
        dest.writeBoolean(mOrderedSelection);
        dest.writeInt(mMaxSelectionLimit);
        dest.writeTypedList(mPreSelectedUris, flags);
        dest.writeInt(mThemeNightMode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<EmbeddedPhotoPickerFeatureInfo> CREATOR =
            new Creator<EmbeddedPhotoPickerFeatureInfo>() {
                @Override
                public EmbeddedPhotoPickerFeatureInfo createFromParcel(Parcel in) {
                    return new EmbeddedPhotoPickerFeatureInfo(in);
                }

                @Override
                public EmbeddedPhotoPickerFeatureInfo[] newArray(int size) {
                    return new EmbeddedPhotoPickerFeatureInfo[size];
                }
            };

    @Override
    public String toString() {
        return "EmbeddedPhotoPickerFeatureInfo{"
                + "mMimeTypes=" + mMimeTypes
                + ", mAccentColor=" + mAccentColor
                + ", mOrderedSelection=" + mOrderedSelection
                + ", mMaxSelectionLimit=" + mMaxSelectionLimit
                + ", mPreSelectedUris=" + mPreSelectedUris
                + ", mThemeNightMode=" + mThemeNightMode
                + '}';
    }
}

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

package android.graphics.pdf.models;

import android.annotation.FlaggedApi;
import android.annotation.FloatRange;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.Rect;
import android.graphics.pdf.flags.Flags;
import android.graphics.pdf.utils.Preconditions;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Information about a form widget of a PDF document.
 *
 * @see <a
 * href="https://opensource.adobe.com/dc-acrobat-sdk-docs/pdfstandards/PDF32000_2008.pdf">PDF
 * 32000-1:2008</a>
 */
@FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
public final class FormWidgetInfo implements Parcelable {

    /** Represents a form widget type that is unknown */
    public static final int WIDGET_TYPE_UNKNOWN = 0;
    /** Represents a push button type form widget */
    public static final int WIDGET_TYPE_PUSHBUTTON = 1;
    /** Represents a checkbox type form widget */
    public static final int WIDGET_TYPE_CHECKBOX = 2;
    /** Represents a radio button type form widget */
    public static final int WIDGET_TYPE_RADIOBUTTON = 3;
    /** Represents a combobox type form widget */
    public static final int WIDGET_TYPE_COMBOBOX = 4;
    /** Represents a listbox type form widget */
    public static final int WIDGET_TYPE_LISTBOX = 5;
    /** Represents a text field type form widget */
    public static final int WIDGET_TYPE_TEXTFIELD = 6;
    /** Represents a signature type form widget */
    public static final int WIDGET_TYPE_SIGNATURE = 7;
    @NonNull
    public static final Creator<FormWidgetInfo> CREATOR =
            new Creator<>() {
                @Override
                public FormWidgetInfo createFromParcel(Parcel in) {
                    return new FormWidgetInfo(in);
                }

                @Override
                public FormWidgetInfo[] newArray(int size) {
                    return new FormWidgetInfo[size];
                }
            };
    private final @WidgetType int mWidgetType;
    private final int mWidgetIndex;
    private final Rect mWidgetRect;
    private final boolean mReadOnly;
    private final String mTextValue;
    private final String mAccessibilityLabel;
    private final boolean mEditableText; // Combobox only.
    private final boolean mMultiSelect; // Listbox only.
    private final boolean mMultiLineText; // Text Field only.
    private final int mMaxLength; // Text Field only.
    private final float mFontSize; // Editable Text only.
    private final List<ListItem> mListItems; // Combo/Listbox only.

    /**
     * Creates a new instance
     *
     * @hide
     */
    public FormWidgetInfo(
            @WidgetType int widgetType,
            int widgetIndex,
            @NonNull Rect widgetRect,
            boolean readOnly,
            @Nullable String textValue,
            @Nullable String accessibilityLabel,
            boolean editableText,
            boolean multiSelect,
            boolean multiLineText,
            int maxLength,
            float fontSize,
            List<ListItem> listItems) {
        this.mWidgetType = widgetType;
        this.mWidgetIndex = widgetIndex;
        this.mWidgetRect = widgetRect;
        this.mReadOnly = readOnly;
        this.mTextValue = textValue;
        this.mAccessibilityLabel = accessibilityLabel;
        this.mEditableText = editableText;
        this.mMultiSelect = multiSelect;
        this.mMultiLineText = multiLineText;
        this.mMaxLength = maxLength;
        this.mFontSize = fontSize;
        // Defensive copy
        this.mListItems = Collections.unmodifiableList(new ArrayList<>(listItems));
    }

    private FormWidgetInfo(Parcel in) {
        mWidgetType = in.readInt();
        mWidgetIndex = in.readInt();
        mWidgetRect = in.readParcelable(Rect.class.getClassLoader());
        mReadOnly = in.readInt() != 0;
        mTextValue = in.readString();
        mAccessibilityLabel = in.readString();
        mEditableText = in.readInt() != 0;
        mMultiSelect = in.readInt() != 0;
        mMultiLineText = in.readInt() != 0;
        mMaxLength = in.readInt();
        mFontSize = in.readFloat();
        ArrayList<ListItem> listItems = new ArrayList<>();
        in.readTypedList(listItems, ListItem.CREATOR);
        mListItems = Collections.unmodifiableList(listItems);
    }

    /** Returns the type of this widget */
    @WidgetType
    public int getWidgetType() {
        return mWidgetType;
    }

    /** Returns the index of the widget within the page's "Annot" array in the PDF document */
    @IntRange(from = 0)
    public int getWidgetIndex() {
        return mWidgetIndex;
    }

    /**
     * Returns the {@link Rect} in page coordinates occupied by the widget
     */
    @NonNull
    public Rect getWidgetRect() {
        return mWidgetRect;
    }

    /** Returns {@code true} if the widget is read-only */
    public boolean isReadOnly() {
        return mReadOnly;
    }

    /**
     * Returns the field's text value, if present
     *
     * <p><strong>Note:</strong> Comes from the "V" value in the annotation dictionary. See <a
     * href="https://opensource.adobe.com/dc-acrobat-sdk-docs/pdfstandards/pdfreference1.7old
     * .pdf">PDF
     * Spec 1.7 Table 8.69</a>
     * Table 8.69
     */
    @Nullable
    public String getTextValue() {
        return mTextValue;
    }

    /**
     * Returns the field's accessibility label, if present
     *
     * <p><strong>Note:</strong> Comes from the "TU" value in the annotation dictionary, if present,
     * or else the "T" value. See PDF Spec 1.7 Table 8.69
     */
    @Nullable
    public String getAccessibilityLabel() {
        return mAccessibilityLabel;
    }

    /** Returns {@code true} if the widget is editable text */
    public boolean isEditableText() {
        return mEditableText;
    }

    /**
     * Returns {@code true} if the widget supports selecting multiple values
     */
    public boolean isMultiSelect() {
        return mMultiSelect;
    }


    /**
     * Returns true if the widget supports multiple lines of text input
     */
    public boolean isMultiLineText() {
        return mMultiLineText;
    }

    /**
     * Returns the maximum length of text supported by a text input widget, or -1 for text inputs
     * without a maximum length and widgets that are not text inputs.
     */
    @IntRange(from = -1)
    public int getMaxLength() {
        return mMaxLength;
    }

    /**
     * Returns the font size in pixels for text input, or 0 for text inputs without a specified font
     * size and widgets that are not text inputs.
     */
    @FloatRange(from = 0f)
    public float getFontSize() {
        return mFontSize;
    }

    /**
     * Returns the list of choice options in the order that it was passed in, or an empty list for
     * widgets without choice options.
     */
    @NonNull
    public List<ListItem> getListItems() {
        return mListItems;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mWidgetType,
                mWidgetIndex,
                mWidgetRect,
                mReadOnly,
                mTextValue,
                mAccessibilityLabel,
                mEditableText,
                mMultiSelect,
                mMultiLineText,
                mMaxLength,
                mFontSize,
                mListItems);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FormWidgetInfo other) {
            return mWidgetType == other.mWidgetType
                    && mWidgetIndex == other.mWidgetIndex
                    && Objects.equals(mWidgetRect, other.mWidgetRect)
                    && mReadOnly == other.mReadOnly
                    && Objects.equals(mTextValue, other.mTextValue)
                    && Objects.equals(mAccessibilityLabel, other.mAccessibilityLabel)
                    && mEditableText == other.mEditableText
                    && mMultiSelect == other.mMultiSelect
                    && mMultiLineText == other.mMultiLineText
                    && mMaxLength == other.mMaxLength
                    && mFontSize == other.mFontSize
                    && mListItems.equals(other.mListItems);
        }
        return false;
    }

    @Override
    public String toString() {
        return "FormWidgetInfo{"
                + "\n\ttype=" + mWidgetType + "\n\tindex=" + mWidgetIndex + "\n\trect="
                + mWidgetRect + "\n\treadOnly=" + mReadOnly + "\n\ttextValue=" + mTextValue
                + "\n\taccessibilityLabel=" + mAccessibilityLabel + "\n\teditableText="
                + mEditableText + "\n\tmultiSelect=" + mMultiSelect + "\n\tmultiLineText="
                + mMultiLineText + "\n\tmaxLength=" + mMaxLength + "\n\tfontSize=" + mFontSize
                + "\n\tmChoiceOptions=" + mListItems + "\n}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mWidgetType);
        dest.writeInt(mWidgetIndex);
        dest.writeParcelable(mWidgetRect, flags);
        dest.writeInt(mReadOnly ? 1 : 0);
        dest.writeString(mTextValue);
        dest.writeString(mAccessibilityLabel);
        dest.writeInt(mEditableText ? 1 : 0);
        dest.writeInt(mMultiSelect ? 1 : 0);
        dest.writeInt(mMultiLineText ? 1 : 0);
        dest.writeInt(mMaxLength);
        dest.writeFloat(mFontSize);
        dest.writeTypedList(mListItems);
    }

    /**
     * Represents the type of a form widget
     *
     * @hide
     */
    @IntDef({
            WIDGET_TYPE_UNKNOWN,
            WIDGET_TYPE_PUSHBUTTON,
            WIDGET_TYPE_CHECKBOX,
            WIDGET_TYPE_RADIOBUTTON,
            WIDGET_TYPE_COMBOBOX,
            WIDGET_TYPE_LISTBOX,
            WIDGET_TYPE_TEXTFIELD,
            WIDGET_TYPE_SIGNATURE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface WidgetType {

    }

    /** Builder for {@link FormWidgetInfo} */
    public static final class Builder {
        private final @WidgetType int mWidgetType;
        private final int mWidgetIndex;
        private final Rect mWidgetRect;
        private final String mTextValue;
        private final String mAccessibilityLabel;
        private boolean mReadOnly = false;
        private boolean mEditableText = false; // Combobox only.
        private boolean mMultiSelect = false; // Listbox only.
        private boolean mMultiLineText = false; // Text Field only.
        private int mMaxLength = -1; // Text Field only.
        private float mFontSize = 0f; // Editable Text only.
        private List<ListItem> mListItems = List.of(); // Combo/Listbox only.

        /**
         * Creates an instance
         *
         * @param widgetType         the type of widget
         * @param widgetIndex        the index of the widget in the page's "Annot" array in the PDF
         * @param widgetRect         the {@link Rect} in page coordinates occupied by the widget
         * @param textValue          the widget's text value
         * @param accessibilityLabel the field's accessibility label
         * @throws NullPointerException if any of {@code widgetRect}, {@code textValue}, or {@code
         *                              accessibilityLabel} are null
         */
        public Builder(
                @WidgetType int widgetType,
                @IntRange(from = 0) int widgetIndex,
                @NonNull Rect widgetRect,
                @NonNull String textValue,
                @NonNull String accessibilityLabel) {
            mWidgetType = widgetType;
            mWidgetIndex = widgetIndex;
            mWidgetRect = Preconditions.checkNotNull(widgetRect, "widgetRect cannot be null");
            mTextValue = Preconditions.checkNotNull(textValue, "textValue cannot be null");
            mAccessibilityLabel = Preconditions.checkNotNull(accessibilityLabel,
                    "accessibilityLabel cannot be null");
        }

        /** Sets whether this widget is read-only */
        @NonNull
        public Builder setReadOnly(boolean readOnly) {
            mReadOnly = readOnly;
            return this;
        }

        /**
         * Sets whether this widget contains editable text. Only supported for comboboxes and
         * text fields
         *
         * @throws IllegalArgumentException if this is not a combobox or text field type widget
         */
        @NonNull
        public Builder setEditableText(boolean editableText) {
            Preconditions.checkArgument(mWidgetType == WIDGET_TYPE_COMBOBOX
                            || mWidgetType == WIDGET_TYPE_TEXTFIELD,
                    "Editable text is only supported on comboboxes and text fields");
            mEditableText = editableText;
            return this;
        }

        /**
         * Sets whether this widget supports multiple choice selections. Only supported for
         * list boxes
         *
         * @throws IllegalArgumentException if this is not a list box
         */
        @NonNull
        public Builder setMultiSelect(boolean multiSelect) {
            Preconditions.checkArgument(mWidgetType == WIDGET_TYPE_LISTBOX,
                    "Multi-select is only supported on list boxes");
            mMultiSelect = multiSelect;
            return this;
        }

        /**
         * Sets whether this widget supports multi-line text input. Only supported for text fields
         *
         * @throws IllegalArgumentException if this is not a text field
         */
        @NonNull
        public Builder setMultiLineText(boolean multiLineText) {
            Preconditions.checkArgument(mWidgetType == WIDGET_TYPE_TEXTFIELD,
                    "Multiline text is only supported on text fields");
            mMultiLineText = multiLineText;
            return this;
        }

        /**
         * Sets the maximum character length of input text supported by this widget. Only supported
         * for text fields
         *
         * @throws IllegalArgumentException if this is not a text field, or if a negative max length
         *                                  is supplied
         */
        @NonNull
        public Builder setMaxLength(@IntRange(from = 0) int maxLength) {
            Preconditions.checkArgument(maxLength > 0, "Invalid max length");
            Preconditions.checkArgument(mWidgetType == WIDGET_TYPE_TEXTFIELD,
                    "Max length is only supported on text fields");
            mMaxLength = maxLength;
            return this;
        }

        /**
         * Sets the font size for this widget. Only supported for text fields and comboboxes
         *
         * @throws IllegalArgumentException if this is not a combobox or text field, or if a
         *                                  negative font size is supplied
         */
        @NonNull
        public Builder setFontSize(@FloatRange(from = 0f) float fontSize) {
            Preconditions.checkArgument(fontSize > 0, "Invalid font size");
            Preconditions.checkArgument(mWidgetType == WIDGET_TYPE_COMBOBOX
                            || mWidgetType == WIDGET_TYPE_TEXTFIELD,
                    "Font size is only supported on comboboxes and text fields");
            mFontSize = fontSize;
            return this;
        }

        /**
         * Sets the choice options for this widget. Only supported for comboboxes and list boxes
         *
         * @throws IllegalArgumentException if this is not a combobox or list box
         * @throws NullPointerException     if {@code choiceOptions} is null
         */
        @NonNull
        public Builder setListItems(@NonNull List<ListItem> listItems) {
            Preconditions.checkNotNull(listItems, "choiceOptions cannot be null");
            Preconditions.checkArgument(mWidgetType == WIDGET_TYPE_COMBOBOX
                            || mWidgetType == WIDGET_TYPE_LISTBOX,
                    "Choice options are only supported on comboboxes and list boxes");
            mListItems = listItems;
            return this;
        }

        /** Builds a {@link FormWidgetInfo} */
        @NonNull
        public FormWidgetInfo build() {
            return new FormWidgetInfo(
                    mWidgetType,
                    mWidgetIndex,
                    mWidgetRect,
                    mReadOnly,
                    mTextValue,
                    mAccessibilityLabel,
                    mEditableText,
                    mMultiSelect,
                    mMultiLineText,
                    mMaxLength,
                    mFontSize,
                    mListItems);
        }
    }
}

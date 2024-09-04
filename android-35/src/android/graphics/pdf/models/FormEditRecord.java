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
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.Point;
import android.graphics.pdf.flags.Flags;
import android.graphics.pdf.utils.Preconditions;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;

/**
 * Record of a form filling operation that has been executed on a single form field in a PDF.
 * Contains the minimum amount of data required to replicate the action on the form.
 *
 * @see <a
 * href="https://opensource.adobe.com/dc-acrobat-sdk-docs/pdfstandards/PDF32000_2008.pdf">PDF
 * 32000-1:2008</a>
 */
@FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
public final class FormEditRecord implements Parcelable {

    /** Indicates a click on a clickable form widget */
    public static final int EDIT_TYPE_CLICK = 0;
    /** Represents setting indices on a combobox or listbox form widget */
    public static final int EDIT_TYPE_SET_INDICES = 1;
    /** Represents setting text on a text field or editable combobox form widget */
    public static final int EDIT_TYPE_SET_TEXT = 2;
    @NonNull
    public static final Creator<FormEditRecord> CREATOR =
            new Creator<FormEditRecord>() {
                @Override
                public FormEditRecord createFromParcel(Parcel in) {
                    return new FormEditRecord(in);
                }

                @Override
                public FormEditRecord[] newArray(int size) {
                    return new FormEditRecord[size];
                }
            };
    /** Represents the page number on which the edit occurred */
    private final int mPageNumber;

    /** Represents the index of the widget that was edited. */
    private final int mWidgetIndex;

    private final @EditType int mType;

    @Nullable
    private final Point mClickPoint;

    @NonNull
    private final int[] mSelectedIndices;

    @Nullable
    private final String mText;

    /** Private, use {@link Builder}. */
    private FormEditRecord(
            int pageNumber,
            int widgetIndex,
            @EditType int type,
            @Nullable Point clickPoint,
            @Nullable int[] selectedIndices,
            @Nullable String text) {
        this.mPageNumber = pageNumber;
        this.mWidgetIndex = widgetIndex;
        this.mType = type;
        this.mClickPoint = clickPoint;
        this.mSelectedIndices = Objects.requireNonNullElseGet(selectedIndices, () -> new int[0]);
        this.mText = text;
    }

    private FormEditRecord(@NonNull Parcel in) {
        mPageNumber = in.readInt();
        mWidgetIndex = in.readInt();
        mType = in.readInt();
        mClickPoint = in.readParcelable(Point.class.getClassLoader());

        int selectedIndicesSize = in.readInt();
        mSelectedIndices = new int[selectedIndicesSize];
        in.readIntArray(mSelectedIndices);

        mText = in.readString();
    }

    /**
     * @return the page on which the edit occurred
     */
    @IntRange(from = 0)
    public int getPageNumber() {
        return mPageNumber;
    }

    /**
     * @return the index of the widget within the page's "Annot" array in the PDF document
     */
    @IntRange(from = 0)
    public int getWidgetIndex() {
        return mWidgetIndex;
    }

    /** @return the type of the edit */
    @EditType
    public int getType() {
        return mType;
    }

    /**
     * @return the point on which the user tapped, if this record is of type {@link
     * #EDIT_TYPE_CLICK}, else null
     */
    @Nullable
    public Point getClickPoint() {
        return mClickPoint;
    }

    /**
     * @return the selected indices in the choice widget, if this record is of type {@link
     * #EDIT_TYPE_SET_INDICES}, else an empty array
     */
    @NonNull
    public int[] getSelectedIndices() {
        return mSelectedIndices;
    }

    /**
     * @return the text input by the user, if this record is of type {@link #EDIT_TYPE_SET_TEXT},
     * else null
     */
    @Nullable
    public String getText() {
        return mText;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mPageNumber);
        dest.writeInt(mWidgetIndex);
        dest.writeInt(mType);
        dest.writeParcelable(mClickPoint, flags);
        dest.writeInt(mSelectedIndices.length);
        dest.writeIntArray(mSelectedIndices);
        dest.writeString(mText);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FormEditRecord formEditRecord)) {
            return false;
        }

        return mPageNumber == formEditRecord.mPageNumber
                && mWidgetIndex == formEditRecord.mWidgetIndex
                && mType == formEditRecord.mType
                && Objects.equals(mClickPoint, formEditRecord.mClickPoint)
                && Objects.equals(mText, formEditRecord.mText)
                && Arrays.equals(mSelectedIndices, formEditRecord.mSelectedIndices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPageNumber, mWidgetIndex, mType, mClickPoint,
                Arrays.hashCode(mSelectedIndices), mText);
    }

    /**
     * Form edit operation type
     *
     * @hide
     */
    @IntDef({
            EDIT_TYPE_CLICK,
            EDIT_TYPE_SET_INDICES,
            EDIT_TYPE_SET_TEXT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EditType {
    }

    /** Builder for {@link FormEditRecord} */
    public static final class Builder {
        private final @EditType int mType;

        private final int mPageNumber;
        private final int mWidgetIndex;

        @Nullable
        private Point mClickPoint = null;

        @Nullable
        private int[] mSelectedIndices = null;

        @Nullable
        private String mText = null;

        /**
         * Creates a new instance.
         *
         * @param type        the type of {@link FormEditRecord} to create
         * @param pageNumber  the page number of which the record is
         * @param widgetIndex the index of the widget within the page's "Annot" array in the PDF
         * @throws IllegalArgumentException if a negative page number or widget index is provided
         */
        public Builder(
                @EditType int type,
                @IntRange(from = 0) int pageNumber,
                @IntRange(from = 0) int widgetIndex) {
            Preconditions.checkArgument(pageNumber >= 0, "Invalid pageNumber.");
            Preconditions.checkArgument(widgetIndex >= 0, "Invalid widgetIndex.");
            this.mType = type;
            this.mPageNumber = pageNumber;
            this.mWidgetIndex = widgetIndex;
        }

        /**
         * Builds this record
         *
         * @throws NullPointerException if the click point is not provided for a click type record,
         *                              if the selected indices are not provided for a set indices
         *                              type record, or if the text is
         *                              not provided for a set text type record
         */
        @NonNull
        public FormEditRecord build() {
            switch (mType) {
                case EDIT_TYPE_CLICK:
                    Preconditions.checkNotNull(
                            mClickPoint, "Cannot construct CLICK record without clickPoint.");
                    break;
                case EDIT_TYPE_SET_INDICES:
                    Preconditions.checkNotNull(
                            mSelectedIndices,
                            "Cannot construct SET_INDICES record without selectedIndices.");
                    break;
                case EDIT_TYPE_SET_TEXT:
                    Preconditions.checkNotNull(
                            mText, "Cannot construct SET_TEXT record without text.");
                    break;
            }
            return new FormEditRecord(
                    mPageNumber, mWidgetIndex, mType, mClickPoint, mSelectedIndices, mText);
        }

        /**
         * Sets the click point for this record
         *
         * @throws IllegalArgumentException if this is not a click type record
         */
        @NonNull
        public Builder setClickPoint(@Nullable Point clickPoint) {
            Preconditions.checkArgument(
                    mType == EDIT_TYPE_CLICK, "Cannot set clickPoint on a record of this type");
            Preconditions.checkNotNull(clickPoint, "Click point cannot be null");
            this.mClickPoint = clickPoint;
            return this;
        }

        /**
         * Sets the selected indices for this record
         *
         * @throws IllegalArgumentException if this is not a set indices type record
         */
        @NonNull
        public Builder setSelectedIndices(@Nullable int[] selectedIndices) {
            Preconditions.checkArgument(
                    mType == EDIT_TYPE_SET_INDICES,
                    "Cannot set selectedIndices on a record of this type.");
            this.mSelectedIndices = selectedIndices;
            return this;
        }

        /**
         * Sets the text for this record
         *
         * @throws IllegalArgumentException if this is not a set text type record
         */
        @NonNull
        public Builder setText(@Nullable String text) {
            Preconditions.checkArgument(
                    mType == EDIT_TYPE_SET_TEXT, "Cannot set text on a record of this type");
            this.mText = text;
            return this;
        }
    }
}

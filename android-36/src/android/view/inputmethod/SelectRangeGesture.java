/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.view.inputmethod;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.TextView;

import java.util.Objects;

/**
 * A subclass of {@link HandwritingGesture} for selecting a range of text by defining start and end
 * rectangles. This can be useful when the range cannot be defined with a single rectangle.
 * This class holds the information required for selection of text in
 * toolkit widgets like {@link TextView}.
 * <p>Note: this selects text within a range <em>between</em> two given areas. To select all text
 * <em>within</em> a single area, use {@link SelectGesture}</p>
 */
public final class SelectRangeGesture extends PreviewableHandwritingGesture implements Parcelable {

    private @Granularity int mGranularity;
    private RectF mStartArea;
    private RectF mEndArea;

    private SelectRangeGesture(
            int granularity, RectF startArea, RectF endArea, String fallbackText) {
        mType = GESTURE_TYPE_SELECT_RANGE;
        mStartArea = startArea;
        mEndArea = endArea;
        mGranularity = granularity;
        mFallbackText = fallbackText;
    }

    private SelectRangeGesture(@NonNull Parcel source) {
        mType = GESTURE_TYPE_SELECT_RANGE;
        mFallbackText = source.readString8();
        mGranularity = source.readInt();
        mStartArea = source.readTypedObject(RectF.CREATOR);
        mEndArea = source.readTypedObject(RectF.CREATOR);
    }

    /**
     * Returns Granular level on which text should be operated.
     * @see #GRANULARITY_CHARACTER
     * @see #GRANULARITY_WORD
     */
    @Granularity
    public int getGranularity() {
        return mGranularity;
    }

    /**
     * Returns the Selection start area {@link RectF} in screen coordinates.
     *
     * Getter for selection area set with {@link Builder#setSelectionStartArea(RectF)}.
     */
    @NonNull
    public RectF getSelectionStartArea() {
        return mStartArea;
    }

    /**
     * Returns the Selection end area {@link RectF} in screen coordinates.
     *
     * Getter for selection area set with {@link Builder#setSelectionEndArea(RectF)}.
     */
    @NonNull
    public RectF getSelectionEndArea() {
        return mEndArea;
    }

    /**
     * Builder for {@link SelectRangeGesture}. This class is not designed to be thread-safe.
     */
    public static final class Builder {
        private int mGranularity;
        private RectF mStartArea;
        private RectF mEndArea;
        private String mFallbackText;

        /**
         * Define text selection granularity. Intersecting words/characters will be
         * included in the operation.
         * @param granularity {@link HandwritingGesture#GRANULARITY_WORD} or
         * {@link HandwritingGesture#GRANULARITY_CHARACTER}.
         * @return {@link Builder}.
         */
        @NonNull
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setGranularity(@HandwritingGesture.Granularity int granularity) {
            mGranularity = granularity;
            return this;
        }

        /**
         * Set rectangular single/multiline start of text selection area intersecting with text.
         *
         * The resulting selection is performed from the start of first word/character in the start
         * rectangle to the end of the last word/character in the end rectangle
         * {@link #setSelectionEndArea(RectF)}.
         * <br/>
         * <img src="{@docRoot}reference/android/images/input_method/stylus_handwriting
         * /select_range_gesture_rects.png"
         * height="300" alt="Selection strategy using two rectangles"/>
         *  <br/>
         *
         * Intersection is determined using
         * {@link #setGranularity(int)}. e.g. {@link HandwritingGesture#GRANULARITY_WORD} includes
         * all the words with their width/height center included in the selection rectangle.
         * @param startArea {@link RectF} (in screen coordinates) for start of selection. This
         * rectangle belongs to first line where selection should start.
         */
        @NonNull
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setSelectionStartArea(@NonNull RectF startArea) {
            mStartArea = startArea;
            return this;
        }

        /**
         * Set rectangular single/multiline end of text selection area intersecting with text.
         *
         * The resulting selection is performed from the start of first word/character in the start
         * rectangle {@link #setSelectionStartArea(RectF)} to the end of the last word/character in
         * the end rectangle.
         * <br/>
         * <img src="{@docRoot}reference/android/images/input_method/stylus_handwriting
         * /select_range_gesture_rects.png"
         * height="300" alt="Selection strategy using two rectangles"/>
         *  <br/>
         *
         * The selection includes the first word/character in the rectangle, the last
         * word/character in the rectangle, and everything in between even if it's not in the
         * rectangle.
         *
         * Intersection is determined using
         * {@link #setGranularity(int)}. e.g. {@link HandwritingGesture#GRANULARITY_WORD} includes
         * all the words with their width/height center included in the selection rectangle.
         * @param endArea {@link RectF} (in screen coordinates) for start of selection. This
         * rectangle belongs to the last line where selection should end.
         */
        @NonNull
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setSelectionEndArea(@NonNull RectF endArea) {
            mEndArea = endArea;
            return this;
        }

        /**
         * Set fallback text that will be committed at current cursor position if there is no
         * applicable text beneath the area of gesture.
         * @param fallbackText text to set
         */
        @NonNull
        public Builder setFallbackText(@Nullable String fallbackText) {
            mFallbackText = fallbackText;
            return this;
        }

        /**
         * @return {@link SelectRangeGesture} using parameters in this
         * {@link SelectRangeGesture.Builder}.
         * @throws IllegalArgumentException if one or more positional parameters are not specified.
         */
        @NonNull
        public SelectRangeGesture build() {
            if (mStartArea == null || mStartArea.isEmpty() || mEndArea == null
                    || mEndArea.isEmpty()) {
                throw new IllegalArgumentException("Selection area must be set.");
            }
            if (mGranularity <= GRANULARITY_UNDEFINED) {
                throw new IllegalArgumentException("Selection granularity must be set.");
            }
            return new SelectRangeGesture(mGranularity, mStartArea, mEndArea, mFallbackText);
        }
    }

    /**
     * Used to make this class parcelable.
     */
    @NonNull
    public static final Parcelable.Creator<SelectRangeGesture> CREATOR =
            new Parcelable.Creator<SelectRangeGesture>() {
                @Override
                public SelectRangeGesture createFromParcel(Parcel source) {
                    return new SelectRangeGesture(source);
                }

                @Override
                public SelectRangeGesture[] newArray(int size) {
                    return new SelectRangeGesture[size];
                }
            };

    @Override
    public int hashCode() {
        return Objects.hash(mGranularity, mStartArea, mEndArea, mFallbackText);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SelectRangeGesture)) return false;

        SelectRangeGesture that = (SelectRangeGesture) o;

        if (mGranularity != that.mGranularity) return false;
        if (!Objects.equals(mFallbackText, that.mFallbackText)) return false;
        if (!Objects.equals(mStartArea, that.mStartArea)) return false;
        return Objects.equals(mEndArea, that.mEndArea);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Used to package this object into a {@link Parcel}.
     *
     * @param dest The {@link Parcel} to be written.
     * @param flags The flags used for parceling.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString8(mFallbackText);
        dest.writeInt(mGranularity);
        dest.writeTypedObject(mStartArea, flags);
        dest.writeTypedObject(mEndArea, flags);
    }
}

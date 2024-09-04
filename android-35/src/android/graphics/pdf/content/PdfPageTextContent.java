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

package android.graphics.pdf.content;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.graphics.RectF;
import android.graphics.pdf.flags.Flags;
import android.graphics.pdf.utils.Preconditions;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * <p>
 * Represents a continuous stream of text in a page of a PDF document in the order of viewing.
 */
@FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
public final class PdfPageTextContent implements Parcelable {
    private final String mText;
    private final List<RectF> mBounds;


    /**
     * Creates a new instance of {@link PdfPageTextContent} using the raw text on the page of the
     * document. By default, the bounds will be an empty list.
     *
     * @param text Text content on the page.
     * @throws NullPointerException If text is null.
     */
    public PdfPageTextContent(@NonNull String text) {
        Preconditions.checkNotNull(text, "Text cannot be null");
        this.mText = text;
        this.mBounds = List.of();
    }

    /**
     * Creates a new instance of {@link PdfPageTextContent} to represent text content within defined
     * bounds represented by a non-empty list of {@link RectF} on the page of the document.
     *
     * @param text   Text content within the bounds.
     * @param bounds Bounds for the text content
     * @throws NullPointerException If text or bounds is null.
     */
    public PdfPageTextContent(@NonNull String text, @NonNull List<RectF> bounds) {
        Preconditions.checkNotNull(text, "Text cannot be null");
        Preconditions.checkNotNull(bounds, "Bounds cannot be null");
        Preconditions.checkArgument(!bounds.isEmpty(), "Bounds cannot be empty");
        this.mText = text;
        this.mBounds = bounds;
    }

    private PdfPageTextContent(Parcel in) {
        mText = in.readString();
        mBounds = in.createTypedArrayList(RectF.CREATOR);
    }

    @NonNull
    public static final Creator<PdfPageTextContent> CREATOR = new Creator<PdfPageTextContent>() {
        @Override
        public PdfPageTextContent createFromParcel(Parcel in) {
            return new PdfPageTextContent(in);
        }

        @Override
        public PdfPageTextContent[] newArray(int size) {
            return new PdfPageTextContent[size];
        }
    };

    /**
     * Gets the text content on the document.
     *
     * @return The text content on the page.
     */
    @NonNull
    public String getText() {
        return mText;
    }

    /**
     * Gets the bounds for the text content represented as a list of {@link RectF}. Each
     * {@link RectF} represents text content in a single line defined in points (1/72") for its 4
     * corners. Content spread across multiple lines is represented by list of {@link RectF} in the
     * order of viewing (left to right and top to bottom). If the text content is unbounded then the
     * list will be empty.
     *
     * @return The bounds of the text content.
     */
    @NonNull
    public List<RectF> getBounds() {
        return mBounds;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@androidx.annotation.NonNull Parcel dest, int flags) {
        dest.writeString(mText);
        dest.writeTypedList(mBounds);
    }
}

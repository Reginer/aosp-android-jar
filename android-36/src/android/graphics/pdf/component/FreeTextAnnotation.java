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

package android.graphics.pdf.component;

import android.annotation.ColorInt;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.pdf.flags.Flags;

/**
 * Represents a free text annotation in a PDF document.
 * <p>
 * This class allows creating and manipulating free text
 * annotations. A free text annotation in a PDF is a type of
 * annotation that allows you to add text directly onto the page.
 * <p>
 * If text color is not set using
 * {@link #setTextColor(int)}, the default text color is
 * black and if the background color is not set using
 * {@link #setBackgroundColor(int)}, the default background color is
 * white.
 */
@FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_TEXT_ANNOTATIONS)
public final class FreeTextAnnotation extends PdfAnnotation {
    @NonNull private String mTextContent;
    private @ColorInt int mTextColor;
    private @ColorInt int mBackgroundColor;

    /**
     * Creates a new free text annotation with the specified bounds and text content.
     * <p>
     * The default text color and background color will be black and white respectively
     *
     * @param bounds The bounding rectangle of the annotation.
     * @param textContent The text content of the annotation
     */
    public FreeTextAnnotation(@NonNull RectF bounds, @NonNull String textContent) {
        super(PdfAnnotationType.FREETEXT, bounds);
        this.mTextContent = textContent;
        this.mTextColor = Color.BLACK;
        this.mBackgroundColor = Color.WHITE;
    }

    /**
     * Sets the text content of the annotation.
     *
     * @param text The new text content.
     */
    public void setTextContent(@NonNull String text) {
        mTextContent = text;
    }

    /**
     * Returns the text content of the freetext annotation.
     *
     * @return The text content.
     */
    @NonNull public String getTextContent() {
        return mTextContent;
    }

    /**
     * Sets the text color of the annotation.
     *
     * @param color The new text color.
     */
    public void setTextColor(@ColorInt int color) {
        this.mTextColor = color;
    }

    /**
     * Returns the text color of the freetext annotation.
     *
     * @return The text color.
     */
    public @ColorInt int getTextColor() {
        return mTextColor;
    }

    /**
     * Sets the background color of the freetext annotation.
     *
     * @param color The new background color.
     */
    public void setBackgroundColor(@ColorInt int color) {
        this.mBackgroundColor = color;
    }

    /**
     * Returns the background color of the freetext annotation.
     *
     * @return The background color.
     */
    public @ColorInt int getBackgroundColor() {
        return this.mBackgroundColor;
    }

}

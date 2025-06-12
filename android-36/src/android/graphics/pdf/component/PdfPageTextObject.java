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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.pdf.flags.Flags;

/**
 * Represents a text object on a PDF page.
 * This class extends PageObject and provides methods to access and modify the text content.
 */
@FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_TEXT_OBJECTS)
public final class PdfPageTextObject extends PdfPageObject {
    private String mText;
    private Typeface mTypeface;
    private float mFontSize;
    private Color mStrokeColor = new Color(); // Default is opaque black in the sRGB color space.
    private float mStrokeWidth = 1.0f;
    private Color mFillColor;

    /**
     * Constructor for the PdfPageTextObject.
     * Sets the object type to TEXT and initializes the text color to black.
     *
     * @param typeface The font of the text.
     * @param fontSize The font size of the text.
     */
    public PdfPageTextObject(@NonNull String text, @NonNull Typeface typeface, float fontSize) {
        super(PdfPageObjectType.TEXT);
        this.mText = text;
        this.mTypeface = typeface;
        this.mFontSize = fontSize;
    }

    /**
     * Returns the text content of the object.
     *
     * @return The text content.
     */
    @NonNull
    public String getText() {
        return mText;
    }

    /**
     * Sets the text content of the object.
     *
     * @param text The text content to set.
     */
    public void setText(@NonNull String text) {
        this.mText = text;
    }

    /**
     * Returns the font size of the object.
     *
     * @return The font size.
     */
    public float getFontSize() {
        return mFontSize;
    }

    /**
     * Sets the font size of the object.
     *
     * @param fontSize The font size to set.
     */
    public void setFontSize(float fontSize) {
        mFontSize = fontSize;
    }

    /**
     * Returns the stroke color of the object.
     *
     * @return The stroke color of the object.
     */
    @NonNull
    public Color getStrokeColor() {
        return mStrokeColor;
    }

    /**
     * Sets the stroke color of the object.
     *
     * @param strokeColor The stroke color of the object.
     */
    public void setStrokeColor(@NonNull Color strokeColor) {
        this.mStrokeColor = strokeColor;
    }

    /**
     * Returns the stroke width of the object.
     *
     * @return The stroke width of the object.
     */
    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    /**
     * Sets the stroke width of the object.
     *
     * @param strokeWidth The stroke width of the object.
     */
    public void setStrokeWidth(float strokeWidth) {
        this.mStrokeWidth = strokeWidth;
    }

    /**
     * Returns the font of the text.
     *
     * @return The font.
     */
    @NonNull
    public Typeface getTypeface() {
        return mTypeface;
    }

    /**
     * Sets the font of the text.
     *
     * @param typeface The font to set.
     */
    public void setTypeface(@NonNull Typeface typeface) {
        this.mTypeface = typeface;
    }

    /**
     * Returns the fill color of the object.
     *
     * @return The fill color of the object.
     */
    @Nullable
    public Color getFillColor() {
        return mFillColor;
    }

    /**
     * Sets the fill color of the object.
     *
     * @param fillColor The fill color of the object.
     */
    public void setFillColor(@Nullable Color fillColor) {
        this.mFillColor = fillColor;
    }
}

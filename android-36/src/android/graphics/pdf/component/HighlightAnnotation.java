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
 * Represents a highlight annotation in a PDF document.
 *
 * <p>If the highlight color is not explicitly set using {@link #setColor(Color)},
 * the default color is yellow.</p>
 */
@FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_ANNOTATIONS)
public final class HighlightAnnotation extends PdfAnnotation {
    private @ColorInt int mColor;

    /**
     * Creates a new highlight annotation with the specified bounds.
     * <p>
     * The default highlight color is yellow
     *
     * @param bounds The bounding rectangle of the annotation.
     */
    public HighlightAnnotation(@NonNull RectF bounds) {
        super(PdfAnnotationType.HIGHLIGHT, bounds);
        this.mColor = Color.YELLOW;
    }

    /**
     * Returns the highlight color of the annotation.
     *
     * @return The highlight color.
     */
    public @ColorInt int getColor() {
        return mColor;
    }

    /**
     * Sets the highlight color of the annotation.
     *
     * @param color The new highlight color.
     */
    public void setColor(@ColorInt int color) {
        this.mColor = color;
    }

}

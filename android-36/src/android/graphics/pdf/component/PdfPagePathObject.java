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
import android.graphics.Path;
import android.graphics.pdf.flags.Flags;

/**
 * Represents a path object on a PDF page. This class extends
 * {@link PdfPageObject} and provides methods to access and modify the
 * path's content, such as its shape, fill color, stroke color and line width.
 */
@FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_PAGE_OBJECTS)
public final class PdfPagePathObject extends PdfPageObject {
    private final Path mPath;
    private Color mStrokeColor;
    private float mStrokeWidth;
    private Color mFillColor;

    /**
     * Constructor for the PdfPagePathObject. Sets the object type
     * to {@link PdfPageObjectType#PATH}.
     */
    public PdfPagePathObject(@NonNull Path path) {
        super(PdfPageObjectType.PATH);
        this.mPath = path;
    }

    /**
     * Returns the path of the object.
     * The returned path object might be an approximation of the one used to
     * create the original one if the original object has elements with curvature.
     * <p>
     * Note: The path is immutable because the underlying library does
     * not allow modifying the path once it is created.
     *
     * @return The path.
     */
    @NonNull
    public Path toPath() {
        return new Path(mPath);
    }

    /**
     * Returns the stroke color of the object.
     *
     * @return The stroke color of the object.
     */
    @Nullable
    public Color getStrokeColor() {
        return mStrokeColor;
    }

    /**
     * Sets the stroke color of the object.
     *
     * @param strokeColor The stroke color of the object.
     */
    public void setStrokeColor(@Nullable Color strokeColor) {
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

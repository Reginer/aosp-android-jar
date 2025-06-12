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
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.graphics.RectF;
import android.graphics.pdf.flags.Flags;
import android.graphics.pdf.utils.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a stamp annotation in a PDF document.
 * <p>
 * Only path, image, or text objects created using the {@link PdfPagePathObject},
 * {@link PdfPageImageObject}, or {@link PdfPageTextObject} constructors respectively
 * can be added to a stamp annotation.
 */
@FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_STAMP_ANNOTATIONS)
public final class StampAnnotation extends PdfAnnotation {
    @NonNull private List<PdfPageObject> mObjects;

    /**
     * Creates a new stamp annotation with the specified bounds
     *
     * @param bounds The bounding rectangle of the annotation.
     */
    public StampAnnotation(@NonNull RectF bounds) {
        super(PdfAnnotationType.STAMP, bounds);
        mObjects = new ArrayList<>();
    }

    /**
     * Adds a PDF page object to the stamp annotation.
     * <p>
     * The page object should be a path, text or an image.
     *
     * @param pageObject The PDF page object to add.
     * @throws IllegalArgumentException if the page object is already added to a page or an
     *         annotation.
     */
    public void addObject(@NonNull PdfPageObject pageObject) {
        Preconditions.checkArgument(pageObject.getPdfObjectType() == PdfPageObjectType.TEXT
                        || pageObject.getPdfObjectType() == PdfPageObjectType.IMAGE
                        || pageObject.getPdfObjectType() == PdfPageObjectType.PATH,
                "Unsupported page object type");
        mObjects.add(pageObject);
    }


    /**
     * Returns all the known PDF page objects in the stamp annotation.
     *
     * @return The list of page objects in the annotation.
     */
    @NonNull
    public List<PdfPageObject> getObjects() {
        return mObjects;
    }

    /**
     * Remove the page object at the given index inside the stamp annotation. Here index is the
     * index of the page object in the list of page objects returned by {@link #getObjects()}
     *
     * @param index - index of the object to be removed
     * @throws IllegalArgumentException if there is no object in the annotation with the given
     *         id
     */
    public void removeObject(@IntRange(from = 0) int index) {
        Preconditions.checkArgument(index >= 0 && index < mObjects.size(),
                "Invalid Index");
        mObjects.remove(index);
    }
}

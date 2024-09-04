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

package android.graphics.pdf.models.jni;

import android.annotation.FlaggedApi;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.content.PdfPageTextContent;
import android.graphics.pdf.flags.Flags;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents text selection on a particular page of a PDF coming from the JNI layer.
 *
 * @hide
 */
// TODO(b/324536951): Remove this class after updating the native code to directly use
//  android.graphics.pdf.models.PageSelection
public class PageSelection {
    /** The page the selection is on. */
    private final int mPage;

    /** The left edge of the selection - index is inclusive. */
    private final SelectionBoundary mLeft;

    /** The right edge of the selection - index is exclusive. */
    private final SelectionBoundary mRight;

    /** The bounding boxes of the highlighted text. */
    private final List<Rect> mBounds;

    /** The highlighted text. */
    private final String mText;

    public PageSelection(int page, SelectionBoundary left, SelectionBoundary right,
            List<Rect> rects, String text) {
        this.mPage = page;
        this.mLeft = left;
        this.mRight = right;
        this.mBounds = rects;
        this.mText = text;
    }

    public int getPage() {
        return mPage;
    }

    public SelectionBoundary getLeft() {
        return mLeft;
    }

    public SelectionBoundary getRight() {
        return mRight;
    }

    public List<Rect> getBounds() {
        return mBounds;
    }

    public String getText() {
        return mText;
    }

    /** Converts JNI models to the public class */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public android.graphics.pdf.models.selection.PageSelection convert() {
        PdfPageTextContent selectedTextContent = new PdfPageTextContent(mText,
                mBounds.stream().map(RectF::new).collect(Collectors.toList()));
        return new android.graphics.pdf.models.selection.PageSelection(mPage, mLeft.convert(),
                mRight.convert(),
                /* selectedContents = */ List.of(selectedTextContent));
    }
}
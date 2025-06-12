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
import android.graphics.Point;
import android.graphics.pdf.flags.Flags;

/** @hide */
// TODO(b/324536951): Remove this class after updating the native code to directly use
//  android.graphics.pdf.models.SelectionBoundary
public class SelectionBoundary {
    private final int mIndex;

    private final int mX;

    private final int mY;

    private final boolean mIsRtl;

    public SelectionBoundary(int index, int x, int y, boolean isRtl) {
        mIndex = index;
        mX = x;
        mY = y;
        mIsRtl = isRtl;
    }

    /**
     * Converts the AOSP {@link android.graphics.pdf.models.selection.SelectionBoundary} to
     * {@link android.graphics.pdf.models.jni.SelectionBoundary}.
     *
     * @param selectionBoundary AOSP input
     * @return JNI output
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public static SelectionBoundary convert(
            android.graphics.pdf.models.selection.SelectionBoundary selectionBoundary) {
        if (selectionBoundary.getIndex() >= 0) {
            return new SelectionBoundary(
                    selectionBoundary.getIndex(), -1, -1, selectionBoundary.getIsRtl());
        }
        return new SelectionBoundary(-1,
                selectionBoundary.getPoint().x, selectionBoundary.getPoint().y,
                selectionBoundary.getIsRtl());
    }

    public int getIndex() {
        return mIndex;
    }

    public int getX() {
        return mX;
    }

    public int getY() {
        return mY;
    }

    public boolean isRtl() {
        return mIsRtl;
    }

    /** Converts JNI models to the public class */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public android.graphics.pdf.models.selection.SelectionBoundary convert() {
        if (mX >= 0 & mY >= 0) {
            return new android.graphics.pdf.models.selection.SelectionBoundary(
                    new Point(mX, mY));
        }
        return new android.graphics.pdf.models.selection.SelectionBoundary(mIndex);
    }
}

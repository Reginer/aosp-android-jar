/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.graphics.text;

import com.android.layoutlib.bridge.impl.DelegateManager;
import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import android.annotation.NonNull;
import android.graphics.BidiRenderer;
import android.graphics.Paint;
import android.graphics.Paint_Delegate;
import android.graphics.RectF;
import android.graphics.text.LineBreaker_Delegate.Builder;
import android.graphics.text.LineBreaker_Delegate.Run;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Delegate that provides implementation for native methods in
 * {@link android.graphics.text.MeasuredText}
 * <p/>
 * Through the layoutlib_create tool, selected methods of StaticLayout have been replaced
 * by calls to methods of the same name in this delegate class.
 *
 */
public class MeasuredText_Builder_Delegate {
    // ---- Builder delegate manager ----
    protected static final DelegateManager<MeasuredText_Builder_Delegate>
            sBuilderManager =
            new DelegateManager<>(MeasuredText_Builder_Delegate.class);

    protected final ArrayList<Run> mRuns = new ArrayList<>();

    @LayoutlibDelegate
    /*package*/ static long nInitBuilder() {
        return sBuilderManager.addNewDelegate(new MeasuredText_Builder_Delegate());
    }

    /**
     * Apply style to make native measured text.
     *
     * @param nativeBuilderPtr The native NativeMeasuredParagraph builder pointer.
     * @param paintPtr The native paint pointer to be applied.
     * @param start The start offset in the copied buffer.
     * @param end The end offset in the copied buffer.
     * @param isRtl True if the text is RTL.
     */
    @LayoutlibDelegate
    /*package*/ static void nAddStyleRun(long nativeBuilderPtr, long paintPtr, int start,
            int end, boolean isRtl) {
        MeasuredText_Builder_Delegate builder = sBuilderManager.getDelegate(nativeBuilderPtr);
        if (builder == null) {
            return;
        }
        builder.mRuns.add(new StyleRun(paintPtr, start, end, isRtl));
    }

    /**
     * Apply ReplacementRun to make native measured text.
     *
     * @param nativeBuilderPtr The native NativeMeasuredParagraph builder pointer.
     * @param paintPtr The native paint pointer to be applied.
     * @param start The start offset in the copied buffer.
     * @param end The end offset in the copied buffer.
     * @param width The width of the replacement.
     */
    @LayoutlibDelegate
    /*package*/ static void nAddReplacementRun(long nativeBuilderPtr, long paintPtr, int start,
            int end, float width) {
        MeasuredText_Builder_Delegate builder = sBuilderManager.getDelegate(nativeBuilderPtr);
        if (builder == null) {
            return;
        }
        builder.mRuns.add(new ReplacementRun(start, end, width));
    }

    @LayoutlibDelegate
    /*package*/ static long nBuildMeasuredText(long nativeBuilderPtr, long hintMtPtr,
            @NonNull char[] text, boolean computeHyphenation, boolean computeLayout) {
        MeasuredText_Delegate delegate = new MeasuredText_Delegate();
        delegate.mNativeBuilderPtr = nativeBuilderPtr;
        return MeasuredText_Delegate.sManager.addNewDelegate(delegate);
    }

    @LayoutlibDelegate
    /*package*/ static void nFreeBuilder(long nativeBuilderPtr) {
        sBuilderManager.removeJavaReferenceFor(nativeBuilderPtr);
    }

    private static float measureText(long nativePaint, char[] text, int index, int count,
            float[] widths, int bidiFlags) {
        Paint_Delegate paint = Paint_Delegate.getDelegate(nativePaint);
        RectF bounds =
                new BidiRenderer(null, paint, text).renderText(index, index + count, bidiFlags,
                        widths, 0, false);
        return bounds.right - bounds.left;
    }

    private static class StyleRun extends Run {
        private final long mNativePaint;
        private final boolean mIsRtl;

        private StyleRun(long nativePaint, int start, int end, boolean isRtl) {
            super(start, end);
            mNativePaint = nativePaint;
            mIsRtl = isRtl;
        }

        @Override
        void addTo(Builder builder) {
            int bidiFlags = mIsRtl ? Paint.BIDI_FORCE_RTL : Paint.BIDI_FORCE_LTR;
            measureText(mNativePaint, builder.mText, mStart, mEnd - mStart, builder.mWidths,
                    bidiFlags);
        }
    }

    private static class ReplacementRun extends Run {
        private final float mWidth;

        private ReplacementRun(int start, int end, float width) {
            super(start, end);
            mWidth = width;
        }

        @Override
        void addTo(Builder builder) {
            builder.mWidths[mStart] = mWidth;
            Arrays.fill(builder.mWidths, mStart + 1, mEnd, 0.0f);
        }
    }
}

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
import android.annotation.Nullable;
import android.icu.text.BreakIterator;
import android.text.Layout;
import android.text.Layout.BreakStrategy;
import android.text.Layout.HyphenationFrequency;
import android.graphics.text.Primitive.PrimitiveType;

import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.Segment;
import libcore.util.NativeAllocationRegistry_Delegate;

/**
 * Delegate that provides implementation for native methods in {@link android.text.StaticLayout}
 * <p/>
 * Through the layoutlib_create tool, selected methods of StaticLayout have been replaced
 * by calls to methods of the same name in this delegate class.
 *
 */
public class LineBreaker_Delegate {

    private static final char CHAR_SPACE     = 0x20;
    private static final char CHAR_TAB       = 0x09;
    private static final char CHAR_NEWLINE   = 0x0A;
    private static final char CHAR_ZWSP      = 0x200B;  // Zero width space.

    // ---- Builder delegate manager ----
    private static final DelegateManager<Builder> sBuilderManager =
        new DelegateManager<>(Builder.class);
    private static long sFinalizer = -1;

    // ---- Result delegate manager ----
    private static final DelegateManager<Result> sResultManager =
        new DelegateManager<>(Result.class);
    private static long sResultFinalizer = -1;

    @LayoutlibDelegate
    /*package*/ static long nInit(
            @BreakStrategy int breakStrategy,
            @HyphenationFrequency int hyphenationFrequency,
            boolean isJustified,
            @Nullable int[] indents) {
        Builder builder = new Builder();
        builder.mBreakStrategy = breakStrategy;
        return sBuilderManager.addNewDelegate(builder);
    }

    @LayoutlibDelegate
    /*package*/ static long nGetReleaseFunc() {
        synchronized (MeasuredText_Delegate.class) {
            if (sFinalizer == -1) {
                sFinalizer = NativeAllocationRegistry_Delegate.createFinalizer(
                        sBuilderManager::removeJavaReferenceFor);
            }
        }
        return sFinalizer;
    }

    @LayoutlibDelegate
    /*package*/ static long nComputeLineBreaks(
            /* non zero */ long nativePtr,

            // Inputs
            @NonNull char[] text,
            long measuredTextPtr,
            int length,
            float firstWidth,
            int firstWidthLineCount,
            float restWidth,
            @Nullable float[] variableTabStops,
            float defaultTabStop,
            int indentsOffset) {
        Builder builder = sBuilderManager.getDelegate(nativePtr);
        if (builder == null) {
            return 0;
        }

        builder.mText = text;
        builder.mWidths = new float[length];
        builder.mLineWidth = new LineWidth(firstWidth, firstWidthLineCount, restWidth);
        builder.mTabStopCalculator = new TabStops(variableTabStops, defaultTabStop);

        MeasuredText_Delegate.computeRuns(measuredTextPtr, builder);

        // compute all possible breakpoints.
        BreakIterator it = BreakIterator.getLineInstance();
        it.setText((CharacterIterator) new Segment(builder.mText, 0, length));

        // average word length in english is 5. So, initialize the possible breaks with a guess.
        List<Integer> breaks = new ArrayList<Integer>((int) Math.ceil(length / 5d));
        int loc;
        it.first();
        while ((loc = it.next()) != BreakIterator.DONE) {
            breaks.add(loc);
        }

        List<Primitive> primitives =
                computePrimitives(builder.mText, builder.mWidths, length, breaks);
        switch (builder.mBreakStrategy) {
            case Layout.BREAK_STRATEGY_SIMPLE:
                builder.mLineBreaker = new GreedyLineBreaker(primitives, builder.mLineWidth,
                        builder.mTabStopCalculator);
                break;
            case Layout.BREAK_STRATEGY_HIGH_QUALITY:
                // TODO
//                break;
            case Layout.BREAK_STRATEGY_BALANCED:
                builder.mLineBreaker = new OptimizingLineBreaker(primitives, builder.mLineWidth,
                        builder.mTabStopCalculator);
                break;
            default:
                assert false : "Unknown break strategy: " + builder.mBreakStrategy;
                builder.mLineBreaker = new GreedyLineBreaker(primitives, builder.mLineWidth,
                        builder.mTabStopCalculator);
        }
        Result result = new Result(builder.mLineBreaker.computeBreaks());
        return sResultManager.addNewDelegate(result);
    }

    @LayoutlibDelegate
    /*package*/ static int nGetLineCount(long ptr) {
        Result result = sResultManager.getDelegate(ptr);
        return result.mResult.mLineBreakOffset.size();
    }

    @LayoutlibDelegate
    /*package*/ static int nGetLineBreakOffset(long ptr, int idx) {
        Result result = sResultManager.getDelegate(ptr);
        return result.mResult.mLineBreakOffset.get(idx);
    }

    @LayoutlibDelegate
    /*package*/ static float nGetLineWidth(long ptr, int idx) {
        Result result = sResultManager.getDelegate(ptr);
        return result.mResult.mLineWidths.get(idx);
    }

    @LayoutlibDelegate
    /*package*/ static float nGetLineAscent(long ptr, int idx) {
        Result result = sResultManager.getDelegate(ptr);
        return result.mResult.mLineAscents.get(idx);
    }

    @LayoutlibDelegate
    /*package*/ static float nGetLineDescent(long ptr, int idx) {
        Result result = sResultManager.getDelegate(ptr);
        return result.mResult.mLineDescents.get(idx);
    }

    @LayoutlibDelegate
    /*package*/ static int nGetLineFlag(long ptr, int idx) {
        Result result = sResultManager.getDelegate(ptr);
        return result.mResult.mLineFlags.get(idx);
    }

    @LayoutlibDelegate
    /*package*/ static long nGetReleaseResultFunc() {
        synchronized (MeasuredText_Delegate.class) {
            if (sResultFinalizer == -1) {
                sResultFinalizer = NativeAllocationRegistry_Delegate.createFinalizer(
                        sBuilderManager::removeJavaReferenceFor);
            }
        }
        return sResultFinalizer;
    }

    /**
     * Compute metadata each character - things which help in deciding if it's possible to break
     * at a point or not.
     */
    @NonNull
    private static List<Primitive> computePrimitives(@NonNull char[] text, @NonNull float[] widths,
            int length, @NonNull List<Integer> breaks) {
        // Initialize the list with a guess of the number of primitives:
        // 2 Primitives per non-whitespace char and approx 5 chars per word (i.e. 83% chars)
        List<Primitive> primitives = new ArrayList<Primitive>(((int) Math.ceil(length * 1.833)));
        int breaksSize = breaks.size();
        int breakIndex = 0;
        for (int i = 0; i < length; i++) {
            char c = text[i];
            if (c == CHAR_SPACE || c == CHAR_ZWSP) {
                primitives.add(PrimitiveType.GLUE.getNewPrimitive(i, widths[i]));
            } else if (c == CHAR_TAB) {
                primitives.add(PrimitiveType.VARIABLE.getNewPrimitive(i));
            } else if (c != CHAR_NEWLINE) {
                while (breakIndex < breaksSize && breaks.get(breakIndex) < i) {
                    breakIndex++;
                }
                Primitive p;
                if (widths[i] != 0) {
                    if (breakIndex < breaksSize && breaks.get(breakIndex) == i) {
                        p = PrimitiveType.PENALTY.getNewPrimitive(i, 0, 0);
                    } else {
                        p = PrimitiveType.WORD_BREAK.getNewPrimitive(i, 0);
                    }
                    primitives.add(p);
                }

                primitives.add(PrimitiveType.BOX.getNewPrimitive(i, widths[i]));
            }
        }
        // final break at end of everything
        primitives.add(
                PrimitiveType.PENALTY.getNewPrimitive(length, 0, -PrimitiveType.PENALTY_INFINITY));
        return primitives;
    }

    // TODO: Rename to LineBreakerRef and move everything other than LineBreaker to LineBreaker.
    /**
     * Java representation of the native Builder class.
     */
    public static class Builder {
        char[] mText;
        float[] mWidths;
        private BaseLineBreaker mLineBreaker;
        private int mBreakStrategy;
        private LineWidth mLineWidth;
        private TabStops mTabStopCalculator;
    }

    public abstract static class Run {
        int mStart;
        int mEnd;

        Run(int start, int end) {
            mStart = start;
            mEnd = end;
        }

        abstract void addTo(Builder builder);
    }

    public static class Result {
        final BaseLineBreaker.Result mResult;
        public Result(BaseLineBreaker.Result result) {
            mResult = result;
        }
    }
}

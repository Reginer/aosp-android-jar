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
import android.graphics.pdf.flags.Flags;
import android.graphics.pdf.models.PageMatchBounds;
import android.graphics.pdf.utils.Preconditions;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the bounds of search matches as a {@code List<List<Rect>>}, where
 * the first {@code List<Rect>} is all the rectangles needed to bound the
 * first match, and so on. Most matches will be surrounded with a single Rect.
 * <p>
 * Internally, data is stored as 1-dimensional Lists, to avoid the overhead of
 * a large amount of single-element lists.
 * <p>
 * Also contains data about the character index of each match - so {@link #get}
 * returns the rectangles that bound the match, and {@link #getCharIndexes}
 * returns the character index that the match starts at.
 *
 * @hide
 */
// TODO(b/324536951): Remove this class after updating the native code to directly use
//  PageMatchBounds
public class MatchRects extends ListOfList<Rect> {
    @NonNull
    public static final MatchRects NO_MATCHES = new MatchRects(Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList());

    private final List<Rect> mRects;
    private final List<Integer> mMatchToRect;
    private final List<Integer> mCharIndexes;

    public MatchRects(@NonNull List<Rect> rects, @NonNull List<Integer> matchToRect,
            @NonNull List<Integer> charIndexes) {
        super(rects, matchToRect);
        this.mRects = Preconditions.checkNotNull(rects, "rects cannot be null");
        this.mMatchToRect = Preconditions.checkNotNull(matchToRect, "matchToRect cannot be null");
        this.mCharIndexes = Preconditions.checkNotNull(charIndexes, "charIndexes cannot be null");
    }

    public List<Rect> getRects() {
        return mRects;
    }

    public List<Integer> getMatchToRect() {
        return mMatchToRect;
    }

    public List<Integer> getCharIndexes() {
        return mCharIndexes;
    }

    /**
     * Un-flattens the list and converts to the public class.
     * <p>As an example, in case there are 2 matches on the page of the document with the 1st match
     * overflowing to the next line, the {@link LinkRects} would have the following values -
     * <pre>
     * MatchRects(
     *      mRects=[Rect(l1, t1, r1, b1), Rect(l2, t2, r2, b2), Rect(l3, t3, r3, b3)],
     *      mMatchToRect=[0,2,3],
     *      mCharIndexes=[1, 3]
     * )
     *
     * // In this case, the first match is represented by the first two {@link Rect}. The mapping to
     * // these Rect is done through the {@code mMatchToRect} array. This is the flattened
     * // representation of the matches and bounds. Using the method below, we can un-flatten this
     * // to the following representation -
     * List(
     *      PageMatchBounds(
     *          bounds = [Rect(l1, t1, r1, b1), Rect(l2, t2, r2, b2)],
     *          mTextStartIndex = 1
     *      ),
     *      PageMatchBounds(
     *          bounds = [Rect(l3, t3, r3, b3)],
     *          mTextStartIndex = 3
     *      ),
     * )
     * </pre>
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public List<PageMatchBounds> unflattenToList() {
        List<PageMatchBounds> matches = new ArrayList<>();
        for (int index = 0; index < mMatchToRect.size(); index++) {
            List<Rect> bounds = new ArrayList<>();
            int boundsForCurrentMatch = (index + 1 < mMatchToRect.size()) ? mMatchToRect.get(
                    index + 1) : mRects.size();
            for (int boundIndex = mMatchToRect.get(index); boundIndex < boundsForCurrentMatch;
                    boundIndex++) {
                bounds.add(mRects.get(boundIndex));
            }
            matches.add(new PageMatchBounds(
                    bounds.stream().map(RectF::new).collect(Collectors.toList()),
                    mCharIndexes.get(index)));
        }

        return matches;
    }
}

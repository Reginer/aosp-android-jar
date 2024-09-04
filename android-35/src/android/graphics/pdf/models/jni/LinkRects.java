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
import android.annotation.NonNull;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.content.PdfPageLinkContent;
import android.graphics.pdf.flags.Flags;
import android.graphics.pdf.utils.Preconditions;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the bounds of links as a {@code List<List<Rect>>}, where
 * the first {@code List<Rect>} is all of the rectangles needed to bound the
 * first link, and so on. Most links will be surrounded with a single Rect.
 * <p>
 * Internally, data is stored as 1-dimensional Lists, to avoid the overhead of
 * a large amount of single-element lists.
 * <p>
 * Also contains the URL index of each link - so {@link #get} returns the
 * rectangles that bound the link, and {@link #getUrls} returns the URLs that is
 * linked to.
 *
 * @hide
 */
// TODO(b/324536951): Remove this class after updating the native code to directly use
//  PdfPageLinkContent
public class LinkRects extends ListOfList<Rect> {
    /** Required at the JNI layer to detect no links and return an empty {@link LinkRects} */
    public static final LinkRects NO_LINKS = new LinkRects(Collections.<Rect>emptyList(),
            Collections.<Integer>emptyList(), Collections.<String>emptyList());
    private final List<Rect> mRects;
    private final List<Integer> mLinkToRect;
    private final List<String> mUrls;

    public LinkRects(@NonNull List<Rect> rects, @NonNull List<Integer> linkToRect,
            @NonNull List<String> urls) {
        super(rects, linkToRect);
        this.mRects = Preconditions.checkNotNull(rects, "rects cannot be null");
        this.mLinkToRect = Preconditions.checkNotNull(linkToRect, "linkToRect cannot be null");
        this.mUrls = Preconditions.checkNotNull(urls, "urls cannot be null");
    }

    /** Returns the list of bounds for the embedded weblinks. */
    public List<Rect> getRects() {
        return mRects;
    }

    /**
     * Returns the mapping list of bounds to the consecutive link.
     *
     * @see #unflattenToList()
     */
    public List<Integer> getLinkToRect() {
        return mLinkToRect;
    }

    /** Returns the list of embedded links on the page of the document. */
    public List<String> getUrls() {
        return mUrls;
    }

    /**
     * Un-flattens the list and converts to the public class.
     * <p>As an example, in case there are 2 weblinks on the page of the document with the 1st link
     * overflowing to the next line, the {@link LinkRects} would have the following values -
     * <pre>
     * LinkRects(
     *      mRects=[Rect(l1, t1, r1, b1), Rect(l2, t2, r2, b2), Rect(l3, t3, r3, b3)],
     *      mLinkToRect=[0,2,3],
     *      mUrls=[url1, url2]
     * )
     *
     * // In this case, the first link is represented by the first two {@link Rect}. The mapping to
     * // these Rect is done through the {@code mLinkToRect} array. This is the flattened
     * // representation of the links and bounds. Using the method below, we can un-flatten this
     * // to the following representation -
     * List(
     *      PdfPageLinkContent(
     *          bounds = [Rect(l1, t1, r1, b1), Rect(l2, t2, r2, b2)],
     *          url = url1
     *      ),
     *      PdfPageLinkContent(
     *          bounds = [Rect(l3, t3, r3, b3)],
     *          url = url2
     *      ),
     * )
     * </pre>
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public List<PdfPageLinkContent> unflattenToList() {
        List<PdfPageLinkContent> boundedLinks = new ArrayList<>();
        for (int index = 0; index < mLinkToRect.size(); index++) {
            List<Rect> bounds = new ArrayList<>();
            int boundsForCurrentLink = (index + 1 < mLinkToRect.size()) ? mLinkToRect.get(index + 1)
                    : mRects.size();
            for (int boundIndex = mLinkToRect.get(index); boundIndex < boundsForCurrentLink;
                    boundIndex++) {
                bounds.add(mRects.get(boundIndex));
            }
            boundedLinks.add(new PdfPageLinkContent(
                    bounds.stream().map(RectF::new).collect(Collectors.toList()),
                    Uri.parse(mUrls.get(index))));
        }

        return boundedLinks;
    }
}

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

package android.graphics.pdf.content;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.graphics.RectF;
import android.graphics.pdf.flags.Flags;
import android.graphics.pdf.utils.Preconditions;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * <p>
 * Represents the bounds and link on a page of the PDF document. Weblinks are those links implicitly
 * embedded in PDF pages.
 * <strong>Note:</strong> Only weblinks that are embedded will be supported. Links encoded as
 * plain text will be returned as part of {@link PdfPageTextContent}.
 */
@FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
public final class PdfPageLinkContent implements Parcelable {
    @NonNull
    public static final Creator<PdfPageLinkContent> CREATOR = new Creator<PdfPageLinkContent>() {
        @Override
        public PdfPageLinkContent createFromParcel(Parcel in) {
            return new PdfPageLinkContent(in);
        }

        @Override
        public PdfPageLinkContent[] newArray(int size) {
            return new PdfPageLinkContent[size];
        }
    };
    private final List<RectF> mBounds;
    private final Uri mUri;

    /**
     * Creates a new instance of {@link PdfPageLinkContent} using the embedded {@link Uri} and the
     * bounds of the uri.
     *
     * @param bounds Bounds which envelop the URI.
     * @param uri    Uri embedded in the PDF document.
     * @throws NullPointerException     If bounds or uri is null.
     * @throws IllegalArgumentException If the bounds list is empty.
     */
    public PdfPageLinkContent(@NonNull List<RectF> bounds, @NonNull Uri uri) {
        Preconditions.checkNotNull(bounds, "Bounds cannot be null");
        Preconditions.checkArgument(!bounds.isEmpty(), "Link bounds cannot be empty");
        Preconditions.checkNotNull(uri, "Uri cannot be null");
        this.mBounds = bounds;
        this.mUri = uri;
    }

    private PdfPageLinkContent(Parcel in) {
        mBounds = in.createTypedArrayList(RectF.CREATOR);
        mUri = in.readParcelable(Uri.class.getClassLoader());
    }

    /**
     * <p>
     * Gets the bounds of the embedded weblink represented as a list of {@link RectF}. Links which
     * are spread across multiple lines will be surrounded by multiple {@link RectF} in order of
     * viewing.
     * <p><strong>Note:</strong> Each {@link RectF} represents a bound of the weblink in a single
     * line and defines the coordinates of its 4 edges (left, top, right and bottom) in
     * points (1/72"). The developer will need to render the highlighter as well as intercept the
     * touch events for functionalities such as clicking the link.
     *
     * @return The bounds of the link.
     */
    @NonNull
    public List<RectF> getBounds() {
        return mBounds;
    }

    /**
     * <p>
     * Gets the weblink on the page of the PDF document. Weblinks are those links implicitly
     * embedded in PDF pages.
     * <strong>Note:</strong> Only weblinks that are embedded will be supported. Links encoded as
     * plain text will be returned as part of {@link PdfPageTextContent}.
     *
     * @return The weblink embedded on the page.
     */
    @NonNull
    public Uri getUri() {
        return mUri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@androidx.annotation.NonNull Parcel dest, int flags) {
        dest.writeTypedList(mBounds);
        dest.writeParcelable(mUri, flags);
    }
}

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

package android.graphics.pdf.models.selection;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.graphics.pdf.content.PdfPageTextContent;
import android.graphics.pdf.flags.Flags;
import android.graphics.pdf.utils.Preconditions;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * <p>
 * Represents the list of selected content on a particular page of the PDF document. By
 * default, the selection boundary is represented from left to right.
 * <strong>Note: </strong>Currently supports text selection only.
 */
@FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
public final class PageSelection implements Parcelable {
    @NonNull
    public static final Creator<PageSelection> CREATOR = new Creator<PageSelection>() {
        @Override
        public PageSelection createFromParcel(Parcel in) {
            return new PageSelection(in);
        }

        @Override
        public PageSelection[] newArray(int size) {
            return new PageSelection[size];
        }
    };

    private final int mPage;
    private final SelectionBoundary mStart;
    private final SelectionBoundary mStop;
    private final List<PdfPageTextContent> mSelectedContents;

    /**
     * Creates a new instance of {@link PageSelection} for the specified page, the start and stop
     * selection boundary, and the selected text content.
     *
     * @param page             The page number of the selection.
     * @param start            Boundary where the selection starts.
     * @param stop             Boundary where the selection stops.
     * @param selectedContents list of segments of selected text content.
     * @throws IllegalArgumentException If the page number is negative.
     * @throws NullPointerException     If start/stop edge or text selection is null.
     */
    public PageSelection(int page, @NonNull SelectionBoundary start,
            @NonNull SelectionBoundary stop, @NonNull List<PdfPageTextContent> selectedContents) {
        Preconditions.checkArgument(page >= 0, "Page number cannot be negative");
        Preconditions.checkNotNull(start, "Start boundary cannot be null");
        Preconditions.checkNotNull(stop, "Stop boundary cannot be null");
        Preconditions.checkNotNull(selectedContents, "Selected text content " + "cannot be null");
        this.mStart = start;
        this.mStop = stop;
        this.mPage = page;
        this.mSelectedContents = selectedContents;
    }

    private PageSelection(Parcel in) {
        mPage = in.readInt();
        mStart = in.readParcelable(SelectionBoundary.class.getClassLoader());
        mStop = in.readParcelable(SelectionBoundary.class.getClassLoader());
        mSelectedContents = in.createTypedArrayList(PdfPageTextContent.CREATOR);
    }

    /**
     * Gets the particular page for which the selection is highlighted.
     *
     * @return The page number on which the current selection resides.
     */
    public int getPage() {
        return mPage;
    }

    /**
     * <p>
     * Gets the edge from where the selection starts- index is inclusive.
     *
     * @return The starting edge of the selection.
     */
    @NonNull
    public SelectionBoundary getStart() {
        return mStart;
    }

    /**
     * <p>
     * Gets the edge where the selection stops - index is inclusive.
     *
     * @return The stopping edge of the selection.
     */
    @NonNull
    public SelectionBoundary getStop() {
        return mStop;
    }

    /**
     * Returns the text content within the selection boundaries on the page. In case there are
     * non-continuous selections, this method returns the list of those text content in order of
     * viewing.
     *
     * @return list of text contents within the selection boundaries.
     */
    @NonNull
    public List<PdfPageTextContent> getSelectedTextContents() {
        return mSelectedContents;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@androidx.annotation.NonNull Parcel dest, int flags) {
        dest.writeInt(mPage);
        dest.writeParcelable(mStart, flags);
        dest.writeParcelable(mStop, flags);
        dest.writeTypedList(mSelectedContents);
    }
}

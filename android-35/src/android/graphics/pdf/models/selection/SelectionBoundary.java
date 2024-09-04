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
import android.annotation.Nullable;
import android.graphics.Point;
import android.graphics.pdf.content.PdfPageTextContent;
import android.graphics.pdf.flags.Flags;
import android.graphics.pdf.utils.Preconditions;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents one edge of the selected content.
 */
@FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
public final class SelectionBoundary implements Parcelable {
    @NonNull
    public static final Creator<SelectionBoundary> CREATOR = new Creator<SelectionBoundary>() {
        @Override
        public SelectionBoundary createFromParcel(Parcel in) {
            return new SelectionBoundary(in);
        }

        @Override
        public SelectionBoundary[] newArray(int size) {
            return new SelectionBoundary[size];
        }
    };
    private final int mIndex;
    private final Point mPoint;

    private final boolean mIsRtl;

    /**
     * <p>
     * Create a new instance of {@link SelectionBoundary} if index of boundary and isRtl is known.
     * The text returned by {@link PdfPageTextContent#getText()} form a "stream" and inside this
     * "stream" each character has an index.
     *
     * @param index index of the selection boundary
     * @param isRtl Determines whether the direction of selection is right-to-left (rtl) or reverse
     * @throws IllegalArgumentException If the index is negative
     * @hide
     */
    public SelectionBoundary(int index, boolean isRtl) {
        Preconditions.checkArgument(index >= 0, "Index cannot be negative");
        this.mIndex = index;
        this.mPoint = null;
        this.mIsRtl = isRtl;
    }

    /**
     * <p>
     * Create a new instance of {@link SelectionBoundary} if index of boundary is known. The text
     * returned by {@link PdfPageTextContent#getText()} form a "stream" and inside this "stream"
     * each character has an index.
     * <strong>Note: </strong>Point defaults to {@code null} in this case.
     *
     * @param index index of the selection boundary.
     * @throws IllegalArgumentException If the index is negative.
     */
    public SelectionBoundary(int index) {
        Preconditions.checkArgument(index >= 0, "Index cannot be negative");
        this.mIndex = index;
        this.mPoint = null;
        this.mIsRtl = false;
    }

    /**
     * Creates a new instance of {@link SelectionBoundary} if the boundary and isRTL is known.
     *
     * @param point The point of selection boundary.
     * @param isRtl Determines whether the direction of selection is right-to-left (rtl) or reverse
     * @throws NullPointerException If the point is null
     * @hide
     */
    public SelectionBoundary(@NonNull Point point, boolean isRtl) {
        Preconditions.checkNotNull(point, "Point cannot be null");
        this.mIndex = -1;
        this.mPoint = point;
        this.mIsRtl = isRtl;
    }

    /**
     * Create a new instance of {@link SelectionBoundary} if the boundary {@link Point} is known.
     * Index defaults to -1.
     *
     * @param point The point of selection boundary.
     * @throws NullPointerException If the point is null.
     */
    public SelectionBoundary(@NonNull Point point) {
        Preconditions.checkNotNull(point, "Point cannot be null");
        this.mIndex = -1;
        this.mPoint = point;
        this.mIsRtl = false;
    }

    private SelectionBoundary(Parcel in) {
        mIndex = in.readInt();
        mPoint = in.readParcelable(Point.class.getClassLoader());
        mIsRtl = in.readBoolean();
    }

    /**
     * Gets the index of the text as determined by the text stream processed. If the value is -1
     * then the {@link #getPoint()} will determine the selection boundary.
     *
     * @return index of the selection boundary.
     */
    public int getIndex() {
        return mIndex;
    }

    /**
     * Gets the x, y coordinates of the selection boundary in points (1/72"). These coordinates are
     * represented by a {@link Point} . If the value is {@code null} then the {@link #getIndex()}
     * will determine the selection boundary.
     * <p><strong>Note:</strong> Point (0,0) represents the top-left corner of the page
     *
     * @return The point of the selection boundary.
     */
    @Nullable
    public Point getPoint() {
        return mPoint;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@androidx.annotation.NonNull Parcel dest, int flags) {
        dest.writeInt(mIndex);
        dest.writeParcelable(mPoint, flags);
        dest.writeBoolean(mIsRtl);
    }

    /**
     * Gets whether the direction of selection is right-to-left (rtl) or reverse. The value of isRtl
     * is determined by the underlying native layer using the start and stop boundaries.
     *
     * @return The direction of selection
     */
    public boolean getIsRtl() {
        return mIsRtl;
    }
}

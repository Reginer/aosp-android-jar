/*
 * Copyright 2020 The Android Open Source Project
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

package android.app.appsearch;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;

/**
 * This class represents a page of {@link SearchResult}s
 *
 * @hide
 */
@SafeParcelable.Class(creator = "SearchResultPageCreator")
public class SearchResultPage extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<SearchResultPage> CREATOR =
            new SearchResultPageCreator();

    @Field(id = 1, getter = "getNextPageToken")
    private final long mNextPageToken;

    @Nullable
    @Field(id = 2, getter = "getResults")
    private final List<SearchResult> mResults;

    @Constructor
    public SearchResultPage(
            @Param(id = 1) long nextPageToken,
            @Param(id = 2) @Nullable List<SearchResult> results) {
        mNextPageToken = nextPageToken;
        mResults = results;
    }

    /** Default constructor for {@link SearchResultPage}. */
    public SearchResultPage() {
        mNextPageToken = 0;
        mResults = Collections.emptyList();
    }

    /** Returns the Token to get next {@link SearchResultPage}. */
    public long getNextPageToken() {
        return mNextPageToken;
    }

    /** Returns all {@link android.app.appsearch.SearchResult}s of this page */
    @NonNull
    public List<SearchResult> getResults() {
        if (mResults == null) {
            return Collections.emptyList();
        }
        return mResults;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SearchResultPageCreator.writeToParcel(this, dest, flags);
    }
}

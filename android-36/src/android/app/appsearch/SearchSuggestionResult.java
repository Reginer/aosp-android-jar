/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.appsearch.flags.Flags;
import com.android.internal.util.Preconditions;

import java.util.Objects;

/** The result class of the {@link AppSearchSession#searchSuggestion}. */
@SafeParcelable.Class(creator = "SearchSuggestionResultCreator")
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
public final class SearchSuggestionResult extends AbstractSafeParcelable {

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final @NonNull Parcelable.Creator<SearchSuggestionResult> CREATOR =
            new SearchSuggestionResultCreator();

    @Field(id = 1, getter = "getSuggestedResult")
    private final String mSuggestedResult;

    private @Nullable Integer mHashCode;

    @Constructor
    SearchSuggestionResult(@Param(id = 1) String suggestedResult) {
        mSuggestedResult = Objects.requireNonNull(suggestedResult);
    }

    /**
     * Returns the suggested result that could be used as query expression in the {@link
     * AppSearchSession#search}.
     *
     * <p>The suggested result will never be empty.
     *
     * <p>The suggested result only contains lowercase or special characters.
     */
    public @NonNull String getSuggestedResult() {
        return mSuggestedResult;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SearchSuggestionResult)) {
            return false;
        }
        SearchSuggestionResult otherResult = (SearchSuggestionResult) other;
        return mSuggestedResult.equals(otherResult.mSuggestedResult);
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = mSuggestedResult.hashCode();
        }
        return mHashCode;
    }

    /** The Builder class of {@link SearchSuggestionResult}. */
    public static final class Builder {
        private String mSuggestedResult = "";

        /**
         * Sets the suggested result that could be used as query expression in the {@link
         * AppSearchSession#search}.
         *
         * <p>The suggested result should only contain lowercase or special characters.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setSuggestedResult(@NonNull String suggestedResult) {
            Objects.requireNonNull(suggestedResult);
            Preconditions.checkStringNotEmpty(suggestedResult);
            mSuggestedResult = suggestedResult;
            return this;
        }

        /** Build a {@link SearchSuggestionResult} object */
        public @NonNull SearchSuggestionResult build() {
            return new SearchSuggestionResult(mSuggestedResult);
        }
    }

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SearchSuggestionResultCreator.writeToParcel(this, dest, flags);
    }
}

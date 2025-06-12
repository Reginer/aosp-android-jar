/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.app.appsearch.aidl;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.AppSearchResult;
import android.app.appsearch.GetSchemaResponse;
import android.app.appsearch.InternalSetSchemaResponse;
import android.app.appsearch.ParcelableUtil;
import android.app.appsearch.SearchResultPage;
import android.app.appsearch.SearchSuggestionResult;
import android.app.appsearch.SetSchemaResponse.MigrationFailure;
import android.app.appsearch.StorageInfo;
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.GenericDocumentParcel;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Objects;

/**
 * Parcelable wrapper around {@link AppSearchResult}.
 *
 * <p>{@link AppSearchResult} can contain any value, including non-parcelable values. For the
 * specific case of sending {@link AppSearchResult} across Binder, this class wraps an {@link
 * AppSearchResult} that contains a parcelable type and provides parcelability of the whole
 * structure.
 *
 * @deprecated This class is deprecated, you should use {@link AppSearchResultParcelV2}.
 * @param <ValueType> The type of result object for successful calls. Must be a parcelable type.
 * @hide
 */
@Deprecated
@SafeParcelable.Class(creator = "AppSearchResultParcelCreator", creatorIsFinal = false)
public final class AppSearchResultParcel<ValueType> extends AbstractSafeParcelable {

    @NonNull
    @SuppressWarnings("rawtypes")
    public static final Parcelable.Creator<AppSearchResultParcel> CREATOR =
            new AppSearchResultParcelCreator() {
                @Override
                public AppSearchResultParcel createFromParcel(Parcel in) {
                    // We pass the result we get from ParcelableUtil#readBlob to
                    // AppSearchResultParcelCreator to decode.
                    byte[] dataBlob = Objects.requireNonNull(ParcelableUtil.readBlob(in));
                    // Create a parcel object to un-serialize the byte array we are reading from
                    // Parcel.readBlob(). Parcel.WriteBlob() could take care of whether to pass
                    // data via binder directly or Android shared memory if the data is large.
                    Parcel unmarshallParcel = Parcel.obtain();
                    try {
                        unmarshallParcel.unmarshall(dataBlob, 0, dataBlob.length);
                        unmarshallParcel.setDataPosition(0);
                        return super.createFromParcel(unmarshallParcel);
                    } finally {
                        unmarshallParcel.recycle();
                    }
                }
            };

    @NonNull
    private static final Parcelable.Creator<AppSearchResultParcel> CREATOR_WITHOUT_BLOB =
            new AppSearchResultParcelCreator();

    @Field(id = 1)
    @AppSearchResult.ResultCode
    int mResultCode;

    @Field(id = 2)
    @Nullable
    String mErrorMessage;

    @Field(id = 3)
    @Nullable
    InternalSetSchemaResponse mInternalSetSchemaResponse;

    @Field(id = 4)
    @Nullable
    GetSchemaResponse mGetSchemaResponse;

    @Field(id = 5)
    @Nullable
    List<String> mStrings;

    @Field(id = 6)
    @Nullable
    GenericDocumentParcel mGenericDocumentParcel;

    @Field(id = 7)
    @Nullable
    SearchResultPage mSearchResultPage;

    @Field(id = 8)
    @Nullable
    List<MigrationFailure> mMigrationFailures;

    @Field(id = 9)
    @Nullable
    List<SearchSuggestionResult> mSearchSuggestionResults;

    @Field(id = 10)
    @Nullable
    StorageInfo mStorageInfo;

    @NonNull AppSearchResult<ValueType> mResultCached;

    /**
     * Creates an AppSearchResultParcel for given value type.
     *
     * @param resultCode A {@link AppSearchResult} result code for {@link IAppSearchManager} API
     *     response.
     * @param errorMessage An error message in case of a failed response.
     * @param internalSetSchemaResponse An {@link InternalSetSchemaResponse} type response.
     * @param getSchemaResponse An {@link GetSchemaResponse} type response.
     * @param strings An {@link List<String>} type response.
     * @param genericDocumentParcel An {@link GenericDocumentParcel} type response.
     * @param searchResultPage An {@link SearchResultPage} type response.
     * @param migrationFailures An {@link List<MigrationFailure>} type response.
     * @param searchSuggestionResults An {@link List<SearchSuggestionResult>} type response.
     * @param storageInfo {@link StorageInfo} type response.
     */
    @Constructor
    AppSearchResultParcel(
            @Param(id = 1) @AppSearchResult.ResultCode int resultCode,
            @Param(id = 2) @Nullable String errorMessage,
            @Param(id = 3) @Nullable InternalSetSchemaResponse internalSetSchemaResponse,
            @Param(id = 4) @Nullable GetSchemaResponse getSchemaResponse,
            @Param(id = 5) @Nullable List<String> strings,
            @Param(id = 6) @Nullable GenericDocumentParcel genericDocumentParcel,
            @Param(id = 7) @Nullable SearchResultPage searchResultPage,
            @Param(id = 8) @Nullable List<MigrationFailure> migrationFailures,
            @Param(id = 9) @Nullable List<SearchSuggestionResult> searchSuggestionResults,
            @Param(id = 10) @Nullable StorageInfo storageInfo) {
        mResultCode = resultCode;
        mErrorMessage = errorMessage;
        if (resultCode == AppSearchResult.RESULT_OK) {
            mInternalSetSchemaResponse = internalSetSchemaResponse;
            mGetSchemaResponse = getSchemaResponse;
            mStrings = strings;
            mGenericDocumentParcel = genericDocumentParcel;
            mSearchResultPage = searchResultPage;
            mMigrationFailures = migrationFailures;
            mSearchSuggestionResults = searchSuggestionResults;
            mStorageInfo = storageInfo;
            if (mInternalSetSchemaResponse != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mInternalSetSchemaResponse);
            } else if (mGetSchemaResponse != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mGetSchemaResponse);
            } else if (mStrings != null) {
                mResultCached =
                        (AppSearchResult<ValueType>) AppSearchResult.newSuccessfulResult(mStrings);
            } else if (mGenericDocumentParcel != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mGenericDocumentParcel);
            } else if (mSearchResultPage != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mSearchResultPage);
            } else if (mMigrationFailures != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mMigrationFailures);
            } else if (mSearchSuggestionResults != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mSearchSuggestionResults);
            } else if (mStorageInfo != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mStorageInfo);
            } else {
                // Default case where code is OK and value is null.
                mResultCached = AppSearchResult.newSuccessfulResult(null);
            }
        } else {
            mResultCached = AppSearchResult.newFailedResult(mResultCode, mErrorMessage);
        }
    }

    /**
     * Creates a new {@link AppSearchResultParcel} from the given result in case a successful Void
     * response.
     */
    public static AppSearchResultParcel fromVoid() {
        return new AppSearchResultParcel.Builder<>(AppSearchResult.RESULT_OK).build();
    }

    /** Creates a new failed {@link AppSearchResultParcel} from result code and error message. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static AppSearchResultParcel fromFailedResult(AppSearchResult failedResult) {
        if (failedResult.isSuccess()) {
            throw new IllegalStateException(
                    "Creating a failed AppSearchResultParcel from a " + "successful response");
        }

        return new AppSearchResultParcel.Builder<>(failedResult.getResultCode())
                .setErrorMessage(failedResult.getErrorMessage())
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcel} from the given result in case a successful {@link
     * InternalSetSchemaResponse}.
     */
    public static AppSearchResultParcel<InternalSetSchemaResponse> fromInternalSetSchemaResponse(
            InternalSetSchemaResponse internalSetSchemaResponse) {
        return new AppSearchResultParcel.Builder<InternalSetSchemaResponse>(
                        AppSearchResult.RESULT_OK)
                .setInternalSetSchemaResponse(internalSetSchemaResponse)
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcel} from the given result in case a successful {@link
     * GetSchemaResponse}.
     */
    public static AppSearchResultParcel<GetSchemaResponse> fromGetSchemaResponse(
            GetSchemaResponse getSchemaResponse) {
        return new AppSearchResultParcel.Builder<GetSchemaResponse>(AppSearchResult.RESULT_OK)
                .setGetSchemaResponse(getSchemaResponse)
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcel} from the given result in case a successful {@link
     * List}&lt;{@link String}&gt;.
     */
    public static AppSearchResultParcel<List<String>> fromStringList(List<String> stringList) {
        return new AppSearchResultParcel.Builder<List<String>>(AppSearchResult.RESULT_OK)
                .setStrings(stringList)
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcel} from the given result in case a successful {@link
     * GenericDocumentParcel}.
     */
    public static AppSearchResultParcel<GenericDocumentParcel> fromGenericDocumentParcel(
            GenericDocumentParcel genericDocumentParcel) {
        return new AppSearchResultParcel.Builder<GenericDocumentParcel>(AppSearchResult.RESULT_OK)
                .setGenericDocumentParcel(genericDocumentParcel)
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcel} from the given result in case a successful {@link
     * SearchResultPage}.
     */
    public static AppSearchResultParcel<SearchResultPage> fromSearchResultPage(
            SearchResultPage searchResultPage) {
        return new AppSearchResultParcel.Builder<SearchResultPage>(AppSearchResult.RESULT_OK)
                .setSearchResultPage(searchResultPage)
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcel} from the given result in case a successful {@link
     * List}&lt;{@link MigrationFailure}&gt;.
     */
    public static AppSearchResultParcel<List<MigrationFailure>> fromMigrationFailuresList(
            List<MigrationFailure> migrationFailureList) {
        return new AppSearchResultParcel.Builder<List<MigrationFailure>>(AppSearchResult.RESULT_OK)
                .setMigrationFailures(migrationFailureList)
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcel} from the given result in case a successful {@link
     * List}&lt;{@link SearchSuggestionResult}&gt;.
     */
    public static AppSearchResultParcel<List<SearchSuggestionResult>>
            fromSearchSuggestionResultList(
                    List<SearchSuggestionResult> searchSuggestionResultList) {
        return new AppSearchResultParcel.Builder<List<SearchSuggestionResult>>(
                        AppSearchResult.RESULT_OK)
                .setSearchSuggestionResults(searchSuggestionResultList)
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcel} from the given result in case a successful {@link
     * StorageInfo}.
     */
    public static AppSearchResultParcel<StorageInfo> fromStorageInfo(StorageInfo storageInfo) {
        return new AppSearchResultParcel.Builder<StorageInfo>(AppSearchResult.RESULT_OK)
                .setStorageInfo(storageInfo)
                .build();
    }


    @NonNull
    public AppSearchResult<ValueType> getResult() {
        return mResultCached;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // Serializes the whole object, So that we can use Parcel.writeBlob() to send data.
        // WriteBlob() could take care of whether to pass data via binder directly or Android shared
        // memory if the data is large.
        byte[] bytes;
        Parcel data = Parcel.obtain();
        try {
            // We pass encoded result from AppSearchResultParcelCreator to ParcelableUtil#writeBlob.
            directlyWriteToParcel(this, data, flags);
            bytes = data.marshall();
        } finally {
            data.recycle();
        }
        ParcelableUtil.writeBlob(dest, bytes);
    }

    static void directlyWriteToParcel(
            @NonNull AppSearchResultParcel<?> result, @NonNull Parcel data, int flags) {
        AppSearchResultParcelCreator.writeToParcel(result, data, flags);
    }

    static AppSearchResultParcel<?> directlyReadFromParcel(@NonNull Parcel data) {
        return CREATOR_WITHOUT_BLOB.createFromParcel(data);
    }

    /**
     * Builder for {@link AppSearchResultParcel} objects.
     *
     * @param <ValueType> The type of the result objects for successful results.
     */
    static final class Builder<ValueType> {

        @AppSearchResult.ResultCode private final int mResultCode;
        @Nullable private String mErrorMessage;
        @Nullable private InternalSetSchemaResponse mInternalSetSchemaResponse;
        @Nullable private GetSchemaResponse mGetSchemaResponse;
        @Nullable private List<String> mStrings;
        @Nullable private GenericDocumentParcel mGenericDocumentParcel;
        @Nullable private SearchResultPage mSearchResultPage;
        @Nullable private List<MigrationFailure> mMigrationFailures;
        @Nullable private List<SearchSuggestionResult> mSearchSuggestionResults;
        @Nullable private StorageInfo mStorageInfo;

        /** Builds an {@link AppSearchResultParcel.Builder}. */
        Builder(int resultCode) {
            mResultCode = resultCode;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setInternalSetSchemaResponse(
                InternalSetSchemaResponse internalSetSchemaResponse) {
            mInternalSetSchemaResponse = internalSetSchemaResponse;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setGetSchemaResponse(GetSchemaResponse getSchemaResponse) {
            mGetSchemaResponse = getSchemaResponse;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setStrings(List<String> strings) {
            mStrings = strings;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setGenericDocumentParcel(GenericDocumentParcel genericDocumentParcel) {
            mGenericDocumentParcel = genericDocumentParcel;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setSearchResultPage(SearchResultPage searchResultPage) {
            mSearchResultPage = searchResultPage;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setMigrationFailures(List<MigrationFailure> migrationFailures) {
            mMigrationFailures = migrationFailures;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setSearchSuggestionResults(
                List<SearchSuggestionResult> searchSuggestionResults) {
            mSearchSuggestionResults = searchSuggestionResults;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setStorageInfo(StorageInfo storageInfo) {
            mStorageInfo = storageInfo;
            return this;
        }


        /**
         * Builds an {@link AppSearchResultParcel} object from the contents of this {@link
         * AppSearchResultParcel.Builder}.
         */
        @NonNull
        AppSearchResultParcel<ValueType> build() {
            return new AppSearchResultParcel<>(
                    mResultCode,
                    mErrorMessage,
                    mInternalSetSchemaResponse,
                    mGetSchemaResponse,
                    mStrings,
                    mGenericDocumentParcel,
                    mSearchResultPage,
                    mMigrationFailures,
                    mSearchSuggestionResults,
                    mStorageInfo);
        }
    }
}

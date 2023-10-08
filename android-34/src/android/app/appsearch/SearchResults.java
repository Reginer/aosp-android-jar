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

import static android.app.appsearch.AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE;
import static android.app.appsearch.AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID;
import static android.app.appsearch.SearchSessionUtil.safeExecute;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.aidl.AppSearchResultParcel;
import android.app.appsearch.aidl.IAppSearchManager;
import android.app.appsearch.aidl.IAppSearchResultCallback;
import android.content.AttributionSource;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.util.Preconditions;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Encapsulates results of a search operation.
 *
 * <p>Each {@link AppSearchSession#search} operation returns a list of {@link SearchResult} objects,
 * referred to as a "page", limited by the size configured by {@link
 * SearchSpec.Builder#setResultCountPerPage}.
 *
 * <p>To fetch a page of results, call {@link #getNextPage}.
 *
 * <p>All instances of {@link SearchResults} must call {@link SearchResults#close()} after the
 * results are fetched.
 *
 * <p>This class is not thread safe.
 */
public class SearchResults implements Closeable {
    private static final String TAG = "SearchResults";

    private final IAppSearchManager mService;

    // The permission identity of the caller
    private final AttributionSource mAttributionSource;

    // The database name to search over. If null, this will search over all database names.
    @Nullable
    private final String mDatabaseName;

    private final String mQueryExpression;

    private final SearchSpec mSearchSpec;

    private final UserHandle mUserHandle;

    private long mNextPageToken;

    private boolean mIsFirstLoad = true;

    private boolean mIsClosed = false;

    SearchResults(
            @NonNull IAppSearchManager service,
            @NonNull AttributionSource attributionSource,
            @Nullable String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec,
            @NonNull UserHandle userHandle) {
        mService = Objects.requireNonNull(service);
        mAttributionSource = Objects.requireNonNull(attributionSource);
        mDatabaseName = databaseName;
        mQueryExpression = Objects.requireNonNull(queryExpression);
        mSearchSpec = Objects.requireNonNull(searchSpec);
        mUserHandle = Objects.requireNonNull(userHandle);
    }

    /**
     * Retrieves the next page of {@link SearchResult} objects.
     *
     * <p>The page size is configured by {@link SearchSpec.Builder#setResultCountPerPage}.
     *
     * <p>Continue calling this method to access results until it returns an empty list, signifying
     * there are no more results.
     *
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive the pending result of performing this operation.
     */
    public void getNextPage(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<List<SearchResult>>> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        Preconditions.checkState(!mIsClosed, "SearchResults has already been closed");
        try {
            long binderCallStartTimeMillis = SystemClock.elapsedRealtime();
            if (mIsFirstLoad) {
                mIsFirstLoad = false;
                if (mDatabaseName == null) {
                    // Global query, there's no one package-database combination to check.
                    mService.globalQuery(mAttributionSource, mQueryExpression,
                            mSearchSpec.getBundle(), mUserHandle, binderCallStartTimeMillis,
                            wrapCallback(executor, callback));
                } else {
                    // Normal local query, pass in specified database.
                    mService.query(mAttributionSource, mDatabaseName, mQueryExpression,
                            mSearchSpec.getBundle(), mUserHandle,
                            binderCallStartTimeMillis,
                            wrapCallback(executor, callback));
                }
            } else {
                // TODO(b/276349029): Log different join types when they get added.
                @AppSearchSchema.StringPropertyConfig.JoinableValueType
                int joinType = JOINABLE_VALUE_TYPE_NONE;
                if (mSearchSpec.getJoinSpec() != null
                        && !mSearchSpec.getJoinSpec().getChildPropertyExpression().isEmpty()) {
                    joinType = JOINABLE_VALUE_TYPE_QUALIFIED_ID;
                }
                mService.getNextPage(mAttributionSource, mDatabaseName, mNextPageToken, joinType,
                        mUserHandle, binderCallStartTimeMillis, wrapCallback(executor, callback));
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void close() {
        if (!mIsClosed) {
            try {
                mService.invalidateNextPageToken(mAttributionSource, mNextPageToken,
                        mUserHandle, /*binderCallStartTimeMillis=*/ SystemClock.elapsedRealtime());
                mIsClosed = true;
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to close the SearchResults", e);
            }
        }
    }

    private IAppSearchResultCallback wrapCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<List<SearchResult>>> callback) {
        return new IAppSearchResultCallback.Stub() {
            @Override
            public void onResult(AppSearchResultParcel resultParcel) {
                safeExecute(
                        executor,
                        callback,
                        () -> invokeCallback(resultParcel.getResult(), callback));
            }
        };
    }

    private void invokeCallback(
            @NonNull AppSearchResult<Bundle> searchResultPageResult,
            @NonNull Consumer<AppSearchResult<List<SearchResult>>> callback) {
        if (searchResultPageResult.isSuccess()) {
            try {
                SearchResultPage searchResultPage = new SearchResultPage
                        (Objects.requireNonNull(searchResultPageResult.getResultValue()));
                mNextPageToken = searchResultPage.getNextPageToken();
                callback.accept(AppSearchResult.newSuccessfulResult(
                        searchResultPage.getResults()));
            } catch (Throwable t) {
                callback.accept(AppSearchResult.throwableToFailedResult(t));
            }
        } else {
            callback.accept(AppSearchResult.newFailedResult(searchResultPageResult));
        }
    }
}

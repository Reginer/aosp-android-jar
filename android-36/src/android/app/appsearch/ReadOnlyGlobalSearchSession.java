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

package android.app.appsearch;

import static android.app.appsearch.SearchSessionUtil.safeExecute;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.app.appsearch.aidl.AppSearchAttributionSource;
import android.app.appsearch.aidl.AppSearchResultCallback;
import android.app.appsearch.aidl.GetDocumentsAidlRequest;
import android.app.appsearch.aidl.GetSchemaAidlRequest;
import android.app.appsearch.aidl.IAppSearchManager;
import android.app.appsearch.aidl.InitializeAidlRequest;
import android.app.appsearch.aidl.OpenBlobForReadAidlRequest;
import android.app.appsearch.util.ExceptionUtil;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;

import com.android.appsearch.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Holds the shared implementation for the query methods of {@link GlobalSearchSession} and
 * EnterpriseGlobalSearchSession. Enterprise calls direct queries to the user's work profile
 * AppSearch instance.
 *
 * @hide
 */
public abstract class ReadOnlyGlobalSearchSession {
    protected final IAppSearchManager mService;
    protected final UserHandle mUserHandle;
    protected final AppSearchAttributionSource mCallerAttributionSource;
    private final boolean mIsForEnterprise;

    /**
     * Creates a read-only search session with the given {@link IAppSearchManager} service, user,
     * and attribution source.
     *
     * @param service The {@link IAppSearchManager} service from which to make api calls
     * @param userHandle The user for which the session should be created
     * @param callerAttributionSource The attribution source containing the caller's package name
     *     and uid
     * @param isForEnterprise Whether the session should query the user's enterprise profile
     */
    ReadOnlyGlobalSearchSession(
            @NonNull IAppSearchManager service,
            @NonNull UserHandle userHandle,
            @NonNull AppSearchAttributionSource callerAttributionSource,
            boolean isForEnterprise) {
        mService = service;
        mUserHandle = userHandle;
        mCallerAttributionSource = callerAttributionSource;
        mIsForEnterprise = isForEnterprise;
    }

    // Once the callback.accept has been called here, the class is ready to use.
    protected void initialize(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<Void>> callback) {
        try {
            mService.initialize(
                    new InitializeAidlRequest(
                            mCallerAttributionSource,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new AppSearchResultCallback<Void>() {
                        @Override
                        public void onResult(@NonNull AppSearchResult<Void> result) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> {
                                        if (result.isSuccess()) {
                                            callback.accept(
                                                    AppSearchResult.newSuccessfulResult(null));
                                        } else {
                                            callback.accept(
                                                    AppSearchResult.newFailedResult(result));
                                        }
                                    });
                        }
                    });
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        }
    }

    /**
     * Retrieves {@link GenericDocument} documents, belonging to the specified package name and
     * database name and identified by the namespace and ids in the request, from the {@link
     * GlobalSearchSession} database.
     *
     * <p>If the package or database doesn't exist or if the calling package doesn't have access,
     * the gets will be handled as failures in an {@link AppSearchBatchResult} object in the
     * callback.
     *
     * @param packageName the name of the package to get from
     * @param databaseName the name of the database to get from
     * @param request a request containing a namespace and IDs to get documents for.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive the pending result of performing this operation. The keys
     *     of the returned {@link AppSearchBatchResult} are the input IDs. The values are the
     *     returned {@link GenericDocument}s on success, or a failed {@link AppSearchResult}
     *     otherwise. IDs that are not found will return a failed {@link AppSearchResult} with a
     *     result code of {@link AppSearchResult#RESULT_NOT_FOUND}. If an unexpected internal error
     *     occurs in the AppSearch service, {@link BatchResultCallback#onSystemError} will be
     *     invoked with a {@link Throwable}.
     */
    public void getByDocumentId(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull GetByDocumentIdRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull BatchResultCallback<String, GenericDocument> callback) {
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(databaseName);
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.getDocuments(
                    new GetDocumentsAidlRequest(
                            mCallerAttributionSource,
                            /* targetPackageName= */ packageName,
                            databaseName,
                            request,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime(),
                            mIsForEnterprise),
                    SearchSessionUtil.createGetDocumentCallback(executor, callback));
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        }
    }

    /**
     * Opens a batch of AppSearch Blobs for reading.
     *
     * <p>See {@link AppSearchSession#openBlobForRead} for a general description when a blob is for
     * read.
     *
     * <p class="caution">The returned {@link OpenBlobForReadResponse} must be closed after use to
     * avoid resource leaks. Failing to close it will result in system file descriptor exhaustion.
     *
     * @param handles The {@link AppSearchBlobHandle}s that identifies the blobs.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive the {@link OpenBlobForReadResponse}.
     * @see GenericDocument.Builder#setPropertyBlobHandle
     */
    @FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
    public void openBlobForRead(
            @NonNull Set<AppSearchBlobHandle> handles,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<OpenBlobForReadResponse>> callback) {
        try {
            mService.openBlobForRead(
                    new OpenBlobForReadAidlRequest(
                            mCallerAttributionSource,
                            /* callingDatabaseName= */ null,
                            new ArrayList<>(handles),
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new AppSearchResultCallback<OpenBlobForReadResponse>() {
                        @Override
                        public void onResult(
                                @NonNull AppSearchResult<OpenBlobForReadResponse> result) {
                            safeExecute(executor, callback, () -> callback.accept(result));
                        }
                    });
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        }
    }

    /**
     * Retrieves documents from all AppSearch databases that the querying application has access to.
     *
     * <p>Applications can be granted access to documents by specifying {@link
     * SetSchemaRequest.Builder#setSchemaTypeVisibilityForPackage} when building a schema.
     *
     * <p>Document access can also be granted to system UIs by specifying {@link
     * SetSchemaRequest.Builder#setSchemaTypeDisplayedBySystem} when building a schema.
     *
     * <p>See {@link AppSearchSession#search} for a detailed explanation on forming a query string.
     *
     * <p>This method is lightweight. The heavy work will be done in {@link
     * SearchResults#getNextPage}.
     *
     * @param queryExpression query string to search.
     * @param searchSpec spec for setting document filters, adding projection, setting term match
     *     type, etc.
     * @return a {@link SearchResults} object for retrieved matched documents.
     */
    @NonNull
    public SearchResults search(@NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Objects.requireNonNull(queryExpression);
        Objects.requireNonNull(searchSpec);
        return new SearchResults(
                mService,
                mCallerAttributionSource,
                /* databaseName= */ null,
                queryExpression,
                searchSpec,
                mUserHandle,
                mIsForEnterprise);
    }

    /**
     * Retrieves the collection of schemas most recently successfully provided to {@link
     * AppSearchSession#setSchema} for any types belonging to the requested package and database
     * that the caller has been granted access to.
     *
     * <p>If the requested package/database combination does not exist or the caller has not been
     * granted access to it, then an empty GetSchemaResponse will be returned.
     *
     * @param packageName the package that owns the requested {@link AppSearchSchema} instances.
     * @param databaseName the database that owns the requested {@link AppSearchSchema} instances.
     * @param executor Executor on which to invoke the callback.
     * @param callback The pending {@link GetSchemaResponse} containing the schemas that the caller
     *     has access to or an empty GetSchemaResponse if the request package and database does not
     *     exist, has not set a schema or contains no schemas that are accessible to the caller.
     */
    public void getSchema(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<GetSchemaResponse>> callback) {
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(databaseName);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.getSchema(
                    new GetSchemaAidlRequest(
                            mCallerAttributionSource,
                            packageName,
                            databaseName,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime(),
                            mIsForEnterprise),
                    new AppSearchResultCallback<GetSchemaResponse>() {
                        @Override
                        public void onResult(@NonNull AppSearchResult<GetSchemaResponse> result) {
                            safeExecute(executor, callback, () -> callback.accept(result));
                        }
                    });
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        }
    }

    /**
     * Returns the service instance to make IPC calls.
     *
     * @hide
     */
    @VisibleForTesting
    public IAppSearchManager getService() {
        return mService;
    }

    /**
     * Returns if session supports Enterprise flow.
     *
     * @hide
     */
    @VisibleForTesting
    public boolean isForEnterprise() {
        return mIsForEnterprise;
    }
}

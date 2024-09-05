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

import static android.app.appsearch.AppSearchResult.RESULT_INTERNAL_ERROR;
import static android.app.appsearch.SearchSessionUtil.safeExecute;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.aidl.AppSearchAttributionSource;
import android.app.appsearch.aidl.AppSearchBatchResultParcel;
import android.app.appsearch.aidl.AppSearchResultParcel;
import android.app.appsearch.aidl.DocumentsParcel;
import android.app.appsearch.aidl.GetDocumentsAidlRequest;
import android.app.appsearch.aidl.GetNamespacesAidlRequest;
import android.app.appsearch.aidl.GetSchemaAidlRequest;
import android.app.appsearch.aidl.GetStorageInfoAidlRequest;
import android.app.appsearch.aidl.IAppSearchBatchResultCallback;
import android.app.appsearch.aidl.IAppSearchManager;
import android.app.appsearch.aidl.IAppSearchResultCallback;
import android.app.appsearch.aidl.InitializeAidlRequest;
import android.app.appsearch.aidl.PersistToDiskAidlRequest;
import android.app.appsearch.aidl.PutDocumentsAidlRequest;
import android.app.appsearch.aidl.RemoveByDocumentIdAidlRequest;
import android.app.appsearch.aidl.RemoveByQueryAidlRequest;
import android.app.appsearch.aidl.ReportUsageAidlRequest;
import android.app.appsearch.aidl.SearchSuggestionAidlRequest;
import android.app.appsearch.aidl.SetSchemaAidlRequest;
import android.app.appsearch.exceptions.AppSearchException;
import android.app.appsearch.safeparcel.GenericDocumentParcel;
import android.app.appsearch.stats.SchemaMigrationStats;
import android.app.appsearch.util.ExceptionUtil;
import android.app.appsearch.util.SchemaMigrationUtil;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.util.Preconditions;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Provides a connection to a single AppSearch database.
 *
 * <p>An {@link AppSearchSession} instance provides access to database operations such as setting a
 * schema, adding documents, and searching.
 *
 * <p>This class is thread safe.
 *
 * @see GlobalSearchSession
 */
public final class AppSearchSession implements Closeable {
    private static final String TAG = "AppSearchSession";

    private final AppSearchAttributionSource mCallerAttributionSource;
    private final String mDatabaseName;
    private final UserHandle mUserHandle;
    private final IAppSearchManager mService;
    @Nullable private final File mCacheDirectory;

    private boolean mIsMutated = false;
    private boolean mIsClosed = false;

    /**
     * Creates a search session for the client, defined by the {@code userHandle} and {@code
     * packageName}.
     *
     * @param searchContext The {@link AppSearchManager.SearchContext} contains all information to
     *     create a new {@link AppSearchSession}.
     * @param service The {@link IAppSearchManager} service from which to make api calls.
     * @param userHandle The user for which the session should be created.
     * @param callerAttributionSource The attribution source containing the caller's package name
     *     and uid.
     * @param cacheDirectory The directory to create temporary files needed for migration. If this
     *     is null, the default temporary-file directory (/data/local/tmp) will be used.
     * @param executor Executor on which to invoke the callback.
     * @param callback The {@link AppSearchResult}&lt;{@link AppSearchSession}&gt; of performing
     *     this operation. Or a {@link AppSearchResult} with failure reason code and error
     *     information.
     */
    static void createSearchSession(
            @NonNull AppSearchManager.SearchContext searchContext,
            @NonNull IAppSearchManager service,
            @NonNull UserHandle userHandle,
            @NonNull AppSearchAttributionSource callerAttributionSource,
            @Nullable File cacheDirectory,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<AppSearchSession>> callback) {
        AppSearchSession searchSession =
                new AppSearchSession(
                        service,
                        userHandle,
                        callerAttributionSource,
                        searchContext.mDatabaseName,
                        cacheDirectory);
        searchSession.initialize(executor, callback);
    }

    // NOTE: No instance of this class should be created or returned except via initialize().
    // Once the callback.accept has been called here, the class is ready to use.
    private void initialize(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<AppSearchSession>> callback) {
        try {
            mService.initialize(
                    new InitializeAidlRequest(
                            mCallerAttributionSource,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public void onResult(AppSearchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> {
                                        AppSearchResult<Void> result = resultParcel.getResult();
                                        if (result.isSuccess()) {
                                            callback.accept(
                                                    AppSearchResult.newSuccessfulResult(
                                                            AppSearchSession.this));
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

    private AppSearchSession(
            @NonNull IAppSearchManager service,
            @NonNull UserHandle userHandle,
            @NonNull AppSearchAttributionSource callerAttributionSource,
            @NonNull String databaseName,
            @Nullable File cacheDirectory) {
        mService = service;
        mUserHandle = userHandle;
        mCallerAttributionSource = callerAttributionSource;
        mDatabaseName = databaseName;
        mCacheDirectory = cacheDirectory;
    }

    /**
     * Sets the schema that represents the organizational structure of data within the AppSearch
     * database.
     *
     * <p>Upon creating an {@link AppSearchSession}, {@link #setSchema} should be called. If the
     * schema needs to be updated, or it has not been previously set, then the provided schema will
     * be saved and persisted to disk. Otherwise, {@link #setSchema} is handled efficiently as a
     * no-op call.
     *
     * @param request the schema to set or update the AppSearch database to.
     * @param workExecutor Executor on which to schedule heavy client-side background work such as
     *     transforming documents.
     * @param callbackExecutor Executor on which to invoke the callback.
     * @param callback Callback to receive errors resulting from setting the schema. If the
     *     operation succeeds, the callback will be invoked with {@code null}.
     */
    public void setSchema(
            @NonNull SetSchemaRequest request,
            @NonNull Executor workExecutor,
            @NonNull @CallbackExecutor Executor callbackExecutor,
            @NonNull Consumer<AppSearchResult<SetSchemaResponse>> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(workExecutor);
        Objects.requireNonNull(callbackExecutor);
        Objects.requireNonNull(callback);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        List<AppSearchSchema> schemaList = new ArrayList<>(request.getSchemas());
        for (int i = 0; i < schemaList.size(); i++) {
            if (!schemaList.get(i).getParentTypes().isEmpty()
                    && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                throw new UnsupportedOperationException(
                        "SCHEMA_ADD_PARENT_TYPE is not available on this AppSearch "
                                + "implementation.");
            }
        }

        // Extract a List<VisibilityConfig> from the request
        List<InternalVisibilityConfig> visibilityConfigs =
                InternalVisibilityConfig.toInternalVisibilityConfigs(request);
        // No need to trigger migration if user never set migrator
        if (request.getMigrators().isEmpty()) {
            setSchemaNoMigrations(
                    request, schemaList, visibilityConfigs, callbackExecutor, callback);
        } else {
            setSchemaWithMigrations(
                    request,
                    schemaList,
                    visibilityConfigs,
                    workExecutor,
                    callbackExecutor,
                    callback);
        }
        mIsMutated = true;
    }

    /**
     * Retrieves the schema most recently successfully provided to {@link #setSchema}.
     *
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive the pending results of schema.
     */
    public void getSchema(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<GetSchemaResponse>> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        String targetPackageName = mCallerAttributionSource.getPackageName();
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        try {
            mService.getSchema(
                    new GetSchemaAidlRequest(
                            mCallerAttributionSource,
                            targetPackageName,
                            mDatabaseName,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime(),
                            /* isForEnterprise= */ false),
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public void onResult(AppSearchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> {
                                        AppSearchResult<GetSchemaResponse> result =
                                                resultParcel.getResult();
                                        if (result.isSuccess()) {
                                            GetSchemaResponse response =
                                                    Objects.requireNonNull(result.getResultValue());
                                            callback.accept(
                                                    AppSearchResult.newSuccessfulResult(response));
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
     * Retrieves the set of all namespaces in the current database with at least one document.
     *
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive the namespaces.
     */
    public void getNamespaces(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<Set<String>>> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        try {
            mService.getNamespaces(
                    new GetNamespacesAidlRequest(
                            mCallerAttributionSource,
                            mDatabaseName,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public void onResult(AppSearchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> {
                                        AppSearchResult<List<String>> result =
                                                resultParcel.getResult();
                                        if (result.isSuccess()) {
                                            Set<String> namespaces =
                                                    new ArraySet<>(result.getResultValue());
                                            callback.accept(
                                                    AppSearchResult.newSuccessfulResult(
                                                            namespaces));
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
     * Indexes documents into the {@link AppSearchSession} database.
     *
     * <p>Each {@link GenericDocument} object must have a {@code schemaType} field set to an {@link
     * AppSearchSchema} type that has been previously registered by calling the {@link #setSchema}
     * method.
     *
     * @param request containing documents to be indexed.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive pending result of performing this operation. The keys of
     *     the returned {@link AppSearchBatchResult} are the IDs of the input documents. The values
     *     are {@code null} if they were successfully indexed, or a failed {@link AppSearchResult}
     *     otherwise. If an unexpected internal error occurs in the AppSearch service, {@link
     *     BatchResultCallback#onSystemError} will be invoked with a {@link Throwable}.
     */
    public void put(
            @NonNull PutDocumentsRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull BatchResultCallback<String, Void> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        DocumentsParcel documentsParcel =
                new DocumentsParcel(
                        toGenericDocumentParcels(request.getGenericDocuments()),
                        toGenericDocumentParcels(request.getTakenActionGenericDocuments()));
        try {
            mService.putDocuments(
                    new PutDocumentsAidlRequest(
                            mCallerAttributionSource,
                            mDatabaseName,
                            documentsParcel,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new IAppSearchBatchResultCallback.Stub() {
                        @Override
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public void onResult(AppSearchBatchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> callback.onResult(resultParcel.getResult()));
                        }

                        @Override
                        public void onSystemError(AppSearchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () ->
                                            SearchSessionUtil.sendSystemErrorToCallback(
                                                    resultParcel.getResult(), callback));
                        }
                    });
            mIsMutated = true;
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        }
    }

    /**
     * Gets {@link GenericDocument} objects by document IDs in a namespace from the {@link
     * AppSearchSession} database.
     *
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
            @NonNull GetByDocumentIdRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull BatchResultCallback<String, GenericDocument> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        String targetPackageName = mCallerAttributionSource.getPackageName();
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        try {
            mService.getDocuments(
                    new GetDocumentsAidlRequest(
                            mCallerAttributionSource,
                            targetPackageName,
                            mDatabaseName,
                            request,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime(),
                            /* isForEnterprise= */ false),
                    SearchSessionUtil.createGetDocumentCallback(executor, callback));
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        }
    }

    /**
     * Retrieves documents from the open {@link AppSearchSession} that match a given query string
     * and type of search provided.
     *
     * <p>Query strings can be empty, contain one term with no operators, or contain multiple terms
     * and operators.
     *
     * <p>For query strings that are empty, all documents that match the {@link SearchSpec} will be
     * returned.
     *
     * <p>For query strings with a single term and no operators, documents that match the provided
     * query string and {@link SearchSpec} will be returned.
     *
     * <p>The following operators are supported:
     *
     * <ul>
     *   <li>AND (implicit)
     *       <p>AND is an operator that matches documents that contain <i>all</i> provided terms.
     *       <p><b>NOTE:</b> A space between terms is treated as an "AND" operator. Explicitly
     *       including "AND" in a query string will treat "AND" as a term, returning documents that
     *       also contain "AND".
     *       <p>Example: "apple AND banana" matches documents that contain the terms "apple", "and",
     *       "banana".
     *       <p>Example: "apple banana" matches documents that contain both "apple" and "banana".
     *       <p>Example: "apple banana cherry" matches documents that contain "apple", "banana", and
     *       "cherry".
     *   <li>OR
     *       <p>OR is an operator that matches documents that contain <i>any</i> provided term.
     *       <p>Example: "apple OR banana" matches documents that contain either "apple" or
     *       "banana".
     *       <p>Example: "apple OR banana OR cherry" matches documents that contain any of "apple",
     *       "banana", or "cherry".
     *   <li>Exclusion (-)
     *       <p>Exclusion (-) is an operator that matches documents that <i>do not</i> contain the
     *       provided term.
     *       <p>Example: "-apple" matches documents that do not contain "apple".
     *   <li>Grouped Terms
     *       <p>For queries that require multiple operators and terms, terms can be grouped into
     *       subqueries. Subqueries are contained within an open "(" and close ")" parenthesis.
     *       <p>Example: "(donut OR bagel) (coffee OR tea)" matches documents that contain either
     *       "donut" or "bagel" and either "coffee" or "tea".
     *   <li>Property Restricts
     *       <p>For queries that require a term to match a specific {@link AppSearchSchema} property
     *       of a document, a ":" must be included between the property name and the term.
     *       <p>Example: "subject:important" matches documents that contain the term "important" in
     *       the "subject" property.
     * </ul>
     *
     * <p>The above description covers the basic query operators. Additional advanced query operator
     * features should be explicitly enabled in the SearchSpec and are described below.
     *
     * <p>LIST_FILTER_QUERY_LANGUAGE: This feature covers the expansion of the query language to
     * conform to the definition of the list filters language (https://aip.dev/160). This includes:
     *
     * <ul>
     *   <li>addition of explicit 'AND' and 'NOT' operators
     *   <li>property restricts are allowed with groupings (ex. "prop:(a OR b)")
     *   <li>addition of custom functions to control matching
     * </ul>
     *
     * <p>The newly added custom functions covered by this feature are:
     *
     * <ul>
     *   <li>createList(String...)
     *   <li>search(String, List&lt;String&gt;)
     *   <li>propertyDefined(String)
     * </ul>
     *
     * <p>createList takes a variable number of strings and returns a list of strings. It is for use
     * with search.
     *
     * <p>search takes a query string that will be parsed according to the supported query language
     * and an optional list of strings that specify the properties to be restricted to. This exists
     * as a convenience for multiple property restricts. So, for example, the query `(subject:foo OR
     * body:foo) (subject:bar OR body:bar)` could be rewritten as `search("foo bar",
     * createList("subject", "bar"))`.
     *
     * <p>propertyDefined takes a string specifying the property of interest and matches all
     * documents of any type that defines the specified property (ex.
     * `propertyDefined("sender.name")`). Note that propertyDefined will match so long as the
     * document's type defines the specified property. It does NOT require that the document
     * actually hold any values for this property.
     *
     * <p>NUMERIC_SEARCH: This feature covers numeric search expressions. In the query language, the
     * values of properties that have {@link AppSearchSchema.LongPropertyConfig#INDEXING_TYPE_RANGE}
     * set can be matched with a numeric search expression (the property, a supported comparator and
     * an integer value). Supported comparators are <, <=, ==, >= and >.
     *
     * <p>Ex. `price < 10` will match all documents that has a numeric value in its price property
     * that is less than 10.
     *
     * <p>VERBATIM_SEARCH: This feature covers the verbatim string operator (quotation marks).
     *
     * <p>Ex. `"foo/bar" OR baz` will ensure that 'foo/bar' is treated as a single 'verbatim' token.
     *
     * <p>Additional search specifications, such as filtering by {@link AppSearchSchema} type or
     * adding projection, can be set by calling the corresponding {@link SearchSpec.Builder} setter.
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
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return new SearchResults(
                mService,
                mCallerAttributionSource,
                mDatabaseName,
                queryExpression,
                searchSpec,
                mUserHandle,
                /* isForEnterprise= */ false);
    }

    /**
     * Retrieves suggested Strings that could be used as {@code queryExpression} in {@link
     * #search(String, SearchSpec)} API.
     *
     * <p>The {@code suggestionQueryExpression} can contain one term with no operators, or contain
     * multiple terms and operators. Operators will be considered as a normal term. Please see the
     * operator examples below. The {@code suggestionQueryExpression} must end with a valid term,
     * the suggestions are generated based on the last term. If the input {@code
     * suggestionQueryExpression} doesn't have a valid token, AppSearch will return an empty result
     * list. Please see the invalid examples below.
     *
     * <p>Example: if there are following documents with content stored in AppSearch.
     *
     * <ul>
     *   <li>document1: "term1"
     *   <li>document2: "term1 term2"
     *   <li>document3: "term1 term2 term3"
     *   <li>document4: "org"
     * </ul>
     *
     * <p>Search suggestions with the single term {@code suggestionQueryExpression} "t", the
     * suggested results are:
     *
     * <ul>
     *   <li>"term1" - Use it to be queryExpression in {@link #search} could get 3 {@link
     *       SearchResult}s, which contains document 1, 2 and 3.
     *   <li>"term2" - Use it to be queryExpression in {@link #search} could get 2 {@link
     *       SearchResult}s, which contains document 2 and 3.
     *   <li>"term3" - Use it to be queryExpression in {@link #search} could get 1 {@link
     *       SearchResult}, which contains document 3.
     * </ul>
     *
     * <p>Search suggestions with the multiple term {@code suggestionQueryExpression} "org t", the
     * suggested result will be "org term1" - The last token is completed by the suggested String.
     *
     * <p>Operators in {@link #search} are supported.
     *
     * <p><b>NOTE:</b> Exclusion and Grouped Terms in the last term is not supported.
     *
     * <p>example: "apple -f": This Api will throw an {@link
     * android.app.appsearch.exceptions.AppSearchException} with {@link
     * AppSearchResult#RESULT_INVALID_ARGUMENT}.
     *
     * <p>example: "apple (f)": This Api will return an empty results.
     *
     * <p>Invalid example: All these input {@code suggestionQueryExpression} don't have a valid last
     * token, AppSearch will return an empty result list.
     *
     * <ul>
     *   <li>"" - Empty {@code suggestionQueryExpression}.
     *   <li>"(f)" - Ending in a closed brackets.
     *   <li>"f:" - Ending in an operator.
     *   <li>"f " - Ending in trailing space.
     * </ul>
     *
     * @param suggestionQueryExpression the non empty query string to search suggestions
     * @param searchSuggestionSpec spec for setting document filters
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive the pending result of performing this operation, which is
     *     a List of {@link SearchSuggestionResult} on success. The returned suggestion Strings are
     *     ordered by the number of {@link SearchResult} you could get by using that suggestion in
     *     {@link #search}.
     * @see #search(String, SearchSpec)
     */
    public void searchSuggestion(
            @NonNull String suggestionQueryExpression,
            @NonNull SearchSuggestionSpec searchSuggestionSpec,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<List<SearchSuggestionResult>>> callback) {
        Objects.requireNonNull(suggestionQueryExpression);
        Objects.requireNonNull(searchSuggestionSpec);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        try {
            mService.searchSuggestion(
                    new SearchSuggestionAidlRequest(
                            mCallerAttributionSource,
                            mDatabaseName,
                            suggestionQueryExpression,
                            searchSuggestionSpec,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public void onResult(AppSearchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> {
                                        try {
                                            AppSearchResult<List<SearchSuggestionResult>> result =
                                                    resultParcel.getResult();
                                            if (result.isSuccess()) {
                                                callback.accept(result);
                                            } else {
                                                // TODO(b/261897334) save SDK errors/crashes and
                                                // send to
                                                //  server for logging.
                                                callback.accept(
                                                        AppSearchResult.newFailedResult(result));
                                            }
                                        } catch (Exception e) {
                                            callback.accept(
                                                    AppSearchResult.throwableToFailedResult(e));
                                        }
                                    });
                        }
                    });
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        }
    }

    /**
     * Reports usage of a particular document by namespace and ID.
     *
     * <p>A usage report represents an event in which a user interacted with or viewed a document.
     *
     * <p>For each call to {@link #reportUsage}, AppSearch updates usage count and usage recency
     * metrics for that particular document. These metrics are used for ordering {@link #search}
     * results by the {@link SearchSpec#RANKING_STRATEGY_USAGE_COUNT} and {@link
     * SearchSpec#RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP} ranking strategies.
     *
     * <p>Reporting usage of a document is optional.
     *
     * @param request The usage reporting request.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive errors. If the operation succeeds, the callback will be
     *     invoked with {@code null}.
     */
    public void reportUsage(
            @NonNull ReportUsageRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<Void>> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        String targetPackageName = mCallerAttributionSource.getPackageName();
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        try {
            mService.reportUsage(
                    new ReportUsageAidlRequest(
                            mCallerAttributionSource,
                            targetPackageName,
                            mDatabaseName,
                            request,
                            /* systemUsage= */ false,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public void onResult(AppSearchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> callback.accept(resultParcel.getResult()));
                        }
                    });
            mIsMutated = true;
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        }
    }

    /**
     * Removes {@link GenericDocument} objects by document IDs in a namespace from the {@link
     * AppSearchSession} database.
     *
     * <p>Removed documents will no longer be surfaced by {@link #search} or {@link
     * #getByDocumentId} calls.
     *
     * <p>Once the database crosses the document count or byte usage threshold, removed documents
     * will be deleted from disk.
     *
     * @param request {@link RemoveByDocumentIdRequest} with IDs in a namespace to remove from the
     *     index.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive the pending result of performing this operation. The keys
     *     of the returned {@link AppSearchBatchResult} are the input document IDs. The values are
     *     {@code null} on success, or a failed {@link AppSearchResult} otherwise. IDs that are not
     *     found will return a failed {@link AppSearchResult} with a result code of {@link
     *     AppSearchResult#RESULT_NOT_FOUND}. If an unexpected internal error occurs in the
     *     AppSearch service, {@link BatchResultCallback#onSystemError} will be invoked with a
     *     {@link Throwable}.
     */
    public void remove(
            @NonNull RemoveByDocumentIdRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull BatchResultCallback<String, Void> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        try {
            mService.removeByDocumentId(
                    new RemoveByDocumentIdAidlRequest(
                            mCallerAttributionSource,
                            mDatabaseName,
                            request,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new IAppSearchBatchResultCallback.Stub() {
                        @Override
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public void onResult(AppSearchBatchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> callback.onResult(resultParcel.getResult()));
                        }

                        @Override
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public void onSystemError(AppSearchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () ->
                                            SearchSessionUtil.sendSystemErrorToCallback(
                                                    resultParcel.getResult(), callback));
                        }
                    });
            mIsMutated = true;
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        }
    }

    /**
     * Removes {@link GenericDocument}s from the index by Query. Documents will be removed if they
     * match the {@code queryExpression} in given namespaces and schemaTypes which is set via {@link
     * SearchSpec.Builder#addFilterNamespaces} and {@link SearchSpec.Builder#addFilterSchemas}.
     *
     * <p>An empty {@code queryExpression} matches all documents.
     *
     * <p>An empty set of namespaces or schemaTypes matches all namespaces or schemaTypes in the
     * current database.
     *
     * @param queryExpression Query String to search.
     * @param searchSpec Spec containing schemaTypes, namespaces and query expression indicates how
     *     document will be removed. All specific about how to scoring, ordering, snippeting and
     *     resulting will be ignored.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive errors resulting from removing the documents. If the
     *     operation succeeds, the callback will be invoked with {@code null}.
     */
    public void remove(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<Void>> callback) {
        Objects.requireNonNull(queryExpression);
        Objects.requireNonNull(searchSpec);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        if (searchSpec.getJoinSpec() != null) {
            throw new IllegalArgumentException(
                    "JoinSpec not allowed in removeByQuery, but " + "JoinSpec was provided.");
        }
        try {
            mService.removeByQuery(
                    new RemoveByQueryAidlRequest(
                            mCallerAttributionSource,
                            mDatabaseName,
                            queryExpression,
                            searchSpec,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public void onResult(AppSearchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> callback.accept(resultParcel.getResult()));
                        }
                    });
            mIsMutated = true;
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        }
    }

    /**
     * Gets the storage info for this {@link AppSearchSession} database.
     *
     * <p>This may take time proportional to the number of documents and may be inefficient to call
     * repeatedly.
     *
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive the storage info.
     */
    public void getStorageInfo(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<StorageInfo>> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        try {
            mService.getStorageInfo(
                    new GetStorageInfoAidlRequest(
                            mCallerAttributionSource,
                            mDatabaseName,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public void onResult(AppSearchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> {
                                        AppSearchResult<StorageInfo> result =
                                                resultParcel.getResult();
                                        if (result.isSuccess()) {
                                            callback.accept(
                                                    AppSearchResult.newSuccessfulResult(
                                                            result.getResultValue()));
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
     * Closes the {@link AppSearchSession} to persist all schema and document updates, additions,
     * and deletes to disk.
     */
    @Override
    public void close() {
        if (mIsMutated && !mIsClosed) {
            try {
                mService.persistToDisk(
                        new PersistToDiskAidlRequest(
                                mCallerAttributionSource,
                                mUserHandle,
                                /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()));
                mIsClosed = true;
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to close the AppSearchSession", e);
            }
        }
    }

    /**
     * Set schema to Icing for no-migration scenario.
     *
     * <p>We only need one time {@link #setSchema} call for no-migration scenario by using the
     * forceoverride in the request.
     */
    private void setSchemaNoMigrations(
            @NonNull SetSchemaRequest request,
            @NonNull List<AppSearchSchema> schemas,
            @NonNull List<InternalVisibilityConfig> visibilityConfigs,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<SetSchemaResponse>> callback) {
        try {
            SetSchemaAidlRequest setSchemaAidlRequest =
                    new SetSchemaAidlRequest(
                            mCallerAttributionSource,
                            mDatabaseName,
                            schemas,
                            visibilityConfigs,
                            request.isForceOverride(),
                            request.getVersion(),
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime(),
                            SchemaMigrationStats.NO_MIGRATION);
            mService.setSchema(
                    setSchemaAidlRequest,
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public void onResult(AppSearchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> {
                                        AppSearchResult<InternalSetSchemaResponse> result =
                                                resultParcel.getResult();
                                        if (result.isSuccess()) {
                                            try {
                                                InternalSetSchemaResponse
                                                        internalSetSchemaResponse =
                                                                result.getResultValue();
                                                if (internalSetSchemaResponse == null) {
                                                    // Ideally internalSetSchemaResponse should
                                                    // always be non-null as result is success. In
                                                    // other cases we directly put result in
                                                    // AppSearchResult.newSuccessfulResult which
                                                    // accepts a Nullable value, here we need to
                                                    // get response by
                                                    // internalSetSchemaResponse
                                                    // .getSetSchemaResponse().
                                                    callback.accept(
                                                            AppSearchResult.newFailedResult(
                                                                    RESULT_INTERNAL_ERROR,
                                                                    "Received null"
                                                                            + " InternalSetSchema"
                                                                            + "Response"
                                                                            + " during setSchema"
                                                                            + " call"));
                                                    return;
                                                }
                                                if (!internalSetSchemaResponse.isSuccess()) {
                                                    // check is the set schema call failed
                                                    // because incompatible changes. That's the only
                                                    // case we swallowed in the
                                                    // AppSearchImpl#setSchema().
                                                    callback.accept(
                                                            AppSearchResult.newFailedResult(
                                                                    AppSearchResult
                                                                            .RESULT_INVALID_SCHEMA,
                                                                    internalSetSchemaResponse
                                                                            .getErrorMessage()));
                                                    return;
                                                }
                                                callback.accept(
                                                        AppSearchResult.newSuccessfulResult(
                                                                internalSetSchemaResponse
                                                                        .getSetSchemaResponse()));
                                            } catch (RuntimeException e) {
                                                // TODO(b/261897334) save SDK errors/crashes and
                                                // send to
                                                //  server for logging.
                                                callback.accept(
                                                        AppSearchResult.throwableToFailedResult(e));
                                            }
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
     * Set schema to Icing for migration scenario.
     *
     * <p>First time {@link #setSchema} call with forceOverride is false gives us all incompatible
     * changes. After trigger migrations, the second time call {@link #setSchema} will actually
     * apply the changes.
     */
    private void setSchemaWithMigrations(
            @NonNull SetSchemaRequest request,
            @NonNull List<AppSearchSchema> schemas,
            @NonNull List<InternalVisibilityConfig> visibilityConfigs,
            @NonNull Executor workExecutor,
            @NonNull @CallbackExecutor Executor callbackExecutor,
            @NonNull Consumer<AppSearchResult<SetSchemaResponse>> callback) {
        long totalLatencyStartTimeMillis = SystemClock.elapsedRealtime();
        long waitExecutorStartLatencyMillis = SystemClock.elapsedRealtime();
        safeExecute(
                workExecutor,
                callback,
                () -> {
                    try {
                        long waitExecutorEndLatencyMillis = SystemClock.elapsedRealtime();
                        String packageName = mCallerAttributionSource.getPackageName();
                        SchemaMigrationStats.Builder statsBuilder =
                                new SchemaMigrationStats.Builder(packageName, mDatabaseName);

                        // Migration process
                        // 1. Validate and retrieve all active migrators.
                        long getSchemaLatencyStartTimeMillis = SystemClock.elapsedRealtime();
                        CountDownLatch getSchemaLatch = new CountDownLatch(1);
                        AtomicReference<AppSearchResult<GetSchemaResponse>> getSchemaResultRef =
                                new AtomicReference<>();
                        getSchema(
                                callbackExecutor,
                                (result) -> {
                                    getSchemaResultRef.set(result);
                                    getSchemaLatch.countDown();
                                });
                        getSchemaLatch.await();
                        AppSearchResult<GetSchemaResponse> getSchemaResult =
                                getSchemaResultRef.get();
                        if (!getSchemaResult.isSuccess()) {
                            // TODO(b/261897334) save SDK errors/crashes and send to server for
                            // logging.
                            safeExecute(
                                    callbackExecutor,
                                    callback,
                                    () ->
                                            callback.accept(
                                                    AppSearchResult.newFailedResult(
                                                            getSchemaResult)));
                            return;
                        }
                        GetSchemaResponse getSchemaResponse =
                                Objects.requireNonNull(getSchemaResult.getResultValue());
                        int currentVersion = getSchemaResponse.getVersion();
                        int finalVersion = request.getVersion();
                        Map<String, Migrator> activeMigrators =
                                SchemaMigrationUtil.getActiveMigrators(
                                        getSchemaResponse.getSchemas(),
                                        request.getMigrators(),
                                        currentVersion,
                                        finalVersion);
                        long getSchemaLatencyEndTimeMillis = SystemClock.elapsedRealtime();

                        // No need to trigger migration if no migrator is active.
                        if (activeMigrators.isEmpty()) {
                            setSchemaNoMigrations(
                                    request,
                                    schemas,
                                    visibilityConfigs,
                                    callbackExecutor,
                                    callback);
                            return;
                        }

                        // 2. SetSchema with forceOverride=false, to retrieve the list of
                        // incompatible/deleted types.
                        long firstSetSchemaLatencyStartMillis = SystemClock.elapsedRealtime();
                        CountDownLatch setSchemaLatch = new CountDownLatch(1);
                        AtomicReference<AppSearchResult<InternalSetSchemaResponse>>
                                setSchemaResultRef = new AtomicReference<>();

                        SetSchemaAidlRequest setSchemaAidlRequest =
                                new SetSchemaAidlRequest(
                                        mCallerAttributionSource,
                                        mDatabaseName,
                                        schemas,
                                        visibilityConfigs,
                                        /* forceOverride= */ false,
                                        request.getVersion(),
                                        mUserHandle,
                                        /* binderCallStartTimeMillis= */ SystemClock
                                                .elapsedRealtime(),
                                        SchemaMigrationStats.FIRST_CALL_GET_INCOMPATIBLE);
                        mService.setSchema(
                                setSchemaAidlRequest,
                                new IAppSearchResultCallback.Stub() {
                                    @Override
                                    @SuppressWarnings({"rawtypes", "unchecked"})
                                    public void onResult(AppSearchResultParcel resultParcel) {
                                        setSchemaResultRef.set(resultParcel.getResult());
                                        setSchemaLatch.countDown();
                                    }
                                });
                        setSchemaLatch.await();
                        AppSearchResult<InternalSetSchemaResponse> setSchemaResult =
                                setSchemaResultRef.get();
                        if (!setSchemaResult.isSuccess()) {
                            // TODO(b/261897334) save SDK errors/crashes and send to server for
                            // logging.
                            safeExecute(
                                    callbackExecutor,
                                    callback,
                                    () ->
                                            callback.accept(
                                                    AppSearchResult.newFailedResult(
                                                            setSchemaResult)));
                            return;
                        }
                        InternalSetSchemaResponse internalSetSchemaResponse1 =
                                setSchemaResult.getResultValue();
                        if (internalSetSchemaResponse1 == null) {
                            safeExecute(
                                    callbackExecutor,
                                    callback,
                                    () ->
                                            callback.accept(
                                                    AppSearchResult.newFailedResult(
                                                            RESULT_INTERNAL_ERROR,
                                                            "Received null"
                                                                    + " InternalSetSchemaResponse"
                                                                    + " during setSchema call")));
                            return;
                        }
                        long firstSetSchemaLatencyEndTimeMillis = SystemClock.elapsedRealtime();

                        // 3. If forceOverride is false, check that all incompatible types will be
                        // migrated.
                        // If some aren't we must throw an error, rather than proceeding and
                        // deleting those
                        // types.
                        SchemaMigrationUtil.checkDeletedAndIncompatibleAfterMigration(
                                internalSetSchemaResponse1, activeMigrators.keySet());

                        try (AppSearchMigrationHelper migrationHelper =
                                new AppSearchMigrationHelper(
                                        mService,
                                        mUserHandle,
                                        mCallerAttributionSource,
                                        mDatabaseName,
                                        request.getSchemas(),
                                        mCacheDirectory)) {

                            // 4. Trigger migration for all migrators.
                            long queryAndTransformLatencyStartMillis =
                                    SystemClock.elapsedRealtime();
                            for (Map.Entry<String, Migrator> entry : activeMigrators.entrySet()) {
                                migrationHelper.queryAndTransform(
                                        /* schemaType= */ entry.getKey(),
                                        /* migrator= */ entry.getValue(),
                                        currentVersion,
                                        finalVersion,
                                        statsBuilder);
                            }
                            long queryAndTransformLatencyEndTimeMillis =
                                    SystemClock.elapsedRealtime();

                            // 5. SetSchema a second time with forceOverride=true if the first
                            // attempted
                            // failed.
                            long secondSetSchemaLatencyStartMillis = SystemClock.elapsedRealtime();
                            InternalSetSchemaResponse internalSetSchemaResponse;
                            if (internalSetSchemaResponse1.isSuccess()) {
                                internalSetSchemaResponse = internalSetSchemaResponse1;
                            } else {
                                CountDownLatch setSchema2Latch = new CountDownLatch(1);
                                AtomicReference<AppSearchResult<InternalSetSchemaResponse>>
                                        setSchema2ResultRef = new AtomicReference<>();
                                // only trigger second setSchema() call if the first one is fail.
                                SetSchemaAidlRequest setSchemaAidlRequest1 =
                                        new SetSchemaAidlRequest(
                                                mCallerAttributionSource,
                                                mDatabaseName,
                                                schemas,
                                                visibilityConfigs,
                                                /* forceOverride= */ true,
                                                request.getVersion(),
                                                mUserHandle,
                                                /* binderCallStartTimeMillis= */ SystemClock
                                                        .elapsedRealtime(),
                                                SchemaMigrationStats.SECOND_CALL_APPLY_NEW_SCHEMA);
                                mService.setSchema(
                                        setSchemaAidlRequest1,
                                        new IAppSearchResultCallback.Stub() {
                                            @Override
                                            @SuppressWarnings({"rawtypes", "unchecked"})
                                            public void onResult(
                                                    AppSearchResultParcel resultParcel) {
                                                setSchema2ResultRef.set(resultParcel.getResult());
                                                setSchema2Latch.countDown();
                                            }
                                        });
                                setSchema2Latch.await();
                                AppSearchResult<InternalSetSchemaResponse> setSchema2Result =
                                        setSchema2ResultRef.get();
                                if (!setSchema2Result.isSuccess()) {
                                    // we failed to set the schema in second time with forceOverride
                                    // = true, which is an impossible case. Since we only swallow
                                    // the incompatible error in the first setSchema call, all other
                                    // errors will be thrown at the first time.
                                    // TODO(b/261897334) save SDK errors/crashes and send to server
                                    //  for logging.
                                    safeExecute(
                                            callbackExecutor,
                                            callback,
                                            () ->
                                                    callback.accept(
                                                            AppSearchResult.newFailedResult(
                                                                    setSchema2Result)));
                                    return;
                                }
                                InternalSetSchemaResponse internalSetSchemaResponse2 =
                                        setSchema2Result.getResultValue();
                                if (internalSetSchemaResponse2 == null) {
                                    safeExecute(
                                            callbackExecutor,
                                            callback,
                                            () ->
                                                    callback.accept(
                                                            AppSearchResult.newFailedResult(
                                                                    RESULT_INTERNAL_ERROR,
                                                                    "Received null response"
                                                                            + " during setSchema"
                                                                            + " call")));
                                    return;
                                }
                                if (!internalSetSchemaResponse2.isSuccess()) {
                                    // Impossible case, we just set forceOverride to be true, we
                                    // should never fail in incompatible changes. And all other
                                    // cases should failed during the first call.
                                    // TODO(b/261897334) save SDK errors/crashes and send to server
                                    //  for logging.
                                    safeExecute(
                                            callbackExecutor,
                                            callback,
                                            () ->
                                                    callback.accept(
                                                            AppSearchResult.newFailedResult(
                                                                    RESULT_INTERNAL_ERROR,
                                                                    internalSetSchemaResponse2
                                                                            .getErrorMessage())));
                                    return;
                                }
                                internalSetSchemaResponse = internalSetSchemaResponse2;
                            }
                            long secondSetSchemaLatencyEndTimeMillis =
                                    SystemClock.elapsedRealtime();

                            statsBuilder
                                    .setExecutorAcquisitionLatencyMillis(
                                            (int)
                                                    (waitExecutorEndLatencyMillis
                                                            - waitExecutorStartLatencyMillis))
                                    .setGetSchemaLatencyMillis(
                                            (int)
                                                    (getSchemaLatencyEndTimeMillis
                                                            - getSchemaLatencyStartTimeMillis))
                                    .setFirstSetSchemaLatencyMillis(
                                            (int)
                                                    (firstSetSchemaLatencyEndTimeMillis
                                                            - firstSetSchemaLatencyStartMillis))
                                    .setIsFirstSetSchemaSuccess(
                                            internalSetSchemaResponse1.isSuccess())
                                    .setQueryAndTransformLatencyMillis(
                                            (int)
                                                    (queryAndTransformLatencyEndTimeMillis
                                                            - queryAndTransformLatencyStartMillis))
                                    .setSecondSetSchemaLatencyMillis(
                                            (int)
                                                    (secondSetSchemaLatencyEndTimeMillis
                                                            - secondSetSchemaLatencyStartMillis));
                            SetSchemaResponse.Builder responseBuilder =
                                    new SetSchemaResponse.Builder(
                                                    internalSetSchemaResponse
                                                            .getSetSchemaResponse())
                                            .addMigratedTypes(activeMigrators.keySet());

                            // 6. Put all the migrated documents into the index, now that the new
                            // schema is
                            // set.
                            AppSearchResult<SetSchemaResponse> putResult =
                                    migrationHelper.putMigratedDocuments(
                                            responseBuilder,
                                            statsBuilder,
                                            totalLatencyStartTimeMillis);
                            safeExecute(
                                    callbackExecutor, callback, () -> callback.accept(putResult));
                        }
                    } catch (RemoteException
                            | AppSearchException
                            | InterruptedException
                            | IOException
                            | ExecutionException
                            | RuntimeException e) {
                        // TODO(b/261897334) save SDK errors/crashes and send to server for logging.
                        safeExecute(
                                callbackExecutor,
                                callback,
                                () -> callback.accept(AppSearchResult.throwableToFailedResult(e)));
                    }
                });
    }

    @NonNull
    private static List<GenericDocumentParcel> toGenericDocumentParcels(
            List<GenericDocument> docs) {
        List<GenericDocumentParcel> docParcels = new ArrayList<>(docs.size());
        for (int i = 0; i < docs.size(); ++i) {
            docParcels.add(docs.get(i).getDocumentParcel());
        }
        return docParcels;
    }
}
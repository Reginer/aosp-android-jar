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

import static android.app.appsearch.SearchSessionUtil.safeExecute;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.app.appsearch.aidl.AppSearchResultParcel;
import android.app.appsearch.aidl.IAppSearchManager;
import android.app.appsearch.aidl.IAppSearchObserverProxy;
import android.app.appsearch.aidl.IAppSearchResultCallback;
import android.app.appsearch.exceptions.AppSearchException;
import android.app.appsearch.observer.DocumentChangeInfo;
import android.app.appsearch.observer.ObserverCallback;
import android.app.appsearch.observer.ObserverSpec;
import android.app.appsearch.observer.SchemaChangeInfo;
import android.content.AttributionSource;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Provides a connection to all AppSearch databases the querying application has been granted access
 * to.
 *
 * <p>This class is thread safe.
 *
 * @see AppSearchSession
 */
public class GlobalSearchSession implements Closeable {
    private static final String TAG = "AppSearchGlobalSearchSe";

    private final UserHandle mUserHandle;
    private final IAppSearchManager mService;
    private final AttributionSource mCallerAttributionSource;

    // Management of observer callbacks. Key is observed package.
    @GuardedBy("mObserverCallbacksLocked")
    private final Map<String, Map<ObserverCallback, IAppSearchObserverProxy>>
            mObserverCallbacksLocked = new ArrayMap<>();

    private boolean mIsMutated = false;
    private boolean mIsClosed = false;

    /**
     * Creates a search session for the client, defined by the {@code userHandle} and
     * {@code packageName}.
     */
    static void createGlobalSearchSession(
            @NonNull IAppSearchManager service,
            @NonNull UserHandle userHandle,
            @NonNull AttributionSource attributionSource,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<GlobalSearchSession>> callback) {
        GlobalSearchSession globalSearchSession = new GlobalSearchSession(service, userHandle,
                attributionSource);
        globalSearchSession.initialize(executor, callback);
    }

    // NOTE: No instance of this class should be created or returned except via initialize().
    // Once the callback.accept has been called here, the class is ready to use.
    private void initialize(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<GlobalSearchSession>> callback) {
        try {
            mService.initialize(
                    mCallerAttributionSource,
                    mUserHandle,
                    /*binderCallStartTimeMillis=*/ SystemClock.elapsedRealtime(),
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        public void onResult(AppSearchResultParcel resultParcel) {
                            safeExecute(executor, callback, () -> {
                                AppSearchResult<Void> result = resultParcel.getResult();
                                if (result.isSuccess()) {
                                    callback.accept(
                                            AppSearchResult.newSuccessfulResult(
                                                    GlobalSearchSession.this));
                                } else {
                                    callback.accept(AppSearchResult.newFailedResult(result));
                                }
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private GlobalSearchSession(@NonNull IAppSearchManager service, @NonNull UserHandle userHandle,
            @NonNull AttributionSource callerAttributionSource) {
        mService = service;
        mUserHandle = userHandle;
        mCallerAttributionSource = callerAttributionSource;
    }

    /**
     * Retrieves {@link GenericDocument} documents, belonging to the specified package name and
     * database name and identified by the namespace and ids in the request, from the
     * {@link GlobalSearchSession} database.
     *
     * <p>If the package or database doesn't exist or if the calling package doesn't have access,
     * the gets will be handled as failures in an {@link AppSearchBatchResult} object in the
     * callback.
     *
     * @param packageName  the name of the package to get from
     * @param databaseName the name of the database to get from
     * @param request      a request containing a namespace and IDs to get documents for.
     * @param executor     Executor on which to invoke the callback.
     * @param callback     Callback to receive the pending result of performing this operation. The
     *                     keys of the returned {@link AppSearchBatchResult} are the input IDs. The
     *                     values are the returned {@link GenericDocument}s on success, or a failed
     *                     {@link AppSearchResult} otherwise. IDs that are not found will return a
     *                     failed {@link AppSearchResult} with a result code of
     *                     {@link AppSearchResult#RESULT_NOT_FOUND}. If an unexpected internal error
     *                     occurs in the AppSearch service,
     *                     {@link BatchResultCallback#onSystemError} will be invoked with a
     *                     {@link Throwable}.
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
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");

        try {
            mService.getDocuments(
                    mCallerAttributionSource,
                    /*targetPackageName=*/packageName,
                    databaseName,
                    request.getNamespace(),
                    new ArrayList<>(request.getIds()),
                    request.getProjectionsInternal(),
                    mUserHandle,
                    /*binderCallStartTimeMillis=*/ SystemClock.elapsedRealtime(),
                    SearchSessionUtil.createGetDocumentCallback(executor, callback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
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
     * @param searchSpec      spec for setting document filters, adding projection, setting term
     *                        match type, etc.
     * @return a {@link SearchResults} object for retrieved matched documents.
     */
    @NonNull
    public SearchResults search(@NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Objects.requireNonNull(queryExpression);
        Objects.requireNonNull(searchSpec);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return new SearchResults(mService, mCallerAttributionSource, /*databaseName=*/null,
                queryExpression, searchSpec, mUserHandle);
    }

    /**
     * Reports that a particular document has been used from a system surface.
     *
     * <p>See {@link AppSearchSession#reportUsage} for a general description of document usage, as
     * well as an API that can be used by the app itself.
     *
     * <p>Usage reported via this method is accounted separately from usage reported via
     * {@link AppSearchSession#reportUsage} and may be accessed using the constants
     * {@link SearchSpec#RANKING_STRATEGY_SYSTEM_USAGE_COUNT} and
     * {@link SearchSpec#RANKING_STRATEGY_SYSTEM_USAGE_LAST_USED_TIMESTAMP}.
     *
     * @param request  The usage reporting request.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive errors. If the operation succeeds, the callback will be
     *                 invoked with an {@link AppSearchResult} whose value is {@code null}. The
     *                 callback will be invoked with an {@link AppSearchResult} of
     *                 {@link AppSearchResult#RESULT_SECURITY_ERROR} if this API is invoked by an
     *                 app which is not part of the system.
     */
    public void reportSystemUsage(
            @NonNull ReportSystemUsageRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<Void>> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        try {
            mService.reportUsage(
                    mCallerAttributionSource,
                    request.getPackageName(),
                    request.getDatabaseName(),
                    request.getNamespace(),
                    request.getDocumentId(),
                    request.getUsageTimestampMillis(),
                    /*systemUsage=*/ true,
                    mUserHandle,
                    /*binderCallStartTimeMillis=*/ SystemClock.elapsedRealtime(),
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        public void onResult(AppSearchResultParcel resultParcel) {
                            safeExecute(
                                    executor,
                                    callback,
                                    () -> callback.accept(resultParcel.getResult()));
                        }
                    });
            mIsMutated = true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Retrieves the collection of schemas most recently successfully provided to {@link
     * AppSearchSession#setSchema} for any types belonging to the requested package and database
     * that the caller has been granted access to.
     *
     * <p>If the requested package/database combination does not exist or the caller has not been
     * granted access to it, then an empty GetSchemaResponse will be returned.
     *
     * @param packageName  the package that owns the requested {@link AppSearchSchema} instances.
     * @param databaseName the database that owns the requested {@link AppSearchSchema} instances.
     * @return The pending {@link GetSchemaResponse} containing the schemas that the caller has
     *         access to or an empty GetSchemaResponse if the request package and database does not
     *         exist, has not set a schema or contains no schemas that are accessible to the caller.
     */
    // This call hits disk; async API prevents us from treating these calls as properties.
    public void getSchema(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<GetSchemaResponse>> callback) {
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(databaseName);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        try {
            mService.getSchema(
                    mCallerAttributionSource,
                    packageName,
                    databaseName,
                    mUserHandle,
                    /*binderCallStartTimeMillis=*/ SystemClock.elapsedRealtime(),
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        public void onResult(AppSearchResultParcel resultParcel) {
                            safeExecute(executor, callback, () -> {
                                AppSearchResult<Bundle> result = resultParcel.getResult();
                                if (result.isSuccess()) {
                                    GetSchemaResponse response = new GetSchemaResponse(
                                            Objects.requireNonNull(result.getResultValue()));
                                    callback.accept(AppSearchResult.newSuccessfulResult(response));
                                } else {
                                    callback.accept(AppSearchResult.newFailedResult(result));
                                }
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Adds an {@link ObserverCallback} to monitor changes within the databases owned by
     * {@code targetPackageName} if they match the given
     * {@link android.app.appsearch.observer.ObserverSpec}.
     *
     * <p>The observer callback is only triggered for data that changes after it is registered. No
     * notification about existing data is sent as a result of registering an observer. To find out
     * about existing data, you must use the {@link GlobalSearchSession#search} API.
     *
     * <p>If the data owned by {@code targetPackageName} is not visible to you, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} changes its schema visibility settings.
     *
     * <p>If no package matching {@code targetPackageName} exists on the system, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} is installed and starts indexing data.
     *
     * @param targetPackageName Package whose changes to monitor
     * @param spec              Specification of what types of changes to listen for
     * @param executor          Executor on which to call the {@code observer} callback methods.
     * @param observer          Callback to trigger when a schema or document changes
     * @throws AppSearchException If an unexpected error occurs when trying to register an observer.
     */
    public void registerObserverCallback(
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull ObserverCallback observer) throws AppSearchException {
        Objects.requireNonNull(targetPackageName);
        Objects.requireNonNull(spec);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(observer);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");

        synchronized (mObserverCallbacksLocked) {
            IAppSearchObserverProxy stub = null;
            Map<ObserverCallback, IAppSearchObserverProxy> observersForPackage =
                    mObserverCallbacksLocked.get(targetPackageName);
            if (observersForPackage != null) {
                stub = observersForPackage.get(observer);
            }
            if (stub == null) {
                // No stub is associated with this package and observer, so we must create one.
                stub = new IAppSearchObserverProxy.Stub() {
                    @Override
                    public void onSchemaChanged(
                            @NonNull String packageName,
                            @NonNull String databaseName,
                            @NonNull List<String> changedSchemaNames) {
                        safeExecute(executor, this::suppressingErrorCallback, () -> {
                            SchemaChangeInfo changeInfo = new SchemaChangeInfo(
                                    packageName, databaseName, new ArraySet<>(changedSchemaNames));
                            observer.onSchemaChanged(changeInfo);
                        });
                    }

                    @Override
                    public void onDocumentChanged(
                            @NonNull String packageName,
                            @NonNull String databaseName,
                            @NonNull String namespace,
                            @NonNull String schemaName,
                            @NonNull List<String> changedDocumentIds) {
                        safeExecute(executor, this::suppressingErrorCallback, () -> {
                            DocumentChangeInfo changeInfo = new DocumentChangeInfo(
                                    packageName,
                                    databaseName,
                                    namespace,
                                    schemaName,
                                    new ArraySet<>(changedDocumentIds));
                            observer.onDocumentChanged(changeInfo);
                        });
                    }

                    /**
                     * Error-handling callback that simply drops errors.
                     *
                     * <p>If we fail to deliver change notifications, there isn't much we can do.
                     * The API doesn't allow the user to provide a callback to invoke on failure of
                     * change notification delivery. {@link SearchSessionUtil#safeExecute} already
                     * includes a log message. So we just do nothing.
                     */
                    private void suppressingErrorCallback(@NonNull AppSearchResult<?> unused) {
                    }
                };
            }

            // Regardless of whether this stub was fresh or not, we have to register it again
            // because the user might be supplying a different spec.
            AppSearchResultParcel<Void> resultParcel;
            try {
                resultParcel = mService.registerObserverCallback(
                        mCallerAttributionSource,
                        targetPackageName,
                        spec.getBundle(),
                        mUserHandle,
                        /*binderCallStartTimeMillis=*/ SystemClock.elapsedRealtime(),
                        stub);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }

            // See whether registration was successful
            AppSearchResult<Void> result = resultParcel.getResult();
            if (!result.isSuccess()) {
                throw new AppSearchException(result.getResultCode(), result.getErrorMessage());
            }

            // Now that registration has succeeded, save this stub into our in-memory cache. This
            // isn't done when errors occur because the user may not call unregisterObserverCallback
            // if registerObserverCallback threw.
            if (observersForPackage == null) {
                observersForPackage = new ArrayMap<>();
                mObserverCallbacksLocked.put(targetPackageName, observersForPackage);
            }
            observersForPackage.put(observer, stub);
        }
    }

    /**
     * Removes previously registered {@link ObserverCallback} instances from the system.
     *
     * <p>All instances of {@link ObserverCallback} which are registered to observe
     * {@code targetPackageName} and compare equal to the provided callback using the provided
     * argument's {@code ObserverCallback#equals} will be removed.
     *
     * <p>If no matching observers have been registered, this method has no effect. If multiple
     * matching observers have been registered, all will be removed.
     *
     * @param targetPackageName Package which the observers to be removed are listening to.
     * @param observer          Callback to unregister.
     * @throws AppSearchException if an error occurs trying to remove the observer, such as a
     *                            failure to communicate with the system service. Note that no error
     *                            will be thrown if the provided observer doesn't match any
     *                            registered observer.
     */
    public void unregisterObserverCallback(
            @NonNull String targetPackageName,
            @NonNull ObserverCallback observer) throws AppSearchException {
        Objects.requireNonNull(targetPackageName);
        Objects.requireNonNull(observer);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");

        IAppSearchObserverProxy stub;
        synchronized (mObserverCallbacksLocked) {
            Map<ObserverCallback, IAppSearchObserverProxy> observersForPackage =
                    mObserverCallbacksLocked.get(targetPackageName);
            if (observersForPackage == null) {
                return;  // No observers registered for this package. Nothing to do.
            }
            stub = observersForPackage.get(observer);
            if (stub == null) {
                return;  // No such observer registered. Nothing to do.
            }

            AppSearchResultParcel<Void> resultParcel;
            try {
                resultParcel = mService.unregisterObserverCallback(
                        mCallerAttributionSource,
                        targetPackageName,
                        mUserHandle,
                        /*binderCallStartTimeMillis=*/ SystemClock.elapsedRealtime(),
                        stub);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }

            AppSearchResult<Void> result = resultParcel.getResult();
            if (!result.isSuccess()) {
                throw new AppSearchException(result.getResultCode(), result.getErrorMessage());
            }

            // Only remove from the in-memory map once removal from the service side succeeds
            observersForPackage.remove(observer);
            if (observersForPackage.isEmpty()) {
                mObserverCallbacksLocked.remove(targetPackageName);
            }
        }
    }

    /**
     * Closes the {@link GlobalSearchSession}. Persists all mutations, including usage reports, to
     * disk.
     */
    @Override
    public void close() {
        if (mIsMutated && !mIsClosed) {
            try {
                mService.persistToDisk(
                        mCallerAttributionSource,
                        mUserHandle,
                        /*binderCallStartTimeMillis=*/ SystemClock.elapsedRealtime());
                mIsClosed = true;
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to close the GlobalSearchSession", e);
            }
        }
    }
}

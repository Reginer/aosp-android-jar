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
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.app.appsearch.aidl.AppSearchAttributionSource;
import android.app.appsearch.aidl.AppSearchResultCallback;
import android.app.appsearch.aidl.AppSearchResultParcel;
import android.app.appsearch.aidl.IAppSearchManager;
import android.app.appsearch.aidl.IAppSearchObserverProxy;
import android.app.appsearch.aidl.PersistToDiskAidlRequest;
import android.app.appsearch.aidl.RegisterObserverCallbackAidlRequest;
import android.app.appsearch.aidl.ReportUsageAidlRequest;
import android.app.appsearch.aidl.UnregisterObserverCallbackAidlRequest;
import android.app.appsearch.exceptions.AppSearchException;
import android.app.appsearch.observer.DocumentChangeInfo;
import android.app.appsearch.observer.ObserverCallback;
import android.app.appsearch.observer.ObserverSpec;
import android.app.appsearch.observer.SchemaChangeInfo;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import com.android.appsearch.flags.Flags;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
public class GlobalSearchSession extends ReadOnlyGlobalSearchSession implements Closeable {
    private static final String TAG = "AppSearchGlobalSearchSe";

    // Management of observer callbacks. Key is observed package.
    @GuardedBy("mObserverCallbacksLocked")
    private final Map<String, Map<ObserverCallback, IAppSearchObserverProxy>>
            mObserverCallbacksLocked = new ArrayMap<>();

    private boolean mIsMutated = false;
    private boolean mIsClosed = false;

    /**
     * Creates a search session for the client, defined by the {@code userHandle} and {@code
     * packageName}.
     */
    static void createGlobalSearchSession(
            @NonNull IAppSearchManager service,
            @NonNull UserHandle userHandle,
            @NonNull AppSearchAttributionSource attributionSource,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<GlobalSearchSession>> callback) {
        GlobalSearchSession globalSearchSession =
                new GlobalSearchSession(service, userHandle, attributionSource);
        globalSearchSession.initialize(
                executor,
                result -> {
                    if (result.isSuccess()) {
                        callback.accept(AppSearchResult.newSuccessfulResult(globalSearchSession));
                    } else {
                        callback.accept(AppSearchResult.newFailedResult(result));
                    }
                });
    }

    private GlobalSearchSession(
            @NonNull IAppSearchManager service,
            @NonNull UserHandle userHandle,
            @NonNull AppSearchAttributionSource callerAttributionSource) {
        super(service, userHandle, callerAttributionSource, /* isForEnterprise= */ false);
    }

    /**
     * Retrieves {@link GenericDocument} documents, belonging to the specified package name and
     * database name and identified by the namespace and ids in the request, from the {@link
     * GlobalSearchSession} database. When a call is successful, the result will be returned in the
     * successes section of the {@link AppSearchBatchResult} object in the callback. If the package
     * doesn't exist, database doesn't exist, or if the calling package doesn't have access, these
     * failures will be reflected as {@link AppSearchResult} objects with a RESULT_NOT_FOUND status
     * code in the failures section of the {@link AppSearchBatchResult} object.
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
    @Override
    public void getByDocumentId(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull GetByDocumentIdRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull BatchResultCallback<String, GenericDocument> callback) {
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        super.getByDocumentId(packageName, databaseName, request, executor, callback);
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
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        super.openBlobForRead(handles, executor, callback);
    }

    /**
     * Retrieves documents from all AppSearch databases that the querying application has access to.
     *
     * <p>Applications can be granted access to documents by specifying {@link
     * SetSchemaRequest.Builder#setSchemaTypeVisibilityForPackage}, or {@link
     * SetSchemaRequest.Builder#setDocumentClassVisibilityForPackage} when building a schema.
     *
     * <p>Document access can also be granted to system UIs by specifying {@link
     * SetSchemaRequest.Builder#setSchemaTypeDisplayedBySystem}, or {@link
     * SetSchemaRequest.Builder#setDocumentClassDisplayedBySystem} when building a schema.
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
    @Override
    public SearchResults search(@NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return super.search(queryExpression, searchSpec);
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
     * @param callback Callback to receive the pending {@link GetSchemaResponse} containing the
     *     schemas that the caller has access to or an empty GetSchemaResponse if the request
     *     package and database does not exist, has not set a schema or contains no schemas that are
     *     accessible to the caller.
     */
    @Override
    public void getSchema(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<AppSearchResult<GetSchemaResponse>> callback) {
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        super.getSchema(packageName, databaseName, executor, callback);
    }

    /**
     * Reports that a particular document has been used from a system surface.
     *
     * <p>See {@link AppSearchSession#reportUsage} for a general description of document usage, as
     * well as an API that can be used by the app itself.
     *
     * <p>Usage reported via this method is accounted separately from usage reported via {@link
     * AppSearchSession#reportUsage} and may be accessed using the constants {@link
     * SearchSpec#RANKING_STRATEGY_SYSTEM_USAGE_COUNT} and {@link
     * SearchSpec#RANKING_STRATEGY_SYSTEM_USAGE_LAST_USED_TIMESTAMP}.
     *
     * @param request The usage reporting request.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive errors. If the operation succeeds, the callback will be
     *     invoked with an {@link AppSearchResult} whose value is {@code null}. The callback will be
     *     invoked with an {@link AppSearchResult} of {@link AppSearchResult#RESULT_SECURITY_ERROR}
     *     if this API is invoked by an app which is not part of the system.
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
                    new ReportUsageAidlRequest(
                            mCallerAttributionSource,
                            request.getPackageName(),
                            request.getDatabaseName(),
                            new ReportUsageRequest(
                                    request.getNamespace(),
                                    request.getDocumentId(),
                                    request.getUsageTimestampMillis()),
                            /* systemUsage= */ true,
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new AppSearchResultCallback<Void>() {
                        @Override
                        public void onResult(@NonNull AppSearchResult<Void> result) {
                            safeExecute(executor, callback, () -> callback.accept(result));
                        }
                    });
            mIsMutated = true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Adds an {@link ObserverCallback} to monitor changes within the databases owned by {@code
     * targetPackageName} if they match the given {@link
     * android.app.appsearch.observer.ObserverSpec}.
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
     * @param spec Specification of what types of changes to listen for
     * @param executor Executor on which to call the {@code observer} callback methods.
     * @param observer Callback to trigger when a schema or document changes
     * @throws AppSearchException if an error occurs trying to register the observer
     */
    @SuppressWarnings("unchecked")
    public void registerObserverCallback(
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull ObserverCallback observer)
            throws AppSearchException {
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
                stub =
                        new IAppSearchObserverProxy.Stub() {
                            @Override
                            public void onSchemaChanged(
                                    @NonNull String packageName,
                                    @NonNull String databaseName,
                                    @NonNull List<String> changedSchemaNames) {
                                safeExecute(
                                        executor,
                                        this::suppressingErrorCallback,
                                        () -> {
                                            SchemaChangeInfo changeInfo =
                                                    new SchemaChangeInfo(
                                                            packageName,
                                                            databaseName,
                                                            new ArraySet<>(changedSchemaNames));
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
                                safeExecute(
                                        executor,
                                        this::suppressingErrorCallback,
                                        () -> {
                                            DocumentChangeInfo changeInfo =
                                                    new DocumentChangeInfo(
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
                             * <p>If we fail to deliver change notifications, there isn't much we
                             * can do. The API doesn't allow the user to provide a callback to
                             * invoke on failure of change notification delivery. {@link
                             * SearchSessionUtil#safeExecute} already includes a log message. So we
                             * just do nothing.
                             */
                            private void suppressingErrorCallback(
                                    @NonNull AppSearchResult<?> unused) {}
                        };
            }

            // Regardless of whether this stub was fresh or not, we have to register it again
            // because the user might be supplying a different spec.
            AppSearchResultParcel<Void> resultParcel;
            try {
                resultParcel =
                        mService.registerObserverCallback(
                                new RegisterObserverCallbackAidlRequest(
                                        mCallerAttributionSource,
                                        targetPackageName,
                                        spec,
                                        mUserHandle,
                                        /* binderCallStartTimeMillis= */ SystemClock
                                                .elapsedRealtime()),
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
     * <p>All instances of {@link ObserverCallback} which are registered to observe {@code
     * targetPackageName} and compare equal to the provided callback using the provided argument's
     * {@code ObserverCallback#equals} will be removed.
     *
     * <p>If no matching observers have been registered, this method has no effect. If multiple
     * matching observers have been registered, all will be removed.
     *
     * @param targetPackageName Package which the observers to be removed are listening to.
     * @param observer Callback to unregister.
     * @throws AppSearchException if an error occurs trying to remove the observer, such as a
     *     failure to communicate with the system service. Note that no error will be thrown if the
     *     provided observer doesn't match any registered observer.
     */
    @SuppressWarnings("unchecked")
    public void unregisterObserverCallback(
            @NonNull String targetPackageName, @NonNull ObserverCallback observer)
            throws AppSearchException {
        Objects.requireNonNull(targetPackageName);
        Objects.requireNonNull(observer);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");

        IAppSearchObserverProxy stub;
        synchronized (mObserverCallbacksLocked) {
            Map<ObserverCallback, IAppSearchObserverProxy> observersForPackage =
                    mObserverCallbacksLocked.get(targetPackageName);
            if (observersForPackage == null) {
                return; // No observers registered for this package. Nothing to do.
            }
            stub = observersForPackage.get(observer);
            if (stub == null) {
                return; // No such observer registered. Nothing to do.
            }

            AppSearchResultParcel<Void> resultParcel;
            try {
                resultParcel =
                        mService.unregisterObserverCallback(
                                new UnregisterObserverCallbackAidlRequest(
                                        mCallerAttributionSource,
                                        targetPackageName,
                                        mUserHandle,
                                        /* binderCallStartTimeMillis= */ SystemClock
                                                .elapsedRealtime()),
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

    /** Closes the {@link GlobalSearchSession}. */
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
                Log.e(TAG, "Unable to close the GlobalSearchSession", e);
            }
        }
    }
}

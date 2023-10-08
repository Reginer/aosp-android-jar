/**
 * Copyright 2020, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.app.appsearch.aidl;

import android.os.Bundle;
import android.os.UserHandle;

import android.app.appsearch.aidl.AppSearchResultParcel;
import android.app.appsearch.aidl.IAppSearchBatchResultCallback;
import android.app.appsearch.aidl.IAppSearchObserverProxy;
import android.app.appsearch.aidl.IAppSearchResultCallback;
import android.app.appsearch.aidl.DocumentsParcel;
import android.content.AttributionSource;
import android.os.ParcelFileDescriptor;

/** {@hide} */
interface IAppSearchManager {
    /**
     * Creates and initializes AppSearchImpl for the calling app.
     *
     * @param callerAttributionSource The permission identity of the package to initialize for.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback {@link IAppSearchResultCallback#onResult} will be called with an
     *     {@link AppSearchResult}&lt;{@link Void}&gt;.
     */
    void initialize(
        in AttributionSource callerAttributionSource,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchResultCallback callback);

    /**
     * Updates the AppSearch schema for this database.
     *
     * @param callerAttributionSource The permission identity of the package that owns this schema.
     * @param databaseName  The name of the database where this schema lives.
     * @param schemaBundles List of {@link AppSearchSchema} bundles.
     * @param visibilityBundles List of {@link VisibilityDocument} bundles.
     * @param forceOverride Whether to apply the new schema even if it is incompatible. All
     *     incompatible documents will be deleted.
     * @param schemaVersion  The overall schema version number of the request.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param schemaMigrationCallType Indicates how a SetSchema call relative to SchemaMigration
     *     case.
     * @param callback {@link IAppSearchResultCallback#onResult} will be called with an
     *     {@link AppSearchResult}&lt;{@link Bundle}&gt;, where the value are
     *     {@link SetSchemaResponse} bundle.
     */
    void setSchema(
        in AttributionSource callerAttributionSource,
        in String databaseName,
        in List<Bundle> schemaBundles,
        in List<Bundle> visibilityBundles,
        boolean forceOverride,
        in int schemaVersion,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in int schemaMigrationCallType,
        in IAppSearchResultCallback callback);

    /**
     * Retrieves the AppSearch schema for this database.
     *
     * @param callerAttributionSource The permission identity of the package making this call.
     * @param targetPackageName The name of the package that owns the schema.
     * @param databaseName  The name of the database to retrieve.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback {@link IAppSearchResultCallback#onResult} will be called with an
     *     {@link AppSearchResult}&lt;{@link Bundle}&gt; where the bundle is a GetSchemaResponse.
     */
    void getSchema(
        in AttributionSource callerAttributionSource,
        in String targetPackageName,
        in String databaseName,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchResultCallback callback);

    /**
     * Retrieves the set of all namespaces in the current database with at least one document.
     *
     * @param callerAttributionSource The permission identity of the package that owns the schema.
     * @param databaseName  The name of the database to retrieve.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback {@link IAppSearchResultCallback#onResult} will be called with an
     *     {@link AppSearchResult}&lt;{@link List}&lt;{@link String}&gt;&gt;.
     */
    void getNamespaces(
        in AttributionSource callerAttributionSource,
        in String databaseName,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchResultCallback callback);

    /**
     * Inserts documents into the index.
     *
     * @param callerAttributionSource The permission identity of the package that owns this
     *     document.
     * @param databaseName  The name of the database where this document lives.
     * @param documentsParcel Parcelable object contains a list of GenericDocument.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback
     *     If the call fails to start, {@link IAppSearchBatchResultCallback#onSystemError}
     *     will be called with the cause throwable. Otherwise,
     *     {@link IAppSearchBatchResultCallback#onResult} will be called with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link Void}&gt;
     *     where the keys are document IDs, and the values are {@code null}.
     */
    void putDocuments(
        in AttributionSource callerAttributionSource,
        in String databaseName,
        in DocumentsParcel documentsParcel,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchBatchResultCallback callback);

    /**
     * Retrieves documents from the index.
     *
     * @param callerAttributionSource The permission identity of the package that is getting this
     *     document.
     * @param targetPackageName The name of the package that owns this document.
     * @param databaseName  The databaseName this document resides in.
     * @param namespace    The namespace this document resides in.
     * @param ids The IDs of the documents to retrieve.
     * @param typePropertyPaths A map of schema type to a list of property paths to return in the
     *     result.
     * @param userHandle Handle of the calling user.
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis.
     * @param callback
     *     If the call fails to start, {@link IAppSearchBatchResultCallback#onSystemError}
     *     will be called with the cause throwable. Otherwise,
     *     {@link IAppSearchBatchResultCallback#onResult} will be called with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link Bundle}&gt;
     *     where the keys are document IDs, and the values are Document bundles.
     */
    void getDocuments(
        in AttributionSource callerAttributionSource,
        in String targetPackageName,
        in String databaseName,
        in String namespace,
        in List<String> ids,
        in Map<String, List<String>> typePropertyPaths,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchBatchResultCallback callback);

    /**
     * Searches a document based on a given specifications.
     *
     * @param callerAttributionSource The permission identity of the package to query over.
     * @param databaseName The databaseName this query for.
     * @param queryExpression String to search for
     * @param searchSpecBundle SearchSpec bundle
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback {@link AppSearchResult}&lt;{@link Bundle}&gt; of performing this
     *         operation.
     */
    void query(
        in AttributionSource callerAttributionSource,
        in String databaseName,
        in String queryExpression,
        in Bundle searchSpecBundle,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchResultCallback callback);

    /**
     * Executes a global query, i.e. over all permitted databases, against the AppSearch index and
     * returns results.
     *
     * @param callerAttributionSource The permission identity of the package making the query.
     * @param queryExpression String to search for
     * @param searchSpecBundle SearchSpec bundle
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback {@link AppSearchResult}&lt;{@link Bundle}&gt; of performing this
     *         operation.
     */
    void globalQuery(
        in AttributionSource callerAttributionSource,
        in String queryExpression,
        in Bundle searchSpecBundle,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchResultCallback callback);

    /**
     * Fetches the next page of results of a previously executed query. Results can be empty if
     * next-page token is invalid or all pages have been returned.
     *
     * @param callerAttributionSource The permission identity of the package to persist to disk
     *     for.
     * @param databaseName The nullable databaseName this query for. The databaseName will be null
                           if the query is a global search.
     * @param nextPageToken The token of pre-loaded results of previously executed query.
     * @param joinType the type of join performed. 0 if no join is performed
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback {@link AppSearchResult}&lt;{@link Bundle}&gt; of performing this
     *                  operation.
     */
    void getNextPage(
        in AttributionSource callerAttributionSource,
        in String databaseName,
        in long nextPageToken,
        in int joinType,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchResultCallback callback);

    /**
     * Invalidates the next-page token so that no more results of the related query can be returned.
     *
     * @param callerAttributionSource The permission identity of the package to persist to disk
     *     for.
     * @param nextPageToken The token of pre-loaded results of previously executed query to be
     *                      Invalidated.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     */
    void invalidateNextPageToken(
        in AttributionSource callerAttributionSource,
        in long nextPageToken,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis);

    /**
    * Searches a document based on a given specifications.
    *
    * <p>Documents will be save to the given ParcelFileDescriptor
    *
    * @param callerAttributionSource The permission identity of the package to query over.
    * @param databaseName The databaseName this query for.
    * @param fileDescriptor The ParcelFileDescriptor where documents should be written to.
    * @param queryExpression String to search for.
    * @param searchSpecBundle SearchSpec bundle.
    * @param userHandle Handle of the calling user.
    * @param binderCallStartTimeMillis start timestamp of binder call in Millis
    * @param callback {@link IAppSearchResultCallback#onResult} will be called with an
    *        {@link AppSearchResult}&lt;{@code Void}&gt;.
    */
    void writeQueryResultsToFile(
        in AttributionSource callerAttributionSource,
        in String databaseName,
        in ParcelFileDescriptor fileDescriptor,
        in String queryExpression,
        in Bundle searchSpecBundle,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchResultCallback callback);

    /**
    * Inserts documents from the given file into the index.
    *
    * <p>This method does not dispatch change notifications for the individual documents being
    * inserted, so it is only appropriate to use for batch upload situations where a broader change
    * notification will indicate what has changed, like schema migration.
    *
    * @param callerAttributionSource The permission identity of the package that owns this
    *     document.
    * @param databaseName  The name of the database where this document lives.
    * @param fileDescriptor The ParcelFileDescriptor where documents should be read from.
    * @param userHandle Handle of the calling user.
    * @param schemaMigrationStatsBundle the Bundle contains SchemaMigrationStats information.
    * @param totalLatencyStartTimeMillis start timestamp to calculate total migration latency in
    *     Millis
    * @param binderCallStartTimeMillis start timestamp of binder call in Millis
    * @param callback {@link IAppSearchResultCallback#onResult} will be called with an
    *     {@link AppSearchResult}&lt;{@link List}&lt;{@link Bundle}&gt;&gt;, where the value are
    *     MigrationFailure bundles.
    */
    void putDocumentsFromFile(
        in AttributionSource callerAttributionSource,
        in String databaseName,
        in ParcelFileDescriptor fileDescriptor,
        in UserHandle userHandle,
        in Bundle schemaMigrationStatsBundle,
        in long totalLatencyStartTimeMillis,
        in long binderCallStartTimeMillis,
        in IAppSearchResultCallback callback);

    /**
     * Retrieves suggested Strings that could be used as {@code queryExpression} in search API.
     *
     * @param callerAttributionSource The permission identity of the package to suggest over.
     * @param databaseName The databaseName this suggest is for.
     * @param suggestionQueryExpression the non empty query string to search suggestions
     * @param searchSuggestionSpecBundle SearchSuggestionSpec bundle
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback {@link AppSearchResult}&lt;List&lt;{@link Bundle}&gt; of performing this
     *   operation. List contains SearchSuggestionResult bundles.
     */
    void searchSuggestion(
            in AttributionSource callerAttributionSource,
            in String databaseName,
            in String suggestionQueryExpression,
            in Bundle searchSuggestionSpecBundle,
            in UserHandle userHandle,
            in long binderCallStartTimeMillis,
            in IAppSearchResultCallback callback);

    /**
     * Reports usage of a particular document by namespace and id.
     *
     * <p>A usage report represents an event in which a user interacted with or viewed a document.
     *
     * <p>For each call to {@link #reportUsage}, AppSearch updates usage count and usage recency
     * metrics for that particular document. These metrics are used for ordering {@link #query}
     * results by the {@link SearchSpec#RANKING_STRATEGY_USAGE_COUNT} and
     * {@link SearchSpec#RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP} ranking strategies.
     *
     * <p>Reporting usage of a document is optional.
     *
     * @param callerAttributionSource The permission identity of the package that owns this
     *     document.
     * @param targetPackageName The name of the package that owns this document.
     * @param databaseName  The name of the database to report usage against.
     * @param namespace Namespace the document being used belongs to.
     * @param id ID of the document being used.
     * @param usageTimestampMillis The timestamp at which the document was used.
     * @param systemUsage Whether the usage was reported by a system app against another app's doc.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback {@link IAppSearchResultCallback#onResult} will be called with an
     *     {@link AppSearchResult}&lt;{@link Void}&gt;.
     */
    void reportUsage(
        in AttributionSource callerAttributionSource,
        in String targetPackageName,
        in String databaseName,
        in String namespace,
        in String id,
        in long usageTimestampMillis,
        in boolean systemUsage,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchResultCallback callback);

    /**
     * Removes documents by ID.
     *
     * @param callerAttributionSource The permission identity of the package the document is in.
     * @param databaseName The databaseName the document is in.
     * @param namespace    Namespace of the document to remove.
     * @param ids The IDs of the documents to delete
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback
     *     If the call fails to start, {@link IAppSearchBatchResultCallback#onSystemError}
     *     will be called with the cause throwable. Otherwise,
     *     {@link IAppSearchBatchResultCallback#onResult} will be called with an
     *     {@link AppSearchBatchResult}&lt;{@link String}, {@link Void}&gt;
     *     where the keys are document IDs. If a document doesn't exist, it will be reported as a
     *     failure where the {@code throwable} is {@code null}.
     */
    void removeByDocumentId(
        in AttributionSource callerAttributionSource,
        in String databaseName,
        in String namespace,
        in List<String> ids,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchBatchResultCallback callback);

    /**
     * Removes documents by given query.
     *
     * @param callerAttributionSource The permission identity of the package to query over.
     * @param databaseName The databaseName this query for.
     * @param queryExpression String to search for
     * @param searchSpecBundle SearchSpec bundle
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback {@link IAppSearchResultCallback#onResult} will be called with an
     *     {@link AppSearchResult}&lt;{@link Void}&gt;.
     */
    void removeByQuery(
        in AttributionSource callerAttributionSource,
        in String databaseName,
        in String queryExpression,
        in Bundle searchSpecBundle,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchResultCallback callback);

    /**
     * Gets the storage info.
     *
     * @param callerAttributionSource The permission identity of the package to get the storage
     *     info for.
     * @param databaseName The databaseName to get the storage info for.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param callback {@link IAppSearchResultCallback#onResult} will be called with an
     *     {@link AppSearchResult}&lt;{@link Bundle}&gt;, where the value is a
     *     {@link StorageInfo}.
     */
    void getStorageInfo(
        in AttributionSource callerAttributionSource,
        in String databaseName,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchResultCallback callback);

    /**
     * Persists all update/delete requests to the disk.
     *
     * @param callerAttributionSource The permission identity of the package to persist to disk for
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     */
    void persistToDisk(
        in AttributionSource callerAttributionSource,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis);

    /**
     * Adds an observer to monitor changes within the databases owned by {@code observedPackage} if
     * they match the given ObserverSpec.
     *
     * @param callerAttributionSource The permission identity of the package which is registering
     *     an observer.
     * @param targetPackageName Package whose changes to monitor
     * @param observerSpecBundle Bundle of ObserverSpec showing what types of changes to listen for
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param observerProxy Callback to trigger when a schema or document changes
     * @return the success or failure of this operation
     */
    AppSearchResultParcel registerObserverCallback(
        in AttributionSource callerAttributionSource,
        in String targetPackageName,
        in Bundle observerSpecBundle,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchObserverProxy observerProxy);

    /**
     * Removes previously registered {@link ObserverCallback} instances from the system.
     *
     * @param callerAttributionSource The permission identity of the package that owns the observer
     * @param observedPackage Package whose changes are being monitored
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param observerProxy Observer callback to remove
     * @return the success or failure of this operation
     */
    AppSearchResultParcel unregisterObserverCallback(
        in AttributionSource callerAttributionSource,
        in String observedPackage,
        in UserHandle userHandle,
        in long binderCallStartTimeMillis,
        in IAppSearchObserverProxy observerProxy);
}

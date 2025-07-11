/*
 * Copyright 2021 The Android Open Source Project
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

package android.app.appsearch.observer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Contains information about an individual change detected by an {@link ObserverCallback}.
 *
 * <p>This class reports information about document changes, that is, when documents were added,
 * updated or removed.
 *
 * <p>Changes are grouped by package, database, schema type and namespace. Each unique combination
 * of these items will generate a unique {@link DocumentChangeInfo}.
 *
 * <p>Notifications are only sent for documents whose schema type matches an observer's schema
 * filters (as determined by {@link ObserverSpec#getFilterSchemas}).
 *
 * <p>Note that document changes that happen during schema migration from calling {@link
 * android.app.appsearch.AppSearchSession#setSchema} are not reported via this class. Such changes
 * are reported through {@link SchemaChangeInfo}.
 */
public final class DocumentChangeInfo {
    private final String mPackageName;
    private final String mDatabase;
    private final String mNamespace;
    private final String mSchemaName;
    private final Set<String> mChangedDocumentIds;

    /**
     * Constructs a new {@link DocumentChangeInfo}.
     *
     * @param packageName The package name of the app which owns the documents that changed.
     * @param database The database in which the documents that changed reside.
     * @param namespace The namespace in which the documents that changed reside.
     * @param schemaName The name of the schema type that contains the changed documents.
     * @param changedDocumentIds The set of document IDs that have been changed as part of this
     *     notification.
     */
    public DocumentChangeInfo(
            @NonNull String packageName,
            @NonNull String database,
            @NonNull String namespace,
            @NonNull String schemaName,
            @NonNull Set<String> changedDocumentIds) {
        mPackageName = Objects.requireNonNull(packageName);
        mDatabase = Objects.requireNonNull(database);
        mNamespace = Objects.requireNonNull(namespace);
        mSchemaName = Objects.requireNonNull(schemaName);
        mChangedDocumentIds =
                Collections.unmodifiableSet(Objects.requireNonNull(changedDocumentIds));
    }

    /** Returns the package name of the app which owns the documents that changed. */
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    /** Returns the database in which the documents that was changed reside. */
    public @NonNull String getDatabaseName() {
        return mDatabase;
    }

    /** Returns the namespace of the documents that changed. */
    public @NonNull String getNamespace() {
        return mNamespace;
    }

    /** Returns the name of the schema type that contains the changed documents. */
    public @NonNull String getSchemaName() {
        return mSchemaName;
    }

    /**
     * Returns the set of document IDs that have been changed as part of this notification.
     *
     * <p>This will never be empty.
     */
    public @NonNull Set<String> getChangedDocumentIds() {
        return mChangedDocumentIds;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DocumentChangeInfo)) {
            return false;
        }

        DocumentChangeInfo that = (DocumentChangeInfo) o;
        return mPackageName.equals(that.mPackageName)
                && mDatabase.equals(that.mDatabase)
                && mNamespace.equals(that.mNamespace)
                && mSchemaName.equals(that.mSchemaName)
                && mChangedDocumentIds.equals(that.mChangedDocumentIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPackageName, mDatabase, mNamespace, mSchemaName, mChangedDocumentIds);
    }

    @Override
    public @NonNull String toString() {
        return "DocumentChangeInfo{"
                + "packageName='"
                + mPackageName
                + '\''
                + ", database='"
                + mDatabase
                + '\''
                + ", namespace='"
                + mNamespace
                + '\''
                + ", schemaName='"
                + mSchemaName
                + '\''
                + ", changedDocumentIds='"
                + mChangedDocumentIds
                + '\''
                + '}';
    }
}

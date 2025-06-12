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

package android.app.appsearch;

import static android.app.appsearch.AppSearchResult.RESULT_INVALID_SCHEMA;
import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;
import static android.os.ParcelFileDescriptor.MODE_WRITE_ONLY;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.WorkerThread;
import android.app.appsearch.SetSchemaResponse.MigrationFailure;
import android.app.appsearch.aidl.AppSearchAttributionSource;
import android.app.appsearch.aidl.AppSearchResultCallback;
import android.app.appsearch.aidl.IAppSearchManager;
import android.app.appsearch.aidl.PutDocumentsFromFileAidlRequest;
import android.app.appsearch.aidl.WriteSearchResultsToFileAidlRequest;
import android.app.appsearch.exceptions.AppSearchException;
import android.app.appsearch.safeparcel.GenericDocumentParcel;
import android.app.appsearch.stats.SchemaMigrationStats;
import android.app.appsearch.util.ExceptionUtil;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArraySet;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The helper class for {@link AppSearchSchema} migration.
 *
 * <p>It will query and migrate {@link GenericDocument} in given type to a new version.
 * Application-specific cache directory is used to store the temporary files created during
 * migration.
 *
 * @hide
 */
public class AppSearchMigrationHelper implements Closeable {
    private final IAppSearchManager mService;
    private final AppSearchAttributionSource mCallerAttributionSource;
    private final String mDatabaseName;
    private final UserHandle mUserHandle;
    @Nullable private final File mTempDirectoryForSchemaMigration;
    private final File mMigratedFile;
    private final Set<String> mDestinationTypes;
    private int mTotalNeedMigratedDocumentCount = 0;

    /**
     * Initializes an AppSearchMigrationHelper instance.
     *
     * @param service The {@link IAppSearchManager} service from which to make api calls.
     * @param userHandle The user for which the session should be created.
     * @param callerAttributionSource The attribution source containing the caller's package name
     *     and uid
     * @param databaseName The name of the database where this schema lives.
     * @param newSchemas The set of new schemas to update existing schemas.
     * @param tempDirectoryForSchemaMigration The directory to create temporary files needed for
     *     migration. If this is null, the default temporary-file directory (/data/local/tmp) will
     *     be used.
     * @throws IOException on failure to create a temporary file.
     */
    AppSearchMigrationHelper(
            @NonNull IAppSearchManager service,
            @NonNull UserHandle userHandle,
            @NonNull AppSearchAttributionSource callerAttributionSource,
            @NonNull String databaseName,
            @NonNull Set<AppSearchSchema> newSchemas,
            @Nullable File tempDirectoryForSchemaMigration)
            throws IOException {
        mService = Objects.requireNonNull(service);
        mUserHandle = Objects.requireNonNull(userHandle);
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mDatabaseName = Objects.requireNonNull(databaseName);
        mTempDirectoryForSchemaMigration = tempDirectoryForSchemaMigration;
        mMigratedFile =
                File.createTempFile(
                        /* prefix= */ "appsearch",
                        /* suffix= */ null,
                        mTempDirectoryForSchemaMigration);
        mDestinationTypes = new ArraySet<>(newSchemas.size());
        for (AppSearchSchema newSchema : newSchemas) {
            mDestinationTypes.add(newSchema.getSchemaType());
        }
    }

    /**
     * Queries all documents that need to be migrated to a different version and transform documents
     * to that version by passing them to the provided {@link Migrator}.
     *
     * <p>The method will be executed on the executor provided to {@link
     * AppSearchSession#setSchema}.
     *
     * @param schemaType The schema type that needs to be updated and whose {@link GenericDocument}
     *     need to be migrated.
     * @param migrator The {@link Migrator} that will upgrade or downgrade a {@link GenericDocument}
     *     to new version.
     * @param schemaMigrationStatsBuilder The {@link SchemaMigrationStats.Builder} contains schema
     *     migration stats information
     */
    @WorkerThread
    void queryAndTransform(
            @NonNull String schemaType,
            @NonNull Migrator migrator,
            int currentVersion,
            int finalVersion,
            @Nullable SchemaMigrationStats.Builder schemaMigrationStatsBuilder)
            throws IOException, AppSearchException, InterruptedException, ExecutionException {
        File queryFile =
                File.createTempFile(
                        /* prefix= */ "appsearch",
                        /* suffix= */ null,
                        mTempDirectoryForSchemaMigration);
        try (ParcelFileDescriptor fileDescriptor =
                ParcelFileDescriptor.open(queryFile, MODE_WRITE_ONLY)) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<AppSearchResult<Void>> resultReference = new AtomicReference<>();
            mService.writeSearchResultsToFile(
                    new WriteSearchResultsToFileAidlRequest(
                            mCallerAttributionSource,
                            mDatabaseName,
                            fileDescriptor,
                            /* searchExpression= */ "",
                            new SearchSpec.Builder()
                                    .addFilterSchemas(schemaType)
                                    .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                    .build(),
                            mUserHandle,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new AppSearchResultCallback<Void>() {
                        @Override
                        public void onResult(@NonNull AppSearchResult<Void> result) {
                            resultReference.set(result);
                            latch.countDown();
                        }
                    });
            latch.await();
            AppSearchResult<Void> result = resultReference.get();
            if (!result.isSuccess()) {
                throw new AppSearchException(result.getResultCode(), result.getErrorMessage());
            }
            readAndTransform(
                    queryFile, migrator, currentVersion, finalVersion, schemaMigrationStatsBuilder);
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        } finally {
            queryFile.delete();
        }
    }

    /**
     * Puts all {@link GenericDocument} migrated from the previous call to {@link
     * #queryAndTransform} into AppSearch.
     *
     * <p>This method should be only called once.
     *
     * @param responseBuilder a SetSchemaResponse builder whose result will be returned by this
     *     function with any {@link android.app.appsearch.SetSchemaResponse.MigrationFailure} added
     *     in.
     * @param schemaMigrationStatsBuilder The {@link SchemaMigrationStats.Builder} contains schema
     *     migration stats information
     * @param totalLatencyStartTimeMillis start timestamp to calculate total migration latency in
     *     Millis
     * @return the {@link SetSchemaResponse} for {@link AppSearchSession#setSchema} call.
     */
    @NonNull
    AppSearchResult<SetSchemaResponse> putMigratedDocuments(
            @NonNull SetSchemaResponse.Builder responseBuilder,
            @NonNull SchemaMigrationStats.Builder schemaMigrationStatsBuilder,
            long totalLatencyStartTimeMillis) {
        if (mTotalNeedMigratedDocumentCount == 0) {
            return AppSearchResult.newSuccessfulResult(responseBuilder.build());
        }
        try (ParcelFileDescriptor fileDescriptor =
                ParcelFileDescriptor.open(mMigratedFile, MODE_READ_ONLY)) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<AppSearchResult<List<MigrationFailure>>> resultReference =
                    new AtomicReference<>();
            mService.putDocumentsFromFile(
                    new PutDocumentsFromFileAidlRequest(
                            mCallerAttributionSource,
                            mDatabaseName,
                            fileDescriptor,
                            mUserHandle,
                            schemaMigrationStatsBuilder.build(),
                            totalLatencyStartTimeMillis,
                            /* binderCallStartTimeMillis= */ SystemClock.elapsedRealtime()),
                    new AppSearchResultCallback<List<MigrationFailure>>() {
                        @Override
                        public void onResult(
                                @NonNull AppSearchResult<List<MigrationFailure>> result) {
                            resultReference.set(result);
                            latch.countDown();
                        }
                    });
            latch.await();
            AppSearchResult<List<MigrationFailure>> result = resultReference.get();
            if (!result.isSuccess()) {
                return AppSearchResult.newFailedResult(result);
            }
            List<MigrationFailure> migrationFailures =
                    Objects.requireNonNull(result.getResultValue());
            responseBuilder.addMigrationFailures(migrationFailures);
        } catch (RemoteException e) {
            ExceptionUtil.handleRemoteException(e);
        } catch (InterruptedException | IOException | RuntimeException e) {
            return AppSearchResult.throwableToFailedResult(e);
        } finally {
            mMigratedFile.delete();
        }
        return AppSearchResult.newSuccessfulResult(responseBuilder.build());
    }

    /**
     * Reads all saved {@link GenericDocument}s from the given {@link File}.
     *
     * <p>Transforms those {@link GenericDocument}s to the final version.
     *
     * <p>Save migrated {@link GenericDocument}s to the {@link #mMigratedFile}.
     */
    private void readAndTransform(
            @NonNull File file,
            @NonNull Migrator migrator,
            int currentVersion,
            int finalVersion,
            @Nullable SchemaMigrationStats.Builder schemaMigrationStatsBuilder)
            throws IOException, AppSearchException {
        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(file));
                DataOutputStream outputStream =
                        new DataOutputStream(
                                new FileOutputStream(mMigratedFile, /* append= */ true))) {
            GenericDocument document;
            while (true) {
                try {
                    document = readDocumentFromInputStream(inputStream);
                } catch (EOFException e) {
                    break;
                    // Nothing wrong. We just finished reading.
                }

                GenericDocument newDocument;
                if (currentVersion < finalVersion) {
                    newDocument = migrator.onUpgrade(currentVersion, finalVersion, document);
                } else {
                    // currentVersion == finalVersion case won't trigger migration and get here.
                    newDocument = migrator.onDowngrade(currentVersion, finalVersion, document);
                }
                ++mTotalNeedMigratedDocumentCount;

                if (!mDestinationTypes.contains(newDocument.getSchemaType())) {
                    // we exit before the new schema has been set to AppSearch. So no
                    // observable changes will be applied to stored schemas and documents.
                    // And the temp file will be deleted at close(), which will be triggered at
                    // the end of try-with-resources block of SearchSessionImpl.
                    throw new AppSearchException(
                            RESULT_INVALID_SCHEMA,
                            "Receive a migrated document with schema type: "
                                    + newDocument.getSchemaType()
                                    + ". But the schema types doesn't exist in the request");
                }
                writeDocumentToOutputStream(outputStream, newDocument);
            }
        }
        if (schemaMigrationStatsBuilder != null) {
            schemaMigrationStatsBuilder.setTotalNeedMigratedDocumentCount(
                    mTotalNeedMigratedDocumentCount);
        }
    }

    /**
     * Reads a {@link GenericDocument} from given {@link DataInputStream}.
     *
     * @param inputStream The inputStream to read from
     * @throws IOException on read failure.
     * @throws EOFException if {@link java.io.InputStream} reaches the end.
     */
    @NonNull
    public static GenericDocument readDocumentFromInputStream(@NonNull DataInputStream inputStream)
            throws IOException {
        int length = inputStream.readInt();
        if (length == 0) {
            throw new EOFException();
        }
        byte[] serializedMessage = new byte[length];
        inputStream.read(serializedMessage);

        Parcel parcel = Parcel.obtain();
        try {
            parcel.unmarshall(serializedMessage, 0, serializedMessage.length);
            parcel.setDataPosition(0);
            GenericDocumentParcel genericDocumentParcel =
                    GenericDocumentParcel.CREATOR.createFromParcel(parcel);
            return new GenericDocument(genericDocumentParcel);
        } finally {
            parcel.recycle();
        }
    }

    /** Serializes a {@link GenericDocument} and writes into the given {@link DataOutputStream}. */
    public static void writeDocumentToOutputStream(
            @NonNull DataOutputStream outputStream, @NonNull GenericDocument document)
            throws IOException {
        GenericDocumentParcel documentParcel = document.getDocumentParcel();
        Parcel parcel = Parcel.obtain();
        try {
            documentParcel.writeToParcel(parcel, /* flags= */ 0);
            byte[] serializedMessage = parcel.marshall();
            outputStream.writeInt(serializedMessage.length);
            outputStream.write(serializedMessage);
        } finally {
            parcel.recycle();
        }
    }

    @Override
    public void close() throws IOException {
        mMigratedFile.delete();
    }
}

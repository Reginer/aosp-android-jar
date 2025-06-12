/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.app.appsearch.aidl.AppSearchBatchResultParcel;
import android.app.appsearch.aidl.AppSearchResultParcel;
import android.app.appsearch.aidl.IAppSearchBatchResultCallback;
import android.app.appsearch.exceptions.AppSearchException;
import android.app.appsearch.safeparcel.GenericDocumentParcel;
import android.util.Log;

import com.android.internal.util.Preconditions;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Contains util methods used in both {@link GlobalSearchSession} and {@link AppSearchSession}.
 *
 * @hide
 */
public class SearchSessionUtil {
    private static final String TAG = "AppSearchSessionUtil";

    /** Constructor for in case we create an instance */
    private SearchSessionUtil() {}

    /**
     * Calls {@link BatchResultCallback#onSystemError} with a throwable derived from the given
     * failed {@link AppSearchResult}.
     *
     * <p>The {@link AppSearchResult} generally comes from {@link
     * IAppSearchBatchResultCallback#onSystemError}.
     *
     * <p>This method should be called from the callback executor thread.
     *
     * @param failedResult the error
     * @param callback the callback to send the error to
     */
    public static void sendSystemErrorToCallback(
            @NonNull AppSearchResult<?> failedResult, @NonNull BatchResultCallback<?, ?> callback) {
        Preconditions.checkArgument(!failedResult.isSuccess());
        Throwable throwable =
                new AppSearchException(
                        failedResult.getResultCode(), failedResult.getErrorMessage());
        callback.onSystemError(throwable);
    }

    /**
     * Safely executes the given lambda on the given executor.
     *
     * <p>The {@link Executor#execute} call is wrapped in a try/catch. This prevents situations like
     * the executor being shut down or the lambda throwing an exception on a direct executor from
     * crashing the app.
     *
     * <p>If execution fails for the above reasons, a failure notification is delivered to
     * errorCallback synchronously on the calling thread.
     *
     * @param executor The executor on which to safely execute the lambda
     * @param errorCallback The callback to trigger with a failed {@link AppSearchResult} if the
     *     {@link Executor#execute} call fails.
     * @param runnable The lambda to execute on the executor
     */
    public static <T> void safeExecute(
            @NonNull Executor executor,
            @NonNull Consumer<AppSearchResult<T>> errorCallback,
            @NonNull Runnable runnable) {
        try {
            executor.execute(runnable);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to schedule runnable", e);
            errorCallback.accept(AppSearchResult.throwableToFailedResult(e));
        }
    }

    /**
     * Safely executes the given lambda on the given executor.
     *
     * <p>The {@link Executor#execute} call is wrapped in a try/catch. This prevents situations like
     * the executor being shut down or the lambda throwing an exception on a direct executor from
     * crashing the app.
     *
     * <p>If execution fails for the above reasons, a failure notification is delivered to
     * errorCallback synchronously on the calling thread.
     *
     * @param executor The executor on which to safely execute the lambda
     * @param errorCallback The callback to trigger with a failed {@link AppSearchResult} if the
     *     {@link Executor#execute} call fails.
     * @param runnable The lambda to execute on the executor
     */
    public static void safeExecute(
            @NonNull Executor executor,
            @NonNull BatchResultCallback<?, ?> errorCallback,
            @NonNull Runnable runnable) {
        try {
            executor.execute(runnable);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to schedule runnable", e);
            errorCallback.onSystemError(e);
        }
    }

    /**
     * Handler for asynchronous getDocuments method
     *
     * @param executor executor to run the callback
     * @param callback the next method that uses the {@link GenericDocument}
     * @return A callback to be executed once an {@link AppSearchBatchResultParcel} is received
     */
    public static IAppSearchBatchResultCallback createGetDocumentCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull BatchResultCallback<String, GenericDocument> callback) {
        return new IAppSearchBatchResultCallback.Stub() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public void onResult(AppSearchBatchResultParcel resultParcel) {
                safeExecute(
                        executor,
                        callback,
                        () -> {
                            AppSearchBatchResult<String, GenericDocumentParcel> result =
                                    resultParcel.getResult();
                            AppSearchBatchResult.Builder<String, GenericDocument>
                                    documentResultBuilder = new AppSearchBatchResult.Builder<>();

                            for (Map.Entry<String, GenericDocumentParcel> entry :
                                    result.getSuccesses().entrySet()) {
                                GenericDocument document;
                                try {
                                    GenericDocumentParcel genericDocumentParcel = entry.getValue();
                                    if (genericDocumentParcel == null) {
                                        documentResultBuilder.setFailure(
                                                entry.getKey(),
                                                AppSearchResult.RESULT_INTERNAL_ERROR,
                                                "Received null GenericDocumentParcel in"
                                                        + " getByDocumentId API");
                                        continue;
                                    }
                                    document = new GenericDocument(genericDocumentParcel);
                                } catch (RuntimeException e) {
                                    documentResultBuilder.setFailure(
                                            entry.getKey(),
                                            AppSearchResult.RESULT_INTERNAL_ERROR,
                                            e.getMessage());
                                    continue;
                                }
                                documentResultBuilder.setSuccess(entry.getKey(), document);
                            }

                            for (Entry<String, AppSearchResult<GenericDocumentParcel>> entry :
                                    result.getFailures().entrySet()) {
                                documentResultBuilder.setFailure(
                                        entry.getKey(),
                                        entry.getValue().getResultCode(),
                                        entry.getValue().getErrorMessage());
                            }
                            callback.onResult(documentResultBuilder.build());
                        });
            }

            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public void onSystemError(AppSearchResultParcel result) {
                safeExecute(
                        executor,
                        callback,
                        () -> sendSystemErrorToCallback(result.getResult(), callback));
            }
        };
    }
}

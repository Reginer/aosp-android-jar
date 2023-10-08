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
import android.os.Bundle;
import android.util.Log;

import com.android.internal.util.Preconditions;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * @hide
 * Contains util methods used in both {@link GlobalSearchSession} and {@link AppSearchSession}.
 */
public class SearchSessionUtil {
    private static final String TAG = "AppSearchSessionUtil";

    /**
     * Constructor for in case we create an instance
     */
    private SearchSessionUtil() {}

    /**
     * Calls {@link BatchResultCallback#onSystemError} with a throwable derived from the given
     * failed {@link AppSearchResult}.
     *
     * <p>The {@link AppSearchResult} generally comes from
     * {@link IAppSearchBatchResultCallback#onSystemError}.
     *
     * <p>This method should be called from the callback executor thread.
     *
     * @param failedResult the error
     * @param callback the callback to send the error to
     */
    public static void sendSystemErrorToCallback(
            @NonNull AppSearchResult<?> failedResult, @NonNull BatchResultCallback<?, ?> callback) {
        Preconditions.checkArgument(!failedResult.isSuccess());
        Throwable throwable = new AppSearchException(
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
     * @param errorCallback The callback to trigger with a failed {@link AppSearchResult} if
     *                      the {@link Executor#execute} call fails.
     * @param runnable The lambda to execute on the executor
     */
    public static <T> void safeExecute(
            @NonNull Executor executor,
            @NonNull Consumer<AppSearchResult<T>> errorCallback,
            @NonNull Runnable runnable) {
        try {
            executor.execute(runnable);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to schedule runnable", t);
            errorCallback.accept(AppSearchResult.throwableToFailedResult(t));
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
     * @param errorCallback The callback to trigger with a failed {@link AppSearchResult} if
     *                      the {@link Executor#execute} call fails.
     * @param runnable The lambda to execute on the executor
     */
    public static void safeExecute(
            @NonNull Executor executor,
            @NonNull BatchResultCallback<?, ?> errorCallback,
            @NonNull Runnable runnable) {
        try {
            executor.execute(runnable);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to schedule runnable", t);
            errorCallback.onSystemError(t);
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
            public void onResult(AppSearchBatchResultParcel resultParcel) {
                safeExecute(executor, callback, () -> {
                    AppSearchBatchResult<String, Bundle> result =
                            resultParcel.getResult();
                    AppSearchBatchResult.Builder<String, GenericDocument>
                            documentResultBuilder =
                            new AppSearchBatchResult.Builder<>();

                    for (Map.Entry<String, Bundle> bundleEntry :
                            result.getSuccesses().entrySet()) {
                        GenericDocument document;
                        try {
                            document = new GenericDocument(bundleEntry.getValue());
                        } catch (Throwable t) {
                            documentResultBuilder.setFailure(
                                    bundleEntry.getKey(),
                                    AppSearchResult.RESULT_INTERNAL_ERROR,
                                    t.getMessage());
                            continue;
                        }
                        documentResultBuilder.setSuccess(
                                bundleEntry.getKey(), document);
                    }

                    for (Map.Entry<String, AppSearchResult<Bundle>> bundleEntry :
                            ((Map<String, AppSearchResult<Bundle>>)
                                    result.getFailures()).entrySet()) {
                        documentResultBuilder.setFailure(
                                bundleEntry.getKey(),
                                bundleEntry.getValue().getResultCode(),
                                bundleEntry.getValue().getErrorMessage());
                    }
                    callback.onResult(documentResultBuilder.build());

                });
            }

            @Override
            public void onSystemError(AppSearchResultParcel result) {
                safeExecute(
                        executor, callback,
                        () -> sendSystemErrorToCallback(result.getResult(), callback));
            }
        };
    }
}

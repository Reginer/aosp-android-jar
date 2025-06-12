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

package android.adservices.ondevicepersonalization;

import android.adservices.ondevicepersonalization.aidl.IDataAccessService;
import android.adservices.ondevicepersonalization.aidl.IDataAccessServiceCallback;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.WorkerThread;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.RemoteException;

import com.android.ondevicepersonalization.internal.util.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Generates event tracking URLs for a request. The service can embed these URLs within the
 * HTML output as needed. When the HTML is rendered within an ODP WebView, ODP will intercept
 * requests to these URLs, call
 * {@code IsolatedWorker#onEvent(EventInput, android.os.OutcomeReceiver)}, and log the returned
 * output in the EVENTS table.
 *
 */
public class EventUrlProvider {
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getLogger();
    private static final String TAG = EventUrlProvider.class.getSimpleName();
    private static final long ASYNC_TIMEOUT_MS = 1000;

    @NonNull private final IDataAccessService mDataAccessService;

    /** @hide */
    public EventUrlProvider(@NonNull IDataAccessService binder) {
        mDataAccessService = Objects.requireNonNull(binder);
    }

    /**
     * Creates an event tracking URL that returns the provided response. Returns HTTP Status
     * 200 (OK) if the response data is not empty. Returns HTTP Status 204 (No Content) if the
     * response data is empty.
     *
     * @param eventParams The data to be passed to
     *     {@code IsolatedWorker#onEvent(EventInput, android.os.OutcomeReceiver)}
     *     when the event occurs.
     * @param responseData The content to be returned to the WebView when the URL is fetched.
     * @param mimeType The Mime Type of the URL response.
     * @return An ODP event URL that can be inserted into a WebView.
     */
    @WorkerThread
    @NonNull public Uri createEventTrackingUrlWithResponse(
            @NonNull PersistableBundle eventParams,
            @Nullable byte[] responseData,
            @Nullable String mimeType) {
        final long startTimeMillis = System.currentTimeMillis();
        Bundle params = new Bundle();
        params.putParcelable(Constants.EXTRA_EVENT_PARAMS, eventParams);
        params.putByteArray(Constants.EXTRA_RESPONSE_DATA, responseData);
        params.putString(Constants.EXTRA_MIME_TYPE, mimeType);
        return getUrl(params, Constants.API_NAME_EVENT_URL_CREATE_WITH_RESPONSE, startTimeMillis);
    }

    /**
     * Creates an event tracking URL that redirects to the provided destination URL when it is
     * clicked in an ODP webview.
     *
     * @param eventParams The data to be passed to
     *     {@code IsolatedWorker#onEvent(EventInput, android.os.OutcomeReceiver)}
     *     when the event occurs
     * @param destinationUrl The URL to redirect to.
     * @return An ODP event URL that can be inserted into a WebView.
     */
    @WorkerThread
    @NonNull public Uri createEventTrackingUrlWithRedirect(
            @NonNull PersistableBundle eventParams,
            @Nullable Uri destinationUrl) {
        final long startTimeMillis = System.currentTimeMillis();
        Bundle params = new Bundle();
        params.putParcelable(Constants.EXTRA_EVENT_PARAMS, eventParams);
        params.putString(Constants.EXTRA_DESTINATION_URL, destinationUrl.toString());
        return getUrl(params, Constants.API_NAME_EVENT_URL_CREATE_WITH_REDIRECT, startTimeMillis);
    }

    @NonNull private Uri getUrl(
            @NonNull Bundle params, int apiName, long startTimeMillis) {
        int responseCode = Constants.STATUS_SUCCESS;
        try {
            BlockingQueue<CallbackResult> asyncResult = new ArrayBlockingQueue<>(1);

            mDataAccessService.onRequest(
                    Constants.DATA_ACCESS_OP_GET_EVENT_URL,
                    params,
                    new IDataAccessServiceCallback.Stub() {
                        @Override
                        public void onSuccess(@NonNull Bundle result) {
                            asyncResult.add(new CallbackResult(result, 0));
                        }
                        @Override
                        public void onError(int errorCode) {
                            asyncResult.add(new CallbackResult(null, errorCode));
                        }
                });
            CallbackResult callbackResult = asyncResult.take();
            Objects.requireNonNull(callbackResult);
            if (callbackResult.mErrorCode != 0) {
                throw new IllegalStateException("Error: " + callbackResult.mErrorCode);
            }
            Bundle result = Objects.requireNonNull(callbackResult.mResult);
            Uri url = Objects.requireNonNull(
                    result.getParcelable(Constants.EXTRA_RESULT, Uri.class));
            return url;
        } catch (InterruptedException | RemoteException e) {
            responseCode = Constants.STATUS_INTERNAL_ERROR;
            throw new RuntimeException(e);
        } finally {
            try {
                mDataAccessService.logApiCallStats(
                        apiName,
                        System.currentTimeMillis() - startTimeMillis,
                        responseCode);
            } catch (Exception e) {
                sLogger.d(e, TAG + ": failed to log metrics");
            }
        }
    }

    private static class CallbackResult {
        final Bundle mResult;
        final int mErrorCode;

        CallbackResult(Bundle result, int errorCode) {
            mResult = result;
            mErrorCode = errorCode;
        }
    }
}

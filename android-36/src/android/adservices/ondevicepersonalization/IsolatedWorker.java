/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.annotation.NonNull;
import android.os.OutcomeReceiver;

/**
 * Interface with methods that need to be implemented to handle requests from the
 * OnDevicePersonalization service to an {@link IsolatedService}. The {@link IsolatedService}
 * creates an instance of {@link IsolatedWorker} on each request and calls one of the methods
 * below, depending the type of the request. The {@link IsolatedService} calls the method on a
 * Binder thread and the {@link IsolatedWorker} should offload long running operations to a
 * worker thread. The {@link IsolatedWorker} should use the {@code receiver} parameter of each
 * method to return results. If any of these methods throws a {@link RuntimeException}, the
 * platform treats it as an unrecoverable error in the {@link IsolatedService} and ends processing
 * the request.
 */
public interface IsolatedWorker {

    /**
     * Handles a request from an app. This method is called when an app calls {@code
     * OnDevicePersonalizationManager#execute(ComponentName, PersistableBundle,
     * java.util.concurrent.Executor, OutcomeReceiver)} that refers to a named
     * {@link IsolatedService}.
     *
     * @param input Request Parameters from the calling app.
     * @param receiver Callback that receives the result {@link ExecuteOutput} or an
     *     {@link IsolatedServiceException}. If this method throws a {@link RuntimeException} or
     *     returns either {@code null} or {@link IsolatedServiceException}, the error is indicated
     *     to the calling app as an {@link OnDevicePersonalizationException} with error code
     *     {@link OnDevicePersonalizationException#ERROR_ISOLATED_SERVICE_FAILED}. To avoid leaking
     *     private data to the calling app, more detailed errors are not reported to the caller.
     *     If the {@link IsolatedService} needs to report additional data beyond the error code to
     *     its backend servers, it should populate the logging fields in {@link ExecuteOutput} with
     *     the additional error data for logging, and rely on Federated Analytics for the stats.
     */
    default void onExecute(
            @NonNull ExecuteInput input,
            @NonNull OutcomeReceiver<ExecuteOutput, IsolatedServiceException> receiver) {
        receiver.onResult(new ExecuteOutput.Builder().build());
    }

    /**
     * Handles a completed download. The platform downloads content using the parameters defined in
     * the package manifest of the {@link IsolatedService}, calls this function after the download
     * is complete, and updates the REMOTE_DATA table from
     * {@link IsolatedService#getRemoteData(RequestToken)} with the result of this method.
     *
     * @param input Download handler parameters.
     * @param receiver Callback that receives the result {@link DownloadCompletedOutput} or an
     *     {@link IsolatedServiceException}.
     *     <p>If this method returns a {@code null} result or exception via the callback, or
     *     throws a {@link RuntimeException}, no updates are made to the REMOTE_DATA table.
     */
    default void onDownloadCompleted(
            @NonNull DownloadCompletedInput input,
            @NonNull OutcomeReceiver<DownloadCompletedOutput, IsolatedServiceException> receiver) {
        receiver.onResult(new DownloadCompletedOutput.Builder().build());
    }

    /**
     * Generates HTML for the results that were returned as a result of
     * {@link #onExecute(ExecuteInput, android.os.OutcomeReceiver)}. Called when a client app calls
     * {@link OnDevicePersonalizationManager#requestSurfacePackage(SurfacePackageToken, IBinder, int, int, int, java.util.concurrent.Executor, OutcomeReceiver)}.
     * The platform will render this HTML in an {@link android.webkit.WebView} inside a fenced
     * frame.
     *
     * @param input Parameters for the render request.
     * @param receiver Callback that receives the result {@link RenderOutput} or an
     *     {@link IsolatedServiceException}.
     *     <p>If this method returns a {@code null} result or exception via the callback, or
     *     throws a {@link RuntimeException}, the error is also reported to calling
     *     apps as an {@link OnDevicePersonalizationException} with error code {@link
     *     OnDevicePersonalizationException#ERROR_ISOLATED_SERVICE_FAILED}.
     */
    default void onRender(
            @NonNull RenderInput input,
            @NonNull OutcomeReceiver<RenderOutput, IsolatedServiceException> receiver) {
        receiver.onResult(new RenderOutput.Builder().build());
    }

    /**
     * Handles an event triggered by a request to a platform-provided tracking URL {@link
     * EventUrlProvider} that was embedded in the HTML output returned by
     * {@link #onRender(RenderInput, android.os.OutcomeReceiver)}. The platform updates the EVENTS table with
     * {@link EventOutput#getEventLogRecord()}.
     *
     * @param input The parameters needed to compute event data.
     * @param receiver Callback that receives the result {@link EventOutput} or an
     *     {@link IsolatedServiceException}.
     *     <p>If this method returns a {@code null} result or exception via the callback, or
     *     throws a {@link RuntimeException}, no data is written to the EVENTS table.
     */
    default void onEvent(
            @NonNull EventInput input,
            @NonNull OutcomeReceiver<EventOutput, IsolatedServiceException> receiver) {
        receiver.onResult(new EventOutput.Builder().build());
    }

    /**
     * Generate a list of training examples used for federated compute job. The platform will call
     * this function when a federated compute job starts. The federated compute job is scheduled by
     * an app through {@link FederatedComputeScheduler#schedule}.
     *
     * @param input The parameters needed to generate the training example.
     * @param receiver Callback that receives the result {@link TrainingExamplesOutput} or an
     *     {@link IsolatedServiceException}.
     *     <p>If this method returns a {@code null} result or exception via the callback, or
     *     throws a {@link RuntimeException}, no training examples is produced for this
     *     training session.
     */
    default void onTrainingExamples(
            @NonNull TrainingExamplesInput input,
            @NonNull OutcomeReceiver<TrainingExamplesOutput, IsolatedServiceException> receiver) {
        receiver.onResult(new TrainingExamplesOutput.Builder().build());
    }

    /**
     * Handles a Web Trigger event from a browser. A Web Trigger event occurs when a browser
     * registers a web trigger event with the OS using the <a href="https://github.com/WICG/attribution-reporting-api">
     * Attribution and Reporting API</a>. If the data in the web trigger payload indicates that the
     * event should be forwarded to an {@link IsolatedService}, the platform will call this function
     * with the web trigger data.
     *
     * @param input The parameters needed to process Web Trigger event.
     * @param receiver Callback that receives the result {@link WebTriggerOutput} or an
     *     {@link IsolatedServiceException}. Should be called with a
     *     {@link WebTriggerOutput} object populated with a set of records to be written to the
     *     REQUESTS or EVENTS tables.
     *     <p>If this method returns a {@code null} result or exception via the callback, or
     *     throws a {@link RuntimeException}, no data is written to the REQUESTS orEVENTS tables.
     */
    default void onWebTrigger(
            @NonNull WebTriggerInput input,
            @NonNull OutcomeReceiver<WebTriggerOutput, IsolatedServiceException> receiver) {
        receiver.onResult(new WebTriggerOutput.Builder().build());
    }
}

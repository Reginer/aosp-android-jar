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

package android.ondevicepersonalization;

import android.annotation.NonNull;

import java.util.function.Consumer;

/**
 * Interface for services that perform personalized computation using user data.
 * @hide
 */
public interface IsolatedComputationHandler {

    /**
     * Handle a request from an app. A {@link IsolatedComputationService} that
     * processes requests from apps must override this method.
     *
     * @param input App Request Parameters.
     * @param odpContext The per-request state for this request.
     * @param consumer Callback to be invoked on completion.
     */
    default void onExecute(
            @NonNull ExecuteInput input,
            @NonNull OnDevicePersonalizationContext odpContext,
            @NonNull Consumer<ExecuteOutput> consumer
    ) {
        consumer.accept(null);
    }

    /**
     * Handle a completed download. The platform downloads content using the
     * parameters defined in the package manifest of the {@link IsolatedComputationService}
     * and calls this function after the download is complete.
     *
     * @param input Download handler parameters.
     * @param odpContext The per-request state for this request.
     * @param consumer Callback to be invoked on completion.
     */
    default void onDownload(
            @NonNull DownloadInput input,
            @NonNull OnDevicePersonalizationContext odpContext,
            @NonNull Consumer<DownloadOutput> consumer
    ) {
        consumer.accept(null);
    }

    /**
     * Generate HTML for the winning bids that returned as a result of {@link execute}.
     * The platform will render this HTML in a WebView inside a fenced frame.
     *
     * @param input Parameters for the renderContent request.
     * @param odpContext The per-request state for this request.
     * @param consumer Callback to be invoked on completion.
     */
    default void onRender(
            @NonNull RenderInput input,
            @NonNull OnDevicePersonalizationContext odpContext,
            @NonNull Consumer<RenderOutput> consumer
    ) {
        consumer.accept(null);
    }

    /**
     * Compute a list of metrics to be logged in the events table with this event.
     *
     * @param input The query-time data required to compute the event metrics.
     * @param odpContext The per-request state for this request.
     * @param consumer Callback to be invoked on completion.
     */
    // TODO(b/259950177): Also provide the Query event from the Query table.
    default void onEvent(
            @NonNull EventInput input,
            @NonNull OnDevicePersonalizationContext odpContext,
            @NonNull Consumer<EventOutput> consumer
    ) {
        consumer.accept(null);
    }
}

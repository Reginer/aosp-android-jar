/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.server.media;

import static com.android.server.media.MediaRouterStatsLog.MEDIA_ROUTER_EVENT_REPORTED__EVENT_TYPE__EVENT_TYPE_SCANNING_STARTED;
import static com.android.server.media.MediaRouterStatsLog.MEDIA_ROUTER_EVENT_REPORTED__EVENT_TYPE__EVENT_TYPE_SCANNING_STOPPED;
import static com.android.server.media.MediaRouterStatsLog.MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_FAILED_TO_REROUTE_SYSTEM_MEDIA;
import static com.android.server.media.MediaRouterStatsLog.MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_INVALID_COMMAND;
import static com.android.server.media.MediaRouterStatsLog.MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_NETWORK_ERROR;
import static com.android.server.media.MediaRouterStatsLog.MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_REJECTED;
import static com.android.server.media.MediaRouterStatsLog.MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_ROUTE_NOT_AVAILABLE;
import static com.android.server.media.MediaRouterStatsLog.MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_UNIMPLEMENTED;
import static com.android.server.media.MediaRouterStatsLog.MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_UNKNOWN_ERROR;
import static com.android.server.media.MediaRouterStatsLog.MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_UNSPECIFIED;

import android.annotation.NonNull;
import android.media.MediaRoute2ProviderService;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Logs metrics for MediaRouter2.
 *
 * @hide
 */
final class MediaRouterMetricLogger {
    private static final String TAG = "MediaRouterMetricLogger";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final int REQUEST_INFO_CACHE_CAPACITY = 100;

    /** LRU cache to store request info. */
    private final RequestInfoCache mRequestInfoCache;

    /** Constructor for {@link MediaRouterMetricLogger}. */
    public MediaRouterMetricLogger() {
        mRequestInfoCache = new RequestInfoCache(REQUEST_INFO_CACHE_CAPACITY);
    }

    /**
     * Adds a new request info to the cache.
     *
     * @param uniqueRequestId The unique request id.
     * @param eventType The event type.
     */
    public void addRequestInfo(long uniqueRequestId, int eventType) {
        RequestInfo requestInfo = new RequestInfo(uniqueRequestId, eventType);
        mRequestInfoCache.put(requestInfo.mUniqueRequestId, requestInfo);
    }

    /**
     * Removes a request info from the cache.
     *
     * @param uniqueRequestId The unique request id.
     */
    public void removeRequestInfo(long uniqueRequestId) {
        mRequestInfoCache.remove(uniqueRequestId);
    }

    /**
     * Logs an operation failure.
     *
     * @param eventType The event type.
     * @param result The result of the operation.
     */
    public void logOperationFailure(int eventType, int result) {
        logMediaRouterEvent(eventType, result);
    }

    /**
     * Logs an operation triggered.
     *
     * @param eventType The event type.
     */
    public void logOperationTriggered(int eventType, int result) {
        logMediaRouterEvent(eventType, result);
    }

    /**
     * Logs the result of a request.
     *
     * @param uniqueRequestId The unique request id.
     * @param result The result of the request.
     */
    public void logRequestResult(long uniqueRequestId, int result) {
        RequestInfo requestInfo = mRequestInfoCache.get(uniqueRequestId);
        if (requestInfo == null) {
            Slog.w(
                    TAG,
                    "logRequestResult: No RequestInfo found for uniqueRequestId="
                            + uniqueRequestId);
            return;
        }

        int eventType = requestInfo.mEventType;
        logMediaRouterEvent(eventType, result);

        removeRequestInfo(uniqueRequestId);
    }

    /**
     * Logs the overall scanning state.
     *
     * @param isScanning The scanning state for the user.
     */
    public void updateScanningState(boolean isScanning) {
        if (!isScanning) {
            logScanningStopped();
        } else {
            logScanningStarted();
        }
    }

    /** Logs the scanning started event. */
    private void logScanningStarted() {
        logMediaRouterEvent(
                MEDIA_ROUTER_EVENT_REPORTED__EVENT_TYPE__EVENT_TYPE_SCANNING_STARTED,
                MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_UNSPECIFIED);
    }

    /** Logs the scanning stopped event. */
    private void logScanningStopped() {
        logMediaRouterEvent(
                MEDIA_ROUTER_EVENT_REPORTED__EVENT_TYPE__EVENT_TYPE_SCANNING_STOPPED,
                MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_UNSPECIFIED);
    }

    /**
     * Converts a reason code from {@link MediaRoute2ProviderService} to a result code for logging.
     *
     * @param reason The reason code from {@link MediaRoute2ProviderService}.
     * @return The result code for logging.
     */
    public static int convertResultFromReason(int reason) {
        switch (reason) {
            case MediaRoute2ProviderService.REASON_UNKNOWN_ERROR:
                return MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_UNKNOWN_ERROR;
            case MediaRoute2ProviderService.REASON_REJECTED:
                return MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_REJECTED;
            case MediaRoute2ProviderService.REASON_NETWORK_ERROR:
                return MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_NETWORK_ERROR;
            case MediaRoute2ProviderService.REASON_ROUTE_NOT_AVAILABLE:
                return MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_ROUTE_NOT_AVAILABLE;
            case MediaRoute2ProviderService.REASON_INVALID_COMMAND:
                return MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_INVALID_COMMAND;
            case MediaRoute2ProviderService.REASON_UNIMPLEMENTED:
                return MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_UNIMPLEMENTED;
            case MediaRoute2ProviderService.REASON_FAILED_TO_REROUTE_SYSTEM_MEDIA:
                return MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_FAILED_TO_REROUTE_SYSTEM_MEDIA;
            default:
                return MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_UNSPECIFIED;
        }
    }

    /**
     * Gets the size of the request info cache.
     *
     * @return The size of the request info cache.
     */
    @VisibleForTesting
    public int getRequestCacheSize() {
        return mRequestInfoCache.size();
    }

    private void logMediaRouterEvent(int eventType, int result) {
        MediaRouterStatsLog.write(
                MediaRouterStatsLog.MEDIA_ROUTER_EVENT_REPORTED, eventType, result);

        if (DEBUG) {
            Slog.d(TAG, "logMediaRouterEvent: " + eventType + " " + result);
        }
    }

    /** A cache for storing request info that evicts entries when it reaches its capacity. */
    class RequestInfoCache extends LinkedHashMap<Long, RequestInfo> {

        public final int capacity;

        /**
         * Constructor for {@link RequestInfoCache}.
         *
         * @param capacity The maximum capacity of the cache.
         */
        public RequestInfoCache(int capacity) {
            super(capacity, 1.0f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, RequestInfo> eldest) {
            boolean shouldRemove = size() > capacity;
            if (shouldRemove) {
                Slog.d(TAG, "Evicted request info: " + eldest.getValue());
                logOperationTriggered(
                        eldest.getValue().mEventType,
                        MEDIA_ROUTER_EVENT_REPORTED__RESULT__RESULT_UNSPECIFIED);
            }
            return shouldRemove;
        }
    }

    /** Class to store request info. */
    static class RequestInfo {
        public final long mUniqueRequestId;
        public final int mEventType;

        /**
         * Constructor for {@link RequestInfo}.
         *
         * @param uniqueRequestId The unique request id.
         * @param eventType The event type.
         */
        RequestInfo(long uniqueRequestId, int eventType) {
            mUniqueRequestId = uniqueRequestId;
            mEventType = eventType;
        }

        /**
         * Dumps the request info.
         *
         * @param pw The print writer.
         * @param prefix The prefix for the output.
         */
        public void dump(@NonNull PrintWriter pw, @NonNull String prefix) {
            pw.println(prefix + "RequestInfo");
            String indent = prefix + "  ";
            pw.println(indent + "mUniqueRequestId=" + mUniqueRequestId);
            pw.println(indent + "mEventType=" + mEventType);
        }
    }
}

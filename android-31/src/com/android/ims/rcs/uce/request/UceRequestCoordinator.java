/*
 * Copyright (c) 2021 The Android Open Source Project
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

package com.android.ims.rcs.uce.request;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.telephony.ims.RcsUceAdapter;
import android.util.Log;

import com.android.ims.rcs.uce.request.UceRequestManager.RequestManagerCallback;
import com.android.ims.rcs.uce.util.UceUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The base class that is responsible for the communication and interaction between the UceRequests.
 */
public abstract class UceRequestCoordinator {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "ReqCoordinator";

    /**
     * The UceRequest encountered error.
     */
    public static final int REQUEST_UPDATE_ERROR = 0;

    /**
     * The UceRequest received the onCommandError callback.
     */
    public static final int REQUEST_UPDATE_COMMAND_ERROR = 1;

    /**
     * The UceRequest received the onNetworkResponse callback.
     */
    public static final int REQUEST_UPDATE_NETWORK_RESPONSE = 2;

    /**
     * The UceRequest received the onNotifyCapabilitiesUpdate callback.
     */
    public static final int REQUEST_UPDATE_CAPABILITY_UPDATE = 3;

    /**
     * The UceRequest received the onResourceTerminated callback.
     */
    public static final int REQUEST_UPDATE_RESOURCE_TERMINATED = 4;

    /**
     * The UceRequest retrieve the valid capabilities from the cache.
     */
    public static final int REQUEST_UPDATE_CACHED_CAPABILITY_UPDATE = 5;

    /**
     * The UceRequest receive the onTerminated callback.
     */
    public static final int REQUEST_UPDATE_TERMINATED = 6;

    /**
     * The UceRequest does not need to request capabilities to network because all the capabilities
     * can be retrieved from the cache.
     */
    public static final int REQUEST_UPDATE_NO_NEED_REQUEST_FROM_NETWORK = 7;

    /**
     * The remote options request is done.
     */
    public static final int REQUEST_UPDATE_REMOTE_REQUEST_DONE = 8;

    /**
     * The capabilities request is timeout.
     */
    public static final int REQUEST_UPDATE_TIMEOUT = 9;

    @IntDef(value = {
            REQUEST_UPDATE_ERROR,
            REQUEST_UPDATE_COMMAND_ERROR,
            REQUEST_UPDATE_NETWORK_RESPONSE,
            REQUEST_UPDATE_TERMINATED,
            REQUEST_UPDATE_RESOURCE_TERMINATED,
            REQUEST_UPDATE_CAPABILITY_UPDATE,
            REQUEST_UPDATE_CACHED_CAPABILITY_UPDATE,
            REQUEST_UPDATE_NO_NEED_REQUEST_FROM_NETWORK,
            REQUEST_UPDATE_REMOTE_REQUEST_DONE,
            REQUEST_UPDATE_TIMEOUT,
    }, prefix="REQUEST_UPDATE_")
    @Retention(RetentionPolicy.SOURCE)
    @interface UceRequestUpdate {}

    protected static Map<Integer, String> REQUEST_EVENT_DESC = new HashMap<>();
    static {
        REQUEST_EVENT_DESC.put(REQUEST_UPDATE_ERROR, "REQUEST_ERROR");
        REQUEST_EVENT_DESC.put(REQUEST_UPDATE_COMMAND_ERROR, "RETRIEVE_COMMAND_ERROR");
        REQUEST_EVENT_DESC.put(REQUEST_UPDATE_NETWORK_RESPONSE, "REQUEST_NETWORK_RESPONSE");
        REQUEST_EVENT_DESC.put(REQUEST_UPDATE_TERMINATED, "REQUEST_TERMINATED");
        REQUEST_EVENT_DESC.put(REQUEST_UPDATE_RESOURCE_TERMINATED, "REQUEST_RESOURCE_TERMINATED");
        REQUEST_EVENT_DESC.put(REQUEST_UPDATE_CAPABILITY_UPDATE, "REQUEST_CAPABILITY_UPDATE");
        REQUEST_EVENT_DESC.put(REQUEST_UPDATE_CACHED_CAPABILITY_UPDATE, "REQUEST_CACHE_CAP_UPDATE");
        REQUEST_EVENT_DESC.put(REQUEST_UPDATE_NO_NEED_REQUEST_FROM_NETWORK, "NO_NEED_REQUEST");
        REQUEST_EVENT_DESC.put(REQUEST_UPDATE_REMOTE_REQUEST_DONE, "REMOTE_REQUEST_DONE");
        REQUEST_EVENT_DESC.put(REQUEST_UPDATE_TIMEOUT, "REQUEST_TIMEOUT");
    }

    /**
     * The result of the UceRequest. This is the used by the RequestCoordinator to record the
     * result of each sub-requests.
     */
    static class RequestResult {
        /**
         * Create a RequestResult that successfully completes the request.
         * @param taskId the task id of the UceRequest
         */
        public static RequestResult createSuccessResult(long taskId) {
            return new RequestResult(taskId);
        }

        /**
         * Create a RequestResult for the failed request.
         * @param taskId the task id of the UceRequest
         * @param errorCode the error code of the failed request
         * @param retry When the request can be retried.
         */
        public static RequestResult createFailedResult(long taskId, int errorCode, long retry) {
            return new RequestResult(taskId, errorCode, retry);
        }

        private final Long mTaskId;
        private final Boolean mIsSuccess;
        private final Optional<Integer> mErrorCode;
        private final Optional<Long> mRetryMillis;

        /**
         * The private constructor for the successful request.
         */
        private RequestResult(long taskId) {
            mTaskId = taskId;
            mIsSuccess = true;
            mErrorCode = Optional.empty();
            mRetryMillis = Optional.empty();
        }

        /**
         * The private constructor for the failed request.
         */
        private RequestResult(long taskId, int errorCode, long retryMillis) {
            mTaskId = taskId;
            mIsSuccess = false;
            mErrorCode = Optional.of(errorCode);
            mRetryMillis = Optional.of(retryMillis);
        }

        public long getTaskId() {
            return mTaskId;
        }

        public boolean isRequestSuccess() {
            return mIsSuccess;
        }

        public Optional<Integer> getErrorCode() {
            return mErrorCode;
        }

        public Optional<Long> getRetryMillis() {
            return mRetryMillis;
        }
    }

    // The default capability error code.
    protected static final int DEFAULT_ERROR_CODE = RcsUceAdapter.ERROR_GENERIC_FAILURE;

    protected final int mSubId;
    protected final long mCoordinatorId;
    protected volatile boolean mIsFinished;

    // The collection of activated requests.
    protected final Map<Long, UceRequest> mActivatedRequests;
    // The collection of the finished requests.
    protected final Map<Long, RequestResult> mFinishedRequests;
    // The lock of the activated and finished collection.
    protected final Object mCollectionLock = new Object();

    // The callback to communicate with UceRequestManager
    protected final RequestManagerCallback mRequestManagerCallback;

    public UceRequestCoordinator(int subId, Collection<UceRequest> requests,
            RequestManagerCallback requestMgrCallback) {
        mSubId = subId;
        mCoordinatorId = UceUtils.generateRequestCoordinatorId();
        mRequestManagerCallback = requestMgrCallback;

        // Set the coordinatorId to all the given UceRequests
        requests.forEach(request -> request.setRequestCoordinatorId(mCoordinatorId));

        // All the given requests are put in the activated request at the beginning.
        mFinishedRequests = new HashMap<>();
        mActivatedRequests = requests.stream().collect(
                Collectors.toMap(UceRequest::getTaskId, request -> request));
    }

    /**
     * @return Get the request coordinator ID.
     */
    public long getCoordinatorId() {
        return mCoordinatorId;
    }

    /**
     * @return Get the collection of task ID of all the activated requests.
     */
    public @NonNull List<Long> getActivatedRequestTaskIds() {
        synchronized (mCollectionLock) {
            return mActivatedRequests.values().stream()
                    .map(request -> request.getTaskId())
                    .collect(Collectors.toList());
        }
    }

    /**
     * @return Get the UceRequest associated with the given taskId from the activated requests.
     */
    public @Nullable UceRequest getUceRequest(Long taskId) {
        synchronized (mCollectionLock) {
            return mActivatedRequests.get(taskId);
        }
    }

    /**
     * Remove the UceRequest associated with the given taskId from the activated collection and
     * add the {@link RequestResult} into the finished request collection. This method is called by
     * the coordinator instance when it receives the request updated event and judges this request
     * is finished.
     */
    protected void moveRequestToFinishedCollection(Long taskId, RequestResult requestResult) {
        synchronized (mCollectionLock) {
            mActivatedRequests.remove(taskId);
            mFinishedRequests.put(taskId, requestResult);
            mRequestManagerCallback.notifyUceRequestFinished(getCoordinatorId(), taskId);
        }
    }

    /**
     * Notify this coordinator instance is finished. This method sets the finish flag and clear all
     * the UceRequest collections and it can be used anymore after the method is called.
     */
    public void onFinish() {
        mIsFinished = true;
        synchronized (mCollectionLock) {
            mActivatedRequests.forEach((taskId, request) -> request.onFinish());
            mActivatedRequests.clear();
            mFinishedRequests.clear();
        }
    }

    /**
     * Notify the UceRequest associated with the given taskId in the coordinator is updated.
     */
    public abstract void onRequestUpdated(long taskId, @UceRequestUpdate int event);

    protected void logd(String log) {
        Log.d(LOG_TAG, getLogPrefix().append(log).toString());
    }

    protected void logw(String log) {
        Log.w(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private StringBuilder getLogPrefix() {
        StringBuilder builder = new StringBuilder("[");
        builder.append(mSubId).append("][coordId=").append(mCoordinatorId).append("] ");
        return builder;
    }
}

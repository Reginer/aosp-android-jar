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

package com.android.ims.rcs.uce.request;

import android.util.Log;

import com.android.ims.rcs.uce.request.UceRequestManager.RequestManagerCallback;
import com.android.ims.rcs.uce.util.UceUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Calculate network carry capabilities and dispatcher the UceRequests.
 */
public class UceRequestDispatcher {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "RequestDispatcher";

    /**
     * Record the request timestamp.
     */
    private static class Request {
        private final long mTaskId;
        private final long mCoordinatorId;
        private Optional<Instant> mExecutingTime;

        public Request(long coordinatorId, long taskId) {
            mTaskId = taskId;
            mCoordinatorId = coordinatorId;
            mExecutingTime = Optional.empty();
        }

        public long getCoordinatorId() {
            return mCoordinatorId;
        }

        public long getTaskId() {
            return mTaskId;
        }

        public void setExecutingTime(Instant instant) {
            mExecutingTime = Optional.of(instant);
        }

        public Optional<Instant> getExecutingTime() {
            return mExecutingTime;
        }
    }

    private final int mSubId;

    // The interval milliseconds for each request.
    private long mIntervalTime = 100;

    // The number of requests that the network can process at the same time.
    private int mMaxConcurrentNum = 1;

    // The collection of all requests waiting to be executed.
    private final List<Request> mWaitingRequests = new ArrayList<>();

    // The collection of all executing requests.
    private final List<Request> mExecutingRequests = new ArrayList<>();

    // The callback to communicate with UceRequestManager
    private RequestManagerCallback mRequestManagerCallback;

    public UceRequestDispatcher(int subId, RequestManagerCallback callback) {
        mSubId = subId;
        mRequestManagerCallback = callback;
    }

    /**
     * Clear all the collections when the instance is destroyed.
     */
    public synchronized void onDestroy() {
        mWaitingRequests.clear();
        mExecutingRequests.clear();
        mRequestManagerCallback = null;
    }

    /**
     * Add new requests to the waiting collection and trigger sending request if the network is
     * capable of processing the given requests.
     */
    public synchronized void addRequest(long coordinatorId, List<Long> taskIds) {
        taskIds.stream().forEach(taskId -> {
            Request request = new Request(coordinatorId, taskId);
            mWaitingRequests.add(request);
        });
        onRequestUpdated();
    }

    /**
     * Notify that the request with the given taskId is finished.
     */
    public synchronized void onRequestFinished(Long taskId) {
        logd("onRequestFinished: taskId=" + taskId);
        mExecutingRequests.removeIf(request -> request.getTaskId() == taskId);
        onRequestUpdated();
    }

    private synchronized void onRequestUpdated() {
        logd("onRequestUpdated: waiting=" + mWaitingRequests.size()
                + ", executing=" + mExecutingRequests.size());

        // Return if there is no waiting request.
        if (mWaitingRequests.isEmpty()) {
            return;
        }

        // Check how many more requests can be executed and return if the size of executing
        // requests have reached the maximum number.
        int numCapacity = mMaxConcurrentNum - mExecutingRequests.size();
        if (numCapacity <= 0) {
            return;
        }

        List<Request> requestList = getRequestFromWaitingCollection(numCapacity);
        if (!requestList.isEmpty()) {
            notifyStartOfRequest(requestList);
        }
    }

    /*
     * Retrieve the given number of requests from the WaitingRequestList.
     */
    private List<Request> getRequestFromWaitingCollection(int numCapacity) {
        // The number of the requests cannot more than the waiting requests.
        int numRequests = (numCapacity < mWaitingRequests.size()) ?
                numCapacity : mWaitingRequests.size();

        List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < numRequests; i++) {
            requestList.add(mWaitingRequests.get(i));
        }

        mWaitingRequests.removeAll(requestList);
        return requestList;
    }

    /**
     * Notify start of the UceRequest.
     */
    private void notifyStartOfRequest(List<Request> requestList) {
        RequestManagerCallback callback = mRequestManagerCallback;
        if (callback == null) {
            logd("notifyStartOfRequest: The instance is destroyed");
            return;
        }

        Instant lastRequestTime = getLastRequestTime();
        Instant baseTime;
        if (lastRequestTime.plusMillis(mIntervalTime).isAfter(Instant.now())) {
            baseTime = lastRequestTime.plusMillis(mIntervalTime);
        } else {
            baseTime = Instant.now();
        }

        StringBuilder builder = new StringBuilder("notifyStartOfRequest: taskId=");
        for (int i = 0; i < requestList.size(); i++) {
            Instant startExecutingTime = baseTime.plusMillis((mIntervalTime * i));
            Request request = requestList.get(i);
            request.setExecutingTime(startExecutingTime);

            // Add the request to the executing collection
            mExecutingRequests.add(request);

            // Notify RequestManager to execute this task.
            long taskId = request.getTaskId();
            long coordId = request.getCoordinatorId();
            long delayTime = getDelayTime(startExecutingTime);
            mRequestManagerCallback.notifySendingRequest(coordId, taskId, delayTime);

            builder.append(request.getTaskId() + ", ");
        }
        builder.append("ExecutingRequests size=" + mExecutingRequests.size());
        logd(builder.toString());
    }

    private Instant getLastRequestTime() {
        if (mExecutingRequests.isEmpty()) {
            return Instant.MIN;
        }

        Instant lastTime = Instant.MIN;
        for (Request request : mExecutingRequests) {
            if (!request.getExecutingTime().isPresent()) continue;
            Instant executingTime = request.getExecutingTime().get();
            if (executingTime.isAfter(lastTime)) {
                lastTime = executingTime;
            }
        }
        return lastTime;
    }

    private long getDelayTime(Instant executingTime) {
        long delayTime = Duration.between(executingTime, Instant.now()).toMillis();
        if (delayTime < 0L) {
            delayTime = 0;
        }
        return delayTime;
    }

    private void logd(String log) {
        Log.d(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private StringBuilder getLogPrefix() {
        StringBuilder builder = new StringBuilder("[");
        builder.append(mSubId);
        builder.append("] ");
        return builder;
    }
}


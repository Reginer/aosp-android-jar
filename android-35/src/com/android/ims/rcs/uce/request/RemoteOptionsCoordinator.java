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

import static com.android.ims.rcs.uce.util.NetworkSipCode.SIP_CODE_SERVER_INTERNAL_ERROR;
import static com.android.ims.rcs.uce.util.NetworkSipCode.SIP_SERVICE_UNAVAILABLE;

import android.os.RemoteException;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.aidl.IOptionsRequestCallback;

import com.android.ims.rcs.uce.request.RemoteOptionsRequest.RemoteOptResponse;
import com.android.ims.rcs.uce.request.UceRequestManager.RequestManagerCallback;
import com.android.ims.rcs.uce.UceStatsWriter;
import com.android.internal.annotations.VisibleForTesting;

import java.util.Collection;

/**
 * Responsible for the manager the remote options request and triggering the callback to notify
 * the result of the request.
 */
public class RemoteOptionsCoordinator extends UceRequestCoordinator {
    /**
     * The builder of the RemoteOptionsCoordinator.
     */
    public static final class Builder {
        RemoteOptionsCoordinator mRemoteOptionsCoordinator;

        public Builder(int subId, Collection<UceRequest> requests, RequestManagerCallback c) {
            mRemoteOptionsCoordinator = new RemoteOptionsCoordinator(subId, requests, c,
                    UceStatsWriter.getInstance());
        }
        @VisibleForTesting
        public Builder(int subId, Collection<UceRequest> requests, RequestManagerCallback c,
                UceStatsWriter instance) {
            mRemoteOptionsCoordinator = new RemoteOptionsCoordinator(subId, requests, c, instance);
        }

        public Builder setOptionsRequestCallback(IOptionsRequestCallback callback) {
            mRemoteOptionsCoordinator.setOptionsRequestCallback(callback);
            return this;
        }

        public RemoteOptionsCoordinator build() {
            return mRemoteOptionsCoordinator;
        }
    }

    /**
     * Different request updated events will create different {@link RequestResult}. Define the
     * interface to get the {@link RequestResult} instance according to the given task ID and
     * {@link RemoteOptResponse}.
     */
    @FunctionalInterface
    private interface RequestResultCreator {
        RequestResult createRequestResult(long taskId, RemoteOptResponse response);
    }

    // The RequestResult creator of the remote options response.
    private static final RequestResultCreator sRemoteResponseCreator = (taskId, response) -> {
        RcsContactUceCapability capability = response.getRcsContactCapability();
        if (capability != null) {
            return RequestResult.createSuccessResult(taskId);
        } else {
            int errorCode = response.getErrorSipCode().orElse(SIP_CODE_SERVER_INTERNAL_ERROR);
            return RequestResult.createFailedResult(taskId, errorCode, 0L);
        }
    };

    // The callback to notify the result of the remote options request.
    private IOptionsRequestCallback mOptionsReqCallback;

    private final UceStatsWriter mUceStatsWriter;

    private RemoteOptionsCoordinator(int subId, Collection<UceRequest> requests,
            RequestManagerCallback requestMgrCallback, UceStatsWriter instance) {
        super(subId, requests, requestMgrCallback);
        mUceStatsWriter = instance;
        logd("RemoteOptionsCoordinator: created");
    }

    public void setOptionsRequestCallback(IOptionsRequestCallback callback) {
        mOptionsReqCallback = callback;
    }

    @Override
    public void onFinish() {
        logd("RemoteOptionsCoordinator: onFinish");
        mOptionsReqCallback = null;
        super.onFinish();
    }

    @Override
    public void onRequestUpdated(long taskId, int event) {
        if (mIsFinished) return;
        RemoteOptionsRequest request = (RemoteOptionsRequest) getUceRequest(taskId);
        if (request == null) {
            logw("onRequestUpdated: Cannot find RemoteOptionsRequest taskId=" + taskId);
            return;
        }

        logd("onRequestUpdated: taskId=" + taskId + ", event=" + REQUEST_EVENT_DESC.get(event));
        switch (event) {
            case REQUEST_UPDATE_REMOTE_REQUEST_DONE:
                handleRemoteRequestDone(request);
                break;
            default:
                logw("onRequestUpdated: invalid event " + event);
                break;
        }

        // End this instance if all the UceRequests in the coordinator are finished.
        checkAndFinishRequestCoordinator();
    }

    private void handleRemoteRequestDone(RemoteOptionsRequest request) {
        // Trigger the options request callback
        RemoteOptResponse response = request.getRemoteOptResponse();
        RcsContactUceCapability capability = response.getRcsContactCapability();
        if (capability != null) {
            boolean isNumberBlocked = response.isNumberBlocked();
            triggerOptionsReqCallback(capability, isNumberBlocked);
        } else {
            int errorCode = response.getErrorSipCode().orElse(SIP_CODE_SERVER_INTERNAL_ERROR);
            String reason = response.getErrorReason().orElse(SIP_SERVICE_UNAVAILABLE);
            triggerOptionsReqWithErrorCallback(errorCode, reason);
        }

        // Finish this request.
        request.onFinish();

        // Remove this request from the activated collection and notify RequestManager.
        Long taskId = request.getTaskId();
        RequestResult requestResult = sRemoteResponseCreator.createRequestResult(taskId, response);
        moveRequestToFinishedCollection(taskId, requestResult);
    }

    private void triggerOptionsReqCallback(RcsContactUceCapability deviceCaps,
            boolean isRemoteNumberBlocked) {
        try {
            logd("triggerOptionsReqCallback: start");
            mUceStatsWriter.setUceEvent(mSubId, UceStatsWriter.INCOMING_OPTION_EVENT, true, 0,
                200);

            mOptionsReqCallback.respondToCapabilityRequest(deviceCaps, isRemoteNumberBlocked);
        } catch (RemoteException e) {
            logw("triggerOptionsReqCallback exception: " + e);
        } finally {
            logd("triggerOptionsReqCallback: done");
        }
    }

    private void triggerOptionsReqWithErrorCallback(int errorCode, String reason) {
        try {
            logd("triggerOptionsReqWithErrorCallback: start");
            mUceStatsWriter.setUceEvent(mSubId, UceStatsWriter.INCOMING_OPTION_EVENT, true, 0,
                errorCode);

            mOptionsReqCallback.respondToCapabilityRequestWithError(errorCode, reason);
        } catch (RemoteException e) {
            logw("triggerOptionsReqWithErrorCallback exception: " + e);
        } finally {
            logd("triggerOptionsReqWithErrorCallback: done");
        }
    }

    private void checkAndFinishRequestCoordinator() {
        synchronized (mCollectionLock) {
            // Return because there are requests running.
            if (!mActivatedRequests.isEmpty()) {
                return;
            }
            // Notify UceRequestManager to remove this instance from the collection.
            mRequestManagerCallback.notifyRequestCoordinatorFinished(mCoordinatorId);
            logd("checkAndFinishRequestCoordinator: id=" + mCoordinatorId);
        }
    }

    @VisibleForTesting
    public Collection<UceRequest> getActivatedRequest() {
        return mActivatedRequests.values();
    }

    @VisibleForTesting
    public Collection<RequestResult> getFinishedRequest() {
        return mFinishedRequests.values();
    }
}

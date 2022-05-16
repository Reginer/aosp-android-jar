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

import static android.telephony.ims.stub.RcsCapabilityExchangeImplBase.COMMAND_CODE_GENERIC_FAILURE;

import android.os.RemoteException;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsUceAdapter;
import android.telephony.ims.aidl.IRcsUceControllerCallback;

import com.android.ims.rcs.uce.request.UceRequestManager.RequestManagerCallback;
import com.android.ims.rcs.uce.UceStatsWriter;
import com.android.internal.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Responsible for the communication and interaction between OptionsRequests and triggering
 * the callback to notify the result of the capabilities request.
 */
public class OptionsRequestCoordinator extends UceRequestCoordinator {
    /**
     * The builder of the OptionsRequestCoordinator.
     */
    public static final class Builder {
        private OptionsRequestCoordinator mRequestCoordinator;

        public Builder(int subId, Collection<UceRequest> requests,
                RequestManagerCallback callback) {
            mRequestCoordinator = new OptionsRequestCoordinator(subId, requests, callback,
                    UceStatsWriter.getInstance());
        }
        @VisibleForTesting
        public Builder(int subId, Collection<UceRequest> requests,
                RequestManagerCallback callback, UceStatsWriter instance) {
            mRequestCoordinator = new OptionsRequestCoordinator(subId, requests, callback,
                    instance);
        }

        public Builder setCapabilitiesCallback(IRcsUceControllerCallback callback) {
            mRequestCoordinator.setCapabilitiesCallback(callback);
            return this;
        }

        public OptionsRequestCoordinator build() {
            return mRequestCoordinator;
        }
    }

    /**
     * Different request updated events will create different {@link RequestResult}. Define the
     * interface to get the {@link RequestResult} instance according to the given task ID and
     * {@link CapabilityRequestResponse}.
     */
    @FunctionalInterface
    private interface RequestResultCreator {
        RequestResult createRequestResult(long taskId, CapabilityRequestResponse response);
    }

    // The RequestResult creator of the request error.
    private static final RequestResultCreator sRequestErrorCreator = (taskId, response) -> {
        int errorCode = response.getRequestInternalError().orElse(DEFAULT_ERROR_CODE);
        long retryAfter = response.getRetryAfterMillis();
        return RequestResult.createFailedResult(taskId, errorCode, retryAfter);
    };

    // The RequestResult creator of the request command error.
    private static final RequestResultCreator sCommandErrorCreator = (taskId, response) -> {
        int cmdError = response.getCommandError().orElse(COMMAND_CODE_GENERIC_FAILURE);
        int errorCode = CapabilityRequestResponse.getCapabilityErrorFromCommandError(cmdError);
        long retryAfter = response.getRetryAfterMillis();
        return RequestResult.createFailedResult(taskId, errorCode, retryAfter);
    };

    // The RequestResult creator of the network response.
    private static final RequestResultCreator sNetworkRespCreator = (taskId, response) -> {
        if (response.isNetworkResponseOK()) {
            return RequestResult.createSuccessResult(taskId);
        } else {
            int errorCode = CapabilityRequestResponse.getCapabilityErrorFromSipCode(response);
            long retryAfter = response.getRetryAfterMillis();
            return RequestResult.createFailedResult(taskId, errorCode, retryAfter);
        }
    };

    // The RequestResult creator for does not need to request from the network.
    private static final RequestResultCreator sNotNeedRequestFromNetworkCreator =
            (taskId, response) -> RequestResult.createSuccessResult(taskId);

    // The RequestResult creator of the request timeout.
    private static final RequestResultCreator sRequestTimeoutCreator =
            (taskId, response) -> RequestResult.createFailedResult(taskId,
                    RcsUceAdapter.ERROR_REQUEST_TIMEOUT, 0L);

    // The callback to notify the result of the capabilities request.
    private IRcsUceControllerCallback mCapabilitiesCallback;

    private final UceStatsWriter mUceStatsWriter;

    private OptionsRequestCoordinator(int subId, Collection<UceRequest> requests,
            RequestManagerCallback requestMgrCallback, UceStatsWriter instance) {
        super(subId, requests, requestMgrCallback);
        mUceStatsWriter = instance;
        logd("OptionsRequestCoordinator: created");
    }

    private void setCapabilitiesCallback(IRcsUceControllerCallback callback) {
        mCapabilitiesCallback = callback;
    }

    @Override
    public void onFinish() {
        logd("OptionsRequestCoordinator: onFinish");
        mCapabilitiesCallback = null;
        super.onFinish();
    }

    @Override
    public void onRequestUpdated(long taskId, @UceRequestUpdate int event) {
        if (mIsFinished) return;
        OptionsRequest request = (OptionsRequest) getUceRequest(taskId);
        if (request == null) {
            logw("onRequestUpdated: Cannot find OptionsRequest taskId=" + taskId);
            return;
        }

        logd("onRequestUpdated(OptionsRequest): taskId=" + taskId + ", event=" +
                REQUEST_EVENT_DESC.get(event));

        switch (event) {
            case REQUEST_UPDATE_ERROR:
                handleRequestError(request);
                break;
            case REQUEST_UPDATE_COMMAND_ERROR:
                handleCommandError(request);
                break;
            case REQUEST_UPDATE_NETWORK_RESPONSE:
                handleNetworkResponse(request);
                break;
            case REQUEST_UPDATE_CACHED_CAPABILITY_UPDATE:
                handleCachedCapabilityUpdated(request);
                break;
            case REQUEST_UPDATE_NO_NEED_REQUEST_FROM_NETWORK:
                handleNoNeedRequestFromNetwork(request);
                break;
            case REQUEST_UPDATE_TIMEOUT:
                handleRequestTimeout(request);
                break;
            default:
                logw("onRequestUpdated(OptionsRequest): invalid event " + event);
                break;
        }

        // End this instance if all the UceRequests in the coordinator are finished.
        checkAndFinishRequestCoordinator();
    }

    /**
     * Finish the OptionsRequest because it has encountered error.
     */
    private void handleRequestError(OptionsRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        logd("handleRequestError: " + request.toString());

        // Finish this request.
        request.onFinish();

        // Remove this request from the activated collection and notify RequestManager.
        Long taskId = request.getTaskId();
        RequestResult requestResult = sRequestErrorCreator.createRequestResult(taskId, response);
        moveRequestToFinishedCollection(taskId, requestResult);
    }

    /**
     * This method is called when the given OptionsRequest received the onCommandError callback
     * from the ImsService.
     */
    private void handleCommandError(OptionsRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        logd("handleCommandError: " + request.toString());

        // Finish this request.
        request.onFinish();

        int commandErrorCode = response.getCommandError().orElse(0);
        mUceStatsWriter.setUceEvent(mSubId, UceStatsWriter.OUTGOING_OPTION_EVENT,
            false, commandErrorCode, 0);


        // Remove this request from the activated collection and notify RequestManager.
        Long taskId = request.getTaskId();
        RequestResult requestResult = sCommandErrorCreator.createRequestResult(taskId, response);
        moveRequestToFinishedCollection(taskId, requestResult);
    }

    /**
     * This method is called when the given OptionsRequest received the onNetworkResponse
     * callback from the ImsService.
     */
    private void handleNetworkResponse(OptionsRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        logd("handleNetworkResponse: " + response.toString());

        int responseCode = response.getNetworkRespSipCode().orElse(0);
        mUceStatsWriter.setUceEvent(mSubId, UceStatsWriter.OUTGOING_OPTION_EVENT, true,
            0, responseCode);


        List<RcsContactUceCapability> updatedCapList = response.getUpdatedContactCapability();
        if (!updatedCapList.isEmpty()) {
            // Save the capabilities and trigger the capabilities callback
            mRequestManagerCallback.saveCapabilities(updatedCapList);
            triggerCapabilitiesReceivedCallback(updatedCapList);
            response.removeUpdatedCapabilities(updatedCapList);
        }

        // Finish this request.
        request.onFinish();

        // Remove this request from the activated collection and notify RequestManager.
        Long taskId = request.getTaskId();
        RequestResult requestResult = sNetworkRespCreator.createRequestResult(taskId, response);
        moveRequestToFinishedCollection(taskId, requestResult);
    }

    /**
     * This method is called when the OptionsRequest retrieves the capabilities from cache.
     */
    private void handleCachedCapabilityUpdated(OptionsRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        Long taskId = request.getTaskId();
        List<RcsContactUceCapability> cachedCapList = response.getCachedContactCapability();
        logd("handleCachedCapabilityUpdated: taskId=" + taskId + ", CapRequestResp=" + response);

        if (cachedCapList.isEmpty()) {
            return;
        }

        // Trigger the capabilities updated callback.
        triggerCapabilitiesReceivedCallback(cachedCapList);
        response.removeCachedContactCapabilities();
    }

    /**
     * This method is called when all the capabilities can be retrieved from the cached and it does
     * not need to request capabilities from the network.
     */
    private void handleNoNeedRequestFromNetwork(OptionsRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        logd("handleNoNeedRequestFromNetwork: " + response.toString());

        // Finish this request.
        request.onFinish();

        // Remove this request from the activated collection and notify RequestManager.
        long taskId = request.getTaskId();
        RequestResult requestResult = sNotNeedRequestFromNetworkCreator.createRequestResult(taskId,
                response);
        moveRequestToFinishedCollection(taskId, requestResult);
    }

    /**
     * This method is called when the framework does not receive receive the result for
     * capabilities request.
     */
    private void handleRequestTimeout(OptionsRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        logd("handleRequestTimeout: " + response.toString());

        // Finish this request.
        request.onFinish();

        // Remove this request from the activated collection and notify RequestManager.
        long taskId = request.getTaskId();
        RequestResult requestResult = sRequestTimeoutCreator.createRequestResult(taskId,
                response);
        moveRequestToFinishedCollection(taskId, requestResult);
    }

    /**
     * Trigger the capabilities updated callback.
     */
    private void triggerCapabilitiesReceivedCallback(List<RcsContactUceCapability> capList) {
        try {
            logd("triggerCapabilitiesCallback: size=" + capList.size());
            mCapabilitiesCallback.onCapabilitiesReceived(capList);
        } catch (RemoteException e) {
            logw("triggerCapabilitiesCallback exception: " + e);
        } finally {
            logd("triggerCapabilitiesCallback: done");
        }
    }

    /**
     * Trigger the onComplete callback to notify the request is completed.
     */
    private void triggerCompletedCallback() {
        try {
            logd("triggerCompletedCallback");
            mCapabilitiesCallback.onComplete();
        } catch (RemoteException e) {
            logw("triggerCompletedCallback exception: " + e);
        } finally {
            logd("triggerCompletedCallback: done");
        }
    }

    /**
     * Trigger the onError callback to notify the request is failed.
     */
    private void triggerErrorCallback(int errorCode, long retryAfterMillis) {
        try {
            logd("triggerErrorCallback: errorCode=" + errorCode + ", retry=" + retryAfterMillis);
            mCapabilitiesCallback.onError(errorCode, retryAfterMillis);
        } catch (RemoteException e) {
            logw("triggerErrorCallback exception: " + e);
        } finally {
            logd("triggerErrorCallback: done");
        }
    }

    private void checkAndFinishRequestCoordinator() {
        synchronized (mCollectionLock) {
            // Return because there are requests running.
            if (!mActivatedRequests.isEmpty()) {
                return;
            }

            // All the requests has finished, find the request which has the max retryAfter time.
            // If the result is empty, it means all the request are success.
            Optional<RequestResult> optRequestResult =
                    mFinishedRequests.values().stream()
                            .filter(result -> !result.isRequestSuccess())
                            .max(Comparator.comparingLong(result ->
                                    result.getRetryMillis().orElse(-1L)));

            // Trigger the callback
            if (optRequestResult.isPresent()) {
                RequestResult result = optRequestResult.get();
                int errorCode = result.getErrorCode().orElse(DEFAULT_ERROR_CODE);
                long retryAfter = result.getRetryMillis().orElse(0L);
                triggerErrorCallback(errorCode, retryAfter);
            } else {
                triggerCompletedCallback();
            }

            // Notify UceRequestManager to remove this instance from the collection.
            mRequestManagerCallback.notifyRequestCoordinatorFinished(mCoordinatorId);

            logd("checkAndFinishRequestCoordinator(OptionsRequest) done, id=" + mCoordinatorId);
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

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

import android.net.Uri;
import android.os.RemoteException;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsUceAdapter;
import android.telephony.ims.aidl.IRcsUceControllerCallback;

import com.android.ims.rcs.uce.UceDeviceState.DeviceStateResult;
import com.android.ims.rcs.uce.eab.EabCapabilityResult;
import com.android.ims.rcs.uce.presence.pidfparser.PidfParserUtils;
import com.android.ims.rcs.uce.request.SubscriptionTerminatedHelper.TerminatedResult;
import com.android.ims.rcs.uce.request.UceRequestManager.RequestManagerCallback;
import com.android.ims.rcs.uce.UceStatsWriter;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Responsible for the communication and interaction between SubscribeRequests and triggering
 * the callback to notify the result of the capabilities request.
 */
public class SubscribeRequestCoordinator extends UceRequestCoordinator {
    /**
     * The builder of the SubscribeRequestCoordinator.
     */
    public static final class Builder {
        private SubscribeRequestCoordinator mRequestCoordinator;

        /**
         * The builder of the SubscribeRequestCoordinator class.
         */
        public Builder(int subId, Collection<UceRequest> requests, RequestManagerCallback c) {
            mRequestCoordinator = new SubscribeRequestCoordinator(subId, requests, c,
                    UceStatsWriter.getInstance());
        }
        @VisibleForTesting
        public Builder(int subId, Collection<UceRequest> requests, RequestManagerCallback c,
                UceStatsWriter instance) {
            mRequestCoordinator = new SubscribeRequestCoordinator(subId, requests, c, instance);
        }

        /**
         * Set the callback to receive the request updated.
         */
        public Builder setCapabilitiesCallback(IRcsUceControllerCallback callback) {
            mRequestCoordinator.setCapabilitiesCallback(callback);
            return this;
        }

        /**
         * Get the SubscribeRequestCoordinator instance.
         */
        public SubscribeRequestCoordinator build() {
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
        RequestResult createRequestResult(long taskId, CapabilityRequestResponse response,
                RequestManagerCallback requestMgrCallback);
    }

    // The RequestResult creator of the request error.
    private static final RequestResultCreator sRequestErrorCreator = (taskId, response,
            requestMgrCallback) -> {
        int errorCode = response.getRequestInternalError().orElse(DEFAULT_ERROR_CODE);
        long retryAfter = response.getRetryAfterMillis();
        return RequestResult.createFailedResult(taskId, errorCode, retryAfter);
    };

    // The RequestResult creator of the command error.
    private static final RequestResultCreator sCommandErrorCreator = (taskId, response,
            requestMgrCallback) -> {
        int cmdError = response.getCommandError().orElse(COMMAND_CODE_GENERIC_FAILURE);
        int errorCode = CapabilityRequestResponse.getCapabilityErrorFromCommandError(cmdError);
        long retryAfter = response.getRetryAfterMillis();
        return RequestResult.createFailedResult(taskId, errorCode, retryAfter);
    };

    // The RequestResult creator of the network response error.
    private static final RequestResultCreator sNetworkRespErrorCreator = (taskId, response,
            requestMgrCallback) -> {
        DeviceStateResult deviceState = requestMgrCallback.getDeviceState();
        if (deviceState.isRequestForbidden()) {
            int errorCode = deviceState.getErrorCode().orElse(RcsUceAdapter.ERROR_FORBIDDEN);
            long retryAfter = deviceState.getRequestRetryAfterMillis();
            return RequestResult.createFailedResult(taskId, errorCode, retryAfter);
        } else {
            int errorCode = CapabilityRequestResponse.getCapabilityErrorFromSipCode(response);
            long retryAfter = response.getRetryAfterMillis();
            return RequestResult.createFailedResult(taskId, errorCode, retryAfter);
        }
    };

    // The RequestResult creator of the network response is not 200 OK, however, we can to treat
    // it as a successful result and finish the request
    private static final RequestResultCreator sNetworkRespSuccessfulCreator = (taskId, response,
            requestMgrCallback) -> RequestResult.createSuccessResult(taskId);

    // The RequestResult creator of the request terminated.
    private static final RequestResultCreator sTerminatedCreator = (taskId, response,
            requestMgrCallback) -> {
        // Check the given terminated reason to determine whether clients should retry or not.
        TerminatedResult terminatedResult = SubscriptionTerminatedHelper.getAnalysisResult(
                response.getTerminatedReason(), response.getRetryAfterMillis(),
                response.haveAllRequestCapsUpdatedBeenReceived());
        if (terminatedResult.getErrorCode().isPresent()) {
            // If the terminated error code is present, it means that the request is failed.
            int errorCode = terminatedResult.getErrorCode().get();
            long terminatedRetry = terminatedResult.getRetryAfterMillis();
            return RequestResult.createFailedResult(taskId, errorCode, terminatedRetry);
        } else if (!response.isNetworkResponseOK() || response.getRetryAfterMillis() > 0L) {
            // If the network response is failed or the retryAfter is not 0, this request is failed.
            long retryAfterMillis = response.getRetryAfterMillis();
            int errorCode = CapabilityRequestResponse.getCapabilityErrorFromSipCode(response);
            return RequestResult.createFailedResult(taskId, errorCode, retryAfterMillis);
        } else {
            return RequestResult.createSuccessResult(taskId);
        }
    };

    // The RequestResult creator for does not need to request from the network.
    private static final RequestResultCreator sNotNeedRequestFromNetworkCreator =
            (taskId, response, requestMgrCallback) -> RequestResult.createSuccessResult(taskId);

    // The RequestResult creator of the request timeout.
    private static final RequestResultCreator sRequestTimeoutCreator =
            (taskId, response, requestMgrCallback) -> RequestResult.createFailedResult(taskId,
                    RcsUceAdapter.ERROR_REQUEST_TIMEOUT, 0L);

    // The callback to notify the result of the capabilities request.
    private volatile IRcsUceControllerCallback mCapabilitiesCallback;

    private final UceStatsWriter mUceStatsWriter;

    private SubscribeRequestCoordinator(int subId, Collection<UceRequest> requests,
            RequestManagerCallback requestMgrCallback, UceStatsWriter instance) {
        super(subId, requests, requestMgrCallback);
        mUceStatsWriter = instance;
        logd("SubscribeRequestCoordinator: created");
    }

    private void setCapabilitiesCallback(IRcsUceControllerCallback callback) {
        mCapabilitiesCallback = callback;
    }

    @Override
    public void onFinish() {
        logd("SubscribeRequestCoordinator: onFinish");
        mCapabilitiesCallback = null;
        super.onFinish();
    }

    @Override
    public void onRequestUpdated(long taskId, @UceRequestUpdate int event) {
        if (mIsFinished) return;
        SubscribeRequest request = (SubscribeRequest) getUceRequest(taskId);
        if (request == null) {
            logw("onRequestUpdated: Cannot find SubscribeRequest taskId=" + taskId);
            return;
        }

        logd("onRequestUpdated(SubscribeRequest): taskId=" + taskId + ", event=" +
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
            case REQUEST_UPDATE_CAPABILITY_UPDATE:
                handleCapabilitiesUpdated(request);
                break;
            case REQUEST_UPDATE_RESOURCE_TERMINATED:
                handleResourceTerminated(request);
                break;
            case REQUEST_UPDATE_CACHED_CAPABILITY_UPDATE:
                handleCachedCapabilityUpdated(request);
                break;
            case REQUEST_UPDATE_TERMINATED:
                handleTerminated(request);
                break;
            case REQUEST_UPDATE_NO_NEED_REQUEST_FROM_NETWORK:
                handleNoNeedRequestFromNetwork(request);
                break;
            case REQUEST_UPDATE_TIMEOUT:
                handleRequestTimeout(request);
                break;
            default:
                logw("onRequestUpdated(SubscribeRequest): invalid event " + event);
                break;
        }

        // End this instance if all the UceRequests in the coordinator are finished.
        checkAndFinishRequestCoordinator();
    }

    /**
     * Finish the SubscribeRequest because it has encountered error.
     */
    private void handleRequestError(SubscribeRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        logd("handleRequestError: " + request.toString());

        // Finish this request.
        request.onFinish();

        // Remove this request from the activated collection and notify RequestManager.
        Long taskId = request.getTaskId();
        RequestResult requestResult = sRequestErrorCreator.createRequestResult(taskId, response,
                mRequestManagerCallback);
        moveRequestToFinishedCollection(taskId, requestResult);
    }

    /**
     * This method is called when the given SubscribeRequest received the onCommandError callback
     * from the ImsService.
     */
    private void handleCommandError(SubscribeRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        logd("handleCommandError: " + request.toString());

        // Finish this request.
        request.onFinish();

        int commandErrorCode = response.getCommandError().orElse(0);
        mUceStatsWriter.setUceEvent(mSubId, UceStatsWriter.SUBSCRIBE_EVENT,
            false, commandErrorCode, 0);

        // Remove this request from the activated collection and notify RequestManager.
        Long taskId = request.getTaskId();
        RequestResult requestResult = sCommandErrorCreator.createRequestResult(taskId, response,
                mRequestManagerCallback);
        moveRequestToFinishedCollection(taskId, requestResult);
    }

    /**
     * This method is called when the given SubscribeRequest received the onNetworkResponse
     * callback from the ImsService.
     */
    private void handleNetworkResponse(SubscribeRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        logd("handleNetworkResponse: " + response.toString());

        int respCode = response.getNetworkRespSipCode().orElse(0);
        mUceStatsWriter.setSubscribeResponse(mSubId, request.getTaskId(), respCode);

        // Refresh the device state with the request result.
        response.getResponseSipCode().ifPresent(sipCode -> {
            String reason = response.getResponseReason().orElse("");
            mRequestManagerCallback.refreshDeviceState(sipCode, reason);
        });

        // When the network response is unsuccessful, there is no subsequent callback for this
        // request. Check the forbidden state and finish this request. Otherwise, keep waiting for
        // the subsequent callback of this request.
        if (!response.isNetworkResponseOK()) {
            // Handle the network response not OK cases and get the request result to finish this
            // request.
            RequestResult requestResult = handleNetworkResponseFailed(request);

            // Trigger capabilities updated callback if there is any.
            List<RcsContactUceCapability> updatedCapList = response.getUpdatedContactCapability();
            if (!updatedCapList.isEmpty()) {
                mRequestManagerCallback.saveCapabilities(updatedCapList);
                triggerCapabilitiesReceivedCallback(updatedCapList);
                response.removeUpdatedCapabilities(updatedCapList);
            }

            // Finish this request.
            request.onFinish();

            // Remove this request from the activated collection and notify RequestManager.
            moveRequestToFinishedCollection(request.getTaskId(), requestResult);
        }
    }

    private RequestResult handleNetworkResponseFailed(SubscribeRequest request) {
        final long taskId = request.getTaskId();
        final CapabilityRequestResponse response = request.getRequestResponse();
        final List<Uri> requestUris = response.getNotReceiveCapabilityUpdatedContact();
        RequestResult requestResult = null;

        if (response.isNotFound()) {
            // In the network response with the not found case, we won't receive the capabilities
            // updated callback from the ImsService afterward. Therefore, we create the capabilities
            // with the result REQUEST_RESULT_NOT_FOUND by ourself and will trigger the
            // capabilities received callback to the clients later.
            List<RcsContactUceCapability> capabilityList = requestUris.stream().map(uri ->
                    PidfParserUtils.getNotFoundContactCapabilities(uri))
                    .collect(Collectors.toList());
            response.addUpdatedCapabilities(capabilityList);

            // We treat the NOT FOUND is a successful result.
            requestResult = sNetworkRespSuccessfulCreator.createRequestResult(taskId, response,
                    mRequestManagerCallback);
        } else {
            // The request result is unsuccessful and it's not the NOT FOUND error. we need to get
            // the capabilities from the cache.
            List<RcsContactUceCapability> capabilitiesList =
                    getCapabilitiesFromCacheIncludingExpired(requestUris);
            response.addUpdatedCapabilities(capabilitiesList);

            // Add to the throttling list for the inconclusive result of the contacts.
            mRequestManagerCallback.addToThrottlingList(requestUris,
                    response.getResponseSipCode().orElse(
                            com.android.ims.rcs.uce.util.NetworkSipCode.SIP_CODE_REQUEST_TIMEOUT));

            requestResult = sNetworkRespErrorCreator.createRequestResult(taskId, response,
                    mRequestManagerCallback);
        }
        return requestResult;
    }

    /**
     * Get the contact capabilities from the cache even if the capabilities have expired. If the
     * capabilities doesn't exist, create the non-RCS capabilities instead.
     * @param uris the uris to get the capabilities from cache.
     * @return The contact capabilities for the given uris.
     */
    private List<RcsContactUceCapability> getCapabilitiesFromCacheIncludingExpired(List<Uri> uris) {
        List<RcsContactUceCapability> resultList = new ArrayList<>();
        List<RcsContactUceCapability> notFoundFromCacheList = new ArrayList<>();

        // Get the capabilities from the cache.
        List<EabCapabilityResult> eabResultList =
                mRequestManagerCallback.getCapabilitiesFromCacheIncludingExpired(uris);

        eabResultList.forEach(eabResult -> {
            if (eabResult.getStatus() == EabCapabilityResult.EAB_QUERY_SUCCESSFUL ||
                eabResult.getStatus() == EabCapabilityResult.EAB_CONTACT_EXPIRED_FAILURE) {
                // The capabilities are found, add to the result list
                resultList.add(eabResult.getContactCapabilities());
            } else {
                // Cannot get the capabilities from cache, create the non-RCS capabilities instead.
                notFoundFromCacheList.add(PidfParserUtils.getNotFoundContactCapabilities(
                        eabResult.getContact()));
            }
        });

        if (!notFoundFromCacheList.isEmpty()) {
            resultList.addAll(notFoundFromCacheList);
        }

        logd("getCapabilitiesFromCacheIncludingExpired: requesting uris size=" + uris.size() +
                ", capabilities not found from cache size=" + notFoundFromCacheList.size());
        return resultList;
    }

    /**
     * This method is called when the given SubscribeRequest received the onNotifyCapabilitiesUpdate
     * callback from the ImsService.
     */
    private void handleCapabilitiesUpdated(SubscribeRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        Long taskId = request.getTaskId();
        List<RcsContactUceCapability> updatedCapList = response.getUpdatedContactCapability();
        logd("handleCapabilitiesUpdated: taskId=" + taskId + ", size=" + updatedCapList.size());

        if (updatedCapList.isEmpty()) {
            return;
        }

        mUceStatsWriter.setPresenceNotifyEvent(mSubId, taskId, updatedCapList);
        // Save the updated capabilities to the cache.
        mRequestManagerCallback.saveCapabilities(updatedCapList);

        // Trigger the capabilities updated callback and remove the given capabilities that have
        // executed the callback onCapabilitiesReceived.
        triggerCapabilitiesReceivedCallback(updatedCapList);
        response.removeUpdatedCapabilities(updatedCapList);
    }

    /**
     * This method is called when the given SubscribeRequest received the onResourceTerminated
     * callback from the ImsService.
     */
    private void handleResourceTerminated(SubscribeRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        Long taskId = request.getTaskId();
        List<RcsContactUceCapability> terminatedResources = response.getTerminatedResources();
        logd("handleResourceTerminated: taskId=" + taskId + ", size=" + terminatedResources.size());

        if (terminatedResources.isEmpty()) {
            return;
        }

        mUceStatsWriter.setPresenceNotifyEvent(mSubId, taskId, terminatedResources);

        // Save the terminated capabilities to the cache.
        mRequestManagerCallback.saveCapabilities(terminatedResources);

        // Trigger the capabilities updated callback and remove the given capabilities from the
        // resource terminated list.
        triggerCapabilitiesReceivedCallback(terminatedResources);
        response.removeTerminatedResources(terminatedResources);
    }

    /**
     * This method is called when the given SubscribeRequest retrieve the cached capabilities.
     */
    private void handleCachedCapabilityUpdated(SubscribeRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        Long taskId = request.getTaskId();
        List<RcsContactUceCapability> cachedCapList = response.getCachedContactCapability();
        logd("handleCachedCapabilityUpdated: taskId=" + taskId + ", size=" + cachedCapList.size());

        if (cachedCapList.isEmpty()) {
            return;
        }

        // Trigger the capabilities updated callback.
        triggerCapabilitiesReceivedCallback(cachedCapList);
        response.removeCachedContactCapabilities();
    }

    /**
     * This method is called when the given SubscribeRequest received the onTerminated callback
     * from the ImsService.
     */
    private void handleTerminated(SubscribeRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        logd("handleTerminated: " + response.toString());

        // Finish this request.
        request.onFinish();

        // Remove this request from the activated collection and notify RequestManager.
        Long taskId = request.getTaskId();
        mUceStatsWriter.setSubscribeTerminated(mSubId, taskId, response.getTerminatedReason());
        RequestResult requestResult = sTerminatedCreator.createRequestResult(taskId, response,
                mRequestManagerCallback);
        moveRequestToFinishedCollection(taskId, requestResult);
    }

    /**
     * This method is called when all the capabilities can be retrieved from the cached and it does
     * not need to request capabilities from the network.
     */
    private void handleNoNeedRequestFromNetwork(SubscribeRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        logd("handleNoNeedRequestFromNetwork: " + response.toString());

        // Finish this request.
        request.onFinish();

        // Remove this request from the activated collection and notify RequestManager.
        long taskId = request.getTaskId();
        RequestResult requestResult = sNotNeedRequestFromNetworkCreator.createRequestResult(taskId,
                response, mRequestManagerCallback);
        moveRequestToFinishedCollection(taskId, requestResult);
    }

    /**
     * This method is called when the framework did not receive the capabilities request result.
     */
    private void handleRequestTimeout(SubscribeRequest request) {
        CapabilityRequestResponse response = request.getRequestResponse();
        List<Uri> requestUris = response.getNotReceiveCapabilityUpdatedContact();
        logd("handleRequestTimeout: " + response);
        logd("handleRequestTimeout: not received updated uri size=" + requestUris.size());

        // Add to the throttling list for the inconclusive result of the contacts.
        mRequestManagerCallback.addToThrottlingList(requestUris,
                com.android.ims.rcs.uce.util.NetworkSipCode.SIP_CODE_REQUEST_TIMEOUT);

        // Get the capabilities from the cache instead and add to the response.
        List<RcsContactUceCapability> capabilitiesList =
                getCapabilitiesFromCacheIncludingExpired(requestUris);
        response.addUpdatedCapabilities(capabilitiesList);

        // Trigger capabilities updated callback if there is any.
        List<RcsContactUceCapability> updatedCapList = response.getUpdatedContactCapability();
        if (!updatedCapList.isEmpty()) {
            triggerCapabilitiesReceivedCallback(updatedCapList);
            response.removeUpdatedCapabilities(updatedCapList);
        }

        // Remove this request from the activated collection and notify RequestManager.
        long taskId = request.getTaskId();
        RequestResult requestResult = sRequestTimeoutCreator.createRequestResult(taskId,
                response, mRequestManagerCallback);

        // Finish this request
        request.onFinish();

        // Remove this request from the activated collection and notify RequestManager.
        moveRequestToFinishedCollection(taskId, requestResult);
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

            logd("checkAndFinishRequestCoordinator(SubscribeRequest) done, id=" + mCoordinatorId);
        }
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

    @VisibleForTesting
    public Collection<UceRequest> getActivatedRequest() {
        return mActivatedRequests.values();
    }

    @VisibleForTesting
    public Collection<RequestResult> getFinishedRequest() {
        return mFinishedRequests.values();
    }
}

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

import android.net.Uri;
import android.telephony.ims.RcsUceAdapter;
import android.telephony.ims.RcsContactUceCapability;
import android.util.Log;

import com.android.ims.rcs.uce.UceDeviceState.DeviceStateResult;
import com.android.ims.rcs.uce.eab.EabCapabilityResult;
import com.android.ims.rcs.uce.presence.pidfparser.PidfParserUtils;
import com.android.ims.rcs.uce.request.UceRequestManager.RequestManagerCallback;
import com.android.ims.rcs.uce.util.UceUtils;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The base class of the UCE request to request the capabilities from the carrier network.
 */
public abstract class CapabilityRequest implements UceRequest {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "CapabilityRequest";

    protected final int mSubId;
    protected final long mTaskId;
    protected final List<Uri> mUriList;
    protected final @UceRequestType int mRequestType;
    protected final RequestManagerCallback mRequestManagerCallback;
    protected final CapabilityRequestResponse mRequestResponse;

    protected volatile long mCoordinatorId;
    protected volatile boolean mIsFinished;
    protected volatile boolean mSkipGettingFromCache;

    public CapabilityRequest(int subId, @UceRequestType int type, RequestManagerCallback callback) {
        mSubId = subId;
        mRequestType = type;
        mUriList = new ArrayList<>();
        mRequestManagerCallback = callback;
        mRequestResponse = new CapabilityRequestResponse();
        mTaskId = UceUtils.generateTaskId();
    }

    @VisibleForTesting
    public CapabilityRequest(int subId, @UceRequestType int type, RequestManagerCallback callback,
            CapabilityRequestResponse requestResponse) {
        mSubId = subId;
        mRequestType = type;
        mUriList = new ArrayList<>();
        mRequestManagerCallback = callback;
        mRequestResponse = requestResponse;
        mTaskId = UceUtils.generateTaskId();
    }

    @Override
    public void setRequestCoordinatorId(long coordinatorId) {
        mCoordinatorId = coordinatorId;
    }

    @Override
    public long getRequestCoordinatorId() {
        return mCoordinatorId;
    }

    @Override
    public long getTaskId() {
        return mTaskId;
    }

    @Override
    public void onFinish() {
        mIsFinished = true;
        // Remove the timeout timer of this request
        mRequestManagerCallback.removeRequestTimeoutTimer(mTaskId);
    }

    @Override
    public void setContactUri(List<Uri> uris) {
        mUriList.addAll(uris);
        mRequestResponse.setRequestContacts(uris);
    }

    public List<Uri> getContactUri() {
        return Collections.unmodifiableList(mUriList);
    }

    /**
     * Set to check if this request should be getting the capabilities from the cache. The flag is
     * set when the request is triggered by the capability polling service. The contacts from the
     * capability polling service are already expired, skip checking from the cache.
     */
    public void setSkipGettingFromCache(boolean skipFromCache) {
        mSkipGettingFromCache = skipFromCache;
    }

    /**
     * Return if the capabilities request should skip getting from the cache. The flag is set when
     * the request is triggered by the capability polling service and the request doesn't need to
     * check the cache again.
     */
    private boolean isSkipGettingFromCache() {
        return mSkipGettingFromCache;
    }

    /**
     * @return The RequestResponse instance associated with this request.
     */
    public CapabilityRequestResponse getRequestResponse() {
        return mRequestResponse;
    }

    /**
     * Start executing this request.
     */
    @Override
    public void executeRequest() {
        // Return if this request is not allowed to be executed.
        if (!isRequestAllowed()) {
            logd("executeRequest: The request is not allowed.");
            mRequestManagerCallback.notifyRequestError(mCoordinatorId, mTaskId);
            return;
        }

        // Get the contact capabilities from the cache including the expired capabilities.
        final List<EabCapabilityResult> eabResultList = getCapabilitiesFromCache();

        // Get all the unexpired capabilities from the EAB result list and add to the response.
        final List<RcsContactUceCapability> cachedCapList = isSkipGettingFromCache() ?
                Collections.EMPTY_LIST : getUnexpiredCapabilities(eabResultList);
        mRequestResponse.addCachedCapabilities(cachedCapList);

        logd("executeRequest: cached capabilities size=" + cachedCapList.size());

        // Get the rest contacts which are not in the cache or has expired.
        final List<Uri> expiredUris = getRequestingFromNetworkUris(cachedCapList);

        // For those uris that are not in the cache or have expired, we should request their
        // capabilities from the network. However, we still need to check whether these contacts
        // are in the throttling list. If the contact is in the throttling list, even if it has
        // expired, we will get the cached capabilities.
        final List<RcsContactUceCapability> throttlingUris =
                getFromThrottlingList(expiredUris, eabResultList);
        mRequestResponse.addCachedCapabilities(throttlingUris);

        logd("executeRequest: contacts in throttling list size=" + throttlingUris.size());

        // Notify that the cached capabilities are updated.
        if (!cachedCapList.isEmpty() || !throttlingUris.isEmpty()) {
            mRequestManagerCallback.notifyCachedCapabilitiesUpdated(mCoordinatorId, mTaskId);
        }

        // Get the rest contacts which need to request capabilities from the network.
        List<Uri> requestCapUris = getRequestingFromNetworkUris(cachedCapList, throttlingUris);

        logd("executeRequest: requestCapUris size=" + requestCapUris.size());

        // Notify that it doesn't need to request capabilities from the network when all the
        // requested capabilities can be retrieved from cache. Otherwise, it needs to request
        // capabilities from the network for those contacts which cannot retrieve capabilities from
        // the cache.
        if (requestCapUris.isEmpty()) {
            mRequestManagerCallback.notifyNoNeedRequestFromNetwork(mCoordinatorId, mTaskId);
        } else {
            requestCapabilities(requestCapUris);
        }
    }

    // Check whether this request is allowed to be executed or not.
    private boolean isRequestAllowed() {
        if (mUriList == null || mUriList.isEmpty()) {
            logw("isRequestAllowed: uri is empty");
            mRequestResponse.setRequestInternalError(RcsUceAdapter.ERROR_GENERIC_FAILURE);
            return false;
        }

        if (mIsFinished) {
            logw("isRequestAllowed: This request is finished");
            mRequestResponse.setRequestInternalError(RcsUceAdapter.ERROR_GENERIC_FAILURE);
            return false;
        }

        DeviceStateResult deviceStateResult = mRequestManagerCallback.getDeviceState();
        if (deviceStateResult.isRequestForbidden()) {
            logw("isRequestAllowed: The device is disallowed.");
            mRequestResponse.setRequestInternalError(
                    deviceStateResult.getErrorCode().orElse(RcsUceAdapter.ERROR_GENERIC_FAILURE));
            return false;
        }
        return true;
    }

    // Get the cached capabilities by the given request type.
    private List<EabCapabilityResult> getCapabilitiesFromCache() {
        List<EabCapabilityResult> resultList = null;
        if (mRequestType == REQUEST_TYPE_CAPABILITY) {
            resultList = mRequestManagerCallback.getCapabilitiesFromCacheIncludingExpired(mUriList);
        } else if (mRequestType == REQUEST_TYPE_AVAILABILITY) {
            // Always get the first element if the request type is availability.
            Uri uri = mUriList.get(0);
            EabCapabilityResult eabResult =
                    mRequestManagerCallback.getAvailabilityFromCacheIncludingExpired(uri);
            resultList = new ArrayList<>();
            resultList.add(eabResult);
        }
        if (resultList == null) {
            return Collections.emptyList();
        }
        return resultList;
    }

    /**
     * Get the unexpired contact capabilities from the given EAB result list.
     * @param list the query result from the EAB
     */
    private List<RcsContactUceCapability> getUnexpiredCapabilities(List<EabCapabilityResult> list) {
        return list.stream()
                .filter(Objects::nonNull)
                .filter(result -> result.getStatus() == EabCapabilityResult.EAB_QUERY_SUCCESSFUL)
                .map(EabCapabilityResult::getContactCapabilities)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get the contact uris which cannot retrieve capabilities from the cache.
     * @param cachedCapList The capabilities which are already stored in the cache.
     */
    private List<Uri> getRequestingFromNetworkUris(List<RcsContactUceCapability> cachedCapList) {
        return mUriList.stream()
                .filter(uri -> cachedCapList.stream()
                        .noneMatch(cap -> cap.getContactUri().equals(uri)))
                        .collect(Collectors.toList());
    }

    /**
     * Get the contact uris which cannot retrieve capabilities from the cache.
     * @param cachedCapList The capabilities which are already stored in the cache.
     * @param throttlingUris The capabilities which are in the throttling list.
     */
    private List<Uri> getRequestingFromNetworkUris(List<RcsContactUceCapability> cachedCapList,
            List<RcsContactUceCapability> throttlingUris) {
        // We won't request the network query for those contacts in the cache and in the
        // throttling list. Merging the two list and get the rest contact uris.
        List<RcsContactUceCapability> notNetworkQueryList = new ArrayList<>(cachedCapList);
        notNetworkQueryList.addAll(throttlingUris);
        return getRequestingFromNetworkUris(notNetworkQueryList);
    }

    /**
     * Get the contact capabilities for those uri are in the throttling list. If the contact uri is
     * in the throttling list, the capabilities will be retrieved from cache even if it has expired.
     * If the capabilities cannot be found, return the non-RCS contact capabilities instead.
     * @param expiredUris the expired/unknown uris to check whether are in the throttling list
     * @return the contact capabilities for the uris are in the throttling list
     */
    private List<RcsContactUceCapability> getFromThrottlingList(final List<Uri> expiredUris,
            final List<EabCapabilityResult> eabResultList) {
        List<RcsContactUceCapability> resultList = new ArrayList<>();
        List<RcsContactUceCapability> notFoundFromCacheList = new ArrayList<>();

        // Retrieve the uris put in the throttling list from the expired/unknown contacts.
        List<Uri> throttlingUris = mRequestManagerCallback.getInThrottlingListUris(expiredUris);

        // For these uris in the throttling list, check whether their capabilities are in the cache.
        List<EabCapabilityResult> throttlingUriFoundInEab = new ArrayList<>();
        for (Uri uri : throttlingUris) {
            for (EabCapabilityResult eabResult : eabResultList) {
                if (eabResult.getContact().equals(uri)) {
                    throttlingUriFoundInEab.add(eabResult);
                    break;
                }
            }
        }

        throttlingUriFoundInEab.forEach(eabResult -> {
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

        logd("getFromThrottlingList: requesting uris in the list size=" + throttlingUris.size() +
                ", generate non-RCS size=" + notFoundFromCacheList.size());
        return resultList;
    }

    /**
     * Set the timeout timer of this request.
     */
    protected void setupRequestTimeoutTimer() {
        long timeoutAfterMs = UceUtils.getCapRequestTimeoutAfterMillis();
        logd("setupRequestTimeoutTimer(ms): " + timeoutAfterMs);
        mRequestManagerCallback.setRequestTimeoutTimer(mCoordinatorId, mTaskId, timeoutAfterMs);
    }

    /*
     * Requests capabilities from IMS. The inherited request is required to override this method
     * to define the behavior of requesting capabilities.
     */
    protected abstract void requestCapabilities(List<Uri> requestCapUris);

    protected void logd(String log) {
        Log.d(LOG_TAG, getLogPrefix().append(log).toString());
    }

    protected void logw(String log) {
        Log.w(LOG_TAG, getLogPrefix().append(log).toString());
    }

    protected void logi(String log) {
        Log.i(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private StringBuilder getLogPrefix() {
        StringBuilder builder = new StringBuilder("[");
        builder.append(mSubId).append("][taskId=").append(mTaskId).append("] ");
        return builder;
    }
}

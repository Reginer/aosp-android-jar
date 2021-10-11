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

import static android.telephony.ims.RcsContactUceCapability.CAPABILITY_MECHANISM_OPTIONS;
import static android.telephony.ims.RcsContactUceCapability.SOURCE_TYPE_NETWORK;

import android.net.Uri;
import android.telephony.ims.RcsContactUceCapability;
import android.util.Log;

import com.android.ims.rcs.uce.request.UceRequestManager.RequestManagerCallback;
import com.android.ims.rcs.uce.util.FeatureTags;
import com.android.ims.rcs.uce.util.NetworkSipCode;
import com.android.ims.rcs.uce.util.UceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Handle the OPTIONS request from the network.
 */
public class RemoteOptionsRequest implements UceRequest {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "RemoteOptRequest";

    /**
     * The response of the remote capability request.
     */
    public static class RemoteOptResponse {
        private boolean mIsNumberBlocked;
        private RcsContactUceCapability mRcsContactCapability;
        private Optional<Integer> mErrorSipCode;
        private Optional<String> mErrorReason;

        public RemoteOptResponse() {
            mErrorSipCode = Optional.empty();
            mErrorReason = Optional.empty();
        }

        void setRespondToRequest(RcsContactUceCapability capability, boolean isBlocked) {
            mIsNumberBlocked = isBlocked;
            mRcsContactCapability = capability;
        }

        void setRespondToRequestWithError(int code, String reason) {
            mErrorSipCode = Optional.of(code);
            mErrorReason = Optional.of(reason);
        }

        public boolean isNumberBlocked() {
            return mIsNumberBlocked;
        }

        public RcsContactUceCapability getRcsContactCapability() {
            return mRcsContactCapability;
        }

        public Optional<Integer> getErrorSipCode() {
            return mErrorSipCode;
        }

        public Optional<String> getErrorReason() {
            return mErrorReason;
        }
    }

    private final int mSubId;
    private final long mTaskId;
    private volatile long mCoordinatorId;
    private volatile boolean mIsFinished;
    private volatile boolean mIsRemoteNumberBlocked;

    private List<Uri> mUriList;
    private final List<String> mRemoteFeatureTags;
    private final RemoteOptResponse mRemoteOptResponse;
    private final RequestManagerCallback mRequestManagerCallback;

    public RemoteOptionsRequest(int subId, RequestManagerCallback requestMgrCallback) {
        mSubId = subId;
        mTaskId = UceUtils.generateTaskId();
        mRemoteFeatureTags = new ArrayList<>();
        mRemoteOptResponse = new RemoteOptResponse();
        mRequestManagerCallback = requestMgrCallback;
        logd("created");
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
    }

    @Override
    public void setContactUri(List<Uri> uris) {
        mUriList = uris;
    }

    public void setRemoteFeatureTags(List<String> remoteFeatureTags) {
        remoteFeatureTags.forEach(mRemoteFeatureTags::add);
    }

    public void setIsRemoteNumberBlocked(boolean isBlocked) {
        mIsRemoteNumberBlocked = isBlocked;
    }

    /**
     * @return The response of this request.
     */
    public RemoteOptResponse getRemoteOptResponse() {
        return mRemoteOptResponse;
    }

    @Override
    public void executeRequest() {
        logd("executeRequest");
        try {
            executeRequestInternal();
        } catch (Exception e) {
            logw("executeRequest: exception " + e);
            setResponseWithError(NetworkSipCode.SIP_CODE_SERVER_INTERNAL_ERROR,
                    NetworkSipCode.SIP_INTERNAL_SERVER_ERROR);
        } finally {
            mRequestManagerCallback.notifyRemoteRequestDone(mCoordinatorId, mTaskId);
        }
    }

    private void executeRequestInternal() {
        if (mUriList == null || mUriList.isEmpty()) {
            logw("executeRequest: uri is empty");
            setResponseWithError(NetworkSipCode.SIP_CODE_BAD_REQUEST,
                    NetworkSipCode.SIP_BAD_REQUEST);
            return;
        }

        if (mIsFinished) {
            logw("executeRequest: This request is finished");
            setResponseWithError(NetworkSipCode.SIP_CODE_SERVICE_UNAVAILABLE,
                    NetworkSipCode.SIP_SERVICE_UNAVAILABLE);
            return;
        }

        // Store the remote capabilities
        Uri contactUri = mUriList.get(0);
        RcsContactUceCapability remoteCaps = FeatureTags.getContactCapability(contactUri,
                SOURCE_TYPE_NETWORK, mRemoteFeatureTags);
        mRequestManagerCallback.saveCapabilities(Collections.singletonList(remoteCaps));

        // Get the device's capabilities and trigger the request callback
        RcsContactUceCapability deviceCaps = mRequestManagerCallback.getDeviceCapabilities(
                CAPABILITY_MECHANISM_OPTIONS);
        if (deviceCaps == null) {
            logw("executeRequest: The device's capabilities is empty");
            setResponseWithError(NetworkSipCode.SIP_CODE_SERVER_INTERNAL_ERROR,
                    NetworkSipCode.SIP_INTERNAL_SERVER_ERROR);
        } else {
            logd("executeRequest: Respond to capability request, blocked="
                    + mIsRemoteNumberBlocked);
            setResponse(deviceCaps, mIsRemoteNumberBlocked);
        }
    }

    private void setResponse(RcsContactUceCapability deviceCaps,
            boolean isRemoteNumberBlocked) {
        mRemoteOptResponse.setRespondToRequest(deviceCaps, isRemoteNumberBlocked);
    }

    private void setResponseWithError(int errorCode, String reason) {
        mRemoteOptResponse.setRespondToRequestWithError(errorCode, reason);
    }

    private void logd(String log) {
        Log.d(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private void logw(String log) {
        Log.d(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private StringBuilder getLogPrefix() {
        StringBuilder builder = new StringBuilder("[");
        builder.append(mSubId).append("][taskId=").append(mTaskId).append("] ");
        return builder;
    }
}

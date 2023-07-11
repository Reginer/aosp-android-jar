/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.ims.rcs.uce.presence.publish;

import android.annotation.Nullable;
import android.telephony.ims.RcsUceAdapter;
import android.telephony.ims.aidl.IPublishResponseCallback;
import android.telephony.ims.stub.RcsCapabilityExchangeImplBase;
import android.text.TextUtils;
import android.util.Log;

import com.android.ims.rcs.uce.presence.publish.PublishController.PublishControllerCallback;
import com.android.ims.rcs.uce.util.NetworkSipCode;
import com.android.ims.rcs.uce.util.UceUtils;

import java.time.Instant;
import java.util.Optional;

/**
 * Receiving the result callback of the publish request.
 */
public class PublishRequestResponse {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "PublishRequestResp";

    private final long mTaskId;
    private final String mPidfXml;
    private volatile boolean mNeedRetry;
    private volatile PublishControllerCallback mPublishCtrlCallback;

    private Optional<Integer> mCmdErrorCode;
    private Optional<Integer> mNetworkRespSipCode;
    private Optional<String> mReasonPhrase;
    private Optional<Integer> mReasonHeaderCause;
    private Optional<String> mReasonHeaderText;

    // The timestamp when receive the response from the network.
    private Instant mResponseTimestamp;

    public PublishRequestResponse(PublishControllerCallback publishCtrlCallback, long taskId,
            String pidfXml) {
        mTaskId = taskId;
        mPidfXml = pidfXml;
        mPublishCtrlCallback = publishCtrlCallback;
        mCmdErrorCode = Optional.empty();
        mNetworkRespSipCode = Optional.empty();
        mReasonPhrase = Optional.empty();
        mReasonHeaderCause = Optional.empty();
        mReasonHeaderText = Optional.empty();
    }

    public PublishRequestResponse(String pidfXml, int sipCode, String reasonPhrase,
            int reasonHeaderCause, String reasonHeaderText) {
        mTaskId = 0L;
        mPublishCtrlCallback = null;
        mCmdErrorCode = Optional.empty();

        mPidfXml = pidfXml;
        mResponseTimestamp = Instant.now();
        mNetworkRespSipCode = Optional.of(sipCode);
        mReasonPhrase = Optional.ofNullable(reasonPhrase);
        if (reasonHeaderCause != 0) {
            mReasonHeaderCause = Optional.of(reasonHeaderCause);
        } else {
            mReasonHeaderCause = Optional.empty();
        }
        if (TextUtils.isEmpty(reasonHeaderText)) {
            mReasonHeaderText = Optional.empty();
        } else {
            mReasonHeaderText = Optional.ofNullable(reasonHeaderText);
        }

    }

    // The result callback of the publish capability request.
    private IPublishResponseCallback mResponseCallback = new IPublishResponseCallback.Stub() {
        @Override
        public void onCommandError(int code) {
            PublishRequestResponse.this.onCommandError(code);
        }

        @Override
        public void onNetworkResponse(int code, String reason) {
            PublishRequestResponse.this.onNetworkResponse(code, reason);
        }

        @Override
        public void onNetworkRespHeader(int code, String reasonPhrase, int reasonHeaderCause,
                String reasonHeaderText) {
            PublishRequestResponse.this.onNetworkResponse(code, reasonPhrase, reasonHeaderCause,
                    reasonHeaderText);
        }
    };

    public IPublishResponseCallback getResponseCallback() {
        return mResponseCallback;
    }

    public long getTaskId() {
        return mTaskId;
    }

    /**
     * Retrieve the command error code which received from the network.
     */
    public Optional<Integer> getCmdErrorCode() {
        return mCmdErrorCode;
    }

    /**
     * Retrieve the network response sip code which received from the network.
     */
    public Optional<Integer> getNetworkRespSipCode() {
        return mNetworkRespSipCode;
    }

    /**
     * Retrieve the reason phrase of the network response which received from the network.
     */
    public Optional<String> getReasonPhrase() {
        return mReasonPhrase;
    }

    /**
     * Retrieve the reason header from the network response.
     */
    public Optional<Integer> getReasonHeaderCause() {
        return mReasonHeaderCause;
    }

    /**
     * Retrieve the description of the reason header.
     */
    public Optional<String> getReasonHeaderText() {
        return mReasonHeaderText;
    }

    /**
     * Retrieve the SIP code from the network response. It will get the value from the Reason
     * Header first. If the ReasonHeader is not present, it will get the value from the Network
     * response instead.
     */
    public Optional<Integer> getResponseSipCode() {
        return (mReasonHeaderCause.isPresent()) ? mReasonHeaderCause : mNetworkRespSipCode;
    }

    /**
     * Retrieve the REASON from the network response. It will get the value from the Reason Header
     * first. If the ReasonHeader is not present, it will get the value from the Network response
     * instead.
     */
    public Optional<String> getResponseReason() {
        return (mReasonHeaderText.isPresent()) ? mReasonHeaderText : mReasonPhrase;
    }

    /**
     * Get the timestamp of receiving the network response callback.
     */
    public @Nullable Instant getResponseTimestamp() {
        return mResponseTimestamp;
    }

    /**
     * @return the PIDF XML sent during this request.
     */
    public String getPidfXml() {
        return mPidfXml;
    }

    public void onDestroy() {
        mPublishCtrlCallback = null;
    }

    private void onCommandError(int errorCode) {
        mResponseTimestamp = Instant.now();
        mCmdErrorCode = Optional.of(errorCode);
        updateRetryFlagByCommandError();

        PublishControllerCallback ctrlCallback = mPublishCtrlCallback;
        if (ctrlCallback != null) {
            ctrlCallback.onRequestCommandError(this);
        } else {
            Log.d(LOG_TAG, "onCommandError: already destroyed. error code=" + errorCode);
        }
    }

    private void onNetworkResponse(int sipCode, String reason) {
        // When we send a request to PUBLISH and there is no change to the UCE capabilities, we
        // expected onCommandError() with COMMAND_CODE_NO_CHANGE.
        // But some of the vendor will instead send SIP code 999.
        if (sipCode == 999) {
            onCommandError(RcsCapabilityExchangeImplBase.COMMAND_CODE_NO_CHANGE);
            return;
        }
        mResponseTimestamp = Instant.now();
        mNetworkRespSipCode = Optional.of(sipCode);
        mReasonPhrase = Optional.ofNullable(reason);
        updateRetryFlagByNetworkResponse();

        PublishControllerCallback ctrlCallback = mPublishCtrlCallback;
        if (ctrlCallback != null) {
            ctrlCallback.onRequestNetworkResp(this);
        } else {
            Log.d(LOG_TAG, "onNetworkResponse: already destroyed. sip code=" + sipCode);
        }
    }

    private void onNetworkResponse(int sipCode, String reasonPhrase, int reasonHeaderCause,
            String reasonHeaderText) {
        // When we send a request to PUBLISH and there is no change to the UCE capabilities, we
        // expected onCommandError() with COMMAND_CODE_NO_CHANGE.
        // But some of the vendor will instead send SIP code 999.
        if (sipCode == 999) {
            onCommandError(RcsCapabilityExchangeImplBase.COMMAND_CODE_NO_CHANGE);
            return;
        }
        mResponseTimestamp = Instant.now();
        mNetworkRespSipCode = Optional.of(sipCode);
        mReasonPhrase = Optional.ofNullable(reasonPhrase);
        mReasonHeaderCause = Optional.of(reasonHeaderCause);
        mReasonHeaderText = Optional.ofNullable(reasonHeaderText);
        updateRetryFlagByNetworkResponse();

        PublishControllerCallback ctrlCallback = mPublishCtrlCallback;
        if (ctrlCallback != null) {
            ctrlCallback.onRequestNetworkResp(this);
        } else {
            Log.d(LOG_TAG, "onNetworkResponse: already destroyed. sipCode=" + sipCode +
                    ", reasonHeader=" + reasonHeaderCause);
        }
    }

    private void updateRetryFlagByCommandError() {
        switch(getCmdErrorCode().orElse(-1)) {
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_REQUEST_TIMEOUT:
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_INSUFFICIENT_MEMORY:
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_LOST_NETWORK_CONNECTION:
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_SERVICE_UNAVAILABLE:
                mNeedRetry = true;
                break;
        }
    }

    private void updateRetryFlagByNetworkResponse() {
        // Disable retry flag because the retry mechanism is implemented in the ImsService.
        mNeedRetry = false;
    }

    /*
     * Check whether the publishing request is successful.
     */
    public boolean isRequestSuccess() {
        if (isCommandError()) {
            return false;
        }
        // The result of the request was treated as successful if the command error code is present
        // and its value is COMMAND_CODE_NO_CHANGE.
        if (isCommandCodeNoChange()) {
            return true;
        }

        final int sipCodeOk = NetworkSipCode.SIP_CODE_OK;
        if (getNetworkRespSipCode().filter(c -> c == sipCodeOk).isPresent() &&
                (!getReasonHeaderCause().isPresent()
                        || getReasonHeaderCause().filter(c -> c == sipCodeOk).isPresent())) {
            return true;
        }
        return false;
    }

    /**
     * Check if the PUBLISH request is failed with receiving the command error.
     * @return true if the command is failure.
     */
    private boolean isCommandError() {
        // The request is failed if the command error code is present and its value is not
        // COMMAND_CODE_NO_CHANGE.
        if (getCmdErrorCode().isPresent() && !isCommandCodeNoChange()) {
            return true;
        }
        return false;
    }

    // @return true If it received the command code COMMAND_CODE_NO_CHANGE
    private boolean isCommandCodeNoChange() {
        if (getCmdErrorCode().filter(code ->
                code == RcsCapabilityExchangeImplBase.COMMAND_CODE_NO_CHANGE).isPresent()) {
            return true;
        }
        return false;
    }

    /**
     * Check whether the publishing request needs to be retried.
     */
    public boolean needRetry() {
        return mNeedRetry;
    }

    /**
     * @return The publish state when the publish request is finished.
     */
     public int getPublishState() {
         if (isCommandError()) {
             return getPublishStateByCmdErrorCode();
         } else {
             return getPublishStateByNetworkResponse();
         }
     }

    /**
     * Convert the command error code to the publish state
     */
    private int getPublishStateByCmdErrorCode() {
        if (getCmdErrorCode().orElse(-1) ==
                RcsCapabilityExchangeImplBase.COMMAND_CODE_REQUEST_TIMEOUT) {
            return RcsUceAdapter.PUBLISH_STATE_REQUEST_TIMEOUT;
        }
        return RcsUceAdapter.PUBLISH_STATE_OTHER_ERROR;
    }

    /**
     * Convert the network sip code to the publish state
     */
    private int getPublishStateByNetworkResponse() {
        int respSipCode;
        if (isCommandCodeNoChange()) {
            // If the command code is COMMAND_CODE_NO_CHANGE, it should be treated as successful.
            respSipCode = NetworkSipCode.SIP_CODE_OK;
        } else if (getReasonHeaderCause().isPresent()) {
            respSipCode = getReasonHeaderCause().get();
        } else {
            respSipCode = getNetworkRespSipCode().orElse(-1);
        }

        switch (respSipCode) {
            case NetworkSipCode.SIP_CODE_OK:
                return RcsUceAdapter.PUBLISH_STATE_OK;
            case NetworkSipCode.SIP_CODE_FORBIDDEN:
            case NetworkSipCode.SIP_CODE_NOT_FOUND:
            case NetworkSipCode.SIP_CODE_SERVER_TIMEOUT:
                return RcsUceAdapter.PUBLISH_STATE_RCS_PROVISION_ERROR;
            case NetworkSipCode.SIP_CODE_REQUEST_TIMEOUT:
                return RcsUceAdapter.PUBLISH_STATE_REQUEST_TIMEOUT;
            default:
                return RcsUceAdapter.PUBLISH_STATE_OTHER_ERROR;
        }
    }

    /**
     * Get the information of the publish request response.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("taskId=").append(mTaskId)
                .append(", CmdErrorCode=").append(getCmdErrorCode().orElse(-1))
                .append(", NetworkRespSipCode=").append(getNetworkRespSipCode().orElse(-1))
                .append(", ReasonPhrase=").append(getReasonPhrase().orElse(""))
                .append(", ReasonHeaderCause=").append(getReasonHeaderCause().orElse(-1))
                .append(", ReasonHeaderText=").append(getReasonHeaderText().orElse(""))
                .append(", ResponseTimestamp=").append(mResponseTimestamp)
                .append(", isRequestSuccess=").append(isRequestSuccess())
                .append(", needRetry=").append(mNeedRetry);
        return builder.toString();
    }
}

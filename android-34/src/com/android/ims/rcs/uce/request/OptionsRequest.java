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

import static android.telephony.ims.RcsContactUceCapability.SOURCE_TYPE_NETWORK;

import android.annotation.NonNull;
import android.net.Uri;
import android.os.RemoteException;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsUceAdapter;
import android.telephony.ims.aidl.IOptionsResponseCallback;
import android.telephony.ims.stub.RcsCapabilityExchangeImplBase.CommandCode;

import com.android.ims.rcs.uce.options.OptionsController;
import com.android.ims.rcs.uce.request.UceRequestManager.RequestManagerCallback;
import com.android.ims.rcs.uce.util.NetworkSipCode;
import com.android.internal.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The UceRequest to request the capabilities when the OPTIONS mechanism is supported by the
 * network.
 */
public class OptionsRequest extends CapabilityRequest {

    // The result callback of the capabilities request from the IMS service.
    private IOptionsResponseCallback mResponseCallback = new IOptionsResponseCallback.Stub() {
        @Override
        public void onCommandError(int code) {
            OptionsRequest.this.onCommandError(code);
        }

        @Override
        public void onNetworkResponse(int sipCode, String reason, List<String> remoteCaps) {
            OptionsRequest.this.onNetworkResponse(sipCode, reason, remoteCaps);
        }
    };

    private Uri mContactUri;
    private OptionsController mOptionsController;

    public OptionsRequest(int subId, @UceRequestType int requestType,
            RequestManagerCallback taskMgrCallback, OptionsController optionsController) {
        super(subId, requestType, taskMgrCallback);
        mOptionsController = optionsController;
        logd("OptionsRequest created");
    }

    @VisibleForTesting
    public OptionsRequest(int subId, @UceRequestType int requestType,
            RequestManagerCallback taskMgrCallback, OptionsController optionsController,
            CapabilityRequestResponse requestResponse) {
        super(subId, requestType, taskMgrCallback, requestResponse);
        mOptionsController = optionsController;
    }

    @Override
    public void onFinish() {
        mOptionsController = null;
        super.onFinish();
        logd("OptionsRequest finish");
    }

    @Override
    public void requestCapabilities(@NonNull List<Uri> requestCapUris) {
        OptionsController optionsController = mOptionsController;
        if (optionsController == null) {
            logw("requestCapabilities: request is finished");
            mRequestResponse.setRequestInternalError(RcsUceAdapter.ERROR_GENERIC_FAILURE);
            mRequestManagerCallback.notifyRequestError(mCoordinatorId, mTaskId);
            return;
        }

        // Get the device's capabilities to send to the remote client.
        RcsContactUceCapability deviceCap = mRequestManagerCallback.getDeviceCapabilities(
                RcsContactUceCapability.CAPABILITY_MECHANISM_OPTIONS);
        if (deviceCap == null) {
            logw("requestCapabilities: Cannot get device capabilities");
            mRequestResponse.setRequestInternalError(RcsUceAdapter.ERROR_GENERIC_FAILURE);
            mRequestManagerCallback.notifyRequestError(mCoordinatorId, mTaskId);
            return;
        }

        mContactUri = requestCapUris.get(0);
        Set<String> featureTags = deviceCap.getFeatureTags();

        logi("requestCapabilities: featureTag size=" + featureTags.size());
        try {
            // Send the capabilities request.
            optionsController.sendCapabilitiesRequest(mContactUri, featureTags, mResponseCallback);
            // Setup the timeout timer.
            setupRequestTimeoutTimer();
        } catch (RemoteException e) {
            logw("requestCapabilities exception: " + e);
            mRequestResponse.setRequestInternalError(RcsUceAdapter.ERROR_GENERIC_FAILURE);
            mRequestManagerCallback.notifyRequestError(mCoordinatorId, mTaskId);
        }
    }

    // Receive the command error callback which is triggered by IOptionsResponseCallback.
    private void onCommandError(@CommandCode int cmdError) {
        logd("onCommandError: error code=" + cmdError);
        if (mIsFinished) {
            logw("onCommandError: The request is already finished");
            return;
        }
        mRequestResponse.setCommandError(cmdError);
        mRequestManagerCallback.notifyCommandError(mCoordinatorId, mTaskId);
    }

    // Receive the network response callback which is triggered by IOptionsResponseCallback.
    private void onNetworkResponse(int sipCode, String reason, List<String> remoteCaps) {
        logd("onNetworkResponse: sipCode=" + sipCode + ", reason=" + reason
                + ", remoteCap size=" + ((remoteCaps == null) ? "null" : remoteCaps.size()));
        if (mIsFinished) {
            logw("onNetworkResponse: The request is already finished");
            return;
        }

        if (remoteCaps == null) {
            remoteCaps = Collections.EMPTY_LIST;
        }

        // Set the all the results to the request response.
        mRequestResponse.setNetworkResponseCode(sipCode, reason);
        mRequestResponse.setRemoteCapabilities(new HashSet<>(remoteCaps));
        RcsContactUceCapability contactCapabilities = getContactCapabilities(mContactUri, sipCode,
                new HashSet<>(remoteCaps));
        mRequestResponse.addUpdatedCapabilities(Collections.singletonList(contactCapabilities));

        // Notify that the network response is received.
        mRequestManagerCallback.notifyNetworkResponse(mCoordinatorId, mTaskId);
    }

    /**
     * Convert the remote capabilities from string list type to RcsContactUceCapability.
     */
    private RcsContactUceCapability getContactCapabilities(Uri contact, int sipCode,
            Set<String> featureTags) {
        int requestResult = RcsContactUceCapability.REQUEST_RESULT_FOUND;
        if (!mRequestResponse.isNetworkResponseOK()) {
            switch (sipCode) {
                case NetworkSipCode.SIP_CODE_REQUEST_TIMEOUT:
                    // Intentional fallthrough
                case NetworkSipCode.SIP_CODE_TEMPORARILY_UNAVAILABLE:
                    requestResult = RcsContactUceCapability.REQUEST_RESULT_NOT_ONLINE;
                    break;
                case NetworkSipCode.SIP_CODE_NOT_FOUND:
                    // Intentional fallthrough
                case NetworkSipCode.SIP_CODE_DOES_NOT_EXIST_ANYWHERE:
                    requestResult = RcsContactUceCapability.REQUEST_RESULT_NOT_FOUND;
                    break;
                default:
                    requestResult = RcsContactUceCapability.REQUEST_RESULT_NOT_FOUND;
                    break;
            }
        }

        RcsContactUceCapability.OptionsBuilder optionsBuilder
                = new RcsContactUceCapability.OptionsBuilder(contact, SOURCE_TYPE_NETWORK);
        optionsBuilder.setRequestResult(requestResult);
        optionsBuilder.addFeatureTags(featureTags);
        return optionsBuilder.build();
    }

    @VisibleForTesting
    public IOptionsResponseCallback getResponseCallback() {
        return mResponseCallback;
    }
}

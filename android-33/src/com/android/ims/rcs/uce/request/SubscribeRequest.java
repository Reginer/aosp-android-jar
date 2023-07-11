/*
 * Copyright (c) 2020 The Android Open Source Project
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

import android.annotation.NonNull;
import android.net.Uri;
import android.os.RemoteException;
import android.telephony.ims.RcsContactTerminatedReason;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsUceAdapter;
import android.telephony.ims.aidl.ISubscribeResponseCallback;
import android.telephony.ims.stub.RcsCapabilityExchangeImplBase.CommandCode;

import com.android.ims.rcs.uce.eab.EabCapabilityResult;
import com.android.ims.rcs.uce.presence.pidfparser.PidfParser;
import com.android.ims.rcs.uce.presence.pidfparser.PidfParserUtils;
import com.android.ims.rcs.uce.presence.pidfparser.RcsContactUceCapabilityWrapper;
import com.android.ims.rcs.uce.presence.subscribe.SubscribeController;
import com.android.ims.rcs.uce.request.UceRequestManager.RequestManagerCallback;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The UceRequest to request the capabilities when the presence mechanism is supported by the
 * network.
 */
public class SubscribeRequest extends CapabilityRequest {

    // The result callback of the capabilities request from IMS service.
    private final ISubscribeResponseCallback mResponseCallback =
            new ISubscribeResponseCallback.Stub() {
                @Override
                public void onCommandError(int code) {
                    SubscribeRequest.this.onCommandError(code);
                }
                @Override
                public void onNetworkResponse(int code, String reason) {
                    SubscribeRequest.this.onNetworkResponse(code, reason);
                }
                @Override
                public void onNetworkRespHeader(int code, String reasonPhrase,
                        int reasonHeaderCause, String reasonHeaderText) {
                    SubscribeRequest.this.onNetworkResponse(code, reasonPhrase, reasonHeaderCause,
                            reasonHeaderText);
                }
                @Override
                public void onNotifyCapabilitiesUpdate(List<String> pidfXmls) {
                    SubscribeRequest.this.onCapabilitiesUpdate(pidfXmls);
                }
                @Override
                public void onResourceTerminated(List<RcsContactTerminatedReason> terminatedList) {
                    SubscribeRequest.this.onResourceTerminated(terminatedList);
                }
                @Override
                public void onTerminated(String reason, long retryAfterMillis) {
                    SubscribeRequest.this.onTerminated(reason, retryAfterMillis);
                }
            };

    private SubscribeController mSubscribeController;

    public SubscribeRequest(int subId, @UceRequestType int requestType,
            RequestManagerCallback taskMgrCallback, SubscribeController subscribeController) {
        super(subId, requestType, taskMgrCallback);
        mSubscribeController = subscribeController;
        logd("SubscribeRequest created");
    }

    @VisibleForTesting
    public SubscribeRequest(int subId, @UceRequestType int requestType,
            RequestManagerCallback taskMgrCallback, SubscribeController subscribeController,
            CapabilityRequestResponse requestResponse) {
        super(subId, requestType, taskMgrCallback, requestResponse);
        mSubscribeController = subscribeController;
    }

    @Override
    public void onFinish() {
        mSubscribeController = null;
        super.onFinish();
        logd("SubscribeRequest finish");
    }

    @Override
    public void requestCapabilities(@NonNull List<Uri> requestCapUris) {
        SubscribeController subscribeController = mSubscribeController;
        if (subscribeController == null) {
            logw("requestCapabilities: request is finished");
            mRequestResponse.setRequestInternalError(RcsUceAdapter.ERROR_GENERIC_FAILURE);
            mRequestManagerCallback.notifyRequestError(mCoordinatorId, mTaskId);
            return;
        }

        logi("requestCapabilities: size=" + requestCapUris.size());
        try {
            // Send the capabilities request.
            subscribeController.requestCapabilities(requestCapUris, mResponseCallback);
            // Setup the timeout timer.
            setupRequestTimeoutTimer();
        } catch (RemoteException e) {
            logw("requestCapabilities exception: " + e);
            mRequestResponse.setRequestInternalError(RcsUceAdapter.ERROR_GENERIC_FAILURE);
            mRequestManagerCallback.notifyRequestError(mCoordinatorId, mTaskId);
        }
    }

    // Receive the command error callback which is triggered by ISubscribeResponseCallback.
    private void onCommandError(@CommandCode int cmdError) {
        logd("onCommandError: error code=" + cmdError);
        if (mIsFinished) {
            logw("onCommandError: request is already finished");
            return;
        }
        mRequestResponse.setCommandError(cmdError);
        mRequestManagerCallback.notifyCommandError(mCoordinatorId, mTaskId);
    }

    // Receive the network response callback which is triggered by ISubscribeResponseCallback.
    private void onNetworkResponse(int sipCode, String reason) {
        logd("onNetworkResponse: code=" + sipCode + ", reason=" + reason);
        if (mIsFinished) {
            logw("onNetworkResponse: request is already finished");
            return;
        }
        mRequestResponse.setNetworkResponseCode(sipCode, reason);
        mRequestManagerCallback.notifyNetworkResponse(mCoordinatorId, mTaskId);
    }

    // Receive the network response callback which is triggered by ISubscribeResponseCallback.
    private void onNetworkResponse(int sipCode, String reasonPhrase,
        int reasonHeaderCause, String reasonHeaderText) {
        logd("onNetworkResponse: code=" + sipCode + ", reasonPhrase=" + reasonPhrase +
                ", reasonHeaderCause=" + reasonHeaderCause +
                ", reasonHeaderText=" + reasonHeaderText);
        if (mIsFinished) {
            logw("onNetworkResponse: request is already finished");
            return;
        }
        mRequestResponse.setNetworkResponseCode(sipCode, reasonPhrase, reasonHeaderCause,
                reasonHeaderText);
        mRequestManagerCallback.notifyNetworkResponse(mCoordinatorId, mTaskId);
    }

    // Receive the resource terminated callback which is triggered by ISubscribeResponseCallback.
    private void onResourceTerminated(List<RcsContactTerminatedReason> terminatedResource) {
        if (mIsFinished) {
            logw("onResourceTerminated: request is already finished");
            return;
        }

        if (terminatedResource == null) {
            logw("onResourceTerminated: the parameter is null");
            terminatedResource = Collections.emptyList();
        }

        logd("onResourceTerminated: size=" + terminatedResource.size());

        // Add the terminated resource into the RequestResponse and notify the RequestManager
        // to process the RcsContactUceCapabilities update.
        mRequestResponse.addTerminatedResource(terminatedResource);
        mRequestManagerCallback.notifyResourceTerminated(mCoordinatorId, mTaskId);
    }

    // Receive the capabilities update callback which is triggered by ISubscribeResponseCallback.
    private void onCapabilitiesUpdate(List<String> pidfXml) {
        if (mIsFinished) {
            logw("onCapabilitiesUpdate: request is already finished");
            return;
        }

        if (pidfXml == null) {
            logw("onCapabilitiesUpdate: The parameter is null");
            pidfXml = Collections.EMPTY_LIST;
        }

        // Convert from the pidf xml to the list of RcsContactUceCapabilityWrapper
        List<RcsContactUceCapabilityWrapper> capabilityList = pidfXml.stream()
                .map(pidf -> PidfParser.getRcsContactUceCapabilityWrapper(pidf))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // When the given PIDF xml is empty, set the contacts who have not received the
        // capabilities updated as non-RCS user.
        List<RcsContactUceCapability> notReceivedCapabilityList = new ArrayList<>();
        if (capabilityList.isEmpty()) {
            logd("onCapabilitiesUpdate: The capabilities list is empty, Set to non-RCS user.");
            List<Uri> notReceiveCapUpdatedContactList =
                    mRequestResponse.getNotReceiveCapabilityUpdatedContact();
            notReceivedCapabilityList = notReceiveCapUpdatedContactList.stream()
                    .map(PidfParserUtils::getNotFoundContactCapabilities)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        List<RcsContactUceCapability> updateCapabilityList = new ArrayList<>();
        List<Uri> malformedListWithEntityURI = new ArrayList<>();
        for (RcsContactUceCapabilityWrapper capability : capabilityList) {
            if (!capability.isMalformed()) {
                updateCapabilityList.add(capability.toRcsContactUceCapability());
            } else {
                logw("onCapabilitiesUpdate: malformed capability was found and not saved.");
                malformedListWithEntityURI.add(capability.getEntityUri());
            }
        }
        logd("onCapabilitiesUpdate: PIDF size=" + pidfXml.size()
                + ", not received capability size=" + notReceivedCapabilityList.size()
                + ", normal capability size=" + updateCapabilityList.size()
                + ", malformed but entity uri is valid capability size="
                + malformedListWithEntityURI.size());

        for (RcsContactUceCapability emptyCapability : notReceivedCapabilityList) {
            updateCapabilityList.add(emptyCapability);
        }

        // All tuples in received xml are malformed but entity uri is valid.
        // The capability should be get from the DB and report it to callback.
        List<EabCapabilityResult> cachedCapabilityList =
                mRequestManagerCallback.getCapabilitiesFromCache(malformedListWithEntityURI);
        for (EabCapabilityResult cacheEabCapability : cachedCapabilityList) {
            RcsContactUceCapability cachedCapability = cacheEabCapability.getContactCapabilities();
            if (cachedCapability != null) {
                updateCapabilityList.add(cachedCapability);
            }
        }
        // Add these updated RcsContactUceCapability into the RequestResponse and notify
        // the RequestManager to process the RcsContactUceCapabilities updated.
        logd("onCapabilitiesUpdate: updatedCapability size=" + updateCapabilityList.size());
        mRequestResponse.addUpdatedCapabilities(updateCapabilityList);
        mRequestManagerCallback.notifyCapabilitiesUpdated(mCoordinatorId, mTaskId);
    }

    // Receive the terminated callback which is triggered by ISubscribeResponseCallback.
    private void onTerminated(String reason, long retryAfterMillis) {
        logd("onTerminated: reason=" + reason + ", retryAfter=" + retryAfterMillis);
        if (mIsFinished) {
            logd("onTerminated: This request is already finished");
            return;
        }
        mRequestResponse.setTerminated(reason, retryAfterMillis);
        mRequestManagerCallback.notifyTerminated(mCoordinatorId, mTaskId);
    }

    @VisibleForTesting
    public ISubscribeResponseCallback getResponseCallback() {
        return mResponseCallback;
    }
}

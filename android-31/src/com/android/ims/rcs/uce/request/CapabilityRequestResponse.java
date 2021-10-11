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

package com.android.ims.rcs.uce.request;

import android.net.Uri;
import android.telephony.ims.RcsContactTerminatedReason;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsUceAdapter;
import android.telephony.ims.RcsUceAdapter.ErrorCode;
import android.telephony.ims.stub.RcsCapabilityExchangeImplBase;
import android.telephony.ims.stub.RcsCapabilityExchangeImplBase.CommandCode;
import android.text.TextUtils;
import android.util.Log;

import com.android.ims.rcs.uce.UceController;
import com.android.ims.rcs.uce.presence.pidfparser.PidfParserUtils;
import com.android.ims.rcs.uce.util.NetworkSipCode;
import com.android.ims.rcs.uce.util.UceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The container of the result of the capabilities request.
 */
public class CapabilityRequestResponse {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "CapabilityRequestResp";

    // The error code when the request encounters internal errors.
    private @ErrorCode Optional<Integer> mRequestInternalError;

    // The command error code of the request. It is assigned by the callback "onCommandError"
    private @CommandCode Optional<Integer> mCommandError;

    // The SIP code and reason of the network response.
    private Optional<Integer> mNetworkRespSipCode;
    private Optional<String> mReasonPhrase;

    // The SIP code and the phrase read from the reason header
    private Optional<Integer> mReasonHeaderCause;
    private Optional<String> mReasonHeaderText;

    // The reason why the this request was terminated and how long after it can be retried.
    // This value is assigned by the callback "onTerminated"
    private Optional<String> mTerminatedReason;
    private Optional<Long> mRetryAfterMillis;

    // The list of the valid capabilities which is retrieved from the cache.
    private List<RcsContactUceCapability> mCachedCapabilityList;

    // The list of the updated capabilities. This is assigned by the callback
    // "onNotifyCapabilitiesUpdate"
    private List<RcsContactUceCapability> mUpdatedCapabilityList;

    // The list of the terminated resource. This is assigned by the callback
    // "onResourceTerminated"
    private List<RcsContactUceCapability> mTerminatedResource;

    // The list of the remote contact's capability.
    private Set<String> mRemoteCaps;

    // The collection to record whether the request contacts have received the capabilities updated.
    private Map<Uri, Boolean> mContactCapsReceived;

    public CapabilityRequestResponse() {
        mRequestInternalError = Optional.empty();
        mCommandError = Optional.empty();
        mNetworkRespSipCode = Optional.empty();
        mReasonPhrase = Optional.empty();
        mReasonHeaderCause = Optional.empty();
        mReasonHeaderText = Optional.empty();
        mTerminatedReason = Optional.empty();
        mRetryAfterMillis = Optional.of(0L);
        mTerminatedResource = new ArrayList<>();
        mCachedCapabilityList = new ArrayList<>();
        mUpdatedCapabilityList = new ArrayList<>();
        mRemoteCaps = new HashSet<>();
        mContactCapsReceived = new HashMap<>();
    }

    /**
     * Set the request contacts which is expected to receive the capabilities updated.
     */
    public synchronized void setRequestContacts(List<Uri> contactUris) {
        // Initialize the default value to FALSE. All the numbers have not received the
        // capabilities updated.
        contactUris.forEach(contact -> mContactCapsReceived.put(contact, Boolean.FALSE));
        Log.d(LOG_TAG, "setRequestContacts: size=" + mContactCapsReceived.size());
    }

    /**
     * Get the contacts that have not received the capabilities updated yet.
     */
    public synchronized List<Uri> getNotReceiveCapabilityUpdatedContact() {
        return mContactCapsReceived.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), Boolean.FALSE))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Set the request contacts which is expected to receive the capabilities updated.
     */
    public synchronized boolean haveAllRequestCapsUpdatedBeenReceived() {
        return !(mContactCapsReceived.containsValue(Boolean.FALSE));
    }

    /**
     * Set the error code when the request encounters internal unexpected errors.
     * @param errorCode the error code of the internal request error.
     */
    public synchronized void setRequestInternalError(@ErrorCode int errorCode) {
        mRequestInternalError = Optional.of(errorCode);
    }

    /**
     * Get the request internal error code.
     */
    public synchronized Optional<Integer> getRequestInternalError() {
        return mRequestInternalError;
    }

    /**
     * Set the command error code which is sent from ImsService and set the capability error code.
     */
    public synchronized void setCommandError(@CommandCode int commandError) {
        mCommandError = Optional.of(commandError);
    }

    /**
     * Get the command error codeof this request.
     */
    public synchronized Optional<Integer> getCommandError() {
        return mCommandError;
    }

    /**
     * Set the network response of this request which is sent by the network.
     */
    public synchronized void setNetworkResponseCode(int sipCode, String reason) {
        mNetworkRespSipCode = Optional.of(sipCode);
        mReasonPhrase = Optional.ofNullable(reason);
    }

    /**
     * Set the network response of this request which is sent by the network.
     */
    public synchronized void setNetworkResponseCode(int sipCode, String reasonPhrase,
            int reasonHeaderCause, String reasonHeaderText) {
        mNetworkRespSipCode = Optional.of(sipCode);
        mReasonPhrase = Optional.ofNullable(reasonPhrase);
        mReasonHeaderCause = Optional.of(reasonHeaderCause);
        mReasonHeaderText = Optional.ofNullable(reasonHeaderText);
    }

    // Get the sip code of the network response.
    public synchronized Optional<Integer> getNetworkRespSipCode() {
        return mNetworkRespSipCode;
    }

    // Get the reason of the network response.
    public synchronized Optional<String> getReasonPhrase() {
        return mReasonPhrase;
    }

    // Get the response sip code from the reason header.
    public synchronized Optional<Integer> getReasonHeaderCause() {
        return mReasonHeaderCause;
    }

    // Get the response phrae from the reason header.
    public synchronized Optional<String> getReasonHeaderText() {
        return mReasonHeaderText;
    }

    public Optional<Integer> getResponseSipCode() {
        if (mReasonHeaderCause.isPresent()) {
            return mReasonHeaderCause;
        } else {
            return mNetworkRespSipCode;
        }
    }

    public Optional<String> getResponseReason() {
        if (mReasonPhrase.isPresent()) {
            return mReasonPhrase;
        } else {
            return mReasonHeaderText;
        }
    }

    /**
     * Set the reason and retry-after info when the callback onTerminated is called.
     * @param reason The reason why this request is terminated.
     * @param retryAfterMillis How long to wait before retry this request.
     */
    public synchronized void setTerminated(String reason, long retryAfterMillis) {
        mTerminatedReason = Optional.ofNullable(reason);
        mRetryAfterMillis = Optional.of(retryAfterMillis);
    }

    /**
     * @return The reason of terminating the subscription request. empty string if it has not
     * been given.
     */
    public synchronized String getTerminatedReason() {
        return mTerminatedReason.orElse("");
    }

    /**
     * @return Return the retryAfterMillis, 0L if the value is not present.
     */
    public synchronized long getRetryAfterMillis() {
        return mRetryAfterMillis.orElse(0L);
    }

    /**
     * Add the capabilities which are retrieved from the cache.
     */
    public synchronized void addCachedCapabilities(List<RcsContactUceCapability> capabilityList) {
        mCachedCapabilityList.addAll(capabilityList);

        // Update the flag to indicate that these contacts have received the capabilities updated.
        updateCapsReceivedFlag(capabilityList);
    }

    /**
     * Update the flag to indicate that the given contacts have received the capabilities updated.
     */
    private synchronized void updateCapsReceivedFlag(List<RcsContactUceCapability> updatedCapList) {
        for (RcsContactUceCapability updatedCap : updatedCapList) {
            Uri updatedUri = updatedCap.getContactUri();
            if (updatedUri == null) continue;
            String updatedUriStr = updatedUri.toString();

            for (Map.Entry<Uri, Boolean> contactCapEntry : mContactCapsReceived.entrySet()) {
                String number = UceUtils.getContactNumber(contactCapEntry.getKey());
                if (!TextUtils.isEmpty(number) && updatedUriStr.contains(number)) {
                    // Set the flag that this contact has received the capability updated.
                    contactCapEntry.setValue(true);
                }
            }
        }
    }

    /**
     * Clear the cached capabilities when the cached capabilities have been sent to client.
     */
    public synchronized void removeCachedContactCapabilities() {
        mCachedCapabilityList.clear();
    }

    /**
     * @return the cached capabilities.
     */
    public synchronized List<RcsContactUceCapability> getCachedContactCapability() {
        return Collections.unmodifiableList(mCachedCapabilityList);
    }

    /**
     * Add the updated contact capabilities which sent from ImsService.
     */
    public synchronized void addUpdatedCapabilities(List<RcsContactUceCapability> capabilityList) {
        mUpdatedCapabilityList.addAll(capabilityList);

        // Update the flag to indicate that these contacts have received the capabilities updated.
        updateCapsReceivedFlag(capabilityList);
    }

    /**
     * Remove the given capabilities from the UpdatedCapabilityList when these capabilities have
     * updated to the requester.
     */
    public synchronized void removeUpdatedCapabilities(List<RcsContactUceCapability> capList) {
        mUpdatedCapabilityList.removeAll(capList);
    }

    /**
     * Get all the updated capabilities to trigger the capability receive callback.
     */
    public synchronized List<RcsContactUceCapability> getUpdatedContactCapability() {
        return Collections.unmodifiableList(mUpdatedCapabilityList);
    }

    /**
     * Add the terminated resources which sent from ImsService.
     */
    public synchronized void addTerminatedResource(List<RcsContactTerminatedReason> resourceList) {
        // Convert the RcsContactTerminatedReason to RcsContactUceCapability
        List<RcsContactUceCapability> capabilityList = resourceList.stream()
                .filter(Objects::nonNull)
                .map(reason -> PidfParserUtils.getTerminatedCapability(
                        reason.getContactUri(), reason.getReason())).collect(Collectors.toList());

        // Save the terminated resource.
        mTerminatedResource.addAll(capabilityList);

        // Update the flag to indicate that these contacts have received the capabilities updated.
        updateCapsReceivedFlag(capabilityList);
    }

    /*
     * Remove the given capabilities from the mTerminatedResource when these capabilities have
     * updated to the requester.
     */
    public synchronized void removeTerminatedResources(List<RcsContactUceCapability> resourceList) {
        mTerminatedResource.removeAll(resourceList);
    }

    /**
     * Get the terminated resources which sent from ImsService.
     */
    public synchronized List<RcsContactUceCapability> getTerminatedResources() {
        return Collections.unmodifiableList(mTerminatedResource);
    }

    /**
     * Set the remote's capabilities which are sent from the network.
     */
    public synchronized void setRemoteCapabilities(Set<String> remoteCaps) {
        if (remoteCaps != null) {
            remoteCaps.stream().filter(Objects::nonNull).forEach(capability ->
                    mRemoteCaps.add(capability));
        }
    }

    /**
     * Get the remote capability feature tags.
     */
    public synchronized Set<String> getRemoteCapability() {
        return Collections.unmodifiableSet(mRemoteCaps);
    }

    /**
     * Check if the network response is success.
     * @return true if the network response code is OK or Accepted and the Reason header cause
     * is either not present or OK.
     */
    public synchronized boolean isNetworkResponseOK() {
        final int sipCodeOk = NetworkSipCode.SIP_CODE_OK;
        final int sipCodeAccepted = NetworkSipCode.SIP_CODE_ACCEPTED;
        Optional<Integer> respSipCode = getNetworkRespSipCode();
        if (respSipCode.filter(c -> (c == sipCodeOk || c == sipCodeAccepted)).isPresent()
                && (!getReasonHeaderCause().isPresent()
                        || getReasonHeaderCause().filter(c -> c == sipCodeOk).isPresent())) {
            return true;
        }
        return false;
    }

    /**
     * Check whether the request is forbidden or not.
     * @return true if the Reason header sip code is 403(Forbidden) or the response sip code is 403.
     */
    public synchronized boolean isRequestForbidden() {
        final int sipCodeForbidden = NetworkSipCode.SIP_CODE_FORBIDDEN;
        if (getReasonHeaderCause().isPresent()) {
            return getReasonHeaderCause().filter(c -> c == sipCodeForbidden).isPresent();
        } else {
            return getNetworkRespSipCode().filter(c -> c == sipCodeForbidden).isPresent();
        }
    }

    /**
     * Check the contacts of the request is not found.
     * @return true if the sip code of the network response is one of NOT_FOUND(404),
     * SIP_CODE_METHOD_NOT_ALLOWED(405) or DOES_NOT_EXIST_ANYWHERE(604)
     */
    public synchronized boolean isNotFound() {
        Optional<Integer> respSipCode = Optional.empty();
        if (getReasonHeaderCause().isPresent()) {
            respSipCode = getReasonHeaderCause();
        } else if (getNetworkRespSipCode().isPresent()) {
            respSipCode = getNetworkRespSipCode();
        }

        if (respSipCode.isPresent()) {
            int sipCode = respSipCode.get();
            if (sipCode == NetworkSipCode.SIP_CODE_NOT_FOUND ||
            sipCode == NetworkSipCode.SIP_CODE_METHOD_NOT_ALLOWED ||
            sipCode == NetworkSipCode.SIP_CODE_DOES_NOT_EXIST_ANYWHERE) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method convert from the command error code which are defined in the
     * RcsCapabilityExchangeImplBase to the Capabilities error code which are defined in the
     * RcsUceAdapter.
     */
    public static int getCapabilityErrorFromCommandError(@CommandCode int cmdError) {
        int uceError;
        switch (cmdError) {
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_SERVICE_UNKNOWN:
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_GENERIC_FAILURE:
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_INVALID_PARAM:
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_FETCH_ERROR:
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_NOT_SUPPORTED:
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_NO_CHANGE:
                uceError = RcsUceAdapter.ERROR_GENERIC_FAILURE;
                break;
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_NOT_FOUND:
                uceError = RcsUceAdapter.ERROR_NOT_FOUND;
                break;
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_REQUEST_TIMEOUT:
                uceError = RcsUceAdapter.ERROR_REQUEST_TIMEOUT;
                break;
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_INSUFFICIENT_MEMORY:
                uceError = RcsUceAdapter.ERROR_INSUFFICIENT_MEMORY;
                break;
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_LOST_NETWORK_CONNECTION:
                uceError = RcsUceAdapter.ERROR_LOST_NETWORK;
                break;
            case RcsCapabilityExchangeImplBase.COMMAND_CODE_SERVICE_UNAVAILABLE:
                uceError = RcsUceAdapter.ERROR_SERVER_UNAVAILABLE;
                break;
            default:
                uceError = RcsUceAdapter.ERROR_GENERIC_FAILURE;
                break;
        }
        return uceError;
    }

    /**
     * Convert the SIP error code which sent by ImsService to the capability error code.
     */
    public static int getCapabilityErrorFromSipCode(CapabilityRequestResponse response) {
        int sipError;
        String respReason;
        // Check the sip code in the Reason header first if the Reason Header is present.
        if (response.getReasonHeaderCause().isPresent()) {
            sipError = response.getReasonHeaderCause().get();
            respReason = response.getReasonHeaderText().orElse("");
        } else {
            sipError = response.getNetworkRespSipCode().orElse(-1);
            respReason = response.getReasonPhrase().orElse("");
        }
        return NetworkSipCode.getCapabilityErrorFromSipCode(sipError, respReason,
                UceController.REQUEST_TYPE_CAPABILITY);
    }

    @Override
    public synchronized String toString() {
        StringBuilder builder = new StringBuilder();
        return builder.append("RequestInternalError=").append(mRequestInternalError.orElse(-1))
                .append(", CommandErrorCode=").append(mCommandError.orElse(-1))
                .append(", NetworkResponseCode=").append(mNetworkRespSipCode.orElse(-1))
                .append(", NetworkResponseReason=").append(mReasonPhrase.orElse(""))
                .append(", ReasonHeaderCause=").append(mReasonHeaderCause.orElse(-1))
                .append(", ReasonHeaderText=").append(mReasonHeaderText.orElse(""))
                .append(", TerminatedReason=").append(mTerminatedReason.orElse(""))
                .append(", RetryAfterMillis=").append(mRetryAfterMillis.orElse(0L))
                .append(", Terminated resource size=" + mTerminatedResource.size())
                .append(", cached capability size=" + mCachedCapabilityList.size())
                .append(", Updated capability size=" + mUpdatedCapabilityList.size())
                .append(", RemoteCaps size=" + mRemoteCaps.size())
                .toString();
    }
}

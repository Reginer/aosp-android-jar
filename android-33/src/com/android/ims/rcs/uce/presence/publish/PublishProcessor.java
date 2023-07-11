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

import static android.telephony.ims.RcsContactUceCapability.CAPABILITY_MECHANISM_PRESENCE;

import android.annotation.NonNull;
import android.content.Context;
import android.os.RemoteException;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.ims.stub.RcsCapabilityExchangeImplBase;
import android.text.TextUtils;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.Log;

import com.android.ims.RcsFeatureManager;
import com.android.ims.rcs.uce.UceStatsWriter;
import com.android.ims.rcs.uce.presence.pidfparser.PidfParser;
import com.android.ims.rcs.uce.presence.publish.PublishController.PublishControllerCallback;
import com.android.ims.rcs.uce.presence.publish.PublishController.PublishTriggerType;
import com.android.ims.rcs.uce.util.UceUtils;
import com.android.internal.annotations.VisibleForTesting;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.Optional;

/**
 * Send the publish request and handle the response of the publish request result.
 */
public class PublishProcessor {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "PublishProcessor";

    // The length of time waiting for the response callback.
    private static final long RESPONSE_CALLBACK_WAITING_TIME = 60000L;

    private final int mSubId;
    private final Context mContext;
    private volatile boolean mIsDestroyed;
    private volatile RcsFeatureManager mRcsFeatureManager;

    private final UceStatsWriter mUceStatsWriter;

    // Manage the state of the publish processor.
    private PublishProcessorState mProcessorState;

    // The information of the device's capabilities.
    private final DeviceCapabilityInfo mDeviceCapabilities;

    // The callback of the PublishController
    private final PublishControllerCallback mPublishCtrlCallback;

    // The lock of processing the pending request.
    private final Object mPendingRequestLock = new Object();

    private final LocalLog mLocalLog = new LocalLog(UceUtils.LOG_SIZE);

    public PublishProcessor(Context context, int subId, DeviceCapabilityInfo capabilityInfo,
            PublishControllerCallback publishCtrlCallback) {
        mSubId = subId;
        mContext = context;
        mDeviceCapabilities = capabilityInfo;
        mPublishCtrlCallback = publishCtrlCallback;
        mProcessorState = new PublishProcessorState(subId);
        mUceStatsWriter = UceStatsWriter.getInstance();
    }

    @VisibleForTesting
    public PublishProcessor(Context context, int subId, DeviceCapabilityInfo capabilityInfo,
            PublishControllerCallback publishCtrlCallback, UceStatsWriter instance) {
        mSubId = subId;
        mContext = context;
        mDeviceCapabilities = capabilityInfo;
        mPublishCtrlCallback = publishCtrlCallback;
        mProcessorState = new PublishProcessorState(subId);
        mUceStatsWriter = instance;
    }

    /**
     * The RcsFeature has been connected to the framework.
     */
    public void onRcsConnected(RcsFeatureManager featureManager) {
        mLocalLog.log("onRcsConnected");
        logi("onRcsConnected");
        mRcsFeatureManager = featureManager;
        // Check if there is a pending request.
        checkAndSendPendingRequest();
    }

    /**
     * The framework has lost the binding to the RcsFeature.
     */
    public void onRcsDisconnected() {
        mLocalLog.log("onRcsDisconnected");
        logi("onRcsDisconnected");
        mRcsFeatureManager = null;
        mProcessorState.onRcsDisconnected();
    }

    /**
     * Set the destroy flag
     */
    public void onDestroy() {
        mLocalLog.log("onDestroy");
        logi("onDestroy");
        mIsDestroyed = true;
    }

    /**
     * Execute the publish request. This method is called by the handler of the PublishController.
     * @param triggerType The type of triggering the publish request.
     */
    public void doPublish(@PublishTriggerType int triggerType) {
        mProcessorState.setPublishingFlag(true);
        if (!doPublishInternal(triggerType)) {
            // Reset the publishing flag if the request cannot be sent to the IMS service.
            mProcessorState.setPublishingFlag(false);
        }
    }
    /**
     * Execute the publish request internally.
     * @param triggerType The type of triggering the publish request.
     * @return true if the publish is sent to the IMS service successfully, false otherwise.
     */
    private boolean doPublishInternal(@PublishTriggerType int triggerType) {
        if (mIsDestroyed) return false;

        mLocalLog.log("doPublishInternal: trigger type=" + triggerType);
        logi("doPublishInternal: trigger type=" + triggerType);

        // Return if this request is not allowed to be executed.
        if (!isRequestAllowed(triggerType)) {
            mLocalLog.log("doPublishInternal: The request is not allowed.");
            return false;
        }

        // Get the latest device's capabilities.
        RcsContactUceCapability deviceCapability =
                mDeviceCapabilities.getDeviceCapabilities(CAPABILITY_MECHANISM_PRESENCE, mContext);
        if (deviceCapability == null) {
            logw("doPublishInternal: device capability is null");
            return false;
        }

        // Convert the device's capabilities to pidf format.
        String pidfXml = PidfParser.convertToPidf(deviceCapability);
        if (TextUtils.isEmpty(pidfXml)) {
            logw("doPublishInternal: pidfXml is empty");
            return false;
        }

        // Set the pending request and return if RCS is not connected. When the RCS is connected
        // afterward, it will send a new request if there's a pending request.
        RcsFeatureManager featureManager = mRcsFeatureManager;
        if (featureManager == null) {
            logw("doPublishInternal: RCS is not connected.");
            setPendingRequest(triggerType);
            return false;
        }

        featureManager.getImsRegistrationTech((tech) -> {
            int registrationTech = (tech == null)
                    ? ImsRegistrationImplBase.REGISTRATION_TECH_NONE : tech;
            mUceStatsWriter.setImsRegistrationServiceDescStats(mSubId,
                    deviceCapability.getCapabilityTuples(), registrationTech);
        });

        // Publish to the Presence server.
        return publishCapabilities(featureManager, pidfXml);
    }

    /*
     * According to the given trigger type, check whether the request is allowed to be executed or
     * not.
     */
    private boolean isRequestAllowed(@PublishTriggerType int triggerType) {
        // Check if the instance is destroyed.
        if (mIsDestroyed) {
            logd("isPublishAllowed: This instance is already destroyed");
            return false;
        }

        // Check if it has provisioned. When the provisioning changes, a new publish request will
        // be triggered.
        if (!isEabProvisioned()) {
            logd("isPublishAllowed: NOT provisioned");
            return false;
        }

        // Do not request publish if the IMS is not registered. When the IMS is registered
        // afterward, a new publish request will be triggered.
        if (!mDeviceCapabilities.isImsRegistered()) {
            logd("isPublishAllowed: IMS is not registered");
            return false;
        }

        // Skip this request if the PUBLISH is not allowed at current time. Resend the PUBLISH
        // request and it will be triggered with an appropriate delay time.
        if (!mProcessorState.isPublishAllowedAtThisTime()) {
            logd("isPublishAllowed: Current time is not allowed, resend this request");
            mPublishCtrlCallback.requestPublishFromInternal(triggerType);
            return false;
        }
        return true;
    }

    // Publish the device capabilities with the given pidf.
    private boolean publishCapabilities(@NonNull RcsFeatureManager featureManager,
            @NonNull String pidfXml) {
        PublishRequestResponse requestResponse = null;
        try {
            // Clear the pending flag because it is going to send the latest device's capabilities.
            clearPendingRequest();

            // Generate a unique taskId to track this request.
            long taskId = mProcessorState.generatePublishTaskId();
            requestResponse = new PublishRequestResponse(mPublishCtrlCallback, taskId, pidfXml);

            mLocalLog.log("publish capabilities: taskId=" + taskId);
            logi("publishCapabilities: taskId=" + taskId);

            // request publication
            featureManager.requestPublication(pidfXml, requestResponse.getResponseCallback());

            // Send a request canceled timer to avoid waiting too long for the response callback.
            mPublishCtrlCallback.setupRequestCanceledTimer(taskId, RESPONSE_CALLBACK_WAITING_TIME);

            // Inform that the publish request has been sent to the Ims Service.
            mPublishCtrlCallback.notifyPendingPublishRequest();
            return true;
        } catch (RemoteException e) {
            mLocalLog.log("publish capability exception: " + e.getMessage());
            logw("publishCapabilities: exception=" + e.getMessage());
            // Exception occurred, end this request.
            setRequestEnded(requestResponse);
            checkAndSendPendingRequest();
            return false;
        }
   }

    /**
     * Handle the command error callback of the publish request. This method is called by the
     * handler of the PublishController.
     */
    public void onCommandError(PublishRequestResponse requestResponse) {
        if (!checkRequestRespValid(requestResponse)) {
            mLocalLog.log("Command error callback is invalid");
            logw("onCommandError: request response is invalid");
            setRequestEnded(requestResponse);
            checkAndSendPendingRequest();
            return;
        }

        mLocalLog.log("Receive command error code=" + requestResponse.getCmdErrorCode());
        logd("onCommandError: " + requestResponse.toString());

        int cmdError = requestResponse.getCmdErrorCode().orElse(0);
        boolean successful = false;
        if (cmdError == RcsCapabilityExchangeImplBase.COMMAND_CODE_NO_CHANGE) {
            successful = true;
        }
        mUceStatsWriter.setUceEvent(mSubId, UceStatsWriter.PUBLISH_EVENT, successful, cmdError, 0);

        if (requestResponse.needRetry() && !mProcessorState.isReachMaximumRetries()) {
            handleRequestRespWithRetry(requestResponse);
        } else {
            handleRequestRespWithoutRetry(requestResponse);
        }
    }

    /**
     * Handle the network response callback of the publish request. This method is called by the
     * handler of the PublishController.
     */
    public void onNetworkResponse(PublishRequestResponse requestResponse) {
        if (!checkRequestRespValid(requestResponse)) {
            mLocalLog.log("Network response callback is invalid");
            logw("onNetworkResponse: request response is invalid");
            setRequestEnded(requestResponse);
            checkAndSendPendingRequest();
            return;
        }

        mLocalLog.log("Receive network response code=" + requestResponse.getNetworkRespSipCode());
        logd("onNetworkResponse: " + requestResponse.toString());

        int responseCode = requestResponse.getNetworkRespSipCode().orElse(0);
        mUceStatsWriter.setUceEvent(mSubId, UceStatsWriter.PUBLISH_EVENT, true, 0,
            responseCode);

        if (requestResponse.needRetry() && !mProcessorState.isReachMaximumRetries()) {
            handleRequestRespWithRetry(requestResponse);
        } else {
            handleRequestRespWithoutRetry(requestResponse);
        }
    }

    // Check if the request response callback is valid.
    private boolean checkRequestRespValid(PublishRequestResponse requestResponse) {
        if (requestResponse == null) {
            logd("checkRequestRespValid: request response is null");
            return false;
        }

        if (!mProcessorState.isPublishingNow()) {
            logd("checkRequestRespValid: the request is finished");
            return false;
        }

        // Abandon this response callback if the current taskId is different to the response
        // callback taskId. This response callback is obsoleted.
        long taskId = mProcessorState.getCurrentTaskId();
        long responseTaskId = requestResponse.getTaskId();
        if (taskId != responseTaskId) {
            logd("checkRequestRespValid: invalid taskId! current taskId=" + taskId
                    + ", response callback taskId=" + responseTaskId);
            return false;
        }

        if (mIsDestroyed) {
            logd("checkRequestRespValid: is already destroyed! taskId=" + taskId);
            return false;
        }
        return true;
    }

    /*
     * Handle the publishing request with retry. This method is called when it receives a failed
     * request response and need to retry.
     */
    private void handleRequestRespWithRetry(PublishRequestResponse requestResponse) {
        // Increase the retry count
        mProcessorState.increaseRetryCount();

        // Reset the pending flag because it is going to resend a request.
        clearPendingRequest();

        // Finish this request and resend a new publish request
        setRequestEnded(requestResponse);
        mPublishCtrlCallback.requestPublishFromInternal(PublishController.PUBLISH_TRIGGER_RETRY);
    }

    /*
     * Handle the publishing request without retry. This method is called when it receives the
     * request response and it does not need to retry.
     */
    private void handleRequestRespWithoutRetry(PublishRequestResponse requestResponse) {
        updatePublishStateFromResponse(requestResponse);
        // Finish the request and check if there is pending request.
        setRequestEnded(requestResponse);
        checkAndSendPendingRequest();
    }

    // After checking the response, it handles calling PublishCtrlCallback.
    private void updatePublishStateFromResponse(PublishRequestResponse response) {
        Instant responseTime = response.getResponseTimestamp();

        // Record the time when the request is successful and reset the retry count.
        if (response.isRequestSuccess()) {
            mProcessorState.setLastPublishedTime(responseTime);
            mProcessorState.resetRetryCount();
        }

        // Update the publish state after the request has finished.
        int publishState = response.getPublishState();
        String pidfXml = response.getPidfXml();
        mPublishCtrlCallback.updatePublishRequestResult(publishState, responseTime, pidfXml);

        // Refresh the device state with the publish request result.
        response.getResponseSipCode().ifPresent(sipCode -> {
            String reason = response.getResponseReason().orElse("");
            mPublishCtrlCallback.refreshDeviceState(sipCode, reason);
        });
    }

    /**
     * Cancel the publishing request since it has token too long for waiting the response callback.
     * This method is called by the handler of the PublishController.
     */
    public void cancelPublishRequest(long taskId) {
        mLocalLog.log("cancel publish request: taskId=" + taskId);
        logd("cancelPublishRequest: taskId=" + taskId);
        setRequestEnded(null);
        checkAndSendPendingRequest();
    }

    /*
     * Finish the publishing request. This method is required to be called before the publishing
     * request is finished.
     */
    private void setRequestEnded(PublishRequestResponse requestResponse) {
        long taskId = -1L;
        if (requestResponse != null) {
            requestResponse.onDestroy();
            taskId = requestResponse.getTaskId();
        }
        mProcessorState.setPublishingFlag(false);
        mPublishCtrlCallback.clearRequestCanceledTimer();

        mLocalLog.log("Set request ended: taskId=" + taskId);
        logd("setRequestEnded: taskId=" + taskId);
    }

    /*
     * Set the pending flag when it cannot be executed now.
     */
    public void setPendingRequest(@PublishTriggerType int triggerType) {
        synchronized (mPendingRequestLock) {
            mProcessorState.setPendingRequest(triggerType);
        }
    }

    /**
     * Check and trigger a new publish request if there is a pending request.
     */
    public void checkAndSendPendingRequest() {
        synchronized (mPendingRequestLock) {
            if (mIsDestroyed) return;
            if (mProcessorState.hasPendingRequest()) {
                // Retrieve the trigger type of the pending request
                int type = mProcessorState.getPendingRequestTriggerType()
                        .orElse(PublishController.PUBLISH_TRIGGER_RETRY);
                logd("checkAndSendPendingRequest: send pending request, type=" + type);

                // Clear the pending flag because it is going to send a PUBLISH request.
                mProcessorState.clearPendingRequest();
                mPublishCtrlCallback.requestPublishFromInternal(type);
            }
        }
    }

    /**
     * Clear the pending request. It means that the publish request is triggered and this flag can
     * be removed.
     */
    private void clearPendingRequest() {
        synchronized (mPendingRequestLock) {
            mProcessorState.clearPendingRequest();
        }
    }

    /**
     * Update the publishing allowed time with the given trigger type. This method wil be called
     * before adding a PUBLISH request to the handler.
     * @param triggerType The trigger type of this PUBLISH request
     */
    public void updatePublishingAllowedTime(@PublishTriggerType int triggerType) {
        mProcessorState.updatePublishingAllowedTime(triggerType);
    }

    /**
     * @return The delay time to allow to execute the PUBLISH request. This method will be called
     * to determine the delay time before adding a PUBLISH request to the handler.
     */
    public Optional<Long> getPublishingDelayTime() {
        return mProcessorState.getPublishingDelayTime();
    }

    /**
     * Update the publish throttle.
     */
    public void updatePublishThrottle(int publishThrottle) {
        mProcessorState.updatePublishThrottle(publishThrottle);
    }

    /**
     * @return true if the publish request is running now.
     */
    public boolean isPublishingNow() {
        return mProcessorState.isPublishingNow();
    }

    /**
     * Reset the retry count and time related publish.
     */
    public void resetState() {
        mProcessorState.resetState();
    }

    /**
     * Update publish status after handling on onPublishUpdate case
     */
    public void publishUpdated(PublishRequestResponse response) {
        updatePublishStateFromResponse(response);
        if (response != null) {
            response.onDestroy();
        }
    }

    @VisibleForTesting
    public void setProcessorState(PublishProcessorState processorState) {
        mProcessorState = processorState;
    }

    @VisibleForTesting
    protected boolean isEabProvisioned() {
        return UceUtils.isEabProvisioned(mContext, mSubId);
    }

    private void logd(String log) {
       Log.d(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private void logi(String log) {
       Log.i(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private void logw(String log) {
        Log.w(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private StringBuilder getLogPrefix() {
        StringBuilder builder = new StringBuilder("[");
        builder.append(mSubId);
        builder.append("] ");
        return builder;
    }

    public void dump(PrintWriter printWriter) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println("PublishProcessor" + "[subId: " + mSubId + "]:");
        pw.increaseIndent();

        pw.print("ProcessorState: isPublishing=");
        pw.print(mProcessorState.isPublishingNow());
        pw.print(", hasReachedMaxRetries=");
        pw.print(mProcessorState.isReachMaximumRetries());
        pw.print(", delayTimeToAllowPublish=");
        pw.println(mProcessorState.getPublishingDelayTime().orElse(-1L));

        pw.println("Log:");
        pw.increaseIndent();
        mLocalLog.dump(pw);
        pw.decreaseIndent();
        pw.println("---");

        pw.decreaseIndent();
    }
}

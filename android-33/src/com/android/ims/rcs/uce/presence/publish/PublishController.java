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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsContactUceCapability.CapabilityMechanism;
import android.telephony.ims.RcsUceAdapter.PublishState;
import android.telephony.ims.aidl.IRcsUcePublishStateCallback;

import com.android.ims.rcs.uce.ControllerBase;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.util.Set;

/**
 * The interface related to the PUBLISH request.
 */
public interface PublishController extends ControllerBase {

    /** Publish is triggered by the ImsService */
    int PUBLISH_TRIGGER_SERVICE = 1;

    /** Publish trigger type: retry */
    int PUBLISH_TRIGGER_RETRY = 2;

    /** Publish trigger type: TTY preferred changes */
    int PUBLISH_TRIGGER_TTY_PREFERRED_CHANGE = 3;

    /** Publish trigger type: Airplane mode changes */
    int PUBLISH_TRIGGER_AIRPLANE_MODE_CHANGE = 4;

    /** Publish trigger type: Mobile data changes */
    int PUBLISH_TRIGGER_MOBILE_DATA_CHANGE = 5;

    /** Publish trigger type: VT setting changes */
    int PUBLISH_TRIGGER_VT_SETTING_CHANGE = 6;

    /** Publish trigger type: MMTEL registered */
    int PUBLISH_TRIGGER_MMTEL_REGISTERED = 7;

    /** Publish trigger type: MMTEL unregistered */
    int PUBLISH_TRIGGER_MMTEL_UNREGISTERED = 8;

    /** Publish trigger type: MMTEL capability changes */
    int PUBLISH_TRIGGER_MMTEL_CAPABILITY_CHANGE = 9;

    /** Publish trigger type: MMTEL associated uri changes */
    int PUBLISH_TRIGGER_MMTEL_URI_CHANGE = 10;

    /** Publish trigger type: RCS registered */
    int PUBLISH_TRIGGER_RCS_REGISTERED = 11;

    /** Publish trigger type: RCS unregistered */
    int PUBLISH_TRIGGER_RCS_UNREGISTERED = 12;

    /** Publish trigger type: RCS associated uri changes */
    int PUBLISH_TRIGGER_RCS_URI_CHANGE = 13;

    /** Publish trigger type: provisioning changes */
    int PUBLISH_TRIGGER_PROVISIONING_CHANGE = 14;

    /**The caps have been overridden for a test*/
    int PUBLISH_TRIGGER_OVERRIDE_CAPS = 15;

    /** The Carrier Config for the subscription has Changed **/
    int PUBLISH_TRIGGER_CARRIER_CONFIG_CHANGED = 16;

    /** MMTEL and RCS are unregistered. **/
    int PUBLISH_TRIGGER_MMTEL_RCS_UNREGISTERED = 17;

    @IntDef(value = {
            PUBLISH_TRIGGER_SERVICE,
            PUBLISH_TRIGGER_RETRY,
            PUBLISH_TRIGGER_TTY_PREFERRED_CHANGE,
            PUBLISH_TRIGGER_AIRPLANE_MODE_CHANGE,
            PUBLISH_TRIGGER_MOBILE_DATA_CHANGE,
            PUBLISH_TRIGGER_VT_SETTING_CHANGE,
            PUBLISH_TRIGGER_MMTEL_REGISTERED,
            PUBLISH_TRIGGER_MMTEL_UNREGISTERED,
            PUBLISH_TRIGGER_MMTEL_CAPABILITY_CHANGE,
            PUBLISH_TRIGGER_MMTEL_URI_CHANGE,
            PUBLISH_TRIGGER_RCS_REGISTERED,
            PUBLISH_TRIGGER_RCS_UNREGISTERED,
            PUBLISH_TRIGGER_RCS_URI_CHANGE,
            PUBLISH_TRIGGER_PROVISIONING_CHANGE,
            PUBLISH_TRIGGER_OVERRIDE_CAPS,
            PUBLISH_TRIGGER_CARRIER_CONFIG_CHANGED,
            PUBLISH_TRIGGER_MMTEL_RCS_UNREGISTERED
    }, prefix="PUBLISH_TRIGGER_")
    @Retention(RetentionPolicy.SOURCE)
    @interface PublishTriggerType {}

    /**
     * Receive the callback from the sub-components which interact with PublishController.
     */
    interface PublishControllerCallback {
        /**
         * Request publish from local.
         */
        void requestPublishFromInternal(@PublishTriggerType int type);

        /**
         * Receive the command error callback of the request from ImsService.
         */
        void onRequestCommandError(PublishRequestResponse requestResponse);

        /**
         * Receive the network response callback fo the request from ImsService.
         */
        void onRequestNetworkResp(PublishRequestResponse requestResponse);

        /**
         * Set the timer to cancel the request. This timer is to prevent taking too long for
         * waiting the response callback.
         */
        void setupRequestCanceledTimer(long taskId, long delay);

        /**
         * Clear the request canceled timer. This api will be called if the request is finished.
         */
        void clearRequestCanceledTimer();

        /**
         * Update the publish request result.
         */
        void updatePublishRequestResult(int publishState, Instant updatedTimestamp, String pidfXml);

        /**
         * Update the value of the publish throttle.
         */
        void updatePublishThrottle(int value);

        /**
         * Update the device state with the publish request result.
         */
        void refreshDeviceState(int SipCode, String reason);

        /**
         * Sent the publish request to ImsService.
         */
        void notifyPendingPublishRequest();

        /**
         * Update the Ims unregistered. This api will be called if the IMS unregistered.
         */
        void updateImsUnregistered();
    }

    /**
     * Add new feature tags to the Set used to calculate the capabilities in PUBLISH.
     * <p>
     * Used for testing ONLY.
     * @return the new capabilities that will be used for PUBLISH.
     */
    RcsContactUceCapability addRegistrationOverrideCapabilities(Set<String> featureTags);

    /**
     * Remove existing feature tags to the Set used to calculate the capabilities in PUBLISH.
     * <p>
     * Used for testing ONLY.
     * @return the new capabilities that will be used for PUBLISH.
     */
    RcsContactUceCapability removeRegistrationOverrideCapabilities(Set<String> featureTags);

    /**
     * Clear all overrides in the Set used to calculate the capabilities in PUBLISH.
     * <p>
     * Used for testing ONLY.
     * @return the new capabilities that will be used for PUBLISH.
     */
    RcsContactUceCapability clearRegistrationOverrideCapabilities();

    /**
     * @return latest RcsContactUceCapability instance that will be used for PUBLISH.
     */
    RcsContactUceCapability getLatestRcsContactUceCapability();

    /**
     * Retrieve the RCS UCE Publish state.
     */
    @PublishState int getUcePublishState(boolean isSupportPublishingState);

    /**
     * @return the last PIDF XML used for publish or {@code null} if the device is not published.
     */
    String getLastPidfXml();

    /**
     * Notify that the device's capabilities have been unpublished from the network.
     */
    void onUnpublish();

    /**
     * Notify that the device's publish status have been changed.
     */
    void onPublishUpdated(int reasonCode, String reasonPhrase,
            int reasonHeaderCause, String reasonHeaderText);

    /**
     * Retrieve the device's capabilities.
     */
    RcsContactUceCapability getDeviceCapabilities(@CapabilityMechanism int mechanism);

    /**
     * Publish the device's capabilities to the Presence server.
     */
    void requestPublishCapabilitiesFromService(int triggerType);

    /**
     * Register a {@link PublishStateCallback} to listen to the published state changed.
     */
    void registerPublishStateCallback(@NonNull IRcsUcePublishStateCallback c,
            boolean supportPublishingState);

    /**
     * Removes an existing {@link PublishStateCallback}.
     */
    void unregisterPublishStateCallback(@NonNull IRcsUcePublishStateCallback c);

    /**
     * Setup the timer to reset the device state.
     */
    void setupResetDeviceStateTimer(long resetAfterSec);

    /**
     * Clear the reset device state timer.
     */
    void clearResetDeviceStateTimer();

    /**
     * Dump the state of this PublishController to the printWriter.
     */
    void dump(PrintWriter printWriter);
}

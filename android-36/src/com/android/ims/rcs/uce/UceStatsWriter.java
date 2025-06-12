/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.ims.rcs.uce;

import android.annotation.IntDef;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.ims.stub.RcsCapabilityExchangeImplBase;
import android.telephony.ims.RcsContactPresenceTuple;
import android.telephony.ims.RcsContactUceCapability;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * The UceStatsWriter should be a singleton class for storing atoms in RcsStats.
 * ims-common provides an interface for setting atoms to telephony-common.
 **/
public class UceStatsWriter {
    private static UceStatsWriter sInstance = null;
    private UceStatsCallback mCallBack;

    /**
     * @hide
     */
    // Defines which UCE event occurred.
    @IntDef(value = {
        PUBLISH_EVENT,
        SUBSCRIBE_EVENT,
        INCOMING_OPTION_EVENT,
        OUTGOING_OPTION_EVENT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface UceEventType {}
    /**
     * UCE events related to Publish Method.
     */
    public static final int PUBLISH_EVENT = 0;
    /**
     * UCE events related to Subscribe Method.
     */
    public static final int SUBSCRIBE_EVENT = 1;
    /**
     * UCE events related to incoming Options Method.
     */
    public static final int INCOMING_OPTION_EVENT = 2;
    /**
     * UCE events related to outgoing Options Method.
     */
    public static final int OUTGOING_OPTION_EVENT = 3;

    /**
     * The callback interface is called by the Metrics data creator to receive information from
     * others controllers.
     */
    public interface UceStatsCallback {
        /**
         * Notify the callback listener that the feature tag associated with
         * IMS registration of this device have changed.
         */
        public void onImsRegistrationFeatureTagStats(int subId, List<String> featureTagList,
            int registrationTech);

        /**
         * Notify that the active IMS registration to the carrier network has been terminated.
         */
        public void onStoreCompleteImsRegistrationFeatureTagStats(int subId);

        /**
         * Notify the callback listener that the PIDF ServiceDescriptions associated with
         * the UCE presence of this device have changed.
         */
        void onImsRegistrationServiceDescStats(int subId, List<String> serviceIdList,
            List<String> serviceIdVersionList, int registrationTech);

        /**
         * Notify the callback listener that a subscribe response received.
         */
        void onSubscribeResponse(int subId, long taskId, int networkResponse);

        /**
         * Notify the callback listener that a UCE Network Event has occurred.
         */
        void onUceEvent(int subId, int type, boolean successful, int commandCode,
            int networkResponse);

        /**
         * Notify the callback listener that a subscribe has ended.
         */
        void onSubscribeTerminated(int subId, long taskId, String reason);

        /**
         * Notify that the Presence Notify Event has changed.
         */
        void onPresenceNotifyEvent(int subId, long taskId,
            List<RcsContactUceCapability> updatedCapList);

        /**
         * Notify that the active UCE PUBLISH to the carrier network has been terminated.
         */
        void onStoreCompleteImsRegistrationServiceDescStats(int subId);
    }

    /**
     * create an instance of UceStatsWriter
     */
    public static UceStatsWriter init(UceStatsCallback callback) {
        synchronized (UceStatsWriter.class) {
            if (sInstance == null) {
                sInstance = new UceStatsWriter(callback);
            }
            return sInstance;
        }
    }

    /**
     * get the current instance of UceStatsWriter
     */
    public static UceStatsWriter getInstance() {
        synchronized (UceStatsWriter.class) {
            return sInstance;
        }
    }

    /**
     * Stats about each Feature tag that was included in IMS registration received from
     * the network during register.
     * @param subId The subId associated with the event.
     * @param featureTagList Ims Feature tag list.
     * @param registrationTech The registration tech associated with the feature tag.
     */
    public void setImsRegistrationFeatureTagStats(int subId, List<String> featureTagList,
        @ImsRegistrationImplBase.ImsRegistrationTech int registrationTech) {
        if (mCallBack == null) {
            return;
        }
        mCallBack.onImsRegistrationFeatureTagStats(subId, featureTagList, registrationTech);
    }

    /**
     * Update time of stats for each stored feature tag.
     * @param subId The subId associated with the event.
     */
    public void setStoreCompleteImsRegistrationFeatureTagStats(int subId) {
        if (mCallBack == null) {
            return;
        }
        mCallBack.onStoreCompleteImsRegistrationFeatureTagStats(subId);
    }

    /**
     * Stats about each ServiceDescription that was included in the PIDF XML sent to
     * the network during publish
     * @param subId The subId associated with the event.
     * @param tupleList Tuple information set in PIDF.
     * @param registrationTech The registration tech associated with the feature tag.
     */
    public void setImsRegistrationServiceDescStats(int subId,
        List<RcsContactPresenceTuple> tupleList,
        @ImsRegistrationImplBase.ImsRegistrationTech int registrationTech) {
        if (mCallBack == null) {
            return;
        }
        ArrayList<String> svcId = new ArrayList<>();
        ArrayList<String> svcVersion = new ArrayList<>();

        for (RcsContactPresenceTuple tuple : tupleList) {
            svcId.add(tuple.getServiceId());
            svcVersion.add(tuple.getServiceVersion());
        }
        mCallBack.onImsRegistrationServiceDescStats(subId, svcId, svcVersion, registrationTech);
    }

    /**
     * Stats related to UCE queries to the network
     * @param subId The subId associated with the event.
     * @param taskId The taskId associate with the event.
     * @param networkResponse The network response code for the Uce event
     */
    public void setSubscribeResponse(int subId, long taskId, int networkResponse) {
        if (mCallBack != null) {
            mCallBack.onSubscribeResponse(subId, taskId, networkResponse);
        }
    }

    /**
     * Stats related to UCE queries to the network
     * @param subId The subId associated with the event.
     * @param type Used to identify the message type.
     * @param successful Whether the UCE event is successfully finished.
     * @param commandCode The command error code for the Uce event
     * @param networkResponse The network response code for the Uce event
     */
    public void setUceEvent(int subId, @UceEventType int type, boolean successful,
        @RcsCapabilityExchangeImplBase.CommandCode int commandCode, int networkResponse) {
        if (mCallBack != null) {
            mCallBack.onUceEvent(subId, type, successful, commandCode, networkResponse);
        }
    }

    /**
     * The result of processing received notify messages.
     * @param subId The subId associated with the event.
     * @param taskId The taskId associate with the event.
     * @param updatedCapList Capability information of the user contained in Presence Notify.
     */
    public void setPresenceNotifyEvent(int subId, long taskId,
        List<RcsContactUceCapability> updatedCapList) {
        if (mCallBack == null || updatedCapList == null || updatedCapList.isEmpty()) {
            return;
        }
        mCallBack.onPresenceNotifyEvent(subId, taskId, updatedCapList);
    }

    /**
     * Indicates that the subscription request has become a terminated state.
     * @param subId The subId associated with the event.
     * @param taskId The taskId associate with the event.
     * @param reason The terminated reason associated with the subscription state.
     */
    public void setSubscribeTerminated(int subId, long taskId, String reason) {
        if (mCallBack != null) {
            mCallBack.onSubscribeTerminated(subId, taskId, reason);
        }
    }

    /**
     * indicates that the device has removed an existing PUBLISH from the carrier's network
     * containing the device's RCS capabilities state.
     * The registered time of publication must be set in ImsRegistrationServiceDescStats,
     * which is the life time of publication, so it can be set only when publication is over.
     * @param subId The subId associated with the event.
     */
    public void setUnPublish(int subId) {
        if (mCallBack != null) {
            mCallBack.onStoreCompleteImsRegistrationServiceDescStats(subId);
        }
    }

    @VisibleForTesting
    protected UceStatsWriter(UceStatsCallback callback) {
        mCallBack = callback;
    }
}

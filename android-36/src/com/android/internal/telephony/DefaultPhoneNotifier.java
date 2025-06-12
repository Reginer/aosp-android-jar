/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony;

import android.annotation.NonNull;
import android.content.Context;
import android.telephony.Annotation;
import android.telephony.Annotation.RadioPowerState;
import android.telephony.Annotation.SrvccState;
import android.telephony.BarringInfo;
import android.telephony.CallQuality;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.CellularIdentifierDisclosure;
import android.telephony.LinkCapacityEstimate;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PhoneCapability;
import android.telephony.PhysicalChannelConfig;
import android.telephony.PreciseCallState;
import android.telephony.PreciseDataConnectionState;
import android.telephony.SecurityAlgorithmUpdate;
import android.telephony.ServiceState;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager.DataEnabledReason;
import android.telephony.TelephonyManager.EmergencyCallbackModeStopReason;
import android.telephony.TelephonyManager.EmergencyCallbackModeType;
import android.telephony.TelephonyRegistryManager;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.MediaQualityStatus;
import android.telephony.satellite.NtnSignalStrength;

import com.android.internal.telephony.flags.FeatureFlags;
import com.android.telephony.Rlog;

import java.util.List;
import java.util.Set;

/**
 * broadcast intents
 */
public class DefaultPhoneNotifier implements PhoneNotifier {

    private static final String LOG_TAG = "DefaultPhoneNotifier";
    private static final boolean DBG = false; // STOPSHIP if true

    private TelephonyRegistryManager mTelephonyRegistryMgr;

    /** Feature flags */
    @NonNull
    private final FeatureFlags mFeatureFlags;


    public DefaultPhoneNotifier(Context context, @NonNull FeatureFlags featureFlags) {
        mTelephonyRegistryMgr = (TelephonyRegistryManager) context.getSystemService(
            Context.TELEPHONY_REGISTRY_SERVICE);
        mFeatureFlags = featureFlags;
    }

    @Override
    public void notifyPhoneState(Phone sender) {
        Call ringingCall = sender.getRingingCall();
        int subId = sender.getSubId();
        int phoneId = sender.getPhoneId();
        String incomingNumber = "";
        if (ringingCall != null && ringingCall.getEarliestConnection() != null) {
            incomingNumber = ringingCall.getEarliestConnection().getAddress();
        }
        mTelephonyRegistryMgr.notifyCallStateChanged(phoneId, subId,
                PhoneConstantConversions.convertCallState(sender.getState()), incomingNumber);
    }

    @Override
    public void notifyServiceState(Phone sender) {
        notifyServiceStateForSubId(sender, sender.getServiceState(), sender.getSubId());
    }

    @Override
    public void notifyServiceStateForSubId(Phone sender, ServiceState ss, int subId) {
        int phoneId = sender.getPhoneId();

        Rlog.d(LOG_TAG, "notifyServiceStateForSubId: mRegistryMgr=" + mTelephonyRegistryMgr + " ss="
                + ss + " sender=" + sender + " phondId=" + phoneId + " subId=" + subId);
        if (ss == null) {
            ss = new ServiceState();
            ss.setStateOutOfService();
        }
        mTelephonyRegistryMgr.notifyServiceStateChanged(phoneId, subId, ss);
    }

    @Override
    public void notifySignalStrength(Phone sender) {
        int phoneId = sender.getPhoneId();
        int subId = sender.getSubId();
        if (DBG) {
            // too chatty to log constantly
            Rlog.d(LOG_TAG, "notifySignalStrength: mRegistryMgr=" + mTelephonyRegistryMgr
                + " ss=" + sender.getSignalStrength() + " sender=" + sender);
        }
        mTelephonyRegistryMgr.notifySignalStrengthChanged(phoneId, subId,
                sender.getSignalStrength());
    }

    @Override
    public void notifyMessageWaitingChanged(Phone sender) {
        int phoneId = sender.getPhoneId();
        int subId = sender.getSubId();
        mTelephonyRegistryMgr.notifyMessageWaitingChanged(phoneId, subId,
                sender.getMessageWaitingIndicator());
    }

    @Override
    public void notifyCallForwardingChanged(Phone sender) {
        int subId = sender.getSubId();
        Rlog.d(LOG_TAG, "notifyCallForwardingChanged: subId=" + subId + ", isCFActive="
            + sender.getCallForwardingIndicator());

        mTelephonyRegistryMgr.notifyCallForwardingChanged(subId,
            sender.getCallForwardingIndicator());
    }

    @Override
    public void notifyDataActivity(Phone sender) {

        int subId = sender.getSubId();

        int phoneId = sender.getPhoneId();
        mTelephonyRegistryMgr.notifyDataActivityChanged(phoneId, subId,
                sender.getDataActivityState());
    }

    @Override
    public void notifyDataConnection(Phone sender, PreciseDataConnectionState preciseState) {
        mTelephonyRegistryMgr.notifyDataConnectionForSubscriber(sender.getPhoneId(),
                sender.getSubId(), preciseState);
    }

    @Override
    public void notifyCellLocation(Phone sender, CellIdentity cellIdentity) {
        int subId = sender.getSubId();
        mTelephonyRegistryMgr.notifyCellLocation(subId, cellIdentity);
    }

    @Override
    public void notifyCellInfo(Phone sender, List<CellInfo> cellInfo) {
        int subId = sender.getSubId();
        mTelephonyRegistryMgr.notifyCellInfoChanged(subId, cellInfo);
    }

    /**
     * Notify precise call state of foreground, background and ringing call states.
     *
     * @param imsCallIds Array of IMS call session ID{@link ImsCallSession#getCallId} for
     *                   ringing, foreground & background calls.
     * @param imsCallServiceTypes Array of IMS call service type for ringing, foreground &
     *                        background calls.
     * @param imsCallTypes Array of IMS call type for ringing, foreground & background calls.
     */
    public void notifyPreciseCallState(Phone sender, String[] imsCallIds,
            @Annotation.ImsCallServiceType int[] imsCallServiceTypes,
            @Annotation.ImsCallType int[] imsCallTypes) {
        Call ringingCall = sender.getRingingCall();
        Call foregroundCall = sender.getForegroundCall();
        Call backgroundCall = sender.getBackgroundCall();

        if (ringingCall != null && foregroundCall != null && backgroundCall != null) {
            int[] callStates = {convertPreciseCallState(ringingCall.getState()),
                    convertPreciseCallState(foregroundCall.getState()),
                    convertPreciseCallState(backgroundCall.getState())};
            mTelephonyRegistryMgr.notifyPreciseCallState(sender.getPhoneId(), sender.getSubId(),
                    callStates, imsCallIds, imsCallServiceTypes, imsCallTypes);
        }
    }

    public void notifyDisconnectCause(Phone sender, int cause, int preciseCause) {
        mTelephonyRegistryMgr.notifyDisconnectCause(sender.getPhoneId(), sender.getSubId(), cause,
                preciseCause);
    }

    @Override
    public void notifyImsDisconnectCause(@NonNull Phone sender, ImsReasonInfo imsReasonInfo) {
        mTelephonyRegistryMgr.notifyImsDisconnectCause(sender.getSubId(), imsReasonInfo);
    }

    @Override
    public void notifySrvccStateChanged(Phone sender, @SrvccState int state) {
        mTelephonyRegistryMgr.notifySrvccStateChanged(sender.getSubId(), state);
    }

    @Override
    public void notifyDataActivationStateChanged(Phone sender, int activationState) {
        mTelephonyRegistryMgr.notifyDataActivationStateChanged(sender.getPhoneId(),
                sender.getSubId(), activationState);
    }

    @Override
    public void notifyVoiceActivationStateChanged(Phone sender, int activationState) {
        mTelephonyRegistryMgr.notifyVoiceActivationStateChanged(sender.getPhoneId(),
                sender.getSubId(), activationState);
    }

    @Override
    public void notifyUserMobileDataStateChanged(Phone sender, boolean state) {
        mTelephonyRegistryMgr.notifyUserMobileDataStateChanged(sender.getPhoneId(),
                sender.getSubId(), state);
    }

    @Override
    public void notifyDisplayInfoChanged(Phone sender, TelephonyDisplayInfo telephonyDisplayInfo) {
        mTelephonyRegistryMgr.notifyDisplayInfoChanged(sender.getPhoneId(), sender.getSubId(),
                telephonyDisplayInfo);
    }

    @Override
    public void notifyPhoneCapabilityChanged(PhoneCapability capability) {
        mTelephonyRegistryMgr.notifyPhoneCapabilityChanged(capability);
    }

    @Override
    public void notifyRadioPowerStateChanged(Phone sender, @RadioPowerState int state) {
        mTelephonyRegistryMgr.notifyRadioPowerStateChanged(sender.getPhoneId(), sender.getSubId(),
                state);
    }

    @Override
    public void notifyEmergencyNumberList(Phone sender) {
        mTelephonyRegistryMgr.notifyEmergencyNumberList(sender.getPhoneId(), sender.getSubId());
    }

    @Override
    public void notifyOutgoingEmergencySms(Phone sender, EmergencyNumber emergencyNumber) {
        mTelephonyRegistryMgr.notifyOutgoingEmergencySms(
                sender.getPhoneId(), sender.getSubId(), emergencyNumber);
    }

    @Override
    public void notifyCallQualityChanged(Phone sender, CallQuality callQuality,
            int callNetworkType) {
        mTelephonyRegistryMgr.notifyCallQualityChanged(sender.getPhoneId(), sender.getSubId(),
                callQuality, callNetworkType);
    }

    @Override
    public void notifyMediaQualityStatusChanged(Phone sender, MediaQualityStatus status) {
        mTelephonyRegistryMgr.notifyMediaQualityStatusChanged(
                sender.getPhoneId(), sender.getSubId(), status);
    }

    @Override
    public void notifyRegistrationFailed(Phone sender, @NonNull CellIdentity cellIdentity,
            @NonNull String chosenPlmn, int domain, int causeCode, int additionalCauseCode) {
        mTelephonyRegistryMgr.notifyRegistrationFailed(sender.getPhoneId(), sender.getSubId(),
                cellIdentity, chosenPlmn, domain, causeCode, additionalCauseCode);
    }

    @Override
    public void notifyBarringInfoChanged(Phone sender, BarringInfo barringInfo) {
        mTelephonyRegistryMgr.notifyBarringInfoChanged(sender.getPhoneId(), sender.getSubId(),
                barringInfo);
    }

    @Override
    public void notifyPhysicalChannelConfig(Phone sender,
                                                   List<PhysicalChannelConfig> configs) {
        mTelephonyRegistryMgr.notifyPhysicalChannelConfigForSubscriber(
                sender.getPhoneId(), sender.getSubId(), configs);
    }

    @Override
    public void notifyDataEnabled(Phone sender, boolean enabled, @DataEnabledReason int reason) {
        mTelephonyRegistryMgr.notifyDataEnabled(sender.getPhoneId(), sender.getSubId(),
                enabled, reason);
    }

    @Override
    public void notifyAllowedNetworkTypesChanged(Phone sender, int reason,
            long allowedNetworkType) {
        mTelephonyRegistryMgr.notifyAllowedNetworkTypesChanged(sender.getPhoneId(),
                sender.getSubId(), reason, allowedNetworkType);
    }

    @Override
    public void notifyLinkCapacityEstimateChanged(Phone sender,
            List<LinkCapacityEstimate> linkCapacityEstimateList) {
        mTelephonyRegistryMgr.notifyLinkCapacityEstimateChanged(sender.getPhoneId(),
                sender.getSubId(), linkCapacityEstimateList);
    }

    @Override
    public void notifySimultaneousCellularCallingSubscriptionsChanged(Set<Integer> subIds) {
        mTelephonyRegistryMgr.notifySimultaneousCellularCallingSubscriptionsChanged(subIds);
    }

    @Override
    public void notifyCallbackModeStarted(Phone sender, @EmergencyCallbackModeType int type,
            long durationMillis) {
        if (!mFeatureFlags.emergencyCallbackModeNotification()) return;

        mTelephonyRegistryMgr.notifyCallbackModeStarted(sender.getPhoneId(),
                sender.getSubId(), type, durationMillis);
    }

    @Override
    public void notifyCallbackModeRestarted(Phone sender, @EmergencyCallbackModeType int type,
            long durationMillis) {
        if (!mFeatureFlags.emergencyCallbackModeNotification()) return;

        mTelephonyRegistryMgr.notifyCallbackModeRestarted(sender.getPhoneId(),
                sender.getSubId(), type, durationMillis);
    }

    @Override
    public void notifyCallbackModeStopped(Phone sender, @EmergencyCallbackModeType int type,
            @EmergencyCallbackModeStopReason int reason) {
        if (!mFeatureFlags.emergencyCallbackModeNotification()) return;

        mTelephonyRegistryMgr.notifyCallbackModeStopped(sender.getPhoneId(),
                sender.getSubId(), type, reason);
    }

    @Override
    public void notifyCarrierRoamingNtnModeChanged(Phone sender, boolean active) {
        mTelephonyRegistryMgr.notifyCarrierRoamingNtnModeChanged(sender.getSubId(), active);
    }

    @Override
    public void notifyCarrierRoamingNtnEligibleStateChanged(Phone sender, boolean eligible) {
        mTelephonyRegistryMgr.notifyCarrierRoamingNtnEligibleStateChanged(
                sender.getSubId(), eligible);
    }

    @Override
    public void notifyCarrierRoamingNtnAvailableServicesChanged(
            Phone sender, @NetworkRegistrationInfo.ServiceType int[] availableServices) {
        mTelephonyRegistryMgr.notifyCarrierRoamingNtnAvailableServicesChanged(
                sender.getSubId(), availableServices);
    }

    @Override
    public void notifyCarrierRoamingNtnSignalStrengthChanged(Phone sender,
            @NonNull NtnSignalStrength ntnSignalStrength) {
        mTelephonyRegistryMgr.notifyCarrierRoamingNtnSignalStrengthChanged(
                sender.getSubId(), ntnSignalStrength);
    }

    @Override
    public void notifySecurityAlgorithmsChanged(Phone sender, SecurityAlgorithmUpdate update) {
        if (!mFeatureFlags.securityAlgorithmsUpdateIndications()) return;

        mTelephonyRegistryMgr.notifySecurityAlgorithmsChanged(sender.getPhoneId(),
                sender.getSubId(), update);
    }

    @Override
    public void notifyCellularIdentifierDisclosedChanged(Phone sender,
            CellularIdentifierDisclosure disclosure) {
        if (!mFeatureFlags.cellularIdentifierDisclosureIndications()) return;

        mTelephonyRegistryMgr.notifyCellularIdentifierDisclosedChanged(sender.getPhoneId(),
                sender.getSubId(), disclosure);
    }

    /**
     * Convert the {@link Call.State} enum into the PreciseCallState.PRECISE_CALL_STATE_* constants
     * for the public API.
     */
    public static int convertPreciseCallState(Call.State state) {
        switch (state) {
            case ACTIVE:
                return PreciseCallState.PRECISE_CALL_STATE_ACTIVE;
            case HOLDING:
                return PreciseCallState.PRECISE_CALL_STATE_HOLDING;
            case DIALING:
                return PreciseCallState.PRECISE_CALL_STATE_DIALING;
            case ALERTING:
                return PreciseCallState.PRECISE_CALL_STATE_ALERTING;
            case INCOMING:
                return PreciseCallState.PRECISE_CALL_STATE_INCOMING;
            case WAITING:
                return PreciseCallState.PRECISE_CALL_STATE_WAITING;
            case DISCONNECTED:
                return PreciseCallState.PRECISE_CALL_STATE_DISCONNECTED;
            case DISCONNECTING:
                return PreciseCallState.PRECISE_CALL_STATE_DISCONNECTING;
            default:
                return PreciseCallState.PRECISE_CALL_STATE_IDLE;
        }
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, s);
    }
}

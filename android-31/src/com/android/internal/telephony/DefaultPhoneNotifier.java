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
import android.telephony.Annotation.RadioPowerState;
import android.telephony.Annotation.SrvccState;
import android.telephony.BarringInfo;
import android.telephony.CallQuality;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.LinkCapacityEstimate;
import android.telephony.PhoneCapability;
import android.telephony.PhysicalChannelConfig;
import android.telephony.PreciseCallState;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.DataEnabledReason;
import android.telephony.TelephonyRegistryManager;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.ImsReasonInfo;

import com.android.internal.telephony.PhoneInternalInterface.DataActivityState;
import com.android.telephony.Rlog;

import java.util.List;

/**
 * broadcast intents
 */
public class DefaultPhoneNotifier implements PhoneNotifier {

    private static final String LOG_TAG = "DefaultPhoneNotifier";
    private static final boolean DBG = false; // STOPSHIP if true

    private TelephonyRegistryManager mTelephonyRegistryMgr;


    public DefaultPhoneNotifier(Context context) {
        mTelephonyRegistryMgr = (TelephonyRegistryManager) context.getSystemService(
            Context.TELEPHONY_REGISTRY_SERVICE);
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
        mTelephonyRegistryMgr.notifyDataActivityChanged(subId,
                convertDataActivityState(sender.getDataActivityState()));
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

    public void notifyPreciseCallState(Phone sender) {
        Call ringingCall = sender.getRingingCall();
        Call foregroundCall = sender.getForegroundCall();
        Call backgroundCall = sender.getBackgroundCall();
        if (ringingCall != null && foregroundCall != null && backgroundCall != null) {
            mTelephonyRegistryMgr.notifyPreciseCallState(sender.getPhoneId(), sender.getSubId(),
                    convertPreciseCallState(ringingCall.getState()),
                    convertPreciseCallState(foregroundCall.getState()),
                    convertPreciseCallState(backgroundCall.getState()));
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

    /**
     * Convert the {@link DataActivityState} enum into the TelephonyManager.DATA_* constants for the
     * public API.
     */
    public static int convertDataActivityState(DataActivityState state) {
        switch (state) {
            case DATAIN:
                return TelephonyManager.DATA_ACTIVITY_IN;
            case DATAOUT:
                return TelephonyManager.DATA_ACTIVITY_OUT;
            case DATAINANDOUT:
                return TelephonyManager.DATA_ACTIVITY_INOUT;
            case DORMANT:
                return TelephonyManager.DATA_ACTIVITY_DORMANT;
            default:
                return TelephonyManager.DATA_ACTIVITY_NONE;
        }
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

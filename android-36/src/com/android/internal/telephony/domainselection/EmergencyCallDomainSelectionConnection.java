/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.telephony.domainselection;

import static android.telephony.AccessNetworkConstants.TRANSPORT_TYPE_INVALID;
import static android.telephony.AccessNetworkConstants.TRANSPORT_TYPE_WLAN;
import static android.telephony.AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
import static android.telephony.DomainSelectionService.SELECTOR_TYPE_CALLING;
import static android.telephony.NetworkRegistrationInfo.DOMAIN_PS;

import static com.android.internal.telephony.PhoneConstants.DOMAIN_NON_3GPP_PS;
import static com.android.internal.telephony.emergency.EmergencyConstants.MODE_EMERGENCY_WLAN;
import static com.android.internal.telephony.emergency.EmergencyConstants.MODE_EMERGENCY_WWAN;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;
import android.telecom.PhoneAccount;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.DisconnectCauses;
import android.telephony.Annotation.NetCapability;
import android.telephony.DomainSelectionService;
import android.telephony.EmergencyRegistrationResult;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PreciseDisconnectCause;
import android.telephony.data.ApnSetting;
import android.telephony.ims.ImsReasonInfo;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.data.AccessNetworksManager;
import com.android.internal.telephony.data.AccessNetworksManager.QualifiedNetworks;
import com.android.internal.telephony.emergency.EmergencyStateTracker;
import com.android.internal.telephony.flags.Flags;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages the information of request and the callback binder for emergency calling.
 */
public class EmergencyCallDomainSelectionConnection extends DomainSelectionConnection {

    private static final boolean DBG = false;

    private @NonNull EmergencyStateTracker mEmergencyStateTracker = null;
    private @Nullable DomainSelectionConnectionCallback mCallback;
    private @TransportType int mPreferredTransportType = TRANSPORT_TYPE_INVALID;

    /**
     * Create an instance.
     *
     * @param phone For which this service is requested.
     * @param controller The controller to communicate with the domain selection service.
     */
    public EmergencyCallDomainSelectionConnection(@NonNull Phone phone,
            @NonNull DomainSelectionController controller) {
        this(phone, controller, EmergencyStateTracker.getInstance());
    }

    /**
     * Create an instance.
     *
     * @param phone For which this service is requested.
     * @param controller The controller to communicate with the domain selection service.
     * @param tracker The {@link EmergencyStateTracker} instance.
     */
    @VisibleForTesting
    public EmergencyCallDomainSelectionConnection(@NonNull Phone phone,
            @NonNull DomainSelectionController controller, @NonNull EmergencyStateTracker tracker) {
        super(phone, SELECTOR_TYPE_CALLING, true, controller);
        mTag = "EmergencyCallDomainSelectionConnection";

        mEmergencyStateTracker = tracker;
    }

    /** {@inheritDoc} */
    @Override
    public void onWlanSelected(boolean useEmergencyPdn) {
        mEmergencyStateTracker.onEmergencyTransportChanged(
                EmergencyStateTracker.EMERGENCY_TYPE_CALL, MODE_EMERGENCY_WLAN);
        if (useEmergencyPdn) {
            AccessNetworksManager anm = mPhone.getAccessNetworksManager();
            int transportType = anm.getPreferredTransport(ApnSetting.TYPE_EMERGENCY);
            logi("onWlanSelected curTransportType=" + transportType);
            if (transportType != TRANSPORT_TYPE_WLAN) {
                changePreferredTransport(TRANSPORT_TYPE_WLAN);
                return;
            }
        }

        CompletableFuture<Integer> future = getCompletableFuture();
        if (future != null) future.complete(DOMAIN_NON_3GPP_PS);
    }

    /** {@inheritDoc} */
    @Override
    public void onWwanSelected() {
        mEmergencyStateTracker.onEmergencyTransportChangedAndWait(
                EmergencyStateTracker.EMERGENCY_TYPE_CALL, MODE_EMERGENCY_WWAN);
    }

    /** {@inheritDoc} */
    @Override
    public void onSelectionTerminated(@DisconnectCauses int cause) {
        if (mCallback != null) mCallback.onSelectionTerminated(cause);
    }

    /** {@inheritDoc} */
    @Override
    public void onDomainSelected(@NetworkRegistrationInfo.Domain int domain,
            boolean useEmergencyPdn) {
        if (domain == DOMAIN_PS && useEmergencyPdn) {
            AccessNetworksManager anm = mPhone.getAccessNetworksManager();
            int transportType = anm.getPreferredTransport(ApnSetting.TYPE_EMERGENCY);
            logi("onDomainSelected curTransportType=" + transportType);
            if (transportType != TRANSPORT_TYPE_WWAN) {
                changePreferredTransport(TRANSPORT_TYPE_WWAN);
                return;
            }
        }
        super.onDomainSelected(domain, useEmergencyPdn);
    }

    /**
     * Request a domain for emergency call.
     *
     * @param attr The attributes required to determine the domain.
     * @param callback A callback to receive the response.
     * @return the callback to receive the response.
     */
    public @NonNull CompletableFuture<Integer> createEmergencyConnection(
            @NonNull DomainSelectionService.SelectionAttributes attr,
            @NonNull DomainSelectionConnectionCallback callback) {
        mCallback = callback;
        selectDomain(attr);
        return getCompletableFuture();
    }

    private void changePreferredTransport(@TransportType int transportType) {
        logi("changePreferredTransport " + transportType);
        initHandler();
        mPreferredTransportType = transportType;
        AccessNetworksManager anm = mPhone.getAccessNetworksManager();
        anm.registerForQualifiedNetworksChanged(mHandler, EVENT_QUALIFIED_NETWORKS_CHANGED);
        mPhone.notifyEmergencyDomainSelected(transportType);
    }

    private AccessNetworksManager.AccessNetworksManagerCallback mPreferredTransportCallback =
            new AccessNetworksManager.AccessNetworksManagerCallback(Runnable::run) {
        @Override
        public void onPreferredTransportChanged(
                @NetCapability int capability, boolean forceReconnect) {
        }
    };

    /** {@inheritDoc} */
    @Override
    protected void onQualifiedNetworksChanged(List<QualifiedNetworks> networksList) {
        int preferredTransport = getPreferredTransport(ApnSetting.TYPE_EMERGENCY, networksList);
        logi("onQualifiedNetworksChanged preferred=" + mPreferredTransportType
                + ", current=" + preferredTransport);
        if (preferredTransport == mPreferredTransportType) {
            CompletableFuture<Integer> future = getCompletableFuture();
            if (future != null) {
                if (preferredTransport == TRANSPORT_TYPE_WLAN) {
                    future.complete(DOMAIN_NON_3GPP_PS);
                } else {
                    future.complete(DOMAIN_PS);
                }
            }
            AccessNetworksManager anm = mPhone.getAccessNetworksManager();
            anm.unregisterForQualifiedNetworksChanged(mHandler);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void cancelSelection() {
        logi("cancelSelection");
        AccessNetworksManager anm = mPhone.getAccessNetworksManager();
        anm.unregisterForQualifiedNetworksChanged(mHandler);
        super.cancelSelection();
    }

    @Override
    public @NonNull CompletableFuture<Integer> reselectDomain(
        @NonNull DomainSelectionService.SelectionAttributes attr) {
        if (Flags.hangupEmergencyCallForCrossSimRedialing()) {
            int disconnectCause = getDisconnectCause();
            int preciseDisconnectCause = attr.getCsDisconnectCause();
            if (disconnectCause == android.telephony.DisconnectCause.EMERGENCY_TEMP_FAILURE) {
                preciseDisconnectCause = PreciseDisconnectCause.EMERGENCY_TEMP_FAILURE;
            } else if (disconnectCause
                    == android.telephony.DisconnectCause.EMERGENCY_PERM_FAILURE) {
                preciseDisconnectCause = PreciseDisconnectCause.EMERGENCY_PERM_FAILURE;
            }
            if (preciseDisconnectCause != attr.getCsDisconnectCause()) {
                attr = EmergencyCallDomainSelectionConnection.getSelectionAttributes(
                        attr.getSlotIndex(), attr.getSubscriptionId(),
                        attr.isExitedFromAirplaneMode(), attr.getCallId(),
                        (attr.getAddress() != null)
                        ? attr.getAddress().getSchemeSpecificPart() : "",
                        attr.isTestEmergencyNumber(), preciseDisconnectCause,
                        attr.getPsDisconnectCause(),
                        attr.getEmergencyRegistrationResult());
            }
        }
        return super.reselectDomain(attr);
    }

    /**
     * Returns the attributes required to determine the domain for a telephony service.
     *
     * @param slotId The slot identifier.
     * @param subId The subscription identifier.
     * @param exited {@code true} if the request caused the device to move out of airplane mode.
     * @param callId The call identifier.
     * @param number The dialed number.
     * @param isTest Indicates it's a test emergency number.
     * @param callFailCause The reason why the last CS attempt failed.
     * @param imsReasonInfo The reason why the last PS attempt failed.
     * @param emergencyRegResult The current registration result for emergency services.
     * @return The attributes required to determine the domain.
     */
    public static @NonNull DomainSelectionService.SelectionAttributes getSelectionAttributes(
            int slotId, int subId, boolean exited,
            @NonNull String callId, @NonNull String number, boolean isTest,
            int callFailCause, @Nullable ImsReasonInfo imsReasonInfo,
            @Nullable EmergencyRegistrationResult emergencyRegResult) {

        int preciseDisconnectCause = callFailCause;
        switch (callFailCause) {
            case CallFailCause.IMS_EMERGENCY_TEMP_FAILURE:
                preciseDisconnectCause = PreciseDisconnectCause.EMERGENCY_TEMP_FAILURE;
                break;
            case CallFailCause.IMS_EMERGENCY_PERM_FAILURE:
                preciseDisconnectCause = PreciseDisconnectCause.EMERGENCY_PERM_FAILURE;
                break;
            default:
                break;
        }

        DomainSelectionService.SelectionAttributes.Builder builder =
                new DomainSelectionService.SelectionAttributes.Builder(
                        slotId, subId, SELECTOR_TYPE_CALLING)
                .setEmergency(true)
                .setTestEmergencyNumber(isTest)
                .setExitedFromAirplaneMode(exited)
                .setCallId(callId)
                .setAddress(Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null))
                .setCsDisconnectCause(preciseDisconnectCause);

        if (imsReasonInfo != null) builder.setPsDisconnectCause(imsReasonInfo);
        if (emergencyRegResult != null) builder.setEmergencyRegistrationResult(emergencyRegResult);

        return builder.build();
    }

    @Override
    protected DomainSelectionService.SelectionAttributes
            getSelectionAttributesToRebindService() {
        DomainSelectionService.SelectionAttributes attr = getSelectionAttributes();
        if (attr == null) return null;
        DomainSelectionService.SelectionAttributes.Builder builder =
                new DomainSelectionService.SelectionAttributes.Builder(
                        attr.getSlotIndex(), attr.getSubscriptionId(), SELECTOR_TYPE_CALLING)
                .setCallId(attr.getCallId())
                .setAddress(attr.getAddress())
                .setVideoCall(attr.isVideoCall())
                .setEmergency(true)
                .setTestEmergencyNumber(attr.isTestEmergencyNumber())
                .setExitedFromAirplaneMode(attr.isExitedFromAirplaneMode())
                .setEmergencyRegistrationResult(
                        new EmergencyRegistrationResult(AccessNetworkType.UNKNOWN,
                        NetworkRegistrationInfo.REGISTRATION_STATE_UNKNOWN,
                        NetworkRegistrationInfo.DOMAIN_UNKNOWN, false, false, 0, 0,
                        "", "", ""));
        return builder.build();
    }
}

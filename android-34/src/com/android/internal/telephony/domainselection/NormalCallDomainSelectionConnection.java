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

import static android.telephony.DomainSelectionService.SELECTOR_TYPE_CALLING;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.telephony.AccessNetworkConstants.RadioAccessNetworkType;
import android.telephony.Annotation.DisconnectCauses;
import android.telephony.DomainSelectionService;
import android.telephony.DomainSelectionService.EmergencyScanType;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ims.ImsReasonInfo;

import com.android.internal.telephony.Phone;

import java.util.concurrent.CompletableFuture;

/**
 * Manages the information of request and the callback binder for normal calling.
 */
public class NormalCallDomainSelectionConnection extends DomainSelectionConnection {

    private static final boolean DBG = false;

    private static final String PREFIX_WPS = "*272";

    // WPS prefix when CLIR is being activated for the call.
    private static final String PREFIX_WPS_CLIR_ACTIVATE = "*31#*272";

    // WPS prefix when CLIR is being deactivated for the call.
    private static final String PREFIX_WPS_CLIR_DEACTIVATE = "#31#*272";


    private @Nullable DomainSelectionConnectionCallback mCallback;

    /**
     * Create an instance.
     *
     * @param phone For which this service is requested.
     * @param controller The controller to communicate with the domain selection service.
     */
    public NormalCallDomainSelectionConnection(@NonNull Phone phone,
            @NonNull DomainSelectionController controller) {
        super(phone, SELECTOR_TYPE_CALLING, false, controller);
        mTag = "NormalCallDomainSelectionConnection";
    }

    /** {@inheritDoc} */
    @Override
    public void onWlanSelected() {
        CompletableFuture<Integer> future = getCompletableFuture();
        future.complete(NetworkRegistrationInfo.DOMAIN_PS);
    }

    /** {@inheritDoc} */
    @Override
    public void onWwanSelected() {
    }

    /** {@inheritDoc} */
    @Override
    public void onSelectionTerminated(@DisconnectCauses int cause) {
        if (mCallback != null) mCallback.onSelectionTerminated(cause);
    }

    /** {@inheritDoc} */
    @Override
    public void onRequestEmergencyNetworkScan(@RadioAccessNetworkType int[] preferredNetworks,
            @EmergencyScanType int scanType) {
        // Not expected with normal calling.
        // Override to prevent abnormal behavior.
    }

    /**
     * Request a domain for normal call.
     *
     * @param attr The attributes required to determine the domain.
     * @param callback A callback to receive the response.
     * @return A {@link CompletableFuture} callback to receive the result.
     */
    public CompletableFuture<Integer> createNormalConnection(
            @NonNull DomainSelectionService.SelectionAttributes attr,
            @NonNull DomainSelectionConnectionCallback callback) {
        mCallback = callback;
        selectDomain(attr);
        return getCompletableFuture();
    }

    /**
     * Returns the attributes required to determine the domain for a normal call.
     *
     * @param slotId The slot identifier.
     * @param subId The subscription identifier.
     * @param callId The call identifier.
     * @param number The dialed number.
     * @param isVideoCall flag for video call.
     * @param callFailCause The reason why the last CS attempt failed.
     * @param imsReasonInfo The reason why the last PS attempt failed.
     * @return The attributes required to determine the domain.
     */
    public static @NonNull DomainSelectionService.SelectionAttributes getSelectionAttributes(
            int slotId, int subId, @NonNull String callId, @NonNull String number,
            boolean isVideoCall, int callFailCause, @Nullable ImsReasonInfo imsReasonInfo) {

        DomainSelectionService.SelectionAttributes.Builder builder =
                new DomainSelectionService.SelectionAttributes.Builder(
                        slotId, subId, SELECTOR_TYPE_CALLING)
                        .setEmergency(false)
                        .setCallId(callId)
                        .setNumber(number)
                        .setCsDisconnectCause(callFailCause)
                        .setVideoCall(isVideoCall);

        if (imsReasonInfo != null) {
            builder.setPsDisconnectCause(imsReasonInfo);
        }
        return builder.build();
    }

    /**
     * Check if the call is Wireless Priority Service call
     * @param dialString  The number being dialed.
     * @return {@code true} if dialString matches WPS pattern and {@code false} otherwise.
     */
    public static boolean isWpsCall(String dialString) {
        return (dialString != null) && (dialString.startsWith(PREFIX_WPS)
                || dialString.startsWith(PREFIX_WPS_CLIR_ACTIVATE)
                || dialString.startsWith(PREFIX_WPS_CLIR_DEACTIVATE));
    }
}

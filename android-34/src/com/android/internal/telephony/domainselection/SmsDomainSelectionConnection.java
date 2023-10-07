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

import static android.telephony.DomainSelectionService.SELECTOR_TYPE_SMS;

import android.annotation.NonNull;
import android.telephony.Annotation.DisconnectCauses;
import android.telephony.DomainSelectionService;
import android.telephony.NetworkRegistrationInfo;

import com.android.internal.telephony.Phone;

import java.util.concurrent.CompletableFuture;

/**
 * Manages the information of request and the callback binder for SMS.
 */
public class SmsDomainSelectionConnection extends DomainSelectionConnection {
    private DomainSelectionConnectionCallback mCallback;

    public SmsDomainSelectionConnection(Phone phone, DomainSelectionController controller) {
        this(phone, controller, false);
        mTag = "DomainSelectionConnection-SMS";
    }

    protected SmsDomainSelectionConnection(Phone phone, DomainSelectionController controller,
            boolean isEmergency) {
        super(phone, SELECTOR_TYPE_SMS, isEmergency, controller);
    }

    @Override
    public void onWlanSelected() {
        super.onDomainSelected(NetworkRegistrationInfo.DOMAIN_PS);
    }

    @Override
    public void onSelectionTerminated(@DisconnectCauses int cause) {
        if (mCallback != null) mCallback.onSelectionTerminated(cause);
    }

    @Override
    public void finishSelection() {
        CompletableFuture<Integer> future = getCompletableFuture();

        if (future != null && !future.isDone()) {
            cancelSelection();
        } else {
            super.finishSelection();
        }
    }

    /**
     * Requests a domain selection for SMS.
     *
     * @param attr The attributes required to determine the domain.
     * @param callback A callback to notify an error of the domain selection.
     * @return A {@link CompletableFuture} to get the selected domain
     *         {@link NetworkRegistrationInfo#DOMAIN_PS} or
     *         {@link NetworkRegistrationInfo#DOMAIN_CS}.
     */
    public @NonNull CompletableFuture<Integer> requestDomainSelection(
            @NonNull DomainSelectionService.SelectionAttributes attr,
            @NonNull DomainSelectionConnectionCallback callback) {
        mCallback = callback;
        selectDomain(attr);
        return getCompletableFuture();
    }
}

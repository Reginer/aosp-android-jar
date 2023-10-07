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

import static com.android.internal.telephony.emergency.EmergencyConstants.MODE_EMERGENCY_WLAN;
import static com.android.internal.telephony.emergency.EmergencyConstants.MODE_EMERGENCY_WWAN;

import android.annotation.NonNull;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.data.ApnSetting;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.data.AccessNetworksManager;
import com.android.internal.telephony.emergency.EmergencyStateTracker;

/**
 * Manages the information of request and the callback binder for an emergency SMS.
 */
public class EmergencySmsDomainSelectionConnection extends SmsDomainSelectionConnection {
    private final Object mLock = new Object();
    private @NonNull EmergencyStateTracker mEmergencyStateTracker;
    private @TransportType int mPreferredTransportType =
            AccessNetworkConstants.TRANSPORT_TYPE_INVALID;

    public EmergencySmsDomainSelectionConnection(
            Phone phone, DomainSelectionController controller) {
        this(phone, controller, EmergencyStateTracker.getInstance());
    }

    @VisibleForTesting
    public EmergencySmsDomainSelectionConnection(Phone phone,
            DomainSelectionController controller, EmergencyStateTracker tracker) {
        super(phone, controller, true);
        mTag = "DomainSelectionConnection-EmergencySMS";
        mEmergencyStateTracker = tracker;
    }

    /**
     * Notifies that WLAN transport has been selected.
     *
     * @param useEmergencyPdn A flag specifying whether Wi-Fi emergency service uses emergency PDN
     *                        or not.
     */
    @Override
    public void onWlanSelected(boolean useEmergencyPdn) {
        synchronized (mLock) {
            if (mPreferredTransportType != AccessNetworkConstants.TRANSPORT_TYPE_INVALID) {
                logi("Domain selection completion is in progress");
                return;
            }

            mEmergencyStateTracker.onEmergencyTransportChanged(
                    EmergencyStateTracker.EMERGENCY_TYPE_SMS, MODE_EMERGENCY_WLAN);

            if (useEmergencyPdn) {
                // Change the transport type if the current preferred transport type for
                // an emergency is not {@link AccessNetworkConstants#TRANSPORT_TYPE_WLAN}.
                AccessNetworksManager anm = mPhone.getAccessNetworksManager();
                if (anm.getPreferredTransport(ApnSetting.TYPE_EMERGENCY)
                        != AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                    changePreferredTransport(AccessNetworkConstants.TRANSPORT_TYPE_WLAN);
                    // The {@link #onDomainSlected()} will be called after the preferred transport
                    // is successfully changed and notified from the {@link AccessNetworksManager}.
                    return;
                }
            }

            super.onWlanSelected(useEmergencyPdn);
        }
    }

    @Override
    public void onWwanSelected() {
        mEmergencyStateTracker.onEmergencyTransportChanged(
                EmergencyStateTracker.EMERGENCY_TYPE_SMS, MODE_EMERGENCY_WWAN);
    }

    /**
     * Notifies the domain selected.
     *
     * @param domain The selected domain.
     * @param useEmergencyPdn A flag specifying whether emergency service uses emergency PDN or not.
     */
    @Override
    public void onDomainSelected(@NetworkRegistrationInfo.Domain int domain,
            boolean useEmergencyPdn) {
        synchronized (mLock) {
            if (mPreferredTransportType != AccessNetworkConstants.TRANSPORT_TYPE_INVALID) {
                logi("Domain selection completion is in progress");
                return;
            }

            if (useEmergencyPdn && domain == NetworkRegistrationInfo.DOMAIN_PS) {
                // Change the transport type if the current preferred transport type for
                // an emergency is not {@link AccessNetworkConstants#TRANSPORT_TYPE_WWAN}.
                AccessNetworksManager anm = mPhone.getAccessNetworksManager();
                if (anm.getPreferredTransport(ApnSetting.TYPE_EMERGENCY)
                        != AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                    changePreferredTransport(AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
                    // The {@link #onDomainSlected()} will be called after the preferred transport
                    // is successfully changed and notified from the {@link AccessNetworksManager}.
                    return;
                }
            }

            super.onDomainSelected(domain, useEmergencyPdn);
        }
    }

    @Override
    public void finishSelection() {
        AccessNetworksManager anm = mPhone.getAccessNetworksManager();

        synchronized (mLock) {
            if (mPreferredTransportType != AccessNetworkConstants.TRANSPORT_TYPE_INVALID) {
                mPreferredTransportType = AccessNetworkConstants.TRANSPORT_TYPE_INVALID;
                anm.unregisterForQualifiedNetworksChanged(mHandler);
            }
        }

        super.finishSelection();
    }

    @Override
    protected void onQualifiedNetworksChanged() {
        AccessNetworksManager anm = mPhone.getAccessNetworksManager();
        int preferredTransportType = anm.getPreferredTransport(ApnSetting.TYPE_EMERGENCY);

        synchronized (mLock) {
            if (preferredTransportType == mPreferredTransportType) {
                mPreferredTransportType = AccessNetworkConstants.TRANSPORT_TYPE_INVALID;
                super.onDomainSelected(NetworkRegistrationInfo.DOMAIN_PS, true);
                anm.unregisterForQualifiedNetworksChanged(mHandler);
            }
        }
    }

    private void changePreferredTransport(@TransportType int transportType) {
        logi("Change preferred transport: " + transportType);
        initHandler();
        mPreferredTransportType = transportType;
        AccessNetworksManager anm = mPhone.getAccessNetworksManager();
        anm.registerForQualifiedNetworksChanged(mHandler, EVENT_QUALIFIED_NETWORKS_CHANGED);
        mPhone.notifyEmergencyDomainSelected(transportType);
    }
}

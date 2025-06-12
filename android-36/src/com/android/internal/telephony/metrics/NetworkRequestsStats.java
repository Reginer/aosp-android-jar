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

package com.android.internal.telephony.metrics;

import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.nano.PersistAtomsProto.NetworkRequestsV2;


/** Metrics for the network requests. */
public class NetworkRequestsStats {
    private NetworkRequestsStats() { }

    /** Generate metrics when network request occurs. */
    public static void addNetworkRequest(NetworkRequest networkRequest, int subId) {
        PersistAtomsStorage storage = PhoneFactory.getMetricsCollector().getAtomsStorage();

        NetworkRequestsV2 networkRequestsTemplate = new NetworkRequestsV2();
        networkRequestsTemplate.carrierId = getCarrierId(subId);
        networkRequestsTemplate.requestCount = 1;

        if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_LATENCY)) {
            networkRequestsTemplate.capability =
                    NetworkRequestsV2.NetworkCapability.PRIORITIZE_LATENCY;
            storage.addNetworkRequestsV2(networkRequestsTemplate);
        }

        if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_BANDWIDTH)) {
            networkRequestsTemplate.capability =
                    NetworkRequestsV2.NetworkCapability.PRIORITIZE_BANDWIDTH;
            storage.addNetworkRequestsV2(networkRequestsTemplate);
        }

        if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_CBS)) {
            networkRequestsTemplate.capability = NetworkRequestsV2.NetworkCapability.CBS;
            storage.addNetworkRequestsV2(networkRequestsTemplate);
        }

        if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE)) {
            networkRequestsTemplate.capability = NetworkRequestsV2.NetworkCapability.ENTERPRISE;
            storage.addNetworkRequestsV2(networkRequestsTemplate);
        }

        if (networkRequest.hasTransport(NetworkCapabilities.TRANSPORT_SATELLITE)
                && !networkRequest.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)) {

            if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                networkRequestsTemplate.capability =
                        NetworkRequestsV2.NetworkCapability.SATELLITE_INTERNET_RESTRICTED;
                storage.addNetworkRequestsV2(networkRequestsTemplate);
            }

            if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS)) {
                networkRequestsTemplate.capability =
                        NetworkRequestsV2.NetworkCapability.SATELLITE_MMS_RESTRICTED;
                storage.addNetworkRequestsV2(networkRequestsTemplate);
            }

            if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_IMS)) {
                networkRequestsTemplate.capability =
                        NetworkRequestsV2.NetworkCapability.SATELLITE_IMS_RESTRICTED;
                storage.addNetworkRequestsV2(networkRequestsTemplate);
            }

            if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_XCAP)) {
                networkRequestsTemplate.capability =
                        NetworkRequestsV2.NetworkCapability.SATELLITE_XCAP_RESTRICTED;
                storage.addNetworkRequestsV2(networkRequestsTemplate);
            }

            if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_EIMS)) {
                networkRequestsTemplate.capability =
                        NetworkRequestsV2.NetworkCapability.SATELLITE_EIMS_RESTRICTED;
                storage.addNetworkRequestsV2(networkRequestsTemplate);
            }

            if (networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_SUPL)) {
                networkRequestsTemplate.capability =
                        NetworkRequestsV2.NetworkCapability.SATELLITE_SUPL_RESTRICTED;
                storage.addNetworkRequestsV2(networkRequestsTemplate);
            }
        }
    }

    /** Returns the carrier ID of the given subscription id. */
    private static int getCarrierId(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        Phone phone = PhoneFactory.getPhone(phoneId);
        return phone != null ? phone.getCarrierId() : TelephonyManager.UNKNOWN_CARRIER_ID;
    }
}

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
import com.android.internal.telephony.nano.PersistAtomsProto.NetworkRequests;


/** Metrics for the network requests. */
public class NetworkRequestsStats {
    private NetworkRequestsStats() { }

    /** Generate metrics when network request occurs. */
    public static void addNetworkRequest(NetworkRequest networkRequest, int subId) {
        if (!networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE)) {
            // Currently only handle enterprise
            return;
        }
        NetworkRequests networkRequests = new NetworkRequests();
        networkRequests.carrierId = getCarrierId(subId);
        networkRequests.enterpriseRequestCount = 1;

        PersistAtomsStorage storage = PhoneFactory.getMetricsCollector().getAtomsStorage();
        storage.addNetworkRequests(networkRequests);
    }

    /** Generate metrics when network release occurs. */
    public static void addNetworkRelease(NetworkRequest networkRequest, int subId) {
        if (!networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE)) {
            // Currently only handle enterprise
            return;
        }
        NetworkRequests networkRequests = new NetworkRequests();
        networkRequests.carrierId = getCarrierId(subId);
        networkRequests.enterpriseReleaseCount = 1;

        PersistAtomsStorage storage = PhoneFactory.getMetricsCollector().getAtomsStorage();
        storage.addNetworkRequests(networkRequests);
    }

    /** Returns the carrier ID of the given subscription id. */
    private static int getCarrierId(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        Phone phone = PhoneFactory.getPhone(phoneId);
        return phone != null ? phone.getCarrierId() : TelephonyManager.UNKNOWN_CARRIER_ID;
    }
}

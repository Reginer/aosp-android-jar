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
package com.android.internal.telephony.dataconnection;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.TelephonyNetworkSpecifier;
import android.telephony.Annotation.ApnType;

import com.android.telephony.Rlog;

/**
 * Wraps cellular network requests to configured apn types.
 */
public class DcRequest implements Comparable<DcRequest> {
    private static final String LOG_TAG = "DcRequest";

    @NonNull
    public final NetworkRequest networkRequest;
    public final int priority;
    public final @ApnType int apnType;

    private DcRequest(@NonNull final NetworkRequest nr, @ApnType final int type,
            int apnPriority) {
        networkRequest = nr;
        priority = apnPriority;
        apnType = type;
    }

    /**
     * Create a DcRequest based off of the network request.  If the network request is not cellular,
     * then null is returned and a warning is generated.
     * @param networkRequest sets the type of dc request
     * @param apnConfigTypeRepository apn config types to match on the network request
     * @return corresponding DcRequest
     *
     */
    @Nullable
    public static DcRequest create(@NonNull final NetworkRequest networkRequest,
            @NonNull final ApnConfigTypeRepository apnConfigTypeRepository) {
        final int apnType = ApnContext.getApnTypeFromNetworkRequest(networkRequest);
        final ApnConfigType apnConfigType = apnConfigTypeRepository.getByType(apnType);
        if (apnConfigType == null) {
            Rlog.d(LOG_TAG, "Non cellular request ignored: " + networkRequest.toString());
            checkForAnomalousNetworkRequest(networkRequest);
            return null;
        } else {
            Rlog.d(LOG_TAG, "Cellular request confirmed: " + networkRequest.toString());
            return new DcRequest(networkRequest, apnType, apnConfigType.getPriority());
        }
    }

    private static void checkForAnomalousNetworkRequest(NetworkRequest networkRequest) {
        NetworkSpecifier specifier = networkRequest.getNetworkSpecifier();
        if (specifier != null) {
            if (specifier instanceof TelephonyNetworkSpecifier) {
                reportAnomalousNetworkRequest(networkRequest);
            }
        }
    }

    private static void reportAnomalousNetworkRequest(NetworkRequest networkRequest) {
        //TODO: Report anomaly if this happens
        Rlog.w(LOG_TAG, "A TelephonyNetworkSpecifier for a non-cellular request is invalid: "
                + networkRequest.toString());

    }

    public String toString() {
        return networkRequest.toString() + ", priority=" + priority + ", apnType=" + apnType;
    }

    public int hashCode() {
        return networkRequest.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof DcRequest) {
            return networkRequest.equals(((DcRequest)o).networkRequest);
        }
        return false;
    }

    public int compareTo(DcRequest o) {
        return o.priority - priority;
    }
}

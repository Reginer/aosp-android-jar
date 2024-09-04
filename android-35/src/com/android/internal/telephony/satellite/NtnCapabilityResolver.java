/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.internal.telephony.satellite;

import android.annotation.NonNull;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.Rlog;
import android.text.TextUtils;

import java.util.List;

/**
 * This utility class is responsible for resolving NTN capabilities of a
 * {@link NetworkRegistrationInfo}.
 */
public class NtnCapabilityResolver {
    private static final String TAG = "NtnCapabilityResolver";

    /**
     * Resolve NTN capability by updating the input NetworkRegistrationInfo to indicate whether
     * connecting to a non-terrestrial network and the available services supported by the network.
     *
     * @param networkRegistrationInfo The NetworkRegistrationInfo of a network.
     * @param subId The subscription ID associated with a phone.
     */
    public static void resolveNtnCapability(
            @NonNull NetworkRegistrationInfo networkRegistrationInfo, int subId) {
        String registeredPlmn = networkRegistrationInfo.getRegisteredPlmn();
        if (TextUtils.isEmpty(registeredPlmn)) {
            return;
        }

        SatelliteController satelliteController = SatelliteController.getInstance();
        List<String> satellitePlmnList = satelliteController.getSatellitePlmnsForCarrier(subId);
        for (String satellitePlmn : satellitePlmnList) {
            if (TextUtils.equals(satellitePlmn, registeredPlmn)) {
                logd("Registered to satellite PLMN " + satellitePlmn);
                networkRegistrationInfo.setIsNonTerrestrialNetwork(true);
                networkRegistrationInfo.setAvailableServices(
                        satelliteController.getSupportedSatelliteServices(subId, satellitePlmn));
                break;
            }
        }
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }
}

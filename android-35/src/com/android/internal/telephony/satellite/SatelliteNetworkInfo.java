/*
 * Copyright (C) 2024 The Android Open Source Project
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

/**
 * Data class of the satellite configuration received from the entitlement server.
 */
public class SatelliteNetworkInfo {
    /** Stored the allowed plmn for using the satellite service. */
    public String mPlmn;
    /** Stored the DataPlanType. It is an optional value that can be one of the following three
     * values.
     * 1. "unmetered"
     * 2. "metered"
     * 3. empty string. */
    public String mDataPlanType;

    public SatelliteNetworkInfo(String plmn, String dataPlanType) {
        mPlmn = plmn;
        mDataPlanType = dataPlanType;
    }
}

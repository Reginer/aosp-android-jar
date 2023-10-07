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

package com.android.internal.telephony.uicc;

public final class IsimServiceTable extends IccServiceTable {
    private static final String TAG = "IsimServiceTable";
    public enum IsimService {
        PCSCF_ADDRESS,
        GBA,                                // Generic Bootstrapping Architecture (GBA)
        HTTP_DIGEST,
        GBA_LOCALKEY_ESTABLISHMENT,         // GBA-based Local Key Establishment Mechanism
        PCSCF_DISCOVERY_FOR_IMS,            // Support of P-CSCF discovery for IMS Local Break Out
        SMS,
        SMSR,                               // Short Message Status Reports
        SM_OVERIP_AND_DATA_DL_VIA_SMS_PP,   // Support for SM-over-IP including data download via
        // SMS-PP
        COMMUNICATION_CONTROL_FOR_IMS_BY_ISIM,
        UICC_ACCESS_TO_IMS
    }

    public IsimServiceTable(byte[] table) {
        super(table);
    }

    public boolean isAvailable(IsimService service) {
        return super.isAvailable(service.ordinal());
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected Object[] getValues() {
        return IsimService.values();
    }

    public byte[] getISIMServiceTable() {
        return mServiceTable;
    }
}

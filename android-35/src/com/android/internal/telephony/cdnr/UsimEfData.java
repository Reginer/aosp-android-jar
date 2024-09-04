/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.telephony.cdnr;

import android.text.TextUtils;

import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRecords.OperatorPlmnInfo;
import com.android.internal.telephony.uicc.IccRecords.PlmnNetworkName;
import com.android.internal.telephony.uicc.SIMRecords;

import java.util.Arrays;
import java.util.List;

/** Ef data from Usim. */
public final class UsimEfData implements EfData {
    private final SIMRecords mUsim;

    public UsimEfData(SIMRecords usim) {
        mUsim = usim;
    }

    @Override
    public String getServiceProviderName() {
        String spn = mUsim.getServiceProviderName();
        if (TextUtils.isEmpty(spn)) spn = null;
        return spn;
    }

    @Override
    public int getServiceProviderNameDisplayCondition(boolean isRoaming) {
        if (isRoaming) {
            // Show PLMN on roaming.
            return IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_PLMN
                    | mUsim.getCarrierNameDisplayCondition();
        } else {
            // Show SPN on non-roaming.
            return IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN
                    | mUsim.getCarrierNameDisplayCondition();
        }
    }

    @Override
    public List<String> getServiceProviderDisplayInformation() {
        String[] spdi = mUsim.getServiceProviderDisplayInformation();
        return spdi != null ? Arrays.asList(spdi) : null;
    }

    @Override
    public List<String> getEhplmnList() {
        String[] ehplmns = mUsim.getEhplmns();
        return ehplmns != null ? Arrays.asList(ehplmns) : null;
    }

    @Override
    public List<PlmnNetworkName> getPlmnNetworkNameList() {
        String pnnHomeName = mUsim.getPnnHomeName();
        if (!TextUtils.isEmpty(pnnHomeName)) {
            // TODO: Update the whole list rather than the pnn home name when IccRecords can
            // read all pnn records.
            return Arrays.asList(new PlmnNetworkName(pnnHomeName, "" /* shortName */));
        }
        return null;
    }

    @Override
    public List<OperatorPlmnInfo> getOperatorPlmnList() {
        // TODO: update the OPL when SIMRecords supports it.
        return null;
    }
}

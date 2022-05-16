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

import com.android.internal.telephony.uicc.IccRecords;

import java.util.Arrays;
import java.util.List;

/** EF data from brand override. */
public final class BrandOverrideEfData implements EfData {
    private final String mSpn;
    private final String mRegisteredPlmn;

    public BrandOverrideEfData(String operatorName, String registeredPlmn) {
        mSpn = operatorName;
        mRegisteredPlmn = registeredPlmn;
    }

    @Override
    public String getServiceProviderName() {
        return mSpn;
    }

    @Override
    public int getServiceProviderNameDisplayCondition(boolean isRoaming) {
        return IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN;
    }

    @Override
    public List<String> getServiceProviderDisplayInformation() {
        // Registered PLMN should be regarded as HOME PLMN
        return Arrays.asList(mRegisteredPlmn);
    }
}

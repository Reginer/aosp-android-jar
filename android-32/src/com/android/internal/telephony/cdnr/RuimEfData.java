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
import com.android.internal.telephony.uicc.RuimRecords;

/** Ef data from Ruim. */
public final class RuimEfData implements EfData {
    // No spn on roaming, no plmn on home.
    private static final int DEFAULT_CARRIER_NAME_DISPLAY_CONDITION_BITMASK = 0;
    private final RuimRecords mRuim;

    public RuimEfData(RuimRecords ruim) {
        mRuim = ruim;
    }

    @Override
    public String getServiceProviderName() {
        String spn = mRuim.getServiceProviderName();
        return TextUtils.isEmpty(spn) ? null : spn;
    }

    @Override
    public int getServiceProviderNameDisplayCondition(boolean isRoaming) {
        return mRuim.getCsimSpnDisplayCondition()
                ? IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN :
                DEFAULT_CARRIER_NAME_DISPLAY_CONDITION_BITMASK;
    }
}

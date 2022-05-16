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

import android.annotation.NonNull;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;

import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRecords.OperatorPlmnInfo;
import com.android.internal.telephony.uicc.IccRecords.PlmnNetworkName;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Ef data from carrier config. */
public final class CarrierConfigEfData implements EfData {
    private static final String TAG = "CarrierConfigEfData";
    private final PersistableBundle mConfig;

    public CarrierConfigEfData(@NonNull PersistableBundle config) {
        mConfig = config;
    }

    @Override
    public String getServiceProviderName() {
        String spn = mConfig.getString(CarrierConfigManager.KEY_CARRIER_NAME_STRING);
        if (TextUtils.isEmpty(spn)) return null;
        return spn;
    }

    @Override
    public int getServiceProviderNameDisplayCondition(boolean isRoaming) {
        int condition = mConfig.getInt(
                CarrierConfigManager.KEY_SPN_DISPLAY_CONDITION_OVERRIDE_INT,
                IccRecords.INVALID_CARRIER_NAME_DISPLAY_CONDITION_BITMASK);
        return condition;
    }

    @Override
    public List<String> getServiceProviderDisplayInformation() {
        String[] spdi = mConfig.getStringArray(
                CarrierConfigManager.KEY_SPDI_OVERRIDE_STRING_ARRAY);
        return spdi != null ? Arrays.asList(spdi) : null;
    }

    @Override
    public List<String> getEhplmnList() {
        String[] ehplmn = mConfig.getStringArray(
                CarrierConfigManager.KEY_EHPLMN_OVERRIDE_STRING_ARRAY);
        return ehplmn != null ? Arrays.asList(ehplmn) : null;
    }

    @Override
    public List<PlmnNetworkName> getPlmnNetworkNameList() {
        String[] pnn = mConfig.getStringArray(
                CarrierConfigManager.KEY_PNN_OVERRIDE_STRING_ARRAY);
        List<PlmnNetworkName> pnnList = null;
        if (pnn != null) {
            pnnList = new ArrayList<>(pnn.length);
            for (String pnnStr : pnn) {
                try {
                    String[] names = pnnStr.split("\\s*,\\s*");
                    String alphal = names[0];
                    String alphas = names.length > 1 ? names[1] : "";
                    pnnList.add(new PlmnNetworkName(alphal, alphas));
                } catch (Exception ex) {
                    Rlog.e(TAG, "CarrierConfig wrong pnn format, pnnStr = " + pnnStr);
                }
            }
        }
        return pnnList;
    }

    @Override
    public List<OperatorPlmnInfo> getOperatorPlmnList() {
        // OPL
        String[] opl = mConfig.getStringArray(
                CarrierConfigManager.KEY_OPL_OVERRIDE_STRING_ARRAY);
        List<OperatorPlmnInfo> oplList = null;
        if (opl != null) {
            oplList = new ArrayList<>(opl.length);
            for (String oplStr : opl) {
                try {
                    String[] info = oplStr.split("\\s*,\\s*");
                    oplList.add(new OperatorPlmnInfo(
                            info[0] /* plmn */,
                            Integer.parseInt(info[1]) /* lactac_start */,
                            Integer.parseInt(info[2]) /* lactac_end */,
                            Integer.parseInt(info[3]) /* pnn index */));
                } catch (Exception ex) {
                    Rlog.e(TAG, "CarrierConfig wrong opl format, oplStr = " + oplStr);
                }
            }
        }
        return oplList;
    }
}

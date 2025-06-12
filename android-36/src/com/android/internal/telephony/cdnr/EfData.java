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

import android.annotation.IntDef;

import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRecords.CarrierNameDisplayConditionBitmask;
import com.android.internal.telephony.uicc.IccRecords.OperatorPlmnInfo;
import com.android.internal.telephony.uicc.IccRecords.PlmnNetworkName;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/** Interface of EF fileds container. */
public interface EfData {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"EF_SOURCE_"}, value = {
            EF_SOURCE_CARRIER_API,
            EF_SOURCE_CARRIER_CONFIG,
            EF_SOURCE_USIM,
            EF_SOURCE_SIM,
            EF_SOURCE_CSIM,
            EF_SOURCE_RUIM,
            EF_SOURCE_VOICE_OPERATOR_SIGNALLING,
            EF_SOURCE_DATA_OPERATOR_SIGNALLING,
            EF_SOURCE_MODEM_CONFIG,
            EF_SOURCE_ERI
    })
    @interface EFSource {}

    int EF_SOURCE_CARRIER_CONFIG = 1;
    int EF_SOURCE_CARRIER_API = 2;
    int EF_SOURCE_USIM = 3;
    int EF_SOURCE_SIM = 4;
    int EF_SOURCE_CSIM = 5;
    int EF_SOURCE_RUIM = 6;
    int EF_SOURCE_VOICE_OPERATOR_SIGNALLING = 7;
    int EF_SOURCE_DATA_OPERATOR_SIGNALLING = 8;
    int EF_SOURCE_MODEM_CONFIG = 9;
    int EF_SOURCE_ERI = 10;

    /**
     * Get the service provider name for the registered PLMN.
     *
     * Reference: 3GPP TS 131.102 Section 4.2.12 EF_SPN.
     *
     * @return service provider name or {@code null} if it's not existed.
     */
    default String getServiceProviderName() {
        return null;
    }

    /**
     * Get the display condition of service provider name and PLMN network name. The display
     * condition has two bits(lsb). Service provider name display is required if the first bit
     * is set to 1. PLMN network name display is required if the second bit is set to 1.
     *
     * @see {@link IccRecords#CARRIER_NAME_DISPLAY_CONDITION_BITMASK_PLMN}
     * @see {@link IccRecords#CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN}
     *
     * Reference: 3GPP TS 131.102 Section 4.2.12 EF_SPN
     *
     * @retrun the bitmask of display condition or {@link com.android.internal.telephony.uicc
     * .IccRecords.INVALID_CARRIER_NAME_DISPLAY_CONDITION_BITMASK} if it's not existed.
     */
    @CarrierNameDisplayConditionBitmask
    default int getServiceProviderNameDisplayCondition(boolean isRoaming) {
        return IccRecords.INVALID_CARRIER_NAME_DISPLAY_CONDITION_BITMASK;
    }

    /**
     * Get the service provider display information. This is a list of PLMNs in which the
     * service provider name shall be displayed.
     *
     * Reference: 3GPP TS 131.102 Section 4.2.66 EF_SPDI
     *
     * @return a list of PLMNs or {@code null} if it's not existed.
     */
    default List<String> getServiceProviderDisplayInformation() {
        return null;
    }

    /**
     * Get the list of full and short form versions of the network name for the registered PLMN.
     *
     * Reference: 3GPP TS 131.102 Section 4.2.58 EF_PNN.
     *
     * @return a list of {@link PlmnNetworkName} or {@code null} if it's not existed.
     */
    default List<PlmnNetworkName> getPlmnNetworkNameList() {
        return null;
    }

    /**
     * Get the list of equivalent HPLMN.
     *
     * Reference: 3GPP TS 31.102 v15.2.0 Section 4.2.84 EF_EHPLMN
     * Reference: 3GPP TS 23.122 v15.6.0 Section 1.2 Equivalent HPLMN list
     *
     * @return a list of PLMN that indicates the equivalent HPLMN or {@code null} if it's not
     * existed.
     */
    default List<String> getEhplmnList() {
        return null;
    }

    /**
     * Get the list of operator PLMN information.
     *
     * Reference: 3GPP TS 131.102 Section 4.2.59 EF_OPL.
     *
     * @return a list of {@link OperatorPlmnInfo} or {@code null} if it's not existed.
     */
    default List<OperatorPlmnInfo> getOperatorPlmnList() {
        return null;
    }
}

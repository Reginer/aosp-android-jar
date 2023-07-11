/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.Annotation.ApnType;
import android.telephony.CarrierConfigManager;
import android.telephony.data.ApnSetting;

import com.android.internal.telephony.Phone;
import com.android.telephony.Rlog;

import java.util.Arrays;
import java.util.HashSet;

/**
 * This class represents a apn setting for create PDP link
 */
public class ApnSettingUtils {

    static final String LOG_TAG = "ApnSetting";

    private static final boolean DBG = false;

    /**
     * Check if this APN type is metered.
     *
     * @param apnType the APN type
     * @param phone the phone object
     * @return {@code true} if the APN type is metered, {@code false} otherwise.
     */
    public static boolean isMeteredApnType(@ApnType int apnType, Phone phone) {
        if (phone == null) {
            return true;
        }

        boolean isRoaming = phone.getServiceState().getDataRoaming();
        int subId = phone.getSubId();

        String carrierConfig;
        // First check if the device is roaming. If yes, use the roaming metered APN list.
        // Otherwise use the normal metered APN list.
        if (isRoaming) {
            carrierConfig = CarrierConfigManager.KEY_CARRIER_METERED_ROAMING_APN_TYPES_STRINGS;
        } else {
            carrierConfig = CarrierConfigManager.KEY_CARRIER_METERED_APN_TYPES_STRINGS;
        }

        if (DBG) {
            Rlog.d(LOG_TAG, "isMeteredApnType: isRoaming=" + isRoaming);
        }

        CarrierConfigManager configManager = (CarrierConfigManager)
                phone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager == null) {
            Rlog.e(LOG_TAG, "Carrier config service is not available");
            return true;
        }

        PersistableBundle b = configManager.getConfigForSubId(subId);
        if (b == null) {
            Rlog.e(LOG_TAG, "Can't get the config. subId = " + subId);
            return true;
        }

        String[] meteredApnTypes = b.getStringArray(carrierConfig);
        if (meteredApnTypes == null) {
            Rlog.e(LOG_TAG, carrierConfig +  " is not available. " + "subId = " + subId);
            return true;
        }

        HashSet<String> meteredApnSet = new HashSet<>(Arrays.asList(meteredApnTypes));
        if (DBG) {
            Rlog.d(LOG_TAG, "For subId = " + subId + ", metered APN types are "
                    + Arrays.toString(meteredApnSet.toArray()));
        }

        if (meteredApnSet.contains(ApnSetting.getApnTypeString(apnType))) {
            if (DBG) Rlog.d(LOG_TAG, ApnSetting.getApnTypeString(apnType) + " is metered.");
            return true;
        } else if (apnType == ApnSetting.TYPE_ALL) {
            // Assuming no configuration error, if at least one APN type is
            // metered, then this APN setting is metered.
            if (meteredApnSet.size() > 0) {
                if (DBG) Rlog.d(LOG_TAG, "APN_TYPE_ALL APN is metered.");
                return true;
            }
        }

        if (DBG) Rlog.d(LOG_TAG, ApnSetting.getApnTypeString(apnType) + " is not metered.");
        return false;
    }

    /**
     * Check if this APN setting is metered.
     *
     * @param apn APN setting
     * @param phone The phone object
     * @return True if this APN setting is metered, otherwise false.
     */
    public static boolean isMetered(ApnSetting apn, Phone phone) {
        if (phone == null || apn == null) {
            return true;
        }

        for (int apnType : apn.getApnTypes()) {
            // If one of the APN type is metered, then this APN setting is metered.
            if (isMeteredApnType(apnType, phone)) {
                return true;
            }
        }
        return false;
    }
}

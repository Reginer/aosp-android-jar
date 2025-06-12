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

package com.android.internal.telephony.metrics;

import android.telephony.CellularIdentifierDisclosure;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.telephony.Rlog;

/**
 * Facilitates writing stats relating to cellular transparency features. Delegates the actual
 * writing of stats out to {@link TelephonyStatsLog}.
 */
public class CellularSecurityTransparencyStats {

    private static final String LOG_TAG = "CellularSecurityTransparencyStats";
    private static final String LOG_DESCRIPTOR_SIM_MCC = "SIM MCC";
    private static final String LOG_DESCRIPTOR_SIM_MNC = "SIM MNC";
    private static final String LOG_DESCRIPTOR_DISCLOSURE_MCC = "disclosure MCC";
    private static final String LOG_DESCRIPTOR_DISCLOSURE_MNC = "disclosure MNC";
    private static final int DEFAULT_PLMN_PART = -1;

    /**
     * Log an identifier disclosure to be written out to {@link TelephonyStatsLog}
     */
    public void logIdentifierDisclosure(CellularIdentifierDisclosure disclosure, String simMcc,
            String simMnc, boolean notificationsEnabled) {

        int mcc = parsePlmnPartOrDefault(simMcc, LOG_DESCRIPTOR_SIM_MCC);
        int mnc = parsePlmnPartOrDefault(simMnc, LOG_DESCRIPTOR_SIM_MNC);

        int disclosureMcc = DEFAULT_PLMN_PART;
        int disclosureMnc = DEFAULT_PLMN_PART;
        String plmn = disclosure.getPlmn();
        if (plmn != null) {
            String[] plmnParts = plmn.split("-");
            if (plmnParts.length == 2) {
                disclosureMcc = parsePlmnPartOrDefault(plmnParts[0], LOG_DESCRIPTOR_DISCLOSURE_MCC);
                disclosureMnc = parsePlmnPartOrDefault(plmnParts[1], LOG_DESCRIPTOR_DISCLOSURE_MNC);
            }
        }

        writeIdentifierDisclosure(mcc, mnc, disclosureMcc, disclosureMnc,
                disclosure.getCellularIdentifier(), disclosure.getNasProtocolMessage(),
                disclosure.isEmergency(), notificationsEnabled);

    }

    private int parsePlmnPartOrDefault(String input, String logDescriptor) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Rlog.d(LOG_TAG, "Failed to parse " + logDescriptor + ": " + input);
        }

        return DEFAULT_PLMN_PART;
    }

    /**
     * Write identifier disclosure data out to {@link TelephonyStatsLog}. This method is split
     * out to enable testing, since {@link TelephonyStatsLog} is a final static class.
     */
    @VisibleForTesting
    public void writeIdentifierDisclosure(int mcc, int mnc, int disclosureMcc, int disclosureMnc,
            int identifier, int protocolMessage, boolean isEmergency,
            boolean areNotificationsEnabled) {
        TelephonyStatsLog.write(TelephonyStatsLog.CELLULAR_IDENTIFIER_DISCLOSED, mcc, mnc,
                disclosureMcc, disclosureMnc, identifier, protocolMessage, isEmergency,
                areNotificationsEnabled);
    }
}

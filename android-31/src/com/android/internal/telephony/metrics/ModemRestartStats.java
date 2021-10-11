/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import static com.android.internal.telephony.TelephonyStatsLog.MODEM_RESTART;

import android.os.Build;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.telephony.Rlog;

/** Metrics for the modem restarts. */
public class ModemRestartStats {
    private static final String TAG = ModemRestartStats.class.getSimpleName();

    /* Maximum length of the baseband version. */
    private static final int MAX_BASEBAND_LEN = 100;

    /* Maximum length of the modem restart reason. */
    private static final int MAX_REASON_LEN = 100;

    private ModemRestartStats() { }

    /** Generate metrics when modem restart occurs. */
    public static void onModemRestart(String reason) {
        reason = truncateString(reason, MAX_REASON_LEN);
        String basebandVersion = truncateString(Build.getRadioVersion(), MAX_BASEBAND_LEN);
        int carrierId = getCarrierId();

        Rlog.d(TAG, "Modem restart (carrier=" + carrierId + "): " + reason);
        TelephonyStatsLog.write(MODEM_RESTART, basebandVersion, reason, carrierId);
    }

    private static String truncateString(String string, int maxLen) {
        string = nullToEmpty(string);
        if (string.length() > maxLen) {
            string = string.substring(0, maxLen);
        }
        return string;
    }

    private static String nullToEmpty(String string) {
        return string != null ? string : "";
    }

    /** Returns the carrier ID of the first SIM card for which carrier ID is available. */
    private static int getCarrierId() {
        int carrierId = INVALID_SUBSCRIPTION_ID;
        try {
            for (Phone phone : PhoneFactory.getPhones()) {
                carrierId = phone.getCarrierId();
                if (carrierId != INVALID_SUBSCRIPTION_ID) {
                    break;
                }
            }
        } catch (IllegalStateException e) {
            // Nothing to do here.
        }
        return carrierId;
    }
}

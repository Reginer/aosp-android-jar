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

import static com.android.internal.telephony.TelephonyStatsLog.CARRIER_ID_MISMATCH_REPORTED;
import static com.android.internal.telephony.TelephonyStatsLog.CARRIER_ID_TABLE_UPDATED;

import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.internal.telephony.nano.PersistAtomsProto.CarrierIdMismatch;
import com.android.telephony.Rlog;

/** Metrics for the carrier id matching. */
public class CarrierIdMatchStats {
    private static final String TAG = CarrierIdMatchStats.class.getSimpleName();

    private CarrierIdMatchStats() { }

    /** Generate metrics when carrier ID mismatch occurs. */
    public static void onCarrierIdMismatch(
            int cid, String mccMnc, String gid1, String spn, String pnn) {
        PersistAtomsStorage storage = PhoneFactory.getMetricsCollector().getAtomsStorage();

        CarrierIdMismatch carrierIdMismatch = new CarrierIdMismatch();
        carrierIdMismatch.mccMnc = nullToEmpty(mccMnc);
        carrierIdMismatch.gid1 = nullToEmpty(gid1);
        carrierIdMismatch.spn = nullToEmpty(spn);
        carrierIdMismatch.pnn = carrierIdMismatch.spn.isEmpty() ? nullToEmpty(pnn) : "";

        // Add to storage and generate atom only if it was added (new SIM card).
        boolean isAdded = storage.addCarrierIdMismatch(carrierIdMismatch);
        if (isAdded) {
            Rlog.d(TAG, "New carrier ID mismatch event: " + carrierIdMismatch.toString());
            TelephonyStatsLog.write(CARRIER_ID_MISMATCH_REPORTED, cid, mccMnc, gid1, spn, pnn);
        }
    }

    /** Generate metrics for the carrier ID table version. */
    public static void sendCarrierIdTableVersion(int carrierIdTableVersion) {
        PersistAtomsStorage storage = PhoneFactory.getMetricsCollector().getAtomsStorage();

        if (storage.setCarrierIdTableVersion(carrierIdTableVersion)) {
            Rlog.d(TAG, "New carrier ID table version: " + carrierIdTableVersion);
            TelephonyStatsLog.write(CARRIER_ID_TABLE_UPDATED, carrierIdTableVersion);
        }
    }

    private static String nullToEmpty(String string) {
        return string != null ? string : "";
    }
}

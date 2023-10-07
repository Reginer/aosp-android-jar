/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.clockwork.telecom;

import android.annotation.NonNull;
import android.content.Context;
import android.telephony.emergency.EmergencyNumber;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Writes/reads the emergency number data that synced from connected primary device. */
public class CompanionTelecomService {

    private Map<Integer, List<EmergencyNumber>> mNumberCache;
    private HashSet<String> mRawNumberCache;
    private EmergencyNumberDbHelper mEmergencyNumberDbHelper;

    public CompanionTelecomService(@NonNull Context context) {
        this(EmergencyNumberDbHelper.getInstance(context));
    }

    @VisibleForTesting
    public CompanionTelecomService(@NonNull EmergencyNumberDbHelper dbHelper) {
        mEmergencyNumberDbHelper = dbHelper;
    }

    /** Fetches all emergency number synced from primary device. */
    public Map getEmergencyNumbers() {
        tryInitAllCache();
        return mNumberCache;
    }

    /** Determines if a raw number is an emergency number. */
    public boolean isEmergencyNumber(@NonNull String number) {
        tryInitAllCache();
        return mRawNumberCache.contains(number);
    }

    /** Sets emergency number data. */
    public void setEmergencyNumbers(Map numbers) {
        synchronized (CompanionTelecomService.this) {
            if (!numbers.equals(mNumberCache)) {
                mNumberCache = new HashMap<>(numbers);
                tryInitRawNumberCache(true);
                mEmergencyNumberDbHelper.updateEmergencyNumbers(numbers);
            }
        }
    }

    /** Prints the current cached emergency number data to dump. */
    public void dump(IndentingPrintWriter ipw) {
        try {
            ipw.println("================ CompanionTelecomService ================");
            ipw.increaseIndent();
            if (mNumberCache != null) {
                for (List<EmergencyNumber> emergencyNumberList : mNumberCache.values()) {
                    if (emergencyNumberList != null) {
                        for (EmergencyNumber emergencyNumber : emergencyNumberList) {
                            if (emergencyNumber != null) {
                                ipw.println(emergencyNumber);
                            }
                        }
                    }
                }
            }
            ipw.decreaseIndent();
        } catch (Throwable throwable) {
            ipw.println("caught exception while dumping " + throwable.getMessage());
        } finally {
            ipw.println();
        }
    }

    private void tryInitAllCache() {
        if (mNumberCache == null) {
            mNumberCache = new HashMap<>();
            mNumberCache = mEmergencyNumberDbHelper.fetchEmergencyNumbers();
        }
        tryInitRawNumberCache(false);
    }

    /**
     * Tries to init the rawNumberCache, rawNumberCache is used to determine if a raw number in
     * String format represents an emergency number or not.
     *
     * @param force If true then force to refresh the rawNumberCache.
     */
    private void tryInitRawNumberCache(boolean force) {
        if (mRawNumberCache == null || force) {
            mRawNumberCache = new HashSet<>();
            for (List<EmergencyNumber> emergencyNumberList : mNumberCache.values()) {
                if (emergencyNumberList != null) {
                    for (EmergencyNumber emergencyNumber : emergencyNumberList) {
                        if (!TextUtils.isEmpty(emergencyNumber.getNumber())) {
                            mRawNumberCache.add(emergencyNumber.getNumber());
                        }
                    }
                }
            }
        }
    }
}

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

package com.android.internal.telephony.metrics;

import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__ROUTE__EMERGENCY_CALL_ROUTE_EMERGENCY;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__ROUTE__EMERGENCY_CALL_ROUTE_NORMAL;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__ROUTE__EMERGENCY_CALL_ROUTE_UNKNOWN;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_AIEC;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_AMBULANCE;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_MIEC;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_POLICE;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SOURCES__EMERGENCY_NUMBER_SOURCE_DATABASE;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SOURCES__EMERGENCY_NUMBER_SOURCE_DEFAULT;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SOURCES__EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SOURCES__EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING;
import static com.android.internal.telephony.TelephonyStatsLog.EMERGENCY_NUMBERS_INFO__SOURCES__EMERGENCY_NUMBER_SOURCE_SIM;

import android.telephony.emergency.EmergencyNumber;
import android.util.SparseIntArray;

import com.android.internal.telephony.nano.PersistAtomsProto;

import java.util.ArrayList;
import java.util.List;

/**
 * EmergencyStats logs the atoms for consolidated emergency number list in framework. It also logs
 * the details of a dialed emergency number. To avoid repeated information this class stores the
 * emergency numbers list in map and verifies the information for duplicacy before logging it. Note:
 * This locally stored information will erase on process restart scenarios (like reboot, crash,
 * etc.).
 */
public class EmergencyNumberStats {

    private static final String TAG = EmergencyNumberStats.class.getSimpleName();
    private static final SparseIntArray sRoutesMap;
    private static final SparseIntArray sServiceCategoriesMap;
    private static final SparseIntArray sSourcesMap;
    private static EmergencyNumberStats sInstance;

    static {
        sRoutesMap = new SparseIntArray() {
            {
                put(EmergencyNumber.EMERGENCY_CALL_ROUTING_EMERGENCY,
                        EMERGENCY_NUMBERS_INFO__ROUTE__EMERGENCY_CALL_ROUTE_EMERGENCY);
                put(EmergencyNumber.EMERGENCY_CALL_ROUTING_NORMAL,
                        EMERGENCY_NUMBERS_INFO__ROUTE__EMERGENCY_CALL_ROUTE_NORMAL);
                put(EmergencyNumber.EMERGENCY_CALL_ROUTING_UNKNOWN,
                        EMERGENCY_NUMBERS_INFO__ROUTE__EMERGENCY_CALL_ROUTE_UNKNOWN);
            }
        };

        sServiceCategoriesMap = new SparseIntArray() {
            {
                put(EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED,
                        EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED);
                put(EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_POLICE,
                        EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_POLICE);
                put(EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_AMBULANCE,
                        EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_AMBULANCE);
                put(EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE,
                        EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE);
                put(EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD,
                        EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD);
                put(EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE,
                        EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE);
                put(EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MIEC,
                        EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_MIEC);
                put(EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_AIEC,
                        EMERGENCY_NUMBERS_INFO__SERVICE_CATEGORIES__EMERGENCY_SERVICE_CATEGORY_AIEC);
            }
        };

        sSourcesMap = new SparseIntArray() {
            {
                put(EmergencyNumber.EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING,
                        EMERGENCY_NUMBERS_INFO__SOURCES__EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING);
                put(EmergencyNumber.EMERGENCY_NUMBER_SOURCE_SIM,
                        EMERGENCY_NUMBERS_INFO__SOURCES__EMERGENCY_NUMBER_SOURCE_SIM);
                put(EmergencyNumber.EMERGENCY_NUMBER_SOURCE_DATABASE,
                        EMERGENCY_NUMBERS_INFO__SOURCES__EMERGENCY_NUMBER_SOURCE_DATABASE);
                put(EmergencyNumber.EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG,
                        EMERGENCY_NUMBERS_INFO__SOURCES__EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG);
                put(EmergencyNumber.EMERGENCY_NUMBER_SOURCE_DEFAULT,
                        EMERGENCY_NUMBERS_INFO__SOURCES__EMERGENCY_NUMBER_SOURCE_DEFAULT);
            }
        };
    }

    private EmergencyNumberStats() {
    }

    /** Static method to provide singleton instance for EmergencyNumberStats. */
    public static EmergencyNumberStats getInstance() {
        if (sInstance == null) {
            sInstance = new EmergencyNumberStats();
        }
        return sInstance;
    }

    /**
     * It converts the {@link android.telephony.emergency.EmergencyNumber} to
     * {@link PersistAtomsProto.EmergencyNumber} for
     * logging the EmergencyNumber atoms with pulled event.
     *
     * @param emergencyNumberList android.telephony.EmergencyNumber list
     * @param assetVersion        assert version
     * @param otaVersion          ota version
     * @param isDbRoutingIgnored  flag that defines if routing is ignored through database.
     */
    public PersistAtomsProto.EmergencyNumbersInfo[] convertEmergencyNumbersListToProto(
            List<EmergencyNumber> emergencyNumberList, int assetVersion, int otaVersion,
            boolean isDbRoutingIgnored) {
        List<PersistAtomsProto.EmergencyNumbersInfo> numberProtoList = new ArrayList<>();
        for (EmergencyNumber number : emergencyNumberList) {
            numberProtoList.add(convertEmergencyNumberToProto(number, assetVersion, otaVersion,
                    isDbRoutingIgnored));
        }
        return numberProtoList.toArray(new PersistAtomsProto.EmergencyNumbersInfo[0]);
    }

    private PersistAtomsProto.EmergencyNumbersInfo convertEmergencyNumberToProto(
            EmergencyNumber number, int assetVer, int otaVer, boolean isDbRoutingIgnored) {
        String dialNumber = number.getNumber();
        PersistAtomsProto.EmergencyNumbersInfo emergencyNumber =
                new PersistAtomsProto.EmergencyNumbersInfo();
        emergencyNumber.isDbVersionIgnored = isDbRoutingIgnored;
        emergencyNumber.assetVersion = assetVer;
        emergencyNumber.otaVersion = otaVer;
        emergencyNumber.number = dialNumber;
        emergencyNumber.countryIso = number.getCountryIso();
        emergencyNumber.mnc = number.getMnc();
        emergencyNumber.route = sRoutesMap.get(number.getEmergencyCallRouting());
        emergencyNumber.urns = number.getEmergencyUrns().toArray(new String[0]);
        emergencyNumber.serviceCategories = getMappedServiceCategories(
                number.getEmergencyServiceCategories());
        emergencyNumber.sources = getMappedSources(number.getEmergencyNumberSources());
        return emergencyNumber;
    }

    private int[] getMappedServiceCategories(List<Integer> serviceCategories) {
        if (serviceCategories == null || serviceCategories.isEmpty()) {
            return null;
        }
        return serviceCategories.stream().map(sServiceCategoriesMap::get).mapToInt(
                Integer::intValue).toArray();
    }

    private int[] getMappedSources(List<Integer> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        return sources.stream().map(sSourcesMap::get).mapToInt(Integer::intValue).toArray();
    }
}

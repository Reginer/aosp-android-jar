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

package com.android.internal.telephony.dataconnection;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.PersistableBundle;
import android.telephony.Annotation;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.data.ApnSetting;
import android.util.ArrayMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Hard coded configuration of specific network types that the telephony module needs.
 * Formerly stored in network attributes within the resources file.
 */
public class ApnConfigTypeRepository {

    private static final String TAG = ApnConfigTypeRepository.class.getSimpleName();

    private final Map<Integer, ApnConfigType> mConfigTypeMap;

    public ApnConfigTypeRepository(PersistableBundle carrierConfig) {
        mConfigTypeMap = new HashMap<>();
        setup(carrierConfig);
    }

    /**
     * Gets list of apn config types.
     * @return All apn config types.
     */
    public Collection<ApnConfigType> getTypes() {
        return mConfigTypeMap.values();
    }

    /**
     * Gets the apn config type by apn type.
     * @param type The ApnType to search for.
     * @return The config type matching the given apn type.
     */
    @Nullable
    public ApnConfigType getByType(@Annotation.ApnType int type) {
        return mConfigTypeMap.get(type);
    }

    private void setup(PersistableBundle carrierConfig) {
        addApns(getCarrierApnTypeMap(CarrierConfigManager.getDefaultConfig()));
        addApns(getCarrierApnTypeMap(carrierConfig));
    }

    private void addApns(Map<Integer, Integer> apnTypeMap) {
        add(ApnSetting.TYPE_DEFAULT, apnTypeMap);
        add(ApnSetting.TYPE_MMS, apnTypeMap);
        add(ApnSetting.TYPE_SUPL, apnTypeMap);
        add(ApnSetting.TYPE_DUN, apnTypeMap);
        add(ApnSetting.TYPE_HIPRI, apnTypeMap);
        add(ApnSetting.TYPE_FOTA, apnTypeMap);
        add(ApnSetting.TYPE_IMS, apnTypeMap);
        add(ApnSetting.TYPE_CBS, apnTypeMap);
        add(ApnSetting.TYPE_IA, apnTypeMap);
        add(ApnSetting.TYPE_EMERGENCY, apnTypeMap);
        add(ApnSetting.TYPE_MCX, apnTypeMap);
        add(ApnSetting.TYPE_XCAP, apnTypeMap);
        add(ApnSetting.TYPE_ENTERPRISE, apnTypeMap);
    }

    @NonNull
    private Map<Integer, Integer> getCarrierApnTypeMap(PersistableBundle carrierConfig) {
        if (carrierConfig == null) {
            Rlog.w(TAG, "carrier config is null");
            return new ArrayMap<>();
        }

        final String[] apnTypeConfig =
                carrierConfig.getStringArray(CarrierConfigManager.KEY_APN_PRIORITY_STRING_ARRAY);

        final Map<Integer, Integer> apnTypeMap = new ArrayMap<>();
        if (apnTypeConfig != null) {
            for (final String entry : apnTypeConfig) {
                try {
                    final String[] keyValue = entry.split(":");
                    if (keyValue.length != 2) {
                        Rlog.e(TAG, "Apn type entry must have exactly one ':'");
                    } else if (keyValue[0].contains(",")) {
                        //getApnTypesBitmaskFromString parses commas to a list, not valid here.
                        Rlog.e(TAG, "Invalid apn type name, entry: " + entry);
                    } else {
                        int apnTypeBitmask = ApnSetting.getApnTypesBitmaskFromString(keyValue[0]);
                        if (apnTypeBitmask > 0) {
                            apnTypeMap.put(apnTypeBitmask, Integer.parseInt(keyValue[1]));
                        } else {
                            Rlog.e(TAG, "Invalid apn type name, entry: " + entry);
                        }
                    }

                } catch (Exception ex) {
                    Rlog.e(TAG, "Exception on apn type entry: " + entry + "\n", ex);
                }
            }
        }
        return apnTypeMap;
    }

    private void add(@Annotation.ApnType int type, Map<Integer, Integer> apnTypeMap) {
        if (apnTypeMap.containsKey(type)) {
            mConfigTypeMap.put(type, new ApnConfigType(type, apnTypeMap.get(type)));
        }
    }
}

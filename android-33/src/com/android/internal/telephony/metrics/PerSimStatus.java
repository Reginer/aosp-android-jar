/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.provider.Telephony.Carriers.CONTENT_URI;
import static android.telephony.PhoneNumberUtils.areSamePhoneNumber;
import static android.telephony.SubscriptionManager.PHONE_NUMBER_SOURCE_CARRIER;
import static android.telephony.SubscriptionManager.PHONE_NUMBER_SOURCE_IMS;
import static android.telephony.SubscriptionManager.PHONE_NUMBER_SOURCE_UICC;

import static com.android.internal.telephony.TelephonyStatsLog.PER_SIM_STATUS__SIM_VOLTAGE_CLASS__VOLTAGE_CLASS_A;
import static com.android.internal.telephony.TelephonyStatsLog.PER_SIM_STATUS__SIM_VOLTAGE_CLASS__VOLTAGE_CLASS_B;
import static com.android.internal.telephony.TelephonyStatsLog.PER_SIM_STATUS__SIM_VOLTAGE_CLASS__VOLTAGE_CLASS_C;
import static com.android.internal.telephony.TelephonyStatsLog.PER_SIM_STATUS__SIM_VOLTAGE_CLASS__VOLTAGE_CLASS_UNKNOWN;
import static com.android.internal.telephony.TelephonyStatsLog.PER_SIM_STATUS__WFC_MODE__CELLULAR_PREFERRED;
import static com.android.internal.telephony.TelephonyStatsLog.PER_SIM_STATUS__WFC_MODE__UNKNOWN;
import static com.android.internal.telephony.TelephonyStatsLog.PER_SIM_STATUS__WFC_MODE__WIFI_ONLY;
import static com.android.internal.telephony.TelephonyStatsLog.PER_SIM_STATUS__WFC_MODE__WIFI_PREFERRED;

import android.annotation.Nullable;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.text.TextUtils;

import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccSlot;

import java.util.Optional;

/** Stores the per SIM status. */
public class PerSimStatus {
    private static final long BITMASK_2G =
            TelephonyManager.NETWORK_TYPE_BITMASK_GSM
                    | TelephonyManager.NETWORK_TYPE_BITMASK_GPRS
                    | TelephonyManager.NETWORK_TYPE_BITMASK_EDGE
                    | TelephonyManager.NETWORK_TYPE_BITMASK_CDMA
                    | TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT;

    public final int carrierId;
    public final int phoneNumberSourceUicc;
    public final int phoneNumberSourceCarrier;
    public final int phoneNumberSourceIms;
    public final boolean advancedCallingSettingEnabled;
    public final boolean voWiFiSettingEnabled;
    public final int voWiFiModeSetting;
    public final int voWiFiRoamingModeSetting;
    public final boolean vtSettingEnabled;
    public final boolean dataRoamingEnabled;
    public final long preferredNetworkType;
    public final boolean disabled2g;
    public final boolean pin1Enabled;
    public final int minimumVoltageClass;
    public final int userModifiedApnTypes;

    /** Returns the current sim status of the given {@link Phone}. */
    @Nullable
    public static PerSimStatus getCurrentState(Phone phone) {
        int[] numberIds = getNumberIds(phone);
        if (numberIds == null) return null;
        ImsMmTelManager imsMmTelManager = getImsMmTelManager(phone);
        IccCard iccCard = phone.getIccCard();
        return new PerSimStatus(
                phone.getCarrierId(),
                numberIds[0],
                numberIds[1],
                numberIds[2],
                imsMmTelManager == null ? false : imsMmTelManager.isAdvancedCallingSettingEnabled(),
                imsMmTelManager == null ? false : imsMmTelManager.isVoWiFiSettingEnabled(),
                imsMmTelManager == null
                        ? PER_SIM_STATUS__WFC_MODE__UNKNOWN
                        : wifiCallingModeToProtoEnum(imsMmTelManager.getVoWiFiModeSetting()),
                imsMmTelManager == null
                        ? PER_SIM_STATUS__WFC_MODE__UNKNOWN
                        : wifiCallingModeToProtoEnum(imsMmTelManager.getVoWiFiRoamingModeSetting()),
                imsMmTelManager == null ? false : imsMmTelManager.isVtSettingEnabled(),
                phone.getDataRoamingEnabled(),
                phone.getAllowedNetworkTypes(TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER),
                is2gDisabled(phone),
                iccCard == null ? false : iccCard.getIccLockEnabled(),
                getMinimumVoltageClass(phone),
                getUserModifiedApnTypes(phone));
    }

    private PerSimStatus(
            int carrierId,
            int phoneNumberSourceUicc,
            int phoneNumberSourceCarrier,
            int phoneNumberSourceIms,
            boolean advancedCallingSettingEnabled,
            boolean voWiFiSettingEnabled,
            int voWiFiModeSetting,
            int voWiFiRoamingModeSetting,
            boolean vtSettingEnabled,
            boolean dataRoamingEnabled,
            long preferredNetworkType,
            boolean disabled2g,
            boolean pin1Enabled,
            int minimumVoltageClass,
            int userModifiedApnTypes) {
        this.carrierId = carrierId;
        this.phoneNumberSourceUicc = phoneNumberSourceUicc;
        this.phoneNumberSourceCarrier = phoneNumberSourceCarrier;
        this.phoneNumberSourceIms = phoneNumberSourceIms;
        this.advancedCallingSettingEnabled = advancedCallingSettingEnabled;
        this.voWiFiSettingEnabled = voWiFiSettingEnabled;
        this.voWiFiModeSetting = voWiFiModeSetting;
        this.voWiFiRoamingModeSetting = voWiFiRoamingModeSetting;
        this.vtSettingEnabled = vtSettingEnabled;
        this.dataRoamingEnabled = dataRoamingEnabled;
        this.preferredNetworkType = preferredNetworkType;
        this.disabled2g = disabled2g;
        this.pin1Enabled = pin1Enabled;
        this.minimumVoltageClass = minimumVoltageClass;
        this.userModifiedApnTypes = userModifiedApnTypes;
    }

    @Nullable
    private static ImsMmTelManager getImsMmTelManager(Phone phone) {
        ImsManager imsManager = phone.getContext().getSystemService(ImsManager.class);
        if (imsManager == null) {
            return null;
        }
        try {
            return imsManager.getImsMmTelManager(phone.getSubId());
        } catch (IllegalArgumentException e) {
            return null; // Invalid subId
        }
    }

    /**
     * Returns an array of integer ids representing phone numbers. If number is empty then id will
     * be 0. Two same numbers will have same id, and different numbers will have different ids. For
     * example, [1, 0, 1] means that uicc and ims numbers are the same while carrier number is empty
     * and [1, 2, 3] means all numbers are different.
     *
     * <ul>
     *   <li>Index 0: id associated with {@code PHONE_NUMBER_SOURCE_UICC}.</li>
     *   <li>Index 1: id associated with {@code PHONE_NUMBER_SOURCE_CARRIER}.</li>
     *   <li>Index 2: id associated with {@code PHONE_NUMBER_SOURCE_IMS}.</li>
     * </ul>
     */
    @Nullable
    private static int[] getNumberIds(Phone phone) {
        SubscriptionController subscriptionController = SubscriptionController.getInstance();
        if (subscriptionController == null) {
            return null;
        }
        int subId = phone.getSubId();
        String countryIso =
                Optional.ofNullable(subscriptionController.getSubscriptionInfo(subId))
                        .map(SubscriptionInfo::getCountryIso)
                        .orElse("");
        // numbersFromAllSources[] - phone numbers from each sources:
        String[] numbersFromAllSources =
                new String[] {
                    subscriptionController.getPhoneNumber(
                            subId, PHONE_NUMBER_SOURCE_UICC, null, null), // 0
                    subscriptionController.getPhoneNumber(
                            subId, PHONE_NUMBER_SOURCE_CARRIER, null, null), // 1
                    subscriptionController.getPhoneNumber(
                            subId, PHONE_NUMBER_SOURCE_IMS, null, null), // 2
                };
        int[] numberIds = new int[numbersFromAllSources.length]; // default value 0
        for (int i = 0, idForNextUniqueNumber = 1; i < numberIds.length; i++) {
            if (TextUtils.isEmpty(numbersFromAllSources[i])) {
                // keep id 0 if number not available
                continue;
            }
            // the number is available:
            // try to find the same number from other sources and reuse the id
            for (int j = 0; j < i; j++) {
                if (!TextUtils.isEmpty(numbersFromAllSources[j])
                        && areSamePhoneNumber(
                                numbersFromAllSources[i], numbersFromAllSources[j], countryIso)) {
                    numberIds[i] = numberIds[j];
                }
            }
            // didn't find same number (otherwise should not be id 0), assign a new id
            if (numberIds[i] == 0) {
                numberIds[i] = idForNextUniqueNumber++;
            }
        }
        return numberIds;
    }

    /**
     * Returns {@code true} if 2G cellular network is disabled (Allow 2G toggle in the settings).
     */
    private static boolean is2gDisabled(Phone phone) {
        return (phone.getAllowedNetworkTypes(
                                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G)
                        & BITMASK_2G)
                == 0;
    }

    /** Converts {@link ImsMmTelManager.WifiCallingMode} to the value of PerSimStatus WfcMode. */
    private static int wifiCallingModeToProtoEnum(@ImsMmTelManager.WiFiCallingMode int mode) {
        switch (mode) {
            case ImsMmTelManager.WIFI_MODE_WIFI_ONLY:
                return PER_SIM_STATUS__WFC_MODE__WIFI_ONLY;
            case ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED:
                return PER_SIM_STATUS__WFC_MODE__CELLULAR_PREFERRED;
            case ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED:
                return PER_SIM_STATUS__WFC_MODE__WIFI_PREFERRED;
            default:
                return PER_SIM_STATUS__WFC_MODE__UNKNOWN;
        }
    }

    /** Returns the minimum voltage class supported by the UICC. */
    private static int getMinimumVoltageClass(Phone phone) {
        UiccSlot uiccSlot = UiccController.getInstance().getUiccSlotForPhone(phone.getPhoneId());
        if (uiccSlot == null) {
            return PER_SIM_STATUS__SIM_VOLTAGE_CLASS__VOLTAGE_CLASS_UNKNOWN;
        }
        switch (uiccSlot.getMinimumVoltageClass()) {
            case UiccSlot.VOLTAGE_CLASS_A:
                return PER_SIM_STATUS__SIM_VOLTAGE_CLASS__VOLTAGE_CLASS_A;
            case UiccSlot.VOLTAGE_CLASS_B:
                return PER_SIM_STATUS__SIM_VOLTAGE_CLASS__VOLTAGE_CLASS_B;
            case UiccSlot.VOLTAGE_CLASS_C:
                return PER_SIM_STATUS__SIM_VOLTAGE_CLASS__VOLTAGE_CLASS_C;
            default:
                return PER_SIM_STATUS__SIM_VOLTAGE_CLASS__VOLTAGE_CLASS_UNKNOWN;
        }
    }

    /** Returns the bitmask representing types of APNs modified by user. */
    private static int getUserModifiedApnTypes(Phone phone) {
        String[] projections = {Telephony.Carriers.TYPE};
        String selection = Telephony.Carriers.EDITED_STATUS + "=?";
        String[] selectionArgs = {Integer.toString(Telephony.Carriers.USER_EDITED)};
        try (Cursor cursor =
                phone.getContext()
                        .getContentResolver()
                        .query(
                                Uri.withAppendedPath(CONTENT_URI, "subId/" + phone.getSubId()),
                                projections,
                                selection,
                                selectionArgs,
                                null)) {
            int bitmask = 0;
            while (cursor != null && cursor.moveToNext()) {
                bitmask |= ApnSetting.getApnTypesBitmaskFromString(cursor.getString(0));
            }
            return bitmask;
        }
    }
}

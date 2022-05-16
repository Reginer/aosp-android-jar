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

package com.android.internal.telephony.emergency;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.emergency.EmergencyNumber.EmergencyCallRouting;
import android.telephony.emergency.EmergencyNumber.EmergencyServiceCategories;
import android.text.TextUtils;
import android.util.LocalLog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HalVersion;
import com.android.internal.telephony.LocaleTracker;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.util.IndentingPrintWriter;
import com.android.phone.ecc.nano.ProtobufEccData;
import com.android.phone.ecc.nano.ProtobufEccData.EccInfo;
import com.android.telephony.Rlog;

import com.google.i18n.phonenumbers.ShortNumberInfo;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Emergency Number Tracker that handles update of emergency number list from RIL and emergency
 * number database. This is multi-sim based and each Phone has a EmergencyNumberTracker.
 */
public class EmergencyNumberTracker extends Handler {
    private static final String TAG = EmergencyNumberTracker.class.getSimpleName();

    private static final int INVALID_DATABASE_VERSION = -1;
    private static final String EMERGENCY_NUMBER_DB_OTA_FILE_NAME = "emergency_number_db";
    private static final String EMERGENCY_NUMBER_DB_OTA_FILE_PATH =
            "misc/emergencynumberdb/" + EMERGENCY_NUMBER_DB_OTA_FILE_NAME;

    /** Used for storing overrided (non-default) OTA database file path */
    private ParcelFileDescriptor mOverridedOtaDbParcelFileDescriptor = null;

    /** @hide */
    public static boolean DBG = false;
    /** @hide */
    public static final int ADD_EMERGENCY_NUMBER_TEST_MODE = 1;
    /** @hide */
    public static final int REMOVE_EMERGENCY_NUMBER_TEST_MODE = 2;
    /** @hide */
    public static final int RESET_EMERGENCY_NUMBER_TEST_MODE = 3;

    private final CommandsInterface mCi;
    private final Phone mPhone;
    private String mCountryIso;
    private String mLastKnownEmergencyCountryIso = "";
    private int mCurrentDatabaseVersion = INVALID_DATABASE_VERSION;
    /**
     * Indicates if the country iso is set by another subscription.
     * @hide
     */
    public boolean mIsCountrySetByAnotherSub = false;
    private String[] mEmergencyNumberPrefix = new String[0];

    private static final String EMERGENCY_NUMBER_DB_ASSETS_FILE = "eccdata";

    private List<EmergencyNumber> mEmergencyNumberListFromDatabase = new ArrayList<>();
    private List<EmergencyNumber> mEmergencyNumberListFromRadio = new ArrayList<>();
    private List<EmergencyNumber> mEmergencyNumberListWithPrefix = new ArrayList<>();
    private List<EmergencyNumber> mEmergencyNumberListFromTestMode = new ArrayList<>();
    private List<EmergencyNumber> mEmergencyNumberList = new ArrayList<>();

    private final LocalLog mEmergencyNumberListDatabaseLocalLog = new LocalLog(20);
    private final LocalLog mEmergencyNumberListRadioLocalLog = new LocalLog(20);
    private final LocalLog mEmergencyNumberListPrefixLocalLog = new LocalLog(20);
    private final LocalLog mEmergencyNumberListTestModeLocalLog = new LocalLog(20);
    private final LocalLog mEmergencyNumberListLocalLog = new LocalLog(20);

    /** Event indicating the update for the emergency number list from the radio. */
    private static final int EVENT_UNSOL_EMERGENCY_NUMBER_LIST = 1;
    /**
     * Event indicating the update for the emergency number list from the database due to the
     * change of country code.
     **/
    private static final int EVENT_UPDATE_DB_COUNTRY_ISO_CHANGED = 2;
    /** Event indicating the update for the emergency number list in the testing mode. */
    private static final int EVENT_UPDATE_EMERGENCY_NUMBER_TEST_MODE = 3;
    /** Event indicating the update for the emergency number prefix from carrier config. */
    private static final int EVENT_UPDATE_EMERGENCY_NUMBER_PREFIX = 4;
    /** Event indicating the update for the OTA emergency number database. */
    @VisibleForTesting
    public static final int EVENT_UPDATE_OTA_EMERGENCY_NUMBER_DB = 5;
    /** Event indicating the override for the test OTA emergency number database. */
    @VisibleForTesting
    public static final int EVENT_OVERRIDE_OTA_EMERGENCY_NUMBER_DB_FILE_PATH = 6;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)) {
                onCarrierConfigChanged();
                return;
            } else if (intent.getAction().equals(
                    TelephonyManager.ACTION_NETWORK_COUNTRY_CHANGED)) {
                int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY, -1);
                if (phoneId == mPhone.getPhoneId()) {
                    String countryIso = intent.getStringExtra(
                            TelephonyManager.EXTRA_NETWORK_COUNTRY);
                    logd("ACTION_NETWORK_COUNTRY_CHANGED: PhoneId: " + phoneId + " CountryIso: "
                            + countryIso);

                    // Update country iso change for available Phones
                    updateEmergencyCountryIsoAllPhones(countryIso == null ? "" : countryIso);
                }
                return;
            }
        }
    };

    public EmergencyNumberTracker(Phone phone, CommandsInterface ci) {
        mPhone = phone;
        mCi = ci;

        if (mPhone != null) {
            CarrierConfigManager configMgr = (CarrierConfigManager)
                    mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configMgr != null) {
                PersistableBundle b = configMgr.getConfigForSubId(mPhone.getSubId());
                if (b != null) {
                    mEmergencyNumberPrefix = b.getStringArray(
                            CarrierConfigManager.KEY_EMERGENCY_NUMBER_PREFIX_STRING_ARRAY);
                }
            } else {
                loge("CarrierConfigManager is null.");
            }

            // Receive Carrier Config Changes
            IntentFilter filter = new IntentFilter(
                    CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
            // Receive Telephony Network Country Changes
            filter.addAction(TelephonyManager.ACTION_NETWORK_COUNTRY_CHANGED);

            mPhone.getContext().registerReceiver(mIntentReceiver, filter);
        } else {
            loge("mPhone is null.");
        }

        initializeDatabaseEmergencyNumberList();
        mCi.registerForEmergencyNumberList(this, EVENT_UNSOL_EMERGENCY_NUMBER_LIST, null);
    }

    /**
     * Message handler for updating emergency number list from RIL, updating emergency number list
     * from database if the country ISO is changed, and notifying the change of emergency number
     * list.
     *
     * @param msg The message
     */
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_UNSOL_EMERGENCY_NUMBER_LIST:
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.result == null) {
                    loge("EVENT_UNSOL_EMERGENCY_NUMBER_LIST: Result from RIL is null.");
                } else if ((ar.result != null) && (ar.exception == null)) {
                    updateRadioEmergencyNumberListAndNotify((List<EmergencyNumber>) ar.result);
                } else {
                    loge("EVENT_UNSOL_EMERGENCY_NUMBER_LIST: Exception from RIL : "
                            + ar.exception);
                }
                break;
            case EVENT_UPDATE_DB_COUNTRY_ISO_CHANGED:
                if (msg.obj == null) {
                    loge("EVENT_UPDATE_DB_COUNTRY_ISO_CHANGED: Result from UpdateCountryIso is"
                            + " null.");
                } else {
                    updateEmergencyNumberListDatabaseAndNotify((String) msg.obj);
                }
                break;
            case EVENT_UPDATE_EMERGENCY_NUMBER_TEST_MODE:
                if (msg.obj == null && msg.arg1 != RESET_EMERGENCY_NUMBER_TEST_MODE) {
                    loge("EVENT_UPDATE_EMERGENCY_NUMBER_TEST_MODE: Result from"
                            + " executeEmergencyNumberTestModeCommand is null.");
                } else {
                    updateEmergencyNumberListTestModeAndNotify(
                            msg.arg1, (EmergencyNumber) msg.obj);
                }
                break;
            case EVENT_UPDATE_EMERGENCY_NUMBER_PREFIX:
                if (msg.obj == null) {
                    loge("EVENT_UPDATE_EMERGENCY_NUMBER_PREFIX: Result from"
                            + " onCarrierConfigChanged is null.");
                } else {
                    updateEmergencyNumberPrefixAndNotify((String[]) msg.obj);
                }
                break;
            case EVENT_UPDATE_OTA_EMERGENCY_NUMBER_DB:
                updateOtaEmergencyNumberListDatabaseAndNotify();
                break;
            case EVENT_OVERRIDE_OTA_EMERGENCY_NUMBER_DB_FILE_PATH:
                if (msg.obj == null) {
                    overrideOtaEmergencyNumberDbFilePath(null);
                } else {
                    overrideOtaEmergencyNumberDbFilePath((ParcelFileDescriptor) msg.obj);
                }
                break;
        }
    }

    private boolean isAirplaneModeEnabled() {
        ServiceStateTracker serviceStateTracker = mPhone.getServiceStateTracker();
        if (serviceStateTracker != null) {
            if (serviceStateTracker.getServiceState().getState()
                    == ServiceState.STATE_POWER_OFF) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if it's sim absent to decide whether to apply sim-absent emergency numbers from 3gpp
     */
    @VisibleForTesting
    public boolean isSimAbsent() {
        for (Phone phone: PhoneFactory.getPhones()) {
            int slotId = SubscriptionController.getInstance().getSlotIndex(phone.getSubId());
            // If slot id is invalid, it means that there is no sim card.
            if (slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                // If there is at least one sim active, sim is not absent; it returns false.
                return false;
            }
        }
        return true;
    }

    private void initializeDatabaseEmergencyNumberList() {
        // If country iso has been cached when listener is set, don't need to cache the initial
        // country iso and initial database.
        if (mCountryIso == null) {
            String countryForDatabaseCache = getInitialCountryIso().toLowerCase();
            updateEmergencyCountryIso(countryForDatabaseCache);
            // Use the last known country to cache the database in APM
            if (TextUtils.isEmpty(countryForDatabaseCache)
                    && isAirplaneModeEnabled()) {
                countryForDatabaseCache = getCountryIsoForCachingDatabase();
            }
            cacheEmergencyDatabaseByCountry(countryForDatabaseCache);
        }
    }

    /**
     * Update Emergency country iso for all the Phones
     */
    @VisibleForTesting
    public void updateEmergencyCountryIsoAllPhones(String countryIso) {
        // Notify country iso change for current Phone
        mIsCountrySetByAnotherSub = false;
        updateEmergencyNumberDatabaseCountryChange(countryIso);

        // Share and notify country iso change for other Phones if the country
        // iso in their emergency number tracker is not available or the country
        // iso there is set by another active subscription.
        for (Phone phone: PhoneFactory.getPhones()) {
            if (phone.getPhoneId() == mPhone.getPhoneId()) {
                continue;
            }
            EmergencyNumberTracker emergencyNumberTracker;
            if (phone != null && phone.getEmergencyNumberTracker() != null) {
                emergencyNumberTracker = phone.getEmergencyNumberTracker();
                // If signal is lost, do not update the empty country iso for other slots.
                if (!TextUtils.isEmpty(countryIso)) {
                    if (TextUtils.isEmpty(emergencyNumberTracker.getEmergencyCountryIso())
                            || emergencyNumberTracker.mIsCountrySetByAnotherSub) {
                        emergencyNumberTracker.mIsCountrySetByAnotherSub = true;
                        emergencyNumberTracker.updateEmergencyNumberDatabaseCountryChange(
                            countryIso);
                    }
                }
            }
        }
    }

    private void onCarrierConfigChanged() {
        if (mPhone != null) {
            CarrierConfigManager configMgr = (CarrierConfigManager)
                    mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configMgr != null) {
                PersistableBundle b = configMgr.getConfigForSubId(mPhone.getSubId());
                if (b != null) {
                    String[] emergencyNumberPrefix = b.getStringArray(
                            CarrierConfigManager.KEY_EMERGENCY_NUMBER_PREFIX_STRING_ARRAY);
                    if (!Arrays.equals(mEmergencyNumberPrefix, emergencyNumberPrefix)) {
                        this.obtainMessage(EVENT_UPDATE_EMERGENCY_NUMBER_PREFIX,
                                emergencyNumberPrefix).sendToTarget();
                    }
                }
            }
        } else {
            loge("onCarrierConfigChanged mPhone is null.");
        }
    }

    private String getInitialCountryIso() {
        if (mPhone != null) {
            ServiceStateTracker sst = mPhone.getServiceStateTracker();
            if (sst != null) {
                LocaleTracker lt = sst.getLocaleTracker();
                if (lt != null) {
                    return lt.getCurrentCountry();
                }
            }
        } else {
            loge("getInitialCountryIso mPhone is null.");

        }
        return "";
    }

    /**
     * Update Emergency Number database based on changed Country ISO.
     *
     * @param countryIso
     *
     * @hide
     */
    public void updateEmergencyNumberDatabaseCountryChange(String countryIso) {
        this.obtainMessage(EVENT_UPDATE_DB_COUNTRY_ISO_CHANGED, countryIso).sendToTarget();
    }

    /**
     * Update changed OTA Emergency Number database.
     *
     * @hide
     */
    public void updateOtaEmergencyNumberDatabase() {
        this.obtainMessage(EVENT_UPDATE_OTA_EMERGENCY_NUMBER_DB).sendToTarget();
    }

    /**
     * Override the OTA Emergency Number database file path.
     *
     * @hide
     */
    public void updateOtaEmergencyNumberDbFilePath(ParcelFileDescriptor otaParcelFileDescriptor) {
        this.obtainMessage(
                EVENT_OVERRIDE_OTA_EMERGENCY_NUMBER_DB_FILE_PATH,
                        otaParcelFileDescriptor).sendToTarget();
    }

    /**
     * Override the OTA Emergency Number database file path.
     *
     * @hide
     */
    public void resetOtaEmergencyNumberDbFilePath() {
        this.obtainMessage(
                EVENT_OVERRIDE_OTA_EMERGENCY_NUMBER_DB_FILE_PATH, null).sendToTarget();
    }

    private EmergencyNumber convertEmergencyNumberFromEccInfo(EccInfo eccInfo, String countryIso) {
        String phoneNumber = eccInfo.phoneNumber.trim();
        if (phoneNumber.isEmpty()) {
            loge("EccInfo has empty phone number.");
            return null;
        }
        int emergencyServiceCategoryBitmask = 0;
        for (int typeData : eccInfo.types) {
            switch (typeData) {
                case EccInfo.Type.POLICE:
                    emergencyServiceCategoryBitmask |=
                            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_POLICE;
                    break;
                case EccInfo.Type.AMBULANCE:
                    emergencyServiceCategoryBitmask |=
                            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_AMBULANCE;
                    break;
                case EccInfo.Type.FIRE:
                    emergencyServiceCategoryBitmask |=
                            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE;
                    break;
                case EccInfo.Type.MARINE_GUARD:
                    emergencyServiceCategoryBitmask |=
                            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD;
                    break;
                case EccInfo.Type.MOUNTAIN_RESCUE:
                    emergencyServiceCategoryBitmask |=
                            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE;
                    break;
                case EccInfo.Type.MIEC:
                    emergencyServiceCategoryBitmask |=
                            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MIEC;
                    break;
                case EccInfo.Type.AIEC:
                    emergencyServiceCategoryBitmask |=
                            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_AIEC;
                    break;
                default:
                    // Ignores unknown types.
            }
        }
        return new EmergencyNumber(phoneNumber, countryIso, "", emergencyServiceCategoryBitmask,
                new ArrayList<String>(), EmergencyNumber.EMERGENCY_NUMBER_SOURCE_DATABASE,
                EmergencyNumber.EMERGENCY_CALL_ROUTING_UNKNOWN);
    }

    private void cacheEmergencyDatabaseByCountry(String countryIso) {
        BufferedInputStream inputStream = null;
        ProtobufEccData.AllInfo allEccMessages = null;
        int assetsDatabaseVersion = INVALID_DATABASE_VERSION;

        // Read the Asset emergency number database
        List<EmergencyNumber> updatedAssetEmergencyNumberList = new ArrayList<>();
        try {
            inputStream = new BufferedInputStream(
                    mPhone.getContext().getAssets().open(EMERGENCY_NUMBER_DB_ASSETS_FILE));
            allEccMessages = ProtobufEccData.AllInfo.parseFrom(readInputStreamToByteArray(
                    new GZIPInputStream(inputStream)));
            assetsDatabaseVersion = allEccMessages.revision;
            logd(countryIso + " asset emergency database is loaded. Ver: " + assetsDatabaseVersion
                    + " Phone Id: " + mPhone.getPhoneId());
            for (ProtobufEccData.CountryInfo countryEccInfo : allEccMessages.countries) {
                if (countryEccInfo.isoCode.equals(countryIso.toUpperCase())) {
                    for (ProtobufEccData.EccInfo eccInfo : countryEccInfo.eccs) {
                        updatedAssetEmergencyNumberList.add(convertEmergencyNumberFromEccInfo(
                                eccInfo, countryIso));
                    }
                }
            }
            EmergencyNumber.mergeSameNumbersInEmergencyNumberList(updatedAssetEmergencyNumberList);
        } catch (IOException ex) {
            logw("Cache asset emergency database failure: " + ex);
        } finally {
            // close quietly by catching non-runtime exceptions.
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (RuntimeException rethrown) {
                    throw rethrown;
                } catch (Exception ignored) {
                }
            }
        }

        // Cache OTA emergency number database
        int otaDatabaseVersion = cacheOtaEmergencyNumberDatabase();

        // Use a valid database that has higher version.
        if (otaDatabaseVersion == INVALID_DATABASE_VERSION
                && assetsDatabaseVersion == INVALID_DATABASE_VERSION) {
            loge("No database available. Phone Id: " + mPhone.getPhoneId());
            return;
        } else if (assetsDatabaseVersion > otaDatabaseVersion) {
            logd("Using Asset Emergency database. Version: " + assetsDatabaseVersion);
            mCurrentDatabaseVersion = assetsDatabaseVersion;
            mEmergencyNumberListFromDatabase = updatedAssetEmergencyNumberList;
        } else {
            logd("Using Ota Emergency database. Version: " + otaDatabaseVersion);
        }
    }

    private int cacheOtaEmergencyNumberDatabase() {
        FileInputStream fileInputStream = null;
        BufferedInputStream inputStream = null;
        ProtobufEccData.AllInfo allEccMessages = null;
        int otaDatabaseVersion = INVALID_DATABASE_VERSION;

        // Read the OTA emergency number database
        List<EmergencyNumber> updatedOtaEmergencyNumberList = new ArrayList<>();
        try {
            // If OTA File partition is not available, try to reload the default one.
            if (mOverridedOtaDbParcelFileDescriptor == null) {
                fileInputStream = new FileInputStream(
                        new File(Environment.getDataDirectory(),
                                EMERGENCY_NUMBER_DB_OTA_FILE_PATH));
            } else {
                File file = ParcelFileDescriptor
                        .getFile(mOverridedOtaDbParcelFileDescriptor.getFileDescriptor());
                fileInputStream = new FileInputStream(new File(file.getAbsolutePath()));
            }
            inputStream = new BufferedInputStream(fileInputStream);
            allEccMessages = ProtobufEccData.AllInfo.parseFrom(readInputStreamToByteArray(
                    new GZIPInputStream(inputStream)));
            String countryIso = getLastKnownEmergencyCountryIso();
            logd(countryIso + " ota emergency database is loaded. Ver: " + otaDatabaseVersion);
            otaDatabaseVersion = allEccMessages.revision;
            for (ProtobufEccData.CountryInfo countryEccInfo : allEccMessages.countries) {
                if (countryEccInfo.isoCode.equals(countryIso.toUpperCase())) {
                    for (ProtobufEccData.EccInfo eccInfo : countryEccInfo.eccs) {
                        updatedOtaEmergencyNumberList.add(convertEmergencyNumberFromEccInfo(
                                eccInfo, countryIso));
                    }
                }
            }
            EmergencyNumber.mergeSameNumbersInEmergencyNumberList(updatedOtaEmergencyNumberList);
        } catch (IOException ex) {
            loge("Cache ota emergency database IOException: " + ex);
        } finally {
            // Close quietly by catching non-runtime exceptions.
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (RuntimeException rethrown) {
                    throw rethrown;
                } catch (Exception ignored) {
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (RuntimeException rethrown) {
                    throw rethrown;
                } catch (Exception ignored) {
                }
            }
        }

        // Use a valid database that has higher version.
        if (otaDatabaseVersion != INVALID_DATABASE_VERSION
                && mCurrentDatabaseVersion < otaDatabaseVersion) {
            mCurrentDatabaseVersion = otaDatabaseVersion;
            mEmergencyNumberListFromDatabase = updatedOtaEmergencyNumberList;
        }
        return otaDatabaseVersion;
    }

    /**
     * Util function to convert inputStream to byte array before parsing proto data.
     */
    private static byte[] readInputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        int size = 16 * 1024; // Read 16k chunks
        byte[] data = new byte[size];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private void updateRadioEmergencyNumberListAndNotify(
            List<EmergencyNumber> emergencyNumberListRadio) {
        Collections.sort(emergencyNumberListRadio);
        logd("updateRadioEmergencyNumberListAndNotify(): receiving " + emergencyNumberListRadio);
        if (!emergencyNumberListRadio.equals(mEmergencyNumberListFromRadio)) {
            try {
                EmergencyNumber.mergeSameNumbersInEmergencyNumberList(emergencyNumberListRadio);
                writeUpdatedEmergencyNumberListMetrics(emergencyNumberListRadio);
                mEmergencyNumberListFromRadio = emergencyNumberListRadio;
                if (!DBG) {
                    mEmergencyNumberListRadioLocalLog.log("updateRadioEmergencyNumberList:"
                            + emergencyNumberListRadio);
                }
                updateEmergencyNumberList();
                if (!DBG) {
                    mEmergencyNumberListLocalLog.log("updateRadioEmergencyNumberListAndNotify:"
                            + mEmergencyNumberList);
                }
                notifyEmergencyNumberList();
            } catch (NullPointerException ex) {
                loge("updateRadioEmergencyNumberListAndNotify() Phone already destroyed: " + ex
                        + " EmergencyNumberList not notified");
            }
        }
    }

    private void updateEmergencyNumberListDatabaseAndNotify(String countryIso) {
        logd("updateEmergencyNumberListDatabaseAndNotify(): receiving countryIso: "
                + countryIso);
        updateEmergencyCountryIso(countryIso.toLowerCase());
        // Use cached country iso in APM to load emergency number database.
        if (TextUtils.isEmpty(countryIso) && isAirplaneModeEnabled()) {
            countryIso = getCountryIsoForCachingDatabase();
            logd("updateEmergencyNumberListDatabaseAndNotify(): using cached APM country "
                    + countryIso);
        }
        cacheEmergencyDatabaseByCountry(countryIso);
        writeUpdatedEmergencyNumberListMetrics(mEmergencyNumberListFromDatabase);
        if (!DBG) {
            mEmergencyNumberListDatabaseLocalLog.log(
                    "updateEmergencyNumberListDatabaseAndNotify:"
                            + mEmergencyNumberListFromDatabase);
        }
        updateEmergencyNumberList();
        if (!DBG) {
            mEmergencyNumberListLocalLog.log("updateEmergencyNumberListDatabaseAndNotify:"
                    + mEmergencyNumberList);
        }
        notifyEmergencyNumberList();
    }

    private void overrideOtaEmergencyNumberDbFilePath(
            ParcelFileDescriptor otaParcelableFileDescriptor) {
        logd("overrideOtaEmergencyNumberDbFilePath:" + otaParcelableFileDescriptor);
        mOverridedOtaDbParcelFileDescriptor = otaParcelableFileDescriptor;
    }

    private void updateOtaEmergencyNumberListDatabaseAndNotify() {
        logd("updateOtaEmergencyNumberListDatabaseAndNotify():"
                + " receiving Emegency Number database OTA update");
        if (cacheOtaEmergencyNumberDatabase() != INVALID_DATABASE_VERSION) {
            writeUpdatedEmergencyNumberListMetrics(mEmergencyNumberListFromDatabase);
            if (!DBG) {
                mEmergencyNumberListDatabaseLocalLog.log(
                        "updateOtaEmergencyNumberListDatabaseAndNotify:"
                            + mEmergencyNumberListFromDatabase);
            }
            updateEmergencyNumberList();
            if (!DBG) {
                mEmergencyNumberListLocalLog.log("updateOtaEmergencyNumberListDatabaseAndNotify:"
                        + mEmergencyNumberList);
            }
            notifyEmergencyNumberList();
        }
    }

    private void updateEmergencyNumberPrefixAndNotify(String[] emergencyNumberPrefix) {
        logd("updateEmergencyNumberPrefixAndNotify(): receiving emergencyNumberPrefix: "
                + Arrays.toString(emergencyNumberPrefix));
        mEmergencyNumberPrefix = emergencyNumberPrefix;
        updateEmergencyNumberList();
        if (!DBG) {
            mEmergencyNumberListLocalLog.log("updateEmergencyNumberPrefixAndNotify:"
                    + mEmergencyNumberList);
        }
        notifyEmergencyNumberList();
    }

    private void notifyEmergencyNumberList() {
        try {
            if (getEmergencyNumberList() != null) {
                mPhone.notifyEmergencyNumberList();
                logd("notifyEmergencyNumberList(): notified");
            }
        } catch (NullPointerException ex) {
            loge("notifyEmergencyNumberList(): failure: Phone already destroyed: " + ex);
        }
    }

    /**
     * Update emergency numbers based on the radio, database, and test mode, if they are the same
     * emergency numbers.
     */
    private void updateEmergencyNumberList() {
        List<EmergencyNumber> mergedEmergencyNumberList =
                new ArrayList<>(mEmergencyNumberListFromDatabase);
        mergedEmergencyNumberList.addAll(mEmergencyNumberListFromRadio);
        // 'updateEmergencyNumberList' is called every time there is a change for emergency numbers
        // from radio indication, emergency numbers from database, emergency number prefix from
        // carrier config, or test mode emergency numbers, the emergency number prefix is changed
        // by carrier config, the emergency number list with prefix needs to be clear, and re-apply
        // the new prefix for the current emergency numbers.
        mEmergencyNumberListWithPrefix.clear();
        if (mEmergencyNumberPrefix.length != 0) {
            mEmergencyNumberListWithPrefix.addAll(getEmergencyNumberListWithPrefix(
                    mEmergencyNumberListFromRadio));
            mEmergencyNumberListWithPrefix.addAll(getEmergencyNumberListWithPrefix(
                    mEmergencyNumberListFromDatabase));
        }
        if (!DBG) {
            mEmergencyNumberListPrefixLocalLog.log("updateEmergencyNumberList:"
                    + mEmergencyNumberListWithPrefix);
        }
        mergedEmergencyNumberList.addAll(mEmergencyNumberListWithPrefix);
        mergedEmergencyNumberList.addAll(mEmergencyNumberListFromTestMode);
        EmergencyNumber.mergeSameNumbersInEmergencyNumberList(mergedEmergencyNumberList);
        mEmergencyNumberList = mergedEmergencyNumberList;
    }

    /**
     * Get the emergency number list.
     *
     * @return the emergency number list based on radio indication or ril.ecclist if radio
     *         indication not support from the HAL.
     */
    public List<EmergencyNumber> getEmergencyNumberList() {
        if (!mEmergencyNumberListFromRadio.isEmpty()) {
            return Collections.unmodifiableList(mEmergencyNumberList);
        } else {
            return getEmergencyNumberListFromEccListDatabaseAndTest();
        }
    }

    /**
     * Checks if the number is an emergency number in the current Phone.
     *
     * @return {@code true} if it is; {@code false} otherwise.
     */
    public boolean isEmergencyNumber(String number, boolean exactMatch) {
        if (number == null) {
            return false;
        }
        number = PhoneNumberUtils.stripSeparators(number);
        if (!mEmergencyNumberListFromRadio.isEmpty()) {
            for (EmergencyNumber num : mEmergencyNumberList) {
                // According to com.android.i18n.phonenumbers.ShortNumberInfo, in
                // these countries, if extra digits are added to an emergency number,
                // it no longer connects to the emergency service.
                String countryIso = getLastKnownEmergencyCountryIso();
                if (countryIso.equals("br") || countryIso.equals("cl")
                        || countryIso.equals("ni")) {
                    exactMatch = true;
                } else {
                    exactMatch = false || exactMatch;
                }
                if (exactMatch) {
                    if (num.getNumber().equals(number)) {
                        return true;
                    }
                } else {
                    if (number.startsWith(num.getNumber())) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            return isEmergencyNumberFromEccList(number, exactMatch)
                    || isEmergencyNumberFromDatabase(number) || isEmergencyNumberForTest(number);
        }
    }

    /**
     * Get the {@link EmergencyNumber} for the corresponding emergency number address.
     *
     * @param emergencyNumber - the supplied emergency number.
     * @return the {@link EmergencyNumber} for the corresponding emergency number address.
     */
    public EmergencyNumber getEmergencyNumber(String emergencyNumber) {
        emergencyNumber = PhoneNumberUtils.stripSeparators(emergencyNumber);
        for (EmergencyNumber num : getEmergencyNumberList()) {
            if (num.getNumber().equals(emergencyNumber)) {
                return num;
            }
        }
        return null;
    }

    /**
     * Get the emergency service categories for the corresponding emergency number. The only
     * trusted sources for the categories are the
     * {@link EmergencyNumber#EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING} and
     * {@link EmergencyNumber#EMERGENCY_NUMBER_SOURCE_SIM}.
     *
     * @param emergencyNumber - the supplied emergency number.
     * @return the emergency service categories for the corresponding emergency number.
     */
    public @EmergencyServiceCategories int getEmergencyServiceCategories(String emergencyNumber) {
        emergencyNumber = PhoneNumberUtils.stripSeparators(emergencyNumber);
        for (EmergencyNumber num : getEmergencyNumberList()) {
            if (num.getNumber().equals(emergencyNumber)) {
                if (num.isFromSources(EmergencyNumber.EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING)
                        || num.isFromSources(EmergencyNumber.EMERGENCY_NUMBER_SOURCE_SIM)) {
                    return num.getEmergencyServiceCategoryBitmask();
                }
            }
        }
        return EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED;
    }

    /**
     * Get the emergency call routing for the corresponding emergency number. The only trusted
     * source for the routing is {@link EmergencyNumber#EMERGENCY_NUMBER_SOURCE_DATABASE}.
     *
     * @param emergencyNumber - the supplied emergency number.
     * @return the emergency call routing for the corresponding emergency number.
     */
    public @EmergencyCallRouting int getEmergencyCallRouting(String emergencyNumber) {
        emergencyNumber = PhoneNumberUtils.stripSeparators(emergencyNumber);
        for (EmergencyNumber num : getEmergencyNumberList()) {
            if (num.getNumber().equals(emergencyNumber)) {
                if (num.isFromSources(EmergencyNumber.EMERGENCY_NUMBER_SOURCE_DATABASE)) {
                    return num.getEmergencyCallRouting();
                }
            }
        }
        return EmergencyNumber.EMERGENCY_CALL_ROUTING_UNKNOWN;
    }

    public String getEmergencyCountryIso() {
        return mCountryIso;
    }

    public String getLastKnownEmergencyCountryIso() {
        return mLastKnownEmergencyCountryIso;
    }

    private String getCountryIsoForCachingDatabase() {
        ServiceStateTracker sst = mPhone.getServiceStateTracker();
        if (sst != null) {
            LocaleTracker lt = sst.getLocaleTracker();
            if (lt != null) {
                return lt.getLastKnownCountryIso();
            }
        }
        return "";
    }

    public int getEmergencyNumberDbVersion() {
        return mCurrentDatabaseVersion;
    }

    private synchronized void updateEmergencyCountryIso(String countryIso) {
        mCountryIso = countryIso;
        if (!TextUtils.isEmpty(mCountryIso)) {
            mLastKnownEmergencyCountryIso = mCountryIso;
        }
        mCurrentDatabaseVersion = INVALID_DATABASE_VERSION;
    }

    /**
     * Get Emergency number list based on EccList. This util is used for solving backward
     * compatibility if device does not support the 1.4 IRadioIndication HAL that reports
     * emergency number list.
     */
    private List<EmergencyNumber> getEmergencyNumberListFromEccList() {
        List<EmergencyNumber> emergencyNumberList = new ArrayList<>();
        int slotId = SubscriptionController.getInstance().getSlotIndex(mPhone.getSubId());

        String ecclist = (slotId <= 0) ? "ril.ecclist" : ("ril.ecclist" + slotId);
        String emergencyNumbers = SystemProperties.get(ecclist, "");
        if (TextUtils.isEmpty(emergencyNumbers)) {
            // then read-only ecclist property since old RIL only uses this
            emergencyNumbers = SystemProperties.get("ro.ril.ecclist");
        }
        if (!TextUtils.isEmpty(emergencyNumbers)) {
            // searches through the comma-separated list for a match,
            // return true if one is found.
            for (String emergencyNum : emergencyNumbers.split(",")) {
                emergencyNumberList.add(getLabeledEmergencyNumberForEcclist(emergencyNum));
            }
        }
        emergencyNumbers = ((isSimAbsent()) ? "112,911,000,08,110,118,119,999" : "112,911");
        for (String emergencyNum : emergencyNumbers.split(",")) {
            emergencyNumberList.add(getLabeledEmergencyNumberForEcclist(emergencyNum));
        }
        if (mEmergencyNumberPrefix.length != 0) {
            emergencyNumberList.addAll(getEmergencyNumberListWithPrefix(emergencyNumberList));
        }
        EmergencyNumber.mergeSameNumbersInEmergencyNumberList(emergencyNumberList);
        return emergencyNumberList;
    }

    private List<EmergencyNumber> getEmergencyNumberListWithPrefix(
            List<EmergencyNumber> emergencyNumberList) {
        List<EmergencyNumber> emergencyNumberListWithPrefix = new ArrayList<>();
        if (emergencyNumberList != null) {
            for (EmergencyNumber num : emergencyNumberList) {
                for (String prefix : mEmergencyNumberPrefix) {
                    // If an emergency number has started with the prefix,
                    // no need to apply the prefix.
                    if (!num.getNumber().startsWith(prefix)) {
                        emergencyNumberListWithPrefix.add(new EmergencyNumber(
                            prefix + num.getNumber(), num.getCountryIso(),
                            num.getMnc(), num.getEmergencyServiceCategoryBitmask(),
                            num.getEmergencyUrns(), num.getEmergencyNumberSourceBitmask(),
                            num.getEmergencyCallRouting()));
                    }
                }
            }
        }
        return emergencyNumberListWithPrefix;
    }

    private boolean isEmergencyNumberForTest(String number) {
        number = PhoneNumberUtils.stripSeparators(number);
        for (EmergencyNumber num : mEmergencyNumberListFromTestMode) {
            if (num.getNumber().equals(number)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmergencyNumberFromDatabase(String number) {
        if (!mPhone.getHalVersion().greaterOrEqual(new HalVersion(1, 4))) {
            return false;
        }
        number = PhoneNumberUtils.stripSeparators(number);
        for (EmergencyNumber num : mEmergencyNumberListFromDatabase) {
            if (num.getNumber().equals(number)) {
                return true;
            }
        }
        List<EmergencyNumber> emergencyNumberListFromDatabaseWithPrefix =
                getEmergencyNumberListWithPrefix(mEmergencyNumberListFromDatabase);
        for (EmergencyNumber num : emergencyNumberListFromDatabaseWithPrefix) {
            if (num.getNumber().equals(number)) {
                return true;
            }
        }
        return false;
    }

    private EmergencyNumber getLabeledEmergencyNumberForEcclist(String number) {
        number = PhoneNumberUtils.stripSeparators(number);
        for (EmergencyNumber num : mEmergencyNumberListFromDatabase) {
            if (num.getNumber().equals(number)) {
                return new EmergencyNumber(number, getLastKnownEmergencyCountryIso().toLowerCase(),
                        "", num.getEmergencyServiceCategoryBitmask(),
                        new ArrayList<String>(), EmergencyNumber.EMERGENCY_NUMBER_SOURCE_DATABASE,
                        EmergencyNumber.EMERGENCY_CALL_ROUTING_UNKNOWN);
            }
        }
        return new EmergencyNumber(number, "", "",
                EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED,
                new ArrayList<String>(), 0,
                EmergencyNumber.EMERGENCY_CALL_ROUTING_UNKNOWN);
    }

    /**
     * Back-up old logics for {@link PhoneNumberUtils#isEmergencyNumberInternal} for legacy
     * and deprecate purpose.
     */
    private boolean isEmergencyNumberFromEccList(String number, boolean useExactMatch) {
        // If the number passed in is null, just return false:
        if (number == null) return false;

        // If the number passed in is a SIP address, return false, since the
        // concept of "emergency numbers" is only meaningful for calls placed
        // over the cell network.
        // (Be sure to do this check *before* calling extractNetworkPortionAlt(),
        // since the whole point of extractNetworkPortionAlt() is to filter out
        // any non-dialable characters (which would turn 'abc911def@example.com'
        // into '911', for example.))
        if (PhoneNumberUtils.isUriNumber(number)) {
            return false;
        }

        // Strip the separators from the number before comparing it
        // to the list.
        number = PhoneNumberUtils.extractNetworkPortionAlt(number);

        String emergencyNumbers = "";
        int slotId = SubscriptionController.getInstance().getSlotIndex(mPhone.getSubId());

        // retrieve the list of emergency numbers
        // check read-write ecclist property first
        String ecclist = (slotId <= 0) ? "ril.ecclist" : ("ril.ecclist" + slotId);

        emergencyNumbers = SystemProperties.get(ecclist, "");

        String countryIso = getLastKnownEmergencyCountryIso();
        logd("slotId:" + slotId + " country:" + countryIso + " emergencyNumbers: "
                +  emergencyNumbers);

        if (TextUtils.isEmpty(emergencyNumbers)) {
            // then read-only ecclist property since old RIL only uses this
            emergencyNumbers = SystemProperties.get("ro.ril.ecclist");
        }

        if (!TextUtils.isEmpty(emergencyNumbers)) {
            // searches through the comma-separated list for a match,
            // return true if one is found.
            for (String emergencyNum : emergencyNumbers.split(",")) {
                // According to com.android.i18n.phonenumbers.ShortNumberInfo, in
                // these countries, if extra digits are added to an emergency number,
                // it no longer connects to the emergency service.
                if (useExactMatch || countryIso.equals("br") || countryIso.equals("cl")
                        || countryIso.equals("ni")) {
                    if (number.equals(emergencyNum)) {
                        return true;
                    } else {
                        for (String prefix : mEmergencyNumberPrefix) {
                            if (number.equals(prefix + emergencyNum)) {
                                return true;
                            }
                        }
                    }
                } else {
                    if (number.startsWith(emergencyNum)) {
                        return true;
                    } else {
                        for (String prefix : mEmergencyNumberPrefix) {
                            if (number.startsWith(prefix + emergencyNum)) {
                                return true;
                            }
                        }
                    }
                }
            }
            // no matches found against the list!
            return false;
        }

        logd("System property doesn't provide any emergency numbers."
                + " Use embedded logic for determining ones.");

        // According spec 3GPP TS22.101, the following numbers should be
        // ECC numbers when SIM/USIM is not present.
        emergencyNumbers = ((isSimAbsent()) ? "112,911,000,08,110,118,119,999" : "112,911");

        for (String emergencyNum : emergencyNumbers.split(",")) {
            if (useExactMatch) {
                if (number.equals(emergencyNum)) {
                    return true;
                } else {
                    for (String prefix : mEmergencyNumberPrefix) {
                        if (number.equals(prefix + emergencyNum)) {
                            return true;
                        }
                    }
                }
            } else {
                if (number.startsWith(emergencyNum)) {
                    return true;
                } else {
                    for (String prefix : mEmergencyNumberPrefix) {
                        if (number.equals(prefix + emergencyNum)) {
                            return true;
                        }
                    }
                }
            }
        }

        // No ecclist system property, so use our own list.
        if (countryIso != null) {
            ShortNumberInfo info = ShortNumberInfo.getInstance();
            if (useExactMatch) {
                if (info.isEmergencyNumber(number, countryIso.toUpperCase())) {
                    return true;
                } else {
                    for (String prefix : mEmergencyNumberPrefix) {
                        if (info.isEmergencyNumber(prefix + number, countryIso.toUpperCase())) {
                            return true;
                        }
                    }
                }
                return false;
            } else {
                if (info.connectsToEmergencyNumber(number, countryIso.toUpperCase())) {
                    return true;
                } else {
                    for (String prefix : mEmergencyNumberPrefix) {
                        if (info.connectsToEmergencyNumber(prefix + number,
                                countryIso.toUpperCase())) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }

        return false;
    }

    /**
     * Execute command for updating emergency number for test mode.
     */
    public void executeEmergencyNumberTestModeCommand(int action, EmergencyNumber num) {
        this.obtainMessage(EVENT_UPDATE_EMERGENCY_NUMBER_TEST_MODE, action, 0, num).sendToTarget();
    }

    /**
     * Update emergency number list for test mode.
     */
    private void updateEmergencyNumberListTestModeAndNotify(int action, EmergencyNumber num) {
        if (action == ADD_EMERGENCY_NUMBER_TEST_MODE) {
            if (!isEmergencyNumber(num.getNumber(), true)) {
                mEmergencyNumberListFromTestMode.add(num);
            }
        } else if (action == RESET_EMERGENCY_NUMBER_TEST_MODE) {
            mEmergencyNumberListFromTestMode.clear();
        } else if (action == REMOVE_EMERGENCY_NUMBER_TEST_MODE) {
            mEmergencyNumberListFromTestMode.remove(num);
        } else {
            loge("updateEmergencyNumberListTestModeAndNotify: Unexpected action in test mode.");
            return;
        }
        if (!DBG) {
            mEmergencyNumberListTestModeLocalLog.log(
                    "updateEmergencyNumberListTestModeAndNotify:"
                            + mEmergencyNumberListFromTestMode);
        }
        updateEmergencyNumberList();
        if (!DBG) {
            mEmergencyNumberListLocalLog.log(
                    "updateEmergencyNumberListTestModeAndNotify:"
                            + mEmergencyNumberList);
        }
        notifyEmergencyNumberList();
    }

    private List<EmergencyNumber> getEmergencyNumberListFromEccListDatabaseAndTest() {
        List<EmergencyNumber> mergedEmergencyNumberList = getEmergencyNumberListFromEccList();
        if (mPhone.getHalVersion().greaterOrEqual(new HalVersion(1, 4))) {
            loge("getEmergencyNumberListFromEccListDatabaseAndTest: radio indication is"
                    + " unavailable in 1.4 HAL.");
            mergedEmergencyNumberList.addAll(mEmergencyNumberListFromDatabase);
            mergedEmergencyNumberList.addAll(getEmergencyNumberListWithPrefix(
                    mEmergencyNumberListFromDatabase));
        }
        mergedEmergencyNumberList.addAll(getEmergencyNumberListTestMode());
        EmergencyNumber.mergeSameNumbersInEmergencyNumberList(mergedEmergencyNumberList);
        return mergedEmergencyNumberList;
    }

    /**
     * Get emergency number list for test.
     */
    public List<EmergencyNumber> getEmergencyNumberListTestMode() {
        return Collections.unmodifiableList(mEmergencyNumberListFromTestMode);
    }

    @VisibleForTesting
    public List<EmergencyNumber> getRadioEmergencyNumberList() {
        return new ArrayList<>(mEmergencyNumberListFromRadio);
    }

    private static void logd(String str) {
        Rlog.d(TAG, str);
    }

    private static void logw(String str) {
        Rlog.w(TAG, str);
    }

    private static void loge(String str) {
        Rlog.e(TAG, str);
    }

    private void writeUpdatedEmergencyNumberListMetrics(
            List<EmergencyNumber> updatedEmergencyNumberList) {
        if (updatedEmergencyNumberList == null) {
            return;
        }
        for (EmergencyNumber num : updatedEmergencyNumberList) {
            TelephonyMetrics.getInstance().writeEmergencyNumberUpdateEvent(
                    mPhone.getPhoneId(), num, getEmergencyNumberDbVersion());
        }
    }

    /**
     * Dump Emergency Number List info in the tracking
     *
     * @param fd FileDescriptor
     * @param pw PrintWriter
     * @param args args
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        final IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        ipw.println(" Hal Version:" + mPhone.getHalVersion());
        ipw.println(" ========================================= ");

        ipw.println(" Country Iso:" + getEmergencyCountryIso());
        ipw.println(" ========================================= ");

        ipw.println(" Database Version:" + getEmergencyNumberDbVersion());
        ipw.println(" ========================================= ");

        ipw.println("mEmergencyNumberListDatabaseLocalLog:");
        ipw.increaseIndent();
        mEmergencyNumberListDatabaseLocalLog.dump(fd, pw, args);
        ipw.decreaseIndent();
        ipw.println(" ========================================= ");

        ipw.println("mEmergencyNumberListRadioLocalLog:");
        ipw.increaseIndent();
        mEmergencyNumberListRadioLocalLog.dump(fd, pw, args);
        ipw.decreaseIndent();
        ipw.println(" ========================================= ");

        ipw.println("mEmergencyNumberListPrefixLocalLog:");
        ipw.increaseIndent();
        mEmergencyNumberListPrefixLocalLog.dump(fd, pw, args);
        ipw.decreaseIndent();
        ipw.println(" ========================================= ");

        ipw.println("mEmergencyNumberListTestModeLocalLog:");
        ipw.increaseIndent();
        mEmergencyNumberListTestModeLocalLog.dump(fd, pw, args);
        ipw.decreaseIndent();
        ipw.println(" ========================================= ");

        ipw.println("mEmergencyNumberListLocalLog (valid >= 1.4 HAL):");
        ipw.increaseIndent();
        mEmergencyNumberListLocalLog.dump(fd, pw, args);
        ipw.decreaseIndent();
        ipw.println(" ========================================= ");

        int slotId = SubscriptionController.getInstance().getSlotIndex(mPhone.getSubId());
        String ecclist = (slotId <= 0) ? "ril.ecclist" : ("ril.ecclist" + slotId);
        ipw.println(" ril.ecclist: " + SystemProperties.get(ecclist, ""));
        ipw.println(" ========================================= ");

        ipw.println("Emergency Number List for Phone" + "(" + mPhone.getPhoneId() + ")");
        ipw.increaseIndent();
        ipw.println(getEmergencyNumberList());
        ipw.decreaseIndent();
        ipw.println(" ========================================= ");

        ipw.flush();
    }
}

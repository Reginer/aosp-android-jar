/*
 * Copyright 2022 The Android Open Source Project
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

package com.android.internal.telephony.subscription;

import android.annotation.CallbackExecutor;
import android.annotation.ColorInt;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Telephony;
import android.provider.Telephony.SimInfo;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.DataRoamingMode;
import android.telephony.SubscriptionManager.DeviceToDeviceStatusSharingPreference;
import android.telephony.SubscriptionManager.ProfileClass;
import android.telephony.SubscriptionManager.SimDisplayNameSource;
import android.telephony.SubscriptionManager.SubscriptionType;
import android.telephony.SubscriptionManager.UsageSetting;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.telephony.ims.ImsMmTelManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.function.TriConsumer;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The subscription database manager is the wrapper of {@link SimInfo}
 * table. It's a full memory cache of the entire subscription database, and the update can be
 * asynchronously or synchronously. The database's cache allows multi threads to read
 * simultaneously, if no write is ongoing.
 *
 * Note that from Android 14, directly writing into the subscription database through content
 * resolver with {@link SimInfo#CONTENT_URI} will cause cache/db out of sync. All the read/write
 * to the database should go through {@link SubscriptionManagerService}.
 */
public class SubscriptionDatabaseManager extends Handler {
    private static final String LOG_TAG = "SDMGR";

    /** Whether enabling verbose debugging message or not. */
    private static final boolean VDBG = false;

    /** Invalid database row index. */
    private static final int INVALID_ROW_INDEX = -1;

    /** The mapping from {@link SimInfo} table to {@link SubscriptionInfoInternal} get methods. */
    private static final Map<String, Function<SubscriptionInfoInternal, ?>>
            SUBSCRIPTION_GET_METHOD_MAP = Map.ofEntries(
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_UNIQUE_KEY_SUBSCRIPTION_ID,
                    SubscriptionInfoInternal::getSubscriptionId),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ICC_ID,
                    SubscriptionInfoInternal::getIccId),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_SIM_SLOT_INDEX,
                    SubscriptionInfoInternal::getSimSlotIndex),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_DISPLAY_NAME,
                    SubscriptionInfoInternal::getDisplayName),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CARRIER_NAME,
                    SubscriptionInfoInternal::getCarrierName),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_NAME_SOURCE,
                    SubscriptionInfoInternal::getDisplayNameSource),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_COLOR,
                    SubscriptionInfoInternal::getIconTint),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_NUMBER,
                    SubscriptionInfoInternal::getNumber),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_DATA_ROAMING,
                    SubscriptionInfoInternal::getDataRoaming),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_MCC_STRING,
                    SubscriptionInfoInternal::getMcc),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_MNC_STRING,
                    SubscriptionInfoInternal::getMnc),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_EHPLMNS,
                    SubscriptionInfoInternal::getEhplmns),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_HPLMNS,
                    SubscriptionInfoInternal::getHplmns),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_IS_EMBEDDED,
                    SubscriptionInfoInternal::getEmbedded),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CARD_ID,
                    SubscriptionInfoInternal::getCardString),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ACCESS_RULES,
                    SubscriptionInfoInternal::getNativeAccessRules),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ACCESS_RULES_FROM_CARRIER_CONFIGS,
                    SubscriptionInfoInternal::getCarrierConfigAccessRules),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_IS_REMOVABLE,
                    SubscriptionInfoInternal::getRemovableEmbedded),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_EXTREME_THREAT_ALERT,
                    SubscriptionInfoInternal::getCellBroadcastExtremeThreatAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_SEVERE_THREAT_ALERT,
                    SubscriptionInfoInternal::getCellBroadcastSevereThreatAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_AMBER_ALERT,
                    SubscriptionInfoInternal::getCellBroadcastAmberAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_EMERGENCY_ALERT,
                    SubscriptionInfoInternal::getCellBroadcastEmergencyAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_ALERT_SOUND_DURATION,
                    SubscriptionInfoInternal::getCellBroadcastAlertSoundDuration),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_ALERT_REMINDER_INTERVAL,
                    SubscriptionInfoInternal::getCellBroadcastAlertReminderInterval),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_ALERT_VIBRATE,
                    SubscriptionInfoInternal::getCellBroadcastAlertVibrationEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_ALERT_SPEECH,
                    SubscriptionInfoInternal::getCellBroadcastAlertSpeechEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_ETWS_TEST_ALERT,
                    SubscriptionInfoInternal::getCellBroadcastEtwsTestAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_CHANNEL_50_ALERT,
                    SubscriptionInfoInternal::getCellBroadcastAreaInfoMessageEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_CMAS_TEST_ALERT,
                    SubscriptionInfoInternal::getCellBroadcastTestAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_OPT_OUT_DIALOG,
                    SubscriptionInfoInternal::getCellBroadcastOptOutDialogEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ENHANCED_4G_MODE_ENABLED,
                    SubscriptionInfoInternal::getEnhanced4GModeEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_VT_IMS_ENABLED,
                    SubscriptionInfoInternal::getVideoTelephonyEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_WFC_IMS_ENABLED,
                    SubscriptionInfoInternal::getWifiCallingEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_WFC_IMS_MODE,
                    SubscriptionInfoInternal::getWifiCallingMode),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_WFC_IMS_ROAMING_MODE,
                    SubscriptionInfoInternal::getWifiCallingModeForRoaming),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_WFC_IMS_ROAMING_ENABLED,
                    SubscriptionInfoInternal::getWifiCallingEnabledForRoaming),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_IS_OPPORTUNISTIC,
                    SubscriptionInfoInternal::getOpportunistic),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_GROUP_UUID,
                    SubscriptionInfoInternal::getGroupUuid),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ISO_COUNTRY_CODE,
                    SubscriptionInfoInternal::getCountryIso),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CARRIER_ID,
                    SubscriptionInfoInternal::getCarrierId),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_PROFILE_CLASS,
                    SubscriptionInfoInternal::getProfileClass),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_SUBSCRIPTION_TYPE,
                    SubscriptionInfoInternal::getSubscriptionType),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_GROUP_OWNER,
                    SubscriptionInfoInternal::getGroupOwner),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ENABLED_MOBILE_DATA_POLICIES,
                    SubscriptionInfoInternal::getEnabledMobileDataPolicies),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_IMSI,
                    SubscriptionInfoInternal::getImsi),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_UICC_APPLICATIONS_ENABLED,
                    SubscriptionInfoInternal::getUiccApplicationsEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_IMS_RCS_UCE_ENABLED,
                    SubscriptionInfoInternal::getRcsUceEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CROSS_SIM_CALLING_ENABLED,
                    SubscriptionInfoInternal::getCrossSimCallingEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_RCS_CONFIG,
                    SubscriptionInfoInternal::getRcsConfig),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ALLOWED_NETWORK_TYPES_FOR_REASONS,
                    SubscriptionInfoInternal::getAllowedNetworkTypesForReasons),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_D2D_STATUS_SHARING,
                    SubscriptionInfoInternal::getDeviceToDeviceStatusSharingPreference),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_VOIMS_OPT_IN_STATUS,
                    SubscriptionInfoInternal::getVoImsOptInEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_D2D_STATUS_SHARING_SELECTED_CONTACTS,
                    SubscriptionInfoInternal::getDeviceToDeviceStatusSharingContacts),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_NR_ADVANCED_CALLING_ENABLED,
                    SubscriptionInfoInternal::getNrAdvancedCallingEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_PHONE_NUMBER_SOURCE_CARRIER,
                    SubscriptionInfoInternal::getNumberFromCarrier),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_PHONE_NUMBER_SOURCE_IMS,
                    SubscriptionInfoInternal::getNumberFromIms),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_PORT_INDEX,
                    SubscriptionInfoInternal::getPortIndex),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_USAGE_SETTING,
                    SubscriptionInfoInternal::getUsageSetting),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_TP_MESSAGE_REF,
                    SubscriptionInfoInternal::getLastUsedTPMessageReference),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_USER_HANDLE,
                    SubscriptionInfoInternal::getUserId),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_SATELLITE_ENABLED,
                    SubscriptionInfoInternal::getSatelliteEnabled)
    );

    /**
     * The mapping from columns in {@link android.provider.Telephony.SimInfo} table to
     * {@link SubscriptionDatabaseManager} setting integer methods.
     */
    private static final Map<String, TriConsumer<SubscriptionDatabaseManager, Integer, Integer>>
            SUBSCRIPTION_SET_INTEGER_METHOD_MAP = Map.ofEntries(
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_SIM_SLOT_INDEX,
                    SubscriptionDatabaseManager::setSimSlotIndex),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_NAME_SOURCE,
                    SubscriptionDatabaseManager::setDisplayNameSource),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_COLOR,
                    SubscriptionDatabaseManager::setIconTint),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_DATA_ROAMING,
                    SubscriptionDatabaseManager::setDataRoaming),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_IS_EMBEDDED,
                    SubscriptionDatabaseManager::setEmbedded),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_IS_REMOVABLE,
                    SubscriptionDatabaseManager::setRemovableEmbedded),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_EXTREME_THREAT_ALERT,
                    SubscriptionDatabaseManager::setCellBroadcastExtremeThreatAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_SEVERE_THREAT_ALERT,
                    SubscriptionDatabaseManager::setCellBroadcastSevereThreatAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_AMBER_ALERT,
                    SubscriptionDatabaseManager::setCellBroadcastAmberAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_EMERGENCY_ALERT,
                    SubscriptionDatabaseManager::setCellBroadcastEmergencyAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_ALERT_SOUND_DURATION,
                    SubscriptionDatabaseManager::setCellBroadcastAlertSoundDuration),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_ALERT_REMINDER_INTERVAL,
                    SubscriptionDatabaseManager::setCellBroadcastAlertReminderInterval),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_ALERT_VIBRATE,
                    SubscriptionDatabaseManager::setCellBroadcastAlertVibrationEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_ALERT_SPEECH,
                    SubscriptionDatabaseManager::setCellBroadcastAlertSpeechEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_ETWS_TEST_ALERT,
                    SubscriptionDatabaseManager::setCellBroadcastEtwsTestAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_CHANNEL_50_ALERT,
                    SubscriptionDatabaseManager::setCellBroadcastAreaInfoMessageEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_CMAS_TEST_ALERT,
                    SubscriptionDatabaseManager::setCellBroadcastTestAlertEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CB_OPT_OUT_DIALOG,
                    SubscriptionDatabaseManager::setCellBroadcastOptOutDialogEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ENHANCED_4G_MODE_ENABLED,
                    SubscriptionDatabaseManager::setEnhanced4GModeEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_VT_IMS_ENABLED,
                    SubscriptionDatabaseManager::setVideoTelephonyEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_WFC_IMS_ENABLED,
                    SubscriptionDatabaseManager::setWifiCallingEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_WFC_IMS_MODE,
                    SubscriptionDatabaseManager::setWifiCallingMode),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_WFC_IMS_ROAMING_MODE,
                    SubscriptionDatabaseManager::setWifiCallingModeForRoaming),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_WFC_IMS_ROAMING_ENABLED,
                    SubscriptionDatabaseManager::setWifiCallingEnabledForRoaming),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_IS_OPPORTUNISTIC,
                    SubscriptionDatabaseManager::setOpportunistic),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CARRIER_ID,
                    SubscriptionDatabaseManager::setCarrierId),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_PROFILE_CLASS,
                    SubscriptionDatabaseManager::setProfileClass),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_SUBSCRIPTION_TYPE,
                    SubscriptionDatabaseManager::setSubscriptionType),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_UICC_APPLICATIONS_ENABLED,
                    SubscriptionDatabaseManager::setUiccApplicationsEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_IMS_RCS_UCE_ENABLED,
                    SubscriptionDatabaseManager::setRcsUceEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CROSS_SIM_CALLING_ENABLED,
                    SubscriptionDatabaseManager::setCrossSimCallingEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_D2D_STATUS_SHARING,
                    SubscriptionDatabaseManager::setDeviceToDeviceStatusSharingPreference),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_VOIMS_OPT_IN_STATUS,
                    SubscriptionDatabaseManager::setVoImsOptInEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_NR_ADVANCED_CALLING_ENABLED,
                    SubscriptionDatabaseManager::setNrAdvancedCallingEnabled),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_PORT_INDEX,
                    SubscriptionDatabaseManager::setPortIndex),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_USAGE_SETTING,
                    SubscriptionDatabaseManager::setUsageSetting),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_TP_MESSAGE_REF,
                    SubscriptionDatabaseManager::setLastUsedTPMessageReference),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_USER_HANDLE,
                    SubscriptionDatabaseManager::setUserId),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_SATELLITE_ENABLED,
                    SubscriptionDatabaseManager::setSatelliteEnabled)
    );

    /**
     * The mapping from columns in {@link android.provider.Telephony.SimInfo} table to
     * {@link SubscriptionDatabaseManager} setting string methods.
     */
    private static final Map<String, TriConsumer<SubscriptionDatabaseManager, Integer, String>>
            SUBSCRIPTION_SET_STRING_METHOD_MAP = Map.ofEntries(
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ICC_ID,
                    SubscriptionDatabaseManager::setIccId),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_DISPLAY_NAME,
                    SubscriptionDatabaseManager::setDisplayName),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CARRIER_NAME,
                    SubscriptionDatabaseManager::setCarrierName),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_NUMBER,
                    SubscriptionDatabaseManager::setNumber),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_MCC_STRING,
                    SubscriptionDatabaseManager::setMcc),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_MNC_STRING,
                    SubscriptionDatabaseManager::setMnc),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_EHPLMNS,
                    SubscriptionDatabaseManager::setEhplmns),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_HPLMNS,
                    SubscriptionDatabaseManager::setHplmns),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_CARD_ID,
                    SubscriptionDatabaseManager::setCardString),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_GROUP_UUID,
                    SubscriptionDatabaseManager::setGroupUuid),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ISO_COUNTRY_CODE,
                    SubscriptionDatabaseManager::setCountryIso),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_GROUP_OWNER,
                    SubscriptionDatabaseManager::setGroupOwner),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ENABLED_MOBILE_DATA_POLICIES,
                    SubscriptionDatabaseManager::setEnabledMobileDataPolicies),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_IMSI,
                    SubscriptionDatabaseManager::setImsi),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ALLOWED_NETWORK_TYPES_FOR_REASONS,
                    SubscriptionDatabaseManager::setAllowedNetworkTypesForReasons),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_D2D_STATUS_SHARING_SELECTED_CONTACTS,
                    SubscriptionDatabaseManager::setDeviceToDeviceStatusSharingContacts),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_PHONE_NUMBER_SOURCE_CARRIER,
                    SubscriptionDatabaseManager::setNumberFromCarrier),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_PHONE_NUMBER_SOURCE_IMS,
                    SubscriptionDatabaseManager::setNumberFromIms)
    );

    /**
     * The mapping from columns in {@link android.provider.Telephony.SimInfo} table to
     * {@link SubscriptionDatabaseManager} setting byte array methods.
     */
    private static final Map<String, TriConsumer<SubscriptionDatabaseManager, Integer, byte[]>>
            SUBSCRIPTION_SET_BYTE_ARRAY_METHOD_MAP = Map.ofEntries(
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ACCESS_RULES,
                    SubscriptionDatabaseManager::setNativeAccessRules),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_ACCESS_RULES_FROM_CARRIER_CONFIGS,
                    SubscriptionDatabaseManager::setCarrierConfigAccessRules),
            new AbstractMap.SimpleImmutableEntry<>(
                    SimInfo.COLUMN_RCS_CONFIG,
                    SubscriptionDatabaseManager::setRcsConfig)
    );

    /**
     * The columns that should be in-sync between the subscriptions in the same group. Changing
     * the value in those fields will automatically apply to the rest of the subscriptions in the
     * group.
     *
     * @see SubscriptionManager#getSubscriptionsInGroup(ParcelUuid)
     */
    private static final Set<String> GROUP_SHARING_COLUMNS = Set.of(
            SimInfo.COLUMN_DISPLAY_NAME,
            SimInfo.COLUMN_NAME_SOURCE,
            SimInfo.COLUMN_COLOR,
            SimInfo.COLUMN_DATA_ROAMING,
            SimInfo.COLUMN_ENHANCED_4G_MODE_ENABLED,
            SimInfo.COLUMN_VT_IMS_ENABLED,
            SimInfo.COLUMN_WFC_IMS_ENABLED,
            SimInfo.COLUMN_WFC_IMS_MODE,
            SimInfo.COLUMN_WFC_IMS_ROAMING_MODE,
            SimInfo.COLUMN_WFC_IMS_ROAMING_ENABLED,
            SimInfo.COLUMN_ENABLED_MOBILE_DATA_POLICIES,
            SimInfo.COLUMN_UICC_APPLICATIONS_ENABLED,
            SimInfo.COLUMN_IMS_RCS_UCE_ENABLED,
            SimInfo.COLUMN_CROSS_SIM_CALLING_ENABLED,
            SimInfo.COLUMN_RCS_CONFIG,
            SimInfo.COLUMN_D2D_STATUS_SHARING,
            SimInfo.COLUMN_VOIMS_OPT_IN_STATUS,
            SimInfo.COLUMN_D2D_STATUS_SHARING_SELECTED_CONTACTS,
            SimInfo.COLUMN_NR_ADVANCED_CALLING_ENABLED,
            SimInfo.COLUMN_USER_HANDLE
    );

    /**
     * The deprecated columns that do not have corresponding set methods in
     * {@link SubscriptionDatabaseManager}.
     */
    private static final Set<String> DEPRECATED_DATABASE_COLUMNS = Set.of(
            SimInfo.COLUMN_DISPLAY_NUMBER_FORMAT,
            SimInfo.COLUMN_MCC,
            SimInfo.COLUMN_MNC,
            SimInfo.COLUMN_SIM_PROVISIONING_STATUS,
            SimInfo.COLUMN_IS_METERED,
            SimInfo.COLUMN_DATA_ENABLED_OVERRIDE_RULES,
            SimInfo.COLUMN_ALLOWED_NETWORK_TYPES
    );

    /** The context */
    @NonNull
    private final Context mContext;

    /** The callback used for passing events back to {@link SubscriptionManagerService}. */
    @NonNull
    private final SubscriptionDatabaseManagerCallback mCallback;

    /** UICC controller */
    private final UiccController mUiccController;

    /**
     * The read/write lock to protect the entire database access. Using a Re-entrant read lock as
     * much more read requests are expected than the write requests. All the access to
     * {@link #mAllSubscriptionInfoInternalCache} needs to be protected by this lock.
     */
    @NonNull
    private final ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

    /** Indicating whether access the database asynchronously or not. */
    private final boolean mAsyncMode;

    /** Local log for most important debug messages. */
    @NonNull
    private final LocalLog mLocalLog = new LocalLog(128);

    /**
     * The entire subscription database, including subscriptions from inserted, previously inserted
     * SIMs. This is the full memory cache of the subscription database. The key is the subscription
     * id. Note all the access to this map needs to be protected by the re-entrant lock
     * {@link #mReadWriteLock}.
     *
     * @see SimInfo
     */
    @GuardedBy("mReadWriteLock")
    @NonNull
    private final Map<Integer, SubscriptionInfoInternal> mAllSubscriptionInfoInternalCache =
            new HashMap<>(16);

    /** Whether database has been initialized after boot up. */
    @GuardedBy("this")
    private boolean mDatabaseInitialized = false;

    /**
     * This is the callback used for listening events from {@link SubscriptionDatabaseManager}.
     */
    public abstract static class SubscriptionDatabaseManagerCallback {
        /** The executor of the callback. */
        private final @NonNull Executor mExecutor;

        /**
         * Constructor
         *
         * @param executor The executor of the callback.
         */
        public SubscriptionDatabaseManagerCallback(@NonNull @CallbackExecutor Executor executor) {
            mExecutor = executor;
        }

        /**
         * @return The executor of the callback.
         */
        @VisibleForTesting
        public @NonNull Executor getExecutor() {
            return mExecutor;
        }

        /**
         * Invoke the callback from executor.
         *
         * @param runnable The callback method to invoke.
         */
        public void invokeFromExecutor(@NonNull Runnable runnable) {
            mExecutor.execute(runnable);
        }

        /**
         * Called when database has been initialized.
         */
        public abstract void onInitialized();

        /**
         * Called when subscription changed.
         *
         * @param subId The subscription id.
         */
        public abstract void onSubscriptionChanged(int subId);
    }

    /**
     * The constructor.
     *
     * @param context The context.
     * @param looper Looper for the handler.
     * @param callback Subscription database callback.
     */
    public SubscriptionDatabaseManager(@NonNull Context context, @NonNull Looper looper,
            @NonNull SubscriptionDatabaseManagerCallback callback) {
        super(looper);
        log("Created SubscriptionDatabaseManager.");
        mContext = context;
        mCallback = callback;
        mUiccController = UiccController.getInstance();
        mAsyncMode = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_subscription_database_async_update);
        initializeDatabase();
    }

    /**
     * Helper method to get specific field from {@link SubscriptionInfoInternal} by the database
     * column name. {@link SubscriptionInfoInternal} represent one single record in the
     * {@link SimInfo} table. So every column has a corresponding get method in
     * {@link SubscriptionInfoInternal} (except for unused or deprecated columns).
     *
     * @param subInfo The subscription info.
     * @param columnName The database column name.
     *
     * @return The corresponding value from {@link SubscriptionInfoInternal}.
     *
     * @throws IllegalArgumentException if {@code columnName} is invalid.
     *
     * @see android.provider.Telephony.SimInfo for all the columns.
     */
    @NonNull
    private static Object getSubscriptionInfoFieldByColumnName(
            @NonNull SubscriptionInfoInternal subInfo, @NonNull String columnName) {
        if (SUBSCRIPTION_GET_METHOD_MAP.containsKey(columnName)) {
            return SUBSCRIPTION_GET_METHOD_MAP.get(columnName).apply(subInfo);
        }
        throw new IllegalArgumentException("Invalid column name " + columnName);
    }

    /**
     * Get a specific field from the subscription database by {@code subId} and {@code columnName}.
     *
     * @param subId The subscription id.
     * @param columnName The database column name.
     *
     * @return The value from subscription database.
     *
     * @throws IllegalArgumentException if {@code subId} or {@code columnName} is invalid.
     *
     * @see android.provider.Telephony.SimInfo for all the columns.
     */
    @NonNull
    public Object getSubscriptionProperty(int subId, @NonNull String columnName) {
        SubscriptionInfoInternal subInfo = getSubscriptionInfoInternal(subId);
        if (subInfo == null) {
            throw new IllegalArgumentException("getSubscriptionProperty: Invalid subId " + subId
                    + ", columnName=" + columnName);
        }

        return getSubscriptionInfoFieldByColumnName(subInfo, columnName);
    }

    /**
     * Set a field in the subscription database. Note not all fields are supported.
     *
     * @param subId Subscription Id of Subscription.
     * @param columnName Column name in the database. Note not all fields are supported.
     * @param value Value to store in the database.
     *
     * @throws IllegalArgumentException if {@code subId} or {@code columnName} is invalid, or
     * {@code value} cannot be converted to the corresponding database column format.
     * @throws NumberFormatException if a string value cannot be converted to integer.
     * @throws ClassCastException if {@code value} cannot be casted to the required type.
     *
     * @see android.provider.Telephony.SimInfo for all the columns.
     */
    public void setSubscriptionProperty(int subId, @NonNull String columnName,
            @NonNull Object value) {
        if (SUBSCRIPTION_SET_INTEGER_METHOD_MAP.containsKey(columnName)) {
            // For integer type columns, accepting both integer and string that can be converted to
            // integer.
            int intValue;
            if (value instanceof String) {
                intValue = Integer.parseInt((String) value);
            } else if (value instanceof Integer) {
                intValue = (int) value;
            } else {
                throw new ClassCastException("columnName=" + columnName + ", cannot cast "
                        + value.getClass() + " to integer.");
            }
            SUBSCRIPTION_SET_INTEGER_METHOD_MAP.get(columnName).accept(this, subId, intValue);
        } else if (SUBSCRIPTION_SET_STRING_METHOD_MAP.containsKey(columnName)) {
            // For string type columns. Will throw exception if value is not string type.
            SUBSCRIPTION_SET_STRING_METHOD_MAP.get(columnName).accept(this, subId, (String) value);
        } else if (SUBSCRIPTION_SET_BYTE_ARRAY_METHOD_MAP.containsKey(columnName)) {
            // For byte array type columns, accepting both byte[] and string that can be converted
            // to byte[] using base 64 encoding/decoding.
            byte[] byteArrayValue;
            if (value instanceof String) {
                byteArrayValue = Base64.decode((String) value, Base64.DEFAULT);
            } else if (value instanceof byte[]) {
                byteArrayValue = (byte[]) value;
            } else {
                throw new ClassCastException("columnName=" + columnName + ", cannot cast "
                        + value.getClass() + " to byte[].");
            }
            SUBSCRIPTION_SET_BYTE_ARRAY_METHOD_MAP.get(columnName).accept(
                    this, subId, byteArrayValue);
        } else {
            throw new IllegalArgumentException("Does not support set " + columnName + ".");
        }
    }

    /**
     * Comparing the old/new {@link SubscriptionInfoInternal} and create content values for database
     * update. If any field in the new subscription info is different from the old one, then each
     * delta will be added into the {@link ContentValues}.
     *
     * @param oldSubInfo The old {@link SubscriptionInfoInternal}.
     * @param newSubInfo The new {@link SubscriptionInfoInternal}.
     *
     * @return The delta content values for database update.
     */
    @NonNull
    private ContentValues createDeltaContentValues(@Nullable SubscriptionInfoInternal oldSubInfo,
            @NonNull SubscriptionInfoInternal newSubInfo) {
        ContentValues deltaContentValues = new ContentValues();

        for (String columnName : Telephony.SimInfo.getAllColumns()) {
            if (DEPRECATED_DATABASE_COLUMNS.contains(columnName)) continue;
            // subId is generated by the database. Cannot be updated.
            if (columnName.equals(SimInfo.COLUMN_UNIQUE_KEY_SUBSCRIPTION_ID)) continue;
            Object newValue = getSubscriptionInfoFieldByColumnName(newSubInfo, columnName);
            if (newValue != null) {
                Object oldValue = null;
                if (oldSubInfo != null) {
                    oldValue = getSubscriptionInfoFieldByColumnName(oldSubInfo, columnName);
                }
                // Some columns need special handling. We need to convert them into a format that
                // is accepted by the database.
                if (!Objects.equals(oldValue, newValue)) {
                    deltaContentValues.putObject(columnName, newValue);
                }
            }
        }
        return deltaContentValues;
    }

    /**
     * Synchronously insert a new record into the database. This operation is synchronous because
     * we need to convert the inserted row index into the subscription id.
     *
     * @param contentValues The fields of the subscription to be inserted into the database.
     *
     * @return The row index of the new record. {@link #INVALID_ROW_INDEX} if insertion failed.
     */
    private int insertNewRecordIntoDatabaseSync(@NonNull ContentValues contentValues) {
        Objects.requireNonNull(contentValues);
        Uri uri = mContext.getContentResolver().insert(SimInfo.CONTENT_URI, contentValues);
        if (uri != null && uri.getLastPathSegment() != null) {
            int subId = Integer.parseInt(uri.getLastPathSegment());
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                logv("insertNewRecordIntoDatabaseSync: contentValues=" + contentValues);
                logl("insertNewRecordIntoDatabaseSync: Successfully added subscription. subId="
                        + uri.getLastPathSegment());
                return subId;
            }
        }

        logel("insertNewRecordIntoDatabaseSync: Failed to insert subscription into database. "
                + "contentValues=" + contentValues);
        return INVALID_ROW_INDEX;
    }

    /**
     * Insert a new subscription info. The subscription info must have subscription id
     * {@link SubscriptionManager#INVALID_SUBSCRIPTION_ID}. Note this is a slow method, so be
     * cautious to call this method.
     *
     * @param subInfo The subscription info to update.
     *
     * @return The new subscription id. {@link SubscriptionManager#INVALID_SUBSCRIPTION_ID} (-1) if
     * insertion fails.
     */
    public int insertSubscriptionInfo(@NonNull SubscriptionInfoInternal subInfo) {
        Objects.requireNonNull(subInfo);
        // A new subscription to be inserted must have invalid subscription id.
        if (SubscriptionManager.isValidSubscriptionId(subInfo.getSubscriptionId())) {
            throw new RuntimeException("insertSubscriptionInfo: Not a new subscription to "
                    + "insert. subInfo=" + subInfo);
        }

        synchronized (this) {
            if (!mDatabaseInitialized) {
                throw new IllegalStateException(
                        "Database has not been initialized. Can't insert new "
                                + "record at this point.");
            }
        }

        int subId;
        // Grab the write lock so no other threads can read or write the cache.
        mReadWriteLock.writeLock().lock();
        try {
            // Synchronously insert into the database. Note this should be the only synchronous
            // write operation performed by the subscription database manager. The reason is that
            // we need to get the sub id for cache update.
            subId = insertNewRecordIntoDatabaseSync(createDeltaContentValues(null, subInfo));
            if (subId > 0) {
                mAllSubscriptionInfoInternalCache.put(subId, new SubscriptionInfoInternal
                        .Builder(subInfo)
                        .setId(subId).build());
            } else {
                logel("insertSubscriptionInfo: Failed to insert a new subscription. subInfo="
                        + subInfo);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }

        mCallback.invokeFromExecutor(() -> mCallback.onSubscriptionChanged(subId));
        return subId;
    }

    /**
     * Remove a subscription record from the database.
     *
     * @param subId The subscription id of the subscription to be deleted.
     *
     * @throws IllegalArgumentException If {@code subId} is invalid.
     */
    public void removeSubscriptionInfo(int subId) {
        if (!mAllSubscriptionInfoInternalCache.containsKey(subId)) {
            throw new IllegalArgumentException("subId " + subId + " is invalid.");
        }

        mReadWriteLock.writeLock().lock();
        try {
            if (mContext.getContentResolver().delete(SimInfo.CONTENT_URI,
                    SimInfo.COLUMN_UNIQUE_KEY_SUBSCRIPTION_ID + "=?",
                    new String[]{Integer.toString(subId)}) > 0) {
                mAllSubscriptionInfoInternalCache.remove(subId);
            } else {
                logel("Failed to remove subscription with subId=" + subId);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }

        mCallback.invokeFromExecutor(() -> mCallback.onSubscriptionChanged(subId));
    }

    /**
     * Update a subscription in the database (synchronously or asynchronously).
     *
     * @param subId The subscription id of the subscription to be updated.
     * @param contentValues The fields to be update.
     *
     * @return The number of rows updated. Note if the database is configured as asynchronously
     * update, then this will be always 1.
     */
    private int updateDatabase(int subId, @NonNull ContentValues contentValues) {
        logv("updateDatabase: prepare to update sub " + subId);

        synchronized (this) {
            if (!mDatabaseInitialized) {
                logel("updateDatabase: Database has not been initialized. Can't update database at "
                        + "this point. contentValues=" + contentValues);
                return 0;
            }
        }

        if (mAsyncMode) {
            // Perform the update in the handler thread asynchronously.
            post(() -> {
                mContext.getContentResolver().update(Uri.withAppendedPath(
                        SimInfo.CONTENT_URI, String.valueOf(subId)), contentValues, null, null);
                logv("updateDatabase: async updated subscription in the database."
                            + " subId=" + subId + ", contentValues= " + contentValues.getValues());
            });
            return 1;
        } else {
            logv("updateDatabase: sync updated subscription in the database."
                    + " subId=" + subId + ", contentValues= " + contentValues.getValues());

            return mContext.getContentResolver().update(Uri.withAppendedPath(
                    SimInfo.CONTENT_URI, String.valueOf(subId)), contentValues, null, null);
        }
    }

    /**
     * Update a certain field of subscription in the database. Also update the subscription cache
     * {@link #mAllSubscriptionInfoInternalCache}.
     *
     * @param subId The subscription id.
     * @param columnName The database column name from the database table {@link SimInfo}.
     * @param newValue The new value to update the subscription info cache
     * {@link #mAllSubscriptionInfoInternalCache}.
     * @param builderSetMethod The {@link SubscriptionInfo.Builder} method to set a specific field
     * when constructing the new {@link SubscriptionInfo}. This should be one of the
     * SubscriptionInfoInternal.Builder.setXxxx method.
     * @param <T> The type of newValue for subscription cache update.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    private <T> void writeDatabaseAndCacheHelper(int subId, @NonNull String columnName,
            @Nullable T newValue,
            BiFunction<SubscriptionInfoInternal.Builder, T, SubscriptionInfoInternal.Builder>
                    builderSetMethod) {
        ContentValues contentValues = new ContentValues();

        // Grab the write lock so no other threads can read or write the cache.
        mReadWriteLock.writeLock().lock();
        try {
            final SubscriptionInfoInternal oldSubInfo =
                    mAllSubscriptionInfoInternalCache.get(subId);
            if (oldSubInfo == null) {
                logel("Subscription doesn't exist. subId=" + subId + ", columnName=" + columnName);
                throw new IllegalArgumentException("Subscription doesn't exist. subId=" + subId
                        + ", columnName=" + columnName);
            }

            // Check if writing this field should automatically write to the rest of subscriptions
            // in the same group.
            final boolean syncToGroup = GROUP_SHARING_COLUMNS.contains(columnName);

            mAllSubscriptionInfoInternalCache.forEach((id, subInfo) -> {
                if (id == subId || (syncToGroup && !oldSubInfo.getGroupUuid().isEmpty()
                        && oldSubInfo.getGroupUuid().equals(subInfo.getGroupUuid()))) {
                    // Check if the new value is different from the old value in the cache.
                    if (!Objects.equals(getSubscriptionInfoFieldByColumnName(subInfo, columnName),
                            newValue)) {
                        logv("writeDatabaseAndCacheHelper: subId=" + subId + ",columnName="
                                + columnName + ", newValue=" + newValue);
                        // If the value is different, then we need to update the cache. Since all
                        // fields in SubscriptionInfo are final, we need to create a new
                        // SubscriptionInfo.
                        SubscriptionInfoInternal.Builder builder = new SubscriptionInfoInternal
                                .Builder(subInfo);

                        // Apply the new value to the builder. This line is equivalent to
                        // builder.setXxxxxx(newValue);
                        builder = builderSetMethod.apply(builder, newValue);

                        // Prepare the content value for update.
                        contentValues.putObject(columnName, newValue);
                        if (updateDatabase(id, contentValues) > 0) {
                            // Update the subscription database cache.
                            mAllSubscriptionInfoInternalCache.put(id, builder.build());
                            mCallback.invokeFromExecutor(()
                                    -> mCallback.onSubscriptionChanged(subId));
                        }
                    }
                }
            });
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Update the database with the {@link SubscriptionInfoInternal}, and also update the cache.
     *
     * @param newSubInfo The new {@link SubscriptionInfoInternal}.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void updateSubscription(@NonNull SubscriptionInfoInternal newSubInfo) {
        Objects.requireNonNull(newSubInfo);

        // Grab the write lock so no other threads can read or write the cache.
        mReadWriteLock.writeLock().lock();
        try {
            int subId = newSubInfo.getSubscriptionId();
            SubscriptionInfoInternal oldSubInfo = mAllSubscriptionInfoInternalCache.get(
                    newSubInfo.getSubscriptionId());
            if (oldSubInfo == null) {
                throw new IllegalArgumentException("updateSubscription: subscription does not "
                        + "exist. subId=" + subId);
            }
            if (oldSubInfo.equals(newSubInfo)) return;

            if (updateDatabase(subId, createDeltaContentValues(oldSubInfo, newSubInfo)) > 0) {
                mAllSubscriptionInfoInternalCache.put(subId, newSubInfo);
                mCallback.invokeFromExecutor(() -> mCallback.onSubscriptionChanged(subId));
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Set the ICCID of the SIM that is associated with the subscription.
     *
     * @param subId Subscription id.
     * @param iccId The ICCID of the SIM that is associated with this subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setIccId(int subId, @NonNull String iccId) {
        Objects.requireNonNull(iccId);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_ICC_ID, iccId,
                SubscriptionInfoInternal.Builder::setIccId);
    }

    /**
     * Set the SIM index of the slot that currently contains the subscription. Set to
     * {@link SubscriptionManager#INVALID_SIM_SLOT_INDEX} if the subscription is inactive.
     *
     * @param subId Subscription id.
     * @param simSlotIndex The SIM slot index.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setSimSlotIndex(int subId, int simSlotIndex) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_SIM_SLOT_INDEX, simSlotIndex,
                SubscriptionInfoInternal.Builder::setSimSlotIndex);
    }

    /**
     * Set the name displayed to the user that identifies this subscription. This name is used
     * in Settings page and can be renamed by the user.
     *
     * @param subId Subscription id.
     * @param displayName The display name.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setDisplayName(int subId, @NonNull String displayName) {
        Objects.requireNonNull(displayName);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_DISPLAY_NAME, displayName,
                SubscriptionInfoInternal.Builder::setDisplayName);
    }

    /**
     * Set the name displayed to the user that identifies subscription provider name. This name
     * is the SPN displayed in status bar and many other places. Can't be renamed by the user.
     *
     * @param subId Subscription id.
     * @param carrierName The carrier name.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCarrierName(int subId, @NonNull String carrierName) {
        Objects.requireNonNull(carrierName);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CARRIER_NAME, carrierName,
                SubscriptionInfoInternal.Builder::setCarrierName);
    }

    /**
     * Set the source of the display name.
     *
     * @param subId Subscription id.
     * @param displayNameSource The source of the display name.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     *
     * @see SubscriptionInfo#getDisplayName()
     */
    public void setDisplayNameSource(int subId, @SimDisplayNameSource int displayNameSource) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_NAME_SOURCE, displayNameSource,
                SubscriptionInfoInternal.Builder::setDisplayNameSource);
    }

    /**
     * Set the color to be used for tinting the icon when displaying to the user.
     *
     * @param subId Subscription id.
     * @param iconTint The color to be used for tinting the icon when displaying to the user.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setIconTint(int subId, @ColorInt int iconTint) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_COLOR, iconTint,
                SubscriptionInfoInternal.Builder::setIconTint);
    }

    /**
     * Set the number presented to the user identify this subscription.
     *
     * @param subId Subscription id.
     * @param number the number presented to the user identify this subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setNumber(int subId, @NonNull String number) {
        Objects.requireNonNull(number);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_NUMBER, number,
                SubscriptionInfoInternal.Builder::setNumber);
    }

    /**
     * Set whether user enables data roaming for this subscription or not.
     *
     * @param subId Subscription id.
     * @param dataRoaming Data roaming mode. Either
     * {@link SubscriptionManager#DATA_ROAMING_ENABLE} or
     * {@link SubscriptionManager#DATA_ROAMING_DISABLE}
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setDataRoaming(int subId, @DataRoamingMode int dataRoaming) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_DATA_ROAMING, dataRoaming,
                SubscriptionInfoInternal.Builder::setDataRoaming);
    }

    /**
     * Set the mobile country code.
     *
     * @param subId Subscription id.
     * @param mcc The mobile country code.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setMcc(int subId, @NonNull String mcc) {
        Objects.requireNonNull(mcc);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_MCC_STRING, mcc,
                SubscriptionInfoInternal.Builder::setMcc);
    }

    /**
     * Set the mobile network code.
     *
     * @param subId Subscription id.
     * @param mnc Mobile network code.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setMnc(int subId, @NonNull String mnc) {
        Objects.requireNonNull(mnc);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_MNC_STRING, mnc,
                SubscriptionInfoInternal.Builder::setMnc);
    }

    /**
     * Set EHPLMNs associated with the subscription.
     *
     * @param subId Subscription id.
     * @param ehplmns EHPLMNs associated with the subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setEhplmns(int subId, @NonNull String[] ehplmns) {
        Objects.requireNonNull(ehplmns);
        setEhplmns(subId, Arrays.stream(ehplmns)
                .filter(Predicate.not(TextUtils::isEmpty))
                .collect(Collectors.joining(",")));
    }

    /**
     * Set EHPLMNs associated with the subscription.
     *
     * @param subId Subscription id.
     * @param ehplmns EHPLMNs associated with the subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setEhplmns(int subId, @NonNull String ehplmns) {
        Objects.requireNonNull(ehplmns);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_EHPLMNS, ehplmns,
                SubscriptionInfoInternal.Builder::setEhplmns);
    }

    /**
     * Set HPLMNs associated with the subscription.
     *
     * @param subId Subscription id.
     * @param hplmns HPLMNs associated with the subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setHplmns(int subId, @NonNull String[] hplmns) {
        Objects.requireNonNull(hplmns);
        setHplmns(subId, Arrays.stream(hplmns)
                .filter(Predicate.not(TextUtils::isEmpty))
                .collect(Collectors.joining(",")));
    }

    /**
     * Set HPLMNs associated with the subscription.
     *
     * @param subId Subscription id.
     * @param hplmns HPLMNs associated with the subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setHplmns(int subId, @NonNull String hplmns) {
        Objects.requireNonNull(hplmns);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_HPLMNS, hplmns,
                SubscriptionInfoInternal.Builder::setHplmns);
    }

    /**
     * Set whether the subscription is from eSIM or not.
     *
     * @param subId Subscription id.
     * @param isEmbedded if the subscription is from eSIM.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setEmbedded(int subId, int isEmbedded) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_IS_EMBEDDED, isEmbedded,
                SubscriptionInfoInternal.Builder::setEmbedded);
    }

    /**
     * Set whether the subscription is from eSIM or not.
     *
     * @param subId Subscription id.
     * @param isEmbedded {@code true} if the subscription is from eSIM.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setEmbedded(int subId, boolean isEmbedded) {
        setEmbedded(subId, isEmbedded ? 1 : 0);
    }

    /**
     * Set the card string of the SIM card. This is usually the ICCID or EID.
     *
     * @param subId Subscription id.
     * @param cardString The card string of the SIM card.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     *
     * @see SubscriptionInfo#getCardString()
     */
    public void setCardString(int subId, @NonNull String cardString) {
        Objects.requireNonNull(cardString);
        // Also update the public card id.
        setCardId(subId, mUiccController.convertToPublicCardId(cardString));
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CARD_ID, cardString,
                SubscriptionInfoInternal.Builder::setCardString);
    }

    /**
     * Set the card id. This is the non-PII card id converted from
     * {@link SubscriptionInfoInternal#getCardString()}. This field only exists in
     * {@link SubscriptionInfo}, but not the database.
     *
     * @param subId Subscription id.
     * @param cardId The card id.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCardId(int subId, int cardId) {
        // card id does not have a corresponding SimInfo column. So we only update the cache.

        // Grab the write lock so no other threads can read or write the cache.
        mReadWriteLock.writeLock().lock();
        try {
            SubscriptionInfoInternal subInfoCache = mAllSubscriptionInfoInternalCache.get(subId);
            if (subInfoCache == null) {
                throw new IllegalArgumentException("setCardId: Subscription doesn't exist. subId="
                        + subId);
            }
            mAllSubscriptionInfoInternalCache.put(subId,
                    new SubscriptionInfoInternal.Builder(subInfoCache)
                            .setCardId(cardId).build());
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Set the native access rules for this subscription, if it is embedded and defines any.
     * This does not include access rules for non-embedded subscriptions.
     *
     * @param subId Subscription id.
     * @param nativeAccessRules The native access rules for this subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setNativeAccessRules(int subId, @NonNull byte[] nativeAccessRules) {
        Objects.requireNonNull(nativeAccessRules);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_ACCESS_RULES, nativeAccessRules,
                SubscriptionInfoInternal.Builder::setNativeAccessRules);
    }

    /**
     * Set the carrier certificates for this subscription that are saved in carrier configs.
     * This does not include access rules from the Uicc, whether embedded or non-embedded.
     *
     * @param subId Subscription id.
     * @param carrierConfigAccessRules The carrier certificates for this subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCarrierConfigAccessRules(int subId, @NonNull byte[] carrierConfigAccessRules) {
        Objects.requireNonNull(carrierConfigAccessRules);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_ACCESS_RULES_FROM_CARRIER_CONFIGS,
                carrierConfigAccessRules,
                SubscriptionInfoInternal.Builder::setCarrierConfigAccessRules);
    }

    /**
     * Set the carrier certificates for this subscription that are saved in carrier configs.
     * This does not include access rules from the Uicc, whether embedded or non-embedded.
     *
     * @param subId Subscription id.
     * @param carrierConfigAccessRules The carrier certificates for this subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCarrierConfigAccessRules(int subId,
            @NonNull UiccAccessRule[] carrierConfigAccessRules) {
        Objects.requireNonNull(carrierConfigAccessRules);
        byte[] carrierConfigAccessRulesBytes = UiccAccessRule.encodeRules(carrierConfigAccessRules);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_ACCESS_RULES_FROM_CARRIER_CONFIGS,
                carrierConfigAccessRulesBytes,
                SubscriptionInfoInternal.Builder::setCarrierConfigAccessRules);
    }

    /**
     * Set whether an embedded subscription is on a removable card. Such subscriptions are
     * marked inaccessible as soon as the current card is removed. Otherwise, they will remain
     * accessible unless explicitly deleted. Only meaningful for embedded subscription.
     *
     * @param subId Subscription id.
     * @param isRemovableEmbedded if the subscription is from the removable embedded SIM.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setRemovableEmbedded(int subId, int isRemovableEmbedded) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_IS_REMOVABLE, isRemovableEmbedded,
                SubscriptionInfoInternal.Builder::setRemovableEmbedded);
    }

    /**
     * Set whether cell broadcast extreme threat alert is enabled by the user or not.
     *
     * @param subId Subscription id.
     * @param isExtremeThreatAlertEnabled whether cell broadcast extreme threat alert is enabled by
     * the user or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastExtremeThreatAlertEnabled(int subId,
            int isExtremeThreatAlertEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_EXTREME_THREAT_ALERT,
                isExtremeThreatAlertEnabled,
                SubscriptionInfoInternal.Builder::setCellBroadcastExtremeThreatAlertEnabled);
    }

    /**
     * Set whether cell broadcast severe threat alert is enabled by the user or not.
     *
     * @param subId Subscription id.
     * @param isSevereThreatAlertEnabled whether cell broadcast severe threat alert is enabled by
     * the user or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastSevereThreatAlertEnabled(int subId,
            int isSevereThreatAlertEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_SEVERE_THREAT_ALERT,
                isSevereThreatAlertEnabled,
                SubscriptionInfoInternal.Builder::setCellBroadcastSevereThreatAlertEnabled);
    }

    /**
     * Set whether cell broadcast amber alert is enabled by the user or not.
     *
     * @param subId Subscription id.
     * @param isAmberAlertEnabled whether cell broadcast amber alert is enabled by
     * the user or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastAmberAlertEnabled(int subId, int isAmberAlertEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_AMBER_ALERT, isAmberAlertEnabled,
                SubscriptionInfoInternal.Builder::setCellBroadcastAmberAlertEnabled);
    }

    /**
     * Set whether cell broadcast emergency alert is enabled by the user or not.
     *
     * @param subId Subscription id.
     * @param isEmergencyAlertEnabled whether cell broadcast emergency alert is enabled by
     * the user or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastEmergencyAlertEnabled(int subId,
            int isEmergencyAlertEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_EMERGENCY_ALERT,
                isEmergencyAlertEnabled,
                SubscriptionInfoInternal.Builder::setCellBroadcastEmergencyAlertEnabled);
    }

    /**
     * Set cell broadcast alert sound duration.
     *
     * @param subId Subscription id.
     * @param alertSoundDuration Alert sound duration in seconds.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastAlertSoundDuration(int subId, int alertSoundDuration) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_ALERT_SOUND_DURATION,
                alertSoundDuration,
                SubscriptionInfoInternal.Builder::setCellBroadcastAlertSoundDuration);
    }

    /**
     * Set cell broadcast alert reminder interval.
     *
     * @param subId Subscription id.
     * @param reminderInterval Alert reminder interval in milliseconds.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastAlertReminderInterval(int subId, int reminderInterval) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_ALERT_REMINDER_INTERVAL,
                reminderInterval,
                SubscriptionInfoInternal.Builder::setCellBroadcastAlertReminderInterval);
    }

    /**
     * Set whether cell broadcast alert vibration is enabled by the user or not.
     *
     * @param subId Subscription id.
     * @param isAlertVibrationEnabled whether cell broadcast alert vibration is enabled by the user
     * or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastAlertVibrationEnabled(int subId, int isAlertVibrationEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_ALERT_VIBRATE, isAlertVibrationEnabled,
                SubscriptionInfoInternal.Builder::setCellBroadcastAlertVibrationEnabled);
    }

    /**
     * Set whether cell broadcast alert speech is enabled by the user or not.
     *
     * @param subId Subscription id.
     * @param isAlertSpeechEnabled whether cell broadcast alert speech is enabled by the user or
     * not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastAlertSpeechEnabled(int subId, int isAlertSpeechEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_ALERT_SPEECH, isAlertSpeechEnabled,
                SubscriptionInfoInternal.Builder::setCellBroadcastAlertSpeechEnabled);
    }

    /**
     * Set whether ETWS test alert is enabled by the user or not.
     *
     * @param subId Subscription id.
     * @param isEtwsTestAlertEnabled whether cell broadcast ETWS test alert is enabled by the user
     * or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastEtwsTestAlertEnabled(int subId, int isEtwsTestAlertEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_ETWS_TEST_ALERT,
                isEtwsTestAlertEnabled,
                SubscriptionInfoInternal.Builder::setCellBroadcastEtwsTestAlertEnabled);
    }

    /**
     * Set whether area info message is enabled by the user or not.
     *
     * @param subId Subscription id.
     * @param isAreaInfoMessageEnabled whether cell broadcast area info message is enabled by the
     * user or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastAreaInfoMessageEnabled(int subId, int isAreaInfoMessageEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_CHANNEL_50_ALERT,
                isAreaInfoMessageEnabled,
                SubscriptionInfoInternal.Builder::setCellBroadcastAreaInfoMessageEnabled);
    }

    /**
     * Set whether cell broadcast test alert is enabled by the user or not.
     *
     * @param subId Subscription id.
     * @param isTestAlertEnabled whether cell broadcast test alert is enabled by the user or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastTestAlertEnabled(int subId, int isTestAlertEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_CMAS_TEST_ALERT, isTestAlertEnabled,
                SubscriptionInfoInternal.Builder::setCellBroadcastTestAlertEnabled);
    }

    /**
     * Set whether cell broadcast opt-out dialog should be shown or not.
     *
     * @param subId Subscription id.
     * @param isOptOutDialogEnabled whether cell broadcast opt-out dialog should be shown or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCellBroadcastOptOutDialogEnabled(int subId, int isOptOutDialogEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CB_OPT_OUT_DIALOG, isOptOutDialogEnabled,
                SubscriptionInfoInternal.Builder::setCellBroadcastOptOutDialogEnabled);
    }

    /**
     * Set whether enhanced 4G mode is enabled by the user or not.
     *
     * @param subId Subscription id.
     * @param isEnhanced4GModeEnabled whether enhanced 4G mode is enabled by the user or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setEnhanced4GModeEnabled(int subId, int isEnhanced4GModeEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_ENHANCED_4G_MODE_ENABLED,
                isEnhanced4GModeEnabled,
                SubscriptionInfoInternal.Builder::setEnhanced4GModeEnabled);
    }

    /**
     * Set whether video telephony is enabled by the user or not.
     *
     * @param subId Subscription id.
     * @param isVideoTelephonyEnabled whether video telephony is enabled by the user or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setVideoTelephonyEnabled(int subId, int isVideoTelephonyEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_VT_IMS_ENABLED, isVideoTelephonyEnabled,
                SubscriptionInfoInternal.Builder::setVideoTelephonyEnabled);
    }

    /**
     * Set whether Wi-Fi calling is enabled by the user or not when the device is not roaming.
     *
     * @param subId Subscription id.
     * @param isWifiCallingEnabled whether Wi-Fi calling is enabled by the user or not when the
     * device is not roaming.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setWifiCallingEnabled(int subId, int isWifiCallingEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_WFC_IMS_ENABLED, isWifiCallingEnabled,
                SubscriptionInfoInternal.Builder::setWifiCallingEnabled);
    }

    /**
     * Set Wi-Fi calling mode when the device is not roaming.
     *
     * @param subId Subscription id.
     * @param wifiCallingMode Wi-Fi calling mode when the device is not roaming.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setWifiCallingMode(int subId,
            @ImsMmTelManager.WiFiCallingMode int wifiCallingMode) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_WFC_IMS_MODE, wifiCallingMode,
                SubscriptionInfoInternal.Builder::setWifiCallingMode);
    }

    /**
     * Set Wi-Fi calling mode when the device is roaming.
     *
     * @param subId Subscription id.
     * @param wifiCallingModeForRoaming Wi-Fi calling mode when the device is roaming.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setWifiCallingModeForRoaming(int subId,
            @ImsMmTelManager.WiFiCallingMode int wifiCallingModeForRoaming) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_WFC_IMS_ROAMING_MODE,
                wifiCallingModeForRoaming,
                SubscriptionInfoInternal.Builder::setWifiCallingModeForRoaming);
    }

    /**
     * Set whether Wi-Fi calling is enabled by the user or not when the device is roaming.
     *
     * @param subId Subscription id.
     * @param isWifiCallingEnabledForRoaming whether Wi-Fi calling is enabled by the user or not
     * when the device is roaming.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setWifiCallingEnabledForRoaming(int subId, int isWifiCallingEnabledForRoaming) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_WFC_IMS_ROAMING_ENABLED,
                isWifiCallingEnabledForRoaming,
                SubscriptionInfoInternal.Builder::setWifiCallingEnabledForRoaming);
    }

    /**
     * Set whether the subscription is opportunistic or not.
     *
     * @param subId Subscription id.
     * @param isOpportunistic if the subscription is opportunistic.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setOpportunistic(int subId, boolean isOpportunistic) {
        setOpportunistic(subId, isOpportunistic ? 1 : 0);
    }

    /**
     * Set whether the subscription is opportunistic or not.
     *
     * @param subId Subscription id.
     * @param isOpportunistic if the subscription is opportunistic.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setOpportunistic(int subId, int isOpportunistic) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_IS_OPPORTUNISTIC, isOpportunistic,
                SubscriptionInfoInternal.Builder::setOpportunistic);
    }

    /**
     * Set the group UUID of the subscription group.
     *
     * @param subId Subscription id.
     * @param groupUuid The group UUID.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     *
     * @see SubscriptionInfo#getGroupUuid()
     */
    public void setGroupUuid(int subId, @NonNull String groupUuid) {
        Objects.requireNonNull(groupUuid);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_GROUP_UUID, groupUuid,
                SubscriptionInfoInternal.Builder::setGroupUuid);
    }

    /**
     * Set the ISO Country code for the subscription's provider.
     *
     * @param subId Subscription id.
     * @param countryIso The ISO country code for the subscription's provider.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCountryIso(int subId, @NonNull String countryIso) {
        Objects.requireNonNull(countryIso);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_ISO_COUNTRY_CODE, countryIso,
                SubscriptionInfoInternal.Builder::setCountryIso);
    }

    /**
     * Set the subscription carrier id.
     *
     * @param subId Subscription id.
     * @param carrierId The carrier id.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     *
     * @see TelephonyManager#getSimCarrierId()
     */
    public void setCarrierId(int subId, int carrierId) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CARRIER_ID, carrierId,
                SubscriptionInfoInternal.Builder::setCarrierId);
    }

    /**
     * Set the profile class populated from the profile metadata if present.
     *
     * @param subId Subscription id.
     * @param profileClass the profile class populated from the profile metadata if present.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     *
     * @see SubscriptionInfo#getProfileClass()
     */
    public void setProfileClass(int subId, @ProfileClass int profileClass) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_PROFILE_CLASS, profileClass,
                SubscriptionInfoInternal.Builder::setProfileClass);
    }

    /**
     * Set the subscription type.
     *
     * @param subId Subscription id.
     * @param type Subscription type.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setSubscriptionType(int subId, @SubscriptionType int type) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_SUBSCRIPTION_TYPE, type,
                SubscriptionInfoInternal.Builder::setType);
    }

    /**
     * Set the owner package of group the subscription belongs to.
     *
     * @param subId Subscription id.
     * @param groupOwner Owner package of group the subscription belongs to.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setGroupOwner(int subId, @NonNull String groupOwner) {
        Objects.requireNonNull(groupOwner);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_GROUP_OWNER, groupOwner,
                SubscriptionInfoInternal.Builder::setGroupOwner);
    }

    /**
     * Set the enabled mobile data policies.
     *
     * @param subId Subscription id.
     * @param enabledMobileDataPolicies The enabled mobile data policies.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setEnabledMobileDataPolicies(int subId, @NonNull String enabledMobileDataPolicies) {
        Objects.requireNonNull(enabledMobileDataPolicies);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_ENABLED_MOBILE_DATA_POLICIES,
                enabledMobileDataPolicies,
                SubscriptionInfoInternal.Builder::setEnabledMobileDataPolicies);
    }

    /**
     * Set the IMSI (International Mobile Subscriber Identity) of the subscription.
     *
     * @param subId Subscription id.
     * @param imsi The IMSI.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setImsi(int subId, @NonNull String imsi) {
        Objects.requireNonNull(imsi);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_IMSI, imsi,
                SubscriptionInfoInternal.Builder::setImsi);
    }

    /**
     * Set whether Uicc applications are configured to enable or not.
     *
     * @param subId Subscription id.
     * @param areUiccApplicationsEnabled if Uicc applications are configured to enable.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setUiccApplicationsEnabled(int subId, boolean areUiccApplicationsEnabled) {
        setUiccApplicationsEnabled(subId, areUiccApplicationsEnabled ? 1 : 0);
    }

    /**
     * Set whether Uicc applications are configured to enable or not.
     *
     * @param subId Subscription id.
     * @param areUiccApplicationsEnabled if Uicc applications are configured to enable.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setUiccApplicationsEnabled(int subId, int areUiccApplicationsEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_UICC_APPLICATIONS_ENABLED,
                areUiccApplicationsEnabled,
                SubscriptionInfoInternal.Builder::setUiccApplicationsEnabled);
    }

    /**
     * Set whether the user has enabled IMS RCS User Capability Exchange (UCE) for this
     * subscription.
     *
     * @param subId Subscription id.
     * @param isRcsUceEnabled If the user enabled RCS UCE for this subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setRcsUceEnabled(int subId, int isRcsUceEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_IMS_RCS_UCE_ENABLED, isRcsUceEnabled,
                SubscriptionInfoInternal.Builder::setRcsUceEnabled);
    }

    /**
     * Set whether the user has enabled cross SIM calling for this subscription.
     *
     * @param subId Subscription id.
     * @param isCrossSimCallingEnabled If the user enabled cross SIM calling for this
     * subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setCrossSimCallingEnabled(int subId, int isCrossSimCallingEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_CROSS_SIM_CALLING_ENABLED,
                isCrossSimCallingEnabled,
                SubscriptionInfoInternal.Builder::setCrossSimCallingEnabled);
    }

    /**
     * Set the RCS config for this subscription.
     *
     * @param subId Subscription id.
     * @param rcsConfig The RCS config for this subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setRcsConfig(int subId, @NonNull byte[] rcsConfig) {
        Objects.requireNonNull(rcsConfig);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_RCS_CONFIG, rcsConfig,
                SubscriptionInfoInternal.Builder::setRcsConfig);
    }

    /**
     * Set the allowed network types for reasons.
     *
     * @param subId Subscription id.
     * @param allowedNetworkTypesForReasons The allowed network types for reasons in string
     * format. The format is "[reason]=[network types bitmask], [reason]=[network types bitmask],
     * ...". For example, "user=1239287394, thermal=298791239, carrier=3456812312".
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setAllowedNetworkTypesForReasons(int subId,
            @NonNull String allowedNetworkTypesForReasons) {
        Objects.requireNonNull(allowedNetworkTypesForReasons);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_ALLOWED_NETWORK_TYPES_FOR_REASONS,
                allowedNetworkTypesForReasons,
                SubscriptionInfoInternal.Builder::setAllowedNetworkTypesForReasons);
    }

    /**
     * Set device to device sharing status.
     *
     * @param subId Subscription id.
     * @param deviceToDeviceStatusSharingPreference Device to device sharing status.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setDeviceToDeviceStatusSharingPreference(int subId,
            @DeviceToDeviceStatusSharingPreference int deviceToDeviceStatusSharingPreference) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_D2D_STATUS_SHARING,
                deviceToDeviceStatusSharingPreference,
                SubscriptionInfoInternal.Builder::setDeviceToDeviceStatusSharingPreference);
    }

    /**
     * Set whether the user has opted-in voice over IMS.
     *
     * @param subId Subscription id.
     * @param isVoImsOptInEnabled Whether the user has opted-in voice over IMS.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setVoImsOptInEnabled(int subId, int isVoImsOptInEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_VOIMS_OPT_IN_STATUS, isVoImsOptInEnabled,
                SubscriptionInfoInternal.Builder::setVoImsOptInEnabled);
    }

    /**
     * Set contacts information that allow device to device sharing.
     *
     * @param subId Subscription id.
     * @param deviceToDeviceStatusSharingContacts contacts information that allow device to
     * device sharing.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setDeviceToDeviceStatusSharingContacts(int subId,
            @NonNull String deviceToDeviceStatusSharingContacts) {
        Objects.requireNonNull(deviceToDeviceStatusSharingContacts);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_D2D_STATUS_SHARING_SELECTED_CONTACTS,
                deviceToDeviceStatusSharingContacts,
                SubscriptionInfoInternal.Builder::setDeviceToDeviceStatusSharingContacts);
    }

    /**
     * Set whether the user has enabled NR advanced calling.
     *
     * @param subId Subscription id.
     * @param isNrAdvancedCallingEnabled Whether the user has enabled NR advanced calling.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setNrAdvancedCallingEnabled(int subId, int isNrAdvancedCallingEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_NR_ADVANCED_CALLING_ENABLED,
                isNrAdvancedCallingEnabled,
                SubscriptionInfoInternal.Builder::setNrAdvancedCallingEnabled);
    }

    /**
     * Set the phone number retrieved from carrier.
     *
     * @param subId Subscription id.
     * @param numberFromCarrier The phone number retrieved from carrier.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setNumberFromCarrier(int subId, @NonNull String numberFromCarrier) {
        Objects.requireNonNull(numberFromCarrier);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_PHONE_NUMBER_SOURCE_CARRIER,
                numberFromCarrier, SubscriptionInfoInternal.Builder::setNumberFromCarrier);
    }

    /**
     * Set the phone number retrieved from IMS.
     *
     * @param subId Subscription id.
     * @param numberFromIms The phone number retrieved from IMS.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setNumberFromIms(int subId, @NonNull String numberFromIms) {
        Objects.requireNonNull(numberFromIms);
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_PHONE_NUMBER_SOURCE_IMS,
                numberFromIms, SubscriptionInfoInternal.Builder::setNumberFromIms);
    }

    /**
     * Set the port index of the Uicc card.
     *
     * @param subId Subscription id.
     * @param portIndex The port index of the Uicc card.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setPortIndex(int subId, int portIndex) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_PORT_INDEX, portIndex,
                SubscriptionInfoInternal.Builder::setPortIndex);
    }

    /**
     * Set subscription's preferred usage setting.
     *
     * @param subId Subscription id.
     * @param usageSetting Subscription's preferred usage setting.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setUsageSetting(int subId, @UsageSetting int usageSetting) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_USAGE_SETTING, usageSetting,
                SubscriptionInfoInternal.Builder::setUsageSetting);
    }

    /**
     * Set last used TP message reference.
     *
     * @param subId Subscription id.
     * @param lastUsedTPMessageReference Last used TP message reference.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setLastUsedTPMessageReference(int subId, int lastUsedTPMessageReference) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_TP_MESSAGE_REF,
                lastUsedTPMessageReference,
                SubscriptionInfoInternal.Builder::setLastUsedTPMessageReference);
    }

    /**
     * Set the user id associated with this subscription.
     *
     * @param subId Subscription id.
     * @param userId The user id associated with this subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setUserId(int subId, @UserIdInt int userId) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_USER_HANDLE, userId,
                SubscriptionInfoInternal.Builder::setUserId);
    }

    /**
     * Set whether satellite is enabled or not.
     *
     * @param subId Subscription id.
     * @param isSatelliteEnabled if satellite is enabled or not.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setSatelliteEnabled(int subId, int isSatelliteEnabled) {
        writeDatabaseAndCacheHelper(subId, SimInfo.COLUMN_SATELLITE_ENABLED,
                isSatelliteEnabled,
                SubscriptionInfoInternal.Builder::setSatelliteEnabled);
    }

    /**
     * Set whether group of the subscription is disabled. This is only useful if it's a grouped
     * opportunistic subscription. In this case, if all primary (non-opportunistic)
     * subscriptions in the group are deactivated (unplugged pSIM or deactivated eSIM profile),
     * we should disable this opportunistic subscription.
     *
     * @param subId Subscription id.
     * @param isGroupDisabled if group of the subscription is disabled.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void setGroupDisabled(int subId, boolean isGroupDisabled) {
        // group disabled does not have a corresponding SimInfo column. So we only update the cache.

        // Grab the write lock so no other threads can read or write the cache.
        mReadWriteLock.writeLock().lock();
        try {
            SubscriptionInfoInternal subInfoCache = mAllSubscriptionInfoInternalCache.get(subId);
            if (subInfoCache == null) {
                throw new IllegalArgumentException("setGroupDisabled: Subscription doesn't exist. "
                        + "subId=" + subId);
            }
            mAllSubscriptionInfoInternalCache.put(subId,
                    new SubscriptionInfoInternal.Builder(subInfoCache)
                            .setGroupDisabled(isGroupDisabled).build());
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Reload the database from content provider to the cache.
     */
    public void reloadDatabase() {
        if (mAsyncMode) {
            post(this::loadDatabaseInternal);
        } else {
            loadDatabaseInternal();
        }
    }

    /**
     * Load the database from content provider to the cache.
     */
    private void loadDatabaseInternal() {
        log("loadDatabaseInternal");
        try (Cursor cursor = mContext.getContentResolver().query(
                SimInfo.CONTENT_URI, null, null, null, null)) {
            mReadWriteLock.writeLock().lock();
            try {
                Map<Integer, SubscriptionInfoInternal> newAllSubscriptionInfoInternalCache =
                        new HashMap<>();
                boolean changed = false;
                while (cursor != null && cursor.moveToNext()) {
                    SubscriptionInfoInternal subInfo = createSubscriptionInfoFromCursor(cursor);
                    newAllSubscriptionInfoInternalCache.put(subInfo.getSubscriptionId(), subInfo);
                    if (!Objects.equals(mAllSubscriptionInfoInternalCache
                            .get(subInfo.getSubscriptionId()), subInfo)) {
                        mCallback.invokeFromExecutor(() -> mCallback.onSubscriptionChanged(
                                subInfo.getSubscriptionId()));
                        changed = true;
                    }
                }

                if (changed) {
                    mAllSubscriptionInfoInternalCache.clear();
                    mAllSubscriptionInfoInternalCache.putAll(newAllSubscriptionInfoInternalCache);

                    logl("Loaded " + mAllSubscriptionInfoInternalCache.size()
                            + " records from the subscription database.");
                    mAllSubscriptionInfoInternalCache.forEach(
                            (subId, subInfo) -> log("  " + subInfo.toString()));
                }
            } finally {
                mReadWriteLock.writeLock().unlock();
            }
        }
    }

    /**
     * Initialize the database cache. Load the entire database into the cache.
     */
    private void initializeDatabase() {
        if (mAsyncMode) {
            // Load the database asynchronously.
            post(() -> {
                synchronized (this) {
                    loadDatabaseInternal();
                    mDatabaseInitialized = true;
                    mCallback.invokeFromExecutor(mCallback::onInitialized);
                }
            });
        } else {
            // Load the database synchronously.
            synchronized (this) {
                loadDatabaseInternal();
                mDatabaseInitialized = true;
                mCallback.invokeFromExecutor(mCallback::onInitialized);
            }
        }
    }

    /**
     * Build the {@link SubscriptionInfoInternal} from database.
     *
     * @param cursor  The cursor of the database.
     *
     * @return The subscription info from a single database record.
     */
    @NonNull
    private SubscriptionInfoInternal createSubscriptionInfoFromCursor(@NonNull Cursor cursor) {
        SubscriptionInfoInternal.Builder builder = new SubscriptionInfoInternal.Builder();
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(
                SimInfo.COLUMN_UNIQUE_KEY_SUBSCRIPTION_ID));
        builder.setId(id)
                .setIccId(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_ICC_ID))))
                .setSimSlotIndex(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_SIM_SLOT_INDEX)))
                .setDisplayName(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_DISPLAY_NAME))))
                .setCarrierName(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CARRIER_NAME))))
                .setDisplayNameSource(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_NAME_SOURCE)))
                .setIconTint(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_COLOR)))
                .setNumber(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_NUMBER))))
                .setDataRoaming(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_DATA_ROAMING)))
                .setMcc(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_MCC_STRING))))
                .setMnc(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_MNC_STRING))))
                .setEhplmns(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_EHPLMNS))))
                .setHplmns(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_HPLMNS))))
                .setEmbedded(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_IS_EMBEDDED)));

        String cardString = TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                SimInfo.COLUMN_CARD_ID)));
        builder.setCardString(cardString);
        // publicCardId is the publicly exposed int card ID
        int publicCardId = mUiccController.convertToPublicCardId(cardString);

        byte[] rules = cursor.getBlob(cursor.getColumnIndexOrThrow(SimInfo.COLUMN_ACCESS_RULES));
        if (rules != null) {
            builder.setNativeAccessRules(rules);
        }

        rules = cursor.getBlob(cursor.getColumnIndexOrThrow(
                SimInfo.COLUMN_ACCESS_RULES_FROM_CARRIER_CONFIGS));
        if (rules != null) {
            builder.setCarrierConfigAccessRules(rules);
        }

        byte[] config = cursor.getBlob(cursor.getColumnIndexOrThrow(SimInfo.COLUMN_RCS_CONFIG));
        if (config != null) {
            builder.setRcsConfig(config);
        }

        builder.setCardId(publicCardId)
                .setRemovableEmbedded(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_IS_REMOVABLE)))
                .setCellBroadcastExtremeThreatAlertEnabled(cursor.getInt(cursor
                        .getColumnIndexOrThrow(SimInfo.COLUMN_CB_EXTREME_THREAT_ALERT)))
                .setCellBroadcastSevereThreatAlertEnabled(cursor.getInt(cursor
                        .getColumnIndexOrThrow(SimInfo.COLUMN_CB_SEVERE_THREAT_ALERT)))
                .setCellBroadcastAmberAlertEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CB_AMBER_ALERT)))
                .setCellBroadcastEmergencyAlertEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CB_EMERGENCY_ALERT)))
                .setCellBroadcastAlertSoundDuration(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CB_ALERT_SOUND_DURATION)))
                .setCellBroadcastAlertReminderInterval(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CB_ALERT_REMINDER_INTERVAL)))
                .setCellBroadcastAlertVibrationEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CB_ALERT_VIBRATE)))
                .setCellBroadcastAlertSpeechEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CB_ALERT_SPEECH)))
                .setCellBroadcastEtwsTestAlertEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CB_ETWS_TEST_ALERT)))
                .setCellBroadcastAreaInfoMessageEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CB_CHANNEL_50_ALERT)))
                .setCellBroadcastTestAlertEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CB_CMAS_TEST_ALERT)))
                .setCellBroadcastOptOutDialogEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CB_OPT_OUT_DIALOG)))
                .setEnhanced4GModeEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_ENHANCED_4G_MODE_ENABLED)))
                .setVideoTelephonyEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_VT_IMS_ENABLED)))
                .setWifiCallingEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_WFC_IMS_ENABLED)))
                .setWifiCallingMode(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_WFC_IMS_MODE)))
                .setWifiCallingModeForRoaming(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_WFC_IMS_ROAMING_MODE)))
                .setWifiCallingEnabledForRoaming(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_WFC_IMS_ROAMING_ENABLED)))
                .setOpportunistic(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_IS_OPPORTUNISTIC)))
                .setGroupUuid(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_GROUP_UUID))))
                .setCountryIso(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_ISO_COUNTRY_CODE))))
                .setCarrierId(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CARRIER_ID)))
                .setProfileClass(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_PROFILE_CLASS)))
                .setType(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_SUBSCRIPTION_TYPE)))
                .setGroupOwner(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_GROUP_OWNER))))
                .setEnabledMobileDataPolicies(TextUtils.emptyIfNull(
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                SimInfo.COLUMN_ENABLED_MOBILE_DATA_POLICIES))))
                .setImsi(TextUtils.emptyIfNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_IMSI))))
                .setUiccApplicationsEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_UICC_APPLICATIONS_ENABLED)))
                .setRcsUceEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_IMS_RCS_UCE_ENABLED)))
                .setCrossSimCallingEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_CROSS_SIM_CALLING_ENABLED)))
                .setAllowedNetworkTypesForReasons(TextUtils.emptyIfNull(
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                SimInfo.COLUMN_ALLOWED_NETWORK_TYPES_FOR_REASONS))))
                .setDeviceToDeviceStatusSharingPreference(cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                SimInfo.COLUMN_D2D_STATUS_SHARING)))
                .setVoImsOptInEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_VOIMS_OPT_IN_STATUS)))
                .setDeviceToDeviceStatusSharingContacts(TextUtils.emptyIfNull(cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                SimInfo.COLUMN_D2D_STATUS_SHARING_SELECTED_CONTACTS))))
                .setNrAdvancedCallingEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_NR_ADVANCED_CALLING_ENABLED)))
                .setNumberFromCarrier(TextUtils.emptyIfNull(cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                SimInfo.COLUMN_PHONE_NUMBER_SOURCE_CARRIER))))
                .setNumberFromIms(TextUtils.emptyIfNull(cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                SimInfo.COLUMN_PHONE_NUMBER_SOURCE_IMS))))
                .setPortIndex(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_PORT_INDEX)))
                .setUsageSetting(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_USAGE_SETTING)))
                .setLastUsedTPMessageReference(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_TP_MESSAGE_REF)))
                .setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_USER_HANDLE)))
                .setSatelliteEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(
                        SimInfo.COLUMN_SATELLITE_ENABLED)));
        return builder.build();
    }

    /**
     * Sync the group sharing fields from reference subscription to the rest of the subscriptions in
     * the same group. For example, if user enables wifi calling, the same setting should be applied
     * to all subscriptions in the same group.
     *
     * @param subId The subscription id of reference subscription.
     *
     * @throws IllegalArgumentException if the subscription does not exist.
     */
    public void syncToGroup(int subId) {
        if (!mAllSubscriptionInfoInternalCache.containsKey(subId)) {
            throw new IllegalArgumentException("Invalid subId " + subId);
        }

        for (String column : GROUP_SHARING_COLUMNS) {
            // Get the value from the reference subscription, and set to itself again.
            // writeDatabaseAndCacheHelper() will automatically sync to the rest of the group.
            setSubscriptionProperty(subId, column, getSubscriptionProperty(subId, column));
        }
    }

    /**
     * Get the subscription info by subscription id.
     *
     * @param subId The subscription id.
     *
     * @return The subscription info. {@code null} if not found.
     */
    @Nullable
    public SubscriptionInfoInternal getSubscriptionInfoInternal(int subId) {
        mReadWriteLock.readLock().lock();
        try {
            return mAllSubscriptionInfoInternalCache.get(subId);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * @return All subscription infos in the database.
     */
    @NonNull
    public List<SubscriptionInfoInternal> getAllSubscriptions() {
        mReadWriteLock.readLock().lock();
        try {
            return new ArrayList<>(mAllSubscriptionInfoInternalCache.values());
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Get subscription info by ICCID.
     *
     * @param iccId The ICCID of the SIM card.
     * @return The subscription info if found. {@code null} if not found.
     */
    @Nullable
    public SubscriptionInfoInternal getSubscriptionInfoInternalByIccId(@NonNull String iccId) {
        mReadWriteLock.readLock().lock();
        try {
            return mAllSubscriptionInfoInternalCache.values().stream()
                    .filter(subInfo -> subInfo.getIccId().equals(iccId))
                    .findFirst()
                    .orElse(null);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Log debug messages.
     *
     * @param s debug messages
     */
    private void log(@NonNull String s) {
        Rlog.d(LOG_TAG, s);
    }

    /**
     * Log error messages.
     *
     * @param s error messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(LOG_TAG, s);
    }

    /**
     * Log verbose messages.
     *
     * @param s debug messages.
     */
    private void logv(@NonNull String s) {
        if (VDBG) Rlog.v(LOG_TAG, s);
    }

    /**
     * Log error messages and also log into the local log.
     *
     * @param s debug messages
     */
    private void logel(@NonNull String s) {
        loge(s);
        mLocalLog.log(s);
    }

    /**
     * Log debug messages and also log into the local log.
     *
     * @param s debug messages
     */
    private void logl(@NonNull String s) {
        log(s);
        mLocalLog.log(s);
    }

    /**
     * Dump the state of {@link SubscriptionDatabaseManager}.
     *
     * @param fd File descriptor
     * @param printWriter Print writer
     * @param args Arguments
     */
    public void dump(@NonNull FileDescriptor fd, @NonNull PrintWriter printWriter,
            @NonNull String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(SubscriptionDatabaseManager.class.getSimpleName() + ":");
        pw.increaseIndent();
        pw.println("All subscriptions:");
        pw.increaseIndent();
        mReadWriteLock.readLock().lock();
        try {
            mAllSubscriptionInfoInternalCache.forEach((subId, subInfo) -> pw.println(subInfo));
        } finally {
            mReadWriteLock.readLock().unlock();
        }
        pw.decreaseIndent();
        pw.println();
        pw.println("mAsyncMode=" + mAsyncMode);
        synchronized (this) {
            pw.println("mDatabaseInitialized=" + mDatabaseInitialized);
        }
        pw.println("mReadWriteLock=" + mReadWriteLock);
        pw.println();
        pw.println("Local log:");
        pw.increaseIndent();
        mLocalLog.dump(fd, printWriter, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
    }
}

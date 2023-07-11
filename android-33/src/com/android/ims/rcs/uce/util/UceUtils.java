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

package com.android.ims.rcs.uce.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.provider.BlockedNumberContract;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsRcsManager;
import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.text.TextUtils;
import android.util.Log;

import com.android.ims.rcs.uce.UceDeviceState.DeviceStateResult;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class UceUtils {

    public static final int LOG_SIZE = 20;
    private static final String LOG_PREFIX = "RcsUce.";
    private static final String LOG_TAG = LOG_PREFIX + "UceUtils";

    private static final String SHARED_PREF_DEVICE_STATE_KEY = "UceDeviceState";

    private static final int DEFAULT_RCL_MAX_NUM_ENTRIES = 100;
    private static final long DEFAULT_RCS_PUBLISH_SOURCE_THROTTLE_MS = 60000L;
    private static final long DEFAULT_NON_RCS_CAPABILITIES_CACHE_EXPIRATION_SEC =
            TimeUnit.DAYS.toSeconds(30);
    private static final long DEFAULT_REQUEST_RETRY_INTERVAL_MS = TimeUnit.MINUTES.toMillis(20);
    private static final long DEFAULT_MINIMUM_REQUEST_RETRY_AFTER_MS = TimeUnit.SECONDS.toMillis(3);

    // The default of the capabilities request timeout.
    private static final long DEFAULT_CAP_REQUEST_TIMEOUT_AFTER_MS = TimeUnit.MINUTES.toMillis(3);
    private static Optional<Long> OVERRIDE_CAP_REQUEST_TIMEOUT_AFTER_MS = Optional.empty();

    // The default value of the availability cache expiration.
    private static final long DEFAULT_AVAILABILITY_CACHE_EXPIRATION_SEC = 60L;   // 60 seconds

    // The task ID of the UCE request
    private static long TASK_ID = 0L;

    // The request coordinator ID
    private static long REQUEST_COORDINATOR_ID = 0;

    /**
     * Get the log prefix of RCS UCE
     */
    public static String getLogPrefix() {
        return LOG_PREFIX;
    }

    /**
     * Generate the unique UCE request task id.
     */
    public static synchronized long generateTaskId() {
        return ++TASK_ID;
    }

    /**
     * Generate the unique request coordinator id.
     */
    public static synchronized long generateRequestCoordinatorId() {
        return ++REQUEST_COORDINATOR_ID;
    }

    public static boolean isEabProvisioned(Context context, int subId) {
        boolean isProvisioned = false;
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.w(LOG_TAG, "isEabProvisioned: invalid subscriptionId " + subId);
            return false;
        }
        CarrierConfigManager configManager = (CarrierConfigManager)
                context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            PersistableBundle config = configManager.getConfigForSubId(subId);
            if (config != null && !config.getBoolean(
                    CarrierConfigManager.KEY_CARRIER_VOLTE_PROVISIONED_BOOL)) {
                return true;
            }
        }
        try {
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            isProvisioned = manager.getRcsProvisioningStatusForCapability(
                    ImsRcsManager.CAPABILITY_TYPE_PRESENCE_UCE,
                    ImsRegistrationImplBase.REGISTRATION_TECH_LTE);
        } catch (Exception e) {
            Log.w(LOG_TAG, "isEabProvisioned: exception=" + e.getMessage());
        }
        return isProvisioned;
    }

    /**
     * Check whether or not this carrier supports the exchange of phone numbers with the carrier's
     * presence server.
     */
    public static boolean isPresenceCapExchangeEnabled(Context context, int subId) {
        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        if (configManager == null) {
            return false;
        }
        PersistableBundle config = configManager.getConfigForSubId(subId);
        if (config == null) {
            return false;
        }
        return config.getBoolean(
                CarrierConfigManager.Ims.KEY_ENABLE_PRESENCE_CAPABILITY_EXCHANGE_BOOL);
    }

    /**
     * Check if Presence is supported by the carrier.
     */
    public static boolean isPresenceSupported(Context context, int subId) {
        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        if (configManager == null) {
            return false;
        }
        PersistableBundle config = configManager.getConfigForSubId(subId);
        if (config == null) {
            return false;
        }
        return config.getBoolean(CarrierConfigManager.Ims.KEY_ENABLE_PRESENCE_PUBLISH_BOOL);
    }

    /**
     * Check if SIP OPTIONS is supported by the carrier.
     */
    public static boolean isSipOptionsSupported(Context context, int subId) {
        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        if (configManager == null) {
            return false;
        }
        PersistableBundle config = configManager.getConfigForSubId(subId);
        if (config == null) {
            return false;
        }
        return config.getBoolean(CarrierConfigManager.KEY_USE_RCS_SIP_OPTIONS_BOOL);
    }

    /**
     * Check whether the PRESENCE group subscribe is enabled or not.
     *
     * @return true when the Presence group subscribe is enabled, false otherwise.
     */
    public static boolean isPresenceGroupSubscribeEnabled(Context context, int subId) {
        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        if (configManager == null) {
            return false;
        }
        PersistableBundle config = configManager.getConfigForSubId(subId);
        if (config == null) {
            return false;
        }
        return config.getBoolean(CarrierConfigManager.Ims.KEY_ENABLE_PRESENCE_GROUP_SUBSCRIBE_BOOL);
    }

    /**
     *  Returns {@code true} if {@code phoneNumber} is blocked.
     *
     * @param context the context of the caller.
     * @param phoneNumber the number to check.
     * @return true if the number is blocked, false otherwise.
     */
    public static boolean isNumberBlocked(Context context, String phoneNumber) {
        int blockStatus;
        try {
            blockStatus = BlockedNumberContract.SystemContract.shouldSystemBlockNumber(
                    context, phoneNumber, null /*extras*/);
        } catch (Exception e) {
            return false;
        }
        return blockStatus != BlockedNumberContract.STATUS_NOT_BLOCKED;
    }

    /**
     * Check whether sip uri should be used for presence subscribe
     */
    public static boolean isSipUriForPresenceSubscribeEnabled(Context context, int subId) {
        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        if (configManager == null) {
            return false;
        }
        PersistableBundle config = configManager.getConfigForSubId(subId);
        if (config == null) {
            return false;
        }
        return config.getBoolean(
                CarrierConfigManager.Ims.KEY_USE_SIP_URI_FOR_PRESENCE_SUBSCRIBE_BOOL);
    }

    /**
     * Check whether tel uri should be used for pidf xml
     */
    public static boolean isTelUriForPidfXmlEnabled(Context context, int subId) {
        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        if (configManager == null) {
            return false;
        }
        PersistableBundle config = configManager.getConfigForSubId(subId);
        if (config == null) {
            return false;
        }
        return config.getBoolean(
                CarrierConfigManager.Ims.KEY_USE_TEL_URI_FOR_PIDF_XML_BOOL);
    }

    /**
     * Get the minimum time that allow two PUBLISH requests can be executed continuously.
     *
     * @param subId The subscribe ID
     * @return The milliseconds that allowed two consecutive publish request.
     */
    public static long getRcsPublishThrottle(int subId) {
        long throttle = DEFAULT_RCS_PUBLISH_SOURCE_THROTTLE_MS;
        try {
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            long provisioningValue = manager.getProvisioningIntValue(
                    ProvisioningManager.KEY_RCS_PUBLISH_SOURCE_THROTTLE_MS);
            if (provisioningValue > 0) {
                throttle = provisioningValue;
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "getRcsPublishThrottle: exception=" + e.getMessage());
        }
        return throttle;
    }

    /**
     * Retrieve the maximum number of contacts that is in one Request Contained List(RCL)
     *
     * @param subId The subscribe ID
     * @return The maximum number of contacts.
     */
    public static int getRclMaxNumberEntries(int subId) {
        int maxNumEntries = DEFAULT_RCL_MAX_NUM_ENTRIES;
        try {
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            int provisioningValue = manager.getProvisioningIntValue(
                    ProvisioningManager.KEY_RCS_MAX_NUM_ENTRIES_IN_RCL);
            if (provisioningValue > 0) {
                maxNumEntries = provisioningValue;
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "getRclMaxNumberEntries: exception=" + e.getMessage());
        }
        return maxNumEntries;
    }

    public static long getNonRcsCapabilitiesCacheExpiration(Context context, int subId) {
        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        if (configManager == null) {
            return DEFAULT_NON_RCS_CAPABILITIES_CACHE_EXPIRATION_SEC;
        }
        PersistableBundle config = configManager.getConfigForSubId(subId);
        if (config == null) {
            return DEFAULT_NON_RCS_CAPABILITIES_CACHE_EXPIRATION_SEC;
        }
        return config.getInt(
                CarrierConfigManager.Ims.KEY_NON_RCS_CAPABILITIES_CACHE_EXPIRATION_SEC_INT);
    }

    public static boolean isRequestForbiddenBySip489(Context context, int subId) {
        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        if (configManager == null) {
            return false;
        }
        PersistableBundle config = configManager.getConfigForSubId(subId);
        if (config == null) {
            return false;
        }
        return config.getBoolean(
                CarrierConfigManager.Ims.KEY_RCS_REQUEST_FORBIDDEN_BY_SIP_489_BOOL);
    }

    public static long getRequestRetryInterval(Context context, int subId) {
        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        if (configManager == null) {
            return DEFAULT_REQUEST_RETRY_INTERVAL_MS;
        }
        PersistableBundle config = configManager.getConfigForSubId(subId);
        if (config == null) {
            return DEFAULT_REQUEST_RETRY_INTERVAL_MS;
        }
        return config.getLong(
                CarrierConfigManager.Ims.KEY_RCS_REQUEST_RETRY_INTERVAL_MILLIS_LONG);
    }

    public static boolean saveDeviceStateToPreference(Context context, int subId,
            DeviceStateResult deviceState) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getDeviceStateSharedPrefKey(subId),
                getDeviceStateSharedPrefValue(deviceState));
        return editor.commit();
    }

    public static Optional<DeviceStateResult> restoreDeviceState(Context context, int subId) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        final String sharedPrefKey = getDeviceStateSharedPrefKey(subId);
        String sharedPrefValue = sharedPreferences.getString(sharedPrefKey, "");
        if (TextUtils.isEmpty(sharedPrefValue)) {
            return Optional.empty();
        }
        String[] valueAry = sharedPrefValue.split(",");
        if (valueAry == null || valueAry.length != 4) {
            return Optional.empty();
        }
        try {
            int deviceState = Integer.valueOf(valueAry[0]);
            Optional<Integer> errorCode = (Integer.valueOf(valueAry[1]) == -1L) ?
                    Optional.empty() : Optional.of(Integer.valueOf(valueAry[1]));

            long retryTimeMillis = Long.valueOf(valueAry[2]);
            Optional<Instant> retryTime = (retryTimeMillis == -1L) ?
                    Optional.empty() : Optional.of(Instant.ofEpochMilli(retryTimeMillis));

            long exitStateTimeMillis = Long.valueOf(valueAry[3]);
            Optional<Instant> exitStateTime = (exitStateTimeMillis == -1L) ?
                    Optional.empty() : Optional.of(Instant.ofEpochMilli(exitStateTimeMillis));

            return Optional.of(new DeviceStateResult(deviceState, errorCode, retryTime,
                    exitStateTime));
        } catch (Exception e) {
            Log.d(LOG_TAG, "restoreDeviceState: exception " + e);
            return Optional.empty();
        }
    }

    public static boolean removeDeviceStateFromPreference(Context context, int subId) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(getDeviceStateSharedPrefKey(subId));
        return editor.commit();
    }

    private static String getDeviceStateSharedPrefKey(int subId) {
        return SHARED_PREF_DEVICE_STATE_KEY + subId;
    }

    /**
     * Build the device state preference value.
     */
    private static String getDeviceStateSharedPrefValue(DeviceStateResult deviceState) {
        StringBuilder builder = new StringBuilder();
        builder.append(deviceState.getDeviceState())  // device state
                .append(",").append(deviceState.getErrorCode().orElse(-1));  // error code

        long retryTimeMillis = -1L;
        Optional<Instant> retryTime = deviceState.getRequestRetryTime();
        if (retryTime.isPresent()) {
            retryTimeMillis = retryTime.get().toEpochMilli();
        }
        builder.append(",").append(retryTimeMillis);   // retryTime

        long exitStateTimeMillis = -1L;
        Optional<Instant> exitStateTime = deviceState.getExitStateTime();
        if (exitStateTime.isPresent()) {
            exitStateTimeMillis = exitStateTime.get().toEpochMilli();
        }
        builder.append(",").append(exitStateTimeMillis);   // exit state time
        return builder.toString();
    }

    /**
     * Get the minimum value of the capabilities request retry after.
     */
    public static long getMinimumRequestRetryAfterMillis() {
        return DEFAULT_MINIMUM_REQUEST_RETRY_AFTER_MS;
    }

    /**
     * Override the capability request timeout to the millisecond value specified. Sending a
     * value <= 0 will reset the capabilities.
     */
    public static synchronized void setCapRequestTimeoutAfterMillis(long timeoutAfterMs) {
        if (timeoutAfterMs <= 0L) {
            OVERRIDE_CAP_REQUEST_TIMEOUT_AFTER_MS = Optional.empty();
        } else {
            OVERRIDE_CAP_REQUEST_TIMEOUT_AFTER_MS = Optional.of(timeoutAfterMs);
        }
    }

    /**
     * Get the milliseconds of the capabilities request timed out.
     * @return the time in milliseconds before a pending capabilities request will time out.
     */
    public static synchronized long getCapRequestTimeoutAfterMillis() {
        if(OVERRIDE_CAP_REQUEST_TIMEOUT_AFTER_MS.isPresent()) {
            return OVERRIDE_CAP_REQUEST_TIMEOUT_AFTER_MS.get();
        } else {
            return DEFAULT_CAP_REQUEST_TIMEOUT_AFTER_MS;
        }
    }

    /**
     * Get the contact number from the given URI.
     * @param contactUri The contact uri of the capabilities to request for.
     * @return The number of the contact uri. NULL if the number cannot be retrieved.
     */
    public static String getContactNumber(Uri contactUri) {
        if (contactUri == null) {
            return null;
        }
        String number = contactUri.getSchemeSpecificPart();
        if (TextUtils.isEmpty(number)) {
            return null;
        }

        String numberParts[] = number.split("[@;:]");
        if (numberParts.length == 0) {
            Log.d(LOG_TAG, "getContactNumber: the length of numberPars is 0");
            return contactUri.toString();
        }
        return numberParts[0];
    }

    /**
     * Get the availability expiration from provisioning manager.
     * @param subId The subscription ID
     * @return the number of seconds for the availability cache expiration.
     */
    public static long getAvailabilityCacheExpiration(int subId) {
        long value = -1;
        try {
            ProvisioningManager pm = ProvisioningManager.createForSubscriptionId(subId);
            value = pm.getProvisioningIntValue(
                    ProvisioningManager.KEY_RCS_AVAILABILITY_CACHE_EXPIRATION_SEC);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Exception in getAvailabilityCacheExpiration: " + e);
        }

        if (value <= 0) {
            Log.w(LOG_TAG, "The availability expiration cannot be less than 0.");
            value = DEFAULT_AVAILABILITY_CACHE_EXPIRATION_SEC;
        }
        return value;
    }
}

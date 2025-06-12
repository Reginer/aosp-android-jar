/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.internal.telephony;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.annotation.NonNull;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;


import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.flags.Flags;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

/**
 * This class contains logic to get Certificates and keep them current.
 * The class will be instantiated by various Phone implementations.
 */
public class CarrierKeyDownloadManager extends Handler {
    private static final String LOG_TAG = "CarrierKeyDownloadManager";

    private static final String CERT_BEGIN_STRING = "-----BEGIN CERTIFICATE-----";

    private static final String CERT_END_STRING = "-----END CERTIFICATE-----";

    private static final int DAY_IN_MILLIS = 24 * 3600 * 1000;

    // Create a window prior to the key expiration, during which the cert will be
    // downloaded. Defines the start date of that window. So if the key expires on
    // Dec  21st, the start of the renewal window will be Dec 1st.
    private static final int START_RENEWAL_WINDOW_DAYS = 21;

    // This will define the end date of the window.
    private static final int END_RENEWAL_WINDOW_DAYS = 7;

    /* Intent for downloading the public key */
    private static final String INTENT_KEY_RENEWAL_ALARM_PREFIX =
            "com.android.internal.telephony.carrier_key_download_alarm";

    @VisibleForTesting
    public int mKeyAvailability = 0;

    private static final String JSON_CERTIFICATE = "certificate";
    private static final String JSON_CERTIFICATE_ALTERNATE = "public-key";
    private static final String JSON_TYPE = "key-type";
    private static final String JSON_IDENTIFIER = "key-identifier";
    private static final String JSON_CARRIER_KEYS = "carrier-keys";
    private static final String JSON_TYPE_VALUE_WLAN = "WLAN";
    private static final String JSON_TYPE_VALUE_EPDG = "EPDG";

    private static final int EVENT_ALARM_OR_CONFIG_CHANGE = 0;
    private static final int EVENT_DOWNLOAD_COMPLETE = 1;
    private static final int EVENT_NETWORK_AVAILABLE = 2;
    private static final int EVENT_SCREEN_UNLOCKED = 3;


    private static final int[] CARRIER_KEY_TYPES = {TelephonyManager.KEY_TYPE_EPDG,
            TelephonyManager.KEY_TYPE_WLAN};

    private final Phone mPhone;
    private final Context mContext;
    public final DownloadManager mDownloadManager;
    private String mURL;
    private boolean mAllowedOverMeteredNetwork = false;
    private boolean mDeleteOldKeyAfterDownload = false;
    private boolean mIsRequiredToHandleUnlock;
    private TelephonyManager mTelephonyManager;
    private UserManager mUserManager;
    @VisibleForTesting
    public String mMccMncForDownload = "";
    public int mCarrierId = TelephonyManager.UNKNOWN_CARRIER_ID;
    @VisibleForTesting
    public long mDownloadId;
    private DefaultNetworkCallback mDefaultNetworkCallback;
    private ConnectivityManager mConnectivityManager;
    private KeyguardManager mKeyguardManager;

    public CarrierKeyDownloadManager(Phone phone) {
        mPhone = phone;
        mContext = phone.getContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_KEY_RENEWAL_ALARM_PREFIX);
        filter.addAction(TelephonyIntents.ACTION_CARRIER_CERTIFICATE_DOWNLOAD);
        if (Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
            filter.addAction(Intent.ACTION_USER_UNLOCKED);
        }
        mContext.registerReceiver(mBroadcastReceiver, filter, null, phone);
        mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mPhone.getSubId());
        if (Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
            mKeyguardManager = mContext.getSystemService(KeyguardManager.class);
        }
        mUserManager = mContext.getSystemService(UserManager.class);

        CarrierConfigManager carrierConfigManager = mContext.getSystemService(
                CarrierConfigManager.class);
        // Callback which directly handle config change should be executed on handler thread
        if (carrierConfigManager != null) {
            carrierConfigManager.registerCarrierConfigChangeListener(this::post,
                (slotIndex, subId, carrierId, specificCarrierId) -> {
                    if (Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
                        logd("CarrierConfig changed slotIndex = " + slotIndex + " subId = " + subId
                                + " CarrierId = " + carrierId + " phoneId = "
                                + mPhone.getPhoneId());
                        // Below checks are necessary to optimise the logic.
                        if ((slotIndex == mPhone.getPhoneId()) && (carrierId > 0
                                || !TextUtils.isEmpty(
                                mMccMncForDownload))) {
                            if (mTelephonyManager == null
                                    || mTelephonyManager.getSubscriptionId() != subId) {
                                logd("recreating TelManager with SubId = " + subId);
                                mTelephonyManager = mContext.getSystemService(
                                                TelephonyManager.class)
                                        .createForSubscriptionId(subId);
                            }
                            if (Flags.ignoreCarrieridResetForSimRemoval()) {
                                if (carrierId > 0) {
                                    mCarrierId = carrierId;
                                }
                            } else {
                                mCarrierId = carrierId;
                            }
                            updateSimOperator();
                            // If device is screen locked do not proceed to handle
                            // EVENT_ALARM_OR_CONFIG_CHANGE
                            printDeviceLockStatus();
                            if (Flags.ignoreCarrieridResetForSimRemoval()) {
                                if (!mUserManager.isUserUnlocked()) {
                                    mIsRequiredToHandleUnlock = true;
                                    return;
                                }
                            } else if (mKeyguardManager.isDeviceLocked()) {
                                mIsRequiredToHandleUnlock = true;
                                return;
                            }
                            logd("Carrier Config changed: slotIndex=" + slotIndex);
                            sendEmptyMessage(EVENT_ALARM_OR_CONFIG_CHANGE);

                        }
                    } else {
                        boolean isUserUnlocked = mUserManager.isUserUnlocked();

                        if (isUserUnlocked && slotIndex == mPhone.getPhoneId()) {
                            Log.d(LOG_TAG, "Carrier Config changed: slotIndex=" + slotIndex);
                            handleAlarmOrConfigChange();
                        } else {
                            Log.d(LOG_TAG, "User is locked");
                            mContext.registerReceiver(mUserUnlockedReceiver, new IntentFilter(
                                    Intent.ACTION_USER_UNLOCKED));
                        }
                    }
                });
        }
        mConnectivityManager = mContext.getSystemService(ConnectivityManager.class);
    }

    private void printDeviceLockStatus() {
        logd(" Device Status: isDeviceLocked = " + mKeyguardManager.isDeviceLocked()
                + "  iss User unlocked = " + mUserManager.isUserUnlocked());
    }

    // TODO remove this method upon imsiKeyRetryDownloadOnPhoneUnlock enabled.
    private final BroadcastReceiver mUserUnlockedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_UNLOCKED.equals(intent.getAction())) {
                Log.d(LOG_TAG, "Received UserUnlockedReceiver");
                handleAlarmOrConfigChange();
            }
        }
    };

    private final BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                logd("Download Complete");
                sendMessage(obtainMessage(EVENT_DOWNLOAD_COMPLETE,
                        intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)));
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int slotIndex = SubscriptionManager.getSlotIndex(mPhone.getSubId());
            int phoneId = mPhone.getPhoneId();
            switch (action) {
                case INTENT_KEY_RENEWAL_ALARM_PREFIX -> {
                    int slotIndexExtra = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                            -1);
                    if (slotIndexExtra == slotIndex) {
                        logd("Handling key renewal alarm: " + action);
                        if (Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
                            updateSimOperator();
                        }
                        sendEmptyMessage(EVENT_ALARM_OR_CONFIG_CHANGE);
                    }
                }
                case TelephonyIntents.ACTION_CARRIER_CERTIFICATE_DOWNLOAD -> {
                    if (phoneId == intent.getIntExtra(PhoneConstants.PHONE_KEY,
                            SubscriptionManager.INVALID_SIM_SLOT_INDEX)) {
                        logd("Handling reset intent: " + action);
                        sendEmptyMessage(EVENT_ALARM_OR_CONFIG_CHANGE);
                    }
                }
                case Intent.ACTION_USER_UNLOCKED -> {
                    // The Carrier key download fails when SIM is inserted while device is locked
                    // hence adding a retry logic when device is unlocked.
                    logd("device fully unlocked, isRequiredToHandleUnlock = "
                            + mIsRequiredToHandleUnlock
                            + ", slotIndex = " + slotIndex + "  hasActiveDataNetwork = " + (
                            mConnectivityManager.getActiveNetwork() != null));
                    if (mIsRequiredToHandleUnlock) {
                        mIsRequiredToHandleUnlock = false;
                        sendEmptyMessage(EVENT_SCREEN_UNLOCKED);
                    }
                }
            }
        }
    };

    @Override
    public void handleMessage (Message msg) {
        switch (msg.what) {
            case EVENT_ALARM_OR_CONFIG_CHANGE, EVENT_NETWORK_AVAILABLE, EVENT_SCREEN_UNLOCKED ->
                    handleAlarmOrConfigChange();
            case EVENT_DOWNLOAD_COMPLETE -> {
                long carrierKeyDownloadIdentifier = (long) msg.obj;
                String currentMccMnc = Flags.imsiKeyRetryDownloadOnPhoneUnlock()
                        ? mTelephonyManager.getSimOperator(mPhone.getSubId()) : getSimOperator();
                int carrierId = Flags.imsiKeyRetryDownloadOnPhoneUnlock()
                        ? mTelephonyManager.getSimCarrierId() : getSimCarrierId();
                if (isValidDownload(currentMccMnc, carrierKeyDownloadIdentifier, carrierId)) {
                    onDownloadComplete(carrierKeyDownloadIdentifier, currentMccMnc, carrierId);
                    onPostDownloadProcessing();
                }
            }
        }
    }

    private void onPostDownloadProcessing() {
        resetRenewalAlarm();
        if(Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
            mDownloadId = -1;
        } else {
            cleanupDownloadInfo();
        }
        // unregister from DOWNLOAD_COMPLETE
        mContext.unregisterReceiver(mDownloadReceiver);
    }

    private void handleAlarmOrConfigChange() {
        if (Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
            if (carrierUsesKeys()) {
                if (areCarrierKeysAbsentOrExpiring()) {
                    boolean hasActiveDataNetwork =
                            (mConnectivityManager.getActiveNetwork() != null);
                    boolean downloadStartedSuccessfully = hasActiveDataNetwork && downloadKey();
                    // if the download was attempted, but not started successfully, and if
                    // carriers uses keys, we'll still want to renew the alarms, and try
                    // downloading the key a day later.
                    int slotIndex = SubscriptionManager.getSlotIndex(mPhone.getSubId());
                    if (downloadStartedSuccessfully) {
                        unregisterDefaultNetworkCb(slotIndex);
                    } else {
                        // If download fails due to the device user lock, we will reattempt once
                        // the device is unlocked.
                        if (Flags.ignoreCarrieridResetForSimRemoval()) {
                            mIsRequiredToHandleUnlock = !mUserManager.isUserUnlocked();
                        } else {
                            mIsRequiredToHandleUnlock = mKeyguardManager.isDeviceLocked();
                        }

                        loge("hasActiveDataConnection = " + hasActiveDataNetwork
                                + "    isDeviceUserLocked = " + mIsRequiredToHandleUnlock);
                        if (!hasActiveDataNetwork) {
                            registerDefaultNetworkCb(slotIndex);
                        }
                        resetRenewalAlarm();
                    }
                }
                logd("handleAlarmOrConfigChange :: areCarrierKeysAbsentOrExpiring returned false");
            } else {
                cleanupRenewalAlarms();
                if (!isOtherSlotHasCarrier()) {
                    // delete any existing alarms.
                    mPhone.deleteCarrierInfoForImsiEncryption(getSimCarrierId(), getSimOperator());
                }
                cleanupDownloadInfo();
            }
        } else {
            if (carrierUsesKeys()) {
                if (areCarrierKeysAbsentOrExpiring()) {
                    boolean downloadStartedSuccessfully = downloadKey();
                    // if the download was attempted, but not started successfully, and if
                    // carriers uses keys, we'll still want to renew the alarms, and try
                    // downloading the key a day later.
                    if (!downloadStartedSuccessfully) {
                        resetRenewalAlarm();
                    }
                }
            } else {
                // delete any existing alarms.
                cleanupRenewalAlarms();
                mPhone.deleteCarrierInfoForImsiEncryption(getSimCarrierId());
            }
        }
    }

    private boolean isOtherSlotHasCarrier() {
        SubscriptionManager subscriptionManager = mPhone.getContext().getSystemService(
                SubscriptionManager.class);
        List<SubscriptionInfo> subscriptionInfoList =
                subscriptionManager.getActiveSubscriptionInfoList();
        loge("handleAlarmOrConfigChange ActiveSubscriptionInfoList = " + (
                (subscriptionInfoList != null) ? subscriptionInfoList.size() : null));
        for (SubscriptionInfo subInfo : subscriptionInfoList) {
            if (mPhone.getSubId() != subInfo.getSubscriptionId()
                    && subInfo.getCarrierId() == mPhone.getCarrierId()) {
                // We do not proceed to remove the Key from the DB as another slot contains
                // same operator sim which is in active state.
                loge("handleAlarmOrConfigChange same operator sim in another slot");
                return true;
            }
        }
        return false;
    }

    private void cleanupDownloadInfo() {
        logd("Cleaning up download info");
        mDownloadId = -1;
        if (Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
            mMccMncForDownload = "";
        } else {
            mMccMncForDownload = null;
        }
        mCarrierId = TelephonyManager.UNKNOWN_CARRIER_ID;
    }

    private void cleanupRenewalAlarms() {
        logd("Cleaning up existing renewal alarms");
        int slotIndex = SubscriptionManager.getSlotIndex(mPhone.getSubId());
        Intent intent = new Intent(INTENT_KEY_RENEWAL_ALARM_PREFIX);
        intent.putExtra(SubscriptionManager.EXTRA_SLOT_INDEX, slotIndex);
        PendingIntent carrierKeyDownloadIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager =mContext.getSystemService(AlarmManager.class);
        alarmManager.cancel(carrierKeyDownloadIntent);
    }

    /**
     * this method returns the date to be used to decide on when to start downloading the key.
     * from the carrier.
     **/
    @VisibleForTesting
    public long getExpirationDate()  {
        long minExpirationDate = Long.MAX_VALUE;
        for (int key_type : CARRIER_KEY_TYPES) {
            if (!isKeyEnabled(key_type)) {
                continue;
            }
            ImsiEncryptionInfo imsiEncryptionInfo =
                    mPhone.getCarrierInfoForImsiEncryption(key_type, false);
            if (imsiEncryptionInfo != null && imsiEncryptionInfo.getExpirationTime() != null) {
                if (minExpirationDate > imsiEncryptionInfo.getExpirationTime().getTime()) {
                    minExpirationDate = imsiEncryptionInfo.getExpirationTime().getTime();
                }
            }
        }

        // if there are no keys, or expiration date is in the past, or within 7 days, then we
        // set the alarm to run in a day. Else, we'll set the alarm to run 7 days prior to
        // expiration.
        if (minExpirationDate == Long.MAX_VALUE || (minExpirationDate
                < System.currentTimeMillis() + END_RENEWAL_WINDOW_DAYS * DAY_IN_MILLIS)) {
            minExpirationDate = System.currentTimeMillis() + DAY_IN_MILLIS;
        } else {
            // We don't want all the phones to download the certs simultaneously, so
            // we pick a random time during the download window to avoid this situation.
            Random random = new Random();
            int max = START_RENEWAL_WINDOW_DAYS * DAY_IN_MILLIS;
            int min = END_RENEWAL_WINDOW_DAYS * DAY_IN_MILLIS;
            int randomTime = random.nextInt(max - min) + min;
            minExpirationDate = minExpirationDate - randomTime;
        }
        return minExpirationDate;
    }

    /**
     * this method resets the alarm. Starts by cleaning up the existing alarms.
     * We look at the earliest expiration date, and setup an alarms X days prior.
     * If the expiration date is in the past, we'll setup an alarm to run the next day. This
     * could happen if the download has failed.
     **/
    @VisibleForTesting
    public void resetRenewalAlarm() {
        cleanupRenewalAlarms();
        int slotIndex = SubscriptionManager.getSlotIndex(mPhone.getSubId());
        long minExpirationDate = getExpirationDate();
        logd("minExpirationDate: " + new Date(minExpirationDate));
        final AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(
                Context.ALARM_SERVICE);
        Intent intent = new Intent(INTENT_KEY_RENEWAL_ALARM_PREFIX);
        intent.putExtra(SubscriptionManager.EXTRA_SLOT_INDEX, slotIndex);
        PendingIntent carrierKeyDownloadIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, minExpirationDate, carrierKeyDownloadIntent);
        logd("setRenewalAlarm: action=" + intent.getAction() + " time="
                + new Date(minExpirationDate));
    }

    /**
     * Read the store the sim operetor value and update the value in case of change in the sim
     * operetor.
     */
    public void updateSimOperator() {
        if (Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
            String simOperator = mPhone.getOperatorNumeric();
            if (!TextUtils.isEmpty(simOperator) && !simOperator.equals(mMccMncForDownload)) {
                mMccMncForDownload = simOperator;
                logd("updateSimOperator, Initialized mMccMncForDownload = " + mMccMncForDownload);
            }
        }
    }

    /**
     * Returns the sim operator.
     **/
    @VisibleForTesting
    public String getSimOperator() {
        if (Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
            updateSimOperator();
            return mMccMncForDownload;
        } else {
            return mTelephonyManager.getSimOperator(mPhone.getSubId());
        }
    }

    /**
     * Returns the sim operator.
     **/
    @VisibleForTesting
    public int getSimCarrierId() {
        if (Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
            return (mCarrierId > 0) ? mCarrierId : mPhone.getCarrierId();
        } else {
            return mTelephonyManager.getSimCarrierId();
        }
    }

    /**
     *  checks if the download was sent by this particular instance. We do this by including the
     *  slot id in the key. If no value is found, we know that the download was not for this
     *  instance of the phone.
     **/
    @VisibleForTesting
    public boolean isValidDownload(String currentMccMnc, long currentDownloadId, int carrierId) {
        if (currentDownloadId != mDownloadId) {
            loge( "download ID=" + currentDownloadId
                    + " for completed download does not match stored id=" + mDownloadId);
            return false;
        }

        if (TextUtils.isEmpty(currentMccMnc) || TextUtils.isEmpty(mMccMncForDownload)
                || !TextUtils.equals(currentMccMnc, mMccMncForDownload)
                || mCarrierId != carrierId) {
            loge( "currentMccMnc=" + currentMccMnc + " storedMccMnc =" + mMccMncForDownload
                    + "currentCarrierId = " + carrierId + "  storedCarrierId = " + mCarrierId);
            return false;
        }

        logd("Matched MccMnc =  " + currentMccMnc + ", carrierId = " + carrierId
                + ", downloadId: " + currentDownloadId);
        return true;
    }

    /**
     * This method will try to parse the downloaded information, and persist it in the database.
     **/
    private void onDownloadComplete(long carrierKeyDownloadIdentifier, String mccMnc,
            int carrierId) {
        logd("onDownloadComplete: " + carrierKeyDownloadIdentifier);
        String jsonStr;
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(carrierKeyDownloadIdentifier);
        Cursor cursor = mDownloadManager.query(query);

        if (cursor == null) {
            return;
        }
        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                try {
                    jsonStr = convertToString(mDownloadManager, carrierKeyDownloadIdentifier);
                    if (TextUtils.isEmpty(jsonStr)) {
                        logd("fallback to no gzip");
                        jsonStr = convertToStringNoGZip(mDownloadManager,
                                carrierKeyDownloadIdentifier);
                    }
                    parseJsonAndPersistKey(jsonStr, mccMnc, carrierId);
                    logd("Completed downloading keys");
                } catch (Exception e) {
                    loge( "Error in download:" + carrierKeyDownloadIdentifier
                            + ". " + e);
                } finally {
                    mDownloadManager.remove(carrierKeyDownloadIdentifier);
                }
            } else {
                loge("Download Failed reason = " + cursor.getInt(columnIndex)
                        + "Failed Status reason" + cursor.getInt(
                        cursor.getColumnIndex(DownloadManager.COLUMN_REASON)));
                printDeviceLockStatus();
            }
        }
        cursor.close();
    }

    /**
     * This method checks if the carrier requires key. We'll read the carrier config to make that
     * determination.
     * @return boolean returns true if carrier requires keys, else false.
     **/
    private boolean carrierUsesKeys() {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager)
                mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (carrierConfigManager == null) {
            return false;
        }
        int subId = mPhone.getSubId();
        PersistableBundle b = null;
        try {
            b = carrierConfigManager.getConfigForSubId(subId,
                    CarrierConfigManager.IMSI_KEY_AVAILABILITY_INT,
                    CarrierConfigManager.IMSI_KEY_DOWNLOAD_URL_STRING,
                    CarrierConfigManager.KEY_ALLOW_METERED_NETWORK_FOR_CERT_DOWNLOAD_BOOL);
        } catch (RuntimeException e) {
            loge( "CarrierConfigLoader is not available.");
        }
        if (b == null || b.isEmpty()) {
            return false;
        }

        mKeyAvailability = b.getInt(CarrierConfigManager.IMSI_KEY_AVAILABILITY_INT);
        mURL = b.getString(CarrierConfigManager.IMSI_KEY_DOWNLOAD_URL_STRING);
        mAllowedOverMeteredNetwork = b.getBoolean(
                CarrierConfigManager.KEY_ALLOW_METERED_NETWORK_FOR_CERT_DOWNLOAD_BOOL);

        if (mKeyAvailability == 0 || TextUtils.isEmpty(mURL)) {
            logd("Carrier not enabled or invalid values. mKeyAvailability=" + mKeyAvailability
                    + " mURL=" + mURL);
            return false;
        }
        for (int key_type : CARRIER_KEY_TYPES) {
            if (isKeyEnabled(key_type)) {
                return true;
            }
        }
        return false;
    }

    private static String convertToStringNoGZip(DownloadManager downloadManager, long downloadId) {
        StringBuilder sb = new StringBuilder();
        try (InputStream source = new FileInputStream(
                downloadManager.openDownloadedFile(downloadId).getFileDescriptor())) {
            // If the carrier does not have the data gzipped, fallback to assuming it is not zipped.
            // parseJsonAndPersistKey may still fail if the data is malformed, so we won't be
            // persisting random bogus strings thinking it's the cert
            BufferedReader reader = new BufferedReader(new InputStreamReader(source, UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }

    private static String convertToString(DownloadManager downloadManager, long downloadId) {
        try (InputStream source = new FileInputStream(
                downloadManager.openDownloadedFile(downloadId).getFileDescriptor());
             InputStream gzipIs = new GZIPInputStream(source)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzipIs, UTF_8));
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (ZipException e) {
            // GZIPInputStream constructor will throw exception if stream is not GZIP
            Log.d(LOG_TAG, "Stream is not gzipped e=" + e);
            return null;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unexpected exception in convertToString e=" + e);
            return null;
        }
    }

    /**
     * Converts the string into a json object to retreive the nodes. The Json should have 3 nodes,
     * including the Carrier public key, the key type and the key identifier. Once the nodes have
     * been extracted, they get persisted to the database. Sample:
     *      "carrier-keys": [ { "certificate": "",
     *                         "key-type": "WLAN",
     *                         "key-identifier": ""
     *                        } ]
     * @param jsonStr the json string.
     * @param mccMnc contains the mcc, mnc.
     */
    @VisibleForTesting
    public void parseJsonAndPersistKey(String jsonStr, String mccMnc, int carrierId) {
        if (TextUtils.isEmpty(jsonStr) || TextUtils.isEmpty(mccMnc)
                || carrierId == TelephonyManager.UNKNOWN_CARRIER_ID) {
            loge( "jsonStr or mcc, mnc: is empty or carrierId is UNKNOWN_CARRIER_ID");
            return;
        }
        try {
            String mcc = mccMnc.substring(0, 3);
            String mnc = mccMnc.substring(3);
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONArray keys = jsonObj.getJSONArray(JSON_CARRIER_KEYS);
            for (int i = 0; i < keys.length(); i++) {
                JSONObject key = keys.getJSONObject(i);
                // Support both "public-key" and "certificate" String property.
                String cert = null;
                if (key.has(JSON_CERTIFICATE)) {
                    cert = key.getString(JSON_CERTIFICATE);
                } else {
                    cert = key.getString(JSON_CERTIFICATE_ALTERNATE);
                }
                // The key-type property is optional, therefore, the default value is WLAN type if
                // not specified.
                int type = TelephonyManager.KEY_TYPE_WLAN;
                if (key.has(JSON_TYPE)) {
                    String typeString = key.getString(JSON_TYPE);
                    if (typeString.equals(JSON_TYPE_VALUE_EPDG)) {
                        type = TelephonyManager.KEY_TYPE_EPDG;
                    } else if (!typeString.equals(JSON_TYPE_VALUE_WLAN)) {
                        loge( "Invalid key-type specified: " + typeString);
                    }
                }
                String identifier = key.getString(JSON_IDENTIFIER);
                Pair<PublicKey, Long> keyInfo =
                        getKeyInformation(cleanCertString(cert).getBytes());
                if (mDeleteOldKeyAfterDownload) {
                    if (Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
                        mPhone.deleteCarrierInfoForImsiEncryption(
                                TelephonyManager.UNKNOWN_CARRIER_ID, null);
                    } else {
                        mPhone.deleteCarrierInfoForImsiEncryption(
                                TelephonyManager.UNKNOWN_CARRIER_ID);
                    }
                    mDeleteOldKeyAfterDownload = false;
                }
                savePublicKey(keyInfo.first, type, identifier, keyInfo.second, mcc, mnc, carrierId);
            }
        } catch (final JSONException e) {
            loge( "Json parsing error: " + e.getMessage());
        } catch (final Exception e) {
            loge( "Exception getting certificate: " + e);
        }
    }

    /**
     * introspects the mKeyAvailability bitmask
     * @return true if the digit at position k is 1, else false.
     */
    @VisibleForTesting
    public boolean isKeyEnabled(int keyType) {
        // since keytype has values of 1, 2.... we need to subtract 1 from the keytype.
        return isKeyEnabled(keyType, mKeyAvailability);
    }

    /**
     * introspects the mKeyAvailability bitmask
     * @return true if the digit at position k is 1, else false.
     */
    public static boolean isKeyEnabled(int keyType, int keyAvailability) {
        // since keytype has values of 1, 2.... we need to subtract 1 from the keytype.
        int returnValue = (keyAvailability >> (keyType - 1)) & 1;
        return returnValue == 1;
    }

    /**
     * Checks whether is the keys are absent or close to expiration. Returns true, if either of
     * those conditions are true.
     * @return boolean returns true when keys are absent or close to expiration, else false.
     */
    @VisibleForTesting
    public boolean areCarrierKeysAbsentOrExpiring() {
        for (int key_type : CARRIER_KEY_TYPES) {
            if (!isKeyEnabled(key_type)) {
                continue;
            }
            // get encryption info with fallback=false so that we attempt a download even if there's
            // backup info stored in carrier config
            ImsiEncryptionInfo imsiEncryptionInfo =
                    mPhone.getCarrierInfoForImsiEncryption(key_type, false);
            if (imsiEncryptionInfo == null) {
                logd("Key not found for: " + key_type);
                return true;
            } else if (imsiEncryptionInfo.getCarrierId() == TelephonyManager.UNKNOWN_CARRIER_ID) {
                logd("carrier key is unknown carrier, so prefer to reDownload");
                mDeleteOldKeyAfterDownload = true;
                return true;
            }
            Date imsiDate = imsiEncryptionInfo.getExpirationTime();
            long timeToExpire = imsiDate.getTime() - System.currentTimeMillis();
            return timeToExpire < START_RENEWAL_WINDOW_DAYS * DAY_IN_MILLIS;
        }
        return false;
    }

    private boolean downloadKey() {
        logd("starting download from: " + mURL);
        String mccMnc = null;
        int carrierId = -1;
        if (Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
            if (TextUtils.isEmpty(mMccMncForDownload)
                    || mCarrierId == TelephonyManager.UNKNOWN_CARRIER_ID) {
                loge("mccmnc or carrierId is UnKnown");
                return false;
            }
        } else {
            mccMnc = getSimOperator();
            carrierId = getSimCarrierId();
            if (!TextUtils.isEmpty(mccMnc) || carrierId != TelephonyManager.UNKNOWN_CARRIER_ID) {
                Log.d(LOG_TAG, "downloading key for mccmnc : " + mccMnc + ", carrierId : "
                        + carrierId);
            } else {
                Log.e(LOG_TAG, "mccmnc or carrierId is UnKnown");
                return false;
            }
        }

        try {
            // register the broadcast receiver to listen for download complete
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            mContext.registerReceiver(mDownloadReceiver, filter, null, mPhone,
                    Context.RECEIVER_EXPORTED);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mURL));

            // TODO(b/128550341): Implement the logic to minimize using metered network such as
            // LTE for downloading a certificate.
            request.setAllowedOverMetered(mAllowedOverMeteredNetwork);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.addRequestHeader("Accept-Encoding", "gzip");
            long carrierKeyDownloadRequestId = mDownloadManager.enqueue(request);
            if (!Flags.imsiKeyRetryDownloadOnPhoneUnlock()) {
                mMccMncForDownload = mccMnc;
                mCarrierId = carrierId;
            }
            mDownloadId = carrierKeyDownloadRequestId;
            logd("saving values mccmnc: " + mMccMncForDownload + ", downloadId: "
                    + carrierKeyDownloadRequestId + ", carrierId: " + mCarrierId);
        } catch (Exception e) {
            loge( "exception trying to download key from url: " + mURL + ",  Exception = "
                    + e.getMessage());
            printDeviceLockStatus();
            return false;
        }
        return true;
    }

    /**
     * Save the public key
     * @param certificate certificate that contains the public key.
     * @return Pair containing the Public Key and the expiration date.
     **/
    @VisibleForTesting
    public static Pair<PublicKey, Long> getKeyInformation(byte[] certificate) throws Exception {
        InputStream inStream = new ByteArrayInputStream(certificate);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
        Pair<PublicKey, Long> keyInformation =
                new Pair<>(cert.getPublicKey(), cert.getNotAfter().getTime());
        return keyInformation;
    }

    /**
     * Save the public key
     * @param publicKey public key.
     * @param type key-type.
     * @param identifier which is an opaque string.
     * @param expirationDate expiration date of the key.
     * @param mcc
     * @param mnc
     **/
    @VisibleForTesting
    public void savePublicKey(PublicKey publicKey, int type, String identifier, long expirationDate,
            String mcc, String mnc, int carrierId) {
        ImsiEncryptionInfo imsiEncryptionInfo = new ImsiEncryptionInfo(mcc, mnc,
                type, identifier, publicKey, new Date(expirationDate), carrierId);
        mPhone.setCarrierInfoForImsiEncryption(imsiEncryptionInfo);
    }

    /**
     * Remove potential extraneous text in a certificate string
     * @param cert certificate string
     * @return Cleaned up version of the certificate string
     */
    @VisibleForTesting
    public static String cleanCertString(String cert) {
        return cert.substring(
                cert.indexOf(CERT_BEGIN_STRING),
                cert.indexOf(CERT_END_STRING) + CERT_END_STRING.length());
    }

    /**
     * Registering the callback to listen on data connection availability.
     *
     * @param slotId Sim slotIndex that tries to download the key.
     */
    private void registerDefaultNetworkCb(int slotId) {
        logd("RegisterDefaultNetworkCb for slotId = " + slotId);
        if (mDefaultNetworkCallback == null
                && slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            mDefaultNetworkCallback = new DefaultNetworkCallback(slotId);
            mConnectivityManager.registerDefaultNetworkCallback(mDefaultNetworkCallback, this);
        }
    }

    /**
     * Unregister the data connection monitor listener.
     */
    private void unregisterDefaultNetworkCb(int slotId) {
        logd("unregisterDefaultNetworkCb for slotId = " + slotId);
        if (mDefaultNetworkCallback != null) {
            mConnectivityManager.unregisterNetworkCallback(mDefaultNetworkCallback);
            mDefaultNetworkCallback = null;
        }
    }

    final class DefaultNetworkCallback extends ConnectivityManager.NetworkCallback {
        final int mSlotIndex;

        public DefaultNetworkCallback(int slotId) {
            mSlotIndex = slotId;
        }

        /** Called when the framework connects and has declared a new network ready for use. */
        @Override
        public void onAvailable(@NonNull Network network) {
            logd("Data network connected, slotId = " + mSlotIndex);
            if (mConnectivityManager.getActiveNetwork() != null && SubscriptionManager.getSlotIndex(
                    mPhone.getSubId()) == mSlotIndex) {
                mIsRequiredToHandleUnlock = false;
                unregisterDefaultNetworkCb(mSlotIndex);
                sendEmptyMessage(EVENT_NETWORK_AVAILABLE);
            }
        }
    }

    private void loge(String logStr) {
        String TAG = LOG_TAG + " [" + mPhone.getPhoneId() + "]";
        Log.e(TAG, logStr);
    }

    private void logd(String logStr) {
        String TAG = LOG_TAG + " [" + mPhone.getPhoneId() + "]";
        Log.d(TAG, logStr);
    }
}

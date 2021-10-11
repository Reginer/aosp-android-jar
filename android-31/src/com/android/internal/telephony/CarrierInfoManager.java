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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.android.internal.telephony.metrics.TelephonyMetrics;

import java.security.PublicKey;
import java.util.Date;

/**
 * This class provides methods to retreive information from the CarrierKeyProvider.
 */
public class CarrierInfoManager {
    private static final String LOG_TAG = "CarrierInfoManager";
    private static final String KEY_TYPE = "KEY_TYPE";

    /*
    * Rate limit (in milliseconds) the number of times the Carrier keys can be reset.
    * Do it at most once every 12 hours.
    */
    private static final int RESET_CARRIER_KEY_RATE_LIMIT = 12 * 60 * 60 * 1000;

    // Key ID used with the backup key from carrier config
    private static final String EPDG_BACKUP_KEY_ID = "backup_key_from_carrier_config_epdg";
    private static final String WLAN_BACKUP_KEY_ID = "backup_key_from_carrier_config_wlan";

    // Last time the resetCarrierKeysForImsiEncryption API was called successfully.
    private long mLastAccessResetCarrierKey = 0;

    /**
     * Returns Carrier specific information that will be used to encrypt the IMSI and IMPI.
     * @param keyType whether the key is being used for WLAN or ePDG.
     * @param context
     * @param fallback whether to fallback to the IMSI key info stored in carrier config
     * @return ImsiEncryptionInfo which contains the information, including the public key, to be
     *         used for encryption.
     */
    public static ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int keyType,
                                                                     Context context,
                                                                     String operatorNumeric,
                                                                     boolean fallback,
                                                                     int subId) {
        String mcc = "";
        String mnc = "";
        if (!TextUtils.isEmpty(operatorNumeric)) {
            mcc = operatorNumeric.substring(0, 3);
            mnc = operatorNumeric.substring(3);
            Log.i(LOG_TAG, "using values for mcc, mnc: " + mcc + "," + mnc);
        } else {
            Log.e(LOG_TAG, "Invalid networkOperator: " + operatorNumeric);
            return null;
        }
        Cursor findCursor = null;
        try {
            // In the current design, MVNOs are not supported. If we decide to support them,
            // we'll need to add to this CL.
            ContentResolver mContentResolver = context.getContentResolver();
            String[] columns = {Telephony.CarrierColumns.PUBLIC_KEY,
                    Telephony.CarrierColumns.EXPIRATION_TIME,
                    Telephony.CarrierColumns.KEY_IDENTIFIER};
            findCursor = mContentResolver.query(Telephony.CarrierColumns.CONTENT_URI, columns,
                    "mcc=? and mnc=? and key_type=?",
                    new String[]{mcc, mnc, String.valueOf(keyType)}, null);
            if (findCursor == null || !findCursor.moveToFirst()) {
                Log.d(LOG_TAG, "No rows found for keyType: " + keyType);
                if (!fallback) {
                    Log.d(LOG_TAG, "Skipping fallback logic");
                    return null;
                }
                // return carrier config key as fallback
                CarrierConfigManager carrierConfigManager = (CarrierConfigManager)
                        context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
                if (carrierConfigManager == null) {
                    Log.d(LOG_TAG, "Could not get CarrierConfigManager for backup key");
                    return null;
                }
                if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    Log.d(LOG_TAG, "Could not get carrier config with invalid subId");
                    return null;
                }
                PersistableBundle b = carrierConfigManager.getConfigForSubId(subId);
                if (b == null) {
                    Log.d(LOG_TAG, "Could not get carrier config bundle for backup key");
                    return null;
                }
                int keyAvailabilityBitmask = b.getInt(
                        CarrierConfigManager.IMSI_KEY_AVAILABILITY_INT);
                if (!CarrierKeyDownloadManager.isKeyEnabled(keyType, keyAvailabilityBitmask)) {
                    Log.d(LOG_TAG, "Backup key does not have matching keyType. keyType=" + keyType
                            + " keyAvailability=" + keyAvailabilityBitmask);
                    return null;
                }
                String keyString = null;
                String keyId = null;
                if (keyType == TelephonyManager.KEY_TYPE_EPDG) {
                    keyString = b.getString(
                            CarrierConfigManager.IMSI_CARRIER_PUBLIC_KEY_EPDG_STRING);
                    keyId = EPDG_BACKUP_KEY_ID;
                } else if (keyType == TelephonyManager.KEY_TYPE_WLAN) {
                    keyString = b.getString(
                            CarrierConfigManager.IMSI_CARRIER_PUBLIC_KEY_WLAN_STRING);
                    keyId = WLAN_BACKUP_KEY_ID;
                }
                if (TextUtils.isEmpty(keyString)) {
                    Log.d(LOG_TAG,
                            "Could not get carrier config key string for backup key. keyType="
                                    + keyType);
                    return null;
                }
                Pair<PublicKey, Long> keyInfo =
                        CarrierKeyDownloadManager.getKeyInformation(keyString.getBytes());
                return new ImsiEncryptionInfo(mcc, mnc, keyType, keyId,
                        keyInfo.first, new Date(keyInfo.second));
            }
            if (findCursor.getCount() > 1) {
                Log.e(LOG_TAG, "More than 1 row found for the keyType: " + keyType);
            }
            byte[] carrier_key = findCursor.getBlob(0);
            Date expirationTime = new Date(findCursor.getLong(1));
            String keyIdentifier = findCursor.getString(2);
            return new ImsiEncryptionInfo(mcc, mnc, keyType, keyIdentifier, carrier_key,
                    expirationTime);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Bad arguments:" + e);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Query failed:" + e);
        } finally {
            if (findCursor != null) {
                findCursor.close();
            }
        }
        return null;
    }

    /**
     * Inserts or update the Carrier Key in the database
     * @param imsiEncryptionInfo ImsiEncryptionInfo object.
     * @param context Context.
     */
    public static void updateOrInsertCarrierKey(ImsiEncryptionInfo imsiEncryptionInfo,
                                                Context context, int phoneId) {
        byte[] keyBytes = imsiEncryptionInfo.getPublicKey().getEncoded();
        ContentResolver mContentResolver = context.getContentResolver();
        TelephonyMetrics tm = TelephonyMetrics.getInstance();
        // In the current design, MVNOs are not supported. If we decide to support them,
        // we'll need to add to this CL.
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.CarrierColumns.MCC, imsiEncryptionInfo.getMcc());
        contentValues.put(Telephony.CarrierColumns.MNC, imsiEncryptionInfo.getMnc());
        contentValues.put(Telephony.CarrierColumns.KEY_TYPE,
                imsiEncryptionInfo.getKeyType());
        contentValues.put(Telephony.CarrierColumns.KEY_IDENTIFIER,
                imsiEncryptionInfo.getKeyIdentifier());
        contentValues.put(Telephony.CarrierColumns.PUBLIC_KEY, keyBytes);
        contentValues.put(Telephony.CarrierColumns.EXPIRATION_TIME,
                imsiEncryptionInfo.getExpirationTime().getTime());
        boolean downloadSuccessfull = true;
        try {
            Log.i(LOG_TAG, "Inserting imsiEncryptionInfo into db");
            mContentResolver.insert(Telephony.CarrierColumns.CONTENT_URI, contentValues);
        } catch (SQLiteConstraintException e) {
            Log.i(LOG_TAG, "Insert failed, updating imsiEncryptionInfo into db");
            ContentValues updatedValues = new ContentValues();
            updatedValues.put(Telephony.CarrierColumns.PUBLIC_KEY, keyBytes);
            updatedValues.put(Telephony.CarrierColumns.EXPIRATION_TIME,
                    imsiEncryptionInfo.getExpirationTime().getTime());
            updatedValues.put(Telephony.CarrierColumns.KEY_IDENTIFIER,
                    imsiEncryptionInfo.getKeyIdentifier());
            try {
                int nRows = mContentResolver.update(Telephony.CarrierColumns.CONTENT_URI,
                        updatedValues,
                        "mcc=? and mnc=? and key_type=?", new String[]{
                                imsiEncryptionInfo.getMcc(),
                                imsiEncryptionInfo.getMnc(),
                                String.valueOf(imsiEncryptionInfo.getKeyType())});
                if (nRows == 0) {
                    Log.d(LOG_TAG, "Error updating values:" + imsiEncryptionInfo);
                    downloadSuccessfull = false;
                }
            } catch (Exception ex) {
                Log.d(LOG_TAG, "Error updating values:" + imsiEncryptionInfo + ex);
                downloadSuccessfull = false;
            }
        }  catch (Exception e) {
            Log.d(LOG_TAG, "Error inserting/updating values:" + imsiEncryptionInfo + e);
            downloadSuccessfull = false;
        } finally {
            tm.writeCarrierKeyEvent(phoneId, imsiEncryptionInfo.getKeyType(), downloadSuccessfull);
        }
    }

    /**
     * Sets the Carrier specific information that will be used to encrypt the IMSI and IMPI.
     * This includes the public key and the key identifier. This information will be stored in the
     * device keystore.
     * @param imsiEncryptionInfo which includes the Key Type, the Public Key
     *        {@link java.security.PublicKey} and the Key Identifier.
     *        The keyIdentifier Attribute value pair that helps a server locate
     *        the private key to decrypt the permanent identity.
     * @param context Context.
     */
    public static void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo,
                                                       Context context, int phoneId) {
        Log.i(LOG_TAG, "inserting carrier key: " + imsiEncryptionInfo);
        updateOrInsertCarrierKey(imsiEncryptionInfo, context, phoneId);
        //todo send key to modem. Will be done in a subsequent CL.
    }

    /**
     * Resets the Carrier Keys in the database. This involves 2 steps:
     *  1. Delete the keys from the database.
     *  2. Send an intent to download new Certificates.
     * @param context Context
     * @param mPhoneId phoneId
     *
     */
    public void resetCarrierKeysForImsiEncryption(Context context, int mPhoneId) {
        Log.i(LOG_TAG, "resetting carrier key");
        // Check rate limit.
        long now = System.currentTimeMillis();
        if (now - mLastAccessResetCarrierKey < RESET_CARRIER_KEY_RATE_LIMIT) {
            Log.i(LOG_TAG, "resetCarrierKeysForImsiEncryption: Access rate exceeded");
            return;
        }
        mLastAccessResetCarrierKey = now;
        int[] subIds = context.getSystemService(SubscriptionManager.class)
                .getSubscriptionIds(mPhoneId);
        if (subIds == null || subIds.length < 1) {
            Log.e(LOG_TAG, "Could not reset carrier keys, subscription for mPhoneId=" + mPhoneId);
            return;
        }
        deleteCarrierInfoForImsiEncryption(context, subIds[0]);
        Intent resetIntent = new Intent(TelephonyIntents.ACTION_CARRIER_CERTIFICATE_DOWNLOAD);
        SubscriptionManager.putPhoneIdAndSubIdExtra(resetIntent, mPhoneId);
        context.sendBroadcastAsUser(resetIntent, UserHandle.ALL);
    }

    /**
     * Deletes all the keys for a given Carrier from the device keystore.
     * @param context Context
     */
    public static void deleteCarrierInfoForImsiEncryption(Context context, int subId) {
        Log.i(LOG_TAG, "deleting carrier key from db for subId=" + subId);
        String mcc = "";
        String mnc = "";
        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        String simOperator = telephonyManager.getSimOperator();
        if (!TextUtils.isEmpty(simOperator)) {
            mcc = simOperator.substring(0, 3);
            mnc = simOperator.substring(3);
        } else {
            Log.e(LOG_TAG, "Invalid networkOperator: " + simOperator);
            return;
        }
        ContentResolver mContentResolver = context.getContentResolver();
        try {
            String whereClause = "mcc=? and mnc=?";
            String[] whereArgs = new String[] { mcc, mnc };
            mContentResolver.delete(Telephony.CarrierColumns.CONTENT_URI, whereClause, whereArgs);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Delete failed" + e);
        }
    }

    /**
     * Deletes all the keys from the device keystore.
     * @param context Context
     */
    public static void deleteAllCarrierKeysForImsiEncryption(Context context) {
        Log.i(LOG_TAG, "deleting ALL carrier keys from db");
        ContentResolver mContentResolver = context.getContentResolver();
        try {
            mContentResolver.delete(Telephony.CarrierColumns.CONTENT_URI, null, null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Delete failed" + e);
        }
    }
}

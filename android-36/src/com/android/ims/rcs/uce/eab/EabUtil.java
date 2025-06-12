/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.ims.rcs.uce.eab;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.android.ims.rcs.uce.eab.EabProvider.ContactColumns;
import com.android.ims.rcs.uce.eab.EabProvider.EabCommonColumns;
import com.android.ims.rcs.uce.eab.EabProvider.OptionsColumns;
import com.android.ims.rcs.uce.eab.EabProvider.PresenceTupleColumns;
import com.android.ims.rcs.uce.util.UceUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The util to modify the EAB database.
 */
public class EabUtil {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "EabUtil";

    /**
     * Get the given EAB contacts from the EAB database.
     *
     * Output format:
     * [PHONE_NUMBER], [RAW_CONTACT_ID], [CONTACT_ID], [DATA_ID]
     */
    public static String getContactFromEab(Context context, String contact) {
        StringBuilder result = new StringBuilder();
        try (Cursor cursor = context.getContentResolver().query(
                EabProvider.CONTACT_URI,
                new String[]{ContactColumns.PHONE_NUMBER,
                        ContactColumns.RAW_CONTACT_ID,
                        ContactColumns.CONTACT_ID,
                        ContactColumns.DATA_ID},
                ContactColumns.PHONE_NUMBER + "=?",
                new String[]{contact}, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                result.append(cursor.getString(cursor.getColumnIndex(
                        ContactColumns.PHONE_NUMBER)));
                result.append(",");
                result.append(cursor.getString(cursor.getColumnIndex(
                        ContactColumns.RAW_CONTACT_ID)));
                result.append(",");
                result.append(cursor.getString(cursor.getColumnIndex(
                        ContactColumns.CONTACT_ID)));
                result.append(",");
                result.append(cursor.getString(cursor.getColumnIndex(
                        ContactColumns.DATA_ID)));
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "getEabContactId exception " + e);
        }
        Log.d(LOG_TAG, "getContactFromEab() result: " + result);
        return result.toString();
    }

    /**
     * Get the given EAB capability from the EAB database.
     *
     * Output format:
     * [PHONE_NUMBER], [RAW_PRESENCE_ID], [PRESENCE_TIMESTAMP], [RAW_OPTION_ID], [OPTION_TIMESTAMP]
     */
    public static String getCapabilityFromEab(Context context, String contact) {
        StringBuilder result = new StringBuilder();
        try (Cursor cursor = context.getContentResolver().query(
                EabProvider.ALL_DATA_URI,
                new String[]{ContactColumns.PHONE_NUMBER,
                        PresenceTupleColumns._ID,
                        PresenceTupleColumns.REQUEST_TIMESTAMP,
                        OptionsColumns._ID,
                        OptionsColumns.REQUEST_TIMESTAMP},
                ContactColumns.PHONE_NUMBER + "=?",
                new String[]{contact}, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                result.append(cursor.getString(cursor.getColumnIndex(
                        ContactColumns.PHONE_NUMBER)));
                result.append(",");
                result.append(cursor.getString(cursor.getColumnIndex(
                        PresenceTupleColumns._ID)));
                result.append(",");
                result.append(cursor.getString(cursor.getColumnIndex(
                        PresenceTupleColumns.REQUEST_TIMESTAMP)));
                result.append(",");
                result.append(cursor.getString(cursor.getColumnIndex(
                        OptionsColumns._ID)));
                result.append(",");
                result.append(cursor.getString(cursor.getColumnIndex(
                        OptionsColumns.REQUEST_TIMESTAMP)));
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "getCapability exception " + e);
        }
        Log.d(LOG_TAG, "getCapabilityFromEab() result: " + result);
        return result.toString();
    }

    /**
     * Remove the given EAB contacts from the EAB database.
     */
    public static int removeContactFromEab(int subId, String contacts, Context context) {
        if (TextUtils.isEmpty(contacts)) {
            return -1;
        }
        List<String> contactList = Arrays.stream(contacts.split(",")).collect(Collectors.toList());
        if (contactList == null || contactList.isEmpty()) {
            return -1;
        }
        int count = 0;
        for (String contact : contactList) {
            int contactId = getEabContactId(contact, context);
            if (contactId == -1) {
                continue;
            }
            int commonId = getEabCommonId(contactId, context);
            count += removeContactCapabilities(contactId, commonId, context);
        }
        return count;
    }

    private static int getEabContactId(String contactNumber, Context context) {
        int contactId = -1;
        Cursor cursor = null;
        String formattedNumber = EabControllerImpl.formatNumber(context, contactNumber);
        try {
            cursor = context.getContentResolver().query(
                    EabProvider.CONTACT_URI,
                    new String[] { EabProvider.EabCommonColumns._ID },
                    EabProvider.ContactColumns.PHONE_NUMBER + "=?",
                    new String[] { formattedNumber }, null);
            if (cursor != null && cursor.moveToFirst()) {
                contactId = cursor.getInt(cursor.getColumnIndex(EabProvider.ContactColumns._ID));
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "getEabContactId exception " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return contactId;
    }

    private static int getEabCommonId(int contactId, Context context) {
        int commonId = -1;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    EabProvider.COMMON_URI,
                    new String[] { EabProvider.EabCommonColumns._ID },
                    EabProvider.EabCommonColumns.EAB_CONTACT_ID + "=?",
                    new String[] { String.valueOf(contactId) }, null);
            if (cursor != null && cursor.moveToFirst()) {
                commonId = cursor.getInt(cursor.getColumnIndex(EabProvider.EabCommonColumns._ID));
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "getEabCommonId exception " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return commonId;
    }

    private static int removeContactCapabilities(int contactId, int commonId, Context context) {
        int count = 0;
        count = context.getContentResolver().delete(EabProvider.PRESENCE_URI,
                PresenceTupleColumns.EAB_COMMON_ID + "=?", new String[]{String.valueOf(commonId)});
        context.getContentResolver().delete(EabProvider.OPTIONS_URI,
                OptionsColumns.EAB_COMMON_ID + "=?", new String[]{String.valueOf(commonId)});
        context.getContentResolver().delete(EabProvider.COMMON_URI,
                EabCommonColumns.EAB_CONTACT_ID + "=?", new String[]{String.valueOf(contactId)});
        context.getContentResolver().delete(EabProvider.CONTACT_URI,
                ContactColumns._ID + "=?", new String[]{String.valueOf(contactId)});
        return count;
    }
}

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

package com.android.ims.rcs.uce.eab;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sync the contacts from Contact Provider to EAB Provider
 */
public class EabContactSyncController {
    private final String TAG = this.getClass().getSimpleName();

    private static final int NOT_INIT_LAST_UPDATED_TIME = -1;
    private static final String LAST_UPDATED_TIME_KEY = "eab_last_updated_time";

    /**
     * Sync contact from Contact provider to EAB provider. There are 4 kinds of cases need to be
     * handled when received the contact db changed:
     *
     * 1. Contact deleted
     * 2. Delete the phone number in the contact
     * 3. Update the phone number
     * 4. Add a new contact and add phone number
     *
     * @return The contacts that need to refresh
     */
    @VisibleForTesting
    public List<Uri> syncContactToEabProvider(Context context) {
        Log.d(TAG, "syncContactToEabProvider");
        List<Uri> refreshContacts = null;
        StringBuilder selection = new StringBuilder();
        String[] selectionArgs = null;

        // Get the last update timestamp from shared preference.
        long lastUpdatedTimeStamp = getLastUpdatedTime(context);
        if (lastUpdatedTimeStamp != -1) {
            selection.append(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + ">?");
            selectionArgs = new String[]{String.valueOf(lastUpdatedTimeStamp)};
        }

        // Contact deleted cases (case 1)
        handleContactDeletedCase(context, lastUpdatedTimeStamp);

        // Query the contacts that have not been synchronized to eab contact table.
        Cursor updatedContact = context.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                null,
                selection.toString(),
                selectionArgs,
                null);

        if (updatedContact != null) {
            Log.d(TAG, "Contact changed count: " + updatedContact.getCount());

            if (updatedContact.getCount() == 0) {
                return new ArrayList<>();
            }

            // Delete the EAB phone number that not in contact provider (case 2). Updated phone
            // number(case 3) also delete in here and re-insert in next step.
            handlePhoneNumberDeletedCase(context, updatedContact);

            // Insert the phone number that not in EAB provider (case 3 and case 4)
            refreshContacts = handlePhoneNumberInsertedCase(context, updatedContact);

            // Update the last update time in shared preference
            if (updatedContact.getCount() > 0) {
                long maxTimestamp = findMaxTimestamp(updatedContact);
                if (maxTimestamp != Long.MIN_VALUE) {
                    setLastUpdatedTime(context, maxTimestamp);
                }
            }
            updatedContact.close();
        } else {
            Log.e(TAG, "Cursor is null.");
        }
        return refreshContacts;
    }

    /**
     * Delete the phone numbers that contact has been deleted in contact provider. Query based on
     * {@link ContactsContract.DeletedContacts#CONTENT_URI} to know which contact has been removed.
     *
     * @param timeStamp last updated timestamp
     */
    private void handleContactDeletedCase(Context context, long timeStamp) {
        String selection = "";
        if (timeStamp != -1) {
            selection =
                    ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP + ">" + timeStamp;
        }

        Cursor cursor = context.getContentResolver().query(
                ContactsContract.DeletedContacts.CONTENT_URI,
                new String[]{ContactsContract.DeletedContacts.CONTACT_ID,
                        ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP},
                selection,
                null,
                null);

        if (cursor == null) {
            Log.d(TAG, "handleContactDeletedCase() cursor is null.");
            return;
        }

        Log.d(TAG, "(Case 1) The count of contact that need to be deleted: "
                + cursor.getCount());

        StringBuilder deleteClause = new StringBuilder();
        while (cursor.moveToNext()) {
            if (deleteClause.length() > 0) {
                deleteClause.append(" OR ");
            }

            String contactId = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.DeletedContacts.CONTACT_ID));
            deleteClause.append(EabProvider.ContactColumns.CONTACT_ID + "=" + contactId);
        }

        if (deleteClause.toString().length() > 0) {
            int number = context.getContentResolver().delete(
                    EabProvider.CONTACT_URI,
                    deleteClause.toString(),
                    null);
            Log.d(TAG, "(Case 1) Deleted contact count=" + number);
        }
    }

    /**
     * Delete phone numbers that have been deleted in the contact provider. There is no API to get
     * deleted phone numbers easily, so check all updated contact's phone number and delete the
     * phone number. It will also delete the phone number that has been changed.
     */
    private void handlePhoneNumberDeletedCase(Context context, Cursor cursor) {
        // The map represent which contacts have which numbers.
        Map<String, List<String>> phoneNumberMap = new HashMap<>();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String mimeType = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
            if (!mimeType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                continue;
            }

            String rawContactId = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID));
            String number = formatNumber(context, cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));

            if (phoneNumberMap.containsKey(rawContactId)) {
                phoneNumberMap.get(rawContactId).add(number);
            } else {
                List<String> phoneNumberList = new ArrayList<>();
                phoneNumberList.add(number);
                phoneNumberMap.put(rawContactId, phoneNumberList);
            }
        }

        // Build a SQL statement that delete the phone number not exist in contact provider.
        // For example:
        // raw_contact_id = 1 AND phone_number NOT IN (12345, 23456)
        StringBuilder deleteClause = new StringBuilder();
        List<String> deleteClauseArgs = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : phoneNumberMap.entrySet()) {
            String rawContactId = entry.getKey();
            List<String> phoneNumberList = entry.getValue();

            if (deleteClause.length() > 0) {
                deleteClause.append(" OR ");
            }

            deleteClause.append("(" + EabProvider.ContactColumns.RAW_CONTACT_ID + "=? ");
            deleteClauseArgs.add(rawContactId);

            if (phoneNumberList.size() > 0) {
                String argsList = phoneNumberList.stream()
                        .map(s -> "?")
                        .collect(Collectors.joining(", "));
                deleteClause.append(" AND "
                        + EabProvider.ContactColumns.PHONE_NUMBER
                        + " NOT IN (" + argsList + "))");
                deleteClauseArgs.addAll(phoneNumberList);
            } else {
                deleteClause.append(")");
            }
        }

        int number = context.getContentResolver().delete(
                EabProvider.CONTACT_URI,
                deleteClause.toString(),
                deleteClauseArgs.toArray(new String[0]));
        Log.d(TAG, "(Case 2, 3) handlePhoneNumberDeletedCase number count= " + number);
    }

    /**
     * Insert new phone number.
     *
     * @param contactCursor the result of updated contact
     * @return the contacts that need to refresh
     */
    private List<Uri> handlePhoneNumberInsertedCase(Context context,
            Cursor contactCursor) {
        List<Uri> refreshContacts = new ArrayList<>();
        List<ContentValues> allContactData = new ArrayList<>();
        contactCursor.moveToPosition(-1);

        // Query all of contacts that store in eab provider
        Cursor eabContact = context.getContentResolver().query(
                EabProvider.CONTACT_URI,
                null,
                EabProvider.ContactColumns.DATA_ID + " IS NOT NULL",
                null,
                EabProvider.ContactColumns.DATA_ID);

        while (contactCursor.moveToNext()) {
            String contactId = contactCursor.getString(contactCursor.getColumnIndex(
                    ContactsContract.Data.CONTACT_ID));
            String rawContactId = contactCursor.getString(contactCursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID));
            String dataId = contactCursor.getString(
                    contactCursor.getColumnIndex(ContactsContract.Data._ID));
            String mimeType = contactCursor.getString(
                    contactCursor.getColumnIndex(ContactsContract.Data.MIMETYPE));

            if (!mimeType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                continue;
            }

            String number = formatNumber(context, contactCursor.getString(
                    contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));

            int index = searchDataIdIndex(eabContact, Integer.parseInt(dataId));
            if (index == -1) {
                Log.d(TAG, "Data id does not exist. Insert phone number into EAB db.");
                refreshContacts.add(Uri.parse(number));
                ContentValues data = new ContentValues();
                data.put(EabProvider.ContactColumns.CONTACT_ID, contactId);
                data.put(EabProvider.ContactColumns.DATA_ID, dataId);
                data.put(EabProvider.ContactColumns.RAW_CONTACT_ID, rawContactId);
                data.put(EabProvider.ContactColumns.PHONE_NUMBER, number);
                allContactData.add(data);
            }
        }

        // Insert contacts at once
        int result = context.getContentResolver().bulkInsert(
                EabProvider.CONTACT_URI,
                allContactData.toArray(new ContentValues[0]));
        Log.d(TAG, "(Case 3, 4) Phone number insert count: " + result);
        return refreshContacts;
    }

    /**
     * Binary search the target data_id in the cursor.
     *
     * @param cursor       EabProvider contact which sorted by
     *                     {@link EabProvider.ContactColumns#DATA_ID}
     * @param targetDataId the data_id to search for
     * @return the index of cursor
     */
    private int searchDataIdIndex(Cursor cursor, int targetDataId) {
        int start = 0;
        int end = cursor.getCount() - 1;

        while (start <= end) {
            int position = (start + end) >>> 1;
            cursor.moveToPosition(position);
            int dataId = cursor.getInt(cursor.getColumnIndex(EabProvider.ContactColumns.DATA_ID));

            if (dataId > targetDataId) {
                end = position - 1;
            } else if (dataId < targetDataId) {
                start = position + 1;
            } else {
                return position;
            }
        }
        return -1;
    }


    private long findMaxTimestamp(Cursor cursor) {
        long maxTimestamp = Long.MIN_VALUE;
        cursor.moveToPosition(-1);
        while(cursor.moveToNext()) {
            long lastUpdatedTimeStamp = cursor.getLong(cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP));
            Log.d(TAG, lastUpdatedTimeStamp + " " + maxTimestamp);
            if (lastUpdatedTimeStamp > maxTimestamp) {
                maxTimestamp = lastUpdatedTimeStamp;
            }
        }
        return maxTimestamp;
    }

    private void setLastUpdatedTime(Context context, long timestamp) {
        Log.d(TAG, "setLastUpdatedTime: " + timestamp);
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putLong(LAST_UPDATED_TIME_KEY, timestamp).apply();
    }

    private long getLastUpdatedTime(Context context) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(LAST_UPDATED_TIME_KEY, NOT_INIT_LAST_UPDATED_TIME);
    }

    private String formatNumber(Context context, String number) {
        TelephonyManager manager = context.getSystemService(TelephonyManager.class);
        String simCountryIso = manager.getSimCountryIso();
        if (simCountryIso != null) {
            simCountryIso = simCountryIso.toUpperCase();
            PhoneNumberUtil util = PhoneNumberUtil.getInstance();
            try {
                Phonenumber.PhoneNumber phoneNumber = util.parse(number, simCountryIso);
                return util.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
            } catch (NumberParseException e) {
                Log.w(TAG, "formatNumber: could not format " + number + ", error: " + e);
            }
        }
        return number;
    }
}

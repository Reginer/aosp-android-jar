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

package com.android.internal.telephony.analytics;

import static android.os.Build.VERSION.INCREMENTAL;

import static com.android.internal.telephony.analytics.TelephonyAnalyticsDatabase.DATE_FORMAT;

import android.content.ContentValues;
import android.database.Cursor;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.analytics.TelephonyAnalyticsDatabase.SmsMmsAnalyticsTable;
import com.android.telephony.Rlog;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Provider class for Sms and Mms Receives the data from SmsMmsAnalytics Performs business logic on
 * the received data Uses Util class for required db operation. Implements the
 * TelephonyAnalyticsProvider interface to provide aggregation functionality.
 */
public class SmsMmsAnalyticsProvider implements TelephonyAnalyticsProvider {
    private static final String TAG = SmsMmsAnalyticsProvider.class.getSimpleName();

    private TelephonyAnalyticsUtil mTelephonyAnalyticsUtil;
    private String mDateOfDeletedRecordsSmsMmsTable;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    private static final String CREATE_SMS_MMS_ANALYTICS_TABLE =
            "CREATE TABLE IF NOT EXISTS "
                    + SmsMmsAnalyticsTable.TABLE_NAME
                    + "("
                    + SmsMmsAnalyticsTable._ID
                    + " INTEGER PRIMARY KEY,"
                    + SmsMmsAnalyticsTable.LOG_DATE
                    + " DATE ,"
                    + SmsMmsAnalyticsTable.SMS_MMS_STATUS
                    + " TEXT DEFAULT '',"
                    + SmsMmsAnalyticsTable.SMS_MMS_TYPE
                    + " TEXT DEFAULT '',"
                    + SmsMmsAnalyticsTable.SLOT_ID
                    + " INTEGER , "
                    + SmsMmsAnalyticsTable.RAT
                    + " TEXT DEFAULT '',"
                    + SmsMmsAnalyticsTable.FAILURE_REASON
                    + " TEXT DEFAULT '',"
                    + SmsMmsAnalyticsTable.RELEASE_VERSION
                    + " TEXT DEFAULT '' , "
                    + SmsMmsAnalyticsTable.COUNT
                    + " INTEGER DEFAULT 1 "
                    + ");";
    private static final String SMS_MMS_OVERFLOW_DATA_DELETION_SELECTION =
            SmsMmsAnalyticsTable._ID
                    + " IN "
                    + " ( SELECT "
                    + SmsMmsAnalyticsTable._ID
                    + " FROM "
                    + SmsMmsAnalyticsTable.TABLE_NAME
                    + " ORDER BY "
                    + SmsMmsAnalyticsTable.LOG_DATE
                    + " DESC LIMIT -1 OFFSET ? ) ";
    private static final String SMS_MMS_OLD_DATA_DELETION_SELECTION =
            SmsMmsAnalyticsTable.LOG_DATE + " < ? ";

    private enum SmsMmsType {
        SMS_OUTGOING("SMS Outgoing"),
        SMS_INCOMING("SMS Incoming"),
        MMS_OUTGOING("MMS Outgoing"),
        MMS_INCOMING("MMS Incoming");
        public final String value;

        SmsMmsType(String value) {
            this.value = value;
        }
    }

    private enum SmsMmsStatus {
        SUCCESS("Success"),
        FAILURE("Failure");
        public final String value;

        SmsMmsStatus(String value) {
            this.value = value;
        }
    }

    private final int mSlotIndex;

    public SmsMmsAnalyticsProvider(TelephonyAnalyticsUtil databaseUtil, int slotIndex) {
        mTelephonyAnalyticsUtil = databaseUtil;
        mSlotIndex = slotIndex;
        mTelephonyAnalyticsUtil.createTable(CREATE_SMS_MMS_ANALYTICS_TABLE);
    }

    private static final String[] SMS_MMS_INSERTION_PROJECTION = {
            SmsMmsAnalyticsTable._ID, SmsMmsAnalyticsTable.COUNT
    };

    private static final String SMS_MMS_INSERTION_SUCCESS_SELECTION =
            SmsMmsAnalyticsTable.LOG_DATE
                    + " = ? AND "
                    + SmsMmsAnalyticsTable.SMS_MMS_TYPE
                    + " = ? AND "
                    + SmsMmsAnalyticsTable.SMS_MMS_STATUS
                    + " = ? AND "
                    + SmsMmsAnalyticsTable.SLOT_ID
                    + " = ? ";

    private static final String SMS_MMS_INSERTION_FAILURE_SELECTION =
            SmsMmsAnalyticsTable.LOG_DATE
                    + " = ? AND "
                    + SmsMmsAnalyticsTable.SMS_MMS_STATUS
                    + " = ? AND "
                    + SmsMmsAnalyticsTable.SMS_MMS_TYPE
                    + " = ? AND "
                    + SmsMmsAnalyticsTable.RAT
                    + " = ? AND "
                    + SmsMmsAnalyticsTable.SLOT_ID
                    + " = ? AND "
                    + SmsMmsAnalyticsTable.FAILURE_REASON
                    + " = ? AND "
                    + SmsMmsAnalyticsTable.RELEASE_VERSION
                    + " = ? ";

    private ContentValues getContentValues(
            String status, String smsMmsType, String rat, String failureReason) {
        ContentValues values = new ContentValues();
        String dateToday = DATE_FORMAT.format(Calendar.getInstance().toInstant());
        values.put(SmsMmsAnalyticsTable.LOG_DATE, dateToday);
        values.put(SmsMmsAnalyticsTable.SMS_MMS_STATUS, status);
        values.put(SmsMmsAnalyticsTable.SMS_MMS_TYPE, smsMmsType);
        values.put(SmsMmsAnalyticsTable.RAT, rat);
        values.put(SmsMmsAnalyticsTable.SLOT_ID, mSlotIndex);
        values.put(SmsMmsAnalyticsTable.FAILURE_REASON, failureReason);
        values.put(SmsMmsAnalyticsTable.RELEASE_VERSION, INCREMENTAL);
        return values;
    }

    /**
     * Processes the received data for insertion to the database.
     *
     * @param status : SMS Status ,i.e. Success or Failure
     * @param smsMmsType : Type ,i.e. outgoing/incoming
     * @param rat : Radio Access Technology
     * @param failureReason : Reason for failure
     */
    @VisibleForTesting
    public void insertDataToDb(String status, String smsMmsType, String rat, String failureReason) {
        ContentValues values = getContentValues(status, smsMmsType, rat, failureReason);
        Rlog.d(TAG, values.toString());
        Cursor cursor = null;
        String[] selectionArgs;
        try {
            if (values.getAsString(SmsMmsAnalyticsTable.SMS_MMS_STATUS)
                    .equals(SmsMmsStatus.SUCCESS.value)) {
                Rlog.d(TAG, "Success Entry Data for Sms/Mms: " + values.toString());
                selectionArgs =
                        new String[] {
                                values.getAsString(SmsMmsAnalyticsTable.LOG_DATE),
                                values.getAsString(SmsMmsAnalyticsTable.SMS_MMS_TYPE),
                                values.getAsString(SmsMmsAnalyticsTable.SMS_MMS_STATUS),
                                values.getAsString(SmsMmsAnalyticsTable.SLOT_ID)
                        };
                cursor =
                        mTelephonyAnalyticsUtil.getCursor(
                                SmsMmsAnalyticsTable.TABLE_NAME,
                                SMS_MMS_INSERTION_PROJECTION,
                                SMS_MMS_INSERTION_SUCCESS_SELECTION,
                                selectionArgs,
                                null,
                                null,
                                null,
                                null);

            } else {
                selectionArgs =
                        new String[] {
                                values.getAsString(SmsMmsAnalyticsTable.LOG_DATE),
                                values.getAsString(SmsMmsAnalyticsTable.SMS_MMS_STATUS),
                                values.getAsString(SmsMmsAnalyticsTable.SMS_MMS_TYPE),
                                values.getAsString(SmsMmsAnalyticsTable.RAT),
                                values.getAsString(SmsMmsAnalyticsTable.SLOT_ID),
                                values.getAsString(SmsMmsAnalyticsTable.FAILURE_REASON),
                                values.getAsString(SmsMmsAnalyticsTable.RELEASE_VERSION)
                        };
                cursor =
                        mTelephonyAnalyticsUtil.getCursor(
                                SmsMmsAnalyticsTable.TABLE_NAME,
                                SMS_MMS_INSERTION_PROJECTION,
                                SMS_MMS_INSERTION_FAILURE_SELECTION,
                                selectionArgs,
                                null,
                                null,
                                null,
                                null);
            }
            updateIfEntryExistsOtherwiseInsert(cursor, values);
            deleteOldAndOverflowData();
        } catch (Exception e) {
            Rlog.e(TAG, "Exception during Sms/Mms Insertion [insertDataToDb()] " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    /**
     * Checks if a similar entry exists and updates the existing entry. Otherwise, inserts a new
     * entry.
     */
    @VisibleForTesting
    public void updateIfEntryExistsOtherwiseInsert(Cursor cursor, ContentValues values) {
        if (cursor != null && cursor.moveToFirst()) {
            int idColumnIndex = cursor.getColumnIndex(SmsMmsAnalyticsTable._ID);
            int countColumnIndex = cursor.getColumnIndex(SmsMmsAnalyticsTable.COUNT);
            if (idColumnIndex != -1 && countColumnIndex != -1) {
                int id = cursor.getInt(idColumnIndex);
                int count = cursor.getInt(countColumnIndex);
                int newCount = count + 1;

                values.put(SmsMmsAnalyticsTable.COUNT, newCount);

                String updateSelection = SmsMmsAnalyticsTable._ID + " = ? ";
                String[] updateSelectionArgs = {String.valueOf(id)};
                Rlog.d(TAG, "Updated Count = " + values.getAsString(SmsMmsAnalyticsTable.COUNT));

                mTelephonyAnalyticsUtil.update(
                        SmsMmsAnalyticsTable.TABLE_NAME,
                        values,
                        updateSelection,
                        updateSelectionArgs);
            }
        } else {
            mTelephonyAnalyticsUtil.insert(SmsMmsAnalyticsTable.TABLE_NAME, values);
        }
    }

    /** Gets the count from the cursor */
    @VisibleForTesting
    public long getCount(String[] columns, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        long count = 0;
        try {
            cursor =
                    mTelephonyAnalyticsUtil.getCursor(
                            SmsMmsAnalyticsTable.TABLE_NAME,
                            columns,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            null,
                            null);
            count = mTelephonyAnalyticsUtil.getCountFromCursor(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    private long getSmsMmsOfTypeAndStatus(String smsMmsType, String smsMmsStatus) {
        String[] columns = {"SUM(" + SmsMmsAnalyticsTable.COUNT + ")"};
        String selection = SmsMmsAnalyticsTable.SLOT_ID + " = ? ";
        String[] selectionArgs = {Integer.toString(mSlotIndex)};

        if (smsMmsType != null && smsMmsStatus != null) {
            selection =
                    SmsMmsAnalyticsTable.SMS_MMS_TYPE
                            + " = ? AND "
                            + SmsMmsAnalyticsTable.SMS_MMS_STATUS
                            + " = ? AND "
                            + SmsMmsAnalyticsTable.SLOT_ID
                            + " = ? ";
            selectionArgs = new String[] {smsMmsType, smsMmsStatus, Integer.toString(mSlotIndex)};
        } else if (smsMmsType != null) {
            selection =
                    SmsMmsAnalyticsTable.SMS_MMS_TYPE
                            + " = ? AND "
                            + SmsMmsAnalyticsTable.SLOT_ID
                            + " = ? ";
            selectionArgs = new String[] {smsMmsType, Integer.toString(mSlotIndex)};
        } else if (smsMmsStatus != null) {
            selection =
                    SmsMmsAnalyticsTable.SMS_MMS_STATUS
                            + " = ? AND "
                            + SmsMmsAnalyticsTable.SLOT_ID
                            + " = ? ";
            selectionArgs = new String[] {smsMmsStatus, Integer.toString(mSlotIndex)};
        }
        return getCount(columns, selection, selectionArgs);
    }

    private long getSmsOutgoingCount() {
        return getSmsMmsOfTypeAndStatus(SmsMmsType.SMS_OUTGOING.value, null);
    }

    private long getSmsIncomingCount() {
        return getSmsMmsOfTypeAndStatus(SmsMmsType.SMS_INCOMING.value, null);
    }

    private long getMmsOutgoingCount() {
        return getSmsMmsOfTypeAndStatus(SmsMmsType.MMS_OUTGOING.value, null);
    }

    private long getMmsIncomingCount() {
        return getSmsMmsOfTypeAndStatus(SmsMmsType.MMS_INCOMING.value, null);
    }

    private long getFailedOutgoingSms() {
        return getSmsMmsOfTypeAndStatus(SmsMmsType.SMS_OUTGOING.value, SmsMmsStatus.FAILURE.value);
    }

    private long getFailedIncomingSms() {
        return getSmsMmsOfTypeAndStatus(SmsMmsType.SMS_INCOMING.value, SmsMmsStatus.FAILURE.value);
    }

    private long getFailedOutgoingMms() {
        return getSmsMmsOfTypeAndStatus(SmsMmsType.MMS_OUTGOING.value, SmsMmsStatus.FAILURE.value);
    }

    private long getFailedIncomingMms() {
        return getSmsMmsOfTypeAndStatus(SmsMmsType.MMS_INCOMING.value, SmsMmsStatus.FAILURE.value);
    }

    private HashMap<String, Integer> getSmsFailedCountByRat() {
        HashMap<String, Integer> failedSmsTypeCountByRat = new HashMap<>();
        String[] columns = {
                SmsMmsAnalyticsTable.RAT, "SUM(" + SmsMmsAnalyticsTable.COUNT + ") as count "
        };
        String selection =
                SmsMmsAnalyticsTable.SLOT_ID
                        + " = ? AND "
                        + SmsMmsAnalyticsTable.SMS_MMS_STATUS
                        + " = ? AND "
                        + SmsMmsAnalyticsTable.SMS_MMS_TYPE
                        + " IN ('"
                        + SmsMmsType.SMS_INCOMING.value
                        + "', '"
                        + SmsMmsType.SMS_OUTGOING.value
                        + "' ) ";
        String[] selectionArgs = {Integer.toString(mSlotIndex), SmsMmsStatus.FAILURE.value};
        String groupBy = SmsMmsAnalyticsTable.RAT;
        Cursor cursor = null;
        try {
            cursor =
                    mTelephonyAnalyticsUtil.getCursor(
                            SmsMmsAnalyticsTable.TABLE_NAME,
                            columns,
                            selection,
                            selectionArgs,
                            groupBy,
                            null,
                            null,
                            null);
            if (cursor != null) {
                int ratIndex = cursor.getColumnIndex(SmsMmsAnalyticsTable.RAT);
                int countIndex = cursor.getColumnIndex("count");

                if (ratIndex != -1 && countIndex != -1) {
                    while (cursor.moveToNext()) {
                        String rat = cursor.getString(ratIndex);
                        int count = cursor.getInt(countIndex);
                        failedSmsTypeCountByRat.put(rat, count);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return failedSmsTypeCountByRat;
    }

    protected void deleteOldAndOverflowData() {
        String dateToday = DATE_FORMAT.format(Calendar.getInstance().toInstant());
        if (mDateOfDeletedRecordsSmsMmsTable == null
                || !mDateOfDeletedRecordsSmsMmsTable.equals(dateToday)) {
            mTelephonyAnalyticsUtil.deleteOverflowAndOldData(
                    SmsMmsAnalyticsTable.TABLE_NAME,
                    SMS_MMS_OVERFLOW_DATA_DELETION_SELECTION,
                    SMS_MMS_OLD_DATA_DELETION_SELECTION);
            mDateOfDeletedRecordsSmsMmsTable = dateToday;
        }
    }

    /** Setter function for mDateOfDeletedRecordsSmsMmsTable for testing purposes */
    public void setDateOfDeletedRecordsSmsMmsTable(String deletedDate) {
        mDateOfDeletedRecordsSmsMmsTable = deletedDate;
    }

    /**
     * Sets all the received information in a list along with the associated String description.
     *
     * @return A list of all the information with their respective description.
     */
    @VisibleForTesting
    public ArrayList<String> dumpInformationInList(
            long totalOutgoingSms,
            long totalIncomingSms,
            long failedOutgoingSms,
            long failedIncomingSms,
            long totalOutgoingMms,
            long totalIncomingMms,
            double percentageFailureOutgoingSms,
            double percentageFailureIncomingSms,
            double percentageFailureOutgoingMms,
            double percentageFailureIncomingMms,
            HashMap<String, Integer> failedSmsTypeCountByRat) {

        ArrayList<String> aggregatedSmsMmsInformation = new ArrayList<>();
        aggregatedSmsMmsInformation.add("Total Outgoing Sms Count = " + totalOutgoingSms);
        aggregatedSmsMmsInformation.add("Total Incoming Sms Count = " + totalIncomingSms);
        aggregatedSmsMmsInformation.add(
                "Failed Outgoing SMS Count = "
                        + failedOutgoingSms
                        + " Percentage failure rate for Outgoing SMS :"
                        + DECIMAL_FORMAT.format(percentageFailureOutgoingSms)
                        + "%");
        aggregatedSmsMmsInformation.add(
                "Failed Incoming SMS Count = "
                        + failedIncomingSms
                        + " Percentage failure rate for Incoming SMS :"
                        + DECIMAL_FORMAT.format(percentageFailureIncomingSms)
                        + "%");

        double overallFailPercentage =
                (double) (failedIncomingSms + failedOutgoingSms)
                        / (double) (totalIncomingSms + totalOutgoingSms)
                        * 100.0;
        aggregatedSmsMmsInformation.add(
                "Overall Fail Percentage = " + DECIMAL_FORMAT.format(overallFailPercentage) + "%");
        try {
            failedSmsTypeCountByRat.forEach(
                    (k, v) -> {
                        double percentageFail =
                                ((double) v
                                        / (double) (totalIncomingSms + totalOutgoingSms)
                                        * 100.0);
                        aggregatedSmsMmsInformation.add(
                                "Failed SMS Count for RAT : "
                                        + k
                                        + " = "
                                        + v
                                        + ", Percentage = "
                                        + DECIMAL_FORMAT.format(percentageFail)
                                        + "%");
                    });

        } catch (Exception e) {
            Rlog.d(TAG, "Exception in adding to List = " + e);
        }

        return aggregatedSmsMmsInformation;
    }

    /**
     * Gathers all the necessary information for the report by using specific methods.
     *
     * @return List of SmsMms analytics information.
     */
    public ArrayList<String> aggregate() {
        long totalOutgoingSms = getSmsOutgoingCount();
        long totalIncomingSms = getSmsIncomingCount();
        long totalOutgoingMms = getMmsOutgoingCount();
        long totalIncomingMms = getMmsIncomingCount();
        long totalFailedOutgoingSms = getFailedOutgoingSms();
        long totalFailedIncomingSms = getFailedIncomingSms();
        long totalFailedOutgoingMms = getFailedOutgoingMms();
        long totalFailedIncomingMms = getFailedIncomingMms();
        HashMap<String, Integer> failedSmsTypeCountByRat = getSmsFailedCountByRat();

        double percentageFailureOutgoingSms =
                totalOutgoingSms != 0
                        ? (double) totalFailedOutgoingSms / totalOutgoingSms * 100.0
                        : 0;
        double percentageFailureIncomingSms =
                totalIncomingSms != 0
                        ? (double) totalFailedIncomingSms / totalIncomingSms * 100.0
                        : 0;

        double percentageFailureOutgoingMms =
                totalOutgoingMms != 0
                        ? (double) totalFailedOutgoingMms / totalOutgoingMms * 100.0
                        : 0;
        double percentageFailureIncomingMms =
                totalIncomingMms != 0
                        ? (double) totalFailedIncomingMms / totalIncomingMms * 100.0
                        : 0;

        return dumpInformationInList(
                totalOutgoingSms,
                totalIncomingSms,
                totalFailedOutgoingSms,
                totalFailedIncomingSms,
                totalOutgoingMms,
                totalIncomingMms,
                percentageFailureOutgoingSms,
                percentageFailureIncomingSms,
                percentageFailureOutgoingMms,
                percentageFailureIncomingMms,
                failedSmsTypeCountByRat);
    }
}

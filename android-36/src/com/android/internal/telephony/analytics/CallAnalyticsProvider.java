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
import com.android.internal.telephony.analytics.TelephonyAnalyticsDatabase.CallAnalyticsTable;
import com.android.telephony.Rlog;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Provider class for calls, receives the data from CallAnalytics and performs business logic on the
 * received data. It uses util class for required db operation. This class implements the
 * TelephonyAnalyticsProvider interface to provide aggregation functionality.
 */
public class CallAnalyticsProvider implements TelephonyAnalyticsProvider {
    private static final String TAG = CallAnalyticsProvider.class.getSimpleName();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    private TelephonyAnalyticsUtil mTelephonyAnalyticsUtil;
    private String mDateOfDeletedRecordsCallTable;
    private static final String CREATE_CALL_ANALYTICS_TABLE =
            "CREATE TABLE IF NOT EXISTS "
                    + CallAnalyticsTable.TABLE_NAME
                    + "("
                    + CallAnalyticsTable._ID
                    + " INTEGER PRIMARY KEY,"
                    + CallAnalyticsTable.LOG_DATE
                    + " DATE ,"
                    + CallAnalyticsTable.CALL_STATUS
                    + " TEXT DEFAULT '',"
                    + CallAnalyticsTable.CALL_TYPE
                    + " TEXT DEFAULT '',"
                    + CallAnalyticsTable.RAT
                    + " TEXT DEFAULT '',"
                    + CallAnalyticsTable.SLOT_ID
                    + " INTEGER ,"
                    + CallAnalyticsTable.FAILURE_REASON
                    + " TEXT DEFAULT '',"
                    + CallAnalyticsTable.RELEASE_VERSION
                    + " TEXT DEFAULT '' , "
                    + CallAnalyticsTable.COUNT
                    + " INTEGER DEFAULT 1 "
                    + ");";

    private static final String[] CALL_INSERTION_PROJECTION = {
        CallAnalyticsTable._ID, CallAnalyticsTable.COUNT
    };

    private static final String CALL_SUCCESS_INSERTION_SELECTION =
            CallAnalyticsTable.CALL_TYPE
                    + " = ? AND "
                    + CallAnalyticsTable.LOG_DATE
                    + " = ? AND "
                    + CallAnalyticsTable.CALL_STATUS
                    + " = ? AND "
                    + CallAnalyticsTable.SLOT_ID
                    + " = ? ";

    private static final String CALL_FAILED_INSERTION_SELECTION =
            CallAnalyticsTable.LOG_DATE
                    + " = ? AND "
                    + CallAnalyticsTable.CALL_STATUS
                    + " = ? AND "
                    + CallAnalyticsTable.CALL_TYPE
                    + " = ? AND "
                    + CallAnalyticsTable.SLOT_ID
                    + " = ? AND "
                    + CallAnalyticsTable.RAT
                    + " = ? AND "
                    + CallAnalyticsTable.FAILURE_REASON
                    + " = ? AND "
                    + CallAnalyticsTable.RELEASE_VERSION
                    + " = ? ";

    private static final String CALL_OLD_DATA_DELETION_SELECTION =
            CallAnalyticsTable.LOG_DATE + " < ? ";

    private static final String CALL_OVERFLOW_DATA_DELETION_SELECTION =
            CallAnalyticsTable._ID
                    + " IN "
                    + " ( SELECT "
                    + CallAnalyticsTable._ID
                    + " FROM "
                    + CallAnalyticsTable.TABLE_NAME
                    + " ORDER BY "
                    + CallAnalyticsTable.LOG_DATE
                    + " DESC LIMIT -1 OFFSET ? )";

    private enum CallStatus {
        SUCCESS("Success"),
        FAILURE("Failure");
        public String value;

        CallStatus(String value) {
            this.value = value;
        }
    }

    private enum CallType {
        NORMAL("Normal Call"),
        SOS("SOS Call");
        public String value;

        CallType(String value) {
            this.value = value;
        }
    }

    private final int mSlotIndex;

    /**
     * Initializes the CallAnalyticsProvider object and creates a table in the DB to log the
     * information related to Calls.
     *
     * @param telephonyAnalyticsUtil : Util Class object to support db operations
     * @param slotIndex : Logical slot index.
     */
    public CallAnalyticsProvider(TelephonyAnalyticsUtil telephonyAnalyticsUtil, int slotIndex) {
        mTelephonyAnalyticsUtil = telephonyAnalyticsUtil;
        mSlotIndex = slotIndex;
        mTelephonyAnalyticsUtil.createTable(CREATE_CALL_ANALYTICS_TABLE);
    }

    private ContentValues getContentValues(
            String callType, String callStatus, int slotId, String rat, String failureReason) {
        ContentValues values = new ContentValues();
        String dateToday = DATE_FORMAT.format(Calendar.getInstance().toInstant());
        values.put(CallAnalyticsTable.LOG_DATE, dateToday);
        values.put(CallAnalyticsTable.CALL_TYPE, callType);
        values.put(CallAnalyticsTable.CALL_STATUS, callStatus);
        values.put(CallAnalyticsTable.SLOT_ID, slotId);
        values.put(CallAnalyticsTable.RAT, rat);
        values.put(CallAnalyticsTable.FAILURE_REASON, failureReason);
        values.put(CallAnalyticsTable.RELEASE_VERSION, INCREMENTAL);
        return values;
    }

    private String[] getSuccessfulCallSelectionArgs(ContentValues values) {
        return new String[] {
            values.getAsString(CallAnalyticsTable.CALL_TYPE),
            values.getAsString(CallAnalyticsTable.LOG_DATE),
            CallStatus.SUCCESS.value,
            values.getAsString(CallAnalyticsTable.SLOT_ID)
        };
    }

    private String[] getFailedCallSelectionArgs(ContentValues values) {

        return new String[] {
            values.getAsString(CallAnalyticsTable.LOG_DATE),
            values.getAsString(CallAnalyticsTable.CALL_STATUS),
            values.getAsString(CallAnalyticsTable.CALL_TYPE),
            values.getAsString(CallAnalyticsTable.SLOT_ID),
            values.getAsString(CallAnalyticsTable.RAT),
            values.getAsString(CallAnalyticsTable.FAILURE_REASON),
            values.getAsString(CallAnalyticsTable.RELEASE_VERSION)
        };
    }

    /**
     * Receives data, processes it and sends for insertion to db.
     *
     * @param callType : Type of the Call , i.e. Normal or Sos
     * @param callStatus : Defines call was success or failure
     * @param slotId : Logical Slot index derived from Phone object.
     * @param rat : Radio Access Technology on which call ended.
     * @param failureReason : Failure Reason of the call.
     */
    public void insertDataToDb(
            String callType, String callStatus, int slotId, String rat, String failureReason) {
        ContentValues values = getContentValues(callType, callStatus, slotId, rat, failureReason);
        Cursor cursor = null;
        try {
            if (values.getAsString(CallAnalyticsTable.CALL_STATUS)
                    .equals(CallStatus.SUCCESS.value)) {
                Rlog.d(TAG, "Insertion for Success Call");
                String[] selectionArgs = getSuccessfulCallSelectionArgs(values);
                cursor =
                        mTelephonyAnalyticsUtil.getCursor(
                                CallAnalyticsTable.TABLE_NAME,
                                CALL_INSERTION_PROJECTION,
                                CALL_SUCCESS_INSERTION_SELECTION,
                                selectionArgs,
                                null,
                                null,
                                null,
                                null);
            } else {

                String[] selectionArgs = getFailedCallSelectionArgs(values);
                cursor =
                        mTelephonyAnalyticsUtil.getCursor(
                                CallAnalyticsTable.TABLE_NAME,
                                CALL_INSERTION_PROJECTION,
                                CALL_FAILED_INSERTION_SELECTION,
                                selectionArgs,
                                null,
                                null,
                                null,
                                null);
            }
            updateEntryIfExistsOrInsert(cursor, values);
            deleteOldAndOverflowData();
        } catch (Exception e) {
            Rlog.e(TAG, "Error caught in insertDataToDb while insertion.");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void updateEntryIfExistsOrInsert(Cursor cursor, ContentValues values) {
        if (cursor != null && cursor.moveToFirst()) {
            int idColumnIndex = cursor.getColumnIndex(CallAnalyticsTable._ID);
            int countColumnIndex = cursor.getColumnIndex(CallAnalyticsTable.COUNT);
            if (idColumnIndex != -1 && countColumnIndex != -1) {
                int id = cursor.getInt(idColumnIndex);
                int count = cursor.getInt(countColumnIndex);
                int newCount = count + 1;

                values.put(CallAnalyticsTable.COUNT, newCount);

                String updateSelection = CallAnalyticsTable._ID + " = ? ";
                String[] updateSelectionArgs = {String.valueOf(id)};
                Rlog.d(TAG, "Updated Count = " + values.getAsString(CallAnalyticsTable.COUNT));

                mTelephonyAnalyticsUtil.update(
                        CallAnalyticsTable.TABLE_NAME,
                        values,
                        updateSelection,
                        updateSelectionArgs);
            }
        } else {
            Rlog.d(TAG, "Simple Insertion");
            mTelephonyAnalyticsUtil.insert(CallAnalyticsTable.TABLE_NAME, values);
        }
    }

    /** Gets the count stored in the cursor object. */
    @VisibleForTesting
    public long getCount(
            String tableName,
            String[] columns,
            String selection,
            String[] selectionArgs,
            String groupBy,
            String having,
            String orderBy,
            String limit) {
        Cursor cursor = null;
        long totalCount = 0;
        try {
            cursor =
                    mTelephonyAnalyticsUtil.getCursor(
                            CallAnalyticsTable.TABLE_NAME,
                            columns,
                            selection,
                            selectionArgs,
                            groupBy,
                            having,
                            orderBy,
                            limit);
            totalCount = mTelephonyAnalyticsUtil.getCountFromCursor(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return totalCount;
    }

    private String[] getColumnSelectionAndArgs(String callType, String callStatus) {
        String selection;
        String[] selectionAndArgs;
        if (callType != null) {
            if (callStatus != null) {
                selection =
                        CallAnalyticsTable.CALL_TYPE
                                + " = ? AND "
                                + CallAnalyticsTable.CALL_STATUS
                                + " = ? AND "
                                + CallAnalyticsTable.SLOT_ID
                                + " = ? ";
                selectionAndArgs =
                        new String[] {
                            selection, callType, callStatus, Integer.toString(mSlotIndex)
                        };
            } else {
                selection =
                        CallAnalyticsTable.CALL_TYPE
                                + " = ? AND "
                                + CallAnalyticsTable.SLOT_ID
                                + " = ? ";
                selectionAndArgs = new String[] {selection, callType, Integer.toString(mSlotIndex)};
            }
        } else {
            if (callStatus != null) {
                selection =
                        CallAnalyticsTable.CALL_STATUS
                                + " = ? AND "
                                + CallAnalyticsTable.SLOT_ID
                                + " = ? ";
                selectionAndArgs =
                        new String[] {selection, callStatus, Integer.toString(mSlotIndex)};
            } else {
                selection = CallAnalyticsTable.SLOT_ID + " = ? ";
                selectionAndArgs = new String[] {selection, Integer.toString(mSlotIndex)};
            }
        }
        return selectionAndArgs;
    }

    private long countCallsOfTypeAndStatus(String callType, String callStatus) {
        int selectionIndex = 0;
        String[] columns = {"sum(" + CallAnalyticsTable.COUNT + ")"};
        String[] selectionAndArgs = getColumnSelectionAndArgs(callType, callStatus);
        String selection = selectionAndArgs[selectionIndex];
        int selectionArgsStartIndex = 1, selectionArgsEndIndex = selectionAndArgs.length;
        String[] selectionArgs =
                Arrays.copyOfRange(
                        selectionAndArgs, selectionArgsStartIndex, selectionArgsEndIndex);
        return getCount(
                CallAnalyticsTable.TABLE_NAME,
                columns,
                selection,
                selectionArgs,
                null,
                null,
                null,
                null);
    }

    private long countTotalCalls() {
        return countCallsOfTypeAndStatus(null, null);
    }

    private long countFailedCalls() {
        return countCallsOfTypeAndStatus(null, CallStatus.FAILURE.value);
    }

    private long countNormalCalls() {
        return countCallsOfTypeAndStatus(CallType.NORMAL.value, null);
    }

    private long countFailedNormalCalls() {
        return countCallsOfTypeAndStatus(CallType.NORMAL.value, CallStatus.FAILURE.value);
    }

    private long countSosCalls() {
        return countCallsOfTypeAndStatus(CallType.SOS.value, null);
    }

    private long countFailedSosCalls() {
        return countCallsOfTypeAndStatus(CallType.SOS.value, CallStatus.FAILURE.value);
    }

    private String getMaxFailureVersion() {
        String[] columns = {CallAnalyticsTable.RELEASE_VERSION};
        String selection =
                CallAnalyticsTable.CALL_STATUS + " = ? AND " + CallAnalyticsTable.SLOT_ID + " = ? ";
        String[] selectionArgs = {CallStatus.FAILURE.value, Integer.toString(mSlotIndex)};
        String groupBy = CallAnalyticsTable.RELEASE_VERSION;
        String orderBy = "SUM(" + CallAnalyticsTable.COUNT + ") DESC ";
        String limit = "1";
        Cursor cursor = null;
        String version = "";
        try {
            cursor =
                    mTelephonyAnalyticsUtil.getCursor(
                            CallAnalyticsTable.TABLE_NAME,
                            columns,
                            selection,
                            selectionArgs,
                            groupBy,
                            null,
                            orderBy,
                            limit);

            if (cursor != null && cursor.moveToFirst()) {
                int releaseVersionColumnIndex =
                        cursor.getColumnIndex(CallAnalyticsTable.RELEASE_VERSION);
                if (releaseVersionColumnIndex != -1) {
                    version = cursor.getString(releaseVersionColumnIndex);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return version;
    }

    private String getMaxFailureNetworkType() {
        String[] columns = {CallAnalyticsTable.RAT};
        String selection =
                CallAnalyticsTable.CALL_STATUS + " = ? AND " + CallAnalyticsTable.SLOT_ID + " = ? ";
        String[] selectionArgs = {CallStatus.FAILURE.value, Integer.toString(mSlotIndex)};
        String groupBy = CallAnalyticsTable.RAT;
        String orderBy = "SUM(" + CallAnalyticsTable.COUNT + ") DESC ";
        String limit = "1";
        Cursor cursor = null;
        String networkType = "";

        try {
            cursor =
                    mTelephonyAnalyticsUtil.getCursor(
                            CallAnalyticsTable.TABLE_NAME,
                            columns,
                            selection,
                            selectionArgs,
                            groupBy,
                            null,
                            orderBy,
                            limit);

            if (cursor != null && cursor.moveToFirst()) {
                int networkColumnIndex = cursor.getColumnIndex(CallAnalyticsTable.RAT);
                networkType = cursor.getString(networkColumnIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return networkType;
    }

    private HashMap<String, Integer> getFailureCountByRatForCallType(String callType) {
        return getFailureCountByColumnForCallType(CallAnalyticsTable.RAT, callType);
    }

    private HashMap<String, Integer> getFailureCountByReasonForCallType(String callType) {
        return getFailureCountByColumnForCallType(CallAnalyticsTable.FAILURE_REASON, callType);
    }

    private HashMap<String, Integer> getFailureCountByColumnForCallType(
            String column, String callType) {
        String[] columns = {column, "SUM(" + CallAnalyticsTable.COUNT + ") AS count"};
        String selection =
                CallAnalyticsTable.CALL_TYPE
                        + " = ? AND "
                        + CallAnalyticsTable.CALL_STATUS
                        + " = ? AND "
                        + CallAnalyticsTable.SLOT_ID
                        + " = ? ";
        String[] selectionArgs = {callType, CallStatus.FAILURE.value, Integer.toString(mSlotIndex)};
        String groupBy = column;
        Cursor cursor = null;
        HashMap<String, Integer> failureCountByReason = new HashMap<>();

        try {
            cursor =
                    mTelephonyAnalyticsUtil.getCursor(
                            CallAnalyticsTable.TABLE_NAME,
                            columns,
                            selection,
                            selectionArgs,
                            groupBy,
                            null,
                            null,
                            null);

            if (cursor != null) {
                int failureColumnIndex = cursor.getColumnIndex(column);
                int failureCountColumnIndex = cursor.getColumnIndex("count");

                if (failureCountColumnIndex != -1 && failureColumnIndex != -1) {
                    while (cursor.moveToNext()) {
                        String failureReason = cursor.getString(failureColumnIndex);
                        int failureCount = cursor.getInt(failureCountColumnIndex);
                        failureCountByReason.put(failureReason, failureCount);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return failureCountByReason;
    }

    protected void deleteOldAndOverflowData() {
        String dateToday = DATE_FORMAT.format(Calendar.getInstance().toInstant());
        if (mDateOfDeletedRecordsCallTable == null
                || !mDateOfDeletedRecordsCallTable.equals(dateToday)) {
            mTelephonyAnalyticsUtil.deleteOverflowAndOldData(
                    CallAnalyticsTable.TABLE_NAME,
                    CALL_OVERFLOW_DATA_DELETION_SELECTION,
                    CALL_OLD_DATA_DELETION_SELECTION);
            mDateOfDeletedRecordsCallTable = dateToday;
        }
    }

    /** Setter function for mDateOfDeletedRecordsCallTable. */
    public void setDateOfDeletedRecordsCallTable(String dateOfDeletedRecordsCallTable) {
        mDateOfDeletedRecordsCallTable = dateOfDeletedRecordsCallTable;
    }

    private ArrayList<String> dumpInformationInList(
            long totalCalls,
            long failedCalls,
            double percentageFailedCalls,
            long normalCallsCount,
            double percentageFailedNormalCalls,
            long sosCallCount,
            double percentageFailedSosCalls,
            String maxFailureVersion,
            String maxFailureNetworkType,
            HashMap<String, Integer> failureCountByReasonNormalCall,
            HashMap<String, Integer> failureCountByReasonSosCall,
            HashMap<String, Integer> failureCountByRatNormalCall,
            HashMap<String, Integer> failureCountByRatSosCall) {

        ArrayList<String> aggregatedCallInformation = new ArrayList<>();
        aggregatedCallInformation.add("Normal Call Stats");
        aggregatedCallInformation.add("\tTotal Normal Calls = " + normalCallsCount);
        aggregatedCallInformation.add(
                "\tPercentage Failure of Normal Calls = "
                        + DECIMAL_FORMAT.format(percentageFailedNormalCalls)
                        + "%");
        addFailureStatsFromHashMap(
                failureCountByReasonNormalCall,
                CallType.NORMAL.value,
                normalCallsCount,
                CallAnalyticsTable.FAILURE_REASON,
                aggregatedCallInformation);
        addFailureStatsFromHashMap(
                failureCountByRatNormalCall,
                CallType.NORMAL.value,
                normalCallsCount,
                CallAnalyticsTable.RAT,
                aggregatedCallInformation);

        if (sosCallCount > 0) {
            aggregatedCallInformation.add("SOS Call Stats");
            aggregatedCallInformation.add("\tTotal SOS Calls = " + sosCallCount);
            aggregatedCallInformation.add(
                    "\tPercentage Failure of SOS Calls = "
                            + DECIMAL_FORMAT.format(percentageFailedSosCalls)
                            + "%");
            addFailureStatsFromHashMap(
                    failureCountByReasonSosCall,
                    CallType.SOS.value,
                    sosCallCount,
                    CallAnalyticsTable.FAILURE_REASON,
                    aggregatedCallInformation);
            addFailureStatsFromHashMap(
                    failureCountByRatSosCall,
                    CallType.SOS.value,
                    sosCallCount,
                    CallAnalyticsTable.RAT,
                    aggregatedCallInformation);
        }
        if (failedCalls != 0) {
            aggregatedCallInformation.add(
                    "\tMax Call(Normal+SOS) Failures at Version : " + maxFailureVersion);
            aggregatedCallInformation.add(
                    "\tMax Call(Normal+SOS) Failures at Network Type: " + maxFailureNetworkType);
        }

        return aggregatedCallInformation;
    }

    private void addFailureStatsFromHashMap(
            HashMap<String, Integer> failureCountByColumn,
            String callType,
            long totalCallsOfCallType,
            String column,
            ArrayList<String> aggregatedCallInformation) {
        failureCountByColumn.forEach(
                (k, v) -> {
                    double percentageFail = (double) v / (double) totalCallsOfCallType * 100.0;
                    aggregatedCallInformation.add(
                            "\tNo. of "
                                    + callType
                                    + " failures at "
                                    + column
                                    + " : "
                                    + k
                                    + " = "
                                    + v
                                    + ", Percentage = "
                                    + DECIMAL_FORMAT.format(percentageFail)
                                    + "%");
                });
    }

    /**
     * Collects all information which is intended to be a part of the report by calling the required
     * functions implemented in the class.
     *
     * @return List which contains all the Calls related information
     */
    public ArrayList<String> aggregate() {
        long totalCalls = countTotalCalls();
        long failedCalls = countFailedCalls();
        double percentageFailedCalls = (double) failedCalls / (double) totalCalls * 100.0;
        String maxFailuresVersion = getMaxFailureVersion();
        String maxFailuresNetworkType = getMaxFailureNetworkType();
        HashMap<String, Integer> failureCountByReasonNormalCall =
                getFailureCountByReasonForCallType(CallType.NORMAL.value);
        HashMap<String, Integer> failureCountByReasonSosCall =
                getFailureCountByReasonForCallType(CallType.SOS.value);
        HashMap<String, Integer> failureCountByRatNormalCall =
                getFailureCountByRatForCallType(CallType.NORMAL.value);
        HashMap<String, Integer> failureCountByRatSosCall =
                getFailureCountByRatForCallType(CallType.SOS.value);
        long normalCallsCount = countNormalCalls();
        long normalCallFailureCount = countFailedNormalCalls();
        double percentageFailureNormalCall =
                (double) normalCallFailureCount / (double) normalCallsCount * 100.0;
        long sosCallCount = countSosCalls();
        long sosCallFailureCount = countFailedSosCalls();
        double percentageFailureSosCall =
                (double) sosCallFailureCount / (double) sosCallCount * 100.0;
        ArrayList<String> aggregatedCallInformation =
                dumpInformationInList(
                        totalCalls,
                        failedCalls,
                        percentageFailedCalls,
                        normalCallsCount,
                        percentageFailureNormalCall,
                        sosCallCount,
                        percentageFailureSosCall,
                        maxFailuresVersion,
                        maxFailuresNetworkType,
                        failureCountByReasonNormalCall,
                        failureCountByReasonSosCall,
                        failureCountByRatNormalCall,
                        failureCountByRatSosCall);
        return aggregatedCallInformation;
    }
}

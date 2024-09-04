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
import com.android.internal.telephony.analytics.TelephonyAnalytics.ServiceStateAnalytics.TimeStampedServiceState;
import com.android.internal.telephony.analytics.TelephonyAnalyticsDatabase.ServiceStateAnalyticsTable;
import com.android.telephony.Rlog;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Provider class for ServiceState. Receives the data from ServiceStateAnalytics. Performs business
 * logic on the received data. Uses Util class for required db operation. Implements the
 * TelephonyAnalyticsProvider interface to provide aggregation functionality.
 */
public class ServiceStateAnalyticsProvider implements TelephonyAnalyticsProvider {
    protected TelephonyAnalyticsUtil mTelephonyAnalyticsUtil;
    private static final String TAG = ServiceStateAnalyticsProvider.class.getSimpleName();
    private static final String CREATE_SERVICE_STATE_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS "
                    + ServiceStateAnalyticsTable.TABLE_NAME
                    + " ( "
                    + ServiceStateAnalyticsTable._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ServiceStateAnalyticsTable.LOG_DATE
                    + " DATE ,"
                    + ServiceStateAnalyticsTable.SLOT_ID
                    + " INTEGER , "
                    + ServiceStateAnalyticsTable.TIME_DURATION
                    + " INTEGER ,"
                    + ServiceStateAnalyticsTable.RAT
                    + " TEXT ,"
                    + ServiceStateAnalyticsTable.DEVICE_STATUS
                    + " TEXT ,"
                    + ServiceStateAnalyticsTable.RELEASE_VERSION
                    + " TEXT "
                    + ");";

    private static final String[] SERVICE_STATE_INSERTION_COLUMNS = {
        ServiceStateAnalyticsTable._ID, ServiceStateAnalyticsTable.TIME_DURATION
    };

    private String mDateOfDeletedRecordsServiceStateTable;

    private static final String SERVICE_STATE_INSERTION_SELECTION =
            ServiceStateAnalyticsTable.LOG_DATE
                    + " = ? AND "
                    + ServiceStateAnalyticsTable.SLOT_ID
                    + " = ? AND "
                    + ServiceStateAnalyticsTable.RAT
                    + " = ? AND "
                    + ServiceStateAnalyticsTable.DEVICE_STATUS
                    + " = ? AND "
                    + ServiceStateAnalyticsTable.RELEASE_VERSION
                    + " = ? ";

    private static final String SERVICE_STATE_OVERFLOW_DATA_DELETION_SELECTION =
            ServiceStateAnalyticsTable._ID
                    + " IN "
                    + " ( SELECT "
                    + ServiceStateAnalyticsTable._ID
                    + " FROM "
                    + ServiceStateAnalyticsTable.TABLE_NAME
                    + " ORDER BY "
                    + ServiceStateAnalyticsTable.LOG_DATE
                    + " DESC LIMIT -1 OFFSET ? )";

    private static final String SERVICE_STATE_OLD_DATA_DELETION_SELECTION =
            ServiceStateAnalyticsTable.LOG_DATE + " < ? ";

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private final int mSlotIndex;

    /**
     * Instantiates the ServiceStateAnalyticsProvider Object. Creates a table in the db for Storing
     * ServiceState Related Information.
     */
    public ServiceStateAnalyticsProvider(TelephonyAnalyticsUtil databaseUtil, int slotIndex) {
        mTelephonyAnalyticsUtil = databaseUtil;
        mSlotIndex = slotIndex;
        mTelephonyAnalyticsUtil.createTable(CREATE_SERVICE_STATE_TABLE_QUERY);
    }

    private ContentValues getContentValues(
            TelephonyAnalytics.ServiceStateAnalytics.TimeStampedServiceState lastState,
            long endTimeStamp) {
        ContentValues values = new ContentValues();
        long timeInterval = endTimeStamp - lastState.mTimestampStart;
        String dateToday = DATE_FORMAT.format(Calendar.getInstance().toInstant());
        values.put(ServiceStateAnalyticsTable.LOG_DATE, dateToday);
        values.put(ServiceStateAnalyticsTable.TIME_DURATION, timeInterval);
        values.put(ServiceStateAnalyticsTable.SLOT_ID, lastState.mSlotIndex);
        values.put(ServiceStateAnalyticsTable.RAT, lastState.mRAT);
        values.put(ServiceStateAnalyticsTable.DEVICE_STATUS, lastState.mDeviceStatus);
        values.put(ServiceStateAnalyticsTable.RELEASE_VERSION, INCREMENTAL);

        return values;
    }

    /** Receives the data, processes it and sends it for insertion to db. */
    @VisibleForTesting
    public void insertDataToDb(TimeStampedServiceState lastState, long endTimeStamp) {
        ContentValues values = getContentValues(lastState, endTimeStamp);
        Rlog.d(TAG, "  " + values.toString() + "Time = " + System.currentTimeMillis());
        String[] selectionArgs = {
            values.getAsString(ServiceStateAnalyticsTable.LOG_DATE),
            values.getAsString(ServiceStateAnalyticsTable.SLOT_ID),
            values.getAsString(ServiceStateAnalyticsTable.RAT),
            values.getAsString(ServiceStateAnalyticsTable.DEVICE_STATUS),
            values.getAsString(ServiceStateAnalyticsTable.RELEASE_VERSION)
        };
        Cursor cursor = null;
        try {
            cursor =
                    mTelephonyAnalyticsUtil.getCursor(
                            ServiceStateAnalyticsTable.TABLE_NAME,
                            SERVICE_STATE_INSERTION_COLUMNS,
                            SERVICE_STATE_INSERTION_SELECTION,
                            selectionArgs,
                            null,
                            null,
                            null,
                            null);
            updateIfEntryExistsOtherwiseInsert(cursor, values);
            deleteOldAndOverflowData();

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void updateIfEntryExistsOtherwiseInsert(Cursor cursor, ContentValues values) {
        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(ServiceStateAnalyticsTable._ID);
            int timeDurationIndex = cursor.getColumnIndex(ServiceStateAnalyticsTable.TIME_DURATION);
            if (idIndex != -1 && timeDurationIndex != -1) {
                int id = cursor.getInt(idIndex);
                int oldTimeDuration = cursor.getInt(timeDurationIndex);
                int updatedTimeDuration =
                        oldTimeDuration
                                + Integer.parseInt(
                                        values.getAsString(
                                                ServiceStateAnalyticsTable.TIME_DURATION));
                String updateSelection = ServiceStateAnalyticsTable._ID + " = ? ";
                String[] updateSelectionArgs = {Integer.toString(id)};
                values.put(ServiceStateAnalyticsTable.TIME_DURATION, updatedTimeDuration);
                mTelephonyAnalyticsUtil.update(
                        ServiceStateAnalyticsTable.TABLE_NAME,
                        values,
                        updateSelection,
                        updateSelectionArgs);
            }
        } else {
            mTelephonyAnalyticsUtil.insert(ServiceStateAnalyticsTable.TABLE_NAME, values);
        }
    }

    private long getTotalUpTime() {
        String[] columns = {"SUM(" + ServiceStateAnalyticsTable.TIME_DURATION + ")"};
        String selection = ServiceStateAnalyticsTable.SLOT_ID + " = ? ";
        String[] selectionArgs = {Integer.toString(mSlotIndex)};
        Cursor cursor = null;
        long duration = 0;

        try {
            cursor =
                    mTelephonyAnalyticsUtil.getCursor(
                            ServiceStateAnalyticsTable.TABLE_NAME,
                            columns,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            null,
                            null);
            if (cursor != null && cursor.moveToFirst()) {
                duration = cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return duration;
    }

    private long outOfServiceDuration() {
        String[] columns = {"SUM(" + ServiceStateAnalyticsTable.TIME_DURATION + ")"};
        String selection =
                ServiceStateAnalyticsTable.DEVICE_STATUS
                        + " != ? "
                        + " AND "
                        + ServiceStateAnalyticsTable.SLOT_ID
                        + " = ? ";
        String[] selectionArgs = {"IN_SERVICE", Integer.toString(mSlotIndex)};
        long oosDuration = 0;
        Cursor cursor = null;
        try {
            cursor =
                    mTelephonyAnalyticsUtil.getCursor(
                            ServiceStateAnalyticsTable.TABLE_NAME,
                            columns,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            null,
                            null);
            if (cursor != null && cursor.moveToFirst()) {
                oosDuration = cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return oosDuration;
    }

    private HashMap<String, Long> getOutOfServiceDurationByReason() {
        HashMap<String, Long> outOfServiceDurationByReason = new HashMap<>();
        String[] columns = {
            ServiceStateAnalyticsTable.DEVICE_STATUS,
            "SUM(" + ServiceStateAnalyticsTable.TIME_DURATION + ") AS totalTime"
        };
        String selection =
                ServiceStateAnalyticsTable.DEVICE_STATUS
                        + " != ? AND "
                        + ServiceStateAnalyticsTable.SLOT_ID
                        + " = ? ";
        String[] selectionArgs = {"IN_SERVICE", Integer.toString(mSlotIndex)};
        String groupBy = ServiceStateAnalyticsTable.DEVICE_STATUS;
        Cursor cursor = null;
        try {
            cursor =
                    mTelephonyAnalyticsUtil.getCursor(
                            ServiceStateAnalyticsTable.TABLE_NAME,
                            columns,
                            selection,
                            selectionArgs,
                            groupBy,
                            null,
                            null,
                            null);

            if (cursor != null) {
                int reasonIndex = cursor.getColumnIndex(ServiceStateAnalyticsTable.DEVICE_STATUS);
                int timeIndex = cursor.getColumnIndex("totalTime");

                if (reasonIndex != -1 && timeIndex != -1) {
                    while (cursor.moveToNext()) {
                        String oosReason = cursor.getString(reasonIndex);
                        long timeInterval = cursor.getLong(timeIndex);
                        outOfServiceDurationByReason.put(oosReason, timeInterval);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return outOfServiceDurationByReason;
    }

    private HashMap<String, Long> getInServiceDurationByRat() {
        HashMap<String, Long> inServiceDurationByRat = new HashMap<>();
        String[] columns = {
            ServiceStateAnalyticsTable.RAT,
            "SUM(" + ServiceStateAnalyticsTable.TIME_DURATION + ") AS totalTime"
        };
        String selection =
                ServiceStateAnalyticsTable.RAT
                        + " != ? AND "
                        + ServiceStateAnalyticsTable.SLOT_ID
                        + " = ? ";
        String[] selectionArgs = {"NA", Integer.toString(mSlotIndex)};
        String groupBy = ServiceStateAnalyticsTable.RAT;
        Cursor cursor = null;
        try {
            cursor =
                    mTelephonyAnalyticsUtil.getCursor(
                            ServiceStateAnalyticsTable.TABLE_NAME,
                            columns,
                            selection,
                            selectionArgs,
                            groupBy,
                            null,
                            null,
                            null);

            if (cursor != null) {
                int ratIndex = cursor.getColumnIndex(ServiceStateAnalyticsTable.RAT);
                int timeIndex = cursor.getColumnIndex("totalTime");

                if (ratIndex != -1 && timeIndex != -1) {
                    while (cursor.moveToNext()) {
                        String rat = cursor.getString(ratIndex);
                        long timeInterval = cursor.getLong(timeIndex);
                        inServiceDurationByRat.put(rat, timeInterval);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return inServiceDurationByRat;
    }

    protected void deleteOldAndOverflowData() {
        String dateToday = DATE_FORMAT.format(Calendar.getInstance().toInstant());
        if (mDateOfDeletedRecordsServiceStateTable == null
                || !mDateOfDeletedRecordsServiceStateTable.equals(dateToday)) {
            mTelephonyAnalyticsUtil.deleteOverflowAndOldData(
                    ServiceStateAnalyticsTable.TABLE_NAME,
                    SERVICE_STATE_OVERFLOW_DATA_DELETION_SELECTION,
                    SERVICE_STATE_OLD_DATA_DELETION_SELECTION);
            mDateOfDeletedRecordsServiceStateTable = dateToday;
        }
    }

    /** Setter function for mDateOfDeletedRecordsServiceStateTable */
    public void setDateOfDeletedRecordsServiceStateTable(
            String dateOfDeletedRecordsServiceStateTable) {
        mDateOfDeletedRecordsServiceStateTable = dateOfDeletedRecordsServiceStateTable;
    }

    private ArrayList<String> dumpInformationInList(
            Long upTime,
            Long outOfServiceTime,
            double percentageOutOfService,
            HashMap<String, Long> oosByReason,
            HashMap<String, Long> timeDurationByRat) {
        ArrayList<String> aggregatedServiceStateInfo = new ArrayList<>();
        aggregatedServiceStateInfo.add("Total UpTime = " + upTime + " millis");
        aggregatedServiceStateInfo.add(
                "Out of Service Time = "
                        + outOfServiceTime
                        + " millis, Percentage "
                        + DECIMAL_FORMAT.format(percentageOutOfService)
                        + "%");
        oosByReason.forEach(
                (k, v) -> {
                    double percentageOosOfReason;
                    if (upTime == 0) {
                        percentageOosOfReason = 0.0;
                    } else {
                        percentageOosOfReason = (double) v / (double) upTime * 100.0;
                    }
                    aggregatedServiceStateInfo.add(
                            "Out of service Reason = "
                                    + k
                                    + ", Percentage = "
                                    + DECIMAL_FORMAT.format(percentageOosOfReason)
                                    + "%");
                });
        timeDurationByRat.forEach(
                (k, v) -> {
                    double percentageInService;
                    if (upTime == 0) {
                        percentageInService = 0.0;
                    } else {
                        percentageInService = (double) v / (double) upTime * 100.0;
                    }
                    aggregatedServiceStateInfo.add(
                            "IN_SERVICE RAT : "
                                    + k
                                    + ", Percentage = "
                                    + DECIMAL_FORMAT.format(percentageInService)
                                    + "%");
                });
        return aggregatedServiceStateInfo;
    }

    /**
     * Collects all information which is intended to be a part of the report by calling the required
     * functions implemented in the class.
     *
     * @return List which contains all the ServiceState related collected information.
     */
    public ArrayList<String> aggregate() {

        long upTime = getTotalUpTime();
        long outOfServiceTime = outOfServiceDuration();
        double percentageOutOfService;
        if (upTime == 0) {
            percentageOutOfService = 0.0;
        } else {
            percentageOutOfService = (double) outOfServiceTime / upTime * 100.0;
        }
        HashMap<String, Long> oosByReason = getOutOfServiceDurationByReason();
        HashMap<String, Long> timeDurationByRat = getInServiceDurationByRat();
        return dumpInformationInList(
                upTime, outOfServiceTime, percentageOutOfService, oosByReason, timeDurationByRat);
    }
}

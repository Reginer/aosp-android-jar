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

import static com.android.internal.telephony.analytics.TelephonyAnalyticsDatabase.DATE_FORMAT;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.util.Calendar;

/**
 * Singleton Utility class to support TelephonyAnalytics Extends SQLiteOpenHelper class. Supports db
 * related operations which includes creating tables,insertion,updation,deletion. Supports some
 * generic functionality like getting the Cursor resulting from a query.
 */
public class TelephonyAnalyticsUtil extends SQLiteOpenHelper {
    private static TelephonyAnalyticsUtil sTelephonyAnalyticsUtil;
    private static final String DATABASE_NAME = "telephony_analytics.db";
    private static final int DATABASE_VERSION = 10;
    private static final String TAG = TelephonyAnalyticsUtil.class.getSimpleName();
    private static final int MAX_ENTRIES_LIMIT = 1000;
    private static final int CUTOFF_MONTHS = 2;

    private TelephonyAnalyticsUtil(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Get the instance of the TelephonyAnalyticsUtil class. Instantiates the TelephonyAnalyticsUtil
     * object sTelephonyAnalyticsUtil only once.
     *
     * @return Returns the TelephonyAnalyticsUtil object sTelephonyAnalyticsUtil.
     */
    public static synchronized TelephonyAnalyticsUtil getInstance(Context context) {
        if (sTelephonyAnalyticsUtil == null) {
            sTelephonyAnalyticsUtil = new TelephonyAnalyticsUtil(context);
        }
        return sTelephonyAnalyticsUtil;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    /**
     * Uses Util class functionality to create CallTable in db
     *
     * @param createTableQuery : CallTable Schema
     */
    @VisibleForTesting
    public synchronized void createTable(String createTableQuery) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL(createTableQuery);
        } catch (Exception e) {
            Rlog.e(TAG, "Error during table creation : " + e);
        }
    }

    /** Utility function that performs insertion on the given database table */
    @VisibleForTesting
    public synchronized void insert(String tableName, ContentValues values) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.insert(tableName, null, values);
        } catch (SQLException e) {
            Rlog.e(TAG, "error occurred during insertion");
        }
    }

    /** Utility function that performs update query on the given database table */
    @VisibleForTesting
    public synchronized int update(
            String table, ContentValues values, String whereClause, String[] whereArgs) {
        int rowsAffected = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            rowsAffected = db.update(table, values, whereClause, whereArgs);

        } catch (SQLException e) {
            Rlog.e(TAG, "Error during update.");
        }
        return rowsAffected;
    }

    /**
     * @Return the cursor object obtained from running a query based on given parameters.
     */
    @VisibleForTesting
    public synchronized Cursor getCursor(
            String tableName,
            String[] columns,
            String selection,
            String[] selectionArgs,
            String groupBy,
            String having,
            String orderBy,
            String limit) {

        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor =
                    db.query(
                            tableName,
                            columns,
                            selection,
                            selectionArgs,
                            groupBy,
                            having,
                            orderBy,
                            limit);
        } catch (SQLException e) {
            Rlog.e(TAG, "Error during querying for getCursor()" + e);
        }
        return cursor;
    }

    /** Returns the count stored in the cursor obtained from query execution. */
    @VisibleForTesting
    public synchronized long getCountFromCursor(Cursor cursor) {
        long count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        return count;
    }

    /** Deletes Old Data and Overflow data in the db. */
    @VisibleForTesting
    public void deleteOverflowAndOldData(
            String tableName, String overflowWhereClause, String oldDataWhereClause) {
        deleteOverFlowData(tableName, overflowWhereClause);
        deleteOldData(tableName, oldDataWhereClause);
    }

    protected void deleteOverFlowData(String tableName, String whereClause) {
        String[] whereArgs = {Integer.toString(MAX_ENTRIES_LIMIT)};
        delete(tableName, whereClause, whereArgs);
    }

    protected void deleteOldData(String tableName, String whereClause) {
        String[] whereArgs = {getCutoffDate()};
        delete(tableName, whereClause, whereArgs);
    }

    /** Utility function that performs deletion on the database table */
    @VisibleForTesting
    public synchronized void delete(String tableName, String whereClause, String[] whereArgs) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(tableName, whereClause, whereArgs);
        } catch (SQLException e) {
            Rlog.e(TAG, "Sqlite Operation Error during deletion of Overflow data " + e);
        }
    }

    private String getCutoffDate() {
        Calendar cutoffDate = Calendar.getInstance();
        cutoffDate.add(Calendar.MONTH, -1 * CUTOFF_MONTHS);
        return DATE_FORMAT.format(cutoffDate.toInstant());
    }
}

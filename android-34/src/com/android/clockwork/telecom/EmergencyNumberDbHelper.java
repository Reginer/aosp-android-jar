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

package com.android.clockwork.telecom;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.telephony.emergency.EmergencyNumber;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class EmergencyNumberDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "EmergencyNumberDatabase";
    private static final String TAG = "EmergencyNumberDbHelper";
    private static final int DB_VERSION = 1;

    private static final String SQL_CLEAR_EMERGENCY_NUMBER_TABLE =
            "DELETE FROM " + EmergencyNumberTable.TABLE_NAME;

    private static final String SQL_CREATE_EMERGENCY_NUMBER_TABLE =
            "CREATE TABLE "
                    + EmergencyNumberTable.TABLE_NAME
                    + " ("
                    + EmergencyNumberTable._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + EmergencyNumberTable.NUMBER
                    + " Text DEFAULT NULL, "
                    + EmergencyNumberTable.ISO
                    + " Text DEFAULT NULL, "
                    + EmergencyNumberTable.MNC
                    + " Text DEFAULT NULL, "
                    + EmergencyNumberTable.CATEGORY_MASK
                    + " INTEGER DEFAULT NULL, "
                    + EmergencyNumberTable.URNS
                    + " Text DEFAULT NULL, "
                    + EmergencyNumberTable.SOURCE_MASK
                    + " INTEGER DEFAULT NULL, "
                    + EmergencyNumberTable.CALL_ROUTING
                    + " INTEGER DEFAULT NULL, "
                    + EmergencyNumberTable.SUBSCRIPTION_ID
                    + " INTEGER DEFAULT NULL); ";

    private static final String SQL_QUERY_EMERGENCY_NUMBER_TABLE =
            "SELECT * FROM " + EmergencyNumberTable.TABLE_NAME;

    private static EmergencyNumberDbHelper sInstance;

    /** Gets a singleton instance of EmergencyNumberHelper. */
    public static synchronized EmergencyNumberDbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new EmergencyNumberDbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    EmergencyNumberDbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_EMERGENCY_NUMBER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No upgrade needed yet.
    }

    /** Writes the emergency number map into database. */
    public synchronized void updateEmergencyNumbers(
            @NonNull Map<Integer, List<EmergencyNumber>> numbers) {
        // clear the current table first
        cleanEmergencyNumberTable();
        for (int subId : numbers.keySet()) {
            List<EmergencyNumber> numberList = numbers.get(subId);
            if (numberList == null || numberList.isEmpty()) {
                continue;
            }
            SQLiteDatabase writableDatabase = null;
            try {
                writableDatabase = getWritableDatabase();
                writableDatabase.beginTransaction();
                for (EmergencyNumber currentNumber : numberList) {
                    writableDatabase.insertOrThrow(
                            EmergencyNumberTable.TABLE_NAME,
                            null,
                            getContentValuesFromEmergencyNumber(currentNumber, subId));
                }
                writableDatabase.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Failed to update emergency number database " + e);
            } finally {
                if (writableDatabase != null) {
                    writableDatabase.endTransaction();
                }
            }
        }
    }

    /** Fetches the emergency number map from the database. */
    public Map<Integer, List<EmergencyNumber>> fetchEmergencyNumbers() {
        Map<Integer, List<EmergencyNumber>> numbers = new HashMap<>();

        try {
            Cursor cursor = getReadableDatabase().rawQuery(SQL_QUERY_EMERGENCY_NUMBER_TABLE, null);
            if (cursor.moveToNext()) {
                Pair<Integer, EmergencyNumber> pair = cursorToEmergencyNumber(cursor);
                numbers.putIfAbsent(pair.first, new ArrayList<>());
                numbers.get(pair.first).add(pair.second);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch emergency numbers " + e);
        }
        return numbers;
    }

    private synchronized void cleanEmergencyNumberTable() {
        SQLiteDatabase writableDatabase = null;
        try {
            writableDatabase = getWritableDatabase();
            writableDatabase.beginTransaction();
            writableDatabase.execSQL(SQL_CLEAR_EMERGENCY_NUMBER_TABLE);
            writableDatabase.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e(TAG, "Failed to clean emergency number database " + e);
        } finally {
            if (writableDatabase != null) {
                writableDatabase.endTransaction();
            }
        }
    }

    private String getUrnsString(@NonNull List<String> urns) {
        StringBuilder builder = new StringBuilder();
        for (String urn : urns) {
            builder.append(urn);
            builder.append(",");
        }

        return builder.toString();
    }

    private List<String> getUrns(@NonNull String urns) {
        String[] urnArray = urns.split(",");
        return Arrays.asList(urnArray);
    }

    private Pair<Integer, EmergencyNumber> cursorToEmergencyNumber(Cursor cursor) {
        String number = cursor.getString(cursor.getColumnIndex(EmergencyNumberTable.NUMBER));
        String iso = cursor.getString(cursor.getColumnIndex(EmergencyNumberTable.ISO));
        String mnc = cursor.getString(cursor.getColumnIndex(EmergencyNumberTable.MNC));
        int categoryMask = cursor.getInt(cursor.getColumnIndex(EmergencyNumberTable.CATEGORY_MASK));
        String urnsString = cursor.getString(cursor.getColumnIndex(EmergencyNumberTable.URNS));
        int sourceMask = cursor.getInt(cursor.getColumnIndex(EmergencyNumberTable.SOURCE_MASK));
        int callRouting = cursor.getInt(cursor.getColumnIndex(EmergencyNumberTable.CALL_ROUTING));
        int subId = cursor.getInt(cursor.getColumnIndex(EmergencyNumberTable.SUBSCRIPTION_ID));
        EmergencyNumber emergencyNumber =
                new EmergencyNumber(
                        number,
                        iso,
                        mnc,
                        categoryMask,
                        getUrns(urnsString),
                        sourceMask,
                        callRouting);
        return new Pair<>(subId, emergencyNumber);
    }

    private ContentValues getContentValuesFromEmergencyNumber(
            @NonNull EmergencyNumber emergencyNumber, int subscriptionId) {
        ContentValues values = new ContentValues();
        values.put(EmergencyNumberTable.NUMBER, emergencyNumber.getNumber());
        values.put(EmergencyNumberTable.ISO, emergencyNumber.getCountryIso());
        values.put(EmergencyNumberTable.MNC, emergencyNumber.getMnc());
        values.put(
                EmergencyNumberTable.CATEGORY_MASK,
                emergencyNumber.getEmergencyServiceCategoryBitmask());
        values.put(EmergencyNumberTable.URNS, getUrnsString(emergencyNumber.getEmergencyUrns()));
        values.put(
                EmergencyNumberTable.SOURCE_MASK,
                emergencyNumber.getEmergencyNumberSourceBitmask());
        values.put(EmergencyNumberTable.CALL_ROUTING, emergencyNumber.getEmergencyCallRouting());
        values.put(EmergencyNumberTable.SUBSCRIPTION_ID, subscriptionId);
        return values;
    }

    public static class EmergencyNumberTable implements BaseColumns {
        public static final String TABLE_NAME = "emergency_number";
        public static final String NUMBER = "number";
        public static final String ISO = "iso";
        public static final String MNC = "mnc";
        public static final String CATEGORY_MASK = "category_mask";
        public static final String URNS = "urns";
        public static final String SOURCE_MASK = "source_mask";
        public static final String CALL_ROUTING = "call_routing";
        public static final String SUBSCRIPTION_ID = "sub_id";
    }
}

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

import static android.content.ContentResolver.NOTIFY_DELETE;
import static android.content.ContentResolver.NOTIFY_INSERT;
import static android.content.ContentResolver.NOTIFY_UPDATE;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the ability to query the enhanced address book databases(A.K.A. EAB) based on
 * both SIP options and UCE presence server data.
 *
 * <p>
 * There are 4 tables in EAB DB:
 * <ul>
 *     <li><em>Contact:</em> It stores the name and phone number of the contact.
 *
 *     <li><em>Common:</em> It's a general table for storing the query results and the mechanisms of
 *     querying UCE capabilities. It should be 1:1 mapped to the contact table and has a foreign
 *     key(eab_contact_id) that refers to the id of contact table. If the value of mechanism is
 *     1 ({@link android.telephony.ims.RcsContactUceCapability#CAPABILITY_MECHANISM_PRESENCE}), the
 *     capability information should be stored in presence table, if the value of mechanism is
 *     2({@link android.telephony.ims.RcsContactUceCapability#CAPABILITY_MECHANISM_OPTIONS}), it
 *     should be stored in options table.
 *
 *     <li><em>Presence:</em> It stores the information
 *     ({@link android.telephony.ims.RcsContactUceCapability}) that queried through presence server.
 *     It should be *:1 mapped to the common table and has a foreign key(eab_common_id) that refers
 *     to the id of common table.
 *
 *     <li><em>Options:</em> It stores the information
 *     ({@link android.telephony.ims.RcsContactUceCapability}) that queried through SIP OPTIONS. It
 *     should be *:1 mapped to the common table and it has a foreign key(eab_common_id) that refers
 *     to the id of common table.
 * </ul>
 * </p>
 */
public class EabProvider extends ContentProvider {
    // The public URI for operating Eab DB. They support query, insert, delete and update.
    public static final Uri CONTACT_URI = Uri.parse("content://eab/contact");
    public static final Uri COMMON_URI = Uri.parse("content://eab/common");
    public static final Uri PRESENCE_URI = Uri.parse("content://eab/presence");
    public static final Uri OPTIONS_URI = Uri.parse("content://eab/options");

    // The public URI for querying EAB DB. Only support query.
    public static final Uri ALL_DATA_URI = Uri.parse("content://eab/all");

    @VisibleForTesting
    public static final String AUTHORITY = "eab";

    private static final String TAG = "EabProvider";
    private static final int DATABASE_VERSION = 4;

    public static final String EAB_CONTACT_TABLE_NAME = "eab_contact";
    public static final String EAB_COMMON_TABLE_NAME = "eab_common";
    public static final String EAB_PRESENCE_TUPLE_TABLE_NAME = "eab_presence";
    public static final String EAB_OPTIONS_TABLE_NAME = "eab_options";

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int URL_CONTACT = 1;
    private static final int URL_COMMON = 2;
    private static final int URL_PRESENCE = 3;
    private static final int URL_OPTIONS = 4;
    private static final int URL_ALL = 5;
    private static final int URL_ALL_WITH_SUB_ID_AND_PHONE_NUMBER = 6;

    static {
        URI_MATCHER.addURI(AUTHORITY, "contact", URL_CONTACT);
        URI_MATCHER.addURI(AUTHORITY, "common", URL_COMMON);
        URI_MATCHER.addURI(AUTHORITY, "presence", URL_PRESENCE);
        URI_MATCHER.addURI(AUTHORITY, "options", URL_OPTIONS);
        URI_MATCHER.addURI(AUTHORITY, "all", URL_ALL);
        URI_MATCHER.addURI(AUTHORITY, "all/#/*", URL_ALL_WITH_SUB_ID_AND_PHONE_NUMBER);
    }

    private static final String QUERY_CONTACT_TABLE =
            " SELECT * FROM " + EAB_CONTACT_TABLE_NAME;

    private static final String JOIN_ALL_TABLES =
            // join common table
            " INNER JOIN " + EAB_COMMON_TABLE_NAME
            + " ON " + EAB_CONTACT_TABLE_NAME + "." + ContactColumns._ID
            + "=" + EAB_COMMON_TABLE_NAME + "." + EabCommonColumns.EAB_CONTACT_ID

            // join options table
            + " LEFT JOIN " + EAB_OPTIONS_TABLE_NAME
            + " ON " + EAB_COMMON_TABLE_NAME + "." + EabCommonColumns._ID
            + "=" + EAB_OPTIONS_TABLE_NAME + "." + OptionsColumns.EAB_COMMON_ID

            // join presence table
            + " LEFT JOIN " + EAB_PRESENCE_TUPLE_TABLE_NAME
            + " ON " + EAB_COMMON_TABLE_NAME + "." + EabCommonColumns._ID
            + "=" + EAB_PRESENCE_TUPLE_TABLE_NAME + "."
            + PresenceTupleColumns.EAB_COMMON_ID;

    /**
     * The contact table's columns.
     */
    public static class ContactColumns implements BaseColumns {

        /**
         * The contact's phone number. It may come from contact provider or someone via
         * {@link EabControllerImpl#saveCapabilities(List)} to save the capability but the phone
         * number not in contact provider.
         *
         * <P>Type: TEXT</P>
         */
        public static final String PHONE_NUMBER = "phone_number";

        /**
         * The ID of contact that store in contact provider. It refer to the
         * {@link android.provider.ContactsContract.Data#CONTACT_ID}. If the phone number not in
         * contact provider, the value should be null.
         *
         * <P>Type: INTEGER</P>
         */
        public static final String CONTACT_ID = "contact_id";

        /**
         * The ID of contact that store in contact provider. It refer to the
         * {@link android.provider.ContactsContract.Data#RAW_CONTACT_ID}. If the phone number not in
         * contact provider, the value should be null.
         *
         * <P>Type: INTEGER</P>
         */
        public static final String RAW_CONTACT_ID = "raw_contact_id";

        /**
         * The ID of phone number that store in contact provider. It refer to the
         * {@link android.provider.ContactsContract.Data#_ID}. If the phone number not in
         * contact provider, the value should be null.
         *
         * <P>Type:  INTEGER</P>
         */
        public static final String DATA_ID = "data_id";
    }

    /**
     * The common table's columns. The eab_contact_id should refer to the id of contact table.
     */
    public static class EabCommonColumns implements BaseColumns {

        /**
         * A reference to the {@link ContactColumns#_ID} that this data belongs to.
         * <P>Type:  INTEGER</P>
         */
        public static final String EAB_CONTACT_ID = "eab_contact_id";

        /**
         * The mechanism of querying UCE capability. Possible values are
         * {@link android.telephony.ims.RcsContactUceCapability#CAPABILITY_MECHANISM_OPTIONS }
         * and
         * {@link android.telephony.ims.RcsContactUceCapability#CAPABILITY_MECHANISM_PRESENCE }
         * <P>Type: INTEGER</P>
         */
        public static final String MECHANISM = "mechanism";

        /**
         * The result of querying UCE capability. Possible values are
         * {@link android.telephony.ims.RcsContactUceCapability#REQUEST_RESULT_NOT_ONLINE }
         * and
         * {@link android.telephony.ims.RcsContactUceCapability#REQUEST_RESULT_NOT_FOUND }
         * and
         * {@link android.telephony.ims.RcsContactUceCapability#REQUEST_RESULT_FOUND }
         * and
         * {@link android.telephony.ims.RcsContactUceCapability#REQUEST_RESULT_UNKNOWN }
         * <P>Type: INTEGER</P>
         */
        public static final String REQUEST_RESULT = "request_result";

        /**
         * The subscription id.
         * <P>Type:  INTEGER</P>
         */
        public static final String SUBSCRIPTION_ID = "subscription_id";

        /**
         * The value of the 'entity' attribute is the 'pres' URL of the PRESENTITY publishing
         * presence document
         * <P>Type:  TEXT</P>
         */
        public static final String ENTITY_URI = "entity_uri";
    }

    /**
     * This is used to generate a instance of {@link android.telephony.ims.RcsContactPresenceTuple}.
     * See that class for more information on each of these parameters.
     */
    public static class PresenceTupleColumns implements BaseColumns {

        /**
         * A reference to the {@link ContactColumns#_ID} that this data belongs to.
         * <P>Type:  INTEGER</P>
         */
        public static final String EAB_COMMON_ID = "eab_common_id";

        /**
         * The basic status of service capabilities. Possible values are
         * {@link android.telephony.ims.RcsContactPresenceTuple#TUPLE_BASIC_STATUS_OPEN}
         * and
         * {@link android.telephony.ims.RcsContactPresenceTuple#TUPLE_BASIC_STATUS_CLOSED}
         * <P>Type:  TEXT</P>
         */
        public static final String BASIC_STATUS = "basic_status";

        /**
         * The OMA Presence service-id associated with this capability. See the OMA Presence SIMPLE
         * specification v1.1, section 10.5.1.
         * <P>Type:  TEXT</P>
         */
        public static final String SERVICE_ID = "service_id";

        /**
         * The contact uri of service capabilities.
         * <P>Type:  TEXT</P>
         */
        public static final String CONTACT_URI = "contact_uri";

        /**
         * The service version of service capabilities.
         * <P>Type:  TEXT</P>
         */
        public static final String SERVICE_VERSION = "service_version";

        /**
         * The description of service capabilities.
         * <P>Type:  TEXT</P>
         */
        public static final String DESCRIPTION = "description";

        /**
         * The supported duplex mode of service capabilities. Possible values are
         * {@link android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities#DUPLEX_MODE_FULL}
         * and
         * {@link android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities#DUPLEX_MODE_HALF}
         * and
         * {@link android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities#DUPLEX_MODE_RECEIVE_ONLY}
         * and
         * {@link android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities#DUPLEX_MODE_SEND_ONLY}
         * <P>Type:  TEXT</P>
         */
        public static final String DUPLEX_MODE = "duplex_mode";

        /**
         * The unsupported duplex mode of service capabilities. Possible values are
         * {@link android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities#DUPLEX_MODE_FULL}
         * and
         * {@link android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities#DUPLEX_MODE_HALF}
         * and
         * {@link android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities#DUPLEX_MODE_RECEIVE_ONLY}
         * and
         * {@link android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities#DUPLEX_MODE_SEND_ONLY}
         * <P>Type:  TEXT</P>
         */
        public static final String UNSUPPORTED_DUPLEX_MODE = "unsupported_duplex_mode";

        /**
         * The presence request timestamp. Represents seconds of UTC time since Unix epoch
         * 1970-01-01 00:00:00.
         * <P>Type:  LONG</P>
         */
        public static final String REQUEST_TIMESTAMP = "presence_request_timestamp";

        /**
         * The audio capable.
         * <P>Type:  BOOLEAN </P>
         */
        public static final String AUDIO_CAPABLE = "audio_capable";

        /**
         * The video capable.
         * <P>Type:  BOOLEAN </P>
         */
        public static final String VIDEO_CAPABLE = "video_capable";
    }

    /**
     * This is used to generate a instance of {@link android.telephony.ims.RcsContactPresenceTuple}.
     * See that class for more information on each of these parameters.
     */
    public static class OptionsColumns implements BaseColumns {

        /**
         * A reference to the {@link ContactColumns#_ID} that this data belongs to.
         * <P>Type:  INTEGER</P>
         */
        public static final String EAB_COMMON_ID = "eab_common_id";

        /**
         * An IMS feature tag indicating the capabilities of the contact. See RFC3840 #section-9.
         * <P>Type:  TEXT</P>
         */
        public static final String FEATURE_TAG = "feature_tag";

        /**
         * The request timestamp of options capabilities.
         * <P>Type:  LONG</P>
         */
        public static final String REQUEST_TIMESTAMP = "options_request_timestamp";
    }

    @VisibleForTesting
    public static final class EabDatabaseHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "EabDatabase";
        private static final List<String> CONTACT_UNIQUE_FIELDS = new ArrayList<>();
        private static final List<String> COMMON_UNIQUE_FIELDS = new ArrayList<>();

        static {
            CONTACT_UNIQUE_FIELDS.add(ContactColumns.PHONE_NUMBER);
        }

        @VisibleForTesting
        public static final String SQL_CREATE_CONTACT_TABLE = "CREATE TABLE "
                + EAB_CONTACT_TABLE_NAME
                + " ("
                + ContactColumns._ID + " INTEGER PRIMARY KEY, "
                + ContactColumns.PHONE_NUMBER + " Text DEFAULT NULL, "
                + ContactColumns.CONTACT_ID + " INTEGER DEFAULT -1, "
                + ContactColumns.RAW_CONTACT_ID + " INTEGER DEFAULT -1, "
                + ContactColumns.DATA_ID + " INTEGER DEFAULT -1, "
                + "UNIQUE (" + TextUtils.join(", ", CONTACT_UNIQUE_FIELDS) + ")"
                + ");";

        @VisibleForTesting
        public static final String SQL_CREATE_COMMON_TABLE = "CREATE TABLE "
                + EAB_COMMON_TABLE_NAME
                + " ("
                + EabCommonColumns._ID + " INTEGER PRIMARY KEY, "
                + EabCommonColumns.EAB_CONTACT_ID + " INTEGER DEFAULT -1, "
                + EabCommonColumns.MECHANISM + " INTEGER DEFAULT NULL, "
                + EabCommonColumns.REQUEST_RESULT + " INTEGER DEFAULT -1, "
                + EabCommonColumns.SUBSCRIPTION_ID + " INTEGER DEFAULT -1, "
                + EabCommonColumns.ENTITY_URI + " TEXT DEFAULT NULL "
                + ");";

        @VisibleForTesting
        public static final String SQL_CREATE_PRESENCE_TUPLE_TABLE = "CREATE TABLE "
                + EAB_PRESENCE_TUPLE_TABLE_NAME
                + " ("
                + PresenceTupleColumns._ID + " INTEGER PRIMARY KEY, "
                + PresenceTupleColumns.EAB_COMMON_ID + " INTEGER DEFAULT -1, "
                + PresenceTupleColumns.BASIC_STATUS + " TEXT DEFAULT NULL, "
                + PresenceTupleColumns.SERVICE_ID + " TEXT DEFAULT NULL, "
                + PresenceTupleColumns.SERVICE_VERSION + " TEXT DEFAULT NULL, "
                + PresenceTupleColumns.DESCRIPTION + " TEXT DEFAULT NULL, "
                + PresenceTupleColumns.REQUEST_TIMESTAMP + " LONG DEFAULT NULL, "
                + PresenceTupleColumns.CONTACT_URI + " TEXT DEFAULT NULL, "

                // For ServiceCapabilities
                + PresenceTupleColumns.DUPLEX_MODE + " TEXT DEFAULT NULL, "
                + PresenceTupleColumns.UNSUPPORTED_DUPLEX_MODE + " TEXT DEFAULT NULL, "
                + PresenceTupleColumns.AUDIO_CAPABLE + " BOOLEAN DEFAULT NULL, "
                + PresenceTupleColumns.VIDEO_CAPABLE + " BOOLEAN DEFAULT NULL"
                + ");";

        @VisibleForTesting
        public static final String SQL_CREATE_OPTIONS_TABLE = "CREATE TABLE "
                + EAB_OPTIONS_TABLE_NAME
                + " ("
                + OptionsColumns._ID + " INTEGER PRIMARY KEY, "
                + OptionsColumns.EAB_COMMON_ID + " INTEGER DEFAULT -1, "
                + OptionsColumns.REQUEST_TIMESTAMP + " LONG DEFAULT NULL, "
                + OptionsColumns.FEATURE_TAG + " TEXT DEFAULT NULL "
                + ");";

        EabDatabaseHelper(Context context) {
            super(context, DB_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_CONTACT_TABLE);
            db.execSQL(SQL_CREATE_COMMON_TABLE);
            db.execSQL(SQL_CREATE_PRESENCE_TUPLE_TABLE);
            db.execSQL(SQL_CREATE_OPTIONS_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            Log.d(TAG, "DB upgrade from " + oldVersion + " to " + newVersion);

            if (oldVersion < 2) {
                sqLiteDatabase.execSQL("ALTER TABLE " + EAB_CONTACT_TABLE_NAME + " ADD COLUMN "
                        + ContactColumns.CONTACT_ID + " INTEGER DEFAULT -1;");
                oldVersion = 2;
            }

            if (oldVersion < 3) {
                // Drop UNIQUE constraint in EAB_COMMON_TABLE, SQLite didn't support DROP
                // constraint, so drop the old one and migrate data to new one

                // Create temp table
                final String createTempTableCommand = "CREATE TABLE temp"
                        + " ("
                        + EabCommonColumns._ID + " INTEGER PRIMARY KEY, "
                        + EabCommonColumns.EAB_CONTACT_ID + " INTEGER DEFAULT -1, "
                        + EabCommonColumns.MECHANISM + " INTEGER DEFAULT NULL, "
                        + EabCommonColumns.REQUEST_RESULT + " INTEGER DEFAULT -1, "
                        + EabCommonColumns.SUBSCRIPTION_ID + " INTEGER DEFAULT -1 "
                        + ");";
                sqLiteDatabase.execSQL(createTempTableCommand);

                // Migrate data to temp table
                sqLiteDatabase.execSQL("INSERT INTO temp ("
                        + EabCommonColumns._ID + ", "
                        + EabCommonColumns.EAB_CONTACT_ID + ", "
                        + EabCommonColumns.MECHANISM + ", "
                        + EabCommonColumns.REQUEST_RESULT + ", "
                        + EabCommonColumns.SUBSCRIPTION_ID + ") "
                        + " SELECT "
                        + EabCommonColumns._ID + ", "
                        + EabCommonColumns.EAB_CONTACT_ID + ", "
                        + EabCommonColumns.MECHANISM + ", "
                        + EabCommonColumns.REQUEST_RESULT + ", "
                        + EabCommonColumns.SUBSCRIPTION_ID + " "
                        + " FROM "
                        + EAB_COMMON_TABLE_NAME
                        +";");

                // Drop old one
                sqLiteDatabase.execSQL("DROP TABLE " + EAB_COMMON_TABLE_NAME + ";");

                // Rename temp to eab_common
                sqLiteDatabase.execSQL("ALTER TABLE temp RENAME TO " + EAB_COMMON_TABLE_NAME + ";");
                oldVersion = 3;
            }

            if (oldVersion < 4) {
                sqLiteDatabase.execSQL("ALTER TABLE " + EAB_COMMON_TABLE_NAME + " ADD COLUMN "
                        + EabCommonColumns.ENTITY_URI + " Text DEFAULT NULL;");
                oldVersion = 4;
            }
        }
    }

    private EabDatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new EabDatabaseHelper(getContext());
        return true;
    }

    /**
     * Support 6 URLs for querying:
     *
     * <ul>
     * <li>{@link #URL_CONTACT}: query contact table.
     *
     * <li>{@link #URL_COMMON}: query common table.
     *
     * <li>{@link #URL_PRESENCE}: query presence capability table.
     *
     * <li>{@link #URL_OPTIONS}: query options capability table.
     *
     * <li>{@link #URL_ALL_WITH_SUB_ID_AND_PHONE_NUMBER}: To provide more efficient query way,
     * filter by the {@link ContactColumns#PHONE_NUMBER} first and join with others tables. The
     * format is like content://eab/all/[sub_id]/[phone_number]
     *
     * <li> {@link #URL_ALL}: Join all of tables at once
     * </ul>
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = getReadableDatabase();
        int match = URI_MATCHER.match(uri);
        int subId;
        String subIdString;

        Log.d(TAG, "Query URI: " + match);

        switch (match) {
            case URL_CONTACT:
                qb.setTables(EAB_CONTACT_TABLE_NAME);
                break;

            case URL_COMMON:
                qb.setTables(EAB_COMMON_TABLE_NAME);
                break;

            case URL_PRESENCE:
                qb.setTables(EAB_PRESENCE_TUPLE_TABLE_NAME);
                break;

            case URL_OPTIONS:
                qb.setTables(EAB_OPTIONS_TABLE_NAME);
                break;

            case URL_ALL_WITH_SUB_ID_AND_PHONE_NUMBER:
                List<String> pathSegment = uri.getPathSegments();

                subIdString = pathSegment.get(1);
                try {
                    subId = Integer.parseInt(subIdString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "NumberFormatException" + e);
                    return null;
                }
                qb.appendWhereStandalone(EabCommonColumns.SUBSCRIPTION_ID + "=" + subId);

                String phoneNumber = pathSegment.get(2);
                String whereClause;
                if (TextUtils.isEmpty(phoneNumber)) {
                    Log.e(TAG, "phone number is null");
                    return null;
                }
                whereClause = " where " + ContactColumns.PHONE_NUMBER + "='" + phoneNumber + "' ";
                qb.setTables(
                        "((" + QUERY_CONTACT_TABLE + whereClause + ") AS " + EAB_CONTACT_TABLE_NAME
                                + JOIN_ALL_TABLES + ")");
                break;

            case URL_ALL:
                qb.setTables("(" + QUERY_CONTACT_TABLE + JOIN_ALL_TABLES + ")");
                break;

            default:
                Log.d(TAG, "Query failed. Not support URL.");
                return null;
        }
        return qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteDatabase db = getWritableDatabase();
        int match = URI_MATCHER.match(uri);
        long result = 0;
        String tableName = "";
        switch (match) {
            case URL_CONTACT:
                tableName = EAB_CONTACT_TABLE_NAME;
                break;
            case URL_COMMON:
                tableName = EAB_COMMON_TABLE_NAME;
                break;
            case URL_PRESENCE:
                tableName = EAB_PRESENCE_TUPLE_TABLE_NAME;
                break;
            case URL_OPTIONS:
                tableName = EAB_OPTIONS_TABLE_NAME;
                break;
        }
        if (!TextUtils.isEmpty(tableName)) {
            result = db.insertWithOnConflict(tableName, null, contentValues,
                    SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "Insert uri: " + match + " ID: " + result);
            if (result > 0) {
                getContext().getContentResolver().notifyChange(uri, null, NOTIFY_INSERT);
            }
        } else {
            Log.d(TAG, "Insert. Not support URI.");
        }

        return Uri.withAppendedPath(uri, String.valueOf(result));
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = getWritableDatabase();
        int match = URI_MATCHER.match(uri);
        int result = 0;
        String tableName = "";
        switch (match) {
            case URL_CONTACT:
                tableName = EAB_CONTACT_TABLE_NAME;
                break;
            case URL_COMMON:
                tableName = EAB_COMMON_TABLE_NAME;
                break;
            case URL_PRESENCE:
                tableName = EAB_PRESENCE_TUPLE_TABLE_NAME;
                break;
            case URL_OPTIONS:
                tableName = EAB_OPTIONS_TABLE_NAME;
                break;
        }

        if (TextUtils.isEmpty(tableName)) {
            Log.d(TAG, "bulkInsert. Not support URI.");
            return 0;
        }

        try {
            // Batch all insertions in a single transaction to improve efficiency.
            db.beginTransaction();
            for (ContentValues contentValue : values) {
                if (contentValue != null) {
                    db.insertWithOnConflict(tableName, null, contentValue,
                            SQLiteDatabase.CONFLICT_REPLACE);
                    result++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (result > 0) {
            getContext().getContentResolver().notifyChange(uri, null, NOTIFY_INSERT);
        }
        Log.d(TAG, "bulkInsert uri: " + match + " count: " + result);
        return result;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = getWritableDatabase();
        int match = URI_MATCHER.match(uri);
        int result = 0;
        String tableName = "";
        switch (match) {
            case URL_CONTACT:
                tableName = EAB_CONTACT_TABLE_NAME;
                break;
            case URL_COMMON:
                tableName = EAB_COMMON_TABLE_NAME;
                break;
            case URL_PRESENCE:
                tableName = EAB_PRESENCE_TUPLE_TABLE_NAME;
                break;
            case URL_OPTIONS:
                tableName = EAB_OPTIONS_TABLE_NAME;
                break;
        }
        if (!TextUtils.isEmpty(tableName)) {
            result = db.delete(tableName, selection, selectionArgs);
            if (result > 0) {
                getContext().getContentResolver().notifyChange(uri, null, NOTIFY_DELETE);
            }
            Log.d(TAG, "Delete uri: " + match + " result: " + result);
        } else {
            Log.d(TAG, "Delete. Not support URI.");
        }
        return result;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
            String[] selectionArgs) {
        SQLiteDatabase db = getWritableDatabase();
        int match = URI_MATCHER.match(uri);
        int result = 0;
        String tableName = "";
        switch (match) {
            case URL_CONTACT:
                tableName = EAB_CONTACT_TABLE_NAME;
                break;
            case URL_COMMON:
                tableName = EAB_COMMON_TABLE_NAME;
                break;
            case URL_PRESENCE:
                tableName = EAB_PRESENCE_TUPLE_TABLE_NAME;
                break;
            case URL_OPTIONS:
                tableName = EAB_OPTIONS_TABLE_NAME;
                break;
        }
        if (!TextUtils.isEmpty(tableName)) {
            result = db.updateWithOnConflict(tableName, contentValues,
                    selection, selectionArgs, SQLiteDatabase.CONFLICT_REPLACE);
            if (result > 0) {
                getContext().getContentResolver().notifyChange(uri, null, NOTIFY_UPDATE);
            }
            Log.d(TAG, "Update uri: " + match + " result: " + result);
        } else {
            Log.d(TAG, "Update. Not support URI.");
        }
        return result;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @VisibleForTesting
    public SQLiteDatabase getWritableDatabase() {
        return mOpenHelper.getWritableDatabase();
    }

    @VisibleForTesting
    public SQLiteDatabase getReadableDatabase() {
        return mOpenHelper.getReadableDatabase();
    }
}

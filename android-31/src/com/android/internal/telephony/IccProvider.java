/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.compat.annotation.UnsupportedAppUsage;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyFrameworkInitializer;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.telephony.Rlog;

import java.util.List;

/**
 * {@hide}
 */
public class IccProvider extends ContentProvider {
    private static final String TAG = "IccProvider";
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private static final boolean DBG = true;


    @UnsupportedAppUsage
    private static final String[] ADDRESS_BOOK_COLUMN_NAMES = new String[] {
        "name",
        "number",
        "emails",
        "anrs",
        "_id"
    };

    protected static final int ADN = 1;
    protected static final int ADN_SUB = 2;
    protected static final int FDN = 3;
    protected static final int FDN_SUB = 4;
    protected static final int SDN = 5;
    protected static final int SDN_SUB = 6;
    protected static final int ADN_ALL = 7;

    @VisibleForTesting
    public static final String STR_TAG = "tag";
    @VisibleForTesting
    public static final String STR_NUMBER = "number";
    @VisibleForTesting
    public static final String STR_EMAILS = "emails";
    @VisibleForTesting
    public static final String STR_ANRS = "anrs";
    @VisibleForTesting
    public static final String STR_NEW_TAG = "newTag";
    @VisibleForTesting
    public static final String STR_NEW_NUMBER = "newNumber";
    @VisibleForTesting
    public static final String STR_NEW_EMAILS = "newEmails";
    @VisibleForTesting
    public static final String STR_NEW_ANRS = "newAnrs";
    @VisibleForTesting
    public static final String STR_PIN2 = "pin2";

    private static final UriMatcher URL_MATCHER =
                            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URL_MATCHER.addURI("icc", "adn", ADN);
        URL_MATCHER.addURI("icc", "adn/subId/#", ADN_SUB);
        URL_MATCHER.addURI("icc", "fdn", FDN);
        URL_MATCHER.addURI("icc", "fdn/subId/#", FDN_SUB);
        URL_MATCHER.addURI("icc", "sdn", SDN);
        URL_MATCHER.addURI("icc", "sdn/subId/#", SDN_SUB);
    }

    private SubscriptionManager mSubscriptionManager;

    @UnsupportedAppUsage
    public IccProvider() {
    }

    @Override
    public boolean onCreate() {
        mSubscriptionManager = SubscriptionManager.from(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        if (DBG) log("query");

        switch (URL_MATCHER.match(url)) {
            case ADN:
                return loadFromEf(IccConstants.EF_ADN,
                        SubscriptionManager.getDefaultSubscriptionId());

            case ADN_SUB:
                return loadFromEf(IccConstants.EF_ADN, getRequestSubId(url));

            case FDN:
                return loadFromEf(IccConstants.EF_FDN,
                        SubscriptionManager.getDefaultSubscriptionId());

            case FDN_SUB:
                return loadFromEf(IccConstants.EF_FDN, getRequestSubId(url));

            case SDN:
                return loadFromEf(IccConstants.EF_SDN,
                        SubscriptionManager.getDefaultSubscriptionId());

            case SDN_SUB:
                return loadFromEf(IccConstants.EF_SDN, getRequestSubId(url));

            case ADN_ALL:
                return loadAllSimContacts(IccConstants.EF_ADN);

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    private Cursor loadAllSimContacts(int efType) {
        Cursor [] result;
        List<SubscriptionInfo> subInfoList = mSubscriptionManager
                .getActiveSubscriptionInfoList(false);

        if ((subInfoList == null) || (subInfoList.size() == 0)) {
            result = new Cursor[0];
        } else {
            int subIdCount = subInfoList.size();
            result = new Cursor[subIdCount];
            int subId;

            for (int i = 0; i < subIdCount; i++) {
                subId = subInfoList.get(i).getSubscriptionId();
                result[i] = loadFromEf(efType, subId);
                Rlog.i(TAG,"ADN Records loaded for Subscription ::" + subId);
            }
        }

        return new MergeCursor(result);
    }

    @Override
    public String getType(Uri url) {
        switch (URL_MATCHER.match(url)) {
            case ADN:
            case ADN_SUB:
            case FDN:
            case FDN_SUB:
            case SDN:
            case SDN_SUB:
            case ADN_ALL:
                return "vnd.android.cursor.dir/sim-contact";

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        Uri resultUri;
        int efType;
        String pin2 = null;
        int subId;

        if (DBG) log("insert");

        int match = URL_MATCHER.match(url);
        switch (match) {
            case ADN:
                efType = IccConstants.EF_ADN;
                subId = SubscriptionManager.getDefaultSubscriptionId();
                break;

            case ADN_SUB:
                efType = IccConstants.EF_ADN;
                subId = getRequestSubId(url);
                break;

            case FDN:
                efType = IccConstants.EF_FDN;
                subId = SubscriptionManager.getDefaultSubscriptionId();
                pin2 = initialValues.getAsString("pin2");
                break;

            case FDN_SUB:
                efType = IccConstants.EF_FDN;
                subId = getRequestSubId(url);
                pin2 = initialValues.getAsString("pin2");
                break;

            default:
                throw new UnsupportedOperationException(
                        "Cannot insert into URL: " + url);
        }

        // We're not using the incoming initialValues
        // so we can check/gate the arguments.
        String tag = initialValues.getAsString(STR_TAG);
        String number = initialValues.getAsString(STR_NUMBER);
        String emails = initialValues.getAsString(STR_EMAILS);
        String anrs = initialValues.getAsString(STR_ANRS);

        ContentValues values = new ContentValues();
        values.put(STR_NEW_TAG, tag);
        values.put(STR_NEW_NUMBER, number);
        values.put(STR_NEW_EMAILS, emails);
        values.put(STR_NEW_ANRS, anrs);
        boolean success = updateIccRecordInEf(efType, values, pin2, subId);

        if (!success) {
            return null;
        }

        StringBuilder buf = new StringBuilder("content://icc/");
        switch (match) {
            case ADN:
                buf.append("adn/");
                break;

            case ADN_SUB:
                buf.append("adn/subId/");
                break;

            case FDN:
                buf.append("fdn/");
                break;

            case FDN_SUB:
                buf.append("fdn/subId/");
                break;
        }

        // TODO: we need to find out the rowId for the newly added record
        buf.append(0);

        resultUri = Uri.parse(buf.toString());

        getContext().getContentResolver().notifyChange(url, null);
        /*
        // notify interested parties that an insertion happened
        getContext().getContentResolver().notifyInsert(
                resultUri, rowID, null);
        */

        return resultUri;
    }

    private String normalizeValue(String inVal) {
        int len = inVal.length();
        // If name is empty in contact return null to avoid crash.
        if (len == 0) {
            if (DBG) log("len of input String is 0");
            return inVal;
        }
        String retVal = inVal;

        if (inVal.charAt(0) == '\'' && inVal.charAt(len-1) == '\'') {
            retVal = inVal.substring(1, len-1);
        }

        return retVal;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        int efType;
        int subId;

        int match = URL_MATCHER.match(url);
        switch (match) {
            case ADN:
                efType = IccConstants.EF_ADN;
                subId = SubscriptionManager.getDefaultSubscriptionId();
                break;

            case ADN_SUB:
                efType = IccConstants.EF_ADN;
                subId = getRequestSubId(url);
                break;

            case FDN:
                efType = IccConstants.EF_FDN;
                subId = SubscriptionManager.getDefaultSubscriptionId();
                break;

            case FDN_SUB:
                efType = IccConstants.EF_FDN;
                subId = getRequestSubId(url);
                break;

            default:
                throw new UnsupportedOperationException(
                        "Cannot insert into URL: " + url);
        }

        if (DBG) log("delete");

        // parse where clause
        String tag = null;
        String number = null;
        String emails = null;
        String anrs = null;
        String pin2 = null;

        String[] tokens = where.split(" AND ");
        int n = tokens.length;

        while (--n >= 0) {
            String param = tokens[n];
            if (DBG) log("parsing '" + param + "'");

            String[] pair = param.split("=", 2);

            if (pair.length != 2) {
                Rlog.e(TAG, "resolve: bad whereClause parameter: " + param);
                continue;
            }
            String key = pair[0].trim();
            String val = pair[1].trim();

            if (STR_TAG.equals(key)) {
                tag = normalizeValue(val);
            } else if (STR_NUMBER.equals(key)) {
                number = normalizeValue(val);
            } else if (STR_EMAILS.equals(key)) {
                emails = normalizeValue(val);
            } else if (STR_ANRS.equals(key)) {
                anrs = normalizeValue(val);
            } else if (STR_PIN2.equals(key)) {
                pin2 = normalizeValue(val);
            }
        }

        ContentValues values = new ContentValues();
        values.put(STR_TAG, tag);
        values.put(STR_NUMBER, number);
        values.put(STR_EMAILS, emails);
        values.put(STR_ANRS, anrs);
        if ((efType == FDN) && TextUtils.isEmpty(pin2)) {
            return 0;
        }
        if (DBG) log("delete mvalues= " + values);
        boolean success = updateIccRecordInEf(efType, values, pin2, subId);
        if (!success) {
            return 0;
        }

        getContext().getContentResolver().notifyChange(url, null);
        return 1;
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        String pin2 = null;
        int efType;
        int subId;

        if (DBG) log("update");

        int match = URL_MATCHER.match(url);
        switch (match) {
            case ADN:
                efType = IccConstants.EF_ADN;
                subId = SubscriptionManager.getDefaultSubscriptionId();
                break;

            case ADN_SUB:
                efType = IccConstants.EF_ADN;
                subId = getRequestSubId(url);
                break;

            case FDN:
                efType = IccConstants.EF_FDN;
                subId = SubscriptionManager.getDefaultSubscriptionId();
                pin2 = values.getAsString("pin2");
                break;

            case FDN_SUB:
                efType = IccConstants.EF_FDN;
                subId = getRequestSubId(url);
                pin2 = values.getAsString("pin2");
                break;

            default:
                throw new UnsupportedOperationException(
                        "Cannot insert into URL: " + url);
        }

        boolean success = updateIccRecordInEf(efType, values, pin2, subId);

        if (!success) {
            return 0;
        }

        getContext().getContentResolver().notifyChange(url, null);
        return 1;
    }

    private MatrixCursor loadFromEf(int efType, int subId) {
        if (DBG) log("loadFromEf: efType=0x" +
                Integer.toHexString(efType).toUpperCase() + ", subscription=" + subId);

        List<AdnRecord> adnRecords = null;
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(
                    TelephonyFrameworkInitializer
                            .getTelephonyServiceManager()
                            .getIccPhoneBookServiceRegisterer()
                            .get());
            if (iccIpb != null) {
                adnRecords = iccIpb.getAdnRecordsInEfForSubscriber(subId, efType);
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            if (DBG) log(ex.toString());
        }

        if (adnRecords != null) {
            // Load the results
            final int N = adnRecords.size();
            final MatrixCursor cursor = new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES, N);
            if (DBG) log("adnRecords.size=" + N);
            for (int i = 0; i < N ; i++) {
                loadRecord(adnRecords.get(i), cursor, i);
            }
            return cursor;
        } else {
            // No results to load
            Rlog.w(TAG, "Cannot load ADN records");
            return new MatrixCursor(ADDRESS_BOOK_COLUMN_NAMES);
        }
    }

    private boolean
    updateIccRecordInEf(int efType, ContentValues values, String pin2, int subId) {
        boolean success = false;
        if (DBG) log("updateIccRecordInEf: efType=" + efType +
                ", values: [ "+ values + "  ], subId:" + subId);
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(
                    TelephonyFrameworkInitializer
                            .getTelephonyServiceManager()
                            .getIccPhoneBookServiceRegisterer()
                            .get());
            if (iccIpb != null) {
                success = iccIpb
                        .updateAdnRecordsInEfBySearchForSubscriber(
                            subId, efType, values, pin2);
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            if (DBG) log(ex.toString());
        }
        if (DBG) log("updateIccRecordInEf: " + success);
        return success;
    }

    /**
     * Loads an AdnRecord into a MatrixCursor. Must be called with mLock held.
     *
     * @param record the ADN record to load from
     * @param cursor the cursor to receive the results
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void loadRecord(AdnRecord record, MatrixCursor cursor, int id) {
        if (!record.isEmpty()) {
            Object[] contact = new Object[5];
            String alphaTag = record.getAlphaTag();
            String number = record.getNumber();

            if (DBG) log("loadRecord: " + alphaTag + ", " + Rlog.pii(TAG, number));
            contact[0] = alphaTag;
            contact[1] = number;

            String[] emails = record.getEmails();
            if (emails != null) {
                StringBuilder emailString = new StringBuilder();
                for (String email: emails) {
                    log("Adding email:" + Rlog.pii(TAG, email));
                    emailString.append(email);
                    emailString.append(",");
                }
                contact[2] = emailString.toString();
            }

            String[] anrs = record.getAdditionalNumbers();
            if (anrs != null) {
                StringBuilder anrString = new StringBuilder();
                for (String anr : anrs) {
                    if (DBG) log("Adding anr:" + anr);
                    anrString.append(anr);
                    anrString.append(":");
                }
                contact[3] = anrString.toString();
            }

            contact[4] = id;
            cursor.addRow(contact);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void log(String msg) {
        Rlog.d(TAG, "[IccProvider] " + msg);
    }

    private int getRequestSubId(Uri url) {
        if (DBG) log("getRequestSubId url: " + url);

        try {
            return Integer.parseInt(url.getLastPathSegment());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Unknown URL " + url);
        }
    }
}

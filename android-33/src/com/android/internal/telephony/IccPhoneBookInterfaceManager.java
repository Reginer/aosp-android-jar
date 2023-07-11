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
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.android.internal.telephony.uicc.AdnCapacity;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.SimPhonebookRecordCache;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.telephony.Rlog;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IccPhoneBookInterfaceManager to provide an inter-process communication to
 * access ADN-like SIM records.
 */
public class IccPhoneBookInterfaceManager {
    static final String LOG_TAG = "IccPhoneBookIM";
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected static final boolean DBG = true;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected Phone mPhone;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected AdnRecordCache mAdnCache;
    protected SimPhonebookRecordCache mSimPbRecordCache;

    protected static final int EVENT_GET_SIZE_DONE = 1;
    protected static final int EVENT_LOAD_DONE = 2;
    protected static final int EVENT_UPDATE_DONE = 3;

    private static final class Request {
        AtomicBoolean mStatus = new AtomicBoolean(false);
        Object mResult = null;
    }

    @UnsupportedAppUsage
    protected Handler mBaseHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            Request request = (Request) ar.userObj;

            switch (msg.what) {
                case EVENT_GET_SIZE_DONE:
                    int[] recordSize = null;
                    if (ar.exception == null) {
                        recordSize = (int[]) ar.result;
                        // recordSize[0]  is the record length
                        // recordSize[1]  is the total length of the EF file
                        // recordSize[2]  is the number of records in the EF file
                        logd("GET_RECORD_SIZE Size " + recordSize[0]
                                + " total " + recordSize[1]
                                + " #record " + recordSize[2]);
                    } else {
                        loge("EVENT_GET_SIZE_DONE: failed; ex=" + ar.exception);
                    }
                    notifyPending(request, recordSize);
                    break;
                case EVENT_UPDATE_DONE:
                    boolean success = (ar.exception == null);
                    if (!success) {
                        loge("EVENT_UPDATE_DONE - failed; ex=" + ar.exception);
                    }
                    notifyPending(request, success);
                    break;
                case EVENT_LOAD_DONE:
                    List<AdnRecord> records = null;
                    if (ar.exception == null) {
                        records = (List<AdnRecord>) ar.result;
                    } else {
                        loge("EVENT_LOAD_DONE: Cannot load ADN records; ex="
                                + ar.exception);
                    }
                    notifyPending(request, records);
                    break;
            }
        }

        private void notifyPending(Request request, Object result) {
            if (request != null) {
                synchronized (request) {
                    request.mResult = result;
                    request.mStatus.set(true);
                    request.notifyAll();
                }
            }
        }
    };

    public IccPhoneBookInterfaceManager(Phone phone) {
        this.mPhone = phone;
        IccRecords r = phone.getIccRecords();
        if (r != null) {
            mAdnCache = r.getAdnCache();
        }

        mSimPbRecordCache = new SimPhonebookRecordCache(
                phone.getContext(), phone.getPhoneId(), phone.mCi);
    }

    public void dispose() {
        mSimPbRecordCache.dispose();
    }

    public void updateIccRecords(IccRecords iccRecords) {
        if (iccRecords != null) {
            mAdnCache = iccRecords.getAdnCache();
        } else {
            mAdnCache = null;
        }
    }

    @UnsupportedAppUsage
    protected void logd(String msg) {
        Rlog.d(LOG_TAG, "[IccPbInterfaceManager] " + msg);
    }

    @UnsupportedAppUsage
    protected void loge(String msg) {
        Rlog.e(LOG_TAG, "[IccPbInterfaceManager] " + msg);
    }

    private AdnRecord generateAdnRecordWithOldTagByContentValues(ContentValues values) {
        if (values == null) {
            return null;
        }
        final String oldTag = values.getAsString(IccProvider.STR_TAG);
        final String oldPhoneNumber = values.getAsString(IccProvider.STR_NUMBER);
        final String oldEmail = values.getAsString(IccProvider.STR_EMAILS);
        final String oldAnr = values.getAsString(IccProvider.STR_ANRS);;
        String[] oldEmailArray = TextUtils.isEmpty(oldEmail)
                ? null : getEmailStringArray(oldEmail);
        String[] oldAnrArray = TextUtils.isEmpty(oldAnr) ? null : getAnrStringArray(oldAnr);
        return new AdnRecord(oldTag, oldPhoneNumber, oldEmailArray, oldAnrArray);
    }

    private AdnRecord generateAdnRecordWithNewTagByContentValues(
            int efId, int recordNumber, ContentValues values) {
        if (values == null) {
            return null;
        }
        final String newTag = values.getAsString(IccProvider.STR_NEW_TAG);
        final String newPhoneNumber = values.getAsString(IccProvider.STR_NEW_NUMBER);
        final String newEmail = values.getAsString(IccProvider.STR_NEW_EMAILS);
        final String newAnr = values.getAsString(IccProvider.STR_NEW_ANRS);
        String[] newEmailArray = TextUtils.isEmpty(newEmail)
                ? null : getEmailStringArray(newEmail);
        String[] newAnrArray = TextUtils.isEmpty(newAnr) ? null : getAnrStringArray(newAnr);
        return new AdnRecord(
                efId, recordNumber, newTag, newPhoneNumber, newEmailArray, newAnrArray);
    }

    /**
     * Replace oldAdn with newAdn in ADN-like record in EF
     *
     * getAdnRecordsInEf must be called at least once before this function,
     * otherwise an error will be returned.
     * throws SecurityException if no WRITE_CONTACTS permission
     *
     * @param efid must be one among EF_ADN, EF_FDN, and EF_SDN
     * @param values old adn tag,  phone number, email and anr to be replaced
     *        new adn tag,  phone number, email and anr to be stored
     * @param pin2 required to update EF_FDN, otherwise must be null
     * @return true for success
     */
    public boolean updateAdnRecordsInEfBySearchForSubscriber(int efid, ContentValues values,
            String pin2) {

        if (mPhone.getContext().checkCallingOrSelfPermission(
                android.Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }

        efid = updateEfForIccType(efid);

        if (DBG) {
            logd("updateAdnRecordsWithContentValuesInEfBySearch: efid=" + efid + ", values = " +
                values + ", pin2=" + pin2);
        }

        checkThread();
        Request updateRequest = new Request();
        synchronized (updateRequest) {
            Message response = mBaseHandler.obtainMessage(EVENT_UPDATE_DONE, updateRequest);
            AdnRecord oldAdn = generateAdnRecordWithOldTagByContentValues(values);
            if (usesPbCache(efid)) {
                AdnRecord newAdn =
                        generateAdnRecordWithNewTagByContentValues(IccConstants.EF_ADN, 0, values);
                mSimPbRecordCache.updateSimPbAdnBySearch(oldAdn, newAdn, response);
                waitForResult(updateRequest);
                return (boolean) updateRequest.mResult;
            } else {
                AdnRecord newAdn = generateAdnRecordWithNewTagByContentValues(efid, 0, values);
                if (mAdnCache != null) {
                    mAdnCache.updateAdnBySearch(efid, oldAdn, newAdn, pin2, response);
                    waitForResult(updateRequest);
                    return (boolean) updateRequest.mResult;
                } else {
                    loge("Failure while trying to update by search due to uninitialised adncache");
                    return false;
                }
            }
        }
    }

    /**
     * Update an ADN-like EF record by record index
     *
     * This is useful for iteration the whole ADN file, such as write the whole
     * phone book or erase/format the whole phonebook. Currently the email field
     * if set in the ADN record is ignored.
     * throws SecurityException if no WRITE_CONTACTS permission
     *
     * @param efid must be one among EF_ADN, EF_FDN, and EF_SDN
     * @param newTag adn tag to be stored
     * @param newPhoneNumber adn number to be stored
     *        Set both newTag and newPhoneNumber to "" means to replace the old
     *        record with empty one, aka, delete old record
     * @param index is 1-based adn record index to be updated
     * @param pin2 required to update EF_FDN, otherwise must be null
     * @return true for success
     */
    public boolean
    updateAdnRecordsInEfByIndex(int efid, ContentValues values, int index, String pin2) {

        if (mPhone.getContext().checkCallingOrSelfPermission(
                android.Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(
                    "Requires android.permission.WRITE_CONTACTS permission");
        }
        if (DBG) {
            logd("updateAdnRecordsInEfByIndex: efid=" + efid + ", values = " +
                values + " index=" + index + ", pin2=" + pin2);
        }

        checkThread();
        Request updateRequest = new Request();
        synchronized (updateRequest) {
            Message response = mBaseHandler.obtainMessage(EVENT_UPDATE_DONE, updateRequest);
            if (usesPbCache(efid)) {
                AdnRecord newAdn =
                        generateAdnRecordWithNewTagByContentValues(IccConstants.EF_ADN,
                        index, values);
                mSimPbRecordCache.updateSimPbAdnByRecordId(index, newAdn, response);
                waitForResult(updateRequest);
                return (boolean) updateRequest.mResult;
            } else {
                AdnRecord newAdn = generateAdnRecordWithNewTagByContentValues(efid, index, values);
                if (mAdnCache != null) {
                    mAdnCache.updateAdnByIndex(efid, newAdn, index, pin2, response);
                    waitForResult(updateRequest);
                    return (boolean) updateRequest.mResult;
                } else {
                    loge("Failure while trying to update by index due to uninitialised adncache");
                    return false;
                }
            }
        }
    }

    /**
     * Get the capacity of records in efid
     *
     * @param efid the EF id of a ADN-like ICC
     * @return  int[3] array
     *            recordSizes[0]  is the single record length
     *            recordSizes[1]  is the total length of the EF file
     *            recordSizes[2]  is the number of records in the EF file
     */
    public int[] getAdnRecordsSize(int efid) {
        if (DBG) logd("getAdnRecordsSize: efid=" + efid);
        checkThread();
        Request getSizeRequest = new Request();
        synchronized (getSizeRequest) {
            //Using mBaseHandler, no difference in EVENT_GET_SIZE_DONE handling
            Message response = mBaseHandler.obtainMessage(EVENT_GET_SIZE_DONE, getSizeRequest);
            IccFileHandler fh = mPhone.getIccFileHandler();
            if (fh != null) {
                fh.getEFLinearRecordSize(efid, response);
                waitForResult(getSizeRequest);
            }
        }

        return getSizeRequest.mResult == null ? new int[3] : (int[]) getSizeRequest.mResult;
    }


    /**
     * Loads the AdnRecords in efid and returns them as a
     * List of AdnRecords
     *
     * throws SecurityException if no READ_CONTACTS permission
     *
     * @param efid the EF id of a ADN-like ICC
     * @return List of AdnRecord
     */
    public List<AdnRecord> getAdnRecordsInEf(int efid) {

        if (mPhone.getContext().checkCallingOrSelfPermission(
                android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(
                    "Requires android.permission.READ_CONTACTS permission");
        }

        efid = updateEfForIccType(efid);
        if (DBG) logd("getAdnRecordsInEF: efid=0x" + Integer.toHexString(efid).toUpperCase());

        checkThread();
        Request loadRequest = new Request();
        synchronized (loadRequest) {
            Message response = mBaseHandler.obtainMessage(EVENT_LOAD_DONE, loadRequest);
            if (usesPbCache(efid)) {
                mSimPbRecordCache.requestLoadAllPbRecords(response);
                waitForResult(loadRequest);
                return (List<AdnRecord>) loadRequest.mResult;
            } else {
                if (mAdnCache != null) {
                    mAdnCache.requestLoadAllAdnLike(efid,
                            mAdnCache.extensionEfForEf(efid), response);
                    waitForResult(loadRequest);
                    return (List<AdnRecord>) loadRequest.mResult;
                } else {
                    loge("Failure while trying to load from SIM due to uninitialised adncache");
                    return null;
                }
            }
        }
    }

    @UnsupportedAppUsage
    protected void checkThread() {
        // Make sure this isn't the UI thread, since it will block
        if (mBaseHandler.getLooper().equals(Looper.myLooper())) {
            loge("query() called on the main UI thread!");
            throw new IllegalStateException(
                    "You cannot call query on this provder from the main UI thread.");
        }
    }

    protected void waitForResult(Request request) {
        synchronized (request) {
            while (!request.mStatus.get()) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    logd("interrupted while trying to update by search");
                }
            }
        }
    }

    @UnsupportedAppUsage
    private int updateEfForIccType(int efid) {
        // Check if we are trying to read ADN records
        if (efid == IccConstants.EF_ADN) {
            if (mPhone.getCurrentUiccAppType() == AppType.APPTYPE_USIM) {
                return IccConstants.EF_PBR;
            }
        }
        return efid;
    }

    private String[] getEmailStringArray(String str) {
        return str != null ? str.split(",") : null;
    }

    private String[] getAnrStringArray(String str) {
        return str != null ? str.split(":") : null;
    }

    /**
     * Get the capacity of ADN records
     *
     * @return AdnCapacity
     */
    public AdnCapacity getAdnRecordsCapacity() {
        if (DBG) logd("getAdnRecordsCapacity" );
        if (mPhone.getContext().checkCallingOrSelfPermission(
                android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(
                    "Requires android.permission.READ_CONTACTS permission");
        }
        int phoneId = mPhone.getPhoneId();

        UiccProfile profile = UiccController.getInstance().getUiccProfileForPhone(phoneId);

        if (profile != null) {
            IccCardConstants.State cardstate = profile.getState();
            if (cardstate == IccCardConstants.State.READY
                    || cardstate == IccCardConstants.State.LOADED) {
                checkThread();
                AdnCapacity capacity = mSimPbRecordCache.isEnabled()
                        ? mSimPbRecordCache.getAdnCapacity() : null;
                if (capacity == null) {
                    loge("Adn capacity is null");
                    return null;
                }

                if (DBG) logd("getAdnRecordsCapacity on slot " + phoneId
                        + ": max adn=" + capacity.getMaxAdnCount()
                        + ", used adn=" + capacity.getUsedAdnCount()
                        + ", max email=" + capacity.getMaxEmailCount()
                        + ", used email=" + capacity.getUsedEmailCount()
                        + ", max anr=" + capacity.getMaxAnrCount()
                        + ", used anr=" + capacity.getUsedAnrCount()
                        + ", max name length="+ capacity.getMaxNameLength()
                        + ", max number length =" + capacity.getMaxNumberLength()
                        + ", max email length =" + capacity.getMaxEmailLength()
                        + ", max anr length =" + capacity.getMaxAnrLength());
                return capacity;
            } else {
                logd("No UICC when getAdnRecordsCapacity.");
            }
        } else {
            logd("sim state is not ready when getAdnRecordsCapacity.");
        }
        return null;
    }

    private boolean usesPbCache(int efid) {
        return mSimPbRecordCache.isEnabled() &&
                    (efid == IccConstants.EF_PBR || efid == IccConstants.EF_ADN);
    }
}

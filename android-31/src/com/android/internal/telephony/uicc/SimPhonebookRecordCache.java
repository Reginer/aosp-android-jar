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

package com.android.internal.telephony.uicc;

import android.annotation.RequiresFeature;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.RadioInterfaceCapabilityController;
import com.android.internal.telephony.uicc.AdnCapacity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Used to store SIM phonebook records.
 * <p/>
 * This will be {@link #INVALID} if either is the case:
 * <ol>
 *   <li>The device does not support
 * {@link android.telephony.TelephonyManager#CAPABILITY_SIM_PHONEBOOK_IN_MODEM}.</li>
 * </ol>
 * {@hide}
 */
@RequiresFeature(
        enforcement = "android.telephony.TelephonyManager#isRadioInterfaceCapabilitySupported",
        value = "TelephonyManager.CAPABILITY_SIM_PHONEBOOK_IN_MODEM")
public class SimPhonebookRecordCache extends Handler {
    // Instance Variables
    private static final String LOG_TAG = "SimPhonebookRecordCache";
    private static final boolean DBG = true;
    // Event Constants
    private static final int EVENT_PHONEBOOK_CHANGED = 1;
    private static final int EVENT_PHONEBOOK_RECORDS_RECEIVED = 2;
    private static final int EVENT_GET_PHONEBOOK_RECORDS_DONE = 3;
    private static final int EVENT_GET_PHONEBOOK_CAPACITY_DONE = 4;
    private static final int EVENT_UPDATE_PHONEBOOK_RECORD_DONE = 5;
    private static final int EVENT_SIM_REFRESH = 6;
    private static final int EVENT_GET_PHONEBOOK_RECORDS_RETRY = 7;

    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_INTERVAL = 3000; // 3S

    // member variables
    private final CommandsInterface mCi;
    private int mPhoneId;
    private Context mContext;

    // Presenting ADN capacity, including ADN, EMAIL ANR, and so on.
    private AtomicReference<AdnCapacity> mAdnCapacity = new AtomicReference<AdnCapacity>(null);
    private Object mReadLock = new Object();
    private List<AdnRecord> mSimPbRecords =
            Collections.synchronizedList(new ArrayList<AdnRecord>());
    private List<UpdateRequest> mUpdateRequests =
            Collections.synchronizedList(new ArrayList<UpdateRequest>());
    // If true, clear the records in the cache and re-query from modem
    private AtomicBoolean mIsCacheInvalidated = new AtomicBoolean(false);
    private AtomicBoolean mIsRecordLoading = new AtomicBoolean(false);
    private AtomicBoolean mIsInRetry = new AtomicBoolean(false);
    private AtomicBoolean mIsInitialized = new AtomicBoolean(false);

    // People waiting for SIM phonebook records to be loaded
    ArrayList<Message> mAdnLoadingWaiters = new ArrayList<Message>();
    /**
     * The manual update from upper layer will result in notifying SIM phonebook changed,
     * leading to fetch the Adn capacity, then whether to need to reload phonebook records
     * is a problem. the SIM phoneback changed shall follow by updating record done, so that
     * uses this flag to avoid unnecessary loading.
     */
    boolean mIsUpdateDone = false;

    public SimPhonebookRecordCache(Context context, int phoneId, CommandsInterface ci) {
        mCi = ci;
        mPhoneId = phoneId;
        mContext = context;
        mCi.registerForSimPhonebookChanged(this, EVENT_PHONEBOOK_CHANGED, null);
        mCi.registerForIccRefresh(this, EVENT_SIM_REFRESH, null);
        mCi.registerForSimPhonebookRecordsReceived(this, EVENT_PHONEBOOK_RECORDS_RECEIVED, null);
    }

    /**
     * This is recommended to use in work thread like IccPhoneBookInterfaceManager
     * because it can't block main thread.
     * @return true if this feature is supported
     */
    public boolean isEnabled() {
        boolean isEnabled = RadioInterfaceCapabilityController
                .getInstance()
                .getCapabilities()
                .contains(TelephonyManager.CAPABILITY_SIM_PHONEBOOK_IN_MODEM);
        return mIsInitialized.get() || isEnabled;
    }

    public void dispose() {
        reset();
        mCi.unregisterForSimPhonebookChanged(this);
        mCi.unregisterForIccRefresh(this);
        mCi.unregisterForSimPhonebookRecordsReceived(this);
    }

    private void reset() {
        mAdnCapacity.set(null);
        mSimPbRecords.clear();
        mIsCacheInvalidated.set(false);
        mIsRecordLoading.set(false);
        mIsInRetry.set(false);
        mIsInitialized.set(false);
        mIsUpdateDone = false;
    }

    private void sendErrorResponse(Message response, String errString) {
        if (response != null) {
            Exception e = new RuntimeException(errString);
            AsyncResult.forMessage(response).exception = e;
            response.sendToTarget();
        }
    }

    private void notifyAndClearWaiters() {
        synchronized (mReadLock) {
            for (Message response : mAdnLoadingWaiters){
                if (response != null) {
                    AsyncResult.forMessage(response).result = mSimPbRecords;
                    response.sendToTarget();
                }
            }
            mAdnLoadingWaiters.clear();
        }
    }

    private void sendResponsesToWaitersWithError() {
        synchronized (mReadLock) {
            mReadLock.notify();

            for (Message response : mAdnLoadingWaiters) {
                sendErrorResponse(response, "Query adn record failed");
            }
            mAdnLoadingWaiters.clear();
        }
    }

    private void getSimPhonebookCapacity() {
        logd("Start to getSimPhonebookCapacity");
        mCi.getSimPhonebookCapacity(obtainMessage(EVENT_GET_PHONEBOOK_CAPACITY_DONE));
    }

    public AdnCapacity getAdnCapacity() {
        return mAdnCapacity.get();
    }

    private void fillCache() {
        synchronized (mReadLock) {
            fillCacheWithoutWaiting();
            try {
                mReadLock.wait();
            } catch (InterruptedException e) {
                loge("Interrupted Exception in queryAdnRecord");
            }
        }
    }

    private void fillCacheWithoutWaiting() {
        logd("Start to queryAdnRecord");
        if (mIsRecordLoading.compareAndSet(false, true)) {
            mCi.getSimPhonebookRecords(obtainMessage(EVENT_GET_PHONEBOOK_RECORDS_DONE));
        } else {
            logd("The loading is ongoing");
        }
    }

    public void requestLoadAllPbRecords(Message response) {
        if (response == null && !mIsInitialized.get()) {
            logd("Try to enforce flushing cache");
            fillCacheWithoutWaiting();
            return;
        }

        synchronized (mReadLock) {
            mAdnLoadingWaiters.add(response);
            final int pendingSize = mAdnLoadingWaiters.size();
            boolean isCapacityInvalid = getAdnCapacity() == null;
            if (isCapacityInvalid) {
                getSimPhonebookCapacity();
            }
            if (pendingSize > 1 || mIsInRetry.get()
                    || !mIsInitialized.get() || isCapacityInvalid) {
                logd("Add to the pending list as pending size = "
                        + pendingSize + " is retrying = " + mIsInRetry.get()
                        + " IsInitialized = " + mIsInitialized.get());
                return;
            }
        }
        if (!mIsRecordLoading.get() && !mIsInRetry.get()) {
            logd("ADN cache has already filled in");
            if (!mIsCacheInvalidated.get()) {
                notifyAndClearWaiters();
                return;
            }
        }
        fillCache();
    }

    @VisibleForTesting
    public boolean isLoading() {
        return mIsRecordLoading.get();
    }

    @VisibleForTesting
    public List<AdnRecord> getAdnRecords() {
        return mSimPbRecords;
    }

    private void notifyAdnLoadingWaiters() {
        synchronized (mReadLock) {
            mReadLock.notify();
        }
        notifyAndClearWaiters();
    }

    public void updateSimPbAdnByRecordId(int recordId, AdnRecord newAdn, Message response) {
        if (newAdn == null) {
            sendErrorResponse(response, "There is an invalid new Adn for update");
            return;
        }
        boolean found = false;
        int index = 0;
        for (Iterator<AdnRecord> it = mSimPbRecords.iterator(); it.hasNext();) {
            AdnRecord oldAdn = it.next();
            ++index;
            if (oldAdn.getRecId() == recordId) {
                found = true;
                break;
            }
        }
        if (!found) {
            sendErrorResponse(response, "There is an invalid old Adn for update");
            return;
        }
        updateSimPhonebookByNewAdn(index, newAdn, response);
    }

    public void updateSimPbAdnBySearch(AdnRecord oldAdn, AdnRecord newAdn, Message response) {
        int index = -1;
        if ((oldAdn == null || oldAdn.isEmpty()) && !newAdn.isEmpty()) {
            // Add contact
            index = 0;
        } else {
            int count = 1;
            // Delete or update contact
            for (Iterator<AdnRecord> it = mSimPbRecords.iterator(); it.hasNext();) {
                if (oldAdn.isEqual(it.next())) {
                    index = count;
                    break;
                }
                count++;
            }
        }
        if (index == -1) {
            sendErrorResponse(response, "SIM Phonebook record don't exist for " + oldAdn);
            return;
        }

        if (newAdn == null) {
            sendErrorResponse(response, "There is an invalid new Adn for update");
            return;
        }

        if (index == 0 && mAdnCapacity.get() != null && mAdnCapacity.get().isSimFull()) {
            sendErrorResponse(response, "SIM Phonebook record is full");
            return;
        }

        updateSimPhonebookByNewAdn(index, newAdn, response);
    }

    private void updateSimPhonebookByNewAdn(int index, AdnRecord newAdn, Message response) {
        int recordIndex = (index == 0) ? newAdn.getRecId()
                : mSimPbRecords.get(index - 1).getRecId();
        SimPhonebookRecord updateAdn = new SimPhonebookRecord.Builder()
                .setRecordIndex(recordIndex)
                .setAlphaTag(newAdn.getAlphaTag())
                .setNumber(newAdn.getNumber())
                .setEmails(newAdn.getEmails())
                .setAdditionalNumbers(newAdn.getAdditionalNumbers())
                .build();
        UpdateRequest updateRequest = new UpdateRequest(index, newAdn, updateAdn, response);
        mUpdateRequests.add(updateRequest);
        boolean isCapacityInvalid = getAdnCapacity() == null;
        if (isCapacityInvalid) {
            getSimPhonebookCapacity();
        }
        if (mIsRecordLoading.get() || mIsInRetry.get() || mUpdateRequests.size() > 1
                || !mIsInitialized.get() || isCapacityInvalid) {
            logd("It is pending on update as " + " mIsRecordLoading = " + mIsRecordLoading.get()
                    + " mIsInRetry = " + mIsInRetry.get() + " pending size = "
                    + mUpdateRequests.size() + " mIsInitialized = " + mIsInitialized.get());
            return;
        }

        updateSimPhonebook(updateRequest);
    }

    private void updateSimPhonebook(UpdateRequest request) {
        logd("update Sim phonebook");
        mCi.updateSimPhonebookRecord(request.phonebookRecord,
                obtainMessage(EVENT_UPDATE_PHONEBOOK_RECORD_DONE, request));
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        switch(msg.what) {
            case EVENT_PHONEBOOK_CHANGED:
                logd("EVENT_PHONEBOOK_CHANGED");
                handlePhonebookChanged();
                break;
            case EVENT_GET_PHONEBOOK_RECORDS_DONE:
                logd("EVENT_GET_PHONEBOOK_RECORDS_DONE");
                ar = (AsyncResult)msg.obj;
                if (ar != null && ar.exception != null) {
                    loge("Failed to gain phonebook records");
                    invalidateSimPbCache();
                    if (!mIsInRetry.get()) {
                        sendGettingPhonebookRecordsRetry(0);
                    }
                }
                break;
            case EVENT_GET_PHONEBOOK_CAPACITY_DONE:
                logd("EVENT_GET_PHONEBOOK_CAPACITY_DONE");
                ar = (AsyncResult)msg.obj;
                if (ar != null && ar.exception == null) {
                    AdnCapacity capacity = (AdnCapacity)ar.result;
                    handlePhonebookCapacityChanged(capacity);
                }
                break;
            case EVENT_PHONEBOOK_RECORDS_RECEIVED:
                logd("EVENT_PHONEBOOK_RECORDS_RECEIVED");
                ar = (AsyncResult)msg.obj;
                if (ar.exception != null) {
                    loge("Unexpected exception happened");
                    ar.result = null;
                }

                handlePhonebookRecordReceived((ReceivedPhonebookRecords)(ar.result));
                break;
            case EVENT_UPDATE_PHONEBOOK_RECORD_DONE:
                logd("EVENT_UPDATE_PHONEBOOK_RECORD_DONE");
                ar = (AsyncResult)msg.obj;
                handleUpdatePhonebookRecordDone(ar);
                break;
            case EVENT_SIM_REFRESH:
                logd("EVENT_SIM_REFRESH");
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    handleSimRefresh((IccRefreshResponse)ar.result);
                } else {
                    logd("SIM refresh Exception: " + ar.exception);
                }
                break;
            case EVENT_GET_PHONEBOOK_RECORDS_RETRY:
                int retryCount = msg.arg1;
                logd("EVENT_GET_PHONEBOOK_RECORDS_RETRY cnt = " + retryCount);
                if (retryCount < MAX_RETRY_COUNT) {
                    mIsRecordLoading.set(false);
                    fillCacheWithoutWaiting();
                    sendGettingPhonebookRecordsRetry(++retryCount);
                } else {
                    responseToWaitersWithErrorOrSuccess(false);
                }
                break;
            default:
                loge("Unexpected event: " + msg.what);
        }

    }

    private void responseToWaitersWithErrorOrSuccess(boolean success) {
        logd("responseToWaitersWithErrorOrSuccess success = " + success);
        mIsRecordLoading.set(false);
        mIsInRetry.set(false);
        if (success) {
            notifyAdnLoadingWaiters();
        } else {
            sendResponsesToWaitersWithError();

        }
        tryFireUpdatePendingList();
    }

    private void handlePhonebookChanged() {
        if (mUpdateRequests.isEmpty()) {
            // If this event is received, means this feature is supported.
            getSimPhonebookCapacity();
        } else {
            logd("Do nothing in the midst of multiple update");
        }
    }

    private void handlePhonebookCapacityChanged(AdnCapacity newCapacity) {
        AdnCapacity oldCapacity = mAdnCapacity.get();
        mAdnCapacity.set(newCapacity);
        if (oldCapacity == null && newCapacity != null) {
            if (newCapacity.getMaxAdnCount() > 0){
                mSimPbRecords.clear();
                fillCacheWithoutWaiting();
            } else {
                notifyAdnLoadingWaiters();
            }
            mIsInitialized.set(true); // Let's say the whole process is ready
        } else {
            // There is nothing from PB, so notify waiters directly if any
            if (newCapacity != null && newCapacity.getMaxAdnCount() == 0) {
                notifyAdnLoadingWaiters();
            } else if (!mIsUpdateDone) {
                invalidateSimPbCache();
                fillCacheWithoutWaiting();
            }
            mIsUpdateDone = false;
        }
    }

    private void handlePhonebookRecordReceived(ReceivedPhonebookRecords records) {
        if (records != null) {
            if (records.isOk()) {
                logd("Partial data is received");
                populateAdnRecords(records.getPhonebookRecords());
            } else if (records.isCompleted()) {
                logd("The whole loading process is finished");
                populateAdnRecords(records.getPhonebookRecords());
                mIsRecordLoading.set(false);
                mIsInRetry.set(false);
                notifyAdnLoadingWaiters();
                tryFireUpdatePendingList();
            } else if (records.isRetryNeeded() && !mIsInRetry.get()) {
                logd("Start to retry as aborted");
                sendGettingPhonebookRecordsRetry(0);
            } else {
                loge("Error happened");
                // Let's keep the stale data, in example of SIM getting removed during loading,
                // expects to finish the whole process.
                responseToWaitersWithErrorOrSuccess(true);
            }
        } else {
            loge("No records there");
            responseToWaitersWithErrorOrSuccess(true);
        }
    }

    private void handleUpdatePhonebookRecordDone(AsyncResult ar) {
        Exception e = null;
        UpdateRequest updateRequest = (UpdateRequest)ar.userObj;
        mIsUpdateDone = true;
        if (ar.exception == null) {
            int index = updateRequest.index;
            AdnRecord adn = updateRequest.adnRecord;
            int recordIndex = ((int[]) (ar.result))[0];

            if (index == 0) {
                // add contact
                addSimPbRecord(adn, recordIndex);
            } else if (adn.isEmpty()){
                // delete contact
                AdnRecord deletedRecord = mSimPbRecords.get(index - 1);
                int adnRecordIndex = deletedRecord.getRecId();
                logd("Record number for deleted ADN is " + adnRecordIndex);
                if(recordIndex == adnRecordIndex) {
                    deleteSimPbRecord(index);
                } else {
                    e = new RuntimeException(
                            "The index for deleted ADN record did not match");
                }
            } else {
                // Change contact
                if (mSimPbRecords.size() > index - 1) {
                    AdnRecord oldRecord = mSimPbRecords.get(index - 1);
                    int adnRecordIndex = oldRecord.getRecId();
                    logd("Record number for changed ADN is " + adnRecordIndex);
                    if(recordIndex == adnRecordIndex) {
                        updateSimPbRecord(adn, recordIndex, index);
                    } else {
                        e = new RuntimeException(
                                "The index for changed ADN record did not match");
                    }
                } else {
                    e = new RuntimeException(
                            "The index for changed ADN record is out of the border");
                }
            }
        } else {
            e = new RuntimeException("Update adn record failed", ar.exception);
        }

        if (mUpdateRequests.contains(updateRequest)) {
            mUpdateRequests.remove(updateRequest);
            updateRequest.responseResult(e);
        } else {
            loge("this update request isn't found");
        }
        tryFireUpdatePendingList();
    }

    private void tryFireUpdatePendingList() {
        if (!mUpdateRequests.isEmpty()) {
            updateSimPhonebook(mUpdateRequests.get(0));
        }
    }

    private void handleSimRefresh(IccRefreshResponse iccRefreshResponse) {
        if (iccRefreshResponse != null) {
            if (iccRefreshResponse.refreshResult == IccRefreshResponse.REFRESH_RESULT_FILE_UPDATE
                    && (iccRefreshResponse.efId == IccConstants.EF_PBR ||
                    iccRefreshResponse.efId == IccConstants.EF_ADN) ||
                    iccRefreshResponse.refreshResult == IccRefreshResponse.REFRESH_RESULT_INIT) {
                invalidateSimPbCache();
                getSimPhonebookCapacity();
            }
        } else {
            logd("IccRefreshResponse received is null");
        }
    }

    private void populateAdnRecords(List<SimPhonebookRecord> records) {
        if (records != null) {
            List<AdnRecord> newRecords = records.stream().map(record -> {return
                    new AdnRecord(0, // PBR or ADN
                    record.getRecordIndex(),
                    record.getAlphaTag(),
                    record.getNumber(),
                    record.getEmails(),
                    record.getAdditionalNumbers());}).collect(Collectors.toList());
            mSimPbRecords.addAll(newRecords);
        }
    }

    private void sendGettingPhonebookRecordsRetry (int times) {
        if (hasMessages(EVENT_GET_PHONEBOOK_RECORDS_RETRY)) {
            removeMessages(EVENT_GET_PHONEBOOK_RECORDS_RETRY);
        }
        mIsInRetry.set(true);
        Message message = obtainMessage(EVENT_GET_PHONEBOOK_RECORDS_RETRY, 1, 0);
        sendMessageDelayed(message, RETRY_INTERVAL);
    }

    private void addSimPbRecord(AdnRecord addedRecord, int recordIndex) {
        logd("Record number for the added ADN is " + recordIndex);
        addedRecord.setRecId(recordIndex);
        mSimPbRecords.add(addedRecord);
    }


    private void deleteSimPbRecord(int index) {
        logd("Record number for the deleted ADN is " + index);
        mSimPbRecords.remove(index - 1);
    }

    private void updateSimPbRecord(AdnRecord newRecord,
            int recordIndex, int index) {
        logd("Record number for the updated ADN is " + recordIndex);
        newRecord.setRecId(recordIndex);
        mSimPbRecords.set(index - 1, newRecord);
    }

    private void invalidateSimPbCache() {
        logd("invalidateSimPbCache");
        mIsCacheInvalidated.set(true);
        mSimPbRecords.clear();
    }

    private void logd(String msg) {
        if (DBG) {
            Rlog.d(LOG_TAG, msg);
        }
    }

    private void loge(String msg) {
        if (DBG) {
            Rlog.e(LOG_TAG, msg);
        }
    }

    private final static class UpdateRequest {
        private int index;
        private Message response;
        private AdnRecord adnRecord;
        private SimPhonebookRecord phonebookRecord;

        UpdateRequest(int index, AdnRecord record, SimPhonebookRecord phonebookRecord,
                Message response) {
            this.index = index;
            this.adnRecord = record;
            this.phonebookRecord = phonebookRecord;
            this.response = response;
        }

        void responseResult(Exception e) {
            if (response != null) {
                AsyncResult.forMessage(response, null, e);
                response.sendToTarget();
            }
        }
    }
}

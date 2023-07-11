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

package com.android.internal.telephony;

import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CARRIER_INFO_IMSI_ENCRYPTION;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_RECORDS_RECEIVED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SIM_REFRESH;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_EVENT_NOTIFY;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_PROACTIVE_COMMAND;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_SESSION_END;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED;

import android.hardware.radio.sim.IRadioSimIndication;
import android.os.AsyncResult;

import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.ReceivedPhonebookRecords;
import com.android.internal.telephony.uicc.SimPhonebookRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface declaring unsolicited radio indications for SIM APIs.
 */
public class SimIndication extends IRadioSimIndication.Stub {
    private final RIL mRil;

    public SimIndication(RIL ril) {
        mRil = ril;
    }

    /**
     * Indicates when the carrier info to encrypt IMSI is being requested.
     * @param indicationType Type of radio indication
     */
    public void carrierInfoForImsiEncryption(int indicationType) {
        mRil.processIndication(RIL.SIM_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CARRIER_INFO_IMSI_ENCRYPTION, null);

        mRil.mCarrierInfoForImsiEncryptionRegistrants.notifyRegistrants(
                new AsyncResult(null, null, null));
    }

    /**
     * Indicates when CDMA subscription source changed.
     * @param indicationType Type of radio indication
     * @param cdmaSource New CdmaSubscriptionSource
     */
    public void cdmaSubscriptionSourceChanged(int indicationType, int cdmaSource) {
        mRil.processIndication(RIL.SIM_SERVICE, indicationType);

        int[] response = new int[]{cdmaSource};
        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED, response);

        mRil.mCdmaSubscriptionChangedRegistrants.notifyRegistrants(
                new AsyncResult(null, response, null));
    }

    /**
     * Indicates when the phonebook is changed.
     * @param indicationType Type of radio indication
     */
    public void simPhonebookChanged(int indicationType) {
        mRil.processIndication(RIL.SIM_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) {
            mRil.unsljLog(RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_CHANGED);
        }

        mRil.mSimPhonebookChangedRegistrants.notifyRegistrants();
    }

    /**
     * Indicates the content of all the used records in the SIM phonebook.
     * @param indicationType Type of radio indication
     * @param status Status of PbReceivedStatus
     * @param records Content of the SIM phonebook records
     */
    public void simPhonebookRecordsReceived(int indicationType, byte status,
            android.hardware.radio.sim.PhonebookRecordInfo[] records) {
        mRil.processIndication(RIL.SIM_SERVICE, indicationType);

        List<SimPhonebookRecord> simPhonebookRecords = new ArrayList<>();

        for (android.hardware.radio.sim.PhonebookRecordInfo record : records) {
            simPhonebookRecords.add(RILUtils.convertHalPhonebookRecordInfo(record));
        }

        if (RIL.RILJ_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_RECORDS_RECEIVED,
                    "status = " + status + " received " + records.length + " records");
        }

        mRil.mSimPhonebookRecordsReceivedRegistrants.notifyRegistrants(new AsyncResult(
                null, new ReceivedPhonebookRecords(status, simPhonebookRecords), null));
    }

    /**
     * Indicates that file(s) on the SIM have been updated, or the SIM has been reinitialized.
     * @param indicationType Type of radio indication
     * @param refreshResult Result of SIM refresh
     */
    public void simRefresh(int indicationType,
            android.hardware.radio.sim.SimRefreshResult refreshResult) {
        mRil.processIndication(RIL.SIM_SERVICE, indicationType);

        IccRefreshResponse response = new IccRefreshResponse();
        response.refreshResult = refreshResult.type;
        response.efId = refreshResult.efId;
        response.aid = refreshResult.aid;

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_SIM_REFRESH, response);

        mRil.mIccRefreshRegistrants.notifyRegistrants(new AsyncResult(null, response, null));
    }

    /**
     * Indicates that SIM state changed.
     * @param indicationType Type of radio indication
     */
    public void simStatusChanged(int indicationType) {
        mRil.processIndication(RIL.SIM_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED);

        mRil.mIccStatusChangedRegistrants.notifyRegistrants();
    }

    /**
     * Indicates when SIM notifies applications some event happens.
     * @param indicationType Type of radio indication
     * @param cmd SAT/USAT commands or responses sent by ME to SIM or commands handled by ME,
     *        represented as byte array starting with first byte of response data for command tag.
     *        Refer to TS 102.223 section 9.4 for command types
     */
    public void stkEventNotify(int indicationType, String cmd) {
        mRil.processIndication(RIL.SIM_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_STK_EVENT_NOTIFY);

        if (mRil.mCatEventRegistrant != null) {
            mRil.mCatEventRegistrant.notifyRegistrant(new AsyncResult(null, cmd, null));
        }
    }

    /**
     * Indicates when SIM issue a STK proactive command to applications.
     * @param indicationType Type of radio indication
     * @param cmd SAT/USAT proactive represented as byte array starting with command tag.
     *        Refer to TS 102.223 section 9.4 for command types
     */
    public void stkProactiveCommand(int indicationType, String cmd) {
        mRil.processIndication(RIL.SIM_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_STK_PROACTIVE_COMMAND);

        if (mRil.mCatProCmdRegistrant != null) {
            mRil.mCatProCmdRegistrant.notifyRegistrant(new AsyncResult(null, cmd, null));
        }
    }

    /**
     * Indicates when STK session is terminated by SIM.
     * @param indicationType Type of radio indication
     */
    public void stkSessionEnd(int indicationType) {
        mRil.processIndication(RIL.SIM_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_STK_SESSION_END);

        if (mRil.mCatSessionEndRegistrant != null) {
            mRil.mCatSessionEndRegistrant.notifyRegistrant(new AsyncResult(null, null, null));
        }
    }

    /**
     * Indicated when there is a change in subscription status.
     * @param indicationType Type of radio indication
     * @param activate false for subscription deactivated, true for subscription activated
     */
    public void subscriptionStatusChanged(int indicationType, boolean activate) {
        mRil.processIndication(RIL.SIM_SERVICE, indicationType);

        int[] response = new int[]{activate ? 1 : 0};

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED, response);

        mRil.mSubscriptionStatusRegistrants.notifyRegistrants(
                new AsyncResult(null, response, null));
    }

    /**
     * Report change of whether uiccApplications are enabled or disabled.
     * @param indicationType Type of radio indication
     * @param enabled Whether uiccApplications are enabled or disabled
     */
    public void uiccApplicationsEnablementChanged(int indicationType, boolean enabled) {
        mRil.processIndication(RIL.SIM_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED, enabled);
        }

        mRil.mUiccApplicationsEnablementRegistrants.notifyResult(enabled);
    }

    @Override
    public String getInterfaceHash() {
        return IRadioSimIndication.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return IRadioSimIndication.VERSION;
    }
}

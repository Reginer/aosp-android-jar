/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.telephony.TelephonyManager.UNKNOWN_CARRIER_ID;

import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CALL_RING;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CARRIER_INFO_IMSI_ENCRYPTION;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_CALL_WAITING;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_INFO_REC;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_OTA_PROVISION_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_PRL_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CELL_INFO_LIST;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_DATA_CALL_LIST_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_EMERGENCY_NUMBER_LIST;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_HARDWARE_CONFIG_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_KEEPALIVE_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_LCEDATA_RECV;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_MODEM_RESTART;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_NETWORK_SCAN_RESULT;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_NITZ_TIME_RECEIVED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_ON_SS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_ON_USSD;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_PCO_DATA;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_PHYSICAL_CHANNEL_CONFIG;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RADIO_CAPABILITY;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESEND_INCALL_MUTE;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_CDMA_NEW_SMS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NETWORK_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_SMS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_RECORDS_RECEIVED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESTRICTED_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RIL_CONNECTED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RINGBACK_TONE;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SIGNAL_STRENGTH;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SIM_REFRESH;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SIM_SMS_STORAGE_FULL;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SRVCC_STATE_NOTIFY;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_CALL_SETUP;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_CC_ALPHA_NOTIFY;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_EVENT_NOTIFY;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_PROACTIVE_COMMAND;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_SESSION_END;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SUPP_SVC_NOTIFICATION;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_UNTHROTTLE_APN;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_VOICE_RADIO_TECH_CHANGED;

import android.hardware.radio.V1_0.CdmaCallWaiting;
import android.hardware.radio.V1_0.CdmaInformationRecord;
import android.hardware.radio.V1_0.CdmaLineControlInfoRecord;
import android.hardware.radio.V1_0.CdmaNumberInfoRecord;
import android.hardware.radio.V1_0.CdmaRedirectingNumberInfoRecord;
import android.hardware.radio.V1_0.CdmaSignalInfoRecord;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.CdmaT53AudioControlInfoRecord;
import android.hardware.radio.V1_0.CfData;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.PcoDataInfo;
import android.hardware.radio.V1_0.SimRefreshResult;
import android.hardware.radio.V1_0.SsInfoData;
import android.hardware.radio.V1_0.StkCcUnsolSsResult;
import android.hardware.radio.V1_0.SuppSvcNotification;
import android.hardware.radio.V1_6.IRadioIndication;
import android.hardware.radio.V1_6.PhonebookRecordInfo;
import android.hardware.radio.V1_6.PhysicalChannelConfig.Band;
import android.os.AsyncResult;
import android.os.RemoteException;
import android.sysprop.TelephonyProperties;
import android.telephony.AnomalyReporter;
import android.telephony.BarringInfo;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.LinkCapacityEstimate;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PcoData;
import android.telephony.PhysicalChannelConfig;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SmsMessage;
import android.telephony.data.DataCallResponse;
import android.telephony.emergency.EmergencyNumber;
import android.text.TextUtils;

import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.data.KeepaliveStatus;
import com.android.internal.telephony.gsm.SsData;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.ReceivedPhonebookRecords;
import com.android.internal.telephony.uicc.SimPhonebookRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RadioIndication extends IRadioIndication.Stub {
    RIL mRil;

    RadioIndication(RIL ril) {
        mRil = ril;
    }

    /**
     * Indicates when radio state changes.
     * @param indicationType RadioIndicationType
     * @param radioState android.hardware.radio.V1_0.RadioState
     */
    public void radioStateChanged(int indicationType, int radioState) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        int state = RILUtils.convertHalRadioState(radioState);
        if (RIL.RILJ_LOGD) {
            mRil.unsljLogMore(RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED, "radioStateChanged: " +
                    state);
        }

        mRil.setRadioState(state, false /* forceNotifyRegistrants */);
    }

    public void callStateChanged(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED);

        mRil.mCallStateRegistrants.notifyRegistrants();
    }

    /**
     * Indicates when either voice or data network state changed
     * @param indicationType RadioIndicationType
     */
    public void networkStateChanged(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_NETWORK_STATE_CHANGED);

        mRil.mNetworkStateRegistrants.notifyRegistrants();
    }

    public void newSms(int indicationType, ArrayList<Byte> pdu) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        byte[] pduArray = RILUtils.arrayListToPrimitiveArray(pdu);
        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_NEW_SMS);

        SmsMessageBase smsb = com.android.internal.telephony.gsm.SmsMessage.createFromPdu(pduArray);
        if (mRil.mGsmSmsRegistrant != null) {
            mRil.mGsmSmsRegistrant.notifyRegistrant(
                    new AsyncResult(null, smsb == null ? null : new SmsMessage(smsb), null));
        }
    }

    public void newSmsStatusReport(int indicationType, ArrayList<Byte> pdu) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        byte[] pduArray = RILUtils.arrayListToPrimitiveArray(pdu);
        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT);

        if (mRil.mSmsStatusRegistrant != null) {
            mRil.mSmsStatusRegistrant.notifyRegistrant(new AsyncResult(null, pduArray, null));
        }
    }

    public void newSmsOnSim(int indicationType, int recordNumber) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM);

        if (mRil.mSmsOnSimRegistrant != null) {
            mRil.mSmsOnSimRegistrant.notifyRegistrant(new AsyncResult(null, recordNumber, null));
        }
    }

    public void onUssd(int indicationType, int ussdModeType, String msg) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogMore(RIL_UNSOL_ON_USSD, "" + ussdModeType);

        // todo: Clean this up with a parcelable class for better self-documentation
        String[] resp = new String[2];
        resp[0] = "" + ussdModeType;
        resp[1] = msg;
        if (mRil.mUSSDRegistrant != null) {
            mRil.mUSSDRegistrant.notifyRegistrant(new AsyncResult (null, resp, null));
        }
    }

    public void nitzTimeReceived(int indicationType, String nitzTime, long receivedTime) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_NITZ_TIME_RECEIVED, nitzTime);

        // todo: Clean this up with a parcelable class for better self-documentation
        Object[] result = new Object[2];
        result[0] = nitzTime;
        result[1] = receivedTime;

        boolean ignoreNitz = TelephonyProperties.ignore_nitz().orElse(false);

        if (ignoreNitz) {
            if (RIL.RILJ_LOGD) mRil.riljLog("ignoring UNSOL_NITZ_TIME_RECEIVED");
        } else {
            if (mRil.mNITZTimeRegistrant != null) {
                mRil.mNITZTimeRegistrant.notifyRegistrant(new AsyncResult (null, result, null));
            }
            // in case NITZ time registrant isn't registered yet, or a new registrant
            // registers later
            mRil.mLastNITZTimeInfo = result;
        }
    }

    public void currentSignalStrength(int indicationType,
                                      android.hardware.radio.V1_0.SignalStrength signalStrength) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        SignalStrength ssInitial = RILUtils.convertHalSignalStrength(signalStrength);

        SignalStrength ss = mRil.fixupSignalStrength10(ssInitial);
        // Note this is set to "verbose" because it happens frequently
        if (RIL.RILJ_LOGV) mRil.unsljLogvRet(RIL_UNSOL_SIGNAL_STRENGTH, ss);

        if (mRil.mSignalStrengthRegistrant != null) {
            mRil.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult (null, ss, null));
        }
    }

    /**
     * Indicates current link capacity estimate.
     */
    public void currentLinkCapacityEstimate(int indicationType,
                                            android.hardware.radio.V1_2.LinkCapacityEstimate lce) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        List<LinkCapacityEstimate> response = RILUtils.convertHalLceData(lce);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_LCEDATA_RECV, response);

        if (mRil.mLceInfoRegistrants != null) {
            mRil.mLceInfoRegistrants.notifyRegistrants(new AsyncResult(null, response, null));
        }
    }

    /**
     * Indicates current link capacity estimate.
     */
    public void currentLinkCapacityEstimate_1_6(int indicationType,
            android.hardware.radio.V1_6.LinkCapacityEstimate lce) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        List<LinkCapacityEstimate> response = RILUtils.convertHalLceData(lce);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_LCEDATA_RECV, response);

        if (mRil.mLceInfoRegistrants != null) {
            mRil.mLceInfoRegistrants.notifyRegistrants(new AsyncResult(null, response, null));
        }
    }

    /**
     * Indicates the current signal strength of the camped or primary serving cell.
     */
    public void currentSignalStrength_1_2(int indicationType,
                                      android.hardware.radio.V1_2.SignalStrength signalStrength) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        SignalStrength ss = RILUtils.convertHalSignalStrength(signalStrength);
        // Note this is set to "verbose" because it happens frequently
        if (RIL.RILJ_LOGV) mRil.unsljLogvRet(RIL_UNSOL_SIGNAL_STRENGTH, ss);

        if (mRil.mSignalStrengthRegistrant != null) {
            mRil.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult(null, ss, null));
        }
    }

    /**
     * Indicates the current signal strength of the camped or primary serving cell.
     */
    public void currentSignalStrength_1_4(int indicationType,
            android.hardware.radio.V1_4.SignalStrength signalStrength) {

        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        SignalStrength ss = RILUtils.convertHalSignalStrength(signalStrength);

        if (RIL.RILJ_LOGV) mRil.unsljLogvRet(RIL_UNSOL_SIGNAL_STRENGTH, ss);

        if (mRil.mSignalStrengthRegistrant != null) {
            mRil.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult(null, ss, null));
        }
    }

    /**
     * Indicates the current signal strength of the camped or primary serving cell.
     */
    public void currentSignalStrength_1_6(int indicationType,
            android.hardware.radio.V1_6.SignalStrength signalStrength) {

        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        SignalStrength ss = RILUtils.convertHalSignalStrength(signalStrength);

        if (RIL.RILJ_LOGV) mRil.unsljLogvRet(RIL_UNSOL_SIGNAL_STRENGTH, ss);

        if (mRil.mSignalStrengthRegistrant != null) {
            mRil.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult(null, ss, null));
        }
    }

    /**
     * Indicates current physical channel configuration.
     */
    public void currentPhysicalChannelConfigs_1_4(int indicationType,
            ArrayList<android.hardware.radio.V1_4.PhysicalChannelConfig> configs) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);
        physicalChannelConfigsIndication(configs);
    }

    /**
     * Indicates current physical channel configuration.
     */
    public void currentPhysicalChannelConfigs_1_6(int indicationType,
            ArrayList<android.hardware.radio.V1_6.PhysicalChannelConfig> configs) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);
        physicalChannelConfigsIndication(configs);
    }

    /**
     * Indicates current physical channel configuration.
     */
    public void currentPhysicalChannelConfigs(int indicationType,
            ArrayList<android.hardware.radio.V1_2.PhysicalChannelConfig> configs) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);
        physicalChannelConfigsIndication(configs);
    }

    /**
     * Indicates current emergency number list.
     */
    public void currentEmergencyNumberList(int indicationType,
            ArrayList<android.hardware.radio.V1_4.EmergencyNumber> emergencyNumberList) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);
        List<EmergencyNumber> response = new ArrayList<>(emergencyNumberList.size());

        for (android.hardware.radio.V1_4.EmergencyNumber emergencyNumberHal
                : emergencyNumberList) {
            EmergencyNumber emergencyNumber = new EmergencyNumber(emergencyNumberHal.number,
                    MccTable.countryCodeForMcc(emergencyNumberHal.mcc), emergencyNumberHal.mnc,
                    emergencyNumberHal.categories, emergencyNumberHal.urns,
                    emergencyNumberHal.sources, EmergencyNumber.EMERGENCY_CALL_ROUTING_UNKNOWN);
            response.add(emergencyNumber);
        }

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_EMERGENCY_NUMBER_LIST, response);

        // Cache emergency number list from last indication.
        mRil.cacheEmergencyNumberListIndication(response);

        // Notify emergency number list from radio to registrants
        mRil.mEmergencyNumberListRegistrants.notifyRegistrants(
                new AsyncResult(null, response, null));
    }

    /** Indicates current data call list. */
    public void dataCallListChanged(int indicationType,
            ArrayList<android.hardware.radio.V1_0.SetupDataCallResult> dcList) {
        responseDataCallListChanged(indicationType, dcList);
    }

    /** Indicates current data call list with radio HAL 1.4. */
    public void dataCallListChanged_1_4(int indicationType,
            ArrayList<android.hardware.radio.V1_4.SetupDataCallResult> dcList) {
        responseDataCallListChanged(indicationType, dcList);

    }

    /** Indicates current data call list with radio HAL 1.5. */
    public void dataCallListChanged_1_5(int indicationType,
            ArrayList<android.hardware.radio.V1_5.SetupDataCallResult> dcList) {
        responseDataCallListChanged(indicationType, dcList);
    }

    /** Indicates current data call list with radio HAL 1.6. */
    public void dataCallListChanged_1_6(int indicationType,
            ArrayList<android.hardware.radio.V1_6.SetupDataCallResult> dcList) {
        responseDataCallListChanged(indicationType, dcList);
    }

    @Override
    public void unthrottleApn(int indicationType, String apn)
            throws RemoteException {
        responseApnUnthrottled(indicationType, apn);
    }

    public void suppSvcNotify(int indicationType, SuppSvcNotification suppSvcNotification) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        SuppServiceNotification notification = new SuppServiceNotification();
        notification.notificationType = suppSvcNotification.isMT ? 1 : 0;
        notification.code = suppSvcNotification.code;
        notification.index = suppSvcNotification.index;
        notification.type = suppSvcNotification.type;
        notification.number = suppSvcNotification.number;

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_SUPP_SVC_NOTIFICATION, notification);

        if (mRil.mSsnRegistrant != null) {
            mRil.mSsnRegistrant.notifyRegistrant(new AsyncResult (null, notification, null));
        }
    }

    public void stkSessionEnd(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_STK_SESSION_END);

        if (mRil.mCatSessionEndRegistrant != null) {
            mRil.mCatSessionEndRegistrant.notifyRegistrant(new AsyncResult (null, null, null));
        }
    }

    public void stkProactiveCommand(int indicationType, String cmd) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_STK_PROACTIVE_COMMAND);

        if (mRil.mCatProCmdRegistrant != null) {
            mRil.mCatProCmdRegistrant.notifyRegistrant(new AsyncResult (null, cmd, null));
        }
    }

    public void stkEventNotify(int indicationType, String cmd) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_STK_EVENT_NOTIFY);

        if (mRil.mCatEventRegistrant != null) {
            mRil.mCatEventRegistrant.notifyRegistrant(new AsyncResult (null, cmd, null));
        }
    }

    public void stkCallSetup(int indicationType, long timeout) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_STK_CALL_SETUP, timeout);

        if (mRil.mCatCallSetUpRegistrant != null) {
            mRil.mCatCallSetUpRegistrant.notifyRegistrant(new AsyncResult (null, timeout, null));
        }
    }

    public void simSmsStorageFull(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_SIM_SMS_STORAGE_FULL);

        if (mRil.mIccSmsFullRegistrant != null) {
            mRil.mIccSmsFullRegistrant.notifyRegistrant();
        }
    }

    public void simRefresh(int indicationType, SimRefreshResult refreshResult) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        IccRefreshResponse response = new IccRefreshResponse();
        response.refreshResult = refreshResult.type;
        response.efId = refreshResult.efId;
        response.aid = refreshResult.aid;

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_SIM_REFRESH, response);

        mRil.mIccRefreshRegistrants.notifyRegistrants(new AsyncResult (null, response, null));
    }

    public void callRing(int indicationType, boolean isGsm, CdmaSignalInfoRecord record) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        char response[] = null;

        // Ignore record for gsm
        if (!isGsm) {
            // todo: Clean this up with a parcelable class for better self-documentation
            response = new char[4];
            response[0] = (char) (record.isPresent ? 1 : 0);
            response[1] = (char) record.signalType;
            response[2] = (char) record.alertPitch;
            response[3] = (char) record.signal;
            mRil.writeMetricsCallRing(response);
        }

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CALL_RING, response);

        if (mRil.mRingRegistrant != null) {
            mRil.mRingRegistrant.notifyRegistrant(new AsyncResult (null, response, null));
        }
    }

    public void simStatusChanged(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED);

        mRil.mIccStatusChangedRegistrants.notifyRegistrants();
    }

    public void cdmaNewSms(int indicationType, CdmaSmsMessage msg) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_CDMA_NEW_SMS);

        SmsMessage sms = new SmsMessage(RILUtils.convertHalCdmaSmsMessage(msg));
        if (mRil.mCdmaSmsRegistrant != null) {
            mRil.mCdmaSmsRegistrant.notifyRegistrant(new AsyncResult(null, sms, null));
        }
    }

    public void newBroadcastSms(int indicationType, ArrayList<Byte> data) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        byte[] response = RILUtils.arrayListToPrimitiveArray(data);
        if (RIL.RILJ_LOGD) {
            mRil.unsljLogvRet(RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS,
                    IccUtils.bytesToHexString(response));
        }

        if (mRil.mGsmBroadcastSmsRegistrant != null) {
            mRil.mGsmBroadcastSmsRegistrant.notifyRegistrant(new AsyncResult(null, response, null));
        }
    }

    public void cdmaRuimSmsStorageFull(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL);

        if (mRil.mIccSmsFullRegistrant != null) {
            mRil.mIccSmsFullRegistrant.notifyRegistrant();
        }
    }

    public void restrictedStateChanged(int indicationType, int state) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogvRet(RIL_UNSOL_RESTRICTED_STATE_CHANGED, state);

        if (mRil.mRestrictedStateRegistrant != null) {
            mRil.mRestrictedStateRegistrant.notifyRegistrant(new AsyncResult (null, state, null));
        }
    }

    public void enterEmergencyCallbackMode(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE);

        if (mRil.mEmergencyCallbackModeRegistrant != null) {
            mRil.mEmergencyCallbackModeRegistrant.notifyRegistrant();
        }
    }

    public void cdmaCallWaiting(int indicationType, CdmaCallWaiting callWaitingRecord) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        // todo: create a CdmaCallWaitingNotification constructor that takes in these fields to make
        // sure no fields are missing
        CdmaCallWaitingNotification notification = new CdmaCallWaitingNotification();
        notification.number = callWaitingRecord.number;
        notification.numberPresentation = CdmaCallWaitingNotification.presentationFromCLIP(
                callWaitingRecord.numberPresentation);
        notification.name = callWaitingRecord.name;
        notification.namePresentation = notification.numberPresentation;
        notification.isPresent = callWaitingRecord.signalInfoRecord.isPresent ? 1 : 0;
        notification.signalType = callWaitingRecord.signalInfoRecord.signalType;
        notification.alertPitch = callWaitingRecord.signalInfoRecord.alertPitch;
        notification.signal = callWaitingRecord.signalInfoRecord.signal;
        notification.numberType = callWaitingRecord.numberType;
        notification.numberPlan = callWaitingRecord.numberPlan;

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CDMA_CALL_WAITING, notification);

        mRil.mCallWaitingInfoRegistrants.notifyRegistrants(
                new AsyncResult (null, notification, null));
    }

    public void cdmaOtaProvisionStatus(int indicationType, int status) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        int response[] = new int[1];
        response[0] = status;

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CDMA_OTA_PROVISION_STATUS, response);

        mRil.mOtaProvisionRegistrants.notifyRegistrants(new AsyncResult (null, response, null));
    }

    public void cdmaInfoRec(int indicationType,
                            android.hardware.radio.V1_0.CdmaInformationRecords records) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        int numberOfInfoRecs = records.infoRec.size();
        for (int i = 0; i < numberOfInfoRecs; i++) {
            CdmaInformationRecord record = records.infoRec.get(i);
            int id = record.name;
            CdmaInformationRecords cdmaInformationRecords;
            switch (id) {
                case CdmaInformationRecords.RIL_CDMA_DISPLAY_INFO_REC:
                case CdmaInformationRecords.RIL_CDMA_EXTENDED_DISPLAY_INFO_REC:
                    CdmaInformationRecords.CdmaDisplayInfoRec cdmaDisplayInfoRec =
                            new CdmaInformationRecords.CdmaDisplayInfoRec(id,
                            record.display.get(0).alphaBuf);
                    cdmaInformationRecords = new CdmaInformationRecords(cdmaDisplayInfoRec);
                    break;

                case CdmaInformationRecords.RIL_CDMA_CALLED_PARTY_NUMBER_INFO_REC:
                case CdmaInformationRecords.RIL_CDMA_CALLING_PARTY_NUMBER_INFO_REC:
                case CdmaInformationRecords.RIL_CDMA_CONNECTED_NUMBER_INFO_REC:
                    CdmaNumberInfoRecord numInfoRecord = record.number.get(0);
                    CdmaInformationRecords.CdmaNumberInfoRec cdmaNumberInfoRec =
                            new CdmaInformationRecords.CdmaNumberInfoRec(id,
                            numInfoRecord.number,
                            numInfoRecord.numberType,
                            numInfoRecord.numberPlan,
                            numInfoRecord.pi,
                            numInfoRecord.si);
                    cdmaInformationRecords = new CdmaInformationRecords(cdmaNumberInfoRec);
                    break;

                case CdmaInformationRecords.RIL_CDMA_SIGNAL_INFO_REC:
                    CdmaSignalInfoRecord signalInfoRecord = record.signal.get(0);
                    CdmaInformationRecords.CdmaSignalInfoRec cdmaSignalInfoRec =
                            new CdmaInformationRecords.CdmaSignalInfoRec(
                            signalInfoRecord.isPresent ? 1 : 0,
                            signalInfoRecord.signalType,
                            signalInfoRecord.alertPitch,
                            signalInfoRecord.signal);
                    cdmaInformationRecords = new CdmaInformationRecords(cdmaSignalInfoRec);
                    break;

                case CdmaInformationRecords.RIL_CDMA_REDIRECTING_NUMBER_INFO_REC:
                    CdmaRedirectingNumberInfoRecord redirectingNumberInfoRecord =
                            record.redir.get(0);
                    CdmaInformationRecords.CdmaRedirectingNumberInfoRec
                            cdmaRedirectingNumberInfoRec =
                            new CdmaInformationRecords.CdmaRedirectingNumberInfoRec(
                            redirectingNumberInfoRecord.redirectingNumber.number,
                            redirectingNumberInfoRecord.redirectingNumber.numberType,
                            redirectingNumberInfoRecord.redirectingNumber.numberPlan,
                            redirectingNumberInfoRecord.redirectingNumber.pi,
                            redirectingNumberInfoRecord.redirectingNumber.si,
                            redirectingNumberInfoRecord.redirectingReason);
                    cdmaInformationRecords = new CdmaInformationRecords(
                            cdmaRedirectingNumberInfoRec);
                    break;

                case CdmaInformationRecords.RIL_CDMA_LINE_CONTROL_INFO_REC:
                    CdmaLineControlInfoRecord lineControlInfoRecord = record.lineCtrl.get(0);
                    CdmaInformationRecords.CdmaLineControlInfoRec cdmaLineControlInfoRec =
                            new CdmaInformationRecords.CdmaLineControlInfoRec(
                            lineControlInfoRecord.lineCtrlPolarityIncluded,
                            lineControlInfoRecord.lineCtrlToggle,
                            lineControlInfoRecord.lineCtrlReverse,
                            lineControlInfoRecord.lineCtrlPowerDenial);
                    cdmaInformationRecords = new CdmaInformationRecords(cdmaLineControlInfoRec);
                    break;

                case CdmaInformationRecords.RIL_CDMA_T53_CLIR_INFO_REC:
                    CdmaInformationRecords.CdmaT53ClirInfoRec cdmaT53ClirInfoRec =
                            new CdmaInformationRecords.CdmaT53ClirInfoRec(record.clir.get(0).cause);
                    cdmaInformationRecords = new CdmaInformationRecords(cdmaT53ClirInfoRec);
                    break;

                case CdmaInformationRecords.RIL_CDMA_T53_AUDIO_CONTROL_INFO_REC:
                    CdmaT53AudioControlInfoRecord audioControlInfoRecord = record.audioCtrl.get(0);
                    CdmaInformationRecords.CdmaT53AudioControlInfoRec cdmaT53AudioControlInfoRec =
                            new CdmaInformationRecords.CdmaT53AudioControlInfoRec(
                            audioControlInfoRecord.upLink,
                            audioControlInfoRecord.downLink);
                    cdmaInformationRecords = new CdmaInformationRecords(cdmaT53AudioControlInfoRec);
                    break;

                default:
                    throw new RuntimeException("RIL_UNSOL_CDMA_INFO_REC: unsupported record. Got "
                            + CdmaInformationRecords.idToString(id) + " ");
            }

            if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CDMA_INFO_REC, cdmaInformationRecords);
            mRil.notifyRegistrantsCdmaInfoRec(cdmaInformationRecords);
        }
    }

    public void indicateRingbackTone(int indicationType, boolean start) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogvRet(RIL_UNSOL_RINGBACK_TONE, start);

        mRil.mRingbackToneRegistrants.notifyRegistrants(new AsyncResult(null, start, null));
    }

    public void resendIncallMute(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESEND_INCALL_MUTE);

        mRil.mResendIncallMuteRegistrants.notifyRegistrants();
    }

    public void cdmaSubscriptionSourceChanged(int indicationType, int cdmaSource) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        int response[] = new int[1];
        response[0] = cdmaSource;

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED, response);

        mRil.mCdmaSubscriptionChangedRegistrants.notifyRegistrants(
                new AsyncResult (null, response, null));
    }

    public void cdmaPrlChanged(int indicationType, int version) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        int response[] = new int[1];
        response[0] = version;

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CDMA_PRL_CHANGED, response);

        mRil.mCdmaPrlChangedRegistrants.notifyRegistrants(
                new AsyncResult (null, response, null));
    }

    public void exitEmergencyCallbackMode(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE);

        mRil.mExitEmergencyCallbackModeRegistrants.notifyRegistrants();
    }

    public void rilConnected(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RIL_CONNECTED);

        // Initial conditions
        mRil.setRadioPower(false, null);
        mRil.setCdmaSubscriptionSource(mRil.mCdmaSubscription, null);
        // todo: this should not require a version number now. Setting it to latest RIL version for
        // now.
        mRil.notifyRegistrantsRilConnectionChanged(15);
    }

    public void voiceRadioTechChanged(int indicationType, int rat) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        int response[] = new int[1];
        response[0] = rat;

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_VOICE_RADIO_TECH_CHANGED, response);

        mRil.mVoiceRadioTechChangedRegistrants.notifyRegistrants(
                new AsyncResult (null, response, null));
    }

    /** Get unsolicited message for cellInfoList */
    public void cellInfoList(int indicationType,
            ArrayList<android.hardware.radio.V1_0.CellInfo> records) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);
        responseCellInfoList(records);
    }

    /** Get unsolicited message for cellInfoList using HAL V1_2 */
    public void cellInfoList_1_2(int indicationType,
            ArrayList<android.hardware.radio.V1_2.CellInfo> records) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);
        responseCellInfoList(records);
    }

    /** Get unsolicited message for cellInfoList using HAL V1_4 */
    public void cellInfoList_1_4(int indicationType,
            ArrayList<android.hardware.radio.V1_4.CellInfo> records) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);
        responseCellInfoList(records);
    }

    /** Get unsolicited message for cellInfoList using HAL V1_5 */
    public void cellInfoList_1_5(int indicationType,
            ArrayList<android.hardware.radio.V1_5.CellInfo> records) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);
        responseCellInfoList(records);
    }

    /** Get unsolicited message for cellInfoList using HAL V1_5 */
    public void cellInfoList_1_6(int indicationType,
            ArrayList<android.hardware.radio.V1_6.CellInfo> records) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);
        responseCellInfoList(records);
    }

    private void responseCellInfoList(ArrayList<? extends Object> records) {
        ArrayList<CellInfo> response = RILUtils.convertHalCellInfoList((ArrayList<Object>) records);
        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CELL_INFO_LIST, response);
        mRil.mRilCellInfoListRegistrants.notifyRegistrants(new AsyncResult(null, response, null));
    }

    /** Get unsolicited message for uicc applications enablement changes. */
    public void uiccApplicationsEnablementChanged(int indicationType, boolean enabled) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED, enabled);
        }

        mRil.mUiccApplicationsEnablementRegistrants.notifyResult(enabled);
    }

    /** Incremental network scan results */
    public void networkScanResult(int indicationType,
                                  android.hardware.radio.V1_1.NetworkScanResult result) {
        responseNetworkScan(indicationType, result);
    }

    /** Incremental network scan results with HAL V1_2 */
    public void networkScanResult_1_2(int indicationType,
                                      android.hardware.radio.V1_2.NetworkScanResult result) {
        responseNetworkScan_1_2(indicationType, result);
    }

    /** Incremental network scan results with HAL V1_4 */
    public void networkScanResult_1_4(int indicationType,
                                      android.hardware.radio.V1_4.NetworkScanResult result) {
        responseNetworkScan_1_4(indicationType, result);
    }

    /** Incremental network scan results with HAL V1_5 */
    public void networkScanResult_1_5(int indicationType,
            android.hardware.radio.V1_5.NetworkScanResult result) {
        responseNetworkScan_1_5(indicationType, result);
    }

    /** Incremental network scan results with HAL V1_6 */
    public void networkScanResult_1_6(int indicationType,
            android.hardware.radio.V1_6.NetworkScanResult result) {
        responseNetworkScan_1_6(indicationType, result);
    }

    public void imsNetworkStateChanged(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED);

        mRil.mImsNetworkStateChangedRegistrants.notifyRegistrants();
    }

    public void subscriptionStatusChanged(int indicationType, boolean activate) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        int response[] = new int[1];
        response[0] = activate ? 1 : 0;

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED, response);

        mRil.mSubscriptionStatusRegistrants.notifyRegistrants(
                new AsyncResult (null, response, null));
    }

    public void srvccStateNotify(int indicationType, int state) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        int response[] = new int[1];
        response[0] = state;

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_SRVCC_STATE_NOTIFY, response);

        mRil.writeMetricsSrvcc(state);

        mRil.mSrvccStateRegistrants.notifyRegistrants(
                new AsyncResult (null, response, null));
    }

    public void hardwareConfigChanged(
            int indicationType,
            ArrayList<android.hardware.radio.V1_0.HardwareConfig> configs) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        ArrayList<HardwareConfig> response = RILUtils.convertHalHardwareConfigList(configs);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_HARDWARE_CONFIG_CHANGED, response);

        mRil.mHardwareConfigChangeRegistrants.notifyRegistrants(
                new AsyncResult (null, response, null));
    }

    public void radioCapabilityIndication(int indicationType,
                                          android.hardware.radio.V1_0.RadioCapability rc) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        RadioCapability response = RILUtils.convertHalRadioCapability(rc, mRil);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_RADIO_CAPABILITY, response);

        mRil.mPhoneRadioCapabilityChangedRegistrants.notifyRegistrants(
                new AsyncResult (null, response, null));
    }

    public void onSupplementaryServiceIndication(int indicationType, StkCcUnsolSsResult ss) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        int num;
        SsData ssData = new SsData();

        ssData.serviceType = ssData.ServiceTypeFromRILInt(ss.serviceType);
        ssData.requestType = ssData.RequestTypeFromRILInt(ss.requestType);
        ssData.teleserviceType = ssData.TeleserviceTypeFromRILInt(ss.teleserviceType);
        ssData.serviceClass = ss.serviceClass; // This is service class sent in the SS request.
        ssData.result = ss.result; // This is the result of the SS request.

        if (ssData.serviceType.isTypeCF() &&
                ssData.requestType.isTypeInterrogation()) {
            CfData cfData = ss.cfData.get(0);
            num = cfData.cfInfo.size();
            ssData.cfInfo = new CallForwardInfo[num];

            for (int i = 0; i < num; i++) {
                android.hardware.radio.V1_0.CallForwardInfo cfInfo = cfData.cfInfo.get(i);
                ssData.cfInfo[i] = new CallForwardInfo();

                ssData.cfInfo[i].status = cfInfo.status;
                ssData.cfInfo[i].reason = cfInfo.reason;
                ssData.cfInfo[i].serviceClass = cfInfo.serviceClass;
                ssData.cfInfo[i].toa = cfInfo.toa;
                ssData.cfInfo[i].number = cfInfo.number;
                ssData.cfInfo[i].timeSeconds = cfInfo.timeSeconds;

                mRil.riljLog("[SS Data] CF Info " + i + " : " +  ssData.cfInfo[i]);
            }
        } else {
            SsInfoData ssInfo = ss.ssInfo.get(0);
            num = ssInfo.ssInfo.size();
            ssData.ssInfo = new int[num];
            for (int i = 0; i < num; i++) {
                ssData.ssInfo[i] = ssInfo.ssInfo.get(i);
                mRil.riljLog("[SS Data] SS Info " + i + " : " +  ssData.ssInfo[i]);
            }
        }

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_ON_SS, ssData);

        if (mRil.mSsRegistrant != null) {
            mRil.mSsRegistrant.notifyRegistrant(new AsyncResult(null, ssData, null));
        }
    }

    public void stkCallControlAlphaNotify(int indicationType, String alpha) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_STK_CC_ALPHA_NOTIFY, alpha);

        if (mRil.mCatCcAlphaRegistrant != null) {
            mRil.mCatCcAlphaRegistrant.notifyRegistrant(new AsyncResult (null, alpha, null));
        }
    }

    public void lceData(int indicationType, LceDataInfo lce) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        List<LinkCapacityEstimate> response = RILUtils.convertHalLceData(lce);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_LCEDATA_RECV, response);

        if (mRil.mLceInfoRegistrants != null) {
            mRil.mLceInfoRegistrants.notifyRegistrants(new AsyncResult(null, response, null));
        }
    }

    public void pcoData(int indicationType, PcoDataInfo pco) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        PcoData response = new PcoData(pco.cid, pco.bearerProto, pco.pcoId,
                RILUtils.arrayListToPrimitiveArray(pco.contents));

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_PCO_DATA, response);

        mRil.mPcoDataRegistrants.notifyRegistrants(new AsyncResult(null, response, null));
    }

    public void modemReset(int indicationType, String reason) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_MODEM_RESTART, reason);

        mRil.writeMetricsModemRestartEvent(reason);
        mRil.mModemResetRegistrants.notifyRegistrants(new AsyncResult(null, reason, null));
    }

    /**
     * Indicates when the carrier info to encrypt IMSI is being requested
     * @param indicationType RadioIndicationType
     */
    public void carrierInfoForImsiEncryption(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CARRIER_INFO_IMSI_ENCRYPTION, null);

        mRil.mCarrierInfoForImsiEncryptionRegistrants.notifyRegistrants(
                new AsyncResult(null, null, null));
    }

    /**
     * Indicates a change in the status of an ongoing Keepalive session
     * @param indicationType RadioIndicationType
     * @param halStatus Status of the ongoing Keepalive session
     */
    public void keepaliveStatus(
            int indicationType, android.hardware.radio.V1_1.KeepaliveStatus halStatus) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_KEEPALIVE_STATUS,
                    "handle=" + halStatus.sessionHandle + " code=" +  halStatus.code);
        }

        KeepaliveStatus ks = new KeepaliveStatus(
                halStatus.sessionHandle, halStatus.code);
        mRil.mNattKeepaliveStatusRegistrants.notifyRegistrants(new AsyncResult(null, ks, null));
    }

    /**
     * Indicates when the phonebook is changed.
     *
     * @param indicationType RadioIndicationType
     */
    public void simPhonebookChanged(int indicationType) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) {
            mRil.unsljLog(RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_CHANGED);
        }

        mRil.mSimPhonebookChangedRegistrants.notifyRegistrants();
    }

    /**
     * Indicates the content of all the used records in the SIM phonebook.
     * @param indicationType RadioIndicationType
     * @param status Status of PbReceivedStatus
     * @param records Content of the SIM phonebook records
     */
    public void simPhonebookRecordsReceived(int indicationType, byte status,
            ArrayList<PhonebookRecordInfo> records) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        List<SimPhonebookRecord> simPhonebookRecords = new ArrayList<>();

        for (PhonebookRecordInfo record : records) {
            simPhonebookRecords.add(RILUtils.convertHalPhonebookRecordInfo(record));
        }

        if (RIL.RILJ_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_RECORDS_RECEIVED,
                    "status = " + status + " received " + records.size() + " records");
        }

        mRil.mSimPhonebookRecordsReceivedRegistrants.notifyRegistrants(
                new AsyncResult(null,
                new ReceivedPhonebookRecords(status, simPhonebookRecords), null));
    }

    /**
     * Indicate that a registration failure has occurred.
     *
     * @param cellIdentity a CellIdentity the CellIdentity of the Cell
     * @param chosenPlmn a 5 or 6 digit alphanumeric string indicating the PLMN on which
     *        registration failed
     * @param domain the domain of the failed procedure: CS, PS, or both
     * @param causeCode the primary failure cause code of the procedure
     * @param additionalCauseCode an additional cause code if applicable
     */
    public void registrationFailed(int indicationType,
            android.hardware.radio.V1_5.CellIdentity cellIdentity, String chosenPlmn,
            @NetworkRegistrationInfo.Domain int domain,
            int causeCode, int additionalCauseCode) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);
        CellIdentity ci = RILUtils.convertHalCellIdentity(cellIdentity);
        if (ci == null
                || TextUtils.isEmpty(chosenPlmn)
                || (domain & NetworkRegistrationInfo.DOMAIN_CS_PS) == 0
                || (domain & ~NetworkRegistrationInfo.DOMAIN_CS_PS) != 0
                || causeCode < 0 || additionalCauseCode < 0
                || (causeCode == Integer.MAX_VALUE && additionalCauseCode == Integer.MAX_VALUE)) {
            reportAnomaly(
                    UUID.fromString("f16e5703-6105-4341-9eb3-e68189156eb4"),
                            "Invalid registrationFailed indication");

            mRil.riljLoge("Invalid registrationFailed indication");
            return;
        }

        mRil.mRegistrationFailedRegistrant.notifyRegistrant(
                new AsyncResult(null, new RegistrationFailedEvent(ci, chosenPlmn, domain,
                        causeCode, additionalCauseCode), null));
    }

    /**
     * Indicate that BarringInfo has changed for the current cell and user.
     *
     * @param cellIdentity a CellIdentity the CellIdentity of the Cell
     * @param barringInfos the updated barring information from the current cell, filtered for the
     *        current PLMN and access class / access category.
     */
    public void barringInfoChanged(int indicationType,
            android.hardware.radio.V1_5.CellIdentity cellIdentity,
            ArrayList<android.hardware.radio.V1_5.BarringInfo> barringInfos) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (cellIdentity == null || barringInfos == null) {
            reportAnomaly(
                    UUID.fromString("645b16bb-c930-4c1c-9c5d-568696542e05"),
                            "Invalid barringInfoChanged indication");

            mRil.riljLoge("Invalid barringInfoChanged indication");
            return;
        }

        BarringInfo cbi = new BarringInfo(RILUtils.convertHalCellIdentity(cellIdentity),
                RILUtils.convertHalBarringInfoList(barringInfos));

        mRil.mBarringInfoChangedRegistrants.notifyRegistrants(
                new AsyncResult(null, cbi, null));
    }

    /**
     * Set the frequency range or channel number from the physical channel config. Only one of them
     * is valid, we should set the other to the unknown value.
     * @param builder the builder of {@link PhysicalChannelConfig}.
     * @param config physical channel config from ril.
     */
    private void setFrequencyRangeOrChannelNumber(PhysicalChannelConfig.Builder builder,
            android.hardware.radio.V1_4.PhysicalChannelConfig config) {

        switch (config.rfInfo.getDiscriminator()) {
            case android.hardware.radio.V1_4.RadioFrequencyInfo.hidl_discriminator.range:
                builder.setFrequencyRange(config.rfInfo.range());
                break;
            case android.hardware.radio.V1_4.RadioFrequencyInfo.hidl_discriminator.channelNumber:
                builder.setDownlinkChannelNumber(config.rfInfo.channelNumber());
                break;
            default:
                mRil.riljLoge("Unsupported frequency type " + config.rfInfo.getDiscriminator());
        }
    }

    private void physicalChannelConfigsIndication(List<? extends Object> configs) {
        List<PhysicalChannelConfig> response = new ArrayList<>(configs.size());
        try {
            for (Object obj : configs) {
                if (obj instanceof android.hardware.radio.V1_2.PhysicalChannelConfig) {
                    android.hardware.radio.V1_2.PhysicalChannelConfig config =
                            (android.hardware.radio.V1_2.PhysicalChannelConfig) obj;

                    response.add(new PhysicalChannelConfig.Builder()
                            .setCellConnectionStatus(RILUtils.convertHalCellConnectionStatus(
                                    config.status))
                            .setCellBandwidthDownlinkKhz(config.cellBandwidthDownlink)
                            .build());
                } else if (obj instanceof android.hardware.radio.V1_4.PhysicalChannelConfig) {
                    android.hardware.radio.V1_4.PhysicalChannelConfig config =
                            (android.hardware.radio.V1_4.PhysicalChannelConfig) obj;
                    PhysicalChannelConfig.Builder builder = new PhysicalChannelConfig.Builder();
                    setFrequencyRangeOrChannelNumber(builder, config);
                    response.add(builder.setCellConnectionStatus(
                            RILUtils.convertHalCellConnectionStatus(config.base.status))
                            .setCellBandwidthDownlinkKhz(config.base.cellBandwidthDownlink)
                            .setNetworkType(
                                    ServiceState.rilRadioTechnologyToNetworkType(config.rat))
                            .setPhysicalCellId(config.physicalCellId)
                            .setContextIds(config.contextIds.stream().mapToInt(x -> x).toArray())
                            .build());
                } else if (obj instanceof android.hardware.radio.V1_6.PhysicalChannelConfig) {
                    android.hardware.radio.V1_6.PhysicalChannelConfig config =
                            (android.hardware.radio.V1_6.PhysicalChannelConfig) obj;
                    PhysicalChannelConfig.Builder builder = new PhysicalChannelConfig.Builder();
                    switch (config.band.getDiscriminator()) {
                        case Band.hidl_discriminator.geranBand:
                            builder.setBand(config.band.geranBand());
                            break;
                        case Band.hidl_discriminator.utranBand:
                            builder.setBand(config.band.utranBand());
                            break;
                        case Band.hidl_discriminator.eutranBand:
                            builder.setBand(config.band.eutranBand());
                            break;
                        case Band.hidl_discriminator.ngranBand:
                            builder.setBand(config.band.ngranBand());
                            break;
                        default:
                            mRil.riljLoge("Unsupported band " + config.band.getDiscriminator());
                    }
                    response.add(builder.setCellConnectionStatus(
                            RILUtils.convertHalCellConnectionStatus(config.status))
                            .setDownlinkChannelNumber(config.downlinkChannelNumber)
                            .setUplinkChannelNumber(config.uplinkChannelNumber)
                            .setCellBandwidthDownlinkKhz(config.cellBandwidthDownlinkKhz)
                            .setCellBandwidthUplinkKhz(config.cellBandwidthUplinkKhz)
                            .setNetworkType(
                                    ServiceState.rilRadioTechnologyToNetworkType(config.rat))
                            .setPhysicalCellId(config.physicalCellId)
                            .setContextIds(config.contextIds.stream().mapToInt(x -> x).toArray())
                            .build());
                } else {
                    mRil.riljLoge("Unsupported PhysicalChannelConfig " + obj);
                }
            }
        } catch (IllegalArgumentException iae) {
            reportAnomaly(UUID.fromString("918f0970-9aa9-4bcd-a28e-e49a83fe77d5"),
                    "RIL reported invalid PCC (HIDL)");
            mRil.riljLoge("Invalid PhysicalChannelConfig " + iae);
            return;
        }

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_PHYSICAL_CHANNEL_CONFIG, response);

        mRil.mPhysicalChannelConfigurationRegistrants.notifyRegistrants(
                new AsyncResult(null, response, null));
    }

    private void responseNetworkScan(int indicationType,
            android.hardware.radio.V1_1.NetworkScanResult result) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        ArrayList<CellInfo> cellInfos =
                RILUtils.convertHalCellInfoList(new ArrayList<>(result.networkInfos));
        NetworkScanResult nsr = new NetworkScanResult(result.status, result.error, cellInfos);
        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_NETWORK_SCAN_RESULT, nsr);
        mRil.mRilNetworkScanResultRegistrants.notifyRegistrants(new AsyncResult(null, nsr, null));
    }

    private void responseNetworkScan_1_2(int indicationType,
            android.hardware.radio.V1_2.NetworkScanResult result) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        ArrayList<CellInfo> cellInfos =
                RILUtils.convertHalCellInfoList(new ArrayList<>(result.networkInfos));
        NetworkScanResult nsr = new NetworkScanResult(result.status, result.error, cellInfos);
        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_NETWORK_SCAN_RESULT, nsr);
        mRil.mRilNetworkScanResultRegistrants.notifyRegistrants(new AsyncResult(null, nsr, null));
    }

    private void responseNetworkScan_1_4(int indicationType,
            android.hardware.radio.V1_4.NetworkScanResult result) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        ArrayList<CellInfo> cellInfos =
                RILUtils.convertHalCellInfoList(new ArrayList<>(result.networkInfos));
        NetworkScanResult nsr = new NetworkScanResult(result.status, result.error, cellInfos);
        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_NETWORK_SCAN_RESULT, nsr);
        mRil.mRilNetworkScanResultRegistrants.notifyRegistrants(new AsyncResult(null, nsr, null));
    }

    private void responseNetworkScan_1_5(int indicationType,
            android.hardware.radio.V1_5.NetworkScanResult result) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        ArrayList<CellInfo> cellInfos =
                RILUtils.convertHalCellInfoList(new ArrayList<>(result.networkInfos));
        NetworkScanResult nsr = new NetworkScanResult(result.status, result.error, cellInfos);
        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_NETWORK_SCAN_RESULT, nsr);
        mRil.mRilNetworkScanResultRegistrants.notifyRegistrants(new AsyncResult(null, nsr, null));
    }

    private void responseNetworkScan_1_6(int indicationType,
            android.hardware.radio.V1_6.NetworkScanResult result) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        ArrayList<CellInfo> cellInfos =
                RILUtils.convertHalCellInfoList(new ArrayList<>(result.networkInfos));
        NetworkScanResult nsr = new NetworkScanResult(result.status, result.error, cellInfos);
        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_NETWORK_SCAN_RESULT, nsr);
        mRil.mRilNetworkScanResultRegistrants.notifyRegistrants(new AsyncResult(null, nsr, null));
    }

    private void responseDataCallListChanged(int indicationType, List<?> dcList) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_DATA_CALL_LIST_CHANGED, dcList);

        ArrayList<DataCallResponse> response = RILUtils.convertHalDataCallResultList(dcList);
        mRil.mDataCallListChangedRegistrants.notifyRegistrants(
                new AsyncResult(null, response, null));
    }

    private void responseApnUnthrottled(int indicationType, String apn) {
        mRil.processIndication(RIL.RADIO_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_UNTHROTTLE_APN, apn);

        mRil.mApnUnthrottledRegistrants.notifyRegistrants(
                new AsyncResult(null, apn, null));
    }

    private void reportAnomaly(UUID uuid, String msg) {
        Phone phone = mRil.mPhoneId == null ? null : PhoneFactory.getPhone(mRil.mPhoneId);
        int carrierId = phone == null ? UNKNOWN_CARRIER_ID : phone.getCarrierId();
        AnomalyReporter.reportAnomaly(uuid, msg, carrierId);
    }
}

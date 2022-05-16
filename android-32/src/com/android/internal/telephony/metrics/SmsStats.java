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

package com.android.internal.telephony.metrics;

import static com.android.internal.telephony.InboundSmsHandler.SOURCE_INJECTED_FROM_IMS;
import static com.android.internal.telephony.InboundSmsHandler.SOURCE_INJECTED_FROM_UNKNOWN;
import static com.android.internal.telephony.InboundSmsHandler.SOURCE_NOT_INJECTED;
import static com.android.internal.telephony.SmsResponse.NO_ERROR_CODE;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__ERROR__SMS_ERROR_GENERIC;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__ERROR__SMS_ERROR_NOT_SUPPORTED;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__ERROR__SMS_ERROR_NO_MEMORY;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__ERROR__SMS_SUCCESS;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__SMS_FORMAT__SMS_FORMAT_3GPP;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__SMS_FORMAT__SMS_FORMAT_3GPP2;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__SMS_TECH__SMS_TECH_CS_3GPP;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__SMS_TECH__SMS_TECH_CS_3GPP2;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__SMS_TECH__SMS_TECH_IMS;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__SMS_TECH__SMS_TECH_UNKNOWN;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__SMS_TYPE__SMS_TYPE_NORMAL;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__SMS_TYPE__SMS_TYPE_SMS_PP;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__SMS_TYPE__SMS_TYPE_VOICEMAIL_INDICATION;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__SMS_TYPE__SMS_TYPE_WAP_PUSH;
import static com.android.internal.telephony.TelephonyStatsLog.INCOMING_SMS__SMS_TYPE__SMS_TYPE_ZERO;
import static com.android.internal.telephony.TelephonyStatsLog.OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_ERROR;
import static com.android.internal.telephony.TelephonyStatsLog.OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_ERROR_FALLBACK;
import static com.android.internal.telephony.TelephonyStatsLog.OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_ERROR_RETRY;
import static com.android.internal.telephony.TelephonyStatsLog.OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_SUCCESS;
import static com.android.internal.telephony.TelephonyStatsLog.OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_UNKNOWN;

import android.annotation.Nullable;
import android.app.Activity;
import android.provider.Telephony.Sms.Intents;
import android.telephony.Annotation.NetworkType;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.telephony.ims.stub.ImsSmsImplBase.SendStatusResult;

import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.nano.PersistAtomsProto.IncomingSms;
import com.android.internal.telephony.nano.PersistAtomsProto.OutgoingSms;
import com.android.telephony.Rlog;

import java.util.Random;

/** Collects sms events per phone ID for the pulled atom. */
public class SmsStats {
    private static final String TAG = SmsStats.class.getSimpleName();

    /** 3GPP error for out of service: "no network service" in TS 27.005 cl 3.2.5 */
    private static final int NO_NETWORK_ERROR_3GPP = 331;

    /** 3GPP2 error for out of service: "Other radio interface problem" in N.S0005 Table 171 */
    private static final int NO_NETWORK_ERROR_3GPP2 = 66;

    private final Phone mPhone;

    private final PersistAtomsStorage mAtomsStorage =
            PhoneFactory.getMetricsCollector().getAtomsStorage();

    private static final Random RANDOM = new Random();

    public SmsStats(Phone phone) {
        mPhone = phone;
    }

    /** Create a new atom when multi-part incoming SMS is dropped due to missing parts. */
    public void onDroppedIncomingMultipartSms(boolean is3gpp2, int receivedCount, int totalCount) {
        IncomingSms proto = getIncomingDefaultProto(is3gpp2, SOURCE_NOT_INJECTED);
        // Keep SMS tech as unknown because it's possible that it changed overtime and is not
        // necessarily the current one. Similarly mark the RAT as unknown.
        proto.smsTech = INCOMING_SMS__SMS_TECH__SMS_TECH_UNKNOWN;
        proto.rat = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        proto.error = INCOMING_SMS__ERROR__SMS_ERROR_GENERIC;
        proto.totalParts = totalCount;
        proto.receivedParts = receivedCount;
        mAtomsStorage.addIncomingSms(proto);
    }

    /** Create a new atom when an SMS for the voicemail indicator is received. */
    public void onIncomingSmsVoicemail(boolean is3gpp2,
            @InboundSmsHandler.SmsSource int smsSource) {
        IncomingSms proto = getIncomingDefaultProto(is3gpp2, smsSource);
        proto.smsType = INCOMING_SMS__SMS_TYPE__SMS_TYPE_VOICEMAIL_INDICATION;
        mAtomsStorage.addIncomingSms(proto);
    }

    /** Create a new atom when an SMS of type zero is received. */
    public void onIncomingSmsTypeZero(@InboundSmsHandler.SmsSource int smsSource) {
        IncomingSms proto = getIncomingDefaultProto(false /* is3gpp2 */, smsSource);
        proto.smsType = INCOMING_SMS__SMS_TYPE__SMS_TYPE_ZERO;
        mAtomsStorage.addIncomingSms(proto);
    }

    /** Create a new atom when an SMS-PP for the SIM card is received. */
    public void onIncomingSmsPP(@InboundSmsHandler.SmsSource int smsSource, boolean success) {
        IncomingSms proto = getIncomingDefaultProto(false /* is3gpp2 */, smsSource);
        proto.smsType = INCOMING_SMS__SMS_TYPE__SMS_TYPE_SMS_PP;
        proto.error = getIncomingSmsError(success);
        mAtomsStorage.addIncomingSms(proto);
    }

    /** Create a new atom when an SMS is received successfully. */
    public void onIncomingSmsSuccess(boolean is3gpp2,
            @InboundSmsHandler.SmsSource int smsSource, int messageCount,
            boolean blocked, long messageId) {
        IncomingSms proto = getIncomingDefaultProto(is3gpp2, smsSource);
        proto.totalParts = messageCount;
        proto.receivedParts = messageCount;
        proto.blocked = blocked;
        proto.messageId = messageId;
        mAtomsStorage.addIncomingSms(proto);
    }

    /** Create a new atom when an incoming SMS has an error. */
    public void onIncomingSmsError(boolean is3gpp2,
            @InboundSmsHandler.SmsSource int smsSource, int result) {
        IncomingSms proto = getIncomingDefaultProto(is3gpp2, smsSource);
        proto.error = getIncomingSmsError(result);
        mAtomsStorage.addIncomingSms(proto);
    }

    /** Create a new atom when an incoming WAP_PUSH SMS is received. */
    public void onIncomingSmsWapPush(@InboundSmsHandler.SmsSource int smsSource,
            int messageCount, int result, long messageId) {
        IncomingSms proto = getIncomingDefaultProto(false, smsSource);
        proto.smsType = INCOMING_SMS__SMS_TYPE__SMS_TYPE_WAP_PUSH;
        proto.totalParts = messageCount;
        proto.receivedParts = messageCount;
        proto.error = getIncomingSmsError(result);
        proto.messageId = messageId;
        mAtomsStorage.addIncomingSms(proto);
    }

    /** Create a new atom when an outgoing SMS is sent. */
    public void onOutgoingSms(boolean isOverIms, boolean is3gpp2, boolean fallbackToCs,
            @SmsManager.Result int errorCode, long messageId, boolean isFromDefaultApp) {
        onOutgoingSms(isOverIms, is3gpp2, fallbackToCs, errorCode, NO_ERROR_CODE,
                messageId, isFromDefaultApp);
    }

    /** Create a new atom when an outgoing SMS is sent. */
    public void onOutgoingSms(boolean isOverIms, boolean is3gpp2, boolean fallbackToCs,
            @SmsManager.Result int errorCode, int radioSpecificErrorCode, long messageId,
            boolean isFromDefaultApp) {
        OutgoingSms proto =
                getOutgoingDefaultProto(is3gpp2, isOverIms, messageId, isFromDefaultApp);

        if (isOverIms) {
            // Populate error code and result for IMS case
            proto.errorCode = errorCode;
            if (fallbackToCs) {
                proto.sendResult = OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_ERROR_FALLBACK;
            } else if (errorCode == SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY) {
                proto.sendResult = OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_ERROR_RETRY;
            } else if (errorCode != SmsManager.RESULT_ERROR_NONE) {
                proto.sendResult = OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_ERROR;
            }
        } else {
            // Populate error code and result for CS case
            if (errorCode == SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY) {
                proto.sendResult = OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_ERROR_RETRY;
            } else if (errorCode != SmsManager.RESULT_ERROR_NONE) {
                proto.sendResult = OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_ERROR;
            }
            proto.errorCode = radioSpecificErrorCode;
            if (errorCode == SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE
                    && radioSpecificErrorCode == NO_ERROR_CODE) {
                proto.errorCode = is3gpp2 ? NO_NETWORK_ERROR_3GPP2 : NO_NETWORK_ERROR_3GPP;
            }
        }
        mAtomsStorage.addOutgoingSms(proto);
    }

    /** Creates a proto for a normal single-part {@code IncomingSms} with default values. */
    private IncomingSms getIncomingDefaultProto(boolean is3gpp2,
            @InboundSmsHandler.SmsSource int smsSource) {
        IncomingSms proto = new IncomingSms();
        proto.smsFormat = getSmsFormat(is3gpp2);
        proto.smsTech = getSmsTech(smsSource, is3gpp2);
        proto.rat = getRat(smsSource);
        proto.smsType = INCOMING_SMS__SMS_TYPE__SMS_TYPE_NORMAL;
        proto.totalParts = 1;
        proto.receivedParts = 1;
        proto.blocked = false;
        proto.error = INCOMING_SMS__ERROR__SMS_SUCCESS;
        proto.isRoaming = getIsRoaming();
        proto.simSlotIndex = getPhoneId();
        proto.isMultiSim = SimSlotState.isMultiSim();
        proto.isEsim = SimSlotState.isEsim(getPhoneId());
        proto.carrierId = getCarrierId();
        // Message ID is initialized with random number, as it is not available for all incoming
        // SMS messages (e.g. those handled by OS or error cases).
        proto.messageId = RANDOM.nextLong();
        return proto;
    }

    /** Create a proto for a normal {@code OutgoingSms} with default values. */
    private OutgoingSms getOutgoingDefaultProto(boolean is3gpp2, boolean isOverIms,
            long messageId, boolean isFromDefaultApp) {
        OutgoingSms proto = new OutgoingSms();
        proto.smsFormat = getSmsFormat(is3gpp2);
        proto.smsTech = getSmsTech(isOverIms, is3gpp2);
        proto.rat = getRat(isOverIms);
        proto.sendResult = OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_SUCCESS;
        proto.errorCode = isOverIms ? SmsManager.RESULT_ERROR_NONE : NO_ERROR_CODE;
        proto.isRoaming = getIsRoaming();
        proto.isFromDefaultApp = isFromDefaultApp;
        proto.simSlotIndex = getPhoneId();
        proto.isMultiSim = SimSlotState.isMultiSim();
        proto.isEsim = SimSlotState.isEsim(getPhoneId());
        proto.carrierId = getCarrierId();
        // If the message ID is invalid, generate a random value
        proto.messageId = messageId != 0L ? messageId : RANDOM.nextLong();
        // Setting the retry ID to zero. If needed, it will be incremented when the atom is added
        // in the persistent storage.
        proto.retryId = 0;
        return proto;
    }

    private static int getSmsFormat(boolean is3gpp2) {
        if (is3gpp2) {
            return INCOMING_SMS__SMS_FORMAT__SMS_FORMAT_3GPP2;
        } else {
            return INCOMING_SMS__SMS_FORMAT__SMS_FORMAT_3GPP;
        }
    }

    private int getSmsTech(@InboundSmsHandler.SmsSource int smsSource, boolean is3gpp2) {
        if (smsSource == SOURCE_INJECTED_FROM_UNKNOWN) {
            return INCOMING_SMS__SMS_TECH__SMS_TECH_UNKNOWN;
        }
        return getSmsTech(smsSource == SOURCE_INJECTED_FROM_IMS, is3gpp2);
    }

    private int getSmsTech(boolean isOverIms, boolean is3gpp2) {
        if (isOverIms) {
            return INCOMING_SMS__SMS_TECH__SMS_TECH_IMS;
        } else if (is3gpp2) {
            return INCOMING_SMS__SMS_TECH__SMS_TECH_CS_3GPP2;
        } else {
            return INCOMING_SMS__SMS_TECH__SMS_TECH_CS_3GPP;
        }
    }

    private static int getIncomingSmsError(int result) {
        switch (result) {
            case Activity.RESULT_OK:
            case Intents.RESULT_SMS_HANDLED:
                return INCOMING_SMS__ERROR__SMS_SUCCESS;
            case Intents.RESULT_SMS_OUT_OF_MEMORY:
                return INCOMING_SMS__ERROR__SMS_ERROR_NO_MEMORY;
            case Intents.RESULT_SMS_UNSUPPORTED:
                return INCOMING_SMS__ERROR__SMS_ERROR_NOT_SUPPORTED;
            case Intents.RESULT_SMS_GENERIC_ERROR:
            default:
                return INCOMING_SMS__ERROR__SMS_ERROR_GENERIC;
        }
    }

    private static int getIncomingSmsError(boolean success) {
        if (success) {
            return INCOMING_SMS__ERROR__SMS_SUCCESS;
        } else {
            return INCOMING_SMS__ERROR__SMS_ERROR_GENERIC;
        }
    }

    private static int getOutgoingSmsError(@SendStatusResult int imsSendResult) {
        switch (imsSendResult) {
            case ImsSmsImplBase.SEND_STATUS_OK:
                return OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_SUCCESS;
            case ImsSmsImplBase.SEND_STATUS_ERROR:
                return OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_ERROR;
            case ImsSmsImplBase.SEND_STATUS_ERROR_RETRY:
                return OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_ERROR_RETRY;
            case ImsSmsImplBase.SEND_STATUS_ERROR_FALLBACK:
                return OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_ERROR_FALLBACK;
            default:
                return OUTGOING_SMS__SEND_RESULT__SMS_SEND_RESULT_UNKNOWN;
        }
    }

    private int getPhoneId() {
        Phone phone = mPhone;
        if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_IMS) {
            phone = mPhone.getDefaultPhone();
        }
        return phone.getPhoneId();
    }

    @Nullable
    private ServiceState getServiceState() {
        Phone phone = mPhone;
        if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_IMS) {
            phone = mPhone.getDefaultPhone();
        }
        ServiceStateTracker serviceStateTracker = phone.getServiceStateTracker();
        return serviceStateTracker != null ? serviceStateTracker.getServiceState() : null;
    }

    private @NetworkType int getRat(@InboundSmsHandler.SmsSource int smsSource) {
        if (smsSource == SOURCE_INJECTED_FROM_UNKNOWN) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
        return getRat(smsSource == SOURCE_INJECTED_FROM_IMS);
    }

    private @NetworkType int getRat(boolean isOverIms) {
        if (isOverIms) {
            if (mPhone.getImsRegistrationTech()
                    == ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN) {
                return TelephonyManager.NETWORK_TYPE_IWLAN;
            }
        }
        // TODO(b/168837897): Returns the RAT at the time the SMS was received..
        ServiceState serviceState = getServiceState();
        return serviceState != null
                ? serviceState.getVoiceNetworkType() : TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }

    private boolean getIsRoaming() {
        ServiceState serviceState = getServiceState();
        return serviceState != null ? serviceState.getRoaming() : false;
    }

    private int getCarrierId() {
        Phone phone = mPhone;
        if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_IMS) {
            phone = mPhone.getDefaultPhone();
        }
        return phone.getCarrierId();
    }

    private void loge(String format, Object... args) {
        Rlog.e(TAG, "[" + mPhone.getPhoneId() + "]" + String.format(format, args));
    }
}

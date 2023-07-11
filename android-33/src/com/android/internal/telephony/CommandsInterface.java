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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.KeepalivePacketData;
import android.net.LinkProperties;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.WorkSource;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.CarrierRestrictionRules;
import android.telephony.ClientRequestStats;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessSpecifier;
import android.telephony.SignalThresholdInfo;
import android.telephony.TelephonyManager;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.telephony.data.NetworkSliceInfo;
import android.telephony.data.TrafficDescriptor;
import android.telephony.emergency.EmergencyNumber;

import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.SimPhonebookRecord;

import java.util.List;

/**
 * {@hide}
 */
public interface CommandsInterface {

    //***** Constants

    // Used as parameter to dial() and setCLIR() below
    static final int CLIR_DEFAULT = 0;      // "use subscription default value"
    static final int CLIR_INVOCATION = 1;   // (restrict CLI presentation)
    static final int CLIR_SUPPRESSION = 2;  // (allow CLI presentation)

    // Used as return value for CDMA SS query
    static final int SS_STATUS_UNKNOWN          = 0xff;

    // Used as parameters for call forward methods below
    static final int CF_ACTION_DISABLE          = 0;
    static final int CF_ACTION_ENABLE           = 1;
//  static final int CF_ACTION_UNUSED           = 2;
    static final int CF_ACTION_REGISTRATION     = 3;
    static final int CF_ACTION_ERASURE          = 4;

    static final int CF_REASON_UNCONDITIONAL    = 0;
    static final int CF_REASON_BUSY             = 1;
    static final int CF_REASON_NO_REPLY         = 2;
    static final int CF_REASON_NOT_REACHABLE    = 3;
    static final int CF_REASON_ALL              = 4;
    static final int CF_REASON_ALL_CONDITIONAL  = 5;

    // Used for call barring methods below
    static final String CB_FACILITY_BAOC         = "AO";
    static final String CB_FACILITY_BAOIC        = "OI";
    static final String CB_FACILITY_BAOICxH      = "OX";
    static final String CB_FACILITY_BAIC         = "AI";
    static final String CB_FACILITY_BAICr        = "IR";
    static final String CB_FACILITY_BA_ALL       = "AB";
    static final String CB_FACILITY_BA_MO        = "AG";
    static final String CB_FACILITY_BA_MT        = "AC";
    static final String CB_FACILITY_BA_SIM       = "SC";
    static final String CB_FACILITY_BA_FD        = "FD";
    static final String CB_FACILITY_BIC_ACR      = "AR";


    // Used for various supp services apis
    // See 27.007 +CCFC or +CLCK
    static final int SERVICE_CLASS_NONE     = 0; // no user input
    static final int SERVICE_CLASS_VOICE    = (1 << 0);
    static final int SERVICE_CLASS_DATA     = (1 << 1); //synonym for 16+32+64+128
    static final int SERVICE_CLASS_FAX      = (1 << 2);
    static final int SERVICE_CLASS_SMS      = (1 << 3);
    static final int SERVICE_CLASS_DATA_SYNC = (1 << 4);
    static final int SERVICE_CLASS_DATA_ASYNC = (1 << 5);
    static final int SERVICE_CLASS_PACKET   = (1 << 6);
    static final int SERVICE_CLASS_PAD      = (1 << 7);
    static final int SERVICE_CLASS_MAX      = (1 << 7); // Max SERVICE_CLASS value

    // Numeric representation of string values returned
    // by messages sent to setOnUSSD handler
    static final int USSD_MODE_NOTIFY        = 0;
    static final int USSD_MODE_REQUEST       = 1;
    static final int USSD_MODE_NW_RELEASE    = 2;
    static final int USSD_MODE_LOCAL_CLIENT  = 3;
    static final int USSD_MODE_NOT_SUPPORTED = 4;
    static final int USSD_MODE_NW_TIMEOUT    = 5;

    // GSM SMS fail cause for acknowledgeLastIncomingSMS. From TS 23.040, 9.2.3.22.
    static final int GSM_SMS_FAIL_CAUSE_MEMORY_CAPACITY_EXCEEDED    = 0xD3;
    static final int GSM_SMS_FAIL_CAUSE_USIM_APP_TOOLKIT_BUSY       = 0xD4;
    static final int GSM_SMS_FAIL_CAUSE_USIM_DATA_DOWNLOAD_ERROR    = 0xD5;
    static final int GSM_SMS_FAIL_CAUSE_UNSPECIFIED_ERROR           = 0xFF;

    // CDMA SMS fail cause for acknowledgeLastIncomingCdmaSms.  From TS N.S0005, 6.5.2.125.
    static final int CDMA_SMS_FAIL_CAUSE_INVALID_TELESERVICE_ID     = 4;
    static final int CDMA_SMS_FAIL_CAUSE_RESOURCE_SHORTAGE          = 35;
    static final int CDMA_SMS_FAIL_CAUSE_OTHER_TERMINAL_PROBLEM     = 39;
    static final int CDMA_SMS_FAIL_CAUSE_ENCODING_PROBLEM           = 96;

    //***** Methods

    /**
     * get latest radio power state from modem
     * @return
     */
    int getRadioState();

    /**
     * response.obj.result is an int[2]
     *
     * response.obj.result[0] is IMS registration state
     *                        0 - Not registered
     *                        1 - Registered
     * response.obj.result[1] is of type RILConstants.GSM_PHONE or
     *                                    RILConstants.CDMA_PHONE
     */
    void getImsRegistrationState(Message result);

    /**
     * Fires on any RadioState transition
     * Always fires immediately as well
     *
     * do not attempt to calculate transitions by storing getRadioState() values
     * on previous invocations of this notification. Instead, use the other
     * registration methods
     */
    @UnsupportedAppUsage
    void registerForRadioStateChanged(Handler h, int what, Object obj);
    void unregisterForRadioStateChanged(Handler h);

    void registerForVoiceRadioTechChanged(Handler h, int what, Object obj);
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    void unregisterForVoiceRadioTechChanged(Handler h);
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    void registerForImsNetworkStateChanged(Handler h, int what, Object obj);
    void unregisterForImsNetworkStateChanged(Handler h);

    /**
     * Fires on any transition into RadioState.isOn()
     * Fires immediately if currently in that state
     * In general, actions should be idempotent. State may change
     * before event is received.
     */
    @UnsupportedAppUsage
    void registerForOn(Handler h, int what, Object obj);
    @UnsupportedAppUsage
    void unregisterForOn(Handler h);

    /**
     * Fires on any transition out of RadioState.isAvailable()
     * Fires immediately if currently in that state
     * In general, actions should be idempotent. State may change
     * before event is received.
     */
    @UnsupportedAppUsage
    void registerForAvailable(Handler h, int what, Object obj);
    @UnsupportedAppUsage
    void unregisterForAvailable(Handler h);

    /**
     * Fires on any transition into !RadioState.isAvailable()
     * Fires immediately if currently in that state
     * In general, actions should be idempotent. State may change
     * before event is received.
     */
    @UnsupportedAppUsage
    void registerForNotAvailable(Handler h, int what, Object obj);
    void unregisterForNotAvailable(Handler h);

    /**
     * Fires on any transition into RADIO_OFF or !RadioState.isAvailable()
     * Fires immediately if currently in that state
     * In general, actions should be idempotent. State may change
     * before event is received.
     */
    @UnsupportedAppUsage
    void registerForOffOrNotAvailable(Handler h, int what, Object obj);
    @UnsupportedAppUsage
    void unregisterForOffOrNotAvailable(Handler h);

    /**
     * Fires on any change in ICC status
     */
    void registerForIccStatusChanged(Handler h, int what, Object obj);
    void unregisterForIccStatusChanged(Handler h);
    /** Register for ICC slot status changed event */
    void registerForIccSlotStatusChanged(Handler h, int what, Object obj);
    /** Unregister for ICC slot status changed event */
    void unregisterForIccSlotStatusChanged(Handler h);

    void registerForCallStateChanged(Handler h, int what, Object obj);
    void unregisterForCallStateChanged(Handler h);
    /** Register for network state changed event */
    void registerForNetworkStateChanged(Handler h, int what, Object obj);
    /** Unregister from network state changed event */
    void unregisterForNetworkStateChanged(Handler h);
    /** Register for data call list changed event */
    void registerForDataCallListChanged(Handler h, int what, Object obj);
    /** Unregister from data call list changed event */
    void unregisterForDataCallListChanged(Handler h);
    /** Register for the apn unthrottled event */
    void registerForApnUnthrottled(Handler h, int what, Object obj);
    /** Unregister for apn unthrottled event */
    void unregisterForApnUnthrottled(Handler h);
    /** Register for the slicing config changed event */
    void registerForSlicingConfigChanged(Handler h, int what, Object obj);
    /** Unregister for slicing config changed event */
    void unregisterForSlicingConfigChanged(Handler h);

    /** InCall voice privacy notifications */
    void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj);
    void unregisterForInCallVoicePrivacyOn(Handler h);
    void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj);
    void unregisterForInCallVoicePrivacyOff(Handler h);

    /** Single Radio Voice Call State progress notifications */
    void registerForSrvccStateChanged(Handler h, int what, Object obj);
    void unregisterForSrvccStateChanged(Handler h);

    /**
     * Handlers for subscription status change indications.
     *
     * @param h Handler for subscription status change messages.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForSubscriptionStatusChanged(Handler h, int what, Object obj);
    void unregisterForSubscriptionStatusChanged(Handler h);

    /**
     * fires on any change in hardware configuration.
     */
    void registerForHardwareConfigChanged(Handler h, int what, Object obj);
    void unregisterForHardwareConfigChanged(Handler h);

    /**
     * unlike the register* methods, there's only one new 3GPP format SMS handler.
     * if you need to unregister, you should also tell the radio to stop
     * sending SMS's to you (via AT+CNMI)
     *
     * AsyncResult.result is a String containing the SMS PDU
     */
    void setOnNewGsmSms(Handler h, int what, Object obj);
    void unSetOnNewGsmSms(Handler h);

    /**
     * unlike the register* methods, there's only one new 3GPP2 format SMS handler.
     * if you need to unregister, you should also tell the radio to stop
     * sending SMS's to you (via AT+CNMI)
     *
     * AsyncResult.result is a String containing the SMS PDU
     */
    void setOnNewCdmaSms(Handler h, int what, Object obj);
    void unSetOnNewCdmaSms(Handler h);

    /**
     * Set the handler for SMS Cell Broadcast messages.
     *
     * AsyncResult.result is a byte array containing the SMS-CB PDU
     */
    @UnsupportedAppUsage
    void setOnNewGsmBroadcastSms(Handler h, int what, Object obj);
    void unSetOnNewGsmBroadcastSms(Handler h);

    /**
     * Register for NEW_SMS_ON_SIM unsolicited message
     *
     * AsyncResult.result is an int array containing the index of new SMS
     */
    @UnsupportedAppUsage
    void setOnSmsOnSim(Handler h, int what, Object obj);
    void unSetOnSmsOnSim(Handler h);

    /**
     * Register for NEW_SMS_STATUS_REPORT unsolicited message
     *
     * AsyncResult.result is a String containing the status report PDU
     */
    @UnsupportedAppUsage
    void setOnSmsStatus(Handler h, int what, Object obj);
    void unSetOnSmsStatus(Handler h);

    /**
     * unlike the register* methods, there's only one NITZ time handler
     *
     * AsyncResult.result is an Object[]
     * ((Object[])AsyncResult.result)[0] is a String containing the NITZ time string
     * ((Object[])AsyncResult.result)[1] is a Long containing the milliseconds since boot as
     *                                   returned by elapsedRealtime() when this NITZ time
     *                                   was posted.
     *
     * Please note that the delivery of this message may be delayed several
     * seconds on system startup
     */
    @UnsupportedAppUsage
    void setOnNITZTime(Handler h, int what, Object obj);
    void unSetOnNITZTime(Handler h);

    /**
     * unlike the register* methods, there's only one USSD notify handler
     *
     * Represents the arrival of a USSD "notify" message, which may
     * or may not have been triggered by a previous USSD send
     *
     * AsyncResult.result is a String[]
     * ((String[])(AsyncResult.result))[0] contains status code
     *      "0"   USSD-Notify -- text in ((const char **)data)[1]
     *      "1"   USSD-Request -- text in ((const char **)data)[1]
     *      "2"   Session terminated by network
     *      "3"   other local client (eg, SIM Toolkit) has responded
     *      "4"   Operation not supported
     *      "5"   Network timeout
     *
     * ((String[])(AsyncResult.result))[1] contains the USSD message
     * The numeric representations of these are in USSD_MODE_*
     */

    void setOnUSSD(Handler h, int what, Object obj);
    void unSetOnUSSD(Handler h);

    /**
     * unlike the register* methods, there's only one signal strength handler
     * AsyncResult.result is an int[2]
     * response.obj.result[0] is received signal strength (0-31, 99)
     * response.obj.result[1] is  bit error rate (0-7, 99)
     * as defined in TS 27.007 8.5
     */

    @UnsupportedAppUsage
    void setOnSignalStrengthUpdate(Handler h, int what, Object obj);
    void unSetOnSignalStrengthUpdate(Handler h);

    /**
     * Sets the handler for SIM/RUIM SMS storage full unsolicited message.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    @UnsupportedAppUsage
    void setOnIccSmsFull(Handler h, int what, Object obj);
    void unSetOnIccSmsFull(Handler h);

    /**
     * Sets the handler for SIM Refresh notifications.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    @UnsupportedAppUsage
    void registerForIccRefresh(Handler h, int what, Object obj);
    void unregisterForIccRefresh(Handler h);

    @UnsupportedAppUsage
    void setOnIccRefresh(Handler h, int what, Object obj);
    void unsetOnIccRefresh(Handler h);

    /**
     * Sets the handler for RING notifications.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    @UnsupportedAppUsage
    void setOnCallRing(Handler h, int what, Object obj);
    void unSetOnCallRing(Handler h);

    /**
     * Sets the handler for RESTRICTED_STATE changed notification,
     * eg, for Domain Specific Access Control
     * unlike the register* methods, there's only one signal strength handler
     *
     * AsyncResult.result is an int[1]
     * response.obj.result[0] is a bitmask of RIL_RESTRICTED_STATE_* values
     */

    void setOnRestrictedStateChanged(Handler h, int what, Object obj);
    void unSetOnRestrictedStateChanged(Handler h);

    /**
     * Sets the handler for Supplementary Service Notifications.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    @UnsupportedAppUsage
    void setOnSuppServiceNotification(Handler h, int what, Object obj);
    void unSetOnSuppServiceNotification(Handler h);

    /**
     * Sets the handler for Session End Notifications for CAT.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    @UnsupportedAppUsage
    void setOnCatSessionEnd(Handler h, int what, Object obj);
    void unSetOnCatSessionEnd(Handler h);

    /**
     * Sets the handler for Proactive Commands for CAT.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    @UnsupportedAppUsage
    void setOnCatProactiveCmd(Handler h, int what, Object obj);
    void unSetOnCatProactiveCmd(Handler h);

    /**
     * Sets the handler for Event Notifications for CAT.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    @UnsupportedAppUsage
    void setOnCatEvent(Handler h, int what, Object obj);
    void unSetOnCatEvent(Handler h);

    /**
     * Sets the handler for Call Set Up Notifications for CAT.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    @UnsupportedAppUsage
    void setOnCatCallSetUp(Handler h, int what, Object obj);
    void unSetOnCatCallSetUp(Handler h);

    /**
     * Enables/disbables supplementary service related notifications from
     * the network.
     *
     * @param enable true to enable notifications, false to disable.
     * @param result Message to be posted when command completes.
     */
    void setSuppServiceNotifications(boolean enable, Message result);
    //void unSetSuppServiceNotifications(Handler h);

    /**
     * Sets the handler for Alpha Notification during STK Call Control.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    @UnsupportedAppUsage
    void setOnCatCcAlphaNotify(Handler h, int what, Object obj);
    void unSetOnCatCcAlphaNotify(Handler h);

    /**
     * Sets the handler for notifying Suplementary Services (SS)
     * Data during STK Call Control.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void setOnSs(Handler h, int what, Object obj);
    void unSetOnSs(Handler h);

    /**
     * Register for unsolicited NATT Keepalive Status Indications
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    default void setOnRegistrationFailed(Handler h, int what, Object obj) {}

    /**
     * @param h Handler for notification message.
     */
    default void unSetOnRegistrationFailed(Handler h) {}

    /**
     * Sets the handler for Event Notifications for CDMA Display Info.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForDisplayInfo(Handler h, int what, Object obj);
    void unregisterForDisplayInfo(Handler h);

    /**
     * Sets the handler for Event Notifications for CallWaiting Info.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForCallWaitingInfo(Handler h, int what, Object obj);
    void unregisterForCallWaitingInfo(Handler h);

    /**
     * Sets the handler for Event Notifications for Signal Info.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForSignalInfo(Handler h, int what, Object obj);
    void unregisterForSignalInfo(Handler h);

    /**
     * Registers the handler for CDMA number information record
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForNumberInfo(Handler h, int what, Object obj);
    void unregisterForNumberInfo(Handler h);

    /**
     * Registers the handler for CDMA redirected number Information record
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForRedirectedNumberInfo(Handler h, int what, Object obj);
    void unregisterForRedirectedNumberInfo(Handler h);

    /**
     * Registers the handler for CDMA line control information record
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForLineControlInfo(Handler h, int what, Object obj);
    void unregisterForLineControlInfo(Handler h);

    /**
     * Registers the handler for CDMA T53 CLIR information record
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerFoT53ClirlInfo(Handler h, int what, Object obj);
    void unregisterForT53ClirInfo(Handler h);

    /**
     * Registers the handler for CDMA T53 audio control information record
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForT53AudioControlInfo(Handler h, int what, Object obj);
    void unregisterForT53AudioControlInfo(Handler h);

    /**
     * Fires on if Modem enters Emergency Callback mode
     */
    @UnsupportedAppUsage
    void setEmergencyCallbackMode(Handler h, int what, Object obj);

     /**
      * Fires on any CDMA OTA provision status change
      */
     @UnsupportedAppUsage
     void registerForCdmaOtaProvision(Handler h,int what, Object obj);
     @UnsupportedAppUsage
     void unregisterForCdmaOtaProvision(Handler h);

     /**
      * Registers the handler when out-band ringback tone is needed.<p>
      *
      *  Messages received from this:
      *  Message.obj will be an AsyncResult
      *  AsyncResult.userObj = obj
      *  AsyncResult.result = boolean. <p>
      */
     void registerForRingbackTone(Handler h, int what, Object obj);
     void unregisterForRingbackTone(Handler h);

     /**
      * Registers the handler when mute/unmute need to be resent to get
      * uplink audio during a call.<p>
      *
      * @param h Handler for notification message.
      * @param what User-defined message code.
      * @param obj User object.
      *
      */
     void registerForResendIncallMute(Handler h, int what, Object obj);
     void unregisterForResendIncallMute(Handler h);

     /**
      * Registers the handler for when Cdma subscription changed events
      *
      * @param h Handler for notification message.
      * @param what User-defined message code.
      * @param obj User object.
      *
      */
     void registerForCdmaSubscriptionChanged(Handler h, int what, Object obj);
     void unregisterForCdmaSubscriptionChanged(Handler h);

     /**
      * Registers the handler for when Cdma prl changed events
      *
      * @param h Handler for notification message.
      * @param what User-defined message code.
      * @param obj User object.
      *
      */
     void registerForCdmaPrlChanged(Handler h, int what, Object obj);
     void unregisterForCdmaPrlChanged(Handler h);

     /**
      * Registers the handler for when Cdma prl changed events
      *
      * @param h Handler for notification message.
      * @param what User-defined message code.
      * @param obj User object.
      *
      */
     void registerForExitEmergencyCallbackMode(Handler h, int what, Object obj);
     void unregisterForExitEmergencyCallbackMode(Handler h);

     /**
      * Registers the handler for RIL_UNSOL_RIL_CONNECT events.
      *
      * When ril connects or disconnects a message is sent to the registrant
      * which contains an AsyncResult, ar, in msg.obj. The ar.result is an
      * Integer which is the version of the ril or -1 if the ril disconnected.
      *
      * @param h Handler for notification message.
      * @param what User-defined message code.
      * @param obj User object.
      */
     @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
     void registerForRilConnected(Handler h, int what, Object obj);
     @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
     void unregisterForRilConnected(Handler h);

    /**
     * Registers the handler for RIL_UNSOL_SIM_DETACH_FROM_NETWORK_CONFIG_CHANGED events.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    default void registerUiccApplicationEnablementChanged(Handler h, int what, Object obj) {};

    /**
     * Unregisters the handler for RIL_UNSOL_SIM_DETACH_FROM_NETWORK_CONFIG_CHANGED events.
     *
     * @param h Handler for notification message.
     */
    default void unregisterUiccApplicationEnablementChanged(Handler h) {};

    /**
     * Supply the ICC PIN to the ICC card
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     *  ar.result is an optional array of integers where the first entry
     *  is the number of attempts remaining before the ICC will be PUK locked.
     *
     * ar.exception and ar.result are null on success
     */

    @UnsupportedAppUsage
    void supplyIccPin(String pin, Message result);

    /**
     * Supply the PIN for the app with this AID on the ICC card
     *
     *  AID (Application ID), See ETSI 102.221 8.1 and 101.220 4
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     *  ar.result is an optional array of integers where the first entry
     *  is the number of attempts remaining before the ICC will be PUK locked.
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPinForApp(String pin, String aid, Message result);

    /**
     * Supply the ICC PUK and newPin to the ICC card
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     *  ar.result is an optional array of integers where the first entry
     *  is the number of attempts remaining before the ICC is permanently disabled.
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPuk(String puk, String newPin, Message result);

    /**
     * Supply the PUK, new pin for the app with this AID on the ICC card
     *
     *  AID (Application ID), See ETSI 102.221 8.1 and 101.220 4
     *
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     *  ar.result is an optional array of integers where the first entry
     *  is the number of attempts remaining before the ICC is permanently disabled.
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPukForApp(String puk, String newPin, String aid, Message result);

    /**
     * Supply the ICC PIN2 to the ICC card
     * Only called following operation where ICC_PIN2 was
     * returned as a a failure from a previous operation
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     *  ar.result is an optional array of integers where the first entry
     *  is the number of attempts remaining before the ICC will be PUK locked.
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPin2(String pin2, Message result);

    /**
     * Supply the PIN2 for the app with this AID on the ICC card
     * Only called following operation where ICC_PIN2 was
     * returned as a a failure from a previous operation
     *
     *  AID (Application ID), See ETSI 102.221 8.1 and 101.220 4
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     *  ar.result is an optional array of integers where the first entry
     *  is the number of attempts remaining before the ICC will be PUK locked.
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPin2ForApp(String pin2, String aid, Message result);

    /**
     * Supply the SIM PUK2 to the SIM card
     * Only called following operation where SIM_PUK2 was
     * returned as a a failure from a previous operation
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     *  ar.result is an optional array of integers where the first entry
     *  is the number of attempts remaining before the ICC is permanently disabled.
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPuk2(String puk2, String newPin2, Message result);

    /**
     * Supply the PUK2, newPin2 for the app with this AID on the ICC card
     * Only called following operation where SIM_PUK2 was
     * returned as a a failure from a previous operation
     *
     *  AID (Application ID), See ETSI 102.221 8.1 and 101.220 4
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     *  ar.result is an optional array of integers where the first entry
     *  is the number of attempts remaining before the ICC is permanently disabled.
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPuk2ForApp(String puk2, String newPin2, String aid, Message result);

    // TODO: Add java doc and indicate that msg.arg1 contains the number of attempts remaining.
    void changeIccPin(String oldPin, String newPin, Message result);
    void changeIccPinForApp(String oldPin, String newPin, String aidPtr, Message result);
    void changeIccPin2(String oldPin2, String newPin2, Message result);
    void changeIccPin2ForApp(String oldPin2, String newPin2, String aidPtr, Message result);

    @UnsupportedAppUsage
    void changeBarringPassword(String facility, String oldPwd, String newPwd, Message result);

    void supplyNetworkDepersonalization(String netpin, Message result);

    void supplySimDepersonalization(PersoSubState persoType, String controlKey, Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result contains a List of DriverCall
     *      The ar.result List is sorted by DriverCall.index
     */
    void getCurrentCalls (Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result contains a List of DataCallResponse
     *  @deprecated Do not use.
     */
    @UnsupportedAppUsage
    @Deprecated
    void getPDPContextList(Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result contains a List of DataCallResponse
     */
    @UnsupportedAppUsage
    void getDataCallList(Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     *
     * CLIR_DEFAULT     == on "use subscription default value"
     * CLIR_SUPPRESSION == on "CLIR suppression" (allow CLI presentation)
     * CLIR_INVOCATION  == on "CLIR invocation" (restrict CLI presentation)
     */
    void dial(String address, boolean isEmergencyCall, EmergencyNumber emergencyNumberInfo,
              boolean hasKnownUserIntentEmergency, int clirMode, Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     *
     * CLIR_DEFAULT     == on "use subscription default value"
     * CLIR_SUPPRESSION == on "CLIR suppression" (allow CLI presentation)
     * CLIR_INVOCATION  == on "CLIR invocation" (restrict CLI presentation)
     */
    void dial(String address, boolean isEmergencyCall, EmergencyNumber emergencyNumberInfo,
              boolean hasKnownUserIntentEmergency, int clirMode, UUSInfo uusInfo, Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is String containing IMSI on success
     */
    @UnsupportedAppUsage
    void getIMSI(Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is String containing IMSI on success
     */
    void getIMSIForApp(String aid, Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is String containing IMEI on success
     */
    void getIMEI(Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is String containing IMEISV on success
     */
    @UnsupportedAppUsage
    void getIMEISV(Message result);

    /**
     * Hang up one individual connection.
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     *
     *  3GPP 22.030 6.5.5
     *  "Releases a specific active call X"
     */
    void hangupConnection (int gsmIndex, Message result);

    /**
     * 3GPP 22.030 6.5.5
     *  "Releases all held calls or sets User Determined User Busy (UDUB)
     *   for a waiting call."
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void hangupWaitingOrBackground (Message result);

    /**
     * 3GPP 22.030 6.5.5
     * "Releases all active calls (if any exist) and accepts
     *  the other (held or waiting) call."
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void hangupForegroundResumeBackground (Message result);

    /**
     * 3GPP 22.030 6.5.5
     * "Places all active calls (if any exist) on hold and accepts
     *  the other (held or waiting) call."
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    @UnsupportedAppUsage
    void switchWaitingOrHoldingAndActive (Message result);

    /**
     * 3GPP 22.030 6.5.5
     * "Adds a held call to the conversation"
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void conference (Message result);

    /**
     * Set preferred Voice Privacy (VP).
     *
     * @param enable true is enhanced and false is normal VP
     * @param result is a callback message
     */
    void setPreferredVoicePrivacy(boolean enable, Message result);

    /**
     * Get currently set preferred Voice Privacy (VP) mode.
     *
     * @param result is a callback message
     */
    void getPreferredVoicePrivacy(Message result);

    /**
     * 3GPP 22.030 6.5.5
     * "Places all active calls on hold except call X with which
     *  communication shall be supported."
     */
    void separateConnection (int gsmIndex, Message result);

    /**
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    @UnsupportedAppUsage
    void acceptCall (Message result);

    /**
     *  also known as UDUB
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void rejectCall (Message result);

    /**
     * 3GPP 22.030 6.5.5
     * "Connects the two calls and disconnects the subscriber from both calls"
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void explicitCallTransfer (Message result);

    /**
     * cause code returned as int[0] in Message.obj.response
     * Returns integer cause code defined in TS 24.008
     * Annex H or closest approximation.
     * Most significant codes:
     * - Any defined in 22.001 F.4 (for generating busy/congestion)
     * - Cause 68: ACM >= ACMMax
     */
    void getLastCallFailCause (Message result);


    /**
     * Reason for last PDP context deactivate or failure to activate
     * cause code returned as int[0] in Message.obj.response
     * returns an integer cause code defined in TS 24.008
     * section 6.1.3.1.3 or close approximation
     * @deprecated Do not use.
     */
    @UnsupportedAppUsage
    @Deprecated
    void getLastPdpFailCause (Message result);

    /**
     * The preferred new alternative to getLastPdpFailCause
     * that is also CDMA-compatible.
     */
    @UnsupportedAppUsage
    void getLastDataCallFailCause (Message result);

    void setMute (boolean enableMute, Message response);

    void getMute (Message response);

    /**
     * response.obj is an AsyncResult
     * response.obj.result is an int[2]
     * response.obj.result[0] is received signal strength (0-31, 99)
     * response.obj.result[1] is  bit error rate (0-7, 99)
     * as defined in TS 27.007 8.5
     */
    @UnsupportedAppUsage
    void getSignalStrength (Message response);


    /**
     * response.obj.result is an int[3]
     * response.obj.result[0] is registration state 0-5 from TS 27.007 7.2
     * response.obj.result[1] is LAC if registered or -1 if not
     * response.obj.result[2] is CID if registered or -1 if not
     * valid LAC and CIDs are 0x0000 - 0xffff
     *
     * Please note that registration state 4 ("unknown") is treated
     * as "out of service" above
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    void getVoiceRegistrationState (Message response);

    /**
     * response.obj.result is an int[3]
     * response.obj.result[0] is registration state 0-5 from TS 27.007 7.2
     * response.obj.result[1] is LAC if registered or -1 if not
     * response.obj.result[2] is CID if registered or -1 if not
     * valid LAC and CIDs are 0x0000 - 0xffff
     *
     * Please note that registration state 4 ("unknown") is treated
     * as "out of service" above
     */
    void getDataRegistrationState (Message response);

    /**
     * response.obj.result is a String[3]
     * response.obj.result[0] is long alpha or null if unregistered
     * response.obj.result[1] is short alpha or null if unregistered
     * response.obj.result[2] is numeric or null if unregistered
     */
    @UnsupportedAppUsage
    void getOperator(Message response);

    /**
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    @UnsupportedAppUsage
    void sendDtmf(char c, Message result);


    /**
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void startDtmf(char c, Message result);

    /**
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void stopDtmf(Message result);

    /**
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void sendBurstDtmf(String dtmfString, int on, int off, Message result);

    /**
     * smscPDU is smsc address in PDU form GSM BCD format prefixed
     *      by a length byte (as expected by TS 27.005) or NULL for default SMSC
     * pdu is SMS in PDU format as an ASCII hex string
     *      less the SMSC address
     */
    void sendSMS (String smscPDU, String pdu, Message response);

    /**
     * Send an SMS message, Identical to sendSMS,
     * except that more messages are expected to be sent soon
     * smscPDU is smsc address in PDU form GSM BCD format prefixed
     *      by a length byte (as expected by TS 27.005) or NULL for default SMSC
     * pdu is SMS in PDU format as an ASCII hex string
     *      less the SMSC address
     */
    void sendSMSExpectMore (String smscPDU, String pdu, Message response);

    /**
     * @param pdu is CDMA-SMS in internal pseudo-PDU format
     * @param response sent when operation completes
     */
    void sendCdmaSms(byte[] pdu, Message response);

    /**
     * Identical to sendCdmaSms, except that more messages are expected to be sent soon
     * @param pdu is CDMA-SMS in internal pseudo-PDU format
     * @param response response sent when operation completed
     */
    void sendCdmaSMSExpectMore(byte[] pdu, Message response);

    /**
     * send SMS over IMS with 3GPP/GSM SMS format
     * @param smscPDU is smsc address in PDU form GSM BCD format prefixed
     *      by a length byte (as expected by TS 27.005) or NULL for default SMSC
     * @param pdu is SMS in PDU format as an ASCII hex string
     *      less the SMSC address
     * @param retry indicates if this is a retry; 0 == not retry, nonzero = retry
     * @param messageRef valid field if retry is set to nonzero.
     *        Contains messageRef from RIL_SMS_Response corresponding to failed MO SMS
     * @param response sent when operation completes
     */
    void sendImsGsmSms (String smscPDU, String pdu, int retry, int messageRef,
            Message response);

    /**
     * send SMS over IMS with 3GPP2/CDMA SMS format
     * @param pdu is CDMA-SMS in internal pseudo-PDU format
     * @param response sent when operation completes
     * @param retry indicates if this is a retry; 0 == not retry, nonzero = retry
     * @param messageRef valid field if retry is set to nonzero.
     *        Contains messageRef from RIL_SMS_Response corresponding to failed MO SMS
     * @param response sent when operation completes
     */
    void sendImsCdmaSms(byte[] pdu, int retry, int messageRef, Message response);

    /**
     * Deletes the specified SMS record from SIM memory (EF_SMS).
     *
     * @param index index of the SMS record to delete
     * @param response sent when operation completes
     */
    @UnsupportedAppUsage
    void deleteSmsOnSim(int index, Message response);

    /**
     * Deletes the specified SMS record from RUIM memory (EF_SMS in DF_CDMA).
     *
     * @param index index of the SMS record to delete
     * @param response sent when operation completes
     */
    @UnsupportedAppUsage
    void deleteSmsOnRuim(int index, Message response);

    /**
     * Writes an SMS message to SIM memory (EF_SMS).
     *
     * @param status status of message on SIM.  One of:
     *                  SmsManger.STATUS_ON_ICC_READ
     *                  SmsManger.STATUS_ON_ICC_UNREAD
     *                  SmsManger.STATUS_ON_ICC_SENT
     *                  SmsManger.STATUS_ON_ICC_UNSENT
     * @param pdu message PDU, as hex string
     * @param response sent when operation completes.
     *                  response.obj will be an AsyncResult, and will indicate
     *                  any error that may have occurred (eg, out of memory).
     */
    @UnsupportedAppUsage
    void writeSmsToSim(int status, String smsc, String pdu, Message response);

    /**
     * Writes an SMS message to RUIM memory (EF_SMS).
     *
     * @param status status of message on SIM. One of:
     *                  SmsManger.STATUS_ON_ICC_READ
     *                  SmsManger.STATUS_ON_ICC_UNREAD
     *                  SmsManger.STATUS_ON_ICC_SENT
     *                  SmsManger.STATUS_ON_ICC_UNSENT
     * @param pdu message PDU, as byte array
     * @param response sent when operation completes. response.obj will be an AsyncResult, and will
     *     indicate any error that may have occurred (eg, out of memory).
     */
    @UnsupportedAppUsage
    void writeSmsToRuim(int status, byte[] pdu, Message response);

    @UnsupportedAppUsage
    default void setRadioPower(boolean on, Message response) {
        setRadioPower(on, false, false, response);
    }

    /**
     * Sets the radio power on/off state (off is sometimes
     * called "airplane mode").
     *
     * @param on true means "on", false means "off".
     * @param forEmergencyCall true means the purpose of turning radio power on is for emergency
     *                         call. No effect if power is set false.
     * @param isSelectedPhoneForEmergencyCall true means this phone / modem is selected to place
     *                                  emergency call after turning power on. No effect if power
     *                                  or forEmergency is set false.
     * @param response sent when operation completes.
     */
    default void setRadioPower(boolean on, boolean forEmergencyCall,
            boolean isSelectedPhoneForEmergencyCall, Message response) {}

    @UnsupportedAppUsage
    void acknowledgeLastIncomingGsmSms(boolean success, int cause, Message response);

    @UnsupportedAppUsage
    void acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message response);

    /**
     * Acknowledge successful or failed receipt of last incoming SMS,
     * including acknowledgement TPDU to send as the RP-User-Data element
     * of the RP-ACK or RP-ERROR PDU.
     *
     * @param success true to send RP-ACK, false to send RP-ERROR
     * @param ackPdu the acknowledgement TPDU in hexadecimal format
     * @param response sent when operation completes.
     */
    void acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message response);

    /**
     * parameters equivalent to 27.007 AT+CRSM command
     * response.obj will be an AsyncResult
     * response.obj.result will be an IccIoResult on success
     */
    @UnsupportedAppUsage
    void iccIO (int command, int fileid, String path, int p1, int p2, int p3,
            String data, String pin2, Message response);

    /**
     * parameters equivalent to 27.007 AT+CRSM command
     * response.obj will be an AsyncResult
     * response.obj.userObj will be a IccIoResult on success
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    void iccIOForApp (int command, int fileid, String path, int p1, int p2, int p3,
            String data, String pin2, String aid, Message response);

    /**
     * (AsyncResult)response.obj).result is an int[] with element [0] set to
     * 1 for "CLIP is provisioned", and 0 for "CLIP is not provisioned".
     *
     * @param response is callback message
     */

    void queryCLIP(Message response);

    /**
     * response.obj will be a an int[2]
     *
     * response.obj[0] will be TS 27.007 +CLIR parameter 'n'
     *  0 presentation indicator is used according to the subscription of the CLIR service
     *  1 CLIR invocation
     *  2 CLIR suppression
     *
     * response.obj[1] will be TS 27.007 +CLIR parameter 'm'
     *  0 CLIR not provisioned
     *  1 CLIR provisioned in permanent mode
     *  2 unknown (e.g. no network, etc.)
     *  3 CLIR temporary mode presentation restricted
     *  4 CLIR temporary mode presentation allowed
     */

    void getCLIR(Message response);

    /**
     * clirMode is one of the CLIR_* constants above
     *
     * response.obj is null
     */

    void setCLIR(int clirMode, Message response);

    /**
     * (AsyncResult)response.obj).result is an int[] with element [0] set to
     * 0 for disabled, 1 for enabled.
     *
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param response is callback message
     */

    @UnsupportedAppUsage
    void queryCallWaiting(int serviceClass, Message response);

    /**
     * @param enable is true to enable, false to disable
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param response is callback message
     */

    @UnsupportedAppUsage
    void setCallWaiting(boolean enable, int serviceClass, Message response);

    /**
     * @param action is one of CF_ACTION_*
     * @param cfReason is one of CF_REASON_*
     * @param serviceClass is a sum of SERVICE_CLASSS_*
     */
    @UnsupportedAppUsage
    void setCallForward(int action, int cfReason, int serviceClass,
                String number, int timeSeconds, Message response);

    /**
     * cfReason is one of CF_REASON_*
     *
     * ((AsyncResult)response.obj).result will be an array of
     * CallForwardInfo's
     *
     * An array of length 0 means "disabled for all codes"
     */
    @UnsupportedAppUsage
    void queryCallForwardStatus(int cfReason, int serviceClass,
            String number, Message response);

    @UnsupportedAppUsage
    void setNetworkSelectionModeAutomatic(Message response);

    /**
     * Ask the radio to connect to the input network with specific RadioAccessNetwork
     * and change selection mode to manual.
     * @param operatorNumeric PLMN ID of the network to select.
     * @param ran radio access network type (see {@link AccessNetworkType}).
     * @param response callback message.
     */
    @UnsupportedAppUsage
    void setNetworkSelectionModeManual(String operatorNumeric, int ran, Message response);

    /**
     * Queries whether the current network selection mode is automatic
     * or manual
     *
     * ((AsyncResult)response.obj).result  is an int[] with element [0] being
     * a 0 for automatic selection and a 1 for manual selection
     */

    @UnsupportedAppUsage
    void getNetworkSelectionMode(Message response);

    /**
     * Queries the currently available networks
     *
     * ((AsyncResult)response.obj).result is a List of NetworkInfo objects
     */
    void getAvailableNetworks(Message response);

    /**
     * Starts a radio network scan
     *
     * ((AsyncResult)response.obj).result is a NetworkScanResult object
     */
    void startNetworkScan(NetworkScanRequest nsr, Message response);

    /**
     * Stops the ongoing network scan
     *
     * ((AsyncResult)response.obj).result is a NetworkScanResult object
     *
     */
    void stopNetworkScan(Message response);

    /**
     * Gets the baseband version
     */
    @UnsupportedAppUsage
    void getBasebandVersion(Message response);

    /**
     * (AsyncResult)response.obj).result will be an Integer representing
     * the sum of enabled service classes (sum of SERVICE_CLASS_*)
     *
     * @param facility one of CB_FACILTY_*
     * @param password password or "" if not required
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param response is callback message
     */

    @UnsupportedAppUsage
    void queryFacilityLock (String facility, String password, int serviceClass,
        Message response);

    /**
     * (AsyncResult)response.obj).result will be an Integer representing
     * the sum of enabled service classes (sum of SERVICE_CLASS_*) for the
     * application with appId.
     *
     * @param facility one of CB_FACILTY_*
     * @param password password or "" if not required
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param appId is application Id or null if none
     * @param response is callback message
     */

    void queryFacilityLockForApp(String facility, String password, int serviceClass, String appId,
        Message response);

    /**
     * @param facility one of CB_FACILTY_*
     * @param lockState true means lock, false means unlock
     * @param password password or "" if not required
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param response is callback message
     */
    @UnsupportedAppUsage
    void setFacilityLock (String facility, boolean lockState, String password,
        int serviceClass, Message response);

    /**
     * Set the facility lock for the app with this AID on the ICC card.
     *
     * @param facility one of CB_FACILTY_*
     * @param lockState true means lock, false means unlock
     * @param password password or "" if not required
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param appId is application Id or null if none
     * @param response is callback message
     */
    void setFacilityLockForApp(String facility, boolean lockState, String password,
        int serviceClass, String appId, Message response);

    void sendUSSD (String ussdString, Message response);

    /**
     * Cancels a pending USSD session if one exists.
     * @param response callback message
     */
    void cancelPendingUssd (Message response);

    void resetRadio(Message result);

    /**
     * Assign a specified band for RF configuration.
     *
     * @param bandMode one of BM_*_BAND
     * @param response is callback message
     */
    void setBandMode (int bandMode, Message response);

    /**
     * Query the list of band mode supported by RF.
     *
     * @param response is callback message
     *        ((AsyncResult)response.obj).result  is an int[] where int[0] is
     *        the size of the array and the rest of each element representing
     *        one available BM_*_BAND
     */
    void queryAvailableBandMode (Message response);

    /**
     *  Requests to set the preferred network type for searching and registering
     * (CS/PS domain, RAT, and operation mode)
     * @param networkType one of  NT_*_TYPE
     * @param response is callback message
     */
    @UnsupportedAppUsage
    void setPreferredNetworkType(int networkType , Message response);

     /**
     *  Query the preferred network type setting
     *
     * @param response is callback message to report one of  NT_*_TYPE
     */
    @UnsupportedAppUsage
    void getPreferredNetworkType(Message response);

    /**
     * Requests to set the allowed network types for searching and registering.
     *
     * @param networkTypeBitmask {@link TelephonyManager.NetworkTypeBitMask}
     * @param response is callback message
     */
    void setAllowedNetworkTypesBitmap(
            @TelephonyManager.NetworkTypeBitMask int networkTypeBitmask, Message response);

     /**
     *  Query the allowed network types setting.
     *
     * @param response is callback message to report allowed network types bitmask
     */
    void getAllowedNetworkTypesBitmap(Message response);

    /**
     * Enable/Disable E-UTRA-NR Dual Connectivity
     * @param nrDualConnectivityState expected NR dual connectivity state
     * This can be passed following states
     * <ol>
     * <li>Enable NR dual connectivity {@link TelephonyManager#NR_DUAL_CONNECTIVITY_ENABLE}
     * <li>Disable NR dual connectivity {@link TelephonyManager#NR_DUAL_CONNECTIVITY_DISABLE}
     * <li>Disable NR dual connectivity and force secondary cell to be released
     * {@link TelephonyManager#NR_DUAL_CONNECTIVITY_DISABLE_IMMEDIATE}
     * </ol>
     */
    default void setNrDualConnectivityState(int nrDualConnectivityState,
            Message message, WorkSource workSource) {}

    /**
     * Is E-UTRA-NR Dual Connectivity enabled
     */
    default void isNrDualConnectivityEnabled(Message message, WorkSource workSource) {}

    /**
     * Enable or disable Voice over NR (VoNR)
     * @param enabled enable or disable VoNR.
     */
    default void setVoNrEnabled(boolean enabled, Message message, WorkSource workSource) {}

    /**
     * Is voice over NR enabled
     */
    default void isVoNrEnabled(Message message, WorkSource workSource) {}

    /**
     * Request to enable/disable network state change notifications when
     * location information (lac and/or cid) has changed.
     *
     * @param enable true to enable, false to disable
     * @param workSource calling WorkSource
     * @param response callback message
     */
    default void setLocationUpdates(boolean enable, WorkSource workSource, Message response) {}

    /**
     * To be deleted
     */
    default void setLocationUpdates(boolean enable, Message response) {}

    /**
     * Gets the default SMSC address.
     *
     * @param result Callback message contains the SMSC address.
     */
    @UnsupportedAppUsage
    void getSmscAddress(Message result);

    /**
     * Sets the default SMSC address.
     *
     * @param address new SMSC address
     * @param result Callback message is empty on completion
     */
    @UnsupportedAppUsage
    void setSmscAddress(String address, Message result);

    /**
     * Indicates whether there is storage available for new SMS messages.
     * @param available true if storage is available
     * @param result callback message
     */
    @UnsupportedAppUsage
    void reportSmsMemoryStatus(boolean available, Message result);

    /**
     * Indicates to the vendor ril that StkService is running
     * and is ready to receive RIL_UNSOL_STK_XXXX commands.
     *
     * @param result callback message
     */
    @UnsupportedAppUsage
    void reportStkServiceIsRunning(Message result);

    @UnsupportedAppUsage
    void invokeOemRilRequestRaw(byte[] data, Message response);

    /**
     * Sends carrier specific information to the vendor ril that can be used to
     * encrypt the IMSI and IMPI.
     *
     * @param publicKey the public key of the carrier used to encrypt IMSI/IMPI.
     * @param keyIdentifier the key identifier is optional information that is carrier
     *        specific.
     * @param response callback message
     */
    void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo,
                                         Message response);

    void invokeOemRilRequestStrings(String[] strings, Message response);

    /**
     * Fires when RIL_UNSOL_OEM_HOOK_RAW is received from the RIL.
     */
    void setOnUnsolOemHookRaw(Handler h, int what, Object obj);
    void unSetOnUnsolOemHookRaw(Handler h);

    /**
     * Send TERMINAL RESPONSE to the SIM, after processing a proactive command
     * sent by the SIM.
     *
     * @param contents  String containing SAT/USAT response in hexadecimal
     *                  format starting with first byte of response data. See
     *                  TS 102 223 for details.
     * @param response  Callback message
     */
    @UnsupportedAppUsage
    public void sendTerminalResponse(String contents, Message response);

    /**
     * Send ENVELOPE to the SIM, after processing a proactive command sent by
     * the SIM.
     *
     * @param contents  String containing SAT/USAT response in hexadecimal
     *                  format starting with command tag. See TS 102 223 for
     *                  details.
     * @param response  Callback message
     */
    @UnsupportedAppUsage
    public void sendEnvelope(String contents, Message response);

    /**
     * Send ENVELOPE to the SIM, such as an SMS-PP data download envelope
     * for a SIM data download message. This method has one difference
     * from {@link #sendEnvelope}: The SW1 and SW2 status bytes from the UICC response
     * are returned along with the response data.
     *
     * response.obj will be an AsyncResult
     * response.obj.result will be an IccIoResult on success
     *
     * @param contents  String containing SAT/USAT response in hexadecimal
     *                  format starting with command tag. See TS 102 223 for
     *                  details.
     * @param response  Callback message
     */
    public void sendEnvelopeWithStatus(String contents, Message response);

    /**
     * Accept or reject the call setup request from SIM.
     *
     * @param accept   true if the call is to be accepted, false otherwise.
     * @param response Callback message
     */
    @UnsupportedAppUsage
    public void handleCallSetupRequestFromSim(boolean accept, Message response);

    /**
     * Activate or deactivate cell broadcast SMS for GSM.
     *
     * @param activate
     *            true = activate, false = deactivate
     * @param result Callback message is empty on completion
     */
    public void setGsmBroadcastActivation(boolean activate, Message result);

    /**
     * Configure cell broadcast SMS for GSM.
     *
     * @param response Callback message is empty on completion
     */
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] config, Message response);

    /**
     * Query the current configuration of cell broadcast SMS of GSM.
     *
     * @param response
     *        Callback message contains the configuration from the modem
     *        on completion
     */
    public void getGsmBroadcastConfig(Message response);

    //***** new Methods for CDMA support

    /**
     * Request the device ESN / MEID / IMEI / IMEISV.
     * "response" is const char **
     *   [0] is IMEI if GSM subscription is available
     *   [1] is IMEISV if GSM subscription is available
     *   [2] is ESN if CDMA subscription is available
     *   [3] is MEID if CDMA subscription is available
     */
    public void getDeviceIdentity(Message response);

    /**
     * Request the device MDN / H_SID / H_NID / MIN.
     * "response" is const char **
     *   [0] is MDN if CDMA subscription is available
     *   [1] is a comma separated list of H_SID (Home SID) in decimal format
     *       if CDMA subscription is available
     *   [2] is a comma separated list of H_NID (Home NID) in decimal format
     *       if CDMA subscription is available
     *   [3] is MIN (10 digits, MIN2+MIN1) if CDMA subscription is available
     */
    @UnsupportedAppUsage
    public void getCDMASubscription(Message response);

    /**
     * Send Flash Code.
     * "response" is is NULL
     *   [0] is a FLASH string
     */
    public void sendCDMAFeatureCode(String FeatureCode, Message response);

    /** Set the Phone type created */
    @UnsupportedAppUsage
    void setPhoneType(int phoneType);

    /**
     *  Query the CDMA roaming preference setting
     *
     * @param response is callback message to report one of  CDMA_RM_*
     */
    void queryCdmaRoamingPreference(Message response);

    /**
     *  Requests to set the CDMA roaming preference
     * @param cdmaRoamingType one of  CDMA_RM_*
     * @param response is callback message
     */
    void setCdmaRoamingPreference(int cdmaRoamingType, Message response);

    /**
     *  Requests to set the CDMA subscription mode
     * @param cdmaSubscriptionType one of  CDMA_SUBSCRIPTION_*
     * @param response is callback message
     */
    void setCdmaSubscriptionSource(int cdmaSubscriptionType, Message response);

    /**
     *  Requests to get the CDMA subscription srouce
     * @param response is callback message
     */
    void getCdmaSubscriptionSource(Message response);

    /**
     *  Set the TTY mode
     *
     * @param ttyMode one of the following:
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_OFF}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_FULL}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_HCO}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_VCO}
     * @param response is callback message
     */
    @UnsupportedAppUsage
    void setTTYMode(int ttyMode, Message response);

    /**
     *  Query the TTY mode
     * (AsyncResult)response.obj).result is an int[] with element [0] set to
     * tty mode:
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_OFF}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_FULL}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_HCO}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_VCO}
     * @param response is callback message
     */
    @UnsupportedAppUsage
    void queryTTYMode(Message response);

    /**
     * Setup a packet data connection On successful completion, the result
     * message will return a SetupDataResult object containing the connection information.
     *
     * @param accessNetworkType
     *            Access network to use. Values is one of AccessNetworkConstants.AccessNetworkType.
     * @param dataProfile
     *            Data profile for data call setup
     * @param isRoaming
     *            Device is roaming or not
     * @param allowRoaming
     *            Flag indicating data roaming is enabled or not
     * @param reason
     *            The reason for data setup
     * @param linkProperties
     *            If the reason is for handover, this indicates the link properties of the existing
     *            data connection
     * @param pduSessionId the pdu session id to be used for this data call.
     *            The standard range of values are 1-15 while 0 means no pdu session id was attached
     *            to this call. Reference: 3GPP TS 24.007 section 11.2.3.1b.
     * @param sliceInfo used within the data connection when a handover occurs from EPDG to 5G.
     *            The value is null unless the access network is
     *            {@link android.telephony.AccessNetworkConstants.AccessNetworkType#NGRAN} and a
     *            handover is occurring from EPDG to 5G.  If the slice passed is rejected, then
     *            {@link DataCallResponse#getCause()} is
     *            {@link android.telephony.DataFailCause#SLICE_REJECTED}.
     * @param trafficDescriptor TrafficDescriptor for which data connection needs to be established.
     *            It is used for URSP traffic matching as described in 3GPP TS 24.526 Section 4.2.2.
     *            It includes an optional DNN which, if present, must be used for traffic matching;
     *            it does not specify the end point to be used for the data call.
     * @param matchAllRuleAllowed indicates if using default match-all URSP rule for this request is
     *            allowed. If false, this request must not use the match-all URSP rule and if a
     *            non-match-all rule is not found (or if URSP rules are not available) then
     *            {@link DataCallResponse#getCause()} is
     *            {@link android.telephony.DataFailCause#MATCH_ALL_RULE_NOT_ALLOWED}. This is needed
     *            as some requests need to have a hard failure if the intention cannot be met,
     *            for example, a zero-rating slice.
     * @param result
     *            Callback message
     */
    void setupDataCall(int accessNetworkType, DataProfile dataProfile, boolean isRoaming,
            boolean allowRoaming, int reason, LinkProperties linkProperties, int pduSessionId,
            NetworkSliceInfo sliceInfo, TrafficDescriptor trafficDescriptor,
            boolean matchAllRuleAllowed, Message result);

    /**
     * Deactivate packet data connection
     *
     * @param cid
     *            The connection ID
     * @param reason
     *            Data disconnect reason.
     * @param result
     *            Callback message is empty on completion
     */
    public void deactivateDataCall(int cid, int reason, Message result);

    /**
     * Activate or deactivate cell broadcast SMS for CDMA.
     *
     * @param activate
     *            true = activate, false = deactivate
     * @param result
     *            Callback message is empty on completion
     */
    @UnsupportedAppUsage
    public void setCdmaBroadcastActivation(boolean activate, Message result);

    /**
     * Configure cdma cell broadcast SMS.
     *
     * @param response
     *            Callback message is empty on completion
     */
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs, Message response);

    /**
     * Query the current configuration of cdma cell broadcast SMS.
     *
     * @param result
     *            Callback message contains the configuration from the modem on completion
     */
    @UnsupportedAppUsage
    public void getCdmaBroadcastConfig(Message result);

    /**
     *  Requests the radio's system selection module to exit emergency callback mode.
     *  This function should only be called from for CDMA.
     *
     * @param response callback message
     */
    @UnsupportedAppUsage
    public void exitEmergencyCallbackMode(Message response);

    /**
     * Request the status of the ICC and UICC cards.
     *
     * @param result
     *          Callback message containing {@link IccCardStatus} structure for the card.
     */
    @UnsupportedAppUsage
    public void getIccCardStatus(Message result);

    /**
     * Request the status of all the physical UICC slots.
     *
     * @param result Callback message containing a {@link java.util.ArrayList} of
     * {@link com.android.internal.telephony.uicc.IccSlotStatus} instances for all the slots.
     */
    void getIccSlotsStatus(Message result);

    /**
     * Set the mapping from logical slots to physical slots.
     *
     * @param physicalSlots Mapping from logical slots to physical slots.
     * @param result Callback message is empty on completion.
     */
    void setLogicalToPhysicalSlotMapping(int[] physicalSlots, Message result);

    /**
     * Request the SIM application on the UICC to perform authentication
     * challenge/response algorithm. The data string and challenge response are
     * Base64 encoded Strings.
     * Can support EAP-SIM, EAP-AKA with results encoded per 3GPP TS 31.102.
     *
     * @param authContext is the P2 parameter that specifies the authentication context per 3GPP TS
     *                    31.102 (Section 7.1.2)
     * @param data authentication challenge data
     * @param aid used to determine which application/slot to send the auth command to. See ETSI
     *            102.221 8.1 and 101.220 4
     * @param response a callback message with the String response in the obj field
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void requestIccSimAuthentication(int authContext, String data, String aid, Message response);

    /**
     * Get the current Voice Radio Technology.
     *
     * AsyncResult.result is an int array with the first value
     * being one of the ServiceState.RIL_RADIO_TECHNOLOGY_xxx values.
     *
     * @param result is sent back to handler and result.obj is a AsyncResult
     */
    void getVoiceRadioTechnology(Message result);

    /**
     * Return the current set of CellInfo records
     *
     * AsyncResult.result is a of Collection<CellInfo>
     *
     * @param result is sent back to handler and result.obj is a AsyncResult
     * @param workSource calling WorkSource
     */
    default void getCellInfoList(Message result, WorkSource workSource) {}

    /**
     * Sets the minimum time in milli-seconds between when RIL_UNSOL_CELL_INFO_LIST
     * should be invoked.
     *
     * The default, 0, means invoke RIL_UNSOL_CELL_INFO_LIST when any of the reported
     * information changes. Setting the value to INT_MAX(0x7fffffff) means never issue
     * A RIL_UNSOL_CELL_INFO_LIST.
     *
     *

     * @param rateInMillis is sent back to handler and result.obj is a AsyncResult
     * @param response.obj is AsyncResult ar when sent to associated handler
     *                        ar.exception carries exception on failure or null on success
     *                        otherwise the error.
     * @param workSource calling WorkSource
     */
    default void setCellInfoListRate(int rateInMillis, Message response, WorkSource workSource){}

    /**
     * Fires when RIL_UNSOL_CELL_INFO_LIST is received from the RIL.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    void registerForCellInfoList(Handler h, int what, Object obj);
    void unregisterForCellInfoList(Handler h);

    /**
     * Fires when a new {@link android.telephony.PhysicalChannelConfig} list is received from the
     * RIL.
     */
    void registerForPhysicalChannelConfiguration(Handler h, int what, Object obj);

    /**
     * Unregisters the handler for {@link android.telephony.PhysicalChannelConfig} updates.
     */
    void unregisterForPhysicalChannelConfiguration(Handler h);

    /**
     * Set Initial Attach Apn
     *
     * @param dataProfile
     *            data profile for initial APN attach
     * @param isRoaming
     *            indicating the device is roaming or not
     * @param result
     *            callback message contains the information of SUCCESS/FAILURE
     */
    void setInitialAttachApn(DataProfile dataProfile, boolean isRoaming, Message result);

    /**
     * Set data profiles in modem
     *
     * @param dps
     *            Array of the data profiles set to modem
     * @param isRoaming
     *            Indicating if the device is roaming or not
     * @param result
     *            callback message contains the information of SUCCESS/FAILURE
     */
    void setDataProfile(DataProfile[] dps, boolean isRoaming, Message result);

    /**
     * Notifiy that we are testing an emergency call
     */
    public void testingEmergencyCall();

    /**
     * Open a logical channel to the SIM.
     *
     * Input parameters equivalent to TS 27.007 AT+CCHO command.
     *
     * @param AID Application id. See ETSI 102.221 and 101.220.
     * @param p2 P2 parameter (described in ISO 7816-4).
     * @param response Callback message. response.obj will be an int [1] with
     *            element [0] set to the id of the logical channel.
     */
    public void iccOpenLogicalChannel(String AID, int p2, Message response);

    /**
     * Close a previously opened logical channel to the SIM.
     *
     * Input parameters equivalent to TS 27.007 AT+CCHC command.
     *
     * @param channel Channel id. Id of the channel to be closed.
     * @param response Callback message.
     */
    public void iccCloseLogicalChannel(int channel, Message response);

    /**
     * Exchange APDUs with the SIM on a logical channel.
     *
     * Input parameters equivalent to TS 27.007 AT+CGLA command.
     *
     * @param channel Channel id of the channel to use for communication. Has to
     *            be greater than zero.
     * @param cla Class of the APDU command.
     * @param instruction Instruction of the APDU command.
     * @param p1 P1 value of the APDU command.
     * @param p2 P2 value of the APDU command.
     * @param p3 P3 value of the APDU command. If p3 is negative a 4 byte APDU
     *            is sent to the SIM.
     * @param data Data to be sent with the APDU.
     * @param response Callback message. response.obj.userObj will be
     *            an IccIoResult on success.
     */
    public void iccTransmitApduLogicalChannel(int channel, int cla, int instruction,
            int p1, int p2, int p3, String data, Message response);

    /**
     * Exchange APDUs with the SIM on a basic channel.
     *
     * Input parameters equivalent to TS 27.007 AT+CSIM command.
     *
     * @param cla Class of the APDU command.
     * @param instruction Instruction of the APDU command.
     * @param p1 P1 value of the APDU command.
     * @param p2 P2 value of the APDU command.
     * @param p3 P3 value of the APDU command. If p3 is negative a 4 byte APDU
     *            is sent to the SIM.
     * @param data Data to be sent with the APDU.
     * @param response Callback message. response.obj.userObj will be
     *            an IccIoResult on success.
     */
    public void iccTransmitApduBasicChannel(int cla, int instruction, int p1, int p2,
            int p3, String data, Message response);

    /**
     * Read one of the NV items defined in {@link RadioNVItems} / {@code ril_nv_items.h}.
     * Used for device configuration by some CDMA operators.
     *
     * @param itemID the ID of the item to read
     * @param response callback message with the String response in the obj field
     * @param workSource calling WorkSource
     */
    default void nvReadItem(int itemID, Message response, WorkSource workSource) {}

    /**
     * Write one of the NV items defined in {@link RadioNVItems} / {@code ril_nv_items.h}.
     * Used for device configuration by some CDMA operators.
     *
     * @param itemID the ID of the item to read
     * @param itemValue the value to write, as a String
     * @param response Callback message.
     * @param workSource calling WorkSource
     */
    default void nvWriteItem(int itemID, String itemValue, Message response,
            WorkSource workSource) {}

    /**
     * Update the CDMA Preferred Roaming List (PRL) in the radio NV storage.
     * Used for device configuration by some CDMA operators.
     *
     * @param preferredRoamingList byte array containing the new PRL
     * @param response Callback message.
     */
    void nvWriteCdmaPrl(byte[] preferredRoamingList, Message response);

    /**
     * Perform the specified type of NV config reset. The radio will be taken offline
     * and the device must be rebooted after erasing the NV. Used for device
     * configuration by some CDMA operators.
     *
     * @param resetType reset type: 1: reload NV reset, 2: erase NV reset, 3: factory NV reset
     * @param response Callback message.
     */
    void nvResetConfig(int resetType, Message response);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result contains a List of HardwareConfig
     */
    void getHardwareConfig (Message result);

    /**
     * @return version of the ril.
     */
    int getRilVersion();

    /**
     * @return the radio hal version
     */
    default HalVersion getHalVersion() {
        return HalVersion.UNKNOWN;
    }

   /**
     * Sets user selected subscription at Modem.
     *
     * @param slotId
     *          Slot.
     * @param appIndex
     *          Application index in the card.
     * @param subId
     *          Indicates subscription 0 or subscription 1.
     * @param subStatus
     *          Activation status, 1 = activate and 0 = deactivate.
     * @param result
     *          Callback message contains the information of SUCCESS/FAILURE.
     */
    // FIXME Update the doc and consider modifying the request to make more generic.
    @UnsupportedAppUsage
    public void setUiccSubscription(int slotId, int appIndex, int subId, int subStatus,
            Message result);

    /**
     * Whether the device modem supports reporting the EID in either the slot or card status or
     * through ATR.
     * @return true if the modem supports EID.
     */
    default boolean supportsEid() {
        return false;
    }

    /**
     * Tells the modem if data is allowed or not.
     *
     * @param allowed
     *          true = allowed, false = not alowed
     * @param result
     *          Callback message contains the information of SUCCESS/FAILURE.
     */
    // FIXME We may need to pass AID and slotid also
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void setDataAllowed(boolean allowed, Message result);

    /**
     * Inform RIL that the device is shutting down
     *
     * @param result Callback message contains the information of SUCCESS/FAILURE
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void requestShutdown(Message result);

    /**
     *  Set phone radio type and access technology.
     *
     *  @param rc the phone radio capability defined in
     *         RadioCapability. It's a input object used to transfer parameter to logic modem
     *
     *  @param result Callback message.
     */
    public void setRadioCapability(RadioCapability rc, Message result);

    /**
     *  Get phone radio capability
     *
     *  @param result Callback message.
     */
    public void getRadioCapability(Message result);

    /**
     * Registers the handler when phone radio capability is changed.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForRadioCapabilityChanged(Handler h, int what, Object obj);

    /**
     * Unregister for notifications when phone radio capability is changed.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForRadioCapabilityChanged(Handler h);

    /**
     * Start LCE (Link Capacity Estimation) service with a desired reporting interval.
     *
     * @param reportIntervalMs
     *        LCE info reporting interval (ms).
     *
     * @param result Callback message contains the current LCE status.
     * {byte status, int actualIntervalMs}
     */
    public void startLceService(int reportIntervalMs, boolean pullMode, Message result);

    /**
     * Stop LCE service.
     *
     * @param result Callback message contains the current LCE status:
     * {byte status, int actualIntervalMs}
     *
     */
    public void stopLceService(Message result);

    /**
     * Pull LCE service for capacity data.
     *
     * @param result Callback message contains the capacity info:
     * {int capacityKbps, byte confidenceLevel, byte lceSuspendedTemporarily}
     */
    public void pullLceData(Message result);

    /**
     * Register a LCE info listener.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForLceInfo(Handler h, int what, Object obj);

    /**
     * Unregister the LCE Info listener.
     *
     * @param h handle to be removed.
     */
    void unregisterForLceInfo(Handler h);

    /**
     *
     * Get modem activity info and stats
     *
     * @param result Callback message contains the modem activity information
     * @param workSource calling WorkSource
     */
    default void getModemActivityInfo(Message result, WorkSource workSource) {}

    /**
     * Set allowed carriers
     *
     * @param carriers Allowed carriers
     * @param result Callback message contains the result of the operation
     * @param workSource calling WorkSource
     */
    default void setAllowedCarriers(CarrierRestrictionRules carrierRestrictionRules,
            Message result, WorkSource workSource) {}

    /**
     * Get allowed carriers
     *
     * @param result Callback message contains the allowed carriers
     * @param workSource calling WorkSource
     */
    default void getAllowedCarriers(Message result, WorkSource workSource) {}

    /**
     * Register for unsolicited PCO data.  This information is carrier-specific,
     * opaque binary blobs destined for carrier apps for interpretation.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForPcoData(Handler h, int what, Object obj);

    /**
     * Unregister for PCO data.
     *
     * @param h handler to be removed
     */
    public void unregisterForPcoData(Handler h);

    /**
     * Register for modem reset indication.
     *
     * @param h  Handler for the notification message
     * @param what User-defined message code
     * @param obj User object
     */
    void registerForModemReset(Handler h, int what, Object obj);

    /**
     * Unregister for modem reset
     *
     * @param h handler to be removed
     */
    void unregisterForModemReset(Handler h);

    /**
     * Send the updated device state
     *
     * @param stateType Device state type
     * @param state True if enabled, otherwise disabled
     * @param result callback message contains the information of SUCCESS/FAILURE
     */
    void sendDeviceState(int stateType, boolean state, Message result);

    /**
     * Send the device state to the modem
     *
     * @param filter unsolicited response filter. See DeviceStateMonitor.UnsolicitedResponseFilter
     * @param result callback message contains the information of SUCCESS/FAILURE
     */
    void setUnsolResponseFilter(int filter, Message result);

    /**
     * Sets or clears the signal strength reporting criteria for multiple RANs in one request.
     *
     * The reporting criteria are set individually for each combination of RAN and measurement type.
     * For each RAN type, if no reporting criteria are set, then the reporting of SignalStrength for
     * that RAN is implementation-defined. If any criteria are supplied for a RAN type, then
     * SignalStrength is only reported as specified by those criteria. For any RAN types not defined
     * by this HAL, reporting is implementation-defined.
     *
     * @param signalThresholdInfos Collection of SignalThresholdInfo specifying the reporting
     *        criteria. See SignalThresholdInfo for details.
     * @param result callback message contains the information of SUCCESS/FAILURE
     */
    void setSignalStrengthReportingCriteria(@NonNull List<SignalThresholdInfo> signalThresholdInfos,
            @Nullable Message result);

    /**
     * Send the link capacity reporting criteria to the modem
     *
     * @param hysteresisMs A hysteresis time in milliseconds. A value of 0 disables hysteresis.
     * @param hysteresisDlKbps An interval in kbps defining the required magnitude change between DL
     *     reports. A value of 0 disables hysteresis.
     * @param hysteresisUlKbps An interval in kbps defining the required magnitude change between UL
     *     reports. A value of 0 disables hysteresis.
     * @param thresholdsDlKbps An array of trigger thresholds in kbps for downlink reports. A size
     *     of 0 disables thresholds.
     * @param thresholdsUlKbps An array of trigger thresholds in kbps for uplink reports. A size
     *     of 0 disables thresholds.
     * @param ran RadioAccessNetwork for which to apply criteria.
     * @param result callback message contains the information of SUCCESS/FAILURE
     */
    void setLinkCapacityReportingCriteria(int hysteresisMs, int hysteresisDlKbps,
            int hysteresisUlKbps, int[] thresholdsDlKbps, int[] thresholdsUlKbps, int ran,
            Message result);

    /**
     * Set SIM card power up or down
     *
     * @param state  State of SIM (power down, power up, pass through)
     * - {@link android.telephony.TelephonyManager#CARD_POWER_DOWN}
     * - {@link android.telephony.TelephonyManager#CARD_POWER_UP}
     * - {@link android.telephony.TelephonyManager#CARD_POWER_UP_PASS_THROUGH}
     * @param result callback message contains the information of SUCCESS/FAILURE
     * @param workSource calling WorkSource
     */
    default void setSimCardPower(int state, Message result, WorkSource workSource) {}

    /**
     * Register for unsolicited Carrier Public Key.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForCarrierInfoForImsiEncryption(Handler h, int what, Object obj);

    /**
     * DeRegister for unsolicited Carrier Public Key.
     *
     * @param h Handler for notification message.
     */
    void unregisterForCarrierInfoForImsiEncryption(Handler h);

    /**
     * Register for unsolicited Network Scan result.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForNetworkScanResult(Handler h, int what, Object obj);

    /**
     * DeRegister for unsolicited Network Scan result.
     *
     * @param h Handler for notification message.
     */
    void unregisterForNetworkScanResult(Handler h);

    /**
     * Register for unsolicited NATT Keepalive Status Indications
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForNattKeepaliveStatus(Handler h, int what, Object obj);

    /**
     * Deregister for unsolicited NATT Keepalive Status Indications.
     *
     * @param h Handler for notification message.
     */
    void unregisterForNattKeepaliveStatus(Handler h);

    /**
     * Register for unsolicited Emergency Number List Indications
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForEmergencyNumberList(Handler h, int what, Object obj);

    /**
     * Deregister for unsolicited Emergency Number List Indications
     *
     * @param h Handler for notification message.
     */
    void unregisterForEmergencyNumberList(Handler h);

    /**
     * Start sending NATT Keepalive packets on a specified data connection
     *
     * @param contextId cid that identifies the data connection for this keepalive
     * @param packetData the keepalive packet data description
     * @param intervalMillis a time interval in ms between keepalive packet transmissions
     * @param result a Message to return to the requester
     */
    void startNattKeepalive(
            int contextId, KeepalivePacketData packetData, int intervalMillis, Message result);

    /**
     * Stop sending NATT Keepalive packets on a specified data connection
     *
     * @param sessionHandle the keepalive session handle (from the modem) to stop
     * @param result a Message to return to the requester
     */
    void stopNattKeepalive(int sessionHandle, Message result);

    /**
     * Enable or disable the logical modem.
     *
     * @param enable whether to enable or disable the modem
     * @param result a Message to return to the requester
     */
    default void enableModem(boolean enable, Message result) {};

    /**
     * Notify CommandsInterface that whether its corresponding slot is active or not. If not,
     * it means it has no RIL service or logical modem to connect to.
     *
     * @param active whether there's a matching active SIM slot.
     */
    default void onSlotActiveStatusChange(boolean active) {}

    /**
     * Query whether logical modem is enabled or disabled
     *
     * @param result a Message to return to the requester
     */
    default void getModemStatus(Message result) {};

    /**
     * Enable or disable uicc applications on the SIM.
     *
     * @param enable enable or disable UiccApplications on the SIM.
     * @param onCompleteMessage a Message to return to the requester
     */
    default void enableUiccApplications(boolean enable, Message onCompleteMessage) {}

    /**
     * Specify which bands modem's background scan must act on.
     * If {@code specifiers} is non-empty, the scan will be restricted to the bands specified.
     * Otherwise, it scans all bands.
     *
     * For example, CBRS is only on LTE band 48. By specifying this band,
     * modem saves more power.
     *
     * @param specifiers which bands to scan.
     * @param onComplete a message to send when complete.
     */
    default void setSystemSelectionChannels(@NonNull List<RadioAccessSpecifier> specifiers,
            Message onComplete) {}

    /**
     * Get which bands the modem's background scan is acting on.
     *
     * @param onComplete a message to send when complete.
     */
    default void getSystemSelectionChannels(Message onComplete) {}

    /**
     * Whether uicc applications are enabled or not.
     *
     * @param onCompleteMessage a Message to return to the requester
     */
    default void areUiccApplicationsEnabled(Message onCompleteMessage) {}

    /**
     * Whether {@link #enableUiccApplications} is supported, based on IRadio version.
     */
    default boolean canToggleUiccApplicationsEnablement() {
        return false;
    }

    default List<ClientRequestStats> getClientRequestStats() {
        return null;
    }

    /**
     * Registers the handler for RIL_UNSOL_BARRING_INFO_CHANGED events.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    default void registerForBarringInfoChanged(Handler h, int what, Object obj) {};

    /**
     * Unregisters the handler for RIL_UNSOL_BARRING_INFO_CHANGED events.
     *
     * @param h Handler for notification message.
     */
    default void unregisterForBarringInfoChanged(Handler h) {};

    /**
     * Get all the barring info for the current camped cell applicable to the current user.
     *
     * AsyncResult.result is the object of {@link android.telephony.BarringInfo}.
     *
     * @param result Message will be sent back to handler and result.obj will be the AsycResult.
     */
    default void getBarringInfo(Message result) {};

    /**
     * Allocates a pdu session id
     *
     * AsyncResult.result is the allocated pdu session id
     *
     * @param result Message will be sent back to handler and result.obj will be the AsycResult.
     *
     */
    default void allocatePduSessionId(Message result) {};

    /**
     * Release the pdu session id
     *
     * @param result Message that will be sent back to handler.
     * @param pduSessionId The id that was allocated and should now be released.
     *
     */
    default void releasePduSessionId(Message result, int pduSessionId) {};

    /**
     * Indicates that a handover has started
     *
     * @param result Message that will be sent back to handler.
     * @param callId Identifier associated with the data call
     */
    default void startHandover(Message result, int callId) {};

    /**
     * Indicates that a handover has been cancelled
     *
     * @param result Message that will be sent back to handler.
     * @param callId Identifier associated with the data call
     */
    default void cancelHandover(Message result, int callId) {};

    /**
     * Control the data throttling at modem.
     *
     * @param result Message that will be sent back to the requester
     * @param workSource calling Worksource
     * @param dataThrottlingAction the DataThrottlingAction that is being requested.
     *      Defined in android.hardware.radio@1.6.types.
     * @param completionWindowMillis milliseconds in which data throttling action has to be
     *      achieved.
     */
    default void setDataThrottling(Message result, WorkSource workSource,
            int dataThrottlingAction, long completionWindowMillis) {};

    /**
     * Request to get the current slicing configuration including URSP rules and
     * NSSAIs (configured, allowed and rejected).
     *
     * @param result Message that will be sent back to handler.
     */
    default void getSlicingConfig(Message result) {};

    /**
     * Request to enable/disable the mock modem service.
     * This is used in shell commands during CTS testing only.
     *
     * @param serviceName the service name which telephony wants to bind to
     */
    default boolean setModemService(String serviceName) {
        return true;
    };

   /**
     * Return the class name of the currently bound modem service.
     *
     * @return the class name of the modem service.
     */
    default String getModemService() {
        return "default";
    };

   /**
     * Request the SIM phonebook records of all activated UICC applications
     *
     * @param result Callback message containing the count of ADN valid record.
     */
    public void getSimPhonebookRecords(Message result);

   /**
     * Request the SIM phonebook Capacity of all activated UICC applications
     *
     */
    public void getSimPhonebookCapacity(Message result);

    /**
     * Request to insert/delete/update the SIM phonebook record
     *
     * @param phonebookRecordInfo adn record information to be updated
     * @param result Callback message containing the SIM phonebook record index.
     */
    public void updateSimPhonebookRecord(SimPhonebookRecord phonebookRecordInfo, Message result);

    /**
     * Registers the handler when the SIM phonebook is changed.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object .
     */
    public void registerForSimPhonebookChanged(Handler h, int what, Object obj);

    /**
     * Unregister for notifications when SIM phonebook has already init done.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSimPhonebookChanged(Handler h);

    /**
     * Registers the handler when a group of SIM phonebook records received.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSimPhonebookRecordsReceived(Handler h, int what, Object obj);

    /**
     * Unregister for notifications when a group of SIM phonebook records received.
     *
     * @param h Handler to be removed from the registrant list.
     */
     public void unregisterForSimPhonebookRecordsReceived(Handler h);

    /**
     * Set the UE's usage setting.
     *
     * @param result Callback message containing the success or failure status.
     * @param usageSetting the UE's usage setting, either VOICE_CENTRIC or DATA_CENTRIC.
     */
    default void setUsageSetting(Message result,
            /* @TelephonyManager.UsageSetting */ int usageSetting) {}

    /**
     * Get the UE's usage setting.
     *
     * @param result Callback message containing the usage setting (or a failure status).
     */
    default void getUsageSetting(Message result) {}
}

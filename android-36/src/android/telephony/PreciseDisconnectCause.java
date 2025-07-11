/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.telephony;

import android.annotation.FlaggedApi;
import android.annotation.SystemApi;

import com.android.internal.telephony.flags.Flags;

/**
 * Contains precise disconnect call causes generated by the framework and the RIL.
 * @hide
 */
@SystemApi
public final class PreciseDisconnectCause {

    /** The disconnect cause is not valid (Not received a disconnect cause).*/
    public static final int NOT_VALID                                        = -1;
    /** No disconnect cause provided. Generally a local disconnect or an incoming missed call. */
    public static final int NO_DISCONNECT_CAUSE_AVAILABLE                    = 0;
    /**
     * The destination cannot be reached because the number, although valid,
     * is not currently assigned.
     */
    public static final int UNOBTAINABLE_NUMBER                              = 1;
    /**
     * The user cannot be reached because the network through which the call has been routed does
     * not serve the destination desired.
     */
    public static final int NO_ROUTE_TO_DESTINATION                          = 3;
    /**
     * The channel most recently identified is not acceptable to the sending entity for use in this
     * call.
     */
    public static final int CHANNEL_UNACCEPTABLE                             = 6;
    /**
     * The mobile station (MS) has tried to access a service that the MS's network operator or
     * service provider is not prepared to allow.
     */
    public static final int OPERATOR_DETERMINED_BARRING                      = 8;
    /** One of the users involved in the call has requested that the call is cleared. */
    public static final int NORMAL                                           = 16;
    /** The called user is unable to accept another call. */
    public static final int BUSY                                             = 17;
    /**
     * The user does not respond to a call establishment message with either an alerting or connect
     * indication within the prescribed period of time allocated.
     */
    public static final int NO_USER_RESPONDING                               = 18;
    /**
     * The user has provided an alerting indication but has not provided a connect indication
     * within a prescribed period of time.
     */
    public static final int NO_ANSWER_FROM_USER                              = 19;
    /** The equipment sending this cause does not wish to accept this call. */
    public static final int CALL_REJECTED                                    = 21;
    /** The called number is no longer assigned. */
    public static final int NUMBER_CHANGED                                   = 22;
    /**
     * This cause is returned to the network when a mobile station clears an active call which is
     * being pre-empted by another call with higher precedence.
     */
    public static final int PREEMPTION                                       = 25;
    /**
     * The destination indicated by the mobile station cannot be reached because the interface to
     * the destination is not functioning correctly.
     */
    public static final int DESTINATION_OUT_OF_ORDER                         = 27;
    /** The called party number is not a valid format or is not complete. */
    public static final int INVALID_NUMBER_FORMAT                            = 28;
    /** The facility requested by user can not be provided by the network. */
    public static final int FACILITY_REJECTED                                = 29;
    /** Provided in response to a STATUS ENQUIRY message. */
    public static final int STATUS_ENQUIRY                                   = 30;
    /** Reports a normal disconnect only when no other normal cause applies. */
    public static final int NORMAL_UNSPECIFIED                               = 31;
    /** There is no channel presently available to handle the call. */
    public static final int NO_CIRCUIT_AVAIL                                 = 34;
    /**
     * The network is not functioning correctly and that the condition is likely to last a
     * relatively long period of time.
     */
    public static final int NETWORK_OUT_OF_ORDER                             = 38;
    /**
     * The network is not functioning correctly and the condition is not likely to last a long
     * period of time.
     */
    public static final int TEMPORARY_FAILURE                                = 41;
    /** The switching equipment is experiencing a period of high traffic. */
    public static final int SWITCHING_CONGESTION                             = 42;
    /** The network could not deliver access information to the remote user as requested. */
    public static final int ACCESS_INFORMATION_DISCARDED                     = 43;
    /** The channel cannot be provided. */
    public static final int CHANNEL_NOT_AVAIL                                = 44;
    /**
     * This cause is used to report a resource unavailable event only when no other cause in the
     * resource unavailable class applies.
     */
    public static final int RESOURCES_UNAVAILABLE_OR_UNSPECIFIED             = 47;
    /** The requested quality of service (ITU-T X.213) cannot be provided. */
    public static final int QOS_NOT_AVAIL                                    = 49;
    /**
     * The facility could not be provided by the network because the user has no complete
     * subscription.
     */
    public static final int REQUESTED_FACILITY_NOT_SUBSCRIBED                = 50;
    /** Incoming calls are not allowed within this calling user group (CUG). */
    public static final int INCOMING_CALLS_BARRED_WITHIN_CUG                 = 55;
    /** The mobile station is not authorized to use bearer capability requested. */
    public static final int BEARER_CAPABILITY_NOT_AUTHORIZED                 = 57;
    /** The requested bearer capability is not available at this time. */
    public static final int BEARER_NOT_AVAIL                                 = 58;
    /** The service option is not available at this time. */
    public static final int SERVICE_OPTION_NOT_AVAILABLE                     = 63;
    /** The equipment sending this cause does not support the bearer capability requested. */
    public static final int BEARER_SERVICE_NOT_IMPLEMENTED                   = 65;
    /** The call clearing is due to ACM being greater than or equal to ACMmax. */
    public static final int ACM_LIMIT_EXCEEDED                               = 68;
    /** The equipment sending this cause does not support the requested facility. */
    public static final int REQUESTED_FACILITY_NOT_IMPLEMENTED               = 69;
    /**
     * The equipment sending this cause only supports the restricted version of the requested bearer
     * capability.
     */
    public static final int ONLY_DIGITAL_INFORMATION_BEARER_AVAILABLE        = 70;
    /** The service requested is not implemented at network. */
    public static final int SERVICE_OR_OPTION_NOT_IMPLEMENTED                = 79;
    /**
     * The equipment sending this cause has received a message with a transaction identifier
     * which is not currently in use on the mobile station network interface.
     */
    public static final int INVALID_TRANSACTION_IDENTIFIER                   = 81;
    /**
     * The called user for the incoming CUG call is not a member of the specified calling user
     * group (CUG).
     */
    public static final int USER_NOT_MEMBER_OF_CUG                           = 87;
    /** The equipment sending this cause has received a request which can't be accomodated. */
    public static final int INCOMPATIBLE_DESTINATION                         = 88;
    /** This cause is used to report receipt of a message with semantically incorrect contents. */
    public static final int SEMANTICALLY_INCORRECT_MESSAGE                   = 95;
    /**
     * The equipment sending this cause has received a message with a non-semantical mandatory
     * information element (IE) error.
     */
    public static final int INVALID_MANDATORY_INFORMATION                    = 96;
    /**
     * This is sent in response to a message which is not defined, or defined but not implemented
     * by the equipment sending this cause.
     */
    public static final int MESSAGE_TYPE_NON_IMPLEMENTED                     = 97;
    /**
     * The equipment sending this cause has received a message not compatible with the protocol
     * state.
     */
    public static final int MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE  = 98;
    /**
     * The equipment sending this cause has received a message which includes information
     * elements not recognized because its identifier is not defined or it is defined but not
     * implemented by the equipment sending the cause.
     */
    public static final int INFORMATION_ELEMENT_NON_EXISTENT                 = 99;
    /** The equipment sending this cause has received a message with conditional IE errors. */
    public static final int CONDITIONAL_IE_ERROR                             = 100;
    /** The message has been received which is incompatible with the protocol state. */
    public static final int MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE       = 101;
    /**
     * The procedure has been initiated by the expiry of a timer in association with
     * 3GPP TS 24.008 error handling procedures.
     */
    public static final int RECOVERY_ON_TIMER_EXPIRED                        = 102;
    /**
     * This protocol error event is reported only when no other cause in the protocol error class
     * applies.
     */
    public static final int PROTOCOL_ERROR_UNSPECIFIED                       = 111;
    /**
     * Interworking with a network which does not provide causes for actions it takes thus, the
     * precise cause for a message which is being sent cannot be ascertained.
     */
    public static final int INTERWORKING_UNSPECIFIED                         = 127;
    /** The call is restricted. */
    public static final int CALL_BARRED                                      = 240;
    /** The call is blocked by the Fixed Dialing Number list. */
    public static final int FDN_BLOCKED                                      = 241;
    /** The given IMSI is not known at the Visitor Location Register (VLR) TS 24.008 cause . */
    public static final int IMSI_UNKNOWN_IN_VLR                              = 242;
    /**
     * The network does not accept emergency call establishment using an IMEI or not accept attach
     * procedure for emergency services using an IMEI.
     */
    public static final int IMEI_NOT_ACCEPTED                                = 243;
    /** The call cannot be established because RADIO is OFF. */
    public static final int RADIO_OFF                                        = 247;
    /** The call cannot be established because of no cell coverage. */
    public static final int OUT_OF_SRV                                       = 248;
    /** The call cannot be established because of no valid SIM. */
    public static final int NO_VALID_SIM                                     = 249;
    /** The call is dropped or failed internally by modem. */
    public static final int RADIO_INTERNAL_ERROR                             = 250;
    /** Call failed because of UE timer expired while waiting for a response from network. */
    public static final int NETWORK_RESP_TIMEOUT                             = 251;
    /** Call failed because of a network reject. */
    public static final int NETWORK_REJECT                                   = 252;
    /** Call failed because of radio access failure. ex. RACH failure. */
    public static final int RADIO_ACCESS_FAILURE                             = 253;
    /** Call failed/dropped because of a Radio Link Failure (RLF). */
    public static final int RADIO_LINK_FAILURE                               = 254;
    /** Call failed/dropped because of radio link lost. */
    public static final int RADIO_LINK_LOST                                  = 255;
    /** Call failed because of a radio uplink issue. */
    public static final int RADIO_UPLINK_FAILURE                             = 256;
    /** Call failed because of a RRC (Radio Resource Control) connection setup failure. */
    public static final int RADIO_SETUP_FAILURE                              = 257;
    /** Call failed/dropped because of RRC (Radio Resource Control) connection release from NW. */
    public static final int RADIO_RELEASE_NORMAL                             = 258;
    /**
     * Call failed/dropped because of RRC (Radio Resource Control) abnormally released by
     * modem/network.
     */
    public static final int RADIO_RELEASE_ABNORMAL                           = 259;
    /** Call setup failed because of access class barring. */
    public static final int ACCESS_CLASS_BLOCKED                             = 260;
    /** Call failed/dropped because of a network detach. */
    public static final int NETWORK_DETACH                                   = 261;

    /**
     * Dialing emergency calls is currently unavailable.
     * The call should be redialed on the other subscription silently.
     * If there are no other subscriptions available then the call may be redialed
     * on this subscription again.
     */
    @FlaggedApi(Flags.FLAG_USE_OEM_DOMAIN_SELECTION_SERVICE)
    public static final int EMERGENCY_TEMP_FAILURE                           = 325;
    /**
     * Dialing emergency calls is currently unavailable.
     * The call should be redialed on the other subscription silently.
     * If there are no other subscriptions available then the call should not
     * be redialed on this subscription again.
     */
    @FlaggedApi(Flags.FLAG_USE_OEM_DOMAIN_SELECTION_SERVICE)
    public static final int EMERGENCY_PERM_FAILURE                           = 326;

    /**
     * Mobile station (MS) is locked until next power cycle.
     * @deprecated Legacy CDMA is unsupported.
     */
    @FlaggedApi(Flags.FLAG_DEPRECATE_CDMA)
    @Deprecated
    public static final int CDMA_LOCKED_UNTIL_POWER_CYCLE                    = 1000;
    /**
     * Drop call.
     * @deprecated Legacy CDMA is unsupported.
     */
    @FlaggedApi(Flags.FLAG_DEPRECATE_CDMA)
    @Deprecated
    public static final int CDMA_DROP                                        = 1001;
    /**
     * INTERCEPT order received, Mobile station (MS) state idle entered.
     * @deprecated Legacy CDMA is unsupported.
     */
    @FlaggedApi(Flags.FLAG_DEPRECATE_CDMA)
    @Deprecated
    public static final int CDMA_INTERCEPT                                   = 1002;
    /**
     * Mobile station (MS) has been redirected, call is cancelled.
     * @deprecated Legacy CDMA is unsupported.
     */
    @FlaggedApi(Flags.FLAG_DEPRECATE_CDMA)
    @Deprecated
    public static final int CDMA_REORDER                                     = 1003;
    /**
     * Service option rejection.
     * @deprecated Legacy CDMA is unsupported.
     */
    @FlaggedApi(Flags.FLAG_DEPRECATE_CDMA)
    @Deprecated
    public static final int CDMA_SO_REJECT                                   = 1004;
    /**
     * Requested service is rejected, retry delay is set.
     * @deprecated Legacy CDMA is unsupported.
     */
    @FlaggedApi(Flags.FLAG_DEPRECATE_CDMA)
    @Deprecated
    public static final int CDMA_RETRY_ORDER                                 = 1005;
    /**
     * Unable to obtain access to the CDMA system.
     * @deprecated Legacy CDMA is unsupported.
     */
    @FlaggedApi(Flags.FLAG_DEPRECATE_CDMA)
    @Deprecated
    public static final int CDMA_ACCESS_FAILURE                              = 1006;
    /**
     * Not a preempted call.
     * @deprecated Legacy CDMA is unsupported.
     */
    @FlaggedApi(Flags.FLAG_DEPRECATE_CDMA)
    @Deprecated
    public static final int CDMA_PREEMPTED                                   = 1007;
    /**
     * Not an emergency call.
     * @deprecated Legacy CDMA is unsupported.
     */
    @FlaggedApi(Flags.FLAG_DEPRECATE_CDMA)
    @Deprecated
    public static final int CDMA_NOT_EMERGENCY                               = 1008;
    /**
     * Access Blocked by CDMA network.
     * @deprecated Legacy CDMA is unsupported.
     */
    @FlaggedApi(Flags.FLAG_DEPRECATE_CDMA)
    @Deprecated
    public static final int CDMA_ACCESS_BLOCKED                              = 1009;

    /** Call was disconnected with cause code retry over volte. */
    @FlaggedApi(Flags.FLAG_ADD_IMS_REDIAL_CODES_FOR_EMERGENCY_CALLS)
    public static final int EMERGENCY_REDIAL_ON_IMS                                = 3001;
    /** Call was disconnected with cause code retry over vowifi. */
    @FlaggedApi(Flags.FLAG_ADD_IMS_REDIAL_CODES_FOR_EMERGENCY_CALLS)
    public static final int EMERGENCY_REDIAL_ON_VOWIFI                             = 3002;

    /* OEM specific error codes. To be used by OEMs when they don't want to
       reveal error code which would be replaced by ERROR_UNSPECIFIED */
    public static final int OEM_CAUSE_1                                      = 0xf001;
    public static final int OEM_CAUSE_2                                      = 0xf002;
    public static final int OEM_CAUSE_3                                      = 0xf003;
    public static final int OEM_CAUSE_4                                      = 0xf004;
    public static final int OEM_CAUSE_5                                      = 0xf005;
    public static final int OEM_CAUSE_6                                      = 0xf006;
    public static final int OEM_CAUSE_7                                      = 0xf007;
    public static final int OEM_CAUSE_8                                      = 0xf008;
    public static final int OEM_CAUSE_9                                      = 0xf009;
    public static final int OEM_CAUSE_10                                     = 0xf00a;
    public static final int OEM_CAUSE_11                                     = 0xf00b;
    public static final int OEM_CAUSE_12                                     = 0xf00c;
    public static final int OEM_CAUSE_13                                     = 0xf00d;
    public static final int OEM_CAUSE_14                                     = 0xf00e;
    public static final int OEM_CAUSE_15                                     = 0xf00f;

    /** Disconnected due to unspecified reasons. */
    public static final int ERROR_UNSPECIFIED                                = 0xffff;

    /** Private constructor to avoid class instantiation. */
    private PreciseDisconnectCause() {
        // Do nothing.
    }
}

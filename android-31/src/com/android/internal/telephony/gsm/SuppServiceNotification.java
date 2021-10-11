/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.internal.telephony.gsm;

import android.telephony.PhoneNumberUtils;

/**
 * Represents a Supplementary Service Notification received from the network.
 *
 * {@hide}
 */
public class SuppServiceNotification {
    /** Type of notification: 0 = code1; 1 = code2 */
    public int notificationType;
    /** TS 27.007 7.17 "code1" or "code2" */
    public int code;
    /** TS 27.007 7.17 "index" */
    public int index;
    /** TS 27.007 7.17 "type" (MT only) */
    public int type;
    /** TS 27.007 7.17 "number" (MT only) */
    public String number;

    /** List of forwarded numbers, if any */
    public String[] history;

    /**
     * Notification type is from the "code 1" group (per TS 27.007 7.17).
     * This means the {@link #code} will be a code such as {@link #CODE_1_CALL_FORWARDED}.
     */
    public static final int NOTIFICATION_TYPE_CODE_1 = 0;

    /**
     * Notification type is from the "code 2" group (per TS 27.007 7.17).
     * This means the {@link #code} will be a code such as {@link #CODE_2_CALL_ON_HOLD}.
     */
    public static final int NOTIFICATION_TYPE_CODE_2 = 1;

    /**
     * Indicates that unconditional call forwarding is active.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_1}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_1_UNCONDITIONAL_CF_ACTIVE     = 0;

    /**
     * Indicates that some conditional call forwarding options are active.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_1}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_1_SOME_CF_ACTIVE              = 1;

    /**
     * Indicates that an outgoing call has been forwarded to another number.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_1}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_1_CALL_FORWARDED              = 2;

    /**
     * Indicates that an outgoing call is waiting.  This means that the called party is already in
     * another call and is hearing the call waiting tone.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_1}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_1_CALL_IS_WAITING             = 3;

    /**
     * Indicates that an outgoing call is to a number in a closed user group.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_1}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_1_CUG_CALL                    = 4;

    /**
     * Indicates that outgoing calls are barred.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_1}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_1_OUTGOING_CALLS_BARRED       = 5;

    /**
     * Indicates that incoming calls are barred.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_1}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_1_INCOMING_CALLS_BARRED       = 6;

    /**
     * Indicates that CLIR suppression has been rejected for an outgoing call.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_1}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_1_CLIR_SUPPRESSION_REJECTED   = 7;

    /**
     * Indicates that an outgoing call bas been deflected to another number.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_1}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_1_CALL_DEFLECTED              = 8;

    /**
     * Indicates that an incoming call is a forwarded call.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_2}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_2_FORWARDED_CALL              = 0;

    /**
     * Indicates that an incoming call is from a member of a closed user group.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_2}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_2_CUG_CALL                    = 1;

    /**
     * Indicates that a call has been remotely put on hold.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_2}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_2_CALL_ON_HOLD                = 2;

    /**
     * Indicates that a call has been remotely resumed (retrieved).
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_2}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_2_CALL_RETRIEVED              = 3;

    /**
     * Indicates that a conference call has been entered.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_2}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_2_MULTI_PARTY_CALL            = 4;

    /**
     * Indicates that an ongoing call on hold has been released.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_2}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_2_ON_HOLD_CALL_RELEASED       = 5;

    /**
     * Indicates that a forward check message was received.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_2}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_2_FORWARD_CHECK_RECEIVED      = 6;

    /**
     * Indicates that a call is being connected (alerting) with another party as a result of an
     * explicit call transfer operation.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_2}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_2_CALL_CONNECTING_ECT         = 7;

    /**
     * Indicates that a call has been connected with another party as a result of an explicit call
     * transfer operation.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_2}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_2_CALL_CONNECTED_ECT          = 8;

    /**
     * Indicates that an outgoing call has been deflected to another number.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_2}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_2_DEFLECTED_CALL              = 9;

    /**
     * Indicates that an additional incoming call has been forwarded.
     * Valid {@link #code} when {@link #type} is {@link #NOTIFICATION_TYPE_CODE_2}.
     * See TS 27.007 7.17.
     */
    public static final int CODE_2_ADDITIONAL_CALL_FORWARDED   = 10;

    @Override
    public String toString()
    {
        return super.toString() + " mobile"
            + (notificationType == 0 ? " originated " : " terminated ")
            + " code: " + code
            + " index: " + index
            + " history: " + history
            + " \""
            + PhoneNumberUtils.stringFromStringAndTOA(number, type) + "\" ";
    }

}

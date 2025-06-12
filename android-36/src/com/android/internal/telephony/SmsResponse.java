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

package com.android.internal.telephony;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;

/**
 * Object returned by the RIL upon successful completion of sendSMS.
 * Contains message reference and ackPdu.
 *
 */
public class SmsResponse {
    public static final int NO_ERROR_CODE = -1;

    /** Message reference of the just-sent SMS. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    int mMessageRef;
    /** ackPdu for the just-sent SMS. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    String mAckPdu;
    /**
     * errorCode: See 3GPP 27.005, 3.2.5 for GSM/UMTS,
     * 3GPP2 N.S0005 (IS-41C) Table 171 for CDMA, -1 if unknown or not applicable.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int mErrorCode;

    public long mMessageId;

    @UnsupportedAppUsage
    public SmsResponse(int messageRef, String ackPdu, int errorCode) {
        this(messageRef, ackPdu, errorCode, /* messageId= */ 0L);
    }

    public SmsResponse(int messageRef, String ackPdu, int errorCode, long messageId) {
        mMessageRef = messageRef;
        mAckPdu = ackPdu;
        mErrorCode = errorCode;
        mMessageId = messageId;
    }

    @Override
    public String toString() {
        String ret = "{ mMessageRef = " + mMessageRef
                        + ", mErrorCode = " + mErrorCode
                + ", mAckPdu = " + mAckPdu
                + ", " + SmsController.formatCrossStackMessageId(mMessageId) + "}";
        return ret;
    }
}

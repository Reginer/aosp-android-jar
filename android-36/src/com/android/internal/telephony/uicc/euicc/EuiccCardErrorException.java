/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.telephony.uicc.euicc;

import android.annotation.IntDef;
import android.annotation.Nullable;

import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.ApduSender;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The exception which is thrown when an error is returned in a successfully executed APDU command
 * sent to eUICC. This exception means the response status is no-error
 * ({@link ApduSender#STATUS_NO_ERROR}), but the action is failed due to eUICC specific logic.
 */
public class EuiccCardErrorException extends EuiccCardException {
    /** Operations */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "OPERATION_", value = {
            OPERATION_UNKNOWN,
            OPERATION_GET_PROFILE,
            OPERATION_PREPARE_DOWNLOAD,
            OPERATION_AUTHENTICATE_SERVER,
            OPERATION_CANCEL_SESSION,
            OPERATION_LOAD_BOUND_PROFILE_PACKAGE,
            OPERATION_LIST_NOTIFICATIONS,
            OPERATION_SET_NICKNAME,
            OPERATION_RETRIEVE_NOTIFICATION,
            OPERATION_REMOVE_NOTIFICATION_FROM_LIST,
            OPERATION_SWITCH_TO_PROFILE,
            OPERATION_DISABLE_PROFILE,
            OPERATION_DELETE_PROFILE,
            OPERATION_RESET_MEMORY,
            OPERATION_SET_DEFAULT_SMDP_ADDRESS,
    })
    public @interface OperationCode {}

    public static final int OPERATION_UNKNOWN = 0;
    public static final int OPERATION_GET_PROFILE = 1;
    public static final int OPERATION_PREPARE_DOWNLOAD = 2;
    public static final int OPERATION_AUTHENTICATE_SERVER = 3;
    public static final int OPERATION_CANCEL_SESSION = 4;
    public static final int OPERATION_LOAD_BOUND_PROFILE_PACKAGE = 5;
    public static final int OPERATION_LIST_NOTIFICATIONS = 6;
    public static final int OPERATION_SET_NICKNAME = 7;
    public static final int OPERATION_RETRIEVE_NOTIFICATION = 8;
    public static final int OPERATION_REMOVE_NOTIFICATION_FROM_LIST = 9;
    public static final int OPERATION_SWITCH_TO_PROFILE = 10;
    public static final int OPERATION_DISABLE_PROFILE = 11;
    public static final int OPERATION_DELETE_PROFILE = 12;
    public static final int OPERATION_RESET_MEMORY = 13;
    public static final int OPERATION_SET_DEFAULT_SMDP_ADDRESS = 14;

    private final @OperationCode int mOperationCode;
    private final int mErrorCode;
    private final @Nullable Asn1Node mErrorDetails;

    /**
     * Creates an exception with an error code in the response of an APDU command.
     *
     * @param errorCode The meaning of the code depends on each APDU command. It should always be
     *     non-negative.
     */
    public EuiccCardErrorException(@OperationCode int operationCode, int errorCode) {
        mOperationCode = operationCode;
        mErrorCode = errorCode;
        mErrorDetails = null;
    }

    /**
     * Creates an exception with an error code and the error details in the response of an APDU
     * command.
     *
     * @param errorCode The meaning of the code depends on each APDU command. It should always be
     *     non-negative.
     * @param errorDetails The content of the details depends on each APDU command.
     */
    public EuiccCardErrorException(@OperationCode int operationCode, int errorCode,
            @Nullable Asn1Node errorDetails) {
        mOperationCode = operationCode;
        mErrorCode = errorCode;
        mErrorDetails = errorDetails;
    }

    /** @return The error code. The meaning of the code depends on each APDU command. */
    public int getErrorCode() {
        return mErrorCode;
    }

    /** @return The operation code. */
    public int getOperationCode() {
        return mOperationCode;
    }

    /** @return The error details. The meaning of the details depends on each APDU command. */
    @Nullable
    public Asn1Node getErrorDetails() {
        return mErrorDetails;
    }

    @Override
    public String getMessage() {
        return "EuiccCardError: mOperatorCode=" + mOperationCode + ", mErrorCode=" + mErrorCode
                + ", errorDetails=" + (mErrorDetails == null ? "null" : mErrorDetails.toHex());
    }
}

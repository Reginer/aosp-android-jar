/*
 * Copyright (C) 2019 The Android Open Source Project
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
package android.net.ipsec.ike.exceptions;

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.annotation.SystemApi;

import com.android.internal.net.ipsec.ike.message.IkeNotifyPayload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * IkeProtocolException is an abstract class that represents the common information for all IKE
 * protocol errors.
 *
 * <p>Error types are as defined by RFC 7296.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.10.1">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public abstract class IkeProtocolException extends IkeException {
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        ERROR_TYPE_UNSUPPORTED_CRITICAL_PAYLOAD,
        ERROR_TYPE_INVALID_IKE_SPI,
        ERROR_TYPE_INVALID_MAJOR_VERSION,
        ERROR_TYPE_INVALID_SYNTAX,
        ERROR_TYPE_INVALID_MESSAGE_ID,
        ERROR_TYPE_NO_PROPOSAL_CHOSEN,
        ERROR_TYPE_INVALID_KE_PAYLOAD,
        ERROR_TYPE_AUTHENTICATION_FAILED,
        ERROR_TYPE_SINGLE_PAIR_REQUIRED,
        ERROR_TYPE_NO_ADDITIONAL_SAS,
        ERROR_TYPE_INTERNAL_ADDRESS_FAILURE,
        ERROR_TYPE_FAILED_CP_REQUIRED,
        ERROR_TYPE_TS_UNACCEPTABLE,
        ERROR_TYPE_INVALID_SELECTORS,
        ERROR_TYPE_TEMPORARY_FAILURE,
        ERROR_TYPE_CHILD_SA_NOT_FOUND,
    })
    public @interface ErrorType {}

    /** Unsupported critical payload */
    public static final int ERROR_TYPE_UNSUPPORTED_CRITICAL_PAYLOAD = 1;
    /** Unrecognized destination IKE SPI */
    public static final int ERROR_TYPE_INVALID_IKE_SPI = 4;
    /** Invalid major version */
    public static final int ERROR_TYPE_INVALID_MAJOR_VERSION = 5;
    /** Invalid syntax */
    public static final int ERROR_TYPE_INVALID_SYNTAX = 7;
    /** Invalid message ID */
    public static final int ERROR_TYPE_INVALID_MESSAGE_ID = 9;
    /** No SA Proposal Chosen is acceptable */
    public static final int ERROR_TYPE_NO_PROPOSAL_CHOSEN = 14;
    /** Invalid Key Exchange Payload */
    public static final int ERROR_TYPE_INVALID_KE_PAYLOAD = 17;
    /** IKE authentication failed */
    public static final int ERROR_TYPE_AUTHENTICATION_FAILED = 24;
    /** Only Traffic Selectors specifying a single pair of addresses are acceptable */
    public static final int ERROR_TYPE_SINGLE_PAIR_REQUIRED = 34;
    /** No additional SAa are acceptable */
    public static final int ERROR_TYPE_NO_ADDITIONAL_SAS = 35;
    /** No internal addresses can be assigned */
    public static final int ERROR_TYPE_INTERNAL_ADDRESS_FAILURE = 36;
    /** Configuration Payload required but not found in IKE setup */
    public static final int ERROR_TYPE_FAILED_CP_REQUIRED = 37;
    /** No Traffic Selectors are acceptable */
    public static final int ERROR_TYPE_TS_UNACCEPTABLE = 38;
    /**
     * An IPsec Packet was found to have mismatched Traffic Selectors of the IPsec SA on which it
     * was delivered
     */
    public static final int ERROR_TYPE_INVALID_SELECTORS = 39;
    /** Temporary failure */
    public static final int ERROR_TYPE_TEMPORARY_FAILURE = 43;
    /** Child SA in the received packet does not exist */
    public static final int ERROR_TYPE_CHILD_SA_NOT_FOUND = 44;

    /** @hide */
    public static final byte[] ERROR_DATA_NOT_INCLUDED = new byte[0];

    private static final int INTEGER_BYTE_SIZE = 4;

    @ErrorType private final int mErrorType;
    private final byte[] mErrorData;

    /** @hide */
    protected IkeProtocolException(@ErrorType int code) {
        super();
        mErrorType = code;
        mErrorData = ERROR_DATA_NOT_INCLUDED;
    }

    /** @hide */
    protected IkeProtocolException(@ErrorType int code, String message) {
        super(message);
        mErrorType = code;
        mErrorData = ERROR_DATA_NOT_INCLUDED;
    }

    /** @hide */
    protected IkeProtocolException(@ErrorType int code, Throwable cause) {
        super(cause);
        mErrorType = code;
        mErrorData = ERROR_DATA_NOT_INCLUDED;
    }

    /** @hide */
    protected IkeProtocolException(@ErrorType int code, String message, Throwable cause) {
        super(message, cause);
        mErrorType = code;
        mErrorData = ERROR_DATA_NOT_INCLUDED;
    }

    /**
     * Construct an instance from a notify Payload.
     *
     * @hide
     */
    protected IkeProtocolException(@ErrorType int code, byte[] notifyData) {
        super();

        if (!isValidDataLength(notifyData.length)) {
            throw new IllegalArgumentException(
                    "Invalid error data for error type: "
                            + code
                            + " Received error data size: "
                            + notifyData.length);
        }

        mErrorType = code;
        mErrorData = notifyData.clone();
    }

    /** @hide */
    protected abstract boolean isValidDataLength(int dataLen);

    /** @hide */
    protected static byte[] integerToByteArray(int integer, int arraySize) {
        if (arraySize > INTEGER_BYTE_SIZE) {
            throw new IllegalArgumentException(
                    "Cannot convert integer to a byte array of length: " + arraySize);
        }

        ByteBuffer dataBuffer = ByteBuffer.allocate(INTEGER_BYTE_SIZE);
        dataBuffer.putInt(integer);
        dataBuffer.rewind();

        byte[] zeroPad = new byte[INTEGER_BYTE_SIZE - arraySize];
        byte[] byteData = new byte[arraySize];
        dataBuffer.get(zeroPad).get(byteData);

        return byteData;
    }

    /** @hide */
    protected static int byteArrayToInteger(byte[] byteArray) {
        if (byteArray == null || byteArray.length > INTEGER_BYTE_SIZE) {
            throw new IllegalArgumentException("Cannot convert the byte array to integer");
        }

        ByteBuffer dataBuffer = ByteBuffer.allocate(INTEGER_BYTE_SIZE);
        byte[] zeroPad = new byte[INTEGER_BYTE_SIZE - byteArray.length];
        dataBuffer.put(zeroPad).put(byteArray);
        dataBuffer.rewind();

        return dataBuffer.getInt();
    }

    /**
     * Returns the IKE protocol error type of this {@link IkeProtocolException} instance.
     *
     * @return the IKE standard protocol error type defined in {@link IkeProtocolException} or the
     *     error code for an unrecognized error type.
     */
    @ErrorType
    public int getErrorType() {
        return mErrorType;
    }

    /**
     * Returns the included error data of this {@link IkeProtocolException} instance.
     *
     * <p>Note that only few error types will go with an error data. This data has different meaning
     * with different error types. Callers should first check if an error data is included before
     * they call this method.
     *
     * @return the included error data in byte array, or {@code null} if no error data is available.
     * @hide
     */
    @SystemApi
    @Nullable
    public byte[] getErrorData() {
        return mErrorData;
    }

    /**
     * Build an IKE Notification Payload for this {@link IkeProtocolException} instance.
     *
     * @return the notification payload.
     * @hide
     */
    public IkeNotifyPayload buildNotifyPayload() {
        return new IkeNotifyPayload(mErrorType, mErrorData);
    }
}

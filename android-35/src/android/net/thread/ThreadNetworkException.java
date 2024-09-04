/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.net.thread;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a Thread network specific failure.
 *
 * @hide
 */
@FlaggedApi(ThreadNetworkFlags.FLAG_THREAD_ENABLED)
@SystemApi
public class ThreadNetworkException extends Exception {
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        ERROR_INTERNAL_ERROR,
        ERROR_ABORTED,
        ERROR_TIMEOUT,
        ERROR_UNAVAILABLE,
        ERROR_BUSY,
        ERROR_FAILED_PRECONDITION,
        ERROR_UNSUPPORTED_CHANNEL,
        ERROR_REJECTED_BY_PEER,
        ERROR_RESPONSE_BAD_FORMAT,
        ERROR_RESOURCE_EXHAUSTED,
        ERROR_UNKNOWN,
        ERROR_THREAD_DISABLED,
    })
    public @interface ErrorCode {}

    /**
     * The operation failed because some invariants expected by the underlying system have been
     * broken. This error code is reserved for serious errors. The caller can do nothing to recover
     * from this error. A bugreport should be created and sent to the Android community if this
     * error is ever returned.
     */
    public static final int ERROR_INTERNAL_ERROR = 1;

    /**
     * The operation failed because concurrent operations are overriding this one. Retrying an
     * aborted operation has the risk of aborting another ongoing operation again. So the caller
     * should retry at a higher level where it knows there won't be race conditions.
     */
    public static final int ERROR_ABORTED = 2;

    /**
     * The operation failed because a deadline expired before the operation could complete. This may
     * be caused by connectivity unavailability and the caller can retry the same operation when the
     * connectivity issue is fixed.
     */
    public static final int ERROR_TIMEOUT = 3;

    /**
     * The operation failed because the service is currently unavailable and that this is most
     * likely a transient condition. The caller can recover from this error by retrying with a
     * back-off scheme. Note that it is not always safe to retry non-idempotent operations.
     */
    public static final int ERROR_UNAVAILABLE = 4;

    /**
     * The operation failed because this device is currently busy processing concurrent requests.
     * The caller may recover from this error when the current operations has been finished.
     */
    public static final int ERROR_BUSY = 5;

    /**
     * The operation failed because required preconditions were not satisfied. For example, trying
     * to schedule a network migration when this device is not attached will receive this error or
     * enable Thread while User Resitration has disabled it. The caller should not retry the same
     * operation before the precondition is satisfied.
     */
    public static final int ERROR_FAILED_PRECONDITION = 6;

    /**
     * The operation was rejected because the specified channel is currently not supported by this
     * device in this country. For example, trying to join or migrate to a network with channel
     * which is not supported. The caller should should change the channel or return an error to the
     * user if the channel cannot be changed.
     */
    public static final int ERROR_UNSUPPORTED_CHANNEL = 7;

    /**
     * The operation failed because a request is rejected by the peer device. This happens because
     * the peer device is not capable of processing the request, or a request from another device
     * has already been accepted by the peer device. The caller may not be able to recover from this
     * error by retrying the same operation.
     */
    public static final int ERROR_REJECTED_BY_PEER = 8;

    /**
     * The operation failed because the received response is malformed. This is typically because
     * the peer device is misbehaving. The caller may only recover from this error by retrying with
     * a different peer device.
     */
    public static final int ERROR_RESPONSE_BAD_FORMAT = 9;

    /**
     * The operation failed because some resource has been exhausted. For example, no enough
     * allocated memory buffers, or maximum number of supported operations has been exceeded. The
     * caller may retry and recover from this error when the resource has been freed.
     */
    public static final int ERROR_RESOURCE_EXHAUSTED = 10;

    /**
     * The operation failed because of an unknown error in the system. This typically indicates that
     * the caller doesn't understand error codes added in newer Android versions.
     */
    public static final int ERROR_UNKNOWN = 11;

    /**
     * The operation failed because the Thread radio is disabled by {@link
     * ThreadNetworkController#setEnabled}, airplane mode or device admin. The caller should retry
     * only after Thread is enabled.
     */
    public static final int ERROR_THREAD_DISABLED = 12;

    /**
     * The operation failed because it is not supported by the platform. For example, some platforms
     * may not support setting the target power of each channel. The caller should not retry and may
     * return an error to the user.
     *
     * @hide
     */
    public static final int ERROR_UNSUPPORTED_OPERATION = 13;

    private static final int ERROR_MIN = ERROR_INTERNAL_ERROR;
    private static final int ERROR_MAX = ERROR_UNSUPPORTED_OPERATION;

    private final int mErrorCode;

    /**
     * Creates a new {@link ThreadNetworkException} object with given error code and message.
     *
     * @throws IllegalArgumentException if {@code errorCode} is not a value in {@link #ERROR_}
     * @throws NullPointerException if {@code message} is {@code null}
     */
    public ThreadNetworkException(@ErrorCode int errorCode, @NonNull String message) {
        super(requireNonNull(message, "message cannot be null"));
        if (errorCode < ERROR_MIN || errorCode > ERROR_MAX) {
            throw new IllegalArgumentException(
                    "errorCode cannot be "
                            + errorCode
                            + " (allowedRange = ["
                            + ERROR_MIN
                            + ", "
                            + ERROR_MAX
                            + "])");
        }
        this.mErrorCode = errorCode;
    }

    /** Returns the error code. */
    public @ErrorCode int getErrorCode() {
        return mErrorCode;
    }
}

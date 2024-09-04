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

package android.health.connect;

import android.annotation.IntDef;
import android.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Class representing health connect exceptions. */
public class HealthConnectException extends RuntimeException {
    /** An unknown error occurred while processing the call. */
    public static final int ERROR_UNKNOWN = 1;
    /**
     * An internal error occurred which the caller cannot address.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}
     */
    public static final int ERROR_INTERNAL = 2;
    /**
     * The caller supplied invalid arguments to the call.
     *
     * <p>This error may be considered similar to {@link IllegalArgumentException}.
     */
    public static final int ERROR_INVALID_ARGUMENT = 3;
    /**
     * An issue occurred reading or writing to storage. The call might succeed if repeated.
     *
     * <p>This error may be considered similar to {@link java.io.IOException}.
     */
    public static final int ERROR_IO = 4;
    /**
     * The caller doesn't have the correct permissions for this call.
     *
     * <p>This error may be considered similar to {@link java.lang.SecurityException}.
     */
    public static final int ERROR_SECURITY = 5;
    /**
     * An IPC related error occurred.
     *
     * <p>This error may be considered similar to {@link android.os.RemoteException}.
     */
    public static final int ERROR_REMOTE = 6;
    /** The caller exhausted the allotted rate limit. */
    public static final int ERROR_RATE_LIMIT_EXCEEDED = 7;
    /**
     * Data sync is in progress. Data read and writes are blocked.
     *
     * <p>Caller should try this api call again later.
     */
    public static final int ERROR_DATA_SYNC_IN_PROGRESS = 8;
    /**
     * This operation is currently not supported by the platform.
     *
     * <p>Caller may try this api call again later.
     */
    public static final int ERROR_UNSUPPORTED_OPERATION = 9;

    @ErrorCode private final int mErrorCode;

    /**
     * Initializes an {@link HealthConnectException} with a result code and message.
     *
     * @param errorCode One of the constants documented in {@link HealthConnectException}.
     * @param message The detailed error message.
     * @hide
     */
    public HealthConnectException(@ErrorCode int errorCode, @Nullable String message) {
        super(message);
        mErrorCode = errorCode;
    }

    /**
     * Initializes an {@link HealthConnectException} with a result code.
     *
     * @param errorCode One of the constants documented in {@link HealthConnectException}.
     * @hide
     */
    public HealthConnectException(@ErrorCode int errorCode) {
        this(errorCode, null);
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    /**
     * Error codes from {@link HealthConnectManager} methods.
     *
     * @hide
     */
    @IntDef(
            value = {
                ERROR_UNKNOWN,
                ERROR_INTERNAL,
                ERROR_INVALID_ARGUMENT,
                ERROR_IO,
                ERROR_SECURITY,
                ERROR_REMOTE,
                ERROR_DATA_SYNC_IN_PROGRESS,
                ERROR_RATE_LIMIT_EXCEEDED,
                ERROR_UNSUPPORTED_OPERATION
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorCode {}
}

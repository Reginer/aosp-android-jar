/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.adservices.common;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.LimitExceededException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeoutException;

/**
 * Utility class containing status codes and functions used by various response objects.
 *
 * <p>Those status codes are internal only.
 *
 * @hide
 */
public class AdServicesStatusUtils {
    /**
     * The status code has not been set. Keep unset status code the lowest value of the status
     * codes.
     */
    public static final int STATUS_UNSET = -1;
    /** The call was successful. */
    public static final int STATUS_SUCCESS = 0;
    /**
     * An internal error occurred within the API, which the caller cannot address.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}.
     */
    public static final int STATUS_INTERNAL_ERROR = 1;
    /**
     * The caller supplied invalid arguments to the call.
     *
     * <p>This error may be considered similar to {@link IllegalArgumentException}.
     */
    public static final int STATUS_INVALID_ARGUMENT = 2;
    /** There was an unknown error. */
    public static final int STATUS_UNKNOWN_ERROR = 3;
    /**
     * There was an I/O error.
     *
     * <p>This error may be considered similar to {@link IOException}.
     */
    public static final int STATUS_IO_ERROR = 4;
    /**
     * Result code for Rate Limit Reached.
     *
     * <p>This error may be considered similar to {@link LimitExceededException}.
     */
    public static final int STATUS_RATE_LIMIT_REACHED = 5;
    /**
     * Killswitch was enabled. AdServices is not available.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}.
     */
    public static final int STATUS_KILLSWITCH_ENABLED = 6;
    /**
     * User consent was revoked. AdServices is not available.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}.
     */
    public static final int STATUS_USER_CONSENT_REVOKED = 7;
    /**
     * AdServices were disabled. AdServices is not available.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}.
     */
    public static final int STATUS_ADSERVICES_DISABLED = 8;
    /**
     * The caller is not authorized to make this call. Permission was not requested.
     *
     * <p>This error may be considered similar to {@link SecurityException}.
     */
    public static final int STATUS_PERMISSION_NOT_REQUESTED = 9;
    /**
     * The caller is not authorized to make this call. Caller is not allowed (not present in the
     * allowed list).
     *
     * <p>This error may be considered similar to {@link SecurityException}.
     */
    public static final int STATUS_CALLER_NOT_ALLOWED = 10;
    /**
     * The caller is not authorized to make this call. Call was executed from background thread.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}.
     */
    public static final int STATUS_BACKGROUND_CALLER = 11;
    /**
     * The caller is not authorized to make this call.
     *
     * <p>This error may be considered similar to {@link SecurityException}.
     */
    public static final int STATUS_UNAUTHORIZED = 12;
    /**
     * There was an internal Timeout within the API, which is non-recoverable by the caller
     *
     * <p>This error may be considered similar to {@link java.util.concurrent.TimeoutException}
     */
    public static final int STATUS_TIMEOUT = 13;
    /**
     * The device is not running a version of WebView that supports JSSandbox, required for FLEDGE
     * Ad Selection.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}.
     */
    public static final int STATUS_JS_SANDBOX_UNAVAILABLE = 14;

    /** The error message to be returned along with {@link IllegalStateException}. */
    public static final String ILLEGAL_STATE_EXCEPTION_ERROR_MESSAGE = "Service is not available.";
    /** The error message to be returned along with {@link LimitExceededException}. */
    public static final String RATE_LIMIT_REACHED_ERROR_MESSAGE = "API rate limit exceeded.";
    /**
     * The error message to be returned along with {@link SecurityException} when permission was not
     * requested in the manifest.
     */
    public static final String SECURITY_EXCEPTION_PERMISSION_NOT_REQUESTED_ERROR_MESSAGE =
            "Caller is not authorized to call this API. Permission was not requested.";
    /**
     * The error message to be returned along with {@link SecurityException} when caller is not
     * allowed to call AdServices (not present in the allowed list).
     */
    public static final String SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ERROR_MESSAGE =
            "Caller is not authorized to call this API. Caller is not allowed.";
    /**
     * The error message to be returned along with {@link SecurityException} when call was executed
     * from the background thread.
     */
    public static final String ILLEGAL_STATE_BACKGROUND_CALLER_ERROR_MESSAGE =
            "Background thread is not allowed to call this service.";

    /**
     * The error message to be returned along with {@link SecurityException} when caller not allowed
     * to perform this operation on behalf of the given package.
     */
    public static final String SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ON_BEHALF_ERROR_MESSAGE =
            "Caller is not allowed to perform this operation on behalf of the given package.";

    /** The error message to be returned along with {@link TimeoutException}. */
    public static final String TIMED_OUT_ERROR_MESSAGE = "API timed out.";

    /** Returns true for a successful status. */
    public static boolean isSuccess(@StatusCode int statusCode) {
        return statusCode == STATUS_SUCCESS;
    }

    /** Converts the input {@code statusCode} to an exception to be used in the callback. */
    @NonNull
    public static Exception asException(@StatusCode int statusCode) {
        switch (statusCode) {
            case STATUS_INVALID_ARGUMENT:
                return new IllegalArgumentException();
            case STATUS_IO_ERROR:
                return new IOException();
            case STATUS_KILLSWITCH_ENABLED: // Intentional fallthrough
            case STATUS_USER_CONSENT_REVOKED: // Intentional fallthrough
            case STATUS_JS_SANDBOX_UNAVAILABLE:
                return new IllegalStateException(ILLEGAL_STATE_EXCEPTION_ERROR_MESSAGE);
            case STATUS_PERMISSION_NOT_REQUESTED:
                return new SecurityException(
                        SECURITY_EXCEPTION_PERMISSION_NOT_REQUESTED_ERROR_MESSAGE);
            case STATUS_CALLER_NOT_ALLOWED:
                return new SecurityException(SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ERROR_MESSAGE);
            case STATUS_BACKGROUND_CALLER:
                return new IllegalStateException(ILLEGAL_STATE_BACKGROUND_CALLER_ERROR_MESSAGE);
            case STATUS_UNAUTHORIZED:
                return new SecurityException(
                        SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ON_BEHALF_ERROR_MESSAGE);
            case STATUS_TIMEOUT:
                return new TimeoutException(TIMED_OUT_ERROR_MESSAGE);
            case STATUS_RATE_LIMIT_REACHED:
                return new LimitExceededException(RATE_LIMIT_REACHED_ERROR_MESSAGE);
            default:
                return new IllegalStateException();
        }
    }

    /** Converts the {@link AdServicesResponse} to an exception to be used in the callback. */
    @NonNull
    public static Exception asException(@NonNull AdServicesResponse adServicesResponse) {
        return asException(adServicesResponse.getStatusCode());
    }

    /**
     * Result codes that are common across various APIs.
     *
     * @hide
     */
    @IntDef(
            prefix = {"STATUS_"},
            value = {
                STATUS_UNSET,
                STATUS_SUCCESS,
                STATUS_INTERNAL_ERROR,
                STATUS_INVALID_ARGUMENT,
                STATUS_RATE_LIMIT_REACHED,
                STATUS_UNKNOWN_ERROR,
                STATUS_IO_ERROR,
                STATUS_KILLSWITCH_ENABLED,
                STATUS_USER_CONSENT_REVOKED,
                STATUS_ADSERVICES_DISABLED,
                STATUS_PERMISSION_NOT_REQUESTED,
                STATUS_CALLER_NOT_ALLOWED,
                STATUS_BACKGROUND_CALLER,
                STATUS_UNAUTHORIZED,
                STATUS_TIMEOUT,
                STATUS_JS_SANDBOX_UNAVAILABLE
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StatusCode {}
}

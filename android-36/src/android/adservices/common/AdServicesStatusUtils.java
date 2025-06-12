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

import com.android.adservices.shared.common.exception.ProviderServiceInternalException;
import com.android.adservices.shared.common.exception.ProviderServiceTaskCancelledException;
import com.android.adservices.shared.common.exception.ServiceUnavailableException;

import java.io.IOException;
import java.io.InvalidObjectException;
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
public final class AdServicesStatusUtils {

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

    /**
     * The service received an invalid object from the remote server.
     *
     * <p>This error may be considered similar to {@link InvalidObjectException}.
     */
    public static final int STATUS_INVALID_OBJECT = 15;

    /**
     * The caller is not authorized to make this call because it crosses user boundaries.
     *
     * <p>This error may be considered similar to {@link SecurityException}.
     */
    public static final int STATUS_CALLER_NOT_ALLOWED_TO_CROSS_USER_BOUNDARIES = 16;

    /**
     * Result code for Server Rate Limit Reached.
     *
     * <p>This error may be considered similar to {@link LimitExceededException}.
     */
    public static final int STATUS_SERVER_RATE_LIMIT_REACHED = 17;

    /**
     * Consent notification has not been displayed yet. AdServices is not available.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}.
     */
    public static final int STATUS_USER_CONSENT_NOTIFICATION_NOT_DISPLAYED_YET = 18;

    /**
     * Result code for Encryption related failures.
     *
     * <p>This error may be considered similar to {@link IllegalArgumentException}.
     */
    public static final int STATUS_ENCRYPTION_FAILURE = 19;

    /**
     * The caller is not authorized to make this call because the package is not in the allowlist.
     *
     * <p>This error may be considered similar to {@link SecurityException}.
     */
    public static final int STATUS_CALLER_NOT_ALLOWED_PACKAGE_NOT_IN_ALLOWLIST = 20;

    /**
     * The caller is not authorized to make this call because the package is not in the allowlist.
     *
     * <p>This error may be considered similar to {@link SecurityException}.
     */
    public static final int STATUS_CALLER_NOT_ALLOWED_PACKAGE_BLOCKLISTED = 21;

    /**
     * The caller is not authorized to make this call because enrollment data can't be found.
     *
     * <p>This error may be considered similar to {@link SecurityException}.
     */
    public static final int STATUS_CALLER_NOT_ALLOWED_ENROLLMENT_MATCH_NOT_FOUND = 22;

    /**
     * The caller is not authorized to make this call because enrollment ID is invalid.
     *
     * <p>This error may be considered similar to {@link SecurityException}.
     */
    public static final int STATUS_CALLER_NOT_ALLOWED_ENROLLMENT_INVALID_ID = 23;

    /**
     * The caller is not authorized to make this call because enrollment ID is in the blocklist.
     *
     * <p>This error may be considered similar to {@link SecurityException}.
     */
    public static final int STATUS_CALLER_NOT_ALLOWED_ENROLLMENT_BLOCKLISTED = 24;

    /**
     * The caller is not authorized to make this call because permission was not requested in the
     * manifest.
     *
     * <p>This error may be considered similar to {@link SecurityException}.
     */
    public static final int STATUS_CALLER_NOT_ALLOWED_MANIFEST_ADSERVICES_CONFIG_NO_PERMISSION = 25;

    /**
     * AdServices activity is disabled.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}.
     */
    public static final int STATUS_ADSERVICES_ACTIVITY_DISABLED = 26;

    /**
     * Callback is shut down and encountered an error when invoking its methods.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}.
     */
    public static final int STATUS_CALLBACK_SHUTDOWN = 27;

    /**
     * The provider service throws an error as the callback when AdServices tries to call it.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}.
     */
    public static final int STATUS_PROVIDER_SERVICE_INTERNAL_ERROR = 28;

    /**
     * The scheduleCustomAudienceUpdate() request failed because an existing update has not been
     * executed yet and should be explicitly replaced by the caller.
     *
     * <p>This error throws an {@link IllegalStateException}.
     */
    public static final int STATUS_UPDATE_ALREADY_PENDING_ERROR = 29;

    /** This error denotes that consent was revoked for all APIS. */
    public static final int STATUS_CONSENT_REVOKED_ALL_APIS = 30;

    /** This error occurs when a dev session is still transitioning between prod or dev. */
    public static final int STATUS_DEV_SESSION_IS_STILL_TRANSITIONING = 31;

    /** This error occurs when a non-debuggable app is calling during a dev session. */
    public static final int STATUS_DEV_SESSION_CALLER_IS_NON_DEBUGGABLE = 32;

    /** This error occurs when dev session state is unable to be read. */
    public static final int STATUS_DEV_SESSION_FAILURE = 33;

    /** This error occurs when the caller is in the deny list. */
    public static final int STATUS_CALLER_NOT_ALLOWED_DENY_LIST = 34;

    /**
     * This error occurs when the package name associated to the calling UID does not match the
     * package name from the request.
     */
    public static final int STATUS_CALLER_NOT_ALLOWED_UID_MISMATCH = 35;

    /**
     * The provider service throws a task cancelled error as the callback when AdServices tries to
     * call it.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}.
     */
    public static final int STATUS_PROVIDER_SERVICE_TASK_CANCELLED_ERROR = 36;

    /** The error message to be returned along with {@link LimitExceededException}. */
    public static final String RATE_LIMIT_REACHED_ERROR_MESSAGE = "API rate limit exceeded.";

    /** The error message to be returned along with {@link LimitExceededException}. */
    public static final String SERVER_RATE_LIMIT_REACHED_ERROR_MESSAGE =
            "Server rate limit exceeded.";

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
     * The error message to be returned along with {@link IllegalStateException} when call failed
     * because AdServices activity is disabled.
     */
    public static final String ILLEGAL_STATE_ACTIVITY_DISABLED_ERROR_MESSAGE =
            "AdServices activity is disabled.";

    /**
     * The error message to be returned along with {@link SecurityException} when call failed
     * because it crosses user boundaries.
     */
    public static final String SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_TO_CROSS_USER_BOUNDARIES =
            "Caller is not authorized to access information from another user";

    /**
     * The error message to be returned along with {@link SecurityException} when caller not allowed
     * to perform this operation on behalf of the given package.
     */
    public static final String SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ON_BEHALF_ERROR_MESSAGE =
            "Caller is not allowed to perform this operation on behalf of the given package.";

    /** The error message to be returned along with {@link TimeoutException}. */
    public static final String TIMED_OUT_ERROR_MESSAGE = "API timed out.";

    /** The error message to be returned along with {@link InvalidObjectException}. */
    public static final String INVALID_OBJECT_ERROR_MESSAGE =
            "The service received an invalid object from the server.";

    /** The error message to be returned along with {@link IllegalArgumentException}. */
    public static final String ENCRYPTION_FAILURE_MESSAGE = "Failed to encrypt responses.";

    /** The error message to be returned along with {@link ServiceUnavailableException}. */
    public static final String SERVICE_UNAVAILABLE_ERROR_MESSAGE = "Service is not available.";

    private static final String DEV_SESSION_ERROR_TRANSITIONING_HELP =
            "If this error persists, run `cmd adservices_manager adservices-api dev-session end` "
                    + "to reset the dev session.";

    /** The error message when a dev session is still transitioning between prod or dev. */
    public static final String DEV_SESSION_IS_TRANSITIONING_MESSAGE =
            "Caller is not allowed during the transition to or from dev mode. "
                    + DEV_SESSION_ERROR_TRANSITIONING_HELP;

    /** The error message when a non-debuggable app is calling during a dev session. */
    public static final String DEV_SESSION_CALLER_IS_NON_DEBUGGABLE_MESSAGE =
            "Caller during a dev session must have android:debuggable=\"true\" in their manifest! "
                    + DEV_SESSION_ERROR_TRANSITIONING_HELP;

    /** The error message when dev session state cannot be read. */
    public static final String DEV_SESSION_FAILURE_MESSAGE =
            "Failed to read dev session state. " + DEV_SESSION_ERROR_TRANSITIONING_HELP;

    /**
     * The error message returned when a call to schedule a custom audience update fails because of
     * an existing pending update.
     */
    public static final String UPDATE_ALREADY_PENDING_ERROR_MESSAGE =
            "Failed to schedule update. A request is already pending.";

    /**
     * The error message to be returned along with {@link SecurityException} when caller is not
     * allowed to call AdServices API (present in the deny list).
     */
    public static final String CALLER_NOT_ALLOWED_DENY_LIST_ERROR_MESSAGE =
            "Caller is not authorized to call this API as caller is in deny list.";

    /** Returns true for a successful status. */
    public static boolean isSuccess(@StatusCode int statusCode) {
        return statusCode == STATUS_SUCCESS;
    }

    /** Converts the input {@code statusCode} to an exception to be used in the callback. */
    @NonNull
    public static Exception asException(@StatusCode int statusCode) {
        switch (statusCode) {
            case STATUS_ENCRYPTION_FAILURE:
                return new IllegalArgumentException(ENCRYPTION_FAILURE_MESSAGE);
            case STATUS_INVALID_ARGUMENT:
                return new IllegalArgumentException();
            case STATUS_IO_ERROR:
                return new IOException();
            case STATUS_KILLSWITCH_ENABLED: // Intentional fallthrough
            case STATUS_USER_CONSENT_NOTIFICATION_NOT_DISPLAYED_YET: // Intentional fallthrough
            case STATUS_USER_CONSENT_REVOKED: // Intentional fallthrough
            case STATUS_CONSENT_REVOKED_ALL_APIS: // Intentional fallthrough
            case STATUS_JS_SANDBOX_UNAVAILABLE:
                return new ServiceUnavailableException(SERVICE_UNAVAILABLE_ERROR_MESSAGE);
            case STATUS_PERMISSION_NOT_REQUESTED:
                return new SecurityException(
                        SECURITY_EXCEPTION_PERMISSION_NOT_REQUESTED_ERROR_MESSAGE);
            case STATUS_CALLER_NOT_ALLOWED:
                return new SecurityException(SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ERROR_MESSAGE);
            case STATUS_CALLER_NOT_ALLOWED_PACKAGE_NOT_IN_ALLOWLIST:
                return new SecurityException(SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ERROR_MESSAGE);
            case STATUS_CALLER_NOT_ALLOWED_PACKAGE_BLOCKLISTED:
                return new SecurityException(SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ERROR_MESSAGE);
            case STATUS_CALLER_NOT_ALLOWED_ENROLLMENT_MATCH_NOT_FOUND:
                return new SecurityException(SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ERROR_MESSAGE);
            case STATUS_CALLER_NOT_ALLOWED_ENROLLMENT_INVALID_ID:
                return new SecurityException(SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ERROR_MESSAGE);
            case STATUS_CALLER_NOT_ALLOWED_ENROLLMENT_BLOCKLISTED:
                return new SecurityException(SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ERROR_MESSAGE);
            case STATUS_CALLER_NOT_ALLOWED_MANIFEST_ADSERVICES_CONFIG_NO_PERMISSION:
                return new SecurityException(SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ERROR_MESSAGE);
            case STATUS_BACKGROUND_CALLER:
                return new IllegalStateException(ILLEGAL_STATE_BACKGROUND_CALLER_ERROR_MESSAGE);
            case STATUS_ADSERVICES_ACTIVITY_DISABLED:
                return new IllegalStateException(ILLEGAL_STATE_ACTIVITY_DISABLED_ERROR_MESSAGE);
            case STATUS_UPDATE_ALREADY_PENDING_ERROR:
                return new IllegalStateException(UPDATE_ALREADY_PENDING_ERROR_MESSAGE);
            case STATUS_UNAUTHORIZED:
                return new SecurityException(
                        SECURITY_EXCEPTION_CALLER_NOT_ALLOWED_ON_BEHALF_ERROR_MESSAGE);
            case STATUS_TIMEOUT:
                return new TimeoutException(TIMED_OUT_ERROR_MESSAGE);
            case STATUS_RATE_LIMIT_REACHED:
                return new LimitExceededException(RATE_LIMIT_REACHED_ERROR_MESSAGE);
            case STATUS_INVALID_OBJECT:
                return new InvalidObjectException(INVALID_OBJECT_ERROR_MESSAGE);
            case STATUS_SERVER_RATE_LIMIT_REACHED:
                return new LimitExceededException(SERVER_RATE_LIMIT_REACHED_ERROR_MESSAGE);
            case STATUS_PROVIDER_SERVICE_INTERNAL_ERROR:
                return new ProviderServiceInternalException();
            case STATUS_PROVIDER_SERVICE_TASK_CANCELLED_ERROR:
                return new ProviderServiceTaskCancelledException();
            case STATUS_DEV_SESSION_IS_STILL_TRANSITIONING:
                return new IllegalStateException(DEV_SESSION_IS_TRANSITIONING_MESSAGE);
            case STATUS_DEV_SESSION_CALLER_IS_NON_DEBUGGABLE:
                return new SecurityException(DEV_SESSION_CALLER_IS_NON_DEBUGGABLE_MESSAGE);
            case STATUS_DEV_SESSION_FAILURE:
                return new IllegalStateException(DEV_SESSION_FAILURE_MESSAGE);
            case STATUS_CALLER_NOT_ALLOWED_DENY_LIST:
                return new SecurityException(CALLER_NOT_ALLOWED_DENY_LIST_ERROR_MESSAGE);
            default:
                return new IllegalStateException();
        }
    }

    /** Converts the {@link AdServicesResponse} to an exception to be used in the callback. */
    // TODO(b/328601595): Add unit test for AdServicesStatusUtils.asException
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
                STATUS_ADSERVICES_ACTIVITY_DISABLED,
                STATUS_PERMISSION_NOT_REQUESTED,
                STATUS_CALLER_NOT_ALLOWED,
                STATUS_BACKGROUND_CALLER,
                STATUS_UNAUTHORIZED,
                STATUS_TIMEOUT,
                STATUS_JS_SANDBOX_UNAVAILABLE,
                STATUS_INVALID_OBJECT,
                STATUS_SERVER_RATE_LIMIT_REACHED,
                STATUS_USER_CONSENT_NOTIFICATION_NOT_DISPLAYED_YET,
                STATUS_ENCRYPTION_FAILURE,
                STATUS_CALLER_NOT_ALLOWED_PACKAGE_NOT_IN_ALLOWLIST,
                STATUS_CALLER_NOT_ALLOWED_PACKAGE_BLOCKLISTED,
                STATUS_CALLER_NOT_ALLOWED_ENROLLMENT_MATCH_NOT_FOUND,
                STATUS_CALLER_NOT_ALLOWED_ENROLLMENT_INVALID_ID,
                STATUS_CALLER_NOT_ALLOWED_ENROLLMENT_BLOCKLISTED,
                STATUS_CALLER_NOT_ALLOWED_MANIFEST_ADSERVICES_CONFIG_NO_PERMISSION,
                STATUS_CALLBACK_SHUTDOWN,
                STATUS_PROVIDER_SERVICE_INTERNAL_ERROR,
                STATUS_PROVIDER_SERVICE_TASK_CANCELLED_ERROR,
                STATUS_UPDATE_ALREADY_PENDING_ERROR,
                STATUS_CONSENT_REVOKED_ALL_APIS,
                STATUS_DEV_SESSION_IS_STILL_TRANSITIONING,
                STATUS_DEV_SESSION_CALLER_IS_NON_DEBUGGABLE,
                STATUS_DEV_SESSION_FAILURE,
                STATUS_CALLER_NOT_ALLOWED_DENY_LIST,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StatusCode {}

    private AdServicesStatusUtils() {
        throw new UnsupportedOperationException();
    }
}

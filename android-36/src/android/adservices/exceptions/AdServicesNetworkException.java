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

package android.adservices.exceptions;

import static java.util.Locale.ENGLISH;

import android.annotation.IntDef;
import android.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Exception thrown by the service when a failed HTTP request is the cause of a failed API call.
 *
 * @hide
 */
public class AdServicesNetworkException extends AdServicesException {
    /**
     * Error code indicating that the service received an <a
     * href="https://httpwg.org/specs/rfc9110.html#status.3xx">HTTP 3xx</a> status code.
     */
    public static final int ERROR_REDIRECTION = 3;

    /**
     * Error code indicating that the service received an <a
     * href="https://httpwg.org/specs/rfc9110.html#status.4xx">HTTP 4xx</a> status code.
     */
    public static final int ERROR_CLIENT = 4;

    /**
     * Error code indicating that the user has sent too many requests in a given amount of time and
     * the service received an <a href="https://httpwg.org/specs/rfc6585.html#status-429">HTTP
     * 429</a> status code.
     */
    public static final int ERROR_TOO_MANY_REQUESTS = 429;

    /**
     * Error code indicating that the service received an <a
     * href="https://httpwg.org/specs/rfc9110.html#status.4xx">HTTP 5xx</a> status code.
     */
    public static final int ERROR_SERVER = 5;

    /** Error code indicating another type of error was encountered. */
    public static final int ERROR_OTHER = 999;

    /** Error codes indicating what caused the HTTP request to fail. */
    @IntDef(
            prefix = {"ERROR_"},
            value = {
                ERROR_REDIRECTION,
                ERROR_CLIENT,
                ERROR_TOO_MANY_REQUESTS,
                ERROR_SERVER,
                ERROR_OTHER
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorCode {}

    /** @hide */
    public static final String INVALID_ERROR_CODE_MESSAGE = "Valid error code must be set.";

    @ErrorCode private final int mErrorCode;

    /**
     * Constructs an {@link AdServicesNetworkException} that is caused by a failed HTTP request.
     *
     * @param errorCode relevant {@link ErrorCode} corresponding to the failure.
     */
    public AdServicesNetworkException(@ErrorCode int errorCode) {
        super();

        checkErrorCode(errorCode);
        mErrorCode = errorCode;
    }

    /**
     * @return the {@link ErrorCode} indicating what caused the HTTP request to fail.
     */
    @NonNull
    @ErrorCode
    public int getErrorCode() {
        return mErrorCode;
    }

    /**
     * @return a human-readable representation of {@link AdServicesNetworkException}.
     */
    @Override
    public String toString() {
        return String.format(
                ENGLISH,
                "%s: {Error code: %s}",
                this.getClass().getCanonicalName(),
                this.getErrorCode());
    }

    private void checkErrorCode(@ErrorCode int errorCode) {
        switch (errorCode) {
            case ERROR_REDIRECTION:
                // Intentional fallthrough
            case ERROR_CLIENT:
                // Intentional fallthrough
            case ERROR_TOO_MANY_REQUESTS:
                // Intentional fallthrough
            case ERROR_SERVER:
                // Intentional fallthrough
            case ERROR_OTHER:
                break;
            default:
                throw new IllegalArgumentException(INVALID_ERROR_CODE_MESSAGE);
        }
    }
}

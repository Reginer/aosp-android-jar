/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.libraries.entitlement;

/**
 * Indicates errors happened in retrieving service entitlement configuration.
 */
public class ServiceEntitlementException extends Exception {
    /**
     * Unknown error.
     */
    public static final int ERROR_UNKNOWN = 0;

    // Android telephony related failures
    /**
     * Android telephony is unable to provide info like IMSI, e.g. when modem crashed.
     */
    public static final int ERROR_PHONE_NOT_AVAILABLE = 10;

    // EAP-AKA authentication related falures
    /**
     * SIM not returning a response to the EAP-AKA challenge, e.g. when the challenge is invalid.
     * This can happen only when an embedded EAP-AKA challange is conducted, as per GMSA spec TS.43
     * section 2.6.1.
     */
    public static final int ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE = 20;
    /**
     * EAP-AKA synchronization failure that cannot be recoverd even after the "Sequence number
     * synchronization" procedure as defined in RFC 4187.
     */
    public static final int ERROR_EAP_AKA_SYNCHRONIZATION_FAILURE = 21;

    // HTTP related failures
    /**
     * Cannot connect to the entitlment server, e.g. due to weak mobile network and Wi-Fi
     * connection.
     */
    public static final int ERROR_SERVER_NOT_CONNECTABLE = 30;
    /**
     * HTTP response received with a status code indicating failure, e.g. 4xx and 5xx. Use {@link
     * #getHttpStatus} to get the status code and {@link #getMessage} the error message in the
     * response body.
     */
    public static final int ERROR_HTTP_STATUS_NOT_SUCCESS = 31;
    /**
     * HTTP response received with a malformed format. e.g. the response with content-type JSON but
     * failing JSON parser.
     */
    public static final int ERROR_MALFORMED_HTTP_RESPONSE = 32;

    /**
     * Default HTTP status if not been specified.
     */
    private static final int HTTP_STATUS_UNSPECIFIED = 0;

    /**
     * An empty string if Retry-After header in HTTP response not been specified.
     */
    private static final String RETRY_AFTER_UNSPECIFIED = "";

    private int mError;
    private int mHttpStatus;
    private String mRetryAfter;

    public ServiceEntitlementException(int error, String message) {
        this(error, HTTP_STATUS_UNSPECIFIED, RETRY_AFTER_UNSPECIFIED, message);
    }

    public ServiceEntitlementException(int error, int httpStatus, String message) {
        this(error, httpStatus, RETRY_AFTER_UNSPECIFIED, message);
    }

    public ServiceEntitlementException(
            int error, int httpStatus, String retryAfter, String message) {
        super(message);
        this.mError = error;
        this.mHttpStatus = httpStatus;
        this.mRetryAfter = retryAfter;
    }

    public ServiceEntitlementException(int error, String message, Throwable cause) {
        this(error, HTTP_STATUS_UNSPECIFIED, RETRY_AFTER_UNSPECIFIED, message, cause);
    }

    public ServiceEntitlementException(int error, int httpStatus, String message, Throwable cause) {
        this(error, httpStatus, RETRY_AFTER_UNSPECIFIED, message, cause);
    }

    public ServiceEntitlementException(
            int error, int httpStatus, String retryAfter, String message, Throwable cause) {
        super(message, cause);
        this.mError = error;
        this.mHttpStatus = httpStatus;
        this.mRetryAfter = retryAfter;
    }

    /**
     * Returns the error code, see {@link #ERROR_*}. {@link #ERROR_UNKNOWN} if not been specified.
     */
    public int getErrorCode() {
        return mError;
    }

    /**
     * Returns the HTTP status code returned by entitlement server; {@link #HTTP_STATUS_UNSPECIFIED}
     * if not been specified.
     */
    public int getHttpStatus() {
        return mHttpStatus;
    }

    /**
     * Returns the "Retry-After" header in HTTP response, often set with HTTP status code 503; an
     * empty string if unavailable.
     *
     * @return the HTTP-date or a number of seconds to delay, as defiend in RFC 7231:
     * https://tools.ietf.org/html/rfc7231#section-7.1.3
     */
    public String getRetryAfter() {
        return mRetryAfter;
    }
}

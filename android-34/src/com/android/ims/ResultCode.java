/*
 * Copyright (c) 2019, The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the name of The Android Open Source Project nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MOTOROLA MOBILITY LLC BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package com.android.ims;

public class ResultCode {
    /**
     * The code is used when the request is success.
     */
    public static final int SUCCESS =0;

    /**
     * Return this code if the service doesn't be enabled on the phone.
     * As per the requirement the feature can be enabled/disabled by DM.
     */
    public static final int ERROR_SERVICE_NOT_ENABLED = -1;

    /**
     * Return this code if the service didn't publish yet.
     */
    public static final int ERROR_SERVICE_NOT_PUBLISHED = -2;

    /**
     * The service is not available, for example it is 1x only
     */
    public static final int ERROR_SERVICE_NOT_AVAILABLE = -3;

    /**
     *  SUBSCRIBE Error base
     */
    public static final int SUBSCRIBER_ERROR_CODE_START = ERROR_SERVICE_NOT_AVAILABLE;

    /**
     * Temporary error and need retry later.
     * such as:
     * 503 Service Unavailable
     * Device shall retry with exponential back-off
     *
     * 408 Request Timeout
     * Device shall retry with exponential back-off
     *
     * 423 Interval Too Short. Requested expiry interval too short and server rejects it
     * Device shall re-attempt subscription after changing the expiration interval in
     * the Expires header field to be equal to or greater than the expiration interval
     * within the Min-Expires header field of the 423 response
     */
    public static final int SUBSCRIBE_TEMPORARY_ERROR = SUBSCRIBER_ERROR_CODE_START - 1;

    /**
     * receives 403 (reason="User Not Registered").
     * Re-Register to IMS then retry the single resource subscription if capability polling.
     * availability fetch: no retry.
     */
     public static final int SUBSCRIBE_NOT_REGISTERED = SUBSCRIBER_ERROR_CODE_START - 2;

    /**
     * Responding for 403 - not authorized (Requestor)
     * No retry.
     */
    public static final int SUBSCRIBE_NOT_AUTHORIZED_FOR_PRESENCE =
            SUBSCRIBER_ERROR_CODE_START - 3;

    /**
     * Responding for "403 Forbidden" or "403"
     * Handle it as same as 404 Not found.
     * No retry.
     */
    public static final int SUBSCRIBE_FORBIDDEN = SUBSCRIBER_ERROR_CODE_START - 4;

    /**
     * Responding for 404 (target number)
     * No retry.
     */
    public static final int SUBSCRIBE_NOT_FOUND = SUBSCRIBER_ERROR_CODE_START - 5;

    /**
     *  Responding for 413 - Too Large. Top app need shrink the size
     *  of request contact list and resend the request
     */
    public static final int SUBSCRIBE_TOO_LARGE = SUBSCRIBER_ERROR_CODE_START - 6;

    /**
     * All subscribe errors not covered by specific errors
     * Other 4xx/5xx/6xx
     *
     * Device shall not retry
     */
    public static final int SUBSCRIBE_GENIRIC_FAILURE = SUBSCRIBER_ERROR_CODE_START - 7;

    /**
     * Invalid parameter - The caller should check the parameter.
     */
    public static final int SUBSCRIBE_INVALID_PARAM = SUBSCRIBER_ERROR_CODE_START - 8;

    /**
     * Fetch error - The RCS statck failed to fetch the presence information.
     */
    public static final int SUBSCRIBE_FETCH_ERROR = SUBSCRIBER_ERROR_CODE_START - 9;

    /**
     * Request timeout - The RCS statck returns timeout error.
     */
    public static final int SUBSCRIBE_REQUEST_TIMEOUT = SUBSCRIBER_ERROR_CODE_START - 10;

    /**
     * Insufficient memory - The RCS statck returns the insufficient memory error.
     */
    public static final int SUBSCRIBE_INSUFFICIENT_MEMORY = SUBSCRIBER_ERROR_CODE_START - 11;

    /**
     * Lost network error - The RCS statck returns the lost network error.
     */
    public static final int SUBSCRIBE_LOST_NETWORK = SUBSCRIBER_ERROR_CODE_START - 12;

    /**
     * Not supported error - The RCS statck returns the not supported error.
     */
    public static final int SUBSCRIBE_NOT_SUPPORTED = SUBSCRIBER_ERROR_CODE_START - 13;

    /**
     * Generic error - RCS Presence stack returns generic error
     */
    public static final int SUBSCRIBE_GENERIC = SUBSCRIBER_ERROR_CODE_START - 14;

    /**
     * There is a request for the same number in queue.
     */
    public static final int SUBSCRIBE_ALREADY_IN_QUEUE = SUBSCRIBER_ERROR_CODE_START - 16;

    /**
     * Request too frequently.
     */
    public static final int SUBSCRIBE_TOO_FREQUENTLY = SUBSCRIBER_ERROR_CODE_START - 17;

    /**
     *  The last Subscriber error code
     */
    public static final int SUBSCRIBER_ERROR_CODE_END = SUBSCRIBER_ERROR_CODE_START - 17;

    /**
     * All publish errors not covered by specific errors
     */
    public static final int PUBLISH_GENERIC_FAILURE =  ResultCode.SUBSCRIBER_ERROR_CODE_END - 1;

    /**
     * Responding for 403 - not authorized
     */
    public static final int PUBLISH_NOT_AUTHORIZED_FOR_PRESENCE
            = ResultCode.SUBSCRIBER_ERROR_CODE_END - 2;

    /**
     * Responding for 404 error code. The subscriber is not provisioned.
     * The Client should not send any EAB traffic after get this error.
     */
    public static final int PUBLISH_NOT_PROVISIONED = ResultCode.SUBSCRIBER_ERROR_CODE_END - 3;

    public static final int PUBLISH_NOT_REGISTERED = ResultCode.SUBSCRIBER_ERROR_CODE_END - 4;

    public static final int PUBLISH_FORBIDDEN = ResultCode.SUBSCRIBER_ERROR_CODE_END - 5;

    public static final int PUBLISH_NOT_FOUND = ResultCode.SUBSCRIBER_ERROR_CODE_END - 6;

    public static final int PUBLISH_REQUEST_TIMEOUT = ResultCode.SUBSCRIBER_ERROR_CODE_END - 7;

    public static final int PUBLISH_TOO_LARGE = ResultCode.SUBSCRIBER_ERROR_CODE_END - 8;

    public static final int PUBLISH_TOO_SHORT = ResultCode.SUBSCRIBER_ERROR_CODE_END - 9;

    public static final int PUBLISH_TEMPORARY_ERROR = ResultCode.SUBSCRIBER_ERROR_CODE_END - 10;
}

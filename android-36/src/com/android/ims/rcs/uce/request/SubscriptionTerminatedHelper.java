/*
 * Copyright (c) 2021 The Android Open Source Project
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

package com.android.ims.rcs.uce.request;

import android.telephony.ims.RcsUceAdapter;
import android.telephony.ims.RcsUceAdapter.ErrorCode;
import android.text.TextUtils;
import android.util.Log;

import com.android.ims.rcs.uce.util.UceUtils;

import java.util.Optional;

/**
 * The helper class to analyze the result of the callback onTerminated to determine whether the
 * subscription request should be retried or not.
 */
public class SubscriptionTerminatedHelper {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "SubscriptionTerminated";

    // The terminated reasons defined in RFC 3265 3.2.4
    private static final String REASON_DEACTIVATED = "deactivated";
    private static final String REASON_PROBATION = "probation";
    private static final String REASON_REJECTED = "rejected";
    private static final String REASON_TIMEOUT = "timeout";
    private static final String REASON_GIVEUP = "giveup";
    private static final String REASON_NORESOURCE = "noresource";

    /**
     * The analysis result of the callback onTerminated.
     */
    static class TerminatedResult {
        private final @ErrorCode Optional<Integer> mErrorCode;
        private final long mRetryAfterMillis;

        public TerminatedResult(@ErrorCode Optional<Integer> errorCode, long retryAfterMillis) {
            mErrorCode = errorCode;
            mRetryAfterMillis = retryAfterMillis;
        }

        /**
         * @return the error code when the request is failed. Optional.empty if the request is
         * successful.
         */
        public Optional<Integer> getErrorCode() {
            return mErrorCode;
        }

        public long getRetryAfterMillis() {
            return mRetryAfterMillis;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TerminatedResult: ")
                    .append("errorCode=").append(mErrorCode)
                    .append(", retryAfterMillis=").append(mRetryAfterMillis);
            return builder.toString();
        }
    }

    /**
     * According to the RFC 3265, Check the given reason to see whether clients should retry the
     * subscribe request.
     * <p>
     * See RFC 3265 3.2.4 for the detail.
     *
     * @param reason The reason why the subscribe request is terminated. The reason is given by the
     * network and it could be empty.
     * @param retryAfterMillis How long should clients wait before retrying.
     * @param allCapsHaveReceived Whether all the request contact capabilities have been received.
     */
    public static TerminatedResult getAnalysisResult(String reason, long retryAfterMillis,
            boolean allCapsHaveReceived) {
        TerminatedResult result = null;
        if (TextUtils.isEmpty(reason)) {
            /*
             * When the value of retryAfterMillis is larger then zero, the client should retry.
             */
            if (retryAfterMillis > 0L) {
                result = new TerminatedResult(Optional.of(RcsUceAdapter.ERROR_GENERIC_FAILURE),
                        retryAfterMillis);
            }
        } else if (REASON_DEACTIVATED.equalsIgnoreCase(reason)) {
            /*
             * When the reason is "deactivated", clients should retry immediately.
             */
            long retry = getRequestRetryAfterMillis(retryAfterMillis);
            result = new TerminatedResult(Optional.of(RcsUceAdapter.ERROR_GENERIC_FAILURE), retry);
        } else if (REASON_PROBATION.equalsIgnoreCase(reason)) {
            /*
             * When the reason is "probation", it means that the subscription has been terminated,
             * but the client should retry at some later time.
             */
            long retry = getRequestRetryAfterMillis(retryAfterMillis);
            result = new TerminatedResult(Optional.of(RcsUceAdapter.ERROR_GENERIC_FAILURE), retry);
        } else if (REASON_REJECTED.equalsIgnoreCase(reason)) {
            /*
             * When the reason is "rejected", it means that the subscription has been terminated
             * due to chang in authorization policy. Clients should NOT retry.
             */
            result = new TerminatedResult(Optional.of(RcsUceAdapter.ERROR_NOT_AUTHORIZED), 0L);
        } else if (REASON_TIMEOUT.equalsIgnoreCase(reason)) {
            if (retryAfterMillis > 0L) {
                /*
                 * When the parameter "retryAfterMillis" is greater than zero, it means that the
                 * ImsService requires clients should retry later.
                 */
                long retry = getRequestRetryAfterMillis(retryAfterMillis);
                result = new TerminatedResult(Optional.of(RcsUceAdapter.ERROR_REQUEST_TIMEOUT),
                        retry);
            } else if (!allCapsHaveReceived) {
                /*
                 * The ImsService does not require to retry when the parameter "retryAfterMillis"
                 * is zero. However, the request is still failed because it has not received all
                 * the capabilities updated from the network.
                 */
                result = new TerminatedResult(Optional.of(RcsUceAdapter.ERROR_REQUEST_TIMEOUT), 0L);
            } else {
                /*
                 * The subscribe request is successfully when the parameter retryAfter is zero and
                 * all the request capabilities have been received.
                 */
                result = new TerminatedResult(Optional.empty(), 0L);
            }
        } else if (REASON_GIVEUP.equalsIgnoreCase(reason)) {
            /*
             * The subscription has been terminated because the notifier could no obtain
             * authorization in a timely fashion. Clients could retry the subscribe request.
             */
            long retry = getRequestRetryAfterMillis(retryAfterMillis);
            result = new TerminatedResult(Optional.of(RcsUceAdapter.ERROR_NOT_AUTHORIZED), retry);
        } else if (REASON_NORESOURCE.equalsIgnoreCase(reason)) {
            /*
             * The subscription has been terminated because the resource is no longer exists.
             * Clients should NOT retry.
             */
            result = new TerminatedResult(Optional.of(RcsUceAdapter.ERROR_NOT_FOUND), 0L);
        } else if (retryAfterMillis > 0L) {
            /*
             * Even if the reason is not listed above, clients should retry the request as long as
             * the value of retry is non-zero.
             */
            long retry = getRequestRetryAfterMillis(retryAfterMillis);
            result = new TerminatedResult(Optional.of(RcsUceAdapter.ERROR_GENERIC_FAILURE), retry);
        }

        // The request should be successful. when the terminated is not in the above cases
        if (result == null) {
            result = new TerminatedResult(Optional.empty(), 0L);
        }

        Log.d(LOG_TAG, "getAnalysisResult: reason=" + reason + ", retry=" + retryAfterMillis +
                ", allCapsHaveReceived=" + allCapsHaveReceived + ", " + result);
        return result;
    }

    /*
     * Get the appropriated retryAfterMillis for the subscribe request.
     */
    private static long getRequestRetryAfterMillis(long retryAfterMillis) {
        // Return the minimum retry after millis if the given retryAfterMillis is less than the
        // minimum value.
        long minRetryAfterMillis = UceUtils.getMinimumRequestRetryAfterMillis();
        return (retryAfterMillis < minRetryAfterMillis) ? minRetryAfterMillis : retryAfterMillis;
    }
}

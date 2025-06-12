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

import android.annotation.NonNull;

import com.android.internal.util.Preconditions;

import java.time.Duration;
import java.util.Objects;

/**
 * Exception thrown by the service when the HTTP failure response that caused the API to fail
 * contains a <a href="https://httpwg.org/specs/rfc6585.html#status-429">Retry-After header</a>.
 *
 * @hide
 */
public class RetryableAdServicesNetworkException extends AdServicesNetworkException {
    /** @hide */
    public static final Duration UNSET_RETRY_AFTER_VALUE = Duration.ZERO;

    /** @hide */
    public static final Duration DEFAULT_RETRY_AFTER_VALUE = Duration.ofMillis(30 * 1000);

    /** @hide */
    public static final String INVALID_RETRY_AFTER_MESSAGE =
            "Retry-after time duration must be strictly greater than zero.";

    // TODO: (b/298100114) make this final again
    private Duration mRetryAfter;

    /**
     * Constructs an {@link RetryableAdServicesNetworkException} that is caused by a failed HTTP
     * request.
     *
     * @param errorCode relevant {@link ErrorCode} corresponding to the failure.
     * @param retryAfter time {@link Duration} to back-off until next retry.
     */
    public RetryableAdServicesNetworkException(
            @ErrorCode int errorCode, @NonNull Duration retryAfter) {
        super(errorCode);

        Objects.requireNonNull(retryAfter);
        Preconditions.checkArgument(
                retryAfter.compareTo(UNSET_RETRY_AFTER_VALUE) > 0, INVALID_RETRY_AFTER_MESSAGE);

        mRetryAfter = retryAfter;
    }

    /**
     * If {@link #mRetryAfter} < {@code defaultDuration}, it gets set to {@code defaultDuration}. If
     * {@link #mRetryAfter} > {@code maxDuration}, it gets set to {@code maxDuration}. If it falls
     * in the range of both numbers, it stays the same.
     *
     * @hide
     */
    public void setRetryAfterToValidDuration(long defaultDuration, long maxDuration) {
        // TODO: (b/298100114) this is a hack! this method should be removed after resolving the bug
        mRetryAfter =
                Duration.ofMillis(
                        Math.min(Math.max(mRetryAfter.toMillis(), defaultDuration), maxDuration));
    }

    /**
     * @return the positive retry-after {@link Duration} if set or else {@link Duration#ZERO} by
     *     default.
     */
    @NonNull
    public Duration getRetryAfter() {
        return mRetryAfter;
    }

    /**
     * @return a human-readable representation of {@link RetryableAdServicesNetworkException}.
     */
    @Override
    public String toString() {
        return String.format(
                ENGLISH,
                "%s: {Error code: %s, Retry after: %sms}",
                this.getClass().getCanonicalName(),
                this.getErrorCode(),
                this.getRetryAfter().toMillis());
    }
}

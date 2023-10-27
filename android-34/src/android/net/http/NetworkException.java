// Copyright 2016 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

import androidx.annotation.Nullable;

/**
 * Exception passed to {@link UrlRequest.Callback#onFailed UrlRequest.Callback.onFailed()} when
 * the HTTP stack fails to process a network request. In this case {@link #getErrorCode} can be used
 * to get more information about the specific type of failure.
 */
public abstract class NetworkException extends HttpException {
    /**
     * Error code indicating the host being sent the request could not be resolved to an IP address.
     */
    public static final int ERROR_HOSTNAME_NOT_RESOLVED = 1;
    /**
     * Error code indicating the device was not connected to any network.
     */
    public static final int ERROR_INTERNET_DISCONNECTED = 2;
    /**
     * Error code indicating that as the request was processed the network configuration changed.
     * When
     * {@link #getErrorCode} returns this code, this exception may be cast to {@link QuicException}
     * for more information if <a href="https://www.chromium.org/quic"> QUIC</a> protocol is used.
     */
    public static final int ERROR_NETWORK_CHANGED = 3;
    /**
     * Error code indicating a timeout expired. Timeouts expiring while attempting to connect will
     * be reported as the more specific {@link #ERROR_CONNECTION_TIMED_OUT}.
     */
    public static final int ERROR_TIMED_OUT = 4;
    /**
     * Error code indicating the connection was closed unexpectedly.
     */
    public static final int ERROR_CONNECTION_CLOSED = 5;
    /**
     * Error code indicating the connection attempt timed out.
     */
    public static final int ERROR_CONNECTION_TIMED_OUT = 6;
    /**
     * Error code indicating the connection attempt was refused.
     */
    public static final int ERROR_CONNECTION_REFUSED = 7;
    /**
     * Error code indicating the connection was unexpectedly reset.
     */
    public static final int ERROR_CONNECTION_RESET = 8;
    /**
     * Error code indicating the IP address being contacted is unreachable, meaning there is no
     * route to the specified host or network.
     */
    public static final int ERROR_ADDRESS_UNREACHABLE = 9;
    /**
     * Error code indicating an error related to the <a href="https://www.chromium.org/quic">
     * QUIC</a> protocol. When {@link #getErrorCode} returns this code, this exception can be cast
     * to {@link QuicException} for more information.
     */
    public static final int ERROR_QUIC_PROTOCOL_FAILED = 10;
    /**
     * Error code indicating another type of error was encountered.
     */
    public static final int ERROR_OTHER = 11;

    /**
     * Constructs an exception that is caused by a network error.
     *
     * @param message explanation of failure.
     * @param cause the cause (which is saved for later retrieval by the {@link
     * java.io.IOException#getCause getCause()} method). A null value is permitted, and indicates
     * that the cause is nonexistent or unknown.
     */
    public NetworkException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns error code, one of {@link #ERROR_HOSTNAME_NOT_RESOLVED ERROR_*}.
     *
     * @return error code, one of {@link #ERROR_HOSTNAME_NOT_RESOLVED ERROR_*}.
     */
    public abstract int getErrorCode();

    /**
     * Returns an internal error code. This may provide more specific error
     * diagnosis than {@link #getErrorCode}, but the constant values are not exposed publicly and
     * may change over time. See
     * <a href=https://chromium.googlesource.com/chromium/src/+/main/net/base/net_error_list.h>
     * here</a> for the latest list of values.
     *
     * @return internal error code or 0 if the internal error code is unknown
     *
     * {@hide as semantics aren't stable}
     */
    public int getInternalErrorCode() {
        return 0;
    }

    /**
     * Returns {@code true} if retrying this request right away might succeed, {@code false}
     * otherwise. For example returns {@code true} when {@link #getErrorCode} returns {@link
     * #ERROR_NETWORK_CHANGED} because trying the request might succeed using the new network
     * configuration, but {@code false} when {@code getErrorCode()} returns {@link
     * #ERROR_INTERNET_DISCONNECTED} because retrying the request right away will encounter the same
     * failure (instead retrying should be delayed until device regains network connectivity).
     *
     * @return {@code true} if retrying this request right away might succeed, {@code false}
     * otherwise.
     */
    public abstract boolean isImmediatelyRetryable();
}

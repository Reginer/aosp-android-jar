// Copyright 2016 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

import androidx.annotation.Nullable;

/**
 * Exception passed to {@link UrlRequest.Callback#onFailed UrlRequest.Callback.onFailed()} when
 * {@link UrlRequest.Callback} or {@link UploadDataProvider} method throws an exception. In this
 * case {@link java.io.IOException#getCause getCause()} can be used to find the thrown exception.
 */
public abstract class CallbackException extends HttpException {
    /**
     * Constructs an exception that wraps {@code cause} thrown by a {@link UrlRequest.Callback}.
     *
     * @param message explanation of failure.
     * @param cause exception thrown by {@link UrlRequest.Callback} that's being wrapped. It is
     *         saved
     * for later retrieval by the {@link java.io.IOException#getCause getCause()}.
     */
    protected CallbackException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}

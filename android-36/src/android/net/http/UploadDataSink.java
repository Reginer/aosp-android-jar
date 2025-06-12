// Copyright 2015 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

import androidx.annotation.NonNull;

/**
 * Defines callbacks methods for {@link UploadDataProvider}. All methods may be called synchronously
 * or asynchronously, on any thread.
 */
public abstract class UploadDataSink {
    /**
     * Called by {@link UploadDataProvider} when a read succeeds.
     *
     * @param finalChunk For chunked uploads, {@code true} if this is the final read. It must be
     * {@code false} for non-chunked uploads.
     */
    public abstract void onReadSucceeded(boolean finalChunk);

    /**
     * Called by {@link UploadDataProvider} when a read fails.
     *
     * @param exception Exception passed on to the embedder.
     */
    public abstract void onReadError(@NonNull Exception exception);

    /**
     * Called by {@link UploadDataProvider} when a rewind succeeds.
     */
    public abstract void onRewindSucceeded();

    /**
     * Called by {@link UploadDataProvider} when a rewind fails, or if rewinding uploads is not
     * supported.
     *
     * @param exception Exception passed on to the embedder.
     */
    public abstract void onRewindError(@NonNull Exception exception);
}

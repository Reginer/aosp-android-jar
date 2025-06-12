// Copyright 2015 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net.urlconnection;

import org.chromium.base.metrics.ScopedSysTraceEvent;
import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * An implementation of {@link java.io.OutputStream} that buffers entire request body in memory.
 * This is used when neither {@link CronetHttpURLConnection#setFixedLengthStreamingMode} nor {@link
 * CronetHttpURLConnection#setChunkedStreamingMode} is set.
 */
final class CronetBufferedOutputStream extends CronetOutputStream {
    // QUIC uses a read buffer of 14520 bytes, SPDY uses 2852 bytes, and normal
    // stream uses 16384 bytes. Therefore, use 16384 for now to avoid growing
    // the buffer too many times.
    private static final int INITIAL_BUFFER_SIZE = 16384;
    // If content length is not passed in the constructor, this is -1.
    private final int mInitialContentLength;
    private final CronetHttpURLConnection mConnection;
    private final UploadDataProvider mUploadDataProvider = new UploadDataProviderImpl();
    // Internal buffer that is used to buffer the request body.
    private ByteBuffer mBuffer;
    private boolean mConnectRequested;
    private boolean mConnected;

    /**
     * Package protected constructor.
     *
     * @param connection The CronetHttpURLConnection object.
     * @param contentLength The content length of the request body. It must not be smaller than 0 or
     *     bigger than {@link Integer.MAX_VALUE}.
     */
    CronetBufferedOutputStream(final CronetHttpURLConnection connection, final long contentLength) {
        Objects.requireNonNull(connection, "Argument connection cannot be null.");

        if (contentLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Use setFixedLengthStreamingMode()"
                            + " or setChunkedStreamingMode() for requests larger than 2GB.");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("Content length < 0.");
        }
        mConnection = connection;
        mInitialContentLength = (int) contentLength;
        mBuffer = ByteBuffer.allocate(mInitialContentLength);
    }

    /**
     * Package protected constructor used when content length is not known.
     *
     * @param connection The CronetHttpURLConnection object.
     */
    CronetBufferedOutputStream(final CronetHttpURLConnection connection) {
        mConnection = Objects.requireNonNull(connection);
        mInitialContentLength = -1;
        // Buffering without knowing content-length.
        mBuffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE);
    }

    @Override
    public void write(int oneByte) throws IOException {
        checkNotClosed();
        ensureCanWrite(1);
        mBuffer.put((byte) oneByte);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        checkNotClosed();
        ensureCanWrite(count);
        mBuffer.put(buffer, offset, count);
    }

    /** Ensures that {@code count} bytes can be written to the internal buffer. */
    private void ensureCanWrite(int count) throws IOException {
        if (mInitialContentLength != -1 && mBuffer.position() + count > mInitialContentLength) {
            // Error message is to match that of the default implementation.
            throw new ProtocolException(
                    "exceeded content-length limit of " + mInitialContentLength + " bytes");
        }
        if (mInitialContentLength != -1) {
            // If mInitialContentLength is known, the buffer should not grow.
            return;
        }
        if (mBuffer.limit() - mBuffer.position() > count) {
            // If there is enough capacity, the buffer should not grow.
            return;
        }
        int afterSize = Math.max(mBuffer.capacity() * 2, mBuffer.capacity() + count);
        ByteBuffer newByteBuffer = ByteBuffer.allocate(afterSize);
        mBuffer.flip();
        newByteBuffer.put(mBuffer);
        mBuffer = newByteBuffer;
    }

    // Below are CronetOutputStream implementations:

    @Override
    boolean connectRequested() throws IOException {
        assert !mConnected;

        if (!isClosed()) {
            // Before we can send the request we need to know the size of the request body in order
            // to populate the `Content-Length` buffer, so defer connection until the stream is
            // closed.
            mConnectRequested = true;
            return false;
        }

        mConnected = true;
        if (mBuffer.position() < mInitialContentLength) {
            throw new ProtocolException("Content received is less than Content-Length");
        }
        // Flip the buffer to prepare it for UploadDataProvider read calls.
        mBuffer.flip();
        return true;
    }

    @Override
    public void close() throws IOException {
        super.close();
        // Now we know the size of the request body, so we can send the request with the correct
        // `Content-Length` header.
        if (mConnectRequested) {
            mConnection.connect();
            mConnectRequested = false;
        }
    }

    @Override
    void checkReceivedEnoughContent() throws IOException {
        // Already checked in setConnected. Skip the check here, since mBuffer
        // might be flipped.
    }

    @Override
    UploadDataProvider getUploadDataProvider() {
        return mUploadDataProvider;
    }

    private class UploadDataProviderImpl extends UploadDataProvider {
        @Override
        public long getLength() {
            // This method is supposed to be called just before starting the request.
            // If content length is not initially passed in, the number of bytes
            // written will be used as the content length.
            // TODO(xunjieli): Think of a less fragile way, since getLength() can be
            // potentially called in other places in the future.
            if (mInitialContentLength == -1) {
                // Account for the fact that setConnected() flip()s mBuffer.
                return mConnected ? mBuffer.limit() : mBuffer.position();
            }
            return mInitialContentLength;
        }

        @Override
        public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) {
            try (var traceEvent =
                    ScopedSysTraceEvent.scoped(
                            "CronetBufferedOutputStream.UploadDataProviderImpl#read")) {
                final int availableSpace = byteBuffer.remaining();
                if (availableSpace < mBuffer.remaining()) {
                    byteBuffer.put(mBuffer.array(), mBuffer.position(), availableSpace);
                    mBuffer.position(mBuffer.position() + availableSpace);
                } else {
                    byteBuffer.put(mBuffer);
                }
                uploadDataSink.onReadSucceeded(false);
            }
        }

        @Override
        public void rewind(UploadDataSink uploadDataSink) {
            try (var traceEvent =
                    ScopedSysTraceEvent.scoped(
                            "CronetBufferedOutputStream.UploadDataProviderImpl#rewind")) {
                mBuffer.position(0);
                uploadDataSink.onRewindSucceeded();
            }
        }
    }
}

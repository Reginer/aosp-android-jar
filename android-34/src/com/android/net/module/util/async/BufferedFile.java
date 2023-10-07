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

package com.android.net.module.util.async;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Buffers inbound and outbound file data within given strict limits.
 *
 * Automatically manages all readability and writeability events in EventManager:
 *   - When read buffer has more space - asks EventManager to notify on more data
 *   - When write buffer has more space - asks the user to provide more data
 *   - When underlying file cannot accept more data - registers EventManager callback
 *
 * @hide
 */
public final class BufferedFile implements AsyncFile.Listener {
    /**
     * Receives notifications when new data or output space is available.
     * @hide
     */
    public interface Listener {
        /** Invoked after the underlying file has been closed. */
        void onBufferedFileClosed();

        /** Invoked when there's new data in the inbound buffer. */
        void onBufferedFileInboundData(int readByteCount);

        /** Notifies on data being flushed from output buffer. */
        void onBufferedFileOutboundSpace();

        /** Notifies on unrecoverable error in file access. */
        void onBufferedFileIoError(String message);
    }

    private final Listener mListener;
    private final EventManager mEventManager;
    private AsyncFile mFile;

    private final CircularByteBuffer mInboundBuffer;
    private final AtomicLong mTotalBytesRead = new AtomicLong();
    private boolean mIsReadingShutdown;

    private final CircularByteBuffer mOutboundBuffer;
    private final AtomicLong mTotalBytesWritten = new AtomicLong();

    /** Creates BufferedFile based on the given file descriptor. */
    public static BufferedFile create(
            EventManager eventManager,
            FileHandle fileHandle,
            Listener listener,
            int inboundBufferSize,
            int outboundBufferSize) throws IOException {
        if (fileHandle == null) {
            throw new NullPointerException();
        }
        BufferedFile file = new BufferedFile(
            eventManager, listener, inboundBufferSize, outboundBufferSize);
        file.mFile = eventManager.registerFile(fileHandle, file);
        return file;
    }

    private BufferedFile(
            EventManager eventManager,
            Listener listener,
            int inboundBufferSize,
            int outboundBufferSize) {
        if (eventManager == null || listener == null) {
            throw new NullPointerException();
        }
        mEventManager = eventManager;
        mListener = listener;

        mInboundBuffer = new CircularByteBuffer(inboundBufferSize);
        mOutboundBuffer = new CircularByteBuffer(outboundBufferSize);
    }

    /** Requests this file to be closed. */
    public void close() {
        mFile.close();
    }

    @Override
    public void onClosed(AsyncFile file) {
        mListener.onBufferedFileClosed();
    }

    ///////////////////////////////////////////////////////////////////////////
    // READ PATH
    ///////////////////////////////////////////////////////////////////////////

    /** Returns buffer that is automatically filled with inbound data. */
    public ReadableByteBuffer getInboundBuffer() {
        return mInboundBuffer;
    }

    public int getInboundBufferFreeSizeForTest() {
        return mInboundBuffer.freeSize();
    }

    /** Permanently disables reading of this file, and clears all buffered data. */
    public void shutdownReading() {
        mIsReadingShutdown = true;
        mInboundBuffer.clear();
        mFile.enableReadEvents(false);
    }

    /** Returns true after shutdownReading() has been called. */
    public boolean isReadingShutdown() {
        return mIsReadingShutdown;
    }

    /** Starts or resumes async read operations on this file. */
    public void continueReading() {
        if (!mIsReadingShutdown && mInboundBuffer.freeSize() > 0) {
            mFile.enableReadEvents(true);
        }
    }

    @Override
    public void onReadReady(AsyncFile file) {
        if (mIsReadingShutdown) {
            return;
        }

        int readByteCount;
        try {
            readByteCount = bufferInputData();
        } catch (IOException e) {
            mListener.onBufferedFileIoError("IOException while reading: " + e.toString());
            return;
        }

        if (readByteCount > 0) {
            mListener.onBufferedFileInboundData(readByteCount);
        }

        continueReading();
    }

    private int bufferInputData() throws IOException {
        int totalReadCount = 0;
        while (true) {
            final int maxReadCount = mInboundBuffer.getDirectWriteSize();
            if (maxReadCount == 0) {
                mFile.enableReadEvents(false);
                break;
            }

            final int bufferOffset = mInboundBuffer.getDirectWritePos();
            final byte[] buffer = mInboundBuffer.getDirectWriteBuffer();

            final int readCount = mFile.read(buffer, bufferOffset, maxReadCount);
            if (readCount <= 0) {
                break;
            }

            mInboundBuffer.accountForDirectWrite(readCount);
            totalReadCount += readCount;
        }

        mTotalBytesRead.addAndGet(totalReadCount);
        return totalReadCount;
    }

    ///////////////////////////////////////////////////////////////////////////
    // WRITE PATH
    ///////////////////////////////////////////////////////////////////////////

    /** Returns the number of bytes currently buffered for output. */
    public int getOutboundBufferSize() {
        return mOutboundBuffer.size();
    }

    /** Returns the number of bytes currently available for buffering for output. */
    public int getOutboundBufferFreeSize() {
        return mOutboundBuffer.freeSize();
    }

    /**
     * Queues the given data for output.
     * Throws runtime exception if there is not enough space.
     */
    public boolean enqueueOutboundData(byte[] data, int pos, int len) {
        return enqueueOutboundData(data, pos, len, null, 0, 0);
    }

    /**
     * Queues data1, then data2 for output.
     * Throws runtime exception if there is not enough space.
     */
    public boolean enqueueOutboundData(
            byte[] data1, int pos1, int len1,
            byte[] buffer2, int pos2, int len2) {
        Assertions.throwsIfOutOfBounds(data1, pos1, len1);
        Assertions.throwsIfOutOfBounds(buffer2, pos2, len2);

        final int totalLen = len1 + len2;

        if (totalLen > mOutboundBuffer.freeSize()) {
            flushOutboundBuffer();

            if (totalLen > mOutboundBuffer.freeSize()) {
                return false;
            }
        }

        mOutboundBuffer.writeBytes(data1, pos1, len1);

        if (buffer2 != null) {
            mOutboundBuffer.writeBytes(buffer2, pos2, len2);
        }

        flushOutboundBuffer();

        return true;
    }

    private void flushOutboundBuffer() {
        try {
            while (mOutboundBuffer.getDirectReadSize() > 0) {
                final int maxReadSize = mOutboundBuffer.getDirectReadSize();
                final int writeCount = mFile.write(
                    mOutboundBuffer.getDirectReadBuffer(),
                    mOutboundBuffer.getDirectReadPos(),
                    maxReadSize);

                if (writeCount == 0) {
                    mFile.enableWriteEvents(true);
                    break;
                }

                if (writeCount > maxReadSize) {
                    throw new IllegalArgumentException(
                        "Write count " + writeCount + " above max " + maxReadSize);
                }

                mOutboundBuffer.accountForDirectRead(writeCount);
            }
        } catch (IOException e) {
            scheduleOnIoError("IOException while writing: " + e.toString());
        }
    }

    private void scheduleOnIoError(String message) {
        mEventManager.execute(() -> {
            mListener.onBufferedFileIoError(message);
        });
    }

    @Override
    public void onWriteReady(AsyncFile file) {
        mFile.enableWriteEvents(false);
        flushOutboundBuffer();
        mListener.onBufferedFileOutboundSpace();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("file={");
        sb.append(mFile);
        sb.append("}");
        if (mIsReadingShutdown) {
            sb.append(", readingShutdown");
        }
        sb.append("}, inboundBuffer={");
        sb.append(mInboundBuffer);
        sb.append("}, outboundBuffer={");
        sb.append(mOutboundBuffer);
        sb.append("}, totalBytesRead=");
        sb.append(mTotalBytesRead);
        sb.append(", totalBytesWritten=");
        sb.append(mTotalBytesWritten);
        return sb.toString();
    }
}

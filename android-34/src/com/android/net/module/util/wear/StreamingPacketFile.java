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

package com.android.net.module.util.wear;

import com.android.net.module.util.async.BufferedFile;
import com.android.net.module.util.async.EventManager;
import com.android.net.module.util.async.FileHandle;
import com.android.net.module.util.async.Assertions;
import com.android.net.module.util.async.ReadableByteBuffer;

import java.io.IOException;

/**
 * Implements PacketFile based on a streaming file descriptor.
 *
 * Packets are delineated using network-order 2-byte length indicators.
 *
 * @hide
 */
public final class StreamingPacketFile implements PacketFile, BufferedFile.Listener {
    private static final int HEADER_SIZE = 2;

    private final EventManager mEventManager;
    private final Listener mListener;
    private final BufferedFile mFile;
    private final int mMaxPacketSize;
    private final ReadableByteBuffer mInboundBuffer;
    private boolean mIsInPreamble = true;

    private final byte[] mTempPacketReadBuffer;
    private final byte[] mTempHeaderWriteBuffer;

    public StreamingPacketFile(
            EventManager eventManager,
            FileHandle fileHandle,
            Listener listener,
            int maxPacketSize,
            int maxBufferedInboundPackets,
            int maxBufferedOutboundPackets) throws IOException {
        if (eventManager == null || fileHandle == null || listener == null) {
            throw new NullPointerException();
        }

        mEventManager = eventManager;
        mListener = listener;
        mMaxPacketSize = maxPacketSize;

        final int maxTotalLength = HEADER_SIZE + maxPacketSize;

        mFile = BufferedFile.create(eventManager, fileHandle, this,
            maxTotalLength * maxBufferedInboundPackets,
            maxTotalLength * maxBufferedOutboundPackets);
        mInboundBuffer = mFile.getInboundBuffer();

        mTempPacketReadBuffer = new byte[maxTotalLength];
        mTempHeaderWriteBuffer = new byte[HEADER_SIZE];
    }

    @Override
    public void close() {
        mFile.close();
    }

    public BufferedFile getUnderlyingFileForTest() {
        return mFile;
    }

    @Override
    public void shutdownReading() {
        mFile.shutdownReading();
    }

    @Override
    public void continueReading() {
        mFile.continueReading();
    }

    @Override
    public int getInboundBufferSize() {
        return mInboundBuffer.size();
    }

    @Override
    public void onBufferedFileClosed() {
    }

    @Override
    public void onBufferedFileInboundData(int readByteCount) {
        if (mFile.isReadingShutdown()) {
            return;
        }

        if (readByteCount > 0) {
            mListener.onInboundBuffered(readByteCount, mInboundBuffer.size());
        }

        if (extractOnePacket() && !mFile.isReadingShutdown()) {
            // There could be more packets already buffered, continue parsing next
            // packet even before another read event comes
            mEventManager.execute(() -> {
                onBufferedFileInboundData(0);
            });
        } else {
            continueReading();
        }
    }

    private boolean extractOnePacket() {
        while (mIsInPreamble) {
            final int directReadSize = Math.min(
                mInboundBuffer.getDirectReadSize(), mTempPacketReadBuffer.length);
            if (directReadSize == 0) {
                return false;
            }

            // Copy for safety, so higher-level callback cannot modify the data.
            System.arraycopy(mInboundBuffer.getDirectReadBuffer(),
                mInboundBuffer.getDirectReadPos(), mTempPacketReadBuffer, 0, directReadSize);

            final int preambleConsumedBytes = mListener.onPreambleData(
                mTempPacketReadBuffer, 0, directReadSize);
            if (mFile.isReadingShutdown()) {
                return false;  // The callback has called shutdownReading().
            }

            if (preambleConsumedBytes == 0) {
                mIsInPreamble = false;
                break;
            }

            mInboundBuffer.accountForDirectRead(preambleConsumedBytes);
        }

        final int bufferedSize = mInboundBuffer.size();
        if (bufferedSize < HEADER_SIZE) {
            return false;
        }

        final int dataLength = NetPacketHelpers.decodeNetworkUnsignedInt16(mInboundBuffer, 0);
        if (dataLength > mMaxPacketSize) {
            mListener.onPacketFileError(
                PacketFile.ErrorCode.INBOUND_PACKET_TOO_LARGE,
                "Inbound packet length: " + dataLength);
            return false;
        }

        final int totalLength = HEADER_SIZE + dataLength;
        if (bufferedSize < totalLength) {
            return false;
        }

        mInboundBuffer.readBytes(mTempPacketReadBuffer, 0, totalLength);

        mListener.onInboundPacket(mTempPacketReadBuffer, HEADER_SIZE, dataLength);
        return true;
    }

    @Override
    public int getOutboundFreeSize() {
        final int freeSize = mFile.getOutboundBufferFreeSize();
        return (freeSize > HEADER_SIZE ? freeSize - HEADER_SIZE : 0);
    }

    @Override
    public boolean enqueueOutboundPacket(byte[] buffer, int pos, int len) {
        Assertions.throwsIfOutOfBounds(buffer, pos, len);

        if (len == 0) {
            return true;
        }

        if (len > mMaxPacketSize) {
            mListener.onPacketFileError(
                PacketFile.ErrorCode.OUTBOUND_PACKET_TOO_LARGE,
                "Outbound packet length: " + len);
            return false;
        }

        NetPacketHelpers.encodeNetworkUnsignedInt16(len, mTempHeaderWriteBuffer, 0);

        mFile.enqueueOutboundData(
            mTempHeaderWriteBuffer, 0, mTempHeaderWriteBuffer.length,
            buffer, pos, len);
        return true;
    }

    @Override
    public void onBufferedFileOutboundSpace() {
        mListener.onOutboundPacketSpace();
    }

    @Override
    public void onBufferedFileIoError(String message) {
        mListener.onPacketFileError(PacketFile.ErrorCode.IO_ERROR, message);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("maxPacket=");
        sb.append(mMaxPacketSize);
        sb.append(", file={");
        sb.append(mFile);
        sb.append("}");
        return sb.toString();
    }
}

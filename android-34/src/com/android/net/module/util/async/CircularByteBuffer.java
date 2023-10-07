/*
 * Copyright (C) 2022 The Android Open Source Project
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

import java.util.Arrays;

/**
 * Implements a circular read-write byte buffer.
 *
 * @hide
 */
public final class CircularByteBuffer implements ReadableByteBuffer {
    private final byte[] mBuffer;
    private final int mCapacity;
    private int mReadPos;
    private int mWritePos;
    private int mSize;

    public CircularByteBuffer(int capacity) {
        mCapacity = capacity;
        mBuffer = new byte[mCapacity];
    }

    @Override
    public void clear() {
        mReadPos = 0;
        mWritePos = 0;
        mSize = 0;
        Arrays.fill(mBuffer, (byte) 0);
    }

    @Override
    public int capacity() {
        return mCapacity;
    }

    @Override
    public int size() {
        return mSize;
    }

    /** Returns the amount of remaining writeable space in this buffer. */
    public int freeSize() {
        return mCapacity - mSize;
    }

    @Override
    public byte peek(int offset) {
        if (offset < 0 || offset >= size()) {
            throw new IllegalArgumentException("Invalid offset=" + offset + ", size=" + size());
        }

        return mBuffer[(mReadPos + offset) % mCapacity];
    }

    @Override
    public void readBytes(byte[] dst, int dstPos, int dstLen) {
        if (dst != null) {
            Assertions.throwsIfOutOfBounds(dst, dstPos, dstLen);
        }
        if (dstLen > size()) {
            throw new IllegalArgumentException("Invalid len=" + dstLen + ", size=" + size());
        }

        while (dstLen > 0) {
            final int copyLen = getCopyLen(mReadPos, mWritePos, dstLen);
            if (dst != null) {
                System.arraycopy(mBuffer, mReadPos, dst, dstPos, copyLen);
            }
            dstPos += copyLen;
            dstLen -= copyLen;
            mSize -= copyLen;
            mReadPos = (mReadPos + copyLen) % mCapacity;
        }

        if (mSize == 0) {
            // Reset to the beginning for better contiguous access.
            mReadPos = 0;
            mWritePos = 0;
        }
    }

    @Override
    public void peekBytes(int offset, byte[] dst, int dstPos, int dstLen) {
        Assertions.throwsIfOutOfBounds(dst, dstPos, dstLen);
        if (offset + dstLen > size()) {
            throw new IllegalArgumentException("Invalid len=" + dstLen
                    + ", offset=" + offset + ", size=" + size());
        }

        int tmpReadPos = (mReadPos + offset) % mCapacity;
        while (dstLen > 0) {
            final int copyLen = getCopyLen(tmpReadPos, mWritePos, dstLen);
            System.arraycopy(mBuffer, tmpReadPos, dst, dstPos, copyLen);
            dstPos += copyLen;
            dstLen -= copyLen;
            tmpReadPos = (tmpReadPos + copyLen) % mCapacity;
        }
    }

    @Override
    public int getDirectReadSize() {
        if (size() == 0) {
            return 0;
        }
        return (mReadPos < mWritePos ? (mWritePos - mReadPos) : (mCapacity - mReadPos));
    }

    @Override
    public int getDirectReadPos() {
        return mReadPos;
    }

    @Override
    public byte[] getDirectReadBuffer() {
        return mBuffer;
    }

    @Override
    public void accountForDirectRead(int len) {
        if (len < 0 || len > size()) {
            throw new IllegalArgumentException("Invalid len=" + len + ", size=" + size());
        }

        mSize -= len;
        mReadPos = (mReadPos + len) % mCapacity;
    }

    /** Copies given data to the end of the buffer. */
    public void writeBytes(byte[] buffer, int pos, int len) {
        Assertions.throwsIfOutOfBounds(buffer, pos, len);
        if (len > freeSize()) {
            throw new IllegalArgumentException("Invalid len=" + len + ", size=" + freeSize());
        }

        while (len > 0) {
            final int copyLen = getCopyLen(mWritePos, mReadPos,len);
            System.arraycopy(buffer, pos, mBuffer, mWritePos, copyLen);
            pos += copyLen;
            len -= copyLen;
            mSize += copyLen;
            mWritePos = (mWritePos + copyLen) % mCapacity;
        }
    }

    private int getCopyLen(int startPos, int endPos, int len) {
        if (startPos < endPos) {
            return Math.min(len, endPos - startPos);
        } else {
            return Math.min(len, mCapacity - startPos);
        }
    }

    /** Returns the amount of contiguous writeable space. */
    public int getDirectWriteSize() {
        if (freeSize() == 0) {
            return 0;  // Return zero in case buffer is full.
        }
        return (mWritePos < mReadPos ? (mReadPos - mWritePos) : (mCapacity - mWritePos));
    }

    /** Returns the position of contiguous writeable space. */
    public int getDirectWritePos() {
        return mWritePos;
    }

    /** Returns the buffer reference for direct write operation. */
    public byte[] getDirectWriteBuffer() {
        return mBuffer;
    }

    /** Must be called after performing a direct write using getDirectWriteBuffer(). */
    public void accountForDirectWrite(int len) {
        if (len < 0 || len > freeSize()) {
            throw new IllegalArgumentException("Invalid len=" + len + ", size=" + freeSize());
        }

        mSize += len;
        mWritePos = (mWritePos + len) % mCapacity;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CircularByteBuffer{c=");
        sb.append(mCapacity);
        sb.append(",s=");
        sb.append(mSize);
        sb.append(",r=");
        sb.append(mReadPos);
        sb.append(",w=");
        sb.append(mWritePos);
        sb.append('}');
        return sb.toString();
    }
}

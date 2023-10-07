package com.android.networkstack.tethering.companionproxy.util;

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
            int copyLen;
            if (mReadPos < mWritePos) {
                copyLen = Math.min(dstLen, mWritePos - mReadPos);
            } else {
                copyLen = Math.min(dstLen, mCapacity - mReadPos);
            }
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
            int copyLen;
            if (tmpReadPos < mWritePos) {
                copyLen = Math.min(dstLen, mWritePos - tmpReadPos);
            } else {
                copyLen = Math.min(dstLen, mCapacity - tmpReadPos);
            }
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
            int copyLen;
            if (mWritePos < mReadPos) {
                copyLen = Math.min(len, mReadPos - mWritePos);
            } else {
                copyLen = Math.min(len, mCapacity - mWritePos);
            }
            System.arraycopy(buffer, pos, mBuffer, mWritePos, copyLen);
            pos += copyLen;
            len -= copyLen;
            mSize += copyLen;
            mWritePos = (mWritePos + copyLen) % mCapacity;
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

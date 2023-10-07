package com.android.networkstack.tethering.companionproxy.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CircularByteBufferTest {
    @Test
    public void writeBytes() {
        final int capacity = 23;
        CircularByteBuffer buffer = new CircularByteBuffer(capacity);
        assertEquals(0, buffer.size());
        assertEquals(0, buffer.getDirectReadSize());
        assertEquals(capacity, buffer.freeSize());
        assertEquals(capacity, buffer.getDirectWriteSize());

        final byte[] writeBuffer = new byte[15];
        buffer.writeBytes(writeBuffer, 0, writeBuffer.length);

        assertEquals(writeBuffer.length, buffer.size());
        assertEquals(writeBuffer.length, buffer.getDirectReadSize());
        assertEquals(capacity - writeBuffer.length, buffer.freeSize());
        assertEquals(capacity - writeBuffer.length, buffer.getDirectWriteSize());

        buffer.clear();
        assertEquals(0, buffer.size());
        assertEquals(0, buffer.getDirectReadSize());
        assertEquals(capacity, buffer.freeSize());
        assertEquals(capacity, buffer.getDirectWriteSize());
    }

    @Test
    public void writeBytes_withRollover() {
        doTestReadWriteWithRollover(new BufferAccessor(false, false));
    }

    @Test
    public void writeBytes_withFullBuffer() {
        doTestReadWriteWithFullBuffer(new BufferAccessor(false, false));
    }

    @Test
    public void directWriteBytes_withRollover() {
        doTestReadWriteWithRollover(new BufferAccessor(true, true));
    }

    @Test
    public void directWriteBytes_withFullBuffer() {
        doTestReadWriteWithFullBuffer(new BufferAccessor(true, true));
    }

    private void doTestReadWriteWithFullBuffer(BufferAccessor accessor) {
        CircularByteBuffer buffer = doTestReadWrite(accessor, 20, 5, 4);

        assertEquals(0, buffer.size());
        assertEquals(20, buffer.freeSize());
    }

    private void doTestReadWriteWithRollover(BufferAccessor accessor) {
        // All buffer sizes are prime numbers to ensure that some read or write
        // operations will roll over the end of the internal buffer.
        CircularByteBuffer buffer = doTestReadWrite(accessor, 31, 13, 7);

        assertNotEquals(0, buffer.size());
    }

    private CircularByteBuffer doTestReadWrite(BufferAccessor accessor,
            final int capacity, final int writeLen, final int readLen) {
        CircularByteBuffer buffer = new CircularByteBuffer(capacity);

        final byte[] writeBuffer = new byte[writeLen + 2];
        final byte[] peekBuffer = new byte[readLen + 2];
        final byte[] readBuffer = new byte[readLen + 2];

        final int numIterations = 1011;
        final int maxRemaining = readLen - 1;

        int currentWriteSymbol = 0;
        int expectedReadSymbol = 0;
        int expectedSize = 0;
        int totalWritten = 0;
        int totalRead = 0;

        for (int i = 0; i < numIterations; i++) {
            // Fill in with write buffers as much as possible.
            while (buffer.freeSize() >= writeLen) {
                currentWriteSymbol = fillTestBytes(writeBuffer, 1, writeLen, currentWriteSymbol);
                accessor.writeBytes(buffer, writeBuffer, 1, writeLen);

                expectedSize += writeLen;
                totalWritten += writeLen;
                assertEquals(expectedSize, buffer.size());
                assertEquals(capacity - expectedSize, buffer.freeSize());
            }

            // Keep reading into read buffers while there's still data.
            while (buffer.size() >= readLen) {
                peekBuffer[1] = 0;
                peekBuffer[2] = 0;
                buffer.peekBytes(2, peekBuffer, 3, readLen - 2);
                assertEquals(0, peekBuffer[1]);
                assertEquals(0, peekBuffer[2]);

                peekBuffer[2] = buffer.peek(1);

                accessor.readBytes(buffer, readBuffer, 1, readLen);
                peekBuffer[1] = readBuffer[1];

                expectedReadSymbol = checkTestBytes(
                    readBuffer, 1, readLen, expectedReadSymbol, totalRead);

                assertArrayEquals(peekBuffer, readBuffer);

                expectedSize -= readLen;
                totalRead += readLen;
                assertEquals(expectedSize, buffer.size());
                assertEquals(capacity - expectedSize, buffer.freeSize());
            }

            if (buffer.size() > maxRemaining) {
                fail("Too much data remaining: " + buffer.size());
            }
        }

        final int maxWritten = capacity * numIterations;
        final int minWritten = maxWritten / 2;
        if (totalWritten < minWritten || totalWritten > maxWritten
                || (totalWritten - totalRead) > maxRemaining) {
            fail("Unexpected counts: read=" + totalRead + ", written=" + totalWritten
                    + ", minWritten=" + minWritten + ", maxWritten=" + maxWritten);
        }

        return buffer;
    }

    @Test
    public void readBytes_overflow() {
        CircularByteBuffer buffer = new CircularByteBuffer(23);

        final byte[] dataBuffer = new byte[15];
        buffer.writeBytes(dataBuffer, 0, dataBuffer.length - 2);

        try {
            buffer.readBytes(dataBuffer, 0, dataBuffer.length);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            // expected
        }

        assertEquals(13, buffer.size());
        assertEquals(10, buffer.freeSize());
    }

    @Test
    public void writeBytes_overflow() {
        CircularByteBuffer buffer = new CircularByteBuffer(23);

        final byte[] dataBuffer = new byte[15];
        buffer.writeBytes(dataBuffer, 0, dataBuffer.length);

        try {
            buffer.writeBytes(dataBuffer, 0, dataBuffer.length);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            // expected
        }

        assertEquals(15, buffer.size());
        assertEquals(8, buffer.freeSize());
    }

    private static int fillTestBytes(byte[] buffer, int pos, int len, int startValue) {
        for (int i = 0; i < len; i++) {
            buffer[pos + i] = (byte) (startValue & 0xFF);
            startValue = (startValue + 1) % 256;
        }
        return startValue;
    }

    private static int checkTestBytes(
            byte[] buffer, int pos, int len, int startValue, int totalRead) {
        for (int i = 0; i < len; i++) {
            byte expectedValue = (byte) (startValue & 0xFF);
            if (expectedValue != buffer[pos + i]) {
                fail("Unexpected byte=" + (((int) buffer[pos + i]) & 0xFF)
                        + ", expected=" + (((int) expectedValue) & 0xFF)
                        + ", pos=" + (totalRead + i));
            }
            startValue = (startValue + 1) % 256;
        }
        return startValue;
    }

    private final class BufferAccessor {
        private final boolean mDirectRead;
        private final boolean mDirectWrite;

        BufferAccessor(boolean directRead, boolean directWrite) {
            mDirectRead = directRead;
            mDirectWrite = directWrite;
        }

        void writeBytes(CircularByteBuffer buffer, byte[] src, int pos, int len) {
            if (mDirectWrite) {
                while (len > 0) {
                    if (buffer.getDirectWriteSize() == 0) {
                        fail("Direct write size is zero: free=" + buffer.freeSize()
                                + ", size=" + buffer.size());
                    }
                    int copyLen = Math.min(len, buffer.getDirectWriteSize());
                    System.arraycopy(src, pos, buffer.getDirectWriteBuffer(),
                        buffer.getDirectWritePos(), copyLen);
                    buffer.accountForDirectWrite(copyLen);
                    len -= copyLen;
                    pos += copyLen;
                }
            } else {
                buffer.writeBytes(src, pos, len);
            }
        }

        void readBytes(CircularByteBuffer buffer, byte[] dst, int pos, int len) {
            if (mDirectRead) {
                while (len > 0) {
                    if (buffer.getDirectReadSize() == 0) {
                        fail("Direct read size is zero: free=" + buffer.freeSize()
                                + ", size=" + buffer.size());
                    }
                    int copyLen = Math.min(len, buffer.getDirectReadSize());
                    System.arraycopy(
                        buffer.getDirectReadBuffer(), buffer.getDirectReadPos(), dst, pos, copyLen);
                    buffer.accountForDirectRead(copyLen);
                    len -= copyLen;
                    pos += copyLen;
                }
            } else {
                buffer.readBytes(dst, pos, len);
            }
        }
    }
}

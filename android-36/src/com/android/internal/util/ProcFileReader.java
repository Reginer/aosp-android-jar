/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.internal.util;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;

/**
 * Reader that specializes in parsing {@code /proc/} files quickly. Walks
 * through the stream using a single space {@code ' '} as token separator, and
 * requires each line boundary to be explicitly acknowledged using
 * {@link #finishLine()}. Assumes {@link StandardCharsets#US_ASCII} encoding.
 * <p>
 * Currently doesn't support formats based on {@code \0}, tabs.
 * Consecutive spaces are treated as a single delimiter.
 */
@android.ravenwood.annotation.RavenwoodKeepWholeClass
public class ProcFileReader implements Closeable {
    private final InputStream mStream;
    private final byte[] mBuffer;

    /** Write pointer in {@link #mBuffer}. */
    private int mTail;
    /** Flag when last read token finished current line. */
    private boolean mLineFinished;

    public ProcFileReader(InputStream stream) throws IOException {
        this(stream, 4096);
    }

    public ProcFileReader(InputStream stream, int bufferSize) throws IOException {
        mStream = stream;
        mBuffer = new byte[bufferSize];
        if (stream.markSupported()) {
            mStream.mark(0);
        }

        // read enough to answer hasMoreData
        fillBuf();
    }

    /**
     * Read more data from {@link #mStream} into internal buffer.
     */
    private int fillBuf() throws IOException {
        final int length = mBuffer.length - mTail;
        if (length == 0) {
            throw new IOException("attempting to fill already-full buffer");
        }

        final int read = mStream.read(mBuffer, mTail, length);
        if (read != -1) {
            mTail += read;
        }
        return read;
    }

    /**
     * Consume number of bytes from beginning of internal buffer. If consuming
     * all remaining bytes, will attempt to {@link #fillBuf()}.
     */
    private void consumeBuf(int count) throws IOException {
        // TODO: consider moving to read pointer, but for now traceview says
        // these copies aren't a bottleneck.

        // skip all consecutive delimiters.
        while (count < mTail && mBuffer[count] == ' ') {
            count++;
        }
        System.arraycopy(mBuffer, count, mBuffer, 0, mTail - count);
        mTail -= count;
        if (mTail == 0) {
            fillBuf();

            if (mTail > 0 && mBuffer[0] == ' ') {
                // After filling the buffer, it contains more consecutive
                // delimiters that need to be skipped.
                consumeBuf(0);
            }
        }
    }

    /**
     * Find buffer index of next token delimiter, usually space or newline.
     * Fills buffer as needed.
     *
     * @return Index of next delimeter, otherwise -1 if no tokens remain on
     *         current line.
     */
    private int nextTokenIndex() throws IOException {
        if (mLineFinished) {
            return -1;
        }

        int i = 0;
        do {
            // scan forward for token boundary
            for (; i < mTail; i++) {
                final byte b = mBuffer[i];
                if (b == '\n') {
                    mLineFinished = true;
                    return i;
                }
                if (b == ' ') {
                    return i;
                }
            }
        } while (fillBuf() > 0);

        throw new ProtocolException("End of stream while looking for token boundary");
    }

    /**
     * Check if stream has more data to be parsed.
     */
    public boolean hasMoreData() {
        return mTail > 0;
    }

    /**
     * Finish current line, skipping any remaining data.
     */
    public void finishLine() throws IOException {
        // last token already finished line; reset silently
        if (mLineFinished) {
            mLineFinished = false;
            return;
        }

        int i = 0;
        do {
            // scan forward for line boundary and consume
            for (; i < mTail; i++) {
                if (mBuffer[i] == '\n') {
                    consumeBuf(i + 1);
                    return;
                }
            }
        } while (fillBuf() > 0);

        throw new ProtocolException("End of stream while looking for line boundary");
    }

    /**
     * Parse and return next token as {@link String}.
     */
    public String nextString() throws IOException {
        final int tokenIndex = nextTokenIndex();
        if (tokenIndex == -1) {
            throw new ProtocolException("Missing required string");
        } else {
            return parseAndConsumeString(tokenIndex);
        }
    }

    /**
     * Parse and return next token as base-10 encoded {@code long}.
     */
    public long nextLong() throws IOException {
        return nextLong(false);
    }

    /**
     * Parse and return next token as base-10 encoded {@code long}.
     */
    public long nextLong(boolean stopAtInvalid) throws IOException {
        final int tokenIndex = nextTokenIndex();
        if (tokenIndex == -1) {
            throw new ProtocolException("Missing required long");
        } else {
            return parseAndConsumeLong(tokenIndex, stopAtInvalid);
        }
    }

    /**
     * Parse and return next token as base-10 encoded {@code long}, or return
     * the given default value if no remaining tokens on current line.
     */
    public long nextOptionalLong(long def) throws IOException {
        final int tokenIndex = nextTokenIndex();
        if (tokenIndex == -1) {
            return def;
        } else {
            return parseAndConsumeLong(tokenIndex, false);
        }
    }

    private String parseAndConsumeString(int tokenIndex) throws IOException {
        final String s = new String(mBuffer, 0, tokenIndex, StandardCharsets.US_ASCII);
        consumeBuf(tokenIndex + 1);
        return s;
    }

    /**
     * If stopAtInvalid is true, don't throw IOException but return whatever parsed so far.
     */
    private long parseAndConsumeLong(int tokenIndex, boolean stopAtInvalid) throws IOException {
        final boolean negative = mBuffer[0] == '-';

        // TODO: refactor into something like IntegralToString
        long result = 0;
        for (int i = negative ? 1 : 0; i < tokenIndex; i++) {
            final int digit = mBuffer[i] - '0';
            if (digit < 0 || digit > 9) {
                if (stopAtInvalid) {
                    break;
                } else {
                    throw invalidLong(tokenIndex);
                }
            }

            // always parse as negative number and apply sign later; this
            // correctly handles MIN_VALUE which is "larger" than MAX_VALUE.
            final long next = result * 10 - digit;
            if (next > result) {
                throw invalidLong(tokenIndex);
            }
            result = next;
        }

        consumeBuf(tokenIndex + 1);
        return negative ? result : -result;
    }

    private NumberFormatException invalidLong(int tokenIndex) {
        return new NumberFormatException(
                "invalid long: " + new String(mBuffer, 0, tokenIndex, StandardCharsets.US_ASCII));
    }

    /**
     * Parse and return next token as base-10 encoded {@code int}.
     */
    public int nextInt() throws IOException {
        final long value = nextLong();
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new NumberFormatException("parsed value larger than integer");
        }
        return (int) value;
    }

    /**
     * Bypass the next token.
     */
    public void nextIgnored() throws IOException {
        final int tokenIndex = nextTokenIndex();
        if (tokenIndex == -1) {
            throw new ProtocolException("Missing required token");
        } else {
            consumeBuf(tokenIndex + 1);
        }
    }

    /**
     * Reset file position and internal buffer
     * @throws IOException
     */
    public void rewind() throws IOException {
        if (mStream instanceof FileInputStream) {
            ((FileInputStream) mStream).getChannel().position(0);
        } else if (mStream.markSupported()) {
            mStream.reset();
        } else {
            throw new IOException("The InputStream is NOT markable");
        }

        mTail = 0;
        mLineFinished = false;
        fillBuf();
    }

    @Override
    public void close() throws IOException {
        mStream.close();
    }
}

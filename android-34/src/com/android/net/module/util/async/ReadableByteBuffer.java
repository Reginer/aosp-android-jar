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

/**
 * Allows reading from a buffer of bytes. The data can be read and thus removed,
 * or peeked at without disturbing the buffer state.
 *
 * @hide
 */
public interface ReadableByteBuffer {
    /** Returns the size of the buffered data. */
    int size();

    /**
     * Returns the maximum amount of the buffered data.
     *
     * The caller may use this method in combination with peekBytes()
     * to estimate when the buffer needs to be emptied using readData().
     */
    int capacity();

    /** Clears all buffered data. */
    void clear();

    /** Returns a single byte at the given offset. */
    byte peek(int offset);

    /** Copies an array of bytes from the given offset to "dst". */
    void peekBytes(int offset, byte[] dst, int dstPos, int dstLen);

    /** Reads and removes an array of bytes from the head of the buffer. */
    void readBytes(byte[] dst, int dstPos, int dstLen);

    /** Returns the amount of contiguous readable data. */
    int getDirectReadSize();

    /** Returns the position of contiguous readable data. */
    int getDirectReadPos();

    /** Returns the buffer reference for direct read operation. */
    byte[] getDirectReadBuffer();

    /** Must be called after performing a direct read using getDirectReadBuffer(). */
    void accountForDirectRead(int len);
}

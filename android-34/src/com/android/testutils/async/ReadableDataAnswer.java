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

package com.android.testutils.async;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;

public class ReadableDataAnswer implements Answer {
    private final ArrayList<byte[]> mBuffers = new ArrayList<>();
    private int mBufferPos;

    public ReadableDataAnswer(byte[] ... buffers) {
        for (byte[] buffer : buffers) {
            addBuffer(buffer);
        }
    }

    public void addBuffer(byte[] buffer) {
        if (buffer.length != 0) {
            mBuffers.add(buffer);
        }
    }

    public int getRemainingSize() {
        int totalSize = 0;
        for (byte[] buffer : mBuffers) {
            totalSize += buffer.length;
        }
        return totalSize - mBufferPos;
    }

    private void cleanupBuffers() {
        if (!mBuffers.isEmpty() && mBufferPos == mBuffers.get(0).length) {
            mBuffers.remove(0);
            mBufferPos = 0;
        }
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        cleanupBuffers();

        if (mBuffers.isEmpty()) {
            return Integer.valueOf(0);
        }

        byte[] src = mBuffers.get(0);

        byte[] dst = invocation.<byte[]>getArgument(0);
        int dstPos = invocation.<Integer>getArgument(1);
        int dstLen = invocation.<Integer>getArgument(2);

        int copyLen = Math.min(dstLen, src.length - mBufferPos);
        System.arraycopy(src, mBufferPos, dst, dstPos, copyLen);
        mBufferPos += copyLen;

        cleanupBuffers();
        return Integer.valueOf(copyLen);
    }
}

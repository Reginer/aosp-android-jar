package com.android.networkstack.tethering.companionproxy.io;

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

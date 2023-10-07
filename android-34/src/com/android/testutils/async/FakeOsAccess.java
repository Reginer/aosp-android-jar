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

import android.os.ParcelFileDescriptor;
import android.system.StructPollfd;
import android.util.Log;

import com.android.net.module.util.async.CircularByteBuffer;
import com.android.net.module.util.async.OsAccess;

import java.io.FileDescriptor;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class FakeOsAccess extends OsAccess {
    public static final boolean ENABLE_FINE_DEBUG = true;

    public static final int DEFAULT_FILE_DATA_QUEUE_SIZE = 8 * 1024;

    private enum FileType { PAIR, PIPE }

    // Common poll() constants:
    private static final short POLLIN  = 0x0001;
    private static final short POLLOUT = 0x0004;
    private static final short POLLERR = 0x0008;
    private static final short POLLHUP = 0x0010;

    private static final Constructor<FileDescriptor> FD_CONSTRUCTOR;
    private static final Field FD_FIELD_DESCRIPTOR;
    private static final Field PFD_FIELD_DESCRIPTOR;
    private static final Field PFD_FIELD_GUARD;
    private static final Method CLOSE_GUARD_METHOD_CLOSE;

    private final int mReadQueueSize = DEFAULT_FILE_DATA_QUEUE_SIZE;
    private final int mWriteQueueSize = DEFAULT_FILE_DATA_QUEUE_SIZE;
    private final HashMap<Integer, File> mFiles = new HashMap<>();
    private final byte[] mTmpBuffer = new byte[1024];
    private final long mStartTime;
    private final String mLogTag;
    private int mFileNumberGen = 3;
    private boolean mHasRateLimitedData;

    public FakeOsAccess(String logTag) {
        mLogTag = logTag;
        mStartTime = monotonicTimeMillis();
    }

    @Override
    public long monotonicTimeMillis() {
        return System.nanoTime() / 1000000;
    }

    @Override
    public FileDescriptor getInnerFileDescriptor(ParcelFileDescriptor fd) {
        try {
            return (FileDescriptor) PFD_FIELD_DESCRIPTOR.get(fd);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close(ParcelFileDescriptor fd) {
        if (fd != null) {
            close(getInnerFileDescriptor(fd));

            try {
                // Reduce CloseGuard warnings.
                Object guard = PFD_FIELD_GUARD.get(fd);
                CLOSE_GUARD_METHOD_CLOSE.invoke(guard);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized void close(FileDescriptor fd) {
        if (fd != null) {
            File file = getFileOrNull(fd);
            if (file != null) {
                file.decreaseRefCount();
                mFiles.remove(getFileDescriptorNumber(fd));
                setFileDescriptorNumber(fd, -1);
                notifyAll();
            }
        }
    }

    private File getFile(String func, FileDescriptor fd) throws IOException {
        File file = getFileOrNull(fd);
        if (file == null) {
            throw newIOException(func, "Unknown file descriptor: " + getFileDebugName(fd));
        }
        return file;
    }

    private File getFileOrNull(FileDescriptor fd) {
        return mFiles.get(getFileDescriptorNumber(fd));
    }

    @Override
    public String getFileDebugName(ParcelFileDescriptor fd) {
        return (fd != null ? getFileDebugName(getInnerFileDescriptor(fd)) : "null");
    }

    public String getFileDebugName(FileDescriptor fd) {
        if (fd == null) {
            return "null";
        }

        final int fdNumber = getFileDescriptorNumber(fd);
        File file = mFiles.get(fdNumber);

        StringBuilder sb = new StringBuilder();
        if (file != null) {
            if (file.name != null) {
                sb.append(file.name);
                sb.append("/");
            }
            sb.append(file.type);
            sb.append("/");
        } else {
            sb.append("BADFD/");
        }
        sb.append(fdNumber);
        return sb.toString();
    }

    public synchronized void setFileName(FileDescriptor fd, String name) {
        File file = getFileOrNull(fd);
        if (file != null) {
            file.name = name;
        }
    }

    @Override
    public synchronized void setNonBlocking(FileDescriptor fd) throws IOException {
        File file = getFile("fcntl", fd);
        file.isBlocking = false;
    }

    @Override
    public synchronized int read(FileDescriptor fd, byte[] buffer, int pos, int len)
            throws IOException {
        checkBoundaries("read", buffer, pos, len);

        File file = getFile("read", fd);
        if (file.readQueue == null) {
            throw newIOException("read", "File not readable");
        }
        file.checkNonBlocking("read");

        if (len == 0) {
            return 0;
        }

        final int availSize = file.readQueue.size();
        if (availSize == 0) {
            if (file.isEndOfStream) {
                // Java convention uses -1 to indicate end of stream.
                return -1;
            }
            return 0;  // EAGAIN
        }

        final int readCount = Math.min(len, availSize);
        file.readQueue.readBytes(buffer, pos, readCount);
        maybeTransferData(file);
        return readCount;
    }

    @Override
    public synchronized int write(FileDescriptor fd, byte[] buffer, int pos, int len)
            throws IOException {
        checkBoundaries("write", buffer, pos, len);

        File file = getFile("write", fd);
        if (file.writeQueue == null) {
            throw newIOException("read", "File not writable");
        }
        if (file.type == FileType.PIPE && file.sink.openCount == 0) {
            throw newIOException("write", "The other end of pipe is closed");
        }
        file.checkNonBlocking("write");

        if (len == 0) {
            return 0;
        }

        final int originalFreeSize = file.writeQueue.freeSize();
        if (originalFreeSize == 0) {
            return 0;  // EAGAIN
        }

        final int writeCount = Math.min(len, originalFreeSize);
        file.writeQueue.writeBytes(buffer, pos, writeCount);
        maybeTransferData(file);

        if (file.writeQueue.freeSize() < originalFreeSize) {
            final int additionalQueuedCount = originalFreeSize - file.writeQueue.freeSize();
            Log.i(mLogTag, logStr("Delaying transfer of " + additionalQueuedCount
                + " bytes, queued=" + file.writeQueue.size() + ", type=" + file.type
                + ", src_red=" + file.outboundLimiter + ", dst_red=" + file.sink.inboundLimiter));
        }

        return writeCount;
    }

    private void maybeTransferData(File file) {
        boolean hasChanges = copyFileBuffers(file, file.sink);
        hasChanges = copyFileBuffers(file.source, file) || hasChanges;

        if (hasChanges) {
            // TODO(b/245971639): Avoid notifying if no-one is polling.
            notifyAll();
        }
    }

    private boolean copyFileBuffers(File src, File dst) {
        if (src.writeQueue == null || dst.readQueue == null) {
            return false;
        }

        final int originalCopyCount = Math.min(mTmpBuffer.length,
            Math.min(src.writeQueue.size(), dst.readQueue.freeSize()));

        final int allowedCopyCount = RateLimiter.limit(
            src.outboundLimiter, dst.inboundLimiter, originalCopyCount);

        if (allowedCopyCount < originalCopyCount) {
            if (ENABLE_FINE_DEBUG) {
                Log.i(mLogTag, logStr("Delaying transfer of "
                    + (originalCopyCount - allowedCopyCount) + " bytes, original="
                    + originalCopyCount + ", allowed=" + allowedCopyCount
                    + ", type=" + src.type));
            }
            if (originalCopyCount > 0) {
                mHasRateLimitedData = true;
            }
            if (allowedCopyCount == 0) {
                return false;
            }
        }

        boolean hasChanges = false;
        if (allowedCopyCount > 0) {
            if (dst.readQueue.size() == 0 || src.writeQueue.freeSize() == 0) {
                hasChanges = true;  // Read queue had no data, or write queue was full.
            }
            src.writeQueue.readBytes(mTmpBuffer, 0, allowedCopyCount);
            dst.readQueue.writeBytes(mTmpBuffer, 0, allowedCopyCount);
        }

        if (!dst.isEndOfStream && src.openCount == 0
                && src.writeQueue.size() == 0 && dst.readQueue.size() == 0) {
            dst.isEndOfStream = true;
            hasChanges = true;
        }

        return hasChanges;
    }

    public void clearInboundRateLimit(FileDescriptor fd) {
        setInboundRateLimit(fd, Integer.MAX_VALUE);
    }

    public void clearOutboundRateLimit(FileDescriptor fd) {
        setOutboundRateLimit(fd, Integer.MAX_VALUE);
    }

    public synchronized void setInboundRateLimit(FileDescriptor fd, int bytesPerSecond) {
        File file = getFileOrNull(fd);
        if (file != null) {
            file.inboundLimiter.setBytesPerSecond(bytesPerSecond);
            maybeTransferData(file);
        }
    }

    public synchronized void setOutboundRateLimit(FileDescriptor fd, int bytesPerSecond) {
        File file = getFileOrNull(fd);
        if (file != null) {
            file.outboundLimiter.setBytesPerSecond(bytesPerSecond);
            maybeTransferData(file);
        }
    }

    public synchronized ParcelFileDescriptor[] socketpair() throws IOException {
        int fdNumber1 = getNextFd("socketpair");
        int fdNumber2 = getNextFd("socketpair");

        File file1 = new File(FileType.PAIR, mReadQueueSize, mWriteQueueSize);
        File file2 = new File(FileType.PAIR, mReadQueueSize, mWriteQueueSize);

        return registerFilePair(fdNumber1, file1, fdNumber2, file2);
    }

    @Override
    public synchronized ParcelFileDescriptor[] pipe() throws IOException {
        int fdNumber1 = getNextFd("pipe");
        int fdNumber2 = getNextFd("pipe");

        File file1 = new File(FileType.PIPE, mReadQueueSize, 0);
        File file2 = new File(FileType.PIPE, 0, mWriteQueueSize);

        return registerFilePair(fdNumber1, file1, fdNumber2, file2);
    }

    private ParcelFileDescriptor[] registerFilePair(
            int fdNumber1, File file1, int fdNumber2, File file2) {
        file1.sink = file2;
        file1.source = file2;
        file2.sink = file1;
        file2.source = file1;

        mFiles.put(fdNumber1, file1);
        mFiles.put(fdNumber2, file2);
        return new ParcelFileDescriptor[] {
            newParcelFileDescriptor(fdNumber1), newParcelFileDescriptor(fdNumber2)};
    }

    @Override
    public short getPollInMask() {
        return POLLIN;
    }

    @Override
    public short getPollOutMask() {
        return POLLOUT;
    }

    @Override
    public synchronized int poll(StructPollfd[] fds, int timeoutMs) throws IOException {
        if (timeoutMs < 0) {
            timeoutMs = (int) TimeUnit.HOURS.toMillis(1);  // Make "infinite" equal to 1 hour.
        }

        if (fds == null || fds.length > 1000) {
            throw newIOException("poll", "Invalid fds param");
        }
        for (StructPollfd pollFd : fds) {
            getFile("poll", pollFd.fd);
        }

        int waitCallCount = 0;
        final long deadline = monotonicTimeMillis() + timeoutMs;
        while (true) {
            if (mHasRateLimitedData) {
                mHasRateLimitedData = false;
                for (File file : mFiles.values()) {
                    if (file.inboundLimiter.getLastRequestReduction() != 0) {
                        copyFileBuffers(file.source, file);
                    }
                    if (file.outboundLimiter.getLastRequestReduction() != 0) {
                        copyFileBuffers(file, file.sink);
                    }
                }
            }

            final int readyCount = calculateReadyCount(fds);
            if (readyCount > 0) {
                if (ENABLE_FINE_DEBUG) {
                    Log.v(mLogTag, logStr("Poll returns " + readyCount
                            + " after " + waitCallCount + " wait calls"));
                }
                return readyCount;
            }

            long remainingTimeoutMs = deadline - monotonicTimeMillis();
            if (remainingTimeoutMs <= 0) {
                if (ENABLE_FINE_DEBUG) {
                    Log.v(mLogTag, logStr("Poll timeout " + timeoutMs
                            + "ms after " + waitCallCount + " wait calls"));
                }
                return 0;
            }

            if (mHasRateLimitedData) {
                remainingTimeoutMs = Math.min(RateLimiter.BUCKET_DURATION_MS, remainingTimeoutMs);
            }

            try {
                wait(remainingTimeoutMs);
            } catch (InterruptedException e) {
                // Ignore and retry
            }
            waitCallCount++;
        }
    }

    private int calculateReadyCount(StructPollfd[] fds) {
        int fdCount = 0;
        for (StructPollfd pollFd : fds) {
            pollFd.revents = 0;

            File file = getFileOrNull(pollFd.fd);
            if (file == null) {
                Log.w(mLogTag, logStr("Ignoring FD concurrently closed by a buggy app: "
                        + getFileDebugName(pollFd.fd)));
                continue;
            }

            if (ENABLE_FINE_DEBUG) {
                Log.v(mLogTag, logStr("calculateReadyCount fd=" + getFileDebugName(pollFd.fd)
                        + ", events=" + pollFd.events + ", eof=" + file.isEndOfStream
                        + ", r=" + (file.readQueue != null ? file.readQueue.size() : -1)
                        + ", w=" + (file.writeQueue != null ? file.writeQueue.freeSize() : -1)));
            }

            if ((pollFd.events & POLLIN) != 0) {
                if (file.readQueue != null && file.readQueue.size() != 0) {
                    pollFd.revents |= POLLIN;
                }
                if (file.isEndOfStream) {
                    pollFd.revents |= POLLHUP;
                }
            }

            if ((pollFd.events & POLLOUT) != 0) {
                if (file.type == FileType.PIPE && file.sink.openCount == 0) {
                    pollFd.revents |= POLLERR;
                }
                if (file.writeQueue != null && file.writeQueue.freeSize() != 0) {
                    pollFd.revents |= POLLOUT;
                }
            }

            if (pollFd.revents != 0) {
                fdCount++;
            }
        }
        return fdCount;
    }

    private int getNextFd(String func) throws IOException {
        if (mFileNumberGen > 100000) {
            throw newIOException(func, "Too many files open");
        }

        return mFileNumberGen++;
    }

    private static IOException newIOException(String func, String message) {
        return new IOException(message + ", func=" + func);
    }

    public static void checkBoundaries(String func, byte[] buffer, int pos, int len)
            throws IOException {
        if (((buffer.length | pos | len) < 0 || pos > buffer.length - len)) {
            throw newIOException(func, "Invalid array bounds");
        }
    }

    private ParcelFileDescriptor newParcelFileDescriptor(int fdNumber) {
        try {
            return new ParcelFileDescriptor(newFileDescriptor(fdNumber));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FileDescriptor newFileDescriptor(int fdNumber) {
        try {
            return FD_CONSTRUCTOR.newInstance(Integer.valueOf(fdNumber));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getFileDescriptorNumber(FileDescriptor fd) {
        try {
            return (Integer) FD_FIELD_DESCRIPTOR.get(fd);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setFileDescriptorNumber(FileDescriptor fd, int fdNumber) {
        try {
            FD_FIELD_DESCRIPTOR.set(fd, Integer.valueOf(fdNumber));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String logStr(String message) {
        return "[FakeOs " + (monotonicTimeMillis() - mStartTime) + "] " + message;
    }

    private class File {
        final FileType type;
        final CircularByteBuffer readQueue;
        final CircularByteBuffer writeQueue;
        final RateLimiter inboundLimiter = new RateLimiter(FakeOsAccess.this, Integer.MAX_VALUE);
        final RateLimiter outboundLimiter = new RateLimiter(FakeOsAccess.this, Integer.MAX_VALUE);
        String name;
        int openCount = 1;
        boolean isBlocking = true;
        File sink;
        File source;
        boolean isEndOfStream;

        File(FileType type, int readQueueSize, int writeQueueSize) {
            this.type = type;
            readQueue = (readQueueSize > 0 ? new CircularByteBuffer(readQueueSize) : null);
            writeQueue = (writeQueueSize > 0 ? new CircularByteBuffer(writeQueueSize) : null);
        }

        void decreaseRefCount() {
            if (openCount <= 0) {
                throw new IllegalStateException();
            }
            openCount--;
        }

        void checkNonBlocking(String func) throws IOException {
            if (isBlocking) {
                throw newIOException(func, "File in blocking mode");
            }
        }
    }

    static {
        try {
            FD_CONSTRUCTOR = FileDescriptor.class.getDeclaredConstructor(int.class);
            FD_CONSTRUCTOR.setAccessible(true);

            Field descriptorIntField;
            try {
                descriptorIntField = FileDescriptor.class.getDeclaredField("descriptor");
            } catch (NoSuchFieldException e) {
                descriptorIntField = FileDescriptor.class.getDeclaredField("fd");
            }
            FD_FIELD_DESCRIPTOR = descriptorIntField;
            FD_FIELD_DESCRIPTOR.setAccessible(true);

            PFD_FIELD_DESCRIPTOR = ParcelFileDescriptor.class.getDeclaredField("mFd");
            PFD_FIELD_DESCRIPTOR.setAccessible(true);

            PFD_FIELD_GUARD = ParcelFileDescriptor.class.getDeclaredField("mGuard");
            PFD_FIELD_GUARD.setAccessible(true);

            CLOSE_GUARD_METHOD_CLOSE = Class.forName("dalvik.system.CloseGuard")
                .getDeclaredMethod("close");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

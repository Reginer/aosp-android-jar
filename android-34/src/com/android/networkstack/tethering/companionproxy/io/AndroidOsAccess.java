package com.android.networkstack.tethering.companionproxy.io;

import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.system.StructPollfd;

import com.android.networkstack.tethering.companionproxy.util.Assertions;

import java.io.FileDescriptor;
import java.io.InterruptedIOException;
import java.io.IOException;

/**
 * Implements OsAccess for Android OS at runtime.
 *
 * @hide
 */
public final class AndroidOsAccess extends OsAccess {
    @Override
    public long monotonicTimeMillis() {
        return System.nanoTime() / 1000000;
    }

    @Override
    public FileDescriptor getInnerFileDescriptor(ParcelFileDescriptor fd) {
        return fd.getFileDescriptor();
    }

    @Override
    public String getFileDebugName(ParcelFileDescriptor fd) {
        if (fd == null) {
            return "null";
        }
        try {
            return Integer.toString(fd.getFd());
        } catch (IllegalStateException e) {
            return "closed";
        }
    }

    @Override
    public void close(ParcelFileDescriptor fd) {
        try {
            // Android version of close() works correctly.
            // Tests use a different implementation.
            fd.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public int read(FileDescriptor fd, byte[] buffer, int pos, int len)
            throws IOException {
        Assertions.throwsIfOutOfBounds(buffer.length, pos, len);
        if (len == 0) {
            return 0;
        }
        try {
            int readCount = android.system.Os.read(fd, buffer, pos, len);
            if (readCount == 0) {
                // Java convention uses -1 to indicate end of stream.
                return -1;
            }
            return readCount;
        } catch (ErrnoException e) {
            if (e.errno == OsConstants.EAGAIN) {
                return 0;
            }
            throw e.rethrowAsIOException();
        }
    }

    @Override
    public int write(FileDescriptor fd, byte[] buffer, int pos, int len)
            throws IOException {
        Assertions.throwsIfOutOfBounds(buffer.length, pos, len);
        if (len == 0) {
            return 0;
        }
        try {
            return android.system.Os.write(fd, buffer, pos, len);
        } catch (ErrnoException e) {
            if (e.errno == OsConstants.EAGAIN) {
                return 0;
            }
            throw e.rethrowAsIOException();
        }
    }

    @Override
    public void setNonBlocking(FileDescriptor fd) throws IOException {
        try {
            int flags = android.system.Os.fcntlInt(fd, OsConstants.F_GETFL, 0);
            if (flags < 0) {
                throw new ErrnoException("Unable to get FD flags", OsConstants.EINVAL);
            }

            flags = flags | OsConstants.O_NONBLOCK | OsConstants.FD_CLOEXEC;

            if (android.system.Os.fcntlInt(fd, OsConstants.F_SETFL, flags) != 0) {
                throw new ErrnoException("Unable to set FD flags", OsConstants.EINVAL);
            }
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    @Override
    public ParcelFileDescriptor[] pipe() throws IOException {
        FileDescriptor[] fds = null;
        try {
            fds = android.system.Os.pipe();
            if (fds == null) {
                throw new IOException("Os.pipe() returned null");
            }

            ParcelFileDescriptor[] result = new ParcelFileDescriptor[2];
            result[0] = ParcelFileDescriptor.dup(fds[0]);
            result[1] = ParcelFileDescriptor.dup(fds[1]);
            return result;
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        } finally {
            if (fds != null) {
                close(fds[0]);
                close(fds[1]);
            }
        }
    }

    private static void close(FileDescriptor fd) {
        if (fd != null) {
            try {
                android.system.Os.close(fd);
            } catch (ErrnoException e) {
                // ignore
            }
        }
    }

    @Override
    public int poll(StructPollfd[] fds, int timeoutMs) throws IOException {
        try {
            return android.system.Os.poll(fds, timeoutMs);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    @Override
    public short getPollInMask() {
        return (short) OsConstants.POLLIN;
    }

    @Override
    public short getPollOutMask() {
        return (short) OsConstants.POLLOUT;
    }
}

package com.android.networkstack.tethering.companionproxy.io;

import android.os.ParcelFileDescriptor;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents an file descriptor or another way to access a file.
 *
 * @hide
 */
public final class FileHandle {
    private final ParcelFileDescriptor mFd;
    private final Closeable mCloseable;
    private final InputStream mInputStream;
    private final OutputStream mOutputStream;

    public static FileHandle fromFileDescriptor(ParcelFileDescriptor fd) {
        if (fd == null) {
            throw new NullPointerException();
        }
        return new FileHandle(fd, null, null, null);
    }

    public static FileHandle fromBlockingStream(
            Closeable closeable, InputStream is, OutputStream os) {
        if (closeable == null || is == null || os == null) {
            throw new NullPointerException();
        }
        return new FileHandle(null, closeable, is, os);
    }

    private FileHandle(ParcelFileDescriptor fd, Closeable closeable,
            InputStream is, OutputStream os) {
        mFd = fd;
        mCloseable = closeable;
        mInputStream = is;
        mOutputStream = os;
    }

    ParcelFileDescriptor getFileDescriptor() {
        return mFd;
    }

    Closeable getCloseable() {
        return mCloseable;
    }

    InputStream getInputStream() {
        return mInputStream;
    }

    OutputStream getOutputStream() {
        return mOutputStream;
    }
}

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

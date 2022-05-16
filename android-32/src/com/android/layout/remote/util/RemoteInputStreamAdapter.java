/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.layout.remote.util;

import com.android.tools.layoutlib.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteInputStreamAdapter implements RemoteInputStream {

    private InputStream mDelegate;

    private RemoteInputStreamAdapter(@NotNull InputStream delegate) {
        mDelegate = delegate;
    }

    public static RemoteInputStream create(@NotNull InputStream is) throws RemoteException {
        return (RemoteInputStream) UnicastRemoteObject.exportObject(
                new RemoteInputStreamAdapter(is), 0);
    }

    @Override
    public int read() throws IOException {
        return mDelegate.read();
    }

    @Override
    public byte[] read(int off, int len) throws IOException, RemoteException {
        byte[] buffer = new byte[len];
        if (mDelegate.read(buffer, off, len) == -1) {
            throw new EndOfStreamException();
        }
        return buffer;
    }

    @Override
    public long skip(long n) throws IOException {
        return mDelegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        return mDelegate.available();
    }

    @Override
    public void close() throws IOException {
        mDelegate.close();
    }

    @Override
    public void mark(int readlimit) {
        mDelegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        mDelegate.reset();
    }

    @Override
    public boolean markSupported() {
        return mDelegate.markSupported();
    }
}

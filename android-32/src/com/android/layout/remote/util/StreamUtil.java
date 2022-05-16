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

import com.android.layout.remote.util.RemoteInputStream.EndOfStreamException;
import com.android.tools.layoutlib.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;

public class StreamUtil {
    /**
     * Returns a local {@link InputStream} from a {@link RemoteInputStream}
     */
    @NotNull
    public static InputStream getInputStream(@NotNull RemoteInputStream is) {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return is.read();
            }

            @SuppressWarnings("NullableProblems")
            @Override
            public int read(byte[] b) throws IOException {
                return read(b, 0, b.length);
            }

            @SuppressWarnings("NullableProblems")
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                try {
                    byte[] read = is.read(off, len);
                    int actualLength = Math.min(len, read.length);
                    System.arraycopy(read, 0, b, off, actualLength);
                    return actualLength;
                } catch (EndOfStreamException e) {
                    return -1;
                }
            }

            @Override
            public long skip(long n) throws IOException {
                return is.skip(n);
            }

            @Override
            public int available() throws IOException {
                return is.available();
            }

            @Override
            public void close() throws IOException {
                is.close();
            }

            @Override
            public synchronized void mark(int readlimit) {
                try {
                    is.mark(readlimit);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public synchronized void reset() throws IOException {
                is.reset();
            }

            @Override
            public boolean markSupported() {
                try {
                    return is.markSupported();
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}

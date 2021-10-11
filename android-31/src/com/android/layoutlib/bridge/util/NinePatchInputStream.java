/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.layoutlib.bridge.util;

import com.android.tools.layoutlib.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simpler wrapper around FileInputStream. This is used when the input stream represent
 * not a normal bitmap but a nine patch.
 * This is useful when the InputStream is created in a method but used in another that needs
 * to know whether this is 9-patch or not, such as BitmapFactory.
 */
public class NinePatchInputStream extends InputStream {
    private final InputStream mDelegate;
    private boolean mFakeMarkSupport = true;

    public NinePatchInputStream(File file) throws FileNotFoundException {
        mDelegate = new FileInputStream(file);
    }

    public NinePatchInputStream(@NotNull InputStream stream) {
        mDelegate = stream;
    }

    @Override
    public boolean markSupported() {
        // this is needed so that BitmapFactory doesn't wrap this in a BufferedInputStream.
        return mFakeMarkSupport || mDelegate.markSupported();
    }

    public void disableFakeMarkSupport() {
        // disable fake mark support so that in case codec actually try to use them
        // we don't lie to them.
        mFakeMarkSupport = false;
    }

    @Override
    public int read() throws IOException {
        return mDelegate.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return mDelegate.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return mDelegate.read(b, off, len);
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
}

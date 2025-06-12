/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.graphics;

import android.compat.annotation.UnsupportedAppUsage;
import android.content.res.AssetManager;
import android.os.Build;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @deprecated Prefer {@link android.graphics.drawable.AnimatedImageDrawable}.
 */
@Deprecated
public class Movie {
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private long mNativeMovie;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private Movie(long nativeMovie) {
        if (nativeMovie == 0) {
            throw new RuntimeException("native movie creation failed");
        }
        mNativeMovie = nativeMovie;
    }

    public native int width();
    public native int height();
    public native boolean isOpaque();
    public native int duration();

    public native boolean setTime(int relativeMilliseconds);

    private native void nDraw(long nativeCanvas, float x, float y, long paintHandle);

    public void draw(Canvas canvas, float x, float y, Paint paint) {
        nDraw(canvas.getNativeCanvasWrapper(), x, y,
                paint != null ? paint.getNativeInstance() : 0);
    }

    public void draw(Canvas canvas, float x, float y) {
        nDraw(canvas.getNativeCanvasWrapper(), x, y, 0);
    }

    public static Movie decodeStream(InputStream is) {
        if (is == null) {
            return null;
        }
        if (is instanceof AssetManager.AssetInputStream) {
            final long asset = ((AssetManager.AssetInputStream) is).getNativeAsset();
            return nativeDecodeAsset(asset);
        }

        return nativeDecodeStream(is);
    }

    private static native Movie nativeDecodeAsset(long asset);
    private static native Movie nativeDecodeStream(InputStream is);
    public static native Movie decodeByteArray(byte[] data, int offset,
                                               int length);

    private static native void nativeDestructor(long nativeMovie);

    public static Movie decodeFile(String pathName) {
        InputStream is;
        try {
            is = new FileInputStream(pathName);
        }
        catch (java.io.FileNotFoundException e) {
            return null;
        }
        return decodeTempStream(is);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            nativeDestructor(mNativeMovie);
            mNativeMovie = 0;
        } finally {
            super.finalize();
        }
    }

    private static Movie decodeTempStream(InputStream is) {
        Movie moov = null;
        try {
            moov = decodeStream(is);
            is.close();
        }
        catch (java.io.IOException e) {
            /*  do nothing.
                If the exception happened on open, moov will be null.
                If it happened on close, moov is still valid.
            */
        }
        return moov;
    }
}

/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.annotation.NonNull;
import android.compat.annotation.UnsupportedAppUsage;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A Picture records drawing calls (via the canvas returned by beginRecording)
 * and can then play them back into Canvas (via {@link Picture#draw(Canvas)} or
 * {@link Canvas#drawPicture(Picture)}).For most content (e.g. text, lines, rectangles),
 * drawing a sequence from a picture can be faster than the equivalent API
 * calls, since the picture performs its playback without incurring any
 * method-call overhead.
 *
 * <p class="note"><strong>Note:</strong> Prior to API level 23 a picture cannot
 * be replayed on a hardware accelerated canvas.</p>
 */
public class Picture {
    private PictureCanvas mRecordingCanvas;
    // TODO: Figure out if this was a false-positive
    @UnsupportedAppUsage(maxTargetSdk = 28)
    private long mNativePicture;
    private boolean mRequiresHwAcceleration;

    private static final int WORKING_STREAM_STORAGE = 16 * 1024;

    /**
     * Creates an empty picture that is ready to record.
     */
    public Picture() {
        this(nativeConstructor(0));
    }

    /**
     * Create a picture by making a copy of what has already been recorded in
     * src. The contents of src are unchanged, and if src changes later, those
     * changes will not be reflected in this picture.
     */
    public Picture(Picture src) {
        this(nativeConstructor(src != null ? src.mNativePicture : 0));
    }

    /** @hide */
    public Picture(long nativePicture) {
        if (nativePicture == 0) {
            throw new IllegalArgumentException();
        }
        mNativePicture = nativePicture;
    }

    /**
     * Immediately releases the backing data of the Picture. This object will no longer
     * be usable after calling this, and any further calls on the Picture will throw an
     * IllegalStateException.
     * // TODO: Support?
     * @hide
     */
    public void close() {
        if (mNativePicture != 0) {
            nativeDestructor(mNativePicture);
            mNativePicture = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private void verifyValid() {
        if (mNativePicture == 0) {
            throw new IllegalStateException("Picture is destroyed");
        }
    }

    /**
     * To record a picture, call beginRecording() and then draw into the Canvas
     * that is returned. Nothing we appear on screen, but all of the draw
     * commands (e.g. {@link Canvas#drawRect(Rect, Paint)}) will be recorded.
     * To stop recording, call endRecording(). After endRecording() the Canvas
     * that was returned must no longer be used, and nothing should be drawn
     * into it.
     */
    @NonNull
    public Canvas beginRecording(int width, int height) {
        verifyValid();
        if (mRecordingCanvas != null) {
            throw new IllegalStateException("Picture already recording, must call #endRecording()");
        }
        long ni = nativeBeginRecording(mNativePicture, width, height);
        mRecordingCanvas = new PictureCanvas(this, ni);
        mRequiresHwAcceleration = false;
        return mRecordingCanvas;
    }

    /**
     * Call endRecording when the picture is built. After this call, the picture
     * may be drawn, but the canvas that was returned by beginRecording must not
     * be used anymore. This is automatically called if {@link Picture#draw}
     * or {@link Canvas#drawPicture(Picture)} is called.
     */
    public void endRecording() {
        verifyValid();
        if (mRecordingCanvas != null) {
            mRequiresHwAcceleration = mRecordingCanvas.mUsesHwFeature;
            mRecordingCanvas = null;
            nativeEndRecording(mNativePicture);
        }
    }

    /**
     * Get the width of the picture as passed to beginRecording. This
     * does not reflect (per se) the content of the picture.
     */
    public int getWidth() {
        verifyValid();
        return nativeGetWidth(mNativePicture);
    }

    /**
     * Get the height of the picture as passed to beginRecording. This
     * does not reflect (per se) the content of the picture.
     */
    public int getHeight() {
        verifyValid();
        return nativeGetHeight(mNativePicture);
    }

    /**
     * Indicates whether or not this Picture contains recorded commands that only work when
     * drawn to a hardware-accelerated canvas. If this returns true then this Picture can only
     * be drawn to another Picture or to a Canvas where canvas.isHardwareAccelerated() is true.
     *
     * Note this value is only updated after recording has finished by a call to
     * {@link #endRecording()}. Prior to that it will be the default value of false.
     *
     * @return true if the Picture can only be drawn to a hardware-accelerated canvas,
     *         false otherwise.
     */
    public boolean requiresHardwareAcceleration() {
        verifyValid();
        return mRequiresHwAcceleration;
    }

    /**
     * Draw this picture on the canvas.
     * <p>
     * Prior to {@link android.os.Build.VERSION_CODES#LOLLIPOP}, this call could
     * have the side effect of changing the matrix and clip of the canvas
     * if this picture had imbalanced saves/restores.
     *
     * <p>
     * <strong>Note:</strong> This forces the picture to internally call
     * {@link Picture#endRecording()} in order to prepare for playback.
     *
     * @param canvas  The picture is drawn to this canvas
     */
    public void draw(@NonNull Canvas canvas) {
        verifyValid();
        if (mRecordingCanvas != null) {
            endRecording();
        }
        if (mRequiresHwAcceleration && !canvas.isHardwareAccelerated()
                && canvas.onHwFeatureInSwMode()) {
            throw new IllegalArgumentException("Software rendering not supported for Pictures that"
                    + " require hardware acceleration");
        }
        nativeDraw(canvas.getNativeCanvasWrapper(), mNativePicture);
    }

    /**
     * Create a new picture (already recorded) from the data in the stream. This
     * data was generated by a previous call to writeToStream(). Pictures that
     * have been persisted across device restarts are not guaranteed to decode
     * properly and are highly discouraged.
     *
     * @see #writeToStream(java.io.OutputStream)
     * @removed
     * @deprecated The recommended alternative is to not use writeToStream and
     * instead draw the picture into a Bitmap from which you can persist it as
     * raw or compressed pixels.
     */
    @Deprecated
    public static Picture createFromStream(@NonNull InputStream stream) {
        return new Picture(nativeCreateFromStream(stream, new byte[WORKING_STREAM_STORAGE]));
    }

    /**
     * Write the picture contents to a stream. The data can be used to recreate
     * the picture in this or another process by calling createFromStream(...)
     * The resulting stream is NOT to be persisted across device restarts as
     * there is no guarantee that the Picture can be successfully reconstructed.
     *
     * @see #createFromStream(java.io.InputStream)
     * @removed
     * @deprecated The recommended alternative is to draw the picture into a
     * Bitmap from which you can persist it as raw or compressed pixels.
     */
    @Deprecated
    public void writeToStream(@NonNull OutputStream stream) {
        verifyValid();
        // do explicit check before calling the native method
        if (stream == null) {
            throw new IllegalArgumentException("stream cannot be null");
        }
        if (!nativeWriteToStream(mNativePicture, stream, new byte[WORKING_STREAM_STORAGE])) {
            throw new RuntimeException();
        }
    }

    // return empty picture if src is 0, or a copy of the native src
    private static native long nativeConstructor(long nativeSrcOr0);
    private static native long nativeCreateFromStream(InputStream stream, byte[] storage);
    private static native int nativeGetWidth(long nativePicture);
    private static native int nativeGetHeight(long nativePicture);
    private static native long nativeBeginRecording(long nativeCanvas, int w, int h);
    private static native void nativeEndRecording(long nativeCanvas);
    private static native void nativeDraw(long nativeCanvas, long nativePicture);
    private static native boolean nativeWriteToStream(long nativePicture,
                                           OutputStream stream, byte[] storage);
    private static native void nativeDestructor(long nativePicture);

    private static class PictureCanvas extends Canvas {
        private final Picture mPicture;
        boolean mUsesHwFeature;

        public PictureCanvas(Picture pict, long nativeCanvas) {
            super(nativeCanvas);
            mPicture = pict;
            // Disable bitmap density scaling. This matches RecordingCanvas.
            mDensity = 0;
        }

        @Override
        public void setBitmap(Bitmap bitmap) {
            throw new RuntimeException("Cannot call setBitmap on a picture canvas");
        }

        @Override
        public void drawPicture(Picture picture) {
            if (mPicture == picture) {
                throw new RuntimeException("Cannot draw a picture into its recording canvas");
            }
            super.drawPicture(picture);
        }

        @Override
        protected boolean onHwFeatureInSwMode() {
            mUsesHwFeature = true;
            return false;
        }
    }
}

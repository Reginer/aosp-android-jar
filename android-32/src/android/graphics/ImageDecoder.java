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

package android.graphics;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.res.AssetManager.AssetInputStream;
import android.content.res.Resources;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Size;
import android.util.TypedValue;

import java.nio.ByteBuffer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ArrayIndexOutOfBoundsException;
import java.lang.AutoCloseable;
import java.lang.NullPointerException;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 *  Class for decoding images as {@link Bitmap}s or {@link Drawable}s.
 */
public final class ImageDecoder implements AutoCloseable {

    /**
     *  Source of the encoded image data.
     */
    public static abstract class Source {
        private Source() {}

        /* @hide */
        @Nullable
        Resources getResources() { return null; }

        /* @hide */
        int getDensity() { return Bitmap.DENSITY_NONE; }

        /* @hide */
        int computeDstDensity() {
            Resources res = getResources();
            if (res == null) {
                return Bitmap.getDefaultDensity();
            }

            return res.getDisplayMetrics().densityDpi;
        }

        /* @hide */
        @NonNull
        abstract ImageDecoder createImageDecoder() throws IOException;
    };

    private static class ByteArraySource extends Source {
        ByteArraySource(@NonNull byte[] data, int offset, int length) {
            mData = data;
            mOffset = offset;
            mLength = length;
        };
        private final byte[] mData;
        private final int    mOffset;
        private final int    mLength;

        @Override
        public ImageDecoder createImageDecoder() throws IOException {
            return new ImageDecoder();
        }
    }

    private static class ByteBufferSource extends Source {
        ByteBufferSource(@NonNull ByteBuffer buffer) {
            mBuffer = buffer;
        }
        private final ByteBuffer mBuffer;

        @Override
        public ImageDecoder createImageDecoder() throws IOException {
            return new ImageDecoder();
        }
    }

    private static class ContentResolverSource extends Source {
        ContentResolverSource(@NonNull ContentResolver resolver, @NonNull Uri uri) {
            mResolver = resolver;
            mUri = uri;
        }

        private final ContentResolver mResolver;
        private final Uri mUri;

        @Override
        public ImageDecoder createImageDecoder() throws IOException {
            return new ImageDecoder();
        }
    }

    /**
     * For backwards compatibility, this does *not* close the InputStream.
     */
    private static class InputStreamSource extends Source {
        InputStreamSource(Resources res, InputStream is, int inputDensity) {
            if (is == null) {
                throw new IllegalArgumentException("The InputStream cannot be null");
            }
            mResources = res;
            mInputStream = is;
            mInputDensity = res != null ? inputDensity : Bitmap.DENSITY_NONE;
        }

        final Resources mResources;
        InputStream mInputStream;
        final int mInputDensity;

        @Override
        public Resources getResources() { return mResources; }

        @Override
        public int getDensity() { return mInputDensity; }

        @Override
        public ImageDecoder createImageDecoder() throws IOException {
            return new ImageDecoder();
        }
    }

    /**
     * Takes ownership of the AssetInputStream.
     *
     * @hide
     */
    public static class AssetInputStreamSource extends Source {
        public AssetInputStreamSource(@NonNull AssetInputStream ais,
                @NonNull Resources res, @NonNull TypedValue value) {
            mAssetInputStream = ais;
            mResources = res;

            if (value.density == TypedValue.DENSITY_DEFAULT) {
                mDensity = DisplayMetrics.DENSITY_DEFAULT;
            } else if (value.density != TypedValue.DENSITY_NONE) {
                mDensity = value.density;
            } else {
                mDensity = Bitmap.DENSITY_NONE;
            }
        }

        private AssetInputStream mAssetInputStream;
        private final Resources  mResources;
        private final int        mDensity;

        @Override
        public Resources getResources() { return mResources; }

        @Override
        public int getDensity() {
            return mDensity;
        }

        @Override
        public ImageDecoder createImageDecoder() throws IOException {
            return new ImageDecoder();
        }
    }

    private static class ResourceSource extends Source {
        ResourceSource(@NonNull Resources res, int resId) {
            mResources = res;
            mResId = resId;
            mResDensity = Bitmap.DENSITY_NONE;
        }

        final Resources mResources;
        final int       mResId;
        int             mResDensity;

        @Override
        public Resources getResources() { return mResources; }

        @Override
        public int getDensity() { return mResDensity; }

        @Override
        public ImageDecoder createImageDecoder() throws IOException {
            return new ImageDecoder();
        }
    }

    private static class FileSource extends Source {
        FileSource(@NonNull File file) {
            mFile = file;
        }

        private final File mFile;

        @Override
        public ImageDecoder createImageDecoder() throws IOException {
            return new ImageDecoder();
        }
    }

    /**
     *  Contains information about the encoded image.
     */
    public static class ImageInfo {
        private ImageDecoder mDecoder;

        private ImageInfo(@NonNull ImageDecoder decoder) {
            mDecoder = decoder;
        }

        /**
         * Size of the image, without scaling or cropping.
         */
        @NonNull
        public Size getSize() {
            return new Size(0, 0);
        }

        /**
         * The mimeType of the image.
         */
        @NonNull
        public String getMimeType() {
            return "";
        }

        /**
         * Whether the image is animated.
         *
         * <p>Calling {@link #decodeDrawable} will return an
         * {@link AnimatedImageDrawable}.</p>
         */
        public boolean isAnimated() {
            return mDecoder.mAnimated;
        }
    };

    /**
     *  Thrown if the provided data is incomplete.
     */
    public static class IncompleteException extends IOException {};

    /**
     *  Optional listener supplied to {@link #decodeDrawable} or
     *  {@link #decodeBitmap}.
     */
    public interface OnHeaderDecodedListener {
        /**
         *  Called when the header is decoded and the size is known.
         *
         *  @param decoder allows changing the default settings of the decode.
         *  @param info Information about the encoded image.
         *  @param source that created the decoder.
         */
        void onHeaderDecoded(@NonNull ImageDecoder decoder,
                @NonNull ImageInfo info, @NonNull Source source);

    };

    /**
     *  An Exception was thrown reading the {@link Source}.
     */
    public static final int ERROR_SOURCE_EXCEPTION  = 1;

    /**
     *  The encoded data was incomplete.
     */
    public static final int ERROR_SOURCE_INCOMPLETE = 2;

    /**
     *  The encoded data contained an error.
     */
    public static final int ERROR_SOURCE_ERROR      = 3;

    @Retention(SOURCE)
    public @interface Error {}

    /**
     *  Optional listener supplied to the ImageDecoder.
     *
     *  Without this listener, errors will throw {@link java.io.IOException}.
     */
    public interface OnPartialImageListener {
        /**
         *  Called when there is only a partial image to display.
         *
         *  If decoding is interrupted after having decoded a partial image,
         *  this listener lets the client know that and allows them to
         *  optionally finish the rest of the decode/creation process to create
         *  a partial {@link Drawable}/{@link Bitmap}.
         *
         *  @param error indicating what interrupted the decode.
         *  @param source that had the error.
         *  @return True to create and return a {@link Drawable}/{@link Bitmap}
         *      with partial data. False (which is the default) to abort the
         *      decode and throw {@link java.io.IOException}.
         */
        boolean onPartialImage(@Error int error, @NonNull Source source);
    }

    private boolean mAnimated;
    private Rect mOutPaddingRect;

    public ImageDecoder() {
        mAnimated = true; // This is too avoid throwing an exception in AnimatedImageDrawable
    }

    /**
     * Create a new {@link Source} from an asset.
     * @hide
     *
     * @param res the {@link Resources} object containing the image data.
     * @param resId resource ID of the image data.
     *      // FIXME: Can be an @DrawableRes?
     * @return a new Source object, which can be passed to
     *      {@link #decodeDrawable} or {@link #decodeBitmap}.
     */
    @NonNull
    public static Source createSource(@NonNull Resources res, int resId)
    {
        return new ResourceSource(res, resId);
    }

    /**
     * Create a new {@link Source} from a {@link android.net.Uri}.
     *
     * @param cr to retrieve from.
     * @param uri of the image file.
     * @return a new Source object, which can be passed to
     *      {@link #decodeDrawable} or {@link #decodeBitmap}.
     */
    @NonNull
    public static Source createSource(@NonNull ContentResolver cr,
            @NonNull Uri uri) {
        return new ContentResolverSource(cr, uri);
    }

    /**
     * Create a new {@link Source} from a byte array.
     *
     * @param data byte array of compressed image data.
     * @param offset offset into data for where the decoder should begin
     *      parsing.
     * @param length number of bytes, beginning at offset, to parse.
     * @throws NullPointerException if data is null.
     * @throws ArrayIndexOutOfBoundsException if offset and length are
     *      not within data.
     * @hide
     */
    @NonNull
    public static Source createSource(@NonNull byte[] data, int offset,
            int length) throws ArrayIndexOutOfBoundsException {
        if (offset < 0 || length < 0 || offset >= data.length ||
                offset + length > data.length) {
            throw new ArrayIndexOutOfBoundsException(
                    "invalid offset/length!");
        }
        return new ByteArraySource(data, offset, length);
    }

    /**
     * See {@link #createSource(byte[], int, int).
     * @hide
     */
    @NonNull
    public static Source createSource(@NonNull byte[] data) {
        return createSource(data, 0, data.length);
    }

    /**
     * Create a new {@link Source} from a {@link java.nio.ByteBuffer}.
     *
     * <p>The returned {@link Source} effectively takes ownership of the
     * {@link java.nio.ByteBuffer}; i.e. no other code should modify it after
     * this call.</p>
     *
     * Decoding will start from {@link java.nio.ByteBuffer#position()}. The
     * position after decoding is undefined.
     */
    @NonNull
    public static Source createSource(@NonNull ByteBuffer buffer) {
        return new ByteBufferSource(buffer);
    }

    /**
     * Internal API used to generate bitmaps for use by Drawables (i.e. BitmapDrawable)
     * @hide
     */
    public static Source createSource(Resources res, InputStream is) {
        return new InputStreamSource(res, is, Bitmap.getDefaultDensity());
    }

    /**
     * Internal API used to generate bitmaps for use by Drawables (i.e. BitmapDrawable)
     * @hide
     */
    public static Source createSource(Resources res, InputStream is, int density) {
        return new InputStreamSource(res, is, density);
    }

    /**
     * Create a new {@link Source} from a {@link java.io.File}.
     */
    @NonNull
    public static Source createSource(@NonNull File file) {
        return new FileSource(file);
    }

    /**
     *  Return the width and height of a given sample size.
     *
     *  <p>This takes an input that functions like
     *  {@link BitmapFactory.Options#inSampleSize}. It returns a width and
     *  height that can be acheived by sampling the encoded image. Other widths
     *  and heights may be supported, but will require an additional (internal)
     *  scaling step. Such internal scaling is *not* supported with
     *  {@link #setRequireUnpremultiplied} set to {@code true}.</p>
     *
     *  @param sampleSize Sampling rate of the encoded image.
     *  @return {@link android.util.Size} of the width and height after
     *      sampling.
     */
    @NonNull
    public Size getSampledSize(int sampleSize) {
        return new Size(0, 0);
    }

    // Modifiers
    /**
     *  Resize the output to have the following size.
     *
     *  @param width must be greater than 0.
     *  @param height must be greater than 0.
     */
    public void setResize(int width, int height) {
    }

    /**
     *  Resize based on a sample size.
     *
     *  <p>This has the same effect as passing the result of
     *  {@link #getSampledSize} to {@link #setResize(int, int)}.</p>
     *
     *  @param sampleSize Sampling rate of the encoded image.
     */
    public void setResize(int sampleSize) {
    }

    // These need to stay in sync with ImageDecoder.cpp's Allocator enum.
    /**
     *  Use the default allocation for the pixel memory.
     *
     *  Will typically result in a {@link Bitmap.Config#HARDWARE}
     *  allocation, but may be software for small images. In addition, this will
     *  switch to software when HARDWARE is incompatible, e.g.
     *  {@link #setMutable}, {@link #setAsAlphaMask}.
     */
    public static final int ALLOCATOR_DEFAULT = 0;

    /**
     *  Use a software allocation for the pixel memory.
     *
     *  Useful for drawing to a software {@link Canvas} or for
     *  accessing the pixels on the final output.
     */
    public static final int ALLOCATOR_SOFTWARE = 1;

    /**
     *  Use shared memory for the pixel memory.
     *
     *  Useful for sharing across processes.
     */
    public static final int ALLOCATOR_SHARED_MEMORY = 2;

    /**
     *  Require a {@link Bitmap.Config#HARDWARE} {@link Bitmap}.
     *
     *  When this is combined with incompatible options, like
     *  {@link #setMutable} or {@link #setAsAlphaMask}, {@link #decodeDrawable}
     *  / {@link #decodeBitmap} will throw an
     *  {@link java.lang.IllegalStateException}.
     */
    public static final int ALLOCATOR_HARDWARE = 3;

    /** @hide **/
    @Retention(SOURCE)
    public @interface Allocator {};

    /**
     *  Choose the backing for the pixel memory.
     *
     *  This is ignored for animated drawables.
     *
     *  @param allocator Type of allocator to use.
     */
    public void setAllocator(@Allocator int allocator) { }

    /**
     *  Specify whether the {@link Bitmap} should have unpremultiplied pixels.
     *
     *  By default, ImageDecoder will create a {@link Bitmap} with
     *  premultiplied pixels, which is required for drawing with the
     *  {@link android.view.View} system (i.e. to a {@link Canvas}). Calling
     *  this method with a value of {@code true} will result in
     *  {@link #decodeBitmap} returning a {@link Bitmap} with unpremultiplied
     *  pixels. See {@link Bitmap#isPremultiplied}. This is incompatible with
     *  {@link #decodeDrawable}; attempting to decode an unpremultiplied
     *  {@link Drawable} will throw an {@link java.lang.IllegalStateException}.
     */
    public ImageDecoder setRequireUnpremultiplied(boolean requireUnpremultiplied) {
        return this;
    }

    /**
     *  Modify the image after decoding and scaling.
     *
     *  <p>This allows adding effects prior to returning a {@link Drawable} or
     *  {@link Bitmap}. For a {@code Drawable} or an immutable {@code Bitmap},
     *  this is the only way to process the image after decoding.</p>
     *
     *  <p>If set on a nine-patch image, the nine-patch data is ignored.</p>
     *
     *  <p>For an animated image, the drawing commands drawn on the
     *  {@link Canvas} will be recorded immediately and then applied to each
     *  frame.</p>
     */
    public void setPostProcessor(@Nullable PostProcessor p) { }

    /**
     *  Set (replace) the {@link OnPartialImageListener} on this object.
     *
     *  Will be called if there is an error in the input. Without one, a
     *  partial {@link Bitmap} will be created.
     */
    public void setOnPartialImageListener(@Nullable OnPartialImageListener l) { }

    /**
     *  Crop the output to {@code subset} of the (possibly) scaled image.
     *
     *  <p>{@code subset} must be contained within the size set by
     *  {@link #setResize} or the bounds of the image if setResize was not
     *  called. Otherwise an {@link IllegalStateException} will be thrown by
     *  {@link #decodeDrawable}/{@link #decodeBitmap}.</p>
     *
     *  <p>NOT intended as a replacement for
     *  {@link BitmapRegionDecoder#decodeRegion}. This supports all formats,
     *  but merely crops the output.</p>
     */
    public void setCrop(@Nullable Rect subset) { }

    /**
     *  Set a Rect for retrieving nine patch padding.
     *
     *  If the image is a nine patch, this Rect will be set to the padding
     *  rectangle during decode. Otherwise it will not be modified.
     *
     *  @hide
     */
    public void setOutPaddingRect(@NonNull Rect outPadding) {
        mOutPaddingRect = outPadding;
    }

    /**
     *  Specify whether the {@link Bitmap} should be mutable.
     *
     *  <p>By default, a {@link Bitmap} created will be immutable, but that can
     *  be changed with this call.</p>
     *
     *  <p>Mutable Bitmaps are incompatible with {@link #ALLOCATOR_HARDWARE},
     *  because {@link Bitmap.Config#HARDWARE} Bitmaps cannot be mutable.
     *  Attempting to combine them will throw an
     *  {@link java.lang.IllegalStateException}.</p>
     *
     *  <p>Mutable Bitmaps are also incompatible with {@link #decodeDrawable},
     *  which would require retrieving the Bitmap from the returned Drawable in
     *  order to modify. Attempting to decode a mutable {@link Drawable} will
     *  throw an {@link java.lang.IllegalStateException}.</p>
     */
    public ImageDecoder setMutable(boolean mutable) {
        return this;
    }

    /**
     *  Specify whether to potentially save RAM at the expense of quality.
     *
     *  Setting this to {@code true} may result in a {@link Bitmap} with a
     *  denser {@link Bitmap.Config}, depending on the image. For example, for
     *  an opaque {@link Bitmap}, this may result in a {@link Bitmap.Config}
     *  with no alpha information.
     */
    public ImageDecoder setPreferRamOverQuality(boolean preferRamOverQuality) {
        return this;
    }

    /**
     *  Specify whether to potentially treat the output as an alpha mask.
     *
     *  <p>If this is set to {@code true} and the image is encoded in a format
     *  with only one channel, treat that channel as alpha. Otherwise this call has
     *  no effect.</p>
     *
     *  <p>setAsAlphaMask is incompatible with {@link #ALLOCATOR_HARDWARE}. Trying to
     *  combine them will result in {@link #decodeDrawable}/
     *  {@link #decodeBitmap} throwing an
     *  {@link java.lang.IllegalStateException}.</p>
     */
    public ImageDecoder setAsAlphaMask(boolean asAlphaMask) {
        return this;
    }

    @Override
    public void close() {
    }

    /**
     *  Create a {@link Drawable} from a {@code Source}.
     *
     *  @param src representing the encoded image.
     *  @param listener for learning the {@link ImageInfo} and changing any
     *      default settings on the {@code ImageDecoder}. If not {@code null},
     *      this will be called on the same thread as {@code decodeDrawable}
     *      before that method returns.
     *  @return Drawable for displaying the image.
     *  @throws IOException if {@code src} is not found, is an unsupported
     *      format, or cannot be decoded for any reason.
     */
    @NonNull
    public static Drawable decodeDrawable(@NonNull Source src,
            @Nullable OnHeaderDecodedListener listener) throws IOException {
        Bitmap bitmap = decodeBitmap(src, listener);
        return new BitmapDrawable(src.getResources(), bitmap);
    }

    /**
     * See {@link #decodeDrawable(Source, OnHeaderDecodedListener)}.
     */
    @NonNull
    public static Drawable decodeDrawable(@NonNull Source src)
            throws IOException {
        return decodeDrawable(src, null);
    }

    /**
     *  Create a {@link Bitmap} from a {@code Source}.
     *
     *  @param src representing the encoded image.
     *  @param listener for learning the {@link ImageInfo} and changing any
     *      default settings on the {@code ImageDecoder}. If not {@code null},
     *      this will be called on the same thread as {@code decodeBitmap}
     *      before that method returns.
     *  @return Bitmap containing the image.
     *  @throws IOException if {@code src} is not found, is an unsupported
     *      format, or cannot be decoded for any reason.
     */
    @NonNull
    public static Bitmap decodeBitmap(@NonNull Source src,
            @Nullable OnHeaderDecodedListener listener) throws IOException {
        TypedValue value = new TypedValue();
        value.density = src.getDensity();
        ImageDecoder decoder = src.createImageDecoder();
        if (listener != null) {
            listener.onHeaderDecoded(decoder, new ImageInfo(decoder), src);
        }
        return BitmapFactory.decodeResourceStream(src.getResources(), value,
                ((InputStreamSource) src).mInputStream, decoder.mOutPaddingRect, null);
    }

    /**
     *  See {@link #decodeBitmap(Source, OnHeaderDecodedListener)}.
     */
    @NonNull
    public static Bitmap decodeBitmap(@NonNull Source src) throws IOException {
        return decodeBitmap(src, null);
    }

    public static final class DecodeException extends IOException {
        /**
         *  An Exception was thrown reading the {@link Source}.
         */
        public static final int SOURCE_EXCEPTION  = 1;

        /**
         *  The encoded data was incomplete.
         */
        public static final int SOURCE_INCOMPLETE = 2;

        /**
         *  The encoded data contained an error.
         */
        public static final int SOURCE_MALFORMED_DATA      = 3;

        @Error final int mError;
        @NonNull final Source mSource;

        DecodeException(@Error int error, @Nullable Throwable cause, @NonNull Source source) {
            super(errorMessage(error, cause), cause);
            mError = error;
            mSource = source;
        }

        /**
         * Private method called by JNI.
         */
        @SuppressWarnings("unused")
        DecodeException(@Error int error, @Nullable String msg, @Nullable Throwable cause,
                @NonNull Source source) {
            super(msg + errorMessage(error, cause), cause);
            mError = error;
            mSource = source;
        }

        /**
         *  Retrieve the reason that decoding was interrupted.
         *
         *  <p>If the error is {@link #SOURCE_EXCEPTION}, the underlying
         *  {@link java.lang.Throwable} can be retrieved with
         *  {@link java.lang.Throwable#getCause}.</p>
         */
        @Error
        public int getError() {
            return mError;
        }

        /**
         *  Retrieve the {@link Source Source} that was interrupted.
         *
         *  <p>This can be used for equality checking to find the Source which
         *  failed to completely decode.</p>
         */
        @NonNull
        public Source getSource() {
            return mSource;
        }

        private static String errorMessage(@Error int error, @Nullable Throwable cause) {
            switch (error) {
                case SOURCE_EXCEPTION:
                    return "Exception in input: " + cause;
                case SOURCE_INCOMPLETE:
                    return "Input was incomplete.";
                case SOURCE_MALFORMED_DATA:
                    return "Input contained an error.";
                default:
                    return "";
            }
        }
    }
}

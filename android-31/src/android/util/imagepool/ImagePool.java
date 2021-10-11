/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.util.imagepool;

import com.android.tools.layoutlib.annotations.NotNull;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.function.Consumer;

/**
 * Simplified version of image pool that exists in Studio.
 *
 * Lacks:
 * - PhantomReference and FinalizableReference to recognize the death of references automatically.
 *   (Meaning devs need to be more deligent in dispose.)
 * Has:
 * + Debugger that allows us to better trace where image is being leaked in stack.
 */
public interface ImagePool {

    /**
     * Returns a new image of width w and height h.
     */
    @NotNull
    Image acquire(final int w, final int h, final int type);

    /**
     * Disposes the image pool, releasing all the references to the buffered images.
     */
    void dispose();

    /**
     * Interface that represents a buffered image. Using this wrapper allows us ot better track
     * memory usages around BufferedImage. When all of it's references are removed, it will
     * automatically be pooled back into the image pool for re-use.
     */
    interface Image {

        /**
         * Same as {@link BufferedImage#setRGB(int, int, int, int, int[], int, int)}
         */
        void setRGB(int x, int y, int width, int height, int[] colors, int offset, int stride);

        /**
         * Same as {@link Graphics2D#drawImage(java.awt.Image, int, int, ImageObserver)}
         */
        void drawImage(Graphics2D graphics, int x, int y, ImageObserver o);

        /**
         * Image orientation. It's not used at the moment. To be used later.
         */
        enum Orientation {
            NONE,
            CW_90
        }

        int getWidth();
        int getHeight();
    }

    /**
     * Policy for how to set up the memory pool.
     */
    class ImagePoolPolicy {

        public final int[] mBucketSizes;
        public final int[] mNumberOfCopies;
        public final long mBucketMaxCacheSize;

        /**
         * @param bucketPixelSizes - list of pixel sizes to bucket (categorize) images. The list
         * must be sorted (low to high).
         * @param numberOfCopies - Allows users to create multiple copies of the bucket. It is
         * recommended to create more copies for smaller images to avoid fragmentation in memory.
         * It must match the size of bucketPixelSizes.
         * @param bucketMaxCacheByteSize - Maximum cache byte sizes image pool is allowed to hold onto
         * in memory.
         */
        public ImagePoolPolicy(
                int[] bucketPixelSizes, int[] numberOfCopies, long bucketMaxCacheByteSize) {
            assert bucketPixelSizes.length == numberOfCopies.length;
            mBucketSizes = bucketPixelSizes;
            mNumberOfCopies = numberOfCopies;
            mBucketMaxCacheSize = bucketMaxCacheByteSize;
        }
    }
}

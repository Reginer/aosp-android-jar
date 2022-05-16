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

import com.android.tools.layoutlib.annotations.Nullable;
import com.android.tools.layoutlib.annotations.VisibleForTesting;

import android.util.imagepool.ImagePool.Image.Orientation;

import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Data model for image pool. Bucket contains the list of same sized buffered image in soft ref.
 */
/* private package */ class Bucket {

    @VisibleForTesting final Queue<SoftReference<BufferedImage>> mBufferedImageRef = new LinkedList<>();

    public boolean isEmpty() {
        return mBufferedImageRef.isEmpty();
    }

    @Nullable
    public BufferedImage remove() {
        if (mBufferedImageRef.isEmpty()) {
            return null;
        }

        SoftReference<BufferedImage> reference = mBufferedImageRef.remove();
        return reference == null ? null : reference.get();
    }

    public void offer(BufferedImage img) {
        mBufferedImageRef.offer(new SoftReference<>(img));
    }

    public void clear() {
        mBufferedImageRef.clear();
    }

    static class BucketCreationMetaData {
        public final int mWidth;
        public final int mHeight;
        public final int mType;
        public final int mNumberOfCopies;
        public final Orientation mOrientation;
        public final long mMaxCacheSize;

        BucketCreationMetaData(int width, int height, int type, int numberOfCopies,
                Orientation orientation, long maxCacheSize) {
            mWidth = width;
            mHeight = height;
            mType = type;
            mNumberOfCopies = numberOfCopies;
            mOrientation = orientation;
            mMaxCacheSize = maxCacheSize;
        }
    }
}
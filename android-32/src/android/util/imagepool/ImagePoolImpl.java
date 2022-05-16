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

import android.util.imagepool.Bucket.BucketCreationMetaData;
import android.util.imagepool.ImagePool.Image.Orientation;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import com.google.common.base.FinalizablePhantomReference;
import com.google.common.base.FinalizableReferenceQueue;

class ImagePoolImpl implements ImagePool {

    private final ReentrantReadWriteLock mReentrantLock = new ReentrantReadWriteLock();
    private final ImagePoolPolicy mPolicy;
    @VisibleForTesting final Map<String, Bucket> mPool = new HashMap<>();
    @VisibleForTesting final ImagePoolStats mImagePoolStats = new ImagePoolStatsProdImpl();
    private final FinalizableReferenceQueue mFinalizableReferenceQueue = new FinalizableReferenceQueue();
    private final Set<Reference<?>> mReferences = new HashSet<>();

    public ImagePoolImpl(ImagePoolPolicy policy) {
        mPolicy = policy;
        mImagePoolStats.start();
    }

    @Override
    public Image acquire(int w, int h, int type) {
        return acquire(w, h, type, null);
    }

    /* package private */ Image acquire(int w, int h, int type,
            @Nullable Consumer<BufferedImage> freedCallback) {
        mReentrantLock.writeLock().lock();
        try {
            BucketCreationMetaData metaData =
                    ImagePoolHelper.getBucketCreationMetaData(w, h, type, mPolicy, mImagePoolStats);
            if (metaData == null) {
                return defaultImageImpl(w, h, type, freedCallback);
            }

            final Bucket existingBucket = ImagePoolHelper.getBucket(mPool, metaData, mPolicy);
            final BufferedImage img =
                    ImagePoolHelper.getBufferedImage(existingBucket, metaData, mImagePoolStats);
            if (img == null) {
                return defaultImageImpl(w, h, type, freedCallback);
            }

            // Clear the image. - is this necessary?
            if (img.getRaster().getDataBuffer().getDataType() == java.awt.image.DataBuffer.TYPE_INT) {
                Arrays.fill(((DataBufferInt)img.getRaster().getDataBuffer()).getData(), 0);
            }

            return prepareImage(
                    new ImageImpl(w, h, img, metaData.mOrientation),
                    true,
                    img,
                    existingBucket,
                    freedCallback);
        } finally {
            mReentrantLock.writeLock().unlock();
        }
    }

    /**
     * Add statistics as well as dispose behaviour before returning image.
     */
    private Image prepareImage(
            Image image,
            boolean offerBackToBucket,
            @Nullable BufferedImage img,
            @Nullable Bucket existingBucket,
            @Nullable Consumer<BufferedImage> freedCallback) {
        final Integer imageHash = image.hashCode();
        mImagePoolStats.acquiredImage(imageHash);
        FinalizablePhantomReference<Image> reference =
                new FinalizablePhantomReference<ImagePool.Image>(image, mFinalizableReferenceQueue) {
                    @Override
                    public void finalizeReferent() {
                        // This method might be called twice if the user has manually called the free() method. The second call will have no effect.
                        if (mReferences.remove(this)) {
                            mImagePoolStats.disposeImage(imageHash);
                            if (offerBackToBucket) {
                                if (!mImagePoolStats.fitsMaxCacheSize(img.getWidth(), img.getHeight(),
                                        mPolicy.mBucketMaxCacheSize)) {
                                    mImagePoolStats.tooBigForCache();
                                    // Adding this back would go over the max cache size we set for ourselves. Release it.
                                    return;
                                }

                                // else stat does not change.
                                existingBucket.offer(img);
                            }
                            if (freedCallback != null) {
                                freedCallback.accept(img);
                            }
                        }
                    }
                };
        mReferences.add(reference);
        return image;
    }

    /**
     * Default Image Impl to be used when the pool is not big enough.
     */
    private Image defaultImageImpl(int w, int h, int type,
            @Nullable Consumer<BufferedImage> freedCallback) {
        BufferedImage bufferedImage = new BufferedImage(w, h, type);
        mImagePoolStats.tooBigForCache();
        mImagePoolStats.recordAllocOutsidePool(w, h);
        return prepareImage(new ImageImpl(w, h, bufferedImage, Orientation.NONE),
                false,  null, null, freedCallback);
    }

    @Override
    public void dispose() {
        mReentrantLock.writeLock().lock();
        try {
            for (Bucket bucket : mPool.values()) {
                bucket.clear();
            }
            mImagePoolStats.clear();
        } finally {
            mReentrantLock.writeLock().unlock();
        }
    }

    /* package private */ void printStat() {
        System.out.println(mImagePoolStats.getStatistic());
    }
}
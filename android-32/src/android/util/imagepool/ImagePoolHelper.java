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

import android.util.imagepool.Bucket.BucketCreationMetaData;
import android.util.imagepool.ImagePool.Image.Orientation;
import android.util.imagepool.ImagePool.ImagePoolPolicy;

import java.awt.image.BufferedImage;
import java.util.Map;

/* private package */ class ImagePoolHelper {

    @Nullable
    public static BucketCreationMetaData getBucketCreationMetaData(int w, int h, int type, ImagePoolPolicy poolPolicy, ImagePoolStats stats) {
        // Find the bucket sizes for both dimensions
        int widthBucket = -1;
        int heightBucket = -1;
        int index = 0;

        for (int bucketMinSize : poolPolicy.mBucketSizes) {
            if (widthBucket == -1 && w <= bucketMinSize) {
                widthBucket = bucketMinSize;

                if (heightBucket != -1) {
                    break;
                }
            }
            if (heightBucket == -1 && h <= bucketMinSize) {
                heightBucket = bucketMinSize;

                if (widthBucket != -1) {
                    break;
                }
            }
            ++index;
        }

        stats.recordBucketRequest(w, h);

        if (index >= poolPolicy.mNumberOfCopies.length) {
            return null;
        }

        // TODO: Apply orientation
//        if (widthBucket < heightBucket) {
//            return new BucketCreationMetaData(heightBucket, widthBucket, type, poolPolicy.mNumberOfCopies[index],
//                    Orientation.CW_90, poolPolicy.mBucketMaxCacheSize);
//        }
        return new BucketCreationMetaData(widthBucket, heightBucket, type, poolPolicy.mNumberOfCopies[index],
                Orientation.NONE, poolPolicy.mBucketMaxCacheSize);
    }

    @Nullable
    public static BufferedImage getBufferedImage(
            Bucket bucket, BucketCreationMetaData metaData, ImagePoolStats stats) {

        // strongref is just for gc.
        BufferedImage strongRef = populateBucket(bucket, metaData, stats);

        // pool is too small to create the requested buffer.
        if (bucket.isEmpty()) {
            assert strongRef == null;
            return null;
        }

        // Go through the bucket of soft references to find the first buffer that's not null.
        // Even if gc is triggered here, strongref should survive.
        BufferedImage img = bucket.remove();
        while (img == null && !bucket.isEmpty()) {
            img = bucket.remove();
        }

        if (img == null && bucket.isEmpty()) {
            // Whole buffer was null. Recurse.
            return getBufferedImage(bucket, metaData, stats);
        }
        return img;
    }

    /**
     * Populate the bucket in greedy manner to avoid fragmentation.
     * Behaviour is controlled by {@link ImagePoolPolicy}.
     * Returns one strong referenced buffer to avoid getting results gc'd. Null if pool is not large
     * enough.
     */
    @Nullable
    private static BufferedImage populateBucket(
            Bucket bucket, BucketCreationMetaData metaData, ImagePoolStats stats) {
        if (!bucket.isEmpty()) {
            // If not empty no need to populate.
            return null;
        }

        BufferedImage strongRef = null;
        for (int i = 0; i < metaData.mNumberOfCopies; i++) {
            if (!stats.fitsMaxCacheSize(
                    metaData.mWidth, metaData.mHeight, metaData.mMaxCacheSize)) {
                break;
            }
            strongRef =new BufferedImage(metaData.mWidth, metaData.mHeight,
                    metaData.mType);
            bucket.offer(strongRef);
            stats.recordBucketCreation(metaData.mWidth, metaData.mHeight);
        }
        return strongRef;
    }

    private static String toKey(int w, int h, int type) {
        return new StringBuilder()
                .append(w)
                .append('x')
                .append(h)
                .append(':')
                .append(type)
                .toString();
    }

    public static Bucket getBucket(Map<String, Bucket> map, BucketCreationMetaData metaData, ImagePoolPolicy mPolicy) {
        String key = toKey(metaData.mWidth, metaData.mHeight, metaData.mType);
        Bucket bucket = map.get(key);
        if (bucket == null) {
            bucket = new Bucket();
            map.put(key, bucket);
        }
        return bucket;
    }
}
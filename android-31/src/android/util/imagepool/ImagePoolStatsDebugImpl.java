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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Useful impl for debugging reproable error.
 */
public class ImagePoolStatsDebugImpl extends ImagePoolStatsProdImpl {

    private static String PACKAGE_NAME = ImagePoolStats.class.getPackage().getName();

    // Used for deugging purposes only.
    private final Map<Integer, String> mCallStack = new HashMap<>();
    private long mRequestedTotalBytes = 0;
    private long mAllocatedOutsidePoolBytes = 0;

    // Used for gc-related stats.
    private long mPreviousGcCollection = 0;
    private long mPreviousGcTime = 0;

    /** Used for policy */
    @Override
    public void recordBucketCreation(int widthBucket, int heightBucket) {
        super.recordBucketCreation(widthBucket, heightBucket);
    }

    @Override
    public boolean fitsMaxCacheSize(int width, int height, long maxCacheSize) {
        return super.fitsMaxCacheSize(width, height, maxCacheSize);
    }

    @Override
    public void clear() {
        super.clear();

        mRequestedTotalBytes = 0;
        mAllocatedOutsidePoolBytes = 0;
        mTooBigForPoolCount = 0;
        mCallStack.clear();
    }

    @Override
    public void tooBigForCache() {
        super.tooBigForCache();
    }

    /** Used for Debugging only */
    @Override
    public void recordBucketRequest(int w, int h) {
        mRequestedTotalBytes += (w * h * ESTIMATED_PIXEL_BYTES);
    }

    @Override
    public void recordAllocOutsidePool(int width, int height) {
        mAllocatedOutsidePoolBytes += (width * height * ESTIMATED_PIXEL_BYTES);
    }

    @Override
    public void acquiredImage(Integer imageHash) {
        for (int i = 1; i < Thread.currentThread().getStackTrace().length; i++) {
            StackTraceElement element = Thread.currentThread().getStackTrace()[i];
            String str = element.toString();

            if (!str.contains(PACKAGE_NAME)) {
                mCallStack.put(imageHash, str);
                break;
            }
        }
    }

    @Override
    public void disposeImage(Integer imageHash) {
        mCallStack.remove(imageHash);
    }

    @Override
    public void start() {
        long totalGarbageCollections = 0;
        long garbageCollectionTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gc.getCollectionCount();
            if (count >= 0) {
                totalGarbageCollections += count;
            }
            long time = gc.getCollectionTime();
            if (time >= 0) {
                garbageCollectionTime += time;
            }
        }
        mPreviousGcCollection = totalGarbageCollections;
        mPreviousGcTime = garbageCollectionTime;
    }

    private String calculateGcStatAndReturn() {
        long totalGarbageCollections = 0;
        long garbageCollectionTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gc.getCollectionCount();
            if (count > 0) {
                totalGarbageCollections += count;
            }
            long time = gc.getCollectionTime();
            if(time > 0) {
                garbageCollectionTime += time;
            }
        }
        totalGarbageCollections -= mPreviousGcCollection;
        garbageCollectionTime -= mPreviousGcTime;

        StringBuilder builder = new StringBuilder();
        builder.append("Total Garbage Collections: ");
        builder.append(totalGarbageCollections);
        builder.append("\n");

        builder.append("Total Garbage Collection Time (ms): ");
        builder.append(garbageCollectionTime);
        builder.append("\n");

        return builder.toString();
    }

    @Override
    public String getStatistic() {
        StringBuilder builder = new StringBuilder();

        builder.append(calculateGcStatAndReturn());
        builder.append("Memory\n");
        builder.append(" requested total         : ");
        builder.append(mRequestedTotalBytes / 1_000_000);
        builder.append(" MB\n");
        builder.append(" allocated (in pool)     : ");
        builder.append(mAllocateTotalBytes / 1_000_000);
        builder.append(" MB\n");
        builder.append(" allocated (out of pool) : ");
        builder.append(mAllocatedOutsidePoolBytes / 1_000_000);
        builder.append(" MB\n");

        double percent = (1.0 - (double) mRequestedTotalBytes / (mAllocateTotalBytes +
                mAllocatedOutsidePoolBytes));
        if (percent < 0.0) {
            builder.append(" saved : ");
            builder.append(-1.0 * percent);
            builder.append("%\n");
        } else {
            builder.append(" wasting : ");
            builder.append(percent);
            builder.append("%\n");
        }

        builder.append("Undispose images\n");
        Multiset<String> countSet = HashMultiset.create();
        for (String callsite : mCallStack.values()) {
            countSet.add(callsite);
        }

        for (Multiset.Entry<String> entry : countSet.entrySet()) {
            builder.append(" - ");
            builder.append(entry.getElement());
            builder.append(" - missed dispose : ");
            builder.append(entry.getCount());
            builder.append(" times\n");
        }

        builder.append("Number of times requested image didn't fit the pool : ");
        builder.append(mTooBigForPoolCount);
        builder.append("\n");

        return builder.toString();
    }
}

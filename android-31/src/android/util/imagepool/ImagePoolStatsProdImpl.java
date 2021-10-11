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
import com.android.tools.layoutlib.annotations.VisibleForTesting;

class ImagePoolStatsProdImpl implements ImagePoolStats {

    static int ESTIMATED_PIXEL_BYTES = 4;

    // Used for determining how many buckets can be created.
    @VisibleForTesting long mAllocateTotalBytes = 0;
    @VisibleForTesting int mTooBigForPoolCount = 0;

    /** Used for policy */
    @Override
    public void recordBucketCreation(int widthBucket, int heightBucket) {
        mAllocateTotalBytes += (widthBucket * heightBucket * ESTIMATED_PIXEL_BYTES);
    }

    @Override
    public boolean fitsMaxCacheSize(int width, int height, long maxCacheSize) {
        long newTotal = mAllocateTotalBytes + (width * height * ESTIMATED_PIXEL_BYTES);
        return newTotal <= maxCacheSize;
    }

    @Override
    public void tooBigForCache() {
        mTooBigForPoolCount++;
    }

    @Override
    public void clear() {
        mAllocateTotalBytes = 0;
    }

    @Override
    public void recordBucketRequest(int w, int h) { }

    @Override
    public void recordAllocOutsidePool(int width, int height) { }

    @Override
    public void acquiredImage(@NotNull Integer imageHash) { }

    @Override
    public void disposeImage(@NotNull Integer imageHash) { }

    @Override
    public void start() { }

    @Override
    public String getStatistic() { return ""; }
}

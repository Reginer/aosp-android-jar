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
import com.android.tools.layoutlib.annotations.Nullable;
import com.android.tools.layoutlib.annotations.VisibleForTesting;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Representation of buffered image. When used with ImagePool, it provides 2 features
 * (last one not yet impl'd):
 *
 * <ul>
 *   <li>Automatic recycle of BufferedImage through use of reference counter</li>
 *   <li>Able to re-use the image for similar size of buffered image</li>
 *   <li>TODO: Able to re-use the iamge for different orientation (not yet impl'd)</li>
 * </ul>
 */
/* private package */ class ImageImpl implements ImagePool.Image {

    private final ReadWriteLock mLock = new ReentrantReadWriteLock();

    private final int mWidth;
    private final int mHeight;
    private final Orientation mOrientation;

    @VisibleForTesting final BufferedImage mImg;

    ImageImpl(
            int width,
            int height,
            @NotNull BufferedImage img,
            Orientation orientation) {
        mImg = img;
        mWidth = width;
        mHeight = height;
        mOrientation = orientation;
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public void setRGB(int x, int y, int width, int height, int[] colors, int offset, int stride) {
        mLock.readLock().lock();
        try {
            // TODO: Apply orientation.
            mImg.setRGB(x, y, width, height, colors, offset, stride);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    public void drawImage(Graphics2D graphics, int x, int y, @Nullable ImageObserver o) {
        mLock.readLock().lock();
        try {
            // TODO: Apply orientation.
            graphics.drawImage(mImg, x, y, mWidth, mHeight, o);
        } finally {
            mLock.readLock().unlock();
        }
    }
}
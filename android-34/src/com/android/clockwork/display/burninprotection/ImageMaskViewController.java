/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.clockwork.display.burninprotection;

import android.annotation.Nullable;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.function.TriFunction;

class ImageMaskViewController {
    private static final String TAG = "BurnInProtector_IMVController";
    private static final boolean DEBUG = BurnInProtector.DEBUG;

    private static final int[] MASK_TYPE_ARRAY = {
            ImageMaskTileFactory.MASK_TYPE_1,
            ImageMaskTileFactory.MASK_TYPE_2
    };

    private final Context mContext;
    private final TriFunction<Context, Integer, Bitmap, ImageMaskView> mImageMaskViewFactory;
    private final ImageMaskTileFactory mImageMaskFactory;
    private final SparseArray<Bitmap> mBitmapCacheArray = new SparseArray<>();

    @Nullable
    private ImageMaskView mImageMaskView;

    ImageMaskViewController(Context context,
            TriFunction<Context, Integer, Bitmap, ImageMaskView> imageMaskViewFactory,
            ImageMaskTileFactory imageMaskFactory) {
        mContext = context;
        mImageMaskViewFactory = imageMaskViewFactory;
        mImageMaskFactory = imageMaskFactory;
    }

    ImageMaskViewController(final Context context) {
        this(context, (c, m, b) -> new ImageMaskView(c, b, m), new ImageMaskTileFactory());
    }

    private static void logd(String msg) {
        if (!DEBUG) {
            return;
        }

        Log.d(TAG, msg);
    }

    void enableImageMask() {
        if (isEnabled()) {
            return;
        }
        logd("enableImageMask: ");
        final int maskType = MASK_TYPE_ARRAY[0];
        mImageMaskView = mImageMaskViewFactory.apply(mContext, maskType,
                getBitmapTileMaskType(maskType));
        mImageMaskView.show();
    }

    private Bitmap getBitmapTileMaskType(int maskType) {
        if (!mBitmapCacheArray.contains(maskType)) {
            mBitmapCacheArray.put(maskType, mImageMaskFactory.createBitmapTile(maskType));
        }

        return mBitmapCacheArray.get(maskType);
    }

    void disableImageMask() {
        if (!isEnabled()) {
            return;
        }
        logd("disableImageMask: ");
        mImageMaskView.hide();
        mImageMaskView = null;

        mBitmapCacheArray.clear();
    }

    @VisibleForTesting
    void updateImageMask() {
        if (!isEnabled()) {
            return;
        }

        int nextMaskType = getNextImageMaskType(MASK_TYPE_ARRAY, mImageMaskView.getMaskViewType());
        logd("updateImageMask: nextMaskType=" + nextMaskType);

        mImageMaskView.updateMaskType(nextMaskType, getBitmapTileMaskType(nextMaskType));
    }

    @VisibleForTesting
    int getNextImageMaskType(int[] maskImageTypeArray, int maskType) {
        return (maskType + 1) % maskImageTypeArray.length;
    }

    @VisibleForTesting
    boolean isEnabled() {
        return mImageMaskView != null;
    }
}

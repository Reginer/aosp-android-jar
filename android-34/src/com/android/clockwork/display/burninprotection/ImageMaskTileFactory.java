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

import android.annotation.IntDef;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Creates image mask tile that is repeated on {@link ImageMaskView}. This bitmap tile will be
 * repeated with {@link android.graphics.BitmapShader}.
 */
class ImageMaskTileFactory {

    static final int MASK_TYPE_1 = 0;
    static final int MASK_TYPE_2 = 1;

    private static final int DEFAULT_TILE_SIZE = 2;

    private final int mTileSize;

    @VisibleForTesting
    ImageMaskTileFactory(int tileSize) {
        mTileSize = tileSize;
    }

    ImageMaskTileFactory() {
        this(DEFAULT_TILE_SIZE);
    }

    Bitmap createBitmapTile(@MaskType final int maskType) {

        final Bitmap bitmap = Bitmap.createBitmap(mTileSize, mTileSize, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        //Initialize transparent color
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        final Paint blackPaint = createTilePaint(Color.BLACK);

        //Draw pattern
        for (int y = 0; y < mTileSize; y++) {
            for (int x = 0; x < mTileSize; x++) {
                if ((y + x) % 2 == maskType) {
                    continue;
                }

                canvas.drawPoint(x, y, blackPaint);
            }
        }

        return bitmap;
    }

    @VisibleForTesting
    Paint createTilePaint(final int color) {
        final Paint paint = new Paint();
        paint.setStrokeWidth(0);
        paint.setColor(color);
        paint.setAntiAlias(false);
        paint.setStyle(Paint.Style.FILL);

        return paint;
    }

    @IntDef({MASK_TYPE_1, MASK_TYPE_2})
    @interface MaskType {
    }
}
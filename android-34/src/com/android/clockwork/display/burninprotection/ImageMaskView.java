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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;

class ImageMaskView {
    private static final String TAG = "BurnInProtector_IMV";

    private static final int Z_ORDER = WindowManager.LayoutParams.TYPE_POINTER
            * WindowManagerPolicyConstants.TYPE_LAYER_MULTIPLIER + 10;

    private final SurfaceControl mSurfaceControl;
    private final SurfaceControl.Transaction mSurfaceControlTransaction;
    private final Surface mSurface;
    private final Rect mDrawRect;
    private final int mLayerStack;

    private Bitmap mBitmapTile;
    private int mMaskViewType;
    private boolean mIsShowing;

    ImageMaskView(Context context, Bitmap bitmapTile, int maskViewType) {
        mBitmapTile = bitmapTile;
        mDrawRect =
                context.getSystemService(WindowManager.class).getCurrentWindowMetrics().getBounds();
        mSurfaceControlTransaction = new SurfaceControl.Transaction();
        mSurfaceControl =
                new SurfaceControl.Builder()
                        .setName(ImageMaskView.class.getName())
                        .setBufferSize(mDrawRect.width(), mDrawRect.height())
                        .setFormat(PixelFormat.TRANSLUCENT)
                        .build();
        mSurface = new Surface(mSurfaceControl);
        Display display =
                context.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        if (display != null) {
            mLayerStack = display.getLayerStack();
        } else {
            mLayerStack = 0;
        }
        mMaskViewType = maskViewType;
    }

    int getMaskViewType() {
        return mMaskViewType;
    }

    private void visualizeBurnInForDebug(Canvas canvas) {
        Paint p = new Paint();
        p.setColor(Color.GREEN);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(50);
        canvas.drawText(
                "BurnIn Mask: " + mMaskViewType, mDrawRect.centerX(), mDrawRect.centerY(), p);
    }

    private void redraw() {
        if (!mSurface.isValid()) {
            Log.w(TAG, "redraw: mSurface is not valid");
            return;
        }

        try {
            final Canvas canvas = mSurface.lockCanvas(mDrawRect);
            if (canvas == null) {
                return;
            }

            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            final Paint paint = new Paint();
            final BitmapShader shader = new BitmapShader(mBitmapTile,
                    Shader.TileMode.REPEAT,
                    Shader.TileMode.REPEAT);
            paint.setShader(shader);
            canvas.drawRect(mDrawRect, paint);

            if (BurnInProtector.DEBUG) {
                visualizeBurnInForDebug(canvas);
            }

            mSurface.unlockCanvasAndPost(canvas);
        } catch (IllegalArgumentException | Surface.OutOfResourcesException e) {
            Log.e(TAG, "redraw: ", e);
        }
    }

    void show() {
        if (!mIsShowing) {
            mIsShowing = true;
            redraw();
            mSurfaceControlTransaction
                    .setLayerStack(mSurfaceControl, mLayerStack)
                    .setLayer(mSurfaceControl, Z_ORDER)
                    .setPosition(mSurfaceControl, 0, 0)
                    .show(mSurfaceControl)
                    .apply();
        }
    }

    void updateMaskType(int maskType, Bitmap bitmapTile) {
        mBitmapTile = bitmapTile;
        mMaskViewType = maskType;
        if (mIsShowing) {
            redraw();
        }
    }

    void hide() {
        if (mIsShowing) {
            mIsShowing = false;
            mSurfaceControlTransaction.hide(mSurfaceControl)
                    .apply();
            if (mSurface.isValid()) {
                mSurface.release();
            }
        }
    }
}

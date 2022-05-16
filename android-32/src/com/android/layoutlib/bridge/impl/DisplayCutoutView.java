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

package com.android.layoutlib.bridge.impl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.View;

class DisplayCutoutView extends View {

    private final DisplayInfo mInfo = new DisplayInfo();
    private final Paint mPaint = new Paint();
    private final Region mBounds = new Region();
    private final Rect mBoundingRect = new Rect();
    private final Path mBoundingPath = new Path();
    private final int[] mLocation = new int[2];
    private final boolean mStart;

    public DisplayCutoutView(Context context, boolean start) {
        super(context);
        mStart = start;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        getLocationOnScreen(mLocation);
        canvas.translate(-mLocation[0], -mLocation[1]);
        if (!mBoundingPath.isEmpty()) {
            mPaint.setColor(Color.BLACK);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(mBoundingPath, mPaint);
        }
    }

    private void update() {
        requestLayout();
        getDisplay().getDisplayInfo(mInfo);
        mBounds.setEmpty();
        mBoundingRect.setEmpty();
        mBoundingPath.reset();
        int newVisible;
        if (hasCutout()) {
            mBounds.set(mInfo.displayCutout.getBoundingRectTop());
            localBounds(mBoundingRect);
            mBounds.getBoundaryPath(mBoundingPath);
            newVisible = VISIBLE;
        } else {
            newVisible = GONE;
        }
        if (newVisible != getVisibility()) {
            setVisibility(newVisible);
        }
    }

    private boolean hasCutout() {
        final DisplayCutout displayCutout = mInfo.displayCutout;
        if (displayCutout == null) {
            return false;
        }
        if (mStart) {
            return displayCutout.getSafeInsetLeft() > 0
                    || displayCutout.getSafeInsetTop() > 0;
        } else {
            return displayCutout.getSafeInsetRight() > 0
                    || displayCutout.getSafeInsetBottom() > 0;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mBounds.isEmpty()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        setMeasuredDimension(
                resolveSizeAndState(mBoundingRect.width(), widthMeasureSpec, 0),
                resolveSizeAndState(mBoundingRect.height(), heightMeasureSpec, 0));
    }

    public static void boundsFromDirection(DisplayCutout displayCutout, int gravity, Rect out) {
        Region bounds = new Region(displayCutout.getBoundingRectTop());
        switch (gravity) {
            case Gravity.TOP:
                bounds.op(0, 0, Integer.MAX_VALUE, displayCutout.getSafeInsetTop(),
                        Region.Op.INTERSECT);
                out.set(bounds.getBounds());
                break;
            case Gravity.LEFT:
                bounds.op(0, 0, displayCutout.getSafeInsetLeft(), Integer.MAX_VALUE,
                        Region.Op.INTERSECT);
                out.set(bounds.getBounds());
                break;
            case Gravity.BOTTOM:
                bounds.op(0, displayCutout.getSafeInsetTop() + 1, Integer.MAX_VALUE,
                        Integer.MAX_VALUE, Region.Op.INTERSECT);
                out.set(bounds.getBounds());
                break;
            case Gravity.RIGHT:
                bounds.op(displayCutout.getSafeInsetLeft() + 1, 0, Integer.MAX_VALUE,
                        Integer.MAX_VALUE, Region.Op.INTERSECT);
                out.set(bounds.getBounds());
                break;
        }
        bounds.recycle();
    }

    private void localBounds(Rect out) {
        final DisplayCutout displayCutout = mInfo.displayCutout;

        if (mStart) {
            if (displayCutout.getSafeInsetLeft() > 0) {
                boundsFromDirection(displayCutout, Gravity.LEFT, out);
            } else if (displayCutout.getSafeInsetTop() > 0) {
                boundsFromDirection(displayCutout, Gravity.TOP, out);
            }
        } else {
            if (displayCutout.getSafeInsetRight() > 0) {
                boundsFromDirection(displayCutout, Gravity.RIGHT, out);
            } else if (displayCutout.getSafeInsetBottom() > 0) {
                boundsFromDirection(displayCutout, Gravity.BOTTOM, out);
            }
        }
    }
}
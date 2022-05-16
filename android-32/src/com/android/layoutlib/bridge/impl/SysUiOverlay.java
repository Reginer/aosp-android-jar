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
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

class SysUiOverlay extends View {
    private final Path mCornerPath;
    private final Path mNotchPath;
    private final Paint mCornerPaint;
    private final Paint mNotchPaint;
    private final int mNotchTopWidth;


    private static Path createCornerPath(float width, float height, float r) {
        Path corner = new Path();
        corner.moveTo(0, 0);
        corner.lineTo(width, 0);
        corner.cubicTo(width, 0, width /2 - r, height /2 - r, 0, height);
        corner.close();

        return corner;
    }

    private static Path createNotchPath(float topWidth, float bottomWidth, float height, float r) {
        Path corner = new Path();
        corner.moveTo(0, 0);
        float widthDiff = topWidth - bottomWidth;
        corner.lineTo(topWidth, 0);
        corner.lineTo(topWidth - widthDiff/2, height);
        corner.lineTo(widthDiff/2, height);
        corner.close();

        return corner;
    }


    private static float dpToPx(DisplayMetrics metrics, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, value, metrics);
    }


    public SysUiOverlay(Context context, int cornerSizeDp, int radiusDp, int topNotchWidthDp, int bottomNotchWidthDp, int notchHeightDp) {
        super(context);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        float cornerSizePx = dpToPx(metrics, cornerSizeDp);
        float radiusPx = dpToPx(metrics, radiusDp);

        float topNotchWidthPx = dpToPx(metrics, topNotchWidthDp);
        float bottomNotchWidthPx = dpToPx(metrics, bottomNotchWidthDp);
        float notchHeightPx = dpToPx(metrics, notchHeightDp);

        mCornerPath = createCornerPath(cornerSizePx, cornerSizePx, radiusPx);
        mNotchPath = createNotchPath(topNotchWidthPx, bottomNotchWidthPx, notchHeightPx, 0);
        mNotchTopWidth = topNotchWidthDp;

        mCornerPaint = new Paint();
        mCornerPaint.setColor(Color.BLACK);
        mCornerPaint.setStrokeWidth(0);
        mCornerPaint.setAntiAlias(true);

        mNotchPaint = new Paint();
        mNotchPaint.setColor(Color.BLACK);
        mNotchPaint.setStrokeWidth(0);
        mNotchPaint.setAntiAlias(true);
    }

    public void setCornerColor(int color) {
        mCornerPaint.setColor(color);
    }

    public void setNotchColor(int color) {
        mNotchPaint.setColor(color);
    }

    private void paintRoundedBorders(Canvas canvas) {
        // Top left
        canvas.drawPath(mCornerPath, mCornerPaint);
        // Top right
        canvas.save();
        canvas.translate(getWidth(), 0);
        canvas.rotate(90);
        canvas.drawPath(mCornerPath, mCornerPaint);
        canvas.restore();
        // Bottom right
        canvas.save();
        canvas.translate( getWidth(), getHeight());
        canvas.rotate(180);
        canvas.drawPath(mCornerPath, mCornerPaint);
        canvas.restore();
        // Bottom left
        canvas.save();
        canvas.translate( 0, getHeight());
        canvas.rotate(270);
        canvas.drawPath(mCornerPath, mCornerPaint);
        canvas.restore();
    }

    private void paintNotch(Canvas canvas) {
        canvas.translate(getWidth() / 2 - mNotchTopWidth / 2, 0);
        canvas.drawPath(mNotchPath, mNotchPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paintRoundedBorders(canvas);
        paintNotch(canvas);

    }
}